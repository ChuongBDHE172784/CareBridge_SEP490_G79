# CareBridge — Bản đồ Unit Test (Feature → Test File)

> Tài liệu này liệt kê **tính năng → file test tương ứng** cho cả 3 tầng: Backend (JUnit 5 + Mockito), Mobile (Flutter `flutter_test`), Web Portal (Vitest + React Testing Library).
> File tổng hợp kết quả: `06_Testing/UnitTesting/Report5_Unit Test_CareBridge.xlsx`.

| | |
|---|---|
| **Dự án** | CareBridge — SEP490_G79 |
| **Ngày cập nhật** | 2026-08-07 |
| **Branch** | `HuyND` |

---

## 1. Cách chạy test

```bash
# Backend — JUnit 5 / Mockito / Spring Boot Test
cd 05_Development/CareBridgeAPI
./mvnw test                                  # toàn bộ
./mvnw test -Dtest='ClassName'               # 1 class

# Mobile — Flutter
cd 05_Development/CareBridgeMobileApp
flutter test
flutter test test/features/<area>/           # 1 nhóm

# Web Portal — Vitest
cd 05_Development/CareBridgeWebApp
npm test                                     # = vitest run
npx vitest run src/features/<area>
```

## 2. Phạm vi & quy ước

**Nằm trong phạm vi Unit Test report:** mọi test chạy hoàn toàn trong bộ nhớ — service/policy/mapper/validator test bằng Mockito, `@WebMvcTest` controller slice, widget test Flutter, và component/service test Vitest.

**Loại trừ khỏi Unit Test report** (nhưng vẫn liệt kê bên dưới để tra cứu): các class có hậu tố
`*IntegrationTest`, `*PostgresTest`, `*PostgresIntegrationTest`, `*EmbeddedPostgresTest`, `*SmokeTest`.
Đây là **integration test** cần Docker/Testcontainers hoặc embedded PostgreSQL; khi Docker tắt chúng báo `ExceptionInInitializerError` / `NoClassDefFoundError` — lỗi môi trường, không phải lỗi code. Chúng thuộc báo cáo Integration/E2E, không thuộc Report 5 (Unit Test).

**Số liệu sau khi hoàn thiện (chạy thật ngày 2026-08-08)**

| Tầng | Test file | Test case | Passed | Failed | Skipped |
|---|---:|---:|---:|---:|---:|
| Backend (unit) | 530 | 3.760 | 3.757 | 0 | 3 |
| Mobile (Flutter) | 156 | 934 | 934 | 0 | 0 |
| Web Portal (Vitest) | 31 | 137 | 137 | 0 | 0 |
| **Tổng (Report 5)** | **717** | **4.831** | **4.828** | **0** | **3** |
| Backend (integration) | 140 | 271 | 62 | 142 | 67 |

Lệnh xác nhận cuối: `./mvnw test` → `Tests run: 4031, Failures: 0, Errors: 142, Skipped: 70`.

> 142 "errors" ở dòng integration là lỗi **môi trường** (Docker/Testcontainers không chạy trên máy
> build), không phải lỗi code, và **không** được tính vào Report 5.
> 3 test `skipped` là các ca có điều kiện môi trường (`@EnabledIf`), không phải test hỏng.

---

## 3. Tính năng chung (Mobile + Web + Backend)

### 3.1 Đăng ký / Đăng nhập / Xác thực (UC Auth)

| Tầng | File test |
|---|---|
| Backend | `src/test/java/com/carebridge/backend/security/service/AuthServiceRegisterTest.java` |
| Backend | `.../security/service/AuthServiceLoginTest.java` |
| Backend | `.../security/service/AuthServiceVerifyOtpTest.java` |
| Backend | `.../security/service/AuthServiceResendOtpTest.java` |
| Backend | `.../security/service/AuthServiceRefreshTest.java` |
| Backend | `.../security/service/AuthServiceGetProfileTest.java` |
| Backend | `.../security/service/AuthServiceDeactivateTest.java` (trong `security/AuthServiceDeactivateTest.java`) |
| Backend | `.../security/service/FederatedAuthServiceTest.java` — đăng nhập Google/Firebase |
| Backend | `.../security/service/FederatedIdentityLinkAuditPolicyTest.java` |
| Backend | `.../security/controller/FederatedAuthControllerTest.java` |
| Backend | `.../security/policy/PasswordComplexityPolicyTest.java` — đổi/đặt lại mật khẩu |
| Backend | `.../security/policy/RateLimitPolicyTest.java`, `RateLimitPolicyResendTest.java` |
| Backend | `.../security/jwt/JwtTokenProviderSecretValidationTest.java`, `TestJwtKeyEnvironmentPostProcessorTest.java` |
| Backend | `.../security/filter/JwtAuthenticationFilterAccountStateTest.java` — chặn tài khoản bị khoá |
| Backend | `.../security/mapper/UserMapperTest.java` |
| Backend | `.../security/config/SecurityConfigCorsTest.java`, `SecurityConfigCorsFilterChainTest.java` |
| Backend | `.../identity/repository/TokenBlacklistRepositoryTest.java`, `.../identity/service/impl/SessionServiceImplTest.java` |
| Backend | `.../common/validation/VietnamesePhoneNumbersTest.java` |
| Mobile | `test/features/auth/password_login_test.dart` |
| Mobile | `test/features/auth/federated_login_test.dart`, `federated_registration_test.dart` |
| Mobile | `test/features/auth/federated_auth_service_test.dart`, `federated_auth_failure_test.dart` |
| Mobile | `test/features/auth/registration_error_message_test.dart` |
| Mobile | `test/features/auth/logout_confirmation_screen_test.dart` |
| Mobile | `test/features/auth/blocked_account_screen_test.dart` |
| Mobile | `test/features/auth/linked_account_test.dart`, `linked_accounts_screen_test.dart` |
| Mobile | `test/features/auth/welcome_screen_accessibility_test.dart` |
| Mobile | `test/core/auth/auth_state_atomicity_test.dart`, `auth_state_logging_test.dart` |
| Mobile | `test/core/storage/token_storage_test.dart` |
| Mobile | `test/core/network/api_client_explicit_session_test.dart`, `api_client_session_race_test.dart` |
| Mobile | `test/core/network/account_block_parser_test.dart`, `api_exception_test.dart` |
| Mobile | `test/core/firebase/firebase_bootstrap_test.dart` |
| Mobile | `test/core/routes/app_router_test.dart` |
| Web | `src/features/auth/services/authApi.test.ts` |
| Web | `src/features/auth/pages/BlockedAccountPage.test.tsx` |
| Web | `src/features/auth/pages/ExpertRegisterPage.test.tsx` |
| Web | `src/shared/api/apiClient.test.ts` — session isolation + maintenance routing |
| Backend (integration) | `.../security/integration/{Login,Logout,RegisterAccount,VerifyOtp,OtpRaceCondition,Registration,AuthProfile,FederatedLogin,FederatedRegistration,FederatedIdentityLink,Readiness*}IntegrationTest.java` |

### 3.2 Khảo sát cá nhân hoá (Mother) & Gợi ý nội dung

| Tầng | File test |
|---|---|
| Backend | `.../recommendation/RecommendationProfileValidatorTest.java` — bộ câu hỏi khảo sát |
| Backend | `.../recommendation/RecommendationServiceTest.java` |
| Backend | `.../recommendation/RecommendationRankerTest.java` |
| Backend | `.../recommendation/RecommendationPolicyTest.java`, `RecommendationMetadataPolicyTest.java` |
| Backend | `.../recommendation/RecommendationPrivacyBoundaryTest.java` |
| Backend | `.../recommendation/RecommendationControllerTest.java` |
| Backend | `.../recommendation/RecommendationJourneyTransitionListenerTest.java` |
| Mobile | `test/features/recommendation/recommendation_questionnaire_test.dart` |
| Mobile | `test/features/recommendation/recommendation_profile_screen_test.dart` |
| Mobile | `test/features/recommendation/recommendation_model_test.dart` |
| Mobile | `test/features/home/mother_home_recommendation_test.dart` |
| Web | `src/features/contentManagement/pages/recommendationMetadata.test.ts` |
| Backend (integration) | `.../recommendation/Recommendation{Content,Migration,DevSeeder}EmbeddedPostgresTest.java` |

### 3.3 Hồ sơ cá nhân & Cài đặt

| Tầng | File test |
|---|---|
| Backend | `.../profile/service/ProfileServiceImplTest.java` |
| Backend | `.../profile/dto/UpdateProfileRequestValidationTest.java` |
| Backend | `.../privacy/service/PrivacySettingsServiceTest.java` |
| Backend | `.../privacy/controller/PrivacySettingsControllerTest.java` |
| Backend | `.../consent/ConsentServiceImplGrantTest.java`, `ConsentServiceImplRevokeTest.java`, `ConsentServiceImplListTest.java` |
| Backend (integration) | `.../profile/ProfileIntegrationTest.java`, `.../settings/SettingsJsonbConcurrencyPostgresTest.java`, `.../consent/ConsentRevocationLocationShare*PostgresTest.java` |

### 3.4 Thông báo (Notification)

| Tầng | File test |
|---|---|
| Backend | `.../notification/NotificationSendServiceTest.java` |
| Backend | `.../notification/NotificationViewServiceTest.java` |
| Backend | `.../notification/NotificationMarkAsReadServiceTest.java` |
| Backend | `.../notification/NotificationPreferenceServiceImplTest.java` |
| Backend | `.../notification/NotificationDeviceTokenServiceTest.java` |
| Backend | `.../notification/NotificationControllerContractTest.java` |
| Backend | `.../notification/ContentReviewNotificationServiceTest.java` |
| Backend | `.../notification/VaccinationNotificationServiceTest.java` |
| Backend | `.../notification/service/ReminderNotificationServiceTest.java` |
| Backend | `.../notification/service/CommunityReplyNotificationServiceTest.java` |
| Backend | `.../notification/service/ConsultationNotificationServiceTest.java` |
| Backend | `.../notification/service/impl/DirectMessageNotificationServiceImplTest.java` |
| Backend | `.../notification/service/impl/ConsultationRequestNotificationServiceImplTest.java` |
| Backend | `.../notification/service/impl/FcmServiceOverloadTest.java` |
| Backend | `.../notification/config/FirebaseConfigTest.java` |
| Mobile | `test/features/notification/notification_model_test.dart` |
| Mobile | `test/features/notification/reminder_notification_routing_test.dart` |
| Mobile | `test/core/notifications/fcm_service_test.dart` |
| Mobile | `test/features/home/family_member_home_notification_test.dart` |
| Backend (integration) | `.../notification/NotificationCanonical{Migration,Schema}IntegrationTest.java`, `.../notification/service/**/*IntegrationTest.java` |

### 3.5 Upload / Quản lý file & ảnh

| Tầng | File test |
|---|---|
| Backend | `.../file/FileServiceImplTest.java`, `FileServiceViewTest.java`, `FileServiceDeleteTest.java` |
| Backend | `.../file/FileControllerUploadTest.java`, `FileControllerViewTest.java`, `FileControllerDeleteTest.java` |
| Backend | `.../file/policy/FileAccessPolicyTest.java`, `FileDeletePolicyTest.java` |
| Backend | `.../file/CloudinaryStorageServiceTest.java`, `config/R2StorageServiceTest.java`, `StorageServiceResolverTest.java` |
| Backend | `.../file/job/PublicContentImageCleanupJobTest.java` |

---

## 4. Mobile — Role MOTHER

### 4.1 Cộng đồng (xem / đăng / trả lời / quản lý / báo cáo câu hỏi)

| Tầng | File test |
|---|---|
| Backend | `.../community/service/CommunityQuestionServiceImplTest.java` — đăng câu hỏi |
| Backend | `.../community/service/CommunityQuestionDetailServiceImplTest.java` |
| Backend | `.../community/service/CommunityQuestionEditServiceImplTest.java` |
| Backend | `.../community/service/CommunityQuestionDeleteServiceImplTest.java` |
| Backend | `.../community/service/CommunityQuestionSearchServiceImplTest.java` |
| Backend | `.../community/service/CommunityQuestionLikeServiceImplTest.java` |
| Backend | `.../community/service/CommunityAnswerServiceImplTest.java` — trả lời |
| Backend | `.../community/service/CommunityAnswerLikeServiceImplTest.java` |
| Backend | `.../community/service/CommunityBookmarkServiceImplTest.java` |
| Backend | `.../community/service/CommunityFeedServiceImplTest.java` |
| Backend | `.../community/service/TopicFollowServiceImplTest.java` |
| Backend | `.../community/service/CommunityAuthorDisplayResolverTest.java` — ẩn danh |
| Backend | `.../community/controller/Community{Question,QuestionEdit,QuestionDelete,QuestionLike,Answer,Feed}ControllerTest.java` |
| Backend | `.../community/mapper/Community{Question,Answer,Feed}MapperTest.java` |
| Backend | `.../community/policy/CommunitySafetyPolicyTest.java` |
| Backend | `.../community/CommunityTopicServiceImplTest.java`, `CommunityTopicControllerTest.java`, `CommunityTopicSearchServiceImplTest.java` |
| Backend | `.../community/util/SlugGeneratorTest.java` |
| Backend | `.../content/report/ReportServiceImplTest.java`, `ReportControllerTest.java` — báo cáo vi phạm |
| Mobile | `test/features/community/community_feed_screen_test.dart` |
| Mobile | `test/features/community/post_answer_screen_test.dart` |
| Mobile | `test/features/community/community_question_topics_test.dart` |
| Mobile | `test/features/community/community_image_model_test.dart`, `community_image_picker_field_test.dart` |
| Mobile | `test/features/community/models/community_stage_model_test.dart` |
| Mobile | `test/features/community/checklist_assignment_context_test.dart` |
| Backend (integration) | `.../community/Community{TopicIntegration,ImageAttachmentIntegration}Test.java`, `.../content/report/ReportIntegrationTest.java` |

### 4.2 Bài tập cho mẹ (camera real-time kiểm tra tư thế)

| Tầng | File test |
|---|---|
| Backend | `.../exercise/ExerciseSessionServiceTest.java` |
| Backend | `.../exercise/ExerciseCompleteSessionServiceTest.java` |
| Backend | `.../exercise/ExercisePauseResumeServiceTest.java` |
| Backend | `.../exercise/ExerciseQueryServiceTest.java`, `ExerciseDetailQueryServiceTest.java` |
| Backend | `.../exercise/ExerciseSafetyCheckServiceTest.java`, `SafetyCheckPolicyTest.java` |
| Backend | `.../exercise/PostureAnalysisServiceTest.java` |
| Backend | `.../exercise/GeometricPostureRulesTest.java` |
| Backend | `.../exercise/PostureFeedbackMessagesTest.java` |
| Backend | `.../exercise/PostureConfigServiceTest.java`, `CreatePostureConfigRequestValidationTest.java` |
| Backend | `.../exercise/policy/PostureSessionTrackerTest.java` |
| Backend | `.../exercise/inference/PostureInferenceConfigResolverTest.java`, `ExerciseCorrectionHttpAdapterTest.java` |
| Backend | `.../exercise/ExerciseMapperTest.java`, `entity/CanonicalObservationMappingTest.java` |
| Backend | `.../exercise/Admin{Exercise,PostureConfig}Controller{,Security}Test.java`, `AdminExerciseServiceTest.java` |
| Backend | `.../exercise/Exercise{ControllerDetail,SessionPostureEvents}SecurityTest.java` |
| Mobile | `test/features/exercise/mother_exercise_screen_test.dart` |
| Mobile | `test/features/exercise/exercise_feedback_analyzer_test.dart` |
| Mobile | `test/features/exercise/exercise_service_posture_event_test.dart` |
| Mobile | `test/features/exercise/posture_event_model_test.dart`, `posture_event_streamer_test.dart` |
| Mobile | `test/features/exercise/posture_camera_js_web_test.dart`, `posture_camera_overlay_web_test.dart` |
| Backend (integration) | `.../exercise/Exercise{Detail,PostureConfigLifecycle}IntegrationTest.java`, `Exercise*EmbeddedPostgresTest.java` |

### 4.3 Nội dung & FAQ (do Content Admin đăng, System Admin duyệt)

| Tầng | File test |
|---|---|
| Backend | `.../content/unit/ContentServiceImplTest.java`, `ContentControllerTest.java` |
| Backend | `.../content/unit/ContentMapperTest.java` |
| Backend | `.../content/unit/ContentSearchServiceTest.java`, `ContentSearchControllerTest.java` |
| Backend | `.../content/unit/LifecycleContentServiceTest.java`, `LifecycleContentControllerTest.java` |
| Backend | `.../content/policy/LifecycleContentStageResolverTest.java` |
| Backend | `.../content/policy/HtmlContentSanitizerTest.java` |
| Backend | `.../content/entity/ContentStageTest.java`, `StageCompatibilityTest.java`, `ContentCategoryArchitectureTest.java` |
| Backend | `.../content/security/ContentSecurityTest.java`, `ContentSearchSecurityTest.java`, `Story69ContentSecurityTest.java` |
| Backend | `.../search/SearchServiceImplTest.java`, `SearchControllerTest.java` |
| Backend | `.../search/ContentSearchProviderTest.java`, `QuestionSearchProviderTest.java` |
| Mobile | `test/features/community/services/content_service_test.dart` |
| Mobile | `test/features/community/models/content_model_test.dart` |
| Mobile | `test/features/community/view_content_lifecycle_screen_test.dart` |
| Mobile | `test/features/community/verified_content_detail_lifecycle_test.dart` |
| Mobile | `test/features/community/widgets/verified_content_body_test.dart` |
| Mobile | `test/features/community/content_service_story_6_9_test.dart` |
| Web | `src/features/contentManagement/models/content.test.ts` |
| Web | `src/features/contentManagement/services/contentApi.test.ts` |
| Backend (integration) | `.../content/integration/*.java`, `.../search/SearchIntegrationTest.java` |

### 4.4 Hành trình thai kỳ / hành trình mẹ / hành trình bé

| Tầng | File test |
|---|---|
| Backend | `.../journey/JourneyServiceImplTest.java` |
| Backend | `.../journey/JourneyUpdateServiceImplTest.java` |
| Backend | `.../journey/JourneyOnboardingServiceTest.java`, `JourneyOnboardingControllerTest.java` |
| Backend | `.../journey/JourneyDashboardServiceImplTest.java` |
| Backend | `.../journey/JourneyCanonicalLifecycleServiceTest.java`, `JourneyCanonicalLifecycleControllerTest.java` |
| Backend | `.../journey/PregnancyOutcomeServiceTest.java`, `JourneyPregnancyOutcomePolicyTest.java` |
| Backend | `.../journey/LifecycleConsentValidatorTest.java` |
| Backend | `.../journey/LifecycleSafetyOutcomeProjectorTest.java`, `LifecycleSafetyOutcomeInsertRepositoryTest.java` |
| Backend | `.../journey/entity/CanonicalAuditEventMappingTest.java` |
| Backend | `.../baby/BabyServiceImplTest.java`, `BabyServiceUpdateTest.java`, `BabyServiceArchiveTest.java` |
| Backend | `.../baby/CreateBabyProfileRequestValidationTest.java` |
| Backend | `.../baby/policy/BabyAccessPolicyTest.java` |
| Backend | `.../baby/entity/BabyProfilePersonMappingTest.java`, `BabyRemovedRouteContractTest.java` |
| Backend | `.../carejourney/BabyDailyLogServiceTest.java`, `BabyLogSummaryServiceTest.java` |
| Backend | `.../carejourney/MilestoneServiceTest.java`, `service/GrowthServiceTest.java` |
| Backend | `.../carejourney/ExpenseServiceTest.java` |
| Backend | `.../carejourney/BabyCareControllerAuthorizationContractTest.java`, `Mf03ApiRouteContractTest.java`, `Mf03OpenApiContractTest.java` |
| Backend | `.../carejourney/HealthBoundaryVocabularyTest.java`, `NotificationLogRedactionTest.java` |
| Backend | `.../carejourney/entity/{DevelopmentMilestone,GrowthMeasurement}CanonicalMappingTest.java` |
| Mobile | `test/features/journey/journey_model_test.dart`, `journey_onboarding_model_test.dart` |
| Mobile | `test/features/journey/journey_onboarding_screen_test.dart`, `journey_setup_screen_test.dart` |
| Mobile | `test/features/journey/journey_service_post_commit_test.dart` |
| Mobile | `test/features/journey/mother_journey_lifecycle_widgets_test.dart` |
| Mobile | `test/features/journey/pregnancy_outcome_screen_test.dart`, `pregnancy_outcome_draft_store_test.dart` |
| Mobile | `test/features/journey/postpartum_recovery_setup_screen_test.dart`, `postpartum_recovery_dashboard_test.dart` |
| Mobile | `test/features/journey/story_6_1_mobile_gap_test.dart`, `story_6_9_lifecycle_content_entry_test.dart` |
| Mobile | `test/features/baby/add_baby_screen_test.dart`, `baby_care_hub_test.dart`, `baby_care_contract_test.dart` |
| Mobile | `test/features/baby/baby_log_summary_screen_test.dart`, `milestone_model_test.dart` |
| Mobile | `test/features/baby/mf03_canonical_journey_test.dart` |
| Backend (integration) | `.../journey/*IntegrationTest.java`, `.../baby/BabyJourneyLinkageRemovalMigrationPostgresTest.java`, `.../carejourney/*IntegrationTest.java` |

### 4.5 Lịch nhắc (Reminder) & Lịch hẹn (Appointment)

| Tầng | File test |
|---|---|
| Backend | `.../reminder/ReminderServiceImplTest.java` |
| Backend | `.../reminder/UpdateReminderServiceTest.java` |
| Backend | `.../reminder/MedicationReminderServiceTest.java` |
| Backend | `.../reminder/VaccinationReminderServiceTest.java` |
| Backend | `.../reminder/schedule/ReminderScheduleServiceTest.java` |
| Backend | `.../reminder/schedule/service/ReminderScheduleProcessingServiceTest.java` |
| Backend | `.../reminder/schedule/job/ReminderScheduleWorkerTest.java` |
| Backend | `.../reminder/appointment/service/CareGroupAppointmentServiceTest.java` — **lịch hẹn** |
| Backend | `.../reminder/appointment/policy/CareGroupAppointmentScopeResolverTest.java` |
| Backend | `.../reminder/notification/service/AppointmentNotificationScheduleServiceTest.java` |
| Backend | `.../reminder/notification/service/AppointmentNotificationProcessingServiceTest.java` |
| Backend | `.../reminder/notification/service/CareGroupAppointmentNotificationServiceTest.java` |
| Backend | `.../reminder/notification/job/AppointmentNotificationWorkerTest.java` |
| Backend | `.../reminder/notification/AppointmentNotificationRuleValidatorTest.java` |
| Backend | `.../reminder/notification/{JobTransitionFlushContract,ReminderOccurrenceIdGenerationContract,ReminderWorkerPropertyBinding,AppointmentNotificationMigrationContract}Test.java` |
| Backend | `.../reminder/entity/ReminderCanonicalMappingTest.java` |
| Backend | `.../reminder/ReminderSecurityTest.java`, `LegacyTodayReminderAuthorizationContractTest.java` |
| Mobile | `test/features/reminder/reminder_model_test.dart` |
| Mobile | `test/features/reminder/reminder_schedule_service_test.dart` |
| Mobile | `test/features/reminder/create_appointment_reminder_screen_test.dart` |
| Mobile | `test/features/reminder/appointment_calendar_screen_test.dart` |
| Mobile | `test/features/reminder/appointment_notification_timing_test.dart` |
| Mobile | `test/features/reminder/shared_appointment_detail_screen_test.dart` |
| Backend (integration) | `.../reminder/job/NotificationJobRepositoryEmbeddedPostgresTest.java`, `.../reminder/entity/ReminderJourneyNullCareSubjectPostgresTest.java` |

### 4.6 Plan việc cần làm hằng ngày (Today Tasks / Checklist)

| Tầng | File test |
|---|---|
| Backend | `.../reminder/TodayTaskServiceTest.java`, `TodayTaskControllerTest.java` |
| Backend | `.../checklist/today/UnifiedTodayTaskServiceTest.java` |
| Backend | `.../checklist/today/UnifiedTaskActionFacadeTest.java` |
| Backend | `.../checklist/today/UnifiedTaskPolicyTest.java`, `UnifiedTaskTypedAuditTest.java` |
| Backend | `.../checklist/today/CareTaskActionHandlerTest.java` |
| Backend | `.../checklist/today/ChecklistTaskReopenActionHandlerTest.java` |
| Backend | `.../checklist/today/ReminderTaskActionHandlerOccurrenceTest.java`, `ReminderLegacyActionAdapterTest.java` |
| Backend | `.../checklist/today/{Reminder,CareTask,Checklist}TodayTaskProvider*Test.java` |
| Backend | `.../checklist/today/ReminderAccessPolicyTest.java`, `ReminderPostLockAuthorizationContractTest.java` |
| Backend | `.../checklist/today/CurrentChecklistServiceImplTest.java` |
| Backend | `.../checklist/today/TodayTaskContextLabelResolverTest.java` |
| Backend | `.../checklist/today/CareGroupChecklistScopeResolverTest.java` |
| Backend | `.../checklist/UserCreatedChecklistTaskServiceTest.java` — **tạo/sửa/xoá plan riêng** |
| Backend | `.../checklist/UserChecklistItemSystemTaskMutationTest.java` |
| Backend | `.../checklist/ChecklistV2CompatibilityMutationServiceTest.java` |
| Backend | `.../checklist/distribution/ChecklistDistributionServiceTest.java` — **gợi ý plan từ hệ thống** |
| Backend | `.../checklist/distribution/EnsureEligibleChecklistAssignmentsServiceTest.java` |
| Backend | `.../checklist/distribution/ChecklistLifecycleEligibilityServiceTest.java` |
| Backend | `.../checklist/distribution/ChecklistHistoryReconciliationServiceTest.java` |
| Backend | `.../checklist/distribution/ChecklistAuditWriterTest.java`, `ChecklistDistributionKeyV1Test.java` |
| Backend | `.../checklist/history/ChecklistHistoryServiceTest.java` |
| Backend | `.../checklist/sequence/ChecklistSequenceResolverTest.java` |
| Backend | `.../checklist/OptionalChecklistTemplateImportServiceTest.java`, `ChecklistImportControllerTest.java` |
| Mobile | `test/features/reminder/today_task_v2_model_test.dart`, `today_task_v2_service_test.dart` |
| Mobile | `test/features/reminder/today_tasks_panel_test.dart` |
| Mobile | `test/features/reminder/today_tasks_navigation_contract_test.dart` |
| Mobile | `test/features/reminder/today_tasks_red_acceptance_test.dart`, `today_tasks_accessibility_red_test.dart` |
| Mobile | `test/features/home/today_tasks_home_integration_test.dart`, `today_tasks_pull_refresh_red_test.dart` |
| Mobile | `test/features/checklist/user_checklist_service_test.dart`, `user_checklist_item_model_test.dart` |
| Mobile | `test/features/checklist/checklist_detail_screen_test.dart` |
| Mobile | `test/features/checklist/add_user_checklist_task_button_test.dart` |
| Mobile | `test/features/checklist/checklist_history_{model,screen,service}_test.dart` |
| Backend (integration) | `.../checklist/**/*{Postgres,EmbeddedPostgres}Test.java` (≈30 file), `.../checklist/operations/*` |

### 4.7 Ghi chú nhanh (BMI / nước / cử động) + EPDS

| Tầng | File test |
|---|---|
| Backend | `.../family/service/FamilyQuickNoteServiceTest.java` |
| Backend | `.../health/HealthMetricAddServiceTest.java` — BMI, nước, cử động thai |
| Backend | `.../health/MetricObservationValidatorTest.java` |
| Backend | `.../health/PostpartumLogServiceTest.java`, `PostpartumLogControllerTest.java` — **EPDS** |
| Backend | `.../triage/LifecycleSafetyMetricsTest.java` |
| Mobile | `test/features/healthRecords/epds_screen_test.dart` — **EPDS** |
| Mobile | `test/features/healthRecords/postpartum_log_list_screen_test.dart`, `postpartum_log_draft_store_test.dart` |
| Backend (integration) | `.../health/PostpartumLogPostgresIntegrationTest.java` |

### 4.8 Chỉ số sức khoẻ & Hồ sơ sức khoẻ

| Tầng | File test |
|---|---|
| Backend | `.../health/HealthMetricServiceImplTest.java` |
| Backend | `.../health/HealthMetricUpdateServiceTest.java` |
| Backend | `.../health/MetricTrendServiceTest.java` |
| Backend | `.../health/HealthRecordServiceImplTest.java` |
| Backend | `.../health/service/HealthRecordServiceUpdateTest.java`, `HealthRecordServiceArchiveTest.java`, `HealthRecordServiceTimelineTest.java` |
| Backend | `.../health/HealthSummaryServiceTest.java`, `ShareSummaryServiceTest.java` |
| Backend | `.../health/entity/MetricDefinitionEntityContractTest.java`, `CanonicalHealthObservationRoundTripTest.java` |
| Backend | `.../health/repository/HealthRecordFileRepositoryTest.java` |
| Backend | `.../vaccination/VaccinationServiceImplTest.java`, `VaccinationBookServiceTest.java`, `VaccinationReminderDispatchTest.java` |
| Mobile | `test/features/healthRecords/health_metric_model_test.dart` |
| Mobile | `test/features/healthRecords/maternal_health_metric_screen_test.dart` |
| Mobile | `test/features/healthRecords/growth_measurement_form_test.dart`, `growth_trend_chart_test.dart` |
| Mobile | `test/features/healthRecords/who_growth_standard_test.dart` |
| Mobile | `test/features/healthRecords/vaccination_model_test.dart` |
| Backend (integration) | `.../health/{MetricDefinitionMigrationContract,repository/HealthObservationRepositoryIntegration}Test.java`, `.../vaccination/VaccinationBookEmbeddedPostgresTest.java` |

### 4.9 Giám sát an toàn IMU (phát hiện ngã)

| Tầng | File test |
|---|---|
| Backend | `.../safety/FallDetectionServiceTest.java`, `FallDetectionControllerTest.java` |
| Backend | `.../safety/SuspectedFallDetectedHandlerTest.java` |
| Backend | `.../safety/SafetyConfigServiceTest.java`, `SafetyConfigControllerTest.java`, `SafetyConfigChangedHandlerTest.java` |
| Backend | `.../safety/SensorSelfTestServiceTest.java`, `SensorSelfTestControllerTest.java` |
| Backend | `.../safety/service/FamilyAlertSentHandlerTest.java` |
| Backend | `.../safety/SafetyEventCanonicalMappingTest.java` |
| Backend | `.../safety/repository/ISafetyEventRepositoryQueryTest.java` |
| Mobile | `test/features/safety/imu_fall_detector_test.dart` |
| Mobile | `test/features/safety/fall_detection_sensor_service_test.dart` |
| Mobile | `test/features/safety/imu_diagnostics_model_test.dart` |
| Mobile | `test/features/safety/safety_monitoring_screen_test.dart` |
| Mobile | `test/features/safety/safety_countdown_sheet_test.dart` |
| Mobile | `test/features/safety/safety_foreground_coordinator_test.dart` |
| Mobile | `test/features/safety/safety_contract_test.dart`, `safety_demo_mode_test.dart` |
| Backend (integration) | `.../safety/{Mf14CanonicalPersistence,SafetyMonitoringConcurrency,SafetyPersistenceMigration}*Test.java` |

### 4.10 Danh sách chuyên gia · Yêu cầu trò chuyện · Chat / gọi video

| Tầng | File test |
|---|---|
| Backend | `.../expert/service/ExpertProfileServiceImplDirectoryTest.java` — danh sách chuyên gia |
| Backend | `.../expert/service/ExpertProfileServiceImplSpecialtyTest.java` |
| Backend | `.../expert/controller/ExpertProfileControllerTest.java` |
| Backend | `.../expert/ExpertConsultationEligibilityTest.java` |
| Backend | `.../consultation/service/impl/ConsultationRequestServiceImplCreateTest.java` — gửi yêu cầu |
| Backend | `.../consultation/service/impl/ConsultationRequestServiceImplLifecycleTest.java` — chấp nhận/từ chối |
| Backend | `.../consultation/service/impl/ConsultationRequestServiceImplListTest.java` |
| Backend | `.../consultation/policy/ConsultationRequestPolicyTest.java` |
| Backend | `.../consultation/controller/ConsultationRequestControllerTest.java`, `ConsultationRequestControllerSecurityTest.java` |
| Backend | `.../consultation/dto/request/PreferredWindowValidatorTest.java` |
| Backend | `.../consultation/event/ConsultationRequestNotificationListenerTest.java` |
| Backend | `.../directchat/service/impl/DirectConversationServiceImplTest.java` |
| Backend | `.../directchat/service/impl/DirectConversationServiceImplReadTest.java`, `...SummaryTest.java` |
| Backend | `.../directchat/service/impl/DirectMessageServiceImplTest.java`, `DirectMessageWriterTest.java` |
| Backend | `.../directchat/service/impl/ConversationCallServiceImplTest.java` — **video call** |
| Backend | `.../directchat/controller/DirectConversationControllerTest.java`, `...SecurityTest.java` |
| Backend | `.../directchat/controller/ConversationCallControllerContractTest.java` |
| Backend | `.../directchat/policy/DirectConversationPolicyImplTest.java` |
| Backend | `.../directchat/service/DirectChatAttachmentAccessServiceTest.java` — gửi ảnh/file |
| Backend | `.../directchat/mapper/TimelineCursorCodecTest.java` |
| Backend | `.../directchat/job/CallTimeoutReconciliationJobTest.java`, `FirebaseEventRetentionJobTest.java` |
| Backend | `.../directchat/event/DirectMessageNotificationListenerTest.java` |
| Backend | `.../integration/zegocloud/ZegoCloudServiceImplTest.java`, `ZegoToken04GeneratorTest.java` |
| Backend | `.../integration/firebase/{FirebaseAuthBridgeServiceImpl,FirebaseRealtimeGatewayImpl,FirebaseAuthGatewayImpl,ConversationEventPublisherImpl,FirebaseTokenController}Test.java` |
| Mobile | `test/features/directChat/expert_directory_screen_test.dart`, `expert_directory_search_test.dart` |
| Mobile | `test/features/directChat/conversation_list_screen_test.dart` |
| Mobile | `test/features/directChat/direct_chat_screen_test.dart`, `direct_conversation_test.dart` |
| Mobile | `test/features/directChat/timeline_item_test.dart` |
| Mobile | `test/features/directChat/services/firebase_token_service_test.dart` |
| Mobile | `test/features/directChat/calls/direct_call_coordinator_test.dart`, `direct_call_host_test.dart`, `direct_call_reducer_test.dart` |
| Mobile | `test/features/directChat/calls/rtc_permissions_test.dart`, `rtc_error_handling_test.dart`, `rtc_platform_capabilities_test.dart` |
| Mobile | `test/features/directChat/calls/zego_stream_extra_info_test.dart`, `zego_web_host_test.dart` |
| Mobile | `test/features/consultation/consultation_request_mobile_test.dart` |
| Mobile | `test/features/consultation/consultation_request_profile_notification_test.dart` |
| Mobile | `test/integrations/firebaseRealtime/firebase_conversation_signaling_port_test.dart` |
| Web | `src/features/directChat/calls/directCallCoordinator.test.ts`, `directCallState.test.ts` |
| Web | `src/features/directChat/calls/rtcMediaPermissions.test.ts`, `zegoRoomSession.test.ts` |
| Web | `src/shared/integrations/firebaseRealtime/conversationSignalingPort.test.ts` |
| Web | `src/features/expert/services/expertApi.test.ts` |
| Backend (integration) | `.../consultation/integration/*.java`, `.../directchat/integration/*.java`, `.../expert/**/*IntegrationTest.java` |

### 4.11 Nhóm chăm sóc (Care Group) — tạo/sửa/xoá, mời, phân quyền

| Tầng | File test |
|---|---|
| Backend | `.../family/CareGroupServiceImplTest.java` |
| Backend | `.../family/service/CareGroupServiceImplInviteTest.java` — gửi lời mời |
| Backend | `.../family/service/CareGroupServiceImplAcceptInvitationTest.java` |
| Backend | `.../family/service/CareGroupServiceImplMembershipLifecycleTest.java` |
| Backend | `.../family/service/CareGroupServiceImplPermissionTest.java` — phân quyền |
| Backend | `.../family/policy/CareGroupAuthorizationPolicyTest.java`, `.../family/CareGroupAccessPolicyTest.java` |
| Backend | `.../family/controller/CareGroupControllerInviteTest.java`, `CareGroupControllerPermissionTest.java`, `CareGroupControllerAssignTaskTest.java` |
| Backend | `.../family/service/CareTaskServiceImplTest.java` + `...Update/UpdateStatus/Cancel/GetDetail/TaskManagement/Security` |
| Backend | `.../family/service/SharedDataServiceImplTest.java` — dữ liệu mother chia sẻ |
| Backend | `.../family/service/FamilyDashboardServiceTest.java` |
| Backend | `.../family/service/CareGroupJourneyRelinkServiceTest.java`, `.../family/CareGroupJourneyRelinkContractTest.java` |
| Backend | `.../family/InviteTokenGeneratorTest.java` |
| Backend | `.../family/entity/{GroupMemberRoleConverter,InviteStatus,CareTaskStatusFsm,CareTaskLegacyMetadataDefaultContract}Test.java` |
| Backend | `.../family/repository/CareGroupMemberRepositoryQueryTest.java` |
| Mobile | `test/features/familySync/mother_care_group_ui_test.dart` |
| Mobile | `test/features/familySync/care_group_invitation_screen_test.dart` |
| Mobile | `test/features/familySync/family_dashboard_contract_test.dart` |
| Mobile | `test/features/familySync/family_health_sharing_contract_test.dart` |
| Backend (integration) | `.../family/{CareGroupInvite,CareTaskAssignment,ManageFamilyPermission,InviteTokenUniqueness,CareTaskRepository}IntegrationTest.java`, `.../family/security/InviteFamilyMemberSecurityTest.java` |

### 4.12 Bản đồ cơ sở y tế · gọi 115 · báo gia đình (Emergency)

| Tầng | File test |
|---|---|
| Backend | `.../map/CareFacilityServiceImplTest.java` — tìm cơ sở y tế gần nhất |
| Backend | `.../emergency/EmergencyServiceTest.java`, `EmergencyControllerTest.java` |
| Backend | `.../emergency/EmergencyMapHandoffServiceImplTest.java` |
| Backend | `.../emergency/EmergencyEscalationHandlerTest.java` |
| Backend | `.../emergency/EmergencySessionOpenedHandlerTest.java` |
| Backend | `.../emergency/FamilyAlertServiceTest.java`, `.../family/service/FamilyAlertServiceImplTest.java` |
| Backend | `.../emergency/EmergencyAlertAttemptServiceTest.java`, `EmergencyAlertRetryJobTest.java` |
| Backend | `.../emergency/EmergencyNotificationDeliveryAdaptersTest.java` |
| Backend | `.../emergency/adapter/FamilyMemberPortAdapterTest.java` |
| Backend | `.../emergency/entity/EmergencyAlertDeliveryOwnerTest.java` |
| Backend | `.../masterdata/MasterDataServiceImplTest.java`, `MasterDataControllerTest.java` |
| Mobile | `test/features/emergency/emergency_map_screen_test.dart` |
| Mobile | `test/features/emergency/nearby_care_contract_test.dart` |
| Mobile | `test/features/emergency/trackasia_web_contract_test.dart` |
| Backend (integration) | `.../emergency/EmergencyTriageLinkPostgresIntegrationTest.java` |

### 4.13 CareBridge AI (trợ lý AI / triage)

| Tầng | File test |
|---|---|
| Backend | `.../triage/TriageServiceTest.java`, `TriageServicePreScreenTest.java` |
| Backend | `.../triage/TriageV2SessionServiceTest.java`, `service/TriageV2ShadowServiceTest.java` |
| Backend | `.../triage/TriageGraphServiceTest.java`, `engine/TriageGraphServiceHealthContextTest.java` |
| Backend | `.../triage/TriageContinuationServiceTest.java` |
| Backend | `.../triage/TriageConsentServiceTest.java`, `TriageConsentControllerTest.java` |
| Backend | `.../triage/TriageRagEnrichmentServiceTest.java`, `SourceRetrieverTest.java` |
| Backend | `.../triage/TriageSessionEvidenceWriterTest.java` |
| Backend | `.../triage/TriageRedFlagPolicyTest.java`, `TriageRedFlagPreScreenPolicyTest.java`, `TriageRedFlagPreScreenSecurityTest.java` |
| Backend | `.../triage/RedFlagRuleServiceImplTest.java`, `RedFlagRuleControllerTest.java` |
| Backend | `.../triage/HealthMemoryServiceImplTest.java`, `HealthMemoryWriteTest.java`, `HealthMemoryContextReadTest.java`, `HealthMemorySummaryPolicyTest.java` |
| Backend | `.../triage/{TriageServiceHealthMemoryContext,TriageServiceGestationalContext,IntakeControllerHealthContext,IntakeController}Test.java` |
| Backend | `.../triage/SymptomNormalizerTest.java`, `TriageRecommendationCodeTest.java`, `PediatricRedParityTest.java` |
| Backend | `.../triage/LifecycleIntakeBindingServiceTest.java`, `Story67LifecycleContractRedTest.java` |
| Backend | `.../triage/HttpChildTriageAiClientTest.java`, `TriageV2MetricsTest.java` |
| Backend | `.../triage/rules/*.java` (16 file: StageResolver, ZeroTrustCalculator, IntentResolver, GeminiFailOpenPrevention, FallbackDriftProtection, TriageRuleParityV2, ContextParityVector, ContextContractParity, QuestionCatalogFilter, LegacyGovernanceCompatibility, ComplaintTaxonomy, ParityResultFingerprint, TargetEntityResolver, CanonicalAnswerMapper, IndependentGlobalSafetyFallback, GeminiOutcomeBoundary, TriageV2Readiness) |
| Backend | `.../ai/StructuredIntakeServiceTest.java`, `IntakeSessionCompletedHandlerTest.java` |
| Backend | `.../integration/gemini/RagServiceTest.java`, `RagPolicyServiceTest.java`, `RagControllerTest.java` |
| Backend | `.../integration/gemini/RagImplementationContractTest.java`, `RagStageBoundaryTest.java`, `RagNoGeminiStartupTest.java` |
| Backend | `.../integration/gemini/GeminiRagServiceStory69Test.java`, `GeminiFallbackLogRedactionTest.java`, `ContentItemContextRetrieverTest.java` |
| Backend | `.../reminder/TriageFollowUpServiceTest.java`, `TriageYellowFollowUpHandlerTest.java`, `TriageFollowUpTitlePolicyTest.java` |
| Backend | `.../consultation/context/TriageExpertHandoffServiceTest.java`, `TriageExpertHandoffPolicyTest.java`, `TriageExpertHandoffControllerTest.java`, `TriageCitationResolverTest.java` |
| Mobile | `test/features/aiTriage/symptom_intake_screen_test.dart` |
| Mobile | `test/features/aiTriage/risk_triage_result_screen_test.dart` |
| Mobile | `test/features/aiTriage/triage_history_screen_test.dart` |
| Mobile | `test/features/aiTriage/triage_v2_screen_test.dart`, `triage_v2_chat_adapter_test.dart`, `triage_v2_model_service_test.dart`, `triage_v2_cutover_test.dart` |
| Mobile | `test/features/aiTriage/triage_model_contract_test.dart`, `triage_origin_entry_test.dart` |
| Mobile | `test/features/aiTriage/floating_ai_triage_host_test.dart`, `triage_demo_visual_test.dart` |
| Mobile | `test/features/aiTriage/story_6_7_continuation_restore_test.dart`, `story_6_7_continuation_storage_test.dart`, `story_6_7_lifecycle_origin_contract_test.dart`, `story_6_8_yellow_handoff_red_test.dart` |
| Mobile | `test/features/consultation/triage_expert_handoff_{mobile,model_service,participant}_test.dart` |
| Backend (integration) | `.../triage/*IntegrationTest.java` (7 file), `.../consultation/context/TriageExpertHandoff*{Postgres,Integration}Test.java` |

---

## 5. Mobile — Role FAMILY

| Tính năng | File test |
|---|---|
| Cộng đồng, Nội dung & FAQ | dùng chung mục **4.1** và **4.3** |
| Tham gia / từ chối lời mời nhóm chăm sóc | `.../family/service/CareGroupServiceImplAcceptInvitationTest.java`; Mobile `test/features/familySync/care_group_invitation_screen_test.dart` |
| Xem dữ liệu mother cấp quyền | `.../family/service/SharedDataServiceImplTest.java`, `.../family/policy/CareGroupAuthorizationPolicyTest.java`; Mobile `test/features/familySync/family_health_sharing_contract_test.dart` |
| Dashboard gia đình | `.../family/service/FamilyDashboardServiceTest.java`; Mobile `test/features/familySync/family_dashboard_contract_test.dart` |
| Chuyên gia / chat / video call | dùng chung mục **4.10** |
| Cảnh báo khi mẹ ngã | `.../safety/service/FamilyAlertSentHandlerTest.java`, `.../emergency/FamilyAlertServiceTest.java`, `.../family/service/FamilyAlertServiceImplTest.java` |
| Cảnh báo EPDS bất thường | `.../health/PostpartumLogServiceTest.java`, `.../triage/LifecycleSafetyMetricsTest.java` |
| Trang chủ Family | Mobile `test/features/home/family_member_home_notification_test.dart` |

---

## 6. Role EXPERT (Mobile + Web Portal)

| Tính năng | File test |
|---|---|
| Đăng ký chuyên gia, gửi hồ sơ chờ xác minh | `.../expertverification/ExpertIdentityVerificationServiceTest.java` |
| | `.../expertverification/ExpertCredentialPreviewServiceTest.java` |
| | `.../expertverification/DuplicateIdentityFaceServiceTest.java` |
| | `.../expert/dto/request/ExpertProfileRequestValidationTest.java` |
| | Mobile `test/features/expert/expert_onboarding_service_test.dart`, `expert_identity_resume_test.dart` |
| | Web `src/features/expert/pages/ExpertOnboardingPage.test.tsx`, `src/features/auth/pages/ExpertRegisterPage.test.tsx` |
| Quản lý hồ sơ chuyên môn / chứng chỉ | `.../expert/service/ExpertProfileServiceImplSpecialtyTest.java` |
| | Mobile `test/features/expert/expert_public_profile_screen_test.dart` |
| | Web `src/features/expert/services/expertApi.test.ts` |
| Trả lời câu hỏi cộng đồng (nhãn chuyên gia) | `.../community/service/CommunityAnswerServiceImplTest.java`, `.../community/mapper/CommunityAnswerMapperTest.java` |
| | `.../expert/service/impl/ContributionPointServiceImplTest.java` |
| Tiếp nhận / từ chối yêu cầu trò chuyện | `.../consultation/service/impl/ConsultationRequestServiceImplLifecycleTest.java` |
| | Mobile `test/features/consultation/expert_dashboard_consultation_test.dart` |
| Khung giờ lịch làm việc | `.../expertavailability/service/impl/ExpertAvailabilityServiceImplTest.java` |
| | `.../expertavailability/controller/ExpertAvailabilityControllerOwnershipTest.java` |
| | `.../expertavailability/mapper/ExpertAvailabilityMapperTest.java` |
| | Mobile `test/features/expert/expert_calendar_screen_test.dart` |
| Trang chủ Expert (mobile) | Mobile `test/features/home/expert_app_home_screen_test.dart` |
| Sinh tài khoản expert | Mobile `test/features/directChat/expert_account_generation_test.dart` |

---

## 7. Web Portal — Role MODERATOR

| Trang / Use case | File test |
|---|---|
| `/moderator/moderator-dashboard` | `.../content/CommunityDashboardServiceImplTest.java`, `CommunityDashboardControllerTest.java`, `.../security/CommunityDashboardControllerSecurityTest.java` |
| `/moderator/pending-content` (duyệt nội dung) | `.../moderation/ModerateContentServiceImplTest.java`, `ModerateContentControllerTest.java`, `.../security/ModerateContentControllerSecurityTest.java` |
| | `.../content/ContentApprovalServiceImplTest.java`, `.../security/ContentApprovalControllerSecurityTest.java` |
| `/moderator/community-content` | `.../moderation/ContentPreviewServiceTest.java`, `.../content/HideContentServiceImplTest.java`, `HideContentControllerTest.java` |
| | `.../content/UnpublishContentServiceImplTest.java`, `UnpublishContentControllerTest.java` |
| | `.../content/UpdateContentServiceImplTest.java`, `UpdateContentControllerTest.java` |
| | `.../security/{HideContent,UnpublishContent,UpdateContent}ControllerSecurityTest.java` |
| `/moderator/reports` (xử lý báo cáo) | `.../moderation/ResolveReportServiceImplTest.java`, `ResolveReportControllerTest.java` |
| | `.../moderation/ClaimReportWorkflowTest.java`, `ClaimReportControllerSecurityTest.java` |
| | `.../security/ResolveReportControllerSecurityTest.java` |
| | `.../content/report/ReportServiceImplTest.java`, `ReportControllerTest.java` |
| `/moderator/violations` (cảnh cáo / khoá tài khoản) | `.../moderation/WarnOrSuspendAccountServiceImplTest.java`, `WarnOrSuspendAccountControllerTest.java` |
| | `.../security/WarnOrSuspendAccount{Controller,}{Security,Enforcement}Test.java` |
| Kiểm duyệt AI (AI moderation queue) | `.../aimoderation/AiModerationCaseServiceTest.java`, `AiAssessmentModeratorServiceTest.java` |
| | `.../aimoderation/AiModerationDecisionPolicyTest.java`, `AiModerationOutcomeApplierTest.java` |
| | `.../aimoderation/AiScan{Enqueue,Processing,ResultRecorder}Test.java`, `AiContentScanWorkerTest.java` |
| | `.../aimoderation/AiVerdictParserTest.java`, `AiModerationPromptBuilderTest.java`, `GeminiModerationClientTest.java` |
| | `.../aimoderation/AiModerationModeratorControllerSecurityTest.java` |
| Chung moderation | `.../moderation/ModerationServiceImplTest.java`, `ModerationControllerTest.java`, `ModerationControllerSecurityTest.java`, `ModerationMapperTest.java` |
| Ranh giới quyền riêng tư sức khoẻ | `.../moderation/ModeratorHealthPrivacySecurityTest.java` |
| Escalation lên System Admin | `.../moderation/SystemAdminEscalationControllerSecurityTest.java` |
| Audit | `.../audit/AuditServiceImplTest.java`, `.../audit/policy/AuditEligibilityPolicyTest.java`, `.../audit/mapper/AuditLogMapperTest.java`, `.../audit/controller/AuditControllerTest.java` |
| | `.../audit/service/SecurityIncidentServiceImplTest.java`, `.../audit/controller/SecurityIncidentControllerTest.java`, `.../audit/repository/SecurityEventRepositoryQueryTest.java` |
| Backend (integration) | `.../integration/{ModerateContent,ResolveReport,HideContent,UnpublishContent,UpdateContent,ContentApproval,WarnOrSuspendAccount*,CommunityDashboard}IntegrationTest.java`, `.../moderation/*IntegrationTest.java` |

---

## 8. Web Portal — Role SYSTEM ADMIN

| Trang / Use case | File test |
|---|---|
| `/admin/dashboard` | `.../content/CommunityDashboardServiceImplTest.java` |
| `/admin/users` (quản lý người dùng) | `.../identity/admin/service/AdminUserServiceImplTest.java` |
| | `.../identity/admin/controller/AdminUserControllerTest.java` |
| | `.../identity/admin/mapper/AdminUserMapperTest.java`, `.../identity/admin/repository/UserRepositorySearchTest.java` |
| | Web `src/features/admin/pages/UserListPage.test.tsx`, `UserDetailPage.test.tsx`, `UpdateUserRolePage.test.tsx` |
| Phân quyền / vai trò | `.../identity/admin/service/AdminRoleServiceImplTest.java`, `.../identity/admin/controller/AdminRoleControllerTest.java` |
| Quản lý nhân sự nội bộ | `.../identity/admin/service/AdminStaffServiceImplTest.java`, `.../identity/admin/controller/AdminStaffControllerTest.java` |
| `/admin/account-lock-appeals` | `.../moderation/WarnOrSuspendAccountServiceImplTest.java`, `.../security/WarnOrSuspendAccountEnforcementTest.java` |
| | `.../security/filter/JwtAuthenticationFilterAccountStateTest.java` |
| | Web `src/features/auth/pages/BlockedAccountPage.test.tsx` |
| `/admin/experts` + `/admin/expert-verification-queue` | `.../expertverification/ExpertIdentityVerificationServiceTest.java` |
| | `.../expertverification/ExpertCredentialPreviewServiceTest.java`, `DuplicateIdentityFaceServiceTest.java` |
| | Web `src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` |
| `/admin/safety-rules` (red-flag rules) | `.../triage/RedFlagRuleServiceImplTest.java`, `RedFlagRuleControllerTest.java`, `.../security/RedFlagRuleControllerSecurityTest.java` |
| `/admin/system-configuration` | `.../systemconfiguration/SystemConfigurationServiceImplTest.java` |
| | `.../systemconfiguration/SystemConfigurationControllerSecurityTest.java` |
| | `.../systemconfiguration/SystemMaintenanceModeServiceTest.java`, `MaintenanceModeFilterTest.java` |
| | Web `src/features/aiRuleManagement/pages/SystemConfigurationPage.test.tsx`, `src/features/system/pages/MaintenancePage.test.tsx` |
| Cấu hình AI moderation | `.../aimoderation/AiPolicyServiceImplTest.java`, `AiModerationAdminControllerSecurityTest.java`, `AiModerationConfigSmokeTest.java` |
| `/admin/content-approval-queue` | `.../content/AdminContentServiceImplTest.java`, `AdminContentControllerTest.java` |
| | `.../content/ContentApprovalServiceImplTest.java` |
| | Web `src/features/contentManagement/pages/ContentApprovalQueuePage.test.tsx` |
| `/admin/notifications` | `.../notification/NotificationSendServiceTest.java`, `NotificationControllerContractTest.java` |
| Cấu hình an toàn IMU | `.../safety/SafetyConfigServiceTest.java`, `SafetyConfigControllerTest.java` |
| Cấu hình bài tập / posture | `.../exercise/AdminExerciseServiceTest.java`, `AdminExerciseControllerTest.java`, `AdminPostureConfigControllerTest.java` |
| Backend (integration) | `.../identity/admin/controller/AdminUserControllerIntegrationTest.java`, `.../security/Canonical Role*IntegrationTest.java` |

---

## 9. Web Portal — Role CONTENT ADMIN

| Trang / Use case | File test |
|---|---|
| `/content/dashboard`, `/content/list` | `.../content/unit/ContentServiceImplTest.java`, `ContentControllerTest.java` |
| `/content/articles` (tạo/sửa bài viết) | `.../content/UpdateContentServiceImplTest.java`, `UpdateContentControllerTest.java` |
| | `.../content/unit/ContentMapperTest.java`, `.../content/policy/HtmlContentSanitizerTest.java` |
| | Web `src/features/contentManagement/components/RichTextEditor.test.tsx` |
| | Web `src/features/contentManagement/pages/ContentDetailPage.test.tsx` |
| | Web `src/features/contentManagement/utils/tableSorting.test.ts` |
| `/content/faq` | `.../content/unit/ContentSearchServiceTest.java`, `ContentSearchControllerTest.java` |
| | `.../content/entity/ContentStageTest.java`, `StageCompatibilityTest.java` |
| `/content/checklists` (mẫu checklist) | `.../content/AdminChecklistTemplateServiceImplTest.java`, `AdminChecklistTemplateControllerTest.java` |
| | `.../content/ChecklistTemplateApprovalServiceImplTest.java`, `ChecklistTemplateApprovalControllerTest.java` |
| | `.../content/ChecklistTemplateInlineMetadataMappingTest.java`, `.../content/unit/AdminChecklistControllerTest.java` |
| | `.../checklist/OptionalChecklistTemplateImportServiceTest.java`, `ChecklistImportControllerTest.java` |
| | Web `src/features/contentManagement/pages/ChecklistListPage.test.tsx`, `ChecklistFormPage.test.tsx`, `ChecklistDetailPage.test.tsx`, `ChecklistVersionHistoryPage.test.tsx`, `checklistApprovalPresentation.test.ts` |
| `/content/topics` | `.../community/CommunityTopicServiceImplTest.java`, `CommunityTopicControllerTest.java`, `CommunityTopicSearchServiceImplTest.java` |
| | Web `src/features/contentManagement/pages/topicTree.test.ts`, `topicErrors.test.ts` |
| `/content/notifications` | dùng chung mục **3.4** |
| Lifecycle content (theo giai đoạn) | `.../content/unit/LifecycleContentServiceTest.java`, `LifecycleContentControllerTest.java`, `.../content/policy/LifecycleContentStageResolverTest.java` |
| | `.../content/StaffContentDetailResponseSerializationTest.java` |
| Backend (integration) | `.../content/integration/*.java` |

---

## 10. Hạ tầng / nền tảng (không gắn use case cụ thể)

| Nhóm | File test |
|---|---|
| Cấu hình môi trường & datasource | `.../common/config/{DotenvEnvironmentPostProcessor,RuntimeDatasourceEnvironmentPostProcessor,HermeticDatasourceEnvironmentPostProcessor,HermeticProfileConfiguration}Test.java` |
| Xử lý exception toàn cục | `.../common/exception/GlobalExceptionHandlerTest.java` |
| Dev seeder | `.../common/dev/DevDataSeederPasswordTest.java` |
| Canonical schema / migration contract | `.../canonical/CanonicalPhysicalTableMappingTest.java`, `.../testsupport/FlywayMigrationChainTest.java` |
| | `.../checklist/distribution/*MigrationContractTest.java`, `.../safety/Mf14MigrationContractTest.java`, `.../health/MetricDefinitionMigrationContractTest.java` |
| Mobile — app shell & router | `test/features/home/home_shell_test.dart`, `test/features/home/mother_home_screen_test.dart`, `test/widget_test.dart` |
| Migration (Postgres, ngoài phạm vi) | `.../migration/*.java`, `.../testsupport/*{Postgres,Testcontainers}*.java` |

---

## 11. Kết quả chạy (2026-08-08)

Baseline trước khi làm: **104 test đỏ** (71 backend unit + 30 mobile + 3 web).
Sau khi hoàn thiện: **0 test đỏ** — 3 defect được phát hiện đã được sửa tận gốc ở tầng ứng dụng.

Nguyên tắc xử lý đã áp dụng:
- test lệch so với code hiện tại (đổi route, đổi API, đổi ràng buộc) → **sửa test**;
- code lệch so với hợp đồng an toàn/RBAC → **giữ test đỏ** cho tới khi chủ dự án xác nhận, rồi **sửa code**.

| Defect | Trạng thái | Cách xử lý |
|---|---|---|
| `RedFlagRuleController` mở cho MODERATOR / CONTENT_ADMIN (commit `ff31c960`) | ✅ Đã sửa | `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` — khớp với route `/admin/safety-rules` vốn đã là `requiredRoles={['SYSTEM_ADMIN']}` ở web. Test mở rộng thành `@ParameterizedTest` phủ 5 role bị từ chối + 1 ca SYSTEM_ADMIN 200. |
| `AiModerationAdminController` mở cho MODERATOR / CONTENT_ADMIN (cùng commit) | ✅ Đã sửa | Class → `hasRole('SYSTEM_ADMIN')`; riêng `GET /status` giữ `hasAnyRole('SYSTEM_ADMIN','MODERATOR')` vì trang moderator `PendingContentQueuePage` đọc nó để biết AI screening đang bật. `policies` CRUD / `test` / `rescan` giờ admin-only. |
| `GrowthMeasurementFormScreen` gửi `note: ""` đè lên `null` | ✅ Đã sửa | `existing?.note?.trim() ?? ''` — chuẩn hoá cả hai vế trước khi so sánh. Bổ sung 2 test: xoá ghi chú có sẵn *vẫn* gửi `note: ""`, và ghi chú không đổi *không* lọt vào payload. |

**Backend (integration)**: bật Docker Desktop trước khi chạy; nếu tắt sẽ báo
`NoClassDefFoundError: AbstractPostgresIntegrationTest` — lỗi môi trường, không phải lỗi code.

## 12. Thay đổi ngoài thư mục test (cần biết khi review)

| File | Thay đổi | Lý do |
|---|---|---|
| `CareBridgeAPI/.../triage/controller/RedFlagRuleController.java` | `@PreAuthorize` → `hasRole('SYSTEM_ADMIN')` | **Sửa RBAC theo yêu cầu**: `/admin/safety-rules` chỉ dành cho SYSTEM_ADMIN. Red-flag rule điều khiển luồng escalation cấp cứu nên quyền soạn thảo phải giữ hẹp. Chỉ `SafetyRuleManagementPage` (đã gate SYSTEM_ADMIN) gọi API này → không ảnh hưởng màn hình khác. |
| `CareBridgeAPI/.../aimoderation/controller/AiModerationAdminController.java` | Class → `hasRole('SYSTEM_ADMIN')`; `GET /status` → `hasAnyRole('SYSTEM_ADMIN','MODERATOR')` | Thu hẹp về admin-only, giữ đúng một ngoại lệ read-only mà moderator thật sự cần. |
| `CareBridgeMobileApp/lib/features/healthRecords/screens/growth_measurement_form_screen.dart` | `existing?.note?.trim() ?? ''` | Sửa lỗi ghi đè `null` thành chuỗi rỗng khi sửa phép đo mà không đụng vào ô ghi chú. |
| `CareBridgeMobileApp/lib/integrations/firebaseRealtime/firebase_conversation_signaling_port.dart` | Thêm `_FirebaseSignInGate.reset()` và `@visibleForTesting resetFirebaseSignInGateForTest()` | `_firebaseSignInGate` là hàng đợi **toàn cục cấp process**; mỗi mắt xích gắn với zone đã tạo nó. Widget test chạy fake-async nên một test kết thúc giữa chừng làm hàng đợi kẹt vĩnh viễn → mọi test sau treo tới timeout 10 phút. Hook này **không có caller ở production**, không đổi hành vi runtime. |
| `CareBridgeWebApp/vitest.setup.ts` (mới) + `vite.config.ts` (`setupFiles`) | Cài lại `localStorage`/`sessionStorage` cho jsdom | Node ≥ 22 có sẵn global `localStorage` thử nghiệm; nó che mất Storage của jsdom và thiếu `setItem`, làm mọi store dùng `zustand/persist` (authStore) văng lỗi. Đã chạy `npm run build` xác nhận không ảnh hưởng build. |
| `CareBridgeMobileApp/test/features/aiTriage/goldens/*.png` (3 ảnh) | Chụp lại golden | Màn hình chọn giai đoạn đã thêm 1 lựa chọn thứ 5 (diff 32%); ảnh mới đã được kiểm tra bằng mắt: bố cục đúng, không tràn, không lỗi render. |
| `CareBridgeMobileApp/macos/.gitignore` + gỡ 10 file sinh tự động khỏi git index | Bỏ theo dõi các artefact do Flutter sinh ra | Xem mục 13. |

## 13. Dọn các file Flutter sinh tự động bị commit nhầm

`flutter test` / `flutter run` **luôn sinh lại** `GeneratedPluginRegistrant` và các file `Flutter/ephemeral/`.
Vì chúng đang được git theo dõi, mỗi lần chạy test là repo lại bẩn — kể cả sau khi commit `55159ec1`
xoá tay `IntegrationTestPlugin`, nó vẫn quay lại ngay lần chạy kế tiếp.

Điểm mấu chốt: `android/.gitignore` và `ios/.gitignore` **đã có sẵn** luật bỏ qua các file này —
chỉ là file đã bị commit trước khi có luật, mà `.gitignore` không có tác dụng với file đã được theo dõi.
Bản thân `macos/Flutter/ephemeral/Flutter-Generated.xcconfig` còn ghi rõ ở dòng đầu
*"This is a generated file; do not edit or check into version control"* và đang chứa đường dẫn máy
cá nhân của một thành viên (`D:\FU\Term 9\...`) — nguồn xung đột mỗi lần merge.

Đã xử lý:
1. Thêm `**/Flutter/GeneratedPluginRegistrant.swift` vào `macos/.gitignore` (chỗ duy nhất còn thiếu luật).
2. `git rm --cached` 10 file sau — **file vẫn nằm trên đĩa**, chỉ thôi được git theo dõi:
   - `android/app/src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java`
   - `ios/Runner/GeneratedPluginRegistrant.h`, `.m`
   - `ios/Flutter/ephemeral/` (4 file)
   - `macos/Flutter/GeneratedPluginRegistrant.swift`
   - `macos/Flutter/ephemeral/` (2 file)

Đã kiểm chứng: chạy lại `flutter test` → file được sinh lại đầy đủ trên đĩa (có `IntegrationTestPlugin`),
`git status` sạch, `git check-ignore` xác nhận cả 4 nhóm đều khớp luật ignore.
`.gitlab-ci.yml` không có job nào build mobile nên thay đổi này không ảnh hưởng CI; máy dev chỉ cần
chạy `flutter test`/`flutter run`/`flutter build` như bình thường là file tự sinh lại.
