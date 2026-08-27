import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ChatPanel.css';
import { useAuthStore } from '../../../shared/auth/authStore';
import { conversationSignalHub } from '../../../shared/integrations/firebaseRealtime/conversationSignalHub';
import * as directChatApi from '../services/directChatApi';
import type { DirectConversation } from '../models/directConversation';
import { mergeTimelineItems, optimisticMessage, type TimelineItem } from '../models/timelineItem';
import { useDirectCall } from '../calls/directCallContext';
import LocationMessageBubble from './LocationMessageBubble';
import { SharedHealthMetricsBubble } from './SharedHealthMetricsBubble';
import { SharedChecklistBubble } from './SharedChecklistBubble';
import { parseHealthMetricsShare, parseChecklistShare } from '../../expert/services/expertSharedRecordsService';

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

function formatTimestamp(value?: string): string {
  if (!value) return '';
  const date = new Date(value);
  const now = new Date();
  const time = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  return date.toDateString() === now.toDateString()
    ? time
    : `${date.toLocaleDateString('vi-VN')} · ${time}`;
}

// Kept in step with what the API accepts. .txt is deliberately absent: the server has
// no MIME rule for it and rejects it with a 415 the sender cannot act on. Documents and
// spreadsheets alike are stored privately in R2; the server decides that, not this list.
const DOCUMENT_ACCEPT =
  '.pdf,.doc,.docx,.xls,.xlsx,' +
  'application/pdf,application/msword,' +
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document,' +
  'application/vnd.ms-excel,' +
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

async function downloadAttachment(url: string, name: string) {
  const response = await fetch(url);
  if (!response.ok) throw new Error('Download failed');
  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = name;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(objectUrl);
}

function AttachmentBubble({
  item,
  conversationId,
  isOwn,
  onPreview,
  onRecall,
  onError,
}: {
  item: TimelineItem;
  conversationId: string;
  isOwn: boolean;
  onPreview: (url: string, name: string) => void;
  onRecall: (messageId: string) => void;
  onError: (message: string) => void;
}) {
  const [attachment, setAttachment] = useState<directChatApi.DirectChatAttachment | null>(null);
  const [failed, setFailed] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    if (!item.messageId || !item.attachmentId || item.recalledAt) return;
    let disposed = false;
    setFailed(false);

    // The first look-up can land before the message row the server just wrote is
    // readable, and a single failure used to leave a spinner that never resolved -
    // the file only appeared after a full page reload. Retry a couple of times with
    // a short backoff, then offer the retry to the sender instead of spinning.
    const attempt = (remaining: number, delayMs: number) => {
      directChatApi.viewAttachment(conversationId, item.messageId!)
        .then((result) => { if (!disposed) setAttachment(result); })
        .catch(() => {
          if (disposed) return;
          if (remaining > 0) {
            window.setTimeout(() => attempt(remaining - 1, delayMs * 2), delayMs);
            return;
          }
          setFailed(true);
          onError('Không thể tải tệp đính kèm.');
        });
    };
    attempt(2, 600);

    return () => { disposed = true; };
  }, [conversationId, item.attachmentId, item.messageId, item.recalledAt, onError, reloadToken]);

  if (item.recalledAt) {
    return <span className="italic text-on-surface-variant">Tin nhắn đã được thu hồi</span>;
  }
  if (!attachment) {
    return failed ? (
      <button
        type="button"
        onClick={() => setReloadToken((n) => n + 1)}
        className="inline-flex min-w-36 items-center gap-2 underline"
      >
        <span className="material-symbols-outlined">refresh</span>Tải lại tệp
      </button>
    ) : (
      <span className="inline-flex min-w-36 items-center gap-2"><span className="material-symbols-outlined animate-spin">progress_activity</span>Đang tải tệp...</span>
    );
  }
  const download = async () => {
    try {
      await downloadAttachment(attachment.presignedUrl, attachment.originalName);
    } catch {
      onError('Không thể tải tệp. Vui lòng thử lại.');
    }
  };
  const actions = (
    <div className="absolute right-2 top-2 hidden gap-1 group-hover:flex">
      <button type="button" onClick={download} className="chat-attachment-action" title="Tải xuống">
        <span className="material-symbols-outlined text-base">download</span>
      </button>
      {isOwn && item.messageId && (
        <button type="button" onClick={() => onRecall(item.messageId!)} className="chat-attachment-action" title="Thu hồi">
          <span className="material-symbols-outlined text-base">undo</span>
        </button>
      )}
    </div>
  );
  if (item.messageType === 'IMAGE') {
    return (
      <div className="group relative flex flex-col gap-2">
        <div className="relative">
          <button type="button" onClick={() => onPreview(attachment.presignedUrl, attachment.originalName)} className="block overflow-hidden rounded-xl cursor-zoom-in">
            <img src={attachment.presignedUrl} alt={attachment.originalName} className="chat-inline-image" />
          </button>
          {actions}
        </div>
        {item.messageBody && (
          <p className="m-0 text-sm whitespace-pre-wrap">{item.messageBody}</p>
        )}
      </div>
    );
  }
  return (
    <div className="group relative flex flex-col gap-2">
      <div className="flex min-w-52 items-center gap-3 pr-14">
        <span className="material-symbols-outlined text-2xl">description</span>
        <div className="min-w-0">
          <p className="m-0 truncate font-semibold">{attachment.originalName}</p>
          <p className="m-0 text-xs opacity-75">Tài liệu đính kèm</p>
        </div>
        {actions}
      </div>
      {item.messageBody && (
        <p className="m-0 text-sm whitespace-pre-wrap pt-1 border-t border-current/10">{item.messageBody}</p>
      )}
    </div>
  );
}

interface PendingAttachment {
  file: File;
  kind: 'IMAGE' | 'DOCUMENT';
  previewUrl?: string;
}

export default function ChatPanel({ conversationId }: ChatPanelProps) {
  const navigate = useNavigate();
  const currentUserId = useAuthStore((state) => state.user?.id);
  const { initiate } = useDirectCall();
  const [conversation, setConversation] = useState<DirectConversation | null>(null);
  const [items, setItems] = useState<TimelineItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [expertAvailable, setExpertAvailable] = useState(true);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [hasMoreOlder, setHasMoreOlder] = useState(false);
  const [preview, setPreview] = useState<{ url: string; name: string } | null>(null);
  const [pendingAttachment, setPendingAttachment] = useState<PendingAttachment | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const documentInputRef = useRef<HTMLInputElement>(null);

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
        setConversation(conversation);
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

  const sendWithClientId = async (
    clientMessageId: string,
    body?: string,
    messageType: 'TEXT' | 'IMAGE' | 'FILE' = 'TEXT',
    attachmentId?: string
  ) => {
    try {
      const confirmed = await directChatApi.sendMessage(conversationId, clientMessageId, body, messageType, attachmentId);
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

  useEffect(() => {
    return () => {
      if (pendingAttachment?.previewUrl) {
        URL.revokeObjectURL(pendingAttachment.previewUrl);
      }
    };
  }, [pendingAttachment]);

  const handleSelectFile = (file: File, kind: 'IMAGE' | 'DOCUMENT') => {
    if (sending || !expertAvailable || !currentUserId) return;
    const image = kind === 'IMAGE';
    const sizeLimit = image ? 10 * 1024 * 1024 : 20 * 1024 * 1024;
    if (file.size > sizeLimit) {
      setError(image ? 'Ảnh phải nhỏ hơn 10 MB.' : 'Tài liệu phải nhỏ hơn 20 MB.');
      return;
    }
    if (pendingAttachment?.previewUrl) {
      URL.revokeObjectURL(pendingAttachment.previewUrl);
    }
    const previewUrl = image ? URL.createObjectURL(file) : undefined;
    setPendingAttachment({ file, kind, previewUrl });
  };

  const handleSend = async () => {
    const body = draft.trim();
    if ((!body && !pendingAttachment) || sending || !expertAvailable || !currentUserId) return;

    const attachment = pendingAttachment;
    const clientMessageId = crypto.randomUUID();

    if (attachment?.previewUrl) {
      URL.revokeObjectURL(attachment.previewUrl);
    }
    setPendingAttachment(null);
    setDraft('');
    setSending(true);

    try {
      if (attachment) {
        const uploaded = await directChatApi.uploadAttachment(conversationId, attachment.file, attachment.kind);
        const messageType = attachment.kind === 'IMAGE' ? 'IMAGE' : 'FILE';
        setItems((prev) =>
          mergeTimelineItems(prev, [
            optimisticMessage({
              clientMessageId,
              senderUserId: currentUserId,
              messageType,
              attachmentId: uploaded.fileId,
              messageBody: body || undefined,
            }),
          ])
        );
        await sendWithClientId(clientMessageId, body || undefined, messageType, uploaded.fileId);
      } else {
        setItems((prev) =>
          mergeTimelineItems(prev, [
            optimisticMessage({
              clientMessageId,
              senderUserId: currentUserId,
              messageBody: body,
            }),
          ])
        );
        await sendWithClientId(clientMessageId, body);
      }
    } catch (e) {
      setError(`Không thể gửi tin nhắn: ${e}`);
      setSending(false);
    }
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

  const handleRecall = async (messageId: string) => {
    if (!window.confirm('Thu hồi tin nhắn và tệp đính kèm này?')) return;
    try {
      await directChatApi.recallMessage(conversationId, messageId);
      setItems((previous) => previous.map((item) =>
        item.messageId === messageId
          ? { ...item, attachmentId: undefined, messageBody: undefined, recalledAt: new Date().toISOString() }
          : item
      ));
    } catch (e) {
      setError(`Không thể thu hồi tin nhắn: ${e}`);
    }
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

  const isMotherCounterpart =
    conversation?.counterpartRole === 'MOTHER' ||
    (!conversation?.counterpartRole && currentUserId === conversation?.expertUserId);
  const displayName =
    conversation?.counterpartDisplayName || (isMotherCounterpart ? 'Mẹ bầu CareBridge' : 'Chuyên gia tư vấn');
  const initial = (displayName || (isMotherCounterpart ? 'M' : 'E'))[0]?.toUpperCase() || 'M';

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
          
          <div className="w-11 h-11 rounded-full bg-primary-container text-primary flex items-center justify-center font-bold text-base shrink-0 shadow-sm overflow-hidden">
            {conversation?.counterpartAvatarUrl ? (
              <img src={conversation.counterpartAvatarUrl} alt={displayName} className="w-full h-full object-cover" />
            ) : (
              initial
            )}
          </div>

          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-on-surface m-0 leading-tight">
                {displayName}
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
                    {describeCall(item)} · {formatTimestamp(item.initiatedAt)}
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
                  {(() => {
                    if (item.recalledAt) {
                      return (
                        <div className="p-3.5 rounded-2xl text-sm leading-relaxed shadow-sm bg-surface border border-outline-variant/60 text-on-surface">
                          <span className="italic text-on-surface-variant">Tin nhắn đã được thu hồi</span>
                        </div>
                      );
                    }
                    if (item.messageType === 'IMAGE' || item.messageType === 'FILE') {
                      return (
                        <div
                          className={`p-3.5 rounded-2xl text-sm leading-relaxed shadow-sm ${
                            isOwn
                              ? 'bg-primary text-on-primary rounded-tr-xs'
                              : 'bg-surface border border-outline-variant/60 text-on-surface rounded-tl-xs'
                          }`}
                        >
                          <AttachmentBubble
                            item={item}
                            conversationId={conversationId}
                            isOwn={isOwn}
                            onPreview={(url, name) => setPreview({ url, name })}
                            onRecall={handleRecall}
                            onError={setError}
                          />
                        </div>
                      );
                    }
                    if (item.messageType === 'LOCATION') {
                      return (
                        <div
                          className={`p-3.5 rounded-2xl text-sm leading-relaxed shadow-sm ${
                            isOwn
                              ? 'bg-primary text-on-primary rounded-tr-xs'
                              : 'bg-surface border border-outline-variant/60 text-on-surface rounded-tl-xs'
                          }`}
                        >
                          <LocationMessageBubble
                            item={item}
                            isOwn={isOwn}
                            onRecall={handleRecall}
                          />
                        </div>
                      );
                    }

                    const healthData = parseHealthMetricsShare(item.messageBody);
                    if (healthData) {
                      return <SharedHealthMetricsBubble data={healthData} isOwn={isOwn} />;
                    }

                    const checklistData = parseChecklistShare(item.messageBody);
                    if (checklistData) {
                      return <SharedChecklistBubble data={checklistData} isOwn={isOwn} />;
                    }

                    return (
                      <div
                        className={`p-3.5 rounded-2xl text-sm leading-relaxed shadow-sm ${
                          isOwn
                            ? 'bg-primary text-on-primary rounded-tr-xs'
                            : 'bg-surface border border-outline-variant/60 text-on-surface rounded-tl-xs'
                        }`}
                      >
                        {item.messageBody}
                      </div>
                    );
                  })()}
                </div>

                {!item.sendStatus && (
                  <span className="text-[11px] text-outline px-1">{formatTimestamp(item.createdAt)}</span>
                )}

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
          <div className="bg-surface border-t border-outline-variant/60 shrink-0">
            {pendingAttachment && (
              <div className="px-4 py-2.5 flex items-center justify-between bg-surface-container-low/90 border-b border-outline-variant/40">
                <div className="flex items-center gap-3 min-w-0">
                  {pendingAttachment.kind === 'IMAGE' && pendingAttachment.previewUrl ? (
                    <img
                      src={pendingAttachment.previewUrl}
                      alt={pendingAttachment.file.name}
                      className="w-11 h-11 rounded-lg object-cover border border-outline-variant shrink-0"
                    />
                  ) : (
                    <div className="w-11 h-11 rounded-lg bg-primary/10 text-primary flex items-center justify-center font-bold shrink-0">
                      <span className="material-symbols-outlined text-2xl">description</span>
                    </div>
                  )}
                  <div className="min-w-0">
                    <p className="m-0 text-xs font-semibold text-on-surface truncate">
                      {pendingAttachment.file.name}
                    </p>
                    <p className="m-0 text-[11px] text-on-surface-variant">
                      {(pendingAttachment.file.size / 1024).toFixed(1)} KB
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    if (pendingAttachment.previewUrl) URL.revokeObjectURL(pendingAttachment.previewUrl);
                    setPendingAttachment(null);
                  }}
                  className="w-7 h-7 rounded-full text-rose-600 hover:bg-rose-50 flex items-center justify-center transition-colors cursor-pointer shrink-0"
                  title="Hủy tệp đính kèm"
                >
                  <span className="material-symbols-outlined text-base">close</span>
                </button>
              </div>
            )}
            <div className="p-4 flex items-center gap-3">
              <input
                ref={imageInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/heic,image/gif"
                className="hidden"
                onChange={(e) => {
                  const file = e.currentTarget.files?.[0];
                  e.currentTarget.value = '';
                  if (file) handleSelectFile(file, 'IMAGE');
                }}
              />
              <input
                ref={documentInputRef}
                type="file"
                accept={DOCUMENT_ACCEPT}
                className="hidden"
                onChange={(e) => {
                  const file = e.currentTarget.files?.[0];
                  e.currentTarget.value = '';
                  if (file) handleSelectFile(file, 'DOCUMENT');
                }}
              />
              <button
                type="button"
                disabled={sending}
                onClick={() => imageInputRef.current?.click()}
                className="w-10 h-10 rounded-full border border-outline-variant text-primary hover:bg-surface-container-low disabled:opacity-40 cursor-pointer"
                title="Gửi ảnh"
              >
                <span className="material-symbols-outlined">image</span>
              </button>
              <button
                type="button"
                disabled={sending}
                onClick={() => documentInputRef.current?.click()}
                className="w-10 h-10 rounded-full border border-outline-variant text-primary hover:bg-surface-container-low disabled:opacity-40 cursor-pointer"
                title="Gửi tài liệu"
              >
                <span className="material-symbols-outlined">attach_file</span>
              </button>
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
                disabled={sending || (!draft.trim() && !pendingAttachment)}
                onClick={handleSend}
                className="w-11 h-11 rounded-full bg-primary text-on-primary flex items-center justify-center hover:brightness-110 disabled:opacity-40 disabled:cursor-not-allowed transition-all shrink-0 cursor-pointer shadow-md"
                title="Gửi tin nhắn"
              >
                <span className="material-symbols-outlined text-xl">send</span>
              </button>
            </div>
          </div>
        )}
      </div>
      {preview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-8" onClick={() => setPreview(null)}>
          <button type="button" className="absolute right-6 top-6 rounded-full bg-white/15 p-3 text-white hover:bg-white/25" onClick={() => setPreview(null)} aria-label="Đóng ảnh">
            <span className="material-symbols-outlined">close</span>
          </button>
          <img
            src={preview.url}
            alt={preview.name}
            className="max-h-full max-w-full object-contain cursor-zoom-out"
            onClick={(event) => event.stopPropagation()}
          />
          <button type="button" onClick={() => void downloadAttachment(preview.url, preview.name)} className="absolute bottom-6 rounded-full bg-white px-5 py-3 text-sm font-bold text-primary shadow-lg">
            <span className="material-symbols-outlined mr-2 align-middle">download</span>Tải xuống
          </button>
        </div>
      )}
    </div>
  );
}
