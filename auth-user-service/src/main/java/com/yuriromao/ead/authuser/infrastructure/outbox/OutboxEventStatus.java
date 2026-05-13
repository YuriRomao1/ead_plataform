package com.yuriromao.ead.authuser.infrastructure.outbox;

/** Publication state used by the transactional outbox publisher lifecycle. */
public enum OutboxEventStatus {
  PENDING,
  PUBLISHED,
  FAILED
}
