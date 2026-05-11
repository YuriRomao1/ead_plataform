package com.yuriromao.ead.authuser.application.usecase;

import com.yuriromao.ead.authuser.domain.model.User;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.domain.model.UserStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CreateUserResult(
    UUID id, String name, String email, UserStatus status, Set<UserRole> roles, Instant createdAt) {

  public CreateUserResult {
    id = Objects.requireNonNull(id, "id must not be null");
    name = Objects.requireNonNull(name, "name must not be null");
    email = Objects.requireNonNull(email, "email must not be null");
    status = Objects.requireNonNull(status, "status must not be null");
    roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  static CreateUserResult from(User user) {
    Objects.requireNonNull(user, "user must not be null");

    return new CreateUserResult(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getStatus(),
        user.getRoles(),
        user.getCreatedAt());
  }
}
