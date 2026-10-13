package com.server.app.domain.dto.role;

import java.util.Set;

import com.server.app.domain.dto.base.IdDto;

import jakarta.validation.constraints.NotBlank;

public record RoleCreateDto(@NotBlank String name, Boolean active, Set<IdDto> permissions) {
}