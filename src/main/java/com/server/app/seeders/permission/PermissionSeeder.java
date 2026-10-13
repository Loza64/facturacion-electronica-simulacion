package com.server.app.seeders.permission;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.server.app.common.constants.PermissionNames;
import com.server.app.service.PermissionService;

@Component
@Order(0)
public class PermissionSeeder implements ApplicationListener<ApplicationReadyEvent> {

  private final PermissionService permissionService;

  public PermissionSeeder(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
      PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @Override
  public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
    List<String> candidates = PermissionNames.HIERARCHY;
    permissionService.createAllIfNotExists(candidates);
  }
}