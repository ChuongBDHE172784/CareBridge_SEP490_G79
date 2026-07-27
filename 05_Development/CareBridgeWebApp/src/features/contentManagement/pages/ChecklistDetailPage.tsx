import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchChecklistTemplateDetail, updateChecklistTemplate, archiveChecklistTemplate } from '../services/contentApi';
import type { ChecklistTemplate } from '../models/content';
import { STAGE_LABELS, STATUS_LABELS } from '../models/content';
import { useAuth } from '../../../shared/auth/useAuth';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';

function statusDotClass(status: string): string {
  if (status === 'APPROVED') return 'bg-[#137333]';
  if (status === 'DRAFT') return 'bg-[#616161]';
  if (status === 'PENDING_REVIEW') return 'bg-[#E65100]';
  return 'bg-[#BA1A1A]';
}

export default function ChecklistDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  // System Admin can reach this page read-only from the approval queue
  // (/admin/content-review/checklists/:id) — /content/checklists/:id/edit is CONTENT_ADMIN-only.
  const canManage = hasRole('CONTENT_ADMIN');
  const [detail, setDetail] = useState<ChecklistTemplate | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [submittingApproval, setSubmittingApproval] = useState(false);

  const loadDetail = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError('');
    try {
      const data = await fetchChecklistTemplateDetail(id);
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
      await updateChecklistTemplate(detail.id, {
        name: detail.name,
        description: detail.description,
        stage: detail.stage,
        status: 'PENDING_REVIEW',
        items: undefined,
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
    const reason = window.prompt(`Nhập lý do xóa (lưu trữ) "${detail.name}":`);
    if (reason === null) return;
    if (!reason.trim()) {
      setActionError('Vui lòng nhập lý do trước khi xóa.');
      return;
    }
    try {
      await archiveChecklistTemplate(detail.id, reason.trim());
      navigate('/content/checklists');
    } catch {
      setActionError('Không thể xóa checklist. Vui lòng thử lại.');
    }
  };

  if (isLoading) {
    return <div className="py-12 text-center text-outline font-sans">Đang tải...</div>;
  }

  if (error || !detail) {
    return (
      <div className="py-12 text-center font-sans">
        <p className="text-error mb-4">{error || 'Không tìm thấy checklist.'}</p>
        <button onClick={() => navigate(-1)} className="py-2.5 px-6 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer">
          Quay lại
        </button>
      </div>
    );
  }

  const editable = detail.status === 'DRAFT' || detail.status === 'PENDING_REVIEW';

  return (
    <div className="p-8 font-sans">
      {/* Breadcrumbs */}
      <div className="flex items-center gap-2 text-[13px] text-outline mb-4">
        {canManage ? (
          <span className="cursor-pointer" onClick={() => navigate('/content/checklists')}>Checklist</span>
        ) : (
          <span>Checklist</span>
        )}
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span className="text-on-surface-variant">Chi tiết checklist</span>
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
      {canManage && <ReviewFeedbackNotice feedback={detail.latestReviewFeedback} />}

      <div className="grid grid-cols-[1fr_340px] gap-6">
        {/* Main content area */}
        <div>
          <h1 className="text-[28px] font-bold text-on-surface mt-0 mb-5 leading-[1.3]">{detail.name}</h1>

          {/* Metadata card */}
          <div className="bg-surface rounded-2xl p-5 shadow-md mb-6 flex gap-8 flex-wrap">
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">GIAI ĐOẠN</div>
              <div className="text-sm text-on-surface font-medium">{STAGE_LABELS[detail.stage]}</div>
            </div>
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">SỐ MỤC</div>
              <div className="text-sm text-on-surface font-medium">{detail.items.length} mục</div>
            </div>
          </div>

          {/* Description */}
          {detail.description && (
            <div className="bg-surface rounded-2xl p-6 shadow-md mb-6">
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">MÔ TẢ</div>
              <p className="text-[15px] leading-7 text-on-surface">{detail.description}</p>
            </div>
          )}

          {/* Items list */}
          <div className="bg-surface rounded-2xl p-6 shadow-md">
            <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-4">
              DANH SÁCH MỤC ({detail.items.length})
            </div>
            {detail.items.length === 0 ? (
              <p className="text-sm text-outline">Checklist này chưa có mục nào.</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {[...detail.items].sort((a, b) => a.order - b.order).map(item => (
                  <li key={item.id} className="flex items-start gap-3 py-2.5 px-3 rounded-xl bg-surface-container-lowest">
                    <span className="material-symbols-outlined text-primary text-lg mt-0.5">
                      {item.isRequired ? 'check_box' : 'check_box_outline_blank'}
                    </span>
                    <div className="flex-1">
                      <div className="text-sm text-on-surface">{item.itemText}</div>
                      <div className="text-xs text-outline mt-0.5">
                        Thứ tự: {item.order} · {item.isRequired ? 'Bắt buộc' : 'Không bắt buộc'}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
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
          </div>

          {/* Action buttons — hidden for read-only reviewers (e.g. System Admin from the approval queue) */}
          {canManage && (
            <>
              {editable ? (
                <button
                  onClick={() => navigate(`/content/checklists/${detail.id}/edit`)}
                  className="w-full py-3.5 rounded-2xl bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2"
                >
                  <span className="material-symbols-outlined text-lg">edit</span>
                  Chỉnh sửa checklist
                </button>
              ) : (
                <p className="text-xs text-outline text-center px-2">
                  Checklist đã xuất bản hoặc lưu trữ không thể chỉnh sửa trực tiếp.
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
        </div>
      </div>
    </div>
  );
}
