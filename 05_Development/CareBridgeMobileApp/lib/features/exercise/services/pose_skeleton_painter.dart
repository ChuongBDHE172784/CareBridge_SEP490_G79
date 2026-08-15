import 'dart:math' as math;
import 'package:flutter/material.dart';

/// Normalized pose landmark point for custom canvas rendering.
class PosePoint {
  final double x;
  final double y;
  final double visibility;

  const PosePoint({
    required this.x,
    required this.y,
    required this.visibility,
  });

  bool get isDrawable =>
      x.isFinite && y.isFinite && visibility.isFinite && visibility >= 0.5;
}

/// Palette used to paint the skeleton overlay.
class PoseSkeletonPalette {
  final Color stroke;
  final Color fill;

  const PoseSkeletonPalette({
    required this.stroke,
    required this.fill,
  });

  static const normal = PoseSkeletonPalette(
    stroke: Color(0xEB4ADE80), // rgba(74, 222, 128, 0.92)
    fill: Color(0xF538BDF8),   // rgba(56, 189, 248, 0.96)
  );

  static const error = PoseSkeletonPalette(
    stroke: Color(0xF5F87171), // rgba(248, 113, 113, 0.96)
    fill: Color(0xFAFB923C),   // rgba(251, 146, 60, 0.98)
  );
}

/// CustomPainter that renders a 33-point body skeleton on Flutter Canvas.
class PoseSkeletonPainter extends CustomPainter {
  final List<PosePoint?> points;
  final bool hasError;
  final bool isFrontCamera;

  const PoseSkeletonPainter({
    required this.points,
    this.hasError = false,
    this.isFrontCamera = true,
  });

  static const List<List<int>> poseConnections = <List<int>>[
    <int>[0, 1],
    <int>[1, 2],
    <int>[2, 3],
    <int>[3, 7],
    <int>[0, 4],
    <int>[4, 5],
    <int>[5, 6],
    <int>[6, 8],
    <int>[9, 10],
    <int>[11, 12],
    <int>[11, 13],
    <int>[13, 15],
    <int>[15, 17],
    <int>[15, 19],
    <int>[15, 21],
    <int>[17, 19],
    <int>[12, 14],
    <int>[14, 16],
    <int>[16, 18],
    <int>[16, 20],
    <int>[16, 22],
    <int>[18, 20],
    <int>[11, 23],
    <int>[12, 24],
    <int>[23, 24],
    <int>[23, 25],
    <int>[24, 26],
    <int>[25, 27],
    <int>[26, 28],
    <int>[27, 29],
    <int>[28, 30],
    <int>[29, 31],
    <int>[30, 32],
    <int>[27, 31],
    <int>[28, 32],
  ];

  @override
  void paint(Canvas canvas, Size size) {
    if (points.isEmpty || size.width <= 0 || size.height <= 0) return;

    final palette = hasError ? PoseSkeletonPalette.error : PoseSkeletonPalette.normal;
    final strokeWidth = math.max(2.0, math.min(size.width, size.height) / 180.0);
    final dotRadius = math.max(2.5, math.min(size.width, size.height) / 120.0);

    final linePaint = Paint()
      ..color = palette.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round
      ..style = PaintingStyle.stroke;

    final pointPaint = Paint()
      ..color = palette.fill
      ..style = PaintingStyle.fill;

    // Draw connection lines
    for (final connection in poseConnections) {
      final p1 = _getPoint(connection[0]);
      final p2 = _getPoint(connection[1]);
      if (p1 == null || p2 == null) continue;

      final offset1 = _toOffset(p1, size);
      final offset2 = _toOffset(p2, size);
      canvas.drawLine(offset1, offset2, linePaint);
    }

    // Draw landmark points
    for (final point in points) {
      if (point == null || !point.isDrawable) continue;
      final offset = _toOffset(point, size);
      canvas.drawCircle(offset, dotRadius, pointPaint);
    }
  }

  Offset _toOffset(PosePoint point, Size size) {
    final double x = isFrontCamera ? (1.0 - point.x) * size.width : point.x * size.width;
    final double y = point.y * size.height;
    return Offset(x, y);
  }

  PosePoint? _getPoint(int index) {
    if (index < 0 || index >= points.length) return null;
    final p = points[index];
    return p != null && p.isDrawable ? p : null;
  }

  @override
  bool shouldRepaint(covariant PoseSkeletonPainter oldDelegate) {
    return oldDelegate.points != points ||
        oldDelegate.hasError != hasError ||
        oldDelegate.isFrontCamera != isFrontCamera;
  }
}
