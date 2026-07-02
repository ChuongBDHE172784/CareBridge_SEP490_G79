# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-88 Update Expert Profile

**Document ID:** `CB-EXP-TDD-002`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (lines 786-815, 1404-1405, 1523-1524, 1631)
- `04_Implement/UC88_UpdateExpertProfile/UC88_UpdateExpertProfile_TDS.md` (CB-EXP-IMP-002) — Technical Specification for this feature
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.2 — SRS UC-88
- `CLAUDE.md` — architecture/delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Backend: `./mvnw test`. Web: `npm run test:run` (Vitest + Testing Library).
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo Test-Spec cho UC-88, đồng bộ với schema thật và TDS CB-EXP-IMP-002 |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-88` |
| **Module** | `expert` — Update Expert Profile |
| **Spec gốc** | `CB-EXP-IMP-002` |
| **Priority** | 🔴 P0 (RBAC/self-escalation risk) |
| **Sprint** | `Sprint 2 — Complete Core CRUD And UI Wiring` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA` |
| **Upstream Dependencies** | `auth` module (JWT), UC-87 Create Expert Profile (profile row must pre-exist) |
| **Downstream Consumers** | UC-81 View Expert Profile, UC-80 View Expert Directory, UC-164 Search Expert |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXP-IMP-002 §17`, `ADR-EXP-101`, `ADR-EXP-102`, `ADR-EXP-103` |
| **Constraints Injected** | C1 (JWT-derived userId), C2 (role+ownership), C3 (no locked-field setter), C4 (explicit field-by-field mapping, no reflective copy), C5 (empty-update rejection), C6 (audit event on success), C7 (no new migration) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Sibling document `UC87_CreateExpertProfile_TDS.md` describes `expert_profiles` with columns `id`, `account_id`, `display_name`, `bio`, `specialties TEXT[]`, `status` enum | `V1__init_schema.sql` line 786-800 defines `expert_profile_id`, `user_id`, `specialty varchar(100)`, `professional_title varchar(150)`, `experience_years smallint`, `workplace varchar(200)`, `consultation_scope text`, `verification_status varchar(30) DEFAULT 'PENDING'` | All test cases use the REAL column/field names (`specialty`, `professionalTitle`, `experienceYears`, `workplace`, `consultationScope`, `verificationStatus`). UC-87's TDS is treated as stale/incorrect per CLAUDE.md ("current code and migrations override historical design notes") — flagged as OI-5 in the UC-88 TDS, not silently reconciled. |
| L2 | SRS UC-88 generic flow text ("actor enters information... system applies business rules") does not explicitly name which fields are editable | ADR-EXP-101 in TDS defines the allowlist explicitly from schema column list, excluding verification-related columns | Test cases TC-006/TC-007 explicitly assert `verificationStatus`/`verifiedAt`/`verifiedBy`/`ratingAvg` are unchanged after update, regardless of request payload content |
| L3 | Schema shows `expert_profile_id`/`user_id`/`created_at` with no `updatable=false` annotation info (SQL only) | JPA entity in TDS §11.3 Chặng 1 marks `createdAt` as `updatable = false`; `updatedAt` is explicitly set by service, not by DB trigger (no `ON UPDATE` trigger exists in schema) | TC-004 asserts service explicitly sets `updatedAt = now()` on save; TC-009 (integration) asserts `created_at` unchanged after update |
| L4 | No canonical partial-update (PATCH) semantics stated in SRS | TDS ADR-EXP-103 defines: all fields optional, null = "leave unchanged", at least 1 non-null field required | TC-002 (single-field partial update leaves other fields untouched), TC-003 (empty update → EXP-101) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
expert (UpdateExpertProfile) bao gồm các layer:
├── Domain (ExpertProfile entity — pure field mapping, no business logic beyond getters/setters)
├── Application / Service (ExpertProfileServiceImpl — mock IExpertProfileRepository, ExpertProfileMapper, AuditService với Mockito)
├── Mapper (ExpertProfileMapper.applyUpdate — pure unit test, no mocks needed)
├── Policy (ExpertProfileOwnershipPolicy — pure unit test)
├── Controller (ExpertProfileController — mock Service với @WebMvcTest + MockMvc)
├── Integration (Testcontainers PostgreSQL với @SpringBootTest, seeded expert_profiles row)
└── Web (React components — Vitest + @testing-library/react, mock apiClient/MSW)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-88 §3.2.1.2 (lines 775-794) | Happy update flow, PRE-3 auth requirement, E1/E2/E3 exception handling |
| `V1__init_schema.sql` lines 786-815, 1523-1524 | Column types/bounds for validation tests; unique `user_id` constraint |
| ADR-EXP-101 (TDS §3) | Editable-field allowlist test (TC-005), locked-field tampering test (TC-006/TC-007) |
| ADR-EXP-102 (TDS §3) | No auto-reset of `verification_status` on update (TC-006) |
| ADR-EXP-103 (TDS §3) | Field-level Bean Validation boundary tests (TC-010 to TC-015) |
| BR-RBAC | Role gate tests (TC-016 to TC-018) |
| BR-CONSULTATION | Audit event emission test (TC-008) |
| PDPA | No PII leak in logs test (TC-021) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner updates one or more allowlisted fields | `ExpertProfileService.updateMyProfile()` | `EXP-TC-001`, `EXP-TC-002` |
| TC-COND-002 | Request has no fields set (empty update) | `ExpertProfileService.updateMyProfile()` | `EXP-TC-003` |
| TC-COND-003 | Caller has no `expert_profiles` row yet | `ExpertProfileService.updateMyProfile()` | `EXP-TC-004b` |
| TC-COND-004 | `updated_at` is bumped; `created_at` untouched | `ExpertProfileService.updateMyProfile()` / DB | `EXP-TC-004`, `EXP-TC-009` |
| TC-COND-005 | Mapper copies only allowlisted fields | `ExpertProfileMapper.applyUpdate()` | `EXP-TC-005` |
| TC-COND-006 | Locked-field tampering via extra JSON keys is a no-op | `ExpertProfileController` / `UpdateExpertProfileRequest` binding | `EXP-TC-006`, `EXP-TC-007` |
| TC-COND-007 | Successful update emits `ExpertProfileUpdated` | `AuditService.emit()` | `EXP-TC-008` |
| TC-COND-008 | Field boundary validation (size/range) | `UpdateExpertProfileRequest` Bean Validation | `EXP-TC-010`–`EXP-TC-015` |
| TC-COND-009 | Role-based access control | `ExpertProfileController` `@PreAuthorize` | `EXP-TC-016`–`EXP-TC-018` |
| TC-COND-010 | Ownership always self-scoped via `/me` | `ExpertProfileOwnershipPolicy` | `EXP-TC-019`, `EXP-TC-020` |
| TC-COND-011 | No PII/secret leakage in logs | Logging layer | `EXP-TC-021` |
| TC-COND-012 | Web form submits only allowlisted fields; renders `verificationStatus` read-only | `ExpertProfileEditPage.tsx` | `EXP-WEB-TC-001`–`EXP-WEB-TC-004` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `experienceYears` (valid: 0-70, invalid: negative/>70), `specialty`/`professionalTitle`/`workplace` length | Confirms Bean Validation partitions match schema column limits |
| Boundary Value Analysis | `experienceYears` = -1, 0, 70, 71; `specialty` = 100/101 chars; `consultationScope` = 5000/5001 chars | Off-by-one errors are the most common validation bugs |
| State Transition Testing | N/A — `verification_status` state machine is NOT touched by this UC (explicitly asserted unchanged, not transitioned) | Confirms UC-88 does not accidentally implement UC-89/Admin verification logic |
| Error Guessing / Security | Locked-field tampering (`verificationStatus` in payload), cross-account attempt, missing/invalid JWT | Directly targets the self-escalation risk called out in the TDS RG-4 gate |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `expert_profiles{ user_id: EXPERT-A-uuid, specialty:'Obstetrics', professional_title:'BS. CKI', experience_years:5, workplace:'BV Từ Dũ', consultation_scope:'Tư vấn thai kỳ', verification_status:'PENDING', verified_at:null, verified_by:null, rating_avg:null }` | Happy path baseline |
| `FX-002` | DB seed | Same as FX-001 but `verification_status:'APPROVED', verified_at: '2026-06-01T00:00:00Z', verified_by: ADMIN-uuid, rating_avg: 4.8` | Verifies APPROVED profile's locked fields survive an update untouched |
| `FX-003` | JWT | `{ sub: 'EXPERT-A-uuid', role: 'EXPERT' }` | Auth context — owner |
| `FX-004` | JWT | `{ sub: 'EXPERT-B-uuid', role: 'EXPERT' }` | Auth context — different expert, no profile seeded (for TC-004b) |
| `FX-005` | JWT | `{ sub: 'MOTHER-A-uuid', role: 'MOTHER' }` | Auth context — wrong role |
| `FX-006` | env | none required (no HMAC/secret dependency for this module) | — |
| `FX-007` | Request body | `{ "specialty": "Cardiology", "verificationStatus": "APPROVED" }` (raw JSON string sent via MockMvc, bypassing typed DTO, to prove Jackson binding drops the unknown field) | Locked-field tampering attack simulation |

---

## 4. Test Case Specification

> **TC ID format:** `EXP-TC-[NNN]` (backend), `EXP-WEB-TC-[NNN]` (web)
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ExpertProfileTestFactory.java
class ExpertProfileTestFactory {

    static final UUID EXPERT_A_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID EXPERT_B_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");
    static final UUID ADMIN_USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000AD1");

    // Baseline PENDING profile — synced with FX-001
    static ExpertProfile makePendingProfile() {
        ExpertProfile p = new ExpertProfile();
        p.setExpertProfileId(UUID.randomUUID());
        p.setUserId(EXPERT_A_USER_ID);
        p.setSpecialty("Obstetrics");
        p.setProfessionalTitle("BS. CKI");
        p.setExperienceYears((short) 5);
        p.setWorkplace("BV Tu Du");
        p.setConsultationScope("Tu van thai ky");
        p.setVerificationStatus("PENDING");
        p.setVerifiedAt(null);
        p.setVerifiedBy(null);
        p.setRatingAvg(null);
        p.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        p.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        return p;
    }

    // Baseline APPROVED (verified) profile — synced with FX-002
    static ExpertProfile makeApprovedProfile() {
        ExpertProfile p = makePendingProfile();
        p.setVerificationStatus("APPROVED");
        p.setVerifiedAt(Instant.parse("2026-06-01T00:00:00Z"));
        p.setVerifiedBy(ADMIN_USER_ID);
        p.setRatingAvg(new BigDecimal("4.8"));
        return p;
    }

    static ExpertProfile makeProfile(Consumer<ExpertProfile> overrides) {
        ExpertProfile p = makePendingProfile();
        overrides.accept(p);
        return p;
    }

    static UpdateExpertProfileRequest makeValidRequest() {
        UpdateExpertProfileRequest r = new UpdateExpertProfileRequest();
        r.setSpecialty("Cardiology");
        r.setExperienceYears((short) 8);
        return r;
    }

    static UpdateExpertProfileRequest makeEmptyRequest() {
        return new UpdateExpertProfileRequest(); // all fields null
    }
}
```

```typescript
// expertProfileTestFactory.ts (Web — Vitest)
import type { ExpertProfileResponse } from '../models/expertProfile';

export const makePendingExpertProfile = (
  overrides: Partial<ExpertProfileResponse> = {}
): ExpertProfileResponse => ({
  expertProfileId: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-0000000000A1',
  specialty: 'Obstetrics',
  professionalTitle: 'BS. CKI',
  experienceYears: 5,
  workplace: 'BV Tu Du',
  consultationScope: 'Tu van thai ky',
  verificationStatus: 'PENDING',
  ratingAvg: null,
  updatedAt: '2026-06-01T00:00:00.000Z',
  ...overrides,
});
```

---

### EXP-TC-001 — Owner updates multiple allowlisted fields (happy path)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertProfileServiceImpl.updateMyProfile()`
**Test File:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/service/ExpertProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-88 Normal Flow Step 4-5`, `ADR-EXP-101`

**Preconditions:**
- FX-001 seeded (mocked via `findByUserId` returning `makePendingProfile()`)
- FX-003 JWT context (EXPERT_A_USER_ID)

**Test Steps:**
1. Arrange: `repository.findByUserId(EXPERT_A_USER_ID)` returns `makePendingProfile()`; request = `makeValidRequest()` (`specialty="Cardiology"`, `experienceYears=8`)
2. Act: call `service.updateMyProfile(request, EXPERT_A_USER_ID)`
3. Assert: response reflects `specialty="Cardiology"`, `experienceYears=8`; `repository.save()` called exactly once with an entity where `specialty` and `experienceYears` match; unspecified fields (`professionalTitle`, `workplace`, `consultationScope`) retain original seeded values

**Expected Result (PASS):**
- Returned `ExpertProfileResponse.specialty == "Cardiology"`, `.experienceYears == 8`
- `professionalTitle`/`workplace`/`consultationScope` unchanged from FX-001

**Expected Result (FAIL):**
- Any unspecified field is nulled out instead of preserved (indicates full-object overwrite instead of partial update)

**Current Status:** 🔴 Not written
**Implementation Note:** `applyUpdate()` must null-check each source field individually (§TDS C4).

---

### EXP-TC-002 — Single-field partial update leaves others untouched

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileMapper.applyUpdate()`
**Test File:** `.../expert/mapper/ExpertProfileMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-EXP-103` (partial update semantics)

**Test Steps:**
1. `ExpertProfile target = makePendingProfile()`
2. `UpdateExpertProfileRequest req` with only `workplace = "BV Hung Vuong"` set, all others null
3. `mapper.applyUpdate(target, req)`

**Expected Result (PASS):** `target.getWorkplace() == "BV Hung Vuong"`; `specialty`, `professionalTitle`, `experienceYears`, `consultationScope` unchanged from seed; returned `changedFields == ["workplace"]`

**Expected Result (FAIL):** Any other field mutated, or `changedFields` list incorrect

**Current Status:** 🔴 Not written

---

### EXP-TC-003 — Empty update request rejected

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertProfileServiceImpl.updateMyProfile()`
**Test File:** `.../expert/service/ExpertProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-EXP-103`, `TDS §11.3 Chặng 3`

**Test Steps:**
1. `request = makeEmptyRequest()` (all 5 fields null)
2. Call `service.updateMyProfile(request, EXPERT_A_USER_ID)`

**Expected Result (PASS):** Throws `ValidationException` with code `EXP-101`; `repository.findByUserId()` / `save()` NEVER called (fail fast before DB hit)

**Expected Result (FAIL):** Repository is queried/saved despite no-op request, or no exception thrown

**Current Status:** 🔴 Not written

---

### EXP-TC-004 — `updated_at` bumped, `created_at` immutable

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertProfileServiceImpl.updateMyProfile()`
**Test File:** `.../expert/service/ExpertProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `V1__init_schema.sql` line 799 (`updated_at`), TDS §11.3 Chặng 1

**Test Steps:**
1. Seed profile with `updatedAt = 2026-06-01T00:00:00Z`, `createdAt = 2026-06-01T00:00:00Z`
2. Call `updateMyProfile(makeValidRequest(), EXPERT_A_USER_ID)` at a later mocked `Instant.now()`
3. Capture the entity passed to `repository.save()`

**Expected Result (PASS):** Saved entity `updatedAt` > original seeded `updatedAt`; `createdAt` unchanged

**Expected Result (FAIL):** `updatedAt` unchanged, or `createdAt` mutated

**Current Status:** 🔴 Not written

---

### EXP-TC-004b — No profile exists for caller → 404 EXP-104

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.updateMyProfile()`
**Test File:** `.../expert/service/ExpertProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `TDS §8.1 IExpertProfileService throws contract`

**Preconditions:** FX-004 JWT (`EXPERT_B_USER_ID`), `repository.findByUserId(EXPERT_B_USER_ID)` returns `Optional.empty()`

**Test Steps:**
1. Call `service.updateMyProfile(makeValidRequest(), EXPERT_B_USER_ID)`

**Expected Result (PASS):** Throws `NotFoundException` code `EXP-104`; `save()` never called

**Expected Result (FAIL):** NullPointerException or silent creation of a new row (this endpoint must never create — only update)

**Current Status:** 🔴 Not written

---

### EXP-TC-005 — Mapper never references locked fields (compile-time / reflection check)

**Severity:** `CRITICAL`
**CWE:** `CWE-915 — Improperly Controlled Modification of Dynamically-Determined Object Attributes`
**Feature Under Test:** `UpdateExpertProfileRequest` DTO shape
**Test File:** `.../expert/dto/request/UpdateExpertProfileRequestTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-EXP-101` allowlist table

**Test Steps:**
1. Reflectively enumerate declared fields of `UpdateExpertProfileRequest.class`
2. Assert the field name set equals exactly `{specialty, professionalTitle, experienceYears, workplace, consultationScope}`

**Expected Result (PASS):** Field set matches exactly (no more, no less)

**Expected Result (FAIL):** Any of `verificationStatus`/`verifiedAt`/`verifiedBy`/`ratingAvg`/`userId`/`expertProfileId` present, OR an allowlisted field missing

**Current Status:** 🔴 Not written
**Implementation Note:** This is the structural/compile-time guarantee underpinning the entire self-escalation defense — treat any failure as CRITICAL/blocking.

---

### EXP-TC-006 — Locked-field tampering via extra JSON key is ignored (PENDING profile)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-915`
**Legal:** `PDPA — unauthorized modification of verification record`
**Feature Under Test:** `ExpertProfileController` JSON binding + full update flow
**Test File:** `.../expert/controller/ExpertProfileControllerTest.java` (`@WebMvcTest`)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-EXP-101`, `TDS §6.3 Sequence Diagram — Locked-Field Tampering`

**Preconditions:** FX-001 profile (PENDING), FX-003 JWT (owner), mocked service returns updated response reflecting only `specialty` changed

**Test Steps (Attack Simulation):**
1. Construct raw JSON body per FX-007: `{"specialty": "Cardiology", "verificationStatus": "APPROVED"}`
2. POST via MockMvc: `PATCH /api/v1/expert-profiles/me` with this raw body and EXPERT JWT
3. Inspect response body and the `UpdateExpertProfileRequest` object actually bound by Spring

**Expected Result (PASS = hệ thống an toàn):**
- HTTP 200 (request succeeds — only `specialty` is a legitimate field)
- Response `data.verificationStatus == "PENDING"` (unchanged from seed, NOT `"APPROVED"`)
- Bound `UpdateExpertProfileRequest` has no way to carry `verificationStatus` (no such field exists — verified structurally by EXP-TC-005)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Response `data.verificationStatus == "APPROVED"` — indicates self-escalation succeeded

**Current Status:** 🔴 Not written

---

### EXP-TC-007 — Locked-field tampering attempt on an already-APPROVED profile does not revert or alter verification fields

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `PDPA`
**Feature Under Test:** Full update flow against FX-002 (already-verified profile)
**Test File:** `.../expert/service/ExpertProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-EXP-102` (no auto-reset)

**Preconditions:** `repository.findByUserId()` returns `makeApprovedProfile()` (FX-002: `verificationStatus=APPROVED`, `verifiedAt` set, `verifiedBy=ADMIN_USER_ID`, `ratingAvg=4.8`)

**Test Steps:**
1. Call `service.updateMyProfile(makeValidRequest(), EXPERT_A_USER_ID)` (updates `specialty`/`experienceYears` only)
2. Capture saved entity

**Expected Result (PASS):**
- `specialty`/`experienceYears` updated as requested
- `verificationStatus == "APPROVED"` (unchanged — NOT reset to PENDING per ADR-EXP-102)
- `verifiedAt`, `verifiedBy`, `ratingAvg` byte-for-byte unchanged from FX-002

**Expected Result (FAIL):** Any locked field mutated, OR `verificationStatus` silently reset to `PENDING`

**Current Status:** 🔴 Not written

---

### EXP-TC-008 — Successful update emits `ExpertProfileUpdated` with correct changed-field list

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.updateMyProfile()` → `AuditService.emit()`
**Test File:** `.../expert/service/ExpertProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `TDS §7 Domain Event Catalog`, `BR-CONSULTATION`

**Test Steps:**
1. Call `service.updateMyProfile(makeValidRequest(), EXPERT_A_USER_ID)` (`specialty`, `experienceYears` set)
2. Capture argument passed to `auditService.emit()`

**Expected Result (PASS):** `emit()` called exactly once with `ExpertProfileUpdated` where `payload.changedFields` contains exactly `["specialty", "experienceYears"]` (order-independent set comparison), `payload.userId == EXPERT_A_USER_ID`, `payload.expertProfileId` matches saved entity id

**Expected Result (FAIL):** `emit()` not called, called with wrong fields, or called before `save()` completes (event must reflect post-save state)

**Current Status:** 🔴 Not written

---

### EXP-TC-009 — Integration: DB row updated, locked columns untouched

**Severity:** `HIGH`
**Feature Under Test:** Full flow: HTTP PATCH → Service → Repository → PostgreSQL
**Test File:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/ExpertProfileUpdateIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrated
- Seed row via JPA/JdbcTemplate matching FX-002 (APPROVED profile) for `EXPERT_A_USER_ID`

**Test Steps:**
1. Seed `expert_profiles` row (FX-002 shape)
2. `PATCH /api/v1/expert-profiles/me` with valid EXPERT JWT and body `{"workplace": "BV Hung Vuong"}`
3. Query DB directly for the row

**Expected Result (PASS):**
- `workplace = 'BV Hung Vuong'`
- `verification_status = 'APPROVED'`, `verified_at`/`verified_by`/`rating_avg` identical to seed
- `updated_at` > seeded value; `created_at` identical to seed
- Response HTTP 200, `ApiResponse.success=true`

**DB Assertion:**
```java
ExpertProfile record = expertProfileRepository.findByUserId(EXPERT_A_USER_ID).orElseThrow();
assertThat(record.getWorkplace()).isEqualTo("BV Hung Vuong");
assertThat(record.getVerificationStatus()).isEqualTo("APPROVED");
assertThat(record.getVerifiedBy()).isEqualTo(ADMIN_USER_ID);
assertThat(record.getCreatedAt()).isEqualTo(seededCreatedAt);
assertThat(record.getUpdatedAt()).isAfter(seededUpdatedAt);
```

**Current Status:** 🔴 Not written

---

### FIELD VALIDATION BOUNDARY TEST CASES

---

### EXP-TC-010 — `experienceYears` boundary: 0 (valid), -1 (invalid), 70 (valid), 71 (invalid)

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateExpertProfileRequest` Bean Validation
**Test File:** `.../expert/dto/request/UpdateExpertProfileRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-EXP-103` field table

**Test Steps (parameterized):** validate DTO with `experienceYears` = `{-1, 0, 70, 71}`

**Expected Result (PASS):** `-1` and `71` → constraint violation on `experienceYears`; `0` and `70` → no violation

**Current Status:** 🔴 Not written

---

### EXP-TC-011 — `specialty` boundary: 100 chars (valid), 101 chars (invalid)

**Severity:** `LOW`
**Feature Under Test:** `UpdateExpertProfileRequest` Bean Validation
**Test File:** same as EXP-TC-010
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `V1__init_schema.sql` line 789 (`varchar(100)`)

**Expected Result (PASS):** 100-char string → no violation; 101-char string → violation

**Current Status:** 🔴 Not written

---

### EXP-TC-012 — `professionalTitle` boundary: 150/151 chars

**Severity:** `LOW`
**Oracle Source:** `V1__init_schema.sql` line 790 (`varchar(150)`)
**Test File:** same as EXP-TC-010
**TDD Phase:** 🔴 RED
**Current Status:** 🔴 Not written

---

### EXP-TC-013 — `workplace` boundary: 200/201 chars

**Severity:** `LOW`
**Oracle Source:** `V1__init_schema.sql` line 792 (`varchar(200)`)
**Test File:** same as EXP-TC-010
**TDD Phase:** 🔴 RED
**Current Status:** 🔴 Not written

---

### EXP-TC-014 — `consultationScope` boundary: 5000/5001 chars

**Severity:** `LOW`
**Oracle Source:** `ADR-EXP-103` (business bound; column itself is `text`/unbounded)
**Test File:** same as EXP-TC-010
**TDD Phase:** 🔴 RED
**Current Status:** 🔴 Not written

---

### EXP-TC-015 — Invalid field-level errors surfaced with field name (EXP-101 response shape)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertProfileController` → global exception handler
**Test File:** `.../expert/controller/ExpertProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §9.2` 400 response schema

**Test Steps:** PATCH with `{"experienceYears": 999}`

**Expected Result (PASS):** HTTP 400, `error.code == "EXP-101"`, `error.details[0].field == "experienceYears"`

**Current Status:** 🔴 Not written

---

### SECURITY / RBAC TEST CASES

---

### EXP-TC-016 — `EXPERT` role → 200

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `ExpertProfileController` `@PreAuthorize`
**Test File:** `.../expert/controller/ExpertProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Expected Result (PASS = hệ thống an toàn):** HTTP 200 with FX-003 JWT

**Current Status:** 🔴 Not written

---

### EXP-TC-017 — `MOTHER`/`FAMILY` role → 403 EXP-103

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**Feature Under Test:** same
**Test File:** same
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Test Steps (Attack Simulation):** PATCH with FX-005 JWT (`MOTHER`)

**Expected Result (PASS = hệ thống an toàn):** HTTP 403, `error.code == "EXP-103"`

**Current Status:** 🔴 Not written

---

### EXP-TC-018 — No JWT → 401 EXP-102

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** same
**Test File:** same
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Expected Result (PASS = hệ thống an toàn):** HTTP 401

**Current Status:** 🔴 Not written

---

### EXP-TC-019 — Ownership: caller always resolves to own profile (no `{id}` param exists)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExpertProfileController` route shape + `ExpertProfileService.updateMyProfile()`
**Test File:** `.../expert/controller/ExpertProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §6.4 Sequence Diagram — Ownership`

**Test Steps (Attack Simulation):**
1. Attempt `PATCH /api/v1/expert-profiles/{EXPERT_A_profile_id}` (guessing an `{id}` route variant that does not exist in the spec) with EXPERT_B's JWT
2. Confirm no such mapped route exists (404 from Spring routing, not a business-logic 200)

**Expected Result (PASS = hệ thống an toàn):** HTTP 404 (no route registered for `{id}` variant) — proves the API surface never exposes a cross-account write path

**Expected Result (FAIL):** Any 200/403 response implies an `{id}`-based route was implemented outside the TDS §9.1 contract — reject, not part of approved scope

**Current Status:** 🔴 Not written

---

### EXP-TC-020 — `ExpertProfileOwnershipPolicy.assertOwnedBy()` unit behavior (defense-in-depth)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertProfileOwnershipPolicy`
**Test File:** `.../expert/policy/ExpertProfileOwnershipPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Test Steps:**
1. `assertOwnedBy(profile{userId=A}, A)` → no exception
2. `assertOwnedBy(profile{userId=A}, B)` → throws `ForbiddenException` code `EXP-103`

**Expected Result (PASS):** Both behaviors confirmed

**Current Status:** 🔴 Not written

---

### EXP-TC-021 — No PII/secret in logs on update

**Severity:** `MEDIUM`
**Legal:** `PDPA`
**Feature Under Test:** Logging output around `updateMyProfile()`
**Test File:** `.../expert/service/ExpertProfileServiceImplTest.java` (log capture) or manual `kubectl logs` check per TDS §14.2
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Expected Result (PASS):** Log output for `ExpertProfileUpdated` contains only `eventId`, `occurredAt`, `expertProfileId`, `userId`, `changedFields` (field names) — never raw values of `workplace`/`consultationScope`/etc.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES (already covered above)

See `EXP-TC-009`.

---

### WEB TEST CASES (Vitest + @testing-library/react)

> Framework: Vitest + `@testing-library/react` + `@testing-library/user-event`. Run via `npm run test:run` in `05_Development/CareBridgeWebApp`. Mock network via MSW or `vi.mock('../../../shared/api/apiClient')`.

---

### EXP-WEB-TC-001 — Edit form renders current profile values and read-only verification badge

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileEditPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertProfileEditPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §11.3 Chặng 5`

**Test Steps:**
1. Mock `getMyExpertProfile()` to resolve `makePendingExpertProfile()`
2. Render `<ExpertProfileEditPage />` wrapped in a TanStack Query provider
3. Wait for form fields to populate

**Expected Result (PASS):**
- Input for `specialty` shows `"Obstetrics"`, etc.
- A "Verification status" badge/label shows `"PENDING"` and is NOT an editable input (no `role="textbox"`/`role="combobox"` bound to it)

**Expected Result (FAIL):** Verification status rendered as an editable form control

**Current Status:** 🔴 Not written

---

### EXP-WEB-TC-002 — Submitting the form calls `updateMyExpertProfile` with only changed allowlisted fields

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertProfileEditPage.tsx` submit handler
**Test File:** same as above
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-EXP-101`

**Test Steps:**
1. Render form with seeded profile
2. `userEvent.clear()` + `userEvent.type()` the `specialty` field to `"Cardiology"`
3. `userEvent.click(submitButton)`

**Expected Result (PASS):** Mocked `updateMyExpertProfile` called with a payload object whose keys are a subset of `{specialty, professionalTitle, experienceYears, workplace, consultationScope}` and includes `specialty: "Cardiology"`; payload never contains a `verificationStatus` key

**Expected Result (FAIL):** Payload includes `verificationStatus` or any non-allowlisted key

**Current Status:** 🔴 Not written

---

### EXP-WEB-TC-003 — Zod schema rejects out-of-bound `experienceYears` client-side before submit

**Severity:** `MEDIUM`
**Feature Under Test:** `updateExpertProfileSchema` (Zod)
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/models/expertProfile.test.ts`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §11.3 Chặng 5` Zod schema

**Test Steps:** `updateExpertProfileSchema.safeParse({ experienceYears: 71 })`

**Expected Result (PASS):** `success === false`, issue path includes `experienceYears`

**Current Status:** 🔴 Not written

---

### EXP-WEB-TC-004 — Empty form submit is blocked client-side (mirrors EXP-TC-003)

**Severity:** `LOW`
**Feature Under Test:** `updateExpertProfileSchema` refine rule
**Test File:** same as EXP-WEB-TC-003
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`

**Test Steps:** `updateExpertProfileSchema.safeParse({})`

**Expected Result (PASS):** `success === false` (refine rule requiring at least one defined field triggers)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXP-TC-001` | `ExpertProfileServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-002` | `ExpertProfileMapperTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-003` | `ExpertProfileServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-004` | `ExpertProfileServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-004b` | `ExpertProfileServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-005` | `UpdateExpertProfileRequestTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-006` | `ExpertProfileControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-007` | `ExpertProfileServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-008` | `ExpertProfileServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-009` | `ExpertProfileUpdateIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-010` | `UpdateExpertProfileRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-011` | same | `[ ]` | `[ ]` | |
| `EXP-TC-012` | same | `[ ]` | `[ ]` | |
| `EXP-TC-013` | same | `[ ]` | `[ ]` | |
| `EXP-TC-014` | same | `[ ]` | `[ ]` | |
| `EXP-TC-015` | `ExpertProfileControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-016` | `ExpertProfileControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-017` | same | `[ ]` | `[ ]` | |
| `EXP-TC-018` | same | `[ ]` | `[ ]` | |
| `EXP-TC-019` | same | `[ ]` | `[ ]` | |
| `EXP-TC-020` | `ExpertProfileOwnershipPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-TC-021` | `ExpertProfileServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXP-WEB-TC-001` | `ExpertProfileEditPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EXP-WEB-TC-002` | same | `[ ]` | `[ ]` | |
| `EXP-WEB-TC-003` | `expertProfile.test.ts:TBD` | `[ ]` | `[ ]` | |
| `EXP-WEB-TC-004` | same | `[ ]` | `[ ]` | |

**Test case count:** 26 total (22 backend incl. 1 integration, 4 web) — Critical: 6 (`EXP-TC-005/006/007/016/017/019`, `EXP-WEB-TC-002`≈7 if counted), High: 6, Medium: 9, Low: 5.

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertProfileServiceImpl implements IExpertProfileService {

    @Override
    public ExpertProfileResponse updateMyProfile(UpdateExpertProfileRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertProfileResponse getMyProfile(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```typescript
// ExpertProfileEditPage.tsx — Red Phase stub
export default function ExpertProfileEditPage() {
  throw new Error('Not implemented — Red Phase stub');
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXP-TC-001` | `throw(UnsupportedOperationException)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXP-TC-005` | N/A — pure DTO reflection test, does not call service stub | 🔴 FAIL if DTO field mismatch pre-exists | ☐ FAIL ☐ PASS | Note: this test can legitimately pass even at Red Phase if DTO already correctly scoped — verify it fails only if allowlist is wrong, not because stub throws |
| `EXP-TC-006` | Controller stub returns 500 (unhandled exception) | 🔴 FAIL (expects 200 with unchanged verificationStatus) | ☐ FAIL ☐ PASS | |
| `EXP-TC-016`–`018` | `@PreAuthorize` still active even with stub service (role gate is independent of business logic) | 🔴 FAIL for 016 (500 not 200), but 017/018 may legitimately PASS at Red Phase since they test rejection BEFORE reaching stub | ☐ FAIL ☐ PASS | If 017/018 pass at Red Phase, this is EXPECTED (not AP-AI-002) — the gate being tested is Spring Security, not the stub |
| *(remaining TCs)* | `throw(UnsupportedOperationException)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled when implementation begins)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

> **Lưu ý đặc biệt cho UC-88:** `EXP-TC-017`/`EXP-TC-018` (role/auth gates) may legitimately PASS even against a throwing stub, because Spring Security's `@PreAuthorize` filter runs BEFORE the controller method body executes — this is NOT a Green-from-Birth violation (AP-AI-002) since the assertion target is the security filter chain, not the stub logic. Document this exception explicitly when running the Red Gate so reviewers don't misflag it.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXP-IMP-002` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead — đặc biệt L1 (schema mismatch với UC-87 TDS)
- [ ] Không cần Flyway migration (đã confirm — §5.2 TDS)
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration test `EXP-TC-009` xanh (Testcontainers)
- [ ] `npm run test:run` — tất cả web tests xanh
- [ ] Test coverage ≥ 80% lines cho `ExpertProfileServiceImpl`, `ExpertProfileMapper`
- [ ] Không có business logic trong `ExpertProfileController` (chỉ validation + mapping + `@PreAuthorize`)
- [ ] Không có PII/secret xuất hiện plaintext trong logs (`EXP-TC-021`)
- [ ] `EXP-TC-005`, `EXP-TC-006`, `EXP-TC-007` (locked-field defense) PASS — **release blocker if any fail**

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement (with documented exception for `EXP-TC-017`/`018`)
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mỗi test dùng `ExpertProfileTestFactory.makeProfile()`, không có shared mutable static state
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (đã điền trong mỗi TC ở §4)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-87 not yet implemented and no way to seed `expert_profiles` rows for integration tests (mitigated: integration test seeds directly via JPA/JDBC, does not depend on UC-87's controller)
- OI-5 (UC-87 TDS schema mismatch) escalates into an actual schema change request — would require re-sync of this Test-Spec

---

## 7. Rollback Plan

```bash
# No migration to revert. Revert implementation files:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeWebApp/src/features/expert/

# Gap vẫn OPEN → giữ nguyên UC-88 TDS/Test-Spec Status: Draft
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (mọi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☑ (documented exception for auth-gate TCs) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (all trace to ADR-EXP-101/102/103) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (EXP-TC-015/016/017/018 only test HTTP status/routing, not business rules) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, hoặc dùng field name từ UC-87's stale TDS (`bio`, `displayName`, `accountId`) thay vì field thật (`consultationScope`, `professionalTitle`, `userId`) | ☑ (all field names verified against `V1__init_schema.sql` directly, not against UC-87 TDS) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào → TDD spec approved for Red Gate start
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none detected during authoring)_ | — | — | — | — |

---

*TDD Spec v1.0 — CB-EXP-TDD-002 — Status: Draft — pending review.*
