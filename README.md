## Android MVVM Architecture Sample

以 Clean Architecture + MVVM + MVI 範例實作 Auth / Profile / Settings / Notifications，並新增 HomeDashboard 主頁。所有網路呼叫由 `MockApiInterceptor` 本地模擬，可離線開發。

### 主要功能
- **HomeDashboard**：歡迎詞 + 使用者資料摘要，快速動作導向 Profile / Settings / Notifications；可擴充未讀通知摘要。顏色與語言遵循 DataStore 的主題/語言/通知偏好。
- **Auth**：登入、註冊、忘記/重設密碼流程；成功後導向 `Routes.HOME`。
- **Profile**：檢視與編輯個人資料，Room 快取 + Mock API 刷新。
- **Settings**：深色模式、語言切換、通知開關及隱私偏好皆寫入 DataStore (`app_settings`)；變更即時套用全域 Theme。
- **Notifications**：採 Paging 3（Compose `LazyPagingItems`）列表、下拉刷新、單筆/全部設已讀；未讀徽章，並透過 `NotificationSyncWorker` (15 分鐘) 依 `notificationsEnabled` 同步與推播。

### 技術與依賴
- Kotlin 2.2.x、AGP 9.2.x、Gradle 9.4.x、KSP
- Jetpack Compose (Material3、Navigation)、Hilt、Coroutines
- Retrofit + OkHttp + Moshi
- Room、DataStore Preferences
- WorkManager + Hilt Worker（背景通知同步）

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

### 設定、通知與背景工作
- DataStore (`SettingsDataStore`) 儲存主題、語言、通知開關與隱私偏好；HomeDashboard、Profile、Notifications 均跟隨其狀態。
- `NotificationSyncWorker` 每 15 分鐘輪詢通知，受 `notificationsEnabled` 影響；若關閉通知則 Worker 直接結束且不推播。
- Notifications 模組提供 `GetUnreadCountUseCase` 可給 HomeDashboard 或 Profile Badge 使用。

### 導航路由
- `home`（啟動頁，登入後導向）
- `login` / `register` / `forgot_password` / `reset_password`
- `profile` / `settings` / `notifications`
