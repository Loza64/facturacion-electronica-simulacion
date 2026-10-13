package com.server.app.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordDto(
    @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,

    @NotBlank(message = "La nueva contraseña es obligatoria") @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres") String newPassword,

    @NotBlank(message = "Debes confirmar la nueva contraseña") String confirmNewPassword) {
}