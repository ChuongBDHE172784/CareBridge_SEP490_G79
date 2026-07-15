# Full Regression Waiver — Sprint 1

**Decision date:** 2026-07-15  
**Decision:** APPROVED  
**Approver:** Project owner  
**Scope:** TV1/Sprint 1 closure

## Waived baseline

The full backend regression baseline contains 29 failures/errors outside the TV1 and UC242 Sprint 1 scope:

| Owner | Count | Areas |
|---|---:|---|
| TV2 | 18 | Care-group integration, journey, reminders/today tasks |
| TV3 | 10 | Content mapper, unpublish content, moderation |
| TV5 | 1 | AI triage |

These failures are not defects in the TV1 authentication, shared foundation, notification, privacy, exercise, or posture gate. The TV1 targeted suite is green (358 tests, 0 failures, 0 errors, 1 known skipped fixture).

## Conditions

1. The waiver does not cover UC242 targeted failures, critical security failures, or object-scoping regressions.
2. Each owning team must create remediation work and rerun full regression before the release candidate or Sprint 2 exit.
3. The waiver is a Sprint 1 delivery decision, not a production-release approval.

## Evidence

- `_bmad-output/test-artifacts/traceability/gate-decision.json`
- `_bmad-output/implementation-artifacts/tests/test-summary.md`
- TV1 targeted Maven run: 358 tests, 0 failures, 0 errors, 1 skipped.
