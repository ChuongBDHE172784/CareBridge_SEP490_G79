# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification â€” UC-192 View Baby Profile

| Field | Value |
|-------|-------|
| **Document ID** | `CB-BABY-IMP-002` |
| **Version** | `1.0` |
| **Date** | `2026-06-26` |
| **Status** | `Partially Implemented` |
| **Document Owner** | `PhuongNT` |
| **Author** | `AI Agent` |
| **Reviewed by** | `[Tech Lead]` |
| **DPO Sign-off** | `[ ] Pending` |
| **Approved by** | `[Principal Architect]` |
| **Last Review** | `2026-06-26` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented: targeted baby/carejourney backend tests PASS; full regression remains blocked by non-baby Family/Exercise/Auth/Triage failures. |
| 2026-06-27 | AI Agent â€” Amelia (Dev Agent) | Implementation completed â€” service, controller, tests ðŸŸ¢ GREEN (45/45) |
| 2026-06-26 | AI Agent | Táº¡o tÃ i liá»‡u láº§n Ä‘áº§u cho UC-192 View Baby Profile |

---

## Má»¤C Lá»¤C

1. [Tá»•ng quan Module](#1-tá»•ng-quan-module)
2. [Ma tráº­n Truy váº¿t](#2-ma-tráº­n-truy-váº¿t)
3. [Architecture Decision Records](#3-architecture-decision-records)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling](#5-static-modeling)
6. [Dynamic Modeling](#6-dynamic-modeling)
7. [Interface Specification](#7-interface-specification)
8. [API Specification](#8-api-specification)
9. [API Specification (Detail)](#9-api-specification-detail)
10. [Báº£ng mÃ£ lá»—i](#9-10-báº£ng-mÃ£-lá»—i)
11. [Ká»‹ch báº£n Kiá»ƒm thá»­](#11-ká»‹ch-báº£n-kiá»ƒm-thá»­)
12. [Authorization Matrix](#12-authorization-matrix)
13. [AI Prompt Constraints](#13-ai-prompt-constraints-case-20)
14. [PhÆ°Æ¡ng phÃ¡p XÃ¡c minh](#14-phÆ°Æ¡ng-phÃ¡p-xÃ¡c-minh)
15. [Máº«u thá»­ thá»±c táº¿](#15-máº«u-thá»­-thá»±c-táº¿)
16. [Authorization Matrix (Detail)](#16-authorization-matrix-detail)
17. [AI Prompt Constraints (Full)](#17-ai-prompt-constraints-case-20-full)

---

## 1. Tá»•ng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | `ViewBabyProfile` |
| **Bounded Context** | `baby` |
| **UC ID** | `UC-192` |
| **SRS Reference** | `3.3.12.1` |
| **Primary Actor** | `Mother (ROLE_MOTHER)` |
| **Platform** | `Mobile App` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `auth, baby_profiles table` |
| **Downstream Consumers** | `baby daily log, vaccination, growth tracking` |

**MÃ´ táº£:** Hiá»ƒn thá»‹ thÃ´ng tin cÆ¡ báº£n vÃ  tráº¡ng thÃ¡i theo dÃµi cá»§a má»™t baby profile: nickname, ngÃ y sinh, giá»›i tÃ­nh, cÃ¢n náº·ng vÃ  chiá»u dÃ i khi sinh, tráº¡ng thÃ¡i (ACTIVE/ARCHIVED). Chá»‰ owner hoáº·c care group member cÃ³ quyá»n xem.

---

## 2. Ma tráº­n Truy váº¿t

| Requirement ID | Loáº¡i | MÃ´ táº£ | ThÃ nh pháº§n Code | Compliance Target | ADR liÃªn quan |
|----------------|------|-------|-----------------|-------------------|---------------|
| UC-192 | Use Case | Mother xem baby profile | `BabyController.getBabyProfile()` | BR-RBAC | ADR-BABY-003 |
| BR-BABY-010 | Business Rule | Chá»‰ account owner vÃ  care group members xem Ä‘Æ°á»£c | `BabyAccessPolicy.canView()` | BR-PRIVACY | ADR-BABY-003 |
| BR-BABY-011 | Business Rule | Archived profiles váº«n viewable nhÆ°ng cÃ³ watermark | `status` trong response | Data Integrity | â€” |

---

## 3. Architecture Decision Records

### ADR-BABY-003 â€” Access Policy cho Baby Profile View

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Date** | `2026-06-26` |

#### Quyáº¿t Ä‘á»‹nh
Baby profile cÃ³ thá»ƒ Ä‘Æ°á»£c xem bá»Ÿi: (1) account owner, (2) care group members vá»›i invite_status=ACCEPTED. Expert khÃ´ng Ä‘Æ°á»£c xem trá»« khi Mother chia sáº» qua consultation.

---

## 4. Non-Functional Requirements & SLA

| Category | Requirement | Target |
|----------|-------------|--------|
| Latency (p99) | GET response | `< 150ms` |
| Availability | Uptime | `99.9%` |

---

## 5. Static Modeling

### 5.1. Class Diagram

```plantuml
@startuml ViewBabyProfile_ClassDiagram

class BabyProfile {
  + id: UUID
  + accountId: UUID
  + nickname: String
  + birthDate: LocalDate
  + gender: Gender
  + birthWeightKg: BigDecimal
  + birthLengthCm: BigDecimal
  + avatarFileId: UUID
  + isActive: Boolean
  + status: BabyProfileStatus
  + createdAt: Instant
}

interface IBabyService {
  + getBabyProfile(profileId: UUID, accountId: UUID): BabyProfileDetailResponse
}

class BabyService implements IBabyService {
  - babyRepository: IBabyRepository
  - babyAccessPolicy: BabyAccessPolicy
  + getBabyProfile(profileId, accountId): BabyProfileDetailResponse
}

class BabyAccessPolicy {
  - careGroupMemberRepository: ICareGroupMemberRepository
  + canView(profileId, accountId): boolean
}

BabyService --> BabyAccessPolicy

@enduml
```

---

## 6. Dynamic Modeling

### 6.1. Sequence Diagram â€” Happy Path

```plantuml
@startuml ViewBabyProfile_HappyPath
actor "Mother" as Client
participant "BabyController" as Controller
participant "BabyService" as Service
participant "BabyAccessPolicy" as Policy
participant "BabyRepository" as Repo
database "PostgreSQL" as DB

Client -> Controller : GET /api/v1/baby-profiles/{profileId}
Controller -> Service : getBabyProfile(profileId, accountId)
Service -> Repo : findById(profileId)
Repo -> DB : SELECT FROM baby_profiles WHERE id=?
DB --> Repo : profile
Service -> Policy : canView(profile, accountId)
Policy --> Service : true
Service --> Controller : BabyProfileDetailResponse
Controller --> Client : 200 OK
@enduml
```

---

## 7. Interface Specification

```java
// BabyProfileDetailResponse.java
public class BabyProfileDetailResponse {
    private UUID id;
    private String nickname;
    private LocalDate birthDate;
    private String gender;
    private BigDecimal birthWeightKg;
    private BigDecimal birthLengthCm;
    private String status;       // ACTIVE or ARCHIVED
    private boolean isActive;
    private Instant createdAt;
}

// IBabyService.java (addition to existing)
/**
 * @throws NotFoundException (BABY-001) when profile not found
 * @throws ForbiddenException (BABY-003) when caller lacks access
 */
BabyProfileDetailResponse getBabyProfile(UUID profileId, UUID accountId);
```

---

## 8. API Specification

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `GET` | `/api/v1/baby-profiles/{profileId}` | JWT Bearer | `ROLE_MOTHER` | 100/min | Yes |

**Response 200:**
```json
{
  "id": "uuid-v4",
  "nickname": "Bean",
  "birthDate": "2026-01-15",
  "gender": "MALE",
  "birthWeightKg": 3.2,
  "birthLengthCm": 50.0,
  "status": "ACTIVE",
  "isActive": true,
  "createdAt": "2026-06-26T00:00:00.000Z"
}
```

---

## 9-10. Báº£ng mÃ£ lá»—i

> **(Corrected 2026-07-03):** Báº£ng nÃ y ban Ä‘áº§u ghi `BABY-002`(403)/`BABY-004`(404) â€” khÃ´ng khá»›p code tháº­t Ä‘Ã£ ship (`BabyServiceImpl.java` dÃ¹ng `BABY-001` cho 404 vÃ  `BABY-003` cho 403). ÄÃ£ sá»­a láº¡i khá»›p thá»±c táº¿; phÃ¡t hiá»‡n trong batch UC194 (Logic Issue L1/OI-3).

| Code | HTTP | Message (EN) | Trigger Condition |
|------|------|--------------|-------------------|
| `BABY-003` | 403 | Insufficient permissions | Caller lacks access rights |
| `BABY-001` | 404 | Baby profile not found | ID not found |
| `BABY-005` | 500 | Internal error | DB error |

---

## 11. Ká»‹ch báº£n Kiá»ƒm thá»­

```gherkin
Feature: View Baby Profile
  Scenario: Owner views own profile â†’ 200
  Scenario: Care group member views profile â†’ 200
  Scenario: Unrelated user views profile â†’ 403
  Scenario: Non-existent profile â†’ 404
  Scenario: Archived profile still viewable â†’ 200 with status ARCHIVED
```

---

## 12. Authorization Matrix

| Endpoint | `GUEST` | `MOTHER (owner)` | `MOTHER (care member)` | `EXPERT` | `ADMIN` |
|----------|---------|------------------|------------------------|----------|---------|
| `GET /api/v1/baby-profiles/:id` | âŒ | âœ… | âœ… | âŒ | âœ… All |

---

## 9. API Specification (Detail)

### 9.1. Endpoints Table

| Method | Path | Auth Level | Required Roles | Rate Limit | Idempotent? |
|--------|------|------------|----------------|------------|-------------|
| `GET` | `/api/v1/baby-profiles/{profileId}` | JWT Bearer | `ROLE_MOTHER` | 100/min | Yes |

### 9.2. Request / Response Schemas

#### `GET /api/v1/baby-profiles/{profileId}`

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response -- 200 OK (Owner or Care Group Member):**
```json
{
  "id": "uuid-v4",
  "nickname": "Bean",
  "birthDate": "2026-01-15",
  "gender": "MALE",
  "birthWeightKg": 3.2,
  "birthLengthCm": 50.0,
  "status": "ACTIVE",
  "isActive": true,
  "createdAt": "2026-06-26T00:00:00.000Z"
}
```

**Response -- 403 Forbidden (Not Owner/Member):**
```json
{
  "error": {
    "code": "BABY-003",
    "message": "Insufficient permissions to view this baby profile"
  }
}
```

**Response -- 404 Not Found:**
```json
{
  "error": {
    "code": "BABY-001",
    "message": "Baby profile not found"
  }
}
```

---

## 10. Báº£ng mÃ£ lá»—i (Error Codes)

| Code | HTTP Status | Message (EN) | Message (VI) | Trigger Condition |
|------|-------------|--------------|--------------|-------------------|
| `BABY-003` | 403 | Insufficient permissions | KhÃ´ng Ä‘á»§ quyá»n truy cáº­p | Caller is not owner and not care group member |
| `BABY-001` | 404 | Baby profile not found | Há»“ sÆ¡ em bÃ© khÃ´ng tá»“n táº¡i | profileId not found in DB |
| `BABY-005` | 500 | Internal error | Lá»—i há»‡ thá»‘ng | Unexpected DB error |

---

## 13. Ká»‹ch báº£n Kiá»ƒm thá»­ Chi tiáº¿t

> **Policy (EDS v2.0):** Má»i test scenario dÃ¹ng dá»¯ liá»‡u `SYNTHETIC`.

```gherkin
Feature: View Baby Profile
  Background:
    Given test data classification: SYNTHETIC
    And MOTHER-001 lÃ  owner cá»§a BABY-001

  Scenario: Owner xem profile â†’ 200
    When getBabyProfile(BABY-001, MOTHER-001)
    Then response 200 vá»›i nickname, birthDate, gender

  Scenario: Non-owner â†’ 403
    Given MOTHER-002 KHÃ”NG pháº£i owner
    When getBabyProfile(BABY-001, MOTHER-002)
    Then throws ForbiddenException BABY-003

  Scenario: Not found â†’ 404
    When getBabyProfile(NONEXISTENT, MOTHER-001)
    Then throws NotFoundException BABY-001

  Scenario: Response khÃ´ng chá»©a diagnosis
    When getBabyProfile(BABY-001, MOTHER-001)
    Then response KHÃ”NG chá»©a "diagnosis", "prescription"
```

---

## 14. PhÆ°Æ¡ng phÃ¡p XÃ¡c minh

### 14.1. Database Inspection

```sql
-- Verify baby profile exists and belongs to caller
SELECT id, account_id, nickname, birth_date, gender, status
FROM baby_profiles WHERE id = '[profileId]';

-- Verify care group membership for non-owner access
SELECT cgm.member_role, cgm.invite_status
FROM care_group_members cgm
JOIN care_groups cg ON cg.id = cgm.group_id
WHERE cg.owner_account_id = (SELECT account_id FROM baby_profiles WHERE id = '[profileId]')
  AND cgm.member_account_id = '[callerId]'
  AND cgm.invite_status = 'ACCEPTED';
```

### 14.2. Access Policy Verification

```bash
# Verify owner access
curl -X GET https://[host]/api/v1/baby-profiles/[profileId] \
  -H "Authorization: Bearer [OWNER_JWT]"
# Expected: 200

# Verify non-owner, non-member access denied
curl -X GET https://[host]/api/v1/baby-profiles/[profileId] \
  -H "Authorization: Bearer [UNRELATED_USER_JWT]"
# Expected: 403
```

---

## 15. Máº«u thá»­ thá»±c táº¿ (API Verification Samples)

### 15.1. Happy Path -- Owner

```bash
curl -X GET https://[host]/api/v1/baby-profiles/[profileId] \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"
# Expected: 200 {id, nickname, birthDate, gender, status}
```

### 15.2. Happy Path -- Care Group Member

```bash
curl -X GET https://[host]/api/v1/baby-profiles/[profileId] \
  -H "Authorization: Bearer [CARE_MEMBER_JWT]"
# Expected: 200 (same schema as owner)
```

### 15.3. Error Paths

```bash
# Unrelated user -> 403
curl -X GET https://[host]/api/v1/baby-profiles/[profileId] \
  -H "Authorization: Bearer [OTHER_USER_JWT]"

# Non-existent profile -> 404
curl -X GET https://[host]/api/v1/baby-profiles/non-existent-uuid \
  -H "Authorization: Bearer [JWT_MOTHER_TOKEN]"

# No JWT -> 401
curl -X GET https://[host]/api/v1/baby-profiles/[profileId]
```

---

## 16. Báº£ng tá»•ng há»£p phÃ¢n quyá»n (Authorization Matrix Detail)

| Endpoint | `GUEST` | `MOTHER (owner)` | `MOTHER (care member)` | `EXPERT` | `ADMIN` |
|----------|---------|------------------|------------------------|----------|---------|
| `GET /api/v1/baby-profiles/{id}` | âŒ (401) | âœ… | âœ… (ACCEPTED only) | âŒ (403) | âœ… All |

**Chu thich:**
- Owner: account_id trong baby_profiles match JWT subject
- Care member: care_group_members.invite_status = ACCEPTED cho group cua owner
- Expert: khong co quyen xem truc tiep, chi qua consultation sharing

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source | Last Verified |
|---|-----------|--------|---------------|
| C1 | BabyAccessPolicy.canView() PHáº¢I check ownership AND care group membership | ADR-BABY-003 | 2026-06-26 |
| C2 | Archived profiles (status=ARCHIVED) váº«n tráº£ vá» 200, khÃ´ng 404 | BR-BABY-011 | 2026-06-26 |
| C3 | accountId tá»« JWT â€” khÃ´ng tá»« URL | BR-RBAC | 2026-06-26 |
| C4 | Read-only endpoint â€” khÃ´ng side effects | â€” | 2026-06-26 |
| C5 | Response khÃ´ng chá»©a sensitive birth data ngoÃ i scope | BR-PRIVACY | 2026-06-26 |

### 17.2 Constraint Injection Block

```
[CONSTRAINT BLOCK â€” Module: ViewBabyProfile (CB-BABY-IMP-002)]
1. BabyAccessPolicy.canView() PHáº¢I check: (a) account owner, HOáº¶C (b) care group member vá»›i invite_status=ACCEPTED â€” ADR-BABY-003
2. Archived profiles (status=ARCHIVED) váº«n tráº£ vá» 200 vá»›i data â€” KHÃ”NG tráº£ 404 â€” BR-BABY-011
3. accountId tá»« JWT SecurityContext, KHÃ”NG tá»« URL path parameter â€” BR-RBAC
4. Read-only endpoint â€” KHÃ”NG cÃ³ side effects (no DB write, no audit event) â€” Design
5. Response KHÃ”NG chá»©a sensitive birth data ngoÃ i scope (e.g., medical records, diagnosis) â€” BR-PRIVACY

[CONTEXT BLOCK]
- Bounded Context: baby
- Data Classification: Sensitive-PII
- Error codes: Â§10 Error Codes Table
- Auth matrix: Â§16 Authorization Matrix
```

### 17.3 Constraint Quality Checklist

- [x] Má»—i constraint traceable vá» ADR hoáº·c BR cá»¥ thá»ƒ
- [x] KhÃ´ng cÃ³ constraint generic
- [x] Constraint block cÃ³ â‰¥ 3 constraints cá»¥ thá»ƒ

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | HÃ nh Ä‘á»™ng |
|-------|-------------|-----------|----------|
| AP-AI-001 | Unconstrained Gen | Code khÃ´ng match constraint C1-C5 | Reject â€” inject láº¡i constraints |
| AP-AI-003 | Implicit Decision | Code assume architecture khÃ´ng cÃ³ ADR | Reject â€” viáº¿t ADR trÆ°á»›c |
| AP-AI-005 | Hallucinated Contract | Code import khÃ´ng cÃ³ trong Â§7 | Reject â€” verify contract |

---

## PHá»¤ Lá»¤C

### A. Glossary (Thuáº­t ngá»¯)

| Thuáº­t ngá»¯ | Äá»‹nh nghÄ©a |
|-----------|------------|
| BabyAccessPolicy | Policy class kiá»ƒm tra quyá»n xem baby profile â€” check ownership vÃ  care group membership |
| CareGroupMember | ThÃ nh viÃªn nhÃ³m chÄƒm sÃ³c â€” cÃ³ invite_status (PENDING, ACCEPTED, REJECTED) |
| PII Masking | áº¨n thÃ´ng tin nháº­n dáº¡ng cÃ¡ nhÃ¢n trong API responses â€” Ã¡p dá»¥ng cho sensitive birth data |

### B. TÃ i liá»‡u tham chiáº¿u

| Document | Path |
|----------|------|
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |

---

*EDS v2.1 â€” TÃ­ch há»£p CASE 2.0 AI Prompt Constraints (Â§17).*
