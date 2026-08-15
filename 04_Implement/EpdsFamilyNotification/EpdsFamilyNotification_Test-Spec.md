# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test Specification — EpdsFamilyNotification: Deliver EPDS Screening Results to Care-Group Family Accounts

**Document ID:** `CB-EPDS-TEST-001`
**Version:** `1.0`
**Date:** `2026-08-14`
**Status:** `Implemented — 2026-08-14 (23/25 TC green; TC-13 & TC-14b not delivered — Docker unavailable)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(Sensitive-PII — maternal mental-health results are distributed to third-party FAMILY accounts; see TDS §5.4 and OPEN-1)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/EpdsFamilyNotification/EpdsFamilyNotification_TDS.md` (`CB-EPDS-IMP-001`) — primary oracle
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyQuickNoteServiceTest.java:90-116` — existing privacy oracle (BR-SAFETY-EPDS-001)
- `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/epds_screen.dart:17-32` — band-threshold oracle
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/repository/CareGroupMemberRepository.java:60-76` — recipient-eligibility oracle
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260811150000__allow_location_share_notifications.sql` — migration-pattern oracle
- `CLAUDE.md` — BR-SAFETY ("Do not hide urgent-care escalation"; preserve RBAC, consent scope, audit)
- Code under test: `health/policy/EpdsSeverityPolicy.java` (NEW), `notification/service/impl/EpdsFamilyNotificationService.java` (NEW), `health/service/impl/HealthMetricServiceImpl.java` (MODIFIED), `family/service/FamilyQuickNoteService.java` (must remain untouched)

> **TDD convention:** This document describes test cases BEFORE any production code is written.
> Mandatory order: write test → run → confirm FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Never mark a test ✅ unless the suite is green.
> No real PII in test data — SYNTHETIC data only.
>
> **Test commands:**
> - Backend full: `./mvnw test`
> - Backend targeted: `./mvnw test -Dtest=EpdsSeverityPolicyTest`, `-Dtest=EpdsFamilyNotificationServiceTest`, `-Dtest=HealthMetricServiceEpdsEventTest`
> - Mobile: `flutter analyze`, `flutter test`

---

## CHANGELOG

| Date       | Performed by | Change description |
| ---------- | ------------ | ------------------ |
| 2026-08-14 | AI Agent     | Initial creation — Test-Spec for EpdsFamilyNotification (Draft). 25 test cases (TC-01..TC-23, with TC-14 and TC-15 each split a/b), 10 CRITICAL (privacy/consent/escalation/duplicate-suppression). TC-13 classified as Docker-gated integration (native query, not H2-safe); TC-15a carries a documented Red Gate exception (structural assertion cannot fail against the stub). |

---

## TABLE OF CONTENTS

1. Module Information
2. Logic Issues Resolved
3. Test Design Specification
4. Test Case Specification
5. Red-Green-Refactor Tracker
6. Entry / Exit Criteria
7. Rollback Plan
8. CASE 2.0 Anti-Pattern Detection

---

## 1. Module Information

| Field | Value |
| ----- | ----- |
| **Feature** | EpdsFamilyNotification |
| **Module** | `health` (producer) + `notification` (consumer) + `family` (recipient resolution) |
| **Priority** | High — safety-adjacent |
| **Data classification** | **Sensitive-PII** (maternal mental-health screening) |
| **Compliance scope** | PDPA / Luật 91/2025 — consent scope and purpose limitation |
| **Platforms** | Backend (Java 21 / Spring Boot), Mobile (Flutter) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
| ----- | ----- |
| **Constraints injected** | BR-SAFETY-EPDS-001 (no Q10 exposure), BR-SAFETY-EPDS-002 (no hidden escalation), BR-CONSENT-EPDS-001 (`QUICK_NOTE_EPDS` gate), TDS ADR-003 |
| **Model** | Claude Opus 5 |
| **Trust level** | **Low — human verification mandatory.** Every CRITICAL case encodes a privacy or safety rule; each must be read by a human reviewer, not accepted on a green run. |

---

## 2. Logic Issues Resolved

| # | Discrepancy | Resolution | Authority |
| - | ----------- | ---------- | --------- |
| L1 | User asked for "score **and** the comment/nhận xét" to be sent. The mobile `epdsGuidance()` text branches on Question 10 and names suicidal ideation — sending it verbatim contradicts the existing family-redaction test. | Family receives the total score + the band derived from **total only**. `epdsGuidance()` is never transmitted. Q10-positive triggers a category-anonymised escalation message. | User decision D1, 2026-08-14; TDS ADR-003 |
| L2 | CLAUDE.md forbids hiding urgent-care escalation, but L1's redaction could hide a Q10-positive result behind a "low risk" band. | Q10 > 0 replaces the message entirely with an escalation that omits the numeric total (so a low total cannot be inferred as the Q10 signal). | TDS ADR-003, §5.3 |
| L3 | Should every ACCEPTED family member receive the result, or only consented ones? | Only members holding `quickNotes` ∧ `quickNoteEpds`. Sending to all would create a consent-bypass channel around the mother's own setting. | User decision D2, 2026-08-14 |
| L4 | `FamilyDashboardService` uses an `owner ||` permission bypass. Applying it here would notify the mother about her own screening. | Bypass deliberately **not** applied; the `role = 'FAMILY'` join already excludes the owner. | TDS §5.2 |
| L5 | `NotificationRecord.careGroupId` exists but `SendNotificationRequest` cannot carry it without breaking 8+ call sites. | Follow the `GROUP_INVITE` precedent: `referenceId = careGroupId`, `referenceType = "CARE_GROUP"`. | TDS ADR-004 |
| L6 | `valueSecondary` may be null for legacy/partial EPDS rows. | Null coerced to `0` — treated as non-escalating. Pinned by TC-19. | TDS §7 |

---

## 3. Test Design Specification

### 3.1 Test Basis

| Source | Contributes |
| ------ | ----------- |
| TDS §5.3 | Band thresholds, exact message templates, forbidden-substring list |
| TDS §5.5 + `CareGroupMemberRepository:60-76` | Recipient eligibility predicate |
| TDS §6.3 | Invariants INV-1..INV-4 |
| `FamilyQuickNoteServiceTest:90` | Pre-existing privacy contract that must stay green |
| `epds_screen.dart:17-21` | Band-threshold values |

### 3.2 Risk-Based Scope

| Risk | Severity | Coverage |
| ---- | -------- | -------- |
| Question-10 / self-harm disclosure to family | **CRITICAL** | TC-06, TC-07 |
| Urgent result silently not escalated | **CRITICAL** | TC-08 |
| Consent bypass (notify without `quickNoteEpds`) | **CRITICAL** | TC-09, TC-10, TC-11 |
| Mother notified about herself | **CRITICAL** | TC-12 |
| Notification failure rolls back the EPDS write | **CRITICAL** | TC-15b |
| Duplicate notifications when a member belongs to two of the mother's groups | **CRITICAL** | TC-22 |
| De-duplication silently suppresses a consented recipient | **CRITICAL** | TC-23 |
| Wrong band shown | High | TC-03, TC-04, TC-05 |
| One recipient's failure drops the rest | High | TC-17 |
| Event fires for non-EPDS metrics | Medium | TC-18 |

### 3.3 Test Techniques

| Technique | Applied to | Rationale |
| --------- | ---------- | --------- |
| Boundary value analysis | Band thresholds at 9/10/12/13 | Two thresholds, off-by-one is the likely defect |
| Decision table | (band × Q10) → message | 3 bands × 2 Q10 states; escalation must dominate all bands |
| Negative/forbidden-content assertion | Escalation message | Cannot prove absence by example — assert the forbidden-substring set explicitly |
| State-based filtering | Invitation status, enabled/locked, role | Mirrors the SQL predicate |
| Fault injection | `send()` throwing | Verifies per-recipient isolation and TX independence |

### 3.4 Test Data Requirements

- All entities built via the Props Isolation factories in §4.1 — no inline literals in test bodies.
- Synthetic UUIDs only; no real user identifiers.
- Clock: `measuredAt` / `occurredAt` pinned to a fixed `Instant` constant.
- `NotificationService`, `CareGroupAuthorizationPolicy`, `CareGroupMemberRepository` mocked with Mockito.
- Testcontainers cases (TC-14b) are Docker-gated and skip cleanly when Docker is unavailable —
  consistent with the ~77 pre-existing Docker-gated suites in this repo.

### 3.5 Applicability Matrix

| Layer | Backend | Mobile |
| ----- | ------- | ------ |
| Unit | ✅ TC-01..TC-12, TC-14a, TC-15a/b, TC-16..TC-19, TC-22, TC-23 | ➖ |
| Integration (Docker-gated) | ✅ TC-13 (native query), TC-14b (migration) | ➖ |
| Widget | ➖ | ✅ TC-20, TC-21 |
| Security/privacy | ✅ TC-06, TC-07, TC-09..TC-12, TC-16, TC-22, TC-23 | ➖ |
| E2E | ➖ Deferred — no automated cross-device harness exists in this repo | ➖ |

---

## 4. Test Case Specification

### 4.1 Props Isolation Boilerplate

```java
// src/test/java/com/carebridge/backend/notification/EpdsNotificationProps.java
final class EpdsNotificationProps {
    static final Instant FIXED_NOW = Instant.parse("2026-08-14T09:00:00Z");
    static final UUID MOTHER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID GROUP_ID    = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID FAMILY_1    = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID FAMILY_2    = UUID.fromString("00000000-0000-0000-0000-000000000102");

    static EpdsScreeningCompleted makeEvent(int total, int q10) { /* fixed ids + FIXED_NOW */ }

    static CareGroupMember makeFamilyMember(UUID userId) { /* ACCEPTED, GROUP_ID */ }

    // Permission stubbing helper — mirrors the production two-flag gate
    static void allowEpds(CareGroupAuthorizationPolicy policy, UUID userId, boolean parent, boolean child) { }
}
```

> **Rule:** every test constructs data only through `makeEvent` / `makeFamilyMember` / `allowEpds`.
> A test containing an inline `new CareGroupMember(...)` fails review.

### 4.2 Individual Test Cases

**Legend — Severity:** 🔴 CRITICAL (privacy/safety/consent) · 🟠 High · 🟡 Medium
**Initial status for every case:** `🔴 Not written`

---

#### TC-01 — Eligible family member receives the EPDS notification
**Severity:** 🟠 High · **Oracle:** TDS §5.2, REQ-EPDS-N-01 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Arrange:** one ACCEPTED family member with `quickNotes = true`, `quickNoteEpds = true`; event total = 8, q10 = 0.
- **Act:** `onEpdsScreeningCompleted(event)`.
- **Assert:** `notificationService.send` called exactly once; `recipientUserId = FAMILY_1`; `type = EPDS_RESULT`; `referenceId = GROUP_ID`; `referenceType = "CARE_GROUP"`.
- **Failure signature:** `send` never called, or called with the wrong recipient/type.

#### TC-02 — Two eligible members each receive exactly one notification
**Severity:** 🟠 High · **Oracle:** TDS INV-4 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Assert:** `send` called exactly twice, once per distinct recipient; no duplicates.

#### TC-03 — Band boundary: score 13 → "Cần được đánh giá chuyên sâu"
**Severity:** 🟠 High · **Oracle:** `epds_screen.dart:18` · **File:** `EpdsSeverityPolicyTest.java`
- **Assert:** `band(13) == ASSESS`; `band(12) != ASSESS`. Body contains `Điểm 13/30` and the label.

#### TC-04 — Band boundary: score 10 → "Cần theo dõi và sàng lọc lại"
**Severity:** 🟠 High · **Oracle:** `epds_screen.dart:19` · **File:** `EpdsSeverityPolicyTest.java`
- **Assert:** `band(10) == MONITOR`; `band(9) == LOW`.

#### TC-05 — Band: score 0 and 9 → "Nguy cơ hiện tại thấp"
**Severity:** 🟡 Medium · **Oracle:** `epds_screen.dart:20` · **File:** `EpdsSeverityPolicyTest.java`

#### TC-06 — 🔴 Escalation message contains no self-harm wording and no sub-score
**Severity:** 🔴 CRITICAL · **Oracle:** BR-SAFETY-EPDS-001, TDS §5.3 · **File:** `EpdsSeverityPolicyTest.java`
- **Arrange:** total = 8, q10 = 3.
- **Act:** `familyTitle(8, 3)` + `familyBody(8, 3)`.
- **Assert (case-insensitive, on title **and** body):** contains none of `tự hại`, `tự sát`, `câu 10`, `question 10`, `self-harm`, `suicid`; and contains neither `"3"` nor `"8"` (no sub-score, no total — TDS §5.3 hard constraints).
- **Failure signature:** any forbidden substring present → **immediate stop-ship**.

#### TC-07 — 🔴 `epdsGuidance` text never appears in any family message
**Severity:** 🔴 CRITICAL · **Oracle:** TDS ADR-003 · **File:** `EpdsSeverityPolicyTest.java`
- **Assert:** across the full matrix (total ∈ {0, 9, 10, 12, 13, 30} × q10 ∈ {0, 1, 2, 3}), no produced message equals or contains the Dart guidance strings, notably `Cần đánh giá sức khỏe tâm thần ngay và hỗ trợ khẩn nếu có ý nghĩ tự sát.`
- **Rationale:** guards against a future "just forward the guidance" regression.

#### TC-08 — 🔴 Q10 > 0 with a LOW total still escalates
**Severity:** 🔴 CRITICAL · **Oracle:** BR-SAFETY-EPDS-002 (CLAUDE.md) · **File:** `EpdsSeverityPolicyTest.java`
- **Arrange:** total = 8 (LOW band), q10 = 1.
- **Assert:** `requiresEscalation(1)` is true; title equals the escalation title, **not** the normal title; body differs from the LOW-band normal body.
- **Failure signature:** family receives "Nguy cơ hiện tại thấp" while Q10 is positive — the exact hidden-escalation defect.

#### TC-09 — 🔴 Member without `quickNoteEpds` receives nothing
**Severity:** 🔴 CRITICAL · **Oracle:** BR-CONSENT-EPDS-001 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Arrange:** `quickNotes = true`, `quickNoteEpds = false`.
- **Assert:** `send` never called.

#### TC-10 — 🔴 Member with parent `quickNotes = false` receives nothing
**Severity:** 🔴 CRITICAL · **Oracle:** TDS §5.2 (two-flag gate) · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Arrange:** `quickNotes = false`, `quickNoteEpds = true` (child granted, parent revoked).
- **Assert:** `send` never called — revoking the parent must disable the child.

#### TC-11 — 🔴 Mixed group: only the consented member is notified
**Severity:** 🔴 CRITICAL · **Oracle:** TDS INV-1 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Arrange:** FAMILY_1 consented, FAMILY_2 not.
- **Assert:** `send` called exactly once, and its `recipientUserId` is FAMILY_1.

#### TC-12 — 🔴 Mother is never a recipient of her own screening
**Severity:** 🔴 CRITICAL · **Oracle:** TDS INV-3 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Assert:** no `send` call carries `recipientUserId = MOTHER_ID`, even when the repository stub also returns a member row bearing the mother's id.

#### TC-13 — Non-eligible states yield no recipients (integration, Docker-gated)
**Severity:** 🟠 High · **Oracle:** `CareGroupMemberRepository:60-76` · **File:** `CareGroupMemberRepositoryEpdsIT.java`
- **Assert:** the query excludes PENDING/DECLINED invitations, non-FAMILY roles, disabled and locked accounts, and non-ACTIVE groups. Also asserts the query keys on `owner_user_id` (pins the TDS §5.5 ownership assumption).
- **⚠️ Integration, not unit.** This is a `nativeQuery = true` PostgreSQL query. It must run against a real Postgres via Testcontainers — **do not attempt it on H2**, whose dialect will not faithfully reproduce the joins and boolean predicates. Docker-gated, skips cleanly when Docker is unavailable.

#### TC-14a — Every send is audited
**Severity:** 🟡 Medium · **Oracle:** NFR-EPDS-N-01 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Assert:** dispatch goes through `NotificationService.send`, which already audits `NOTIFICATION_SENT` / `NOTIFICATION_FAILED` — i.e. the listener must not bypass it by writing `NotificationRecord` directly.

#### TC-14b — Migration admits `EPDS_RESULT` (Docker-gated)
**Severity:** 🟠 High · **Oracle:** TDS §5.6 · **File:** `EpdsNotificationMigrationIT.java`
- **Assert:** after migration, inserting a `notification_records` row with `type = 'EPDS_RESULT'` succeeds; an unknown type still violates the constraint.

#### TC-15a — Listener is annotated `@Async` + `AFTER_COMMIT` (structural)
**Severity:** 🟠 High · **Oracle:** NFR-EPDS-N-02, ADR-001 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Assert:** the handler method carries `@Async` **and** `@TransactionalEventListener(phase = AFTER_COMMIT)` (reflection assertion).
- **⚠️ Documented Red Gate exception:** this case **passes against the red-phase stub**, because the stub already carries the annotations specified in TDS §8. It is structural, not behavioral, so it cannot fail before implementation. Its Red Gate row is recorded as `PASS (documented exception)` rather than `🔴 FAIL` — the same treatment as the SEC exception recorded in `04_Implement/TriageRedFlagPreScreen`. It is retained because annotation loss is a silent, high-impact regression (`@Async` dropped ⇒ NFR-EPDS-N-02 unmet with no test failure anywhere else).
- **Prerequisite verified:** `@EnableAsync` is present on `BackendApplication.java:11`, so `@Async` is honoured at runtime.

#### TC-15b — 🔴 Listener failure does not propagate or roll back the EPDS write
**Severity:** 🔴 CRITICAL · **Oracle:** NFR-EPDS-N-03, ADR-001 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Arrange:** `notificationService.send` stubbed to throw.
- **Act:** `onEpdsScreeningCompleted(event)`.
- **Assert:** the method completes without propagating; the exception is logged. Genuinely fails against the stub.

#### TC-16 — Log lines carry no score and no Q10
**Severity:** 🟠 High · **Oracle:** NFR-EPDS-N-05 · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Assert:** captured log output contains the recipient count and `careGroupId`, and does not contain the total score value or `valueSecondary`.

#### TC-17 — One recipient's failure does not drop the others
**Severity:** 🟠 High · **Oracle:** TDS §6.2 per-recipient isolation · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Arrange:** two eligible members; `send` throws for the first.
- **Assert:** `send` is still invoked for the second.
- **Failure signature:** only one invocation — proves the catch sits outside the loop.

#### TC-18 — Non-EPDS metrics publish no event
**Severity:** 🟡 Medium · **Oracle:** TDS §5.1 step 6 · **File:** `HealthMetricServiceEpdsEventTest.java`
- **Assert:** `addMetric` with `BMI` publishes no `EpdsScreeningCompleted`; with `EPDS_SCORE` it publishes exactly one carrying the correct total and q10.

#### TC-19 — Null `valueSecondary` is treated as non-escalating
**Severity:** 🟡 Medium · **Oracle:** L6, TDS §7 · **File:** `HealthMetricServiceEpdsEventTest.java`
- **Assert:** a null `valueSecondary` maps to `question10Score = 0`; no NPE; the normal branch is used.

#### TC-20 — Mobile: `EPDS_RESULT` renders with a dedicated icon/label
**Severity:** 🟠 High · **Oracle:** TDS §5.1 · **File:** `test/features/notification/notifications_screen_test.dart`
- **Assert:** a notification with `type = 'EPDS_RESULT'` renders its title/body and does not fall through to the unknown-type default.

#### TC-21 — Mobile: notification centre handles `EPDS_RESULT`
**Severity:** 🟡 Medium · **Oracle:** TDS §5.1 · **File:** `test/features/notification/notification_center_screen_test.dart`

#### TC-22 — 🔴 Family member in two of the mother's groups receives exactly one notification
**Severity:** 🔴 CRITICAL · **Oracle:** TDS INV-4, §5.5 duplicate-recipient hazard · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Arrange:** the repository stub returns FAMILY_1 **twice** — once for GROUP_ID and once for a second ACTIVE group — both with full consent. (Realistic: group uniqueness is only on `(owner, groupName)`, `CareGroupServiceImpl:99`.)
- **Act:** `onEpdsScreeningCompleted(event)`.
- **Assert:** `send` invoked **exactly once** for FAMILY_1; `referenceId` equals the first group in `care_group_id ASC` order (deterministic across runs).
- **Failure signature:** two invocations — the duplicate-notification defect that the ungrouped query produces by default.

#### TC-23 — 🔴 Consent is evaluated before de-duplication
**Severity:** 🔴 CRITICAL · **Oracle:** TDS §5.2 step-order note · **File:** `EpdsFamilyNotificationServiceTest.java`
- **Arrange:** FAMILY_1 appears in two groups — **not** consented in the first (`care_group_id ASC` order), **consented** in the second.
- **Assert:** `send` is invoked exactly once for FAMILY_1, carrying the **consented** group's `referenceId`.
- **Failure signature:** zero invocations — proves de-duplication ran first, collapsed to the non-consented row, and silently suppressed a notification the mother had authorised.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----- | --------- | ---------------- | ----------------- | ---------------- |
| TC-01 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-02 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-03 | `EpdsSeverityPolicyTest` | [x] | 🟢 Passed | |
| TC-04 | `EpdsSeverityPolicyTest` | [x] | 🟢 Passed | |
| TC-05 | `EpdsSeverityPolicyTest` | [x] | 🟢 Passed | |
| TC-06 | `EpdsSeverityPolicyTest` | [x] | 🟢 Passed | |
| TC-07 | `EpdsSeverityPolicyTest` | [x] | 🟢 Passed | |
| TC-08 | `EpdsSeverityPolicyTest` | [x] | 🟢 Passed | |
| TC-09 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-10 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-11 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-12 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-13 | `CareGroupMemberRepositoryEpdsIT` | ❌ **NOT WRITTEN** | ❌ **NOT RUN** | Docker unavailable — see §5.2 |
| TC-14a | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-14b | `EpdsNotificationMigrationIT` | ❌ **NOT WRITTEN** | ❌ **NOT RUN** | Docker unavailable — see §5.2 |
| TC-15a | `EpdsFamilyNotificationServiceTest` | ➖ documented exception | 🟢 Passed | structural assertion |
| TC-15b | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-16 | `EpdsFamilyNotificationServiceTest` | ⚠️ post-impl (mutation-proven) | 🟢 Passed | real log-hygiene assertion; replaced a mis-scoped test |
| TC-24 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | renumbered from the original TC-16 slot |
| TC-25 | `epds_result_notification_display_test.dart` | ⚠️ post-impl (scope change) | 🟢 Passed | added 2026-08-14 on user request: detail screen shows no "Xem chi tiết" for `EPDS_RESULT`; paired assertion confirms other types keep theirs |
| TC-17 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-18 | `HealthMetricServiceEpdsEventTest` | [x] positive half; ➖ negative half | 🟢 Passed | see Red Gate note |
| TC-19 | `HealthMetricServiceEpdsEventTest` | [x] | 🟢 Passed | |
| TC-20 | `epds_result_notification_display_test.dart` | [x] | 🟢 Passed | mapping extracted to `notification_type_display.dart` to make it testable |
| TC-21 | `epds_result_notification_display_test.dart` | [x] | 🟢 Passed | |
| TC-22 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |
| TC-23 | `EpdsFamilyNotificationServiceTest` | [x] | 🟢 Passed | |

### 5.2 Outstanding — integration cases not delivered

**TC-13 and TC-14b were not written.** Docker is unavailable in this environment
(`docker info` fails), and the repo's Testcontainers base class
`AbstractPostgresIntegrationTest` cannot initialise without it — the full-suite run showed 159
`NoClassDefFoundError` errors from exactly this cause, all pre-existing. Writing these two cases
without the ability to execute even once would produce untested test code and a false Red Gate
entry.

**Consequence — what is therefore unverified:**
- The native SQL in `findAcceptedFamilyMembersForEpdsAlerts` has never been executed against
  PostgreSQL. Its eligibility predicate is verified only by inspection against the
  `findAcceptedFamilyMembersForEmergencyAlerts` precedent it was copied from.
- Migration `V20260814120000` has never been applied. The `EPDS_RESULT` insert path is unproven;
  a typo in the constraint would surface only at runtime.

Both must be run before this feature is considered production-ready. Recorded in TDS OPEN-6.

### 5.1 Red Gate Verification

Red-phase stubs must throw:
```java
throw new UnsupportedOperationException("Not implemented — Red Phase stub");
```

| TC ID | Expected | Actual |
| ----- | -------- | ------ |
| TC-01 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-02 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-03 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-04 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-05 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-06 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-07 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-08 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-09 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-10 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-11 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-12 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-13 | 🔴 FAIL | ❌ **NOT RUN — test never written** (Docker unavailable, §5.2) |
| TC-14a | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-14b | 🔴 FAIL | ❌ **NOT RUN — test never written** (Docker unavailable, §5.2) |
| TC-15a | ⚪ PASS *(documented structural exception)* | ☐ FAIL ☑ PASS *(as documented)* |
| TC-15b | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-16 | 🔴 FAIL | ⚠️ **Red Gate not observed — written post-implementation.** Non-vacuity proven by mutation instead (see note) |
| TC-17 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-18 *(positive half)* | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-18 *(negative half)* | ⚪ PASS *(documented exception)* | ☐ FAIL ☑ PASS *(as documented)* |
| TC-19 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-20 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-21 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-22 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-23 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| TC-24 | 🔴 FAIL | ☑ FAIL ☐ PASS |

> **TC-16 correction (2026-08-14).** The test originally shipped in the TC-16 slot
> (`noRecipientsResolvesQuietlyWithoutSending`) asserted no-throw and `never().send()` — it did not
> test log hygiene at all, so NFR-EPDS-N-05 was **unverified while the tracker claimed coverage**.
> This was caught in review, not by the suite. The empty-recipient test was renumbered **TC-24**, and
> a real TC-16 (`logsCarryCountsAndIdsButNeitherScoreNorQuestion10`) was written using a Logback
> `ListAppender`: it dispatches a screening with total=27/Q10=3, asserts the captured log contains
> `recipients=2` and the `careGroupId`, and asserts it contains neither the score `27` nor any
> Question-10 marker.
>
> **Honest Red Gate status of the two cases:**
> - **TC-24** (the renumbered empty-recipient test) *was* part of the original Red run and failed
>   correctly against the stub — it is one of the 17 errors.
> - **TC-16** (the new log-hygiene test) was written **after** the implementation existed, so it was
>   never executed against the Red-phase stub. Claiming otherwise would be fabricated evidence.
>   Because a "does not contain" assertion is exactly the vacuous-pass shape AP-01 warns about, its
>   teeth were proven by **mutation** instead: `log.info(...)` was temporarily changed to append
>   `score={}`, the test was re-run and **failed** with
>   `[total score must not appear in logs] ... expected not to contain "27"`, and the mutation was
>   then reverted. That is weaker than a true Red Gate (it does not prove the test predated the
>   code) but it does prove the assertion detects the exact leak NFR-EPDS-N-05 forbids.

**Tất cả FAIL (trừ 2 ngoại lệ có ghi nhận bên dưới)?** `[x] Yes` `[ ] No`

**Red Gate run — actual result (2026-08-14):** `Tests run: 23, Failures: 4, Errors: 17` → **21/23 FAIL**.

Two cases passed in the Red phase. Both are documented exceptions, not silent passes:

| TC | Test method | Why it could not fail against the stub |
| -- | ----------- | -------------------------------------- |
| TC-15a | `listenerIsAsyncAndAfterCommit` | Structural/reflection assertion. The stub already carries `@Async` and `@TransactionalEventListener` per TDS §8, so the annotations are present before any behaviour exists. Predicted in this spec before the run. |
| TC-18 (negative half) | `nonEpdsMetricPublishesNoEpdsEvent` | Asserts that a non-EPDS metric publishes **no** EPDS event. Before implementation `addMetric` published nothing at all, so the assertion held trivially. **Not predicted in the original draft — discovered during the actual Red run and recorded here rather than papered over.** Its value is regression protection: it now guards against an over-broad publish guard. Its positive counterpart (`epdsSubmissionPublishesEventWithScoreAndQuestion10`) did fail correctly in the Red phase. |

> **Red Gate rule:** a test that would PASS against an empty/throw/no-op implementation fails the
> gate and must be rewritten. TC-06 and TC-07 need particular care — an assertion that merely checks
> "does not contain X" can pass trivially against an empty string. Both must first assert the message
> is non-blank and carries the expected escalation title, *then* assert the forbidden set is absent.

---

## 6. Entry / Exit Criteria

### 6.1 Entry Criteria
- [ ] TDS `CB-EPDS-IMP-001` Status = `Approved`
- [ ] This Test-Spec Status = `Approved`
- [ ] OPEN-1 (ADR-003 trade-off) acknowledged by the approver — implementation may proceed while production sign-off remains pending
- [ ] Red-phase stubs exist and throw `UnsupportedOperationException`

### 6.2 Exit Criteria (Definition of Done)
- [ ] All 27 test cases pass — **NOT MET: 25/27.** TC-13 and TC-14b were not written (Docker unavailable, §5.2). Backend feature suite: `Tests run: 24, Failures: 0, Errors: 0`; mobile notification suite: **22/22**
- [x] All 10 CRITICAL cases (TC-06, TC-07, TC-08, TC-09, TC-10, TC-11, TC-12, TC-15b, TC-22, TC-23) pass — ⚠️ human review of these still **pending**; they were observed green, which this spec explicitly says is not sufficient
- [x] TC-15a's Red Gate exception is recorded, not silently marked FAIL — and a second, unpredicted exception (TC-18 negative half) was discovered in the actual run and recorded rather than hidden
- [x] Red Gate verified — 21/23 failed before implementation; the 2 that passed are documented above
- [x] Props Isolation factories used (`EpdsNotificationProps`); no inline entity construction in test bodies
- [x] `FamilyQuickNoteServiceTest.epdsAnswerPayloadIsNotExposedToFamilyHistory` still green (no regression)
- [x] `./mvnw test` shows no new failures — full run `Tests run: 4306, Failures: 13, Errors: 159`; **all 13 failures reproduced identically on a clean `git stash` baseline** (`Tests run: 32, Failures: 13`), and all 159 errors are Docker-off Testcontainers initialisation
- [x] `dart analyze lib` clean for the notification feature (10 pre-existing `info` items elsewhere). ⚠️ `flutter analyze` could not run — the analysis server crashes with exit 255 in this environment, unrelated to these changes
- [x] `flutter test test/features/notification/` → **20/20 pass**. ⚠️ The **full** mobile suite could not be completed: it hangs indefinitely on `test/features/home/family_home_shell_test.dart` (reproduced twice, ~10 min each with no progress past `+1080 -31`). At the point it hung, 31 tests had failed. The two failures touching a file this change refactored (`consultation_request_profile_notification_test.dart`, which imports `notification_center_screen.dart`) were **re-run on a clean `git stash` baseline and fail identically there** — pre-existing, not caused by this change. The remaining 29 were not individually bisected
- [ ] Migration applied — **NOT MET**, Docker unavailable (§5.2 / TDS OPEN-6). `V1__init_schema.sql` sync is **not applicable** (TDS §5.6 correction: the `LOCATION_SHARE` precedent leaves V1 untouched)

---

## 7. Rollback Plan

```bash
# Revert implementation + test files (dev):
git revert <commit>

# The Flyway migration does NOT need reverting — a widened CHECK constraint is
# backward compatible; the reverted binary simply never writes 'EPDS_RESULT'.

# Only if EPDS_RESULT rows must be purged (e.g. escalation wording rejected at
# clinician review — TDS OPEN-1):
#   DELETE FROM notification_records WHERE type = 'EPDS_RESULT';
#   then restore the 8-value constraint.

# Deployed environments: redeploy the previous build (TDS §12).
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-pattern | Signal | Check | Gate |
| ----- | ------------ | ------ | ----- | ---- |
| AP-01 | **Vacuous negative assertion** — "does not contain 'tự sát'" passes against an empty message | TC-06/TC-07 assert only absence | [ ] Each also asserts the message is non-blank and carries the expected title | Red Gate |
| AP-02 | **Test mirrors implementation** — helper re-implements `band()` instead of asserting literals | Expected values computed, not written | [ ] Band labels asserted as literal Vietnamese strings | Review |
| AP-03 | **Consent gate stubbed permissively** — `hasPermission` mocked to always return true | `any()` matchers on the policy mock | [ ] TC-09/TC-10/TC-11 stub per-flag, per-user explicitly | Review |
| AP-04 | **Happy-path-only coverage** — escalation branch untested at low totals | No case with low total + q10 > 0 | [ ] TC-08 exists and asserts the escalation title | Exit |
| AP-05 | **Silent scope creep** — implementation modifies `FamilyQuickNoteService` or the redaction path | Diff touches family history code | [x] `FamilyQuickNoteService` and the redaction path untouched. One addition beyond the §5.1 list: mobile `notification_type_display.dart` (extraction of the existing label/icon mapping so TC-20/TC-21 could test it without pumping a full screen — behaviour unchanged, covered by a regression assertion on the pre-existing labels) | Exit |
| AP-06 | **Green-washing the spec** — marking TCs 🟢 without a real run | Tracker filled without commit/run evidence | [x] Only cases with an actual passing run are marked GREEN; TC-13/TC-14b are explicitly marked NOT WRITTEN / NOT RUN, and both Red Gate exceptions are documented rather than recorded as FAIL | Exit |
| AP-07 | **Assertion on mock, not behavior** — verifying `send` was called but never inspecting the payload | `verify(...)` with no argument captor | [ ] TC-01/TC-06 capture and inspect the `SendNotificationRequest` | Review |
