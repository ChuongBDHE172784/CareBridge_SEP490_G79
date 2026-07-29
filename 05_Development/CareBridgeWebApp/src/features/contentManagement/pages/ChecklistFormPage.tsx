import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  fetchChecklistTemplateDetail,
  createChecklistTemplate,
  updateChecklistTemplate,
} from '../services/contentApi';
import type { ContentStage, ReviewFeedback } from '../models/content';
import { STAGE_OPTIONS } from '../models/content';
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
    <div className="p-8 font-sans">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer mb-6"
      >
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Quay lại
      </button>

      <div className="flex items-center justify-between mb-6">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-on-surface m-0">
              {isEdit ? 'Chỉnh sửa Checklist' : 'Tạo Checklist mới'}
            </h1>
            {isEdit && versionNo !== null && (
              <span className="py-1 px-3 rounded-full bg-surface-container text-primary text-xs font-semibold">
                Version v{versionNo}
              </span>
            )}
          </div>
          <p className="text-sm text-outline mt-1">
            {isEdit && id
              ? `Cập nhật checklist ID: ${id.slice(0, 8).toUpperCase()}`
              : 'Xây dựng danh sách các mục để mẹ/gia đình nhập vào checklist cá nhân.'}
          </p>
        </div>
        <div className="flex gap-2.5">
          <button
            onClick={() => submit('DRAFT')}
            disabled={!isValid || submitting !== null}
            className="flex items-center gap-1.5 py-2.5 px-5 rounded-full bg-surface-container text-primary border-0 text-sm font-semibold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <span className="material-symbols-outlined text-lg">save</span>
            {submitting === 'draft' ? 'Đang lưu...' : 'Lưu nháp'}
          </button>
          <button
            onClick={() => submit('PENDING_REVIEW')}
            disabled={!isValid || submitting !== null}
            className="flex items-center gap-1.5 py-2.5 px-5 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <span className="material-symbols-outlined text-lg">play_arrow</span>
            {submitting === 'submit' ? 'Đang gửi...' : 'Gửi phê duyệt'}
          </button>
        </div>
      </div>

      {submitError && <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{submitError}</div>}
      <ReviewFeedbackNotice feedback={reviewFeedback} />

      <div className="grid grid-cols-[1fr_320px] gap-6">
        <div className="flex flex-col gap-5">
          <div className="bg-surface rounded-2xl p-6 shadow-md">
            <div className="flex items-center gap-2 mb-4">
              <span className="material-symbols-outlined text-primary text-xl">description</span>
              <h2 className="text-base font-bold text-on-surface m-0">Thông tin cơ bản</h2>
            </div>
            <div className="mb-4">
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

            <div className="mb-4">
              <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
                Giai đoạn <span className="text-error">*</span>
              </label>
              <select
                value={stage}
                onChange={(e) => setStage(e.target.value as ContentStage)}
                className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
              >
                <option value="">Chọn giai đoạn</option>
                {STAGE_OPTIONS.map(({ value, label }) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
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

          <div className="bg-surface rounded-2xl p-6 shadow-md">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">checklist</span>
                <h2 className="text-base font-bold text-on-surface m-0">Danh sách các mục</h2>
              </div>
              <button
                type="button"
                onClick={addItem}
                className="flex items-center gap-1 py-2 px-4 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer hover:bg-surface-container"
              >
                <span className="material-symbols-outlined text-lg">add</span>
                Thêm mục
              </button>
            </div>
            <div className="flex flex-col gap-3">
              {items.map((row, index) => (
                <div key={row.key} className="flex items-center gap-3 py-2 px-3 rounded-xl bg-surface-container-lowest border border-outline-variant/40">
                  <span className="text-xs font-semibold text-outline w-6 text-center">{index + 1}</span>
                  <input
                    value={row.itemText}
                    onChange={(e) => updateItem(row.key, { itemText: e.target.value })}
                    placeholder="Nội dung mục..."
                    className="flex-1 py-2.5 px-3 rounded-xl border border-outline-variant bg-surface text-sm text-on-surface font-sans"
                  />
                  <label className="flex items-center gap-1.5 text-xs text-outline whitespace-nowrap cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={row.isRequired}
                      onChange={(e) => updateItem(row.key, { isRequired: e.target.checked })}
                      className="rounded accent-primary cursor-pointer"
                    />
                    Bắt buộc
                  </label>
                  <button
                    type="button"
                    onClick={() => removeItem(row.key)}
                    disabled={items.length === 1}
                    title="Xóa mục"
                    className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center text-error disabled:opacity-30 disabled:cursor-not-allowed hover:bg-error-container/20"
                  >
                    <span className="material-symbols-outlined text-base">delete</span>
                  </button>
                </div>
              ))}
            </div>
            <p className="text-[11px] text-outline mt-3">Có thể để trống danh sách mục và bổ sung sau (checklist bản nháp).</p>
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="bg-surface rounded-2xl p-5 shadow-md">
            <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">Tổng quan mục</p>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-on-surface">Tổng số mục</span>
              <span className="text-sm font-semibold text-primary">{items.filter(i => i.itemText.trim()).length}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-on-surface">Mục bắt buộc</span>
              <span className="text-sm font-semibold text-primary">{items.filter(i => i.itemText.trim() && i.isRequired).length}</span>
            </div>
          </div>

          <div className="bg-surface rounded-2xl p-5 shadow-md">
            <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">Lịch sử phiên bản</p>
            {isEdit && versionNo !== null ? (
              <>
                <p className="text-sm text-on-surface mb-1">Phiên bản hiện tại: v{versionNo}</p>
                <button
                  type="button"
                  onClick={() => navigate(`/content/checklists/${id}/versions`)}
                  className="text-sm text-primary font-semibold cursor-pointer border-0 bg-transparent p-0 hover:underline"
                >
                  Xem toàn bộ lịch sử
                </button>
              </>
            ) : (
              <p className="text-sm text-outline">Chưa có lịch sử (Phiên bản mới)</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
