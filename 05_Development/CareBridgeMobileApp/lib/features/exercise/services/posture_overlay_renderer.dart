// ignore_for_file: avoid_web_libraries_in_flutter, deprecated_member_use

import 'dart:html' as html;
import 'dart:math' as math;

/// [Điểm mốc tọa độ chuẩn hóa trên Canvas]
/// Đại diện cho 1 điểm khớp cơ thể (x, y trong khoảng [0, 1]) kèm độ hiển thị (visibility).
/// Chỉ sử dụng cho việc vẽ đồ họa trực quan trên trình duyệt máy khách.
class PostureOverlayPoint {
  const PostureOverlayPoint({
    required this.x,
    required this.y,
    required this.visibility,
  });

  final double x;
  final double y;
  final double visibility;

  /// Điều kiện hợp lệ để vẽ: tọa độ là số thực hợp lệ và độ tin cậy hiển thị >= 50% (0.5)
  bool get isDrawable =>
      x.isFinite && y.isFinite && visibility.isFinite && visibility >= 0.5;
}

/// [Bảng màu sắc khung xương cục bộ]
/// Màu sắc hiển thị khung xương trên Canvas giúp người dùng nhận biết tư thế:
/// - normal: Màu xanh lá cây/xanh ngọc khi tư thế chuẩn (CORRECT / GOOD).
/// - error: Màu đỏ/cam cảnh báo khi phát hiện sai tư thế (WARNING / CRITICAL).
class PostureOverlayPalette {
  const PostureOverlayPalette({required this.stroke, required this.fill});

  final String stroke;
  final String fill;

  /// Màu chuẩn (Xanh lá & Xanh lam)
  static const normal = PostureOverlayPalette(
    stroke: 'rgba(74, 222, 128, 0.92)',
    fill: 'rgba(56, 189, 248, 0.96)',
  );

  /// Màu cảnh báo lỗi tư thế (Đỏ & Cam)
  static const error = PostureOverlayPalette(
    stroke: 'rgba(248, 113, 113, 0.96)',
    fill: 'rgba(251, 146, 60, 0.98)',
  );
}

/// [Bộ dựng hình khung xương MediaPipe lên HTML Canvas]
///
/// Chịu trách nhiệm xóa và vẽ lại 33 điểm mốc (keypoints) cùng các đoạn xương nối (pose connections)
/// lên Canvas 2D thời gian thực tương ứng với từng khung hình camera.
class PostureOverlayRenderer {
  const PostureOverlayRenderer._();

  /// Danh sách 35 cặp liên kết đoạn xương theo chuẩn MediaPipe Pose (nối 33 điểm mốc):
  /// Ví dụ: [11, 12] nối vai trái - vai phải, [11, 13] nối vai trái - khuỷu tay trái, [13, 15] khuỷu tay - cổ tay.
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
    <int>[11, 12], // Vai trái - Vai phải
    <int>[11, 13], // Vai trái - Khuỷu tay trái
    <int>[13, 15], // Khuỷu tay trái - Cổ tay trái
    <int>[15, 17],
    <int>[15, 19],
    <int>[15, 21],
    <int>[17, 19],
    <int>[12, 14], // Vai phải - Khuỷu tay phải
    <int>[14, 16], // Khuỷu tay phải - Cổ tay phải
    <int>[16, 18],
    <int>[16, 20],
    <int>[16, 22],
    <int>[18, 20],
    <int>[11, 23], // Vai trái - Hông trái
    <int>[12, 24], // Vai phải - Hông phải
    <int>[23, 24], // Hông trái - Hông phải
    <int>[23, 25], // Hông trái - Đầu gối trái
    <int>[24, 26], // Hông phải - Đầu gối phải
    <int>[25, 27], // Đầu gối trái - Mắt cá chân trái
    <int>[26, 28], // Đầu gối phải - Mắt cá chân phải
    <int>[27, 29],
    <int>[28, 30],
    <int>[29, 31],
    <int>[30, 32],
    <int>[27, 31],
    <int>[28, 32],
  ];

  /// Xóa sạch canvas trước khi vẽ khung hình mới
  static void clear(html.CanvasRenderingContext2D context) {
    final canvas = context.canvas;
    final width = canvas.width ?? 0;
    final height = canvas.height ?? 0;
    context.clearRect(0, 0, width, height);
  }

  /// [Thuật toán vẽ khung xương lên Canvas 2D]
  /// 1. Xóa toàn bộ nội dung canvas cũ.
  /// 2. Lấy kích thước width x height của video/canvas hiện tại.
  /// 3. Tính toán độ dày nét vẽ (lineWidth) và bán kính điểm khớp (radius) theo tỉ lệ khung hình.
  /// 4. Lặp qua danh sách các cặp kết nối (poseConnections) để vẽ các đoạn xương (lineTo).
  /// 5. Lặp qua từng điểm mốc để vẽ các hình tròn khớp cơ thể (arc).
  static void draw(
    html.CanvasRenderingContext2D context,
    List<PostureOverlayPoint?> points, {
    bool error = false,
    PostureOverlayPalette? palette,
  }) {
    // [1] Xóa canvas frame cũ
    clear(context);
    final width = context.canvas.width ?? 0;
    final height = context.canvas.height ?? 0;
    if (width <= 0 || height <= 0) return;

    // [2] Cấu hình nét vẽ và màu sắc theo trạng thái đúng/sai tư thế
    final lineWidth = math.max(2.0, math.min(width, height) / 180.0);
    final colors =
        palette ??
        (error ? PostureOverlayPalette.error : PostureOverlayPalette.normal);
    context
      ..lineWidth = lineWidth
      ..lineCap = 'round'
      ..lineJoin = 'round'
      ..strokeStyle = colors.stroke
      ..fillStyle = colors.fill;

    // [3] Vẽ các đoạn thẳng nối các khớp xương
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

    // [4] Vẽ các chấm tròn đại diện cho các mốc khớp cơ thể
    final radius = math.max(2.5, math.min(width, height) / 120.0);
    for (final point in points) {
      if (point == null || !point.isDrawable) continue;
      context
        ..beginPath()
        ..arc(point.x * width, point.y * height, radius, 0, math.pi * 2)
        ..fill();
    }
  }

  /// Trích xuất điểm mốc tại index cụ thể nếu thỏa mãn điều kiện hiển thị
  static PostureOverlayPoint? _pointAt(
    List<PostureOverlayPoint?> points,
    int index,
  ) {
    if (index < 0 || index >= points.length) return null;
    final point = points[index];
    return point != null && point.isDrawable ? point : null;
  }
}
