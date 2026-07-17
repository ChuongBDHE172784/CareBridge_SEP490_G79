import type { ConversationCall } from '../models/directConversation';

export type DirectCallPhase =
  | 'idle'
  | 'outgoing'
  | 'incoming'
  | 'answering'
  | 'readyToJoin'
  | 'joining'
  | 'inCall'
  | 'reconnecting'
  | 'terminal'
  | 'failed';

export interface DirectCallState {
  phase: DirectCallPhase;
  call: ConversationCall | null;
  message?: string;
}

export const idleDirectCallState: DirectCallState = {
  phase: 'idle',
  call: null,
};

const terminalStatuses = new Set([
  'DECLINED',
  'MISSED',
  'CANCELLED',
  'ENDED',
  'FAILED',
]);

export function reduceDirectCallState(
  currentUserId: string,
  call: ConversationCall,
  previous: DirectCallState = idleDirectCallState
): DirectCallState {
  if (
    previous.call?.callId === call.callId &&
    terminalStatuses.has(previous.call.callStatus) &&
    !terminalStatuses.has(call.callStatus)
  ) {
    return previous;
  }
  if (terminalStatuses.has(call.callStatus)) {
    return { phase: 'terminal', call };
  }
  if (call.callStatus === 'ANSWERED') {
    if (
      previous.call?.callId === call.callId &&
      ['joining', 'inCall', 'reconnecting'].includes(previous.phase)
    ) {
      return { ...previous, call };
    }
    return { phase: 'readyToJoin', call };
  }
  return {
    phase: call.initiatedByUserId === currentUserId ? 'outgoing' : 'incoming',
    call,
  };
}
