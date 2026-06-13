package com.yuriromao.ead.authuser.infrastructure.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Registers Micrometer gauges that expose the current outbox state by status. */
@Component
public class OutboxMetrics {

  public OutboxMetrics(
      MeterRegistry meterRegistry, OutboxEventJpaRepository outboxEventJpaRepository) {
    registerGauge(meterRegistry, outboxEventJpaRepository, OutboxEventStatus.PENDING);
    registerGauge(meterRegistry, outboxEventJpaRepository, OutboxEventStatus.PUBLISHED);
    registerGauge(meterRegistry, outboxEventJpaRepository, OutboxEventStatus.FAILED);
  }

  private static void registerGauge(
      MeterRegistry meterRegistry,
      OutboxEventJpaRepository outboxEventJpaRepository,
      OutboxEventStatus status) {
    Gauge.builder(
            "auth_user_service_outbox_events",
            outboxEventJpaRepository,
            repository -> repository.countByStatus(status))
        .description("Number of outbox events by publication status.")
        .tag("status", status.name())
        .register(meterRegistry);
  }
}
