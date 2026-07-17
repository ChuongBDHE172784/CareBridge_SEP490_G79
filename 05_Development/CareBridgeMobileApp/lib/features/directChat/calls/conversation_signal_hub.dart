import 'dart:async';

import '../../../integrations/firebaseRealtime/conversation_event_signal.dart';
import '../../../integrations/firebaseRealtime/conversation_signaling_port.dart';
import '../../../integrations/firebaseRealtime/firebase_conversation_signaling_port.dart';

class ConversationSignalHub {
  ConversationSignalHub._();

  static final ConversationSignalHub instance = ConversationSignalHub._();

  final _events = StreamController<ConversationEventSignal>.broadcast();
  ConversationSignalingPort? _port;
  StreamSubscription<ConversationEventSignal>? _subscription;
  bool _running = false;

  Stream<ConversationEventSignal> get events => _events.stream;

  Future<void> start() async {
    if (_running) return;
    _running = true;
    final port = FirebaseConversationSignalingPort();
    _port = port;
    _subscription = port.events.listen(_events.add);
    try {
      await port.connect();
    } catch (_) {
      // The port retries internally; REST reconciliation remains authoritative.
    }
  }

  Future<void> stop() async {
    if (!_running) return;
    _running = false;
    await _subscription?.cancel();
    _subscription = null;
    await _port?.dispose();
    _port = null;
  }
}
