import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/calls/direct_call_state.dart';
import 'package:untitled/features/directChat/models/conversation_call.dart';

ConversationCall _call({
  String status = 'INITIATED',
  String initiator = 'mother-1',
  String type = 'VOICE',
}) {
  return ConversationCall(
    callId: 'call-1',
    conversationId: 'conversation-1',
    initiatedByUserId: initiator,
    callType: type,
    callStatus: status,
    initiatedAt: DateTime.utc(2026, 7, 16),
  );
}

void main() {
  test('maps caller and callee projections from authoritative REST state', () {
    expect(
      reduceCallState(
        currentUserId: 'mother-1',
        call: _call(status: 'INITIATED'),
      ).phase,
      DirectCallPhase.outgoing,
    );
    expect(
      reduceCallState(
        currentUserId: 'expert-1',
        call: _call(status: 'RINGING'),
      ).phase,
      DirectCallPhase.incoming,
    );
    expect(
      reduceCallState(
        currentUserId: 'mother-1',
        call: _call(status: 'ANSWERED'),
      ).phase,
      DirectCallPhase.readyToJoin,
    );
  });

  test(
    'terminal REST state cannot regress to ringing from a delayed signal',
    () {
      final terminal = reduceCallState(
        currentUserId: 'mother-1',
        call: _call(status: 'ENDED'),
      );
      final delayed = reduceCallState(
        currentUserId: 'mother-1',
        call: _call(status: 'RINGING'),
        previous: terminal,
      );

      expect(delayed.phase, DirectCallPhase.terminal);
      expect(delayed.call?.callStatus, 'ENDED');
    },
  );

  test('duplicate call updates retain one state keyed by callId', () {
    final first = reduceCallState(
      currentUserId: 'expert-1',
      call: _call(status: 'RINGING'),
    );
    final duplicate = reduceCallState(
      currentUserId: 'expert-1',
      call: _call(status: 'RINGING'),
      previous: first,
    );

    expect(duplicate.call?.callId, 'call-1');
    expect(duplicate.phase, DirectCallPhase.incoming);
  });

  test('voice and video preserve media type for RTC configuration', () {
    expect(
      reduceCallState(
        currentUserId: 'mother-1',
        call: _call(type: 'VOICE'),
      ).isVideo,
      isFalse,
    );
    expect(
      reduceCallState(
        currentUserId: 'mother-1',
        call: _call(type: 'VIDEO'),
      ).isVideo,
      isTrue,
    );
  });

  test('answered REST reconcile preserves local reconnecting phase', () {
    final answered = _call(status: 'ANSWERED');
    final previous = DirectCallState(
      phase: DirectCallPhase.reconnecting,
      call: answered,
    );

    final reconciled = reduceCallState(
      currentUserId: 'mother-1',
      call: answered,
      previous: previous,
    );

    expect(reconciled.phase, DirectCallPhase.reconnecting);
  });

  test('app detach cancels only the caller before answer', () {
    expect(
      shouldCancelOutgoingOnDetach(
        call: _call(status: 'RINGING'),
        currentUserId: 'mother-1',
      ),
      isTrue,
    );
    expect(
      shouldCancelOutgoingOnDetach(
        call: _call(status: 'ANSWERED'),
        currentUserId: 'mother-1',
      ),
      isFalse,
    );
    expect(
      shouldCancelOutgoingOnDetach(
        call: _call(status: 'INITIATED'),
        currentUserId: 'expert-1',
      ),
      isFalse,
    );
  });
}
