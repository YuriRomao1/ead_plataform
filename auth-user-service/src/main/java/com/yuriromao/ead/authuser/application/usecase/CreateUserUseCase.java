package com.yuriromao.ead.authuser.application.usecase;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedEventFactory;
import com.yuriromao.ead.authuser.application.exception.UserEmailAlreadyExistsException;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;
import com.yuriromao.ead.authuser.application.port.PasswordHasher;
import com.yuriromao.ead.authuser.application.port.UserRepository;
import com.yuriromao.ead.authuser.domain.model.User;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates user creation inside the application layer.
 *
 * <p>The use case checks email uniqueness, delegates password hashing to a port, and persists the
 * user without depending on HTTP, JPA, or messaging details.
 */
@Service
public class CreateUserUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserUseCase.class);

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final DomainEventRecorder domainEventRecorder;

  public CreateUserUseCase(
      UserRepository userRepository,
      PasswordHasher passwordHasher,
      DomainEventRecorder domainEventRecorder) {
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
    this.domainEventRecorder =
        Objects.requireNonNull(domainEventRecorder, "domainEventRecorder must not be null");
  }

  @Transactional
  public CreateUserResult execute(CreateUserCommand command) {
    Objects.requireNonNull(command, "command must not be null");

    if (userRepository.existsByEmail(command.email())) {
      throw new UserEmailAlreadyExistsException(command.email());
    }

    String passwordHash = passwordHasher.hash(command.password());
    User user = User.createNew(command.name(), command.email(), passwordHash, command.roles());
    User savedUser = userRepository.save(user);
    UserCreatedEvent event = UserCreatedEventFactory.from(savedUser);

    LOGGER.info(
        "Recording UserCreated event in outbox. eventId={} userId={}",
        event.eventId(),
        savedUser.getId());
    domainEventRecorder.record(event);
    LOGGER.info(
        "Recorded UserCreated event in outbox. eventId={} userId={}",
        event.eventId(),
        savedUser.getId());

    return CreateUserResult.from(savedUser);
  }
}
