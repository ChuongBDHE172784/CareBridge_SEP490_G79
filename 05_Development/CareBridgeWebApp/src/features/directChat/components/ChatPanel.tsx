import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ChatPanel.css';
import { useAuthStore } from '../../../shared/auth/authStore';
import { conversationSignalHub } from '../../../shared/integrations/firebaseRealtime/conversationSignalHub';
import * as directChatApi from '../services/directChatApi';
import { mergeTimelineItems, optimisticMessage, type TimelineItem } from '../models/timelineItem';
import { useDirectCall } from '../calls/directCallContext';

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
  const navigate = useNavigate();
  const currentUserId = useAuthStore((state) => state.user?.id);
  const { initiate } = useDirectCall();
  const [items, setItems] = useState<TimelineItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [expertAvailable, setExpertAvailable] = useState(true);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [hasMoreOlder, setHasMoreOlder] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const nextCursorRef = useRef<string | null>(null);
  const previousCursorRef = useRef<string | null>(null);
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

    const unsubscribeSignal = conversationSignalHub.subscribe((signal) => {
      if (signal.conversationId === conversationId) void syncNewer();
    });

    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') syncNewer();
    };
    document.addEventListener('visibilitychange', onVisibilityChange);

    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', onVisibilityChange);
      unsubscribeSignal();
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

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [items]);

  const handleCall = async (callType: 'VOICE' | 'VIDEO') => {
    try {
      await initiate(conversationId, callType);
    } catch (e) {
      setError(`Không thể tạo cuộc gọi: ${e}`);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-on-surface-variant text-sm gap-2">
        <span className="material-symbols-outlined animate-spin text-primary text-xl">progress_activity</span>
        Đang tải cuộc trò chuyện...
      </div>
    );
  }

  return (
    <div className="flex flex-col space-y-5">
      {/* Top Header Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-2 border-b border-outline-variant/60">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center justify-center w-10 h-10 rounded-full border border-outline-variant bg-surface text-on-surface hover:bg-surface-container-low transition-colors shrink-0 cursor-pointer shadow-sm"
            title="Quay lại"
          >
            <span className="material-symbols-outlined text-xl">arrow_back</span>
          </button>
          
          <div className="w-11 h-11 rounded-full bg-primary-container text-primary flex items-center justify-center font-bold text-base shrink-0 shadow-sm">
            M
          </div>

          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-on-surface m-0 leading-tight">
                Tư vấn Mẹ bầu CareBridge
              </h1>
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold border bg-emerald-50 text-emerald-700 border-emerald-300">
                <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                Trực tuyến
              </span>
            </div>
            <p className="text-xs text-on-surface-variant mt-0.5 m-0">
              Mã phòng chat: <span className="font-mono text-outline">{conversationId.slice(0, 8)}...</span>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 self-start md:self-auto">
          <button
            disabled={!expertAvailable}
            onClick={() => handleCall('VOICE')}
            className="flex items-center gap-2 py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-primary text-[13px] font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
          >
            <span className="material-symbols-outlined text-lg">call</span>
            Gọi thoại
          </button>
          <button
            disabled={!expertAvailable}
            onClick={() => handleCall('VIDEO')}
            className="flex items-center gap-2 py-2.5 px-5 rounded-full bg-primary text-on-primary border-0 text-[13px] font-semibold cursor-pointer hover:brightness-110 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-md"
          >
            <span className="material-symbols-outlined text-lg">videocam</span>
            Gọi video
          </button>
        </div>
      </div>

      {/* Main Chat Box Container */}
      <div className="bg-surface rounded-2xl shadow-md border border-outline-variant/60 flex flex-col h-[calc(100vh-220px)] overflow-hidden">
        {!expertAvailable && (
          <div className="bg-amber-50 border-b border-amber-200 text-amber-900 px-4 py-3 text-xs flex items-center gap-2 shrink-0 font-medium">
            <span className="material-symbols-outlined text-lg text-amber-700">warning</span>
            Chuyên gia hiện không khả dụng. Bạn vẫn có thể xem lại lịch sử trò chuyện.
          </div>
        )}

        {error && (
          <div className="bg-rose-50 border-b border-rose-200 text-rose-800 px-4 py-3 text-xs flex items-center justify-between shrink-0 font-medium">
            <span>{error}</span>
            <button onClick={() => setError(null)} className="text-rose-600 hover:underline">Đóng</button>
          </div>
        )}

        {/* Timeline Items list */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4 bg-surface-container-low/30">
          {hasMoreOlder && (
            <div className="text-center my-2">
              <button
                disabled={loadingOlder}
                onClick={loadOlder}
                className="py-1.5 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-primary hover:bg-surface-container-low transition-colors shadow-sm"
              >
                {loadingOlder ? 'Đang tải...' : 'Tải thêm lịch sử cũ hơn'}
              </button>
            </div>
          )}

          {items.map((item) => {
            if (item.kind === 'CALL_EVENT') {
              return (
                <div key={`call-${item.callId}`} className="flex justify-center my-3">
                  <span className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-medium bg-surface-container-highest/80 text-on-surface-variant border border-outline-variant/50 shadow-xs">
                    <span className="material-symbols-outlined text-sm">phone_in_talk</span>
                    {describeCall(item)}
                  </span>
                </div>
              );
            }

            const isOwn = item.senderUserId === currentUserId;
            const key = item.messageId ?? item.clientMessageId;

            return (
              <div key={key} className={`flex flex-col ${isOwn ? 'items-end' : 'items-start'} space-y-1`}>
                <div className={`flex items-end gap-2 max-w-[75%] ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}>
                  {!isOwn && (
                    <div className="w-8 h-8 rounded-full bg-primary-container text-primary flex items-center justify-center text-xs font-bold shrink-0 mb-0.5 shadow-xs">
                      M
                    </div>
                  )}
                  <div
                    className={`p-3.5 rounded-2xl text-sm leading-relaxed shadow-sm ${
                      isOwn
                        ? 'bg-primary text-on-primary rounded-tr-xs'
                        : 'bg-surface border border-outline-variant/60 text-on-surface rounded-tl-xs'
                    }`}
                  >
                    {item.messageBody}
                  </div>
                </div>

                {isOwn && item.sendStatus === 'sending' && (
                  <span className="text-[11px] text-outline px-1">Đang gửi...</span>
                )}
                {isOwn && item.sendStatus === 'failed' && (
                  <button
                    className="text-[11px] font-bold text-rose-600 hover:underline px-1 flex items-center gap-1 cursor-pointer"
                    onClick={() => handleRetry(item)}
                  >
                    <span className="material-symbols-outlined text-xs">refresh</span> Gửi lại
                  </button>
                )}
              </div>
            );
          })}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        {expertAvailable && (
          <div className="p-4 bg-surface border-t border-outline-variant/60 flex items-center gap-3 shrink-0">
            <div className="relative flex-1">
              <input
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                placeholder="Nhập tin nhắn tư vấn cho mẹ bầu..."
                className="w-full py-3 px-5 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all font-sans"
              />
            </div>
            <button
              disabled={sending || !draft.trim()}
              onClick={handleSend}
              className="w-11 h-11 rounded-full bg-primary text-on-primary flex items-center justify-center hover:brightness-110 disabled:opacity-40 disabled:cursor-not-allowed transition-all shrink-0 cursor-pointer shadow-md"
              title="Gửi tin nhắn"
            >
              <span className="material-symbols-outlined text-xl">send</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
