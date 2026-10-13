package com.server.app.domain.dto.user;

import com.server.app.domain.dto.base.IdDto;

import jakarta.validation.constraints.Email;

public record UserUpdateDto(String username, String name, String surname, boolean blocked, @Email String email,
    IdDto role) {
}