import 'conversation_event_signal.dart';

/// Port abstraction so screens/tests never depend on the Firebase SDK directly.
abstract class ConversationSignalingPort {
  Stream<ConversationEventSignal> get events;

  Future<void> connect();

  Future<void> dispose();
}
