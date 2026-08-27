import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/models/reminder_model.dart';
import 'package:untitled/features/reminder/screens/shared_appointment_detail_screen.dart';

Reminder _appointment() {
  return Reminder(
    id: 'appointment-1',
    reminderType: ReminderType.appointment,
    title: 'Prenatal visit',
    scheduledAt: DateTime.utc(2026, 8, 5, 2),
    notificationOffsetsMinutes: const [-60, 0],
  );
}

void main() {
  testWidgets('renders a shared appointment as read-only detail', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: SharedAppointmentDetailScreen(
          careGroupId: 'group-1',
          appointmentId: 'appointment-1',
          loader: (groupId, appointmentId) async {
            expect(groupId, 'group-1');
            expect(appointmentId, 'appointment-1');
            return _appointment();
          },
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('shared-appointment-title')), findsOneWidget);
    expect(find.text('Prenatal visit'), findsOneWidget);
    expect(find.byType(TextButton), findsNothing);
    expect(find.byIcon(Icons.edit), findsNothing);
  });

  testWidgets('shared appointment detail supports retry after an error', (
    tester,
  ) async {
    var calls = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: SharedAppointmentDetailScreen(
          careGroupId: 'group-1',
          appointmentId: 'appointment-1',
          loader: (_, _) async {
            calls++;
            if (calls == 1) throw StateError('network');
            return _appointment();
          },
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('shared-appointment-retry')), findsOneWidget);
    await tester.tap(find.byKey(const Key('shared-appointment-retry')));
    await tester.pumpAndSettle();

    expect(find.text('Prenatal visit'), findsOneWidget);
    expect(calls, 2);
  });
}
