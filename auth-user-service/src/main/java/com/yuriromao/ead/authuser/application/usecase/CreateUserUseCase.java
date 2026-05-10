package com.yuriromao.ead.authuser.application.usecase;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yuriromao.ead.authuser.application.exception.UserEmailAlreadyExistsException;
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

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;

	public CreateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
		this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
		this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
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

		return CreateUserResult.from(savedUser);
	}
}
