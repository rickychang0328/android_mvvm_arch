package com.example.android_mvvm_arch.di

import com.example.android_mvvm_arch.core.security.EncryptedTokenStorage
import com.example.android_mvvm_arch.core.security.TokenStorage
import com.example.android_mvvm_arch.core.util.DefaultDispatcherProvider
import com.example.android_mvvm_arch.core.util.DispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindTokenStorage(impl: EncryptedTokenStorage): TokenStorage
}
