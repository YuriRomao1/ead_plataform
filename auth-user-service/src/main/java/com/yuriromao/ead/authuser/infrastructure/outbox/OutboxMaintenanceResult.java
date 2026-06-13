package com.yuriromao.ead.authuser.infrastructure.outbox;

/** Result returned by outbox operational maintenance tasks. */
public record OutboxMaintenanceResult(String operation, long affectedEvents) {}
