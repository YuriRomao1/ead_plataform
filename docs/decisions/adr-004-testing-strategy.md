# ADR-004: Testing Strategy

## Status

Accepted

## Context

The EAD Platform is a Java 25 and Spring Boot 4 microservices learning project.

The project requires automated tests for features and architectural confidence across domain rules, application use cases, persistence, messaging, operational behavior and HTTP contracts.

The complementary HLDs now reference Cucumber for observable integration flows. This decision records how automated tests should be organized so future FDDs, implementation plans and code changes follow a consistent testing strategy.

The first implemented service, `auth-user-service`, already uses JUnit 5, Spring Boot test support, WebMVC tests, Testcontainers with PostgreSQL, Mockito where useful, and JaCoCo coverage verification.

## Decision

We will use a layered testing strategy:

- unit tests for domain rules, value objects, event factories, application use cases and small infrastructure policies;
- HTTP contract tests for controllers, validation, correlation id behavior, error responses and OpenAPI generation;
- persistence integration tests for JPA adapters, Flyway migrations, constraints, indexes and database-backed invariants;
- messaging adapter tests for serialization, routing, publisher behavior and outbox publication state transitions;
- integration tests for transactional flows that cross application, persistence and outbox boundaries;
- Cucumber tests for observable service or cross-service BDD flows once the related FDD, implementation plan and build configuration exist.

Cucumber should be used to describe behavior visible at service or integration boundaries. It must not be used to test private implementation details, individual classes or low-level framework wiring.

Each feature must include tests appropriate to its risk and scope. No feature is complete if its relevant tests do not pass.

## Tooling

The baseline testing stack is:

- JUnit 5 for unit, integration and contract tests;
- Spring Boot test support for application contexts and WebMVC slices;
- Testcontainers for PostgreSQL-backed persistence and migration coverage;
- Mockito for focused mocks when fakes or direct assertions are not enough;
- JaCoCo for coverage reporting and verification;
- Gradle as the single entry point for test execution.

Cucumber is an accepted strategy for future observable BDD flows, but it should only be added to a module when a documented feature actually needs BDD scenarios. The current `auth-user-service` delivery does not require Cucumber yet because its behavior is covered by unit, WebMVC, persistence and integration tests.

## Coverage Policy

Implemented service modules must enforce coverage through their Gradle build.

For `auth-user-service`, JaCoCo verification is intentionally strict: covered ratio must be `1.0` for instruction, branch, line, complexity, method and class counters, excluding only the Spring Boot application bootstrap class.

Future services may start with the same strict policy. If a service needs a lower threshold, the exception must be explicit in the related FDD or implementation plan and justified by concrete constraints, not convenience.

Coverage does not replace meaningful assertions. Tests must verify behavior, contracts and side effects, not only execute code paths.

## Validation Commands

For the current implemented module, the required validation commands are:

```powershell
.\gradlew.bat :auth-user-service:test
.\gradlew.bat :auth-user-service:build
```

The module build must include test execution, Spotless checks, JaCoCo report generation and JaCoCo coverage verification.

Targeted test commands are acceptable while developing a task, but a task is not complete until the affected module test/build commands pass or an environment blocker is documented.

## Test Boundaries

Use the narrowest test type that proves the behavior:

- domain rules: plain JUnit tests without Spring context;
- application use cases: JUnit tests with simple fakes or Mockito;
- HTTP contracts: WebMVC tests that assert status codes, headers and response bodies;
- persistence rules: Spring/JPA/Flyway tests against PostgreSQL through Testcontainers;
- messaging producers: adapter tests plus outbox tests for retry, final failure and payload safety;
- cross-service behavior: Cucumber scenarios only after the participating services and contracts are documented.

Do not:

- remove, disable or weaken tests to make a build pass;
- replace precise assertions with generic non-null checks;
- add sleeps or order-dependent timing assumptions;
- test private implementation details through Cucumber;
- depend on another service's database;
- allow password, password hash, tokens, stack traces or broker/database internals to leak through HTTP responses, events or expected logs.

## Consequences

### Positive

- Domain and application rules can be validated quickly with unit tests.
- Persistence and messaging behavior can be validated against realistic infrastructure boundaries.
- HTTP contracts become explicit and regression-resistant.
- Coverage verification prevents untested production code from silently entering implemented modules.
- Cucumber scenarios provide readable end-to-end behavior for important integration flows.
- The testing strategy aligns HLDs, FDDs and implementation plans.

### Negative

- More test types increase project structure and maintenance effort.
- Strict coverage can require small tests for operational support classes.
- Cucumber scenarios can become slow or brittle if they test implementation details.
- Integration tests may require containers, local services or test infrastructure.
- Poorly scoped BDD scenarios can duplicate lower-level unit and integration tests.
- Testcontainers requires a working Docker environment for PostgreSQL-backed tests.

## Alternatives Considered

### Unit tests only

Rejected because unit tests alone do not validate persistence, messaging, HTTP contracts or observable cross-component behavior.

### Integration tests only

Rejected because they are slower and less precise for domain and application rules.

### Cucumber for all tests

Rejected because Cucumber is better suited for observable behavior and integration flows, not low-level domain rules or internal implementation details.

### Manual testing

Rejected because the project requires automated tests and professional regression safety.
