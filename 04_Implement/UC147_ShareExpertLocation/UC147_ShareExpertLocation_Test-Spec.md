# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC147 — Share Expert Location

**Document ID:** `CB-LOC-TDD-147`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (`expert_location_shares` dòng 828-840, `consent_grants` dòng 164-180, `audit_logs` dòng 31-42)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.6.1` — UC-147 functional requirements
- `04_Implement/UC147_ShareExpertLocation/UC147_ShareExpertLocation_TDS.md` (`CB-LOC-IMP-147`) — Technical Specification
- `04_Implement/UC129_CalculateDistanceRouteAndETA/UC129_CalculateDistanceRouteAndETA_TDS.md` — map/location convention reference
- Luật 91/2025 (PDPA Vietnam) — location data as Sensitive-PII

> **Quy ước TDD:** Test cases mô tả TRƯỚC khi viết production code. Thứ tự bắt buộc: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵. Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC147 Share Expert Location |

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
| **Feature / Gap ID** | `UC147-SHARE-LOCATION` |
| **Module** | `Share Expert Location — Bounded Context: location` |
| **Spec gốc** | `CB-LOC-IMP-147` |
| **Priority** | 🟡 P2 (Medium, theo SRS) |
| **Sprint** | `S3 (theo function-spec-task-allocation.md — "Expert location visibility" Sprint 3 onward)` |
| **Milestone** | `M3 Alpha` *(Open — chưa có milestone cụ thể cho location trong nguồn đọc được)* |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `consent_grants` (module `privacy`, TV1), `expert_profiles` (module `expert`, TV4) |
| **Downstream Consumers** | UC148 (cùng bảng), UC149/UC153 (Nearby Discovery — đọc), UC150-152 (View/Navigate Support — đọc) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-LOC-IMP-147 §17`, `ADR-LOC-101/102/103/104/105` |
| **Constraints Injected** | C1 (consent verify before write), C2 (upsert semantics), C3 (ownership from SecurityContext), C4 (verified-only), C5 (audit emission), C6 (no IMapProviderService dependency) |
| **Model** | `Claude (Sonnet)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.6.1 không có Business Rule nào ngoài BR-RBAC — không nói rõ về consent | `CLAUDE.md` mandate: "location ... workflows: enforce existing RBAC, consent scope/expiry, and audit requirements"; `consent_grants.data_type` CHECK có literal `'LOCATION'` | Test PHẢI verify consent check trước khi ghi — dùng `'LOCATION'`/`'SHARE'` làm oracle value cho `data_type`/`purpose` |
| L2 | `expert_location_shares.consent_reference` (uuid) không thể FK trực tiếp tới `consent_grants.id` (bigint) — type mismatch | Xác nhận qua đọc trực tiếp `V1__init_schema.sql` dòng 829 (`consent_reference uuid`) và dòng 165 (`id bigint`) | Test KHÔNG assert `consent_reference == consent_grants.id`; thay vào đó verify consent check được thực hiện bằng query riêng theo `(user_id, data_type, purpose)`, và `consent_reference` chỉ là 1 UUID tự sinh (correlation id) — theo ADR-LOC-101 |
| L3 | `expert_location_shares` không có UNIQUE constraint trên `expert_profile_id` trong V1 baseline | Xác nhận qua tìm kiếm toàn bộ `V1__init_schema.sql` — chỉ có PK + FK, không UNIQUE | Test verify upsert invariant ("1 active/expert") được enforce ở **application layer** (`findByExpertProfileId()` trước khi save), KHÔNG dựa vào DB constraint (chưa migration) |
| L4 | `audit_logs.action` CHECK constraint hiện tại KHÔNG có literal cho location sharing | Xác nhận qua đọc `V1__init_schema.sql` dòng 41 — enum đóng, thiếu `LOCATION_SHARE_*` | Integration test giả định migration `V20260705140000` đã chạy (mở rộng enum) — nếu migration này chưa Approved, test audit-related PHẢI được đánh dấu **BLOCKED**, không viết stub giả |
| L5 | `expert_location_shares.expires_at` là nullable trong schema nhưng SRS mô tả "for a selected time period" (bắt buộc có thời hạn) | Cột schema cho phép NULL, nhưng ADR-LOC (TDS §8.1) validate `durationMinutes` là `@NotNull` ở DTO | Test validation phải cover case thiếu `durationMinutes` → 400, và verify `expiresAt` LUÔN được set (không null) trong response/DB sau khi share thành công |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Module `location` (UC147) bao gồm các layer:
├── Domain (ExpertLocationShare entity — pure mapping, no business logic)
├── Service (ExpertLocationShareService — mock Repository/ConsentGrantRepository với Mockito)
├── Controller (ExpertLocationShareController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest, migration thật)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-147` (§3.3.6.1) | Verified Expert share current/approximate location cho 1 time period; BR-RBAC |
| `CB-LOC-IMP-147 §3` ADR-LOC-101 → 105 | Consent verification, upsert semantics, audit emission, no TrackAsia dependency, authorization |
| `CB-LOC-IMP-147 §10` | Error code oracle: LOC-001 → LOC-006 |
| `V1__init_schema.sql` | Schema oracle: `expert_location_shares`, `consent_grants`, `audit_logs`, `expert_profiles` |
| PDPA / Luật 91/2025 | Consent-before-processing requirement cho Location Sensitive-PII |
| BR-RBAC (CareBridge project-wide) | Only `EXPERT` role, own-resource-only access |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| `TC-COND-001` | Share location thành công khi có consent hợp lệ + verified expert | `ExpertLocationShareService.shareLocation()` | `LOC-TC-001` |
| `TC-COND-002` | Share location bị từ chối khi KHÔNG có consent LOCATION/SHARE hợp lệ | `ExpertLocationShareService.shareLocation()` | `LOC-TC-002` |
| `TC-COND-003` | Share lần 2 UPDATE row cũ (upsert), không tạo row mới | `ExpertLocationShareService.shareLocation()` | `LOC-TC-003` |
| `TC-COND-004` | Sau upsert, chỉ có đúng 1 active row cho expert_profile_id | `ExpertLocationShareRepository` (integration) | `LOC-TC-INT-001` |
| `TC-COND-005` | Audit log ghi đúng action `LOCATION_SHARE_CREATED`/`LOCATION_SHARE_UPDATED` | `AuditService` + `ExpertLocationShareService` | `LOC-TC-004` |
| `TC-COND-006` | Unverified expert (`verification_status != 'APPROVED'`) bị từ chối share | `ExpertLocationShareService.shareLocation()` | `LOC-TC-005` |
| `TC-COND-007` | Non-EXPERT role bị từ chối ở Controller layer (403) | `ExpertLocationShareController` | `LOC-TC-006` |
| `TC-COND-008` | Validation: latitude/longitude ngoài range, durationMinutes thiếu/vượt max | `ShareLocationRequest` DTO validation | `LOC-TC-007`, `LOC-TC-008` |
| `TC-COND-009` | Expiry semantics: `getMyActiveShare()` trả rỗng khi `expires_at <= now()` | `ExpertLocationShareService.getMyActiveShare()` | `LOC-TC-009` |
| `TC-COND-010` | Retry an toàn: gọi `shareLocation()` liên tiếp nhiều lần không tạo duplicate/side effect bất thường (SRS E3) | `ExpertLocationShareService.shareLocation()` | `LOC-TC-010` |
| `TC-COND-011` | `revokeMyShare()` idempotent — gọi khi không có active share không throw | `ExpertLocationShareService.revokeMyShare()` | `LOC-TC-011` |
| `TC-COND-012` | `expertProfileId` KHÔNG thể bị client tự chọn qua request body (chống IDOR) | `ExpertLocationShareController`/`Service` | `LOC-TC-SEC-001` |
| `TC-COND-013` | E2E full flow qua API thật (Testcontainers) | Full stack | `LOC-TC-E2E-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `latitude`/`longitude` (valid range vs out-of-range), `durationMinutes` (valid vs null vs > max) | Đảm bảo mỗi partition được kiểm tra ít nhất 1 lần |
| Boundary Value Analysis | `latitude` = -90/90, `longitude` = -180/180, `durationMinutes` = 1/1440/1441 | Giá trị biên dễ gây off-by-one error |
| State Transition Testing | State machine §6.4 TDS (NO_SHARE → ACTIVE → EXPIRED/REVOKED → ACTIVE) | Verify mọi transition hợp lệ, không transition ngầm sai |
| Error Guessing | IDOR attack (chọn expertProfileId khác), consent bypass attempt | Attack vector cho Sensitive-PII endpoint |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `expert_profiles{expertProfileId: 'aaaa...01', userId: 'uuuu...01', verificationStatus: 'APPROVED'}` | Verified expert happy path |
| `FX-002` | DB seed | `expert_profiles{expertProfileId: 'aaaa...02', userId: 'uuuu...02', verificationStatus: 'PENDING'}` | Unverified expert reject path |
| `FX-003` | DB seed | `consent_grants{userId: 'uuuu...01', dataType: 'LOCATION', purpose: 'SHARE', revokedAt: null, expiryAt: now+30d}` | Valid consent |
| `FX-004` | DB seed | `consent_grants{userId: 'uuuu...02', dataType: 'LOCATION', purpose: 'SHARE', revokedAt: now-1d}` | Revoked consent (reject path) |
| `FX-005` | DB seed | `expert_location_shares{expertProfileId: 'aaaa...01', locationShareId: 'llll...01', expiresAt: now+1h}` | Existing active share (upsert test) |
| `FX-006` | JWT | `{sub: 'uuuu...01', roles: ['EXPERT']}` | Auth context — verified expert |
| `FX-007` | JWT | `{sub: 'uuuu...03', roles: ['MOTHER']}` | Auth context — wrong role |
| `FX-008` | env | N/A — no external API key needed (ADR-LOC-104, no TrackAsia call in write path) | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ExpertLocationShareTestFactory.java
// ═══════════════════════════════════════════════════════════

class ExpertLocationShareTestFactory {

    static ExpertProfile makeVerifiedExpertProfile() {
        return makeVerifiedExpertProfile(p -> {});
    }

    static ExpertProfile makeVerifiedExpertProfile(Consumer<ExpertProfile> overrides) {
        ExpertProfile profile = new ExpertProfile();
        profile.setExpertProfileId(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"));
        profile.setUserId(UUID.fromString("uuuuuuuu-0000-0000-0000-000000000001"));
        profile.setVerificationStatus("APPROVED");
        overrides.accept(profile);
        return profile;
    }

    static ExpertProfile makeUnverifiedExpertProfile() {
        ExpertProfile profile = makeVerifiedExpertProfile();
        profile.setExpertProfileId(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002"));
        profile.setUserId(UUID.fromString("uuuuuuuu-0000-0000-0000-000000000002"));
        profile.setVerificationStatus("PENDING");
        return profile;
    }

    static ConsentGrant makeValidLocationConsent(UUID userId) {
        ConsentGrant grant = new ConsentGrant();
        grant.setUserId(userId);
        grant.setDataType("LOCATION");
        grant.setPurpose("SHARE");
        grant.setConsentGivenAt(Instant.now().minus(1, ChronoUnit.DAYS));
        grant.setExpiryAt(Instant.now().plus(30, ChronoUnit.DAYS));
        grant.setRevokedAt(null);
        grant.setVersion(1);
        return grant;
    }

    static ConsentGrant makeRevokedLocationConsent(UUID userId) {
        ConsentGrant grant = makeValidLocationConsent(userId);
        grant.setRevokedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        return grant;
    }

    static ShareLocationRequest makeValidShareRequest() {
        return makeValidShareRequest(r -> {});
    }

    static ShareLocationRequest makeValidShareRequest(Consumer<ShareLocationRequest> overrides) {
        ShareLocationRequest request = new ShareLocationRequest();
        request.setLatitude(new BigDecimal("10.7769"));
        request.setLongitude(new BigDecimal("106.7009"));
        request.setAccuracyMeters(new BigDecimal("15.5"));
        request.setAvailabilityStatus("AVAILABLE");
        request.setDurationMinutes(120);
        overrides.accept(request);
        return request;
    }

    static ExpertLocationShare makeExistingActiveShare(UUID expertProfileId) {
        ExpertLocationShare share = new ExpertLocationShare();
        share.setLocationShareId(UUID.fromString("llllllll-0000-0000-0000-000000000001"));
        share.setExpertProfileId(expertProfileId);
        share.setLatitude(new BigDecimal("10.7700"));
        share.setLongitude(new BigDecimal("106.7000"));
        share.setSharedAt(Instant.now().minus(30, ChronoUnit.MINUTES));
        share.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        return share;
    }
}
```

---

### LOC-TC-001 — Share location thành công (happy path, first-time)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertLocationShareService.shareLocation()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationShareServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-LOC-IMP-147 §6.1 Sequence Diagram Happy Path`, `ADR-LOC-101`, `ADR-LOC-102`

**Preconditions:**
- Mock `expertProfileRepository.findByUserId(userId)` → `makeVerifiedExpertProfile()` (FX-001)
- Mock `consentGrantRepository.findLatestActive(userId, 'LOCATION', 'SHARE')` → `makeValidLocationConsent(userId)` (FX-003)
- Mock `locationShareRepository.findByExpertProfileId(expertProfileId)` → `Optional.empty()`

**Test Steps:**
1. Arrange: dùng `ExpertLocationShareTestFactory.makeValidShareRequest()`
2. Act: gọi `service.shareLocation(userId, request)`
3. Assert: verify `locationShareRepository.save()` được gọi đúng 1 lần với entity có `expiresAt = sharedAt + 120 phút`

**Expected Result (PASS — hành vi đúng):**
- Response trả về `ExpertLocationShareResponse` với `active = true`, `expiresAt` không null
- `locationShareRepository.save()` gọi với `INSERT` semantics (entity mới, `locationShareId` do DB/`gen_random_uuid()` sinh)

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception bị throw dù có đủ điều kiện, hoặc response thiếu field `expiresAt`

**Current Status:** 🔴 Not written
**Implementation Note:** Verify thứ tự gọi: `findByUserId` → `findLatestActive` (consent) → `findByExpertProfileId` → `save`. KHÔNG được đảo thứ tự (consent phải check trước save).

---

### LOC-TC-002 — Share location bị từ chối khi thiếu consent hợp lệ

**Severity:** `CRITICAL`
**Legal:** `PDPA / Luật 91/2025 — Consent required before processing Location Sensitive-PII`
**Feature Under Test:** `ExpertLocationShareService.shareLocation()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationShareServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-LOC-IMP-147 §6.2 Sequence Diagram No Consent`, `ADR-LOC-101`, error code `LOC-003` (§10)

**Preconditions:**
- Mock `expertProfileRepository.findByUserId(userId)` → `makeVerifiedExpertProfile()`
- Mock `consentGrantRepository.findLatestActive(userId, 'LOCATION', 'SHARE')` → `Optional.empty()`

**Test Steps:**
1. Arrange: `makeValidShareRequest()`
2. Act: gọi `service.shareLocation(userId, request)`
3. Assert: exception `LocationConsentMissingException` được throw với code `LOC-003`

**Expected Result (PASS):**
- `LocationConsentMissingException` throw, message chứa "consent"
- `locationShareRepository.save()` **KHÔNG** được gọi (verify 0 interactions)

**Expected Result (FAIL):**
- Service ghi `expert_location_shares` dù thiếu consent (vi phạm PDPA)

**Current Status:** 🔴 Not written
**Implementation Note:** Test cũng cover trường hợp `consentGrantRepository` trả về consent đã `revokedAt != null` (dùng `makeRevokedLocationConsent()`, FX-004) — cùng expected behavior.

---

### LOC-TC-003 — Share lần 2 UPDATE row cũ (upsert, không tạo mới)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertLocationShareService.shareLocation()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationShareServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-LOC-102`, `CB-LOC-IMP-147 §6.3 Sequence Diagram Upsert`

**Preconditions:**
- Mock `expertProfileRepository.findByUserId()` → `makeVerifiedExpertProfile()`
- Mock `consentGrantRepository.findLatestActive()` → `makeValidLocationConsent()`
- Mock `locationShareRepository.findByExpertProfileId(expertProfileId)` → `Optional.of(makeExistingActiveShare(expertProfileId))` (FX-005)

**Test Steps:**
1. Arrange: request mới với toạ độ khác (`latitude=10.7780`)
2. Act: gọi `service.shareLocation(userId, newRequest)`
3. Assert: `save()` gọi với entity có **cùng `locationShareId`** như `FX-005` (`llllllll-0000-0000-0000-000000000001`), nhưng `latitude`/`longitude` đã được update

**Expected Result (PASS):**
- `locationShareRepository.save()` gọi đúng 1 lần, entity `locationShareId` KHÔNG đổi, các field khác (lat/lng/sharedAt/expiresAt) đã update

**Expected Result (FAIL):**
- Service tạo entity mới với `locationShareId` khác — vi phạm upsert invariant

**Current Status:** 🔴 Not written
**Implementation Note:** Audit action emit phải là `LOCATION_SHARE_UPDATED` (không phải `CREATED`) trong case này — verify qua `LOC-TC-004`.

---

### LOC-TC-004 — Audit log ghi đúng action khi share/update

**Severity:** `HIGH`
**Legal:** `CLAUDE.md audit mandate, PDPA`
**Feature Under Test:** `ExpertLocationShareService.shareLocation()` + `AuditService`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationShareServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-LOC-103`, migration `V20260705140000` (§5.2 TDS)

**Preconditions:**
- Mock đầy đủ như `LOC-TC-001` (case CREATE) và `LOC-TC-003` (case UPDATE)
- Mock `auditService.emit(...)`

**Test Steps:**
1. Act (case A — no existing share): gọi `shareLocation()`
2. Assert A: `auditService.emit()` gọi với `action = "LOCATION_SHARE_CREATED"`
3. Act (case B — existing share): gọi `shareLocation()` khi `findByExpertProfileId()` trả về existing
4. Assert B: `auditService.emit()` gọi với `action = "LOCATION_SHARE_UPDATED"`

**Expected Result (PASS):**
- Đúng action tương ứng cho mỗi case, payload chứa `expertProfileId` và `locationShareId`

**Expected Result (FAIL):**
- Audit không được gọi, hoặc gọi sai action, hoặc payload thiếu field bắt buộc

**Current Status:** 🔴 Not written
**Implementation Note:** Test này giả định migration mở rộng CHECK constraint đã Approved/chạy — nếu ADR-LOC-103 chưa Accepted khi implement, đánh dấu test **BLOCKED** thay vì viết stub giả định action string tuỳ ý.

---

### LOC-TC-005 — Unverified expert bị từ chối share

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertLocationShareService.shareLocation()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationShareServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-LOC-105`, SRS-3.3.6.1 Primary Actor "Verified Expert", error code `LOC-004`

**Preconditions:**
- Mock `expertProfileRepository.findByUserId(userId)` → `makeUnverifiedExpertProfile()` (FX-002, `verificationStatus='PENDING'`)

**Test Steps:**
1. Act: gọi `service.shareLocation(userId, makeValidShareRequest())`
2. Assert: `ExpertNotVerifiedException` throw với code `LOC-004`

**Expected Result (PASS):**
- Exception throw TRƯỚC khi check consent (fail-fast theo thứ tự §6.1 sequence — verify status trước) — `consentGrantRepository` KHÔNG được gọi

**Expected Result (FAIL):**
- Service vẫn ghi vị trí cho Expert chưa verified

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### LOC-TC-006 — Non-EXPERT role bị từ chối ở Controller layer

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `ExpertLocationShareController`
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-LOC-105`, `CB-LOC-IMP-147 §16 Authorization Matrix`, BR-RBAC

**Preconditions:**
- `@WebMvcTest(ExpertLocationShareController.class)` với `MOTHER` role JWT (FX-007)

**Test Steps:**
1. Act: `POST /api/v1/experts/me/location-shares` với JWT role `MOTHER`
2. Assert: response status `403`

**Expected Result (PASS):**
- HTTP 403, `@PreAuthorize("hasRole('EXPERT')")` chặn trước khi vào Service layer (verify Service KHÔNG được gọi)

**Expected Result (FAIL):**
- HTTP 200/201 hoặc request lọt vào Service

**Current Status:** 🔴 Not written
**Implementation Note:** Lặp lại test tương tự cho `GET`/`DELETE` endpoints.

---

### LOC-TC-007 — Validation: latitude/longitude ngoài range

**Severity:** `HIGH`
**Feature Under Test:** `ShareLocationRequest` DTO validation (Controller layer)
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-LOC-IMP-147 §8.1` (`@DecimalMin("-90.0") @DecimalMax("90.0")` cho latitude), error code `LOC-001`

**Preconditions:**
- Verified expert JWT (FX-006)

**Test Steps (Boundary Value Analysis):**
1. `latitude = 90.0000001` → Act POST → Assert 400, `LOC-001`
2. `latitude = -90.0000001` → Act POST → Assert 400
3. `longitude = 180.0000001` → Act POST → Assert 400
4. `latitude = 90.0` (boundary hợp lệ) → Assert KHÔNG 400 do latitude (có thể fail vì lý do khác nếu mock chưa đủ, nhưng KHÔNG fail do validation)

**Expected Result (PASS):**
- Case 1-3: HTTP 400 với `error.code = "LOC-001"`, `details` chứa field tương ứng
- Case 4: validation layer pass (không có lỗi 400 liên quan tới `latitude`)

**Expected Result (FAIL):**
- Request với toạ độ ngoài [-90,90]/[-180,180] vẫn được service xử lý

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### LOC-TC-008 — Validation: durationMinutes thiếu hoặc vượt max

**Severity:** `MEDIUM`
**Feature Under Test:** `ShareLocationRequest` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-LOC-IMP-147 §8.1` (`@NotNull @Min(1) @Max(1440)`)

**Preconditions:**
- Verified expert JWT (FX-006)

**Test Steps:**
1. `durationMinutes = null` → Act POST → Assert 400 `LOC-001`
2. `durationMinutes = 0` → Assert 400
3. `durationMinutes = 1441` → Assert 400
4. `durationMinutes = 1440` (boundary hợp lệ) → Assert KHÔNG 400 do field này

**Expected Result (PASS):** như mô tả trên.
**Expected Result (FAIL):** request thiếu `durationMinutes` vẫn tạo share với `expiresAt = null`.

**Current Status:** 🔴 Not written
**Implementation Note:** Test này encode Logic Issue L5 (§2) — `expiresAt` PHẢI luôn có giá trị sau khi share thành công dù cột DB cho phép NULL.

---

### LOC-TC-009 — Expiry semantics: getMyActiveShare() trả rỗng khi hết hạn

**Severity:** `HIGH`
**Feature Under Test:** `ExpertLocationShareService.getMyActiveShare()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationShareServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-LOC-IMP-147 §6.4 State Machine` (invariant: active = `expires_at IS NOT NULL AND expires_at > now()`)

**Preconditions:**
- Mock `locationShareRepository.findByExpertProfileId()` → entity với `expiresAt = Instant.now().minus(1, HOURS)` (đã hết hạn)

**Test Steps:**
1. Act: gọi `service.getMyActiveShare(userId)`
2. Assert: `Optional.empty()` trả về (dù row vẫn còn trong DB — không bị xoá)

**Expected Result (PASS):**
- `Optional.empty()`, row DB không bị ảnh hưởng (không side effect ngoài ý muốn)

**Expected Result (FAIL):**
- Trả về share đã hết hạn như đang active

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### LOC-TC-010 — Retry an toàn: gọi shareLocation() liên tiếp không gây duplicate/unsafe action

**Severity:** `HIGH`
**Legal:** `SRS Exception E3 — "no duplicate unsafe action"`
**Feature Under Test:** `ExpertLocationShareService.shareLocation()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationShareServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** SRS-3.3.6.1 §Exceptions E3

**Preconditions:**
- Mock đầy đủ điều kiện hợp lệ (như LOC-TC-001)
- `locationShareRepository.findByExpertProfileId()` mock trả về entity đã lưu ở lần gọi trước đó (simulate retry sau khi lần đầu đã thành công nhưng client không nhận được response)

**Test Steps:**
1. Act: gọi `shareLocation()` 3 lần liên tiếp với cùng request (simulate client retry)
2. Assert: `save()` gọi 3 lần nhưng LUÔN với cùng `locationShareId` (upsert, không tạo 3 row)

**Expected Result (PASS):**
- 3 lần gọi → 1 row trong DB (verified qua Integration test `LOC-TC-INT-001`), không lỗi/exception bất thường

**Expected Result (FAIL):**
- Mỗi lần retry tạo thêm 1 row mới

**Current Status:** 🔴 Not written
**Implementation Note:** Unit test verify hành vi mock; Integration test (`LOC-TC-INT-001`) verify thực tế trên DB thật.

---

### LOC-TC-011 — revokeMyShare() idempotent

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertLocationShareService.revokeMyShare()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationShareServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-LOC-IMP-147 §8.1` (`revokeMyShare()` javadoc — "Idempotent — gọi khi không có active share không throw lỗi")

**Preconditions:**
- Mock `locationShareRepository.findByExpertProfileId()` → `Optional.empty()` (chưa từng share)

**Test Steps:**
1. Act: gọi `service.revokeMyShare(userId)`
2. Assert: KHÔNG throw exception, method trả về bình thường (void)

**Expected Result (PASS):**
- Không exception, `save()`/`delete()` không được gọi (no-op vì không có gì để revoke)

**Expected Result (FAIL):**
- `NullPointerException` hoặc exception khác khi không tìm thấy share

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### SECURITY TEST CASES

---

### LOC-TC-SEC-001 — IDOR: client không thể tự chọn expertProfileId qua request body

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `PDPA — không cho phép ghi hộ vị trí của Expert khác`
**Feature Under Test:** `ExpertLocationShareController` / `ExpertLocationShareService`
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- JWT của Expert A (`uuuuuuuu-...01`) hợp lệ, verified
- `ShareLocationRequest` DTO KHÔNG có field `expertProfileId`/`userId` (theo §8.1 TDS — DTO chỉ có lat/lng/accuracy/status/duration)

**Test Steps (Attack Simulation):**
1. Gửi `POST /api/v1/experts/me/location-shares` với JWT Expert A, body cố tình thêm field lạ `"expertProfileId": "aaaaaaaa-...-999"` (Expert B's id, nếu DTO parser bỏ qua unknown field hoặc bind nhầm)
2. Kiểm tra response và DB state

**Expected Result (PASS = hệ thống an toàn):**
- Field `expertProfileId` trong body bị bỏ qua hoàn toàn (Jackson `FAIL_ON_UNKNOWN_PROPERTIES` hoặc DTO không có setter cho field này)
- Record được ghi/update **CHỈ** cho `expert_profile_id` resolve từ JWT của Expert A (`findByUserId(A.userId)`), không phải Expert B

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Location bị ghi vào `expert_profile_id` của Expert B — Expert A đã "share hộ" vị trí giả cho Expert khác

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### LOC-TC-INT-001 — Full upsert flow qua DB thật (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: shareLocation() lần 1 (INSERT) → shareLocation() lần 2 (UPDATE)`
**Test File:** `src/test/java/com/carebridge/backend/location/ExpertLocationShareIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động khi Spring context start (bao gồm `V20260705140000` nếu Approved)
- Seed: `expert_profiles` FX-001 (verified), `consent_grants` FX-003 (valid LOCATION/SHARE)

**Test Steps:**
1. Gọi `POST /api/v1/experts/me/location-shares` (lần 1) với `makeValidShareRequest()`
2. Assert response 201, lưu `locationShareId` trả về
3. Gọi `POST /api/v1/experts/me/location-shares` (lần 2) với toạ độ khác nhưng cùng JWT
4. Assert response 200 (hoặc 201 tuỳ convention — xem Open Item HTTP status), `locationShareId` GIỐNG lần 1
5. Query trực tiếp DB: `SELECT COUNT(*) FROM expert_location_shares WHERE expert_profile_id = ?`

**Expected Result (PASS):**
- `COUNT(*) = 1` sau cả 2 lần gọi
- `latitude`/`longitude` trong DB khớp với request lần 2 (đã update)

**Expected Result (FAIL):**
- `COUNT(*) = 2` (duplicate rows — vi phạm ADR-LOC-102)

**DB Assertion:**
```java
List<ExpertLocationShare> shares = locationShareRepository.findAll().stream()
    .filter(s -> s.getExpertProfileId().equals(expertProfileId))
    .toList();
assertThat(shares).hasSize(1);
assertThat(shares.get(0).getLatitude()).isEqualByComparingTo(new BigDecimal("10.7780"));
```

**Current Status:** 🔴 Not written

---

### LOC-TC-E2E-001 — E2E: Expert share → view → revoke

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST share → GET view → DELETE revoke → GET (404)`
**Test File:** `src/test/java/com/carebridge/backend/location/ExpertLocationShareE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- Testcontainers PostgreSQL, seed FX-001 + FX-003
- JWT hợp lệ cho verified Expert (FX-006)

**Test Steps:**
1. `POST /api/v1/experts/me/location-shares` với body hợp lệ → Assert 201
2. `GET /api/v1/experts/me/location-shares` → Assert 200, `active = true`
3. `DELETE /api/v1/experts/me/location-shares` → Assert 204
4. `GET /api/v1/experts/me/location-shares` → Assert 404, `error.code = "LOC-005"`

**Expected Result (PASS):**
- Toàn bộ 4 bước đúng status code và body như mô tả

**Expected Result (FAIL):**
- Bất kỳ bước nào sai status hoặc `GET` sau revoke vẫn trả `active=true`

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `LOC-TC-001` | `ExpertLocationShareServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-002` | `ExpertLocationShareServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-003` | `ExpertLocationShareServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-004` | `ExpertLocationShareServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-005` | `ExpertLocationShareServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-006` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-007` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-008` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-009` | `ExpertLocationShareServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-010` | `ExpertLocationShareServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-011` | `ExpertLocationShareServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-SEC-001` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-INT-001` | `ExpertLocationShareIntegrationTest.java` | `[ ]` | `[ ]` | |
| `LOC-TC-E2E-001` | `ExpertLocationShareE2ETest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertLocationShareService implements IExpertLocationShareService {

    @Override
    public ExpertLocationShareResponse shareLocation(UUID userId, ShareLocationRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public Optional<ExpertLocationShareResponse> getMyActiveShare(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public void revokeMyShare(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `LOC-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-006` | `403 from @PreAuthorize (before Service call)` | 🔴 FAIL *(Controller-level — should still fail because underlying wiring incomplete pre-implementation)* | ☐ FAIL ☐ PASS | |
| `LOC-TC-007` | `throw('Not implemented')` or validation-only | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-008` | `throw('Not implemented')` or validation-only | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-INT-001` | `throw('Not implemented')` (500 at API level) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-TC-E2E-001` | `throw('Not implemented')` (500 at API level) | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(điền khi thực hiện Red Phase)*
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-LOC-IMP-147` đã được review và approve
- [ ] Logic Issues (§2) đã được confirm với Principal Architect/TV4-Lâm
- [ ] Migration `V20260705140000` (audit enum extension) đã được TV1 review và approved trên staging — nếu chưa, `LOC-TC-004` và test liên quan audit BỊ BLOCKED
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ExpertLocationShareService`
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/toạ độ chính xác xuất hiện plaintext trong logs (INFO level)
- [ ] Verify consent-before-write invariant KHÔNG bao giờ bị vi phạm trong test suite (LOC-TC-002 xanh)
- [ ] Verify upsert invariant (1 active/expert) giữ đúng trong Integration test (LOC-TC-INT-001 xanh)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — không có shared mutable state giữa tests (dùng `ExpertLocationShareTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/SRS/schema)

### Suspension Criteria (Điều kiện tạm dừng)

- Migration `V20260705140000` chưa được TV1 approve → tạm hoãn `LOC-TC-004`, `LOC-TC-INT-001` phần audit assertion
- ADR-LOC-101 (type mismatch `consent_reference`) chưa có quyết định cuối từ Product Owner/DPO
- CI pipeline bị broken bởi thay đổi khác ngoài phạm vi UC147

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.audit_logs DROP CONSTRAINT audit_logs_action_check;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.audit_logs ADD CONSTRAINT audit_logs_action_check CHECK (((action)::text = ANY ((ARRAY['LOGIN','LOGOUT','OTP_SENT','OTP_VERIFIED','CONSENT_GRANTED','CONSENT_REVOKED','CREATE_HEALTH_RECORD','VIEW_HEALTH_RECORD','EXPERT_VERIFICATION','MODERATION_ACTION','AI_TRIAGE','PAYMENT','SECURITY_EVENT','VIEW_AUDIT_LOG']::character varying[])::text[])));"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260705140000';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/location/
git checkout -- src/main/resources/db/migration/V20260705140000__extend_audit_logs_action_for_location.sql
git checkout -- src/test/java/com/carebridge/backend/location/

# Gap vẫn OPEN → giữ nguyên entry trong tracking artifact tương ứng
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Cần verify tại thời điểm Red Gate thực thi | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — mọi assumption trace về ADR-LOC-10x | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — LOC-TC-006/007/008 chỉ verify routing/validation | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — mọi type reference từ TDS §8 | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec-review → TDD spec approved cho Red Phase
- [ ] Phát hiện AP ở giai đoạn Red Gate thực thi → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(chưa có — điền khi Red Gate thực thi)_ | | | | ☐ |

---

*TDD Spec — Draft. Chưa Approved. Total: 14 test cases (2 CRITICAL security/consent core, 5 CRITICAL/HIGH business logic, 1 Integration, 1 E2E, còn lại HIGH/MEDIUM validation & idempotency). Blocked items: `LOC-TC-004` (audit) phụ thuộc migration `V20260705140000` được Approve trước.*
