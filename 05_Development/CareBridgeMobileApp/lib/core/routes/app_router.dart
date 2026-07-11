import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../auth/auth_state.dart';
import '../../features/auth/screens/welcome_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/blocked_account_screen.dart';
import '../../features/auth/screens/role_selection_screen.dart';
import '../../features/home/screens/home_shell.dart';
import '../../features/journey/screens/journey_setup_screen.dart';

import '../../features/healthRecords/screens/maternal_health_metric_screen.dart';
import '../../features/healthRecords/screens/health_record_timeline_screen.dart';
import '../../features/healthRecords/screens/edit_health_metric_screen.dart';
import '../../features/healthRecords/screens/health_metric_trend_screen.dart';
import '../../features/healthRecords/models/health_metric_model.dart';
import '../../features/baby/screens/edit_baby_profile_screen.dart';
import '../../features/baby/screens/edit_baby_daily_log_screen.dart';
import '../../features/baby/screens/baby_log_summary_screen.dart';
import '../../features/baby/screens/record_milestone_screen.dart';
import '../../features/baby/models/baby_daily_log_model.dart';

import '../../features/healthRecords/screens/edit_health_record_screen.dart';

import '../../features/reminder/screens/create_medication_reminder_screen.dart';
import '../../features/reminder/screens/create_vaccination_reminder_screen.dart';
import '../../features/reminder/screens/update_snooze_reminder_screen.dart';
import '../../features/reminder/models/reminder_model.dart';

import '../../features/fileManager/screens/file_viewer_screen.dart';
import '../../features/fileManager/screens/shared_file_viewer_screen.dart';

import '../../features/reminder/screens/reminder_detail_screen.dart';
import '../../features/familySync/screens/care_groups_screen.dart';
import '../../features/familySync/screens/care_group_members_screen.dart';
import '../../features/familySync/screens/pending_invitations_screen.dart';
import '../../features/baby/screens/baby_profiles_screen.dart';
import '../../features/baby/screens/baby_profile_detail_screen.dart';
import '../../features/baby/screens/add_baby_screen.dart';
import '../../features/fileManager/screens/upload_file_screen.dart';
import '../../features/healthRecords/screens/vaccination_detail_screen.dart';
import '../../features/community/screens/view_content_screen.dart';
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
import '../../features/expert/screens/expert_answer_composer_screen.dart';
import '../../features/expert/screens/expert_home_screen.dart';
import '../../features/expert/screens/expert_contributions_screen.dart';
import '../../features/expert/screens/expert_calendar_screen.dart';
import '../../features/expert/screens/expert_nearby_support_screen.dart';

/// Global router key for context-less navigation if needed
final GlobalKey<NavigatorState> rootNavigatorKey = GlobalKey<NavigatorState>();

final GoRouter appRouter = GoRouter(
navigatorKey: rootNavigatorKey,
initialLocation: '/',
refreshListenable: AuthState.instance,
redirect: (context, state) {
  final auth = AuthState.instance;
  final isAuth = auth.isAuthenticated;
  final isRestoring = auth.isRestoring;
  final blockedReason = auth.blockedReason;
  final hasAssignedRole = auth.role != null && auth.role!.trim().isNotEmpty;

  // Do not redirect while restoring state
  if (isRestoring) return null;

  final isAuthRoute =
      state.matchedLocation.startsWith('/welcome') ||
      state.matchedLocation.startsWith('/login');

  if (blockedReason != null && state.matchedLocation != '/blocked') {
    return '/blocked';
  }

  if (!isAuth && !isAuthRoute) {
    return '/welcome';
  }

  if (isAuth &&
      !hasAssignedRole &&
      state.matchedLocation != '/role-selection') {
    return '/role-selection';
  }

  if (isAuth &&
      hasAssignedRole &&
      state.matchedLocation == '/role-selection') {
    return auth.role == 'EXPERT' ? '/expert-home' : '/';
  }

  if (isAuth && isAuthRoute) {
    return auth.role == 'EXPERT' ? '/expert-home' : (hasAssignedRole ? '/' : '/role-selection');
  }

  return null;
},
routes: [
GoRoute(path: '/welcome', builder: (context, state) => const WelcomeScreen()),
GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
GoRoute(path: '/blocked', builder: (context, state) => const BlockedAccountScreen()),
GoRoute(
  path: '/role-selection',
  builder: (context, state) => const RoleSelectionScreen(),
),
GoRoute(
  path: '/',
  builder: (context, state) {
    final tabParam = state.uri.queryParameters['tab'];
    final int initialIndex = tabParam != null
        ? int.tryParse(tabParam) ?? 0
        : 0;
    return HomeShell(initialIndex: initialIndex);
  },
),
GoRoute(
  path: '/journey-setup',
  builder: (context, state) => const JourneySetupScreen(),
),
GoRoute(
  path: '/journey-update',
  builder: (context, state) =>
      const Scaffold(body: Center(child: Text('Update Mother Journey Screen (3.3.1.2)'))),
),
GoRoute(
  path: '/health-metrics/:id',
  builder: (context, state) {
    final id = state.pathParameters['id'] ?? '';
    return MaternalHealthMetricScreen(metricId: id);
  },
),
GoRoute(
  path: '/health-records',
  builder: (context, state) => const HealthRecordTimelineScreen(),
),
GoRoute(
  path: '/health-records/add',
  builder: (context, state) =>
      const Scaffold(body: Center(child: Text('Add Health Record Screen (3.3.1.16)'))),
),
GoRoute(
  path: '/health-records/detail/:id',
  builder: (context, state) =>
      const Scaffold(body: Center(child: Text('Health Record Detail Screen (3.3.15.1)'))),
),
GoRoute(path: '/upload-file', builder: (context, state) => const UploadFileScreen()),
GoRoute(
  path: '/reminders/add',
  builder: (context, state) =>
      const Scaffold(body: Center(child: Text('Create Appointment Reminder Screen (3.3.1.22)'))),
),
GoRoute(
  path: '/reminders/detail/:id',
  builder: (context, state) {
    final id = state.pathParameters['id'] ?? '';
    return ReminderDetailScreen(reminderId: id);
  },
),
GoRoute(path: '/care-groups', builder: (context, state) => const CareGroupsScreen()),
GoRoute(
  path: '/care-groups/add',
  builder: (context, state) =>
      const Scaffold(body: Center(child: Text('Create Care Group Screen (3.3.1.47)'))),
),
GoRoute(
  path: '/care-groups/members/:id',
  builder: (context, state) {
    final id = state.pathParameters['id'] ?? '';
    return CareGroupMembersScreen(groupId: id, groupName: 'Nhóm $id', members: const []);
  },
),
GoRoute(
  path: '/care-groups/invitations',
  builder: (context, state) => const PendingInvitationsScreen(),
),
GoRoute(path: '/babies', builder: (context, state) => const BabyProfilesScreen()),
GoRoute(path: '/babies/add', builder: (context, state) => const AddBabyScreen()),
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
  path: '/babies/:babyId/daily-logs/:logId/edit',
  builder: (context, state) {
    final babyId = state.pathParameters['babyId'] ?? '';
    final logId = state.pathParameters['logId'] ?? '';
    final initialLog = state.extra as BabyDailyLog?;
    return EditBabyDailyLogScreen(
        babyId: babyId, logId: logId, initialLog: initialLog);
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
  path: '/babies/:babyId/milestones/add',
  builder: (context, state) {
    final babyId = state.pathParameters['babyId'] ?? '';
    return RecordMilestoneScreen(babyId: babyId);
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
        reminderId: id, initialReminder: initialReminder);
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
  path: '/vaccination/:id',
  builder: (context, state) {
    final id = state.pathParameters['id'] ?? '';
    return VaccinationDetailScreen(vaccinationId: id);
  },
),
GoRoute(path: '/content', builder: (context, state) => const ViewContentScreen()),
GoRoute(
  path: '/triage/intake',
  builder: (context, state) => const SymptomIntakeScreen(),
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
GoRoute(path: '/safety', builder: (context, state) => const SafetyMonitoringScreen()),
GoRoute(
  path: '/safety/fall-detection/enable',
  builder: (context, state) => const EnableFallDetectionScreen(),
),
GoRoute(path: '/rag-chat', builder: (context, state) => const RagChatScreen()),
GoRoute(
  path: '/expert-queue',
  builder: (context, state) => const ExpertQuestionQueueScreen(),
),
// CB-033: Expert Profile Setup (UC-87)
GoRoute(
  path: '/expert-profile-setup',
  builder: (context, state) => const ExpertProfileSetupScreen(),
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
  builder: (context, state) => const ExpertHomeScreen(),
),
// CB-053: Expert Calendar (UC-64)
GoRoute(
  path: '/expert-calendar',
  builder: (context, state) => const ExpertCalendarScreen(),
),
// CB-054: Expert Contributions (UC-69)
GoRoute(
  path: '/expert-contributions',
  builder: (context, state) => const ExpertContributionsScreen(),
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
