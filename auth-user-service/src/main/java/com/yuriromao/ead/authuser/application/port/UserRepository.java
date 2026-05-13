package com.yuriromao.ead.authuser.application.port;

import com.yuriromao.ead.authuser.domain.model.User;

/**
 * Application port for user persistence owned by auth-user-service.
 *
 * <p>Use cases depend on this boundary so persistence details stay in infrastructure and no other
 * service database is accessed directly.
 */
public interface UserRepository {

  /** Persists a user aggregate in the auth-user-service owned database. */
  User save(User user);

  /** Checks the email uniqueness invariant before creating a new user. */
  boolean existsByEmail(String email);
}
