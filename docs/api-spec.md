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
