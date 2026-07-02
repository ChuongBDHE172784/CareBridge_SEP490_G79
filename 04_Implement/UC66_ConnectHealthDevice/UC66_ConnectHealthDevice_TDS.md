# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# UC66 — Connect Health Device

| Field | Value |
|-------|-------|
| **Document ID** | `CB-DEVICE-IMP-001` |
| **Version** | `1.0` |
| **Date** | `2026-07-01` |
| **Status** | `Draft` |
| **Document Owner** | `TV2-Bách` |
| **Author** | `AI Agent — Technical Architect` |
| **Reviewed by** | `[ ] Pending` |
| **DPO Sign-off** | `[ ] Pending` *(bắt buộc — module xử lý health/wearable data + consent)* |
| **Approved by** | `[ ] Pending` |
| **Last Review** | `2026-07-01` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ. Mọi thay đổi phải ghi vào bảng này.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Technical Architect | Tạo tài liệu lần đầu — TDS cho UC66 Connect Health Device (Draft) |

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

> UC66 cho phép Mother kết nối một wearable/health platform (smartwatch, health app) với CareBridge sau khi cấp consent tường minh. Đây là bước khởi đầu của vòng đời "Device Sync" chung cho UC66 (Connect) → UC67 (Import/Sync data) → UC68 (Disconnect) → UC69 (View Trend). Bốn UC này chia sẻ một entity trạng thái kết nối duy nhất: `DeviceConnection`.

| Field | Value |
|-------|-------|
| **Module Name** | `Connect Health Device` |
| **Bounded Context** | `health` (sub-package `health.device`) |
| **Data Classification** | `Sensitive-PII` *(liên kết thiết bị sức khỏe cá nhân + health data provenance)* |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT)`, `consent` module (`ConsentGrant`, `ConsentService`), `carejourney` (`mother_journeys`) |
| **Downstream Consumers** | `UC67 ImportDeviceDataManually`, `UC68 DisconnectHealthDevice`, `UC69 ViewDeviceDataTrend`, `health.MaternalHealthMetric` (provenance link) |

**Nguồn gốc & phạm vi:**
- Function spec: `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.43` (dòng 2662-2679), UC-66.
- Task allocation: `04_Implement/implement_artifacts/function-spec-task-allocation.md` dòng 676-677 — owner TV2-Bách, sprint "Device Sync And Care Edge Cases".
- Primary Actor: Mother. Secondary Actor: Smartwatch/Wearable Device. Platform: Mobile App (Health module). Priority: High.
- **In-scope:** Khởi tạo kết nối thiết bị (chọn loại/nguồn thiết bị), capture consent cho việc truy cập dữ liệu sức khỏe từ thiết bị, lưu trạng thái kết nối (`CONNECTED`), phát sự kiện `DeviceConnected`.
- **Out-of-scope:** Đồng bộ dữ liệu thực từ thiết bị theo thời gian thực (real-time sync qua SDK) — đây là `3.1.2.4 Sync Health Device Data` (task riêng, KHÔNG thuộc 4 UC này); nhập dữ liệu thủ công (UC67); ngắt kết nối (UC68); xem xu hướng (UC69).
- **Preconditions/Postconditions:** Theo bảng UC-66 gốc (PRE-1..4, POST-1..3) — xem §2 Traceability.

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại (BR/ADR/US) | Mô tả yêu cầu | Thành phần Code | Compliance Target | ADR liên quan |
|----------------|------------------|---------------|-----------------|-------------------|---------------|
| UC-66 (SRS §3.3.1.43) | User Story | Mother kết nối wearable/health platform sau khi consent | `DeviceConnectionController.POST /api/v1/health/devices/connections` | — | ADR-DEVICE-001 |
| PRE-3 / BR-RBAC | Business Rule | Chỉ actor đã xác thực với role phù hợp (MOTHER) mới connect | `DeviceConnectionService.connect()` + `@PreAuthorize` | — | — |
| BR-PRIVACY | Business Rule | Kết nối thiết bị bắt buộc capture consent trước khi lưu trạng thái CONNECTED | `ConsentService.grant()` (reuse) + `DeviceConnectionService` | PDPA / Luật 91/2025 | ADR-DEVICE-002 |
| BR-CONSULTATION | Business Rule | Vòng đời kết nối phải auditable (trạng thái + audit trail) | `DeviceConnection.status`, `created_at/updated_at`, `DeviceConnected` event | — | ADR-DEVICE-001 |
| POST-3 | Postcondition | Sensitive actions (connect) phải được ghi nhận cho audit | `DeviceConnected` event + `created_by` | PDPA | ADR-DEVICE-003 |
| E1 (Exceptions) | Exception Flow | Access denied khi actor không auth hoặc không đúng ownership | `DeviceConnectionController` (403) | — | — |
| E2 (Exceptions) | Exception Flow | Dữ liệu thiếu/invalid (deviceType/deviceName) bị reject | `ConnectDeviceRequest` validation | — | — |
| ADR-DEVICE-001 | Decision | State machine dùng chung cho UC66/UC68: `NOT_CONNECTED → CONNECTED → DISCONNECTED → CONNECTED...` | `DeviceConnection.status` enum `DeviceConnectionStatus` | — | — |
| ADR-DEVICE-002 | Decision | Consent bắt buộc trước khi transition sang CONNECTED; consent record tái sử dụng `ConsentGrant` (`dataType=HEALTH_RECORD`, `purpose=SHARE`) | `DeviceConnectionService.connect()` | PDPA Art. (Luật 91/2025 Đ.13) | — |
| ADR-DEVICE-003 | Decision | Không hard-delete kết nối — append-only lifecycle, dùng trạng thái + `disconnected_at` | `DeviceConnection` (không có phương thức `delete()`) | GDPR-equivalent Art. 5.1(e) | — |

---

## 3. Architecture Decision Records (ADR)

### ADR-DEVICE-001 — Device Connection Lifecycle State Machine (dùng chung UC66/UC68)

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `AI Agent — Technical Architect` |
| **Date** | `2026-07-01` |
| **Supersedes** | `—` |

#### Bối cảnh (Context)
UC66 (Connect) và UC68 (Disconnect) thao tác trên cùng một thực thể kết nối thiết bị. Cần một mô hình trạng thái nhất quán để tránh có 2 bảng riêng biệt hoặc state rời rạc giữa 2 UC.

#### Các phương án đã xem xét (Options Considered)

| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A | Một bảng `device_connections` với enum `status` (`NOT_CONNECTED` không cần lưu — record chỉ tồn tại khi có ý định kết nối; `CONNECTED`, `DISCONNECTED`) | Đơn giản, 1 nguồn sự thật, dễ audit lịch sử | Cần đảm bảo chỉ 1 record `CONNECTED` active/thiết bị/user tại một thời điểm |
| B | Xóa record khi disconnect, tạo mới khi connect lại | Đơn giản hơn ở query | Vi phạm nguyên tắc append-only/audit (POST-3), mất lịch sử kết nối |

#### Quyết định (Decision)
Chọn **Phương án A**. Bảng `device_connections` là append-only theo nghĩa không xóa record — disconnect chỉ set `status = DISCONNECTED` + `disconnected_at`. Kết nối lại tạo **record mới** (không tái sử dụng record cũ) để giữ lịch sử đầy đủ mỗi lần connect/disconnect.

#### Hệ quả (Consequences)

**Tích cực:**
- Lịch sử kết nối/ngắt kết nối được giữ nguyên vẹn cho audit (POST-3, BR-CONSULTATION).
- UC66 và UC68 dùng chung 1 entity, 1 repository, tránh trùng lặp logic.

**Tiêu cực / Trade-offs:**
- Có thể có nhiều record `DISCONNECTED` lịch sử cho cùng 1 (user, deviceType) — cần index + query "current active connection" (`status = 'CONNECTED'`) để tránh nhầm lẫn.

**Compliance Impact:**
- Hỗ trợ yêu cầu audit trail của BR-CONSULTATION và PDPA (chứng minh được thời điểm consent/kết nối).

---

### ADR-DEVICE-002 — Consent Capture Required Before CONNECTED Transition

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `AI Agent — Technical Architect` |
| **Date** | `2026-07-01` |
| **Supersedes** | `—` |

#### Bối cảnh (Context)
BR-PRIVACY yêu cầu dữ liệu sức khỏe/gia đình phải theo "consent, purpose, and minimum-necessary access rules". SRS UC-66 Description ghi rõ: "Connects a wearable or health platform **after user consent**". Codebase đã có `consent` module hoàn chỉnh (`ConsentGrant`, `ConsentService`, `ConsentDataType`, `ConsentPurpose`) — không cần tạo consent model mới.

#### Các phương án đã xem xét (Options Considered)

| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A | Tái sử dụng `ConsentGrant` hiện có: gọi `ConsentService.grant()` với `dataType=HEALTH_RECORD`, `purpose=SHARE` trước khi tạo `DeviceConnection` | Nhất quán với hạ tầng consent hiện có, không nhân bản logic | `ConsentDataType` hiện không có giá trị riêng cho "DEVICE_DATA" — dùng `HEALTH_RECORD` là xấp xỉ gần nhất |
| B | Tạo consent field riêng ngay trong `device_connections` (`consent_given_at`) không qua `ConsentGrant` | Đơn giản, ít phụ thuộc | Trùng lặp mô hình consent, không nhất quán với ADR/consent module hiện có, khó audit tập trung |

#### Quyết định (Decision)
Chọn **kết hợp cả hai**: (1) gọi `ConsentService.grant()` để tạo `ConsentGrant` record chính thức (audit tập trung, nhất quán hệ thống), VÀ (2) lưu thêm `consent_grant_id` (FK) + `consented_at` ngay trên `device_connections` để truy vấn nhanh mà không cần join, tránh N+1 khi hiển thị danh sách kết nối.

#### Hệ quả (Consequences)

**Tích cực:**
- Consent cho device data được audit tập trung qua `consent_grants`, nhất quán với các module khác.
- Query trạng thái kết nối nhanh không cần join bảng consent.

**Tiêu cực / Trade-offs:**
- Cần đồng bộ 2 nơi (`ConsentGrant.revokedAt` và `device_connections.status=DISCONNECTED`) khi disconnect (xem UC68 TDS ADR-DEVICE-004) — rủi ro lệch state nếu không transaction hoá đúng.

**Compliance Impact:**
- Đáp ứng PDPA/Luật 91/2025 yêu cầu ghi nhận thời điểm, phạm vi, và mục đích cấp quyền truy cập dữ liệu sức khỏe.

> **Open Item (RG-4):** `ConsentDataType` chưa có giá trị chuyên biệt `DEVICE_DATA`. TDS này **đề xuất** thêm giá trị mới `DEVICE_DATA` vào enum `ConsentDataType` (thay vì tái sử dụng `HEALTH_RECORD`) để tránh nhầm lẫn phạm vi consent. Đây là thay đổi ảnh hưởng đến module `consent` dùng chung — **cần Tech Lead/DPO xác nhận** trước khi implement. Nếu không được chấp thuận, giữ tạm `HEALTH_RECORD` làm giá trị fallback.

---

### ADR-DEVICE-003 — Wearable SDK / Provider Integration: Deferred to ADR, Mock-first

| Field | Value |
|-------|-------|
| **Status** | `Proposed` *(chưa Accepted — cần quyết định bổ sung)* |
| **Deciders** | `[ ] Pending — Tech Lead / TV2-Bách` |
| **Date** | `2026-07-01` |
| **Supersedes** | `—` |

#### Bối cảnh (Context)
SRS UC-66 chỉ mô tả "Connects a wearable or health platform after user consent" — không chỉ định vendor SDK (Apple HealthKit, Google Fit/Health Connect, Fitbit API, v.v.). Task allocation (`function-spec-task-allocation.md` dòng 698) ghi: "Device sync works through real provider if available, **otherwise manual import plus stable mock remains**." Mobile app hiện chỉ có placeholder `lib/integrations/wearable/.gitkeep` — chưa có implementation nào.

#### Các phương án đã xem xét (Options Considered)

| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A | Mock-first: `DeviceConnection` lưu `deviceType` (enum do người dùng chọn thủ công từ danh sách hỗ trợ), không tích hợp SDK thật ở giai đoạn này | Không block delivery bởi quyết định vendor, phù hợp "manual import plus stable mock" | Không có dữ liệu tự động thật — chỉ đáp ứng UC66 (connect) + UC67 (manual import), KHÔNG đáp ứng `3.1.2.4 Sync Health Device Data` (auto-sync) |
| B | Tích hợp ngay 1 SDK cụ thể (vd Google Health Connect) | Trải nghiệm thật hơn | Không có cơ sở nguồn (SRS không chỉ định), rủi ro invent architecture decision không có approval |

#### Quyết định (Decision)
Chọn **Phương án A** cho phạm vi 4 UC này (UC66/67/68/69). Việc tích hợp SDK thật (native platform channel Flutter, ví dụ `health` package hoặc vendor-specific plugin) thuộc phạm vi `3.1.2.4 Sync Health Device Data` — **KHÔNG thuộc 4 TDS này** và cần ADR riêng khi được giao.

#### Hệ quả (Consequences)

**Tích cực:** Không trì hoãn UC66/67/68/69 vì thiếu quyết định vendor.

**Tiêu cực / Trade-offs:** Mobile "Connect Device" flow ở giai đoạn này thực chất là "đăng ký ý định kết nối + chọn loại thiết bị" chứ chưa pair thật với hardware qua Bluetooth/SDK.

> **Open Item (RG-4/RG-6):** Wearable SDK vendor choice là **Open** — cần quyết định của Tech Lead/Product Owner trước khi implement `3.1.2.4 Sync Health Device Data`. TDS này không tự ý chọn vendor.

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
| Durability | Không mất record kết nối | RPO = 0 | Transaction log | PDPA |
| Retention | `device_connections` + liên kết `consent_grants` | Theo vòng đời tài khoản user | DB policy | PDPA / Luật 91/2025 |
| Consistency | Consent ↔ DeviceConnection status đồng bộ | 100% | Transactional service method | PDPA |

### 4.3. Security

| Category | Requirement | Target | Verification Method | Compliance Basis |
|----------|-------------|--------|---------------------|------------------|
| Encryption in transit | TLS 1.3+ | Tất cả endpoints | SSL Labs scan | PDPA |
| Access control | Role-based, ownership-scoped (chỉ Mother sở hữu record) | Least privilege | Auth Matrix (§16) | BR-RBAC |
| Audit | Mọi connect/disconnect action phát domain event | 100% | Log/event assertion | BR-CONSULTATION |

### 4.4. Scalability & Capacity Planning

> Chưa có số liệu tải cụ thể từ nguồn — **Open**. Giả định tải thấp (1 thao tác connect/user/session), không cần thiết kế đặc biệt cho throughput cao ở giai đoạn này.

---

## 5. Static Modeling (Mô hình Tĩnh)

### 5.1. Class Diagram (PlantUML)

```plantuml
@startuml UC66_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

' === ENTITIES (shared across UC66/67/68/69) ===
class DeviceConnection {
  + id: UUID
  + userId: UUID
  + journeyId: UUID
  + deviceType: DeviceType
  + deviceName: String
  + status: DeviceConnectionStatus
  + consentGrantId: Long
  + consentedAt: Instant
  + connectedAt: Instant
  + disconnectedAt: Instant
  + createdAt: Instant
  + updatedAt: Instant
}

enum DeviceType <<enum>> {
  SMARTWATCH
  FITNESS_BAND
  BLOOD_PRESSURE_MONITOR
  PULSE_OXIMETER
  HEALTH_PLATFORM_APP
  OTHER
}

enum DeviceConnectionStatus <<enum>> {
  CONNECTED
  DISCONNECTED
}

' === SERVICES ===
interface IDeviceConnectionService <<interface>> {
  + connect(request: ConnectDeviceRequest, userId: UUID): DeviceConnectionResponse
  + getActiveConnection(userId: UUID): Optional<DeviceConnectionResponse>
  + listConnections(userId: UUID): List<DeviceConnectionResponse>
}

class DeviceConnectionService implements IDeviceConnectionService {
  - deviceConnectionRepository: IDeviceConnectionRepository
  - consentService: ConsentService
  - eventPublisher: ApplicationEventPublisher
  + connect(request: ConnectDeviceRequest, userId: UUID): DeviceConnectionResponse
  + getActiveConnection(userId: UUID): Optional<DeviceConnectionResponse>
  + listConnections(userId: UUID): List<DeviceConnectionResponse>
}

' === REPOSITORIES ===
interface IDeviceConnectionRepository <<interface>> {
  + findByUserIdAndStatus(userId: UUID, status: DeviceConnectionStatus): List<DeviceConnection>
  + findFirstByUserIdAndDeviceTypeAndStatusOrderByConnectedAtDesc(userId, deviceType, status): Optional<DeviceConnection>
  + save(entity: DeviceConnection): DeviceConnection
}

DeviceConnectionService --> IDeviceConnectionRepository : uses
DeviceConnectionService --> ConsentService : reuses (grant consent)
DeviceConnection *-- DeviceType
DeviceConnection *-- DeviceConnectionStatus

@enduml
```

### 5.2. Data Structure (Flyway SQL Migration)

> **CareBridge rule:** V1__init_schema.sql + approved Flyway migrations là nguồn sự thật chính. Không có bảng `device_connections`, `wearable`, hoặc `device` nào tồn tại trong schema hiện tại (đã xác minh — greenfield). Highest migration hiện tại: `V20260629000002__create_community_answer_likes.sql`.

Tạo file: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260701140000__create_device_connections.sql`

```sql
-- === HEALTH DEVICE: DEVICE CONNECTIONS SCHEMA (shared by UC66/UC67/UC68/UC69) ===

CREATE TABLE device_connections (
  id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id            UUID          NOT NULL,                 -- Mother's user_id (owner)
  journey_id         UUID,                                    -- optional link to mother_journeys.journey_id
  device_type        VARCHAR(40)   NOT NULL,                  -- SMARTWATCH / FITNESS_BAND / BLOOD_PRESSURE_MONITOR / PULSE_OXIMETER / HEALTH_PLATFORM_APP / OTHER
  device_name        VARCHAR(120),                            -- user-provided label, e.g. "Mi Band 8"
  status             VARCHAR(20)   NOT NULL DEFAULT 'CONNECTED', -- CONNECTED / DISCONNECTED
  consent_grant_id   BIGINT,                                  -- FK to consent_grants.id (ADR-DEVICE-002)
  consented_at       TIMESTAMPTZ,
  connected_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  disconnected_at    TIMESTAMPTZ,
  created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_device_conn_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_device_conn_journey FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id),
  CONSTRAINT fk_device_conn_consent FOREIGN KEY (consent_grant_id) REFERENCES consent_grants(id),
  CONSTRAINT chk_device_conn_status CHECK (status IN ('CONNECTED','DISCONNECTED')),
  CONSTRAINT chk_device_conn_type CHECK (device_type IN ('SMARTWATCH','FITNESS_BAND','BLOOD_PRESSURE_MONITOR','PULSE_OXIMETER','HEALTH_PLATFORM_APP','OTHER'))
);

CREATE INDEX idx_device_conn_user_id ON device_connections(user_id);
CREATE INDEX idx_device_conn_user_status ON device_connections(user_id, status);
CREATE INDEX idx_device_conn_journey_id ON device_connections(journey_id);
```

**Quy tắc đặt tên:** snake_case cho toàn bộ column. Version migration: `V20260701140000` (timestamp-based, theo pattern hiện tại của dự án `V20260628130000__...`). Không trùng với version hiện có cao nhất (`V20260629000002`).

**V1__init_schema.sql sync action:** Do dự án đang dùng chiến lược "V1 = baseline import + các V-timestamp sau đó là incremental migrations" (không phải rebuild V1 mỗi lần), **KHÔNG chỉnh sửa `V1__init_schema.sql`**. Migration mới `V20260701140000` là migration độc lập, tuân thủ đúng pattern các migration gần nhất (`V20260629000001/2`, `V20260628130000`, v.v.). Ghi chú này áp dụng thống nhất cho cả 4 TDS (UC66/67/68/69) — không tạo xung đột version giữa chúng (xem bảng tổng hợp version ở cuối §11.3 mỗi TDS).

---

## 6. Dynamic Modeling (Mô hình Động)

### 6.1. Sequence Diagram — Happy Path (PlantUML)

```plantuml
@startuml UC66_SequenceDiagram_HappyPath
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam sequenceParticipant underline
skinparam backgroundColor #FAFAFA

actor       "Mother (Mobile App)" as Client
participant "DeviceConnectionController" as Controller
participant "DeviceConnectionService"    as Service
participant "ConsentService"             as Consent
participant "DeviceConnectionRepository" as Repository
database    "PostgreSQL"                 as DB
participant "EventPublisher"             as Publisher

Client -> Controller : POST /api/v1/health/devices/connections\n{deviceType, deviceName, consentAccepted:true}
activate Controller
Controller -> Controller : Validate ConnectDeviceRequest (@Valid)
Controller -> Service : connect(request, userId)
activate Service

Service -> Consent : grant(userId, dataType=HEALTH_RECORD|DEVICE_DATA, purpose=SHARE)
activate Consent
Consent --> Service : ConsentGrant (id, consentGivenAt)
deactivate Consent

Service -> Repository : save(DeviceConnection{status=CONNECTED, consentGrantId, ...})
activate Repository
Repository -> DB : INSERT INTO device_connections
DB --> Repository : saved row
deactivate Repository

Service -> Publisher : publish(DeviceConnected)
Service --> Controller : DeviceConnectionResponse
deactivate Service

Controller --> Client : HTTP 201\n{id, deviceType, status:"CONNECTED", connectedAt}
deactivate Controller

@enduml
```

### 6.2. Sequence Diagram — Alternative Flow: Already Connected (Reconnect)

```plantuml
@startuml UC66_SequenceDiagram_AltPath_AlreadyConnected
skinparam backgroundColor #FAFAFA
actor "Mother" as Client
participant "DeviceConnectionController" as Controller
participant "DeviceConnectionService" as Service
participant "DeviceConnectionRepository" as Repository

Client -> Controller : POST /api/v1/health/devices/connections\n{deviceType: SMARTWATCH}
activate Controller
Controller -> Service : connect(request, userId)
activate Service
Service -> Repository : findFirstByUserIdAndDeviceTypeAndStatus(userId, SMARTWATCH, CONNECTED)
Repository --> Service : existing active connection found
Service --> Controller : DeviceConnectionResponse (existing, idempotent return)
deactivate Service
Controller --> Client : HTTP 200\n{id, status:"CONNECTED"} (no duplicate row created)
deactivate Controller
@enduml
```

### 6.3. Sequence Diagram — Error Path (PlantUML)

```plantuml
@startuml UC66_SequenceDiagram_ErrorPath
skinparam backgroundColor #FAFAFA
actor "Mother" as Client
participant "DeviceConnectionController" as Controller

Client -> Controller : POST /api/v1/health/devices/connections\n{deviceType: "INVALID_TYPE"}
activate Controller
Controller --> Client : HTTP 400\n{error:{code:"DEVICE-001"}}
deactivate Controller

Client -> Controller : POST .../connections\n{consentAccepted:false}
activate Controller
Controller --> Client : HTTP 400\n{error:{code:"DEVICE-006", message:"Consent required"}}
deactivate Controller
@enduml
```

### 6.4. State Machine — Device Connection Lifecycle (spans UC66 + UC68)

```plantuml
@startuml UC66_UC68_DeviceConnection_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> CONNECTED : UC66 Connect Health Device\n[consent granted]\nINSERT device_connections(status=CONNECTED)

CONNECTED --> DISCONNECTED : UC68 Disconnect Health Device\n[user confirms]\nUPDATE status=DISCONNECTED, disconnected_at=now()

DISCONNECTED --> [*] : (terminal for this record)
CONNECTED --> [*] : (record persists; only status changes, never deleted)

note right of CONNECTED
  Invariant: Tối đa 1 record CONNECTED
  active per (user_id, device_type) tại một thời điểm.
  Reconnect trong khi đang CONNECTED = idempotent
  return (không tạo record mới) — xem §6.2.
end note

note right of DISCONNECTED
  Invariant: DISCONNECTED là trạng thái cuối của record đó.
  Reconnect sau khi disconnect TẠO record MỚI
  (ADR-DEVICE-001) — không revive record cũ.
end note

@enduml
```

> **⚠️ Invariant bất biến:**
> 1. Không bao giờ xóa record `device_connections` (append-only theo trạng thái).
> 2. Tối đa 1 record `CONNECTED` cho mỗi `(user_id, device_type)` tại một thời điểm — enforced ở service layer (kiểm tra trước khi INSERT) vì không có UNIQUE constraint composite do lịch sử cần giữ nhiều record DISCONNECTED.
> 3. Consent phải tồn tại (`consent_grant_id` NOT NULL khi status=CONNECTED) trước khi transition sang CONNECTED.

---

## 7. Domain Event Catalog

### 7.1. Events Published (Phát ra)

| Event Name | Trigger | Publisher | Subscriber(s) | Payload Schema | Async? |
|------------|---------|-----------|---------------|----------------|--------|
| `DeviceConnected` | Kết nối thiết bị thành công (consent granted + record CONNECTED) | `DeviceConnectionService` | `UC69 ViewDeviceDataTrend` (hiển thị badge "connected"), Audit log sink | `DeviceConnected.java` | Yes |

### 7.2. Events Consumed (Tiêu thụ)

> UC66 không consume events từ module khác.

### 7.3. Payload Schema

```java
// DeviceConnected.java
package com.carebridge.backend.health.device.event;

public record DeviceConnected(
    UUID    eventId,
    String  eventType,        // "DeviceConnected"
    Instant occurredAt,
    String  version,          // "1.0"
    Payload payload,
    Metadata metadata
) implements ApplicationEvent {

    public record Payload(
        UUID   deviceConnectionId,
        UUID   userId,
        String deviceType,     // DeviceType enum name
        String deviceName,
        Long   consentGrantId
    ) {}

    public record Metadata(
        UUID   correlationId,
        String causedBy        // userId as string
    ) {}
}
```

---

## 8. Interface Specification (Đặc tả Giao diện)

### 8.1. Service Interface

```java
// ConnectDeviceRequest.java — Input DTO
// @version 1.0
package com.carebridge.backend.health.device.dto;

public class ConnectDeviceRequest {
    @NotNull
    private DeviceType deviceType;      // required — must be a valid DeviceType enum value

    @Size(max = 120)
    private String deviceName;          // optional — user-friendly label

    @NotNull
    @AssertTrue(message = "Consent must be explicitly accepted before connecting a device")
    private Boolean consentAccepted;    // required — must be true (BR-PRIVACY / ADR-DEVICE-002)

    private UUID journeyId;             // optional — link to active mother journey
    // getters / setters
}

// DeviceConnectionResponse.java — Output DTO
public class DeviceConnectionResponse {
    private UUID   id;
    private String deviceType;
    private String deviceName;
    private String status;              // "CONNECTED" | "DISCONNECTED"
    private Instant connectedAt;
    private Instant consentedAt;
    // getters / setters
}

// IDeviceConnectionService.java — Service Contract
// @version 1.0
package com.carebridge.backend.health.device.service;

public interface IDeviceConnectionService {
    /**
     * Connects a wearable/health platform for the authenticated Mother after consent capture.
     * Idempotent: if an active CONNECTED record already exists for the same (userId, deviceType),
     * returns the existing record instead of creating a duplicate.
     * @throws DeviceConnectionException (DEVICE-001) if deviceType invalid
     * @throws DeviceConnectionException (DEVICE-006) if consentAccepted is false
     * @throws AccessDeniedException (DEVICE-004) if caller is not ROLE_MOTHER
     */
    DeviceConnectionResponse connect(ConnectDeviceRequest request, UUID userId);

    /**
     * Returns the active (status=CONNECTED) connections for the given user.
     */
    List<DeviceConnectionResponse> listActiveConnections(UUID userId);
}
```

### 8.2. Repository Interface

```java
// IDeviceConnectionRepository.java
// @version 1.0
package com.carebridge.backend.health.device.repository;

public interface IDeviceConnectionRepository extends JpaRepository<DeviceConnection, UUID> {

    List<DeviceConnection> findByUserIdAndStatus(UUID userId, DeviceConnectionStatus status);

    Optional<DeviceConnection> findFirstByUserIdAndDeviceTypeAndStatusOrderByConnectedAtDesc(
        UUID userId, DeviceType deviceType, DeviceConnectionStatus status);

    // Append-only: no delete() method exposed for this entity (ADR-DEVICE-001/003)
}
```

---

## 9. API Specification

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `POST` | `/api/v1/health/devices/connections` | JWT Bearer | `ROLE_MOTHER` | 30/min | Yes (returns existing active connection) |
| `GET` | `/api/v1/health/devices/connections` | JWT Bearer | `ROLE_MOTHER` | 60/min | Yes |

### 9.2. Request / Response Schemas

#### `POST /api/v1/health/devices/connections` — Connect a device

**Request Body:**
```json
{
  "deviceType": "SMARTWATCH",
  "deviceName": "Mi Band 8",
  "consentAccepted": true,
  "journeyId": "uuid-v4-optional"
}
```

**Response — 201 Created (Happy Path):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "deviceType": "SMARTWATCH",
  "deviceName": "Mi Band 8",
  "status": "CONNECTED",
  "connectedAt": "2026-07-01T08:00:00.000Z",
  "consentedAt": "2026-07-01T08:00:00.000Z"
}
```

**Response — 400 Bad Request (Validation Error):**
```json
{
  "error": {
    "code": "DEVICE-001",
    "message": "Validation failed",
    "details": [
      { "field": "deviceType", "message": "deviceType is required" }
    ]
  }
}
```

**Response — 400 Bad Request (Consent Not Accepted):**
```json
{
  "error": {
    "code": "DEVICE-006",
    "message": "Consent must be accepted before connecting a device"
  }
}
```

#### `GET /api/v1/health/devices/connections` — List active connections

**Response — 200 OK:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "deviceType": "SMARTWATCH",
    "deviceName": "Mi Band 8",
    "status": "CONNECTED",
    "connectedAt": "2026-07-01T08:00:00.000Z"
  }
]
```

---

## 10. Bảng mã lỗi (Error Codes)

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|--------------|--------------|-------------------|
| `DEVICE-001` | 400 | Validation failed | Dữ liệu không hợp lệ | `deviceType` missing/invalid enum value |
| `DEVICE-002` | 409 | Device connection conflict | Xung đột trạng thái kết nối | (reserved — not expected in normal connect flow since idempotent; used if concurrent race detected) |
| `DEVICE-003` | 404 | Device connection not found | Không tìm thấy kết nối | Referenced `journeyId` does not belong to caller |
| `DEVICE-004` | 403 | Insufficient permissions | Không đủ quyền | Caller is not `ROLE_MOTHER`, or accessing another user's connection |
| `DEVICE-005` | 500 | Internal error | Lỗi hệ thống | Unexpected failure (DB, consent service unavailable) |
| `DEVICE-006` | 400 | Consent required | Yêu cầu phải có sự đồng ý | `consentAccepted` is false or missing |

---

## 11. Quy trình Triển khai (Step-by-Step)

### 11.1. Prerequisites

- [ ] ADR-DEVICE-001, ADR-DEVICE-002 đã Accepted
- [ ] ADR-DEVICE-003 (SDK vendor) — chưa cần Accepted cho phạm vi UC66 (mock-first), nhưng phải Accepted trước khi triển khai `3.1.2.4 Sync Health Device Data`
- [ ] DPO sign-off (module xử lý health/wearable consent data)
- [ ] Quyết định về `ConsentDataType.DEVICE_DATA` (Open Item ADR-DEVICE-002) đã được Tech Lead xác nhận

### 11.2. Pre-Migration Checklist

- [ ] Backup DB production: `pg_dump -h $HOST -U $USER carebridge > backup_20260701.sql`
- [ ] Migration `V20260701140000__create_device_connections.sql` chạy thành công trên staging ≥ 24 giờ
- [ ] Rollback script đã test trên staging (xem §12)
- [ ] DPO sign-off migration này (tạo bảng lưu liên kết consent + thiết bị sức khỏe)

### 11.3. Implementation Steps

#### Chặng 1 — Tạo Flyway migration

Tạo file: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260701140000__create_device_connections.sql`

```sql
-- Nội dung migration SQL đầy đủ — xem §5.2
```

Chạy migration:
```bash
./mvnw flyway:migrate
```

> ⚠️ **Chú ý:** Đây là bảng mới hoàn toàn (không lock existing table) — rủi ro thấp.

**Bảng tổng hợp version migration (áp dụng chung 4 UC66/67/68/69 — không trùng lặp):**

| UC | Migration file | Mục đích |
|----|----------------|----------|
| UC66 | `V20260701140000__create_device_connections.sql` | Tạo bảng `device_connections` |
| UC67 | `V20260701140100__extend_metric_type_and_source.sql` | Mở rộng `MetricType` (SLEEP/STEPS/SPO2), thêm `device_connection_id` FK vào `maternal_health_metrics` |
| UC68 | *(không cần migration riêng — tái sử dụng bảng `device_connections` từ UC66, chỉ UPDATE status)* | — |
| UC69 | *(không cần migration riêng — chỉ đọc dữ liệu từ `maternal_health_metrics` + `device_connections`)* | — |

#### Chặng 2 — Implement entity + repository

```java
// package com.carebridge.backend.health.device.entity.DeviceConnection
// package com.carebridge.backend.health.device.entity.DeviceType
// package com.carebridge.backend.health.device.entity.DeviceConnectionStatus
// package com.carebridge.backend.health.device.repository.IDeviceConnectionRepository
```

#### Chặng 3 — Implement service + controller

```java
// package com.carebridge.backend.health.device.service.DeviceConnectionService
// package com.carebridge.backend.health.device.controller.DeviceConnectionController
```

### 11.4. Deployment Checklist

- [ ] Migration `V20260701140000` chạy thành công
- [ ] `POST /api/v1/health/devices/connections` trả 201 với record CONNECTED mới
- [ ] `DeviceConnected` event được publish và log đúng format
- [ ] Reconnect (idempotent) không tạo duplicate record CONNECTED

---

## 12. Rollback & Incident Runbook

### 12.1. Điều kiện kích hoạt Rollback (Trigger Conditions)

| Điều kiện | Ngưỡng | Người quyết định |
|-----------|--------|------------------|
| Error rate tăng đột biến | > 5% trong 5 phút | On-call Engineer |
| Consent không được ghi nhận trước khi CONNECTED | Bất kỳ case nào | Tech Lead + DPO |
| Duplicate CONNECTED record cho cùng (user, deviceType) | Bất kỳ case nào | Tech Lead |

### 12.2. Rollback Procedure

```bash
# Bước 1: Revert migration
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DROP TABLE IF EXISTS device_connections CASCADE;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260701000001';"

# Bước 2: Re-deploy phiên bản cũ
kubectl rollout undo deployment/carebridge-api

# Bước 3: Verify
kubectl rollout status deployment/carebridge-api
curl -X GET https://$HOST/api/v1/health
```

### 12.3. Notification Protocol

| Thời điểm | Người nhận | Kênh | Template |
|-----------|------------|------|----------|
| Ngay khi phát hiện | On-call team | Slack `#incident` | "🚨 DEVICE-CONNECTION incident: [mô tả]" |
| Trong 30 phút | DPO | Email | *(bắt buộc nếu consent/health data bị ảnh hưởng)* |

### 12.4. Post-Incident Review (PIR)

- **Timeline / Root Cause / Impact / Remediation / Prevention** — theo template chuẩn EDS §12.4.

---

## 13. Kịch bản Kiểm thử Chi tiết

> Chi tiết đầy đủ nằm trong `UC66_ConnectHealthDevice_Test-Spec.md`. Section này tóm tắt chiến lược xác minh — tham chiếu Test Condition IDs.

### 13.1. Unit Tests
- `DEVICE-TC-001`..`005`: connect happy path, idempotent reconnect, invalid deviceType, consent not accepted, wrong role.

### 13.2. Integration Tests
- `DEVICE-TC-INT-001`: full connect flow qua Testcontainers PostgreSQL — verify persisted row + consent_grant_id linkage.

### 13.3. E2E / Security Tests
- `DEVICE-TC-E2E-001`: ROLE_PARTNER attempts connect → 403.
- `DEVICE-TC-SEC-001`: injection attempt trong `deviceName` field.

---

## 14. Phương pháp Xác minh

### 14.1. Database Inspection

```sql
SELECT id, user_id, device_type, status, consent_grant_id, connected_at
FROM device_connections
WHERE user_id = '[uuid]'
ORDER BY connected_at DESC;

-- Verify no duplicate active connections
SELECT user_id, device_type, count(*) FROM device_connections
WHERE status = 'CONNECTED'
GROUP BY user_id, device_type
HAVING count(*) > 1;
-- Expected: 0 rows
```

### 14.2. Log / Audit Verification

```bash
kubectl logs -l app=carebridge-api | grep '"eventType":"DeviceConnected"' | head -5
```

---

## 15. Mẫu thử thực tế (API Verification Samples)

### 15.1. Happy Path

```bash
curl -X POST https://$HOST/api/v1/health/devices/connections \
  -H "Authorization: Bearer $MOTHER_JWT" \
  -H "Content-Type: application/json" \
  -d '{"deviceType":"SMARTWATCH","deviceName":"Mi Band 8","consentAccepted":true}'
```

### 15.2. Error Paths

```bash
# Missing consent
curl -X POST https://$HOST/api/v1/health/devices/connections \
  -H "Authorization: Bearer $MOTHER_JWT" \
  -H "Content-Type: application/json" \
  -d '{"deviceType":"SMARTWATCH","consentAccepted":false}'
```
**Expected Response (400):**
```json
{"error":{"code":"DEVICE-006","message":"Consent must be accepted before connecting a device"}}
```

---

## 16. Bảng tổng hợp phân quyền (Authorization Matrix)

| Endpoint | `GUEST` | `ROLE_MOTHER` | `ROLE_PARTNER` | `ROLE_FAMILY` | `ROLE_EXPERT` | `ROLE_SYSTEM_ADMIN` |
|----------|---------|---------------|----------------|---------------|---------------|---------------------|
| `POST /api/v1/health/devices/connections` | ❌ | ✅ Own | ❌ | ❌ | ❌ | ✅ All (admin support) |
| `GET /api/v1/health/devices/connections` | ❌ | ✅ Own | ❌ | ❌ | ❌ | ✅ All |

**Chú thích:** `Own` = chỉ thao tác trên `device_connections` mà `user_id` khớp với JWT `sub` (strict ownership per BR-RBAC).

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source (ADR/BR) | Last Verified |
|---|-----------|-----------------|---------------|
| C1 | Consent PHẢI được capture (qua `ConsentService.grant()`) trước khi tạo record `status=CONNECTED` | `ADR-DEVICE-002 / BR-PRIVACY` | `2026-07-01` |
| C2 | Reconnect khi đã có active CONNECTED record cùng (userId, deviceType) → idempotent, KHÔNG tạo record mới | `ADR-DEVICE-001` | `2026-07-01` |
| C3 | userId lấy từ JWT SecurityContext — KHÔNG lấy từ request body | `BR-RBAC` | `2026-07-01` |
| C4 | KHÔNG bao giờ xóa (`DELETE`) record `device_connections` — chỉ đổi status | `ADR-DEVICE-001/003` | `2026-07-01` |
| C5 | `DeviceConnected` event PHẢI publish sau mỗi connect thành công | `§7.1 Domain Event Catalog` | `2026-07-01` |

### 17.2 Constraint Injection Block (Copy-Paste vào AI Prompt)

```
[CONSTRAINT BLOCK — Module: Connect Health Device — CB-DEVICE-IMP-001]
Theo TDS CB-DEVICE-IMP-001 và các ADR liên quan:

1. Consent PHẢI capture qua ConsentService.grant() TRƯỚC khi set status=CONNECTED (ADR-DEVICE-002/BR-PRIVACY)
2. Reconnect với active CONNECTED record cùng (userId, deviceType) => trả về record hiện có, KHÔNG tạo duplicate (ADR-DEVICE-001)
3. userId từ JWT SecurityContext (BR-RBAC)
4. KHÔNG implement delete() cho DeviceConnection — append-only (ADR-DEVICE-001/003)
5. Publish DeviceConnected event sau mỗi connect thành công (§7.1)

[CONTEXT BLOCK]
- Bounded Context: health.device
- Data Classification: Sensitive-PII
- Compliance: PDPA / Luật 91/2025
- Existing interfaces: §8 Service Interface + §8.2 Repository Interface
- Error codes: §10 Error Codes Table
- Auth matrix: §16 Authorization Matrix

[TASK BLOCK]
Implement DeviceConnectionService.connect() và listActiveConnections() thỏa mãn constraints trên.
Output phải tuân thủ §8 Interface Specification. Tests phải cover §13 Test Scenarios.
```

### 17.3 Constraint Quality Checklist

- [x] Mỗi constraint traceable về ADR hoặc BR cụ thể
- [x] Không có constraint generic
- [x] Mỗi constraint có `Last Verified` ≤ 2 sprints
- [x] Constraint block có ≥ 3 constraints (có 5)
- [x] Constraint block reference §8 và §16

### 17.4 Anti-Pattern Detection (cho AI-Generated Code từ Block này)

| AP-ID | Anti-Pattern | Dấu hiệu | Hành động |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code không match bất kỳ constraint C1-C5 nào | Reject — inject lại constraints |
| AP-AI-003 | Implicit Decision | Code assume có SDK vendor cụ thể không có trong ADR-DEVICE-003 | Reject — ADR-DEVICE-003 còn Proposed, không được assume |
| AP-AI-005 | Hallucinated Contract | Code import service/type không có trong §8 | Reject — verify contract existence |

---

## PHỤ LỤC

### A. Glossary (Thuật ngữ)

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| DeviceConnection | Entity đại diện cho 1 lần kết nối thiết bị sức khỏe của Mother |
| Append-only | Chiến lược lưu trữ không cho phép UPDATE trạng thái xóa/DELETE, chỉ thay đổi status |
| Idempotent Connect | Gọi connect() nhiều lần với cùng input không tạo duplicate record |
| DPO | Data Protection Officer |

### B. Tài liệu tham chiếu

| Document | Link / Path |
|----------|-------------|
| SRS UC-66 | `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.43` |
| Task Allocation | `04_Implement/implement_artifacts/function-spec-task-allocation.md` (dòng 676-680) |
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |
| Existing consent module | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/` |
| Existing health metric entity | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/entity/MaternalHealthMetric.java` |
| Related sibling TDS | `04_Implement/UC67_ImportDeviceDataManually/`, `UC68_DisconnectHealthDevice/`, `UC69_ViewDeviceDataTrend/` |

---

## OPEN ITEMS (chưa resolve — cần quyết định của user/Tech Lead/DPO)

| # | Open Item | Impact nếu không resolve | Đề xuất tạm thời |
|---|-----------|---------------------------|-------------------|
| O1 | `ConsentDataType` chưa có giá trị `DEVICE_DATA` chuyên biệt | Consent scope cho device data lẫn với `HEALTH_RECORD` chung | Dùng `HEALTH_RECORD` làm fallback cho đến khi Tech Lead duyệt thêm enum value |
| O2 | Wearable SDK vendor (Apple HealthKit / Google Health Connect / Fitbit / khác) | Không thể triển khai `3.1.2.4 Sync Health Device Data` thật | Mock-first (ADR-DEVICE-003) — connect chỉ là "đăng ký ý định", không pair phần cứng thật |
| O3 | SRS flows (Normal/Alt/Exception) là template chung, không có business logic cụ thể theo field cho UC66 | Một số hành vi field-level (vd giới hạn số lượng thiết bị/user) không có nguồn rõ ràng | Giả định không giới hạn số lượng `deviceType` khác nhau/user; chỉ giới hạn 1 active CONNECTED per (user, deviceType) theo ADR-DEVICE-001 |

---

*TDS Draft — chờ review và approval. KHÔNG tự set Status = Approved.*
