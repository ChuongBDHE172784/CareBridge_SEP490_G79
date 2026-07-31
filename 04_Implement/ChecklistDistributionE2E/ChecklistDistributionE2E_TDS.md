# Checklist Distribution E2E — Technical Design and API Contract

**Status:** Complete for accepted student-project scope — 2026-07-30 (CHK-041 `WAIVED / ACCEPTED RISK`; not a technical PASS)
**Scope:** Spring Boot/PostgreSQL/Flyway backend, React Content Admin, Flutter Mother/Family Home
**Feature flag:** `checklist_distribution_v2` (default `false`)

**Implementation companion:** [ChecklistDistributionE2E_Phase1-Schema-Addendum.md](ChecklistDistributionE2E_Phase1-Schema-Addendum.md)

## 1. Contract intent

Approved immutable template versions are projected into recipient-owned checklist instances. Distribution is lifecycle-aware and idempotent. Mother Home and Family Home consume the same Today Tasks projection; reads never create or repair data.

Normative source: [`SPEC.md`](../../_bmad-output/specs/spec-checklist-distribution-e2e/SPEC.md), [`contract-rules.md`](../../_bmad-output/specs/spec-checklist-distribution-e2e/contract-rules.md) and the approved [architecture spine](../../_bmad-output/planning-artifacts/architecture/architecture-CareBridge_SEP490_G79-2026-07-29/ARCHITECTURE-SPINE.md).

## 2. Domain contract

### 2.1 Template authoring

- `recipientRoles`: non-empty subset of `MOTHER`, `FAMILY`.
- `stage`: one of `PREGNANCY`, `POSTPARTUM`, `BABY_CARE`, or `null` for V2 authoring. Existing `PRE_PREGNANCY` rows remain legacy/read-only and cannot activate until a Content Admin reclassifies them to a supported named-anchor stage; no pre-pregnancy anchor is inferred.
- `substage`: structured catalog reference or `null`; it is valid only with a lifecycle-aware role and matching stage.
- FAMILY-only templates normalize `stage` and `substage` to `null`.
- Every item has exactly one `targetSubject`: `MOTHER` or `BABY`; a version may mix targets.
- Approval freezes content, recipient roles, stage/substage, target and lineage/version IDs. Edit means clone to a new DRAFT version.

### 2.2 Eligibility

Eligibility requires an active approved version, a canonical context exactly equal to one `JOURNEY` or `BABY`, and context-owner equality with the care-group owner. Lifecycle-aware versions require an eligible stage/substage window. FAMILY-only lifecycle-neutral versions use `windowStart=NONE/windowEnd=NONE` and reconcile once per member/context when approval, membership or context events occur. Mother recipients are the owner; Family distribution requires `ACCEPTED` membership plus `CHECKLIST_VIEW`; action also requires `CHECKLIST_COMPLETE`. Ambiguous links are quarantined.

For lifecycle-aware templates, the named anchor is mandatory and no fallback anchor is inferred: LMP/EDD applies to the canonical journey, DELIVERY_DATE to postpartum journey and BIRTH_DATE to the canonical baby. Missing or contradictory anchors produce no distribution and a controlled reconciliation failure. Evaluate in the owner's IANA timezone: DAY is completed local calendar days from anchor; WEEK is `floor(DAY/7)`; MONTH is completed calendar months with day-of-month clamping. Ranges are non-negative, `startInclusive <= endInclusive`, and both bounds are inclusive. Item `dueAt` is the local start of its configured offset day converted to `Instant`; malformed or DST-ambiguous values use the timezone library's deterministic earlier-valid-offset rule and are covered by golden tests.

### 2.3 Persistence

`checklist_instances` stores recipient, role, group, context, exact template version, origin, applicability window, status, lock version, deterministic instance key and audit timestamps. Target is not a parent attribute. `checklist_task_instances` stores the parent, item version, text/timing/target snapshot, status and deterministic child key. Keys are non-null and unique at their stated grains. Instance status is `PENDING|IN_PROGRESS|COMPLETED|CANCELLED`; task status is `PENDING|IN_PROGRESS|COMPLETED|SKIPPED|CANCELLED`; UNSCHEDULED is a projection bucket, never a persisted status.

Key serialization is versioned `v1`: ordered length-prefixed UTF-8 fields, lowercase UUID strings, ISO-8601 local dates, Unicode NFC, then SHA-256. Literal `NONE` represents an explicit lifecycle-neutral window; `<ABSENT>` represents a component that is structurally unavailable, including a USER_CREATED template token. Those tokens must never collide. Parent grain is template version + recipient + role + care-group token + context type/ID + window start/end. Child grain is persisted parent ID + item version ID; USER_CREATED children use an explicit client task token. Parent upsert returns the persisted ID and children insert in the same transaction; Java/Flyway golden vectors must match.

## 3. API contract

All endpoints use JSON, authenticated user context and an error envelope `{code, message, correlationId, details?}`. IDs are UUID strings and timestamps are ISO-8601 UTC. API responses never include another care group's identifiers or checklist text after authorization failure.

### 3.1 Content Admin

`POST /api/v1/admin/checklist-templates` creates a DRAFT.

```json
{"lineageId":null,"title":"Postpartum checks","recipientRoles":["MOTHER","FAMILY"],"stage":"POSTPARTUM","substage":{"code":"DAY_0_7","anchor":"DELIVERY_DATE","startInclusive":0,"endInclusive":7,"unit":"DAY"},"items":[{"clientKey":"hydration","text":"Drink water","targetSubject":"MOTHER","offset":{"anchor":"DELIVERY_DATE","start":0,"end":7,"unit":"DAY"}}]}
```

`POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/approve` approves a new reviewed DRAFT after validation. Imported versions start `PENDING_REVIEW` with legacy catch-all metadata and remain distribution-disabled. A Content Admin must first replace legacy `NONE`/unsupported stage metadata with a supported named-anchor substage; `POST .../{versionId}/review` validates and records that corrected version, and `POST .../{versionId}/activate` alone moves the reviewed version to APPROVED with `distributionEnabled=true`. Any later Content Admin edit invalidates the review and requires review again. `POST .../{versionId}/clone` creates a new DRAFT. `POST .../{versionId}/archive` blocks new distribution. `GET /api/v1/admin/checklist-templates/{lineageId}` returns lineage and versions. Approval/activation emits a durable distribution candidate and triggers reconciliation. Content Admin scope owns authoring, submission, cloning and archive requests; SYSTEM_ADMIN scope alone owns approve, reject, imported review and activation. Distribution-affecting mutations are audited.

Validation errors: `TEMPLATE_ROLE_REQUIRED`, `FAMILY_STAGE_NOT_ALLOWED`, `SUBSTAGE_STAGE_MISMATCH`, `ITEM_TARGET_REQUIRED`, `VERSION_IMMUTABLE`, `MIGRATION_REVIEW_REQUIRED`.

### 3.2 Today projection

`GET /api/v1/tasks/today?date=YYYY-MM-DD` with optional `X-User-Timezone: Area/Location` header.

Response:

```json
{"asOf":"2026-08-03T01:00:00Z","zoneId":"Asia/Ho_Chi_Minh","horizonDays":7,"sections":{"overdue":[{"taskKind":"CHECKLIST","taskId":"...","instanceId":"...","templateVersionId":"...","careGroupId":"...","careContextType":"JOURNEY","careContextId":"...","title":"Drink water","targetSubject":"MOTHER","origin":"SYSTEM_TEMPLATE","status":"PENDING","timeBucket":"OVERDUE","allowedActions":["COMPLETE","SKIP"],"dueAt":"2026-08-02T08:00:00Z"}],"today":[],"upcoming":[],"unscheduled":[]},"counts":{"overdue":1,"today":0,"upcoming":0,"unscheduled":0},"correlationId":"..."}
```

`date` defaults to the effective local date; timezone is resolved from the valid IANA header or Asia/Ho_Chi_Minh. `OVERDUE` is non-terminal work due before local day start; `TODAY` is work due in the local day (including terminal actions made for that due date); `UPCOMING` is non-terminal work due in the next seven days; `UNSCHEDULED` preserves active migrated work without a safe due date; `CANCELLED` is excluded. The server ignores client role/group filters and derives scope from the authenticated principal. The endpoint reads V2 projections and related care/reminder tasks; it never inserts, updates status or triggers reconciliation.

For an unscheduled checklist task, a terminal `COMPLETED` or `SKIPPED` transition is projected into `TODAY` only when its persisted terminal timestamp falls inside the requested local day. This keeps completion visible across the stable refresh required by CHK-042/043 without retaining unscheduled terminal history in Today indefinitely.

Compatibility `GET /api/v1/reminders/today` remains byte/schema-compatible with the legacy reminder response until mobile cutover; it reads V2-backed reminder state where applicable and emits a deprecation metric. It does not return the unified envelope.

Provider normalization:

| taskKind | Stable task ID | origin/target | normalized status | allowed actions |
|---|---|---|---|---|
| CHECKLIST | checklist task-instance UUID | persisted origin and item target | checklist persisted state | COMPLETE/SKIP only when policy and state allow |
| REMINDER | existing reminder occurrence UUID, never definition-only ID | existing system/user provenance; explicit mapped target or null when legacy schema has none | provider maps to PENDING/COMPLETED/CANCELLED | only provider-supported actions advertised; no implicit SKIP |
| CARE_TASK | existing care-task UUID | persisted manual/system origin and explicit target | provider FSM mapping | only actions currently legal in Family/care-task FSM |

The aggregator does not invent actions. The facade authorizes first, dispatches by `taskKind`, and rejects any action absent from `allowedActions`. One principal receives the exact union of all currently ACCEPTED + CHECKLIST_VIEW groups; unpermitted, pending and revoked groups are excluded. Every item carries `careGroupId` and canonical context fields, and instances/counts are never merged across groups.

### 3.3 Unified task actions

`POST /api/v1/tasks/{taskKind}/{taskId}/actions` accepts:

```json
{"action":"COMPLETE","clientRequestId":"client-uuid","reason":null}
```

`action` is `COMPLETE` or `SKIP`; `reason` is required for `SKIP` and must be controlled (for example `NOT_APPLICABLE`, `USER_CHOICE`, `LIFECYCLE_CHANGED`). Response returns `{taskKind, taskId, instanceId, action, previousStatus, status, appliedAt, idempotentReplay, correlationId}`. Durable command identity is `(actorId, taskKind, taskId, clientRequestId)` plus canonical payload hash. Authorization occurs before replay lookup. Concurrent claimants serialize on the unique row; same payload returns the original result and different payload returns `IDEMPOTENCY_KEY_REUSE`. Command rows are retained until the task is terminal and for seven years from `appliedAt`, extended by legal hold; operations-only purge is audited. After lawful purge, authorization and terminal CAS still prevent reapplication and return `TASK_ALREADY_TERMINAL`; no active-task command row may be purged. System tasks reject edit/delete actions with `SYSTEM_TASK_IMMUTABLE`; terminal tasks return `TASK_ALREADY_TERMINAL` with the current state.

Legacy action routes adapt to this facade and do not write legacy tables after V2 activation.

### 3.4 Reconciliation operations

`POST /api/v1/operations/checklist-reconciliation/runs` starts an authorized run (normally scheduler-only). `POST /api/v1/operations/checklist-reconciliation/replay/{correlationId}` replays an exhausted candidate. `GET /api/v1/operations/checklist-reconciliation/health` exposes last success, lag, candidate counts and retry exhaustion without checklist text. Only operations/SYSTEM_ADMIN may call these routes.

## 4. Authorization and privacy

| Operation | MOTHER | FAMILY | Content Admin | SYSTEM_ADMIN/Operations |
|---|---|---|---|---|
| Own Today/tasks | own group/context | exact union of accepted groups with CHECKLIST_VIEW | scoped metadata admin only, no recipient task content | no recipient task content |
| Complete/skip | own task | own accepted group/context + CHECKLIST_VIEW + CHECKLIST_COMPLETE | no | no recipient mutation |
| Author/submit/clone | no | no | yes | no |
| Approve/reject/review/activate | no | no | no | SYSTEM_ADMIN only |
| Audit read | no | no | no | yes |

Every query predicates by authenticated user, accepted membership, permission, group owner and canonical context owner. Nonexistent and unauthorized direct IDs return the same `404 TASK_NOT_FOUND` envelope with no resource metadata. Revoke invalidates access immediately but does not mutate the recipient-owned instance; regrant to the same group/context/window reuses that existing deterministic instance, while a genuinely new context/window creates one new instance. Relink never transfers an old instance across context keys. Audit/operations retain history; revoked users cannot read it.

## 5. State and concurrency rules

Use row locks or optimistic CAS on instance/task version. Complete/skip and lifecycle reconciliation are serialized. A terminal mutation and audit event commit atomically. New checklist audit action enums must be added to the eligibility policy; an ineligible required action, allowlisted-DTO serialization failure or persistence failure aborts the business transaction. The audit schema stores actor type/service, recipient, context, task/version IDs, before/after status, controlled reason and indexed UUID correlation ID as typed columns or constrained fields; arbitrary details are forbidden. Distribution conflict compares canonical payload; same key with differing payload is encrypted/quarantined and alerted rather than silently overwritten. GET requests are side-effect free.

## 6. Migration and rollout

1. Expand schema and permissions (default deny).
2. Backfill templates, contexts, instances/tasks and audit/quarantine records; preserve original IDs and row hashes. Group repeated work by deterministic legacy occurrence token, validate baby/journey/group ownership, and quarantine collisions.
3. Verify counts, deterministic grouping, status/timestamp mapping, ordering, context owner and seeded `IN_PROGRESS` rows.
4. Activate compatibility adapters, then enable `checklist_distribution_v2` for a controlled cohort.
5. Observe duplicate rate, authorization denials, reconciliation lag and audit failures.
6. Contract legacy writes only after mobile/web cutover and rollback rehearsal. Rollback disables new distribution and new UI flags only; V2 storage, task actions and compatibility adapters remain authoritative. It never restores legacy writes or hides V2 completions.

## 7. Operational and NFR contract

Domain mutations that affect eligibility publish a durable outbox/candidate record in the source transaction. Reconciliation consumes by event ID and also scans from the last successful watermark with an overlap window; per-candidate outcomes make replay idempotent. It runs hourly and at startup. Alert when no successful run exists for two hours or a candidate exhausts retries. Correlate events with UUIDs, expose per-run counts and redact text from logs/audits/errors/quarantine views. Audit retention starts at event creation, honors legal hold, restricts purge to operations and audits every purge. Release requires WCAG 2.1 AA evidence and changed-code line coverage ≥80% with branch coverage for eligibility, state and authorization policies. On the production-like reference environment: Today at 50 requests/second with 500 tasks and 20 groups per principal must meet p95 ≤500 ms and p99 ≤1 second; one run must process 10,000 candidates within 15 minutes; blocking Flyway locks must stay ≤5 seconds; backfill must sustain ≥500 rows/second and the full migration must finish within 30 minutes. Abort before cohort enablement on any threshold breach, migration error, lock timeout, count/hash mismatch or quarantine-rate increase above the reviewed fixture baseline; retain the expanded schema and roll forward after correction.

## 8. UI contract and verification status

### Verification status - 2026-07-30

- Core checklist backend code review gate: PASS. Three independent final passes report no open code P0/P1; the later migration-readiness additions are evidenced separately below.
- PostgreSQL functional gate: PASS for occurrence repair (2/2), Family multi-group lifecycle (8/8), authorization/Today API (9/9) and business/audit atomicity (9/9).
- CHK-041 standalone local reference rerun: PASS after the performance harness was corrected to use production-like INFO logging instead of the repository test profile's synchronous DEBUG SQL/ORM/application logging. Today p95/p99 were 442.29/660.22 ms; 10,000 reconciliation candidates completed in 10.43 s; Flyway lock completion was 1.29 s; backfill and occurrence repair sustained 5,983.44 and 1,506.43 rows/s; full seeded migration completed in 10.70 s. The same idle-host harness was RED at p95 581.85 ms before the logging correction, confirming that the DEBUG test instrumentation materially distorted the measurement. CHK-041 remains open until the external production-like count/hash/quarantine and abort-to-roll-forward rehearsal is executed.
- Changed-code line coverage: PASS at 80.78% across the four primary changed orchestration/policy classes. Branch coverage is reported in the Test-Spec and is not represented as an 80% gate.
- CHK-042 Mother mobile/API E2E: PASS. Authoritative run `20260730T064329247Z-7f23c4cc65db4e5492709fd2e2253012` used `emulator-5554` and disposable environment `e2e-checklist-20260730-1345`; it authored, approved and reconciled a unique template, completed the Mother-owned task from Mother Home, verified stable terminal refresh, persistence and typed audit.
- CHK-043 Family mobile/API E2E: PASS. The same run verified accepted+permitted Family distribution, separate recipient-owned instance/task IDs, shared canonical group/context, bidirectional direct-ID 404 isolation, unrelated-family exclusion, Family Home completion, stable refresh and typed audit.
- Flutter verification: PASS. The prior focused Today/Home/View Content/checklist suite remains 34/34; the user-created composer, response metadata and production wiring delta passed 9/9; and the full Flutter suite passed 575/575. Full Flutter analyze reports no issues and the debug APK build succeeds. The deterministic lifecycle-content CTA regression discovered by the full suite was restored and its six-test file passes.
- Live mobile/API execution gate: PASS. The hardened fail-closed runner passed 23/23 hermetic cases and independent review with P0=0/P1=0/P2=0. Authoritative evidence has exactly one visible, non-skipped canonical test, Flutter exit 0, one successful overall done event, no malformed events and matching sanitized-log SHA-256. Port 18080 was closed and the defines/ready/stop artifacts were deleted after the run; leak scan found zero JWTs, raw credential assignments or encoded credential prefixes.
- Migration rehearsal tooling/readiness gate: PASS for local implementation evidence. The verifier now fails closed on unresolved quarantine above the reviewed baseline, exposes an OPERATIONS-only readiness endpoint, and the external script gates exact Flyway version/checksum, dataset fingerprint, structural count/hash, forced-RLS visibility, exactly-one target/quarantine outcome, derived throughput, sealed abort evidence and roll-forward binding. Focused backend and real embedded-PostgreSQL script/RLS tests passed 11/11.
- Production-like CHK-041 load/migration rehearsal: `WAIVED / ACCEPTED RISK` for the student-project scope by explicit owner decision on 2026-07-30. This is not a PASS and does not establish production capacity. The latest full disposable run passed clean/corrected Flyway chains, `CapturePre`, remainder and `VerifyPost`, then failed Today under concurrent load when Hikari reached `total=10, active=10, waiting=40`; reconciliation load, `ABORT_PROVEN` and `ROLL_FORWARD_PASS` did not execute. Cleanup completed with zero portable PostgreSQL processes and zero active run roots. A batching optimization and harness logging isolation subsequently passed 11/11 narrow tests, but no successful full performance rerun is claimed.

### UI contract

Mother Home and Family Home each render Today sections, loading, empty, error and retry states. Source and target badges use Warm Claymorphism tokens with icon plus text (“System template”, “My care”, “Baby”). Mother Today exposes a context-bound user-created task composer with explicit MOTHER/BABY target, one canonical journey/baby context, a stable client task ID for identical retries and automatic Today refresh after creation; Family does not gain create permission. PreparationChecklistScreen and its direct navigation are removed only after both replacement entry points pass route/import and widget tests. Focus order, semantics labels, contrast and scalable text are required.
### Approved CHK-041 reference-environment decision — 2026-07-30

The product owner explicitly approved a deterministic **synthetic production-representative dataset** and a **local disposable PostgreSQL 18** cluster as the final CHK-041 reference environment. This supersedes only the earlier requirement for a sanitized external backup/host; it does not waive or lower any CHK-041 threshold or safety gate. The approval is valid only when one sealed run binds the exact fixture fingerprint, PostgreSQL/tool identities, host profile, application commit, Flyway history, raw timing/lock logs and cohort-disabled simulation, and proves count/hash reconciliation, reviewed quarantine baseline, a real fail-closed abort, a same-threshold forward-only correction, roll-forward pass, cleanup and leak scan. It does not establish production capacity beyond the approved local host profile.

### Owner-accepted CHK-041 waiver — 2026-07-30

The owner subsequently classified production-scale performance as non-critical for this student project and explicitly authorized closing the goal without completing CHK-041. Therefore CHK-041 is recorded as **`WAIVED / ACCEPTED RISK`**, never `PASS`. The waiver accepts the failed 50-RPS Today evidence and the absence of final reconciliation-load, abort and roll-forward proof. Any future production or pilot release must reopen CHK-041 and complete the unchanged gates.
