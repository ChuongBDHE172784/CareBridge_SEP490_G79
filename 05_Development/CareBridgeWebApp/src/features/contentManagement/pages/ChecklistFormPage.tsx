import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, CalendarRange, ClipboardList, Plus, Save, Send, Trash2 } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';
import type {
  ChecklistRecipientRole,
  ChecklistSubstage,
  ChecklistTargetSubject,
  ChecklistSupportFunction,
  ChecklistMaterializationPolicy,
  ChecklistScheduleEndMode,
  ChecklistScheduleType,
  ChecklistWeekBoundaryRule,
  ChecklistTemplateStatus,
  ChecklistTemplateType,
  ContentStage,
  ReviewFeedback,
} from '../models/content';
import { CHECKLIST_SUPPORT_FUNCTION_OPTIONS, STAGE_LABELS, STAGE_OPTIONS } from '../models/content';
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
  isRequired: boolean | null;
  targetSubject: ChecklistTargetSubject | null;
  supportFunction: ChecklistSupportFunction | '';
  repeatWeekly: boolean;
  repeatDaily: boolean;
}



const AUTHORABLE_STAGES: readonly ContentStage[] = STAGE_OPTIONS.map(({ value }) => value);
const DEFAULT_CHECKLIST_CONTRACT_VERSION = 2;
const OPEN_ENDED_OFFSET = 2_147_483_647;
// The authoring surface uses the source-facing week number (1, 2, 3, ...).
// Runtime eligibility remains zero-based, so the conversion is kept at this
// boundary instead of leaking an implementation offset into the form.
const SOURCE_WEEK_OPTIONS = Array.from({ length: 52 }, (_, index) => index + 1);
// Retained for legacy postpartum day windows. New authoring always uses weeks.
const SOURCE_DAY_OPTIONS = Array.from({ length: 53 }, (_, index) => index);
let fallbackRowSequence = 0;

function contractVersionForStage(value: ContentStage | ''): number {
  return value === 'POSTPARTUM' || value === 'BABY_CARE' ? 1 : 2;
}

function defaultSubstage(stage: ContentStage): ChecklistSubstage | null {
  if (stage === 'PRE_PREGNANCY') return null;
  const anchor = stage === 'PREGNANCY'
    ? 'LMP'
    : stage === 'BABY_CARE'
      ? 'BIRTH_DATE'
      : 'DELIVERY_DATE';
  // Pregnancy Plan 1 covers source weeks 1-20.  The legacy postpartum
  // window covered seven source weeks (the stored offset was 0-6).
  const end = stage === 'PREGNANCY' ? 19 : 6;
  return {
    code: `${stage}_${anchor}_WEEK_0_${end}`,
    anchor,
    startInclusive: 0,
    endInclusive: end,
    unit: 'WEEK',
  };
}

function buildSubstage(
  stage: ContentStage,
  anchor: ChecklistSubstage['anchor'],
  sourceStartInclusive: number,
  sourceEndInclusive: number,
  unit: ChecklistSubstage['unit'] = 'WEEK',
  openEnded = false,
): ChecklistSubstage {
  const startInclusive = unit === 'WEEK' ? sourceStartInclusive - 1 : sourceStartInclusive;
  const endInclusive = openEnded
    ? OPEN_ENDED_OFFSET
    : unit === 'WEEK' ? sourceEndInclusive - 1 : sourceEndInclusive;
  return {
    code: `${stage}_${anchor}_${unit}_${startInclusive}_${endInclusive}`,
    anchor,
    startInclusive,
    endInclusive,
    unit,
  };
}

function newRow(targetless = false, repeatWeekly = false, repeatDaily = false): ItemRow {
  return {
    key: typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? `${crypto.randomUUID()}-${fallbackRowSequence++}`
      : `row-${Date.now()}-${fallbackRowSequence++}`,
    itemText: '',
    description: '',
    isRequired: true,
    targetSubject: targetless ? null : 'MOTHER',
    supportFunction: '',
    repeatWeekly,
    repeatDaily,
  };
}

export default function ChecklistFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [templateType, setTemplateType] = useState<ChecklistTemplateType>('MANDATORY');
  const [checklistContractVersion, setChecklistContractVersion] = useState<number>(DEFAULT_CHECKLIST_CONTRACT_VERSION);
  const [recipientRoles, setRecipientRoles] = useState<ChecklistRecipientRole[]>(['MOTHER']);
  const [stage, setStage] = useState<ContentStage | ''>('');
  const [substage, setSubstage] = useState<ChecklistSubstage | null>(null);
  const [windowMode, setWindowMode] = useState<'SINGLE' | 'RANGE'>('RANGE');
  const [windowAnchor, setWindowAnchor] = useState<ChecklistSubstage['anchor']>('LMP');
  const [windowUnit, setWindowUnit] = useState<ChecklistSubstage['unit']>('WEEK');
  // These are source-facing values shown to the author; substage stores the
  // corresponding zero-based runtime offsets.
  const [windowStart, setWindowStart] = useState(1);
  const [windowEnd, setWindowEnd] = useState(20);
  const [windowEndAtStageExit, setWindowEndAtStageExit] = useState(false);
  const [scheduleType, setScheduleType] = useState<ChecklistScheduleType | null>(null);
  const [materializationPolicy, setMaterializationPolicy] = useState<ChecklistMaterializationPolicy | null>(null);
  const [scheduleGroupKey, setScheduleGroupKey] = useState<string | null>(null);
  const [scheduleContextType, setScheduleContextType] = useState<'JOURNEY' | 'BABY' | null>(null);
  const [scheduleEndMode, setScheduleEndMode] = useState<ChecklistScheduleEndMode | null>(null);
  const [weekBoundaryRule, setWeekBoundaryRule] = useState<ChecklistWeekBoundaryRule | null>(null);
  const [displayOrder, setDisplayOrder] = useState(1);
  const [items, setItems] = useState<ItemRow[]>([
    newRow(DEFAULT_CHECKLIST_CONTRACT_VERSION === 2),
  ]);
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
      const loadedContractVersion = data.checklistContractVersion === 1 || data.checklistContractVersion === 2
        ? data.checklistContractVersion
        : contractVersionForStage(data.stage ?? '');
      setChecklistContractVersion(loadedContractVersion);
      const loadedRoles = data.recipientRoles ?? ['MOTHER'];
      const hasMotherRecipient = loadedRoles.includes('MOTHER');
      setRecipientRoles(loadedRoles);
      setStage(hasMotherRecipient ? (data.stage ?? '') : '');
      setDisplayOrder(data.displayOrder ?? 1);
      const loadedSubstage = hasMotherRecipient && data.stage != null && data.stage !== 'PRE_PREGNANCY'
        ? (data.substage ?? defaultSubstage(data.stage as ContentStage))
        : null;
      setSubstage(loadedSubstage);
      if (loadedSubstage) {
        setWindowAnchor(loadedSubstage.anchor);
        setWindowUnit(loadedSubstage.unit);
        const loadedOpenEnded = loadedSubstage.endInclusive >= OPEN_ENDED_OFFSET;
        const loadedStart = loadedSubstage.unit === 'WEEK'
          ? loadedSubstage.startInclusive + 1
          : loadedSubstage.startInclusive;
        const loadedEnd = loadedSubstage.unit === 'WEEK'
          ? (loadedOpenEnded ? loadedStart : loadedSubstage.endInclusive + 1)
          : loadedSubstage.endInclusive;
        setWindowStart(loadedStart);
        setWindowEnd(loadedEnd);
        setWindowEndAtStageExit(loadedOpenEnded);
        setWindowMode(loadedOpenEnded || loadedStart !== loadedEnd ? 'RANGE' : 'SINGLE');
        setScheduleEndMode(data.scheduleEndMode ?? (loadedOpenEnded ? 'STAGE_EXIT' : null));
      } else {
        setWindowUnit('WEEK');
        setWindowEndAtStageExit(false);
      }
      setScheduleType(data.scheduleType ?? null);
      setMaterializationPolicy(data.materializationPolicy ?? null);
      setScheduleGroupKey(data.scheduleGroupKey ?? null);
      const derivedContext = data.stage === 'POSTPARTUM'
        ? 'JOURNEY'
        : data.stage === 'BABY_CARE'
          ? 'BABY'
          : data.scheduleContextType ?? null;
      setScheduleContextType(derivedContext);
      setScheduleEndMode(data.scheduleEndMode ?? null);
      setWeekBoundaryRule(data.weekBoundaryRule ?? null);
      setStatus(data.status);
      setVersionNo(data.versionNo);
      setReviewFeedback(data.latestReviewFeedback ?? null);
      setItems(data.items.length > 0
        ? [...data.items].sort((a, b) => a.order - b.order).map((item) => ({
          key: item.id,
          id: item.id,
          itemText: item.itemText,
          description: item.description ?? '',
          isRequired: item.isRequired ?? true,
          targetSubject: loadedContractVersion === 2 ? null : (item.targetSubject ?? 'MOTHER'),
          supportFunction: item.supportFunction ?? '',
          repeatWeekly: Boolean(item.repeatWeekly),
          repeatDaily: Boolean(item.repeatDaily),
        }))
        : [newRow(loadedContractVersion === 2)]);
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
  const isTargetlessV2 = checklistContractVersion === 2;
  const populatedItems = items.filter((row) => row.itemText.trim());
  const cadenceFlags = populatedItems;
  const hasWeeklyItems = cadenceFlags.some((row) => row.repeatWeekly);
  const hasDailyItems = cadenceFlags.some((row) => row.repeatDaily);

  const listRepeatWeekly = items.length > 0 && items.every((row) => row.repeatWeekly);
  const listRepeatDaily = items.length > 0 && items.every((row) => row.repeatDaily);

  const isSingleWeekWindow = stage !== '' && stage !== 'PRE_PREGNANCY'
    && windowUnit === 'WEEK'
    && (windowMode === 'SINGLE' || (!windowEndAtStageExit && windowStart === windowEnd));

  useEffect(() => {
    if (isSingleWeekWindow) {
      setItems((previous) =>
        previous.some((row) => row.repeatWeekly)
          ? previous.map((row) => ({ ...row, repeatWeekly: false }))
          : previous
      );
    }
  }, [isSingleWeekWindow]);

  const handleListWeeklyChange = (checked: boolean) => {
    setItems((previous) =>
      previous.map((row) => ({
        ...row,
        repeatWeekly: checked,
        repeatDaily: checked ? false : row.repeatDaily,
      }))
    );
  };

  const handleListDailyChange = (checked: boolean) => {
    setItems((previous) =>
      previous.map((row) => ({
        ...row,
        repeatDaily: checked,
        repeatWeekly: checked ? false : row.repeatWeekly,
      }))
    );
  };

  const hasUnsupportedPrePregnancyWeekly = stage === 'PRE_PREGNANCY' && hasWeeklyItems;
  const sequenceEligible = templateType === 'MANDATORY'
    && recipientRoles.length === 1
    && recipientRoles[0] === 'MOTHER'
    && stage === 'PRE_PREGNANCY';
  const isImmutable = status === 'APPROVED' || status === 'ARCHIVED';
  const isValid = name.trim().length > 0
    && recipientRoles.length > 0
    && (!hasMotherRecipient || (stage !== ''
      && (stage === 'PRE_PREGNANCY' || (substage !== null && substage.anchor !== 'NONE'))))
    && !hasUnsupportedPrePregnancyWeekly
    && populatedItems.every((row) => isTargetlessV2
      ? row.targetSubject == null && row.isRequired != null
      : row.targetSubject != null && row.isRequired != null)
    && (!sequenceEligible || (Number.isInteger(displayOrder) && displayOrder >= 1));



  const updateStage = (value: ContentStage | '') => {
    setStage(value);
    setScheduleContextType(
      value === 'POSTPARTUM' ? 'JOURNEY' : value === 'BABY_CARE' ? 'BABY' : null,
    );
    setScheduleType(null);
    setMaterializationPolicy(null);
    setScheduleEndMode(value ? 'FIXED_OFFSET' : null);
    const nextVersion = contractVersionForStage(value);
    setChecklistContractVersion(nextVersion);
    setSubstage(value ? defaultSubstage(value) : null);
    setWindowUnit('WEEK');
    setWindowEndAtStageExit(false);
    if (value === 'PREGNANCY') {
      setWindowAnchor('LMP'); setWindowStart(1); setWindowEnd(20); setWindowMode('RANGE');
    } else if (value === 'POSTPARTUM' || value === 'BABY_CARE') {
      setWindowAnchor(value === 'BABY_CARE' ? 'BIRTH_DATE' : 'DELIVERY_DATE'); setWindowStart(1); setWindowEnd(7); setWindowMode('RANGE');
    }
    setItems((previous) => previous.map((row) => nextVersion === 2
      ? {
        ...row,
        targetSubject: null,
        isRequired: row.isRequired ?? true,
        repeatWeekly: value === 'PRE_PREGNANCY' ? false : row.repeatWeekly,
      }
      : {
        ...row,
        targetSubject: value === 'BABY_CARE' ? 'BABY' : 'MOTHER',
        isRequired: row.isRequired ?? true,
        repeatWeekly: value === 'PRE_PREGNANCY' ? false : row.repeatWeekly,
      }));
  };

  const updateWindow = (
    nextAnchor = windowAnchor,
    nextStart = windowStart,
    nextEnd = windowMode === 'SINGLE' ? nextStart : windowEnd,
    nextOpenEnded = windowEndAtStageExit,
    nextUnit = windowUnit,
  ) => {
    if (stage === 'PRE_PREGNANCY' || stage === '') return;
    const minimum = nextUnit === 'WEEK' ? 1 : 0;
    const maximum = nextUnit === 'WEEK'
      ? (SOURCE_WEEK_OPTIONS.at(-1) ?? 52)
      : (SOURCE_DAY_OPTIONS.at(-1) ?? 52);
    const normalizedStart = Math.max(minimum, Math.min(nextStart, maximum));
    const normalizedEnd = Math.max(normalizedStart, Math.min(nextEnd, maximum));
    setWindowAnchor(nextAnchor);
    setWindowUnit(nextUnit);
    setWindowStart(normalizedStart);
    setWindowEnd(normalizedEnd);
    setWindowEndAtStageExit(nextOpenEnded);
    setSubstage(buildSubstage(stage, nextAnchor, normalizedStart, normalizedEnd, nextUnit, nextOpenEnded));
  };

  const updateWindowMode = (mode: 'SINGLE' | 'RANGE') => {
    setWindowMode(mode);
    if (mode === 'SINGLE') {
      setScheduleEndMode(stage ? 'FIXED_OFFSET' : null);
      updateWindow(windowAnchor, windowStart, windowStart, false);
    }
    else updateWindow(windowAnchor, windowStart, Math.max(windowEnd, windowStart), windowEndAtStageExit);
  };

  const updateWindowEnd = (value: string) => {
    if (value === 'STAGE_EXIT') {
      setScheduleEndMode('STAGE_EXIT');
      updateWindow(windowAnchor, windowStart, windowEnd, true);
      return;
    }
    setScheduleEndMode(stage ? 'FIXED_OFFSET' : null);
    updateWindow(windowAnchor, windowStart, Number(value), false);
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
        ...(isTargetlessV2 ? {} : { targetSubject: row.targetSubject }),
        ...(description ? { description } : {}),
        ...(row.supportFunction ? { supportFunction: row.supportFunction } : {}),
        repeatWeekly: row.repeatWeekly,
        repeatDaily: row.repeatDaily,
      };
    });

  const submit = async (targetStatus: 'DRAFT' | 'PENDING_REVIEW') => {
    if (!isValid || isImmutable) return;
    setSubmitting(targetStatus === 'PENDING_REVIEW' ? 'submit' : 'draft');
    setSubmitError('');
    const normalizedStage = hasMotherRecipient ? (stage || null) : null;
    const normalizedSubstage = hasMotherRecipient && stage !== 'PRE_PREGNANCY' ? substage : null;
    const derivedScheduleType = hasWeeklyItems
      ? 'WEEKLY'
      : hasDailyItems
        ? 'DAILY'
        : stage === 'PRE_PREGNANCY'
          ? 'SET'
          : stage
            ? (scheduleType ?? 'WEEKLY')
            : scheduleType;
    const derivedMaterializationPolicy = hasWeeklyItems
      ? 'EACH_WEEK'
      : hasDailyItems
        ? 'EACH_DAY'
        : stage === 'PRE_PREGNANCY'
          ? 'SEQUENCE_STEP'
          : stage
            ? (materializationPolicy ?? 'ONCE_PER_WINDOW')
            : materializationPolicy;
    try {
      const commonPayload = {
        name: name.trim(),
        description: description.trim() || undefined,
        templateType,
        checklistContractVersion,
        recipientRoles,
        stage: normalizedStage,
        substage: normalizedSubstage,
        displayOrder: sequenceEligible ? displayOrder : 0,
        scheduleType: derivedScheduleType,
        materializationPolicy: derivedMaterializationPolicy,
        scheduleGroupKey,
        scheduleContextType,
        scheduleEndMode: windowEndAtStageExit ? 'STAGE_EXIT' : (scheduleEndMode ?? (stage ? 'FIXED_OFFSET' : null)),
        weekBoundaryRule: weekBoundaryRule ?? (hasWeeklyItems ? 'ANCHOR_RELATIVE_7D' : 'NONE'),
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
          <p className="mt-1 text-sm text-on-surface-variant">
            {isTargetlessV2
              ? 'Checklist V2 lưu nội dung khuyến nghị; mỗi mục vẫn có thể đánh dấu bắt buộc.'
              : 'Thiết lập người nhận, giai đoạn và nhịp lặp cho checklist.'}
          </p>
          {stage && (
            <span role="status" className="mt-2 inline-flex rounded-full bg-surface-container-low px-3 py-1 text-xs font-semibold text-primary">
              Phiên bản nội dung V{checklistContractVersion} · {isTargetlessV2 ? 'Khuyến nghị' : 'Tương thích'}
            </span>
          )}
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
                <fieldset className="mt-4 grid gap-3 border-0 p-0">
                  <legend className="text-sm font-semibold text-on-surface">Cửa sổ vòng đời</legend>
                  <div className="grid gap-3 sm:grid-cols-3">
                    <label className="grid gap-2 font-normal text-on-surface-variant">Kiểu cửa sổ
                      <select aria-label="Lifecycle window mode" disabled={isImmutable} value={windowMode} onChange={(event) => updateWindowMode(event.target.value as 'SINGLE' | 'RANGE')} className={field}><option value="SINGLE">Một {windowUnit === 'DAY' ? 'ngày' : 'tuần'}</option><option value="RANGE">Khoảng {windowUnit === 'DAY' ? 'ngày' : 'tuần'}</option></select>
                    </label>
                    <label className="grid gap-2 font-normal text-on-surface-variant">Từ {windowUnit === 'DAY' ? 'ngày' : 'tuần'}
                      <select aria-label="Lifecycle window start" disabled={isImmutable} value={windowStart} onChange={(event) => updateWindow(windowAnchor, Number(event.target.value), windowEnd)} className={field}>
                        {(windowUnit === 'DAY' ? SOURCE_DAY_OPTIONS : SOURCE_WEEK_OPTIONS).map((value) => <option key={value} value={value}>{value}</option>)}
                      </select>
                    </label>
                    {windowMode === 'RANGE' && (
                      <label className="grid gap-2 font-normal text-on-surface-variant">Đến {windowUnit === 'DAY' ? 'ngày' : 'tuần'}
                        <select aria-label="Lifecycle window end" disabled={isImmutable} value={windowEndAtStageExit ? 'STAGE_EXIT' : windowEnd} onChange={(event) => updateWindowEnd(event.target.value)} className={field}>
                          {(windowUnit === 'DAY' ? SOURCE_DAY_OPTIONS : SOURCE_WEEK_OPTIONS).filter((value) => value >= windowStart).map((value) => <option key={value} value={value}>{value}</option>)}
                          {stage === 'PREGNANCY' && windowUnit === 'WEEK' && <option value="STAGE_EXIT">Đến khi kết thúc thai kỳ</option>}
                        </select>
                      </label>
                    )}
                  </div>
                </fieldset>
              )}
              {hasUnsupportedPrePregnancyWeekly && <div role="alert" className="mt-4 rounded-xl border border-error-container bg-error-container/60 p-3 text-xs font-normal text-error">Giai đoạn Chuẩn bị mang thai dùng theo bộ; chỉ có thể chọn “Từng ngày” cho mục lặp.</div>}
              {sequenceEligible && (
                <label className="mt-4 grid gap-2 text-sm font-semibold text-on-surface">
                  Vị trí bộ checklist
                  <input
                    aria-label="Checklist sequence position"
                    type="number"
                    min={1}
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
                </label>
              )}
            </section>
          )}

          <section className={card}>
            <div className="mb-5 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2.5"><ClipboardList className="text-primary" size={22} /><h2 className="m-0 text-lg font-bold text-on-surface">Danh sách mục</h2></div>
              <button type="button" disabled={isImmutable} onClick={() => setItems((previous) => [...previous, newRow(isTargetlessV2, listRepeatWeekly, listRepeatDaily)])} className="inline-flex items-center gap-1.5 py-2 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-primary hover:bg-surface-container-low cursor-pointer disabled:opacity-40"><Plus size={16} /> Thêm mục</button>
            </div>
            <div className="mb-5 flex flex-wrap items-center gap-4 rounded-xl border border-surface-container-highest bg-surface-container-low p-4">
              <span className="text-sm font-semibold text-on-surface">Nhịp lặp của danh sách mục</span>
              <div className="flex flex-wrap gap-4">
                <label className="flex items-center gap-2 text-sm font-semibold text-on-surface-variant cursor-pointer">
                  <input
                    aria-label="List weekly recurrence"
                    type="checkbox"
                    disabled={isImmutable || stage === 'PRE_PREGNANCY' || isSingleWeekWindow}
                    checked={listRepeatWeekly}
                    onChange={(event) => handleListWeeklyChange(event.target.checked)}
                    className="h-4 w-4 accent-primary"
                  />
                  Từng tuần
                </label>
                <label className="flex items-center gap-2 text-sm font-semibold text-on-surface-variant cursor-pointer">
                  <input
                    aria-label="List daily recurrence"
                    type="checkbox"
                    disabled={isImmutable}
                    checked={listRepeatDaily}
                    onChange={(event) => handleListDailyChange(event.target.checked)}
                    className="h-4 w-4 accent-primary"
                  />
                  Từng ngày
                </label>
              </div>
            </div>
            <div className="grid gap-4">
              {items.map((row, index) => (
                <div key={row.key} className="grid gap-3 rounded-2xl border border-surface-container-highest bg-surface-bright p-4 md:grid-cols-[minmax(0,1fr)_auto] md:items-end">
                  <label className="grid gap-2 text-sm font-semibold text-on-surface">Mục {index + 1}<input aria-label={`Item ${index + 1} text`} disabled={isImmutable} value={row.itemText} onChange={(event) => updateItem(row.key, { itemText: event.target.value })} className={field} /></label>
                  <button aria-label={`Delete item ${index + 1}`} type="button" disabled={isImmutable || items.length === 1} onClick={() => setItems((previous) => previous.filter((item) => item.key !== row.key))} className="flex h-10 w-10 items-center justify-center rounded-xl border border-error-container text-error hover:bg-error-container/20 cursor-pointer disabled:opacity-30 self-end mb-0.5"><Trash2 size={18} /></button>
                  {!isTargetlessV2 && (
                    <label className="grid gap-2 text-sm font-semibold text-on-surface md:col-span-2">Đối tượng
                      <select
                        aria-label={`Đối tượng mục ${index + 1}`}
                        disabled={isImmutable || stage === 'POSTPARTUM' || stage === 'BABY_CARE'}
                        value={stage === 'BABY_CARE' ? 'BABY' : row.targetSubject ?? 'MOTHER'}
                        onChange={(event) => updateItem(row.key, { targetSubject: event.target.value as ChecklistTargetSubject })}
                        className={field}
                      >
                        <option value="MOTHER">Mẹ</option>
                        <option value="BABY">Bé</option>
                      </select>
                    </label>
                  )}
                  <label className="grid gap-2 text-sm font-semibold text-on-surface md:col-span-2">Nội dung chi tiết
                    <textarea aria-label={`Nội dung chi tiết mục ${index + 1}`} disabled={isImmutable} value={row.description} onChange={(event) => updateItem(row.key, { description: event.target.value })} rows={3} className={`${field} py-3`} />
                  </label>
                  <label className="grid gap-2 text-sm font-semibold text-on-surface md:col-span-2">Chức năng hỗ trợ
                    <select aria-label={`Chức năng hỗ trợ mục ${index + 1}`} disabled={isImmutable} value={row.supportFunction} onChange={(event) => updateItem(row.key, { supportFunction: event.target.value as ChecklistSupportFunction | '' })} className={field}>
                      <option value="">Không liên kết</option>
                      {CHECKLIST_SUPPORT_FUNCTION_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                    </select>
                  </label>
                  <label className="flex items-center gap-2 text-sm font-semibold text-on-surface-variant md:col-span-2"><input type="checkbox" disabled={isImmutable} checked={Boolean(row.isRequired)} onChange={(event) => updateItem(row.key, { isRequired: event.target.checked })} className="h-4 w-4 accent-primary" /> Bắt buộc</label>
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
