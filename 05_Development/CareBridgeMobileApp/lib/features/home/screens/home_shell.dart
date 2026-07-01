import 'package:flutter/material.dart';
import 'mother_home_screen.dart';
import '../../journey/screens/mother_journey_screen.dart';
import '../../community/screens/community_feed_screen.dart';
import '../../auth/screens/account_profile_screen.dart';
import 'screens_explorer_screen.dart';

/// Main app shell housing the BottomNavigationBar (6 tabs).
/// Tabs: Home (CB-008) | Journey (CB-009) | Community | Tasks | Screens | Profile
class HomeShell extends StatefulWidget {
  /// optionally jump to a specific tab on launch (e.g. from a notification)
  final int initialIndex;

  const HomeShell({super.key, this.initialIndex = 0});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _onSurfaceVariant = Color(0xFF524440);

  late int _index;

  @override
  void initState() {
    super.initState();
    _index = widget.initialIndex;
  }

  // Tab page widgets — constant instances so IndexedStack preserves scroll position
  static const _pages = <Widget>[
    MotherHomeScreen(),       // 0: Trang chủ  (CB-008)
    MotherJourneyScreen(),    // 1: Hành trình (CB-009)
    CommunityFeedScreen(),    // 2: Cộng đồng  (CB-014)
    _PlaceholderTab('Việc cần làm'), // 3: TODO: tasks tab (CB-013 is modal-style)
    ScreensExplorerScreen(),        // 4: Screen directory/explorer
    AccountProfileScreen(),         // 5: Hồ sơ tài khoản
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
        onDestinationSelected: (i) => setState(() => _index = i),
        backgroundColor: _canvas,
        indicatorColor: _primaryContainer.withAlpha(51),
        shadowColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home, color: _primary),
            label: 'Trang chủ',
          ),
          NavigationDestination(
            icon: Icon(Icons.auto_stories_outlined),
            selectedIcon: Icon(Icons.auto_stories, color: _primary),
            label: 'Hành trình',
          ),
          NavigationDestination(
            icon: Icon(Icons.group_outlined),
            selectedIcon: Icon(Icons.group, color: _primary),
            label: 'Cộng đồng',
          ),
          NavigationDestination(
            icon: Icon(Icons.fact_check_outlined),
            selectedIcon: Icon(Icons.fact_check, color: _primary),
            label: 'Việc cần làm',
          ),
          NavigationDestination(
            icon: Icon(Icons.explore_outlined),
            selectedIcon: Icon(Icons.explore, color: _primary),
            label: 'Screen',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outlined),
            selectedIcon: Icon(Icons.person, color: _primary),
            label: 'Hồ sơ',
          ),
        ],
      ),
    );
  }
}

// Placeholder for tabs not yet implemented
class _PlaceholderTab extends StatelessWidget {
  final String name;
  const _PlaceholderTab(this.name);

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.construction, size: 48, color: const Color(0xFFC98C7B).withAlpha(128)),
          const SizedBox(height: 16),
          Text('$name\n(đang xây dựng)',
              textAlign: TextAlign.center,
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: Color(0xFF524440))),
        ],
      ),
    );
  }
}
