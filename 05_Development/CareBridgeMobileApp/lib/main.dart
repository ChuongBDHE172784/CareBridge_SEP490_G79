import 'package:flutter/material.dart';
import 'features/community/screens/community_feed_screen.dart';
import 'features/community/screens/search_questions_screen.dart';
import 'features/healthRecords/screens/view_content_screen.dart';
import 'features/healthRecords/screens/search_content_screen.dart';
import 'features/aiTriage/screens/rag_chat_screen.dart';

void main() {
  runApp(const CareBridgeApp());
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
      home: const MainShell(),
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

  static const _screens = <Widget>[
    CommunityFeedScreen(),
    SearchQuestionsScreen(),
    ViewContentScreen(),
    SearchContentScreen(),
    RagChatScreen(),
  ];

  static const _navItems = <BottomNavigationBarItem>[
    BottomNavigationBarItem(
      icon: Icon(Icons.people_outline),
      activeIcon: Icon(Icons.people),
      label: 'Bảng tin',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.search_outlined),
      activeIcon: Icon(Icons.search),
      label: 'Hỏi đáp',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.article_outlined),
      activeIcon: Icon(Icons.article),
      label: 'Kiến thức',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.find_in_page_outlined),
      activeIcon: Icon(Icons.find_in_page),
      label: 'Tìm kiếm',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.smart_toy_outlined),
      activeIcon: Icon(Icons.smart_toy),
      label: 'AI Chat',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (i) => setState(() => _currentIndex = i),
        type: BottomNavigationBarType.fixed,
        selectedItemColor: const Color(0xFFC98C7B),
        unselectedItemColor: Colors.grey,
        selectedFontSize: 11,
        unselectedFontSize: 11,
        items: _navItems,
      ),
    );
  }
}
