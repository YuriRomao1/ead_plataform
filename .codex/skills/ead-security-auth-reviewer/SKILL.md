---
name: ead-security-auth-reviewer
description: Review and plan EAD Platform authentication, authorization, password, role, token, blocked-user, and service-to-service security behavior. Use when work touches login, JWT, refresh tokens, logout, password recovery/change, BCrypt, STUDENT/TEACHER/ADMIN roles, blocked users, admin flows, interservice auth, API Gateway, security ADRs, or sensitive data handling.
---

# EAD Security Auth Reviewer

## Overview

Use this skill to prevent security regressions while the Auth/User context evolves beyond public user registration.

## Required Context

Read:

- `AGENTS.md`
- `.codex/skills/ead-java-spring-microservices/SKILL.md`
- `docs/domain-context.md`
- `docs/hld.md`
- `docs/hlds/hld-001-auth-user-service.md`
- `docs/decisions/adr-002-password-hashing-strategy.md`
- related auth/security FDD, plan, and ADRs

## Security Checklist

- Passwords are stored only as hashes and never returned, logged, or published.
- BCrypt strength remains configurable.
- Public registration accepts only `STUDENT` unless a protected admin flow exists.
- `TEACHER` and `ADMIN` creation requires protected authorization.
- Blocked users cannot authenticate.
- Tokens do not expose secrets or unnecessary personal data.
- Refresh tokens, revocation, and expiry are documented before implementation.
- Service-to-service auth is documented before `course-service` trusts Auth/User decisions.
- Errors do not expose stack traces, database details, token internals, or password state.

## Decisions That Need ADRs

Require or recommend ADR work for:

- token format and signing strategy;
- refresh token persistence and revocation;
- validation of tokens between services;
- API Gateway versus direct service exposure;
- service-to-service authentication;
- authorization propagation to `course-service`.

## Review Output

Report concrete security risks first, then missing tests, missing documentation/ADR work, and suggested next steps.
