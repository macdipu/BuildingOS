# BOS-010 F6 — Back-office console

Status: REQUIREMENTS in progress.
Run: `RUN-8AAD6211653349D5BB3702342862F437` (new_feature, NO_REPLAN).
Source: BRD §149.1-149.2, §149.11-149.12, §149.17-149.19; parent
[REQUIREMENTS.md](../REQUIREMENTS.md) BO-01, BO-02, ON-03; [DECISIONS.md](../DECISIONS.md)
D-09 (resolved: separate web stack), D-10 (resolved: fine-grained scopes,
SUPER_ADMIN-only elevated approval).

A distinct back-office web application (`buildingos_backoffice_web`, separate tech stack
per D-09) giving `SUPER_ADMIN`/`PLATFORM_ADMIN`/`ONBOARDING_AGENT`/`SUPPORT_AGENT`/
`SUBSCRIPTION_ADMIN` a UI over capability that mostly already exists as backend API
(building-application review, building lifecycle, subscription plans/fees) plus new
capability this feature must build (platform user management, assisted-onboarding
sessions, support/controlled-impersonation sessions, system/audit views).

See [REQUIREMENTS.md](REQUIREMENTS.md) for the bounded scope and
[context-result.json](context-result.json) for the confirmed reusable-API-vs-gap split.
