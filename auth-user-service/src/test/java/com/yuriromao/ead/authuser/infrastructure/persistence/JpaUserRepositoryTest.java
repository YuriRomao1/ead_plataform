package com.yuriromao.ead.authuser.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.yuriromao.ead.authuser.application.port.UserRepository;
import com.yuriromao.ead.authuser.domain.model.User;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.domain.model.UserStatus;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = { "delete from user_roles", "delete from users" })
class JpaUserRepositoryTest {

	private static final String VALID_PASSWORD_HASH = "$2a$10$hashedPassword";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Test
	void shouldSaveValidUser() {
		User user = createUser("saved-user@email.com", Set.of(UserRole.STUDENT));

		User savedUser = userRepository.save(user);

		UserJpaEntity persistedUser = findPersistedUser(user);
		assertEquals(user.getId(), savedUser.getId());
		assertEquals(user.getId(), persistedUser.getId());
		assertEquals(user.getName(), persistedUser.getName());
		assertEquals(user.getEmail(), persistedUser.getEmail());
		assertEquals(user.getPasswordHash(), persistedUser.getPasswordHash());
	}

	@Test
	void shouldPersistActiveStatus() {
		User user = createUser("active-user@email.com", Set.of(UserRole.STUDENT));

		userRepository.save(user);

		UserJpaEntity persistedUser = findPersistedUser(user);
		assertEquals(UserStatus.ACTIVE, persistedUser.getStatus());
	}

	@Test
	void shouldPersistRoles() {
		Set<UserRole> roles = Set.of(UserRole.STUDENT, UserRole.TEACHER);
		User user = createUser("roles-user@email.com", roles);

		userRepository.save(user);

		UserJpaEntity persistedUser = findPersistedUser(user);
		assertEquals(roles, persistedUser.getRoles());
	}

	@Test
	void shouldReturnTrueWhenEmailExists() {
		User user = createUser("existing-user@email.com", Set.of(UserRole.STUDENT));
		userRepository.save(user);

		boolean exists = userRepository.existsByEmail(user.getEmail());

		assertTrue(exists);
	}

	@Test
	void shouldReturnFalseWhenEmailDoesNotExist() {
		boolean exists = userRepository.existsByEmail("missing-user@email.com");

		assertFalse(exists);
	}

	@Test
	void shouldFailWhenSavingDuplicateEmail() {
		User user = createUser("duplicate-user@email.com", Set.of(UserRole.STUDENT));
		User duplicateEmailUser = createUser("duplicate-user@email.com", Set.of(UserRole.ADMIN));
		userRepository.save(user);

		assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(duplicateEmailUser));
	}

	private UserJpaEntity findPersistedUser(User user) {
		return userJpaRepository.findById(user.getId()).orElseThrow();
	}

	private User createUser(String email, Set<UserRole> roles) {
		return User.createNew("User Name", email, VALID_PASSWORD_HASH, roles);
	}
}
