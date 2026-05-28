# Notification Schema (PostgreSQL)

本文件定義 FCM 通知後端最小可用資料模型，對應以下 API：

- `POST /api/v1/push/register-token`
- `POST /api/v1/internal/notifications/send`
- `POST /api/v1/notifications/{id}/open`

---

## 1) 型別（Enums）

```sql
CREATE TYPE platform_type AS ENUM ('ANDROID');
CREATE TYPE token_status AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE notification_type AS ENUM ('SYSTEM', 'PROMOTION', 'ACTIVITY');
CREATE TYPE delivery_status AS ENUM ('QUEUED', 'SENT', 'DELIVERED', 'FAILED');
```

---

## 2) `push_registrations`

儲存使用者與裝置的 FCM token 註冊狀態。

```sql
CREATE TABLE push_registrations (
  id                TEXT PRIMARY KEY,
  user_id           TEXT NOT NULL,
  device_id         TEXT NOT NULL,
  fcm_token         TEXT NOT NULL,
  platform          platform_type NOT NULL DEFAULT 'ANDROID',
  app_version       TEXT,
  locale            TEXT,
  timezone          TEXT,
  status            token_status NOT NULL DEFAULT 'ACTIVE',
  token_hash        TEXT NOT NULL,
  last_seen_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_push_registrations_user_device
  ON push_registrations (user_id, device_id);

CREATE UNIQUE INDEX uq_push_registrations_fcm_token
  ON push_registrations (fcm_token);

CREATE INDEX idx_push_registrations_status_last_seen
  ON push_registrations (status, last_seen_at DESC);
```

---

## 3) `notification_events`

通知事件主檔（內容、發送者、冪等鍵、排程）。

```sql
CREATE TABLE notification_events (
  id                TEXT PRIMARY KEY,
  type              notification_type NOT NULL,
  title             TEXT NOT NULL,
  body              TEXT NOT NULL,
  payload_jsonb     JSONB NOT NULL DEFAULT '{}'::jsonb,
  sender_type       TEXT NOT NULL, -- internal_service / admin_console / cron
  idempotency_key   TEXT NOT NULL,
  schedule_at       TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_notification_events_idempotency_key
  ON notification_events (idempotency_key);

CREATE INDEX idx_notification_events_created_at
  ON notification_events (created_at DESC);
```

---

## 4) `notification_recipients`

每位收件者與每個裝置的投遞結果（重試、失敗原因、供應商訊息 ID）。

```sql
CREATE TABLE notification_recipients (
  id                   TEXT PRIMARY KEY,
  notification_id      TEXT NOT NULL REFERENCES notification_events(id) ON DELETE CASCADE,
  user_id              TEXT NOT NULL,
  registration_id      TEXT NOT NULL REFERENCES push_registrations(id) ON DELETE RESTRICT,
  delivery_status      delivery_status NOT NULL DEFAULT 'QUEUED',
  provider_message_id  TEXT,
  retry_count          INT NOT NULL DEFAULT 0,
  sent_at              TIMESTAMPTZ,
  delivered_at         TIMESTAMPTZ,
  failed_at            TIMESTAMPTZ,
  failure_reason       TEXT,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_notification_recipients_triplet
  ON notification_recipients (notification_id, user_id, registration_id);

CREATE INDEX idx_notification_recipients_user_status
  ON notification_recipients (user_id, delivery_status, created_at DESC);
```

---

## 5) `user_notifications`

使用者收件匣來源。對齊既有 `GET /api/v1/notifications`、`PATCH .../read` 的業務語意。

```sql
CREATE TABLE user_notifications (
  id                TEXT PRIMARY KEY,
  user_id           TEXT NOT NULL,
  notification_id   TEXT NOT NULL REFERENCES notification_events(id) ON DELETE CASCADE,
  title             TEXT NOT NULL,
  body              TEXT NOT NULL,
  type              notification_type NOT NULL,
  is_read           BOOLEAN NOT NULL DEFAULT FALSE,
  read_at           TIMESTAMPTZ,
  opened_at         TIMESTAMPTZ,
  source            TEXT, -- push_tap / inbox_click
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_notifications_user_created
  ON user_notifications (user_id, created_at DESC);

CREATE INDEX idx_user_notifications_user_unread
  ON user_notifications (user_id, is_read, created_at DESC);

CREATE UNIQUE INDEX uq_user_notifications_user_notification
  ON user_notifications (user_id, notification_id);
```

---

## 6) 冪等與一致性建議

- `register-token`：以 `(user_id, device_id)` upsert，更新 `fcm_token` 與 `last_seen_at`。
- `send`：`notification_events.idempotency_key` 唯一，重複請求回傳同一事件 ID。
- `mark-open`：`user_notifications.opened_at` 僅首次寫入；重複寫入回傳 `opened=false`。

---

## 7) 查詢範例

### 7.1 讀取使用者通知列表（分頁）

```sql
SELECT id, title, body, type, is_read, EXTRACT(EPOCH FROM created_at) * 1000 AS created_at
FROM user_notifications
WHERE user_id = $1
ORDER BY created_at DESC
LIMIT $2 OFFSET $3;
```

### 7.2 查詢未讀數

```sql
SELECT COUNT(*)
FROM user_notifications
WHERE user_id = $1 AND is_read = FALSE;
```

