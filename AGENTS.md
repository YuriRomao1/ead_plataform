# AGENTS.md

## Project Overview

EAD Platform is a Java-based online learning platform built as a microservices architecture.

The project is used as a professional learning project to practice:

- Java
- Spring Boot
- microservices architecture
- database per service
- synchronous communication with REST
- asynchronous communication with RabbitMQ
- domain events
- automated tests
- AI-assisted software development workflow

## Language Rules

Documentation may be written in Portuguese.

All code must be written in English:

- package names
- class names
- method names
- variables
- database tables
- API fields
- comments when needed
- commit messages

## Architecture Principles

The system follows a microservices architecture.

Initial services:

- auth-user-service
- course-service
- notification-service

Each service owns its own database.

A service must never access another service's database directly.

Synchronous communication must use REST APIs.

Asynchronous communication must use RabbitMQ events.

## Development Workflow

Before implementing a feature, the agent must check:

1. `docs/domain-context.md`
2. `docs/hld.md`, when available
3. related FDD in `docs/fdds/`
4. related implementation plan in `docs/implementation-plans/`

Do not implement features without documentation unless explicitly asked.

## Project AI Skill

This repository includes a project-specific Codex skill:

- `.codex/skills/ead-java-spring-microservices/SKILL.md`

Use or consult this skill for Java, Spring Boot, Gradle, REST API, persistence, RabbitMQ, testing, architecture review, and code review tasks in this project.

The skill adapts useful ideas from external Java AI development skills to this repository's baseline. External guidance must never override this `AGENTS.md`, the project documentation, FDDs, implementation plans, or ADRs.

## Task Rules

Work on one task at a time.

Do not mix unrelated changes.

Do not refactor unrelated code.

Do not create unnecessary abstractions.

Do not change architecture without creating or updating an ADR.

## Java Rules

Use Java 25.

Use Spring Boot 4.

Use dependency versions managed by the Spring Boot Gradle plugin whenever possible.

Current baseline:

- Java 25
- Spring Boot 4.0.x
- Gradle 9.x

Prefer constructor injection.

Do not put business rules inside controllers.

Controllers must only:

1. receive requests;
2. validate input;
3. call application services/use cases;
4. return responses.

Business rules must live in application/domain layers.

## Testing Rules

Every feature must include tests.

Minimum expected tests:

- unit tests for domain rules;
- integration tests for persistence and messaging when applicable;
- controller tests for HTTP contracts when applicable.

No task is complete if tests do not pass.

## Definition of Done

A task is complete only when:

- code is implemented;
- tests are implemented;
- tests pass;
- project builds;
- documentation is updated when needed;
- commit summary is updated in `docs/commit-summaries.md` when commits are created;
- the final response explains what changed.

## Git Rules

Do not commit automatically unless explicitly asked.

After each task, provide:

- files changed;
- tests executed;
- result;
- suggested commit message.

When one or more commits are created, also update `docs/commit-summaries.md`.

Commit messages must stay short and objective.

Use this split of responsibility:

- commit: short summary of what was done;
- pull request: complete engineering context;
- `docs/commit-summaries.md`: persistent and auditable record inside the repository.

Each new entry in `docs/commit-summaries.md` must include:

- commit hash;
- commit message;
- Changelog;
- Motivation;
- Consequences;
- Metrics;
- Test Scenarios;
- Evidence.

If more than one commit is created in the same task, each commit must have its own entry.

## Pull Request Rules

Every pull request description must include these sections:

- Changelog
- Motivation
- Consequences
- Metrics
- Test Scenarios
- Evidence
