package com.yuriromao.ead.authuser.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping for durable domain events stored in the auth-user-service outbox table.
 *
 * <p>The entity keeps publication metadata next to the event payload so the asynchronous publisher
 * can retry, mark success, or stop after the configured attempt limit.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "event_id", nullable = false, unique = true)
  private UUID eventId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OutboxEventStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "next_attempt_at", nullable = false)
  private LocalDateTime nextAttemptAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected OutboxEventJpaEntity() {}

  private OutboxEventJpaEntity(
      UUID id,
      String aggregateType,
      UUID aggregateId,
      String eventType,
      UUID eventId,
      Map<String, Object> payload,
      OutboxEventStatus status,
      int attempts,
      String lastError,
      LocalDateTime nextAttemptAt,
      LocalDateTime createdAt,
      LocalDateTime publishedAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.eventId = eventId;
    this.payload = new LinkedHashMap<>(payload);
    this.status = status;
    this.attempts = attempts;
    this.lastError = lastError;
    this.nextAttemptAt = nextAttemptAt;
    this.createdAt = createdAt;
    this.publishedAt = publishedAt;
    this.updatedAt = updatedAt;
  }

  /** Creates a new outbox row that is eligible for publication as soon as nextAttemptAt is due. */
  public static OutboxEventJpaEntity pending(
      String aggregateType,
      UUID aggregateId,
      String eventType,
      UUID eventId,
      Map<String, Object> payload,
      LocalDateTime now) {
    return new OutboxEventJpaEntity(
        UUID.randomUUID(),
        aggregateType,
        aggregateId,
        eventType,
        eventId,
        payload,
        OutboxEventStatus.PENDING,
        0,
        null,
        now,
        now,
        null,
        now);
  }

  /** Marks the event as successfully published and clears any previous error. */
  public void markAsPublished(LocalDateTime publishedAt) {
    if (publishedAt == null) {
      throw new NullPointerException("publishedAt must not be null");
    }

    this.status = OutboxEventStatus.PUBLISHED;
    this.attempts++;
    this.lastError = null;
    this.publishedAt = publishedAt;
    this.updatedAt = publishedAt;
  }

  /** Records a recoverable publication failure and keeps the event pending for retry. */
  public void markAsFailed(
      String errorMessage, LocalDateTime nextAttemptAt, LocalDateTime updatedAt) {
    markAsFailed(OutboxEventStatus.PENDING, errorMessage, nextAttemptAt, updatedAt);
  }

  /** Records a publication failure with the final status chosen by the retry policy. */
  public void markAsFailed(
      OutboxEventStatus status,
      String errorMessage,
      LocalDateTime nextAttemptAt,
      LocalDateTime updatedAt) {
    if (status == null) {
      throw new NullPointerException("status must not be null");
    }

    if (status == OutboxEventStatus.PUBLISHED) {
      throw new IllegalArgumentException("status must not be PUBLISHED for failure");
    }

    if (nextAttemptAt == null) {
      throw new NullPointerException("nextAttemptAt must not be null");
    }

    if (updatedAt == null) {
      throw new NullPointerException("updatedAt must not be null");
    }

    this.status = status;
    this.attempts++;
    this.lastError = errorMessage;
    this.nextAttemptAt = nextAttemptAt;
    this.updatedAt = updatedAt;
  }

  /** Requeues a failed event for a new publication cycle after an operator intervention. */
  public void requeueFailed(LocalDateTime nextAttemptAt, LocalDateTime updatedAt) {
    if (nextAttemptAt == null) {
      throw new NullPointerException("nextAttemptAt must not be null");
    }

    if (updatedAt == null) {
      throw new NullPointerException("updatedAt must not be null");
    }

    if (status != OutboxEventStatus.FAILED) {
      throw new IllegalStateException("only FAILED events can be requeued");
    }

    this.status = OutboxEventStatus.PENDING;
    this.attempts = 0;
    this.lastError = null;
    this.nextAttemptAt = nextAttemptAt;
    this.publishedAt = null;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public UUID getEventId() {
    return eventId;
  }

  public Map<String, Object> getPayload() {
    return Map.copyOf(payload);
  }

  public OutboxEventStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public LocalDateTime getNextAttemptAt() {
    return nextAttemptAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getPublishedAt() {
    return publishedAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
