package com.example.android_mvvm_arch.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drawer 導航 UI 整合測試骨架。
 *
 * 待補強項目：
 * 1) 以測試專用 DI 覆寫登入狀態與假資料。
 * 2) 透過 compose-test-rule 驗證 Drawer 點擊後實際畫面切換。
 * 3) 驗證登出後回到 Login 且無法 back 到 Main 區。
 * 4) 驗證 Notification deep link 啟動後導向 Notifications。
 */
@Ignore("需要先補齊測試用 DI 與 ActivityScenario 設定")
@RunWith(AndroidJUnit4::class)
class DrawerNavigationFlowTest {

    @Test
    fun start_when_logged_in_opens_main_area() = Unit

    @Test
    fun drawer_switches_between_home_profile_settings_notifications() = Unit

    @Test
    fun logout_from_main_area_returns_to_login_and_clears_back_stack() = Unit

    @Test
    fun notification_deep_link_navigates_to_notifications() = Unit
}
