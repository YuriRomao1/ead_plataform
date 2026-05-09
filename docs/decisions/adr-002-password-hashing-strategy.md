# ADR-002: Password Hashing Strategy

## Status

Accepted

## Context

The system needs to store user passwords securely.

The first version of the `auth-user-service` includes user creation and must persist passwords only as hashes. This version must be professional enough to follow secure development practices, but simple enough to evolve gradually as the platform grows.

The password hashing strategy must be supported by the Java and Spring Boot ecosystem, easy to test, and suitable for the first delivery of the project.

## Decision

We will use BCrypt to hash passwords in the first version of the `auth-user-service`.

Passwords must never be stored in plain text. The service must store only the generated BCrypt hash.

The implementation should keep password hashing behind a dedicated component so the strategy can evolve later without spreading hashing details across the application.

## Consequences

### Positive

- BCrypt is designed for password hashing.
- BCrypt is widely supported in Java and Spring Security.
- BCrypt includes a salt in the generated hash.
- BCrypt is simple to adopt in the first version of the project.
- The hashing strategy can evolve later behind a dedicated component.

### Negative

- BCrypt is CPU-intensive by design.
- The work factor will need performance evaluation before production use.
- Future migration to another algorithm will require compatibility with existing hashes.

## Alternatives Considered

### BCrypt

Accepted because it is secure enough for the first version, widely supported in the Java ecosystem, and simple to integrate with Spring Boot.

### Argon2

Rejected for the first version. Argon2 is a strong modern password hashing option, but BCrypt is simpler for the current stage of the project and is enough for the initial learning MVP.

### Plain text password

Rejected because storing passwords in plain text is insecure and violates the project rule that passwords must be stored only as hashes.
