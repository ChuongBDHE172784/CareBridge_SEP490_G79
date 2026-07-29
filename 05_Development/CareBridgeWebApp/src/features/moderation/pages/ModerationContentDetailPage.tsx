import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { fetchContentDetail } from '../services/moderationApi';
import type { ModerationContentDetail, ModerationHistoryItem, ReportTargetType } from '../models/moderation';
import { ACTION_TYPE_LABELS, TARGET_TYPE_LABELS } from '../models/moderation';

const VALID_TARGET_TYPES: ReadonlySet<string> = new Set(['QUESTION', 'ANSWER']);

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'full', timeStyle: 'short' });
}

function statusStyle(status: string): string {
  if (status === 'APPROVED') return 'bg-[#E6F4EA] text-[#137333]';
  if (status === 'HIDDEN') return 'bg-[#FCE8E6] text-[#C5221F]';
  if (status === 'PENDING') return 'bg-primary-container text-primary';
  return 'bg-surface-container-highest text-on-surface-variant';
}

function ImageGallery({ images, label }: { images: string[]; label: string }) {
  if (images.length === 0) return null;

  return (
    <section aria-label={label}>
      <h2 className="mb-3 text-sm font-bold text-on-surface">{label} ({images.length})</h2>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {images.map((url, index) => (
          <a key={`${url}-${index}`} href={url} target="_blank" rel="noreferrer" className="group overflow-hidden rounded-xl border border-surface-container-highest bg-surface-container-low">
            <img src={url} alt={`${label} ${index + 1}`} className="h-52 w-full object-cover transition-transform duration-200 group-hover:scale-[1.02]" />
          </a>
        ))}
      </div>
    </section>
  );
}

export default function ModerationContentDetailPage() {
  const { targetType: routeTargetType, targetId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const historyItem = (location.state as { historyItem?: ModerationHistoryItem } | null)?.historyItem;
  const returnTo = (location.state as { returnTo?: string } | null)?.returnTo ?? '/moderator/pending-content';
  const [detail, setDetail] = useState<ModerationContentDetail | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const targetType = VALID_TARGET_TYPES.has(routeTargetType ?? '') ? routeTargetType as ReportTargetType : null;

  useEffect(() => {
    if (!targetType || !targetId) {
      setError('Liên kết nội dung không hợp lệ.');
      setLoading(false);
      return;
    }

    let active = true;
    void fetchContentDetail(targetType, targetId)
      .then((data) => { if (active) setDetail(data); })
      .catch(() => { if (active) setError('Không tải được nội dung chi tiết. Vui lòng thử lại.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [targetId, targetType]);

  const targetLabel = targetType ? TARGET_TYPE_LABELS[targetType] : 'nội dung cộng đồng';

  return (
    <div className="portal-page font-sans">
      <main className="mx-auto max-w-6xl p-5 md:p-8">
        <button type="button" onClick={() => navigate(returnTo)} className="mb-6 inline-flex items-center gap-2 rounded-full border border-outline-variant bg-surface px-4 py-2 text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low">
          <span className="material-symbols-outlined text-lg">arrow_back</span>
          Quay lại danh sách kiểm duyệt
        </button>

        {loading ? (
          <div className="rounded-2xl border border-surface-container-highest bg-surface p-12 text-center text-on-surface-variant">Đang tải nội dung đầy đủ...</div>
        ) : error ? (
          <div className="rounded-2xl border border-error-container bg-error-container/60 p-6 text-error">{error}</div>
        ) : detail && (
          <div className="space-y-6">
            <header className="rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-sm md:p-8">
              <div className="mb-5 flex flex-wrap items-center gap-2">
                <span className="rounded-full bg-surface-container-low px-3 py-1 text-xs font-bold text-primary">{targetLabel}</span>
                <span className={`rounded-full px-3 py-1 text-xs font-bold ${statusStyle(detail.status)}`}>{detail.status}</span>
                {detail.anonymous && <span className="rounded-full bg-surface-container-highest px-3 py-1 text-xs font-bold text-on-surface-variant">Đăng ẩn danh với cộng đồng</span>}
                {detail.expertLabeled && <span className="rounded-full bg-tertiary-container px-3 py-1 text-xs font-bold text-on-tertiary-container">Chuyên gia xác thực</span>}
                {detail.personalExperience && <span className="rounded-full bg-secondary-container px-3 py-1 text-xs font-bold text-on-secondary-container">Chia sẻ trải nghiệm</span>}
              </div>
              <h1 className="m-0 text-2xl font-bold tracking-tight text-on-surface md:text-3xl">{detail.title ?? 'Câu trả lời cộng đồng'}</h1>
              <div className="mt-5 grid gap-3 border-t border-surface-container-highest pt-5 text-sm text-on-surface-variant sm:grid-cols-2 lg:grid-cols-3">
                <p className="m-0"><span className="font-semibold text-on-surface">Người đăng: </span>{detail.authorName ?? 'Không xác định'}</p>
                <p className="m-0"><span className="font-semibold text-on-surface">Đăng lúc: </span>{formatDateTime(detail.createdAt)}</p>
                {detail.updatedAt && <p className="m-0"><span className="font-semibold text-on-surface">Cập nhật: </span>{formatDateTime(detail.updatedAt)}</p>}
              </div>
            </header>

            {detail.questionTitle && (
              <section className="rounded-2xl border border-primary-container bg-primary-container/20 p-6 md:p-7">
                <div className="mb-3 flex items-center gap-2 text-primary"><span className="material-symbols-outlined">forum</span><h2 className="m-0 text-base font-bold">Ngữ cảnh câu hỏi đang được trả lời</h2></div>
                <h3 className="m-0 text-lg font-bold text-on-surface">{detail.questionTitle}</h3>
                {detail.questionBody && <p className="mb-0 mt-3 whitespace-pre-wrap text-[15px] leading-7 text-on-surface">{detail.questionBody}</p>}
                <div className="mt-5"><ImageGallery images={detail.questionImageUrls} label="Ảnh của câu hỏi" /></div>
              </section>
            )}

            <section className="rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-sm md:p-8">
              <h2 className="mb-4 text-lg font-bold text-on-surface">{detail.targetType === 'QUESTION' ? 'Nội dung câu hỏi' : 'Nội dung câu trả lời'}</h2>
              <div className="whitespace-pre-wrap break-words text-[16px] leading-8 text-on-surface">{detail.body}</div>
              <div className="mt-7"><ImageGallery images={detail.imageUrls} label="Ảnh đính kèm" /></div>
            </section>

            {historyItem && (
              <section className="rounded-2xl border border-outline-variant bg-surface-container-low p-6">
                <div className="mb-2 flex items-center gap-2"><span className="material-symbols-outlined text-primary">gavel</span><h2 className="m-0 text-base font-bold text-on-surface">Thông tin xử lý kiểm duyệt</h2></div>
                <div className="grid gap-3 text-sm text-on-surface-variant sm:grid-cols-2">
                  <p className="m-0"><span className="font-semibold text-on-surface">Kết quả: </span>{ACTION_TYPE_LABELS[historyItem.actionType]}</p>
                  <p className="m-0"><span className="font-semibold text-on-surface">Người xử lý: </span>{historyItem.moderatorName ?? '—'}</p>
                  <p className="m-0"><span className="font-semibold text-on-surface">Thời gian: </span>{formatDateTime(historyItem.actionAt)}</p>
                  <p className="m-0"><span className="font-semibold text-on-surface">Lý do: </span>{historyItem.reason ?? '—'}</p>
                </div>
              </section>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
