package com.server.app.domain.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.server.app.domain.dto.role.RoleCreateDto;
import com.server.app.domain.dto.role.RoleResponseDto;
import com.server.app.domain.dto.role.RoleUpdateDto;
import com.server.app.domain.model.Role;

@Mapper(componentModel = "spring", uses = { PermissionMapper.class })
public interface RoleMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "permissions", ignore = true)
  Role toEntity(RoleCreateDto dto);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "permissions", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(RoleUpdateDto dto, @MappingTarget Role entity);

  RoleResponseDto toResponseDto(Role r);

  @Named("toSummaryDto")
  @Mapping(target = "permissions", ignore = true)
  RoleResponseDto toSummaryDto(Role r);
}