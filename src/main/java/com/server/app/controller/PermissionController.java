package com.server.app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.app.common.constants.PermissionNames;
import com.server.app.common.pagination.PaginationResponse;
import com.server.app.domain.dto.permission.PermissionResponseDto;
import com.server.app.domain.dto.permission.PermissionUpdateDto;
import com.server.app.service.PermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Valid
public class PermissionController {

  private final PermissionService permissionService;

  @GetMapping
  @PreAuthorize("hasAuthority('" + PermissionNames.PERMISSION_READ + "')")
  public ResponseEntity<PaginationResponse<PermissionResponseDto>> findAll(Pageable pageable) {
    return ResponseEntity.ok(permissionService.findAll(pageable));
  }

  @PutMapping("{id}")
  @PreAuthorize("hasAuthority('" + PermissionNames.PERMISSION_UPDATE + "')")
  public ResponseEntity<PermissionResponseDto> update(@PathVariable Long id, @RequestBody PermissionUpdateDto entity) {
    return ResponseEntity.ok(permissionService.update(id, entity));
  }
}
