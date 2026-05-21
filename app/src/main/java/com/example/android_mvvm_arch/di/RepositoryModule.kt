package com.example.android_mvvm_arch.di

import com.example.android_mvvm_arch.feature.auth.data.repo.AuthRepositoryImpl
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import com.example.android_mvvm_arch.feature.profile.data.repo.ProfileRepositoryImpl
import com.example.android_mvvm_arch.feature.profile.domain.repo.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
