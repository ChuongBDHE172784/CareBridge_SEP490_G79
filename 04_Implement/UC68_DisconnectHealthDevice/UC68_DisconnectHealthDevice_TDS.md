# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# UC68 — Disconnect Health Device

| Field | Value |
|-------|-------|
| **Document ID** | `CB-DEVICE-IMP-003` |
| **Version** | `1.0` |
| **Date** | `2026-07-01` |
| **Status** | `Draft` |
| **Document Owner** | `TV2-Bách` |
| **Author** | `AI Agent — Technical Architect` |
| **Reviewed by** | `[ ] Pending` |
| **DPO Sign-off** | `[ ] Pending` *(bắt buộc — module xử lý consent revocation)* |
| **Approved by** | `[ ] Pending` |
| **Last Review** | `2026-07-01` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Technical Architect | Tạo tài liệu lần đầu — TDS cho UC68 Disconnect Health Device (Draft) |

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

> UC68 cho phép Mother ngắt kết nối một thiết bị đang `CONNECTED` (được tạo bởi UC66) và dừng đồng bộ dữ liệu sức khỏe từ thiết bị đó. UC68 tái sử dụng **hoàn toàn** entity `DeviceConnection` từ UC66 — KHÔNG tạo bảng mới. Đây là nửa còn lại của state machine `CONNECTED → DISCONNECTED` đã định nghĩa trong UC66 TDS (ADR-DEVICE-001).

| Field | Value |
|-------|-------|
| **Module Name** | `Disconnect Health Device` |
| **Bounded Context** | `health.device` (dùng chung entity/repository với UC66) |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC66 ConnectHealthDevice` (bảng `device_connections`), `consent` module (`ConsentGrant.revoke`), `IAM (JWT)` |
| **Downstream Consumers** | `UC69 ViewDeviceDataTrend` (ẩn/đánh dấu thiết bị đã ngắt), audit trail |

**Nguồn gốc & phạm vi:**
- Function spec: `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.45` (dòng 2704-2721), UC-68.
- Description gốc: "Disconnects the device and stops health device synchronization."
- **In-scope:** Chuyển trạng thái 1 `device_connections` record từ `CONNECTED` → `DISCONNECTED`, revoke consent liên kết (nếu có), phát `DeviceDisconnected` event để dừng mọi tác vụ sync liên quan.
- **Out-of-scope:** Xóa lịch sử `maternal_health_metrics` đã import từ thiết bị đó (dữ liệu lịch sử KHÔNG bị xóa khi disconnect — chỉ dừng sync tương lai); kết nối lại (thuộc UC66 lần nữa, tạo record mới theo ADR-DEVICE-001).

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại (BR/ADR/US) | Mô tả yêu cầu | Thành phần Code | Compliance Target | ADR liên quan |
|----------------|------------------|---------------|-----------------|-------------------|---------------|
| UC-68 (SRS §3.3.1.45) | User Story | Mother ngắt kết nối thiết bị, dừng đồng bộ | `DeviceConnectionController.PATCH /api/v1/health/devices/connections/{id}/disconnect` | — | ADR-DEVICE-007 |
| PRE-3 / BR-RBAC | Business Rule | Chỉ owner (Mother sở hữu record) mới disconnect được | `DeviceConnectionService.disconnect()` | — | — |
| BR-PRIVACY | Business Rule | Ngắt kết nối phải revoke consent liên quan | `ConsentService.revoke()` (reuse, gọi với `consentGrantId` lưu ở UC66) | PDPA | ADR-DEVICE-007 |
| BR-CONSULTATION | Business Rule | Vòng đời kết nối auditable — disconnect ghi nhận thời điểm | `DeviceConnection.disconnectedAt` + `DeviceDisconnected` event | — | ADR-DEVICE-001 (UC66) |
| E1 (Exceptions) | Exception Flow | Access denied khi không sở hữu record | `DeviceConnectionController` (403) | — | — |
| E2 (Exceptions) | Exception Flow | Disconnect trên record đã DISCONNECTED hoặc không tồn tại | `DeviceConnectionService.disconnect()` | — | — |
| ADR-DEVICE-007 | Decision | Disconnect = UPDATE status + revoke ConsentGrant trong 1 transaction; KHÔNG xóa record hay lịch sử metric | `DeviceConnectionService.disconnect()` | GDPR-equivalent Art. 17 (right to restrict, not necessarily erase) | ADR-DEVICE-001 (UC66) |

---

## 3. Architecture Decision Records (ADR)

### ADR-DEVICE-007 — Disconnect Transactionally Updates Status + Revokes Consent

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `AI Agent — Technical Architect` |
| **Date** | `2026-07-01` |
| **Supersedes** | `—` |

#### Bối cảnh (Context)
UC66 ADR-DEVICE-002 quyết định lưu `consent_grant_id` trên `device_connections` khi connect. Khi disconnect, nếu chỉ update `status=DISCONNECTED` mà không revoke `ConsentGrant` tương ứng, sẽ có tình trạng "lệch state": kết nối đã ngắt nhưng consent vẫn còn hiệu lực trong `consent_grants` — vi phạm nguyên tắc đồng bộ consent ↔ trạng thái thực tế (NFR §4.2 của UC66 TDS).

#### Các phương án đã xem xét (Options Considered)

| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A | Disconnect thực hiện trong 1 `@Transactional` method: (1) UPDATE `device_connections.status=DISCONNECTED, disconnected_at=now()`, (2) gọi `ConsentService.revoke(consentGrantId)` để set `revoked_at` trên `ConsentGrant` | Đồng bộ 2 nơi trong 1 transaction, atomic, nhất quán với ADR-DEVICE-002 | Phụ thuộc `ConsentService` phải có method `revoke()` khả dụng (đã tồn tại — xác nhận qua `RevokeConsentRequest.java`) |
| B | Chỉ update `device_connections.status`, không đụng đến consent (để consent tự hết hạn theo `expiryAt`) | Đơn giản hơn | Vi phạm kỳ vọng người dùng: user disconnect thiết bị nhưng consent "chia sẻ dữ liệu sức khỏe" vẫn hiệu lực đến khi hết hạn (có thể hàng năm) — vi phạm tinh thần BR-PRIVACY |

#### Quyết định (Decision)
Chọn **Phương án A**. `DeviceConnectionService.disconnect()` là 1 `@Transactional` method thực hiện cả 2 thao tác. Nếu `consentGrantId` là null (trường hợp hiếm — record cũ trước khi có consent linkage), chỉ update status và log warning.

#### Hệ quả (Consequences)

**Tích cực:** Nhất quán tuyệt đối giữa trạng thái kết nối và trạng thái consent — đáp ứng đúng kỳ vọng người dùng khi disconnect.

**Tiêu cực / Trade-offs:** Coupling giữa `health.device` và `consent` module tăng lên (nhưng đã có sẵn từ UC66, không phải rủi ro mới).

**Compliance Impact:** Hỗ trợ PDPA/Luật 91/2025 về quyền rút lại sự đồng ý (right to withdraw consent) một cách kịp thời.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA | Measurement Method | Compliance Basis |
|----------|-------------|------------|---------------------|------------------|
| Latency | API response (p99) | `< 300ms` | k6 load test | — |
| Availability | Uptime (monthly) | `99.9%` | Uptime monitor | — |

### 4.2. Data Integrity & Retention

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Consistency | disconnect luôn atomic: status + consent revoke cùng lúc hoặc cùng rollback | 100% | `@Transactional` integration test | PDPA |
| Retention | Lịch sử metric đã import KHÔNG bị xóa khi disconnect | 100% giữ nguyên | DB assertion (metric count unchanged) | PDPA — data minimization áp dụng cho consent, không phải xóa lịch sử y tế |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Access control | Chỉ owner mới disconnect được record của mình | Least privilege | Auth Matrix (§16) | BR-RBAC |
| Audit | Disconnect action ghi nhận qua event + `disconnected_at` | 100% | Log/event assertion | BR-CONSULTATION |

### 4.4. Scalability & Capacity Planning

> Không có số liệu tải cụ thể — Open, giả định tải thấp.

---

## 5. Static Modeling (Mô hình Tĩnh)

### 5.1. Class Diagram (PlantUML)

```plantuml
@startuml UC68_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

' === REUSED FROM UC66 (no changes to entity structure) ===
class DeviceConnection {
  + id: UUID
  + userId: UUID
  + deviceType: DeviceType
  + status: DeviceConnectionStatus
  + consentGrantId: Long
  + disconnectedAt: Instant
  ' ... (see UC66 TDS §5.1 for full attribute list)
}

enum DeviceConnectionStatus <<enum — from UC66, unchanged>> {
  CONNECTED
  DISCONNECTED
}

' === SERVICE (extends UC66's IDeviceConnectionService) ===
interface IDeviceConnectionService <<interface — extended from UC66>> {
  + disconnect(connectionId: UUID, userId: UUID): DeviceConnectionResponse
}

class DeviceConnectionService <<extended from UC66>> {
  - deviceConnectionRepository: IDeviceConnectionRepository
  - consentService: ConsentService
  - eventPublisher: ApplicationEventPublisher
  + disconnect(connectionId: UUID, userId: UUID): DeviceConnectionResponse
}

DeviceConnectionService --> IDeviceConnectionRepository : uses (same repo as UC66)
DeviceConnectionService --> ConsentService : reuses (revoke consent)

@enduml
```

### 5.2. Data Structure (Flyway SQL Migration)

> **Không cần migration mới cho UC68.** Bảng `device_connections` đã được tạo ở UC66 (`V20260701140000__create_device_connections.sql`) với đầy đủ cột `status`, `disconnected_at`, `consent_grant_id` cần thiết cho disconnect. UC68 chỉ thực hiện `UPDATE`, không cần `ALTER TABLE`.

**V1__init_schema.sql sync action:** Không áp dụng — không có thay đổi schema cho UC68.

---

## 6. Dynamic Modeling (Mô hình Động)

### 6.1. Sequence Diagram — Happy Path (PlantUML)

```plantuml
@startuml UC68_SequenceDiagram_HappyPath
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor       "Mother (Mobile App)" as Client
participant "DeviceConnectionController" as Controller
participant "DeviceConnectionService"    as Service
participant "ConsentService"             as Consent
participant "DeviceConnectionRepository" as Repository
database    "PostgreSQL"                 as DB
participant "EventPublisher"             as Publisher

Client -> Controller : PATCH /api/v1/health/devices/connections/{id}/disconnect
activate Controller
Controller -> Service : disconnect(connectionId, userId)
activate Service

Service -> Repository : findById(connectionId)
Repository --> Service : DeviceConnection{status=CONNECTED, userId matches, consentGrantId=1}
Service -> Service : Verify ownership (connection.userId == userId)

Service -> Repository : save(connection{status=DISCONNECTED, disconnectedAt=now()})
activate Repository
Repository -> DB : UPDATE device_connections SET status='DISCONNECTED', disconnected_at=NOW()
DB --> Repository : updated row
deactivate Repository

Service -> Consent : revoke(consentGrantId, userId)
activate Consent
Consent --> Service : ConsentGrant{revokedAt=now()}
deactivate Consent

Service -> Publisher : publish(DeviceDisconnected)
Service --> Controller : DeviceConnectionResponse{status:"DISCONNECTED"}
deactivate Service

Controller --> Client : HTTP 200\n{id, status:"DISCONNECTED", disconnectedAt}
deactivate Controller
@enduml
```

### 6.2. Sequence Diagram — Error Path (PlantUML)

```plantuml
@startuml UC68_SequenceDiagram_ErrorPath
skinparam backgroundColor #FAFAFA
actor "Mother" as Client
participant "DeviceConnectionController" as Controller
participant "DeviceConnectionService" as Service

Client -> Controller : PATCH .../connections/{id}/disconnect
activate Controller
Controller -> Service : disconnect(connectionId, userId)
activate Service
Service -> Service : findById → not found OR already DISCONNECTED
Service --> Controller : throws DeviceConnectionException(DEVICE-203)
deactivate Service
Controller --> Client : HTTP 409\n{error:{code:"DEVICE-203", message:"Connection is already disconnected or not found"}}
deactivate Controller

Client -> Controller : PATCH .../connections/{otherUsersId}/disconnect
activate Controller
Controller --> Client : HTTP 403\n{error:{code:"DEVICE-204"}}
deactivate Controller
@enduml
```

### 6.3. State Machine (Reference — full definition owned by UC66 TDS §6.4)

> UC68 thực hiện transition `CONNECTED → DISCONNECTED` đã được định nghĩa đầy đủ trong `UC66_ConnectHealthDevice_TDS.md §6.4` (ADR-DEVICE-001). UC68 KHÔNG định nghĩa lại state machine — chỉ tham chiếu và tuân thủ invariant đã có:
> 1. Không xóa record.
> 2. `DISCONNECTED` là trạng thái cuối của record đó (terminal).
> 3. Reconnect sau disconnect tạo record MỚI qua UC66 (không revive record cũ).

---

## 7. Domain Event Catalog

### 7.1. Events Published (Phát ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|---------------|----------------|--------|
| `DeviceDisconnected` | Ngắt kết nối thành công (status → DISCONNECTED + consent revoked) | `DeviceConnectionService` | `UC69 ViewDeviceDataTrend` (đánh dấu nguồn "disconnected" trong trend view), Audit log sink | `DeviceDisconnected.java` | Yes |

### 7.2. Events Consumed (Tiêu thụ)

> UC68 không consume events từ module khác.

### 7.3. Payload Schema

```java
// DeviceDisconnected.java
package com.carebridge.backend.health.device.event;

public record DeviceDisconnected(
    UUID    eventId,
    String  eventType,        // "DeviceDisconnected"
    Instant occurredAt,
    String  version,          // "1.0"
    Payload payload,
    Metadata metadata
) implements ApplicationEvent {

    public record Payload(
        UUID   deviceConnectionId,
        UUID   userId,
        String deviceType,
        Long   revokedConsentGrantId // nullable if consentGrantId was null
    ) {}

    public record Metadata(UUID correlationId, String causedBy) {}
}
```

---

## 8. Interface Specification (Đặc tả Giao diện)

### 8.1. Service Interface

```java
// DeviceConnectionResponse.java — reused from UC66 (§8.1), no change

// IDeviceConnectionService.java — EXTENDED interface (adds disconnect() to UC66's contract)
// @version 1.1
// @breaking-change None — additive method only
package com.carebridge.backend.health.device.service;

public interface IDeviceConnectionService {
    // ... connect(), listActiveConnections() from UC66 (§8.1 there) ...

    /**
     * Disconnects an active device connection owned by the caller.
     * Transactionally updates status to DISCONNECTED and revokes the linked ConsentGrant.
     * @throws DeviceConnectionException (DEVICE-203) if connection not found or already DISCONNECTED
     * @throws AccessDeniedException (DEVICE-204) if connection does not belong to caller
     */
    DeviceConnectionResponse disconnect(UUID connectionId, UUID userId);
}
```

### 8.2. Repository Interface

```java
// IDeviceConnectionRepository.java — reused from UC66 (§8.2), no new method required.
// disconnect() uses existing findById() (inherited from JpaRepository) + save().
```

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `PATCH` | `/api/v1/health/devices/connections/{id}/disconnect` | JWT Bearer | `ROLE_MOTHER` | 30/min | No (second call on already-DISCONNECTED returns 409) |

### 9.2. Request / Response Schemas

#### `PATCH /api/v1/health/devices/connections/{id}/disconnect`

**Request Body:** *(empty — action endpoint)*

**Response — 200 OK (Happy Path):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "deviceType": "SMARTWATCH",
  "status": "DISCONNECTED",
  "connectedAt": "2026-07-01T08:00:00.000Z",
  "disconnectedAt": "2026-07-01T10:00:00.000Z"
}
```

**Response — 409 Conflict (Already Disconnected / Not Found):**
```json
{
  "error": {
    "code": "DEVICE-203",
    "message": "Connection is already disconnected or not found"
  }
}
```

**Response — 403 Forbidden (Not Owner):**
```json
{
  "error": {
    "code": "DEVICE-204",
    "message": "You do not have permission to disconnect this device"
  }
}
```

---

## 10. Bảng mã lỗi (Error Codes)

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|--------------|--------------|-------------------|
| `DEVICE-200` | 400 | Validation failed | Dữ liệu không hợp lệ | Malformed `id` path param (not a valid UUID) |
| `DEVICE-203` | 409 | Connection is already disconnected or not found | Kết nối đã ngắt hoặc không tồn tại | Record `status != CONNECTED`, or `id` does not exist |
| `DEVICE-204` | 403 | Insufficient permissions | Không đủ quyền | `connection.userId != caller userId`, or not `ROLE_MOTHER` |
| `DEVICE-205` | 500 | Internal error | Lỗi hệ thống | Unexpected failure (e.g., consent revoke fails, transaction rollback) |

---

## 11. Quy trình Triển khai (Step-by-Step)

### 11.1. Prerequisites

- [ ] ADR-DEVICE-007 đã Accepted
- [ ] UC66 đã implement (bảng `device_connections` tồn tại, `ConsentGrant` linkage hoạt động)
- [ ] `ConsentService.revoke()` tồn tại và hoạt động đúng (đã có sẵn trong codebase — `RevokeConsentRequest.java`)
- [ ] DPO sign-off

### 11.2. Pre-Migration Checklist

- [ ] **Không áp dụng** — UC68 không có migration mới (xem §5.2)

### 11.3. Implementation Steps

#### Chặng 1 — Không có migration mới
> Bỏ qua — dùng bảng đã có từ UC66.

#### Chặng 2 — Implement disconnect() trong DeviceConnectionService (mở rộng từ UC66)

```java
// Thêm method disconnect() vào DeviceConnectionService.java (đã tạo ở UC66)
// package com.carebridge.backend.health.device.service.DeviceConnectionService
```

#### Chặng 3 — Thêm endpoint vào DeviceConnectionController

```java
// Thêm @PatchMapping("/{id}/disconnect") vào DeviceConnectionController.java (đã tạo ở UC66)
```

### 11.4. Deployment Checklist

- [ ] `PATCH /connections/{id}/disconnect` trả 200 với status DISCONNECTED
- [ ] `ConsentGrant.revokedAt` được set sau disconnect
- [ ] `DeviceDisconnected` event published
- [ ] Metric lịch sử KHÔNG bị xóa (kiểm tra `maternal_health_metrics` count không đổi)

---

## 12. Rollback & Incident Runbook

### 12.1. Điều kiện kích hoạt Rollback

| Điều kiện | Ngưỡng | Người quyết định |
|-----------|--------|------------------|
| Disconnect xóa nhầm lịch sử metric | Bất kỳ case nào | Tech Lead + DPO (nghiêm trọng) |
| Consent không được revoke sau disconnect | Bất kỳ case nào | Tech Lead + DPO |

### 12.2. Rollback Procedure

```bash
# Không có migration để revert. Rollback chỉ cần revert code deploy.
kubectl rollout undo deployment/carebridge-api
kubectl rollout status deployment/carebridge-api
curl -X GET https://$HOST/api/v1/health
```

### 12.3. Notification Protocol

| Thời điểm | Người nhận | Kênh | Template |
|-----------|------------|------|----------|
| Ngay khi phát hiện | On-call team | Slack `#incident` | "🚨 DEVICE-DISCONNECT incident: [mô tả]" |
| Trong 30 phút | DPO | Email | *(bắt buộc nếu consent revoke thất bại — ảnh hưởng quyền riêng tư)* |

### 12.4. Post-Incident Review (PIR)

- Theo template chuẩn EDS §12.4.

---

## 13. Kịch bản Kiểm thử Chi tiết

> Chi tiết trong `UC68_DisconnectHealthDevice_Test-Spec.md`.

### 13.1. Unit Tests
- `DISCONNECT-TC-001`..`005`: happy path, already disconnected, not found, not owner, consent revoke called.

### 13.2. Integration Tests
- `DISCONNECT-TC-INT-001`: transactional atomicity (status + consent revoke) via Testcontainers.

### 13.3. E2E / Security Tests
- `DISCONNECT-TC-E2E-001`: cross-user disconnect attempt → 403.

---

## 14. Phương pháp Xác minh

### 14.1. Database Inspection

```sql
SELECT id, status, disconnected_at, consent_grant_id FROM device_connections WHERE id = '[uuid]';

SELECT revoked_at FROM consent_grants WHERE id = (
  SELECT consent_grant_id FROM device_connections WHERE id = '[uuid]'
);
-- Expected: revoked_at IS NOT NULL after disconnect

-- Verify metric history untouched
SELECT count(*) FROM maternal_health_metrics WHERE source_reference_id = '[uuid]';
-- Expected: unchanged before/after disconnect
```

### 14.2. Log / Audit Verification

```bash
kubectl logs -l app=carebridge-api | grep '"eventType":"DeviceDisconnected"' | head -5
```

---

## 15. Mẫu thử thực tế (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X PATCH https://$HOST/api/v1/health/devices/connections/$CONN_ID/disconnect \
  -H "Authorization: Bearer $MOTHER_JWT"
```

### 15.2. Error Paths

```bash
# Already disconnected
curl -X PATCH https://$HOST/api/v1/health/devices/connections/$CONN_ID/disconnect \
  -H "Authorization: Bearer $MOTHER_JWT"
```
**Expected Response (409):**
```json
{"error":{"code":"DEVICE-203","message":"Connection is already disconnected or not found"}}
```

---

## 16. Bảng tổng hợp phân quyền (Authorization Matrix)

| Endpoint | `GUEST` | `ROLE_MOTHER` | `ROLE_PARTNER` | `ROLE_FAMILY` | `ROLE_EXPERT` | `ROLE_SYSTEM_ADMIN` |
|----------|---------|---------------|----------------|---------------|---------------|---------------------|
| `PATCH /api/v1/health/devices/connections/{id}/disconnect` | ❌ | ✅ Own | ❌ | ❌ | ❌ | ✅ All (support only) |

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source (ADR/BR) | Last Verified |
|---|-----------|-----------------|---------------|
| C1 | disconnect() PHẢI là `@Transactional` — update status VÀ revoke consent cùng lúc hoặc cùng rollback | `ADR-DEVICE-007` | `2026-07-01` |
| C2 | KHÔNG xóa record `device_connections` khi disconnect — chỉ UPDATE status/disconnected_at | `ADR-DEVICE-001 (UC66)` | `2026-07-01` |
| C3 | KHÔNG xóa lịch sử `maternal_health_metrics` liên kết với device đã disconnect | `TDS §1 Out-of-scope` | `2026-07-01` |
| C4 | Chỉ owner (`connection.userId == callerUserId`) mới disconnect được | `BR-RBAC` | `2026-07-01` |
| C5 | `DeviceDisconnected` event PHẢI publish sau mỗi disconnect thành công | `§7.1 Domain Event Catalog` | `2026-07-01` |

### 17.2 Constraint Injection Block (Copy-Paste vào AI Prompt)

```
[CONSTRAINT BLOCK — Module: Disconnect Health Device — CB-DEVICE-IMP-003]
Theo TDS CB-DEVICE-IMP-003 và các ADR liên quan:

1. disconnect() là @Transactional: UPDATE device_connections.status=DISCONNECTED VÀ revoke ConsentGrant trong cùng transaction (ADR-DEVICE-007)
2. KHÔNG xóa record device_connections — chỉ update status (ADR-DEVICE-001)
3. KHÔNG xóa maternal_health_metrics lịch sử của device đã disconnect (TDS §1)
4. Chỉ owner mới disconnect (BR-RBAC)
5. Publish DeviceDisconnected event sau khi thành công (§7.1)

[CONTEXT BLOCK]
- Bounded Context: health.device
- Data Classification: Sensitive-PII
- Compliance: PDPA / Luật 91/2025
- Existing interfaces: §8 Service Interface (extends UC66's IDeviceConnectionService)
- Error codes: §10 Error Codes Table
- Auth matrix: §16 Authorization Matrix

[TASK BLOCK]
Implement DeviceConnectionService.disconnect() thỏa mãn constraints trên.
Output phải tuân thủ §8 Interface Specification. Tests phải cover §13 Test Scenarios.
```

### 17.3 Constraint Quality Checklist

- [x] Mỗi constraint traceable về ADR hoặc BR cụ thể
- [x] Không có constraint generic
- [x] Mỗi constraint có `Last Verified` ≤ 2 sprints
- [x] Constraint block có ≥ 3 constraints (có 5)
- [x] Constraint block reference §8 và §16

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu | Hành động |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code xóa record thay vì update status | Reject — enforce C2 |
| AP-AI-003 | Implicit Decision | Code xóa metric lịch sử khi disconnect | Reject — enforce C3, không có ADR nào cho phép xóa |
| AP-AI-005 | Hallucinated Contract | Code import service/type không có trong §8 | Reject |

---

## PHỤ LỤC

### A. Glossary

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| Revoke Consent | Đánh dấu `ConsentGrant.revokedAt` — không xóa record consent |
| Terminal State | Trạng thái cuối của 1 record — không transition tiếp (DISCONNECTED cho `device_connections`) |

### B. Tài liệu tham chiếu

| Document | Link / Path |
|----------|-------------|
| SRS UC-68 | `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.45` |
| UC66 TDS (state machine gốc) | `04_Implement/UC66_ConnectHealthDevice/UC66_ConnectHealthDevice_TDS.md §6.4` |
| Existing consent module | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/ConsentService.java` |

---

## OPEN ITEMS

| # | Open Item | Impact nếu không resolve | Đề xuất tạm thời |
|---|-----------|---------------------------|-------------------|
| O1 | SRS UC-68 flows là generic template — không có business rule cụ thể về việc có cho phép "tạm ngắt rồi tự động dừng background sync job" hay không (vì auto-sync thật thuộc `3.1.2.4`, ngoài phạm vi) | Không ảnh hưởng phạm vi UC68 hiện tại (mock-first, không có sync job thật) | Ghi rõ: disconnect chỉ đổi trạng thái DB + revoke consent; không có background job nào cần dừng ở giai đoạn này |
| O2 | Không rõ có cần thông báo (notification) cho Mother xác nhận disconnect thành công hay không (SRS không đặc tả UI/UX chi tiết) | UX polish — không block backend logic | Backend trả response 200 đầy đủ thông tin; UI/UX decision để mobile team quyết định khi build màn hình |

---

*TDS Draft — chờ review và approval. KHÔNG tự set Status = Approved.*
