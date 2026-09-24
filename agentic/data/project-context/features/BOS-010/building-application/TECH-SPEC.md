# BOS-010 F2 — Technical design: building application & lifecycle

Status: IMPLEMENTED (revision 1, technical gate approved by operator 2026-09-24) — see §12 for evidence and deviations; REVIEW next.
Requirements: [REQUIREMENTS.md](REQUIREMENTS.md) AP-01..AP-10. Architecture:
[docs/ARCHITECTURE.md](../../../../../../docs/ARCHITECTURE.md). Mirrors subscription-service.

## 1. Where things live

| Concern | Owner |
|---|---|
| Applications, documents metadata, notes, transitions, buildings, memberships | building-service (`building_db`, Flyway `V2__building_application.sql`) |
| Document bytes | MinIO/S3 bucket `building-documents` via `DocumentStorage` port (D-28) |
| Initial admin user | auth-service new internal endpoint (D-29) |
| Creation-fee status | subscription-service existing `GET /api/v1/platform/fees/BUILDING_CREATION/status` |
| Customer UI | `user_app/lib/features/building_application/` (GetX, same layering as `authentication`) |

**Service-to-service auth: token relay.** building-service calls auth-service and
subscription-service directly (`AUTH_SERVICE_URL`, `SUBSCRIPTION_SERVICE_URL`, not through the
gateway) forwarding the approver's bearer token. Both already validate the token and both accept
`SUPER_ADMIN`/`PLATFORM_ADMIN`, so no client credentials or new trust machinery is needed now.
Outbound clients sit behind application ports (`CreationFeeGateway`, `UserProvisioning`) with
`RestClient` adapters, connect/read timeouts, and failure → 503 `DEPENDENCY_UNAVAILABLE`.

## 2. building-service feature packages (one use case per action)

- `application` — `BuildingApplication` aggregate (state machine in the domain, transitions
  return a `Transition` value), `ApplicationNumber` (`BA-{yyyy}-{6-digit sequence}`, DB sequence),
  `ApplicationStatus`, `BuildingType`, `ApplicantRelationship`, `ManagementType`, `Coordinates`.
  Use cases: `createapplication`, `updateapplication`, `submitapplication`, `getapplication`,
  `listmyapplications`, `listapplications` (platform), `startreview`, `requestinformation`,
  `rejectapplication`, `approveapplication`, `getapplicationhistory`.
- `document` — `ApplicationDocument` metadata; use cases `uploaddocument`, `removedocument`,
  `downloaddocument`; port `DocumentStorage` (`put`, `open`, `delete`), adapter `S3DocumentStorage`
  (AWS SDK v2, endpoint override + path-style for MinIO). Upload writes the object, then the row;
  a failed row insert deletes the object. Remove deletes the row, then the object (orphan object
  tolerated, never an orphan row).
- `note` — `InternalNote`; use cases `addnote`, `listnotes` (platform only).
- `duplicate` — `DuplicateSignalFinder` port, JDBC adapter; use case `finddupsignals`.
  Normalization (domain, unit-tested): lowercase, Unicode NFKC, strip punctuation, collapse
  whitespace, drop common tokens (`building`, `tower`, `bhaban`, `house`, `road`, `rd`) —
  the token list is configuration. Coordinates: haversine ≤ `duplicate.radius-meters` (default 100).
- `building` — `Building` (`id`, `application_id`, name/type/address fields copied at approval,
  `status` `ONBOARDING|ACTIVE|SUSPENDED`), `BuildingMembership` (`building_id`, `user_id`, `role`
  `BUILDING_ADMIN`, `status` `ACTIVE`); use cases `activatebuilding`, `suspendbuilding`,
  `reactivatebuilding`, `getbuilding`.
- `shared` — `Actor`/`CurrentActor` (copied from subscription-service), `TransitionRecorder` port +
  JDBC adapter, platform-role check `PlatformAdminPolicy` (`SUPER_ADMIN|PLATFORM_ADMIN`).

## 3. Schema (`V2__building_application.sql`, forward-only)

```sql
create sequence building_application_number_seq;
building_application(id uuid pk, application_number varchar(16) unique not null,
  applicant_user_id uuid not null, building_name varchar(200), building_type varchar(16),
  address varchar(500), area varchar(120), district varchar(120), postal_code varchar(16),
  total_floors int check (total_floors > 0), estimated_units int check (estimated_units > 0),
  applicant_relationship varchar(24), relationship_note varchar(200),
  contact_name varchar(200), contact_phone varchar(11), contact_email varchar(254),
  management_type varchar(24), latitude numeric(9,6), longitude numeric(9,6),
  source varchar(16) not null, status varchar(32) not null,
  submitted_at timestamptz, reviewed_at timestamptz, reviewed_by uuid, rejection_reason varchar(1000),
  info_request_message varchar(1000), version int not null,
  created_at timestamptz not null, updated_at timestamptz not null)
  -- draft columns nullable; submit enforces completeness in the domain
application_document(id uuid pk, application_id uuid not null references building_application,
  object_key varchar(200) unique not null, file_name varchar(255) not null,
  content_type varchar(100) not null, size_bytes bigint not null, uploaded_by uuid not null,
  uploaded_at timestamptz not null)
internal_note(id uuid pk, application_id uuid not null references building_application,
  author_user_id uuid not null, body varchar(2000) not null, created_at timestamptz not null)
building(id uuid pk, application_id uuid unique not null references building_application,
  name varchar(200) not null, building_type varchar(16) not null, address varchar(500) not null,
  area varchar(120) not null, district varchar(120) not null, postal_code varchar(16),
  latitude numeric(9,6), longitude numeric(9,6), contact_phone varchar(11) not null,
  status varchar(16) not null, version int not null, created_at timestamptz, updated_at timestamptz)
building_membership(id uuid pk, building_id uuid not null references building, user_id uuid not null,
  role varchar(32) not null, status varchar(16) not null, created_at timestamptz,
  unique (building_id, user_id, role))
lifecycle_transition(id uuid pk, entity_type varchar(16) not null, entity_id uuid not null,
  from_status varchar(32), to_status varchar(32) not null, actor_user_id uuid not null,
  reason varchar(1000), occurred_at timestamptz not null)
  -- index (entity_type, entity_id, occurred_at)
```

Optimistic locking (`version`) on application and building: concurrent review actions → one
winner, loser 409 `CONCURRENT_MODIFICATION`. `building.application_id unique` makes double
approval impossible even across instances.

## 4. API (all under gateway; `ApiEnvelope`/`ApiError` from platform-web)

Customer (authenticated; ownership = token `sub`, others' applications → 404):

| Method & path | Use case |
|---|---|
| `POST /api/v1/building-applications` | create draft (partial body allowed) |
| `GET /api/v1/me/building-applications` | list own |
| `GET/PUT /api/v1/building-applications/{id}` | read (owner or platform admin) / edit draft |
| `POST /api/v1/building-applications/{id}/submit` | submit |
| `GET /api/v1/building-applications/{id}/history` | transitions (no internal notes) |
| `POST /api/v1/building-applications/{id}/documents` (multipart) | upload |
| `GET/DELETE /api/v1/building-applications/{id}/documents/{docId}` | download (stream) / remove |

Platform (`SUPER_ADMIN|PLATFORM_ADMIN`):

| Method & path | Use case |
|---|---|
| `GET /api/v1/platform/building-applications?status=&page=&size=` | list |
| `POST /api/v1/platform/building-applications/{id}/start-review` | `SUBMITTED → UNDER_REVIEW` |
| `POST …/{id}/request-information` `{message}` | `→ MORE_INFORMATION_REQUIRED` |
| `POST …/{id}/reject` `{reason}` | `→ REJECTED` |
| `POST …/{id}/approve` `{adminPhone, reason}` | AP-08 |
| `GET …/{id}/duplicates` | AP-07 signals |
| `GET/POST …/{id}/notes` | internal notes |
| `GET /api/v1/platform/buildings/{id}` | building + admins |
| `POST /api/v1/platform/buildings/{id}/activate` `{reason}` / `suspend` / `reactivate` | AP-09 |

Error codes: `APPLICATION_NOT_FOUND` 404, `BUILDING_NOT_FOUND` 404, `INVALID_TRANSITION` 409,
`APPLICATION_INCOMPLETE` 400 (lists missing fields), `NOT_EDITABLE` 409, `CONCURRENT_MODIFICATION`
409, `CREATION_FEE_UNPAID` 409, `FEE_NOT_CONFIGURED` 409 (passed through), `NO_BUILDING_ADMIN` 409,
`DOCUMENT_TOO_LARGE` 413, `UNSUPPORTED_DOCUMENT_TYPE` 415, `DOCUMENT_LIMIT_REACHED` 409,
`DEPENDENCY_UNAVAILABLE` 503; validation → 400 `INVALID_REQUEST`.

Document policy (config, `buildingos.documents.*`): max 10 MB each, max 10 per application,
types `application/pdf`, `image/jpeg`, `image/png` checked by magic bytes (not only header).
Multipart limit in Spring and gateway raised to match. Object key `applications/{appId}/{docId}`;
the original file name is metadata only.

## 5. auth-service addition (D-29)

`POST /internal/users/provision` `{phone}` → `{userId}` using existing
`UserRepository.findOrCreateByPhone` and `PhoneNumber.parse`; requires
`platform_role.SUPER_ADMIN|PLATFORM_ADMIN`; idempotent; audited through the existing logger
(no auth-service audit table exists). Not routed by the gateway. New use case
`provisionuser` in feature `auth`.

## 6. Approve sequence (AP-08)

1. Load application, check `UNDER_REVIEW` and admin phone valid (fail fast, no remote calls).
2. `CreationFeeGateway.status(applicationId)` → `SETTLED|NOT_REQUIRED` continue; `UNPAID` 409.
3. `UserProvisioning.provision(adminPhone)` → user id (idempotent, safe to retry).
4. One transaction: application `APPROVED` (version check), insert `building` (`ONBOARDING`),
   insert `BUILDING_ADMIN` membership, two transition rows (application, building).
A failure after step 3 leaves at most a provisioned user with no membership — harmless, the user
exists on first login anyway.

## 7. Infra and contracts

- `infra/docker/compose.yaml`: `minio` service (pinned image, healthcheck, volume) and a one-shot
  `minio-init` (`mc mb --ignore-existing`); building-service gets `DOCUMENTS_S3_ENDPOINT`,
  `DOCUMENTS_S3_BUCKET`, `DOCUMENTS_S3_ACCESS_KEY`, `DOCUMENTS_S3_SECRET_KEY`,
  `AUTH_SERVICE_URL`, `SUBSCRIPTION_SERVICE_URL`. `.env.example` placeholders, no real secrets.
  Production: same variables pointing to S3 (no endpoint override).
- Gateway: pass-through route `building-api` for the customer and platform paths above.
- `contracts/openapi/platform.yaml` paths + schemas; `docs/LOCAL_DEVELOPMENT.md` MinIO notes;
  `scripts/verify-platform.sh` exercises draft → submit → review → approve with a recorded fee.

## 8. Flutter (AP-10)

`features/building_application/` with `data` (Dio datasource, models, mappers, repository impl),
`domain` (entities, repository, one use case per action), `presentation` (GetX controllers via
`Get.lazyPut` bindings, pages: list, form, detail). Documents via existing `image_picker` /
`file_picker`; upload multipart with progress. Canonical phone formatting reused from the auth
feature. Errors mapped from `ApiError.code` to localized messages (en, bn). Entry: a "Register a
building" action from the home shell.

## 9. Tests

Unit: state machine (every legal and illegal edge), completeness rules, relationship `OTHER` note,
coordinates pairing, name/address normalization, haversine, approve orchestration with fakes
(unpaid, not configured, dependency down, happy path), each use case. Integration (Testcontainers
Postgres + MinIO, WireMock for auth/subscription): full lifecycle, ownership isolation (user B →
404), 401/403 per endpoint group, concurrent approve → one building, upload type/size/magic-bytes
rejection, download streams same bytes, transition rows per change, notes hidden from applicant.
auth-service: provision idempotent + 403. Gateway route tests. Flutter: controller and use case
tests, form validation, widget test for list/form, `flutter analyze`. ArchUnit, `check-contracts.py`,
`mvn -B verify`, `verify-platform.sh`.

## 10. Task split (implementation order)

| Task | Category | Content |
|---|---|---|
| F2-T1 | BE | Schema, application aggregate + customer/platform review endpoints (not approve), transitions, notes, history |
| F2-T2 | DB/Integration | MinIO infra, `DocumentStorage`, document endpoints |
| F2-T3 | BE | auth-service provision, fee/provision clients, approve, building activate/suspend/reactivate, duplicate signals |
| F2-T4 | BE | Gateway routes, OpenAPI, verify-platform, docs |
| F2-T5 | Mobile | Flutter feature (list, form, documents, submit/resubmit) |

## 11. Risks / accepted

Token relay couples outbound calls to the approver's token lifetime (short approve call, fine now;
revisit when async jobs call other services). Duplicate matching is heuristic by design (signals
only). No virus scanning of uploads (content type by magic bytes only) — flag for security before
production. Only `SUPER_ADMIN` exists today; tests mint `PLATFORM_ADMIN` tokens.

Estimate: agent ~6 h across 2–3 sessions; human ~1 h (spec review ~15 min, API smoke ~20 min,
device check of Flutter flow ~25 min).

## 12. Implementation progress

| Task | Status | Evidence |
|---|---|---|
| F2-T1 | DONE 2026-09-24 | `V2__building_application.sql`; packages `buildingapplication`, `note`, `shared`; `BuildingApplicationTest` (7), `BuildingApplicationApiIntegrationTest` (9); building-service `mvn verify` 33/0 incl. ArchUnit |
| F2-T2 | DONE 2026-09-24 | `V3__application_document.sql`; package `document` (`DocumentStorage` port, `S3DocumentStorage`); compose `minio` + `minio-init` (quay.io images pinned by digest; smoke: bucket created private, init idempotent); `verify-platform.sh` requires MinIO env and checks bucket init; `DocumentPolicyTest` (3), `DocumentApiIntegrationTest` (4, Testcontainers Postgres + MinIO); building-service `mvn verify` 40/0 |
| F2-T3 | DONE 2026-09-24 | auth-service `provisionuser` + `POST /internal/users/provision` (`UserProvisioningIntegrationTest` 3); building-service `V4__building.sql`, features `building` (activate/suspend/reactivate/get), `duplicate`, approve use case with `CreationFeeGateway`/`UserProvisioning` ports and RestClient adapters (token relay); `BuildingTest` (2), `DuplicateMatcherTest` (4), `ApprovalLifecycleIntegrationTest` (8, JDK-server stubs for auth/subscription); full backend `mvn verify` 187/0 |
| F2-T4 | DONE 2026-09-24 | Gateway route `building-api` (+ tests: 8 paths forwarded with bearer, 2 MB multipart body byte-identical, `/internal/**` never forwarded; gateway 31/0); OpenAPI 20 new paths + request schemas (`check-contracts.py` passed, Redocly lint valid); `scripts/smoke-building-application.sh` run live against an isolated full stack (compose project `bos-f2-smoke`, 4 services from jars, MinIO) — SMOKE PASSED |
| F2-T5 | DONE 2026-09-24 | `user_app/lib/features/building_application` (generated with `generate_feature.dart`, then replaced): 8 use cases, remote data source, list/form/detail screens, documents (camera/gallery/PDF), submit/resubmit, en+bn strings; Home tab entry. Core: `ApiClient` keeps the error body on 4xx (API `code` survives), `BuildingApplicationApiUrls`. Tests: validator (3), repository against a real local HTTP server (5), form controller (4), list screen widget en/bn/error (3); `flutter analyze` clean, `flutter test` 50/0 |

Deviations from the text above (design intent unchanged):
- Feature package is `buildingapplication`, not `application`: ArchUnit's `..application..` layer rule would
  otherwise treat the feature's presentation/infrastructure as application layer.
- One migration per task (V2 applications/notes/transitions; documents and buildings follow in V3/V4) instead of
  a single V2, so each task ships a forward-only migration it fully tests.
- Concurrency uses the row lock (`SELECT … FOR UPDATE`) inside each transition; `version` is incremented for later
  client-side `If-Match`. Concurrent start-review → one 200, rest 409 `INVALID_TRANSITION` (tested).
- The platform queue never lists `DRAFT` applications (applicant's private work); `status=DRAFT` → 400.
- Application numbers use the UTC year.
- T2: added `GET …/{id}/documents` (list metadata) for the Flutter screen. Upload runs inside the application row
  lock (limit race-safe); downloads send `Content-Disposition: attachment` + `nosniff`. AWS SDK v2 with the
  URL-connection HTTP client (Apache/Netty excluded). MinIO images come from quay.io — Docker Hub pulls were denied
  from this machine.
- T3: building-service now requires `AUTH_SERVICE_URL` and `SUBSCRIPTION_SERVICE_URL` (fail at startup if missing).
  Duplicate candidates are pre-filtered in SQL (same district, same contact phone, or coordinate bounding box;
  max 50 per kind) and matched in the domain; approved applications are represented by their building. Filler
  words and radius are config (`buildingos.duplicates.*`). The fee gateway treats any non-409 error response or
  unexpected body from subscription-service as 503 `DEPENDENCY_UNAVAILABLE`.
- T4: `verify-platform.sh` checks infrastructure only (it never starts services), so the draft → approve smoke is a
  separate `scripts/smoke-building-application.sh` against a running local gateway. It never invents the fee amount:
  an unconfigured `BUILDING_CREATION` fee stops it unless `SMOKE_SET_FEE=1`. `ApiError.code` in OpenAPI is now a
  documented string (the old 3-value enum was already contradicted by F5a codes).
- T5: the app has no local cache for applications (status changes server-side); customers do not download documents
  (list/add/remove only). Latitude/longitude are typed manually (no location picker yet). `pubspec.lock` moved only
  because the local Flutter SDK re-pinned test packages during `pub get`. Bangla strings need native review.
- Found, not fixed (pre-existing, platform-web): unknown gateway paths return 500 from the catch-all handler instead
  of 404.
