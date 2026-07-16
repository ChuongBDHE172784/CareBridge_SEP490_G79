import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/calls/direct_call_api.dart';
import 'package:untitled/features/directChat/calls/direct_call_coordinator.dart';
import 'package:untitled/features/directChat/calls/direct_call_state.dart';
import 'package:untitled/features/directChat/models/conversation_call.dart';
import 'package:untitled/features/directChat/models/zego_join_credentials.dart';
import 'package:untitled/integrations/firebaseRealtime/conversation_event_signal.dart';

class _FakeCallApi implements DirectCallApiPort {
  final activeCompleter = Completer<List<ConversationCall>>();
  final detailRequests = <String>[];
  ConversationCall? detail;

  @override
  Future<List<ConversationCall>> listActiveCalls() => activeCompleter.future;

  @override
  Future<ConversationCall> getCall(String conversationId, String callId) async {
    detailRequests.add(callId);
    return detail!;
  }

  @override
  Future<ConversationCall> initiate(String conversationId, String callType) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> markRinging(
    String conversationId,
    String callId,
  ) async => detail!;

  @override
  Future<ConversationCall> answer(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> decline(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> end(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ZegoJoinCredentials> issueJoinCredentials(
    String conversationId,
    String callId,
  ) => throw UnimplementedError();
}

ConversationCall _call(String status) => ConversationCall(
  callId: 'call-1',
  conversationId: 'conversation-1',
  initiatedByUserId: 'mother-1',
  callType: 'VOICE',
  callStatus: status,
  initiatedAt: DateTime.utc(2026, 7, 16),
);

ConversationEventSignal _signal(String eventId) => ConversationEventSignal(
  eventId: eventId,
  eventType: 'CALL_STATE_CHANGED',
  conversationId: 'conversation-1',
  resourceId: 'call-1',
  occurredAt: DateTime.utc(2026, 7, 16),
);

void main() {
  test(
    'signal during initial sync is reconciled once without duplicate overlay',
    () async {
      final api = _FakeCallApi()..detail = _call('RINGING');
      final signals = StreamController<ConversationEventSignal>.broadcast();
      final coordinator = DirectCallCoordinator(
        api: api,
        signals: signals.stream,
        currentUserId: () => 'expert-1',
      );

      final start = coordinator.start();
      signals.add(_signal('event-1'));
      api.activeCompleter.complete([_call('RINGING')]);
      await start;
      await Future<void>.delayed(Duration.zero);

      expect(coordinator.state.phase, DirectCallPhase.incoming);
      expect(coordinator.state.call?.callId, 'call-1');
      expect(api.detailRequests.length, lessThanOrEqualTo(1));
      await coordinator.dispose();
      await signals.close();
    },
  );

  test(
    'duplicate and replayed signals cannot regress a terminal REST state',
    () async {
      final api = _FakeCallApi()..detail = _call('ENDED');
      final signals = StreamController<ConversationEventSignal>.broadcast();
      final coordinator = DirectCallCoordinator(
        api: api,
        signals: signals.stream,
        currentUserId: () => 'mother-1',
      );
      api.activeCompleter.complete(const []);
      await coordinator.start();

      signals
        ..add(_signal('event-1'))
        ..add(_signal('event-1'));
      await Future<void>.delayed(Duration.zero);
      await Future<void>.delayed(Duration.zero);

      expect(coordinator.state.phase, DirectCallPhase.terminal);
      expect(coordinator.state.call?.callStatus, 'ENDED');
      await coordinator.dispose();
      await signals.close();
    },
  );
}
