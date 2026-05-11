package com.yuriromao.ead.authuser.application.usecase;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yuriromao.ead.authuser.application.exception.UserEmailAlreadyExistsException;
import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedEventFactory;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import com.yuriromao.ead.authuser.application.port.PasswordHasher;
import com.yuriromao.ead.authuser.application.port.UserRepository;
import com.yuriromao.ead.authuser.domain.model.User;

/**
 * Coordinates user creation inside the application layer.
 *
 * The use case checks email uniqueness, delegates password hashing to a port,
 * and persists the user without depending on HTTP, JPA, or messaging details.
 */
@Service
public class CreateUserUseCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserUseCase.class);

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;
	private final EventPublisher eventPublisher;

	public CreateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher, EventPublisher eventPublisher) {
		this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
		this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
		this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
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

		LOGGER.info("Publishing UserCreated event. eventId={} userId={}", event.eventId(), savedUser.getId());
		eventPublisher.publish(event);
		LOGGER.info("Published UserCreated event. eventId={} userId={}", event.eventId(), savedUser.getId());

		return CreateUserResult.from(savedUser);
	}
}
