import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'core/auth/auth_state.dart';
import 'core/firebase/firebase_bootstrap.dart';
import 'core/notifications/fcm_service.dart';
import 'core/routes/app_router.dart';
import 'features/auth/services/auth_service.dart';
import 'features/directChat/calls/direct_call_host.dart';
import 'features/reminder/services/reminder_service.dart';
import 'features/safety/services/safety_foreground_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  if (!kIsWeb) {
    FlutterForegroundTask.initCommunicationPort();
  }
  late final bool firebaseReady;
  try {
    firebaseReady = await FirebaseBootstrap.initialize();
  } catch (error, stackTrace) {
    FlutterError.reportError(
      FlutterErrorDetails(
        exception: error,
        stack: stackTrace,
        library: 'CareBridge startup',
        context: ErrorDescription('while initializing Firebase'),
      ),
    );
    runApp(const FirebaseStartupErrorApp());
    return;
  }
  // Auth and reminder state must be ready before the router renders.
  // Running them after runApp() causes a race: HomeShell fires API calls
  // before init() completes, which clears valid tokens from storage.
  await AuthState.instance.init();
  if (AuthState.instance.isAuthenticated) {
    try {
      await AuthService.instance.getProfile().timeout(
        const Duration(seconds: 5),
      );
    } catch (_) {
      // The API client clears revoked sessions. Keep local state for
      // transient connectivity failures so offline users can retry.
    }
  }
  await ReminderService.instance.loadState();
  SafetyForegroundServiceCoordinator.instance.initialize();
  // Re-register the FCM token on relaunch for users with an existing
  // session (fresh logins register via AuthService instead).
  if (firebaseReady && AuthState.instance.isAuthenticated) {
    unawaited(FcmService.instance.registerToken());
  }
  runApp(CareBridgeApp(firebaseEnabled: firebaseReady));
  if (!kIsWeb) {
    unawaited(SafetyForegroundServiceCoordinator.instance.reconcile());
  }
  if (firebaseReady) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      FcmService.instance.markNavigationReady();
      unawaited(FcmService.instance.initTapHandling());
    });
  }
}

class CareBridgeApp extends StatelessWidget {
  const CareBridgeApp({super.key, this.firebaseEnabled = true});

  final bool firebaseEnabled;

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
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
      routerConfig: appRouter,
      builder: (context, child) {
        final app = child ?? const SizedBox.shrink();
        return DirectCallHost(
          manageAuthenticatedSession: firebaseEnabled,
          child: app,
        );
      },
    );
  }
}

class FirebaseStartupErrorApp extends StatelessWidget {
  const FirebaseStartupErrorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        body: SafeArea(
          child: Center(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.cloud_off_outlined, size: 48),
                  SizedBox(height: 16),
                  Text(
                    'Không thể khởi tạo CareBridge',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'Firebase chưa được cấu hình đúng cho thiết bị này. '
                    'Vui lòng kiểm tra cấu hình ứng dụng và thử lại.',
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
