import type { ReviewFeedback } from '../models/content';

function formatRequestedAt(value: string | null): string {
  if (!value) return 'Không rõ thời gian';
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

export default function ReviewFeedbackNotice({
  feedback,
  compact = false,
}: {
  feedback?: ReviewFeedback | null;
  compact?: boolean;
}) {
  if (!feedback) return null;

  if (compact) {
    return (
      <div className="mt-1 flex min-w-0 items-center gap-1 text-xs text-error" title={feedback.reason}>
        <span className="material-symbols-outlined text-sm">assignment_return</span>
        <span className="truncate">{feedback.reason}</span>
      </div>
    );
  }

  return (
    <section className="mb-5 border-l-4 border-error bg-error-container px-4 py-3 text-error" aria-label="Phản hồi yêu cầu chỉnh sửa">
      <div className="flex items-start gap-3">
        <span className="material-symbols-outlined mt-0.5 text-xl">assignment_return</span>
        <div className="min-w-0">
          <h2 className="m-0 text-sm font-bold">System Admin yêu cầu chỉnh sửa</h2>
          <p className="mt-1 whitespace-pre-wrap text-sm leading-6">{feedback.reason}</p>
          <p className="mt-2 text-xs opacity-80">
            Trả về lúc {formatRequestedAt(feedback.requestedAt)}
            {feedback.versionNo ? ` · Phiên bản v${feedback.versionNo}` : ''}
          </p>
        </div>
      </div>
    </section>
  );
}

