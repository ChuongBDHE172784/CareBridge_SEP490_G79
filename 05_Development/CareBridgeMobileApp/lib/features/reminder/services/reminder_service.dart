import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/network/api_client.dart';
import '../models/reminder_model.dart';

class ReminderService extends ChangeNotifier {
  ReminderService._();
  static final instance = ReminderService._();

  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _storageKey = 'cb_task_done_state';

  final Map<String, bool> _doneOverrides = {};

  Future<void> loadState() async {
    final raw = await _storage.read(key: _storageKey);
    if (raw != null) {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      _doneOverrides
        ..clear()
        ..addAll(map.map((k, v) => MapEntry(k, v as bool)));
      notifyListeners();
    }
  }

  Future<void> _persist() async {
    await _storage.write(key: _storageKey, value: jsonEncode(_doneOverrides));
  }

  bool isDone(Reminder r) {
    if (_doneOverrides.containsKey(r.id)) return _doneOverrides[r.id]!;
    return r.status == ReminderStatus.done;
  }

  void toggleDone(Reminder r) {
    _doneOverrides[r.id] = !isDone(r);
    notifyListeners();
    _persist();
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
          .map((e) => Reminder.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (_) {
      return [];
    }
  }

  // TODO: PATCH /api/v1/reminders/{id}/status when endpoint available (UC-213/214/215)
  Future<void> markDone(String reminderId) async {
    _doneOverrides[reminderId] = true;
    notifyListeners();
    await _persist();
  }

  Future<void> snooze(String reminderId) async {
    _doneOverrides[reminderId] = false;
    notifyListeners();
    await _persist();
  }

  Future<void> skip(String reminderId) async {
    // placeholder
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
  }) async {
    final body = <String, dynamic>{
      'title': ?title,
      if (scheduledAt != null)
        'scheduledAt': scheduledAt.toUtc().toIso8601String(),
      if (recurrenceType != null) 'recurrenceType': recurrenceType.toApiValue(),
      if (recurrenceEndDate != null)
        'recurrenceEndDate': recurrenceEndDate.toUtc().toIso8601String(),
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
}
