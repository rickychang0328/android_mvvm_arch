# Software Design Document (SDD)
# Android MVVM Architecture — Auth & Profile Feature

**專案名稱：** android_mvvm_arch  
**套件名稱：** `com.example.android_mvvm_arch`  
**版本：** 1.0.0  
**架構風格：** Clean Architecture + MVVM + MVI  
**語言：** Kotlin  
**日期：** 2026-05-21  

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
7. [安全設計（DLP）](#7-安全設計dlp)
8. [錯誤處理策略](#8-錯誤處理策略)
9. [導航設計](#9-導航設計)
10. [測試策略](#10-測試策略)

---

## 1. 專案概述

本專案為 Android 行動應用程式，示範以 **Clean Architecture** 搭配 **MVVM + MVI** 風格實作「登入 / 登出」與「個人資料檢視 / 編輯」兩大功能模組。所有網路呼叫透過 `MockApiInterceptor` 於本地模擬，可無需後端即獨立運行。

### 核心目標

| 目標 | 說明 |
|------|------|
| **可測試性** | 每層以介面隔離，便於 Unit Test 使用 MockK 替換 |
| **可維護性** | 功能依 feature 分包，各層職責明確 |
| **安全性** | Token 加密儲存，敏感資料不寫入 Log（DLP） |
| **可擴充性** | Domain 層不依賴 Android，可直接移植至多平台 |

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
│  DTOs · Mappers · EncryptedTokenStorage │
└─────────────────────────────────────────┘
```

**依賴方向：** `Presentation → Domain ← Data`（嚴格向內依賴）

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
| `GetUserProfileUseCase` | `feature/profile/domain/usecase` | 訂閱 / 刷新個人資料 |
| `UpdateUserProfileUseCase` | `feature/profile/domain/usecase` | 驗證欄位並呼叫 ProfileRepository.update |

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
| `AuthApi` | `feature/auth/data/remote` | Retrofit 介面，定義登入 / 登出端點 |
| `ProfileApi` | `feature/profile/data/remote` | Retrofit 介面，定義個人資料端點 |
| `LoginRequestDto` | `feature/auth/data/remote/dto` | 登入請求 DTO |
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
| `EncryptedTokenStorage` | `core/security` | Token 加密儲存（EncryptedSharedPreferences） |

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

### 3.5 DI 模組

| 模組 | 安裝範圍 | 提供內容 |
|------|----------|----------|
| `AppModule` | `SingletonComponent` | `DispatcherProvider`, `TokenStorage` 綁定 |
| `NetworkModule` | `SingletonComponent` | `Moshi`, `OkHttpClient`, `Retrofit`, `AuthApi`, `ProfileApi` |
| `DatabaseModule` | `SingletonComponent` | `AppDatabase`, `ProfileDao` |
| `RepositoryModule` | `SingletonComponent` | `AuthRepository`, `ProfileRepository` 綁定 |

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
│   └── database/
│       └── AppDatabase.kt
│
├── di/
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
│
└── feature/
    ├── auth/
    │   ├── domain/
    │   │   ├── model/  LoginCredentials.kt, AuthTokens.kt
    │   │   ├── repo/   AuthRepository.kt
    │   │   └── usecase/ LoginUseCase.kt, LogoutUseCase.kt,
    │   │                IsLoggedInUseCase.kt
    │   ├── data/
    │   │   ├── remote/ AuthApi.kt
    │   │   │   └── dto/ LoginRequestDto.kt, LoginResponseDto.kt,
    │   │   │            ApiErrorDto.kt
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
| **本地儲存** | DataStore Preferences | 1.1.1 |
| **安全** | Security Crypto | 1.1.0-alpha06 |
| **ViewModel** | Lifecycle | 2.8.7 |
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
Routes.LOGIN   = "login"
Routes.PROFILE = "profile"
```

### 啟動路由決策（MainViewModel）

```
App 啟動
  └─► IsLoggedInUseCase.invoke()
        ├─ true  → startDestination = "profile"
        └─ false → startDestination = "login"
```

### 導航流程

```
LoginScreen ─── 登入成功 ──► ProfileScreen
                              (popUpTo login, inclusive=true)

ProfileScreen ─ 登出 ──────► LoginScreen
                              (popUpTo profile, inclusive=true)
```

### 底層實作

- 使用 Jetpack Navigation Compose（`NavHost` / `composable`）
- `AppNavGraph` 統一管理路由，由 `MainActivity` 持有
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
