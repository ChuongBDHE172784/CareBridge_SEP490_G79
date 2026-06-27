import 'package:flutter/material.dart';
import 'core/auth/auth_state.dart';
import 'core/routes/app_router.dart';

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