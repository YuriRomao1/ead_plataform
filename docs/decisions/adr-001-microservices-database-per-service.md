# ADR-001: Microservices with Database per Service

## Status

Accepted

## Context

The project is an EAD platform built to practice professional microservices architecture in Java.

The system needs to separate responsibilities between user management, course management and notifications.

## Decision

We will use a microservices architecture with database per service.

Initial services:

- auth-user-service
- course-service
- notification-service

Each service will own its own PostgreSQL database.

Services must not access each other's databases directly.

## Consequences

### Positive

- Services are more autonomous.
- Data ownership is clear.
- The architecture better represents real microservices.
- Each service can evolve its schema independently.

### Negative

- Queries across services become harder.
- Consistency becomes eventual in some flows.
- More infrastructure is required.
- Local development becomes more complex.

## Alternatives Considered

### Shared database

Rejected because it creates strong coupling between services.

### Modular monolith

Rejected for this project because the learning goal is microservices architecture.
