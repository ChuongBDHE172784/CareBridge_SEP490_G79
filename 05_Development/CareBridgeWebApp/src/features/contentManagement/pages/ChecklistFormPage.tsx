import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  fetchChecklistTemplateDetail,
  createChecklistTemplate,
  updateChecklistTemplate,
} from '../services/contentApi';
import type { ContentStage, ReviewFeedback } from '../models/content';
import { STAGE_LABELS } from '../models/content';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';

interface ItemRow {
  key: string;
  itemText: string;
  isRequired: boolean;
}

function newRow(): ItemRow {
  return { key: crypto.randomUUID(), itemText: '', isRequired: true };
}

export default function ChecklistFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [stage, setStage] = useState<ContentStage | ''>('');
  const [items, setItems] = useState<ItemRow[]>([newRow()]);
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
      setStage(data.stage);
      setVersionNo(data.versionNo);
      setReviewFeedback(data.latestReviewFeedback ?? null);
      setItems(
        data.items.length > 0
          ? [...data.items].sort((a, b) => a.order - b.order).map(i => ({
              key: i.id,
              itemText: i.itemText,
              isRequired: i.isRequired,
            }))
          : [newRow()],
      );
    } catch {
      setLoadError('Không thể tải checklist để chỉnh sửa. Vui lòng thử lại hoặc kiểm tra quyền Content Admin.');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => { if (isEdit) loadDetail(); }, [isEdit, loadDetail]);

  const isValid = name.trim().length > 0 && stage !== '';

  const updateItem = (key: string, patch: Partial<ItemRow>) => {
    setItems(prev => prev.map(row => (row.key === key ? { ...row, ...patch } : row)));
  };

  const removeItem = (key: string) => {
    setItems(prev => (prev.length > 1 ? prev.filter(row => row.key !== key) : prev));
  };

  const addItem = () => setItems(prev => [...prev, newRow()]);

  const buildItemsPayload = () =>
    items
      .filter(row => row.itemText.trim().length > 0)
      .map((row, index) => ({ itemText: row.itemText.trim(), order: index + 1, isRequired: row.isRequired }));

  const submit = async (targetStatus: 'DRAFT' | 'PENDING_REVIEW') => {
    if (!isValid) return;
    setSubmitting(targetStatus === 'PENDING_REVIEW' ? 'submit' : 'draft');
    setSubmitError('');
    try {
      const itemsPayload = buildItemsPayload();
      if (isEdit && id) {
        await updateChecklistTemplate(id, {
          name: name.trim(),
          description: description.trim() || undefined,
          stage,
          status: targetStatus,
          items: itemsPayload,
        });
        navigate(`/content/checklists/${id}`);
      } else {
        const created = await createChecklistTemplate({
          name: name.trim(),
          description: description.trim() || undefined,
          stage,
          items: itemsPayload,
        });
        if (targetStatus === 'PENDING_REVIEW') {
          await updateChecklistTemplate(created.id, {
            name: created.name,
            description: created.description,
            stage: created.stage,
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

  if (isLoading) {
    return <div className="p-8 font-sans text-center text-outline py-16">Đang tải...</div>;
  }

  if (loadError) {
    return (
      <div className="p-8 font-sans max-w-[700px]">
        <div className="bg-error-container rounded-2xl p-6 text-error text-sm mb-4">{loadError}</div>
        <button
          onClick={() => navigate('/content/checklists')}
          className="py-2.5 px-6 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer"
        >
          Quay lại danh sách
        </button>
      </div>
    );
  }

  return (
    <div className="p-8 font-sans max-w-[1200px]">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer mb-6"
      >
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Quay lại
      </button>

      <h1 className="text-[26px] font-bold text-on-surface m-0">{isEdit ? 'Chỉnh sửa Checklist' : 'Tạo Checklist mới'}</h1>
      <p className="text-on-surface-variant text-sm mt-1 mb-6">
        Xây dựng danh sách các mục để mẹ/gia đình nhập vào checklist cá nhân.
      </p>

      <div className={isEdit ? 'grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]' : 'max-w-[900px]'}>
        <div>
          <ReviewFeedbackNotice feedback={reviewFeedback} />

      <div className="bg-surface rounded-2xl p-6 shadow-md mb-6">
        <div className="mb-5">
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Tên checklist <span className="text-error">*</span>
          </label>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="VD: Checklist khám thai tháng 3"
            className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
          />
        </div>

        <div className="mb-5">
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Giai đoạn <span className="text-error">*</span>
          </label>
          <select
            value={stage}
            onChange={(e) => setStage(e.target.value as ContentStage)}
            className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
          >
            <option value="">Chọn giai đoạn</option>
            {Object.entries(STAGE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </div>

        <div>
          <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
            Mô tả
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Mô tả ngắn về mục đích của checklist..."
            rows={2}
            className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans resize-none"
          />
        </div>
      </div>

      <div className="bg-surface rounded-2xl p-6 shadow-md mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-on-surface m-0">Danh sách mục</h2>
          <button
            onClick={addItem}
            className="flex items-center gap-1 py-2 px-4 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer"
          >
            <span className="material-symbols-outlined text-lg">add</span>
            Thêm mục
          </button>
        </div>
        <div className="flex flex-col gap-3">
          {items.map((row, index) => (
            <div key={row.key} className="flex items-center gap-3 py-2 px-3 rounded-xl bg-surface-container-lowest">
              <span className="text-xs text-outline w-6 text-center">{index + 1}</span>
              <input
                value={row.itemText}
                onChange={(e) => updateItem(row.key, { itemText: e.target.value })}
                placeholder="Nội dung mục..."
                className="flex-1 py-2.5 px-3 rounded-xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
              />
              <label className="flex items-center gap-1.5 text-xs text-outline whitespace-nowrap">
                <input
                  type="checkbox"
                  checked={row.isRequired}
                  onChange={(e) => updateItem(row.key, { isRequired: e.target.checked })}
                />
                Bắt buộc
              </label>
              <button
                onClick={() => removeItem(row.key)}
                disabled={items.length === 1}
                title="Xóa mục"
                className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center disabled:opacity-30 disabled:cursor-not-allowed"
              >
                <span className="material-symbols-outlined text-error text-base">delete</span>
              </button>
            </div>
          ))}
        </div>
        <p className="text-[11px] text-outline mt-3">Có thể để trống danh sách mục và bổ sung sau (checklist bản nháp).</p>
      </div>

      {submitError && <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{submitError}</div>}

      <div className="flex items-center justify-end sticky bottom-0 bg-background py-4">
        <div className="flex gap-3">
          <button
            onClick={() => submit('DRAFT')}
            disabled={!isValid || submitting !== null}
            className="py-3 px-6 rounded-full border border-outline-variant bg-surface text-on-surface-variant text-sm font-semibold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {submitting === 'draft' ? 'Đang lưu...' : 'Lưu nháp'}
          </button>
          <button
            onClick={() => submit('PENDING_REVIEW')}
            disabled={!isValid || submitting !== null}
            className="py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {submitting === 'submit' ? 'Đang gửi...' : 'Gửi phê duyệt'}
          </button>
        </div>
      </div>
        </div>

        {isEdit && versionNo !== null && id && (
          <aside className="h-fit bg-surface rounded-2xl p-5 shadow-md">
            <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">Lịch sử phiên bản</p>
            <p className="text-sm text-on-surface mb-1">Phiên bản hiện tại: v{versionNo}</p>
            <button
              type="button"
              onClick={() => navigate(`/content/checklists/${id}/versions`)}
              className="text-sm text-primary font-semibold cursor-pointer border-0 bg-transparent p-0"
            >
              Xem toàn bộ lịch sử
            </button>
          </aside>
        )}
      </div>
    </div>
  );
}
