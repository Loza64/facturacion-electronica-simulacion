package com.server.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.app.domain.model.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
  List<Permission> findByNameIn(List<String> names);
}
