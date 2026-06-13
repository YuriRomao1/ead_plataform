package com.yuriromao.ead.authuser.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class OutboxMetricsTest {

  @Test
  void shouldRegisterOutboxStatusGauges() {
    OutboxEventJpaRepository outboxEventJpaRepository =
        org.mockito.Mockito.mock(OutboxEventJpaRepository.class);
    when(outboxEventJpaRepository.countByStatus(OutboxEventStatus.PENDING)).thenReturn(1L);
    when(outboxEventJpaRepository.countByStatus(OutboxEventStatus.PUBLISHED)).thenReturn(2L);
    when(outboxEventJpaRepository.countByStatus(OutboxEventStatus.FAILED)).thenReturn(3L);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    new OutboxMetrics(meterRegistry, outboxEventJpaRepository);

    assertAll(
        () -> assertEquals(1.0, gaugeValue(meterRegistry, "PENDING")),
        () -> assertEquals(2.0, gaugeValue(meterRegistry, "PUBLISHED")),
        () -> assertEquals(3.0, gaugeValue(meterRegistry, "FAILED")));
  }

  private double gaugeValue(SimpleMeterRegistry meterRegistry, String status) {
    return meterRegistry
        .get("auth_user_service_outbox_events")
        .tag("status", status)
        .gauge()
        .value();
  }
}
