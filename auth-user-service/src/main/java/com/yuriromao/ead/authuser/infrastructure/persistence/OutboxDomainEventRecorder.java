package com.yuriromao.ead.authuser.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;

import tools.jackson.databind.json.JsonMapper;

/**
 * Persists domain events in the transactional outbox table.
 *
 * The RabbitMQ delivery is intentionally left to a separate asynchronous
 * publisher so use cases do not depend on broker availability.
 */
@Repository
public class OutboxDomainEventRecorder implements DomainEventRecorder {

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

		jdbcTemplate.update("""
				insert into outbox_events (
				    event_id,
				    event_type,
				    occurred_at,
				    payload,
				    status,
				    attempts,
				    created_at,
				    updated_at,
				    next_attempt_at
				)
				values (?, ?, ?, cast(? as jsonb), ?, ?, ?, ?, ?)
				""",
				event.eventId(),
				event.eventType(),
				toDatabaseTimestamp(event.occurredAt()),
				serializePayload(event),
				OutboxEventStatus.PENDING.name(),
				0,
				createdAt,
				createdAt,
				createdAt);
	}

	private String serializePayload(UserCreatedEvent event) {
		try {
			return jsonMapper.writeValueAsString(event.payload());
		}
		catch (Exception exception) {
			throw new IllegalStateException("Failed to serialize domain event payload", exception);
		}
	}

	private static LocalDateTime toDatabaseTimestamp(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
