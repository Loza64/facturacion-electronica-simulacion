package com.server.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.app.common.exceptions.UnauthorizedException;
import com.server.app.domain.model.RefreshToken;
import com.server.app.domain.model.User;
import com.server.app.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final long REFRESH_TOKEN_TTL_DAYS = 7;

  private final RefreshTokenRepository refreshTokenRepository;

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
    }
  }

  public String issue(User user) {
    return issue(user, UUID.randomUUID().toString());
  }

  @Transactional
  public String issue(User user, String familyId) {
    String refreshTokenValue = UUID.randomUUID().toString();

    RefreshToken entity = RefreshToken.builder()
        .token(hash(refreshTokenValue))
        .familyId(familyId)
        .user(user)
        .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS))
        .build();

    refreshTokenRepository.save(entity);

    return refreshTokenValue;
  }

  @Transactional
  public RotationResult rotate(String incomingToken) {
    String hashed = hash(incomingToken);
    RefreshToken stored = refreshTokenRepository.findByToken(hashed)
        .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

    if (stored.isUsed() || stored.isRevoked()) {
      revokeFamily(stored.getFamilyId());
      throw new UnauthorizedException("Reuso de refresh token detectado. Todas las sesiones fueron revocadas.");
    }

    if (stored.getExpiresAt().isBefore(Instant.now())) {
      throw new UnauthorizedException("Refresh token expirado");
    }

    User user = stored.getUser();
    if (user.getDeletedAt() != null || user.isBlocked()) {
      throw new UnauthorizedException("Cuenta inactiva o bloqueada");
    }

    stored.setUsed(true);
    refreshTokenRepository.save(stored);

    String newRefreshToken = issue(user, stored.getFamilyId());

    return new RotationResult(newRefreshToken, user);
  }

  @Transactional
  public void revoke(String incomingToken) {
    String hashed = hash(incomingToken);
    refreshTokenRepository.findByToken(hashed)
        .ifPresent(stored -> revokeFamily(stored.getFamilyId()));
  }

  private void revokeFamily(String familyId) {
    refreshTokenRepository.revokeAllByFamilyId(familyId);
  }

  public record RotationResult(String refreshToken, User user) {
  }
}