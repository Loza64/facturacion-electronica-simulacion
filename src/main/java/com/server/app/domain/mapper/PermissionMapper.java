package com.server.app.domain.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.server.app.domain.dto.permission.PermissionCreatedDto;
import com.server.app.domain.dto.permission.PermissionResponseDto;
import com.server.app.domain.dto.permission.PermissionUpdateDto;
import com.server.app.domain.model.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

  PermissionResponseDto toResponseDto(Permission p);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "title", ignore = true)
  Permission toCreateSeeder(PermissionCreatedDto p);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", ignore = true)
  void updateEntity(PermissionUpdateDto dto, @MappingTarget Permission entity);
}