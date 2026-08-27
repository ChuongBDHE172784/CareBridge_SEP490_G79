import 'dart:async';

class ConsultationRequestRefreshBus {
  ConsultationRequestRefreshBus._();

  static final StreamController<void> _controller =
      StreamController<void>.broadcast();

  static Stream<void> get events => _controller.stream;

  static void notify() => _controller.add(null);
}
