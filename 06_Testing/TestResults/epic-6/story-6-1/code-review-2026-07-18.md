# Story 6.1 Code Review — 2026-07-18

Workflow: `bmad-code-review` full review. Layers: Blind Hunter, Edge Case Hunter, Acceptance Auditor. All layers completed successfully.

## Triage summary

- Decision needed: 0 (D1 resolved by project approver)
- Patch: 21
- Deferred as pre-existing/outside Story 6.1: 1
- Dismissed after spec/code validation: 4

## Resolved decisions

- [x] **D1 — Define server-side bounds for client supplied `effectiveAt`.** Approved option 1: allow backdated effective times, reject values more than five minutes ahead of the server clock, and retain `recordedAt` as the authoritative ingestion timestamp. Converted to P21. (`JourneyTransitionServiceImpl.java:338`)

## Patch findings

- [x] **P1 — Preserve first-journey onboarding for an unassigned user.** Null-role users are promoted to MOTHER transactionally; other roles remain forbidden.
- [x] **P2 — Make Journey audit actions eligible and verify audit atomicity.** Journey audit actions are eligible and PostgreSQL integration tests assert persisted create/update audit records.
- [x] **P3 — Supply required creation context for stage-only mobile onboarding.** PRE_PREGNANCY creation sends source, confidence, reason, and effective time.
- [x] **P4 — Persist the specified change JSON shape.** Transition changes use `{previous,new}` and are asserted exactly.
- [x] **P5 — Implement end-to-end paginated history.** Backend returns bounded page metadata; mobile consumes every page while retaining legacy-list compatibility.
- [x] **P6 — Preserve legacy BABY_CARE readability.** Dashboard falls back to the latest active legacy BABY_CARE row.
- [x] **P7 — Enforce append-only transition persistence.** Repository mutation APIs are hidden, entity callbacks reject mutation, and `V20260718091000` adds a PostgreSQL UPDATE/DELETE guard.
- [x] **P8 — Add the claimed POST/PUT authorization coverage.** Guest and Expert POST/PUT/GET paths are covered by controller tests.
- [x] **P9 — Handle empty and notes-only updates accurately.** No-op updates return `JOURNEY-020`; notes-only updates create `DETAILS_CHANGED`.
- [x] **P10 — Record provenance mutations in immutable history.** Date source/confidence changes are included in the allow-listed history payload.
- [x] **P11 — Reject unsupported status values.** Unknown values return `JOURNEY-021`.
- [x] **P12 — Treat POSTPARTUM as a canonical maternal lifecycle on mobile.** POSTPARTUM renders maternal state/history and cannot be shadowed by a settled pregnancy cache.
- [x] **P13 — Align version/provenance DTOs across backend and mobile.** Dashboard/create/update cache paths retain version and provenance.
- [x] **P14 — Reconcile nullable LMP symmetrically.** Null comparisons are symmetric and EDD-only updates clear stale cached LMP values.
- [x] **P15 — Distinguish history load failure from empty history.** Previously loaded history is retained and a visible retry action is rendered.
- [x] **P16 — Replace source-substring regression checks with behavior tests.** Story mobile tests now execute model/reconciler/widget behavior.
- [x] **P17 — Preserve adjusted EDD for non-28-day cycles.** An explicit adjusted EDD is authoritative when LMP is also supplied.
- [x] **P18 — Clear stale non-pending cache on authoritative `NO_JOURNEY`.** Only a pending mutation may temporarily override authoritative absence.
- [x] **P19 — Prevent cross-account async cache writes.** Requests capture user identity and scoped writes are rejected after an account switch.
- [x] **P20 — Route dashboard transport failures to retry, not onboarding.** Auth landing retains the user on a warm error/retry state instead of stage selection.
- [x] **P21 — Enforce the approved `effectiveAt` future bound.** Backdating is allowed; values beyond server time plus five minutes return `JOURNEY-019`.

## Deferred

- [x] **W1 — Cross-field LMP/EDD clinical consistency validation.** The legacy create/update domain already allowed inconsistent date pairs and Story 6.1 does not define the clinical tolerance rule. Track separately with an approved business rule. (`JourneyTransitionPolicy.java:20`)

## Dismissed after validation

- Backend `PREGNANCY -> POSTPARTUM` being disabled is intentional until Story 6.3 per TDS §6.3.
- Returning 403 for a non-owner and 404 for a missing journey is the explicitly approved `JOURNEY-011`/`JOURNEY-010` contract.
- Sequential stale-client version matching is not part of Story 6.1; the approved concurrency rule is JPA optimistic locking for overlapping writes.
- Mobile provenance defaults are currently confined to self-report flows; no clinician/import mobile caller exists in this Story.

## Review gate

The review is **clean for Story 6.1**. All 21 approved patch findings are resolved; W1 remains explicitly deferred outside the Story.

Verification evidence after patching:

- Backend Story/contract suite: **45/45 passed**, including PostgreSQL/Flyway, append-only enforcement, authorization, audit, pagination, and lifecycle edge cases.
- Changed service/policy coverage: **90.83% line** and **60.53% branch**; see `backend-coverage-2026-07-18.md`.
- Mobile Story behavior suite: **15/15 passed** with no analyzer issues in changed Story files.
- Full mobile regression: **187/187 passed** after the final cache reconciliation patch.
- Full-repository Flutter analysis still reports 19 pre-existing diagnostics outside the Story files; targeted analysis of all changed Story files reports no issues.
- Full backend baseline remains red only in waived non-Journey areas; no Journey failure is waived.
- Final graph review reported risk `0.60`; its structural test-gap list does not link the newly untracked Story test classes, so executable test results above are the authoritative coverage evidence.
