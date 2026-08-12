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
import type { AdminChecklistTemplateDetail, ChecklistSupportFunction } from '../models/content';
import { CHECKLIST_STATUS_LABELS, CHECKLIST_SUPPORT_FUNCTION_OPTIONS, STAGE_LABELS } from '../models/content';
import {
  checklistCadenceLabel,
  checklistCoexistenceGuidance,
  checklistProvenanceStatusLabel,
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
        checklistContractVersion: detail.checklistContractVersion ?? null,
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
  const isTargetlessV2 = detail.checklistContractVersion === 2;
  const provenanceSignedOff = detail.provenance?.provenanceStatus === 'SIGNED_OFF';

  return (
    <main data-testid="checklist-detail-page" className="min-h-screen bg-background p-8 font-sans">
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
                {detail.checklistQuarantineReasonCode ? 'Đang cách ly' : 'Trạng thái provenance'}
              </div>
              {detail.checklistQuarantineReasonCode && <p className="mb-2 text-sm text-amber-900">Lý do: {detail.checklistQuarantineReasonCode}</p>}
              {detail.migrationReviewRequired && <p className="mb-2 text-sm text-amber-900">Phiên bản nhập cần được rà soát trước khi phân phối.</p>}
              {!detail.migrationReviewRequired && detail.migrationReviewedAt != null && !provenanceSignedOff && (
                <p className="mb-2 text-sm text-amber-900">Đã rà soát kỹ thuật nhưng chưa có sign-off clinical/content; chưa thể kích hoạt.</p>
              )}
              {detail.provenance && (
                <dl className="grid grid-cols-1 gap-2 text-xs text-amber-950 sm:grid-cols-2">
                  <div><dt className="font-semibold">Nguồn</dt><dd className="break-words">{detail.provenance.sourceArtifactPath ?? 'Chưa có'}</dd></div>
                  <div><dt className="font-semibold">Sign-off</dt><dd>{checklistProvenanceStatusLabel(detail.provenance.provenanceStatus)}</dd></div>
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
                    {!isTargetlessV2 && (
                      <span className="material-symbols-outlined mt-0.5 text-xl text-primary">
                        {item.isRequired ? 'check_box' : 'check_box_outline_blank'}
                      </span>
                    )}
                    <div className="min-w-0 flex-1 break-words">
                      <div className="text-sm font-semibold text-on-surface">{item.itemText}</div>
                      <div className="mt-0.5 text-xs text-outline">
                        {isTargetlessV2
                          ? `Thứ tự: ${item.order} · Nội dung khuyến nghị`
                          : `Thứ tự: ${item.order} · ${item.isRequired ? 'Bắt buộc' : 'Không bắt buộc'}`}
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
          {canReview && !detail.migrationReviewRequired && detail.migrationReviewedAt != null && provenanceSignedOff && !detail.distributionEnabled
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
    </main>
  );
}
