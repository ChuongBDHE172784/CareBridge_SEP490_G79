import 'package:flutter/material.dart';

import '../models/reminder_schedule_model.dart';
import '../services/reminder_schedule_service.dart';
import '../widgets/reminder_schedule_editor.dart';

class ReminderSchedulesScreen extends StatefulWidget {
  const ReminderSchedulesScreen({super.key, this.scheduleId});

  final String? scheduleId;

  @override
  State<ReminderSchedulesScreen> createState() =>
      _ReminderSchedulesScreenState();
}

class _ReminderSchedulesScreenState extends State<ReminderSchedulesScreen> {
  final _service = ReminderScheduleService.instance;
  List<ReminderSchedule> _schedules = const [];
  bool _loading = true;
  String? _error;
  bool _openedInitial = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (!mounted) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final schedules = await _service.list();
      if (!mounted) return;
      setState(() {
        _schedules = schedules;
        _loading = false;
      });
      final id = widget.scheduleId;
      if (id != null && !_openedInitial) {
        final schedule = schedules.where((item) => item.id == id).firstOrNull;
        if (schedule != null) {
          _openedInitial = true;
          WidgetsBinding.instance.addPostFrameCallback((_) => _edit(schedule));
        } else if (mounted) {
          setState(() => _error = 'Không tìm thấy lịch nhắc.');
        }
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Không thể tải lịch nhắc.';
      });
    }
  }

  Future<void> _edit([ReminderSchedule? schedule]) async {
    final changed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) =>
          ReminderScheduleEditor(initial: schedule, service: _service),
    );
    if (changed == true) _load();
  }

  Future<void> _delete(ReminderSchedule schedule) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Xóa lịch nhắc?'),
        content: Text('Bạn có chắc chắn muốn xóa lịch nhắc "${schedule.title}" không?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Hủy')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: const Color(0xFFBA1A1A)),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await _service.delete(schedule.id);
      await _load();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể xóa lịch nhắc.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Lịch nhắc')),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _edit(),
        child: const Icon(Icons.add_alarm_rounded),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(child: Text(_error!))
          : RefreshIndicator(
              onRefresh: _load,
              child: _schedules.isEmpty
                  ? ListView(
                      children: const [
                        SizedBox(height: 180),
                        Center(child: Text('Chưa có lịch nhắc.')),
                      ],
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: _schedules.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (_, index) {
                        final schedule = _schedules[index];
                        return Card(
                          child: ListTile(
                            title: Text(schedule.title),
                            subtitle: Text(
                              '${schedule.times.join(' · ')}  •  ${schedule.recurrence == ReminderScheduleRecurrence.daily ? 'Hàng ngày' : 'Một lần'}',
                            ),
                            leading: Icon(
                              schedule.active
                                  ? Icons.notifications_active_outlined
                                  : Icons.notifications_off_outlined,
                            ),
                            onTap: () => _edit(schedule),
                            trailing: IconButton(
                              tooltip: 'Xóa',
                              onPressed: () => _delete(schedule),
                              icon: const Icon(Icons.delete_outline),
                            ),
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}

