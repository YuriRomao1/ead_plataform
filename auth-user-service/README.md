# auth-user-service

`auth-user-service` owns the Auth/User bounded context for the EAD Platform.

The current delivery implements user creation only. Authentication flows such as login, JWT, refresh tokens, logout, password recovery, user blocking, user update, user deletion, and user listing are not implemented yet.

## Responsibilities

- Create users through `POST /users`.
- Validate required user input.
- Enforce unique email inside `auth_user_db`.
- Store passwords only as BCrypt hashes.
- Persist users and roles in the service-owned PostgreSQL database.
- Record `UserCreated` in Transactional Outbox after successful user creation and publish it asynchronously through RabbitMQ.
- Requeue failed outbox events through a trusted Actuator maintenance operation.
- Delete published outbox events after the configured retention window.
- Expose outbox status metrics through Micrometer/Actuator.
- Keep HTTP, persistence, security, and messaging details outside application rules.

## Local Dependencies

Start the local infrastructure from the repository root:

```bash
docker compose up -d
```

Required local services:

| Service | Port | Purpose |
| --- | ---: | --- |
| `auth-user-db` | `5432` | PostgreSQL database owned by `auth-user-service` |
| `rabbitmq` | `5672` | RabbitMQ broker for domain events |
| `rabbitmq-management` | `15672` | RabbitMQ Management UI |

RabbitMQ local credentials:

```text
username: ead
password: ead
```

## Running

Run the service from the repository root:

```bash
./gradlew :auth-user-service:bootRun
```

The service listens on:

```text
http://localhost:8081
```

## API

Interactive API documentation is available when the service is running:

```text
http://localhost:8081/swagger-ui.html
```

The generated OpenAPI JSON is available at:

```text
http://localhost:8081/v3/api-docs
```

### POST /users

Creates a user and records `UserCreated` in the outbox after successful persistence. The outbox publisher later publishes pending events to RabbitMQ.

Public registration only accepts the `STUDENT` role. `TEACHER` and `ADMIN` must be created by a future protected administrative flow.

Request:

```json
{
  "name": "User Name",
  "email": "user@email.com",
  "password": "plainPassword",
  "roles": ["STUDENT"]
}
```

Success response:

```json
{
  "id": "uuid",
  "name": "User Name",
  "email": "user@email.com",
  "status": "ACTIVE",
  "roles": ["STUDENT"],
  "createdAt": "2026-01-01T10:00:00Z"
}
```

The response never includes `password` or `passwordHash`.

Expected errors:

| Status | Code |
| ---: | --- |
| `400` | `USER_NAME_REQUIRED` |
| `400` | `USER_EMAIL_REQUIRED` |
| `400` | `USER_EMAIL_INVALID` |
| `400` | `USER_PASSWORD_REQUIRED` |
| `400` | `USER_ROLE_REQUIRED` |
| `400` | `USER_ROLE_INVALID` |
| `400` | `USER_ROLE_NOT_ALLOWED_FOR_PUBLIC_REGISTRATION` |
| `409` | `USER_EMAIL_ALREADY_EXISTS` |

## Events

### UserCreated

Recorded in the outbox after a user is created successfully and published asynchronously to RabbitMQ.

Payload:

```json
{
  "eventId": "uuid",
  "eventType": "UserCreated",
  "occurredAt": "2026-01-01T10:00:00Z",
  "payload": {
    "userId": "uuid",
    "name": "User Name",
    "email": "user@email.com"
  }
}
```

The event never includes password or password hash.

Current RabbitMQ configuration:

| Property | Default |
| --- | --- |
| Exchange | `ead.domain.events` |
| UserCreated routing key | `auth-user.user-created` |

## Outbox Operations

Published outbox records are deleted automatically after the configured retention window:

| Property | Default |
| --- | --- |
| `auth-user-service.outbox.cleanup.enabled` | `true` |
| `auth-user-service.outbox.cleanup.fixed-delay` | `86400000` |
| `auth-user-service.outbox.cleanup.retention` | `P30D` |

Failed events are not requeued automatically. After the operational cause is understood and fixed, a trusted operator can requeue failed events through the Actuator `outbox` endpoint.

The endpoint is intentionally not included in the default web exposure. To enable it in a trusted administrative environment, include `outbox` explicitly:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,outbox
```

Outbox status:

```text
GET /actuator/outbox
```

Requeue failed events:

```text
POST /actuator/outbox/requeue-failed
```

Optional request body:

```json
{
  "batchSize": 100
}
```

Delete published events older than a retention window:

```text
POST /actuator/outbox/cleanup-published
```

Optional request body:

```json
{
  "retention": "P30D"
}
```

Outbox metrics are exposed through Micrometer as:

```text
auth_user_service_outbox_events{status="PENDING"}
auth_user_service_outbox_events{status="PUBLISHED"}
auth_user_service_outbox_events{status="FAILED"}
```

## Validation

Run tests:

```bash
./gradlew :auth-user-service:test
```

Build the service:

```bash
./gradlew :auth-user-service:build
```

The tests use the `test` Spring profile and Testcontainers for PostgreSQL-backed persistence coverage.
