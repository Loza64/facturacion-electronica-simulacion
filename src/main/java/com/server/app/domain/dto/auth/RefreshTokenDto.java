package com.server.app.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenDto(
    @NotBlank(message = "El refresh token es obligatorio") String refreshToken) {
}
