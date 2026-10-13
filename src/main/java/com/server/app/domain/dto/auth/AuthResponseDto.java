package com.server.app.domain.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDto {
  private String token;
  private String refreshToken;
  private ProfileResponseDto data;
}
