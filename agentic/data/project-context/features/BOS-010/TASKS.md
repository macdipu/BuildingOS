# BOS-010 implementation sequence (identity & platform roles feature only)

Classification: provisional EPIC at the work-item level (see FEATURE.md); this
document covers only its first feature slice — identity, platform roles, phone+OTP
login. No route in this run has a separate PLANNING stage (route:
INTAKE → CONTEXT → REQUIREMENTS → TECHNICAL → IMPLEMENTATION → REVIEW → QA → RELEASE →
COMPLETED); task breakdown for this slice is recorded here as part of TECHNICAL.

Status: TASK-001 implemented and tested (backend only). TASK-002/003 not started.

| Task | Outcome | Status |
|---|---|---|
| [TASK-001](tasks/TASK-001.md) | Global user identity, platform roles, seed SUPER_ADMIN, phone+OTP login, local token issuance (backend) | DONE (backend); see TECH-SPEC.md |
| TASK-002 | Flutter (`user_app`) integration: wire the new OTP endpoints into the existing GetX auth feature (repository, controller, session storage, error/loading states) | NOT STARTED |
| TASK-003 | Production token-issuance path: replace/complement the local RSA issuer with an operator-configured external issuer story, and a real OTP/SMS provider adapter behind `OtpCodeVerifier` | NOT STARTED |

Sequence: TASK-001 (done) → TASK-002 and TASK-003 can proceed independently of each
other once picked up.

Later features under this EPIC (not yet intake'd, see FEATURE.md/DECISIONS.md):
building application & lifecycle; units & ownership; subscription & entitlements;
back-office console (needs D-09/D-10).
