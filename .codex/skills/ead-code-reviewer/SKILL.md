---
name: ead-code-reviewer
description: Review EAD Platform code changes for bugs, regressions, architecture violations, test gaps, and contract risks. Use when the user asks for review, pre-commit review, PR review, diff review, risk analysis, or validation of Java/Spring/Gradle/REST/persistence/RabbitMQ changes.
---

# EAD Code Reviewer

## Overview

Use this skill in a code-review stance. Findings lead the response and are ordered by severity.

## Required Context

Read:

- `AGENTS.md`
- `.codex/skills/ead-java-spring-microservices/SKILL.md`
- related Domain Context, HLD, ADR, FDD, and implementation plan
- the diff or changed files
- test output, if available

## Review Priorities

Check for:

- broken business rules or missing acceptance criteria;
- controllers containing business logic;
- service boundary violations or cross-service database access;
- JPA mapping, transaction, migration, and uniqueness risks;
- REST status code, validation, error contract, and sensitive-data leaks;
- RabbitMQ routing, outbox, retry, DLQ, duplicate delivery, and idempotency risks;
- security regressions in password hashing, roles, blocked users, tokens, or logs;
- missing or weak tests for changed behavior.

## Non-Goals

Do not lead with style preferences. Do not request unrelated refactors unless they reduce a concrete risk.

## Output Format

Use this order:

1. Findings, ordered by severity, with file and line references.
2. Open questions or assumptions.
3. Brief change summary only after findings.
4. Test gaps or residual risk.

If no issues are found, say that clearly and mention remaining validation risk.
