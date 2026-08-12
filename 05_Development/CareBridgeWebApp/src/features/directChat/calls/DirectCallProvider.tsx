import {
  type PropsWithChildren,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import { useAuthStore } from '../../../shared/auth/authStore';
import { conversationSignalHub } from '../../../shared/integrations/firebaseRealtime/conversationSignalHub';
import type { ZegoJoinCredentials } from '../models/directConversation';
import { directCallApi } from './directCallApi';
import { DirectCallCoordinator } from './directCallCoordinator';
import { DirectCallContext } from './directCallContext';
import {
  idleDirectCallState,
  type DirectCallState,
} from './directCallState';
import {
  mediaErrorMessage,
  requestRtcMediaPermission,
  type MediaDevicesPort,
} from './rtcMediaPermissions';
import { mountZegoRoomSession } from './zegoRoomSession';
import './DirectCallProvider.css';

import { ringtonePlayer } from './ringtonePlayer';

export function DirectCallProvider({ children }: PropsWithChildren) {
  const userId = useAuthStore((state) => state.user?.id);
  const accessToken = useAuthStore((state) => state.accessToken);
  const [state, setState] = useState<DirectCallState>(idleDirectCallState);
  const [credentials, setCredentials] = useState<ZegoJoinCredentials | null>(null);
  const [joining, setJoining] = useState(false);
  const [rtcError, setRtcError] = useState<string | null>(null);
  const coordinatorRef = useRef<DirectCallCoordinator | null>(null);
  const renewTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const rtcContainerRef = useRef<HTMLDivElement | null>(null);
  const joinGenerationRef = useRef(0);

  const clearRtc = useCallback(() => {
    joinGenerationRef.current += 1;
    if (renewTimerRef.current) clearTimeout(renewTimerRef.current);
    renewTimerRef.current = null;
    setCredentials(null);
    setJoining(false);
    ringtonePlayer.stop();
  }, []);

  useEffect(() => {
    if (!userId || !accessToken) {
      clearRtc();
      setState(idleDirectCallState);
      return;
    }

    const coordinator = new DirectCallCoordinator(
      directCallApi,
      conversationSignalHub,
      () => useAuthStore.getState().user?.id
    );
    coordinatorRef.current = coordinator;
    const unsubscribeState = coordinator.subscribe((next) => {
      setState(next);
      if (next.phase === 'outgoing') {
        ringtonePlayer.start('outgoing');
      } else if (next.phase === 'incoming') {
        ringtonePlayer.start('incoming');
      } else {
        ringtonePlayer.stop();
      }
      if (next.phase === 'terminal') {
        sessionStorage.removeItem('carebridge-active-call-id');
        clearRtc();
      }
    });
    void conversationSignalHub.start();
    void coordinator.start();

    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') void coordinator.reconcileActiveCalls();
    };
    const onOffline = () => {
      if (coordinator.state.phase === 'inCall') coordinator.setPhase('reconnecting');
    };
    const onOnline = () => {
      void coordinator.reconcileActiveCalls();
    };
    const onPageHide = () => {
      const call = coordinator.state.call;
      if (
        call &&
        call.initiatedByUserId === userId &&
        (call.callStatus === 'INITIATED' || call.callStatus === 'RINGING')
      ) {
        void coordinator.endCurrent();
      }
    };
    document.addEventListener('visibilitychange', onVisibilityChange);
    window.addEventListener('offline', onOffline);
    window.addEventListener('online', onOnline);
    window.addEventListener('pagehide', onPageHide);

    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('offline', onOffline);
      window.removeEventListener('online', onOnline);
      window.removeEventListener('pagehide', onPageHide);
      unsubscribeState();
      coordinator.dispose();
      if (coordinatorRef.current === coordinator) coordinatorRef.current = null;
      conversationSignalHub.stop();
      clearRtc();
    };
  }, [accessToken, clearRtc, userId]);

  const beginJoin = useCallback(async () => {
    const coordinator = coordinatorRef.current;
    const call = coordinator?.state.call;
    if (!coordinator || !call || call.callStatus !== 'ANSWERED' || joining) return;
    const generation = ++joinGenerationRef.current;
    setJoining(true);
    setRtcError(null);
    coordinator.setPhase('joining');
    try {
      await requestRtcMediaPermission(
        call.callType === 'VIDEO',
        navigator.mediaDevices as unknown as MediaDevicesPort | undefined
      );
      const issued = await coordinator.issueJoinCredentialsForCurrent();
      if (
        generation !== joinGenerationRef.current ||
        !issued ||
        coordinator.state.call?.callId !== call.callId
      ) {
        return;
      }
      setCredentials(issued);
    } catch (error) {
      if (generation !== joinGenerationRef.current) return;
      setRtcError(mediaErrorMessage(error));
      coordinator.fail('RTC join failed');
    } finally {
      if (generation === joinGenerationRef.current) setJoining(false);
    }
  }, [joining]);

  useEffect(() => {
    const call = state.call;
    const container = rtcContainerRef.current;
    if (!credentials || !call || !container) return;

    return mountZegoRoomSession({
      call,
      credentials,
      container,
      onJoin: () => {
        coordinatorRef.current?.setPhase('inCall');
        sessionStorage.setItem('carebridge-active-call-id', call.callId);
        const renewInMs = Math.max(
          1_000,
          new Date(credentials.expiresAt).getTime() - Date.now() - 5 * 60_000
        );
        renewTimerRef.current = setTimeout(async () => {
          const coordinator = coordinatorRef.current;
          if (!coordinator || coordinator.state.call?.callId !== call.callId) return;
          coordinator.setPhase('reconnecting');
          try {
            const renewed = await coordinator.issueJoinCredentialsForCurrent();
            if (renewed && coordinator.state.call?.callId === call.callId) {
              setCredentials(renewed);
            }
          } catch {
            setRtcError('Phiên RTC sắp hết hạn. Hãy rời và tham gia lại cuộc gọi.');
          }
        }, renewInMs);
      },
      onLeave: () => {
        sessionStorage.removeItem('carebridge-active-call-id');
        clearRtc();
        void coordinatorRef.current?.endCurrent();
      },
      onError: () => {
        setRtcError('Không thể khởi tạo ZEGOCLOUD Call Kit.');
        coordinatorRef.current?.fail('RTC SDK initialization failed');
      },
    });
  }, [clearRtc, credentials, state.call]);

  const initiate = useCallback(
    async (conversationId: string, callType: 'VOICE' | 'VIDEO') => {
      setRtcError(null);
      await coordinatorRef.current?.initiate(conversationId, callType);
    },
    []
  );

  const accept = async () => {
    try {
      await coordinatorRef.current?.answerCurrent();
      await beginJoin();
    } catch {
      setRtcError('Cuộc gọi đã được xử lý ở thiết bị khác.');
    }
  };

  const end = async () => {
    clearRtc();
    await coordinatorRef.current?.endCurrent();
  };

  const call = state.call;
  const incoming = state.phase === 'incoming';
  const ready = state.phase === 'readyToJoin' || state.phase === 'failed';
  const showPrompt =
    call &&
    !credentials &&
    state.phase !== 'idle' &&
    state.phase !== 'terminal';

  return (
    <DirectCallContext.Provider value={{ initiate }}>
      {children}
      {showPrompt && (
        <div className="direct-call-overlay" role="dialog" aria-modal="true">
          <div className="direct-call-card">
            <h2>{call.callType === 'VIDEO' ? 'Cuộc gọi video' : 'Cuộc gọi thoại'}</h2>
            <p>
              {incoming
                ? 'Cuộc gọi đến'
                : ready
                  ? 'Cuộc gọi đã được trả lời'
                  : state.phase === 'answering'
                    ? 'Đang trả lời…'
                    : 'Đang đổ chuông…'}
            </p>
            {rtcError && <p className="direct-call-error">{rtcError}</p>}
            <div className="direct-call-actions">
              {incoming && <button onClick={() => void accept()}>Chấp nhận</button>}
              {incoming && (
                <button className="secondary" onClick={() => void coordinatorRef.current?.declineCurrent()}>
                  Từ chối
                </button>
              )}
              {ready && (
                <button disabled={joining} onClick={() => void beginJoin()}>
                  {joining ? 'Đang kết nối…' : 'Tham gia cuộc gọi'}
                </button>
              )}
              {!incoming && (
                <button className="secondary" onClick={() => void end()}>
                  {ready ? 'Kết thúc' : 'Huỷ'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
      {credentials && call && (
        <div className="direct-call-room">
          <div ref={rtcContainerRef} className="direct-call-room-container" />
          <div className="direct-call-room-status">
            {state.phase === 'reconnecting' && 'Đang kết nối lại…'}
            {rtcError ?? 'Nếu trình duyệt chặn âm thanh, hãy tương tác với màn hình cuộc gọi.'}
          </div>
        </div>
      )}
    </DirectCallContext.Provider>
  );
}
