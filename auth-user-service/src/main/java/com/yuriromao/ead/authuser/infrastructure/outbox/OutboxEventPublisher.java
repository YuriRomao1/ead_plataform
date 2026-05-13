package com.yuriromao.ead.authuser.infrastructure.outbox;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Publishes durable outbox events to RabbitMQ and records publication attempts. */
@Component
@ConditionalOnProperty(
    name = "auth-user-service.outbox.publisher.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OutboxEventPublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventPublisher.class);
  private static final int LAST_ERROR_LIMIT = 4_000;

  private final OutboxEventJpaRepository outboxEventJpaRepository;
  private final EventPublisher eventPublisher;
  private final JsonMapper jsonMapper;
  private final int batchSize;
  private final int maxAttempts;
  private final Duration retryDelay;
  private final Clock clock;

  public OutboxEventPublisher(
      OutboxEventJpaRepository outboxEventJpaRepository,
      EventPublisher eventPublisher,
      JsonMapper jsonMapper,
      @Value("${auth-user-service.outbox.publisher.batch-size:20}") int batchSize,
      @Value("${auth-user-service.outbox.publisher.max-attempts:5}") int maxAttempts,
      @Value("${auth-user-service.outbox.publisher.retry-delay:PT30S}") Duration retryDelay) {
    this(
        outboxEventJpaRepository,
        eventPublisher,
        jsonMapper,
        batchSize,
        maxAttempts,
        retryDelay,
        Clock.systemUTC());
  }

  OutboxEventPublisher(
      OutboxEventJpaRepository outboxEventJpaRepository,
      EventPublisher eventPublisher,
      JsonMapper jsonMapper,
      int batchSize,
      int maxAttempts,
      Duration retryDelay,
      Clock clock) {
    this.outboxEventJpaRepository =
        Objects.requireNonNull(
            outboxEventJpaRepository, "outboxEventJpaRepository must not be null");
    this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
    this.batchSize = requirePositive(batchSize, "batchSize");
    this.maxAttempts = requirePositive(maxAttempts, "maxAttempts");
    this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  /**
   * Publishes the next batch of eligible PENDING events.
   *
   * <p>The method is scheduled when the publisher is enabled, but it is also public so tests and
   * operational callers can trigger one publishing cycle explicitly.
   */
  @Scheduled(fixedDelayString = "${auth-user-service.outbox.publisher.fixed-delay:5000}")
  @Transactional
  public void publishPendingEvents() {
    LocalDateTime now = now();

    outboxEventJpaRepository
        .findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus.PENDING, now, PageRequest.of(0, batchSize))
        .forEach(this::publish);
  }

  private void publish(OutboxEventJpaEntity outboxEvent) {
    if (outboxEvent.getStatus() != OutboxEventStatus.PENDING) {
      return;
    }

    try {
      UserCreatedEvent event = toUserCreatedEvent(outboxEvent);

      LOGGER.info(
          "Publishing outbox event. eventId={} eventType={} attempt={}",
          outboxEvent.getEventId(),
          outboxEvent.getEventType(),
          outboxEvent.getAttempts() + 1);
      eventPublisher.publish(event);
      outboxEvent.markAsPublished(now());
      outboxEventJpaRepository.save(outboxEvent);
      LOGGER.info(
          "Published outbox event. eventId={} eventType={}",
          outboxEvent.getEventId(),
          outboxEvent.getEventType());
    } catch (Exception exception) {
      markFailure(outboxEvent, exception);
    }
  }

  private UserCreatedEvent toUserCreatedEvent(OutboxEventJpaEntity outboxEvent) {
    if (!UserCreatedEvent.EVENT_TYPE.equals(outboxEvent.getEventType())) {
      throw new IllegalArgumentException(
          "Unsupported outbox event type: " + outboxEvent.getEventType());
    }

    try {
      String json = jsonMapper.writeValueAsString(outboxEvent.getPayload());
      UserCreatedEvent event = jsonMapper.readValue(json, UserCreatedEvent.class);
      if (!outboxEvent.getEventId().equals(event.eventId())) {
        throw new IllegalStateException("Outbox event id does not match payload event id");
      }

      return event;
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to deserialize outbox event payload", exception);
    }
  }

  private void markFailure(OutboxEventJpaEntity outboxEvent, Exception exception) {
    LocalDateTime failedAt = now();
    int nextAttempt = outboxEvent.getAttempts() + 1;
    boolean attemptsExhausted = nextAttempt >= maxAttempts;
    OutboxEventStatus status =
        attemptsExhausted ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING;
    LocalDateTime nextAttemptAt = attemptsExhausted ? failedAt : failedAt.plus(retryDelay);

    outboxEvent.markAsFailed(status, truncateError(exception), nextAttemptAt, failedAt);
    outboxEventJpaRepository.save(outboxEvent);

    LOGGER.warn(
        "Failed to publish outbox event. eventId={} eventType={} attempt={} status={}",
        outboxEvent.getEventId(),
        outboxEvent.getEventType(),
        nextAttempt,
        status,
        exception);
  }

  private LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  private static String truncateError(Exception exception) {
    String message = exception.getMessage();
    String error = message == null ? exception.getClass().getName() : message;

    if (error.length() <= LAST_ERROR_LIMIT) {
      return error;
    }

    return error.substring(0, LAST_ERROR_LIMIT);
  }

  private static int requirePositive(int value, String fieldName) {
    if (value < 1) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }

    return value;
  }
}
