# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification — Pending Content Queue (First-Time Moderation)

| Field              | Value                                   |
| ------------------ | ---------------------------------------- |
| **Document ID**    | `CB-MOD-IMP-004`                        |
| **Version**        | `1.0`                                    |
| **Date**           | `2026-07-03`                             |
| **Status**         | `Draft`                                  |
| **Document Owner** | `HuyND`                                  |
| **Author**         | `AI Agent — Winston (System Architect)`  |
| **Reviewed by**    | `[ ] Pending`                            |
| **DPO Sign-off**   | `[ ] Pending — Internal data only, N/A`  |
| **Approved by**    | `[ ] Pending`                            |
| **Last Review**    | `2026-07-03`                             |
| **Based on EDS**   | `v2.0`                                   |

---

## CHANGELOG

| Ngày       | Người thực hiện          | Nội dung thay đổi                                                                 |
| ---------- | ------------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-03 | AI Agent — Winston        | Tạo tài liệu lần đầu — TDS mở rộng UC-99, bổ sung queue duyệt nội dung lần đầu     |

---

## MỤC LỤC

1. [Tổng quan Module](#1-tổng-quan-module)
2. [Ma trận Truy vết](#2-ma-trận-truy-vết)
3. [Architecture Decision Records (ADR)](#3-architecture-decision-records-adr)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling](#5-static-modeling)
6. [Dynamic Modeling](#6-dynamic-modeling)
7. [Interface Specification](#7-interface-specification)
8. [API Specification](#8-api-specification)
9. [Bảng mã lỗi](#9-bảng-mã-lỗi)
10. [Frontend Changes](#10-frontend-changes)
11. [Quy trình Triển khai](#11-quy-trình-triển-khai)
12. [Rollback & Incident Runbook](#12-rollback--incident-runbook)
13. [Phương pháp Xác minh](#13-phương-pháp-xác-minh)
14. [Authorization Matrix](#14-authorization-matrix)
15. [AI Prompt Constraints (CASE 2.0)](#15-ai-prompt-constraints-case-20)

---

## 1. Tổng quan Module

| Field                     | Value                                                                                     |
| -------------------------- | ------------------------------------------------------------------------------------------ |
| **Extends**                | `UC-99 (View Moderation Queue)` + `UC-100 (Moderate Community Content)` — không có UC number riêng, xem §1.1 |
| **Module Name**            | `Pending Content Queue`                                                                    |
| **Bounded Context**        | `content` + `community`                                                                    |
| **Primary Actor**          | `Community Moderator (ROLE_MODERATOR)`                                                     |
| **Platform**               | `Admin Web Portal`                                                                          |
| **Priority**               | `High — chặn toàn bộ nội dung community mới không bao giờ hiển thị công khai`               |
| **Data Classification**    | `Internal`                                                                                  |
| **Compliance Scope**       | `N/A`                                                                                       |
| **Upstream Dependencies**  | `community (CommunityQuestion, CommunityAnswer)`, `content (ContentPreviewService, ModerationService.moderateContent — UC-100, không đổi)` |
| **Downstream Consumers**   | `Admin Web Portal — Moderation module`                                                      |

**Mô tả:**
Sau khi implement UC-59-style "like câu hỏi" trong session này, người dùng phát hiện: câu hỏi/câu trả lời mới đăng luôn ở trạng thái `PENDING` và **không có nơi nào trong Admin Web Portal để moderator duyệt lần đầu**. `GET /api/v1/admin/moderation/queue` (UC-99) chỉ query bảng `ContentReport` (ADR-001 của UC-99 — "Aggregated Report View Pattern"), tức là chỉ hiển thị nội dung đã bị **báo cáo**. Nội dung mới đăng, chưa từng bị báo cáo, không xuất hiện ở đâu cả — moderator không có cách nào biết nó tồn tại để duyệt.

Điểm quan trọng: `POST /api/v1/admin/moderation/actions` (UC-100) đã được thiết kế từ đầu để hoạt động "độc lập với việc có ContentReport hay không" (UC100 TDS §3, dòng 70) — tức là **backend đã hỗ trợ sẵn hành động duyệt trực tiếp**. Gap thực sự chỉ nằm ở hai chỗ: (1) không có endpoint liệt kê nội dung `PENDING` chưa từng bị báo cáo, và (2) frontend `moderationApi.ts` chưa từng gọi tới `POST /actions` — chỉ gọi `resolveReport()` (yêu cầu `reportId`).

Vì vậy, phạm vi tài liệu này **chỉ** thêm:
- 1 endpoint mới: `GET /api/v1/admin/moderation/pending-content` (liệt kê `CommunityQuestion`/`CommunityAnswer` có `status = PENDING`, không qua `ContentReport`).
- Frontend wiring để gọi `POST /api/v1/admin/moderation/actions` (UC-100, **không đổi backend UC-100**) từ trang mới.

### 1.1. Vì sao không có UC number chính thức

Đây là phần mở rộng của UC-99 (thêm 1 endpoint đọc mới, cùng bounded context, cùng actor), không phải một use case nghiệp vụ độc lập mới. Đặt trong thư mục riêng `UC99_PendingContentQueue/` (theo đúng cách `CommunityQuestionLike/` đã làm trước đó trong session này) để giữ UC-99's TDS/Test-Spec gốc (đã `Approved`) nguyên vẹn, không sửa file đã approved.

---

## 2. Ma trận Truy vết

| Requirement ID  | Loại          | Mô tả yêu cầu                                                                 | Thành phần Code                                          | ADR liên quan |
| --------------- | ------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------- | ------------- |
| GAP-MOD-001      | Bug/Gap       | Nội dung PENDING chưa từng bị báo cáo không có nơi để moderator duyệt          | `ModerationController.getPendingContentQueue()`             | ADR-005       |
| BR-MOD-004       | Business Rule | Chỉ liệt kê CommunityQuestion/CommunityAnswer có status = PENDING              | `ModerationService.getPendingContentQueue()`                | ADR-005       |
| BR-MOD-005       | Business Rule | targetType bắt buộc, chỉ nhận QUESTION hoặc ANSWER                             | `PendingContentQueueFilter`                                 | ADR-006       |
| BR-RBAC-001      | Business Rule | Chỉ MODERATOR truy cập được (tái dùng rule của UC-99)                          | `@PreAuthorize("hasRole('MODERATOR')")`                      | —             |
| BR-MOD-006       | Business Rule | Duyệt nội dung PENDING gọi thẳng UC-100's moderateContent(), không cần report  | `moderationApi.moderateContentDirect()` (FE)                | ADR-005       |
| SRS-3.2.2.1      | Functional    | Màn hình moderation queue (mở rộng — thêm tab "Nội dung mới")                  | `GET /api/v1/admin/moderation/pending-content`               | —             |

---

## 3. Architecture Decision Records (ADR)

### ADR-005 — Endpoint riêng cho Pending Content, không mở rộng `/queue`

| Field          | Value                      |
| -------------- | -------------------------- |
| **Status**     | `Accepted`                 |
| **Deciders**   | `HuyND — System Architect` |
| **Date**       | `2026-07-03`               |
| **Supersedes** | —                           |

#### Bối cảnh
UC-99's `GET /queue` query trực tiếp bảng `ContentReport` (ADR-001 của UC-99). Nguồn dữ liệu cho "nội dung chờ duyệt lần đầu" hoàn toàn khác: `CommunityQuestion`/`CommunityAnswer` có `status = PENDING`, không liên quan gì tới `ContentReport`. Cần quyết định: thêm param `source=REPORTED|NEW` vào `/queue` hiện tại, hay tạo endpoint mới.

#### Các phương án đã xem xét

| Phương án | Mô tả                                                          | Ưu điểm                          | Nhược điểm                                                                 |
| --------- | ---------------------------------------------------------------- | ---------------------------------- | ----------------------------------------------------------------------------- |
| A         | Thêm `source` param vào `/queue`, union 2 nguồn dữ liệu trong response | 1 endpoint duy nhất               | Vi phạm ADR-001 (Aggregated Report View Pattern chỉ định nghĩa cho ContentReport); response phải có field optional (reportCount/reportReason = null khi source=NEW) — gây hiểu lầm cho moderator |
| B         | Endpoint mới `GET /pending-content`, response DTO riêng          | Rõ ràng, đúng nguồn dữ liệu, không đụng code UC-99 đã Approved | Frontend cần 1 request riêng (chấp nhận được — moderator xem 2 tab khác nhau) |

#### Quyết định
Chọn **Phương án B**. `ContentReport`-based queue (UC-99) và `PENDING`-content queue là hai luồng dữ liệu khác nhau về bản chất; hợp nhất chúng vào 1 response sẽ tạo ra các field "N/A" gây nhầm lẫn (vd. `reportCount = 0` có thể bị đọc nhầm là "0 báo cáo" thay vì "không áp dụng vì không phải report").

#### Hệ quả

**Tích cực:**
- Không sửa bất kỳ dòng code nào đã `Approved` của UC-99.
- DTO trung thực — mỗi field trong response đều có ý nghĩa rõ ràng.

**Tiêu cực / Trade-offs:**
- Frontend hiển thị 2 tab/nguồn riêng thay vì 1 danh sách hợp nhất (chấp nhận được — đây là cách các hệ thống moderation khác, ví dụ Reddit AutoModerator, cũng phân tách "reported" vs "new").

**Compliance Impact:** N/A

---

### ADR-006 — `targetType` bắt buộc, không hỗ trợ query đa bảng cùng lúc

| Field          | Value                      |
| -------------- | -------------------------- |
| **Status**     | `Accepted`                 |
| **Deciders**   | `HuyND — System Architect` |
| **Date**       | `2026-07-03`               |

#### Bối cảnh
`CommunityQuestion` và `CommunityAnswer` là 2 bảng vật lý riêng biệt. Muốn trả về 1 trang kết quả gộp cả 2, sắp xếp theo `createdAt DESC`, cần UNION query hoặc merge-in-memory — phức tạp không cần thiết cho một internal tool có tải thấp.

#### Quyết định
`targetType` là param **bắt buộc**, chỉ nhận `QUESTION` hoặc `ANSWER` (400 nếu thiếu hoặc là `CONTENT`/`ACCOUNT` — dùng lại `ModerationException` factory, mã lỗi mới `MOD-023`). Mỗi request chỉ query 1 bảng — Spring Data derived query đơn giản, có index sẵn trên cột `status` (xem `idx_community_questions_status`, `idx_community_answers_status`).

#### Hệ quả

**Tích cực:** Query đơn giản, tận dụng index có sẵn, không cần thêm bất kỳ migration nào.

**Tiêu cực / Trade-offs:** Frontend cần 1 dropdown/tab chọn `targetType` (đã có pattern tương tự ở `ReportsQueuePage.tsx` cho UC-99's `targetType` filter — tái dùng UI pattern, không tự thiết kế mới).

**Compliance Impact:** N/A

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability
- Cùng SLA với UC-99: p95 < 300ms cho `page=0, size<=20`.
- `size` tối đa 50 (tái dùng `ModerationException.pageSizeExceeded()` — MOD-002).

### 4.2. Data Integrity & Retention
- Read-only endpoint — không ghi dữ liệu. Hành động duyệt (approve) dùng nguyên `moderateContent()` (UC-100) — không đổi transaction boundary hiện có.

### 4.3. Security
- `@PreAuthorize("hasRole('MODERATOR')")` — giống hệt UC-99/UC-100.
- `contentPreview` tái dùng `ContentPreviewService.fetchPreview()` (đã có, đã strip PII — chỉ trả `body`/`title`, không có `authorId`) — không phát sinh rủi ro PII mới.

---

## 5. Static Modeling

### 5.1. Repository methods mới (không cần migration)

```java
// CommunityQuestionRepository — thêm 1 method
Page<CommunityQuestion> findByStatus(QuestionStatus status, Pageable pageable);

// CommunityAnswerRepository — thêm 1 method
Page<CommunityAnswer> findByStatus(AnswerStatus status, Pageable pageable);
```

Cả 2 bảng đều đã có index trên cột `status` (`idx_community_questions_status`, `idx_community_answers_status`) — không cần migration mới.

### 5.2. DTOs mới

```java
// content/dto/request/PendingContentQueueFilter.java
public record PendingContentQueueFilter(
        @NotNull ReportTargetType targetType,   // chỉ QUESTION | ANSWER — validate ở service
        @Min(0) int page,
        @Min(1) @Max(50) int size
) {
    public PendingContentQueueFilter {
        if (size == 0) size = 20;
    }
}

// content/dto/response/PendingContentItemResponse.java
public record PendingContentItemResponse(
        UUID targetId,
        ReportTargetType targetType,
        String contentPreview,
        Instant createdAt
) {}

// content/dto/response/PendingContentQueueResponse.java
public record PendingContentQueueResponse(
        List<PendingContentItemResponse> content,
        long totalElements,
        int page,
        int size
) {}
```

Lưu ý: **không** tái dùng `ModerationQueueItemResponse` (có field `reportCount`/`reportReason`/`id`-là-report-id) — DTO mới chỉ có field thật sự áp dụng được, tránh field "N/A" gây hiểu lầm (đúng theo ADR-005).

---

## 6. Dynamic Modeling

### 6.1. Sequence — Xem Pending Content Queue (Happy Path)

```
Moderator → Web (PendingContentQueuePage) → GET /api/v1/admin/moderation/pending-content?targetType=QUESTION&page=0&size=20
Web → ModerationController.getPendingContentQueue()
Controller → ModerationService.getPendingContentQueue(filter, principal)
Service → CommunityQuestionRepository.findByStatus(PENDING, pageable)
Service → ContentPreviewService.batchFetchPreviews(targetIds, QUESTION)
Service → AuditService.log(MODERATION_QUEUE_VIEWED, ...)   // tái dùng action đã có, không thêm enum mới
Service → Controller: PendingContentQueueResponse
Controller → Web: 200 OK
Moderator → Web: click "Duyệt" trên 1 item
Web → POST /api/v1/admin/moderation/actions {targetId, targetType, actionType: APPROVE}   // UC-100, không đổi
ModerationController.moderateContent() → (không đổi, đã tồn tại)
Web → refetch pending-content queue → item đã duyệt biến mất khỏi danh sách
```

### 6.2. Error Path — targetType không hợp lệ

```
Moderator → Web → GET .../pending-content?targetType=CONTENT
Controller → Service.getPendingContentQueue()
Service: targetType not in {QUESTION, ANSWER} → throw ModerationException.pendingContentTargetTypeUnsupported() (MOD-023)
→ GlobalExceptionHandler → 400 Bad Request
```

---

## 7. Interface Specification

### 7.1. Service Interface (thêm vào `ModerationService`, không đổi method có sẵn)

```java
/**
 * Lists CommunityQuestion or CommunityAnswer rows with status = PENDING, queried directly
 * (not via ContentReport — see ADR-005). Complements getModerationQueue() (UC-99, report-driven).
 *
 * @throws ModerationException (MOD-023) if filter.targetType() is not QUESTION or ANSWER
 */
PendingContentQueueResponse getPendingContentQueue(PendingContentQueueFilter filter, Principal principal);
```

### 7.2. Repository Interface additions

```java
// CommunityQuestionRepository
Page<CommunityQuestion> findByStatus(QuestionStatus status, Pageable pageable);

// CommunityAnswerRepository
Page<CommunityAnswer> findByStatus(AnswerStatus status, Pageable pageable);
```

---

## 8. API Specification

### 8.1. Endpoints Table

| Method | Path                                          | Role       | Rate Limit |
| ------ | ---------------------------------------------- | ---------- | ---------- |
| GET    | `/api/v1/admin/moderation/pending-content`     | MODERATOR  | — (internal tool, cùng UC-99) |

`POST /api/v1/admin/moderation/actions` — **không đổi**, đã tồn tại từ UC-100.

### 8.2. Request / Response Schema

#### `GET /api/v1/admin/moderation/pending-content`

Query params: `targetType` (bắt buộc — `QUESTION` \| `ANSWER`), `page` (default 0), `size` (default 20, max 50).

```json
// 200 OK
{
  "content": [
    {
      "targetId": "b1e7...",
      "targetType": "QUESTION",
      "contentPreview": "Em bị đau bụng dưới ở tuần 20...",
      "createdAt": "2026-07-03T10:00:00Z"
    }
  ],
  "totalElements": 3,
  "page": 0,
  "size": 20
}
```

```json
// 400 — targetType thiếu hoặc không hợp lệ (MOD-023)
{ "code": "MOD-023", "message": "targetType must be QUESTION or ANSWER for pending-content queue" }
```

---

## 9. Bảng mã lỗi

| Code      | HTTP Status | Message (EN)                                                    |
| --------- | ----------- | ------------------------------------------------------------------ |
| `MOD-002` | 400         | (tái dùng) page size exceeds maximum of 50                        |
| `MOD-023` | 400         | targetType must be QUESTION or ANSWER for pending-content queue   |

---

## 10. Frontend Changes

**Không đổi bất kỳ file backend nào của UC-100.** Chỉ thêm wiring còn thiếu ở frontend:

| File                                                                 | Thay đổi                                                                                  |
| ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `src/features/moderation/api/moderationApi.ts`                         | + `fetchPendingContentQueue(targetType, page, size)`; + `moderateContentDirect(targetId, targetType, actionType, reason)` (gọi `POST /actions`, chưa từng có wrapper) |
| `src/features/moderation/models/moderation.ts` (hoặc file types tương ứng) | + `PendingContentItem`, `PendingContentQueueResponse` types                                |
| `src/features/moderation/pages/PendingContentQueuePage.tsx` (mới)      | Trang mới, tái dùng layout/style của `ReportsQueuePage.tsx` (đã test, đang hoạt động) — KHÔNG dựa trên `EscalatedModerationCasesPage.tsx` (có TODO `console.log` chưa wire) |
| Router config (nơi khai báo route `/moderator/...`)                    | + route mới `/moderator/pending-content`                                                    |
| `src/features/moderation/components/ModPortalSidebar.tsx`              | + link điều hướng tới trang mới — nếu không có link, trang không thể truy cập được (chính là bug đang sửa) |

**Ngoài phạm vi (không đụng trong lần này — liệt kê để user biết, không tự ý sửa):**
- `EscalatedModerationCasesPage.tsx`'s stub `console.log` resolve action.
- Route `/moderator/queue/:reportId` hiện đang mồ côi theo báo cáo của subagent.
- Response envelope mismatch (`res.data?.data?.content`) trên trang Escalated.

---

## 11. Quy trình Triển khai

### 11.1. Backend
1. `CommunityQuestionRepository` — thêm `findByStatus`.
2. `CommunityAnswerRepository` — thêm `findByStatus`.
3. `PendingContentQueueFilter`, `PendingContentItemResponse`, `PendingContentQueueResponse` — DTO mới.
4. `ModerationException.pendingContentTargetTypeUnsupported()` — MOD-023.
5. `ModerationMapper` — `toPendingContentItemResponse()`.
6. `ModerationService`/`ModerationServiceImpl` — `getPendingContentQueue()`.
7. `ModerationController` — `GET /pending-content`.
8. **Không có migration** — index đã tồn tại, audit action tái dùng `MODERATION_QUEUE_VIEWED` đã có.

### 11.2. Frontend
1. `moderationApi.ts` — 2 hàm mới.
2. Types mới.
3. `PendingContentQueuePage.tsx` — trang mới.
4. Route + sidebar link.

### 11.3. Verification bắt buộc (theo bài học của session này)
Unit test xanh **không đủ** — phải verify qua UI thật: đăng nhập `moderator@carebridge.dev`, xác nhận câu hỏi PENDING xuất hiện ở trang mới, bấm Duyệt, xác nhận nó biến mất khỏi pending-content queue và xuất hiện trên community feed (đăng nhập `mother@carebridge.dev`).

---

## 12. Rollback & Incident Runbook

Không có migration → rollback chỉ cần revert code deploy (redeploy version cũ), không có bước DB nào cần đảo ngược.

---

## 13. Phương pháp Xác minh

- `./mvnw test -Dtest=ModerationServiceImplTest,ModerationControllerTest`
- Chrome DevTools MCP: đăng nhập moderator, thao tác thật trên UI (bắt buộc theo §11.3).

---

## 14. Authorization Matrix

| Endpoint                                    | MODERATOR | SYSTEM_ADMIN | Khác |
| --------------------------------------------- | :-------: | :-----------: | :---: |
| `GET /pending-content`                        | ✅        | ❌ (trừ khi cũng có role MODERATOR) | ❌ |

(Giống hệt UC-99 — không có role mới nào được thêm.)

---

## 15. AI Prompt Constraints (CASE 2.0)

- C1: RBAC — MODERATOR only, enforce tại controller (`@PreAuthorize`).
- C2: Không lộ PII trong `contentPreview` — tái dùng `ContentPreviewService` đã strip PII.
- C3: Không sửa code UC-99/UC-100 đã `Approved`/`Implemented` — chỉ thêm mới.
- C4: `targetType` phải validate nghiêm ngặt (chỉ QUESTION/ANSWER) — reject CONTENT/ACCOUNT với MOD-023.
- C5: Sort mặc định `createdAt DESC` (nhất quán với UC-99's BR-MOD-003).
- C6: `size` tối đa 50 (tái dùng MOD-002).
