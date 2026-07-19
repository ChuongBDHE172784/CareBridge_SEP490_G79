function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export interface ContentDetail {
  targetId: string;
  targetType: string;
  authorId: string | null;
  authorName: string | null;
  title: string | null;
  body: string;
  status: string;
  anonymous: boolean;
  questionId: string | null;
  questionTitle: string | null;
  createdAt: string;
  updatedAt: string | null;
}

// Action-level info (from the "Đã xử lý" history row) — distinct from ContentDetail, which only
// describes the underlying question/answer itself, not the moderation decision made on it.
export interface ModerationActionContext {
  actionTypeLabel: string;
  reason: string | null;
  moderatorName: string | null;
  actionAt: string;
}

interface ContentDetailDialogProps {
  open: boolean;
  targetTypeLabel: string;
  statusLabel?: string;
  loading: boolean;
  errorText?: string;
  detail: ContentDetail | null;
  moderationContext?: ModerationActionContext;
  onClose: () => void;
}

// CB-MOD-IMP-008: shows the full (non-truncated) body of a question/answer that a moderator is
// reviewing — the list tables only ever show contentPreview (server-truncated at 200 chars).
export default function ContentDetailDialog({
  open,
  targetTypeLabel,
  statusLabel,
  loading,
  errorText,
  detail,
  moderationContext,
  onClose,
}: ContentDetailDialogProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center font-sans">
      <div
        className="absolute inset-0 bg-slate-950/35 backdrop-blur-[3px]"
        onClick={onClose}
      />
      <div className="relative mx-4 max-h-[85vh] w-full max-w-lg overflow-y-auto rounded-lg border border-outline-variant/80 bg-surface p-5 shadow-[0_16px_40px_rgba(15,23,42,0.14)]">
        <div className="mb-4 flex items-start justify-between gap-3 border-b border-outline-variant/60 pb-3">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-lg text-primary">article</span>
            <h2 className="m-0 text-sm font-semibold text-on-surface">Chi tiết {targetTypeLabel}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex cursor-pointer items-center rounded-md text-outline hover:bg-surface-container-low hover:text-on-surface"
            aria-label="Đóng"
          >
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        {loading ? (
          <div className="py-8 text-center text-xs text-outline">Đang tải...</div>
        ) : errorText ? (
          <div className="rounded-md bg-error-container p-3 text-xs text-error">{errorText}</div>
        ) : detail ? (
          <>
            <div className="mb-3 flex flex-wrap items-center gap-2">
              {statusLabel && (
                <span className="rounded bg-primary-container px-2 py-0.5 text-[11px] font-semibold text-primary">
                  {statusLabel}
                </span>
              )}
              {detail.anonymous && (
                <span className="rounded bg-surface-container-highest px-2 py-0.5 text-[11px] font-semibold text-on-surface-variant">
                  Đăng ẩn danh
                </span>
              )}
            </div>

            {detail.questionTitle && (
              <div className="mb-3 rounded-md border border-outline-variant/50 bg-surface-container-low p-3">
                <p className="mb-1 text-[10px] font-semibold uppercase tracking-[0.02em] text-outline">
                  Trả lời cho câu hỏi
                </p>
                <p className="m-0 text-xs text-on-surface-variant">{detail.questionTitle}</p>
              </div>
            )}

            {detail.title && (
              <h3 className="mb-2 text-sm font-semibold text-on-surface">{detail.title}</h3>
            )}

            <div className="mb-3 rounded-md border border-outline-variant/60 bg-surface-container-low p-3.5">
              <p className="m-0 whitespace-pre-wrap text-xs leading-relaxed text-on-surface">{detail.body}</p>
            </div>

            {moderationContext && (
              <div className="mb-3 rounded-md border border-primary-container/60 bg-primary-container/30 p-3.5">
                <div className="mb-1.5 flex items-center gap-2">
                  <span className="material-symbols-outlined text-base text-primary">gavel</span>
                  <p className="m-0 text-[10px] font-semibold uppercase tracking-[0.02em] text-outline">
                    Xử lý kiểm duyệt — {moderationContext.actionTypeLabel}
                  </p>
                </div>
                <p className="mb-1.5 text-xs text-on-surface">
                  Lý do: <span className="text-on-surface-variant">{moderationContext.reason ?? '—'}</span>
                </p>
                <div className="flex flex-wrap gap-x-4 text-[10px] text-outline">
                  <span>Người xử lý: {moderationContext.moderatorName ?? '—'}</span>
                  <span>Thời gian: {formatDateTime(moderationContext.actionAt)}</span>
                </div>
              </div>
            )}

            <div className="grid grid-cols-2 gap-2 border-t border-outline-variant/50 pt-2 text-[10px] text-outline">
              <p className="m-0">
                Tác giả: <span className="text-on-surface-variant">{detail.authorName ?? '—'}</span>
              </p>
              <p className="m-0">Đăng lúc: {formatDateTime(detail.createdAt)}</p>
              {detail.updatedAt && <p className="m-0 col-span-2">Cập nhật lúc: {formatDateTime(detail.updatedAt)}</p>}
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
}
