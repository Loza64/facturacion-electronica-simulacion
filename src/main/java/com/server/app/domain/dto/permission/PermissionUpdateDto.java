package com.server.app.domain.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionUpdateDto(
    @NotBlank(message = "El titulo es obligatorio") @Size(min = 3, max = 100) String title) {
}