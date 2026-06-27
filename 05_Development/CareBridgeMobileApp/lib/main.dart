import 'features/auth/screens/welcome_screen.dart';
import 'package:flutter/material.dart';
import 'core/auth/auth_state.dart';
import 'features/auth/screens/blocked_account_screen.dart';
import 'features/journey/screens/journey_setup_screen.dart';
import 'features/baby/screens/baby_profiles_screen.dart';
import 'features/baby/screens/baby_profile_detail_screen.dart';
import 'features/healthRecords/screens/maternal_health_metric_screen.dart';
import 'features/healthRecords/screens/health_record_timeline_screen.dart';
import 'features/reminder/screens/today_tasks_screen.dart';
import 'features/reminder/screens/reminder_detail_screen.dart';
import 'features/familySync/screens/care_groups_screen.dart';
import 'features/familySync/screens/my_care_groups_screen.dart';
import 'features/fileManager/screens/file_manager_screen.dart';


void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const CareBridgeApp());
  // init() completes async; notifyListeners() inside triggers rebuild
  AuthState.instance.init();
}

class CareBridgeApp extends StatelessWidget {
  const CareBridgeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'CareBridge',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFC98C7B),
          primary: const Color(0xFFC98C7B),
          secondary: const Color(0xFFD4A895),
        ),
        primaryColor: const Color(0xFFC98C7B),
        scaffoldBackgroundColor: const Color(0xFFF6F1EC),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFFC98C7B),
          foregroundColor: Colors.white,
          elevation: 0,
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFFC98C7B),
            foregroundColor: Colors.white,
            shape: const StadiumBorder(),
          ),
        ),
        useMaterial3: true,
      ),
      home: ListenableBuilder(
        listenable: AuthState.instance,
        builder: (context, _) {
          if (AuthState.instance.isRestoring) {
            return const _SplashScreen();
          }
          if (AuthState.instance.blockedReason != null) {
            return const BlockedAccountScreen();
          }
          if (!AuthState.instance.isAuthenticated) {
            return const WelcomeScreen();
          }
          return const MainShell();
        },
      ),
    );
  }
}

class _SplashScreen extends StatelessWidget {
  const _SplashScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      backgroundColor: Color(0xFFF6F1EC),
      body: Center(
        child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
      ),
    );
  }
}

class _TestFab extends StatefulWidget {
  @override
  State<_TestFab> createState() => _TestFabState();
}

class _TestFabState extends State<_TestFab> {
  bool _open = false;

  void _navigate(Widget screen) {
    setState(() => _open = false);
    Navigator.push(context, MaterialPageRoute(builder: (_) => screen));
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        if (_open) ...[
          _miniButton('CB-007', Icons.pregnant_woman, () => _navigate(const JourneySetupScreen())),
          const SizedBox(height: 8),
          _miniButton('CB-010', Icons.child_care, () => _navigate(const BabyProfilesScreen())),
          const SizedBox(height: 8),
          _miniButton('CB-011', Icons.face, () => _navigate(const BabyProfileDetailScreen(babyId: 'mock-1'))),
          const SizedBox(height: 8),
          _miniButton('CB-157', Icons.monitor_weight, () => _navigate(const MaternalHealthMetricScreen(metricId: 'mock-metric-1'))),
          const SizedBox(height: 8),
          _miniButton('CB-012', Icons.history, () => _navigate(const HealthRecordTimelineScreen())),
          const SizedBox(height: 8),
          _miniButton('CB-013', Icons.task_alt, () => _navigate(const TodayTasksScreen())),
          const SizedBox(height: 8),
          _miniButton('CB-167', Icons.notifications_active, () => _navigate(const ReminderDetailScreen(reminderId: 'r-1'))),
          const SizedBox(height: 8),
          _miniButton('CB-021', Icons.group, () => _navigate(const CareGroupsScreen())),
          const SizedBox(height: 8),
          _miniButton('CB-026', Icons.groups, () => _navigate(const MyCareGroupsScreen())),
          const SizedBox(height: 8),
          _miniButton('CB-122', Icons.folder, () => _navigate(const FileManagerScreen())),
          const SizedBox(height: 8),
        ],
        FloatingActionButton.extended(
          backgroundColor: const Color(0xFFC98C7B),
          foregroundColor: Colors.white,
          icon: Icon(_open ? Icons.close : Icons.grid_view),
          label: Text(_open ? 'Đóng' : 'Test Screen'),
          onPressed: () => setState(() => _open = !_open),
        ),
      ],
    );
  }

  Widget _miniButton(String label, IconData icon, VoidCallback onTap) {
    return FloatingActionButton.small(
      heroTag: label,
      backgroundColor: const Color(0xFF845143),
      foregroundColor: Colors.white,
      tooltip: label,
      onPressed: onTap,
      child: Icon(icon, size: 18),
    );
  }
}

class MainShell extends StatelessWidget {
  const MainShell({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('CareBridge'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => AuthState.instance.clear(),
          ),
        ],
      ),
      body: const Center(
        child: Text(
          'Chào mừng bạn đến với CareBridge!\nCác chức năng đang được phát triển.',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            color: Color(0xFF5A463F),
          ),
        ),
      ),
      floatingActionButton: _TestFab(),
    );
  }

}