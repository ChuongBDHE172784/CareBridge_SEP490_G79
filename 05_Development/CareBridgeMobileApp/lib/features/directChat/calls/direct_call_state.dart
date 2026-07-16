import '../models/conversation_call.dart';

enum DirectCallPhase {
  idle,
  outgoing,
  incoming,
  answering,
  readyToJoin,
  joining,
  inCall,
  reconnecting,
  terminal,
  failed,
}

class DirectCallState {
  final DirectCallPhase phase;
  final ConversationCall? call;
  final String? message;

  const DirectCallState({
    this.phase = DirectCallPhase.idle,
    this.call,
    this.message,
  });

  bool get isVideo => call?.callType == 'VIDEO';
}

const _terminalStatuses = {
  'DECLINED',
  'MISSED',
  'CANCELLED',
  'ENDED',
  'FAILED',
};

bool shouldCancelOutgoingOnDetach({
  required ConversationCall? call,
  required String? currentUserId,
}) {
  return call != null &&
      currentUserId != null &&
      call.initiatedByUserId == currentUserId &&
      (call.callStatus == 'INITIATED' || call.callStatus == 'RINGING');
}

DirectCallState reduceCallState({
  required String currentUserId,
  required ConversationCall call,
  DirectCallState? previous,
}) {
  if (previous?.call?.callId == call.callId &&
      _terminalStatuses.contains(previous!.call!.callStatus) &&
      !_terminalStatuses.contains(call.callStatus)) {
    return previous;
  }
  if (_terminalStatuses.contains(call.callStatus)) {
    return DirectCallState(phase: DirectCallPhase.terminal, call: call);
  }
  if (call.callStatus == 'ANSWERED') {
    if (previous?.call?.callId == call.callId &&
        {
          DirectCallPhase.joining,
          DirectCallPhase.inCall,
          DirectCallPhase.reconnecting,
        }.contains(previous!.phase)) {
      return DirectCallState(
        phase: previous.phase,
        call: call,
        message: previous.message,
      );
    }
    return DirectCallState(phase: DirectCallPhase.readyToJoin, call: call);
  }
  final isCaller = call.initiatedByUserId == currentUserId;
  return DirectCallState(
    phase: isCaller ? DirectCallPhase.outgoing : DirectCallPhase.incoming,
    call: call,
  );
}
