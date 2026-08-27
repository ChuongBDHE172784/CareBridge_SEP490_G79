# CareBridge Integration Test Execution Report

Execution-based verification of the 60 Suggested Integration Test Cases defined in
`INTEGRATION_TEST_ANALYSIS.md`. Every status in this report comes from a command that was actually
run against the current working tree during this task. `TEST_STATUS_2026-08-09.md` was read as
historical baseline only; none of its statuses were copied forward.

---

## 1. Execution Metadata

| Item | Value |
| --- | --- |
| Report generated | 2026-08-09, execution completed at approximately 10:55 Asia/Saigon |
| Git branch | `ChuongBD` |
| Commit SHA | `a92f502a45d2844ae91d1099ac2685bc05c91130` |
| Working tree | **Dirty** — 47 tracked status entries (including 2 staged and 3 Flutter-generated stat/line-ending-only entries) plus untracked files; full classification in §10 |
| Java | OpenJDK 21.0.10 LTS (Temurin 21.0.10+7) |
| Maven | Apache Maven 3.9.16 (via `./mvnw`) |
| Node | v22.22.3 |
| npm | 10.9.8 |
| Flutter | 3.38.5 stable (rev f6ff1529fd) |
| Python | 3.14.6 (`05_Development/CareBridgeAITriageService/.venv`) |
| Docker | Server 29.2.1 — **daemon reachable**, Testcontainers usable |
| Playwright | 1.61.1, chromium-1228 + msedge channel installed |
| firebase-tools | 15.11.0 available via `npx` |
| Flutter devices | Windows (desktop), Chrome (web), Edge (web) — **no Android/iOS emulator** |

### Why the dirty tree matters

The working tree carries changes made **before** this task, most importantly regenerated
`.sha256` digest sidecars for the AI Triage rule registry. On a clean checkout of `a92f502a` those
digests do not match their artifacts, and the Python suite fails with 85 failures + 50 errors while
four Java triage classes error out. All results below were measured **with** those pre-existing
changes in place. This task modified no production source (§10).

---

## 2. Full Suite Results

### Backend — `.\mvnw.cmd test`

| Metric | Value |
| --- | ---: |
| Tests run | 4158 |
| Failures | 13 |
| Errors | 15 |
| Skipped | 30 |
| Build result | `BUILD FAILURE` |

Fresh aggregation used only Surefire XML files written after the full-suite start time; stale XML
from renamed/deleted classes was excluded. Result: 4,100 passed, 13 failed, 15 errors and 30 skipped
across 672 report files. The 28 problems are concentrated in database, migration, checklist, journey,
chat, postpartum, health-observation and emergency-triage tests.

A targeted historical-failure rerun exercised eight affected classes:

| Metric | Value |
| --- | ---: |
| Tests run | 33 |
| Passed | 23 |
| Failures | 6 |
| Errors | 4 |
| Skipped | 0 |

This rerun reproduced the relevant current failures. A separate Round 3 rerun of the five cases
previously labelled `Existing` ran 10 tests with 0 failures/errors/skips. The new exact
ITS-SEC-002 verification then passed 1/1 independently.

### Flutter — `flutter test`

| Metric | Value |
| --- | ---: |
| Tests passed | 939 |
| Tests failed | 0 |
| Exit code | 0 |

`flutter test -d windows integration_test/…` cannot build: `firebase_cpp_sdk_windows/CMakeLists.txt`
declares `cmake_minimum_required` below 3.5, which the installed CMake no longer supports
(`Compatibility with CMake < 3.5 has been removed from CMake`). `flutter test -d chrome
integration_test/…` returns `Web devices are not supported for integration tests yet`. No Android or
iOS emulator is connected. **All three Flutter integration tests are therefore unexecutable here.**

### Web — Vitest and Playwright

| Suite | Result |
| --- | --- |
| `npx vitest run` | **137 passed / 137**, 31 files, exit 0 |
| `npx tsc -b --noEmit` | clean |
| `npx eslint .` | clean |
| `npm run test:e2e` (5 specs) | **2 passed, 4 failed, 3 skipped** of 9 tests, exit 1 |

Two Playwright UI failures are in `e2e/federated-login.spec.ts`: the spec asserts
`getByRole('button', { name: /continue with google/i })` and `/continue with phone/i` on `/login`,
and neither exists. Grepping `src/features/auth/` finds `Sign up with Google` only in
`FederatedRegisterPage.tsx`; `LoginPage.tsx` renders an email/phone + password form with no
federated control. Federated **registration** is implemented on the web; federated **login** is not.

The other two failures are in `e2e/federated-emulator.spec.ts`: phone login timed out while the
stored access token remained null, and the Google flow closed while selecting `Add new account`.
All three `mf03-hub.spec.ts` tests were intentionally skipped because `MF03_E2E_ENABLED` was false.
The ordinary Playwright command did not start a CareBridge API or Firebase emulator. The dedicated
runner was not used because it imports values from the backend `.env`; using it as-is would violate
the requirement not to use real production credentials.

### AI Triage — `pytest`

| Metric | Value |
| --- | ---: |
| Tests passed | 1013 |
| Tests failed | 0 |
| Exit code | 0 |

---

## 3. Previously Failing Tests — Fresh Verification

Every failure recorded in `TEST_STATUS_2026-08-09.md` was covered by the fresh full suite. The
aggregate failure/error counts remain 13 failures + 15 errors; targeted reruns confirmed the key
journey, postpartum, health, emergency, chat, checklist and canonical-schema failures. Error text
below records the current behavior rather than assuming the historical result.

| Test | Previous Status | Current Status | Current Error if any |
| --- | --- | --- | --- |
| `JourneyCanonicalLifecycleIntegrationTest` | Failed (2F/1E) | **Still fails (2F/1E)** | `FlywayException: No migration with a target version 20260727010000`; expected unique-index root cause is pre-empted by the missing migration; transition-history assertion expects PL/pgSQL line 3 but current error reports line 11 |
| `PostpartumLogPostgresIntegrationTest` | Failed (1E) | **Still fails (1E)** | `NullPointerException: Cannot invoke "java.lang.Short.shortValue()"` |
| `HealthObservationRepositoryIntegrationTest` | Failed (1E) | **Still fails (1E)** | `UnsupportedOperationException` |
| `EmergencyTriageLinkPostgresIntegrationTest` | Failed (1E) | **Still fails (1E)** | `null value in column "alert_successful_recipient_count" of relation "safety_events" violates not-null constraint` |
| `DirectChatIntegrationTest` | Failed (1F) | **Still fails (1F)** | `Status expected:<201> but was:<404>` |
| `Postgresql18CanonicalSchemaIntegrationTest` | Failed (1F) | **Still fails (1F)** | `expected: 57 but was: 63` (canonical base-table count) |
| `ChecklistTemplateMigrationTest` | Failed (1F) | **Still fails (1F)** | migration checksum `CA629ACE…` vs `29D413A5…` |
| `ChecklistRetentionDeploymentRunnerContractTest` | Failed (1F) | **Still fails (1F)** | runner checksum `fc3fe025…` vs `4e1e2e14…` |
| `ChecklistBusinessAuditAtomicityPostgresTest` | Failed (3F) | **Still fails (3F)** | `Expecting code to raise a throwable` |
| `ChecklistAuthorizationAndTodayApiEmbeddedPostgresTest` | Failed (1F) | **Still fails (1F)** | `Expecting size ≥ 3 but was 0` |
| `ChecklistAuditQueriesEmbeddedPostgresTest` | Failed (1F) | **Still fails (1F)** | missing `_bmad-output/…/AUDIT-QUERIES.sql` |
| `ChecklistImportPostgresIntegrationTest` | Failed (1E) | **Still fails (1E)** | `bad SQL grammar … preparation_checklist_items` |
| `ChecklistTemplateTypeEmbeddedPostgresTest` | Failed (2E) | **Still fails (2E)** | `bad SQL grammar … care_item_templates` |
| `ChecklistPersonalScopeMigrationEmbeddedPostgresTest` | Failed (1F/3E) | **Still fails (1F/3E)** | `bad SQL grammar … checklist_care_group_contexts` |
| `ReminderJourneyNullCareSubjectPostgresTest` | Failed (1E) | **Still fails (1E)** | `bad SQL grammar … checklist_context_authorities` |
| `ChecklistRetirementLiveUpgradeEmbeddedPostgresTest` | Failed (1E) | **Still fails (1E)** | `CHECKLIST_RETENTION_PRIVILEGED_FUNCTION_INTEGRITY_FAILED` |
| `ChecklistRollForwardEmbeddedPostgresTest` | Failed (1E) | **Still fails (1E)** | `CHECKLIST_RETENTION_PRIVILEGED_FUNCTION_INTEGRITY_FAILED` |
| `CommunityTopicIntegrationTest` | Failed (1E) | **Still fails (1E)** | `No migration with a target version 20260727010000` |
| `ContentStageConsolidationMigrationTest` | Failed (1E) | **Still fails (1E)** | `No migration with a target version 3` |
| `LifecycleContentPostgresIntegrationTest` | Failed (1F) | **Still fails (1F)** | `expected: 1L but was: 0L` |
| `DatabaseGate0IntegrationTest` | Fixed in prior session | **Passes** | — |
| `BabyJourneyLinkageRemovalMigrationPostgresTest` | Fixed in prior session | **Passes** | — |
| `HermeticDatasourceTestcontainersSmokeTest` | Fixed in prior session | **Passes** | — |

No previously failing test recovered on its own, and no new class started failing.

---
## 4. Integration Test Case Execution

Status vocabulary is the one defined for this task. **PASSED is used only where the complete Expected Result was executed and observed**; a green supporting unit or MockMvc-slice test is recorded as evidence but never promoted to PASSED on its own.

Where **Evidence test** names only a class or spec file, that is the exact executable selector used
for supporting evidence; it also means no single existing method verifies the complete ITS scenario.
`NONE` is used when no executable test exists.

### ITS-SEC-001 — A guest can register and activate a unique account through OTP

**Module:** Identity & Security  
**Original coverage:** Partial  
**Evidence class:** Integration (Spring Boot + real DB)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java`

**Evidence test:** `RegistrationIntegrationTest`

**Command:** `./mvnw -o test -Dtest=RegistrationIntegrationTest`

**Expected:** Registration and challenge persist; verification activates exactly one user; login returns tokens/session; no secret is logged.

**Observed:** 18r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Registration/verify/login persistence is exercised, but the described end-to-end chain "read OTP from the SMTP stub then log in" is not asserted, and "no secret is logged" has no assertion at all.


### ITS-SEC-002 — Concurrent OTP verification cannot consume one challenge twice

**Module:** Identity & Security  
**Original coverage:** Existing  
**Evidence class:** Integration (real PostgreSQL, 2 threads)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/OtpServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/OtpConcurrentVerificationSessionIntegrationTest.java` (new — created by this task; the analysis cited `OtpRaceConditionIntegrationTest.java`, which asserts only one of the three Expected clauses)

**Evidence test:** `OtpConcurrentVerificationSessionIntegrationTest.concurrentVerify_electsOneWinner_consumesProofOnce_andCreatesNoDuplicateSession`

**Command:** `.\mvnw.cmd -Dtest=OtpConcurrentVerificationSessionIntegrationTest test`

**Expected:** One request wins; the proof is consumed once; no duplicate user/session is created.

**Observed:** Current independent rerun: 1 test, 0 failures, 0 errors, 0 skipped, exit 0.

**Execution Result:** PASSED

**Verdict:** All three Expected clauses asserted and observed: exactly one winner, OTP consumed once (used_at set), one users row and <=1 auth_sessions row.


### ITS-SEC-003 — Firebase emulator identity exchange creates or links one CareBridge account

**Module:** Identity & Security  
**Original coverage:** Partial  
**Evidence class:** End-to-End (Playwright; emulator/API prerequisites absent)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/FederatedAuthServiceImpl.java`

**Test:** `05_Development/CareBridgeWebApp/e2e/federated-emulator.spec.ts`

**Evidence test:** `federated-emulator.spec.ts`

**Command:** `npm run test:e2e`

**Expected:** Both calls resolve the same identity; no duplicate account; invalid/revoked token returns 401 and creates nothing.

**Observed:** Both emulator-oriented tests failed; the ordinary Playwright run did not start the Firebase emulator or CareBridge API.

**Execution Result:** BLOCKED

**Verdict:** The token exchange needs the Firebase emulator plus CareBridge API on 127.0.0.1:8081. The dedicated runner imports the backend `.env`, so it was not run with potentially real credentials; no emulator-backed PASS is claimed.


### ITS-SEC-004 — Refresh rotation and logout reject replay under concurrency

**Module:** Identity & Security  
**Original coverage:** Partial  
**Evidence class:** Unit (no Spring context, no DB)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/service/AuthServiceRefreshTest.java`

**Evidence test:** `AuthServiceRefreshTest`

**Command:** `./mvnw -o test -Dtest=AuthServiceRefreshTest`

**Expected:** At most one new refresh chain survives; old/revoked tokens return 401; session rows are consistent.

**Observed:** 10r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Refresh rotation logic is unit-tested with mocks. The described concurrent race of two refreshes plus a logout against real session rows is not executed.


### ITS-SEC-005 — Active JWTs stop working after account lock, suspension, disable and role change

**Module:** Identity & Security  
**Original coverage:** Partial  
**Evidence class:** Controller/MockMvc slice

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/filter/JwtAuthenticationFilterAccountStateTest.java`

**Evidence test:** `JwtAuthenticationFilterAccountStateTest`

**Command:** `./mvnw -o test -Dtest=JwtAuthenticationFilterAccountStateTest`

**Expected:** Requests return the defined 403 code or role denial immediately; protected data is never returned.

**Observed:** 15r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Filter behaviour per account state is verified against mocked collaborators; the described mutate-then-reuse-JWT flow against persisted account state is not executed.


### ITS-SEC-006 — Revoking consent atomically blocks health, location, family and file reads

**Module:** Identity & Security  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consent/ConsentRevocationLocationShareConcurrencyPostgresTest.java`

**Evidence test:** `ConsentRevocationLocationShareConcurrencyPostgresTest`

**Command:** `./mvnw -o test -Dtest=ConsentRevocationLocationShareConcurrencyPostgresTest`

**Expected:** No read begun after commit returns revoked data; location share stops; owner data remains intact.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Only the location-share branch is verified. The Expected Result also requires health, family and file reads to stop after revocation; none of those endpoints are exercised.


### ITS-SEC-007 — Required audit failure rolls back a privileged user/role mutation

**Module:** Identity & Security  
**Original coverage:** Partial  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/AuditServiceImplTest.java`

**Evidence test:** `AuditServiceImplTest`

**Command:** `./mvnw -o test -Dtest=AuditServiceImplTest`

**Expected:** HTTP failure; business state remains unchanged; no partial audit chain.

**Observed:** 28r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Audit service behaviour is mocked. The described fault-injected audit failure rolling back a privileged role mutation over a real transaction is not executed.


### ITS-SEC-008 — Authorization for every Java endpoint and role using a generated method/path matrix

**Module:** Identity & Security  
**Original coverage:** Partial  
**Evidence class:** Controller/MockMvc slice

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/filter/JwtAuthenticationFilterAccountStateTest.java`

**Evidence test:** `JwtAuthenticationFilterAccountStateTest`

**Command:** `./mvnw -o test -Dtest=JwtAuthenticationFilterAccountStateTest`

**Expected:** Public-only endpoints remain public; protected endpoints return 401/403 or allowed result exactly; no data leaks.

**Observed:** 15r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** No generated endpoint x role authorization matrix exists. A single filter slice cannot establish the described whole-surface guarantee.


### ITS-CARE-001 — A Mother can onboard and transition a journey once with downstream events after commit

**Module:** Care Journeys  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/service/impl/JourneyServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/journey/JourneyCanonicalLifecycleIntegrationTest.java`

**Evidence test:** `JourneyCanonicalLifecycleIntegrationTest`

**Command:** `./mvnw -o test -Dtest=JourneyCanonicalLifecycleIntegrationTest`

**Expected:** Canonical journey changes once; audit and dependent records appear only after commit.

**Observed:** 10r 2F 1E

**Execution Result:** FAILED

**Verdict:** Flyway target 20260727010000 does not exist; the canonical unique index and the audit_events immutability trigger did not raise the expected root causes.


### ITS-CARE-002 — Maternal observations persist with canonical units and update trend/summary

**Module:** Care Journeys  
**Original coverage:** Partial  
**Evidence class:** End-to-End (Flutter integration_test)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthMetricController.java`

**Test:** `05_Development/CareBridgeMobileApp/integration_test/maternal_metric_p0_e2e_test.dart`

**Evidence test:** `maternal_metric_p0_e2e_test.dart`

**Command:** `flutter test --no-pub -d windows integration_test/maternal_metric_p0_e2e_test.dart`

**Expected:** Valid state is consistent across views; invalid request returns 400 and writes nothing.

**Observed:** not runnable

**Execution Result:** BLOCKED

**Verdict:** Requires a live API on 127.0.0.1:8080 plus tokens from a gitignored dart-define file. Windows desktop build fails in firebase_cpp_sdk_windows CMake; web devices are unsupported for integration tests.


### ITS-CARE-003 — A high-risk screening/log persists and triggers only supported safety guidance

**Module:** Care Journeys  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/PostpartumLogController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/PostpartumLogPostgresIntegrationTest.java`

**Evidence test:** `PostpartumLogPostgresIntegrationTest`

**Command:** `./mvnw -o test -Dtest=PostpartumLogPostgresIntegrationTest`

**Expected:** Records persist correctly; safety guidance/event follows implemented policy; no diagnostic claim.

**Observed:** 4r 0F 1E

**Execution Result:** FAILED

**Verdict:** listLogs_equalDateAndTimestamp_usesIdAsStablePostgresPageBoundary throws NullPointerException: Cannot invoke "java.lang.Short.shortValue()".


### ITS-CARE-004 — Multi-baby ownership, active selection and daily-log summary consistency

**Module:** Care Journeys  
**Original coverage:** Partial  
**Evidence class:** End-to-End (Flutter integration_test)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java`

**Test:** `05_Development/CareBridgeMobileApp/integration_test/mf03_hub_e2e_test.dart`

**Evidence test:** `mf03_hub_e2e_test.dart`

**Command:** `flutter test --no-pub -d windows integration_test/mf03_hub_e2e_test.dart`

**Expected:** Each summary contains only target baby data; foreign request is denied; selection does not move records.

**Observed:** not runnable

**Execution Result:** BLOCKED

**Verdict:** Same device/backend blocker as ITS-CARE-002; the test body is additionally gated behind skip: !apiBacked.


### ITS-CARE-005 — Vaccination completion racing with reminder dispatch produces one final state and no stale notification

**Module:** Care Journeys  
**Original coverage:** Partial  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/job/VaccinationReminderJob.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/VaccinationReminderDispatchTest.java`

**Evidence test:** `VaccinationReminderDispatchTest`

**Command:** `./mvnw -o test -Dtest=VaccinationReminderDispatchTest`

**Expected:** Completion wins canonically; no duplicate/stale due notification; book reflects completed dose.

**Observed:** 8r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Dispatch logic is mocked. The described race between vaccination completion and the reminder job over real rows and a controllable clock is not executed.


### ITS-CARE-006 — Backend-to-ML posture inference for success and stable provider failures

**Module:** Care Journeys  
**Original coverage:** Missing  
**Evidence class:** Unit (adapter, no ML container)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapter.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapterTest.java`

**Evidence test:** `ExerciseCorrectionHttpAdapterTest`

**Command:** `./mvnw -o test -Dtest=ExerciseCorrectionHttpAdapterTest`

**Expected:** Success persists feedback; 400/422/503 stable ML codes map safely; failed inference does not complete session.

**Observed:** 13r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Adapter mapping is unit-tested. The described backend-to-ML container round trip including a stopped model is not executed.


### ITS-COM-001 — A question, verified-expert answer, image and reply notification integrate after commit

**Module:** Community & Moderation  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/CommunityImageAttachmentIntegrationTest.java`

**Evidence test:** `CommunityImageAttachmentIntegrationTest`

**Command:** `./mvnw -o test -Dtest=CommunityImageAttachmentIntegrationTest`

**Expected:** Durable visible records and one notification; wrong-role answer denied; rollback leaves no orphan image.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Only image-URL round-tripping is asserted. Expert answer role denial, feed visibility, the reply notification and orphan-image rollback are not executed.


### ITS-COM-002 — Concurrent like/bookmark toggles maintain unique interaction and correct counts

**Module:** Community & Moderation  
**Original coverage:** Missing  
**Evidence class:** Controller/MockMvc slice

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityQuestionLikeControllerTest.java`

**Evidence test:** `CommunityQuestionLikeControllerTest`

**Command:** `./mvnw -o test -Dtest=CommunityQuestionLikeControllerTest`

**Expected:** Unique constraint/policy yields one canonical actor-target state and accurate counts.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Service layer is mocked, so the described concurrent toggle against a real unique constraint and count is not executed.


### ITS-MOD-001 — Report creation, atomic moderator claim and resolution preserve evidence

**Module:** Community & Moderation  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ModerationController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/report/ReportIntegrationTest.java`

**Evidence test:** `ReportIntegrationTest`

**Command:** `./mvnw -o test -Dtest=ReportIntegrationTest`

**Expected:** One claim wins; one supported action applies; report/audit/target state are consistent.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Only report creation is asserted. The described two-moderator claim race, resolution and audit consistency are not executed.


### ITS-MOD-002 — Stale or superseded moderation action cannot be undone

**Module:** Community & Moderation  
**Original coverage:** Existing  
**Evidence class:** Controller/MockMvc slice

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/exception/ModerationException.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/UndoModerationActionIntegrationTest.java`

**Evidence test:** `UndoModerationActionIntegrationTest`

**Command:** `./mvnw -o test -Dtest=UndoModerationActionIntegrationTest`

**Expected:** A returns `MOD-029`/`MOD-030` conflict; only valid latest action changes target; audit preserved.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Verifies happy-path 201 and repeat-call 409. The described stale/superseded action A-after-B conflict (MOD-029/MOD-030) and audit/target preservation are not executed, and a MockMvc slice cannot observe them.


### ITS-MOD-003 — Concurrent workers claim one scan and safely handle malformed Gemini output

**Module:** Community & Moderation  
**Original coverage:** Partial  
**Evidence class:** Integration (Spring Boot)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/job/AiContentScanWorker.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiClaimAtomicityIntegrationTest.java`

**Evidence test:** `AiClaimAtomicityIntegrationTest`

**Command:** `./mvnw -o test -Dtest=AiClaimAtomicityIntegrationTest`

**Expected:** One claim per attempt; malformed output creates no unsafe action; retry yields one assessment/case.

**Observed:** 4r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Claim atomicity is covered; the described malformed-Gemini-output-then-valid-retry branch producing exactly one assessment/case is not executed.


### ITS-EXP-001 — R2 document plus CompreFace result controls expert approval eligibility

**Module:** Expert & Consultation  
**Original coverage:** Missing  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/adapter/CompreFacePipelineAdapter.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java`

**Evidence test:** `ExpertIdentityVerificationServiceTest`

**Command:** `./mvnw -o test -Dtest=ExpertIdentityVerificationServiceTest`

**Expected:** Only valid match can advance implemented state; failures never approve; private files remain protected.

**Observed:** 5r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** R2 and CompreFace are mocked. The described document upload plus match/no-face/mismatch/timeout matrix against stubs is not executed.


### ITS-EXP-002 — Consent revocation stops an Expert location share and future reads

**Module:** Expert & Consultation  
**Original coverage:** Existing  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertavailability/controller/ExpertAvailabilityController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consent/ConsentRevocationLocationShareConcurrencyPostgresTest.java`

**Evidence test:** `ConsentRevocationLocationShareConcurrencyPostgresTest`

**Command:** `./mvnw -o test -Dtest=ConsentRevocationLocationShareConcurrencyPostgresTest`

**Expected:** Share ends or becomes inaccessible after revoke; no post-revoke snapshot is returned.

**Observed:** 1r 0F 0E

**Execution Result:** PASSED

**Verdict:** Real two-thread advisory-lock race; final state asserted in SQL: 0 rows in expert_location_shares for the consent and the data_permissions row REVOKED with revoked_at/revoked_by set.


### ITS-CON-001 — Only one Expert response wins against accept/reject/expiry races and creates one conversation

**Module:** Expert & Consultation  
**Original coverage:** Existing  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/service/impl/ConsultationRequestServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/integration/ConsultationRequestAcceptLockConcurrencyIntegrationTest.java`

**Evidence test:** `ConsultationRequestAcceptLockConcurrencyIntegrationTest`

**Command:** `./mvnw -o test -Dtest=ConsultationRequestAcceptLockConcurrencyIntegrationTest`

**Expected:** One terminal transition; at most one conversation; losers receive conflict; notification follows commit.

**Observed:** 4r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Three of four Expected clauses are verified (one terminal transition, at most one conversation, losers get CONREQ-005). The expiry race and "notification follows commit" are not executed.


### ITS-CON-002 — Only consented triage context/citations are shared with the selected Expert

**Module:** Expert & Consultation  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/context/controller/TriageExpertHandoffController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/context/TriageExpertHandoffPostgresIntegrationTest.java`

**Evidence test:** `TriageExpertHandoffPostgresIntegrationTest`

**Command:** `./mvnw -o test -Dtest=TriageExpertHandoffPostgresIntegrationTest`

**Expected:** Initial response contains allowed context only; revoked access is denied; no unrelated health data leaks.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Handoff persistence is covered; the described revoke-then-reread denial and file-access boundary are not executed.


### ITS-CHAT-001 — Idempotent durable send emits one Firebase signal and one notification after commit

**Module:** Expert & Consultation  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/integration/DirectChatWriteLockConcurrencyIntegrationTest.java`

**Evidence test:** `DirectChatWriteLockConcurrencyIntegrationTest`

**Command:** `./mvnw -o test -Dtest=DirectChatWriteLockConcurrencyIntegrationTest`

**Expected:** One durable message/signal/notification; same retry returns canonical result; changed payload returns `DCC-005`.

**Observed:** 6r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Write-lock concurrency is covered. No Firebase/FCM emulator participates, so "one Firebase signal and one notification after commit" and the DCC-005 changed-payload branch are not executed.


### ITS-CHAT-002 — Authorized call transitions and Zego join credentials; provider failure preserves state

**Module:** Expert & Consultation  
**Original coverage:** Partial  
**Evidence class:** Contract (no Spring context)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/controller/ConversationCallControllerContractTest.java`

**Evidence test:** `ConversationCallControllerContractTest`

**Command:** `./mvnw -o test -Dtest=ConversationCallControllerContractTest`

**Expected:** Participants receive scoped credentials; outsider gets 403; `DCC-008` on provider failure; canonical call state remains valid.

**Observed:** 4r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Contract shapes only. The described Zego token-provider failure (DCC-008), outsider 403 and timeout job are not executed against real call state.


### ITS-AI-002 — Java and Python apply the same versioned rule vectors and dataset integrity

**Module:** AI Triage & Gemini  
**Original coverage:** Partial  
**Evidence class:** Contract (Python only)

**Source:** `05_Development/CareBridgeAITriageService/tests/data/triage_rule_parity_vectors_v2.json`

**Test:** `05_Development/CareBridgeAITriageService/tests/test_rule_registry_parity_v2.py`

**Evidence test:** `test_rule_registry_parity_v2.py`

**Command:** `./.venv/Scripts/python.exe -m pytest -q tests/test_rule_registry_parity_v2.py`

**Expected:** Results match canonical expectations; any hash/version mismatch fails the gate.

**Observed:** 112r 0F (with 2 other files)

**Execution Result:** NOT_EXECUTED

**Verdict:** Python-side parity vectors pass. The described cross-runtime comparison actually running the same vectors through the Java boundary is not executed as one gate.


### ITS-AI-003 — Internal V2 requires service authority distinct from user/admin JWT

**Module:** AI Triage & Gemini  
**Original coverage:** Partial  
**Evidence class:** Contract (Python only, FastAPI TestClient)

**Source:** `05_Development/CareBridgeAITriageService/app/main.py`

**Test:** `05_Development/CareBridgeAITriageService/tests/test_triage_v2_api.py`

**Evidence test:** `test_triage_v2_api.py`

**Command:** `./.venv/Scripts/python.exe -m pytest -q tests/test_triage_v2_api.py`

**Expected:** Invalid credentials are rejected without session mutation; valid service call returns schema-valid turn.

**Observed:** 112r 0F (with 2 other files)

**Execution Result:** NOT_EXECUTED

**Verdict:** Python-side auth rejection is covered. The described matrix using a real user/admin JWT issued by the Java service is not executed.


### ITS-AI-004 — A timed-out V2 turn can be retried/cancelled without duplicate or corrupt state

**Module:** AI Triage & Gemini  
**Original coverage:** Missing  
**Evidence class:** None

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/controller/InternalTriageV2Controller.java`

**Test:** NONE

**Evidence test:** `NONE`

**Command:** `n/a — no executable test exists`

**Expected:** One canonical terminal/resumable state; late response cannot resurrect cancelled state; no duplicate turn.

**Observed:** no test

**Execution Result:** NOT_EXECUTED

**Verdict:** The analysis records no existing test. No timeout/retry/cancel verification of an internal V2 turn exists and building one requires both services running with a controllable delayed AI response.


### ITS-AI-005 — Only reviewed/active evidence is visible internally and citations disappear after withdrawal

**Module:** AI Triage & Gemini  
**Original coverage:** Partial  
**Evidence class:** Contract (Python client)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/controller/InternalEvidenceSourceController.java`

**Test:** `05_Development/CareBridgeAITriageService/tests/test_evidence_registry_client.py`

**Evidence test:** `test_evidence_registry_client.py`

**Command:** `./.venv/Scripts/python.exe -m pytest -q tests/test_evidence_registry_client.py`

**Expected:** Draft/withdrawn source excluded; active source cited with correct version; no fabricated fallback citation.

**Observed:** 112r 0F (with 2 other files)

**Execution Result:** NOT_EXECUTED

**Verdict:** Client filtering is covered. The described publish/withdraw lifecycle driven through the Java internal evidence controller is not executed.


### ITS-AI-006 — Committed intake writes minimized memory and consent revoke/delete removes it from later context

**Module:** AI Triage & Gemini  
**Original coverage:** Partial  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/service/HealthMemoryWriteHandler.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/HealthMemoryWriteTest.java`

**Evidence test:** `HealthMemoryWriteTest`

**Command:** `./mvnw -o test -Dtest=HealthMemoryWriteTest`

**Expected:** Only minimized allowed data persists; later context excludes revoked/deleted entry; raw sensitive payload absent.

**Observed:** 4r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Write minimisation is unit-tested. The described revoke/delete then later-intake context exclusion is not executed against persisted memory.


### ITS-AI-007 — Timeout, 429, 5xx, malformed output and bad citations fail safely

**Module:** AI Triage & Gemini  
**Original coverage:** Partial  
**Evidence class:** Unit (no Spring context)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/service/GeminiRagServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/GeminiRagServiceStory69Test.java`

**Evidence test:** `GeminiRagServiceStory69Test`

**Command:** `./mvnw -o test -Dtest=GeminiRagServiceStory69Test`

**Expected:** Typed unavailable/fallback behavior; no fabricated citations/diagnosis; prompt/key redacted; no authoritative health state written.

**Observed:** 9r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Some fallback paths are unit-tested. The full described failure matrix (timeout, 429, 5xx, malformed, bad citations) with log-redaction assertions is not executed.


### ITS-MAP-001 — TrackAsia success and failure contracts retain source/status and never fabricate route/ETA

**Module:** Emergency Map & TrackAsia  
**Original coverage:** Missing  
**Evidence class:** Contract (Flutter widget)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/trackasia/TrackAsiaClient.java`

**Test:** `05_Development/CareBridgeMobileApp/test/features/emergency/trackasia_web_contract_test.dart`

**Evidence test:** `trackasia_web_contract_test.dart`

**Command:** `flutter test --no-pub test/features/emergency/trackasia_web_contract_test.dart`

**Expected:** Verified DB facilities remain correct; provider results are labeled; failures show safe fallback/manual guidance.

**Observed:** 2r 0F

**Execution Result:** NOT_EXECUTED

**Verdict:** Verifies the web runtime is the pinned official TrackAsia build. The described success/empty/malformed/timeout/5xx provider matrix and the backend TrackAsiaClient are not executed.


### ITS-EMR-001 — Completed owned RED intake opens/reuses one emergency session and family alert

**Module:** Emergency Map & TrackAsia  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/service/impl/EmergencyService.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyTriageLinkPostgresIntegrationTest.java`

**Evidence test:** `EmergencyTriageLinkPostgresIntegrationTest`

**Command:** `./mvnw -o test -Dtest=EmergencyTriageLinkPostgresIntegrationTest`

**Expected:** One emergency event; location purpose-bound; one logical family alert; urgent actions visible.

**Observed:** 4r 0F 1E

**Execution Result:** FAILED

**Verdict:** seedRetryCandidate fails: null value in column "alert_successful_recipient_count" of relation "safety_events" violates not-null constraint.


### ITS-EMR-002 — Partial FCM success is recorded and only failed recipients/tokens are retried

**Module:** Emergency Map & TrackAsia  
**Original coverage:** Partial  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/service/EmergencyAlertRetryJob.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyAlertRetryJobTest.java`

**Evidence test:** `EmergencyAlertRetryJobTest`

**Command:** `./mvnw -o test -Dtest=EmergencyAlertRetryJobTest`

**Expected:** Successful token is not duplicated; failed eligible token retries; attempt/status history remains consistent.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Retry-job logic is mocked; the described escalation against real safety events and FCM is not executed.


### ITS-HLT-001 — Field-level health projection for Mother, permitted Family, unpermitted Family and Expert

**Module:** Health Records & Storage  
**Original coverage:** Missing  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthSummaryController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`

**Evidence test:** `SharedDataServiceImplTest`

**Command:** `./mvnw -o test -Dtest=SharedDataServiceImplTest`

**Expected:** Each role receives only allowed fields; denied requests return 403/404 without existence leak.

**Observed:** 9r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Sharing rules are mocked. The described cross-endpoint shared-data boundary against persisted grants is not executed.


### ITS-FILE-001 — Provider upload success followed by DB failure does not leave an untracked private object

**Module:** Health Records & Storage  
**Original coverage:** Missing  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/service/impl/FileServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/FileServiceImplTest.java`

**Evidence test:** `FileServiceImplTest`

**Command:** `./mvnw -o test -Dtest=FileServiceImplTest`

**Expected:** HTTP failure; no accessible metadata; object is compensated or deterministically queued for cleanup.

**Observed:** 15r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Storage provider is mocked. The described upload plus DB-failure compensation against a real object store is not executed.


### ITS-FILE-002 — Health, identity and chat files require current target authorization and do not expose raw URLs

**Module:** Health Records & Storage  
**Original coverage:** Partial  
**Evidence class:** Controller/MockMvc slice

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/policy/FileAccessPolicy.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/FileControllerViewTest.java`

**Evidence test:** `FileControllerViewTest`

**Command:** `./mvnw -o test -Dtest=FileControllerViewTest`

**Expected:** Allowed actor receives content/proxy result; outsider/revoked actor gets 403/404; no private raw URL leaks.

**Observed:** 3r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** View authorization is sliced with mocks; private-URL leakage and deletion behaviour against real storage are not executed.


### ITS-REM-001 — One recurrence occurrence creates one job, notification and task action

**Module:** Reminders & Notifications  
**Original coverage:** Partial  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/schedule/job/ReminderScheduleWorker.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/schedule/job/ReminderScheduleWorkerTest.java`

**Evidence test:** `ReminderScheduleWorkerTest`

**Command:** `./mvnw -o test -Dtest=ReminderScheduleWorkerTest`

**Expected:** One occurrence/job/delivery; action updates canonical state; duplicates are prevented.

**Observed:** 4r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Worker logic is mocked. The described occurrence generation and idempotent dispatch against real schedule rows are not executed.


### ITS-REM-002 — Recurrence and appointment notifications at timezone/daylight boundaries

**Module:** Reminders & Notifications  
**Original coverage:** Missing  
**Evidence class:** None

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/schedule/job/ReminderScheduleHorizonPlanner.java`

**Test:** NONE

**Evidence test:** `NONE`

**Command:** `n/a — no executable test exists`

**Expected:** No skipped/double logical occurrence; response and notification use intended local time.

**Observed:** no test

**Execution Result:** NOT_EXECUTED

**Verdict:** The analysis records no existing test and no executable verification exists in the tree.


### ITS-REM-003 — Concurrent Mother/Family complete/snooze/skip commands resolve to one canonical task state

**Module:** Reminders & Notifications  
**Original coverage:** Partial  
**Evidence class:** Unit (no Spring context)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/TodayTaskController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/UnifiedTodayTaskServiceTest.java`

**Evidence test:** `UnifiedTodayTaskServiceTest`

**Command:** `./mvnw -o test -Dtest=UnifiedTodayTaskServiceTest`

**Expected:** One valid transition; idempotent replay; losing invalid transitions return conflict; history matches state.

**Observed:** 5r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Today-task assembly is unit-tested; the described cross-source unified view over persisted reminders and checklist tasks is not executed.


### ITS-NOT-001 — An FCM token moves safely between logged-in users on one device

**Module:** Reminders & Notifications  
**Original coverage:** Missing  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/notification/NotificationDeviceTokenServiceTest.java`

**Evidence test:** `NotificationDeviceTokenServiceTest`

**Command:** `./mvnw -o test -Dtest=NotificationDeviceTokenServiceTest`

**Expected:** Token is bound only to current intended user; A receives nothing after switch; no duplicate token rows.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Token registration is mocked; the described account-switch and multi-recipient FCM behaviour against an emulator is not executed.


### ITS-NOT-002 — Provider success followed by worker crash/DB failure does not duplicate logical notification

**Module:** Reminders & Notifications  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/job/DirectMessageNotificationOutboxJob.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/notification/service/DirectMessageNotificationServiceIdempotencyIntegrationTest.java`

**Evidence test:** `DirectMessageNotificationServiceIdempotencyIntegrationTest`

**Command:** `./mvnw -o test -Dtest=DirectMessageNotificationServiceIdempotencyIntegrationTest`

**Expected:** Idempotency policy prevents or clearly detects duplicate; job reaches consistent terminal/retry state.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Verifies two sequential calls produce one row and one FCM call. The described worker-crash / DB-failure fault injection and terminal retry state are not executed.


### ITS-FAM-001 — A care-group invite can be accepted exactly once by the intended user

**Module:** Family Sync  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareGroupInviteIntegrationTest.java`

**Evidence test:** `CareGroupInviteIntegrationTest`

**Command:** `./mvnw -o test -Dtest=CareGroupInviteIntegrationTest`

**Expected:** One active membership; replay/foreign attempt denied; no duplicate permissions.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Invite persistence is covered; the described concurrent accept/revoke race and downstream permission effects are not executed.


### ITS-FAM-002 — Family permission revocation races safely with dashboard, health, calendar, task and file reads

**Module:** Family Sync  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/policy/CareGroupAuthorizationPolicy.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/ManageFamilyPermissionIntegrationTest.java`

**Evidence test:** `ManageFamilyPermissionIntegrationTest`

**Command:** `./mvnw -o test -Dtest=ManageFamilyPermissionIntegrationTest`

**Expected:** No post-commit access; no partial field leak; owner state remains; assigned-task policy is consistent.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Only patch-then-get persistence is asserted. The described revocation racing dashboard, health, calendar, task and file reads is not executed.


### ITS-FAM-003 — Removing a member with open assigned tasks preserves a deterministic task state

**Module:** Family Sync  
**Original coverage:** Missing  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java`

**Evidence test:** `CareTaskAssignmentIntegrationTest`

**Command:** `./mvnw -o test -Dtest=CareTaskAssignmentIntegrationTest`

**Expected:** One policy-defined result; removed member loses access; no orphan/cross-group task exposure.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Assign-then-list is covered; the described assignment policy under permission change is not executed.


### ITS-CNT-001 — Draft-to-approved content is immutable, sanitized and visible only after commit

**Module:** Content & Checklist  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentApprovalController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/integration/ContentBodySanitizeIntegrationTest.java`

**Evidence test:** `ContentBodySanitizeIntegrationTest`

**Command:** `./mvnw -o test -Dtest=ContentBodySanitizeIntegrationTest`

**Expected:** Sanitized approved version appears; draft never appears; edit returns `VERSION_IMMUTABLE`; audit exists.

**Observed:** 2r 0F 0E 1S

**Execution Result:** BLOCKED

**Verdict:** Sanitisation passes, but uploadPublicContentImage_endToEnd is @Disabled("Requires real CLOUDINARY_* credentials"), so the described draft-to-approved immutability and VERSION_IMMUTABLE branch cannot complete here.


### ITS-CNT-002 — Unpublished content disappears from list/search/lifecycle and future Gemini context

**Module:** Content & Checklist  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentUnpublishController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/UnpublishContentIntegrationTest.java`

**Evidence test:** `UnpublishContentIntegrationTest`

**Command:** `./mvnw -o test -Dtest=UnpublishContentIntegrationTest`

**Expected:** Content no longer appears or is cited; historical audit/version remains.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Unpublish is covered; the described visibility-after-commit and audit assertions of the full lifecycle are not executed.


### ITS-CHK-001 — Approving a template distributes one instance/task set with required audit

**Module:** Content & Checklist  
**Original coverage:** Partial  
**Evidence class:** End-to-End (Flutter integration_test)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/distribution/ChecklistDistributionService.java`

**Test:** `05_Development/CareBridgeMobileApp/integration_test/checklist_distribution_e2e_test.dart`

**Evidence test:** `checklist_distribution_e2e_test.dart`

**Command:** `flutter test --no-pub -d windows integration_test/checklist_distribution_e2e_test.dart`

**Expected:** One approved version and one distribution; correct ordered tasks; audit and response consistent.

**Observed:** not runnable

**Execution Result:** BLOCKED

**Verdict:** Requires CHK_E2E_* dart-defines, real access/refresh tokens and a dedicated device. Same Windows CMake / web-unsupported blocker as the other Flutter integration tests.


### ITS-CHK-002 — Recommendations rank approved content without exposing health context or returning revoked-context results

**Module:** Content & Checklist  
**Original coverage:** Partial  
**Evidence class:** Unit (no Spring context)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/service/RecommendationRanker.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/recommendation/RecommendationPrivacyBoundaryTest.java`

**Evidence test:** `RecommendationPrivacyBoundaryTest`

**Command:** `./mvnw -o test -Dtest=RecommendationPrivacyBoundaryTest`

**Expected:** Only eligible content/tags returned; health DTO values absent; revoked context no longer influences result.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Privacy boundary is unit-asserted; the described checklist/recommendation distribution across real journeys is not executed.


### ITS-EXPENSE-001 — Expense CRUD updates owner-only summary with defined currency behavior

**Module:** Expense & Device Data  
**Original coverage:** Missing  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/ExpenseController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/ExpenseServiceTest.java`

**Evidence test:** `ExpenseServiceTest`

**Command:** `./mvnw -o test -Dtest=ExpenseServiceTest`

**Expected:** Correct persisted totals for supported currency rule; foreign denied; invalid request writes nothing.

**Observed:** 13r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Expense rules are mocked; no persisted-boundary verification of the described scenario exists.


### ITS-DEV-001 — Sensor/camera/location collection stops safely when OS permission is revoked mid-feature

**Module:** Expense & Device Data  
**Original coverage:** Missing  
**Evidence class:** Unit (Flutter widget/coordinator)

**Source:** `05_Development/CareBridgeMobileApp/lib/features/safety/services/safety_foreground_service.dart`

**Test:** `05_Development/CareBridgeMobileApp/test/features/safety/safety_foreground_coordinator_test.dart`

**Evidence test:** `safety_foreground_coordinator_test.dart`

**Command:** `flutter test --no-pub test/features/safety/safety_foreground_coordinator_test.dart`

**Expected:** Feature stops/limits safely; manual guidance remains; no subsequent derived upload from revoked source.

**Observed:** 14r 0F

**Execution Result:** NOT_EXECUTED

**Verdict:** Coordinator state machine passes on device-side only. The described device-data ingestion boundary to the backend is not executed.


### ITS-SAFE-001 — Burst/replayed fall signals create one pending event/countdown

**Module:** Safety Alerts & FCM  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyMonitoringConcurrencyPostgresIntegrationTest.java`

**Evidence test:** `SafetyMonitoringConcurrencyPostgresIntegrationTest`

**Command:** `./mvnw -o test -Dtest=SafetyMonitoringConcurrencyPostgresIntegrationTest`

**Expected:** One canonical event/countdown; invalid/disabled signals rejected; no raw stream stored.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Only concurrentEnableReturnsOneCanonicalActiveSession is asserted. The described burst/replayed fall signals, invalid/disabled rejection and "no raw stream stored" are not executed.


### ITS-SAFE-002 — Cancel/need-help/job timeout at the exact boundary yields one terminal state

**Module:** Safety Alerts & FCM  
**Original coverage:** Missing  
**Evidence class:** Unit (no Spring context)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/SafetyCountdownJob.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SuspectedFallDetectedHandlerTest.java`

**Evidence test:** `SuspectedFallDetectedHandlerTest`

**Command:** `./mvnw -o test -Dtest=SuspectedFallDetectedHandlerTest`

**Expected:** Exactly one terminal transition; cancel suppresses escalation only if it wins; alert is never duplicated.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Handler logic only; the described countdown/escalation against persisted events is not executed.


### ITS-SAFE-003 — Urgent safety FCM reaches only authorized family and deep-links to an accessible event

**Module:** Safety Alerts & FCM  
**Original coverage:** Missing  
**Evidence class:** Unit (Mockito)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/adapter/FcmNotificationPortAdapter.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyNotificationDeliveryAdaptersTest.java`

**Evidence test:** `EmergencyNotificationDeliveryAdaptersTest`

**Command:** `./mvnw -o test -Dtest=EmergencyNotificationDeliveryAdaptersTest`

**Expected:** Only eligible devices receive minimized payload; link authorizes current membership; revoked actor cannot view event.

**Observed:** 3r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Adapters are mocked; no FCM emulator participates in the described delivery/retry verification.


### ITS-OTH-001 — Search excludes hidden/unapproved/foreign data and remains stable across pages

**Module:** Cross-cutting / Other  
**Original coverage:** Partial  
**Evidence class:** Repository/PostgreSQL integration

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/search/controller/SearchController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/search/SearchIntegrationTest.java`

**Evidence test:** `SearchIntegrationTest`

**Command:** `./mvnw -o test -Dtest=SearchIntegrationTest`

**Expected:** Only authorized visible items returned; no duplicates/private fields; cursor/page behavior follows implementation.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Search persistence is covered; the described cross-module scope and permission filtering are not executed.


### ITS-OTH-002 — Only DB-backed readiness is anonymous and reflects DB outage

**Module:** Cross-cutting / Other  
**Original coverage:** Existing  
**Evidence class:** Integration (Spring Boot)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/ReadinessEndpointSecurityIntegrationTest.java`

**Evidence test:** `ReadinessEndpointSecurityIntegrationTest`

**Command:** `./mvnw -o test -Dtest=ReadinessEndpointSecurityIntegrationTest`

**Expected:** Readiness reports unavailable when DB is down; other actuator operations are denied; no sensitive details leak.

**Observed:** 2r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Anonymous readiness and actuator denial are verified. The described "readiness reports unavailable when the DB is down" branch is not executed.


### ITS-OTH-003 — Manual seed endpoint is absent outside `dev & !prod` even if the enable property is set

**Module:** Cross-cutting / Other  
**Original coverage:** Missing  
**Evidence class:** Integration (Testcontainers + embedded Postgres)

**Source:** `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/ManualSeedController.java`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/common/dev/DevDataSeederPostgresIntegrationTest.java`

**Evidence test:** `DevDataSeederPostgresIntegrationTest`

**Command:** `./mvnw -o test -Dtest=DevDataSeederPostgresIntegrationTest`

**Expected:** Dev+enabled seeds deterministically; staging/prod route is absent and DB unchanged.

**Observed:** 1r 0F 0E

**Execution Result:** NOT_EXECUTED

**Verdict:** Seeder runs; the described gated dev-seed policy matrix is not executed.


### ITS-OTH-004 — A clean database and each supported prior schema migrate to the canonical PostgreSQL schema

**Module:** Cross-cutting / Other  
**Original coverage:** Partial  
**Evidence class:** Integration (Testcontainers, PostgreSQL 18.1)

**Source:** `05_Development/CareBridgeAPI/src/main/resources/db/migration`

**Test:** `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/migration/Postgresql18CanonicalSchemaIntegrationTest.java`

**Evidence test:** `Postgresql18CanonicalSchemaIntegrationTest`

**Command:** `./mvnw -o test -Dtest=Postgresql18CanonicalSchemaIntegrationTest`

**Expected:** Flyway completes once; canonical tables/indexes/constraints exist; no data loss.

**Observed:** 1r 1F 0E

**Execution Result:** FAILED

**Verdict:** Canonical base-table count assertion failed: expected 57 but was 63. The migration chain itself now completes.


### ITS-OTH-005 — Nginx/Cloudflare edge preserves auth, CORS, multipart and service failure behavior

**Module:** Cross-cutting / Other  
**Original coverage:** Missing  
**Evidence class:** None

**Source:** `05_Development/Deployment/nginx/edge-api.conf`

**Test:** NONE

**Evidence test:** `NONE`

**Command:** `n/a — no executable test exists`

**Expected:** Correct routing/CORS/limits; no auth header loss; controlled 4xx/5xx; no direct private-service exposure.

**Observed:** no test

**Execution Result:** NOT_EXECUTED

**Verdict:** The analysis records no existing test and no executable verification exists in the tree.


---

## 5. Integration Test Round Mapping

`Round 1` = historical evidence from `TEST_STATUS_2026-08-09.md` (only the four scenarios it recorded an exact matching failure for). `Round 2` = the full-suite execution performed in this task. `Round 3` = the targeted re-execution performed after Round 2. `Pending` means no execution produced a verdict for that round.

| Test Case ID | Description | Round 1 | Round 2 | Round 3 | Final Status | Test Path | Exact Test | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ITS-SEC-001 | Verify that a guest can register and activate a unique account through OTP. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java` | `RegistrationIntegrationTest` | Registration/verify/login persistence is exercised, but the described end-to-end chain "read OTP from the SMTP stub then log in" is not asserted, and "no secret i... |
| ITS-SEC-002 | Verify that concurrent OTP verification cannot consume one challenge twice. | Pending | Passed | Passed | Passed | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/OtpRaceConditionIntegrationTest.java` | `OtpConcurrentVerificationSessionIntegrationTest (new)` | All three Expected clauses asserted and observed: exactly one winner, OTP consumed once (used_at set), one users row and <=1 auth_sessions row. |
| ITS-SEC-003 | Verify that Firebase emulator identity exchange creates or links one CareBridge account. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeWebApp/e2e/federated-emulator.spec.ts` | `federated-emulator.spec.ts` | Ordinary Playwright run did not provide the required emulator/API; the dedicated runner imports backend `.env`, so no real credentials were used and no emulator-backed PASS is claimed. |
| ITS-SEC-004 | Verify that refresh rotation and logout reject replay under concurrency. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/service/AuthServiceRefreshTest.java` | `AuthServiceRefreshTest` | Refresh rotation logic is unit-tested with mocks. The described concurrent race of two refreshes plus a logout against real session rows is not executed. |
| ITS-SEC-005 | Verify that active JWTs stop working after account lock, suspension, disable and role change. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/filter/JwtAuthenticationFilterAccountStateTest.java` | `JwtAuthenticationFilterAccountStateTest` | Filter behaviour per account state is verified against mocked collaborators; the described mutate-then-reuse-JWT flow against persisted account state is not execu... |
| ITS-SEC-006 | Verify that revoking consent atomically blocks health, location, family and file reads. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consent/ConsentRevocationLocationShareConcurrencyPostgresTest.java` | `ConsentRevocationLocationShareConcurrencyPostgresTest` | Only the location-share branch is verified. The Expected Result also requires health, family and file reads to stop after revocation; none of those endpoints are ... |
| ITS-SEC-007 | Verify that required audit failure rolls back a privileged user/role mutation. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/AuditServiceImplTest.java` | `AuditServiceImplTest` | Audit service behaviour is mocked. The described fault-injected audit failure rolling back a privileged role mutation over a real transaction is not executed. |
| ITS-SEC-008 | Verify authorization for every Java endpoint and role using a generated method/path matrix. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/filter/JwtAuthenticationFilterAccountStateTest.java` | `JwtAuthenticationFilterAccountStateTest` | No generated endpoint x role authorization matrix exists. A single filter slice cannot establish the described whole-surface guarantee. |
| ITS-CARE-001 | Verify that a Mother can onboard and transition a journey once with downstream events after commit. | Failed | Failed | Failed | Failed | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/journey/JourneyCanonicalLifecycleIntegrationTest.java` | `JourneyCanonicalLifecycleIntegrationTest` | Flyway target 20260727010000 does not exist; the canonical unique index and the audit_events immutability trigger did not raise the expected root causes. |
| ITS-CARE-002 | Verify that maternal observations persist with canonical units and update trend/summary. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeMobileApp/integration_test/maternal_metric_p0_e2e_test.dart` | `maternal_metric_p0_e2e_test.dart` | Requires a live API on 127.0.0.1:8080 plus tokens from a gitignored dart-define file. Windows desktop build fails in firebase_cpp_sdk_windows CMake; web devices a... |
| ITS-CARE-003 | Verify that a high-risk screening/log persists and triggers only supported safety guidance. | Failed | Failed | Failed | Failed | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/PostpartumLogPostgresIntegrationTest.java` | `PostpartumLogPostgresIntegrationTest` | listLogs_equalDateAndTimestamp_usesIdAsStablePostgresPageBoundary throws NullPointerException: Cannot invoke "java.lang.Short.shortValue()". |
| ITS-CARE-004 | Verify multi-baby ownership, active selection and daily-log summary consistency. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeMobileApp/integration_test/mf03_hub_e2e_test.dart` | `mf03_hub_e2e_test.dart` | Same device/backend blocker as ITS-CARE-002; the test body is additionally gated behind skip: !apiBacked. |
| ITS-CARE-005 | Verify vaccination completion racing with reminder dispatch produces one final state and no stale notification. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/VaccinationReminderDispatchTest.java` | `VaccinationReminderDispatchTest` | Dispatch logic is mocked. The described race between vaccination completion and the reminder job over real rows and a controllable clock is not executed. |
| ITS-CARE-006 | Verify backend-to-ML posture inference for success and stable provider failures. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapterTest.java` | `ExerciseCorrectionHttpAdapterTest` | Adapter mapping is unit-tested. The described backend-to-ML container round trip including a stopped model is not executed. |
| ITS-COM-001 | Verify a question, verified-expert answer, image and reply notification integrate after commit. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/CommunityImageAttachmentIntegrationTest.java` | `CommunityImageAttachmentIntegrationTest` | Only image-URL round-tripping is asserted. Expert answer role denial, feed visibility, the reply notification and orphan-image rollback are not executed. |
| ITS-COM-002 | Verify concurrent like/bookmark toggles maintain unique interaction and correct counts. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityQuestionLikeControllerTest.java` | `CommunityQuestionLikeControllerTest` | Service layer is mocked, so the described concurrent toggle against a real unique constraint and count is not executed. |
| ITS-MOD-001 | Verify report creation, atomic moderator claim and resolution preserve evidence. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/report/ReportIntegrationTest.java` | `ReportIntegrationTest` | Only report creation is asserted. The described two-moderator claim race, resolution and audit consistency are not executed. |
| ITS-MOD-002 | Verify stale or superseded moderation action cannot be undone. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/UndoModerationActionIntegrationTest.java` | `UndoModerationActionIntegrationTest` | Verifies happy-path 201 and repeat-call 409. The described stale/superseded action A-after-B conflict (MOD-029/MOD-030) and audit/target preservation are not exec... |
| ITS-MOD-003 | Verify concurrent workers claim one scan and safely handle malformed Gemini output. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiClaimAtomicityIntegrationTest.java` | `AiClaimAtomicityIntegrationTest` | Claim atomicity is covered; the described malformed-Gemini-output-then-valid-retry branch producing exactly one assessment/case is not executed. |
| ITS-EXP-001 | Verify R2 document plus CompreFace result controls expert approval eligibility. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | `ExpertIdentityVerificationServiceTest` | R2 and CompreFace are mocked. The described document upload plus match/no-face/mismatch/timeout matrix against stubs is not executed. |
| ITS-EXP-002 | Verify consent revocation stops an Expert location share and future reads. | Pending | Passed | Passed | Passed | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consent/ConsentRevocationLocationShareConcurrencyPostgresTest.java` | `ConsentRevocationLocationShareConcurrencyPostgresTest` | Real two-thread advisory-lock race; final state asserted in SQL: 0 rows in expert_location_shares for the consent and the data_permissions row REVOKED with revoke... |
| ITS-CON-001 | Verify only one Expert response wins against accept/reject/expiry races and creates one conversation. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/integration/ConsultationRequestAcceptLockConcurrencyIntegrationTest.java` | `ConsultationRequestAcceptLockConcurrencyIntegrationTest` | Three of four Expected clauses are verified (one terminal transition, at most one conversation, losers get CONREQ-005). The expiry race and "notification follows ... |
| ITS-CON-002 | Verify only consented triage context/citations are shared with the selected Expert. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/context/TriageExpertHandoffPostgresIntegrationTest.java` | `TriageExpertHandoffPostgresIntegrationTest` | Handoff persistence is covered; the described revoke-then-reread denial and file-access boundary are not executed. |
| ITS-CHAT-001 | Verify idempotent durable send emits one Firebase signal and one notification after commit. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/integration/DirectChatWriteLockConcurrencyIntegrationTest.java` | `DirectChatWriteLockConcurrencyIntegrationTest` | Write-lock concurrency is covered. No Firebase/FCM emulator participates, so "one Firebase signal and one notification after commit" and the DCC-005 changed-paylo... |
| ITS-CHAT-002 | Verify authorized call transitions and Zego join credentials; provider failure preserves state. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/controller/ConversationCallControllerContractTest.java` | `ConversationCallControllerContractTest` | Contract shapes only. The described Zego token-provider failure (DCC-008), outsider 403 and timeout job are not executed against real call state. |
| ITS-AI-002 | Verify Java and Python apply the same versioned rule vectors and dataset integrity. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAITriageService/tests/test_rule_registry_parity_v2.py` | `test_rule_registry_parity_v2.py` | Python-side parity vectors pass. The described cross-runtime comparison actually running the same vectors through the Java boundary is not executed as one gate. |
| ITS-AI-003 | Verify internal V2 requires service authority distinct from user/admin JWT. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAITriageService/tests/test_triage_v2_api.py` | `test_triage_v2_api.py` | Python-side auth rejection is covered. The described matrix using a real user/admin JWT issued by the Java service is not executed. |
| ITS-AI-004 | Verify a timed-out V2 turn can be retried/cancelled without duplicate or corrupt state. | Pending | Pending | Pending | Pending | NONE | `NONE` | The analysis records no existing test. No timeout/retry/cancel verification of an internal V2 turn exists and building one requires both services running with a c... |
| ITS-AI-005 | Verify only reviewed/active evidence is visible internally and citations disappear after withdrawal. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAITriageService/tests/test_evidence_registry_client.py` | `test_evidence_registry_client.py` | Client filtering is covered. The described publish/withdraw lifecycle driven through the Java internal evidence controller is not executed. |
| ITS-AI-006 | Verify committed intake writes minimized memory and consent revoke/delete removes it from later context. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage/HealthMemoryWriteTest.java` | `HealthMemoryWriteTest` | Write minimisation is unit-tested. The described revoke/delete then later-intake context exclusion is not executed against persisted memory. |
| ITS-AI-007 | Verify timeout, 429, 5xx, malformed output and bad citations fail safely. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/GeminiRagServiceStory69Test.java` | `GeminiRagServiceStory69Test` | Some fallback paths are unit-tested. The full described failure matrix (timeout, 429, 5xx, malformed, bad citations) with log-redaction assertions is not executed. |
| ITS-MAP-001 | Verify TrackAsia success and failure contracts retain source/status and never fabricate route/ETA. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeMobileApp/test/features/emergency/trackasia_web_contract_test.dart` | `trackasia_web_contract_test.dart` | Verifies the web runtime is the pinned official TrackAsia build. The described success/empty/malformed/timeout/5xx provider matrix and the backend TrackAsiaClient... |
| ITS-EMR-001 | Verify completed owned RED intake opens/reuses one emergency session and family alert. | Failed | Failed | Failed | Failed | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyTriageLinkPostgresIntegrationTest.java` | `EmergencyTriageLinkPostgresIntegrationTest` | seedRetryCandidate fails: null value in column "alert_successful_recipient_count" of relation "safety_events" violates not-null constraint. |
| ITS-EMR-002 | Verify partial FCM success is recorded and only failed recipients/tokens are retried. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyAlertRetryJobTest.java` | `EmergencyAlertRetryJobTest` | Retry-job logic is mocked; the described escalation against real safety events and FCM is not executed. |
| ITS-HLT-001 | Verify field-level health projection for Mother, permitted Family, unpermitted Family and Expert. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java` | `SharedDataServiceImplTest` | Sharing rules are mocked. The described cross-endpoint shared-data boundary against persisted grants is not executed. |
| ITS-FILE-001 | Verify provider upload success followed by DB failure does not leave an untracked private object. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/FileServiceImplTest.java` | `FileServiceImplTest` | Storage provider is mocked. The described upload plus DB-failure compensation against a real object store is not executed. |
| ITS-FILE-002 | Verify health, identity and chat files require current target authorization and do not expose raw URLs. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/FileControllerViewTest.java` | `FileControllerViewTest` | View authorization is sliced with mocks; private-URL leakage and deletion behaviour against real storage are not executed. |
| ITS-REM-001 | Verify one recurrence occurrence creates one job, notification and task action. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/schedule/job/ReminderScheduleWorkerTest.java` | `ReminderScheduleWorkerTest` | Worker logic is mocked. The described occurrence generation and idempotent dispatch against real schedule rows are not executed. |
| ITS-REM-002 | Verify recurrence and appointment notifications at timezone/daylight boundaries. | Pending | Pending | Pending | Pending | NONE | `NONE` | The analysis records no existing test and no executable verification exists in the tree. |
| ITS-REM-003 | Verify concurrent Mother/Family complete/snooze/skip commands resolve to one canonical task state. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/UnifiedTodayTaskServiceTest.java` | `UnifiedTodayTaskServiceTest` | Today-task assembly is unit-tested; the described cross-source unified view over persisted reminders and checklist tasks is not executed. |
| ITS-NOT-001 | Verify an FCM token moves safely between logged-in users on one device. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/notification/NotificationDeviceTokenServiceTest.java` | `NotificationDeviceTokenServiceTest` | Token registration is mocked; the described account-switch and multi-recipient FCM behaviour against an emulator is not executed. |
| ITS-NOT-002 | Verify provider success followed by worker crash/DB failure does not duplicate logical notification. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/notification/service/DirectMessageNotificationServiceIdempotencyIntegrationTest.java` | `DirectMessageNotificationServiceIdempotencyIntegrationTest` | Verifies two sequential calls produce one row and one FCM call. The described worker-crash / DB-failure fault injection and terminal retry state are not executed. |
| ITS-FAM-001 | Verify a care-group invite can be accepted exactly once by the intended user. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareGroupInviteIntegrationTest.java` | `CareGroupInviteIntegrationTest` | Invite persistence is covered; the described concurrent accept/revoke race and downstream permission effects are not executed. |
| ITS-FAM-002 | Verify family permission revocation races safely with dashboard, health, calendar, task and file reads. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/ManageFamilyPermissionIntegrationTest.java` | `ManageFamilyPermissionIntegrationTest` | Only patch-then-get persistence is asserted. The described revocation racing dashboard, health, calendar, task and file reads is not executed. |
| ITS-FAM-003 | Verify removing a member with open assigned tasks preserves a deterministic task state. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java` | `CareTaskAssignmentIntegrationTest` | Assign-then-list is covered; the described assignment policy under permission change is not executed. |
| ITS-CNT-001 | Verify draft-to-approved content is immutable, sanitized and visible only after commit. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/integration/ContentBodySanitizeIntegrationTest.java` | `ContentBodySanitizeIntegrationTest` | Sanitisation passes, but uploadPublicContentImage_endToEnd is @Disabled("Requires real CLOUDINARY_* credentials"), so the described draft-to-approved immutability... |
| ITS-CNT-002 | Verify unpublished content disappears from list/search/lifecycle and future Gemini context. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/UnpublishContentIntegrationTest.java` | `UnpublishContentIntegrationTest` | Unpublish is covered; the described visibility-after-commit and audit assertions of the full lifecycle are not executed. |
| ITS-CHK-001 | Verify approving a template distributes one instance/task set with required audit. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeMobileApp/integration_test/checklist_distribution_e2e_test.dart` | `checklist_distribution_e2e_test.dart` | Requires CHK_E2E_* dart-defines, real access/refresh tokens and a dedicated device. Same Windows CMake / web-unsupported blocker as the other Flutter integration ... |
| ITS-CHK-002 | Verify recommendations rank approved content without exposing health context or returning revoked-context results. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/recommendation/RecommendationPrivacyBoundaryTest.java` | `RecommendationPrivacyBoundaryTest` | Privacy boundary is unit-asserted; the described checklist/recommendation distribution across real journeys is not executed. |
| ITS-EXPENSE-001 | Verify expense CRUD updates owner-only summary with defined currency behavior. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/ExpenseServiceTest.java` | `ExpenseServiceTest` | Expense rules are mocked; no persisted-boundary verification of the described scenario exists. |
| ITS-DEV-001 | Verify sensor/camera/location collection stops safely when OS permission is revoked mid-feature. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeMobileApp/test/features/safety/safety_foreground_coordinator_test.dart` | `safety_foreground_coordinator_test.dart` | Coordinator state machine passes on device-side only. The described device-data ingestion boundary to the backend is not executed. |
| ITS-SAFE-001 | Verify burst/replayed fall signals create one pending event/countdown. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyMonitoringConcurrencyPostgresIntegrationTest.java` | `SafetyMonitoringConcurrencyPostgresIntegrationTest` | Only concurrentEnableReturnsOneCanonicalActiveSession is asserted. The described burst/replayed fall signals, invalid/disabled rejection and "no raw stream stored... |
| ITS-SAFE-002 | Verify cancel/need-help/job timeout at the exact boundary yields one terminal state. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SuspectedFallDetectedHandlerTest.java` | `SuspectedFallDetectedHandlerTest` | Handler logic only; the described countdown/escalation against persisted events is not executed. |
| ITS-SAFE-003 | Verify urgent safety FCM reaches only authorized family and deep-links to an accessible event. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyNotificationDeliveryAdaptersTest.java` | `EmergencyNotificationDeliveryAdaptersTest` | Adapters are mocked; no FCM emulator participates in the described delivery/retry verification. |
| ITS-OTH-001 | Verify search excludes hidden/unapproved/foreign data and remains stable across pages. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/search/SearchIntegrationTest.java` | `SearchIntegrationTest` | Search persistence is covered; the described cross-module scope and permission filtering are not executed. |
| ITS-OTH-002 | Verify only DB-backed readiness is anonymous and reflects DB outage. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/ReadinessEndpointSecurityIntegrationTest.java` | `ReadinessEndpointSecurityIntegrationTest` | Anonymous readiness and actuator denial are verified. The described "readiness reports unavailable when the DB is down" branch is not executed. |
| ITS-OTH-003 | Verify manual seed endpoint is absent outside `dev & !prod` even if the enable property is set. | Pending | Pending | Pending | Pending | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/common/dev/DevDataSeederPostgresIntegrationTest.java` | `DevDataSeederPostgresIntegrationTest` | Seeder runs; the described gated dev-seed policy matrix is not executed. |
| ITS-OTH-004 | Verify a clean database and each supported prior schema migrate to the canonical PostgreSQL schema. | Failed | Failed | Failed | Failed | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/migration/Postgresql18CanonicalSchemaIntegrationTest.java` | `Postgresql18CanonicalSchemaIntegrationTest` | Canonical base-table count assertion failed: expected 57 but was 63. The migration chain itself now completes. |
| ITS-OTH-005 | Verify Nginx/Cloudflare edge preserves auth, CORS, multipart and service failure behavior. | Pending | Pending | Pending | Pending | NONE | `NONE` | The analysis records no existing test and no executable verification exists in the tree. |

---

## 6. Module Statistics

| Module | Passed | Failed | Pending | Total |
| --- | ---: | ---: | ---: | ---: |
| Identity & Security | 1 | 0 | 7 | 8 |
| Care Journeys | 0 | 2 | 4 | 6 |
| Community & Moderation | 0 | 0 | 5 | 5 |
| Expert & Consultation | 1 | 0 | 5 | 6 |
| AI Triage & Gemini | 0 | 0 | 7 | 7 |
| Emergency Map & TrackAsia | 0 | 1 | 2 | 3 |
| Health Records & Storage | 0 | 0 | 3 | 3 |
| Reminders & Notifications | 0 | 0 | 5 | 5 |
| Family Sync | 0 | 0 | 3 | 3 |
| Content & Checklist | 0 | 0 | 4 | 4 |
| Expense & Device Data | 0 | 0 | 2 | 2 |
| Safety Alerts & FCM | 0 | 0 | 3 | 3 |
| Cross-cutting / Other | 0 | 1 | 4 | 5 |
| **Total** | **2** | **4** | **54** | **60** |

> The suggested-case set includes five cross-cutting `ITS-OTH-*` scenarios that do not belong to any of the twelve feature modules named in the reporting template. They are kept in their own row so the module totals still reconcile to 60.

---

## 7. Overall Statistics

| Metric | Value |
| --- | ---: |
| Total Integration Test cases | 60 |
| Passed | 2 |
| Failed | 4 |
| Pending | 54 |
| Pass percentage | 3.3% |

Pending decomposes into 5 BLOCKED (environment or credentials prevent execution) and 49 NOT_EXECUTED (no executable verification of the complete scenario exists yet).
---

## 8. Cases Still Preventing 100% Passed

58 of 60 cases are not Passed. They fall into four causes.

### 8.1 Production/data defects — 4 cases, Failed

| Test Case ID | Exact failing test | Exact error | Category | Recommended next action |
| --- | --- | --- | --- | --- |
| ITS-CARE-001 | `JourneyCanonicalLifecycleIntegrationTest.jrnTcInt004_migrationCreatesSchemaAndHistoryIsReadable` / `.jrnTcInt005_uniqueIndexRejectsDuplicateCanonicalActiveOwner` / `.transitionHistoryRejectsDirectUpdateAndDelete` | `FlywayException: No migration with a target version 20260727010000`; expected unique-index and `audit_events` immutability root causes absent | Migration/data defect + possibly missing guard | Locate or restore migration `20260727010000`; confirm whether `uq_mother_journeys_one_canonical_active` and the `carebridge_reject_mutation()` trigger exist in the canonical schema |
| ITS-CARE-003 | `PostpartumLogPostgresIntegrationTest.listLogs_equalDateAndTimestamp_usesIdAsStablePostgresPageBoundary` | `NullPointerException: Cannot invoke "java.lang.Short.shortValue()"` | Production defect (unboxing a null `Short`) | Find the nullable short column read on the postpartum pagination path and make it null-safe |
| ITS-EMR-001 | `EmergencyTriageLinkPostgresIntegrationTest.retryCandidateQueryUsesStableCreatedAtOrderingAndLimitFifty` | `null value in column "alert_successful_recipient_count" of relation "safety_events" violates not-null constraint` | Schema/fixture mismatch | Either the column needs a DB default or the insert path must supply it; decide with the safety-alert owner |
| ITS-OTH-004 | `Postgresql18CanonicalSchemaIntegrationTest.cleanBootstrapKeepsCanonicalTableCountAndPassesHibernateValidation` | `expected: 57 but was: 63` | Pinned-contract drift | **Do not simply raise 57 to 63.** The pin is a canary; six tables were added while this test could not run. The DB owner must confirm each new table is intended |

### 8.2 Environment / credentials — 5 cases, Pending (BLOCKED)

| Test Case ID | Blocker | Recommended next action |
| --- | --- | --- |
| ITS-SEC-003 | The token exchange needs a Firebase emulator and CareBridge API on `127.0.0.1:8081`; the dedicated runner imports backend `.env` values, which were not used | Provision a disposable database and sanitized test-only `.env`, then run the dedicated emulator suite |
| ITS-CARE-002 | Flutter integration test: Windows desktop build fails in `firebase_cpp_sdk_windows` CMake; web devices unsupported; no mobile emulator | Attach an Android emulator, or pin/patch the Firebase C++ SDK CMake minimum, then supply the dart-define file |
| ITS-CARE-004 | Same as ITS-CARE-002, plus `skip: !apiBacked` | Same |
| ITS-CHK-001 | Same, plus required `CHK_E2E_*` dart-defines and real access/refresh tokens | Same |
| ITS-CNT-001 | `uploadPublicContentImage_endToEnd_persistsPublicAccessModeAndPermanentUrl` is `@Disabled("Requires real CLOUDINARY_* credentials wired into the Testcontainers test context")` | Stand up a Cloudinary sandbox or a local S3/R2-compatible stub, then re-enable |

### 8.3 No executable verification of the complete scenario — 46 cases, Pending

These cases have a cited test that **executes and passes**, but its assertions cover only part of the
Expected Result, or the test is a unit/MockMvc slice that cannot observe the described cross-component
boundary. Per the rule that a unit test is not an integration test, they are not Passed.

They are, in dependency order of effort:

- **Unit or slice evidence only, needs a real-boundary test written** (28): ITS-SEC-004, ITS-SEC-005,
  ITS-SEC-007, ITS-SEC-008, ITS-CARE-005, ITS-CARE-006, ITS-COM-002, ITS-MOD-002, ITS-EXP-001,
  ITS-CHAT-002, ITS-AI-006, ITS-AI-007, ITS-MAP-001, ITS-EMR-002, ITS-HLT-001, ITS-FILE-001,
  ITS-FILE-002, ITS-REM-001, ITS-REM-003, ITS-NOT-001, ITS-CHK-002, ITS-EXPENSE-001, ITS-DEV-001,
  ITS-SAFE-002, ITS-SAFE-003, ITS-AI-002, ITS-AI-003, ITS-AI-005.
- **Real integration test exists but asserts a subset** (15): ITS-SEC-001, ITS-SEC-006, ITS-CARE-001*,
  ITS-COM-001, ITS-MOD-001, ITS-MOD-003, ITS-CON-001, ITS-CON-002, ITS-CHAT-001, ITS-NOT-002,
  ITS-FAM-001, ITS-FAM-002, ITS-FAM-003, ITS-CNT-002, ITS-OTH-001, ITS-OTH-002, ITS-OTH-003.
  These are the cheapest to convert: extend the existing Postgres-backed class with the missing
  assertions, exactly as was done for ITS-SEC-002 (§9).
- **No test at all** (3): ITS-AI-004, ITS-REM-002, ITS-OTH-005 — the analysis itself records `NONE`.

\* ITS-CARE-001 also appears in §8.1 because its cited test currently fails outright.

### 8.4 Complete non-passed action register

This register names every non-passed case individually. For Pending rows, there is no exact failing test because the complete scenario was not executed; the cited test is supporting evidence only.

| Test Case ID | Final | Category and reason | Exact failing/supporting test and error | Recommended next action |
| --- | --- | --- | --- | --- |
| ITS-SEC-001 | Pending | Missing/partial automation: Registration/verify/login persistence is exercised, but the described end-to-end chain "read OTP from the SMTP stub then log in" is not asserted, and "no secret i... | No exact failing test; supporting evidence: `RegistrationIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-SEC-003 | Pending | Environment/credential blocker: Firebase emulator and CareBridge API were absent; dedicated runner imports backend `.env` values and was not used. | No exact failing test; supporting evidence: `federated-emulator.spec.ts`. | Provision the stated disposable non-production infrastructure and rerun the exact scenario. |
| ITS-SEC-004 | Pending | Missing/partial automation: Refresh rotation logic is unit-tested with mocks. The described concurrent race of two refreshes plus a logout against real session rows is not executed. | No exact failing test; supporting evidence: `AuthServiceRefreshTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-SEC-005 | Pending | Missing/partial automation: Filter behaviour per account state is verified against mocked collaborators; the described mutate-then-reuse-JWT flow against persisted account state is not execu... | No exact failing test; supporting evidence: `JwtAuthenticationFilterAccountStateTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-SEC-006 | Pending | Missing/partial automation: Only the location-share branch is verified. The Expected Result also requires health, family and file reads to stop after revocation; none of those endpoints are ... | No exact failing test; supporting evidence: `ConsentRevocationLocationShareConcurrencyPostgresTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-SEC-007 | Pending | Missing/partial automation: Audit service behaviour is mocked. The described fault-injected audit failure rolling back a privileged role mutation over a real transaction is not executed. | No exact failing test; supporting evidence: `AuditServiceImplTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-SEC-008 | Pending | Missing/partial automation: No generated endpoint x role authorization matrix exists. A single filter slice cannot establish the described whole-surface guarantee. | No exact failing test; supporting evidence: `JwtAuthenticationFilterAccountStateTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-CARE-001 | Failed | Production/data defect: Flyway target 20260727010000 does not exist; the canonical unique index and the audit_events immutability trigger did not raise the expected root causes. | `JourneyCanonicalLifecycleIntegrationTest`: Flyway target 20260727010000 does not exist; the canonical unique index and the audit_events immutability trigger did not raise the expected root causes. | Fix the recorded defect, rerun the exact class, then rerun the full suite. |
| ITS-CARE-002 | Pending | Environment/credential blocker: Requires a live API on 127.0.0.1:8080 plus tokens from a gitignored dart-define file. Windows desktop build fails in firebase_cpp_sdk_windows CMake; web devices a... | No exact failing test; supporting evidence: `maternal_metric_p0_e2e_test.dart`. | Provision the stated disposable non-production infrastructure and rerun the exact scenario. |
| ITS-CARE-003 | Failed | Production/data defect: listLogs_equalDateAndTimestamp_usesIdAsStablePostgresPageBoundary throws NullPointerException: Cannot invoke "java.lang.Short.shortValue()". | `PostpartumLogPostgresIntegrationTest`: listLogs_equalDateAndTimestamp_usesIdAsStablePostgresPageBoundary throws NullPointerException: Cannot invoke "java.lang.Short.shortValue()". | Fix the recorded defect, rerun the exact class, then rerun the full suite. |
| ITS-CARE-004 | Pending | Environment/credential blocker: Same device/backend blocker as ITS-CARE-002; the test body is additionally gated behind skip: !apiBacked. | No exact failing test; supporting evidence: `mf03_hub_e2e_test.dart`. | Provision the stated disposable non-production infrastructure and rerun the exact scenario. |
| ITS-CARE-005 | Pending | Missing/partial automation: Dispatch logic is mocked. The described race between vaccination completion and the reminder job over real rows and a controllable clock is not executed. | No exact failing test; supporting evidence: `VaccinationReminderDispatchTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-CARE-006 | Pending | Missing/partial automation: Adapter mapping is unit-tested. The described backend-to-ML container round trip including a stopped model is not executed. | No exact failing test; supporting evidence: `ExerciseCorrectionHttpAdapterTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-COM-001 | Pending | Missing/partial automation: Only image-URL round-tripping is asserted. Expert answer role denial, feed visibility, the reply notification and orphan-image rollback are not executed. | No exact failing test; supporting evidence: `CommunityImageAttachmentIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-COM-002 | Pending | Missing/partial automation: Service layer is mocked, so the described concurrent toggle against a real unique constraint and count is not executed. | No exact failing test; supporting evidence: `CommunityQuestionLikeControllerTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-MOD-001 | Pending | Missing/partial automation: Only report creation is asserted. The described two-moderator claim race, resolution and audit consistency are not executed. | No exact failing test; supporting evidence: `ReportIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-MOD-002 | Pending | Missing/partial automation: Verifies happy-path 201 and repeat-call 409. The described stale/superseded action A-after-B conflict (MOD-029/MOD-030) and audit/target preservation are not exec... | No exact failing test; supporting evidence: `UndoModerationActionIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-MOD-003 | Pending | Missing/partial automation: Claim atomicity is covered; the described malformed-Gemini-output-then-valid-retry branch producing exactly one assessment/case is not executed. | No exact failing test; supporting evidence: `AiClaimAtomicityIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-EXP-001 | Pending | Missing/partial automation: R2 and CompreFace are mocked. The described document upload plus match/no-face/mismatch/timeout matrix against stubs is not executed. | No exact failing test; supporting evidence: `ExpertIdentityVerificationServiceTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-CON-001 | Pending | Missing/partial automation: Three of four Expected clauses are verified (one terminal transition, at most one conversation, losers get CONREQ-005). The expiry race and "notification follows ... | No exact failing test; supporting evidence: `ConsultationRequestAcceptLockConcurrencyIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-CON-002 | Pending | Missing/partial automation: Handoff persistence is covered; the described revoke-then-reread denial and file-access boundary are not executed. | No exact failing test; supporting evidence: `TriageExpertHandoffPostgresIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-CHAT-001 | Pending | Missing/partial automation: Write-lock concurrency is covered. No Firebase/FCM emulator participates, so "one Firebase signal and one notification after commit" and the DCC-005 changed-paylo... | No exact failing test; supporting evidence: `DirectChatWriteLockConcurrencyIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-CHAT-002 | Pending | Missing/partial automation: Contract shapes only. The described Zego token-provider failure (DCC-008), outsider 403 and timeout job are not executed against real call state. | No exact failing test; supporting evidence: `ConversationCallControllerContractTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-AI-002 | Pending | Missing/partial automation: Python-side parity vectors pass. The described cross-runtime comparison actually running the same vectors through the Java boundary is not executed as one gate. | No exact failing test; supporting evidence: `test_rule_registry_parity_v2.py`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-AI-003 | Pending | Missing/partial automation: Python-side auth rejection is covered. The described matrix using a real user/admin JWT issued by the Java service is not executed. | No exact failing test; supporting evidence: `test_triage_v2_api.py`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-AI-004 | Pending | Missing/partial automation: The analysis records no existing test. No timeout/retry/cancel verification of an internal V2 turn exists and building one requires both services running with a c... | No exact failing test; supporting evidence: `NONE`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-AI-005 | Pending | Missing/partial automation: Client filtering is covered. The described publish/withdraw lifecycle driven through the Java internal evidence controller is not executed. | No exact failing test; supporting evidence: `test_evidence_registry_client.py`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-AI-006 | Pending | Missing/partial automation: Write minimisation is unit-tested. The described revoke/delete then later-intake context exclusion is not executed against persisted memory. | No exact failing test; supporting evidence: `HealthMemoryWriteTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-AI-007 | Pending | Missing/partial automation: Some fallback paths are unit-tested. The full described failure matrix (timeout, 429, 5xx, malformed, bad citations) with log-redaction assertions is not executed. | No exact failing test; supporting evidence: `GeminiRagServiceStory69Test`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-MAP-001 | Pending | Missing/partial automation: Verifies the web runtime is the pinned official TrackAsia build. The described success/empty/malformed/timeout/5xx provider matrix and the backend TrackAsiaClient... | No exact failing test; supporting evidence: `trackasia_web_contract_test.dart`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-EMR-001 | Failed | Production/data defect: seedRetryCandidate fails: null value in column "alert_successful_recipient_count" of relation "safety_events" violates not-null constraint. | `EmergencyTriageLinkPostgresIntegrationTest`: seedRetryCandidate fails: null value in column "alert_successful_recipient_count" of relation "safety_events" violates not-null constraint. | Fix the recorded defect, rerun the exact class, then rerun the full suite. |
| ITS-EMR-002 | Pending | Missing/partial automation: Retry-job logic is mocked; the described escalation against real safety events and FCM is not executed. | No exact failing test; supporting evidence: `EmergencyAlertRetryJobTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-HLT-001 | Pending | Missing/partial automation: Sharing rules are mocked. The described cross-endpoint shared-data boundary against persisted grants is not executed. | No exact failing test; supporting evidence: `SharedDataServiceImplTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-FILE-001 | Pending | Missing/partial automation: Storage provider is mocked. The described upload plus DB-failure compensation against a real object store is not executed. | No exact failing test; supporting evidence: `FileServiceImplTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-FILE-002 | Pending | Missing/partial automation: View authorization is sliced with mocks; private-URL leakage and deletion behaviour against real storage are not executed. | No exact failing test; supporting evidence: `FileControllerViewTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-REM-001 | Pending | Missing/partial automation: Worker logic is mocked. The described occurrence generation and idempotent dispatch against real schedule rows are not executed. | No exact failing test; supporting evidence: `ReminderScheduleWorkerTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-REM-002 | Pending | Missing/partial automation: The analysis records no existing test and no executable verification exists in the tree. | No exact failing test; supporting evidence: `NONE`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-REM-003 | Pending | Missing/partial automation: Today-task assembly is unit-tested; the described cross-source unified view over persisted reminders and checklist tasks is not executed. | No exact failing test; supporting evidence: `UnifiedTodayTaskServiceTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-NOT-001 | Pending | Missing/partial automation: Token registration is mocked; the described account-switch and multi-recipient FCM behaviour against an emulator is not executed. | No exact failing test; supporting evidence: `NotificationDeviceTokenServiceTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-NOT-002 | Pending | Missing/partial automation: Verifies two sequential calls produce one row and one FCM call. The described worker-crash / DB-failure fault injection and terminal retry state are not executed. | No exact failing test; supporting evidence: `DirectMessageNotificationServiceIdempotencyIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-FAM-001 | Pending | Missing/partial automation: Invite persistence is covered; the described concurrent accept/revoke race and downstream permission effects are not executed. | No exact failing test; supporting evidence: `CareGroupInviteIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-FAM-002 | Pending | Missing/partial automation: Only patch-then-get persistence is asserted. The described revocation racing dashboard, health, calendar, task and file reads is not executed. | No exact failing test; supporting evidence: `ManageFamilyPermissionIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-FAM-003 | Pending | Missing/partial automation: Assign-then-list is covered; the described assignment policy under permission change is not executed. | No exact failing test; supporting evidence: `CareTaskAssignmentIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-CNT-001 | Pending | Environment/credential blocker: Sanitisation passes, but uploadPublicContentImage_endToEnd is @Disabled("Requires real CLOUDINARY_* credentials"), so the described draft-to-approved immutability... | No exact failing test; supporting evidence: `ContentBodySanitizeIntegrationTest`. | Provision the stated disposable non-production infrastructure and rerun the exact scenario. |
| ITS-CNT-002 | Pending | Missing/partial automation: Unpublish is covered; the described visibility-after-commit and audit assertions of the full lifecycle are not executed. | No exact failing test; supporting evidence: `UnpublishContentIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-CHK-001 | Pending | Environment/credential blocker: Requires CHK_E2E_* dart-defines, real access/refresh tokens and a dedicated device. Same Windows CMake / web-unsupported blocker as the other Flutter integration ... | No exact failing test; supporting evidence: `checklist_distribution_e2e_test.dart`. | Provision the stated disposable non-production infrastructure and rerun the exact scenario. |
| ITS-CHK-002 | Pending | Missing/partial automation: Privacy boundary is unit-asserted; the described checklist/recommendation distribution across real journeys is not executed. | No exact failing test; supporting evidence: `RecommendationPrivacyBoundaryTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-EXPENSE-001 | Pending | Missing/partial automation: Expense rules are mocked; no persisted-boundary verification of the described scenario exists. | No exact failing test; supporting evidence: `ExpenseServiceTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-DEV-001 | Pending | Missing/partial automation: Coordinator state machine passes on device-side only. The described device-data ingestion boundary to the backend is not executed. | No exact failing test; supporting evidence: `safety_foreground_coordinator_test.dart`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-SAFE-001 | Pending | Missing/partial automation: Only concurrentEnableReturnsOneCanonicalActiveSession is asserted. The described burst/replayed fall signals, invalid/disabled rejection and "no raw stream stored... | No exact failing test; supporting evidence: `SafetyMonitoringConcurrencyPostgresIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-SAFE-002 | Pending | Missing/partial automation: Handler logic only; the described countdown/escalation against persisted events is not executed. | No exact failing test; supporting evidence: `SuspectedFallDetectedHandlerTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-SAFE-003 | Pending | Missing/partial automation: Adapters are mocked; no FCM emulator participates in the described delivery/retry verification. | No exact failing test; supporting evidence: `EmergencyNotificationDeliveryAdaptersTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-OTH-001 | Pending | Missing/partial automation: Search persistence is covered; the described cross-module scope and permission filtering are not executed. | No exact failing test; supporting evidence: `SearchIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-OTH-002 | Pending | Missing/partial automation: Anonymous readiness and actuator denial are verified. The described "readiness reports unavailable when the DB is down" branch is not executed. | No exact failing test; supporting evidence: `ReadinessEndpointSecurityIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-OTH-003 | Pending | Missing/partial automation: Seeder runs; the described gated dev-seed policy matrix is not executed. | No exact failing test; supporting evidence: `DevDataSeederPostgresIntegrationTest`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |
| ITS-OTH-004 | Failed | Production/data defect: Canonical base-table count assertion failed: expected 57 but was 63. The migration chain itself now completes. | `Postgresql18CanonicalSchemaIntegrationTest`: Canonical base-table count assertion failed: expected 57 but was 63. The migration chain itself now completes. | Fix the recorded defect, rerun the exact class, then rerun the full suite. |
| ITS-OTH-005 | Pending | Missing/partial automation: The analysis records no existing test and no executable verification exists in the tree. | No exact failing test; supporting evidence: `NONE`. | Create or extend a real-boundary integration test for every missing expected clause, then execute it. |


### 8.5 What ITS-SEC-002 proves about method

ITS-SEC-002 started this task as `Existing` coverage whose test bounded successes to `1..2` and never
counted sessions — its own javadoc stated the "exactly one winner" clause "is not enforced by the
current lock-free design". Rather than trust that note, a new test asserting all three Expected
clauses was written and independently rerun. The current run passed 1/1, so the production behaviour
meets those asserted clauses and the pessimistic comment is stale. The same treatment applied to the 15 cases in the
second bullet of §8.3 is the fastest route to a genuinely green report.

---

## 9. Newly Created Test Files

| File | Test Case ID | Purpose |
| --- | --- | --- |
| `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/OtpConcurrentVerificationSessionIntegrationTest.java` | ITS-SEC-002 | Asserts the complete Expected Result the existing `OtpRaceConditionIntegrationTest` leaves unasserted: exactly one winning verification, the OTP proof consumed exactly once, one `users` row and at most one `auth_sessions` row after a two-thread race on real PostgreSQL |

No existing test was modified. No production source was modified.

---

## 10. Files Changed During Verification

### Changes created during this verification

| File | Kind |
| --- | --- |
| `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/OtpConcurrentVerificationSessionIntegrationTest.java` | New test (untracked) |
| `INTEGRATION_TEST_EXECUTION_REPORT.md` | This report (untracked) |
| `05_Development/CareBridgeWebApp/test-results/.last-run.json` | Playwright run artifact |
| `05_Development/CareBridgeWebApp/test-results/baseline-onboarding.png` | Playwright run artifact |
| `05_Development/CareBridgeWebApp/test-results/federated-emulator-*/error-context.md` (2 dirs) | Playwright failure artifact |
| `05_Development/CareBridgeWebApp/test-results/federated-login-*/error-context.md` (2 dirs) | Playwright failure artifact |
| `artifacts/test-tmp/ai-pytest-round3-20260809-1039/` | Pytest temporary evidence directory |
| `_bmad-output/test-artifacts/traceability-matrix.md` | Traceability/gate analysis generated during verification |
| `_bmad-output/test-artifacts/tea-trace-coverage-matrix-2026-08-09T03-35-00Z.json` | Machine-readable trace evidence |
| `_bmad-output/test-artifacts/e2e-trace-summary.json` | Machine-readable E2E trace summary |
| `_bmad-output/test-artifacts/gate-decision.json` | Quality-gate decision; `FAIL` until scenario coverage is complete |
| `05_Development/CareBridgeMobileApp/android/app/src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java` | Flutter-generated status/line-ending refresh; no content diff |
| `05_Development/CareBridgeMobileApp/ios/Flutter/ephemeral/Packages/FlutterGeneratedPluginSwiftPackage/Package.swift` | Flutter-generated status/line-ending refresh; no content diff |
| `05_Development/CareBridgeMobileApp/macos/Flutter/ephemeral/Packages/FlutterGeneratedPluginSwiftPackage/Package.swift` | Flutter-generated status/line-ending refresh; no content diff |

Flutter tooling also refreshed three generated registrant/package files in the worktree index view,
but `git diff` shows no content delta for them. They are not production behavior changes.

**No production source file was created, modified or deleted by this task.**

### Pre-existing changes (present before this task started)

Tracked modifications carried in from earlier work — listed for completeness, **not** produced by this
verification:

`.gitignore` · 8 × `*.sha256` digest sidecars under `CareBridgeAITriageService/data`,
`CareBridgeAITriageService/tests/data`, `CareBridgeAPI/src/main/resources/triage`,
`CareBridgeAPI/src/test/resources/triage` · `Contracts/triage/artifact_integrity_manifest.json` ·
4 × backend test classes (`BabyJourneyLinkageRemovalMigrationPostgresTest`,
`Postgresql18CanonicalSchemaIntegrationTest`, `DatabaseGate0IntegrationTest`,
`HermeticDatasourceTestcontainersSmokeTest`) · 19 × `CareBridgeMobileApp/lib/**` Dart files ·
3 × `CareBridgeMobileApp/test/**` Dart test files · 3 × `CareBridgeWebApp/src/**` TSX files ·
`Deployment/database/CHECKLIST_RETENTION_SECURITY_RUNBOOK.md`

Staged: 2 × `CareBridgeMobileApp/test/features/aiTriage/goldens/*.png`.

Untracked and pre-existing: `CareBridgeMobileApp/dart_test.yaml`,
`Deployment/database/00_provision_checklist_roles.sql`, `06_Testing/TestReports/TEST_STATUS_2026-08-09.md`,
`INTEGRATION_TEST_ANALYSIS.md`, `artifacts/postman-demo/*`.

The pre-existing set does include production source (the 19 Dart files and 3 TSX files), but those
edits predate this task and were not touched by it.

---

## 11. External Integration Verification

| Provider | What was verified | What could not be | Evidence |
| --- | --- | --- | --- |
| **Firebase Auth emulator** | Emulator-oriented specs were discovered and executed by the ordinary Playwright suite | The ordinary suite did not start the emulator/API. The dedicated runner imports backend `.env`, so it was not executed with potentially real credentials. Token registration, notification dispatch, account switch, family recipients and retry/idempotency remain unverified | `npm run test:e2e` → emulator-oriented cases failed; no emulator-backed PASS claimed |
| **FCM** | Only through mocked adapters (`EmergencyNotificationDeliveryAdaptersTest`, `NotificationDeviceTokenServiceTest`, `DirectMessageNotificationServiceIdempotencyIntegrationTest` — the last asserts one row + one FCM call) | No FCM emulator participates in any executed test; no real dispatch, retry or multi-recipient behaviour was observed | Round 2/3 Java results |
| **Gemini** | Deterministic Python-side fallbacks pass (1013 pytest); `GeminiRagServiceStory69Test` covers some fallback branches as a plain unit test | The full failure matrix — timeout, 429, 5xx, malformed output, bad citations, prompt/key redaction — is not executed end to end | `pytest` + `GeminiRagServiceStory69Test` 9r 0F |
| **TrackAsia** | `trackasia_web_contract_test.dart` passes: the web runtime uses the pinned official build with no stub override | Backend `TrackAsiaClient` success / empty / malformed / timeout / 5xx contract is not executed anywhere | `flutter test test/features/emergency/trackasia_web_contract_test.dart` → 2 passed |
| **Cloudinary** | Nothing executed | `uploadPublicContentImage_endToEnd` is `@Disabled` pending real `CLOUDINARY_*` credentials | Surefire skip count 1 in `ContentBodySanitizeIntegrationTest` |
| **Cloudflare R2** | Nothing executed | `FileServiceImplTest` mocks the storage provider; no upload, DB-failure compensation, private authorization, deletion or URL-leakage check runs against a real or emulated object store | `FileServiceImplTest` 15r 0F (unit) |
| **CompreFace** | Nothing executed | `ExpertIdentityVerificationServiceTest` mocks the pipeline; match / no-face / mismatch / timeout are not exercised against a container | `ExpertIdentityVerificationServiceTest` 5r 0F (unit) |
| **ZEGOCLOUD** | Nothing executed | `ConversationCallControllerContractTest` is a shape-only contract test; `DCC-008` provider failure and scoped join credentials are not exercised | `ConversationCallControllerContractTest` 4r 0F |
| **SMTP / Gmail** | Mocked via `@MockitoBean EmailService` in the OTP tests | No SMTP stub captures a real message body, so "read the OTP from the stub" in ITS-SEC-001 is not executed | Round 2/3 Java results |

**No real provider sandbox was exercised, and no production credential was used.** The backend `.env`
holds live Supabase, TrackAsia and Cloudinary secrets; starting the API with it would have pointed
these tests at the production database, so it was deliberately not started.

---

## 12. Final Verdict

**NOT YET 100% Passed.**

| | |
| --- | ---: |
| Total Integration Test cases | 60 |
| Passed | 2 |
| Failed | 4 |
| Pending | 54 |
| Pass percentage | 3.3% |

58 cases remain before the spreadsheet can legitimately read 100% Passed:

1. **4 Failed** — real defects in journey migrations, postpartum pagination, the `safety_events`
   not-null column, and canonical schema drift. These need code or data fixes, not test edits.
2. **5 Pending (BLOCKED)** — need a non-production backend, a mobile emulator or a working Windows
   Flutter build, and Cloudinary test credentials.
3. **49 Pending (NOT_EXECUTED)** — the described cross-component behaviour has never been executed.
   46 have partial supporting evidence; 3 have no test at all.

The full backend suite is also red overall (`BUILD FAILURE`, 13 failures + 15 errors across 20
classes), which is a separate gate from the 60 suggested cases and must be green before any release
claim.

Reporting this set as green today would misstate the evidence: 46 of the 60 rows would be resting on
unit or MockMvc-slice tests that never cross the boundary the case describes.

---

Integration-test execution verification completed.

Total cases: 60 | Passed: 2 | Failed: 4 | Pending: 54
