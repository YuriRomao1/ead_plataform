package com.yuriromao.ead.authuser.infrastructure.web;

import com.yuriromao.ead.authuser.application.usecase.CreateUserResult;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.domain.model.UserStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Public HTTP response returned after a user is created.
 *
 * <p>The response intentionally contains only public user data and excludes password and password
 * hash fields.
 */
public record UserResponse(
    UUID id, String name, String email, UserStatus status, Set<UserRole> roles, Instant createdAt) {

  public UserResponse {
    id = Objects.requireNonNull(id, "id must not be null");
    name = Objects.requireNonNull(name, "name must not be null");
    email = Objects.requireNonNull(email, "email must not be null");
    status = Objects.requireNonNull(status, "status must not be null");
    roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  /** Converts the application use-case result to the HTTP response contract. */
  static UserResponse from(CreateUserResult result) {
    Objects.requireNonNull(result, "result must not be null");

    return new UserResponse(
        result.id(),
        result.name(),
        result.email(),
        result.status(),
        result.roles(),
        result.createdAt());
  }
}
