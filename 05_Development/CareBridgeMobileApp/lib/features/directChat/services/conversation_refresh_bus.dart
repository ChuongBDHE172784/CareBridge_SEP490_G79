import 'dart:async';

/// Process-local invalidation signal for inbox rows and shell unread badges.
/// Firestore/FCM remain hints only; consumers always reconcile through REST.
class ConversationRefreshBus {
  ConversationRefreshBus._();

  static final StreamController<void> _controller =
      StreamController<void>.broadcast(sync: true);

  static Stream<void> get events => _controller.stream;

  static void notify() => _controller.add(null);
}
