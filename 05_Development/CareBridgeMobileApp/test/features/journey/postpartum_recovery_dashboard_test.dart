import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/healthRecords/models/postpartum_log_model.dart';
import 'package:untitled/features/healthRecords/screens/postpartum_log_form_screen.dart';
import 'package:untitled/features/healthRecords/screens/postpartum_safety_help_screen.dart';
import 'package:untitled/features/healthRecords/services/postpartum_log_draft_store.dart';
import 'package:untitled/features/healthRecords/services/postpartum_log_service.dart';
import 'package:untitled/features/aiTriage/models/triage_entry_context.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';

void main() {
  setUp(() => FlutterSecureStorage.setMockInitialValues({}));
  tearDown(AuthState.instance.clearState);

  testWidgets('zero-baby postpartum dashboard exposes logs and safety help', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: MotherJourneyScreen(
            loadData: false,
            loadSupportingData: false,
            initialDashboard: JourneyDashboard(
              journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
              journeyType: 'POSTPARTUM',
              status: 'ACTIVE_POSTPARTUM',
              startDate: null,
              version: 1,
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.byKey(const Key('postpartum-open-logs')),
      500,
      scrollable: find.byType(Scrollable).first,
    );

    expect(find.byKey(const Key('postpartum-open-logs')), findsOneWidget);
    expect(find.byKey(const Key('postpartum-safety-help')), findsOneWidget);
    expect(find.byKey(const Key('pregnancy-outcome-entry')), findsNothing);
    expect(find.textContaining('không cần tạo hồ sơ em bé'), findsNothing);
  });

  testWidgets('ambiguous retry keeps submission id until intent changes', (
    tester,
  ) async {
    final service = _FailingPostpartumLogService();
    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumLogFormScreen(
          journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
          service: service,
        ),
      ),
    );

    await tester.drag(find.byType(ListView), const Offset(0, -900));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('postpartum-log-save')));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(const Key('postpartum-log-save')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('postpartum-log-save')));
    await tester.pumpAndSettle();

    expect(service.submissionIds, hasLength(2));
    expect(service.submissionIds[1], service.submissionIds[0]);

    await tester.drag(find.byType(ListView), const Offset(0, 900));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.widgetWithText(TextField, 'Mức đau (0–10)'),
      '3',
    );
    await tester.drag(find.byType(ListView), const Offset(0, -900));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(const Key('postpartum-log-save')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('postpartum-log-save')));
    await tester.pumpAndSettle();

    expect(service.submissionIds, hasLength(3));
    expect(service.submissionIds[2], isNot(service.submissionIds[1]));
  });

  testWidgets('late draft restore never overwrites newer form input', (
    tester,
  ) async {
    await AuthState.instance.setTokens(
      accessToken: 'test-access-token',
      refreshToken: 'test-refresh-token',
      userId: 'mother-a',
      role: 'MOTHER',
    );
    final draftStore = _DelayedPostpartumLogDraftStore();

    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumLogFormScreen(
          journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
          draftStore: draftStore,
        ),
      ),
    );
    await tester.enterText(
      find.widgetWithText(TextField, 'Mức đau (0–10)'),
      '7',
    );

    draftStore.completeRead({
      'submissionId': '10000000-0000-4000-8000-000000000064',
      'logDate': DateTime(2026, 7, 19).toIso8601String(),
      'pain': '2',
      'mood': '',
      'sleep': '',
      'symptom': '',
      'breastfeeding': '',
      'bleeding': null,
      'attempted': false,
    });
    await tester.pump();

    expect(
      tester
          .widget<TextField>(find.widgetWithText(TextField, 'Mức đau (0–10)'))
          .controller
          ?.text,
      '7',
    );
  });

  testWidgets('save before draft restore preserves retry submission id', (
    tester,
  ) async {
    await AuthState.instance.setTokens(
      accessToken: 'test-access-token',
      refreshToken: 'test-refresh-token',
      userId: 'mother-a',
      role: 'MOTHER',
    );
    final draftStore = _DelayedPostpartumLogDraftStore();
    final service = _FailingPostpartumLogService();

    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumLogFormScreen(
          journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
          service: service,
          draftStore: draftStore,
        ),
      ),
    );
    await tester.drag(find.byType(ListView), const Offset(0, -900));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('postpartum-log-save')));
    await tester.pumpAndSettle();

    draftStore.completeRead({
      'submissionId': '10000000-0000-4000-8000-000000000064',
      'logDate': DateTime(2026, 7, 19).toIso8601String(),
      'pain': '2',
      'mood': '',
      'sleep': '',
      'symptom': '',
      'breastfeeding': '',
      'bleeding': null,
      'attempted': true,
    });
    await tester.pump();
    await tester.ensureVisible(find.byKey(const Key('postpartum-log-save')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('postpartum-log-save')));
    await tester.pumpAndSettle();

    expect(service.submissionIds, hasLength(2));
    expect(service.submissionIds[1], service.submissionIds[0]);
  });

  testWidgets('postpartum safety surface is neutral and not infant intake', (
    tester,
  ) async {
    TriageEntryContext? receivedContext;
    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumSafetyHelpScreen(
          onStartAssessment: (context) => receivedContext = context,
        ),
      ),
    );

    expect(find.text('Hỗ trợ an toàn sau sinh'), findsOneWidget);
    expect(find.textContaining('em bé'), findsNothing);
    expect(
      find.byKey(const Key('postpartum-start-symptom-assessment')),
      findsOneWidget,
    );
    await tester.tap(
      find.byKey(const Key('postpartum-start-symptom-assessment')),
    );
    expect(receivedContext?.stage, TriageStageIntent.postpartum);
    expect(receivedContext?.lockStage, isTrue);
    expect(find.byKey(const Key('postpartum-call-emergency')), findsOneWidget);
  });

  testWidgets('failed emergency launch keeps manual 115 guidance visible', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumSafetyHelpScreen(emergencyCaller: () async => false),
      ),
    );

    await tester.tap(find.byKey(const Key('postpartum-call-emergency')));
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('postpartum-manual-call-guidance')),
      findsOneWidget,
    );
    expect(find.textContaining('tự gọi 115'), findsOneWidget);
  });

  testWidgets('malformed non-finite sleep is rejected before create', (
    tester,
  ) async {
    final service = _FailingPostpartumLogService();
    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumLogFormScreen(
          journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
          service: service,
        ),
      ),
    );

    await tester.enterText(
      find.widgetWithText(TextField, 'Giờ ngủ (0–24)'),
      'NaN',
    );
    await tester.drag(find.byType(ListView), const Offset(0, -900));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('postpartum-log-save')));
    await tester.pump();

    expect(find.text('Giờ ngủ phải là một số hợp lệ.'), findsOneWidget);
    expect(service.submissionIds, isEmpty);
  });

  testWidgets('late create response is discarded after account switch', (
    tester,
  ) async {
    await AuthState.instance.setTokens(
      accessToken: 'access-a',
      refreshToken: 'refresh-a',
      userId: 'mother-a',
      role: 'MOTHER',
    );
    final service = _DelayedPostpartumLogService();
    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumLogFormScreen(
          journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
          service: service,
        ),
      ),
    );
    await tester.drag(find.byType(ListView), const Offset(0, -900));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('postpartum-log-save')));
    await tester.pump();

    await AuthState.instance.setTokens(
      accessToken: 'access-b',
      refreshToken: 'refresh-b',
      userId: 'mother-b',
      role: 'MOTHER',
    );
    service.completeCreate(_redFlagLog());
    await tester.pumpAndSettle();

    expect(find.text('Thêm nhật ký'), findsOneWidget);
    expect(find.textContaining('đánh giá sớm'), findsNothing);
  });

  testWidgets('late edit response is discarded after account switch', (
    tester,
  ) async {
    await AuthState.instance.setTokens(
      accessToken: 'access-a',
      refreshToken: 'refresh-a',
      userId: 'mother-a',
      role: 'MOTHER',
    );
    final service = _DelayedPostpartumLogService();
    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumLogFormScreen(
          journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
          logId: 'cccccccc-0000-4000-8000-000000000028',
          initialLog: _redFlagLog(symptomNote: 'account-a-sensitive'),
          service: service,
        ),
      ),
    );
    await tester.drag(find.byType(ListView), const Offset(0, -900));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('postpartum-log-save')));
    await tester.pump();

    await AuthState.instance.setTokens(
      accessToken: 'access-b',
      refreshToken: 'refresh-b',
      userId: 'mother-b',
      role: 'MOTHER',
    );
    service.completeUpdate(_redFlagLog());
    await tester.pumpAndSettle();

    expect(find.text('Sửa nhật ký'), findsOneWidget);
    expect(find.text('account-a-sensitive'), findsNothing);
    expect(find.textContaining('đánh giá sớm'), findsNothing);
  });
}

class _FailingPostpartumLogService extends PostpartumLogService {
  final submissionIds = <String>[];

  @override
  Future<PostpartumLog> create(
    String journeyId,
    PostpartumLogDraft draft,
  ) async {
    submissionIds.add(draft.submissionId);
    throw const FormatException('ambiguous network failure');
  }
}

class _DelayedPostpartumLogDraftStore extends PostpartumLogDraftStore {
  final _readCompleter = Completer<Map<String, dynamic>?>();

  void completeRead(Map<String, dynamic> value) =>
      _readCompleter.complete(value);

  @override
  Future<Map<String, dynamic>?> read(String userId, String journeyId) =>
      _readCompleter.future;

  @override
  Future<void> write(
    String userId,
    String journeyId,
    Map<String, dynamic> value,
  ) async {}

  @override
  Future<void> delete(String userId, String journeyId) async {}
}

class _DelayedPostpartumLogService extends PostpartumLogService {
  final _createCompleter = Completer<PostpartumLog>();
  final _updateCompleter = Completer<PostpartumLog>();

  void completeCreate(PostpartumLog value) => _createCompleter.complete(value);
  void completeUpdate(PostpartumLog value) => _updateCompleter.complete(value);

  @override
  Future<PostpartumLog> create(String journeyId, PostpartumLogDraft draft) =>
      _createCompleter.future;

  @override
  Future<PostpartumLog> update(String logId, PostpartumLogDraft draft) =>
      _updateCompleter.future;
}

PostpartumLog _redFlagLog({String? symptomNote}) => PostpartumLog(
  id: 'cccccccc-0000-4000-8000-000000000028',
  journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
  submissionId: 'dddddddd-0000-4000-8000-000000000028',
  logDate: DateTime(2026, 7, 19),
  symptomNote: symptomNote,
  redFlagAlert: true,
);
