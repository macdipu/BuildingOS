# TASK-007 — account-service: canonical phone format (local 01XXXXXXXXX)

## Status: COMPLETED (2026-09-23) — run RUN-F334D51A33AE4944B2F04BE708F7BBE1; evidence in ../REVIEW-TASK-007.md

Category: Backend (`be-agent`). Scope: `backend/account-service` auth only.
Source: TASK-003 review finding 2 (phone strings not canonicalized); operator decision
2026-09-23: store BD mobile numbers in local 11-digit form `01XXXXXXXXX`; no data migration.

## Current behavior (HEAD `6033884`)

- `StartOtpService` accepts `^\+?[0-9]{7,15}$` and stores the string as given.
- `VerifyOtpService` compares the request phone to the challenge phone by exact string and
  calls `findOrCreateByPhone` with it, so `01711…`, `8801711…`, `+8801711…` are three users.
- `JdbcOtpChallengeRepositoryAdapter.start` folds only an optional `+` for rate limiting.
- The app sends `01XXXXXXXXX` (`PhoneNumber.withoutCountryCode`); the SUPER_ADMIN seed is
  `01306999005`. So existing app-created data is already canonical.

## Design

1. Domain value object `domain/model/PhoneNumber` (framework-free):
   `PhoneNumber.parse(String raw)` accepts exactly `01[3-9]XXXXXXXX`, `8801[3-9]XXXXXXXX`,
   or `+8801[3-9]XXXXXXXX` (digits only; no spaces or dashes) and returns canonical
   `01[3-9]XXXXXXXX`; anything else → `IllegalArgumentException` (→ existing 400
   `INVALID_REQUEST`). Operator prefixes 013–019 are the current BD mobile ranges.
2. `StartOtpService`: replace the regex with `PhoneNumber.parse`; persist canonical value.
3. `VerifyOtpService`: parse the request phone; unparseable → `PHONE_MISMATCH` rejection
   (no new error code); compare and create users with the canonical value.
4. `JdbcOtpChallengeRepositoryAdapter.start`: drop the `+` folding; lock and count by the
   (already canonical) phone with `phone = ?`.
5. `SeedSuperAdminService`: parse the seed phone (already canonical; guards future edits).
6. Tests: value-object unit tests (accepted forms, rejected forms incl. landline/012/short/
   long/spaces); verify with `+8801…` after start with `01…` succeeds and yields one user;
   rate limit shared across all three forms. Existing test phones move to canonical input
   where they assert stored values.
7. Docs: `docs/AUTH_CONFIGURATION.md` phone section.

## Out of scope

Non-BD numbers; migrating existing rows (only local/test data exists; any non-canonical rows
stay unreachable and are harmless); Flutter changes (already sends canonical form); gateway.

## Acceptance

1. All three input forms map to the same challenge limits and the same user; stored and
   token `phone` claim are `01XXXXXXXXX`.
2. Invalid numbers → 400 on start; verify with unparseable phone → 401 `OTP_PHONE_MISMATCH`.
3. `mvn -B -f backend/pom.xml verify` green; ArchUnit unchanged; `verify-flutter.sh` passes.
