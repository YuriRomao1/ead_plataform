---
name: ead-docs-governance
description: Keep EAD Platform documentation, ADRs, FDDs, implementation plans, README files, PR template, and commit summaries synchronized with code and commits. Use when the user asks to audit docs, update HLD/FDD/ADR/plan alignment, prepare a PR description, record commit summaries, verify Definition of Done, or check documentation drift after implementation.
---

# EAD Docs Governance

## Overview

Use this skill to maintain traceability between business rules, architecture decisions, implementation plans, code changes, tests, commits, and pull request context.

## Required Context

Read:

- `AGENTS.md`
- `docs/domain-context.md`
- `docs/hld.md`
- `docs/hlds/`
- `docs/decisions/`
- `docs/fdds/`
- `docs/implementation-plans/`
- `README.md` and service READMEs
- `.github/pull_request_template.md`
- `docs/commit-summaries.md`
- git diff, git log, and test evidence when relevant

## Governance Checks

- HLD states current modules and planned modules accurately.
- Component HLDs match implemented and planned service behavior.
- ADRs exist for architecture, persistence, messaging, security, observability, or testing decisions.
- FDDs define scope, exclusions, contracts, events, persistence, validations, errors, and tests before implementation.
- Implementation plans split work into small tasks with acceptance criteria and validation commands.
- READMEs do not claim unimplemented features are ready.
- Commit summaries are updated when commits are created.
- PR descriptions contain Changelog, Motivation, Consequences, Metrics, Test Scenarios, and Evidence.

## Commit Summary Rule

When one or more commits are created, ensure each commit has an entry in `docs/commit-summaries.md` with:

- commit hash;
- commit message;
- Changelog;
- Motivation;
- Consequences;
- Metrics;
- Test Scenarios;
- Evidence.

Do not update commit summaries for uncommitted work unless the user explicitly asks for a draft.

## Output

Report documentation drift, files updated, missing ADR/FDD/plan work, validation evidence, and a suggested commit message.
