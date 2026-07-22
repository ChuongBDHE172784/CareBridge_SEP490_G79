# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification — UC-243 Manage Checklist Templates (Content Admin CRUD)

| Field              | Value                                   |
| ------------------ | ---------------------------------------- |
| **Document ID**    | `CB-CONTENT-IMP-011`                    |
| **Version**        | `1.0`                                   |
| **Date**           | `2026-07-22`                            |
| **Status**         | `Approved`                              |
| **Document Owner** | `HuyND`                                 |
| **Author**         | `AI Agent — Claude`                     |
| **Reviewed by**    | `[x] HuyND`                             |
| **DPO Sign-off**   | `N/A — Data Classification: Internal, no PII` |
| **Approved by**    | `[x] HuyND — 2026-07-22, "approved with defaults"` |
| **Last Review**    | `2026-07-22`                            |
| **Based on EDS**   | `v2.0`                                  |

---

## CHANGELOG

| Ngày       | Người thực hiện     | Nội dung thay đổi                          |
| ---------- | -------------------- | -------------------------------------------- |
| 2026-07-22 | AI Agent — Claude   | Tạo tài liệu lần đầu — Draft, chờ review     |
| 2026-07-22 | HuyND               | Duyệt với đề xuất mặc định ("approved with defaults") cho cả §11.1 và §11.2 → Status: Approved |
| 2026-07-22 | AI Agent — Claude   | 3 điều chỉnh kỹ thuật trước khi code (xem §14): (1) `decide()` cho checklist template tách thành service/controller riêng thay vì mở rộng `ContentApprovalService.decide()` — tránh ép field `versionNo`/`publishedAt` (không tồn tại trên `ChecklistTemplate`) vào response dùng chung; (2) thêm `GET /api/v1/admin/checklist-templates` (list, có filter status/stage) — cần thiết để `/admin/content-approval-queue` hiển thị được checklist template đang chờ duyệt; (3) class-level `@PreAuthorize` của `AdminChecklistTemplateController` đổi thành `hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN')` (giống `AdminContentController`) để SYSTEM_ADMIN đọc được list/detail, ghi vẫn override về CONTENT_ADMIN-only |

---

## 0. Bối cảnh phát sinh (tại sao tài liệu này tồn tại)

Trong lúc QA lại `/content/checklists` (Content Admin portal), phát hiện: nút "Tạo Checklist" dẫn đến form tạo content chung (`CreateContentPage`, type=CHECKLIST) — form này ghi vào bảng `content_items`. Nhưng trang danh sách `/content/checklists` lại đọc từ bảng **khác**: `checklist_templates` / `checklist_items` (qua `ContentController.getChecklists()` → `ContentService.getChecklists(stage)`). Hai bảng này không liên kết (`checklist_templates.content_item_id` tồn tại nhưng luôn NULL) — nút Xem/Sửa trên `/content/checklists` từng bị vô hiệu hóa tạm thời (disabled, tooltip "sắp ra mắt") vì lý do này.

**Phát hiện quan trọng khi điều tra thêm (không phải giả định — đã verify bằng code):**
`checklist_templates` / `checklist_items` **không phải bảng mồ côi** — đây chính là dữ liệu nền cho **UC-50 (Manage Preparation Checklist)**, một tính năng đã triển khai và đang chạy thật cho vai trò MOTHER: `UserChecklistItemServiceImpl.importFromTemplate()` (package `com.carebridge.backend.checklist`) gọi thẳng `ChecklistItemRepository.findById(templateItemId)` (cùng entity `ChecklistItem` ở package `content`) để mẹ "nhập" mục checklist mẫu vào checklist cá nhân (`user_checklist_items`). Nói cách khác: **`checklist_templates`/`checklist_items` đã là nguồn thật, đang được dùng thật — chỉ là chưa có API nào cho Content Admin tạo/sửa/xóa chúng** (hiện chỉ có `DevDataSeeder`, dev-only, tạo được).

→ Do đó phạm vi tài liệu này **hẹp hơn** so với "hợp nhất 2 hệ thống độc lập": không cần di trú dữ liệu giữa hai mô hình dữ liệu khác nhau, vì chúng chưa từng đại diện cho cùng một khái niệm nghiệp vụ. Việc cần làm:

1. Xây CRUD (Create/Update/Archive) cho `checklist_templates`/`checklist_items`, dành cho `CONTENT_ADMIN`, tái sử dụng entity/repository đã có.
2. Bỏ tùy chọn `CHECKLIST` khỏi form tạo nội dung chung (`CreateContentPage` / `ContentType` dropdown) vì tạo một `content_items` row kiểu CHECKLIST không phục vụ mục đích thật nào (không liên kết tới checklist thật mà mẹ dùng) — tài liệu này không xóa `ContentType.CHECKLIST` khỏi enum (tránh phá dữ liệu demo hiện có), chỉ ẩn khỏi UI tạo mới.
3. Frontend `/content/checklists`: bật lại nút Xem/Sửa, thêm form tạo/sửa đúng với model thật (tên, mô tả, giai đoạn, danh sách mục có thứ tự + bắt buộc/không bắt buộc) — không dùng chung form với Article/FAQ.

---

## 1. Tổng quan Module

| Field                     | Value                                                                                              |
| ------------------------- | ---------------------------------------------------------------------------------------------------|
| **Module Name**           | `Manage Checklist Templates (Admin CRUD)`                                                          |
| **Bounded Context**       | `content` (entity đã tồn tại), tiêu thụ bởi `checklist` (UC-50)                                    |
| **UC ID**                 | `UC-243`                                                                                            |
| **Platform**              | `Admin Web Portal (React + Vite)`                                                                  |
| **Data Classification**   | `Internal` (không có PII — tên/mô tả checklist là nội dung biên tập, không phải dữ liệu người dùng)|
| **Compliance Scope**      | `BR-RBAC (CONTENT_ADMIN)` — không có yêu cầu GDPR/PDPA riêng cho module này                        |
| **Upstream Dependencies** | `security (JWT Auth)`, `audit (AuditService)`                                                      |
| **Downstream Consumers**  | `UC-50 (Manage Preparation Checklist — MOTHER)` qua `ChecklistItemRepository`, `ContentController.getChecklists()` (public read, đã tồn tại) |

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại          | Mô tả yêu cầu                                                        | Thành phần Code                                    | Compliance Target | ADR liên quan |
| -------------- | ------------- | ---------------------------------------------------------------------| ---------------------------------------------------| ------------------ | ------------- |
| UC-243         | User Story    | Content Admin tạo/sửa/xóa checklist template + items                 | `AdminChecklistTemplateController`                 | BR-RBAC            | ADR-CHK-001   |
| BR-RBAC-WRITE  | Business Rule | Chỉ CONTENT_ADMIN được tạo/sửa/xóa checklist template                 | `@PreAuthorize("hasRole('CONTENT_ADMIN')")`        | BR-RBAC            | ADR-CHK-001   |
| BR-NO-HARD-DEL | Business Rule | Không xóa cứng — "xóa" = archive (status=ARCHIVED, cần lý do)         | `AdminChecklistTemplateServiceImpl.archive()`      | Audit              | ADR-CHK-002 (kế thừa ADR-001 của UC-107) |
| BR-AUDIT       | Business Rule | Mọi tạo/sửa/xóa phải ghi audit log                                    | `AdminChecklistTemplateServiceImpl` → `AuditService`| Audit             | ADR-CHK-003   |
| DOWNSTREAM-50  | Constraint    | Không được đổi cấu trúc bảng `checklist_items` vì UC-50 đang FK vào đó | Không có migration DDL trong tài liệu này          | —                  | ADR-CHK-004   |

---

## 3. Architecture Decision Records (ADR)

### ADR-CHK-001 — Controller riêng cho Admin CRUD, tách khỏi read-path công khai

| Field          | Value               |
| -------------- | ------------------- |
| **Status**     | `Proposed`          |
| **Deciders**   | `HuyND — Tech Lead` |
| **Date**       | `2026-07-22`         |

#### Bối cảnh
`GET /api/v1/content/checklists` (public, mọi role đã login) đã tồn tại và đang được `ChecklistListPage` (admin) lẫn (tiềm năng) client khác dùng để đọc. Ghi (create/update/archive) cần quyền CONTENT_ADMIN riêng — theo đúng pattern đã áp dụng cho `ContentItem` (ADR-005, UC-105): `AdminContentController` (`/api/v1/admin/content`, ghi) tách khỏi `ContentController` (`/api/v1/content`, đọc công khai).

#### Quyết định
Tạo `AdminChecklistTemplateController` tại `/api/v1/admin/checklist-templates`, class-level `@PreAuthorize("hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN')")` (điều chỉnh §14 — cần cho SYSTEM_ADMIN đọc list/detail phục vụ hàng đợi phê duyệt), các endpoint ghi (`POST`/`PUT`/`archive`) override method-level về `@PreAuthorize("hasRole('CONTENT_ADMIN')")` — giống hệt pattern `AdminContentController`. Không sửa `ContentController.getChecklists()` (giữ nguyên hành vi đọc hiện tại — không breaking change cho UC-50 hay frontend đang gọi).

#### Hệ quả
**Tích cực:** nhất quán với pattern đã có (ADR-005); không đổi hành vi đọc hiện tại → an toàn cho UC-50.
**Trade-off:** thêm 1 controller/service — chấp nhận được, đổi lại rõ ràng về security boundary.

---

### ADR-CHK-002 — "Xóa" = archive (status=ARCHIVED), không hard-delete

| Field          | Value               |
| -------------- | ------------------- |
| **Status**     | `Proposed`          |
| **Deciders**   | `HuyND — Tech Lead` |
| **Date**       | `2026-07-22`         |

#### Bối cảnh
`checklist_items.checklist_item_id` được `user_checklist_items.template_item_id` tham chiếu (FK). Nếu hard-delete một `ChecklistTemplate`/`ChecklistItem` đang được mẹ nào đó đã "import" trước đó, dữ liệu lịch sử của mẹ (`user_checklist_items`) sẽ tham chiếu tới ID không còn tồn tại (mồ côi) hoặc phá vỡ nếu FK có `ON DELETE CASCADE`/`RESTRICT`. Đây cũng là pattern y hệt lý do UC-107 (`HideOrDeleteContent`) đã chọn archive thay vì DELETE cho `ContentItem`.

#### Quyết định
Tái sử dụng `ContentStatus.ARCHIVED` đã có sẵn trên `ChecklistTemplate.status` (thêm ở phiên làm việc trước — xem `ChecklistTemplateResponse`). "Xóa" một template = set `status = ARCHIVED` + bắt buộc `reason` (giống `HideContentRequest`). Không xóa row, không xóa `checklist_items` con.

**Điều cần xác nhận với UC-50 (ghi nhận, không tự quyết trong tài liệu này):** hiện `UserChecklistItemServiceImpl.importFromTemplate()` KHÔNG kiểm tra `status` của template khi import — mẹ có thể import từ một `ChecklistItem` dù template cha đang ARCHIVED. Tài liệu này **không** thay đổi hành vi đó (ngoài phạm vi UC-243); nêu ra để Tech Lead cân nhắc có cần một UC riêng để gate import theo status hay không.

---

### ADR-CHK-003 — Audit log bắt buộc, tái dùng `AuditService` có sẵn

| Field          | Value               |
| -------------- | ------------------- |
| **Status**     | `Proposed`          |
| **Deciders**   | `HuyND — Tech Lead` |
| **Date**       | `2026-07-22`         |

#### Quyết định
Mọi `create`/`update`/`archive` gọi `AuditService.log(...)` trong cùng transaction, giống pattern `AdminContentServiceImpl` (ADR-007, UC-105).

---

### ADR-CHK-004 — Bỏ `CHECKLIST` khỏi form tạo nội dung chung, không đổi schema `content_items`

| Field          | Value               |
| -------------- | ------------------- |
| **Status**     | `Proposed`          |
| **Deciders**   | `HuyND — Tech Lead` |
| **Date**       | `2026-07-22`         |

#### Bối cảnh
`ContentType.CHECKLIST` vẫn tồn tại trong enum và có dữ liệu demo hiện có (seed qua `DevDataSeeder`) trong `content_items`. Không có UC nào đọc các row này theo cách khác với ARTICLE/FAQ — chúng không phục vụ UC-50. Giữ nguyên tùy chọn này trong `CreateContentPage` gây nhầm lẫn giống lỗi đã phát hiện (nút "Tạo Checklist" tạo nhầm loại dữ liệu).

#### Các phương án
| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|---|---|---|---|
| A | Xóa `CHECKLIST` khỏi `ContentType` enum + migration xóa/di trú các row hiện có | Triệt để | Cần migration, rủi ro cao hơn, ảnh hưởng `/content/list` filter hiện tại |
| B (khuyến nghị) | Giữ enum + dữ liệu cũ nguyên trạng (không phá gì); chỉ ẩn `CHECKLIST` khỏi dropdown loại nội dung khi **tạo mới** ở `CreateContentPage` | An toàn, không migration, dễ rollback (revert 1 file frontend) | `content_items` vẫn còn "rác" CHECKLIST cũ (chấp nhận được, không ảnh hưởng chức năng) |

#### Quyết định
Chọn **Phương án B**.

---

## 4. Non-Functional Requirements & SLA

Module nội bộ, admin-only, tần suất thấp (ước tính < 20 lượt tạo/sửa mỗi ngày). Không có yêu cầu NFR đặc biệt ngoài baseline chung của hệ thống.

| Category     | Requirement                 | Target SLA | Ghi chú |
| ------------ | ---------------------------- | ---------- | ------- |
| Latency      | API response (p99)           | `< 500ms`  | Tương tự UC-105 |
| Data Integrity | Archive không xóa `checklist_items` con | 100% | Test TC-INT tương ứng |
| Security     | Chỉ CONTENT_ADMIN được ghi   | 100%       | Test TC-SEC |

*(§4.2 Retention, §4.3 Encryption, §4.4 Scalability: N/A — không có PII, không có yêu cầu riêng ngoài baseline hạ tầng hiện có.)*

---

## 5. Static Modeling

### 5.1. Entities (đã tồn tại — không đổi)

```java
// ChecklistTemplate.java — com.carebridge.backend.content.entity (KHÔNG THAY ĐỔI)
// id, name, stage(ContentStage), status(ContentStatus, default DRAFT), description, createdAt, updatedAt

// ChecklistItem.java — com.carebridge.backend.content.entity (KHÔNG THAY ĐỔI)
// id, template(ChecklistTemplate FK), itemText, order(Integer), isRequired(Boolean), createdAt, updatedAt
```

### 5.2. New DTOs

```java
// CreateChecklistTemplateRequest.java — com.carebridge.backend.content.dto.request
public record CreateChecklistTemplateRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    @NotNull ContentStage stage,
    @Valid List<ChecklistItemRequest> items   // §11.2: rỗng/null đều hợp lệ (draft rỗng cho phép) — KHÔNG @NotEmpty
) {}

// ChecklistItemRequest.java — com.carebridge.backend.content.dto.request
public record ChecklistItemRequest(
    @NotBlank @Size(max = 500) String itemText,
    @NotNull Integer order,
    @NotNull Boolean isRequired
) {}

// UpdateChecklistTemplateRequest.java — com.carebridge.backend.content.dto.request
public record UpdateChecklistTemplateRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    @NotNull ContentStage stage,
    @NotNull ContentStatus status,          // DRAFT hoặc PENDING_REVIEW — giống BR-CNT-006 của ContentItem (không cho set APPROVED trực tiếp — xem §17 constraint)
    @Valid List<ChecklistItemRequest> items  // full replace, giống ContentItem.sources — null = không đổi items, [] = xóa hết mục
) {}

// HideChecklistTemplateRequest.java — com.carebridge.backend.content.dto.request
public record HideChecklistTemplateRequest(
    @NotBlank @Size(max = 1000) String reason
) {}

// Response: TÁI SỬ DỤNG ChecklistTemplateResponse / ChecklistItemResponse đã có (đã bổ sung field `status` ở phiên trước)
```

### 5.3. Service / Controller

```java
// AdminChecklistTemplateService.java — com.carebridge.backend.content.service
public interface AdminChecklistTemplateService {
    Page<ChecklistTemplateResponse> list(ContentStatus status, ContentStage stage, Pageable pageable); // §14 addendum — powers admin table + approval queue
    ChecklistTemplateResponse getById(UUID id); // admin detail/edit view — public getChecklists() only returns list shape today
    ChecklistTemplateResponse create(CreateChecklistTemplateRequest request, UUID adminUserId);
    ChecklistTemplateResponse update(UUID id, UpdateChecklistTemplateRequest request, UUID adminUserId);
    HideChecklistTemplateResponse archive(UUID id, HideChecklistTemplateRequest request, UUID adminUserId);
}

// AdminChecklistTemplateController.java — com.carebridge.backend.content.controller
@RestController
@RequestMapping("/api/v1/admin/checklist-templates")
@PreAuthorize("hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN')")   // §14 — read open to both, write overridden below
public class AdminChecklistTemplateController {
    @GetMapping                                                    // list (status/stage filter + paging)
    @GetMapping("/{id}")                                           // detail (for edit form)
    @PostMapping @PreAuthorize("hasRole('CONTENT_ADMIN')")         // create
    @PutMapping("/{id}") @PreAuthorize("hasRole('CONTENT_ADMIN')") // update
    @PostMapping("/{id}/archive") @PreAuthorize("hasRole('CONTENT_ADMIN')") // "delete" = archive
}

// ChecklistTemplateApprovalService.java / Impl / ChecklistTemplateApprovalController.java — §14 addendum
// mirrors ContentApprovalService/ContentApprovalController exactly (same base path, separate class,
// SYSTEM_ADMIN-only), reusing ContentDecisionRequest as the request body:
public interface ChecklistTemplateApprovalService {
    ChecklistTemplateDecisionResponse decide(UUID id, ContentDecisionRequest request, Principal principal);
}
@RestController
@RequestMapping("/api/v1/admin/checklist-templates")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class ChecklistTemplateApprovalController {
    @PostMapping("/{id}/decision")
}
```

**Lưu ý quan trọng khi implement — ghi từ bài học phiên trước (không lặp lại 2 bug đã fix):**
1. `ChecklistItem` list khi build lại (update: xóa hết items cũ, insert lại theo request) PHẢI dùng `ArrayList`/`Collectors.toCollection(ArrayList::new)`, KHÔNG dùng `Stream.toList()` nếu field đó có khả năng bị Hibernate quản lý qua `@ElementCollection`/`@OneToMany` merge — xem bug đã fix trong `AdminContentServiceImpl.updateContent()` (immutable list → `UnsupportedOperationException` khi flush). `ChecklistItem` là `@OneToMany` con riêng (không phải `@ElementCollection`) nên cách update items nên là: xóa các `ChecklistItem` cũ qua repository (`checklistItemRepository.deleteAll(existingItems)`) rồi `saveAll()` items mới — KHÔNG gán thẳng list vào field của `ChecklistTemplate` (không có quan hệ `@OneToMany` ngược trên entity này hiện tại).
2. Nếu thêm filter theo `type`/`stage`/`status` cho endpoint list mới, dùng pattern `findByAdminFilters` (null-safe AND, đã có sẵn cho `ContentItem`) — KHÔNG lặp lại lỗi if/else-if loại trừ lẫn nhau đã fix ở `AdminContentServiceImpl.getStaffContents`.

---

## 6. Dynamic Modeling

### 6.1. Sequence — Create (Happy Path)

```
Content Admin -> AdminChecklistTemplateController: POST /api/v1/admin/checklist-templates
  {name, description, stage, items: [{itemText, order, isRequired}, ...]}
Controller -> Controller: @PreAuthorize CONTENT_ADMIN — PASS
Controller -> Service: create(request, adminUserId)
Service -> Service: status = DRAFT (hardcode, giống ADR-006 của ContentItem)
Service -> ChecklistTemplateRepository: save(template)
Service -> ChecklistItemRepository: saveAll(items linked to saved template)
Service -> AuditService: log(CHECKLIST_TEMPLATE_CREATED, adminUserId, template.id)
Service --> Controller: ChecklistTemplateResponse
Controller --> Admin: 201 Created
```

### 6.2. Sequence — Archive ("Delete")

```
Content Admin -> Controller: POST /api/v1/admin/checklist-templates/{id}/archive {reason}
Controller -> Service: archive(id, request, adminUserId)
Service -> Repository: findById(id) — 404 nếu không tồn tại (CHKTPL-003)
Service -> Service: guard — nếu status đã ARCHIVED → 409 (CHKTPL-006, giống CNT-006 của ContentItem)
Service -> Service: reason blank? → 400 (CHKTPL-007)
Service -> Repository: save(template với status=ARCHIVED)
Service -> AuditService: log(CHECKLIST_TEMPLATE_ARCHIVED, ...)
Service --> Controller: 200 OK
note right: checklist_items con KHÔNG bị xóa — user_checklist_items (UC-50) không bị ảnh hưởng
```

### 6.3. State Machine

Tái sử dụng state machine `ContentStatus` đã có (DRAFT → PENDING_REVIEW → APPROVED → ARCHIVED), invariant giống ContentItem: không transition từ ARCHIVED quay lại (terminal state).

---

## 7. Domain Event Catalog

| Event Name                    | Trigger            | Publisher                          | Payload chính                          |
| ------------------------------ | ------------------- | ------------------------------------| --------------------------------------- |
| `CHECKLIST_TEMPLATE_CREATED`  | Tạo template mới    | `AdminChecklistTemplateServiceImpl` | templateId, adminUserId, stage         |
| `CHECKLIST_TEMPLATE_UPDATED`  | Sửa template        | `AdminChecklistTemplateServiceImpl` | templateId, adminUserId                |
| `CHECKLIST_TEMPLATE_ARCHIVED` | Archive ("xóa")     | `AdminChecklistTemplateServiceImpl` | templateId, adminUserId, reason        |

*(Thực hiện qua `AuditService.log(AuditAction.<new enum value>, ...)` — cần thêm 3 giá trị vào `AuditAction` enum, giống cách `AuditAction.CONTENT_CREATED`/`CONTENT_UPDATED`/`CONTENT_UNPUBLISHED` đã có cho ContentItem.)*

---

## 8. API Specification

### 8.1. Endpoints

| Method | Path                                              | Roles          | Idempotent? |
| ------ | -------------------------------------------------- | -------------- | ----------- |
| `GET`  | `/api/v1/admin/checklist-templates` (§14 addendum) | `CONTENT_ADMIN`, `SYSTEM_ADMIN` | Yes |
| `GET`  | `/api/v1/admin/checklist-templates/{id}`           | `CONTENT_ADMIN`, `SYSTEM_ADMIN` | Yes |
| `POST` | `/api/v1/admin/checklist-templates`                | `CONTENT_ADMIN`| No          |
| `PUT`  | `/api/v1/admin/checklist-templates/{id}`           | `CONTENT_ADMIN`| Yes         |
| `POST` | `/api/v1/admin/checklist-templates/{id}/archive`   | `CONTENT_ADMIN`| No (guarded — 409 nếu đã archived) |
| `POST` | `/api/v1/admin/checklist-templates/{id}/decision` (§14 addendum) | `SYSTEM_ADMIN` | No (guarded — 409 nếu không PENDING_REVIEW) |

*(`GET /api/v1/content/checklists` — public list, đã tồn tại, KHÔNG đổi trong tài liệu này.)*

### 8.2. Request/Response mẫu

**POST /api/v1/admin/checklist-templates**
```json
{
  "name": "Checklist khám thai tháng 3",
  "description": "Các mốc khám cần thực hiện trong tháng thứ 3",
  "stage": "PREGNANCY",
  "items": [
    { "itemText": "Siêu âm đo độ mờ da gáy", "order": 1, "isRequired": true },
    { "itemText": "Xét nghiệm Double test", "order": 2, "isRequired": true }
  ]
}
```
**201 Created** → `ChecklistTemplateResponse` (id, name, stage, status="DRAFT", description, items[])

---

## 9. Bảng mã lỗi

| Code          | HTTP | Message (VI)                          | Trigger |
| ------------- | ---- | -------------------------------------- | ------- |
| `CHKTPL-001`  | 400  | Dữ liệu không hợp lệ                   | name blank/quá dài; stage sai enum; item thiếu itemText/order |
| `CHKTPL-002`  | 403  | Không đủ quyền                         | Role khác CONTENT_ADMIN |
| `CHKTPL-003`  | 404  | Không tìm thấy checklist template      | id không tồn tại |
| `CHKTPL-004`  | 400  | status không hợp lệ khi cập nhật       | client gửi status=APPROVED trực tiếp (chỉ SYSTEM_ADMIN qua approval flow mới được set APPROVED — xem Open Question §11.1) |
| `CHKTPL-005`  | 400  | Lý do bắt buộc khi lưu trữ (xóa)       | `reason` blank khi archive |
| `CHKTPL-006`  | 409  | Template đã được lưu trữ trước đó      | status hiện tại đã là ARCHIVED |
| `CHKTPL-007`  | 409  | Checklist không ở trạng thái chờ duyệt | (§14 addendum) `decide()` gọi khi status ≠ PENDING_REVIEW |
| `CHKTPL-008`  | 400  | Lý do bắt buộc khi từ chối             | (§14 addendum) REJECT với reason rỗng/null |

---

## 10. Bảng phân quyền

| Endpoint | `MOTHER` | `CONTENT_ADMIN` | `SYSTEM_ADMIN` |
| --- | --- | --- | --- |
| `GET /api/v1/admin/checklist-templates` (§14) | ❌ | ✅ | ✅ |
| `GET /api/v1/admin/checklist-templates/{id}` | ❌ | ✅ | ✅ |
| `POST /api/v1/admin/checklist-templates` | ❌ | ✅ | ❌* |
| `PUT /api/v1/admin/checklist-templates/{id}` | ❌ | ✅ | ❌* |
| `POST /api/v1/admin/checklist-templates/{id}/archive` | ❌ | ✅ | ❌* |
| `POST /api/v1/admin/checklist-templates/{id}/decision` (§14) | ❌ | ❌ | ✅ |
| `GET /api/v1/content/checklists` (đã có) | ✅ | ✅ | ✅ |

*\* Giữ nhất quán với `AdminContentController` hiện tại: `createContent`/`updateContent` chỉ `hasRole('CONTENT_ADMIN')`, KHÔNG cho SYSTEM_ADMIN ghi trực tiếp — SYSTEM_ADMIN's write path for checklist templates is exclusively the decision endpoint (§14), giống ContentItem.*

---

## 11. Câu hỏi mở cần Tech Lead xác nhận trước khi implement

### 11.1. Approval flow cho checklist template — ✅ ĐÃ QUYẾT ĐỊNH (2026-07-22, điều chỉnh kỹ thuật xem §14)

`ContentItem` có luồng riêng: CONTENT_ADMIN tạo/sửa (DRAFT/PENDING_REVIEW) → SYSTEM_ADMIN duyệt qua `ContentApprovalController.decide()` (APPROVE/REJECT) → APPROVED.

**Quyết định (HuyND, "approved with defaults"):** áp dụng đúng luồng approval hiện có — SYSTEM_ADMIN duyệt (APPROVE/REJECT), REJECT bắt buộc reason, cùng state machine `ContentStatus`. **Cách hiện thực hoá (điều chỉnh sau review kỹ thuật — xem §14):** KHÔNG mở rộng `ContentApprovalService.decide()` bằng discriminator `entityType`, vì `ContentDecisionResponse`/`decide()` hiện tại đọc/ghi `versionNo` và `publishedAt` — hai field không tồn tại trên `ChecklistTemplate`. Thay vào đó: controller/service riêng, cùng base path (đúng pattern `AdminContentController`+`ContentApprovalController` đã coexist trên `/api/v1/admin/content`):
- `ChecklistTemplateApprovalController` — `/api/v1/admin/checklist-templates`, class-level `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`, 1 endpoint `POST /{id}/decision`.
- `ChecklistTemplateApprovalService`/`Impl` — tái dùng DTO `ContentDecisionRequest` (decision + reason, không cần entityType); response mới `ChecklistTemplateDecisionResponse` (id, previousStatus, newStatus, decidedByAdminId, reason, decidedAt — không có versionNo).
- Lỗi mới: `CHKTPL-007` (409, không ở trạng thái PENDING_REVIEW), `CHKTPL-008` (400, thiếu reason khi REJECT).
- Audit action mới: `CHECKLIST_TEMPLATE_DECIDED`.

### 11.2. Ràng buộc `items` khi tạo mới — ✅ ĐÃ QUYẾT ĐỊNH (2026-07-22)

**Quyết định (HuyND, "approved with defaults"):** cho phép tạo template với `items: []` (khớp với dữ liệu seed hiện có, ví dụ `Checklist chuẩn bị tâm lý trước sinh` = 0 mục, DRAFT) — không bắt buộc `@NotEmpty` trên field `items`.

---

## 14. Addendum — Điều chỉnh kỹ thuật trước khi code (post-approval, cùng phiên, 2026-07-22)

Ngay sau khi tài liệu này được duyệt, người dùng yêu cầu thêm: "nâng cấp giao diện và logic xử lý `/admin/content-approval-queue` cho phù hợp luồng phê duyệt này". Trong lúc thiết kế phần đó, phát hiện 3 điểm cần điều chỉnh so với bản Draft ban đầu — đã sửa trực tiếp vào các mục liên quan ở trên (§3 ADR-CHK-001, §5.3, §8.1, §9, §10, §11.1); mục này chỉ tóm tắt lý do:

1. **Không tái sử dụng `ContentApprovalService.decide()` qua discriminator `entityType`.** `decide()` hiện tại đọc `saved.getVersionNo()` và ghi `item.setPublishedAt(...)` — cả hai field không tồn tại trên `ChecklistTemplate`. Ép chung sẽ cần null-branching trong response DTO dùng chung, mâu thuẫn với chính lý do ADR-CHK-001 chọn tách controller riêng (security boundary rõ ràng). → Tạo `ChecklistTemplateApprovalService`/`Controller` riêng, cùng base path với `AdminChecklistTemplateController` — đúng khuôn mẫu `AdminContentController`+`ContentApprovalController` đã coexist trên `/api/v1/admin/content` từ trước.
2. **Thêm `GET /api/v1/admin/checklist-templates`** (list, filter `status`+`stage`, phân trang) — cần thiết để hàng đợi phê duyệt (`/admin/content-approval-queue`) hiển thị được cả `ChecklistTemplate` đang `PENDING_REVIEW`, không chỉ `ContentItem`. Đây là mở rộng thuần bổ sung (additive), không đổi hành vi endpoint nào đã duyệt.
3. **Class-level `@PreAuthorize` của `AdminChecklistTemplateController`** đổi từ `hasRole('CONTENT_ADMIN')` thành `hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN')`, các endpoint ghi override method-level về CONTENT_ADMIN-only — cần thiết vì điểm (2) ở trên (SYSTEM_ADMIN phải đọc được list/detail để duyệt).

Không phát sinh thay đổi nào tới quyết định nghiệp vụ đã duyệt ở §11.1/§11.2 (SYSTEM_ADMIN vẫn duyệt qua đúng state machine `ContentStatus`, items rỗng vẫn được phép) — chỉ là cách hiện thực hoá kỹ thuật khác đi.

---

## 12. Rollback Plan

Không có migration DDL trong tài liệu này (tái sử dụng bảng có sẵn) → rollback chỉ là revert code:
```bash
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/AdminChecklistTemplateService*.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/dto/request/*ChecklistTemplate*.java
```
Không cần rollback DB — không có ALTER/CREATE TABLE mới.

---

## 13. AI Prompt Constraints (CASE 2.0)

| # | Constraint | Nguồn |
|---|---|---|
| C1 | KHÔNG được sửa `ContentController.getChecklists()` (public read) — chỉ thêm controller mới | ADR-CHK-001 |
| C2 | "Xóa" PHẢI là archive (status=ARCHIVED + reason bắt buộc), KHÔNG hard-delete row | ADR-CHK-002 |
| C3 | Update items PHẢI dùng `deleteAll()` + `saveAll()` qua repository, KHÔNG gán `Stream.toList()` vào field quản lý bởi Hibernate | Bài học bug phiên trước (xem §5.3) |
| C4 | KHÔNG xóa `ContentType.CHECKLIST` khỏi enum hay dữ liệu `content_items` hiện có — chỉ ẩn khỏi dropdown tạo mới ở frontend | ADR-CHK-004 |
| C5 | Mọi create/update/archive phải gọi `AuditService.log()` trong cùng transaction | ADR-CHK-003 |

Xác nhận sau implement: C1–C5 đều được tuân thủ (xem code + Test-Spec §5 Red-Green-Refactor Tracker, toàn bộ 🟢 GREEN).

---

## 15. Xác nhận hoàn thành (Post-Implementation)

- Backend: `AdminChecklistTemplateController`/`Service`/`Impl`, `ChecklistTemplateApprovalController`/`Service`/`Impl`, DTO mới (§5.2), lỗi `CHKTPL-001..008` (§9), audit action `CHECKLIST_TEMPLATE_CREATED/UPDATED/ARCHIVED/DECIDED` — toàn bộ implement đúng theo §5.3/§14.
- Test: 26 unit/controller test + 4 integration test (Testcontainers, bao gồm `CHKTPL-TC-INT-004` bảo vệ UC-50) — 30/30 GREEN. Không regression trên bộ test hiện có của repo (ngoại trừ lỗi môi trường thiếu secret Firebase/Zego và 6 lỗi `ContentMapperTest` đã biết từ trước, không liên quan tính năng này).
- Frontend: `ChecklistListPage` (CRUD thật, bỏ `disabled` tooltip), `ChecklistDetailPage`, `ChecklistFormPage` (create+edit dùng chung), `CreateContentPage` ẩn `CHECKLIST` khỏi dropdown (ADR-CHK-004), `ContentApprovalQueuePage` nâng cấp hiển thị cả `ContentItem` lẫn `ChecklistTemplate` bằng nhãn tiếng Việt và prompt lý do từ chối thật.
- QA thủ công (trình duyệt): create → submit phê duyệt → approve/reject → edit → archive, đầy đủ cho cả CONTENT_ADMIN và SYSTEM_ADMIN. Phát hiện và fix 1 bug trong lúc QA: nút "Xem chi tiết" ở hàng đợi phê duyệt trỏ tới route CONTENT_ADMIN-only khiến SYSTEM_ADMIN nhận `/forbidden` — thêm route review-only `/admin/content-review/:id` và `/admin/content-review/checklists/:id`, ẩn các nút ghi khi viewer không có role CONTENT_ADMIN.

*Tài liệu này đã hoàn thành implementation — Status: Approved, toàn bộ Exit Criteria (Test-Spec §6) đã đạt.*
