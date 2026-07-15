import 'dart:async';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_database/firebase_database.dart';
import '../../features/directChat/services/firebase_token_service.dart';
import 'conversation_event_signal.dart';
import 'conversation_signaling_port.dart';

/// ADR-DCC-004: signs in to Firebase with the backend-issued custom token (uid ==
/// CareBridge user id), then listens at `/user-conversation-events/{uid}` — the caller's
/// own inbox, enforced server-side by RTDB Rules (`auth.uid === $uid`).
class FirebaseConversationSignalingPort implements ConversationSignalingPort {
  final _controller = StreamController<ConversationEventSignal>.broadcast();
  StreamSubscription<DatabaseEvent>? _subscription;

  @override
  Stream<ConversationEventSignal> get events => _controller.stream;

  @override
  Future<void> connect() async {
    final token = await FirebaseTokenService.instance.fetchCustomToken();
    final credential = await FirebaseAuth.instance.signInWithCustomToken(token);
    final uid = credential.user?.uid;
    if (uid == null) {
      throw StateError('Firebase sign-in with custom token did not return a uid');
    }

    final ref = FirebaseDatabase.instance.ref('/user-conversation-events/$uid');
    _subscription = ref.onChildAdded.listen((event) {
      final raw = event.snapshot.value;
      if (raw is Map) {
        try {
          _controller.add(ConversationEventSignal.fromSnapshotValue(raw));
        } catch (_) {
          // Malformed/unexpected payload — signal-only, ignore and let the client's
          // periodic/on-demand REST reconciliation catch up regardless.
        }
      }
    });
  }

  @override
  Future<void> dispose() async {
    await _subscription?.cancel();
    await _controller.close();
  }
}
