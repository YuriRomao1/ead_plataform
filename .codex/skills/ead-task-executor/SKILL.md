---
name: ead-task-executor
description: Execute one documented EAD Platform implementation task end to end with Java 25, Spring Boot 4, Gradle, tests, and documentation alignment. Use when the user asks to implement a specific task from an implementation plan, build a documented feature slice, fix a documented bug, or continue work after an FDD and plan already exist.
---

# EAD Task Executor

## Overview

Use this skill to implement exactly one documented task with focused code changes, tests, validation, and a clear completion report.

## Required Context

Read these files before editing:

- `AGENTS.md`
- `docs/domain-context.md`
- `docs/hld.md`
- related HLD in `docs/hlds/`
- related ADRs in `docs/decisions/`
- related FDD in `docs/fdds/`
- related plan in `docs/implementation-plans/`
- affected source and test files

Also use `.codex/skills/ead-java-spring-microservices/SKILL.md`.

## Execution Workflow

1. Confirm the task ID or smallest coherent task from the implementation plan.
2. Inspect current code, tests, and git status before editing.
3. Implement domain/application behavior before infrastructure adapters.
4. Keep controllers thin: validate input, call use cases, return responses.
5. Add or update tests at the narrowest useful level.
6. Run targeted validation first, then module build when feasible.
7. Update documentation only when the task changes documented behavior.
8. Do not commit unless explicitly requested.

## Guardrails

- Work on one task at a time.
- Do not refactor unrelated code.
- Do not access another service's database.
- Do not introduce architecture changes without ADR work.
- Code, comments, package names, API fields, table names, and commit messages must be in English.
- Documentation may be Portuguese.
- Preserve user changes in the working tree.

## Validation

Prefer commands scoped to the affected service, for example:

```powershell
.\gradlew.bat :auth-user-service:test
.\gradlew.bat :auth-user-service:build
```

If Docker/Testcontainers is required and Docker is unavailable, report that as environmental risk instead of weakening tests.

## Final Report

Report files changed, tests executed, result, risks, and a short suggested commit message.
