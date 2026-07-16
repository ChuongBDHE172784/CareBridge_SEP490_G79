import 'dart:async';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'core/auth/auth_state.dart';
import 'core/notifications/fcm_service.dart';
import 'core/routes/app_router.dart';
import 'features/auth/services/auth_service.dart';
import 'features/directChat/calls/direct_call_host.dart';
import 'features/reminder/services/reminder_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // FCM/Firebase is only configured for Android (google-services.json).
  // There is no web Firebase config yet, so Firebase.initializeApp()
  // throws on web and blocks runApp(), producing a blank white page.
  if (!kIsWeb) {
    await Firebase.initializeApp();
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
  // Re-register the FCM token on relaunch for users with an existing
  // session (fresh logins register via AuthService instead).
  if (!kIsWeb && AuthState.instance.isAuthenticated) {
    unawaited(FcmService.instance.registerToken());
  }
  runApp(const CareBridgeApp());
  if (!kIsWeb) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(FcmService.instance.initTapHandling());
    });
  }
}

class CareBridgeApp extends StatelessWidget {
  const CareBridgeApp({super.key});

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
      builder: (context, child) =>
          DirectCallHost(child: child ?? const SizedBox.shrink()),
    );
  }
}
