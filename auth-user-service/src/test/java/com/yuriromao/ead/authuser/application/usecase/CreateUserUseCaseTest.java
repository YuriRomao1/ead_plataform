package com.yuriromao.ead.authuser.application.usecase;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.yuriromao.ead.authuser.application.exception.UserEmailAlreadyExistsException;
import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import com.yuriromao.ead.authuser.application.port.PasswordHasher;
import com.yuriromao.ead.authuser.application.port.UserRepository;
import com.yuriromao.ead.authuser.domain.model.User;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.domain.model.UserStatus;

class CreateUserUseCaseTest {

	private static final String NAME = "User Name";
	private static final String EMAIL = "user@email.com";
	private static final String PASSWORD = "plainPassword";
	private static final String PASSWORD_HASH = "$2a$10$hashedPassword";

	private final FakeUserRepository userRepository = new FakeUserRepository();
	private final FakePasswordHasher passwordHasher = new FakePasswordHasher(PASSWORD_HASH);
	private final FakeEventPublisher eventPublisher = new FakeEventPublisher(userRepository);
	private final CreateUserUseCase createUserUseCase = new CreateUserUseCase(userRepository, passwordHasher, eventPublisher);

	@Test
	void shouldCreateValidUser() {
		CreateUserResult result = createUserUseCase.execute(validCommand());

		assertAll(
				() -> assertNotNull(result.id()),
				() -> assertEquals(NAME, result.name()),
				() -> assertEquals(EMAIL, result.email()),
				() -> assertEquals(UserStatus.ACTIVE, result.status()),
				() -> assertEquals(Set.of(UserRole.STUDENT), result.roles()),
				() -> assertNotNull(result.createdAt()));
	}

	@Test
	void shouldRejectDuplicateEmail() {
		userRepository.existingEmail = EMAIL;

		assertThrows(UserEmailAlreadyExistsException.class, () -> createUserUseCase.execute(validCommand()));
		assertAll(
				() -> assertEquals(1, userRepository.existsByEmailCalls),
				() -> assertEquals(0, passwordHasher.hashCalls),
				() -> assertFalse(userRepository.saved),
				() -> assertFalse(eventPublisher.published));
	}

	@Test
	void shouldHashPasswordBeforeSavingUser() {
		createUserUseCase.execute(validCommand());

		assertAll(
				() -> assertEquals(1, passwordHasher.hashCalls),
				() -> assertEquals(PASSWORD, passwordHasher.lastRawPassword),
				() -> assertEquals(PASSWORD_HASH, userRepository.savedUser.getPasswordHash()),
				() -> assertNotEquals(PASSWORD, userRepository.savedUser.getPasswordHash()));
	}

	@Test
	void shouldCheckDuplicateEmailBeforeSavingUser() {
		createUserUseCase.execute(validCommand());

		assertAll(
				() -> assertEquals(EMAIL, userRepository.lastCheckedEmail),
				() -> assertEquals(1, userRepository.existsByEmailCalls),
				() -> assertTrue(userRepository.saved));
	}

	@Test
	void shouldReturnResultWithoutPasswordOrPasswordHash() {
		CreateUserResult result = createUserUseCase.execute(validCommand());

		boolean exposesSensitivePasswordData = Arrays.stream(result.getClass().getRecordComponents())
				.map(component -> component.getName().toLowerCase())
				.anyMatch(name -> name.contains("password") || name.contains("hash"));

		assertFalse(exposesSensitivePasswordData);
	}

	@Test
	void shouldPublishUserCreatedEventAfterSavingUser() {
		createUserUseCase.execute(validCommand());

		assertAll(
				() -> assertTrue(eventPublisher.published),
				() -> assertTrue(eventPublisher.publishedAfterSave),
				() -> assertNotNull(eventPublisher.publishedEvent.eventId()),
				() -> assertEquals("UserCreated", eventPublisher.publishedEvent.eventType()));
	}

	@Test
	void shouldPublishEventWithCreatedUserPayload() {
		createUserUseCase.execute(validCommand());

		assertAll(
				() -> assertEquals(userRepository.savedUser.getId(), eventPublisher.publishedEvent.payload().userId()),
				() -> assertEquals(NAME, eventPublisher.publishedEvent.payload().name()),
				() -> assertEquals(EMAIL, eventPublisher.publishedEvent.payload().email()));
	}

	@Test
	void shouldNotPublishEventWhenValidationFails() {
		CreateUserCommand command = new CreateUserCommand(NAME, "invalid-email", PASSWORD, Set.of(UserRole.STUDENT));

		assertThrows(IllegalArgumentException.class, () -> createUserUseCase.execute(command));
		assertFalse(eventPublisher.published);
	}

	@Test
	void shouldRejectMissingPassword() {
		assertThrows(IllegalArgumentException.class,
				() -> new CreateUserCommand(NAME, EMAIL, " ", Set.of(UserRole.STUDENT)));
	}

	private CreateUserCommand validCommand() {
		return new CreateUserCommand(NAME, EMAIL, PASSWORD, Set.of(UserRole.STUDENT));
	}

	private static final class FakeUserRepository implements UserRepository {

		private String existingEmail;
		private String lastCheckedEmail;
		private int existsByEmailCalls;
		private boolean saved;
		private User savedUser;

		@Override
		public User save(User user) {
			this.saved = true;
			this.savedUser = user;
			return user;
		}

		@Override
		public boolean existsByEmail(String email) {
			this.existsByEmailCalls++;
			this.lastCheckedEmail = email;
			return email.equals(existingEmail);
		}
	}

	private static final class FakePasswordHasher implements PasswordHasher {

		private final String passwordHash;
		private String lastRawPassword;
		private int hashCalls;

		private FakePasswordHasher(String passwordHash) {
			this.passwordHash = passwordHash;
		}

		@Override
		public String hash(String rawPassword) {
			this.hashCalls++;
			this.lastRawPassword = rawPassword;
			return passwordHash;
		}

		@Override
		public boolean matches(String rawPassword, String passwordHash) {
			return this.passwordHash.equals(passwordHash) && PASSWORD.equals(rawPassword);
		}
	}

	private static final class FakeEventPublisher implements EventPublisher {

		private final FakeUserRepository userRepository;
		private boolean published;
		private boolean publishedAfterSave;
		private UserCreatedEvent publishedEvent;

		private FakeEventPublisher(FakeUserRepository userRepository) {
			this.userRepository = userRepository;
		}

		@Override
		public void publish(UserCreatedEvent event) {
			this.published = true;
			this.publishedAfterSave = userRepository.saved;
			this.publishedEvent = event;
		}
	}
}
