import 'package:flutter/material.dart';
import '../models/family_task_model.dart';
import '../services/family_task_service.dart';

/// CB-028 — Assigned / Group Tasks Screen (Read-Only)
/// Displays tasks assigned through the group-scoped care-task resource.
/// Read-only view with filter tabs (Cần làm, Bỏ qua, Đã xong, Quá hạn).
class AssignedTasksScreen extends StatefulWidget {
  final String groupId;
  final String groupName;

  const AssignedTasksScreen({
    super.key,
    required this.groupId,
    required this.groupName,
  });

  @override
  State<AssignedTasksScreen> createState() => _AssignedTasksScreenState();
}

class _AssignedTasksScreenState extends State<AssignedTasksScreen> {
  final _familyTaskService = FamilyTaskService();
  bool _isLoading = true;
  List<FamilyTask> _allTasks = [];
  String _currentFilter = 'TODO'; // TODO, SKIPPED, COMPLETED, OVERDUE

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final tasks = await _familyTaskService.listTasks(widget.groupId);
      if (mounted) {
        setState(() {
          _allTasks = tasks;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    List<FamilyTask> filteredTasks = _allTasks.where((t) {
      final completed =
          t.status.toUpperCase() == 'COMPLETED' ||
          t.status.toUpperCase() == 'DONE';
      final skipped =
          t.status.toUpperCase() == 'CANCELLED' ||
          t.status.toUpperCase() == 'SKIPPED';
      final isOverdue = !completed && !skipped && t.dueAt.isBefore(now);
      if (_currentFilter == 'OVERDUE') {
        return isOverdue;
      }
      if (_currentFilter == 'COMPLETED') {
        return completed;
      }
      if (_currentFilter == 'SKIPPED') {
        return skipped;
      }
      // TODO filter
      return !completed && !skipped && !isOverdue;
    }).toList();

    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: const Color(0xFFF6F1EC),
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Việc cần làm',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 24,
            fontWeight: FontWeight.bold,
            fontFamily: 'Lexend',
          ),
        ),
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: 24.0,
              vertical: 8.0,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Nhiệm vụ của bạn',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF271812),
                    fontFamily: 'Lexend',
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Hôm nay, ${now.day} tháng ${now.month}',
                  style: const TextStyle(
                    fontSize: 14,
                    color: Color(0xFF524440),
                    fontFamily: 'Lexend',
                  ),
                ),
              ],
            ),
          ),

          // Filter Tabs
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(
              horizontal: 20.0,
              vertical: 8.0,
            ),
            child: Row(
              children: [
                _buildFilterTab('TODO', 'Cần làm'),
                const SizedBox(width: 8),
                _buildFilterTab('SKIPPED', 'Bỏ qua'),
                const SizedBox(width: 8),
                _buildFilterTab('COMPLETED', 'Đã xong'),
                const SizedBox(width: 8),
                _buildFilterTab('OVERDUE', 'Quá hạn'),
              ],
            ),
          ),

          Expanded(
            child: _isLoading
                ? const Center(
                    child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
                  )
                : RefreshIndicator(
                    color: const Color(0xFFC98C7B),
                    onRefresh: _loadData,
                    child: filteredTasks.isEmpty
                        ? ListView(
                            padding: const EdgeInsets.all(24),
                            children: const [
                              Center(
                                child: Padding(
                                  padding: EdgeInsets.all(32.0),
                                  child: Column(
                                    children: [
                                      Icon(
                                        Icons.task_alt,
                                        size: 48,
                                        color: Color(0xFFA09A95),
                                      ),
                                      SizedBox(height: 16),
                                      Text(
                                        'Không có nhiệm vụ nào',
                                        style: TextStyle(
                                          color: Color(0xFF524440),
                                          fontFamily: 'Lexend',
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.all(24),
                            itemCount: filteredTasks.length,
                            itemBuilder: (context, index) {
                              return _buildTaskCard(filteredTasks[index]);
                            },
                          ),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterTab(String id, String label) {
    final isActive = _currentFilter == id;
    return GestureDetector(
      onTap: () => setState(() => _currentFilter = id),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 10),
        decoration: BoxDecoration(
          color: isActive ? const Color(0xFFC98C7B) : Colors.white,
          borderRadius: BorderRadius.circular(999),
          border: isActive ? null : Border.all(color: const Color(0xFFD6C2BD)),
          boxShadow: isActive
              ? const [
                  BoxShadow(
                    color: Color(0x0F5A463F),
                    blurRadius: 20,
                    offset: Offset(0, 4),
                  ),
                ]
              : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isActive ? Colors.white : const Color(0xFF524440),
            fontWeight: FontWeight.bold,
            fontFamily: 'Lexend',
          ),
        ),
      ),
    );
  }

  Widget _buildTaskCard(FamilyTask task) {
    final completed =
        task.status.toUpperCase() == 'COMPLETED' ||
        task.status.toUpperCase() == 'DONE';
    final skipped =
        task.status.toUpperCase() == 'CANCELLED' ||
        task.status.toUpperCase() == 'SKIPPED';
    Color indicatorColor;
    if (completed) {
      indicatorColor = Colors.green;
    } else if (skipped) {
      indicatorColor = const Color(0xFF84736F);
    } else if (task.dueAt.isBefore(DateTime.now())) {
      indicatorColor = const Color(0xFFBA1A1A);
    } else {
      indicatorColor = const Color(0xFFA09A95);
    }

    String statusText = 'Cần làm';
    if (completed) {
      statusText = 'Đã xong';
    } else if (skipped) {
      statusText = 'Bỏ qua';
    } else if (task.dueAt.isBefore(DateTime.now())) {
      statusText = 'Quá hạn';
    }

    return GestureDetector(
      onTap: () {
        _showReadOnlyTaskDetails(task);
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(24),
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
              left: 0,
              top: 0,
              bottom: 0,
              width: 4,
              child: Container(color: indicatorColor),
            ),
            Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFE9E3),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Row(
                          children: [
                            const Icon(
                              Icons.info,
                              size: 14,
                              color: Color(0xFF524440),
                            ),
                            const SizedBox(width: 4),
                            Text(
                              statusText,
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF524440),
                                fontFamily: 'Lexend',
                              ),
                            ),
                          ],
                        ),
                      ),
                      const Icon(
                        Icons.remove_red_eye_outlined,
                        size: 18,
                        color: Color(0xFF84736F),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    task.title,
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF271812),
                      fontFamily: 'Lexend',
                    ),
                  ),
                  const SizedBox(height: 4),
                  Builder(
                    builder: (context) {
                      final DateTime? dueLocal = task.dueAt.toLocal();
                      return Row(
                        children: [
                          const Icon(
                            Icons.schedule,
                            size: 16,
                            color: Color(0xFF524440),
                          ),
                          const SizedBox(width: 4),
                          Text(
                            dueLocal == null
                                ? 'Chưa xếp lịch'
                                : '${dueLocal.hour.toString().padLeft(2, '0')}:${dueLocal.minute.toString().padLeft(2, '0')} - ${dueLocal.day}/${dueLocal.month}',
                            style: const TextStyle(
                              fontSize: 14,
                              color: Color(0xFF524440),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ],
                      );
                    },
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showReadOnlyTaskDetails(FamilyTask task) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      backgroundColor: Colors.white,
      builder: (context) {
        return Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Chi tiết nhiệm vụ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF271812),
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFADCD3),
                      borderRadius: BorderRadius.circular(99),
                    ),
                    child: const Text(
                      'Chỉ xem',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF845143),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Text(
                task.title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF271812),
                ),
              ),
              const SizedBox(height: 12),
              Builder(
                builder: (context) {
                  final DateTime? dueLocal = task.dueAt.toLocal();
                  return Row(
                    children: [
                      const Icon(
                        Icons.event,
                        color: Color(0xFF845143),
                        size: 20,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        dueLocal == null
                            ? 'Chưa xếp lịch'
                            : 'Hạn chót: ${dueLocal.hour.toString().padLeft(2, '0')}:${dueLocal.minute.toString().padLeft(2, '0')} - ${dueLocal.day}/${dueLocal.month}/${dueLocal.year}',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: Color(0xFF524440),
                        ),
                      ),
                    ],
                  );
                },
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  const Icon(
                    Icons.info_outline,
                    color: Color(0xFF845143),
                    size: 20,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Loại: Công việc gia đình',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: Color(0xFF524440),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: () => Navigator.pop(context),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text(
                    'Đóng',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      color: Color(0xFF845143),
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
