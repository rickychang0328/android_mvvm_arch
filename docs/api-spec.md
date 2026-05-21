# API 規格書 — Auth & Profile

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

## 5. 錯誤格式（通用）

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

## 6. DLP 與安全要求

- 所有 API 必須透過 **HTTPS** 傳輸。
- **禁止** 在 Log、Crash Report 中記錄 `password`、`access_token`、`refresh_token`。
- Token 僅儲存於 **EncryptedSharedPreferences**（本專案 `EncryptedTokenStorage`）。
- PII（email、phone）在本地快取（Room）不加密儲存時，需限制 Log 輸出；生產環境建議評估 SQLCipher。
