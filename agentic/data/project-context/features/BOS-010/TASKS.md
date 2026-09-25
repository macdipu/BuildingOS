# BOS-010 implementation sequence (identity & platform roles feature only)

BOS-010 remains a provisional EPIC (see FEATURE.md). This sequence covers the
identity, platform roles, and phone OTP slice. Execute existing tasks without
recreating planning artifacts; TASK-003 uses TASK_ONLY / NO_REPLAN.

| Task | Outcome | Status |
|---|---|---|
| [TASK-001](tasks/TASK-001.md) | Backend global identity, roles, seed SUPER_ADMIN, development login | COMPLETED; local/test release in [RELEASE.md](RELEASE.md) |
| [TASK-002](tasks/TASK-002.md) | Flutter phone OTP integration | COMPLETED; automated and live QA in [REVIEW-QA.md](REVIEW-QA.md) |
| [TASK-004](tasks/TASK-004.md) | Architecture conformance and account-service rename | COMPLETED; [review/QA](REVIEW-TASK-004.md) |
| [TASK-005](tasks/TASK-005.md) | Locale toggle and login/OTP polish | COMPLETED; [review/QA](REVIEW-TASK-005.md) |
| [TASK-003](tasks/TASK-003.md) | Account-service signing keys and provider-neutral OTP/SMS boundary; vendor deferred | COMPLETED; [review/QA](REVIEW-TASK-003.md), [release](RELEASE-TASK-003.md) local/test, production fail-closed |
| [TASK-006](tasks/TASK-006.md) | App-owned message for OTP start rate limit (429) | COMPLETED; [review/QA](REVIEW-TASK-006.md) |
| [TASK-007](tasks/TASK-007.md) | Canonical BD mobile phone format `01XXXXXXXXX` in account-service | COMPLETED; [review/QA](REVIEW-TASK-007.md) |
| [TASK-008](tasks/TASK-008.md) | Rename `account-service` to `auth-service` (dir, artifact, package, DB, env, gateway route) | COMPLETED 2026-09-24; `mvn verify` 147/0 |

Continuation run: `RUN-0CF5A7A84513476697D476E1334896C1`.
The old TASK-003 run was cancelled before code changes. TASK-004 is now complete.
[Post-cleanup readiness](TASK-003-READINESS.md) records the concrete design and
validation plan. Implementation evidence:
[task-003-implementation-result.json](task-003-implementation-result.json).

Follow the repository commit policy: commit after a governed task finishes with green checks, or on an explicit operator request.

## Feature slices (after the identity slice)

| Slice | Outcome | Status |
|---|---|---|
| [F5a revenue foundation](subscription-plans/REQUIREMENTS.md) | `subscription-service`: plans, per-user subscriptions, free tier, one-time building-creation fee ([design](subscription-plans/TECH-SPEC.md)) | COMPLETED 2026-09-23 (local/test); [review/QA](subscription-plans/REVIEW.md); run `RUN-4ABC6B2BAC974B0E878CA39C9A27AE00` |
| [F2 building application & lifecycle](building-application/REQUIREMENTS.md) | Apply → review → approve (creation fee, D-26; admin provisioned in auth-service, D-29) → onboarding → activate/suspend (D-30); documents in MinIO/S3 (D-28); Flutter submit flow ([design](building-application/TECH-SPEC.md)) | COMPLETED 2026-09-24 (local/test); T1–T5 done; [review/QA](building-application/REVIEW.md) READY (backend 197/0, live smoke, Flutter 50/0); operator confirmed device check and approved [release](building-application/RELEASE.md); run `RUN-5AC974860CB74DA790FDBA00532A15A2` |
| [F4 units & ownership](units-ownership/FEATURE.md) | Carried BOS-002 domain reconciled with F2; [requirements](units-ownership/REQUIREMENTS.md), [decisions](units-ownership/DECISIONS.md), [delivery proposal](units-ownership/DELIVERY-PLAN.md) | COMPLETED 2026-09-25 (local/test); F4-T1..T9 done (F4-T1a..T4b on `RUN-C73FA516545143FAA9B35B168EEFE0C9`/`RUN-0BF1AD24`; F4-T9 QA/REVIEW/RELEASE on `RUN-0C7A44A5EE0743098A534883071D5BB1`, superseding the former after a kit-config change); review READY, QA READY with one documented exclusion at release time (ownership-transfer document upload/download unverified — MinIO's own registry denied anonymous image pulls on quay.io and Docker Hub), release approved by operator; commits `3ec5219` (ApiClient session-loss-after-login fix, found live during device QA), `32d13c4`. **Exclusion resolved 2026-09-25**: switched to `bitnamilegacy/minio` (frozen, still-pullable) and fixed a `MinIOContainer` data-dir/non-root-user mismatch (`476a3f9`) — `DocumentApiIntegrationTest`/`TransferDocumentApiIntegrationTest` now pass; full backend `mvn verify` 292/0. |
| Back-office console (F6, proposed) | UI for the above (Dashboard/Buildings/Users/Subscriptions/Support/System, BO-01/BO-02, ON-03) | Not started; D-09/D-10 resolved 2026-09-25 (DECISIONS.md) -- ready for requirements/technical-design intake as a new_feature (separate web stack per D-09, outside the Flutter+Spring monorepo conventions used elsewhere) |

Revenue model is per-user (D-22..D-27), superseding the BRD's building-level subscription.
