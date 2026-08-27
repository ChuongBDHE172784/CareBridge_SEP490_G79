import { describe, expect, it } from 'vitest';
import type { ConversationCall } from '../models/directConversation';
import { reduceDirectCallState } from './directCallState';

const call = (callStatus: string): ConversationCall => ({
  callId: 'call-1',
  conversationId: 'conversation-1',
  initiatedByUserId: 'mother-1',
  callType: 'VOICE',
  callStatus,
  initiatedAt: '2026-07-16T00:00:00Z',
  answeredAt: callStatus === 'ANSWERED' ? '2026-07-16T00:01:00Z' : null,
  endedAt: callStatus === 'ENDED' ? '2026-07-16T00:02:00Z' : null,
  durationSeconds: callStatus === 'ENDED' ? 60 : null,
});

describe('reduceDirectCallState', () => {
  it('maps authoritative caller and callee state', () => {
    expect(reduceDirectCallState('mother-1', call('RINGING')).phase).toBe('outgoing');
    expect(reduceDirectCallState('expert-1', call('RINGING')).phase).toBe('incoming');
    expect(reduceDirectCallState('expert-1', call('ANSWERED')).phase).toBe('readyToJoin');
  });

  it('does not regress a terminal call from duplicate or delayed signaling', () => {
    const terminal = reduceDirectCallState('mother-1', call('ENDED'));
    expect(reduceDirectCallState('mother-1', call('RINGING'), terminal)).toBe(terminal);
  });

  it('keeps the local RTC phase when REST reconciles the same answered call', () => {
    const reconnecting = { phase: 'reconnecting' as const, call: call('ANSWERED') };
    expect(
      reduceDirectCallState('mother-1', call('ANSWERED'), reconnecting).phase
    ).toBe('reconnecting');
  });
});
