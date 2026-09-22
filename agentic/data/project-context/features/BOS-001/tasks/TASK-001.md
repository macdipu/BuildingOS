# TASK-001 — Establish the backend build and governed execution setup

## Category
Security/DevOps

## Status
DONE. Technical gate approved 2026-09-22 (run RUN-F9BD7D203B614576A5B7B61637E5E05F). Maven reactor builds; `mvn -B -f backend/pom.xml verify` registered in agentic/kit/config/allowed-commands.json and passing (BUILD SUCCESS across platform-web, identity-service, building-service, api-gateway).

## Objective
Establish the backend build and governed execution setup.

## Scope
BOS-001 platform foundation; requirements PF-01,PF-08.

## Dependencies
None; explicit technical approval before implementation.

## Implementation Requirements
Create the Maven reactor and independent gateway/identity/building build modules; configure Java 17, Boot 4.1.1 and Cloud BOM 2025.1.3. Reconcile harness pins and register exact build/check commands before executing them. Record the replacement or refreshed run's provenance if the runtime requires a new run; do not edit stored approval/state JSON.

## Acceptance Criteria
All three modules compile; no cross-service persistence dependencies; exact commands and environment requirements documented. Harness timing/tool results remain traceable.

## Test Requirements
Maven compile/verify after later service tasks; dependency tree and architecture-boundary checks.

## References
../SRS.md; ../TECH-SPEC.md; ../ARCHITECTURE.md; ../../../BuildingOS_BRD_Agentic_Development.md

## Out of Scope
Real user auth, UI changes, deployment.
