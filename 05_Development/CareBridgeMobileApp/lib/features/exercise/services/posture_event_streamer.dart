import 'dart:async';

import '../models/posture_event_model.dart';

typedef PostureEventSender =
    Future<PostureFeedback> Function(
      int eventTimeMs,
      Map<String, PostureLandmark> landmarks,
    );

/// Cơ chế truyền tải "Mẫu mới nhất thắng" (Latest-sample-wins transport) cho phân tích tư thế thời gian thực.
///
/// Camera MediaPipe xuất hàng chục khung hình mỗi giây (30-60 FPS), trong khi Backend Spring Boot/AI
/// xử lý tối ưu ở tần suất 10 requests/giây (khoảng cách tối thiểu 100ms).
/// Lớp này đảm bảo:
/// 1. Chỉ có duy nhất 1 request in-flight tại một thời điểm (tránh tràn hàng đợi mạng).
/// 2. Tự động bỏ qua các frame cũ nếu Backend đang bận xử lý frame trước đó.
/// 3. Không tự động retry khi lỗi mạng để tránh spam request.
class PostureEventStreamer {
  PostureEventStreamer({
    required PostureEventSender send,
    this.minimumInterval = const Duration(milliseconds: 100),
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

  /// Bắt đầu cho phép tiếp nhận và gửi dữ liệu tư thế
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

  /// Đưa khung hình vào hàng đợi. Tự động bỏ qua các frame trùng hoặc cũ hơn timestamp gần nhất.
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

  /// Thực hiện gửi request phân tích tư thế sang Backend Spring Boot
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
