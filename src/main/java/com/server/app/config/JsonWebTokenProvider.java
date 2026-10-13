package com.server.app.config;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.server.app.domain.dto.auth.ProfileResponseDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JsonWebTokenProvider {
  @Value("${security.jwt.expiration-time}")
  private int tokenTime;

  @Value("${security.jwt.secret-key}")
  private String tokenSecret;

  private SecretKey getTokenKey() {
    byte[] keyBytes = Decoders.BASE64.decode(tokenSecret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String createToken(ProfileResponseDto user) {
    Map<String, Object> json = new LinkedHashMap<>();
    json.put("id", user.id());
    return Jwts.builder()
        .claims(json)
        .signWith(getTokenKey())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + tokenTime))
        .compact();
  }

  public Claims extracClaims(String token) {
    try {
      return Jwts.parser()
          .verifyWith(getTokenKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException e) {
      return null;
    }
  }

  public Long extractIdUser(String token) {
    Claims claims = extracClaims(token);
    if (claims == null) {
      return null;
    }
    return claims.get("id", Long.class);
  }

  public boolean isTokenExpired(String token) {
    Claims claims = extracClaims(token);
    return claims == null || claims.getExpiration().before(new Date());
  }
}
