package com.example.android_mvvm_arch.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.android_mvvm_arch.feature.notifications.data.local.NotificationDao
import com.example.android_mvvm_arch.feature.notifications.data.local.NotificationEntity
import com.example.android_mvvm_arch.feature.profile.data.local.ProfileDao
import com.example.android_mvvm_arch.feature.profile.data.local.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        NotificationEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun notificationDao(): NotificationDao
}
