package com.yuriromao.ead.authuser.application.usecase;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.exception.UserEmailAlreadyExistsException;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import com.yuriromao.ead.authuser.application.port.PasswordHasher;
import com.yuriromao.ead.authuser.application.port.UserRepository;
import com.yuriromao.ead.authuser.domain.model.User;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.domain.model.UserStatus;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CreateUserUseCaseTest {

  private static final String NAME = "User Name";
  private static final String EMAIL = "user@email.com";
  private static final String PASSWORD = "plainPassword";
  private static final String PASSWORD_HASH = "$2a$10$hashedPassword";

  private final FakeUserRepository userRepository = new FakeUserRepository();
  private final FakePasswordHasher passwordHasher = new FakePasswordHasher(PASSWORD_HASH);
  private final FakeDomainEventRecorder domainEventRecorder =
      new FakeDomainEventRecorder(userRepository);
  private final CreateUserUseCase createUserUseCase =
      new CreateUserUseCase(userRepository, passwordHasher, domainEventRecorder);

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

    assertThrows(
        UserEmailAlreadyExistsException.class, () -> createUserUseCase.execute(validCommand()));
    assertAll(
        () -> assertEquals(1, userRepository.existsByEmailCalls),
        () -> assertEquals(0, passwordHasher.hashCalls),
        () -> assertFalse(userRepository.saved),
        () -> assertFalse(domainEventRecorder.recorded));
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

    boolean exposesSensitivePasswordData =
        Arrays.stream(result.getClass().getRecordComponents())
            .map(component -> component.getName().toLowerCase())
            .anyMatch(name -> name.contains("password") || name.contains("hash"));

    assertFalse(exposesSensitivePasswordData);
  }

  @Test
  void shouldRecordUserCreatedEventAfterSavingUser() {
    createUserUseCase.execute(validCommand());

    assertAll(
        () -> assertTrue(domainEventRecorder.recorded),
        () -> assertTrue(domainEventRecorder.recordedAfterSave),
        () -> assertNotNull(domainEventRecorder.recordedEvent.eventId()),
        () -> assertEquals("UserCreated", domainEventRecorder.recordedEvent.eventType()));
  }

  @Test
  void shouldNotDependOnEventPublisherDirectly() {
    boolean dependsOnEventPublisher =
        Arrays.stream(CreateUserUseCase.class.getDeclaredFields())
            .map(Field::getType)
            .anyMatch(EventPublisher.class::equals);

    assertFalse(dependsOnEventPublisher);
  }

  @Test
  void shouldRecordEventWithCreatedUserPayload() {
    createUserUseCase.execute(validCommand());

    assertAll(
        () ->
            assertEquals(
                userRepository.savedUser.getId(),
                domainEventRecorder.recordedEvent.payload().userId()),
        () -> assertEquals(NAME, domainEventRecorder.recordedEvent.payload().name()),
        () -> assertEquals(EMAIL, domainEventRecorder.recordedEvent.payload().email()));
  }

  @Test
  void shouldNotRecordEventWhenValidationFails() {
    CreateUserCommand command =
        new CreateUserCommand(NAME, "invalid-email", PASSWORD, Set.of(UserRole.STUDENT));

    assertThrows(IllegalArgumentException.class, () -> createUserUseCase.execute(command));
    assertFalse(domainEventRecorder.recorded);
  }

  @Test
  void shouldRejectMissingPassword() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CreateUserCommand(NAME, EMAIL, " ", Set.of(UserRole.STUDENT)));
  }

  @Test
  void shouldRejectInvalidCommandFields() {
    Set<UserRole> rolesWithNull = new HashSet<>();
    rolesWithNull.add(UserRole.STUDENT);
    rolesWithNull.add(null);

    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CreateUserCommand(null, EMAIL, PASSWORD, Set.of(UserRole.STUDENT))),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CreateUserCommand(NAME, null, PASSWORD, Set.of(UserRole.STUDENT))),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CreateUserCommand(NAME, EMAIL, null, Set.of(UserRole.STUDENT))),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CreateUserCommand(NAME, EMAIL, PASSWORD, null)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CreateUserCommand(NAME, EMAIL, PASSWORD, Set.of())),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CreateUserCommand(NAME, EMAIL, PASSWORD, rolesWithNull)));
  }

  @Test
  void shouldExposeDuplicateEmailInException() {
    UserEmailAlreadyExistsException exception = new UserEmailAlreadyExistsException(EMAIL);

    assertEquals(EMAIL, exception.getEmail());
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

  private static final class FakeDomainEventRecorder implements DomainEventRecorder {

    private final FakeUserRepository userRepository;
    private boolean recorded;
    private boolean recordedAfterSave;
    private UserCreatedEvent recordedEvent;

    private FakeDomainEventRecorder(FakeUserRepository userRepository) {
      this.userRepository = userRepository;
    }

    @Override
    public void record(UserCreatedEvent event) {
      this.recorded = true;
      this.recordedAfterSave = userRepository.saved;
      this.recordedEvent = event;
    }
  }
}
