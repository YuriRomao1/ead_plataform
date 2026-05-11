package com.yuriromao.ead.authuser.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
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

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from outbox_events")
class JdbcOutboxEventRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");

	@Autowired
	private DomainEventRecorder domainEventRecorder;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void shouldFindPendingEventsReadyForPublication() {
		UserCreatedEvent event = userCreatedEvent();
		domainEventRecorder.record(event);

		List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(10, Instant.now().plusSeconds(5));

		assertAll(
				() -> assertEquals(1, pendingEvents.size()),
				() -> assertEquals(event.eventId(), pendingEvents.getFirst().eventId()),
				() -> assertEquals(UserCreatedEvent.EVENT_TYPE, pendingEvents.getFirst().eventType()),
				() -> assertTrue(pendingEvents.getFirst().payload().contains(USER_ID.toString())),
				() -> assertEquals(0, pendingEvents.getFirst().attempts()));
	}

	@Test
	void shouldMarkOutboxEventAsPublished() {
		UserCreatedEvent event = userCreatedEvent();
		domainEventRecorder.record(event);

		outboxEventRepository.markPublished(event.eventId(), Instant.now());

		Map<String, Object> row = findOutboxEvent(event.eventId());
		assertAll(
				() -> assertEquals(OutboxEventStatus.PUBLISHED.name(), row.get("status")),
				() -> assertNotNull(row.get("published_at")),
				() -> assertNull(row.get("last_error")),
				() -> assertNull(row.get("next_attempt_at")));
	}

	@Test
	void shouldIncrementAttemptsAndStoreFailureDetails() {
		UserCreatedEvent event = userCreatedEvent();
		domainEventRecorder.record(event);
		Instant nextAttemptAt = Instant.now().plusSeconds(30);

		outboxEventRepository.markFailed(
				event.eventId(),
				OutboxEventStatus.PENDING,
				"rabbit unavailable",
				nextAttemptAt);

		Map<String, Object> row = findOutboxEvent(event.eventId());
		assertAll(
				() -> assertEquals(OutboxEventStatus.PENDING.name(), row.get("status")),
				() -> assertEquals(1, row.get("attempts")),
				() -> assertEquals("rabbit unavailable", row.get("last_error")),
				() -> assertNotNull(row.get("next_attempt_at")),
				() -> assertNull(row.get("published_at")));
	}

	private Map<String, Object> findOutboxEvent(UUID eventId) {
		return jdbcTemplate.queryForMap("""
				select status,
				       attempts,
				       last_error,
				       published_at,
				       next_attempt_at
				from outbox_events
				where event_id = ?
				""", eventId);
	}

	private UserCreatedEvent userCreatedEvent() {
		UserCreatedPayload payload = new UserCreatedPayload(USER_ID, "User Name", "user@email.com");
		return UserCreatedEvent.create(payload);
	}
}
