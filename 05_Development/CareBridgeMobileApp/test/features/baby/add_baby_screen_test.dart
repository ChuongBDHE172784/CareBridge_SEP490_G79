import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/add_baby_screen.dart';
import 'package:untitled/features/baby/services/baby_service.dart';

void main() {
  testWidgets('defer is rendered only for live-birth transition entry', (
    tester,
  ) async {
    final router = GoRouter(
      initialLocation: '/babies/add',
      routes: [
        GoRoute(
          path: '/babies/add',
          builder: (_, _) => const AddBabyScreen(
            entryPoint: AddBabyEntryPoint.liveBirthTransition,
          ),
        ),
        GoRoute(
          path: '/mother-home',
          builder: (_, _) => const Scaffold(body: Text('journey')),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('add-baby-defer')), findsOneWidget);
    await tester.ensureVisible(find.byKey(const Key('add-baby-defer')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('add-baby-defer')));
    await tester.pumpAndSettle();
    expect(find.text('journey'), findsOneWidget);
  });

  testWidgets('profile-list entry does not expose transition defer', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: AddBabyScreen()));
    await tester.pump();

    expect(find.byKey(const Key('add-baby-defer')), findsNothing);
  });

  testWidgets('gender options do not contain "Chưa biết" and default to Nam', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: AddBabyScreen()));
    await tester.pumpAndSettle();

    expect(find.text('Chưa biết'), findsNothing);
    expect(find.text('Nam'), findsOneWidget);
    expect(find.text('Nữ'), findsOneWidget);
  });

  testWidgets('live-birth create is single-submit and returns to Journey', (
    tester,
  ) async {
    final completion = Completer<Map<String, dynamic>>();
    final service = _FakeBabyService((_) => completion.future);
    final router = _routerFor(service: service);
    addTearDown(router.dispose);

    await _pumpValidLiveBirthForm(tester, router);
    final submit = find.byKey(const Key('add-baby-submit'));
    await tester.tap(submit);
    await tester.tap(submit);
    await tester.pump();

    expect(service.createCalls, 1);
    completion.complete({'id': 'baby-1'});
    await tester.pumpAndSettle();
    expect(find.text('journey'), findsOneWidget);
  });

  testWidgets('birth measurements stay in the single baby-create request', (
    tester,
  ) async {
    final service = _FakeBabyService((_) async => {'id': 'baby-1'});
    final router = _routerFor(service: service);
    addTearDown(router.dispose);

    await _pumpValidLiveBirthForm(tester, router);
    await tester.ensureVisible(find.byKey(const Key('add-baby-birth-weight')));
    await tester.enterText(
      find.byKey(const Key('add-baby-birth-weight')),
      '3.5',
    );
    await tester.ensureVisible(find.byKey(const Key('add-baby-birth-length')));
    await tester.enterText(
      find.byKey(const Key('add-baby-birth-length')),
      '49',
    );
    await tester.ensureVisible(find.byKey(const Key('add-baby-submit')));
    await tester.tap(find.byKey(const Key('add-baby-submit')));
    await tester.pumpAndSettle();

    expect(service.createCalls, 1);
    expect(service.requests.single.birthWeightKg, 3.5);
    expect(service.requests.single.birthLengthCm, 49);
    expect(find.text('journey'), findsOneWidget);
  });

  testWidgets('late create response cannot navigate a different account', (
    tester,
  ) async {
    var accountId = 'account-a';
    final completion = Completer<Map<String, dynamic>>();
    final service = _FakeBabyService((_) => completion.future);
    final router = _routerFor(
      service: service,
      accountIdProvider: () => accountId,
    );
    addTearDown(router.dispose);

    await _pumpValidLiveBirthForm(tester, router);
    await tester.tap(find.byKey(const Key('add-baby-submit')));
    await tester.pump();
    accountId = 'account-b';
    completion.complete({'id': 'baby-1'});
    await tester.pumpAndSettle();

    expect(service.createCalls, 1);
    expect(service.tokens, ['token-a']);
    expect(service.expectedAccountIds, ['account-a']);
    expect(find.text('journey'), findsNothing);
    expect(find.byType(AddBabyScreen), findsOneWidget);
  });

  testWidgets('account switch before submit cannot create from stale form', (
    tester,
  ) async {
    var accountId = 'account-a';
    final service = _FakeBabyService((_) async => {'id': 'baby-1'});
    final router = _routerFor(
      service: service,
      accountIdProvider: () => accountId,
    );
    addTearDown(router.dispose);

    await _pumpValidLiveBirthForm(tester, router);
    accountId = 'account-b';
    await tester.tap(find.byKey(const Key('add-baby-submit')));
    await tester.pump();

    expect(service.createCalls, 0);
    expect(
      find.text('Phiên đăng nhập đã thay đổi. Vui lòng mở lại biểu mẫu.'),
      findsOneWidget,
    );
  });

  testWidgets('back is disabled while create is in flight', (tester) async {
    final completion = Completer<Map<String, dynamic>>();
    final service = _FakeBabyService((_) => completion.future);
    final router = _routerFor(service: service);
    addTearDown(router.dispose);

    await _pumpValidLiveBirthForm(tester, router);
    await tester.tap(find.byKey(const Key('add-baby-submit')));
    await tester.pump();

    final backButton = tester.widget<IconButton>(
      find.widgetWithIcon(IconButton, Icons.arrow_back),
    );
    expect(backButton.onPressed, isNull);
    expect(tester.widget<PopScope>(find.byType(PopScope)).canPop, isFalse);

    completion.complete({'id': 'baby-1'});
    await tester.pumpAndSettle();
    expect(find.text('journey'), findsOneWidget);
  });

  testWidgets('create failure stays on Add Baby and preserves defer', (
    tester,
  ) async {
    final service = _FakeBabyService(
      (_) async => throw ApiException(503, 'offline'),
    );
    final router = _routerFor(service: service);
    addTearDown(router.dispose);

    await _pumpValidLiveBirthForm(tester, router);
    await tester.tap(find.byKey(const Key('add-baby-submit')));
    await tester.pumpAndSettle();

    expect(find.text('journey'), findsNothing);
    expect(find.text('Tạo hồ sơ thất bại (503).'), findsOneWidget);
    expect(find.byKey(const Key('add-baby-defer')), findsOneWidget);
  });

  testWidgets('same-account 401 preserves the completed form for retry', (
    tester,
  ) async {
    final service = _FakeBabyService(
      (_) async => throw ApiException(401, '{"error":"expired"}'),
    );
    final router = _routerFor(service: service);
    addTearDown(router.dispose);

    await _pumpValidLiveBirthForm(tester, router);
    await tester.tap(find.byKey(const Key('add-baby-submit')));
    await tester.pumpAndSettle();

    expect(find.byType(AddBabyScreen), findsOneWidget);
    expect(find.text('Baby Bean'), findsOneWidget);
    expect(find.textContaining('(401)'), findsOneWidget);
    expect(find.byKey(const Key('add-baby-defer')), findsOneWidget);
    expect(service.expectedAccountIds, ['account-a']);
  });

  test('BabyService forwards the frozen token and expected account', () async {
    String? receivedToken;
    String? receivedAccountId;
    Map<String, dynamic>? receivedBody;
    final service = BabyService(
      post: (path, body, {token, expectedAccountId}) async {
        expect(path, '/api/v1/babies');
        receivedBody = body;
        receivedToken = token;
        receivedAccountId = expectedAccountId;
        return {
          'data': {'id': 'baby-1'},
        };
      },
    );

    await service.createBabyProfile(
      const CreateBabyRequest(
        nickname: 'Baby Bean',
        birthDate: '2026-07-29',
        gender: BabyGender.male,
        birthWeightKg: 3.5,
        birthLengthCm: 49,
      ),
      token: 'access-a',
      expectedAccountId: 'account-a',
    );

    expect(receivedToken, 'access-a');
    expect(receivedAccountId, 'account-a');
    expect(receivedBody, {
      'nickname': 'Baby Bean',
      'birthDate': '2026-07-29',
      'gender': 'MALE',
      'birthWeightKg': 3.5,
      'birthLengthCm': 49.0,
    });
  });
}

GoRouter _routerFor({
  required BabyService service,
  String? Function()? accountIdProvider,
  String? Function()? accessTokenProvider,
}) {
  return GoRouter(
    initialLocation: '/babies/add',
    routes: [
      GoRoute(
        path: '/babies/add',
        builder: (_, _) => AddBabyScreen(
          entryPoint: AddBabyEntryPoint.liveBirthTransition,
          service: service,
          accountIdProvider: accountIdProvider ?? () => 'account-a',
          accessTokenProvider: accessTokenProvider ?? () => 'token-a',
        ),
      ),
      GoRoute(
        path: '/mother-home',
        builder: (_, _) => const Scaffold(body: Text('journey')),
      ),
    ],
  );
}

Future<void> _pumpValidLiveBirthForm(
  WidgetTester tester,
  GoRouter router,
) async {
  await tester.binding.setSurfaceSize(const Size(900, 1800));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  await tester.pumpAndSettle();
  await tester.enterText(
    find.byKey(const Key('add-baby-nickname')),
    'Baby Bean',
  );
  await tester.tap(find.byKey(const Key('add-baby-birth-date')));
  await tester.pumpAndSettle();
  await tester.tap(find.text('OK'));
  await tester.pumpAndSettle();
  await tester.ensureVisible(find.byKey(const Key('add-baby-submit')));
  await tester.pumpAndSettle();
}

class _FakeBabyService extends BabyService {
  _FakeBabyService(this._create);

  final Future<Map<String, dynamic>> Function(CreateBabyRequest request)
  _create;
  int createCalls = 0;
  final List<CreateBabyRequest> requests = [];
  final List<String?> tokens = [];
  final List<String?> expectedAccountIds = [];

  @override
  Future<Map<String, dynamic>> createBabyProfile(
    CreateBabyRequest request, {
    String? token,
    String? expectedAccountId,
  }) {
    createCalls++;
    requests.add(request);
    tokens.add(token);
    expectedAccountIds.add(expectedAccountId);
    return _create(request);
  }
}
