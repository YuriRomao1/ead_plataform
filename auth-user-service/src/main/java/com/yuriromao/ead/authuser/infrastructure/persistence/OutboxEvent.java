package com.yuriromao.ead.authuser.infrastructure.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Database representation of a domain event waiting for broker publication.
 */
public record OutboxEvent(
		UUID eventId,
		String eventType,
		Instant occurredAt,
		String payload,
		int attempts) {

	public OutboxEvent {
		eventId = Objects.requireNonNull(eventId, "eventId must not be null");
		eventType = requireText(eventType, "eventType");
		occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		payload = requireText(payload, "payload");

		if (attempts < 0) {
			throw new IllegalArgumentException("attempts must not be negative");
		}
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be null or empty");
		}

		return value;
	}
}
