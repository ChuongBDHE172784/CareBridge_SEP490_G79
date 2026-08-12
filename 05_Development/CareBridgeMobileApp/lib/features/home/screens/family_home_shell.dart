import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'family_member_home_screen.dart';
import '../../familySync/screens/my_care_groups_screen.dart';
import '../../directChat/screens/expert_directory_screen.dart';
import '../../directChat/screens/conversation_list_screen.dart';
import '../../auth/screens/account_profile_screen.dart';
import '../../directChat/services/direct_chat_service.dart';
import '../../directChat/services/conversation_refresh_bus.dart';
import '../../checklist/services/checklist_assignment_refresh_bus.dart';

/// Main app shell housing the BottomNavigationBar (5 tabs) for Family role.
/// Tabs: Trang chủ | Nhóm | Chuyên gia | Trò chuyện | Hồ sơ
class FamilyHomeShell extends StatefulWidget {
  final int initialIndex;

  const FamilyHomeShell({
    super.key,
    this.initialIndex = 0,
  });

  @override
  State<FamilyHomeShell> createState() => _FamilyHomeShellState();
}

class _FamilyHomeShellState extends State<FamilyHomeShell>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);

  late int _index;
  int _unreadConversationCount = 0;
  StreamSubscription<void>? _refreshSubscription;
  int _unreadLoadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _index = widget.initialIndex;
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
      // best-effort
    }
  }

  void _onDestinationSelected(int i) {
    final previousIndex = _index;
    setState(() => _index = i);
    if (i == 0 && previousIndex != 0) {
      ChecklistAssignmentRefreshBus.notify();
    }
    _refreshUnreadCount();
  }

  static const _pages = <Widget>[
    FamilyMemberHomeScreen(hideBottomNav: true), // 0: Trang chủ
    MyCareGroupsScreen(), // 1: Nhóm
    ExpertDirectoryScreen(), // 2: Chuyên gia
    ConversationListScreen(), // 3: Trò chuyện
    AccountProfileScreen(), // 4: Hồ sơ
  ];

  @override
  Widget build(BuildContext context) {
    final useSelectedOnlyNavigationLabels =
        MediaQuery.textScalerOf(context).scale(1) >= 1.3;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
        statusBarBrightness: Brightness.light,
      ),
      child: Scaffold(
        backgroundColor: _canvas,
        body: IndexedStack(index: _index, children: _pages),
        bottomNavigationBar: NavigationBar(
          selectedIndex: _index,
          onDestinationSelected: _onDestinationSelected,
          backgroundColor: _canvas,
          indicatorColor: _primaryContainer.withAlpha(51),
          shadowColor: Colors.transparent,
          surfaceTintColor: Colors.transparent,
          labelBehavior: useSelectedOnlyNavigationLabels
              ? NavigationDestinationLabelBehavior.onlyShowSelected
              : NavigationDestinationLabelBehavior.alwaysShow,
          destinations: [
            const NavigationDestination(
              icon: Icon(Icons.home_outlined),
              selectedIcon: Icon(Icons.home, color: _primary),
              label: 'Trang chủ',
            ),
            const NavigationDestination(
              icon: Icon(Icons.group_outlined),
              selectedIcon: Icon(Icons.group, color: _primary),
              label: 'Nhóm',
            ),
            const NavigationDestination(
              icon: Icon(Icons.medical_services_outlined),
              selectedIcon: Icon(Icons.medical_services, color: _primary),
              label: 'Chuyên gia',
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
              icon: Icon(Icons.person_outlined),
              selectedIcon: Icon(Icons.person, color: _primary),
              label: 'Hồ sơ',
            ),
          ],
        ),
      ),
    );
  }
}
