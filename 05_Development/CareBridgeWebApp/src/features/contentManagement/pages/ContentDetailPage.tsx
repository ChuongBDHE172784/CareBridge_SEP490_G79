import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchStaffContentDetail, updateContent, archiveContent } from '../services/contentApi';
import type { ContentDetail } from '../models/content';
import { STAGE_LABELS, STATUS_LABELS, TYPE_LABELS } from '../models/content';
import { useAuth } from '../../../shared/auth/useAuth';
import '../richContentBody.css';


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
  const [detail, setDetail] = useState<ContentDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [submittingApproval, setSubmittingApproval] = useState(false);

  const loadDetail = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError('');
    try {
      const data = await fetchStaffContentDetail(id);
      setDetail(data);
    } catch {
      setError('Không tải được nội dung. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

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

  return (
    <div className="p-8 font-sans">
      {/* Breadcrumbs */}
      <div className="flex items-center gap-2 text-[13px] text-outline mb-4">
        {canManage ? (
          <span className="cursor-pointer" onClick={() => navigate(typeListPath)}>{typeLabel}</span>
        ) : (
          <span>{typeLabel}</span>
        )}
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span className="text-on-surface-variant">Chi tiết {typeLabel.toLowerCase()}</span>
      </div>

      {/* Back button */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer mb-6"
      >
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Quay lại
      </button>

      {actionError && <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{actionError}</div>}

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

          {/* Article canvas */}
          <div className="bg-surface rounded-2xl p-8 shadow-md">
            {/* Hero image placeholder */}
            <div
              className="w-full h-[220px] rounded-xl flex items-center justify-center mb-6 bg-[linear-gradient(135deg,#FFE9E3_0%,#F6DACF_100%)]"
            >
              <span className="material-symbols-outlined text-[#C98C7B] text-5xl">image</span>
            </div>

            {/* Body content */}
            <div
              className="rich-content-body text-[15px] leading-7 text-on-surface"
              dangerouslySetInnerHTML={{ __html: detail.body }}
            />
          </div>
        </div>

        {/* Right sidebar */}
        <div className="flex flex-col gap-4">
          {/* Status widget */}
          <div className="bg-surface rounded-2xl p-5 shadow-md">
            <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">TRẠNG THÁI</div>
            <div className="flex items-center gap-2 mb-2">
              <span className={`w-2.5 h-2.5 rounded-full ${statusDotClass(detail.status)}`} />
              <span className="text-sm font-semibold text-on-surface">{STATUS_LABELS[detail.status]}</span>
            </div>
            <div className="text-xs text-outline mb-1">Phiên bản: v{detail.version}</div>
            <div className="text-xs text-outline">Xuất bản: {formatDate(detail.publishedAt)}</div>
          </div>

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
                <button
                  onClick={submitForApproval}
                  disabled={submittingApproval}
                  className="w-full py-3.5 rounded-2xl bg-transparent text-primary border border-outline-variant text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  <span className="material-symbols-outlined text-lg">send</span>
                  {submittingApproval ? 'Đang gửi...' : 'Gửi phê duyệt'}
                </button>
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
    </div>
  );
}
