# Independent Healthcare, Data-Integrity, and Privacy Review

## Review identity

- Artifact reviewed: `_bmad-output/planning-artifacts/architecture/architecture-CareBridge_SEP490_G79-2026-07-29/IMPLEMENTATION-PLAN.md`
- Review discipline: adversarial healthcare/data-integrity/privacy review
- Review date: 2026-08-10 (Asia/Saigon)
- Independence: reviewer did not author or modify the plan, spine, design, or source
- Scope boundary: this verdict approves the implementation plan as an execution plan only. It does **not** approve production code, database migration, rollout, or cadence activation.

## Frozen review boundary

All four inputs existed and matched the supplied SHA-256 values before analysis.

| Input | Bytes | Expected SHA-256 | Before-review SHA-256 | After-review SHA-256 | Result |
| --- | ---: | --- | --- | --- | --- |
| `IMPLEMENTATION-PLAN.md` | 32,493 | `F120E4E899F7AC1F58D2E864C0CACBD28599FFF1CDB610EE6715C14D7993A8BF` | `F120E4E899F7AC1F58D2E864C0CACBD28599FFF1CDB610EE6715C14D7993A8BF` | `F120E4E899F7AC1F58D2E864C0CACBD28599FFF1CDB610EE6715C14D7993A8BF` | MATCH |
| `ARCHITECTURE-SPINE.md` | 65,580 | `25D317D43B7D11A117EBBC85851556CAF17F77B71F44085255BD352708BCADA1` | `25D317D43B7D11A117EBBC85851556CAF17F77B71F44085255BD352708BCADA1` | `25D317D43B7D11A117EBBC85851556CAF17F77B71F44085255BD352708BCADA1` | MATCH |
| `CHECKLIST-CADENCE-DESIGN.md` | 83,505 | `4356B3AFBFD9170E4A4E4F680DD3662E7EDB77B0AD8AD19A962C8279B61A10DA` | `4356B3AFBFD9170E4A4E4F680DD3662E7EDB77B0AD8AD19A962C8279B61A10DA` | `4356B3AFBFD9170E4A4E4F680DD3662E7EDB77B0AD8AD19A962C8279B61A10DA` | MATCH |
| `08_References/Checklist giai đoạn đang mai thai.md` | 5,151 | `D68EDC9F3D2D595876F8B1F9D3332E6FCFA55986535B52D8AE01BD25FAAFE133` | `D68EDC9F3D2D595876F8B1F9D3332E6FCFA55986535B52D8AE01BD25FAAFE133` | `D68EDC9F3D2D595876F8B1F9D3332E6FCFA55986535B52D8AE01BD25FAAFE133` | MATCH |

The code-review knowledge graph was queried first and returned no nodes for the planning artifact, so the review used exact-file document extraction and line-bounded reads. The source was also checked independently: 124 UTF-8 lines, 79 checkbox rows, 17 structural/conditional headings, 62 completable leaves, and eight `/ngày` wording rows. This agrees with the normative 62 = 26 COMMON + 36 WEEKLY contract; the structural checkboxes are not tasks.

## Verdict

**PASS — 0 Critical, 0 High, 0 Medium.**

| Severity | Count |
| --- | ---: |
| Critical | 0 |
| High | 0 |
| Medium | 0 |
| Low | 12 |
| Informational | 0 |

The plan is safe to approve for Phase 0. It is not release evidence. The plan explicitly keeps P0 blocked pending approval, keeps distribution disabled through the implementation phases, requires P7 to execute every design §9 row, and allows P8 only when every design §10 blocker is green. Missing, skipped, environment-blocked, stale-hash, or failed evidence cannot be treated as PASS.

## Final design §10 activation-gate trace

| Design §10 blocker | Plan coverage attacked | Result |
| --- | --- | --- |
| Pinned source and 62-leaf locator/text-hash bijection | Frontmatter and P0 freeze all hashes; P3B pins byte count, line count, normalizer, 62/26/36/8 split, locator/hash manifest, and mismatch rejection; P7 reruns §9/§10 evidence. | PASS, fail closed |
| Complete, valid, unexpired, unrevoked manifest-bound attestation | P3B requires the policy version, allowlisted authority, qualification evidence, citation/authorship/translation fields, two named approvals, validity/expiry/revocation, source/leaf/manifest binding; Draft/non-distributing remains until valid. | PASS, fail closed |
| V1/V2 Journey header/request matrix | P0 freezes OpenAPI and red tests; P3A implements the final XOR/V1 matrix on existing POST/PUT routes; P3A exit requires PostgreSQL/API coverage; P7 rechecks the blocker. | PASS, fail closed |
| Atomic rejection of non-pregnancy dating and legacy raw-date leakage | P2 refuses non-pregnancy raw shapes as authority; P3A rejects date-bearing PRE_PREGNANCY/POSTPARTUM requests atomically; the verification matrix retains the complete normative §9 cases. | PASS, fail closed |
| Future LMP and lifecycle exit closure | P3A fails closed for a future canonical LMP and closes all active pregnancy occurrences at authoritative exit; PostgreSQL boundary and lifecycle tests are required before distribution. | PASS, fail closed |
| Source mapping, Plan, context, and recipient validation | P3B creates eight COMMON and eight WEEKLY JOURNEY roots grouped by Plan, proves Plan 2 starts at week 21 and pregnancy has zero DAILY roots, and keeps all roots Draft/non-distributing. | PASS, fail closed |
| Family membership/timeline/baseline and Mother-share exclusion | P2 requires unique accepted membership, current VIEW, empty-timeline-only atomic baseline/event/audit/epoch/stamps, no adoption into an existing epoch, quarantine on ambiguity, and zero Mother-owned candidates. P5 uniformly excludes Mother rows and prior epochs. | PASS, fail closed |
| Dating/VIEW intervals, cursor order, repair, close-before-authorize, History, V2 identity | P4 specifies one `asOf`, non-null tokens, deterministic round-robin paging, durable complete-sweep watermark, failure-forward retry, logical close transaction, app-independent catch-up, no retroactive action, and required alerts. | PASS, fail closed |
| Contract isolation, Family re-grant/V1 POST guard, STANDARD_V2 identity and permissions | P5 isolates V1/V2 parents, enforces current stamped epoch, first-epoch-only V1 POST, non-disclosing 426 after authorization, epoch-qualified V2, clientTaskId conflict, complete POST/GET/DELETE/from-template negotiation, and USER_CREATED/SYSTEM_TEMPLATE permission differences. | PASS, fail closed |
| Targetless V2 across storage/API/Admin/mobile | P1 makes target nullable with V2 null checks; P5 uses distinct DTOs and rejects target; P6 removes target inputs, output, filters, badges, fixtures, and network fields; P9 defers physical removal until zero consumers. | PASS, fail closed |
| Audit/privacy/meta-audit, catch-up UX, client floor, alerts | P7 requires atomic audit/state, controlled reasons, data minimization, least privilege, bounded purpose-bound access/export, fail-closed primary meta-audit, secondary-sink break-glass, redaction, client-floor enforcement, dashboards/alerts, and every §10 evidence artifact before P8. | PASS, fail closed |

No design §10 blocker was downgraded to a warning, deferred past activation, or made optional by a unit-only test. The plan explicitly states that the design's exhaustive §9 case list remains normative and that PostgreSQL-marked rows require the real Flyway chain; H2, unit-only, a skipped container, or an unavailable sink is invalid evidence.

## Targeted healthcare, integrity, and privacy attack results

### Dating authority and pregnancy epochs

The plan preserves one server-owned LMP-or-EDD authority, a monotonic revision, a database/server commit-time effective instant, immutable transition reconstruction, and a fresh `PREGNANCY_EPOCH_STARTED` boundary. It does not allow caller date, display timezone, client effective time, or generic row version to become occurrence identity. Corrections are prospective, close old current work as `DATING_CORRECTED`, derive `SUPERSEDED` rather than false `MISSED`, and never copy completion or materialize wholly pre-authority work. New pregnancy epochs cannot carry an old anchor.

### Recommendation-only boundary and source/copy governance

The source contains clinical wording, amounts, conditions, and `/ngày` phrases. The plan correctly freezes these as immutable advisory recommendation copy. It neither turns amounts into dosage logic nor evaluates Rh, GBS, provider direction, requiredness, applicability, medical due rules, or health evidence. Conditional ancestor wording remains visible provenance, structural headings do not become tasks, and one source leaf remains one user-completable task. Human sign-off governs wording/source integrity only; it cannot silently create clinical automation.

### History and non-retroactivity

The plan closes cadence occurrences before action authorization, commits the controlled close reason/timestamp/audit atomically, exposes closed rows only in History, and admits only COMPLETE/REOPEN on open rows. It rejects a new SKIP action and cadence OVERDUE. Catch-up rows are born closed with `wasActionable=false`, are neutrally presented, and are excluded from adherence, escalation, and completion-rate measures. Date browsing and app opens cannot create or mutate historical work.

### Family ownership, baseline, revoke/re-grant, and V1 POST

The retained-Family path is defined by persisted `recipient_role=FAMILY` plus the matching recipient. Only a unique accepted member with current VIEW and an empty timeline can receive the one atomic legacy baseline. Event, database-time effective instant, audit, positive epoch, and every eligible parent stamp commit together. Existing timelines permit exact-stamp verification only; unstamped or mismatched parents are never restamped. Revocation/re-grant hides prior-epoch current, History, DELETE, action, and replay uniformly. Family V1 POST is first-epoch-only; after a prior closed epoch or old-key collision, the service authorizes current context first and then returns the same non-disclosing 426 with zero mutation. Header 2 creates an isolated epoch-qualified STANDARD_V2 parent.

### Mother-share exclusion

A Mother-owned row exposed by a brownfield Family projection remains Mother-owned and unchanged. It is not stamped, relabeled, cloned, copied into Family History, or used to seed Family status/completion/timestamps. Family work is prospectively materialized as a new recipient-owned occurrence with independent state. The plan requires this invariant in migration candidates, API visibility, actions, History, canary checks, and PostgreSQL tests.

### Targetless V2 and data minimization

Target is absent from V2 identity, database-valid row shape, DTOs, responses, filters, Admin controls, mobile models, badges, and fixtures. The V1 target-bearing contract remains isolated behind negotiated compatibility until a separately approved zero-consumer migration. The plan also rejects free-text action reasons, excludes recommendation text and dating dates from audit/log payloads, treats remaining identifiers as linkable health metadata, and makes ordinary metrics identifier-free through the inherited normative requirements.

### PostgreSQL migration evidence

The plan requires nullable expand, deterministic backfill/quarantine, deferred validation, table-count invariance, row/key/status/time preservation, second-run no-op, parent-child version checks, V2 target-null enforcement, dating/event cardinality, append-only Family timeline/epoch enforcement, retained-member foreign keys, duplicate-membership quarantine and partial uniqueness. Empty bootstrap plus production-shaped upgrade must run against PostgreSQL and the full Flyway chain. A unit mock, H2 result, skipped PostgreSQL environment, or vacuous unseeded edge test cannot satisfy these gates.

### Audit, meta-audit, and break-glass

Mutation and audit evidence share the transaction. Audit access/export is named-role, purpose-bound, bounded, redacted, and meta-audited before data is returned; primary meta-audit failure denies normal access. Break-glass is time-limited, reason-controlled, dependent on an independent append-only secondary sink, immediately alerted, later reconciled, and disabled when its evidence write fails. Cadence cannot activate without the sink.

### Operations and rollback

Repair is hourly/startup/manual, app-independent, bounded by page budgets rather than a lossy lookback, serialized by PostgreSQL advisory lock, failure-forward, retryable, and governed by complete-sweep and oldest-unrepaired evidence. After any V2 write, rollback is forward-only through flags: stop new distribution/request ensure and V2 UI for affected scopes while preserving instances, actions, dating revisions, provenance, Family epochs, History, and audit. Legacy writers are never restored and V2 evidence is never deleted.

## Non-blocking findings

- **L-01:** P3A's phrase “require a fresh V2 basis or start V1 unresolved” can be read as making every V1 new-pregnancy transition unresolved, while the preceding final-matrix clause and normative design allow fresh safe V1 LMP-only, EDD-only, or exact-pair resolution. Phase 0 should copy the exact design matrix into the contract fixture so this shorthand cannot misdirect implementation.
- **L-02:** The required verification table groups gate IDs by broad ranges rather than assigning one identifier to each of the eleven §10 bullets. P0 already requires a per-blocker owner/environment/command/artifact map; that map should preserve one explicit row per bullet instead of relying only on range labels.
- **L-03:** “Production-shaped schema copy” could be misread as authorizing copied production health data. The P0 migration specification should state schema-only or synthetic/de-identified fixtures and prohibit production PHI in developer or CI evidence; the execution section already prefers synthetic fixtures.
- **L-04:** Quarantine is correctly constrained to existing audit/operational storage, but the concrete store, retention class, access role, and opaque payload schema are intentionally unnamed. P0 must freeze these before a migration is written so engineers do not improvise a free-text or PHI-bearing quarantine payload.
- **L-05:** P7's numbered audit deliverables do not repeat the spine's backup-encryption and retention/purge proofs, although the plan's AUD matrix retains retention evidence and declares design §9 exhaustive. Put those controls into the named AUD evidence manifest so they are not hidden in inherited text.
- **L-06:** The break-glass secondary sink is an activation dependency but its owner, availability objective, append-only proof, retention, and reconciliation runbook remain implementation-owned. P0/P7 should name those artifacts before any sink test is credited.
- **L-07:** The supported mobile-version floor has no literal version in this revision. This is acceptable because P0 must freeze it and P7/P8 enforce it, but the evidence record should bind the chosen value to store/build telemetry and the code/config revision.
- **L-08:** Only the two-hour complete-sweep threshold is fixed; backlog, retention, export, latency, and oldest-unrepaired thresholds are deliberately deferred to approved operations policy. P7 must treat an absent policy value as failure, exactly as the plan says, rather than choosing a convenient test-time threshold.
- **L-09:** Standard commands are listed, but the focused PostgreSQL, migration, concurrency, authorization, and fault-injection test class names are not. P0's command/evidence map should pin exact selectors and make skipped/zero-test execution fail the gate.
- **L-10:** The rollback model is sound, but the drill's acceptance record should explicitly prove flag propagation, no new occurrence writes, continued preservation/readability of V2 evidence, audit continuity, and no legacy-writer restoration. P0's rollback runbook is the proper place to make those assertions executable.
- **L-11:** The copy-review allowlist and qualification authority are intentionally not named in this plan. P3B/P7 evidence should bind stable authority identifiers and opaque qualification references to the manifest without copying credentials or unnecessary reviewer personal data into audit artifacts.
- **L-12:** P4 may begin when a P3B contract fixture is available rather than after P3B exit. That enables parallel development safely only if the fixture is generated from the frozen normalizer/manifest contract and drift makes dependent tests red; the P0 fixture approval should state that dependency explicitly.

These are precision improvements, not missing safety controls. Each is already contained by a plan phase, a normative-design override, or a fail-closed activation dependency. None creates a Critical, High, or Medium plan defect.

## Approval statement

Approve this implementation plan for Phase 0 only at the frozen hashes above. Do not activate cadence unless every final design §10 blocker has current named PASS evidence with timestamp, environment, code/config revision, owner, retained artifact, real PostgreSQL where required, supported-client enforcement, and operational/audit sink availability. Any normative/source hash drift, target reintroduction, clinical-rule proposal, new-table proposal, incomplete Family baseline, Mother-share leakage, retroactive action path, audit/meta-audit weakness, missing PostgreSQL evidence, or rollback-to-legacy-writes proposal invalidates this review and requires a new approval pass.
