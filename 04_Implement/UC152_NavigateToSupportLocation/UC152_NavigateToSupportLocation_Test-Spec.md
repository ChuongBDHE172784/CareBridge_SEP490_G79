# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC152 — Navigate to Support Location — Test Specification

**Document ID:** `CB-MAP-TDD-005`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Tech Lead — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — schema source (`emergency_events` L1081-1095, `location_snapshots` L1097-1108)
- `04_Implement/UC152_NavigateToSupportLocation/UC152_NavigateToSupportLocation_TDS.md` (`CB-MAP-IMP-005`) — companion TDS, this spec implements §6/§8/§9/§10/§16/§17 of it
- `04_Implement/UC129_CalculateDistanceRouteAndETA/UC129_CalculateDistanceRouteAndETA_TDS.md` — `IMapProviderService` contract, REUSED not reimplemented
- `04_Implement/UC151_ContactNearbyUser/UC151_ContactNearbyUser_Test-Spec.md` — sibling spec, same batch, gating-policy reuse and style reference
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.6.4 — UC-152 functional requirements
- `CLAUDE.md` — RBAC/audit/consent delivery rules

> **Quy ước TDD:** Viết test TRƯỚC production code. Thứ tự: viết test → chạy →
> xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật — chỉ SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo tài liệu — Test-Spec cho UC152 (Draft) |
| 2026-07-02 | AI Agent | **Đóng Open Item (gating mechanism):** Product Owner đã CONFIRMED cơ chế "accept"/gating (`selected_expert_id`) — xem TDS §1/§2 CHANGELOG (ADR-MAP-213 Accepted). L3 (§2) cập nhật trạng thái; Suspension Criteria liên quan đã gỡ bỏ. `consent_status` enum values (L1) KHÔNG thay đổi — vẫn Open. |

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
| **Feature / Gap ID** | `UC-152` |
| **Module** | `Navigate to Support Location — map` (Bounded Context: `map`, shared with UC63/UC64/UC129/UC150/UC151) |
| **Spec gốc** | `CB-MAP-IMP-005` |
| **Priority** | 🟠 Medium (per SRS), but Sensitive-PII/consent-critical |
| **Sprint** | `Sprint 3` — TV4-Lâm |
| **Milestone** | Sprint 3 — final step of the UC150→UC151→UC152 nearby-support chain |
| **Data Classification** | `Sensitive-PII` (exact coordinates) |
| **Compliance Scope** | `PDPA (Luật 91/2025)`, `BR-RBAC`, `BR-PRIVACY` (explicit in SRS §3.3.6.4) |
| **Upstream Dependencies** | `UC129 IMapProviderService` (BLOCKING — must exist first), `UC151 SupportRequestGatingPolicy` (BLOCKING — gating reuse), `emergency_events`, `location_snapshots` |
| **Downstream Consumers** | None — terminal client-side navigation launch |
| **Platform** | Backend (Java 21/Spring Boot) + Mobile (Flutter/Dart) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC152_NavigateToSupportLocation_TDS.md §17` (C1-C6), ADR-MAP-210 through 214 |
| **Constraints Injected** | Thin orchestration only (C1), gating-before-everything (C2), consent-before-map-call (C3), no PII beyond coordinates in response (C4), client-side launch only (C5), server-resolved identity (C6) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.6.4 does not name the exact `consent_status` valid/invalid enum values | TDS ADR-MAP-212 proposes `{NULL, 'REVOKED', 'EXPIRED'}` = invalid, everything else = valid — explicitly flagged Open pending Product/UC141/UC63 owner confirmation | Tests use exactly this proposed set; a test asserting a different enum boundary would contradict the TDS's own documented assumption — flagged as the same Open item, not silently resolved differently |
| L2 | SRS does not specify the source of the Expert's own (origin) coordinates for route calculation | ADR-MAP-210 chose Option (a): client-supplied query parameter, never persisted, not read from `expert_location_shares` | Tests assert `originLatitude`/`originLongitude` are accepted as query params and NEVER written to any table (structural DB-write-absence check) |
| L3 | Gating mechanism (`selected_expert_id`) is inherited, shared with UC150/UC151 | **[RESOLVED 2026-07-02]** CONFIRMED by Product Owner as the official mechanism (ADR-MAP-213 Accepted) — TDS explicitly states this was resolved simultaneously across UC150/UC151/UC152, not independently by UC152 | Tests encode the CONFIRMED gating behavior (`selected_expert_id == currentExpertProfileId`) consistently with UC151's Test-Spec fixtures — any future change to this mechanism must update all three Test-Specs together, not just this one |
| L4 | Order-of-operations (gating vs consent vs map-service call) is not explicit in generic template flows | ADR-MAP-210/212/213 mandate: gating FIRST (fail-fast, avoid unnecessary PII query), consent SECOND, `IMapProviderService.calculateRoute()` LAST (avoid unnecessary external API cost) | `UC152-TC-009` explicitly asserts this call order via Mockito `InOrder` verification — this ordering is itself a testable invariant, not just documentation |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Navigate to Support Location bao gồm các layer:
├── Domain (SupportRequestGatingPolicy — REUSED from UC151, not reimplemented)
├── Application (SupportNavigationService — mock IEmergencyEventRepository + ILocationSnapshotRepository + IMapProviderService + gating policy với Mockito)
├── Controller (SupportNavigationController — @WebMvcTest, mock Service)
├── Integration (Testcontainers PostgreSQL — full gating+consent+route flow)
└── Mobile (SupportNavigationMobileService — flutter_test, mirrors UC64 launch pattern)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử (per TDS §13 mapping table)

| Source | Items Derived | Condition Ref |
|--------|--------------|----------------|
| `ADR-MAP-210` | Thin orchestration — MUST call `IMapProviderService.calculateRoute()`, never reimplement | `TC-COND-001, 002` |
| `ADR-MAP-211` | Client-side navigation launch (Mobile) | `TC-COND-003` |
| `ADR-MAP-212` | Consent gate — BR-PRIVACY, CRITICAL | `TC-COND-004, 005, 006` |
| `ADR-MAP-213` | Gating reused verbatim from UC151 — CRITICAL | `TC-COND-007, 008` |
| `ADR-MAP-214` | Best-effort audit event | `TC-COND-009` |
| UC129 fallback propagation | `degraded=true` must propagate through UC152 unchanged | `TC-COND-010` |
| SRS E3 | External/DB failure handling | `TC-COND-011` |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — accepted Expert with valid consent gets route via `IMapProviderService` | `SupportNavigationService.calculateRouteToSupportLocation()` | `UC152-TC-001` |
| TC-COND-002 | Service NEVER computes Haversine/timeout/cache itself — delegates 100% to `IMapProviderService` | `SupportNavigationService` (structural guard) | `UC152-TC-002` |
| TC-COND-003 | Mobile: TrackAsia deep-link launch, fallback to default map app | `SupportNavigationMobileService.navigate()` | `UC152-TC-MOB-001` |
| TC-COND-004 | Consent invalid (`REVOKED`) → 403 `MAP-212`, no route computed | `SupportNavigationService` | `UC152-TC-004` |
| TC-COND-005 | Consent invalid (`EXPIRED`) → 403 `MAP-212` | `SupportNavigationService` | `UC152-TC-005` |
| TC-COND-006 | Consent `NULL` → 403 `MAP-212` (treated as invalid, per L1) | `SupportNavigationService` | `UC152-TC-006` |
| TC-COND-007 | Not-accepting Expert (`selected_expert_id != caller`) → 403 `MAP-208` | `SupportRequestGatingPolicy` (reused from UC151) | `UC152-TC-007` |
| TC-COND-008 | Gating check runs BEFORE consent check and BEFORE map-service call (fail-fast order) | `SupportNavigationService` | `UC152-TC-008`, `UC152-TC-009` |
| TC-COND-009 | `NavigationRouteCalculated` event emitted best-effort, failure doesn't block response | `SupportNavigationService` | `UC152-TC-010` |
| TC-COND-010 | TrackAsia degraded (Haversine fallback) → `degraded=true` propagates through UC152 unchanged, `etaMinutes=null` | `SupportNavigationService` | `UC152-TC-011` |
| TC-COND-011 | DB read failure (emergency_events/location_snapshots unavailable) → 503 `MAP-213` | `SupportNavigationService` | `UC152-TC-012` |
| — | Response DTO never contains `motherFullName`/`motherPhone` or any field beyond `NavigationResponse` schema | `NavigationResponse` mapping | `UC152-TC-013` |
| — | `expertUserId` resolved from JWT `SecurityContext`, never from request body/query param | `SupportNavigationController` | `UC152-TC-014` |
| — | Origin coordinates (`originLatitude`/`originLongitude`) never persisted to any table | `SupportNavigationService` (structural DB-write-absence check) | `UC152-TC-015` |
| — | Invalid coordinate range (`originLatitude=999`) → 400 `MAP-211` | DTO validation | `UC152-TC-016` |
| — | Unauthenticated / non-EXPERT / unverified-EXPERT → 403/401 (full auth matrix) | `SupportNavigationController` | `UC152-TC-017` |
| — | Full flow integration: gating + consent + route computed, verified against real DB | `SupportNavigationController` + real DB | `UC152-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `consent_status` valid/invalid classes, role classes | Covers each authorization/consent class with one representative case |
| Decision Table | Gating × consent × TrackAsia-availability combinations | 200/degraded vs 403 (gating) vs 403 (consent) branching |
| Order Verification | Gating→consent→map-service call sequence | Fail-fast cost-avoidance invariant (ADR-MAP-210/212/213) is itself testable via Mockito `InOrder` |
| Error Guessing | Consent-revoked-after-accept race, coordinate boundary values | Highest-risk PII exposure surface in this batch |
| Structural/Negative Assertion | No PII beyond coordinates in response; no persistence of origin coords | Privacy-by-design verification (C4, L2) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-N01` | DB seed | `emergency_events{emergency_event_id: E1, selected_expert_id: EXPERT-001, status: 'ACCEPTED'}` | Happy path — accepted expert |
| `FX-N02` | DB seed | `location_snapshots{context_type: 'EMERGENCY_EVENT', context_id: E1, latitude: 10.776889, longitude: 106.700912, consent_status: 'GRANTED'}` | Valid consent destination |
| `FX-N03` | DB seed | `location_snapshots{..., consent_status: 'REVOKED'}` | Invalid consent (TC-004) |
| `FX-N04` | DB seed | `location_snapshots{..., consent_status: 'EXPIRED'}` | Invalid consent (TC-005) |
| `FX-N05` | DB seed | `location_snapshots{..., consent_status: null}` | Invalid consent (TC-006) |
| `FX-N06` | DB seed | `emergency_events{..., selected_expert_id: OTHER-EXPERT}` | Not-accepting expert (TC-007) |
| `FX-N07` | JWT | `{sub: EXPERT_USER_ID_1, role: 'EXPERT'}` (mapped to `EXPERT-001` profile, `verification_status='VERIFIED'`) | Auth context — accepting expert |
| `FX-N08` | JWT | `{sub: OTHER_EXPERT_USER_ID, role: 'EXPERT'}` | Auth context — non-accepting expert |
| `FX-N09` | Mock | `IMapProviderService.calculateRoute(...)` returns `RouteEstimate(distanceKm=1.8, etaMinutes=7, degraded=false)` | Happy path route |
| `FX-N10` | Mock | `IMapProviderService.calculateRoute(...)` returns `RouteEstimate(distanceKm=1.6, etaMinutes=null, degraded=true)` | Degraded/fallback path |
| `FX-N11` | Mock | `IEmergencyEventRepository.findById(...)` throws `DataAccessException` | DB failure (TC-012) |

### Applicability Matrix

| Platform | Unit | Integration | Component | Widget | E2E | Security |
|----------|------|--------------|-----------|--------|-----|----------|
| Backend (Spring Boot) | ✅ | ✅ (Testcontainers) | — | — | ✅ (MockMvc) | ✅ (gating, consent, identity-spoofing) |
| Mobile (Flutter) | ✅ (service) | — | — | ✅ (widget test — navigate button, fallback UI) | — | — |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Reuses UC150/UC151's SupportRequestTestFactory naming convention
// where entities overlap (EmergencyEvent, LocationSnapshot are shared).
// ═══════════════════════════════════════════════════════════

class SupportNavigationTestFactory {

    static final UUID EMERGENCY_EVENT_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000E01");
    static final UUID EXPERT_PROFILE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");
    static final UUID OTHER_EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");

    static EmergencyEvent makeAcceptedEvent(Consumer<EmergencyEvent> overrides) {
        EmergencyEvent event = new EmergencyEvent();
        event.setEmergencyEventId(EMERGENCY_EVENT_ID_1);
        event.setSelectedExpertId(EXPERT_PROFILE_ID_1);
        event.setStatus("ACCEPTED");
        overrides.accept(event);
        return event;
    }

    static LocationSnapshot makeValidConsentSnapshot(Consumer<LocationSnapshot> overrides) {
        LocationSnapshot snapshot = new LocationSnapshot();
        snapshot.setContextType("EMERGENCY_EVENT");
        snapshot.setContextId(EMERGENCY_EVENT_ID_1);
        snapshot.setLatitude(new BigDecimal("10.776889"));
        snapshot.setLongitude(new BigDecimal("106.700912"));
        snapshot.setConsentStatus("GRANTED");
        overrides.accept(snapshot);
        return snapshot;
    }

    static NavigationQueryRequest makeQueryRequest(Consumer<NavigationQueryRequest> overrides) {
        NavigationQueryRequest request = new NavigationQueryRequest();
        request.setOriginLatitude(10.7700);
        request.setOriginLongitude(106.6950);
        overrides.accept(request);
        return request;
    }

    static RouteEstimate makeHappyRouteEstimate() {
        return new RouteEstimate(1.8, 7, false);
    }

    static RouteEstimate makeDegradedRouteEstimate() {
        return new RouteEstimate(1.6, null, true);
    }
}
```

---

### UC152-TC-001 — Happy path: accepted Expert with valid consent gets route

**Severity:** `CRITICAL`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC152 TDS §6.1` happy-path sequence diagram, `ADR-MAP-210`

**Preconditions:** `FX-N01` (accepted event), `FX-N02` (valid consent), `FX-N07` (accepting expert JWT)

**Test Steps:**
1. Arrange: mock `emergencyEventRepository.findById(E1)` returns `FX-N01`; mock `locationSnapshotRepository.findTopByContextTypeAndContextIdOrderByCapturedAtDesc(...)` returns `FX-N02`; mock `mapProviderService.calculateRoute(10.77, 106.695, 10.776889, 106.700912)` returns `FX-N09`.
2. Act: `service.calculateRouteToSupportLocation(E1, EXPERT_USER_ID_1, 10.77, 106.695)`.
3. Assert: response `destLatitude==10.776889`, `distanceKm==1.8`, `etaMinutes==7`, `degraded==false`.

**Expected Result (PASS):** Route computed via `IMapProviderService`, correct coordinates returned.
**Expected Result (FAIL):** Wrong coordinates, or exception thrown.

**Current Status:** 🔴 Not written

---

### UC152-TC-002 — Structural guard: service never computes Haversine/timeout/cache itself

**Severity:** `CRITICAL`
**Feature Under Test:** `SupportNavigationService`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-MAP-210` — "KHÔNG có logic Haversine/timeout/retry/cache logic riêng"

**Test Steps:**
1. Act: `service.calculateRouteToSupportLocation(E1, EXPERT_USER_ID_1, 10.77, 106.695)` (happy path).
2. Assert (via Mockito `verify`): `mapProviderService.calculateRoute(...)` is called EXACTLY ONCE; assert (via reflection/class inspection) `SupportNavigationService` has no method named `calculateHaversine`/`computeDistance`/similar — i.e. no parallel distance-calculation logic exists in this class.

**Expected Result (PASS):** All distance/route computation delegated to `IMapProviderService`.
**Expected Result (FAIL):** Service contains its own Haversine implementation or direct TrackAsia HTTP client — violates AP-AI-002.

**Current Status:** 🔴 Not written

---

### UC152-TC-004 — Consent revoked → 403 (`MAP-212`)

**Severity:** `CRITICAL`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-MAP-212`, `UC152 TDS §6.2` error-path sequence diagram

**Preconditions:** `FX-N01`, `FX-N03` (`consent_status='REVOKED'`)

**Test Steps:**
1. Arrange: mock repositories return `FX-N01`/`FX-N03`.
2. Act: `service.calculateRouteToSupportLocation(E1, EXPERT_USER_ID_1, 10.77, 106.695)`.
3. Assert: throws `LocationConsentInvalidException` code `MAP-212`; `mapProviderService.calculateRoute(...)` NEVER invoked (`verifyNoInteractions`).

**Expected Result (PASS):** Rejected before any external map-service call — fail-fast per ADR-MAP-212.
**Expected Result (FAIL):** Route computed despite revoked consent — BR-PRIVACY violation.

**Current Status:** 🔴 Not written

---

### UC152-TC-005 — Consent expired → 403 (`MAP-212`)

**Severity:** `CRITICAL`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MAP-212`

**Preconditions:** `FX-N04` (`consent_status='EXPIRED'`)

**Test Steps:**
1. Arrange: mock returns `FX-N04`.
2. Act: `service.calculateRouteToSupportLocation(...)`.
3. Assert: throws `LocationConsentInvalidException` code `MAP-212`.

**Current Status:** 🔴 Not written

---

### UC152-TC-006 — Consent NULL → 403 (`MAP-212`), per L1 documented assumption

**Severity:** `HIGH`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-MAP-212` (Phương án A), Logic Issue L1 — `consent_status` treated as invalid when `NULL`

**Preconditions:** `FX-N05` (`consent_status=null`)

**Test Steps:**
1. Arrange: mock returns `FX-N05`.
2. Act: `service.calculateRouteToSupportLocation(...)`.
3. Assert: throws `LocationConsentInvalidException` code `MAP-212`.

**Expected Result (PASS):** `NULL` treated as invalid, matching the TDS's documented (Open, proposed) interpretation.
**Expected Result (FAIL):** `NULL` silently treated as valid — contradicts ADR-MAP-212's explicit rejection list.

**Current Status:** 🔴 Not written

---

### UC152-TC-007 — Not-accepting Expert → 403 (`MAP-208`), gating reused from UC151

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `SupportRequestGatingPolicy.assertAcceptedByCurrentExpert()` (reused from UC151)
**Test File:** `src/test/java/com/carebridge/backend/map/policy/SupportRequestGatingPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-MAP-213`, reused verbatim from `UC151 ADR-MAP-208`

**Preconditions:** `FX-N06` (event accepted by `OTHER_EXPERT_PROFILE_ID`), caller is `EXPERT_PROFILE_ID_1`

**Test Steps:**
1. Arrange: `event = SupportNavigationTestFactory.makeAcceptedEvent(e -> e.setSelectedExpertId(OTHER_EXPERT_PROFILE_ID))`.
2. Act: `gatingPolicy.assertAcceptedByCurrentExpert(event, EXPERT_PROFILE_ID_1)`.
3. Assert: throws `AccessDeniedException` code `MAP-208`.

**Expected Result (PASS):** Rejected — identical behavior to UC151's own gating test for the same fixture shape (cross-document consistency).
**Expected Result (FAIL):** Non-accepting expert gains access to navigation.

**Current Status:** 🔴 Not written
**Implementation Note:** This test MUST use the SAME `SupportRequestGatingPolicy` class/method as UC151's Test-Spec — if a duplicate gating implementation is found, that itself is a defect (violates ADR-MAP-213).

---

### UC152-TC-008 — Gating check runs BEFORE consent check (fail-fast order)

**Severity:** `HIGH`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC152 TDS §11.3 Chặng 2` — "Thứ tự BẮT BUỘC: gating TRƯỚC, consent SAU"

**Preconditions:** `FX-N06` (non-accepting expert) combined with `FX-N03` (also invalid consent, to test which check fires first)

**Test Steps:**
1. Arrange: event NOT accepted by caller AND consent also revoked (both violations present simultaneously).
2. Act: `service.calculateRouteToSupportLocation(...)`.
3. Assert: throws `AccessDeniedException` code `MAP-208` (gating failure), NOT `MAP-212` (consent failure) — proves gating is checked first.

**Expected Result (PASS):** `MAP-208` raised, `assertConsentValid()` never even reached (verify no `locationSnapshotRepository` interaction).
**Expected Result (FAIL):** `MAP-212` raised instead, or both checks run redundantly.

**Current Status:** 🔴 Not written

---

### UC152-TC-009 — Consent check runs BEFORE `IMapProviderService.calculateRoute()` call (cost-avoidance fail-fast)

**Severity:** `HIGH`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-MAP-212` — "tránh gọi external API không cần thiết khi consent invalid"

**Preconditions:** `FX-N01` (accepted), `FX-N03` (revoked consent)

**Test Steps:**
1. Arrange: gating passes, consent fails.
2. Act: `service.calculateRouteToSupportLocation(...)`.
3. Assert: throws `MAP-212`; `mapProviderService.calculateRoute(...)` is NEVER invoked (`verifyNoInteractions(mapProviderService)`) — via Mockito `InOrder`, confirm the full call sequence is gating→consent, with map-service unreached.

**Expected Result (PASS):** Zero calls to the external map provider when consent is invalid — cost/latency avoidance confirmed.
**Expected Result (FAIL):** `IMapProviderService` is called despite invalid consent (wasted external API cost, and a potential information-disclosure risk if the route calculation itself leaks timing info).

**Current Status:** 🔴 Not written

---

### UC152-TC-010 — `NavigationRouteCalculated` event emitted best-effort, failure doesn't block response

**Severity:** `MEDIUM`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-MAP-214` — mirrors UC150's best-effort event pattern

**Test Steps:**
1. Act (happy path): `service.calculateRouteToSupportLocation(...)`.
2. Assert: `eventPublisher.publishEvent(captor.capture())` captured `NavigationRouteCalculated` with `payload.emergencyEventId==E1`, `payload.expertProfileId==EXPERT_PROFILE_ID_1`, `payload.distanceKm==1.8`, `payload.degraded==false`.
3. Act (event publish throws): mock `eventPublisher.publishEvent(...)` to throw `RuntimeException`.
4. Assert: `calculateRouteToSupportLocation(...)` STILL returns the `NavigationResponse` successfully — the event-publish exception is caught/logged, never propagated.

**Expected Result (PASS):** Event emitted on success; a publish failure never blocks the response (best-effort, mirrors UC150's ADR-MAP-203).
**Expected Result (FAIL):** Response fails when event publishing throws.

**Current Status:** 🔴 Not written

---

### UC152-TC-011 — TrackAsia degraded (Haversine fallback) propagates through UC152 unchanged

**Severity:** `HIGH`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `UC129 ADR-MAP-103` (fallback owner), `UC152 TDS §6.2` error-path sequence diagram

**Preconditions:** `FX-N10` (`mapProviderService.calculateRoute(...)` returns degraded estimate)

**Test Steps:**
1. Arrange: mock `mapProviderService.calculateRoute(...)` returns `RouteEstimate(1.6, null, true)`.
2. Act: `service.calculateRouteToSupportLocation(...)`.
3. Assert: response `distanceKm==1.6`, `etaMinutes==null`, `degraded==true` — NOT an error, still `200`-equivalent success.

**Expected Result (PASS):** Degraded flag and null ETA propagate through unchanged; no exception thrown for a degraded-but-successful fallback.
**Expected Result (FAIL):** UC152 throws an error instead of returning the degraded response, or silently fabricates a non-null `etaMinutes`.

**Current Status:** 🔴 Not written

---

### UC152-TC-012 — DB read failure (emergency_events/location_snapshots unavailable) → 503 (`MAP-213`)

**Severity:** `MEDIUM`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `UC152 TDS §10` error table — "lỗi TrackAsia riêng KHÔNG trả mã này"

**Preconditions:** `FX-N11` (`emergencyEventRepository.findById(...)` throws `DataAccessException`)

**Test Steps:**
1. Arrange: mock repository throws.
2. Act: `service.calculateRouteToSupportLocation(...)`.
3. Assert: throws `ServiceUnavailableException` code `MAP-213`.

**Expected Result (PASS):** `503` surfaced for DB-layer failure specifically — distinct from a TrackAsia-layer failure, which never reaches this code path since `IMapProviderService` (UC129) always returns a (possibly degraded) `RouteEstimate` rather than throwing.
**Expected Result (FAIL):** Wrong error code, or exception swallowed silently.

**Current Status:** 🔴 Not written

---

### UC152-TC-013 — Response DTO never leaks PII beyond destination coordinates

**Severity:** `CRITICAL`
**CWE:** `CWE-213 — Exposure of Sensitive Information Due to Incompatible Policies`
**Feature Under Test:** `NavigationResponse` mapping
**Test File:** `src/test/java/com/carebridge/backend/map/dto/NavigationResponseTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `C4`, `ADR-MAP-211`

**Test Steps:**
1. Act: build a `NavigationResponse` from happy-path service output; serialize to JSON.
2. Assert: JSON contains ONLY `emergencyEventId`, `destLatitude`, `destLongitude`, `distanceKm`, `etaMinutes`, `degraded` — NO `motherFullName`, `motherPhone`, or any other field. Those belong exclusively to UC151's `contact-detail` response.

**Expected Result (PASS):** Response schema matches exactly `UC152 TDS §9.2`.
**Expected Result (FAIL):** Any extra PII field present — violates C4 and AP-AI-001.

**Current Status:** 🔴 Not written

---

### UC152-TC-014 — `expertUserId` resolved from JWT `SecurityContext`, never client-supplied

**Severity:** `CRITICAL`
**CWE:** `CWE-290 — Authentication Bypass by Spoofing`
**Feature Under Test:** `SupportNavigationController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/SupportNavigationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `C6`, `ADR-MAP-213`

**Test Steps:**
1. Act: `GET /api/v1/map/support-requests/{id}/navigation?originLatitude=10.77&originLongitude=106.70` with JWT `FX-N07` AND a spoofed request attempting to inject a different expert identity via any client-controlled field.
2. Assert: the `expertUserId` used for gating is resolved SOLELY from the JWT `sub` claim, never from any request body/query param.

**Expected Result (PASS):** Identity always server-resolved.
**Expected Result (FAIL):** A client-controlled value influences the identity used for gating — spoofing vulnerability.

**Current Status:** 🔴 Not written

---

### UC152-TC-015 — Origin coordinates never persisted to any table

**Severity:** `MEDIUM`
**Feature Under Test:** `SupportNavigationService.calculateRouteToSupportLocation()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/SupportNavigationServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** Logic Issue L2, `§4.2 Data Integrity`

**Test Steps:**
1. Act: `service.calculateRouteToSupportLocation(E1, EXPERT_USER_ID_1, 10.77, 106.695)` (happy path).
2. Assert: NO repository `save()`/`insert()` method is ever invoked by this service for any entity (`verifyNoInteractions` on any writable repository mock) — the service is read-only end-to-end except for the best-effort event publish.

**Expected Result (PASS):** Zero persistence of Expert's origin coordinates, confirming the stateless design (mirrors UC129).
**Expected Result (FAIL):** Origin coordinates written to any table — violates the "no new persistence" NFR (§4.2).

**Current Status:** 🔴 Not written

---

### UC152-TC-016 — Invalid coordinate range → 400 (`MAP-211`)

**Severity:** `MEDIUM`
**Feature Under Test:** `NavigationQueryRequest` DTO validation
**Test File:** `src/test/java/com/carebridge/backend/map/dto/NavigationQueryRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `UC152 TDS §8.1` `@DecimalMin`/`@DecimalMax` annotations

**Test Steps:**
1. Act: validate `SupportNavigationTestFactory.makeQueryRequest(r -> r.setOriginLatitude(999.0))`.
2. Assert: violation on `originLatitude` (`@DecimalMax("90.0")`).
3. Act: validate with `originLongitude=null`.
4. Assert: violation on `originLongitude` (`@NotNull`).

**Expected Result (PASS):** Out-of-range/missing coordinates rejected with `400 MAP-211`.
**Expected Result (FAIL):** Invalid coordinates silently accepted and passed to `IMapProviderService`.

**Current Status:** 🔴 Not written

---

### UC152-TC-017 — Full authorization matrix: unauthenticated / non-EXPERT / unverified-EXPERT rejected

**Severity:** `CRITICAL`
**Feature Under Test:** `SupportNavigationController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/SupportNavigationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `UC152 TDS §16` Authorization Matrix

**Test Steps:**
1. Act (no JWT): `GET .../navigation` with no `Authorization` header. Assert `401`.
2. Act (MOTHER role): `GET .../navigation` with a MOTHER JWT. Assert `403`.
3. Act (unverified EXPERT): `GET .../navigation` with an EXPERT JWT where `verification_status != 'VERIFIED'`. Assert `403` `MAP-208`.
4. Act (verified EXPERT, not accepted): covered by `UC152-TC-007`.

**Expected Result (PASS):** Full matrix enforced exactly per `§16`.
**Expected Result (FAIL):** Any unauthorized role/state gains access.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### UC152-TC-INT-001 — Full flow: gating + consent + route computed, verified against real DB (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `GET /api/v1/map/support-requests/{id}/navigation` → real DB gating/consent checks + mocked `IMapProviderService`
**Test File:** `src/test/java/com/carebridge/backend/map/SupportNavigationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** TDS-03 E2E row

**Preconditions:**
- PostgreSQL container running (`@Testcontainers`)
- Flyway migration applied automatically (no new migration for this UC)
- Seed: `FX-N01` + `FX-N02` inserted via JPA; `IMapProviderService` mocked (external HTTP boundary, not the DB)

**Test Steps:**
1. Seed accepted event + valid-consent snapshot.
2. `GET /api/v1/map/support-requests/{E1}/navigation?originLatitude=10.77&originLongitude=106.695` with `FX-N07` JWT.
3. Assert response `200`, body matches §9.2 schema exactly.
4. Repeat with `FX-N03` (revoked consent) seeded instead — assert `403 MAP-212`.
5. Repeat with event accepted by a DIFFERENT expert — assert `403 MAP-208`.

**Expected Result (PASS):** All three DB-backed scenarios (happy, consent-revoked, not-accepted) behave correctly against a real database.
**Expected Result (FAIL):** Any scenario diverges from the mocked-unit-test behavior — indicates a gap between unit and integration coverage.

**Current Status:** 🔴 Not written

---

### MOBILE TEST CASES (flutter_test)

---

### UC152-TC-MOB-001 — Mobile: TrackAsia deep-link launch with fallback

**Severity:** `MEDIUM`
**Feature Under Test:** `SupportNavigationMobileService.navigate()`
**Test File:** `test/features/nearbySupport/services/support_navigation_service_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`, `ADR-MAP-211` (mirrors UC64 ADR-MAP-006)

**Test Steps:**
1. Arrange: mock `canLaunchUrl` returns `true` for the TrackAsia deep-link scheme.
2. Act: `navigate(destLatitude: 10.776889, destLongitude: 106.700912)`.
3. Assert: TrackAsia deep-link launched with correct coordinates.
4. Arrange (fallback): mock `canLaunchUrl` returns `false` for TrackAsia.
5. Act: `navigate(...)` again.
6. Assert: fallback `geo:`/Google Maps URL launched instead.

**Expected Result (PASS):** Correct deep-link priority and fallback behavior, mirroring UC64's established pattern.
**Expected Result (FAIL):** No fallback attempted, or wrong coordinates passed.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UC152-TC-001` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-002` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-004` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-005` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-006` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-007` | `SupportRequestGatingPolicyTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-008` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-009` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-010` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-011` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-012` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-013` | `NavigationResponseTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-014` | `SupportNavigationControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-015` | `SupportNavigationServiceTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-016` | `NavigationQueryRequestValidationTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-017` | `SupportNavigationControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-INT-001` | `SupportNavigationIntegrationTest.java` | `[ ]` | `[ ]` | |
| `UC152-TC-MOB-001` | `support_navigation_service_test.dart` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class SupportNavigationService implements ISupportNavigationService {
    @Override
    public NavigationResponse calculateRouteToSupportLocation(
            UUID emergencyEventId, UUID expertUserId, double originLatitude, double originLongitude) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `UC152-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC152-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC152-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC152-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC152-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC152-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC152-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] `UC152_NavigateToSupportLocation_TDS.md` reviewed and Approved
- [ ] **BLOCKING:** `UC129 IMapProviderService` implemented — hard dependency
- [ ] **BLOCKING:** `UC151 SupportRequestGatingPolicy` implemented or implemented in parallel — gating logic MUST be extracted/shared, not duplicated
- [ ] ADR-MAP-210, 211, 212, 214 confirmed by Product/Tech Lead (currently `Proposed`) — especially ADR-MAP-212's `consent_status` enum values need confirmation with UC63/UC141 owner; ADR-MAP-213 already **Accepted** (Confirmed by Product Owner 2026-07-02 — gating mechanism)
- [ ] Test fixtures (§3 TDS-05) prepared

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration test xanh (Testcontainers)
- [ ] `flutter test` — Mobile tests xanh
- [ ] `UC152-TC-004/005/006` (consent gate), `UC152-TC-007/008/009` (gating + fail-fast order), `UC152-TC-013` (no PII leakage) pass — release-blocking privacy/security gates
- [ ] `UC152-TC-007` verified to use the SAME `SupportRequestGatingPolicy` class as UC151 (no duplicate gating logic)
- [ ] Không có business logic trong Controller

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` no errors
- [ ] **Props Isolation** — dùng `SupportNavigationTestFactory`, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn

### Suspension Criteria

- UC129/UC151 not yet implemented or not deployed to test environment
- `consent_status` enum values (L1) not yet confirmed by Product/UC63/UC141 owner
- ~~Gating mechanism (`selected_expert_id`, shared Open item with UC150/UC151) unresolved~~ — RESOLVED 2026-07-02 (Confirmed by Product Owner, ADR-MAP-213 Accepted); no longer a suspension condition

---

## 7. Rollback Plan

```bash
# Code-only rollback (no migration in baseline scope)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/SupportNavigationController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/service/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/map/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/nearbySupport/services/support_navigation_service*.dart
kubectl rollout undo deployment/carebridge-api
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | Code thêm field `motherFullName`/`motherPhone` vào response | ☑ `UC152-TC-013` explicitly guards | G-0 |
| AP-AI-002 | Reimplementation | Code viết lại Haversine hoặc gọi TrackAsia trực tiếp thay vì `IMapProviderService` | ☑ `UC152-TC-002` explicitly guards | G-1 |
| AP-AI-003 | Implicit Decision | Code bỏ qua consent check hoặc đảo thứ tự gating/consent | ☑ `UC152-TC-008/009` explicitly assert call order | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — controller tests chỉ check security/mapping | G-4 |
| AP-AI-005 | Hallucinated Contract | Code import service/entity không tồn tại (vd: tự bịa `ExpertLocationTrackingService`) | ☑ All types match TDS §8 interfaces exactly | G-3 |
| **AP-CB-501** *(project-specific)* | **Duplicate gating logic instead of reusing UC151's policy** | `UC152-TC-007` fails because a SEPARATE, divergent gating check exists in UC152's own code | `UC152-TC-007` explicitly requires the SAME `SupportRequestGatingPolicy` class | **Release-blocking** |
| **AP-CB-502** *(project-specific)* | **Consent check bypassed or reordered after map-service call** | `UC152-TC-009` fails — `IMapProviderService` invoked despite invalid consent | `UC152-TC-009` explicitly asserts zero map-service interaction on consent failure | **Release-blocking** |

**Kết quả review:**

- [x] Anti-pattern coverage identified and encoded as explicit test cases (`UC152-TC-002`, `007`, `008`, `009`, `013`)
- [ ] Actual Red Gate execution pending (this Test-Spec is Draft, not yet executed)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | No anti-patterns detected in spec drafting (pre-implementation) | N/A | N/A |

---

*Test-Spec UC152 v1.0 — Draft. Total test cases: 18 (13 unit/component/security + 1 integration + 1 mobile widget + 3 boundary/structural). Critical-severity: 7 (`UC152-TC-001, 002, 004, 005, 007, 013, 014, 017` — consent, gating, and PII-leakage gates). Gating mechanism (shared with UC150/UC151) RESOLVED (Confirmed by Product Owner 2026-07-02). `consent_status` enum values (L1) remain Open. Requires Approved status change only by user/Tech Lead.*
