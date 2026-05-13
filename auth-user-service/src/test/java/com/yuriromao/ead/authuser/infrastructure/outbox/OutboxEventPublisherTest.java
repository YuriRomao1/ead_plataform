package com.yuriromao.ead.authuser.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedPayload;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;

class OutboxEventPublisherTest {

  private static final UUID EVENT_ID = UUID.fromString("0750699f-0141-4ee5-b936-3add0b35b0a4");
  private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");
  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
  private static final LocalDateTime NOW_TIMESTAMP = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

  private final OutboxEventJpaRepository outboxEventJpaRepository =
      mock(OutboxEventJpaRepository.class);
  private final EventPublisher eventPublisher = mock(EventPublisher.class);
  private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
  private final OutboxEventPublisher outboxEventPublisher =
      new OutboxEventPublisher(
          outboxEventJpaRepository,
          eventPublisher,
          jsonMapper,
          10,
          3,
          Duration.ofSeconds(30),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void shouldPublishPendingEventAndMarkAsPublished() {
    OutboxEventJpaEntity outboxEvent = pendingOutboxEvent(0);
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of(outboxEvent));

    outboxEventPublisher.publishPendingEvents();

    ArgumentCaptor<UserCreatedEvent> publishedEventCaptor =
        ArgumentCaptor.forClass(UserCreatedEvent.class);
    verify(eventPublisher).publish(publishedEventCaptor.capture());
    verify(outboxEventJpaRepository).save(outboxEvent);

    UserCreatedEvent publishedEvent = publishedEventCaptor.getValue();
    assertAll(
        () -> assertEquals(EVENT_ID, publishedEvent.eventId()),
        () -> assertEquals(UserCreatedEvent.EVENT_TYPE, publishedEvent.eventType()),
        () -> assertEquals(USER_ID, publishedEvent.payload().userId()),
        () -> assertEquals(OutboxEventStatus.PUBLISHED, outboxEvent.getStatus()),
        () -> assertNotNull(outboxEvent.getPublishedAt()),
        () -> assertEquals(NOW_TIMESTAMP, outboxEvent.getPublishedAt()),
        () -> assertEquals(1, outboxEvent.getAttempts()),
        () -> assertNull(outboxEvent.getLastError()));
  }

  @Test
  void shouldUseConfiguredBatchSizeWhenSearchingPendingEvents() {
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of());

    outboxEventPublisher.publishPendingEvents();

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(outboxEventJpaRepository)
        .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), pageableCaptor.capture());
    assertEquals(10, pageableCaptor.getValue().getPageSize());
  }

  @Test
  void shouldRecordErrorAndKeepEventPendingWhenPublicationFails() {
    OutboxEventJpaEntity outboxEvent = pendingOutboxEvent(0);
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of(outboxEvent));
    doThrow(new IllegalStateException("rabbit unavailable"))
        .when(eventPublisher)
        .publish(any(UserCreatedEvent.class));

    outboxEventPublisher.publishPendingEvents();

    assertAll(
        () -> assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus()),
        () -> assertEquals(1, outboxEvent.getAttempts()),
        () -> assertEquals("rabbit unavailable", outboxEvent.getLastError()),
        () -> assertEquals(NOW_TIMESTAMP.plusSeconds(30), outboxEvent.getNextAttemptAt()),
        () -> assertNull(outboxEvent.getPublishedAt()));
    verify(outboxEventJpaRepository).save(outboxEvent);
  }

  @Test
  void shouldMarkEventAsFailedWhenMaxAttemptsIsReached() {
    OutboxEventJpaEntity outboxEvent = pendingOutboxEvent(2);
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of(outboxEvent));
    doThrow(new IllegalStateException("rabbit unavailable"))
        .when(eventPublisher)
        .publish(any(UserCreatedEvent.class));

    outboxEventPublisher.publishPendingEvents();

    assertAll(
        () -> assertEquals(OutboxEventStatus.FAILED, outboxEvent.getStatus()),
        () -> assertEquals(3, outboxEvent.getAttempts()),
        () -> assertEquals("rabbit unavailable", outboxEvent.getLastError()),
        () -> assertEquals(NOW_TIMESTAMP, outboxEvent.getNextAttemptAt()),
        () -> assertNull(outboxEvent.getPublishedAt()));
    verify(outboxEventJpaRepository).save(outboxEvent);
  }

  @Test
  void shouldRecordExceptionTypeWhenFailureHasNoMessage() {
    OutboxEventJpaEntity outboxEvent = pendingOutboxEvent(0);
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of(outboxEvent));
    doThrow(new IllegalStateException()).when(eventPublisher).publish(any(UserCreatedEvent.class));

    outboxEventPublisher.publishPendingEvents();

    assertEquals(IllegalStateException.class.getName(), outboxEvent.getLastError());
  }

  @Test
  void shouldTruncateLongPublicationErrors() {
    OutboxEventJpaEntity outboxEvent = pendingOutboxEvent(0);
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of(outboxEvent));
    doThrow(new IllegalStateException("x".repeat(5_000)))
        .when(eventPublisher)
        .publish(any(UserCreatedEvent.class));

    outboxEventPublisher.publishPendingEvents();

    assertEquals(4_000, outboxEvent.getLastError().length());
  }

  @Test
  void shouldKeepEventPendingWhenOutboxEventTypeIsUnsupported() {
    OutboxEventJpaEntity outboxEvent =
        OutboxEventJpaEntity.pending(
            "User", USER_ID, "UnsupportedEvent", EVENT_ID, payload(), NOW_TIMESTAMP);
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of(outboxEvent));

    outboxEventPublisher.publishPendingEvents();

    assertAll(
        () -> assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus()),
        () -> assertEquals(1, outboxEvent.getAttempts()),
        () ->
            assertEquals(
                "Unsupported outbox event type: UnsupportedEvent", outboxEvent.getLastError()));
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void shouldKeepEventPendingWhenPayloadEventIdDoesNotMatchOutboxEventId() {
    OutboxEventJpaEntity outboxEvent =
        OutboxEventJpaEntity.pending(
            "User",
            USER_ID,
            UserCreatedEvent.EVENT_TYPE,
            EVENT_ID,
            payload(UUID.randomUUID()),
            NOW_TIMESTAMP);
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of(outboxEvent));

    outboxEventPublisher.publishPendingEvents();

    assertAll(
        () -> assertEquals(OutboxEventStatus.PENDING, outboxEvent.getStatus()),
        () -> assertEquals(1, outboxEvent.getAttempts()),
        () ->
            assertEquals("Failed to deserialize outbox event payload", outboxEvent.getLastError()));
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void shouldNotPublishAlreadyPublishedEvent() {
    OutboxEventJpaEntity outboxEvent = pendingOutboxEvent(0);
    outboxEvent.markAsPublished(NOW_TIMESTAMP);
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of(outboxEvent));

    outboxEventPublisher.publishPendingEvents();

    verifyNoInteractions(eventPublisher);
    verify(outboxEventJpaRepository, never()).save(outboxEvent);
  }

  @Test
  void shouldNotFailWhenThereAreNoPendingEvents() {
    when(outboxEventJpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), eq(NOW_TIMESTAMP), any(Pageable.class)))
        .thenReturn(List.of());

    outboxEventPublisher.publishPendingEvents();

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void shouldRejectInvalidConfiguration() {
    new OutboxEventPublisher(
        outboxEventJpaRepository, eventPublisher, jsonMapper, 10, 3, Duration.ofSeconds(30));

    assertAll(
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new OutboxEventPublisher(
                        null,
                        eventPublisher,
                        jsonMapper,
                        10,
                        3,
                        Duration.ofSeconds(30),
                        Clock.fixed(NOW, ZoneOffset.UTC))),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new OutboxEventPublisher(
                        outboxEventJpaRepository,
                        null,
                        jsonMapper,
                        10,
                        3,
                        Duration.ofSeconds(30),
                        Clock.fixed(NOW, ZoneOffset.UTC))),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new OutboxEventPublisher(
                        outboxEventJpaRepository,
                        eventPublisher,
                        null,
                        10,
                        3,
                        Duration.ofSeconds(30),
                        Clock.fixed(NOW, ZoneOffset.UTC))),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new OutboxEventPublisher(
                        outboxEventJpaRepository,
                        eventPublisher,
                        jsonMapper,
                        0,
                        3,
                        Duration.ofSeconds(30),
                        Clock.fixed(NOW, ZoneOffset.UTC))),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new OutboxEventPublisher(
                        outboxEventJpaRepository,
                        eventPublisher,
                        jsonMapper,
                        10,
                        0,
                        Duration.ofSeconds(30),
                        Clock.fixed(NOW, ZoneOffset.UTC))),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new OutboxEventPublisher(
                        outboxEventJpaRepository,
                        eventPublisher,
                        jsonMapper,
                        10,
                        3,
                        null,
                        Clock.fixed(NOW, ZoneOffset.UTC))),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new OutboxEventPublisher(
                        outboxEventJpaRepository,
                        eventPublisher,
                        jsonMapper,
                        10,
                        3,
                        Duration.ofSeconds(30),
                        null)));
  }

  @Test
  void shouldRejectInvalidLifecycleTransitions() {
    OutboxEventJpaEntity outboxEvent = pendingOutboxEvent(0);

    assertAll(
        () -> assertThrows(NullPointerException.class, () -> outboxEvent.markAsPublished(null)),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    outboxEvent.markAsFailed(
                        null, "error", NOW_TIMESTAMP.plusSeconds(30), NOW_TIMESTAMP)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    outboxEvent.markAsFailed(
                        OutboxEventStatus.PUBLISHED,
                        "error",
                        NOW_TIMESTAMP.plusSeconds(30),
                        NOW_TIMESTAMP)),
        () ->
            assertThrows(
                NullPointerException.class,
                () -> outboxEvent.markAsFailed("error", null, NOW_TIMESTAMP)),
        () ->
            assertThrows(
                NullPointerException.class,
                () -> outboxEvent.markAsFailed("error", NOW_TIMESTAMP.plusSeconds(30), null)));
  }

  private OutboxEventJpaEntity pendingOutboxEvent(int attempts) {
    OutboxEventJpaEntity outboxEvent =
        OutboxEventJpaEntity.pending(
            "User",
            USER_ID,
            UserCreatedEvent.EVENT_TYPE,
            EVENT_ID,
            payload(),
            NOW_TIMESTAMP.minusSeconds(5));

    for (int i = 0; i < attempts; i++) {
      outboxEvent.markAsFailed(
          "previous failure", NOW_TIMESTAMP.minusSeconds(1), NOW_TIMESTAMP.minusSeconds(1));
    }

    return outboxEvent;
  }

  private Map<String, Object> payload() {
    return payload(EVENT_ID);
  }

  private Map<String, Object> payload(UUID eventId) {
    try {
      UserCreatedPayload payload = new UserCreatedPayload(USER_ID, "User Name", "user@email.com");
      String json =
          jsonMapper.writeValueAsString(
              new UserCreatedEvent(eventId, UserCreatedEvent.EVENT_TYPE, NOW, payload));
      @SuppressWarnings("unchecked")
      Map<String, Object> serializedPayload = jsonMapper.readValue(json, Map.class);
      return serializedPayload;
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to create outbox payload", exception);
    }
  }
}
