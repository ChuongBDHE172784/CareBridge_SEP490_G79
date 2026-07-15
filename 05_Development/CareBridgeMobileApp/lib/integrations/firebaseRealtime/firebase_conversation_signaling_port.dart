import 'dart:async';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../../features/directChat/services/firebase_token_service.dart';
import 'conversation_event_signal.dart';
import 'conversation_signaling_port.dart';

/// ADR-DCC-004: signs in to Firebase with the backend-issued custom token (uid ==
/// CareBridge user id), then listens to the caller's owner-only Firestore inbox.
class FirebaseConversationSignalingPort implements ConversationSignalingPort {
  final _controller = StreamController<ConversationEventSignal>.broadcast();
  StreamSubscription<QuerySnapshot<Map<String, dynamic>>>? _subscription;
  Timer? _reconnectTimer;
  bool _disposed = false;

  @override
  Stream<ConversationEventSignal> get events => _controller.stream;

  @override
  Future<void> connect() async {
    _disposed = false;
    try {
      await _establishListener();
    } catch (_) {
      _scheduleReconnect();
    }
  }

  Future<void> _establishListener() async {
    if (_disposed) return;
    final token = await FirebaseTokenService.instance.fetchCustomToken();
    if (_disposed) return;
    final credential = await FirebaseAuth.instance.signInWithCustomToken(token);
    final uid = credential.user?.uid;
    if (uid == null) {
      throw StateError(
        'Firebase sign-in with custom token did not return a uid',
      );
    }

    if (_disposed) return;
    final query = FirebaseFirestore.instance
        .collection('userConversationEvents')
        .doc(uid)
        .collection('events')
        .orderBy('occurredAt', descending: true)
        .limit(1);
    _subscription = query.snapshots().listen((snapshot) {
      for (final change in snapshot.docChanges) {
        if (change.type != DocumentChangeType.added) continue;
        try {
          _controller.add(
            ConversationEventSignal.fromSnapshotValue(change.doc.data()!),
          );
        } catch (_) {
          // Malformed/unexpected payload — REST reconciliation remains authoritative.
        }
      }
    }, onError: (_) => _scheduleReconnect());
  }

  void _scheduleReconnect() {
    _subscription?.cancel();
    _subscription = null;
    if (_disposed || _reconnectTimer != null) return;
    _reconnectTimer = Timer(const Duration(seconds: 5), () async {
      _reconnectTimer = null;
      try {
        await _establishListener();
      } catch (_) {
        _scheduleReconnect();
      }
    });
  }

  @override
  Future<void> dispose() async {
    _disposed = true;
    _reconnectTimer?.cancel();
    _reconnectTimer = null;
    await _subscription?.cancel();
    await _controller.close();
  }
}
