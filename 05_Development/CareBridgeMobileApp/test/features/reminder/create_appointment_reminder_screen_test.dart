import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/screens/create_appointment_reminder_screen.dart';

String _ddMMyyyy(DateTime value) =>
    '${value.day.toString().padLeft(2, '0')}/'
    '${value.month.toString().padLeft(2, '0')}/'
    '${value.year}';

void main() {
  // The appointment form no longer renders a start-date field: the legacy block is switched off
  // and the day comes from the calendar the user tapped, so the screen only asks for a time.
  // These two cases pin that contract — the date the caller passes must never leak back into the
  // form as an editable legacy field, and a past date must not blow the screen up.
  testWidgets('accepts a date selected from the appointment calendar without a legacy date field', (
    tester,
  ) async {
    final selected = DateTime.now().add(const Duration(days: 10));
    await tester.pumpWidget(
      MaterialApp(home: CreateAppointmentReminderScreen(initialDate: selected)),
    );

    expect(tester.takeException(), isNull);
    expect(find.text('Ngày bắt đầu'), findsNothing);
    expect(find.text(_ddMMyyyy(selected)), findsNothing);
    expect(find.byKey(const Key('appointment-time-picker')), findsOneWidget);
  });

  testWidgets('renders normally when the requested date is in the past', (
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

    expect(tester.takeException(), isNull);
    expect(find.text('Ngày bắt đầu'), findsNothing);
    expect(find.byKey(const Key('appointment-time-picker')), findsOneWidget);
  });

  testWidgets('offers a single appointment time picker', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: CreateAppointmentReminderScreen()),
    );

    expect(find.text('Giờ hẹn'), findsOneWidget);
    expect(find.byKey(const Key('appointment-time-picker')), findsOneWidget);
    expect(find.text('Chọn giờ'), findsOneWidget);
  });
}
