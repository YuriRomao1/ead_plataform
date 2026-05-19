package com.yuriromao.ead.authuser.infrastructure.persistence;

import com.yuriromao.ead.authuser.application.exception.UserEmailAlreadyExistsException;
import com.yuriromao.ead.authuser.application.port.UserRepository;
import com.yuriromao.ead.authuser.domain.model.User;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter for the application user repository port.
 *
 * <p>This class is the only place where the application repository contract is translated to Spring
 * Data JPA entities for the auth-user-service database.
 */
@Repository
public class JpaUserRepository implements UserRepository {

  private static final String EMAIL_UNIQUE_CONSTRAINT = "users_email_key";

  private final UserJpaRepository userJpaRepository;

  public JpaUserRepository(UserJpaRepository userJpaRepository) {
    this.userJpaRepository = userJpaRepository;
  }

  /** Saves the domain user by converting it to the JPA entity shape used by the database schema. */
  @Override
  public User save(User user) {
    Objects.requireNonNull(user, "user must not be null");

    try {
      userJpaRepository.saveAndFlush(UserJpaEntity.fromDomain(user));
    } catch (DataIntegrityViolationException exception) {
      if (isEmailUniqueConstraintViolation(exception)) {
        throw new UserEmailAlreadyExistsException(user.getEmail(), exception);
      }

      throw exception;
    }

    return user;
  }

  /** Delegates email uniqueness checks to the database-backed Spring Data repository. */
  @Override
  public boolean existsByEmail(String email) {
    Objects.requireNonNull(email, "email must not be null");
    return userJpaRepository.existsByEmail(email);
  }

  private static boolean isEmailUniqueConstraintViolation(
      DataIntegrityViolationException exception) {
    String message =
        Objects.toString(exception.getMostSpecificCause().getMessage(), "")
            .toLowerCase(Locale.ROOT);

    return message.contains(EMAIL_UNIQUE_CONSTRAINT);
  }
}
