import 'package:flutter/material.dart';
import 'mother_home_screen.dart';
import '../../journey/screens/mother_journey_screen.dart';
import '../../auth/screens/account_profile_screen.dart';
import '../../directChat/screens/expert_directory_screen.dart';
import '../../directChat/screens/conversation_list_screen.dart';
import '../../directChat/services/direct_chat_service.dart';

/// Main app shell housing the BottomNavigationBar (5 tabs).
/// Tabs: Home (CB-008) | Journey (CB-009) | Expert directory | Conversations | Profile
/// (TDS MotherExpertDiscoveryInbox §13.1 — Cộng đồng/Bài tập moved into MotherHomeScreen's
/// "Khám phá" quick-action section; those 2 slots now host the new Chuyên gia/Trò chuyện tabs.)
class HomeShell extends StatefulWidget {
  /// optionally jump to a specific tab on launch (e.g. from a notification)
  final int initialIndex;

  const HomeShell({super.key, this.initialIndex = 0});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);

  late int _index;
  int _unreadConversationCount = 0;

  @override
  void initState() {
    super.initState();
    _index = widget.initialIndex;
    WidgetsBinding.instance.addObserver(this);
    _refreshUnreadCount();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refreshUnreadCount();
  }

  Future<void> _refreshUnreadCount() async {
    try {
      final summary = await DirectChatService.instance.getUnreadSummary();
      if (!mounted) return;
      setState(() => _unreadConversationCount = summary.unreadConversationCount);
    } catch (_) {
      // best-effort — badge just stays at its last known value
    }
  }

  void _onDestinationSelected(int i) {
    setState(() => _index = i);
    _refreshUnreadCount();
  }

  // Tab page widgets — constant instances so IndexedStack preserves scroll position
  static const _pages = <Widget>[
    MotherHomeScreen(), // 0: Trang chủ  (CB-008)
    MotherJourneyScreen(), // 1: Hành trình (CB-009)
    ExpertDirectoryScreen(), // 2: Chuyên gia
    ConversationListScreen(), // 3: Trò chuyện
    AccountProfileScreen(), // 4: Hồ sơ tài khoản
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        bottom: false,
        child: IndexedStack(index: _index, children: _pages),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: _onDestinationSelected,
        backgroundColor: _canvas,
        indicatorColor: _primaryContainer.withAlpha(51),
        shadowColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        destinations: [
          const NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home, color: _primary),
            label: 'Trang chủ',
          ),
          const NavigationDestination(
            icon: Icon(Icons.auto_stories_outlined),
            selectedIcon: Icon(Icons.auto_stories, color: _primary),
            label: 'Hành trình',
          ),
          const NavigationDestination(
            icon: Icon(Icons.medical_services_outlined),
            selectedIcon: Icon(Icons.medical_services, color: _primary),
            label: 'Chuyên gia',
          ),
          NavigationDestination(
            icon: _unreadConversationCount > 0
                ? Badge(label: Text('$_unreadConversationCount'), child: const Icon(Icons.chat_bubble_outline))
                : const Icon(Icons.chat_bubble_outline),
            selectedIcon: _unreadConversationCount > 0
                ? Badge(
                    label: Text('$_unreadConversationCount'),
                    child: const Icon(Icons.chat_bubble, color: _primary),
                  )
                : const Icon(Icons.chat_bubble, color: _primary),
            label: 'Trò chuyện',
          ),
          const NavigationDestination(
            icon: Icon(Icons.person_outlined),
            selectedIcon: Icon(Icons.person, color: _primary),
            label: 'Hồ sơ',
          ),
        ],
      ),
    );
  }
}
