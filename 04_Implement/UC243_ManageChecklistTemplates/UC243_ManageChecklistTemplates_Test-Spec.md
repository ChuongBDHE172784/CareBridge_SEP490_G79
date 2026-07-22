# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-243 Manage Checklist Templates (Content Admin CRUD)

**Document ID:** `CB-CONTENT-IMP-011-TS`
**Version:** `1.0`
**Date:** `2026-07-22`
**Status:** `Approved`
**Author:** `AI Agent — Claude`
**Reviewed by:** `[x] HuyND`
**DPO Sign-off:** `N/A — Internal, no PII`
**Approved by:** `[x] HuyND — 2026-07-22, "approved with defaults"`

**References:**
- `04_Implement/UC243_ManageChecklistTemplates/UC243_ManageChecklistTemplates_TDS.md` (this feature's TDS)
- `04_Implement/UC50_ManagePreparationChecklist/` — downstream consumer, must not break
- `04_Implement/UC105_CreateContentFAQChecklist/`, `UC106_UpdateContentFAQChecklist/`, `UC107_HideOrDeleteContent/` — sibling pattern for `ContentItem` (create/update/archive), reused here
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — `checklist_templates`/`checklist_items` schema (no new migration in this feature)

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-22 | AI Agent — Claude | Khởi tạo tài liệu — Draft, chờ review |
| 2026-07-22 | HuyND | Duyệt với đề xuất mặc định ("approved with defaults") → Status: Approved. Đồng thời yêu cầu nâng cấp `/admin/content-approval-queue` để hỗ trợ luồng duyệt checklist template (mở rộng §11.1) |
| 2026-07-22 | AI Agent — Claude | Implement xong theo TDD Red→Green: 18 unit/controller test viết trước ở trạng thái stub `UnsupportedOperationException` (RED xác nhận), sau đó implement service/controller thật → toàn bộ 26 unit/controller test + 4 integration test (Testcontainers) GREEN. Không có regression trên bộ test hiện có (`./mvnw test` toàn repo — chỉ còn lỗi môi trường thiếu secret Firebase/Zego, không liên quan tính năng này, và 6 lỗi `ContentMapperTest` đã biết từ trước). Đã QA thủ công trên trình duyệt toàn bộ luồng create→submit→approve/reject→edit→archive. Phát hiện và fix thêm 1 bug trong lúc QA: nút "Xem chi tiết" ở hàng đợi phê duyệt trỏ tới route bị chặn bởi ProtectedRoute CONTENT_ADMIN-only, khiến SYSTEM_ADMIN nhận /forbidden — thêm 2 route review-only mới (`/admin/content-review/:id`, `/admin/content-review/checklists/:id`) và ẩn các nút ghi khi người xem không có role CONTENT_ADMIN |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-243` |
| **Module** | `content` (checklist template admin CRUD) |
| **Spec gốc** | `CB-CONTENT-IMP-011` (TDS đi kèm) |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC` |
| **Upstream Dependencies** | `ChecklistTemplateRepository`, `ChecklistItemRepository`, `AuditService` (đã tồn tại) |
| **Downstream Consumers** | `UC-50` (`UserChecklistItemServiceImpl.importFromTemplate`) — **phải verify không bị break** |

### 1.1 AI Generation Context

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC243 TDS §13` |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T1 (Draft — chưa qua Red Gate, chưa implement)` |

---

## 2. Logic Issues Resolved

| # | Giả định ban đầu (sai) | Thực tế đã verify bằng code | Fix áp dụng trong test |
|---|------------------------|------------------------------|------------------------|
| L1 | `checklist_templates`/`checklist_items` là bảng mồ côi, không ai dùng, có thể tự do đổi cấu trúc | `UserChecklistItemServiceImpl.importFromTemplate()` (UC-50) FK thẳng vào `checklist_items` qua `ChecklistItemRepository.findById()` — đang chạy thật cho MOTHER | Mọi test archive/update PHẢI có 1 test case xác nhận `user_checklist_items` không bị ảnh hưởng (xem TC-INT-004) |
| L2 | Xóa = archive (status=ARCHIVED) sẽ tự động chặn mẹ import từ template đã archived | Đã verify: `importFromTemplate()` hiện KHÔNG kiểm tra status của `ChecklistItem`/`ChecklistTemplate` cha — import vẫn thành công dù template đã ARCHIVED | Test-spec này **không** test hành vi chặn import (ngoài phạm vi UC-243) — ghi rõ trong TC-INT-004 là "known gap, not this feature's job" để tránh false expectation |
| L3 | Cập nhật `items` có thể gán thẳng `List` mới vào field entity rồi `save()` | Bug đã fix ở phiên trước: gán `Stream.toList()` (immutable) vào field Hibernate quản lý → `UnsupportedOperationException` khi flush (xem `AdminContentServiceImpl.updateContent` cũ) | TC-UNIT tương ứng phải assert repository được gọi qua `deleteAll()`+`saveAll()`, không assert trực tiếp field entity |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
UC-243 bao gồm:
├── Service (mock JPA Repository với Mockito) — AdminChecklistTemplateServiceImplTest
├── Controller (@WebMvcTest, mock Service) — AdminChecklistTemplateControllerTest
└── Integration (Testcontainers PostgreSQL) — ChecklistTemplateAdminIntegrationTest
    (bao gồm 1 test xuyên-module xác nhận UC-50 không bị break — TC-INT-004)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|---------------|
| `UC243 TDS §5-9` | DTO validation, service logic, error codes |
| `UC243 TDS §11.2` | items rỗng khi tạo — cho phép |
| `UC107 TDS` (ContentItem archive) | Pattern archive: reason bắt buộc, guard đã-archived → 409 |
| `UC50 TDS` | Ràng buộc downstream — không phá FK, không đổi schema |

### TDS-03 — Test Conditions and Coverage

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|-----------------|---------------|-----------|
| TC-COND-001 | Tạo template hợp lệ với items | `AdminChecklistTemplateServiceImpl.create()` | `CHKTPL-TC-001` |
| TC-COND-002 | Tạo template với items rỗng | `create()` | `CHKTPL-TC-002` |
| TC-COND-003 | Validation fail (name blank) | `create()` | `CHKTPL-TC-003` |
| TC-COND-004 | Update thay toàn bộ items | `update()` | `CHKTPL-TC-004` |
| TC-COND-005 | Update items=null giữ nguyên items cũ | `update()` | `CHKTPL-TC-005` |
| TC-COND-006 | Archive thành công, reason hợp lệ | `archive()` | `CHKTPL-TC-006` |
| TC-COND-007 | Archive thiếu reason → 400 | `archive()` | `CHKTPL-TC-007` |
| TC-COND-008 | Archive template đã archived → 409 | `archive()` | `CHKTPL-TC-008` |
| TC-COND-009 | Role khác CONTENT_ADMIN → 403 | Controller (mọi endpoint) | `CHKTPL-TC-009` |
| TC-COND-010 | Archive không xóa `checklist_items`, không phá `user_checklist_items` (UC-50) | Integration | `CHKTPL-TC-010` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | `items` = rỗng / 1 mục / nhiều mục | Bao phủ dữ liệu seed hiện có (0, 1, nhiều mục) |
| State Transition | `status`: DRAFT → PENDING_REVIEW → ARCHIVED | Không cho transition ngược từ ARCHIVED |
| Error Guessing | Archive 2 lần liên tiếp, update với items=[] (xóa hết mục) | Idempotency guard, data-loss edge case |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value | Mục đích |
|-----------|------|-------|---------|
| `FX-CHK-001` | DB seed | `ChecklistTemplate{status=DRAFT, items=[]}` | Happy path tạo/sửa |
| `FX-CHK-002` | DB seed | `ChecklistTemplate{status=ARCHIVED}` | Test guard 409 |
| `FX-CHK-003` | DB seed | `ChecklistItem` đã được `user_checklist_items` (UC-50) tham chiếu | TC-INT-004/010 — xác nhận archive không phá FK |
| `FX-CHK-004` | JWT | `{role: CONTENT_ADMIN}` / `{role: MOTHER}` | Auth context |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class ChecklistTemplateTestFactory {
    static ChecklistTemplate makeTemplate() {
        return ChecklistTemplate.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .name("Checklist mẫu kiểm thử")
                .stage(ContentStage.PREGNANCY)
                .status(ContentStatus.DRAFT)
                .description("Mô tả kiểm thử")
                .build();
    }
    static ChecklistTemplate makeTemplate(Consumer<ChecklistTemplate> overrides) {
        ChecklistTemplate t = makeTemplate();
        overrides.accept(t);
        return t;
    }
    static ChecklistItem makeItem(ChecklistTemplate template, int order) {
        return ChecklistItem.builder()
                .id(UUID.randomUUID())
                .template(template)
                .itemText("Mục kiểm thử " + order)
                .order(order)
                .isRequired(true)
                .build();
    }
}
```

---

### CHKTPL-TC-001 — Tạo template hợp lệ với items trả về DRAFT

**Severity:** `HIGH`
**Feature Under Test:** `AdminChecklistTemplateServiceImpl.create()`
**Test File:** `src/test/java/com/carebridge/backend/content/AdminChecklistTemplateServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC243 TDS §6.1 (ADR-006-style DRAFT enforcement, kế thừa từ UC-105)`

**Preconditions:** `ChecklistTemplateRepository`, `ChecklistItemRepository`, `AuditService` mocked.

**Test Steps:**
1. Arrange: `CreateChecklistTemplateRequest{name, stage=PREGNANCY, items=[2 mục]}`
2. Act: gọi `create(request, adminUserId)`
3. Assert: `templateRepository.save()` được gọi với `status=DRAFT`; `itemRepository.saveAll()` được gọi với 2 items liên kết đúng template id; `auditService.log()` được gọi 1 lần.

**Expected Result (PASS):** response có `status="DRAFT"`, `items.size()==2`.
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-002 — Tạo template với items rỗng (đúng §11.2)

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC243 TDS §11.2 (đề xuất mặc định: cho phép rỗng)`

**Test Steps:** Arrange `items=[]` → Act `create()` → Assert: không lỗi, `items=[]` trong response, khớp dữ liệu seed hiện có (`Checklist chuẩn bị tâm lý trước sinh`).
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-003 — name blank → CHKTPL-001 (400)

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`

**Test Steps:** Arrange `name=""` → Act `POST /api/v1/admin/checklist-templates` (WebMvcTest) → Assert: 400, error code `CHKTPL-001`.
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-004 — Update thay toàn bộ items (deleteAll + saveAll, KHÔNG dùng Stream.toList trực tiếp lên field)

**Severity:** `CRITICAL` — bug lặp lại từ phiên trước nếu implement sai (xem Logic Issue L3)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `UC243 TDS §5.3 note (bài học bug immutable list)`

**Preconditions:** Template có sẵn 2 items (FX-CHK-001 + 2 `ChecklistItem`).

**Test Steps:**
1. Act: `update(id, request{items=[3 mục mới]}, adminUserId)`
2. Assert: `checklistItemRepository.deleteAll(...)` được gọi với 2 items cũ; `checklistItemRepository.saveAll(...)` được gọi với 3 items mới.
3. **Regression guard:** verify method KHÔNG throw `UnsupportedOperationException` khi chạy với repository thật (integration variant — xem CHKTPL-TC-INT-002).

**Current Status:** 🟢 Passing

---

### CHKTPL-TC-005 — Update với items=null giữ nguyên items cũ

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `UC243 TDS §5.2 (items: null = không đổi, [] = xóa hết — giống ContentItem.sources)`

**Test Steps:** Act `update(id, request{items=null})` → Assert: `checklistItemRepository.deleteAll()`/`saveAll()` KHÔNG được gọi; items cũ vẫn còn nguyên khi đọc lại.
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-006 — Archive thành công

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `UC243 TDS §6.2, ADR-CHK-002`

**Test Steps:** Act `archive(id, {reason="Nội dung lỗi thời"}, adminUserId)` → Assert: `status` chuyển `ARCHIVED`; `checklist_items` con KHÔNG bị xóa (repository `deleteAll` KHÔNG được gọi cho items khi archive — chỉ khi update items).
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-007 — Archive thiếu reason → CHKTPL-005 (400)

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`

**Test Steps:** Act `archive(id, {reason=""})` → Assert: throw exception code `CHKTPL-005`, HTTP 400 ở controller layer; `templateRepository.save()` KHÔNG được gọi.
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-008 — Archive template đã ARCHIVED → CHKTPL-006 (409)

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-CHK-002 — idempotency guard, giống CNT-006 của ContentItem`

**Test Steps:** Precondition template có `status=ARCHIVED` (FX-CHK-002) → Act `archive(id, {reason="..."})` → Assert: throw `CHKTPL-006`, HTTP 409.
**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

### CHKTPL-TC-009 — Role MOTHER không thể gọi bất kỳ endpoint ghi nào

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`

**Test Steps (x3, mỗi endpoint ghi):**
1. `POST /api/v1/admin/checklist-templates` với JWT role=MOTHER → Assert 403.
2. `PUT /api/v1/admin/checklist-templates/{id}` với JWT role=MOTHER → Assert 403.
3. `POST /api/v1/admin/checklist-templates/{id}/archive` với JWT role=MOTHER → Assert 403.

**Expected Result (PASS = an toàn):** `403 Forbidden` cả 3 case; DB không có thay đổi.
**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES (Testcontainers PostgreSQL)

### CHKTPL-TC-INT-001 — Create end-to-end ghi đúng cả 2 bảng

**Severity:** `HIGH`
**Test File:** `src/test/java/com/carebridge/backend/content/integration/ChecklistTemplateAdminIntegrationTest.java`
**TDD Phase:** 🟢 GREEN

**Test Steps:** POST với JWT CONTENT_ADMIN, body có 2 items → Assert: `checklist_templates` có 1 row mới status=DRAFT; `checklist_items` có 2 rows với `checklist_template_id` đúng.
**DB Assertion:**
```java
ChecklistTemplate saved = templateRepository.findById(id).orElseThrow();
assertThat(saved.getStatus()).isEqualTo(ContentStatus.DRAFT);
List<ChecklistItem> items = itemRepository.findByTemplate_IdOrderByOrder(id);
assertThat(items).hasSize(2);
```
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-INT-002 — Update items không throw UnsupportedOperationException (regression guard cho bug đã fix ở ContentItem)

**Severity:** `CRITICAL`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `Logic Issue L3 — bug đã gặp thật ở AdminContentServiceImpl.updateContent trong phiên trước`

**Test Steps:** Tạo template với 2 items thật (DB) → gọi `PUT` với 3 items mới → Assert: HTTP 200, không có exception; DB có đúng 3 `checklist_items` mới, 2 items cũ đã bị xóa (không còn trong bảng).
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-INT-003 — Archive giữ nguyên checklist_items con

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`

**Test Steps:** Tạo template + 2 items → archive → Assert: `checklist_templates.status = ARCHIVED`; `checklist_items` vẫn còn đúng 2 rows (COUNT không đổi).
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-INT-004 — Archive KHÔNG phá `user_checklist_items` của UC-50 (downstream safety)

**Severity:** `CRITICAL` — bảo vệ tính năng đang chạy thật (UC-50)
**Test File:** cùng file trên, seed thêm dữ liệu UC-50
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `Logic Issue L1, L2`

**Preconditions:**
- Template + 1 `ChecklistItem` (FX-CHK-003) đã được một `MOTHER` "import" qua `POST /api/v1/checklist/import-from-template` (UC-50 endpoint có sẵn) → tạo ra 1 row `user_checklist_items` tham chiếu `template_item_id`.

**Test Steps:**
1. Act: `POST /api/v1/admin/checklist-templates/{id}/archive {reason}` (CONTENT_ADMIN).
2. Assert: HTTP 200, không có FK violation exception.
3. Assert: `user_checklist_items` row vẫn còn nguyên, `template_item_id` vẫn trỏ đúng `checklist_items` row (không bị xóa/null).
4. **Ghi chú (Logic Issue L2 — không phải bug của UC-243):** test này KHÔNG assert rằng mẹ bị chặn import thêm từ template đã archived — hành vi đó ngoài phạm vi tài liệu này (xem TDS §11.1/ADR-CHK-002 note).

**Current Status:** 🟢 Passing

---

### ADDENDUM TEST CASES (§14 TDS addendum — decide() + list() added post-approval same session)

### CHKTPL-TC-010 — Approve PENDING_REVIEW template → APPROVED

**Severity:** `HIGH`
**Feature Under Test:** `ChecklistTemplateApprovalServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/content/ChecklistTemplateApprovalServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `UC243 TDS §11.1/§14 addendum`

**Test Steps:** Arrange template `status=PENDING_REVIEW` → Act `decide(id, {decision=APPROVE}, principal)` → Assert: `status=APPROVED`, `auditService.log(CHECKLIST_TEMPLATE_DECIDED, ...)` gọi 1 lần.
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-011 — Reject PENDING_REVIEW template với reason → DRAFT

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Oracle Source:** `UC243 TDS §11.1/§14 addendum`

**Test Steps:** Arrange template `status=PENDING_REVIEW` → Act `decide(id, {decision=REJECT, reason="Thiếu mục quan trọng"}, principal)` → Assert: `status=DRAFT`, response `reason` khớp.
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-012 — decide() trên template không PENDING_REVIEW → CHKTPL-007 (409); reject thiếu reason → CHKTPL-008 (400)

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN

**Test Steps (2 case):**
1. Arrange `status=DRAFT` → Act `decide(APPROVE)` → Assert throw `CHKTPL-007`, `templateRepository.save()` không gọi.
2. Arrange `status=PENDING_REVIEW` → Act `decide(REJECT, reason=null/blank)` → Assert throw `CHKTPL-008`.
**Current Status:** 🟢 Passing

---

### CHKTPL-TC-013 — Role không phải SYSTEM_ADMIN gọi `/decision` → 403; list endpoint mở cho cả CONTENT_ADMIN và SYSTEM_ADMIN

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Test File:** `ChecklistTemplateApprovalControllerTest.java` (decision) + `AdminChecklistTemplateControllerTest.java` (list)
**TDD Phase:** 🟢 GREEN

**Test Steps:**
1. `POST /api/v1/admin/checklist-templates/{id}/decision` với JWT role=CONTENT_ADMIN → Assert 403.
2. `GET /api/v1/admin/checklist-templates` với JWT role=SYSTEM_ADMIN → Assert 200 (không 403 — đọc mở cho cả 2 role, §14).
3. `GET /api/v1/admin/checklist-templates` với JWT role=MOTHER → Assert 403.
**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CHKTPL-TC-001` | `AdminChecklistTemplateServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-002` | `AdminChecklistTemplateServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-003` | `AdminChecklistTemplateControllerTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-004` | `AdminChecklistTemplateServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-005` | `AdminChecklistTemplateServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-006` | `AdminChecklistTemplateServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-007` | `AdminChecklistTemplateServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-008` | `AdminChecklistTemplateServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-009` | `AdminChecklistTemplateControllerTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-INT-001` | `ChecklistTemplateAdminIntegrationTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-INT-002` | `ChecklistTemplateAdminIntegrationTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-INT-003` | `ChecklistTemplateAdminIntegrationTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-INT-004` | `ChecklistTemplateAdminIntegrationTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-010` | `ChecklistTemplateApprovalServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-011` | `ChecklistTemplateApprovalServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-012` | `ChecklistTemplateApprovalServiceImplTest.java` | `[x]` | Passed | |
| `CHKTPL-TC-013` | `ChecklistTemplateApprovalControllerTest.java` / `AdminChecklistTemplateControllerTest.java` | `[x]` | Passed | |

### 5.1 Red Gate Protocol

**Stub cho Red Phase:**
```java
@Service
public class AdminChecklistTemplateServiceImpl implements AdminChecklistTemplateService {
    @Override public ChecklistTemplateResponse create(CreateChecklistTemplateRequest r, UUID u) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    // ... tương tự cho update/archive/getById
}
```

**Red Gate Verification:** stub viết cho `AdminChecklistTemplateServiceImpl` và `ChecklistTemplateApprovalServiceImpl` (throw `UnsupportedOperationException`); chạy `./mvnw test -Dtest=AdminChecklistTemplateServiceImplTest,ChecklistTemplateApprovalServiceImplTest,AdminChecklistTemplateControllerTest,ChecklistTemplateApprovalControllerTest` → 18/26 test FAIL đúng như kỳ vọng (các test controller thuần bảo mật/validation không chạm service nên PASS ngay từ RED, đúng bản chất — không phải Green-from-Birth vì chúng không assert hành vi service). Sau khi implement thật, cùng lệnh cho kết quả 26/26 PASS.

| TC ID | Expected | Actual | Tất cả FAIL? |
|-------|----------|--------|--------------|
| `CHKTPL-TC-001, 002, 004-008, 010-012` (service logic) | 🔴 FAIL | ☑ FAIL ☐ PASS | `[x] Yes` |
| `CHKTPL-TC-003, 009, 013` (controller security/validation — không phụ thuộc service impl) | N/A (không throw qua service) | N/A | `[x] Yes` (loại trừ hợp lệ, xem ghi chú trên) |

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] `UC243_ManageChecklistTemplates_TDS.md` đã được đánh dấu `Approved`
- [x] Câu hỏi mở §11.1 (approval flow) và §11.2 (items rỗng) trong TDS đã được Tech Lead trả lời
- [x] Test-Spec này đã được đánh dấu `Approved`

### Exit Criteria (DoD)
- [x] `./mvnw test` — tất cả unit test xanh (26/26 test module này; toàn repo không có regression — chỉ lỗi môi trường thiếu secret Firebase/Zego + 6 lỗi `ContentMapperTest` đã biết từ trước, không liên quan)
- [x] `./mvnw test` (Testcontainers) — tất cả integration test xanh, bao gồm `CHKTPL-TC-INT-004` (UC-50 safety)
- [x] Không có business logic trong Controller
- [x] `CHKTPL-TC-INT-002` xanh — xác nhận không lặp lại bug immutable-list
- [x] Frontend: `ChecklistListPage` có nút Tạo/Sửa/Xóa hoạt động thật (không còn `disabled` tooltip "sắp ra mắt") — thêm `ChecklistDetailPage`, `ChecklistFormPage` (create+edit dùng chung)
- [x] `CreateContentPage` không còn hiển thị `CHECKLIST` trong dropdown loại nội dung (ADR-CHK-004)

### Suspension Criteria
- Câu hỏi mở §11.1/§11.2 chưa được trả lời
- Phát hiện `importFromTemplate()` (UC-50) cần thay đổi hành vi — cần TDS riêng, ngoài phạm vi UC-243

---

## 7. Rollback Plan

Xem `UC243_ManageChecklistTemplates_TDS.md` §12 — chỉ revert code, không có migration DB cần rollback.

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Check | Gate |
|-------|-------------|-------|------|
| AP-AI-001 | Test không reference constraint nào trong TDS §13 | ☑ (không phát hiện — mỗi TC có Oracle Source/comment trỏ về TDS/ADR) | G-0 |
| AP-AI-002 | Green-from-Birth (test PASS với stub throw) | ☑ (không phát hiện — RED gate xác nhận 18 test service-layer FAIL đúng trước khi implement, xem §5.1) | G-2 ★ |
| AP-AI-003 | Test giả định kiến trúc không có trong TDS §3 ADR | ☑ (không phát hiện — controller split khớp ADR-CHK-001, decide riêng khớp §14 addendum) | G-1 |
| AP-AI-004 | Test kiểm tra business logic trong Controller | ☑ (không phát hiện — controller test chỉ assert HTTP status/403/400, business logic assert ở service test) | G-4 |
| AP-AI-005 | Test import class không tồn tại trong TDS §5 | ☑ (không phát hiện — biên dịch thành công, toàn bộ import khớp §5.2/§5.3 đã cập nhật) | G-3 |

**Kết quả review:** Không phát hiện anti-pattern nào trong 5 mục CASE 2.0. Toàn bộ 26 unit/controller test + 4 integration test đã chạy PASS thật (không mock giả hành vi), đã QA thủ công trên trình duyệt xác nhận luồng hoạt động đúng.

---

*Test-Spec này đã hoàn thành implementation — Status: Approved, toàn bộ test case Status: 🟢 GREEN. Xem CHANGELOG để biết chi tiết quá trình Red→Green và các phát hiện trong lúc QA.*
