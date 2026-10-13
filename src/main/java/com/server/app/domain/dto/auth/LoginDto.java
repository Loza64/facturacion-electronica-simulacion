package com.server.app.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(
    @NotBlank(message = "El Usuario es obligatorio") String username,
    @NotBlank(message = "La contraseña es obligatoria") String password) {
}