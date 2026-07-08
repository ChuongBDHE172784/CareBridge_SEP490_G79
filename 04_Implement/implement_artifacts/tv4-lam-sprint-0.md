# Sprint 0 — TV4 Lâm (Contract Freeze & Vertical-Slice Skeleton)

**Owner:** Lâm (TV4)
**Sprint Goal:** Isolate modules, publish stable contracts, seed mock data. This sprint does **not** attempt to finish every screen — it prepares the foundation so other team members can build independently.

**Domains:** `expert`, `expertverification`, `expertavailability`, `map`, `location`, `nearbycare`
**Integration contracts OWNED (publish):** `ExpertBadgeReadPort`, `RouteProvider`, `EmergencyMapHandoff`
**Integration contracts CONSUMED:** `TriageResultPort` (from TV5), `NotificationCommand` / `AuditEvent` (from TV1)

---







## 1. Domain Boundaries (Non-Negotiable)

| TV4 OWNS — Do NOT let another member edit | TV4 MUST NOT touch without coordination |
|---|---|
| `expert` package: profile, availability, contribution | Community post/answer storage (`TV3` domain) |
| `expertverification` package: documents, review, status | User/permission internals (`TV1` domain) |
| `map`, `location`, `nearbycare` packages: facility, route, emergency map | AI risk rules and triage logic (`TV5` domain) |
| `ExpertBadgeReadPort` (published for TV3) | IMU detection and safety-event logic (`TV5` domain) |
| `RouteProvider`, `EmergencyMapHandoff` (published for TV5) | Maternal/baby business tables (`TV2` domain) |

---







## 2. UCs Owned by TV4 (Full List)

| # | UC | Feature | Description | Sprint |
|---|---|---|---|---|
| 1 | UC-60 | MF-05 Submit Expert Profile | Create an expert application profile | Sprint 0 + 1 |
| 2 | UC-62 | MF-05 Submit/Replace Verification Documents | Upload credentials for verification | Sprint 0 + 1 |
| 3 | UC-65 | MF-05 Browse Verified Expert Directory | Public browse of verified experts | Sprint 0 + 1 |
| 4 | UC-66 | MF-05 View Verified Expert Profile | Public profile detail view | Sprint 0 + 1 |
| 5 | UC-67 | MF-05 View Expert Question Queue | Community questions matched to expert specialty | Sprint 0 + 1 |
| 6 | UC-70 | MF-05 Review Expert Verification Submission | Admin approve/request supplement/reject | Sprint 0 + 1 |
| 7 | UC-61 | MF-05 Update Expert Profile | Update approved professional fields | Sprint 2 |
| 8 | UC-63 | MF-05 View Verification Status and Renew | Show expiry, allow renewal | Sprint 2 |
| 9 | UC-64 | MF-05 Configure Expert Availability & Scope | Availability, support methods, service area | Sprint 2 |
| 10 | UC-68 | MF-05 Post Verified Expert Answer | Post answer via TV3 community API (expert badge included) | Sprint 1 |
| 11 | UC-69 | MF-05 View Contribution Points & Badges | Community activity recognition | Sprint 2 |
| 12 | UC-71 | MF-05 Restrict/Suspend/Reinstate Expert Trust Status | Admin enforcement on verified experts | Sprint 2 |
| 13 | UC-77 | MF-07 Open Emergency Map | Shared emergency map with location permission + non-dispatch disclaimer | Sprint 2 |
| 14 | UC-78 | MF-07 Find Nearby Care Facilities | Find facilities around approved location | Sprint 2 |
| 15 | UC-79 | MF-07 View Route, ETA and Quick Call/Navigate | Route/ETA display, quick action | Sprint 2 |
| 16 | UC-80 | MF-07 Share Time-Limited Location + Family Emergency Alert | Time-limited emergency alert with location | Sprint 2 |
| 17 | UC-81 | MF-07 Create/Cancel Nearby Support Request | Consent-based request visible to eligible experts | Sprint 2 |
| 18 | UC-82 | MF-07 Manage Expert Nearby Availability & Respond to Request | Expert opt-in, view request, accept/decline/stop | Sprint 2 |

**Total: 18 UCs. All P0. No P1 UC ownership.**

---







## 3. Delivery Phases

| Phase | Deadline | Focus |
|---|---|---|
| **Sprint 0** (this file) | T3 (today) | Package structure, contracts, migrations, mock data — modules compile together |
| **Sprint 1** | T6 | Demo Gate A: Expert registration → verification → directory → answer question |
| **Sprint 2** | T10 | Demo Gate B + P0 complete: AI safety → emergency map → route/ETA + remaining expert flows |
| **Stabilization** | T14 | Regression, hardening, test evidence, merge freeze |

---







## 4. Sprint 0 Tasks (T3 Delivery)

> **Sprint 0 exit condition:** All five TV4 modules compile independently and together. Integration contracts are published and other team members can consume them. One seeded verified expert exists for Demo Gate A testing.

---







## 4.1 Package Structure Setup

| # | Task | Related UC(s) | Acceptance Criteria |
|---|---|---|---|
| S0-1 | Create domain package boundaries with exact structure: | — | — |
| — | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/expert/` | — | Package exists; compiles cleanly |
| — | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/expert/controller/` | — | Empty controller package ready |
| — | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/expert/service/` | — | Empty service package ready |
| — | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/expert/repository/` | — | Empty repository package ready |
| — | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/expert/entity/` | — | Empty entity package ready |
| — | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/expert/dto/` | — | Empty DTO package ready |
| — | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/expert/mapper/` | — | Empty mapper package ready |
| — | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/expert/policy/` | — | Empty policy package ready |
| — | Repeat same structure for `expertverification`, `expertavailability`, `map`, `location`, `nearbycare` | All | All 6 packages compile; `./mvnw compile` passes with zero errors |
| S0-2 | Follow backend architecture rules: controller → validation/request-response only; service → business logic; repository → persistence only; never expose JPA entities in API | — | Each package follows the layering rule from project architecture |

---







## 4.2 JPA Entities (Skeleton Only — No Business Logic)

| # | Task | Entity Fields | Related UC(s) |
|---|---|---|---|
| S0-3 | Create `ExpertProfile` entity | `id` (UUID), `userId` (UUID), `specialty`, `experienceYears`, `supportScope`, `publicBio`, `trustStatus` (enum), `createdAt`, `updatedAt` | UC-60, UC-61 |
| S0-4 | Create `VerificationDocument` entity | `id`, `expertProfileId`, `documentType`, `fileUrl`, `status` (PENDING/UPLOADED/REJECTED/APPROVED), `adminNote`, `uploadedAt`, `reviewedAt` | UC-62, UC-70 |
| S0-5 | Create `ExpertAvailability` entity | `id`, `expertProfileId`, `isAvailable`, `supportMethods`, `serviceArea`, `nearbyOptIn` (boolean), `effectiveFrom`, `effectiveTo`, `createdAt`, `updatedAt` | UC-64, UC-82 |
| S0-6 | Create `ContributionRecord` entity | `id`, `expertProfileId`, `points`, `badge`, `activityType`, `relatedAnswerId`, `recordedAt` | UC-69, UC-68 |
| S0-7 | Create `CareFacility` entity | `id`, `name`, `type`, `address`, `lat`, `lng`, `phone`, `operatingHours`, `isEmergencyCapable`, `serviceTags` | UC-78, UC-79 |
| S0-8 | Create `EmergencyMapHandoff` entity (or equivalent) | `id`, `motherId`, `triageHandoffId` (from TV5), `riskLevel`, `lat`, `lng`, `status`, `createdAt`, `resolvedAt` | UC-77, UC-74 (extension) |
| S0-9 | Create `NearbySupportRequest` entity | `id`, `motherId`, `status` (OPEN/ACCEPTED/CANCELLED/COMPLETED), `supportType`, `description`, `lat`, `lng`, `consentToken`, `createdAt`, `respondedAt`, `completedAt` | UC-81, UC-82 |
| S0-10 | Create `NearbySupportResponse` entity | `id`, `requestId`, `expertProfileId`, `action` (ACCEPT/DECLINE/STOP), `respondedAt`, `note` | UC-82 |

---







## 4.3 Repository Interfaces

| # | Task | Related UC(s) |
|---|---|---|
| S0-11 | `ExpertProfileRepository` — JpaRepository + custom query methods (findVerified, findByTrustStatus, findBySpecialty) | All expert UCs |
| S0-12 | `VerificationDocumentRepository` — JpaRepository + custom queries (findPendingByExpert, findLatestByExpert) | UC-62, UC-70 |
| S0-13 | `ExpertAvailabilityRepository` — JpaRepository + custom queries (findAvailableBySpecialty, findActive) | UC-64, UC-82 |
| S0-14 | `ContributionRecordRepository` — JpaRepository | UC-69, UC-68 |
| S0-15 | `CareFacilityRepository` — JpaRepository + spatial query method (findNearby) | UC-78, UC-79 |
| S0-16 | `EmergencyMapHandoffRepository` — JpaRepository | UC-77, UC-74 |
| S0-17 | `NearbySupportRequestRepository` — JpaRepository + custom queries (findOpenNearby, findByMother, findByExpert) | UC-81, UC-82 |
| S0-18 | `NearbySupportResponseRepository` — JpaRepository | UC-82 |

**Rule:** Repository layer = persistence/query ONLY. No business decisions, no authorization logic.

---







## 4.4 Publish Integration Contracts

These are the contracts OTHER team members depend on. They must be committed and communicated **before** anyone starts Sprint 1.

| # | Contract | Type | Purpose | Consumer | Related UC(s) |
|---|---|---|---|---|---|
| S0-19 | `ExpertBadgeReadPort` | **Java interface** (published in `expertverification` or `expert` package) | TV4 supplies verification/badge state; TV3 calls it to display verified badge on community answers | TV3 (Huy) — community answer badge display | UC-68, UC-65, UC-66 |
| S0-20 | `RouteProvider` | **Java interface** (published in `map` package) | Accepts origin/destination coordinates; returns route + ETA DTO | TV4 internal + TV5 (via `EmergencyMapHandoff`) | UC-78, UC-79 |
| S0-21 | `EmergencyMapHandoff` | **Java interface** (published in `nearbycare` package) | Accepts triage handoff payload from TV5 with minimum-permitted context; opens emergency map with red-risk context | TV5 (Chương) — triage-to-map handoff | UC-74 (extension), UC-77 |

**For each contract, provide:**
- Interface file with method signatures
- A brief README or comment block explaining input types, output types, and who calls what
- Notify the consuming team member **in person** (or via project chat) when committed

---







## 4.5 Create Empty Module Shells for TV5 (Do NOT Implement)

TV5 owns `triage`, `airiskrule`, `safety`, `imu`, `safetyevent` packages. You only need to create empty shells so the entire project compiles together.

| # | Task | Package | Notes |
|---|---|---|---|
| S0-22 | Create empty skeleton packages under TV5 path | `triage`, `airiskrule`, `safety`, `imu`, `safetyevent` | Only package structure — zero logic, zero entity, zero controller |
| S0-23 | Create placeholder `TriageResultPort` if not yet created by TV5 (confirm with TV5/Chương) | `triage` port | If TV5 hasn't created it yet, create a minimal interface with TODO; else skip |

---







## 4.6 Flyway Database Migrations

| # | Task | Tables Created | Related UC(s) | Rules |
|---|---|---|---|---|
| S0-24 | Create timestamped Flyway migration: `V{timestamp}__init_expert_tables.sql` | `expert_profiles`, `verification_documents`, `expert_availability`, `contribution_records` | UC-60…UC-71 | - NEW timestamped file — never amend TV1/TV2/TV3 migration files<br>- Follow existing Flyway naming convention |
| S0-25 | Create timestamped Flyway migration: `V{timestamp}__init_map_nearby_tables.sql` | `care_facilities`, `emergency_map_handoffs`, `nearby_support_requests`, `nearby_support_responses` | UC-77…UC-82 | - Same rules: new file, no amendments<br>- Include enum constraints for `trustStatus`, `documentStatus`, `riskLevel`, `supportRequestStatus` |
| S0-26 | Verify both migrations apply cleanly: drop + recreate local PostgreSQL, run all migrations, confirm all 8 tables exist | — | — | `./mvnw spring-boot:run` with `SPRING_FLYWAY_CLEAN_ON_VALIDATION=true` (local only) then clean |

---







## 4.7 Seed Mock Data (Deterministic — For Demo & Team Testing)

| # | Task | Data | Related UC(s) | Acceptance Criteria |
|---|---|---|---|---|
| S0-27 | Seed one `VERIFIED` expert in `expert_profiles` for Demo Gate A | Name: "BS. Nguyễn Thị Minh", Specialty: "Sản phụ khoa", Trust status: `VERIFIED`, Availability: `AVAILABLE`, Nearby opt-in: `true` | UC-65, UC-70 | Running app shows 1 verified expert in directory API |
| S0-28 | Seed 3–5 `CareFacility` records (mock/TrackAsia-ready) | Mock hospitals, clinics in Hà Nội with real-ish coordinates | UC-78, UC-79 | Facility API returns seeded list with coordinates |
| S0-29 | Seed 2 `CareFacility` records marked `is_emergency_capable = true` | For emergency map demo | UC-77, UC-78 | Only these 2 appear when emergency map is opened with red-risk context |
| S0-30 | Create SQL data-loader seed script (`data.sql` or dedicated seeder utility) | All seed data | All | Seed script is idempotent — can be re-run without errors |

---







## 4.8 Internal Compile & Integration Check

| # | Task | Command | Acceptance Criteria |
|---|---|---|---|
| S0-31 | Run `./mvnw compile` with all TV4 modules + TV1/TV2/TV3 modules | `./mvnw compile` | Build passes with **zero errors** |
| S0-32 | Run `./mvnw test` to confirm no test failures from other modules | `./mvnw test` | All green (or only pre-existing failures, not introduced by TV4) |
| S0-33 | Confirm `ExpertBadgeReadPort` consumer compiles: TV3 team member (Huy) pulls your contract and confirms it compiles in their module | — | Huy confirms via chat/slack: "contract compiles, I have what I need" |
| S0-34 | Confirm `RouteProvider` + `EmergencyMapHandoff` compile at their published locations | — | No compile errors from consuming modules |

---







## 5. Sprint 0 Checklist — Definition of Done

Before claiming Sprint 0 complete, verify ALL items below:

- [ ] **S0-1 through S0-2:** All 6 package structures created with correct layering
- [ ] **S0-3 through S0-10:** All 8 JPA entities created with correct fields
- [ ] **S0-11 through S0-18:** All 8 repository interfaces created with no business logic
- [ ] **S0-19 through S0-21:** All 3 integration contracts published and declared publicly
- [ ] **S0-22 through S0-23:** TV5 empty shells created (or confirmed already done by TV5)
- [ ] **S0-24 through S0-26:** Flyway migrations created, clean apply verified
- [ ] **S0-27 through S0-30:** Mock data seeded, idempotent script ready
- [ ] **S0-31 through S0-34:** Full project compiles; all consuming modules confirmed
- [ ] **Git:** All changes committed to `LamVH1` with conventional commit messages; PR opened (or ready to open) to `dev`

---







## 6. What TV4 Does NOT Do in Sprint 0 (Important Boundaries)

| ❌ NOT Owned by TV4 | ✅ Owner | Notes |
|---|---|---|
| OTP, login, auth scaffolding | TV1 (Phương) | TV1 provides `AuthContext` — you consume it, don't build it |
| User table, role, permission | TV1 (Phương) | Do NOT create or modify `user`, `role`, `permission` tables |
| Community post/answer table | TV3 (Huy) | TV4 posts answers via TV3's API — do NOT create an `answers` table |
| `MotherJourneyReadPort`, `BabyReadPort` | TV2 (Bách) | TV2 owns mother/baby data — read via ports if needed |
| Map provider implementation | TV4 owns, but **use mock for Sprint 0** | Replace with TrackAsia in Sprint 2, mock is fine for now |
| AI triage rules, IMU logic | TV5 (Chương) | TV4 only renders the map result — TV5 owns the triage logic |
| Notification delivery, audit logging | TV1 (Phương) | TV4 emits events; TV1 delivers. Don't build notification infrastructure. |
| Consultation, payment, realtime chat, commission | **Excluded — V2 only** | Do NOT create these packages |

---







## 7. Commit Message Convention (Use for Every Commit)

```
feat(tv4): add expert profile entity and repository (UC-60, UC-61)
feat(tv4): publish ExpertBadgeReadPort contract for TV3 consumption
feat(tv4): create Flyway migration for expert and map tables (UC-60..UC-82)
chore(tv4): seed mock verified expert and care facilities for Sprint 0
```

---







## 8. Next Steps After Sprint 0

| Priority | Task | Sprint |
|---|---|---|
| 1 | Notify TV1/TV2/TV3 that contracts are ready for consumption | Immediately after commit |
| 2 | Get TV1 Pull-Request merge for shared contracts before starting Sprint 1 | Week of Sprint 0 |
| 3 | Sprint 1: UC-60, UC-62 → Expert registration and document upload | Sprint 1 (T6) |
| 4 | Sprint 1: UC-70 → Admin expert verification review flow | Sprint 1 (T6) |
| 5 | Sprint 1: UC-65, UC-66, UC-67 → Directory, profile, question queue (API only) | Sprint 1 (T6) |
| 6 | Sprint 1: UC-68 → Post verified expert answer via TV3 API | Sprint 1 (T6) |

> **⚠️ Reminder:** TV4 owns `map`, `location`, `nearbycare` packages — TV5 only hands off a triage result or safety event to you. Map/location stays entirely inside TV4. Every schema change is a new timestamped Flyway migration. Never amend another member's committed migration.
