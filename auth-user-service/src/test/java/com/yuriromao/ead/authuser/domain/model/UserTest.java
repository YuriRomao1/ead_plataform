package com.yuriromao.ead.authuser.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class UserTest {

	private static final String VALID_NAME = "User Name";
	private static final String VALID_EMAIL = "user@email.com";
	private static final String VALID_PASSWORD_HASH = "$2a$10$hashedPassword";

	@Test
	void shouldCreateValidUserWithActiveStatus() {
		User user = createValidUser();

		assertEquals(UserStatus.ACTIVE, user.getStatus());
		assertEquals(VALID_NAME, user.getName());
		assertEquals(VALID_EMAIL, user.getEmail());
		assertEquals(VALID_PASSWORD_HASH, user.getPasswordHash());
		assertEquals(Set.of(UserRole.STUDENT), user.getRoles());
	}

	@Test
	void shouldCreateUserWithId() {
		User user = createValidUser();

		assertNotNull(user.getId());
	}

	@Test
	void shouldCreateUserWithCreatedAtAndUpdatedAt() {
		User user = createValidUser();

		assertNotNull(user.getCreatedAt());
		assertNotNull(user.getUpdatedAt());
		assertEquals(user.getCreatedAt(), user.getUpdatedAt());
	}

	@Test
	void shouldAcceptStudentTeacherAndAdminRoles() {
		Set<UserRole> roles = Set.of(UserRole.STUDENT, UserRole.TEACHER, UserRole.ADMIN);

		User user = User.createNew(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH, roles);

		assertEquals(roles, user.getRoles());
	}

	@Test
	void shouldRejectNullName() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew(null, VALID_EMAIL, VALID_PASSWORD_HASH, validRoles()));
	}

	@Test
	void shouldRejectEmptyName() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew("", VALID_EMAIL, VALID_PASSWORD_HASH, validRoles()));
	}

	@Test
	void shouldRejectNullEmail() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew(VALID_NAME, null, VALID_PASSWORD_HASH, validRoles()));
	}

	@Test
	void shouldRejectEmptyEmail() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew(VALID_NAME, "", VALID_PASSWORD_HASH, validRoles()));
	}

	@Test
	void shouldRejectInvalidEmail() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew(VALID_NAME, "invalid-email", VALID_PASSWORD_HASH, validRoles()));
	}

	@Test
	void shouldRejectNullPasswordHash() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew(VALID_NAME, VALID_EMAIL, null, validRoles()));
	}

	@Test
	void shouldRejectEmptyPasswordHash() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew(VALID_NAME, VALID_EMAIL, "", validRoles()));
	}

	@Test
	void shouldRejectNullRoles() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH, null));
	}

	@Test
	void shouldRejectEmptyRoles() {
		assertThrows(IllegalArgumentException.class,
				() -> User.createNew(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH, Set.of()));
	}

	@Test
	void shouldProtectRolesFromExternalModification() {
		Set<UserRole> roles = EnumSet.of(UserRole.STUDENT);
		User user = User.createNew(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH, roles);

		roles.add(UserRole.ADMIN);

		assertEquals(Set.of(UserRole.STUDENT), user.getRoles());
		assertThrows(UnsupportedOperationException.class, () -> user.getRoles().add(UserRole.TEACHER));
	}

	private User createValidUser() {
		return User.createNew(VALID_NAME, VALID_EMAIL, VALID_PASSWORD_HASH, validRoles());
	}

	private Set<UserRole> validRoles() {
		return Set.of(UserRole.STUDENT);
	}
}
