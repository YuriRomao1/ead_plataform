# EAD Platform

A professional Java microservices learning project built with AI-assisted software development workflow.

## Goals

- Practice Java and Spring Boot
- Build a microservices architecture
- Use database per service
- Use RabbitMQ for asynchronous communication
- Keep a professional commit history
- Document architectural decisions

## Initial Services

- auth-user-service
- course-service
- notification-service

## auth-user-service

The `auth-user-service` is responsible for user creation in the Auth/User bounded context.

Current implemented scope:

- `POST /users`
- user persistence in the `auth_user_db` PostgreSQL database
- email uniqueness validation in the application and database
- password hashing with BCrypt
- public response without password or password hash
- `UserCreated` recorded in Transactional Outbox and published asynchronously through RabbitMQ

Out of scope for the current implementation:

- login
- JWT
- refresh tokens
- logout
- password recovery or change
- user blocking or unblocking
- user update, deletion, or listing

Run the service locally:

```bash
./gradlew :auth-user-service:bootRun
```

Run the service tests:

```bash
./gradlew :auth-user-service:test
```

Build the service:

```bash
./gradlew :auth-user-service:build
```

See the service-specific documentation in [auth-user-service/README.md](auth-user-service/README.md).

## Technical Baseline

- Java 25
- Spring Boot 4.0.x
- Gradle 9.x
- PostgreSQL 16
- RabbitMQ 3 with Management UI

Dependency versions should be managed by the Spring Boot Gradle plugin whenever possible.

## Architecture

The platform follows a microservices architecture with database per service.

- Each service owns its own PostgreSQL database.
- Services must not access each other's databases directly.
- Synchronous communication must happen through REST APIs.
- Asynchronous communication must happen through RabbitMQ events.

## Local Infrastructure

Start the local infrastructure:

```bash
docker compose up -d
```

Check running containers:

```bash
docker compose ps
```

Stop the local infrastructure:

```bash
docker compose down
```

## Infrastructure Services

| Service | Technology | Host Port | Internal Port |
| --- | --- | ---: | ---: |
| rabbitmq | RabbitMQ Management | 5672 | 5672 |
| rabbitmq-management | RabbitMQ Management UI | 15672 | 15672 |
| auth-user-db | PostgreSQL | 5432 | 5432 |
| course-db | PostgreSQL | 5433 | 5432 |
| notification-db | PostgreSQL | 5434 | 5432 |

RabbitMQ Management UI:

```text
http://localhost:15672
```

Default local credentials:

```text
username: ead
password: ead
```

## Documentation

- [Domain Context](docs/domain-context.md)
- [High-Level Design](docs/hld.md)
- [ADR-001: Microservices with Database per Service](docs/decisions/adr-001-microservices-database-per-service.md)

## Development Rules

- Code must be written in English.
- Documentation may be written in Portuguese.
- Do not implement features without related documentation unless explicitly requested.
- Do not commit automatically.
