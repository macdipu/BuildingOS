# BOS-002 decision register

Status: PARTIALLY RESOLVED. The user selected `000000` for development OTP and required
a replaceable vendor layer; the development flow needs no vendor account. Phone-only login is the current stated interpretation;
Google is deferred pending optional clarification. D-02 is now RESOLVED.

| ID | Decision needed | Known source constraint | Needed before |
|---|---|---|---|
| D-01 / Q-01 | RESOLVED development choice: DevelopmentOtpProvider accepts `000000` for active challenges in local/test mode only, behind replaceable app/backend interfaces. No SMS/vendor account now. Future live-SMS provider, cost policy and numeric abuse-control/session limits remain technical-design inputs. | Latest user direction; 6-digit OTP; abuse controls; +880; Google deferred for now | See OTP-PROVIDER.md; no billing or live-SMS activation implied |
| D-02 / Q-02 | RESOLVED: first SUPER_ADMIN is provisioned by a deploy-time seed script/config, not through the app login/sign-up flow. Seed phone number: `01306999005`. This seeded account authenticates the same way as every other user — phone + OTP (dev code `000000` locally) — no password field exists anywhere in the design; no admin panel exists (only `user_app` mobile + `backend`). Building admins/membership: an admin invites a user by phone number; the invited user activates membership by completing normal phone-OTP verification. Ownership assignment is a separate action from membership grant — assigning a unit owner does not by itself create/activate building membership. | SUPER_ADMIN creates buildings/manages building admins; roles vary by building; invitations belong to Identity; user 2026-09-22 direction: seed script + phone-invite, no admin panel, no password | Bootstrap/membership design and building onboarding — see REQUIREMENTS.md ID-05, BL-01, BL-02 |
| D-03 | Phone identity recovery/reassignment, unverified or colliding identifiers; future Google linking deferred; session duration/reuse/revocation and device behavior | Identity mapping, short-lived access, rotated refresh and session security required | Login contract and security tests |
| D-04 | Exact permission/object matrix for membership, unit edits, owner assignment/transfer and history access; delegation and revocation behavior | Existing role statements and owner-unit restriction are binding but not exhaustive | Authorization design and protected business endpoints |
| D-05 | Ownership date granularity/timezone and interval boundaries; back/future dating; partial/co-owner transfer; share precision; corrections; inactive-owner visibility | History never overwritten; effective dates; active shares <=100%; lease unchanged | Ownership schema/use cases and acceptance examples |
| D-06 | Required building fields; unit numbering normalization and area units/precision; supported settings; optional Join Building scope | Name/address shown in selector; unit number unique within building; positive area; enum types specified | Building/unit API validation and onboarding UI |

D-03–D-06 will be resolved against concrete technical/UX proposals after the remaining D-02,
without silently filling missing business rules. Infrastructure mechanics (indexes,
locking, DTOs, migration ordering, outbox polling) can be proposed by engineering;
provider, permission and data-policy choices remain explicit stakeholder decisions.

The future real-provider cost research and development isolation are recorded in OTP-PROVIDER.md; no live usage or throughput promise is implied. Do not put
credentials in this document. Credential provisioning can follow provider selection.
Q-03 rental invoice ownership and Q-05 production operations stay in their later
milestones; Q-04's relevant permission/ownership subset is represented by D-04/D-05.
