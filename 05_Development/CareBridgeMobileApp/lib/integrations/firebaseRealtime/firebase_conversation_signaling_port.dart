import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../../features/directChat/services/firebase_token_service.dart';
import 'conversation_event_signal.dart';
import 'conversation_signaling_port.dart';

typedef FirebaseTokenLoader = Future<FirebaseTokenCapability> Function();
typedef FirebaseSigner = Future<String> Function(String customToken);
typedef FirebaseStaleSignInCleaner = Future<void> Function(String uid);
typedef FirebaseListenerCancellation = Future<void> Function();
typedef FirebaseListenerStarter =
    Future<FirebaseListenerCancellation> Function({
      required String uid,
      required void Function(ConversationEventSignal event) onEvent,
      required void Function() onError,
    });

class _FirebaseSignInGate {
  Future<void> _tail = Future<void>.value();

  Future<T> run<T>(Future<T> Function() operation) {
    final result = Completer<T>();
    _tail = _tail.then((_) async {
      try {
        result.complete(await operation());
      } catch (error, stackTrace) {
        result.completeError(error, stackTrace);
      }
    });
    return result.future;
  }
}

final _firebaseSignInGate = _FirebaseSignInGate();

/// ADR-DCC-004: signs in to Firebase with the backend-issued custom token (uid ==
/// CareBridge user id), then listens to the caller's owner-only Firestore inbox.
class FirebaseConversationSignalingPort implements ConversationSignalingPort {
  FirebaseConversationSignalingPort({
    FirebaseTokenLoader? tokenLoader,
    FirebaseSigner? signer,
    FirebaseStaleSignInCleaner? staleSignInCleaner,
    FirebaseListenerStarter? listenerStarter,
  }) : _tokenLoader =
           tokenLoader ?? FirebaseTokenService.instance.fetchCapability,
       _signer = signer ?? _signInWithFirebase,
       _staleSignInCleaner =
           staleSignInCleaner ?? _signOutIfCurrentFirebaseUser,
       _listenerStarter = listenerStarter ?? _startFirestoreListener;

  static const _reconnectDelay = Duration(seconds: 5);

  final _controller = StreamController<ConversationEventSignal>.broadcast();
  final FirebaseTokenLoader _tokenLoader;
  final FirebaseSigner _signer;
  final FirebaseStaleSignInCleaner _staleSignInCleaner;
  final FirebaseListenerStarter _listenerStarter;
  FirebaseListenerCancellation? _cancelListener;
  Timer? _reconnectTimer;
  bool _disposed = false;
  bool _terminal = false;
  bool _attemptInFlight = false;
  int _generation = 0;
  int _attemptGeneration = 0;

  @override
  Stream<ConversationEventSignal> get events => _controller.stream;

  @override
  Future<void> connect() async {
    if (_disposed ||
        _terminal ||
        _attemptInFlight ||
        _cancelListener != null ||
        _reconnectTimer != null) {
      return;
    }
    await _attemptConnect(_generation);
  }

  Future<void> _attemptConnect(int generation) async {
    if (!_isActive(generation) || _attemptInFlight) return;
    final attemptGeneration = ++_attemptGeneration;
    _attemptInFlight = true;
    try {
      final capability = await _tokenLoader();
      if (!_isCurrentAttempt(generation, attemptGeneration)) return;
      if (!capability.firestoreSignalingEnabled) {
        _finishTerminalAttempt(generation, attemptGeneration);
        return;
      }

      final token = capability.firebaseCustomToken;
      if (token == null || token.trim().isEmpty) {
        _finishTerminalAttempt(generation, attemptGeneration);
        return;
      }

      final uid = await _firebaseSignInGate.run<String?>(() async {
        if (!_isCurrentAttempt(generation, attemptGeneration)) return null;
        final signedInUid = await _signer(token);
        if (_isCurrentAttempt(generation, attemptGeneration)) {
          return signedInUid;
        }
        await _staleSignInCleaner(signedInUid);
        return null;
      });
      if (uid == null || !_isCurrentAttempt(generation, attemptGeneration)) {
        return;
      }

      final cancellation = await _listenerStarter(
        uid: uid,
        onEvent: (event) {
          if (_isCurrentAttempt(generation, attemptGeneration)) {
            _controller.add(event);
          }
        },
        onError: () {
          if (_isCurrentAttempt(generation, attemptGeneration)) {
            _scheduleReconnect(generation, attemptGeneration);
          }
        },
      );
      if (!_isCurrentAttempt(generation, attemptGeneration) ||
          _reconnectTimer != null) {
        await _cancelIgnoringErrors(cancellation);
        return;
      }
      _cancelListener = cancellation;
    } on FirebaseSignalingTerminalException {
      _finishTerminalAttempt(generation, attemptGeneration);
    } catch (_) {
      _scheduleReconnect(generation, attemptGeneration);
    } finally {
      _attemptInFlight = false;
    }
  }

  static Future<String> _signInWithFirebase(String customToken) async {
    final credential = await FirebaseAuth.instance.signInWithCustomToken(
      customToken,
    );
    final uid = credential.user?.uid;
    if (uid == null) {
      throw StateError(
        'Firebase sign-in with custom token did not return a uid',
      );
    }
    return uid;
  }

  static Future<void> _signOutIfCurrentFirebaseUser(String uid) async {
    final auth = FirebaseAuth.instance;
    if (auth.currentUser?.uid == uid) await auth.signOut();
  }

  static Future<FirebaseListenerCancellation> _startFirestoreListener({
    required String uid,
    required void Function(ConversationEventSignal event) onEvent,
    required void Function() onError,
  }) async {
    final query = FirebaseFirestore.instance
        .collection('userConversationEvents')
        .doc(uid)
        .collection('events')
        .orderBy('occurredAt', descending: true)
        .limit(1);
    final subscription = query.snapshots().listen((snapshot) {
      for (final change in snapshot.docChanges) {
        if (change.type != DocumentChangeType.added) continue;
        try {
          onEvent(
            ConversationEventSignal.fromSnapshotValue(change.doc.data()!),
          );
        } catch (_) {
          // Malformed/unexpected payload — REST reconciliation remains authoritative.
        }
      }
    }, onError: (_, _) => onError());
    return subscription.cancel;
  }

  bool _isActive(int generation) =>
      !_disposed && !_terminal && generation == _generation;

  bool _isCurrentAttempt(int generation, int attemptGeneration) =>
      _isActive(generation) && attemptGeneration == _attemptGeneration;

  void _finishTerminalAttempt(int generation, int attemptGeneration) {
    if (!_isCurrentAttempt(generation, attemptGeneration)) return;
    _terminal = true;
    _attemptGeneration++;
  }

  void _scheduleReconnect(int generation, int attemptGeneration) {
    if (!_isCurrentAttempt(generation, attemptGeneration) ||
        _reconnectTimer != null) {
      return;
    }
    _attemptGeneration++;
    final cancellation = _cancelListener;
    _cancelListener = null;
    if (cancellation != null) {
      unawaited(_cancelIgnoringErrors(cancellation));
    }
    _reconnectTimer = Timer(_reconnectDelay, () {
      _reconnectTimer = null;
      unawaited(_attemptConnect(generation));
    });
  }

  static Future<void> _cancelIgnoringErrors(
    FirebaseListenerCancellation cancellation,
  ) async {
    try {
      await cancellation();
    } catch (_) {
      // Listener teardown cannot make REST reconciliation unavailable.
    }
  }

  @override
  Future<void> dispose() async {
    if (_disposed) return;
    _disposed = true;
    _generation++;
    _attemptGeneration++;
    _reconnectTimer?.cancel();
    _reconnectTimer = null;
    final cancellation = _cancelListener;
    _cancelListener = null;
    if (cancellation != null) {
      await _cancelIgnoringErrors(cancellation);
    }
    await _controller.close();
  }
}
