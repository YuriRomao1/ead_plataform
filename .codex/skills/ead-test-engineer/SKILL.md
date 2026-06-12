---
name: ead-test-engineer
description: Design, add, review, and run automated tests for the EAD Platform. Use when Codex needs to cover domain rules, application use cases, HTTP contracts, JPA/Flyway persistence, Testcontainers, RabbitMQ producers or consumers, Cucumber scenarios, regression tests, flaky test diagnosis, or test failure investigation.
---

# EAD Test Engineer

## Overview

Use this skill to choose the smallest reliable test type for EAD Platform behavior and to preserve validation integrity.

## Required Context

Read these files when relevant:

- `AGENTS.md`
- `.codex/skills/ead-java-spring-microservices/SKILL.md`
- `docs/decisions/adr-004-testing-strategy.md`
- related FDD and implementation plan
- affected production code and existing tests
- affected service `build.gradle`

## Test Selection

- Domain rules: JUnit unit tests.
- Application use cases: unit tests with simple fakes or mocks already used by the project.
- HTTP contracts: Spring Boot WebMVC tests.
- Persistence and migrations: Spring, Flyway, PostgreSQL, and Testcontainers.
- Messaging publisher/consumer behavior: adapter tests plus integration tests when broker behavior matters.
- Observable cross-service flows: Cucumber only after build configuration and related FDD/plan exist.

## Rules

- Do not remove, disable, or weaken tests to make a build pass.
- Do not replace specific assertions with generic assertions.
- Do not add sleeps or order-dependent tests.
- Keep fixtures local and deterministic.
- Clean database state in integration tests.
- Treat Docker/Testcontainers failures as environment failures when Docker is unavailable.
- Validate sensitive data is absent from responses, logs, and events when auth/user behavior is touched.

## Workflow

1. Identify the behavior and acceptance criteria.
2. Inspect existing tests before adding patterns.
3. Add the narrowest useful test first.
4. Run the targeted Gradle test command.
5. Broaden to service build when behavior crosses layers.
6. Diagnose failures from the first meaningful root cause.

## Validation Commands

```powershell
.\gradlew.bat :auth-user-service:test
.\gradlew.bat :auth-user-service:build
```

Report exact commands, pass/fail result, and remaining risk.
