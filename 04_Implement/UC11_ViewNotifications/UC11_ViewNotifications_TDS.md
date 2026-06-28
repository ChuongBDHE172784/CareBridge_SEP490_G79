# Technical Design Specification — UC-11: View Notifications

| Field            | Value                          |
|------------------|--------------------------------|
| Document ID      | CB-NOTIF-IMP-011               |
| Version          | 1.0                            |
| Date             | 2026-06-26                     |
| Status           | Approved                          |
| Document Owner   | PhuongNT                       |
| Author           | AI Agent                       |
| Based on EDS     | v2.0                           |
| Related UC       | UC-11 ViewNotifications        |
| Package          | com.carebridge.backend.notification |

---

## 1. Tổng quan Module

### 1.1 Mục đích

UC-11 cho phép người dùng đã xác thực xem danh sách thông báo nội ứng dụng (in-app notifications) của chính họ theo phân trang. Tính năng này hỗ trợ trên cả nền tảng Mobile (Flutter) và Web (React).

### 1.2 Phạm vi

| Hạng mục            | Nằm trong phạm vi                             | Ngoài phạm vi                         |
|---------------------|-----------------------------------------------|---------------------------------------|
| API                 | GET /api/v1/notifications (phân trang, lọc)   | Push notification gửi đi              |
| Phân quyền          | ROLE_MOTHER, ROLE_EXPERT, ROLE_ADMIN          | Anonymous access                      |
| Bộ lọc              | ALL, UNREAD, READ                             | Tìm kiếm full-text                    |
| Phân trang          | Offset pagination (page, size)                | Cursor-based pagination               |
| Platform            | Mobile + Web                                  | SMS, Email notifications              |

### 1.3 Actors

- **Primary**: Authenticated User (ROLE_MOTHER, ROLE_EXPERT, ROLE_ADMIN)
- **System**: JWT Security Context, PostgreSQL Database

### 1.4 Preconditions

1. Người dùng đã đăng nhập và có JWT hợp lệ trong header `Authorization: Bearer <token>`.
2. Bảng `notifications` đã được tạo bởi Flyway migration V2.
3. `userId` trong JWT phải tương ứng với một bản ghi trong bảng `users`.

### 1.5 Postconditions

- Trả về danh sách thông báo thuộc về `userId` hiện tại, phân trang theo `page` và `size`.
- Không có side effect (read-only operation).
- Audit log được ghi nhận qua `AuditService.emit()`.

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Mô tả yêu cầu                                              | Artifact ánh xạ                          |
|----------------|-------------------------------------------------------------|------------------------------------------|
| SRS-11-FR-01   | Người dùng xem danh sách thông báo của mình                | NotificationController.getNotifications() |
| SRS-11-FR-02   | Phân trang theo page, size                                  | PaginatedResponse<NotificationResponse>  |
| SRS-11-FR-03   | Lọc theo trạng thái: ALL, UNREAD, READ                     | NotificationStatus enum, repository query|
| SRS-11-FR-04   | Ownership: chỉ xem thông báo của chính mình                | SecurityUtils.requireCurrentUserId()     |
| SRS-11-NFR-01  | API phản hồi ≤ 500ms (P95)                                 | Index idx_notifications_user_id          |
| SRS-11-NFR-02  | Xác thực JWT bắt buộc                                      | Spring Security filter chain             |
| SRS-11-SEC-01  | Không trả về notification của user khác                    | Service-level ownership enforcement      |
| ERR-NOTIF-010  | 401 khi không có JWT                                        | Spring Security 401 handler              |
| ERR-NOTIF-011  | 400 khi page params không hợp lệ                           | @Valid + ConstraintViolationException    |

---

## 3. Architecture Decision Records (ADR)

### ADR-011-001: Bảng `notifications` mới thay vì lưu trong các bảng hiện có

| Trường      | Nội dung                                                                                          |
|-------------|---------------------------------------------------------------------------------------------------|
| **Status**  | Accepted                                                                                          |
| **Context** | Cần lưu trữ thông báo đa loại (REMINDER, COMMUNITY, CONSULTATION, EMERGENCY, SAFETY) cho nhiều role. |
| **Decision**| Tạo bảng `notifications` riêng biệt với cột `category` VARCHAR và `metadata` JSONB.              |
| **Rationale**| Tách biệt concern, dễ scale, JSONB linh hoạt cho metadata từng loại thông báo.                   |
| **Consequences**| Cần Flyway migration V2. Không ảnh hưởng các bảng hiện có.                                   |

### ADR-011-002: Offset Pagination thay vì Cursor Pagination

| Trường      | Nội dung                                                                                          |
|-------------|---------------------------------------------------------------------------------------------------|
| **Status**  | Accepted                                                                                          |
| **Context** | Cần pagination cho danh sách notification. Hai lựa chọn: offset hoặc cursor.                     |
| **Decision**| Sử dụng offset pagination (page, size, total).                                                    |
| **Rationale**| Volume < 1M rows/user. Offset đơn giản hơn, tương thích với Spring Data Pageable, frontend dễ implement số trang. |
| **Consequences**| Hiệu năng giảm khi user có rất nhiều notification (> 100k), nhưng chấp nhận được ở scale hiện tại. |

### ADR-011-003: Soft Filter bằng cột `is_read` thay vì bảng riêng

| Trường      | Nội dung                                                                                          |
|-------------|---------------------------------------------------------------------------------------------------|
| **Status**  | Accepted                                                                                          |
| **Context** | Cần phân biệt notification đã đọc / chưa đọc.                                                    |
| **Decision**| Dùng cột `is_read BOOLEAN` và partial index `WHERE is_read = false` cho query chưa đọc.          |
| **Rationale**| Tránh JOIN phức tạp, partial index đảm bảo performance cho query UNREAD phổ biến nhất.           |
| **Consequences**| Cần cập nhật `is_read` và `read_at` khi UC-12 MarkAsRead được thực hiện.                    |

---

## 4. Non-Functional Requirements & SLA

| NFR              | Mục tiêu                        | Cơ chế đảm bảo                                   |
|------------------|---------------------------------|---------------------------------------------------|
| Latency          | P95 ≤ 500ms                     | Index `idx_notifications_user_id`, default size=20 |
| Throughput       | 200 RPS sustained               | Connection pool HikariCP                          |
| Availability     | 99.5% uptime                    | Spring Boot health check, Docker restart policy   |
| Security         | JWT required, ownership enforced| Spring Security, service-level userId check       |
| Data Integrity   | Read-only, no mutation          | GET method only, @Transactional(readOnly=true)    |
| Observability    | Audit log per request           | AuditService.emit() in service layer              |
| Pagination Limit | Max size=100 per page           | @Max(100) validation on size param                |

---

## 5. Static Modeling (Mô hình Tĩnh)

### 5.1 Class Diagram

```plantuml
@startuml UC11_StaticModel
skinparam classAttributeIconSize 0
skinparam packageStyle rectangle

package "com.carebridge.backend.notification" {

    package "entity" {
        class Notification {
            - notificationId: UUID
            - userId: UUID
            - title: String
            - body: String
            - category: NotificationCategory
            - isRead: boolean
            - readAt: OffsetDateTime
            - metadata: Map<String, Object>
            - createdAt: OffsetDateTime
            - updatedAt: OffsetDateTime
        }

        enum NotificationCategory {
            REMINDER
            COMMUNITY
            CONSULTATION
            EMERGENCY
            SAFETY
        }

        enum NotificationStatus {
            ALL
            UNREAD
            READ
        }

        Notification --> NotificationCategory
    }

    package "dto" {
        class NotificationResponse {
            + notificationId: UUID
            + title: String
            + body: String
            + category: String
            + isRead: boolean
            + readAt: OffsetDateTime
            + metadata: Map<String, Object>
            + createdAt: OffsetDateTime
        }

        class NotificationListRequest {
            + page: int = 0
            + size: int = 20
            + status: NotificationStatus = ALL
        }
    }

    package "repository" {
        interface INotificationRepository {
            + findByUserId(userId: UUID, pageable: Pageable): Page<Notification>
            + findByUserIdAndIsRead(userId: UUID, isRead: boolean, pageable: Pageable): Page<Notification>
        }
    }

    package "service" {
        interface INotificationService {
            + getNotifications(userId: UUID, request: NotificationListRequest): PaginatedResponse<NotificationResponse>
        }

        class NotificationServiceImpl {
            - notificationRepository: INotificationRepository
            - auditService: AuditService
            + getNotifications(userId: UUID, request: NotificationListRequest): PaginatedResponse<NotificationResponse>
            - toResponse(notification: Notification): NotificationResponse
        }

        NotificationServiceImpl ..|> INotificationService
        NotificationServiceImpl --> INotificationRepository
    }

    package "mapper" {
        class NotificationMapper {
            + toResponse(notification: Notification): NotificationResponse
            + toResponseList(notifications: List<Notification>): List<NotificationResponse>
        }
    }

    package "controller" {
        class NotificationController {
            - notificationService: INotificationService
            + getNotifications(principal: Principal, request: NotificationListRequest): ResponseEntity<ApiResponse<PaginatedResponse<NotificationResponse>>>
        }

        NotificationController --> INotificationService
    }

    INotificationRepository --> Notification
    NotificationServiceImpl --> NotificationMapper
    NotificationMapper --> NotificationResponse
}

package "com.carebridge.backend.common" {
    class ApiResponse<T> {
        + success: boolean
        + message: String
        + data: T
        + timestamp: OffsetDateTime
    }

    class PaginatedResponse<T> {
        + content: List<T>
        + page: int
        + size: int
        + totalElements: long
        + totalPages: int
        + unreadCount: long
    }

    class SecurityUtils {
        + {static} requireCurrentUserId(principal: Principal): UUID
    }

    class AuditService {
        + emit(event: AuditEvent): void
    }
}

NotificationController ..> ApiResponse
NotificationController ..> PaginatedResponse
NotificationServiceImpl ..> SecurityUtils
NotificationServiceImpl ..> AuditService

@enduml
```

### 5.2 Database Schema

```sql
-- Bảng notifications (từ Flyway V2)
notifications (
    notification_id UUID PK,
    user_id         UUID FK → users(user_id),
    title           VARCHAR(255) NOT NULL,
    body            TEXT NOT NULL,
    category        VARCHAR(50) NOT NULL,  -- REMINDER|COMMUNITY|CONSULTATION|EMERGENCY|SAFETY
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
)
INDEX: idx_notifications_user_id ON (user_id)
INDEX: idx_notifications_user_unread ON (user_id, is_read) WHERE is_read = false
```

---

## 6. Dynamic Modeling (Mô hình Động)

### 6.1 Sequence Diagram — Happy Path GET /api/v1/notifications

```plantuml
@startuml UC11_SequenceDiagram
skinparam sequenceMessageAlign center
skinparam responseMessageBelowArrow true

actor       "User\n(Mobile/Web)"    as User
participant "Spring Security\nFilter"  as Security
participant "NotificationController"   as Controller
participant "SecurityUtils"            as SecUtils
participant "NotificationServiceImpl"  as Service
participant "INotificationRepository"  as Repo
participant "NotificationMapper"       as Mapper
participant "AuditService"             as Audit
database    "PostgreSQL"               as DB

User -> Security : GET /api/v1/notifications\n?page=0&size=20&status=UNREAD\nAuthorization: Bearer <jwt>

Security -> Security : validateJWT(token)
Security -> Controller : forward(request, principal)

Controller -> SecUtils : requireCurrentUserId(principal)
SecUtils --> Controller : userId: UUID

Controller -> Service : getNotifications(userId, request{page=0, size=20, status=UNREAD})

Service -> Service : validate(page ≥ 0, 1 ≤ size ≤ 100)

alt status == UNREAD
    Service -> Repo : findByUserIdAndIsRead(userId, false, Pageable.of(0, 20))
else status == READ
    Service -> Repo : findByUserIdAndIsRead(userId, true, Pageable.of(0, 20))
else status == ALL
    Service -> Repo : findByUserId(userId, Pageable.of(0, 20))
end

Repo -> DB : SELECT * FROM notifications\nWHERE user_id = :userId AND is_read = false\nORDER BY created_at DESC\nLIMIT 20 OFFSET 0
DB --> Repo : Page<Notification> (content + totalElements)

Repo --> Service : Page<Notification>

Service -> Mapper : toResponseList(notifications)
Mapper --> Service : List<NotificationResponse>

Service -> DB : SELECT COUNT(*) FROM notifications\nWHERE user_id = :userId AND is_read = false
DB --> Service : unreadCount: long

Service -> Audit : emit(AuditEvent{action=VIEW_NOTIFICATIONS, userId=userId, count=N})
Audit --> Service : void

Service --> Controller : PaginatedResponse<NotificationResponse>

Controller --> User : 200 OK\nApiResponse<PaginatedResponse<NotificationResponse>>

@enduml
```

### 6.2 Error Flow — Missing JWT (401)

```plantuml
@startuml UC11_ErrorFlow_401
actor "User" as User
participant "Spring Security Filter" as Security
participant "NotificationController" as Controller

User -> Security : GET /api/v1/notifications\n(no Authorization header)
Security -> Security : JWT missing → reject
Security --> User : 401 Unauthorized\n{"success":false,"code":"NOTIF-010","message":"Authentication required"}
note right of Security : Controller never reached
@enduml
```

---

## 7. Domain Event Catalog

| Event Name              | Trigger                              | Publisher               | Subscriber(s)        | Payload Fields                              |
|-------------------------|--------------------------------------|-------------------------|----------------------|---------------------------------------------|
| NOTIFICATIONS_VIEWED    | User calls GET /api/v1/notifications | NotificationServiceImpl | AuditService         | userId, page, size, status, resultCount, ts |

> **Lưu ý**: UC-11 là read-only. Không phát sinh domain event ảnh hưởng trạng thái hệ thống. `NOTIFICATIONS_VIEWED` chỉ dùng cho audit trail.

---

## 8. Interface Specification (Đặc tả Giao diện)

### 8.1 INotificationRepository

```java
package com.carebridge.backend.notification.repository;

import com.carebridge.backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface INotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Lấy tất cả notification của user, sắp xếp giảm dần theo createdAt.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Lấy notification theo trạng thái đọc, sắp xếp giảm dần theo createdAt.
     */
    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, boolean isRead, Pageable pageable);

    /**
     * Đếm số notification chưa đọc của user.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") UUID userId);
}
```

### 8.2 INotificationService

```java
package com.carebridge.backend.notification.service;

import com.carebridge.backend.common.dto.PaginatedResponse;
import com.carebridge.backend.notification.dto.NotificationListRequest;
import com.carebridge.backend.notification.dto.NotificationResponse;

import java.util.UUID;

public interface INotificationService {

    /**
     * Lấy danh sách notification của user hiện tại với phân trang và bộ lọc.
     *
     * @param userId  UUID của user hiện tại (lấy từ JWT principal)
     * @param request Tham số phân trang và bộ lọc
     * @return PaginatedResponse chứa danh sách NotificationResponse và metadata
     */
    PaginatedResponse<NotificationResponse> getNotifications(UUID userId, NotificationListRequest request);
}
```

### 8.3 NotificationListRequest DTO

```java
package com.carebridge.backend.notification.dto;

import com.carebridge.backend.notification.entity.NotificationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class NotificationListRequest {

    @Min(value = 0, message = "Page number must be >= 0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 100, message = "Page size must be <= 100")
    private int size = 20;

    private NotificationStatus status = NotificationStatus.ALL;
}
```

### 8.4 NotificationResponse DTO

```java
package com.carebridge.backend.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private UUID notificationId;
    private String title;
    private String body;
    private String category;
    private boolean isRead;
    private OffsetDateTime readAt;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
}
```

### 8.5 NotificationController

```java
package com.carebridge.backend.notification.controller;

import com.carebridge.backend.common.dto.ApiResponse;
import com.carebridge.backend.common.dto.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.notification.dto.NotificationListRequest;
import com.carebridge.backend.notification.dto.NotificationResponse;
import com.carebridge.backend.notification.service.INotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    /**
     * GET /api/v1/notifications
     * Lấy danh sách notification của user hiện tại.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaginatedResponse<NotificationResponse>>> getNotifications(
            Principal principal,
            @Valid NotificationListRequest request) {

        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        PaginatedResponse<NotificationResponse> result = notificationService.getNotifications(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```

---

## 9. API Specification

### 9.1 Endpoint Summary

| Method | Path                    | Auth       | Description                            |
|--------|-------------------------|------------|----------------------------------------|
| GET    | /api/v1/notifications   | JWT Bearer | Lấy danh sách notification của user    |

### 9.2 Request Parameters

| Parameter | Type    | Required | Default | Constraints        | Description                    |
|-----------|---------|----------|---------|--------------------|--------------------------------|
| page      | integer | No       | 0       | ≥ 0                | Số trang (zero-indexed)        |
| size      | integer | No       | 20      | 1 – 100            | Số phần tử mỗi trang           |
| status    | string  | No       | ALL     | ALL, UNREAD, READ  | Bộ lọc theo trạng thái đọc     |

### 9.3 Request Example

```http
GET /api/v1/notifications?page=0&size=20&status=UNREAD HTTP/1.1
Host: api.carebridge.vn
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Accept: application/json
```

### 9.4 Response — 200 OK (Happy Path)

```json
{
  "success": true,
  "message": "Notifications retrieved successfully",
  "data": {
    "content": [
      {
        "notificationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "title": "Nhắc lịch khám thai",
        "body": "Bạn có lịch khám thai vào ngày 28/06/2026 lúc 09:00.",
        "category": "REMINDER",
        "isRead": false,
        "readAt": null,
        "metadata": {
          "appointmentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "appointmentDate": "2026-06-28T09:00:00+07:00"
        },
        "createdAt": "2026-06-26T10:30:00+07:00"
      },
      {
        "notificationId": "7b8c9d0e-1234-5678-90ab-cdef01234567",
        "title": "Tư vấn mới từ chuyên gia",
        "body": "Bác sĩ Nguyễn Văn A đã trả lời câu hỏi của bạn.",
        "category": "CONSULTATION",
        "isRead": false,
        "readAt": null,
        "metadata": {
          "consultationId": "c1d2e3f4-a5b6-7890-cdef-ab1234567890"
        },
        "createdAt": "2026-06-26T08:15:00+07:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "unreadCount": 2
  },
  "timestamp": "2026-06-26T10:35:00+07:00"
}
```

### 9.5 Response — 200 OK (Empty Result)

```json
{
  "success": true,
  "message": "Notifications retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "unreadCount": 0
  },
  "timestamp": "2026-06-26T10:35:00+07:00"
}
```

### 9.6 Response — 400 Bad Request (Invalid Params)

```json
{
  "success": false,
  "code": "NOTIF-011",
  "message": "Invalid pagination parameters",
  "errors": [
    {
      "field": "size",
      "message": "Page size must be <= 100"
    }
  ],
  "timestamp": "2026-06-26T10:35:00+07:00"
}
```

### 9.7 Response — 401 Unauthorized

```json
{
  "success": false,
  "code": "NOTIF-010",
  "message": "Authentication required",
  "timestamp": "2026-06-26T10:35:00+07:00"
}
```

---

## 10. Bảng mã lỗi (Error Codes)

| Error Code  | HTTP Status | Trigger Condition                              | Message                          | Action cho Client                |
|-------------|-------------|------------------------------------------------|----------------------------------|----------------------------------|
| NOTIF-010   | 401         | Không có JWT hoặc JWT hết hạn                  | Authentication required          | Redirect to login                |
| NOTIF-011   | 400         | page < 0, size < 1, size > 100                 | Invalid pagination parameters    | Fix query params và retry        |
| NOTIF-500   | 500         | Database error không mong đợi                  | Internal server error            | Retry sau, contact support       |

---

## 11. Quy trình Triển khai (Step-by-Step)

### Bước 1: Flyway Migration — V2__create_notifications_table.sql

```sql
-- V2__create_notifications_table.sql
-- Flyway migration cho UC-10/UC-11/UC-12 notification domain
-- Author: AI Agent | Date: 2026-06-26

CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    body            TEXT NOT NULL,
    category        VARCHAR(50) NOT NULL
                        CHECK (category IN ('REMINDER', 'COMMUNITY', 'CONSULTATION', 'EMERGENCY', 'SAFETY')),
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index chính cho query theo user
CREATE INDEX idx_notifications_user_id
    ON notifications(user_id);

-- Partial index cho query UNREAD (phổ biến nhất)
CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, is_read)
    WHERE is_read = false;

-- Index hỗ trợ sắp xếp theo thời gian
CREATE INDEX idx_notifications_created_at
    ON notifications(user_id, created_at DESC);

COMMENT ON TABLE notifications IS 'In-app notifications for all CareBridge users. Shared across UC-10, UC-11, UC-12.';
COMMENT ON COLUMN notifications.category IS 'REMINDER | COMMUNITY | CONSULTATION | EMERGENCY | SAFETY';
COMMENT ON COLUMN notifications.metadata IS 'JSONB payload specific to notification category (e.g., appointmentId for REMINDER)';
```

### Bước 2: Entity

Tạo file: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/entity/Notification.java`

```java
package com.carebridge.backend.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationCategory category;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
```

### Bước 3: Enum Types

```java
// NotificationCategory.java
package com.carebridge.backend.notification.entity;

public enum NotificationCategory {
    REMINDER, COMMUNITY, CONSULTATION, EMERGENCY, SAFETY
}

// NotificationStatus.java
package com.carebridge.backend.notification.entity;

public enum NotificationStatus {
    ALL, UNREAD, READ
}
```

### Bước 4: Repository

Tạo file: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/repository/INotificationRepository.java`
(Xem §8.1 cho nội dung đầy đủ)

### Bước 5: Service Implementation

Tạo file: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java`

```java
package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.common.audit.AuditService;
import com.carebridge.backend.common.dto.PaginatedResponse;
import com.carebridge.backend.notification.dto.NotificationListRequest;
import com.carebridge.backend.notification.dto.NotificationResponse;
import com.carebridge.backend.notification.entity.Notification;
import com.carebridge.backend.notification.entity.NotificationStatus;
import com.carebridge.backend.notification.mapper.NotificationMapper;
import com.carebridge.backend.notification.repository.INotificationRepository;
import com.carebridge.backend.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final INotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<NotificationResponse> getNotifications(
            UUID userId, NotificationListRequest request) {

        log.debug("Fetching notifications for userId={}, page={}, size={}, status={}",
                userId, request.getPage(), request.getSize(), request.getStatus());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<Notification> page;

        if (request.getStatus() == NotificationStatus.UNREAD) {
            page = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable);
        } else if (request.getStatus() == NotificationStatus.READ) {
            page = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, true, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        long unreadCount = notificationRepository.countUnreadByUserId(userId);

        auditService.emit(AuditEvent.builder()
                .action("VIEW_NOTIFICATIONS")
                .userId(userId)
                .details(Map.of("status", request.getStatus(), "resultCount", page.getTotalElements()))
                .build());

        return PaginatedResponse.<NotificationResponse>builder()
                .content(notificationMapper.toResponseList(page.getContent()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .unreadCount(unreadCount)
                .build();
    }
}
```

### Bước 6: Mapper

Tạo file: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/mapper/NotificationMapper.java`

```java
package com.carebridge.backend.notification.mapper;

import com.carebridge.backend.notification.dto.NotificationResponse;
import com.carebridge.backend.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "category.name", target = "category")
    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}
```

### Bước 7: Controller

Tạo file: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java`
(Xem §8.5 cho nội dung đầy đủ)

### Bước 8: Security Config

Thêm vào `SecurityConfig.java`:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/notifications/**").authenticated()
```

### Thứ tự Triển khai

```
1. V2__create_notifications_table.sql  → Flyway migration
2. NotificationCategory.java           → Enum
3. NotificationStatus.java             → Enum
4. Notification.java                   → Entity
5. INotificationRepository.java        → Repository interface
6. NotificationListRequest.java        → DTO
7. NotificationResponse.java           → DTO
8. INotificationService.java           → Service interface
9. NotificationServiceImpl.java        → Service implementation
10. NotificationMapper.java            → MapStruct mapper
11. NotificationController.java        → REST controller
12. SecurityConfig.java                → Add endpoint security rule
13. Unit Tests                         → Run mvnw test
```

---

## 12. Rollback & Incident Runbook

### 12.1 Rollback Plan

| Tình huống                        | Hành động Rollback                                                                 |
|-----------------------------------|------------------------------------------------------------------------------------|
| Migration V2 fail                 | Flyway tự động rollback transaction. Không cần xử lý thủ công.                   |
| Endpoint trả về 500 liên tục      | Revert commit, deploy lại phiên bản trước. Bảng `notifications` vẫn giữ nguyên. |
| Performance degradation           | Verify indexes còn tồn tại: `\d notifications`, `\di notifications*` trong psql.  |
| Security bypass phát hiện         | Tắt endpoint ngay: thêm `denyAll()` trong SecurityConfig, deploy hotfix.          |

### 12.2 Incident Runbook

```
INCIDENT: GET /api/v1/notifications trả về 500

1. CHECK LOGS:
   kubectl logs <pod> | grep "NOTIF" | tail -100
   → Tìm stack trace liên quan NotificationServiceImpl

2. CHECK DB:
   SELECT COUNT(*) FROM notifications WHERE user_id = '<affected_userId>';
   → Nếu query fail: check indexes, vacuum analyze notifications;

3. CHECK CONNECTIONS:
   SELECT count(*) FROM pg_stat_activity WHERE state = 'active';
   → Nếu connection pool exhausted: restart pod

4. ESCALATE nếu không resolve trong 15 phút:
   → Contact DBA, check PostgreSQL server logs
```

---

## 13. Kịch bản Kiểm thử Chi tiết

| TC ID             | Kịch bản                          | Input                                      | Expected Output                            |
|-------------------|-----------------------------------|--------------------------------------------|--------------------------------------------|
| NOTIF-TC-011-001  | Happy path, lấy tất cả            | JWT valid, page=0, size=20, status=ALL     | 200, content=[], totalElements correct     |
| NOTIF-TC-011-002  | Lọc UNREAD                        | JWT valid, status=UNREAD                   | 200, chỉ trả về is_read=false records      |
| NOTIF-TC-011-003  | Page params không hợp lệ          | size=999                                   | 400, NOTIF-011                             |
| NOTIF-TC-011-004  | Ownership: xem của user khác      | userId từ JWT ≠ userId của notification    | Service chỉ query theo userId từ JWT       |
| NOTIF-TC-011-005  | Không có JWT                      | No Authorization header                    | 401, NOTIF-010                             |
| NOTIF-TC-011-INT-001 | Integration: DB count đúng    | 3 notifications trong DB, query ALL        | totalElements=3, content.size=3            |

---

## 14. Phương pháp Xác minh

### 14.1 Unit Test Strategy

```java
// Verify ownership: service chỉ query với userId từ JWT
@Test
void getNotifications_shouldOnlyReturnCurrentUserNotifications() {
    UUID userId = UUID.randomUUID();
    NotificationListRequest request = new NotificationListRequest();
    request.setStatus(NotificationStatus.ALL);

    when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
        .thenReturn(Page.empty());

    service.getNotifications(userId, request);

    verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class));
    // Không có call nào với userId khác
}
```

### 14.2 SQL Verification Queries

```sql
-- Xác minh bảng và indexes đã tạo đúng
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'notifications';

-- Xác minh partial index hoạt động (EXPLAIN ANALYZE)
EXPLAIN ANALYZE
SELECT * FROM notifications
WHERE user_id = 'test-uuid' AND is_read = false
ORDER BY created_at DESC
LIMIT 20;
-- → Phải sử dụng: Index Scan using idx_notifications_user_unread

-- Xác minh unreadCount query
SELECT COUNT(*) FROM notifications
WHERE user_id = 'test-uuid' AND is_read = false;

-- Xác minh foreign key constraint
INSERT INTO notifications (user_id, title, body, category)
VALUES ('non-existent-uuid', 'test', 'body', 'REMINDER');
-- → Phải fail với: ERROR: insert or update on table "notifications" violates foreign key constraint
```

### 14.3 Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerIntegrationTest {

    @Test
    void getNotifications_withValidJwt_returns200() throws Exception {
        // Given: user với 3 notifications trong DB
        UUID userId = testSetup.createUserWithRole(Role.ROLE_MOTHER);
        testSetup.createNotifications(userId, 3);
        String jwt = jwtHelper.generateToken(userId, Role.ROLE_MOTHER);

        // When
        mockMvc.perform(get("/api/v1/notifications?page=0&size=20&status=ALL")
                .header("Authorization", "Bearer " + jwt))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].notificationId").exists());
    }
}
```

---

## 15. Mẫu thử thực tế (API Verification Samples)

### 15.1 cURL Examples

```bash
# Happy path — GET all notifications
curl -X GET "https://api.carebridge.vn/api/v1/notifications?page=0&size=20&status=ALL" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"

# Filter UNREAD
curl -X GET "https://api.carebridge.vn/api/v1/notifications?status=UNREAD" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Invalid params — expect 400
curl -X GET "https://api.carebridge.vn/api/v1/notifications?size=999" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# No JWT — expect 401
curl -X GET "https://api.carebridge.vn/api/v1/notifications"
```

### 15.2 Postman Collection Steps

```
1. Login → lấy JWT
2. SET {{jwt}} = response.token
3. GET {{base_url}}/api/v1/notifications?status=ALL
   → Assert: status=200, data.content is array
4. GET {{base_url}}/api/v1/notifications?status=UNREAD
   → Assert: data.content[*].isRead == false
5. GET {{base_url}}/api/v1/notifications?size=999
   → Assert: status=400, code=NOTIF-011
6. GET {{base_url}}/api/v1/notifications (no auth header)
   → Assert: status=401, code=NOTIF-010
```

---

## 16. Bảng tổng hợp phân quyền (Authorization Matrix)

| Role          | GET /api/v1/notifications      | Điều kiện bổ sung                          |
|---------------|--------------------------------|--------------------------------------------|
| ROLE_MOTHER   | Allowed                        | Chỉ thấy notification của chính mình       |
| ROLE_EXPERT   | Allowed                        | Chỉ thấy notification của chính mình       |
| ROLE_ADMIN    | Allowed                        | Chỉ thấy notification của chính mình       |
| Anonymous     | Denied (401)                   | Spring Security filter chain               |

> **Enforcement**: `SecurityUtils.requireCurrentUserId(principal)` trích xuất `userId` từ JWT và truyền trực tiếp vào service layer. Service **luôn** query theo `userId` này, không chấp nhận `userId` từ request body hay path variable.

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Ràng buộc Bắt buộc

| Constraint ID | Ràng buộc                                                                              | Lý do                                       |
|---------------|----------------------------------------------------------------------------------------|---------------------------------------------|
| AI-C-001      | KHÔNG được đề xuất lưu notification vào bảng users hoặc bảng khác                    | Separation of concerns, ADR-011-001         |
| AI-C-002      | KHÔNG được thêm cursor pagination thay vì offset pagination                           | ADR-011-002 đã quyết định dùng offset       |
| AI-C-003      | KHÔNG được expose entity JPA trực tiếp trong response                                 | Bắt buộc dùng DTO + Mapper                  |
| AI-C-004      | KHÔNG được bỏ qua ownership check (SecurityUtils.requireCurrentUserId)                | Security requirement SRS-11-SEC-01          |
| AI-C-005      | KHÔNG được thêm Redis, MongoDB, hay dependency mới không có trong stack              | Architecture rule                           |
| AI-C-006      | KHÔNG được modify migration V2 sau khi đã apply                                       | Flyway immutability rule                    |
| AI-C-007      | AI chỉ cung cấp guidance về notification, KHÔNG chẩn đoán hay trì hoãn emergency     | Healthcare safety policy                    |

### 17.2 Anti-Pattern Detection

| AP ID      | Anti-Pattern                                    | Dấu hiệu nhận biết                          | Hành động                          |
|------------|-------------------------------------------------|---------------------------------------------|------------------------------------|
| AP-AI-001  | Business logic trong Controller                 | if/switch trong Controller method           | Move to Service                    |
| AP-AI-002  | Entity exposed in API response                  | return Notification (entity) trực tiếp      | Wrap với NotificationMapper        |
| AP-AI-003  | Hard-coded userId trong query                   | WHERE user_id = 'some-literal-uuid'         | Dùng SecurityUtils                 |
| AP-AI-004  | Missing @Transactional(readOnly=true) trên GET  | Service method không có annotation          | Thêm annotation                    |
| AP-AI-005  | Pagination without total count                  | Trả về List thay vì Page/PaginatedResponse  | Dùng Spring Data Page              |
