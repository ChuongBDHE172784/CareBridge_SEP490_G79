import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/pregnancy_outcome_screen.dart';
import 'package:untitled/features/journey/services/pregnancy_outcome_draft_store.dart';

class _MemoryDraftStore implements PregnancyOutcomeDraftStore {
  final drafts = <String, PregnancyOutcomeDraft>{};
  String key(String accountId, String journeyId) => '$accountId:$journeyId';

  @override
  Future<void> clear(String accountId, String journeyId) async {
    drafts.remove(key(accountId, journeyId));
  }

  @override
  Future<void> clearForAccount(String accountId) async {
    drafts.removeWhere((key, _) => key.startsWith('$accountId:'));
  }

  @override
  Future<PregnancyOutcomeDraft?> read(
    String accountId,
    String journeyId,
  ) async => drafts[key(accountId, journeyId)];

  @override
  Future<void> write(
    String accountId,
    String journeyId,
    PregnancyOutcomeDraft draft,
  ) async {
    drafts[key(accountId, journeyId)] = draft;
  }
}

void main() {
  test('outcome request serializes the approved contract', () {
    final request = RecordPregnancyOutcomeRequest(
      submissionId: '30000000-0000-0000-0000-000000000001',
      expectedJourneyVersion: 3,
      outcomeType: PregnancyOutcome.pregnancyLoss,
      source: 'SELF_REPORTED',
      reason: 'Outcome confirmed',
      effectiveAt: DateTime.utc(2026, 7, 19, 8),
    );

    expect(request.toJson(), {
      'submissionId': '30000000-0000-0000-0000-000000000001',
      'expectedJourneyVersion': 3,
      'outcomeType': 'PREGNANCY_LOSS',
      'source': 'SELF_REPORTED',
      'reason': 'Outcome confirmed',
      'effectiveAt': '2026-07-19T08:00:00.000Z',
      'correction': false,
    });
  });

  testWidgets('loss uses neutral copy and submits without a date or baby', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(800, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    RecordPregnancyOutcomeRequest? submitted;
    await tester.pumpWidget(
      MaterialApp(
        home: PregnancyOutcomeScreen(
          journeyId: 'journey-1',
          journeyVersion: 3,
          submitOutcome: (request) async {
            submitted = request;
            return const PregnancyOutcomeResult(
              evidenceId: 'evidence-1',
              journeyId: 'journey-1',
              outcomeType: PregnancyOutcome.pregnancyLoss,
              journeyType: 'POSTPARTUM',
              journeyVersion: 4,
              revisionNumber: 1,
            );
          },
        ),
      ),
    );

    expect(find.text('Cập nhật tình trạng thai kỳ'), findsOneWidget);
    expect(find.text('Thai kỳ đã kết thúc'), findsOneWidget);
    expect(find.text('Em bé đã chào đời'), findsOneWidget);

    await tester.tap(find.text('Thai kỳ đã kết thúc'));
    await tester.pump();
    await tester.tap(find.text('Tiếp tục'));
    await tester.pumpAndSettle();

    expect(find.text('Xác nhận cập nhật'), findsOneWidget);
    expect(
      find.textContaining('không yêu cầu tạo hồ sơ em bé'),
      findsOneWidget,
    );
    await tester.tap(find.widgetWithText(FilledButton, 'Xác nhận'));
    await tester.pumpAndSettle();

    expect(submitted?.outcomeType, PregnancyOutcome.pregnancyLoss);
    expect(submitted?.outcomeDate, isNull);
    expect(find.text('Đã cập nhật hành trình'), findsOneWidget);
  });

  testWidgets('ongoing can progress to a final outcome without correction', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(800, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    RecordPregnancyOutcomeRequest? submitted;
    await tester.pumpWidget(
      MaterialApp(
        home: PregnancyOutcomeScreen(
          journeyId: 'journey-1',
          journeyVersion: 4,
          currentOutcome: PregnancyOutcome.ongoing,
          submitOutcome: (request) async {
            submitted = request;
            return const PregnancyOutcomeResult(
              evidenceId: 'evidence-2',
              journeyId: 'journey-1',
              outcomeType: PregnancyOutcome.pregnancyLoss,
              journeyType: 'POSTPARTUM',
              journeyVersion: 5,
              revisionNumber: 2,
            );
          },
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text(PregnancyOutcome.pregnancyLoss.displayLabel));
    await tester.pump();
    await tester.tap(find.byType(FilledButton).first);
    await tester.pumpAndSettle();
    await tester.tap(find.byType(FilledButton).last);
    await tester.pumpAndSettle();

    expect(submitted?.correction, isFalse);
  });

  testWidgets('live birth requires a date before submit', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(
      MaterialApp(
        home: PregnancyOutcomeScreen(
          journeyId: 'journey-1',
          journeyVersion: 3,
          submitOutcome: (_) => throw StateError('must not submit'),
        ),
      ),
    );

    await tester.tap(find.text('Em bé đã chào đời'));
    await tester.pump();
    await tester.tap(find.text('Tiếp tục'));
    await tester.pump();

    expect(find.text('Vui lòng chọn ngày'), findsOneWidget);
  });

  testWidgets('restores only the same-account draft and clears it on success', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(800, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final store = _MemoryDraftStore();
    store.drafts['mother-a:journey-1'] = PregnancyOutcomeDraft(
      submissionId: '30000000-0000-0000-0000-000000000009',
      outcome: PregnancyOutcome.pregnancyLoss,
      effectiveAt: DateTime.utc(2026, 7, 19, 8),
    );
    store.drafts['mother-b:journey-1'] = const PregnancyOutcomeDraft(
      submissionId: '30000000-0000-0000-0000-000000000010',
      outcome: PregnancyOutcome.liveBirth,
    );

    RecordPregnancyOutcomeRequest? submitted;
    await tester.pumpWidget(
      MaterialApp(
        home: PregnancyOutcomeScreen(
          journeyId: 'journey-1',
          journeyVersion: 3,
          accountId: 'mother-a',
          draftStore: store,
          submitOutcome: (request) async {
            submitted = request;
            return const PregnancyOutcomeResult(
              evidenceId: 'evidence-1',
              journeyId: 'journey-1',
              outcomeType: PregnancyOutcome.pregnancyLoss,
              journeyType: 'POSTPARTUM',
              journeyVersion: 4,
              revisionNumber: 1,
            );
          },
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.text('Tiếp tục'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Xác nhận'));
    await tester.pumpAndSettle();

    expect(submitted?.submissionId, '30000000-0000-0000-0000-000000000009');
    expect(submitted?.effectiveAt, DateTime.utc(2026, 7, 19, 8));
    expect(store.drafts.containsKey('mother-a:journey-1'), isFalse);
    expect(store.drafts.containsKey('mother-b:journey-1'), isTrue);
  });

  testWidgets(
    'account switch during submit discards the stale outcome result',
    (tester) async {
      await tester.binding.setSurfaceSize(const Size(800, 1400));
      addTearDown(() => tester.binding.setSurfaceSize(null));
      final store = _MemoryDraftStore();
      final submitted = Completer<PregnancyOutcomeResult>();
      var sameAccount = true;
      await tester.pumpWidget(
        MaterialApp(
          home: PregnancyOutcomeScreen(
            journeyId: 'journey-1',
            journeyVersion: 3,
            accountId: 'mother-a',
            sameAccountCheck: () => sameAccount,
            draftStore: store,
            submitOutcome: (_) => submitted.future,
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text('Thai kỳ đã kết thúc'));
      await tester.pump();
      await tester.tap(find.text('Tiếp tục'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Xác nhận'));
      await tester.pump();

      sameAccount = false;
      submitted.complete(
        const PregnancyOutcomeResult(
          evidenceId: 'evidence-stale',
          journeyId: 'journey-1',
          outcomeType: PregnancyOutcome.pregnancyLoss,
          journeyType: 'POSTPARTUM',
          journeyVersion: 4,
          revisionNumber: 1,
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Đã cập nhật hành trình'), findsNothing);
      expect(
        find.text('Tài khoản đã thay đổi. Vui lòng mở lại màn hình.'),
        findsOneWidget,
      );
      expect(store.drafts.containsKey('mother-a:journey-1'), isTrue);
    },
  );

  testWidgets('keeps five semantic choices usable at 150 percent landscape', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(1000, 700));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    await tester.pumpWidget(
      MediaQuery(
        data: const MediaQueryData(textScaler: TextScaler.linear(1.5)),
        child: MaterialApp(
          home: PregnancyOutcomeScreen(
            journeyId: 'journey-1',
            journeyVersion: 3,
            accountId: 'mother-a',
            draftStore: _MemoryDraftStore(),
            submitOutcome: (_) => throw StateError('not submitted'),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    for (final outcome in PregnancyOutcome.values.where(
      (o) => o != PregnancyOutcome.ongoing,
    )) {
      final label = find.text(outcome.displayLabel);
      await tester.scrollUntilVisible(
        label,
        160,
        scrollable: find.byType(Scrollable).first,
      );
      final semanticChoice = find
          .ancestor(of: label, matching: find.byType(Semantics))
          .first;
      expect(semanticChoice, findsOneWidget);
      expect(tester.getSize(semanticChoice).height, greaterThanOrEqualTo(48));
    }
    expect(tester.takeException(), isNull);
  });
}
