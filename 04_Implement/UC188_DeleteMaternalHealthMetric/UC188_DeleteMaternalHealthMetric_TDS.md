# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# UC188 — Delete Maternal Health Metric

| Field | Value |
|-------|-------|
| **Document ID** | `CB-HEALTH-IMP-004` |
| **Version** | `1.0` |
| **Date** | `2026-07-03` |
| **Status** | `Draft` |
| **Document Owner** | `TV2-Bách` |
| **Author** | `AI Agent — Technical Architect` |
| **Reviewed by** | `[ ] Pending` |
| **DPO Sign-off** | `[ ] Pending` *(bắt buộc — module xử lý PII sức khỏe thai sản)* |
| **Approved by** | `[ ] Pending` |
| **Last Review** | `2026-07-03` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent — Technical Architect | Tạo tài liệu lần đầu — TDS cho UC188 Delete Maternal Health Metric (Draft) |

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

> UC188 cho phép Mother soft-delete một `maternal_health_metrics` record do chính mình nhập (nhập sai hoặc không còn cần). UC188 tái sử dụng **hoàn toàn** entity `MaternalHealthMetric`, `MetricStatus` enum, và `MaternalHealthMetricRepository` đã được tạo bởi UC187 (View Maternal Health Metric Detail) — bảng thực `maternal_health_metrics` với cột `status` đã tồn tại từ migration `V20260627100200__add_maternal_metric_status.sql`. UC188 KHÔNG tạo bảng mới, KHÔNG tạo entity mới.

| Field | Value |
|-------|-------|
| **Module Name** | `Delete Maternal Health Metric` |
| **Bounded Context** | `health` (dùng chung entity/repository với UC187) |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC187 View Maternal Health Metric Detail` (entity `MaternalHealthMetric`, repository `MaternalHealthMetricRepository`), `journey.MotherJourneyRepository` (ownership check), `IAM (JWT)` |
| **Downstream Consumers** | `UC69 ViewDeviceDataTrend` / trend views (phải loại trừ metric `DELETED`), audit trail |

**Nguồn gốc & phạm vi:**
- Function spec: `02_Requirements/SRS/3_Functional_Specification.md §3.3.11.2` (dòng 4047-4066), UC-188.
- Description gốc: "Soft-deletes a Mother-entered metric that is no longer needed or was entered incorrectly."
- **In-scope:** Chuyển trạng thái 1 `maternal_health_metrics` record từ `ACTIVE` → `DELETED` (soft-delete, không xóa vật lý), verify ownership qua `journey.owner_user_id`, phát `MaternalHealthMetricDeleted` event.
- **Out-of-scope:** Xóa vĩnh viễn (hard delete) — không được hỗ trợ theo BR-PRIVACY (audit/retention); khôi phục (restore) metric đã xóa — không có UC nào yêu cầu, đánh dấu **Open**; xóa hàng loạt (bulk delete) — không có trong SRS.

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại (BR/ADR/US) | Mô tả yêu cầu | Thành phần Code | Compliance Target | ADR liên quan |
|----------------|------------------|---------------|-----------------|-------------------|---------------|
| UC-188 (SRS §3.3.11.2) | User Story | Mother soft-deletes a metric she entered | `HealthMetricController.DELETE /api/v1/health-metrics/{metricId}` | — | ADR-HEALTH-001 |
| PRE-3 / BR-RBAC | Business Rule | Chỉ Mother sở hữu journey chứa metric mới xóa được | `HealthMetricServiceImpl.deleteMetric()` | — | — |
| BR-PRIVACY | Business Rule | Xóa phải là soft-delete (giữ audit trail, không mất dữ liệu vật lý) | `MaternalHealthMetric.status = DELETED` (single-column UPDATE, tái sử dụng `MetricStatus` enum) | PDPA | ADR-HEALTH-001 (kế thừa UC187) |
| E1 (Exceptions) | Exception Flow | Access denied khi không sở hữu record | `HealthMetricController` (403, `METRIC-003`) | — | — |
| E2 (Exceptions) | Exception Flow | Metric không tồn tại hoặc đã bị xóa trước đó → 404, idempotent theo semantics của UC187 | `HealthMetricServiceImpl.deleteMetric()` (`METRIC-001`) | — | — |
| POST-3 | Postcondition | Sensitive action ghi log audit | `MaternalHealthMetricDeleted` event → `AuditService` (nếu có consumer đăng ký) | — | — |

---

## 3. Architecture Decision Records (ADR)

### ADR-HEALTH-004 — Soft-delete pattern for `maternal_health_metrics` via reused `status` column

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `AI Agent — Technical Architect (TV2-Bách delegation)` |
| **Date** | `2026-07-03` |
| **Supersedes** | — |

#### Bối cảnh (Context)
UC187 đã thêm cột `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` vào `maternal_health_metrics` (migration `V20260627100200`) và enum `MetricStatus { ACTIVE, DELETED }`, cùng repository method `findByIdAndStatus(id, ACTIVE)`. UC188 cần một cơ chế xóa an toàn, có thể audit, không phá vỡ FK từ các bảng khác (không có FK trỏ vào `metric_id` trong schema hiện tại — an toàn để soft-delete mà không cascade).

#### Các phương án đã xem xét (Options Considered)

| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A | Hard DELETE row khỏi bảng | Đơn giản, không cần thêm logic | Mất audit trail, vi phạm BR-PRIVACY, không thể khôi phục nếu Mother nhấn nhầm |
| B | Soft-delete: UPDATE `status = 'DELETED'` (tái sử dụng cột đã có từ UC187) | Nhất quán với UC187, không cần migration mới, giữ nguyên audit/retention, đảo ngược được về mặt kỹ thuật (DB level) | Query mặc định phải luôn filter `status = ACTIVE` (rủi ro leak nếu quên filter) |

#### Quyết định (Decision)
Chọn **Phương án B** vì nhất quán tuyệt đối với pattern đã Accepted ở UC187, không cần Flyway migration mới, và tuân thủ BR-PRIVACY (giữ dữ liệu cho audit/retention theo chính sách CareBridge).

#### Hệ quả (Consequences)

**Tích cực:**
- Tái sử dụng 100% entity/enum/repository hiện có — thay đổi code tối thiểu (Delivery Rules: "smallest scoped change").
- Nhất quán pattern xuyên suốt UC187/UC188/UC190/UC191 (cùng convention `status: ACTIVE|DELETED`).

**Tiêu cực / Trade-offs:**
- Mọi query đọc list/detail phải nhớ filter `status = ACTIVE`; giảm thiểu bằng cách thêm repository method chuyên biệt (`findByIdAndStatus`) thay vì `findById` trần.

**Compliance Impact:**
- PDPA: dữ liệu sức khỏe không bị xóa vật lý ngay, đáp ứng yêu cầu lưu trữ tối thiểu cho audit; nếu Mother yêu cầu xóa vĩnh viễn theo quyền PDPA, cần một UC riêng (không thuộc phạm vi UC188).

### ADR-HEALTH-005 — Idempotent delete: repeated DELETE on already-DELETED metric

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `AI Agent — Technical Architect` |
| **Date** | `2026-07-03` |

#### Bối cảnh (Context)
UC187 đã định nghĩa "DELETED metric trả về 404" cho GET. UC188 cần quyết định hành vi khi Mother gọi DELETE hai lần trên cùng 1 metric (network retry, double-tap UI).

#### Các phương án đã xem xét (Options Considered)

| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A | DELETE lần 2 trả 404 (giống GET) | Nhất quán với UC187 semantics — "not found" cho mọi non-ACTIVE state | Client phải xử lý 404 như "already deleted" |
| B | DELETE lần 2 trả 200 (idempotent no-op) | An toàn hơn cho retry logic phía mobile | Không nhất quán với UC187's "DELETED = not found" |

#### Quyết định (Decision)
Chọn **Phương án A** — trả `404 METRIC-001` khi metric không ACTIVE (đã DELETED hoặc không tồn tại), nhất quán tuyệt đối với `findByIdAndStatus(id, ACTIVE)` pattern của UC187. Mobile client coi 404 sau delete là thành công về mặt UX (idempotent ở tầng client).

#### Hệ quả (Consequences)

**Tích cực:** Một nguồn sự thật duy nhất cho "metric có truy cập được không" — dùng chung `findByIdAndStatus`.

**Tiêu cực / Trade-offs:** Client phải biết diễn giải 404 sau DELETE là "đã xóa rồi", không phải lỗi thật — ghi rõ trong API spec §9.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA | Measurement Method | Compliance Basis |
|----------|-------------|------------|---------------------|------------------|
| Latency | API response (p99) | `< 300ms` | k6 load test | — |
| Availability | Uptime (monthly) | `99.9%` | Uptime monitor | — |
| Throughput | Concurrent requests | `200 req/s` (thao tác occasional-frequency) | Load test | — |

### 4.2. Data Integrity & Retention

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Durability | Zero record loss (soft-delete only) | RPO = 0, row luôn tồn tại vật lý | SQL query `SELECT * WHERE metric_id = X` sau delete | GDPR Art. 5.1(f) / PDPA |
| Retention | Metric đã xóa vẫn giữ trong DB (không hard-delete) | Vô thời hạn cho đến khi có UC xóa vĩnh viễn riêng | DB inspection | PDPA |
| Consistency | `status` transition chỉ 1 chiều `ACTIVE → DELETED` trong phạm vi UC188 | 100% | Repository test | — |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Access control | Chỉ owner của journey chứa metric mới DELETE được (IDOR guard) | 100% requests từ non-owner → 403 | Security test §13.3 | GDPR Art. 25 |
| Encryption in transit | Toàn bộ endpoint | TLS 1.3+ | SSL Labs scan | GDPR Art. 32 |

### 4.4. Scalability & Capacity Planning

> Tải dự kiến: thao tác `Occasional` (theo SRS Frequency of Use) — không yêu cầu scale đặc biệt. Dùng chung connection pool/service layer với UC187.

---

## 5. Static Modeling (Mô hình Tĩnh)

### 5.1. Class Diagram (PlantUML)

```plantuml
@startuml MaternalHealthMetric_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

' === ENTITIES (existing, reused from UC187) ===
class MaternalHealthMetric {
  + id: UUID
  + journeyId: UUID
  + metricType: MetricType
  + valueNumeric: BigDecimal
  + valueSecondary: BigDecimal
  + unit: String
  + measuredAt: Instant
  + sourceType: DataSource
  + note: String
  + status: MetricStatus
  + createdAt: Instant
  + updatedAt: Instant
}

enum MetricStatus <<enum>> {
  ACTIVE
  DELETED
}

' === SERVICES ===
interface IHealthMetricService <<interface>> {
  + getMetricDetail(metricId: UUID, callerId: UUID): MetricDetailResponse
  + deleteMetric(metricId: UUID, callerId: UUID): void
}

class HealthMetricServiceImpl implements IHealthMetricService {
  - metricRepository: MaternalHealthMetricRepository
  - journeyRepository: MotherJourneyRepository
  - eventPublisher: ApplicationEventPublisher
  + getMetricDetail(metricId: UUID, callerId: UUID): MetricDetailResponse
  + deleteMetric(metricId: UUID, callerId: UUID): void
}

' === REPOSITORIES (existing, reused) ===
interface MaternalHealthMetricRepository <<interface>> {
  + findByIdAndStatus(id: UUID, status: MetricStatus): Optional<MaternalHealthMetric>
  + save(entity: MaternalHealthMetric): MaternalHealthMetric
}

' === RELATIONSHIPS ===
HealthMetricServiceImpl --> MaternalHealthMetricRepository : uses
HealthMetricServiceImpl --> MotherJourneyRepository : uses (ownership check)
MaternalHealthMetric *-- MetricStatus : has

@enduml
```

### 5.2. Data Structure (Flyway SQL Migration)

> **No new migration required.** `maternal_health_metrics.status` column already exists (migration `V20260627100200__add_maternal_metric_status.sql`, verified in `V1__init_schema.sql` context). UC188 performs a single-column `UPDATE ... SET status = 'DELETED'` via JPA `save()` — no DDL change.

```sql
-- Reference only — schema already applied by UC187's migration:
-- ALTER TABLE maternal_health_metrics ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
-- CREATE INDEX IF NOT EXISTS idx_mhm_status ON maternal_health_metrics(status);
```

> **Quy tắc đặt tên:** Không có DDL mới cho UC188.

---

## 6. Dynamic Modeling (Mô hình Động)

### 6.1. Sequence Diagram — Happy Path (PlantUML)

```plantuml
@startuml DeleteMaternalHealthMetric_SequenceDiagram_HappyPath
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam sequenceParticipant underline
skinparam backgroundColor #FAFAFA

actor       "Mother"               as Client
participant "HealthMetricController" as Controller
participant "HealthMetricServiceImpl" as Service
participant "MaternalHealthMetricRepository" as MetricRepo
participant "MotherJourneyRepository" as JourneyRepo
database    "PostgreSQL"           as DB
participant "ApplicationEventPublisher" as Audit

Client -> Controller : DELETE /api/v1/health-metrics/{metricId}\nAuthorization: Bearer JWT
activate Controller

Controller -> Controller : SecurityUtils.requireCurrentUserId(principal)
Controller -> Service : deleteMetric(metricId, callerId)
activate Service

Service -> MetricRepo : findByIdAndStatus(metricId, ACTIVE)
activate MetricRepo
MetricRepo -> DB : SELECT * FROM maternal_health_metrics WHERE metric_id=? AND status='ACTIVE'
DB --> MetricRepo : row
MetricRepo --> Service : Optional<MaternalHealthMetric>
deactivate MetricRepo

Service -> JourneyRepo : findById(metric.journeyId)
activate JourneyRepo
JourneyRepo -> DB : SELECT * FROM mother_journeys WHERE journey_id=?
DB --> JourneyRepo : row
JourneyRepo --> Service : Optional<MotherJourney>
deactivate JourneyRepo

Service -> Service : verify journey.ownerUserId == callerId (C1)
Service -> Service : metric.setStatus(DELETED)
Service -> MetricRepo : save(metric)
activate MetricRepo
MetricRepo -> DB : UPDATE maternal_health_metrics SET status='DELETED', updated_at=now() WHERE metric_id=?
DB --> MetricRepo : ok
deactivate MetricRepo

Service -> Audit : publishEvent(MaternalHealthMetricDeleted)
Service --> Controller : void
deactivate Service

Controller --> Client : HTTP 204 No Content
deactivate Controller

@enduml
```

### 6.2. Sequence Diagram — Error Path (PlantUML)

```plantuml
@startuml DeleteMaternalHealthMetric_SequenceDiagram_ErrorPath
skinparam backgroundColor #FAFAFA

actor "Mother" as Client
participant "HealthMetricController" as Controller
participant "HealthMetricServiceImpl" as Service
participant "MaternalHealthMetricRepository" as MetricRepo
participant "MotherJourneyRepository" as JourneyRepo

== Case 1: Metric not found or already DELETED ==
Client -> Controller : DELETE /api/v1/health-metrics/{metricId}
Controller -> Service : deleteMetric(metricId, callerId)
Service -> MetricRepo : findByIdAndStatus(metricId, ACTIVE)
MetricRepo --> Service : Optional.empty()
Service -> Service : throw BusinessException(404, METRIC-001)
Service --> Controller : BusinessException
Controller --> Client : HTTP 404 {code: METRIC-001}

== Case 2: Not owner (IDOR attempt) ==
Client -> Controller : DELETE /api/v1/health-metrics/{metricId}
Controller -> Service : deleteMetric(metricId, callerId)
Service -> MetricRepo : findByIdAndStatus(metricId, ACTIVE)
MetricRepo --> Service : Optional<MaternalHealthMetric>
Service -> JourneyRepo : findById(metric.journeyId)
JourneyRepo --> Service : Optional<MotherJourney> (ownerUserId != callerId)
Service -> Service : throw BusinessException(403, METRIC-003)
Service --> Controller : BusinessException
Controller --> Client : HTTP 403 {code: METRIC-003}

@enduml
```

### 6.3. State Machine

```plantuml
@startuml MaternalHealthMetric_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : Metric created (UC not in this scope)

ACTIVE --> DELETED : UC188 DELETE /health-metrics/{id}\n(owner-only, single-column UPDATE)

note right of DELETED
  Invariant: Không có transition nào
  từ DELETED quay lại ACTIVE trong phạm vi UC188
  (restore = Open item, ngoài phạm vi)
end note

@enduml
```

> **⚠️ Invariant bất biến:** `DELETED` là trạng thái chung cuộc trong phạm vi UC188. Không có endpoint restore. Row vật lý không bao giờ bị xóa (append-only ở tầng storage).

---

## 7. Domain Event Catalog

### 7.1. Events Published (Phát ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|---------------|----------------|--------|
| `MaternalHealthMetricDeleted` | Soft-delete thành công (status → DELETED) | `HealthMetricServiceImpl` | Audit log consumer (nếu có), `UC69 ViewDeviceDataTrend` cache invalidation (nếu có) | `MaternalHealthMetricDeleted.java` | No (đồng bộ trong cùng transaction, Spring `ApplicationEventPublisher`) |

### 7.2. Events Consumed (Tiêu thụ)

| Event Name | Source | Handler | Action thực hiện |
|------------|--------|---------|------------------|
| _(none)_ | — | — | UC188 không tiêu thụ event từ module khác |

### 7.3. Payload Schema

```java
// MaternalHealthMetricDeleted.java
public record MaternalHealthMetricDeleted(
    UUID    eventId,          // UUID.randomUUID()
    String  eventType,        // "MaternalHealthMetricDeleted"
    Instant occurredAt,       // Instant.now()
    String  version,          // "1.0"
    Payload payload,
    Metadata metadata
) {

    public record Payload(
        UUID metricId,
        UUID journeyId,
        String metricType      // MetricType.name() — no measured value included (BR-PRIVACY minimum-necessary)
    ) {}

    public record Metadata(
        UUID   correlationId,
        String causedBy        // callerId (Mother userId)
    ) {}
}
```

---

## 8. Interface Specification (Đặc tả Giao diện)

### 8.1. Service Interface

```java
// I HealthMetricService.java — extended, existing interface
// @version 1.1
public interface IHealthMetricService {

    /** @throws BusinessException (METRIC-001/404) if not found or already deleted
     *  @throws BusinessException (METRIC-002/404) if parent journey missing
     *  @throws BusinessException (METRIC-003/403) if not owner */
    MetricDetailResponse getMetricDetail(UUID metricId, UUID callerId);

    /**
     * UC188: Soft-deletes a Mother-entered metric (status ACTIVE -> DELETED).
     * @throws BusinessException (METRIC-001/404) if not found or already deleted
     * @throws BusinessException (METRIC-002/404) if parent journey missing
     * @throws BusinessException (METRIC-003/403) if caller is not the journey owner
     */
    void deleteMetric(UUID metricId, UUID callerId);
}
```

### 8.2. Repository Interface

```java
// MaternalHealthMetricRepository.java — existing, no change needed
// @version 1.0
public interface MaternalHealthMetricRepository extends JpaRepository<MaternalHealthMetric, UUID> {

    Optional<MaternalHealthMetric> findByIdAndStatus(UUID id, MetricStatus status);
    // save() inherited from JpaRepository — used for status transition (no new method required)
}
```

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `DELETE` | `/api/v1/health-metrics/{metricId}` | JWT Bearer | `MOTHER` | 60/min | Yes (2nd call → 404, treated as already-deleted at client) |

### 9.2. Request / Response Schemas

#### `DELETE /api/v1/health-metrics/{metricId}` — Soft-delete metric

**Request Body:** none (path param `metricId` only)

**Response — 204 No Content (Happy Path):**
```
(empty body)
```

**Response — 404 Not Found (already deleted / never existed):**
```json
{
  "error": {
    "code": "METRIC-001",
    "message": "Metric not found or deleted: {metricId}"
  }
}
```

**Response — 403 Forbidden (not owner — IDOR guard):**
```json
{
  "error": {
    "code": "METRIC-003",
    "message": "Access denied to health metric"
  }
}
```

---

## 10. Bảng mã lỗi (Error Codes)

> Tiền tố `METRIC-` đã được thiết lập bởi UC187 — UC188 tái sử dụng mã lỗi hiện có, không tạo mã mới.

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|--------------|--------------|-------------------|
| `METRIC-001` | 404 | Metric not found or deleted | Không tìm thấy hoặc đã bị xóa | `findByIdAndStatus(id, ACTIVE)` trả empty (không tồn tại hoặc đã DELETED) |
| `METRIC-002` | 404 | Parent journey not found | Không tìm thấy hành trình liên kết | `journeyRepository.findById()` trả empty (data integrity issue) |
| `METRIC-003` | 403 | Access denied to health metric | Không có quyền truy cập chỉ số sức khỏe | `journey.ownerUserId != callerId` |

---

## 11. Quy trình Triển khai (Step-by-Step)

### 11.1. Prerequisites

- [ ] ADR-HEALTH-004, ADR-HEALTH-005 đã Accepted (xem §3)
- [ ] DPO sign-off pending (module PII sức khỏe)
- [ ] TDS + Test-Spec approved bởi user (theo `implement-flow.md`)
- [ ] UC187 code (`MaternalHealthMetric`, `MetricStatus`, `MaternalHealthMetricRepository`) đã tồn tại và pass test

### 11.2. Pre-Migration Checklist

- [ ] Không cần migration mới — N/A cho UC188

### 11.3. Implementation Steps

#### Chặng 1 — Extend Service Interface + Impl

```java
// IHealthMetricService.java — add deleteMetric() signature (§8.1)
// HealthMetricServiceImpl.java — implement deleteMetric():
//   1. findByIdAndStatus(metricId, ACTIVE) -> orElseThrow(METRIC-001)
//   2. journeyRepository.findById(journeyId) -> orElseThrow(METRIC-002)
//   3. verify journey.ownerUserId == callerId -> else throw METRIC-003
//   4. metric.setStatus(DELETED); metricRepository.save(metric)
//   5. eventPublisher.publishEvent(new MaternalHealthMetricDeleted(...))
```

Class phải đổi `@Transactional(readOnly = true)` cấp class thành method-level override `@Transactional` (không readOnly) cho `deleteMetric()`, giữ nguyên `readOnly = true` cho `getMetricDetail()`.

#### Chặng 2 — Extend Controller

```java
// HealthMetricController.java — add:
@DeleteMapping("/{metricId}")
@PreAuthorize("hasRole('MOTHER')")
public ResponseEntity<Void> deleteMetric(@PathVariable UUID metricId, Principal principal) {
    var callerId = SecurityUtils.requireCurrentUserId(principal);
    healthMetricService.deleteMetric(metricId, callerId);
    return ResponseEntity.noContent().build();
}
```

#### Chặng 3 — Verification sau deploy

```bash
curl -X DELETE https://[host]/api/v1/health-metrics/[metricId] \
  -H "Authorization: Bearer [JWT_TOKEN]"
# Expected: 204 No Content
curl -X GET https://[host]/api/v1/health-metrics/[metricId] \
  -H "Authorization: Bearer [JWT_TOKEN]"
# Expected: 404 METRIC-001 (confirms soft-delete visible to UC187's read path)
```

### 11.4. Deployment Checklist

- [ ] `./mvnw test` xanh
- [ ] Health check 200
- [ ] Mobile `HealthMetricService.deleteMetric()` (đã tồn tại — `05_Development/CareBridgeMobileApp/lib/features/healthRecords/services/health_metric_service.dart:13`) đã trỏ đúng endpoint mới bật
- [ ] Audit log sinh đúng `MaternalHealthMetricDeleted`

---

## 12. Rollback & Incident Runbook

### 12.1. Điều kiện kích hoạt Rollback

| Điều kiện | Ngưỡng | Người quyết định |
|-----------|--------|------------------|
| Error rate tăng đột biến | > 5% trong 5 phút | On-call Engineer |
| Metric bị xóa nhầm hàng loạt (không do Mother chủ động) | Bất kỳ case nào | Tech Lead + DPO |

### 12.2. Rollback Procedure

```bash
# Không có migration mới — rollback chỉ cần revert code deploy
kubectl rollout undo deployment/carebridge-api
kubectl rollout status deployment/carebridge-api

# Nếu cần khôi phục dữ liệu (data-level, không qua API — chỉ dùng khi có sự cố):
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "UPDATE maternal_health_metrics SET status='ACTIVE' WHERE metric_id IN (...) AND updated_at > '[incident_start_ts]';"
```

### 12.3. Notification Protocol

| Thời điểm | Người nhận | Kênh | Template |
|-----------|------------|------|----------|
| Ngay khi phát hiện | On-call team | Slack `#incident` | "🚨 UC188 incident: [mô tả]" |
| Trong 30 phút | DPO | Email | Bắt buộc — module PII |

### 12.4. Post-Incident Review (PIR)

> Bắt buộc hoàn thành PIR trong 48 giờ nếu có incident liên quan đến xóa nhầm dữ liệu sức khỏe.

---

## 13. Kịch bản Kiểm thử Chi tiết

> Chi tiết đầy đủ nằm ở `UC188_DeleteMaternalHealthMetric_Test-Spec.md`. Tóm tắt scope:

- Unit: `HealthMetricServiceImpl.deleteMetric()` — happy path, not-found, not-owner, already-deleted (idempotency).
- Integration: full DELETE flow qua Testcontainers PostgreSQL, verify row vẫn tồn tại vật lý với `status='DELETED'`.
- Security/E2E: IDOR — Mother B cố xóa metric của Mother A → 403; unauthenticated → 401.

---

## 14. Phương pháp Xác minh

### 14.1. Database Inspection

```sql
-- Verify soft-delete: row vẫn tồn tại, chỉ đổi status
SELECT metric_id, status, updated_at
FROM maternal_health_metrics
WHERE metric_id = '[uuid]';
-- Expected: status = 'DELETED', row vẫn tồn tại (không bị xóa vật lý)
```

### 14.2. Log / Audit Verification

```bash
kubectl logs -l app=carebridge-api | grep '"eventType":"MaternalHealthMetricDeleted"' | head -5
```

### 14.3. Tool-based Verification

```bash
echo "[JWT_TOKEN]" | cut -d'.' -f2 | base64 -d | jq .
```

---

## 15. Mẫu thử thực tế (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X DELETE https://[host]/api/v1/health-metrics/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer [JWT_TOKEN]"
```

**Expected Response (204):** empty body

### 15.2. Error Paths

```bash
# Non-owner attempts delete → 403
curl -X DELETE https://[host]/api/v1/health-metrics/[other-mother-metric-id] \
  -H "Authorization: Bearer [JWT_TOKEN_MOTHER_B]"
```

**Expected Response (403):**
```json
{ "error": { "code": "METRIC-003", "message": "Access denied to health metric" } }
```

```bash
# No JWT → 401
curl -X DELETE https://[host]/api/v1/health-metrics/[id]
```

**Expected Response (401):**
```json
{ "error": { "code": "IAM-001", "message": "Authentication required" } }
```

---

## 16. Bảng tổng hợp phân quyền (Authorization Matrix)

| Endpoint | `GUEST` | `MOTHER` (own) | `MOTHER` (other's) | `FAMILY` | `SYSTEM_ADMIN` |
|----------|---------|-----------------|---------------------|----------|----------------|
| `GET /api/v1/health-metrics/{id}` | ❌ | ✅ Own | ❌ 403 | ❌ | ❌ (out of scope UC187/188) |
| `DELETE /api/v1/health-metrics/{id}` | ❌ | ✅ Own | ❌ 403 | ❌ | ❌ |

**Chú thích:**
- ✅ = Được phép
- ❌ = Bị từ chối (403) hoặc không route tới (401 nếu unauthenticated)
- `Own` = Chỉ được phép với metric thuộc journey của chính Mother đó (`journey.owner_user_id == callerId`)

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source (ADR/BR) | Last Verified |
|---|-----------|-----------------|---------------|
| C1 | Ownership resolved via `metric.journeyId -> MotherJourney.ownerUserId == callerId`. KHÔNG có `owner_user_id` trực tiếp trên `maternal_health_metrics`. | ADR-HEALTH-004, BR-RBAC | 2026-07-03 |
| C2 | Delete PHẢI là soft-delete (`status = DELETED`) — KHÔNG được gọi `repository.delete()` hoặc `deleteById()`. | ADR-HEALTH-004, BR-PRIVACY | 2026-07-03 |
| C3 | Dùng `MaternalHealthMetricRepository.findByIdAndStatus(id, ACTIVE)` — không dùng `findById()` trần (sẽ bỏ sót đã-DELETED nên phải filter tường minh). | UC187 pattern | 2026-07-03 |
| C4 | Identity lấy từ `SecurityUtils.requireCurrentUserId(principal)` — không tự parse JWT trong Controller. | UC187 pattern | 2026-07-03 |
| C5 | Controller chỉ validate + map; toàn bộ ownership/soft-delete logic nằm trong `HealthMetricServiceImpl`. | CLAUDE.md Architecture rules | 2026-07-03 |

### 17.2 Constraint Injection Block (Copy-Paste vào AI Prompt)

```
[CONSTRAINT BLOCK — Module: Delete Maternal Health Metric (UC188)]
Theo TDS CB-HEALTH-IMP-004 và các ADR liên quan:

1. Ownership qua metric.journeyId -> MotherJourney.ownerUserId == callerId (KHÔNG có owner_user_id trực tiếp trên bảng metric).
2. Delete là soft-delete (status=DELETED) — KHÔNG hard-delete.
3. Dùng findByIdAndStatus(id, ACTIVE) — không findById() trần.
4. Identity từ SecurityUtils.requireCurrentUserId(principal).
5. Controller chỉ validate/map; logic nằm ở Service.

[CONTEXT BLOCK]
- Bounded Context: health
- Data Classification: Sensitive-PII
- Compliance: PDPA / Luật 91/2025
- Existing interfaces: §8 Service Interface + §8.2 Repository Interface
- Error codes: §10 Error Codes Table (METRIC-001/002/003 — reused from UC187)
- Auth matrix: §16 Authorization Matrix

[TASK BLOCK]
Implement deleteMetric() thỏa mãn constraints trên.
Output phải tuân thủ §8 Interface Specification.
Tests phải cover §13 Test Scenarios (xem Test-Spec).
```

### 17.3 Constraint Quality Checklist

- [x] Mỗi constraint traceable về ADR hoặc BR cụ thể
- [x] Không có constraint generic
- [x] Mỗi constraint có `Last Verified` date ≤ 2 sprints
- [x] Constraint block có ≥ 3 constraints cụ thể
- [x] Constraint block reference §8 Interface
- [x] Constraint block reference §16 Auth Matrix

### 17.4 Anti-Pattern Detection (cho AI-Generated Code từ Block này)

| AP-ID | Anti-Pattern | Dấu hiệu | Hành động |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code không match C1-C5 | Reject — inject lại constraints |
| AP-AI-003 | Implicit Decision | Code assume `owner_user_id` tồn tại trực tiếp trên `maternal_health_metrics` (không có trong schema) | Reject — dùng journey join |
| AP-AI-005 | Hallucinated Contract | Code gọi `metricRepository.deleteById()` (vi phạm C2) | Reject — dùng `save()` với status=DELETED |

---

## PHỤ LỤC

### A. Glossary (Thuật ngữ)

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| Soft-delete | Đánh dấu record là đã xóa qua cột `status` thay vì xóa vật lý khỏi DB |
| IDOR | Insecure Direct Object Reference — truy cập resource của người khác qua đoán ID |
| PII | Personally Identifiable Information |

### B. Tài liệu tham chiếu

| Document | Link / Path |
|----------|-------------|
| SRS §3.3.11.2 | `02_Requirements/SRS/3_Functional_Specification.md` (dòng 4047-4066) |
| UC187 TDS/code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/` |
| Schema baseline | `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`, `V20260627100200__add_maternal_metric_status.sql` |
| Task allocation | `04_Implement/implement_artifacts/function-spec-task-allocation.md` (dòng 670-694) |

---

*EDS v2.1 — CASE 2.0 constraints applied. Status: Draft — pending user review/approval per `.claude/rules/implement-flow.md`.*
