import 'features/auth/screens/welcome_screen.dart';
import 'package:flutter/material.dart';
import 'core/auth/auth_state.dart';
import 'features/auth/screens/blocked_account_screen.dart';

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
      body: Center(child: CircularProgressIndicator(color: Color(0xFFC98C7B))),
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
    );
  }
}
