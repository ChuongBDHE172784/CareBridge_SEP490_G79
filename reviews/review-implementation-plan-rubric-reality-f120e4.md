# Final implementation-plan review — rubric/reality lens

## Review identity

- Artifact: `_bmad-output/planning-artifacts/architecture/architecture-CareBridge_SEP490_G79-2026-07-29/IMPLEMENTATION-PLAN.md`
- Reviewed plan SHA-256: `F120E4E899F7AC1F58D2E864C0CACBD28599FFF1CDB610EE6715C14D7993A8BF`
- Lens: complete stale-plan replacement; buildable phase dependencies; complete architecture/operations/rollout landing; no new table or invented requirement; approval status and authorization boundary.
- Verdict: **CHANGES REQUIRED**
- Severity counts: **Critical 0 / High 0 / Medium 8 / Low 2**
- Approval rule: this review does **not** PASS because PASS requires zero Critical, High, and Medium findings.

## Frozen-input verification

The review started only after all four supplied hashes matched. The same four hashes were recomputed immediately before writing this report and again after writing it.

| Input | Expected | Before review | Immediately pre-write | Post-write |
| --- | --- | --- | --- | --- |
| `IMPLEMENTATION-PLAN.md` | `F120E4E899F7AC1F58D2E864C0CACBD28599FFF1CDB610EE6715C14D7993A8BF` | match | match | match |
| `ARCHITECTURE-SPINE.md` | `25D317D43B7D11A117EBBC85851556CAF17F77B71F44085255BD352708BCADA1` | match | match | match |
| `CHECKLIST-CADENCE-DESIGN.md` | `4356B3AFBFD9170E4A4E4F680DD3662E7EDB77B0AD8AD19A962C8279B61A10DA` | match | match | match |
| `08_References/Checklist giai đoạn đang mai thai.md` | `D68EDC9F3D2D595876F8B1F9D3332E6FCFA55986535B52D8AE01BD25FAAFE133` | match | match | match |

The frozen bytes were 32,493; 65,580; 83,505; and 5,151 respectively. No normative input or plan was edited by this review.

## Rubric result

| Rubric dimension | Result | Evidence |
| --- | --- | --- |
| Replaces the stale July plan | **FAIL** | The plan explicitly supersedes the July plan and forbids implementing it, but it does not carry several adopted base-spine invariants into any deliverable or exit gate. A replacement must be self-contained enough to schedule all required work; precedence language cannot supply missing work items. |
| Phase dependencies are buildable | **PASS** | P0→P1→P2, parallel P3A/P3B, then P4→P5→P6→P7→P8 is acyclic and buildable. P4 can use the P3B contract fixture while clinical-copy activation remains gated on P3B/P7/P8. P9 is correctly deferred. |
| Architecture invariants land | **FAIL** | Cadence-specific AD-19 through AD-34 coverage is generally strong, but AD-6, AD-7, AD-11, AD-12, AD-13, AD-14, and AD-25 are incompletely placed. |
| Operations and rollout land | **PARTIAL** | Repair, cursoring, budgets, alerts, shadow, client floor, canary, staged activation, and roll-forward rollback are placed well. Audit lifecycle and compatibility-route operations remain incomplete. |
| No new table | **PASS** | The plan repeatedly forbids a cadence, dating-history, reconciliation, quarantine, or Family-access table and names existing rows/storage to reuse. P1 also gates unchanged table count. |
| No invented requirement | **PASS** | Source counts, Plan boundaries, targetless V2, dating, Family epochs, repair budgets, rollout gates, and the deferred target contract migration all trace to the frozen inputs. The plan correctly refuses to invent operational thresholds. |
| Status/no code authorization | **PASS** | Frontmatter and body remain `ready-for-approval`; P0 is BLOCKED pending explicit approval; the plan states that updating it authorizes no production code or database change. |

## Findings

1. **[MEDIUM] The replacement omits AD-11’s user-facing cutover.** P6B says to render current cadence in Today, but no phase replaces both old direct entry points, removes `PreparationChecklistScreen`, or performs the required router/deep-link sweep. That leaves a duplicate checklist surface and dead-route risk even if every cadence test passes. Add an owned P6 deliverable and exit evidence for route replacement, screen removal, navigation/deep-link inventory, and regression coverage.

2. **[MEDIUM] AD-6’s explicit origin-versus-kind contract has no implementation or verification owner.** The plan never requires persisted/projected `SYSTEM_TEMPLATE|USER_CREATED` origin to remain separate from technical `REMINDER|CARE_TASK|CHECKLIST` kind; the terms `origin` and `taskKind` do not occur. V2 key text mentioning `USER_CREATED` is not a substitute for the domain field and cross-domain projection contract. Place storage/mapping/API/client work and tests in P1/P5/P6.

3. **[MEDIUM] AD-14’s deterministic Today contract is only partially represented.** The plan covers “closed cadence is History-only” and “no cadence OVERDUE,” but omits the valid-IANA `X-User-Timezone`/`Asia/Ho_Chi_Minh` fallback, returned `asOf` and `zoneId`, seven-day horizon, preservation of legacy point-due `OVERDUE`, legacy-only `UNSCHEDULED`, and `CANCELLED` exclusion. These are coexistence rules needed during the V1/V2 union, not optional UI detail. Add P5 implementation and API/client contract evidence.

4. **[MEDIUM] AD-25’s schedule-zone authority is underspecified.** P4 uses a persisted schedule zone but does not establish the server-configured `Asia/Ho_Chi_Minh` authority, prohibit template/caller override, reject active-context zone change until an append-only zone timeline exists, or require `scheduleZoneId` to remain distinct from display `zoneId`. Without those tasks, two valid implementations can produce incompatible identities. Add resolver/configuration, write-gate, response, and key-stability tests.

5. **[MEDIUM] AD-7’s `/api/v1/reminders/today` compatibility obligation is absent.** P5 correctly keeps `/api/v1/tasks/today` as a projection, but never schedules preservation of `/api/v1/reminders/today` or verifies stable task references plus origin/cadence/canonical-context metadata across the cross-domain adapter. The stale plan cannot be discarded safely until this compatibility surface has an owner and regression gate.

6. **[MEDIUM] AD-13/AD-29’s generic task-kind compatibility action route is not placed.** P0 and P5 enumerate scoped canonical checklist action routes and the user-created surface, while the normative contract also requires the generic task-kind route to apply the same header-first, scoped-authorization, indistinguishable-not-found, and authorized-426 ordering. “Every route” in the final evidence table is not an implementation deliverable. Name the route/adapter in P0/P5 and add its full matrix to the exit gate.

7. **[MEDIUM] The automated system-actor contract from the spine is missing.** No phase requires automated distribution to persist `actorType=SYSTEM`, `actorUserId=null`, service `CHECKLIST_DISTRIBUTOR`, and UUID correlation independently from the distribution key. P7’s generic audit-atomicity statement cannot prevent an implementation from conflating actor identity and idempotency identity. Add the actor mapping to P4 and fault-injection/audit assertions to P7.

8. **[MEDIUM] The audit evidence lifecycle is incomplete.** P7 covers least privilege, meta-audit, break-glass, and redaction, but does not place the adopted seven-year project baseline, retention/purge verification, encrypted-backup handling, or the stricter identifier-free ordinary-log/metric rule. These are explicit AD-12/consistency-convention obligations. Add named operational evidence, environment, owner, purge rehearsal, and log/metric scans before P7 can pass.

9. **[LOW] P4 does not state the equal-instant close-cause precedence used by the code.** The final design’s verification matrix is referenced later, so the case may be tested, but the implementing phase never tells the closure service to encode `CADENCE_PERIOD_CLOSED > CADENCE_SCOPE_EXIT > ACCESS_REVOKED > DATING_CORRECTED`. Put the ordering in P4 rather than relying on P7 to discover a wrong implementation.

10. **[LOW] As-of version selection is tested by reference but not explicitly built.** P4 says “version selector” and P3B covers expiry/revocation, yet neither deliverable states that repair may select a currently archived/replaced version before its administrative cutoff while independently enforcing the copy-revocation cutoff. Add the immutable approval/enable interval and distinct archive/revocation selection rules to P4 so the architecture’s repair semantics are implementable before P7.

## What is already solid

- The plan’s frontmatter pins the correct architecture, cadence-design, and source hashes and explicitly invalidates stale bytes.
- The no-new-table constraint is unambiguous and consistently backed by same-table extensions and existing operational/audit storage.
- Pregnancy source handling is faithful: 62 leaves, 26 COMMON, 36 WEEKLY, all eight `/ngày` leaves remain WEEKLY, Plan 2 begins at week 21, Plan 8 closes on exit, and no DAILY pregnancy root is invented.
- Targetless V2, V1 compatibility, parent/child contract isolation, Family epoch ownership, Mother-share exclusion, Journey dating authority, catch-up repair, close-before-authorize, source attestation, and staged roll-forward rollout all have plausible phase homes and evidence gates.
- The phase DAG preserves server-first contract cutover, supported-client gating, shadow operation, canary activation, and deferred physical target removal.

## Required disposition

Revise the plan at a new hash to place findings 1–8 into concrete phases and exit evidence, and address findings 9–10 while touching those phases. Re-run an independent frozen-hash review. This report does not authorize P0, production code, database changes, or cadence activation.
