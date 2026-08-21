# -*- coding: utf-8 -*-
"""Use-case map: sheet name -> (module label, regex over backend FQCN / mobile+web path).

Ordered: the FIRST matching entry wins, so put narrow patterns before broad ones.
"""

USE_CASES = [
    # ---------- 1. Auth & account ----------
    ("Register Account",        "Auth / Identity",
     r"security\.service\.AuthServiceRegister|security\.service\.AuthServiceVerifyOtp|security\.service\.AuthServiceResendOtp|security\.policy\.RateLimitPolicy|auth/registration_error_message|auth/welcome_screen"),
    ("Login",                   "Auth / Identity",
     r"security\.service\.AuthServiceLogin|security\.jwt\.|security\.filter\.JwtAuthenticationFilter|auth/password_login|auth/blocked_account|auth/pages/BlockedAccountPage|auth/services/authApi|shared/api/apiClient|core/network/api_client|core/network/account_block_parser|core/network/api_exception|core/storage/token_storage|core/auth/auth_state"),
    ("Federated Login (Google)", "Auth / Identity",
     r"FederatedAuthService|FederatedAuthController|FederatedIdentityLink|auth/federated_|auth/linked_account|core/firebase/firebase_bootstrap"),
    ("Logout & Session",        "Auth / Identity",
     r"SessionServiceImpl|TokenBlacklistRepository|AuthServiceRefresh|AuthServiceDeactivate|auth/logout_confirmation"),
    ("Change / Reset Password", "Auth / Identity",
     r"PasswordComplexityPolicy|VietnamesePhoneNumbers|security\.mapper\.UserMapper|security\.config\.SecurityConfigCors"),
    ("View / Edit Profile",     "Profile",
     r"\.profile\.|AuthServiceGetProfile"),
    ("Privacy & Consent Settings", "Profile",
     r"\.privacy\.|\.consent\.|LifecycleConsentValidator"),

    # ---------- 2. Personalisation ----------
    ("Personalisation Survey",  "Recommendation",
     r"RecommendationProfileValidator|recommendation_questionnaire|recommendation_profile_screen"),
    ("System Recommendations",  "Recommendation",
     r"\.recommendation\.|features/recommendation/|mother_home_recommendation|recommendationMetadata"),

    # ---------- 3. Notification ----------
    ("Notification Center",     "Notification",
     r"NotificationView|NotificationMarkAsRead|NotificationControllerContract|features/notification/notification_model|NotificationPreference|reminder_notification_routing|family_member_home_notification"),
    ("Push Notification (FCM)", "Notification",
     r"FcmService|FirebaseConfig|NotificationDeviceToken|NotificationSendService|core/notifications/fcm_service"),
    ("Domain Notifications",    "Notification",
     r"\.notification\.|ContentReviewNotification|VaccinationNotification"),

    # ---------- 4. File ----------
    ("Upload / View / Delete File", "File Storage",
     r"\.file\.|CloudinaryStorageService|R2StorageService|StorageServiceResolver"),

    # ---------- 5. Community ----------
    ("Post Question",           "Community",
     r"CommunityQuestionServiceImpl|CommunityQuestionController(?!Security)|CommunityQuestionMapper|CommunityQuestionDetailServiceImpl|CommunityAuthorDisplayResolver|CommunityQuestionLike|CommunityAnswerLike|CommunityBookmark|community_image_|CommunityImageAttachment"),
    ("Edit / Delete Question",  "Community",
     r"CommunityQuestionEdit|CommunityQuestionDelete"),
    ("Answer Question",         "Community",
     r"CommunityAnswerServiceImpl|CommunityAnswerController|CommunityAnswerMapper|post_answer_screen|ContributionPointServiceImpl"),
    ("Community Feed",          "Community",
     r"CommunityFeed|community_feed_screen|community_question_topics|community/models/community_stage_model|community/checklist_assignment_context"),
    ("Community Topic",         "Community",
     r"CommunityTopic|TopicFollowService|SlugGenerator|topicTree|topicErrors"),
    ("Report Content",          "Community",
     r"content\.report\.|community\.policy\.CommunitySafetyPolicy"),
    ("Search",                  "Search",
     r"\.search\.|ContentSearch(Provider|Service|Controller|Security)|QuestionSearchProvider|CommunityQuestionSearch"),

    # ---------- 6. Exercise ----------
    ("Exercise Session",        "Exercise",
     r"ExerciseSessionService|ExerciseCompleteSession|ExercisePauseResume|ExerciseQueryService|ExerciseDetailQuery|ExerciseMapper|mother_exercise_screen|exercise_service_posture_event"),
    ("Posture Analysis (Camera)", "Exercise",
     r"PostureAnalysisService|GeometricPostureRules|PostureFeedbackMessages|PostureSessionTracker|PostureInferenceConfigResolver|ExerciseCorrectionHttpAdapter|posture_event_|posture_camera_|exercise_feedback_analyzer"),
    ("Exercise Safety Check",   "Exercise",
     r"ExerciseSafetyCheck|SafetyCheckPolicy"),
    ("Admin Exercise & Posture Config", "Exercise",
     r"AdminExercise|AdminPostureConfig|PostureConfigService|CreatePostureConfigRequestValidation|ExerciseControllerDetailSecurity|ExerciseSessionPostureEventsSecurity|exercise\.entity\."),

    # ---------- 7. Content ----------
    ("View Content & FAQ",      "Content",
     r"content\.unit\.ContentServiceImpl|content\.unit\.ContentController|content\.unit\.ContentMapper|content\.entity\.|StageCompatibility|ContentCategoryArchitecture|contentManagement/models/content|contentManagement/services/contentApi|community/models/content_model|community/services/content_service"),
    ("Lifecycle Content",       "Content",
     r"LifecycleContent|Story69Content|content_service_story_6_9|view_content_lifecycle|verified_content_|story_6_9_lifecycle"),
    ("Create / Update Content", "Content",
     r"UpdateContent|HtmlContentSanitizer|StaffContentDetailResponseSerialization|RichTextEditor|ContentDetailPage|tableSorting"),
    ("Hide / Unpublish Content", "Content",
     r"HideContent|UnpublishContent"),
    ("Content Approval Queue",  "Content",
     r"ContentApproval|AdminContentService|AdminContentController|ContentSecurityTest"),
    ("Checklist Template Admin", "Content",
     r"ChecklistTemplateApproval|AdminChecklistTemplate|ChecklistTemplateInlineMetadata|AdminChecklistController|ChecklistImport|OptionalChecklistTemplateImport|Checklist(List|Form|Detail|VersionHistory)Page|checklistApprovalPresentation"),

    # ---------- 8. Journey ----------
    ("Journey Onboarding",      "Journey",
     r"JourneyOnboarding|journey_onboarding_|journey_setup_screen|journey/journey_model"),
    ("Manage Pregnancy Journey", "Journey",
     r"JourneyServiceImpl|JourneyUpdateServiceImpl|JourneyDashboardServiceImpl|journey_service_post_commit|mother_journey_lifecycle"),
    ("Journey Lifecycle & Outcome", "Journey",
     r"JourneyCanonicalLifecycle|PregnancyOutcome|JourneyPregnancyOutcomePolicy|LifecycleSafetyOutcome|journey\.entity\.|pregnancy_outcome_|postpartum_recovery_|story_6_1_mobile_gap"),
    ("Baby Profile",            "Journey",
     r"\.baby\.|features/baby/add_baby_screen|features/baby/milestone_model"),
    ("Baby Care Journey",       "Journey",
     r"\.carejourney\.|baby_care_|baby_log_summary|mf03_canonical_journey"),

    # ---------- 9. Reminder / appointment ----------
    ("Create / Update Reminder", "Reminder",
     r"ReminderServiceImpl|UpdateReminderService|MedicationReminderService|reminder/reminder_model|reminder/reminder_schedule_service|reminder\.entity\.|ReminderSchedule|ReminderWorkerPropertyBinding|ReminderOccurrenceIdGeneration|JobTransitionFlushContract|ReminderSecurityTest|LegacyTodayReminderAuthorizationContract|ReminderAccessPolicy|ReminderPostLockAuthorizationContract"),
    ("Appointment",             "Reminder",
     r"CareGroupAppointment|AppointmentNotification|appointment_calendar_screen|create_appointment_reminder|appointment_notification_timing|shared_appointment_detail"),

    # ---------- 10. Today tasks / checklist ----------
    ("Today Tasks",             "Checklist",
     r"TodayTaskService|TodayTaskController|UnifiedTodayTaskService|TodayTaskContextLabelResolver|TodayTasksSequenceProjection|today_task_v2_|today_tasks_|TodayTaskProvider|ReminderProviderRepositoryScopeContract|CareGroupChecklistScopeResolver|CurrentChecklistServiceImpl"),
    ("Task Action Handling",    "Checklist",
     r"UnifiedTaskAction|UnifiedTaskPolicy|UnifiedTaskTyped|UnifiedTaskCurrentContext|CareTaskActionHandler|ChecklistTaskReopenActionHandler|ReminderTaskActionHandler|ReminderLegacyActionAdapter|ChecklistTaskCancelledParentActionContract"),
    ("User-created Checklist Task", "Checklist",
     r"UserCreatedChecklistTaskService|UserChecklistItemSystemTaskMutation|ChecklistV2CompatibilityMutation|user_checklist_|add_user_checklist_task|checklist_detail_screen"),
    ("Checklist Distribution",  "Checklist",
     r"checklist\.distribution\.|ChecklistSequenceResolver|ChecklistHistory|checklist_history_"),
    ("Checklist Retention Ops", "Checklist",
     r"checklist\.operations\."),

    # ---------- 11. Health ----------
    ("Quick Note (BMI/Water/Movement)", "Health",
     r"FamilyQuickNoteService|HealthMetricAddService|MetricObservationValidator"),
    ("EPDS Screening",          "Health",
     r"PostpartumLog|epds_screen|postpartum_log_"),
    ("Health Metric & Trend",   "Health",
     r"HealthMetricServiceImpl|HealthMetricUpdateService|MetricTrendService|MetricDefinition|health\.entity\.|health_metric_model|maternal_health_metric_screen|growth_trend_chart"),
    ("Health Record",           "Health",
     r"HealthRecordService|health\.repository\.HealthRecordFileRepository|HealthSummaryService|ShareSummaryService"),
    ("Growth & WHO Standard",   "Health",
     r"growth_measurement_form|who_growth_standard|GrowthService|GrowthMeasurement"),
    ("Vaccination",             "Health",
     r"\.vaccination\.|VaccinationReminderService|vaccination_model"),

    # ---------- 12. Safety (IMU) ----------
    ("Fall Detection (IMU)",    "Safety",
     r"FallDetection|SuspectedFallDetected|imu_fall_detector|fall_detection_sensor_service|imu_diagnostics"),
    ("Safety Monitoring & Config", "Safety",
     r"\.safety\.|safety/safety_"),

    # ---------- 13. Expert ----------
    ("Expert Directory",        "Expert",
     r"ExpertProfileServiceImplDirectory|ExpertProfileController|ExpertConsultationEligibility|expert_directory_|expertApi"),
    ("Expert Profile & Specialty", "Expert",
     r"ExpertProfileServiceImplSpecialty|ExpertProfileRequestValidation|expert_public_profile_screen|expert_app_home_screen"),
    ("Expert Onboarding & Verification", "Expert",
     r"\.expertverification\.|expert_onboarding_service|expert_identity_resume|expert_registration_service|ExpertOnboardingPage|ExpertRegisterPage|ExpertVerificationQueuePage|expert_account_generation"),
    ("Expert Availability",     "Expert",
     r"\.expertavailability\.|expert_calendar_screen"),

    # ---------- 14. Consultation & chat ----------
    ("Consultation Request",    "Consultation",
     r"ConsultationRequest(?!NotificationWriter)|consultation\.policy\.|PreferredWindowValidator|consultation_request_|expert_dashboard_consultation"),
    ("Triage → Expert Handoff", "Consultation",
     r"TriageExpertHandoff|TriageCitationResolver|triage_expert_handoff_"),
    ("Direct Conversation",     "Direct Chat",
     r"DirectConversation|TimelineCursorCodec|conversation_list_screen|direct_conversation_test|timeline_item_test|DirectMessage|direct_chat_screen|DirectChatAttachmentAccessService"),
    ("Video Call",              "Direct Chat",
     r"ConversationCall|CallTimeoutReconciliationJob|\.zegocloud\.|direct_call_|rtc_|zego_|directCall|rtcMediaPermissions|zegoRoomSession"),
    ("Firebase Realtime Bridge", "Direct Chat",
     r"integration\.firebase\.|FirebaseEventRetentionJob|firebase_token_service|firebase_conversation_signaling_port|conversationSignalingPort"),

    # ---------- 15. Care group ----------
    ("Create / Manage Care Group", "Care Group",
     r"CareGroupServiceImplTest|CareGroupJourneyRelink|family\.entity\.|CareGroupMemberRepositoryQuery|mother_care_group_ui"),
    ("Invite Family Member",    "Care Group",
     r"CareGroupServiceImplInvite|CareGroupServiceImplAcceptInvitation|CareGroupServiceImplMembershipLifecycle|CareGroupControllerInvite|InviteTokenGenerator|care_group_invitation_screen"),
    ("Care Group Permission",   "Care Group",
     r"CareGroupServiceImplPermission|CareGroupControllerPermission|CareGroupAuthorizationPolicy|CareGroupAccessPolicy"),
    ("Care Task Assignment",    "Care Group",
     r"CareTaskServiceImpl|CareGroupControllerAssignTask"),
    ("Family Shared Data & Dashboard", "Care Group",
     r"SharedDataServiceImpl|FamilyDashboardService|family_dashboard_contract|family_health_sharing_contract"),

    # ---------- 16. Emergency ----------
    ("Emergency Escalation",    "Emergency",
     r"\.emergency\.|FamilyAlertService|FamilyAlertSentHandler"),
    ("Nearby Care Facility Map", "Emergency",
     r"CareFacilityServiceImpl|\.masterdata\.|emergency_map_screen|nearby_care_contract|trackasia_web_contract"),

    # ---------- 17. AI triage ----------
    ("Symptom Intake",          "CareBridge AI",
     r"StructuredIntakeService|IntakeSessionCompletedHandler|IntakeController|SymptomNormalizer|symptom_intake_screen"),
    ("Triage Assessment",       "CareBridge AI",
     r"TriageService|TriageGraphService|TriageV2|TriageRecommendationCode|PediatricRedParity|LifecycleIntakeBindingService|Story67LifecycleContract|HttpChildTriageAiClient|risk_triage_result_screen|triage_v2_|triage_model_contract|triage_demo_visual|HealthMemory"),
    ("Triage Consent",          "CareBridge AI",
     r"TriageConsent"),
    ("Triage Continuation & History", "CareBridge AI",
     r"TriageContinuationService|triage_history_screen|story_6_7_|triage_origin_entry|floating_ai_triage_host|story_6_8_"),
    ("RAG & Gemini",            "CareBridge AI",
     r"integration\.gemini\.|TriageRagEnrichmentService|SourceRetriever|TriageSessionEvidenceWriter"),
    ("Triage Follow-up",        "CareBridge AI",
     r"TriageFollowUp|TriageYellowFollowUpHandler|LifecycleSafetyMetrics"),

    # ---------- 18. Moderation ----------
    ("Moderate Content",        "Moderation",
     r"ModerateContent|moderation\.ModerationServiceImpl|moderation\.ModerationController|ModerationMapper|ContentPreviewService|ModeratorHealthPrivacySecurity|SystemAdminEscalationControllerSecurity"),
    ("Resolve Report",          "Moderation",
     r"ResolveReport|ClaimReport"),
    ("Warn / Suspend Account",  "Moderation",
     r"WarnOrSuspendAccount"),
    ("Community Dashboard",     "Moderation",
     r"CommunityDashboard"),
    ("AI Moderation",           "Moderation",
     r"\.aimoderation\."),
    ("Audit & Security Incident", "Moderation",
     r"\.audit\."),

    # ---------- 19. Admin ----------
    ("Admin User Management",   "System Admin",
     r"identity\.admin\.|UserListPage|UserDetailPage|UpdateUserRolePage"),
    ("System Configuration",    "System Admin",
     r"\.systemconfiguration\.|SystemConfigurationPage|MaintenancePage"),

    # ---------- 20. App shell ----------
    ("Home & Navigation", "Mobile Shell",
     r"home_shell|mother_home_screen|widget_test|app_router"),

    # ---------- 21. Platform (excluded from the report, see EXCLUDED_USE_CASES) ----------
    ("Platform & Configuration", "Platform",
     r"common\.config\.|common\.exception\.|common\.dev\.|\.canonical\.|FlywayMigrationChain|MigrationContractTest|MigrationTest|BackendApplicationTests|PrintSupabaseTables"),
]


# Buckets that stay in the mapping (so every test is still accounted for) but are kept OUT of the
# Unit Test report: they are cross-cutting plumbing rather than a use case a reviewer can click
# through in the product. Decided with the project owner on 2026-08-08.
EXCLUDED_USE_CASES = {
    "Platform & Configuration",   # .env loading, exception handler, migration contracts, seeder
    "Checklist Retention Ops",    # scheduled data-retention purge job
    "Audit & Security Incident",  # cross-cutting audit trail
    "Firebase Realtime Bridge",   # technical adapter behind chat signalling
    "Search",
}


# The `Test requirement` header cell used to hold the list of source test files, but those already
# live in the MethodList sheet and in 06_Testing/UnitTesting/linkFileTest.md. The reference reports
# put an actual requirement sentence there, so that is what goes in now: what the use case must do,
# stated as the thing the test cases below are checking.
REQUIREMENTS = {
    "Register Account":
        "Đăng ký tài khoản bằng email hoặc số điện thoại Việt Nam, xác thực OTP trước khi kích hoạt, "
        "chặn trùng định danh và giới hạn tần suất gửi lại OTP.",
    "Login":
        "Đăng nhập bằng định danh + mật khẩu, cấp cặp JWT RS256, từ chối tài khoản bị khoá/chưa kích "
        "hoạt và trả về thông báo không tiết lộ tài khoản nào tồn tại.",
    "Federated Login (Google)":
        "Đăng nhập Google: xác minh ID token Firebase, liên kết hoặc tạo tài khoản nội bộ, và chặn "
        "liên kết một danh tính ngoài vào nhiều tài khoản CareBridge.",
    "Logout & Session":
        "Đăng xuất thu hồi refresh token đưa vào blacklist, làm mới token chỉ chấp nhận token còn "
        "hiệu lực và chưa thu hồi.",
    "Change / Reset Password":
        "Mật khẩu phải đủ độ phức tạp (độ dài tối thiểu, chữ hoa, chữ số, ký tự đặc biệt); số điện "
        "thoại được chuẩn hoá theo định dạng Việt Nam trước khi dùng làm định danh.",
    "View / Edit Profile":
        "Người dùng xem và cập nhật hồ sơ của chính mình; API trả DTO, không lộ entity JPA hay "
        "trường nhạy cảm như hash mật khẩu.",
    "Privacy & Consent Settings":
        "Người dùng bật/tắt từng phạm vi chia sẻ dữ liệu; đồng ý có thời hạn và hết hạn thì mặc "
        "định là từ chối.",
    "Personalisation Survey":
        "Khảo sát cá nhân hoá lưu câu trả lời theo người dùng, cho phép làm lại và không chặn luồng "
        "sử dụng nếu bỏ qua.",
    "System Recommendations":
        "Hệ thống gợi ý nội dung/việc cần làm dựa trên giai đoạn hành trình, không gợi ý nội dung "
        "chưa xuất bản hoặc ngoài phạm vi quyền.",
    "Notification Center":
        "Trung tâm thông báo liệt kê theo người nhận, hỗ trợ đánh dấu đã đọc từng mục và tất cả, "
        "đếm chưa đọc chính xác.",
    "Push Notification (FCM)":
        "Đăng ký/huỷ đăng ký device token, gửi push qua FCM và xử lý token hỏng mà không làm hỏng "
        "giao dịch nghiệp vụ gọi nó.",
    "Domain Notifications":
        "Các nghiệp vụ (nhắc lịch, lịch hẹn, nhóm chăm sóc, chuyên gia) phát sinh thông báo đúng "
        "người nhận, đúng loại, không trùng lặp.",
    "Upload / View / Delete File":
        "Tải tệp lên kiểm tra loại và dung lượng, chỉ chủ sở hữu hoặc người được cấp quyền mới xem "
        "và xoá được.",
    "Post Question":
        "Đăng câu hỏi cộng đồng yêu cầu tiêu đề/nội dung hợp lệ, gắn chủ đề, và đi qua kiểm duyệt "
        "trước khi hiển thị công khai.",
    "Edit / Delete Question":
        "Chỉ tác giả sửa được câu hỏi của mình; xoá thì tác giả hoặc MODERATOR đều được, còn vai trò "
        "khác bị từ chối. Bật ẩn danh không được để lộ authorId trong phản hồi.",
    "Answer Question":
        "Trả lời câu hỏi hiển thị theo trạng thái duyệt; câu trả lời của chuyên gia được đánh dấu "
        "riêng và không bị lẫn với câu trả lời thường.",
    "Community Feed":
        "Feed cộng đồng phân trang ổn định, chỉ trả nội dung đã duyệt và còn hiển thị, sắp xếp theo "
        "tiêu chí đã định.",
    "Community Topic":
        "Chủ đề cộng đồng do quản trị nội dung tạo/sửa; câu hỏi phải thuộc chủ đề đang hoạt động.",
    "Report Content":
        "Người dùng báo cáo nội dung vi phạm kèm lý do; mỗi người báo cáo một nội dung một lần, báo "
        "cáo vào hàng đợi kiểm duyệt.",
    "Exercise Session":
        "Phiên tập tạo theo bài tập đã chọn, ghi nhận tiến độ và kết thúc phiên đúng trạng thái.",
    "Posture Analysis (Camera)":
        "Phân tích tư thế thời gian thực từ khung hình camera, trả điểm/nhãn tư thế và không gửi "
        "ảnh thô ra ngoài phạm vi cho phép.",
    "Exercise Safety Check":
        "Chặn bài tập không an toàn theo giai đoạn thai kỳ/bệnh nền và luôn hiển thị cảnh báo dừng "
        "tập khi có dấu hiệu nguy hiểm.",
    "Admin Exercise & Posture Config":
        "Quản trị cấu hình bài tập và ngưỡng phân tích tư thế; thay đổi cấu hình phải được ghi vết.",
    "View Content & FAQ":
        "Người dùng chỉ đọc được bài viết và FAQ ở trạng thái đã xuất bản, kèm phân trang và lọc "
        "theo chủ đề.",
    "Lifecycle Content":
        "Nội dung theo vòng đời (tuần thai, tháng tuổi bé) chỉ hiển thị đúng giai đoạn hiện tại của "
        "người dùng.",
    "Create / Update Content":
        "Quản trị nội dung soạn/sửa bài viết và FAQ; bản nháp không lộ ra ngoài, mỗi lần sửa ghi "
        "nhận người sửa và thời điểm.",
    "Hide / Unpublish Content":
        "Ẩn hoặc gỡ xuất bản nội dung khiến nội dung biến mất khỏi mọi API đọc công khai ngay lập tức.",
    "Content Approval Queue":
        "Hàng đợi duyệt nội dung chỉ dành cho vai trò được phép; duyệt/từ chối ghi vết và chuyển "
        "trạng thái đúng một lần.",
    "Checklist Template Admin":
        "Quản trị mẫu checklist theo giai đoạn; sửa mẫu không được phá dữ liệu checklist người dùng "
        "đã phát sinh.",
    "Journey Onboarding":
        "Khởi tạo hành trình dùng chủ sở hữu lấy từ token đăng nhập, không nhận ownerId từ client.",
    "Manage Pregnancy Journey":
        "Quản lý hành trình mang thai: ngày dự sinh và tuần thai được tính nhất quán; chủ sở hữu "
        "hành trình luôn lấy từ người gọi đã đăng nhập, không nhận từ dữ liệu client gửi lên.",
    "Journey Lifecycle & Outcome":
        "Chuyển trạng thái hành trình (đang theo dõi, kết thúc, kết quả) theo đúng máy trạng thái, "
        "không cho quay ngược trạng thái cuối.",
    "Baby Profile":
        "Hồ sơ bé gắn với hành trình/chủ sở hữu; từ chối các thuộc tính liên kết cũ gửi kèm ở ranh "
        "giới JSON.",
    "Baby Care Journey":
        "Hành trình chăm sóc sau sinh theo tháng tuổi bé, sinh nội dung và nhiệm vụ đúng giai đoạn.",
    "Create / Update Reminder":
        "Tạo/sửa nhắc nhở với thời điểm và chu kỳ hợp lệ; chỉ chủ sở hữu hoặc thành viên nhóm được "
        "cấp quyền mới thao tác được.",
    "Appointment":
        "Lịch hẹn khám lưu thời gian, nơi khám và trạng thái; nhắc trước lịch hẹn và không cho đặt "
        "lịch trong quá khứ.",
    "Today Tasks":
        "Danh sách việc hôm nay gộp nhắc nhở, lịch hẹn và checklist theo đúng phạm vi quyền của "
        "người đang đăng nhập.",
    "Task Action Handling":
        "Hoàn thành/bỏ qua/hoãn một việc ghi vào sổ hành động một lần duy nhất và cập nhật đúng "
        "trạng thái nguồn.",
    "User-created Checklist Task":
        "Người dùng tự thêm việc vào checklist của mình; việc tự tạo không bị mẫu hệ thống ghi đè.",
    "Checklist Distribution":
        "Phân phối checklist theo giai đoạn tới đúng người dùng, nâng cấp mẫu giữ nguyên dữ liệu đã "
        "làm và lịch sử hành động.",
    "Quick Note (BMI/Water/Movement)":
        "Ghi chú nhanh BMI, lượng nước, cử động thai: kiểm tra miền giá trị và tổng hợp theo ngày.",
    "EPDS Screening":
        "Sàng lọc trầm cảm sau sinh EPDS tính điểm theo thang chuẩn và kích hoạt cảnh báo khi vượt "
        "ngưỡng, không tự chẩn đoán.",
    "Health Metric & Trend":
        "Chỉ số sức khoẻ lưu theo thời điểm, dựng biểu đồ xu hướng và từ chối giá trị âm hoặc không "
        "hữu hạn.",
    "Health Record":
        "Hồ sơ sức khoẻ chỉ chủ sở hữu và người được cấp quyền còn hiệu lực mới đọc được; mọi truy "
        "cập của người khác đều bị từ chối.",
    "Growth & WHO Standard":
        "Tăng trưởng của bé so với chuẩn WHO theo tuổi/giới; sửa bản ghi không được ghi đè trường "
        "chưa thay đổi.",
    "Vaccination":
        "Lịch tiêm chủng theo tuổi bé, đánh dấu đã tiêm và nhắc mũi sắp tới.",
    "Fall Detection (IMU)":
        "Phát hiện té ngã từ cảm biến IMU theo ngưỡng cấu hình, gửi cảnh báo tới người thân và cho "
        "phép người dùng huỷ báo động giả.",
    "Safety Monitoring & Config":
        "Cấu hình ngưỡng giám sát an toàn và người nhận cảnh báo; thay đổi cấu hình được ghi vết.",
    "Expert Directory":
        "Danh bạ chuyên gia chỉ hiển thị chuyên gia đã được xác minh và đang hoạt động, lọc theo "
        "chuyên môn.",
    "Expert Profile & Specialty":
        "Hồ sơ chuyên gia và chuyên môn do chính chuyên gia cập nhật; thông tin xác minh không tự "
        "sửa được.",
    "Expert Onboarding & Verification":
        "Đăng ký làm chuyên gia nộp bằng cấp/chứng chỉ; chỉ quản trị duyệt mới chuyển sang trạng "
        "thái đã xác minh, kèm ghi vết.",
    "Expert Availability":
        "Chuyên gia khai báo khung giờ rảnh; khung giờ chồng lấn bị từ chối và slot đã đặt không bị "
        "xoá mất.",
    "Consultation Request":
        "Yêu cầu tư vấn gửi tới chuyên gia, chuyên gia chấp nhận hoặc từ chối một lần; hết hạn thì "
        "tự đóng.",
    "Triage → Expert Handoff":
        "Chuyển kết quả sàng lọc sang chuyên gia chỉ gửi các trường đã duyệt, có làm sạch nội dung "
        "và tôn trọng phạm vi đồng ý.",
    "Direct Conversation":
        "Hội thoại trực tiếp giữa người dùng và chuyên gia chỉ mở cho hai bên trong cuộc; tin nhắn "
        "lưu đúng thứ tự.",
    "Video Call":
        "Cuộc gọi video cấp token phiên theo cuộc hội thoại đã có quyền; không cấp token cho người "
        "ngoài cuộc.",
    "Create / Manage Care Group":
        "Tạo và quản lý nhóm chăm sóc; chỉ chủ nhóm mới đổi cấu hình nhóm hoặc giải tán nhóm.",
    "Invite Family Member":
        "Mời người thân bằng lời mời có hạn dùng; lời mời hết hạn hoặc đã dùng thì không nhận lại "
        "được.",
    "Care Group Permission":
        "Quyền trong nhóm chăm sóc quy định phạm vi dữ liệu mỗi thành viên đọc/ghi được; thu hồi "
        "quyền có hiệu lực ngay.",
    "Care Task Assignment":
        "Giao việc trong nhóm cho thành viên có quyền; người được giao mới cập nhật được trạng thái "
        "việc đó.",
    "Family Shared Data & Dashboard":
        "Bảng tổng quan cho người thân chỉ hiển thị dữ liệu nằm trong phạm vi đồng ý còn hiệu lực.",
    "Emergency Escalation":
        "Luồng khẩn cấp gọi 115 và báo người thân phải luôn hiển thị, không bị AI hay bước xác nhận "
        "nào che đi hoặc làm chậm.",
    "Nearby Care Facility Map":
        "Bản đồ cơ sở y tế gần nhất tra cứu theo vị trí hiện tại, có phương án dự phòng khi dịch vụ "
        "bản đồ lỗi.",
    "Symptom Intake":
        "Khai báo triệu chứng kiểm tra dữ liệu đầu vào ở ranh giới request, không nhận ngữ cảnh sức "
        "khoẻ nhét thêm từ client.",
    "Triage Assessment":
        "Đánh giá mức độ khẩn theo triệu chứng, trả mức phân loại và hướng xử trí; không chẩn đoán "
        "và không kê đơn.",
    "Triage Red Flag Rules":
        "Quy tắc dấu hiệu nguy hiểm chỉ SYSTEM_ADMIN được tạo/sửa/xoá/đọc; mọi vai trò khác bị từ "
        "chối ở mọi endpoint.",
    "Triage Consent":
        "Sàng lọc chỉ chạy khi có đồng ý còn hiệu lực; đồng ý hết hạn hoặc bị rút thì dừng và không "
        "lưu dữ liệu mới.",
    "Triage Continuation & History":
        "Tiếp tục phiên sàng lọc đang dở và tra cứu lịch sử theo đúng chủ sở hữu phiên.",
    "RAG & Gemini":
        "Trợ lý CareBridge AI trả lời dựa trên tri thức đã duyệt, luôn kèm khuyến cáo và định tuyến "
        "cấp cứu khi phát hiện dấu hiệu nguy hiểm.",
    "Triage Follow-up":
        "Theo dõi sau sàng lọc nhắc người dùng kiểm tra lại và ghi nhận diễn biến.",
    "Moderate Content":
        "Kiểm duyệt viên duyệt/ẩn/gỡ nội dung trong hàng đợi; mỗi nội dung chỉ xử lý một lần và "
        "luôn ghi vết.",
    "Resolve Report":
        "Xử lý báo cáo vi phạm với kết luận rõ ràng, cập nhật trạng thái báo cáo và nội dung liên "
        "quan.",
    "Warn / Suspend Account":
        "Cảnh cáo hoặc khoá tài khoản vi phạm; tài khoản bị khoá không đăng nhập được và có luồng "
        "khiếu nại.",
    "Community Dashboard":
        "Bảng điều khiển cộng đồng tổng hợp số liệu kiểm duyệt theo đúng phạm vi quyền của người xem.",
    "AI Moderation":
        "Kiểm duyệt bằng AI: chính sách chỉ SYSTEM_ADMIN quản lý, endpoint trạng thái mở thêm cho "
        "MODERATOR, và payload không bao giờ lộ khoá API.",
    "Admin User Management":
        "Quản trị hệ thống xem/khoá/mở khoá tài khoản và xử lý khiếu nại khoá; thao tác ghi vết đầy đủ.",
    "System Configuration":
        "Cấu hình hệ thống chỉ SYSTEM_ADMIN đọc và sửa được (@PreAuthorize cấp class trên "
        "/api/v1/admin/system-configuration); giá trị cấu hình được kiểm tra trước khi áp dụng.",
    "Home & Navigation":
        "Điều hướng và màn hình chính hiển thị đúng theo vai trò, giữ được khả năng truy cập ở cỡ "
        "chữ và hướng màn hình khác nhau.",
}
