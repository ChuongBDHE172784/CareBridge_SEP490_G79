enum ReminderType { appointment, medication, vaccination, task, other }

extension ReminderTypeExtension on ReminderType {
  String get displayLabel {
    switch (this) {
      case ReminderType.appointment:
        return 'Lịch hẹn';
      case ReminderType.medication:
        return 'Thuốc / vitamin';
      case ReminderType.vaccination:
        return 'Tiêm chủng';
      case ReminderType.task:
        return 'Công việc';
      case ReminderType.other:
        return 'Khác';
    }
  }

  static ReminderType fromApi(String? value) {
    switch (value) {
      case 'APPOINTMENT':
        return ReminderType.appointment;
      case 'MEDICATION':
        return ReminderType.medication;
      case 'VACCINATION':
        return ReminderType.vaccination;
      case 'TASK':
      case 'CARE_TASK':
        return ReminderType.task;
      default:
        return ReminderType.other;
    }
  }
}

enum ReminderStatus { pending, done, snoozed, skipped, cancelled }

extension ReminderStatusExtension on ReminderStatus {
  String get displayLabel {
    switch (this) {
      case ReminderStatus.pending:
        return 'Đang chờ';
      case ReminderStatus.done:
        return 'Đã hoàn thành';
      case ReminderStatus.snoozed:
        return 'Đã hoãn';
      case ReminderStatus.skipped:
        return 'Đã bỏ qua';
      case ReminderStatus.cancelled:
        return 'Đã tắt';
    }
  }

  bool get isTerminal => this == ReminderStatus.cancelled;

  static ReminderStatus fromApi(String? value) {
    switch (value) {
      case 'DONE':
      case 'COMPLETED':
        return ReminderStatus.done;
      case 'SNOOZED':
        return ReminderStatus.snoozed;
      case 'SKIPPED':
        return ReminderStatus.skipped;
      case 'CANCELLED':
        return ReminderStatus.cancelled;
      default:
        return ReminderStatus.pending;
    }
  }
}

enum RecurrenceType { none, daily, weekly, monthly }

extension RecurrenceTypeExtension on RecurrenceType {
  String get displayLabel {
    switch (this) {
      case RecurrenceType.none:
        return 'Không lặp lại';
      case RecurrenceType.daily:
        return 'Hằng ngày';
      case RecurrenceType.weekly:
        return 'Hằng tuần';
      case RecurrenceType.monthly:
        return 'Hằng tháng';
    }
  }

  static RecurrenceType fromApi(String? value) {
    switch (value) {
      case 'DAILY':
        return RecurrenceType.daily;
      case 'WEEKLY':
        return RecurrenceType.weekly;
      case 'MONTHLY':
        return RecurrenceType.monthly;
      default:
        return RecurrenceType.none;
    }
  }
}

extension RecurrenceTypeApi on RecurrenceType {
  String toApiValue() {
    switch (this) {
      case RecurrenceType.daily:
        return 'DAILY';
      case RecurrenceType.weekly:
        return 'WEEKLY';
      case RecurrenceType.monthly:
        return 'MONTHLY';
      case RecurrenceType.none:
        return 'NONE';
    }
  }
}

enum ReminderAssignee { mother, baby }

extension ReminderAssigneeExtension on ReminderAssignee {
  String get displayLabel {
    switch (this) {
      case ReminderAssignee.mother:
        return 'Mẹ';
      case ReminderAssignee.baby:
        return 'Bé';
    }
  }
}

class Reminder {
  final String id;
  final ReminderType reminderType;
  final String title;
  final DateTime scheduledAt;
  final RecurrenceType recurrenceType;
  final DateTime? recurrenceEndDate;
  final ReminderStatus status;
  final bool isImportant;
  final String? location;
  final String? note;
  final ReminderAssignee assignee;
  final List<int> notificationOffsetsMinutes;
  final String? timeZone;

  const Reminder({
    required this.id,
    required this.reminderType,
    required this.title,
    required this.scheduledAt,
    this.recurrenceType = RecurrenceType.none,
    this.recurrenceEndDate,
    this.status = ReminderStatus.pending,
    this.isImportant = false,
    this.location,
    this.note,
    this.assignee = ReminderAssignee.mother,
    this.notificationOffsetsMinutes = const [],
    this.timeZone,
  });

  factory Reminder.fromJson(Map<String, dynamic> json) {
    return Reminder(
      id: json['id'] as String,
      reminderType: ReminderTypeExtension.fromApi(
        (json['reminderType'] ?? json['type']) as String?,
      ),
      title: json['title'] as String,
      scheduledAt: DateTime.parse(json['scheduledAt'] as String),
      recurrenceType: RecurrenceTypeExtension.fromApi(
        json['recurrenceType'] as String?,
      ),
      recurrenceEndDate: json['recurrenceEndDate'] == null
          ? null
          : DateTime.parse(json['recurrenceEndDate'] as String),
      status: ReminderStatusExtension.fromApi(json['status'] as String?),
      location: json['location'] as String?,
      note: json['note'] as String?,
      notificationOffsetsMinutes:
          (json['notificationOffsetsMinutes'] as List<dynamic>? ?? const [])
              .whereType<num>()
              .map((value) => value.toInt())
              .toList(),
      timeZone: json['timeZone'] as String?,
    );
  }
}
