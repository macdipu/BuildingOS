# EPIC-BOS-009: Hardening and production readiness

Status: PROPOSED (planning draft 2026-10-22; not approved)

No dates, hours or capacity are implied. Sizes are relative (S/M/L/XL) and carry the
uncertainty noted per story. Every rule below cites the BRD
(`agentic/data/project-context/BuildingOS_BRD_Agentic_Development.md`), an existing decision
file, or a UI gap file. Open points are in [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) as `Q-BOS-009-NN`.

## Objective

Take the delivered BuildingOS platform (auth-service, building-service, subscription-service,
back-office-service + web console, api-gateway, Flutter `user_app`) from "local/test release
approved" to "production deployable with evidence": security controls (§93-94), observability
(§95), Kafka failure handling (§96), testing and contract gates (§97-99), CI/CD (§102),
environments (§103), backup/recovery (§135), retention (§136), NFRs (§142), performance
targets (§134) and the MVP success-criteria evidence (§146), plus the live SMS OTP adapter
deferred from BOS-010 TASK-003.

## Business Context

- BACKLOG (`features/BOS-001/BACKLOG.md`): BOS-009 = "Security, load/failure recovery, DLQ and
  release evidence", BRD 93-99, 102, 135-136, 142, 146; depends on prior slices. §146 #24
  (complete audit trace) is "verified in BOS-009"; audit itself ships with each feature.
- ARCHITECTURE.md (HLD draft 2026-09-24): "Production auth, scanning, backup and operational
  readiness remain their own explicit gates"; "Do not infer production deployment from
  local/test release approval"; "No general production-readiness claim".
- BOS-010 RELEASE-TASK-003: "No production deployment; production SMS login unavailable until
  the vendor task." D-01: `DevelopmentOtpProvider` is local/test only behind a replaceable
  adapter.
- docs/AUTH_CONFIGURATION.md: "Automated key management, refresh tokens, session revocation"
  are not implemented, against §93 "Refresh token rotation".

Current state observed 2026-10-22 (scoped survey, not an audit):

| Area | Present | Gap vs BRD |
|---|---|---|
| Auth | OTP challenge with attempt count, `OtpRateLimitedException`, `OtpProvider`/`SmsSender` ports, RS256 JWT + JWKS, 15-min access TTL default | No live SMS adapter; no refresh token / rotation / revocation (§93) |
| Gateway | Routing, token validation, correlation | No rate limiting, circuit breaking (§7.1, §93) |
| Observability | Actuator liveness/readiness, `prometheus` exposure, correlation ids; back-office System Health aggregates readiness (BOC-08) | No structured JSON logs, distributed tracing, consumer-lag or DLQ visibility (§95) |
| Kafka | building-service transactional outbox + publisher, `contracts/kafka` envelope + `ownership-transferred.v1` | No consumer retry/DLQ framework (§96); no idempotent-consumer store (§14.1 rule 3) |
| Tests | ArchUnit per service, Testcontainers, `scripts/check-contracts.py` | No OpenAPI breaking-change check, migration validation job, permission-matrix suite (§97-98, §102) |
| CI | PR: `mvn verify`, contracts, compose verify, image build (auth/building/subscription/gateway only; back-office-service image not built), Flutter job `continue-on-error` | No lint/format gate, security scan, versioning, registry push, deploy, smoke (§102) |
| Environments | `infra/local`, `infra/dev` compose | No staging/production definitions (§103) |
| Backup/retention | None | §135, §136 entirely |
| Harness | `agentic/kit/config/allowed-commands.json` lists only git/python/kit checks; local `python3` is 3.9.6 while CI uses 3.12 | Agents cannot run `mvn`/`npm`/`flutter` under the harness; Python scripts must stay 3.9-compatible or the toolchain must be pinned (Q-BOS-009-15) |

## Scope

### Owning services (no new service proposed)

| Concern | Owner | Note |
|---|---|---|
| Live SMS adapter, brute-force controls, refresh tokens, session revocation | auth-service (existing) | §8.1 Identity & Access; extends existing ports |
| Edge rate limiting, CORS, circuit breaking, request logging | api-gateway (existing) | §7.1; no domain logic in gateway |
| Shared logging/tracing/metrics, Kafka consumer retry + DLQ + idempotent-consumer support | platform-web (existing shared kernel) | Shared transport conventions only (ARCHITECTURE.md) |
| DLQ and consumer-lag visibility | back-office-service + `buildingos_backoffice_web` System Health (existing, BOC-08) | §95, §149.2 "System Health" |
| Retention purge jobs | each owning service | §99 "each microservice owns its migrations"; each service owns its DB |
| Audit trace verification | existing per-service audit tables + back-office aggregation (D-37, F6-T5d) | BRD §8.10/§145 names an `audit-service`; whether it is required before production is Q-BOS-009-07 (needs technical approval) |
| Infra: observability stack, Redis (if approved), environment definitions, backup | `infra/` | §103-104, §135 |

The BRD §145 service set (rental, finance, work, communication, notification, document,
reporting, audit) is not created by this epic; those are BOS-003..BOS-008.

### Data entities (proposed; field lists finalised at TECHNICAL)

- auth-service: `refresh_token` (hashed token, family id, user id, issued/expires/revoked at,
  replaced-by) - §93 rotation; `login_attempt` throttle state only if Redis is not approved
  (Q-BOS-009-04).
- platform-web consumers: `processed_event` (consumer name, `eventId`, processed at) per
  consuming service DB - §14.1 rule 3, §16.
- DLQ record is the Kafka DLQ message itself with reason headers (§96 step 4); no new table
  unless Q-BOS-009-06 chooses a persisted DLQ index.
- No change to business entities.

### Events (Kafka + outbox)

- No new domain events. §149.20 `support.session.*`, `subscription.*`, `building.*` lifecycle
  events are owned by their features (D-13 defers F2 lifecycle events to a later slice).
- New infrastructure topics: `<source-topic>.dlq` per §96 example `finance.events.dlq`
  (naming confirmed in Q-BOS-009-06).
- Optional `identity.events` `session.revoked` is NOT proposed (BRD lists no such event).

### APIs (proposed)

- `POST /api/v1/auth/token/refresh` (rotate) and `POST /api/v1/auth/logout` (revoke family) -
  §93; exact paths follow `contracts/openapi/platform.yaml` conventions at TECHNICAL.
- back-office-service: extend System Health read with consumer lag + DLQ counts per topic
  (§95). Read-only; platform permission required (§149.22).
- No new building-domain endpoints.

### Flutter screens

- No new screens. `user_app` authentication feature: silent refresh on 401/expiry and
  logout-revokes-session; OTP screen error states for rate limit (§41 "Expiration, Retry
  limits, Rate limit"; mobile mockup 03 per `ui/stitch/UI-INDEX.md` / `gaps/mobile-core.md`).
  C-11 (Google sign-in deferred) stays binding.

### Back-office impact

- System Health screen (BOC-08, `back-office/` System section, §149.2) gains DLQ and
  consumer-lag panels. `gaps/back-office.md` line ~199 notes only Audit Logs was mocked; no
  mockup exists for System Health, so it reuses the existing screen pattern (no invented UI).
- Back-office login uses the same auth refresh flow (§93).

## Out of Scope

- Building the remaining §145 services or their domain features (BOS-003..BOS-008).
- Automated billing/payment gateways (C-2, C-9, §125, §149.23).
- Google sign-in (C-11, BOS-010 D-01).
- End-to-end chat encryption (§150.12) and chat-specific NFRs (BOS-007).
- Mobile certificate pinning / root detection (BRD silent; not invented).
- Numeric SLAs/RPO/RTO beyond §134/§150.12 unless the operator supplies them (Q-BOS-009-12/13).
- Editing `agentic/kit/` itself (harness gap is reported, fixed by an operator-approved kit change).

## Stories

| Story | Title | Size |
|---|---|---|
| [STORY-001](stories/STORY-001.md) | Live SMS OTP adapter and login brute-force controls | M |
| [STORY-002](stories/STORY-002.md) | Refresh token rotation and session revocation | L |
| [STORY-003](stories/STORY-003.md) | Gateway edge hardening: rate limiting, CORS, circuit breaking | M |
| [STORY-004](stories/STORY-004.md) | Observability baseline: JSON logs, tracing, metrics, redaction | L |
| [STORY-005](stories/STORY-005.md) | Kafka consumer failure handling, DLQ and visibility | L |
| [STORY-006](stories/STORY-006.md) | Quality gates: contracts, migrations, permission matrix, audit trace | L |
| [STORY-007](stories/STORY-007.md) | CI/CD pipeline, environments and production topology | XL |
| [STORY-008](stories/STORY-008.md) | Backup, recovery and data retention | L |
| [STORY-009](stories/STORY-009.md) | Load testing and MVP release evidence | L |

## Cross-Story Dependencies

- STORY-001, 002, 003, 004, 006 are independent and can start in any order.
- STORY-003 rate limiting for OTP endpoints complements STORY-001 per-phone limits (soft).
- STORY-005 uses STORY-004 metrics for lag/DLQ export (soft: can ship counters first).
- STORY-008 needs STORY-007 environments (staging/production) for restore drills (hard).
- STORY-009 needs STORY-007 staging, STORY-004 metrics, and for full §146 evidence the domain
  epics BOS-003, BOS-004, BOS-006, BOS-007, BOS-008 (hard for criteria 11-22).

## Acceptance Criteria

1. Every §93 bullet has a passing automated test or a cited configuration evidence in the
   release record; "Never rely on Flutter UI permissions" is proven by server-side tests
   (STORY-006).
2. §94: NID and tokens/OTP codes never appear in logs (redaction test, STORY-004).
3. §95: every service emits JSON logs with `traceId`/`correlationId`, exposes metrics and
   health, and traces propagate gateway -> service -> Kafka (STORY-004/005).
4. §96: consumers retry transient errors with a bounded policy, route unrecoverable events to a
   DLQ with a recorded reason, and DLQ counts are visible to platform operators (STORY-005).
5. §97-99, §102: PR pipeline enforces lint, unit, integration, build, OpenAPI check, migration
   validation, image build and security scan; merge pipeline versions, pushes, deploys and
   smoke-tests (STORY-006/007).
6. §103: local, dev, staging and production environments are defined; no raw credentials in
   Git (§93) (STORY-007).
7. §135/§136: automated backups, PITR where available, one recorded restore drill, object
   versioning, Kafka retention/replay policy and configurable retention jobs (STORY-008).
8. §134/§142: load test report against §134 targets on staging; no duplicate financial
   transaction after retry once finance exists (STORY-009).
9. §146: a release evidence matrix maps each of criteria 1-24 to a test/run/record, with
   items owned by unbuilt epics marked NOT YET EVIDENCED, never claimed (STORY-009).

## Risks

- Production topology and hosting are undecided (Q-BOS-009-08); STORY-007/008/009 cannot
  finish without it.
- SMS vendor choice is external (Q-BOS-009-01); production login is blocked until then.
- §146 full evidence depends on five unbuilt domain epics; BOS-009 can only close
  platform-level criteria early.
- Harness cannot run `mvn`/`npm`/`flutter` (Q-BOS-009-15); agent QA evidence for every
  story depends on fixing that or on operator-run checks.
- Refresh tokens change the auth contract for both clients (Flutter, back-office web).

## References

- BRD §7.1, §8.1, §8.10, §14-16, §41, §93-99, §101-104, §110, §114, §128, §134-138, §142,
  §145-146, §149.2, §149.20-149.22, §150.12
- `features/BOS-001/BACKLOG.md`; `features/BOS-010/DECISIONS.md` (D-01, D-02, D-22..D-28, D-37,
  D-38); `features/BOS-010/RELEASE-TASK-003.md`; `features/BOS-010/TASK-003-READINESS.md`
- `ui/stitch/UI-INDEX.md` (C-1..C-13); `ui/stitch/gaps/back-office.md`; `ui/stitch/gaps/mobile-core.md`
- `agentic/data/project-context/ARCHITECTURE.md`; `docs/ARCHITECTURE.md`; `docs/AUTH_CONFIGURATION.md`
- `.github/workflows/platform.yml`; `agentic/kit/config/allowed-commands.json`
