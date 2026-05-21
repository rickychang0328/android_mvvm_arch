package com.example.android_mvvm_arch.feature.profile.data.mapper

import com.example.android_mvvm_arch.feature.profile.data.local.ProfileEntity
import com.example.android_mvvm_arch.feature.profile.data.remote.dto.UpdateProfileRequestDto
import com.example.android_mvvm_arch.feature.profile.data.remote.dto.UserProfileDto
import com.example.android_mvvm_arch.feature.profile.domain.model.ProfileUpdate
import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileMapper @Inject constructor() {
    fun toDomain(dto: UserProfileDto): UserProfile = UserProfile(
        id = dto.id,
        email = dto.email,
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl,
        phone = dto.phone,
        bio = dto.bio,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
    )

    fun toEntity(profile: UserProfile): ProfileEntity = ProfileEntity(
        id = profile.id,
        email = profile.email,
        displayName = profile.displayName,
        avatarUrl = profile.avatarUrl,
        phone = profile.phone,
        bio = profile.bio,
        createdAt = profile.createdAt,
        updatedAt = profile.updatedAt,
    )

    fun toDomain(entity: ProfileEntity): UserProfile = UserProfile(
        id = entity.id,
        email = entity.email,
        displayName = entity.displayName,
        avatarUrl = entity.avatarUrl,
        phone = entity.phone,
        bio = entity.bio,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    fun toUpdateRequestDto(update: ProfileUpdate): UpdateProfileRequestDto =
        UpdateProfileRequestDto(
            displayName = update.displayName,
            phone = update.phone,
            bio = update.bio,
        )
}
