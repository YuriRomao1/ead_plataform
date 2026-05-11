package com.yuriromao.ead.authuser.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedPayload;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from outbox_events")
class OutboxDomainEventRecorderTest {

	private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");

	@Autowired
	private DomainEventRecorder domainEventRecorder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void shouldRecordUserCreatedEventAsPendingOutboxEvent() {
		UserCreatedEvent event = userCreatedEvent();

		domainEventRecorder.record(event);

		Map<String, Object> row = jdbcTemplate.queryForMap("""
				select id,
				       aggregate_type,
				       aggregate_id,
				       event_id,
				       event_type,
				       payload::text as payload,
				       status,
				       attempts,
				       last_error,
				       created_at,
				       updated_at,
				       published_at,
				       next_attempt_at
				from outbox_events
				where event_id = ?
				""", event.eventId());
		String payload = (String) row.get("payload");

		assertAll(
				() -> assertNotNull(row.get("id")),
				() -> assertEquals("User", row.get("aggregate_type")),
				() -> assertEquals(USER_ID, row.get("aggregate_id")),
				() -> assertEquals(event.eventId(), row.get("event_id")),
				() -> assertEquals(UserCreatedEvent.EVENT_TYPE, row.get("event_type")),
				() -> assertEquals(OutboxEventStatus.PENDING.name(), row.get("status")),
				() -> assertEquals(0, row.get("attempts")),
				() -> assertNull(row.get("last_error")),
				() -> assertNotNull(row.get("created_at")),
				() -> assertNotNull(row.get("updated_at")),
				() -> assertNull(row.get("published_at")),
				() -> assertNotNull(row.get("next_attempt_at")),
				() -> assertTrue(payload.contains(event.eventId().toString())),
				() -> assertTrue(payload.contains(UserCreatedEvent.EVENT_TYPE)),
				() -> assertTrue(payload.contains("occurredAt")),
				() -> assertTrue(payload.contains(USER_ID.toString())),
				() -> assertTrue(payload.contains("User Name")),
				() -> assertTrue(payload.contains("user@email.com")),
				() -> assertFalse(payload.toLowerCase().contains("password")),
				() -> assertFalse(payload.toLowerCase().contains("hash")));
	}

	@Test
	void shouldFailWhenEventSerializationFails() throws Exception {
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		JsonMapper jsonMapper = mock(JsonMapper.class);
		DomainEventRecorder recorder = new OutboxDomainEventRecorder(jdbcTemplate, jsonMapper);
		UserCreatedEvent event = userCreatedEvent();
		when(jsonMapper.writeValueAsString(any(UserCreatedEvent.class)))
				.thenThrow(new IllegalStateException("serialization failed"));

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> recorder.record(event));

		assertEquals("Failed to serialize domain event", exception.getMessage());
		verifyNoInteractions(jdbcTemplate);
	}

	private UserCreatedEvent userCreatedEvent() {
		UserCreatedPayload payload = new UserCreatedPayload(USER_ID, "User Name", "user@email.com");
		return UserCreatedEvent.create(payload);
	}
}
