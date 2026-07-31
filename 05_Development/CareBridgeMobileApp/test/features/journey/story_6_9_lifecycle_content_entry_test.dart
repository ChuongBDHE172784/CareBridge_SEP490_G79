import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/models/content_model.dart';
import 'package:untitled/features/community/screens/view_content_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';

Future<void> _pumpJourney(
  WidgetTester tester, {
  required String journeyType,
  required String status,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: MotherJourneyScreen(
          loadData: false,
          loadSupportingData: false,
          initialDashboard: JourneyDashboard(
            journeyId: 'synthetic-journey-$journeyType',
            journeyType: journeyType,
            status: status,
          ),
        ),
      ),
    ),
  );
  await tester.pump();
}

void main() {
  group('UC82-69-MOB-003 Mother lifecycle content entry', () {
    for (final fixture in const [
      ('PRE_PREGNANCY', 'ACTIVE_PRE_PREGNANCY'),
      ('PREGNANCY', 'ACTIVE_PREGNANCY'),
      ('POSTPARTUM', 'ACTIVE_POSTPARTUM'),
    ]) {
      testWidgets('${fixture.$1} opens reviewed lifecycle content', (
        tester,
      ) async {
        await _pumpJourney(tester, journeyType: fixture.$1, status: fixture.$2);

        final entry = find.byKey(const Key('mother-lifecycle-content-entry'));
        await tester.scrollUntilVisible(
          entry,
          500,
          scrollable: find.byType(Scrollable).first,
        );
        expect(entry, findsOneWidget);
        await Scrollable.ensureVisible(tester.element(entry), alignment: 0.5);
        await tester.pumpAndSettle();
        await tester.tap(entry);
        await tester.pumpAndSettle();

        expect(find.byType(ViewContentScreen), findsOneWidget);
        expect(
          tester.widget<ViewContentScreen>(find.byType(ViewContentScreen)).mode,
          ContentBrowseMode.lifecycle,
        );
      });
    }

    testWidgets('PRE card does not display redundant not-yet button', (
      tester,
    ) async {
      await _pumpJourney(
        tester,
        journeyType: 'PRE_PREGNANCY',
        status: 'ACTIVE_PRE_PREGNANCY',
      );

      final notYet = find.byKey(const Key('mother-pre-not-yet'));
      expect(notYet, findsNothing);
    });
  });
}
