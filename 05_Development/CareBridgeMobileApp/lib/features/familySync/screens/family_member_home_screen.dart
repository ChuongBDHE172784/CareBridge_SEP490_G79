import 'package:flutter/material.dart';
import '../../../../core/network/api_client.dart';
import '../services/care_group_service.dart';
import '../services/family_task_service.dart';
import '../models/care_group_model.dart';
import '../models/family_task_model.dart';
import 'assigned_tasks_screen.dart';
import 'shared_care_calendar_screen.dart';
import 'shared_data_screen.dart';
import 'family_alerts_screen.dart';
import 'my_care_groups_screen.dart';

class FamilyMemberHomeScreen extends StatefulWidget {
  const FamilyMemberHomeScreen({super.key});

  @override
  State<FamilyMemberHomeScreen> createState() => _FamilyMemberHomeScreenState();
}

class _FamilyMemberHomeScreenState extends State<FamilyMemberHomeScreen> {
  final _groupService = CareGroupService();
  final _taskService = FamilyTaskService();

  bool _isLoading = true;
  CareGroup? _defaultGroup;
  FamilyTask? _nextTask;
  Map<String, dynamic>? _nextSchedule;
  Map<String, dynamic>? _latestAlert;

  String _userName = 'bạn';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      // Get user profile for name
      final profile = await apiGet('/api/v1/auth/profile');
      if (mounted) {
        setState(() {
          _userName = profile['data']['fullName'] ?? 'bạn';
        });
      }

      // Get first care group
      final groups = await _groupService.listMyGroups();
      if (groups.isNotEmpty) {
        _defaultGroup = groups.first;
        final groupId = _defaultGroup!.id;

        // Fetch task, schedule, alert in parallel
        final results = await Future.wait([
          _taskService.listTasks(groupId),
          _groupService.getSharedCalendar(
            groupId,
            DateTime.now(),
            DateTime.now().add(const Duration(days: 7)),
          ),
          _groupService.getFamilyAlerts(groupId),
        ]);

        final tasks = results[0] as List<FamilyTask>;
        final calendar = results[1] as List<Map<String, dynamic>>;
        final alerts = results[2] as List<Map<String, dynamic>>;

        if (mounted) {
          setState(() {
            final pendingTasks = tasks
                .where(
                  (t) => t.status != 'COMPLETED' && t.status != 'CANCELLED',
                )
                .toList();
            if (pendingTasks.isNotEmpty) {
              pendingTasks.sort((a, b) => a.dueAt.compareTo(b.dueAt));
              _nextTask = pendingTasks.first;
            }
            if (calendar.isNotEmpty) {
              _nextSchedule = calendar.first;
            }
            if (alerts.isNotEmpty) {
              _latestAlert = alerts.first;
            }
          });
        }
      }

      if (mounted) {
        setState(() => _isLoading = false);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi tải dữ liệu: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
            )
          : SafeArea(
              child: RefreshIndicator(
                color: const Color(0xFFC98C7B),
                onRefresh: _loadData,
                child: ListView(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 24.0,
                    vertical: 16.0,
                  ),
                  children: [
                    // App Bar
                    Row(
                      children: [
                        Container(
                          width: 48,
                          height: 48,
                          decoration: const BoxDecoration(
                            color: Color(0xFFFFE2D9),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.person,
                            color: Color(0xFF845143),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            'Chào $_userName,',
                            style: const TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF845143),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ),
                        IconButton(
                          icon: const Icon(
                            Icons.notifications,
                            color: Color(0xFF845143),
                          ),
                          style: IconButton.styleFrom(
                            backgroundColor: const Color(0xFFFFF1EC),
                          ),
                          onPressed: () {},
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),

                    if (_defaultGroup == null) ...[
                      const Center(
                        child: Padding(
                          padding: EdgeInsets.all(32.0),
                          child: Text(
                            'Bạn chưa tham gia nhóm gia đình nào.',
                            style: TextStyle(
                              color: Color(0xFF524440),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ),
                      ),
                    ] else ...[
                      // Quick Shortcuts Bento
                      Row(
                        children: [
                          Expanded(
                            child: _ShortcutButton(
                              icon: Icons.check_circle,
                              label: 'Nhiệm vụ',
                              color: const Color(0xFFC98C7B),
                              bgColor: const Color(0x33C98C7B),
                              onTap: () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => AssignedTasksScreen(
                                      groupId: _defaultGroup!.id,
                                      groupName: _defaultGroup!.groupName,
                                    ),
                                  ),
                                );
                              },
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _ShortcutButton(
                              icon: Icons.calendar_today,
                              label: 'Lịch',
                              color: const Color(0xFF6E5A52),
                              bgColor: const Color(0x4DF6DACF),
                              onTap: () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => SharedCareCalendarScreen(
                                      groupId: _defaultGroup!.id,
                                    ),
                                  ),
                                );
                              },
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _ShortcutButton(
                              icon: Icons.warning,
                              label: 'Cảnh báo',
                              color: const Color(0xFFBA1A1A),
                              bgColor: const Color(0x66FFDAD6),
                              onTap: () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => FamilyAlertsScreen(
                                      groupId: _defaultGroup!.id,
                                    ),
                                  ),
                                );
                              },
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _ShortcutButton(
                              icon: Icons.share,
                              label: 'Dữ liệu',
                              color: const Color(0xFF625D59),
                              bgColor: const Color(0x4DA09A95),
                              onTap: () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => SharedDataScreen(
                                      groupId: _defaultGroup!.id,
                                    ),
                                  ),
                                );
                              },
                            ),
                          ),
                        ],
                      ),

                      const SizedBox(height: 24),

                      // Tasks Card
                      Row(
                        children: [
                          const Icon(
                            Icons.assignment,
                            color: Color(0xFF845143),
                            size: 24,
                          ),
                          const SizedBox(width: 8),
                          const Text(
                            'Việc cần làm sắp tới',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF271812),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(24),
                          border: Border.all(color: const Color(0xFFFFF1EC)),
                          boxShadow: const [
                            BoxShadow(
                              color: Color(0x0F5A463F),
                              blurRadius: 20,
                              offset: Offset(0, 4),
                            ),
                          ],
                        ),
                        child: _nextTask != null
                            ? Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Container(
                                    width: 24,
                                    height: 24,
                                    margin: const EdgeInsets.only(top: 2),
                                    decoration: BoxDecoration(
                                      shape: BoxShape.circle,
                                      border: Border.all(
                                        color: const Color(0xFF84736F),
                                        width: 2,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 16),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          _nextTask!.title,
                                          style: const TextStyle(
                                            fontSize: 16,
                                            color: Color(0xFF271812),
                                            fontFamily: 'Lexend',
                                          ),
                                        ),
                                        const SizedBox(height: 8),
                                        Container(
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 8,
                                            vertical: 4,
                                          ),
                                          decoration: BoxDecoration(
                                            color: const Color(0xFFFFE2D9),
                                            borderRadius: BorderRadius.circular(
                                              6,
                                            ),
                                          ),
                                          child: Row(
                                            mainAxisSize: MainAxisSize.min,
                                            children: [
                                              const Icon(
                                                Icons.schedule,
                                                size: 14,
                                                color: Color(0xFF524440),
                                              ),
                                              const SizedBox(width: 4),
                                              Text(
                                                '${_nextTask!.dueAt.hour.toString().padLeft(2, '0')}:${_nextTask!.dueAt.minute.toString().padLeft(2, '0')}',
                                                style: const TextStyle(
                                                  fontSize: 12,
                                                  color: Color(0xFF524440),
                                                  fontFamily: 'Lexend',
                                                ),
                                              ),
                                            ],
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              )
                            : const Text(
                                'Không có nhiệm vụ nào.',
                                style: TextStyle(
                                  color: Color(0xFF524440),
                                  fontFamily: 'Lexend',
                                ),
                              ),
                      ),

                      const SizedBox(height: 24),

                      // Schedule Card
                      Row(
                        children: [
                          const Icon(
                            Icons.event,
                            color: Color(0xFF6E5A52),
                            size: 24,
                          ),
                          const SizedBox(width: 8),
                          const Text(
                            'Lịch chăm sóc',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF271812),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(24),
                          border: Border.all(color: const Color(0xFFFFF1EC)),
                          boxShadow: const [
                            BoxShadow(
                              color: Color(0x0F5A463F),
                              blurRadius: 20,
                              offset: Offset(0, 4),
                            ),
                          ],
                        ),
                        clipBehavior: Clip.antiAlias,
                        child: Stack(
                          children: [
                            Positioned(
                              left: -20,
                              top: -20,
                              bottom: -20,
                              width: 8,
                              child: Container(color: const Color(0xFFF6DACF)),
                            ),
                            _nextSchedule != null
                                ? Padding(
                                    padding: const EdgeInsets.only(left: 8),
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Row(
                                          children: [
                                            const Icon(
                                              Icons.schedule,
                                              size: 16,
                                              color: Color(0xFF6E5A52),
                                            ),
                                            const SizedBox(width: 4),
                                            Text(
                                              _formatDate(
                                                _nextSchedule!['dueAt']
                                                    as String?,
                                              ),
                                              style: const TextStyle(
                                                fontSize: 12,
                                                color: Color(0xFF6E5A52),
                                                fontFamily: 'Lexend',
                                              ),
                                            ),
                                          ],
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          _nextSchedule!['title'] as String? ??
                                              'Sự kiện',
                                          style: const TextStyle(
                                            fontSize: 16,
                                            fontWeight: FontWeight.bold,
                                            color: Color(0xFF271812),
                                            fontFamily: 'Lexend',
                                          ),
                                        ),
                                      ],
                                    ),
                                  )
                                : const Padding(
                                    padding: EdgeInsets.only(left: 8),
                                    child: Text(
                                      'Không có lịch trình sắp tới.',
                                      style: TextStyle(
                                        color: Color(0xFF524440),
                                        fontFamily: 'Lexend',
                                      ),
                                    ),
                                  ),
                          ],
                        ),
                      ),

                      const SizedBox(height: 24),

                      // Notification Card
                      Row(
                        children: [
                          const Icon(
                            Icons.notifications_active,
                            color: Color(0xFF845143),
                            size: 24,
                          ),
                          const SizedBox(width: 8),
                          const Text(
                            'Thông báo mới nhất',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF271812),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: const Color(0x1AC98C7B),
                          borderRadius: BorderRadius.circular(24),
                          border: Border.all(color: const Color(0x33C98C7B)),
                        ),
                        child: _latestAlert != null
                            ? Row(
                                children: [
                                  Container(
                                    width: 40,
                                    height: 40,
                                    decoration: const BoxDecoration(
                                      color: Color(0xFFC98C7B),
                                      shape: BoxShape.circle,
                                    ),
                                    child: const Icon(
                                      Icons.bedtime,
                                      color: Colors.white,
                                      size: 20,
                                    ),
                                  ),
                                  const SizedBox(width: 16),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          _latestAlert!['createdAt'] != null
                                              ? _formatDate(
                                                  _latestAlert!['createdAt'],
                                                )
                                              : 'Mới đây',
                                          style: const TextStyle(
                                            fontSize: 12,
                                            color: Color(0xFF845143),
                                            fontFamily: 'Lexend',
                                          ),
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          _latestAlert!['title'] as String? ??
                                              '',
                                          style: const TextStyle(
                                            fontSize: 16,
                                            color: Color(0xFF271812),
                                            fontFamily: 'Lexend',
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              )
                            : const Text(
                                'Không có cảnh báo.',
                                style: TextStyle(
                                  color: Color(0xFF524440),
                                  fontFamily: 'Lexend',
                                ),
                              ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          color: Color(0xFFFEF8F4),
          boxShadow: [
            BoxShadow(
              color: Color(0x0F5A463F),
              blurRadius: 20,
              offset: Offset(0, -4),
            ),
          ],
          borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: 16.0,
              vertical: 8.0,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _BottomNavItem(
                  icon: Icons.home,
                  label: 'Tổng quan',
                  isSelected: true,
                  onTap: () {},
                ),
                _BottomNavItem(
                  icon: Icons.group,
                  label: 'Nhóm',
                  onTap: () {
                    Navigator.pushReplacement(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const MyCareGroupsScreen(),
                      ),
                    );
                  },
                ),
                _BottomNavItem(
                  icon: Icons.calendar_month,
                  label: 'Lịch',
                  onTap: () {},
                ),
                _BottomNavItem(
                  icon: Icons.notifications,
                  label: 'Thông báo',
                  onTap: () {},
                ),
                _BottomNavItem(
                  icon: Icons.person,
                  label: 'Hồ sơ',
                  onTap: () {},
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _formatDate(String? isoDate) {
    if (isoDate == null) return '';
    try {
      final date = DateTime.parse(isoDate).toLocal();
      return '${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')} ngày ${date.day}/${date.month}';
    } catch (e) {
      return '';
    }
  }
}

class _ShortcutButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final Color bgColor;
  final VoidCallback onTap;

  const _ShortcutButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.bgColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: bgColor,
              borderRadius: BorderRadius.circular(19.2),
            ),
            child: Icon(icon, color: color, size: 24),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              color: Color(0xFF524440),
              fontFamily: 'Lexend',
            ),
            textAlign: TextAlign.center,
            maxLines: 1,
          ),
        ],
      ),
    );
  }
}

class _BottomNavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool isSelected;
  final VoidCallback onTap;

  const _BottomNavItem({
    required this.icon,
    required this.label,
    this.isSelected = false,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: isSelected ? const Color(0x33C98C7B) : Colors.transparent,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              icon,
              color: isSelected
                  ? const Color(0xFF845143)
                  : const Color(0xFF735E56),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              color: Color(0xFF735E56),
              fontFamily: 'Lexend',
            ),
          ),
        ],
      ),
    );
  }
}
