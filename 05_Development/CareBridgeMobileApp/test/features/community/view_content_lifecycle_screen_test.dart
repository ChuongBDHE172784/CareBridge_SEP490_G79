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
  group('Family generic content', () {
    testWidgets(
      'loads all stages and supports normal search and stage filters',
      (tester) async {
        final paths = <String>[];
        final service = ContentService(
          getRequest: (path) async {
            paths.add(path);
            if (path == '/api/v1/content/family-article') {
              return {
                'data': {
                  'id': 'family-article',
                  'type': 'ARTICLE',
                  'title': 'Dinh dưỡng thai kỳ',
                  'body': 'Nội dung chi tiết',
                  'stage': 'PREGNANCY',
                  'version': 1,
                },
              };
            }
            final isFaq = path.contains('type=FAQ');
            return {
              'data': [
                {
                  'id': isFaq ? 'family-faq' : 'family-article',
                  'type': isFaq ? 'FAQ' : 'ARTICLE',
                  'title': isFaq ? 'FAQ sau sinh' : 'Dinh dưỡng thai kỳ',
                  'stage': isFaq ? 'POSTPARTUM' : 'PREGNANCY',
                  'topicId': 'topic-family',
                },
              ],
              'page': 0,
              'size': 50,
              'totalElements': 1,
              'totalPages': 1,
            };
          },
        );

        await tester.pumpWidget(
          MaterialApp(
            home: ViewContentScreen(
              mode: ContentBrowseMode.family,
              contentService: service,
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.text('Dinh dưỡng thai kỳ'), findsOneWidget);
        expect(find.text('Giai đoạn nội dung'), findsNothing);
        expect(find.text('Đang xác định từ máy chủ'), findsNothing);
        expect(find.text('Tất cả giai đoạn'), findsOneWidget);
        expect(find.text('Danh mục'), findsNothing);
        expect(find.widgetWithText(ChoiceChip, 'Checklist'), findsNothing);
        expect(paths.where((path) => path.contains('stage=')), isEmpty);
        expect(
          paths.where((path) => path.contains('/family/care-groups/')),
          isEmpty,
        );

        await tester.tap(find.widgetWithText(ChoiceChip, 'FAQ'));
        await tester.pump();
        expect(find.text('FAQ sau sinh'), findsOneWidget);
        await tester.tap(find.widgetWithText(ChoiceChip, 'Tất cả'));
        await tester.pump();

        await tester.enterText(find.byType(TextField), 'Dinh dưỡng');
        await tester.pump();
        expect(find.text('Dinh dưỡng thai kỳ'), findsOneWidget);
        expect(find.text('FAQ sau sinh'), findsNothing);

        final stageList = find.byWidgetPredicate(
          (widget) =>
              widget is ListView && widget.scrollDirection == Axis.horizontal,
        );
        await tester.drag(stageList, const Offset(-500, 0));
        await tester.pump();
        await tester.tap(find.text('Hậu sản & Chăm bé'));
        await tester.pumpAndSettle();

        expect(paths.any((path) => path.contains('stage=POSTPARTUM')), isTrue);
        expect(tester.takeException(), isNull);
      },
    );
  });

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
      expect(find.text('Khám phá nội dung theo lựa chọn'), findsNothing);
      expect(
        find.byKey(const Key('lifecycle-content-generic-browse')),
        findsNothing,
      );
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

    testWidgets(
      'direct lifecycle screen enables checklist action without a client journey id',
      (tester) async {
        final service = ContentService(
          getRequest: (path) async {
            if (path.endsWith('/checklists')) {
              return {
                'data': {
                  'stage': 'PRE_PREGNANCY',
                  'payload': [
                    {
                      'id': 'checklist-69',
                      'name': 'Direct lifecycle checklist',
                      'stage': 'PRE_PREGNANCY',
                      'description': 'Server-resolved import context',
                      'items': [
                        {
                          'id': 'template-item-69',
                          'itemText': 'Prepare lifecycle item',
                          'order': 1,
                          'isRequired': true,
                        },
                      ],
                    },
                  ],
                },
              };
            }
            return _contentEnvelope(items: const []);
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

        await tester.tap(find.text('Direct lifecycle checklist'));
        await tester.pumpAndSettle();

        // The detail action bar uses ElevatedButton.icon, whose factory returns the
        // private _ElevatedButtonWithIcon subclass. find.byType matches the exact
        // runtime type, so it never sees that button — match on the supertype instead.
        final action = tester.widget<ElevatedButton>(
          find.byWidgetPredicate((widget) => widget is ElevatedButton).last,
        );
        expect(action.onPressed, isNotNull);
        expect(tester.takeException(), isNull);
      },
    );
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

    testWidgets('category filter is removed in lifecycle mode (mother role)', (
      tester,
    ) async {
      await tester.pumpWidget(
        MaterialApp(
          home: ViewContentScreen(
            mode: ContentBrowseMode.lifecycle,
            contentService: _service(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Lọc nội dung'), findsNothing);
      expect(find.text('Chủ đề'), findsNothing);
      expect(tester.takeException(), isNull);
    });
  });
}
