import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:go_router/go_router.dart';
import 'core/auth/auth_state.dart';
import 'core/firebase/firebase_bootstrap.dart';
import 'core/notifications/fcm_service.dart';
import 'core/routes/app_router.dart';
import 'features/auth/services/auth_service.dart';
import 'features/aiTriage/widgets/floating_ai_triage_host.dart';
import 'features/aiTriage/models/triage_entry_context.dart';
import 'features/directChat/calls/direct_call_host.dart';
import 'features/reminder/services/reminder_service.dart';
import 'features/safety/models/safety_config_model.dart';
import 'features/safety/services/safety_foreground_service.dart';
import 'features/safety/widgets/safety_countdown_sheet.dart';
import 'features/safety/screens/safety_monitoring_screen.dart';

@visibleForTesting
bool isSafetyMonitoringScreenActive(GoRouter router) {
  final currentUri = router.routerDelegate.currentConfiguration.uri;
  final providerUri = router.routeInformationProvider.value.uri;
  final path1 = currentUri.path;
  final path2 = providerUri.path;
  return path1 == '/safety' ||
      path1.startsWith('/safety/') ||
      path2 == '/safety' ||
      path2.startsWith('/safety/');
}

@visibleForTesting
bool shouldOpenSafetyMonitoringForDetectedEvent({
  required String eventStatus,
  required String currentPath,
  bool navigationInFlight = false,
  bool countdownShowing = false,
  bool screenMounted = false,
}) =>
    eventStatus == 'OPEN' &&
    currentPath != '/safety' &&
    !currentPath.startsWith('/safety/') &&
    !navigationInFlight &&
    !countdownShowing &&
    !screenMounted;

@visibleForTesting
Future<T?> pushSafetyMonitoringRoute<T extends Object?>(GoRouter router) =>
    router.push<T>('/safety');

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

class CareBridgeApp extends StatefulWidget {
  const CareBridgeApp({super.key, this.firebaseEnabled = true});

  final bool firebaseEnabled;

  @override
  State<CareBridgeApp> createState() => _CareBridgeAppState();
}

class _CareBridgeAppState extends State<CareBridgeApp> {
  StreamSubscription<SafetyEvent>? _detectedSafetyEventSubscription;
  bool _safetyNavigationInFlight = false;
  DateTime? _lastSafetyRoutePoppedAt;
  String? _lastHandledEventId;

  @override
  void initState() {
    super.initState();
    // This listener owns route-independent safety UX. The foreground task
    // keeps collecting IMU samples while the app is open; a detected event
    // must still bring the user to its countdown even outside `/safety`.
    _detectedSafetyEventSubscription = SafetyForegroundServiceCoordinator
        .instance
        .detectedEvents
        .listen(_openSafetyMonitoringForDetectedEvent);
  }

  Future<void> _openSafetyMonitoringForDetectedEvent(SafetyEvent event) async {
    if (!mounted) return;
    if (SafetyCountdownGuard.isShowing || SafetyMonitoringScreen.isMounted) {
      return;
    }
    if (_safetyNavigationInFlight || isSafetyMonitoringScreenActive(appRouter)) {
      return;
    }
    final currentPath = appRouter.routerDelegate.currentConfiguration.uri.path;
    final now = DateTime.now();
    if (!shouldOpenSafetyMonitoringForDetectedEvent(
      eventStatus: event.status,
      currentPath: currentPath,
      navigationInFlight: _safetyNavigationInFlight,
      countdownShowing: SafetyCountdownGuard.isShowing,
      screenMounted: SafetyMonitoringScreen.isMounted,
    )) {
      return;
    }
    if (_lastHandledEventId == event.id) {
      return;
    }
    if (_lastSafetyRoutePoppedAt != null &&
        now.difference(_lastSafetyRoutePoppedAt!) < const Duration(seconds: 5)) {
      return;
    }
    _lastHandledEventId = event.id;
    // Preserve the screen that was active before the fall alert. Using go()
    // replaces the entire stack and leaves a black screen when Safety pops.
    _safetyNavigationInFlight = true;
    try {
      await pushSafetyMonitoringRoute(appRouter);
    } finally {
      _safetyNavigationInFlight = false;
      _lastSafetyRoutePoppedAt = DateTime.now();
    }
  }

  @override
  void dispose() {
    _detectedSafetyEventSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'CareBridge',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        fontFamily: 'Lexend',
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
          manageAuthenticatedSession: widget.firebaseEnabled,
          child: FloatingAiTriageHost(
            authListenable: AuthState.instance,
            navigationListenable: Listenable.merge([
              appRouter.routeInformationProvider,
              appRouter.routerDelegate,
            ]),
            modalListenable: floatingAiTriageRouteObserver,
            isAuthenticated: () => AuthState.instance.isAuthenticated,
            currentRole: () => AuthState.instance.role,
            currentPath: () =>
                appRouter.routeInformationProvider.value.uri.path,
            hasModal: () => floatingAiTriageRouteObserver.hasPopupRoute,
            onOpen: () async {
              await appRouter.push(
                '/triage/intake',
                extra: const TriageEntryContext(requiresStageSelection: true),
              );
            },
            child: app,
          ),
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
