package com.yuriromao.ead.authuser.infrastructure.outbox;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * JPA adapter that records domain events in the transactional outbox table.
 *
 * <p>The recorder is called by application use cases inside their database transaction, so the
 * local state change and the event publication intent are persisted atomically.
 */
@Primary
@Repository
public class JpaDomainEventRecorder implements DomainEventRecorder {

  private static final String USER_AGGREGATE_TYPE = "User";

  private final OutboxEventJpaRepository outboxEventJpaRepository;
  private final JsonMapper jsonMapper;

  public JpaDomainEventRecorder(
      OutboxEventJpaRepository outboxEventJpaRepository, JsonMapper jsonMapper) {
    this.outboxEventJpaRepository =
        Objects.requireNonNull(
            outboxEventJpaRepository, "outboxEventJpaRepository must not be null");
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
  }

  /** Persists UserCreated as a PENDING outbox row ready for asynchronous publication. */
  @Override
  public void record(UserCreatedEvent event) {
    Objects.requireNonNull(event, "event must not be null");

    LocalDateTime now = toDatabaseTimestamp(Instant.now());
    OutboxEventJpaEntity entity =
        OutboxEventJpaEntity.pending(
            USER_AGGREGATE_TYPE,
            event.payload().userId(),
            event.eventType(),
            event.eventId(),
            serializePayload(event),
            now);

    outboxEventJpaRepository.save(entity);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> serializePayload(UserCreatedEvent event) {
    try {
      String json = jsonMapper.writeValueAsString(event);
      return jsonMapper.readValue(json, Map.class);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize domain event", exception);
    }
  }

  private static LocalDateTime toDatabaseTimestamp(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
