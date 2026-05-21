package com.example.android_mvvm_arch.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.android_mvvm_arch.feature.profile.data.local.ProfileDao
import com.example.android_mvvm_arch.feature.profile.data.local.ProfileEntity

@Database(
    entities = [ProfileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
