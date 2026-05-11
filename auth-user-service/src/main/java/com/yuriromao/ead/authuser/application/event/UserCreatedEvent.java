package com.yuriromao.ead.authuser.application.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Application event model for the user-created fact.
 *
 * <p>The event carries only public user data so it can later be serialized and published without
 * exposing passwords, hashes, or persistence internals.
 */
public record UserCreatedEvent(
    UUID eventId, String eventType, Instant occurredAt, UserCreatedPayload payload) {

  public static final String EVENT_TYPE = "UserCreated";

  public UserCreatedEvent {
    eventId = Objects.requireNonNull(eventId, "eventId must not be null");
    eventType = requireExpectedEventType(eventType);
    occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    payload = Objects.requireNonNull(payload, "payload must not be null");
  }

  public static UserCreatedEvent create(UserCreatedPayload payload) {
    return new UserCreatedEvent(UUID.randomUUID(), EVENT_TYPE, Instant.now(), payload);
  }

  private static String requireExpectedEventType(String eventType) {
    if (!EVENT_TYPE.equals(eventType)) {
      throw new IllegalArgumentException("eventType must be " + EVENT_TYPE);
    }

    return eventType;
  }
}
