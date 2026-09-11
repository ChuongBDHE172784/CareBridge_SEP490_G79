import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  activateChecklistVersion,
  archiveChecklistTemplate,
  cloneChecklistVersion,
  decideChecklistTemplate,
  decideExpertChecklist,
  fetchChecklistTemplateDetail,
  reviewMigratedChecklistVersion,
  updateChecklistTemplate,
} from '../services/contentApi';
import type { AdminChecklistTemplateDetail, ChecklistSupportFunction } from '../models/content';
import { CHECKLIST_STATUS_LABELS, CHECKLIST_SUPPORT_FUNCTION_OPTIONS, STAGE_LABELS } from '../models/content';
import {
  checklistCadenceLabel,
  checklistCoexistenceGuidance,
  checklistRecipientLabel,
  checklistSequenceLabel,
  checklistWindowLabel,
} from './checklistApprovalPresentation';
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
const supportFunctionLabel = (value?: ChecklistSupportFunction | null) => (
  CHECKLIST_SUPPORT_FUNCTION_OPTIONS.find((option) => option.value === value)?.label
  ?? value
  ?? 'Không liên kết'
);
function getChecklistTargetIcon(checklist: {
  name?: string;
  stage?: string | null;
  items?: Array<{ targetSubject?: 'MOTHER' | 'BABY' | null }>;
}): 'child_care' | 'pregnant_woman' {
  const hasBabyItem = checklist.items?.some((i) => i.targetSubject === 'BABY');
  const isBabyStage = checklist.stage === 'BABY_CARE';
  const nameLower = (checklist.name || '').toLowerCase();
  const isBabyName = nameLower.includes('bé') || nameLower.includes('trẻ') || nameLower.includes('sơ sinh');

  if (hasBabyItem || isBabyStage || isBabyName) {
    return 'child_care';
  }
  return 'pregnant_woman';
}

const warmBadge = 'inline-flex shrink-0 items-center rounded-full bg-surface-container-low px-3 py-1 text-xs font-semibold text-primary';

export default function ChecklistDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  // System Admin can reach this page read-only from the approval queue
  // (/admin/content-review/checklists/:id) — /content/checklists/:id/edit is CONTENT_ADMIN-only.
  const canManage = hasRole('CONTENT_ADMIN');
  const isExpert = hasRole('EXPERT');
  const isSystemAdmin = hasRole('SYSTEM_ADMIN');
  const canReview = isExpert || isSystemAdmin;
  const [detail, setDetail] = useState<AdminChecklistTemplateDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [submittingApproval, setSubmittingApproval] = useState(false);
  const [versionAction, setVersionAction] = useState<'clone' | 'review' | 'activate' | null>(null);

  // Phê duyệt chỉ mở sau khi người thẩm định đã cuộn hết danh sách mục. Một cái mốc
  // đặt cuối danh sách rẻ hơn đo scrollTop: đúng với mọi chiều cao màn hình, và
  // checklist ngắn không cần cuộn thì mốc đã nằm trong khung nhìn nên mở luôn.
  const [hasReadItems, setHasReadItems] = useState(false);
  const itemsEndRef = useRef<HTMLDivElement | null>(null);

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
        await decideExpertChecklist(detail.id, decisionModal, decisionModal === 'REJECT' ? rejectReason.trim() : undefined);
      } else {
        await decideChecklistTemplate(detail.id, decisionModal, decisionModal === 'REJECT' ? rejectReason.trim() : undefined);
      }
      setDecisionSuccess(
        decisionModal === 'APPROVE'
          ? 'Mẫu checklist đã được phê duyệt và xuất bản thành công.'
          : 'Đã gửi yêu cầu chỉnh sửa cho tác giả checklist.'
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
      const data = await fetchChecklistTemplateDetail(id);
      setHasReadItems(false);
      setDetail(data);
    } catch {
      setError('Không tải được nội dung. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

  useEffect(() => {
    if (!detail || hasReadItems) return;
    const node = itemsEndRef.current;
    if (!node) return;
    // Trình duyệt không hỗ trợ thì mở nút — thà bỏ lỡ một lần nhắc còn hơn khoá
    // cứng người thẩm định khỏi hàng đợi của họ.
    if (typeof IntersectionObserver === 'undefined') {
      setHasReadItems(true);
      return;
    }
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) setHasReadItems(true);
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, [detail, hasReadItems]);

  const submitForApproval = async () => {
    if (!detail) return;
    setSubmittingApproval(true);
    setActionError('');
    try {
      await updateChecklistTemplate(detail.id, {
        name: detail.name,
        templateType: detail.templateType,
        checklistContractVersion: detail.checklistContractVersion ?? null,
        description: detail.description,
        recipientRoles: detail.recipientRoles ?? ['MOTHER'],
        stage: detail.stage,
        substage: detail.stage === 'PRE_PREGNANCY' ? null : (detail.substage ?? null),
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
  const isTargetlessV2 = detail.checklistContractVersion === 2;

  return (
    <main data-testid="checklist-detail-page" className="min-h-screen bg-background p-8 font-sans">
      {/* Breadcrumbs */}
      <div className="mb-4 flex items-center gap-2 text-[13px] text-outline">
        {isExpert ? (
          <button type="button" className="inline-flex items-center gap-1 font-semibold text-primary cursor-pointer hover:underline border-0 bg-transparent p-0" onClick={() => navigate('/expert/content-approval')}>
            <span aria-hidden="true" className="material-symbols-outlined text-base">rate_review</span>
            Thẩm định nội dung
          </button>
        ) : isSystemAdmin ? (
          <button type="button" className="inline-flex items-center gap-1 font-semibold text-primary cursor-pointer hover:underline border-0 bg-transparent p-0" onClick={() => navigate('/admin/content-approval-queue')}>
            <span aria-hidden="true" className="material-symbols-outlined text-base">rate_review</span>
            Hàng đợi phê duyệt
          </button>
        ) : canManage ? (
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
        onClick={() => {
          if (isExpert) navigate('/expert/content-approval');
          else if (isSystemAdmin) navigate('/admin/content-approval-queue');
          else navigate(-1);
        }}
        className="mb-6 inline-flex items-center gap-2 py-2 px-5 rounded-full border border-outline-variant bg-surface text-sm font-semibold text-on-surface-variant shadow-sm hover:bg-surface-container-low cursor-pointer transition-colors"
      >
        <span aria-hidden="true" className="material-symbols-outlined text-lg">arrow_back</span>
        {isExpert || isSystemAdmin ? 'Quay lại hàng đợi' : 'Quay lại'}
      </button>

      {decisionSuccess && (
        <div className="rounded-2xl bg-emerald-500/10 border border-emerald-500/20 p-4 mb-4 text-emerald-700 dark:text-emerald-300 text-sm flex items-center gap-2">
          <span className="material-symbols-outlined text-emerald-600">check_circle</span>
          {decisionSuccess}
        </div>
      )}
      {actionError && <div role="alert" className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm font-semibold text-error">{actionError}</div>}
      {canManage && <ReviewFeedbackNotice feedback={detail.latestReviewFeedback} />}

      <div data-testid="checklist-detail-layout" className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
        {/* Main content area */}
        <div>
          <h1 className="mb-5 mt-0 text-[28px] font-bold leading-[1.3] text-on-surface flex items-center gap-2.5">
            <span aria-hidden="true" className="material-symbols-outlined text-primary text-2xl select-none">{getChecklistTargetIcon(detail)}</span>
            <span>{detail.name}</span>
          </h1>

          {/* Metadata card */}
          <div className="mb-6 flex flex-wrap gap-8 rounded-2xl border border-surface-container-highest bg-surface p-5 shadow-md">
            <div>
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">GIAI ĐOẠN</div>
              <div className="text-sm font-medium text-on-surface">
                {detail.stage ? STAGE_LABELS[detail.stage] : 'Trung lập theo vòng đời'}
              </div>
            </div>
            <div>
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">HỢP ĐỒNG</div>
              <div className="text-sm font-medium text-on-surface">
                {isTargetlessV2 ? 'V2 · Khuyến nghị targetless' : 'V1 · Tương thích target'}
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
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">CHUỖI CHECKLIST</div>
              <div className="text-sm font-medium text-on-surface">{checklistSequenceLabel(detail.displayOrder, detail.stage)}</div>
            </div>
            <div>
              <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">CỬA SỔ VÒNG ĐỜI</div>
              <span className={warmBadge}>{checklistWindowLabel(detail)}</span>
            </div>
            {detail.planNumber != null && (
              <div>
                <div className="mb-1 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">NHỊP CHECKLIST</div>
                <div className="text-sm font-medium text-on-surface">
                  Plan {detail.planNumber} · {detail.section ?? 'chung'} · {checklistCadenceLabel(detail.scheduleType, detail.materializationPolicy)}
                </div>
            </div>
            )}
          </div>

          {(detail.checklistQuarantineReasonCode || detail.migrationReviewRequired || detail.provenance) && (
            <section aria-label="Trạng thái dữ liệu checklist" className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 p-5 shadow-sm">
              <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-amber-950">
                <span aria-hidden="true" className="material-symbols-outlined text-lg">{detail.checklistQuarantineReasonCode ? 'lock' : 'fact_check'}</span>
                {detail.checklistQuarantineReasonCode ? 'Đang cách ly' : 'Thông tin nguồn checklist'}
              </div>
              {detail.checklistQuarantineReasonCode && <p className="mb-2 text-sm text-amber-900">Lý do: {detail.checklistQuarantineReasonCode}</p>}
              {detail.migrationReviewRequired && <p className="mb-2 text-sm text-amber-900">Phiên bản nhập cần được rà soát trước khi phân phối.</p>}
              {detail.provenance && (
                <dl className="grid grid-cols-1 gap-2 text-xs text-amber-950 sm:grid-cols-2">
                  <div><dt className="font-semibold">Nguồn</dt><dd className="break-words">{detail.provenance.sourceArtifactPath ?? 'Chưa có'}</dd></div>
                  <div><dt className="font-semibold">Import batch</dt><dd className="break-words">{detail.provenance.importBatchId ?? 'Chưa có'}</dd></div>
                  <div><dt className="font-semibold">Manifest hash</dt><dd className="break-words">{detail.provenance.renderedManifestHash ?? 'Chưa có'}</dd></div>
                </dl>
              )}
            </section>
          )}

          {detail.stage === 'PRE_PREGNANCY' && detail.templateType === 'MANDATORY' && (
            <div role="note" className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              {checklistCoexistenceGuidance(detail.displayOrder)}
              <span className="mt-1 block text-xs">Người nhận: {checklistRecipientLabel(detail.recipientRoles)}</span>
            </div>
          )}

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
                        {`Thứ tự: ${item.order} · ${item.isRequired ? 'Bắt buộc' : 'Không bắt buộc'}`}
                      </div>
                      {item.description && (
                        <div className="mt-3 rounded-xl border border-surface-container-highest bg-background/70 p-3">
                          <div className="text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">NỘI DUNG CHI TIẾT</div>
                          <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-on-surface-variant">{item.description}</p>
                        </div>
                      )}
                      <div className="mt-2 flex flex-wrap items-center gap-2">
                        {!isTargetlessV2 && item.targetSubject && (
                          <span
                            aria-label={`Đối tượng mục ${item.order}: ${targetLabel(item.targetSubject)}`}
                            className="inline-flex items-center rounded-full bg-surface-container-low px-2.5 py-0.5 text-xs font-medium text-primary"
                          >
                            {targetLabel(item.targetSubject)}
                          </span>
                        )}
                        <span
                          aria-label={`Chức năng hỗ trợ mục ${item.order}: ${supportFunctionLabel(item.supportFunction)}`}
                          className="inline-flex items-center rounded-full border border-outline-variant bg-surface px-2.5 py-0.5 text-xs font-medium text-on-surface-variant"
                        >
                          Chức năng hỗ trợ: {supportFunctionLabel(item.supportFunction)}
                        </span>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
            {/* Mốc đánh dấu đã đọc hết — xem khối state ở đầu file. */}
            <div ref={itemsEndRef} aria-hidden="true" className="h-px w-full" />
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

          {/* Review actions card for Expert / System Admin */}
          {canReview && detail.status === 'PENDING_REVIEW' && (
            <div className="rounded-2xl border border-primary/20 bg-surface p-5 shadow-md">
              <div className="text-[11px] font-bold text-primary uppercase tracking-[0.05em] mb-2 flex items-center gap-1.5">
                <span className="material-symbols-outlined text-base">rate_review</span>
                THẨM ĐỊNH & PHÊ DUYỆT
              </div>
              <p className="text-xs text-on-surface-variant mb-4 leading-relaxed">
                Rà soát kỹ nội dung danh mục, chức năng hỗ trợ và mức độ ưu tiên của checklist trước khi phê duyệt.
              </p>
              <div className="flex flex-col gap-2.5">
                <button
                  type="button"
                  disabled={!hasReadItems}
                  title={hasReadItems ? undefined : 'Đọc hết danh sách mục trước khi phê duyệt'}
                  onClick={() => handleOpenDecision('APPROVE')}
                  className="w-full py-3 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold flex items-center justify-center gap-2 shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed enabled:cursor-pointer"
                >
                  <span className="material-symbols-outlined text-base">
                    {hasReadItems ? 'check_circle' : 'lock'}
                  </span>
                  Phê duyệt & Xuất bản
                </button>
                {!hasReadItems && (
                  <p className="text-[11px] text-outline text-center px-1 -mt-1">
                    Cuộn hết danh sách mục để mở nút phê duyệt.
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
              {editable ? (
                <button
                  type="button"
                  aria-label="Edit checklist"
                  onClick={() => navigate(`/content/checklists/${detail.id}/edit`)}
                  className="inline-flex min-h-12 py-3 px-6 w-full items-center justify-center gap-2 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer shadow-md hover:bg-primary/90"
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
                  className="inline-flex min-h-12 py-3 px-6 w-full items-center justify-center gap-2 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer shadow-md hover:bg-primary/90 disabled:opacity-50"
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
                  className="inline-flex min-h-12 py-3 px-6 w-full items-center justify-center gap-2 rounded-full border border-outline-variant bg-surface text-primary text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
                >
                  <span aria-hidden="true" className="material-symbols-outlined text-lg">send</span>
                  {submittingApproval ? 'Đang gửi...' : 'Gửi phê duyệt'}
                </button>
              )}
              <button
                type="button"
                aria-label="Delete checklist"
                onClick={handleDelete}
                  className="inline-flex min-h-12 py-3 px-6 w-full items-center justify-center gap-2 rounded-full border border-error-container bg-surface text-error text-sm font-semibold cursor-pointer hover:bg-error-container/20"
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
                  className="inline-flex min-h-12 py-3 px-6 w-full items-center justify-center gap-2 rounded-full border border-outline-variant bg-surface-container-low text-primary text-sm font-semibold cursor-pointer hover:bg-surface-bright disabled:opacity-50"
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
                  className="inline-flex min-h-12 py-3 px-6 w-full items-center justify-center gap-2 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer shadow-md hover:bg-primary/90 disabled:opacity-50"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-lg">rocket_launch</span>
              {versionAction === 'activate' ? 'Đang kích hoạt...' : 'Kích hoạt phiên bản đã rà soát'}
            </button>
          )}
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
                  ? `Bạn đang phê duyệt mẫu checklist "${detail.name}". Sau khi xuất bản, checklist sẽ có hiệu lực và được phân phối tới người dùng.`
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
                    placeholder="Ví dụ: Cần điều chỉnh lại thứ tự các đầu mục; kiểm tra lại nội dung hướng dẫn y khoa..."
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
    </main>
  );
}
