# 循序圖 (Sequence Diagram)
# Android MVVM Architecture — Auth, Profile, Settings & Notifications

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

---

## 11. Settings 讀取流程（App 啟動 → DataStore → Theme）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant MA as MainActivity
    participant DS as SettingsDataStoreImpl
    participant Prefs as DataStore Preferences<br/>(app_settings)
    participant Theme as Android_mvvm_archTheme
    participant Nav as AppNavGraph

    User->>MA: 啟動 App (onCreate)
    activate MA
    MA->>DS: settingsFlow.collectAsStateWithLifecycle()
    activate DS
    DS->>Prefs: data.map { preferences }
    Prefs-->>DS: AppSettings(isDarkMode=false, language="zh-TW", ...)
    DS-->>MA: Flow emit AppSettings
    deactivate DS

    MA->>Theme: Android_mvvm_archTheme(darkTheme = appSettings.isDarkMode)
    activate Theme
    Theme-->>MA: MaterialTheme 套用 light / dark colorScheme
    deactivate Theme

    MA->>Nav: AppNavGraph(startDestination)
    Nav-->>User: 顯示 LoginScreen 或 ProfileScreen
    deactivate MA

    note over MA,Theme: 設定變更時 settingsFlow 自動 re-emit，<br/>Compose 重組並即時切換主題
```

---

## 12. Settings 更新流程（User toggle → ViewModel → DataStore → UI）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SS as SettingsScreen
    participant SVM as SettingsViewModel
    participant UC as UpdateDarkModeUseCase
    participant Repo as SettingsRepositoryImpl
    participant DS as SettingsDataStoreImpl
    participant Prefs as DataStore Preferences
    participant MA as MainActivity
    participant Theme as Android_mvvm_archTheme

    User->>SS: 切換「深色模式」Switch
    SS->>SVM: onIntent(DarkModeChanged(enabled=true))
    activate SVM

    SVM->>UC: invoke(enabled=true)
    activate UC
    UC->>Repo: updateDarkMode(true)
    activate Repo
    Repo->>DS: updateDarkMode(true)
    activate DS
    DS->>Prefs: edit { IS_DARK_MODE = true }
    Prefs-->>DS: 寫入完成
    deactivate DS
    deactivate Repo
    UC-->>SVM: Result.success(Unit)
    deactivate UC

    par SettingsScreen 更新
        DS->>SVM: settingsFlow emit AppSettings(isDarkMode=true)
        SVM-->>SS: uiState.isDarkMode = true
    and MainActivity 主題更新
        DS->>MA: settingsFlow emit AppSettings(isDarkMode=true)
        MA->>Theme: Android_mvvm_archTheme(darkTheme=true)
        Theme-->>User: 全 App 切換為深色主題
    end

    deactivate SVM
```

---

## 13. 清除快取流程（Privacy Settings）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SS as SettingsScreen
    participant Dialog as AlertDialog
    participant SVM as SettingsViewModel
    participant UC as ClearCacheUseCase
    participant PR as ProfileRepositoryImpl
    participant DAO as ProfileDao
    participant Snack as SnackbarHostState

    User->>SS: 點擊「清除快取」
    SS->>Dialog: showClearCacheDialog = true
    Dialog-->>User: 顯示 Material3 AlertDialog（確認 / 取消）

    alt 使用者點擊「確認」
        User->>Dialog: 點擊「確認」
        Dialog->>SS: showClearCacheDialog = false
        SS->>SVM: onIntent(ClearCache)
        activate SVM

        SVM->>UC: invoke()
        activate UC
        UC->>PR: clearProfileCache()
        activate PR
        PR->>DAO: clear() ← 清除 Profile Room 快取
        DAO-->>PR: Unit
        PR-->>UC: Unit
        deactivate PR
        UC-->>SVM: Result.success(Unit)
        deactivate UC

        note over SVM: Token 與 Settings 偏好皆保留<br/>不會觸發登出
        SVM->>SS: uiEvent emit CacheCleared
        deactivate SVM
        SS->>Snack: showSnackbar("已清除本地快取")
        Snack-->>User: 顯示 Snackbar 提示
    else 失敗
        UC-->>SVM: Result.failure(throwable)
        SVM->>SS: uiEvent emit ShowError("清除快取失敗，請稍後再試。")
        SS->>Snack: showSnackbar(message)
        Snack-->>User: 顯示錯誤 Snackbar
    else 使用者點擊「取消」
        User->>Dialog: 點擊「取消」
        Dialog->>SS: showClearCacheDialog = false
        note over SS: 不發出 Intent，UI 還原為設定頁
    end
```

---

## 14. Settings 語言切換（UseCase 驗證）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SS as SettingsScreen
    participant SVM as SettingsViewModel
    participant UC as UpdateLanguageUseCase
    participant Repo as SettingsRepositoryImpl
    participant DS as SettingsDataStoreImpl

    User->>SS: 選擇「English」SegmentedButton
    SS->>SVM: onIntent(LanguageChanged("en"))
    activate SVM

    SVM->>UC: invoke("en")
    activate UC
    UC->>UC: 驗證 language in {"zh-TW", "en"}
    UC->>Repo: updateLanguage("en")
    Repo->>DS: updateLanguage("en")
    DS->>DS: edit { LANGUAGE = "en" }
    UC-->>SVM: Result.success(Unit)
    deactivate UC

    DS-->>SVM: settingsFlow emit AppSettings(language="en")
    SVM-->>SS: uiState.language = "en"
    deactivate SVM
    SS-->>User: SegmentedButton 顯示 English 已選中
```

---

## 15. Notifications 列表載入與下拉刷新

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant PS as ProfileScreen
    participant NS as NotificationsScreen
    participant NVM as NotificationsViewModel
    participant GetUC as GetNotificationsUseCase
    participant RefUC as RefreshNotificationsUseCase
    participant NR as NotificationsRepositoryImpl
    participant DAO as NotificationDao
    participant API as NotificationsApi
    participant Mock as MockApiInterceptor

    User->>PS: 點擊鈴鐺 IconButton（含未讀 Badge）
    PS->>NS: navigate(Routes.NOTIFICATIONS)
    NS->>NVM: hiltViewModel()
    activate NVM

    par 訂閱本地 Room Flow
        NVM->>GetUC: invoke()
        GetUC->>NR: observeNotifications()
        NR->>DAO: observeAll() (Flow)
        DAO-->>NR: Flow<List<NotificationEntity>>
        NR-->>NVM: Flow<List<Notification>>
        NVM-->>NS: uiState.items 更新（先顯示快取，可能為空）
    and 初次刷新
        NVM->>RefUC: invoke()
        RefUC->>NR: refresh()
        NR->>API: getNotifications()
        API->>Mock: 攔截 GET /api/v1/notifications
        Mock-->>API: 200 NotificationsResponseDto (7 筆 demo)
        API-->>NR: NotificationsResponseDto
        NR->>DAO: upsertAll(entities) ← 寫入快取
        DAO-->>NVM: Flow 自動 re-emit
        NR-->>RefUC: Result.success(Unit)
        RefUC-->>NVM: Result.success(Unit)
        NVM-->>NS: uiState.isLoading = false
    end

    deactivate NVM
    NS-->>User: 顯示通知列表 + 未讀小圓點

    note over User,NS: 使用者下拉刷新
    User->>NS: 下拉 PullToRefreshBox
    NS->>NVM: onIntent(Refresh)
    activate NVM
    NVM-->>NS: uiState.isRefreshing = true
    NVM->>RefUC: invoke() (同上流程)
    RefUC-->>NVM: Result.success(Unit)
    NVM-->>NS: uiState.isRefreshing = false
    deactivate NVM
```

---

## 16. 標記單筆通知為已讀（樂觀更新）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant NS as NotificationsScreen
    participant NVM as NotificationsViewModel
    participant UC as MarkNotificationReadUseCase
    participant NR as NotificationsRepositoryImpl
    participant API as NotificationsApi
    participant Mock as MockApiInterceptor
    participant DAO as NotificationDao

    User->>NS: 點擊未讀通知列
    NS->>NVM: onIntent(MarkRead("ntf_002"))
    activate NVM

    NVM->>UC: invoke("ntf_002")
    activate UC
    UC->>UC: 驗證 id.isNotBlank()
    UC->>NR: markAsRead("ntf_002")
    activate NR
    NR->>API: PATCH /api/v1/notifications/ntf_002/read
    activate API
    API->>Mock: 攔截 PATCH
    Mock-->>API: 204 No Content
    deactivate API
    API-->>NR: Unit
    NR->>DAO: markAsRead("ntf_002") ← UPDATE isRead = 1
    DAO-->>NVM: Flow 自動 re-emit（含已讀狀態）
    NR-->>UC: Result.success(Unit)
    deactivate NR
    UC-->>NVM: Result.success(Unit)
    deactivate UC
    deactivate NVM
    NS-->>User: 該列未讀小圓點消失、字重變正常

    note over NS,NVM: 失敗路徑：API 回 4xx/5xx<br/>UC 回 Result.failure → uiEvent emit ShowError(message)<br/>Snackbar 顯示「標記為已讀失敗」+ 重試按鈕
```

---

## 17. 全部標記為已讀

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant NS as NotificationsScreen
    participant NVM as NotificationsViewModel
    participant UC as MarkAllNotificationsReadUseCase
    participant NR as NotificationsRepositoryImpl
    participant API as NotificationsApi
    participant Mock as MockApiInterceptor
    participant DAO as NotificationDao
    participant Snack as SnackbarHostState

    User->>NS: 點擊 TopAppBar "全部標記為已讀"
    NS->>NVM: onIntent(MarkAllRead)
    activate NVM

    NVM->>UC: invoke()
    activate UC
    UC->>NR: markAllAsRead()
    activate NR
    NR->>API: POST /api/v1/notifications/read-all
    API->>Mock: 攔截 POST
    Mock-->>API: 204 No Content
    API-->>NR: Unit
    NR->>DAO: markAllAsRead() ← UPDATE WHERE isRead = 0
    DAO-->>NVM: Flow 自動 re-emit
    NR-->>UC: Result.success(Unit)
    deactivate NR
    UC-->>NVM: Result.success(Unit)
    deactivate UC

    NVM->>NS: uiEvent emit AllMarkedRead
    deactivate NVM
    NS->>Snack: showSnackbar("已全部標記為已讀")
    Snack-->>User: 顯示 Snackbar，所有未讀圓點消失
```

---

## 18. WorkManager 背景同步 + 系統通知

```mermaid
sequenceDiagram
    autonumber
    participant WM as WorkManager
    participant W as NotificationSyncWorker
    participant DS as SettingsDataStore
    participant GetUC as GetNotificationsUseCase
    participant RefUC as RefreshNotificationsUseCase
    participant NR as NotificationsRepositoryImpl
    participant API as NotificationsApi
    participant DAO as NotificationDao
    participant Helper as NotificationHelper
    participant Sys as Android NotificationManager
    actor User

    note over WM,W: Application.onCreate 排程<br/>enqueueUniquePeriodicWork(<br/>  name = "notification_sync",<br/>  policy = KEEP,<br/>  interval = 15 min<br/>)

    WM->>W: 每 15 分鐘執行 doWork()
    activate W

    W->>DS: settingsFlow.first().notificationsEnabled
    DS-->>W: Boolean

    alt notificationsEnabled == false
        W-->>WM: Result.success()
        note right of W: 直接跳過本次同步，<br/>不打 API、不顯示系統通知
    else 啟用
        W->>GetUC: invoke().first()
        GetUC->>NR: observeNotifications()
        NR->>DAO: observeAll()
        DAO-->>W: 計算 previousUnreadIds: Set<String>

        W->>RefUC: invoke()
        RefUC->>NR: refresh()
        NR->>API: getNotifications()
        API-->>NR: NotificationsResponseDto
        NR->>DAO: upsertAll(entities)
        NR-->>RefUC: Result.success
        RefUC-->>W: Result.success

        W->>GetUC: invoke().first() (再次讀取最新清單)
        GetUC-->>W: latestUnread: List<Notification>
        W->>W: newUnread = latestUnread \\ previousUnreadIds

        loop 最多前 3 筆新通知
            W->>Helper: showNotification(notification)
            Helper->>Sys: NotificationManagerCompat.notify(...)
            Sys-->>User: 顯示系統通知<br/>（標題、內文、Big Text）
        end

        W-->>WM: Result.success()
    end
    deactivate W

    note over User,Sys: 使用者點擊系統通知
    User->>Sys: tap
    Sys->>Sys: PendingIntent 啟動 MainActivity<br/>(singleTask, EXTRA_DEEP_LINK="notifications")
    Sys-->>User: 開啟 App → NotificationsScreen
```

---

## 19. 系統通知 deep link 導航流程

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Sys as Android NotificationManager
    participant MA as MainActivity
    participant Nav as NavController
    participant NS as NotificationsScreen

    User->>Sys: 點擊系統通知
    Sys->>MA: PendingIntent.getActivity()<br/>(singleTask, EXTRA_DEEP_LINK="notifications")
    activate MA

    alt 應用程式已執行
        MA->>MA: onNewIntent(intent)
        MA->>MA: setIntent(intent) + consumeDeepLink(intent)
    else 冷啟動
        MA->>MA: onCreate() → consumeDeepLink(intent)
    end

    MA->>MA: pendingDeepLink.value = "notifications"
    MA->>MA: LaunchedEffect 觀察 deepLink + startDestination

    alt startDestination != LOGIN
        MA->>Nav: navigate(Routes.NOTIFICATIONS, launchSingleTop = true)
        Nav->>NS: 顯示通知列表
        NS-->>User: 看到該通知對應的列表頁
        MA->>MA: pendingDeepLink.value = null
    else 未登入
        note over MA,Nav: 未登入時暫不導航，<br/>避免繞過登入流程；<br/>使用者登入後可從入口進入
    end
    deactivate MA
```

---

## 20. 更換頭像（相簿 / 相機）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant PS as ProfileScreen
    participant PVM as ProfileViewModel
    participant UploadUC as UploadAvatarUseCase
    participant PR as ProfileRepositoryImpl
    participant API as ProfileApi
    participant Mock as MockApiInterceptor
    participant DAO as ProfileDao

    User->>PS: 點擊頭像編輯 IconButton
    PS->>PS: ModalBottomSheet（相簿 / 相機）

    alt 選擇相簿
        PS->>PS: 檢查 READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE 權限
        PS->>PS: ActivityResultContracts.StartActivityForResult(Intent.ACTION_PICK)
        PS-->>User: 選擇圖片 Uri
        PS->>PS: copyUriToCache(uri) → File
    else 選擇相機
        PS->>PS: 檢查 CAMERA + 讀取圖片權限
        PS->>PS: FileProvider("${applicationId}.fileprovider") 建立 cache File + Uri
        PS->>PS: ActivityResultContracts.StartActivityForResult(MediaStore.ACTION_IMAGE_CAPTURE, EXTRA_OUTPUT=uri)
        PS-->>User: 拍照完成，得到 File
    end

    PS->>PVM: onIntent(UploadAvatar(file))
    activate PVM
    PVM->>UploadUC: invoke(file)
    activate UploadUC
    UploadUC->>PR: uploadAvatar(file)
    activate PR
    PR->>API: PUT /api/v1/users/me/avatar (Multipart avatar)
    API->>Mock: 攔截並回傳新的 avatar_url（timestamp）
    Mock-->>API: 200 UserProfileDto
    PR->>DAO: upsert(ProfileEntity) ← 更新 Room 快取
    PR-->>UploadUC: Result.success(UserProfile)
    deactivate PR
    UploadUC-->>PVM: Result.success(UserProfile)
    deactivate UploadUC
    PVM-->>PS: uiState.isUploadingAvatar = false, profile 更新
    PVM-->>PS: uiEvent ShowMessage("頭像已更新")
    deactivate PVM
    PS-->>User: 頭像即時更新（Coil）+ Snackbar
```

---

## 21. Notifications 分頁載入流程（Paging 3）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant NS as NotificationsScreen
    participant NVM as NotificationsViewModel
    participant LP as LazyPagingItems
    participant UC as GetNotificationsPagingUseCase
    participant Repo as NotificationsRepositoryImpl
    participant PS as NotificationsPagingSource
    participant API as NotificationsApi
    participant Mock as MockApiInterceptor
    participant DAO as NotificationDao

    User->>NS: 進入通知頁
    NS->>NVM: collect pagingDataFlow
    NVM->>UC: invoke(pageSize=20)
    UC->>Repo: getNotificationsPagingData(20)
    Repo-->>NVM: Flow<PagingData<Notification>>
    NVM-->>NS: pagingDataFlow.cachedIn(viewModelScope)
    NS->>LP: collectAsLazyPagingItems()

    LP->>PS: load(page=1, pageSize=20)
    PS->>API: GET /api/v1/notifications?page=1&pageSize=20
    API->>Mock: 攔截並分頁切片
    Mock-->>API: {items, next_page, has_more}
    API-->>PS: NotificationsResponseDto
    PS->>DAO: upsertAll(loadedPageEntities)
    PS-->>LP: LoadResult.Page(data, nextKey)
    LP-->>NS: render list + refresh/append loadState

    opt 使用者滾動到底部
        LP->>PS: load(page=next_page)
        PS->>API: GET /api/v1/notifications?page=n&pageSize=20
        API-->>PS: 下一頁資料
        PS-->>LP: LoadResult.Page(...)
        LP-->>NS: append 成功或錯誤（可 retry）
    end
```

---

## 22. 標記已讀後刷新流程（Paging 3）

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant NS as NotificationsScreen
    participant NVM as NotificationsViewModel
    participant MarkUC as MarkNotificationReadUseCase
    participant Repo as NotificationsRepositoryImpl
    participant API as NotificationsApi
    participant Mock as MockApiInterceptor
    participant DAO as NotificationDao
    participant LP as LazyPagingItems

    User->>NS: 點擊未讀通知
    NS->>NVM: onIntent(MarkRead(id))
    NVM->>MarkUC: invoke(id)
    MarkUC->>Repo: markAsRead(id)
    Repo->>API: PATCH /api/v1/notifications/{id}/read
    API->>Mock: 更新 mock 通知狀態為已讀
    Mock-->>API: 204 No Content
    Repo->>DAO: markAsRead(id)
    Repo-->>MarkUC: Result.success(Unit)
    MarkUC-->>NVM: success
    NVM-->>NS: uiEvent.RefreshList
    NS->>LP: refresh()
    LP->>API: 重新載入第一頁
    API-->>LP: 最新 is_read 狀態
    LP-->>NS: UI 已讀狀態即時更新
```
