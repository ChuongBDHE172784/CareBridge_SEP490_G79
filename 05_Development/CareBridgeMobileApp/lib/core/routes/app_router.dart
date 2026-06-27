import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../auth/auth_state.dart';
import '../../features/auth/screens/welcome_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/blocked_account_screen.dart';
import '../../features/home/screens/home_shell.dart';
import '../../features/journey/screens/journey_setup_screen.dart';
import '../../features/journey/screens/mother_journey_screen.dart';
import '../../features/healthRecords/screens/maternal_health_metric_screen.dart';
import '../../features/healthRecords/screens/health_record_timeline_screen.dart';
import '../../features/reminder/screens/today_tasks_screen.dart';
import '../../features/reminder/screens/reminder_detail_screen.dart';
import '../../features/familySync/screens/care_groups_screen.dart';
import '../../features/familySync/screens/care_group_members_screen.dart';
import '../../features/baby/screens/baby_profiles_screen.dart';
import '../../features/baby/screens/baby_profile_detail_screen.dart';
import '../../features/baby/screens/add_baby_screen.dart';
import '../../features/fileManager/screens/upload_file_screen.dart';
import '../../features/healthRecords/screens/vaccination_detail_screen.dart';

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

    final isAuthRoute = state.matchedLocation.startsWith('/welcome') ||
                        state.matchedLocation.startsWith('/login');

    if (blockedReason != null && state.matchedLocation != '/blocked') {
      return '/blocked';
    }

    if (!isAuth && !isAuthRoute) {
      return '/welcome';
    }

    if (isAuth && isAuthRoute) {
      // Logic kiểm tra xem có Mother Journey chưa (mock).
      // Thực tế bạn có thể fetch JourneyService() trong một Provider / Bloc,
      // hoặc AuthState để biết có journey chưa.
      // Tạm thời redirect thẳng về trang Home / Dashboard.
      return '/'; 
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
        return MaternalHealthMetricScreen(metricId: id);
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
    GoRoute(
      path: '/vaccination/:id',
      builder: (context, state) {
        final id = state.pathParameters['id'] ?? '';
        return VaccinationDetailScreen(vaccinationId: id);
      },
    ),
  ],
);
