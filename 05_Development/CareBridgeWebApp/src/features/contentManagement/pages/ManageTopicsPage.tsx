import { useEffect, useState, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  CommunityTopic,
  CommunityTopicType,
  CreateCommunityTopicPayload,
  UpdateCommunityTopicPayload,
} from '../models/content';
import { getTopicMutationErrorMessage } from './topicErrors';
import { buildTopicTree } from './topicTree';

/* ------------------------------------------------------------------ */
/*  Types                                                              */
/* ------------------------------------------------------------------ */
interface TopicFormState {
  name: string;
  type: CommunityTopicType;
  parentId: string;
  sortOrder: number;
  description: string;
}

const DEFAULT_FORM: TopicFormState = {
  name: '',
  type: 'TOPIC',
  parentId: '',
  sortOrder: 0,
  description: '',
};

/* ------------------------------------------------------------------ */
/*  Sub-components                                                     */
/* ------------------------------------------------------------------ */
interface TypeBadgeProps { type: CommunityTopicType }
function TypeBadge({ type }: TypeBadgeProps) {
  if (type === 'CATEGORY') {
    return (
      <span className="inline-flex items-center gap-1 px-3 py-[3px] rounded-full bg-surface-container-high text-on-surface text-xs font-medium">
        <span className="material-symbols-outlined text-sm">label</span>
        Danh mục
      </span>
    );
  }
  if (type === 'TAG') {
    return (
      <span className="inline-flex items-center gap-1 px-3 py-[3px] rounded-full bg-surface-dim text-on-surface text-xs font-medium">
        <span className="material-symbols-outlined text-sm">sell</span>
        Thẻ (Tag)
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 px-3 py-[3px] rounded-full bg-tertiary-fixed text-on-tertiary-fixed text-xs font-medium">
      <span className="material-symbols-outlined text-sm">folder</span>
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
  const [editingSlug, setEditingSlug] = useState<string | null>(null);
  const [form, setForm] = useState<TopicFormState>(DEFAULT_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [actionError, setActionError] = useState('');

  // Expand/collapse for CATEGORY root rows
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

  /* ── Filter + tree ── */
  const filteredTopics = topics.filter((t) =>
    t.name.toLowerCase().includes(searchQuery.toLowerCase()),
  );
  const categoryItems = topics.filter((topic) => topic.type === 'CATEGORY');
  const renderRows = buildTopicTree(filteredTopics, expandedIds);

  /* ── Drawer helpers ── */
  function openCreateDrawer() {
    setEditingId(null);
    setEditingSlug(null);
    setForm(DEFAULT_FORM);
    setSubmitError('');
    setIsDrawerOpen(true);
  }

  function openEditDrawer(topic: CommunityTopic) {
    setEditingId(topic.id);
    setEditingSlug(topic.slug);
    setForm({
      name: topic.name,
      type: topic.type,
      parentId: topic.parentId ?? '',
      sortOrder: topic.sortOrder,
      description: topic.description ?? '',
    });
    setSubmitError('');
    setIsDrawerOpen(true);
  }

  function closeDrawer() {
    setIsDrawerOpen(false);
    setEditingId(null);
    setEditingSlug(null);
  }

  function handleNameChange(name: string) {
    setForm((prev) => ({ ...prev, name }));
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

  async function handleDelete(topic: CommunityTopic) {
    if (!window.confirm(`Xoá "${topic.name}"? Hành động này không thể hoàn tác.`)) return;
    setActionError('');
    try {
      await apiClient.delete(`/api/v1/community/topics/${topic.id}`);
      setTopics((prev) => prev.filter((item) => item.id !== topic.id));
      setExpandedIds((prev) => {
        const next = new Set(prev);
        next.delete(topic.id);
        return next;
      });
    } catch (err: unknown) {
      setActionError(getTopicMutationErrorMessage(err));
    }
  }

  async function handleSubmit() {
    if (!form.name.trim() || (form.type === 'TOPIC' && !form.parentId)) return;
    setIsSubmitting(true);
    setSubmitError('');
    try {
      const commonPayload = {
        name: form.name.trim(),
        description: form.description,
        sortOrder: form.sortOrder,
      };
      if (editingId) {
        const payload: UpdateCommunityTopicPayload = {
          ...commonPayload,
          parentId: form.type === 'TOPIC' ? form.parentId : null,
        };
        const res = await apiClient.patch<ApiResponse<CommunityTopic>>(
          `/api/v1/community/topics/${editingId}`,
          payload,
        );
        setTopics((prev) => prev.map((t) => (t.id === editingId ? res.data.data : t)));
      } else {
        const payload: CreateCommunityTopicPayload = form.type === 'TOPIC'
          ? { ...commonPayload, type: 'TOPIC', parentId: form.parentId }
          : { ...commonPayload, type: form.type, parentId: null };
        const res = await apiClient.post<ApiResponse<CommunityTopic>>(
          '/api/v1/community/topics',
          payload,
        );
        setTopics((prev) => [...prev, res.data.data]);
      }
      closeDrawer();
    } catch (err: unknown) {
      setSubmitError(getTopicMutationErrorMessage(err));
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

  /* ── Real reorder (ADR-COM-019): swap sortOrder with the adjacent sibling, persisted via PATCH.
     Siblings share both type and parentId, so CATEGORY roots, child TOPICs and flat TAGs never mix. ── */
  function getSiblings(item: CommunityTopic): CommunityTopic[] {
    return topics
      .filter((topic) => topic.parentId === item.parentId && topic.type === item.type)
      .sort((a, b) => a.sortOrder - b.sortOrder);
  }

  async function handleMove(item: CommunityTopic, direction: 'up' | 'down') {
    const siblings = getSiblings(item);
    const index = siblings.findIndex((s) => s.id === item.id);
    const swapIndex = direction === 'up' ? index - 1 : index + 1;
    if (swapIndex < 0 || swapIndex >= siblings.length) return;
    const other = siblings[swapIndex];

    try {
      const [updatedItem, updatedOther] = await Promise.all([
        apiClient.patch<ApiResponse<CommunityTopic>>(`/api/v1/community/topics/${item.id}`, { sortOrder: other.sortOrder }),
        apiClient.patch<ApiResponse<CommunityTopic>>(`/api/v1/community/topics/${other.id}`, { sortOrder: item.sortOrder }),
      ]);
      setTopics((prev) => prev.map((t) => {
        if (t.id === item.id) return updatedItem.data.data;
        if (t.id === other.id) return updatedOther.data.data;
        return t;
      }));
    } catch {
      alert('Không thể sắp xếp lại. Vui lòng thử lại.');
    }
  }

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
              className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-xl"
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
            className="flex items-center gap-2 h-[52px] px-6 rounded-full bg-primary text-white border-none text-[15px] font-semibold cursor-pointer shadow-[0_4px_20px_rgba(90,70,63,0.12)] font-sans"
          >
            <span className="material-symbols-outlined text-xl">add</span>
            Tạo mới
          </button>
        </div>
      </div>

      {actionError && (
        <div className="mb-5 rounded-xl bg-error-container px-4 py-3 text-sm text-error">
          {actionError}
        </div>
      )}

      {/* ── Table ── */}
      <div className="bg-surface rounded-3xl shadow-[0_4px_20px_rgba(90,70,63,0.06)] overflow-hidden">

        {/* Table header */}
        <div className="flex px-6 py-[14px] bg-surface-container-low border-b border-surface-container-high text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">
          <div className="w-[30%]">Tên / Chủ đề</div>
          <div className="w-[15%]">Loại</div>
          <div className="w-[15%] text-center">Nội dung</div>
          <div className="w-[15%] text-center">Sắp xếp</div>
          <div className="w-[25%] text-right">Trạng thái &amp; Thao tác</div>
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
            const isCategory = item.type === 'CATEGORY';
            const isExpanded = expandedIds.has(item.id);
            const isHovered = hoveredId === item.id;
            const isActive = !item.isHidden;
            const siblings = getSiblings(item);
            const siblingIndex = siblings.findIndex((s) => s.id === item.id);

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
                  <div className="w-[30%] flex items-center gap-3">
                    {isCategory && (
                      <button
                        onClick={() => toggleExpand(item.id)}
                        className="w-8 h-8 rounded-full border-none bg-transparent cursor-pointer flex items-center justify-center text-on-surface-variant flex-shrink-0"
                      >
                        <span className="material-symbols-outlined text-xl">
                          {isExpanded ? 'expand_more' : 'chevron_right'}
                        </span>
                      </button>
                    )}
                    {isChild && (
                      <span className="w-2 h-2 rounded-full bg-outline-variant flex-shrink-0" />
                    )}
                    {item.type === 'TAG' && !isChild && <span className="w-8 flex-shrink-0" />}
                    <div>
                      <div className={`${isCategory ? 'font-semibold' : 'font-medium'} text-sm text-on-surface`}>
                        {item.name}
                      </div>
                      <div className="text-xs text-on-surface-variant mt-0.5">
                        {item.slug}
                      </div>
                    </div>
                  </div>

                  {/* Type badge */}
                  <div className="w-[15%]">
                    <TypeBadge type={item.type} />
                  </div>

                  {/* Content count — real, from backend (ADR-COM-015) */}
                  <div className="w-[15%] text-center text-sm text-on-surface-variant">
                    {item.questionCount} bài
                  </div>

                  {/* Reorder — real, persisted via PATCH (ADR-COM-019) */}
                  <div className="w-[15%] flex items-center justify-center gap-1">
                    <button
                      onClick={() => handleMove(item, 'up')}
                      disabled={siblingIndex <= 0}
                      title="Di chuyển lên"
                      className="border-none bg-transparent cursor-pointer text-outline p-1 disabled:opacity-30 disabled:cursor-not-allowed"
                    >
                      <span className="material-symbols-outlined text-lg">arrow_upward</span>
                    </button>
                    <button
                      onClick={() => handleMove(item, 'down')}
                      disabled={siblingIndex === -1 || siblingIndex >= siblings.length - 1}
                      title="Di chuyển xuống"
                      className="border-none bg-transparent cursor-pointer text-outline p-1 disabled:opacity-30 disabled:cursor-not-allowed"
                    >
                      <span className="material-symbols-outlined text-lg">arrow_downward</span>
                    </button>
                  </div>

                  {/* Actions */}
                  <div className="w-[25%] flex items-center justify-end gap-3">
                    <Toggle checked={isActive} onChange={() => handleToggleHidden(item)} />
                    <button
                      onClick={() => openEditDrawer(item)}
                      title="Chỉnh sửa"
                      className="border-none bg-transparent cursor-pointer text-outline p-1 flex items-center"
                    >
                      <span className="material-symbols-outlined text-xl">edit</span>
                    </button>
                    {(item.type === 'CATEGORY' || item.type === 'TOPIC') && (
                      <button
                        onClick={() => handleDelete(item)}
                        title="Xoá"
                        className="border-none bg-transparent cursor-pointer text-outline p-1 flex items-center"
                      >
                        <span className="material-symbols-outlined text-xl">delete</span>
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
            <span className="material-symbols-outlined text-[22px]">close</span>
          </button>
        </div>

        {/* Drawer body */}
        <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-6">

          {/* Type selection */}
          <div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
              Loại phân loại
            </label>
            {editingId ? (
              <div className="h-14 px-4 flex items-center border border-surface-container-highest rounded-xl bg-surface-container-low">
                <TypeBadge type={form.type} />
                <span className="ml-3 text-xs text-on-surface-variant">Không thể thay đổi sau khi tạo</span>
              </div>
            ) : (
              <div className="grid grid-cols-3 gap-2">
                {([['TOPIC', 'Chủ đề'], ['CATEGORY', 'Danh mục'], ['TAG', 'Thẻ (Tag)']] as [CommunityTopicType, string][]).map(([value, label]) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setForm((prev) => ({ ...prev, type: value, parentId: '' }))}
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
            )}
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

          {/* Slug — read-only, server-generated (ADR-COM-018) */}
          <div>
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
              Đường dẫn tĩnh (Slug)
            </label>
            <div className="w-full h-14 px-4 flex items-center border border-surface-container-highest rounded-xl text-sm text-on-surface-variant bg-surface-container-low box-border">
              {editingSlug ?? 'Sẽ được tạo tự động khi lưu'}
            </div>
          </div>

          {/* TOPIC requires a visible CATEGORY parent (ADR-COM-020). CATEGORY/TAG stay flat. */}
          {form.type === 'TOPIC' && (
            <div>
              <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
                Danh mục cha <span className="text-error">*</span>
              </label>
              <div className="relative">
                <select
                  required
                  value={form.parentId}
                  onChange={(e) => setForm((prev) => ({ ...prev, parentId: e.target.value }))}
                  className="w-full h-14 pr-12 pl-4 border border-outline-variant rounded-xl text-sm text-on-surface bg-surface outline-none appearance-none cursor-pointer font-sans box-border"
                >
                  <option value="">-- Chọn danh mục --</option>
                  {categoryItems.filter((category) => !category.isHidden).map((category) => (
                    <option key={category.id} value={category.id}>{category.name}</option>
                  ))}
                </select>
                <span
                  className="material-symbols-outlined absolute right-[14px] top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant text-xl"
                >
                  expand_more
                </span>
              </div>
            </div>
          )}

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
            disabled={isSubmitting || !form.name.trim() || (form.type === 'TOPIC' && !form.parentId)}
            className={`h-[52px] px-8 rounded-full text-white border-none text-[15px] font-semibold font-sans shadow-[0_2px_8px_rgba(90,70,63,0.2)] transition-[background] duration-200 ${
              isSubmitting || !form.name.trim() || (form.type === 'TOPIC' && !form.parentId)
                ? 'bg-outline-variant cursor-not-allowed'
                : 'bg-primary cursor-pointer'
            }`}
          >
            {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
      </div>
    </div>
  );
}
