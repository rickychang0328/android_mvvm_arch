# API 規格書 — Auth, Profile & Notifications

**Base URL:** `https://api.example.com/`  
**API Version:** `v1`  
**Content-Type:** `application/json`  
**認證方式:** Bearer Token（JWT）

> 本專案以 `MockApiInterceptor` 模擬下列端點回應，便於離線開發與測試。接上真實後端時，請保持 DTO 欄位與路徑一致。

---

## 1. 登入

### `POST /api/v1/auth/login`

以電子郵件與密碼換取存取權杖。

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "your-password"
}
```

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| email | string | 是 | 使用者電子郵件 |
| password | string | 是 | 密碼（傳輸須 HTTPS） |

**Response `200 OK`**

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "expires_in": 3600,
  "token_type": "Bearer"
}
```

**Response `401 Unauthorized`**

```json
{
  "error": "invalid_credentials",
  "message": "Email or password is incorrect."
}
```

**Response `422 Unprocessable Entity`**

```json
{
  "error": "validation_error",
  "message": "Invalid email format.",
  "fields": { "email": "must be a valid email" }
}
```

**Demo 帳號（Mock）**

| email | password | 說明 |
|-------|----------|------|
| demo@example.com | password123 | 登入成功 |
| 其他 | 任意 | 回傳 401 |

---

## 2. 登出

### `POST /api/v1/auth/logout`

使目前存取權杖失效（伺服器端黑名單，可選）。

**Headers**

```
Authorization: Bearer {access_token}
```

**Response `204 No Content`**

無 Body。

---

## 3. 取得個人資料

### `GET /api/v1/users/me`

取得已登入使用者的個人資料。

**Headers**

```
Authorization: Bearer {access_token}
```

**Response `200 OK`**

```json
{
  "id": "usr_001",
  "email": "demo@example.com",
  "display_name": "Demo User",
  "avatar_url": "https://api.example.com/avatars/usr_001.png",
  "phone": "+886912345678",
  "bio": "Android MVVM sample user.",
  "created_at": "2024-01-15T08:30:00Z",
  "updated_at": "2025-03-01T12:00:00Z"
}
```

**Response `401 Unauthorized`**

```json
{
  "error": "unauthorized",
  "message": "Invalid or expired token."
}
```

---

## 4. 更新個人資料

### `PUT /api/v1/users/me`

更新已登入使用者的可編輯欄位。

**Headers**

```
Authorization: Bearer {access_token}
```

**Request Body**

```json
{
  "display_name": "New Display Name",
  "phone": "+886987654321",
  "bio": "Updated bio text."
}
```

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| display_name | string | 否 | 顯示名稱，1–50 字 |
| phone | string | 否 | 電話號碼 |
| bio | string | 否 | 個人簡介，最多 200 字 |

**Response `200 OK`**

回傳與 `GET /api/v1/users/me` 相同結構的完整使用者物件。

---

### 4.1 `PUT /api/v1/users/me/avatar`

上傳新頭像（Multipart）。

**Headers**

```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**Request Body**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| avatar | file | 是 | 圖片檔，建議 JPG/PNG，< 5 MB |

**Response `200 OK`**

```json
{
  "id": "usr_001",
  "email": "demo@example.com",
  "display_name": "Demo User",
  "avatar_url": "https://api.example.com/avatars/usr_001_1748250000000.png",
  "phone": "+886912345678",
  "bio": "Android MVVM sample user.",
  "created_at": "2024-01-15T08:30:00Z",
  "updated_at": "2025-05-26T12:00:00Z"
}
```

**Response `401 Unauthorized`**

```json
{
  "error": "unauthorized",
  "message": "Invalid or expired token."
}
```

> Mock：`MockApiInterceptor` 忽略檔案內容，回傳帶 timestamp 的 `avatar_url`，並同時更新 Room 快取。

---

## 5. 取得通知列表

### `GET /api/v1/notifications`

回傳目前使用者的通知列表（分頁），依 `created_at` 由新到舊排序。

**Headers**

```
Authorization: Bearer {access_token}
```

**Query 參數**

| 名稱 | 類型 | 必填 | 預設值 | 說明 |
|------|------|------|--------|------|
| page | number | 否 | 1 | 1-based 頁碼 |
| pageSize | number | 否 | 20 | 單頁筆數 |

**Response `200 OK`**

```json
{
  "items": [
    {
      "id": "ntf_001",
      "title": "歡迎使用本應用",
      "body": "感謝您下載使用，點擊查看新手導覽。",
      "type": "SYSTEM",
      "is_read": false,
      "created_at": 1748257200000
    },
    {
      "id": "ntf_002",
      "title": "限時優惠",
      "body": "升級會員享有 8 折優惠。",
      "type": "PROMOTION",
      "is_read": false,
      "created_at": 1748255400000
    }
  ],
  "next_page": 2,
  "has_more": true
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| items | array | 通知陣列 |
| items[].id | string | 通知唯一識別碼 |
| items[].title | string | 通知標題 |
| items[].body | string | 通知內容（純文字） |
| items[].type | string | 類型，允許 `SYSTEM` / `PROMOTION` / `ACTIVITY`，未知值由 Client Mapper 視為 `SYSTEM` |
| items[].is_read | boolean | 是否已讀 |
| items[].created_at | number | epoch millis |
| next_page | number\|null | 下一頁頁碼；若無下一頁則為 `null` |
| has_more | boolean | 是否尚有更多資料 |

Mock 階段固定回傳 6–7 筆 demo 資料，且 `created_at` 由 `System.currentTimeMillis()` 動態計算；若未帶 query 參數，視同 `page=1&pageSize=20`。

**Response `401 Unauthorized`**

```json
{ "error": "unauthorized", "message": "Invalid or expired token." }
```

---

## 6. 標記單筆通知為已讀

### `PATCH /api/v1/notifications/{id}/read`

**Headers**

```
Authorization: Bearer {access_token}
```

**Path 參數**

| 名稱 | 類型 | 必填 | 說明 |
|------|------|------|------|
| id | string | 是 | 通知識別碼 |

**Response `204 No Content`**

無 Body。Client 在收到 204 後將本地 Room 該筆 `is_read` 更新為 `true`。

**Response `401 Unauthorized`**

```json
{ "error": "unauthorized", "message": "Invalid or expired token." }
```

---

## 7. 全部標記為已讀

### `POST /api/v1/notifications/read-all`

**Headers**

```
Authorization: Bearer {access_token}
```

**Request Body**

無。

**Response `204 No Content`**

無 Body。Client 將本地所有 `is_read = false` 的紀錄改為 `true`。

**Response `401 Unauthorized`**

```json
{ "error": "unauthorized", "message": "Invalid or expired token." }
```

---

## 8. 錯誤格式（通用）

所有非 2xx 回應建議使用統一格式：

```json
{
  "error": "error_code",
  "message": "Human-readable message."
}
```

| HTTP 狀態 | error 代碼範例 | 說明 |
|-----------|----------------|------|
| 400 | bad_request | 請求格式錯誤 |
| 401 | unauthorized / invalid_credentials | 未授權或登入失敗 |
| 403 | forbidden | 無權限 |
| 404 | not_found | 資源不存在 |
| 422 | validation_error | 欄位驗證失敗 |
| 500 | internal_error | 伺服器錯誤 |

---

## 9. DLP 與安全要求

- 所有 API 必須透過 **HTTPS** 傳輸。
- **禁止** 在 Log、Crash Report 中記錄 `password`、`access_token`、`refresh_token`。
- Token 僅儲存於 **EncryptedSharedPreferences**（本專案 `EncryptedTokenStorage`）。
- PII（email、phone）在本地快取（Room）不加密儲存時，需限制 Log 輸出；生產環境建議評估 SQLCipher。

---

## 10. 註冊 FCM Token

### `POST /api/v1/push/register-token`

註冊或更新目前登入使用者的 FCM token 與裝置資訊。  
此 API 由 App 呼叫，需帶使用者 Bearer token。

**Headers**

```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**

```json
{
  "token": "fcm_token_string",
  "platform": "ANDROID",
  "device_id": "android_5f3d9b3f-1e4a-4f9f-a111-1f4ac8e7f001",
  "app_version": "1.0.0(1)",
  "locale": "zh-TW",
  "timezone": "Asia/Taipei"
}
```

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| token | string | 是 | FCM registration token |
| platform | string | 是 | 目前固定 `ANDROID` |
| device_id | string | 是 | App 產生且可長期識別同裝置的 ID |
| app_version | string | 否 | App 版本字串（觀測用） |
| locale | string | 否 | 例如 `zh-TW` |
| timezone | string | 否 | 例如 `Asia/Taipei` |

**Response `200 OK`**

```json
{
  "registration_id": "prg_01JY2Q8WFW1VVQ8P2M2M2M2M2M",
  "token_status": "ACTIVE",
  "updated_at": "2026-05-28T15:20:45Z"
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| registration_id | string | 裝置註冊紀錄 ID |
| token_status | string | `ACTIVE` / `INACTIVE` |
| updated_at | string | ISO 8601 UTC 時間 |

**規則與冪等**

- 同一 `(user_id, device_id)` 以 upsert 更新，不重複新增紀錄。
- 若同一 `token` 出現在其他裝置或帳號，舊紀錄應失效或轉移到最新註冊者。
- 可重複呼叫；相同資料重送應得到同一註冊狀態（冪等）。

**Response `401 Unauthorized`**

```json
{
  "error": "unauthorized",
  "message": "Invalid or expired token."
}
```

**Response `422 Unprocessable Entity`**

```json
{
  "error": "validation_error",
  "message": "token is required.",
  "fields": { "token": "must not be blank" }
}
```

---

## 11. 建立並發送通知（內部服務）

### `POST /api/v1/internal/notifications/send`

建立通知事件，寫入使用者通知收件匣，並將投遞工作送入非同步佇列。  
此 API 不對一般 App JWT 開放，僅供內部服務呼叫。

**Headers**

```
Authorization: Bearer {service_or_admin_token}
X-Idempotency-Key: {uuid-or-unique-key}
Content-Type: application/json
```

> `X-Idempotency-Key` 與 body 中 `idempotency_key` 建議二擇一；若兩者都提供，值需一致。

**Request Body**

```json
{
  "idempotency_key": "notif-20260528-order-5566",
  "type": "SYSTEM",
  "title": "訂單已出貨",
  "body": "您的訂單 #5566 已出貨，預計 2 天內送達。",
  "data": {
    "deep_link": "notifications",
    "order_id": "5566"
  },
  "targets": {
    "user_ids": ["usr_001", "usr_002"],
    "topic": "promo_weekend",
    "segments": []
  },
  "schedule_at": null
}
```

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| idempotency_key | string | 是 | 業務唯一鍵，避免重送重複發送 |
| type | string | 是 | 建議沿用 `SYSTEM` / `PROMOTION` / `ACTIVITY` |
| title | string | 是 | 通知標題 |
| body | string | 是 | 通知內容 |
| data | object | 否 | 自訂 payload（供 deep link 或業務識別） |
| targets.user_ids | string[] | 否 | 指定一或多位使用者 |
| targets.topic | string | 否 | 指定 topic 廣播 |
| targets.segments | array | 否 | 保留欄位，v1 可先不實作 |
| schedule_at | string\|null | 否 | ISO 8601，為空則立即送出 |

**目標限制**

- `targets` 至少需提供一種：`user_ids` 或 `topic`。
- 若同時提供，視為聯集投遞，收件者需去重。

**Response `202 Accepted`**

```json
{
  "notification_id": "nev_01JY2QFG9M9Z6N1N7Q4R6A3C55",
  "accepted_targets": 1250,
  "queued_jobs": 1250,
  "scheduled_at": null,
  "status": "QUEUED"
}
```

**Response `401 Unauthorized`**

```json
{
  "error": "unauthorized",
  "message": "Invalid service token."
}
```

**Response `403 Forbidden`**

```json
{
  "error": "forbidden",
  "message": "Insufficient scope for internal notifications."
}
```

**Response `409 Conflict`**

```json
{
  "error": "idempotency_conflict",
  "message": "A request with the same idempotency key already exists."
}
```

---

## 12. 記錄通知開啟事件

### `POST /api/v1/notifications/{id}/open`

記錄使用者「開啟通知」事件（例如點擊系統推播後進入 App）。  
此 API 補強 engagement 分析，不取代既有已讀 API。

**Headers**

```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path 參數**

| 名稱 | 類型 | 必填 | 說明 |
|------|------|------|------|
| id | string | 是 | 通知識別碼（對應 `user_notifications.id`） |

**Request Body**

```json
{
  "opened_at": 1748443800000,
  "source": "push_tap",
  "campaign_id": "cmp_202605_promo01"
}
```

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| opened_at | number | 否 | client epoch millis；未提供時以 server time 為準 |
| source | string | 否 | `push_tap` / `inbox_click`，預設 `push_tap` |
| campaign_id | string | 否 | 行銷活動識別碼 |

**Response `200 OK`**

```json
{
  "notification_id": "untf_01JY2QKQ6GNY3M7K2W6WE4EHJT",
  "opened": true,
  "opened_at": "2026-05-28T15:30:00Z"
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| opened | boolean | 是否首次記錄開啟；若重複上報則為 `false` |

**規則與冪等**

- 同一 `(user_id, notification_id)` 僅允許一筆 first-open 時間。
- 重複上報應回傳 `200` 且 `opened=false`（冪等成功）。
- 可與 `PATCH /api/v1/notifications/{id}/read` 並存；`open` 用於行為分析，`read` 用於 UI 狀態。

**Response `401 Unauthorized`**

```json
{
  "error": "unauthorized",
  "message": "Invalid or expired token."
}
```

**Response `404 Not Found`**

```json
{
  "error": "not_found",
  "message": "Notification not found for current user."
}
```
