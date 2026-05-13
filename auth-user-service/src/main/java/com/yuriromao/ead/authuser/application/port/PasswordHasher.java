package com.yuriromao.ead.authuser.application.port;

/**
 * Application port for password hashing.
 *
 * <p>Keeping hashing behind this boundary lets the application layer enforce secure password
 * storage without depending on a specific crypto library or algorithm.
 */
public interface PasswordHasher {

  /** Hashes a raw password before it is persisted. */
  String hash(String rawPassword);

  /** Checks whether a raw password matches a stored password hash. */
  boolean matches(String rawPassword, String passwordHash);
}
