import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';
import 'package:untitled/features/healthRecords/screens/growth_measurement_history_screen.dart';
import 'package:untitled/features/healthRecords/widgets/growth_trend_chart.dart';

GrowthMeasurement _measurement(
  String id,
  DateTime measuredAt, {
  double? weightKg,
  double? heightCm,
  double? headCircumferenceCm,
}) {
  return GrowthMeasurement(
    id: id,
    measuredAt: measuredAt,
    weightKg: weightKg,
    heightCm: heightCm,
    headCircumferenceCm: headCircumferenceCm,
    recordedBy: 'test-user',
  );
}

Future<void> _pumpChart(
  WidgetTester tester, {
  required List<GrowthMeasurement> measurements,
  GrowthTrendMetric metric = GrowthTrendMetric.automatic,
  DateTime? birthDate,
  BabyGender? gender,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Center(
          child: SizedBox(
            width: 360,
            child: GrowthTrendChart(
              measurements: measurements,
              metric: metric,
              birthDate: birthDate,
              gender: gender,
            ),
          ),
        ),
      ),
    ),
  );
}

Future<void> _pumpHistoryScreen(
  WidgetTester tester, {
  required List<GrowthMeasurement> measurements,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: GrowthMeasurementHistoryScreen(
        babyId: 'baby-1',
        loadAvatarImage: false,
        historyLoader: (_) async => measurements,
        profileLoader: (_) async => BabyProfile(
          id: 'baby-1',
          nickname: 'Bé',
          birthDate: DateTime(2024, 1, 15),
          gender: BabyGender.female,
          isActive: true,
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('automatic metric prefers weight and renders a real chart', (
    tester,
  ) async {
    await _pumpChart(
      tester,
      measurements: [
        _measurement(
          'newer',
          DateTime(2026, 3, 1),
          weightKg: 5.1,
          heightCm: 57,
        ),
        _measurement(
          'older',
          DateTime(2026, 2, 1),
          weightKg: 4.4,
          heightCm: 53,
        ),
      ],
    );

    expect(find.byKey(const Key('growth-trend-chart')), findsOneWidget);
    expect(
      find.byKey(const Key('growth-trend-chart-metric-weight')),
      findsOneWidget,
    );
    expect(
      find.byKey(const ValueKey('growth-trend-chart-points-2')),
      findsOneWidget,
    );
    expect(find.text('Số đo của bé: Cân nặng (kg)'), findsOneWidget);
    expect(find.text('Chart Visualization Area'), findsNothing);
    expect(
      find.byKey(const Key('growth-trend-chart-who-unavailable')),
      findsOneWidget,
    );
    expect(
      find.byKey(const Key('growth-trend-chart-who-legend')),
      findsNothing,
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('automatic metric falls back to height then head circumference', (
    tester,
  ) async {
    await _pumpChart(
      tester,
      measurements: [
        _measurement('height', DateTime(2026, 2, 1), heightCm: 54),
        _measurement('head', DateTime(2026, 3, 1), headCircumferenceCm: 37),
      ],
    );

    expect(
      find.byKey(const Key('growth-trend-chart-metric-height')),
      findsOneWidget,
    );
    expect(find.text('Số đo của bé: Chiều cao (cm)'), findsOneWidget);

    await _pumpChart(
      tester,
      measurements: [
        _measurement(
          'head-only',
          DateTime(2026, 3, 1),
          headCircumferenceCm: 37,
        ),
      ],
    );

    expect(
      find.byKey(const Key('growth-trend-chart-metric-head-circumference')),
      findsOneWidget,
    );
    expect(find.text('Số đo của bé: Vòng đầu (cm)'), findsOneWidget);
  });

  testWidgets('non-finite values are ignored during selection and painting', (
    tester,
  ) async {
    await _pumpChart(
      tester,
      measurements: [
        _measurement(
          'invalid-weight',
          DateTime(2026, 2, 1),
          weightKg: double.nan,
          heightCm: 54,
        ),
      ],
    );

    expect(
      find.byKey(const Key('growth-trend-chart-metric-height')),
      findsOneWidget,
    );

    await _pumpChart(
      tester,
      measurements: [
        _measurement(
          'infinite',
          DateTime(2026, 2, 1),
          weightKg: double.infinity,
        ),
        _measurement('valid', DateTime(2026, 3, 1), weightKg: 4.2),
      ],
      metric: GrowthTrendMetric.weight,
    );

    final customPaint = tester.widget<CustomPaint>(
      find.byKey(const Key('growth-trend-chart-canvas')),
    );
    final painter = customPaint.painter! as GrowthTrendChartPainter;
    expect(painter.points.map((point) => point.value), [4.2]);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'explicit metric ignores nulls, sorts a copy, and preserves input order',
    (tester) async {
      final measurements = [
        _measurement('newest', DateTime(2026, 4, 1), heightCm: 61),
        _measurement('oldest', DateTime(2026, 2, 1), heightCm: 53),
        _measurement('middle-null', DateTime(2026, 3, 1)),
      ];

      await _pumpChart(
        tester,
        measurements: measurements,
        metric: GrowthTrendMetric.height,
      );

      final customPaint = tester.widget<CustomPaint>(
        find.byKey(const Key('growth-trend-chart-canvas')),
      );
      final painter = customPaint.painter! as GrowthTrendChartPainter;
      expect(painter.points.map((point) => point.measuredAt), [
        DateTime(2026, 2, 1),
        DateTime(2026, 4, 1),
      ]);
      expect(painter.points.map((point) => point.value), [53, 61]);
      expect(measurements.map((measurement) => measurement.id), [
        'newest',
        'oldest',
        'middle-null',
      ]);
    },
  );

  testWidgets(
    'selected metric with no usable values shows accessible empty state',
    (tester) async {
      await _pumpChart(
        tester,
        measurements: [
          _measurement('height-only', DateTime(2026, 2, 1), heightCm: 54),
        ],
        metric: GrowthTrendMetric.weight,
      );

      expect(find.byKey(const Key('growth-trend-chart-empty')), findsOneWidget);
      expect(
        find.text('Chưa có dữ liệu cân nặng để hiển thị biểu đồ.'),
        findsOneWidget,
      );
      expect(find.byKey(const Key('growth-trend-chart-canvas')), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('one usable point renders centered without an exception', (
    tester,
  ) async {
    await _pumpChart(
      tester,
      measurements: [
        _measurement('single', DateTime(2026, 2, 1), weightKg: 4.2),
      ],
      metric: GrowthTrendMetric.weight,
    );

    expect(
      find.byKey(const ValueKey('growth-trend-chart-points-1')),
      findsOneWidget,
    );
    final customPaint = tester.widget<CustomPaint>(
      find.byKey(const Key('growth-trend-chart-canvas')),
    );
    final painter = customPaint.painter! as GrowthTrendChartPainter;
    expect(painter.points, hasLength(1));
    expect(tester.takeException(), isNull);
  });

  testWidgets('multiple same-date points avoid divide-by-zero failures', (
    tester,
  ) async {
    final measuredAt = DateTime(2026, 2, 1);
    await _pumpChart(
      tester,
      measurements: [
        _measurement('one', measuredAt, headCircumferenceCm: 35),
        _measurement('two', measuredAt, headCircumferenceCm: 35.5),
        _measurement('three', measuredAt, headCircumferenceCm: 36),
      ],
      metric: GrowthTrendMetric.headCircumference,
    );

    expect(
      find.byKey(const ValueKey('growth-trend-chart-points-3')),
      findsOneWidget,
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('renders the correct WHO P50 series and combined legend', (
    tester,
  ) async {
    await _pumpChart(
      tester,
      measurements: [
        _measurement('month-12', DateTime(2025, 1, 15), weightKg: 9.1),
      ],
      metric: GrowthTrendMetric.weight,
      birthDate: DateTime(2024, 1, 15),
      gender: BabyGender.female,
    );

    final customPaint = tester.widget<CustomPaint>(
      find.byKey(const Key('growth-trend-chart-canvas')),
    );
    final painter = customPaint.painter! as GrowthTrendChartPainter;
    expect(painter.whoPoints, hasLength(2));
    expect(
      painter.whoPoints.any(
        (point) =>
            point.measuredAt == DateTime(2025, 1, 15) && point.value == 8.9,
      ),
      isTrue,
    );
    expect(find.text('Số đo của bé: Cân nặng (kg)'), findsOneWidget);
    expect(find.text('WHO P50 nội suy (tham khảo)'), findsOneWidget);
    expect(find.textContaining('không dùng để chẩn đoán'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('does not infer WHO sex when gender is unknown', (tester) async {
    await _pumpChart(
      tester,
      measurements: [
        _measurement('month-12', DateTime(2025, 1, 15), heightCm: 73),
      ],
      metric: GrowthTrendMetric.height,
      birthDate: DateTime(2024, 1, 15),
      gender: BabyGender.unknown,
    );

    final customPaint = tester.widget<CustomPaint>(
      find.byKey(const Key('growth-trend-chart-canvas')),
    );
    final painter = customPaint.painter! as GrowthTrendChartPainter;
    expect(painter.points, hasLength(1));
    expect(painter.whoPoints, isEmpty);
    expect(find.text('WHO P50 nội suy (tham khảo)'), findsNothing);
    expect(find.textContaining('Cần cập nhật giới tính'), findsOneWidget);
  });

  testWidgets('does not show WHO when every measurement is out of range', (
    tester,
  ) async {
    await _pumpChart(
      tester,
      measurements: [
        _measurement('month-25', DateTime(2026, 2, 16), weightKg: 12),
      ],
      metric: GrowthTrendMetric.weight,
      birthDate: DateTime(2024, 1, 15),
      gender: BabyGender.male,
    );

    final customPaint = tester.widget<CustomPaint>(
      find.byKey(const Key('growth-trend-chart-canvas')),
    );
    final painter = customPaint.painter! as GrowthTrendChartPainter;
    expect(painter.whoPoints, isEmpty);
    expect(find.textContaining('0–24 tháng'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('height comparison clearly identifies WHO recumbent length', (
    tester,
  ) async {
    await _pumpChart(
      tester,
      measurements: [
        _measurement('month-12', DateTime(2025, 1, 15), heightCm: 74),
      ],
      metric: GrowthTrendMetric.height,
      birthDate: DateTime(2024, 1, 15),
      gender: BabyGender.female,
    );

    expect(find.textContaining('WHO dùng chiều dài nằm'), findsOneWidget);
    expect(find.textContaining('không dùng để chẩn đoán'), findsOneWidget);
  });

  testWidgets('profile failure does not block measurement history', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: GrowthMeasurementHistoryScreen(
          babyId: 'baby-1',
          loadAvatarImage: false,
          historyLoader: (_) async => [
            _measurement('measurement', DateTime(2025, 1, 15), weightKg: 8.9),
          ],
          profileLoader: (_) async => throw Exception('profile unavailable'),
        ),
      ),
    );
    await tester.pump();
    await tester.pump();

    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(find.byKey(const Key('growth-trend-chart-canvas')), findsOneWidget);
    expect(find.textContaining('Không thể tải thông tin bé'), findsOneWidget);
  });

  testWidgets('chart accepts a presentation-only custom plot height', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: GrowthTrendChart(
          measurements: [
            _measurement('one', DateTime(2025, 1, 15), weightKg: 8.9),
          ],
          chartHeight: 360,
        ),
      ),
    );

    expect(
      tester.getSize(find.byKey(const Key('growth-trend-chart-plot'))).height,
      360,
    );
  });

  testWidgets('metric filters are contained inside the growth chart card', (
    tester,
  ) async {
    await _pumpHistoryScreen(
      tester,
      measurements: [
        _measurement('one', DateTime(2025, 1, 15), weightKg: 8.9, heightCm: 74),
      ],
    );

    final chartCard = find.byKey(const Key('growth-chart-card'));
    final filters = find.byKey(const Key('growth-chart-filters'));
    expect(chartCard, findsOneWidget);
    expect(filters, findsOneWidget);
    expect(find.descendant(of: chartCard, matching: filters), findsOneWidget);
    expect(
      find.descendant(of: filters, matching: find.byType(ChoiceChip)),
      findsNWidgets(3),
    );

    await tester.tap(find.text('Chiều cao'));
    await tester.pumpAndSettle();
    expect(
      find.byKey(const Key('growth-trend-chart-metric-height')),
      findsOneWidget,
    );
  });

  testWidgets('chart card remains usable on a narrow screen', (tester) async {
    tester.view.physicalSize = const Size(320, 568);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await _pumpHistoryScreen(tester, measurements: const []);

    expect(find.byKey(const Key('growth-chart-card')), findsOneWidget);
    expect(
      find.byKey(const Key('growth-chart-fullscreen-button')),
      findsOneWidget,
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('short history sizes naturally without an artificial block', (
    tester,
  ) async {
    await _pumpHistoryScreen(
      tester,
      measurements: [_measurement('one', DateTime(2025, 1, 15), weightKg: 8.9)],
    );
    await tester.scrollUntilVisible(
      find.byKey(const Key('growth-history-record-list')),
      200,
      scrollable: find.byType(Scrollable).first,
    );

    expect(
      tester
          .getSize(find.byKey(const Key('growth-history-record-list')))
          .height,
      lessThan(480),
    );
  });

  testWidgets('long history is bounded and scrolls independently', (
    tester,
  ) async {
    final measurements = List.generate(
      12,
      (index) => _measurement(
        'record-$index',
        DateTime(2025, 1, 15).add(Duration(days: index * 7)),
        weightKg: 8.9 + index / 10,
      ),
    );
    await _pumpHistoryScreen(tester, measurements: measurements);
    final listFinder = find.byKey(const Key('growth-history-record-list'));
    await tester.scrollUntilVisible(
      listFinder,
      200,
      scrollable: find.byType(Scrollable).first,
    );

    final list = tester.widget<ListView>(listFinder);
    expect(tester.getSize(listFinder).height, lessThanOrEqualTo(480));
    expect(list.controller, isNotNull);
    expect(list.controller!.offset, 0);

    await tester.dragFrom(
      tester.getTopLeft(listFinder) + const Offset(100, 100),
      const Offset(0, -300),
    );
    await tester.pumpAndSettle();

    expect(list.controller!.offset, greaterThan(0));
    expect(find.byKey(const Key('growth-history-scrollbar')), findsOneWidget);

    list.controller!.jumpTo(list.controller!.position.maxScrollExtent);
    await tester.pumpAndSettle();
    expect(
      find.byKey(const ValueKey('growth-history-record-record-11')),
      findsOneWidget,
    );
  });

  testWidgets('fullscreen preserves metric and WHO after close', (
    tester,
  ) async {
    await _pumpHistoryScreen(
      tester,
      measurements: [
        _measurement(
          'month-12',
          DateTime(2025, 1, 15),
          weightKg: 8.9,
          heightCm: 74,
        ),
      ],
    );

    await tester.tap(find.byKey(const Key('growth-chart-fullscreen-button')));
    await tester.pumpAndSettle();
    final fullscreen = find.byKey(const Key('growth-chart-fullscreen-screen'));
    expect(fullscreen, findsOneWidget);
    expect(
      find.descendant(
        of: fullscreen,
        matching: find.byKey(const Key('growth-trend-chart-who-legend')),
      ),
      findsOneWidget,
    );
    expect(
      tester
          .getSize(
            find.descendant(
              of: fullscreen,
              matching: find.byKey(const Key('growth-trend-chart-plot')),
            ),
          )
          .height,
      greaterThan(192),
    );

    await tester.tap(find.text('Chiều cao'));
    await tester.pumpAndSettle();
    expect(
      find.descendant(
        of: fullscreen,
        matching: find.byKey(const Key('growth-trend-chart-who-legend')),
      ),
      findsOneWidget,
    );
    await tester.tap(find.byKey(const Key('growth-chart-fullscreen-close')));
    await tester.pumpAndSettle();

    expect(fullscreen, findsNothing);
    expect(
      find.byKey(const Key('growth-trend-chart-metric-height')),
      findsOneWidget,
    );
    expect(
      find.byKey(const Key('growth-trend-chart-who-legend')),
      findsOneWidget,
    );
  });

  testWidgets('fullscreen waits for the latest profile before showing WHO', (
    tester,
  ) async {
    final profileCompleter = Completer<BabyProfile>();
    await tester.pumpWidget(
      MaterialApp(
        home: GrowthMeasurementHistoryScreen(
          babyId: 'baby-1',
          loadAvatarImage: false,
          historyLoader: (_) async => [
            _measurement('month-12', DateTime(2025, 1, 15), weightKg: 8.9),
          ],
          profileLoader: (_) => profileCompleter.future,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('growth-chart-fullscreen-button')));
    await tester.pump();
    expect(
      find.byKey(const Key('growth-chart-fullscreen-screen')),
      findsNothing,
    );

    profileCompleter.complete(
      BabyProfile(
        id: 'baby-1',
        nickname: 'Bé',
        birthDate: DateTime(2024, 1, 15),
        gender: BabyGender.female,
        isActive: true,
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('growth-chart-fullscreen-screen')),
      findsOneWidget,
    );
    expect(
      find.byKey(const Key('growth-trend-chart-who-legend')),
      findsOneWidget,
    );
  });

  testWidgets('system back closes fullscreen and keeps latest metric', (
    tester,
  ) async {
    await _pumpHistoryScreen(
      tester,
      measurements: [
        _measurement('month-12', DateTime(2025, 1, 15), heightCm: 74),
      ],
    );
    await tester.tap(find.byKey(const Key('growth-chart-fullscreen-button')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Chiều cao'));
    await tester.pumpAndSettle();

    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('growth-chart-fullscreen-screen')),
      findsNothing,
    );
    expect(
      find.byKey(const Key('growth-trend-chart-metric-height')),
      findsOneWidget,
    );
  });
}
