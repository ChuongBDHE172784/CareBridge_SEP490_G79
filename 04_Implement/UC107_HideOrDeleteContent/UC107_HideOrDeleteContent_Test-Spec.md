# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-107 Hide or Delete Content — Test Specification

**Document ID:** `CB-CONTENT-TDD-107`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `04_Implement/UC107_HideOrDeleteContent/UC107_HideOrDeleteContent_TDS.md` (`CB-CONTENT-IMP-005`) — Technical Design Specification (this Test-Spec's primary Oracle Source)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — `content_items`, `content_reports`, `moderation_actions` schema baseline
- `04_Implement/UC105_CreateContentFAQChecklist/UC105_CreateContentFAQChecklist_TDS.md` — `AdminContentController`/`AdminContentServiceImpl` base pattern (ADR-005 audit pattern)
- `04_Implement/UC104_RevokeExpertBadge/UC104_RevokeExpertBadge_Test-Spec.md` — sibling Test-Spec, style/structure and Props Isolation factory pattern reference
- `CLAUDE.md` — BR-RBAC, audit requirements for moderation/safety workflows; Flyway rules (never modify applied migration)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`/`.test.tsx`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo Test-Spec cho UC-107, dựa trên TDS `CB-CONTENT-IMP-005` (Draft) |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-107` |
| **Module** | `HideOrDeleteContent — Bounded Context: content` |
| **Spec gốc** | `CB-CONTENT-IMP-005` |
| **Priority** | 🔴 P0 (moderation/safety-adjacent write path) |
| **Sprint** | `S3 — TV3-Huy` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC` (`CONTENT_ADMIN` only) |
| **Upstream Dependencies** | UC-105 Create Content (`ContentItem`, `AdminContentController`, `AdminContentServiceImpl`, `ContentRepository`, `ContentMapper`, `AuditService` all pre-existing) |
| **Downstream Consumers** | `ContentService` public read paths (UC-82/224/225), `ModerationService` (UC-109 dossier, moderation queue) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CONTENT-IMP-005 §17`, ADR-001 through ADR-006 |
| **Constraints Injected** | C1-C9 per TDS §17.1 (hide sets `status=HIDDEN` only; delete sets `deletedAt`/`deletedBy` only, never touches `status`; zero hard-DELETE SQL; existing read methods must add `deletedAt IS NULL`; `reason` `@NotBlank` on both DTOs; `AuditService.log()` same-transaction; optional `reportId` linkage resolves report + writes `ModerationAction`; reuse class-level `@PreAuthorize`; no unhide/undelete endpoint) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS "hides or soft-deletes" text does not name distinct target states, and TDS itself flags UC-107 vs UC-227 (Unpublish Content) as an unresolved scope overlap (OI-1, §18) — both aim at "ẩn nội dung" via Content Admin | TDS §ADR-002 makes a **TẠM THỜI** (provisional) decision: introduce `ContentStatus.HIDDEN` as a NEW value, distinct from `ARCHIVED` (which UC-227 would use) | Tests assert `hideContent()` sets `status` to exactly `ContentStatus.HIDDEN`, never `ARCHIVED`; a dedicated regression test (`CNT107-TC-003`) locks in this distinction so a future merge with UC-227 cannot silently reuse `ARCHIVED` without a spec change |
| L2 | `content_items.status` has no DB CHECK constraint (confirmed via ADR-002/§5.2 note: "không có CHECK constraint trên content_items.status") — nothing in the schema prevents an invalid status string, and `content_items` has no `deleted_at`/`deleted_by` columns pre-UC-107 | ADR-003 adds `deleted_at`/`deleted_by` as an **overlay flag** independent of `status` — `deleteContent()` must NOT change `status` at all, unlike `hideContent()` | Integration test (`CNT107-TC-INT-002`) asserts `status` is byte-identical before/after a delete action performed from every one of the 4 pre-delete states (DRAFT/APPROVED/HIDDEN/ARCHIVED), proving the overlay invariant at the DB layer, not just via mock |
| L3 | TDS §9.1 API table explicitly leaves hide-idempotency **Open** (OI-3) — "hide again on already-HIDDEN → 200, no-op" is the TDS's own assumption but text admits "409 Conflict cũng hợp lý" | This Test-Spec must pin ONE behavior to make tests executable, while flagging the choice as inherited-from-TDS-not-invented-by-tests | Tests implement the TDS's stated assumption (idempotent 200/no-op) in `CNT107-TC-004`, with an explicit comment citing OI-3 so a Tech Lead reviewing this Test-Spec sees the same Open Item surfaced, not silently resolved a second way |
| L4 | ADR-006 adds `ModerationActionType.DELETE` as a brand-new enum value — no prior UC in this codebase batch has tested a `DELETE`-typed `ModerationAction` row (only `HIDE` existed before, per ADR-006's context section) | ADR-006 also requires reason and target linkage to be shared logic (`resolveLinkedReport()` private helper) between `hideContent()` and `deleteContent()`, avoiding two divergent report-resolution implementations | Tests explicitly assert `ModerationAction.actionType == HIDE` for the hide-with-report path and `== DELETE` for the delete-with-report path (`CNT107-TC-009`, `CNT107-TC-010`), guarding against the helper collapsing both into one action type |
| L5 | TDS §10 error-code table documents `CNT-006`/`CNT-007` as explicitly **unused placeholders** (deviation flagged at OI-4) rather than the literal "reserved for UC-107" claim inherited from UC-108's Draft TDS | Only `CNT-013` is a genuinely new error code for UC-107; `CNT-001`/`CNT-003`/`CNT-004`/`CNT-005` are reused from UC-105/UC-106 | Tests never assert `CNT-006`/`CNT-007` are produced by any UC-107 code path (would be an `AP-AI-005 Hallucinated Contract` violation); only `CNT-001`, `CNT-003`, `CNT-004`, `CNT-005`, `CNT-013` appear as expected error codes anywhere in this Test-Spec |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
HideOrDeleteContent bao gồm các layer:
├── DTO Validation (HideContentRequest / DeleteContentRequest — Bean Validation)
├── Services (AdminContentServiceImpl.hideContent()/.deleteContent() — mock ContentRepository,
│             ContentReportRepository, ModerationActionRepository, ContentMapper, AuditService với Mockito)
├── Controller (AdminContentController — mock Service với @WebMvcTest, security chain)
├── Integration (Testcontainers PostgreSQL với @SpringBootTest — bao gồm public-read-exclusion assertion)
└── Web (ContentDetailPage hide/delete action buttons — Vitest + Testing Library, mock API client)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-107` (§3.2.2.9, lines 1176-1195) | Content Admin hides/soft-deletes an existing `ContentItem`; `reason` required; optional report linkage |
| `ADR-001` | `AdminContentController`/`AdminContentServiceImpl` extended, not a new controller/class-level `@PreAuthorize` reused |
| `ADR-002` | `HIDE` → `ContentStatus.HIDDEN` (new value, distinct from `ARCHIVED`); reversible in theory, no self-service unhide endpoint |
| `ADR-003` | `DELETE` → `deleted_at`/`deleted_by` overlay columns; `status` untouched; append-only, no hard DELETE SQL |
| `ADR-004` | Delete NOT reversible via self-service API in UC-107 scope; no unhide/undelete endpoint of any kind |
| `ADR-005` | `reason` `@NotBlank` (max 1000) on both request DTOs; `AuditService.log()` called post-save, same transaction |
| `ADR-006` | Optional `reportId` → validate `ContentReport` exists + `status=PENDING` → set `RESOLVED` + insert `ModerationAction` (action_type=HIDE for hide, DELETE for delete), all same transaction; invalid linkage → `CNT-013` |
| `BR-RBAC` | Only `CONTENT_ADMIN` may call either endpoint |
| `TDS §10` Error Codes | `CNT-001` (validation), `CNT-003` (not found/already deleted), `CNT-004` (forbidden), `CNT-005` (500), `CNT-013` (bad/missing report linkage) |
| `TDS §16` Authorization Matrix | `GUEST`/`USER`/`MODERATOR`/`SYSTEM_ADMIN` all denied; only `CONTENT_ADMIN` allowed (OI-5 flags SYSTEM_ADMIN exclusion as provisional) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Admin hides an APPROVED content item (happy path) | `AdminContentServiceImpl.hideContent()` | `CNT107-TC-001` |
| TC-COND-002 | Admin hides a DRAFT content item (happy path, per §6.4 state diagram `DRAFT→HIDDEN`) | `AdminContentServiceImpl.hideContent()` | `CNT107-TC-002` |
| TC-COND-003 | Hide never sets `deletedAt`; delete never changes `status` (ADR-002/ADR-003 mutual exclusivity) | `AdminContentServiceImpl.hideContent()`/`.deleteContent()` | `CNT107-TC-003` |
| TC-COND-004 | Hide an already-HIDDEN item is idempotent no-op 200 (OI-3 assumption) | `AdminContentServiceImpl.hideContent()` | `CNT107-TC-004` |
| TC-COND-005 | Admin soft-deletes an APPROVED content item (happy path) | `AdminContentServiceImpl.deleteContent()` | `CNT107-TC-005` |
| TC-COND-006 | Admin soft-deletes a HIDDEN content item (delete overlays on top of any status) | `AdminContentServiceImpl.deleteContent()` | `CNT107-TC-006` |
| TC-COND-007 | Second delete on an already-deleted item → `CNT-003` (not idempotent, per §9.1) | `AdminContentServiceImpl.deleteContent()` | `CNT107-TC-007` |
| TC-COND-008 | Hide/delete on unknown or already-deleted `id` → `CNT-003` | `AdminContentServiceImpl.hideContent()`/`.deleteContent()` | `CNT107-TC-008` |
| TC-COND-009 | Hide with valid PENDING `reportId` → report resolves RESOLVED + `ModerationAction(action_type=HIDE)` | `AdminContentServiceImpl.hideContent()` (`resolveLinkedReport()`) | `CNT107-TC-009` |
| TC-COND-010 | Delete with valid PENDING `reportId` → report resolves RESOLVED + `ModerationAction(action_type=DELETE)` | `AdminContentServiceImpl.deleteContent()` (`resolveLinkedReport()`) | `CNT107-TC-010` |
| TC-COND-011 | `reportId` provided but not found → `CNT-013` (404 semantics) | `AdminContentServiceImpl` (`resolveLinkedReport()`) | `CNT107-TC-011` |
| TC-COND-012 | `reportId` provided but `report.status != PENDING` → `CNT-013` (400 semantics) | `AdminContentServiceImpl` (`resolveLinkedReport()`) | `CNT107-TC-012` |
| TC-COND-013 | No `reportId` → report resolution skipped entirely, only `AuditService.log()` called | `AdminContentServiceImpl.hideContent()`/`.deleteContent()` | `CNT107-TC-013` |
| TC-COND-014 | `reason` blank/null on hide or delete → `CNT-001` (Bean Validation) | `HideContentRequest`/`DeleteContentRequest` `@NotBlank` | `CNT107-TC-014`, `CNT107-TC-015` |
| TC-COND-015 | Boundary: `reason` length (1000 accepted, 1001 rejected) | `HideContentRequest`/`DeleteContentRequest` `@Size(max=1000)` | `CNT107-TC-016`, `CNT107-TC-017` |
| TC-COND-016 | `AuditService.log()` called with `CONTENT_HIDDEN`/`CONTENT_DELETED` on every successful action | `AdminContentServiceImpl.hideContent()`/`.deleteContent()` | `CNT107-TC-018`, `CNT107-TC-019` |
| TC-COND-017 | No hard-DELETE SQL statement anywhere in the implementation (ADR-003/ADR-004 invariant) | `AdminContentServiceImpl` (structural) | `CNT107-TC-020` |
| TC-COND-018 | Existing read methods (`findByFilters`, `findByIdAndStatus`, `searchByFilters`) exclude soft-deleted rows | `ContentRepository` (structural/integration) | `CNT107-TC-021` |
| TC-COND-019 | Role-based access (`CONTENT_ADMIN` vs others) on both endpoints | `AdminContentController` + Spring Security | `CNT107-TC-022` to `CNT107-TC-027` |
| TC-COND-020 | `reason` field with XSS payload stored safely, not executed | `AdminContentServiceImpl` / persistence layer | `CNT107-TC-028` |
| TC-COND-021 | No unhide/undelete endpoint exists anywhere in the controller (ADR-004/C9) | `AdminContentController` (structural) | `CNT107-TC-029` |
| TC-COND-022 | Web: hide/delete action buttons require a filled `reason` before submit | `ContentDetailPage.tsx` confirm dialog | `CNT107-TC-WEB-001`, `CNT107-TC-WEB-002` |
| TC-COND-023 | Full integration: hide → DB `status='HIDDEN'`; delete → DB `deleted_at IS NOT NULL`; public read excludes both | `AdminContentController` E2E | `CNT107-TC-INT-001`, `CNT107-TC-INT-002`, `CNT107-TC-INT-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Pre-action `status` values (DRAFT/APPROVED/HIDDEN/ARCHIVED) × action (HIDE/DELETE) | Confirms hide/delete behave uniformly regardless of the item's current status, per ADR-002/ADR-003 |
| Boundary Value Analysis | `reason` length (0, 1, 1000, 1001 chars) | Confirms `@NotBlank` + `@Size(max=1000)` boundary (ADR-005) |
| State/Overlay-Flag Testing | `ContentStatus` transitions (HIDE) vs `deletedAt` overlay (DELETE) | Core distinguishing behavior of this UC — must prove the two mechanisms never conflate (Logic Issue L1/L2) |
| Error Guessing | Double-delete, hide-already-hidden, reportId pointing at a RESOLVED report, role bypass attempts | Idempotency and security assurance |
| Negative/Structural Testing | Grep/reflective scan for hard-DELETE SQL, scan for unhide/undelete endpoint | Enforces ADR-003/ADR-004 at the architecture level, not just behaviorally |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-CNT107-001` | DB seed | `content_items` row, `status='APPROVED'`, `deletedAt=null` | Happy path HIDE/DELETE from APPROVED |
| `FX-CNT107-002` | DB seed | `content_items` row, `status='DRAFT'`, `deletedAt=null` | Happy path HIDE from DRAFT (§6.4 state diagram) |
| `FX-CNT107-003` | DB seed | `content_items` row, `status='HIDDEN'`, `deletedAt=null` | Idempotent re-hide test; DELETE-overlay-on-HIDDEN test |
| `FX-CNT107-004` | DB seed | `content_items` row, `deletedAt=<past timestamp>`, `deletedBy=<admin uuid>` | Not-found/already-deleted negative tests (`findByIdAndDeletedAtIsNull` excludes it) |
| `FX-CNT107-005` | DB seed | `content_reports` row, `status='PENDING'`, linked to `FX-CNT107-001`'s content id | ADR-006 report auto-resolution happy path |
| `FX-CNT107-006` | DB seed | `content_reports` row, `status='RESOLVED'` (already closed) | ADR-006 invalid-linkage negative test (`CNT-013`) |
| `FX-CNT107-007` | JWT | `{ sub: 'content-admin-001', role: 'CONTENT_ADMIN' }` | Auth context for admin actions |
| `FX-CNT107-008` | JWT | `{ sub: 'mother-001', role: 'MOTHER' }` | Negative auth test |
| `FX-CNT107-009` | JWT | `{ sub: 'mod-001', role: 'MODERATOR' }` | Negative auth test |
| `FX-CNT107-010` | JWT | `{ sub: 'sysadmin-001', role: 'SYSTEM_ADMIN' }` | Negative auth test — OI-5 confirms SYSTEM_ADMIN is NOT granted access in this TDS |
| `FX-CNT107-011` | JWT | none (no `Authorization` header) | 401 negative test |

---

## 4. Test Case Specification

> **TC ID format:** `CNT107-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ContentModerationTestFactory.java — NEW for UC-107, extends the UC-105
// ContentTestFactory pattern if one exists; otherwise standalone factory
// scoped to package com.carebridge.backend.content.
// Do NOT create a second competing factory for hide/delete fixtures.
// ═══════════════════════════════════════════════════════════

class ContentModerationTestFactory {

    static final UUID CONTENT_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    // --- ContentItem builders ---

    static ContentItem makeApprovedContent() {
        return makeApprovedContent(c -> {});
    }

    static ContentItem makeApprovedContent(Consumer<ContentItem> overrides) {
        ContentItem item = new ContentItem();
        item.setId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        item.setType(ContentType.ARTICLE);
        item.setTitle("Dinh dưỡng cho mẹ bầu 3 tháng đầu");
        item.setBody("Nội dung mẫu về dinh dưỡng thai kỳ...");
        item.setStage(ContentStage.PUBLISHED);
        item.setTopicId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        item.setStatus(ContentStatus.APPROVED);
        item.setVersionNo(1);
        item.setAuthorUserId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
        item.setSourceLabel("Nội bộ");
        item.setPublishedAt(Instant.parse("2026-06-20T09:00:00Z"));
        item.setCreatedAt(Instant.parse("2026-06-18T09:00:00Z"));
        item.setUpdatedAt(Instant.parse("2026-06-20T09:00:00Z"));
        item.setDeletedAt(null);
        item.setDeletedBy(null);
        overrides.accept(item);
        return item;
    }

    static ContentItem makeDraftContent() {
        return makeApprovedContent(c -> {
            c.setId(UUID.fromString("00000000-0000-0000-0000-000000000102"));
            c.setStatus(ContentStatus.DRAFT);
            c.setPublishedAt(null);
        });
    }

    static ContentItem makeHiddenContent() {
        return makeApprovedContent(c -> {
            c.setId(UUID.fromString("00000000-0000-0000-0000-000000000103"));
            c.setStatus(ContentStatus.HIDDEN);
        });
    }

    static ContentItem makeArchivedContent() {
        return makeApprovedContent(c -> {
            c.setId(UUID.fromString("00000000-0000-0000-0000-000000000104"));
            c.setStatus(ContentStatus.ARCHIVED);
        });
    }

    static ContentItem makeSoftDeletedContent() {
        return makeApprovedContent(c -> {
            c.setId(UUID.fromString("00000000-0000-0000-0000-000000000105"));
            c.setDeletedAt(Instant.parse("2026-06-30T09:00:00Z"));
            c.setDeletedBy(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
        });
    }

    // --- Request DTO builders ---

    static HideContentRequest makeHideRequest() {
        return makeHideRequest("Nội dung lỗi thời, cần cập nhật thông tin y tế mới", null);
    }

    static HideContentRequest makeHideRequest(String reason, UUID reportId) {
        HideContentRequest req = new HideContentRequest();
        req.setReason(reason);
        req.setReportId(reportId);
        return req;
    }

    static DeleteContentRequest makeDeleteRequest() {
        return makeDeleteRequest("Nội dung vi phạm chính sách, cần loại bỏ khỏi hệ thống", null);
    }

    static DeleteContentRequest makeDeleteRequest(String reason, UUID reportId) {
        DeleteContentRequest req = new DeleteContentRequest();
        req.setReason(reason);
        req.setReportId(reportId);
        return req;
    }

    // --- ContentReport builders (ADR-006) ---

    static ContentReport makePendingReport(UUID contentId) {
        ContentReport report = new ContentReport();
        report.setId(UUID.fromString("00000000-0000-0000-0000-000000000301"));
        report.setContentId(contentId);
        report.setStatus(ContentReportStatus.PENDING);
        report.setCreatedAt(Instant.parse("2026-06-29T09:00:00Z"));
        return report;
    }

    static ContentReport makeResolvedReport(UUID contentId) {
        ContentReport report = makePendingReport(contentId);
        report.setId(UUID.fromString("00000000-0000-0000-0000-000000000302"));
        report.setStatus(ContentReportStatus.RESOLVED);
        report.setResolvedAt(Instant.parse("2026-06-29T10:00:00Z"));
        return report;
    }
}
```

---

### CNT107-TC-001 — Hide APPROVED content sets status=HIDDEN (happy path)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §6.1 Sequence Diagram Happy Path HIDE`, `ADR-002`

**Preconditions:** `FX-CNT107-001` seeded (mocked `findByIdAndDeletedAtIsNull` returns `makeApprovedContent()`).

**Test Steps:**
1. Arrange: `contentRepository.findByIdAndDeletedAtIsNull(id)` returns `Optional.of(makeApprovedContent())`
2. Act: `service.hideContent(id, makeHideRequest(), CONTENT_ADMIN_ID)`
3. Assert: returned `ContentActionResponse.status == "HIDDEN"`; `contentRepository.save()` called with entity where `status == ContentStatus.HIDDEN`; `body`/`versionNo`/`title` unchanged from the fixture

**Expected Result (PASS):** Status transitions to `HIDDEN`, content payload untouched.
**Expected Result (FAIL):** Status unchanged, wrong target state, or `body`/`versionNo` mutated.

**Current Status:** 🔴 Not written

---

### CNT107-TC-002 — Hide DRAFT content sets status=HIDDEN (happy path, §6.4 state diagram)

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `TDS §6.4 State Machine` — `DRAFT --> HIDDEN : UC-107 hideContent()`

**Test Steps:**
1. Arrange: mocked repo returns `makeDraftContent()`
2. Act: `service.hideContent(id, makeHideRequest(), CONTENT_ADMIN_ID)`
3. Assert: result `status == "HIDDEN"`

**Expected Result (PASS):** Hide is not restricted to APPROVED-only items — DRAFT content can also be hidden per the documented state diagram.
**Expected Result (FAIL):** Service rejects the DRAFT-origin hide, contradicting §6.4.

**Current Status:** 🔴 Not written

---

### CNT107-TC-003 — Hide and delete are mutually exclusive mechanisms (ADR-002/ADR-003 non-conflation)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()` / `.deleteContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §17.1 C1/C2`, Logic Issue L1/L2 (§2)

**Test Steps:**
1. Act: `service.hideContent(id, makeHideRequest(), CONTENT_ADMIN_ID)` on `makeApprovedContent()`
2. Assert: saved entity has `status == HIDDEN` AND `deletedAt == null` (hide never touches the delete overlay)
3. Act: `service.deleteContent(id2, makeDeleteRequest(), CONTENT_ADMIN_ID)` on a fresh `makeApprovedContent()`
4. Assert: saved entity has `deletedAt != null` AND `status == APPROVED` (delete never changes status — overlay flag, per ADR-003)

**Expected Result (PASS):** The two mechanisms are provably independent — this is the single highest-value regression guard for this UC.
**Expected Result (FAIL):** Hide sets `deletedAt`, or delete mutates `status` — a C1/C2 violation.

**Current Status:** 🔴 Not written
**Implementation Note:** This test would catch the most dangerous implementer mistake: silently reusing `ARCHIVED` for hide (AP-AI-003) or setting `status=DELETED` for delete (no such enum value exists).

---

### CNT107-TC-004 — Re-hiding an already-HIDDEN item is idempotent (200, no-op) — OI-3 assumption

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §9.1 Endpoints Table` footnote — "hide again on already-HIDDEN item → 200, no-op" (explicitly flagged **Open** at OI-3, §18)

**Test Steps:**
1. Arrange: mocked repo returns `makeHiddenContent()`
2. Act: `service.hideContent(id, makeHideRequest(), CONTENT_ADMIN_ID)`
3. Assert: no exception thrown; result `status == "HIDDEN"` (unchanged); `save()` still invoked (or a no-op short-circuit — either satisfies "no-op" per the TDS's own wording)

**Expected Result (PASS):** 200-equivalent success, matching the TDS's stated (but Open) assumption.
**Expected Result (FAIL):** Exception thrown (e.g., a 409-style rejection) — contradicts the TDS text as written; if Tech Lead later picks the 409 alternative (OI-3), this test must be updated alongside a TDS revision, not silently.

**Current Status:** 🔴 Not written
**Implementation Note:** This test's oracle is explicitly the TDS's OWN provisional choice, not an invented behavior — see Logic Issue L3 (§2).

---

### CNT107-TC-005 — Delete APPROVED content sets deletedAt/deletedBy (happy path)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminContentServiceImpl.deleteContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS §6.2 Sequence Diagram Happy Path DELETE`, `ADR-003`

**Test Steps:**
1. Arrange: mocked repo returns `makeApprovedContent()`
2. Act: `service.deleteContent(id, makeDeleteRequest(), CONTENT_ADMIN_ID)`
3. Assert: saved entity `deletedAt != null`, `deletedBy == CONTENT_ADMIN_ID`; result `deletedAt` non-null in response DTO

**Expected Result (PASS):** Soft-delete columns populated, `status` unchanged (`APPROVED`).
**Expected Result (FAIL):** `deletedAt`/`deletedBy` not set, or `status` incorrectly mutated.

**Current Status:** 🔴 Not written

---

### CNT107-TC-006 — Delete overlays on top of HIDDEN status (delete works regardless of status, §6.4)

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl.deleteContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §6.4 State Machine` — `HIDDEN --> HIDDEN : UC-107 deleteContent() (deletedAt set, status KHÔNG đổi)`

**Test Steps:**
1. Arrange: mocked repo returns `makeHiddenContent()`
2. Act: `service.deleteContent(id, makeDeleteRequest(), CONTENT_ADMIN_ID)`
3. Assert: saved entity `deletedAt != null`, `status == HIDDEN` (unchanged — overlay, not replaced)

**Expected Result (PASS):** A HIDDEN item can also be soft-deleted; its `status` field is preserved as `HIDDEN` for audit/forensic purposes.
**Expected Result (FAIL):** `status` reset to something else, or delete rejected on a HIDDEN item (no such restriction is documented).

**Current Status:** 🔴 Not written

---

### CNT107-TC-007 — Second delete on an already-deleted item returns CNT-003 (not idempotent)

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl.deleteContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §9.1 Endpoints Table` — "DELETE ... No (2nd delete on already-deleted → 404 CNT-003)"

**Test Steps:**
1. Arrange: `contentRepository.findByIdAndDeletedAtIsNull(id)` returns `Optional.empty()` (simulating the id belongs to `makeSoftDeletedContent()`, already excluded by the query)
2. Act/Assert: `service.deleteContent(id, makeDeleteRequest(), CONTENT_ADMIN_ID)` throws `ContentException("CNT-003")`

**Expected Result (PASS):** `ContentException("CNT-003")` thrown; `save()` never called.
**Expected Result (FAIL):** Delete silently succeeds a second time, or throws the wrong error code.

**Current Status:** 🔴 Not written
**Implementation Note:** Distinguishes DELETE (not idempotent) from HIDE (idempotent, `CNT107-TC-004`) — deliberately asymmetric per TDS §9.1.

---

### CNT107-TC-008 — Hide or delete on unknown/soft-deleted id returns CNT-003

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()` / `.deleteContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §6.3 Sequence Diagram Error Path`, `TDS §10 CNT-003`

**Test Steps:**
1. Arrange: `contentRepository.findByIdAndDeletedAtIsNull(unknownId)` returns `Optional.empty()`
2. Act/Assert: `service.hideContent(unknownId, makeHideRequest(), CONTENT_ADMIN_ID)` throws `ContentException("CNT-003")`
3. Act/Assert: `service.deleteContent(unknownId, makeDeleteRequest(), CONTENT_ADMIN_ID)` throws `ContentException("CNT-003")`

**Expected Result (PASS):** Both write paths return `CNT-003` for a non-existent OR already-soft-deleted id (single lookup method covers both cases per §8.2).
**Expected Result (FAIL):** NPE, wrong error code, or the two methods diverge in behavior.

**Current Status:** 🔴 Not written

---

### CNT107-TC-009 — Hide with valid PENDING reportId resolves report and writes ModerationAction(HIDE)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()` (`resolveLinkedReport()`)
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-006 Decision step 1-3`, `TDS §6.1 alt request.reportId != null block`

**Preconditions:** `FX-CNT107-005` (`makePendingReport(contentId)`).

**Test Steps:**
1. Arrange: mocked repo returns `makeApprovedContent()`; `contentReportRepository.findById(reportId)` returns `Optional.of(makePendingReport(contentId))`
2. Act: `service.hideContent(id, makeHideRequest("Nội dung bị báo cáo sai lệch", reportId), CONTENT_ADMIN_ID)`
3. Assert: `contentReportRepository.save()` called with `status == RESOLVED`, `resolvedAt != null`
4. Assert: `moderationActionRepository.save()` called with `actionType == ModerationActionType.HIDE`, `moderatorUserId == CONTENT_ADMIN_ID`, `reason` matches request

**Expected Result (PASS):** Both report resolution and moderation-action logging occur in the same call, `action_type=HIDE` (NOT `DELETE`).
**Expected Result (FAIL):** Report not resolved, or wrong `actionType` recorded — see Logic Issue L4.

**Current Status:** 🔴 Not written

---

### CNT107-TC-010 — Delete with valid PENDING reportId resolves report and writes ModerationAction(DELETE)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminContentServiceImpl.deleteContent()` (`resolveLinkedReport()`)
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-006 Decision step 3` — "insert ModerationAction: ... hoặc thêm giá trị mới DELETE ... (cho delete)"

**Test Steps:**
1. Arrange: mocked repo returns `makeApprovedContent()`; `contentReportRepository.findById(reportId)` returns `Optional.of(makePendingReport(contentId))`
2. Act: `service.deleteContent(id, makeDeleteRequest("Nội dung vi phạm chính sách nghiêm trọng", reportId), CONTENT_ADMIN_ID)`
3. Assert: `contentReportRepository.save()` called with `status == RESOLVED`
4. Assert: `moderationActionRepository.save()` called with `actionType == ModerationActionType.DELETE` (the NEW enum value from ADR-006, distinct from `HIDE`)

**Expected Result (PASS):** `ModerationActionType.DELETE` (new value) correctly distinguishes this from the hide path's `HIDE` value.
**Expected Result (FAIL):** `resolveLinkedReport()` helper collapses both actions into `HIDE`, losing the delete-vs-hide distinction in the moderation audit trail.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the test that most directly guards Logic Issue L4 — the shared `resolveLinkedReport()` helper (§11.3 step 9) must accept the action type as a parameter, not hardcode `HIDE`.

---

### CNT107-TC-011 — reportId provided but report not found returns CNT-013

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl` (`resolveLinkedReport()`)
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-006 step 1`, `TDS §10 CNT-013` — "report không tồn tại ... (404)"

**Test Steps:**
1. Arrange: mocked repo returns `makeApprovedContent()`; `contentReportRepository.findById(unknownReportId)` returns `Optional.empty()`
2. Act/Assert: `service.hideContent(id, makeHideRequest("lý do", unknownReportId), CONTENT_ADMIN_ID)` throws `ContentException("CNT-013")`
3. Assert: `contentRepository.save()` NEVER called (whole action rolled back — same transaction, per ADR-006)

**Expected Result (PASS):** Exception thrown BEFORE the content mutation is persisted — no half-applied state.
**Expected Result (FAIL):** Content is hidden despite the invalid report linkage, violating the "same transaction" guarantee.

**Current Status:** 🔴 Not written

---

### CNT107-TC-012 — reportId provided but report.status != PENDING returns CNT-013

**Severity:** `HIGH`
**Feature Under Test:** `AdminContentServiceImpl` (`resolveLinkedReport()`)
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-006 step 1` — "nếu không → CNT-013 (400/409 tùy trạng thái)"

**Preconditions:** `FX-CNT107-006` (`makeResolvedReport(contentId)`).

**Test Steps:**
1. Arrange: mocked repo returns `makeApprovedContent()`; `contentReportRepository.findById(reportId)` returns `Optional.of(makeResolvedReport(contentId))` (already `RESOLVED`)
2. Act/Assert: `service.deleteContent(id, makeDeleteRequest("lý do", reportId), CONTENT_ADMIN_ID)` throws `ContentException("CNT-013")`

**Expected Result (PASS):** A non-PENDING report cannot be double-resolved by a second hide/delete action.
**Expected Result (FAIL):** Report silently re-resolved, or content deleted despite the stale linkage.

**Current Status:** 🔴 Not written

---

### CNT107-TC-013 — No reportId skips report/moderation-action logic entirely, only audit log written

**Severity:** `MEDIUM`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()` / `.deleteContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `ADR-006 Decision` — "Nếu KHÔNG có reportId ... bỏ qua bước 1-3, chỉ audit log"

**Test Steps:**
1. Act: `service.hideContent(id, makeHideRequest(), CONTENT_ADMIN_ID)` (`reportId == null`)
2. Assert: `contentReportRepository` has zero interactions; `moderationActionRepository` has zero interactions
3. Assert: `auditService.log(eq(AuditAction.CONTENT_HIDDEN), ...)` called exactly once

**Expected Result (PASS):** Report/moderation-action collaborators are never touched for a standalone (non-report-triggered) hide/delete.
**Expected Result (FAIL):** Report repository invoked with a null/garbage id, or a `ModerationAction` spuriously created.

**Current Status:** 🔴 Not written

---

### CNT107-TC-014 — Hide with blank reason rejected (CNT-001)

**Severity:** `CRITICAL`
**Feature Under Test:** `HideContentRequest` Bean Validation (`@NotBlank`)
**Test File:** `src/test/java/com/carebridge/backend/content/dto/HideContentRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-005`, `TDS §9.2 Validation Rules table`

**Test Steps:**
1. Arrange: `reason = null` and `reason = "   "` (blank)
2. Act: validate `HideContentRequest` via `jakarta.validation.Validator`
3. Assert: both produce a constraint violation on `reason`

**Expected Result (PASS):** `CNT-001` surfaced at the controller layer for both null and blank reason.
**Expected Result (FAIL):** No violation raised.

**Current Status:** 🔴 Not written

---

### CNT107-TC-015 — Delete with blank reason rejected (CNT-001)

**Severity:** `CRITICAL`
**Feature Under Test:** `DeleteContentRequest` Bean Validation (`@NotBlank`)
**Test File:** `src/test/java/com/carebridge/backend/content/dto/DeleteContentRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-005`

**Test Steps:**
1. Arrange: `reason = null` and `reason = ""`
2. Act: validate `DeleteContentRequest`
3. Assert: both produce a constraint violation on `reason`

**Expected Result (PASS):** `CNT-001` for both cases, mirroring `HideContentRequest`'s rule (symmetry required by ADR-005 — both DTOs must be equally strict).
**Expected Result (FAIL):** Delete's reason requirement is weaker than hide's, an inconsistency not documented anywhere.

**Current Status:** 🔴 Not written

---

### CNT107-TC-016 — Reason at exactly 1000 chars accepted (boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `HideContentRequest`/`DeleteContentRequest` Bean Validation (`@Size(max=1000)`)
**Test File:** `src/test/java/com/carebridge/backend/content/dto/HideContentRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §8.3 @Size(max = 1000)`

**Test Steps:**
1. Arrange: `reason` = a string of exactly 1000 characters
2. Act: validate `HideContentRequest`
3. Assert: zero constraint violations

**Expected Result (PASS):** No false-positive violation at the upper boundary.
**Expected Result (FAIL):** Violation incorrectly raised at exactly 1000 chars.

**Current Status:** 🔴 Not written

---

### CNT107-TC-017 — Reason at 1001 chars rejected (boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `HideContentRequest`/`DeleteContentRequest` Bean Validation (`@Size(max=1000)`)
**Test File:** `src/test/java/com/carebridge/backend/content/dto/HideContentRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §8.3 @Size(max = 1000)`

**Test Steps:**
1. Arrange: `reason` = a string of exactly 1001 characters
2. Act: validate `HideContentRequest`
3. Assert: exactly one constraint violation on `reason`

**Expected Result (PASS):** Boundary enforced, not silently truncated.
**Expected Result (FAIL):** No violation raised at 1001 chars.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### CNT107-TC-018 — Successful HIDE emits audit log CONTENT_HIDDEN

**Severity:** `CRITICAL`
**Legal:** `Audit (BR-AUDIT)`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `ADR-005`, `TDS §11.3 step 3` — "AuditAction: thêm CONTENT_HIDDEN, CONTENT_DELETED"

**Test Steps:**
1. Act: `service.hideContent(id, makeHideRequest(), CONTENT_ADMIN_ID)`
2. Assert: `verify(auditService).log(eq(AuditAction.CONTENT_HIDDEN), eq(CONTENT_ADMIN_ID), eq("ContentItem"), eq(id.toString()), any())` called exactly once, AFTER `contentRepository.save()` (same transaction, per ADR-005)

**Expected Result (PASS):** Audit call occurs with the new `CONTENT_HIDDEN` enum value (not a reused/generic action).
**Expected Result (FAIL):** No audit call, or wrong `AuditAction` value used.

**Current Status:** 🔴 Not written

---

### CNT107-TC-019 — Successful DELETE emits audit log CONTENT_DELETED

**Severity:** `CRITICAL`
**Legal:** `Audit (BR-AUDIT)`
**Feature Under Test:** `AdminContentServiceImpl.deleteContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `ADR-005`

**Test Steps:**
1. Act: `service.deleteContent(id, makeDeleteRequest(), CONTENT_ADMIN_ID)`
2. Assert: `verify(auditService).log(eq(AuditAction.CONTENT_DELETED), eq(CONTENT_ADMIN_ID), eq("ContentItem"), eq(id.toString()), any())` called exactly once

**Expected Result (PASS):** Distinct `CONTENT_DELETED` value used, never collapsed with `CONTENT_HIDDEN`.
**Expected Result (FAIL):** Missing call or wrong action code.

**Current Status:** 🔴 Not written

---

### CNT107-TC-020 — No hard-DELETE SQL statement exists anywhere in the implementation (ADR-003/ADR-004 invariant)

**Severity:** `CRITICAL`
**Legal:** `ADR-003`, `ADR-004`, `CLAUDE.md` (Flyway/append-only rules)
**Feature Under Test:** `AdminContentServiceImpl`, `ContentRepository` (structural/static scan)
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplTest.java` (or a dedicated static-analysis test)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `TDS §4.2 Soft-delete invariant`, `TDS §17.4 constraint C3`

**Test Steps:**
1. Act: `service.deleteContent(id, makeDeleteRequest(), CONTENT_ADMIN_ID)` against mocked `contentRepository`
2. Assert: `contentRepository.save()` is called (an UPDATE-shaped operation), and NO `deleteById()`/`delete()`/`deleteByX()` method on `contentRepository` is ever invoked (`verify(contentRepository, never()).delete(any())`, `verify(contentRepository, never()).deleteById(any())`)
3. Static-scan step (documented, run outside JUnit if needed): grep production source for `DELETE FROM content_items` / `@Query(...DELETE...)` targeting `content_items` — expect zero matches

**Expected Result (PASS):** Zero forbidden delete-method invocations; append-only invariant provable at the mock-interaction level.
**Expected Result (FAIL):** `contentRepository.delete()`/`deleteById()` invoked — CRITICAL, release-blocking per TDS §12.1.

**Current Status:** 🔴 Not written

---

### CNT107-TC-021 — Existing read methods (findByFilters/findByIdAndStatus/searchByFilters) exclude soft-deleted rows

**Severity:** `CRITICAL`
**Feature Under Test:** `ContentRepository.findByFilters()` / `.findByIdAndStatus()` / `.searchByFilters()`
**Test File:** `src/test/java/com/carebridge/backend/content/repository/ContentRepositoryTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `ADR-003 §Existing query methods cần rà soát`, `TDS §17.1 constraint C4`

**Preconditions:** DB seeded with `FX-CNT107-001` (active) and `FX-CNT107-005`-linked content soft-deleted (`deletedAt` set).

**Test Steps:**
1. Seed one active `content_items` row and one soft-deleted `content_items` row (same `status`, differing only by `deletedAt`)
2. Act: call each of `findByFilters(...)`, `findByIdAndStatus(id, status)`, `searchByFilters(...)` against a filter that would match BOTH rows if `deletedAt` were ignored
3. Assert: the soft-deleted row is absent from every result set; the active row is present

**Expected Result (PASS):** All three pre-existing query methods honor the new `deletedAt IS NULL` filter — no read path bypasses the soft-delete invariant.
**Expected Result (FAIL):** Any of the three methods returns the soft-deleted row — a data-leak regression per ADR-003.

**Current Status:** 🔴 Not written
**Implementation Note:** This test targets a MODIFICATION to pre-existing methods, not a new method — the highest-risk item flagged in TDS §8.2's "⚠️ Existing query methods cần rà soát" warning.

---

### CNT107-TC-022 — CONTENT_ADMIN can call PATCH .../hide

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminContentController.hideContent()` + Spring Security chain
**Test File:** `src/test/java/com/carebridge/backend/content/controller/AdminContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-CNT107-007` (CONTENT_ADMIN JWT).

**Test Steps (Attack Simulation):** N/A — positive case.
1. Send `PATCH /api/v1/admin/content/{id}/hide` with CONTENT_ADMIN JWT and valid `{reason:"..."}` body
2. Assert `200 OK`

**Expected Result (PASS):** `200 OK`.
**Expected Result (FAIL):** `403 Forbidden` incorrectly returned for a valid admin.

**Current Status:** 🔴 Not written

---

### CNT107-TC-023 — MOTHER role forbidden from hide/delete endpoints

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminContentController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/content/controller/AdminContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-CNT107-008` (MOTHER JWT).

**Test Steps (Attack Simulation):**
1. Send `PATCH .../hide` with MOTHER JWT
2. Assert `403 Forbidden`, body `error.code == "CNT-004"`
3. Repeat for `DELETE /api/v1/admin/content/{id}`, same assertion

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` on both endpoints.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK`.

**Current Status:** 🔴 Not written

---

### CNT107-TC-024 — MODERATOR role forbidden from hide/delete endpoints

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminContentController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/content/controller/AdminContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `TDS §16 Authorization Matrix` — MODERATOR explicitly ❌

**Preconditions:** `FX-CNT107-009` (MODERATOR JWT).

**Test Steps (Attack Simulation):**
1. Send `PATCH .../hide` with MODERATOR JWT
2. Assert `403 Forbidden`, body `error.code == "CNT-004"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` — MODERATOR handles report queues/read access elsewhere (UC-109) but does NOT have write access to hide/delete content in UC-107's scope.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK` — privilege scope creep beyond SRS Primary Actor.

**Current Status:** 🔴 Not written

---

### CNT107-TC-025 — SYSTEM_ADMIN forbidden from hide/delete endpoints (OI-5 provisional exclusion)

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `BR-RBAC`
**Feature Under Test:** `AdminContentController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/content/controller/AdminContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `TDS §16 Authorization Matrix` — "SYSTEM_ADMIN ❌ (see OI-5)"

**Preconditions:** `FX-CNT107-010` (SYSTEM_ADMIN JWT).

**Test Steps (Attack Simulation):**
1. Send `PATCH .../hide` with SYSTEM_ADMIN JWT
2. Assert `403 Forbidden`

**Expected Result (PASS):** `403 Forbidden`, consistent with the TDS's current (explicitly provisional, OI-5) decision to NOT grant SYSTEM_ADMIN override access.
**Expected Result (FAIL):** `200 OK`.

**Current Status:** 🔴 Not written
**Implementation Note:** If Tech Lead resolves OI-5 to grant SYSTEM_ADMIN access, this test must flip alongside a TDS §16 revision — do not silently change only the test.

---

### CNT107-TC-026 — Unauthenticated request rejected (401)

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/content/controller/AdminContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `TDS §9.2 Response — 401 Unauthorized`

**Preconditions:** `FX-CNT107-011` (no `Authorization` header).

**Test Steps (Attack Simulation):**
1. Send `DELETE /api/v1/admin/content/{id}` with no `Authorization` header
2. Assert `401 Unauthorized`, body `error.code == "IAM-001"`

**Expected Result (PASS):** `401 Unauthorized`.
**Expected Result (FAIL):** Request processed without authentication.

**Current Status:** 🔴 Not written

---

### CNT107-TC-027 — DELETE endpoint returns CNT-003 (not IDOR-leaking) for a non-existent content id

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control (IDOR)`
**Feature Under Test:** `AdminContentController.deleteContent()`
**Test File:** `src/test/java/com/carebridge/backend/content/controller/AdminContentControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `TDS §4.3 IDOR protection` — "Content không tồn tại hoặc đã xóa → 404, không leak thông tin"

**Preconditions:** `FX-CNT107-007` (CONTENT_ADMIN JWT).

**Test Steps (Attack Simulation):**
1. Send `DELETE /api/v1/admin/content/00000000-0000-0000-0000-000000000000` with valid CONTENT_ADMIN JWT
2. Assert `404 Not Found`, body contains ONLY `{"error":{"code":"CNT-003","message":"Content item not found"}}` — no stack trace, no hint whether the id never existed vs. was already deleted

**Expected Result (PASS = hệ thống an toàn):** Generic not-found response, no information disclosure distinguishing "never existed" from "already soft-deleted."
**Expected Result (FAIL = lỗ hổng tồn tại):** Response body leaks internal details (e.g., a distinct error for "already deleted" vs "never existed").

**Current Status:** 🔴 Not written

---

### CNT107-TC-028 — reason field with XSS payload stored safely, not executed

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**Feature Under Test:** `AdminContentServiceImpl.hideContent()` persistence path
**Test File:** `src/test/java/com/carebridge/backend/content/service/AdminContentServiceImplSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `TDS §4.3 Reason injection` — "reason field không chứa XSS payload thực thi được"

**Test Steps:**
1. Arrange: `reason = "<script>alert('xss')</script>"`
2. Act: `service.hideContent(id, makeHideRequest(reason, null), CONTENT_ADMIN_ID)`
3. Assert: the raw string is persisted as-is (JPA parameterized binding, no string-concatenated SQL) into the audit log payload; assert no `Statement`/string-concatenation-based JDBC access exists on this path (structural check against `AuditService.log()` and `contentRepository.save()` both using JPA)

**Expected Result (PASS = hệ thống an toàn):** Payload is stored as inert text (escaped on any future render by the Web layer, out of this UC's backend scope) with no SQL injection vector.
**Expected Result (FAIL = lỗ hổng tồn tại):** Any raw SQL concatenation of `reason` is discovered, or the save silently strips/executes the payload.

**Current Status:** 🔴 Not written

---

### CNT107-TC-029 — No unhide/undelete endpoint exists anywhere in AdminContentController (ADR-004/C9)

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminContentController` (structural/reflective scan)
**Test File:** `src/test/java/com/carebridge/backend/content/controller/AdminContentControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `ADR-004`, `TDS §17.4 AP-AI-001` — "Code tạo thêm unhide/undelete endpoint không có trong SRS"

**Test Steps:**
1. Reflectively enumerate all `@RequestMapping`/`@PatchMapping`/`@PostMapping`/`@PutMapping` methods declared on `AdminContentController`
2. Assert: no method path matches any of `unhide`, `undelete`, `restore`, `recover` (case-insensitive substring scan) for the `/api/v1/admin/content/**` route family

**Expected Result (PASS):** No self-service reversal endpoint exists — matches ADR-004's explicit scope exclusion.
**Expected Result (FAIL):** An unhide/undelete/restore endpoint is found — `AP-AI-001 Unconstrained Generation` violation, must block merge.

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### CNT107-TC-WEB-001 — Hide confirm dialog blocks submit until reason is provided

**Severity:** `HIGH`
**Feature Under Test:** `ContentDetailPage.tsx` hide action confirm dialog
**Test File:** `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/__tests__/ContentDetailPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `TDS §11.3 Chặng 3` — "confirm dialog yêu cầu nhập reason"

**Test Steps:**
1. Render `ContentDetailPage` with an `APPROVED` content fixture
2. Click "Ẩn nội dung" ("Hide") action button to open the confirm dialog
3. Leave the reason textarea empty and attempt to submit
4. Assert: submit is blocked (button disabled or validation error shown); `hideContent` API call NOT invoked

**Expected Result (PASS):** Form blocks submission with an empty reason, mirroring backend `CNT-001`.
**Expected Result (FAIL):** Dialog submits with an empty reason, causing an avoidable 400 round-trip.

**Current Status:** 🔴 Not written

---

### CNT107-TC-WEB-002 — Delete confirm dialog blocks submit until reason is provided

**Severity:** `HIGH`
**Feature Under Test:** `ContentDetailPage.tsx` delete action confirm dialog
**Test File:** `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/__tests__/ContentDetailPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `TDS §11.3 Chặng 3`

**Test Steps:**
1. Render `ContentDetailPage` with an `APPROVED` content fixture
2. Click "Xóa nội dung" ("Delete") action button to open the confirm dialog
3. Leave the reason textarea empty and attempt to submit
4. Assert: submit is blocked; `deleteContent` API call NOT invoked
5. Fill in a valid reason and submit; assert `deleteContent` API called with the entered reason

**Expected Result (PASS):** Delete dialog enforces the same reason requirement as hide (symmetric UX, per `CNT107-TC-015`'s backend symmetry).
**Expected Result (FAIL):** Delete dialog allows an empty-reason submit while hide does not — inconsistent UX/validation.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### CNT107-TC-INT-001 — Full flow: hide via real API + DB, status becomes HIDDEN

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: PATCH /api/v1/admin/content/{id}/hide → DB update`
**Test File:** `src/test/java/com/carebridge/backend/content/AdminContentHideDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-023`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration `V20260704110000__add_content_soft_delete.sql` applied automatically on Spring context start
- Seed: one `users` row (role CONTENT_ADMIN), one `content_items` row with `status='APPROVED'`

**Test Steps:**
1. Seed data as above
2. `PATCH /api/v1/admin/content/{id}/hide` with CONTENT_ADMIN JWT, body `{"reason":"Thông tin y tế đã lỗi thời"}`
3. Assert response `200`, `data.status == "HIDDEN"`
4. Query DB directly: `SELECT status, deleted_at FROM content_items WHERE content_item_id = ?`

**Expected Result (PASS):**
- API response `200` with `status: "HIDDEN"`
- DB row: `status = 'HIDDEN'`, `deleted_at IS NULL` (unaffected), `updated_at` bumped

**Expected Result (FAIL):** DB row not updated, or `deleted_at` incorrectly populated by a hide action.

**DB Assertion:**
```java
ContentItem record = contentRepository.findById(savedId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(ContentStatus.HIDDEN);
assertThat(record.getDeletedAt()).isNull();
```

**Current Status:** 🔴 Not written

---

### CNT107-TC-INT-002 — Full flow: delete via real API + DB, deleted_at set, status preserved across all 4 origin states

**Severity:** `CRITICAL`
**Legal:** `ADR-003`, `ADR-004`
**Feature Under Test:** `Full flow: DELETE /api/v1/admin/content/{id} → DB verification`
**Test File:** `src/test/java/com/carebridge/backend/content/AdminContentHideDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-023`
**Oracle Source:** `ADR-003`, `TDS §14.1 DB Inspection`, Logic Issue L2 (§2)

**Preconditions:**
- 4 seeded `content_items` rows, one per status: `DRAFT`, `APPROVED`, `HIDDEN`, `ARCHIVED` — all with `deleted_at IS NULL`

**Test Steps:**
1. For each of the 4 seeded rows, `DELETE /api/v1/admin/content/{id}` with CONTENT_ADMIN JWT, body `{"reason":"Kiểm tra soft-delete overlay"}`
2. Assert response `200` for each
3. Re-query DB for all 4 rows: assert `deleted_at IS NOT NULL`, `deleted_by = <admin userId>` on every row
4. Assert `status` column is UNCHANGED per row (still `DRAFT`/`APPROVED`/`HIDDEN`/`ARCHIVED` respectively — proves the overlay invariant across the full state space, not just one status)
5. Assert `SELECT COUNT(*) FROM content_items WHERE content_item_id IN (...)` still returns 4 — no row was ever hard-deleted

**Expected Result (PASS):** Delete overlays correctly regardless of origin status; append-only invariant holds (row count unchanged).
**Expected Result (FAIL):** Any row's `status` mutated, or row count drops below 4 (a hard-delete occurred).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important integration test in this Test-Spec — it is the only test that proves ADR-003's overlay-flag invariant against a REAL database across every documented status origin, not just one mocked case.

---

### CNT107-TC-INT-003 — Public read paths exclude hidden and soft-deleted content after hide/delete

**Severity:** `CRITICAL`
**Legal:** `ADR-002`, `ADR-003`, `TDS §4.2 Public read exclusion`
**Feature Under Test:** `Full flow: PATCH .../hide + DELETE ... → GET /api/v1/content/{id} (public)`
**Test File:** `src/test/java/com/carebridge/backend/content/AdminContentHideDeleteIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-023`
**Oracle Source:** `TDS §4.2 Public read exclusion`, `TDS §11.4 Deployment Checklist`

**Preconditions:**
- 2 seeded `content_items` rows with `status='APPROVED'` (publicly visible baseline)

**Test Steps:**
1. Confirm baseline: `GET /api/v1/content/{id1}` (no auth, public endpoint) returns `200` for both seeded rows before any action
2. `PATCH /api/v1/admin/content/{id1}/hide` with CONTENT_ADMIN JWT, reason provided
3. `DELETE /api/v1/admin/content/{id2}` with CONTENT_ADMIN JWT, reason provided
4. Re-query: `GET /api/v1/content/{id1}` (public) and `GET /api/v1/content/{id2}` (public)
5. Assert both return `404` (or the public-facing not-found shape), NOT `200` with hidden/deleted content leaked

**Expected Result (PASS):** Neither hidden (`status=HIDDEN`) nor soft-deleted (`deletedAt != null`) content is ever visible on the public read path — the core moderation guarantee of this UC.
**Expected Result (FAIL):** Public endpoint still returns the hidden or deleted content — CRITICAL data-exposure regression, maps directly to the P0 rollback trigger in `TDS §12.1`.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CNT107-TC-001` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-002` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-003` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-004` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-005` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-006` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-007` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-008` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-009` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-010` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-011` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-012` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-013` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-014` | `HideContentRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-015` | `DeleteContentRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-016` | `HideContentRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-017` | `HideContentRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-018` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-019` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-020` | `AdminContentServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-021` | `ContentRepositoryTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-022` | `AdminContentControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-023` | `AdminContentControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-024` | `AdminContentControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-025` | `AdminContentControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-026` | `AdminContentControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-027` | `AdminContentControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-028` | `AdminContentServiceImplSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-029` | `AdminContentControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-WEB-001` | `ContentDetailPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-WEB-002` | `ContentDetailPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-INT-001` | `AdminContentHideDeleteIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-INT-002` | `AdminContentHideDeleteIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `CNT107-TC-INT-003` | `AdminContentHideDeleteIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class AdminContentServiceImpl implements AdminContentService {

    // ... existing createContent() from UC-105 untouched ...

    @Override
    public ContentActionResponse hideContent(UUID id, HideContentRequest request, UUID adminId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ContentActionResponse deleteContent(UUID id, DeleteContentRequest request, UUID adminId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    private void resolveLinkedReport(UUID reportId, ModerationActionType actionType, UUID adminId, String reason) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `CNT107-TC-001` to `004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-005` to `013` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-014` to `017` | `DTO class not yet annotated` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-018`, `019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-020` | `reflective/mock-interaction scan fails — class not yet wired` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-021` | `deletedAt column/filter not yet added — query methods unmodified` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-022` to `027` | `403/401/404 forced by missing controller wiring or missing migration` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-028` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-029` | `endpoint absent — trivially passes; MUST re-verify manually post-implementation that no new bad endpoint appears` | ⚠️ Vacuous pre-implementation — treat as PASS-by-absence, re-run post-GREEN | ☐ Confirmed | |
| `CNT107-TC-WEB-001/002` | `component/dialog not implemented / API not wired` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `CNT107-TC-INT-001` to `003` | `500 from stub exception, or migration not applied` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CONTENT-IMP-005` đã được review và approve
- [ ] Open Items OI-1 (UC-107 vs UC-227 scope overlap) và OI-3 (idempotency choice) đã được Tech Lead xác nhận HOẶC explicitly deferred với sign-off ghi nhận rủi ro
- [ ] UC-105's `AdminContentController`/`AdminContentServiceImpl`/`ContentRepository`/`ContentMapper` đã implement TRƯỚC (UC-107 extend, không tạo mới)
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead
- [ ] Migration `V20260704110000__add_content_soft_delete.sql` đã review (ADD COLUMN only, không sửa migration cũ)
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị, bao gồm `FX-CNT107-005`/`006` (report linkage seed cho ADR-006 tests)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers), đặc biệt `CNT107-TC-INT-002` và `CNT107-TC-INT-003`
- [ ] `npm run test:run` — web tests xanh
- [ ] Test coverage ≥ 80% lines cho `AdminContentServiceImpl` (hide/delete methods + `resolveLinkedReport()`)
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `CNT107-TC-003` (hide/delete mutual exclusivity) và `CNT107-TC-INT-002` (real-DB overlay invariant across 4 states) đều xanh — ADR-002/ADR-003 non-conflation là non-negotiable cho UC này
- [ ] `CNT107-TC-021` (existing query methods modified) và `CNT107-TC-INT-003` (public read exclusion) đều xanh — ADR-003's "MỌI existing query phải rà soát" risk (§3 Trade-offs) is closed
- [ ] `CNT107-TC-029` (no unhide/undelete endpoint) re-verified GREEN post-implementation, not just vacuously pre-implementation

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` không lỗi
- [ ] **Props Isolation** — mọi test dùng `ContentModerationTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (ADR/SRS/schema)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-105 chưa implement `AdminContentController`/`AdminContentServiceImpl`/`ContentRepository` — UC-107 không thể bắt đầu vì extend cùng class (hard blocker)
- OI-1 (UC-107 vs UC-227 overlap) escalates to a decision that `HIDDEN` must be replaced by `ARCHIVED` mid-implementation — requires TDS revision before continuing, not a silent test rewrite
- Phát hiện lỗi kiến trúc mới cần Tech Lead review (đặc biệt bất kỳ đề xuất nào bỏ qua `deletedAt IS NULL` filter trên một read path mới)

---

## 7. Rollback Plan

```bash
# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/impl/AdminContentServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/AdminContentService.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/dto/request/HideContentRequest.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/dto/request/DeleteContentRequest.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/dto/response/ContentActionResponse.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/
git checkout -- 05_Development/CareBridgeWebApp/src/features/contentManagement/

# CAUTION: do NOT revert ContentItem.java/ContentStatus.java/ModerationActionType.java/AuditAction.java
# wholesale — only remove the deletedAt/deletedBy fields, HIDDEN value, DELETE value, and
# CONTENT_HIDDEN/CONTENT_DELETED values THIS UC introduced, to avoid breaking UC-105/106/108's
# already-shipped functionality on the same shared classes.

# If migration V20260704110000 has already run and must be reverted:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "ALTER TABLE content_items DROP COLUMN IF EXISTS deleted_at, DROP COLUMN IF EXISTS deleted_by;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "DELETE FROM flyway_schema_history WHERE version = '20260704110000';"

# Gap vẫn OPEN → giữ nguyên Status: Draft trong UC107_HideOrDeleteContent_TDS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC introduces an unhide/undelete endpoint not in SRS | ☑ Not detected — `CNT107-TC-029` explicitly asserts absence, per ADR-004/C9 | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Pending Red Gate run | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes hide reuses `ARCHIVED` instead of the TDS's chosen `HIDDEN` | ☑ Not detected — `CNT107-TC-001`/`003` explicitly assert `status == HIDDEN`, never `ARCHIVED` | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller contains report-resolution/soft-delete business logic | ☑ Not detected — all state-mutation logic tested exclusively against `AdminContentServiceImpl`, controller tests are auth/routing-only | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports/asserts `CNT-006`/`CNT-007` as active error codes, or invents a 4th soft-delete mechanism | ☑ Not detected — only `CNT-001/003/004/005/013` appear anywhere in this spec, per Logic Issue L5 | G-3 |
| AP-AI-006 *(custom, project-specific)* | Overlay-Flag Conflation | Test suite fails to assert hide never sets `deletedAt` AND delete never mutates `status` | ☑ Not detected — `CNT107-TC-003` (unit) + `CNT107-TC-INT-002` (real-DB, 4 origin states) both directly target this, per ADR-002/ADR-003 | G-2 ★★ CRITICAL |
| AP-AI-007 *(custom, project-specific)* | Read-Path Leak | Test suite fails to assert existing `findByFilters`/`findByIdAndStatus`/`searchByFilters` exclude soft-deleted rows, or that public GET endpoints exclude hidden/deleted content | ☑ Not detected — `CNT107-TC-021` (repository-level) + `CNT107-TC-INT-003` (full public-endpoint E2E) both directly target this, per ADR-003 §Trade-offs risk note | G-2 ★★ CRITICAL |
| AP-AI-008 *(custom, project-specific)* | Report-Linkage Type Erosion | Test suite fails to distinguish `ModerationActionType.HIDE` (from hide+report) vs `.DELETE` (from delete+report), silently collapsing both into one action type via the shared `resolveLinkedReport()` helper | ☑ Not detected — `CNT107-TC-009`/`CNT107-TC-010` explicitly assert the distinct `actionType` per call site, per Logic Issue L4 | G-1 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec-authoring → TDD spec approved for Red Gate execution
- [ ] AP-AI-002 (Green-from-Birth) check pending actual Red Gate run once stubs are committed
- [ ] AP-AI-006 (Overlay-Flag Conflation) — CRITICAL, treat as a release-blocking gate distinct from standard G-2 severity, given this is the core mechanism distinguishing HIDE from DELETE per TDS §17.1 C1/C2
- [ ] AP-AI-007 (Read-Path Leak) — CRITICAL, matches the P0 rollback trigger explicitly documented in TDS §12.1 ("Hidden/deleted content vẫn hiển thị cho public user")
- [ ] Open Items OI-1/OI-3/OI-5 (TDS §18) remain unresolved at spec time — this Test-Spec implements the TDS's stated provisional assumptions (`HIDDEN` distinct from `ARCHIVED`; hide idempotent/delete not; SYSTEM_ADMIN excluded) and flags each with an inline citation so a later TDS revision on any of these three items requires a corresponding, non-silent Test-Spec update

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending Tech Lead / TV3-Huy review and Red Gate execution.*
