import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

void main() {
  test(
    'successful PUT is not rethrown when optimistic cache write fails',
    () async {
      var apiCalls = 0;
      var cacheCalls = 0;
      final revisionBefore = JourneyService.dashboardRevision.value;
      final service = JourneyService(
        currentUserIdProvider: () => 'mother-610210',
        apiPutOverride: (path, body) async {
          apiCalls++;
          return {
            'data': {
              'journeyId': 'journey-610210',
              'journeyType': 'PREGNANCY',
              'estimatedDueDate': '2027-02-01',
              'version': 1,
            },
          };
        },
        dashboardCacheWriterOverride:
            (dashboard, {pendingSync = false, expectedUserId}) async {
              cacheCalls++;
              throw StateError('synthetic secure-storage failure');
            },
      );

      await expectLater(
        service.updateJourney(
          'journey-610210',
          UpdateJourneyRequest(
            journeyType: JourneyType.pregnancy,
            estimatedDueDate: '2027-02-01',
            dateSource: 'SELF_REPORTED',
            dateConfidence: 'ESTIMATED',
            changeReason: 'DATE_CORRECTION',
          ),
        ),
        completes,
      );

      expect(apiCalls, 1);
      expect(cacheCalls, 1);
      expect(JourneyService.dashboardRevision.value, revisionBefore + 1);
    },
  );

  test(
    'account change after successful PUT throws before local reconciliation',
    () async {
      var currentUserId = 'mother-610210';
      var cacheCalls = 0;
      final service = JourneyService(
        currentUserIdProvider: () => currentUserId,
        apiPutOverride: (path, body) async {
          currentUserId = 'mother-other';
          return {
            'data': {
              'journeyId': 'journey-610210',
              'journeyType': 'PREGNANCY',
              'estimatedDueDate': '2027-02-01',
              'version': 1,
            },
          };
        },
        dashboardCacheWriterOverride:
            (dashboard, {pendingSync = false, expectedUserId}) async {
              cacheCalls++;
            },
      );

      await expectLater(
        service.updateJourney(
          'journey-610210',
          UpdateJourneyRequest(
            journeyType: JourneyType.pregnancy,
            estimatedDueDate: '2027-02-01',
            changeReason: 'DATE_CORRECTION',
          ),
        ),
        throwsA(isA<StateError>()),
      );

      expect(cacheCalls, 0);
    },
  );

  test('account change during cache write blocks final PUT delivery', () async {
    var currentUserId = 'mother-610210';
    var cacheCalls = 0;
    final service = JourneyService(
      currentUserIdProvider: () => currentUserId,
      apiPutOverride: (path, body) async => {
        'data': {
          'journeyId': 'journey-610210',
          'journeyType': 'PREGNANCY',
          'estimatedDueDate': '2027-02-01',
          'version': 1,
        },
      },
      dashboardCacheWriterOverride:
          (dashboard, {pendingSync = false, expectedUserId}) async {
            cacheCalls++;
            currentUserId = 'mother-other';
          },
    );

    await expectLater(
      service.updateJourney(
        'journey-610210',
        UpdateJourneyRequest(
          journeyType: JourneyType.pregnancy,
          estimatedDueDate: '2027-02-01',
          changeReason: 'DATE_CORRECTION',
        ),
      ),
      throwsA(isA<StateError>()),
    );

    expect(cacheCalls, 1);
  });

  test(
    'outcome result is not delivered after API-time account switch',
    () async {
      var currentUserId = 'mother-610210';
      var cacheCalls = 0;
      final service = JourneyService(
        currentUserIdProvider: () => currentUserId,
        apiPostOverride: (path, body) async {
          currentUserId = 'mother-other';
          return {
            'data': {
              'evidenceId': 'evidence-610210',
              'journeyId': 'journey-610210',
              'outcomeType': 'PREGNANCY_LOSS',
              'journeyType': 'POSTPARTUM',
              'journeyVersion': 2,
              'revisionNumber': 1,
              'babyActionsEligible': false,
            },
          };
        },
        dashboardCacheWriterOverride:
            (dashboard, {pendingSync = false, expectedUserId}) async {
              cacheCalls++;
            },
      );

      await expectLater(
        service.recordPregnancyOutcome(
          'journey-610210',
          RecordPregnancyOutcomeRequest(
            submissionId: '00000000-0000-0000-0000-000000610210',
            expectedJourneyVersion: 1,
            outcomeType: PregnancyOutcome.pregnancyLoss,
            source: 'SELF_REPORTED',
            reason: 'MOTHER_OUTCOME_CONFIRMATION',
            effectiveAt: DateTime.utc(2026, 7, 26),
          ),
        ),
        throwsA(isA<StateError>()),
      );

      expect(cacheCalls, 0);
    },
  );
}
