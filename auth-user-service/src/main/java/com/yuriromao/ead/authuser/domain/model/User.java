package com.yuriromao.ead.authuser.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class User {

  private final UUID id;
  private final String name;
  private final String email;
  private final String passwordHash;
  private final UserStatus status;
  private final Set<UserRole> roles;
  private final Instant createdAt;
  private final Instant updatedAt;

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

  private User(
      UUID id,
      String name,
      String email,
      String passwordHash,
      UserStatus status,
      Set<UserRole> roles,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.name = requireText(name, "name");
    this.email = requireValidEmail(email);
    this.passwordHash = requireText(passwordHash, "passwordHash");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.roles = requireRoles(roles);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }

  public static User createNew(
      String name, String email, String passwordHash, Set<UserRole> roles) {
    Instant now = Instant.now();

    return new User(
        UUID.randomUUID(), name, email, passwordHash, UserStatus.ACTIVE, roles, now, now);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserStatus getStatus() {
    return status;
  }

  public Set<UserRole> getRoles() {
    return roles;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be null or empty");
    }

    return value;
  }

  private static String requireValidEmail(String email) {
    requireText(email, "email");

    if (!EMAIL_PATTERN.matcher(email).matches()) {
      throw new IllegalArgumentException("email must be valid");
    }

    return email;
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
