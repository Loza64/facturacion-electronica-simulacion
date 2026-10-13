package com.server.app.domain.dto.role;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

import com.server.app.domain.dto.base.IdDto;

public record RoleUpdateDto(
    @NotBlank String name,
    Boolean active,
    Set<IdDto> permissions) {
}