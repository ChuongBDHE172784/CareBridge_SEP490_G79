import 'reminder_model.dart';

enum TodayTaskSourceType { reminder, careTask, unknown }

extension TodayTaskSourceTypeExtension on TodayTaskSourceType {
  static TodayTaskSourceType fromApi(String? value) {
    switch (value) {
      case 'REMINDER':
        return TodayTaskSourceType.reminder;
      case 'CARE_TASK':
        return TodayTaskSourceType.careTask;
      default:
        return TodayTaskSourceType.unknown;
    }
  }
}

class TodayTask {
  final String id;
  final TodayTaskSourceType sourceType;
  final ReminderType type;
  final String title;
  final DateTime scheduledAt;
  final DateTime dueAt;
  final DateTime? snoozedUntil;
  final ReminderStatus status;
  final int priority;

  const TodayTask({
    required this.id,
    required this.sourceType,
    required this.type,
    required this.title,
    required this.scheduledAt,
    required this.dueAt,
    this.snoozedUntil,
    required this.status,
    required this.priority,
  });

  bool get isReminder => sourceType == TodayTaskSourceType.reminder;
  bool get isCareTask => sourceType == TodayTaskSourceType.careTask;
  bool get isPending => status == ReminderStatus.pending;
  bool get isCompleted => status == ReminderStatus.done;
  bool get isSnoozed => status == ReminderStatus.snoozed;
  bool get isSkipped => status == ReminderStatus.skipped;

  factory TodayTask.fromJson(Map<String, dynamic> json) {
    final scheduledRaw = json['scheduledAt'] as String?;
    final dueRaw = json['dueAt'] as String?;
    final scheduledAt = DateTime.parse(scheduledRaw ?? dueRaw!);
    return TodayTask(
      id: json['id'] as String,
      sourceType: TodayTaskSourceTypeExtension.fromApi(
        json['sourceType'] as String?,
      ),
      type: ReminderTypeExtension.fromApi(
        (json['type'] ?? json['reminderType']) as String?,
      ),
      title: json['title'] as String,
      scheduledAt: scheduledAt,
      dueAt: DateTime.parse(dueRaw ?? scheduledRaw!),
      snoozedUntil: json['snoozedUntil'] == null
          ? null
          : DateTime.parse(json['snoozedUntil'] as String),
      status: ReminderStatusExtension.fromApi(json['status'] as String?),
      priority: (json['priority'] as num?)?.toInt() ?? 99,
    );
  }
}
