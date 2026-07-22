import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../auth/auth_state.dart';
import '../../features/auth/screens/welcome_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/blocked_account_screen.dart';
import '../../features/auth/screens/auth_landing_screen.dart';
import '../../features/auth/screens/role_selection_screen.dart';
import '../../features/home/screens/home_shell.dart';
import '../../features/home/screens/expert_home_shell.dart';
import '../../features/home/screens/family_member_home_screen.dart';
import '../../features/journey/screens/mother_stage_selection_screen.dart';
import '../../features/journey/screens/journey_onboarding_screen.dart';
import '../../features/journey/screens/journey_setup_screen.dart';
import '../../features/journey/screens/postpartum_recovery_setup_screen.dart';

import '../../features/healthRecords/screens/maternal_health_metric_screen.dart';
import '../../features/healthRecords/screens/health_record_timeline_screen.dart';
import '../../features/healthRecords/screens/add_maternal_health_metric_screen.dart';
import '../../features/healthRecords/screens/edit_health_metric_screen.dart';
import '../../features/healthRecords/screens/postpartum_log_list_screen.dart';
import '../../features/healthRecords/screens/postpartum_log_detail_screen.dart';
import '../../features/healthRecords/screens/postpartum_log_form_screen.dart';
import '../../features/healthRecords/screens/postpartum_safety_help_screen.dart';
import '../../features/healthRecords/models/postpartum_log_model.dart';
import '../../features/healthRecords/screens/health_metric_trend_screen.dart';
import '../../features/healthRecords/models/health_metric_model.dart';
import '../../features/baby/screens/edit_baby_profile_screen.dart';
import '../../features/baby/screens/edit_baby_daily_log_screen.dart';
import '../../features/baby/screens/baby_daily_log_detail_screen.dart';
import '../../features/baby/screens/baby_log_summary_screen.dart';
import '../../features/baby/screens/development_milestone_detail_screen.dart';
import '../../features/baby/screens/record_milestone_screen.dart';
import '../../features/baby/models/baby_daily_log_model.dart';
import '../../features/baby/models/milestone_model.dart';

import '../../features/healthRecords/screens/edit_health_record_screen.dart';

import '../../features/reminder/screens/create_medication_reminder_screen.dart';
import '../../features/reminder/screens/create_vaccination_reminder_screen.dart';
import '../../features/reminder/screens/update_snooze_reminder_screen.dart';
import '../../features/reminder/screens/all_reminders_screen.dart';
import '../../features/reminder/models/reminder_model.dart';

import '../../features/fileManager/screens/file_viewer_screen.dart';
import '../../features/fileManager/screens/shared_file_viewer_screen.dart';

import '../../features/reminder/screens/reminder_detail_screen.dart';
import '../../features/familySync/screens/care_groups_screen.dart';
import '../../features/familySync/screens/care_group_members_screen.dart';
import '../../features/familySync/screens/pending_invitations_screen.dart';
import '../../features/baby/screens/baby_profiles_screen.dart';
import '../../features/baby/screens/baby_care_hub_screen.dart';
import '../../features/baby/screens/baby_profile_detail_screen.dart';
import '../../features/baby/screens/add_baby_screen.dart';
import '../../features/baby/screens/baby_journey_linkage_screen.dart';
import '../../features/fileManager/screens/upload_file_screen.dart';
import '../../features/healthRecords/screens/vaccination_detail_screen.dart';
import '../../features/healthRecords/models/vaccination_model.dart';
import '../../features/healthRecords/screens/growth_measurement_history_screen.dart';
import '../../features/healthRecords/screens/add_vaccination_record_screen.dart';
import '../../features/community/screens/view_content_screen.dart';
import '../../features/aiTriage/models/triage_entry_context.dart';
import '../../features/aiTriage/screens/symptom_intake_screen.dart';
import '../../features/aiTriage/screens/risk_triage_result_screen.dart';
import '../../features/emergency/screens/emergency_map_screen.dart';
import '../../features/emergency/screens/emergency_alert_detail_screen.dart';
import '../../features/emergency/screens/family_alert_detail_screen.dart';
import '../../features/safety/screens/safety_monitoring_screen.dart';
import '../../features/safety/screens/enable_fall_detection_screen.dart';
import '../../features/aiTriage/screens/rag_chat_screen.dart';
import '../../features/expert/screens/expert_question_queue_screen.dart';
import '../../features/expert/screens/expert_profile_setup_screen.dart';
import '../../features/expert/screens/upload_verification_docs_screen.dart';
import '../../features/expert/screens/verification_status_screen.dart';
import '../../features/expert/screens/expert_public_profile_screen.dart';
import '../../features/expert/screens/expert_contribution_list_screen.dart';
import '../../features/expert/screens/expert_contribution_draft_screen.dart';
import '../../features/expert/screens/expert_contribution_detail_screen.dart';
import '../../features/expert/screens/expert_calendar_screen.dart';
import '../../features/expert/screens/expert_nearby_support_screen.dart';
import '../../features/expert/screens/expert_onboarding_gate_screen.dart';
import '../../features/expert/screens/expert_identity_capture_screen.dart';
import '../../features/expert/services/expert_onboarding_store.dart';
import '../../features/directChat/screens/expert_directory_screen.dart';
import '../../features/directChat/screens/conversation_list_screen.dart';
import '../../features/directChat/screens/direct_chat_screen.dart';
import '../../features/consultation/screens/my_consultation_requests_screen.dart';
import '../../features/consultation/screens/consultation_request_detail_screen.dart';

Widget _buildHomeForRole(String? role, {required int initialIndex}) {
  switch ((role ?? '').trim().toUpperCase()) {
    case 'FAMILY':
      return const FamilyMemberHomeScreen();
    case 'EXPERT':
      return const ExpertHomeShell();
    case 'MOTHER':
      return HomeShell(initialIndex: initialIndex);
    default:
      return const _UnsupportedRoleHome();
  }
}

bool _isUuid(String? value) =>
    value != null &&
    RegExp(
      r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$',
    ).hasMatch(value);

class _InvalidRouteScreen extends StatelessWidget {
  const _InvalidRouteScreen();

  @override
  Widget build(BuildContext context) => const Scaffold(
    body: Center(
      child: Padding(
        padding: EdgeInsets.all(24),
        child: Text(
          'Liên kết nhật ký không hợp lệ.',
          textAlign: TextAlign.center,
        ),
      ),
    ),
  );
}

class _UnsupportedRoleHome extends StatelessWidget {
  const _UnsupportedRoleHome();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text(
            'Vai trò tài khoản chưa được hỗ trợ trên mobile app.',
            textAlign: TextAlign.center,
            style: TextStyle(fontFamily: 'Lexend', fontSize: 16),
          ),
        ),
      ),
    );
  }
}

/// Global router key for context-less navigation if needed
final GlobalKey<NavigatorState> rootNavigatorKey = GlobalKey<NavigatorState>();

@visibleForTesting
String? resolveAppRedirect({
  required bool isAuthenticated,
  required bool isRestoring,
  required String? blockedReason,
  required String? role,
  required String location,
}) {
  final hasAssignedRole = role != null && role.trim().isNotEmpty;

  if (isRestoring) return null;

  final isAuthRoute =
      location.startsWith('/welcome') ||
      location.startsWith('/login') ||
      location == '/auth-landing';

  if (blockedReason != null && location != '/blocked') return '/blocked';
  if (!isAuthenticated && !isAuthRoute) return '/welcome';
  if (isAuthenticated && !hasAssignedRole && location != '/role-selection') {
    return '/role-selection';
  }
  if (isAuthenticated && hasAssignedRole && location == '/role-selection') {
    return role == 'EXPERT'
        ? '/expert-onboarding'
        : (role == 'MOTHER' ? '/journey-onboarding' : '/');
  }
  if (isAuthenticated && isAuthRoute && location != '/auth-landing') {
    return role == 'EXPERT'
        ? '/expert-onboarding'
        : (role == 'MOTHER'
              ? '/auth-landing'
              : (hasAssignedRole ? '/' : '/role-selection'));
  }
  if (isAuthenticated && location == '/auth-landing') {
    return role == 'MOTHER'
        ? null
        : (role == 'EXPERT'
              ? '/expert-onboarding'
              : (hasAssignedRole ? '/' : '/role-selection'));
  }

  // `/` is the app-start dispatcher for mothers. The landing screen verifies
  // both the journey and the required consent before opening the real home.
  if (isAuthenticated && role == 'MOTHER' && location == '/') {
    return '/auth-landing';
  }
  return null;
}

final GoRouter appRouter = GoRouter(
  navigatorKey: rootNavigatorKey,
  initialLocation: '/',
  refreshListenable: AuthState.instance,
  redirect: (context, state) {
    final auth = AuthState.instance;
    final isExpert = auth.role?.trim().toUpperCase() == 'EXPERT';
    final isExpertOnboardingRoute =
        state.matchedLocation == '/expert-onboarding' ||
        state.matchedLocation == '/expert-profile-setup' ||
        state.matchedLocation == '/expert/identity' ||
        state.matchedLocation == '/expert/credentials' ||
        state.matchedLocation == '/expert-verification-status';

    final baseRedirect = resolveAppRedirect(
      isAuthenticated: auth.isAuthenticated,
      isRestoring: auth.isRestoring,
      blockedReason: auth.blockedReason,
      role: auth.role,
      location: state.matchedLocation,
    );
    if (baseRedirect != null) return baseRedirect;

    if (auth.isAuthenticated &&
        isExpert &&
        !isExpertOnboardingRoute &&
        !ExpertOnboardingStore.instance.approvedFor(auth.userId)) {
      return '/expert-onboarding';
    }

    return null;
  },
  routes: [
    GoRoute(
      path: '/welcome',
      builder: (context, state) => const WelcomeScreen(),
    ),
    GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
    GoRoute(
      path: '/blocked',
      builder: (context, state) => const BlockedAccountScreen(),
    ),
    GoRoute(
      path: '/role-selection',
      builder: (context, state) => const RoleSelectionScreen(),
    ),
    GoRoute(
      path: '/auth-landing',
      builder: (context, state) => const AuthLandingScreen(),
    ),
    GoRoute(
      path: '/journey-onboarding',
      builder: (context, state) => const JourneyOnboardingScreen(),
    ),
    GoRoute(
      path: '/mother-stage-selection',
      builder: (context, state) => const MotherStageSelectionScreen(),
    ),
    GoRoute(
      path: '/postpartum-recovery-setup',
      builder: (context, state) => const PostpartumRecoverySetupScreen(),
    ),
    GoRoute(
      path: '/',
      builder: (context, state) {
        final tabParam = state.uri.queryParameters['tab'];
        final int initialIndex = tabParam != null
            ? int.tryParse(tabParam) ?? 0
            : 0;
        return _buildHomeForRole(
          AuthState.instance.role,
          initialIndex: initialIndex,
        );
      },
    ),
    GoRoute(
      path: '/mother-home',
      builder: (context, state) {
        final tabParam = state.uri.queryParameters['tab'];
        return HomeShell(initialIndex: int.tryParse(tabParam ?? '') ?? 0);
      },
    ),
    GoRoute(
      path: '/journey-setup',
      builder: (context, state) {
        final journeyId = state.uri.queryParameters['journeyId'];
        return JourneySetupScreen(
          journeyId: journeyId,
          isEditMode:
              state.uri.queryParameters['mode'] == 'edit' &&
              journeyId != null &&
              journeyId.isNotEmpty,
          isPrePregnancyTransition:
              state.uri.queryParameters['transition'] == 'pre-pregnancy',
        );
      },
    ),
    GoRoute(
      path: '/journey-update',
      builder: (context, state) => const Scaffold(
        body: Center(child: Text('Update Mother Journey Screen (3.3.1.2)')),
      ),
    ),
    GoRoute(
      path: '/postpartum-logs',
      builder: (context, state) {
        final journeyId = state.uri.queryParameters['journeyId'];
        return _isUuid(journeyId)
            ? PostpartumLogListScreen(journeyId: journeyId!)
            : const _InvalidRouteScreen();
      },
    ),
    GoRoute(
      path: '/postpartum-logs/new',
      builder: (context, state) {
        final journeyId = state.uri.queryParameters['journeyId'];
        return _isUuid(journeyId)
            ? PostpartumLogFormScreen(journeyId: journeyId!)
            : const _InvalidRouteScreen();
      },
    ),
    GoRoute(
      path: '/postpartum-logs/:logId',
      builder: (context, state) {
        final logId = state.pathParameters['logId'];
        return _isUuid(logId)
            ? PostpartumLogDetailScreen(logId: logId!)
            : const _InvalidRouteScreen();
      },
    ),
    GoRoute(
      path: '/postpartum-logs/:logId/edit',
      builder: (context, state) {
        final logId = state.pathParameters['logId'];
        final initial = state.extra as PostpartumLog?;
        return _isUuid(logId) && initial != null
            ? PostpartumLogFormScreen(
                journeyId: initial.journeyId,
                logId: logId,
                initialLog: initial,
              )
            : const _InvalidRouteScreen();
      },
    ),
    GoRoute(
      path: '/postpartum-safety-help',
      builder: (context, state) => const PostpartumSafetyHelpScreen(),
    ),
    GoRoute(
      path: '/health-metrics/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        final extra = state.extra as Map<String, dynamic>?;
        return MaternalHealthMetricScreen(
          metricId: id,
          initialMetric: extra?['metric'] as HealthMetricDetail?,
        );
      },
    ),
    GoRoute(
      path: '/journeys/:journeyId/metrics/add',
      builder: (context, state) {
        final journeyId = state.pathParameters['journeyId'] ?? '';
        final metricType = state.uri.queryParameters['metricType'] ?? 'WEIGHT';
        return AddMaternalHealthMetricScreen(
          journeyId: journeyId,
          initialMetricType: metricType,
        );
      },
    ),
    GoRoute(
      path: '/health-records',
      builder: (context, state) => const HealthRecordTimelineScreen(),
    ),
    // UC-144 (redesign, CB-CHAT-IMP-144D) — Direct Consult Chat & Call. Role/authorization
    // is always resolved server-side from the JWT, never from a client-supplied flag.
    GoRoute(
      path: '/experts',
      builder: (context, state) => const ExpertDirectoryScreen(),
    ),
    GoRoute(
      path: '/direct-chats',
      builder: (context, state) => const ConversationListScreen(),
    ),
    GoRoute(
      path: '/direct-chat/:conversationId',
      builder: (context, state) {
        final conversationId = state.pathParameters['conversationId'] ?? '';
        return DirectChatScreen(conversationId: conversationId);
      },
    ),
    GoRoute(
      path: '/consultation-requests',
      builder: (context, state) => const MyConsultationRequestsScreen(),
    ),
    GoRoute(
      path: '/consultation-requests/:requestId',
      builder: (context, state) => ConsultationRequestDetailScreen(
        requestId: state.pathParameters['requestId'] ?? '',
      ),
    ),
    GoRoute(
      path: '/reminders/all',
      builder: (context, state) => const AllRemindersScreen(),
    ),
    GoRoute(
      path: '/health-records/add',
      builder: (context, state) => const Scaffold(
        body: Center(child: Text('Add Health Record Screen (3.3.1.16)')),
      ),
    ),
    GoRoute(
      path: '/health-records/detail/:id',
      builder: (context, state) => const Scaffold(
        body: Center(child: Text('Health Record Detail Screen (3.3.15.1)')),
      ),
    ),
    GoRoute(
      path: '/upload-file',
      builder: (context, state) => const UploadFileScreen(),
    ),
    GoRoute(
      path: '/reminders/add',
      builder: (context, state) => const Scaffold(
        body: Center(
          child: Text('Create Appointment Reminder Screen (3.3.1.22)'),
        ),
      ),
    ),
    GoRoute(
      path: '/reminders/detail/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return ReminderDetailScreen(reminderId: id);
      },
    ),
    GoRoute(
      path: '/care-groups',
      builder: (context, state) => const CareGroupsScreen(),
    ),
    GoRoute(
      path: '/care-groups/add',
      builder: (context, state) => const Scaffold(
        body: Center(child: Text('Create Care Group Screen (3.3.1.47)')),
      ),
    ),
    GoRoute(
      path: '/care-groups/members/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return CareGroupMembersScreen(
          groupId: id,
          groupName: 'Nhóm $id',
          members: const [],
        );
      },
    ),
    GoRoute(
      path: '/care-groups/invitations',
      builder: (context, state) => const PendingInvitationsScreen(),
    ),
    GoRoute(
      path: '/babies',
      builder: (context, state) => const BabyProfilesScreen(),
    ),
    GoRoute(
      path: '/baby-care-hub',
      builder: (context, state) => const BabyCareHubScreen(),
    ),
    GoRoute(
      path: '/babies/add',
      builder: (context, state) {
        final entry = state.uri.queryParameters['entry'];
        final relatedJourneyId = state.uri.queryParameters['relatedJourneyId'];
        return AddBabyScreen(
          entryPoint: entry == 'onboarding'
              ? AddBabyEntryPoint.onboarding
              : AddBabyEntryPoint.profileList,
          relatedJourneyId: relatedJourneyId,
        );
      },
    ),
    GoRoute(
      path: '/journeys/:journeyId/babies',
      builder: (context, state) {
        final journeyId = state.pathParameters['journeyId'] ?? '';
        return BabyJourneyLinkageScreen(
          journeyId: journeyId,
          onCreate: () => context.push<bool>(
            '/babies/add?entry=list&relatedJourneyId=${Uri.encodeComponent(journeyId)}',
          ),
        );
      },
    ),
    GoRoute(
      path: '/babies/detail/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return BabyProfileDetailScreen(babyId: id);
      },
    ),
    GoRoute(
      path: '/babies/:id/edit',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return EditBabyProfileScreen(babyId: id);
      },
    ),
    GoRoute(
      path: '/babies/:babyId/daily-logs/:logId',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        final logId = state.pathParameters['logId'] ?? '';
        return BabyDailyLogDetailScreen(babyId: babyId, logId: logId);
      },
    ),
    GoRoute(
      path: '/babies/:babyId/daily-logs/:logId/edit',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        final logId = state.pathParameters['logId'] ?? '';
        final initialLog = state.extra as BabyDailyLog?;
        return EditBabyDailyLogScreen(
          babyId: babyId,
          logId: logId,
          initialLog: initialLog,
        );
      },
    ),
    GoRoute(
      path: '/babies/:babyId/log-summary',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        return BabyLogSummaryScreen(babyId: babyId);
      },
    ),
    GoRoute(
      path: '/babies/:babyId/growth',
      builder: (context, state) => GrowthMeasurementHistoryScreen(
        babyId: state.pathParameters['babyId'] ?? '',
      ),
    ),
    GoRoute(
      path: '/babies/:babyId/vaccinations/add',
      builder: (context, state) => AddVaccinationRecordScreen(
        babyId: state.pathParameters['babyId'] ?? '',
      ),
    ),
    GoRoute(
      path: '/babies/:babyId/milestones/add',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        return RecordMilestoneScreen(babyId: babyId);
      },
    ),
    GoRoute(
      path: '/babies/:babyId/milestones/:milestoneId',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        final milestoneId = state.pathParameters['milestoneId'] ?? '';
        return DevelopmentMilestoneDetailScreen(
          babyId: babyId,
          milestoneId: milestoneId,
          initialMilestone: state.extra as Milestone?,
        );
      },
    ),
    GoRoute(
      path: '/health-records/:id/edit',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return EditHealthRecordScreen(recordId: id);
      },
    ),
    GoRoute(
      path: '/reminders/medication/add',
      builder: (context, state) => const CreateMedicationReminderScreen(),
    ),
    GoRoute(
      path: '/reminders/vaccination/add',
      builder: (context, state) => const CreateVaccinationReminderScreen(),
    ),
    GoRoute(
      path: '/reminders/:id/manage',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        final initialReminder = state.extra as Reminder?;
        return UpdateSnoozeReminderScreen(
          reminderId: id,
          initialReminder: initialReminder,
        );
      },
    ),
    GoRoute(
      path: '/files/:fileId/view',
      builder: (context, state) {
        final fileId = state.pathParameters['fileId'] ?? '';
        return FileViewerScreen(
          fileId: fileId,
          fileName:
              (state.extra as Map<String, dynamic>?)?['fileName'] as String?,
        );
      },
    ),
    GoRoute(
      path: '/files/:fileId/shared-view',
      builder: (context, state) {
        final fileId = state.pathParameters['fileId'] ?? '';
        final extra = state.extra as Map<String, dynamic>?;
        return SharedFileViewerScreen(
          fileId: fileId,
          expertName: extra?['expertName'] as String?,
          expertAvatarUrl: extra?['expertAvatarUrl'] as String?,
          patientName: extra?['patientName'] as String?,
          consultationLink: extra?['consultationLink'] as String?,
          accessExpiry: extra?['accessExpiry'] as DateTime?,
          accessPurpose: extra?['accessPurpose'] as String?,
        );
      },
    ),
    GoRoute(
      path: '/health-metrics/:metricId/edit',
      builder: (context, state) {
        final extra = state.extra as Map<String, dynamic>?;
        final journeyId = extra?['journeyId'] as String? ?? '';
        final metric = extra?['metric'] as HealthMetricDetail?;
        if (metric == null) {
          return const Scaffold(body: Center(child: Text('Metric not found')));
        }
        return EditHealthMetricScreen(journeyId: journeyId, metric: metric);
      },
    ),
    GoRoute(
      path: '/journeys/:journeyId/metrics/trend',
      builder: (context, state) {
        final journeyId = state.pathParameters['journeyId'] ?? '';
        final metricType = state.uri.queryParameters['metricType'];
        return HealthMetricTrendScreen(
          journeyId: journeyId,
          initialMetricType: metricType,
        );
      },
    ),
    GoRoute(
      path: '/babies/:babyId/vaccinations/:recordId',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        final recordId = state.pathParameters['recordId'] ?? '';
        return VaccinationDetailScreen(
          babyId: babyId,
          vaccinationId: recordId,
          initialRecord: state.extra as VaccinationRecord?,
        );
      },
    ),
    GoRoute(
      path: '/vaccination/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return VaccinationDetailScreen(
          babyId: state.uri.queryParameters['babyId'] ?? '',
          vaccinationId: id,
          initialRecord: state.extra as VaccinationRecord?,
        );
      },
    ),
    GoRoute(
      path: '/content',
      builder: (context, state) => const ViewContentScreen(),
    ),
    GoRoute(
      path: '/triage/intake',
      builder: (context, state) {
        final extra = state.extra;
        if (extra != null && extra is! TriageEntryContext) {
          return const _InvalidRouteScreen();
        }
        return SymptomIntakeScreen(
          entryContext:
              extra as TriageEntryContext? ?? const TriageEntryContext(),
        );
      },
    ),
    GoRoute(
      path: '/triage/result/:sessionId',
      builder: (context, state) {
        final sessionId = state.pathParameters['sessionId'] ?? '';
        return RiskTriageResultScreen(sessionId: sessionId);
      },
    ),
    GoRoute(
      path: '/emergency/map',
      builder: (context, state) => const EmergencyMapScreen(),
    ),
    GoRoute(
      path: '/emergency/alert/:sessionId',
      builder: (context, state) {
        final sessionId = state.pathParameters['sessionId'] ?? '';
        return EmergencyAlertDetailScreen(sessionId: sessionId);
      },
    ),
    GoRoute(
      path: '/family-alert/:sessionId',
      builder: (context, state) {
        final sessionId = state.pathParameters['sessionId'] ?? '';
        return FamilyAlertDetailScreen(sessionId: sessionId);
      },
    ),
    GoRoute(
      path: '/safety',
      builder: (context, state) => const SafetyMonitoringScreen(),
    ),
    GoRoute(
      path: '/safety/fall-detection/enable',
      builder: (context, state) => const EnableFallDetectionScreen(),
    ),
    GoRoute(
      path: '/rag-chat',
      builder: (context, state) => const RagChatScreen(),
    ),
    GoRoute(
      path: '/expert-queue',
      builder: (context, state) => const ExpertQuestionQueueScreen(),
    ),
    // CB-033: Expert Profile Setup (UC-87)
    GoRoute(
      path: '/expert-onboarding',
      builder: (context, state) => const ExpertOnboardingGateScreen(),
    ),
    GoRoute(
      path: '/expert-profile-setup',
      builder: (context, state) => const ExpertProfileSetupScreen(),
    ),
    GoRoute(
      path: '/expert/identity',
      builder: (context, state) => const ExpertIdentityCaptureScreen(),
    ),
    // CB-034: Upload Verification Documents (UC-89)
    GoRoute(
      path: '/expert/credentials',
      builder: (context, state) => const UploadVerificationDocsScreen(),
    ),
    // CB-035: Verification Status (UC-103, UC-173)
    GoRoute(
      path: '/expert-verification-status',
      builder: (context, state) => const VerificationStatusScreen(),
    ),
    // CB-036: Expert Home / Dashboard (UC-60)
    GoRoute(
      path: '/expert-home',
      builder: (context, state) => const ExpertHomeShell(),
    ),
    // CB-053: Expert Calendar (UC-64)
    GoRoute(
      path: '/expert-calendar',
      builder: (context, state) => const ExpertCalendarScreen(),
    ),
    // CB-054: Expert Contributions (UC-69)
    GoRoute(
      path: '/expert/contributions',
      builder: (context, state) => const ExpertContributionListScreen(),
    ),
    GoRoute(
      path: '/expert/contributions/new',
      builder: (context, state) => const ExpertContributionDraftScreen(),
    ),
    GoRoute(
      path: '/expert/contributions/:id/edit',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return ExpertContributionDraftScreen(contributionId: id);
      },
    ),
    GoRoute(
      path: '/expert/contributions/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return ExpertContributionDetailScreen(contributionId: id);
      },
    ),
    // CB-061: Nearby Support Expert (UC-82)
    GoRoute(
      path: '/expert/nearby-support',
      builder: (context, state) => const ExpertNearbySupportScreen(),
    ),
    // TV4 public expert profile -- navigated to from community answers (expertProfileId)
    GoRoute(
      path: '/expert/public/:expertProfileId',
      builder: (context, state) {
        final id = state.pathParameters['expertProfileId'] ?? '';
        return ExpertPublicProfileScreen(expertProfileId: id);
      },
    ),
  ],
);
