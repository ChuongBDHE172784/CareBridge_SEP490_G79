import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import './ChatPanel.css';
import { useAuthStore } from '../../../shared/auth/authStore';
import { ConversationSignalingPort } from '../../../shared/integrations/firebaseRealtime/conversationSignalingPort';
import * as directChatApi from '../services/directChatApi';
import { mergeTimelineItems, optimisticMessage, type TimelineItem } from '../models/timelineItem';

interface ChatPanelProps {
  conversationId: string;
}

function describeCall(item: TimelineItem): string {
  const kindLabel = item.callType === 'VIDEO' ? 'Cuộc gọi video' : 'Cuộc gọi thoại';
  switch (item.callStatus) {
    case 'ENDED':
      return `${kindLabel} — ${item.durationSeconds ?? 0}s`;
    case 'MISSED':
      return `${kindLabel} nhỡ`;
    case 'DECLINED':
      return `${kindLabel} bị từ chối`;
    case 'CANCELLED':
      return `${kindLabel} đã huỷ`;
    default:
      return kindLabel;
  }
}

export default function ChatPanel({ conversationId }: ChatPanelProps) {
  const currentUserId = useAuthStore((state) => state.user?.id);
  const [items, setItems] = useState<TimelineItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [expertAvailable, setExpertAvailable] = useState(true);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [hasMoreOlder, setHasMoreOlder] = useState(false);

  const nextCursorRef = useRef<string | null>(null);
  const previousCursorRef = useRef<string | null>(null);
  const signalingRef = useRef<ConversationSignalingPort | null>(null);
  const initialLoadCompleteRef = useRef(false);
  const syncingNewerRef = useRef(false);
  const pendingNewerSyncRef = useRef(false);
  const activeConversationRef = useRef(conversationId);

  useLayoutEffect(() => {
    activeConversationRef.current = conversationId;
    initialLoadCompleteRef.current = false;
    syncingNewerRef.current = false;
    pendingNewerSyncRef.current = false;
  }, [conversationId]);

  const syncNewer = useCallback(async () => {
    const requestedConversationId = conversationId;
    if (activeConversationRef.current !== requestedConversationId) return;
    if (!initialLoadCompleteRef.current || syncingNewerRef.current) {
      pendingNewerSyncRef.current = true;
      return;
    }
    syncingNewerRef.current = true;
    try {
      if (!nextCursorRef.current) {
        const page = await directChatApi.getTimeline(conversationId);
        if (activeConversationRef.current !== requestedConversationId) return;
        setItems((prev) => mergeTimelineItems(prev, page.items));
        nextCursorRef.current = page.nextCursor;
        previousCursorRef.current = page.previousCursor;
        setHasMoreOlder(page.hasMoreOlder);
        return;
      }
      let cursor: string | null = nextCursorRef.current;
      while (cursor) {
        const page = await directChatApi.getTimeline(conversationId, { after: cursor });
        if (activeConversationRef.current !== requestedConversationId) return;
        if (page.items.length > 0) {
          setItems((prev) => mergeTimelineItems(prev, page.items));
          nextCursorRef.current = page.nextCursor;
        }
        const next = page.nextCursor;
        if (!page.hasMoreNewer || !next || next === cursor) break;
        cursor = next;
      }
    } catch {
      // best-effort background sync — next resume/manual refresh retries.
    } finally {
      if (activeConversationRef.current === requestedConversationId) {
        syncingNewerRef.current = false;
        if (pendingNewerSyncRef.current) {
          pendingNewerSyncRef.current = false;
          queueMicrotask(() => void syncNewer());
        }
      }
    }
  }, [conversationId]);

  useEffect(() => {
    let cancelled = false;
    initialLoadCompleteRef.current = false;
    pendingNewerSyncRef.current = false;

    (async () => {
      try {
        const [conversation, page] = await Promise.all([
          directChatApi.getConversation(conversationId),
          directChatApi.getTimeline(conversationId),
        ]);
        if (cancelled) return;
        setItems(page.items);
        nextCursorRef.current = page.nextCursor;
        previousCursorRef.current = page.previousCursor;
        setHasMoreOlder(page.hasMoreOlder);
        setExpertAvailable(conversation.expertAvailable);
        if (pendingNewerSyncRef.current) {
          pendingNewerSyncRef.current = false;
          queueMicrotask(() => void syncNewer());
        }
      } catch (e) {
        if (!cancelled) setError(`Không thể tải cuộc trò chuyện: ${e}`);
      } finally {
        if (!cancelled) {
          initialLoadCompleteRef.current = true;
          setLoading(false);
          if (pendingNewerSyncRef.current) {
            pendingNewerSyncRef.current = false;
            queueMicrotask(() => void syncNewer());
          }
        }
      }
    })();

    const port = new ConversationSignalingPort();
    signalingRef.current = port;
    void port
      .connect((signal) => {
        if (signal.conversationId === conversationId) void syncNewer();
      })
      .catch(() => {
        // Firebase is transport-only. REST reload/resume remains the recovery path.
      });

    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') syncNewer();
    };
    document.addEventListener('visibilitychange', onVisibilityChange);

    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', onVisibilityChange);
      port.dispose();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversationId]);

  const loadOlder = async () => {
    if (loadingOlder || !hasMoreOlder || !previousCursorRef.current) return;
    setLoadingOlder(true);
    try {
      const page = await directChatApi.getTimeline(conversationId, { before: previousCursorRef.current });
      setItems((prev) => mergeTimelineItems(prev, page.items));
      previousCursorRef.current = page.previousCursor;
      setHasMoreOlder(page.hasMoreOlder);
    } catch (e) {
      setError(`Không thể tải thêm lịch sử: ${e}`);
    } finally {
      setLoadingOlder(false);
    }
  };

  const sendWithClientId = async (clientMessageId: string, body: string) => {
    try {
      const confirmed = await directChatApi.sendMessage(conversationId, clientMessageId, body);
      setItems((prev) => mergeTimelineItems(prev, [confirmed]));
    } catch {
      setItems((prev) =>
        prev.map((item) =>
          item.clientMessageId === clientMessageId ? { ...item, sendStatus: 'failed' } : item
        )
      );
    } finally {
      setSending(false);
    }
  };

  const handleSend = async () => {
    const body = draft.trim();
    if (!body || sending || !expertAvailable || !currentUserId) return;
    const clientMessageId = crypto.randomUUID();
    setItems((prev) =>
      mergeTimelineItems(prev, [optimisticMessage({ clientMessageId, senderUserId: currentUserId, messageBody: body })])
    );
    setDraft('');
    setSending(true);
    await sendWithClientId(clientMessageId, body);
  };

  const handleRetry = async (item: TimelineItem) => {
    if (!item.clientMessageId || !item.messageBody) return;
    setItems((prev) =>
      prev.map((i) => (i.clientMessageId === item.clientMessageId ? { ...i, sendStatus: 'sending' } : i))
    );
    setSending(true);
    // Same clientMessageId — idempotent retry (BR-DCC-005), never creates a duplicate.
    await sendWithClientId(item.clientMessageId, item.messageBody);
  };

  const handleCall = async (callType: 'VOICE' | 'VIDEO') => {
    try {
      await directChatApi.initiateCall(conversationId, callType);
      // Approved scope (TDS §1.1, CB-CHAT-IMP-144D v1.2): record + Firebase signaling only —
      // no live audio/video in this pass.
      window.alert(
        'Cuộc gọi đã được ghi nhận và gửi tín hiệu tới người nhận. Tính năng đàm thoại trực tiếp chưa được hỗ trợ trong bản này.'
      );
    } catch (e) {
      setError(`Không thể tạo cuộc gọi: ${e}`);
    }
  };

  if (loading) return <div className="chat-panel-loading">Đang tải...</div>;

  return (
    <div className="chat-panel">
      <div className="chat-panel-toolbar">
        <button disabled={!expertAvailable} onClick={() => handleCall('VOICE')}>
          Gọi thoại
        </button>
        <button disabled={!expertAvailable} onClick={() => handleCall('VIDEO')}>
          Gọi video
        </button>
      </div>

      {!expertAvailable && (
        <div className="chat-panel-banner">
          Chuyên gia hiện không khả dụng. Bạn vẫn có thể xem lại lịch sử trò chuyện.
        </div>
      )}
      {error && <div className="chat-panel-error">{error}</div>}

      <div className="chat-panel-messages">
        {hasMoreOlder && (
          <button disabled={loadingOlder} onClick={loadOlder}>
            {loadingOlder ? 'Đang tải...' : 'Tải thêm lịch sử'}
          </button>
        )}
        {items.map((item) => {
          if (item.kind === 'CALL_EVENT') {
            return (
              <div key={`call-${item.callId}`} className="chat-panel-call-event">
                {describeCall(item)}
              </div>
            );
          }
          const isOwn = item.senderUserId === currentUserId;
          const key = item.messageId ?? item.clientMessageId;
          return (
            <div key={key} className={`chat-panel-message ${isOwn ? 'own' : 'other'}`}>
              <div className="chat-panel-message-bubble">{item.messageBody}</div>
              {item.sendStatus === 'sending' && <span className="chat-panel-status">Đang gửi...</span>}
              {item.sendStatus === 'failed' && (
                <button className="chat-panel-retry" onClick={() => handleRetry(item)}>
                  Gửi lại
                </button>
              )}
            </div>
          );
        })}
      </div>

      {expertAvailable && (
        <div className="chat-panel-input">
          <input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Nhập tin nhắn..."
          />
          <button disabled={sending} onClick={handleSend}>
            Gửi
          </button>
        </div>
      )}
    </div>
  );
}
