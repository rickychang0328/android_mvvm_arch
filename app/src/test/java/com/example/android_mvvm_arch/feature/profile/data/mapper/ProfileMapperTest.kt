package com.example.android_mvvm_arch.feature.profile.data.mapper

import com.example.android_mvvm_arch.feature.profile.data.local.ProfileEntity
import com.example.android_mvvm_arch.feature.profile.data.remote.dto.UserProfileDto
import com.example.android_mvvm_arch.feature.profile.domain.model.ProfileUpdate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfileMapperTest {

    private val mapper = ProfileMapper()

    private val dto = UserProfileDto(
        id = "usr_001",
        email = "demo@example.com",
        displayName = "Demo",
        avatarUrl = "https://example.com/avatar.png",
        phone = "+886912345678",
        bio = "Bio text",
        createdAt = "2024-01-15T08:30:00Z",
        updatedAt = "2025-03-01T12:00:00Z",
    )

    @Test
    fun `toDomain maps dto fields`() {
        val domain = mapper.toDomain(dto)

        assertEquals("usr_001", domain.id)
        assertEquals("demo@example.com", domain.email)
        assertEquals("Demo", domain.displayName)
    }

    @Test
    fun `entity round trip preserves data`() {
        val domain = mapper.toDomain(dto)
        val entity = mapper.toEntity(domain)
        val restored = mapper.toDomain(entity)

        assertEquals(domain, restored)
    }

    @Test
    fun `toUpdateRequestDto maps update fields`() {
        val request = mapper.toUpdateRequestDto(
            ProfileUpdate("Name", "+886900000000", "Bio"),
        )

        assertEquals("Name", request.displayName)
        assertEquals("+886900000000", request.phone)
        assertEquals("Bio", request.bio)
    }
}
