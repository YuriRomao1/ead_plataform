package com.yuriromao.ead.authuser.application.port;

/**
 * Application port for password hashing.
 *
 * <p>Keeping hashing behind this boundary lets the application layer enforce secure password
 * storage without depending on a specific crypto library or algorithm.
 */
public interface PasswordHasher {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String passwordHash);
}
