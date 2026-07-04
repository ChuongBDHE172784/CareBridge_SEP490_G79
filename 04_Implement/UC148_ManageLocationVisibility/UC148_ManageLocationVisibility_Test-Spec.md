# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC148 — Manage Location Visibility

**Document ID:** `CB-LOC-TDD-148`
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
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.6.2` — UC-148 functional requirements (dòng 3680-3699)
- `04_Implement/UC148_ManageLocationVisibility/UC148_ManageLocationVisibility_TDS.md` (`CB-LOC-IMP-148`) — Technical Specification
- `04_Implement/UC147_ShareExpertLocation/UC147_ShareExpertLocation_TDS.md` (`CB-LOC-IMP-147`) — write-side owner TDS, entity/consent/upsert convention
- `04_Implement/UC147_ShareExpertLocation/UC147_ShareExpertLocation_Test-Spec.md` — sibling Test-Spec, factory naming convention reference
- Luật 91/2025 (PDPA Vietnam) — location data as Sensitive-PII

> **Quy ước TDD:** Test cases mô tả TRƯỚC khi viết production code. Thứ tự bắt buộc: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵. Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC148 Manage Location Visibility |

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
| **Feature / Gap ID** | `UC148-MANAGE-VISIBILITY` |
| **Module** | `Manage Location Visibility — Bounded Context: location` |
| **Spec gốc** | `CB-LOC-IMP-148` |
| **Priority** | 🟡 P2 (Medium, theo SRS) |
| **Sprint** | `S3/S4 (theo function-spec-task-allocation.md — "expert location visibility" Sprint 3 onward, owner TV4-Lâm)` |
| **Milestone** | `M3 Alpha` *(Open — chưa có milestone cụ thể, kế thừa UC147 §1)* |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC147 ExpertLocationShareService`/`ExpertLocationShareRepository` (record owner), `consent_grants` (module `privacy`, TV1), `expert_profiles` (module `expert`, TV4) |
| **Downstream Consumers** | UC149/UC153 (Nearby Discovery — đọc `availability_status` để filter `HIDDEN`), UC150-152 (đọc gián tiếp) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-LOC-IMP-148 §17`, `ADR-LOC-201/202/203/204/205` |
| **Constraints Injected** | C1 (update-only, no INSERT), C2 (chỉ ghi availabilityStatus+expiresAt, không đổi toạ độ), C3 (re-verify consent), C4 (ownership from SecurityContext), C5 (verified-only), C6 (audit emission tái sử dụng literal của UC147), C7 (availabilityStatus enum giới hạn) |
| **Model** | `Claude (Sonnet)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.6.2 mô tả "visibility scope, duration, and display conditions" nhưng không định nghĩa cụ thể "display conditions" là gì | Schema V1 KHÔNG có cột structured cho display conditions (không có `business_hours`, `radius_km`, `visible_to_roles`, ...) — chỉ có `availability_status` varchar(20) free-text | Test PHẢI dùng `availability_status ∈ {AVAILABLE, BUSY, HIDDEN}` làm oracle cho "visibility scope/display condition", KHÔNG test business-hours/radius (không có implementation cho những thứ đó trong Draft này — out of scope, không phải test thiếu) |
| L2 | SRS mô tả "Manages ... duration" — có thể hiểu nhầm là UC148 tự set `expires_at` tuyệt đối (absolute timestamp) | TDS §8.1 (ADR-LOC-201/202) chọn `extendByMinutes` (relative extend) để tránh timezone bug từ client | Test verify `expiresAt` sau update = `existing.expiresAt + extendByMinutes` (relative), KHÔNG test client gửi absolute timestamp |
| L3 | UC147's TDS/Test-Spec không định nghĩa Java class `LocationShareNotFoundException` một cách tường minh (chỉ liệt kê mã lỗi `LOC-005` trong bảng lỗi) | Đọc `CB-LOC-IMP-147 §8.1` xác nhận `IExpertLocationShareService` chỉ khai báo `@throws` cho `LOC-003`/`LOC-004` — không có exception class riêng cho `LOC-005` được đặt tên tường minh trong Interface Specification | Test dùng tên class `LocationShareNotFoundException` như đề xuất mới của UC148 TDS §5.1 — nếu UC147 code thực tế đã có exception class khác cho `LOC-005` (implementation phase), test PHẢI đổi tên tương ứng, KHÔNG hard-code tên sai |
| L4 | UC147's audit migration `V20260705140000` hiện tại **CHỈ Ở TRẠNG THÁI PROPOSED** trong UC147 TDS/Test-Spec — chưa phải file thật đã chạy trên đĩa (xác nhận qua liệt kê toàn bộ `db/migration/`, không có file này) | Migration chưa tồn tại như file thật | Test liên quan audit (`LOC-VIS-TC-004`) PHẢI đánh dấu **BLOCKED** cho tới khi UC147's migration được approve và merge — không viết stub audit giả định action string tuỳ ý |
| L5 | UC148 chia sẻ chính xác entity/repository `ExpertLocationShare`/`ExpertLocationShareRepository` với UC147 — nếu test UC148 dùng tên field/getter khác với UC147's Test-Spec factory (`ExpertLocationShareTestFactory`), sẽ gây duplicate/inconsistent test fixtures | UC147 Test-Spec đã định nghĩa `ExpertLocationShareTestFactory.makeExistingActiveShare(expertProfileId)` | Test UC148 PHẢI tái sử dụng `ExpertLocationShareTestFactory` của UC147 (cùng class, cùng package test) thay vì tạo factory trùng lặp cho cùng entity — chỉ thêm factory method mới cho `UpdateVisibilityRequest` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Module `location` (UC148) bao gồm các layer:
├── Domain (ExpertLocationShare entity — TÁI SỬ DỤNG từ UC147, không test lại domain logic đã cover)
├── Service (ExpertLocationVisibilityService — mock ExpertLocationShareRepository/
│            ConsentGrantRepository/ExpertProfileRepository với Mockito)
├── Controller (ExpertLocationShareController.updateVisibility() — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — @SpringBootTest, migration thật, seed record từ UC147 flow trước)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-148` (§3.3.6.2) | Verified Expert manages visibility scope/duration/display conditions cho location share đã tồn tại; BR-RBAC |
| `CB-LOC-IMP-148 §3` ADR-LOC-201 → 205 | Update-only semantics, field write scope giới hạn, consent re-verify, audit emission tái sử dụng, authorization kế thừa |
| `CB-LOC-IMP-148 §10` | Error code oracle: LOC-001, LOC-003, LOC-004, LOC-005, LOC-006 (tái sử dụng từ UC147) |
| `CB-LOC-IMP-147 §5.2, §8` | Entity/Repository contract, error code baseline (không định nghĩa lại) |
| `V1__init_schema.sql` | Schema oracle: `expert_location_shares`, `consent_grants`, `audit_logs`, `expert_profiles` |
| PDPA / Luật 91/2025 | Consent-before-processing requirement cho Location Sensitive-PII, áp dụng cả khi UPDATE (không chỉ CREATE) |
| BR-RBAC (CareBridge project-wide) | Only `EXPERT` role, own-resource-only access |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| `TC-COND-101` | Update visibility thành công khi record đã tồn tại + consent hợp lệ + verified expert | `ExpertLocationVisibilityService.updateVisibility()` | `LOC-VIS-TC-001` |
| `TC-COND-102` | Update visibility KHÔNG BAO GIỜ thay đổi `latitude`/`longitude`/`accuracyMeters` | `ExpertLocationVisibilityService.updateVisibility()` | `LOC-VIS-TC-002` |
| `TC-COND-103` | Update visibility bị từ chối (404 LOC-005) khi Expert chưa từng gọi UC147 `shareLocation()` | `ExpertLocationVisibilityService.updateVisibility()` | `LOC-VIS-TC-003` |
| `TC-COND-104` | `availabilityStatus` ngoài tập `{AVAILABLE,BUSY,HIDDEN}` bị từ chối validation | `UpdateVisibilityRequest` DTO validation | `LOC-VIS-TC-007` |
| `TC-COND-105` | Audit log ghi đúng action `LOCATION_VISIBILITY_UPDATED` với payload đầy đủ (previous/new status) | `AuditService` + `ExpertLocationVisibilityService` | `LOC-VIS-TC-004` *(BLOCKED — phụ thuộc migration UC147)* |
| `TC-COND-106` | Unverified expert bị từ chối update visibility | `ExpertLocationVisibilityService.updateVisibility()` | `LOC-VIS-TC-005` |
| `TC-COND-107` | Non-EXPERT role bị từ chối ở Controller layer (403) | `ExpertLocationShareController` | `LOC-VIS-TC-006` |
| `TC-COND-108` | Update visibility bị từ chối (403 LOC-003) khi consent LOCATION/SHARE đã bị revoke sau share ban đầu | `ExpertLocationVisibilityService.updateVisibility()` | `LOC-VIS-TC-008` |
| `TC-COND-109` | Validation: `extendByMinutes` ngoài range `[1,1440]`, hoặc cả 2 field đều rỗng | `UpdateVisibilityRequest` DTO validation | `LOC-VIS-TC-009`, `LOC-VIS-TC-010` |
| `TC-COND-110` | `expertProfileId` KHÔNG thể bị client tự chọn qua request body (IDOR) | `ExpertLocationShareController`/`Service` | `LOC-VIS-TC-SEC-001` |
| `TC-COND-111` | `extendByMinutes` áp dụng relative extend đúng (`newExpiresAt = oldExpiresAt + N phút`), không phải absolute set | `ExpertLocationVisibilityService.updateVisibility()` | `LOC-VIS-TC-011` |
| `TC-COND-112` | Ownership: Expert A không thể update visibility của Expert B's share (không tồn tại route param, chỉ dựa vào `findByUserId` — verify không có cách nào bypass) | `ExpertLocationVisibilityService.updateVisibility()` | `LOC-VIS-TC-SEC-002` |
| `TC-COND-113` | E2E full flow: share (UC147) → update visibility (UC148) → view → verify toạ độ giữ nguyên | Full stack | `LOC-VIS-TC-E2E-001` |
| `TC-COND-114` | Integration: update-only invariant — số row `expert_location_shares` không tăng sau update | `ExpertLocationShareRepository` (integration) | `LOC-VIS-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `availabilityStatus` (valid enum vs invalid string), `extendByMinutes` (valid vs null vs out-of-range) | Đảm bảo mỗi partition được kiểm tra ít nhất 1 lần |
| Boundary Value Analysis | `extendByMinutes` = 0/1/1440/1441 | Giá trị biên dễ gây off-by-one error |
| State Transition Testing | State machine §6.4 TDS (AVAILABLE ↔ BUSY ↔ HIDDEN trong ACTIVE) | Verify mọi transition con hợp lệ, UC148 không tự chuyển ACTIVE↔EXPIRED/REVOKED |
| Error Guessing | IDOR attack, consent-revoked-after-share attempt, INSERT-instead-of-UPDATE regression | Attack vector + regression cho Sensitive-PII endpoint update path |
| Regression against UC147 | Verify UC148 KHÔNG phá vỡ hành vi `shareLocation()`/`getMyActiveShare()`/`revokeMyShare()` của UC147 | Cùng entity/repository — rủi ro side effect chéo module |

### TDS-05 — Test Data Requirements

> **Tái sử dụng tối đa fixture của UC147 Test-Spec (`ExpertLocationShareTestFactory`) — không định nghĩa trùng lặp FX-001 → FX-004 đã có.**

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-101` | DB seed (reuse UC147 `FX-001`) | `expert_profiles{expertProfileId: 'aaaa...01', userId: 'uuuu...01', verificationStatus: 'APPROVED'}` | Verified expert happy path |
| `FX-102` | DB seed (reuse UC147 `FX-002`) | `expert_profiles{expertProfileId: 'aaaa...02', userId: 'uuuu...02', verificationStatus: 'PENDING'}` | Unverified expert reject path |
| `FX-103` | DB seed (reuse UC147 `FX-003`) | `consent_grants{userId: 'uuuu...01', dataType: 'LOCATION', purpose: 'SHARE', revokedAt: null, expiryAt: now+30d}` | Valid consent |
| `FX-104` | DB seed | `consent_grants{userId: 'uuuu...01', dataType: 'LOCATION', purpose: 'SHARE', revokedAt: now-1h}` — consent bị revoke SAU KHI share ban đầu đã tồn tại | Consent-revoked-after-share reject path (khác UC147's FX-004 vì đây mô phỏng revoke muộn, không phải revoke từ đầu) |
| `FX-105` | DB seed (reuse UC147 `FX-005`) | `expert_location_shares{expertProfileId: 'aaaa...01', locationShareId: 'llll...01', latitude: 10.7700, longitude: 106.7000, availabilityStatus: 'AVAILABLE', expiresAt: now+1h}` | Existing active share — record để UC148 update lên |
| `FX-106` | DB seed | `expert_profiles{expertProfileId: 'aaaa...01'}` **KHÔNG có** `expert_location_shares` tương ứng (chưa từng gọi UC147) | `LOC-VIS-TC-003` — 404 LOC-005 path |
| `FX-107` | JWT (reuse UC147 `FX-006`) | `{sub: 'uuuu...01', roles: ['EXPERT']}` | Auth context — verified expert (owner của FX-105) |
| `FX-108` | JWT (reuse UC147 `FX-007`) | `{sub: 'uuuu...03', roles: ['MOTHER']}` | Auth context — wrong role |
| `FX-109` | JWT | `{sub: 'uuuu...99', roles: ['EXPERT']}` — Expert B, verified, KHÔNG có share nào liên quan đến `aaaa...01` | Ownership isolation test (LOC-VIS-TC-SEC-002) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> **Tái sử dụng `ExpertLocationShareTestFactory` từ UC147 Test-Spec** (`src/test/java/com/carebridge/backend/location/ExpertLocationShareTestFactory.java` — cùng class, KHÔNG tạo bản sao). UC148 chỉ bổ sung factory method mới cho `UpdateVisibilityRequest` vào **cùng class đó** (extend, không duplicate).

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// BỔ SUNG vào ExpertLocationShareTestFactory.java đã có từ UC147
// (KHÔNG tạo class mới — tái sử dụng makeVerifiedExpertProfile(),
//  makeUnverifiedExpertProfile(), makeValidLocationConsent(),
//  makeRevokedLocationConsent(), makeExistingActiveShare() đã có)
// ═══════════════════════════════════════════════════════════

class ExpertLocationShareTestFactory {

    // ... (các method đã có từ UC147, không lặp lại ở đây) ...

    // === UC148 additions ===

    static UpdateVisibilityRequest makeUpdateVisibilityRequest() {
        return makeUpdateVisibilityRequest(r -> {});
    }

    static UpdateVisibilityRequest makeUpdateVisibilityRequest(Consumer<UpdateVisibilityRequest> overrides) {
        UpdateVisibilityRequest request = new UpdateVisibilityRequest();
        request.setAvailabilityStatus("HIDDEN");
        request.setExtendByMinutes(60);
        overrides.accept(request);
        return request;
    }

    // ConsentGrant đã revoke SAU KHI share ban đầu tồn tại (khác makeRevokedLocationConsent()
    // của UC147 vốn mô phỏng "chưa từng có consent" — đây mô phỏng "có rồi bị thu hồi muộn")
    static ConsentGrant makeLateRevokedLocationConsent(UUID userId) {
        ConsentGrant grant = makeValidLocationConsent(userId);
        grant.setRevokedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return grant;
    }
}
```

---

### LOC-VIS-TC-001 — Update visibility thành công (happy path)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertLocationVisibilityService.updateVisibility()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-101`
**Oracle Source:** `CB-LOC-IMP-148 §6.1 Sequence Diagram Happy Path`, `ADR-LOC-201`, `ADR-LOC-202`

**Preconditions:**
- Mock `expertProfileRepository.findByUserId(userId)` → `makeVerifiedExpertProfile()` (FX-101)
- Mock `locationShareRepository.findByExpertProfileId(expertProfileId)` → `Optional.of(makeExistingActiveShare(expertProfileId))` (FX-105)
- Mock `consentGrantRepository.findLatestActive(userId, 'LOCATION', 'SHARE')` → `makeValidLocationConsent(userId)` (FX-103)

**Test Steps:**
1. Arrange: `makeUpdateVisibilityRequest()` (`availabilityStatus="HIDDEN"`, `extendByMinutes=60`)
2. Act: gọi `service.updateVisibility(userId, request)`
3. Assert: `locationShareRepository.save()` gọi đúng 1 lần với entity có `availabilityStatus="HIDDEN"`, `expiresAt = FX-105.expiresAt + 60min`, **cùng `locationShareId`** như FX-105

**Expected Result (PASS — hành vi đúng):**
- Response `ExpertLocationShareResponse` với `availabilityStatus="HIDDEN"`, `expiresAt` đã extend
- `latitude`/`longitude` trong response **giống hệt** FX-105 (không đổi)

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception bị throw dù có đủ điều kiện, hoặc `latitude`/`longitude` bị thay đổi

**Current Status:** 🔴 Not written
**Implementation Note:** Verify thứ tự gọi: `findByUserId` → `findByExpertProfileId` (record must exist first) → `findLatestActive` (consent) → `save`. Thứ tự khác với UC147 (UC147 check consent trước tìm record; UC148 check record tồn tại trước — vì cần biết record để so sánh state cũ/mới cho audit payload).

---

### LOC-VIS-TC-002 — Update visibility KHÔNG BAO GIỜ thay đổi toạ độ

**Severity:** `CRITICAL`
**Legal:** `PDPA — data minimization; write scope violation risk`
**Feature Under Test:** `ExpertLocationVisibilityService.updateVisibility()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-102`
**Oracle Source:** `ADR-LOC-201`, RG-3 delineation (`CB-LOC-IMP-148 §2`)

**Preconditions:**
- Giống `LOC-VIS-TC-001`
- `UpdateVisibilityRequest` DTO **KHÔNG có** setter cho `latitude`/`longitude` (theo §8.1 TDS)

**Test Steps:**
1. Arrange: `makeExistingActiveShare()` với `latitude=10.7700, longitude=106.7000` (FX-105)
2. Act: gọi `service.updateVisibility(userId, makeUpdateVisibilityRequest())`
3. Assert: entity được `save()` có `latitude`/`longitude` **giống hệt giá trị trước update** (bit-for-bit `BigDecimal` comparison)

**Expected Result (PASS):**
- `latitude`/`longitude`/`accuracyMeters` bất biến qua toàn bộ update

**Expected Result (FAIL):**
- Bất kỳ field toạ độ nào bị null hoá hoặc thay đổi giá trị

**Current Status:** 🔴 Not written
**Implementation Note:** Test này encode Logic Issue implicit trong ADR-LOC-201 — nếu implementation dùng cách build entity mới thay vì mutate entity đã fetch, dễ vô tình để `latitude`/`longitude` = null. Assert PHẢI so sánh giá trị cụ thể, không chỉ "not null".

---

### LOC-VIS-TC-003 — 404 khi Expert chưa từng share (update-only invariant)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertLocationVisibilityService.updateVisibility()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-103`
**Oracle Source:** `CB-LOC-IMP-148 §6.2 Sequence Diagram No Existing Share`, `ADR-LOC-202`, error code `LOC-005`

**Preconditions:**
- Mock `expertProfileRepository.findByUserId(userId)` → `makeVerifiedExpertProfile()` (FX-101/FX-106)
- Mock `locationShareRepository.findByExpertProfileId(expertProfileId)` → `Optional.empty()`

**Test Steps:**
1. Act: gọi `service.updateVisibility(userId, makeUpdateVisibilityRequest())`
2. Assert: `LocationShareNotFoundException` throw với code `LOC-005`

**Expected Result (PASS):**
- Exception throw, `locationShareRepository.save()` **KHÔNG** được gọi (verify 0 interactions — xác nhận không có INSERT ngầm)
- `consentGrantRepository` KHÔNG được gọi (fail-fast trước khi check consent — record-existence check đứng trước)

**Expected Result (FAIL):**
- Service tự tạo record mới với toạ độ null/default — vi phạm ADR-LOC-202

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### LOC-VIS-TC-004 — Audit log ghi đúng action khi update visibility

**Severity:** `HIGH`
**Legal:** `CLAUDE.md audit mandate, PDPA`
**Feature Under Test:** `ExpertLocationVisibilityService.updateVisibility()` + `AuditService`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java`
**TDD Phase:** 🔴 RED — **⚠️ BLOCKED**
**Condition Ref:** `TC-COND-105`
**Oracle Source:** `ADR-LOC-204`, migration `V20260705140000` (sở hữu bởi UC147's ADR-LOC-103)

**Preconditions:**
- Mock đầy đủ như `LOC-VIS-TC-001`
- Mock `auditService.emit(...)`
- **⚠️ Precondition ngoại lệ:** migration `V20260705140000` (thêm literal `LOCATION_VISIBILITY_UPDATED` vào `audit_logs.action` CHECK) PHẢI đã được TV1 approve và chạy trên staging — nếu chưa, test này ở trạng thái **BLOCKED**, không viết stub giả định string action tuỳ ý

**Test Steps:**
1. Act: gọi `service.updateVisibility(userId, request)` với `availabilityStatus` đổi từ `"AVAILABLE"` → `"HIDDEN"`
2. Assert: `auditService.emit()` gọi với `action = "LOCATION_VISIBILITY_UPDATED"`, payload chứa `previousAvailabilityStatus="AVAILABLE"`, `newAvailabilityStatus="HIDDEN"`

**Expected Result (PASS):**
- Đúng action, payload đầy đủ `expertProfileId`, `locationShareId`, `previousAvailabilityStatus`, `newAvailabilityStatus`

**Expected Result (FAIL):**
- Audit không được gọi, hoặc gọi sai action, hoặc payload thiếu field

**Current Status:** 🔴 Not written — **BLOCKED tại thời điểm viết spec vì `V20260705140000` chưa phải file migration thật trên đĩa (chỉ Proposed trong UC147 TDS)**
**Implementation Note:** KHÔNG viết implementation giả định action string khác để "cho test pass" — chờ migration UC147 Approved.

---

### LOC-VIS-TC-005 — Unverified expert bị từ chối update visibility

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertLocationVisibilityService.updateVisibility()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-106`
**Oracle Source:** `ADR-LOC-205`, SRS-3.3.6.2 Primary Actor "Verified Expert", error code `LOC-004`

**Preconditions:**
- Mock `expertProfileRepository.findByUserId(userId)` → `makeUnverifiedExpertProfile()` (FX-102, `verificationStatus='PENDING'`)

**Test Steps:**
1. Act: gọi `service.updateVisibility(userId, makeUpdateVisibilityRequest())`
2. Assert: `ExpertNotVerifiedException` throw với code `LOC-004`

**Expected Result (PASS):**
- Exception throw TRƯỚC khi check record tồn tại/consent (fail-fast theo verification status trước) — `locationShareRepository` KHÔNG được gọi

**Expected Result (FAIL):**
- Service vẫn cho phép Expert chưa verified sửa visibility

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### LOC-VIS-TC-006 — Non-EXPERT role bị từ chối ở Controller layer

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `ExpertLocationShareController.updateVisibility()`
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-107`
**Oracle Source:** `ADR-LOC-205`, `CB-LOC-IMP-148 §16 Authorization Matrix`, BR-RBAC

**Preconditions:**
- `@WebMvcTest(ExpertLocationShareController.class)` với `MOTHER` role JWT (FX-108)

**Test Steps:**
1. Act: `PATCH /api/v1/experts/me/location-shares/visibility` với JWT role `MOTHER`
2. Assert: response status `403`

**Expected Result (PASS):**
- HTTP 403, `@PreAuthorize("hasRole('EXPERT')")` chặn trước khi vào Service layer (verify Service KHÔNG được gọi)

**Expected Result (FAIL):**
- HTTP 200 hoặc request lọt vào Service

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### LOC-VIS-TC-007 — Validation: availabilityStatus ngoài enum cho phép

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateVisibilityRequest` DTO validation (Controller layer)
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-104`
**Oracle Source:** `CB-LOC-IMP-148 §8.1` (`@Pattern(regexp = "AVAILABLE|BUSY|HIDDEN")`), error code `LOC-001`

**Preconditions:**
- Verified expert JWT (FX-107), record đã tồn tại (FX-105)

**Test Steps:**
1. `availabilityStatus = "INVISIBLE"` (không thuộc enum) → Act PATCH → Assert 400 `LOC-001`
2. `availabilityStatus = "available"` (lowercase, không khớp Pattern case-sensitive) → Assert 400 *(Open — case sensitivity chưa có nguồn BR, đề xuất strict match)*
3. `availabilityStatus = "HIDDEN"` (hợp lệ) → Assert KHÔNG 400 do field này

**Expected Result (PASS):**
- Case 1-2: HTTP 400 với `error.code = "LOC-001"`
- Case 3: validation layer pass

**Expected Result (FAIL):**
- Request với giá trị tuỳ ý (vd: SQL injection string, script tag) được service xử lý mà không bị chặn ở validation layer

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### LOC-VIS-TC-008 — Update visibility bị từ chối khi consent bị revoke sau share ban đầu

**Severity:** `CRITICAL`
**Legal:** `PDPA / Luật 91/2025 — Consent revocation phải propagate tới mọi write path, không chỉ create`
**Feature Under Test:** `ExpertLocationVisibilityService.updateVisibility()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-108`
**Oracle Source:** `CB-LOC-IMP-148 §6.3 Sequence Diagram Consent Revoked`, `ADR-LOC-101` (kế thừa UC147), error code `LOC-003`

**Preconditions:**
- Mock `expertProfileRepository.findByUserId()` → `makeVerifiedExpertProfile()` (FX-101)
- Mock `locationShareRepository.findByExpertProfileId()` → `Optional.of(makeExistingActiveShare())` (FX-105 — record cũ VẪN tồn tại dù consent đã revoke)
- Mock `consentGrantRepository.findLatestActive()` → `Optional.empty()` (đã bị revoke, dùng `makeLateRevokedLocationConsent()` seed nếu integration test — unit test mock trực tiếp empty)

**Test Steps:**
1. Act: gọi `service.updateVisibility(userId, makeUpdateVisibilityRequest())`
2. Assert: `LocationConsentMissingException` throw với code `LOC-003`

**Expected Result (PASS):**
- Exception throw, `locationShareRepository.save()` **KHÔNG** được gọi — record cũ giữ nguyên trạng thái trước đó (không "mở lại" visibility)

**Expected Result (FAIL):**
- Service cho phép update visibility (kể cả set `HIDDEN`) dù consent đã revoke — đây là lỗi nghiêm trọng vì record vẫn "âm thầm" tồn tại và có thể bị đọc bởi consumer nếu logic filter sai

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test case CRITICAL riêng biệt của UC148 (không có ở UC147, vì UC147 chỉ check consent tại thời điểm CREATE — UC148 chứng minh consent được re-verify tại mọi UPDATE).

---

### LOC-VIS-TC-009 — Validation: extendByMinutes ngoài range

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateVisibilityRequest` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-109`
**Oracle Source:** `CB-LOC-IMP-148 §8.1` (`@Min(1) @Max(1440)`)

**Preconditions:**
- Verified expert JWT (FX-107), record tồn tại (FX-105)

**Test Steps (Boundary Value Analysis):**
1. `extendByMinutes = 0` → Act PATCH → Assert 400 `LOC-001`
2. `extendByMinutes = 1441` → Assert 400
3. `extendByMinutes = 1` (boundary hợp lệ) → Assert KHÔNG 400 do field này
4. `extendByMinutes = 1440` (boundary hợp lệ) → Assert KHÔNG 400 do field này

**Expected Result (PASS):** như mô tả trên.
**Expected Result (FAIL):** giá trị ngoài range vẫn được service xử lý, tạo `expiresAt` bất thường (quá xa tương lai hoặc quá khứ).

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### LOC-VIS-TC-010 — Validation: cả 2 field đều rỗng bị từ chối

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateVisibilityRequest` DTO validation (cross-field)
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-109`
**Oracle Source:** `CB-LOC-IMP-148 §8.1` (comment "Bắt buộc: ít nhất 1 trong 2 field PHẢI có giá trị")

**Preconditions:**
- Verified expert JWT (FX-107), record tồn tại (FX-105)

**Test Steps:**
1. Request body `{}` (cả `availabilityStatus` và `extendByMinutes` đều null) → Act PATCH → Assert 400 `LOC-001`

**Expected Result (PASS):**
- HTTP 400, `error.details` chứa message "At least one of availabilityStatus or extendByMinutes is required"

**Expected Result (FAIL):**
- Request rỗng vẫn được xử lý như no-op thành công (200) — gây nhầm lẫn cho client, không rõ ràng về ý định

**Current Status:** 🔴 Not written
**Implementation Note:** Dùng `@AssertTrue` cross-field validation ở DTO level (Bean Validation), không xử lý ở Service level để tránh business logic leak vào Controller path.

---

### LOC-VIS-TC-011 — extendByMinutes áp dụng relative extend đúng

**Severity:** `HIGH`
**Feature Under Test:** `ExpertLocationVisibilityService.updateVisibility()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-111`
**Oracle Source:** `CB-LOC-IMP-148 §8.1` (comment "relative extend, KHÔNG phải absolute set")

**Preconditions:**
- Mock `locationShareRepository.findByExpertProfileId()` → entity với `expiresAt = T0 + 30min` (FX-105 override)

**Test Steps:**
1. Act: gọi `service.updateVisibility(userId, makeUpdateVisibilityRequest(r -> r.setExtendByMinutes(60)))`
2. Assert: entity `save()` có `expiresAt = T0 + 30min + 60min = T0 + 90min` (KHÔNG phải `now() + 60min`)

**Expected Result (PASS):**
- `expiresAt` mới = `expiresAt` cũ + `extendByMinutes`, tính chính xác tới giây

**Expected Result (FAIL):**
- `expiresAt` bị set thành `now() + extendByMinutes` (absolute, sai theo ADR) hoặc bị ghi đè hoàn toàn thay vì cộng dồn

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test case dễ bị AI-generated code làm sai nhất (nhầm relative extend thành absolute set) — Oracle Source ghi rõ trong TDS §8.1 comment.

---

### SECURITY TEST CASES

---

### LOC-VIS-TC-SEC-001 — IDOR: client không thể tự chọn expertProfileId qua request body

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `PDPA — không cho phép sửa visibility hộ vị trí của Expert khác`
**Feature Under Test:** `ExpertLocationShareController.updateVisibility()` / `ExpertLocationVisibilityService`
**Test File:** `src/test/java/com/carebridge/backend/location/controller/ExpertLocationShareControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- JWT của Expert A (`uuuuuuuu-...01`) hợp lệ, verified
- `UpdateVisibilityRequest` DTO KHÔNG có field `expertProfileId`/`userId`/`locationShareId` (theo §8.1 TDS)

**Test Steps (Attack Simulation):**
1. Gửi `PATCH /api/v1/experts/me/location-shares/visibility` với JWT Expert A, body cố tình thêm field lạ `"expertProfileId": "aaaaaaaa-...-999"` (Expert B's id)
2. Kiểm tra response và DB state

**Expected Result (PASS = hệ thống an toàn):**
- Field lạ bị bỏ qua hoàn toàn; record được update **CHỈ** cho `expert_profile_id` resolve từ JWT của Expert A

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Visibility bị sửa cho `expert_profile_id` của Expert B

**Current Status:** 🔴 Not written

---

### LOC-VIS-TC-SEC-002 — Ownership isolation: Expert B không thể update share của Expert A

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-284 — Improper Access Control`
**Feature Under Test:** `ExpertLocationVisibilityService.updateVisibility()`
**Test File:** `src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-112`

**Preconditions:**
- JWT Expert B (`uuuuuuuu-...99`, FX-109), verified, KHÔNG có `expert_location_shares` record nào của chính mình
- Expert A (`aaaa...01`) có active share (FX-105)

**Test Steps (Attack Simulation):**
1. Act: gọi `service.updateVisibility(expertB.userId, request)` — Service tự resolve `findByUserId(expertB.userId)` → trả về Expert B's profile (`aaaa...99`, KHÔNG PHẢI `aaaa...01`)
2. `locationShareRepository.findByExpertProfileId(aaaa...99)` → `Optional.empty()` (Expert B chưa từng share)
3. Assert: `LocationShareNotFoundException` (LOC-005) — KHÔNG có cách nào Expert B chạm tới Expert A's record vì query luôn scope theo `expertProfileId` resolve từ chính JWT của caller

**Expected Result (PASS = hệ thống an toàn):**
- Expert B nhận `404 LOC-005` cho chính resource của mình, KHÔNG BAO GIỜ nhận/sửa được resource của Expert A — vì thiết kế không có tham số nào (path/body) cho phép chọn `expertProfileId` khác

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Có bất kỳ code path nào cho phép truyền `expertProfileId`/`locationShareId` từ request để Service query record của actor khác

**Current Status:** 🔴 Not written
**Implementation Note:** Test này khẳng định lại nguyên tắc thiết kế "không route param, chỉ SecurityContext" — không phải test hành vi động, mà test **kiến trúc** (đảm bảo không có backdoor param).

---

### INTEGRATION TEST CASES

---

### LOC-VIS-TC-INT-001 — Update-only invariant qua DB thật (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: shareLocation() (UC147, INSERT) → updateVisibility() (UC148, UPDATE) x2`
**Test File:** `src/test/java/com/carebridge/backend/location/ExpertLocationVisibilityIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-114`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động (bao gồm `V20260705140000` nếu Approved — nếu chưa, audit assertion trong test này BỊ SKIP, phần còn lại vẫn chạy)
- Seed: `expert_profiles` FX-101 (verified), `consent_grants` FX-103 (valid LOCATION/SHARE)

**Test Steps:**
1. Gọi `POST /api/v1/experts/me/location-shares` (UC147) → Assert 201, lưu `locationShareId`
2. Gọi `PATCH /api/v1/experts/me/location-shares/visibility` với `{"availabilityStatus": "HIDDEN"}` (UC148, lần 1) → Assert 200
3. Gọi `PATCH /api/v1/experts/me/location-shares/visibility` với `{"extendByMinutes": 30}` (UC148, lần 2) → Assert 200
4. Query trực tiếp DB: `SELECT COUNT(*) FROM expert_location_shares WHERE expert_profile_id = ?`

**Expected Result (PASS):**
- `COUNT(*) = 1` sau toàn bộ 3 lần gọi (1 INSERT từ UC147 + 2 UPDATE từ UC148)
- `locationShareId` giống nhau xuyên suốt cả 3 response
- `latitude`/`longitude` trong DB giống hệt giá trị từ bước 1 (UC147), không bị bước 2-3 (UC148) thay đổi
- `availability_status = 'HIDDEN'`, `expires_at` = giá trị ban đầu + 30 phút

**Expected Result (FAIL):**
- `COUNT(*) > 1` (UC148 vô tình INSERT thay vì UPDATE — vi phạm ADR-LOC-202)
- `latitude`/`longitude` bị thay đổi bởi UC148 (vi phạm ADR-LOC-201)

**DB Assertion:**
```java
List<ExpertLocationShare> shares = locationShareRepository.findAll().stream()
    .filter(s -> s.getExpertProfileId().equals(expertProfileId))
    .toList();
assertThat(shares).hasSize(1);
assertThat(shares.get(0).getLatitude()).isEqualByComparingTo(originalLatitude);
assertThat(shares.get(0).getLongitude()).isEqualByComparingTo(originalLongitude);
assertThat(shares.get(0).getAvailabilityStatus()).isEqualTo("HIDDEN");
```

**Current Status:** 🔴 Not written

---

### LOC-VIS-TC-E2E-001 — E2E: Expert share → update visibility → view → verify toạ độ bất biến

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST share (UC147) → PATCH visibility (UC148) → GET view → verify`
**Test File:** `src/test/java/com/carebridge/backend/location/ExpertLocationVisibilityE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-113`

**Preconditions:**
- Testcontainers PostgreSQL, seed FX-101 + FX-103
- JWT hợp lệ cho verified Expert (FX-107)

**Test Steps:**
1. `POST /api/v1/experts/me/location-shares` với body hợp lệ → Assert 201, lưu `latitude0`/`longitude0`/`expiresAt0`
2. `PATCH /api/v1/experts/me/location-shares/visibility` với `{"availabilityStatus": "BUSY", "extendByMinutes": 45}` → Assert 200
3. `GET /api/v1/experts/me/location-shares` → Assert 200, `availabilityStatus = "BUSY"`, `expiresAt = expiresAt0 + 45min`, `latitude = latitude0`, `longitude = longitude0`
4. `PATCH ... visibility` với `{"availabilityStatus": "HIDDEN"}` (không extend) → Assert 200, `expiresAt` KHÔNG đổi so với bước 3

**Expected Result (PASS):**
- Toàn bộ 4 bước đúng status code và body; toạ độ bất biến xuyên suốt; `expiresAt` chỉ đổi khi có `extendByMinutes`

**Expected Result (FAIL):**
- Bất kỳ bước nào sai status, hoặc toạ độ bị thay đổi bởi UC148, hoặc `expiresAt` đổi dù không gửi `extendByMinutes`

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `LOC-VIS-TC-001` | `ExpertLocationVisibilityServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-002` | `ExpertLocationVisibilityServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-003` | `ExpertLocationVisibilityServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-004` | `ExpertLocationVisibilityServiceTest.java` | `[ ]` *(BLOCKED)* | `[ ]` | Chờ migration `V20260705140000` (UC147) |
| `LOC-VIS-TC-005` | `ExpertLocationVisibilityServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-006` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-007` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-008` | `ExpertLocationVisibilityServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-009` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-010` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-011` | `ExpertLocationVisibilityServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-SEC-001` | `ExpertLocationShareControllerTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-SEC-002` | `ExpertLocationVisibilityServiceTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-INT-001` | `ExpertLocationVisibilityIntegrationTest.java` | `[ ]` | `[ ]` | |
| `LOC-VIS-TC-E2E-001` | `ExpertLocationVisibilityE2ETest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertLocationVisibilityService implements IExpertLocationVisibilityService {

    @Override
    public ExpertLocationShareResponse updateVisibility(UUID userId, UpdateVisibilityRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `LOC-VIS-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-004` | `throw('Not implemented')` (BLOCKED — do not run until migration ready) | 🔴 FAIL / SKIP | ☐ FAIL ☐ PASS ☐ SKIP | |
| `LOC-VIS-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-006` | `403 from @PreAuthorize (before Service call)` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-007` | `throw('Not implemented')` or validation-only | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-009` | `throw('Not implemented')` or validation-only | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-010` | `throw('Not implemented')` or validation-only | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-SEC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-SEC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-INT-001` | `throw('Not implemented')` (500 at API level) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `LOC-VIS-TC-E2E-001` | `throw('Not implemented')` (500 at API level) | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(điền khi thực hiện Red Phase)*
- Tất cả FAIL (trừ `LOC-VIS-TC-004` BLOCKED)? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-LOC-IMP-148` đã được review và approve
- [ ] Logic Issues (§2) đã được confirm với Principal Architect/TV4-Lâm
- [ ] **UC147's TDS/Test-Spec (`CB-LOC-IMP-147`) đã được review** — UC148 phụ thuộc trực tiếp vào entity/repository/consent convention của UC147
- [ ] Migration `V20260705140000` (audit enum extension, sở hữu UC147) đã được TV1 review và approved trên staging — nếu chưa, `LOC-VIS-TC-004` BỊ BLOCKED (các test khác vẫn có thể tiến hành)
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị, tái sử dụng `ExpertLocationShareTestFactory` của UC147

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip, trừ `LOC-VIS-TC-004` nếu vẫn BLOCKED)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ExpertLocationVisibilityService`
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/toạ độ chính xác xuất hiện plaintext trong logs (INFO level)
- [ ] Verify update-only invariant (0 INSERT từ UC148) giữ đúng trong Integration test (`LOC-VIS-TC-INT-001` xanh)
- [ ] Verify toạ độ bất biến qua toàn bộ UC148 test suite (`LOC-VIS-TC-002` xanh)
- [ ] Verify consent re-verification tại UPDATE-time (không chỉ CREATE-time) hoạt động đúng (`LOC-VIS-TC-008` xanh)
- [ ] Regression: UC147's test suite (`ExpertLocationShareServiceTest`, `ExpertLocationShareControllerTest`) vẫn xanh sau khi thêm route UC148 vào cùng Controller

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement (trừ BLOCKED)
- [ ] **Contract Existence** — mọi class được inject đều tồn tại: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — không có shared mutable state giữa tests (dùng `ExpertLocationShareTestFactory` mở rộng, không duplicate)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/SRS/schema)

### Suspension Criteria (Điều kiện tạm dừng)

- Migration `V20260705140000` (sở hữu UC147) chưa được TV1 approve → tạm hoãn `LOC-VIS-TC-004`
- UC147's TDS/code chưa Approved/implement — UC148 KHÔNG THỂ implement trước UC147 (hard dependency trên `ExpertLocationShareRepository`/entity đã tồn tại)
- RG-6 Open Item (ADR-LOC-203, display conditions) chưa có quyết định Product Owner → tạm hoãn mọi mở rộng ngoài `{AVAILABLE,BUSY,HIDDEN}`
- CI pipeline bị broken bởi thay đổi khác ngoài phạm vi UC148

---

## 7. Rollback Plan

```bash
# UC148 không có migration riêng — chỉ revert code
git checkout -- src/main/java/com/carebridge/backend/location/service/IExpertLocationVisibilityService.java
git checkout -- src/main/java/com/carebridge/backend/location/service/impl/ExpertLocationVisibilityService.java
git checkout -- src/main/java/com/carebridge/backend/location/dto/request/UpdateVisibilityRequest.java
git checkout -- src/main/java/com/carebridge/backend/location/event/ExpertLocationVisibilityUpdated.java
git checkout -- src/main/java/com/carebridge/backend/location/controller/ExpertLocationShareController.java
git checkout -- src/test/java/com/carebridge/backend/location/service/ExpertLocationVisibilityServiceTest.java
git checkout -- src/test/java/com/carebridge/backend/location/ExpertLocationVisibilityIntegrationTest.java
git checkout -- src/test/java/com/carebridge/backend/location/ExpertLocationVisibilityE2ETest.java

# Revert deployment
kubectl rollout undo deployment/carebridge-api

# Gap vẫn OPEN → giữ nguyên entry trong tracking artifact tương ứng
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Cần verify tại thời điểm Red Gate thực thi | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — mọi assumption trace về ADR-LOC-20x | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — `LOC-VIS-TC-006/007/009/010` chỉ verify routing/validation | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase, hoặc tạo entity/repository trùng lặp với UC147 | ☑ Không phát hiện — mọi type reference từ TDS §8, tái sử dụng entity của UC147 (không duplicate) | G-3 |
| AP-AI-006 | Duplicate Contract | Test factory tạo lại `ExpertLocationShareTestFactory` mới thay vì extend factory UC147 đã có | ☑ Không phát hiện — §4 Props Isolation ghi rõ "BỔ SUNG vào... đã có từ UC147" | G-0 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec-review → TDD spec approved cho Red Phase
- [ ] Phát hiện AP ở giai đoạn Red Gate thực thi → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(chưa có — điền khi Red Gate thực thi)_ | | | | ☐ |

---

*TDD Spec — Draft. Chưa Approved. Total: 15 test cases (3 CRITICAL security/ownership core: SEC-001, SEC-002, TC-008 consent-revoked; 5 CRITICAL business logic: TC-001/002/003/005/006; 1 Integration; 1 E2E; còn lại HIGH/MEDIUM validation & relative-extend semantics). Blocked items: `LOC-VIS-TC-004` (audit) phụ thuộc migration `V20260705140000` (sở hữu UC147) được Approve trước. Hard dependency: toàn bộ UC148 test suite phụ thuộc UC147's entity/repository đã được implement trước (UC148 không thể code trước UC147).*
