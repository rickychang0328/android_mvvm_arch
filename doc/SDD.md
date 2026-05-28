# Software Design Document (SDD)
# Android MVVM Architecture — Auth, Profile, Settings & Notifications Feature

**專案名稱：** android_mvvm_arch  
**套件名稱：** `com.example.android_mvvm_arch`  
**版本：** 1.0.0  
**架構風格：** Clean Architecture + MVVM + MVI  
**語言：** Kotlin  
**日期：** 2026-05-28  

---

## 目錄

1. [專案概述](#1-專案概述)
2. [架構總覽](#2-架構總覽)
3. [模組分層說明](#3-模組分層說明)
   - 3.1 [Domain Layer](#31-domain-layer)
   - 3.2 [Data Layer](#32-data-layer)
   - 3.3 [Presentation Layer](#33-presentation-layer)
   - 3.4 [Core 模組](#34-core-模組)
   - 3.5 [DI 模組](#35-di-模組)
4. [目錄結構](#4-目錄結構)
5. [技術棧與依賴版本](#5-技術棧與依賴版本)
6. [功能模組設計](#6-功能模組設計)
   - 6.1 [Auth 登入功能](#61-auth-登入功能)
   - 6.2 [Profile 個人資料功能](#62-profile-個人資料功能)
   - 6.3 [Settings 設定功能](#63-settings-設定功能)
   - 6.4 [Notifications 通知功能](#64-notifications-通知功能)
   - 6.5 [HomeDashboard 主頁功能](#65-homedashboard-主頁功能)
   - 6.6 [Paging 3 列表模板](#66-paging-3-列表模板)
   - 6.7 [FCM 推播整合](#67-fcm-推播整合)
7. [安全設計（DLP）](#7-安全設計dlp)
8. [錯誤處理策略](#8-錯誤處理策略)
9. [導航設計](#9-導航設計)
10. [測試策略](#10-測試策略)
11. [Offline-First Sync Strategy（SyncManager + WorkManager）](#11-offline-first-sync-strategysyncmanager--workmanager)

---

## 1. 專案概述

本專案為 Android 行動應用程式，示範以 **Clean Architecture** 搭配 **MVVM + MVI** 風格實作「登入 / 登出」、「Home Dashboard + Drawer 主導航」、「個人資料檢視 / 編輯」、「應用程式設定（深色模式、語言、通知）」與「FCM 推播整合（登入後上報 Token）」等功能模組。所有網路呼叫透過 `MockApiInterceptor` 於本地模擬，可無需後端即獨立運行。

### 核心目標

| 目標 | 說明 |
|------|------|
| **可測試性** | 每層以介面隔離，便於 Unit Test 使用 MockK 替換 |
| **可維護性** | 功能依 feature 分包，各層職責明確 |
| **安全性** | Token 加密儲存，敏感資料不寫入 Log（DLP） |
| **可擴充性** | Domain 層不依賴 Android，可直接移植至多平台 |

### 開發者閱讀路徑

1. 先讀 `doc/SDD.md` 的章節 6（功能）與章節 9（導航）。
2. 再讀 `doc/sequence-diagram.md` 對應流程，掌握操作時序。
3. 接著讀 `doc/class-diagram.md` 對應章節，掌握類別責任與依賴。
4. 最後對照 `app/src/main/java/com/example/android_mvvm_arch/navigation` 與各 `feature/*/presentation/ui` 進行實作。

---

## 2. 架構總覽

```
┌─────────────────────────────────────────┐
│          Presentation Layer             │
│  Composable UI ← ViewModel ← UseCase   │
│  (StateFlow / SharedFlow / MVI Intent) │
└──────────────────┬──────────────────────┘
                   │  invoke UseCases only
┌──────────────────▼──────────────────────┐
│             Domain Layer                │
│   Models · Repository Interfaces        │
│   UseCases (pure Kotlin, no android.*)  │
└──────────────────┬──────────────────────┘
                   │  implements interfaces
┌──────────────────▼──────────────────────┐
│              Data Layer                 │
│  RepositoryImpl · Remote API · Room DAO │
│  DTOs · Mappers · SettingsDataStore     │
│  EncryptedTokenStorage（Token 專用）     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│              Core 模組                  │
│  core/fcm/      FcmService              │
│    ├─ onNewToken → registerFcmToken     │
│    └─ onMessageReceived → showNotif.    │
│  core/notification/  NotificationHelper │
│  core/network/       MockApiInterceptor │
└─────────────────────────────────────────┘
```

**依賴方向：** `Presentation → Domain ← Data`（嚴格向內依賴）  
**FCM 服務層**：`FcmService`（`core/fcm/`）為 Android 系統元件，可直接依賴 Domain 層的 Repository 介面（透過 Hilt 注入）

---

## 3. 模組分層說明

### 3.1 Domain Layer

Domain 層是應用程式的核心，**不得引用任何 `android.*` 套件**。

#### 元件

| 類別 | 位置 | 職責 |
|------|------|------|
| `LoginCredentials` | `feature/auth/domain/model` | 封裝登入輸入（email, password） |
| `AuthTokens` | `feature/auth/domain/model` | 封裝登入成功回傳的 Token |
| `UserProfile` | `feature/profile/domain/model` | 使用者個人資料業務模型 |
| `ProfileUpdate` | `feature/profile/domain/model` | 更新個人資料的輸入模型 |
| `AuthRepository` | `feature/auth/domain/repo` | 登入 / 登出 / 狀態查詢介面 |
| `ProfileRepository` | `feature/profile/domain/repo` | 個人資料 CRUD + 本地快取介面 |
| `LoginUseCase` | `feature/auth/domain/usecase` | 驗證輸入並呼叫 AuthRepository.login |
| `LogoutUseCase` | `feature/auth/domain/usecase` | 呼叫 AuthRepository.logout |
| `IsLoggedInUseCase` | `feature/auth/domain/usecase` | 查詢是否已登入 |
| `RegisterFcmTokenUseCase` | `feature/auth/domain/usecase` | 取得 Firebase FCM Token，呼叫 AuthRepository.registerFcmToken 上報至後端 |
| `GetUserProfileUseCase` | `feature/profile/domain/usecase` | 訂閱 / 刷新個人資料 |
| `UpdateUserProfileUseCase` | `feature/profile/domain/usecase` | 驗證欄位並呼叫 ProfileRepository.update |
| `UploadAvatarUseCase` | `feature/profile/domain/usecase` | 驗證檔案存在後呼叫 ProfileRepository.uploadAvatar（Multipart） |

#### Use Case 設計原則

- 每個 Use Case 單一職責（SRP）
- 輸入驗證（email 格式、bio 字數限制）在 Use Case 內完成
- Dispatcher 由建構子注入，不硬編碼 `Dispatchers.IO`
- 回傳 `Result<T>`，呼叫端決定如何呈現錯誤

### 3.2 Data Layer

Data 層實作 Domain 介面，處理網路與本地資料。

#### 元件

| 類別 | 位置 | 職責 |
|------|------|------|
| `AuthApi` | `feature/auth/data/remote` | Retrofit 介面，定義登入 / 登出 / FCM Token 上報端點 |
| `ProfileApi` | `feature/profile/data/remote` | Retrofit 介面，定義個人資料端點（get / update / uploadAvatar Multipart） |
| `LoginRequestDto` | `feature/auth/data/remote/dto` | 登入請求 DTO |
| `RegisterFcmTokenRequestDto` | `feature/auth/data/remote/dto` | FCM Token 上報請求 DTO（`fcm_token`: String、`platform`: String = "android"） |
| `LoginResponseDto` | `feature/auth/data/remote/dto` | 登入回應 DTO |
| `UserProfileDto` | `feature/profile/data/remote/dto` | 個人資料回應 DTO |
| `UpdateProfileRequestDto` | `feature/profile/data/remote/dto` | 更新個人資料請求 DTO |
| `ApiErrorDto` | `feature/auth/data/remote/dto` | 通用錯誤回應 DTO |
| `AuthMapper` | `feature/auth/data/mapper` | DTO ↔ Domain Model 轉換 |
| `ProfileMapper` | `feature/profile/data/mapper` | DTO / Entity ↔ Domain Model 轉換 |
| `ProfileEntity` | `feature/profile/data/local` | Room Entity |
| `ProfileDao` | `feature/profile/data/local` | Room DAO（observe / upsert / clear） |
| `AuthRepositoryImpl` | `feature/auth/data/repo` | 實作 AuthRepository |
| `ProfileRepositoryImpl` | `feature/profile/data/repo` | 實作 ProfileRepository |
| `file_paths.xml` | `app/src/main/res/xml` | FileProvider cache 路徑，提供相機輸出 URI |
| `EncryptedTokenStorage` | `core/security` | Token 加密儲存（EncryptedSharedPreferences） |
| `AppSettings` | `core/datastore` | 應用程式設定資料模型 |
| `SettingsDataStore` | `core/datastore` | DataStore Preferences 存取介面 |
| `SettingsDataStoreImpl` | `core/datastore` | DataStore 實作（檔名：`app_settings`） |
| `SettingsRepositoryImpl` | `feature/settings/data/repo` | 委派 SettingsDataStore，實作 Domain 介面 |

#### 本地儲存分工

| 儲存機制 | 用途 | 敏感等級 |
|----------|------|----------|
| `EncryptedSharedPreferences`（`EncryptedTokenStorage`） | Access / Refresh Token | 高（加密） |
| `DataStore Preferences`（`SettingsDataStore`） | 深色模式、語言、通知開關 | 低（非敏感偏好） |
| Room（`ProfileDao`） | 個人資料離線快取 | 中（PII） |

#### Mapper 職責

```
DTO   ─AuthMapper──►  Domain Model
Entity─ProfileMapper►  Domain Model
       ProfileMapper►  Entity (cache write)
       ProfileMapper►  Request DTO (update)
```

### 3.3 Presentation Layer

遵循 **MVI（Model-View-Intent）** 單向資料流。

#### 狀態管理三元素

每個功能定義三個密封類別：

| 元素 | 類別 | 說明 |
|------|------|------|
| 狀態（State） | `*UiState` (data class) | 畫面所有可渲染狀態，由 `StateFlow` 暴露 |
| 事件（Event） | `*UiEvent` (sealed interface) | 一次性導航或 Toast 事件，由 `SharedFlow` 暴露 |
| 意圖（Intent） | `*Intent` (sealed interface) | 使用者操作，呼叫 `onIntent()` 送入 ViewModel |

#### ViewModel 責任範圍

- 僅依賴 Use Cases，不直接操作 Repository
- 以 `viewModelScope` 管理協程生命週期
- 使用 `MutableStateFlow` 維護狀態，外部唯讀

#### Composable 責任範圍

- 訂閱 `StateFlow` 並被動渲染
- 以 `LaunchedEffect` 收集 `SharedFlow` 事件
- 不持有任何業務邏輯，所有互動透過 `Intent` 送入 ViewModel

### 3.4 Core 模組

| 套件 | 類別 | 說明 |
|------|------|------|
| `core/network` | `ApiException` | HTTP 錯誤統一封裝 |
| `core/network` | `AuthInterceptor` | OkHttp 攔截器，自動附加 Bearer Token |
| `core/network` | `MockApiInterceptor` | 本地 API 模擬（離線開發用） |
| `core/network` | `safeApiCall` | Retrofit 呼叫統一 try-catch 包裝 |
| `core/security` | `TokenStorage` | Token 存取介面 |
| `core/security` | `EncryptedTokenStorage` | AES-256 加密實作 |
| `core/util` | `DispatcherProvider` | Coroutine Dispatcher 介面 |
| `core/util` | `DefaultDispatcherProvider` | 正式 Dispatcher 實作 |
| `core/database` | `AppDatabase` | Room Database 定義 |
| `core/datastore` | `AppSettings` | 設定資料 class（isDarkMode, language, notificationsEnabled, analyticsEnabled, crashReportingEnabled, personalizedAdsEnabled, biometricLoginEnabled） |
| `core/datastore` | `SettingsDataStore` | 設定讀寫 Flow 介面 |
| `core/datastore` | `SettingsDataStoreImpl` | Preferences DataStore 實作 |
| `core/notification` | `NotificationHelper` | 建立 channel、發送系統通知、設定 deep link PendingIntent |
| `core/notification` | `NotificationSyncWorker` | `@HiltWorker` + `@AssistedInject`，週期 15 分鐘同步通知並彈出系統通知 |
| `core/fcm` | `FcmService` | `@AndroidEntryPoint` Firebase 推播服務；`onNewToken` 在已登入時重新上報 Token；`onMessageReceived` 透過 `NotificationHelper` 顯示即時系統通知 |

### 3.5 DI 模組

| 模組 | 安裝範圍 | 提供內容 |
|------|----------|----------|
| `AppModule` | `SingletonComponent` | `DispatcherProvider`, `TokenStorage` 綁定 |
| `NetworkModule` | `SingletonComponent` | `Moshi`, `OkHttpClient`, `Retrofit`, `AuthApi`, `ProfileApi`, `NotificationsApi` |
| `DatabaseModule` | `SingletonComponent` | `AppDatabase`, `ProfileDao`, `NotificationDao` |
| `RepositoryModule` | `SingletonComponent` | `AuthRepository`, `ProfileRepository`, `SettingsRepository`, `NotificationsRepository` 綁定 |
| `DataStoreModule` | `SingletonComponent` | `SettingsDataStore` 綁定 |
| Hilt 自動產生 | `SingletonComponent` | `HiltWorkerFactory`（注入於 `AndroidMvvmArchApplication.workManagerConfiguration`） |

---

## 4. 目錄結構

```
app/src/main/java/com/example/android_mvvm_arch/
│
├── AndroidMvvmArchApplication.kt   # @HiltAndroidApp
├── MainActivity.kt                 # @AndroidEntryPoint, NavHost 宿主
│
├── navigation/
│   ├── Routes.kt                   # 路由常數
│   └── AppNavGraph.kt              # Compose NavHost 導航圖
│
├── presentation/
│   └── MainViewModel.kt            # 判斷啟動頁面（login / profile）
│
├── core/
│   ├── network/
│   │   ├── ApiException.kt
│   │   ├── AuthInterceptor.kt
│   │   ├── MockApiInterceptor.kt
│   │   └── RetrofitExtensions.kt
│   ├── security/
│   │   ├── TokenStorage.kt
│   │   └── EncryptedTokenStorage.kt
│   ├── util/
│   │   ├── DispatcherProvider.kt
│   │   └── DefaultDispatcherProvider.kt
│   ├── database/
│   │   └── AppDatabase.kt
│   ├── datastore/
│   │   ├── AppSettings.kt
│   │   ├── SettingsDataStore.kt
│   │   └── SettingsDataStoreImpl.kt
│   ├── notification/
│   │   ├── NotificationHelper.kt
│   │   └── NotificationSyncWorker.kt
│   └── fcm/
│       └── FcmService.kt
│
├── di/
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   ├── DataStoreModule.kt
│   └── RepositoryModule.kt
│
└── feature/
    ├── auth/
    │   ├── domain/
    │   │   ├── model/  LoginCredentials.kt, AuthTokens.kt
    │   │   ├── repo/   AuthRepository.kt
    │   │   └── usecase/ LoginUseCase.kt, LogoutUseCase.kt,
    │   │                IsLoggedInUseCase.kt,
    │   │                RegisterFcmTokenUseCase.kt
    │   ├── data/
    │   │   ├── remote/ AuthApi.kt
    │   │   │   └── dto/ LoginRequestDto.kt, LoginResponseDto.kt,
    │   │   │            ApiErrorDto.kt,
    │   │   │            RegisterFcmTokenRequestDto.kt
    │   │   ├── mapper/ AuthMapper.kt
    │   │   └── repo/   AuthRepositoryImpl.kt
    │   └── presentation/
    │       ├── state/  LoginUiState.kt, LoginUiEvent.kt, LoginIntent.kt
    │       ├── viewmodel/ LoginViewModel.kt
    │       └── ui/     LoginScreen.kt
    │
    └── profile/
        ├── domain/
        │   ├── model/  UserProfile.kt, ProfileUpdate.kt
        │   ├── repo/   ProfileRepository.kt
        │   └── usecase/ GetUserProfileUseCase.kt,
        │                UpdateUserProfileUseCase.kt
        ├── data/
        │   ├── remote/ ProfileApi.kt
        │   │   └── dto/ UserProfileDto.kt, UpdateProfileRequestDto.kt
        │   ├── local/  ProfileEntity.kt, ProfileDao.kt
        │   ├── mapper/ ProfileMapper.kt
        │   └── repo/   ProfileRepositoryImpl.kt
        └── presentation/
            ├── state/  ProfileUiState.kt, ProfileUiEvent.kt,
            │           ProfileIntent.kt
            ├── viewmodel/ ProfileViewModel.kt
            └── ui/     ProfileScreen.kt
    │
    └── settings/
        ├── domain/
        │   ├── repo/   SettingsRepository.kt
        │   └── usecase/ GetAppSettingsUseCase.kt,
        │                UpdateDarkModeUseCase.kt,
        │                UpdateLanguageUseCase.kt,
        │                UpdateNotificationsUseCase.kt,
        │                UpdateAnalyticsUseCase.kt,
        │                UpdateCrashReportingUseCase.kt,
        │                UpdatePersonalizedAdsUseCase.kt,
        │                UpdateBiometricLoginUseCase.kt,
        │                ClearCacheUseCase.kt
        ├── data/
        │   └── repo/   SettingsRepositoryImpl.kt
        └── presentation/
            ├── state/  SettingsUiState.kt, SettingsUiEvent.kt,
            │           SettingsIntent.kt
            ├── viewmodel/ SettingsViewModel.kt
            └── ui/     SettingsScreen.kt
    │
    └── notifications/
        ├── domain/
        │   ├── model/   Notification.kt, NotificationType.kt
        │   ├── repo/    NotificationsRepository.kt
        │   └── usecase/ GetNotificationsUseCase.kt,
        │                 RefreshNotificationsUseCase.kt,
        │                 MarkNotificationReadUseCase.kt,
        │                 MarkAllNotificationsReadUseCase.kt,
        │                 GetUnreadCountUseCase.kt
        ├── data/
        │   ├── local/   NotificationEntity.kt, NotificationDao.kt
        │   ├── remote/  NotificationsApi.kt
        │   │   └── dto/ NotificationDto.kt, NotificationsResponseDto.kt
        │   ├── mapper/  NotificationMapper.kt
        │   └── repo/    NotificationsRepositoryImpl.kt
        └── presentation/
            ├── state/   NotificationsUiState.kt, NotificationsUiEvent.kt,
            │            NotificationsIntent.kt
            ├── viewmodel/ NotificationsViewModel.kt
            └── ui/      NotificationsScreen.kt, RelativeTime.kt
```

---

## 5. 技術棧與依賴版本

| 分類 | 函式庫 | 版本 |
|------|--------|------|
| **語言** | Kotlin | 2.2.10 |
| **建構** | AGP | 9.2.1 |
| **建構** | Gradle | 9.4.1 |
| **建構** | KSP | 2.2.10-2.0.2 |
| **UI** | Jetpack Compose BOM | 2026.02.01 |
| **UI** | Material3 | BOM 管理 |
| **UI** | Navigation Compose | 2.8.5 |
| **DI** | Hilt | 2.59.1 |
| **DI** | Hilt Navigation Compose | 1.2.0 |
| **非同步** | Kotlinx Coroutines | 1.10.1 |
| **網路** | Retrofit | 2.11.0 |
| **網路** | OkHttp | 4.12.0 |
| **序列化** | Moshi + Kotlin Codegen | 1.15.2 |
| **本地儲存** | Room | 2.7.0 |
| **本地儲存** | DataStore Preferences | 1.1.1（已整合 Settings） |
| **背景排程** | WorkManager (Runtime KTX) | 2.10.0 |
| **背景排程** | Hilt Work / Hilt Compiler (androidx.hilt) | 1.2.0 |
| **安全** | Security Crypto | 1.1.0-alpha06 |
| **ViewModel** | Lifecycle | 2.8.7 |
| **推播** | Firebase BOM | 33.14.0 |
| **推播** | Firebase Messaging KTX | BOM 管理 |
| **單元測試** | JUnit 5 (Jupiter) | 5.11.4 |
| **單元測試** | MockK | 1.13.14 |
| **單元測試** | Turbine (Flow) | 1.2.0 |
| **單元測試** | Coroutines Test | 1.10.1 |

---

## 6. 功能模組設計

### 6.1 Auth 登入功能

#### 使用者故事

> 使用者輸入 email 與密碼，點擊「登入」後，系統驗證身份並導航至個人資料頁。

#### 狀態定義

```kotlin
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LoginUiEvent {
    data object NavigateToProfile : LoginUiEvent
    data class ShowMessage(val message: String) : LoginUiEvent
}

sealed interface LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data object SubmitLogin : LoginIntent
}
```

#### 業務邏輯（LoginUseCase）

1. 去除 email 前後空白
2. 驗證 email 不為空白
3. 驗證 email 格式（Regex）
4. 驗證 password 不為空白
5. 呼叫 `AuthRepository.login(credentials)`
6. Repository 收到成功回應後：
   - 儲存 Access Token 至 EncryptedSharedPreferences
   - 儲存 Refresh Token 至 EncryptedSharedPreferences
   - 立即刷新個人資料（`ProfileRepository.refreshProfile()`）

#### 輸入驗證規則

| 欄位 | 規則 | 錯誤訊息 |
|------|------|---------|
| email | 不得為空 | "Email cannot be empty." |
| email | 格式：`^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` | "Invalid email format." |
| password | 不得為空 | "Password cannot be empty." |

#### 登入後 FCM Token 上報

登入成功後，`LoginViewModel` 以 **fire-and-forget** 模式呼叫 `RegisterFcmTokenUseCase`，將裝置 FCM Token 上報至後端（`POST /api/v1/device/fcm-token`）。此操作不阻斷登入流程，即使失敗也不影響使用者體驗。詳細設計請參閱 [6.7 FCM 推播整合](#67-fcm-推播整合) 與 `doc/FCM.md`。

```kotlin
// LoginViewModel 登入成功後
_uiEvent.emit(LoginUiEvent.NavigateToProfile)
viewModelScope.launch {
    registerFcmTokenUseCase()  // fire-and-forget：失敗時靜默忽略
}
```

### 6.2 Profile 個人資料功能

#### 使用者故事

> 登入後使用者可檢視個人資料；點擊「編輯」後可修改顯示名稱、電話、簡介，儲存後即時反映。

#### 狀態定義

```kotlin
data class ProfileUiState(
    val profile: UserProfile? = null,
    val displayName: String = "",
    val phone: String = "",
    val bio: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
```

#### 資料流（Offline-First）

```
API 回應
  └─► ProfileRepositoryImpl.refreshProfile()
        ├─► ProfileMapper.toDomain(dto)
        ├─► ProfileDao.upsert(entity)      ← 本地快取寫入
        └─► 回傳 Result<UserProfile>

ProfileDao.observeProfile()               ← Room Flow
  └─► ProfileRepositoryImpl.observeProfile()
        └─► ProfileMapper.toDomain(entity)
              └─► ProfileViewModel._uiState 更新
```

#### 輸入驗證規則（UpdateUserProfileUseCase）

| 欄位 | 規則 | 錯誤訊息 |
|------|------|---------|
| displayName | 不得為空 | "Display name cannot be empty." |
| displayName | ≤ 50 字 | "Display name must be 50 characters or less." |
| bio | ≤ 200 字 | "Bio must be 200 characters or less." |

#### 頭像上傳（Camera / Gallery）

- 入口：ProfileScreen 頭像區塊上的編輯 IconButton → ModalBottomSheet（相機 / 相簿）
- 權限：Android 13+ `READ_MEDIA_IMAGES`，Android 12- `READ_EXTERNAL_STORAGE`，相機需 `CAMERA`；缺少權限以 Snackbar 提示
- 圖片來源：
  - 相簿：`Intent.ACTION_PICK` 取得 `Uri` → `Context.copyUriToCache()` 轉存 cache File → `ProfileIntent.UploadAvatar(file)`
  - 相機：`MediaStore.ACTION_IMAGE_CAPTURE` 搭配 `FileProvider("${applicationId}.fileprovider")` 輸出至 cache File
- UseCase：`UploadAvatarUseCase` 驗證檔案存在 / 非空 → `ProfileRepository.uploadAvatar(file)`（Multipart `avatar` 欄位）
- Repository：成功回傳後寫回 Room（`ProfileDao.upsert`）並透過 Flow 即時更新 UI；Mock 以 timestamp 產生新的 `avatar_url`
- UI：上傳時於頭像顯示遮罩 + 進度圈，成功 / 失敗以 Snackbar / `successMessage` 呈現

### 6.3 Settings 設定功能

#### 使用者故事

> 登入後使用者可從個人資料頁進入設定頁，切換深色模式、選擇語言（繁體中文 / English）、開關通知，並可從設定頁登出。

#### 狀態定義

```kotlin
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val language: String = "zh-TW",
    val notificationsEnabled: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface SettingsUiEvent {
    data object NavigateBack : SettingsUiEvent
    data object NavigateToLogin : SettingsUiEvent
}

sealed interface SettingsIntent {
    data class DarkModeChanged(val enabled: Boolean) : SettingsIntent
    data class LanguageChanged(val language: String) : SettingsIntent
    data class NotificationsChanged(val enabled: Boolean) : SettingsIntent
    data object Logout : SettingsIntent
}
```

#### DataStore 資料模型

```kotlin
data class AppSettings(
    val isDarkMode: Boolean = false,
    val language: String = "zh-TW",
    val notificationsEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val personalizedAdsEnabled: Boolean = false,
    val biometricLoginEnabled: Boolean = false,
)
```

Preferences Keys：`IS_DARK_MODE`、`LANGUAGE`、`NOTIFICATIONS_ENABLED`、`ANALYTICS_ENABLED`、`CRASH_REPORTING_ENABLED`、`PERSONALIZED_ADS_ENABLED`、`BIOMETRIC_LOGIN_ENABLED`（檔名 `app_settings`）

#### 資料流

```
SettingsScreen
  └─► SettingsViewModel.onIntent()
        └─► Update*UseCase（語言驗證在 UseCase 層）
              └─► SettingsRepositoryImpl
                    └─► SettingsDataStoreImpl.edit { }
                          └─► settingsFlow 自動 emit 新值
                                ├─► SettingsViewModel._uiState 更新
                                └─► MainActivity 訂閱 → Android_mvvm_archTheme(darkTheme)
```

#### 輸入驗證規則（UpdateLanguageUseCase）

| 欄位 | 規則 | 錯誤訊息 |
|------|------|---------|
| language | 僅允許 `"zh-TW"` 或 `"en"` | "Unsupported language. Allowed values: zh-TW, en." |

#### 深色模式整合

- `MainActivity` 注入 `SettingsDataStore`，以 `collectAsStateWithLifecycle` 訂閱 `settingsFlow`
- 將 `appSettings.isDarkMode` 傳入 `Android_mvvm_archTheme(darkTheme = ...)`
- 設定頁切換深色模式後，全 App 主題即時更新，無需重啟

#### 6.3.1 隱私設定（Privacy Settings）

延伸自既有的 Settings 模組，於設定頁下方新增「隱私與資料」分組，所有偏好皆儲存於相同的 `app_settings` DataStore Preferences；不涉及網路請求，亦不影響 `EncryptedTokenStorage`。

##### 4 個 Switch 偏好

| 偏好 | 預設值 | UseCase | Preferences Key | 說明 |
|------|--------|---------|-----------------|------|
| 使用分析（`analyticsEnabled`） | `true` | `UpdateAnalyticsUseCase` | `analytics_enabled` | 協助改善 App 體驗，可關閉以停止匿名使用資料蒐集 |
| 當機回報（`crashReportingEnabled`） | `true` | `UpdateCrashReportingUseCase` | `crash_reporting_enabled` | 自動回報當機資訊，協助診斷穩定性問題 |
| 個人化廣告（`personalizedAdsEnabled`） | `false` | `UpdatePersonalizedAdsUseCase` | `personalized_ads_enabled` | 根據使用習慣顯示相關內容；預設關閉以保守隱私 |
| 生物辨識登入（`biometricLoginEnabled`） | `false` | `UpdateBiometricLoginUseCase` | `biometric_login_enabled` | 僅儲存使用者偏好；實際 BiometricPrompt 整合不在本版本範圍 |

> 每個 UseCase 均委派至 `SettingsRepository`，最終寫入 `SettingsDataStoreImpl.edit { }`，由 `settingsFlow` 自動 re-emit 並更新 `SettingsUiState`。

##### 清除快取（ClearCacheUseCase）

| 項目 | 說明 |
|------|------|
| 入口 | 設定頁「清除快取」按鈕，點擊後跳出 Material3 `AlertDialog` 確認 |
| 範圍 | 透過 `ProfileRepository.clearProfileCache()` 清空 Profile Room 快取 |
| 保留項目 | `EncryptedTokenStorage`（Access / Refresh Token）、`SettingsDataStore`（所有偏好） |
| 成功回饋 | `SettingsViewModel` 發出 `SettingsUiEvent.CacheCleared`，UI 透過 `SnackbarHostState` 顯示「已清除本地快取」 |
| 失敗回饋 | 包裝為 `SettingsUiEvent.ShowError`，由 Snackbar 顯示錯誤訊息 |

##### 隱私權政策

| 項目 | 說明 |
|------|------|
| 入口 | 設定頁「隱私權政策」TextButton |
| 行為 | 以 `Intent.ACTION_VIEW` 開啟外部瀏覽器 |
| URL | `https://example.com/privacy`（示範用，可於發行時替換） |

##### MVI 擴充

```kotlin
sealed interface SettingsIntent {
    // 既有：DarkModeChanged / LanguageChanged / NotificationsChanged / Logout
    data class UpdateAnalytics(val enabled: Boolean) : SettingsIntent
    data class UpdateCrashReporting(val enabled: Boolean) : SettingsIntent
    data class UpdatePersonalizedAds(val enabled: Boolean) : SettingsIntent
    data class UpdateBiometricLogin(val enabled: Boolean) : SettingsIntent
    data object ClearCache : SettingsIntent
}

sealed interface SettingsUiEvent {
    // 既有：NavigateBack / NavigateToLogin
    data object CacheCleared : SettingsUiEvent
    data class ShowError(val message: String) : SettingsUiEvent
}
```

##### UI 規範

- 沿用 Material3 `Switch` 與 `TextButton` 風格
- 分組區塊以 `HorizontalDivider` + 區塊標題（`MaterialTheme.typography.titleMedium`）分隔
- 每個 Switch 項目使用 `Row` 配置：icon（`Icons.Default.*`）+ 標題 + 描述 + Switch
- 清除快取以 `AlertDialog` 二次確認
- 成功 / 失敗訊息以 `Snackbar` 呈現（透過 `SnackbarHostState`）

### 6.4 Notifications 通知功能

#### 使用者故事

> 登入後使用者可從個人資料頁 TopAppBar 的鈴鐺圖示（含未讀數量徽章）進入通知列表，瀏覽系統公告、行銷活動與活動提醒，下拉重新整理、單筆/全部標記為已讀；App 在背景每 15 分鐘同步最新通知，若有新項目則彈出系統通知，點擊後 deep link 回到通知列表。

#### Paging 3 整合（2026-05）

- 列表渲染改為 `LazyPagingItems<Notification>`，由 `NotificationsViewModel.pagingDataFlow` 提供，並透過 `cachedIn(viewModelScope)` 避免重建分頁流。
- Data Layer 新增 `NotificationsPagingSource`（`feature/notifications/data/paging/`），依 `page/pageSize` 呼叫 `NotificationsApi.getNotifications(...)` 並回傳 `LoadResult.Page`。
- `NotificationsApi` 與 `NotificationsResponseDto` 擴充分頁欄位：`page`、`pageSize`、`next_page`、`has_more`；Mock API 支援 query 並保留未帶參數時回第一頁的相容行為。
- 為維持未讀徽章與 Worker 的既有能力，PagingSource 在載入頁面時同步寫入 Room，`GetUnreadCountUseCase` 與 `NotificationSyncWorker` 無需改動流程。
- `markAsRead` / `markAllAsRead` 成功後會觸發列表 refresh，確保已讀狀態即時反映在 Paging UI。

#### 元件職責總表

| 類別 | 位置 | 職責 |
|------|------|------|
| `Notification` / `NotificationType` | `feature/notifications/domain/model` | Domain 模型（id, title, body, type, isRead, createdAt） |
| `NotificationsRepository` | `feature/notifications/domain/repo` | 通知資料抽象介面（observe / refresh / mark read） |
| `GetNotificationsUseCase` | `feature/notifications/domain/usecase` | 訂閱本地 Room Flow |
| `RefreshNotificationsUseCase` | 同上 | 拉取遠端並寫入 Room |
| `MarkNotificationReadUseCase` | 同上 | 驗證 id 後標記單筆已讀 |
| `MarkAllNotificationsReadUseCase` | 同上 | 全部標記已讀 |
| `GetUnreadCountUseCase` | 同上 | 訂閱未讀數量（驅動 ProfileScreen Badge） |
| `NotificationEntity` / `NotificationDao` | `feature/notifications/data/local` | Room 持久層 |
| `NotificationDto` / `NotificationsResponseDto` | `feature/notifications/data/remote/dto` | Retrofit DTO |
| `NotificationsApi` | `feature/notifications/data/remote` | Retrofit 介面（GET list / PATCH read / POST read-all） |
| `NotificationMapper` | `feature/notifications/data/mapper` | DTO ↔ Entity ↔ Domain Model 轉換 |
| `NotificationsRepositoryImpl` | `feature/notifications/data/repo` | Offline-first 實作 |
| `NotificationHelper` | `core/notification` | 建立 channel、發出系統通知、設定 deep link PendingIntent |
| `NotificationSyncWorker` | `core/notification` | `@HiltWorker` + `@AssistedInject`，週期 15 分鐘 |
| `NotificationsViewModel` | `feature/notifications/presentation/viewmodel` | MVI 狀態管理，啟動即訂閱 + 觸發 refresh |
| `NotificationsScreen` | `feature/notifications/presentation/ui` | Material3 列表、`PullToRefreshBox`、空狀態、`Snackbar` 重試 |

#### 狀態定義

```kotlin
data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<Notification> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface NotificationsUiEvent {
    data class ShowError(val message: String) : NotificationsUiEvent
    data object AllMarkedRead : NotificationsUiEvent
}

sealed interface NotificationsIntent {
    data object Load : NotificationsIntent
    data object Refresh : NotificationsIntent
    data class MarkRead(val id: String) : NotificationsIntent
    data object MarkAllRead : NotificationsIntent
    data object Retry : NotificationsIntent
}
```

#### Offline-first 資料流

```
NotificationsScreen
  └─► NotificationsViewModel
        ├─ 訂閱：GetNotificationsUseCase() ──► NotificationDao.observeAll() (Flow)
        │                                       │
        │                          ┌────────────┘
        │                          ▼
        │                  Room → Domain（NotificationMapper.toDomain(entity)）
        │                          │
        │                          ▼
        │                NotificationsUiState.items 自動更新
        │
        └─ 觸發：RefreshNotificationsUseCase()
              └─► NotificationsRepositoryImpl.refresh()
                    ├─ NotificationsApi.getNotifications()
                    ├─ safeApiCall 包裝錯誤
                    └─ NotificationDao.upsertAll(entities)
                          └─► 上游 Flow 自動 re-emit
```

#### MarkRead 樂觀更新策略

| 步驟 | 動作 |
|------|------|
| 1 | UI 點擊單筆通知 → `onIntent(MarkRead(id))` |
| 2 | ViewModel → `MarkNotificationReadUseCase(id)`（先驗證 id 非空） |
| 3 | RepositoryImpl 先呼叫 `NotificationsApi.markAsRead(id)`（PATCH） |
| 4 | API 成功 → `NotificationDao.markAsRead(id)` 將該筆 `isRead = 1` |
| 5 | UI 透過 Flow 收到變更，未讀小圓點消失 |
| 失敗 | 不更新 Room，由 `NotificationsUiEvent.ShowError` 顯示 Snackbar |

> 「樂觀更新」由 Room Flow + Mock API 的低延遲特性近似達成；正式上線可改為先寫 Room、再向後端非同步同步並在失敗時 rollback。

#### WorkManager 排程策略

| 項目 | 內容 |
|------|------|
| Worker | `NotificationSyncWorker`（`CoroutineWorker`，`@HiltWorker` + `@AssistedInject`） |
| Worker Factory | `HiltWorkerFactory`（由 Hilt 自動產生），注入於 `AndroidMvvmArchApplication.workManagerConfiguration` |
| 排程器 | `WorkManager.enqueueUniquePeriodicWork` |
| 唯一名稱 | `notification_sync` |
| 衝突策略 | `ExistingPeriodicWorkPolicy.KEEP`（已存在則不重建） |
| 間隔 | 15 分鐘（系統最短允許值） |
| 啟動時機 | `AndroidMvvmArchApplication.onCreate`（無條件 enqueue，Worker 內部會檢查 settings） |
| 失敗重試 | 最多 `MAX_RETRY_COUNT = 3` 次，超過直接 `Result.success()` 等下個週期 |

#### Worker 與 Settings notificationsEnabled 互動

```
doWork()
  1. settingsDataStore.settingsFlow.first().notificationsEnabled
       ├─ false → Result.success() 直接結束（不打 API、不顯示系統通知）
       └─ true  → 繼續下一步
  2. 讀取目前未讀 id 集合 (previousUnreadIds)
  3. RefreshNotificationsUseCase()
       ├─ Result.failure → runAttemptCount < 3 則 Result.retry()，否則 success
       └─ Result.success
  4. 重新讀取最新未讀清單，過濾出不在 previousUnreadIds 的新項目
  5. NotificationHelper.showNotification(...) 最多顯示 3 筆
  6. Result.success()
```

#### 系統通知設計

| 項目 | 內容 |
|------|------|
| Channel ID | `general_notifications` |
| Channel 名稱 | `一般通知` |
| Importance | `IMPORTANCE_DEFAULT` |
| Manifest 權限 | `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` |
| Runtime 請求 | Android 13+（`Build.VERSION.SDK_INT >= TIRAMISU`）才需要；UI 進入 `NotificationsScreen` 時透過 `rememberLauncherForActivityResult(RequestPermission)` 觸發 |
| 點擊行為 | PendingIntent 啟動 `MainActivity`（`singleTask`），帶 `EXTRA_DEEP_LINK = "notifications"`；MainActivity 解析後使用既有 `NavController` 跳轉至 `Routes.NOTIFICATIONS` |
| Channel 建立 | `AndroidMvvmArchApplication.onCreate` 呼叫 `NotificationHelper.createChannel()`；重複呼叫安全（先檢查 channel 是否存在） |

#### Hilt + WorkManager 整合

```kotlin
// AndroidMvvmArchApplication
@HiltAndroidApp
class AndroidMvvmArchApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
        NotificationSyncWorker.enqueuePeriodic(WorkManager.getInstance(this))
    }
}
```

> Manifest 中已停用 `WorkManagerInitializer`（透過 `androidx.startup.InitializationProvider` + `tools:node="remove"`），避免雙重初始化；改由 Application 的 `Configuration.Provider` 在首次 `WorkManager.getInstance(this)` 觸發時建立。

#### 與 Settings 模組整合

| 場景 | 行為 |
|------|------|
| 使用者於 SettingsScreen 關閉「通知」Switch（`notificationsEnabled = false`） | DataStore 立即寫入；下一次 Worker 啟動讀到 false 時不執行 API 與系統通知 |
| 使用者重新開啟 | DataStore 立即寫入；Worker 下一週期會恢復同步並於有新通知時推播 |
| 應用內通知列表 | 不受 `notificationsEnabled` 影響（仍可手動進入查看歷史） |

#### Mock 替換點（未來接 FCM）

| 抽象層 | 目前實作 | 替換為真實後端 |
|--------|---------|----------------|
| `NotificationsApi`（Retrofit interface） | `MockApiInterceptor` 三個端點回應 | 移除 mock 後 Retrofit 自動打真實 URL |
| `NotificationsRepository` | `NotificationsRepositoryImpl`（Offline-first） | 不需修改 |
| 推播來源 | `NotificationSyncWorker` 週期輪詢 | 新增 `FirebaseMessagingService` → 收到推播後同樣呼叫 `RefreshNotificationsUseCase()` + `NotificationHelper.showNotification(...)` |
| 系統通知顯示 | `NotificationHelper` | 共用，不必修改 |

#### UI 規範

- 列表項目：`Row` 配置 = 未讀小圓點（`primary` color，已讀則透明）+ 標題（`titleSmall`，未讀粗體）+ 內容（`bodyMedium`，maxLines=2 ellipsis）+ 類型標籤（`labelSmall`，primary 色）+ 相對時間（`labelSmall`，`onSurfaceVariant`）
- 下拉刷新：Material3 `PullToRefreshBox`（`isRefreshing = uiState.isRefreshing`）
- 空狀態：`Icons.Filled.NotificationsNone` + 「目前沒有通知」
- Loading：`CircularProgressIndicator` 置中（僅在 items 為空時顯示）
- 錯誤：`Snackbar`（含「重試」action label）→ 觸發 `NotificationsIntent.Retry`
- TopAppBar：標題「通知」、返回鈕、`Icons.Filled.DoneAll` action 觸發 `MarkAllRead`（無未讀時 disabled）
- ProfileScreen 入口：TopAppBar 鈴鐺 `IconButton` + `BadgedBox`（未讀數量 > 0 時顯示 `Badge`）

---

### 6.5 HomeDashboard 主頁功能

#### 使用者故事

> 登入成功後進入 Home Dashboard（Main Shell），可透過 Drawer 導航切換 Home / Profile / Settings / Notifications；Dashboard 快速動作改為觸發同一套 Drawer 導航目的地，避免多套路由分流。

#### UI 與互動

- **歡迎卡片**：顯示 `displayName`、`email`，背景採用當前 Theme（深/淺色、語言由 Settings DataStore 決定，Home 自動套用）。
- **快速動作（Quick Actions）**：維持 2 欄佈局，預設三個入口：個人資料、設定、通知；點擊後走與 Drawer 相同的主導航切換邏輯（單一導航入口）。
- **近期通知分頁列表（Paging 3）**：重用 Notifications Domain 的 `GetNotificationsPagingUseCase`，在 Home 以卡片形式呈現近期通知摘要（標題、內容、相對時間、通知類型、未讀點）。
- **刷新行為**：Home 採 `PullToRefreshBox`；一次 refresh 會同時觸發 `HomeViewModel.onRefresh()`（Profile 同步）與 `LazyPagingItems.refresh()`（通知分頁重載）。
- **錯誤與重試**：支援 `refresh` 初次載入錯誤重試、`append` 載入更多錯誤重試，以及手動「刷新」按鈕。

#### 狀態定義（MVI）

```kotlin
data class HomeUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val quickActions: List<QuickAction> = emptyList(),
)

data class QuickAction(
    val title: String,
    val icon: Int? = null,
    val route: String,
)
```

> Home 的分頁資料不放入 `HomeUiState`，而是由 `HomeViewModel` 直接暴露 `recentNotificationsPagingDataFlow: Flow<PagingData<Notification>>`，並在 Compose 端轉為 `LazyPagingItems`。

#### 資料流與邊界

```
HomeViewModel.init
  ├─► quickActions = [個人資料, 設定, 通知]
  ├─► collect GetUserProfileUseCase.observeProfile()
  │       └─► _uiState.userProfile 更新 → HomeContent 歡迎卡渲染
  ├─► launch getUserProfileUseCase.refresh()（首次同步 Profile）
  └─► recentNotificationsPagingDataFlow =
         GetNotificationsPagingUseCase().cachedIn(viewModelScope)
           └─► HomeScreen.collectAsLazyPagingItems()
                 └─► NotificationsPagingSource 分頁載入近期通知

HomeViewModel.onRefresh()
  └─► getUserProfileUseCase.refresh()（更新 Profile）
        └─ success/failure 更新 _uiState.isLoading / error

HomeScreen (PullToRefresh / 手動刷新)
  └─► 同步呼叫：
       1) viewModel.onRefresh()
       2) lazyPagingItems.refresh()
           └─ refresh/append loadState 決定 loading/error/retry UI

HomeScreen (QuickAction 點擊)
  └─► onNavigateToRoute(route)
       └─ AppNavGraph.navigateToMainDestination(route)
            └─ 與 Drawer item click 共用（HOME/PROFILE/SETTINGS/NOTIFICATIONS）
```

資料來源沿用 Mock API（`MockApiInterceptor`）+ Room 快取，無需真實後端即可展示 Dashboard。

#### 與既有模組整合

| 模組 | 整合點 |
|------|--------|
| Auth | `MainViewModel` 依 `IsLoggedInUseCase` 決定啟動路由；Login / Register 成功後 `popUpTo(Routes.AUTH_GRAPH)` 進入 `Routes.MAIN_GRAPH`（起始頁 `Routes.HOME`）。 |
| Profile | `GetUserProfileUseCase.observeProfile()` 提供 `UserProfile` Flow；`refresh()` 觸發 Repository 拉取並寫入 Room，Home 歡迎卡即時更新。 |
| Settings（DataStore） | Home 不直接寫入 DataStore，但 Theme / 語言 / `notificationsEnabled` 由 `SettingsDataStore` 決定並全域套用；快速動作連至 Settings 供使用者調整偏好。 |
| Notifications | 快速動作導向 `NotificationsScreen`；未讀資料由 `NotificationsRepository` + `NotificationSyncWorker`（15 分鐘輪詢，受 `notificationsEnabled` 影響）維持最新，Home 可訂閱 `GetUnreadCountUseCase` 作為摘要徽章或卡片。 |
| Navigation | `AppNavGraph` 採 `AUTH_GRAPH` + `MAIN_GRAPH` 巢狀結構；`MAIN_GRAPH` 由 `ModalNavigationDrawer` + 共用 TopAppBar 包裹，統一主導航與標題。 |

---

### 6.6 Paging 3 列表模板

為讓後續 ActivityLog 等列表能快速套用，本專案將 Notifications 抽象為可複用的 Paging 模板：

1. **API 規格**：列表端點統一支援 `page`、`pageSize`，回傳 `items + next_page + has_more`。
2. **Data Layer**：每個列表建立 `feature/<feature>/data/paging/*PagingSource.kt`，`load()` 只負責分頁拉取、錯誤映射與必要快取同步。
3. **Repository 介面**：提供 `Flow<PagingData<DomainModel>>`（例如 `getNotificationsPagingData()`），由 `Pager(PagingConfig(...))` 組裝。
4. **UseCase**：建立對應 `Get*PagingUseCase`，保持 Presentation 僅依賴 Domain。
5. **Presentation (MVI + Compose)**：ViewModel 暴露 `pagingDataFlow.cachedIn(viewModelScope)`；Screen 使用 `collectAsLazyPagingItems()`，統一處理 `loadState.refresh/append` 的 loading、error、retry、refresh。
6. **與既有快取協調**：若同時需要未讀徽章、背景同步、離線資料，PagingSource 可在成功載入後寫入 Room；標記已讀後觸發 `lazyPagingItems.refresh()` 以對齊遠端狀態。

---

### 6.7 FCM 推播整合

#### 功能概述

本節說明 Firebase Cloud Messaging（FCM）在本專案中的整合設計，包含登入後 Token 上報、Token 刷新機制與即時推播接收。完整技術細節請參閱 `doc/FCM.md`。

#### 設計決策

##### 為何採用 fire-and-forget 觸發？

| 考量點 | 決策 |
|--------|------|
| **使用者體驗優先** | FCM Token 上報失敗不應阻止使用者進入 App，登入成功即導航 |
| **非關鍵路徑** | 推播為輔助功能，不影響核心業務流程 |
| **最終一致性** | Token 刷新時 `FcmService.onNewToken` 自動重試 |
| **Firebase 冪等性** | 後端對重複上報 Token 通常設計為冪等，不需錯誤重試邏輯 |

##### 為何在 LoginViewModel 觸發，而非 LoginUseCase？

- `LoginUseCase` 遵循 **Domain 層不依賴 Android SDK** 原則；`FirebaseMessaging` 為 Android 元件，不應出現在 Domain 層
- `RegisterFcmTokenUseCase` 雖位於 Domain 層，但其 **實作**（`FirebaseMessaging.getInstance().token.await()`）可在 UseCase 內部處理 Android 依賴，或以介面隔離
- `LoginViewModel` 作為 Presentation 層的協調者，負責「登入成功後觸發哪些副作用」的決策，語意清晰

#### 元件職責總表

| 類別 | 位置 | 職責 |
|------|------|------|
| `RegisterFcmTokenUseCase` | `feature/auth/domain/usecase` | 取得 FCM Token 並呼叫 Repository 上報 |
| `RegisterFcmTokenRequestDto` | `feature/auth/data/remote/dto` | FCM Token 上報請求 DTO（`fcm_token`、`platform`） |
| `FcmService` | `core/fcm` | Firebase 推播服務；處理 Token 刷新與訊息接收 |
| `AuthApi.registerFcmToken` | `feature/auth/data/remote` | `POST /api/v1/device/fcm-token` Retrofit 端點 |
| `AuthRepositoryImpl.registerFcmToken` | `feature/auth/data/repo` | 透過 `safeApiCall` 呼叫 API |
| `LoginViewModel` | `feature/auth/presentation/viewmodel` | 登入成功後以 fire-and-forget 觸發 UseCase |

#### API 端點

| 方法 | 路徑 | 請求體 | 回應 | 說明 |
|------|------|--------|------|------|
| `POST` | `/api/v1/device/fcm-token` | `{ "fcm_token": "string", "platform": "android" }` | `204 No Content` | 登入後上報裝置 FCM Token |

#### 登入後上報時序

```
LoginScreen → LoginIntent.SubmitLogin → LoginViewModel.submitLogin()
  → loginUseCase() ✅
  → emit(NavigateToProfile)                    ← 立即導航，不等待 FCM
  → launch { registerFcmTokenUseCase() }        ← fire-and-forget
      → FirebaseMessaging.getInstance().token.await()
      → AuthRepository.registerFcmToken(token)
          → AuthApi: POST /api/v1/device/fcm-token
              body: { "fcm_token": "...", "platform": "android" }
              response: 204 No Content
```

#### FcmService 職責

```
FcmService (@AndroidEntryPoint)
  ├─ onNewToken(token)
  │    └─ if (authRepository.isLoggedIn())
  │            authRepository.registerFcmToken(token)
  └─ onMessageReceived(message)
       └─ notificationHelper.showNotification(title, body)
```

#### Mock 環境整合

`MockApiInterceptor` 已加入 `POST /api/v1/device/fcm-token` 路由，回傳 `204 No Content`，確保開發環境不依賴真實 Firebase 後端即可完整測試上報流程。


---

## 7. 安全設計（DLP）

### Token 管理

| 項目 | 實作方式 |
|------|---------|
| 儲存方式 | `EncryptedSharedPreferences`（AES-256-GCM key + AES-256-SIV key enc） |
| 金鑰管理 | Android Keystore（`MasterKey.KeyScheme.AES256_GCM`） |
| 存取介面 | `TokenStorage` interface，可在測試中替換 |
| Log 防護 | OkHttp `redactHeader("Authorization")`，禁止記錄 Token |
| 密碼防護 | `MockApiInterceptor` 解析後不寫入任何 Log |

### 網路安全

- 所有通訊使用 HTTPS（`API_BASE_URL` 為 `https://`）
- `HttpLoggingInterceptor.Level.BASIC`（不記錄 body）
- `AuthInterceptor` 在請求未帶 Authorization 時自動注入

### 本地資料庫

- Room 預設未加密；PII（email、phone）限制 Log 輸出
- 生產環境建議評估 SQLCipher 加密整個資料庫

---

## 8. 錯誤處理策略

### 統一包裝

```
Retrofit 呼叫
  └─► safeApiCall { }
        ├─ HttpException   → ApiException(code, message, errorCode)
        ├─ IOException     → 原始 IOException
        └─ 其他 Exception  → 原始 Exception
```

### ViewModel 錯誤映射

```kotlin
onFailure { error ->
    val message = when (error) {
        is ApiException           → error.message          // HTTP 錯誤
        is IllegalArgumentException → error.message        // 輸入驗證
        else                      → "通用錯誤訊息"
    }
    _uiState.update { it.copy(errorMessage = message) }
}
```

### HTTP 狀態碼對應

| 狀態碼 | 說明 | 使用者看到 |
|--------|------|----------|
| 401 | 帳號密碼錯誤 / Token 過期 | "Email or password is incorrect." |
| 422 | 驗證失敗 | 後端 message 欄位 |
| 500 | 伺服器錯誤 | "Login failed. Please try again." |
| 網路斷線 | IOException | "Login failed. Please try again." |

---

## 9. 導航設計

### 路由定義

```
Routes.AUTH_GRAPH      = "auth_graph"
Routes.MAIN_GRAPH      = "main_graph"

Routes.LOGIN           = "login"
Routes.HOME            = "home"
Routes.REGISTER        = "register"
Routes.FORGOT_PASSWORD = "forgot_password"
Routes.RESET_PASSWORD  = "reset_password"
Routes.PROFILE         = "profile"
Routes.SETTINGS        = "settings"
Routes.NOTIFICATIONS   = "notifications"
```

### 啟動路由決策（MainViewModel）

```
App 啟動
  └─► IsLoggedInUseCase.invoke()
        ├─ true  → startDestination = "home"
        └─ false → startDestination = "login"
```

### 導航流程

```
LoginScreen / RegisterScreen ─ 登入成功 ─► MainGraph(Home)
                                        (popUpTo auth_graph, inclusive=true)

MainGraph (Drawer + TopAppBar)
  ├─ DrawerItem(Home)          ─► HomeScreen
  ├─ DrawerItem(Profile)       ─► ProfileScreen
  ├─ DrawerItem(Settings)      ─► SettingsScreen
  └─ DrawerItem(Notifications) ─► NotificationsScreen

### 主導航實作細節（Drawer & Scaffold）

- **抽屜配置**：使用 Material 3 `ModalNavigationDrawer` 提供側邊導覽選單。
  - **寬度**：選單寬度動態計算為螢幕寬度的 70%（`LocalConfiguration.current.screenWidthDp.dp * 0.7f`）。
  - **手勢控制**：透過 `gesturesEnabled = inMainArea` 控制，僅在登入後的主畫面區域允許手勢開啟抽屜。
  - **內容**：包含 `ModalDrawerSheet` 與多個 `NavigationDrawerItem`，點擊後會關閉抽屜並切換路由。
- **頂部欄 (TopAppBar)**：
  - 整合於 `Scaffold` 中，僅在 `inMainArea` 為真時顯示。
  - 標題透過 `Routes.titleForRoute(currentRoute)` 動態解析。
  - 左側提供 `IconButton` 點擊開啟抽屜。
- **實驗性 API 處理**：
  - 由於 Material 3 部分組件（如 `TopAppBar`）仍處於實驗階段，`AppNavGraph` 標註了 `@OptIn(ExperimentalMaterial3Api::class)`。

HomeScreen ─ QuickAction(Profile/Settings/Notifications)
          ─► 與 Drawer 共用 navigateToMainDestination(route)

System Notification ─ 點擊 ──► MainActivity (deep link "notifications")
                              └─► NavController.navigate(Routes.NOTIFICATIONS)
                                   (Drawer 自動高亮 Notifications)

ProfileScreen / SettingsScreen ─ 登出 ──► LoginScreen
                              (popUpTo main_graph, inclusive=true)
```

### 底層實作

- 使用 Jetpack Navigation Compose（`NavHost` / `composable`）
- 使用 `navigation(...)` 建立 `AUTH_GRAPH` 與 `MAIN_GRAPH` 巢狀圖
- `MAIN_GRAPH` 外層以 `ModalNavigationDrawer` + 共用 TopAppBar 統一導覽
- `AppNavGraph` 統一管理路由，由 `MainActivity` 持有 `NavController`
- `hiltViewModel()` 在 NavGraph 內自動注入 ViewModel

---

## 10. 測試策略

### 測試分層

| 層級 | 測試類型 | 工具 |
|------|----------|------|
| Domain UseCase | 單元測試 | JUnit 5, MockK, Coroutines Test |
| Data Mapper | 單元測試 | JUnit 5（不需 Mock） |
| Presentation ViewModel | 單元測試 | JUnit 5, MockK, Turbine |
| Repository | 整合測試（可選） | MockWebServer, Room In-Memory |

### 測試檔案清單

| 測試類別 | 涵蓋案例 |
|----------|---------|
| `LoginUseCaseTest` | 正常登入、email 空白、email 格式錯誤、password 空白 |
| `UpdateUserProfileUseCaseTest` | 正常更新、displayName 空白、bio 超過 200 字 |
| `LoginViewModelTest` | Email 輸入狀態、登入成功導航事件、API 錯誤訊息、清除錯誤 |
| `ProfileViewModelTest` | 開始編輯、儲存成功、登出導航事件 |
| `AuthMapperTest` | DTO→Request、Response DTO→Domain |
| `ProfileMapperTest` | DTO→Domain、Entity 來回轉換、Update→RequestDTO |

### 測試慣例

- `MainDispatcherRule`：JUnit 5 Extension，替換 `Dispatchers.Main` 為測試 Dispatcher
- MockK：`mockk()` 建立假物件，`coEvery` 設定 suspend 函式行為
- Turbine：`SharedFlow.test { awaitItem() }` 驗證一次性事件
- 使用 `UnconfinedTestDispatcher` 讓協程同步執行

### 執行指令

```bash
# 執行所有單元測試
./gradlew :app:testDebugUnitTest

# 產生測試報告
./gradlew :app:testDebugUnitTest --continue
# 報告位置：app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 11. Offline-First Sync Strategy（SyncManager + WorkManager）

### 11.1 設計目標

- 建立單一同步協調入口 `SyncManager`，統一 Profile 與 Notifications 同步流程。
- 將同步執行收斂至 WorkManager（`Periodic + OneTime`），避免各功能分散調度。
- 保持 Offline-first 原則：UI 一律讀本地（Room / DataStore），同步僅負責遠端抓取後回寫本地。

### 11.2 核心元件

| 元件 | 職責 |
|------|------|
| `SyncTarget` | 宣告可擴充同步目標（目前 `PROFILE`、`NOTIFICATIONS`） |
| `SyncResult` | 彙整成功/失敗/跳過 target 與是否建議重試 |
| `SyncManager` | 對外提供週期排程、即時同步請求、實際同步執行 |
| `SyncManagerImpl` | 整合 `GetUserProfileUseCase`、`RefreshNotificationsUseCase`、`SettingsDataStore`、`IsLoggedInUseCase` |
| `SyncWorker` | WorkManager 主協調 Worker，解析 targets 後委派 `SyncManager.runSync()` |

### 11.3 排程與重試策略

- 週期同步：`PeriodicWorkRequest<SyncWorker>`，15 分鐘，`ExistingPeriodicWorkPolicy.KEEP`。
- 即時同步：`OneTimeWorkRequest<SyncWorker>`，`ExistingWorkPolicy.APPEND_OR_REPLACE`，可由登入成功、前景喚醒、手動刷新觸發。
- 網路限制：`Constraints(requiredNetworkType = NetworkType.CONNECTED)`。
- Backoff：`BackoffPolicy.EXPONENTIAL`（30 秒起跳）。
- Retry 邏輯：
  - `SyncManager` 針對可重試錯誤（`IOException`、`ApiException.code >= 500`）標記 `shouldRetry = true`。
  - `SyncWorker` 在 `runAttemptCount < 3` 時回傳 `Result.retry()`，否則 `Result.success()` 等下次週期。

### 11.4 觸發點整合

| 場景 | 同步入口 |
|------|---------|
| App 啟動 | `AndroidMvvmArchApplication.onCreate()` 呼叫 `syncManager.schedulePeriodicSync()` |
| App 回前景 | `MainActivity.onStart()` 呼叫 `syncManager.requestImmediateSync()` |
| 登入成功 | `LoginViewModel` 呼叫 `requestImmediateSync(PROFILE + NOTIFICATIONS)` |
| Profile 手動刷新 | `ProfileViewModel` 呼叫 `runSync(PROFILE)` |
| Notifications 手動刷新 | `NotificationsViewModel` 呼叫 `requestImmediateSync(NOTIFICATIONS)`，並觸發列表 refresh |

### 11.5 與 Settings / Notifications 整合

- `SyncManagerImpl` 會讀取 `SettingsDataStore.notificationsEnabled`：
  - `false`：將 `NOTIFICATIONS` 標記為 skipped（不打 API）。
  - `true`：正常執行通知同步。
- 既有 `NotificationSyncWorker` 保留「新未讀通知比對 + 系統通知顯示」能力；
  實際資料同步改委派 `SyncManager.runSync(NOTIFICATIONS)`，降低搬遷風險。

### 11.6 Offline-First 行為保證

- ViewModel / UI 不直接依賴遠端 API 結果顯示資料，仍以本地 Flow 為主。
- 同步失敗不會清空 Room 快取，使用者可持續瀏覽既有資料。
- 錯誤以 UI event / Snackbar 呈現，不阻斷既有本地瀏覽與操作流程。
