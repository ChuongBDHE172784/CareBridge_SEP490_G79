import { useEffect, useState, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type { CommunityTopic } from '../models/content';

/* ------------------------------------------------------------------ */
/*  Types                                                              */
/* ------------------------------------------------------------------ */
type TaxonomyType = 'TOPIC' | 'CATEGORY' | 'TAG';

interface TopicFormState {
  name: string;
  slug: string;
  type: TaxonomyType;
  parentId: string;
  sortOrder: number;
  description: string;
}

const DEFAULT_FORM: TopicFormState = {
  name: '',
  slug: '',
  type: 'TOPIC',
  parentId: '',
  sortOrder: 0,
  description: '',
};

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function generateSlug(name: string): string {
  return name
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'd')
    .replace(/[^a-z0-9\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-');
}

function iconForType(type: TaxonomyType): string {
  if (type === 'CATEGORY') return 'label';
  if (type === 'TAG') return 'sell';
  return 'folder';
}

function typeFromIcon(icon?: string): TaxonomyType {
  if (icon === 'label') return 'CATEGORY';
  if (icon === 'sell') return 'TAG';
  return 'TOPIC';
}

/* ------------------------------------------------------------------ */
/*  Sub-components                                                     */
/* ------------------------------------------------------------------ */
interface TypeBadgeProps { icon?: string }
function TypeBadge({ icon }: TypeBadgeProps) {
  if (icon === 'label') {
    return (
      <span className="inline-flex items-center gap-1 px-3 py-[3px] rounded-full bg-surface-container-high text-on-surface text-xs font-medium">
        <span className="material-symbols-outlined" style={{ fontSize: 14 }}>label</span>
        Danh mục
      </span>
    );
  }
  if (icon === 'sell') {
    return (
      <span className="inline-flex items-center gap-1 px-3 py-[3px] rounded-full bg-surface-dim text-on-surface text-xs font-medium">
        <span className="material-symbols-outlined" style={{ fontSize: 14 }}>sell</span>
        Thẻ (Tag)
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 px-3 py-[3px] rounded-full bg-tertiary-fixed text-on-tertiary-fixed text-xs font-medium">
      <span className="material-symbols-outlined" style={{ fontSize: 14 }}>folder</span>
      Chủ đề
    </span>
  );
}

interface ToggleProps { checked: boolean; onChange: () => void }
function Toggle({ checked, onChange }: ToggleProps) {
  return (
    <label className="relative inline-flex items-center cursor-pointer">
      <input
        type="checkbox"
        checked={checked}
        onChange={onChange}
        className="absolute opacity-0 w-0 h-0"
      />
      <span
        className={`inline-block w-11 h-6 rounded-xl relative transition-[background] duration-200 ${
          checked ? 'bg-primary-container' : 'bg-surface-container-highest'
        }`}
      >
        <span
          className={`absolute top-0.5 w-5 h-5 rounded-full bg-white shadow-[0_1px_4px_rgba(0,0,0,0.15)] transition-[left] duration-200 ${
            checked ? 'left-[22px]' : 'left-0.5'
          }`}
        />
      </span>
    </label>
  );
}

/* ------------------------------------------------------------------ */
/*  Page                                                               */
/* ------------------------------------------------------------------ */
export default function ManageTopicsPage() {
  const [topics, setTopics] = useState<CommunityTopic[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Drawer state
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<TopicFormState>(DEFAULT_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  // Expand/collapse for topic rows
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  // Row hover tracking
  const [hoveredId, setHoveredId] = useState<string | null>(null);

  const loadTopics = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await apiClient.get<ApiResponse<CommunityTopic[]>>(
        '/api/v1/community/topics?includeHidden=true',
      );
      setTopics(res.data.data ?? []);
    } catch {
      setError('Không thể tải danh sách. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { loadTopics(); }, [loadTopics]);

  /* ── Filter ── */
  const filteredTopics = topics.filter((t) =>
    t.name.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  // Separate topics (folder), categories (label), tags (sell)
  const topicItems = filteredTopics.filter((t) => t.icon !== 'label' && t.icon !== 'sell');
  const categoryItems = filteredTopics.filter((t) => t.icon === 'label');
  const tagItems = filteredTopics.filter((t) => t.icon === 'sell');

  /* ── Drawer helpers ── */
  function openCreateDrawer() {
    setEditingId(null);
    setForm(DEFAULT_FORM);
    setSubmitError('');
    setIsDrawerOpen(true);
  }

  function openEditDrawer(topic: CommunityTopic) {
    setEditingId(topic.id);
    setForm({
      name: topic.name,
      slug: generateSlug(topic.name),
      type: typeFromIcon(topic.icon),
      parentId: '',
      sortOrder: topic.sortOrder,
      description: topic.description ?? '',
    });
    setSubmitError('');
    setIsDrawerOpen(true);
  }

  function closeDrawer() {
    setIsDrawerOpen(false);
    setEditingId(null);
  }

  function handleNameChange(name: string) {
    setForm((prev) => ({ ...prev, name, slug: generateSlug(name) }));
  }

  /* ── API actions ── */
  async function handleToggleHidden(topic: CommunityTopic) {
    try {
      const res = await apiClient.patch<ApiResponse<CommunityTopic>>(
        `/api/v1/community/topics/${topic.id}`,
        { isHidden: !topic.isHidden },
      );
      setTopics((prev) => prev.map((t) => (t.id === topic.id ? res.data.data : t)));
    } catch {
      // toggle reverts automatically — no extra state needed
    }
  }

  async function handleSoftDelete(topic: CommunityTopic) {
    if (!confirm(`Ẩn "${topic.name}"? Các nội dung liên quan vẫn được giữ lại trong hệ thống.`)) return;
    try {
      const res = await apiClient.patch<ApiResponse<CommunityTopic>>(
        `/api/v1/community/topics/${topic.id}`,
        { isHidden: true },
      );
      setTopics((prev) => prev.map((t) => (t.id === topic.id ? res.data.data : t)));
    } catch {
      alert('Không thể ẩn phân loại. Vui lòng thử lại.');
    }
  }

  async function handleSubmit() {
    if (!form.name.trim()) return;
    setIsSubmitting(true);
    setSubmitError('');
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description,
        icon: iconForType(form.type),
        sortOrder: form.sortOrder,
      };
      if (editingId) {
        const res = await apiClient.patch<ApiResponse<CommunityTopic>>(
          `/api/v1/community/topics/${editingId}`,
          payload,
        );
        setTopics((prev) => prev.map((t) => (t.id === editingId ? res.data.data : t)));
      } else {
        const res = await apiClient.post<ApiResponse<CommunityTopic>>(
          '/api/v1/community/topics',
          payload,
        );
        setTopics((prev) => [...prev, res.data.data]);
      }
      closeDrawer();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error?.message ?? '';
      if (msg.toLowerCase().includes('exist') || msg.toLowerCase().includes('duplicate') || msg.toLowerCase().includes('com-009')) {
        setSubmitError('Tên phân loại này đã tồn tại. Vui lòng chọn tên khác.');
      } else {
        setSubmitError('Đã xảy ra lỗi. Vui lòng thử lại.');
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  function toggleExpand(id: string) {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  /* ── Build render list (topics with nested categories, then standalone tags) ── */
  // For now backend has no parentId — we render topics first, then categories, then tags.
  // TODO: wire parent-child hierarchy when backend adds parentId to CommunityTopic.
  const renderRows: Array<{ item: CommunityTopic; isChild: boolean }> = [];
  topicItems.forEach((topic) => {
    renderRows.push({ item: topic, isChild: false });
    if (expandedIds.has(topic.id)) {
      categoryItems.forEach((cat) => renderRows.push({ item: cat, isChild: true }));
    }
  });
  tagItems.forEach((tag) => renderRows.push({ item: tag, isChild: false }));

  return (
    <div className="p-8 font-sans bg-background min-h-screen">

      {/* ── Page header ── */}
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-[30px] font-bold text-on-surface m-0">
            Quản lý Chủ đề &amp; Danh mục
          </h1>
          <p className="text-on-surface-variant text-sm mt-[6px]">
            Tổ chức và sắp xếp cấu trúc nội dung hiển thị trên ứng dụng.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {/* Search */}
          <div className="relative">
            <span
              className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline"
              style={{ fontSize: 20 }}
            >
              search
            </span>
            <input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm kiếm danh mục..."
              className="pl-10 pr-4 py-2.5 rounded-full border border-outline-variant bg-surface-container-low text-sm text-on-surface outline-none w-60 font-sans"
            />
          </div>
          {/* Tạo mới button */}
          <button
            onClick={openCreateDrawer}
            className="flex items-center gap-2 h-[52px] px-6 rounded-full bg-primary-container text-white border-none text-[15px] font-semibold cursor-pointer shadow-[0_4px_20px_rgba(90,70,63,0.12)] font-sans"
          >
            <span className="material-symbols-outlined" style={{ fontSize: 20 }}>add</span>
            Tạo mới
          </button>
        </div>
      </div>

      {/* ── Table ── */}
      <div className="bg-surface rounded-3xl shadow-[0_4px_20px_rgba(90,70,63,0.06)] overflow-hidden">

        {/* Table header */}
        <div className="flex px-6 py-[14px] bg-surface-container-low border-b border-surface-container-high text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">
          <div className="w-[33%]">Tên / Chủ đề</div>
          <div className="w-[16%]">Loại</div>
          <div className="w-[16%] text-center">Nội dung</div>
          <div className="w-[16%] text-center">Sắp xếp</div>
          <div className="w-[19%] text-right">Trạng thái &amp; Thao tác</div>
        </div>

        {/* Table body */}
        <div>
          {isLoading && (
            <div className="px-6 py-12 text-center text-outline text-sm">
              Đang tải...
            </div>
          )}
          {error && (
            <div className="px-6 py-12 text-center text-error text-sm">
              {error}
            </div>
          )}
          {!isLoading && !error && renderRows.length === 0 && (
            <div className="px-6 py-12 text-center text-outline text-sm">
              Chưa có phân loại nào. Nhấn <strong>Tạo mới</strong> để bắt đầu.
            </div>
          )}

          {renderRows.map(({ item, isChild }) => {
            const isTopic = item.icon !== 'label' && item.icon !== 'sell';
            const isExpanded = expandedIds.has(item.id);
            const isHovered = hoveredId === item.id;
            const isActive = !item.isHidden;

            return (
              <div
                key={item.id}
                className={`border-b border-surface-container-low transition-[background] duration-[150ms] ${
                  isHovered || isChild ? 'bg-surface-bright' : 'bg-surface'
                }`}
                onMouseEnter={() => setHoveredId(item.id)}
                onMouseLeave={() => setHoveredId(null)}
              >
                <div className={`flex items-center ${isChild ? 'py-3 pr-6 pl-16' : 'py-4 px-6'}`}>
                  {/* Name column */}
                  <div className="w-[33%] flex items-center gap-3">
                    {isTopic && (
                      <button
                        onClick={() => toggleExpand(item.id)}
                        className="w-8 h-8 rounded-full border-none bg-transparent cursor-pointer flex items-center justify-center text-on-surface-variant flex-shrink-0"
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: 20 }}>
                          {isExpanded ? 'expand_more' : 'chevron_right'}
                        </span>
                      </button>
                    )}
                    {isChild && (
                      <span className="w-2 h-2 rounded-full bg-outline-variant flex-shrink-0" />
                    )}
                    {item.icon === 'sell' && <span className="w-8 flex-shrink-0" />}
                    <div>
                      <div className={`${isTopic ? 'font-semibold' : 'font-medium'} text-sm text-on-surface`}>
                        {item.name}
                      </div>
                      <div className="text-xs text-on-surface-variant mt-0.5">
                        {generateSlug(item.name)}
                      </div>
                    </div>
                  </div>

                  {/* Type badge */}
                  <div className="w-[16%]">
                    <TypeBadge icon={item.icon ?? 'folder'} />
                  </div>

                  {/* Content count — TODO: wire to content count API */}
                  <div className="w-[16%] text-center text-sm text-on-surface-variant">
                    — bài
                  </div>

                  {/* Drag handle */}
                  <div className="w-[16%] text-center">
                    <button
                      className="border-none bg-transparent cursor-grab text-outline p-1"
                      title="Kéo để sắp xếp"
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: 20 }}>drag_indicator</span>
                    </button>
                  </div>

                  {/* Actions */}
                  <div className="w-[19%] flex items-center justify-end gap-3">
                    <Toggle checked={isActive} onChange={() => handleToggleHidden(item)} />
                    <button
                      onClick={() => openEditDrawer(item)}
                      title="Chỉnh sửa"
                      className="border-none bg-transparent cursor-pointer text-outline p-1 flex items-center"
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: 20 }}>edit</span>
                    </button>
                    {isTopic && (
                      <button
                        onClick={() => handleSoftDelete(item)}
                        title="Ẩn"
                        className="border-none bg-transparent cursor-pointer text-outline p-1 flex items-center"
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: 20 }}>delete</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* ── Drawer overlay ── */}
      {isDrawerOpen && (
        <div
          onClick={closeDrawer}
          className="fixed inset-0 bg-[rgba(39,24,18,0.4)] backdrop-blur-[4px] z-50"
        />
      )}

      {/* ── Side drawer ── */}
      <div
        className={`fixed right-0 top-0 h-full w-[400px] bg-surface shadow-[-8px_0_40px_rgba(90,70,63,0.15)] z-[51] flex flex-col font-sans transition-transform duration-300 ease-in-out ${
          isDrawerOpen ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        {/* Drawer header */}
        <div className="px-6 py-5 border-b border-surface-container-high flex justify-between items-center bg-surface-bright">
          <h3 className="m-0 text-[20px] font-semibold text-on-surface">
            {editingId ? 'Chỉnh sửa Phân loại' : 'Tạo mới Phân loại'}
          </h3>
          <button
            onClick={closeDrawer}
            className="w-10 h-10 rounded-full border-none bg-transparent cursor-pointer flex items-center justify-center text-on-surface-variant"
          >
            <span className="material-symbols-outlined" style={{ fontSize: 22 }}>close</span>
          </button>
        </div>

        {/* Drawer body */}
        <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-6">

          {/* Type selection */}
          <div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
              Loại phân loại
            </label>
            <div className="grid grid-cols-3 gap-2">
              {([['TOPIC', 'Chủ đề'], ['CATEGORY', 'Danh mục'], ['TAG', 'Thẻ (Tag)']] as [TaxonomyType, string][]).map(([value, label]) => (
                <button
                  key={value}
                  onClick={() => setForm((prev) => ({ ...prev, type: value }))}
                  className={`py-[10px] px-1 rounded-xl cursor-pointer text-[13px] font-medium font-sans transition-all duration-[150ms] ${
                    form.type === value
                      ? 'border-2 border-primary bg-[rgba(201,140,123,0.08)] text-primary'
                      : 'border border-outline-variant bg-surface text-on-surface-variant'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          {/* Name input */}
          <div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
              Tên hiển thị <span className="text-error">*</span>
            </label>
            <input
              value={form.name}
              onChange={(e) => handleNameChange(e.target.value)}
              placeholder="VD: Sức khỏe bà bầu"
              className="w-full h-14 px-4 border border-outline-variant rounded-xl text-base text-on-surface bg-surface outline-none font-sans box-border focus:border-primary focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Slug input */}
          <div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
              Đường dẫn tĩnh (Slug)
            </label>
            <div className="relative">
              <input
                value={form.slug}
                onChange={(e) => setForm((prev) => ({ ...prev, slug: e.target.value }))}
                placeholder="VD: suc-khoe-ba-bau"
                className="w-full h-14 pr-[112px] pl-4 border border-surface-container-highest rounded-xl text-sm text-on-surface-variant bg-surface-container-low outline-none font-sans box-border focus:border-primary focus:ring-1 focus:ring-primary"
              />
              <button
                onClick={() => setForm((prev) => ({ ...prev, slug: generateSlug(prev.name) }))}
                className="absolute right-3 top-1/2 -translate-y-1/2 border-none bg-transparent cursor-pointer text-xs font-semibold text-primary font-sans"
              >
                Tạo tự động
              </button>
            </div>
          </div>

          {/* Parent topic — UI only; TODO: add parentId to backend */}
          <div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
              Chủ đề cha
            </label>
            <div className="relative">
              <select
                value={form.parentId}
                onChange={(e) => setForm((prev) => ({ ...prev, parentId: e.target.value }))}
                className="w-full h-14 pr-12 pl-4 border border-outline-variant rounded-xl text-sm text-on-surface bg-surface outline-none appearance-none cursor-pointer font-sans box-border"
              >
                <option value="">-- Không có (Làm chủ đề gốc) --</option>
                {topicItems.filter((t) => !t.isHidden).map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
              <span
                className="material-symbols-outlined absolute right-[14px] top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant"
                style={{ fontSize: 20 }}
              >
                expand_more
              </span>
            </div>
          </div>

          {/* Sort order */}
          <div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
              Thứ tự hiển thị
            </label>
            <input
              type="number"
              min={0}
              value={form.sortOrder}
              onChange={(e) => setForm((prev) => ({ ...prev, sortOrder: Number(e.target.value) }))}
              className="w-full h-14 px-4 border border-outline-variant rounded-xl text-sm text-on-surface bg-surface outline-none font-sans box-border focus:border-primary focus:ring-1 focus:ring-primary"
            />
          </div>

          {submitError && (
            <p className="text-error text-[13px] m-0">{submitError}</p>
          )}
        </div>

        {/* Drawer footer */}
        <div className="px-6 py-5 border-t border-surface-container-high bg-surface flex justify-end gap-3">
          <button
            onClick={closeDrawer}
            className="h-[52px] px-6 rounded-full border border-outline-variant bg-surface text-on-surface-variant text-[15px] font-semibold cursor-pointer font-sans"
          >
            Hủy
          </button>
          <button
            onClick={handleSubmit}
            disabled={isSubmitting || !form.name.trim()}
            className={`h-[52px] px-8 rounded-full text-white border-none text-[15px] font-semibold font-sans shadow-[0_2px_8px_rgba(90,70,63,0.2)] transition-[background] duration-200 ${
              isSubmitting || !form.name.trim()
                ? 'bg-outline-variant cursor-not-allowed'
                : 'bg-primary-container cursor-pointer'
            }`}
          >
            {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
      </div>
    </div>
  );
}
