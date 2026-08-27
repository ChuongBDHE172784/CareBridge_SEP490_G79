import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/services/pose_skeleton_painter.dart';

void main() {
  group('PosePoint', () {
    test('isDrawable is true for valid coordinates and high visibility', () {
      const point = PosePoint(x: 0.5, y: 0.5, visibility: 0.85);
      expect(point.isDrawable, isTrue);
    });

    test('isDrawable is false when visibility < 0.5', () {
      const point = PosePoint(x: 0.5, y: 0.5, visibility: 0.49);
      expect(point.isDrawable, isFalse);
    });

    test('isDrawable is false for infinite or NaN coordinates', () {
      const p1 = PosePoint(x: double.nan, y: 0.5, visibility: 0.9);
      const p2 = PosePoint(x: 0.5, y: double.infinity, visibility: 0.9);
      const p3 = PosePoint(x: 0.5, y: 0.5, visibility: double.nan);
      expect(p1.isDrawable, isFalse);
      expect(p2.isDrawable, isFalse);
      expect(p3.isDrawable, isFalse);
    });
  });

  group('PoseSkeletonPalette', () {
    test('provides distinct colors for normal and error palettes', () {
      expect(PoseSkeletonPalette.normal.stroke, isNot(equals(PoseSkeletonPalette.error.stroke)));
      expect(PoseSkeletonPalette.normal.fill, isNot(equals(PoseSkeletonPalette.error.fill)));
    });
  });

  group('PoseSkeletonPainter', () {
    testWidgets('paints skeleton on canvas without throwing', (tester) async {
      final points = List<PosePoint?>.generate(33, (index) {
        return PosePoint(
          x: 0.2 + (index % 5) * 0.1,
          y: 0.1 + (index ~/ 5) * 0.1,
          visibility: 0.9,
        );
      });

      const key = Key('skeleton_custom_paint');
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CustomPaint(
              key: key,
              size: const Size(400, 600),
              painter: PoseSkeletonPainter(
                points: points,
                hasError: false,
                isFrontCamera: true,
              ),
            ),
          ),
        ),
      );

      expect(find.byKey(key), findsOneWidget);
    });

    testWidgets('paints safely with empty or partial points', (tester) async {
      const key = Key('skeleton_custom_paint_empty');
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: CustomPaint(
              key: key,
              size: Size(400, 600),
              painter: PoseSkeletonPainter(
                points: <PosePoint?>[],
                hasError: true,
              ),
            ),
          ),
        ),
      );

      expect(find.byKey(key), findsOneWidget);
    });

    test('shouldRepaint returns true when state changes', () {
      const p1 = PoseSkeletonPainter(points: [], hasError: false);
      const p2 = PoseSkeletonPainter(points: [], hasError: true);
      const p3 = PoseSkeletonPainter(
        points: [PosePoint(x: 0.5, y: 0.5, visibility: 0.9)],
        hasError: false,
      );

      expect(p1.shouldRepaint(p2), isTrue);
      expect(p1.shouldRepaint(p3), isTrue);
      expect(p1.shouldRepaint(p1), isFalse);
    });
  });
}
