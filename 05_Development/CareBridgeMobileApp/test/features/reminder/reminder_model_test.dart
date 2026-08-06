import 'package:flutter_test/flutter_test.dart';

import 'package:untitled/features/reminder/models/reminder_model.dart';

void main() {
  test('terminal reminder states include completed, skipped and cancelled', () {
    expect(ReminderStatus.done.isTerminal, isTrue);
    expect(ReminderStatus.skipped.isTerminal, isTrue);
    expect(ReminderStatus.cancelled.isTerminal, isTrue);
    expect(ReminderStatus.pending.isTerminal, isFalse);
    expect(ReminderStatus.snoozed.isTerminal, isFalse);
  });

  test('vaccination reminder payload keeps source and dose context', () {
    final reminder = Reminder.fromJson({
      'id': 'rem-1',
      'reminderType': 'VACCINATION',
      'title': '6 trong 1 · mũi 2',
      'scheduledAt': '2026-08-10T02:00:00Z',
      'status': 'SNOOZED',
      'babyId': 'baby-1',
      'vaccinationRecordId': 'vac-1',
      'doseNumber': 2,
    });

    expect(reminder.reminderType, ReminderType.vaccination);
    expect(reminder.status, ReminderStatus.snoozed);
    expect(reminder.babyId, 'baby-1');
    expect(reminder.vaccinationRecordId, 'vac-1');
    expect(reminder.doseNumber, 2);
  });
}
