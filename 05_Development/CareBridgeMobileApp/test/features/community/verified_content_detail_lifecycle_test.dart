import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/community/models/content_model.dart';
import 'package:untitled/features/community/screens/verified_content_detail_screen.dart';
import 'package:untitled/features/community/services/content_service.dart';

Map<String, dynamic> _payload({
  String stage = 'PRE_PREGNANCY',
  String title = 'Synthetic lifecycle',
}) => {
  'id': 'detail-69',
  'type': 'ARTICLE',
  'title': title,
  'body': '$title body',
  'stage': stage,
  'topicId': 'topic-69',
  'version': 1,
};

Map<String, dynamic> _envelope({
  String payloadStage = 'PRE_PREGNANCY',
  String title = 'Synthetic lifecycle',
}) => {
  'data': {
    'stage': 'PRE_PREGNANCY',
    'payload': _payload(stage: payloadStage, title: title),
  },
};

void main() {
  group('UC82-69-MOB-006 lifecycle detail boundary', () {
    testWidgets('family detail uses the generic approved-content route', (
      tester,
    ) async {
      final paths = <String>[];
      final service = ContentService(
        getRequest: (path) async {
          paths.add(path);
          return {'data': _payload()};
        },
      );

      await tester.pumpWidget(
        MaterialApp(
          home: VerifiedContentDetailScreen(
            contentId: 'detail-69',
            mode: ContentBrowseMode.family,
            contentService: service,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Synthetic lifecycle body'), findsOneWidget);
      expect(paths.single, '/api/v1/content/detail-69');
      expect(
        find.byKey(const Key('lifecycle-content-detail-stage')),
        findsNothing,
      );
      expect(tester.takeException(), isNull);
    });

    testWidgets('lifecycle route renders only same-stage detail', (
      tester,
    ) async {
      final paths = <String>[];
      final service = ContentService(
        getRequest: (path) async {
          paths.add(path);
          return _envelope();
        },
      );
      await tester.pumpWidget(
        MaterialApp(
          home: VerifiedContentDetailScreen(
            contentId: 'detail-69',
            mode: ContentBrowseMode.lifecycle,
            contentService: service,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Synthetic lifecycle body'), findsOneWidget);
      expect(
        find.byKey(const Key('lifecycle-content-detail-stage')),
        findsOneWidget,
      );
      expect(paths.single, '/api/v1/content/lifecycle/detail-69');
    });

    testWidgets('stage mismatch renders retry without stale body', (
      tester,
    ) async {
      final service = ContentService(
        getRequest: (_) async => _envelope(payloadStage: 'PREGNANCY'),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: VerifiedContentDetailScreen(
            contentId: 'detail-69',
            mode: ContentBrowseMode.lifecycle,
            contentService: service,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Synthetic lifecycle body'), findsNothing);
      expect(
        find.byKey(const Key('lifecycle-content-detail-error')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('lifecycle-content-detail-retry')),
        findsOneWidget,
      );
    });

    testWidgets('generic mode preserves the UC-225 detail route', (
      tester,
    ) async {
      final paths = <String>[];
      final service = ContentService(
        getRequest: (path) async {
          paths.add(path);
          return {'data': _payload()};
        },
      );
      await tester.pumpWidget(
        MaterialApp(
          home: VerifiedContentDetailScreen(
            contentId: 'detail-69',
            contentService: service,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Synthetic lifecycle body'), findsOneWidget);
      expect(paths.single, '/api/v1/content/detail-69');
    });
  });

  group('UC82-69-MOB-004 lifecycle detail stale response guard', () {
    testWidgets('account switch discards stale lifecycle detail response', (
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
      final service = ContentService(
        getRequest: (_) {
          final completer = Completer<dynamic>();
          pending.add(completer);
          return completer.future;
        },
      );

      await tester.pumpWidget(
        MaterialApp(
          home: VerifiedContentDetailScreen(
            contentId: 'detail-69',
            mode: ContentBrowseMode.lifecycle,
            contentService: service,
          ),
        ),
      );
      await tester.pump();
      expect(pending, hasLength(1));

      await AuthState.instance.setTokens(
        accessToken: 'synthetic-access-b',
        refreshToken: 'synthetic-refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      await tester.pump();
      expect(pending, hasLength(2));

      pending[0].complete(_envelope(title: 'Account A stale detail'));
      await tester.pump();
      expect(find.text('Account A stale detail body'), findsNothing);

      pending[1].complete(_envelope(title: 'Account B current detail'));
      await tester.pumpAndSettle();
      expect(find.text('Account A stale detail body'), findsNothing);
      expect(find.text('Account B current detail body'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  });
}
