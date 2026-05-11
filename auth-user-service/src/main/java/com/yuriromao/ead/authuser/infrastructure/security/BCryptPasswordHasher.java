package com.yuriromao.ead.authuser.infrastructure.security;

import com.yuriromao.ead.authuser.application.port.PasswordHasher;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt implementation of the password hashing port.
 *
 * <p>This adapter applies the strategy accepted in ADR-002 while keeping BCrypt details outside the
 * application use cases.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

  private final PasswordEncoder passwordEncoder;

  @Autowired
  public BCryptPasswordHasher(
      @Value("${auth-user-service.security.password.bcrypt-strength:10}") int strength) {
    this(new BCryptPasswordEncoder(strength));
  }

  BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
    this.passwordEncoder =
        Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
  }

  @Override
  public String hash(String rawPassword) {
    return passwordEncoder.encode(requireText(rawPassword, "rawPassword"));
  }

  @Override
  public boolean matches(String rawPassword, String passwordHash) {
    return passwordEncoder.matches(
        requireText(rawPassword, "rawPassword"), requireText(passwordHash, "passwordHash"));
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be null or empty");
    }

    return value;
  }
}
