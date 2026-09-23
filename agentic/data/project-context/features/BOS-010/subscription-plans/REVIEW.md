# BOS-010 F5a — Code review and automated QA

Run `RUN-4ABC6B2BAC974B0E878CA39C9A27AE00` (scope revision 1). 2026-09-23. Revision: HEAD `be9ae2f`
+ uncommitted F5a working tree. Verdicts: review **READY**, QA **READY** (agent, not human approval).

## Code review

| Check | Result |
|---|---|
| Layering | Domain pure Java (catalog, freetier, plan, subscription, fee); one use case per action; repositories behind domain interfaces; JDBC adapters + `TransactionalUnitOfWork` in infrastructure; controllers call use cases only. Copied ArchUnit rules pass unchanged. |
| Changeability (D-27) | Free tier, plans, fee amount/required are data; `Feature`, `FeeCode`, `SubjectType`, `PaymentMethod` enums are the only code seams; `EntitlementResolver` is the swap point for another revenue model. |
| Authorization | Revenue-admin check (`SUPER_ADMIN`/`PLATFORM_ADMIN`/`SUBSCRIPTION_ADMIN`) in every platform use case, and in write controllers before body parsing (a non-admin never learns validation details). `/me/*` uses only the token `sub`. |
| Consistency | Mutations + audit row in one transaction; plan rows `FOR UPDATE` on edit/retire/attach; one-active-subscription enforced by a partial unique index (`ON CONFLICT DO NOTHING`). |
| Fail closed | Unconfigured fee → 409 `FEE_NOT_CONFIGURED`, never free. |
| Shared changes | platform-web CORS now allows `PUT` (needed by plan/free-tier/fee edits); gateway pass-through route for the revenue paths. |

Findings:
1. Fixed: `SubscriptionStarter` took a nullable "plan not allowed" error for admin grants; now an
   overload plus a `Supplier`. `planId` validation moved onto `SubscribeRequest`.
2. Added beyond spec (needed for RV-06 self-subscribe): `GET /api/v1/me/plans` lists active
   self-service plans.
3. Implementation detail vs TECH-SPEC: `billing_cycles` stored as `jsonb` (not `varchar[]`).
4. Accepted risk (TECH-SPEC §7): user ids are not validated against account-service; an admin can
   grant a plan to any UUID. No enforcement of entitlements in other services yet.
5. Operator action: existing local `infra/docker/.env` files need `SUBSCRIPTION_DB_PASSWORD`;
   `docker compose` refuses to start without it (same `:?` rule as other passwords).

## Automated QA

| Requirement | Evidence | Result |
|---|---|---|
| RV-01 service, DB, gateway | Module builds; disposable Postgres: init scripts create 3 DBs, 9-way role isolation (only own DB allowed), Flyway V1+V2 migrate twice; `GatewayRoutingTest` +10 (paths forwarded unchanged with bearer, PUT/POST, 401 without token) | PASS |
| RV-02/03 catalog, free tier | `EntitlementsTest` (types, unknown keys, merge); `freeTierIsDataAndEditsApplyImmediately` | PASS |
| RV-04/05 plans, edits immediate, retire | `planCrudValidationAndAudit`, `planEditsApplyImmediatelyToSubscribersAndRetireIsOneWay` | PASS |
| RV-05..07 per-user subscriptions | `entitlementsArePerUserOnly` (owner vs tenant), `selfSubscribeRules`, `concurrentSubscribeHasExactlyOneWinner` (6 parallel → one 201) | PASS |
| RV-08/09 creation fee | `FeeRulesTest`; `buildingCreationFeeFlow` (not configured 409 → UNPAID → partial → currency mismatch → SETTLED → NOT_REQUIRED) | PASS |
| RV-10 roles | `platformEndpointsNeedTokenAndRevenueAdminRole` (401, 403, 403 before 400, three admin roles 200, SUPPORT_AGENT 403) | PASS |
| RV-11 audit | audit row counts asserted per action | PASS |
| Build | `mvn -B -f backend/pom.xml verify`: 147 tests, 0 failures; `check-contracts.py` PASSED (OpenAPI paths added); `compose config` valid with the new variable | PASS |

Live smoke (disposable Postgres; account-service `local` + subscription-service + gateway on
18081/18083/18080): SUPER_ADMIN logged in via OTP `000000`; no token 401; plain user
free tier = maintenance only; plain user → platform 403; create plan 201; `/me/plans` lists it;
self-subscribe 201; subscriber sees rent/max_units + maintenance; another user still maintenance
only; fee status 409 until configured; set 5000 BDT; manual payment 201; status SETTLED; logs held
no tokens. Processes and container removed afterwards.

Not covered: `verify-platform.sh` against the operator's real local volume (not run: it would
create `subscription_app` with a password the operator does not know; operator runs it after adding
`SUBSCRIPTION_DB_PASSWORD`); UI; human QA.
