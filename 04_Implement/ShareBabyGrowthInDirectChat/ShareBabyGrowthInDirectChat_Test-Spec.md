# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Share Baby Growth in Direct Chat

| Field | Value |
| --- | --- |
| Document ID | `SBG-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-09-14` |
| Status | `Approved` |
| Feature / Gap ID | `ShareBabyGrowthInDirectChat` |
| Function ID | `Open — no SRS function ID (TDS OPEN-05)` |
| Canonical Use Case | Extends `UC-EX-10` / `UC-EX-12` — Mother shares all baby growth trends with an expert in direct chat |
| Module | Direct chat sharing + baby growth |
| Bounded Context | Backend `carejourney`; Mobile `features/directChat`, `features/baby`; Web `features/directChat`, `features/expert` |
| Paired TDS | `SBG-TDS` (`04_Implement/ShareBabyGrowthInDirectChat/ShareBabyGrowthInDirectChat_TDS.md`) |
| Priority | `Open` |
| Sprint / Milestone | `Open` |
| Owner | `CareBridge Team` |
| Author | `AI Agent` |
| Reviewer | |
| Approver | huynd4104 (user, replied "Approved" 2026-09-14; DPO sign-off Open — TDS OPEN-02) |
| Platforms | Backend / Web / Mobile |
| Data Classification | Restricted (child health measurements — Sensitive-PII) |
| Compliance Scope | Health-data RBAC (`CLAUDE.md` Safety Rules); PDPA/consent `Open` (TDS OPEN-02) |
| Upstream Dependencies | `GET /api/v1/babies/{babyId}/growth-chart`, `GET /api/v1/babies`, direct message send API |
| Downstream Consumers | Mobile `DirectChatScreen`; Web `ChatPanel`, `ExpertSharedRecordsPage` |
| Source Baseline | Branch `HuyND` @ `85aa9ad51` + staged worktree (2026-09-14) |

## CHANGELOG

| Version | Date | Author | Change | Status |
| --- | --- | --- | --- | --- |
| 0.1 | `2026-09-14` | `AI Agent` | Initial code-first draft paired with SBG-TDS v1.0 | Draft |
| 0.2 | `2026-09-14` | `AI Agent — Amelia (Dev Agent)` | Implemented Backend/Mobile/Web; 18/18 SBG TCs pass; Red Gate recorded for 16 non-guard TCs; deviations and out-of-scope failures in §5.4 | Approved |

## TABLE OF CONTENTS

1. [Module Information and AI Generation Context](#1-module-information-and-ai-generation-context)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry, Exit, and Suspension Criteria](#6-entry-exit-and-suspension-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Module Information and AI Generation Context

### 1.1 Module Information

| Item | Specification | Oracle Source |
| --- | --- | --- |
| Actor goal | Mother selects a baby in the chat attachment menu and sends a card. Mother and expert see 3 growth trend charts over all measurements and an expandable history list below them. | `SRC-DEC-01` (UD-01..UD-08) |
| Current implementation state | Not implemented (health/checklist shares exist as precedent) | `SRC-CODE-01..05` |
| Supported entry points | Mobile attachment menu in `DirectChatScreen`; Web `ChatPanel` timeline; Web `ExpertSharedRecordsPage`; API `GET /api/v1/babies/{babyId}/growth-chart` | TDS §9.1 |
| In-scope layers | Backend, Web, Mobile | TDS §5.1 |
| Out-of-scope layers | AI Service — not involved | TDS §1.3 |
| Protected or sensitive data | `weightKg`, `heightCm`, `headCircumferenceCm`, `nickname`, `birthDate` — Sensitive-PII | TDS §5.3 |
| Authorization boundary | growth-chart: owner / care group `BABY_VIEW` / any ExpertProfile; history+write: unchanged MOTHER/FAMILY | TDS §16 |
| Primary state transitions | Not applicable — no persisted lifecycle; UI `collapsed ↔ expanded` | TDS §6.4 |
| External dependencies | Not applicable — no third-party provider | TDS §5.1 |

### 1.2 AI Generation Context (CASE 2.0)

This document may be drafted with AI assistance, but every expected value must be
grounded in an explicit oracle. Current code is evidence of current behavior; it is
not automatically an approved business requirement. Contradictions remain `Open`
until a recorded decision selects the authoritative behavior.

| Control | Required value |
| --- | --- |
| Generation mode | Evidence-first; no invented contracts or pass results |
| Permitted sources | User decisions 2026-09-14, paired TDS, exact code/schema/tests listed in §1.3 |
| Trust level | Draft until human review |
| Unknown handling | `Open — <question>; evidence needed: <source/decision>` |
| Non-applicable handling | `Not applicable — <feature-specific reason>` |
| Existing test status | Evidence only; rerun before recording current pass/fail |
| Safety constraint | Synthetic babies/measurements only; no production credentials or real child data |
| Constraints injected | TDS §17.1 C1–C8 |

### 1.3 Reference Baseline

| Ref ID | Type | Exact path / locator / symbol | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-DEC-01` | User decision | UD-01..UD-08 (TDS §2) | 2026-09-14 | Approved (user) |
| `SRC-TDS-01` | Design | `ShareBabyGrowthInDirectChat_TDS.md` §§3, 5.3, 6, 9, 11.2, 16 | 1.0 | Draft |
| `SRC-DB-01` | Schema | `CareBridgeAPI/src/main/resources/db/migration/V12__restore_direct_message_body_check.sql` | worktree | Applied migration |
| `SRC-CODE-01` | Current code | `CareBridgeAPI/.../carejourney/service/impl/GrowthServiceImpl.java::getGrowthChart, assertViewAccess, assertWriteAccess` | worktree | Current-state evidence |
| `SRC-CODE-02` | Current code | `CareBridgeAPI/.../carejourney/controller/GrowthChartController.java`, `GrowthMeasurementController.java` | worktree | Current-state evidence |
| `SRC-CODE-03` | Current code | `CareBridgeAPI/.../health/service/impl/HealthMetricServiceImpl.java::requireTrendAccess` | worktree | Precedent for UD-05 |
| `SRC-CODE-04` | Current code | `CareBridgeMobileApp/lib/features/directChat/widgets/health_metrics_message_card.dart::HealthMetricsShareData.parse/serialize`; `screens/direct_chat_screen.dart:450-488,1215-1219,1424-1440` | worktree | Precedent |
| `SRC-CODE-05` | Current code | `CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts::parseHealthMetricsShare, fetchExpertSharedRecords`; `ExpertSharedRecordsPage.tsx` | worktree | Precedent |
| `SRC-CODE-06` | Current code | `CareBridgeMobileApp/lib/features/baby/models/baby_model.dart::BabyProfile.ageLabel` | worktree | Age-format oracle |
| `SRC-CODE-07` | Current code | `CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart::_buildTrendChart` (empty/range texts) | worktree | UI text oracle |
| `SRC-TEST-01` | Existing test | `CareBridgeMobileApp/test/features/baby/baby_growth_charts_tab_test.dart` | staged | Regression evidence |
| `SRC-TEST-02` | Existing test | `CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java` | worktree | Pattern evidence |
| `SRC-TEST-03` | Existing test | `CareBridgeMobileApp/test/features/directChat/direct_chat_screen_test.dart::'renders shared health metrics as an interactive health card'` | worktree | Pattern evidence |

---

## 2. Logic Issues Resolved

| Issue ID | Competing sources / observed discrepancy | Impact | Resolution | Decision / Oracle Source | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | UD-02 needs all measurements, but `V12` limits body to 2000 chars | Snapshot design cannot pass for >~40 measurements | Reference payload + live fetch (ADR-SBG-001) | `SRC-DB-01`, `SRC-TDS-01` §3 | Resolved |
| `LI-02` | Live fetch needs EXPERT read, but `GrowthChartController` denies EXPERT | Expert card cannot load | Grant EXPERT on growth-chart only, with any-ExpertProfile rule | `SRC-DEC-01` UD-05; `SRC-CODE-03` | Resolved (DPO Open) |
| `LI-03` | User asked for "ngày giờ", but `measured_date` is `LocalDate` | History row format | Date + age at measurement, no time | `SRC-DEC-01` UD-06 | Resolved |
| `LI-04` | User wrote "vòng đời"; growth data has head circumference | Third chart identity | Interpreted as vòng đầu | TDS OPEN-03 | Resolved pending review |
| `LI-05` | Health card expands per metric; user wants one history list below all 3 charts | UI structure | Follow user request (single combined list) | `SRC-DEC-01` UD-03 | Resolved |

### 2.1 Open Questions Blocking Test Oracles

| Open ID | Question | Why it matters | Evidence / decision needed | Owner | Status |
| --- | --- | --- | --- | --- | --- |
| `OPEN-01` | Audit expert growth-chart reads? | Would add an audit assertion to SBG-TC-001 | DPO/reviewer decision (TDS OPEN-01). Draft asserts **no** audit call. | Reviewer | Open |
| `OPEN-02` | Broad expert read risk acceptance | May change SBG-TC-001/002 if scoping is later required | DPO sign-off | DPO | Open |

---

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Risk / failure mode | Severity | Likelihood | Detectability | In-scope test levels | Mitigation / Test Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Expert role grant leaks to history/write endpoints | Critical | M | M | Unit, WebMvc security | COND-03, COND-04 |
| `RISK-02` | Non-expert, non-owner reads growth chart | Critical | L | M | Unit | COND-02 |
| `RISK-03` | Body exceeds 2000 → send rejected by DB check | High | M | H | Unit | COND-05 |
| `RISK-04` | Card silently filters by month / truncates | High | M | M | Widget, Component | COND-08 |
| `RISK-05` | Fetch failure breaks chat timeline | High | M | H | Widget, Component | COND-10 |
| `RISK-06` | Painter extraction changes baby growth tab | Medium | M | H | Widget regression | COND-12 |
| `RISK-07` | Web Shared Records ignores new type | Medium | M | M | Unit, Component | COND-13, COND-14 |
| `RISK-08` | Card shows clinical interpretation (unsafe) | High | L | M | Widget, Component | COND-15 |

#### Platform and Test-Level Applicability Matrix

| Platform / Layer | Unit | Integration | Contract / Component | Widget / UI | E2E | Security |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | Applicable — `GrowthServiceTest` access rules | Not applicable — no persistence/query change; existing `GrowthMeasurementStoreEmbeddedPostgresTest` unchanged | Applicable — WebMvc slice for `@PreAuthorize` | Not applicable — backend has no UI | Not applicable — no E2E harness for chat share | Applicable — role matrix SBG-TC-005 |
| Web | Applicable — `parseBabyGrowthShare`, `formatBabyAgeAt`, `fetchExpertSharedRecords` | Not applicable — API mocked; no backend-in-loop web tests | Applicable — `SharedBabyGrowthBubble`, `ExpertSharedRecordsPage` with Testing Library | Applicable (same as component) | Not applicable — Playwright suite has no direct-chat fixtures | Not applicable — authorization enforced server-side |
| Mobile | Applicable — `BabyGrowthShareData`, `formatBabyAgeAt` | Not applicable — services faked | Not applicable — covered by widget tests | Applicable — dialog, card, chat screen, growth tab regression | Not applicable — no device E2E harness | Not applicable — authorization enforced server-side |
| AI Service | Not applicable — no AI component | Not applicable | Not applicable | Not applicable — reason: no AI | Not applicable | Not applicable |

### TDS-02 — Test Basis and Oracle Hierarchy

| Basis ID | Requirement / ADR / Rule / Contract | Exact source | Authoritative oracle | Covered by conditions |
| --- | --- | --- | --- | --- |
| `BASIS-01` | UD-05 / ADR-SBG-002 expert read of growth-chart | TDS §3 ADR-SBG-002, §9.2 | ExpertProfile holder → 200 with all measurements ASC | COND-01 |
| `BASIS-02` | Existing deny rules | `SRC-CODE-01` `getBabyOrThrow`, `assertViewAccess` | 404 `BABY-070`; 403 `BABY-071` | COND-02 |
| `BASIS-03` | ADR-SBG-002 scope limit | TDS §16 | EXPERT on history → 403 `BABY-071`; add → 403 + `SECURITY_EVENT` audit | COND-03 |
| `BASIS-04` | Controller role grant | TDS §9.1 | EXPERT allowed on growth-chart; MODERATOR denied; EXPERT denied on growth-measurements | COND-04 |
| `BASIS-05` | BR-DCC-005 + ADR-SBG-001 payload | `SRC-DB-01`; TDS §5.3 | Tag prefix; trimmed length ≤ 2000; no history array | COND-05 |
| `BASIS-06` | Parse rule | TDS §5.3 | null on non-tag / bad JSON / missing babyId | COND-06 |
| `BASIS-07` | UD-04 picker | TDS §6.2 ALT-01..03 | preselect/auto-select/empty state | COND-07 |
| `BASIS-08` | UD-02 charts | TDS §11.2 layout; `SRC-CODE-07` | 3 titled charts over all points | COND-08 |
| `BASIS-09` | UD-03/UD-06 history | TDS §11.2 layout, §15 sample; `SRC-CODE-06` | Toggle labels, newest first, row format | COND-09 |
| `BASIS-10` | ERR-01, ALT-04 | TDS §6.2, §6.3 | Fallback text + snapshot; empty texts | COND-10 |
| `BASIS-11` | UD-01/UD-03 chat integration | TDS §11.2 step 6; ALT-07, ERR-02 | Menu option; card for own+counterpart; recalled not card | COND-11 |
| `BASIS-12` | ADR-SBG-003 | `SRC-TEST-01` | Existing test passes unchanged | COND-12 |
| `BASIS-13` | UD-07 web records | TDS §5.1 | `type: 'BABY_GROWTH'` entries; ChatPanel route | COND-13 |
| `BASIS-14` | UD-07 web page | TDS §11.2 step 7 | Tab "Tăng trưởng bé" filters cards | COND-14 |
| `BASIS-15` | Safety C7 | TDS §17.1 | No status/percentile labels ("Bình thường", "Nguy hiểm", "Cần lưu ý") in growth card | COND-15 |

Oracle precedence for this feature:

1. User decisions UD-01..UD-08 (2026-09-14)
2. Applied schema rule BR-DCC-005 (`V12`) and `CLAUDE.md` Safety Rules
3. Paired TDS contract (SBG-TDS v1.0)
4. Current implementation evidence (`SRC-CODE-*`) for existing deny rules and UI text precedents
5. Existing automated tests (`SRC-TEST-*`) as regression evidence only

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Requirement / risk | Condition | Layer / platform | Coverage type | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | BASIS-01 | Expert (non-owner, no care group) gets full ASC chart | Backend | Positive | SBG-TC-001 |
| `COND-02` | BASIS-02 / RISK-02 | Non-expert stranger → 403; unknown baby → 404 | Backend | Negative / Security | SBG-TC-002, SBG-TC-004 |
| `COND-03` | BASIS-03 / RISK-01 | Expert profile does not grant history or write | Backend | Security | SBG-TC-003 |
| `COND-04` | BASIS-04 / RISK-01 | Controller role matrix | Backend | Security | SBG-TC-005 |
| `COND-05` | BASIS-05 / RISK-03 | Serialize: tag, ≤2000 at boundary, no history | Mobile | Boundary / Privacy | SBG-TC-006 |
| `COND-06` | BASIS-06 | Parse valid/invalid | Mobile, Web | Negative | SBG-TC-007, SBG-TC-014 |
| `COND-07` | BASIS-07 | Picker preselect / single / empty | Mobile | Positive / Alternative | SBG-TC-008 |
| `COND-08` | BASIS-08 / RISK-04 | 3 charts over all measurements incl. >12 months old | Mobile, Web | Positive | SBG-TC-009, SBG-TC-015 |
| `COND-09` | BASIS-09 | History toggle below charts, rows newest first, null `—` | Mobile, Web | Positive / State | SBG-TC-010, SBG-TC-015 |
| `COND-10` | BASIS-10 / RISK-05 | Fetch failure fallback; empty measurements | Mobile, Web | Resilience | SBG-TC-011, SBG-TC-016 |
| `COND-11` | BASIS-11 | Chat menu option, card routing, recalled plain | Mobile | Integration (widget) | SBG-TC-012 |
| `COND-12` | BASIS-12 / RISK-06 | Growth tab regression | Mobile | Regression | SBG-TC-013 |
| `COND-13` | BASIS-13 / RISK-07 | Shared records service emits BABY_GROWTH | Web | Positive | SBG-TC-017 |
| `COND-14` | BASIS-14 / RISK-07 | Shared Records tab filter | Web | Positive | SBG-TC-018 |
| `COND-15` | BASIS-15 / RISK-08 | No clinical status labels | Mobile, Web | Safety | SBG-TC-009, SBG-TC-015 |

#### State and Transition Coverage

| State / invariant | Allowed transition or observation | Forbidden transition | Oracle Source | Test cases |
| --- | --- | --- | --- | --- |
| History `collapsed` (default) | tap toggle → `expanded` | Rows visible before tap | TDS §6.4, UD-03 | SBG-TC-010, SBG-TC-015 |
| History `expanded` | tap "Thu gọn lịch sử" → `collapsed` | — | UD-03 | SBG-TC-010, SBG-TC-015 |
| Card `loading → loaded` | fetch resolves | — | TDS §6.4 | SBG-TC-009 |
| Card `loading → failed` | fetch rejects → fallback | Crash / blank | ERR-01 | SBG-TC-011, SBG-TC-016 |
| Message recalled | plain rendering | Rich card | ALT-07 | SBG-TC-012 |

#### API and Error Coverage

| Endpoint / interface | Auth / role | Success contract | Validation / domain errors | Ownership / security errors | Test cases |
| --- | --- | --- | --- | --- | --- |
| `GET /api/v1/babies/{babyId}/growth-chart` | MOTHER/FAMILY/EXPERT + service rule | 200 `GrowthChartResponse` ASC | 404 `BABY-070` | 403 `BABY-071`; 403 role | SBG-TC-001, 002, 004, 005 |
| `GET /api/v1/babies/{babyId}/growth-measurements` | MOTHER/FAMILY | unchanged | unchanged | EXPERT → 403 | SBG-TC-003, SBG-TC-005 |
| `POST /api/v1/babies/{babyId}/growth-measurements` | MOTHER/FAMILY | unchanged | unchanged | expert service path → 403 `BABY-071` + audit | SBG-TC-003 |

### TDS-04 — Test Techniques

| Technique | Applied to | Rationale | Conditions / Test cases |
| --- | --- | --- | --- |
| Equivalence partitioning | Caller classes: owner, care-group viewer, expert, stranger | Access rule has distinct branches | COND-01, COND-02 / TC-001, 002 |
| Boundary value analysis | Body length 2000 vs 2001; note 500 | DB check boundary | COND-05 / TC-006 |
| Decision table | Role × endpoint | Role grant must be precise | COND-04 / TC-005 |
| State-transition testing | History collapsed/expanded; card loading/failed | UI states sourced in UD-03 | COND-09, COND-10 / TC-010, 011, 015, 016 |
| Pairwise / combinatorial | Not applicable — few independent parameters | — | — |
| Error guessing | Null series values; malformed tag JSON; recalled message | Known precedents in health card | COND-06, COND-09, COND-11 |
| Contract testing | growth-chart JSON → client parse | Client relies on field names `measuredDate`, `weightKg`, `heightCm`, `headCircumferenceCm`. TC-009/015 mock the loader, so the seam is covered only on Web by `babyGrowthShare.test.ts › fetchBabyGrowthChart contract`; Mobile `GrowthMeasurementService.getGrowthChartMeasurements` has no automated contract test (it calls `apiGet` directly, no injection) | COND-08 / TC-009, 015 + Web contract test |

### TDS-05 — Test Data, Fixtures, Environment, and Isolation

#### Data Requirements

| Data ID | Purpose | Minimal synthetic fields | Boundary / variants | Source / factory | Cleanup |
| --- | --- | --- | --- | --- | --- |
| `DATA-01` | Baby owned by mother | `id=BABY_ID`, `ownerUserId=MOTHER_ID`, `nickname="Bé Test"`, `birthDate=2025-01-10`, ACTIVE | archived variant not needed | `GrowthServiceTest.makeBaby()` (existing) | Mock only |
| `DATA-02` | Measurements | 3 rows: 2025-01-10 (3.2/50.0/34.0), 2025-02-10 (4.5/54.5/null), 2025-03-10 (5.6/58.0/39.0) | plus a recent row 2026-09-01 (9.8/75.0/45.0) in UI tests so data spans 20 months (defeats any 1/3/6/12-month filter) | `makeMeasurements()` / Dart `makeGrowthMeasurements()` / TS `makeGrowthPoints()` | In-memory |
| `DATA-03` | Expert caller | `EXPERT_ID`, `ExpertProfile` present | stranger `STRANGER_ID` without profile | Mockito `expertProfileRepository` | Mock only |
| `DATA-04` | Share data | `BabyGrowthShareData` defaults per TDS §15 | note 500 chars; nickname 100 chars | `makeShareData()` | In-memory |
| `DATA-05` | Babies list | 1, 2 and 0 babies | last-opened id matches second | Dart fake `BabyService` + `BabyProfileSelectionStorage` | In-memory |

#### Determinism and Isolation Controls

| Concern | Required control | Exact implementation / intended path |
| --- | --- | --- |
| Clock | Age-at-measurement uses `birthDate` and `measuredDate` only; header "age now" not asserted exactly | `formatBabyAgeAt(birthDate, at)` pure function |
| Randomness / IDs | Fixed UUID constants | Test classes |
| Authentication | Mockito caller IDs; WebMvc `@WithMockUser(roles=…)` | `GrowthChartControllerSecurityTest` |
| Database | Not applicable — no persistence change | — |
| External providers | Not applicable | — |
| Event delivery | Not applicable — no events | — |
| Files / media | Not applicable | — |
| AI model / embeddings | Not applicable | — |
| Sensors / camera / location | Not applicable | — |
| Network (Mobile) | Injectable loader: `BabyGrowthMessageCard(loadMeasurements: …)`, `ShareBabyGrowthDialog(babyLoader:, selectionStorage:, measurementsLoader:)` | planned constructor params |
| Network (Web) | `vi.mock('../../../shared/api/apiClient')` / mock `fetchBabyGrowthChart` | Vitest |

#### Environment Matrix

| Environment | Purpose | Dependencies | Secrets/data policy | Supported command |
| --- | --- | --- | --- | --- |
| Local isolated (Backend) | Unit/WebMvc | JDK 21, Maven wrapper | Synthetic; move `.env` aside to mirror CI (known contamination) | `./mvnw test -Dtest=GrowthServiceTest,GrowthChartControllerSecurityTest` |
| Local isolated (Web) | Unit/component | Node, jsdom | Synthetic | `npm run test:run` |
| Local isolated (Mobile) | Unit/widget | Flutter SDK | Synthetic | `flutter test test/features/directChat test/features/baby` |
| Test container | Not applicable — no persistence change | — | — | — |
| Approved sandbox | Not applicable — no provider | — | — | — |

---

## 4. Test Case Specification

### 4.1 Props Isolation Boilerplate (CASE 2.0 — Required)

#### Java / Kotlin Example

```java
// GrowthServiceTest (extend existing factories)
private BabyProfile makeBaby(Consumer<BabyProfile> overrides) { /* owner MOTHER_ID, birthDate 2025-01-10, ACTIVE */ }
private List<GrowthMeasurement> makeMeasurements() { /* DATA-02, ASC */ }
private ExpertProfile makeExpertProfile(UUID userId) { /* minimal profile with userId */ }
```

#### TypeScript / React Example

```ts
const makeShareData = (overrides: Partial<BabyGrowthShareData> = {}): BabyGrowthShareData => ({
  title: 'Phát triển của bé',
  babyId: '11111111-1111-1111-1111-111111111111',
  babyNickname: 'Bé Test',
  birthDate: '2025-01-10',
  measurementCount: 3,
  latest: { measuredDate: '2025-03-10', weightKg: 5.6, heightCm: 58.0, headCircumferenceCm: 39.0 },
  isLiveSync: true,
  note: undefined,
  ...overrides,
});
const makeGrowthPoints = (overrides: Partial<BabyGrowthPoint>[] = []): BabyGrowthPoint[] => /* DATA-02 */ [];
```

#### Dart / Flutter Example

```dart
BabyGrowthShareData makeShareData({String? note, String nickname = 'Bé Test', int count = 3}) => BabyGrowthShareData(
  babyId: '11111111-1111-1111-1111-111111111111', babyNickname: nickname,
  birthDate: DateTime(2025, 1, 10), measurementCount: count, latest: makeLatest(), note: note);

List<GrowthMeasurement> makeGrowthMeasurements({bool includeOld = false}) => [/* DATA-02 */];

Widget makeCard({BabyGrowthShareData? data, Future<List<GrowthMeasurement>> Function(String)? loader, bool own = false}) =>
  MaterialApp(home: Scaffold(body: SingleChildScrollView(child: BabyGrowthMessageCard(
    data: data ?? makeShareData(), isOwnMessage: own,
    loadMeasurements: loader ?? (_) async => makeGrowthMeasurements()))));
```

### 4.2 Detailed Test Cases

### `SBG-TC-001` — Expert without ownership reads full growth chart

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-001` |
| Severity | Critical |
| Test Condition | `COND-01` |
| Test Level | Unit |
| Platform / Layer | Backend |
| Technique | Equivalence partitioning |
| Oracle Source | `BASIS-01` (TDS ADR-SBG-002, §9.2) |
| Preconditions | Baby DATA-01 owned by MOTHER_ID; caller EXPERT_ID; `babyAccessPolicy.canView` → false; `expertProfileRepository.findByUserId(EXPERT_ID)` → present |
| Intended Test File | Existing `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java` (add `@Mock ExpertProfileRepository`) |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Stub `babyProfileRepository.findById(BABY_ID)` → DATA-01; `growthMeasurementStore.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc` → DATA-02.
2. Stub canView false, expert profile present.

**Act**

1. `growthService.getGrowthChart(EXPERT_ID, BABY_ID)`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | `measurements.size()==3`; dates ASC 2025-01-10, 2025-02-10, 2025-03-10; `ageInDays` 0, 31, 59; `nickname=="Bé Test"` | BASIS-01; `SRC-CODE-01` mapping |
| Persistence | No save/delete on store | TDS POST-03 |
| Audit | `verifyNoInteractions(auditService)` (Draft; see OPEN-01) | TDS OPEN-01 |
| Event / notification | N/A — no events | TDS §7 |
| Provider side effect | N/A | — |
| UI state | N/A | — |
| Privacy / logging | N/A | — |

**Failure signature**

`BusinessException BABY-071` thrown → expert rule missing in `assertChartViewAccess`.

**Cleanup / isolation**

Mockito per-test mocks.

### `SBG-TC-002` — Stranger without expert profile is denied

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-002` |
| Severity | Critical |
| Test Condition | `COND-02` |
| Test Level | Unit |
| Platform / Layer | Backend |
| Technique | Equivalence partitioning |
| Oracle Source | `BASIS-02` (`GrowthServiceImpl.assertViewAccess` BABY-071) |
| Preconditions | Caller STRANGER_ID; canView false; `findByUserId(STRANGER_ID)` → empty |
| Intended Test File | Existing `.../carejourney/service/GrowthServiceTest.java` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Stub baby DATA-01; canView false; expert profile empty.

**Act**

1. `growthService.getGrowthChart(STRANGER_ID, BABY_ID)`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | `BusinessException` status 403, code `BABY-071` | BASIS-02 |
| Persistence | `growthMeasurementStore` never queried | TDS §6.3 ERR-06 |
| Audit | No audit call | `SRC-CODE-01` (read path has no audit) |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | N/A | — |
| Privacy / logging | No measurement data returned | TDS §16 |

**Failure signature**

Returns a response → access widened to all users.

**Cleanup / isolation**

Mockito per-test mocks.

### `SBG-TC-003` — Expert profile does not unlock history or write

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-003` |
| Severity | Critical |
| Test Condition | `COND-03` |
| Test Level | Unit |
| Platform / Layer | Backend |
| Technique | Decision table |
| Oracle Source | `BASIS-03` (TDS ADR-SBG-002 Decision, §16) |
| Preconditions | Caller EXPERT_ID with profile; canView false; canManageGrowth false |
| Intended Test File | Existing `.../carejourney/service/GrowthServiceTest.java` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Stub baby DATA-01 ACTIVE; expert profile present (lenient); policy false.

**Act**

1. `getGrowthMeasurementHistory(EXPERT_ID, BABY_ID, PageRequest.of(0, 20))`.
2. `addGrowthMeasurement(EXPERT_ID, BABY_ID, validAddRequest)`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Both throw 403 `BABY-071` | `SRC-CODE-01` assertViewAccess/assertWriteAccess |
| Persistence | No `save` on store | TDS POST-03 |
| Audit | Add path: `auditService.log(AuditAction.SECURITY_EVENT, EXPERT_ID, "GROWTH_MEASUREMENT_ACCESS_DENIED", BABY_ID.toString(), "Growth write permission denied")` once | `SRC-CODE-01` assertWriteAccess |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | N/A | — |
| Privacy / logging | N/A | — |

**Failure signature**

History returns a page → shared `assertViewAccess` was widened instead of the chart-only helper.

**Cleanup / isolation**

Mockito per-test mocks.

> Red note: this TC characterizes an invariant that already holds. It must be confirmed RED by temporarily running it against a stub where `assertViewAccess` includes the expert rule; otherwise record it as a regression guard (see §5.2).

### `SBG-TC-004` — Unknown baby returns 404 for expert

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-004` |
| Severity | Medium |
| Test Condition | `COND-02` |
| Test Level | Unit |
| Platform / Layer | Backend |
| Technique | Equivalence partitioning |
| Oracle Source | `BASIS-02` (`getBabyOrThrow` BABY-070) |
| Preconditions | `findById` → empty; caller EXPERT_ID |
| Intended Test File | Existing `.../carejourney/service/GrowthServiceTest.java` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Stub `babyProfileRepository.findById(UNKNOWN_ID)` → `Optional.empty()`.

**Act**

1. `getGrowthChart(EXPERT_ID, UNKNOWN_ID)`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | 404 `BABY-070` | BASIS-02 |
| Persistence | Store not queried | `SRC-CODE-01` |
| Audit | None | `SRC-CODE-01` |
| Event / notification | N/A | — |
| Provider side effect | `expertProfileRepository` not invoked | TDS §6.3 (not-found before access) |
| UI state | N/A | — |
| Privacy / logging | N/A | — |

**Failure signature**

403 returned instead of 404 → access check ordered before lookup.

**Cleanup / isolation**

Mockito per-test mocks.

### `SBG-TC-005` — Controller role matrix for growth endpoints

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-005` |
| Severity | Critical |
| Test Condition | `COND-04` |
| Test Level | Contract (WebMvc security slice) |
| Platform / Layer | Backend |
| Technique | Decision table |
| Oracle Source | `BASIS-04` (TDS §9.1, §16) |
| Preconditions | `@MockitoBean IGrowthService` returns a stub `GrowthChartResponse`; method security enabled |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/controller/GrowthChartControllerSecurityTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Principal factory with user id for each role: EXPERT, MOTHER, MODERATOR, SYSTEM_ADMIN; anonymous.

**Act**

1. `GET /api/v1/babies/{BABY_ID}/growth-chart` per role.
2. `GET /api/v1/babies/{BABY_ID}/growth-measurements` as EXPERT.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | EXPERT 200; MOTHER 200; MODERATOR 403; SYSTEM_ADMIN 403; anonymous 401; EXPERT on growth-measurements 403 | TDS §16 |
| Persistence | N/A — service mocked | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | Service invoked only for 200 cases | TDS §9.2 |
| UI state | N/A | — |
| Privacy / logging | N/A | — |

**Failure signature**

EXPERT 403 on growth-chart → `@PreAuthorize` not updated; EXPERT 200 on growth-measurements → over-grant.

**Cleanup / isolation**

Spring test context per class. Pattern: existing `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/systemconfiguration/SystemConfigurationControllerSecurityTest.java` (`@WebMvcTest` asserting a `@PreAuthorize` denial). No `RoleHierarchy` bean exists in `src/main/java` (checked 2026-09-14), so SYSTEM_ADMIN does not inherit EXPERT and `403` is the expected result.

### `SBG-TC-006` — Serialized share is tagged, bounded and has no history

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-006` |
| Severity | High |
| Test Condition | `COND-05` |
| Test Level | Unit |
| Platform / Layer | Mobile |
| Technique | Boundary value analysis |
| Oracle Source | `BASIS-05` (`SRC-DB-01` ≤2000; TDS §5.3) |
| Preconditions | None |
| Intended Test File | Planned — `05_Development/CareBridgeMobileApp/test/features/directChat/baby_growth_share_data_test.dart` (not present at Draft baseline) |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. `makeShareData(note: 'a' * 500, nickname: 'B' * 100)`.

**Act**

1. `final body = data.serialize();`
2. `BabyGrowthShareData.fitsMessageLimit(body)` (planned static helper used by ERR-04).

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | `body.startsWith('[CAREBRIDGE_BABY_GROWTH_SHARE]')`; `body.trim().length <= 2000`; decoded JSON has no key `measurements`/`history`; `babyId` equals input; `fitsMessageLimit('x' * 2001)` is false and `('x' * 2000)` is true | BASIS-05 |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | N/A | — |
| Privacy / logging | Only `latest` snapshot present | ADR-SBG-001 |

**Failure signature**

Length > 2000 or `measurements` key present → snapshot overflow design.

**Cleanup / isolation**

Pure function.

### `SBG-TC-007` — Mobile parse rejects non-tag, malformed and babyId-less bodies

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-007` |
| Severity | Medium |
| Test Condition | `COND-06` |
| Test Level | Unit |
| Platform / Layer | Mobile |
| Technique | Error guessing |
| Oracle Source | `BASIS-06` (TDS §5.3 parse rule) |
| Preconditions | None |
| Intended Test File | Planned — `.../test/features/directChat/baby_growth_share_data_test.dart` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Inputs: `null`; `'hello'`; `'[CAREBRIDGE_HEALTH_SHARE]\n{}'`; `'[CAREBRIDGE_BABY_GROWTH_SHARE]\n{bad'`; `'[CAREBRIDGE_BABY_GROWTH_SHARE]\n{"babyNickname":"x"}'`; valid `makeShareData().serialize()`.

**Act**

1. `BabyGrowthShareData.parse(input)`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | First five → `null`; valid → non-null with `babyNickname=='Bé Test'`, `measurementCount==3`, `latest.weightKg==5.6`, `isLiveSync==true` | BASIS-06 |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | N/A | — |
| Privacy / logging | N/A | — |

**Failure signature**

Exception thrown or non-null for invalid input.

**Cleanup / isolation**

Pure function.

### `SBG-TC-008` — Baby picker preselects, auto-selects and handles no babies

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-008` |
| Severity | High |
| Test Condition | `COND-07` |
| Test Level | Widget |
| Platform / Layer | Mobile |
| Technique | Equivalence partitioning |
| Oracle Source | `BASIS-07` (TDS §6.2 ALT-01..03) |
| Preconditions | Injected `babyLoader`, `selectionStorage`, `measurementsLoader` fakes (DATA-05) |
| Intended Test File | Planned — `.../test/features/directChat/share_baby_growth_dialog_test.dart` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Variant A: babies [A "Bé An", B "Bé Bình"], last opened = B.
2. Variant B: one baby A.
3. Variant C: no babies.

**Act**

1. Open dialog via a button that calls `ShareBabyGrowthDialog.show(context)`; `pumpAndSettle`.
2. Variant A: tap send button `find.byKey(Key('share-baby-growth-send'))`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | A: returned data `babyId == B.id`, `measurementCount == 3`. B: A selected. C: send button disabled; returns null on dismiss | ALT-01..03 |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | `measurementsLoader` called with selected baby id | TDS §6.1 |
| UI state | Title `Chia sẻ phát triển của bé`; C shows `Chưa có hồ sơ bé` | TDS §6.2 |
| Privacy / logging | N/A | — |

**Failure signature**

The first baby is chosen despite last-opened B, or send is enabled with no baby.

**Cleanup / isolation**

`tester.view` reset.

### `SBG-TC-009` — Card renders three charts over all measurements without clinical labels

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-009` |
| Severity | High |
| Test Condition | `COND-08`, `COND-15` |
| Test Level | Widget |
| Platform / Layer | Mobile |
| Technique | Contract testing |
| Oracle Source | `BASIS-08`, `BASIS-15` (TDS §11.2 layout; `SRC-CODE-07` range text format) |
| Preconditions | Loader returns DATA-02 plus recent row 2026-09-01 (9.8 kg / 75.0 cm / 45.0 cm) |
| Intended Test File | Planned — `.../test/features/directChat/baby_growth_message_card_test.dart` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. `makeCard(loader: (_) async => makeGrowthMeasurements(includeOld: true))`, view 400×1600.

**Act**

1. `pumpAndSettle`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | N/A | — |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | Loader called once with `11111111-1111-1111-1111-111111111111` | ADR-SBG-001 |
| UI state | Texts `Phát triển của bé`, `Xu hướng cân nặng`, `Xu hướng chiều cao`, `Xu hướng vòng đầu` each found once, in that vertical order. Range texts (first → last chronological value, per `SRC-CODE-07`) `3.2 kg – 9.8 kg`, `50.0 cm – 75.0 cm`, `34.0 cm – 45.0 cm` (proves 2025 rows are not filtered out). Subtitle contains `4 lần đo`. | UD-02; `SRC-CODE-07` |
| Privacy / logging | `find.text('Bình thường')`, `Cần lưu ý`, `Nguy hiểm` → findsNothing | C7 |

**Failure signature**

Range caption does not start at `3.2 kg` → a period filter was applied.

**Cleanup / isolation**

`tester.view` reset.

### `SBG-TC-010` — History toggle below charts, newest first, null shown as dash

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-010` |
| Severity | High |
| Test Condition | `COND-09` |
| Test Level | Widget |
| Platform / Layer | Mobile |
| Technique | State-transition testing |
| Oracle Source | `BASIS-09` (TDS §11.2 layout, §15 sample; `SRC-CODE-06` age rules) |
| Preconditions | Loader returns DATA-02 |
| Intended Test File | Planned — `.../test/features/directChat/baby_growth_message_card_test.dart` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. `makeCard()`.

**Act**

1. `pumpAndSettle`; tap `Xem lịch sử đo (3)`; `pumpAndSettle`; tap `Thu gọn lịch sử`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | N/A | — |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | No additional loader call on toggle | TDS §6.4 |
| UI state | Before tap: `10/03/2025 · 2 tháng tuổi` findsNothing. After tap: rows `10/03/2025 · 2 tháng tuổi`, `10/02/2025 · 1 tháng tuổi`, `10/01/2025 · 0 ngày tuổi` in descending vertical order; values `5.6 kg · 58.0 cm · 39.0 cm`, `4.5 kg · 54.5 cm · —`. Toggle's top is below the `Xu hướng vòng đầu` chart's bottom. After collapse: rows findsNothing | UD-03, UD-06, TDS §15 |
| Privacy / logging | N/A | — |

**Failure signature**

Rows visible initially, ascending order, or `null` text shown.

**Cleanup / isolation**

`tester.view` reset.

### `SBG-TC-011` — Card falls back on fetch failure and shows empty states

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-011` |
| Severity | High |
| Test Condition | `COND-10` |
| Test Level | Widget |
| Platform / Layer | Mobile |
| Technique | Error guessing |
| Oracle Source | `BASIS-10` (TDS ERR-01, ALT-04; `SRC-CODE-07` empty texts) |
| Preconditions | Variant A loader throws `Exception('403')`; Variant B loader returns `[]` with `makeShareData(count: 0)` and `latest: null` |
| Intended Test File | Planned — `.../test/features/directChat/baby_growth_message_card_test.dart` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Build both variants.

**Act**

1. `pumpAndSettle`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | `tester.takeException()` is null | ERR-01 |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | A: `Không thể tải dữ liệu tăng trưởng mới nhất` plus snapshot text containing `5.6 kg`. B: `Chưa có dữ liệu cân nặng.`, `Chưa có dữ liệu chiều cao.`, `Chưa có dữ liệu vòng đầu.`; toggle `Xem lịch sử đo` findsNothing | ERR-01; ALT-04 |
| Privacy / logging | Error text does not include exception message `403` | TDS §6.3 |

**Failure signature**

Exception rethrown / infinite spinner.

**Cleanup / isolation**

`tester.view` reset.

### `SBG-TC-012` — Chat screen offers the option and renders the card for both sides

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-012` |
| Severity | High |
| Test Condition | `COND-11` |
| Test Level | Widget |
| Platform / Layer | Mobile |
| Technique | Error guessing |
| Oracle Source | `BASIS-11` (TDS §11.2 step 6; ALT-07; `SRC-TEST-03` pattern) |
| Preconditions | Fake `DirectChatService` like `SRC-TEST-03`, with a timeline containing: counterpart growth share, own growth share, recalled growth share |
| Intended Test File | Existing `05_Development/CareBridgeMobileApp/test/features/directChat/direct_chat_screen_test.dart` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Timeline items built from `makeShareData().serialize()`; third item has `recalledAt` set.

**Act**

1. Pump screen; open attachment menu.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | N/A | — |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | Menu shows `Chia sẻ phát triển của bé`. `find.byType(BabyGrowthMessageCard)` findsNWidgets(2); recalled item not a card; raw tag text `[CAREBRIDGE_BABY_GROWTH_SHARE]` findsNothing for the two cards | UD-01, UD-03, ALT-07 |
| Privacy / logging | N/A | — |

**Failure signature**

Raw JSON shown in bubble → routing not wired.

**Cleanup / isolation**

Restore `DirectChatService.instance` as existing `setUp` does.

### `SBG-TC-013` — Baby growth tab unchanged after painter extraction

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-013` |
| Severity | Medium |
| Test Condition | `COND-12` |
| Test Level | Widget (regression) |
| Platform / Layer | Mobile |
| Technique | Error guessing |
| Oracle Source | `BASIS-12` (`SRC-TEST-01`) |
| Preconditions | Existing test unchanged |
| Intended Test File | Existing `05_Development/CareBridgeMobileApp/test/features/baby/baby_growth_charts_tab_test.dart` |
| Initial Status | `🔴 Not written` (existing test; rerun evidence required) |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Record passing result before refactor.

**Act**

1. `flutter test test/features/baby/baby_growth_charts_tab_test.dart` after extraction.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | All tests in file pass with no edits to the test file | SRC-TEST-01 |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | e.g. `Xu hướng cân nặng`, `3.2 kg – 4.5 kg` still found | SRC-TEST-01 |
| Privacy / logging | N/A | — |

**Failure signature**

Any failure in the file.

**Cleanup / isolation**

Not applicable — existing test.

> Red note: regression guard; RED is not expected. Record the pre-refactor pass as baseline (§5.2).

### `SBG-TC-014` — Web parse of growth share

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-014` |
| Severity | Medium |
| Test Condition | `COND-06` |
| Test Level | Unit |
| Platform / Layer | Web |
| Technique | Error guessing |
| Oracle Source | `BASIS-06` |
| Preconditions | None |
| Intended Test File | Planned — `05_Development/CareBridgeWebApp/src/features/expert/services/babyGrowthShare.test.ts` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Same six inputs as SBG-TC-007 (TS strings); valid body is `BABY_GROWTH_SHARE_TAG + '\n' + JSON.stringify(makeShareData())`.

**Act**

1. `parseBabyGrowthShare(input)`; also `parseHealthMetricsShare(validGrowthBody)`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Invalid → `null`; valid → `babyId` and `latest.headCircumferenceCm === 39`; `parseHealthMetricsShare(validGrowthBody) === null`; `formatBabyAgeAt('2025-01-10','2025-03-10') === '2 tháng tuổi'`, `('2025-01-10','2025-01-10') === '0 ngày tuổi'`, `('2024-01-10','2025-03-10') === '1 tuổi 2 tháng'`, `('2024-01-10','2025-01-10') === '1 tuổi'` | BASIS-06; SRC-CODE-06 |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | N/A | — |
| Privacy / logging | N/A | — |

**Failure signature**

`JSON.parse` exception escapes.

**Cleanup / isolation**

Pure functions.

### `SBG-TC-015` — Web bubble: three charts, all data, toggle history, no clinical labels

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-015` |
| Severity | High |
| Test Condition | `COND-08`, `COND-09`, `COND-15` |
| Test Level | Component |
| Platform / Layer | Web |
| Technique | State-transition testing |
| Oracle Source | `BASIS-08`, `BASIS-09`, `BASIS-15` |
| Preconditions | `vi.mock` `fetchBabyGrowthChart` resolves DATA-02 + recent row 2026-09-01 |
| Intended Test File | Planned — `05_Development/CareBridgeWebApp/src/features/directChat/components/SharedBabyGrowthBubble.test.tsx` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. `render(<SharedBabyGrowthBubble data={makeShareData()} isOwn={false} />)`.

**Act**

1. `await findByText('Xu hướng cân nặng')`; click button `Xem lịch sử đo (4)`; click `Thu gọn lịch sử`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | N/A | — |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | `fetchBabyGrowthChart` called once with babyId | ADR-SBG-001 |
| UI state | Titles for the 3 charts present; 3 `svg` charts rendered; captions `3.2 kg – 9.8 kg`, `50.0 cm – 75.0 cm`, `34.0 cm – 45.0 cm`; history rows hidden initially; after expand the first row text is `01/09/2026 · 1 tuổi 8 tháng`, the second is `10/03/2025 · 2 tháng tuổi`, the last is `10/01/2025 · 0 ngày tuổi`, and a row contains `4.5 kg · 54.5 cm · —`; after collapse rows are removed; the toggle follows the third chart in DOM order (`compareDocumentPosition`) | UD-02, UD-03, UD-06 |
| Privacy / logging | `queryByText('Bình thường')`, `Cần lưu ý`, `Nguy hiểm` → null | C7 |

**Failure signature**

Only two svgs, or captions from the filtered range.

**Cleanup / isolation**

`vi.clearAllMocks()`; RTL auto-cleanup.

### `SBG-TC-016` — Web bubble fallback on fetch failure

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-016` |
| Severity | High |
| Test Condition | `COND-10` |
| Test Level | Component |
| Platform / Layer | Web |
| Technique | Error guessing |
| Oracle Source | `BASIS-10` |
| Preconditions | `fetchBabyGrowthChart` rejects; second variant resolves `[]` with `measurementCount: 0, latest: null` |
| Intended Test File | Planned — `.../directChat/components/SharedBabyGrowthBubble.test.tsx` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Render both variants.

**Act**

1. `await waitFor`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | No uncaught rejection | ERR-01 |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | Reject: `Không thể tải dữ liệu tăng trưởng mới nhất` and text containing `5.6 kg`. Empty: the 3 empty texts from SBG-TC-011; no toggle button | ERR-01, ALT-04 |
| Privacy / logging | N/A | — |

**Failure signature**

Bubble blank or throws.

**Cleanup / isolation**

`vi.clearAllMocks()`.

### `SBG-TC-017` — Shared records service emits BABY_GROWTH entries

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-017` |
| Severity | Medium |
| Test Condition | `COND-13` |
| Test Level | Unit |
| Platform / Layer | Web |
| Technique | Equivalence partitioning |
| Oracle Source | `BASIS-13` (TDS §5.1; `SRC-CODE-05` record shape) |
| Preconditions | `vi.mock('../../directChat/services/directChatApi')`: one conversation `c1` (`counterpartUserId` synthetic) whose timeline has one growth share, one health share, one recalled growth share; apiClient mocked for growth-chart + metrics |
| Intended Test File | Planned — `.../expert/services/babyGrowthShare.test.ts` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Mocks as above.

**Act**

1. `await fetchExpertSharedRecords()`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Exactly one entry `type === 'BABY_GROWTH'` with `babyGrowthData.babyId` set, `conversationId === 'c1'`, `alertLevel === 'NORMAL'`, `status === 'PENDING_REVIEW'`; one `HEALTH_METRICS` entry still present; recalled share excluded | TDS §5.1; SRC-CODE-05 |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | N/A | — |
| Privacy / logging | N/A | — |

**Failure signature**

No BABY_GROWTH entry, or growth body parsed as health.

**Cleanup / isolation**

`vi.resetAllMocks()`.

### `SBG-TC-018` — Shared Records page tab "Tăng trưởng bé"

| Field | Specification |
| --- | --- |
| Stable ID | `SBG-TC-018` |
| Severity | Medium |
| Test Condition | `COND-14` |
| Test Level | Component |
| Platform / Layer | Web |
| Technique | Equivalence partitioning |
| Oracle Source | `BASIS-14` (TDS §11.2 step 7) |
| Preconditions | `fetchExpertSharedRecords` mocked: mother M1 has a BABY_GROWTH record; mother M2 has only a CHECKLIST record; `fetchBabyGrowthChart` resolves DATA-02; router wrapper |
| Intended Test File | Planned — `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.test.tsx` |
| Initial Status | `🔴 Not written` |

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

**Arrange**

1. Render page in `MemoryRouter` with `QueryClientProvider` if required by the page.

**Act**

1. Click tab `Tăng trưởng bé`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | N/A | — |
| Persistence | N/A | — |
| Audit | N/A | — |
| Event / notification | N/A | — |
| Provider side effect | N/A | — |
| UI state | Only M1's card is visible; opening M1's growth section shows `Xu hướng cân nặng` | UD-07 |
| Privacy / logging | N/A | — |

**Failure signature**

Tab missing or M2 still shown.

**Cleanup / isolation**

RTL auto-cleanup; `vi.resetAllMocks()`.

### 4.3 Required Case Families

| Family | Minimum coverage expectation | Case IDs / applicability |
| --- | --- | --- |
| Happy path | Primary actor outcome | SBG-TC-001, 008, 009, 010, 012, 015, 017, 018 |
| Validation and boundaries | Each validated field and critical boundary | SBG-TC-006, 007, 014 |
| Authentication and RBAC | Unauthenticated and disallowed roles | SBG-TC-005 |
| Ownership / membership / consent | Cross-actor isolation | SBG-TC-002, 003 (consent scoping N/A — UD-05) |
| State transitions | Allowed/forbidden UI transitions | SBG-TC-010, 011, 015, 016 |
| Persistence and migration | Not applicable — no schema change (TDS §5.3) | N/A |
| Events / notifications / audit | Audit on denied write; no audit on read (OPEN-01) | SBG-TC-001, 003 |
| External failure | Fetch failure | SBG-TC-011, 016 |
| Concurrency / retries | Double-send guard reuses existing `_sending`; not separately tested (unchanged mechanism) | N/A — reason stated |
| Empty / loading / error / recovery UI | Empty measurements, no babies, fetch failure | SBG-TC-008, 011, 016 |
| Accessibility | Toggle is a labeled button (found by text/role) | SBG-TC-010, 015 |
| Data protection | Minimization of body, no clinical labels, over-grant guard | SBG-TC-003, 005, 006, 009, 015 |

---

## 5. Red-Green-Refactor Tracker

### 5.1 Tracker

| TC ID | Intended test file | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note | Current status |
| --- | --- | --- | --- | --- | --- |
| SBG-TC-001 | `CareBridgeAPI/.../carejourney/service/GrowthServiceTest.java` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-002 | same | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-003 | same | [ ] regression guard — no Red phase | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | Regression guard; unaffected by Red stub | `🟢 Passing` |
| SBG-TC-004 | same | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-005 | Planned — `CareBridgeAPI/.../carejourney/controller/GrowthChartControllerSecurityTest.java` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | Existing BabyCareControllerAuthorizationContractTest updated for ADR-SBG-002 | `🟢 Passing` |
| SBG-TC-006 | Planned — `CareBridgeMobileApp/test/features/directChat/baby_growth_share_data_test.dart` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-007 | same | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-008 | Planned — `CareBridgeMobileApp/test/features/directChat/share_baby_growth_dialog_test.dart` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-009 | Planned — `CareBridgeMobileApp/test/features/directChat/baby_growth_message_card_test.dart` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-010 | same | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-011 | same | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-012 | `CareBridgeMobileApp/test/features/directChat/direct_chat_screen_test.dart` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-013 | `CareBridgeMobileApp/test/features/baby/baby_growth_charts_tab_test.dart` | [ ] regression guard — no Red phase | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | Painter extracted to lib/features/baby/widgets/growth_trend_chart.dart; test file unchanged | `🟢 Passing` |
| SBG-TC-014 | Planned — `CareBridgeWebApp/src/features/expert/services/babyGrowthShare.test.ts` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-015 | Planned — `CareBridgeWebApp/src/features/directChat/components/SharedBabyGrowthBubble.test.tsx` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | Explicit RTL cleanup in afterEach (Vitest has no globals) | `🟢 Passing` |
| SBG-TC-016 | same | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | Explicit RTL cleanup in afterEach (Vitest has no globals) | `🟢 Passing` |
| SBG-TC-017 | Planned — `CareBridgeWebApp/src/features/expert/services/babyGrowthShare.test.ts` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | — | `🟢 Passing` |
| SBG-TC-018 | Planned — `CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.test.tsx` | [x] | 2026-09-14 Passed (uncommitted; base 80cbe4b5f) | Explicit RTL cleanup in afterEach (Vitest has no globals) | `🟢 Passing` |

### 5.2 Red Gate Protocol (CASE 2.0 — GATE-2)

For each new or changed behavior:

1. Write the narrowest applicable test from Section 4.
2. Execute the exact supported command.
3. Confirm failure for the intended missing or incorrect behavior, not setup noise.
4. Record the command, timestamp, environment, and failure signature.
5. Implement the smallest production change only in the implementation phase.
6. Rerun and record green evidence.
7. Refactor while keeping targeted and affected suites green.

Red stubs (implementation phase only):
- Backend: `assertChartViewAccess` body → `throw new UnsupportedOperationException("Not implemented — Red Phase stub");` (TC-001 fails; TC-002/004 fail on wrong exception type).
- Mobile: `BabyGrowthShareData.parse/serialize/fitsMessageLimit`, `BabyGrowthMessageCard.build`, `ShareBabyGrowthDialog.build` → `throw UnimplementedError('Not implemented — Red Phase stub');`
- Web: `parseBabyGrowthShare`, `formatBabyAgeAt`, `SharedBabyGrowthBubble` → `throw new Error('Not implemented — Red Phase stub');`
#### Regression guards — no Red phase

These cases describe behavior that already holds, so they cannot fail against a Red stub. They are excluded from §5.2.1 and verified by the evidence listed here.

| TC ID | Why no Red phase | Required evidence |
| --- | --- | --- |
| SBG-TC-003 | Current `assertViewAccess`/`assertWriteAccess` already deny EXPERT | Passed in the Red run (stub did not touch history/write paths) and in the Green run; optional mutation not performed |
| SBG-TC-013 | Existing growth tab test is green before painter extraction | Post-refactor pass recorded in EVD-02; a separate pre-refactor baseline run was not recorded |

#### 5.2.1 Red Gate Verification

| TC ID | Expected | Actual |
| --- | --- | --- |
| SBG-TC-001 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-002 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-004 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-005 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-006 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-007 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-008 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-009 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-010 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-011 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-012 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-014 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-015 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-016 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-017 | 🔴 FAIL | ☑ FAIL ☐ PASS |
| SBG-TC-018 | 🔴 FAIL | ☑ FAIL ☐ PASS |

Tất cả FAIL? ☑ Yes ☐ No — GATE-2 PASS (16/16 non-guard TCs failed against stubs; base 80cbe4b5f, uncommitted)

### 5.3 Verification Evidence Table

| Evidence ID | Date/time | Environment | Command | Result/counts | Artifact/log | Recorded by |
| --- | --- | --- | --- | --- | --- | --- |
| `EVD-00` | 2026-09-14 (Red) | Local macOS | Same targeted commands as EVD-01..03 against stubs | Backend: TC-001/002/004 `UnsupportedOperationException`, TC-005 EXPERT 403; Mobile: TC-006..012 `UnimplementedError`; Web: 8/8 fail | Console | AI Agent |
| `EVD-01` | 2026-09-14 19:46 | Local macOS | `./mvnw test -Dtest=GrowthServiceTest,GrowthChartControllerSecurityTest` | GrowthServiceTest 28/28, GrowthChartControllerSecurityTest 6/6, 0 failures | `target/surefire-reports` | AI Agent |
| `EVD-02` | 2026-09-14 | Local macOS | `flutter test` on the 4 SBG test files + `test/features/baby/baby_growth_charts_tab_test.dart`; `dart analyze` on changed files | 15 passed, 1 failed (pre-existing `renders shared checklist as an interactive progress card`, fails in isolation, file untouched); `dart analyze`: No issues (`flutter analyze` crashed with analysis server exit 255) | Console | AI Agent |
| `EVD-03` | 2026-09-14 | Local macOS | `npx vitest run` (3 SBG files); `npx eslint` (changed files); `npm run build`; `npx vitest run src/features/directChat src/features/expert` | 8/8 pass, then 9/9 after adding the `fetchBabyGrowthChart` contract test; eslint clean; build succeeded (before the later copy-only edits to page headings); 40/40 pass. Full `npm run lint` / `npm run test:run` not run | Console | AI Agent |
| `EVD-04` | 2026-09-14 19:48 | Local macOS | `./mvnw test` (full) then `./mvnw test -Dtest='com.carebridge.backend.carejourney.**.*Test'` | Full: 4273 run, 2 failures, 272 skipped — contract test (expected by ADR-SBG-002, updated, now passing) and `HealthBoundaryVocabularyTest` (`baby_daily_log_detail_screen.dart:428 FontStyle.normal`, not part of this feature). Package rerun: 124 run, 1 failure (same vocabulary test) | `scratchpad/backend-full-test.log` | AI Agent |
| `EVD-05` | 2026-09-14 | Local macOS | `flutter test test/features/directChat test/features/baby` | 127 passed, 5 failed — all outside SBG scope: checklist card (1), `expert_account_generation_test` (1), `expert_directory_search_test` (3); none import changed files | Console | AI Agent |

### 5.4 Implementation Notes and Deviations (2026-09-14)

| Item | Spec said | Implemented / observed | Impact |
| --- | --- | --- | --- |
| SBG-TC-001/004 data | DATA-02 three rows (2025 dates) | Reused existing `GrowthChartTestFactory` (two rows 2026-02-15/2026-03-15, ageInDays 31/59) | Same oracle intent (all rows ASC, ageInDays), fewer rows |
| Dialog injection | `selectionStorage:` parameter | `lastOpenedBabyIdReader: Future<String?> Function()` (defaults to `BabyProfileSelectionStorage().readLastOpenedBabyProfileId`) | Simpler fake; behavior unchanged |
| SBG-TC-008 | "returns null on dismiss" | Not asserted | Minor gap |
| ERR-02 | Send-failure snackbar | Implemented in `_openShareBabyGrowth`, no automated TC | Uncovered contract (see §8) |
| Existing guard test | — | `BabyCareControllerAuthorizationContractTest` split: GrowthMeasurementController stays MOTHER/FAMILY; `getGrowthChart` expects MOTHER/FAMILY/EXPERT | Required by approved ADR-SBG-002 |
| Out-of-scope failures | — | Backend `HealthBoundaryVocabularyTest` (concurrent edit in `baby_daily_log_detail_screen.dart`); mobile checklist card + expert directory tests | Not caused by this feature; left untouched |

---

## 6. Entry, Exit, and Suspension Criteria

### 6.1 Entry Criteria

- [x] Paired TDS is `Draft` or `In Review` and all 17 sections are populated.
- [x] Requirement, API, state, authorization, data, and error oracles are explicit.
- [ ] Architecture/test-changing contradictions in Section 2 are resolved (OPEN-01/OPEN-02 decided or deferred).
- [x] Applicable test levels and environments are available.
- [x] Synthetic fixtures and provider fakes are identified.
- [x] Schema/migration requirements are known or explicitly `Open` (none required).

### 6.2 Exit Criteria

- [x] Every in-scope requirement maps to at least one Test Condition and TC.
- [ ] Every TDS field/state/error/auth/event/side-effect contract has coverage.
- [x] All Critical and High TCs have current execution evidence.
- [ ] All applicable automated suites pass with recorded commands/counts.
- [ ] No Critical/High unresolved defect remains.
- [x] No secrets or real protected data exist in fixtures, logs, or snapshots.
- [ ] Rollback checks are executable and reviewed.
- [x] Red Gate passed for all non-guard TCs; Props Isolation factories used.
- [ ] Reviewer and approver sign-off are recorded.

### 6.3 Suspension and Resumption Criteria

| Trigger | Suspend when | Resume when |
| --- | --- | --- |
| Oracle ambiguity | DPO rejects UD-05 broad access (OPEN-02) | TDS/Test-Spec updated with new rule |
| Environment | `.env` contamination makes backend tests pass falsely | `.env` moved aside and rerun |
| Security/privacy | Test risks real credentials or protected data | Approved synthetic substitute exists |
| Destructive behavior | Test may corrupt shared state or migration history | Recoverable isolated procedure is approved |
| Provider instability | Not applicable — no provider | — |

---

## 7. Rollback Plan

### 7.1 Test Artifact Rollback

| Artifact | Safe rollback action | Verification |
| --- | --- | --- |
| New/changed tests | Revert the focused test change on the working branch | Targeted baseline suite returns to prior result |
| Fixtures/factories | Restore prior factory contract; remove only feature-owned synthetic data | Unrelated suites remain green |
| Test configuration | Restore versioned config; never delete shared secrets/state | Supported smoke command succeeds |
| Schema fixture/migration | Not applicable — no migration | — |

Commands: `git revert <feature-commit>` on `HuyND`; no Flyway action.

### 7.2 Production-Change Rollback Verification

| Rollback risk | Verification case | Oracle Source | Status |
| --- | --- | --- | --- |
| Reverting the backend grant leaves expert cards crashing | SBG-TC-011 / SBG-TC-016 (403 → fallback) | TDS §12.2 | `🔴 Not written` |
| Reverting clients leaves tagged bodies | Existing plain-text rendering of unknown bodies (no new TC) | TDS §11.3 | `🔴 Not written` |

Never recommend editing or deleting applied Flyway history in a shared environment.

---

## 8. CASE 2.0 Anti-Pattern Detection

| Anti-pattern | Detection question | Required evidence | Result |
| --- | --- | --- | --- |
| Hallucinated oracle | Does any expected value lack an exact source? | All assertion rows cite `SRC/BASIS` | Pass — assertions cite BASIS/SRC; deviations in §5.4 |
| Generic test matrix | Could the same cases be pasted into an unrelated UC unchanged? | Feature-specific states, fields, roles, errors, paths | Pass — feature-specific fields, roles, texts |
| False green claim | Is any test marked passing without current execution evidence? | Command, timestamp, counts, failure/pass artifact | Pass — evidence in §5.3; out-of-scope failures reported |
| Hidden contradiction | Was code chosen over requirement without a decision? | Section 2 ledger | Pass — §2 ledger |
| Missing Props Isolation | Do tests construct large shared objects inline? | Applicable `makeXxx()` factories | Pass — `baby_growth_test_factory.dart`, `makeShareData`/`makeGrowthPoints`/`makeRecord` |
| Over-mocking | Does the test bypass the contract or state being verified? | Mock-boundary rationale | Pass — loaders/API mocked only at network boundary |
| Brittle implementation assertion | Does the test assert private call order instead of observable behavior? | Behavior-focused oracle | Pass — visible text/order/role assertions |
| Cross-test pollution | Can order, clock, DB, provider, or global state change the result? | TDS-05 isolation controls | Pass — RTL cleanup added after a detected DOM leak |
| Unsafe data | Are real health/location/identity/conversation values used? | Synthetic fixture audit | Pass — synthetic babies and measurements |
| Wrong-layer test | Is a UI/E2E test generated for an absent consumer? | Applicability matrix | Pass — matches applicability matrix |
| Uncovered contract | Is any field/state/error/auth/event missing a TC? | Traceability comparison | Fail — ERR-02 send-failure snackbar has no TC; Mobile growth-chart JSON mapping (`getGrowthChartMeasurements`) has no contract test |
| AI safety bypass | Can model output directly mutate clinical/safety state without deterministic policy? | Not applicable — no AI component | Not applicable |

### 8.1 Final Self-Check

- [x] Exactly 8 top-level sections are present.
- [x] All metadata and reference fields are populated or explicitly `Open`/`Not applicable`.
- [x] Each expected result cites an oracle source.
- [x] Each applicable TC has stable ID, severity, condition, preconditions, AAA,
      persistence/audit/event/provider/UI assertions, failure signature, intended path,
      cleanup, and initial status.
- [x] The applicability matrix prevents irrelevant boilerplate tests.
- [x] The Red Gate is usable without claiming unexecuted evidence.
- [x] Contradictions and research gaps remain visible.
- [x] Paired TDS and this Test-Spec are bidirectionally traceable.
