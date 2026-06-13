package com.yuriromao.ead.authuser.infrastructure.outbox;

import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxCleanupSchedulerTest {

  @Test
  void shouldCleanupPublishedEventsUsingConfiguredRetention() {
    OutboxMaintenanceService outboxMaintenanceService =
        org.mockito.Mockito.mock(OutboxMaintenanceService.class);
    OutboxCleanupScheduler outboxCleanupScheduler =
        new OutboxCleanupScheduler(outboxMaintenanceService, Duration.ofDays(30));

    outboxCleanupScheduler.cleanupPublishedEvents();

    verify(outboxMaintenanceService).cleanupPublishedEvents(Duration.ofDays(30));
  }
}
