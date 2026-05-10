# ADR-004: Testing Strategy

## Status

Accepted

## Context

The EAD Platform is a Java 25 and Spring Boot 4 microservices learning project.

The project requires automated tests for features and architectural confidence across domain rules, application use cases, persistence, messaging and HTTP contracts.

The complementary HLDs now reference Cucumber for observable integration flows. This decision records how automated tests should be organized so future FDDs, implementation plans and code changes follow a consistent testing strategy.

## Decision

We will use a layered testing strategy:

- unit tests for domain rules and application use cases;
- integration tests for persistence and messaging;
- HTTP tests for API contracts;
- Cucumber tests for observable integration and BDD flows.

Cucumber should be used to describe behavior visible at service or integration boundaries. It must not be used to test private implementation details, individual classes or low-level framework wiring.

Each feature must include tests appropriate to its risk and scope. No feature is complete if its relevant tests do not pass.

## Consequences

### Positive

- Domain and application rules can be validated quickly with unit tests.
- Persistence and messaging behavior can be validated against realistic infrastructure boundaries.
- HTTP contracts become explicit and regression-resistant.
- Cucumber scenarios provide readable end-to-end behavior for important integration flows.
- The testing strategy aligns HLDs, FDDs and implementation plans.

### Negative

- More test types increase project structure and maintenance effort.
- Cucumber scenarios can become slow or brittle if they test implementation details.
- Integration tests may require containers, local services or test infrastructure.
- Poorly scoped BDD scenarios can duplicate lower-level unit and integration tests.

## Alternatives Considered

### Unit tests only

Rejected because unit tests alone do not validate persistence, messaging, HTTP contracts or observable cross-component behavior.

### Integration tests only

Rejected because they are slower and less precise for domain and application rules.

### Cucumber for all tests

Rejected because Cucumber is better suited for observable behavior and integration flows, not low-level domain rules or internal implementation details.

### Manual testing

Rejected because the project requires automated tests and professional regression safety.
