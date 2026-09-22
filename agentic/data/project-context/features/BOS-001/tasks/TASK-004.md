# TASK-004 — Add repeatable local verification and CI

## Category
Security/DevOps

## Status
DONE. docs/LOCAL_DEVELOPMENT.md (build/start/check/stop runbook, explicit user_app working directory, troubleshooting); scripts/verify-flutter.sh and scripts/check-contracts.py added (both were already allowlisted in agentic/kit/config/allowed-commands.json, unused until now); Dockerfiles for identity-service/building-service/api-gateway (pinned eclipse-temurin digest, non-root user) — all three build and run `docker build` successfully; .github/workflows/platform.yml (backend verify, contract checks, compose+platform verification, container builds, and a separate continue-on-error Flutter baseline job so unrelated pre-existing Flutter debt doesn't block backend CI). No credentials committed (infra/docker/.env gitignored, only .env.example tracked); `docker compose down` retains named volumes.

## Objective
Add repeatable local verification and CI.

## Scope
BOS-001 platform foundation; requirements PF-06,PF-08,PF-10.

## Dependencies
TASK-001, TASK-002, TASK-003.

## Implementation Requirements
Write local development runbook, verification script, service Dockerfiles and CI checks. Use explicit working directories for user_app commands. Separate checks from service lifecycle side effects; configure effect-aware execution. Record Flutter baseline failures rather than silently fixing unrelated template debt.

## Acceptance Criteria
Developer can build/start/check/stop the foundation using documented commands. CI invokes the same core checks. No credentials committed; shutdown retains data.

## Test Requirements
Full backend verify; Compose configuration/health; secret/config review; Flutter analyze/tests when environment is available; container builds.

## References
../SRS.md; ../TECH-SPEC.md; ../ARCHITECTURE.md; ../../../BuildingOS_BRD_Agentic_Development.md

## Out of Scope
Publishing images, deployments, signing changes, unrelated Flutter fixes.
