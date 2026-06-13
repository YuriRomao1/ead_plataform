package com.yuriromao.ead.authuser.infrastructure.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operational maintenance service for failed and old outbox records. */
@Service
public class OutboxMaintenanceService {

  private final OutboxEventJpaRepository outboxEventJpaRepository;
  private final Clock clock;

  @Autowired
  public OutboxMaintenanceService(OutboxEventJpaRepository outboxEventJpaRepository) {
    this(outboxEventJpaRepository, Clock.systemUTC());
  }

  OutboxMaintenanceService(OutboxEventJpaRepository outboxEventJpaRepository, Clock clock) {
    this.outboxEventJpaRepository =
        Objects.requireNonNull(
            outboxEventJpaRepository, "outboxEventJpaRepository must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  /** Requeues failed events so the existing publisher can try them again. */
  @Transactional
  public OutboxMaintenanceResult requeueFailedEvents(int batchSize) {
    int validBatchSize = requirePositive(batchSize, "batchSize");
    LocalDateTime now = now();
    var failedEvents =
        outboxEventJpaRepository.findByStatusOrderByUpdatedAtAsc(
            OutboxEventStatus.FAILED, PageRequest.of(0, validBatchSize));

    failedEvents.forEach(outboxEvent -> outboxEvent.requeueFailed(now, now));
    outboxEventJpaRepository.saveAll(failedEvents);

    return new OutboxMaintenanceResult("requeue-failed", failedEvents.size());
  }

  /** Deletes already published events older than the configured retention window. */
  @Transactional
  public OutboxMaintenanceResult cleanupPublishedEvents(Duration retention) {
    Duration validRetention = requirePositive(retention, "retention");
    LocalDateTime cutoff = now().minus(validRetention);
    long deletedEvents =
        outboxEventJpaRepository.deleteByStatusAndPublishedAtBefore(
            OutboxEventStatus.PUBLISHED, cutoff);

    return new OutboxMaintenanceResult("cleanup-published", deletedEvents);
  }

  private LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  private static int requirePositive(int value, String fieldName) {
    if (value < 1) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }

    return value;
  }

  private static Duration requirePositive(Duration value, String fieldName) {
    Duration duration = Objects.requireNonNull(value, fieldName + " must not be null");
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }

    return duration;
  }
}
