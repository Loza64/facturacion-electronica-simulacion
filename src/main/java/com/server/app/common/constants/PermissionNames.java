package com.server.app.common.constants;

import java.util.List;

public class PermissionNames {
  public static final String USER_CREATE = "USER_CREATE";
  public static final String USER_READ = "USER_READ";
  public static final String USER_UPDATE = "USER_UPDATE";
  public static final String USER_DELETE = "USER_DELETE";

  public static final String ROLE_CREATE = "ROLE_CREATE";
  public static final String ROLE_READ = "ROLE_READ";
  public static final String ROLE_UPDATE = "ROLE_UPDATE";
  public static final String ROLE_DELETE = "ROLE_DELETE";

  public static final String PERMISSION_READ = "PERMISSION_READ";
  public static final String PERMISSION_UPDATE = "PERMISSION_UPDATE";

  public static final List<String> HIERARCHY = List.of(
      USER_CREATE,
      USER_DELETE,
      USER_READ,
      USER_UPDATE,
      ROLE_CREATE,
      ROLE_DELETE,
      ROLE_READ,
      ROLE_UPDATE,
      PERMISSION_READ,
      PERMISSION_UPDATE);

  private PermissionNames() {
  }
}
