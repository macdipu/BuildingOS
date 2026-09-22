# BOS-001 continuation review

Date: 2026-09-22. Baseline: `463db1a` plus the continuation diff.
Run: `RUN-85F84025CDE9409DB09779174AC7CB86`.

## Scope and corrected findings

- PF-02: Both database bootstrap scripts embedded environment passwords directly in
  SQL literals. An apostrophe broke initialization. They now use psql environment
  variables and SQL literal quoting. A disposable isolated PostgreSQL instance runs
  the actual init scripts with apostrophe/backslash passwords and checks both roles.
- PF-03: The named Kafka volume was not explicitly selected as the broker log
  directory, and the old check deleted its topic before restart. Set KAFKA_LOG_DIRS
  to the mounted directory; compare the exact payload before and after broker
  container recreation. This now verifies volume persistence directly.
- PF-08: The runbook stopped at infrastructure/image startup. It now documents
  identity/building/gateway JAR execution, per-process configuration, health and
  protected routes, and shutdown. No development issuer is introduced.
- Repaired the task acceptance and local runbook links. Reconciled stale design
  status and documented the delivered topology: Compose infrastructure plus
  independently launched application JARs, rather than all services in Compose.

## Review coverage

Inspected shared JWT issuer/audience/RS256 enforcement, fail-closed URL configuration,
actuator scope restriction, safe correlation identifiers, exact gateway routes and
sanitized downstream errors, service configuration, migration ownership, contracts,
CI/runbook commands, and the continuation diff. Inspected representative security,
metadata, correlation and downstream-failure test assertions.
No unresolved material defect was found in this bounded review.

## Verification

[Recorded command results](reconciliation/checks.json): Maven reactor BUILD SUCCESS;
37 tests, zero failures/errors/skips; contract and Compose checks exit 0; platform
verification passes fresh quoted-password bootstrap, migration/rerun, role isolation,
exact Kafka round trip and record persistence after container recreation.
The initial sandbox runs failed on Docker/socket access; explicitly approved reruns
passed. Git whitespace check passed. The final platform script, including anonymous-volume cleanup and exit-on-signal
handling, was rerun successfully during governed QA.

## Limits and next action

Flutter and image-build results are reused from the prior acceptance record; those
components were not modified. Gateway routing tests use stub downstreams and direct
service tests validate security independently; no new full-stack login test is claimed.
The runbook requires an operator-supplied issuer/JWKS for manual authenticated calls.
Phone/Google login and membership decisions remain BOS-002, with Q-01/Q-02 unresolved.
No production readiness or human release approval is implied. Governed review and QA are complete. The user subsequently approved local milestone closure; see RELEASE-READINESS.md.
This review remains evidence for that decision.
