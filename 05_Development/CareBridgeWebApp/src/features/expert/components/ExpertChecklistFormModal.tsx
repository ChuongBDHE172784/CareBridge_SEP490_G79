import React, { useState } from 'react';
import {
  CalendarRange,
  ClipboardList,
  Plus,
  Save,
  Trash2,
  X,
} from 'lucide-react';
import {
  type ChecklistShareData,
  type ChecklistItemShareData,
  savePersonalizedChecklist,
} from '../services/expertSharedRecordsService';
import {
  CHECKLIST_SUPPORT_FUNCTION_OPTIONS,
  STAGE_LABELS,
  STAGE_OPTIONS,
  type ChecklistSupportFunction,
  type ContentStage,
} from '../../contentManagement/models/content';

export interface ExpertChecklistFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (updatedChecklist: ChecklistShareData) => void;
  mode: 'ADD' | 'EDIT';
  motherName: string;
  conversationId: string;
  checklistData: ChecklistShareData;
  initialItemIndex?: number;
  initialTargetGroup?: 'CURRENT' | 'FUTURE' | 'HISTORY';
  initialItem?: ChecklistItemShareData;
}

interface ItemRow {
  key: string;
  itemText: string;
  description: string;
  sourceUrl: string;
  isRequired: boolean;
  category: string;
  supportFunction: ChecklistSupportFunction | '';
  repeatWeekly: boolean;
  repeatDaily: boolean;
  completed: boolean;
}

const AUTHORABLE_STAGES: readonly ContentStage[] = STAGE_OPTIONS.map(({ value }) => value);
const SOURCE_WEEK_OPTIONS = Array.from({ length: 42 }, (_, index) => index + 1);

let fallbackSequence = 0;
function newRow(initialText = '', initialCategory = 'Khám thai & Y tế', completed = false): ItemRow {
  return {
    key: `item-${Date.now()}-${fallbackSequence++}`,
    itemText: initialText,
    description: '',
    sourceUrl: '',
    isRequired: true,
    category: initialCategory,
    supportFunction: '',
    repeatWeekly: false,
    repeatDaily: false,
    completed,
  };
}

export const ExpertChecklistFormModal: React.FC<ExpertChecklistFormModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  mode,
  motherName,
  conversationId,
  checklistData,
  initialItemIndex,
  initialTargetGroup = 'CURRENT',
  initialItem,
}) => {
  // Basic info
  const [title, setTitle] = useState(
    mode === 'EDIT' && initialItem
      ? initialItem.text
      : checklistData.title || 'Checklist theo dõi & chăm sóc thai kỳ'
  );
  const [description, setDescription] = useState(checklistData.note || '');
  const [doctorNote, setDoctorNote] = useState(initialItem?.doctorNote || '');

  // Lifecycle & timing
  const [targetGroup, setTargetGroup] = useState<'CURRENT' | 'FUTURE' | 'HISTORY'>(initialTargetGroup);
  const [stage, setStage] = useState<ContentStage>('PREGNANCY');
  const [windowMode, setWindowMode] = useState<'SINGLE' | 'RANGE'>('SINGLE');
  const [windowStart, setWindowStart] = useState<number>(checklistData.gestationalWeek || 12);
  const [windowEnd, setWindowEnd] = useState<number>(checklistData.gestationalWeek || 16);

  // Items list
  const [items, setItems] = useState<ItemRow[]>(() => {
    if (mode === 'EDIT' && initialItem) {
      return [
        {
          key: `edit-row-0`,
          itemText: initialItem.text,
          description: '',
          sourceUrl: initialItem.sourceUrl || '',
          isRequired: true,
          category: initialItem.category || 'Khám thai & Y tế',
          supportFunction: (initialItem.supportFunction as ChecklistSupportFunction) || '',
          repeatWeekly: false,
          repeatDaily: false,
          completed: initialItem.completed,
        },
      ];
    }
    return [newRow('', 'Khám thai & Y tế', false)];
  });

  const [listRepeatWeekly, setListRepeatWeekly] = useState(false);
  const [listRepeatDaily, setListRepeatDaily] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  if (!isOpen) return null;

  const card = 'bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest';
  const field =
    'w-full py-2.5 px-3.5 rounded-xl border border-outline-variant bg-surface text-xs text-on-surface outline-none font-sans focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-surface-container-low disabled:text-outline';

  const handleListWeeklyChange = (checked: boolean) => {
    setListRepeatWeekly(checked);
    if (checked) setListRepeatDaily(false);
    setItems((prev) => prev.map((r) => ({ ...r, repeatWeekly: checked, repeatDaily: false })));
  };

  const handleListDailyChange = (checked: boolean) => {
    setListRepeatDaily(checked);
    if (checked) setListRepeatWeekly(false);
    setItems((prev) => prev.map((r) => ({ ...r, repeatDaily: checked, repeatWeekly: false })));
  };

  const updateItem = (key: string, patch: Partial<ItemRow>) => {
    setItems((prev) => prev.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validItems = items.filter((i) => i.itemText.trim().length > 0);
    if (validItems.length === 0) {
      setErrorMsg('Vui lòng nhập ít nhất 1 mục việc cần làm');
      return;
    }

    setSubmitting(true);
    setErrorMsg('');

    try {
      const currentList = [...(checklistData.currentItems || checklistData.items || [])];
      const historyList = [...(checklistData.historyItems || [])];
      const futureList = [...(checklistData.futureItems || [])];

      const timeLabel =
        targetGroup === 'CURRENT'
          ? `Tuần ${windowStart}`
          : targetGroup === 'FUTURE'
          ? `Tuần ${windowStart}${windowMode === 'RANGE' ? `-${windowEnd}` : ''}`
          : 'Đã qua';

      if (mode === 'EDIT') {
        const updatedRow = validItems[0];
        const newItem: ChecklistItemShareData = {
          text: updatedRow.itemText.trim(),
          completed: updatedRow.completed,
          category: 'Khám thai & Y tế',
          timeLabel,
          origin: 'EXPERT',
          createdBy: 'EXPERT',
          isExpertCustom: true,
          doctorNote: doctorNote.trim() || undefined,
          sourceUrl: updatedRow.sourceUrl.trim() || undefined,
          supportFunction: updatedRow.supportFunction || undefined,
        };

        const targetList =
          targetGroup === 'CURRENT'
            ? currentList
            : targetGroup === 'FUTURE'
            ? futureList
            : historyList;

        let targetIdx = -1;
        if (initialItem?.text) {
          targetIdx = targetList.findIndex(
            (item) => item.text.trim().toLowerCase() === initialItem.text.trim().toLowerCase()
          );
        }
        if (
          targetIdx < 0 &&
          initialItemIndex !== undefined &&
          initialItemIndex >= 0 &&
          initialItemIndex < targetList.length
        ) {
          targetIdx = initialItemIndex;
        }

        if (targetIdx >= 0 && targetIdx < targetList.length) {
          targetList[targetIdx] = newItem;
        } else {
          targetList.push(newItem);
        }
      } else {
        // ADD mode: Add all valid items
        for (const row of validItems) {
          const newItem: ChecklistItemShareData = {
            text: row.itemText.trim(),
            completed: row.completed,
            category: 'Khám thai & Y tế',
            timeLabel,
            origin: 'EXPERT',
            createdBy: 'EXPERT',
            isExpertCustom: true,
            doctorNote: doctorNote.trim() || undefined,
            sourceUrl: row.sourceUrl.trim() || undefined,
            supportFunction: row.supportFunction || undefined,
          };

          if (targetGroup === 'CURRENT') {
            currentList.push(newItem);
          } else if (targetGroup === 'FUTURE') {
            futureList.push(newItem);
          } else {
            historyList.push(newItem);
          }
        }
      }

      const allItems = [...currentList, ...historyList, ...futureList];
      const completedCount = allItems.filter((i) => i.completed).length;
      const totalCount = allItems.length;
      const progressPercent = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

      const actionNote = doctorNote.trim()
        ? `[Bác sĩ chỉ định]: ${doctorNote.trim()}`
        : checklistData.note;

      const updatedPayload: ChecklistShareData = {
        ...checklistData,
        title: title.trim() || checklistData.title,
        currentItems: currentList,
        historyItems: historyList,
        futureItems: futureList,
        items: currentList,
        completedCount,
        totalCount,
        progressPercent,
        isLiveSync: true,
        note: actionNote,
      };

      const saved = await savePersonalizedChecklist(conversationId, updatedPayload, actionNote);
      onSuccess(saved);
      onClose();
    } catch (err) {
      console.error('Failed to submit checklist form', err);
      setErrorMsg('Không thể lưu checklist. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[80] bg-black/60 backdrop-blur-xs flex items-center justify-center p-4"
      style={{ zIndex: 80 }}
    >
      <div className="bg-surface rounded-2xl border border-outline-variant shadow-2xl max-w-4xl w-full max-h-[92vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Modal Header */}
        <div className="p-5 border-b border-outline-variant/50 flex items-center justify-between bg-surface-container-low/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shadow-xs">
              <ClipboardList size={22} />
            </div>
            <div>
              <h2 className="text-base font-bold text-on-surface m-0">
                {mode === 'EDIT' ? 'Chỉnh sửa Việc cần làm' : 'Thêm việc chỉ định y tế'} cho {motherName}
              </h2>
              <p className="text-xs text-on-surface-variant m-0 mt-0.5">
                Các chỉ định y tế của bác sĩ được xếp vào lộ trình chuẩn CareBridge và hiển thị nổi bật cho mẹ bầu
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-surface-container-high flex items-center justify-center text-on-surface-variant cursor-pointer transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
          {errorMsg && (
            <div className="rounded-xl border border-error-container bg-error-container/60 p-3.5 text-xs text-error font-medium">
              {errorMsg}
            </div>
          )}

          {mode === 'EDIT' ? (
            /* --- Chế độ EDIT: Form gọn gàng chỉnh sửa trực tiếp mục việc cần làm --- */
            <section className={card}>
              <div className="mb-4 flex items-center gap-2">
                <ClipboardList className="text-primary" size={18} />
                <h3 className="m-0 text-sm font-bold text-on-surface">Thông tin việc cần làm</h3>
              </div>

              <div className="grid gap-4">
                <div>
                  <label className="block text-xs font-semibold text-on-surface mb-1.5">
                    Nội dung việc cần làm / Chỉ định y tế <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={items[0]?.itemText || ''}
                    onChange={(e) => updateItem(items[0]?.key || 'edit-row-0', { itemText: e.target.value })}
                    placeholder="Ví dụ: Đo huyết áp 2 lần/ngày, Ăn 7kg thịt/tuần..."
                    className={field}
                    required
                  />
                </div>

                <div className="grid sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-on-surface mb-1.5">
                      Lời dặn / Ghi chú của Bác sĩ chuyên khoa
                    </label>
                    <textarea
                      rows={2}
                      value={doctorNote}
                      onChange={(e) => setDoctorNote(e.target.value)}
                      placeholder="Lời dặn riêng cho mẹ bầu..."
                      className={`${field} resize-none`}
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-on-surface mb-1.5">
                      Link nguồn / Tài liệu tham khảo y khoa
                    </label>
                    <input
                      type="url"
                      value={items[0]?.sourceUrl || ''}
                      onChange={(e) => updateItem(items[0]?.key || 'edit-row-0', { sourceUrl: e.target.value })}
                      placeholder="https://... bài viết hướng dẫn y tế"
                      className={field}
                    />
                  </div>
                </div>

                <div className="grid sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-on-surface mb-1.5">
                      Chức năng hỗ trợ liên kết
                    </label>
                    <select
                      value={items[0]?.supportFunction || ''}
                      onChange={(e) =>
                        updateItem(items[0]?.key || 'edit-row-0', {
                          supportFunction: (e.target.value as ChecklistSupportFunction) || '',
                        })
                      }
                      className={field}
                    >
                      <option value="">🚫 Không liên kết</option>
                      {CHECKLIST_SUPPORT_FUNCTION_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-on-surface mb-1.5">
                      Tuần thai áp dụng
                    </label>
                    <select
                      value={windowStart}
                      onChange={(e) => setWindowStart(Number(e.target.value))}
                      className={field}
                    >
                      {SOURCE_WEEK_OPTIONS.map((w) => (
                        <option key={w} value={w}>
                          Tuần {w}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            </section>
          ) : (
            /* --- Chế độ ADD: Thêm mới chỉ định --- */
            <>
              {/* Section 1: Thông tin cơ bản */}
              <section className={card}>
                <div className="mb-4 flex items-center gap-2">
                  <ClipboardList className="text-primary" size={18} />
                  <h3 className="m-0 text-sm font-bold text-on-surface">Thông tin cơ bản</h3>
                </div>
                <div className="grid gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-on-surface mb-1.5">
                      Tên checklist / Lộ trình <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      placeholder="Ví dụ: Checklist theo dõi & chỉ định y tế..."
                      className={field}
                      required
                    />
                  </div>
                  <div className="grid sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-on-surface mb-1.5">
                        Mô tả hướng dẫn cho mẹ
                      </label>
                      <textarea
                        rows={2}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="Hướng dẫn chung cách thực hiện các việc trong danh sách..."
                        className={`${field} resize-none`}
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-on-surface mb-1.5">
                        Lời dặn / Ghi chú của Bác sĩ chuyên khoa
                      </label>
                      <textarea
                        rows={2}
                        value={doctorNote}
                        onChange={(e) => setDoctorNote(e.target.value)}
                        placeholder="Lời dặn riêng cho mẹ (vd: Chú ý đo huyết áp đều đặn...)"
                        className={`${field} resize-none`}
                      />
                    </div>
                  </div>
                </div>
              </section>

              {/* Section 2: Giai đoạn & Thời điểm áp dụng */}
              <section className={card}>
                <div className="mb-4 flex items-center gap-2">
                  <CalendarRange className="text-primary" size={18} />
              <h3 className="m-0 text-sm font-bold text-on-surface">Giai đoạn & Thời điểm áp dụng</h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Target Group */}
              <div>
                <label className="block text-xs font-semibold text-on-surface mb-1.5">
                  Giai đoạn phân loại cho mẹ
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {(
                    [
                      { id: 'CURRENT', label: 'Tuần hiện tại' },
                      { id: 'FUTURE', label: 'Lộ trình tới' },
                      { id: 'HISTORY', label: 'Lịch sử qua' },
                    ] as const
                  ).map((grp) => (
                    <button
                      key={grp.id}
                      type="button"
                      onClick={() => setTargetGroup(grp.id)}
                      className={`py-2 px-2 rounded-xl border text-xs font-semibold transition-all cursor-pointer text-center ${
                        targetGroup === grp.id
                          ? 'bg-primary/10 border-primary text-primary font-bold shadow-xs'
                          : 'bg-surface border-outline-variant/60 text-on-surface-variant'
                      }`}
                    >
                      {grp.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Stage Selection & Week Window */}
              <div className="space-y-3">
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-on-surface mb-1">Giai đoạn thai kỳ</label>
                    <select
                      value={stage}
                      onChange={(e) => setStage(e.target.value as ContentStage)}
                      className={field}
                    >
                      {AUTHORABLE_STAGES.map((s) => (
                        <option key={s} value={s}>
                          {STAGE_LABELS[s]}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-on-surface mb-1">Kiểu cửa sổ</label>
                    <select
                      value={windowMode}
                      onChange={(e) => setWindowMode(e.target.value as 'SINGLE' | 'RANGE')}
                      className={field}
                    >
                      <option value="SINGLE">Một tuần</option>
                      <option value="RANGE">Khoảng tuần</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-on-surface mb-1">Từ tuần thai</label>
                    <select
                      value={windowStart}
                      onChange={(e) => setWindowStart(Number(e.target.value))}
                      className={field}
                    >
                      {SOURCE_WEEK_OPTIONS.map((w) => (
                        <option key={w} value={w}>
                          Tuần {w}
                        </option>
                      ))}
                    </select>
                  </div>

                  {windowMode === 'RANGE' && (
                    <div>
                      <label className="block text-xs font-semibold text-on-surface mb-1">Đến tuần thai</label>
                      <select
                        value={windowEnd}
                        onChange={(e) => setWindowEnd(Number(e.target.value))}
                        className={field}
                      >
                        {SOURCE_WEEK_OPTIONS.filter((w) => w >= windowStart).map((w) => (
                          <option key={w} value={w}>
                            Tuần {w}
                          </option>
                        ))}
                      </select>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </section>

          {/* Section 3: Danh sách các mục công việc */}
          <section className={card}>
            <div className="mb-4 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <ClipboardList className="text-primary" size={18} />
                <h3 className="m-0 text-sm font-bold text-on-surface">
                  Danh sách việc cần làm / Chỉ định y tế ({items.length})
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setItems((prev) => [...prev, newRow('', 'Khám thai & Y tế', false)])}
                className="inline-flex items-center gap-1.5 py-1.5 px-3 rounded-full border border-primary/40 bg-primary/5 text-xs font-bold text-primary hover:bg-primary/10 transition-colors cursor-pointer"
              >
                <Plus size={15} /> Thêm mục
              </button>
            </div>

            {/* Recurrence Header */}
            <div className="mb-4 flex flex-wrap items-center gap-4 rounded-xl border border-surface-container-highest bg-surface-container-low p-3">
              <span className="text-xs font-bold text-on-surface">Nhịp lặp lại của danh sách:</span>
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-1.5 text-xs font-medium text-on-surface-variant cursor-pointer">
                  <input
                    type="checkbox"
                    checked={listRepeatWeekly}
                    onChange={(e) => handleListWeeklyChange(e.target.checked)}
                    className="w-4 h-4 accent-primary rounded"
                  />
                  Lặp từng tuần
                </label>
                <label className="flex items-center gap-1.5 text-xs font-medium text-on-surface-variant cursor-pointer">
                  <input
                    type="checkbox"
                    checked={listRepeatDaily}
                    onChange={(e) => handleListDailyChange(e.target.checked)}
                    className="w-4 h-4 accent-primary rounded"
                  />
                  Lặp từng ngày
                </label>
              </div>
            </div>

            {/* Items List */}
            <div className="space-y-3.5">
              {items.map((row, idx) => (
                <div
                  key={row.key}
                  className="p-4 rounded-xl border border-surface-container-highest bg-surface-bright space-y-3 relative group"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <span className="w-6 h-6 rounded-full bg-primary/10 text-primary font-bold text-xs flex items-center justify-center">
                        {idx + 1}
                      </span>
                      <span className="text-xs font-bold text-on-surface">Mục {idx + 1} (Bắt buộc)</span>
                    </div>

                    {items.length > 1 && (
                      <button
                        type="button"
                        onClick={() => setItems((prev) => prev.filter((i) => i.key !== row.key))}
                        className="w-7 h-7 rounded-lg flex items-center justify-center text-rose-500 hover:bg-rose-50 transition-colors cursor-pointer"
                        title="Xóa mục này"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>

                  {/* Item Text */}
                  <div>
                    <label className="block text-[11px] font-semibold text-on-surface mb-1">
                      Nội dung việc cần làm / Chỉ định y tế <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={row.itemText}
                      onChange={(e) => updateItem(row.key, { itemText: e.target.value })}
                      placeholder="Ví dụ: Đo huyết áp 2 lần/ngày, Xét nghiệm nghiệm pháp dung nạp đường huyết..."
                      className={field}
                      required
                    />
                  </div>

                  {/* Link nguồn & Chức năng hỗ trợ */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-semibold text-on-surface mb-1">
                        Link nguồn / Tài liệu tham khảo y khoa
                      </label>
                      <input
                        type="url"
                        value={row.sourceUrl}
                        onChange={(e) => updateItem(row.key, { sourceUrl: e.target.value })}
                        placeholder="https://... liên kết bài viết, hướng dẫn y tế hoặc tài liệu tham khảo"
                        className={field}
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-semibold text-on-surface mb-1">
                        Chức năng hỗ trợ liên kết
                      </label>
                      <select
                        value={row.supportFunction}
                        onChange={(e) =>
                          updateItem(row.key, {
                            supportFunction: (e.target.value as ChecklistSupportFunction) || '',
                          })
                        }
                        className={field}
                      >
                        <option value="">🚫 Không liên kết</option>
                        {CHECKLIST_SUPPORT_FUNCTION_OPTIONS.map((opt) => (
                          <option key={opt.value} value={opt.value}>
                            {opt.label}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
          </>
          )}

          {/* Modal Footer Actions */}
          <div className="sticky bottom-0 bg-surface border-t border-outline-variant/50 p-4 -mx-6 -mb-6 flex items-center justify-end gap-3 shadow-lg">
            <button
              type="button"
              onClick={onClose}
              className="py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-on-surface text-xs font-semibold hover:bg-surface-container-low cursor-pointer transition-colors"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="inline-flex items-center gap-2 py-2.5 px-6 rounded-full bg-primary text-on-primary text-xs font-bold shadow-md hover:bg-primary/90 disabled:opacity-50 cursor-pointer transition-all"
            >
              {submitting ? (
                <>
                  <span className="material-symbols-outlined text-sm animate-spin">progress_activity</span>
                  Đang lưu vào lộ trình...
                </>
              ) : (
                <>
                  <Save size={16} />
                  {mode === 'EDIT' ? 'Cập nhật việc cần làm' : 'Lưu vào lộ trình của mẹ bầu'}
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
