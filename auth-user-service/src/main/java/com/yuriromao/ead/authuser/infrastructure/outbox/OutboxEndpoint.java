package com.yuriromao.ead.authuser.infrastructure.outbox;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

/** Actuator operations for trusted outbox maintenance environments. */
@Component
@Endpoint(id = "outbox")
public class OutboxEndpoint {

  private static final String REQUEUE_FAILED = "requeue-failed";
  private static final String CLEANUP_PUBLISHED = "cleanup-published";

  private final OutboxEventJpaRepository outboxEventJpaRepository;
  private final OutboxMaintenanceService outboxMaintenanceService;
  private final int defaultRequeueBatchSize;
  private final Duration defaultRetention;

  public OutboxEndpoint(
      OutboxEventJpaRepository outboxEventJpaRepository,
      OutboxMaintenanceService outboxMaintenanceService,
      @Value("${auth-user-service.outbox.maintenance.requeue-batch-size:100}")
          int defaultRequeueBatchSize,
      @Value("${auth-user-service.outbox.cleanup.retention:P30D}") Duration defaultRetention) {
    this.outboxEventJpaRepository = outboxEventJpaRepository;
    this.outboxMaintenanceService = outboxMaintenanceService;
    this.defaultRequeueBatchSize = defaultRequeueBatchSize;
    this.defaultRetention = defaultRetention;
  }

  /** Returns the current number of outbox events by status. */
  @ReadOperation
  public OutboxStatus status() {
    return new OutboxStatus(
        outboxEventJpaRepository.countByStatus(OutboxEventStatus.PENDING),
        outboxEventJpaRepository.countByStatus(OutboxEventStatus.PUBLISHED),
        outboxEventJpaRepository.countByStatus(OutboxEventStatus.FAILED));
  }

  /** Runs an outbox maintenance operation selected by path. */
  @WriteOperation
  public OutboxMaintenanceResult maintain(
      @Selector String operation, Integer batchSize, String retention) {
    return switch (operation) {
      case REQUEUE_FAILED ->
          outboxMaintenanceService.requeueFailedEvents(
              batchSize == null ? defaultRequeueBatchSize : batchSize);
      case CLEANUP_PUBLISHED ->
          outboxMaintenanceService.cleanupPublishedEvents(
              retention == null ? defaultRetention : Duration.parse(retention));
      default -> throw new IllegalArgumentException("Unsupported outbox operation: " + operation);
    };
  }
}
