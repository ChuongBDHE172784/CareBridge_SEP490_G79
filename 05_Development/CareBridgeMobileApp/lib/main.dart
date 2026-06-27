import 'features/auth/screens/welcome_screen.dart';
import 'package:flutter/material.dart';
import 'core/auth/auth_state.dart';
import 'features/auth/screens/blocked_account_screen.dart';
import 'features/auth/screens/account_profile_screen.dart';

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

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _currentIndex = 0;

  static const _primaryColor = Color(0xFFC98C7B);
  static const _inactiveColor = Color(0xFF84736F);
  static const _bgColor = Color(0xFFFFF8F6);

  static const _placeholderLabels = [
    'Trang chủ',
    'Hành trình',
    'Cộng đồng',
    'Việc cần làm',
  ];

  Widget _buildPlaceholder(String label) {
    return Center(
      child: Text(
        '$label\nĐang phát triển...',
        textAlign: TextAlign.center,
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 16,
          color: Color(0xFF5A463F),
        ),
      ),
    );
  }

  Widget _buildBody() {
    if (_currentIndex == 4) return const AccountProfileScreen();
    return _buildPlaceholder(_placeholderLabels[_currentIndex]);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(child: _buildBody()),
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          color: _bgColor,
          boxShadow: [
            BoxShadow(
              color: Color.fromRGBO(90, 70, 63, 0.06),
              blurRadius: 20,
              offset: Offset(0, -4),
            ),
          ],
          borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        ),
        child: BottomNavigationBar(
          currentIndex: _currentIndex,
          onTap: (i) => setState(() => _currentIndex = i),
          type: BottomNavigationBarType.fixed,
          backgroundColor: Colors.transparent,
          elevation: 0,
          selectedItemColor: _primaryColor,
          unselectedItemColor: _inactiveColor,
          selectedLabelStyle: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w600,
          ),
          unselectedLabelStyle: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w400,
          ),
          items: const [
            BottomNavigationBarItem(
              icon: Icon(Icons.home_outlined),
              activeIcon: Icon(Icons.home),
              label: 'Trang chủ',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.auto_stories_outlined),
              activeIcon: Icon(Icons.auto_stories),
              label: 'Hành trình',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.groups_outlined),
              activeIcon: Icon(Icons.groups),
              label: 'Cộng đồng',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.checklist_outlined),
              activeIcon: Icon(Icons.checklist),
              label: 'Việc cần làm',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.person_outline),
              activeIcon: Icon(Icons.person),
              label: 'Hồ sơ',
            ),
          ],
        ),
      ),
    );
  }
}
