package com.yuriromao.ead.authuser.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import com.yuriromao.ead.authuser.application.usecase.CreateUserCommand;
import com.yuriromao.ead.authuser.application.usecase.CreateUserUseCase;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.infrastructure.persistence.UserJpaEntity;
import com.yuriromao.ead.authuser.infrastructure.persistence.UserJpaRepository;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = { "delete from user_roles", "delete from users" })
class CreateUserIntegrationTest {

	private static final String NAME = "Integration User";
	private static final String EMAIL = "integration-user@email.com";
	private static final String PASSWORD = "plainPassword";

	@Autowired
	private CreateUserUseCase createUserUseCase;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@MockitoBean
	private EventPublisher eventPublisher;

	@Test
	void shouldPersistUserAndPublishUserCreatedEvent() {
		createUserUseCase.execute(new CreateUserCommand(NAME, EMAIL, PASSWORD, Set.of(UserRole.STUDENT)));

		UserJpaEntity persistedUser = userJpaRepository.findAll().stream()
				.filter(user -> EMAIL.equals(user.getEmail()))
				.findFirst()
				.orElseThrow();
		ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());
		UserCreatedEvent event = eventCaptor.getValue();

		assertAll(
				() -> assertEquals(NAME, persistedUser.getName()),
				() -> assertEquals(EMAIL, persistedUser.getEmail()),
				() -> assertNotEquals(PASSWORD, persistedUser.getPasswordHash()),
				() -> assertTrue(persistedUser.getPasswordHash().startsWith("$2")),
				() -> assertEquals(persistedUser.getId(), event.payload().userId()),
				() -> assertEquals(NAME, event.payload().name()),
				() -> assertEquals(EMAIL, event.payload().email()),
				() -> assertEquals(UserCreatedEvent.EVENT_TYPE, event.eventType()));
	}
}
