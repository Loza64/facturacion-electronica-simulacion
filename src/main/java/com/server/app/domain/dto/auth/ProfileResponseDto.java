package com.server.app.domain.dto.auth;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.server.app.domain.dto.role.RoleResponseDto;

public record ProfileResponseDto(
    @JsonIgnore Long id,
    String username,
    String name,
    String surname,
    String email,
    boolean blocked,
    RoleResponseDto role,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt) {
}