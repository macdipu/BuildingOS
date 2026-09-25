# F6-T4b — Support-session action relay (BOC-07, TECH-SPEC mutation pattern)

## Category
BE

## Objective
Actions taken "under" an active `SupportSession` are scope-checked by `back-office-service`
(F6-T4 `checksupportscope`) and then relayed with the acting user's own bearer token to the
owning service, whose own authorization stays the real boundary.

## Scope (D-36e mapping; existing endpoints only)
- `SUPPORT_EDIT_UNIT` → building-service `…/buildings/{id}/units/{unitId}`
- `SUPPORT_MANAGE_MEMBERSHIP_INVITE` → `…/buildings/{id}/invitations` (+ revoke)
- `SUPPORT_REVOKE_MEMBERSHIP` → `…/buildings/{id}/members/{membershipId}/revoke`
- `SUPPORT_TRANSFER_OWNERSHIP` (high-risk) → `…/units/{unitId}/ownership-transfers`
- `SUPPORT_REMOVE_BUILDING_ADMIN` (high-risk) → `…/members/{membershipId}/revoke` on a
  BUILDING_ADMIN membership
- View scopes → the matching existing read endpoints.
- Not actionable yet (no owning endpoint): `SUPPORT_REVERSE_PAYMENT`,
  `SUPPORT_EXPORT_FINANCIAL_UNRESTRICTED`, `SUPPORT_VIEW_PAYMENTS` beyond creation-fee payments.

## Open before planning
Whether owning services accept a platform support user on these building-scoped endpoints
(today they authorize building members) — needs a check and possibly a technical decision.

## Status
PLANNED (after F6-T4).
