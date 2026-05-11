package com.yuriromao.ead.authuser.infrastructure.outbox;

public enum OutboxEventStatus {
  PENDING,
  PUBLISHED,
  FAILED
}
