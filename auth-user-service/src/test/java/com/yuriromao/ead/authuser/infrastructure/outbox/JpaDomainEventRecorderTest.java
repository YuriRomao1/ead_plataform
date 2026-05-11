package com.yuriromao.ead.authuser.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedPayload;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from outbox_events")
class JpaDomainEventRecorderTest {

  private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");

  @Autowired private DomainEventRecorder domainEventRecorder;

  @Autowired private OutboxEventJpaRepository outboxEventJpaRepository;

  @Test
  void shouldPersistUserCreatedEventInOutboxEventsTable() {
    UserCreatedEvent event = userCreatedEvent();

    domainEventRecorder.record(event);

    OutboxEventJpaEntity saved =
        outboxEventJpaRepository.findByEventId(event.eventId()).orElseThrow();
    Map<String, Object> payload = saved.getPayload();
    @SuppressWarnings("unchecked")
    Map<String, Object> nestedPayload = (Map<String, Object>) payload.get("payload");

    assertAll(
        () -> assertNotNull(saved.getId()),
        () -> assertEquals("User", saved.getAggregateType()),
        () -> assertEquals(USER_ID, saved.getAggregateId()),
        () -> assertEquals(event.eventId(), saved.getEventId()),
        () -> assertEquals(UserCreatedEvent.EVENT_TYPE, saved.getEventType()),
        () -> assertEquals(OutboxEventStatus.PENDING, saved.getStatus()),
        () -> assertEquals(0, saved.getAttempts()),
        () -> assertNull(saved.getLastError()),
        () -> assertNotNull(saved.getNextAttemptAt()),
        () -> assertNotNull(saved.getCreatedAt()),
        () -> assertNotNull(saved.getUpdatedAt()),
        () -> assertNull(saved.getPublishedAt()),
        () -> assertEquals(event.eventId().toString(), payload.get("eventId")),
        () -> assertEquals(UserCreatedEvent.EVENT_TYPE, payload.get("eventType")),
        () -> assertNotNull(payload.get("occurredAt")),
        () -> assertEquals(USER_ID.toString(), nestedPayload.get("userId")),
        () -> assertEquals("User Name", nestedPayload.get("name")),
        () -> assertEquals("user@email.com", nestedPayload.get("email")),
        () -> assertFalse(payload.toString().toLowerCase().contains("password")),
        () -> assertFalse(payload.toString().toLowerCase().contains("hash")));
  }

  @Test
  void shouldFailWhenEventIsNull() {
    assertThrows(NullPointerException.class, () -> domainEventRecorder.record(null));
    assertEquals(0, outboxEventJpaRepository.count());
  }

  @Test
  void shouldFailWhenEventSerializationFails() throws Exception {
    OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    JsonMapper jsonMapper = mock(JsonMapper.class);
    JpaDomainEventRecorder recorder = new JpaDomainEventRecorder(repository, jsonMapper);
    UserCreatedEvent event = userCreatedEvent();

    when(jsonMapper.writeValueAsString(any(UserCreatedEvent.class)))
        .thenThrow(new IllegalStateException("serialization failed"));

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> recorder.record(event));

    assertEquals("Failed to serialize domain event", exception.getMessage());
    verifyNoInteractions(repository);
  }

  private UserCreatedEvent userCreatedEvent() {
    UserCreatedPayload payload = new UserCreatedPayload(USER_ID, "User Name", "user@email.com");
    return UserCreatedEvent.create(payload);
  }
}
