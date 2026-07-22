import 'dart:async';

import '../../../integrations/firebaseRealtime/conversation_event_signal.dart';
import '../models/conversation_call.dart';
import '../models/zego_join_credentials.dart';
import 'direct_call_api.dart';
import 'direct_call_state.dart';

class DirectCallCoordinator {
  DirectCallCoordinator({
    required DirectCallApiPort api,
    required Stream<ConversationEventSignal> signals,
    required String? Function() currentUserId,
  }) : _api = api,
       _signals = signals,
       _currentUserId = currentUserId;

  final DirectCallApiPort _api;
  final Stream<ConversationEventSignal> _signals;
  final String? Function() _currentUserId;
  final _states = StreamController<DirectCallState>.broadcast();
  final _seenEventIds = <String>{};
  final _pendingCallIds = <String, String>{};
  StreamSubscription<ConversationEventSignal>? _signalSubscription;
  DirectCallState _state = const DirectCallState();
  bool _syncing = false;
  bool _pendingSync = false;
  bool _disposed = false;
  int _generation = 0;
  Timer? _pollTimer;

  static const _pollInterval = Duration(seconds: 5);

  DirectCallState get state => _state;
  Stream<DirectCallState> get states => _states.stream;

  Future<void> start() async {
    if (_disposed || _signalSubscription != null) return;
    _signalSubscription = _signals.listen(_onSignal);
    await reconcileActiveCalls();
  }

  Future<void> reconcileActiveCalls() async {
    if (_disposed) return;
    if (_syncing) {
      _pendingSync = true;
      return;
    }
    _syncing = true;
    final generation = ++_generation;
    try {
      final calls = await _api.listActiveCalls();
      if (_disposed || generation != _generation) return;
      for (final call in calls) {
        await _apply(call);
      }
      final pending = Map<String, String>.from(_pendingCallIds);
      _pendingCallIds.clear();
      for (final entry in pending.entries) {
        await _fetchDetail(entry.value, entry.key);
      }
    } catch (_) {
      // Listener/auth/network failures degrade to future resume or signal reconciliation.
    } finally {
      _syncing = false;
      if (_pendingSync && !_disposed) {
        _pendingSync = false;
        scheduleMicrotask(reconcileActiveCalls);
      }
    }
  }

  void _onSignal(ConversationEventSignal signal) {
    if (_disposed ||
        !signal.eventType.startsWith('CALL_') ||
        !_seenEventIds.add(signal.eventId)) {
      return;
    }
    if (_syncing) {
      _pendingCallIds[signal.resourceId] = signal.conversationId;
      return;
    }
    unawaited(_fetchDetail(signal.conversationId, signal.resourceId));
  }

  Future<void> _fetchDetail(String conversationId, String callId) async {
    try {
      final call = await _api.getCall(conversationId, callId);
      if (!_disposed) await _apply(call);
    } catch (_) {
      // A competing terminal transition may make a nudge stale; active REST sync recovers.
    }
  }

  Future<void> _apply(ConversationCall call) async {
    final userId = _currentUserId();
    if (_disposed || userId == null) return;
    var authoritative = call;
    if (call.callStatus == 'INITIATED' && call.initiatedByUserId != userId) {
      try {
        authoritative = await _api.markRinging(
          call.conversationId,
          call.callId,
        );
      } catch (_) {
        try {
          authoritative = await _api.getCall(call.conversationId, call.callId);
        } catch (_) {
          return;
        }
      }
    }
    _emit(
      reduceCallState(
        currentUserId: userId,
        call: authoritative,
        previous: _state,
      ),
    );
  }

  Future<void> initiate(String conversationId, String callType) async {
    final call = await _api.initiate(conversationId, callType);
    await _apply(call);
  }

  Future<ConversationCall?> answerCurrent() async {
    final call = _state.call;
    if (call == null) return null;
    _emit(DirectCallState(phase: DirectCallPhase.answering, call: call));
    try {
      final answered = await _api.answer(call.conversationId, call.callId);
      await _apply(answered);
      return answered;
    } catch (_) {
      await _fetchDetail(call.conversationId, call.callId);
      rethrow;
    }
  }

  Future<void> declineCurrent() async {
    final call = _state.call;
    if (call == null) return;
    await _apply(await _api.decline(call.conversationId, call.callId));
  }

  Future<void> endCurrent() async {
    final call = _state.call;
    if (call == null) return;
    try {
      await _apply(await _api.end(call.conversationId, call.callId));
    } catch (_) {
      await _fetchDetail(call.conversationId, call.callId);
    }
  }

  Future<ZegoJoinCredentials> issueJoinCredentials() {
    final call = _state.call;
    if (call == null || call.callStatus != 'ANSWERED') {
      throw StateError('Call is not ready to join');
    }
    return _api.issueJoinCredentials(call.conversationId, call.callId);
  }

  void markJoining() {
    if (_state.call != null) {
      _emit(DirectCallState(phase: DirectCallPhase.joining, call: _state.call));
    }
  }

  void markInCall() {
    if (_state.call != null) {
      _emit(DirectCallState(phase: DirectCallPhase.inCall, call: _state.call));
    }
  }

  void markReconnecting() {
    if (_state.call != null) {
      _emit(
        DirectCallState(phase: DirectCallPhase.reconnecting, call: _state.call),
      );
    }
  }

  void fail(String message) {
    _emit(
      DirectCallState(
        phase: DirectCallPhase.failed,
        call: _state.call,
        message: message,
      ),
    );
  }

  void _emit(DirectCallState next) {
    if (_disposed) return;
    _state = next;
    _states.add(next);
    _syncPollTimer(next);
  }

  void _syncPollTimer(DirectCallState state) {
    final shouldPoll =
        state.call != null &&
        state.phase != DirectCallPhase.idle &&
        state.phase != DirectCallPhase.terminal &&
        state.phase != DirectCallPhase.failed;
    if (!shouldPoll) {
      _pollTimer?.cancel();
      _pollTimer = null;
      return;
    }
    _pollTimer ??= Timer.periodic(_pollInterval, (_) => _pollActiveCall());
  }

  void _pollActiveCall() {
    final call = _state.call;
    if (_disposed || call == null) return;
    unawaited(_fetchDetail(call.conversationId, call.callId));
  }

  Future<void> dispose() async {
    if (_disposed) return;
    _disposed = true;
    _generation++;
    _pollTimer?.cancel();
    _pollTimer = null;
    await _signalSubscription?.cancel();
    unawaited(_states.close());
  }
}
