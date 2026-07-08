# CB-EXPPROF-PKG-01-TDS — Expert Profile (UC-60, UC-61, UC-65, UC-70)

| Field | Value |
|-------|-------|
| **Document ID** | CB-EXPPROF-PKG-01-TDS |
| **Version** | 1.0 |
| **Date** | 2026-07-03 |
| **Status** | DRAFT |
| **Package** | PKG-01 — Expert Profile |
| **Included UCs** | UC-60, UC-61, UC-65, UC-70 |
| **Document Owner** | Lâm (TV4) |
| **Author** | Lâm — TV4 Member |
| **Reviewed by** | [ ] Tech Lead |
| **Approved by** | [ ] Pending |
| **Based on** | CAREBRIDGE_TDS_TEMPLATE.md v1.0 |

---

## CHANGELOG

| Date | Author | Content |
|------|--------|---------|
| 2026-07-03 | Lâm — TV4 | Create TDS Sprint 0 — Expert Profile |

---

## 1. Module Overview

| Field | Value |
|-------|-------|
| **Module Name** | Expert Profile |
| **Bounded Context** | expert |
| **UC IDs** | UC-60, UC-61, UC-65, UC-70 |
| **Primary Actor(s)** | EXPERT, SYSTEM_ADMIN, CONTENT_ADMIN |
| **Platform** | Backend API |
| **Data Classification** | Internal (profile), Confidential (credentials) |
| **Upstream Dependencies** | PKG-AUTH (User entity, SecurityUtils) |
| **Downstream Consumers** | expertverification (reads profile), nearbycare (reads verified badge) |

**Description:** ExpertProfile là entity root cho chức năng chuyên gia. Một user với role EXPERT tạo profile, admin approve/reject. Public directory chỉ expose APPROVED profiles. Package này KHÔNG chứa consultation, payment, realtime — các package đó bị loại khỏi scope này.

---

## 2. Schema Mapping (V1 Verification)

| Entity Field | Java Type | V1 Column | V1 SQL Type | V1 Nullable | Match? | Notes |
|-------------|-----------|-----------|-------------|-------------|--------|-------|
| expertProfileId | UUID | expert_profile_id | uuid | NOT NULL DEFAULT gen_random_uuid() | ✅ | PK |
| userId | UUID | user_id | uuid | NOT NULL | ✅ | FK → users.user_id, UNIQUE |
| specialty | String | specialty | varchar(100) | NULL | ✅ | |
| professionalTitle | String | professional_title | varchar(150) | NULL | ✅ | |
| experienceYears | Integer | experience_years | smallint | NULL | ✅ | Integer map OK |
| workplace | String | workplace | varchar(200) | NULL | ✅ | |
| consultationScope | String | consultation_scope | text | NULL | ✅ | |
| verificationStatus | VerificationStatus (ENUM) | verification_status | varchar(30) | NOT NULL DEFAULT 'PENDING' | ✅ | ENUM: PENDING/UNDER_REVIEW/APPROVED/REJECTED/SUSPENDED/EXPIRED |
| verifiedAt | LocalDateTime | verified_at | timestamptz | NULL | ✅ | |
| verifiedBy | UUID | verified_by | uuid | NULL | ✅ | FK → users.user_id |
| ratingAvg | BigDecimal | rating_avg | numeric | NULL | ✅ | precision 3, scale 2 |
| createdAt | LocalDateTime | created_at | timestamptz | NOT NULL DEFAULT now() | ✅ | @CreationTimestamp |
| updatedAt | LocalDateTime | updated_at | timestamptz | NOT NULL DEFAULT now() | ✅ | @UpdateTimestamp |

**FK verification (V1):**
| Entity Field | FK Target | V1 Constraint | Match? |
|-------------|-----------|---------------|--------|
| userId | users.user_id | FK inferred, UNIQUE constraint exists | ✅ |
| verifiedBy | users.user_id | No explicit FK in V1 (nullable) | ✅ Application-layer ref |

---

## 3. Architecture Decisions

### ADR-EXPPROF-001: VerificationStatus enum stored as VARCHAR

**Context:** V1 schema uses `varchar(30)` for verification_status. Options: store enum as string, ordinal, or join to lookup table.
**Decision:** Use `@Enumerated(EnumType.STRING)` with length=30. Matches V1 exactly. No lookup table needed.
**Consequences:** ✅ Human-readable in DB; ✅ V1 compatible; ⚠️ Renaming enum value requires migration.

### ADR-EXPPROF-002: Only APPROVED experts visible in public directory

**Context:** Expert directory (UC-65) should only show approved experts.
**Decision:** Repository query `findVerifiedPublic()` filters `verification_status = 'APPROVED'`. Expert's own profile (`getMyProfile`) shows regardless of status.
**Consequences:** ✅ Privacy first; ✅ Consistent with RBAC (expert sees own, public sees approved).

### ADR-EXPPROF-003: UserId extracted from JWT, never from request body

**Context:** Ownership of profile — user might try to create profile for another user.
**Decision:** ALWAYS use `SecurityUtils.requireCurrentUserId(principal)`. Never accept `userId` in request DTO.
**Consequences:** ✅ Prevents spoofing; ✅ Audit trail is accurate.

---

## 4. NFR & SLA

| Category | Requirement | Target | Verification |
|----------|-------------|--------|--------------|
| Latency | GET /directory p50 | < 200ms | Integration test |
| Latency | POST /profiles p99 | < 500ms | Integration test |
| Auth | 401 for unauthenticated | 100% | Security test |
| RBAC | 403 for non-EXPERT on write | 100% | Security test |
| Data | userId unique (1 profile per user) | Enforced | DB constraint + service check |

---

## 5. API Specification

### Endpoints

| Method | Path | Auth | Required Roles | Idempotent? |
|--------|------|------|---------------|-------------|
| POST | /api/v1/expert/profiles | JWT Bearer | EXPERT | No |
| GET | /api/v1/expert/profiles/me | JWT Bearer | EXPERT | Yes |
| PATCH | /api/v1/expert/profiles/me | JWT Bearer | EXPERT | No |
| GET | /api/v1/expert/directory | JWT Bearer | AUTHENTICATED | Yes |
| GET | /api/v1/expert/profiles/{id} | JWT Bearer | AUTHENTICATED | Yes |
| POST | /api/v1/expert/profiles/{id}/approve | JWT Bearer | SYSTEM_ADMIN, CONTENT_ADMIN | No |
| POST | /api/v1/expert/profiles/{id}/reject | JWT Bearer | SYSTEM_ADMIN, CONTENT_ADMIN | No |
| GET | /api/v1/expert/verified | JWT Bearer | AUTHENTICATED | Yes |

### Request/Response: POST /api/v1/expert/profiles

**Request Body:**
```json
{
  "specialty": "Sản khoa",
  "professionalTitle": "Bác sĩ chuyên khoa I",
  "experienceYears": 10,
  "workplace": "Bệnh viện Phụ sản Trung ương",
  "consultationScope": "Tư vấn thai kỳ, theo dõi thai nhi"
}
```

**Response — 201:**
```json
{
  "success": true,
  "data": {
    "expertProfileId": "uuid",
    "userId": "uuid",
    "specialty": "Sản khoa",
    "professionalTitle": "Bác sĩ chuyên khoa I",
    "experienceYears": 10,
    "workplace": "Bệnh viện Phụ sản Trung ương",
    "consultationScope": "...",
    "verificationStatus": "PENDING",
    "ratingAvg": null,
    "createdAt": "2026-07-03T...",
    "updatedAt": "2026-07-03T..."
  },
  "message": null,
  "timestamp": "2026-07-03T..."
}
```

### Integration contracts used:
- `SecurityUtils.requireCurrentUserId(principal)` — Owner extraction
- `GlobalExceptionHandler` — Error mapping (EXPERT-001/002/003/004)
- `ApiResponse` wrapper

---

## 6. Error Codes

| Code | HTTP Status | Trigger |
|------|-------------|---------|
| EXPERT-001 | 409 CONFLICT | Profile already exists for user |
| EXPERT-002 | 404 NOT FOUND | Expert profile not found |
| EXPERT-003 | 404 NOT_FOUND | Expert profile not found (public) |
| EXPERT-004 | 404 NOT_FOUND | Expert not approved (public view) |

---

## 7. Authorization Matrix

| Endpoint | UNAUTH | MOTHER | FAMILY | EXPERT | MODERATOR | CONTENT_ADMIN | SYSTEM_ADMIN | PARTNER |
|----------|--------|--------|--------|--------|-----------|---------------|--------------|---------|
| POST /profiles | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| GET /profiles/me | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| PATCH /profiles/me | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| GET /directory | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| GET /profiles/{id} | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| POST /{id}/approve | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ |
| POST /{id}/reject | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ |
| GET /verified | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 8. AI Prompt Constraints (CASE 2.0 — Sprint 0)

### Constraint Summary (C1–C6)

| # | Constraint | Source |
|---|-----------|--------|
| C1 | POST /profiles: `@PreAuthorize("hasRole('EXPERT')")` | ADR-EXPPROF-003, BR-RBAC |
| C2 | UserId: `SecurityUtils.requireCurrentUserId(principal)` — NEVER from request body | ADR-EXPPROF-003 |
| C3 | VerificationStatus: VARCHAR(30), ENUM = PENDING/UNDER_REVIEW/APPROVED/REJECTED/SUSPENDED/EXPIRED | V1__init_schema.sql, ADR-EXPPROF-001 |
| C4 | userId UNIQUE: `existsByUserId()` check BEFORE save | V1 unique constraint |
| C5 | Public directory: only APPROVED profiles exposed | ADR-EXPPROF-002 |
| C6 | Delete: soft-delete NOT used — expert_profiles has no deleted_at; use verification_status=SUSPENDED | V1 schema |

### Constraint Injection Block

```
[CONSTRAINT BLOCK — Package: PKG-01 Expert Profile]
Theo TDS CB-EXPPROF-PKG-01-TDS:

1. Role enforcement:
   POST /api/v1/expert/profiles: @PreAuthorize("hasRole('EXPERT')")
   PATCH /api/v1/expert/profiles/me: @PreAuthorize("hasRole('EXPERT')")
   GET /api/v1/expert/directory: isAuthenticated()
   POST /{id}/approve: @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CONTENT_ADMIN')")

2. Owner extraction:
   ALL profile endpoints: PHẢI dùng SecurityUtils.requireCurrentUserId(principal)
   KHÔNG accept userId từ request body

3. Uniqueness:
   PHẢI check existsByUserId(userId) trước khi create (C4)

4. Public visibility:
   GET /directory và GET /profiles/{id} chỉ trả APPROVED profiles
   APPROVE/REJECT đặt verification_status + verified_at + verified_by

5. Forbidden:
   KHÔNG tạo consultation, payment, booking, realtime, commission tables/endpoints
   KHÔNG hard-delete expert_profiles (không có deleted_at column)
```

### Anti-Pattern Detection

| AP-ID | Anti-Pattern | Signal | Action |
|-------|-------------|--------|--------|
| AP-001 | Wrong role on write | `isAuthenticated()` cho POST/PATCH | Reject — C1 |
| AP-002 | Request body owner | `request.getUserId()` | Reject — C2 |
| AP-003 | Hallucinated endpoint | Không có trong §9 | Reject |
| AP-004 | Entity in response | `return entity` thay vì DTO | Reject |
| AP-005 | Missing unique check | Không gọi existsByUserId() trước save | Reject — C4 |
| AP-006 | Consultation/payment table creation | CREATE TABLE expert_consultations | Reject |

---

## 9. Implementation Order

1. `entity/ExpertProfile.java` — JPA entity matching V1
2. `entity/VerificationStatus.java` — enum
3. `repository/ExpertProfileRepository.java` — JpaRepository + custom queries
4. `dto/request/CreateExpertProfileRequest.java` — validation annotations
5. `dto/request/UpdateExpertProfileRequest.java`
6. `dto/response/ExpertProfileResponse.java` — light response
7. `dto/response/ExpertProfileDetailResponse.java` — full response
8. `dto/response/ExpertDirectoryResponse.java` — paginated list
9. `exception/ExpertException.java` — domain exception
10. `mapper/ExpertProfileMapper.java` — entity ↔ DTO mapping
11. `service/IExpertProfileService.java` — interface
12. `service/impl/ExpertProfileServiceImpl.java` — business logic
13. `controller/ExpertProfileController.java` — REST endpoints

**No migration needed** — all V1 tables already exist.

---

## 10. As-Built Reconciliation (fill after implementation)

| Item | TDS Claim | As-Built | Diff? |
|------|-----------|----------|-------|
| Entity fields | §2 mapping | | ☐ |
| Endpoints | §9 table | | ☐ |
| Role checks | §9, §8 | | ☐ |

---

*CareBridge TDS v1.0 — Adapted for Spring Boot 3.5.x / JDK 21*
