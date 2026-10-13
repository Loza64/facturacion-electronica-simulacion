package com.server.app.domain.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.server.app.domain.dto.user.UserCreateDto;
import com.server.app.domain.dto.user.UserResponseDto;
import com.server.app.domain.dto.user.UserUpdateDto;
import com.server.app.domain.model.User;

@Mapper(componentModel = "spring", uses = { RoleMapper.class })
public interface UserMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "blocked", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "password", ignore = true)
  User toEntity(UserCreateDto dto);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "role", ignore = true)
  void updateEntity(UserUpdateDto dto, @MappingTarget User entity);

  UserResponseDto toResponseDto(User u);

  @Mapping(target = "role", qualifiedByName = "toSummaryDto")
  UserResponseDto toListResponseDto(User u);
}