package com.example.android_mvvm_arch.feature.auth.data.mapper

import com.example.android_mvvm_arch.feature.auth.data.remote.dto.LoginResponseDto
import com.example.android_mvvm_arch.feature.auth.domain.model.LoginCredentials
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthMapperTest {

    private val mapper = AuthMapper()

    @Test
    fun `toLoginRequestDto maps credentials`() {
        val dto = mapper.toLoginRequestDto(LoginCredentials("a@b.com", "secret"))

        assertEquals("a@b.com", dto.email)
        assertEquals("secret", dto.password)
    }

    @Test
    fun `toDomain maps login response`() {
        val domain = mapper.toDomain(
            LoginResponseDto(
                accessToken = "access",
                refreshToken = "refresh",
                expiresIn = 3600,
                tokenType = "Bearer",
            ),
        )

        assertEquals("access", domain.accessToken)
        assertEquals("refresh", domain.refreshToken)
        assertEquals(3600, domain.expiresInSeconds)
        assertEquals("Bearer", domain.tokenType)
    }
}
