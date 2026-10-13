package com.server.app.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.app.common.constants.RoleNames;
import com.server.app.common.exceptions.BadRequestException;
import com.server.app.common.exceptions.ConfictException;
import com.server.app.common.exceptions.ForbiddenException;
import com.server.app.common.exceptions.NotFoundException;
import com.server.app.common.exceptions.UnauthorizedException;
import com.server.app.config.JsonWebTokenProvider;
import com.server.app.domain.dto.auth.AuthResponseDto;
import com.server.app.domain.dto.auth.LoginDto;
import com.server.app.domain.dto.auth.ProfileResponseDto;
import com.server.app.domain.dto.auth.SignUpDto;
import com.server.app.domain.dto.auth.UpdatePasswordDto;
import com.server.app.domain.dto.auth.UpdateProfileDto;
import com.server.app.domain.mapper.AuthMapper;
import com.server.app.domain.model.Role;
import com.server.app.domain.model.User;
import com.server.app.repository.RoleRepository;
import com.server.app.repository.UserRepository;
import com.server.app.service.RefreshTokenService.RotationResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final AuthMapper authMapper;
  private final JsonWebTokenProvider jwt;
  private final RefreshTokenService refreshTokenService;

  private static final String DEFAULT_ROLE_NAME = RoleNames.ADMIN;

  @Transactional
  public AuthResponseDto login(LoginDto login) {
    User profile = userRepository.findByUsername(login.username())
        .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

    if (!passwordEncoder.matches(login.password(), profile.getPassword())) {
      throw new UnauthorizedException("La contraseña es incorrecta");
    }

    if (profile.isBlocked() || profile.getDeletedAt() != null) {
      throw new UnauthorizedException("Cuenta inactiva o bloqueada");
    }

    ProfileResponseDto response = authMapper.toResponseDto(profile);
    String refreshToken = refreshTokenService.issue(profile);
    return new AuthResponseDto(jwt.createToken(response), refreshToken, response);
  }

  @Transactional
  public AuthResponseDto signup(SignUpDto dto) {
    if (userRepository.existsByEmail(dto.email()))
      throw new ConfictException("El email ya está registrado");
    if (userRepository.existsByUsername(dto.username()))
      throw new ConfictException("El nombre de usuario ya está ocupado");

    User data = authMapper.toSignUp(dto);
    data.setPassword(passwordEncoder.encode(dto.password()));

    Role defaultRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
        .orElseThrow(() -> new NotFoundException("Rol por defecto no configurado"));
    data.setRole(defaultRole);

    User savedUser = userRepository.save(data);
    ProfileResponseDto response = authMapper.toResponseDto(savedUser);
    String refreshToken = refreshTokenService.issue(savedUser);
    return new AuthResponseDto(jwt.createToken(response), refreshToken, response);
  }

  @Transactional
  public AuthResponseDto refresh(String incomingToken) {
    RotationResult result = refreshTokenService.rotate(incomingToken);
    ProfileResponseDto response = authMapper.toResponseDto(result.user());
    return new AuthResponseDto(jwt.createToken(response), result.refreshToken(), response);
  }

  @Transactional
  public void logout(String incomingToken) {
    refreshTokenService.revoke(incomingToken);
  }

  public ProfileResponseDto profile(Long id) {
    User profile = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Profile not found"));
    return authMapper.toResponseDto(profile);
  }

  @Transactional
  public ProfileResponseDto updateProfile(Long id, UpdateProfileDto dto) {
    User profile = validateAndGetUser(id);
    authMapper.updateEntity(dto, profile);
    return authMapper.toResponseDto(userRepository.save(profile));
  }

  @Transactional
  public ProfileResponseDto updatePassword(Long id, UpdatePasswordDto dto) {
    User profile = validateAndGetUser(id);
    if (!passwordEncoder.matches(dto.currentPassword(), profile.getPassword()))
      throw new ForbiddenException("La contraseña actual es incorrecta");

    if (!dto.newPassword().equals(dto.confirmNewPassword()))
      throw new BadRequestException("Las contraseñas nuevas no coinciden");

    if (passwordEncoder.matches(dto.newPassword(), profile.getPassword()))
      throw new BadRequestException("La nueva contraseña no puede ser igual a la anterior");

    profile.setPassword(passwordEncoder.encode(dto.newPassword()));
    return authMapper.toResponseDto(userRepository.save(profile));
  }

  private User validateAndGetUser(Long id) {
    User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    if (user.isBlocked())
      throw new UnauthorizedException("Cuenta bloqueada");
    if (user.getDeletedAt() != null)
      throw new UnauthorizedException("Cuenta eliminada");

    return user;
  }
}