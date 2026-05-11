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
