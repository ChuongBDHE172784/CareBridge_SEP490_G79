import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  activateChecklistVersion,
  archiveChecklistTemplate,
  cloneChecklistVersion,
  fetchChecklistTemplateDetail,
  reviewMigratedChecklistVersion,
  updateChecklistTemplate,
} from '../services/contentApi';
import type { AdminChecklistTemplateDetail } from '../models/content';
import { CHECKLIST_STATUS_LABELS, STAGE_LABELS } from '../models/content';
import { useAuth } from '../../../shared/auth/useAuth';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';

function statusDotClass(status: string): string {
  if (status === 'APPROVED') return 'bg-emerald-600';
  if (status === 'DRAFT') return 'bg-outline';
  if (status === 'PENDING_REVIEW') return 'bg-amber-600';
  return 'bg-error';
}

const recipientLabel = (role: 'MOTHER' | 'FAMILY') => (role === 'MOTHER' ? 'Mẹ' : 'Gia đình');
const targetLabel = (target: 'MOTHER' | 'BABY') => (target === 'MOTHER' ? 'Mẹ' : 'Em bé');
const warmBadge = 'inline-flex shrink-0 items-center rounded-full bg-surface-container-low px-3 py-1 text-xs font-semibold text-primary';

export default function ChecklistDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  // System Admin can reach this page read-only from the approval queue
  // (/admin/content-review/checklists/:id) — /content/checklists/:id/edit is CONTENT_ADMIN-only.
  const canManage = hasRole('CONTENT_ADMIN');
  const canReview = hasRole('SYSTEM_ADMIN');
  const [detail, setDetail] = useState<AdminChecklistTemplateDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [submittingApproval, setSubmittingApproval] = useState(false);
  const [versionAction, setVersionAction] = useState<'clone' | 'review' | 'activate' | null>(null);

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
        templateType: detail.templateType,
        description: detail.description,
        recipientRoles: detail.recipientRoles ?? ['MOTHER'],
        stage: detail.stage,
        substage: detail.substage ?? null,
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

  const requireVersionIdentity = () => {
    if (!detail?.lineageId || !detail.versionId) {
      setActionError('Thiếu định danh lineage/version. Vui lòng tải lại trang.');
      return null;
    }
    return { lineageId: detail.lineageId, versionId: detail.versionId };
  };

  const handleClone = async () => {
    const identity = requireVersionIdentity();
    if (!identity) return;
    setVersionAction('clone');
    setActionError('');
    try {
      const clone = await cloneChecklistVersion(identity.lineageId, identity.versionId);
      navigate(`/content/checklists/${clone.id}/edit`);
    } catch {
      setActionError('Không thể clone phiên bản đã duyệt. Vui lòng thử lại.');
    } finally {
      setVersionAction(null);
    }
  };

  const handleReviewImported = async () => {
    const identity = requireVersionIdentity();
    if (!identity) return;
    setVersionAction('review');
    setActionError('');
    try {
      await reviewMigratedChecklistVersion(identity.lineageId, identity.versionId);
      await loadDetail();
    } catch {
      setActionError('Không thể xác nhận rà soát phiên bản nhập cũ.');
    } finally {
      setVersionAction(null);
    }
  };

  const handleActivate = async () => {
    const identity = requireVersionIdentity();
    if (!identity) return;
    setVersionAction('activate');
    setActionError('');
    try {
      await activateChecklistVersion(identity.lineageId, identity.versionId);
      await loadDetail();
    } catch {
      setActionError('Không thể kích hoạt phiên bản đã rà soát.');
    } finally {
      setVersionAction(null);
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
    return <main className="p-8 text-center text-outline font-sans py-16">Đang tải...</main>;
  }

  if (error || !detail) {
    return (
      <main className="p-8 text-center font-sans py-16">
        <p className="mb-4 text-error font-semibold">{error || 'Không tìm thấy checklist.'}</p>
        <button type="button" onClick={() => navigate(-1)} className="inline-flex items-center gap-2 py-2.5 px-6 rounded-full border border-outline-variant bg-surface text-primary text-sm font-semibold cursor-pointer hover:bg-surface-container-low">
          <span aria-hidden="true" className="material-symbols-outlined text-lg">arrow_back</span>
          Quay lại
        </button>
      </main>
    );
  }

  const editable = detail.status === 'DRAFT' || detail.status === 'PENDING_REVIEW';

  return (
    <main data-testid="checklist-detail-page" className="p-8 font-sans">
      {/* Breadcrumbs */}
      <div className="mb-4 flex items-center gap-2 text-[13px] text-outline">
        {canManage ? (
          <button type="button" aria-label="Checklist" className="inline-flex items-center gap-1 font-semibold text-primary cursor-pointer hover:underline border-0 bg-transparent p-0" onClick={() => navigate('/content/checklists')}>
            <span aria-hidden="true" className="material-symbols-outlined text-base">checklist</span>
            Checklist
          </button>
        ) : (
          <span>Checklist</span>
        )}
        <span aria-hidden="true" className="material-symbols-outlined text-base">chevron_right</span>
        <span className="text-on-surface-variant">Chi tiết checklist</span>
      </div>

      {/* Back button */}
      <button
        type="button"
        aria-label="Quay lại"
        onClick={() => navigate(-1)}
        className="mb-6 inline-flex items-center gap-2 py-2 px-5 rounded-full border border-outline-variant bg-surface text-sm font-semibold text-on-surface-variant shadow-sm hover:bg-surface-container-low cursor-pointer"
      >
        <span aria-hidden="true" className="material-symbols-outlined text-lg">arrow_back</span>
        Quay lại
      </button>

      {actionError && <div role="alert" className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm font-semibold text-error">{actionError}</div>}
      {canManage && <ReviewFeedbackNotice feedback={detail.latestReviewFeedback} />}

      <div data-testid="checklist-detail-layout" className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
        {/* Main content area */}
        <div>
          <h1 className="mb-5 mt-0 text-[28px] font-bold leading-[1.3] text-on-surface">{detail.name}</h1>

          {/* Metadata card */}
          <div className="mb-6 flex flex-wrap gap-8 rounded-2xl border border-surface-container-highest bg-surface p-5 shadow-md">
            <div>
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">GIAI ĐOẠN</div>
              <div className="text-sm font-medium text-on-surface">
                {detail.stage ? STAGE_LABELS[detail.stage] : 'Trung lập theo vòng đời'}
              </div>
            </div>
            <div>
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">SỐ MỤC</div>
              <div className="text-sm font-medium text-on-surface">{detail.items.length} mục</div>
            </div>
            <div>
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">PHIÊN BẢN</div>
              <div className="text-sm font-medium text-on-surface">v{detail.versionNo}</div>
            </div>
            <div>
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">NGƯỜI NHẬN</div>
              <div className="flex flex-wrap gap-1.5">
                {detail.recipientRoles.map((role) => (
                  <span key={role} aria-label={`Người nhận: ${recipientLabel(role)}`} className={warmBadge}>
                    {recipientLabel(role)}
                  </span>
                ))}
              </div>
            </div>
            <div>
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">CỬA SỔ VÒNG ĐỜI</div>
              <span className={warmBadge}>{detail.substage?.code ?? 'Không áp dụng'}</span>
            </div>
          </div>

          {/* Description */}
          {detail.description && (
            <div className="mb-6 rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-md">
              <div className="mb-2 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">MÔ TẢ</div>
              <p className="text-[15px] leading-7 text-on-surface m-0">{detail.description}</p>
            </div>
          )}

          {/* Items list */}
          <div className="rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-md">
            <div className="mb-4 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">
              DANH SÁCH MỤC ({detail.items.length})
            </div>
            {detail.items.length === 0 ? (
              <p className="text-sm text-outline m-0">Checklist này chưa có mục nào.</p>
            ) : (
              <ul className="flex flex-col gap-2.5 p-0 m-0 list-none">
                {[...detail.items].sort((a, b) => a.order - b.order).map(item => (
                  <li key={item.id} className="flex items-start gap-3 rounded-2xl border border-surface-container-highest bg-surface-bright p-4">
                    <span className="material-symbols-outlined mt-0.5 text-xl text-primary">
                      {item.isRequired ? 'check_box' : 'check_box_outline_blank'}
                    </span>
                    <div className="min-w-0 flex-1 break-words">
                      <div className="text-sm font-semibold text-on-surface">{item.itemText}</div>
                      <div className="mt-0.5 text-xs text-outline">
                        Thứ tự: {item.order} · {item.isRequired ? 'Bắt buộc' : 'Không bắt buộc'}
                      </div>
                      <div className="mt-2">
                        <span
                          aria-label={`Đối tượng mục ${item.order}: ${targetLabel(item.targetSubject)}`}
                          className="inline-flex items-center rounded-full bg-surface-container-low px-2.5 py-0.5 text-xs font-medium text-primary"
                        >
                          {targetLabel(item.targetSubject)}
                        </span>
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
          <div className="rounded-2xl border border-surface-container-highest bg-surface p-5 shadow-md">
            <div className="mb-3 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">TRẠNG THÁI</div>
            <div className="flex items-center gap-2 mb-2">
              <span className={`w-2.5 h-2.5 rounded-full ${statusDotClass(detail.status)}`} />
              <span className="text-sm font-semibold text-on-surface">
                {detail.latestReviewFeedback ? 'Cần chỉnh sửa' : CHECKLIST_STATUS_LABELS[detail.status]}
              </span>
            </div>
          </div>

          {/* Action buttons — hidden for read-only reviewers (e.g. System Admin from the approval queue) */}
          {canManage && (
            <>
              {editable ? (
                <button
                  type="button"
                  aria-label="Edit checklist"
                  onClick={() => navigate(`/content/checklists/${detail.id}/edit`)}
                  className="inline-flex py-3 px-6 w-full items-center justify-center gap-2 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer shadow-md hover:bg-primary/90"
                >
                  <span aria-hidden="true" className="material-symbols-outlined text-lg">edit</span>
                  Chỉnh sửa checklist
                </button>
              ) : (
                <>
                  <p className="px-2 text-center text-xs text-outline m-0">
                    Phiên bản đã duyệt hoặc lưu trữ không thể chỉnh sửa trực tiếp.
                  </p>
                  {detail.status === 'APPROVED' && (
                    <button
                      aria-label="Clone approved version"
                      type="button"
                      disabled={versionAction !== null}
                      onClick={handleClone}
                      className="inline-flex py-3 px-6 w-full items-center justify-center gap-2 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer shadow-md hover:bg-primary/90 disabled:opacity-50"
                    >
                      <span aria-hidden="true" className="material-symbols-outlined text-lg">content_copy</span>
                      {versionAction === 'clone' ? 'Đang clone...' : 'Clone thành bản nháp mới'}
                    </button>
                  )}
                </>
              )}
              {detail.status === 'DRAFT' && (
                <button
                  type="button"
                  aria-label="Submit for review"
                  onClick={submitForApproval}
                  disabled={submittingApproval}
                  className="inline-flex py-3 px-6 w-full items-center justify-center gap-2 rounded-full border border-outline-variant bg-surface text-primary text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
                >
                  <span aria-hidden="true" className="material-symbols-outlined text-lg">send</span>
                  {submittingApproval ? 'Đang gửi...' : 'Gửi phê duyệt'}
                </button>
              )}
              <button
                type="button"
                aria-label="Delete checklist"
                onClick={handleDelete}
                className="inline-flex py-3 px-6 w-full items-center justify-center gap-2 rounded-full border border-error-container bg-surface text-error text-sm font-semibold cursor-pointer hover:bg-error-container/20"
              >
                <span aria-hidden="true" className="material-symbols-outlined text-lg">delete</span>
                Xóa
              </button>
            </>
          )}
          {canReview && detail.migrationReviewRequired && (
            <button
              aria-label="Review migrated version"
              type="button"
              disabled={versionAction !== null}
              onClick={handleReviewImported}
              className="inline-flex py-3 px-6 w-full items-center justify-center gap-2 rounded-full border border-outline-variant bg-surface-container-low text-primary text-sm font-semibold cursor-pointer hover:bg-surface-bright disabled:opacity-50"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-lg">verified</span>
              {versionAction === 'review' ? 'Đang xác nhận...' : 'Xác nhận rà soát dữ liệu nhập cũ'}
            </button>
          )}
          {canReview && !detail.migrationReviewRequired && detail.migrationReviewedAt != null && !detail.distributionEnabled
            && detail.status === 'PENDING_REVIEW' && (
            <button
              aria-label="Activate reviewed version"
              type="button"
              disabled={versionAction !== null}
              onClick={handleActivate}
              className="inline-flex py-3 px-6 w-full items-center justify-center gap-2 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer shadow-md hover:bg-primary/90 disabled:opacity-50"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-lg">rocket_launch</span>
              {versionAction === 'activate' ? 'Đang kích hoạt...' : 'Kích hoạt phiên bản đã rà soát'}
            </button>
          )}
        </div>
      </div>
    </main>
  );
}
