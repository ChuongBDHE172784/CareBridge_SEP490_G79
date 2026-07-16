import 'package:flutter/material.dart';
import 'expert_app_home_screen.dart';
import '../../auth/screens/account_profile_screen.dart';
import '../../community/screens/expert_question_queue_screen.dart';
import '../../directChat/screens/conversation_list_screen.dart';
import '../../directChat/services/direct_chat_service.dart';
import '../../expert/screens/expert_calendar_screen.dart';

/// EXPERT app shell — ADR-MEDI-005: converts the EXPERT role from a single Scaffold with a
/// custom-drawn bottom row + Navigator.push per tab (no state retention, navigator stack could
/// grow unbounded) to the same IndexedStack + NavigationBar pattern already used by MOTHER's
/// [HomeShell]. [ExpertAppHomeScreen] (unchanged dashboard content) becomes tab 0 "Tổng quan".
class ExpertHomeShell extends StatefulWidget {
  const ExpertHomeShell({super.key});

  @override
  State<ExpertHomeShell> createState() => _ExpertHomeShellState();
}

class _ExpertHomeShellState extends State<ExpertHomeShell> with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);

  int _index = 0;
  int _unreadConversationCount = 0;

  @override
  void initState() {
    super.initState();
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

  static const _pages = <Widget>[
    ExpertAppHomeScreen(), // 0: Tổng quan (unchanged dashboard content)
    ConversationListScreen(), // 1: Trò chuyện (shared with MOTHER)
    ExpertQuestionQueueScreen(), // 2: Yêu cầu
    ExpertCalendarScreen(), // 3: Lịch
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
            icon: Icon(Icons.assignment_outlined),
            selectedIcon: Icon(Icons.assignment, color: _primary),
            label: 'Yêu cầu',
          ),
          const NavigationDestination(
            icon: Icon(Icons.calendar_month_outlined),
            selectedIcon: Icon(Icons.calendar_month, color: _primary),
            label: 'Lịch',
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
