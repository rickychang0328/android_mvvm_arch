# 循序圖 (Sequence Diagram)
# Android MVVM Architecture — Auth & Profile

> 使用 [Mermaid](https://mermaid.js.org/) 語法繪製，可在 GitHub、GitLab、Markdown 預覽工具中直接渲染。

---

## 1. 應用程式啟動 — 路由決策

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant MA as MainActivity
    participant MVM as MainViewModel
    participant UC as IsLoggedInUseCase
    participant TS as TokenStorage
    participant Nav as AppNavGraph

    User->>MA: 啟動 App (onCreate)
    MA->>MVM: hiltViewModel()
    activate MVM
    MVM->>UC: invoke()
    activate UC
    UC->>TS: hasAccessToken()
    TS-->>UC: true / false
    UC-->>MVM: Boolean
    deactivate UC
    MVM-->>MA: startDestination = "profile" / "login"
    deactivate MVM
    MA->>Nav: AppNavGraph(startDestination)
    alt 已登入
        Nav-->>User: 顯示 ProfileScreen
    else 未登入
        Nav-->>User: 顯示 LoginScreen
    end
```

---

## 2. 使用者登入（成功路徑）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant LS as LoginScreen
    participant LVM as LoginViewModel
    participant UC as LoginUseCase
    participant AR as AuthRepositoryImpl
    participant API as AuthApi (Retrofit)
    participant Mock as MockApiInterceptor
    participant TS as TokenStorage
    participant PR as ProfileRepositoryImpl
    participant PAPI as ProfileApi
    participant DAO as ProfileDao

    User->>LS: 輸入 Email & Password
    LS->>LVM: onIntent(EmailChanged / PasswordChanged)
    LVM-->>LS: uiState 更新（email, password）

    User->>LS: 點擊「登入」
    LS->>LVM: onIntent(SubmitLogin)
    activate LVM
    LVM-->>LS: uiState.isLoading = true

    LVM->>UC: invoke(email, password)
    activate UC
    UC->>UC: 驗證 email 格式、password 不為空
    UC->>AR: login(LoginCredentials)
    activate AR

    AR->>API: login(LoginRequestDto)
    activate API
    API->>Mock: 攔截 POST /api/v1/auth/login
    Mock-->>API: 200 LoginResponseDto (mock token)
    deactivate API

    AR->>TS: saveAccessToken("mock_access_token_demo")
    AR->>TS: saveRefreshToken("mock_refresh_token_demo")

    AR->>PR: refreshProfile()
    activate PR
    PR->>PAPI: getProfile()
    activate PAPI
    PAPI->>Mock: 攔截 GET /api/v1/users/me
    Mock-->>PAPI: 200 UserProfileDto
    deactivate PAPI
    PR->>DAO: upsert(ProfileEntity)
    PR-->>AR: Result.success(UserProfile)
    deactivate PR

    AR-->>UC: Result.success(AuthTokens)
    deactivate AR
    UC-->>LVM: Result.success(AuthTokens)
    deactivate UC

    LVM-->>LS: uiState.isLoading = false
    LVM-->>LS: uiEvent emit NavigateToProfile
    deactivate LVM
    LS->>LS: LaunchedEffect 收到 NavigateToProfile
    LS-->>User: 導航至 ProfileScreen
```

---

## 3. 使用者登入（失敗路徑）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant LS as LoginScreen
    participant LVM as LoginViewModel
    participant UC as LoginUseCase
    participant AR as AuthRepositoryImpl
    participant API as AuthApi
    participant Mock as MockApiInterceptor

    User->>LS: 輸入錯誤帳密並點擊「登入」
    LS->>LVM: onIntent(SubmitLogin)
    activate LVM
    LVM-->>LS: uiState.isLoading = true

    LVM->>UC: invoke(email, password)
    activate UC
    UC->>AR: login(LoginCredentials)
    activate AR

    AR->>API: login(LoginRequestDto)
    activate API
    API->>Mock: 攔截 POST /api/v1/auth/login
    Mock-->>API: 401 Unauthorized
    deactivate API

    API-->>AR: HttpException(401)
    AR->>AR: safeApiCall 捕捉 HttpException
    AR->>AR: 解析 errorBody → ApiErrorDto
    AR-->>UC: Result.failure(ApiException(401, "Email or password is incorrect."))
    deactivate AR
    UC-->>LVM: Result.failure(ApiException)
    deactivate UC

    LVM-->>LS: uiState.isLoading = false
    LVM-->>LS: uiState.errorMessage = "Email or password is incorrect."
    deactivate LVM
    LS-->>User: 顯示錯誤訊息（紅字）

    note over LS,LVM: 使用者修改輸入後，ErrorMessage 清除
    User->>LS: 修改 Email 或 Password
    LS->>LVM: onIntent(EmailChanged / PasswordChanged)
    LVM-->>LS: uiState.errorMessage = null
```

---

## 4. 輸入驗證失敗（前端）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant LS as LoginScreen
    participant LVM as LoginViewModel
    participant UC as LoginUseCase

    User->>LS: 輸入無效 Email（例如 "notanemail"）並點擊「登入」
    LS->>LVM: onIntent(SubmitLogin)
    activate LVM
    LVM->>UC: invoke("notanemail", "password")
    activate UC
    UC->>UC: Regex 驗證失敗
    UC-->>LVM: Result.failure(IllegalArgumentException("Invalid email format."))
    deactivate UC
    LVM-->>LS: uiState.errorMessage = "Invalid email format."
    LVM-->>LS: uiState.isLoading = false（不會設為 true）
    deactivate LVM
    LS-->>User: 顯示驗證錯誤訊息
    note over LVM: 完全本地驗證，不發出網路請求
```

---

## 5. 個人資料頁面載入

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant PS as ProfileScreen
    participant PVM as ProfileViewModel
    participant GetUC as GetUserProfileUseCase
    participant PR as ProfileRepositoryImpl
    participant DAO as ProfileDao
    participant PAPI as ProfileApi
    participant Mock as MockApiInterceptor

    note over PS, DAO: ViewModel 初始化（init block）

    PS->>PVM: hiltViewModel()
    activate PVM

    par 訂閱本地 Flow
        PVM->>GetUC: observeProfile()
        GetUC->>PR: observeProfile()
        PR->>DAO: observeProfile() → Flow
        DAO-->>PR: Flow<ProfileEntity?>
        PR-->>GetUC: Flow<UserProfile?>
        GetUC-->>PVM: Flow<UserProfile?>
        PVM-->>PS: uiState.profile 更新（Room 快取）
    and 刷新遠端資料
        PVM->>GetUC: refresh()
        GetUC->>PR: refreshProfile()
        PR->>PAPI: getProfile()
        activate PAPI
        PAPI->>Mock: 攔截 GET /api/v1/users/me
        Mock-->>PAPI: 200 UserProfileDto
        deactivate PAPI
        PR->>DAO: upsert(ProfileEntity) ← 寫入快取
        DAO-->>PVM: Flow 自動觸發新值
        PVM-->>PS: uiState.profile 更新（最新資料）
        PVM-->>PS: uiState.isLoading = false
    end

    deactivate PVM
    PS-->>User: 顯示個人資料
```

---

## 6. 編輯並儲存個人資料

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant PS as ProfileScreen
    participant PVM as ProfileViewModel
    participant UC as UpdateUserProfileUseCase
    participant PR as ProfileRepositoryImpl
    participant PAPI as ProfileApi
    participant Mock as MockApiInterceptor
    participant DAO as ProfileDao

    User->>PS: 點擊「編輯」
    PS->>PVM: onIntent(StartEditing)
    PVM-->>PS: uiState.isEditing = true（顯示輸入欄位）

    User->>PS: 修改 displayName / phone / bio
    PS->>PVM: onIntent(DisplayNameChanged / PhoneChanged / BioChanged)
    PVM-->>PS: uiState 欄位更新

    User->>PS: 點擊「儲存」
    PS->>PVM: onIntent(SaveProfile)
    activate PVM
    PVM-->>PS: uiState.isSaving = true

    PVM->>UC: invoke(displayName, phone, bio)
    activate UC
    UC->>UC: 驗證 displayName 不為空、≤ 50 字
    UC->>UC: 驗證 bio ≤ 200 字
    UC->>PR: updateProfile(ProfileUpdate)
    activate PR

    PR->>PAPI: updateProfile(UpdateProfileRequestDto)
    activate PAPI
    PAPI->>Mock: 攔截 PUT /api/v1/users/me
    Mock-->>PAPI: 200 UserProfileDto（更新後資料）
    deactivate PAPI

    PR->>DAO: upsert(ProfileEntity) ← 同步更新快取
    PR-->>UC: Result.success(UserProfile)
    deactivate PR
    UC-->>PVM: Result.success(UserProfile)
    deactivate UC

    PVM-->>PS: uiState.isSaving = false
    PVM-->>PS: uiState.isEditing = false
    PVM-->>PS: uiState.successMessage = "Profile updated."
    deactivate PVM
    PS-->>User: 顯示成功提示，回到檢視模式
```

---

## 7. 編輯個人資料失敗（驗證錯誤）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant PS as ProfileScreen
    participant PVM as ProfileViewModel
    participant UC as UpdateUserProfileUseCase

    User->>PS: 清空 displayName 後點擊「儲存」
    PS->>PVM: onIntent(SaveProfile)
    activate PVM

    PVM->>UC: invoke("", phone, bio)
    activate UC
    UC->>UC: displayName.isBlank() → true
    UC-->>PVM: Result.failure(IllegalArgumentException("Display name cannot be empty."))
    deactivate UC

    PVM-->>PS: uiState.isSaving = false
    PVM-->>PS: uiState.errorMessage = "Display name cannot be empty."
    deactivate PVM
    PS-->>User: 顯示驗證錯誤，留在編輯模式
```

---

## 8. 使用者登出

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant PS as ProfileScreen
    participant PVM as ProfileViewModel
    participant LogUC as LogoutUseCase
    participant AR as AuthRepositoryImpl
    participant API as AuthApi
    participant Mock as MockApiInterceptor
    participant TS as TokenStorage
    participant PR as ProfileRepositoryImpl
    participant DAO as ProfileDao
    participant Nav as NavController

    User->>PS: 點擊「登出」
    PS->>PVM: onIntent(Logout)
    activate PVM

    PVM->>LogUC: invoke()
    activate LogUC
    LogUC->>AR: logout()
    activate AR

    AR->>API: logout()
    activate API
    API->>Mock: 攔截 POST /api/v1/auth/logout
    Mock-->>API: 204 No Content
    deactivate API

    AR->>TS: clearTokens() ← 清除 Access + Refresh Token
    AR->>PR: clearProfileCache()
    PR->>DAO: clear() ← 清除 Room 快取

    AR-->>LogUC: Result.success(Unit)
    deactivate AR
    LogUC-->>PVM: Result.success(Unit)
    deactivate LogUC

    PVM-->>PS: uiEvent emit NavigateToLogin
    deactivate PVM

    PS->>Nav: navigate("login", popUpTo "profile" inclusive=true)
    Nav-->>User: 顯示 LoginScreen（返回鍵無法回到 Profile）
```

---

## 9. Token 自動附加（AuthInterceptor）

```mermaid
sequenceDiagram
    autonumber
    participant VM as ViewModel / Repository
    participant OkHttp as OkHttpClient
    participant AI as AuthInterceptor
    participant Mock as MockApiInterceptor
    participant TS as EncryptedTokenStorage

    VM->>OkHttp: 發起 API 請求（無 Authorization header）
    activate OkHttp

    OkHttp->>Mock: 先執行 MockApiInterceptor
    note right of Mock: MockApiInterceptor 在前，<br/>模擬回應後直接返回，<br/>AuthInterceptor 不再執行
    Mock-->>OkHttp: 模擬回應（包含 Authorization 驗證）

    note over OkHttp, AI: 真實後端情境：MockApiInterceptor 移除後的流程

    OkHttp->>AI: 執行 AuthInterceptor
    activate AI
    AI->>TS: getAccessToken()
    TS-->>AI: "Bearer eyJ..."
    AI->>AI: 注入 Authorization header
    AI-->>OkHttp: 帶 Authorization 的請求
    deactivate AI

    OkHttp-->>VM: Response
    deactivate OkHttp
```

---

## 10. safeApiCall 錯誤包裝流程

```mermaid
sequenceDiagram
    autonumber
    participant Repo as RepositoryImpl
    participant Safe as safeApiCall
    participant API as Retrofit API
    participant Moshi as Moshi ErrorParser

    Repo->>Safe: safeApiCall { api.someCall() }
    activate Safe

    Safe->>API: suspend 呼叫
    alt 成功
        API-->>Safe: Response 物件
        Safe-->>Repo: Result.success(data)
    else HttpException
        API-->>Safe: HttpException(code)
        Safe->>Moshi: 解析 errorBody → ApiErrorDto
        Moshi-->>Safe: message: String?
        Safe-->>Repo: Result.failure(ApiException(code, message))
    else IOException / 其他
        API-->>Safe: Exception
        Safe-->>Repo: Result.failure(原始 Exception)
    end

    deactivate Safe
```
