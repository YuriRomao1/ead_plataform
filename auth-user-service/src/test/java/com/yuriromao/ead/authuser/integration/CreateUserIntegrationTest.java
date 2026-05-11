package com.yuriromao.ead.authuser.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.usecase.CreateUserCommand;
import com.yuriromao.ead.authuser.application.usecase.CreateUserUseCase;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.infrastructure.persistence.UserJpaEntity;
import com.yuriromao.ead.authuser.infrastructure.persistence.UserJpaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = { "delete from outbox_events", "delete from user_roles", "delete from users" })
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

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void shouldCreateUserThroughHttpAndPersistHashAndRecordOutboxEvent() throws Exception {
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
		Map<String, Object> outboxEvent = findSingleOutboxEvent();
		String payload = (String) outboxEvent.get("payload");

		assertAll(
				() -> assertNotEquals(PASSWORD, persistedUser.getPasswordHash()),
				() -> assertTrue(persistedUser.getPasswordHash().startsWith("$2")),
				() -> assertNotNull(outboxEvent.get("id")),
				() -> assertEquals("User", outboxEvent.get("aggregate_type")),
				() -> assertEquals(persistedUser.getId(), outboxEvent.get("aggregate_id")),
				() -> assertNotNull(outboxEvent.get("event_id")),
				() -> assertEquals(UserCreatedEvent.EVENT_TYPE, outboxEvent.get("event_type")),
				() -> assertEquals("PENDING", outboxEvent.get("status")),
				() -> assertEquals(0, outboxEvent.get("attempts")),
				() -> assertTrue(payload.contains(outboxEvent.get("event_id").toString())),
				() -> assertTrue(payload.contains(UserCreatedEvent.EVENT_TYPE)),
				() -> assertTrue(payload.contains("occurredAt")),
				() -> assertTrue(payload.contains(persistedUser.getId().toString())),
				() -> assertTrue(payload.contains(NAME)),
				() -> assertTrue(payload.contains(EMAIL)),
				() -> assertFalse(payload.toLowerCase().contains("password")),
				() -> assertFalse(payload.toLowerCase().contains("hash")));
	}

	@Test
	void shouldPersistUserAndRecordUserCreatedOutboxEvent() {
		createUserUseCase.execute(new CreateUserCommand(NAME, EMAIL, PASSWORD, Set.of(UserRole.STUDENT)));

		UserJpaEntity persistedUser = findUserByEmail(EMAIL);
		Map<String, Object> outboxEvent = findSingleOutboxEvent();
		String payload = (String) outboxEvent.get("payload");

		assertAll(
				() -> assertEquals(NAME, persistedUser.getName()),
				() -> assertEquals(EMAIL, persistedUser.getEmail()),
				() -> assertNotEquals(PASSWORD, persistedUser.getPasswordHash()),
				() -> assertTrue(persistedUser.getPasswordHash().startsWith("$2")),
				() -> assertEquals(UserCreatedEvent.EVENT_TYPE, outboxEvent.get("event_type")),
				() -> assertEquals("PENDING", outboxEvent.get("status")),
				() -> assertTrue(payload.contains(persistedUser.getId().toString())),
				() -> assertTrue(payload.contains(NAME)),
				() -> assertTrue(payload.contains(EMAIL)));
	}

	@Test
	void shouldReturnConflictAndNotRecordNewOutboxEventWhenEmailAlreadyExistsThroughHttp() throws Exception {
		createUserUseCase.execute(new CreateUserCommand(NAME, EMAIL, PASSWORD, Set.of(UserRole.STUDENT)));

		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(EMAIL)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.message").value("Email already exists."))
				.andExpect(jsonPath("$.details").isArray());

		assertEquals(1, userJpaRepository.count());
		assertEquals(1, outboxEventCount());
	}

	@Test
	void shouldRejectInvalidHttpRequestWithoutPersistingUserOrRecordingOutboxEvent() throws Exception {
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_REQUIRED"));

		assertEquals(0, userJpaRepository.count());
		assertEquals(0, outboxEventCount());
	}

	private UserJpaEntity findUserByEmail(String email) {
		return userJpaRepository.findAll().stream()
				.filter(user -> email.equals(user.getEmail()))
				.findFirst()
				.orElseThrow();
	}

	private Map<String, Object> findSingleOutboxEvent() {
		return jdbcTemplate.queryForMap("""
				select id,
				       aggregate_type,
				       aggregate_id,
				       event_id,
				       event_type,
				       payload::text as payload,
				       status,
				       attempts
				from outbox_events
				""");
	}

	private int outboxEventCount() {
		Integer count = jdbcTemplate.queryForObject("select count(*) from outbox_events", Integer.class);
		return count == null ? 0 : count;
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
