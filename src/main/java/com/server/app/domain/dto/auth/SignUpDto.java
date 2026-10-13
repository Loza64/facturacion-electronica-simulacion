package com.server.app.domain.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpDto(
    @NotBlank(message = "El nombre de usuario es obligatorio") String username,

    @NotBlank(message = "El nombre es obligatorio") String name,

    @NotBlank(message = "El nombre es obligatorio") String surname,

    @Email(message = "El formato del email es inválido") @NotBlank(message = "El email es obligatorio") String email,

    @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password) {
}