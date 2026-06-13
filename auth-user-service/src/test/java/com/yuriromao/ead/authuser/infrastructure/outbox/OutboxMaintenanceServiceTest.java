package com.yuriromao.ead.authuser.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class OutboxMaintenanceServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
  private static final LocalDateTime NOW_TIMESTAMP = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

  private final OutboxEventJpaRepository outboxEventJpaRepository =
      org.mockito.Mockito.mock(OutboxEventJpaRepository.class);
  private final OutboxMaintenanceService outboxMaintenanceService =
      new OutboxMaintenanceService(outboxEventJpaRepository, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void shouldRequeueFailedEvents() {
    OutboxEventJpaEntity failedEvent = failedOutboxEvent();
    when(outboxEventJpaRepository.findByStatusOrderByUpdatedAtAsc(
            OutboxEventStatus.FAILED, PageRequest.of(0, 10)))
        .thenReturn(List.of(failedEvent));

    OutboxMaintenanceResult result = outboxMaintenanceService.requeueFailedEvents(10);

    assertAll(
        () -> assertEquals("requeue-failed", result.operation()),
        () -> assertEquals(1, result.affectedEvents()),
        () -> assertEquals(OutboxEventStatus.PENDING, failedEvent.getStatus()),
        () -> assertEquals(0, failedEvent.getAttempts()),
        () -> assertNull(failedEvent.getLastError()),
        () -> assertEquals(NOW_TIMESTAMP, failedEvent.getNextAttemptAt()),
        () -> assertEquals(NOW_TIMESTAMP, failedEvent.getUpdatedAt()),
        () -> assertNull(failedEvent.getPublishedAt()));
    verify(outboxEventJpaRepository).saveAll(List.of(failedEvent));
  }

  @Test
  void shouldRejectInvalidRequeueBatchSize() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> outboxMaintenanceService.requeueFailedEvents(0));

    assertEquals("batchSize must be greater than zero", exception.getMessage());
  }

  @Test
  void shouldCleanupPublishedEventsOlderThanRetention() {
    when(outboxEventJpaRepository.deleteByStatusAndPublishedAtBefore(
            eq(OutboxEventStatus.PUBLISHED), eq(NOW_TIMESTAMP.minusDays(30))))
        .thenReturn(2L);

    OutboxMaintenanceResult result =
        outboxMaintenanceService.cleanupPublishedEvents(Duration.ofDays(30));

    assertAll(
        () -> assertEquals("cleanup-published", result.operation()),
        () -> assertEquals(2L, result.affectedEvents()));
  }

  @Test
  void shouldRejectInvalidCleanupRetention() {
    assertAll(
        () ->
            assertEquals(
                "retention must not be null",
                assertThrows(
                        NullPointerException.class,
                        () -> outboxMaintenanceService.cleanupPublishedEvents(null))
                    .getMessage()),
        () ->
            assertEquals(
                "retention must be greater than zero",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> outboxMaintenanceService.cleanupPublishedEvents(Duration.ZERO))
                    .getMessage()),
        () ->
            assertEquals(
                "retention must be greater than zero",
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                            outboxMaintenanceService.cleanupPublishedEvents(Duration.ofSeconds(-1)))
                    .getMessage()));
  }

  @Test
  void shouldRejectInvalidRequeueTransition() {
    OutboxEventJpaEntity pendingEvent = pendingOutboxEvent();
    OutboxEventJpaEntity failedEvent = failedOutboxEvent();

    assertAll(
        () ->
            assertEquals(
                "nextAttemptAt must not be null",
                assertThrows(
                        NullPointerException.class,
                        () -> failedEvent.requeueFailed(null, NOW_TIMESTAMP))
                    .getMessage()),
        () ->
            assertEquals(
                "updatedAt must not be null",
                assertThrows(
                        NullPointerException.class,
                        () -> failedEvent.requeueFailed(NOW_TIMESTAMP, null))
                    .getMessage()),
        () ->
            assertEquals(
                "only FAILED events can be requeued",
                assertThrows(
                        IllegalStateException.class,
                        () -> pendingEvent.requeueFailed(NOW_TIMESTAMP, NOW_TIMESTAMP))
                    .getMessage()));
  }

  private OutboxEventJpaEntity failedOutboxEvent() {
    OutboxEventJpaEntity outboxEvent = pendingOutboxEvent();
    outboxEvent.markAsFailed(
        OutboxEventStatus.FAILED, "rabbit unavailable", NOW_TIMESTAMP, NOW_TIMESTAMP);
    return outboxEvent;
  }

  private OutboxEventJpaEntity pendingOutboxEvent() {
    return OutboxEventJpaEntity.pending(
        "User",
        UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17"),
        "UserCreated",
        UUID.fromString("a72f6db8-4c4f-42dd-b2a2-cc0f0d7b7310"),
        Map.of("eventType", "UserCreated"),
        NOW_TIMESTAMP);
  }
}
