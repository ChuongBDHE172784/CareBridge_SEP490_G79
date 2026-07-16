import 'dart:async';
import 'package:flutter/material.dart';
import 'expert_app_home_screen.dart';
import '../../auth/screens/account_profile_screen.dart';
import '../../community/screens/expert_question_queue_screen.dart';
import '../../consultation/screens/expert_requests_tab_screen.dart';
import '../../consultation/services/consultation_request_refresh_bus.dart';
import '../../consultation/services/consultation_request_service.dart';
import '../../directChat/screens/conversation_list_screen.dart';
import '../../directChat/services/direct_chat_service.dart';
import '../../directChat/services/conversation_refresh_bus.dart';
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

class _ExpertHomeShellState extends State<ExpertHomeShell>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);

  int _index = 0;
  int _unreadConversationCount = 0;
  int _pendingConsultationCount = 0;
  StreamSubscription<void>? _refreshSubscription;
  StreamSubscription<void>? _consultationRefreshSubscription;
  int _unreadLoadGeneration = 0;
  int _consultationLoadGeneration = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refreshUnreadCount();
    _refreshSubscription = ConversationRefreshBus.events.listen(
      (_) => _refreshUnreadCount(),
    );
    _refreshPendingConsultationCount();
    _consultationRefreshSubscription = ConsultationRequestRefreshBus.events
        .listen((_) => _refreshPendingConsultationCount());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _refreshSubscription?.cancel();
    _consultationRefreshSubscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _refreshUnreadCount();
      _refreshPendingConsultationCount();
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

  Future<void> _refreshPendingConsultationCount() async {
    final generation = ++_consultationLoadGeneration;
    try {
      final count = await ConsultationRequestService.instance.pendingCount();
      if (!mounted || generation != _consultationLoadGeneration) return;
      setState(() => _pendingConsultationCount = count);
    } catch (_) {
      // Best effort: retain the latest known pending count.
    }
  }

  void _onDestinationSelected(int i) {
    setState(() => _index = i);
    _refreshUnreadCount();
    _refreshPendingConsultationCount();
  }

  static const _pages = <Widget>[
    ExpertAppHomeScreen(), // 0: Tổng quan (unchanged dashboard content)
    ConversationListScreen(), // 1: Trò chuyện (shared with MOTHER)
    ExpertRequestsTabScreen(), // 2: Tư vấn + Community Q&A
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
          NavigationDestination(
            icon: _pendingConsultationCount > 0
                ? Badge(
                    label: Text('$_pendingConsultationCount'),
                    child: const Icon(Icons.assignment_outlined),
                  )
                : const Icon(Icons.assignment_outlined),
            selectedIcon: _pendingConsultationCount > 0
                ? Badge(
                    label: Text('$_pendingConsultationCount'),
                    child: const Icon(Icons.assignment, color: _primary),
                  )
                : const Icon(Icons.assignment, color: _primary),
            label: 'Yêu cầu tư vấn',
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
