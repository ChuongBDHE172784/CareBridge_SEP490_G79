enum ReminderScheduleRecurrence { none, daily }

extension ReminderScheduleRecurrenceApi on ReminderScheduleRecurrence {
  String get apiValue => switch (this) {
    ReminderScheduleRecurrence.none => 'NONE',
    ReminderScheduleRecurrence.daily => 'DAILY',
  };

  static ReminderScheduleRecurrence fromApi(String? value) =>
      value?.trim().toUpperCase() == 'DAILY'
      ? ReminderScheduleRecurrence.daily
      : ReminderScheduleRecurrence.none;
}

/// One logical alarm schedule. `times` are strict local `HH:mm` values and
/// are kept as strings so the API contract is independent of device locale.
class ReminderSchedule {
  final String id;
  final String title;
  final List<String> times;
  final String timeZone;
  final ReminderScheduleRecurrence recurrence;
  final DateTime startDate;
  final DateTime? endDate;
  final bool active;
  final int revision;

  const ReminderSchedule({
    required this.id,
    required this.title,
    required this.times,
    required this.timeZone,
    required this.recurrence,
    required this.startDate,
    required this.endDate,
    required this.active,
    required this.revision,
  });

  factory ReminderSchedule.fromJson(Map<String, dynamic> json) {
    final rawTimes = json['times'];
    final values = rawTimes is List
        ? rawTimes.whereType<String>().map((value) => value.trim()).toList()
        : const <String>[];
    final start = DateTime.tryParse(json['startDate'] as String? ?? '');
    if (start == null) {
      throw const FormatException('Reminder schedule startDate is required');
    }
    return ReminderSchedule(
      id: json['id'] as String? ?? json['scheduleId'] as String,
      title: json['title'] as String? ?? '',
      times: List.unmodifiable(values),
      timeZone: json['timeZone'] as String? ?? 'Asia/Ho_Chi_Minh',
      recurrence: ReminderScheduleRecurrenceApi.fromApi(
        json['recurrence'] as String?,
      ),
      startDate: start,
      endDate: DateTime.tryParse(json['endDate'] as String? ?? ''),
      active: json['active'] != false,
      revision: (json['revision'] as num?)?.toInt() ?? 1,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'times': times,
    'timeZone': timeZone,
    'recurrence': recurrence.apiValue,
    'startDate': _dateOnly(startDate),
    'endDate': endDate == null ? null : _dateOnly(endDate!),
    'active': active,
    'revision': revision,
  };

  static String _dateOnly(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}
