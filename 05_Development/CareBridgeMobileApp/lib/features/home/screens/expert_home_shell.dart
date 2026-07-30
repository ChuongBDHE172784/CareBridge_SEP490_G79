import 'dart:async';
import 'package:flutter/material.dart';
import 'expert_app_home_screen.dart';
import '../../auth/screens/account_profile_screen.dart';
import '../../directChat/screens/conversation_list_screen.dart';
import '../../directChat/services/direct_chat_service.dart';
import '../../directChat/services/conversation_refresh_bus.dart';
import '../../consultation/screens/expert_requests_tab_screen.dart';
import '../../expert/screens/expert_calendar_screen.dart';

class ExpertHomeShell extends StatefulWidget {
  const ExpertHomeShell({super.key});

  @override
  State<ExpertHomeShell> createState() => _ExpertHomeShellState();
}

class _ExpertHomeShellState extends State<ExpertHomeShell>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);

  int _index = 0;
  int _unreadConversationCount = 0;
  StreamSubscription<void>? _refreshSubscription;
  int _unreadLoadGeneration = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refreshUnreadCount();
    _refreshSubscription = ConversationRefreshBus.events.listen(
      (_) => _refreshUnreadCount(),
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _refreshSubscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _refreshUnreadCount();
    }
  }

  Future<void> _refreshUnreadCount() async {
    final generation = ++_unreadLoadGeneration;
    try {
      final summary = await DirectChatService.instance.getUnreadSummary();
      if (!mounted || generation != _unreadLoadGeneration) return;
      setState(
        () => _unreadConversationCount = summary.unreadConversationCount,
      );
    } catch (_) {
      // best-effort — badge just stays at its last known value
    }
  }

  void _onDestinationSelected(int i) {
    setState(() => _index = i);
    _refreshUnreadCount();
  }

  static const _pages = <Widget>[
    ExpertAppHomeScreen(), // 0: Tổng quan
    ConversationListScreen(), // 1: Trò chuyện
    ExpertRequestsTabScreen(), // 2: Yêu cầu
    ExpertCalendarScreen(), // 3: Lịch rảnh
    AccountProfileScreen(), // 4: Tài khoản
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: IndexedStack(index: _index, children: _pages),
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
            label: 'Tổng quan',
          ),
          NavigationDestination(
            icon: _unreadConversationCount > 0
                ? Badge(
                    label: Text('$_unreadConversationCount'),
                    child: const Icon(Icons.chat_bubble_outline),
                  )
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
            icon: Icon(Icons.assignment_outlined),
            selectedIcon: Icon(Icons.assignment, color: _primary),
            label: 'Yêu cầu',
          ),
          const NavigationDestination(
            icon: Icon(Icons.calendar_month_outlined),
            selectedIcon: Icon(Icons.calendar_month, color: _primary),
            label: 'Lịch rảnh',
          ),
          const NavigationDestination(
            icon: Icon(Icons.person_outlined),
            selectedIcon: Icon(Icons.person, color: _primary),
            label: 'Tài khoản',
          ),
        ],
      ),
    );
  }
}
