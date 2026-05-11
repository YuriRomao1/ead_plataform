package com.yuriromao.ead.authuser.application.event;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UserCreatedEventTest {

  private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");
  private static final String NAME = "User Name";
  private static final String EMAIL = "user@email.com";

  @Test
  void shouldCreateEventWithExpectedType() {
    UserCreatedEvent event = UserCreatedEvent.create(validPayload());

    assertEquals(UserCreatedEvent.EVENT_TYPE, event.eventType());
  }

  @Test
  void shouldGenerateEventId() {
    UserCreatedEvent event = UserCreatedEvent.create(validPayload());

    assertNotNull(event.eventId());
  }

  @Test
  void shouldFillOccurredAt() {
    UserCreatedEvent event = UserCreatedEvent.create(validPayload());

    assertNotNull(event.occurredAt());
  }

  @Test
  void shouldFillPayload() {
    UserCreatedEvent event = UserCreatedEvent.create(validPayload());

    assertAll(
        () -> assertEquals(USER_ID, event.payload().userId()),
        () -> assertEquals(NAME, event.payload().name()),
        () -> assertEquals(EMAIL, event.payload().email()));
  }

  @Test
  void shouldNotExposeSensitiveData() {
    boolean exposesSensitiveData =
        Stream.concat(
                Arrays.stream(UserCreatedEvent.class.getRecordComponents()),
                Arrays.stream(UserCreatedPayload.class.getRecordComponents()))
            .map(component -> component.getName().toLowerCase())
            .anyMatch(name -> name.contains("password") || name.contains("hash"));

    assertFalse(exposesSensitiveData);
  }

  @Test
  void shouldRejectUnexpectedEventType() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new UserCreatedEvent(
                UUID.randomUUID(), "UnexpectedEvent", java.time.Instant.now(), validPayload()));
  }

  @Test
  void shouldRejectMissingPayloadData() {
    assertAll(
        () ->
            assertThrows(
                NullPointerException.class, () -> new UserCreatedPayload(null, NAME, EMAIL)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new UserCreatedPayload(USER_ID, null, EMAIL)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new UserCreatedPayload(USER_ID, "", EMAIL)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new UserCreatedPayload(USER_ID, NAME, null)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new UserCreatedPayload(USER_ID, NAME, " ")));
  }

  private UserCreatedPayload validPayload() {
    return new UserCreatedPayload(USER_ID, NAME, EMAIL);
  }
}
