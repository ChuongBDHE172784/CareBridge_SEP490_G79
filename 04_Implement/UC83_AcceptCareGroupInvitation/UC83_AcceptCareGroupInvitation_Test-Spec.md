# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-83 Accept Care Group Invitation

**Document ID:** `CB-FAM-TDD-006`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC83_AcceptCareGroupInvitation/UC83_AcceptCareGroupInvitation_TDS.md` (CB-FAM-IMP-006)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` §3.3.3.1 (lines 3233-3252)
- Schema oracle: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` lines 730-765
- Dependency migration (not owned here): `V20260702090000__add_care_group_invite_token.sql` (UC-71)
- Sibling TDS/Test-Spec for style reference: `04_Implement/UC70_CreateCareGroup/`, `04_Implement/UC216_ViewCareGroupMembers/`

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo TDD spec cho UC-83 Accept Care Group Invitation (Draft) |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-83` |
| **Module** | `AcceptCareGroupInvitation — family` |
| **Spec gốc** | `CB-FAM-IMP-006` |
| **Priority** | 🟡 P2 (SRS Priority: Medium) |
| **Sprint** | `S3 Cross-Domain Integration` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `PII` (phone-number matching, family membership) |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, PDPA` — note: SRS row for UC-83 does NOT list BR-CONSULTATION (verified against SRS lines 3233-3252); this is a genuine difference from sibling UC-71/72/73 and is NOT added here artificially. |
| **Upstream Dependencies** | `UC-71 migration V20260702090000__add_care_group_invite_token.sql` (invite_token/invite_channel/invite_expires_at/invited_phone columns), `InviteStatus` enum extension (`REJECTED`, `EXPIRED`, also owned by UC-71) |
| **Downstream Consumers** | `UC-216 View Care Group Members` (reads resulting ACCEPTED status), future `UC-3.3.3.2 View Shared Data` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-FAM-IMP-006 §17`, `ADR-FAM-006/007/008` |
| **Constraints Injected** | Extend existing controller/service/repository only; no new migration (depends on UC-71's `V20260702090000`); conditional-update single-use accept (ADR-FAM-008); identity via `SecurityUtils.requireCurrentUserId`; error codes limited to `FAM-040`..`FAM-043`; identity-matching (not role-based) authorization |
| **Model** | `Claude (Sonnet)` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> **Dependency note:** This Test-Spec's fixtures assume UC-71's schema columns
> (`invite_token`, `invite_channel`, `invite_expires_at`, `invited_phone`) and `InviteStatus`
> enum values (`REJECTED`, `EXPIRED`) already exist. If UC-71 has not landed, all integration
> tests in this file are BLOCKED (see §6 Entry Criteria).

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS: "Lets a family member sign in or register and accept" — không rõ auth flow chi tiết | Existing auth/registration endpoints reused as-is (out of scope to redesign); UC-83 only starts after user is authenticated | Test giả định user đã có JWT hợp lệ (auth flow test coverage is out of scope — see TDS §Out of Scope); test riêng cho unauthenticated request → 401 |
| L2 | SRS: không đặc tả cụ thể cơ chế token/phone-based invite | UC-71 schema: `invite_token VARCHAR(64)`, `invite_channel VARCHAR(20)` (LINK\|QR\|PHONE), `invite_expires_at TIMESTAMPTZ`, `invited_phone VARCHAR(20)` | Test fixtures cover all 3 channel values; PHONE channel tests include phone-match/mismatch |
| L3 | SRS không đặc tả xử lý hết hạn | ADR-FAM-006: lazy expiry tại accept-time, không có scheduled job | Test: accept call trên expired PENDING invite phải transition sang EXPIRED VÀ trả `FAM-041`, không chỉ trả lỗi mà không transition |
| L4 | SRS không đặc tả race condition | ADR-FAM-008: conditional UPDATE `WHERE invitation_status='PENDING'`, không có `@Version` | Test concurrency: 2 threads gọi `acceptIfPending` cùng lúc trên cùng 1 row → đúng 1 thread thành công, thread kia nhận `rowsUpdated=0` → `FAM-042` |
| L5 | SRS không đặc tả identity binding cho LINK/QR vs PHONE | ADR-FAM-007 (Open, proposed default): LINK/QR không ràng buộc danh tính; PHONE ràng buộc phone-match | Test: LINK channel → bất kỳ authenticated user nào cũng accept được; PHONE channel → chỉ user có phone khớp mới accept được |
| L6 | `care_group_members.invitation_status` là `varchar(20)` không có CHECK constraint (verified `V1__init_schema.sql` — no CHECK found) | Không có DB-level guard chặn giá trị enum lạ | Test không dựa vào DB CHECK constraint để chặn giá trị sai — validate hoàn toàn ở tầng Java enum/service |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
AcceptCareGroupInvitation bao gồm các layer:
├── Domain (InviteStatus transitions — pure logic, no deps)
├── Application / Use Cases (CareGroupServiceImpl.acceptCareGroupInvitation — mock JPA Repository với Mockito)
├── Services (CareGroupAuthorizationPolicy.isPhoneMatchForInvite — mock Repository với Mockito)
├── Controller (CareGroupController.acceptInvitation — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full accept flow + concurrency race)
```

> **Out of scope for this Test-Spec** (per TDS §Out of Scope): auth/registration flow tests
> (assume existing, reused as-is), invite CREATION tests (UC-71's own Test-Spec), reject-invite
> tests (future UC-3.3.17.3), permission management tests (UC-72), task assignment tests (UC-73).

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-83` (§3.3.3.1, lines 3233-3252) | E1 access denied, E2 invalid/missing/expired/conflicting data, E3 external/network failure handling |
| `CB-FAM-IMP-006 ADR-FAM-006` | Lazy expiry transition test |
| `CB-FAM-IMP-006 ADR-FAM-007` | Token-vs-phone identity binding tests |
| `CB-FAM-IMP-006 ADR-FAM-008` | Concurrency/single-use test |
| `BR-RBAC / BR-PRIVACY` | Authenticated-only access; phone-match minimum-necessary check |
| `V1__init_schema.sql` lines 730-765 + UC-71 migration `V20260702090000` | Schema-derived fixtures for `care_group_members` columns |
| PDPA | No PII (phone number) leaked in error messages beyond match/mismatch boolean outcome |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path: PENDING + LINK channel + valid token → ACCEPTED | `CareGroupServiceImpl.acceptCareGroupInvitation()` | `FAM83-TC-001` |
| TC-COND-002 | Happy path: PENDING + QR channel + valid token → ACCEPTED | `CareGroupServiceImpl.acceptCareGroupInvitation()` | `FAM83-TC-002` |
| TC-COND-003 | Happy path: PENDING + PHONE channel + phone matches → ACCEPTED | `CareGroupServiceImpl.acceptCareGroupInvitation()`, `CareGroupAuthorizationPolicy.isPhoneMatchForInvite()` | `FAM83-TC-003` |
| TC-COND-004 | Token not found → `FAM-040` 404 | `CareGroupMemberRepository.findByInviteToken()` | `FAM83-TC-004` |
| TC-COND-005 | Token expired (PENDING, now > invite_expires_at) → lazy transition to EXPIRED + `FAM-041` 410 | `CareGroupServiceImpl` + `markExpiredIfPending()` | `FAM83-TC-005` |
| TC-COND-006 | Token already ACCEPTED → `FAM-042` 409 | `CareGroupServiceImpl` | `FAM83-TC-006` |
| TC-COND-007 | Token REVOKED → `FAM-042` 409 | `CareGroupServiceImpl` | `FAM83-TC-007` |
| TC-COND-008 | Token REJECTED → `FAM-042` 409 | `CareGroupServiceImpl` | `FAM83-TC-008` |
| TC-COND-009 | Token already EXPIRED (pre-existing) → `FAM-042` 409 (not `FAM-041`, since transition already happened) | `CareGroupServiceImpl` | `FAM83-TC-009` |
| TC-COND-010 | PHONE channel, phone mismatch → `FAM-043` 403 | `CareGroupAuthorizationPolicy.isPhoneMatchForInvite()` | `FAM83-TC-010` |
| TC-COND-011 | Concurrent double-accept race on same token → exactly 1 success, 1 `FAM-042` | `CareGroupMemberRepository.acceptIfPending()` | `FAM83-TC-011` |
| TC-COND-012 | Unauthenticated request → 401 | `CareGroupController.acceptInvitation()` + Spring Security | `FAM83-TC-012` |
| TC-COND-013 | Successful accept sets `joined_at` exactly once, matches accept timestamp | `CareGroupServiceImpl` | `FAM83-TC-013` |
| TC-COND-014 | Successful accept emits `CARE_GROUP_INVITATION_ACCEPTED` audit log | `AuditService.log()` | `FAM83-TC-014` |
| TC-COND-015 | Successful accept triggers `FcmService.sendToToken()` to owner (best-effort, does not fail transaction if FCM throws) | `FcmService` | `FAM83-TC-015` |
| TC-COND-016 | Token format boundary: empty/blank path variable → 400/404 (framework-level, not `FAM-040` necessarily — clarify at controller test) | `CareGroupController` | `FAM83-TC-016` |
| TC-COND-017 | Token enumeration attempt (random 64-char string not in DB) → `FAM-040`, no distinguishing info leak vs a real-but-differently-invalid token | `CareGroupServiceImpl` | `FAM83-TC-017` |
| TC-COND-018 | `CareGroupInvitationAccepted` event payload contains exactly careGroupId, careGroupMemberId, acceptedByUserId, acceptedAt (per shared batch context) | Event publishing | `FAM83-TC-018` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `invite_channel` values (LINK, QR, PHONE) | Each channel has distinct identity-binding behavior (ADR-FAM-007) |
| Boundary Value Analysis | `invite_expires_at` exactly at `now()`, 1 second before/after | Lazy expiry transition boundary (ADR-FAM-006) |
| State Transition Testing | `InviteStatus` FSM (PENDING → ACCEPTED/EXPIRED; terminal REVOKED/REJECTED/EXPIRED all reject with FAM-042) | Core business logic of this feature (§6.3 state machine) |
| Error Guessing | Token enumeration, replay of already-used token, phone spoofing attempt | Security-sensitive PII-adjacent endpoint |
| Concurrency / Race Testing | Two simultaneous `acceptIfPending()` calls on the same row | ADR-FAM-008 single-use enforcement |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `CareGroupMember{inviteStatus=PENDING, inviteChannel=LINK, inviteToken='tok-link-001', inviteExpiresAt=now()+1h}` | Happy path LINK |
| `FX-002` | DB seed | `CareGroupMember{inviteStatus=PENDING, inviteChannel=QR, inviteToken='tok-qr-001', inviteExpiresAt=now()+1h}` | Happy path QR |
| `FX-003` | DB seed | `CareGroupMember{inviteStatus=PENDING, inviteChannel=PHONE, inviteToken='tok-phone-001', invitedPhone='+84900000001', inviteExpiresAt=now()+1h}` | Happy path PHONE, matching authenticated user's phone |
| `FX-004` | DB seed | Same as FX-003 but authenticated user's verified phone = `'+84900000099'` (mismatch) | Phone mismatch case |
| `FX-005` | DB seed | `CareGroupMember{inviteStatus=PENDING, inviteToken='tok-expired-001', inviteExpiresAt=now()-1h}` | Expired token (lazy transition test) |
| `FX-006` | DB seed | `CareGroupMember{inviteStatus=ACCEPTED, inviteToken='tok-accepted-001'}` | Already-accepted conflict |
| `FX-007` | DB seed | `CareGroupMember{inviteStatus=REVOKED, inviteToken='tok-revoked-001'}` | Revoked conflict |
| `FX-008` | DB seed | `CareGroupMember{inviteStatus=REJECTED, inviteToken='tok-rejected-001'}` | Rejected conflict |
| `FX-009` | DB seed | `CareGroupMember{inviteStatus=EXPIRED, inviteToken='tok-preexpired-001'}` | Already-expired (pre-transitioned) conflict |
| `FX-010` | JWT / auth context | `{sub: 'user-fam-001', role: 'FAMILY', verifiedPhone: '+84900000001'}` | Authenticated Family Member identity |
| `FX-011` | env | none required (no HMAC/secret dependency for this feature) | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeCareGroupMember(...)
// Package: com.carebridge.backend.family (test sources)
// Shared factory name across UC-70/71/72/73/83 Test-Spec files.
// ═══════════════════════════════════════════════════════════

// CareGroupTestFactory.java
class CareGroupTestFactory {

    static CareGroup makeCareGroup() {
        CareGroup group = new CareGroup();
        group.setId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
        group.setOwnerUserId(UUID.fromString("00000000-0000-0000-0000-0000000000a0"));
        group.setGroupName("Test Care Group");
        group.setStatus(CareGroupStatus.ACTIVE);
        return group;
    }

    static CareGroup makeCareGroup(Consumer<CareGroup> overrides) {
        CareGroup group = makeCareGroup();
        overrides.accept(group);
        return group;
    }

    // Baseline PENDING member with a LINK-channel invite — sync with FX-001
    static CareGroupMember makeCareGroupMember() {
        CareGroupMember member = new CareGroupMember();
        member.setId(UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
        member.setCareGroupId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
        member.setUserId(null); // not yet bound to an accepting user until accepted
        member.setMemberRole(GroupMemberRole.MEMBER);
        member.setInviteStatus(InviteStatus.PENDING);
        member.setInviteChannel("LINK");
        member.setInviteToken("tok-link-001");
        member.setInviteExpiresAt(Instant.now().plusSeconds(3600));
        member.setInvitedPhone(null);
        return member;
    }

    // Overload để override specific fields (channel, status, expiry, phone, token)
    static CareGroupMember makeCareGroupMember(Consumer<CareGroupMember> overrides) {
        CareGroupMember member = makeCareGroupMember();
        overrides.accept(member);
        return member;
    }

    // Convenience overloads for common states used across FAM83-TC-* cases
    static CareGroupMember makeExpiredPendingMember() {
        return makeCareGroupMember(m -> {
            m.setInviteToken("tok-expired-001");
            m.setInviteExpiresAt(Instant.now().minusSeconds(3600));
        });
    }

    static CareGroupMember makePhoneChannelMember(String invitedPhone) {
        return makeCareGroupMember(m -> {
            m.setInviteChannel("PHONE");
            m.setInviteToken("tok-phone-001");
            m.setInvitedPhone(invitedPhone);
        });
    }

    static CareGroupMember makeTerminalStatusMember(InviteStatus status, String token) {
        return makeCareGroupMember(m -> {
            m.setInviteStatus(status);
            m.setInviteToken(token);
        });
    }
}
```

---

### FAM83-TC-001 — Happy path: LINK channel accept succeeds

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** SRS UC-83 Normal Flow steps 3-5; ADR-FAM-007 Option A (LINK/QR unbound identity)

**Preconditions:**
- FX-001 seeded: `CareGroupMember{inviteStatus=PENDING, inviteChannel=LINK, inviteToken='tok-link-001'}`
- FX-010: authenticated user context (any user, since LINK is identity-unbound)

**Test Steps:**
1. Arrange: `CareGroupTestFactory.makeCareGroupMember()` (LINK channel, PENDING), mock repository `findByInviteToken("tok-link-001")` returns it; mock `acceptIfPending(...)` returns 1.
2. Act: call `acceptCareGroupInvitation("tok-link-001", principal)`.
3. Assert: response has `inviteStatus="ACCEPTED"`, `joinedAt` non-null; repository `acceptIfPending` called exactly once with the member's id.

**Expected Result (PASS — hành vi đúng):**
- Returns `AcceptCareGroupInvitationResponse` with ACCEPTED status and non-null joinedAt.

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception thrown, or status remains PENDING, or `acceptIfPending` never invoked.

**Current Status:** 🔴 Not written
**Implementation Note:** No identity check needed for LINK channel per ADR-FAM-007.

---

### FAM83-TC-002 — Happy path: QR channel accept succeeds

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** ADR-FAM-007 Option A (QR treated identically to LINK — both token-based, unbound)

**Preconditions:** FX-002 seeded (QR channel, PENDING).

**Test Steps:**
1. Arrange: `makeCareGroupMember(m -> { m.setInviteChannel("QR"); m.setInviteToken("tok-qr-001"); })`.
2. Act: call `acceptCareGroupInvitation("tok-qr-001", principal)`.
3. Assert: same as FAM83-TC-001 — ACCEPTED, joinedAt set.

**Expected Result (PASS):** ACCEPTED, no phone-match check invoked for QR channel.
**Expected Result (FAIL):** Phone-match logic incorrectly invoked for QR channel (would indicate ADR-FAM-007 misimplementation).

**Current Status:** 🔴 Not written
**Implementation Note:** Service must gate the phone-match check strictly on `"PHONE".equals(inviteChannel)`.

---

### FAM83-TC-003 — Happy path: PHONE channel, phone matches

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`, `CareGroupAuthorizationPolicy.isPhoneMatchForInvite()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** ADR-FAM-007 Option A (PHONE channel bound to verified phone)

**Preconditions:** FX-003 seeded; FX-010 authenticated user with `verifiedPhone='+84900000001'` matching `invitedPhone`.

**Test Steps:**
1. Arrange: `makePhoneChannelMember("+84900000001")`; mock policy returns `true` for phone match.
2. Act: call `acceptCareGroupInvitation("tok-phone-001", principal)`.
3. Assert: ACCEPTED; `isPhoneMatchForInvite` called exactly once.

**Expected Result (PASS):** ACCEPTED, phone-match check invoked and returned true.
**Expected Result (FAIL):** Accept succeeds without invoking phone-match check (bypass bug) OR incorrectly rejects a valid match.

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### FAM83-TC-004 — Token not found → FAM-040

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** TDS §10 Error Codes (`FAM-040`, 404)

**Preconditions:** No `CareGroupMember` row with `inviteToken='nonexistent'`.

**Test Steps:**
1. Arrange: mock `findByInviteToken("nonexistent")` returns `Optional.empty()`.
2. Act: call `acceptCareGroupInvitation("nonexistent", principal)`.
3. Assert: `BusinessException` thrown with `HttpStatus.NOT_FOUND` and code `"FAM-040"`.

**Expected Result (PASS):** Exception with code FAM-040, HTTP 404.
**Expected Result (FAIL):** Wrong error code, wrong status, or no exception (silently returns something).

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### FAM83-TC-005 — Token expired → lazy transition to EXPIRED + FAM-041

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-FAM-006` (lazy expiry), TDS §10 (`FAM-041`, 410)

**Preconditions:** FX-005 seeded: PENDING, `inviteExpiresAt = now() - 1h`.

**Test Steps:**
1. Arrange: `makeExpiredPendingMember()`; mock `findByInviteToken` returns it; mock `markExpiredIfPending` returns 1.
2. Act: call `acceptCareGroupInvitation("tok-expired-001", principal)`.
3. Assert: `markExpiredIfPending` invoked exactly once with the member's id; exception thrown with code `"FAM-041"`, HTTP 410 (Gone).

**Expected Result (PASS):** `markExpiredIfPending` called (lazy transition happens), then FAM-041 raised.
**Expected Result (FAIL):** FAM-041 raised WITHOUT calling `markExpiredIfPending` (violates ADR-FAM-006 — DB state never actually transitions), or wrong error code.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the key ADR-FAM-006 behavior — must verify the lazy transition SQL call happens, not just the error code.

---

### FAM83-TC-006 — Already ACCEPTED → FAM-042

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** TDS §6.3 State Machine (terminal-ish transitions all reject with FAM-042)

**Preconditions:** FX-006 seeded: `inviteStatus=ACCEPTED`.

**Test Steps:**
1. Arrange: `makeTerminalStatusMember(InviteStatus.ACCEPTED, "tok-accepted-001")`.
2. Act: call `acceptCareGroupInvitation("tok-accepted-001", principal)`.
3. Assert: exception code `"FAM-042"`, HTTP 409; `acceptIfPending` NEVER called (short-circuited before the conditional update).

**Expected Result (PASS):** FAM-042, no further repository write attempted.
**Expected Result (FAIL):** Wrong code, or `acceptIfPending` called unnecessarily on an already-terminal row.

**Current Status:** 🔴 Not written

---

### FAM83-TC-007 — REVOKED → FAM-042

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** same as above
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** TDS §6.3 State Machine

**Preconditions:** FX-007 seeded: `inviteStatus=REVOKED`.
**Test Steps:** Same pattern as FAM83-TC-006 with `makeTerminalStatusMember(InviteStatus.REVOKED, "tok-revoked-001")`.
**Expected Result (PASS):** FAM-042, 409.
**Expected Result (FAIL):** Wrong code/status.
**Current Status:** 🔴 Not written

---

### FAM83-TC-008 — REJECTED → FAM-042

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** same as above
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** TDS §6.3 (REJECTED shown defensively even though not yet produced by any implemented UC in this batch)

**Preconditions:** FX-008 seeded: `inviteStatus=REJECTED`.
**Test Steps:** Same pattern with `makeTerminalStatusMember(InviteStatus.REJECTED, "tok-rejected-001")`.
**Expected Result (PASS):** FAM-042, 409.
**Expected Result (FAIL):** Wrong code/status, or NPE if enum extension not present (would indicate UC-71 dependency not landed — see §6 Entry Criteria).
**Current Status:** 🔴 Not written

---

### FAM83-TC-009 — Pre-existing EXPIRED → FAM-042 (not FAM-041)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** same as above
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-FAM-006` — distinguishes "expiring now" (FAM-041) from "already expired previously" (FAM-042, generic terminal-state conflict)

**Preconditions:** FX-009 seeded: `inviteStatus=EXPIRED` already (not PENDING).

**Test Steps:**
1. Arrange: `makeTerminalStatusMember(InviteStatus.EXPIRED, "tok-preexpired-001")`.
2. Act: call `acceptCareGroupInvitation("tok-preexpired-001", principal)`.
3. Assert: code `"FAM-042"` (NOT `"FAM-041"`, since the member is not currently PENDING); `markExpiredIfPending` never called (only invoked for PENDING+expired rows, per service logic branch order).

**Expected Result (PASS):** FAM-042 — this distinguishes "was already expired" from "expiring right now" (FAM-041), matching the branch condition `inviteStatus == PENDING && expiresAt < now()` in §11.3.
**Expected Result (FAIL):** Returns FAM-041 (would indicate status check is missing/wrong, e.g. checking only expiry timestamp without checking current status is PENDING).

**Current Status:** 🔴 Not written
**Implementation Note:** This test specifically guards against a subtle logic bug: checking `invite_expires_at < now()` without also checking `inviteStatus == PENDING` first would misclassify this case as FAM-041.

---

### FAM83-TC-010 — PHONE channel, phone mismatch → FAM-043

**Severity:** `CRITICAL`
**CWE:** `CWE-863 — Incorrect Authorization`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`, `CareGroupAuthorizationPolicy.isPhoneMatchForInvite()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-FAM-007` Option A; TDS §10 (`FAM-043`, 403)

**Preconditions:** FX-004: PHONE-channel member with `invitedPhone='+84900000001'`; authenticated user's `verifiedPhone='+84900000099'` (mismatch).

**Test Steps:**
1. Arrange: `makePhoneChannelMember("+84900000001")`; mock policy `isPhoneMatchForInvite(...)` returns `false`.
2. Act: call `acceptCareGroupInvitation("tok-phone-001", principal)`.
3. Assert: exception code `"FAM-043"`, HTTP 403; `acceptIfPending` never invoked.

**Expected Result (PASS = hệ thống an toàn):** 403 FAM-043, no state change, `invitedPhone`/`verifiedPhone` values not echoed back in the error message (only a generic mismatch message per §9.2).
**Expected Result (FAIL = lỗ hổng tồn tại):** Accept incorrectly succeeds despite phone mismatch (privacy/RBAC bypass), or the error message leaks the actual phone numbers being compared.

**Current Status:** 🔴 Not written

---

### FAM83-TC-011 — Concurrent double-accept race → single winner

**Severity:** `CRITICAL`
**Feature Under Test:** `CareGroupMemberRepository.acceptIfPending()` (integration-level, real DB)
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupInvitationAcceptConcurrencyIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-FAM-008` (conditional UPDATE, no `@Version`)

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migrations applied (including UC-71's `V20260702090000`).
- Seed one `PENDING` `CareGroupMember` row with `inviteToken='tok-race-001'`.

**Test Steps:**
1. Arrange: two `ExecutorService` threads, each independently invoking
   `careGroupService.acceptCareGroupInvitation("tok-race-001", differentPrincipals)` at
   approximately the same time (use a `CountDownLatch` to align start).
2. Act: run both threads concurrently, `await()` completion of both.
3. Assert: exactly one thread's result is a successful `ACCEPTED` response; the other thread's
   result is a `BusinessException` with code `"FAM-042"`. DB row shows `invitation_status='ACCEPTED'`
   exactly once (no double-processing of FCM/audit side effects beyond the single winner).

**DB Assertion:**
```java
CareGroupMember record = memberRepository.findById(savedId).orElseThrow();
assertThat(record.getInviteStatus()).isEqualTo(InviteStatus.ACCEPTED);
assertThat(record.getJoinedAt()).isNotNull();
// Only one audit log entry for CARE_GROUP_INVITATION_ACCEPTED for this member id
```

**Expected Result (PASS):** Exactly 1 success + 1 FAM-042, DB shows single consistent ACCEPTED state.
**Expected Result (FAIL):** Both threads report success (double-accept bug — race not actually prevented, e.g. service used check-then-act instead of the atomic conditional UPDATE).

**Current Status:** 🔴 Not written
**Implementation Note:** This is the definitive test for ADR-FAM-008 — do not mock the repository here; must run against a real DB via Testcontainers to catch true race conditions.

---

### FAM83-TC-012 — Unauthenticated request → 401

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `CareGroupController.acceptInvitation()` + Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupControllerAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** No `Authorization` header / invalid JWT.

**Test Steps (Attack Simulation):**
1. Send `POST /api/v1/care-groups/invitations/tok-link-001/accept` with no `Authorization` header.
2. Assert response status.
3. Assert no DB state changed (member remains PENDING).

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`, no service method invoked.
**Expected Result (FAIL = lỗ hổng tồn tại):** Request reaches `CareGroupServiceImpl` without a valid principal (would NPE or, worse, silently proceed).

**Current Status:** 🔴 Not written

---

### FAM83-TC-013 — Successful accept sets joined_at exactly once

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** SRS POST-2 (records/statuses updated); TDS §6.3 invariant ("joined_at chỉ được set đúng một lần")

**Preconditions:** FX-001 seeded (PENDING, LINK).

**Test Steps:**
1. Arrange: capture the `Instant` passed to `acceptIfPending(id, joinedAt)` via Mockito `ArgumentCaptor`.
2. Act: call `acceptCareGroupInvitation("tok-link-001", principal)`.
3. Assert: captured `joinedAt` is within a reasonable delta (e.g. 2 seconds) of test execution time; response `joinedAt` equals the captured value exactly.

**Expected Result (PASS):** `joinedAt` set once, consistent between DB call arg and response DTO.
**Expected Result (FAIL):** `joinedAt` null, or mismatched between repository call and response.

**Current Status:** 🔴 Not written

---

### FAM83-TC-014 — Successful accept emits audit log

**Severity:** `HIGH`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()` → `AuditService.log()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** SRS POST-3 (sensitive actions recorded for audit); shared batch context `AuditAction.CARE_GROUP_INVITATION_ACCEPTED`

**Preconditions:** FX-001 seeded.

**Test Steps:**
1. Arrange: mock `AuditService`.
2. Act: call `acceptCareGroupInvitation("tok-link-001", principal)`.
3. Assert: `auditService.log(AuditAction.CARE_GROUP_INVITATION_ACCEPTED, currentUserId, "CareGroupMember", member.getId().toString(), anyString())` invoked exactly once.

**Expected Result (PASS):** Audit log call verified with correct `AuditAction` enum value and actor id.
**Expected Result (FAIL):** Audit call missing, wrong `AuditAction`, or wrong actor id (e.g. logs the inviter instead of the accepting user).

**Current Status:** 🔴 Not written

---

### FAM83-TC-015 — FCM notification is best-effort (does not fail the transaction)

**Severity:** `MEDIUM`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()` → `FcmService.sendToToken()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** SRS Secondary Actor "Firebase Cloud Messaging"; TDS §11.3 Chặng 3 comment ("best-effort, does not fail the transaction on FCM error")

**Preconditions:** FX-001 seeded; mock `FcmService.sendToToken(...)` to throw a `RuntimeException` (simulating FCM outage).

**Test Steps:**
1. Arrange: as above, FCM mock throws.
2. Act: call `acceptCareGroupInvitation("tok-link-001", principal)`.
3. Assert: method still returns a successful `ACCEPTED` response; DB state still transitions; no exception propagates to the caller.

**Expected Result (PASS):** Accept succeeds despite FCM failure (E3 — external/network/server failure handled gracefully per SRS).
**Expected Result (FAIL):** FCM failure causes the whole accept to fail/rollback (violates SRS E3 guidance and degrades UX for an unrelated external dependency issue).

**Current Status:** 🔴 Not written
**Implementation Note:** Confirms whether FCM call should be wrapped in try/catch (best-effort) — **Open** decision, propose try/catch swallow-and-log as default; mark for Tech Lead confirmation alongside ADR-FAM-006/007/008.

---

### FAM83-TC-016 — Blank/empty token path variable boundary

**Severity:** `LOW`
**Feature Under Test:** `CareGroupController.acceptInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupControllerAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:** None.

**Test Steps:**
1. Send `POST /api/v1/care-groups/invitations//accept` (empty token segment) or `POST /api/v1/care-groups/invitations/%20/accept` (blank/whitespace token).
2. Assert response.

**Expected Result (PASS):** Framework-level 404 (no route match) for a truly empty segment, or service-level `FAM-040` for a whitespace token that fails `findByInviteToken`. Either is acceptable as long as no 500 error occurs and no stack trace leaks.
**Expected Result (FAIL):** 500 Internal Server Error, or exception detail leaked to client.

**Current Status:** 🔴 Not written

---

### FAM83-TC-017 — Token enumeration attempt (security)

**Severity:** `MEDIUM`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-203 — Observable Discrepancy`
**Feature Under Test:** `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** No matching row for a random 64-char guessed token.

**Test Steps (Attack Simulation):**
1. Generate a random 64-character alphanumeric string not present in DB.
2. Call `acceptCareGroupInvitation(randomToken, principal)`.
3. Compare response/timing characteristics against FAM83-TC-004 (genuinely-not-found case) — both should be indistinguishable (`FAM-040`, same response shape, no timing side-channel asserted beyond functional equivalence in this test suite).

**Expected Result (PASS = hệ thống an toàn):** `FAM-040` 404, identical error shape regardless of whether the token "looks like" a real one.
**Expected Result (FAIL = lỗ hổng tồn tại):** Different error code/message reveals whether a token format is "plausible" vs genuinely random (information leak aiding brute force).

**Current Status:** 🔴 Not written

---

### FAM83-TC-018 — CareGroupInvitationAccepted event payload shape

**Severity:** `MEDIUM`
**Feature Under Test:** Event publishing within `CareGroupServiceImpl.acceptCareGroupInvitation()`
**Test File:** `src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** Shared batch context §Domain events; TDS §7.3 Payload Schema

**Preconditions:** FX-001 seeded.

**Test Steps:**
1. Arrange: capture published event (via `ApplicationEventPublisher` mock or direct method call capture, depending on final §7.2 implementation choice).
2. Act: call `acceptCareGroupInvitation("tok-link-001", principal)`.
3. Assert: event payload contains exactly `careGroupId`, `careGroupMemberId`, `acceptedByUserId`, `acceptedAt` — no extra/missing fields, matching shared batch context contract used identically by sibling UC-71/72/73 event payloads.

**Expected Result (PASS):** Payload fields match exactly.
**Expected Result (FAIL):** Missing field (e.g. no `acceptedByUserId`) or extra undocumented field (contract drift from shared batch spec).

**Current Status:** 🔴 Not written
**Implementation Note:** If the codebase has no existing `ApplicationEventPublisher` usage pattern by the time this is implemented, this test may need adjustment per the Open note in TDS §7.2 — confirm actual publishing mechanism before finalizing this test's assertion style.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `FAM83-TC-001` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-002` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-003` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-004` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-005` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-006` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-007` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-008` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-009` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-010` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-011` | `CareGroupInvitationAcceptConcurrencyIntegrationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-012` | `CareGroupControllerAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-013` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-014` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-015` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-016` | `CareGroupControllerAcceptInvitationTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-017` | `CareGroupServiceImplAcceptInvitationSecurityTest.java` | `[ ]` | `[ ]` | |
| `FAM83-TC-018` | `CareGroupServiceImplAcceptInvitationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class CareGroupServiceImpl implements ICareGroupService {

    // ... existing createCareGroup/listMembers unaffected ...

    @Override
    public AcceptCareGroupInvitationResponse acceptCareGroupInvitation(String inviteToken, Principal principal) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `FAM83-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `FAM83-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM83-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM83-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM83-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `FAM83-TC-006`–`FAM83-TC-018` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | (same protocol, abbreviated for brevity — each TC must be individually verified, not batch-assumed) |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled during implementation phase)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` (to be generated during implementation phase)

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-FAM-IMP-006` đã được review và approve (currently `Draft`)
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] **UC-71 migration `V20260702090000__add_care_group_invite_token.sql` đã được approved và
      chạy thành công trên staging** — hard blocker, this Test-Spec's fixtures cannot be seeded
      without the `invite_token`/`invite_channel`/`invite_expires_at`/`invited_phone` columns
- [ ] `InviteStatus` enum extended with `REJECTED`, `EXPIRED` (UC-71 code dependency) confirmed
      present in the codebase before writing FAM83-TC-008/TC-009
- [ ] ADR-FAM-006/007/008 reviewed — currently all 3 `Proposed`/`Open`; at minimum a working
      default is accepted for test-writing purposes even if formal `Accepted` status is pending
- [ ] Test fixtures (§3 TDS-05) chuẩn bị sẵn sàng

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả 18 unit/security tests xanh (không có skip)
- [ ] `./mvnw verify` — integration test `FAM83-TC-011` (concurrency, Testcontainers) xanh
- [ ] Test coverage ≥ 80% lines cho `CareGroupServiceImpl.acceptCareGroupInvitation()` và mọi
      method mới thêm vào `CareGroupMemberRepository`/`CareGroupAuthorizationPolicy`
- [ ] Không có business logic trong `CareGroupController` (chỉ validation + mapping)
- [ ] Không có PII (invite_token, invited_phone, verifiedPhone) xuất hiện plaintext trong logs
- [ ] Đúng 1 accept thắng trong mọi concurrency test run (FAM83-TC-011 xanh 100%, không flaky)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả 18 tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi entity qua
      `CareGroupTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR) — verified
      present in every FAM83-TC-* block above

### Suspension Criteria (Điều kiện tạm dừng)

- UC-71 migration chưa apply lên môi trường test/staging (blocker dependency)
- ADR-FAM-006/007/008 bị Tech Lead/Security reject hoàn toàn (cần re-design trước khi viết lại test)
- CI pipeline bị broken bởi thay đổi khác ngoài phạm vi UC-83

---

## 7. Rollback Plan

```bash
# No migration owned by this feature — nothing to revert at the DB schema level here.
# If UC-71's migration itself needs rollback, follow UC-71's own Test-Spec rollback plan.

# Revert implementation files scoped to UC-83's additions
git checkout -- src/main/java/com/carebridge/backend/family/controller/CareGroupController.java
git checkout -- src/main/java/com/carebridge/backend/family/service/
git checkout -- src/main/java/com/carebridge/backend/family/repository/CareGroupMemberRepository.java
git checkout -- src/main/java/com/carebridge/backend/family/policy/CareGroupAuthorizationPolicy.java
git checkout -- src/test/java/com/carebridge/backend/family/CareGroupServiceImplAcceptInvitationTest.java
git checkout -- src/test/java/com/carebridge/backend/family/CareGroupControllerAcceptInvitationTest.java
git checkout -- src/test/java/com/carebridge/backend/family/CareGroupInvitationAcceptConcurrencyIntegrationTest.java

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu có)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (mọi TC trên có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ (to verify at Red Gate execution time) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (ADR-FAM-006/007/008 all referenced) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only assert HTTP status/auth, not business rules) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ (to verify: `CareGroupAuthorizationPolicy.isPhoneMatchForInvite` and repository methods `acceptIfPending`/`markExpiredIfPending` are NEW — must be created per TDS §8 before tests can compile) | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [x] Phát hiện AP tiềm ẩn (AP-AI-005 risk) → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-005` | (all TCs using `acceptIfPending`/`markExpiredIfPending`/`isPhoneMatchForInvite`) | These are NEW methods not yet in the codebase — tests will fail to compile until TDS §8 interfaces are created first | Create method signatures (empty/throw stubs per §5.1) BEFORE writing/running these tests, so Red Gate measures logic failure, not compile failure | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending UC-71 dependency landing, and Tech Lead/Security review of
ADR-FAM-006/007/008 (see companion TDS §3).*
