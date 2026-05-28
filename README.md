## Android MVVM Architecture Sample

以 Clean Architecture + MVVM + MVI 範例實作 Auth / Profile / Settings / Notifications，並新增 HomeDashboard + Drawer 主導航。所有網路呼叫由 `MockApiInterceptor` 本地模擬，可離線開發。

### 主要功能
- **HomeDashboard + Drawer**：登入後以 `ModalNavigationDrawer` 作為主殼層，統一切換 Home / Profile / Settings / Notifications；Dashboard 快速動作與 Drawer 共用同一套路由切換邏輯。
  - **Drawer 實作細節**：選單寬度採螢幕寬度 70% 動態計算；僅在登入後的主畫面區域啟動側滑手勢（Gestures）；整合 Material 3 實驗性 `TopAppBar` 提供選單切換入口。
- **Auth**：登入、註冊、忘記/重設密碼流程；成功後導向 `Routes.HOME`。
- **Profile**：檢視與編輯個人資料，Room 快取 + Mock API 刷新。
- **Settings**：深色模式、語言切換、通知開關及隱私偏好皆寫入 DataStore (`app_settings`)；變更即時套用全域 Theme。
- **Notifications**：採 Paging 3（Compose `LazyPagingItems`）列表、下拉刷新、單筆/全部設已讀；未讀徽章，並整合至 `SyncManager + WorkManager` 統一同步框架。
- **FCM 推播整合**：完整整合 Firebase Cloud Messaging。
  - **自動上報**：登入成功後 fire-and-forget 上報 FCM Token（`POST /api/v1/device/fcm-token`）。
  - **自動刷新**：`FcmService.onNewToken` 在已登入狀態下自動同步最新 Token。
  - **即時接收**：`FcmService.onMessageReceived` 即時顯示系統通知。
  - **Mock 支援**：開發環境透過 `MockApiInterceptor` 模擬，無需真實 Firebase 即可驗證流程。
- **Offline-First Sync**：新增 `core/sync/`，以 `SyncManager` 統一管理 `PROFILE`、`NOTIFICATIONS` 週期/即時同步，支援網路約束、指數退避、Settings 條件過濾。

### 技術與依賴
- Kotlin 2.2.x、AGP 9.2.x、Gradle 9.4.x、KSP
- Jetpack Compose (Material3、Navigation)、Hilt、Coroutines
- Retrofit + OkHttp + Moshi
- Room、DataStore Preferences
- WorkManager + Hilt Worker（Offline-first 週期/即時同步）
- **Firebase BOM 33.14.0 + Firebase Messaging KTX**（FCM 推播整合）

### Firebase 設定（可選）

本專案預設使用 Mock 模式，**無需** `google-services.json` 即可完整建置與執行。若需接入真實 Firebase（取得真實 FCM Token、接收真實推播），請參閱以下步驟：

1. 前往 [Firebase Console](https://console.firebase.google.com/) 建立 Android 應用程式（套件名稱：`com.example.android_mvvm_arch`）
2. 下載 `google-services.json` 並放置於 `app/` 目錄下
3. 在 Firebase Console 啟用 Cloud Messaging 功能

> **注意：** `google-services.json` 包含 API Key，請勿提交至版本控制（`.gitignore` 已預設排除）。  
> 詳細步驟請參閱 `doc/FCM.md`。

### 建置與執行
```bash
# 建置 Debug APK
./gradlew :app:assembleDebug

# 以模擬器/實體裝置安裝
./gradlew :app:installDebug
```
> 也可直接在 Android Studio 以 Run/Debug 執行 `app` module。

### 測試
```bash
# 執行單元測試
./gradlew :app:testDebugUnitTest

# 產生測試報告
./gradlew :app:testDebugUnitTest --continue
# 報告：app/build/reports/tests/testDebugUnitTest/index.html
```

### Mock API 與資料來源
- 所有 API 由 `MockApiInterceptor` 提供固定回應；無需後端即可登入、讀寫 Profile 或通知。
- Profile 採 Offline-first：Repository 先寫入 Room，再由 Flow 供 UI 訂閱。
- Notifications 清單採遠端分頁（Paging 3 + `NotificationsPagingSource`）；同時將已載入頁同步寫入 Room，維持未讀徽章與背景同步一致。
- HomeDashboard 直接重用 `GetNotificationsPagingUseCase` 與 `LazyPagingItems`，在不影響通知已讀/未讀流程下呈現「近期通知」摘要分頁列表。
- **FCM Token 上報**（`POST /api/v1/device/fcm-token`）由 `MockApiInterceptor` 攔截並回傳 `204 No Content`，登入後上報流程可在完全離線環境下驗證。

### 設定、通知與背景工作
- DataStore (`SettingsDataStore`) 儲存主題、語言、通知開關與隱私偏好；HomeDashboard、Profile、Notifications 均跟隨其狀態。
- `SyncWorker` 由 `SyncManager` 協調同步（`Periodic + OneTime`），並使用 `NetworkType.CONNECTED` + Exponential Backoff。
- 觸發時機統一：App 啟動註冊週期同步、App 回前景補即時同步、登入成功與手動刷新皆透過 `SyncManager` 入口觸發。
- `notificationsEnabled = false` 時會跳過 `NOTIFICATIONS` target；既有 `NotificationSyncWorker` 保留系統通知顯示邏輯，資料同步改委派 `SyncManager`。
- Notifications 模組提供 `GetUnreadCountUseCase` 可給 HomeDashboard 或 Profile Badge 使用。
- **FCM 推播**：`FcmService`（`core/fcm/`）作為 Firebase 推播服務入口，`onNewToken` 在已登入狀態下自動重新上報 Token；`onMessageReceived` 透過 `NotificationHelper` 顯示系統通知，與 `NotificationSyncWorker` 共用同一通知 Channel（`general_notifications`）。

### 導航路由
- `home`（啟動頁，登入後導向）
- `login` / `register` / `forgot_password` / `reset_password`
- `profile` / `settings` / `notifications`

### 設計文件
- 主 SDD：`doc/SDD.md`
- FCM 推播整合：`doc/FCM.md`
- Drawer 專用 SDD：`doc/SDD-DrawerNavigation.md`
- 類別圖：`doc/class-diagram.md`
- 循序圖：`doc/sequence-diagram.md`
