package com.example.android_mvvm_arch.feature.profile.data.repo

import com.example.android_mvvm_arch.core.network.safeApiCall
import com.example.android_mvvm_arch.feature.profile.data.local.ProfileDao
import com.example.android_mvvm_arch.feature.profile.data.mapper.ProfileMapper
import com.example.android_mvvm_arch.feature.profile.data.remote.ProfileApi
import com.example.android_mvvm_arch.feature.profile.domain.model.ProfileUpdate
import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile
import com.example.android_mvvm_arch.feature.profile.domain.repo.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
    private val profileDao: ProfileDao,
    private val profileMapper: ProfileMapper,
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        profileDao.observeProfile().map { entity ->
            entity?.let(profileMapper::toDomain)
        }

    override suspend fun refreshProfile(): Result<UserProfile> = safeApiCall {
        profileApi.getProfile()
    }.map { dto ->
        val profile = profileMapper.toDomain(dto)
        profileDao.upsert(profileMapper.toEntity(profile))
        profile
    }

    override suspend fun updateProfile(update: ProfileUpdate): Result<UserProfile> = safeApiCall {
        profileApi.updateProfile(profileMapper.toUpdateRequestDto(update))
    }.map { dto ->
        val profile = profileMapper.toDomain(dto)
        profileDao.upsert(profileMapper.toEntity(profile))
        profile
    }

    override suspend fun clearProfileCache() {
        profileDao.clear()
    }
}
