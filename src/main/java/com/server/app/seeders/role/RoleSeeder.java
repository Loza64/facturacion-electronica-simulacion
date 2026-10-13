package com.server.app.seeders.role;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.server.app.common.constants.RoleNames;
import com.server.app.service.RoleServiceImpl;

@Component
@Order(1)
public class RoleSeeder implements ApplicationListener<ApplicationReadyEvent> {

  private final RoleServiceImpl roleService;

  public RoleSeeder(RoleServiceImpl roleService) {
    this.roleService = roleService;
  }

  @Override
  public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
    roleService.createAllIfNotExists(List.of(RoleNames.SUPER_ADMIN, RoleNames.ADMIN, RoleNames.CLIENT));
    roleService.assignAllPermissions(RoleNames.SUPER_ADMIN);
  }
}