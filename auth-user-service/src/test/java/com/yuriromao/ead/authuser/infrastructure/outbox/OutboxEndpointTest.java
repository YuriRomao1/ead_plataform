package com.yuriromao.ead.authuser.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxEndpointTest {

  private final OutboxEventJpaRepository outboxEventJpaRepository =
      org.mockito.Mockito.mock(OutboxEventJpaRepository.class);
  private final OutboxMaintenanceService outboxMaintenanceService =
      org.mockito.Mockito.mock(OutboxMaintenanceService.class);
  private final OutboxEndpoint outboxEndpoint =
      new OutboxEndpoint(
          outboxEventJpaRepository, outboxMaintenanceService, 100, Duration.ofDays(30));

  @Test
  void shouldReturnOutboxStatus() {
    when(outboxEventJpaRepository.countByStatus(OutboxEventStatus.PENDING)).thenReturn(1L);
    when(outboxEventJpaRepository.countByStatus(OutboxEventStatus.PUBLISHED)).thenReturn(2L);
    when(outboxEventJpaRepository.countByStatus(OutboxEventStatus.FAILED)).thenReturn(3L);

    OutboxStatus status = outboxEndpoint.status();

    assertAll(
        () -> assertEquals(1L, status.pending()),
        () -> assertEquals(2L, status.published()),
        () -> assertEquals(3L, status.failed()));
  }

  @Test
  void shouldRequeueFailedEventsWithDefaultBatchSize() {
    when(outboxMaintenanceService.requeueFailedEvents(100))
        .thenReturn(new OutboxMaintenanceResult("requeue-failed", 4));

    OutboxMaintenanceResult result = outboxEndpoint.maintain("requeue-failed", null, null);

    assertAll(
        () -> assertEquals("requeue-failed", result.operation()),
        () -> assertEquals(4L, result.affectedEvents()));
  }

  @Test
  void shouldRequeueFailedEventsWithRequestBatchSize() {
    when(outboxMaintenanceService.requeueFailedEvents(25))
        .thenReturn(new OutboxMaintenanceResult("requeue-failed", 5));

    OutboxMaintenanceResult result = outboxEndpoint.maintain("requeue-failed", 25, null);

    assertAll(
        () -> assertEquals("requeue-failed", result.operation()),
        () -> assertEquals(5L, result.affectedEvents()));
  }

  @Test
  void shouldCleanupPublishedEventsWithDefaultRetention() {
    when(outboxMaintenanceService.cleanupPublishedEvents(Duration.ofDays(30)))
        .thenReturn(new OutboxMaintenanceResult("cleanup-published", 6));

    OutboxMaintenanceResult result = outboxEndpoint.maintain("cleanup-published", null, null);

    assertAll(
        () -> assertEquals("cleanup-published", result.operation()),
        () -> assertEquals(6L, result.affectedEvents()));
  }

  @Test
  void shouldCleanupPublishedEventsWithRequestRetention() {
    when(outboxMaintenanceService.cleanupPublishedEvents(Duration.ofDays(7)))
        .thenReturn(new OutboxMaintenanceResult("cleanup-published", 7));

    OutboxMaintenanceResult result = outboxEndpoint.maintain("cleanup-published", null, "P7D");

    assertAll(
        () -> assertEquals("cleanup-published", result.operation()),
        () -> assertEquals(7L, result.affectedEvents()));
  }

  @Test
  void shouldRejectUnsupportedOperation() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> outboxEndpoint.maintain("unsupported", null, null));

    assertEquals("Unsupported outbox operation: unsupported", exception.getMessage());
  }
}
