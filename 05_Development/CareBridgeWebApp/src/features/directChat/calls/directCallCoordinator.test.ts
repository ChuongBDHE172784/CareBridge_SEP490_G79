import { describe, expect, it, vi } from 'vitest';
import type { ConversationEventSignal } from '../../../shared/integrations/firebaseRealtime/conversationEventSignal';
import type { ConversationCall, ZegoJoinCredentials } from '../models/directConversation';
import type { DirectCallApiPort } from './directCallApi';
import { DirectCallCoordinator } from './directCallCoordinator';

const call = (callStatus: string, callId = 'call-1'): ConversationCall => ({
  callId,
  conversationId: 'conversation-1',
  initiatedByUserId: 'mother-1',
  callType: 'VIDEO',
  callStatus,
  initiatedAt: '2026-07-16T00:00:00Z',
  answeredAt: callStatus === 'ANSWERED' ? '2026-07-16T00:01:00Z' : null,
  endedAt: null,
  durationSeconds: null,
});

const signal = (eventId: string, callId = 'call-1'): ConversationEventSignal => ({
  eventId,
  eventType: 'CALL_STATE_CHANGED',
  conversationId: 'conversation-1',
  resourceId: callId,
  occurredAt: '2026-07-16T00:00:00Z',
});

class FakeSignals {
  listener?: (event: ConversationEventSignal) => void;
  unsubscribe = vi.fn();

  subscribe(listener: (event: ConversationEventSignal) => void) {
    this.listener = listener;
    return this.unsubscribe;
  }
}

class FakeApi implements DirectCallApiPort {
  active = Promise.resolve<ConversationCall[]>([]);
  detail = call('RINGING');
  credentials = Promise.resolve<ZegoJoinCredentials>({
    appId: 1,
    roomId: 'cb_room',
    userId: 'u_mother',
    displayName: 'Mother',
    token: 'token',
    expiresAt: '2026-07-16T01:00:00Z',
  });
  getCall = vi.fn(async () => this.detail);
  listActiveCalls = vi.fn(() => this.active);
  markRinging = vi.fn(async () => this.detail);
  initiate = vi.fn(async () => this.detail);
  answer = vi.fn(async () => call('ANSWERED'));
  decline = vi.fn(async () => call('DECLINED'));
  end = vi.fn(async () => call('ENDED'));
  issueJoinCredentials = vi.fn(() => this.credentials);
}

describe('DirectCallCoordinator', () => {
  it('reconciles a signal arriving during initial sync once and deduplicates replay', async () => {
    let resolveActive!: (calls: ConversationCall[]) => void;
    const api = new FakeApi();
    api.active = new Promise((resolve) => {
      resolveActive = resolve;
    });
    const signals = new FakeSignals();
    const coordinator = new DirectCallCoordinator(api, signals, () => 'expert-1');

    const starting = coordinator.start();
    signals.listener?.(signal('event-1'));
    signals.listener?.(signal('event-1'));
    resolveActive([call('RINGING')]);
    await starting;
    await vi.waitFor(() => expect(coordinator.state.phase).toBe('incoming'));

    expect(api.getCall.mock.calls.length).toBeLessThanOrEqual(1);
  });

  it('ignores stale credential results after another call becomes current', async () => {
    let resolveCredentials!: (value: ZegoJoinCredentials) => void;
    const api = new FakeApi();
    api.active = Promise.resolve([call('ANSWERED')]);
    api.credentials = new Promise((resolve) => {
      resolveCredentials = resolve;
    });
    const signals = new FakeSignals();
    const coordinator = new DirectCallCoordinator(api, signals, () => 'mother-1');
    await coordinator.start();

    const pending = coordinator.issueJoinCredentialsForCurrent();
    api.detail = call('RINGING', 'call-2');
    signals.listener?.(signal('event-2', 'call-2'));
    await vi.waitFor(() => expect(coordinator.state.call?.callId).toBe('call-2'));
    resolveCredentials({
      appId: 1,
      roomId: 'cb_old',
      userId: 'u_mother',
      displayName: 'Mother',
      token: 'old-token',
      expiresAt: '2026-07-16T01:00:00Z',
    });

    await expect(pending).resolves.toBeNull();
  });

  it('unsubscribes and ignores later events on dispose', async () => {
    const api = new FakeApi();
    const signals = new FakeSignals();
    const coordinator = new DirectCallCoordinator(api, signals, () => 'mother-1');
    await coordinator.start();

    coordinator.dispose();
    signals.listener?.(signal('event-after-dispose'));
    await Promise.resolve();

    expect(signals.unsubscribe).toHaveBeenCalledOnce();
    expect(api.getCall).not.toHaveBeenCalled();
  });
});
