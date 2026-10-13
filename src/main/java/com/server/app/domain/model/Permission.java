package com.server.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = { "path", "method" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = true, unique = false)
  private String title;
}