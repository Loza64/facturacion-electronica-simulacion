package com.server.app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.app.common.constants.PermissionNames;
import com.server.app.common.pagination.PaginationResponse;
import com.server.app.domain.dto.role.RoleCreateDto;
import com.server.app.domain.dto.role.RoleResponseDto;
import com.server.app.domain.dto.role.RoleUpdateDto;
import com.server.app.service.RoleServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

  private final RoleServiceImpl roleService;

  @GetMapping
  @PreAuthorize("hasAuthority('" + PermissionNames.ROLE_READ + "')")
  public ResponseEntity<PaginationResponse<RoleResponseDto>> findAll(
      @RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "false") Boolean deleted,
      Pageable pageable) {
    return ResponseEntity.ok(roleService.findAll(search, deleted, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('" + PermissionNames.ROLE_READ + "')")
  public ResponseEntity<RoleResponseDto> findById(@PathVariable Long id) {
    return ResponseEntity.ok(roleService.findById(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('" + PermissionNames.ROLE_CREATE + "')")
  public ResponseEntity<RoleResponseDto> create(@Valid @RequestBody RoleCreateDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(dto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('" + PermissionNames.ROLE_UPDATE + "')")
  public ResponseEntity<RoleResponseDto> update(
      @PathVariable Long id,
      @Valid @RequestBody RoleUpdateDto dto) {
    return ResponseEntity.ok(roleService.update(id, dto));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('" + PermissionNames.ROLE_DELETE + "')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    roleService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/restore")
  @PreAuthorize("hasAuthority('" + PermissionNames.ROLE_DELETE + "')")
  public ResponseEntity<Void> restore(@PathVariable Long id) {
    roleService.restore(id);
    return ResponseEntity.noContent().build();
  }
}
