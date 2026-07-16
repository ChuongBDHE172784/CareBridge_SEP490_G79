import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../baby/models/baby_model.dart';
import '../models/growth_measurement_model.dart';
import '../models/who_growth_standard.dart';

enum GrowthTrendMetric { automatic, weight, height, headCircumference }

class GrowthTrendChart extends StatelessWidget {
  static const _accentColor = Color(0xFFC98C7B);
  static const _surfaceColor = Color(0xFFF6F1EC);
  static const _borderColor = Color(0xFFE7E1DD);

  final List<GrowthMeasurement> measurements;
  final GrowthTrendMetric metric;
  final DateTime? birthDate;
  final BabyGender? gender;
  final bool profileLoadFailed;

  const GrowthTrendChart({
    super.key,
    required this.measurements,
    this.metric = GrowthTrendMetric.automatic,
    this.birthDate,
    this.gender,
    this.profileLoadFailed = false,
  });

  @override
  Widget build(BuildContext context) {
    final resolvedMetric = _resolveMetric();
    final presentation = _MetricPresentation.forMetric(resolvedMetric);
    final points = _buildPoints(presentation);
    final whoPoints = _buildWhoPoints(resolvedMetric, points);
    final whoUnavailableMessage = _whoUnavailableMessage(points, whoPoints);

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
                '${points.length} điểm dữ liệu'
                '${whoPoints.isEmpty ? '' : ' và đường WHO P50 tham khảo'}',
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
                    painter: GrowthTrendChartPainter(
                      points,
                      whoPoints: whoPoints,
                    ),
                    size: Size.infinite,
                  ),
                ),
              ),
            ),
          ),
        const SizedBox(height: 16),
        Wrap(
          alignment: WrapAlignment.center,
          spacing: 16,
          runSpacing: 10,
          children: [
            _LegendItem(
              key: Key('growth-trend-chart-metric-${presentation.keyName}'),
              color: _accentColor,
              label:
                  'Số đo của bé: ${presentation.label} (${presentation.unit})',
            ),
            if (whoPoints.isNotEmpty)
              const _LegendItem(
                key: Key('growth-trend-chart-who-legend'),
                color: Color(0xFF8E756B),
                label: 'WHO P50 nội suy (tham khảo)',
                dashed: true,
              ),
          ],
        ),
        if (whoPoints.isNotEmpty) ...[
          const SizedBox(height: 10),
          Text(
            resolvedMetric == GrowthTrendMetric.height
                ? 'WHO dùng chiều dài nằm cho trẻ dưới 24 tháng. Đường này chỉ mang tính tham khảo, không dùng để chẩn đoán.'
                : 'Đường WHO chỉ mang tính tham khảo, không dùng để chẩn đoán.',
            key: const Key('growth-trend-chart-who-disclaimer'),
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: Color(0xFF5A463F),
            ),
          ),
        ] else if (whoUnavailableMessage != null) ...[
          const SizedBox(height: 10),
          Text(
            whoUnavailableMessage,
            key: const Key('growth-trend-chart-who-unavailable'),
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: Color(0xFF5A463F),
            ),
          ),
        ],
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

  List<GrowthTrendPoint> _buildWhoPoints(
    GrowthTrendMetric resolvedMetric,
    List<GrowthTrendPoint> points,
  ) {
    final resolvedBirthDate = birthDate;
    final sex = switch (gender) {
      BabyGender.male => WhoGrowthSex.male,
      BabyGender.female => WhoGrowthSex.female,
      _ => null,
    };
    if (resolvedBirthDate == null || sex == null || points.isEmpty) {
      return const [];
    }

    final whoMetric = switch (resolvedMetric) {
      GrowthTrendMetric.weight => WhoGrowthMetric.weight,
      GrowthTrendMetric.height => WhoGrowthMetric.length,
      GrowthTrendMetric.headCircumference => WhoGrowthMetric.headCircumference,
      GrowthTrendMetric.automatic => null,
    };
    if (whoMetric == null) return const [];

    final comparablePoints = points
        .where((point) {
          return WhoGrowthStandard.medianAtDate(
                sex: sex,
                metric: whoMetric,
                birthDate: resolvedBirthDate,
                measuredAt: point.measuredAt,
              ) !=
              null;
        })
        .toList(growable: false);
    if (comparablePoints.isEmpty) return const [];

    final monthly = WhoGrowthStandard.monthlySeries(
      sex: sex,
      metric: whoMetric,
      birthDate: resolvedBirthDate,
    );
    final firstDate = comparablePoints.first.measuredAt;
    final lastDate = comparablePoints.last.measuredAt;

    if (firstDate == lastDate) {
      var lowerIndex = 0;
      for (var index = 0; index < monthly.length; index++) {
        if (!monthly[index].measuredAt.isAfter(firstDate)) lowerIndex = index;
      }
      var upperIndex = monthly.length - 1;
      for (var index = monthly.length - 1; index >= 0; index--) {
        if (!monthly[index].measuredAt.isBefore(firstDate)) upperIndex = index;
      }
      if (lowerIndex == upperIndex) {
        if (upperIndex < monthly.length - 1) {
          upperIndex++;
        } else if (lowerIndex > 0) {
          lowerIndex--;
        }
      }
      return [
        _toTrendPoint(monthly[lowerIndex]),
        if (upperIndex != lowerIndex) _toTrendPoint(monthly[upperIndex]),
      ];
    }

    final result = <GrowthTrendPoint>[];
    void addPoint(DateTime measuredAt, double value) {
      if (result.isNotEmpty && result.last.measuredAt == measuredAt) return;
      result.add(GrowthTrendPoint(measuredAt: measuredAt, value: value));
    }

    addPoint(
      firstDate,
      WhoGrowthStandard.medianAtDate(
        sex: sex,
        metric: whoMetric,
        birthDate: resolvedBirthDate,
        measuredAt: firstDate,
      )!,
    );
    for (final point in monthly) {
      if (point.measuredAt.isAfter(firstDate) &&
          point.measuredAt.isBefore(lastDate)) {
        addPoint(point.measuredAt, point.value);
      }
    }
    addPoint(
      lastDate,
      WhoGrowthStandard.medianAtDate(
        sex: sex,
        metric: whoMetric,
        birthDate: resolvedBirthDate,
        measuredAt: lastDate,
      )!,
    );
    return List.unmodifiable(result);
  }

  GrowthTrendPoint _toTrendPoint(WhoGrowthReferencePoint point) =>
      GrowthTrendPoint(measuredAt: point.measuredAt, value: point.value);

  String? _whoUnavailableMessage(
    List<GrowthTrendPoint> points,
    List<GrowthTrendPoint> whoPoints,
  ) {
    if (points.isEmpty || whoPoints.isNotEmpty) return null;
    if (profileLoadFailed) {
      return 'Không thể tải thông tin bé để hiển thị WHO P50. Kéo xuống để thử lại.';
    }
    if (birthDate == null) {
      return 'Cần cập nhật ngày sinh của bé để hiển thị WHO P50.';
    }
    if (gender == null || gender == BabyGender.unknown) {
      return 'Cần cập nhật giới tính của bé để hiển thị WHO P50.';
    }
    return 'WHO P50 trong nguồn hiện chỉ áp dụng cho trẻ 0–24 tháng.';
  }
}

class _LegendItem extends StatelessWidget {
  final Color color;
  final String label;
  final bool dashed;

  const _LegendItem({
    super.key,
    required this.color,
    required this.label,
    this.dashed = false,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: 20,
          height: 12,
          child: CustomPaint(
            painter: _LegendLinePainter(color: color, dashed: dashed),
          ),
        ),
        const SizedBox(width: 7),
        Flexible(
          child: Text(
            label,
            key: dashed
                ? const Key('growth-trend-chart-who-label')
                : const Key('growth-trend-chart-metric-label'),
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: Color(0xFF5A463F),
            ),
          ),
        ),
      ],
    );
  }
}

class _LegendLinePainter extends CustomPainter {
  final Color color;
  final bool dashed;

  const _LegendLinePainter({required this.color, required this.dashed});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;
    final y = size.height / 2;
    if (!dashed) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
      return;
    }
    canvas.drawLine(Offset(0, y), Offset(6, y), paint);
    canvas.drawLine(Offset(11, y), Offset(17, y), paint);
  }

  @override
  bool shouldRepaint(covariant _LegendLinePainter oldDelegate) =>
      color != oldDelegate.color || dashed != oldDelegate.dashed;
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
  static const _whoColor = Color(0xFF8E756B);
  static const _gridColor = Color(0x24A88E84);

  final List<GrowthTrendPoint> points;
  final List<GrowthTrendPoint> whoPoints;

  GrowthTrendChartPainter(
    List<GrowthTrendPoint> points, {
    List<GrowthTrendPoint> whoPoints = const [],
  }) : points = List<GrowthTrendPoint>.unmodifiable(points),
       whoPoints = List<GrowthTrendPoint>.unmodifiable(whoPoints);

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

    final allPoints = [...points, ...whoPoints];
    final values = allPoints
        .map((point) => point.value)
        .toList(growable: false);
    final minimum = values.reduce(math.min);
    final maximum = values.reduce(math.max);
    final rawRange = maximum - minimum;
    final displayRange = rawRange == 0 ? 1.0 : rawRange * 1.3;
    final displayMinimum = rawRange == 0
        ? minimum - 0.5
        : minimum - rawRange * 0.15;

    final times = allPoints
        .map((point) => point.measuredAt.millisecondsSinceEpoch)
        .toList(growable: false);
    final firstTime = times.reduce(math.min);
    final lastTime = times.reduce(math.max);
    final timeRange = lastTime - firstTime;
    List<Offset> offsetsFor(List<GrowthTrendPoint> series) {
      return List<Offset>.generate(series.length, (index) {
        final point = series[index];
        final x = timeRange == 0
            ? plotRect.center.dx
            : plotRect.left +
                  plotRect.width *
                      (point.measuredAt.millisecondsSinceEpoch - firstTime) /
                      timeRange;
        final normalized = (point.value - displayMinimum) / displayRange;
        final y = plotRect.bottom - normalized * plotRect.height;
        return Offset(x, y);
      });
    }

    final offsets = offsetsFor(points);
    final whoOffsets = offsetsFor(whoPoints);

    if (whoOffsets.isNotEmpty) {
      final whoPaint = Paint()
        ..color = _whoColor
        ..strokeWidth = 2.5
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round;
      _drawDashedPolyline(canvas, whoOffsets, whoPaint);
    }

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

  void _drawDashedPolyline(Canvas canvas, List<Offset> offsets, Paint paint) {
    if (offsets.length == 1) {
      canvas.drawCircle(offsets.single, 2.5, paint..style = PaintingStyle.fill);
      return;
    }
    const dashLength = 7.0;
    const gapLength = 5.0;
    for (var index = 1; index < offsets.length; index++) {
      final start = offsets[index - 1];
      final end = offsets[index];
      final delta = end - start;
      final distance = delta.distance;
      if (distance == 0) continue;
      final direction = delta / distance;
      var travelled = 0.0;
      while (travelled < distance) {
        final dashEnd = math.min(travelled + dashLength, distance);
        canvas.drawLine(
          start + direction * travelled,
          start + direction * dashEnd,
          paint,
        );
        travelled += dashLength + gapLength;
      }
    }
  }

  @override
  bool shouldRepaint(covariant GrowthTrendChartPainter oldDelegate) {
    return _seriesChanged(points, oldDelegate.points) ||
        _seriesChanged(whoPoints, oldDelegate.whoPoints);
  }

  bool _seriesChanged(
    List<GrowthTrendPoint> current,
    List<GrowthTrendPoint> previous,
  ) {
    if (current.length != previous.length) return true;
    for (var index = 0; index < current.length; index++) {
      if (current[index].measuredAt != previous[index].measuredAt ||
          current[index].value != previous[index].value) {
        return true;
      }
    }
    return false;
  }
}
