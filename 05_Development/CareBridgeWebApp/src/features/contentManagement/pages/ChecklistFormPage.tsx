import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, CalendarRange, ClipboardList, Plus, Save, Send, Trash2, Users } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';
import type {
  ChecklistRecipientRole,
  ChecklistSubstage,
  ChecklistTargetSubject,
  ChecklistSupportFunction,
  ChecklistTemplateStatus,
  ChecklistTemplateType,
  ContentStage,
  ReviewFeedback,
} from '../models/content';
import { CHECKLIST_SUPPORT_FUNCTION_OPTIONS, STAGE_LABELS, STAGE_OPTIONS } from '../models/content';
import { checklistSequenceLabel } from './checklistApprovalPresentation';
import {
  createChecklistTemplate,
  fetchChecklistTemplateDetail,
  updateChecklistTemplate,
} from '../services/contentApi';

interface ItemRow {
  key: string;
  id?: string;
  itemText: string;
  description: string;
  isRequired: boolean;
  targetSubject: ChecklistTargetSubject;
  supportFunction: ChecklistSupportFunction | '';
}

const ROLE_ORDER: ChecklistRecipientRole[] = ['MOTHER', 'FAMILY'];

const SUBSTAGE_OPTIONS: Partial<Record<ContentStage, ChecklistSubstage[]>> = {
  PREGNANCY: [
    { code: 'PREGNANCY_LMP_WEEK_0_12', anchor: 'LMP', startInclusive: 0, endInclusive: 12, unit: 'WEEK' },
    { code: 'PREGNANCY_EDD_WEEK_0_40', anchor: 'EDD', startInclusive: 0, endInclusive: 40, unit: 'WEEK' },
  ],
  POSTPARTUM: [
    { code: 'POSTPARTUM_DAY_0_7', anchor: 'DELIVERY_DATE', startInclusive: 0, endInclusive: 7, unit: 'DAY' },
    { code: 'POSTPARTUM_WEEK_0_6', anchor: 'DELIVERY_DATE', startInclusive: 0, endInclusive: 6, unit: 'WEEK' },
  ],
};

const AUTHORABLE_STAGES: readonly ContentStage[] = STAGE_OPTIONS.map(({ value }) => value);

function newRow(): ItemRow {
  return {
    key: crypto.randomUUID(),
    itemText: '',
    description: '',
    isRequired: true,
    targetSubject: 'MOTHER',
    supportFunction: '',
  };
}

export default function ChecklistFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [templateType, setTemplateType] = useState<ChecklistTemplateType>('MANDATORY');
  const [recipientRoles, setRecipientRoles] = useState<ChecklistRecipientRole[]>(['MOTHER']);
  const [stage, setStage] = useState<ContentStage | ''>('');
  const [substage, setSubstage] = useState<ChecklistSubstage | null>(null);
  const [displayOrder, setDisplayOrder] = useState(0);
  const [items, setItems] = useState<ItemRow[]>([newRow()]);
  const [status, setStatus] = useState<ChecklistTemplateStatus>('DRAFT');
  const [isLoading, setIsLoading] = useState(isEdit);
  const [loadError, setLoadError] = useState('');
  const [submitting, setSubmitting] = useState<'draft' | 'submit' | null>(null);
  const [submitError, setSubmitError] = useState('');
  const [reviewFeedback, setReviewFeedback] = useState<ReviewFeedback | null>(null);
  const [versionNo, setVersionNo] = useState<number | null>(null);

  const loadDetail = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setLoadError('');
    try {
      const data = await fetchChecklistTemplateDetail(id);
      setName(data.name);
      setDescription(data.description ?? '');
      setTemplateType(data.templateType ?? 'MANDATORY');
      const loadedRoles = data.recipientRoles ?? ['MOTHER'];
      const hasMotherRecipient = loadedRoles.includes('MOTHER');
      setRecipientRoles(loadedRoles);
      setStage(hasMotherRecipient ? (data.stage ?? '') : '');
      setDisplayOrder(data.displayOrder ?? 0);
      setSubstage(hasMotherRecipient && data.stage !== 'PRE_PREGNANCY'
        ? (data.substage ?? null)
        : null);
      setStatus(data.status);
      setVersionNo(data.versionNo);
      setReviewFeedback(data.latestReviewFeedback ?? null);
      setItems(data.items.length > 0
        ? [...data.items].sort((a, b) => a.order - b.order).map((item) => ({
            key: item.id,
            id: item.id,
            itemText: item.itemText,
            description: item.description ?? '',
            isRequired: item.isRequired,
            targetSubject: item.targetSubject ?? 'MOTHER',
            supportFunction: item.supportFunction ?? '',
          }))
        : [newRow()]);
    } catch {
      setLoadError('Không thể tải checklist để chỉnh sửa. Vui lòng thử lại hoặc kiểm tra quyền Content Admin.');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (isEdit) void loadDetail();
  }, [isEdit, loadDetail]);

  const hasMotherRecipient = recipientRoles.includes('MOTHER');
  const sequenceEligible = templateType === 'MANDATORY'
    && recipientRoles.length === 1
    && recipientRoles[0] === 'MOTHER'
    && stage === 'PRE_PREGNANCY';
  const legacyPreconceptionWarning = templateType === 'MANDATORY'
    && hasMotherRecipient
    && stage === 'PRE_PREGNANCY'
    && displayOrder <= 0;
  const isImmutable = status === 'APPROVED' || status === 'ARCHIVED';
  const isValid = name.trim().length > 0
    && recipientRoles.length > 0
    && (!hasMotherRecipient || (stage !== ''
      && (stage === 'PRE_PREGNANCY' || (substage !== null && substage.anchor !== 'NONE'))))
    && items.filter((row) => row.itemText.trim()).every((row) => Boolean(row.targetSubject))
    && (!sequenceEligible || (Number.isInteger(displayOrder) && displayOrder >= 0));

  const toggleRole = (role: ChecklistRecipientRole) => {
    setRecipientRoles((previous) => {
      const next = previous.includes(role)
        ? previous.filter((value) => value !== role)
        : [...previous, role];
      const ordered = ROLE_ORDER.filter((value) => next.includes(value));
      if (!ordered.includes('MOTHER')) {
        setStage('');
        setSubstage(null);
      }
      return ordered;
    });
  };

  const updateStage = (value: ContentStage | '') => {
    setStage(value);
    setSubstage(value ? (SUBSTAGE_OPTIONS[value]?.[0] ?? null) : null);
  };

  const updateSubstage = (code: string) => {
    setSubstage(stage ? (SUBSTAGE_OPTIONS[stage]?.find((option) => option.code === code) ?? null) : null);
  };

  const updateItem = (key: string, patch: Partial<ItemRow>) => {
    setItems((previous) => previous.map((row) => (row.key === key ? { ...row, ...patch } : row)));
  };

  const buildItemsPayload = () => items
    .filter((row) => row.itemText.trim())
    .map((row, index) => {
      const description = row.description.trim();
      return {
        ...(row.id ? { id: row.id } : {}),
        itemText: row.itemText.trim(),
        order: index + 1,
        isRequired: row.isRequired,
        targetSubject: row.targetSubject,
        ...(description ? { description } : {}),
        ...(row.supportFunction ? { supportFunction: row.supportFunction } : {}),
      };
    });

  const submit = async (targetStatus: 'DRAFT' | 'PENDING_REVIEW') => {
    if (!isValid || isImmutable) return;
    setSubmitting(targetStatus === 'PENDING_REVIEW' ? 'submit' : 'draft');
    setSubmitError('');
    const normalizedStage = hasMotherRecipient ? (stage || null) : null;
    const normalizedSubstage = hasMotherRecipient && stage !== 'PRE_PREGNANCY' ? substage : null;
    try {
      const commonPayload = {
        name: name.trim(),
        description: description.trim() || undefined,
        templateType,
        recipientRoles,
        stage: normalizedStage,
        substage: normalizedSubstage,
        displayOrder: sequenceEligible ? displayOrder : 0,
      };
      const itemsPayload = buildItemsPayload();
      if (isEdit && id) {
        await updateChecklistTemplate(id, { ...commonPayload, status: targetStatus, items: itemsPayload });
        navigate(`/content/checklists/${id}`);
      } else {
        const created = await createChecklistTemplate({ ...commonPayload, items: itemsPayload });
        if (targetStatus === 'PENDING_REVIEW') {
          await updateChecklistTemplate(created.id, {
            ...commonPayload,
            status: 'PENDING_REVIEW',
            items: undefined,
          });
        }
        navigate(`/content/checklists/${created.id}`);
      }
    } catch {
      setSubmitError(isEdit ? 'Cập nhật thất bại. Vui lòng thử lại.' : 'Không thể tạo checklist. Vui lòng thử lại.');
    } finally {
      setSubmitting(null);
    }
  };

  if (isLoading) return <div className="p-8 text-center text-outline py-16">Đang tải...</div>;
  if (loadError) {
    return (
      <div className="mx-auto max-w-3xl p-6 font-sans">
        <div role="alert" className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error font-semibold">{loadError}</div>
        <button type="button" onClick={() => navigate('/content/checklists')} className="py-2.5 px-6 rounded-full border border-outline-variant bg-surface text-on-surface text-sm font-semibold hover:bg-surface-container-low cursor-pointer">
          Quay lại danh sách
        </button>
      </div>
    );
  }

  const card = 'bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest';
  const field = 'w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-surface-container-low disabled:text-outline';

  return (
    <main className="p-8 font-sans">
      <button type="button" onClick={() => navigate(-1)} className="mb-6 inline-flex items-center gap-2 py-2 px-4 rounded-full border border-outline-variant bg-surface text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low cursor-pointer shadow-sm">
        <ArrowLeft size={18} aria-hidden="true" /> Quay lại
      </button>

      <header className="mb-6 flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="m-0 text-[26px] font-bold text-on-surface">{isEdit ? 'Chỉnh sửa Checklist' : 'Tạo Checklist mới'}</h1>
            {isEdit && versionNo !== null && <span className="inline-flex items-center rounded-full bg-surface-container-low px-3 py-1 text-xs font-semibold text-primary">Version v{versionNo}</span>}
          </div>
          <p className="mt-1 text-sm text-on-surface-variant">Thiết lập đúng người nhận, giai đoạn và đối tượng cho từng mục.</p>
        </div>
        <div className="flex flex-wrap gap-3">
          <button aria-label="Save draft" type="button" onClick={() => void submit('DRAFT')} disabled={!isValid || isImmutable || submitting !== null} className="inline-flex items-center gap-2 py-2.5 px-6 rounded-full border border-outline-variant bg-surface text-on-surface text-sm font-semibold hover:bg-surface-container-low cursor-pointer disabled:opacity-40">
            <Save size={18} aria-hidden="true" /> {submitting === 'draft' ? 'Đang lưu...' : 'Lưu nháp'}
          </button>
          <button aria-label="Submit for review" type="button" onClick={() => void submit('PENDING_REVIEW')} disabled={!isValid || isImmutable || submitting !== null} className="inline-flex items-center gap-2 py-2.5 px-6 rounded-full bg-primary text-on-primary text-sm font-semibold shadow-md hover:bg-primary/90 cursor-pointer disabled:opacity-40">
            <Send size={18} aria-hidden="true" /> {submitting === 'submit' ? 'Đang gửi...' : 'Gửi phê duyệt'}
          </button>
        </div>
      </header>

      {submitError && <div role="alert" className="mb-6 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">{submitError}</div>}
      {isImmutable && <div role="status" className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">Phiên bản đã duyệt là bất biến. Hãy clone để tạo một bản nháp mới.</div>}
      <ReviewFeedbackNotice feedback={reviewFeedback} />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="flex min-w-0 flex-col gap-6">
          <section className={card}>
            <div className="mb-5 flex items-center gap-2.5"><ClipboardList className="text-primary" size={22} /><h2 className="m-0 text-lg font-bold text-on-surface">Thông tin cơ bản</h2></div>
            <div className="grid gap-5">
              <label className="grid gap-2 text-sm font-semibold text-on-surface">Tên checklist <input aria-label="Template name" disabled={isImmutable} value={name} onChange={(event) => setName(event.target.value)} className={field} /></label>
              <label className="grid gap-2 text-sm font-semibold text-on-surface">Mô tả <textarea aria-label="Template description" disabled={isImmutable} value={description} onChange={(event) => setDescription(event.target.value)} rows={3} className={`${field} py-3`} /></label>
            </div>
          </section>

          <section aria-label="Checklist type" className={card}>
            <div className="mb-5 flex items-center gap-2.5"><ClipboardList className="text-primary" size={22} /><h2 className="m-0 text-lg font-bold text-on-surface">Loại checklist</h2></div>
            <div className="grid gap-3 sm:grid-cols-2">
              <label className={`cursor-pointer rounded-2xl border p-4 ${templateType === 'MANDATORY' ? 'border-primary bg-surface-container-low' : 'border-outline-variant bg-surface'}`}>
                <input aria-label="Mandatory checklist" type="radio" name="templateType" value="MANDATORY" disabled={isImmutable} checked={templateType === 'MANDATORY'} onChange={() => setTemplateType('MANDATORY')} className="mr-2 accent-primary" />
                <span className="font-semibold text-on-surface">Bắt buộc</span>
                <p className="mt-1 text-xs text-on-surface-variant">Tự động hiển thị trong “Việc hôm nay” của người dùng phù hợp.</p>
              </label>
              <label className={`cursor-pointer rounded-2xl border p-4 ${templateType === 'OPTIONAL' ? 'border-primary bg-surface-container-low' : 'border-outline-variant bg-surface'}`}>
                <input aria-label="Optional checklist" type="radio" name="templateType" value="OPTIONAL" disabled={isImmutable} checked={templateType === 'OPTIONAL'} onChange={() => setTemplateType('OPTIONAL')} className="mr-2 accent-primary" />
                <span className="font-semibold text-on-surface">Không bắt buộc</span>
                <p className="mt-1 text-xs text-on-surface-variant">Người dùng tự thêm từ tab Checklist trong “Nội dung & FAQ”.</p>
              </label>
            </div>
          </section>

          <section className={card}>
            <div className="mb-5 flex items-center gap-2.5"><Users className="text-primary" size={22} /><h2 className="m-0 text-lg font-bold text-on-surface">Người nhận</h2></div>
            <div className="grid gap-3 sm:grid-cols-2">
              {ROLE_ORDER.map((role) => (
                <label key={role} className={`flex min-h-12 cursor-pointer items-center gap-3 rounded-2xl border px-4 text-sm font-semibold transition-colors ${recipientRoles.includes(role) ? 'border-primary bg-surface-container-low text-primary' : 'border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-bright'}`}>
                  <input aria-label={`Recipient ${role}`} type="checkbox" disabled={isImmutable} checked={recipientRoles.includes(role)} onChange={() => toggleRole(role)} className="h-4 w-4 accent-primary" />
                  {role === 'MOTHER' ? 'Mẹ' : 'Gia đình'}
                </label>
              ))}
            </div>
            {recipientRoles.length === 0 && <p role="alert" className="mt-3 text-xs font-semibold text-error">Cần chọn ít nhất một người nhận.</p>}
          </section>

          {hasMotherRecipient && (
            <section aria-label="Lifecycle targeting" className={card}>
              <div className="mb-5 flex items-center gap-2.5"><CalendarRange className="text-primary" size={22} /><h2 className="m-0 text-lg font-bold text-on-surface">Giai đoạn áp dụng</h2></div>
              <label className="grid gap-2 text-sm font-semibold text-on-surface">Giai đoạn
                <select aria-label="Lifecycle stage" disabled={isImmutable} value={stage} onChange={(event) => updateStage(event.target.value as ContentStage | '')} className={field}>
                  <option value="">Chọn giai đoạn</option>
                  {AUTHORABLE_STAGES.map((value) => <option key={value} value={value}>{STAGE_LABELS[value]}</option>)}
                </select>
              </label>
              {stage && stage !== 'PRE_PREGNANCY' && (
                <label className="mt-4 grid gap-2 text-sm font-semibold text-on-surface">Cửa sổ vòng đời
                  <select aria-label="Lifecycle substage" disabled={isImmutable} value={substage?.code ?? ''} onChange={(event) => updateSubstage(event.target.value)} className={field}>
                    {(SUBSTAGE_OPTIONS[stage] ?? []).map((option) => (
                      <option key={option.code} value={option.code}>{option.code}</option>
                    ))}
                  </select>
                </label>
              )}
              {sequenceEligible && (
                <label className="mt-4 grid gap-2 text-sm font-semibold text-on-surface">
                  Vị trí bộ checklist (0 = chưa tham gia chuỗi)
                  <input
                    aria-label="Checklist sequence position"
                    type="number"
                    min={0}
                    max={1000}
                    step={1}
                    disabled={isImmutable}
                    value={displayOrder}
                    onChange={(event) => setDisplayOrder(Number(event.target.value))}
                    className={field}
                  />
                  <span className="text-xs font-normal text-on-surface-variant">
                    Nhập 1, 2, 3... theo thứ tự các bộ. Vị trí được kiểm tra lại khi phê duyệt.
                  </span>
                  {legacyPreconceptionWarning && (
                    <span role="note" className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs font-normal text-amber-900">
                      {checklistSequenceLabel(displayOrder)}: checklist này không thuộc chuỗi 1, 2, 3... và không thể hoạt động cùng chuỗi mới.
                    </span>
                  )}
                </label>
              )}
              {legacyPreconceptionWarning && !sequenceEligible && (
                <div role="note" className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs font-normal text-amber-900">
                  Checklist Mẹ + Gia đình ở vị trí legacy (0) không thuộc chuỗi 1, 2, 3... và không thể hoạt động cùng chuỗi mới. Hãy lưu trữ hoặc tắt checklist này trước khi xuất bản bộ chuỗi.
                </div>
              )}
              {substage && <div className="mt-4 rounded-2xl bg-surface-bright border border-surface-container-highest p-4"><strong className="text-on-surface text-sm">{substage.code}</strong><p className="mt-1 text-xs text-on-surface-variant">{substage.anchor} · {substage.startInclusive}–{substage.endInclusive} {substage.unit}</p></div>}
            </section>
          )}

          <section className={card}>
            <div className="mb-5 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2.5"><ClipboardList className="text-primary" size={22} /><h2 className="m-0 text-lg font-bold text-on-surface">Danh sách mục</h2></div>
              <button type="button" disabled={isImmutable} onClick={() => setItems((previous) => [...previous, newRow()])} className="inline-flex items-center gap-1.5 py-2 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-primary hover:bg-surface-container-low cursor-pointer disabled:opacity-40"><Plus size={16} /> Thêm mục</button>
            </div>
            <div className="grid gap-4">
              {items.map((row, index) => (
                <div key={row.key} className="grid gap-3 rounded-2xl border border-surface-container-highest bg-surface-bright p-4 md:grid-cols-[minmax(0,1fr)_160px_auto] md:items-end">
                  <label className="grid gap-2 text-sm font-semibold text-on-surface">Mục {index + 1}<input aria-label={`Item ${index + 1} text`} disabled={isImmutable} value={row.itemText} onChange={(event) => updateItem(row.key, { itemText: event.target.value })} className={field} /></label>
                  <label className="grid gap-2 text-sm font-semibold text-on-surface">Đối tượng<select aria-label={`Item ${index + 1} target`} disabled={isImmutable} value={row.targetSubject} onChange={(event) => updateItem(row.key, { targetSubject: event.target.value as ChecklistTargetSubject })} className={field}><option value="MOTHER">Mẹ</option><option value="BABY">Em bé</option></select></label>
                  <button aria-label={`Delete item ${index + 1}`} type="button" disabled={isImmutable || items.length === 1} onClick={() => setItems((previous) => previous.filter((item) => item.key !== row.key))} className="flex h-10 w-10 items-center justify-center rounded-xl border border-error-container text-error hover:bg-error-container/20 cursor-pointer disabled:opacity-30 self-end mb-0.5"><Trash2 size={18} /></button>
                  <label className="grid gap-2 text-sm font-semibold text-on-surface md:col-span-3">Nội dung chi tiết
                    <textarea aria-label={`Nội dung chi tiết mục ${index + 1}`} disabled={isImmutable} value={row.description} onChange={(event) => updateItem(row.key, { description: event.target.value })} rows={3} className={`${field} py-3`} />
                  </label>
                  <label className="grid gap-2 text-sm font-semibold text-on-surface md:col-span-2">Chức năng hỗ trợ
                    <select aria-label={`Chức năng hỗ trợ mục ${index + 1}`} disabled={isImmutable} value={row.supportFunction} onChange={(event) => updateItem(row.key, { supportFunction: event.target.value as ChecklistSupportFunction | '' })} className={field}>
                      <option value="">Không liên kết</option>
                      {CHECKLIST_SUPPORT_FUNCTION_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                    </select>
                  </label>
                  <label className="flex items-center gap-2 text-sm font-semibold text-on-surface-variant md:col-span-3"><input type="checkbox" disabled={isImmutable} checked={row.isRequired} onChange={(event) => updateItem(row.key, { isRequired: event.target.checked })} className="h-4 w-4 accent-primary" /> Bắt buộc</label>
                </div>
              ))}
            </div>
          </section>
        </div>

        <aside className="flex flex-col gap-5">
          <section className={card}><h2 className="mb-3 text-base font-bold text-on-surface">Tổng quan</h2><p className="text-sm text-on-surface-variant">{recipientRoles.length} nhóm người nhận</p><p className="text-sm text-on-surface-variant">{items.filter((item) => item.itemText.trim()).length} mục có nội dung</p></section>
          <section className={card}><h2 className="mb-3 text-base font-bold text-on-surface">Lịch sử phiên bản</h2>{isEdit && versionNo !== null ? <><p className="text-sm text-on-surface-variant">Phiên bản hiện tại: v{versionNo}</p><button type="button" onClick={() => navigate(`/content/checklists/${id}/versions`)} className="mt-3 text-xs font-semibold text-primary underline cursor-pointer">Xem toàn bộ lịch sử</button></> : <p className="text-sm text-on-surface-variant">Chưa có lịch sử (Phiên bản mới)</p>}</section>
        </aside>
      </div>
    </main>
  );
}
