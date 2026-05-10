---
name: ead-java-spring-microservices
description: Project-specific Java, Spring Boot 4, Gradle, and microservices guidance for the EAD Platform. Use when implementing, reviewing, testing, or designing Java code, REST APIs, persistence, RabbitMQ events, service boundaries, or architecture in this repository.
---

# EAD Java Spring Microservices Skill

Use this skill for Java, Spring Boot, Gradle, REST API, persistence, messaging, architecture review, code review, and testing tasks in the EAD Platform repository.

This skill is adapted for this project after evaluating `decebals/claude-code-java`. The external repository is useful inspiration, but `AGENTS.md`, project documentation, FDDs, implementation plans, and ADRs always take precedence.

## Required Context

Before implementing a feature, read:

- `AGENTS.md`
- `docs/domain-context.md`
- `docs/hld.md`, when available
- related FDD in `docs/fdds/`
- related implementation plan in `docs/implementation-plans/`
- related ADRs when architecture, security, persistence, or messaging decisions are involved

Do not implement undocumented features unless the user explicitly asks for that.

## Project Baseline

- Java 25.
- Spring Boot 4.0.x.
- Gradle 9.x.
- Dependency versions should normally be managed by the Spring Boot Gradle plugin.
- Follow existing Gradle structure and existing Spring Boot starters.
- Do not add Lombok or other convenience libraries unless the project already uses them or the user approves the dependency.
- Code must be written in English: packages, classes, methods, variables, database names, API fields, comments, and commit messages.
- Documentation may be written in Portuguese.

## Architecture Guardrails

- The system is a microservices architecture.
- Initial services are `auth-user-service`, `course-service`, and `notification-service`.
- Each service owns its own database.
- A service must never access another service's database directly.
- Synchronous service communication must use REST APIs.
- Asynchronous service communication must use RabbitMQ events.
- Domain events must represent facts that already happened.
- Commands represent intent and should not be modeled as domain events.
- Do not change architecture without creating or updating an ADR.

## Spring Boot Layering

- Controllers only receive requests, validate input, call application services or use cases, and return responses.
- Do not put business rules in controllers.
- Business rules live in application or domain layers.
- Persistence details stay behind repositories/adapters.
- Request and response DTOs should not expose persistence entities directly.
- Prefer constructor injection.
- Keep transactions in application/service boundaries, not in controllers.
- Use Bean Validation on request DTOs when validating HTTP input.

## Implementation Workflow

1. Confirm the task scope from the related documentation.
2. Identify the service, package, layer, and contract affected by the change.
3. Implement domain/application behavior before HTTP, persistence, or messaging adapters.
4. Add persistence, REST, or RabbitMQ integration only when required by the documented feature.
5. Add focused tests for the behavior and contracts touched by the task.
6. Run the narrowest useful validation first, then the broader Gradle build/test task when feasible.
7. Report files changed, tests executed, result, risks, and a suggested commit message.

## Java Review Checklist

Check for:

- Null-safety problems, invalid `Optional` use, and unexpected `null` returns.
- Exceptions that hide root causes, swallow failures, or expose internals to API clients.
- Mutable shared state and check-then-act race conditions.
- Collection and stream misuse that hurts readability or correctness.
- `equals`/`hashCode` consistency when value objects or entities require them.
- Resource leaks; use try-with-resources for `AutoCloseable` resources.
- Boolean parameter traps and oversized method signatures.
- Performance smells such as N+1 queries, regex compilation in loops, and avoidable repeated work.

## REST API Checklist

Check for:

- Resource-oriented URLs with consistent versioning where public contracts need it.
- Correct HTTP methods and status codes.
- `201 Created` with `Location` for creation when practical.
- `204 No Content` for successful deletes or commands without response bodies.
- DTOs instead of JPA entities in request/response contracts.
- Consistent error responses with machine-readable codes.
- No stack traces, password hashes, secrets, or internal implementation details in responses.
- Pagination or limits for collection endpoints that can grow.

## Persistence Checklist

Check for:

- Service-owned schema only; no cross-service database reads.
- Transactions around use cases that mutate state.
- Database uniqueness for business invariants that require it.
- Clear handling for not-found, duplicate, and conflict cases.
- N+1 query risks and lazy-loading leaks through API responses.
- Migration needs when schema changes are introduced.

## Messaging Checklist

Check for:

- Events include `eventId`, `eventType`, `occurredAt`, and a documented payload.
- Events are published only after the fact they describe has happened.
- Consumers are idempotent or have an explicit idempotency plan.
- Retry and dead-letter behavior are considered for consumers.
- Logs include enough context to trace event publication and consumption.
- Event contract changes remain backward-compatible or are documented.

## Security Checklist

Check for:

- Passwords are stored only as hashes.
- Authentication and authorization are enforced outside controllers' business logic.
- Role checks respect `STUDENT`, `TEACHER`, and `ADMIN`.
- Blocked users cannot authenticate.
- Input is validated at boundaries.
- Secrets are read from configuration or environment, never hardcoded.
- Logs do not leak passwords, tokens, or sensitive personal data.

## Testing Expectations

Every feature should include tests.

Minimum expected tests:

- Unit tests for domain rules.
- Application/service tests for use-case behavior.
- Controller tests for HTTP contracts when endpoints are touched.
- Persistence integration tests when repositories or schema behavior matter.
- Messaging integration tests when RabbitMQ publishing or consumption is changed.

Prefer JUnit 5. Use AssertJ or Mockito only when they are already available or deliberately added for the task. Test behavior rather than implementation details, and include happy paths, validation failures, boundary cases, and relevant error cases.

## Validation

Use Gradle commands that match the affected scope:

```powershell
.\gradlew.bat test
.\gradlew.bat :auth-user-service:test
.\gradlew.bat build
```

If validation cannot run, explain why and state the remaining risk.
