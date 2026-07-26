import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/community/models/content_model.dart';
import 'package:untitled/features/community/screens/verified_content_search_screen.dart';
import 'package:untitled/features/community/screens/view_content_screen.dart';
import 'package:untitled/features/community/services/content_service.dart';

Map<String, dynamic> _lifecycleContentEnvelope() => {
  'data': {
    'stage': 'PRE_PREGNANCY',
    'payload': {
      'data': [
        {
          'id': 'content-69',
          'type': 'ARTICLE',
          'title': 'Synthetic lifecycle guidance',
          'stage': 'PRE_PREGNANCY',
          'topicId': 'topic-69',
        },
      ],
      'page': 0,
      'size': 20,
      'totalElements': 1,
      'totalPages': 1,
    },
  },
};

Map<String, dynamic> _genericPage(String title) => {
  'data': [
    {
      'id': 'generic-${title.hashCode}',
      'type': 'ARTICLE',
      'title': title,
      'stage': 'PREGNANCY',
      'topicId': 'topic-69',
    },
  ],
  'page': 0,
  'size': 20,
  'totalElements': 1,
  'totalPages': 1,
};

class _DeferredSearchHarness {
  final paths = <String>[];
  final pending = <Completer<dynamic>>[];

  late final ContentService service = ContentService(
    getRequest: (path) {
      paths.add(path);
      final completer = Completer<dynamic>();
      pending.add(completer);
      return completer.future;
    },
  );
}

Future<void> _pumpSearch(
  WidgetTester tester,
  _DeferredSearchHarness harness,
) async {
  await tester.pumpWidget(
    MaterialApp(
      home: VerifiedContentSearchScreen(contentService: harness.service),
    ),
  );
  await tester.pump();
  expect(harness.pending, hasLength(1));
}

void main() {
  group('UC82-69-MOB-005 deliberate generic verified search', () {
    testWidgets(
      'lifecycle CTA opens editable generic browse without another lifecycle request',
      (tester) async {
        final paths = <String>[];
        final service = ContentService(
          getRequest: (path) async {
            paths.add(path);
            if (path == '/api/v1/content/lifecycle/checklists') {
              return {
                'data': {'stage': 'PRE_PREGNANCY', 'payload': const []},
              };
            }
            if (path.startsWith('/api/v1/content/lifecycle?')) {
              return _lifecycleContentEnvelope();
            }
            return {
              'data': const [],
              'page': 0,
              'size': 20,
              'totalElements': 0,
              'totalPages': 0,
            };
          },
        );

        await tester.pumpWidget(
          MaterialApp(
            home: ViewContentScreen(
              mode: ContentBrowseMode.lifecycle,
              contentService: service,
            ),
          ),
        );
        await tester.pumpAndSettle();
        expect(
          paths.where((path) => path.contains('/lifecycle')),
          hasLength(2),
        );

        await tester.tap(
          find.widgetWithText(
            OutlinedButton,
            'Khám phá nội dung theo lựa chọn',
          ),
        );
        await tester.pumpAndSettle();

        expect(find.byType(VerifiedContentSearchScreen), findsOneWidget);
        expect(find.byType(TextField), findsOneWidget);
        expect(find.text('Sơ sinh'), findsOneWidget);
        expect(
          find.byKey(const Key('lifecycle-content-stage-locked')),
          findsNothing,
        );
        expect(
          paths.where((path) => path.startsWith('/api/v1/content?')),
          hasLength(1),
        );
        expect(
          paths.where((path) => path.contains('/lifecycle')),
          hasLength(2),
        );
        expect(tester.takeException(), isNull);
      },
    );
  });

  group('R69-026 generic search async ownership', () {
    testWidgets(
      'query edit invalidates initial success before debounce and latest response wins',
      (tester) async {
        final harness = _DeferredSearchHarness();
        await _pumpSearch(tester, harness);

        await tester.enterText(find.byType(TextField), 'latest query');
        await tester.pump();
        harness.pending[0].complete(_genericPage('Stale initial result'));
        await tester.pump();

        expect(find.text('Stale initial result'), findsNothing);
        expect(find.byType(CircularProgressIndicator), findsOneWidget);

        await tester.pump(const Duration(milliseconds: 401));
        expect(harness.pending, hasLength(2));
        expect(harness.paths.last, contains('/api/v1/content/search?'));
        harness.pending[1].complete(_genericPage('Latest query result'));
        await tester.pumpAndSettle();

        expect(find.text('Latest query result'), findsOneWidget);
        expect(find.text('Stale initial result'), findsNothing);
        expect(find.byType(CircularProgressIndicator), findsNothing);
      },
    );

    testWidgets(
      'stale error cannot erase newer filter results or loading state',
      (tester) async {
        final harness = _DeferredSearchHarness();
        await _pumpSearch(tester, harness);

        await tester.tap(find.widgetWithText(OutlinedButton, 'Bài viết'));
        await tester.pump();
        expect(harness.pending, hasLength(2));

        harness.pending[0].completeError(StateError('stale offline'));
        await tester.pump();
        expect(find.byType(CircularProgressIndicator), findsOneWidget);
        expect(
          find.byKey(const Key('generic-content-search-error')),
          findsNothing,
        );

        harness.pending[1].complete(_genericPage('Current filtered result'));
        await tester.pumpAndSettle();
        expect(find.text('Current filtered result'), findsOneWidget);
        expect(find.byType(CircularProgressIndicator), findsNothing);
        expect(
          find.byKey(const Key('generic-content-search-error')),
          findsNothing,
        );
      },
    );

    testWidgets('stale success cannot replace the latest visible error', (
      tester,
    ) async {
      final harness = _DeferredSearchHarness();
      await _pumpSearch(tester, harness);

      await tester.tap(find.text('Sơ sinh'));
      await tester.pump();
      expect(harness.pending, hasLength(2));

      harness.pending[1].completeError(StateError('latest offline'));
      await tester.pumpAndSettle();
      expect(
        find.byKey(const Key('generic-content-search-error')),
        findsOneWidget,
      );
      expect(find.byType(CircularProgressIndicator), findsNothing);

      harness.pending[0].complete(_genericPage('Stale success'));
      await tester.pump();
      expect(find.text('Stale success'), findsNothing);
      expect(
        find.byKey(const Key('generic-content-search-error')),
        findsOneWidget,
      );
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });

    testWidgets(
      'account switch reloads and discards the previous account response',
      (tester) async {
        FlutterSecureStorage.setMockInitialValues({});
        await AuthState.instance.clear();
        await AuthState.instance.setTokens(
          accessToken: 'synthetic-access-a',
          refreshToken: 'synthetic-refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        );
        addTearDown(AuthState.instance.clear);
        final harness = _DeferredSearchHarness();
        await _pumpSearch(tester, harness);

        await AuthState.instance.setTokens(
          accessToken: 'synthetic-access-b',
          refreshToken: 'synthetic-refresh-b',
          userId: 'account-b',
          role: 'MOTHER',
        );
        await tester.pump();
        expect(harness.pending, hasLength(2));

        harness.pending[1].complete(_genericPage('Account B result'));
        await tester.pumpAndSettle();
        harness.pending[0].complete(_genericPage('Account A stale result'));
        await tester.pump();

        expect(find.text('Account B result'), findsOneWidget);
        expect(find.text('Account A stale result'), findsNothing);
        expect(find.byType(CircularProgressIndicator), findsNothing);
      },
    );

    testWidgets(
      'dispose invalidates a pending response without setState errors',
      (tester) async {
        final harness = _DeferredSearchHarness();
        await _pumpSearch(tester, harness);

        await tester.pumpWidget(const MaterialApp(home: SizedBox.shrink()));
        harness.pending.single.completeError(StateError('late disposed error'));
        await tester.pump();

        expect(tester.takeException(), isNull);
      },
    );
  });
}
