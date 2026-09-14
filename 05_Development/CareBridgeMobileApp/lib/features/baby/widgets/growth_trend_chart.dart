import 'package:flutter/material.dart';
import '../../healthRecords/models/growth_measurement_model.dart';

/// Age of a baby at [at], using the same calendar rules as `BabyProfile.ageLabel`
/// (day-of-month is ignored when counting months).
String formatBabyAgeAt(DateTime birthDate, DateTime at) {
  final months = (at.year - birthDate.year) * 12 + at.month - birthDate.month;
  if (months < 1) {
    final days = DateTime(at.year, at.month, at.day)
        .difference(DateTime(birthDate.year, birthDate.month, birthDate.day))
        .inDays;
    return '$days ngày tuổi';
  }
  if (months < 12) return '$months tháng tuổi';
  final years = months ~/ 12;
  final rem = months % 12;
  if (rem == 0) return '$years tuổi';
  return '$years tuổi $rem tháng';
}

/// Smooth growth trend line shared by the baby profile growth tab and the
/// direct-chat baby growth card.
class GrowthTrendChart extends StatelessWidget {
  const GrowthTrendChart({
    super.key,
    required this.measurements,
    required this.valueExtractor,
    this.unit = 'kg',
    this.accentColor = const Color(0xFFC98C7B),
    this.dotColor = const Color(0xFF845143),
    this.height = 120,
  });

  final List<GrowthMeasurement> measurements;
  final double? Function(GrowthMeasurement) valueExtractor;
  final String unit;
  final Color accentColor;
  final Color dotColor;
  final double height;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      width: double.infinity,
      child: CustomPaint(
        painter: GrowthTrendChartPainter(
          measurements: measurements,
          valueExtractor: valueExtractor,
          unit: unit,
          accentColor: accentColor,
          dotColor: dotColor,
        ),
      ),
    );
  }
}

class GrowthTrendChartPainter extends CustomPainter {
  final List<GrowthMeasurement> measurements;
  final double? Function(GrowthMeasurement) valueExtractor;
  final String unit;
  final Color accentColor;
  final Color dotColor;

  const GrowthTrendChartPainter({
    required this.measurements,
    required this.valueExtractor,
    this.unit = 'kg',
    this.accentColor = const Color(0xFFC98C7B),
    this.dotColor = const Color(0xFF845143),
  });

  @override
  void paint(Canvas canvas, Size size) {
    final linePaint = Paint()
      ..color = accentColor
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final fillPaint = Paint()
      ..shader = LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [
          accentColor.withAlpha(51),
          accentColor.withAlpha(0),
        ],
      ).createShader(Rect.fromLTWH(0, 0, size.width, size.height))
      ..style = PaintingStyle.fill;

    final values = measurements
        .map((measurement) => valueExtractor(measurement))
        .whereType<double>()
        .toList(growable: false);
    if (values.isEmpty) return;

    final minVal = values.reduce((a, b) => a < b ? a : b);
    final maxVal = values.reduce((a, b) => a > b ? a : b);
    final centerVal = (minVal + maxVal) / 2;
    final displayRange = (maxVal - minVal)
        .clamp(1.0, double.infinity)
        .toDouble();
    final displayMin = centerVal - displayRange / 2;
    final firstTime = measurements.first.measuredAt.millisecondsSinceEpoch;
    final lastTime = measurements.last.measuredAt.millisecondsSinceEpoch;
    final timeRange = lastTime - firstTime;
    final points = List.generate(measurements.length, (index) {
      final x = measurements.length == 1 || timeRange == 0
          ? size.width / 2
          : size.width *
                (measurements[index].measuredAt.millisecondsSinceEpoch -
                    firstTime) /
                timeRange;
      final normalized = (values[index] - displayMin) / displayRange;
      return Offset(x, size.height * (0.9 - normalized * 0.7));
    });

    // Draw smooth curve
    final path = Path()..moveTo(points.first.dx, points.first.dy);
    for (int i = 0; i < points.length - 1; i++) {
      final cp1 = Offset((points[i].dx + points[i + 1].dx) / 2, points[i].dy);
      final cp2 = Offset(
        (points[i].dx + points[i + 1].dx) / 2,
        points[i + 1].dy,
      );
      path.cubicTo(
        cp1.dx,
        cp1.dy,
        cp2.dx,
        cp2.dy,
        points[i + 1].dx,
        points[i + 1].dy,
      );
    }

    // Fill under curve
    final fillPath = Path.from(path)
      ..lineTo(size.width, size.height)
      ..lineTo(0, size.height)
      ..close();
    canvas.drawPath(fillPath, fillPaint);

    // Draw line
    canvas.drawPath(path, linePaint);

    // Draw dots and value labels
    final pDotPaint = Paint()
      ..color = dotColor
      ..style = PaintingStyle.fill;
    final textPainter = TextPainter(textDirection: TextDirection.ltr);

    for (var i = 0; i < points.length; i++) {
      final p = points[i];
      final val = values[i];
      canvas.drawCircle(p, p == points.last ? 5 : 3, pDotPaint);

      final valStr =
          '${val % 1 == 0 ? val.toInt() : val.toStringAsFixed(1)} $unit';
      textPainter.text = TextSpan(
        text: valStr,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 10,
          fontWeight: FontWeight.w700,
          color: dotColor,
        ),
      );
      textPainter.layout();

      final labelWidth = textPainter.width + 8;
      final labelHeight = textPainter.height + 2;
      final labelX = (p.dx - labelWidth / 2).clamp(
        4.0,
        size.width - labelWidth - 4.0,
      );
      final isTopHalf = p.dy < size.height / 2;
      final labelY = isTopHalf
          ? (p.dy + 6).clamp(2.0, size.height - labelHeight - 2.0)
          : (p.dy - labelHeight - 6).clamp(
              2.0,
              size.height - labelHeight - 2.0,
            );

      final bgRRect = RRect.fromRectAndRadius(
        Rect.fromLTWH(labelX, labelY, labelWidth, labelHeight),
        const Radius.circular(5),
      );
      canvas.drawRRect(bgRRect, Paint()..color = const Color(0xFFFFFDFB));
      canvas.drawRRect(
        bgRRect,
        Paint()
          ..color = accentColor.withValues(alpha: 0.4)
          ..strokeWidth = 1
          ..style = PaintingStyle.stroke,
      );

      textPainter.paint(canvas, Offset(labelX + 4, labelY + 1));
    }
  }

  @override
  bool shouldRepaint(covariant GrowthTrendChartPainter oldDelegate) {
    if (measurements.length != oldDelegate.measurements.length ||
        unit != oldDelegate.unit ||
        accentColor != oldDelegate.accentColor ||
        dotColor != oldDelegate.dotColor) {
      return true;
    }
    for (var i = 0; i < measurements.length; i++) {
      if (valueExtractor(measurements[i]) !=
              oldDelegate.valueExtractor(oldDelegate.measurements[i]) ||
          measurements[i].measuredAt !=
              oldDelegate.measurements[i].measuredAt) {
        return true;
      }
    }
    return false;
  }
}
