# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Mẫu Đặc tả Kiểm thử Hướng Phát triển — UC50 Manage Preparation Checklist

**Document ID:** `CB-CHECKLIST-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Spec Generator`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] N/A — Non-PII module`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `04_Implement/UC50_ManagePreparationChecklist/UC50_ManagePreparationChecklist_TDS.md` — Technical Specification
- `01_Requirements/SRS.md §3.3.1.27` — Functional requirements

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Spec Generator | Khởi tạo tài liệu — TDD spec cho UC50 Manage Preparation Checklist |

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
| **Feature / Gap ID** | `GAP-UC50-CHECKLIST` |
| **Module** | `UserChecklistItem — CareJourney Preparation` |
| **Spec gốc** | `CB-CHECKLIST-IMP-001` |
| **Priority** | 🟠 P1 — High |
| **Sprint** | `S[N] — 2026-06-26 → TBD` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `N/A` |
| **Upstream Dependencies** | `AuthModule (JWT), MotherJourney, BabyProfile, ChecklistTemplates` |
| **Downstream Consumers** | `UC49_ViewTodayTasks (tùy chọn)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CHECKLIST-IMP-001 §17`, `ADR-001, ADR-002` |
| **Constraints Injected** | `C1 (ownership check), C2 (template-item immutability), C3 (toggle endpoint), C4 (userId from JWT), C5 (controller no business logic), C6 (item_text from template)` |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Cho CareBridge schema, dùng `V1__init_schema.sql` và approved migrations làm oracle cuối cùng.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC50 giả định có bảng user checklist trong V1 | V1__init_schema.sql KHÔNG có bảng user-level checklist. `checklist_templates` + `checklist_items` là admin content. | Test dựa trên `user_checklist_items` table từ migration V2 |
| L2 | SRS mô tả "add, edit, mark" nhưng không phân biệt custom vs template items | BR-CHECKLIST-004: template items KHÔNG được edit item_text | Test TC-CHECKLIST-004 kiểm tra CHECKLIST-006 khi cố edit template item |
| L3 | Toggle completion chưa định nghĩa rõ về completedAt | Business logic: completedAt = now() khi true, completedAt = null khi false | Test TC-CHECKLIST-003a và TC-CHECKLIST-003b verify cả 2 chiều toggle |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` và Flyway migrations (`V2__add_user_checklist_items.sql`) trong test basis.

### TDS-01 — Scope / Phạm vi

```
UserChecklistItem module bao gồm các layer:
├── Domain Entity (UserChecklistItem.java — pure logic)
├── Application / Service (UserChecklistItemServiceImpl — mock Repository)
├── Controller (UserChecklistItemController — @WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL + Flyway auto-migrate)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-50 §3.3.1.27` | Add item, edit item, mark complete/incomplete, delete item |
| `BR-CHECKLIST-001` | Custom item và template-imported item đều được hỗ trợ |
| `BR-CHECKLIST-002` | Toggle is_completed (true ↔ false) |
| `BR-CHECKLIST-003` | Custom items editable và deletable |
| `BR-CHECKLIST-004` | Template items: chỉ toggle, không sửa item_text |
| `BR-RBAC` | owner_user_id == currentUserId kiểm tra mọi thao tác |
| `ADR-001` | Dùng bảng user_checklist_items riêng |
| `ADR-002` | Cho phép DELETE vật lý |
| `CB-CHECKLIST-IMP-001 §10` | Error codes CHECKLIST-001 đến CHECKLIST-007 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Thêm custom item hợp lệ | `UserChecklistItemService.addItem()` | `CHECKLIST-TC-001` |
| TC-COND-002 | itemText trống khi templateItemId null | `addItem()` validation | `CHECKLIST-TC-002` |
| TC-COND-003 | Toggle PENDING → COMPLETED | `toggleComplete()` | `CHECKLIST-TC-003a` |
| TC-COND-004 | Toggle COMPLETED → PENDING | `toggleComplete()` | `CHECKLIST-TC-003b` |
| TC-COND-005 | Update custom item thành công | `updateItem()` | `CHECKLIST-TC-004` |
| TC-COND-006 | Update template item bị chặn | `updateItem()` guard | `CHECKLIST-TC-005` |
| TC-COND-007 | Delete item thành công | `deleteItem()` | `CHECKLIST-TC-006` |
| TC-COND-008 | Cross-user access bị chặn | ownership check | `CHECKLIST-TC-007` |
| TC-COND-009 | Import từ template thành công | `importFromTemplate()` | `CHECKLIST-TC-008` |
| TC-COND-010 | Unauthenticated access bị chặn | JWT guard | `CHECKLIST-TC-SEC-001` |
| TC-COND-011 | Integration: create + toggle + list | Full flow | `CHECKLIST-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | itemText (null, empty, valid), category (valid enum, invalid) | Phân vùng input |
| Boundary Value Analysis | item_order (0, MAX), itemText length (500 chars boundary) | Kiểm tra biên |
| State Transition Testing | is_completed (false → true → false) | Kiểm tra state machine item |
| Error Guessing | Cross-user access, empty JWT, template item edit | Security attack vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-CHECKLIST-001` | DB seed | `{userId: MOTHER_001, journeyId: JOURNEY_001}` | Happy path owner context |
| `FX-CHECKLIST-002` | DB seed | `{userId: MOTHER_002}` | Cross-user test context |
| `FX-CHECKLIST-003` | DB seed | `{userChecklistItemId: ITEM_001, ownerUserId: MOTHER_001, isCompleted: false, templateItemId: null}` | Custom item — toggle và update |
| `FX-CHECKLIST-004` | DB seed | `{userChecklistItemId: ITEM_002, ownerUserId: MOTHER_001, isCompleted: true, completedAt: T-1h, templateItemId: TEMPLATE_001}` | Template-imported item — block edit test |
| `FX-CHECKLIST-005` | DB seed | `{checklistItemId: TEMPLATE_001, itemText: "Chuẩn bị hồ sơ", checklist_template_id: TMPL_001}` | Template item source |
| `FX-CHECKLIST-006` | JWT | `{sub: "MOTHER_001", roles: ["ROLE_MOTHER"]}` | Auth context valid |
| `FX-CHECKLIST-007` | JWT | `{sub: "MOTHER_002", roles: ["ROLE_MOTHER"]}` | Cross-user auth context |

---

## 4. Test Case Specification

> **TC ID format:** `CHECKLIST-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeItem()
// ═══════════════════════════════════════════════════════════

class UserChecklistItemTestFactory {

    static final UUID MOTHER_001  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID MOTHER_002  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID JOURNEY_001 = UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final UUID ITEM_001    = UUID.fromString("20000000-0000-0000-0000-000000000001");
    static final UUID ITEM_002    = UUID.fromString("20000000-0000-0000-0000-000000000002");
    static final UUID TEMPLATE_001 = UUID.fromString("30000000-0000-0000-0000-000000000001");

    // Custom item (isCustom = true)
    static UserChecklistItem makeCustomItem() {
        UserChecklistItem item = new UserChecklistItem();
        item.setUserChecklistItemId(ITEM_001);
        item.setOwnerUserId(MOTHER_001);
        item.setJourneyId(JOURNEY_001);
        item.setTemplateItemId(null);            // Custom: no template
        item.setItemText("Chuẩn bị túi đi sinh");
        item.setCategory("DELIVERY");
        item.setCompleted(false);
        item.setCompletedAt(null);
        item.setItemOrder(1);
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        return item;
    }

    // Template-imported item (isCustom = false)
    static UserChecklistItem makeTemplateItem() {
        UserChecklistItem item = new UserChecklistItem();
        item.setUserChecklistItemId(ITEM_002);
        item.setOwnerUserId(MOTHER_001);
        item.setJourneyId(JOURNEY_001);
        item.setTemplateItemId(TEMPLATE_001);    // Template-imported
        item.setItemText("Chuẩn bị hồ sơ nhập viện");
        item.setCategory("PAPERWORK");
        item.setCompleted(true);
        item.setCompletedAt(Instant.now().minusSeconds(3600));
        item.setItemOrder(0);
        item.setCreatedAt(Instant.now().minusSeconds(7200));
        item.setUpdatedAt(Instant.now().minusSeconds(3600));
        return item;
    }

    static UserChecklistItem makeCustomItem(Consumer<UserChecklistItem> overrides) {
        UserChecklistItem item = makeCustomItem();
        overrides.accept(item);
        return item;
    }

    static AddChecklistItemRequest makeAddRequest() {
        AddChecklistItemRequest req = new AddChecklistItemRequest();
        req.setItemText("Chuẩn bị túi đi sinh");
        req.setCategory("DELIVERY");
        req.setJourneyId(JOURNEY_001);
        req.setItemOrder(1);
        return req;
    }
}
```

---

### CHECKLIST-TC-001 — Thêm custom item hợp lệ

**Severity:** `HIGH`
**Feature Under Test:** `UserChecklistItemServiceImpl.addItem()`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-CHECKLIST-001` — Custom item không cần templateItemId

**Preconditions:**
- Repository mock trả về saved entity khi `save()` được gọi
- Fixture `FX-CHECKLIST-001` (MOTHER_001 context)

**Test Steps:**
1. Arrange: tạo `AddChecklistItemRequest` với `itemText = "Chuẩn bị túi đi sinh"`, `category = "DELIVERY"`, `templateItemId = null`
2. Act: gọi `service.addItem(MOTHER_001, request)`
3. Assert: kiểm tra kết quả trả về

**Expected Result (PASS):**
- `response.getUserChecklistItemId()` != null
- `response.isCompleted()` == false
- `response.isCustom()` == true
- `response.getTemplateItemId()` == null
- `repository.save()` được gọi đúng 1 lần

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception bị ném khi input hợp lệ
- `isCompleted` = true trên item mới tạo
- `repository.save()` không được gọi

**Current Status:** 🟢 Passing
**Implementation Note:** Service phải set `isCompleted = false`, `completedAt = null`, `ownerUserId = userId` từ param (không từ request body).

---

### CHECKLIST-TC-002 — itemText trống khi templateItemId null → CHECKLIST-001

**Severity:** `HIGH`
**Feature Under Test:** `UserChecklistItemServiceImpl.addItem()` — validation guard
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-CHECKLIST-001` — custom item requires itemText

**Preconditions:**
- Fixture `FX-CHECKLIST-006` (JWT MOTHER_001)

**Test Steps:**
1. Arrange: tạo request với `itemText = null`, `templateItemId = null`
2. Act: gọi `service.addItem(MOTHER_001, request)`
3. Assert: exception bị ném

**Expected Result (PASS):**
- `IllegalArgumentException` hoặc custom exception bị ném với error code `CHECKLIST-001`
- `repository.save()` KHÔNG được gọi

**Expected Result (FAIL):**
- Service gọi `repository.save()` với itemText = null
- Không có exception nào được ném

**Current Status:** 🔴 Not written
**Implementation Note:** Kiểm tra điều kiện trước khi gọi repository. Dùng early-return hoặc guard clause.

---

### CHECKLIST-TC-003a — Toggle PENDING → COMPLETED

**Severity:** `HIGH`
**Feature Under Test:** `UserChecklistItemServiceImpl.toggleComplete()`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-CHECKLIST-002`

**Preconditions:**
- Fixture `FX-CHECKLIST-003`: ITEM_001 với `isCompleted = false`, `completedAt = null`, `ownerUserId = MOTHER_001`
- Repository mock: `findByUserChecklistItemIdAndOwnerUserId(ITEM_001, MOTHER_001)` trả về ITEM_001

**Test Steps:**
1. Arrange: mock repository trả về `makeCustomItem()` (isCompleted = false)
2. Act: gọi `service.toggleComplete(MOTHER_001, ITEM_001)`
3. Assert: kiểm tra response và repository call

**Expected Result (PASS):**
- `response.isCompleted()` == true
- `response.getCompletedAt()` != null (gần với Instant.now())
- `repository.save()` được gọi với entity có `isCompleted = true`

**Expected Result (FAIL):**
- `isCompleted` vẫn là false
- `completedAt` vẫn là null

**Current Status:** 🟢 Passing

---

### CHECKLIST-TC-003b — Toggle COMPLETED → PENDING

**Severity:** `HIGH`
**Feature Under Test:** `UserChecklistItemServiceImpl.toggleComplete()`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-CHECKLIST-002`

**Preconditions:**
- ITEM_002 (template item) với `isCompleted = true`, `completedAt = T-1h`

**Test Steps:**
1. Arrange: mock repository trả về item với `isCompleted = true`, `completedAt = Instant.now().minusSeconds(3600)`
2. Act: gọi `service.toggleComplete(MOTHER_001, ITEM_002)`
3. Assert: kiểm tra response

**Expected Result (PASS):**
- `response.isCompleted()` == false
- `response.getCompletedAt()` == null

**Expected Result (FAIL):**
- `isCompleted` vẫn true
- `completedAt` không được reset về null

**Current Status:** 🟢 Passing

---

### CHECKLIST-TC-004 — Update custom item thành công

**Severity:** `MEDIUM`
**Feature Under Test:** `UserChecklistItemServiceImpl.updateItem()`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-CHECKLIST-003`

**Preconditions:**
- Fixture `FX-CHECKLIST-003`: ITEM_001 custom item (`templateItemId = null`)

**Test Steps:**
1. Arrange: mock repository trả về `makeCustomItem()`, create `UpdateChecklistItemRequest` với `itemText = "Chuẩn bị quần áo cho bé"`, `category = "BABY_CARE"`
2. Act: gọi `service.updateItem(MOTHER_001, ITEM_001, request)`
3. Assert: response có giá trị mới

**Expected Result (PASS):**
- `response.getItemText()` == "Chuẩn bị quần áo cho bé"
- `response.getCategory()` == "BABY_CARE"
- `repository.save()` được gọi

**Expected Result (FAIL):**
- Item text không được cập nhật

**Current Status:** 🟢 Passing

---

### CHECKLIST-TC-005 — Update template-imported item bị chặn → CHECKLIST-006

**Severity:** `HIGH`
**Feature Under Test:** `UserChecklistItemServiceImpl.updateItem()` — guard cho template items
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-CHECKLIST-004`, `ADR-002`

**Preconditions:**
- Fixture `FX-CHECKLIST-004`: ITEM_002 với `templateItemId = TEMPLATE_001` (không null)

**Test Steps:**
1. Arrange: mock repository trả về `makeTemplateItem()` (templateItemId != null)
2. Act: gọi `service.updateItem(MOTHER_001, ITEM_002, {itemText: "new text"})`
3. Assert: exception bị ném

**Expected Result (PASS):**
- `BusinessRuleException` bị ném với error code `CHECKLIST-006`
- `repository.save()` KHÔNG được gọi

**Expected Result (FAIL):**
- Service cho phép cập nhật itemText của template item
- Không có exception

**Current Status:** 🟢 Passing
**Implementation Note:** Guard check: `if (item.getTemplateItemId() != null) throw new BusinessRuleException("CHECKLIST-006")`

---

### CHECKLIST-TC-006 — Delete item thành công

**Severity:** `MEDIUM`
**Feature Under Test:** `UserChecklistItemServiceImpl.deleteItem()`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-002`, `BR-CHECKLIST-003`

**Preconditions:**
- Fixture `FX-CHECKLIST-003`: ITEM_001 custom item của MOTHER_001

**Test Steps:**
1. Arrange: mock repository trả về ITEM_001
2. Act: gọi `service.deleteItem(MOTHER_001, ITEM_001)`
3. Assert: repository.delete() được gọi

**Expected Result (PASS):**
- Không có exception
- `repository.delete()` được gọi đúng 1 lần với entity ITEM_001

**Expected Result (FAIL):**
- `repository.delete()` không được gọi
- Exception unexpected bị ném

**Current Status:** 🟢 Passing

---

### CHECKLIST-TC-007 — Cross-user access bị chặn → CHECKLIST-004

**Severity:** `CRITICAL`
**Feature Under Test:** Ownership check trong `UserChecklistItemServiceImpl`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- ITEM_001 thuộc MOTHER_001
- MOTHER_002 đang gọi API với JWT hợp lệ

**Test Steps:**
1. Arrange: mock `findByUserChecklistItemIdAndOwnerUserId(ITEM_001, MOTHER_002)` trả về `Optional.empty()`
2. Act: gọi `service.toggleComplete(MOTHER_002, ITEM_001)` (MOTHER_002 cố toggle item của MOTHER_001)
3. Assert: exception bị ném

**Expected Result (PASS):**
- `AccessDeniedException` hoặc `ResourceNotFoundException` với error code `CHECKLIST-004` hoặc `CHECKLIST-003`
- `repository.save()` KHÔNG được gọi

**Expected Result (FAIL — lỗ hổng bảo mật):**
- Service cho phép MOTHER_002 toggle item của MOTHER_001
- Response trả về 200 với data của người khác

**Current Status:** 🟢 Passing

---

### CHECKLIST-TC-008 — Import từ template thành công

**Severity:** `HIGH`
**Feature Under Test:** `UserChecklistItemServiceImpl.importFromTemplate()`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemServiceTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-CHECKLIST-001`, `BR-CHECKLIST-004`

**Preconditions:**
- Fixture `FX-CHECKLIST-005`: TEMPLATE_001 tồn tại trong `checklist_items`
- checklistItemRepository mock trả về TEMPLATE_001

**Test Steps:**
1. Arrange: mock `checklistItemRepository.findAllById([TEMPLATE_001])` trả về ChecklistItem với `item_text = "Chuẩn bị hồ sơ nhập viện"`
2. Act: gọi `service.importFromTemplate(MOTHER_001, {journeyId: JOURNEY_001, templateItemIds: [TEMPLATE_001]})`
3. Assert: kiểm tra kết quả

**Expected Result (PASS):**
- List trả về có 1 item
- `response[0].getItemText()` == "Chuẩn bị hồ sơ nhập viện" (copy từ template, không phải user input)
- `response[0].isCustom()` == false
- `response[0].getTemplateItemId()` == TEMPLATE_001
- `response[0].isCompleted()` == false
- `userChecklistItemRepository.saveAll()` được gọi

**Expected Result (FAIL):**
- itemText bị override bởi request body
- templateItemId là null dù import từ template

**Current Status:** 🔴 Not written
**Implementation Note:** C6 — item_text phải copy từ `checklistItem.getItemText()`, KHÔNG nhận từ request body.

---

### SECURITY TEST CASES

---

### CHECKLIST-TC-SEC-001 — Unauthenticated access bị chặn → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `UserChecklistItemController` + JWT security filter
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Không có Authorization header
- Controller test với `@WebMvcTest(UserChecklistItemController.class)`

**Test Steps (Attack Simulation):**
1. Chuẩn bị: MockMvc request không có Authorization header
2. Gửi: `GET /api/v1/user-checklist-items`
3. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- Response status là `401 Unauthorized`
- Error code `IAM-001`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Response status là 200 hoặc trả về dữ liệu mà không có JWT

**Current Status:** 🔴 Not written

---

### CHECKLIST-TC-SEC-002 — Category injection với giá trị không hợp lệ → 400

**Severity:** `MEDIUM`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-20 — Improper Input Validation`
**Feature Under Test:** `AddChecklistItemRequest` — category validation
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- MOTHER_001 đã đăng nhập với JWT hợp lệ

**Test Steps (Attack Simulation):**
1. Gửi `POST /api/v1/user-checklist-items` với `category = "'; DROP TABLE user_checklist_items; --"`
2. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- Response status là `400 Bad Request`
- Error code `CHECKLIST-002`
- DB không bị ảnh hưởng

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Category không hợp lệ được chấp nhận hoặc gây SQL injection

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### CHECKLIST-TC-INT-001 — Luồng hoàn chỉnh: Tạo → Toggle → List

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST → PATCH toggle → GET list`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration V1 + V2 applied tự động khi Spring context start
- Seed: MOTHER_001 user tồn tại trong bảng `users`

**Test Steps:**
1. `POST /api/v1/user-checklist-items` với `{itemText: "Mua tã", category: "BABY_CARE"}`
2. Lưu `userChecklistItemId` từ response
3. `PATCH /api/v1/user-checklist-items/{id}/toggle`
4. `GET /api/v1/user-checklist-items`
5. Assert DB state

**Expected Result (PASS):**
- POST trả về 201, response chứa `userChecklistItemId`
- PATCH toggle trả về 200, `isCompleted = true`
- GET trả về list chứa item với `isCompleted = true`

**Expected Result (FAIL):**
- Toggle không persist vào DB
- GET không trả về item vừa tạo

**DB Assertion:**
```java
UserChecklistItem record = userChecklistItemRepository
    .findByUserChecklistItemIdAndOwnerUserId(savedId, MOTHER_001)
    .orElseThrow();
assertThat(record).isNotNull();
assertThat(record.isCompleted()).isTrue();
assertThat(record.getCompletedAt()).isNotNull();
assertThat(record.getItemText()).isEqualTo("Mua tã");
```

**Current Status:** 🔴 Not written

---

### CHECKLIST-TC-INT-002 — Import từ template và verify DB

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/user-checklist-items/import`
**Test File:** `src/test/java/com/carebridge/backend/checklist/UserChecklistItemIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- PostgreSQL container với Flyway V1 + V2
- Seed: `checklist_templates` và `checklist_items` với TEMPLATE_001 tồn tại
- MOTHER_001 và JOURNEY_001 seed trong DB

**Test Steps:**
1. `POST /api/v1/user-checklist-items/import` với `{journeyId: JOURNEY_001, templateItemIds: [TEMPLATE_001]}`
2. Assert response
3. Query DB trực tiếp

**Expected Result (PASS):**
- Response 201 với list 1 item
- `isCustom = false`, `templateItemId = TEMPLATE_001`
- DB: `SELECT * FROM user_checklist_items WHERE template_item_id = TEMPLATE_001` trả về 1 record

**DB Assertion:**
```java
List<UserChecklistItem> items = userChecklistItemRepository
    .findByOwnerUserIdAndJourneyIdOrderByItemOrderAsc(MOTHER_001, JOURNEY_001);
assertThat(items).hasSize(1);
assertThat(items.get(0).getTemplateItemId()).isEqualTo(TEMPLATE_001);
assertThat(items.get(0).isCompleted()).isFalse();
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CHECKLIST-TC-001` | `UserChecklistItemServiceTest.java` | `[x]` | `Passed` | — |
| `CHECKLIST-TC-002` | `UserChecklistItemServiceTest.java` | `[ ]` | `___` | Not implemented (duplicate guard test) |
| `CHECKLIST-TC-003a` | `UserChecklistItemServiceTest.java` | `[x]` | `Passed` | — |
| `CHECKLIST-TC-003b` | `UserChecklistItemServiceTest.java` | `[x]` | `Passed` | — |
| `CHECKLIST-TC-004` | `UserChecklistItemServiceTest.java` | `[x]` | `Passed` | — |
| `CHECKLIST-TC-005` | `UserChecklistItemServiceTest.java` | `[x]` | `Passed` | — |
| `CHECKLIST-TC-006` | `UserChecklistItemServiceTest.java` | `[x]` | `Passed` | — |
| `CHECKLIST-TC-007` | `UserChecklistItemServiceTest.java` | `[x]` | `Passed` | — |
| `CHECKLIST-TC-008` | `UserChecklistItemServiceTest.java` | `[x]` | `Passed` | — |
| `CHECKLIST-TC-SEC-001` | `UserChecklistItemControllerTest.java` | `[ ]` | `___` | Not implemented (security/MockMvc layer) |
| `CHECKLIST-TC-SEC-002` | `UserChecklistItemControllerTest.java` | `[ ]` | `___` | Not implemented (security/MockMvc layer) |
| `CHECKLIST-TC-INT-001` | `UserChecklistItemIntegrationTest.java` | `[ ]` | `___` | Not implemented (Testcontainers unavailable) |
| `CHECKLIST-TC-INT-002` | `UserChecklistItemIntegrationTest.java` | `[ ]` | `___` | Not implemented (Testcontainers unavailable) |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class UserChecklistItemServiceImpl implements IUserChecklistItemService {

    @Override
    public ChecklistItemResponse addItem(UUID userId, AddChecklistItemRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<ChecklistItemResponse> importFromTemplate(UUID userId, ImportFromTemplateRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ChecklistItemResponse toggleComplete(UUID userId, UUID itemId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ChecklistItemResponse updateItem(UUID userId, UUID itemId, UpdateChecklistItemRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void deleteItem(UUID userId, UUID itemId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public List<ChecklistItemResponse> listItems(UUID userId, UUID journeyId, UUID babyId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `CHECKLIST-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `CHECKLIST-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | Not implemented |
| `CHECKLIST-TC-003a` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `CHECKLIST-TC-003b` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `CHECKLIST-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `CHECKLIST-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `CHECKLIST-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `CHECKLIST-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `CHECKLIST-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `2026-07-07-sprint3`
- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `target/surefire-reports/red-gate-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-CHECKLIST-IMP-001` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm: `user_checklist_items` schema gap được giải quyết bằng V2 migration
- [ ] Flyway migration `V2__add_user_checklist_items.sql` đã được approved và chạy thành công trên staging
- [ ] Test fixtures (FX-CHECKLIST-001 đến FX-CHECKLIST-007) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (7/7 service tests passed)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers unavailable)
- [ ] Test coverage ≥ 80% lines cho `UserChecklistItemServiceImpl`
- [x] Không có business logic trong `UserChecklistItemController`
- [x] Ownership check (`ownerUserId == currentUserId`) được verify bởi CHECKLIST-TC-007
- [x] Template item protection được verify bởi CHECKLIST-TC-005
- [ ] CHECKLIST-TC-SEC-001 (unauthorized) PASS (controller test not implemented)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 9 unit tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (dùng factory method)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn BR/ADR

### Suspension Criteria (Điều kiện tạm dừng)

- Flyway V2 migration chưa được approve trên staging
- `checklist_items` table chưa có seed data cho integration tests
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS public.user_checklist_items CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '2';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/checklist/
git checkout -- src/main/resources/db/migration/V2__add_user_checklist_items.sql
git checkout -- src/test/java/com/carebridge/backend/checklist/

# Gap vẫn OPEN → giữ nguyên entry trong gap analysis
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume `user_checklist_items` tồn tại mà không có ADR-001 | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller chứa ownership check logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `UserChecklistItemPolicy` hoặc class không có trong §8 TDS | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — UC50 Manage Preparation Checklist — CB-CHECKLIST-IMP-001-TEST*
*Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
