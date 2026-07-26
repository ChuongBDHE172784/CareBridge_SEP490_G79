import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/aiTriage/models/triage_continuation.dart';
import 'package:untitled/features/aiTriage/models/triage_intake_flow_model.dart';
import 'package:untitled/features/aiTriage/models/triage_entry_context.dart';
import 'package:untitled/features/aiTriage/models/triage_result_model.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_restore_coordinator.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_store.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';

const _sessionId = '11111111-1111-1111-1111-111111111111';

IntakeFlowResponse _complete(TriageResult result) => IntakeFlowResponse(
  status: 'TRIAGE_COMPLETE',
  intakeSessionId: _sessionId,
  stage: result.stage,
  mergedIntake: {'persisted': true, 'stage': result.stage},
  round: 2,
  triageResult: result,
);

TriageResult _result(
  String risk, {
  bool emergency = false,
  String stage = 'INFANT',
  List<TriageCitation> citations = const [],
}) => TriageResult(
  sessionId: _sessionId,
  stage: stage,
  status: 'COMPLETED',
  riskLevel: risk,
  emergencyActionRequired: emergency,
  summary: 'Ket qua $risk',
  recommendedAction: 'Theo doi huong dan',
  citations: citations,
  disclaimer: 'Khong thay the chan doan y khoa',
);

class _StaticTriageService extends TriageService {
  _StaticTriageService(this.response);
  final IntakeFlowResponse response;

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async => response;
}

class _RetryContinuationStore implements TriageContinuationStore {
  _RetryContinuationStore(this.pending);

  PendingTriageContinuation? pending;

  @override
  int generationFor(String userId) => 0;

  @override
  Future<PendingTriageContinuation?> read(String userId) async => pending;

  @override
  Future<void> invalidateUser(String userId) async => pending = null;

  @override
  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  }) async => pending = continuation;
}

class _OfflineContinuationGateway implements TriageContinuationGateway {
  @override
  Future<void> acknowledge(String token) async {}

  @override
  Future<TriageContinuationResolution> resolve(String token) =>
      Future.error(StateError('offline'));
}

class _RecordingPostpartumTriageService extends TriageService {
  Map<String, dynamic>? receivedIntake;

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async {
    receivedIntake = Map<String, dynamic>.from(currentIntake);
    return const IntakeFlowResponse(
      status: 'ASK_MORE',
      intakeSessionId: _sessionId,
      stage: 'POSTPARTUM',
      mergedIntake: {'stage': 'POSTPARTUM'},
      assistantMessage: 'Cần thêm thông tin về quá trình hồi phục.',
      questions: [
        IntakeQuestion(
          questionKey: 'duration',
          text: 'Triệu chứng đã xuất hiện bao lâu?',
          answerType: 'TEXT',
        ),
      ],
      round: 2,
    );
  }
}

class _MismatchedInfantTriageService extends TriageService {
  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async => const IntakeFlowResponse(
    status: 'ASK_MORE',
    intakeSessionId: _sessionId,
    stage: 'INFANT',
    mergedIntake: {'stage': 'INFANT', 'childAgeMonths': 2},
    questions: [
      IntakeQuestion(
        questionKey: 'childAgeMonths',
        text: 'Bé bao nhiêu tháng?',
        answerType: 'NUMBER',
      ),
    ],
    round: 2,
  );
}

class _AskMoreTriageService extends TriageService {
  String? continuedSessionId;
  int? continuedRound;
  Map<String, dynamic>? continuedIntake;
  Map<String, dynamic>? continuedAnswers;

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async => const IntakeFlowResponse(
    status: 'ASK_MORE',
    intakeSessionId: _sessionId,
    mergedIntake: {'serverState': 'kept'},
    assistantMessage: 'Cần thêm tuổi của bé',
    questions: [
      IntakeQuestion(
        questionKey: 'childAgeMonths',
        text: 'Bé bao nhiêu tháng?',
        answerType: 'SINGLE_CHOICE',
        options: ['24'],
      ),
    ],
    round: 3,
  );

  @override
  Future<IntakeFlowResponse> continueConversation({
    required String intakeSessionId,
    required Map<String, dynamic> currentIntake,
    required Map<String, dynamic> newAnswers,
    required int round,
  }) async {
    continuedSessionId = intakeSessionId;
    continuedRound = round;
    continuedIntake = currentIntake;
    continuedAnswers = newAnswers;
    return _complete(_result('GREEN'));
  }
}

class _ThrowingTriageService extends TriageService {
  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) => Future.error(Exception('RAW_SECRET_BACKEND_DETAIL'));
}

class _PendingTriageService extends TriageService {
  final completer = Completer<IntakeFlowResponse>();

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) => completer.future;
}

class _DelayedEmergencyService extends EmergencyService {
  int activeCalls = 0;
  int openCalls = 0;
  final completer = Completer<EmergencySession?>();

  @override
  Future<EmergencySession?> getActive() {
    activeCalls++;
    return completer.future;
  }

  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) {
    openCalls++;
    return Future.error(StateError('RED handoff must not POST a session'));
  }
}

class _FailingEmergencyService extends EmergencyService {
  int activeCalls = 0;
  int openCalls = 0;

  @override
  Future<EmergencySession?> getActive() {
    activeCalls++;
    return Future.error(Exception('RAW_EMERGENCY_FAILURE'));
  }

  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) {
    openCalls++;
    return Future.error(Exception('RAW_EMERGENCY_FAILURE'));
  }
}

Future<GoRouter> _pumpScreen(
  WidgetTester tester, {
  required TriageService triage,
  EmergencyService? emergency,
  TriageEntryContext entryContext = const TriageEntryContext(),
  Future<bool> Function()? postpartumEmergencyLauncher,
  TriageContinuationRestoreCoordinator? continuationCoordinator,
}) async {
  final router = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (_, _) => SymptomIntakeScreen(
          triageService: triage,
          emergencyService: emergency,
          entryContext: entryContext,
          postpartumEmergencyLauncher: postpartumEmergencyLauncher,
          continuationCoordinator: continuationCoordinator,
        ),
      ),
      GoRoute(
        path: '/emergency/map',
        builder: (_, state) {
          final session = state.extra as EmergencySession?;
          return Scaffold(
            body: Text(
              'Emergency map ${session?.sessionId ?? 'manual'} '
              '${state.uri.queryParameters['mode']} '
              '${state.uri.queryParameters['stage']}',
            ),
          );
        },
      ),
    ],
  );
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  return router;
}

Future<void> _submitInitial(WidgetTester tester) async {
  await tester.enterText(find.byType(TextField).first, 'be kho tho');
  await tester.tap(find.byIcon(Icons.send));
  await tester.pump();
}

void main() {
  testWidgets(
    'typed postpartum entry is neutral and sends POSTPARTUM without infant defaults',
    (tester) async {
      final triage = _RecordingPostpartumTriageService();
      await tester.pumpWidget(
        MaterialApp(
          home: SymptomIntakeScreen(
            triageService: triage,
            entryContext: const TriageEntryContext.postpartum(),
          ),
        ),
      );

      expect(find.textContaining('sau sinh'), findsWidgets);
      expect(find.textContaining('Bé 0-12 tháng'), findsNothing);
      expect(find.textContaining('triệu chứng của bé'), findsNothing);

      await tester.enterText(
        find.byType(TextField).first,
        'Tôi thấy chóng mặt',
      );
      await tester.tap(find.byIcon(Icons.send));
      await tester.pumpAndSettle();

      expect(triage.receivedIntake?['stage'], 'POSTPARTUM');
      expect(triage.receivedIntake?.containsKey('childAgeMonths'), isFalse);
      expect(triage.receivedIntake?.containsKey('feedingStatus'), isFalse);
      expect(find.byKey(const Key('triage-emergency-cta')), findsNothing);
    },
  );

  testWidgets('postpartum entry fails closed on an infant response', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: SymptomIntakeScreen(
          triageService: _MismatchedInfantTriageService(),
          entryContext: TriageEntryContext.postpartum(),
        ),
      ),
    );

    await tester.enterText(find.byType(TextField).first, 'Tôi thấy chóng mặt');
    await tester.tap(find.byIcon(Icons.send));
    await tester.pumpAndSettle();

    expect(find.textContaining('không khớp với giai đoạn sau sinh'), findsOne);
    expect(find.text('Bé bao nhiêu tháng?'), findsNothing);
  });

  testWidgets(
    'postpartum RED opens the backend session without posting another session',
    (tester) async {
      final emergency = _DelayedEmergencyService();
      await _pumpScreen(
        tester,
        triage: _StaticTriageService(
          IntakeFlowResponse(
            status: 'TRIAGE_COMPLETE',
            intakeSessionId: _sessionId,
            stage: 'POSTPARTUM',
            mergedIntake: const {'stage': 'POSTPARTUM'},
            round: 1,
            triageResult: _result('RED', emergency: true, stage: 'POSTPARTUM'),
          ),
        ),
        emergency: emergency,
        entryContext: const TriageEntryContext.postpartum(),
      );

      await _submitInitial(tester);
      expect(
        find.byKey(const Key('triage-postpartum-call-115')),
        findsOneWidget,
      );
      await tester.tap(find.byKey(const Key('triage-emergency-cta')));
      await tester.pump();

      expect(emergency.activeCalls, 1);
      expect(emergency.openCalls, 0);
      emergency.completer.complete(
        const EmergencySession(
          sessionId: 'postpartum-emergency',
          userId: 'mother',
          status: 'ACTIVE',
          triggerSource: 'AI_TRIAGE',
        ),
      );
      await tester.pumpAndSettle();
      expect(
        find.text('Emergency map postpartum-emergency triage POSTPARTUM'),
        findsOneWidget,
      );
      expect(find.textContaining('bé'), findsNothing);
    },
  );

  testWidgets('postpartum RED keeps manual 115 guidance when launch throws', (
    tester,
  ) async {
    final emergency = _FailingEmergencyService();
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(
        IntakeFlowResponse(
          status: 'TRIAGE_COMPLETE',
          intakeSessionId: _sessionId,
          stage: 'POSTPARTUM',
          mergedIntake: const {'stage': 'POSTPARTUM'},
          round: 1,
          triageResult: _result('RED', emergency: true, stage: 'POSTPARTUM'),
        ),
      ),
      emergency: emergency,
      entryContext: const TriageEntryContext.postpartum(),
      postpartumEmergencyLauncher: () async =>
          throw StateError('dialer unavailable'),
    );

    await _submitInitial(tester);
    await tester.tap(find.byKey(const Key('triage-emergency-cta')));
    await tester.pumpAndSettle();

    await tester.drag(find.byType(ListView), const Offset(0, -600));
    await tester.pump();
    expect(find.textContaining('Phiên có thể'), findsOneWidget);
    await tester.tap(find.byKey(const Key('triage-postpartum-call-115')));
    await tester.pumpAndSettle();
    expect(
      find.byKey(const Key('triage-postpartum-manual-call-guidance')),
      findsOneWidget,
    );
    expect(find.textContaining('tự gọi 115'), findsOneWidget);
    expect(emergency.activeCalls, 1);
    expect(emergency.openCalls, 0);
    expect(find.textContaining('Phiên có thể'), findsOneWidget);
  });

  testWidgets('postpartum 115 action is immediate and skips backend lookup', (
    tester,
  ) async {
    final emergency = _DelayedEmergencyService();
    var dialerCalls = 0;
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(
        IntakeFlowResponse(
          status: 'TRIAGE_COMPLETE',
          intakeSessionId: _sessionId,
          stage: 'POSTPARTUM',
          mergedIntake: const {'stage': 'POSTPARTUM'},
          round: 1,
          triageResult: _result('RED', emergency: true, stage: 'POSTPARTUM'),
        ),
      ),
      emergency: emergency,
      entryContext: const TriageEntryContext.postpartum(),
      postpartumEmergencyLauncher: () async {
        dialerCalls++;
        return true;
      },
    );

    await _submitInitial(tester);
    await tester.tap(find.byKey(const Key('triage-postpartum-call-115')));
    await tester.pump();

    expect(dialerCalls, 1);
    expect(emergency.activeCalls, 0);
    expect(emergency.openCalls, 0);
  });

  testWidgets('ASK_MORE continues the same server-owned conversation', (
    tester,
  ) async {
    final triage = _AskMoreTriageService();
    await _pumpScreen(tester, triage: triage);
    await _submitInitial(tester);
    await tester.pumpAndSettle();

    expect(find.text('Bé bao nhiêu tháng?'), findsWidgets);
    await tester.tap(find.text('24'));
    await tester.tap(find.text('Gửi câu trả lời'));
    await tester.pumpAndSettle();

    expect(triage.continuedSessionId, _sessionId);
    expect(triage.continuedRound, 3);
    expect(triage.continuedIntake, {'serverState': 'kept'});
    expect(triage.continuedAnswers, {'childAgeMonths': '24'});
    expect(find.text('Mức rủi ro: GREEN'), findsOneWidget);
  });

  testWidgets('RED inline result ignores duplicate emergency tap', (
    tester,
  ) async {
    final emergency = _DelayedEmergencyService();
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(_complete(_result('RED', emergency: true))),
      emergency: emergency,
    );
    await _submitInitial(tester);

    final cta = find.byKey(const Key('triage-emergency-cta'));
    expect(cta, findsOneWidget);
    await tester.tap(cta);
    await tester.tap(cta);
    expect(emergency.activeCalls, 1);
    expect(emergency.openCalls, 0);

    emergency.completer.complete(
      const EmergencySession(
        sessionId: 'e1',
        userId: 'u1',
        status: 'ACTIVE',
        triggerSource: 'AI_TRIAGE',
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Emergency map e1 triage INFANT'), findsOneWidget);
  });

  testWidgets(
    'infant emergency GET discards a late previous-account response',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      final emergency = _DelayedEmergencyService();
      final router = await _pumpScreen(
        tester,
        triage: _StaticTriageService(
          _complete(_result('RED', emergency: true)),
        ),
        emergency: emergency,
      );
      addTearDown(router.dispose);
      await _submitInitial(tester);

      final action = find.byKey(const Key('triage-emergency-cta'));
      await tester.tap(action);
      await tester.pump();
      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      emergency.completer.complete(
        const EmergencySession(
          sessionId: 'account-a-infant-emergency',
          userId: 'account-a',
          status: 'ACTIVE',
          triggerSource: 'AI_TRIAGE',
        ),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('Emergency map'), findsNothing);
      await tester.drag(find.byType(ListView), const Offset(0, -600));
      await tester.pump();
      expect(
        find.textContaining('Phiên đăng nhập đã thay đổi'),
        findsOneWidget,
      );
      expect(tester.widget<ElevatedButton>(action).onPressed, isNotNull);
    },
  );

  testWidgets(
    'postpartum emergency GET discards a late previous-account response',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      final emergency = _DelayedEmergencyService();
      final router = await _pumpScreen(
        tester,
        triage: _StaticTriageService(
          IntakeFlowResponse(
            status: 'TRIAGE_COMPLETE',
            intakeSessionId: _sessionId,
            stage: 'POSTPARTUM',
            mergedIntake: const {'stage': 'POSTPARTUM'},
            round: 1,
            triageResult: _result('RED', emergency: true, stage: 'POSTPARTUM'),
          ),
        ),
        emergency: emergency,
        entryContext: const TriageEntryContext.postpartum(),
      );
      addTearDown(router.dispose);
      await _submitInitial(tester);

      final action = find.byKey(const Key('triage-emergency-cta'));
      await tester.tap(action);
      await tester.pump();
      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      emergency.completer.complete(
        const EmergencySession(
          sessionId: 'account-a-postpartum-emergency',
          userId: 'account-a',
          status: 'ACTIVE',
          triggerSource: 'AI_TRIAGE',
        ),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('Emergency map'), findsNothing);
      await tester.drag(find.byType(ListView), const Offset(0, -600));
      await tester.pump();
      expect(
        find.textContaining('Phiên đăng nhập đã thay đổi'),
        findsOneWidget,
      );
      expect(tester.widget<ElevatedButton>(action).onPressed, isNotNull);
    },
  );

  testWidgets(
    'failed RED emergency request has no false success and offers safe map fallback',
    (tester) async {
      final emergency = _FailingEmergencyService();
      await _pumpScreen(
        tester,
        triage: _StaticTriageService(
          _complete(_result('RED', emergency: true)),
        ),
        emergency: emergency,
      );
      await _submitInitial(tester);
      await tester.tap(find.byKey(const Key('triage-emergency-cta')));
      await tester.pumpAndSettle();

      await tester.drag(find.byType(ListView), const Offset(0, -600));
      await tester.pump();
      expect(find.textContaining('Không thể tải phiên hỗ trợ'), findsOneWidget);
      expect(find.textContaining('RAW_EMERGENCY_FAILURE'), findsNothing);
      expect(find.textContaining('Da kich hoat'), findsNothing);
      expect(emergency.activeCalls, 1);
      expect(emergency.openCalls, 0);
      final fallback = find.byKey(const Key('triage-emergency-fallback-map'));
      expect(fallback, findsOneWidget);
      tester.widget<TextButton>(fallback).onPressed!();
      await tester.pumpAndSettle();
      expect(find.text('Emergency map manual triage INFANT'), findsOneWidget);
    },
  );

  testWidgets('only whitelisted HTTPS citations are interactive', (
    tester,
  ) async {
    const allowed = TriageCitation(
      id: 'allowed',
      title: 'WHO guidance',
      source: 'WHO',
      domain: 'who.int',
      url: 'https://www.who.int/example',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
    );
    const insecure = TriageCitation(
      id: 'insecure',
      title: 'Insecure',
      source: 'WHO',
      domain: 'who.int',
      url: 'http://who.int/example',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
    );
    const lookalike = TriageCitation(
      id: 'lookalike',
      title: 'Lookalike',
      source: 'Unknown',
      domain: 'who.int',
      url: 'https://who.int.attacker.example/e',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
    );
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(
        _complete(
          _result('GREEN', citations: const [allowed, insecure, lookalike]),
        ),
      ),
    );
    await _submitInitial(tester);

    final allowedInk = tester.widget<InkWell>(
      find.byKey(const Key('triage-citation-allowed-0')),
    );
    final insecureInk = tester.widget<InkWell>(
      find.byKey(const Key('triage-citation-insecure-1')),
    );
    final lookalikeInk = tester.widget<InkWell>(
      find.byKey(const Key('triage-citation-lookalike-2')),
    );
    expect(allowedInk.onTap, isNotNull);
    expect(insecureInk.onTap, isNull);
    expect(lookalikeInk.onTap, isNull);
    expect(find.text('Trạng thái: REVIEWED'), findsNothing);
  });

  testWidgets('duplicate citation URLs render with unique widget keys', (
    tester,
  ) async {
    const first = TriageCitation(
      title: 'MOH guidance one',
      source: 'MOH',
      domain: 'moh.gov.vn',
      url: 'https://moh.gov.vn/evidence-one',
      excerpt: 'Evidence one',
      retrievedAt: '2026-07-13',
    );
    const second = TriageCitation(
      title: 'MOH guidance two',
      source: 'MOH',
      domain: 'moh.gov.vn',
      url: 'https://moh.gov.vn/evidence-two',
      excerpt: 'Evidence two',
      retrievedAt: '2026-07-13',
    );
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(
        _complete(_result('YELLOW', citations: const [first, second])),
      ),
    );

    await _submitInitial(tester);

    expect(
      find.byKey(
        const Key('triage-citation-https://moh.gov.vn/evidence-one-0'),
      ),
      findsOneWidget,
    );
    expect(
      find.byKey(
        const Key('triage-citation-https://moh.gov.vn/evidence-two-1'),
      ),
      findsOneWidget,
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('PENDING_REVIEW citation displays governance label', (
    tester,
  ) async {
    const citation = TriageCitation(
      id: 'pending',
      title: 'Realtime official source',
      source: 'MOH',
      url: 'https://moh.gov.vn/example',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
      sourceStatus: 'PENDING_REVIEW',
    );
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(
        _complete(_result('YELLOW', citations: const [citation])),
      ),
    );
    await _submitInitial(tester);

    expect(find.text('Trạng thái: PENDING_REVIEW'), findsNothing);
    expect(find.textContaining('đang chờ kiểm duyệt nội bộ'), findsOneWidget);
  });

  testWidgets('raw triage exception is never rendered', (tester) async {
    await _pumpScreen(tester, triage: _ThrowingTriageService());
    await _submitInitial(tester);
    await tester.pumpAndSettle();

    expect(find.textContaining('Không thể gửi triệu chứng'), findsOneWidget);
    expect(find.textContaining('RAW_SECRET_BACKEND_DETAIL'), findsNothing);
  });

  for (final risk in ['GREEN', 'YELLOW']) {
    testWidgets('$risk result preserves content without emergency CTA', (
      tester,
    ) async {
      await _pumpScreen(
        tester,
        triage: _StaticTriageService(_complete(_result(risk))),
      );
      await _submitInitial(tester);

      expect(find.text('Mức rủi ro: $risk'), findsOneWidget);
      expect(find.text('Theo doi huong dan'), findsOneWidget);
      expect(find.text('Khong thay the chan doan y khoa'), findsOneWidget);
      expect(find.byKey(const Key('triage-emergency-cta')), findsNothing);
    });
  }

  testWidgets(
    'offline continuation resolve keeps completed intake visible and retryable',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      const token = 'opaque-continuation';
      final store = _RetryContinuationStore(
        PendingTriageContinuation(
          token: token,
          intakeSessionId: _sessionId,
          expiresAt: DateTime.utc(2026, 7, 30),
        ),
      );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: _OfflineContinuationGateway(),
      );
      final router = await _pumpScreen(
        tester,
        triage: _StaticTriageService(
          _complete(_result('GREEN', stage: 'PREGNANCY')),
        ),
        entryContext: const TriageEntryContext.locked(
          stage: TriageStageIntent.pregnancy,
          origin: TriageOriginIntent.motherJourney,
          journeyId: 'journey-a',
          originReferenceId: 'journey-a',
        ),
        continuationCoordinator: coordinator,
      );
      addTearDown(router.dispose);
      await _submitInitial(tester);
      await tester.pumpAndSettle();

      final returnAction = find.byKey(
        const Key('triage-inline-return-to-origin'),
      );
      await tester.ensureVisible(returnAction);
      await tester.tap(returnAction);
      await tester.pumpAndSettle();

      expect(find.text('Ket qua GREEN'), findsWidgets);
      expect(find.textContaining('thử lại'), findsWidgets);
      final retry = tester.widget<FilledButton>(
        find.byKey(const Key('triage-inline-return-to-origin')),
      );
      expect(retry.onPressed, isNotNull);
      expect(store.pending?.token, token);
    },
  );

  testWidgets('pending response is ignored after intake screen is disposed', (
    tester,
  ) async {
    final triage = _PendingTriageService();
    final router = await _pumpScreen(tester, triage: triage);
    await _submitInitial(tester);
    await tester.pumpWidget(const MaterialApp(home: SizedBox.shrink()));

    triage.completer.complete(_complete(_result('GREEN')));
    await tester.pump();

    expect(tester.takeException(), isNull);
    router.dispose();
  });

  test(
    'postpartum flow preserves missing nested stage and rejects mismatch',
    () {
      final inherited = IntakeFlowResponse.fromJson({
        'status': 'TRIAGE_COMPLETE',
        'intakeSessionId': _sessionId,
        'stage': 'POSTPARTUM',
        'mergedIntake': {'stage': 'POSTPARTUM'},
        'round': 1,
        'triageResult': {'riskLevel': 'RED', 'emergencyActionRequired': true},
      });

      expect(inherited.triageResult?.stage, 'POSTPARTUM');
      expect(
        () => IntakeFlowResponse.fromJson({
          'status': 'TRIAGE_COMPLETE',
          'intakeSessionId': _sessionId,
          'stage': 'POSTPARTUM',
          'mergedIntake': {'stage': 'POSTPARTUM'},
          'round': 1,
          'triageResult': {'stage': 'INFANT', 'riskLevel': 'RED'},
        }),
        throwsFormatException,
      );
      expect(
        () => IntakeFlowResponse.fromJson({
          'status': 'ASK_MORE',
          'intakeSessionId': _sessionId,
          'stage': 'POSTPARTUM',
          'mergedIntake': {'stage': 'INFANT'},
          'round': 1,
          'questions': const [],
        }),
        throwsFormatException,
      );
    },
  );

  test('flow-level lifecycle identity overrides untrusted nested metadata', () {
    final response = IntakeFlowResponse.fromJson({
      'status': 'TRIAGE_COMPLETE',
      'intakeSessionId': _sessionId,
      'stage': 'POSTPARTUM',
      'mergedIntake': {'stage': 'POSTPARTUM'},
      'round': 1,
      'journeyId': 'journey-authoritative',
      'originDashboard': 'POSTPARTUM_DASHBOARD',
      'originReferenceId': 'origin-authoritative',
      'continuationToken': 'continuation-authoritative',
      'continuationExpiresAt': '2026-07-29T12:00:00Z',
      'triageResult': {
        'status': 'FAILED',
        'triageStatus': 'FAILED',
        'riskLevel': 'RED',
        'continuationToken': 'continuation-untrusted',
        'continuationExpiresAt': '2026-07-24T12:00:00Z',
      },
    });

    expect(response.triageResult?.sessionId, _sessionId);
    expect(response.triageResult?.status, 'COMPLETED');
    expect(response.triageResult?.triageStatus, 'TRIAGE_COMPLETE');
    expect(response.triageResult?.stage, 'POSTPARTUM');
    expect(response.triageResult?.journeyId, 'journey-authoritative');
    expect(response.triageResult?.originDashboard, 'POSTPARTUM_DASHBOARD');
    expect(response.triageResult?.originReferenceId, 'origin-authoritative');
    expect(
      response.triageResult?.continuationToken,
      'continuation-authoritative',
    );
    expect(
      response.triageResult?.continuationExpiresAt,
      DateTime.parse('2026-07-29T12:00:00Z'),
    );
  });

  test('flow rejects mismatched nested lifecycle identity and origin', () {
    Map<String, dynamic> payload(Map<String, dynamic> nested) => {
      'status': 'TRIAGE_COMPLETE',
      'intakeSessionId': _sessionId,
      'stage': 'POSTPARTUM',
      'mergedIntake': {'stage': 'POSTPARTUM'},
      'round': 1,
      'journeyId': 'journey-authoritative',
      'originDashboard': 'POSTPARTUM_DASHBOARD',
      'originReferenceId': 'origin-authoritative',
      'triageResult': {'riskLevel': 'RED', ...nested},
    };

    for (final nested in const [
      {'sessionId': 'different-session'},
      {'journeyId': 'different-journey'},
      {'originDashboard': 'PREGNANCY_DASHBOARD'},
      {'originReferenceId': 'different-origin'},
    ]) {
      expect(
        () => IntakeFlowResponse.fromJson(payload(nested)),
        throwsFormatException,
      );
    }
  });

  testWidgets('applyResponse rejects a nested triage stage mismatch', (
    tester,
  ) async {
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(
        IntakeFlowResponse(
          status: 'TRIAGE_COMPLETE',
          intakeSessionId: _sessionId,
          stage: 'POSTPARTUM',
          mergedIntake: const {'stage': 'POSTPARTUM'},
          round: 1,
          triageResult: _result('RED', emergency: true, stage: 'INFANT'),
        ),
      ),
      entryContext: const TriageEntryContext.postpartum(),
    );

    await _submitInitial(tester);
    await tester.pumpAndSettle();

    expect(find.textContaining('không khớp giai đoạn'), findsOneWidget);
    expect(find.byKey(const Key('triage-emergency-cta')), findsNothing);
  });

  test('legacy result and citation payloads receive safe defaults', () {
    final result = TriageResult.fromJson({
      'sessionId': 'legacy-session',
      'status': 'COMPLETED',
      'riskLevel': 'GREEN',
    });
    final citation = TriageCitation.fromJson({
      'id': 'WHO_LEGACY',
      'title': 'WHO',
      'source': 'WHO',
      'url': 'https://who.int/example',
      'excerpt': 'evidence',
    });

    expect(result.sessionId, 'legacy-session');
    expect(result.status, 'COMPLETED');
    expect(result.emergencyActionRequired, isFalse);
    expect(citation.id, 'WHO_LEGACY');
    expect(citation.sourceVersion, isNull);
    expect(citation.retrievalMode, 'LOCAL');
    expect(citation.matchedRules, isEmpty);
  });
}
