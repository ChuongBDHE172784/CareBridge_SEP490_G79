import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../auth/auth_state.dart';
import '../../features/auth/screens/welcome_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/blocked_account_screen.dart';
import '../../features/auth/screens/auth_landing_screen.dart';
import '../../features/auth/screens/role_selection_screen.dart';
import '../../features/auth/screens/account_profile_screen.dart';
import '../../features/auth/screens/edit_profile_screen.dart';
import '../../features/checklist/screens/checklist_roadmap_screen.dart';

import '../../features/home/screens/home_shell.dart';
import '../../features/home/screens/expert_home_shell.dart';
import '../../features/home/screens/family_home_shell.dart';
import '../../features/notification/screens/notification_center_screen.dart';
import '../../features/journey/screens/mother_stage_selection_screen.dart';
import '../../features/journey/screens/journey_setup_screen.dart';
import '../../features/journey/screens/postpartum_recovery_setup_screen.dart';
import '../../features/journey/services/journey_onboarding_service.dart';
import '../../features/recommendation/screens/recommendation_profile_screen.dart';
import '../../features/exercise/screens/mother_exercise_screen.dart';

import '../../features/healthRecords/screens/maternal_health_metric_screen.dart';
import '../../features/healthRecords/screens/health_record_timeline_screen.dart';
import '../../features/healthRecords/screens/add_health_record_screen.dart';
import '../../features/healthRecords/screens/add_maternal_health_metric_screen.dart';
import '../../features/healthRecords/screens/hydration_tracker_screen.dart';
import '../../features/healthRecords/screens/fetal_movement_tracker_screen.dart';
import '../../features/healthRecords/screens/edit_health_metric_screen.dart';
import '../../features/healthRecords/screens/health_metric_trend_screen.dart';
import '../../features/emergency/models/emergency_session_model.dart';
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

import '../../features/reminder/screens/create_appointment_reminder_screen.dart';
import '../../features/reminder/screens/create_medication_reminder_screen.dart';
import '../../features/reminder/screens/create_vaccination_reminder_screen.dart';
import '../../features/reminder/screens/update_snooze_reminder_screen.dart';
import '../../features/reminder/screens/appointment_calendar_screen.dart';
import '../../features/reminder/screens/reminder_schedules_screen.dart';
import '../../features/reminder/models/reminder_model.dart';
import '../../features/reminder/models/today_task_model.dart';
import '../../features/reminder/screens/checklist_task_detail_screen.dart';

import '../../features/fileManager/screens/file_viewer_screen.dart';
import '../../features/fileManager/screens/shared_file_viewer_screen.dart';

import '../../features/reminder/screens/reminder_detail_screen.dart';
import '../../features/reminder/screens/shared_appointment_detail_screen.dart';
import '../../features/reminder/screens/all_reminders_screen.dart';
import '../../features/familySync/screens/care_groups_screen.dart';
import '../../features/familySync/screens/care_group_members_screen.dart';
import '../../features/baby/screens/baby_profiles_screen.dart';
import '../../features/baby/screens/baby_profile_detail_screen.dart';
import '../../features/baby/screens/add_baby_screen.dart';
import '../../features/fileManager/screens/upload_file_screen.dart';
import '../../features/healthRecords/screens/vaccination_detail_screen.dart';
import '../../features/healthRecords/screens/edit_vaccination_record_screen.dart';
import '../../features/healthRecords/models/vaccination_model.dart';
import '../../features/healthRecords/screens/growth_measurement_history_screen.dart';
import '../../features/healthRecords/screens/add_vaccination_record_screen.dart';
import '../../features/community/screens/view_content_screen.dart';
import '../../features/community/models/content_model.dart';
import '../../features/aiTriage/models/triage_continuation.dart';
import '../../features/aiTriage/services/triage_continuation_restore_coordinator.dart';
import '../../features/aiTriage/screens/rag_chat_screen.dart';
import '../../features/aiTriage/widgets/floating_ai_triage_host.dart';
import '../../features/emergency/screens/emergency_map_screen.dart';
import '../../features/emergency/screens/emergency_alert_detail_screen.dart';
import '../../features/emergency/screens/family_alert_detail_screen.dart';
import '../../features/safety/screens/safety_monitoring_screen.dart';
import '../../features/safety/screens/enable_fall_detection_screen.dart';
import '../../features/expert/screens/expert_profile_setup_screen.dart';
import '../../features/expert/screens/expert_profile_page_screen.dart';
import '../../features/expert/screens/verification_documents_page_screen.dart';
import '../../features/expert/screens/verification_status_screen.dart';
import '../../features/expert/screens/expert_public_profile_screen.dart';
import '../../features/expert/screens/expert_calendar_screen.dart';
import '../../features/expert/screens/expert_onboarding_gate_screen.dart';
import '../../features/expert/screens/expert_contract_screen.dart';
import '../../features/expert/screens/expert_identity_capture_screen.dart';
import '../../features/expert/screens/expert_type_choice_screen.dart';
import '../../features/expert/services/expert_onboarding_store.dart';
import '../../features/directChat/screens/expert_directory_screen.dart';
import '../../features/directChat/screens/conversation_list_screen.dart';
import '../../features/directChat/screens/direct_chat_screen.dart';
import '../../features/consultation/screens/my_consultation_requests_screen.dart';
import '../../features/consultation/screens/consultation_request_detail_screen.dart';
import '../../features/consultation/screens/triage_expert_handoff_screen.dart';

Widget _buildHomeForRole(String? role, {required int initialIndex}) {
  switch ((role ?? '').trim().toUpperCase()) {
    case 'FAMILY':
      return FamilyHomeShell(initialIndex: initialIndex);
    case 'EXPERT':
      return const ExpertHomeShell();
    case 'MOTHER':
      return HomeShell(initialIndex: initialIndex);
    default:
      return const _UnsupportedRoleHome();
  }
}

const _supportedEmergencyStages = {
  'PRECONCEPTION',
  'PREGNANCY',
  'POSTPARTUM',
  'INFANT',
};

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
        child: Text('Liên kết không hợp lệ.', textAlign: TextAlign.center),
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
  bool familyLandingComplete = false,
}) {
  final normalizedRole = role?.trim().toUpperCase();
  final hasAssignedRole = normalizedRole != null && normalizedRole.isNotEmpty;
  const motherOnlySetupRoutes = {
    '/journey-onboarding',
    '/mother-stage-selection',
    '/journey-setup',
    '/postpartum-recovery-setup',
    '/recommendation-profile',
    '/mother-exercise',
  };
  const motherOrFamilyChecklistRoutes = {
    '/checklists/roadmap',
    '/checklists/history',
    '/checklists/task-detail',
  };

  if (isRestoring) return null;

  final isAuthRoute =
      location.startsWith('/welcome') ||
      location.startsWith('/login') ||
      location == '/auth-landing';

  if (blockedReason != null) return location == '/blocked' ? null : '/blocked';
  if (!isAuthenticated && location == '/blocked') return '/welcome';
  if (!isAuthenticated && !isAuthRoute) return '/welcome';
  if (isAuthenticated && !hasAssignedRole && location != '/role-selection') {
    return '/role-selection';
  }
  if (isAuthenticated &&
      hasAssignedRole &&
      normalizedRole != 'MOTHER' &&
      motherOnlySetupRoutes.contains(location)) {
    return '/';
  }
  if (isAuthenticated &&
      hasAssignedRole &&
      normalizedRole != 'MOTHER' &&
      normalizedRole != 'FAMILY' &&
      motherOrFamilyChecklistRoutes.contains(location)) {
    return '/';
  }
  if (isAuthenticated && hasAssignedRole && location == '/role-selection') {
    return normalizedRole == 'EXPERT'
        ? '/expert-onboarding'
        : (normalizedRole == 'MOTHER' ? '/mother-stage-selection' : '/');
  }
  if (isAuthenticated && isAuthRoute && location != '/auth-landing') {
    return normalizedRole == 'EXPERT'
        ? '/expert-onboarding'
        : (normalizedRole == 'MOTHER' || normalizedRole == 'FAMILY'
              ? '/auth-landing'
              : (hasAssignedRole ? '/' : '/role-selection'));
  }
  if (isAuthenticated && location == '/auth-landing') {
    return normalizedRole == 'MOTHER' || normalizedRole == 'FAMILY'
        ? null
        : (normalizedRole == 'EXPERT'
              ? '/expert-onboarding'
              : (hasAssignedRole ? '/' : '/role-selection'));
  }

  // `/` is the app-start dispatcher for mothers. The landing screen verifies
  // the journey before opening the real home or consolidated setup screen.
  if (isAuthenticated &&
      (normalizedRole == 'MOTHER' ||
          (normalizedRole == 'FAMILY' && !familyLandingComplete)) &&
      location == '/') {
    return '/auth-landing';
  }
  return null;
}

@visibleForTesting
Future<String?> resolveMotherOnboardingRedirect({
  required String? role,
  required String location,
  required Future<bool> Function() canStartJourney,
}) async {
  const guardedDestinations = {'/journey-setup', '/postpartum-recovery-setup'};
  if (role?.trim().toUpperCase() != 'MOTHER' ||
      !guardedDestinations.contains(location)) {
    return null;
  }
  try {
    return await canStartJourney() ? null : '/mother-stage-selection';
  } catch (_) {
    // Deep links fail closed when consent status cannot be verified.
    return '/mother-stage-selection';
  }
}

@visibleForTesting
AddBabyEntryPoint resolveAddBabyEntryPoint({
  required Object? extra,
  required String? legacyEntry,
}) {
  if (extra is AddBabyRouteArgs) return extra.entryPoint;
  return legacyEntry == 'onboarding'
      ? AddBabyEntryPoint.onboarding
      : AddBabyEntryPoint.profileList;
}

final GoRouter appRouter = GoRouter(
  navigatorKey: rootNavigatorKey,
  observers: [floatingAiTriageRouteObserver],
  initialLocation: '/',
  refreshListenable: AuthState.instance,
  redirect: (context, state) async {
    final auth = AuthState.instance;
    final isExpert = auth.role?.trim().toUpperCase() == 'EXPERT';
    final isExpertOnboardingRoute =
        state.matchedLocation == '/expert-onboarding' ||
        state.matchedLocation == '/expert-profile-setup' ||
        state.matchedLocation == '/expert/type' ||
        state.matchedLocation == '/expert/contract' ||
        state.matchedLocation == '/expert-calendar' ||
        state.matchedLocation == '/expert/identity' ||
        state.matchedLocation == '/expert/credentials' ||
        state.matchedLocation == '/expert-verification-status';

    final baseRedirect = resolveAppRedirect(
      isAuthenticated: auth.isAuthenticated,
      isRestoring: auth.isRestoring,
      blockedReason: auth.blockedReason,
      role: auth.role,
      location: state.matchedLocation,
      familyLandingComplete:
          state.uri.queryParameters['triageChecked'] == 'true',
    );
    if (baseRedirect != null) return baseRedirect;

    final onboardingRedirect = await resolveMotherOnboardingRedirect(
      role: auth.role,
      location: state.matchedLocation,
      canStartJourney: () async =>
          (await JourneyOnboardingService().getStatus()).canStartJourney,
    );
    if (onboardingRedirect != null) return onboardingRedirect;

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
      path: '/profile',
      builder: (context, state) => const AccountProfileScreen(),
    ),
    GoRoute(
      path: '/profile/edit',
      builder: (context, state) => const EditProfileScreen(),
    ),
    GoRoute(
      path: '/checklists/roadmap',
      builder: (context, state) => const ChecklistRoadmapScreen(),
    ),
    GoRoute(
      path: '/checklists/history',
      redirect: (context, state) => '/checklists/roadmap',
    ),
    GoRoute(
      path: '/checklists/task-detail',
      builder: (context, state) {
        final task = state.extra;
        if (task is! TodayTask) return const _InvalidRouteScreen();
        final audience = state.uri.queryParameters['audience'];
        return ChecklistTaskDetailScreen(
          task: task,
          showSupportFunction: audience != 'family',
        );
      },
    ),
    GoRoute(
      path: '/mother-exercise',
      builder: (context, state) => const MotherExerciseScreen(),
    ),
    GoRoute(
      path: '/journey-onboarding',
      redirect: (context, state) => '/mother-stage-selection',
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
      path: '/recommendation-profile',
      builder: (context, state) => RecommendationProfileScreen(
        journeyStage: state.extra is String ? state.extra as String : null,
      ),
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
      path: '/notifications',
      builder: (context, state) => const NotificationCenterScreen(),
    ),
    GoRoute(
      path: '/mother-home',
      builder: (context, state) {
        final tabParam = state.uri.queryParameters['tab'];
        final triageReturn = state.uri.queryParameters['triageReturn'];
        final arrival = state.extra is TriageContinuationArrival
            ? state.extra as TriageContinuationArrival
            : null;
        final recoveryNotice = state.extra is TriageContinuationRecoveryNotice
            ? state.extra as TriageContinuationRecoveryNotice
            : null;
        return HomeShell(
          key: triageReturn == null
              ? null
              : ValueKey('mother-triage-return-$triageReturn'),
          initialIndex: int.tryParse(tabParam ?? '') ?? 0,
          continuationArrival: arrival,
          continuationRecoveryNotice: recoveryNotice,
        );
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
        final metricType = state.uri.queryParameters['metricType'] ?? 'BMI';
        if (metricType == 'FETAL_MOVEMENT_SESSION' ||
            metricType == 'FETAL_MOVEMENT_COUNT' ||
            metricType == 'FETAL_MOVEMENT') {
          return FetalMovementTrackerScreen(journeyId: journeyId);
        }
        if (metricType == 'HYDRATION') {
          return HydrationTrackerScreen(journeyId: journeyId);
        }
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
      builder: (context, state) {
        final requestId = state.pathParameters['requestId'];
        if (!_isUuid(requestId)) return const _InvalidRouteScreen();
        return ConsultationRequestDetailScreen(requestId: requestId!);
      },
    ),
    GoRoute(
      path: '/reminders/all',
      // Compatibility read for older deep links; canonical new entry is
      // /reminder-schedules and appointments have their own calendar.
      builder: (context, state) => const AllRemindersScreen(),
    ),
    GoRoute(
      path: '/reminders/calendar',
      builder: (context, state) {
        final careGroupId = state.uri.queryParameters['careGroupId'];
        return AppointmentCalendarScreen(
          careGroupId: _isUuid(careGroupId) ? careGroupId : null,
        );
      },
    ),
    GoRoute(
      path: '/appointments/calendar',
      builder: (context, state) {
        final careGroupId = state.uri.queryParameters['careGroupId'];
        return AppointmentCalendarScreen(
          careGroupId: _isUuid(careGroupId) ? careGroupId : null,
        );
      },
    ),
    GoRoute(
      path: '/reminder-schedules',
      builder: (context, state) => const ReminderSchedulesScreen(),
    ),
    GoRoute(
      path: '/reminder-schedules/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'];
        return _isUuid(id)
            ? ReminderSchedulesScreen(scheduleId: id)
            : const _InvalidRouteScreen();
      },
    ),
    GoRoute(
      path: '/health-records/add',
      builder: (context, state) => const AddHealthRecordScreen(),
    ),
    GoRoute(
      path: '/health-records/detail/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return EditHealthRecordScreen(recordId: id);
      },
    ),
    GoRoute(
      path: '/upload-file',
      builder: (context, state) => const UploadFileScreen(),
    ),
    GoRoute(
      path: '/reminders/add',
      builder: (context, state) {
        final rawDate = state.uri.queryParameters['date'];
        final parsedDate = rawDate == null ? null : DateTime.tryParse(rawDate);
        return CreateAppointmentReminderScreen(initialDate: parsedDate);
      },
    ),
    GoRoute(
      path: '/appointments/add',
      builder: (context, state) {
        final rawDate = state.uri.queryParameters['date'];
        final parsedDate = rawDate == null ? null : DateTime.tryParse(rawDate);
        return CreateAppointmentReminderScreen(initialDate: parsedDate);
      },
    ),
    GoRoute(
      path: '/reminders/detail/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return ReminderDetailScreen(reminderId: id);
      },
    ),
    GoRoute(
      path: '/appointments/detail/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'];
        return _isUuid(id)
            ? ReminderDetailScreen(reminderId: id!, appointmentResource: true)
            : const _InvalidRouteScreen();
      },
    ),
    GoRoute(
      path: '/care-groups/:careGroupId/appointments/:id',
      builder: (context, state) {
        final careGroupId = state.pathParameters['careGroupId'];
        final appointmentId = state.pathParameters['id'];
        return _isUuid(careGroupId) && _isUuid(appointmentId)
            ? SharedAppointmentDetailScreen(
                careGroupId: careGroupId!,
                appointmentId: appointmentId!,
              )
            : const _InvalidRouteScreen();
      },
    ),
    GoRoute(
      path: '/appointments/:id/edit',
      builder: (context, state) {
        final id = state.pathParameters['id'];
        final initialReminder = state.extra is Reminder
            ? state.extra as Reminder
            : null;
        return _isUuid(id)
            ? UpdateSnoozeReminderScreen(
                reminderId: id!,
                initialReminder: initialReminder,
                appointmentResource: true,
              )
            : const _InvalidRouteScreen();
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
      path: '/babies',
      builder: (context, state) => const BabyProfilesScreen(),
    ),
    GoRoute(
      path: '/babies/add',
      builder: (context, state) {
        return AddBabyScreen(
          entryPoint: resolveAddBabyEntryPoint(
            extra: state.extra,
            legacyEntry: state.uri.queryParameters['entry'],
          ),
        );
      },
    ),
    GoRoute(
      path: '/babies/detail/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        final arrival = state.extra is TriageContinuationArrival
            ? state.extra as TriageContinuationArrival
            : null;
        return BabyProfileDetailScreen(
          babyId: id,
          continuationArrival: arrival,
        );
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
        final initialLog = state.extra is BabyDailyLog
            ? state.extra as BabyDailyLog
            : null;
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
      builder: (context, state) {
        final extra = state.extra;
        return CreateVaccinationReminderScreen(
          initialBabyId: state.uri.queryParameters['babyId'],
          initialSuggestion: extra is Map
              ? Map<String, dynamic>.from(extra)
              : null,
        );
      },
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
          initialRecord: state.extra is VaccinationRecord
              ? state.extra as VaccinationRecord
              : null,
        );
      },
    ),
    GoRoute(
      path: '/babies/:babyId/vaccinations/:recordId/edit',
      builder: (context, state) => EditVaccinationRecordScreen(
        babyId: state.pathParameters['babyId'] ?? '',
        recordId: state.pathParameters['recordId'] ?? '',
        initialRecord: state.extra is VaccinationRecord
            ? state.extra as VaccinationRecord
            : null,
      ),
    ),
    GoRoute(
      path: '/vaccination/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return VaccinationDetailScreen(
          babyId: state.uri.queryParameters['babyId'] ?? '',
          vaccinationId: id,
          initialRecord: state.extra is VaccinationRecord
              ? state.extra as VaccinationRecord
              : null,
        );
      },
    ),
    GoRoute(
      path: '/content',
      builder: (context, state) =>
          const ViewContentScreen(mode: ContentBrowseMode.lifecycle),
    ),
    GoRoute(
      path: '/rag/chat',
      builder: (context, state) {
        final extra = state.extra;
        String? initialPrompt =
            state.uri.queryParameters['prompt'] ??
            state.uri.queryParameters['initialMessage'];
        Map<String, dynamic>? attachedContext;
        bool autoSend = state.uri.queryParameters['autoSend'] == 'true';
        if (extra is Map<String, dynamic>) {
          initialPrompt ??=
              (extra['prompt'] ?? extra['initialMessage']) as String?;
          attachedContext =
              (extra['attachedContext'] ?? extra['attachedHealthContext'])
                  as Map<String, dynamic>?;
          if (extra.containsKey('autoSend')) {
            autoSend = extra['autoSend'] == true;
          }
        }
        return RagChatScreen(
          initialPrompt: initialPrompt,
          attachedHealthContext: attachedContext,
          autoSendInitialPrompt: autoSend,
        );
      },
    ),
    GoRoute(
      path: '/triage/expert-handoff',
      builder: (context, state) {
        final intakeSessionId = state.extra;
        if (intakeSessionId is! String || !_isUuid(intakeSessionId)) {
          return const _InvalidRouteScreen();
        }
        return TriageExpertHandoffScreen(intakeSessionId: intakeSessionId);
      },
    ),
    GoRoute(
      path: '/emergency/map',
      builder: (context, state) {
        final extra = state.extra;
        if (extra != null && extra is! EmergencySession) {
          return const _InvalidRouteScreen();
        }
        final mode = state.uri.queryParameters['mode'];
        final stage = state.uri.queryParameters['stage'] ?? 'INFANT';
        if ((mode != null && mode != 'manual' && mode != 'triage') ||
            !_supportedEmergencyStages.contains(stage)) {
          return const _InvalidRouteScreen();
        }
        return EmergencyMapScreen(
          existingSession: extra as EmergencySession?,
          triageHandoff: mode == 'triage',
          stage: stage,
        );
      },
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
    // Hai nhóm chuyên gia: bước chọn hình thức + trang ký Thoả thuận hợp tác.
    // Cả hai path phải nằm trong whitelist isExpertOnboardingRoute ở trên.
    GoRoute(
      path: '/expert/type',
      builder: (context, state) => const ExpertTypeChoiceScreen(),
    ),
    GoRoute(
      path: '/expert/contract',
      builder: (context, state) => const ExpertContractScreen(),
    ),
    GoRoute(
      path: '/expert/profile',
      builder: (context, state) => const ExpertProfilePageScreen(),
    ),
    // CB-034: Upload Verification Documents (UC-89)
    GoRoute(
      path: '/expert/credentials',
      builder: (context, state) => const VerificationDocumentsPageScreen(),
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
