import 'reminder_model.dart';

/// Typed appointment view model. Appointments are kept separate from alarm
/// schedules even while legacy JSON fields are still read by `Reminder`.
class Appointment extends Reminder {
  const Appointment({
    required super.id,
    required super.title,
    required super.scheduledAt,
    super.recurrenceType = RecurrenceType.none,
    super.recurrenceEndDate,
    super.status = ReminderStatus.pending,
    super.location,
    super.note,
    super.notificationOffsetsMinutes = const [],
    super.timeZone,
  }) : super(reminderType: ReminderType.appointment);

  factory Appointment.fromReminder(Reminder reminder) => Appointment(
    id: reminder.id,
    title: reminder.title,
    scheduledAt: reminder.scheduledAt,
    recurrenceType: reminder.recurrenceType,
    recurrenceEndDate: reminder.recurrenceEndDate,
    status: reminder.status,
    location: reminder.location,
    note: reminder.note,
    notificationOffsetsMinutes: reminder.notificationOffsetsMinutes,
    timeZone: reminder.timeZone,
  );
}
