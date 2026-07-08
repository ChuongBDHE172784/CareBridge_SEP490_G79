# Sprint Tasks — TV4 Lâm (Verified Expert Network & Emergency Map)

**Owner:** Lâm (TV4)
**Domains:** `expert`, `expertverification`,    `expertavailability`, `map`, `location`, `nearbycare`
**Integration contracts owned:** `ExpertBadgeReadPort`, `RouteProvider`, `EmergencyMapHandoff`
**Integration contracts consumed:** `TriageResultPort` (from TV5), notification/audit events (via TV1)

---







## 1. Domain Boundaries (Must Not Violate)

| What TV4 owns | What TV4 must NOT touch without coordination |
|---|---|
| `expert`, `expertverification`, `expertavailability` packages | Community post/answer storage (TV3 domain) |
| `map`, `location`, `nearbycare` packages | User/permission internals (TV1 domain) |
| `ExpertBadgeReadPort` — published for TV3 consumption | AI risk rules and triage logic (TV5 domain) |
| `RouteProvider` and `EmergencyMapHandoff` — published for TV5/triage-to-map handoff | IMU detection and safety-event logic (TV5 domain) |









## 2. UC Ownership Summary

| Use Case | Feature | Sprint | Notes |
|---|---|---|---|
| UC-60 | MF-05 Submit Expert Profile | Sprint 1 | P0 — Demo Gate A |
| UC-61 | MF-05 Update Expert Profile | Sprint 2 | P0 |
| UC-62 | MF-05 Submit or Replace Verification Documents | Sprint 1 | P0 — Demo Gate A |
| UC-63 | MF-05 View Verification Status and Renew Submission | Sprint 2 | P0 |
| UC-64 | MF-05 Configure Expert Availability and Service Scope | Sprint 2 | P0 |
| UC-65 | MF-05 Browse Verified Expert Directory | Sprint 1 | P0 — Demo Gate A |
| UC-66 | MF-05 View Verified Expert Profile | Sprint 1 | P0 — Demo Gate A |
| UC-67 | MF-05 View Expert Question Queue | Sprint 1 | P0 — Demo Gate A |
| UC-68 | MF-05 Post Verified Expert Answer | Sprint 1 | P0 — Demo Gate A (via TV3 community API) |
| UC-69 | MF-05 View Contribution Points and Badges | Sprint 2 | P0 |
| UC-70 | MF-05 Review Expert Verification Submission | Sprint 1 | P0 — Demo Gate A |
| UC-71 | MF-05 Restrict, Suspend or Reinstate Expert Trust Status | Sprint 2 | P0 |
| UC-77 | MF-07 Open Emergency Map | Sprint 2 | P0 — Demo Gate B |
| UC-78 | MF-07 Find Nearby Care Facilities | Sprint 2 | P0 — Demo Gate B |
| UC-79 | MF-07 View Route, ETA and Quick Call or Navigate | Sprint 2 | P0 — Demo Gate B |
| UC-80 | MF-07 Share Time-Limited Location and Send Family Emergency Alert | Sprint 2 | P0 — Demo Gate B |
| UC-81 | MF-07 Create or Cancel Nearby Support Request | Sprint 2 | P0 — Demo Gate B |
| UC-82 | MF-07 Manage Expert Nearby Availability and Respond to Nearby Support Request | Sprint 2 | P0 — Demo Gate B |

**Total: 18 Use Cases (UC-60 → UC-71, UC-77 → UC-82)**
**All belong to P0 (MF-05 or MF-07). No P1 UC ownership for TV4.**

---







## 3. Sprint-by-Sprint Task Detail

---

#





# **Sprint 0 — Contract Freeze and Vertical-Slice Skeleton**

**Goal:** Isolate modules, publish stable contracts, seed mock data. No finished screens yet.

| # | Task | Related UC(s) | Output / Acceptance Criteria |
|---|---|---|---|
| S0-1 | Create domain package boundaries: `expert`, `expertverification`, `expertavailability`, `map`, `location`, `nearbycare` | — | All packages compile; no controller/entity leaks across packages |
| S0-2 | Define JPA entities for expert profile, verification document, availability config, facility, route, nearby-support request/response | UC-60, UC-62, UC-64, UC-78, UC-82 | Entities compile; Flyway migration `V001__init_expert_map` created (or appropriate timestamped version) |
| S0-3 | Create repository layer interfaces for each entity | — | All repositories compile; no business logic inside |
| S0-4 | Define and publish `ExpertBadgeReadPort` interface | UC-68, UC-65, UC-66 | Interface file committed; TV3 team notified to consume for answer badge display |
| S0-5 | Define and publish `RouteProvider` interface | UC-79, UC-78 | Interface accepts origin/destination; returns route + ETA DTO |
| S0-6 | Define and publish `EmergencyMapHandoff` interface | UC-74 (TV5), UC-77 | Interface accepts triage handoff payload with minimum-permitted context |
| S0-7 | Implement deterministic mock facility provider and seed one verified expert for Demo Gate A | UC-65, UC-70 | Running app shows 1 seeded verified expert in directory; map loads mock facilities |
| S0-8 | Create empty module shells for `triage` / `airiskrule` / `safety` / `imu` / `safetyevent` | — | **Do NOT implement** — TV5 owns these; create shells only so modules compile together |
| S0-9 | Create Flyway migration for expert + map tables | UC-60…UC-82 | Migration file is new (timestamped); does NOT amend any TV1/TV2/TV3 migration |
| S0-10 | Internal compile check: all 5 TV4 modules compile together with shared contracts | — | `./mvnw compile` passes |

**Sprint 0 exit:** All five TV4 modules compile; contracts published; mock data seeds successfully; other domains can consume `ExpertBadgeReadPort`.

---

#





# **Sprint 1 — Deliver Demo Gate A First**

**Goal:** Expert registration → verification approval → expert answers a community question → mother sees verified answer.

| # | Task | Related UC(s) | Output / Acceptance Criteria |
|---|---|---|---|
| S1-1 | Implement **UC-60** — Submit Expert Profile: API + service logic for creating an expert application profile with specialty, experience, support scope | UC-60 | POST `/api/experts/apply` creates profile; returns expert-application DTO; emits `NotificationCommand` for admin review |
| S1-2 | Implement **UC-62** — Submit or Replace Verification Documents: upload credentials, certificates, supporting evidence | UC-62 | POST `/api/experts/{id}/documents` handles mulitpart upload; stores via Protected Object Storage; emits `NotificationCommand` and `AuditEvent` |
| S1-3 | Implement **UC-70** — Review Expert Verification Submission (admin flow): approve, request supplementation, reject | UC-70 | PUT `/api/admin/experts/{id}/review` updates verification state; requires `SYSTEM_ADMIN` role; emits `AuditEvent` |
| S1-4 | Seed verification state machine: test user goes through `PENDING_REVIEW` → approved `VERIFIED` | UC-70, UC-60 | Pre-seeded `expert@carebridge.dev` has VERIFIED status for Demo Gate A |
| S1-5 | Implement **UC-65** — Browse Verified Expert Directory: public API with search/filter by specialty, availability, badge | UC-65 | GET `/api/experts/directory` returns only VERIFIED experts; embeds `ExpertBadgeReadPort` data |
| S1-6 | Implement **UC-66** — View Verified Expert Profile: public profile detail | UC-66 | GET `/api/experts/{id}` returns public profile DTO (no private fields) |
| S1-7 | Implement **UC-67** — View Expert Question Queue: matches community questions to expert specialty and support scope | UC-67 | GET `/api/experts/questions` returns open questions matching logged-in expert's declared specialties; calls `CommunityQuestionPort` (via TV3 contract / HTTP) |
| S1-8 | Implement **UC-68** — Post Verified Expert Answer: expert posts answer via **TV3 community API contract** (do NOT create a second answer table) | UC-68 | POST to TV3 `/api/community/answers` with `verifiedExpertId` and badge data; TV3 creates answer record; `ExpertBadgeReadPort` supplies verification state |
| S1-9 | Wire TV4 verification state → displayed badge on community answers | UC-68, UC-65, UC-66 | When expert trust status changes, downstream consumers (directory, answer badge, nearby availability) reflect the change |
| S1-10 | Unit tests for approval/rejection state machine | UC-70 | Cover all state transitions; mock repository; assert events emitted |
| S1-11 | Integration test: full expert registration → OTP (TV1) → profile → document upload → admin review → verified → view directory | UC-60, UC-62, UC-70, UC-65 | Test seeds data, calls API chain, asserts final `VERIFIED` status and directory visibility |

**Sprint 1 exit — Demo Gate A runnable:**
`Register → Verify OTP → Login as Mother → … → Login as Verified Expert → Post Verified Answer → Mother views verified answer.`

---

#





# **Sprint 2 — Deliver Demo Gate B and Finish the P0 Core**

**Goal:** AI safety support (red-risk → emergency map → nearby facility → route/ETA) + finish all remaining P0 expert flows.

##





## 2A. Expert Trust Completion (Parallel track)

| # | Task | Related UC(s) | Output / Acceptance Criteria |
|---|---|---|---|
| S2-1 | Implement **UC-61** — Update Expert Profile: approved professional fields and public service info | UC-61 | PUT `/api/experts/profile` updates allowed fields; triggers re-review if sensitive fields change |
| S2-2 | Implement **UC-63** — View Verification Status and Renew Submission: show expiry, required corrections, renewal flow | UC-63 | GET `/api/experts/verification-status` returns current state + expiry; POST `/api/experts/renew` starts renewal |
| S2-3 | Implement **UC-64** — Configure Expert Availability and Service Scope: availability, support methods, service area | UC-64 | PUT `/api/experts/availability` updates config; affects directory badge and nearby-support eligibility |
| S2-4 | Implement **UC-69** — View Contribution Points and Badges: community activity tracking | UC-69 | GET `/api/experts/{id}/contribution` returns points + badge list |
| S2-5 | Implement **UC-71** — Restrict, Suspend or Reinstate Expert Trust Status (admin): enforcement action | UC-71 | PUT `/api/admin/experts/{id}/trust` applies restriction/suspension/reinstatement; requires `SYSTEM_ADMIN`; emits `AuditEvent` — suspended experts cannot post answers or appear in directory |
| S2-6 | Implement UC-82 nearby-support sub-flow: expert receives request, accepts/declines/stops responding | UC-82 | PUT `/api/experts/nearby/request/{id}/respond`; availability check enforced |

##





## 2B. Emergency Map & Nearby Care (Demo Gate B)

| # | Task | Related UC(s) | Output / Acceptance Criteria |
|---|---|---|---|
| S2-7 | Implement **UC-77** — Open Emergency Map: opens map with location-permission explanation and non-dispatch disclaimer | UC-77 | GET `/api/nearbycare/map` returns map config with location permission required flag; user is informed before enabling location |
| S2-8 | Implement **UC-78** — Find Nearby Care Facilities: query facilities around approved location or selected area | UC-78 | GET `/api/nearbycare/facilities?lat=&lng=&radius=` returns mock/TrackAsia facilities; `RouteProvider` provides fallback |
| S2-9 | Implement **UC-79** — View Route, ETA and Quick Call or Navigate: show route, enable quick call or navigation | UC-79 | GET `/api/nearbycare/route?from=&to=` returns route + ETA DTO; quick-call opens device dialer; navigation opens map app |
| S2-10 | Implement **UC-80** — Share Time-Limited Location and Send Family Emergency Alert (via TV1 notification delivery) | UC-80 | POST `/api/nearbycare/emergency-alert` validates active consent; minimally-scoped location; time-limited; TV1 delivers notification via `NotificationCommand` |
| S2-11 | Implement **UC-81** — Create or Cancel Nearby Support Request: mother creates consented request visible to eligible experts | UC-81 | POST `/api/nearbycare/support-request` creates request (consent + purpose validated); PUT cancel revokes visibility |
| S2-12 | Wire `TriageResultPort` consumer: TV4 reads handoff payload from TV5 and opens emergency map with safe context | UC-74 (extension of UC-73), UC-77 | TV5 creates `triageHandoffId`; TV4 reads minimum permitted context; map opens with red-risk context — NOT a diagnosis |
| S2-13 | Wire `EmergencyMapHandoff` producer: TV4 exposes route/facility data that TV5 triage result can trigger | UC-74, UC-77 | `EmergencyMapHandoff` contract implemented and callable by TV5 |
| S2-14 | Replace deterministic mock facility provider with configurable track-asia/real provider (mock as fallback) | UC-78, UC-79 | Provider is pluggable; mock data is the default for demo; real config is optional based on environment |

**Sprint 2 exit — Demo Gate B and P0 completion:**
`Mother logs symptoms → AI triage result (red-risk) → emergency map opens → nearby facility appears → route/ETA and quick action work → family location alert with consent.`

**All UC-60 to UC-82 are now implemented or have a controlled-mock fallback.**

---

#





# **Sprint 3 — P0 Regression and Map/Expert Hardening**

**Goal:** No new P1 feature scope for TV4. Harden existing flows, replace stable mocks, prepare contract tests.

| # | Task | Related UC(s) | Output / Acceptance Criteria |
|---|---|---|---|
| S3-1 | Harden expert verification flow: edge cases (expired cert, duplicate application, concurrent reviews) | UC-62, UC-70 | State machine handles all transitions; concurrent-review protection; audit trail complete |
| S3-2 | Harden expert directory: filter/sort/pagination resilience; empty-state handling; no verified expert fallback message | UC-65 | Directory handles 0 experts gracefully; all filter combinations tested |
| S3-3 | Harden answer posting flow: suspended expert cannot post; badge updates atomically with answer | UC-68, UC-71 | Suspended expert receives clear error; badge revocation is immediate |
| S3-4 | Harden availability + nearby-support flow: stale availability, time-window expiry, concurrent request handling | UC-64, UC-82 | Expert availability expires correctly; stale requests are flagged |
| S3-5 | Harden emergency map: location-permission denied fallback, no-facility-found fallback, offline/error states | UC-77, UC-78 | Map shows helpful message instead of blank screen; no-dispatch disclaimer always visible |
| S3-6 | Harden route/ETA: missing provider fallback, network-error retry, route-unavailable message | UC-79 | Graceful degradation to mock route data when provider fails |
| S3-7 | Prepare contract tests for triage-to-map handoff (TV5 → TV4) | UC-74 → UC-77 | Contract test suite covers all `TriageResultPort` → `EmergencyMapHandoff` scenarios |
| S3-8 | Prepare contract tests for safety-to-map handoff (TV5 safety event → TV4 map/location) | UC-80 (extension), UC-77 | Contract test suite covers safety-event → map handoff with consent validation |
| S3-9 | Replace map/location mocks with production provider configuration (TrackAsia) where safe | UC-78, UC-79 | Provider swap is behind feature flag or env var; mock remains fallback |
| S3-10 | Regression run: Demo Gate A script + Demo Gate B script — both pass without provider dependency | All P0 UCs | Two end-to-end demo scripts verified passing |

**Sprint 3 exit:** Health records/reminders/content (TV2/TV3) demo independently; TV4 P0 demo flows remain regression-safe.

---

#





# **Sprint 4 — No New P1 Feature Scope (Cross-Domain Validation)**

**Goal:** TV4 has **zero P1 UC ownership**. This sprint is for validating location-expiry, expert trust-status consistency, and regression safety across all TV4 P0 flows.

| # | Task | Related UC(s) | Output / Acceptance Criteria |
|---|---|---|---|
| S4-1 | Validate location-expiry: emergency map location data expires correctly after configured time window | UC-77, UC-80 | Location data is inaccessible post-expiry; UI shows expiry message |
| S4-2 | Validate location-accuracy fallback: weak GPS → degraded map experience with user notice | UC-77, UC-78 | Accuracy level surfaced to user; facility search radius adjusts |
| S4-3 | Validate route/ETA fallback matrix: every route/provider failure mode has a documented fallback | UC-79 | Fallback matrix documented; no route-unavailable state crashes the map screen |
| S4-3 | Validate no-dispatch disclaimer is always visible and user-confirmed before action | UC-77, UC-79, UC-80 | Disclaimer shown on every emergency map entry point |
| S4-4 | Validate nearby-support request scope: only eligible verified experts see requests | UC-81, UC-82 | Non-eligible users receive empty queue; eligible list updates when new requests arrive |
| S4-5 | Validate expert trust-status drives consistent behavior across all TV4 surfaces | UC-68, UC-65, UC-66, UC-64, UC-82 | Suspended/restricted expert: cannot post answers, not in directory, badge hidden, cannot accept nearby requests |
| S4-6 | Full regression: run all 4 final demo scripts with TV4 active | All P0 UCs | Gate A + Gate B + care continuity + smart safety scripts all pass |

**Sprint 4 exit:** All **UC-60 to UC-82** are implemented or have a controlled-mock fallback. Safety and privacy boundaries are preserved.

---

#





# **Sprint 5 — Stabilization, Test Evidence and Merge Freeze**

**Goal:** No new feature scope. Fix P0/P1 defects, integration gaps, accessibility issues, and deliver test evidence.

| # | Task | Related UC(s) | Output / Acceptance Criteria |
|---|---|---|---|
| S5-1 | Validate all 18 TV4 UCs have test evidence (unit tests for business rules + integration tests) | UC-60…UC-71, UC-77…UC-82 | Each UC has ≥1 unit test + ≥1 integration test; coverage report generated |
| S5-2 | Fix only P0/P1 defects found in regression — no new features | All | Defect tracker updated; each fix has test evidence |
| S5-3 | Accessibility/usability defects: map screen, expert directory, answer posting | UC-65, UC-66, UC-68, UC-77, UC-79 | Screen-reader labels, contrast, touch-target sizes verified |
| S5-4 | Data/security review: no private fields leaked in public expert profile or directory DTOs | UC-65, UC-66 | DTO audit passed; private fields not serialized in public API responses |
| S5-5 | Run backend build + tests | All | `./mvnw clean test` passes in TV4 modules |
| S5-6 | Verify provider fallback documentation | UC-78, UC-79 | `.env.example` updated with map provider config; fallback behavior documented |
| S5-7 | Run 4 end-to-end demo scripts without live-provider dependency | All (Gate A + Gate B) | Scripts: (1) Community Care Core, (2) AI Safety Support, (3) Care Continuity (observe, not own), (4) Smart Safety (observe, not own) |

---







## 4. Integration Handoff Reference

| Scenario | TV4 Role | Partner | Contract / Mechanism |
|---|---|---|---|
| Expert answers a community question | TV4 supplies `verifiedExpertId` and badge state | TV3 creates answer record | TV4 publishes `ExpertBadgeReadPort`; TV3 calls it |
| Red-risk AI result opens emergency support | TV4 reads handoff with safe context | TV5 creates `triageHandoffId` | TV4 implements `EmergencyMapHandoff` consumer |
| Smart safety event sends family emergency alert | TV4 handles location/map after consent | TV1 delivers notification | TV4 exposes location API; TV1 sends notifications |
| Expert trust restriction → community badge + nearby availability all update | TV4 owns all 3 surfaces | TV3 (badge on answer), TV2 (health sharing permission check) | Single trust-status change event cascades to all consumers |
| Nearby support request → expert responds | TV4 persists request, manages matching | TV5 (safety event triggers request) | TV5 writes safety event; TV4 reads and creates support request |

---







## 5. Definition of Done Per UC

A use case is not "done" until it has:

1. An authorized API/service flow owned by TV4 (`expert` or `map` domain).
2. A working mobile/web screen or documented screen trigger.
3. Validation and error states (denied/expired consent, failed verification, no-facility, no-route).
4. Required `NotificationCommand` / `AuditEvent` through TV1 contracts where applicable.
5. Unit tests for core business rules (trust status transitions, consent validation, location expiry).
6. A small demo script or test evidence showing the use case works with seeded data.
7. No direct repository/entity dependency on TV1/TV2/TV3 internal tables (use read ports only).

---







## 6. Quick-Reference: Sprint → UC Mapping

```
Sprint 0 (Skeleton):     UC-60, UC-62, UC-65, UC-66, UC-67, UC-70 (contracts + mocks)
Sprint 1 (Gate A):       UC-60, UC-62, UC-65, UC-66, UC-67, UC-68, UC-70
Sprint 2 (Gate B):       UC-61, UC-63, UC-64, UC-69, UC-71, UC-77, UC-78, UC-79, UC-80, UC-81, UC-82
Sprint 3 (Hardening):    (no new UC scope — regression + contract tests for all above)
Sprint 4 (Validation):   (no new UC scope — cross-cutting regression only)
Sprint 5 (Stabilize):    (test evidence only — all 18 UCs)
```

---

> **Reminder:** All schema changes must be new timestamped Flyway migrations. Never amend another member's migration. Every PR states affected UC IDs, migration file, API contract impact and test evidence.
