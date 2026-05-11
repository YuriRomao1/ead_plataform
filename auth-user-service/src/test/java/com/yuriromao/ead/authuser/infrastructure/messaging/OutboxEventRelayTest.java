package com.yuriromao.ead.authuser.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedPayload;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import com.yuriromao.ead.authuser.infrastructure.persistence.OutboxEvent;
import com.yuriromao.ead.authuser.infrastructure.persistence.OutboxEventRepository;
import com.yuriromao.ead.authuser.infrastructure.persistence.OutboxEventStatus;

import tools.jackson.databind.json.JsonMapper;

class OutboxEventRelayTest {

	private static final UUID EVENT_ID = UUID.fromString("0750699f-0141-4ee5-b936-3add0b35b0a4");
	private static final UUID OUTBOX_ID = UUID.fromString("56c45f57-7d6f-4dc4-84a7-a4b56ae02a97");
	private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");
	private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T10:00:00Z");

	private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
	private final FakeOutboxEventRepository outboxEventRepository = new FakeOutboxEventRepository();
	private final FakeEventPublisher eventPublisher = new FakeEventPublisher();
	private final OutboxEventRelay relay = new OutboxEventRelay(
			outboxEventRepository,
			eventPublisher,
			jsonMapper,
			10,
			3,
			Duration.ofSeconds(30));

	@Test
	void shouldPublishPendingOutboxEventAndMarkAsPublished() throws Exception {
		outboxEventRepository.pendingEvents = List.of(userCreatedOutboxEvent(0));

		relay.publishPendingEvents();

		assertAll(
				() -> assertEquals(10, outboxEventRepository.lastLimit),
				() -> assertNotNull(outboxEventRepository.lastNow),
				() -> assertEquals(EVENT_ID, eventPublisher.publishedEvent.eventId()),
				() -> assertEquals(UserCreatedEvent.EVENT_TYPE, eventPublisher.publishedEvent.eventType()),
				() -> assertEquals(OCCURRED_AT, eventPublisher.publishedEvent.occurredAt()),
				() -> assertEquals(USER_ID, eventPublisher.publishedEvent.payload().userId()),
				() -> assertEquals("User Name", eventPublisher.publishedEvent.payload().name()),
				() -> assertEquals("user@email.com", eventPublisher.publishedEvent.payload().email()),
				() -> assertEquals(EVENT_ID, outboxEventRepository.publishedEventId),
				() -> assertNotNull(outboxEventRepository.publishedAt),
				() -> assertFalse(outboxEventRepository.failed));
	}

	@Test
	void shouldKeepEventPendingAndScheduleRetryWhenPublicationFails() throws Exception {
		outboxEventRepository.pendingEvents = List.of(userCreatedOutboxEvent(0));
		eventPublisher.exception = new IllegalStateException("rabbit unavailable");

		relay.publishPendingEvents();

		assertAll(
				() -> assertTrue(outboxEventRepository.failed),
				() -> assertEquals(EVENT_ID, outboxEventRepository.failedEventId),
				() -> assertEquals(OutboxEventStatus.PENDING, outboxEventRepository.failedStatus),
				() -> assertTrue(outboxEventRepository.lastError.contains("rabbit unavailable")),
				() -> assertNotNull(outboxEventRepository.nextAttemptAt),
				() -> assertNull(outboxEventRepository.publishedEventId));
	}

	@Test
	void shouldMarkEventAsFailedWhenMaxAttemptsIsReached() throws Exception {
		outboxEventRepository.pendingEvents = List.of(userCreatedOutboxEvent(2));
		eventPublisher.exception = new IllegalStateException("rabbit unavailable");

		relay.publishPendingEvents();

		assertAll(
				() -> assertTrue(outboxEventRepository.failed),
				() -> assertEquals(EVENT_ID, outboxEventRepository.failedEventId),
				() -> assertEquals(OutboxEventStatus.FAILED, outboxEventRepository.failedStatus),
				() -> assertTrue(outboxEventRepository.lastError.contains("rabbit unavailable")),
				() -> assertNotNull(outboxEventRepository.nextAttemptAt),
				() -> assertNull(outboxEventRepository.publishedEventId));
	}

	@Test
	void shouldKeepEventPendingWhenOutboxEventTypeIsUnsupported() throws Exception {
		outboxEventRepository.pendingEvents = List.of(outboxEvent(
				EVENT_ID,
				"UnsupportedEvent",
				jsonMapper.writeValueAsString(userCreatedEvent(EVENT_ID))));

		relay.publishPendingEvents();

		assertAll(
				() -> assertTrue(outboxEventRepository.failed),
				() -> assertEquals(OutboxEventStatus.PENDING, outboxEventRepository.failedStatus),
				() -> assertTrue(outboxEventRepository.lastError.contains("Unsupported outbox event type")),
				() -> assertNull(eventPublisher.publishedEvent));
	}

	@Test
	void shouldKeepEventPendingWhenPayloadEventIdDoesNotMatchOutboxEventId() throws Exception {
		outboxEventRepository.pendingEvents = List.of(outboxEvent(
				EVENT_ID,
				UserCreatedEvent.EVENT_TYPE,
				jsonMapper.writeValueAsString(userCreatedEvent(UUID.randomUUID()))));

		relay.publishPendingEvents();

		assertAll(
				() -> assertTrue(outboxEventRepository.failed),
				() -> assertEquals(OutboxEventStatus.PENDING, outboxEventRepository.failedStatus),
				() -> assertTrue(outboxEventRepository.lastError.contains("Failed to deserialize outbox event payload")),
				() -> assertNull(eventPublisher.publishedEvent));
	}

	@Test
	void shouldUseExceptionTypeWhenPublicationFailureHasNoMessage() throws Exception {
		outboxEventRepository.pendingEvents = List.of(userCreatedOutboxEvent(0));
		eventPublisher.exception = new IllegalStateException();

		relay.publishPendingEvents();

		assertEquals(IllegalStateException.class.getName(), outboxEventRepository.lastError);
	}

	@Test
	void shouldTruncateLongPublicationErrors() throws Exception {
		outboxEventRepository.pendingEvents = List.of(userCreatedOutboxEvent(0));
		eventPublisher.exception = new IllegalStateException("x".repeat(5_000));

		relay.publishPendingEvents();

		assertEquals(4_000, outboxEventRepository.lastError.length());
	}

	@Test
	void shouldRejectInvalidConfiguration() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new OutboxEventRelay(null, eventPublisher, jsonMapper, 10, 3, Duration.ofSeconds(30))),
				() -> assertThrows(NullPointerException.class,
						() -> new OutboxEventRelay(outboxEventRepository, null, jsonMapper, 10, 3, Duration.ofSeconds(30))),
				() -> assertThrows(NullPointerException.class,
						() -> new OutboxEventRelay(outboxEventRepository, eventPublisher, null, 10, 3, Duration.ofSeconds(30))),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new OutboxEventRelay(outboxEventRepository, eventPublisher, jsonMapper, 0, 3, Duration.ofSeconds(30))),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new OutboxEventRelay(outboxEventRepository, eventPublisher, jsonMapper, 10, 0, Duration.ofSeconds(30))),
				() -> assertThrows(NullPointerException.class,
						() -> new OutboxEventRelay(outboxEventRepository, eventPublisher, jsonMapper, 10, 3, null)));
	}

	private OutboxEvent userCreatedOutboxEvent(int attempts) throws Exception {
		UserCreatedPayload payload = new UserCreatedPayload(USER_ID, "User Name", "user@email.com");
		return outboxEvent(
				EVENT_ID,
				UserCreatedEvent.EVENT_TYPE,
				jsonMapper.writeValueAsString(new UserCreatedEvent(EVENT_ID, UserCreatedEvent.EVENT_TYPE, OCCURRED_AT, payload)),
				attempts);
	}

	private OutboxEvent outboxEvent(UUID eventId, String eventType, String payload) {
		return outboxEvent(eventId, eventType, payload, 0);
	}

	private OutboxEvent outboxEvent(UUID eventId, String eventType, String payload, int attempts) {
		return new OutboxEvent(
				OUTBOX_ID,
				"User",
				USER_ID,
				eventId,
				eventType,
				payload,
				attempts);
	}

	private UserCreatedEvent userCreatedEvent(UUID eventId) {
		UserCreatedPayload payload = new UserCreatedPayload(USER_ID, "User Name", "user@email.com");
		return new UserCreatedEvent(eventId, UserCreatedEvent.EVENT_TYPE, OCCURRED_AT, payload);
	}

	private static final class FakeOutboxEventRepository implements OutboxEventRepository {

		private List<OutboxEvent> pendingEvents = List.of();
		private int lastLimit;
		private Instant lastNow;
		private UUID publishedEventId;
		private Instant publishedAt;
		private boolean failed;
		private UUID failedEventId;
		private OutboxEventStatus failedStatus;
		private String lastError;
		private Instant nextAttemptAt;

		@Override
		public List<OutboxEvent> findPendingEvents(int limit, Instant now) {
			this.lastLimit = limit;
			this.lastNow = now;
			return pendingEvents;
		}

		@Override
		public void markPublished(UUID eventId, Instant publishedAt) {
			this.publishedEventId = eventId;
			this.publishedAt = publishedAt;
		}

		@Override
		public void markFailed(UUID eventId, OutboxEventStatus status, String lastError, Instant nextAttemptAt) {
			this.failed = true;
			this.failedEventId = eventId;
			this.failedStatus = status;
			this.lastError = lastError;
			this.nextAttemptAt = nextAttemptAt;
		}
	}

	private static final class FakeEventPublisher implements EventPublisher {

		private UserCreatedEvent publishedEvent;
		private RuntimeException exception;

		@Override
		public void publish(UserCreatedEvent event) {
			if (exception != null) {
				throw exception;
			}

			this.publishedEvent = event;
		}
	}
}
