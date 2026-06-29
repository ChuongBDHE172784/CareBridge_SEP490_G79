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

  // TODO: Replace with GET /api/v1/reminders?date=today when endpoint available (UC-45/46/47)
  Future<List<Reminder>> listTodayReminders() async {
    final now = DateTime.now();
    return [
      Reminder(
        id: 'r-1',
        reminderType: ReminderType.appointment,
        title: 'Khám thai định kỳ',
        scheduledAt: DateTime(now.year, now.month, now.day, 9, 0),
        status: ReminderStatus.pending,
        isImportant: true,
        location: 'Bệnh viện Phụ Sản',
        assignee: ReminderAssignee.mother,
      ),
      Reminder(
        id: 'r-2',
        reminderType: ReminderType.medication,
        title: 'Uống Vitamin tổng hợp',
        scheduledAt: DateTime(now.year, now.month, now.day, 7, 30),
        status: ReminderStatus.done,
        location: 'Sau bữa sáng',
        assignee: ReminderAssignee.mother,
      ),
      Reminder(
        id: 'r-3',
        reminderType: ReminderType.vaccination,
        title: 'Tiêm phòng phế cầu',
        scheduledAt: DateTime(now.year, now.month, now.day, 14, 0),
        status: ReminderStatus.pending,
        location: 'Trạm y tế phường',
        assignee: ReminderAssignee.baby,
      ),
    ];
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

  // TODO: DELETE /api/v1/reminders/{id} when endpoint available (UC-215)
  Future<void> deleteReminder(String reminderId) async {
    await apiDelete('/api/v1/reminders/$reminderId');
  }
}
