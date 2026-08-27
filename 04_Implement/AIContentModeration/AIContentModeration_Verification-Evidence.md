> [!IMPORTANT]
> Historical verification evidence for `UC-AD-17` and `UC-AD-19`; this is not a canonical current Test-Spec. Current code and the canonical code-first specifications override conflicts.

# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Test Specification — AI-assisted Content Moderation (Gemini)

| Field           | Value                          |
| --------------- | ------------------------------ |
| **Document ID** | `CB-MOD-IMP-016-TS`            |
| **Version**     | `1.0`                          |
| **Date**        | `2026-07-26`                   |
| **Status**      | `Approved`                     |
| **Author**      | `AI Agent — Claude (Fable)`    |
| **Approved by** | `[x] HuyND — 2026-07-26 (uỷ quyền trước trong đề bài, xem TDS header)` |

## CHANGELOG
| Ngày | Người thực hiện | Nội dung |
|------|-----------------|----------|
| 2026-07-26 | AI Agent — Claude | Tạo lần đầu. Mapping 22 backend scenarios của đề bài → test class cụ thể. |
| 2026-07-26 | AI Agent — Claude (Dev Agent) | Kết quả cuối: 15 test class mới / 93 test PASS. Full suite (`./mvnw clean test`): 2932 tests — mọi lỗi thuộc phạm vi thay đổi này đã xanh sau khi cập nhật 6 test class hiện có (ReportServiceImplTest, CommunityQuestion/Answer/QuestionEdit/ContentApproval ServiceImplTest, CommunitySafetyPolicyTest — thêm mock `AiScanEnqueueService` hoặc đổi stub sang `existsByReporterUserIdAndTargetIdAndStatusIn`). Còn lại NGOÀI phạm vi: 1 failure pre-existing (V1-hash pin, hỏng từ commit `faef9640`), ~77 lỗi Testcontainers (Docker off), 1 lỗi `TriageIntegrationTest` do thay đổi triage CHƯA COMMIT của một phiên làm việc song song khác trên cùng working tree (TriageService.java +145 dòng không thuộc phiên này — đã xác minh vị trí stacktrace nằm trong hunk của họ). Web: `npx tsc -b` PASS, `npm run build` PASS, vitest 45/45 PASS. Migration validate thật trên Postgres 18 scratch DB: sạch + idempotent. |

## CB-MOD-IMP-017 — Test bổ sung (2026-07-26, consolidation)

| Yêu cầu VII | Test | Kết quả |
|---|---|---|
| 1. Assessment persist 1 lần cùng matches_jsonb | `AiScanResultRecorderTest::violationSuccess_persistsMatchesInline_createsAndLinksCase` (round-trip typed qua mapper thật) | 🟢 |
| 2. Không match → `[]` | `AiScanResultRecorderTest::safeSuccess_persistsEmptyMatchesArray_noCase_jobCompleted` | 🟢 |
| 3. Malformed matches JSON | ghi: serialize fail → IllegalStateException (retryable); đọc: `malformedStoredMatches_degradeToEmptyTypedList`; model JSON sai → `AiVerdictParserTest` (retryable, không SAFE); DB CHECK jsonb_typeof='array' | 🟢 |
| 4. Evidence không tồn tại bị loại | `AiVerdictParserTest::fabricatedEvidence_isDropped_realEvidenceKept` (giữ nguyên) | 🟢 |
| 5. API deserialize typed list | recorder round-trip + `AiAssessmentModeratorServiceTest::assessmentView_*` | 🟢 |
| 6/7. AGREE/DISAGREE cập nhật moderation_cases | `AiAssessmentModeratorServiceTest::feedbackAgree_updatesModerationCaseColumns`, `::feedbackDisagree_appendsModerationEventWithSanitizedPayload` | 🟢 |
| 8/9. Feedback lần 2 thay current, cả 2 còn trong events | `::secondFeedback_replacesCurrent_andRecordsPreviousDecisionInHistory` | 🟢 |
| 10/11. Không feedback assessment chưa attach / case người khác đang review | `::feedback_onUnattachedAssessment_isRejected` (AIM-013), `::feedback_whileClaimedByAnotherModerator_isRejected` (MOD-038) | 🟢 |
| 12. RBAC MODERATOR | `AiModerationModeratorControllerSecurityTest` (giữ nguyên, vẫn xanh) | 🟢 |
| 13/14. Không còn SQL/reference tới 2 bảng cũ | grep sweep: 0 runtime reference (chỉ migration + comment); entity/repo đã xóa → compile-time guarantee | 🟢 |
| 15. Benign symptom không tạo case | `CommunitySafetyPolicyTest` (reflection pins — hook đã xóa) + `AiModerationDecisionPolicyTest::safeClassification_createsNoCase` | 🟢 |
| 16. Dangerous advice vẫn review | `AiModerationDecisionPolicyTest::dangerousMedicalAdvice_createsHighPriorityCase` | 🟢 |
| 17. Triage safety tests | không sửa file triage nào; graph affected-flows = 0 flow triage | 🟢 (xem caveat phiên song song ở changelog) |
| 18. AI moderation tests cập nhật | 139-test focused run PASS (xem bên dưới) | 🟢 |

Migration tests (Postgres 18 scratch, Docker/Testcontainers không khả dụng):
1. Upgrade từ schema có dữ liệu (3 match, 3 feedback, 2 case, 2 assessment) — PASS.
2. 3 match row → 2+1 JSON object, đúng thứ tự + policy snapshot — PASS.
3. Latest feedback đúng trên moderation_cases (case1=DISAGREE mới nhất, case2 qua assessment-fallback) — PASS.
4. 3/3 feedback có moderation_event (deterministic id; pre-inserted event không nhân đôi → retry-idempotent) — PASS.
5. 2 bảng cũ không còn (`to_regclass` NULL) — PASS.
6. Fresh chain baseline→V…100000→V…150000 chạy sạch — PASS.
7. CHECK `chk_ai_assessment_matches_array` + `chk_moderation_cases_ai_feedback_decision` + FK tồn tại — PASS.
8. Reconciliation mismatch (sabotage trigger) → RAISE `AI_CONSOLIDATION_LATEST_FEEDBACK_MISMATCH` → rollback, bảng cũ nguyên vẹn, 0 event — PASS.

Web: `npx tsc -b` PASS, `npm run build` PASS, vitest 45/45 PASS (chỉ thêm 2 field optional vào type `AiAssessmentMatch`; component không đổi).

## 1. Test harness (theo convention repo — đã kiểm chứng)
- Unit: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), mock ở **client interface** (không WireMock — repo không có).
- Controller/RBAC: `@WebMvcTest` + `@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})` + `@MockitoBean` + `@WithMockUser(username = "<uuid>", roles = ...)`.
- Persistence/atomic claim: `@DataJpaTest` trên H2 `MODE=PostgreSQL` (precedent: `AuthServiceRegisterTest`).
- Không gọi Gemini thật; `GeminiModerationClient`/`GeminiClient` được mock. HTTP-level test cho `GeminiHttpClient` dùng RestClient + custom `ClientHttpRequestFactory` giả lập response.

## 2. Test matrix (22 scenario bắt buộc)

| # | Scenario (đề bài §7) | Test class :: case | TDD Phase | Current Status |
|---|----------------------|--------------------|-----------|----------------|
| 1 | Gemini disabled không gọi HTTP | `GeminiHttpClientTest::disabled_throwsUnavailable_withoutHttpCall`, `GeminiModerationClientTest::disabled_reportsDisabledState_noHttp` | 🟢 GREEN | 🟢 Passing |
| 2 | Missing API key → configuration state rõ ràng | `GeminiModerationClientTest::missingKey_reportsNotConfigured_noHttp` | 🟢 GREEN | 🟢 Passing |
| 3 | Success parse structured response | `GeminiModerationClientTest::success_parsesStructuredJson` | 🟢 GREEN | 🟢 Passing |
| 4 | Malformed JSON không được coi là SAFE | `GeminiModerationClientTest::malformedJson_throwsRetryable_notSafe`; `AiScanProcessingServiceTest::malformedResponse_marksRetry_notSafe_noCase` | 🟢 GREEN | 🟢 Passing |
| 5 | Timeout/429/5xx retry đúng | `GeminiModerationClientTest::http429_isRetryable`, `::http503_isRetryable`, `::timeout_isRetryable`; `AiScanProcessingServiceTest::retryableFailure_requeuesWithBackoff` | 🟢 GREEN | 🟢 Passing |
| 6 | 4xx config/model không retry vô hạn | `GeminiModerationClientTest::http404Model_isConfigurationError`, `::http401_isConfigurationError`; `AiScanProcessingServiceTest::configurationError_failsJobImmediately` | 🟢 GREEN | 🟢 Passing |
| 7 | API key không lộ trong log/error/response | `GeminiModerationClientTest::errors_neverContainApiKey`; `AiModerationAdminControllerTest::status_neverExposesKey` | 🟢 GREEN | 🟢 Passing |
| 8 | Benign symptom description không bị coi là violation | `AiModerationDecisionPolicyTest::safeClassification_createsNoCase` (+ prompt builder test khẳng định guidance phân biệt symptom-vs-advice) | 🟢 GREEN | 🟢 Passing |
| 9 | Dangerous medical advice / giả mạo chuyên gia → review | `AiScanProcessingServiceTest::dangerousAdvice_createsPriorityCase` | 🟢 GREEN | 🟢 Passing |
| 10 | Spam/harassment → automated case | `AiScanProcessingServiceTest::spamViolation_createsAutomatedCase` | 🟢 GREEN | 🟢 Passing |
| 11 | Prompt injection không điều khiển classifier | `AiModerationPromptBuilderTest::userContent_isDelimited_asUntrustedData`; `AiModerationDecisionPolicyTest::inconsistentRecommendation_ignored_serverDecides` | 🟢 GREEN | 🟢 Passing |
| 12 | SAFE không tạo case | `AiScanProcessingServiceTest::safeResult_persistsAssessment_noCase` | 🟢 GREEN | 🟢 Passing |
| 13 | FAILED không tạo violation case | `AiScanProcessingServiceTest::exhaustedRetries_failedAssessment_noCase` | 🟢 GREEN | 🟢 Passing |
| 14 | Idempotent rescan không duplicate | `AiScanProcessingServiceTest::duplicateCompletedAssessment_skipsGeminiAndCase`; `AiScanJobRepositoryTest::uniqueActiveJob_perTargetHash` | 🟢 GREEN | 🟢 Passing |
| 15 | Content update hash mới → scan mới | `AiScanEnqueueServiceTest::changedContent_newJob` | 🟢 GREEN | 🟢 Passing |
| 16 | USER report không bị overwrite source | `AiScanProcessingServiceTest::existingUserCase_attachesAssessment_keepsUserSource` | 🟢 GREEN | 🟢 Passing |
| 17 | Claim concurrency chỉ 1 người thành công | `ContentReportClaimRepositoryTest::claim_atomic_secondClaimFails` (@DataJpaTest guarded UPDATE) + `ModerationServiceImplTest::claimReport_alreadyClaimed_throwsConflict` | 🟢 GREEN | 🟢 Passing |
| 18 | Transition hợp lệ/không hợp lệ | `ModerationServiceImplTest::resolve_fromInReviewByClaimer_ok`, `::resolve_fromInReviewByOther_conflict`, `::claim_nonPending_rejected`, `::revert_inReview_rejected`, `::release_byNonClaimer_rejected` | 🟢 GREEN | 🟢 Passing |
| 19 | RBAC | `AiModerationAdminControllerSecurityTest` (SYSTEM_ADMIN ok, MODERATOR/khác 403), `ModerationClaimControllerSecurityTest` (MODERATOR ok, SYSTEM_ADMIN/MOTHER 403) | 🟢 GREEN | 🟢 Passing |
| 20 | Audit không chứa raw sensitive text | `AiScanProcessingServiceTest::auditDetails_containOnlyIdsAndCodes` | 🟢 GREEN | 🟢 Passing |
| 21 | Existing moderation tests vẫn chạy | `./mvnw test` full-suite regression | 🟢 GREEN | 🟢 Passing (0 regression mới) |
| 22 | Triage red-flag safety tests không yếu đi | full-suite regression (không sửa file nào của triage/policy/engine) | 🟢 GREEN | 🟢 Passing |

Bổ sung: `AiPolicyServiceTest` (version++ khi đổi guidance, system-default không delete, validation), `AiPolicySetServiceTest` (hash ổn định), `GeminiHttpClientTest` (real generate cho RAG/triage giữ contract), context-load smoke: `AiModerationConfigSmokeTest` (GEMINI_ENABLED=false và true-không-key đều start).

## 3. Web verification
- `npx tsc -b` + `npm run build` PASS (vitest hiện không có test moderation nào từ trước; không thêm bắt buộc).

## 4. Red Gate — ghi nhận trung thực
Do quy mô end-to-end (36 file backend mới/sửa), phiên này KHÔNG chạy red-gate từng test một cách nghiêm ngặt (viết test fail trước rồi mới implement). Thực tế: implementation viết trước theo TDS, 15 test class (93 test) viết ngay sau đó và chạy xanh sau 3 vòng sửa lỗi thật do test phát hiện: (1) HttpServer single-thread executor gây timeout chéo test → thêm cached thread pool; (2) `@Valid` binding chạy trước method-security → security test phải gửi body hợp lệ; (3) `@SpringBootTest` cần mock EmailService/SmsService theo convention repo; (4) shared H2 `testdb` bị drop khi context cache evict → test atomicity dùng DB H2 riêng; (5) `DATASOURCE_DIRECT_CONFIGURATION_INCOMPLETE` guard đòi đủ url+username+password. Các cột "TDD Phase 🟢/Current Status 🟢" ở Section 2 phản ánh kết quả CHẠY THẬT (93/93 pass, xem §5), không phải red-green đúng nghi thức.

## 5. Kết quả chạy thật (2026-07-26)
- Narrow: `./mvnw test -Dtest='com.carebridge.backend.aimoderation.*Test,ClaimReportWorkflowTest,ClaimReportControllerSecurityTest'` → **93/93 PASS**.
- Migration validation trên Postgres 18 (scratch DB `carebridge_mig_check`): baseline + `V20260726100000__ai_content_moderation.sql` apply sạch, chạy lại idempotent (11 policies không nhân đôi), 2 cột mới trên `moderation_cases` hiện diện. Scratch DB đã drop.
- Full suite regression (rows 21/22): lần chạy cuối ghi nhận ở CHANGELOG — các lỗi còn lại chỉ gồm (a) ~77 lỗi Testcontainers do Docker không chạy trên máy này (pre-existing, xuất hiện cả khi không có thay đổi) và (b) 1 failure pre-existing `ChecklistTemplateMigrationTest.uc82_69_int_005_v1RemainsByteIdentical` (hash pin của `V1__init_schema.sql` đã lệch từ commit `faef9640`, file không bị phiên này chạm vào).
