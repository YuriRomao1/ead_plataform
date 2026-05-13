package com.yuriromao.ead.authuser.infrastructure.persistence;

import com.yuriromao.ead.authuser.domain.model.User;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.domain.model.UserStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA mapping for the users table and its role collection.
 *
 * <p>This persistence shape is intentionally separate from the domain aggregate so database
 * annotations and collection mapping concerns do not leak into the domain model.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private UserStatus status;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id", nullable = false))
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Set<UserRole> roles = new HashSet<>();

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected UserJpaEntity() {}

  private UserJpaEntity(
      UUID id,
      String name,
      String email,
      String passwordHash,
      UserStatus status,
      Set<UserRole> roles,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.status = status;
    this.roles = new HashSet<>(roles);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Converts a domain user into the persistence entity written by Spring Data JPA. */
  static UserJpaEntity fromDomain(User user) {
    return new UserJpaEntity(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getStatus(),
        user.getRoles(),
        toDatabaseTimestamp(user.getCreatedAt()),
        toDatabaseTimestamp(user.getUpdatedAt()));
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
    return Set.copyOf(roles);
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  private static LocalDateTime toDatabaseTimestamp(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
