package com.server.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.server.app.domain.model.RefreshToken;

public interface RefreshTokenRepository
    extends JpaRepository<RefreshToken, Long>, JpaSpecificationExecutor<RefreshToken> {

  Optional<RefreshToken> findByToken(String token);

  @Modifying
  @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
  void revokeAllByFamilyId(@Param("familyId") String familyId);
}