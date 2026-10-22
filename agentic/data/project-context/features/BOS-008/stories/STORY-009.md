# STORY-009: Projection rebuild, DLQ handling and performance verification

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Make projections recoverable (replay/rebuild), observable, and verified against the §134 dashboard
target, with near-immediate cached first render on mobile.

## Upstream event sources
All topics consumed by STORY-001..007 (replay from Kafka retention, §135).

## Scope
- Operator-triggered rebuild of a projection for one building from retained events (§135 "Kafka:
  Appropriate retention, Replay policy"), without serving partial data during rebuild.
- DLQ inspection/reprocess path; financial-event DLQ alerting (§96 rule 5).
- Metrics: consumer lag, DLQ depth, projection `updated_at` staleness (§95).
- Read indexes on `building_id` (+ owner/tenant) (§110).
- Flutter: cache last dashboard payload locally and render it immediately, then refresh (§134).
- Performance test of dashboard endpoints under "normal load" (load profile per Q-BOS008-17).

## Acceptance Criteria
1. Rebuilding a building's projections from replay yields the same values as the incremental projections (§14.1 rule 3, §135).
2. A poison event goes to DLQ after bounded retry; other events keep flowing; financial-event DLQ raises an alert (§96).
3. Dashboard API P95 < 1 s under the agreed normal-load profile (§134).
4. Mobile dashboard shows cached data immediately on open, then the fresh payload; cached data is cleared on logout/building switch so no cross-building leak (§134, §110).
5. Lag and DLQ metrics visible on the existing observability stack (§95).
6. Rebuild is restricted to platform operators and audited (§114 administrative actions; §5 platform vs building context).

## Tasks
Proposed lanes:
- BE: rebuild/replay command, DLQ reprocess, metrics, indexes — M
- Mobile: dashboard cache-then-refresh — S
- QA: replay equivalence test, load test, poison-event test — M
Size: **M** (uncertainty: medium — load profile undefined).

## Dependencies
STORY-001 (start), re-run after each of STORY-002..007. BOS-009 owns release-level load/failure evidence. Q-BOS008-17.

## Out of Scope
Platform-wide load/chaos testing and release sign-off (BOS-009).

## References
BRD §5, §14.1, §95–96, §110, §114, §134–135; BACKLOG BOS-009.
