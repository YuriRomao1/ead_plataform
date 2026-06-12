---
name: ead-feature-planner
description: Create or update EAD Platform Feature Design Documents and implementation plans from Domain Context, HLDs, ADRs, and repository state. Use when planning a new feature, endpoint, service capability, REST integration, RabbitMQ event flow, persistence change, or when the user asks for FDD, feature design, implementation plan, planning, roadmap breakdown, or task sequencing before code.
---

# EAD Feature Planner

## Overview

Use this skill to turn documented product intent into an executable engineering plan for the EAD Platform. Do not implement code while using this skill.

## Required Context

Read these files when they exist:

- `AGENTS.md`
- `docs/domain-context.md`
- `docs/hld.md`
- related HLD in `docs/hlds/`
- related ADRs in `docs/decisions/`
- related FDDs in `docs/fdds/`
- related plans in `docs/implementation-plans/`
- `settings.gradle`, root `build.gradle`, and affected service `build.gradle`

Also consult `.codex/skills/ead-java-spring-microservices/SKILL.md` for Java, Spring Boot, Gradle, persistence, REST, RabbitMQ, and testing guardrails.

## Workflow

1. Identify the bounded context, service, business rules, and architecture decisions affected.
2. Check whether the requested work needs a new ADR before feature planning.
3. Create or update one FDD for the feature, keeping business rules, contracts, events, persistence, validations, errors, observability, and tests explicit.
4. Create or update one implementation plan that breaks the FDD into small reviewable tasks.
5. Keep each task scoped to one service and one coherent behavior.
6. Include expected files, acceptance criteria, validation commands, and suggested commit message per task.
7. Mark uncertainties as open questions instead of inventing hidden decisions.

## Planning Rules

- Do not create code.
- Do not plan undocumented architecture changes without an ADR task.
- Keep documentation in Portuguese when writing project docs.
- Keep code-facing names, API fields, table names, class names, and commit messages in English.
- Prefer the existing monorepo Gradle and service layering patterns.
- Include tests as required work, not optional follow-up.
- Separate producer retry, consumer retry, idempotency, and DLQ concerns when planning messaging.

## Output

- Summary of created or updated planning documents.
- Files changed.
- Open ADRs or decisions still needed.
- Suggested next implementation task.
- Suggested short commit message.
