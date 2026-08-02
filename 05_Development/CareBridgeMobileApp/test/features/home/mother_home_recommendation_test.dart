import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/screens/verified_content_detail_screen.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/recommendation/models/recommendation_model.dart';
import 'package:untitled/features/reminder/services/today_task_service.dart';

TodayTaskService _todayTaskService() => TodayTaskService(
  getRequest: (_, {queryParams}) async => {
    'data': {
      'asOf': '2026-08-02T00:00:00Z',
      'zoneId': 'Asia/Ho_Chi_Minh',
      'horizonDays': 7,
      'sections': {
        'overdue': [],
        'today': [],
        'upcoming': [],
        'unscheduled': [],
      },
      'counts': {'overdue': 0, 'today': 0, 'upcoming': 0, 'unscheduled': 0},
      'correlationId': 'recommendation-home-test',
    },
  },
  postRequest: (_, body) async => {'data': body},
);

JourneyDashboard _dashboard({
  String? journeyId = 'journey-recommendation',
  String? journeyType = 'PREGNANCY',
  String? status = 'ACTIVE_PREGNANCY',
}) => JourneyDashboard(
  journeyId: journeyId,
  journeyType: journeyType,
  status: status,
  pregnancyWeek: journeyType == 'PREGNANCY' ? 12 : null,
);

RecommendationContentResponse _response(String id, String title) =>
    RecommendationContentResponse(
      stage: 'PREGNANCY',
      pregnancyWeek: 12,
      weekEligibilityMode: 'BOUNDED_AND_STAGE_WIDE',
      profileStatus: RecommendationProfileStatus.active,
      selectionMode: 'TARGETED_ONLY',
      coverageStatus: 'COMPLETE',
      fallbackUsed: false,
      items: [
        RecommendationContentItem(
          rank: 1,
          selectionType: RecommendationSelectionType.targeted,
          reasonLabel: 'Selected for your current care context',
          id: id,
          title: title,
          summary: 'Actionable and approved guidance.',
          stage: 'PREGNANCY',
        ),
      ],
    );

Widget _host({
  required Future<RecommendationContentResponse> Function() loader,
  JourneyDashboard? dashboard,
}) {
  return MaterialApp(
    home: MotherHomeScreen(
      todayTaskService: _todayTaskService(),
      dashboardLoader: () async => dashboard ?? _dashboard(),
      reminderLoader: () async => const [],
      recommendationLoader: loader,
    ),
  );
}

void main() {
  testWidgets('renders independent recommendation loading and cards', (
    tester,
  ) async {
    final pending = Completer<RecommendationContentResponse>();
    await tester.pumpWidget(_host(loader: () => pending.future));

    await tester.dragUntilVisible(
      find.byKey(const Key('mother-home-recommendation-loading')),
      find.byType(CustomScrollView),
      const Offset(0, -300),
    );
    expect(
      find.byKey(const Key('mother-home-recommendation-loading')),
      findsOneWidget,
    );
    pending.complete(_response('article-1', 'An toàn trong tuần 12'));
    await tester.pumpAndSettle();

    await tester.dragUntilVisible(
      find.byKey(const Key('mother-home-recommendation-card-article-1')),
      find.byType(CustomScrollView),
      const Offset(0, -300),
    );
    expect(find.text('An toàn trong tuần 12'), findsOneWidget);
    expect(
      find.byKey(const Key('mother-home-recommendation-card-article-1')),
      findsOneWidget,
    );
  });

  testWidgets('shows retry state and recovers after recommendation failure', (
    tester,
  ) async {
    var calls = 0;
    await tester.pumpWidget(
      _host(
        loader: () async {
          calls++;
          if (calls == 1) throw StateError('temporary');
          return _response('article-2', 'Dinh dưỡng dễ áp dụng');
        },
      ),
    );
    await tester.pumpAndSettle();

    await tester.dragUntilVisible(
      find.byKey(const Key('mother-home-recommendation-error')),
      find.byType(CustomScrollView),
      const Offset(0, -300),
    );
    expect(
      find.byKey(const Key('mother-home-recommendation-error')),
      findsOneWidget,
    );
    await tester.tap(find.byKey(const Key('mother-home-recommendation-retry')));
    await tester.pumpAndSettle();

    expect(calls, 2);
    await tester.dragUntilVisible(
      find.text('Dinh dưỡng dễ áp dụng'),
      find.byType(CustomScrollView),
      const Offset(0, -300),
    );
    expect(find.text('Dinh dưỡng dễ áp dụng'), findsOneWidget);
  });

  testWidgets('discards an older overlapping recommendation response', (
    tester,
  ) async {
    final requests = <Completer<RecommendationContentResponse>>[];
    await tester.pumpWidget(
      _host(
        loader: () {
          final request = Completer<RecommendationContentResponse>();
          requests.add(request);
          return request.future;
        },
      ),
    );
    await tester.pump();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();

    requests[1].complete(_response('article-new', 'Mới nhất'));
    await tester.pumpAndSettle();
    requests[0].complete(_response('article-old', 'Cũ'));
    await tester.pumpAndSettle();

    await tester.dragUntilVisible(
      find.text('Mới nhất'),
      find.byType(CustomScrollView),
      const Offset(0, -300),
    );
    expect(find.text('Mới nhất'), findsOneWidget);
    expect(find.text('Cũ'), findsNothing);
  });

  testWidgets('opens the approved content detail from a recommendation card', (
    tester,
  ) async {
    await tester.pumpWidget(
      _host(loader: () async => _response('article-detail', 'Mở bài viết')),
    );
    await tester.pumpAndSettle();

    await tester.dragUntilVisible(
      find.byKey(const Key('mother-home-recommendation-card-article-detail')),
      find.byType(CustomScrollView),
      const Offset(0, -300),
    );
    await tester.tap(
      find.byKey(const Key('mother-home-recommendation-card-article-detail')),
    );
    await tester.pumpAndSettle();

    expect(find.byType(VerifiedContentDetailScreen), findsOneWidget);
  });

  testWidgets(
    'does not call recommendations without an active maternal journey',
    (tester) async {
      var calls = 0;
      await tester.pumpWidget(
        _host(
          dashboard: _dashboard(
            journeyId: null,
            journeyType: null,
            status: 'NO_JOURNEY',
          ),
          loader: () async {
            calls++;
            return _response('should-not-render', 'Should not render');
          },
        ),
      );
      await tester.pumpAndSettle();

      expect(calls, 0);
      expect(
        find.byKey(const Key('mother-home-recommendation-loading')),
        findsNothing,
      );
      expect(find.text('Should not render'), findsNothing);
    },
  );

  testWidgets(
    'does not call recommendations for an unsupported journey stage',
    (tester) async {
      var calls = 0;
      await tester.pumpWidget(
        _host(
          dashboard: _dashboard(journeyType: 'BABY_CARE', status: 'BABY_CARE'),
          loader: () async {
            calls++;
            return _response('should-not-render', 'Should not render');
          },
        ),
      );
      await tester.pumpAndSettle();

      expect(calls, 0);
      expect(find.text('Should not render'), findsNothing);
    },
  );
}
