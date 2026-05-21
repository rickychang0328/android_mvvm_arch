package com.example.android_mvvm_arch.di

import android.content.Context
import androidx.room.Room
import com.example.android_mvvm_arch.core.database.AppDatabase
import com.example.android_mvvm_arch.feature.profile.data.local.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "android_mvvm_arch.db",
        ).build()

    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao = database.profileDao()
}
