# ADR-002: Password Hashing Strategy

## Status

Accepted

## Context

The `auth-user-service` is responsible for user credentials in the EAD Platform.

According to the domain rules, passwords must never be stored in plain text. The first delivery of the `auth-user-service` includes user creation and must persist only a password hash.

The project needs an initial password hashing strategy that is secure enough for the learning MVP, supported by Spring Boot, easy to test and simple to evolve later if needed.

## Decision

We will use BCrypt as the initial password hashing strategy.

The implementation should use a dedicated password hashing component instead of calling the hashing library directly from controllers or domain entities.

The BCrypt strength factor should be configurable by application configuration when the service is implemented.

## Consequences

### Positive

- BCrypt is designed for password hashing.
- BCrypt is widely supported in Java and Spring Security.
- BCrypt includes a salt in the generated hash.
- The work factor can be increased as hardware capacity evolves.
- The strategy is simple enough for the first version of the `auth-user-service`.

### Negative

- BCrypt is CPU-intensive by design, so high signup or login volume can increase resource usage.
- The chosen work factor will require performance testing before production use.
- Future migration to another algorithm will require compatibility handling for existing hashes.

## Alternatives Considered

### Plain text passwords

Rejected because passwords must never be stored in plain text.

### Reversible encryption

Rejected because the service does not need to recover the original password. Password verification should compare a raw candidate against a stored hash.

### PBKDF2

Rejected for the initial version because BCrypt is simpler to adopt with Spring Security defaults and is enough for the current MVP.

### Argon2

Rejected for the initial version. Argon2 is a strong modern option, but BCrypt is sufficient for the first delivery and simpler for the current project stage. Argon2 can be reconsidered in a future ADR if security requirements increase.
