import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';
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
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Center(
          child: SizedBox(
            width: 360,
            child: GrowthTrendChart(measurements: measurements, metric: metric),
          ),
        ),
      ),
    ),
  );
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
    expect(find.text('Cân nặng (kg)'), findsOneWidget);
    expect(find.text('Chart Visualization Area'), findsNothing);
    expect(find.textContaining('WHO'), findsNothing);
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
    expect(find.text('Chiều cao (cm)'), findsOneWidget);

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
    expect(find.text('Vòng đầu (cm)'), findsOneWidget);
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
}
