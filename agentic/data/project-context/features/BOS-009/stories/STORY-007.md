# STORY-007: CI/CD pipeline, environments and production topology

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Complete §102 (PR and merge pipelines) and §103 (local, dev, staging, production), and define
a production deployment topology that the operator approves technically. Today CI runs
`mvn verify`, contract checks, compose verification and image builds for four services
(back-office-service and the back-office web are not built as images); there is no lint gate,
security scan, versioning, registry push, deployment or smoke test.

## Scope
- PR pipeline (§102): format/lint (Java, Dart, TypeScript), unit, integration, build, OpenAPI
  check + migration validation (from STORY-006), Docker image build for every deployable
  including back-office-service and `buildingos_backoffice_web`, security scan (dependency +
  image + secret scan, Q-BOS-009-09).
- Merge pipeline (§102): version (Q-BOS-009-10), build image, push registry, deploy to dev
  automatically, promote to staging/production by an explicit approval step, smoke test after
  each deploy (extend `scripts/smoke-building-application.sh` pattern).
- Environment definitions (§103): dev, staging, production config overlays; secrets from a
  secret manager/env, never Git (§93); `DevelopmentOtpProvider` disabled outside local/test
  (D-01); D-02 SUPER_ADMIN seed via deploy-time config.
- Production topology document + IaC skeleton per Q-BOS-009-08: one deployment per service
  (§142 "independently scalable"), one database per service (ARCHITECTURE.md), Kafka, S3 per
  D-28, TLS on all external and internal hops (§93 "Encryption in transit"), ingress in front of
  api-gateway, observability stack from STORY-004.
- Harness gap note: agent-run QA needs `mvn`, `npm`, `flutter` in
  `agentic/kit/config/allowed-commands.json`, and Python tooling must match CI (local 3.9.6 vs
  CI 3.12) - Q-BOS-009-15; kit change itself is outside this story.

## Acceptance Criteria
1. A PR failing any §102 PR step blocks merge (branch protection documented).
2. Every deployable (api-gateway, auth, building, subscription, back-office services,
   back-office web) produces a versioned image on merge and is pushed to the registry (§102).
3. A merge deploys to dev and the smoke test passes (login via dev OTP, read building list);
   failure marks the deploy failed (§102 "Smoke test").
4. Staging and production deploys require an explicit human approval in the pipeline; no
   production deploy is triggered by agents (AGENTS.md "Never infer approval").
5. No credential exists in Git; secret scan passes; each environment reads secrets from the
   chosen secret store (§93).
6. Production config refuses development OTP and dev JWKS (D-01; docs/AUTH_CONFIGURATION.md).
7. Topology document is approved by the operator before any production resource is created
   (ARCHITECTURE.md "Do not infer production deployment").
8. §101 build standard checklist (Dockerfile, README, OpenAPI, Flyway, application-{local,test}
   yml, health, metrics, Testcontainers) is verified per service in CI.

## Tasks
Proposed task lanes only:
- Infra/CI: PR pipeline lint + scan + full image matrix - M
- Infra/CI: merge pipeline version/push/deploy/smoke - L
- Infra: environment overlays + secret manager wiring - M
- Infra: production topology doc + IaC skeleton - L
- Web: back-office web Dockerfile/image - S
- QA: smoke suite per environment - S

Size: XL (uncertainty: high; hosting not chosen, may split into 007a pipeline / 007b topology
once Q-BOS-009-08 is answered).

## Dependencies
- Hard: Q-BOS-009-08 (topology/hosting), Q-BOS-009-09 (scan tools + registry),
  Q-BOS-009-10 (versioning). Soft: STORY-004 (observability in each env), STORY-006 gates.

## Out of Scope
- Multi-region or active-active (BRD silent). Blue/green vs rolling choice beyond what the
  topology decision states.

## References
BRD §93, §101, §102, §103, §142; BOS-010 DECISIONS D-01, D-02, D-09, D-28;
agentic/data/project-context/ARCHITECTURE.md; .github/workflows/platform.yml;
agentic/kit/config/allowed-commands.json; infra/local, infra/dev.
