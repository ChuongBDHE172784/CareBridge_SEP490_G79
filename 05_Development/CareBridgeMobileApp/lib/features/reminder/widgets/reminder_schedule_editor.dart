import 'package:flutter/material.dart';

import '../../../core/network/api_client.dart';
import '../models/reminder_schedule_model.dart';
import '../services/reminder_schedule_service.dart';

class ReminderScheduleEditor extends StatefulWidget {
  const ReminderScheduleEditor({
    super.key,
    this.initial,
    this.initialTitle,
    required this.service,
  });

  final ReminderSchedule? initial;
  final String? initialTitle;
  final ReminderScheduleService service;

  @override
  State<ReminderScheduleEditor> createState() => _ReminderScheduleEditorState();
}

class _ReminderScheduleEditorState extends State<ReminderScheduleEditor> {
  static const _primary = Color(0xFF845143);
  static const _text = Color(0xFF4A3831);
  static const _muted = Color(0xFF765F55);
  static const _border = Color(0xFFE7DCD5);

  late final TextEditingController _title;
  late List<TimeOfDay> _times;
  late ReminderScheduleRecurrence _recurrence;
  late DateTime _selectedDate;
  bool _saving = false;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    _title = TextEditingController(
      text: widget.initial?.title ?? widget.initialTitle ?? '',
    );

    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    if (widget.initial != null) {
      _selectedDate = widget.initial!.startDate;
      _times = widget.initial!.times
          .map(_parseTime)
          .whereType<TimeOfDay>()
          .toList();
      if (_times.isEmpty) _times = [const TimeOfDay(hour: 7, minute: 0)];
    } else {
      // If after 20:00, default date is tomorrow and default time is 08:00.
      // Otherwise, default date is today and default time is next upcoming hour.
      if (now.hour >= 20) {
        _selectedDate = today.add(const Duration(days: 1));
        _times = [const TimeOfDay(hour: 8, minute: 0)];
      } else {
        _selectedDate = today;
        _times = [TimeOfDay(hour: now.hour + 1, minute: 0)];
      }
    }

    _recurrence =
        widget.initial?.recurrence ?? ReminderScheduleRecurrence.daily;
  }

  @override
  void dispose() {
    _title.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate.isBefore(today) ? today : _selectedDate,
      firstDate: today,
      lastDate: today.add(const Duration(days: 365)),
    );
    if (picked != null) {
      setState(() {
        _selectedDate = picked;
        _errorText = null;
      });
    }
  }

  Future<void> _addTime() async {
    final now = DateTime.now();
    final picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay(hour: (now.hour + 1) % 24, minute: 0),
    );
    if (picked == null || _times.any((value) => _sameTime(value, picked))) {
      return;
    }
    setState(() {
      _times.add(picked);
      _times.sort((a, b) => _minutes(a).compareTo(_minutes(b)));
      if (_errorText != null && _times.isNotEmpty) _errorText = null;
    });
  }

  Future<void> _save() async {
    final title = _title.text.trim();
    if (title.isEmpty) {
      setState(() => _errorText = 'Vui lòng nhập nội dung nhắc.');
      return;
    }
    if (_times.isEmpty) {
      setState(() => _errorText = 'Vui lòng thêm ít nhất một giờ nhắc.');
      return;
    }

    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    if (_recurrence == ReminderScheduleRecurrence.none) {
      final isFuture = _times.any((time) {
        final reminderDt = DateTime(
          _selectedDate.year,
          _selectedDate.month,
          _selectedDate.day,
          time.hour,
          time.minute,
        );
        return reminderDt.isAfter(now);
      });
      if (!isFuture) {
        setState(() {
          _errorText = _isSameDay(_selectedDate, today)
              ? 'Giờ nhắc hôm nay đã qua. Vui lòng chọn giờ sau ${TimeOfDay.fromDateTime(now).format(context)} hoặc đổi sang ngày mai.'
              : 'Thời gian nhắc phải ở tương lai.';
        });
        return;
      }
    }

    setState(() {
      _saving = true;
      _errorText = null;
    });
    final times = _times.map(_formatTime).toList();
    try {
      if (widget.initial == null) {
        await widget.service.create(
          title: title,
          times: times,
          timeZone: _deviceTimeZone(),
          recurrence: _recurrence,
          startDate: _selectedDate,
        );
      } else {
        await widget.service.update(
          widget.initial!.id,
          title: title,
          times: times,
          recurrence: _recurrence,
          startDate: _selectedDate,
        );
      }
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        String msg = 'Không thể lưu lịch nhắc.';
        if (e is ApiException) {
          final display = e.displayMessage;
          if (display.contains('future configured time')) {
            msg = 'Lịch nhắc một lần phải có thời gian ở tương lai.';
          } else if (display.isNotEmpty) {
            msg = display;
          }
        }
        setState(() => _errorText = msg);
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.initial != null;
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
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: const BoxDecoration(
                  color: Color(0xFFFFEEE8),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.alarm_add_rounded,
                  color: _primary,
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  isEditing ? 'Chỉnh sửa lịch nhắc' : 'Tạo lịch nhắc nhanh',
                  style: const TextStyle(
                    fontFamily: 'Quicksand',
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: _text,
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.close_rounded),
                onPressed: () => Navigator.pop(context, false),
                tooltip: 'Đóng',
              ),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            key: const Key('reminder-schedule-title-input'),
            controller: _title,
            onChanged: (_) {
              if (_errorText != null) {
                setState(() => _errorText = null);
              }
            },
            decoration: InputDecoration(
              labelText: 'Nội dung nhắc',
              errorText: _errorText,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: _border),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: _border),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: _primary, width: 1.5),
              ),
            ),
          ),
          const SizedBox(height: 16),
          DropdownButtonFormField<ReminderScheduleRecurrence>(
            key: const Key('reminder-schedule-recurrence-dropdown'),
            initialValue: _recurrence,
            decoration: InputDecoration(
              labelText: 'Lặp lại',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: _border),
              ),
            ),
            items: const [
              DropdownMenuItem(
                value: ReminderScheduleRecurrence.none,
                child: Text('Một lần'),
              ),
              DropdownMenuItem(
                value: ReminderScheduleRecurrence.daily,
                child: Text('Hàng ngày'),
              ),
            ],
            onChanged: _saving
                ? null
                : (value) => setState(() {
                    _recurrence = value!;
                    _errorText = null;
                  }),
          ),
          const SizedBox(height: 16),
          Text(
            _recurrence == ReminderScheduleRecurrence.none
                ? 'Ngày nhắc'
                : 'Bắt đầu từ ngày',
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: _muted,
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              ChoiceChip(
                key: const Key('reminder-schedule-date-today'),
                label: const Text(
                  'Hôm nay',
                  style: TextStyle(
                    fontFamily: 'Quicksand',
                    fontWeight: FontWeight.w700,
                  ),
                ),
                selected: _isSameDay(_selectedDate, DateTime.now()),
                onSelected: (selected) {
                  if (selected) {
                    setState(() {
                      final n = DateTime.now();
                      _selectedDate = DateTime(n.year, n.month, n.day);
                      _errorText = null;
                    });
                  }
                },
              ),
              ChoiceChip(
                key: const Key('reminder-schedule-date-tomorrow'),
                label: const Text(
                  'Ngày mai',
                  style: TextStyle(
                    fontFamily: 'Quicksand',
                    fontWeight: FontWeight.w700,
                  ),
                ),
                selected: _isSameDay(
                  _selectedDate,
                  DateTime.now().add(const Duration(days: 1)),
                ),
                onSelected: (selected) {
                  if (selected) {
                    setState(() {
                      final t = DateTime.now().add(const Duration(days: 1));
                      _selectedDate = DateTime(t.year, t.month, t.day);
                      _errorText = null;
                    });
                  }
                },
              ),
              ActionChip(
                key: const Key('reminder-schedule-date-pick'),
                avatar: const Icon(Icons.calendar_today_rounded, size: 16),
                label: Text(
                  !_isSameDay(_selectedDate, DateTime.now()) &&
                          !_isSameDay(
                            _selectedDate,
                            DateTime.now().add(const Duration(days: 1)),
                          )
                      ? '${_selectedDate.day.toString().padLeft(2, '0')}/${_selectedDate.month.toString().padLeft(2, '0')}/${_selectedDate.year}'
                      : 'Chọn ngày',
                  style: const TextStyle(
                    fontFamily: 'Quicksand',
                    fontWeight: FontWeight.w700,
                  ),
                ),
                onPressed: _saving ? null : _pickDate,
              ),
            ],
          ),
          const SizedBox(height: 16),
          const Text(
            'Giờ nhắc',
            style: TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: _muted,
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              ..._times.map(
                (time) => InputChip(
                  label: Text(
                    _formatTime(time),
                    style: const TextStyle(
                      fontFamily: 'Quicksand',
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  onDeleted: _times.length == 1
                      ? null
                      : () => setState(() => _times.remove(time)),
                ),
              ),
              ActionChip(
                key: const Key('reminder-schedule-add-time-button'),
                avatar: const Icon(Icons.add_alarm_rounded, size: 18),
                label: const Text(
                  'Thêm giờ',
                  style: TextStyle(
                    fontFamily: 'Quicksand',
                    fontWeight: FontWeight.w700,
                  ),
                ),
                onPressed: _saving ? null : _addTime,
              ),
            ],
          ),
          const SizedBox(height: 20),
          SizedBox(
            height: 50,
            child: FilledButton(
              key: const Key('reminder-schedule-save-button'),
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              onPressed: _saving ? null : _save,
              child: _saving
                  ? const SizedBox.square(
                      dimension: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Text(
                      isEditing ? 'Cập nhật' : 'Lưu lịch nhắc',
                      style: const TextStyle(
                        fontFamily: 'Quicksand',
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }

  static bool _isSameDay(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

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
    try {
      final offset = DateTime.now().timeZoneOffset;
      if (offset.inHours == 7 && !offset.isNegative) {
        return 'Asia/Ho_Chi_Minh';
      }
      final totalMinutes = offset.inMinutes.abs();
      final sign = offset.isNegative ? '-' : '+';
      final hours = (totalMinutes ~/ 60).toString().padLeft(2, '0');
      final minutes = (totalMinutes % 60).toString().padLeft(2, '0');
      return 'UTC$sign$hours:$minutes';
    } catch (_) {
      return 'Asia/Ho_Chi_Minh';
    }
  }
}
