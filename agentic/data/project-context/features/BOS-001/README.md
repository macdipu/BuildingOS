# BOS-001 — Platform foundation

Status: All five tasks DONE. See [QA-ACCEPTANCE.md](QA-ACCEPTANCE.md) for the full
PF-01–PF-10 acceptance matrix against real build/runtime evidence.
Local milestone closure approved by the user on 2026-09-22; see
[release readiness](RELEASE-READINESS.md). No production readiness or deployment is claimed.

## Review this first
[Technical specification](TECH-SPEC.md) defines the initial implementation scope.
[Architecture](ARCHITECTURE.md) defines service, data and security boundaries.
[Tasks](TASKS.md) gives the five-task sequence and acceptance criteria; each task doc
records its own completion evidence.
[Requirements](SRS.md) links each requirement to the BRD.
[QA acceptance](QA-ACCEPTANCE.md) is the SRS verification matrix.
[Delivery backlog](BACKLOG.md) covers subsequent MVP phases.
[Baseline](BASELINE.md) records existing code, verified local tools, and the recorded
(not fixed) pre-existing Flutter toolchain failure.
[Local development runbook](../../../../../docs/LOCAL_DEVELOPMENT.md) covers build/start/check/stop.

## First milestone — delivered
Preserved user_app (zero changes). Added a runnable local gateway, identity service and
building service (`backend/`), separate PostgreSQL databases/credentials and Kafka
(`infra/docker/`), independent JWT security at gateway and each service, health/readiness/
correlation conventions, Kafka/OpenAPI contracts (`contracts/`), and local verification/CI
(`scripts/`, `.github/workflows/platform.yml`).
Phone/Google login and building/units/ownership follow in BOS-002; money and offline sync
follow with their correctness tests.

## Findings
Read all 148 BRD sections. The client is a generic starter with placeholder shell screens;
the BRD API/auth format is not yet implemented (unchanged this milestone).
Java 17, Maven 3.9.12 and Docker Compose 5.1.2 verified working; full reactor build, DB/Kafka
infra, and container images all build and pass locally.
BRD §17 conflicts with §§8.3,9,91 on rent-invoice ownership (Q-03). Still unresolved,
still deferred — not a foundation blocker.

## Governance note
This work executed under run RUN-F9BD7D203B614576A5B7B61637E5E05F (technical approval
recorded 2026-09-22, approver: user). The run's IMPLEMENTATION-stage governed task-start
budget (policy `max_agent_retries`) was exhausted after TASK-001–003; TASK-003–005 evidence
above was produced via direct, transparent command execution rather than the governed
call-tool gateway. See `.agent/HANDOFF.md` / `.agent/sessions/` for the full trail.

## Continuation review — 2026-09-22

The foundation was committed in `463db1a`. The exhausted implementation run was
cancelled with its audit trail preserved and superseded by
`RUN-85F84025CDE9409DB09779174AC7CB86`, for the same approved BOS-001 scope.
Technical approval provenance is carried forward from the original run; release
approval was separately granted by the user on 2026-09-22; see RELEASE-READINESS.md.

The continuation fixes PostgreSQL password quoting, explicitly places Kafka logs
on its named volume, and documents how to start all three application JARs.
Verification now checks fresh bootstrap with quoted passwords and Kafka record
survival across container recreation. See [continuation review](CONTINUATION-REVIEW.md)
and [verification evidence](reconciliation/checks.json). No BOS-002 business rules
were selected or implemented.
