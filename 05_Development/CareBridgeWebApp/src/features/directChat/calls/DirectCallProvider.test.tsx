import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DirectCallProvider } from './DirectCallProvider';
import { useDirectCall } from './directCallContext';

const coordinatorMocks = vi.hoisted(() => ({
  answerCurrent: vi.fn(),
  initiate: vi.fn(),
  instance: null as {
    state: { phase: string; call: Record<string, unknown> | null };
  } | null,
  subscriber: null as ((state: unknown) => void) | null,
}));

vi.mock('../../../shared/auth/authStore', () => ({
  useAuthStore: Object.assign(
    (selector: (state: unknown) => unknown) =>
      selector({ user: { id: 'expert-1' }, accessToken: 'access-token' }),
    { getState: () => ({ user: { id: 'expert-1' } }) }
  ),
}));

vi.mock('../../../shared/integrations/firebaseRealtime/conversationSignalHub', () => ({
  conversationSignalHub: {
    start: vi.fn().mockResolvedValue(undefined),
    stop: vi.fn(),
  },
}));

vi.mock('./ringtonePlayer', () => ({
  ringtonePlayer: { start: vi.fn(), stop: vi.fn() },
}));

vi.mock('./zegoRoomSession', () => ({ mountZegoRoomSession: vi.fn() }));

vi.mock('./directCallCoordinator', () => ({
  DirectCallCoordinator: class {
    state = { phase: 'idle', call: null };

    constructor() {
      coordinatorMocks.instance = this;
    }

    subscribe(callback: (state: unknown) => void) {
      coordinatorMocks.subscriber = callback;
      return vi.fn();
    }

    start = vi.fn().mockResolvedValue(undefined);
    dispose = vi.fn();
    reconcileActiveCalls = vi.fn().mockResolvedValue(undefined);
    endCurrent = vi.fn().mockResolvedValue(undefined);
    declineCurrent = vi.fn().mockResolvedValue(undefined);
    setPhase = vi.fn();
    fail = vi.fn();
    issueJoinCredentialsForCurrent = vi.fn();
    initiate = coordinatorMocks.initiate;
    answerCurrent = coordinatorMocks.answerCurrent;
  },
}));

function CallStarter({ children }: PropsWithChildren) {
  const { initiate } = useDirectCall();
  return (
    <button type="button" onClick={() => void initiate('conversation-1', 'VIDEO')}>
      {children}
    </button>
  );
}

describe('DirectCallProvider recording consent', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    coordinatorMocks.instance = null;
    coordinatorMocks.subscriber = null;
  });

  it('does not initiate an outgoing call until the user agrees', async () => {
    render(
      <DirectCallProvider>
        <CallStarter>Gọi video</CallStarter>
      </DirectCallProvider>
    );

    fireEvent.click(screen.getByRole('button', { name: 'Gọi video' }));
    expect(screen.getByRole('dialog').textContent).toContain(
      'Cuộc gọi này sẽ được ghi âm/ghi hình nhằm đảm bảo chất lượng tư vấn y tế (Tuân thủ PDPA)'
    );
    expect(coordinatorMocks.initiate).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Không đồng ý' }));
    expect(coordinatorMocks.initiate).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Gọi video' }));
    fireEvent.click(screen.getByRole('button', { name: 'Đồng ý' }));

    await waitFor(() =>
      expect(coordinatorMocks.initiate).toHaveBeenCalledWith('conversation-1', 'VIDEO')
    );
  });

  it('does not answer an incoming call until the user agrees', async () => {
    render(<DirectCallProvider><div>Chat</div></DirectCallProvider>);
    const incomingCall = {
      callId: 'call-1',
      conversationId: 'conversation-1',
      initiatedByUserId: 'mother-1',
      callType: 'VOICE',
      callStatus: 'RINGING',
    };

    act(() => {
      if (coordinatorMocks.instance) {
        coordinatorMocks.instance.state = { phase: 'incoming', call: incomingCall };
      }
      coordinatorMocks.subscriber?.({ phase: 'incoming', call: incomingCall });
    });

    fireEvent.click(screen.getByRole('button', { name: 'Chấp nhận' }));
    expect(screen.getByRole('dialog').textContent).toContain('Tuân thủ PDPA');
    expect(coordinatorMocks.answerCurrent).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Đồng ý' }));
    await waitFor(() => expect(coordinatorMocks.answerCurrent).toHaveBeenCalledTimes(1));
  });
});
