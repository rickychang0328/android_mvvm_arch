package com.example.android_mvvm_arch.di

import com.example.android_mvvm_arch.core.datastore.SettingsDataStore
import com.example.android_mvvm_arch.core.datastore.SettingsDataStoreImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    @Binds
    @Singleton
    abstract fun bindSettingsDataStore(impl: SettingsDataStoreImpl): SettingsDataStore
}
