@TestOn('browser')
library;

// Browser-only DOM and Canvas assertions intentionally use dart:html.
// ignore_for_file: avoid_web_libraries_in_flutter, deprecated_member_use

import 'dart:html' as html;

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/services/posture_camera_source_web.dart';
import 'package:untitled/features/exercise/services/posture_overlay_renderer.dart';

void main() {
  test('platform preview keeps video and canvas in one mirrored layer', () {
    final source = PostureCameraSource();
    addTearDown(source.dispose);

    final root = source.debugPreviewElement;
    final video = root.querySelector('video');
    final canvas = root.querySelector('canvas');

    expect(video, isA<html.VideoElement>());
    expect(canvas, isA<html.CanvasElement>());
    expect(root.style.transform, 'scaleX(-1)');
    expect(video!.style.objectFit, 'cover');
    expect(canvas!.style.objectFit, 'cover');
    expect(canvas.style.getPropertyValue('pointer-events'), 'none');
  });

  test('renderer draws a pose locally and clears it', () {
    final canvas = html.CanvasElement(width: 100, height: 100);
    final context = canvas.context2D;
    final points = List<PostureOverlayPoint?>.filled(33, null);
    points[11] = const PostureOverlayPoint(x: 0.25, y: 0.25, visibility: 1);
    points[12] = const PostureOverlayPoint(x: 0.75, y: 0.25, visibility: 1);
    points[13] = const PostureOverlayPoint(x: 0.2, y: 0.5, visibility: 1);

    PostureOverlayRenderer.draw(context, points);
    expect(_hasInk(context), isTrue);
    expect(PostureOverlayRenderer.poseConnections, hasLength(35));

    PostureOverlayRenderer.clear(context);
    expect(_hasInk(context), isFalse);
  });

  test('stopping the camera clears a previously drawn overlay', () async {
    final source = PostureCameraSource();
    addTearDown(source.dispose);
    final canvas =
        source.debugPreviewElement.querySelector('canvas')!
            as html.CanvasElement;
    final context = canvas.context2D;
    final points = List<PostureOverlayPoint?>.filled(33, null);
    points[11] = const PostureOverlayPoint(x: 0.25, y: 0.25, visibility: 1);
    points[12] = const PostureOverlayPoint(x: 0.75, y: 0.25, visibility: 1);

    PostureOverlayRenderer.draw(context, points);
    expect(_hasInk(context), isTrue);
    await source.stop();
    expect(_hasInk(context), isFalse);
  });
}

bool _hasInk(html.CanvasRenderingContext2D context) {
  final width = context.canvas.width ?? 0;
  final height = context.canvas.height ?? 0;
  final pixels = context.getImageData(0, 0, width, height).data;
  return pixels.any((value) => value != 0);
}
