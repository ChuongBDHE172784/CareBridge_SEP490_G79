import 'package:flutter/material.dart';

import '../models/reminder_schedule_model.dart';
import '../services/reminder_schedule_service.dart';

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
          setState(() => _error = 'Khong tim thay lich nhac.');
        }
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Khong the tai lich nhac.';
      });
    }
  }

  Future<void> _edit([ReminderSchedule? schedule]) async {
    final changed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) =>
          _ReminderScheduleEditor(initial: schedule, service: _service),
    );
    if (changed == true) _load();
  }

  Future<void> _toggle(ReminderSchedule schedule) async {
    try {
      await (schedule.active
          ? _service.disable(schedule.id)
          : _service.enable(schedule.id));
      await _load();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Khong the cap nhat lich nhac.')),
      );
    }
  }

  Future<void> _delete(ReminderSchedule schedule) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Tat lich nhac?'),
        content: Text('Lich "${schedule.title}" se dung nhac va co the bat lai.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Huy')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Tat')),
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
        const SnackBar(content: Text('Khong the xoa lich nhac.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Lich nhac')),
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
                        Center(child: Text('Chua co lich nhac.')),
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
                              '${schedule.times.join(' · ')}  •  ${schedule.recurrence == ReminderScheduleRecurrence.daily ? 'Hang ngay' : 'Mot lan'}',
                            ),
                            leading: Icon(
                              schedule.active
                                  ? Icons.notifications_active_outlined
                                  : Icons.notifications_off_outlined,
                            ),
                            onTap: () => _edit(schedule),
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                IconButton(
                                  tooltip: schedule.active ? 'Tat' : 'Bat',
                                  onPressed: () => _toggle(schedule),
                                  icon: Icon(
                                    schedule.active
                                        ? Icons.pause_circle_outline
                                        : Icons.play_circle_outline,
                                  ),
                                ),
                                IconButton(
                                  tooltip: 'Xoa',
                                  onPressed: () => _delete(schedule),
                                  icon: const Icon(Icons.delete_outline),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}

class _ReminderScheduleEditor extends StatefulWidget {
  const _ReminderScheduleEditor({this.initial, required this.service});

  final ReminderSchedule? initial;
  final ReminderScheduleService service;

  @override
  State<_ReminderScheduleEditor> createState() =>
      _ReminderScheduleEditorState();
}

class _ReminderScheduleEditorState extends State<_ReminderScheduleEditor> {
  late final TextEditingController _title;
  late List<TimeOfDay> _times;
  late ReminderScheduleRecurrence _recurrence;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _title = TextEditingController(text: widget.initial?.title ?? '');
    _times = (widget.initial?.times ?? const ['07:00'])
        .map(_parseTime)
        .whereType<TimeOfDay>()
        .toList();
    if (_times.isEmpty) _times = [const TimeOfDay(hour: 7, minute: 0)];
    _recurrence =
        widget.initial?.recurrence ?? ReminderScheduleRecurrence.daily;
  }

  @override
  void dispose() {
    _title.dispose();
    super.dispose();
  }

  Future<void> _addTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: const TimeOfDay(hour: 8, minute: 0),
    );
    if (picked == null || _times.any((value) => _sameTime(value, picked)))
      return;
    setState(() {
      _times.add(picked);
      _times.sort((a, b) => _minutes(a).compareTo(_minutes(b)));
    });
  }

  Future<void> _save() async {
    final title = _title.text.trim();
    if (title.isEmpty || _times.isEmpty) return;
    setState(() => _saving = true);
    final times = _times.map(_formatTime).toList();
    try {
      if (widget.initial == null) {
        await widget.service.create(
          title: title,
          times: times,
          timeZone: _deviceTimeZone(),
          recurrence: _recurrence,
          startDate: DateTime.now(),
        );
      } else {
        await widget.service.update(
          widget.initial!.id,
          title: title,
          times: times,
          recurrence: _recurrence,
        );
      }
      if (mounted) Navigator.pop(context, true);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Khong the luu lich nhac.')),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        20,
        20,
        MediaQuery.viewInsetsOf(context).bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _title,
            decoration: const InputDecoration(labelText: 'Noi dung nhac'),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            children: _times
                .map(
                  (time) => InputChip(
                    label: Text(_formatTime(time)),
                    onDeleted: _times.length == 1
                        ? null
                        : () => setState(() => _times.remove(time)),
                  ),
                )
                .toList(),
          ),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton.icon(
              onPressed: _saving ? null : _addTime,
              icon: const Icon(Icons.add_alarm_outlined),
              label: const Text('Them gio'),
            ),
          ),
          DropdownButtonFormField<ReminderScheduleRecurrence>(
            initialValue: _recurrence,
            items: const [
              DropdownMenuItem(
                value: ReminderScheduleRecurrence.none,
                child: Text('Mot lan'),
              ),
              DropdownMenuItem(
                value: ReminderScheduleRecurrence.daily,
                child: Text('Hang ngay'),
              ),
            ],
            onChanged: _saving
                ? null
                : (value) => setState(() => _recurrence = value!),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: _saving ? null : _save,
            child: Text(widget.initial == null ? 'Luu lich nhac' : 'Cap nhat'),
          ),
        ],
      ),
    );
  }

  static TimeOfDay? _parseTime(String value) {
    final parts = value.split(':');
    if (parts.length != 2) return null;
    final hour = int.tryParse(parts[0]);
    final minute = int.tryParse(parts[1]);
    if (hour == null || minute == null || hour > 23 || minute > 59) return null;
    return TimeOfDay(hour: hour, minute: minute);
  }

  static bool _sameTime(TimeOfDay left, TimeOfDay right) =>
      left.hour == right.hour && left.minute == right.minute;

  static int _minutes(TimeOfDay value) => value.hour * 60 + value.minute;

  static String _formatTime(TimeOfDay value) =>
      '${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}';

  static String _deviceTimeZone() {
    final offset = DateTime.now().timeZoneOffset;
    final totalMinutes = offset.inMinutes.abs();
    final sign = offset.isNegative ? '-' : '+';
    final hours = (totalMinutes ~/ 60).toString().padLeft(2, '0');
    final minutes = (totalMinutes % 60).toString().padLeft(2, '0');
    return 'UTC$sign$hours:$minutes';
  }
}
