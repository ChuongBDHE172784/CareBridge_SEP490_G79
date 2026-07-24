import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/community/models/content_model.dart';
import 'package:untitled/features/community/screens/view_content_screen.dart';
import 'package:untitled/features/community/services/content_service.dart';

Map<String, dynamic> _contentEnvelope({
  String title = 'Synthetic lifecycle guidance',
  List<Map<String, dynamic>>? items,
}) => {
  'data': {
    'stage': 'PRE_PREGNANCY',
    'payload': {
      'data':
          items ??
          [
            {
              'id': 'content-69',
              'type': 'ARTICLE',
              'title': title,
              'stage': 'PRE_PREGNANCY',
              'topicId': 'topic-69',
            },
          ],
      'page': 0,
      'size': 20,
      'totalElements': items?.length ?? 1,
      'totalPages': (items?.isEmpty ?? false) ? 0 : 1,
    },
  },
};

Map<String, dynamic> _checklistEnvelope() => {
  'data': {'stage': 'PRE_PREGNANCY', 'payload': const []},
};

ContentService _service({int failures = 0, List<String>? paths}) {
  var failuresRemaining = failures;
  return ContentService(
    getRequest: (path) async {
      paths?.add(path);
      if (failuresRemaining > 0) {
        failuresRemaining--;
        throw StateError('synthetic offline');
      }
      return path.endsWith('/checklists')
          ? _checklistEnvelope()
          : _contentEnvelope();
    },
  );
}

void main() {
  group('UC82-69-MOB-002 lifecycle list states', () {
    testWidgets('server PRE stage is locked and lifecycle data is rendered', (
      tester,
    ) async {
      final paths = <String>[];
      await tester.pumpWidget(
        MaterialApp(
          home: ViewContentScreen(
            mode: ContentBrowseMode.lifecycle,
            contentService: _service(paths: paths),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('lifecycle-content-stage')), findsOneWidget);
      expect(
        find.byKey(const Key('lifecycle-content-stage-locked')),
        findsOneWidget,
      );
      expect(find.text('Synthetic lifecycle guidance'), findsOneWidget);
      expect(paths, hasLength(2));
      expect(paths.first, isNot(contains('stage=')));
      expect(tester.takeException(), isNull);
    });

    testWidgets('empty lifecycle response renders the accessible empty state', (
      tester,
    ) async {
      final service = ContentService(
        getRequest: (path) async => path.endsWith('/checklists')
            ? _checklistEnvelope()
            : _contentEnvelope(items: const []),
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

      expect(find.byKey(const Key('lifecycle-content-empty')), findsOneWidget);
      expect(
        find.text('Chưa có nội dung đã kiểm duyệt cho giai đoạn này.'),
        findsOneWidget,
      );
      expect(find.byKey(const Key('lifecycle-content-error')), findsNothing);
      expect(tester.takeException(), isNull);
    });

    testWidgets(
      'selected empty tab renders its empty state instead of blank content',
      (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: ViewContentScreen(
              mode: ContentBrowseMode.lifecycle,
              contentService: _service(),
            ),
          ),
        );
        await tester.pumpAndSettle();
        expect(find.text('Synthetic lifecycle guidance'), findsOneWidget);

        await tester.tap(find.widgetWithText(ChoiceChip, 'FAQ'));
        await tester.pump();

        expect(
          find.byKey(const Key('lifecycle-content-empty')),
          findsOneWidget,
        );
        expect(find.text('Synthetic lifecycle guidance'), findsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'article and checklist tabs also use the selected empty state',
      (tester) async {
        final service = ContentService(
          getRequest: (path) async => path.endsWith('/checklists')
              ? _checklistEnvelope()
              : _contentEnvelope(
                  items: const [
                    {
                      'id': 'faq-69',
                      'type': 'FAQ',
                      'title': 'Synthetic lifecycle FAQ',
                      'stage': 'PRE_PREGNANCY',
                      'topicId': 'topic-69',
                    },
                  ],
                ),
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

        for (final label in const ['Bài viết', 'Checklist']) {
          await tester.tap(find.widgetWithText(ChoiceChip, label));
          await tester.pump();
          expect(
            find.byKey(const Key('lifecycle-content-empty')),
            findsOneWidget,
          );
        }
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets('failure clears stale rows and exposes a working retry', (
      tester,
    ) async {
      await tester.pumpWidget(
        MaterialApp(
          home: ViewContentScreen(
            mode: ContentBrowseMode.lifecycle,
            contentService: _service(failures: 1),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('lifecycle-content-error')), findsOneWidget);
      expect(find.text('Synthetic lifecycle guidance'), findsNothing);

      await tester.tap(find.byKey(const Key('lifecycle-content-retry')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('lifecycle-content-error')), findsNothing);
      expect(find.text('Synthetic lifecycle guidance'), findsOneWidget);
    });
  });

  group('UC82-69-MOB-004 stale response and accessibility boundary', () {
    testWidgets('account switch discards A and reloads lifecycle data for B', (
      tester,
    ) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'synthetic-access-a',
        refreshToken: 'synthetic-refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      final pending = <Completer<dynamic>>[];
      final paths = <String>[];
      final service = ContentService(
        getRequest: (path) {
          paths.add(path);
          final completer = Completer<dynamic>();
          pending.add(completer);
          return completer.future;
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
      await tester.pump();
      expect(pending, hasLength(2));

      await AuthState.instance.setTokens(
        accessToken: 'synthetic-access-b',
        refreshToken: 'synthetic-refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      await tester.pump();
      expect(pending, hasLength(4));

      pending[0].complete(_contentEnvelope(title: 'Account A stale content'));
      pending[1].complete(_checklistEnvelope());
      await tester.pump();
      expect(find.text('Account A stale content'), findsNothing);

      pending[2].complete(_contentEnvelope(title: 'Account B current content'));
      pending[3].complete(_checklistEnvelope());
      await tester.pumpAndSettle();

      expect(find.text('Account A stale content'), findsNothing);
      expect(find.text('Account B current content'), findsOneWidget);
      expect(paths, hasLength(4));
    });

    testWidgets('lifecycle content remains usable at 200 percent text scale', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(430, 932);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await tester.pumpWidget(
        MaterialApp(
          home: MediaQuery(
            data: const MediaQueryData(textScaler: TextScaler.linear(2)),
            child: ViewContentScreen(
              mode: ContentBrowseMode.lifecycle,
              contentService: _service(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('lifecycle-content-stage')), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  });
}
