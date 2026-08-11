import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import {
  decideChecklistTemplate,
  decideContent,
  fetchAdminChecklists,
  fetchStaffContentList,
} from '../services/contentApi';
import type { AdminChecklistTemplate, ContentDetail, ContentStage, ContentType } from '../models/content';
import { STAGE_LABELS, STAGE_OPTIONS, TYPE_LABELS } from '../models/content';
import {
  checklistApprovalErrorMessage,
  checklistCoexistenceGuidance,
  checklistRecipientLabel,
  checklistSequenceLabel,
} from './checklistApprovalPresentation';
import { SortableTableHeader, type SortDirection } from '../components/SortableTableHeader';
import { nextSortDirection, sortRows } from '../utils/tableSorting';

type QueueKind = 'CONTENT' | 'CHECKLIST';
type QueueSortKey = 'title' | 'type' | 'stage' | 'detail' | 'submittedAt';
type TypeFilter = 'ALL' | ContentType;
type BatchTarget = 'ALL' | 'ARTICLE' | 'FAQ' | 'CHECKLIST';

type QueueEntry = {
  kind: QueueKind;
  id: string;
  title: string;
  type: ContentType;
  typeLabel: string;
  stage: ContentStage | null;
  stageLabel: string;
  detail: string;
  displayOrder?: number | null;
  recipientRoles?: AdminChecklistTemplate['recipientRoles'];
  submittedAt: string | null;
  searchText: string;
};

type PendingDecision = {
  entry: QueueEntry;
  decision: 'APPROVE' | 'REJECT';
};

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;

function toContentEntry(item: ContentDetail): QueueEntry {
  const typeLabel = TYPE_LABELS[item.type];
  const stageLabel = STAGE_LABELS[item.stage];
  const detail = `Phiên bản ${item.version}`;

  return {
    kind: 'CONTENT',
    id: item.id,
    title: item.title,
    type: item.type,
    typeLabel,
    stage: item.stage,
    stageLabel,
    detail,
    submittedAt: item.updatedAt ?? item.createdAt,
    searchText: [item.title, typeLabel, stageLabel, detail, item.sourceLabel ?? ''].join(' ').toLowerCase(),
  };
}

function toChecklistEntry(item: AdminChecklistTemplate): QueueEntry {
  const typeLabel = TYPE_LABELS.CHECKLIST;
  const stageLabel = item.stage ? STAGE_LABELS[item.stage] : 'Chưa xác định giai đoạn';
  const sequenceLabel = checklistSequenceLabel(item.displayOrder, item.stage);
  const recipientLabel = checklistRecipientLabel(item.recipientRoles);
  const detail = `${sequenceLabel} · ${recipientLabel} · Phiên bản ${item.versionNo} · ${item.itemCount} mục`;

  return {
    kind: 'CHECKLIST',
    id: item.id,
    title: item.name,
    type: 'CHECKLIST',
    typeLabel,
    stage: item.stage,
    stageLabel,
    detail,
    displayOrder: item.displayOrder,
    recipientRoles: item.recipientRoles,
    submittedAt: item.updatedAt,
    searchText: [item.name, typeLabel, stageLabel, detail, item.description, recipientLabel].join(' ').toLowerCase(),
  };
}

function formatDateTime(iso: string | null): string {
  if (!iso) return 'Chưa có dữ liệu';
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

// System Admin lacks CONTENT_ADMIN, so it must use the read-only review routes rather than
// /content/:id or /content/checklists/:id (both gated to CONTENT_ADMIN — see app/router/index.tsx).
const DETAIL_PATH: Record<QueueKind, (id: string) => string> = {
  CONTENT: (id) => `/admin/content-review/${id}`,
  CHECKLIST: (id) => `/admin/content-review/checklists/${id}`,
};

/** System Admin review queue. Drafts are deliberately excluded by the API filter. */
export default function ContentApprovalQueuePage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<QueueEntry[]>([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [working, setWorking] = useState<string | null>(null);
  const [pendingDecision, setPendingDecision] = useState<PendingDecision | null>(null);
  const [dialogError, setDialogError] = useState('');
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');
  const [stageFilter, setStageFilter] = useState<'ALL' | ContentStage>('ALL');
  const [pageSize, setPageSize] = useState<(typeof PAGE_SIZE_OPTIONS)[number]>(10);
  const [page, setPage] = useState(0);
  const [sortKey, setSortKey] = useState<QueueSortKey>('submittedAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  // Dropdown & Batch publish state
  const [isBatchMenuOpen, setIsBatchMenuOpen] = useState(false);
  const [batchTarget, setBatchTarget] = useState<BatchTarget | null>(null);
  const [isBatchPublishing, setIsBatchPublishing] = useState(false);
  const [batchError, setBatchError] = useState('');
  const [selectedBatchItemIds, setSelectedBatchItemIds] = useState<Set<string>>(new Set());
  const [isBatchListExpanded, setIsBatchListExpanded] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsBatchMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      let allContent: ContentDetail[] = [];
      let contentPageNum = 0;
      let contentTotalPages = 1;

      do {
        const contentPage = await fetchStaffContentList({
          status: 'PENDING_REVIEW',
          size: 50,
          page: contentPageNum,
        });
        allContent = [...allContent, ...(contentPage.content || [])];
        contentTotalPages = contentPage.totalPages || 1;
        contentPageNum += 1;
      } while (contentPageNum < contentTotalPages && contentPageNum < 50);

      let allChecklists: AdminChecklistTemplate[] = [];
      let checklistPageNum = 0;
      let checklistTotalPages = 1;

      do {
        const checklistPage = await fetchAdminChecklists({
          status: 'PENDING_REVIEW',
          size: 50,
          page: checklistPageNum,
        });
        allChecklists = [...allChecklists, ...(checklistPage.content || [])];
        checklistTotalPages = checklistPage.totalPages || 1;
        checklistPageNum += 1;
      } while (checklistPageNum < checklistTotalPages && checklistPageNum < 50);

      setItems([
        ...allContent.map(toContentEntry),
        ...allChecklists.map(toChecklistEntry),
      ]);
    } catch {
      setItems([]);
      setError('Không tải được hàng đợi phê duyệt.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => { setPage(0); }, [search, typeFilter, stageFilter, pageSize]);

  const stats = useMemo(() => {
    const content = items.filter((item) => item.kind === 'CONTENT').length;
    const checklist = items.filter((item) => item.kind === 'CHECKLIST').length;
    const pregnancy = items.filter((item) => item.stage === 'PREGNANCY').length;
    const postpartum = items.filter((item) => item.stage === 'POSTPARTUM').length;
    return { total: items.length, content, checklist, pregnancy, postpartum };
  }, [items]);

  const batchCounts = useMemo(() => {
    const article = items.filter((item) => item.type === 'ARTICLE').length;
    const faq = items.filter((item) => item.type === 'FAQ').length;
    const checklist = items.filter((item) => item.type === 'CHECKLIST').length;
    return {
      ALL: items.length,
      ARTICLE: article,
      FAQ: faq,
      CHECKLIST: checklist,
    };
  }, [items]);

  const matchingBatchItems = useMemo(() => {
    if (!batchTarget) return [];
    return items.filter((item) => {
      if (batchTarget === 'ALL') return true;
      return item.type === batchTarget;
    });
  }, [items, batchTarget]);

  const filteredItems = useMemo(() => {
    const query = search.trim().toLowerCase();
    return items.filter((item) => {
      const matchesSearch = query.length === 0 || item.searchText.includes(query);
      const matchesType = typeFilter === 'ALL' || item.type === typeFilter;
      const matchesStage = stageFilter === 'ALL' || item.stage === stageFilter;
      return matchesSearch && matchesType && matchesStage;
    });
  }, [items, search, stageFilter, typeFilter]);

  const sortedItems = useMemo(() => sortRows(filteredItems, sortDirection, (item) => {
    switch (sortKey) {
      case 'title': return item.title;
      case 'type': return item.typeLabel;
      case 'stage': return item.stageLabel;
      case 'detail': return item.detail;
      case 'submittedAt': {
        const timestamp = item.submittedAt ? new Date(item.submittedAt).getTime() : null;
        return timestamp !== null && !Number.isNaN(timestamp) ? timestamp : null;
      }
    }
  }), [filteredItems, sortDirection, sortKey]);

  const totalPages = Math.max(1, Math.ceil(sortedItems.length / pageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const pagedItems = sortedItems.slice(currentPage * pageSize, currentPage * pageSize + pageSize);
  const pageStart = sortedItems.length === 0 ? 0 : currentPage * pageSize + 1;
  const pageEnd = Math.min((currentPage + 1) * pageSize, sortedItems.length);

  const changeSort = (key: QueueSortKey) => {
    setSortDirection(nextSortDirection(sortKey, key, sortDirection));
    setSortKey(key);
    setPage(0);
  };

  const openDecision = (entry: QueueEntry, decision: PendingDecision['decision']) => {
    setDialogError('');
    setPendingDecision({ entry, decision });
  };

  const confirmDecision = async (reason?: string) => {
    if (!pendingDecision) return;
    const { entry, decision } = pendingDecision;
    const workingKey = `${entry.kind}-${entry.id}`;
    setWorking(workingKey);
    setDialogError('');
    try {
      if (entry.kind === 'CONTENT') {
        await decideContent(entry.id, decision, reason);
      } else {
        await decideChecklistTemplate(entry.id, decision, reason);
      }
      setPendingDecision(null);
      await load();
    } catch (approvalError) {
      setDialogError(entry.kind === 'CHECKLIST'
        ? checklistApprovalErrorMessage(approvalError, decision)
        : (decision === 'APPROVE'
          ? 'Không thể xuất bản mục này. Vui lòng thử lại.'
          : 'Không thể trả mục này về nháp. Vui lòng thử lại.'));
    } finally {
      setWorking(null);
    }
  };

  const openBatchConfirmation = (target: BatchTarget) => {
    setIsBatchMenuOpen(false);
    setBatchError('');
    setIsBatchListExpanded(false);

    const targetItems = items.filter((item) => {
      if (target === 'ALL') return true;
      return item.type === target;
    });

    setSelectedBatchItemIds(new Set(targetItems.map((item) => `${item.kind}-${item.id}`)));
    setBatchTarget(target);
  };

  const toggleSelectBatchItem = (key: string) => {
    setSelectedBatchItemIds((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const toggleSelectAllBatch = () => {
    if (selectedBatchItemIds.size === matchingBatchItems.length) {
      setSelectedBatchItemIds(new Set());
    } else {
      setSelectedBatchItemIds(new Set(matchingBatchItems.map((item) => `${item.kind}-${item.id}`)));
    }
  };

  const confirmBatchPublish = async () => {
    if (!batchTarget) return;

    const targetItems = matchingBatchItems.filter((item) =>
      selectedBatchItemIds.has(`${item.kind}-${item.id}`),
    );

    if (targetItems.length === 0) {
      setBatchTarget(null);
      return;
    }

    setIsBatchPublishing(true);
    setBatchError('');

    try {
      const results = await Promise.allSettled(
        targetItems.map((entry) =>
          entry.kind === 'CONTENT'
            ? decideContent(entry.id, 'APPROVE')
            : decideChecklistTemplate(entry.id, 'APPROVE'),
        ),
      );

      const failedCount = results.filter((r) => r.status === 'rejected').length;
      if (failedCount > 0) {
        const successCount = results.length - failedCount;
        setBatchError(`Đã xuất bản ${successCount}/${results.length} mục. ${failedCount} mục bị lỗi, vui lòng thử lại.`);
        await load();
      } else {
        setBatchTarget(null);
        await load();
      }
    } catch {
      setBatchError('Không thể xuất bản các mục đã chọn. Vui lòng thử lại.');
    } finally {
      setIsBatchPublishing(false);
    }
  };

  const resetFilters = () => {
    setSearch('');
    setTypeFilter('ALL');
    setStageFilter('ALL');
  };

  const dialogTitle = pendingDecision?.decision === 'APPROVE'
    ? 'Xuất bản mục này?'
    : 'Trả mục này về nháp?';
  const dialogDescription = pendingDecision?.decision === 'APPROVE'
    ? 'Mục đã duyệt sẽ chuyển sang trạng thái xuất bản và có thể hiển thị trong thư viện nội dung.'
    : 'Mục sẽ quay về bản nháp để Content Admin chỉnh sửa trước khi gửi duyệt lại.';
  const checklistDialogGuidance = pendingDecision?.entry.kind === 'CHECKLIST'
    && pendingDecision.entry.stage === 'PRE_PREGNANCY'
    ? checklistCoexistenceGuidance(pendingDecision.entry.displayOrder, pendingDecision.entry.stage)
    : null;

  const batchDialogTitle = batchTarget === 'ALL'
    ? 'Xuất bản tất cả nội dung?'
    : batchTarget === 'ARTICLE'
      ? 'Xuất bản tất cả bài viết?'
      : batchTarget === 'FAQ'
        ? 'Xuất bản tất cả FAQ?'
        : batchTarget === 'CHECKLIST'
          ? 'Xuất bản tất cả Checklist?'
          : '';

  const batchTargetLabelMap: Record<BatchTarget, string> = {
    ALL: 'nội dung (bài viết, FAQ, checklist)',
    ARTICLE: 'bài viết',
    FAQ: 'câu hỏi FAQ',
    CHECKLIST: 'mẫu checklist',
  };

  return (
    <div className="p-8 font-sans">
      <div>
        {/* Header */}
        <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-[26px] font-bold text-on-surface m-0">Hàng đợi phê duyệt nội dung</h1>
            <p className="text-on-surface-variant text-sm mt-1">
              Bảng này gom bài viết, FAQ và checklist đang chờ System Admin quyết định. Dùng bộ lọc để tách theo loại nội dung, giai đoạn chăm sóc và tìm nhanh theo tiêu đề.
            </p>
          </div>
          <div className="flex items-center gap-3 self-start md:self-auto">
            {/* Batch Publish Dropdown */}
            <div className="relative inline-block text-left" ref={dropdownRef}>
              <button
                type="button"
                onClick={() => setIsBatchMenuOpen((prev) => !prev)}
                disabled={isLoading || isBatchPublishing || items.length === 0}
                className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer hover:bg-primary/90 disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-lg">publish</span>
                Xuất bản tất cả
                <span className="material-symbols-outlined text-lg">arrow_drop_down</span>
              </button>

              {isBatchMenuOpen && (
                <div className="absolute right-0 mt-2 w-64 rounded-2xl bg-surface border border-surface-container-highest shadow-xl py-2 z-30">
                  <button
                    type="button"
                    onClick={() => openBatchConfirmation('ALL')}
                    className="w-full text-left px-4 py-2.5 text-sm font-semibold text-on-surface hover:bg-surface-container-low flex items-center justify-between cursor-pointer"
                  >
                    <span className="flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-lg">select_all</span>
                      Xuất bản tất cả
                    </span>
                    <span className="py-0.5 px-2.5 rounded-full bg-surface-container-high text-xs font-bold text-outline">
                      {batchCounts.ALL}
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => openBatchConfirmation('ARTICLE')}
                    className="w-full text-left px-4 py-2.5 text-sm font-semibold text-on-surface hover:bg-surface-container-low flex items-center justify-between cursor-pointer"
                  >
                    <span className="flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-lg">article</span>
                      Xuất bản tất cả bài viết
                    </span>
                    <span className="py-0.5 px-2.5 rounded-full bg-surface-container-high text-xs font-bold text-outline">
                      {batchCounts.ARTICLE}
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => openBatchConfirmation('FAQ')}
                    className="w-full text-left px-4 py-2.5 text-sm font-semibold text-on-surface hover:bg-surface-container-low flex items-center justify-between cursor-pointer"
                  >
                    <span className="flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-lg">quiz</span>
                      Xuất bản tất cả FAQ
                    </span>
                    <span className="py-0.5 px-2.5 rounded-full bg-surface-container-high text-xs font-bold text-outline">
                      {batchCounts.FAQ}
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => openBatchConfirmation('CHECKLIST')}
                    className="w-full text-left px-4 py-2.5 text-sm font-semibold text-on-surface hover:bg-surface-container-low flex items-center justify-between cursor-pointer"
                  >
                    <span className="flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary text-lg">checklist</span>
                      Xuất bản tất cả Checklist
                    </span>
                    <span className="py-0.5 px-2.5 rounded-full bg-surface-container-high text-xs font-bold text-outline">
                      {batchCounts.CHECKLIST}
                    </span>
                  </button>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={() => void load()}
              disabled={isLoading || isBatchPublishing}
              className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
            >
              <span className="material-symbols-outlined text-lg">refresh</span>
              Làm mới
            </button>
          </div>
        </div>

        {/* Stats Bar */}
        <div className="mb-6 grid gap-4 md:grid-cols-5">
          {[
            { label: 'Tổng chờ duyệt', value: stats.total, icon: 'pending_actions' },
            { label: 'Bài viết / FAQ', value: stats.content, icon: 'article' },
            { label: 'Checklist', value: stats.checklist, icon: 'checklist' },
            { label: 'Thai kỳ', value: stats.pregnancy, icon: 'pregnant_woman' },
            { label: 'Hậu sản & Chăm bé', value: stats.postpartum, icon: 'family_restroom' },
          ].map((stat) => (
            <div key={stat.label} className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
              <div>
                <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">{stat.label}</span>
                <p className="text-2xl font-bold text-on-surface m-0">{stat.value}</p>
              </div>
              <span className="material-symbols-outlined text-3xl text-primary/70">{stat.icon}</span>
            </div>
          ))}
        </div>

        {/* Action & Filter Bar */}
        <div className="bg-surface rounded-2xl p-4 shadow-sm border border-surface-container-highest mb-6">
          <div className="flex flex-col xl:flex-row items-center gap-3">
            <div className="flex-1 w-full relative">
              <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Tìm theo tiêu đề, loại, giai đoạn..."
                className="w-full py-2.5 pr-[14px] pl-[42px] rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
              />
            </div>

            <div className="flex flex-wrap md:flex-nowrap items-center gap-2 w-full xl:w-auto">
              <select
                value={typeFilter}
                onChange={(event) => setTypeFilter(event.target.value as TypeFilter)}
                className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
              >
                <option value="ALL">Tất cả loại</option>
                <option value="ARTICLE">Bài viết</option>
                <option value="FAQ">FAQ</option>
                <option value="CHECKLIST">Checklist</option>
              </select>
              <select
                value={stageFilter}
                onChange={(event) => setStageFilter(event.target.value as 'ALL' | ContentStage)}
                className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
              >
                <option value="ALL">Tất cả giai đoạn</option>
                {STAGE_OPTIONS.map(({ value, label }) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>

              <select
                value={pageSize}
                onChange={(event) => setPageSize(Number(event.target.value) as typeof pageSize)}
                className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
              >
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>{size} / trang</option>
                ))}
              </select>

              {(search || typeFilter !== 'ALL' || stageFilter !== 'ALL') && (
                <button
                  type="button"
                  onClick={resetFilters}
                  className="py-2.5 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-on-surface-variant cursor-pointer hover:bg-surface-container-low flex items-center gap-1 whitespace-nowrap"
                >
                  <span className="material-symbols-outlined text-base">filter_alt_off</span>
                  Xóa lọc
                </button>
              )}
            </div>
          </div>
        </div>

        {error && (
          <div className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
            {error}
          </div>
        )}

        <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
          {isLoading ? (
            <div className="py-12 text-center text-outline">Đang tải hàng đợi phê duyệt...</div>
          ) : filteredItems.length === 0 ? (
            <div className="py-12 text-center text-outline">
              {items.length === 0 ? 'Không có nội dung chờ duyệt.' : 'Không có mục nào khớp bộ lọc hiện tại.'}
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b-2 border-surface-container-highest text-left">
                      {([
                        ['title', 'NỘI DUNG'],
                        ['type', 'PHÂN LOẠI'],
                        ['stage', 'GIAI ĐOẠN'],
                        ['detail', 'THÔNG TIN'],
                        ['submittedAt', 'GỬI DUYỆT'],
                      ] as const).map(([key, label]) => (
                        <SortableTableHeader
                          key={key}
                          label={label}
                          active={sortKey === key}
                          direction={sortDirection}
                          onClick={() => changeSort(key)}
                        />
                      ))}
                      <th scope="col" className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">THAO TÁC</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedItems.map((entry) => {
                      const workingKey = `${entry.kind}-${entry.id}`;
                      return (
                        <tr key={workingKey} className="border-b border-surface-container-highest hover:bg-surface-bright">
                          <td className="py-3.5 px-2 max-w-[340px]">
                            <div className="font-semibold text-sm text-on-surface">{entry.title}</div>
                            <div className="text-xs text-outline mt-0.5">{entry.kind === 'CHECKLIST' ? 'Mẫu checklist' : 'Nội dung thư viện'}</div>
                          </td>
                          <td className="py-3.5 px-2">
                            <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                              {entry.typeLabel}
                            </span>
                          </td>
                          <td className="py-3.5 px-2 text-[13px] text-on-surface-variant whitespace-nowrap">{entry.stageLabel}</td>
                          <td className="py-3.5 px-2 text-[13px] text-on-surface-variant">{entry.detail}</td>
                          <td className="py-3.5 px-2 text-[13px] text-outline whitespace-nowrap">{formatDateTime(entry.submittedAt)}</td>
                          <td className="py-3.5 px-2">
                            <div className="flex items-center gap-1.5 justify-end">
                              <button
                                type="button"
                                onClick={() => navigate(DETAIL_PATH[entry.kind](entry.id))}
                                className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-transparent cursor-pointer text-xs font-semibold text-primary flex items-center gap-1 hover:bg-surface-container-low"
                                title="Xem chi tiết"
                              >
                                <span className="material-symbols-outlined text-base">visibility</span>
                                Xem
                              </button>
                              <button
                                type="button"
                                disabled={working === workingKey || isBatchPublishing}
                                onClick={() => openDecision(entry, 'APPROVE')}
                                className="h-8 py-1 px-4 rounded-full bg-primary text-on-primary border-0 text-xs font-semibold cursor-pointer flex items-center gap-1 hover:bg-primary/90 disabled:opacity-50"
                              >
                                <span className="material-symbols-outlined text-base">publish</span>
                                Xuất bản
                              </button>
                              <button
                                type="button"
                                disabled={working === workingKey || isBatchPublishing}
                                onClick={() => openDecision(entry, 'REJECT')}
                                className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-surface text-on-surface-variant text-xs font-semibold cursor-pointer flex items-center gap-1 hover:bg-surface-container-low disabled:opacity-50"
                              >
                                <span className="material-symbols-outlined text-base">undo</span>
                                Trả nháp
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
                <span className="text-[13px] text-outline">
                  Hiển thị {filteredItems.length === 0 ? 0 : pageStart}-{pageEnd} trong {filteredItems.length} kết quả
                </span>
                <div className="flex gap-1">
                  <button
                    type="button"
                    disabled={currentPage === 0}
                    onClick={() => setPage((value) => Math.max(0, value - 1))}
                    className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${currentPage === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                  >
                    <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
                  </button>
                  {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                    const startPage = Math.max(0, Math.min(currentPage - 2, totalPages - 5));
                    const p = startPage + i;
                    if (p >= totalPages) return null;
                    return (
                      <button
                        key={p}
                        type="button"
                        onClick={() => setPage(p)}
                        className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${currentPage === p ? 'border-0 bg-primary text-on-primary' : 'border border-outline-variant bg-surface text-on-surface-variant'}`}
                      >
                        {p + 1}
                      </button>
                    );
                  })}
                  <button
                    type="button"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))}
                    className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${currentPage >= totalPages - 1 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                  >
                    <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Single Item Confirm Dialog */}
      <ConfirmDialog
        key={pendingDecision ? `${pendingDecision.entry.kind}-${pendingDecision.entry.id}-${pendingDecision.decision}` : 'single-none'}
        open={pendingDecision !== null}
        title={dialogTitle}
        description={pendingDecision
          ? `${dialogDescription} ${checklistDialogGuidance ? `${checklistDialogGuidance} ` : ''}Mục: "${pendingDecision.entry.title}".`
          : undefined}
        icon={pendingDecision?.decision === 'APPROVE' ? 'publish' : 'assignment_return'}
        tone={pendingDecision?.decision === 'APPROVE' ? 'default' : 'danger'}
        confirmLabel={pendingDecision?.decision === 'APPROVE' ? 'Xuất bản' : 'Trả về nháp'}
        reasonLabel={pendingDecision?.decision === 'REJECT' ? 'Lý do trả về nháp (bắt buộc)' : undefined}
        reasonPlaceholder="Nêu rõ phần cần chỉnh sửa để Content Admin xử lý..."
        submitting={pendingDecision !== null && working === `${pendingDecision.entry.kind}-${pendingDecision.entry.id}`}
        errorText={dialogError}
        onConfirm={confirmDecision}
        onCancel={() => setPendingDecision(null)}
      />

      {/* Batch Publish Confirm Dialog */}
      <ConfirmDialog
        key={batchTarget ? `batch-${batchTarget}` : 'batch-none'}
        open={batchTarget !== null}
        title={batchDialogTitle}
        description={
          matchingBatchItems.length === 0
            ? 'Không có mục nào thuộc phân loại này đang chờ phê duyệt.'
            : `Bạn có chắc chắn muốn xuất bản ${selectedBatchItemIds.size}/${matchingBatchItems.length} ${batchTarget ? batchTargetLabelMap[batchTarget] : ''} đã chọn không?`
        }
        icon="publish"
        tone="default"
        confirmLabel={
          matchingBatchItems.length === 0
            ? 'Đóng'
            : selectedBatchItemIds.size > 0
              ? `Xuất bản (${selectedBatchItemIds.size} mục)`
              : 'Chọn ít nhất 1 mục'
        }
        submitting={isBatchPublishing}
        errorText={batchError}
        onConfirm={selectedBatchItemIds.size > 0 ? confirmBatchPublish : () => setBatchTarget(null)}
        onCancel={() => setBatchTarget(null)}
      >
        {matchingBatchItems.length > 0 && (
          <div className="mt-4">
            <button
              type="button"
              onClick={() => setIsBatchListExpanded((prev) => !prev)}
              className="w-full py-2.5 px-3.5 rounded-xl border border-outline-variant bg-surface-container-low hover:bg-surface-container flex items-center justify-between text-xs font-semibold text-on-surface cursor-pointer font-sans transition-colors"
            >
              <span className="flex items-center gap-2">
                <span className="material-symbols-outlined text-base text-primary">list_alt</span>
                <span>
                  Danh sách mục chờ xuất bản ({selectedBatchItemIds.size}/{matchingBatchItems.length} đã chọn)
                </span>
              </span>
              <span className="material-symbols-outlined text-base text-outline">
                {isBatchListExpanded ? 'expand_less' : 'expand_more'}
              </span>
            </button>

            {isBatchListExpanded && (
              <div className="mt-2.5 max-h-56 overflow-y-auto rounded-xl border border-outline-variant bg-surface p-2 space-y-1">
                <div className="flex items-center justify-between px-2.5 py-1.5 border-b border-surface-container-highest text-xs font-semibold text-outline">
                  <button
                    type="button"
                    onClick={toggleSelectAllBatch}
                    className="text-primary cursor-pointer hover:underline bg-transparent border-0 p-0 text-xs font-semibold font-sans"
                  >
                    {selectedBatchItemIds.size === matchingBatchItems.length ? 'Bỏ chọn tất cả' : 'Chọn tất cả'}
                  </button>
                  <span>
                    Đã chọn {selectedBatchItemIds.size} / {matchingBatchItems.length}
                  </span>
                </div>

                {matchingBatchItems.map((item) => {
                  const key = `${item.kind}-${item.id}`;
                  const isChecked = selectedBatchItemIds.has(key);
                  return (
                    <label
                      key={key}
                      className="flex items-center justify-between p-2.5 rounded-lg hover:bg-surface-container-low cursor-pointer transition-colors"
                    >
                      <div className="flex flex-col flex-1 pr-3 max-w-[85%]">
                        <span className="text-xs font-medium text-on-surface line-clamp-1">
                          {item.title}
                        </span>
                        <span className="text-[11px] text-outline">
                          {item.typeLabel} · {item.stageLabel}
                        </span>
                      </div>
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => toggleSelectBatchItem(key)}
                        className="w-4 h-4 text-primary rounded border-outline-variant focus:ring-primary/20 cursor-pointer accent-primary"
                      />
                    </label>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </ConfirmDialog>
    </div>
  );
}


