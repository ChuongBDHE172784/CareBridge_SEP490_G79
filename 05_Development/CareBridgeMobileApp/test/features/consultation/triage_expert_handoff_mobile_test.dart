import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/consultation/screens/triage_expert_handoff_screen.dart';
import 'package:untitled/features/consultation/services/triage_expert_handoff_service.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';

const _intakeId = '68000000-0000-0000-0000-000000000101';
const _eligibleExpertId = '68000000-0000-0000-0000-000000000401';
const _ineligibleExpertId = '68000000-0000-0000-0000-000000000402';
const _requestId = '68000000-0000-0000-0000-000000000501';
const _clientKey = '68000000-0000-0000-0000-000000000301';
const _policy = 'YELLOW_EXPERT_CONTEXT_V1';

Map<String, dynamic> _previewEnvelope() => {
  'data': {
    'intakeSessionId': _intakeId,
    'consentPolicyVersion': _policy,
    'riskLevel': 'YELLOW',
    'stage': 'POSTPARTUM',
    'riskSummary': 'Synthetic sanitized YELLOW summary.',
    'citations': const [],
    'sharedFields': const [
      'YELLOW risk',
      'Lifecycle stage',
      'Risk summary',
      'Approved source metadata',
    ],
    'excludedFields': const [
      'Raw answers or symptoms',
      'Normalized symptoms',
      'Red flags',
      'Claims',
      'Health notes',
      'AI payload',
      'Identifiers or tokens',
      'Route or origin data',
      'Pending or unreviewed sources',
      'Surplus health data',
    ],
  },
};

Map<String, dynamic> _createEnvelope() => {
  'data': {
    'consultationRequestId': _requestId,
    'requestStatus': 'PENDING',
    'replayed': false,
    'sharedAt': '2026-07-23T00:00:00Z',
    'context': {
      'riskLevel': 'YELLOW',
      'stage': 'POSTPARTUM',
      'riskSummary': 'Synthetic sanitized YELLOW summary.',
      'citations': const [],
    },
  },
};

class _DirectoryService extends DirectChatService {
  _DirectoryService({Future<ExpertDirectoryPage>? pending})
    : _pending = pending;

  final Future<ExpertDirectoryPage>? _pending;

  @override
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) =>
      _pending ??
      Future.value(
        const ExpertDirectoryPage(
          experts: [
            ExpertDirectoryItem(
              expertProfileId: _eligibleExpertId,
              displayName: 'Synthetic eligible expert',
              specialty: 'Postpartum care',
              verificationStatus: 'APPROVED',
              isConsultationEligible: true,
            ),
            ExpertDirectoryItem(
              expertProfileId: _ineligibleExpertId,
              displayName: 'Synthetic ineligible expert',
              verificationStatus: 'PENDING',
              isConsultationEligible: false,
            ),
          ],
          currentPage: 0,
          pageSize: 20,
          totalElements: 2,
          totalPages: 1,
        ),
      );
}

Future<GoRouter> _pumpSubject(
  WidgetTester tester, {
  required TriageExpertHandoffService handoffService,
  DirectChatService? directoryService,
  Duration? directoryTimeout,
}) async {
  final router = GoRouter(
    initialLocation: '/handoff',
    routes: [
      GoRoute(
        path: '/handoff',
        builder: (_, _) => TriageExpertHandoffScreen(
          intakeSessionId: _intakeId,
          handoffService: handoffService,
          directoryService: directoryService ?? _DirectoryService(),
          clientRequestIdFactory: () => _clientKey,
          directoryTimeout: directoryTimeout ?? const Duration(seconds: 8),
        ),
      ),
      GoRoute(
        path: '/consultation-requests/:requestId',
        builder: (_, state) =>
            Scaffold(body: Text('detail:${state.pathParameters['requestId']}')),
      ),
    ],
  );
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  return router;
}

void main() {
  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.clear();
    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access-a',
      refreshToken: 'synthetic-refresh-a',
      userId: 'mother-a',
      role: 'MOTHER',
    );
  });

  tearDown(() async => AuthState.instance.clear());

  testWidgets(
    'verified eligible selection opens unchecked exact consent disclosure',
    (tester) async {
      var createCalls = 0;
      final service = TriageExpertHandoffService(
        getRequest: (_) async => _previewEnvelope(),
        postRequest: (_, _) async {
          createCalls++;
          return _createEnvelope();
        },
      );
      final router = await _pumpSubject(tester, handoffService: service);
      addTearDown(router.dispose);
      await tester.pumpAndSettle();

      expect(find.text('Synthetic eligible expert'), findsOneWidget);
      expect(find.text('Synthetic ineligible expert'), findsNothing);
      await tester.tap(
        find.byKey(const Key('triage-handoff-select-$_eligibleExpertId')),
      );
      await tester.pumpAndSettle();

      expect(
        find.byKey(const Key('triage-handoff-consent-sheet')),
        findsOneWidget,
      );
      expect(find.text('YELLOW risk'), findsOneWidget);
      expect(find.text('Raw answers or symptoms'), findsOneWidget);
      final checkbox = tester.widget<CheckboxListTile>(
        find.byKey(const Key('triage-handoff-consent-checkbox')),
      );
      expect(checkbox.value, isFalse);
      final submit = tester.widget<FilledButton>(
        find.byKey(const Key('triage-handoff-consent-submit')),
      );
      expect(submit.onPressed, isNull);
      expect(createCalls, 0);
    },
  );

  testWidgets('cancel closes consent with zero create calls', (tester) async {
    var createCalls = 0;
    final service = TriageExpertHandoffService(
      getRequest: (_) async => _previewEnvelope(),
      postRequest: (_, _) async {
        createCalls++;
        return _createEnvelope();
      },
    );
    final router = await _pumpSubject(tester, handoffService: service);
    addTearDown(router.dispose);
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(const Key('triage-handoff-select-$_eligibleExpertId')),
    );
    await tester.pumpAndSettle();

    final cancel = find.byKey(const Key('triage-handoff-consent-cancel'));
    await tester.ensureVisible(cancel);
    await tester.tap(cancel);
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('triage-handoff-consent-sheet')), findsNothing);
    expect(createCalls, 0);
    expect(find.text('Synthetic sanitized YELLOW summary.'), findsOneWidget);
  });

  testWidgets('retry keeps one stable client key and navigates after commit', (
    tester,
  ) async {
    final clientKeys = <String>[];
    var attempt = 0;
    final service = TriageExpertHandoffService(
      getRequest: (_) async => _previewEnvelope(),
      postRequest: (_, body) async {
        clientKeys.add(body['clientRequestId'] as String);
        attempt++;
        if (attempt == 1) throw TimeoutException('synthetic offline timeout');
        return _createEnvelope();
      },
    );
    final router = await _pumpSubject(tester, handoffService: service);
    addTearDown(router.dispose);
    await tester.pumpAndSettle();

    Future<void> approve() async {
      await tester.tap(
        find.byKey(const Key('triage-handoff-select-$_eligibleExpertId')),
      );
      await tester.pumpAndSettle();
      final checkbox = find.byKey(const Key('triage-handoff-consent-checkbox'));
      await tester.ensureVisible(checkbox);
      await tester.tap(checkbox);
      await tester.pump();
      final submit = find.byKey(const Key('triage-handoff-consent-submit'));
      await tester.ensureVisible(submit);
      await tester.tap(submit);
      await tester.pumpAndSettle();
    }

    await approve();
    expect(find.byKey(const Key('triage-handoff-status')), findsOneWidget);
    await approve();

    expect(clientKeys, [_clientKey, _clientKey]);
    expect(find.text('detail:$_requestId'), findsOneWidget);
  });

  testWidgets('late account-A preview is discarded after switching to B', (
    tester,
  ) async {
    final preview = Completer<dynamic>();
    final service = TriageExpertHandoffService(
      getRequest: (_) => preview.future,
    );
    final router = await _pumpSubject(tester, handoffService: service);
    addTearDown(router.dispose);
    await tester.pump();

    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access-b',
      refreshToken: 'synthetic-refresh-b',
      userId: 'mother-b',
      role: 'MOTHER',
    );
    await tester.pump();
    preview.complete(_previewEnvelope());
    await tester.pumpAndSettle();

    expect(find.text('Synthetic sanitized YELLOW summary.'), findsNothing);
    expect(find.textContaining('Phiên đăng nhập'), findsOneWidget);
    expect(find.text('Synthetic eligible expert'), findsNothing);
  });

  testWidgets('simultaneous offline failures stay in the inline retry state', (
    tester,
  ) async {
    final preview = Completer<dynamic>();
    final directory = Completer<ExpertDirectoryPage>();
    final service = TriageExpertHandoffService(
      getRequest: (_) => preview.future,
    );
    final router = await _pumpSubject(
      tester,
      handoffService: service,
      directoryService: _DirectoryService(pending: directory.future),
    );
    addTearDown(router.dispose);
    await tester.pump();

    preview.completeError(StateError('synthetic preview offline'));
    directory.completeError(StateError('synthetic directory offline'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('triage-handoff-retry')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('hanging directory times out into the inline retry state', (
    tester,
  ) async {
    final directory = Completer<ExpertDirectoryPage>();
    final service = TriageExpertHandoffService(
      getRequest: (_) async => _previewEnvelope(),
    );
    final router = await _pumpSubject(
      tester,
      handoffService: service,
      directoryService: _DirectoryService(pending: directory.future),
      directoryTimeout: const Duration(milliseconds: 10),
    );
    addTearDown(router.dispose);

    await tester.pumpAndSettle();

    expect(find.byKey(const Key('triage-handoff-retry')), findsOneWidget);
    expect(find.text('Synthetic eligible expert'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('late account-A create cannot navigate or render for B', (
    tester,
  ) async {
    final create = Completer<dynamic>();
    final service = TriageExpertHandoffService(
      getRequest: (_) async => _previewEnvelope(),
      postRequest: (_, _) => create.future,
    );
    final router = await _pumpSubject(tester, handoffService: service);
    addTearDown(router.dispose);
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(const Key('triage-handoff-select-$_eligibleExpertId')),
    );
    await tester.pumpAndSettle();
    final checkbox = find.byKey(const Key('triage-handoff-consent-checkbox'));
    await tester.ensureVisible(checkbox);
    await tester.tap(checkbox);
    await tester.pump();
    final submit = find.byKey(const Key('triage-handoff-consent-submit'));
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pump();

    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access-b',
      refreshToken: 'synthetic-refresh-b',
      userId: 'mother-b',
      role: 'MOTHER',
    );
    create.complete(_createEnvelope());
    await tester.pumpAndSettle();

    expect(find.text('detail:$_requestId'), findsNothing);
    expect(find.textContaining('Phiên đăng nhập'), findsOneWidget);
    expect(find.text('Synthetic sanitized YELLOW summary.'), findsNothing);
  });

  testWidgets('consent remains scroll-safe at 200 percent text scale', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(430, 932);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final service = TriageExpertHandoffService(
      getRequest: (_) async => _previewEnvelope(),
    );
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => TriageExpertHandoffScreen(
            intakeSessionId: _intakeId,
            handoffService: service,
            directoryService: _DirectoryService(),
            clientRequestIdFactory: () => _clientKey,
          ),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(
      MaterialApp.router(
        routerConfig: router,
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(textScaler: const TextScaler.linear(2)),
          child: child!,
        ),
      ),
    );
    await tester.pumpAndSettle();
    final select = find.byKey(
      const Key('triage-handoff-select-$_eligibleExpertId'),
    );
    await tester.scrollUntilVisible(
      select,
      300,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(select);
    await tester.pumpAndSettle();

    final submit = find.byKey(const Key('triage-handoff-consent-submit'));
    await tester.ensureVisible(submit);
    final renderBox = tester.renderObject<RenderBox>(submit);

    expect(renderBox.size.height, greaterThanOrEqualTo(48));
    expect(tester.takeException(), isNull);
  });
}
