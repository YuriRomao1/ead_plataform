package com.yuriromao.ead.authuser.infrastructure.outbox;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically applies the retention policy for published outbox records. */
@Component
@ConditionalOnProperty(name = "auth-user-service.outbox.cleanup.enabled", havingValue = "true")
public class OutboxCleanupScheduler {

  private final OutboxMaintenanceService outboxMaintenanceService;
  private final Duration retention;

  public OutboxCleanupScheduler(
      OutboxMaintenanceService outboxMaintenanceService,
      @Value("${auth-user-service.outbox.cleanup.retention:P30D}") Duration retention) {
    this.outboxMaintenanceService = outboxMaintenanceService;
    this.retention = retention;
  }

  /** Deletes published events older than the configured retention window. */
  @Scheduled(fixedDelayString = "${auth-user-service.outbox.cleanup.fixed-delay:86400000}")
  public void cleanupPublishedEvents() {
    outboxMaintenanceService.cleanupPublishedEvents(retention);
  }
}
