// ignore_for_file: avoid_web_libraries_in_flutter, deprecated_member_use

import 'dart:html' as html;
import 'dart:math' as math;

/// A normalized pose point used only by the local browser overlay.
class PostureOverlayPoint {
  const PostureOverlayPoint({
    required this.x,
    required this.y,
    required this.visibility,
  });

  final double x;
  final double y;
  final double visibility;

  bool get isDrawable =>
      x.isFinite && y.isFinite && visibility.isFinite && visibility >= 0.5;
}

/// Draws the standard MediaPipe Pose graph into a local canvas.
///
/// The canvas and video are mirrored together by the owning platform-view
/// wrapper. Therefore x remains in MediaPipe's canonical coordinate space and
/// is never changed in this renderer or in the network payload.
class PostureOverlayRenderer {
  const PostureOverlayRenderer._();

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

  static void clear(html.CanvasRenderingContext2D context) {
    final canvas = context.canvas;
    final width = canvas.width ?? 0;
    final height = canvas.height ?? 0;
    context.clearRect(0, 0, width, height);
  }

  static void draw(
    html.CanvasRenderingContext2D context,
    List<PostureOverlayPoint?> points,
  ) {
    clear(context);
    final width = context.canvas.width ?? 0;
    final height = context.canvas.height ?? 0;
    if (width <= 0 || height <= 0) return;

    final lineWidth = math.max(2.0, math.min(width, height) / 180.0);
    context
      ..lineWidth = lineWidth
      ..lineCap = 'round'
      ..lineJoin = 'round'
      ..strokeStyle = 'rgba(74, 222, 128, 0.92)'
      ..fillStyle = 'rgba(56, 189, 248, 0.96)';

    for (final connection in poseConnections) {
      final start = _pointAt(points, connection[0]);
      final end = _pointAt(points, connection[1]);
      if (start == null || end == null) continue;

      context
        ..beginPath()
        ..moveTo(start.x * width, start.y * height)
        ..lineTo(end.x * width, end.y * height)
        ..stroke();
    }

    final radius = math.max(2.5, math.min(width, height) / 120.0);
    for (final point in points) {
      if (point == null || !point.isDrawable) continue;
      context
        ..beginPath()
        ..arc(point.x * width, point.y * height, radius, 0, math.pi * 2)
        ..fill();
    }
  }

  static PostureOverlayPoint? _pointAt(
    List<PostureOverlayPoint?> points,
    int index,
  ) {
    if (index < 0 || index >= points.length) return null;
    final point = points[index];
    return point != null && point.isDrawable ? point : null;
  }
}
