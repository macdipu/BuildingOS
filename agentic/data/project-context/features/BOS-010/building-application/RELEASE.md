# BOS-010 F2 — Local/test release

Status: READY for local/test, approved 2026-09-24.
Run: `RUN-5AC974860CB74DA790FDBA00532A15A2`. Release task: `eb531a5ec70e4da194783516a9f3f453`.
Implementation: `f1c6dcd06deb7f31a6d1d671bc6a771ba19e0192`; reviewed checkout:
`9054995aa23008ef21402316e2552ec849ffbcac` (subsequent kit changes only).

## Approval and verification

The operator (macdipu) replied **"yes"** on 2026-09-24 to:
"Did the device check pass, and do you approve the local/test release?"
This records the operator's device-check confirmation and explicit local/test release approval.
It is human-reported evidence; no new agent device inspection was performed in this session.
The harness recorded the release gate against scope revision 0.

[Review and automated QA](REVIEW.md) cover AP-01 through AP-10:
backend 197 tests / 0 failures, Flutter 50 tests / 0 failures and clean analysis,
contract checks/Redocly valid, and isolated live full-stack smoke passed.
The device-check scope was the application flow, upload pickers, and Bangla rendering.
Native-speaker linguistic review remains separate.

The application, contracts, infrastructure, scripts, and development docs are unchanged
between the feature commit and the reviewed checkout. This session changes release records
only; it reuses the recorded test evidence. No deployment was performed.

## Configuration and migration checklist

- Follow [local development](../../../../../../docs/LOCAL_DEVELOPMENT.md) for service startup.
  Start Postgres, Kafka and MinIO; ensure the private document bucket exists before uploads.
- Configure building-service's auth/subscription URLs, JWT issuer/JWKS/audience, database
  credentials, and S3 endpoint/bucket/credentials. Keep credentials outside version control.
- Run Flyway in order: existing V1, V2 applications/review history, V3 documents, V4 buildings
  and memberships. These are forward migrations; do not edit applied migration files.
- Run compatible auth-service, subscription-service, building-service, gateway and Flutter
  artifacts together. The internal user-provisioning endpoint stays behind service access.
- Configure the creation fee through the established workflow. Unconfigured or unpaid fees
  fail closed. The smoke script's fee override is only for a throwaway local database.

## Rollback and monitoring plan

The local operator owns startup, rollback and monitoring. Stop application writes before
recovery. Keep a coordinated backup of the local databases and MinIO objects before applying
this slice to data that must be preserved. Application approvals can provision users in auth
and refer to fees in subscription, so database and object recovery must remain consistent.

Prefer a forward fix. To return to a pre-F2 environment, stop the F2 applications and restore
the pre-change databases and object-store snapshot, then run matching prior service/client
artifacts. Do not run a down migration or delete named volumes as an automatic rollback.
A restore drill on the operator's persistent environment has not been evidenced; this is
a local/test recovery plan, not a production recovery certification.

Check /actuator/health/readiness on ports 8080–8083 and Postgres/MinIO health. Inspect service
logs for Flyway/startup errors, document-store failures, and DEPENDENCY_UNAVAILABLE.
Use scripts/smoke-building-application.sh against the chosen local gateway for lifecycle
verification; it creates test data. The prior smoke evidence used an isolated stack;
the operator's own environment/volumes were not revalidated in this release session.

## Carried risks and next work

No blockers remain for the approved local/test scope. Before production, upload virus
scanning and the existing production authentication/configuration prerequisites remain
required. Native-speaker Bangla review is still outstanding. Token-lifetime coupling,
heuristic duplicate matching, the pre-existing unknown-route 500, and activation checking
only the building admin until the units slice remain as recorded in REVIEW.md and TECH-SPEC.md.

The next planned slice is units and ownership; it needs its own scoped intake and decisions.
