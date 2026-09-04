import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  fetchRecommendationTags,
  fetchStaffContentDetail,
  fetchTags,
  updateContent,
  archiveContent,
  decideExpertContent,
  decideContent,
} from '../services/contentApi';
import type { ContentDetail, RecommendationTag } from '../models/content';
import { STAGE_LABELS, STATUS_LABELS, TYPE_LABELS } from '../models/content';
import { formatRecommendationTagLabel, recommendationClassification } from './recommendationMetadata';
import { useAuth } from '../../../shared/auth/useAuth';
import '../richContentBody.css';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';


/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function statusDotClass(status: string): string {
  if (status === 'APPROVED') return 'bg-[#137333]';
  if (status === 'DRAFT') return 'bg-[#616161]';
  if (status === 'PENDING_REVIEW') return 'bg-[#E65100]';
  return 'bg-[#BA1A1A]';
}

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

const TYPE_LIST_PATH: Record<string, string> = {
  ARTICLE: '/content/articles',
  FAQ: '/content/faq',
  CHECKLIST: '/content/checklists',
};

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function ContentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  // System Admin can reach this page read-only from the approval queue (/admin/content-review/:id) —
  // /content/:id/edit and the archive/list routes below are gated to CONTENT_ADMIN only, so those
  // actions must stay hidden rather than link into a route that will bounce to /forbidden.
  const canManage = hasRole('CONTENT_ADMIN');
  const isExpert = hasRole('EXPERT');
  const isSystemAdmin = hasRole('SYSTEM_ADMIN');
  const canReview = isExpert || isSystemAdmin;

  const [detail, setDetail] = useState<ContentDetail | null>(null);
  // Gửi phê duyệt chỉ mở sau khi người gửi đã cuộn tới cuối bài. Một cái mốc đặt
  // ngay dưới thân bài rẻ hơn là đo scrollTop: nó đúng với mọi chiều cao màn hình,
  // và với bài ngắn không cần cuộn thì mốc đã nằm trong khung nhìn nên nút mở luôn.
  const [hasReadBody, setHasReadBody] = useState(false);
  const bodyEndRef = useRef<HTMLDivElement | null>(null);
  const [recommendationTags, setRecommendationTags] = useState<RecommendationTag[]>([]);
  const [ordinaryTagIds, setOrdinaryTagIds] = useState<Set<string> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [submittingApproval, setSubmittingApproval] = useState(false);

  // Review Decision State
  const [decisionModal, setDecisionModal] = useState<'APPROVE' | 'REJECT' | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectReasonError, setRejectReasonError] = useState<string | null>(null);
  const [submittingDecision, setSubmittingDecision] = useState(false);
  const [decisionSuccess, setDecisionSuccess] = useState<string | null>(null);

  const handleOpenDecision = (decision: 'APPROVE' | 'REJECT') => {
    setDecisionModal(decision);
    setRejectReason('');
    setRejectReasonError(null);
  };

  const handleCloseDecision = () => {
    if (submittingDecision) return;
    setDecisionModal(null);
    setRejectReason('');
    setRejectReasonError(null);
  };

  const handleSubmitDecision = async () => {
    if (!detail || !decisionModal) return;
    if (decisionModal === 'REJECT' && !rejectReason.trim()) {
      setRejectReasonError('Vui lòng nhập lý do từ chối / góp ý chỉnh sửa cho tác giả.');
      return;
    }
    setSubmittingDecision(true);
    setActionError('');
    try {
      if (isExpert) {
        await decideExpertContent(detail.id, decisionModal, decisionModal === 'REJECT' ? rejectReason.trim() : undefined);
      } else {
        await decideContent(detail.id, decisionModal, decisionModal === 'REJECT' ? rejectReason.trim() : undefined);
      }
      setDecisionSuccess(
        decisionModal === 'APPROVE'
          ? 'Nội dung đã được phê duyệt và xuất bản thành công.'
          : 'Đã gửi yêu cầu chỉnh sửa cho tác giả nội dung.'
      );
      setTimeout(() => {
        if (isExpert) {
          navigate('/expert/content-approval');
        } else {
          navigate('/admin/content-approval-queue');
        }
      }, 1000);
    } catch (err: unknown) {
      setActionError(err instanceof Error ? err.message : 'Không thể thực hiện thẩm định. Vui lòng thử lại.');
    } finally {
      setSubmittingDecision(false);
    }
  };

  const loadDetail = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError('');
    try {
      const data = await fetchStaffContentDetail(id);
      let catalogItems: RecommendationTag[] = [];
      let ordinaryIds: Set<string> | null = null;
      if (data.type === 'ARTICLE') {
        try {
          const catalog = await fetchRecommendationTags();
          catalogItems = catalog.items;
        } catch { /* detail remains readable when the catalog is unavailable */ }
        try {
          const ordinaryTags = await fetchTags();
          ordinaryIds = new Set(ordinaryTags.filter((tag) => !tag.slug.startsWith('rec-')).map((tag) => tag.id));
        } catch { /* do not label ordinary IDs as stale when the tag catalog is unavailable */ }
      }
      setRecommendationTags(catalogItems);
      setOrdinaryTagIds(ordinaryIds);
      setHasReadBody(false);
      setDetail(data);
    } catch {
      setError('Không tải được nội dung. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

  useEffect(() => {
    if (!detail || hasReadBody) return;
    const node = bodyEndRef.current;
    if (!node) return;
    // Trình duyệt không hỗ trợ thì mở nút, thà bỏ lỡ một lần nhắc còn hơn khoá
    // cứng người dùng khỏi việc gửi bài.
    if (typeof IntersectionObserver === 'undefined') {
      setHasReadBody(true);
      return;
    }
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) setHasReadBody(true);
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, [detail, hasReadBody]);

  const submitForApproval = async () => {
    if (!detail) return;
    setSubmittingApproval(true);
    setActionError('');
    try {
      await updateContent(detail.id, {
        title: detail.title,
        body: detail.body,
        stage: detail.stage,
        topicId: detail.topicId || undefined,
        tagIds: detail.tagIds,
        eligibleFromWeek: detail.eligibleFromWeek ?? null,
        eligibleToWeek: detail.eligibleToWeek ?? null,
        recommendationPriority: detail.recommendationPriority ?? 0,
        status: 'PENDING_REVIEW',
        sourceLabel: detail.sourceLabel || undefined,
        sources: detail.sources,
      });
      await loadDetail();
    } catch {
      setActionError('Không thể gửi phê duyệt. Vui lòng thử lại.');
    } finally {
      setSubmittingApproval(false);
    }
  };

  const handleDelete = async () => {
    if (!detail) return;
    const reason = window.prompt(`Nhập lý do xóa (lưu trữ) "${detail.title}":`);
    if (reason === null) return;
    if (!reason.trim()) {
      setActionError('Vui lòng nhập lý do trước khi xóa.');
      return;
    }
    try {
      await archiveContent(detail.id, reason.trim());
      navigate(TYPE_LIST_PATH[detail.type] ?? '/content/list');
    } catch {
      setActionError('Không thể xóa nội dung. Vui lòng thử lại.');
    }
  };

  if (isLoading) {
    return <div className="py-12 text-center text-outline font-sans">Đang tải...</div>;
  }

  if (error || !detail) {
    return (
      <div className="py-12 text-center font-sans">
        <p className="text-error mb-4">{error || 'Không tìm thấy nội dung.'}</p>
        <button onClick={() => navigate(-1)} className="py-2.5 px-6 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer">
          Quay lại
        </button>
      </div>
    );
  }

  const typeLabel = TYPE_LABELS[detail.type];
  const typeListPath = TYPE_LIST_PATH[detail.type] ?? '/content/list';
  const catalogById = new Map(recommendationTags.map((tag) => [tag.id, tag]));
  const controlledRecommendationTags = (detail.tagIds ?? [])
    .map((tagId) => catalogById.get(tagId))
    .filter((tag): tag is RecommendationTag => Boolean(tag));
  const staleRecommendationTagIds = (detail.tagIds ?? [])
    .filter((tagId) => !catalogById.has(tagId)
      && ordinaryTagIds != null
      && !ordinaryTagIds.has(tagId));
  const recommendationTagIds = controlledRecommendationTags.map((tag) => tag.id);

  return (
    <div className="p-8 font-sans">
      {/* Breadcrumbs */}
      <div className="flex items-center gap-2 text-[13px] text-outline mb-4">
        {isExpert ? (
          <span className="cursor-pointer hover:underline text-primary" onClick={() => navigate('/expert/content-approval')}>Thẩm định nội dung</span>
        ) : isSystemAdmin ? (
          <span className="cursor-pointer hover:underline text-primary" onClick={() => navigate('/admin/content-approval-queue')}>Hàng đợi phê duyệt</span>
        ) : canManage ? (
          <span className="cursor-pointer hover:underline text-primary" onClick={() => navigate(typeListPath)}>{typeLabel}</span>
        ) : (
          <span>{typeLabel}</span>
        )}
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span className="text-on-surface-variant">Chi tiết {typeLabel.toLowerCase()}</span>
      </div>

      {/* Back button */}
      <button
        onClick={() => {
          if (isExpert) navigate('/expert/content-approval');
          else if (isSystemAdmin) navigate('/admin/content-approval-queue');
          else navigate(-1);
        }}
        className="inline-flex items-center gap-1.5 py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer mb-6 hover:bg-surface-container-low transition-colors"
      >
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        {isExpert || isSystemAdmin ? 'Quay lại hàng đợi' : 'Quay lại'}
      </button>

      {decisionSuccess && (
        <div className="rounded-2xl bg-emerald-500/10 border border-emerald-500/20 p-4 mb-4 text-emerald-700 dark:text-emerald-300 text-sm flex items-center gap-2">
          <span className="material-symbols-outlined text-emerald-600">check_circle</span>
          {decisionSuccess}
        </div>
      )}
      {actionError && <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{actionError}</div>}
      {canManage && <ReviewFeedbackNotice feedback={detail.latestReviewFeedback} />}

      <div className="grid grid-cols-[1fr_340px] gap-6">
        {/* Main content area */}
        <div>
          {/* Title */}
          <h1 className="text-[28px] font-bold text-on-surface mt-0 mb-5 leading-[1.3]">{detail.title}</h1>

          {/* Metadata card */}
          <div className="bg-surface rounded-2xl p-5 shadow-md mb-6 flex gap-8 flex-wrap">
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">TÁC GIẢ</div>
              <div className="text-sm text-on-surface font-medium">Content Admin</div>
            </div>
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">NGÀY TẠO</div>
              <div className="text-sm text-on-surface font-medium">{formatDate(detail.createdAt)}</div>
            </div>
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">CHUYÊN MỤC</div>
              <div className="text-sm text-on-surface font-medium">{STAGE_LABELS[detail.stage]}</div>
            </div>
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">ĐỐI TƯỢNG MỤC TIÊU</div>
              <div className="flex gap-1.5 mt-0.5">
                <span className="py-[3px] px-2.5 rounded-full bg-[#FFE9E3] text-primary text-xs font-medium">Mẹ bầu</span>
                <span className="py-[3px] px-2.5 rounded-full bg-[#FFE9E3] text-primary text-xs font-medium">Gia đình</span>
              </div>
            </div>
          </div>

          {detail.type === 'ARTICLE' && (
            <div className="bg-surface rounded-2xl p-5 shadow-md mb-6">
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">THIẾT LẬP ĐỐI TƯỢNG GỢI Ý (RECOMMENDATION)</div>
              <div className="text-sm text-on-surface">
                Phân loại: {recommendationClassification(recommendationTagIds)} {' · '}
                {detail.eligibleFromWeek == null && detail.eligibleToWeek == null
                  ? 'Toàn bộ giai đoạn'
                  : `Tuần thai ${detail.eligibleFromWeek}–${detail.eligibleToWeek}`}
                {' · '}Độ ưu tiên {detail.recommendationPriority ?? 0}
              </div>
              <div className="text-xs text-outline mt-1">
                Đối tượng: {controlledRecommendationTags.length > 0
                  ? controlledRecommendationTags.map((tag) => formatRecommendationTagLabel(tag)).join(', ')
                  : 'Không có tag chỉ định (áp dụng toàn bộ người dùng đủ điều kiện)'}
              </div>
              {staleRecommendationTagIds.length > 0 && (
                <div className="text-xs text-error mt-1" role="alert">
                  Mã tag đối tượng không xác định: {staleRecommendationTagIds.join(', ')}
                </div>
              )}
            </div>
          )}

          {/* Article canvas */}
          <div className="bg-surface rounded-2xl p-8 shadow-md">
            {/* Body content */}
            <div
              className="rich-content-body text-[15px] leading-7 text-on-surface"
              dangerouslySetInnerHTML={{ __html: detail.body }}
            />
            {/* Mốc đánh dấu đã đọc hết — xem khối state ở đầu file. */}
            <div ref={bodyEndRef} aria-hidden="true" className="h-px w-full" />
          </div>

          {/* Sources & References Block */}
          {((detail.sources && detail.sources.length > 0) || detail.sourceLabel) && (
            <div className="bg-surface rounded-2xl p-6 shadow-md mt-6 border border-outline-variant">
              <div className="flex items-center gap-2 text-primary font-bold text-xs uppercase tracking-[0.05em] mb-4 pb-2 border-b border-surface-container-highest">
                <span className="material-symbols-outlined text-lg">verified</span>
                <span>NGUỒN THAM KHẢO / KIỂM DUYỆT</span>
              </div>

              <div className="flex flex-col gap-3">
                {detail.sources && detail.sources.length > 0 ? (
                  detail.sources.map((src, idx) => (
                    <div key={idx} className="p-4 rounded-xl bg-surface-container-low flex flex-col gap-2">
                      {src.title && (
                        <div className="flex items-center gap-2">
                          <span className="material-symbols-outlined text-outline text-base">person</span>
                          <span className="text-xs text-outline font-medium">Tác giả / Tên nguồn:</span>
                          <strong className="text-sm text-on-surface font-semibold">{src.title}</strong>
                        </div>
                      )}
                      {src.url && (
                        <div className="flex items-center gap-2">
                          <span className="material-symbols-outlined text-primary text-base">link</span>
                          <span className="text-xs text-outline font-medium">Liên kết nguồn:</span>
                          <a
                            href={src.url.startsWith('http') ? src.url : `https://${src.url}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-xs text-primary font-semibold underline hover:text-primary-container break-all"
                          >
                            {src.url}
                          </a>
                        </div>
                      )}
                      {src.publisher && (
                        <div className="flex items-center gap-2">
                          <span className="material-symbols-outlined text-outline text-base">domain</span>
                          <span className="text-xs text-outline font-medium">Đơn vị xuất bản:</span>
                          <span className="text-xs text-on-surface font-semibold">{src.publisher}</span>
                        </div>
                      )}
                    </div>
                  ))
                ) : (
                  <div className="p-4 rounded-xl bg-surface-container-low flex items-center gap-2">
                    <span className="material-symbols-outlined text-outline text-base">menu_book</span>
                    <span className="text-xs text-outline font-medium">Nguồn:</span>
                    <strong className="text-sm text-on-surface">{detail.sourceLabel}</strong>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right sidebar */}
        <div className="flex flex-col gap-4">
          {/* Status widget */}
          <div className="bg-surface rounded-2xl p-5 shadow-md">
            <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">TRẠNG THÁI</div>
            <div className="flex items-center gap-2 mb-2">
              <span className={`w-2.5 h-2.5 rounded-full ${statusDotClass(detail.status)}`} />
              <span className="text-sm font-semibold text-on-surface">
                {detail.latestReviewFeedback ? 'Cần chỉnh sửa' : STATUS_LABELS[detail.status]}
              </span>
            </div>
            <div className="text-xs text-outline mb-1">Phiên bản: v{detail.version}</div>
            <div className="text-xs text-outline">Xuất bản: {formatDate(detail.publishedAt)}</div>
          </div>

          {/* Review actions card for Expert / System Admin */}
          {canReview && detail.status === 'PENDING_REVIEW' && (
            <div className="bg-surface rounded-2xl p-5 shadow-md border border-primary/20">
              <div className="text-[11px] font-bold text-primary uppercase tracking-[0.05em] mb-2 flex items-center gap-1.5">
                <span className="material-symbols-outlined text-base">rate_review</span>
                THẨM ĐỊNH & PHÊ DUYỆT
              </div>
              <p className="text-xs text-on-surface-variant mb-4 leading-relaxed">
                Rà soát kỹ nội dung y khoa, thông tin chuyên mục, đối tượng thai kỳ và tài liệu trích dẫn trước khi phê duyệt.
              </p>
              <div className="flex flex-col gap-2.5">
                <button
                  type="button"
                  disabled={!hasReadBody}
                  title={hasReadBody ? undefined : 'Đọc hết nội dung bài viết trước khi phê duyệt'}
                  onClick={() => handleOpenDecision('APPROVE')}
                  className="w-full py-3 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold flex items-center justify-center gap-2 shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed enabled:cursor-pointer"
                >
                  <span className="material-symbols-outlined text-base">
                    {hasReadBody ? 'check_circle' : 'lock'}
                  </span>
                  Phê duyệt & Xuất bản
                </button>
                {!hasReadBody && (
                  <p className="text-[11px] text-outline text-center px-1 -mt-1">
                    Cuộn hết nội dung bài viết để mở nút phê duyệt.
                  </p>
                )}
                <button
                  type="button"
                  onClick={() => handleOpenDecision('REJECT')}
                  className="w-full py-2.5 px-4 rounded-xl border border-rose-500/30 bg-rose-500/10 hover:bg-rose-500/20 text-rose-600 text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer transition-colors"
                >
                  <span className="material-symbols-outlined text-base">undo</span>
                  Yêu cầu chỉnh sửa / Trả về
                </button>
              </div>
            </div>
          )}

          {/* Action buttons — hidden for read-only reviewers (e.g. System Admin from the approval queue) */}
          {canManage && (
            <>
              {(detail.status === 'DRAFT' || detail.status === 'PENDING_REVIEW') ? (
                <button
                  onClick={() => navigate(`/content/${detail.id}/edit`)}
                  className="w-full py-3.5 rounded-2xl bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2"
                >
                  <span className="material-symbols-outlined text-lg">edit</span>
                  Chỉnh sửa nội dung
                </button>
              ) : (
                <p className="text-xs text-outline text-center px-2">
                  Nội dung đã xuất bản hoặc lưu trữ không thể chỉnh sửa trực tiếp.
                </p>
              )}
              {detail.status === 'DRAFT' && (
                <>
                  <button
                    onClick={submitForApproval}
                    disabled={submittingApproval || !hasReadBody}
                    title={hasReadBody ? undefined : 'Đọc hết nội dung bài viết trước khi gửi phê duyệt'}
                    className="w-full py-3.5 rounded-2xl bg-transparent text-primary border border-outline-variant text-sm font-semibold flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed enabled:cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-lg">
                      {hasReadBody ? 'send' : 'lock'}
                    </span>
                    {submittingApproval ? 'Đang gửi...' : 'Gửi phê duyệt'}
                  </button>
                  {!hasReadBody && (
                    <p className="text-xs text-outline text-center px-2 -mt-2">
                      Cuộn hết nội dung bài viết để mở nút gửi phê duyệt.
                    </p>
                  )}
                </>
              )}
              <button
                onClick={handleDelete}
                className="w-full py-3.5 rounded-2xl bg-transparent text-error border border-error/40 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2"
              >
                <span className="material-symbols-outlined text-lg">delete</span>
                Xóa
              </button>
            </>
          )}

          {/* Version history */}
          <div className="bg-surface rounded-2xl p-5 shadow-md">
            <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">LỊCH SỬ PHIÊN BẢN</div>
            <p className="text-xs text-outline">Phiên bản hiện tại: v{detail.version}</p>
          </div>
        </div>
      </div>

      {/* Decision Modal */}
      {decisionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-lg rounded-2xl border border-outline-variant/70 bg-surface p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-outline-variant/70 pb-3">
              <h3 className="text-base font-bold text-on-surface">
                {decisionModal === 'APPROVE' ? 'Xác nhận phê duyệt & xuất bản' : 'Yêu cầu tác giả chỉnh sửa lại'}
              </h3>
              <button
                type="button"
                onClick={handleCloseDecision}
                disabled={submittingDecision}
                className="text-outline hover:text-on-surface cursor-pointer disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div className="mt-4 space-y-3">
              <p className="text-xs text-on-surface-variant leading-relaxed">
                {decisionModal === 'APPROVE'
                  ? `Bạn đang phê duyệt nội dung "${detail.title}". Sau khi xuất bản, nội dung sẽ được hiển thị cho người dùng.`
                  : `Vui lòng nhập lý do hoặc góp ý chuyên môn để tác giả nắm được nội dung cần hoàn thiện trước khi gửi lại:`}
              </p>

              {decisionModal === 'REJECT' && (
                <div>
                  <textarea
                    rows={4}
                    value={rejectReason}
                    onChange={(e) => {
                      setRejectReason(e.target.value);
                      if (rejectReasonError) setRejectReasonError(null);
                    }}
                    placeholder="Ví dụ: Cần bổ sung nguồn tài liệu trích dẫn y khoa; chỉnh sửa lại tuần thai đủ điều kiện..."
                    className="w-full rounded-xl border border-outline-variant/70 bg-surface-container-low p-3 text-xs text-on-surface focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary resize-none"
                  />
                  {rejectReasonError && (
                    <p className="mt-1 text-[11px] text-error font-medium">{rejectReasonError}</p>
                  )}
                </div>
              )}
            </div>

            <div className="mt-6 flex items-center justify-end gap-3 border-t border-outline-variant/70 pt-4">
              <button
                type="button"
                onClick={handleCloseDecision}
                disabled={submittingDecision}
                className="rounded-lg border border-outline-variant bg-surface px-4 py-2 text-xs font-semibold text-on-surface hover:bg-surface-container-low disabled:opacity-50 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleSubmitDecision}
                disabled={submittingDecision}
                className={`inline-flex items-center gap-1.5 rounded-lg px-4 py-2 text-xs font-semibold text-white transition-colors disabled:opacity-50 cursor-pointer ${
                  decisionModal === 'APPROVE'
                    ? 'bg-emerald-600 hover:bg-emerald-700'
                    : 'bg-rose-600 hover:bg-rose-700'
                }`}
              >
                {submittingDecision ? (
                  <>
                    <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>
                    Đang xử lý...
                  </>
                ) : (
                  <>
                    <span className="material-symbols-outlined text-[16px]">
                      {decisionModal === 'APPROVE' ? 'check' : 'send'}
                    </span>
                    {decisionModal === 'APPROVE' ? 'Xác nhận xuất bản' : 'Gửi yêu cầu sửa'}
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
