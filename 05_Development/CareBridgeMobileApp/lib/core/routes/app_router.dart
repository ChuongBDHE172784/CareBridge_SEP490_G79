import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../auth/auth_state.dart';
import '../../features/auth/screens/welcome_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/blocked_account_screen.dart';
import '../../features/auth/screens/auth_landing_screen.dart';
import '../../features/home/screens/home_shell.dart';
import '../../features/journey/screens/journey_setup_screen.dart';

import '../../features/healthRecords/screens/maternal_health_metric_screen.dart';
import '../../features/healthRecords/screens/health_record_timeline_screen.dart';
import '../../features/healthRecords/screens/add_maternal_health_metric_screen.dart';
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
import '../../features/safety/screens/safety_monitoring_screen.dart';
import '../../features/safety/screens/enable_fall_detection_screen.dart';
import '../../features/aiTriage/screens/rag_chat_screen.dart';
import '../../features/community/screens/expert_question_queue_screen.dart';

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

    // Do not redirect while restoring state
    if (isRestoring) return null;

    final isAuthLanding = state.matchedLocation == '/auth-landing';
    final isAuthRoute = state.matchedLocation.startsWith('/welcome') ||
                        state.matchedLocation.startsWith('/login') ||
                        isAuthLanding;

    if (blockedReason != null && state.matchedLocation != '/blocked') {
      return '/blocked';
    }

    if (!isAuth && !isAuthRoute) {
      return '/welcome';
    }

    if (isAuth && isAuthRoute && !isAuthLanding) {
      return '/auth-landing';
    }

    return null;
  },
  routes: [
    GoRoute(
      path: '/welcome',
      builder: (context, state) => const WelcomeScreen(),
    ),
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/blocked',
      builder: (context, state) => const BlockedAccountScreen(),
    ),
    GoRoute(
      path: '/auth-landing',
      builder: (context, state) => const AuthLandingScreen(),
    ),
    GoRoute(
      path: '/',
      builder: (context, state) {
        // Tab index query param (e.g. /?tab=1 for Journey Dashboard)
        final tabParam = state.uri.queryParameters['tab'];
        final int initialIndex = tabParam != null ? int.tryParse(tabParam) ?? 0 : 0;
        return HomeShell(initialIndex: initialIndex);
      },
    ),
    GoRoute(
      path: '/journey-setup',
      builder: (context, state) => const JourneySetupScreen(),
    ),
    GoRoute(
      path: '/journey-update',
      builder: (context, state) => const Scaffold(
        body: Center(child: Text('Update Mother Journey Screen (3.3.1.2)')),
      ),
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
    // UC-25: Add Maternal Health Metric
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
        body: Center(child: Text('Create Appointment Reminder Screen (3.3.1.22)')),
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
        return CareGroupMembersScreen(groupId: id, groupName: 'Nhóm $id', members: const []);
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
      path: '/babies/add',
      builder: (context, state) => const AddBabyScreen(),
    ),
    GoRoute(
      path: '/babies/detail/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return BabyProfileDetailScreen(babyId: id);
      },
    ),
    // CB-233: Edit Baby Profile (UC-32)
    GoRoute(
      path: '/babies/:id/edit',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return EditBabyProfileScreen(babyId: id);
      },
    ),
    // CB-234: Edit Baby Daily Log (UC-35)
    GoRoute(
      path: '/babies/:babyId/daily-logs/:logId/edit',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        final logId = state.pathParameters['logId'] ?? '';
        final initialLog = state.extra as BabyDailyLog?;
        return EditBabyDailyLogScreen(babyId: babyId, logId: logId, initialLog: initialLog);
      },
    ),
    // CB-235: View Baby Log Summary (UC-36)
    GoRoute(
      path: '/babies/:babyId/log-summary',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        return BabyLogSummaryScreen(babyId: babyId);
      },
    ),
    // CB-236: Record Development Milestone (UC-37)
    GoRoute(
      path: '/babies/:babyId/milestones/add',
      builder: (context, state) {
        final babyId = state.pathParameters['babyId'] ?? '';
        return RecordMilestoneScreen(babyId: babyId);
      },
    ),
    // CB-237: Edit Health Record (UC-40)
    GoRoute(
      path: '/health-records/:id/edit',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return EditHealthRecordScreen(recordId: id);
      },
    ),
    // CB-241: Create Medication Reminder (UC-46)
    GoRoute(
      path: '/reminders/medication/add',
      builder: (context, state) => const CreateMedicationReminderScreen(),
    ),
    // CB-242: Create Vaccination Reminder (UC-47)
    GoRoute(
      path: '/reminders/vaccination/add',
      builder: (context, state) => const CreateVaccinationReminderScreen(),
    ),
    // CB-243: Update or Snooze Reminder (UC-48)
    GoRoute(
      path: '/reminders/:id/manage',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        final initialReminder = state.extra as Reminder?;
        return UpdateSnoozeReminderScreen(reminderId: id, initialReminder: initialReminder);
      },
    ),
    // CB-123: File Viewer (UC-168)
    GoRoute(
      path: '/files/:fileId/view',
      builder: (context, state) {
        final fileId = state.pathParameters['fileId'] ?? '';
        final extra = state.extra as Map<String, dynamic>?;
        return FileViewerScreen(fileId: fileId, fileName: extra?['fileName'] as String?);
      },
    ),
    // CB-146: Shared File Viewer (UC-168)
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
    // CB-230: Edit Maternal Health Metric (UC-26)
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
    // CB-231: View Maternal Health Trend (UC-27)
    GoRoute(
      path: '/journeys/:journeyId/metrics/trend',
      builder: (context, state) {
        final journeyId = state.pathParameters['journeyId'] ?? '';
        final metricType = state.uri.queryParameters['metricType'];
        return HealthMetricTrendScreen(journeyId: journeyId, initialMetricType: metricType);
      },
    ),
    GoRoute(
      path: '/vaccination/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return VaccinationDetailScreen(vaccinationId: id);
      },
    ),
    GoRoute(
      path: '/content',
      builder: (context, state) => const ViewContentScreen(),
    ),
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
  ],
);
