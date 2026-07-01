 import 'dart:async';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'core/auth/auth_state.dart';
import 'core/notifications/fcm_service.dart';
import 'core/routes/app_router.dart';
import 'features/reminder/services/reminder_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // FCM/Firebase is only configured for Android (google-services.json).
  // There is no web Firebase config yet, so Firebase.initializeApp()
  // throws on web and blocks runApp(), producing a blank white page.
  if (!kIsWeb) {
    await Firebase.initializeApp();
    unawaited(FcmService.instance.initTapHandling());
  }
  // Auth and reminder state must be ready before the router renders.
  // Running them after runApp() causes a race: HomeShell fires API calls
  // before init() completes, which clears valid tokens from storage.
  await AuthState.instance.init();
  await ReminderService.instance.loadState();
  // Re-register the FCM token on relaunch for users with an existing
  // session (fresh logins register via AuthService instead).
  if (!kIsWeb && AuthState.instance.isAuthenticated) {
    unawaited(FcmService.instance.registerToken());
  }
  runApp(const CareBridgeApp());
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
    );
  }
}
