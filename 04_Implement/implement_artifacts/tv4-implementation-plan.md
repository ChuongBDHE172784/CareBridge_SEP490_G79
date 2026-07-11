# TV4 (Lâm) — Implementation Plan
**Owner:** Lâm (TV4) | **Scope:** 18 UCs (UC-60→UC-71, UC-77→UC-82)
**Workflow:** `/create-specs` → `/implement-feature` → `/build-screen`

---

## Mục tiêu chính

### Demo Gate A (Sprint 1)
```
Register → Verify OTP → Login as Mother → Ask Question
→ Login as Verified Expert → Post Verified Answer → Mother sees verified badge
```

### Demo Gate B (Sprint 2)
```
Mother logs symptoms → AI triage (red-risk) → Emergency Map opens
→ Nearby facility → Route/ETA → Quick action
```

---

## Sprint 0: Foundation (NOW)

### S0-1: 6 Package Boundaries
```
expert/              → UC-60,61,64,69 (expert profile CRUD + contribution)
expertverification/  → UC-62,63,70,71 (document upload, review, trust status)
expertavailability/  → UC-64,82 (availability config + expert nearby response)
map/                 → UC-77,78,79 (emergency map, facility search, route)
location/            → UC-77,80 (location snapshot, family emergency alert)
nearbycare/          → UC-81,82 (nearby support request + expert response)
```

### S0-2: 8 JPA Entities (V1 schema đã có)
| Entity | Table V1 | Package |
|--------|----------|---------|
| `ExpertProfile` | `expert_profiles` | `expert` |
| `ExpertCredential` | `expert_credentials` | `expertverification` |
| `ExpertAvailability` | `expert_availability` | `expertavailability` |
| `CareFacility` | `care_facilities` | `map` |
| `LocationSnapshot` | `location_snapshots` | `location` |
| `ExpertLocationShare` | `expert_location_shares` | `expertavailability` |
| `NearbySupportRequest` | *(new table — cần schema)* | `nearbycare` |
| `NearbySupportResponse` | *(new table — cần schema)* | `nearbycare` |

### S0-3: 3 Integration Contracts
```
ExpertBadgeReadPort    → TV3 xem để hiển thị badge trên expert answers
RouteProvider          → internal map service, có thể mock
EmergencyMapHandoff    → TV5 reads triage result, TV4 provides map API
```

### S0-4: Flyway Migration
- V1 tables đã có → **KHÔNG CẦN** migration mới cho existing tables
- `nearby_support_requests`, `nearby_support_responses` → cần thêm (schema gap check)

### S0-5: Seed Data
- 1 verified expert (expert@carebridge.dev → VERIFIED)
- 5 care facilities (mock TrackAsia data)

---

## Sprint 1: Demo Gate A (UC-60,62,65,66,67,68,70)

### Backend (TDS + Test-Spec + Implement)

#### PKG-01: expert (UC-60,61,64,69)
| UC | API Endpoint | Method |
|----|-------------|--------|
| UC-60 | POST `/api/v1/experts/profile` | Submit expert application |
| UC-61 | PUT `/api/v1/experts/profile` | Update profile (approved fields) |
| UC-64 | PUT `/api/v1/experts/availability` | Configure availability |
| UC-69 | GET `/api/v1/experts/{id}/contribution` | View points + badges |

#### PKG-02: expertverification (UC-62,63,70,71)
| UC | API Endpoint | Method | Auth |
|----|-------------|--------|------|
| UC-62 | POST `/api/v1/experts/{id}/credentials` | Upload docs | EXPERT (own) |
| UC-63 | GET `/api/v1/experts/verification-status` | View status | EXPERT |
| UC-70 | PUT `/api/v1/admin/experts/{id}/review` | Review submission | SYSTEM_ADMIN |
| UC-71 | PUT `/api/v1/admin/experts/{id}/trust` | Suspend/reinstate | SYSTEM_ADMIN |

#### Integration: ExpertBadgeReadPort
```java
public interface ExpertBadgeReadPort {
    boolean isVerified(UUID userId);
    ExpertBadgeInfo getBadgeInfo(UUID userId);  // name, specialty, verificationDate
}
```

### Web UI (Sprint 1 screens)
| Screen | CB ID | Framework | Features |
|--------|-------|-----------|----------|
| Expert Portal Dashboard | CB-054 | React + TS + Vite + Tailwind | Overview, navigation |
| Expert Professional Profile | CB-055 | same | View/edit profile |
| Verification Documents | CB-056 | same | Upload/view credentials |
| Availability Calendar | CB-057 | same | Manage availability slots |
| Expert Question Queue | CB-063 | same | Browse + answer questions |

### Mobile UI (Sprint 1 screens)
| Screen | CB ID | Features |
|--------|-------|----------|
| Expert App Home | CB-036 | Dashboard overview |
| Expert Question Queue | CB-042 | Browse + answer |
| Post Community Answer | CB-256 | Write expert answer |
| Expert Profile Setup | CB-033 | Initial profile creation |

---

## Sprint 2: Demo Gate B (UC-61,63,64,69,71,77,78,79,80,81,82)

### Backend

#### PKG-03: expertavailability (UC-64,82)
| UC | API Endpoint | Method |
|----|-------------|--------|
| UC-64 | PUT `/api/v1/experts/availability` | Set availability window |
| UC-82 | PUT `/api/v1/experts/nearby/request/{id}/respond` | Accept/decline request |

#### PKG-04: map (UC-77,78,79)
| UC | API Endpoint | Method |
|----|-------------|--------|
| UC-77 | GET `/api/v1/nearbycare/map` | Open emergency map |
| UC-78 | GET `/api/v1/nearbycare/facilities` | Find nearby facilities |
| UC-79 | GET `/api/v1/nearbycare/route` | Route + ETA |

#### PKG-05: location (UC-77,80)
| UC | API Endpoint | Method |
|----|-------------|--------|
| (shared) | POST `/api/v1/location/snapshot` | Save location |
| UC-80 | POST `/api/v1/nearbycare/emergency-alert` | Family alert |

#### PKG-06: nearbycare (UC-81,82)
| UC | API Endpoint | Method |
|----|-------------|--------|
| UC-81 | POST `/api/v1/nearbycare/support-request` | Create request |
| UC-81 | DELETE `/api/v1/nearbycare/support-request/{id}` | Cancel request |

#### Integration Contracts (implemented)
```
EmergencyMapHandoff  → TV4 API receives triageHandoffId, opens map with context
RouteProvider        → internal service, pluggable (mock / TrackAsia)
TriageResultPort     → consumed from TV5, maps to emergency map state
```

### Web UI (Sprint 2 screens)
| Screen | CB ID | Features |
|--------|-------|----------|
| Expert Location Sharing | CB-043 | Share/unshare location |
| Nearby Support Requests | CB-044 | View incoming requests |
| Nearby Request Detail | CB-046 | Request detail + respond |
| Route to Nearby User | CB-048 | Display route on map |

### Mobile UI (Sprint 2 screens)
| Screen | CB ID | Features |
|--------|-------|----------|
| Emergency Map | CB-017 | Open map from triage |
| Expert Location Sharing | CB-043 | Share location toggle |
| Nearby Support Requests | CB-044 | Receive + respond |
| Find Nearby Care Facility | CB-248 | Facility search |
| Route to Nearby User | CB-048 | Navigation |

---

## Sprint 3-5: Hardening (no new UCs)
- Edge cases, contract tests, regression, test evidence
- Replace mocks with TrackAsia provider
- Accessibility review

---

## Execution Flow (3 kỹ năng chủ đạo)

```
/create-specs  →  TDS + Test-Spec cho mỗi package (6 packages × 2 docs)
     ↓
/implement-feature  →  Entity → Repo → DTO → Mapper → Service → Controller → Test
     ↓ (per package, in order: expert → expertverification → expertavailability → map → location → nearbycare)
/build-screen  →  UI mocks (HTML) → React/TS/Tailwind (web) + Flutter (mobile)
     ↓
Connect UI ↔ Backend API
```

## File Output Structure

```
04_Implement/implement_artifacts/tv4/
├── PKG-01_expert/
│   ├── TDS.md          (TDS template filled)
│   ├── Test-Spec.md    (Test-Spec template filled)
│   └── IMP-PLAN.md     (implementation checklist)
├── PKG-02_expertverification/
├── PKG-03_expertavailability/
├── PKG-04_map/
├── PKG-05_location/
├── PKG-06_nearbycare/
├── contracts/
│   ├── ExpertBadgeReadPort.md
│   ├── RouteProvider.md
│   └── EmergencyMapHandoff.md
├── migration/
│   └── V20260703__add_nearby_support_tables.sql
└── seed-data/
    └── seed_expert_and_facilities.sql
```

## Key Constraints (không xung đột)
- **KHÔNG** tạo consultation/payment/partner package (V2 deferred)
- **KHÔNG** modify TV1 user tables, TV3 community tables
- **KHÔNG** modify existing Flyway migrations (chỉ tạo mới, timestamped)
- UC-68: dùng TV3 CommunityAnswer API — không tạo answer table riêng
- Seed data: `expert@carebridge.dev` → role=EXPERT, profile=VERIFIED
- TrackAsia: mock-first, real provider configurable via env var
