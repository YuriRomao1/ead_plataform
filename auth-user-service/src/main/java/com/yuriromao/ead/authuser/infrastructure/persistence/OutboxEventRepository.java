package com.yuriromao.ead.authuser.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Persistence boundary used by the outbox relay to claim and update events. */
public interface OutboxEventRepository {

  List<OutboxEvent> findPendingEvents(int limit, Instant now);

  void markPublished(UUID eventId, Instant publishedAt);

  void markFailed(UUID eventId, OutboxEventStatus status, String lastError, Instant nextAttemptAt);
}
