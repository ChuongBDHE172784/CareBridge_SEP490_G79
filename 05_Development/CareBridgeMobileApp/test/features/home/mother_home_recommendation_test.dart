import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/community/models/content_model.dart';
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

BabyProfile _baby({
  String id = 'baby-1',
  String nickname = 'Bé Miu',
  DateTime? birthDate,
}) => BabyProfile(
  id: id,
  nickname: nickname,
  gender: BabyGender.female,
  birthDate: birthDate ?? DateTime.now().subtract(const Duration(days: 3)),
  isActive: true,
);

ContentListItem _babyArticle({
  String id = 'baby-article-1',
  String title = 'Chăm sóc trẻ sơ sinh: Những điều cần biết',
  String summary = 'Hướng dẫn chăm sóc trẻ sơ sinh những ngày đầu.',
  String stage = 'POSTPARTUM',
}) => ContentListItem(
  id: id,
  type: 'ARTICLE',
  title: title,
  summary: summary,
  stage: stage,
  topicId: 'baby-care-topic',
);

RecommendationContentResponse _response(
  String id,
  String title, {
  RecommendationProfileStatus profileStatus =
      RecommendationProfileStatus.active,
}) => RecommendationContentResponse(
  stage: 'PREGNANCY',
  pregnancyWeek: 12,
  weekEligibilityMode: 'BOUNDED_AND_STAGE_WIDE',
  profileStatus: profileStatus,
  selectionMode: 'TARGETED_ONLY',
  coverageStatus: 'COMPLETE',
  fallbackUsed: false,
  items: [
    RecommendationContentItem(
      rank: 1,
      selectionType: RecommendationSelectionType.targeted,
      reasonLabel: 'Phù hợp với ngữ cảnh chăm sóc của bạn',
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
  Future<List<BabyProfile>> Function()? babyLoader,
  Future<List<ContentListItem>> Function()? babyContentLoader,
}) {
  return MaterialApp(
    home: MotherHomeScreen(
      todayTaskService: _todayTaskService(),
      dashboardLoader: () async => dashboard ?? _dashboard(),
      reminderLoader: () async => const [],
      recommendationLoader: loader,
      babyLoader: babyLoader ?? () async => const [],
      babyContentLoader: babyContentLoader,
    ),
  );
}

void main() {
  testWidgets(
    'keeps personalization invitation visible after an empty profile is declined',
    (tester) async {
      await tester.pumpWidget(
        _host(
          loader: () async => _response(
            'fallback-article',
            'Nội dung theo giai đoạn',
            profileStatus: RecommendationProfileStatus.declined,
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.dragUntilVisible(
        find.byKey(const Key('mother-home-recommendation-personalize')),
        find.byType(CustomScrollView),
        const Offset(0, -300),
      );

      expect(
        find.byKey(const Key('mother-home-recommendation-personalize')),
        findsOneWidget,
      );
      expect(find.text('Cá nhân hóa nội dung'), findsOneWidget);
    },
  );

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
    // A short viewport parks the recommendation block under the floating emergency-map button,
    // so the tap lands on the FAB instead of the retry control.
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
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
    await tester.ensureVisible(
      find.byKey(const Key('mother-home-recommendation-retry')),
    );
    await tester.pumpAndSettle();
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
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      _host(loader: () async => _response('article-detail', 'Mở bài viết')),
    );
    await tester.pumpAndSettle();

    await tester.dragUntilVisible(
      find.byKey(const Key('mother-home-recommendation-card-article-detail')),
      find.byType(CustomScrollView),
      const Offset(0, -300),
    );
    await tester.ensureVisible(
      find.byKey(const Key('mother-home-recommendation-card-article-detail')),
    );
    await tester.pumpAndSettle();
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

  testWidgets(
    'renders unified prioritized recommendations for mother and baby together without tab switcher',
    (tester) async {
      tester.view.physicalSize = const Size(800, 1800);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final baby = _baby(nickname: 'Bé Miu');
      final babyArticle = _babyArticle(
        id: 'baby-art-1',
        title: 'Chăm sóc trẻ sơ sinh những ngày đầu',
      );

      await tester.pumpWidget(
        _host(
          loader: () async => _response('maternal-art-1', 'Dinh dưỡng tuần 12'),
          babyLoader: () async => [baby],
          babyContentLoader: () async => [babyArticle],
        ),
      );
      await tester.pumpAndSettle();

      await tester.dragUntilVisible(
        find.text('Bài viết gợi ý cho mẹ và bé'),
        find.byType(CustomScrollView),
        const Offset(0, -300),
      );

      expect(find.text('Bài viết gợi ý cho mẹ và bé'), findsOneWidget);
      expect(
        find.text('Nội dung chăm sóc phù hợp được sắp xếp theo mức độ ưu tiên'),
        findsOneWidget,
      );

      // Verify no tab buttons exist
      expect(find.text('Mẹ (Tuần 12)'), findsNothing);

      // Verify both articles are rendered together in the unified feed
      expect(find.text('Chăm sóc trẻ sơ sinh những ngày đầu'), findsOneWidget);
      expect(find.text('Phù hợp cho Bé Miu (3 ngày tuổi)'), findsOneWidget);
      expect(find.text('Dinh dưỡng tuần 12'), findsOneWidget);

      // Verify tapping baby card navigates to VerifiedContentDetailScreen
      await tester.tap(
        find.byKey(const Key('mother-home-recommendation-card-baby-art-1')),
      );
      await tester.pumpAndSettle();
      expect(find.byType(VerifiedContentDetailScreen), findsOneWidget);
    },
  );

  testWidgets(
    'displays baby recommendations directly when mother has baby profile even without active maternal journey',
    (tester) async {
      final baby = _baby(nickname: 'Bé Miu');
      final article = _babyArticle(
        id: 'baby-art-standalone',
        title: 'Hướng dẫn tắm cho trẻ sơ sinh an toàn',
      );

      await tester.pumpWidget(
        _host(
          dashboard: _dashboard(
            journeyId: null,
            journeyType: null,
            status: 'NO_JOURNEY',
          ),
          loader: () async => _response('should-not-load', 'Should not load'),
          babyLoader: () async => [baby],
          babyContentLoader: () async => [article],
        ),
      );
      await tester.pumpAndSettle();

      await tester.dragUntilVisible(
        find.text('Hướng dẫn tắm cho trẻ sơ sinh an toàn'),
        find.byType(CustomScrollView),
        const Offset(0, -300),
      );
      expect(find.text('Hướng dẫn tắm cho trẻ sơ sinh an toàn'), findsOneWidget);
      expect(find.text('Phù hợp cho Bé Miu (3 ngày tuổi)'), findsOneWidget);
    },
  );
}
