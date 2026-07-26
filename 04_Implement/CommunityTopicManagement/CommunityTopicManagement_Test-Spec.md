# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test Specification — Community Topic Management (Real Taxonomy, No Mocks)

**Document ID:** `CB-COMMUNITY-TEST-010`
**Version:** `2.0-approved`
**Date:** `2026-07-21`
**Status:** `Implementation Complete (Amendment 2) — automated verification complete; pending human browser/device QA and user final approval. Historical RED evidence retained below.`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Amelia (Test Designer)`
**Reviewed by:** `[ ] [Tên] — Pending`
**DPO Sign-off:** `N/A — no PII`
**Approved by:** `HuyND — Decisions A–H via chat, 2026-07-22`
**Classification:** `Internal`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` + `V20260721204919__add_community_topic_taxonomy.sql` + `V20260722054603__invert_community_topic_hierarchy.sql`
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
| 2026-07-22 | AI Agent — Amelia (Dev Agent) | **Bug thật thứ 2** (WEB-TC-001 phạm vi mở rộng): nút ▲/▼ (ADR-COM-019) trên `/content/topics` gửi đúng 2 PATCH (200, `sortOrder` đổi đúng ở backend — xác nhận qua network + reload) nhưng UI không đổi vị trí ngay, phải reload mới thấy đúng. Root cause: `buildTopicTree()` không tự sort theo `sortOrder`, chỉ giữ thứ tự mảng gốc từ lần fetch đầu; `handleMove()` chỉ thay giá trị object tại đúng vị trí cũ trong mảng React state, không sắp xếp lại mảng. Fix: `buildTopicTree()` sort `topicItems`/`childItems` theo `sortOrder` trước khi dựng rows. Thêm test `orders rows by sortOrder, not by array position` — `topicTree.test.ts` nay 4/4 pass (`npx vitest run`). Verify UI thật: bấm ▲/▼ đổi vị trí ngay lập tức. Đồng thời test mobile thật qua `flutter run -d web-server` + Chrome DevTools MCP (đăng nhập `mother@carebridge.dev`): `TopicDirectoryScreen` (WEB-TC... tương đương MOB-TC-001) và `CommunityTopicSearchScreen` (CB-148, sửa ở lượt trước) đều hiển thị `questionCount` thật khớp 100% dữ liệu web, request `type=TOPIC` và `keyword=` gửi đúng qua network — không phát hiện regression nào ở mobile. |
| 2026-07-22 | HuyND + AI Agent — Amelia | **Amendment 2 — Đảo chiều phân cấp (redesign, không phải bug fix).** Sau khi tự tay QA qua UI, user chỉ ra mô hình phân cấp hiện tại (TOPIC=gốc) sai chiều so với domain chuẩn (Category=không gian bao quát chứa Topic), cộng với 2 màn/trang trùng lặp (`CommunityTopicSearchScreen` mobile, `/content/categories` web) và thiếu nút xoá thật. Điều tra + AskUserQuestion xác nhận 4 quyết định thiết kế — xem TDS ADR-COM-020 → 024. Thêm test case `COM-TC-020` → `COM-TC-031`, tất cả ở `🔴 RED` trước implementation. |
| 2026-07-22 | HuyND | **Approved Decisions A–F.** Delete guard type-agnostic (children/questions/follows cho mọi type); type immutable sau create (ADR-COM-025/COM-017); rewrite toàn bộ v1-direction tests; bổ sung COM-TC-032+ cho follow/legacy dependency/controller RBAC/question regression/migration follow preservation/mobile inline search. |
| 2026-07-22 | AI Agent — Amelia (Dev Agent) | **Milestone C backend GREEN.** `CommunityTopicServiceImplTest`, `CommunityTopicControllerTest`, `CommunityQuestionServiceImplTest`, `CommunityTopicIntegrationTest`: 64/64 GREEN; Docker khả dụng và 6 integration cases chạy thật trên PostgreSQL 16 Testcontainers. Regression fixtures bị CHECK v2 bắt đúng trong `ReportIntegrationTest`/`SearchIntegrationTest` đã đổi sang TOPIC-under-CATEGORY và 3/3 GREEN. Full `./mvnw test`: 2.419 test, 9 failures/68 errors/1 skipped; không còn failure/error thuộc Amendment 2, các nhóm còn lại khớp baseline ngoài phạm vi. Backend `ContentCategoryController`, security matcher và ba test chuyên biệt đã xoá; web/mobile vẫn pending. |
| 2026-07-22 | AI Agent — Amelia (Dev Agent) | **Milestone D web GREEN.** `WEB-TC-002` RED 4/4 đúng lý do (implementation v1 còn TOPIC-root), sau implement targeted 7/7 GREEN (`topicTree` 4 + error mapping 3), `npx vitest run src` 18/18 GREEN, `tsc -b`, `npm run build`, `npm run lint` đều exit 0. `npx vitest run` toàn repo: 18 unit PASS nhưng exit 1 vì 4 Playwright e2e specs bị Vitest collect nhầm, đúng baseline đã ghi từ v1. Xác nhận zero reference trong `src` tới `ContentCategoryListPage`, `/content/categories` và ba category API functions sau deletion. |
| 2026-07-22 | AI Agent — Amelia (Dev Agent) | **Milestone E mobile GREEN.** MOB-TC-002/003 theo pure-helper precedent của MOB-TC-001, không DI/refactor `CommunityService`: RED compile đúng vì `buildCategoryChips`, `filterDirectoryTopics`, `type` và `parentId` chưa tồn tại; GREEN targeted và toàn `test/features/community` 6/6. `TopicDirectoryScreen` gọi `getTopics(type: 'TOPIC', keyword: ...)` inline và `getTopicCategories()` (`type=CATEGORY`), chips dùng CATEGORY thật, filter theo `parentId`; file/màn search cũ đã xoá và zero dangling code reference. `dart analyze` bốn file đổi: no issues. `flutter analyze` retry hai lần đều bị analysis-server LSP `FormatException: Unexpected end of input` trước diagnostic; giữ lại cho Milestone F. |
| 2026-07-22 | AI Agent — Amelia (Dev Agent) | **Milestone F final automated verification.** Backend targeted 67/67 GREEN with Docker/PostgreSQL 16; `./mvnw clean test` reproduced the documented unrelated baseline exactly (2,419 tests, 9 failures/68 errors/1 skipped), with no Amendment 2 test class failing. Web: typecheck, scoped Vitest 18/18, build and lint GREEN. Mobile: full `flutter test` 252/252 GREEN; community-wide fallback `dart analyze` clean; full `flutter analyze` still crashes before diagnostics with the same LSP truncated-JSON error (exit 255). Source/test/config/route/nav sweep has zero dangling deleted endpoints/screens and zero active old-direction hierarchy assumptions; historical changelog/RED-evidence references are intentionally retained. No commit; shared Supabase migration and human browser/device QA remain pending. |

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
| **Constraints Injected** | C1 (server-only slug), C2 (CATEGORY-root/TOPIC-child), C3 (APPROVED-only batch count), C4 (universal guarded DELETE), C5 (RBAC unchanged), C6 (type immutable) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|--------------------------|------------------------------|----------------------------|
| L1 | Web `ManageTopicsPage.tsx` suy ra `type` từ field `icon` (`icon==='label'→CATEGORY`) | Backend không có field `type` thật trước feature này | Test khẳng định response JSON có field `type` độc lập với `icon`; đổi `icon` không được làm đổi `type` |
| L2 | Web sinh `slug` ở client, không gửi lên backend | `CreateCommunityTopicRequest` trước đây không có field `slug` | Test khẳng định `slug` không tồn tại trong request DTO (compile-level) và luôn có mặt, unique trong response |
| L3 | Mobile tính `questionCount` giả bằng `sortOrder * 100` | Không có quan hệ toán học nào giữa `sortOrder` và số câu hỏi thật | Test khẳng định `questionCount` khớp `COUNT(*) FROM community_questions WHERE topic_id=... AND status='APPROVED'`, độc lập hoàn toàn với `sortOrder` |
| L4 | Web v1 lồng child sai hướng và có thể lặp child dưới nhiều root | Amendment 2 chuẩn hoá `CATEGORY` root → `TOPIC` child | Test helper khẳng định chỉ TOPIC có `parentId === category.id` mới render dưới CATEGORY đó |

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
| `ADR-COM-020` | Hierarchy v2: CATEGORY root, TOPIC mandatory CATEGORY child, TAG flat |
| `ADR-COM-021` | Questions attach only to TOPIC |
| `ADR-COM-022` | Universal hard-delete guard: children/questions/follows for every type |
| `ADR-COM-025` | Topic type immutable after creation; COM-017 on attempted change |
| `ADR-COM-018` | Slug server-generated, auto-suffix on collision |
| `BR-RBAC` | Create/update chỉ MODERATOR/CONTENT_ADMIN (đã có, hồi quy) |
| `CommunityTopicManagement_TDS.md §8-10` | DTO shape, error codes COM-015 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|-------------------|-------------------|----------------|
| TC-COND-001 | Tên tiếng Việt có dấu → slug ASCII đúng | `SlugGenerator.generate()` | `COM-TC-001` |
| TC-COND-002 | Slug trùng → tự thêm hậu tố -2, -3 | `CommunityTopicServiceImpl.resolveUniqueSlug()` | `COM-TC-002`, `COM-TC-003` |
| TC-COND-003 | Đổi tên → slug tính lại | `updateTopic()` | `COM-TC-004` |
| TC-COND-004 | TOPIC thiếu parentId → reject | `validateHierarchy()` | `COM-TC-005`, `COM-TC-020` |
| TC-COND-005 | CATEGORY không có parentId → chấp nhận | `validateHierarchy()` | `COM-TC-006` |
| TC-COND-006 | TOPIC parentId trỏ tới non-CATEGORY → reject | `validateHierarchy()` | `COM-TC-007`, `COM-TC-021` |
| TC-COND-007 | TOPIC parentId trỏ tới CATEGORY đang ẩn → reject | `validateHierarchy()` | `COM-TC-008`, `COM-TC-022` |
| TC-COND-008 | TOPIC hợp lệ dưới CATEGORY → 201, questionCount=0 | `createTopic()` | `COM-TC-009`, `COM-TC-023` |
| TC-COND-009 | questionCount chỉ đếm APPROVED | `countApprovedQuestionsByTopicIds()` | `COM-TC-010`, `COM-TC-INT-001` |
| TC-COND-010 | questionCount batch không N+1 | Repository query | `COM-TC-INT-001` |
| TC-COND-011 | MOTHER gọi POST → 403 | RBAC (hồi quy) | `COM-TC-SEC-001` |
| TC-COND-012 | `type=TOPIC` filter chỉ trả TOPIC | `GET /topics?type=` | `COM-TC-011`, `COM-TC-INT-002` |
| TC-COND-013 | PATCH đổi type sau creation → COM-017 | `updateTopic()` | `COM-TC-012` |
| TC-COND-014 | Web: TOPIC chỉ lồng dưới đúng CATEGORY cha (`parentId` thật) | `buildTopicTree()` helper | `WEB-TC-001`, `WEB-TC-002` |
| TC-COND-015 | Mobile: badge hiển thị `questionCount` thật, không phải `sortOrder*100` | `_TopicGridCard` | `MOB-TC-001` |
| TC-COND-016 | DB CHECK v2 chặn TOPIC không parent và CATEGORY/TAG có parent | Flyway migration | `COM-TC-INT-002` |
| TC-COND-017 | Type immutable khi PATCH | `updateTopic()` | `COM-TC-012` |
| TC-COND-018 | Delete guard kiểm tra row con cho mọi type | `deleteTopic()` | `COM-TC-027` |
| TC-COND-019 | Delete guard kiểm tra question/follow cho mọi type | `deleteTopic()` | `COM-TC-029`, `COM-TC-032`, `COM-TC-033` |
| TC-COND-020 | TOPIC thiếu CATEGORY parent → reject | `validateHierarchy()` | `COM-TC-020` |
| TC-COND-021 | TOPIC parent là TAG/TOPIC → reject | `validateHierarchy()` | `COM-TC-021` |
| TC-COND-022 | TOPIC parent là hidden CATEGORY → reject | `validateHierarchy()` | `COM-TC-022` |
| TC-COND-023 | TOPIC dưới visible CATEGORY → success | `createTopic()` | `COM-TC-023` |
| TC-COND-024 | CATEGORY có parent → reject | `validateHierarchy()` | `COM-TC-024` |
| TC-COND-025 | TAG có parent → reject | `validateHierarchy()` | `COM-TC-025` |
| TC-COND-026 | Migration tạo 5 CATEGORY và reassign 8 TOPIC giữ nguyên ID | Flyway/Testcontainers | `COM-TC-026` |
| TC-COND-027 | Delete row còn child → COM-016 | `deleteTopic()` | `COM-TC-027` |
| TC-COND-028 | Delete row rỗng → success | `deleteTopic()` | `COM-TC-028`, `COM-TC-030` |
| TC-COND-029 | Delete row còn question → COM-016 | `deleteTopic()` | `COM-TC-029`, `COM-TC-033` |
| TC-COND-030 | Delete row không dependent → success | `deleteTopic()` | `COM-TC-030` |
| TC-COND-031 | Question gắn CATEGORY/TAG → COM-003 semantics | `createQuestion()` | `COM-TC-031` |
| TC-COND-032 | Web tree CATEGORY-root/TOPIC-child | `buildTopicTree()` | `WEB-TC-002` |
| TC-COND-033 | Mobile chips dựa trên CATEGORY data thật | pure helper | `MOB-TC-002` |
| TC-COND-034 | TOPIC còn follow → delete blocked | `deleteTopic()` | `COM-TC-032` |
| TC-COND-035 | Legacy CATEGORY/TAG có question/follow → delete blocked | `deleteTopic()` | `COM-TC-033` |
| TC-COND-036 | DELETE controller success → 204 | controller routing/RBAC | `COM-TC-034` |
| TC-COND-037 | DELETE controller dependent conflict → 409 COM-016 | controller + exception mapping | `COM-TC-035` |
| TC-COND-038 | MOTHER gọi DELETE → 403 | controller RBAC | `COM-TC-036` |
| TC-COND-039 | Question gắn valid TOPIC vẫn success | `createQuestion()` | `COM-TC-037` |
| TC-COND-040 | UserTopicFollow rows giữ nguyên qua migration | Flyway/Testcontainers | `COM-TC-038` |
| TC-COND-041 | Mobile inline search + CATEGORY filter pure logic; search screen không còn reference | pure helper + static verification | `MOB-TC-003` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|---------------------------|----------------|--------------|
| Equivalence Partitioning | `type` ∈ {TOPIC, CATEGORY, TAG} | 3 lớp giá trị hành vi khác nhau hoàn toàn (parentId required/forbidden) |
| Boundary Value Analysis | Slug collision (0 trùng / 1 trùng / 2 trùng liên tiếp → `-2`, `-3`) | Xác nhận suffix tăng đúng, không nhảy số |
| State Transition Testing | PATCH giữ nguyên type hoặc cố đổi type | Invariant ADR-COM-025: type immutable; mọi transition khác type phải trả COM-017 |
| Error Guessing | RBAC bypass (MOTHER gọi thẳng POST), parentId trỏ chính nó (self-parent) | Attack/edge vector thường bị bỏ sót |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|------------|------|------------------|-------------|
| `FX-001` | DB seed | `CommunityTopic{name:"Mang thai", type:CATEGORY, parentId:null, isHidden:false}` | Valid root/parent lookup |
| `FX-002` | DB seed | `CommunityTopic{name:"Dinh dưỡng thai kỳ", type:TOPIC, parentId:FX-001.id}` | Valid TOPIC child |
| `FX-003` | DB seed | `CommunityTopic{name:"Mang thai (ẩn)", type:CATEGORY, parentId:null, isHidden:true}` | Parent-hidden reject case |
| `FX-004` | DB seed | 3× `CommunityQuestion{topicId:FX-002.id, status:APPROVED}` + 1× `{status:PENDING}` + 1× `{status:HIDDEN}` | questionCount phải = 3, không phải 5 |
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

    static CommunityTopic makeCategory() {
        return makeCategory(t -> {});
    }

    static CommunityTopic makeCategory(java.util.function.Consumer<CommunityTopic> overrides) {
        CommunityTopic topic = CommunityTopic.builder()
                .id(UUID.randomUUID())
                .name("Mang thai")
                .description("desc")
                .icon("pregnant_woman")
                .type(TopicType.CATEGORY)
                .slug("mang-thai")
                .parentId(null)
                .isHidden(false)
                .sortOrder(1)
                .createdBy(UUID.randomUUID())
                .build();
        overrides.accept(topic);
        return topic;
    }

    static CommunityTopic makeTopic(UUID categoryId) {
        return makeCategory(t -> {
            t.setId(UUID.randomUUID());
            t.setName("Dinh dưỡng thai kỳ");
            t.setIcon("restaurant");
            t.setType(TopicType.TOPIC);
            t.setSlug("dinh-duong-thai-ky");
            t.setParentId(categoryId);
        });
    }

    static CreateCommunityTopicRequest makeCreateTopicRequest(java.util.function.Consumer<CreateCommunityTopicRequest.CreateCommunityTopicRequestBuilder> overrides) {
        var builder = CreateCommunityTopicRequest.builder()
                .name("Sức khỏe tinh thần")
                .description("desc")
                .type(TopicType.CATEGORY)
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

### COM-TC-005 — Creating a TOPIC without a CATEGORY parentId is rejected

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.validateHierarchy()` (via `createTopic()`)
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🔴 RED — rewritten for Amendment 2
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-COM-020`

**Test Steps:**
1. Arrange: `makeCreateTopicRequest(r -> r.type(TopicType.TOPIC).parentId(null))`
2. Act: `service.createTopic(modId, request)`
3. Assert: throws `InvalidTopicHierarchyException`; `topicRepository.save()` **never called**

**Expected Result (PASS):** exception thrown, không có side-effect ghi DB.
**Expected Result (FAIL):** topic được tạo với `type=TOPIC` và `parentId=null`.

**Milestone B RED evidence:** service still accepted a TOPIC root before Milestone C.

---

### COM-TC-006 — Creating a root CATEGORY without parentId is accepted

**Severity:** `HIGH`
**Feature Under Test:** `validateHierarchy()` / `createTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-COM-020`

**Test Steps:**
1. Arrange: `makeCreateTopicRequest(r -> r.type(TopicType.CATEGORY).parentId(null))`
2. Act: `service.createTopic(modId, request)`
3. Assert: **không** throw; `topicRepository.save()` được gọi; response có `type=CATEGORY`, `parentId=null`

**Expected Result (PASS):** CATEGORY tạo thành công với `parentId=null`, đúng invariant root.
**Expected Result (FAIL):** throw `InvalidTopicHierarchyException` hoặc tự gán parent.
**Current Status:** 🟢 Passing

---

### COM-TC-007 — TOPIC parentId pointing to a non-CATEGORY entity is rejected

**Severity:** `HIGH`
**Feature Under Test:** `validateHierarchy()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🔴 RED — rewritten for Amendment 2
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-COM-020`

**Preconditions:** một TAG hoặc TOPIC khác tồn tại; lookup `findByIdAndTypeAndIsHiddenFalse(parentId, CATEGORY)` trả `Optional.empty()`.

**Test Steps:**
1. Arrange: `makeCreateTopicRequest(r -> r.type(TOPIC).parentId(nonCategoryId))`
2. Act: `service.createTopic(modId, request)`
3. Assert: throws `InvalidTopicHierarchyException` hoặc `CommunityTopicNotFoundException` (theo §10 TDS — cả 2 exception đều hợp lệ vì lookup dùng `findByIdAndTypeAndIsHiddenFalse` trả rỗng khi type sai)

**Expected Result (PASS):** không tạo được TOPIC với cha là TAG/TOPIC.
**Milestone B RED evidence:** CATEGORY-typed parent lookup was not invoked before Milestone C.

---

### COM-TC-008 — TOPIC parentId pointing to a hidden CATEGORY is rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `validateHierarchy()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🔴 RED — rewritten for Amendment 2
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-COM-020`

**Preconditions:** `FX-003` (hidden CATEGORY).

**Test Steps:**
1. Arrange: request `type=TOPIC, parentId=FX-003.id`; mock `findByIdAndTypeAndIsHiddenFalse(FX-003.id, CATEGORY)` → `Optional.empty()`
2. Act / Assert: throws exception, không tạo TOPIC dưới CATEGORY đã ẩn

**Milestone B RED evidence:** CATEGORY-typed parent lookup was not invoked before Milestone C.

---

### COM-TC-009 — Valid TOPIC under an existing CATEGORY succeeds with questionCount=0

**Severity:** `CRITICAL`
**Feature Under Test:** `createTopic()` happy path
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🔴 RED — rewritten for Amendment 2
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-COM-015`, `ADR-COM-020`

**Test Steps:**
1. Arrange: `FX-001` (visible CATEGORY) exists; mock lookup returns it; request `type=TOPIC, parentId=FX-001.id`
2. Act: `service.createTopic(modId, request)`
3. Assert: response `type=TOPIC`, `parentId=FX-001.id`, `slug` non-null/non-empty, `questionCount=0`, `isHidden=false`

**Expected Result (PASS):** đúng như trên.
**Milestone B RED evidence:** the v1 service rejected a TOPIC with parentId before Milestone C.

---

### COM-TC-010 — questionCount counts only APPROVED questions (unit, mocked repo)

**Severity:** `CRITICAL`
**Feature Under Test:** `CommunityTopicServiceImpl.getTopics()` question-count hydration
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-COM-015` (user-approved decision — APPROVED only)

**Test Steps:**
1. Arrange: `topicRepository.findAll...()` trả `[FX-002]`; `questionRepository.countApprovedQuestionsByTopicIds([FX-002.id])` mock trả projection `[{topicId:FX-002.id, cnt:3}]`
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

### COM-TC-012 — Changing type after creation is rejected with COM-017

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityTopicServiceImpl.updateTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🔴 RED — rewritten for Amendment 2
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `ADR-COM-025`, `CommunityTopicManagement_TDS.md §9.2`

**Preconditions:** existing entity `type=CATEGORY, parentId=null`.

**Test Steps:**
1. Arrange: `UpdateCommunityTopicRequest{type:TOPIC, parentId:FX-001.id}`
2. Act: `service.updateTopic(id, modId, request)`
3. Assert: throws `ImmutableTopicTypeException` (`COM-017`, HTTP 400); entity **không** bị save

**Milestone B RED evidence:** service threw COM-015 instead of immutable-type COM-017 before Milestone C.

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

### COM-TC-INT-002 — Migration enforces CATEGORY-root/TOPIC-child at the DB level

**Severity:** `HIGH`
**Feature Under Test:** `V20260721204919__add_community_topic_taxonomy.sql` — `community_topics_parent_rule_check`
**Test File:** `CommunityTopicIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — verified 2026-07-22 against real Testcontainers PostgreSQL
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-COM-020` — DB CHECK enforces the nullability half of the v2 invariant; the parent's `type=CATEGORY` remains a service-layer cross-row validation.

**Test Steps:**
1. Seed một CATEGORY root hợp lệ.
2. Raw INSERT `type='TOPIC', parent_id=<category-id>` qua `JdbcTemplate` phải thành công.
3. Raw INSERT `type='TOPIC', parent_id=NULL` phải ném `DataIntegrityViolationException`.
4. Raw INSERT `type='CATEGORY', parent_id=<category-id>` phải ném `DataIntegrityViolationException`; `type='CATEGORY', parent_id=NULL` phải thành công.

**Expected Result (PASS):** TOPIC bắt buộc có parent; CATEGORY bắt buộc là root. Việc parent có đúng `type=CATEGORY` được COM-TC-021/022 kiểm ở service.

**Current Status:** 🟢 Passing — verified again in Milestone F against PostgreSQL 16 Testcontainers. The initial Docker-unavailable attempt remains recorded historically; the current result is GREEN.

---

### WEB TEST CASE (pure-logic unit — vitest, không component/RTL — xem TDS-01)

---

### WEB-TC-001 — buildTopicTree nests each TOPIC only under its real CATEGORY parentId

**Severity:** `HIGH`
**Feature Under Test:** `buildTopicTree()` (helper mới, extract từ `ManageTopicsPage.tsx` render logic hiện tại thành hàm thuần để test được — thay cho logic cũ "lồng mọi category dưới mọi topic mở")
**Test File:** `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/topicTree.test.ts` (mới)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CommunityTopicManagement_TDS.md` §Chặng 4 (Web implementation step), Logic Issue L4 (§2)

**Preconditions:** input: `topics = [CategoryA(type=CATEGORY), CategoryB(type=CATEGORY), TopicX(type=TOPIC, parentId=CategoryA.id)]`.

**Test Steps:**
1. Arrange: input array trên, `expandedIds = new Set([CategoryA.id, CategoryB.id])` (cả 2 category đều đang mở)
2. Act: `buildTopicTree(topics, expandedIds)`
3. Assert: `TopicX` xuất hiện đúng 1 lần trong output, là con của `CategoryA`; **không** xuất hiện dưới `CategoryB`

**Expected Result (PASS):** đúng như trên — hierarchy CATEGORY → TOPIC và parentId đều được tôn trọng.
**Expected Result (FAIL):** `TopicX` xuất hiện dưới cả hai CATEGORY hoặc bị render theo chiều TOPIC → CATEGORY cũ.

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

### AMENDMENT 2 TEST CASES (nhóm G-K, ADR-COM-020 → 025) — backend GREEN; RED evidence retained

---

### COM-TC-020 — Creating a TOPIC without parentId is rejected (v2: parent is now mandatory)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.validateHierarchy()` (v2)
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `ADR-COM-020`

**Test Steps:**
1. Arrange: `makeCreateTopicRequest(r -> r.type(TopicType.TOPIC).parentId(null))`
2. Act: `service.createTopic(modId, request)`
3. Assert: throws `InvalidTopicHierarchyException` với message chứa "must belong to a parent CATEGORY"; `save()` never called

**Expected Result (PASS):** exception thrown, không ghi DB.
**Expected Result (FAIL):** topic được tạo với `type=TOPIC`, `parentId=null` (hành vi v1 cũ, nay sai).

**Milestone B RED evidence:** current service still accepted the invalid v1 behavior before Milestone C.

---

### COM-TC-021 — Creating a TOPIC with parentId pointing to a TAG (not CATEGORY) is rejected

**Severity:** `HIGH`
**Feature Under Test:** `validateHierarchy()` (v2)
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `ADR-COM-020`

**Test Steps:**
1. Arrange: `parentId` trỏ tới 1 row có `type=TAG`; mock `findByIdAndTypeAndIsHiddenFalse(parentId, CATEGORY)` trả `Optional.empty()`
2. Act: `service.createTopic(modId, request(type=TOPIC, parentId))`
3. Assert: throws `InvalidTopicHierarchyException`

**Expected Result (PASS):** exception thrown.
**Expected Result (FAIL):** topic được tạo với cha là 1 TAG.

**Milestone B RED evidence:** CATEGORY-typed parent validation was absent before Milestone C.

---

### COM-TC-022 — Creating a TOPIC with parentId pointing to a hidden CATEGORY is rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `validateHierarchy()` (v2)
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `ADR-COM-020`

**Test Steps:**
1. Arrange: `findByIdAndTypeAndIsHiddenFalse(parentId, CATEGORY)` trả `Optional.empty()` (vì category đang `isHidden=true`, dù tồn tại)
2. Act: `createTopic(...)`
3. Assert: throws `InvalidTopicHierarchyException`

**Expected Result (PASS):** exception thrown.
**Milestone B RED evidence:** hidden CATEGORY validation was absent before Milestone C.

---

### COM-TC-023 — Creating a TOPIC under a valid visible CATEGORY succeeds (happy path v2)

**Severity:** `HIGH`
**Feature Under Test:** `createTopic()` (v2)
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-023`
**Oracle Source:** `ADR-COM-020`

**Test Steps:**
1. Arrange: `findByIdAndTypeAndIsHiddenFalse(parentId, CATEGORY)` trả `Optional.of(categoryEntity)`
2. Act: `createTopic(modId, request(type=TOPIC, parentId))`
3. Assert: `save()` được gọi với entity có `type=TOPIC`, `parentId` đúng bằng category id; response `parentId` khớp

**Expected Result (PASS):** tạo thành công, `parentId` lưu đúng.
**Milestone B RED evidence:** the valid v2 TOPIC-under-CATEGORY path failed before Milestone C.

---

### COM-TC-024 — Creating a CATEGORY with a parentId is rejected (categories are always top-level)

**Severity:** `HIGH`
**Feature Under Test:** `validateHierarchy()` (v2)
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 Regression already satisfied; retained in Amendment 2 suite
**Condition Ref:** `TC-COND-024`
**Oracle Source:** `ADR-COM-020`

**Test Steps:**
1. Arrange: `request(type=CATEGORY, parentId=UUID.randomUUID())`
2. Act: `createTopic(...)`
3. Assert: throws `InvalidTopicHierarchyException`; `save()` never called

**Expected Result (PASS):** exception thrown.
**Expected Result (FAIL):** category được tạo với 1 parent (v1 cũ cho phép, nay sai).
**Current Status:** 🟢 Existing validation already satisfies this isolated assertion; retained as regression coverage.

---

### COM-TC-025 — Creating a TAG with a parentId is rejected (tags stay flat)

**Severity:** `MEDIUM`
**Feature Under Test:** `validateHierarchy()` (v2)
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 Regression already satisfied; retained in Amendment 2 suite
**Condition Ref:** `TC-COND-025`
**Oracle Source:** `ADR-COM-020`

**Test Steps:**
1. Arrange: `request(type=TAG, parentId=UUID.randomUUID())`
2. Act: `createTopic(...)`
3. Assert: throws `InvalidTopicHierarchyException`

**Expected Result (PASS):** exception thrown.
**Current Status:** 🟢 Existing validation already satisfies this isolated assertion; retained as regression coverage.

---

### COM-TC-026 (Integration) — Migration backfill: exactly 5 categories, 8 topics correctly reassigned

**Severity:** `CRITICAL`
**Feature Under Test:** Migration `V20260722054603__invert_community_topic_hierarchy.sql`
**Test File:** `CommunityTopicIntegrationTest.java` (Testcontainers PostgreSQL, chạy toàn bộ chuỗi Flyway migration thật)
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — migration/backfill verified again on PostgreSQL 16 Testcontainers in Milestone F. Docker was unavailable during the first historical attempt, then became available and has passed repeatedly since Milestone C.
**Condition Ref:** `TC-COND-026`
**Oracle Source:** `TDS §5.3` mapping bảng

**Preconditions:** Docker available (Testcontainers).

**Test Steps:**
1. Arrange: container khởi tạo DB sạch, Flyway chạy hết toàn bộ migration kể cả migration mới
2. Act: query `SELECT name, slug FROM community_topics WHERE type='CATEGORY' ORDER BY sort_order`
3. Assert: đúng 5 row, tên khớp `["Chuẩn bị mang thai","Mang thai","Sau sinh","Chăm bé","Khác"]`, tất cả `parent_id IS NULL`
4. Act: query `SELECT id, parent_id FROM community_topics WHERE type='TOPIC'`
5. Assert: đúng 8 row, `parent_id` mỗi row khớp đúng mapping ở TDS §5.3 (vd. `...567801` → category "Mang thai")
6. Act: chạy query verification ở TDS §5.3 (`orphan_topics`, `invalid_parents`)
7. Assert: cả 2 = 0

**Expected Result (PASS):** đúng cấu trúc như trên; số `community_questions` gắn vào 8 topic id cũ (`WHERE topic_id IN (...)`) khớp đúng số đã seed ở `V10__seed_community_topics.sql` (không đếm "trước/sau" — Testcontainers chỉ quan sát được state SAU khi toàn bộ chuỗi Flyway chạy xong, không có điểm đo "trước migration mới" trong cùng 1 test run).
**Expected Result (FAIL):** thiếu/thừa category, topic nào đó `parent_id` sai hoặc `NULL`, hoặc số câu hỏi gắn vào 8 topic id không khớp seed (nghĩa là ID bị đổi/mất liên kết).

**Milestone B RED evidence:** migration was absent (0 CATEGORY versus expected 5).
**Note:** trên DB sạch (Testcontainers) số category = 5 đúng nguyên văn; trên Supabase dev DB chia sẻ có thể nhiều hơn do dữ liệu QA cũ còn tồn tại (xem TDS §5.3 "Shared-DB caveat") — test case này chỉ chạy trên Testcontainers (DB sạch), không chạy trên shared DB.

---

### COM-TC-027 — DELETE a CATEGORY that still has TOPIC children is rejected (409 COM-016)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.deleteTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-027`
**Oracle Source:** `ADR-COM-022`

**Test Steps:**
1. Arrange: `topicRepository.findById(categoryId)` trả category entity; `existsByParentId(categoryId)` trả `true`
2. Act: `service.deleteTopic(categoryId, modId)`
3. Assert: throws `TopicHasDependentsException`; `topicRepository.delete()` never called

**Expected Result (PASS):** exception thrown, không xoá.
**Milestone B RED evidence:** planned delete/repository contracts were absent.

---

### COM-TC-028 — DELETE an empty CATEGORY succeeds (204)

**Severity:** `HIGH`
**Feature Under Test:** `deleteTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-028`
**Oracle Source:** `ADR-COM-022`

**Test Steps:**
1. Arrange: child/question/follow checks đều trả `false`
2. Act: `service.deleteTopic(categoryId, modId)`
3. Assert: `topicRepository.delete(entity)` được gọi đúng 1 lần; `auditService.log(...)` được gọi với `action=DELETE`

**Expected Result (PASS):** xoá thành công, có audit log.
**Milestone B RED evidence:** planned delete/repository contracts were absent.

---

### COM-TC-029 — DELETE a TOPIC that has questions attached is rejected (409 COM-016)

**Severity:** `HIGH`
**Feature Under Test:** `deleteTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-029`
**Oracle Source:** `ADR-COM-022`

**Test Steps:**
1. Arrange: topic entity `type=TOPIC`; child=false, question=true, follow=false
2. Act: `service.deleteTopic(topicId, modId)`
3. Assert: throws `TopicHasDependentsException`; `delete()` never called

**Expected Result (PASS):** exception thrown.
**Milestone B RED evidence:** planned delete/repository contracts were absent.

---

### COM-TC-030 — DELETE a TOPIC with no dependents succeeds (204)

**Severity:** `MEDIUM`
**Feature Under Test:** `deleteTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-030`
**Oracle Source:** `ADR-COM-022`

**Test Steps:**
1. Arrange: child/question/follow checks đều trả `false`
2. Act: `service.deleteTopic(topicId, modId)`
3. Assert: `delete()` được gọi

**Expected Result (PASS):** xoá thành công.
**Milestone B RED evidence:** planned delete/repository contracts were absent.

---

### COM-TC-031 — Creating a community question with topicId pointing to a CATEGORY is rejected

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl` (create question path)
**Test File:** `CommunityQuestionServiceImplTest.java` (file có sẵn — thêm test method)
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — typed TOPIC lookup verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-031`
**Oracle Source:** `ADR-COM-021`

**Test Steps:**
1. Arrange: `topicRepository.findByIdAndTypeAndIsHiddenFalse(categoryId, TOPIC)` trả `Optional.empty()` (row tồn tại nhưng `type=CATEGORY`, không phải `TOPIC`)
2. Act: `questionService.createQuestion(motherId, request(topicId=categoryId, ...))`
3. Assert: throws exception hiện có cho "topic not found" (COM-003, không đổi mã lỗi)

**Expected Result (PASS):** exception thrown, câu hỏi không được lưu.
**Expected Result (FAIL):** câu hỏi được lưu với `topic_id` trỏ tới 1 CATEGORY (dữ liệu vô nghĩa).

**Milestone B RED evidence:** service still invoked the untyped topic lookup.

---

### COM-TC-032 — DELETE a TOPIC with an active follow is rejected (409 COM-016)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityTopicServiceImpl.deleteTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).
**Condition Ref:** `TC-COND-034`
**Oracle Source:** `ADR-COM-022`

**Test Steps:**
1. Arrange: target row `type=TOPIC`; child count = 0, question count = 0, follow count > 0.
2. Act: `service.deleteTopic(topicId, modId)`.
3. Assert: throws `TopicHasDependentsException` (`COM-016`); `topicRepository.delete()` never called.

**Milestone B RED evidence:** planned delete/repository contracts were absent.

---

### COM-TC-033 — DELETE a legacy CATEGORY or TAG with a question/follow is rejected

**Severity:** `CRITICAL`
**Feature Under Test:** universal dependency guard in `deleteTopic()`
**Test File:** `CommunityTopicServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing — all CATEGORY/TAG question/follow combinations verified in the Milestone F targeted backend suite.
**Condition Ref:** `TC-COND-035`
**Oracle Source:** `ADR-COM-022`, Decision A

**Test Steps:**
1. Parameterize target type over `CATEGORY`, `TAG` and dependent kind over `question`, `follow`.
2. Arrange exactly one dependency count > 0, all remaining counts = 0.
3. Act: `service.deleteTopic(id, modId)`.
4. Assert every combination throws `TopicHasDependentsException`; delete never occurs.

**Expected Result (PASS):** guard does not branch on target type; legacy/edge-case rows cannot leak a database FK failure as HTTP 500.
**Milestone B RED evidence:** planned type-agnostic dependency contracts were absent.

---

### COM-TC-034 — DELETE controller returns 204 on success

**Severity:** `HIGH`
**Feature Under Test:** `DELETE /api/v1/community/topics/{id}`
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-036`
**Oracle Source:** `ADR-COM-022`, TDS §9.2

**Test Steps:** authenticate with the same privileged role accepted by CREATE/UPDATE, invoke DELETE, assert `204 No Content` and one service invocation with target id and actor id.

**Milestone B RED evidence:** no DELETE handler existed; response was 500 via the generic handler.

**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67).

---

### COM-TC-035 — DELETE controller maps dependents conflict to 409 COM-016

**Severity:** `HIGH`
**Feature Under Test:** DELETE endpoint + global exception mapping
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-037`
**Oracle Source:** `ADR-COM-022`, TDS §10

**Test Steps:** privileged request; service raises `TopicHasDependentsException`; assert HTTP 409 and response error code `COM-016`.

**Milestone B RED evidence:** DELETE and TopicHasDependentsException contracts were absent.

**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67), including COM-016 response mapping.

---

### COM-TC-036 — MOTHER cannot DELETE a topic

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** DELETE endpoint RBAC
**Test File:** `CommunityTopicControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-038`
**Oracle Source:** `BR-RBAC`

**Test Steps:** `@WithMockUser(roles={"MOTHER"})`; issue DELETE; assert 403 and service never invoked.

**Milestone B RED evidence:** without endpoint/method security, response was 500 rather than 403.

**Current Status:** 🟢 Passing — verified again in Milestone F targeted backend suite (67/67); MOTHER receives 403.

---

### COM-TC-037 — Creating a question against a valid TOPIC still succeeds

**Severity:** `CRITICAL`
**Feature Under Test:** `CommunityQuestionServiceImpl` create path regression
**Test File:** `CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-039`
**Oracle Source:** `ADR-COM-021`

**Test Steps:** lookup `(id, TOPIC, visible)` returns `FX-002`; create a valid question request; assert saved question retains `topicId=FX-002.id` and response succeeds.

**Milestone B RED evidence:** service still invoked the untyped topic lookup.

**Current Status:** 🟢 Passing — valid TOPIC question creation regression verified again in Milestone F targeted backend suite (67/67).

---

### COM-TC-038 (Integration) — Migration preserves UserTopicFollow rows and topic ids

**Severity:** `CRITICAL`
**Feature Under Test:** hierarchy inversion migration data preservation
**Test File:** `CommunityTopicIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-040`
**Oracle Source:** TDS §5.3, Decision D

**Test Steps:** use the same seed-count-comparison pattern as COM-TC-026: capture seeded follow row count and ordered `topic_id` values before the inversion step, run the migration, then assert both count and values are unchanged.

**Expected Result (PASS):** no follow row is deleted, reassigned, or orphaned while the existing topic ids are retained.
**Milestone B RED evidence:** pre/post snapshot was stable, but the absent migration left 0 v2 CATEGORY rows.

**Current Status:** 🟢 Passing — row count and ordered `topic_id` values verified unchanged again in Milestone F on PostgreSQL 16 Testcontainers. Docker was unavailable only during the first historical attempt; it is available and this test is GREEN now.

---

### WEB TEST CASE (Amendment 2)

---

### WEB-TC-002 — buildTopicTree v2 renders CATEGORY as top-level, TOPIC nested under matching parentId

**Severity:** `HIGH`
**Feature Under Test:** `buildTopicTree()` (v2, `topicTree.ts`)
**Test File:** `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/topicTree.test.ts` (file có sẵn — thêm test case)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-032`
**Oracle Source:** `ADR-COM-020`, `ADR-COM-023`

**Test Steps:**
1. Arrange: `categoryA = topic({type:'CATEGORY', parentId:null})`, `topicX = topic({type:'TOPIC', parentId:'category-a'})`
2. Act: `buildTopicTree([categoryA, topicX], new Set(['category-a']))`
3. Assert: `rows[0].item.id === 'category-a'`, `rows[0].isChild === false`; `rows[1].item.id === 'topic-x'`, `rows[1].isChild === true` (chỉ khi `category-a` đang expanded)

**Expected Result (PASS):** thứ tự lồng đúng CATEGORY ngoài → TOPIC trong.
**Expected Result (FAIL):** vẫn lồng theo chiều cũ (TOPIC ngoài, CATEGORY trong) hoặc không lồng gì cả.

**Current Status:** 🟢 Passing — RED 4/4 rồi GREEN 4/4 trong Milestone D.

---

### MOBILE TEST CASE (Amendment 2)

---

### MOB-TC-002 — TopicDirectoryScreen category chips built from real CATEGORY API data, not hardcoded strings

**Severity:** `MEDIUM`
**Feature Under Test:** pure CATEGORY-to-chip mapping/filter helper extracted from `topic_directory_screen.dart`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/community/topic_directory_screen_test.dart` (file có sẵn — thêm test)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-033`
**Oracle Source:** `ADR-COM-024`

**Test Steps:**
1. Arrange: input pure helper với 2 CATEGORY rows `["Mang thai","Sau sinh"]` và một TOPIC row nhiễu.
2. Act: map rows thành chip data.
3. Assert: output đúng `["Tất cả","Mang thai","Sau sinh"]`; TOPIC row và hardcoded `"Chăm bé"` không xuất hiện.

**Expected Result (PASS):** chip data phản ánh đúng input CATEGORY, không phải danh sách tĩnh cố định.
**Expected Result (FAIL):** helper vẫn trả `["Tất cả","Mang thai","Sau sinh","Chăm bé"]` bất kể input (hardcode còn sót).

**Current Status:** 🟢 Passing — RED compile đúng vì helper/model fields chưa tồn tại; GREEN trong targeted/community suite.

**Scope Caveat:** theo precedent MOB-TC-001, test nhắm vào hàm top-level thuần được extract; không pump toàn widget và không refactor singleton `CommunityService` để DI.

---

### MOB-TC-003 — Inline search works with category filtering; dedicated search route is fully removed

**Severity:** `HIGH`
**Feature Under Test:** pure inline-search/filter logic + static route/reference removal
**Test File:** `topic_directory_screen_test.dart` và static verification command
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-041`
**Oracle Source:** `ADR-COM-024`, Decision D/E

**Test Steps:**
1. Feed the extracted pure filter function with TOPIC rows across two parent CATEGORY ids and a search query; assert only name/description matches under the selected category remain.
2. Assert empty query and “Tất cả” return the unfiltered TOPIC set.
3. Static verification: `CommunityTopicSearchScreen`, its route, import, and navigation call have zero references; the obsolete screen file is absent.

**Expected Result (PASS):** search is available inline on `TopicDirectoryScreen` and no dangling navigation can reach the deleted screen.
**Current Status:** 🟢 Passing — pure filter assertions và static zero-reference verification đều đạt.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|---------------------|------------------------|------------------------|
| `COM-TC-001` | `community/util/SlugGeneratorTest.java` | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-002` | `community/CommunityTopicServiceImplTest.java` | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-003` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-004` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-005` | (idem) | `[x]` — rewritten v2 fails (old code accepts TOPIC root) | — | Amendment 2 rewrite |
| `COM-TC-006` | (idem) | N/A — invariant already matches CATEGORY root | 2026-07-21 | Rewritten fixture/direction; regression stays GREEN legitimately |
| `COM-TC-007` | (idem) | `[x]` — planned CATEGORY lookup not invoked | — | Amendment 2 rewrite |
| `COM-TC-008` | (idem) | `[x]` — planned CATEGORY lookup not invoked | — | Amendment 2 rewrite |
| `COM-TC-009` | (idem) | `[x]` — old service rejects TOPIC child | — | Amendment 2 rewrite |
| `COM-TC-010` | (idem) | `[x]` | 2026-07-21 (uncommitted) | — |
| `COM-TC-011` | `community/CommunityTopicControllerTest.java` | `[ ]` — pure routing test, not stub-gated (see §5.1 note) | 2026-07-21 (uncommitted) | Passed as soon as written; not a Green-from-Birth violation — controller has no deferred business logic, service is fully mocked |
| `COM-TC-012` | `community/CommunityTopicServiceImplTest.java` | `[x]` — COM-015 observed instead of COM-017 | — | Rewritten for immutable type |
| `COM-TC-SEC-001` | `community/CommunityTopicControllerTest.java` | `[x]` (pre-existing test, reused — RBAC unchanged) | 2026-07-21 (uncommitted) | This TC maps to the already-existing `createTopic_asMotherUser_shouldReturn403` regression test |
| `COM-TC-INT-001` | `community/CommunityTopicIntegrationTest.java` | `[x]` | 2026-07-22 (uncommitted at test time) | Verified GREEN against real Testcontainers PostgreSQL after Docker Desktop was started |
| `COM-TC-INT-002` | (idem) | `[x]` — rewritten v2 DB CHECK fails against applied v1 constraint | — | Amendment 2 rewrite; prior v1 verification retained in changelog |
| `WEB-TC-001` | `contentManagement/pages/topicTree.test.ts` | ⚠️ Not stub-gated | 2026-07-21 (uncommitted) | `buildTopicTree` was implemented directly, then the test was written and run against it (3/3 pass) — did NOT follow strict red-stub-first order. No dependency to stub (pure function); low Green-from-Birth risk, but flagged honestly per Truthful Sync policy. |
| `MOB-TC-001` | `test/features/community/topic_directory_screen_test.dart` | ⚠️ Not stub-gated | 2026-07-21 (uncommitted) | Same as WEB-TC-001 — `questionCountLabel` implemented first, then tested (2/2 pass). Also: test targets an extracted top-level function, not the originally-planned `pumpWidget(_TopicGridCard(...))` — `CommunityService`'s private constructor makes full widget-pump testing impractical without a larger DI refactor (out of scope). |
| `COM-TC-020`…`COM-TC-025` | `community/CommunityTopicServiceImplTest.java` | `[x]` 4/6 RED; 024/025 legitimately GREEN because v1 already forbids parent for CATEGORY/TAG | `[x]` — Milestone C | v2 hierarchy enforced |
| `COM-TC-026` | `community/CommunityTopicIntegrationTest.java` | `[x]` — 0 CATEGORY vs expected 5 | `[x]` — PostgreSQL 16 Testcontainers | migration/backfill verified |
| `COM-TC-027`…`COM-TC-030`, `COM-TC-032`…`COM-TC-033` | `community/CommunityTopicServiceImplTest.java` | `[x]` — planned repository/delete contracts absent | `[x]` — Milestone C | universal delete guard |
| `COM-TC-031`, `COM-TC-037` | `community/service/CommunityQuestionServiceImplTest.java` | `[x]` — service still calls untyped lookup | `[x]` — Milestone C | typed TOPIC lookup + success regression |
| `COM-TC-034`…`COM-TC-036` | `community/CommunityTopicControllerTest.java` | `[x]` — endpoint/COM-016 contract absent (500/missing class) | `[x]` — Milestone C | DELETE 204/409/403 |
| `COM-TC-038` | `community/CommunityTopicIntegrationTest.java` | `[x]` — legacy snapshot preserved but hierarchy migration absent | `[x]` — PostgreSQL 16 Testcontainers | follow rows/IDs preserved |
| `WEB-TC-002` | `contentManagement/pages/topicTree.test.ts` | `[x]` — 4/4 failed against v1 direction | `[x]` — Milestone D | CATEGORY root, TOPIC child, TAG flat; sortOrder retained |
| `MOB-TC-002`…`MOB-TC-003` | `test/features/community/topic_directory_screen_test.dart` | `[x]` — compile failed vì pure helpers/model fields chưa tồn tại | `[x]` — Milestone E, 6/6 community tests | Pure helper approach; no singleton DI refactor; `flutter analyze` toolchain crash, scoped `dart analyze` clean |

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

### Amendment 2 — Entry / Exit Criteria (ADR-COM-020..025, riêng biệt với DoD v1.0 ở trên)

**Entry Criteria:**
- [x] TDS Amendment 2 (§ADR-COM-020..025) đã đổi `Status` → `Approved`
- [x] Test-Spec này (Amendment 2 section) đã đổi `Status` → `Approved`

**Exit Criteria (DoD):**
- [x] `COM-TC-020`…`COM-TC-025` (hierarchy v2) — GREEN
- [x] `COM-TC-026` (migration backfill, PostgreSQL 16 Testcontainers) — GREEN
- [x] `COM-TC-027`…`COM-TC-030` (delete) — GREEN
- [x] `COM-TC-031` (question chỉ gắn TOPIC) — GREEN
- [x] `COM-TC-032`…`COM-TC-038` (Decision D backend additions) — GREEN
- [x] `WEB-TC-002` — GREEN
- [x] `MOB-TC-002`, `MOB-TC-003` — GREEN
- [x] Source/test/config/route/nav sweep xác nhận không còn runtime reference tới `ContentCategoryController`, `/api/v1/admin/content/categories`, `ContentCategoryListPage`, `/content/categories`, `community_topic_search_screen`, hoặc `CommunityTopicSearchScreen`. Historical/spec references are retained only where they document the superseded implementation or removal; the older UC-226/UC-171 specs, UI screen tracking, MF11 design note, and the immutable applied v1 migration comment are reported as out-of-scope documentation/history follow-ups rather than runtime dangling references.
- [x] Full regression backend/web/mobile — 0 regression mới do Amendment 2 gây ra: clean backend run reproduces the exact unrelated baseline (2,419 tests; 9 failures, 68 errors, 1 skipped), web checks pass, and full mobile suite passes 252/252. Full `flutter analyze` remains unavailable because the analysis server crashes before diagnostics; scoped `dart analyze lib/features/community test/features/community` is clean.
- [x] Full regression backend — không còn failure/error do Amendment 2; 9 failures/68 errors còn lại là baseline ngoài phạm vi
- [x] Full regression web unit/build/lint — 18/18 unit PASS; 4 Playwright specs bị Vitest collect nhầm là baseline ngoài phạm vi

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

*Amendment 2 đã hoàn tất implementation và automated verification trong working tree chưa commit. Tài liệu vẫn chờ human browser/device QA và user final approval; không tự động nâng trạng thái thành “Approved final”.*
