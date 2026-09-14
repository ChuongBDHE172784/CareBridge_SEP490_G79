import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/widgets/baby_growth_message_card.dart';

import 'baby_growth_test_factory.dart';

Widget makeCard({
  BabyGrowthShareData? data,
  GrowthMeasurementsLoader? loader,
  bool own = false,
}) => MaterialApp(
  home: Scaffold(
    body: SingleChildScrollView(
      child: BabyGrowthMessageCard(
        data: data ?? makeShareData(),
        isOwnMessage: own,
        loadMeasurements: loader ?? (_) async => makeGrowthMeasurements(),
      ),
    ),
  ),
);

void _useTallView(WidgetTester tester) {
  tester.view.physicalSize = const Size(400 * 2, 2400 * 2);
  tester.view.devicePixelRatio = 2.0;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

void main() {
  testWidgets(
    'SBG-TC-009 renders three charts over all measurements without clinical labels',
    (tester) async {
      _useTallView(tester);
      final loadedIds = <String>[];

      await tester.pumpWidget(
        makeCard(
          loader: (id) async {
            loadedIds.add(id);
            return makeGrowthMeasurements(includeRecent: true);
          },
        ),
      );
      await tester.pumpAndSettle();

      expect(loadedIds, [kTestBabyId]);
      expect(find.text('Phát triển của bé'), findsOneWidget);
      final weight = find.text('Xu hướng cân nặng');
      final height = find.text('Xu hướng chiều cao');
      final head = find.text('Xu hướng vòng đầu');
      expect(weight, findsOneWidget);
      expect(height, findsOneWidget);
      expect(head, findsOneWidget);
      expect(tester.getTopLeft(weight).dy, lessThan(tester.getTopLeft(height).dy));
      expect(tester.getTopLeft(height).dy, lessThan(tester.getTopLeft(head).dy));
      expect(find.text('3.2 kg – 9.8 kg'), findsOneWidget);
      expect(find.text('50.0 cm – 75.0 cm'), findsOneWidget);
      expect(find.text('34.0 cm – 45.0 cm'), findsOneWidget);
      expect(find.textContaining('4 lần đo'), findsOneWidget);
      expect(find.text('Bình thường'), findsNothing);
      expect(find.text('Cần lưu ý'), findsNothing);
      expect(find.text('Nguy hiểm'), findsNothing);
    },
  );

  testWidgets(
    'SBG-TC-010 history toggle sits below charts and lists newest first',
    (tester) async {
      _useTallView(tester);
      var loadCount = 0;

      await tester.pumpWidget(
        makeCard(
          loader: (_) async {
            loadCount++;
            return makeGrowthMeasurements();
          },
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('10/03/2025 · 2 tháng tuổi'), findsNothing);
      final toggle = find.text('Xem lịch sử đo (3)');
      expect(
        tester.getTopLeft(toggle).dy,
        greaterThan(
          tester.getBottomLeft(find.byKey(const Key('baby-growth-chart-head'))).dy,
        ),
      );

      await tester.tap(toggle);
      await tester.pumpAndSettle();

      final newest = find.text('10/03/2025 · 2 tháng tuổi');
      final middle = find.text('10/02/2025 · 1 tháng tuổi');
      final oldest = find.text('10/01/2025 · 0 ngày tuổi');
      expect(newest, findsOneWidget);
      expect(middle, findsOneWidget);
      expect(oldest, findsOneWidget);
      expect(tester.getTopLeft(newest).dy, lessThan(tester.getTopLeft(middle).dy));
      expect(tester.getTopLeft(middle).dy, lessThan(tester.getTopLeft(oldest).dy));
      expect(find.text('5.6 kg · 58.0 cm · 39.0 cm'), findsOneWidget);
      expect(find.text('4.5 kg · 54.5 cm · —'), findsOneWidget);
      expect(loadCount, 1);

      await tester.tap(find.text('Thu gọn lịch sử'));
      await tester.pumpAndSettle();

      expect(find.text('10/03/2025 · 2 tháng tuổi'), findsNothing);
    },
  );

  testWidgets('SBG-TC-011 falls back to snapshot when loading fails', (
    tester,
  ) async {
    _useTallView(tester);

    await tester.pumpWidget(
      makeCard(loader: (_) async => throw Exception('403')),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(
      find.text('Không thể tải dữ liệu tăng trưởng mới nhất'),
      findsOneWidget,
    );
    expect(find.textContaining('5.6 kg'), findsWidgets);
    expect(find.textContaining('403'), findsNothing);
  });

  testWidgets('SBG-TC-011 shows empty states when baby has no measurements', (
    tester,
  ) async {
    _useTallView(tester);

    await tester.pumpWidget(
      makeCard(
        data: makeShareData(count: 0, withLatest: false),
        loader: (_) async => const [],
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Chưa có dữ liệu cân nặng.'), findsOneWidget);
    expect(find.text('Chưa có dữ liệu chiều cao.'), findsOneWidget);
    expect(find.text('Chưa có dữ liệu vòng đầu.'), findsOneWidget);
    expect(find.textContaining('Xem lịch sử đo'), findsNothing);
  });
}
