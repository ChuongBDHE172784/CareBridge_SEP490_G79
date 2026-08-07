import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/models/posture_event_model.dart';
import 'package:untitled/features/exercise/services/posture_event_streamer.dart';

PostureLandmark _landmark(double x) =>
    PostureLandmark(x: x, y: 0.5, z: 0.0, visibility: 0.99);

PostureFeedback _feedback() => const PostureFeedback(
  postureCode: 'GOOD_FORM',
  confidenceScore: 0.9,
  severity: 'INFO',
);

void main() {
  test('defaults to a 100 millisecond sampling interval', () {
    final streamer = PostureEventStreamer(
      send: (_, _) => Future<PostureFeedback>.value(_feedback()),
    );

    expect(streamer.minimumInterval, const Duration(milliseconds: 100));
    streamer.dispose();
  });

  test(
    'keeps one request in flight and sends the latest queued frame',
    () async {
      final firstRequest = Completer<PostureFeedback>();
      final sent = <int>[];
      var requestCount = 0;
      final streamer = PostureEventStreamer(
        minimumInterval: Duration.zero,
        send: (eventTimeMs, _) {
          sent.add(eventTimeMs);
          requestCount++;
          return requestCount == 1
              ? firstRequest.future
              : Future<PostureFeedback>.value(_feedback());
        },
      )..start();

      streamer.push(eventTimeMs: 100, landmarks: {'nose': _landmark(0.1)});
      await Future<void>.delayed(Duration.zero);
      streamer.push(eventTimeMs: 200, landmarks: {'nose': _landmark(0.2)});
      streamer.push(eventTimeMs: 300, landmarks: {'nose': _landmark(0.3)});
      expect(sent, [100]);

      firstRequest.complete(_feedback());
      await Future<void>.delayed(const Duration(milliseconds: 10));

      expect(sent, [100, 300]);
      streamer.dispose();
    },
  );

  test('ignores duplicate and out-of-order timestamps', () async {
    final sent = <int>[];
    final streamer = PostureEventStreamer(
      minimumInterval: Duration.zero,
      send: (eventTimeMs, _) {
        sent.add(eventTimeMs);
        return Future<PostureFeedback>.value(_feedback());
      },
    )..start();

    streamer.push(eventTimeMs: 200, landmarks: {'nose': _landmark(0.2)});
    streamer.push(eventTimeMs: 200, landmarks: {'nose': _landmark(0.3)});
    streamer.push(eventTimeMs: 100, landmarks: {'nose': _landmark(0.1)});
    await Future<void>.delayed(const Duration(milliseconds: 10));

    expect(sent, [200]);
    streamer.dispose();
  });
}
