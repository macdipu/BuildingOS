# QA-BOS-001 — SRS acceptance verification

Verified 2026-09-22 against real build/runtime evidence (TASK-005). Not release approval;
not a claim that MVP business scenarios are implemented (SRS-BOS-001 §1).

| ID | Observable acceptance (SRS-BOS-001) | Result | Evidence |
|---|---|---|---|
| PF-01 | Independent module build; no cross-domain entities/JPA imports; user_app stays at its current path | PASS | `backend/pom.xml` reactor (platform-web, identity-service, building-service, api-gateway); no JPA dependency anywhere in the reactor (plain JDBC + Flyway only); `git diff --stat user_app` is empty for this session |
| PF-02 | identity role cannot connect to building DB and vice versa; Flyway migration runs once and reruns cleanly | PASS | `scripts/verify-platform.sh` (governed run, idempotency-key `task-002-verify-platform-1`): cross-db CONNECT denied both directions; `mvn flyway:migrate` run twice per service, second run no-ops cleanly |
| PF-03 | Broker health check and produce/consume smoke pass; envelope schema includes unique eventId, eventVersion, occurredAt, producer, correlationId and buildingId when applicable | PASS | `scripts/verify-platform.sh` Kafka topic create/produce/consume/delete round trip; `contracts/kafka/event-envelope.schema.json` required fields: eventId, eventType, eventVersion, occurredAt, producer, correlationId, data (+ optional buildingId); validated by `scripts/check-contracts.py` |
| PF-04 | Success and error envelopes validated; missing/invalid token returns 401; no raw stack traces | PASS | `ApiEnvelope`/`ApiError` records; `PlatformMetadataSecurityTest`/`GatewayRoutingTest` assert envelope shape and 401 on missing/invalid token; `server.error.include-stacktrace: never` + `include-message: never` in every service's `application.yaml` |
| PF-05 | Gateway and direct services reject unsigned/expired/wrong-issuer/wrong-audience JWTs; building header alone grants no access | PASS | `PlatformMetadataSecurityTest` (identity, building) and `GatewayRoutingTest`/`GatewayDownstreamUnavailableTest`: missing/expired/wrong-audience/wrong-issuer/bad-signature all → 401 at both gateway and service; `spoofedHeaderDoesNotGrantAccess` in all three test classes |
| PF-06 | No embedded production key/password; non-local runtime refuses missing issuer/audience/database config; development issuer cannot activate accidentally | PASS | All secrets env-var-driven (`${JWT_ISSUER}` etc., `@NotBlank` + `@Validated` on `SecuritySettings`); `PlatformWebConfigurationTest` (platform-web, new this task) directly proves an `http://` issuer is refused outside `local`/`test` profiles, including with an unrelated profile active |
| PF-07 | Liveness/readiness, restricted metrics, structured logs with trace/correlation identity; no token/NID logging | PASS | `ReadinessDatabaseDownTest` (identity, building): readiness 503 / liveness 200 when DB is unreachable; `metricsRequireObserveScopeNotJustAnyValidToken` (new this task): `/actuator/prometheus` is 401 with no token and 403 with a valid-but-unscoped token; `CorrelationFilter` sets MDC `traceId`, logs via `logstash` structured format; no code path logs `Authorization` or token content |
| PF-08 | Maven verify, Compose config/health, migration/isolation and security integration tests; Flutter baseline captured when execution is configured | PASS | `mvn -B -f backend/pom.xml verify` (BUILD SUCCESS, all suites); `docker compose -f infra/docker/compose.yaml config --quiet`; `scripts/verify-platform.sh`; Flutter baseline captured in `BASELINE.md` (see below — recorded, not fixed) |
| PF-09 | Every MVP success item maps to a delivery phase; financial rules are explicit test acceptance | PASS (documentation) | `BACKLOG.md` maps BOS-001 to Phase 0; FIN-01–03/OWN-01/AUTH-01 recorded in `SRS.md` as backlog criteria, explicitly not executed in this milestone |
| PF-10 | Record API/auth mismatch; keep GetX/Clean Architecture/theme/localization; no fake dashboard, bypass login or insecure token storage introduced | PASS | `README.md`/`BASELINE.md` record the generic-starter/BRD API mismatch; zero changes to `user_app/` this session; no login bypass, fake dashboard, or token storage code added anywhere in this milestone |

## Known non-blocking gap (recorded, not fixed)

`fvm flutter test --no-pub` fails to compile under the pinned Flutter 3.44.0 toolchain
(`ui.HitTestResponse`/`ui.HitTestRequest` missing from the resolved engine — an SDK/engine
version mismatch in the pin itself, not in `user_app` code). `fvm flutter analyze --no-pub`
finds 144 pre-existing info-level lints, no errors. Both are pre-existing template debt,
out of BOS-001 scope per TASK-004's "no unrelated Flutter fixes." Full detail:
[BASELINE.md](BASELINE.md).

## Corrected during this task

The Kafka envelope schema (`contracts/kafka/event-envelope.schema.json`) was first drafted
with different field names (`envelopeVersion`, `source`, `traceId`) than SRS-BOS-001 PF-03
specifies (`eventVersion`, `producer`, `correlationId`). Caught during this SRS
cross-check and corrected before sign-off; `scripts/check-contracts.py` now asserts the
SRS-named fields.

## Unresolved questions (unchanged, still deferred)

Q-01–Q-05 remain assigned to their future slices per `SRS.md`; none blocks this milestone.
The §17 rent-invoice-owner contradiction (Q-03) is preserved, not resolved, per prior
decision.

## Explicitly not claimed

No production readiness, deployment, or release approval. No business/financial
functionality exists yet — foundation endpoints return service metadata only.

## Continuation verification — 2026-09-22

See [CONTINUATION-REVIEW.md](CONTINUATION-REVIEW.md) and
[reconciliation/checks.json](reconciliation/checks.json) for the fresh governed checks
against `463db1a` plus the continuation changes. PF-02 now additionally covers actual
fresh bootstrap with apostrophes/backslashes in passwords; PF-03 verifies the exact
produced record survives broker container recreation, not just process restart.
PF-08 now has explicit startup commands for all application JARs in the runbook.
Historical Flutter and image-build results above are reused, not presented as reruns.
