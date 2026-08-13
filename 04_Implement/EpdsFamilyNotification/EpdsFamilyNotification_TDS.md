# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification — EpdsFamilyNotification: Deliver EPDS Screening Results to Care-Group Family Accounts

| Field              | Value                                        |
| ------------------ | -------------------------------------------- |
| **Document ID**    | `CB-EPDS-IMP-001`                            |
| **Version**        | `1.0`                                        |
| **Date**           | `2026-08-14`                                 |
| **Status**         | `Implemented — 2026-08-14` *(production still gated on OPEN-1 and OPEN-6)* |
| **Document Owner** | `HuyND`                                      |
| **Author**         | `AI Agent`                                   |
| **Reviewed by**    | `[ ] Pending`                                |
| **DPO Sign-off**   | `[ ] Pending` *(module distributes Sensitive-PII: maternal mental-health screening results leave the mother's own account and reach third-party FAMILY accounts — see §5.4 Data Classification and OPEN-1)* |
| **Approved by**    | `[ ] Pending`                                |
| **Last Review**    | `2026-08-14`                                 |
| **Based on EDS**   | `v2.0`                                       |

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Never delete old information. Every change must be recorded in this table.

| Date       | Performed by | Change description                                                                 |
| ---------- | ------------ | ---------------------------------------------------------------------------------- |
| 2026-08-14 | AI Agent     | Initial creation — TDS for EpdsFamilyNotification (Draft). Two safety/privacy decisions confirmed by user before drafting: (D1) Question-10 positive case delivers a category-anonymised escalation that never names self-harm/suicidal ideation; (D2) delivery is gated on `PermissionFlag.QUICK_NOTE_EPDS` under parent `QUICK_NOTES`. |
| 2026-08-14 | AI Agent — Amelia (Dev Agent) | Phase 3: Implementation. Backend feature suite **23/23 PASS** (Red Gate 21/23 FAIL + 2 documented exceptions — see Test-Spec §5.1). Mobile notification suite **20/20 PASS**. NEW: `health/event/EpdsScreeningCompleted.java`, `health/policy/EpdsSeverityPolicy.java`, `notification/service/impl/EpdsFamilyNotificationService.java`, migration `V20260814120000__allow_epds_result_notifications.sql`, mobile `features/notification/notification_type_display.dart`. MODIFIED: `NotificationType` (+`EPDS_RESULT`), `HealthMetricServiceImpl` (+`publishIfEpdsScreening`), `CareGroupMemberRepository` (+`findAcceptedFamilyMembersForEpdsAlerts`), both mobile notification screens. **Corrections made during implementation:** (1) §5.6 V1 sync requirement was wrong — `LOCATION_SHARE` precedent shows V1 is not synced; V1 left untouched. (2) §5.5 duplicate-recipient hazard confirmed real (mother may own multiple ACTIVE groups) — dedup implemented with consent-before-dedup ordering, TC-22/TC-23 added. **Not delivered:** TC-13, TC-14b (Docker unavailable) → OPEN-6. |

---

## TABLE OF CONTENTS

1. Module Overview
2. Traceability Matrix
3. Architecture Decision Records (ADR)
4. Non-Functional Requirements & SLA
5. Static Modeling
6. Dynamic Modeling
7. Domain Event Catalog
8. Interface Specification
9. API Specification
10. Error Codes
11. Implementation Plan (Step-by-Step)
12. Rollback & Incident Runbook
13. Test Strategy
14. Verification Methods
15. API Verification Samples
16. Authorization Matrix
17. AI Prompt Constraints (CASE 2.0)
- OPEN ITEMS
- APPENDIX

---

## 1. Module Overview

### 1.1 Purpose

When a MOTHER completes an EPDS (Edinburgh Postnatal Depression Scale) mood screening in the
mobile app, the resulting total score and its severity interpretation are currently visible
**only to the mother herself**. This module additionally delivers that result as an in-app /
push notification to the **FAMILY accounts registered in her care group**, so that her support
network is informed without the mother having to relay the result manually.

### 1.2 Actor & Trigger

| Item | Value |
| ---- | ----- |
| **Primary actor** | MOTHER (submits the screening) |
| **Secondary actor** | FAMILY (receives the notification) |
| **Trigger** | MOTHER taps submit on `epds_screen.dart`, producing `POST` add-metric with `metricType = EPDS_SCORE` |
| **Outcome** | Every eligible FAMILY member of the mother's ACTIVE care group receives one notification carrying the total score and a severity band |

### 1.3 Scope

**In scope**
- A domain event published from the EPDS write path.
- An async, after-commit notification listener in the `notification` module.
- A backend policy class that derives the severity band and the family-facing message.
- A new `NotificationType` value plus its Flyway migration.
- Mobile rendering (icon / label / routing) for the new notification type.

**Out of scope** (must not be designed or changed in this round)
- EPDS questionnaire content, option weights, or the scoring formula.
- The existing family dashboard and quick-note history endpoints (`FamilyDashboardService`,
  `FamilyQuickNoteService`) — their EPDS redaction behavior is a *constraint* here, not a target.
- Any change to what the MOTHER herself sees on `epds_screen.dart`.
- Email/SMS channels — this module uses the existing in-app + FCM push channel only.

### 1.4 Preconditions

1. The mother owns an ACTIVE `MotherJourney` (already enforced by `addMetric`).
2. The mother owns an ACTIVE `CareGroup` (`care_groups.owner_user_id = mother.user_id`).
3. At least one FAMILY member has `invitation_status = 'ACCEPTED'` and is enabled/unlocked.
4. That member has been granted `quickNotes` AND `quickNoteEpds` by the mother.

### 1.5 Postconditions

1. One `notification_records` row per eligible recipient, `type = EPDS_RESULT`.
2. The EPDS observation is persisted exactly as before — this module never mutates it.
3. Failure to notify never fails or rolls back the mother's EPDS submission.

### 1.6 Current State vs Target State

| Aspect | Current | Target |
| ------ | ------- | ------ |
| EPDS submit | `HealthMetricServiceImpl.addMetric` persists `HealthObservation`, audits, returns | Unchanged, **plus** publishes `EpdsScreeningCompleted` when `metricCode == "EPDS_SCORE"` |
| Family awareness | Pull-only: family must open the dashboard and already hold `quickNoteEpds` | Push: eligible family are notified proactively |
| Severity interpretation | Computed client-side only, in Dart (`epdsLevel`, `epdsGuidance`) | Additionally computed server-side in a Java policy for the family-facing message |
| `NotificationType` | 8 values, DB `CHECK` constraint | 9 values (`EPDS_RESULT` added) + new Flyway migration |

### 1.7 Key Constraint — Existing Privacy Boundary (BR-SAFETY-EPDS-001)

`FamilyQuickNoteServiceTest.epdsAnswerPayloadIsNotExposedToFamilyHistory` asserts that
family-facing EPDS data has `note == null`, `context` empty, and **`valueSecondary == null`**.

`valueSecondary` carries the **Question-10 score**. Question 10 is the self-harm item
(*"Tôi đã từng nghĩ đến chuyện tự hại bản thân"*, `epds_screen.dart:324`), and the mobile
`epdsGuidance(score, question10Score)` (`epds_screen.dart:23`) **branches on `question10Score > 0`**
to return text that explicitly names suicidal ideation.

**Therefore the family notification must never forward `epdsGuidance` verbatim.** Doing so would
re-introduce, through the notification channel, exactly the disclosure the existing test forbids
on the history channel. See ADR-003 and OPEN-1.

---

## 2. Traceability Matrix

| ID | Source | Requirement | Design element | Test condition |
| -- | ------ | ----------- | -------------- | -------------- |
| REQ-EPDS-N-01 | User request (2026-08-14) | EPDS result must reach FAMILY accounts in the care group | §5.2 `EpdsFamilyNotificationService` | TC-01, TC-02 |
| REQ-EPDS-N-02 | User request (2026-08-14) | Notification carries score **and** interpretation | §5.3 `EpdsSeverityPolicy` | TC-03, TC-04, TC-05 |
| BR-SAFETY-EPDS-001 | `FamilyQuickNoteServiceTest:90` (existing test, current code) | Question-10 score must not be exposed to family | §5.3 + ADR-003 | TC-06, TC-07 (CRITICAL) |
| BR-SAFETY-EPDS-002 | `CLAUDE.md` — "Do not hide urgent-care escalation" | Urgent results must still escalate to family | §5.3 `escalationMessage()` | TC-08 (CRITICAL) |
| BR-CONSENT-EPDS-001 | User decision D2 (2026-08-14); `FamilyDashboardService:327` precedent | Delivery gated on `QUICK_NOTE_EPDS` under `QUICK_NOTES` | §5.2 recipient filter | TC-09, TC-10, TC-11 (CRITICAL) |
| BR-RBAC-EPDS-001 | `CareGroupMemberRepository:60-76` precedent | Only ACCEPTED, enabled, unlocked, `role = FAMILY` receive | §5.5 repository query | TC-12, TC-13 |
| NFR-EPDS-N-06 | `CLAUDE.md` — safety workflows preserve audit | Every send is audited | §5.2 (reuses `NotificationService.send` audit) | TC-14a |
| NFR-EPDS-N-03 | ADR-001 | Notification failure must not roll back the EPDS write | §6.2 error flow | TC-15b (CRITICAL) |
| INV-4 | TDS §5.5 duplicate-recipient hazard | Exactly one notification per (recipient, screening), even across multiple owned groups | §5.2 step 4 de-duplication | TC-22, TC-23 (CRITICAL) |

> **Traceability note:** REQ-EPDS-N-01/02 originate from a direct user request, not from an
> approved SRS clause. The SRS entry closest to this behavior is `UC-32 Manage EPDS Screening`
> (`02_Requirements/SRS/Report3_Functional_Specifications.md:658`), whose Normal Flow step 5 says
> the system "updates related data, history or notifications where applicable" — supportive but
> not specific. Recorded as OPEN-3.

---

## 3. Architecture Decision Records (ADR)

### ADR-001 — Deliver via an after-commit domain event, not an inline call

**Context.** `addMetric` is the single write path for all P0 manual metrics, not just EPDS. It is
`@Transactional`. Calling the notification stack inline would (a) put FCM network latency inside
the mother's write transaction, and (b) risk rolling back a *successfully recorded screening*
because a *notification* failed.

**Decision.** `addMetric` publishes a narrow `EpdsScreeningCompleted` event, guarded on
`metricCode == "EPDS_SCORE"`. A listener in the `notification` module consumes it with
`@Async @TransactionalEventListener(phase = AFTER_COMMIT)` and a top-level try/catch-and-log.

**Consequences.**
- ✅ Mirrors two established precedents: the `MaternalHealthMetricDeleted` publish in
  `deleteMetric` (`HealthMetricServiceImpl:77`) and the listener shape of
  `FamilyNotificationService.onFamilyMemberInvited` (`FamilyNotificationService:30-71`).
- ✅ The EPDS write is durable before any notification work begins.
- ✅ `health` module gains no dependency on `notification`.
- ⚠️ Delivery is best-effort. A crash between commit and dispatch loses the notification with no
  retry. Accepted for v1 — this is an informational notification, not the emergency-alert path
  (which has its own outbox job). Recorded as OPEN-2.

### ADR-002 — Severity interpretation lives in a backend policy class

**Context.** `epdsLevel` / `epdsGuidance` exist only in Dart, inside a screen widget. The backend
must produce the family-facing text, and cannot import Dart.

**Decision.** Add `health/policy/EpdsSeverityPolicy.java` holding the band thresholds and the
family-facing message builders. Thresholds are recorded in §5.3 so the Dart copy and the Java copy
stay reconcilable.

**Consequences.**
- ✅ Matches CLAUDE.md's layer rule ("Policy: reusable domain rules") and the
  `triage/policy/TriageRedFlagPreScreenPolicy` precedent.
- ✅ Pure function, trivially unit-testable with no Spring context.
- ⚠️ Thresholds are now duplicated in Dart and Java. Divergence is a real maintenance risk; §14
  defines the reconciliation check. Deliberately **not** solved by an API round-trip in v1.

### ADR-003 — Family receives the total-score band only; Question 10 escalates without disclosure

**Context.** The direct conflict described in §1.7: BR-SAFETY-EPDS-001 forbids exposing Question 10
to family, while BR-SAFETY-EPDS-002 (CLAUDE.md) forbids hiding urgent-care escalation. A mother can
score a low total (e.g. 8 → "Nguy cơ hiện tại thấp") while answering Question 10 positively.

**Decision** (user-confirmed 2026-08-14, decision D1).
1. The normal message carries `valueNumeric` (total score) and the band from
   `epdsLevel(totalScore)` — **derived from the total only**.
2. When `valueSecondary > 0`, the message is replaced by an **escalation-shaped message that names
   no item, no sub-score, and no self-harm/suicide wording** — it states that the result needs
   prompt attention and asks the family to reach out to the mother.
3. `epdsGuidance()` text is **never** transmitted to family in any branch.

**Consequences.**
- ✅ Satisfies both rules: family is escalated to, without learning *which* item drove it.
- ✅ Keeps the notification channel consistent with the already-redacted history channel.
- ⚠️ **Residual inference — the redaction is partial, not total.** The notification itself names no
  item, but every recipient of it holds `quickNoteEpds` and can therefore open the family history
  and see the **total score** (which is redacted only of `valueSecondary`). A recipient who
  cross-references an escalation message against a visible low total can infer that the escalation
  was driven by Question 10. This inference cannot be closed while the design both escalates and
  leaves the total visible on the history channel; suppressing the numeric total from the *message*
  (§5.3) narrows the leak but does not eliminate it. **What is being accepted is escalation without
  explicit disclosure, not escalation without inferability.** Recorded as OPEN-1 — the approver and
  DPO must sign off on this weaker guarantee specifically.
- ⚠️ The escalation message must be reviewed by a clinician before production. Recorded as OPEN-1.

### ADR-004 — Reuse `referenceId = careGroupId`, do not change `SendNotificationRequest`

**Context.** `NotificationRecord` has a `careGroupId` column, but `SendNotificationRequest` is a
Java `record` with no such component, and `NotificationServiceImpl.send` never populates it.
Adding a component changes the canonical constructor arity and breaks all 8+ existing call sites.

**Decision.** Follow the `GROUP_INVITE` precedent (`FamilyNotificationService:56-63`):
`referenceId = careGroupId`, `referenceType = "CARE_GROUP"`. Leave `SendNotificationRequest`
untouched and `notification_records.care_group_id` NULL for this type.

**Consequences.**
- ✅ Zero blast radius on existing notification producers.
- ✅ Mobile can route the tap to the care-group screen, where the `QUICK_NOTE_EPDS` consent gate is
  already enforced server-side on any data actually rendered.
- ⚠️ These notifications are not group-scoped at the column level, so any future
  "filter notifications by care group" feature will not see them. Acceptable — `GROUP_INVITE`
  already has this property. Recorded as OPEN-4.

---

## 4. Non-Functional Requirements & SLA

| ID | Requirement | Target | Verification |
| -- | ----------- | ------ | ------------ |
| NFR-EPDS-N-01 | EPDS submit latency must not regress | Added synchronous cost ≤ 1 ms (one enum compare + one event publish) | §14.1 |
| NFR-EPDS-N-02 | Notification dispatch is off the request thread | Listener annotated `@Async` + `AFTER_COMMIT`; `@EnableAsync` verified present (`BackendApplication.java:11`) | TC-15a, §14.2 |
| NFR-EPDS-N-03 | Notification failure isolation | EPDS observation persists even when the listener throws | TC-15b |
| NFR-EPDS-N-04 | Recipient resolution cost | **1 recipient query + 2 permission lookups per candidate row** (`1 + 2N`), bounded by care-group size (typically < 10). *Corrected during implementation — the draft claimed "1 query per screening", which the two-flag consent gate makes false.* | §14.1 |
| NFR-EPDS-N-05 | PII hygiene in logs | Log lines carry recipient count, `careGroupId`, band name, and **pseudonymous user ids** — never the total score, never `valueSecondary`, never answers. *User ids are permitted, matching the `FamilyNotificationService.java:66` precedent; they are opaque UUIDs, unlike the mental-health measures which are the sensitive payload.* | TC-16, §14.3 |
| NFR-EPDS-N-06 | Audit preservation | Every dispatch flows through `NotificationService.send`, which audits `NOTIFICATION_SENT` / `NOTIFICATION_FAILED` | TC-14a |

---

## 5. Static Modeling

### 5.1 Planned File Paths

| Action | Path | Responsibility |
| ------ | ---- | -------------- |
| NEW | `.../backend/health/event/EpdsScreeningCompleted.java` | Domain event record |
| NEW | `.../backend/health/policy/EpdsSeverityPolicy.java` | Band thresholds + family message builders |
| NEW | `.../backend/notification/service/impl/EpdsFamilyNotificationService.java` | Async listener, recipient resolution, consent filter, dispatch |
| MODIFY | `.../backend/health/service/impl/HealthMetricServiceImpl.java` | Publish event at the end of `addMetric`, guarded on metric code |
| MODIFY | `.../backend/notification/entity/NotificationType.java` | Add `EPDS_RESULT` |
| MODIFY | `.../backend/family/repository/CareGroupMemberRepository.java` | Add `findAcceptedFamilyMembersForEpdsAlerts` |
| NEW | `.../resources/db/migration/V20260814120000__allow_epds_result_notifications.sql` | Extend `notification_records_type_check` |
| MODIFY | `.../resources/db/migration/V1__init_schema.sql` | Sync canonical baseline (see §5.6) |
| MODIFY | `.../CareBridgeMobileApp/lib/features/notification/screens/notifications_screen.dart` | Icon + label for `EPDS_RESULT`; delegates the label map to the new shared file |
| MODIFY | `.../CareBridgeMobileApp/lib/features/notification/screens/notification_center_screen.dart` | Icon for `EPDS_RESULT`; delegates the icon lookup to the new shared file |
| NEW | `.../CareBridgeMobileApp/lib/features/notification/notification_type_display.dart` | Pure label/icon mapping extracted from the two screens so it is unit-testable without pumping a screen (added during implementation — TC-20/TC-21) |

> **Mobile routing — scope correction (2026-08-14).** This row originally promised "icon/label/routing".
> **Only icon and label were implemented.** Tapping an `EPDS_RESULT` notification falls through to the
> existing default detail screen. This was verified to be benign: despite carrying
> `referenceType = "CARE_GROUP"`, `EPDS_RESULT` is **not** matched by the
> `GROUP_INVITE || CARE_GROUP_INVITATION || FAMILY_SYNC` branch at
> `notification_center_screen.dart:253`, so it cannot render a phantom care-group invitation.
> A dedicated tap target (e.g. into the family EPDS history, where the consent gate is already
> enforced server-side) remains available as a follow-up.

### 5.2 `EpdsFamilyNotificationService` — Responsibilities

1. Listen for `EpdsScreeningCompleted` (`@Async`, `AFTER_COMMIT`).
2. Resolve the mother's ACTIVE care group(s) and eligible FAMILY members via the repository query
   in §5.5.
3. For each member row, apply the consent gate:
   `authorizationPolicy.hasPermission(groupId, userId, QUICK_NOTES)` **AND**
   `authorizationPolicy.hasPermission(groupId, userId, QUICK_NOTE_EPDS)`.
   Reuse `CareGroupAuthorizationPolicy` — do **not** reimplement permission parsing.
4. **De-duplicate by `userId`** across the surviving rows (see the hazard note in §5.5).
5. Build title/body via `EpdsSeverityPolicy`.
6. Call `notificationService.send(...)` per de-duplicated recipient, catching **inside** the loop.
7. Wrap the whole method in try/catch-and-log (mirrors `FamilyNotificationService:33,68`).

> **Step order is load-bearing.** Consent filtering (3) must run *before* de-duplication (4), never
> after. A FAMILY user may belong to two of the mother's groups holding `quickNoteEpds` in only one
> of them. De-duplicating first could retain the non-consented row and drop the consented one,
> silently suppressing a notification the mother did authorise — and, with the opposite row order,
> could send one she did not. Filter first, then collapse the survivors. Pinned by TC-23.

> **Owner-bypass note.** `FamilyDashboardService` uses `owner || hasPermission(...)`. That bypass is
> **deliberately not applied here**: the group owner is the mother herself, who is the subject of
> the screening and must never be a recipient of her own notification. The `role = 'FAMILY'` join in
> §5.5 already excludes her.

### 5.3 `EpdsSeverityPolicy` — Thresholds and Messages

Thresholds mirror `epds_screen.dart:17-21` exactly:

| Total score | Band (`epdsLevel`) |
| ----------- | ------------------ |
| `>= 13` | `Cần được đánh giá chuyên sâu` |
| `>= 10` and `< 13` | `Cần theo dõi và sàng lọc lại` |
| `< 10` | `Nguy cơ hiện tại thấp` |

**Family-facing messages.**

*Normal branch* (`question10 == 0`):
```
Title: Kết quả sàng lọc EPDS
Body:  Mẹ vừa hoàn thành sàng lọc tâm trạng EPDS.
       Điểm {total}/30 — {band}.
       Đây là dữ liệu theo dõi, không phải chẩn đoán y khoa.
```

*Escalation branch* (`question10 > 0`, per ADR-003):
```
Title: Kết quả sàng lọc EPDS cần được quan tâm
Body:  Mẹ vừa hoàn thành sàng lọc tâm trạng EPDS.
       Kết quả cần được quan tâm ngay — hãy liên hệ và ở bên mẹ.
       Đây là dữ liệu theo dõi, không phải chẩn đoán y khoa.
```

**Hard constraints on the escalation branch (test-enforced, TC-06/TC-07):**
- MUST NOT contain the Question-10 score or any sub-score.
- MUST NOT contain the substrings `tự hại`, `tự sát`, `câu 10`, `question 10` (case-insensitive).
- MUST NOT include the numeric total (the total does not explain the escalation and would let a
  recipient infer that a low total + escalation means Question 10 — the exact inference ADR-003
  prevents).

The disclaimer line reuses the existing constant wording at `HealthMetricServiceImpl:45`.

### 5.4 Data Classification

| Field | Classification | Leaves mother's account? | Control |
| ----- | -------------- | ------------------------ | ------- |
| `valueNumeric` (total score) | **Sensitive-PII** (mental-health measure) | Yes — normal branch only | `QUICK_NOTE_EPDS` consent gate |
| Severity band | **Sensitive-PII** (derived) | Yes — normal branch only | `QUICK_NOTE_EPDS` consent gate |
| `valueSecondary` (Question 10) | **Sensitive-PII — highest** (self-harm ideation) | **No — never** | ADR-003; TC-06/TC-07 |
| `note` / `context` (raw answers) | **Sensitive-PII — highest** | **No — never** | Never read by this module |
| `careGroupId` | Non-PII identifier | Yes, as `referenceId` | — |

### 5.5 Repository Query

Add to `CareGroupMemberRepository`, modelled on `findAcceptedFamilyMembersForEmergencyAlerts`
(`CareGroupMemberRepository:60-76`) — returns the `CareGroupMember` rows so `permission_json` and
the group scope are available for the consent filter:

```sql
SELECT cgm.*
  FROM care_groups cg
  JOIN care_group_members cgm ON cgm.care_group_id = cg.care_group_id
  JOIN users u                ON u.user_id = cgm.user_id
 WHERE cg.owner_user_id = :ownerUserId
   AND cg.status = 'ACTIVE'
   AND cgm.invitation_status = 'ACCEPTED'
   AND u.role = 'FAMILY'
   AND u.enabled = TRUE
   AND u.locked = FALSE
 ORDER BY cgm.care_group_id ASC, cgm.user_id ASC
```

> **Ownership assumption (verified).** The query keys on `cg.owner_user_id`. `CareGroupServiceImpl:120`
> sets `ownerUserId(callerId)` at creation, so the mother is the owner of the group she creates.
> If a mother could ever belong to a group she does not own, this query would silently return zero
> recipients. TC-13 pins this assumption.
>
> **⚠️ Duplicate-recipient hazard (INV-4).** This query has **no `GROUP BY cgm.user_id`** — unlike
> `findAcceptedFamilyUserIds` (`CareGroupMemberRepository:37-53`), which does. Group uniqueness is
> enforced only on `(owner_user_id, group_name)` (`CareGroupServiceImpl:99`), so **a mother can own
> several ACTIVE care groups**, and one FAMILY user may be an ACCEPTED member of more than one.
> Such a user would be returned once per group and would receive duplicate notifications for a
> single screening, breaching INV-4.
>
> **Required mitigation:** the listener MUST de-duplicate by `userId` before dispatch, keeping the
> first row in the query's deterministic `care_group_id ASC` order (so `referenceId` is stable
> across runs). De-duplication is done in the listener rather than by adding `GROUP BY` to the SQL
> because the consent check needs each row's `permission_json`; the surviving row's group is the one
> whose permissions and `referenceId` are used. Pinned by TC-22.

### 5.6 Schema Delta

New migration `V20260814120000__allow_epds_result_notifications.sql`, following the exact pattern of
`V20260811150000__allow_location_share_notifications.sql`:

```sql
ALTER TABLE public.notification_records
    DROP CONSTRAINT IF EXISTS notification_records_type_check;

ALTER TABLE public.notification_records
    ADD CONSTRAINT notification_records_type_check CHECK (type IN (
        'REMINDER',
        'COMMUNITY_REPLY',
        'CONSULTATION',
        'EMERGENCY',
        'LOCATION_SHARE',
        'MESSAGE',
        'GROUP_INVITE',
        'CONTENT_REVIEW',
        'EPDS_RESULT'
    ));
```

**Sync action for `V1__init_schema.sql`: NONE — deliberately not synced.**

> **Corrected during implementation (2026-08-14).** The draft of this TDS called for syncing the
> canonical baseline. Inspection of `V1__init_schema.sql:1830-1835` shows it pins only the original
> **seven** values and does **not** contain `LOCATION_SHARE` — i.e. the immediately preceding
> precedent, `V20260811150000__allow_location_share_notifications.sql`, did not sync V1 either.
> The established pattern is that V1 remains the historical baseline and later migrations widen the
> constraint in sequence; a fresh database therefore still converges on the correct definition.
> Editing V1 would additionally aggravate the repo's known V1 checksum-drift failure
> (`ChecklistTemplateMigrationTest`). **V1 is left untouched.**

Version collision checked — no existing migration uses `V20260814120000`; the latest existing is
`V20260813220000`, so the new migration sorts last.

**No other schema change.** No new table, column, or index.

---

## 6. Dynamic Modeling

### 6.1 Happy Path

```
MOTHER            epds_screen        HealthMetricServiceImpl     EpdsFamilyNotificationService
  |  submit          |                        |                              |
  |----------------->| addMetric              |                              |
  |                  |----------------------->| validate + persist           |
  |                  |                        | audit HEALTH_METRIC_ADDED    |
  |                  |                        | publish EpdsScreeningCompleted
  |                  |<-- 200 MetricResponse --|                              |
  |<-- result shown -|                        |  == TX COMMIT ==              |
  |                  |                        |----------- @Async ---------->|
  |                  |                        |                              | resolve recipients
  |                  |                        |                              | filter on QUICK_NOTE_EPDS
  |                  |                        |                              | build msg (policy)
  |                  |                        |                              | send() per recipient
```

The mother's response is returned at commit; all notification work happens after, off-thread.

### 6.2 Error / Degradation Flows

| Condition | Behavior |
| --------- | -------- |
| Mother has no ACTIVE care group | Query returns empty → log at DEBUG, no notification, **no error** |
| No family member holds `quickNoteEpds` | Filter yields empty → log recipient count 0, no notification |
| `notificationService.send` throws for recipient *n* | Caught per-recipient; remaining recipients still processed (see below) |
| Whole listener throws | Top-level catch logs ERROR; EPDS observation already committed and unaffected |
| Recipient has no active device token | Existing `send()` behavior: record persisted with status `FAILED`, still visible in the in-app centre |

> **Per-recipient isolation.** The loop must catch inside the iteration, not only at method level —
> otherwise one recipient's FCM failure silently drops every later recipient. This is stricter than
> `FamilyNotificationService`, which only ever sends to one user. Pinned by TC-17.

### 6.3 Invariants

- **INV-1:** The set of notifications produced is a subset of `{ACCEPTED FAMILY members of the
  mother's ACTIVE groups holding QUICK_NOTES ∧ QUICK_NOTE_EPDS}`.
- **INV-2:** No outbound message contains `valueSecondary`, raw answers, or self-harm wording.
- **INV-3:** The mother never receives a notification about her own screening.
- **INV-4:** Exactly one notification per (recipient, screening) — including when the recipient is an
  ACCEPTED member of several care groups owned by the same mother (§5.5, TC-22).

---

## 7. Domain Event Catalog

### Published: `EpdsScreeningCompleted`

```java
public record EpdsScreeningCompleted(
        UUID observationId,
        UUID journeyId,
        UUID motherUserId,
        int totalScore,
        int question10Score,
        Instant occurredAt
) {}
```

| Field | Source | Note |
| ----- | ------ | ---- |
| `observationId` | `saved.getId()` | Audit correlation only; never sent to family |
| `motherUserId` | `userId` param of `addMetric` | Care-group owner key |
| `totalScore` | `normalized.valueNumeric()` | Family-visible in normal branch |
| `question10Score` | `normalized.valueSecondary()` | **Routing input only — never rendered** |

> `question10Score` is carried in the event because the escalation decision needs it. It is
> consumed as a boolean predicate (`> 0`) and must never reach a message string (INV-2).
> Null `valueSecondary` is coerced to `0` (treated as non-escalating).

**Consumed:** none.

---

## 8. Interface Specification

```java
// health/policy/EpdsSeverityPolicy.java
public final class EpdsSeverityPolicy {
    public enum Band { LOW, MONITOR, ASSESS }

    public static Band band(int totalScore);
    public static String bandLabel(Band band);          // Vietnamese label, §5.3
    public static boolean requiresEscalation(int question10Score);
    public static String familyTitle(int totalScore, int question10Score);
    public static String familyBody(int totalScore, int question10Score);
}
```

```java
// notification/service/impl/EpdsFamilyNotificationService.java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onEpdsScreeningCompleted(EpdsScreeningCompleted event);
```

```java
// family/repository/CareGroupMemberRepository.java
List<CareGroupMember> findAcceptedFamilyMembersForEpdsAlerts(@Param("ownerUserId") UUID ownerUserId);
```

---

## 9. API Specification

**No new or modified HTTP endpoint.** This module is event-driven and rides entirely on existing
contracts:

| Existing endpoint | Change |
| ----------------- | ------ |
| add-metric (EPDS submit) | None — request/response shape unchanged |
| `GET` my notifications | None — returns the new `EPDS_RESULT` type through the existing DTO |

The only client-visible contract change is the **new `type` string value `EPDS_RESULT`** appearing in
notification list responses. Clients that switch on `type` must handle it (§5.1 mobile changes);
clients that don't will fall through to their default rendering.

---

## 10. Error Codes

No new domain error codes. This module is non-interactive and never surfaces an error to a caller —
all failures are logged and swallowed by design (ADR-001, §6.2).

| Existing code | Relevance |
| ------------- | --------- |
| `METRIC-001` / `METRIC-002` | Raised by `addMetric` *before* the event is published; no notification occurs |

---

## 11. Implementation Plan (Step-by-Step)

| # | Step | Files |
| - | ---- | ----- |
| 1 | Add `EPDS_RESULT` to `NotificationType` | `notification/entity/NotificationType.java` |
| 2 | Create Flyway migration; sync `V1__init_schema.sql` | `db/migration/V20260814120000__...sql`, `V1__init_schema.sql` |
| 3 | Create `EpdsScreeningCompleted` event record | `health/event/` |
| 4 | Create `EpdsSeverityPolicy` (pure functions) | `health/policy/` |
| 5 | Add `findAcceptedFamilyMembersForEpdsAlerts` | `family/repository/CareGroupMemberRepository.java` |
| 6 | Publish the event from `addMetric`, guarded on `EPDS_SCORE` | `health/service/impl/HealthMetricServiceImpl.java` |
| 7 | Create `EpdsFamilyNotificationService` listener | `notification/service/impl/` |
| 8 | Mobile: handle `EPDS_RESULT` in both notification screens | 2 Dart files |
| 9 | Run backend + mobile test suites | — |

**Ordering constraint:** step 2 must be deployed before step 1 reaches production, otherwise the
first `EPDS_RESULT` insert violates `notification_records_type_check`.

### Impact analysis (CLAUDE.md mandatory pre-edit check)

| Target | Direction | Result |
| ------ | --------- | ------ |
| `HealthMetricServiceImpl.addMetric` | upstream | **LOW** — 1 direct caller, 1 module (`Health`), 0 affected processes |

No HIGH/CRITICAL impact found; no user warning required. `NotificationType` is an additive enum
change; `CareGroupMemberRepository` gains a new method without altering existing ones.

---

## 12. Rollback & Incident Runbook

```bash
# Step 1: Re-deploy the previous build.
# The old binary neither publishes nor consumes EpdsScreeningCompleted, so EPDS submission
# returns to pull-only family visibility with no data loss.

# Step 2: The migration does NOT need reverting.
# A widened CHECK constraint is backward compatible — the old binary simply never writes
# 'EPDS_RESULT'. Leave it in place.

# Step 3: Only if rows must be purged (e.g. the escalation wording is judged unsafe in
# production before clinician sign-off — see OPEN-1):
#   DELETE FROM notification_records WHERE type = 'EPDS_RESULT';
# Then, and only then, restore the 8-value constraint.
```

**Incident triggers requiring immediate rollback:**
- Any family-facing EPDS message found containing Question-10 wording or sub-score (INV-2 breach).
- Notifications delivered to a recipient lacking `quickNoteEpds` (INV-1 breach).
- EPDS submissions failing or slowing measurably after deploy (NFR-EPDS-N-01 / NFR-EPDS-N-03 breach).

---

## 13. Test Strategy

| Layer | Scope | Tooling |
| ----- | ----- | ------- |
| Unit | `EpdsSeverityPolicy` band boundaries and message-content constraints | JUnit 5 |
| Unit | `EpdsFamilyNotificationService` recipient filtering, consent gate, per-recipient isolation | JUnit 5 + Mockito |
| Unit | `HealthMetricServiceImpl` publishes only for `EPDS_SCORE` | JUnit 5 + Mockito |
| Integration | Migration applies; `EPDS_RESULT` insert satisfies the CHECK constraint | Testcontainers (Docker-gated) |
| Widget | Both notification screens render the new type | `flutter_test` |
| Regression | Existing `FamilyQuickNoteServiceTest` privacy test still green | `./mvnw test` |

Detailed cases live in `EpdsFamilyNotification_Test-Spec.md`; this section defines strategy only.

---

## 14. Verification Methods

**14.1 — Latency (NFR-EPDS-N-01, NFR-EPDS-N-04).** The synchronous addition to `addMetric` is one string compare
plus one `publishEvent` call. Verified by inspection plus the absence of regression in existing
add-metric tests. Recipient resolution costs `1 + 2N` queries (one recipient query, then a parent
and a child permission lookup per candidate row); all of it runs after commit on an async thread, so
it is off the mother's request path entirely.

**14.2 — Async boundary (NFR-EPDS-N-02).** Assert the listener method carries both `@Async` and
`@TransactionalEventListener(phase = AFTER_COMMIT)` (TC-15a). `@EnableAsync` is present on
`BackendApplication.java:11`, so `@Async` is honoured at runtime rather than silently ignored.

**14.3 — PII hygiene in logs (NFR-EPDS-N-05).**
```bash
# Expected pattern — counts and identifiers only:
#   "EPDS family notification dispatched careGroupId=<uuid> recipients=2 band=ASSESS"
# Forbidden in any log line: the total score, valueSecondary, answer payloads.
grep -rn "log\." src/main/java/com/carebridge/backend/notification/service/impl/EpdsFamilyNotificationService.java
```

**14.4 — Dart/Java threshold reconciliation (ADR-002).** Thresholds in
`EpdsSeverityPolicy.band()` must equal `epds_screen.dart:17-21`. Any change to one requires the
other; TC-03/TC-04/TC-05 pin the Java side at the exact boundaries 9/10/12/13.

---

## 15. API Verification Samples

```bash
# 1) Mother submits EPDS with a low total and Question 10 = 0.
#    Expect: 201; eligible family receive "Điểm 8/30 — Nguy cơ hiện tại thấp".

# 2) Mother submits with Question 10 = 3 and a LOW total (e.g. 8).
#    Expect: family receive the escalation title/body from §5.3
#            with NO number and NO self-harm wording. This is the ADR-003 case.

# 3) Family member with quickNoteEpds = false.
#    Expect: zero notification_records rows for that user.
curl -s "$API/api/v1/notifications" -H "Authorization: Bearer $FAMILY2_TOKEN" | jq '[.content[]|select(.type=="EPDS_RESULT")]|length'
# Expected: 0

# 4) Non-EPDS metric (e.g. BMI) submitted.
#    Expect: no EPDS_RESULT row created at all.
```

---

## 16. Authorization Matrix

| Role | Receives `EPDS_RESULT`? | Condition |
| ---- | ----------------------- | --------- |
| MOTHER (subject / group owner) | ❌ Never | Excluded by `u.role = 'FAMILY'` join |
| FAMILY, ACCEPTED, `quickNotes` ∧ `quickNoteEpds` | ✅ Yes | The only eligible class |
| FAMILY, ACCEPTED, missing either flag | ❌ No | Consent gate (BR-CONSENT-EPDS-001) |
| FAMILY, PENDING / DECLINED / REVOKED | ❌ No | `invitation_status = 'ACCEPTED'` filter |
| FAMILY, disabled or locked account | ❌ No | `u.enabled` / `u.locked` filter |
| EXPERT / PARTNER / MODERATOR / ADMIN | ❌ No | Not FAMILY; no path exists |

---

## 17. AI Prompt Constraints (CASE 2.0)

**Not applicable — no AI/LLM component.** This module performs deterministic threshold arithmetic
and static message assembly. No model call, no prompt, no generated text reaches a user.

The CASE 2.0 *anti-pattern* discipline still applies to the AI-assisted authoring of the code and
tests; that is tracked in `EpdsFamilyNotification_Test-Spec.md` §8.

---

## OPEN ITEMS (tracked — none may silently become decisions)

| ID | Item | Status | Owner |
| -- | ---- | ------ | ----- |
| **OPEN-1** | ADR-003 trade-off. Two things need explicit approver + DPO sign-off: **(a)** the escalation wording has not been clinician-reviewed; **(b)** the redaction is *partial* — a recipient can cross-reference the escalation message against the total score still visible on the family history channel and thereby **infer** a positive Question 10. What is being accepted is escalation without explicit disclosure, **not** escalation without inferability. | **Open — blocking production, not blocking implementation** | Approver / DPO |
| **OPEN-2** | Best-effort delivery (ADR-001): a crash between commit and dispatch loses the notification with no retry. Should this move to the outbox-job pattern used by emergency alerts? | Open — deferred to v2 | HuyND |
| **OPEN-3** | REQ-EPDS-N-01/02 come from a user request, not an approved SRS clause. `UC-32` covers EPDS screening generally but does not specify family notification. Needs an SRS/RTM entry. | Open | HuyND |
| **OPEN-4** | `notification_records.care_group_id` is left NULL (ADR-004), matching `GROUP_INVITE`. Revisit if group-scoped notification filtering is introduced. | Open — accepted for v1 | HuyND |
| **OPEN-6** | **TC-13 and TC-14b were not delivered** (Docker unavailable). The native recipient query has never run against PostgreSQL, and migration `V20260814120000` has never been applied. Both must be executed before production. Note this also leaves the query's `ORDER BY care_group_id ASC` unverified — TC-22's determinism assertion exercises `LinkedHashSet` insertion order over a *stubbed* list, so the deterministic `referenceId` guarantee rests on the SQL ordering that no test has yet executed. See Test-Spec §5.2. | **Open — blocking production** | HuyND |
| **OPEN-5** | UC numbering conflict: SRS `Report3_Functional_Specifications.md:658` labels UC-32 "Manage EPDS Screening", while `04_Implement/UC32_UpdateBabyProfile/` uses UC32 for a different feature. This TDS therefore uses a feature-named folder rather than asserting a UC number. | Open | HuyND |

---

## APPENDIX

### A. Evidence Index

| Claim | Evidence |
| ----- | -------- |
| EPDS submits through `addMetric` with `EPDS_SCORE` | `epds_screen.dart:403-417` |
| Mobile sends total in `valueNumeric`, Q10 in `valueSecondary` | `epds_screen.dart:407-408` |
| Question 10 is the self-harm item | `epds_screen.dart:324` |
| `epdsGuidance` branches on Q10 and names suicidal ideation | `epds_screen.dart:23-26` |
| Band thresholds 13 / 10 | `epds_screen.dart:17-21` |
| Family EPDS history is redacted (`valueSecondary` null) | `FamilyQuickNoteServiceTest.java:90-116` |
| EPDS consent flag exists and is checked | `PermissionFlag.java:19`, `FamilyQuickNoteService.java:105`, `FamilyDashboardService.java:327` |
| Eligible-family query shape and owner keying | `CareGroupMemberRepository.java:60-76` |
| Mother is the care-group owner | `CareGroupServiceImpl.java:120` |
| Listener precedent (`@Async` + AFTER_COMMIT + try/catch) | `FamilyNotificationService.java:30-71` |
| Event-publish precedent in the same service | `HealthMetricServiceImpl.java:77` |
| `notification_records_type_check` exists; migration pattern | `V20260811150000__allow_location_share_notifications.sql` |
| Notification preferences default to enabled | `NotificationPreferenceServiceImpl.java:115` |
| Mobile switches on notification `type` string | `notifications_screen.dart:438,481`; `notification_center_screen.dart:253,416` |
| `send()` does not populate `careGroupId` | `NotificationServiceImpl.send` body |
| Disclaimer wording | `HealthMetricServiceImpl.java:45` |

### B. Legacy files used as evidence only

- `02_Requirements/SRS/Report3_Functional_Specifications.md` (UC-32 entry) — context for OPEN-3/OPEN-5;
  not used as a behavioral oracle.
