import 'package:flutter/material.dart';
import '../models/family_task_model.dart';
import '../services/family_task_service.dart';
import 'assign_care_task_screen.dart';
import 'family_task_detail_screen.dart';

class AssignedTasksScreen extends StatefulWidget {
  final String groupId;
  final String groupName;

  const AssignedTasksScreen({Key? key, required this.groupId, required this.groupName}) : super(key: key);

  @override
  State<AssignedTasksScreen> createState() => _AssignedTasksScreenState();
}

class _AssignedTasksScreenState extends State<AssignedTasksScreen> {
  final _service = FamilyTaskService();
  bool _isLoading = true;
  List<FamilyTask> _allTasks = [];
  String _currentFilter = 'TODO'; // TODO, IN_PROGRESS, COMPLETED, CANCELLED

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final tasks = await _service.listTasks(widget.groupId);
      if (mounted) {
        setState(() {
          _allTasks = tasks;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  Future<void> _updateStatus(FamilyTask task, String newStatus) async {
    try {
      await _service.updateTaskStatus(widget.groupId, task.taskId, newStatus);
      _loadData();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    List<FamilyTask> filteredTasks = _allTasks.where((t) {
      if (_currentFilter == 'OVERDUE') {
        return t.status != 'COMPLETED' && t.status != 'CANCELLED' && t.dueAt.isBefore(DateTime.now());
      }
      return t.status == _currentFilter && !(_currentFilter == 'TODO' && t.dueAt.isBefore(DateTime.now()));
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
        title: const Text('Việc cần làm', style: TextStyle(color: Color(0xFF845143), fontSize: 24, fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Nhiệm vụ của bạn', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                const SizedBox(height: 4),
                Text('Hôm nay, ${DateTime.now().day} tháng ${DateTime.now().month}', style: const TextStyle(fontSize: 14, color: Color(0xFF524440), fontFamily: 'Quicksand')),
              ],
            ),
          ),
          
          // Filter Tabs
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 8.0),
            child: Row(
              children: [
                _buildFilterTab('TODO', 'Cần làm'),
                const SizedBox(width: 8),
                _buildFilterTab('IN_PROGRESS', 'Đang làm'),
                const SizedBox(width: 8),
                _buildFilterTab('COMPLETED', 'Đã xong'),
                const SizedBox(width: 8),
                _buildFilterTab('OVERDUE', 'Quá hạn'),
              ],
            ),
          ),
          
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator(color: Color(0xFFC98C7B)))
                : RefreshIndicator(
                    color: const Color(0xFFC98C7B),
                    onRefresh: _loadData,
                    child: filteredTasks.isEmpty
                        ? ListView(
                            padding: const EdgeInsets.all(24),
                            children: const [
                              Center(child: Padding(
                                padding: EdgeInsets.all(32.0),
                                child: Column(
                                  children: [
                                    Icon(Icons.task_alt, size: 48, color: Color(0xFFA09A95)),
                                    SizedBox(height: 16),
                                    Text('Không có nhiệm vụ nào', style: TextStyle(color: Color(0xFF524440), fontFamily: 'Quicksand')),
                                  ],
                                ),
                              ))
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
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: const Color(0xFFC98C7B),
        foregroundColor: Colors.white,
        shape: const StadiumBorder(),
        onPressed: () async {
          final result = await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => AssignCareTaskScreen(
                groupId: widget.groupId,
                groupName: widget.groupName,
              ),
            ),
          );
          if (result == true) {
            _loadData();
          }
        },
        icon: const Icon(Icons.add_task),
        label: const Text('Giao việc mới', style: TextStyle(fontFamily: 'Quicksand', fontWeight: FontWeight.bold)),
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
          boxShadow: isActive ? const [BoxShadow(color: Color(0x0F5A463F), blurRadius: 20, offset: Offset(0, 4))] : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isActive ? Colors.white : const Color(0xFF524440),
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
          ),
        ),
      ),
    );
  }

  Widget _buildTaskCard(FamilyTask task) {
    Color indicatorColor;
    if (task.status == 'COMPLETED') indicatorColor = Colors.green;
    else if (task.status == 'IN_PROGRESS') indicatorColor = const Color(0xFF845143);
    else if (task.dueAt.isBefore(DateTime.now())) indicatorColor = const Color(0xFFBA1A1A);
    else indicatorColor = const Color(0xFFA09A95);

    return GestureDetector(
      onTap: () async {
        final result = await Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => FamilyTaskDetailScreen(task: task)),
        );
        if (result == true) {
          _loadData();
        }
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(24),
          boxShadow: const [BoxShadow(color: Color(0x0F5A463F), blurRadius: 20, offset: Offset(0, 4))],
        ),
        clipBehavior: Clip.antiAlias,
        child: Stack(
          children: [
            Positioned(
              left: 0, top: 0, bottom: 0,
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
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(color: const Color(0xFFFFE9E3), borderRadius: BorderRadius.circular(6)),
                        child: Row(
                          children: [
                            const Icon(Icons.info, size: 14, color: Color(0xFF524440)),
                            const SizedBox(width: 4),
                            Text(task.status, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF524440), fontFamily: 'Quicksand')),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(task.title, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      const Icon(Icons.schedule, size: 16, color: Color(0xFF524440)),
                      const SizedBox(width: 4),
                      Text('${task.dueAt.hour.toString().padLeft(2, '0')}:${task.dueAt.minute.toString().padLeft(2, '0')} - ${task.dueAt.day}/${task.dueAt.month}', 
                        style: const TextStyle(fontSize: 14, color: Color(0xFF524440), fontFamily: 'Quicksand'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  const Divider(color: Color(0xFFFFE2D9)),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      if (task.status == 'TODO')
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: () => _updateStatus(task, 'IN_PROGRESS'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFFFADCD3),
                              foregroundColor: const Color(0xFF524440),
                              minimumSize: const Size(0, 48),
                              elevation: 0,
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                            ),
                            icon: const Icon(Icons.play_arrow),
                            label: const Text('Bắt đầu làm', style: TextStyle(fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
                          ),
                        ),
                      if (task.status == 'IN_PROGRESS')
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: () => _updateStatus(task, 'COMPLETED'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFFC98C7B),
                              foregroundColor: Colors.white,
                              minimumSize: const Size(0, 48),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                            ),
                            icon: const Icon(Icons.check_circle),
                            label: const Text('Đã xong', style: TextStyle(fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
