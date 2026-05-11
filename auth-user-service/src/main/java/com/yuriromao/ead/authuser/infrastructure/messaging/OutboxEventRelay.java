package com.yuriromao.ead.authuser.infrastructure.messaging;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedPayload;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import com.yuriromao.ead.authuser.infrastructure.persistence.OutboxEvent;
import com.yuriromao.ead.authuser.infrastructure.persistence.OutboxEventRepository;
import com.yuriromao.ead.authuser.infrastructure.persistence.OutboxEventStatus;

import tools.jackson.databind.json.JsonMapper;

/**
 * Asynchronous relay that moves durable outbox events to RabbitMQ.
 *
 * The relay runs outside the user creation transaction so broker outages do not
 * roll back persisted users or lose the publication intent.
 */
@Component
@ConditionalOnProperty(
		name = "auth-user-service.outbox.publisher.enabled",
		havingValue = "true",
		matchIfMissing = true)
public class OutboxEventRelay {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventRelay.class);
	private static final int LAST_ERROR_LIMIT = 4_000;

	private final OutboxEventRepository outboxEventRepository;
	private final EventPublisher eventPublisher;
	private final JsonMapper jsonMapper;
	private final int batchSize;
	private final int maxAttempts;
	private final Duration retryDelay;

	public OutboxEventRelay(
			OutboxEventRepository outboxEventRepository,
			EventPublisher eventPublisher,
			JsonMapper jsonMapper,
			@Value("${auth-user-service.outbox.publisher.batch-size:20}") int batchSize,
			@Value("${auth-user-service.outbox.publisher.max-attempts:5}") int maxAttempts,
			@Value("${auth-user-service.outbox.publisher.retry-delay:PT30S}") Duration retryDelay) {
		this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository, "outboxEventRepository must not be null");
		this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
		this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
		this.batchSize = requirePositive(batchSize, "batchSize");
		this.maxAttempts = requirePositive(maxAttempts, "maxAttempts");
		this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay must not be null");
	}

	@Scheduled(fixedDelayString = "${auth-user-service.outbox.publisher.fixed-delay:5000}")
	public void publishPendingEvents() {
		outboxEventRepository.findPendingEvents(batchSize, Instant.now())
				.forEach(this::publish);
	}

	private void publish(OutboxEvent outboxEvent) {
		try {
			UserCreatedEvent event = toUserCreatedEvent(outboxEvent);

			LOGGER.info("Publishing outbox event. eventId={} eventType={} attempt={}",
					outboxEvent.eventId(), outboxEvent.eventType(), outboxEvent.attempts() + 1);
			eventPublisher.publish(event);
			outboxEventRepository.markPublished(outboxEvent.eventId(), Instant.now());
			LOGGER.info("Published outbox event. eventId={} eventType={}",
					outboxEvent.eventId(), outboxEvent.eventType());
		}
		catch (Exception exception) {
			markFailure(outboxEvent, exception);
		}
	}

	private UserCreatedEvent toUserCreatedEvent(OutboxEvent outboxEvent) {
		if (!UserCreatedEvent.EVENT_TYPE.equals(outboxEvent.eventType())) {
			throw new IllegalArgumentException("Unsupported outbox event type: " + outboxEvent.eventType());
		}

		try {
			UserCreatedPayload payload = jsonMapper.readValue(outboxEvent.payload(), UserCreatedPayload.class);
			return new UserCreatedEvent(
					outboxEvent.eventId(),
					outboxEvent.eventType(),
					outboxEvent.occurredAt(),
					payload);
		}
		catch (Exception exception) {
			throw new IllegalStateException("Failed to deserialize outbox event payload", exception);
		}
	}

	private void markFailure(OutboxEvent outboxEvent, Exception exception) {
		int nextAttempt = outboxEvent.attempts() + 1;
		boolean attemptsExhausted = nextAttempt >= maxAttempts;
		OutboxEventStatus status = attemptsExhausted ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING;
		Instant nextAttemptAt = attemptsExhausted ? null : Instant.now().plus(retryDelay);

		outboxEventRepository.markFailed(
				outboxEvent.eventId(),
				status,
				truncateError(exception),
				nextAttemptAt);

		LOGGER.warn("Failed to publish outbox event. eventId={} eventType={} attempt={} status={}",
				outboxEvent.eventId(), outboxEvent.eventType(), nextAttempt, status, exception);
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
