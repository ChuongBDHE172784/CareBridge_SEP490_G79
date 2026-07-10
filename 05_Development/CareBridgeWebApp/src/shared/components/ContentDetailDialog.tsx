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

interface ContentDetailDialogProps {
  open: boolean;
  targetTypeLabel: string;
  statusLabel?: string;
  loading: boolean;
  errorText?: string;
  detail: ContentDetail | null;
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
  onClose,
}: ContentDetailDialogProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center font-sans">
      <div
        className="absolute inset-0 bg-[rgba(39,24,18,0.4)] backdrop-blur-[4px]"
        onClick={onClose}
      />
      <div className="relative w-full max-w-xl mx-4 max-h-[85vh] overflow-y-auto bg-surface rounded-3xl shadow-[0_8px_40px_rgba(90,70,63,0.25)] p-6">
        <div className="flex items-start justify-between gap-3 mb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-xl">article</span>
            <h2 className="text-base font-bold text-on-surface m-0">Chi tiết {targetTypeLabel}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-outline hover:text-on-surface cursor-pointer"
            aria-label="Đóng"
          >
            <span className="material-symbols-outlined text-xl">close</span>
          </button>
        </div>

        {loading ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : errorText ? (
          <div className="bg-error-container rounded-2xl p-4 text-error text-sm">{errorText}</div>
        ) : detail ? (
          <>
            <div className="flex flex-wrap items-center gap-2 mb-4">
              {statusLabel && (
                <span className="py-1 px-3 rounded-full bg-primary-container text-on-primary-container text-xs font-semibold">
                  {statusLabel}
                </span>
              )}
              {detail.anonymous && (
                <span className="py-1 px-3 rounded-full bg-surface-container-highest text-on-surface-variant text-xs font-semibold">
                  Đăng ẩn danh
                </span>
              )}
            </div>

            {detail.questionTitle && (
              <div className="bg-surface-container-low rounded-2xl p-4 mb-4">
                <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">
                  Trả lời cho câu hỏi
                </p>
                <p className="text-sm text-on-surface-variant">{detail.questionTitle}</p>
              </div>
            )}

            {detail.title && (
              <h3 className="text-lg font-bold text-on-surface mb-3">{detail.title}</h3>
            )}

            <div className="bg-surface-container-low rounded-2xl p-4 border border-outline-variant mb-4">
              <p className="text-[15px] leading-7 text-on-surface whitespace-pre-wrap">{detail.body}</p>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs text-outline">
              <p className="m-0">
                Tác giả: <span className="text-on-surface-variant">{detail.authorName ?? '—'}</span>
              </p>
              <p className="m-0">Đăng lúc: {formatDateTime(detail.createdAt)}</p>
              {detail.updatedAt && <p className="m-0">Cập nhật lúc: {formatDateTime(detail.updatedAt)}</p>}
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
}
