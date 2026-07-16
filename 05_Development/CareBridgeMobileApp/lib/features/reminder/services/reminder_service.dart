import 'package:flutter/foundation.dart';
import '../../../core/network/api_client.dart';
import '../models/reminder_model.dart';
import '../models/today_task_model.dart';

class ReminderService extends ChangeNotifier {
  ReminderService._();
  static final instance = ReminderService._();

  Future<void> loadState() async {}

  bool isDone(Reminder r) {
    return r.status == ReminderStatus.done;
  }

  Future<void> toggleDone(Reminder r) async {
    if (isDone(r)) return;
    await completeReminder(r.id);
    notifyListeners();
  }

  Future<Reminder> getReminderDetail(String reminderId) async {
    final data = await apiGet('/api/v1/reminders/$reminderId');
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<List<Reminder>> listTodayReminders() async {
    try {
      final data = await apiGet('/api/v1/reminders/today');
      final list = data['data'] as List? ?? [];
      return list
          .where((e) {
            final item = e as Map<String, dynamic>;
            return item['sourceType'] == null ||
                item['sourceType'] == 'REMINDER';
          })
          .map((e) => Reminder.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (_) {
      return [];
    }
  }

  Future<List<Reminder>> listAllReminders() async {
    try {
      final data = await apiGet('/api/v1/reminders');
      final list = data['data'] as List? ?? [];
      return list
          .map((e) => Reminder.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (_) {
      return [];
    }
  }

  Future<List<TodayTask>> listTodayTasks() async {
    final data = await apiGet('/api/v1/reminders/today');
    final list = data['data'] as List? ?? [];
    return list
        .map((e) => TodayTask.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> markDone(String reminderId) async {
    await completeReminder(reminderId);
    notifyListeners();
  }

  Future<void> snooze(String reminderId) async {
    notifyListeners();
  }

  Future<void> skip(String reminderId) async {
    await skipReminder(reminderId);
    notifyListeners();
  }

  // UC-46: Create medication reminder
  Future<Reminder> createMedicationReminder({
    required String title,
    required DateTime scheduledAt,
    RecurrenceType recurrenceType = RecurrenceType.none,
    DateTime? recurrenceEndDate,
    String? journeyId,
  }) async {
    final body = <String, dynamic>{
      'title': title,
      'scheduledAt': scheduledAt.toUtc().toIso8601String(),
      'recurrenceType': recurrenceType == RecurrenceType.none
          ? null
          : recurrenceType.toApiValue(),
      if (recurrenceEndDate != null)
        'recurrenceEndDate': recurrenceEndDate.toUtc().toIso8601String(),
      'journeyId': ?journeyId,
    };
    final data = await apiPost('/api/v1/reminders/medication', body);
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-47: Create vaccination reminder
  Future<Reminder> createVaccinationReminder({
    required String babyId,
    required String title,
    required DateTime scheduledAt,
    RecurrenceType recurrenceType = RecurrenceType.none,
    DateTime? recurrenceEndDate,
    String? journeyId,
  }) async {
    final body = <String, dynamic>{
      'babyId': babyId,
      'title': title,
      'scheduledAt': scheduledAt.toUtc().toIso8601String(),
      'recurrenceType': recurrenceType == RecurrenceType.none
          ? null
          : recurrenceType.toApiValue(),
      if (recurrenceEndDate != null)
        'recurrenceEndDate': recurrenceEndDate.toUtc().toIso8601String(),
      'journeyId': ?journeyId,
    };
    final data = await apiPost('/api/v1/reminders/vaccination', body);
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-45: Create appointment reminder
  Future<Reminder> createAppointmentReminder({
    required String title,
    required DateTime scheduledAt,
    String? babyId,
    RecurrenceType recurrenceType = RecurrenceType.none,
    DateTime? recurrenceEndDate,
    String? journeyId,
  }) async {
    final body = <String, dynamic>{
      'reminderType': 'APPOINTMENT',
      'title': title,
      'scheduledAt': scheduledAt.toUtc().toIso8601String(),
      'recurrenceType': recurrenceType == RecurrenceType.none
          ? null
          : recurrenceType.toApiValue(),
      if (recurrenceEndDate != null)
        'recurrenceEndDate': recurrenceEndDate.toUtc().toIso8601String(),
      'journeyId': ?journeyId,
      'babyId': ?babyId,
    };
    final data = await apiPost('/api/v1/reminders', body);
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-47: Get vaccination suggestions for baby
  Future<List<Map<String, dynamic>>> getVaccinationSuggestions(
    String babyId,
  ) async {
    try {
      final data = await apiGet(
        '/api/v1/reminders/vaccination/suggestions?babyId=$babyId',
      );
      final list = data['data'] as List? ?? [];
      return list.cast<Map<String, dynamic>>();
    } catch (_) {
      return [];
    }
  }

  // UC-48: Update reminder
  Future<Reminder> updateReminder(
    String reminderId, {
    String? title,
    DateTime? scheduledAt,
    RecurrenceType? recurrenceType,
    DateTime? recurrenceEndDate,
    bool recurrenceEndDateSet = false,
  }) async {
    final body = <String, dynamic>{
      'title': ?title,
      if (scheduledAt != null)
        'scheduledAt': scheduledAt.toUtc().toIso8601String(),
      if (recurrenceType != null) 'recurrenceType': recurrenceType.toApiValue(),
      if (recurrenceEndDateSet) ...{
        'recurrenceEndDate': recurrenceEndDate?.toUtc().toIso8601String(),
        'recurrenceEndDateSet': true,
      },
    };
    final data = await apiPatch('/api/v1/reminders/$reminderId', body);
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-48: Snooze reminder
  Future<Reminder> snoozeReminder(
    String reminderId,
    DateTime snoozedUntil,
  ) async {
    final body = {'snoozedUntil': snoozedUntil.toUtc().toIso8601String()};
    final data = await apiPatch('/api/v1/reminders/$reminderId/snooze', body);
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-48: Complete reminder
  Future<Reminder> completeReminder(String reminderId) async {
    final data = await apiPatch('/api/v1/reminders/$reminderId/complete', {});
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-48: Skip reminder
  Future<Reminder> skipReminder(String reminderId) async {
    final data = await apiPatch('/api/v1/reminders/$reminderId/skip', {});
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-215: Delete reminder
  Future<void> deleteReminder(String reminderId) async {
    await apiDelete('/api/v1/reminders/$reminderId');
  }

  Future<Reminder> enableReminder(String reminderId) async {
    final data = await apiPatch('/api/v1/reminders/$reminderId/enable', {});
    return Reminder.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<void> hardDeleteReminder(String reminderId) async {
    await apiDelete('/api/v1/reminders/$reminderId/permanent');
  }
}
