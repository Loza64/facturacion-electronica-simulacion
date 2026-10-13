package com.server.app.domain.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileDto(
    @NotBlank(message = "El nombre de usuario es obligatorio") String username,

    @NotBlank(message = "El nombre es obligatorio") String name,

    @NotBlank(message = "El apellido es obligatorio") String surname,

    @Email(message = "El formato del email es inválido") @NotBlank(message = "El email es obligatorio") String email) {
}