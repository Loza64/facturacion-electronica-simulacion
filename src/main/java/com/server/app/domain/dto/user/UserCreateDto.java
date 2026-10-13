package com.server.app.domain.dto.user;

import com.server.app.domain.dto.base.IdDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreateDto(
    @NotBlank String username,
    String name,
    String surname,
    @Email @NotBlank String email,
    @NotBlank String password,
    IdDto role) {
}