package com.server.app.seeders.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.server.app.common.constants.RoleNames;
import com.server.app.domain.model.Role;
import com.server.app.domain.model.User;
import com.server.app.repository.RoleRepository;
import com.server.app.repository.UserRepository;

@Component
@Order(2)
public class SuperAdminSeeder implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${security.super-admin.username}")
  private String superAdminUsername;

  @Value("${security.super-admin.email}")
  private String superAdminEmail;

  @Value("${security.super-admin.password}")
  private String superAdminPassword;

  public SuperAdminSeeder(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {

    if (userRepository.existsByRole_Name(RoleNames.SUPER_ADMIN)) {
      log.info("Ya existe un usuario SUPER_ADMIN, se omite la creación automática.");
      return;
    }

    Role superAdminRole = roleRepository.findByName(RoleNames.SUPER_ADMIN)
        .orElseThrow(() -> new IllegalStateException("El rol SUPER_ADMIN no existe todavía. RoleSeeder debe ejecutarse primero (@Order)."));

    User superAdmin = User.builder()
        .username(superAdminUsername)
        .name("Super")
        .surname("Admin")
        .email(superAdminEmail)
        .password(passwordEncoder.encode(superAdminPassword))
        .blocked(false)
        .role(superAdminRole)
        .build();

    userRepository.save(superAdmin);

    log.warn(
        "Usuario SUPER_ADMIN creado automáticamente (username='{}'). Cambia la contraseña por defecto de inmediato.",
        superAdminUsername);
  }
}