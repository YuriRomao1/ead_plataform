package com.yuriromao.ead.authuser.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedPayload;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;
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

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from outbox_events")
class JdbcOutboxEventRepositoryTest {

  private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");

  @Autowired private DomainEventRecorder domainEventRecorder;

  @Autowired private OutboxEventRepository outboxEventRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void shouldFindPendingEventsReadyForPublication() {
    UserCreatedEvent event = userCreatedEvent();
    domainEventRecorder.record(event);

    List<OutboxEvent> pendingEvents =
        outboxEventRepository.findPendingEvents(10, Instant.now().plusSeconds(5));

    assertAll(
        () -> assertEquals(1, pendingEvents.size()),
        () -> assertNotNull(pendingEvents.getFirst().id()),
        () -> assertEquals("User", pendingEvents.getFirst().aggregateType()),
        () -> assertEquals(USER_ID, pendingEvents.getFirst().aggregateId()),
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
        () -> assertNotNull(row.get("next_attempt_at")));
  }

  @Test
  void shouldIncrementAttemptsAndStoreFailureDetails() {
    UserCreatedEvent event = userCreatedEvent();
    domainEventRecorder.record(event);
    Instant nextAttemptAt = Instant.now().plusSeconds(30);

    outboxEventRepository.markFailed(
        event.eventId(), OutboxEventStatus.PENDING, "rabbit unavailable", nextAttemptAt);

    Map<String, Object> row = findOutboxEvent(event.eventId());
    assertAll(
        () -> assertEquals(OutboxEventStatus.PENDING.name(), row.get("status")),
        () -> assertEquals(1, row.get("attempts")),
        () -> assertEquals("rabbit unavailable", row.get("last_error")),
        () -> assertNotNull(row.get("next_attempt_at")),
        () -> assertNull(row.get("published_at")));
  }

  @Test
  void shouldRejectInvalidRepositoryOperations() {
    UserCreatedEvent event = userCreatedEvent();

    assertAll(
        () ->
            assertThrows(
                RuntimeException.class,
                () -> outboxEventRepository.findPendingEvents(0, Instant.now())),
        () ->
            assertThrows(
                RuntimeException.class, () -> outboxEventRepository.findPendingEvents(1, null)),
        () ->
            assertThrows(
                RuntimeException.class,
                () -> outboxEventRepository.markPublished(null, Instant.now())),
        () ->
            assertThrows(
                RuntimeException.class,
                () -> outboxEventRepository.markPublished(event.eventId(), null)),
        () ->
            assertThrows(
                RuntimeException.class,
                () ->
                    outboxEventRepository.markFailed(
                        null, OutboxEventStatus.PENDING, "error", Instant.now())),
        () ->
            assertThrows(
                RuntimeException.class,
                () ->
                    outboxEventRepository.markFailed(
                        event.eventId(), null, "error", Instant.now())),
        () ->
            assertThrows(
                RuntimeException.class,
                () ->
                    outboxEventRepository.markFailed(
                        event.eventId(), OutboxEventStatus.PENDING, "error", null)),
        () ->
            assertThrows(
                RuntimeException.class,
                () ->
                    outboxEventRepository.markFailed(
                        event.eventId(), OutboxEventStatus.PUBLISHED, "error", Instant.now())));
  }

  @Test
  void shouldRejectInvalidOutboxEventValues() {
    UUID outboxId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    assertAll(
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new OutboxEvent(
                        null, "User", USER_ID, eventId, UserCreatedEvent.EVENT_TYPE, "{}", 0)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new OutboxEvent(
                        outboxId, "", USER_ID, eventId, UserCreatedEvent.EVENT_TYPE, "{}", 0)),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new OutboxEvent(
                        outboxId, "User", null, eventId, UserCreatedEvent.EVENT_TYPE, "{}", 0)),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new OutboxEvent(
                        outboxId, "User", USER_ID, null, UserCreatedEvent.EVENT_TYPE, "{}", 0)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(outboxId, "User", USER_ID, eventId, " ", "{}", 0)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new OutboxEvent(
                        outboxId, "User", USER_ID, eventId, UserCreatedEvent.EVENT_TYPE, null, 0)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new OutboxEvent(
                        outboxId,
                        "User",
                        USER_ID,
                        eventId,
                        UserCreatedEvent.EVENT_TYPE,
                        "{}",
                        -1)));
  }

  private Map<String, Object> findOutboxEvent(UUID eventId) {
    return jdbcTemplate.queryForMap(
        """
				select status,
				       attempts,
				       last_error,
				       published_at,
				       next_attempt_at
				from outbox_events
				where event_id = ?
				""",
        eventId);
  }

  private UserCreatedEvent userCreatedEvent() {
    UserCreatedPayload payload = new UserCreatedPayload(USER_ID, "User Name", "user@email.com");
    return UserCreatedEvent.create(payload);
  }
}
