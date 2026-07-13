import 'package:flutter/material.dart';
import '../models/family_task_model.dart';
import '../services/family_task_service.dart';
import 'update_family_task_screen.dart';

class FamilyTaskDetailScreen extends StatefulWidget {
  final FamilyTask task;

  const FamilyTaskDetailScreen({super.key, required this.task});

  @override
  State<FamilyTaskDetailScreen> createState() => _FamilyTaskDetailScreenState();
}

class _FamilyTaskDetailScreenState extends State<FamilyTaskDetailScreen> {
  final _service = FamilyTaskService();
  late FamilyTask _task;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _task = widget.task;
  }

  Future<void> _updateStatus() async {
    setState(() => _isLoading = true);
    try {
      String newStatus = _task.status == 'TODO'
          ? 'IN_PROGRESS'
          : (_task.status == 'IN_PROGRESS' ? 'COMPLETED' : 'TODO');
      final updated = await _service.updateTaskStatus(
        _task.groupId,
        _task.taskId,
        newStatus,
      );
      if (mounted) {
        setState(() => _task = updated);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Đã cập nhật trạng thái')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _cancelTask() async {
    setState(() => _isLoading = true);
    try {
      await _service.cancelTask(_task.groupId, _task.taskId);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Đã hủy công việc')));
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () =>
              Navigator.pop(context, true), // Return true to refresh list
        ),
        title: const Text(
          'Chi tiết công việc',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 22,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit, color: Color(0xFF845143)),
            onPressed: () async {
              final result = await Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => UpdateFamilyTaskScreen(task: _task),
                ),
              );
              if (result == true && mounted) {
                // Refresh task detail
                final updated = await _service.getTaskDetail(
                  _task.groupId,
                  _task.taskId,
                );
                setState(() => _task = updated);
              }
            },
          ),
        ],
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Task Header Card
                Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x0F5A463F),
                        blurRadius: 24,
                        offset: Offset(0, 8),
                      ),
                    ],
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: Stack(
                    children: [
                      Positioned(
                        left: 0,
                        top: 0,
                        right: 0,
                        height: 8,
                        child: Container(color: const Color(0xFFC98C7B)),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 12,
                                    vertical: 4,
                                  ),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFFFDBD1),
                                    borderRadius: BorderRadius.circular(999),
                                  ),
                                  child: Row(
                                    children: [
                                      const Icon(
                                        Icons.family_restroom,
                                        size: 14,
                                        color: Color(0xFF693A2D),
                                      ),
                                      const SizedBox(width: 4),
                                      const Text(
                                        'Gia đình',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color: Color(0xFF693A2D),
                                          fontFamily: 'Quicksand',
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 12,
                                    vertical: 4,
                                  ),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFFADCD3),
                                    borderRadius: BorderRadius.circular(999),
                                  ),
                                  child: Text(
                                    _task.status,
                                    style: const TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF524440),
                                      fontFamily: 'Quicksand',
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 16),
                            Text(
                              _task.title,
                              style: const TextStyle(
                                fontSize: 24,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF271812),
                                fontFamily: 'Quicksand',
                              ),
                            ),
                            const SizedBox(height: 16),
                            const Divider(color: Color(0x4DD6C2BD)),
                            const SizedBox(height: 16),
                            Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      const Text(
                                        'NGƯỜI THỰC HIỆN',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color: Color(0xFF84736F),
                                          fontFamily: 'Quicksand',
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Row(
                                        children: [
                                          Container(
                                            width: 32,
                                            height: 32,
                                            decoration: const BoxDecoration(
                                              color: Color(0xFFF6DACF),
                                              shape: BoxShape.circle,
                                            ),
                                            child: const Icon(
                                              Icons.person,
                                              color: Color(0xFF735E56),
                                              size: 16,
                                            ),
                                          ),
                                          const SizedBox(width: 8),
                                          Text(
                                            _task.assignedToName,
                                            style: const TextStyle(
                                              fontSize: 14,
                                              color: Color(0xFF271812),
                                              fontFamily: 'Quicksand',
                                            ),
                                          ),
                                        ],
                                      ),
                                    ],
                                  ),
                                ),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      const Text(
                                        'NGƯỜI TẠO',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color: Color(0xFF84736F),
                                          fontFamily: 'Quicksand',
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Row(
                                        children: [
                                          Container(
                                            width: 32,
                                            height: 32,
                                            decoration: const BoxDecoration(
                                              color: Color(0xFFF6DACF),
                                              shape: BoxShape.circle,
                                            ),
                                            child: const Icon(
                                              Icons.person,
                                              color: Color(0xFF735E56),
                                              size: 16,
                                            ),
                                          ),
                                          const SizedBox(width: 8),
                                          Text(
                                            _task.assignedByName,
                                            style: const TextStyle(
                                              fontSize: 14,
                                              color: Color(0xFF271812),
                                              fontFamily: 'Quicksand',
                                            ),
                                          ),
                                        ],
                                      ),
                                    ],
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

                const SizedBox(height: 16),

                // Due Date
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x0D5A463F),
                        blurRadius: 16,
                        offset: Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Row(
                        children: [
                          Icon(Icons.event, color: Color(0xFF845143)),
                          SizedBox(width: 8),
                          Text(
                            'THỜI HẠN',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF845143),
                              fontFamily: 'Quicksand',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '${_task.dueAt.day}/${_task.dueAt.month}/${_task.dueAt.year} ${_task.dueAt.hour.toString().padLeft(2, '0')}:${_task.dueAt.minute.toString().padLeft(2, '0')}',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF271812),
                          fontFamily: 'Quicksand',
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 16),

                // Description
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x0D5A463F),
                        blurRadius: 16,
                        offset: Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Row(
                        children: [
                          Icon(Icons.description, color: Color(0xFF845143)),
                          SizedBox(width: 8),
                          Text(
                            'MÔ TẢ CHI TIẾT',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF845143),
                              fontFamily: 'Quicksand',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _task.description.isEmpty
                            ? 'Không có mô tả'
                            : _task.description,
                        style: const TextStyle(
                          fontSize: 16,
                          color: Color(0xFF271812),
                          fontFamily: 'Quicksand',
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 32),

                if (_task.status != 'COMPLETED' && _task.status != 'CANCELLED')
                  OutlinedButton.icon(
                    onPressed: _cancelTask,
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFFBA1A1A),
                      side: const BorderSide(color: Color(0xFFBA1A1A)),
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(999),
                      ),
                    ),
                    icon: const Icon(Icons.cancel),
                    label: const Text(
                      'Hủy công việc',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontFamily: 'Quicksand',
                      ),
                    ),
                  ),

                const SizedBox(height: 100), // Space for bottom button
              ],
            ),
          ),

          if (_isLoading)
            Container(
              color: Colors.black26,
              child: const Center(
                child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
              ),
            ),
        ],
      ),
      bottomSheet: Container(
        padding: const EdgeInsets.all(20),
        decoration: const BoxDecoration(
          color: Colors.white,
          boxShadow: [
            BoxShadow(
              color: Color(0x145A463F),
              blurRadius: 24,
              offset: Offset(0, -8),
            ),
          ],
        ),
        child: ElevatedButton.icon(
          onPressed: _updateStatus,
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF845143),
            foregroundColor: Colors.white,
            minimumSize: const Size(double.infinity, 48),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(999),
            ),
            elevation: 0,
          ),
          icon: const Icon(Icons.check_circle),
          label: Text(
            _task.status == 'TODO'
                ? 'Bắt đầu làm'
                : (_task.status == 'IN_PROGRESS'
                      ? 'Đã hoàn thành'
                      : 'Mở lại công việc'),
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              fontFamily: 'Quicksand',
            ),
          ),
        ),
      ),
    );
  }
}
