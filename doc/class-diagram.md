# 類別圖 (Class Diagram)
# Android MVVM Architecture — Auth & Profile

> 使用 [Mermaid](https://mermaid.js.org/) 語法繪製，可在 GitHub、GitLab、Markdown 預覽工具中直接渲染。

---

## 1. 完整架構類別圖

```mermaid
classDiagram
    direction TB

    %% ─────────── Domain Layer ───────────
    namespace DomainAuth {
        class LoginCredentials {
            +email: String
            +password: String
        }
        class AuthTokens {
            +accessToken: String
            +refreshToken: String
            +expiresInSeconds: Long
            +tokenType: String
        }
        class AuthRepository {
            <<interface>>
            +login(credentials: LoginCredentials) Result~AuthTokens~
            +logout() Result~Unit~
            +isLoggedIn() Boolean
        }
        class LoginUseCase {
            -authRepository: AuthRepository
            -dispatcherProvider: DispatcherProvider
            +invoke(email: String, password: String) Result~AuthTokens~
        }
        class LogoutUseCase {
            -authRepository: AuthRepository
            -dispatcherProvider: DispatcherProvider
            +invoke() Result~Unit~
        }
        class IsLoggedInUseCase {
            -authRepository: AuthRepository
            -dispatcherProvider: DispatcherProvider
            +invoke() Boolean
        }
    }

    namespace DomainProfile {
        class UserProfile {
            +id: String
            +email: String
            +displayName: String
            +avatarUrl: String?
            +phone: String?
            +bio: String?
            +createdAt: String
            +updatedAt: String
        }
        class ProfileUpdate {
            +displayName: String?
            +phone: String?
            +bio: String?
        }
        class ProfileRepository {
            <<interface>>
            +observeProfile() Flow~UserProfile?~
            +refreshProfile() Result~UserProfile~
            +updateProfile(update: ProfileUpdate) Result~UserProfile~
            +clearProfileCache()
        }
        class GetUserProfileUseCase {
            -profileRepository: ProfileRepository
            -dispatcherProvider: DispatcherProvider
            +observeProfile() Flow~UserProfile?~
            +refresh() Result~UserProfile~
        }
        class UpdateUserProfileUseCase {
            -profileRepository: ProfileRepository
            -dispatcherProvider: DispatcherProvider
            +invoke(displayName: String, phone: String, bio: String) Result~UserProfile~
        }
    }

    %% ─────────── Data Layer ───────────
    namespace DataAuth {
        class AuthApi {
            <<interface>>
            +login(request: LoginRequestDto) LoginResponseDto
            +logout()
        }
        class LoginRequestDto {
            +email: String
            +password: String
        }
        class LoginResponseDto {
            +accessToken: String
            +refreshToken: String
            +expiresIn: Long
            +tokenType: String
        }
        class ApiErrorDto {
            +error: String?
            +message: String?
        }
        class AuthMapper {
            +toLoginRequestDto(credentials: LoginCredentials) LoginRequestDto
            +toDomain(dto: LoginResponseDto) AuthTokens
        }
        class AuthRepositoryImpl {
            -authApi: AuthApi
            -authMapper: AuthMapper
            -tokenStorage: TokenStorage
            -profileRepository: ProfileRepository
            +login(credentials: LoginCredentials) Result~AuthTokens~
            +logout() Result~Unit~
            +isLoggedIn() Boolean
        }
    }

    namespace DataProfile {
        class ProfileApi {
            <<interface>>
            +getProfile() UserProfileDto
            +updateProfile(request: UpdateProfileRequestDto) UserProfileDto
        }
        class UserProfileDto {
            +id: String
            +email: String
            +displayName: String
            +avatarUrl: String?
            +phone: String?
            +bio: String?
            +createdAt: String
            +updatedAt: String
        }
        class UpdateProfileRequestDto {
            +displayName: String?
            +phone: String?
            +bio: String?
        }
        class ProfileEntity {
            <<Entity>>
            +id: String
            +email: String
            +displayName: String
            +avatarUrl: String?
            +phone: String?
            +bio: String?
            +createdAt: String
            +updatedAt: String
        }
        class ProfileDao {
            <<interface>>
            +observeProfile() Flow~ProfileEntity?~
            +getProfile() ProfileEntity?
            +upsert(profile: ProfileEntity)
            +clear()
        }
        class ProfileMapper {
            +toDomain(dto: UserProfileDto) UserProfile
            +toDomain(entity: ProfileEntity) UserProfile
            +toEntity(profile: UserProfile) ProfileEntity
            +toUpdateRequestDto(update: ProfileUpdate) UpdateProfileRequestDto
        }
        class ProfileRepositoryImpl {
            -profileApi: ProfileApi
            -profileDao: ProfileDao
            -profileMapper: ProfileMapper
            +observeProfile() Flow~UserProfile?~
            +refreshProfile() Result~UserProfile~
            +updateProfile(update: ProfileUpdate) Result~UserProfile~
            +clearProfileCache()
        }
    }

    %% ─────────── Core Layer ───────────
    namespace Core {
        class TokenStorage {
            <<interface>>
            +saveAccessToken(token: String)
            +saveRefreshToken(token: String)
            +getAccessToken() String?
            +getRefreshToken() String?
            +clearTokens()
            +hasAccessToken() Boolean
        }
        class EncryptedTokenStorage {
            -prefs: SharedPreferences
            +saveAccessToken(token: String)
            +getAccessToken() String?
            +clearTokens()
        }
        class DispatcherProvider {
            <<interface>>
            +io: CoroutineDispatcher
            +default: CoroutineDispatcher
            +main: CoroutineDispatcher
        }
        class ApiException {
            +code: Int
            +message: String
            +errorCode: String?
        }
        class AuthInterceptor {
            -tokenStorage: TokenStorage
            +intercept(chain: Chain) Response
        }
        class MockApiInterceptor {
            +intercept(chain: Chain) Response
        }
    }

    %% ─────────── Presentation Layer ───────────
    namespace PresentationAuth {
        class LoginUiState {
            +email: String
            +password: String
            +isLoading: Boolean
            +errorMessage: String?
        }
        class LoginUiEvent {
            <<sealed interface>>
            NavigateToProfile
            ShowMessage
        }
        class LoginIntent {
            <<sealed interface>>
            EmailChanged
            PasswordChanged
            SubmitLogin
        }
        class LoginViewModel {
            -loginUseCase: LoginUseCase
            +uiState: StateFlow~LoginUiState~
            +uiEvent: SharedFlow~LoginUiEvent~
            +onIntent(intent: LoginIntent)
        }
    }

    namespace PresentationProfile {
        class ProfileUiState {
            +profile: UserProfile?
            +displayName: String
            +phone: String
            +bio: String
            +isLoading: Boolean
            +isSaving: Boolean
            +isEditing: Boolean
            +errorMessage: String?
            +successMessage: String?
        }
        class ProfileUiEvent {
            <<sealed interface>>
            NavigateToLogin
        }
        class ProfileIntent {
            <<sealed interface>>
            Refresh
            StartEditing
            CancelEditing
            DisplayNameChanged
            PhoneChanged
            BioChanged
            SaveProfile
            Logout
        }
        class ProfileViewModel {
            -getUserProfileUseCase: GetUserProfileUseCase
            -updateUserProfileUseCase: UpdateUserProfileUseCase
            -logoutUseCase: LogoutUseCase
            +uiState: StateFlow~ProfileUiState~
            +uiEvent: SharedFlow~ProfileUiEvent~
            +onIntent(intent: ProfileIntent)
        }
    }

    %% ─────────── 依賴關係 ───────────

    %% Domain 使用案例依賴
    LoginUseCase --> AuthRepository
    LoginUseCase --> DispatcherProvider
    LogoutUseCase --> AuthRepository
    LogoutUseCase --> DispatcherProvider
    IsLoggedInUseCase --> AuthRepository
    IsLoggedInUseCase --> DispatcherProvider
    GetUserProfileUseCase --> ProfileRepository
    GetUserProfileUseCase --> DispatcherProvider
    UpdateUserProfileUseCase --> ProfileRepository
    UpdateUserProfileUseCase --> DispatcherProvider

    %% Data 實作 Domain 介面
    AuthRepositoryImpl ..|> AuthRepository
    ProfileRepositoryImpl ..|> ProfileRepository
    EncryptedTokenStorage ..|> TokenStorage

    %% Data 層內部依賴
    AuthRepositoryImpl --> AuthApi
    AuthRepositoryImpl --> AuthMapper
    AuthRepositoryImpl --> TokenStorage
    AuthRepositoryImpl --> ProfileRepository
    AuthMapper --> LoginCredentials
    AuthMapper --> LoginRequestDto
    AuthMapper --> LoginResponseDto
    AuthMapper --> AuthTokens
    ProfileRepositoryImpl --> ProfileApi
    ProfileRepositoryImpl --> ProfileDao
    ProfileRepositoryImpl --> ProfileMapper
    ProfileMapper --> UserProfileDto
    ProfileMapper --> ProfileEntity
    ProfileMapper --> UserProfile
    ProfileMapper --> ProfileUpdate
    ProfileMapper --> UpdateProfileRequestDto

    %% Presentation 依賴 Domain Use Cases
    LoginViewModel --> LoginUseCase
    ProfileViewModel --> GetUserProfileUseCase
    ProfileViewModel --> UpdateUserProfileUseCase
    ProfileViewModel --> LogoutUseCase

    %% State 關聯
    LoginViewModel --> LoginUiState
    LoginViewModel --> LoginUiEvent
    ProfileViewModel --> ProfileUiState
    ProfileViewModel --> ProfileUiEvent
```

---

## 2. Auth 功能類別圖（簡化版）

```mermaid
classDiagram
    direction LR

    class LoginScreen {
        +onNavigateToProfile()
        observes: LoginUiState
        sends: LoginIntent
    }

    class LoginViewModel {
        +uiState: StateFlow~LoginUiState~
        +uiEvent: SharedFlow~LoginUiEvent~
        +onIntent(LoginIntent)
    }

    class LoginUseCase {
        +invoke(email, password) Result~AuthTokens~
    }

    class AuthRepository {
        <<interface>>
        +login() Result~AuthTokens~
        +logout() Result~Unit~
        +isLoggedIn() Boolean
    }

    class AuthRepositoryImpl {
        +login() Result~AuthTokens~
    }

    class AuthApi {
        <<interface>>
        +login(LoginRequestDto) LoginResponseDto
    }

    class TokenStorage {
        <<interface>>
        +saveAccessToken(String)
        +getAccessToken() String?
    }

    class EncryptedTokenStorage {
        +saveAccessToken(String)
        +getAccessToken() String?
    }

    LoginScreen --> LoginViewModel : onIntent()
    LoginViewModel --> LoginUseCase : invoke()
    LoginUseCase --> AuthRepository : login()
    AuthRepositoryImpl ..|> AuthRepository
    AuthRepositoryImpl --> AuthApi : Retrofit
    AuthRepositoryImpl --> TokenStorage : save tokens
    EncryptedTokenStorage ..|> TokenStorage
```

---

## 3. Profile 功能類別圖（簡化版）

```mermaid
classDiagram
    direction LR

    class ProfileScreen {
        +onNavigateToLogin()
        observes: ProfileUiState
        sends: ProfileIntent
    }

    class ProfileViewModel {
        +uiState: StateFlow~ProfileUiState~
        +uiEvent: SharedFlow~ProfileUiEvent~
        +onIntent(ProfileIntent)
    }

    class GetUserProfileUseCase {
        +observeProfile() Flow~UserProfile?~
        +refresh() Result~UserProfile~
    }

    class UpdateUserProfileUseCase {
        +invoke(name, phone, bio) Result~UserProfile~
    }

    class LogoutUseCase {
        +invoke() Result~Unit~
    }

    class ProfileRepository {
        <<interface>>
        +observeProfile() Flow~UserProfile?~
        +refreshProfile() Result~UserProfile~
        +updateProfile() Result~UserProfile~
        +clearProfileCache()
    }

    class ProfileRepositoryImpl {
        +observeProfile() Flow~UserProfile?~
        +refreshProfile() Result~UserProfile~
    }

    class ProfileApi {
        <<interface>>
        +getProfile() UserProfileDto
        +updateProfile() UserProfileDto
    }

    class ProfileDao {
        <<interface>>
        +observeProfile() Flow~ProfileEntity?~
        +upsert(ProfileEntity)
        +clear()
    }

    class ProfileMapper {
        +toDomain(dto) UserProfile
        +toEntity(profile) ProfileEntity
    }

    ProfileScreen --> ProfileViewModel : onIntent()
    ProfileViewModel --> GetUserProfileUseCase : observe/refresh
    ProfileViewModel --> UpdateUserProfileUseCase : invoke()
    ProfileViewModel --> LogoutUseCase : invoke()
    ProfileRepositoryImpl ..|> ProfileRepository
    GetUserProfileUseCase --> ProfileRepository
    UpdateUserProfileUseCase --> ProfileRepository
    ProfileRepositoryImpl --> ProfileApi : Retrofit
    ProfileRepositoryImpl --> ProfileDao : Room
    ProfileRepositoryImpl --> ProfileMapper : convert
```

---

## 4. Core 安全模組類別圖

```mermaid
classDiagram
    direction TB

    class TokenStorage {
        <<interface>>
        +saveAccessToken(token: String)
        +saveRefreshToken(token: String)
        +getAccessToken() String?
        +getRefreshToken() String?
        +clearTokens()
        +hasAccessToken() Boolean
    }

    class EncryptedTokenStorage {
        -prefs: EncryptedSharedPreferences
        -masterKey: MasterKey
        +saveAccessToken(token: String)
        +getAccessToken() String?
        +clearTokens()
        +hasAccessToken() Boolean
    }

    class AuthInterceptor {
        -tokenStorage: TokenStorage
        +intercept(chain: Interceptor.Chain) Response
    }

    class MockApiInterceptor {
        +intercept(chain: Interceptor.Chain) Response
        -handleLogin(body: String) Pair~Int, String~
        -handleGetProfile(auth: String?) Pair~Int, String~
        -handleUpdateProfile(auth: String?, body: String) Pair~Int, String~
    }

    class ApiException {
        +code: Int
        +message: String
        +errorCode: String?
    }

    EncryptedTokenStorage ..|> TokenStorage
    AuthInterceptor --> TokenStorage : getAccessToken()
    MockApiInterceptor ..> ApiException : throws via safeApiCall
```

---

## 5. DI 模組依賴關係圖

```mermaid
classDiagram
    direction LR

    class AppModule {
        <<HiltModule>>
        +bindDispatcherProvider() DispatcherProvider
        +bindTokenStorage() TokenStorage
    }

    class NetworkModule {
        <<HiltModule>>
        +provideMoshi() Moshi
        +provideOkHttpClient() OkHttpClient
        +provideRetrofit() Retrofit
        +provideAuthApi() AuthApi
        +provideProfileApi() ProfileApi
    }

    class DatabaseModule {
        <<HiltModule>>
        +provideAppDatabase() AppDatabase
        +provideProfileDao() ProfileDao
    }

    class RepositoryModule {
        <<HiltModule>>
        +bindAuthRepository() AuthRepository
        +bindProfileRepository() ProfileRepository
    }

    class AppDatabase {
        +profileDao() ProfileDao
    }

    AppModule --> DispatcherProvider : binds
    AppModule --> TokenStorage : binds
    NetworkModule --> AuthApi : provides
    NetworkModule --> ProfileApi : provides
    DatabaseModule --> AppDatabase : provides
    DatabaseModule --> ProfileDao : provides
    RepositoryModule --> AuthRepository : binds
    RepositoryModule --> ProfileRepository : binds
```
