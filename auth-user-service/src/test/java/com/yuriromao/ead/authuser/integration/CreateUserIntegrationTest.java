package com.yuriromao.ead.authuser.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import com.yuriromao.ead.authuser.application.usecase.CreateUserCommand;
import com.yuriromao.ead.authuser.application.usecase.CreateUserUseCase;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.infrastructure.persistence.UserJpaEntity;
import com.yuriromao.ead.authuser.infrastructure.persistence.UserJpaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = { "delete from user_roles", "delete from users" })
class CreateUserIntegrationTest {

	private static final String NAME = "Integration User";
	private static final String EMAIL = "integration-user@email.com";
	private static final String PASSWORD = "plainPassword";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateUserUseCase createUserUseCase;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@MockitoBean
	private EventPublisher eventPublisher;

	@Test
	void shouldCreateUserThroughHttpAndPersistHashAndPublishEvent() throws Exception {
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(EMAIL)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", containsString("/users/")))
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.name").value(NAME))
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.roles[0]").value("STUDENT"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(MockMvcResultMatchers.content().string(not(containsString(PASSWORD))));

		UserJpaEntity persistedUser = findUserByEmail(EMAIL);
		ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());
		UserCreatedEvent event = eventCaptor.getValue();

		assertAll(
				() -> assertNotEquals(PASSWORD, persistedUser.getPasswordHash()),
				() -> assertTrue(persistedUser.getPasswordHash().startsWith("$2")),
				() -> assertEquals(persistedUser.getId(), event.payload().userId()),
				() -> assertEquals(NAME, event.payload().name()),
				() -> assertEquals(EMAIL, event.payload().email()));
	}

	@Test
	void shouldPersistUserAndPublishUserCreatedEvent() {
		createUserUseCase.execute(new CreateUserCommand(NAME, EMAIL, PASSWORD, Set.of(UserRole.STUDENT)));

		UserJpaEntity persistedUser = findUserByEmail(EMAIL);
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

	@Test
	void shouldReturnConflictAndNotPublishEventWhenEmailAlreadyExistsThroughHttp() throws Exception {
		createUserUseCase.execute(new CreateUserCommand(NAME, EMAIL, PASSWORD, Set.of(UserRole.STUDENT)));
		reset(eventPublisher);

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(EMAIL)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.message").value("Email already exists."))
				.andExpect(jsonPath("$.details").isArray());

		verifyNoInteractions(eventPublisher);
		assertEquals(1, userJpaRepository.count());
	}

	@Test
	void shouldRejectInvalidHttpRequestWithoutPersistingUserOrPublishingEvent() throws Exception {
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_REQUIRED"));

		verifyNoInteractions(eventPublisher);
		assertEquals(0, userJpaRepository.count());
	}

	private UserJpaEntity findUserByEmail(String email) {
		return userJpaRepository.findAll().stream()
				.filter(user -> email.equals(user.getEmail()))
				.findFirst()
				.orElseThrow();
	}

	private String validRequest(String email) {
		return """
				{
				  "name": "%s",
				  "email": "%s",
				  "password": "%s",
				  "roles": ["STUDENT"]
				}
				""".formatted(NAME, email, PASSWORD);
	}
}
