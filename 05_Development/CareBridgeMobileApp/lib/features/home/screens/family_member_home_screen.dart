import 'package:flutter/material.dart';

import '../../auth/screens/account_profile_screen.dart';
import '../../familySync/screens/my_care_groups_screen.dart';
import '../../familySync/services/family_home_service.dart';
import '../../notification/screens/notification_center_screen.dart';

class FamilyMemberHomeScreen extends StatefulWidget {
  const FamilyMemberHomeScreen({super.key});

  @override
  State<FamilyMemberHomeScreen> createState() => _FamilyMemberHomeScreenState();
}

class _FamilyMemberHomeScreenState extends State<FamilyMemberHomeScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _surfaceLow = Color(0xFFFFF1EC);
  static const _surfaceHigh = Color(0xFFFFE2D9);
  static const _surfaceHighest = Color(0xFFFADCD3);
  static const _secondary = Color(0xFF6E5A52);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _tertiary = Color(0xFF625D59);
  static const _tertiaryContainer = Color(0xFFA09A95);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);
  static List<BoxShadow> get _softShadow => [
    BoxShadow(
      color: const Color(0xFF5A463F).withValues(alpha: 0.06),
      blurRadius: 22,
      offset: const Offset(0, 4),
    ),
  ];

  FamilyHomeSnapshot? _snapshot;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final snapshot = await FamilyHomeService.instance.loadSnapshot();
    if (!mounted) return;
    setState(() {
      _snapshot = snapshot;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          color: _primaryContainer,
          onRefresh: _load,
          child: Stack(
            children: [
              ListView(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 132),
                children: [
                  _buildHeader(),
                  const SizedBox(height: 28),
                  _buildShortcuts(),
                  const SizedBox(height: 30),
                  if (_loading)
                    const Padding(
                      padding: EdgeInsets.only(top: 80),
                      child: Center(
                        child: CircularProgressIndicator(color: _primary),
                      ),
                    )
                  else ...[
                    _SectionTitle(
                      icon: Icons.assignment_outlined,
                      title: 'Việc cần làm sắp tới',
                      color: _primary,
                    ),
                    const SizedBox(height: 12),
                    _buildTaskCard(),
                    const SizedBox(height: 30),
                    _SectionTitle(
                      icon: Icons.calendar_today_outlined,
                      title: 'Lịch chăm sóc',
                      color: _secondary,
                    ),
                    const SizedBox(height: 12),
                    _buildScheduleCard(),
                    const SizedBox(height: 30),
                    _SectionTitle(
                      icon: Icons.notifications_active_outlined,
                      title: 'Thông báo mới nhất',
                      color: _primary,
                    ),
                    const SizedBox(height: 12),
                    _buildNotificationCard(),
                  ],
                ],
              ),
              Align(
                alignment: Alignment.bottomCenter,
                child: _buildBottomNav(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: _surfaceHigh,
            shape: BoxShape.circle,
            boxShadow: _softShadow,
          ),
          child: const Icon(Icons.elderly_woman, color: _primary, size: 30),
        ),
        const SizedBox(width: 16),
        const Expanded(
          child: Text(
            'Chào bà ngoại,',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 32,
              fontWeight: FontWeight.w700,
              color: _primary,
            ),
          ),
        ),
        _CircleButton(
          icon: Icons.notifications_none_rounded,
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => const NotificationCenterScreen()),
          ),
        ),
      ],
    );
  }

  Widget _buildShortcuts() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        _ShortcutButton(
          icon: Icons.check_circle_outline,
          label: 'Nhiệm vụ',
          color: _primary,
          background: _primaryContainer.withValues(alpha: 0.18),
          onTap: _openGroups,
        ),
        _ShortcutButton(
          icon: Icons.calendar_today_outlined,
          label: 'Lịch',
          color: _secondary,
          background: _secondaryContainer.withValues(alpha: 0.32),
          onTap: _openGroups,
        ),
        _ShortcutButton(
          icon: Icons.warning_amber_rounded,
          label: 'Cảnh báo',
          color: _error,
          background: _errorContainer.withValues(alpha: 0.42),
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => const NotificationCenterScreen()),
          ),
        ),
        _ShortcutButton(
          icon: Icons.share_outlined,
          label: 'Dữ liệu\nchia sẻ',
          color: _tertiary,
          background: _tertiaryContainer.withValues(alpha: 0.24),
          onTap: _openGroups,
        ),
      ],
    );
  }

  Widget _buildTaskCard() {
    final task = _snapshot?.nextTask;
    return _ClayCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 28,
            height: 28,
            margin: const EdgeInsets.only(top: 2),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: _outline, width: 3),
            ),
          ),
          const SizedBox(width: 18),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  task?.title ?? 'Chưa có nhiệm vụ mới',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 20,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                    height: 1.25,
                  ),
                ),
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 7,
                  ),
                  decoration: BoxDecoration(
                    color: _surfaceHighest,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.shopping_cart_outlined,
                        color: _onSurfaceVariant,
                        size: 18,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        task?.category ?? 'Chăm sóc',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildScheduleCard() {
    final schedule = _snapshot?.nextSchedule;
    return _ClayCard(
      padding: EdgeInsets.zero,
      child: IntrinsicHeight(
        child: Row(
          children: [
            Container(
              width: 10,
              decoration: const BoxDecoration(
                color: _secondaryContainer,
                borderRadius: BorderRadius.horizontal(
                  left: Radius.circular(28),
                ),
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.schedule, size: 18, color: _secondary),
                        const SizedBox(width: 8),
                        Text(
                          _timeLabel(schedule?.dueAt),
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: _secondary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Text(
                      schedule?.title ?? 'Chưa có lịch chăm sóc',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 21,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNotificationCard() {
    final alert = _snapshot?.latestAlert;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _primaryContainer.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: _primaryContainer.withValues(alpha: 0.25)),
      ),
      child: Row(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: const BoxDecoration(
              color: _primaryContainer,
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.bedtime_outlined, color: Colors.white),
          ),
          const SizedBox(width: 18),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  alert == null ? 'Mới đây' : _timeAgo(alert.createdAt),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _primary,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  alert?.body.isNotEmpty == true
                      ? alert!.body
                      : alert?.title ?? 'Chưa có thông báo mới',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 19,
                    fontWeight: FontWeight.w500,
                    color: _onSurface,
                    height: 1.25,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomNav() {
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 10, 20, 22),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(32)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withValues(alpha: 0.08),
            blurRadius: 24,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          const _BottomNavItem(
            icon: Icons.home_outlined,
            activeIcon: Icons.home,
            label: 'Tổng quan',
            active: true,
          ),
          _BottomNavItem(
            icon: Icons.group_outlined,
            label: 'Nhóm',
            onTap: _openGroups,
          ),
          _BottomNavItem(
            icon: Icons.calendar_month_outlined,
            label: 'Lịch',
            onTap: _openGroups,
          ),
          _BottomNavItem(
            icon: Icons.notifications_none,
            label: 'Thông báo',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => const NotificationCenterScreen(),
              ),
            ),
          ),
          _BottomNavItem(
            icon: Icons.person_outline,
            label: 'Hồ sơ',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const AccountProfileScreen()),
            ),
          ),
        ],
      ),
    );
  }

  void _openGroups() {
    Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (_) => const MyCareGroupsScreen()));
  }

  String _timeLabel(DateTime? dueAt) {
    if (dueAt == null) return '10:00 sáng mai';
    final local = dueAt.toLocal();
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  String _timeAgo(DateTime? dateTime) {
    if (dateTime == null) return 'Mới đây';
    final diff = DateTime.now().difference(dateTime);
    if (diff.inMinutes < 60) return 'Mới đây';
    if (diff.inHours < 24) return '${diff.inHours} giờ trước';
    return '${diff.inDays} ngày trước';
  }
}

class _SectionTitle extends StatelessWidget {
  final IconData icon;
  final String title;
  final Color color;

  const _SectionTitle({
    required this.icon,
    required this.title,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, color: color, size: 28),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            title,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 25,
              fontWeight: FontWeight.w700,
              color: _FamilyMemberHomeScreenState._onSurface,
            ),
          ),
        ),
      ],
    );
  }
}

class _ShortcutButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final Color background;
  final VoidCallback onTap;

  const _ShortcutButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.background,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(22),
      child: SizedBox(
        width: 78,
        child: Column(
          children: [
            Container(
              width: 68,
              height: 68,
              decoration: BoxDecoration(
                color: background,
                borderRadius: BorderRadius.circular(22),
              ),
              child: Icon(icon, color: color, size: 30),
            ),
            const SizedBox(height: 12),
            Text(
              label,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: _FamilyMemberHomeScreenState._onSurfaceVariant,
                height: 1.15,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ClayCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;

  const _ClayCard({
    required this.child,
    this.padding = const EdgeInsets.all(24),
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: padding,
      decoration: BoxDecoration(
        color: _FamilyMemberHomeScreenState._surface,
        borderRadius: BorderRadius.circular(28),
        boxShadow: _FamilyMemberHomeScreenState._softShadow,
      ),
      child: child,
    );
  }
}

class _CircleButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;

  const _CircleButton({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      customBorder: const CircleBorder(),
      child: Container(
        width: 58,
        height: 58,
        decoration: BoxDecoration(
          color: _FamilyMemberHomeScreenState._surfaceLow,
          shape: BoxShape.circle,
        ),
        child: Icon(icon, color: _FamilyMemberHomeScreenState._primary),
      ),
    );
  }
}

class _BottomNavItem extends StatelessWidget {
  final IconData icon;
  final IconData? activeIcon;
  final String label;
  final bool active;
  final VoidCallback? onTap;

  const _BottomNavItem({
    required this.icon,
    required this.label,
    this.activeIcon,
    this.active = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final color = active
        ? _FamilyMemberHomeScreenState._primary
        : _FamilyMemberHomeScreenState._outline;
    final child = Container(
      width: active ? 104 : 58,
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: BoxDecoration(
        color: active
            ? _FamilyMemberHomeScreenState._primaryContainer.withValues(
                alpha: 0.18,
              )
            : Colors.transparent,
        borderRadius: BorderRadius.circular(32),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(active ? activeIcon ?? icon : icon, color: color, size: 26),
          const SizedBox(height: 5),
          Text(
            label,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: color,
            ),
          ),
        ],
      ),
    );
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(32),
      child: child,
    );
  }
}
