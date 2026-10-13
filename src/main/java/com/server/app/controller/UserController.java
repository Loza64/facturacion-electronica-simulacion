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
import com.server.app.domain.dto.user.UserCreateDto;
import com.server.app.domain.dto.user.UserResponseDto;
import com.server.app.domain.dto.user.UserUpdateDto;
import com.server.app.service.UserServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserServiceImpl userService;

  @GetMapping
  @PreAuthorize("hasAuthority('" + PermissionNames.USER_READ + "')")
  public ResponseEntity<PaginationResponse<UserResponseDto>> findAll(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long role,
      @RequestParam(required = false, defaultValue = "false") Boolean deleted,
      Pageable pageable) {
    return ResponseEntity.ok(userService.findAll(search, role, deleted, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('" + PermissionNames.USER_READ + "')")
  public ResponseEntity<UserResponseDto> findById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('" + PermissionNames.USER_CREATE + "')")
  public ResponseEntity<UserResponseDto> create(@Valid @RequestBody UserCreateDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('" + PermissionNames.USER_UPDATE + "')")
  public ResponseEntity<UserResponseDto> update(
      @PathVariable Long id,
      @Valid @RequestBody UserUpdateDto dto) {
    return ResponseEntity.ok(userService.update(id, dto));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('" + PermissionNames.USER_DELETE + "')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/restore")
  @PreAuthorize("hasAuthority('" + PermissionNames.USER_DELETE + "')")
  public ResponseEntity<Void> restore(@PathVariable Long id) {
    userService.restore(id);
    return ResponseEntity.noContent().build();
  }
}
