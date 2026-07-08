enum ReminderType { appointment, medication, vaccination, task, other }

extension ReminderTypeExtension on ReminderType {
  String get displayLabel {
    switch (this) {
      case ReminderType.appointment: return 'Lịch khám';
      case ReminderType.medication: return 'Thuốc';
      case ReminderType.vaccination: return 'Tiêm chủng';
      case ReminderType.task: return 'Việc cần làm';
      case ReminderType.other: return 'Khác';
    }
  }

  static ReminderType fromApi(String? v) {
    switch (v) {
      case 'APPOINTMENT': return ReminderType.appointment;
      case 'MEDICATION': return ReminderType.medication;
      case 'VACCINATION': return ReminderType.vaccination;
      case 'TASK': return ReminderType.task;
      default: return ReminderType.other;
    }
  }
}

enum ReminderStatus { pending, done, snoozed, skipped }

extension ReminderStatusExtension on ReminderStatus {
  String get displayLabel {
    switch (this) {
      case ReminderStatus.pending: return 'Chờ thực hiện';
      case ReminderStatus.done: return 'Đã hoàn thành';
      case ReminderStatus.snoozed: return 'Đã hoãn';
      case ReminderStatus.skipped: return 'Đã bỏ qua';
    }
  }

  static ReminderStatus fromApi(String? v) {
    switch (v) {
      case 'DONE': return ReminderStatus.done;
      case 'SNOOZED': return ReminderStatus.snoozed;
      case 'SKIPPED': return ReminderStatus.skipped;
      default: return ReminderStatus.pending;
    }
  }
}

enum RecurrenceType { none, daily, weekly, monthly }

extension RecurrenceTypeExtension on RecurrenceType {
  String get displayLabel {
    switch (this) {
      case RecurrenceType.none: return 'Không lặp';
      case RecurrenceType.daily: return '1 Lần / Ngày';
      case RecurrenceType.weekly: return '1 Lần / Tuần';
      case RecurrenceType.monthly: return '1 Lần / Tháng';
    }
  }

  static RecurrenceType fromApi(String? v) {
    switch (v) {
      case 'DAILY': return RecurrenceType.daily;
      case 'WEEKLY': return RecurrenceType.weekly;
      case 'MONTHLY': return RecurrenceType.monthly;
      default: return RecurrenceType.none;
    }
  }
}

extension RecurrenceTypeApi on RecurrenceType {
  String toApiValue() {
    switch (this) {
      case RecurrenceType.daily: return 'DAILY';
      case RecurrenceType.weekly: return 'WEEKLY';
      case RecurrenceType.monthly: return 'MONTHLY';
      case RecurrenceType.none: return 'NONE';
    }
  }
}

// "Mẹ" or "Bé"
enum ReminderAssignee { mother, baby }

extension ReminderAssigneeExtension on ReminderAssignee {
  String get displayLabel => this == ReminderAssignee.mother ? 'Mẹ' : 'Bé';
}

class Reminder {
  final String id;
  final ReminderType reminderType;
  final String title;
  final DateTime scheduledAt;
  final RecurrenceType recurrenceType;
  final ReminderStatus status;
  final bool isImportant;
  final String? location;
  final String? note;
  final ReminderAssignee assignee;

  const Reminder({
    required this.id,
    required this.reminderType,
    required this.title,
    required this.scheduledAt,
    this.recurrenceType = RecurrenceType.none,
    this.status = ReminderStatus.pending,
    this.isImportant = false,
    this.location,
    this.note,
    this.assignee = ReminderAssignee.mother,
  });

  factory Reminder.fromJson(Map<String, dynamic> json) {
    return Reminder(
      id: json['id'] as String,
      reminderType: ReminderTypeExtension.fromApi(json['reminderType'] as String?),
      title: json['title'] as String,
      scheduledAt: DateTime.parse(json['scheduledAt'] as String),
      recurrenceType: RecurrenceTypeExtension.fromApi(json['recurrenceType'] as String?),
      status: ReminderStatusExtension.fromApi(json['status'] as String?),
      note: json['note'] as String?,
    );
  }
}
