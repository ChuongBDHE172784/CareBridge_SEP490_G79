# CB-EXP-AVAIL-LOC-PKG-02-TDS — Expert Availability + Location Share

| Field | Value |
|-------|-------|
| **Document ID** | CB-EXP-AVAIL-LOC-PKG-02-TDS |
| **Version** | 1.0 |
| **Date** | 2026-07-03 |
| **Status** | DRAFT |
| **Package** | PKG-02 — Expert Availability & Location |
| **Included UCs** | UC-63, UC-64, UC-78, UC-79 |
| **Document Owner** | Lâm (TV4) |
| **Based on** | CAREBRIDGE_TDS_TEMPLATE.md v1.0 |

---

## 1. Tổng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | Expert Availability & Location Share |
| **Bounded Context** | expert / location |
| **UC IDs** | UC-63, UC-64, UC-78, UC-79 |
| **Primary Actor(s)** | EXPERT, MOTHER, FAMILY |
| **Platform** | Backend API |
| **Data Classification** | Internal |
| **Upstream Dependencies** | PKG-01 (ExpertProfile), PKG-AUTH (User) |
| **Downstream Consumers** | PKG-06 (NearbyCare reads availability) |

**Description:** ExpertAvailability cho phấy EXPERT đăng lịch rảnh. Location share cho phấy EXPERT chia sẻ vị trí hiện tại. NearbyCare đọc cả hai để match mother với expert gần nhất.

---

## 2. Schema Mapping

### expert_availability

| Field | Java Type | V1 Column | V1 Type | Nullable | Match |
|-------|-----------|-----------|---------|----------|-------|
| availabilityId | UUID | availability_id | uuid | NOT NULL | ✅ |
| expertProfileId | UUID | expert_profile_id | uuid | NOT NULL | ✅ |
| startAt | Instant | start_at | timestamptz | NOT NULL | ✅ |
| endAt | Instant | end_at | timestamptz | NOT NULL | ✅ |
| channelType | String | channel_type | varchar(30) | NOT NULL | ✅ |
| status | AvailabilityStatus (ENUM) | status | varchar(20) | NOT NULL DEFAULT 'AVAILABLE' | ✅ |
| createdAt | Instant | created_at | timestamptz | NOT NULL | ✅ |
| updatedAt | Instant | updated_at | timestamptz | NOT NULL | ✅ |

### expert_location_shares

| Field | Java Type | V1 Column | V1 Type | Nullable | Match |
|-------|-----------|-----------|---------|----------|-------|
| locationShareId | UUID | location_share_id | uuid | NOT NULL | ✅ |
| expertProfileId | UUID | expert_profile_id | uuid | NOT NULL | ✅ |
| latitude | BigDecimal | latitude | numeric | NOT NULL | ✅ |
| longitude | BigDecimal | longitude | numeric | NOT NULL | ✅ |
| accuracyMeters | BigDecimal | accuracy_meters | numeric | NULL | ✅ |
| availabilityStatus | String | availability_status | varchar(20) | NULL | ✅ |
| sharedAt | Instant | shared_at | timestamptz | NOT NULL | ✅ |
| expiresAt | Instant | expires_at | timestamptz | NULL | ✅ |
| consentReference | UUID | consent_reference | uuid | NULL | ✅ |
| createdAt | Instant | created_at | timestamptz | NOT NULL | ✅ |
| updatedAt | Instant | updated_at | timestamptz | NOT NULL | ✅ |

---

## 3. ADRs

### ADR-AVAIL-001: AvailabilityWindow overlap check is deferred to Sprint 1

**Context:** UC-64 says expert shouldn't overlap availability windows.
**Decision:** Sprint 0 allows soft overlaps; enforce uniqueness in Sprint 1 via validation.
**Consequences:** ✅ Simple Sprint 0 delivery; ⚠️ Data cleanup needed before enforcement.

---

## 4. API Specification

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| POST | /api/v1/expert/availability | JWT | EXPERT | UC-63 |
| GET | /api/v1/expert/availability/me | JWT | EXPERT | UC-63 |
| DELETE | /api/v1/expert/availability/{id} | JWT | EXPERT | Remove slot |
| POST | /api/v1/expert/location/share | JWT | EXPERT | UC-78 — requires consent |
| DELETE | /api/v1/expert/location/share | JWT | EXPERT | UC-79 — stop sharing |

### POST /api/v1/expert/availability — Request
```json
{
  "startAt": "2026-07-04T09:00:00Z",
  "endAt": "2026-07-04T17:00:00Z",
  "channelType": "ONLINE_CHAT",
  "status": "AVAILABLE"
}
```

### POST /api/v1/expert/location/share — Request
```json
{
  "latitude": 10.8231,
  "longitude": 106.6297,
  "accuracyMeters": 15.0,
  "availabilityStatus": "AVAILABLE",
  "expiresAt": "2026-07-04T18:00:00Z"
}
```

---

## 5. Error Codes

| Code | HTTP | Trigger |
|------|------|---------|
| EXPERT-010 | 404 | Availability not found |
| EXPERT-011 | 400 | endAt must be after startAt |
| EXPERT-012 | 403 | Expert profile not APPROVED |
| EXPERT-013 | 403 | Location share without valid consent |
| EXPERT-014 | 400 | Invalid coordinates |

---

## 6. Authorization Matrix: Availability + Location

| Endpoint | EXPERT | MOTHER | FAMILY | ADMIN |
|----------|--------|--------|--------|-------|
| POST /availability | ✅ | ❌ | ❌ | ❌ |
| GET /availability/me | ✅ | ❌ | ❌ | ❌ |
| DELETE /availability/{id} | ✅ | ❌ | ❌ | ❌ |
| POST /location/share | ✅ | ❌ | ❌ | ❌ |
| DELETE /location/share | ✅ | ❌ | ❌ | ❌ |

---

## 7. AI Constraints (CASE 2.0)

| # | Constraint |
|---|-----------|
| C1 | POST availability: `@PreAuthorize("hasRole('EXPERT')")` |
| C2 | POST location/share: `@PreAuthorize("hasRole('EXPERT')")` |
| C3 | endAt > startAt validated in service layer |
| C4 | Latitude/longitude ranges validated (-90..90, -180..180) |
| C5 | expertProfileId from SecurityUtils, NOT request body |
| **Forbidden** | No consultation/booking endpoint creation |

---

## 8. Implementation Order

1. `entity/ExpertAvailability.java` + `AvailabilityStatus.java`
2. `entity/ExpertLocationShare.java`
3. `repository/ExpertAvailabilityRepository.java`
4. `repository/ExpertLocationShareRepository.java`
5. `dto/request/*` + `dto/response/*`
6. `exception/ExpertException.java` (shared with PKG-01, add EXPERT-010..014)
7. `mapper/*`
8. `service/I*Service.java` + `impl/*`
9. `controller/ExpertAvailabilityController.java`

**No migration needed** — V1 tables exist.

---

*CareBridge TDS v1.0*
