# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification — UC-171 Follow Topic

| Field              | Value                                       |
| ------------------ | ------------------------------------------- |
| **Document ID**    | `CB-COMMUNITY-IMP-007`                      |
| **Version**        | `1.0`                                       |
| **Date**           | `2026-07-01`                                |
| **Status**         | `Approved`                                  |
| **Document Owner** | `HuyND`                                     |
| **Author**         | `AI Agent — Winston (System Architect)`     |
| **Reviewed by**    | `[x] HuyND — Approved`                      |
| **DPO Sign-off**   | `[ ] Pending`                               |
| **Approved by**    | `HuyND`                                     |
| **Last Review**    | `2026-07-01`                                |
| **Based on EDS**   | `v2.0`                                      |

---

## CHANGELOG

| Ngày       | Người thực hiện                       | Nội dung thay đổi                             |
| ---------- | ------------------------------------- | --------------------------------------------- |
| 2026-07-01 | AI Agent — Winston (System Architect) | Tạo tài liệu lần đầu cho UC-171 Follow Topic |
| 2026-07-01 | AI Agent — Amelia (Dev Agent) | Implemented: migration `V20260701000001__create_topic_follows.sql`, `UserTopicFollow` entity, `UserTopicFollowRepository`, `TopicFollowResponse`, `TopicHiddenException` (COM-014), `TopicFollowServiceImpl`, `POST /api/v1/community/topics/{id}/follow`, mobile `toggleFollowTopic()` wired into `topic_directory_screen.dart` and `community_topic_search_screen.dart` (replacing local-only fake state) with optimistic update + rollback. Reused existing `CommunityTopicNotFoundException` (COM-003) for not-found rather than creating a new one. Tests passing (`TopicFollowServiceImplTest`, `CommunityTopicControllerTest`). Status=Approved. |
| 2026-07-03 | AI Agent — Claude (Audit Pass) | **Correction to the 2026-07-01 entry**: the toggle endpoint was wired, but `GET /api/v1/community/topics` never returned the viewer's follow state, so both mobile screens' "local-only fake state" (`_followedTopics` Set) was in fact never replaced — every topic rendered "not followed" on screen open regardless of real DB state, only correct after an in-session toggle. Added `isFollowed` to `CommunityTopicResponse` (batch-hydrated via new `UserTopicFollowRepository.findFollowedTopicIds()` in `CommunityTopicServiceImpl.getTopics()`/`searchTopics()`), and updated the Flutter `CommunityTopic` model + both screens to read `topic.isFollowed` directly instead of the local Set. All existing + new tests GREEN. |

---

## MỤC LỤC

1. [Tổng quan Module](#1-tổng-quan-module)
2. [Ma trận Truy vết (Traceability Matrix)](#2-ma-trận-truy-vết-traceability-matrix)
3. [Architecture Decision Records (ADR)](#3-architecture-decision-records-adr)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling (Mô hình Tĩnh)](#5-static-modeling-mô-hình-tĩnh)
6. [Dynamic Modeling (Mô hình Động)](#6-dynamic-modeling-mô-hình-động)
7. [Domain Event Catalog](#7-domain-event-catalog)
8. [Interface Specification (Đặc tả Giao diện)](#8-interface-specification-đặc-tả-giao-diện)
9. [API Specification](#9-api-specification)
10. [Bảng mã lỗi (Error Codes)](#10-bảng-mã-lỗi-error-codes)
11. [Quy trình Triển khai (Step-by-Step)](#11-quy-trình-triển-khai-step-by-step)
12. [Rollback & Incident Runbook](#12-rollback--incident-runbook)
13. [Kịch bản Kiểm thử Chi tiết](#13-kịch-bản-kiểm-thử-chi-tiết)
14. [Phương pháp Xác minh](#14-phương-pháp-xác-minh)
15. [Mẫu thử thực tế (API Verification Samples)](#15-mẫu-thử-thực-tế-api-verification-samples)
16. [Bảng tổng hợp phân quyền (Authorization Matrix)](#16-bảng-tổng-hợp-phân-quyền-authorization-matrix)
17. [AI Prompt Constraints (CASE 2.0)](#17-ai-prompt-constraints-case-20)

---

## 1. Tổng quan Module

| Field                     | Value                                                                         |
| ------------------------- | ----------------------------------------------------------------------------- |
| **Module Name**           | `FollowTopic`                                                                 |
| **Bounded Context**       | `community`                                                                   |
| **UC ID**                 | `UC-171`                                                                      |
| **SRS Reference**         | `3.3.8.4`                                                                     |
| **Primary Actor**         | `Authenticated User (any role)`                                               |
| **Secondary Actor**       | `Firebase Cloud Messaging (FCM) — future / out of MVP scope`                 |
| **Platform**              | `Mobile App (Flutter) + Backend (Spring Boot)`                               |
| **Data Classification**   | `Internal`                                                                    |
| **Compliance Scope**      | `BR-RBAC`                                                                     |
| **Upstream Dependencies** | `security (JWT), community (CommunityTopic entity, UC-109)`                  |
| **Downstream Consumers**  | `notification (FCM — future), community feed personalization (future)`        |

**Mô tả:** Cho phép người dùng theo dõi (follow) hoặc hủy theo dõi (unfollow) một community topic để cá nhân hóa feed và nhận thông báo về câu hỏi mới trong topic đó. Hành vi là **idempotent toggle**: gọi lần 1 → follow; gọi lần 2 → unfollow. API trả về trạng thái hiện tại sau toggle.

**Current state:** 
- Table `user_topic_follows` chưa tồn tại trong V1__init_schema.sql.
- Endpoint `POST /api/v1/community/topics/{id}/follow` chưa tồn tại.
- `CommunityTopicController` cần thêm endpoint mới.

**FCM notification scope:** FCM wiring cho "new question in followed topic" notification là **out of scope** cho spec này. Notification sẽ được thiết kế trong UC-notification riêng. Spec này chỉ bao gồm persistence của follow relationship.

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại          | Mô tả yêu cầu                                              | Thành phần Code                                    | Compliance Target | ADR liên quan |
| -------------- | ------------- | ---------------------------------------------------------- | -------------------------------------------------- | ----------------- | ------------- |
| UC-171         | Use Case      | User follow/unfollow topic để cá nhân hóa feed            | `CommunityTopicController.toggleFollow()`          | BR-RBAC           | ADR-COM-171-1 |
| BR-RBAC        | Business Rule | Chỉ authenticated user mới được follow topic              | JWT required on endpoint                           | RBAC              | ADR-COM-171-1 |
| BR-COM-171-1   | Business Rule | Toggle: follow nếu chưa follow; unfollow nếu đã follow    | `TopicFollowService.toggle()` — upsert logic      | Idempotency       | ADR-COM-171-2 |
| BR-COM-171-2   | Business Rule | Không thể follow topic bị hidden                           | `TopicFollowService.toggle()` topic status check   | Data integrity    | ADR-COM-171-1 |
| BR-COM-171-3   | Business Rule | Mỗi user chỉ có 1 follow record cho mỗi topic             | `UNIQUE(user_id, topic_id)` DB constraint          | Data integrity    | ADR-COM-171-2 |

---

## 3. Architecture Decision Records (ADR)

### ADR-COM-171-1 — Single toggle endpoint thay vì POST/DELETE riêng

| Field      | Value        |
| ---------- | ------------ |
| **Status** | `Accepted`   |
| **Date**   | `2026-07-01` |

#### Bối cảnh (Context)
Bookmark (UC) và answer-like đều dùng toggle pattern. UC-171 Follow Topic có cùng semantic. Cần quyết định: 1 endpoint toggle vs 2 endpoint (POST follow + DELETE unfollow).

#### Các phương án đã xem xét (Options Considered)

| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A | Toggle: `POST /topics/{id}/follow` | Consistent với bookmark pattern; đơn giản | HTTP semantics không chuẩn (POST idempotent?) |
| B | `POST /topics/{id}/follow` + `DELETE /topics/{id}/follow` | RESTful | Mobile cần 2 endpoints, phức tạp hơn |

#### Quyết định (Decision)
**Phương án A — Toggle POST** vì nhất quán với `CommunityBookmarkController` và `CommunityAnswerLikeController` đã có trong codebase. Response trả về `{"followed": true/false}`.

---

### ADR-COM-171-2 — Idempotent upsert với UNIQUE constraint

| Field      | Value        |
| ---------- | ------------ |
| **Status** | `Accepted`   |
| **Date**   | `2026-07-01` |

#### Quyết định (Decision)
DB có `UNIQUE(user_id, topic_id)`. Service kiểm tra existence:
- Nếu record tồn tại → DELETE (unfollow) → return `{"followed": false}`
- Nếu không tồn tại → INSERT (follow) → return `{"followed": true}`

Không dùng `INSERT ... ON CONFLICT` vì cần biết trạng thái mới để trả về response.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement            | Target SLA |
| -------- | ---------------------- | ---------- |
| Latency  | Toggle API (p99)       | `< 150ms`  |
| Count    | Max follows per user   | Open (no cap in MVP) |

### 4.2. Security

| Category      | Requirement               | Verification               |
| ------------- | ------------------------- | -------------------------- |
| Authorization | Authentication required   | 401 test case              |
| Data isolation| User can only toggle own follows | Service-level check  |

---

## 5. Static Modeling (Mô hình Tĩnh)

### 5.1. Planned File Paths

| File | Change Type | Notes |
|------|-------------|-------|
| `db/migration/V20260701000001__create_topic_follows.sql` | Create | New table `user_topic_follows` |
| `community/entity/UserTopicFollow.java` | Create | JPA entity for follow record |
| `community/repository/UserTopicFollowRepository.java` | Create | JPA repository |
| `community/service/TopicFollowService.java` | Create | Interface |
| `community/service/TopicFollowServiceImpl.java` | Create | Implementation |
| `community/dto/response/TopicFollowResponse.java` | Create | `{followed: boolean, topicId: UUID}` |
| `community/controller/CommunityTopicController.java` | Modify | Add `POST /{id}/follow` endpoint |

### 5.2. Class Diagram (PlantUML)

```plantuml
@startuml UC171_FollowTopic_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA

class UserTopicFollow {
  + id: UUID
  + userId: UUID
  + topicId: UUID
  + createdAt: Instant
}

class TopicFollowResponse {
  + topicId: UUID
  + followed: boolean
}

interface TopicFollowService <<interface>> {
  + toggleFollow(topicId: UUID, userId: UUID): TopicFollowResponse
}

class TopicFollowServiceImpl implements TopicFollowService {
  - followRepository: UserTopicFollowRepository
  - topicRepository: CommunityTopicRepository
  + toggleFollow(topicId: UUID, userId: UUID): TopicFollowResponse
}

interface UserTopicFollowRepository <<interface>> {
  + findByUserIdAndTopicId(userId: UUID, topicId: UUID): Optional<UserTopicFollow>
  + delete(entity: UserTopicFollow): void
  + save(entity: UserTopicFollow): UserTopicFollow
}

class CommunityTopicController {
  - followService: TopicFollowService
  + toggleFollow(id: UUID, principal: UserPrincipal): ResponseEntity<TopicFollowResponse>
}

CommunityTopicController --> TopicFollowService : uses
TopicFollowServiceImpl --> UserTopicFollowRepository : uses
TopicFollowServiceImpl --> CommunityTopicRepository : checks topic hidden

@enduml
```

### 5.3. Schema / Migration Delta

**Migration required:** `V20260701000001__create_topic_follows.sql`

```sql
-- UC-171: User follows community topics for personalized feed and notifications
CREATE TABLE user_topic_follows (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    topic_id   UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_follow_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)     ON DELETE CASCADE,
    CONSTRAINT fk_follow_topic FOREIGN KEY (topic_id) REFERENCES community_topics(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_topic_follow UNIQUE (user_id, topic_id)
);

CREATE INDEX idx_user_topic_follows_user  ON user_topic_follows(user_id);
CREATE INDEX idx_user_topic_follows_topic ON user_topic_follows(topic_id);
```

---

## 6. Dynamic Modeling (Mô hình Động)

### 6.1. Sequence Diagram — Toggle Follow

```plantuml
@startuml UC171_SequenceDiagram_ToggleFollow
skinparam sequenceArrowThickness 2
skinparam backgroundColor #FAFAFA

actor User
participant "CommunityTopicController" as Ctrl
participant "TopicFollowService" as Svc
participant "UserTopicFollowRepository" as Repo
participant "CommunityTopicRepository" as TopicRepo
database "PostgreSQL" as DB

User -> Ctrl: POST /api/v1/community/topics/{id}/follow\nAuthorization: Bearer <jwt>
Ctrl -> Svc: toggleFollow(topicId, userId)
Svc -> TopicRepo: findById(topicId)
TopicRepo -> DB: SELECT * FROM community_topics WHERE id=?
DB --> TopicRepo: CommunityTopic

alt topic not found
  Svc --> Ctrl: throw TopicNotFoundException
  Ctrl --> User: 404 Not Found
end

alt topic.isHidden == true
  Svc --> Ctrl: throw TopicHiddenException
  Ctrl --> User: 409 Conflict
end

Svc -> Repo: findByUserIdAndTopicId(userId, topicId)
Repo -> DB: SELECT * FROM user_topic_follows WHERE user_id=? AND topic_id=?
DB --> Repo: Optional<UserTopicFollow>

alt follow record exists (unfollow)
  Svc -> Repo: delete(existingFollow)
  Repo -> DB: DELETE FROM user_topic_follows WHERE id=?
  Svc --> Ctrl: TopicFollowResponse{followed=false}
else follow record not exists (follow)
  Svc -> Repo: save(new UserTopicFollow(userId, topicId))
  Repo -> DB: INSERT INTO user_topic_follows ...
  Svc --> Ctrl: TopicFollowResponse{followed=true}
end

Ctrl --> User: 200 OK {followed: true/false, topicId: "..."}

@enduml
```

---

## 7. Domain Event Catalog

| Event | Publisher | Consumer | Payload | Trigger |
|-------|-----------|----------|---------|---------|
| `TopicFollowed` | `TopicFollowServiceImpl` | FCM notification service (future) | `{userId, topicId, followedAt}` | User follows topic |
| `TopicUnfollowed` | `TopicFollowServiceImpl` | FCM notification service (future) | `{userId, topicId}` | User unfollows topic |

> **MVP note:** Event publishing and FCM notification wiring are out of scope for this sprint.

---

## 8. Interface Specification (Đặc tả Giao diện)

```java
package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.response.TopicFollowResponse;
import java.util.UUID;

public interface TopicFollowService {
    /**
     * Toggle follow on a community topic.
     * @return TopicFollowResponse with current followed state after toggle
     * @throws TopicNotFoundException if topic does not exist
     * @throws TopicHiddenException if topic is hidden (is_hidden=true)
     */
    TopicFollowResponse toggleFollow(UUID topicId, UUID userId);
}
```

---

## 9. API Specification

### POST /api/v1/community/topics/{id}/follow

| Field           | Value                                             |
| --------------- | ------------------------------------------------- |
| **Method**      | `POST`                                            |
| **Path**        | `/api/v1/community/topics/{id}/follow`            |
| **Auth**        | `Bearer JWT` (required)                           |
| **Roles**       | Any authenticated user                            |
| **Idempotency** | Yes — double call toggles back to unfollowed state |

**Path Parameters:**

| Parameter | Type   | Required | Description    |
| --------- | ------ | -------- | -------------- |
| `id`      | `UUID` | Yes      | ID of the topic |

**Request Body:** None

**Success Response:** `200 OK`

```json
{
  "topicId": "a1b2c3d4-e5f6-7890-abcd-ef1234567801",
  "followed": true
}
```

**Error Responses:**

| HTTP Status | Error Code         | Condition                          |
| ----------- | ------------------ | ---------------------------------- |
| `401`       | `AUTH_REQUIRED`    | No JWT token                       |
| `404`       | `TOPIC_NOT_FOUND`  | Topic ID does not exist            |
| `409`       | `TOPIC_HIDDEN`     | Topic is hidden (is_hidden = true) |

---

## 10. Bảng mã lỗi (Error Codes)

| Error Code       | HTTP Status | Message (EN)                    | Condition            |
| ---------------- | ----------- | ------------------------------- | -------------------- |
| `TOPIC_NOT_FOUND` | `404`      | Community topic not found       | No row with given UUID |
| `TOPIC_HIDDEN`   | `409`       | Cannot follow a hidden topic    | is_hidden = true     |

---

## 11. Quy trình Triển khai (Step-by-Step)

1. **Migration:** Create and apply `V20260701000001__create_topic_follows.sql`
2. **Entity:** Create `UserTopicFollow.java` JPA entity
3. **Repository:** Create `UserTopicFollowRepository.java` (JpaRepository)
4. **DTO:** Create `TopicFollowResponse.java` with `topicId` and `followed` fields
5. **Service interface:** Create `TopicFollowService.java`
6. **Service impl:** Create `TopicFollowServiceImpl.java` with toggle logic
7. **Controller:** Add `@PostMapping("/{id}/follow")` to `CommunityTopicController`
8. **Security config:** Verify endpoint is covered by existing authenticated rule
9. **Tests:** Write unit + integration tests as per Test-Spec
10. **Mobile:** Add `followTopic(id)` to `community_service.dart`

---

## 12. Rollback & Incident Runbook

### Rollback Procedure

```sql
-- Drop topic follows table (only safe if no follow data exists)
DROP TABLE IF EXISTS user_topic_follows;
```

### Incident Triggers
- Toggle returns wrong state → verify upsert logic in service
- 409 on public topic → verify `isHidden` check logic

---

## 13. Kịch bản Kiểm thử Chi tiết

> Detailed test cases are in `UC171_FollowTopic_Test-Spec.md`.

| TC ID    | Scenario                                   | Expected Result                     |
| -------- | ------------------------------------------ | ----------------------------------- |
| TC-171-1 | Follow a visible topic (first time)        | 200, `{"followed": true}`           |
| TC-171-2 | Unfollow by calling same endpoint again    | 200, `{"followed": false}`          |
| TC-171-3 | Follow non-existent topic                  | 404 Not Found                       |
| TC-171-4 | Follow a hidden topic                      | 409 Conflict                        |
| TC-171-5 | Unauthenticated request                    | 401 Unauthorized                    |
| TC-171-6 | Double follow — idempotency               | 200, second call returns `false`    |
| TC-171-7 | Two different users follow same topic      | Both succeed independently          |

---

## 14. Phương pháp Xác minh

| Verification Method   | Tool                      | Target                        |
| --------------------- | ------------------------- | ----------------------------- |
| Unit test (service)   | JUnit 5 + Mockito         | Toggle logic, hidden check    |
| Controller test       | @WebMvcTest + MockMvc     | HTTP 200/401/404/409 responses |
| Integration test      | Testcontainers + @SpringBootTest | Migration, toggle persistence |

---

## 15. Mẫu thử thực tế (API Verification Samples)

```bash
# Follow a topic
curl -X POST http://localhost:8080/api/v1/community/topics/{id}/follow \
  -H "Authorization: Bearer $MOTHER_TOKEN"
# Expected: 200 {"topicId":"...","followed":true}

# Unfollow same topic (second call)
curl -X POST http://localhost:8080/api/v1/community/topics/{id}/follow \
  -H "Authorization: Bearer $MOTHER_TOKEN"
# Expected: 200 {"topicId":"...","followed":false}
```

---

## 16. Bảng tổng hợp phân quyền (Authorization Matrix)

| Role            | Toggle follow visible topic | Toggle follow hidden topic | Notes               |
| --------------- | --------------------------- | -------------------------- | ------------------- |
| `MOTHER`        | ✅ Yes                      | ❌ 409                     |                     |
| `EXPERT`        | ✅ Yes                      | ❌ 409                     |                     |
| `PARTNER`       | ✅ Yes                      | ❌ 409                     |                     |
| `FAMILY`        | ✅ Yes                      | ❌ 409                     |                     |
| `MODERATOR`     | ✅ Yes                      | ✅ Yes (can see hidden)    | Open: decide if mods can follow hidden topics |
| Unauthenticated | ❌ 401                      | ❌ 401                     |                     |

> **Open:** Whether MODERATOR can follow hidden topics is not specified in UC-171. Default to allowing it for MVP.

---

## 17. AI Prompt Constraints (CASE 2.0)

### Constraint Summary Table

| ID   | Constraint                                        | Source        |
| ---- | ------------------------------------------------- | ------------- |
| C1   | Never auto-approve this TDS                       | EDS v2.0 §1   |
| C2   | Toggle semantics: 1st call = follow, 2nd = unfollow | ADR-COM-171-2 |
| C3   | Hidden topic must return 409, not silently ignore  | BR-COM-171-2  |
| C4   | UNIQUE constraint must be in migration            | ADR-COM-171-2 |
| C5   | FCM notification is explicitly out of scope       | ADR-COM-171-1 |

### Anti-Pattern Detection

| AP-ID | Anti-Pattern                         | Signal                              | Check | Gate  |
| ----- | ------------------------------------ | ----------------------------------- | ----- | ----- |
| AP-1  | Non-idempotent: error on double-call | `409 Conflict` on existing follow   | [ ]   | CG-2  |
| AP-2  | Missing hidden topic guard           | No check on `isHidden` before save  | [ ]   | CG-1  |
| AP-3  | Migration skipped                    | No `user_topic_follows` table file  | [ ]   | CG-4  |
| AP-4  | FCM wiring in this feature           | FCM call inside `toggleFollow()`    | [ ]   | C5    |
