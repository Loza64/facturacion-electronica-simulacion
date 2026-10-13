package com.server.app.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.server.app.common.pagination.PaginationMapper;
import com.server.app.common.pagination.PaginationResponse;
import com.server.app.domain.dto.permission.PermissionCreatedDto;
import com.server.app.domain.dto.permission.PermissionResponseDto;
import com.server.app.domain.dto.permission.PermissionUpdateDto;
import com.server.app.domain.mapper.PermissionMapper;
import com.server.app.domain.model.Permission;
import com.server.app.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.server.app.common.exceptions.NotFoundException;

@Service
@RequiredArgsConstructor
public class PermissionService {

  private final PermissionRepository permissionRepository;
  private final PermissionMapper permissionMapper;
  private final PaginationMapper paginationMapper;

  @Transactional
  public void createAllIfNotExists(List<String> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return;
    }

    Set<String> existingNames = permissionRepository.findByNameIn(candidates).stream()
        .map(Permission::getName)
        .collect(Collectors.toSet());

    List<Permission> listToCreate = candidates.stream()
        .filter(name -> !existingNames.contains(name))
        .map(name -> permissionMapper.toCreateSeeder(new PermissionCreatedDto(name)))
        .toList();

    if (!listToCreate.isEmpty()) {
      permissionRepository.saveAll(listToCreate);
    }
  }

  @Transactional
  public PermissionResponseDto update(Long id, PermissionUpdateDto dto) {
    Permission permission = permissionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Permiso no encontrado con ID: " + id));

    permissionMapper.updateEntity(dto, permission);
    return permissionMapper.toResponseDto(permissionRepository.save(permission));
  }

  @Transactional(readOnly = true)
  public PaginationResponse<PermissionResponseDto> findAll(Pageable pageable) {
    Page<Permission> page = permissionRepository.findAll(pageable);
    return paginationMapper.toPaginationResponse(page.map(permissionMapper::toResponseDto));
  }
}