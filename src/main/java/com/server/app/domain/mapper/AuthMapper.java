package com.server.app.domain.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.server.app.domain.dto.auth.ProfileResponseDto;
import com.server.app.domain.dto.auth.SignUpDto;
import com.server.app.domain.dto.auth.UpdateProfileDto;
import com.server.app.domain.model.User;

@Mapper(componentModel = "spring", uses = { RoleMapper.class })
public interface AuthMapper {
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "blocked", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "password", ignore = true)
  User updateEntity(UpdateProfileDto dto, @MappingTarget User entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "blocked", ignore = true)
  @Mapping(target = "role", ignore = true)
  User toSignUp(SignUpDto dto);

  ProfileResponseDto toResponseDto(User u);
}
