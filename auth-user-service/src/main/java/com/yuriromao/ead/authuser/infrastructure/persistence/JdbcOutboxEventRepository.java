package com.yuriromao.ead.authuser.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for the transactional outbox table owned by auth-user-service.
 */
@Repository
public class JdbcOutboxEventRepository implements OutboxEventRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcOutboxEventRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
	}

	@Override
	public List<OutboxEvent> findPendingEvents(int limit, Instant now) {
		if (limit < 1) {
			throw new IllegalArgumentException("limit must be greater than zero");
		}

		Objects.requireNonNull(now, "now must not be null");

		return jdbcTemplate.query("""
				select event_id,
				       event_type,
				       occurred_at,
				       payload::text as payload,
				       attempts
				from outbox_events
				where status = ?
				  and (next_attempt_at is null or next_attempt_at <= ?)
				order by created_at, event_id
				limit ?
				""",
				this::mapOutboxEvent,
				OutboxEventStatus.PENDING.name(),
				toDatabaseTimestamp(now),
				limit);
	}

	@Override
	public void markPublished(UUID eventId, Instant publishedAt) {
		Objects.requireNonNull(eventId, "eventId must not be null");
		Objects.requireNonNull(publishedAt, "publishedAt must not be null");

		LocalDateTime timestamp = toDatabaseTimestamp(publishedAt);

		jdbcTemplate.update("""
				update outbox_events
				set status = ?,
				    published_at = ?,
				    updated_at = ?,
				    last_error = null,
				    next_attempt_at = null
				where event_id = ?
				""",
				OutboxEventStatus.PUBLISHED.name(),
				timestamp,
				timestamp,
				eventId);
	}

	@Override
	public void markFailed(UUID eventId, OutboxEventStatus status, String lastError, Instant nextAttemptAt) {
		Objects.requireNonNull(eventId, "eventId must not be null");
		Objects.requireNonNull(status, "status must not be null");

		if (status == OutboxEventStatus.PUBLISHED) {
			throw new IllegalArgumentException("status must not be PUBLISHED for failure");
		}

		Instant now = Instant.now();

		jdbcTemplate.update("""
				update outbox_events
				set status = ?,
				    attempts = attempts + 1,
				    last_error = ?,
				    updated_at = ?,
				    next_attempt_at = ?
				where event_id = ?
				""",
				status.name(),
				lastError,
				toDatabaseTimestamp(now),
				nextAttemptAt == null ? null : toDatabaseTimestamp(nextAttemptAt),
				eventId);
	}

	private OutboxEvent mapOutboxEvent(ResultSet resultSet, int rowNumber) throws SQLException {
		return new OutboxEvent(
				resultSet.getObject("event_id", UUID.class),
				resultSet.getString("event_type"),
				resultSet.getTimestamp("occurred_at").toInstant(),
				resultSet.getString("payload"),
				resultSet.getInt("attempts"));
	}

	private static LocalDateTime toDatabaseTimestamp(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
