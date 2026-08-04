import 'dart:async';

import '../models/posture_event_model.dart';

typedef PostureEventSender =
    Future<PostureFeedback> Function(
      int eventTimeMs,
      Map<String, PostureLandmark> landmarks,
    );

/// Latest-sample-wins transport for realtime posture feedback.
///
/// Camera/pose extraction can emit many frames per second, while the backend
/// contract deliberately samples at roughly two requests per second. This
/// class keeps at most one request in flight, drops stale frames, and never
/// retries a failed sample automatically.
class PostureEventStreamer {
  PostureEventStreamer({
    required PostureEventSender send,
    this.minimumInterval = const Duration(milliseconds: 550),
    this.onFeedback,
    this.onError,
  }) : _send = send;

  final PostureEventSender _send;
  final Duration minimumInterval;
  final void Function(PostureFeedback feedback)? onFeedback;
  final void Function(Object error, StackTrace stackTrace)? onError;

  Timer? _timer;
  bool _running = false;
  bool _sending = false;
  int? _lastAcceptedEventTimeMs;
  DateTime? _lastSentAt;
  _PendingPostureEvent? _pending;

  bool get isRunning => _running;

  void start() {
    _running = true;
  }

  Future<void> stop() async {
    _running = false;
    _timer?.cancel();
    _timer = null;
    _pending = null;
    while (_sending) {
      await Future<void>.delayed(const Duration(milliseconds: 10));
    }
  }

  void dispose() {
    _running = false;
    _timer?.cancel();
    _timer = null;
    _pending = null;
  }

  /// Queues a frame. Older or duplicate timestamps are ignored.
  void push({
    required int eventTimeMs,
    required Map<String, PostureLandmark> landmarks,
  }) {
    if (!_running || eventTimeMs < 0 || landmarks.isEmpty) return;
    final previous = _lastAcceptedEventTimeMs;
    if (previous != null && eventTimeMs <= previous) return;

    _lastAcceptedEventTimeMs = eventTimeMs;
    _pending = _PendingPostureEvent(
      eventTimeMs: eventTimeMs,
      landmarks: Map<String, PostureLandmark>.unmodifiable(landmarks),
    );
    _scheduleSend();
  }

  void _scheduleSend() {
    if (!_running || _sending || _pending == null || _timer != null) return;

    final lastSentAt = _lastSentAt;
    final wait = lastSentAt == null
        ? Duration.zero
        : minimumInterval - DateTime.now().difference(lastSentAt);
    _timer = Timer(wait.isNegative ? Duration.zero : wait, () {
      _timer = null;
      unawaited(_sendNext());
    });
  }

  Future<void> _sendNext() async {
    if (!_running || _sending || _pending == null) return;
    final event = _pending;
    _pending = null;
    if (event == null) return;

    _sending = true;
    try {
      final feedback = await _send(event.eventTimeMs, event.landmarks);
      if (_running) onFeedback?.call(feedback);
    } catch (error, stackTrace) {
      if (_running) onError?.call(error, stackTrace);
    } finally {
      _lastSentAt = DateTime.now();
      _sending = false;
      _scheduleSend();
    }
  }
}

class _PendingPostureEvent {
  const _PendingPostureEvent({
    required this.eventTimeMs,
    required this.landmarks,
  });

  final int eventTimeMs;
  final Map<String, PostureLandmark> landmarks;
}
