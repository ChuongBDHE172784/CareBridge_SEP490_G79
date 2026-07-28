import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import {
  decideChecklistTemplate,
  decideContent,
  fetchAdminChecklists,
  fetchStaffContentList,
} from '../services/contentApi';
import type { AdminChecklistTemplate, ContentDetail, ContentStage, ContentType } from '../models/content';
import { STAGE_LABELS, TYPE_LABELS } from '../models/content';

type QueueKind = 'CONTENT' | 'CHECKLIST';
type TypeFilter = 'ALL' | ContentType;

type QueueEntry = {
  kind: QueueKind;
  id: string;
  title: string;
  type: ContentType;
  typeLabel: string;
  stage: ContentStage;
  stageLabel: string;
  detail: string;
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

function toChecklistEntry(item: AdminChecklistTemplate): QueueEntry | null {
  if (!item.stage) return null;
  const typeLabel = TYPE_LABELS.CHECKLIST;
  const stageLabel = STAGE_LABELS[item.stage];
  const detail = `Phiên bản ${item.versionNo} · ${item.itemCount} mục`;

  return {
    kind: 'CHECKLIST',
    id: item.id,
    title: item.name,
    type: 'CHECKLIST',
    typeLabel,
    stage: item.stage,
    stageLabel,
    detail,
    submittedAt: item.updatedAt,
    searchText: [item.name, typeLabel, stageLabel, detail, item.description].join(' ').toLowerCase(),
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

  const load = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const [contentPage, checklistPage] = await Promise.all([
        fetchStaffContentList({ status: 'PENDING_REVIEW', size: 50 }),
        fetchAdminChecklists({ status: 'PENDING_REVIEW', size: 50 }),
      ]);
      setItems([
        ...contentPage.content.map(toContentEntry),
        ...checklistPage.content.map(toChecklistEntry).filter((entry): entry is QueueEntry => entry !== null),
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

  const filteredItems = useMemo(() => {
    const query = search.trim().toLowerCase();
    return items.filter((item) => {
      const matchesSearch = query.length === 0 || item.searchText.includes(query);
      const matchesType = typeFilter === 'ALL' || item.type === typeFilter;
      const matchesStage = stageFilter === 'ALL' || item.stage === stageFilter;
      return matchesSearch && matchesType && matchesStage;
    });
  }, [items, search, stageFilter, typeFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / pageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const pagedItems = filteredItems.slice(currentPage * pageSize, currentPage * pageSize + pageSize);
  const pageStart = filteredItems.length === 0 ? 0 : currentPage * pageSize + 1;
  const pageEnd = Math.min((currentPage + 1) * pageSize, filteredItems.length);

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
    } catch {
      setDialogError(
        decision === 'APPROVE'
          ? 'Không thể xuất bản mục này. Vui lòng thử lại.'
          : 'Không thể trả mục này về nháp. Vui lòng thử lại.',
      );
    } finally {
      setWorking(null);
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
          <button
            type="button"
            onClick={() => void load()}
            disabled={isLoading}
            className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50 self-start md:self-auto"
          >
            <span className="material-symbols-outlined text-lg">refresh</span>
            Làm mới
          </button>
        </div>

        {/* Stats Bar */}
        <div className="mb-6 grid gap-4 md:grid-cols-5">
          {[
            { label: 'Tổng chờ duyệt', value: stats.total, icon: 'pending_actions' },
            { label: 'Bài viết / FAQ', value: stats.content, icon: 'article' },
            { label: 'Checklist', value: stats.checklist, icon: 'checklist' },
            { label: 'Thai kỳ', value: stats.pregnancy, icon: 'pregnant_woman' },
            { label: 'Sau sinh', value: stats.postpartum, icon: 'family_restroom' },
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
                {Object.entries(STAGE_LABELS).map(([value, label]) => (
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
                      {['NỘI DUNG', 'PHÂN LOẠI', 'GIAI ĐOẠN', 'THÔNG TIN', 'GỬI DUYỆT', 'THAO TÁC'].map((heading) => (
                        <th key={heading} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{heading}</th>
                      ))}
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
                                disabled={working === workingKey}
                                onClick={() => openDecision(entry, 'APPROVE')}
                                className="h-8 py-1 px-4 rounded-full bg-primary text-on-primary border-0 text-xs font-semibold cursor-pointer flex items-center gap-1 hover:bg-primary/90 disabled:opacity-50"
                              >
                                <span className="material-symbols-outlined text-base">publish</span>
                                Xuất bản
                              </button>
                              <button
                                type="button"
                                disabled={working === workingKey}
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

      <ConfirmDialog
        key={pendingDecision ? `${pendingDecision.entry.kind}-${pendingDecision.entry.id}-${pendingDecision.decision}` : 'none'}
        open={pendingDecision !== null}
        title={dialogTitle}
        description={pendingDecision ? `${dialogDescription} Mục: "${pendingDecision.entry.title}".` : undefined}
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
    </div>
  );
}
