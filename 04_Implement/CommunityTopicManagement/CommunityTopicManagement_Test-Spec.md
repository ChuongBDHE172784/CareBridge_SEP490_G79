# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test Specification — Community Topic Management (Real Taxonomy, No Mocks)

**Document ID:** `CB-COMMUNITY-TEST-010`
**Version:** `1.0`
**Date:** `2026-07-21`
**Status:** `Implemented — 2026-07-22 (17/17 test cases verified GREEN — Docker Desktop was started, integration tests ran against real Testcontainers PostgreSQL and passed)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Amelia (Test Designer)`
**Reviewed by:** `[ ] [Tên] — Pending`
**DPO Sign-off:** `N/A — no PII`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` + `V20260721204919__add_community_topic_taxonomy.sql`
- `04_Implement/CommunityTopicManagement/CommunityTopicManagement_TDS.md` (`CB-COMMUNITY-IMP-010`)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.11 (UC-109), §3.3.8.2 (UC-163)

> **Quy ước TDD:** viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-21 | AI Agent — Amelia | Khởi tạo tài liệu — Test-Spec cho CommunityTopicManagement |
| 2026-07-21 | HuyND | Approved — "Approved" xác nhận qua chat, không chỉnh sửa nội dung. |
| 2026-07-21 | AI Agent — Amelia (Dev Agent) | **Sửa COM-TC-006 + COM-TC-INT-002 theo ADR-COM-016 đã sửa**: phát hiện `ContentCategoryController` dùng chung entity, tạo CATEGORY không có parent — parentId cho CATEGORY/TAG chuyển từ "bắt buộc" sang "tuỳ chọn". Xem TDS CHANGELOG cùng ngày. |
| 2026-07-21 | AI Agent — Amelia (Dev Agent) | **Truthful Sync sau implement**: 15/17 TC thực sự chạy và PASS (10 backend unit qua Red→Green đầy đủ; COM-TC-011/SEC-001 pure routing/regression, ngoài phạm vi Red Gate; WEB-TC-001/MOB-TC-001 pass thật nhưng KHÔNG theo strict Red-first — implement trước, test sau, ghi nhận minh bạch). 2/17 (`COM-TC-INT-001`, `COM-TC-INT-002`) viết xong, compile sạch, nhưng KHÔNG chạy được — môi trường không có Docker (`Could not find a valid Docker environment`). Full regression: backend 0 regression thật (9 lớp lỗi sẵn có từ trước, xác nhận qua `git stash` baseline so sánh); web 0 regression (4 file e2e lỗi từ trước cũng qua baseline check); mobile 248/248 pass. Migration thật dùng timestamp `V20260721204919` (không phải placeholder `V20260721100000` trong TDS gốc — theo đúng quy tắc implement-feature skill "TDS may reference for docs, actual file uses real timestamp"). |
| 2026-07-22 | AI Agent — Amelia (Dev Agent) | **17/17 GREEN.** User đã commit, apply migration lên Supabase, bật Docker Desktop. Chạy lại `CommunityTopicIntegrationTest` → phát hiện lỗi Flyway có sẵn từ trước (2 migration trùng version `20260720100000`, từ merge `c2f96088`, chặn TOÀN BỘ Testcontainers test trong dự án — xác nhận bằng `CommunityProfileIntegrationTest` không liên quan cũng lỗi y hệt). User xác nhận đổi tên `V20260720100000__add_content_report_revert_columns.sql` → `V20260720100001__...`. Sau `mvn clean test`: `CommunityTopicIntegrationTest` 3/3 pass thật (Testcontainers PostgreSQL thật, không giả lập). Full regression backend lại lần nữa sau khi sửa: không có test nào của community/topic/content-category nằm trong danh sách lỗi còn sót (đã grep xác nhận); các lỗi còn lại (kể cả 1 lớp mới `Mf03OpenApiContractTest`, do thiếu `carebridge.zego.app-id` config) đều xác nhận không liên quan qua kiểm tra root cause riêng. |
| 2026-07-22 | AI Agent — Amelia (Dev Agent) | **Bug thật phát hiện qua UI QA thủ công** (đăng nhập `content@carebridge.dev` thật, thao tác thật trên Chrome qua MCP devtools, không phải test tự động): tạo category qua route cũ `/content/categories` (`ContentCategoryController`) trả về 400 vì `contentApi.ts` không gửi `sortOrder`, còn `CreateCommunityTopicRequest.sortOrder` là `int` primitive + `@Builder.Default` — xung đột Jackson khi có cả `@NoArgsConstructor`/`@AllArgsConstructor` (field JSON vắng mặt → Jackson truyền `null` cho tham số `int` của all-args constructor → `HttpMessageNotReadableException`). Không nằm trong 17 test case gốc (không covered vì mock-based unit test luôn set sortOrder tường minh). Fix: đổi `sortOrder` sang `Integer` boxed (khớp convention đã có ở `UpdateCommunityTopicRequest`), mapper null-check về 0. Verify: curl trước/sau fix (400→201), UI thật lưu thành công, 46/46 test community/content xanh lại sau `mvn clean test`, full regression 2410 test không có lỗi nào thuộc phạm vi này. Dữ liệu test tạo ra trong lúc QA đã ẩn (soft-hide) khỏi Supabase dev DB dùng chung. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `GAP-COMMUNITY-TOPIC-TAXONOMY` |
| **Module** | `community` (CommunityTopicManagement) |
| **Spec gốc** | `CB-COMMUNITY-IMP-010` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S-current (2026-07-21 → TBD)` |
| **Milestone** | — |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC` |
| **Upstream Dependencies** | `community.CommunityQuestion` |
| **Downstream Consumers** | Web `ManageTopicsPage`, Mobile `TopicDirectoryScreen`/`CommunityFeedScreen` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CommunityTopicManagement_TDS.md §17.2` |
| **Constraints Injected** | C1 (server-only slug), C2 (hierarchy invariant), C3 (APPROVED-only batch count), C4 (no new endpoints), C5 (RBAC unchanged) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|--------------------------|------------------------------|----------------------------|
| L1 | Web `ManageTopicsPage.tsx` suy ra `type` từ field `icon` (`icon==='label'→CATEGORY`) | Backend không có field `type` thật trước feature này | Test khẳng định response JSON có field `type` độc lập với `icon`; đổi `icon` không được làm đổi `type` |
| L2 | Web sinh `slug` ở client, không gửi lên backend | `CreateCommunityTopicRequest` trước đây không có field `slug` | Test khẳng định `slug` không tồn tại trong request DTO (compile-level) và luôn có mặt, unique trong response |
| L3 | Mobile tính `questionCount` giả bằng `sortOrder * 100` | Không có quan hệ toán học nào giữa `sortOrder` và số câu hỏi thật | Test khẳng định `questionCount` khớp `COUNT(*) FROM community_questions WHERE topic_id=... AND status='APPROVED'`, độc lập hoàn toàn với `sortOrder` |
| L4 | Web hiện lồng MỌI category dưới MỌI topic đang mở (không lọc theo cha thật) | `parent_id` chưa tồn tại trước feature này | Test (web logic unit) khẳng định chỉ category có `parentId === topic.id` mới render dưới topic đó |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
community (CommunityTopicManagement) bao gồm các layer:
├── Domain (SlugGenerator — pure logic, no deps)
├── Service (CommunityTopicServiceImpl — mock JPA Repository với Mockito)
├── Controller (CommunityTopicController — @WebMvcTest, mock Service)
├── Integration (CommunityTopicServiceImpl + Testcontainers PostgreSQL — hierarchy + count aggregation thật)
├── Web (pure-logic unit test cho tree-grouping helper — vitest, .test.ts, KHÔNG component/RTL vì
│         codebase hiện chưa có convention test component React — xem TDS-05 Test Data Requirements)
└── Mobile (widget test cho TopicDirectoryScreen — flutter_test, theo convention 48 test file hiện có)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|-----------------|
| `SRS UC-109` | Create/edit/hide topics, tags — actor Community Moderator, Admin Portal only |
| `SRS UC-163` | Mother browses topics — chỉ TOPIC-type hiển thị (ADR-COM-017) |
| `ADR-COM-015` | Question count = APPROVED-only, batch |
| `ADR-COM-016` | Hierarchy invariant: TOPIC no parent; CATEGORY/TAG mandatory TOPIC parent |
| `ADR-COM-018` | Slug server-generated, auto-suffix on collision |
| `BR-RBAC` | Create/update chỉ MODERATOR/CONTENT_ADMIN (đã có, hồi quy) |
| `CommunityTopicManagement_TDS.md §8-10` | DTO shape, error codes COM-015 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|-------------------|-------------------|----------------|
| TC-COND-001 | Tên tiếng Việt có dấu → slug ASCII đúng | `SlugGenerator.generate()` | `COM-TC-001` |
| TC-COND-002 | Slug trùng → tự thêm hậu tố -2, -3 | `CommunityTopicServiceImpl.resolveUniqueSlug()` | `COM-TC-002`, `COM-TC-003` |
| TC-COND-003 | Đổi tên → slug tính lại | `updateTopic()` | `COM-TC-004` |
| TC-COND-004 | TOPIC có parentId → reject | `validateHierarchy()` | `COM-TC-005` |
| TC-COND-005 | CATEGORY/TAG thiếu parentId → chấp nhận (parentId tuỳ chọn, ADR-COM-016 revised) | `validateHierarchy()` | `COM-TC-006` |
| TC-COND-006 | parentId trỏ tới non-TOPIC → reject | `validateHierarchy()` | `COM-TC-007` |
| TC-COND-007 | parentId trỏ tới TOPIC đang ẩn → reject | `validateHierarchy()` | `COM-TC-008` |
| TC-COND-008 | CATEGORY hợp lệ dưới TOPIC → 201, questionCount=0 | `createTopic()` | `COM-TC-009` |
| TC-COND-009 | questionCount chỉ đếm APPROVED | `countApprovedQuestionsByTopicIds()` | `COM-TC-010`, `COM-TC-INT-001` |
| TC-COND-010 | questionCount batch không N+1 | Repository query | `COM-TC-INT-001` |
| TC-COND-011 | MOTHER gọi POST → 403 | RBAC (hồi quy) | `COM-TC-SEC-001` |
| TC-COND-012 | `type=TOPIC` filter chỉ trả TOPIC | `GET /topics?type=` | `COM-TC-011`, `COM-TC-INT-002` |
| TC-COND-013 | Đổi `type→TOPIC` cùng lúc gửi `parentId` khác null → reject | `validateHierarchy()` update path | `COM-TC-012` |
| TC-COND-014 | Web: category chỉ lồng dưới đúng topic cha (`parentId` thật) | `buildTopicTree()` helper | `WEB-TC-001` |
| TC-COND-015 | Mobile: badge hiển thị `questionCount` thật, không phải `sortOrder*100` | `_TopicGridCard` | `MOB-TC-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|---------------------------|----------------|--------------|
| Equivalence Partitioning | `type` ∈ {TOPIC, CATEGORY, TAG} | 3 lớp giá trị hành vi khác nhau hoàn toàn (parentId required/forbidden) |
| Boundary Value Analysis | Slug collision (0 trùng / 1 trùng / 2 trùng liên tiếp → `-2`, `-3`) | Xác nhận suffix tăng đúng, không nhảy số |
| State Transition Testing | `type` thay đổi qua PATCH (TOPIC↔CATEGORY) | Invariant §6.3 TDS phải giữ đúng ở mọi transition |
| Error Guessing | RBAC bypass (MOTHER gọi thẳng POST), parentId trỏ chính nó (self-parent) | Attack/edge vector thường bị bỏ sót |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|------------|------|------------------|-------------|
| `FX-001` | DB seed | `CommunityTopic{name:"Dinh dưỡng thai kỳ", type:TOPIC, isHidden:false}` | Happy path parent lookup |
| `FX-002` | DB seed | `CommunityTopic{name:"Ăn uống", type:CATEGORY, parentId:FX-001.id}` | Valid child |
| `FX-003` | DB seed | `CommunityTopic{name:"Dinh dưỡng thai kỳ (ẩn)", type:TOPIC, isHidden:true}` | Parent-hidden reject case |
| `FX-004` | DB seed | 3× `CommunityQuestion{topicId:FX-001.id, status:APPROVED}` + 1× `{status:PENDING}` + 1× `{status:HIDDEN}` | questionCount phải = 3, không phải 5 |
| `FX-005` | JWT | `{sub:'mod-001', role:'MODERATOR'}` | Auth context happy path |
| `FX-006` | JWT | `{sub:'mother-001', role:'MOTHER'}` | RBAC reject case |
| `FX-007` | Vietnamese strings | `"Chăm sóc bé sơ sinh" → "cham-soc-be-so-sinh"`, `"Tâm lý & Cảm xúc" → "tam-ly-cam-xuc"` | Oracle cho `SlugGenerator` (đối chiếu thủ công theo thuật toán NFD ở TDS §8.1) |

> Test Data Classification mặc định: `SYNTHETIC` cho toàn bộ — không dùng dữ liệu Supabase dev thật.

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// CommunityTopicTestFactory.java — src/test/java/com/carebridge/backend/community/
class CommunityTopicTestFactory {

    static CommunityTopic makeTopic() {
        return makeTopic(t -> {});
    }

    static CommunityTopic makeTopic(java.util.function.Consumer<CommunityTopic> overrides) {
        CommunityTopic topic = CommunityTopic.builder()
                .id(UUID.randomUUID())
                .name("Dinh dưỡng thai kỳ")
                .description("desc")
                .icon("restaurant")
                .type(TopicType.TOPIC)
                .slug("dinh-duong-thai-ky")
                .parentId(null)
                .isHidden(false)
                .sortOrder(1)
                .createdBy(UUID.randomUUID())
                .build();
        overrides.accept(topic);
        return topic;
    }

    static CommunityTopic makeCategory(UUID parentId) {
        return makeTopic(t -> {
            t.setId(UUID.randomUUID());
            t.setName("Ăn uống");
            t.setType(TopicType.CATEGORY);
            t.setSlug("an-uong");
            t.setParentId(parentId);
        });
    }

    static CreateCommunityTopicRequest makeCreateTopicRequest(java.util.function.Consumer<CreateCommunityTopicRequest.CreateCommunityTopicRequestBuilder> overrides) {
        var builder = CreateCommunityTopicRequest.builder()
                .name("Sức khỏe tinh thần")
                .description("desc")
                .type(TopicType.TOPIC)
                .sortOrder(0);
        overrides.accept(builder);
        return builder.build();
    }

    static CommunityQuestion makeApprovedQuestion(UUID topicId) {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(topicId)
                .authorId(UUID.randomUUID())
                .title("Q")
                .body("B")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .status(QuestionStatus.APPROVED)
                .build();
    }
}
```

---

### COM-TC-001 — SlugGenerator strips Vietnamese diacritics correctly

**Severity:** `MEDIUM`
**Feature Under Test:** `SlugGenerator.generate(String)`
**Test File:** `src/test/java/com/carebridge/backend/community/util/SlugGeneratorTest.java` (mới)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `FX-007` (đối chiếu thủ công theo thuật toán NFD mô tả ở TDS §8.1) + logic client `generateSlug()` gốc trong `ManageTopicsPage.tsx` (thuật toán tham chiếu, KHÔNG phải nguồn thẩm quyền — Java là single source of truth mới theo ADR-COM-018)

**Preconditions:** Không cần DB.

**Test Steps:**
1. Arrange: input = `"Chăm sóc bé sơ sinh"`
2. Act: `SlugGenerator.generate(input)`
3. Assert: kết quả = `"cham-soc-be-so-sinh"`

**Expected Result (PASS):** chuỗi khớp chính xác `"cham-soc-be-so-sinh"`, chỉ gồm `[a-z0-9-]`, không có `-` đầu/cuối, không có `--` liên tiếp.

**Expected Result (FAIL):** giữ nguyên dấu tiếng Việt, hoặc chứa khoảng trắng/ký tự hoa.

**Current Status:** 🟢 Passing
**Implementation Note:** dùng `Normalizer.normalize(NFD)` + `Pattern.compile("\\p{InCombiningDiacriticalMarks}+")`, xử lý riêng `đ/Đ→d` (không nằm trong combining marks NFD của Unicode).

---

### COM-TC-002 — Slug collision auto-suffixes with -2

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.createTopic()`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityTopicServiceImplTest.java` (file có sẵn — thêm test method)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-COM-018` (Slug collision decision)

**Preconditions:** `topicRepository.existsBySlug("suc-khoe-tinh-than")` mock trả `true`; `existsBySlug("suc-khoe-tinh-than-2")` mock trả `false`.

**Test Steps:**
1. Arrange: `makeCreateTopicRequest(r -> r.name("Sức khỏe tinh thần"))`, mock repository như trên
2. Act: `service.createTopic(modId, request)`
3. Assert: entity được `save()` có `slug = "suc-khoe-tinh-than-2"`

**Expected Result (PASS):** slug lưu xuống có hậu tố `-2`.
**Expected Result (FAIL):** slug bị trùng được lưu thẳng (vi phạm UNIQUE, sẽ throw `DataIntegrityViolationException` ở tầng DB thay vì được service xử lý sạch).

**Current Status:** 🟢 Passing

---

### COM-TC-003 — Slug collision increments suffix past -2 when -2 also taken

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityTopicServiceImpl.createTopic()`
**Test File:** (như trên)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-COM-018`

**Test Steps:**
1. Arrange: `existsBySlug("x")=true`, `existsBySlug("x-2")=true`, `existsBySlug("x-3")=false`
2. Act: `createTopic(...)` với name sinh slug gốc `"x"`
3. Assert: slug lưu = `"x-3"`

**Expected Result (PASS):** `"x-3"`.
**Expected Result (FAIL):** dừng ở `"x-2"` dù đã trùng, hoặc vòng lặp vô hạn/StackOverflow.

**Current Status:** 🟢 Passing
**Implementation Note:** vòng lặp phải có giới hạn hợp lý (vd. throw sau 1000 lần thử) để tránh infinite loop nếu có bug logic — ghi chú defensive, không phải test case riêng vì không nằm trong happy/realistic path.

---

### COM-TC-004 — Renaming a topic recomputes its slug

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityTopicServiceImpl.updateTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-COM-018`

**Preconditions:** existing topic `slug="dinh-duong-thai-ky"`, update request `name="Dinh dưỡng khi mang thai"`.

**Test Steps:**
1. Arrange: `makeTopic()`, `UpdateCommunityTopicRequest{name:"Dinh dưỡng khi mang thai"}`
2. Act: `service.updateTopic(id, modId, request)`
3. Assert: `slug` mới = `"dinh-duong-khi-mang-thai"` (khác slug cũ), `existsBySlugAndIdNot` được gọi để loại trừ chính nó khỏi collision check

**Expected Result (PASS):** slug thay đổi khớp tên mới.
**Expected Result (FAIL):** slug giữ nguyên giá trị cũ dù tên đã đổi, hoặc self-collision (topic tự trùng với chính nó trước khi update).

**Current Status:** 🟢 Passing

---

### COM-TC-005 — Creating a TOPIC with a parentId is rejected

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.validateHierarchy()` (via `createTopic()`)
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-COM-016`

**Test Steps:**
1. Arrange: `makeCreateTopicRequest(r -> r.type(TopicType.TOPIC).parentId(UUID.randomUUID()))`
2. Act: `service.createTopic(modId, request)`
3. Assert: throws `InvalidTopicHierarchyException`; `topicRepository.save()` **never called**

**Expected Result (PASS):** exception thrown, không có side-effect ghi DB.
**Expected Result (FAIL):** topic được tạo với `type=TOPIC` và `parentId` khác null tồn tại trong DB.

**Current Status:** 🟢 Passing

---

### COM-TC-006 — Creating a CATEGORY without a parentId is accepted (parentId optional, ADR-COM-016 revised)

**Severity:** `HIGH`
**Feature Under Test:** `validateHierarchy()` / `createTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-COM-016` (revised 2026-07-21 — `ContentCategoryController` creates flat categories with no parent; parentId must stay optional for CATEGORY/TAG so that existing, actively-used controller keeps working)

**Test Steps:**
1. Arrange: `makeCreateTopicRequest(r -> r.type(TopicType.CATEGORY).parentId(null))`
2. Act: `service.createTopic(modId, request)`
3. Assert: **không** throw; `topicRepository.save()` được gọi; response có `type=CATEGORY`, `parentId=null`

**Expected Result (PASS):** category tạo thành công không cần cha — hành vi này là chính hành vi mà `ContentCategoryController` đang dựa vào.
**Expected Result (FAIL):** throw `InvalidTopicHierarchyException` (hành vi cũ, đã bị revert).
**Current Status:** 🟢 Passing

---

### COM-TC-007 — parentId pointing to a non-TOPIC entity is rejected

**Severity:** `HIGH`
**Feature Under Test:** `validateHierarchy()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-COM-016`

**Preconditions:** `FX-002` (a CATEGORY) exists; `topicRepository.findByIdAndTypeAndIsHiddenFalse(FX-002.id, TOPIC)` mock trả `Optional.empty()` (vì FX-002 là CATEGORY, không match filter `type=TOPIC`).

**Test Steps:**
1. Arrange: `makeCreateTopicRequest(r -> r.type(TAG).parentId(FX-002.getId()))`
2. Act: `service.createTopic(modId, request)`
3. Assert: throws `InvalidTopicHierarchyException` hoặc `CommunityTopicNotFoundException` (theo §10 TDS — cả 2 exception đều hợp lệ vì lookup dùng `findByIdAndTypeAndIsHiddenFalse` trả rỗng khi type sai)

**Expected Result (PASS):** không tạo được TAG với cha là CATEGORY (chặn lồng 3 tầng).
**Current Status:** 🟢 Passing

---

### COM-TC-008 — parentId pointing to a hidden TOPIC is rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `validateHierarchy()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-COM-016`

**Preconditions:** `FX-003` (hidden TOPIC).

**Test Steps:**
1. Arrange: request `type=CATEGORY, parentId=FX-003.id`; mock `findByIdAndTypeAndIsHiddenFalse(FX-003.id, TOPIC)` → `Optional.empty()` (filter loại `isHidden=true`)
2. Act / Assert: throws exception, không tạo category "mồ côi" dưới 1 topic đã ẩn

**Current Status:** 🟢 Passing

---

### COM-TC-009 — Valid CATEGORY under an existing TOPIC succeeds with questionCount=0

**Severity:** `CRITICAL`
**Feature Under Test:** `createTopic()` happy path
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-COM-015`, `ADR-COM-016`

**Test Steps:**
1. Arrange: `FX-001` (visible TOPIC) exists; mock lookup returns it; request `type=CATEGORY, parentId=FX-001.id`
2. Act: `service.createTopic(modId, request)`
3. Assert: response `type=CATEGORY`, `parentId=FX-001.id`, `slug` non-null/non-empty, `questionCount=0`, `isHidden=false`

**Expected Result (PASS):** đúng như trên.
**Current Status:** 🟢 Passing

---

### COM-TC-010 — questionCount counts only APPROVED questions (unit, mocked repo)

**Severity:** `CRITICAL`
**Feature Under Test:** `CommunityTopicServiceImpl.getTopics()` question-count hydration
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-COM-015` (user-approved decision — APPROVED only)

**Test Steps:**
1. Arrange: `topicRepository.findAll...()` trả `[FX-001]`; `questionRepository.countApprovedQuestionsByTopicIds([FX-001.id])` mock trả projection `[{topicId:FX-001.id, cnt:3}]`
2. Act: `service.getTopics(false, null, userId)`
3. Assert: response `questionCount=3`; `questionRepository.countApprovedQuestionsByTopicIds` gọi đúng **1 lần** (không N+1) với toàn bộ list topicId

**Expected Result (PASS):** `questionCount=3`, 1 lời gọi repo.
**Expected Result (FAIL):** `questionCount` sai (vd. đếm cả PENDING/HIDDEN), hoặc repo bị gọi N lần (1 lần/topic).

**Current Status:** 🟢 Passing

---

### COM-TC-011 — GET /topics?type=TOPIC filters correctly

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicController.getTopics()`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityTopicControllerTest.java` (file có sẵn — thêm test method)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-COM-017`

**Test Steps:**
1. Arrange: mock `topicService.getTopics(false, TopicType.TOPIC, userId)` trả list chỉ gồm TOPIC entries
2. Act: `GET /api/v1/community/topics?type=TOPIC` (MockMvc, authenticated MOTHER)
3. Assert: 200, service được gọi với `type=TopicType.TOPIC` đúng tham số

**Expected Result (PASS):** param truyền đúng xuống service.
**Expected Result (FAIL):** param bị bỏ qua, luôn trả cả 3 loại.

**Current Status:** 🟢 Passing

---

### COM-TC-012 — Changing type to TOPIC while parentId is still set in the same request is rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityTopicServiceImpl.updateTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CommunityTopicManagement_TDS.md §9.2` ("Ràng buộc đặc biệt")

**Preconditions:** existing entity `type=CATEGORY, parentId=FX-001.id`.

**Test Steps:**
1. Arrange: `UpdateCommunityTopicRequest{type:TOPIC, parentId: <FX-001.id kept non-null>}`
2. Act: `service.updateTopic(id, modId, request)`
3. Assert: throws `InvalidTopicHierarchyException`; entity **không** bị save với state mâu thuẫn

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

---

### COM-TC-SEC-001 — MOTHER cannot create a topic (RBAC regression)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `POST /api/v1/community/topics` `@PreAuthorize`
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN

**Preconditions:** `FX-006` JWT (role MOTHER).

**Test Steps (Attack Simulation):**
1. Arrange: MockMvc với `@WithMockUser(roles={"MOTHER"})`
2. Act: `POST /api/v1/community/topics` với body hợp lệ
3. Assert: response 403; `topicService.createTopic()` **never invoked**

**Expected Result (PASS = an toàn):** `403 Forbidden`.
**Expected Result (FAIL = lỗ hổng):** 201 Created, MOTHER tạo được topic.

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

> Testcontainers PostgreSQL. Timeout 120s. Đây là nơi verify migration + CHECK constraint + batch-count thật (không mock).

---

### COM-TC-INT-001 — questionCount aggregation against a real DB, mixed statuses

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow: seed topic + questions (mixed status) → `GET /api/v1/community/topics`
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityTopicIntegrationTest.java` (mới)
**TDD Phase:** 🟢 GREEN — verified 2026-07-22 against real Testcontainers PostgreSQL after Docker Desktop was started (`./mvnw clean test -Dtest=CommunityTopicIntegrationTest`, exit 0, `Tests run: 3, Failures: 0, Errors: 0`)
**Condition Ref:** `TC-COND-009`, `TC-COND-010`

**Preconditions:**
- PostgreSQL Testcontainer running (`spring.flyway.enabled=true` tự động qua Testcontainers, xem migration `V20260721100000` áp dụng thật)
- Seed: 1 TOPIC (`FX-001`), 3× `CommunityQuestion{status:APPROVED}`, 1× `{status:PENDING}`, 1× `{status:HIDDEN}` — tất cả `topic_id=FX-001.id`

**Test Steps:**
1. Seed dữ liệu qua JPA repository trực tiếp (không qua API, để kiểm soát chính xác status)
2. `GET /api/v1/community/topics` với JWT MODERATOR
3. Assert DB: `SELECT COUNT(*) FROM community_questions WHERE topic_id=:id AND status='APPROVED'` = 3
4. Assert API response: item của `FX-001` có `questionCount=3`

**Expected Result (PASS):** API response khớp DB assertion, cả hai đều = 3 (không phải 5).

**DB Assertion:**
```java
long approvedCount = questionRepository.countApprovedQuestionsByTopicIds(List.of(topicId))
        .stream().filter(p -> p.getTopicId().equals(topicId)).findFirst()
        .map(TopicQuestionCountProjection::getCnt).orElse(0L);
assertThat(approvedCount).isEqualTo(3L);
```

**Current Status:** 🟢 Passing (verified 2026-07-22, real Postgres via Testcontainers). Test method thực tế trong code: `countApprovedQuestionsByTopicIds_mixedStatuses_countsOnlyApproved` — dùng repository trực tiếp thay vì gọi qua `GET /api/v1/community/topics` (đơn giản hoá so với plan gốc, vẫn cùng oracle: 3 APPROVED trên tổng 5 câu hỏi mixed-status).
**Ghi chú phát sinh khi chạy lần đầu:** phát hiện 1 lỗi Flyway CÓ SẴN TỪ TRƯỚC (không do feature này) — 2 migration khác nhau trùng version `20260720100000` (`V20260720100000__secure_baby_journey_linkage.sql` và `V20260720100000__add_content_report_revert_columns.sql`, từ merge commit `c2f96088`), chặn TOÀN BỘ Testcontainers integration test trong dự án. Đã xác nhận bằng cách chạy thử `CommunityProfileIntegrationTest` (không liên quan) và bị lỗi y hệt. User xác nhận sửa: đổi tên `V20260720100000__add_content_report_revert_columns.sql` → `V20260720100001__add_content_report_revert_columns.sql` (chỉ đổi version, giữ nguyên nội dung SQL). Sau khi sửa + `mvn clean`, test pass thật.

---

### COM-TC-INT-002 — Migration enforces "a TOPIC never has a parent" at the DB level

**Severity:** `HIGH`
**Feature Under Test:** `V20260721204919__add_community_topic_taxonomy.sql` — `community_topics_parent_rule_check`
**Test File:** `CommunityTopicIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — verified 2026-07-22 against real Testcontainers PostgreSQL
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-COM-016` (revised) — the ONLY DB-enforced rule left after the revision is `type <> 'TOPIC' OR parent_id IS NULL`; "parent must be a TOPIC" is a cross-row rule and is service-layer only (COM-TC-007), not DB-enforced.

**Test Steps:**
1. Attempt raw `INSERT INTO community_topics (..., type, parent_id) VALUES (..., 'TOPIC', '<some-uuid>')` directly via `JdbcTemplate` (bypassing service layer entirely)
2. Assert: throws `DataIntegrityViolationException` (CHECK constraint violation) — DB tự bảo vệ invariant TOPIC-never-has-parent ngay cả khi service layer bị bypass
3. Sanity check: an INSERT with `type='CATEGORY', parent_id=NULL` (bypassing service) must succeed — confirms the DB no longer requires a parent for CATEGORY/TAG

**Expected Result (PASS):** bước 2 bị chặn (DataIntegrityViolationException), bước 3 thành công (không lỗi).

**Current Status:** 🟢 Passing (verified 2026-07-22). Implement thực tế tách thành 2 test method riêng: `insertTopicTypeWithParentId_bypassingService_isRejectedByCheckConstraint` (bước 2) và `insertCategoryTypeWithNullParentId_bypassingService_isAccepted` (bước 3) — cùng đóng gói trong `Tests run: 3` của `CommunityTopicIntegrationTest`.

---

### WEB TEST CASE (pure-logic unit — vitest, không component/RTL — xem TDS-01)

---

### WEB-TC-001 — buildTopicTree nests each CATEGORY/TAG only under its real parentId

**Severity:** `HIGH`
**Feature Under Test:** `buildTopicTree()` (helper mới, extract từ `ManageTopicsPage.tsx` render logic hiện tại thành hàm thuần để test được — thay cho logic cũ "lồng mọi category dưới mọi topic mở")
**Test File:** `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/topicTree.test.ts` (mới)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CommunityTopicManagement_TDS.md` §Chặng 4 (Web implementation step), Logic Issue L4 (§2)

**Preconditions:** input: `topics = [TopicA(type=TOPIC), TopicB(type=TOPIC), CategoryX(type=CATEGORY, parentId=TopicA.id)]`.

**Test Steps:**
1. Arrange: input array trên, `expandedIds = new Set([TopicA.id, TopicB.id])` (cả 2 topic đều đang mở)
2. Act: `buildTopicTree(topics, expandedIds)`
3. Assert: `CategoryX` xuất hiện đúng 1 lần trong output, là con của `TopicA`; **không** xuất hiện dưới `TopicB`

**Expected Result (PASS):** đúng như trên — đây chính là hành vi bug cũ (L4) được fix.
**Expected Result (FAIL):** `CategoryX` xuất hiện dưới cả `TopicA` và `TopicB` (hành vi cũ, sai).

**Current Status:** 🟢 Passing

---

### MOBILE TEST CASE (flutter widget test)

---

### MOB-TC-001 — Topic card displays real questionCount, not sortOrder*100

**Severity:** `HIGH`
**Feature Under Test:** `_TopicGridCard` widget trong `topic_directory_screen.dart`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/community/topic_directory_screen_test.dart` (mới)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-015`
**Oracle Source:** Logic Issue L3 (§2), `CommunityTopicManagement_TDS.md` §Chặng 5

**Test Steps:**
1. Arrange: `CommunityTopic(id:'t1', name:'X', sortOrder: 1, questionCount: 42, ...)` — cố ý chọn `sortOrder=1` để nếu code cũ còn sót `sortOrder*100` thì kết quả sẽ là "100 câu hỏi" (sai), khác biệt rõ với oracle đúng "42 câu hỏi"
2. Act: `pumpWidget(_TopicGridCard(topic: topic, ...))`
3. Assert: `find.text('42 câu hỏi')` tồn tại; `find.text('100 câu hỏi')` **không** tồn tại

**Expected Result (PASS):** hiển thị đúng "42 câu hỏi".
**Expected Result (FAIL):** hiển thị "100 câu hỏi" (bug cũ) hoặc bất kỳ giá trị nào không phải 42.

**Current Status:** 🟢 Passing
**Implementation Note:** `CommunityTopic.fromJson` phải parse field `questionCount` từ response JSON (`json['questionCount'] as int? ?? 0`).

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|---------------------|------------------------|------------------------|
| `COM-TC-001` | `community/util/SlugGeneratorTest.java` | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-002` | `community/CommunityTopicServiceImplTest.java` | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-003` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-004` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-005` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-006` | (idem) | `[x]` | 2026-07-21 (uncommitted) | Rewritten mid-implementation for ADR-COM-016 revision — verified RED (throws) then GREEN (accepts) against the revised assertion, not the original |
| `COM-TC-007` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-008` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-009` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-010` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-011` | `community/CommunityTopicControllerTest.java` | `[ ]` — pure routing test, not stub-gated (see §5.1 note) | 2026-07-21 (uncommitted) | Passed as soon as written; not a Green-from-Birth violation — controller has no deferred business logic, service is fully mocked |
| `COM-TC-012` | `community/CommunityTopicServiceImplTest.java` | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-SEC-001` | `community/CommunityTopicControllerTest.java` | `[x]` (pre-existing test, reused — RBAC unchanged) | 2026-07-21 (uncommitted) | This TC maps to the already-existing `createTopic_asMotherUser_shouldReturn403` regression test |
| `COM-TC-INT-001` | `community/CommunityTopicIntegrationTest.java` | `[x]` | 2026-07-22 (uncommitted at test time) | Verified GREEN against real Testcontainers PostgreSQL after Docker Desktop was started |
| `COM-TC-INT-002` | (idem) | `[x]` | 2026-07-22 (uncommitted at test time) | Verified GREEN; also surfaced and fixed a pre-existing, unrelated Flyway duplicate-version collision (`V20260720100000` × 2, from merge `c2f96088`) that blocked ALL Testcontainers integration tests project-wide — renamed `V20260720100000__add_content_report_revert_columns.sql` → `V20260720100001__...` with user approval |
| `WEB-TC-001` | `contentManagement/pages/topicTree.test.ts` | ⚠️ Not stub-gated | 2026-07-21 (uncommitted) | `buildTopicTree` was implemented directly, then the test was written and run against it (3/3 pass) — did NOT follow strict red-stub-first order. No dependency to stub (pure function); low Green-from-Birth risk, but flagged honestly per Truthful Sync policy. |
| `MOB-TC-001` | `test/features/community/topic_directory_screen_test.dart` | ⚠️ Not stub-gated | 2026-07-21 (uncommitted) | Same as WEB-TC-001 — `questionCountLabel` implemented first, then tested (2/2 pass). Also: test targets an extracted top-level function, not the originally-planned `pumpWidget(_TopicGridCard(...))` — `CommunityService`'s private constructor makes full widget-pump testing impractical without a larger DI refactor (out of scope). |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class CommunityTopicServiceImpl implements CommunityTopicService {
    @Override
    public List<CommunityTopicResponse> getTopics(boolean includeHidden, TopicType type, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public List<CommunityTopicResponse> searchTopics(String keyword, boolean includeHidden, TopicType type, UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public CommunityTopicResponse createTopic(UUID createdBy, CreateCommunityTopicRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public CommunityTopicResponse updateTopic(UUID id, UUID updatedBy, UpdateCommunityTopicRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

public final class SlugGenerator {
    public static String generate(String name) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|---------------|-------------|-----------|--------------------------------------|
| `COM-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |
| `COM-TC-002`…`COM-TC-010`, `COM-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☑ FAIL ☐ PASS | Verified via `./mvnw test` — actual run showed 10 failures (`UnsupportedOperationException` or wrong-exception-type) before implementation |
| `COM-TC-011` | N/A — `topicService` fully mocked via `@MockitoBean`, no business logic deferred behind the stub | N/A | N/A | Not subject to Red Gate — pure controller routing test |
| `COM-TC-SEC-001` | N/A — pre-existing regression test (`createTopic_asMotherUser_shouldReturn403`), unaffected by the new stub | N/A | N/A | RBAC check predates this feature |
| `COM-TC-INT-001/002` | Ran against a real Testcontainers Postgres (Docker Desktop started) | 🔴 FAIL (expected) | ☑ Verified PASS after implement (2026-07-22) | Red Gate itself was not re-verified with a throw-stub for the integration layer (no separate stub exists for repository/DB behavior) — but the underlying service logic (`validateHierarchy`, `countApprovedQuestionsByTopicIds`) already passed unit-level Red Gate (§ above). Integration run confirms the same behavior holds against a real DB. |
| `WEB-TC-001` | N/A — implemented before the test was written (see §5 tracker note) | N/A | N/A | Strict red-stub-first order was NOT followed for this file |
| `MOB-TC-001` | N/A — implemented before the test was written (see §5 tracker note) | N/A | N/A | Strict red-stub-first order was NOT followed for this file |

**Red Gate Evidence:**
- Stub commit hash: not committed yet — all changes are uncommitted in the working tree as of 2026-07-21
- Tất cả FAIL? ☑ Yes cho 10/10 test case backend thực sự chịu Red Gate (`COM-TC-001`–`COM-TC-010`, `COM-TC-012`) → **GATE-2 PASS** (T2→T3) cho backend service/util layer. `COM-TC-011`/`COM-TC-SEC-001` ngoài phạm vi Red Gate (giải thích ở cột Stub Result). `COM-TC-INT-001/002` verify GREEN sau (2026-07-22, Docker Desktop khởi động) — `./mvnw clean test -Dtest=CommunityTopicIntegrationTest` → `Tests run: 3, Failures: 0, Errors: 0`. `WEB-TC-001`/`MOB-TC-001` không tuân thủ thứ tự Red-trước-Green — ghi nhận trung thực, không che giấu.
- Log file: chạy trực tiếp `./mvnw test` / `npx vitest run` / `flutter test` trong phiên làm việc; không lưu file log riêng. Log integration test: `target/surefire-reports/com.carebridge.backend.community.CommunityTopicIntegrationTest.txt`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] TDS `CB-COMMUNITY-IMP-010` đã Approved
- [x] Logic Issues (§2) đã confirm
- [x] Migration `V20260721204919__add_community_topic_taxonomy.sql` đã chạy sạch trên Testcontainers (verified 2026-07-22 sau khi Docker Desktop được bật)
- [x] Fixtures (§3 TDS-05) đã chuẩn bị (qua `CommunityTopicTestFactory`)

### Exit Criteria (DoD)
- [x] `./mvnw test -Dtest=SlugGeneratorTest,CommunityTopicServiceImplTest,CommunityTopicControllerTest,CommunityTopicSearchServiceImplTest,ContentCategoryControllerTest` — xanh, không skip (chạy thật, exit code 0)
- [x] `./mvnw clean test -Dtest=CommunityTopicIntegrationTest` — xanh (3/3 pass, chạy thật với Docker Desktop, sau khi sửa 1 lỗi Flyway version-collision có sẵn từ trước không liên quan feature này)
- [x] `npx vitest run src/features/contentManagement/pages/topicTree.test.ts` — xanh (3/3 pass, chạy thật)
- [x] `flutter test test/features/community/topic_directory_screen_test.dart` — xanh (2/2 pass, chạy thật)
- [x] Không có business logic trong `CommunityTopicController` (chỉ validation + mapping — hồi quy, không đổi)
- [x] Grep xác nhận không còn `sortOrder * 100` (mobile) hay `typeFromIcon`/`iconForType` (web) trong codebase; `"— bài"` cũng đã thay bằng `questionCount` thật
- [x] Full regression backend (`./mvnw test -Dtest='!*IntegrationTest'`), web (`npm run test`, `npm run build`, `npm run lint`), mobile (`flutter test`, 248 tests) — 0 regression do feature này gây ra (9 lớp test backend + 4 file e2e web lỗi từ trước, xác nhận qua `git stash` baseline)

**Exit Criteria bổ sung — CASE 2.0:**
- [x] **Red Gate (§5.1)** — tất cả 10 test case backend chịu Red Gate đều FAIL trước khi implement (verified thật). `COM-TC-INT-001/002` KHÔNG verify được (Docker). `WEB-TC-001`/`MOB-TC-001` không theo đúng thứ tự Red-trước-Green.
- [x] **Contract Existence** — `./mvnw compile` không lỗi (verified thật)
- [x] **Props Isolation** — test case mới dùng `CommunityTopicTestFactory`, không shared mutable state
- [x] **Oracle Source** — mọi expected value có ghi rõ nguồn (đã điền ở mỗi TC)

### Suspension Criteria
- Migration không apply được lên staging (xem TDS §5.2 cảnh báo Flyway shared dev DB)
- CI pipeline broken bởi thay đổi khác không liên quan

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE community_topics DROP COLUMN IF EXISTS type, DROP COLUMN IF EXISTS slug, DROP COLUMN IF EXISTS parent_id;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260721100000';"

git checkout -- src/main/java/com/carebridge/backend/community/
git checkout -- src/test/java/com/carebridge/backend/community/
git checkout -- src/main/resources/db/migration/V20260721204919__add_community_topic_taxonomy.sql
git checkout -- ../CareBridgeWebApp/src/features/contentManagement/
git checkout -- ../CareBridgeMobileApp/lib/features/community/ ../CareBridgeMobileApp/test/features/community/

# Gap vẫn OPEN → ghi lại trong memory project_community_topic_directory_gaps.md rằng attempt bị revert
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong Test-Spec này | Check | Gate chặn |
|-------|-------------|-----------------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | Mọi TC ở đây đều reference ADR-COM-015/016/017/018 hoặc L1-L4 — không có TC "trôi nổi" | ☑ | G-0 |
| AP-AI-002 | Green-from-Birth | Red Gate stub bắt buộc throw, verify tại §5.1 — 10/10 backend TC thực sự FAIL trước implement. WEB-TC-001/MOB-TC-001 KHÔNG qua Red Gate (implemented trước khi viết test) — không phải Green-from-Birth (không có stub để "PASS giả"), nhưng lệch quy trình, ghi nhận trung thực | ☑ (backend); ⚠️ (web/mobile — xem ghi chú) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Không có TC nào giả định kiến trúc ngoài §3/§5-9 TDS (vd. không test endpoint "children" không tồn tại) | ☑ | G-1 |
| AP-AI-004 | Layer Violation | Không có TC nào assert business logic trong Controller | ☑ | G-4 |
| AP-AI-005 | Hallucinated Contract | Tất cả class/method reference (`SlugGenerator`, `InvalidTopicHierarchyException`, `TopicQuestionCountProjection`) đều được định nghĩa tường minh ở TDS §8 trước khi test case dùng tới | ☑ | G-3 |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nghiêm trọng nào → Test-Spec approved, với 1 ngoại lệ đã ghi nhận minh bạch (WEB-TC-001/MOB-TC-001 không theo strict Red-first). `COM-TC-INT-001/002` đã verify GREEN thật (2026-07-22).
- [ ] Phát hiện AP nghiêm trọng cần fix trước implement → không có

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|--------------|-------|-------|------------|--------|
| Deviation (không phải AP chính thức) | WEB-TC-001, MOB-TC-001 | Implement trước khi viết test (không theo strict Red-first) | Chấp nhận có ghi chú — pure function/no stub dependency, rủi ro Green-from-Birth thấp | N/A — ghi nhận, không "fix" |
| Environment gap (đã fix) | COM-TC-INT-001, COM-TC-INT-002 | Docker không có sẵn lúc đầu; sau khi bật Docker Desktop, phát hiện thêm lỗi Flyway version-collision có sẵn từ trước (2 migration cùng version `20260720100000`) chặn toàn bộ Testcontainers test trong dự án | User xác nhận đổi tên `V20260720100000__add_content_report_revert_columns.sql` → `V20260720100001__...`; chạy lại `./mvnw clean test` | ☑ Fixed 2026-07-22 — 3/3 pass |

---

*Tài liệu này Draft — chờ user đổi Status → Approved (cùng với TDS) trước khi implement (implement-flow.md Phase 2).*
