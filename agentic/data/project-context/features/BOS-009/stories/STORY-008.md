# STORY-008: Backup, recovery and data retention

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Meet §135 (backups, PITR, restore drills, object versioning, Kafka retention/replay) and §136
(configurable retention) before production scale.

## Scope
- Per service database (auth_db, building_db, subscription_db, back-office DB): automated
  backups and point-in-time recovery where the chosen platform provides it (§135); retention of
  backups per Q-BOS-009-12.
- Restore drill: restore one service DB into staging from backup to a chosen point in time,
  verify with smoke + row-count checks; record the drill (§135 "Restore drills").
- Object storage (MinIO local, S3 production, D-28): bucket versioning for verification and
  ownership documents (§135 "Versioning where practical").
- Kafka: per-topic retention and documented replay policy (§135); DLQ retention (STORY-005).
- Retention jobs (§136): configurable purge per owning service for session/security records -
  expired OTP challenges (auth), rotated/revoked refresh tokens (STORY-002), ended support
  sessions' transient data only if allowed (D-36 keeps history). Financial and audit records are
  long-term and have no purge job (§136). Defaults off until the operator sets periods
  (Q-BOS-009-11).
- Personal-data deletion request handling: documented procedure that respects
  legal/business obligations (§136); no automated deletion invented.

## Acceptance Criteria
1. Each service DB has automated backups enabled in staging and production, verified by
   configuration evidence (§135).
2. One restore drill per service DB is executed in staging and recorded with result; restored
   data passes smoke checks (§135).
3. Document buckets have versioning on; overwriting/deleting an object keeps a recoverable
   version (§135, D-28).
4. Kafka topics have explicit retention configured; replay from a retained offset is
   demonstrated once (§135).
5. Retention jobs read their periods from configuration; with no period set they do nothing;
   with a period set they delete only eligible session/security rows; tests prove financial and
   audit tables are never touched (§136).
6. Purge runs are logged and counted in metrics (§95) and are idempotent on re-run.

## Tasks
Proposed task lanes only:
- Infra: DB backup/PITR + bucket versioning + Kafka retention config - M
- BE/DB: retention purge jobs per service (auth first) - M
- QA: restore drill execution + record; purge tests - M
- Docs: personal-data deletion procedure - S

Size: L (uncertainty: high; depends on hosting capabilities).

## Dependencies
- Hard: STORY-007 (staging/production environments), Q-BOS-009-08, Q-BOS-009-11,
  Q-BOS-009-12. Soft: STORY-002 (refresh tokens to purge), STORY-005 (DLQ retention).

## Out of Scope
- Chat message retention (§150.3 OPEN; BOS-007). Legal hold tooling.

## References
BRD §95, §135, §136, §150.3; BOS-010 DECISIONS D-28, D-36, D-37.
