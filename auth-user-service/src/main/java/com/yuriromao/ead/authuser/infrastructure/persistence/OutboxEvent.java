package com.yuriromao.ead.authuser.infrastructure.persistence;

import java.util.Objects;
import java.util.UUID;

/**
 * Database representation of a domain event waiting for broker publication.
 */
public record OutboxEvent(
		UUID id,
		String aggregateType,
		UUID aggregateId,
		UUID eventId,
		String eventType,
		String payload,
		int attempts) {

	public OutboxEvent {
		id = Objects.requireNonNull(id, "id must not be null");
		aggregateType = requireText(aggregateType, "aggregateType");
		aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
		eventId = Objects.requireNonNull(eventId, "eventId must not be null");
		eventType = requireText(eventType, "eventType");
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
