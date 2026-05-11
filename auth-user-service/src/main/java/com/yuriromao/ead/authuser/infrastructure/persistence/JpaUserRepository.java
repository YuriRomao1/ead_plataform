package com.yuriromao.ead.authuser.infrastructure.persistence;

import com.yuriromao.ead.authuser.application.port.UserRepository;
import com.yuriromao.ead.authuser.domain.model.User;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter for the application user repository port.
 *
 * <p>This class is the only place where the application repository contract is translated to Spring
 * Data JPA entities for the auth-user-service database.
 */
@Repository
public class JpaUserRepository implements UserRepository {

  private final UserJpaRepository userJpaRepository;

  public JpaUserRepository(UserJpaRepository userJpaRepository) {
    this.userJpaRepository = userJpaRepository;
  }

  @Override
  public User save(User user) {
    Objects.requireNonNull(user, "user must not be null");
    userJpaRepository.save(UserJpaEntity.fromDomain(user));
    return user;
  }

  @Override
  public boolean existsByEmail(String email) {
    Objects.requireNonNull(email, "email must not be null");
    return userJpaRepository.existsByEmail(email);
  }
}
