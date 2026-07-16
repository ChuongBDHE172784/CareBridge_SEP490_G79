import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../models/growth_measurement_model.dart';

enum GrowthTrendMetric { automatic, weight, height, headCircumference }

class GrowthTrendChart extends StatelessWidget {
  static const _accentColor = Color(0xFFC98C7B);
  static const _surfaceColor = Color(0xFFF6F1EC);
  static const _borderColor = Color(0xFFE7E1DD);
  static const _textColor = Color(0xFF5A463F);

  final List<GrowthMeasurement> measurements;
  final GrowthTrendMetric metric;

  const GrowthTrendChart({
    super.key,
    required this.measurements,
    this.metric = GrowthTrendMetric.automatic,
  });

  @override
  Widget build(BuildContext context) {
    final resolvedMetric = _resolveMetric();
    final presentation = _MetricPresentation.forMetric(resolvedMetric);
    final points = _buildPoints(presentation);

    return Column(
      key: const Key('growth-trend-chart'),
      children: [
        if (points.isEmpty)
          _EmptyTrendChart(presentation: presentation)
        else
          Semantics(
            container: true,
            label:
                'Biểu đồ ${presentation.label.toLowerCase()} gồm '
                '${points.length} điểm dữ liệu',
            child: Container(
              height: 192,
              width: double.infinity,
              decoration: BoxDecoration(
                color: _surfaceColor,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: _borderColor),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x0F5A463F),
                    blurRadius: 16,
                    offset: Offset(0, 6),
                  ),
                ],
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(15),
                child: RepaintBoundary(
                  key: ValueKey('growth-trend-chart-points-${points.length}'),
                  child: CustomPaint(
                    key: const Key('growth-trend-chart-canvas'),
                    painter: GrowthTrendChartPainter(points),
                    size: Size.infinite,
                  ),
                ),
              ),
            ),
          ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              key: Key('growth-trend-chart-metric-${presentation.keyName}'),
              width: 12,
              height: 12,
              decoration: const BoxDecoration(
                color: _accentColor,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 8),
            Flexible(
              child: Text(
                '${presentation.label} (${presentation.unit})',
                key: const Key('growth-trend-chart-metric-label'),
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: _textColor,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  GrowthTrendMetric _resolveMetric() {
    if (metric != GrowthTrendMetric.automatic) return metric;

    for (final candidate in const [
      GrowthTrendMetric.weight,
      GrowthTrendMetric.height,
      GrowthTrendMetric.headCircumference,
    ]) {
      final presentation = _MetricPresentation.forMetric(candidate);
      if (measurements.any((measurement) {
        final value = presentation.valueOf(measurement);
        return value != null && value.isFinite;
      })) {
        return candidate;
      }
    }

    return GrowthTrendMetric.weight;
  }

  List<GrowthTrendPoint> _buildPoints(_MetricPresentation presentation) {
    final chronological = List<GrowthMeasurement>.of(measurements)
      ..sort((a, b) => a.measuredAt.compareTo(b.measuredAt));

    return List<GrowthTrendPoint>.unmodifiable(
      chronological.expand((measurement) {
        final value = presentation.valueOf(measurement);
        if (value == null || !value.isFinite) {
          return const <GrowthTrendPoint>[];
        }
        return [
          GrowthTrendPoint(measuredAt: measurement.measuredAt, value: value),
        ];
      }),
    );
  }
}

class _EmptyTrendChart extends StatelessWidget {
  final _MetricPresentation presentation;

  const _EmptyTrendChart({required this.presentation});

  @override
  Widget build(BuildContext context) {
    return Container(
      key: const Key('growth-trend-chart-empty'),
      height: 192,
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: const Color(0xFFF6F1EC),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE7E1DD)),
      ),
      child: Center(
        child: Text(
          'Chưa có dữ liệu ${presentation.label.toLowerCase()} '
          'để hiển thị biểu đồ.',
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: Color(0xFF9C857C),
          ),
        ),
      ),
    );
  }
}

class _MetricPresentation {
  final String keyName;
  final String label;
  final String unit;
  final double? Function(GrowthMeasurement measurement) valueOf;

  const _MetricPresentation({
    required this.keyName,
    required this.label,
    required this.unit,
    required this.valueOf,
  });

  factory _MetricPresentation.forMetric(GrowthTrendMetric metric) {
    return switch (metric) {
      GrowthTrendMetric.weight => _MetricPresentation(
        keyName: 'weight',
        label: 'Cân nặng',
        unit: 'kg',
        valueOf: (measurement) => measurement.weightKg,
      ),
      GrowthTrendMetric.height => _MetricPresentation(
        keyName: 'height',
        label: 'Chiều cao',
        unit: 'cm',
        valueOf: (measurement) => measurement.heightCm,
      ),
      GrowthTrendMetric.headCircumference => _MetricPresentation(
        keyName: 'head-circumference',
        label: 'Vòng đầu',
        unit: 'cm',
        valueOf: (measurement) => measurement.headCircumferenceCm,
      ),
      GrowthTrendMetric.automatic => throw StateError(
        'Automatic metric must be resolved before presentation.',
      ),
    };
  }
}

@immutable
class GrowthTrendPoint {
  final DateTime measuredAt;
  final double value;

  const GrowthTrendPoint({required this.measuredAt, required this.value});
}

class GrowthTrendChartPainter extends CustomPainter {
  static const _accentColor = Color(0xFFC98C7B);
  static const _gridColor = Color(0x24A88E84);

  final List<GrowthTrendPoint> points;

  GrowthTrendChartPainter(List<GrowthTrendPoint> points)
    : points = List<GrowthTrendPoint>.unmodifiable(points);

  @override
  void paint(Canvas canvas, Size size) {
    if (points.isEmpty || size.isEmpty) return;

    const horizontalPadding = 18.0;
    const verticalPadding = 20.0;
    final plotRect = Rect.fromLTRB(
      horizontalPadding,
      verticalPadding,
      math.max(horizontalPadding, size.width - horizontalPadding),
      math.max(verticalPadding, size.height - verticalPadding),
    );

    final gridPaint = Paint()
      ..color = _gridColor
      ..strokeWidth = 1;
    for (var index = 0; index < 5; index++) {
      final y = plotRect.top + (plotRect.height * index / 4);
      canvas.drawLine(
        Offset(plotRect.left, y),
        Offset(plotRect.right, y),
        gridPaint,
      );
    }

    final values = points.map((point) => point.value).toList(growable: false);
    final minimum = values.reduce(math.min);
    final maximum = values.reduce(math.max);
    final rawRange = maximum - minimum;
    final displayRange = rawRange == 0 ? 1.0 : rawRange * 1.3;
    final displayMinimum = rawRange == 0
        ? minimum - 0.5
        : minimum - rawRange * 0.15;

    final firstTime = points.first.measuredAt.millisecondsSinceEpoch;
    final lastTime = points.last.measuredAt.millisecondsSinceEpoch;
    final timeRange = lastTime - firstTime;
    final offsets = List<Offset>.generate(points.length, (index) {
      final point = points[index];
      final x = points.length == 1 || timeRange == 0
          ? plotRect.center.dx
          : plotRect.left +
                plotRect.width *
                    (point.measuredAt.millisecondsSinceEpoch - firstTime) /
                    timeRange;
      final normalized = (point.value - displayMinimum) / displayRange;
      final y = plotRect.bottom - normalized * plotRect.height;
      return Offset(x, y);
    });

    final linePath = Path()..moveTo(offsets.first.dx, offsets.first.dy);
    for (var index = 1; index < offsets.length; index++) {
      linePath.lineTo(offsets[index].dx, offsets[index].dy);
    }

    final linePaint = Paint()
      ..color = _accentColor
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;
    canvas.drawPath(linePath, linePaint);

    final pointFillPaint = Paint()
      ..color = const Color(0xFF845143)
      ..style = PaintingStyle.fill;
    final pointBorderPaint = Paint()
      ..color = const Color(0xFFF6F1EC)
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke;
    for (final offset in offsets) {
      canvas.drawCircle(offset, offsets.length == 1 ? 6 : 4.5, pointFillPaint);
      canvas.drawCircle(
        offset,
        offsets.length == 1 ? 6 : 4.5,
        pointBorderPaint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant GrowthTrendChartPainter oldDelegate) {
    if (points.length != oldDelegate.points.length) return true;
    for (var index = 0; index < points.length; index++) {
      if (points[index].measuredAt != oldDelegate.points[index].measuredAt ||
          points[index].value != oldDelegate.points[index].value) {
        return true;
      }
    }
    return false;
  }
}
