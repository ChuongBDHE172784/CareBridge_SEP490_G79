import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_profile_detail_screen.dart';
import 'package:untitled/features/healthRecords/models/vaccination_model.dart';
import 'package:untitled/features/reminder/screens/create_vaccination_reminder_screen.dart';

void main() {
  testWidgets(
    'BabyProfileDetailScreen shows Circular 52/2025/TT-BYT source link in vaccination tab',
    (tester) async {
      final baby = BabyProfile(
        id: 'baby-test-1',
        nickname: 'Bé Bắp',
        birthDate: DateTime(2026, 6, 1),
        gender: BabyGender.unknown,
        isActive: true,
      );

      final schedule = VaccinationSchedule(
        babyId: baby.id,
        doses: [
          VaccinationDose(
            vaccineName: 'Lao (BCG)',
            doseNumber: 1,
            status: VaccinationStatus.scheduled,
          ),
          VaccinationDose(
            vaccineName: 'Viêm gan B',
            doseNumber: 1,
            status: VaccinationStatus.completed,
          ),
        ],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(
              child: BabyProfileDetailScreen(
                babyId: baby.id,
                embedded: true,
                loadData: false,
                loadCareCollectionsData: false,
                initialProfile: baby,
                initialVaccinations: const [],
                initialVaccinationSchedule: schedule,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Switch to vaccination tab
      await tester.tap(find.text('Tiêm chủng'));
      await tester.pumpAndSettle();

      // Verify source link is rendered with key and text
      expect(find.text('Lịch tiêm chủng'), findsOneWidget);
      expect(
        find.byKey(const Key('vaccination-schedule-source-link')),
        findsOneWidget,
      );
      expect(
        find.textContaining('Thông tư 52/2025/TT-BYT'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'CreateVaccinationReminderScreen renders Circular 52/2025/TT-BYT source link',
    (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: CreateVaccinationReminderScreen(),
          ),
        ),
      );
      await tester.pump();

      expect(
        find.byKey(const Key('vaccination-reminder-source-link')),
        findsOneWidget,
      );
      expect(
        find.textContaining('Thông tư 52/2025/TT-BYT'),
        findsOneWidget,
      );
    },
  );
}
