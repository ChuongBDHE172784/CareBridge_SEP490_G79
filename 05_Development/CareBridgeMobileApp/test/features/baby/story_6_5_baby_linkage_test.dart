import 'dart:async';
import 'dart:ui' show SemanticsAction, Tristate;

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_journey_linkage_screen.dart';
import 'package:untitled/features/baby/screens/add_baby_screen.dart';
import 'package:untitled/features/baby/services/baby_create_intent_store.dart';
import 'package:untitled/features/baby/services/baby_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';

void main() {
  setUp(() => FlutterSecureStorage.setMockInitialValues({}));
  tearDown(AuthState.instance.clearState);

  group('Story 6.5 contracts', () {
    test(
      'create intent is account scoped and survives a store rebuild',
      () async {
        final storage = InMemoryBabyCreateIntentStorage();
        final first = BabyCreateIntentStore(storage: storage);
        const intent = BabyCreateIntent(
          submissionId: 'submission-1',
          nickname: 'Mochi',
          birthDate: '2026-07-20',
          gender: BabyGender.female,
          birthWeightKg: 3.2,
        );

        await first.write('account-a', 'journey-1', intent);
        final rebuilt = BabyCreateIntentStore(storage: storage);

        expect(await rebuilt.read('account-a', 'journey-1'), intent);
        expect(await rebuilt.read('account-b', 'journey-1'), isNull);
        await rebuilt.clearForAccount('account-a');
        expect(await rebuilt.read('account-a', 'journey-1'), isNull);
      },
    );
    test('journey-scoped create sends stable submission and journey ids', () {
      const request = CreateBabyRequest(
        nickname: 'Mochi',
        birthDate: '2026-07-20',
        gender: BabyGender.female,
        relatedJourneyId: 'journey-1',
        submissionId: 'submission-1',
      );

      expect(request.toJson(), containsPair('relatedJourneyId', 'journey-1'));
      expect(request.toJson(), containsPair('submissionId', 'submission-1'));
      expect(request.toJson(), equals(request.toJson()));
    });

    test('authoritative paginated read keeps linkage identity', () async {
      String? path;
      Map<String, dynamic>? query;
      final service = BabyService(
        get: (value, {queryParams}) async {
          path = value;
          query = queryParams;
          return {
            'data': {
              'content': [
                {
                  'id': 'baby-1',
                  'nickname': 'An',
                  'birthDate': '2026-07-01',
                  'gender': 'UNKNOWN',
                  'active': true,
                  'relatedJourneyId': 'journey-1',
                },
              ],
              'page': 0,
              'size': 20,
              'totalElements': 1,
              'totalPages': 1,
            },
          };
        },
      );

      final page = await service.listJourneyBabies('journey-1');

      expect(path, '/api/v1/journeys/journey-1/babies');
      expect(query, {'page': 0, 'size': 20});
      expect(page.items.single.relatedJourneyId, 'journey-1');
      expect(page.totalElements, 1);
    });

    test(
      'journey-scoped paginated read accepts the direct backend body',
      () async {
        final service = BabyService(
          get: (_, {queryParams}) async => {
            'success': true,
            'data': [
              {
                'id': 'baby-1',
                'nickname': 'An',
                'birthDate': '2026-07-01',
                'gender': 'UNKNOWN',
                'active': true,
                'relatedJourneyId': 'journey-1',
              },
            ],
            'page': 0,
            'size': 20,
            'totalElements': 1,
            'totalPages': 1,
          },
        );

        final page = await service.listJourneyBabies('journey-1');

        expect(page.items.single.relatedJourneyId, 'journey-1');
        expect(page.totalElements, 1);
      },
    );

    for (final malformedData in <Object?>[null, 'not-a-page', 7]) {
      test('journey-scoped paginated read rejects malformed data: '
          '${malformedData.runtimeType}', () async {
        final service = BabyService(
          get: (_, {queryParams}) async => {
            'success': true,
            'data': malformedData,
            'page': 0,
            'size': 20,
            'totalElements': 0,
            'totalPages': 0,
          },
        );

        await expectLater(
          service.listJourneyBabies('journey-1'),
          throwsA(isA<FormatException>()),
        );
      });
    }

    test(
      'journey-scoped paginated read rejects a body without data or items',
      () async {
        final service = BabyService(
          get: (_, {queryParams}) async => {
            'success': true,
            'page': 0,
            'size': 20,
            'totalElements': 0,
            'totalPages': 0,
          },
        );

        await expectLater(
          service.listJourneyBabies('journey-1'),
          throwsA(isA<FormatException>()),
        );
      },
    );

    test('link existing uses dedicated endpoint and command', () async {
      String? path;
      Map<String, dynamic>? body;
      final service = BabyService(
        put: (value, payload) async {
          path = value;
          body = payload;
          return {
            'data': {'babyId': 'baby-1', 'relatedJourneyId': 'journey-1'},
          };
        },
      );

      await service.linkBabyToJourney(
        babyId: 'baby-1',
        relatedJourneyId: 'journey-1',
        submissionId: 'submission-1',
      );

      expect(path, '/api/v1/babies/baby-1/journey-link');
      expect(body, {
        'relatedJourneyId': 'journey-1',
        'submissionId': 'submission-1',
      });
    });
  });

  group('Story 6.5 linkage surface', () {
    testWidgets('widget route host pops on do later and can reopen linkage', (
      tester,
    ) async {
      // This verifies Navigator behavior against a lightweight host widget; it
      // is not evidence for the production dashboard route configuration.
      final service = _FakeBabyService();
      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => Scaffold(
              body: FilledButton(
                key: const Key('reopen-baby-linkage'),
                onPressed: () => Navigator.of(context).push<void>(
                  MaterialPageRoute(
                    builder: (_) => BabyJourneyLinkageScreen(
                      journeyId: 'journey-1',
                      service: service,
                      eligibilityCheck: (_) async => true,
                    ),
                  ),
                ),
                child: const Text('Open'),
              ),
            ),
          ),
        ),
      );

      for (var visit = 0; visit < 2; visit++) {
        await tester.tap(find.byKey(const Key('reopen-baby-linkage')));
        await tester.pumpAndSettle();
        expect(find.byKey(const Key('baby-link-later')), findsOneWidget);
        await tester.tap(find.byKey(const Key('baby-link-later')));
        await tester.pumpAndSettle();
        expect(find.byKey(const Key('reopen-baby-linkage')), findsOneWidget);
      }

      expect(service.linkCalls, 0);
      expect(service.linked, isEmpty);
    });

    testWidgets('simulated widget-tree rebuild preserves zero-baby UI state', (
      tester,
    ) async {
      // Replacing the whole tree simulates reconstruction only. This is not a
      // process-level cold-start or secure-storage integration test.
      final service = _FakeBabyService();

      Future<void> boot() async {
        await tester.pumpWidget(
          MaterialApp(
            home: BabyJourneyLinkageScreen(
              journeyId: 'journey-1',
              service: service,
              eligibilityCheck: (_) async => true,
            ),
          ),
        );
        await tester.pumpAndSettle();
      }

      await boot();
      expect(find.byKey(const Key('linked-babies-empty')), findsOneWidget);
      expect(find.byKey(const Key('baby-link-later')), findsOneWidget);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump();
      await boot();

      expect(find.byKey(const Key('linked-babies-empty')), findsOneWidget);
      expect(find.byKey(const Key('baby-link-later')), findsOneWidget);
      expect(service.listJourneyCalls, 2);
    });

    testWidgets('fake create result triggers linked-set refresh in widget', (
      tester,
    ) async {
      // The fake service models the authoritative response contract; no real
      // API, persistence, or backend authorization boundary is exercised.
      final service = _FakeBabyService();
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => true,
            onCreate: () async {
              service.linked = [_baby('created-baby', 'journey-1')];
              return true;
            },
          ),
        ),
      );
      await tester.pumpAndSettle();
      final callsBeforeCreate = service.listJourneyCalls;

      await tester.tap(find.byKey(const Key('baby-link-create')));
      await tester.pumpAndSettle();

      expect(find.text('created-baby'), findsOneWidget);
      expect(service.listJourneyCalls, callsBeforeCreate + 1);
    });

    testWidgets('deep-link eligibility fails closed before baby data loads', (
      tester,
    ) async {
      final service = _FakeBabyService();
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => false,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('baby-link-ineligible')), findsOneWidget);
      expect(find.byKey(const Key('baby-link-create')), findsNothing);
      expect(service.listJourneyCalls, 0);
    });

    testWidgets(
      'malformed successful pagination response renders recoverable error',
      (tester) async {
        final service = BabyService(
          get: (_, {queryParams}) async => {
            'success': true,
            'data': null,
            'page': 0,
            'size': 20,
            'totalElements': 0,
            'totalPages': 0,
          },
        );
        await tester.pumpWidget(
          MaterialApp(
            home: BabyJourneyLinkageScreen(
              journeyId: 'journey-1',
              service: service,
              eligibilityCheck: (_) async => true,
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('linked-babies-empty')), findsNothing);
        expect(find.textContaining('Không thể tải hồ sơ bé'), findsOneWidget);
        expect(find.byKey(const Key('linked-babies-refresh')), findsOneWidget);
      },
    );

    testWidgets('direct create deep link also fails closed', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: AddBabyScreen(
            relatedJourneyId: 'journey-1',
            accountId: 'account-a',
            intentStore: BabyCreateIntentStore(
              storage: InMemoryBabyCreateIntentStorage(),
            ),
            eligibilityCheck: (_) async => false,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(Form), findsNothing);
      expect(find.textContaining('Không thể tạo và liên kết'), findsOneWidget);
    });

    testWidgets('many candidates remain scrollable with large text', (
      tester,
    ) async {
      final service = _FakeBabyService(
        all: List.generate(40, (i) => _baby('baby-$i', null)),
      );
      await tester.pumpWidget(
        MediaQuery(
          data: const MediaQueryData(textScaler: TextScaler.linear(2)),
          child: MaterialApp(
            home: BabyJourneyLinkageScreen(
              journeyId: 'journey-1',
              service: service,
              eligibilityCheck: (_) async => true,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('baby-link-existing')));
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(
        find.byKey(const Key('baby-link-candidates-list')),
        findsOneWidget,
      );
      final scrollable = tester.state<ScrollableState>(
        find.descendant(
          of: find.byKey(const Key('baby-link-candidates-list')),
          matching: find.byType(Scrollable),
        ),
      );
      expect(scrollable.position.maxScrollExtent, greaterThan(0));
      scrollable.position.jumpTo(500);
      await tester.pump();
      expect(scrollable.position.pixels, greaterThan(0));
      expect(tester.takeException(), isNull);
    });
    testWidgets('offers equal create, link existing, and do later actions', (
      tester,
    ) async {
      var deferred = false;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BabyJourneyLinkageActions(
              journeyId: 'journey-1',
              onCreate: () {},
              onLinkExisting: () {},
              onDoLater: () => deferred = true,
            ),
          ),
        ),
      );

      expect(find.byKey(const Key('baby-link-create')), findsOneWidget);
      expect(find.byKey(const Key('baby-link-existing')), findsOneWidget);
      expect(find.byKey(const Key('baby-link-later')), findsOneWidget);
      for (final key in const [
        Key('baby-link-create'),
        Key('baby-link-existing'),
        Key('baby-link-later'),
      ]) {
        expect(
          tester.getSize(find.byKey(key)).height,
          greaterThanOrEqualTo(48),
        );
      }
      await tester.tap(find.byKey(const Key('baby-link-later')));
      expect(deferred, isTrue);
    });

    testWidgets('renders zero, one, and multiple authoritative linked babies', (
      tester,
    ) async {
      final service = _FakeBabyService(
        linked: [_baby('one', 'journey-1'), _baby('two', 'journey-1')],
      );
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('one'), findsOneWidget);
      expect(find.text('two'), findsOneWidget);
      expect(
        tester
            .widget<ListTile>(find.byKey(const Key('linked-baby-selector-one')))
            .selected,
        isTrue,
      );
      final semantics = tester
          .getSemantics(find.byKey(const Key('linked-baby-semantics-one')))
          .getSemanticsData();
      expect(semantics.label, 'one, đang chọn');
      expect(semantics.flagsCollection.isButton, isTrue);
      expect(semantics.flagsCollection.isSelected, Tristate.isTrue);
      expect(semantics.hasAction(SemanticsAction.tap), isTrue);

      await tester.tap(find.byKey(const Key('linked-baby-selector-two')));
      await tester.pump();
      expect(
        tester
            .widget<ListTile>(find.byKey(const Key('linked-baby-selector-two')))
            .selected,
        isTrue,
      );
      expect(
        tester
            .getSize(find.byKey(const Key('linked-baby-selector-two')))
            .height,
        greaterThanOrEqualTo(48),
      );

      await tester.tap(find.byKey(const Key('linked-babies-refresh')));
      await tester.pumpAndSettle();
      expect(
        tester
            .widget<ListTile>(find.byKey(const Key('linked-baby-selector-two')))
            .selected,
        isTrue,
      );

      service.linked = [];
      await tester.tap(find.byKey(const Key('linked-babies-refresh')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('linked-babies-empty')), findsOneWidget);
    });

    testWidgets('selector is distinct from active-baby switching', (
      tester,
    ) async {
      final service = _FakeBabyService(
        all: [
          _baby('available', null, active: false),
          _baby('already linked', 'other-journey'),
        ],
      );
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('baby-link-existing')));
      await tester.pumpAndSettle();

      expect(find.text('available'), findsOneWidget);
      expect(find.text('already linked'), findsNothing);
      await tester.tap(find.text('available'));
      await tester.pumpAndSettle();
      expect(service.linkCalls, 1);
      expect(service.switchCalls, 0);
      expect(find.text('available'), findsOneWidget);
      expect(service.listJourneyCalls, greaterThanOrEqualTo(2));
    });

    testWidgets('account switch dismisses a candidate sheet immediately', (
      tester,
    ) async {
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'mother-a',
        role: 'MOTHER',
      );
      final service = _FakeBabyService(all: [_baby('private-a', null)]);
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('baby-link-existing')));
      await tester.pumpAndSettle();
      expect(find.text('private-a'), findsOneWidget);

      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'mother-b',
        role: 'MOTHER',
      );
      await tester.pump();
      expect(
        find.text('private-a'),
        findsNothing,
        reason:
            'Old-account nickname must be cleared before sheet dismissal completes.',
      );
      await tester.pumpAndSettle();

      expect(find.text('private-a'), findsNothing);
      expect(service.linkCalls, 0);
    });

    testWidgets(
      'account switch removes only its candidate sheet below a newer dialog',
      (tester) async {
        await AuthState.instance.setTokens(
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'mother-a',
          role: 'MOTHER',
        );
        final navigatorKey = GlobalKey<NavigatorState>();
        final service = _FakeBabyService(all: [_baby('private-a', null)]);
        await tester.pumpWidget(
          MaterialApp(
            navigatorKey: navigatorKey,
            home: BabyJourneyLinkageScreen(
              journeyId: 'journey-1',
              service: service,
              eligibilityCheck: (_) async => true,
            ),
          ),
        );
        await tester.pumpAndSettle();
        await tester.tap(find.byKey(const Key('baby-link-existing')));
        await tester.pumpAndSettle();
        expect(find.text('private-a'), findsOneWidget);

        navigatorKey.currentState!.push<void>(
          DialogRoute<void>(
            context: navigatorKey.currentContext!,
            builder: (_) => const AlertDialog(
              key: Key('newer-account-dialog'),
              title: Text('New account notice'),
            ),
          ),
        );
        await tester.pumpAndSettle();

        await AuthState.instance.setTokens(
          accessToken: 'access-b',
          refreshToken: 'refresh-b',
          userId: 'mother-b',
          role: 'MOTHER',
        );
        await tester.pumpAndSettle();

        expect(
          find.byKey(const Key('newer-account-dialog')),
          findsOneWidget,
          reason: 'Account cleanup must never pop a newer route.',
        );
        expect(find.text('private-a'), findsNothing);
        expect(service.linkCalls, 0);
      },
    );

    testWidgets('double tap opens only one candidate load lifecycle', (
      tester,
    ) async {
      final service = _DelayedCandidateBabyService();
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('baby-link-existing')));
      await tester.tap(find.byKey(const Key('baby-link-existing')));
      await tester.pump();
      expect(service.listCalls, 1);

      service.candidates.complete(const []);
      await tester.pumpAndSettle();
    });

    testWidgets('late response cannot replace a newer journey linked set', (
      tester,
    ) async {
      final first = Completer<BabyProfilePage>();
      final service = _GenerationService(first);
      final key = GlobalKey<BabyJourneyLinkageScreenState>();
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            key: key,
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => true,
          ),
        ),
      );
      await tester.pump();
      key.currentState!.refresh();
      await tester.pump();
      first.complete(BabyProfilePage(items: [_baby('stale', 'journey-1')]));
      await tester.pumpAndSettle();

      expect(find.text('fresh'), findsOneWidget);
      expect(find.text('stale'), findsNothing);
    });

    testWidgets('loads every authoritative page without duplicates', (
      tester,
    ) async {
      final service = _PagedBabyService();
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(service.pages, [0, 1, 2]);
      expect(find.text('baby-1'), findsOneWidget);
      expect(find.text('baby-2'), findsOneWidget);
      await tester.scrollUntilVisible(find.text('baby-3'), 200);
      expect(find.text('baby-3'), findsOneWidget);
    });

    testWidgets('link retry keeps one submission id', (tester) async {
      final service = _RetryLinkService(all: [_baby('available', null)]);
      await tester.pumpWidget(
        MaterialApp(
          home: BabyJourneyLinkageScreen(
            journeyId: 'journey-1',
            service: service,
            eligibilityCheck: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();

      for (var attempt = 0; attempt < 2; attempt++) {
        await tester.tap(find.byKey(const Key('baby-link-existing')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('available'));
        await tester.pumpAndSettle();
      }

      expect(service.submissionIds, hasLength(2));
      expect(service.submissionIds.toSet(), hasLength(1));
    });
  });
}

BabyProfile _baby(String nickname, String? journeyId, {bool active = true}) =>
    BabyProfile(
      id: nickname,
      nickname: nickname,
      birthDate: DateTime(2026, 7, 1),
      gender: BabyGender.unknown,
      isActive: active,
      relatedJourneyId: journeyId,
    );

class _FakeBabyService extends BabyService {
  _FakeBabyService({this.linked = const [], this.all = const []});
  List<BabyProfile> linked;
  List<BabyProfile> all;
  int linkCalls = 0;
  int switchCalls = 0;
  int listJourneyCalls = 0;

  @override
  Future<BabyProfilePage> listJourneyBabies(
    String journeyId, {
    int page = 0,
    int size = 20,
  }) async {
    listJourneyCalls++;
    return BabyProfilePage(items: linked);
  }

  @override
  Future<List<BabyProfile>> listBabyProfiles() async => all;

  @override
  Future<void> linkBabyToJourney({
    required String babyId,
    required String relatedJourneyId,
    required String submissionId,
  }) async {
    linkCalls++;
    final linkedBaby = _baby(babyId, relatedJourneyId);
    linked = [linkedBaby];
  }

  @override
  Future<BabyProfile> switchActiveBabyProfile(String babyId) async {
    switchCalls++;
    return _baby(babyId, null);
  }
}

class _GenerationService extends BabyService {
  _GenerationService(this.first);
  final Completer<BabyProfilePage> first;
  int calls = 0;

  @override
  Future<BabyProfilePage> listJourneyBabies(
    String journeyId, {
    int page = 0,
    int size = 20,
  }) {
    calls++;
    if (calls == 1) return first.future;
    return Future.value(BabyProfilePage(items: [_baby('fresh', journeyId)]));
  }
}

class _PagedBabyService extends BabyService {
  final List<int> pages = [];

  @override
  Future<BabyProfilePage> listJourneyBabies(
    String journeyId, {
    int page = 0,
    int size = 20,
  }) async {
    pages.add(page);
    return BabyProfilePage(
      items: [
        if (page == 0) _baby('baby-1', journeyId),
        if (page == 1) ...[
          _baby('baby-1', journeyId),
          _baby('baby-2', journeyId),
        ],
        if (page == 2) _baby('baby-3', journeyId),
      ],
      page: page,
      size: size,
      totalPages: 3,
      totalElements: 4,
    );
  }
}

class _RetryLinkService extends _FakeBabyService {
  _RetryLinkService({required super.all});
  final List<String> submissionIds = [];

  @override
  Future<void> linkBabyToJourney({
    required String babyId,
    required String relatedJourneyId,
    required String submissionId,
  }) async {
    submissionIds.add(submissionId);
    if (submissionIds.length == 1) throw Exception('ambiguous timeout');
    linked = [_baby(babyId, relatedJourneyId)];
  }
}

class _DelayedCandidateBabyService extends _FakeBabyService {
  final candidates = Completer<List<BabyProfile>>();
  int listCalls = 0;

  @override
  Future<List<BabyProfile>> listBabyProfiles() {
    listCalls++;
    return candidates.future;
  }
}
