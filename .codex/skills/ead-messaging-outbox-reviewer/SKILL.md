---
name: ead-messaging-outbox-reviewer
description: Review and design EAD Platform RabbitMQ, domain event, transactional outbox, retry, DLQ, and idempotency behavior. Use when work touches UserCreated, EnrollmentCreated, outbox_events, EventPublisher, RabbitMQ topology, consumer queues, retry policies, dead-letter queues, duplicate delivery, processed-event tables, or messaging ADRs/FDDs/plans.
---

# EAD Messaging Outbox Reviewer

## Overview

Use this skill to keep asynchronous communication reliable, observable, and aligned with ADR-006 and ADR-007.

## Required Context

Read:

- `AGENTS.md`
- `.codex/skills/ead-java-spring-microservices/SKILL.md`
- `docs/decisions/adr-006-transactional-outbox-for-domain-events.md`
- `docs/decisions/adr-007-rabbitmq-topology-and-retry-dlq-strategy.md`
- `docs/hlds/hld-004-event-driven-communication.md`
- related FDD/plan and affected messaging code

## Producer Checklist

- Events represent facts that already happened.
- Event envelope includes `eventId`, `eventType`, `occurredAt`, and sanitized payload.
- No password, password hash, token, or sensitive data is published.
- Local state and outbox record commit or rollback together.
- Publisher reads only eligible `PENDING` events.
- Publication success and failure update status, attempts, `next_attempt_at`, and error state correctly.
- Duplicate publication after broker send but before DB update is treated as possible.

## Consumer Checklist

- Consumers own their queues.
- Consumers are idempotent by `eventId` in their own database.
- Invalid payloads and non-retryable failures go to DLQ.
- Transient failures use retry queue/TTL/dead-letter flow.
- Processing logs include `eventId`, `eventType`, routing key, queue, attempt, and status.
- Tests cover success, duplicate event, transient failure, retry exhaustion, and DLQ path.

## RabbitMQ Naming

Follow ADR-007:

- Exchange: `ead.domain.events`
- Retry exchange: `ead.domain.events.retry`
- DLX: `ead.domain.events.dlx`
- Routing keys: `<producer-context>.<event-name-kebab-case>`
- Consumer queue names: `<consumer-service>.<event-name-kebab-case>.queue`

## Output

Report reliability risks, contract concerns, missing tests, operational gaps, and whether an ADR/FDD update is required.
