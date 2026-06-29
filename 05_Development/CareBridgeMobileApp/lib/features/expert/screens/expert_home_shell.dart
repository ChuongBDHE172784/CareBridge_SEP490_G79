import 'package:flutter/material.dart';
import 'expert_profile_setup_screen.dart';
import '../../../../core/auth/auth_state.dart';

/// Expert home shell — shown after EXPERT login.
/// Provides entry point to UC-87 (Expert Profile Setup) and other expert features.
class ExpertHomeShell extends StatefulWidget {
  const ExpertHomeShell({super.key});

  @override
  State<ExpertHomeShell> createState() => _ExpertHomeShellState();
}

class _ExpertHomeShellState extends State<ExpertHomeShell> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceVariant = Color(0xFFFADCD3);

  int _index = 0;

  static const _pages = <Widget>[
    _ExpertHomeTab(),
    _PlaceholderTab('Cộng đồng'),
    _PlaceholderTab('Lịch tư vấn'),
    _PlaceholderTab('Hồ sơ'),
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
            icon: Icon(Icons.group_outlined),
            selectedIcon: Icon(Icons.group, color: _primary),
            label: 'Cộng đồng',
          ),
          NavigationDestination(
            icon: Icon(Icons.calendar_today_outlined),
            selectedIcon: Icon(Icons.calendar_today, color: _primary),
            label: 'Lịch tư vấn',
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

/// Home tab for Expert — shows profile setup CTA if not yet created.
class _ExpertHomeTab extends StatelessWidget {
  const _ExpertHomeTab();

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          children: [
            const SizedBox(height: 8),
            // Greeting
            Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: _primaryContainer.withAlpha(51),
                    border: Border.all(color: _primaryContainer),
                  ),
                  child: const Icon(Icons.volunteer_activism,
                      color: _primary, size: 28),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Chào chuyên gia!',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                          fontFamily: 'Lexend',
                        ),
                      ),
                      Text(
                        'Hãy hoàn thiện hồ sơ để bắt đầu',
                        style: TextStyle(
                          fontSize: 14,
                          color: _onSurfaceVariant,
                          fontFamily: 'Lexend',
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            // UC-87 CTA Card
            _ExpertCard(
              icon: Icons.verified_user,
              title: 'Thiết lập hồ sơ chuyên gia',
              subtitle: 'Tạo hồ sơ chuyên môn để bắt đầu hỗ trợ cộng đồng',
              buttonText: 'Tạo hồ sơ',
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const ExpertProfileSetupScreen(),
                  ),
                );
              },
            ),
            const SizedBox(height: 16),
            _ExpertCard(
              icon: Icons.upload_file,
              title: 'Tải giấy tờ xác thực',
              subtitle: 'Upload bằng cấp, chứng chỉ để admin xét duyệt',
              buttonText: 'Tải lên',
              onTap: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Tính năng đang phát triển (UC-89)')),
                );
              },
            ),
            const SizedBox(height: 16),
            _ExpertCard(
              icon: Icons.calendar_month,
              title: 'Quản lý lịch tư vấn',
              subtitle: 'Thiết lập khung giờ và hình thức tư vấn',
              buttonText: 'Xem lịch',
              onTap: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Tính năng đang phát triển (UC-90)')),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _ExpertCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final String buttonText;
  final VoidCallback onTap;

  const _ExpertCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.buttonText,
    required this.onTap,
  });

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.06),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          Positioned(
            top: 0,
            left: 0,
            bottom: 0,
            width: 4,
            child: Container(
              decoration: BoxDecoration(
                color: _primary,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: _primaryContainer.withAlpha(51),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(icon, color: _primary, size: 22),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        title,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                          fontFamily: 'Lexend',
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  subtitle,
                  style: TextStyle(
                    fontSize: 14,
                    color: _onSurfaceVariant,
                    fontFamily: 'Lexend',
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  height: 44,
                  child: FilledButton(
                    onPressed: onTap,
                    style: FilledButton.styleFrom(
                      backgroundColor: _primaryContainer,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: Text(
                      buttonText,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        fontFamily: 'Lexend',
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PlaceholderTab extends StatelessWidget {
  final String name;
  const _PlaceholderTab(this.name);

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.construction,
              size: 48, color: const Color(0xFFC98C7B).withAlpha(128)),
          const SizedBox(height: 16),
          Text(
            '$name\n(đang xây dựng)',
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              color: Color(0xFF524440),
            ),
          ),
        ],
      ),
    );
  }
}
