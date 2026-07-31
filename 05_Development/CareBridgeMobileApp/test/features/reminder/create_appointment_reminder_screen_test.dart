import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/screens/create_appointment_reminder_screen.dart';

void main() {
  testWidgets('prefills the date selected from the appointment calendar', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: CreateAppointmentReminderScreen(
          initialDate: DateTime.now().add(const Duration(days: 10)),
        ),
      ),
    );

    final selected = DateTime.now().add(const Duration(days: 10));
    final expected =
        '${selected.day.toString().padLeft(2, '0')}/${selected.month.toString().padLeft(2, '0')}/${selected.year}';
    expect(find.text(expected), findsOneWidget);
  });

  testWidgets('falls back to today when the requested date is in the past', (
    tester,
  ) async {
    final now = DateTime.now();
    await tester.pumpWidget(
      MaterialApp(
        home: CreateAppointmentReminderScreen(
          initialDate: now.subtract(const Duration(days: 30)),
        ),
      ),
    );

    final expected =
        '${now.day.toString().padLeft(2, '0')}/${now.month.toString().padLeft(2, '0')}/${now.year}';
    expect(find.text(expected), findsOneWidget);
  });
}
