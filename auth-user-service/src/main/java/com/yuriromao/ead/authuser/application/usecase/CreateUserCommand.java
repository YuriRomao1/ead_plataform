package com.yuriromao.ead.authuser.application.usecase;

import com.yuriromao.ead.authuser.domain.model.UserRole;
import java.util.Objects;
import java.util.Set;

/**
 * Application command for creating a user.
 *
 * <p>The command is created by inbound adapters and validates the minimum data required by the use
 * case before domain creation begins.
 */
public record CreateUserCommand(String name, String email, String password, Set<UserRole> roles) {

  public CreateUserCommand {
    name = requireText(name, "name");
    email = requireText(email, "email");
    password = requireText(password, "password");
    roles = requireRoles(roles);
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be null or empty");
    }

    return value;
  }

  private static Set<UserRole> requireRoles(Set<UserRole> roles) {
    if (roles == null || roles.isEmpty()) {
      throw new IllegalArgumentException("roles must not be null or empty");
    }

    if (roles.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("roles must not contain null values");
    }

    return Set.copyOf(roles);
  }
}
