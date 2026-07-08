# CB-MAP-NEARBY-PKG-06-TDS — Map + Nearby Care (UC-77–UC-82)

| Field | Value |
|-------|-------|
| **Document ID** | CB-MAP-NEARBY-PKG-06-TDS |
| **Version** | 1.0 |
| **Date** | 2026-07-03 |
| **Status** | DRAFT |
| **Package** | PKG-06 — Map & Nearby Care |
| **Included UCs** | UC-77, UC-78, UC-79, UC-80, UC-81, UC-82 |
| **Document Owner** | Lâm (TV4) |
| **Based on** | CAREBRIDGE_TDS_TEMPLATE.md v1.0 |

---

## 1. Module Overview

| Field | Value |
|-------|-------|
| **Module Name** | Map & Nearby Care |
| **Bounded Context** | map / location / nearbycare |
| **UC IDs** | UC-77–UC-82 |
| **Primary Actor(s)** | MOTHER, FAMILY, SYSTEM_ADMIN |
| **Platform** | Backend API |
| **Data Classification** | Internal |
| **Upstream Dependencies** | PKG-01 (ExpertProfile), PKG-TRIAGE (TriageResultPort), PKG-AUTH |
| **Downstream Consumers** | Mobile App (map rendering), Web App (emergency map) |

**Description:** Map quản lý care_facilities (phòng khám, bệnh viện), nearbycare xử lý tìm cơ sở y tế gần nhất, route/ETA, quick-action. Sprint 0: seed static facility data + basic search API. Route/ETA uses TrackAsia stubs. Emergency handoff via `EmergencyMapHandoff` port.

---

## 2. Schema Mapping

### care_facilities

| Field | Java Type | V1 Column | V1 Type | Nullable | Match |
|-------|-----------|-----------|---------|----------|-------|
| facilityId | UUID | facility_id | uuid | NOT NULL | ✅ |
| partnerId | UUID | partner_id | uuid | NULL | ✅ |
| name | String | name | varchar(255) | NOT NULL | ✅ |
| facilityType | String | facility_type | varchar(50) | NULL | ✅ |
| address | String | address | varchar(500) | NULL | ✅ |
| latitude | BigDecimal | latitude | numeric | NULL | ✅ |
| longitude | BigDecimal | longitude | numeric | NULL | ✅ |
| phone | String | phone | varchar(30) | NULL | ✅ |
| openingHoursJson | String | opening_hours_json | jsonb | NULL | ✅ stored as String |
| sourceType | String | source_type | varchar(30) | NULL | ✅ |
| verificationStatus | FacilityStatus | verification_status | varchar(30) | NOT NULL DEFAULT 'UNVERIFIED' | ✅ |
| createdAt | LocalDateTime | created_at | timestamptz | NOT NULL | ✅ |
| updatedAt | LocalDateTime | updated_at | timestamptz | NOT NULL | ✅ |

### location_snapshots

| Field | Java Type | V1 Column | V1 Type | Nullable | Match |
|-------|-----------|-----------|---------|----------|-------|
| locationSnapshotId | UUID | location_snapshot_id | uuid | NOT NULL | ✅ |
| userId | UUID | user_id | uuid | NOT NULL | ✅ |
| contextType | String | context_type | varchar(50) | NULL | ✅ |
| contextId | UUID | context_id | uuid | NULL | ✅ |
| latitude | BigDecimal | latitude | numeric | NOT NULL | ✅ |
| longitude | BigDecimal | longitude | numeric | NOT NULL | ✅ |
| accuracyMeters | BigDecimal | accuracy_meters | numeric | NULL | ✅ |
| capturedAt | Instant | captured_at | timestamptz | NOT NULL | ✅ |
| expiresAt | Instant | expires_at | timestamptz | NULL | ✅ |
| consentStatus | String | consent_status | varchar(20) | NULL | ✅ |

---

## 3. ADRs

### ADR-MAP-001: TrackAsia Stub for Sprint 0

**Context:** TrackAsia API key will be configured in .env but route/ETA calls use a deterministic stub for Demo.
**Decision:** Implement `RouteProvider` interface with a stub impl that returns fixed ETA + route polyline. Real TrackAsia client is injected later (ADR in Sprint 1).
**Consequences:** ✅ Demo reproducibility; ⚠️ Real routes require SDK integration later.

### ADR-MAP-002: Facility Data Seeded via Flyway

**Context:** care_facilities table is empty in V1.
**Decision:** Seed 5 care facilities via Flyway migration V20260703000001. Facilities are immutable for Sprint 0.
**Consequences:** ✅ Demo has real data; ⚠️ Seed data must not overwrite production.

---

## 4. API Specification

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | /api/v1/map/nearby-facilities | JWT | MOTHER, FAMILY | UC-80 — search by lat/lng/radius |
| GET | /api/v1/map/facilities | JWT | AUTHENTICATED | Browse all |
| GET | /api/v1/map/facilities/{id} | JWT | AUTHENTICATED | UC-81 |
| POST | /api/v1/map/route | JWT | MOTHER, FAMILY | UC-82 — get route (stub) |
| POST | /api/v1/map/emergency-handoff | JWT | MOTHER | UC-78 — triage → map handoff |
| GET | /api/v1/map/emergency/{handoffId} | JWT | MOTHER, FAMILY | UC-78 — view emergency map |

### GET /api/v1/map/nearby-facilities
**Params:** `lat` (required), `lng` (required), `radiusMeters` (default 5000), `type` (optional)

```json
{
  "success": true,
  "data": {
    "facilities": [
      {
        "facilityId": "uuid",
        "name": "Bệnh viện Phụ sản Trung ương",
        "facilityType": "HOSPITAL",
        "address": "...",
        "latitude": 10.8231,
        "longitude": 106.6297,
        "phone": "024...",
        "distanceMeters": 1200,
        "verificationStatus": "VERIFIED"
      }
    ]
  }
}
```

### POST /api/v1/map/emergency-handoff (Integration Contract)
**Request (from TriageResultPort):**
```json
{
  "triageHandoffId": "uuid",
  "riskLevel": "RED",
  "userLatitude": 10.8231,
  "userLongitude": 106.6297,
  "symptomSummary": "..."
}
```

---

## 5. Integration Contracts (Sprint 0 Boundaries)

| Contract | Owner | Consumer | Type |
|----------|-------|----------|------|
| `ITriageResultPort` | TV5 | TV4 | Read interface — TV4 reads triage result |
| `EmergencyMapHandoff` | TV4 | TV5 | TV4 creates handoff record |
| `RouteProvider` | TV4 | (internal) | Interface for route/ETA |

> Sprint 0: Interfaces are declared; stubs are implemented. Real implementations in Sprint 1.

---

## 6. Seed Data

Flyway `V20260703000001__seed_care_facilities.sql`:

```sql
INSERT INTO care_facilities (facility_id, name, facility_type, address, latitude, longitude, phone, verification_status)
VALUES
  ('00000000-0000-0000-0000-000000000101', 'Bệnh viện Phụ sản Trung ương Cần Thơ', 'HOSPITAL', '360 Đ. Nguyễn Văn Cừ, An Khánh, Ninh Kiều, Cần Thơ', 10.0186, 105.7878, '02923888888', 'VERIFIED'),
  ('00000000-0000-0000-0000-000000000102', 'Phòng khám sản phụ khoa Hồng Hạc', 'CLINIC', '45B Đ. Lê Lợi, Tân An, Ninh Kiều, Cần Thơ', 10.0123, 105.7856, '0292123456', 'VERIFIED'),
  ('00000000-0000-0000-0000-000000000103', 'Bệnh viện Đa khoa Trung ương Cần Thơ', 'HOSPITAL', '5 Đ. Nguyễn Văn Cừ, Hưng Lợi, Ninh Kiều, Cần Thơ', 10.0156, 105.7867, '02923868888', 'VERIFIED'),
  ('00000000-0000-0000-0000-000000000104', 'Phòng khám Nhi Cửu Long', 'CLINIC', '12 Đ. Nguyễn Trãi, Xuân Khánh, Ninh Kiều, Cần Thơ', 10.0190, 105.7890, '0292765432', 'VERIFIED'),
  ('00000000-0000-0000-0000-000000000105', 'Trạm y tế phường An Khánh', 'HEALTH_STATION', '88 Đ. Đ. Mậu Thân, An Khánh, Ninh Kiều, Cần Thơ', 10.0170, 105.7840, '0292111222', 'VERIFIED');
```

---

## 7. Error Codes

| Code | HTTP | Trigger |
|------|------|---------|
| MAP-001 | 400 | Missing lat/lng params |
| MAP-002 | 404 | Facility not found |
| MAP-003 | 404 | No nearby facilities found |
| MAP-004 | 403 | Not authorized for emergency |
| MAP-005 | 400 | Invalid coordinate range |

---

## 8. Authorization Matrix

| Endpoint | UNAUTH | MOTHER | FAMILY | ADMIN |
|----------|--------|--------|--------|-------|
| GET /nearby-facilities | ❌ | ✅ | ✅ | ✅ |
| GET /facilities | ❌ | ✅ | ✅ | ✅ |
| GET /facilities/{id} | ❌ | ✅ | ✅ | ✅ |
| POST /route | ❌ | ✅ | ✅ | ✅ |
| POST /emergency-handoff | ❌ | ✅ | ❌ | ✅ |
| GET /emergency/{id} | ❌ | ✅ | ✅ | ✅ |

---

## 9. Implementation Order

1. `entity/CareFacility.java` + `FacilityStatus.java`
2. `entity/LocationSnapshot.java`
3. `entity/EmergencyMapHandoff.java`
4. `repository/CareFacilityRepository.java`
5. `repository/LocationSnapshotRepository.java`
6. `repository/EmergencyMapHandoffRepository.java`
7. `dto/request/*` + `dto/response/*`
8. `exception/MapException.java`
9. `service/IRouteProvider.java` (interface stub)
10. `service/impl/StubRouteProvider.java`
11. `service/ICareFacilityService.java` + impl
12. `service/IEmergencyMapHandoffService.java` + impl
13. `service/ILocationSnapshotService.java` + impl
14. `controller/CareFacilityController.java`
15. `controller/EmergencyMapHandoffController.java`
16. `controller/LocationSnapshotController.java`

---

*CareBridge TDS v1.0*
