package com.yuriromao.ead.authuser.infrastructure.persistence;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Persists domain events in the transactional outbox table.
 *
 * <p>The RabbitMQ delivery is intentionally left to a separate asynchronous publisher so use cases
 * do not depend on broker availability.
 */
@Repository
public class OutboxDomainEventRecorder implements DomainEventRecorder {

  private static final String USER_AGGREGATE_TYPE = "User";

  private final JdbcTemplate jdbcTemplate;
  private final JsonMapper jsonMapper;

  public OutboxDomainEventRecorder(JdbcTemplate jdbcTemplate, JsonMapper jsonMapper) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
  }

  @Override
  public void record(UserCreatedEvent event) {
    Objects.requireNonNull(event, "event must not be null");

    Instant now = Instant.now();
    LocalDateTime createdAt = toDatabaseTimestamp(now);

    jdbcTemplate.update(
        """
				insert into outbox_events (
				    id,
				    aggregate_type,
				    aggregate_id,
				    event_type,
				    event_id,
				    payload,
				    status,
				    attempts,
				    next_attempt_at,
				    created_at,
				    updated_at
				)
				values (?, ?, ?, ?, ?, cast(? as jsonb), ?, ?, ?, ?, ?)
				""",
        UUID.randomUUID(),
        USER_AGGREGATE_TYPE,
        event.payload().userId(),
        event.eventType(),
        event.eventId(),
        serializeEvent(event),
        OutboxEventStatus.PENDING.name(),
        0,
        createdAt,
        createdAt,
        createdAt);
  }

  private String serializeEvent(UserCreatedEvent event) {
    try {
      return jsonMapper.writeValueAsString(event);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize domain event", exception);
    }
  }

  private static LocalDateTime toDatabaseTimestamp(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
