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
    <div className="portal-page px-5 py-5 md:px-6 md:py-6">
      <div className="portal-contained">
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">Quản trị nội dung</p>
            <h1 className="portal-title">Hàng đợi phê duyệt</h1>
            <p className="portal-subtitle max-w-3xl">
              Bảng này gom bài viết, FAQ và checklist đang chờ System Admin quyết định. Dùng bộ lọc để tách theo loại nội dung, giai đoạn chăm sóc và tìm nhanh theo tiêu đề.
            </p>
          </div>
          <button type="button" onClick={() => void load()} className="portal-secondary-button" disabled={isLoading}>
            <span className="material-symbols-outlined text-base">refresh</span>
            Làm mới
          </button>
        </div>

        <section className="mb-5 grid gap-3 md:grid-cols-5">
          {[
            { label: 'Tổng chờ duyệt', value: stats.total, icon: 'pending_actions' },
            { label: 'Bài viết / FAQ', value: stats.content, icon: 'article' },
            { label: 'Checklist', value: stats.checklist, icon: 'checklist' },
            { label: 'Thai kỳ', value: stats.pregnancy, icon: 'pregnant_woman' },
            { label: 'Sau sinh', value: stats.postpartum, icon: 'family_restroom' },
          ].map((stat) => (
            <div key={stat.label} className="portal-card-padded">
              <div className="flex items-center justify-between gap-3">
                <span className="text-xs font-semibold text-on-surface-variant">{stat.label}</span>
                <span className="material-symbols-outlined text-[18px] text-outline">{stat.icon}</span>
              </div>
              <p className="portal-metric mt-2">{stat.value}</p>
            </div>
          ))}
        </section>

        <section className="portal-card-padded mb-4">
          <div className="grid gap-3 lg:grid-cols-[1.3fr_0.8fr_0.8fr_0.5fr_auto] lg:items-end">
            <label>
              <span className="portal-label">Tìm kiếm</span>
              <div className="relative">
                <span className="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-outline">search</span>
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  className="portal-field w-full pl-9"
                  placeholder="Tìm theo tiêu đề, loại, giai đoạn..."
                />
              </div>
            </label>
            <label>
              <span className="portal-label">Loại nội dung</span>
              <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value as TypeFilter)} className="portal-field w-full">
                <option value="ALL">Tất cả loại</option>
                <option value="ARTICLE">Bài viết</option>
                <option value="FAQ">FAQ</option>
                <option value="CHECKLIST">Checklist</option>
              </select>
            </label>
            <label>
              <span className="portal-label">Giai đoạn</span>
              <select value={stageFilter} onChange={(event) => setStageFilter(event.target.value as 'ALL' | ContentStage)} className="portal-field w-full">
                <option value="ALL">Tất cả giai đoạn</option>
                {Object.entries(STAGE_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>
            <label>
              <span className="portal-label">Mỗi trang</span>
              <select value={pageSize} onChange={(event) => setPageSize(Number(event.target.value) as typeof pageSize)} className="portal-field w-full">
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>{size}</option>
                ))}
              </select>
            </label>
            <button type="button" onClick={resetFilters} className="portal-secondary-button">
              <span className="material-symbols-outlined text-base">filter_alt_off</span>
              Xóa lọc
            </button>
          </div>
        </section>

        {error && <div className="portal-error mb-4">{error}</div>}

        <section className="portal-table-card">
          <div className="flex flex-col gap-2 border-b border-outline-variant/70 p-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h2 className="text-sm font-semibold text-on-surface">Danh sách chờ quyết định</h2>
              <p className="mt-1 text-xs text-on-surface-variant">
                Hiển thị {pageStart}-{pageEnd} trong {filteredItems.length} mục phù hợp.
              </p>
            </div>
            <span className="rounded-md bg-surface-container-low px-2.5 py-1 text-xs font-semibold text-on-surface-variant">
              Trạng thái: Chờ phê duyệt
            </span>
          </div>

          {isLoading ? (
            <div className="portal-empty m-4">Đang tải hàng đợi phê duyệt...</div>
          ) : filteredItems.length === 0 ? (
            <div className="portal-empty m-4">
              {items.length === 0 ? 'Không có nội dung chờ duyệt.' : 'Không có mục nào khớp bộ lọc hiện tại.'}
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[1080px]">
                  <thead>
                    <tr>
                      <th>Nội dung</th>
                      <th>Phân loại</th>
                      <th>Giai đoạn</th>
                      <th>Thông tin</th>
                      <th>Gửi duyệt</th>
                      <th className="text-right">Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedItems.map((entry) => {
                      const workingKey = `${entry.kind}-${entry.id}`;
                      return (
                        <tr key={workingKey}>
                          <td className="max-w-[360px]">
                            <div className="font-semibold text-on-surface">{entry.title}</div>
                            <div className="mt-1 text-[11px] text-outline">{entry.kind === 'CHECKLIST' ? 'Mẫu checklist' : 'Nội dung thư viện'}</div>
                          </td>
                          <td>
                            <span className="rounded-md bg-primary-container px-2.5 py-1 text-xs font-semibold text-primary">
                              {entry.typeLabel}
                            </span>
                          </td>
                          <td className="whitespace-nowrap text-on-surface-variant">{entry.stageLabel}</td>
                          <td className="text-on-surface-variant">{entry.detail}</td>
                          <td className="whitespace-nowrap text-on-surface-variant">{formatDateTime(entry.submittedAt)}</td>
                          <td>
                            <div className="flex justify-end gap-2">
                              <button
                                type="button"
                                onClick={() => navigate(DETAIL_PATH[entry.kind](entry.id))}
                                className="portal-secondary-button h-8 whitespace-nowrap"
                              >
                                Xem
                              </button>
                              <button
                                type="button"
                                disabled={working === workingKey}
                                onClick={() => openDecision(entry, 'APPROVE')}
                                className="portal-primary-button h-8 whitespace-nowrap disabled:opacity-50"
                              >
                                Xuất bản
                              </button>
                              <button
                                type="button"
                                disabled={working === workingKey}
                                onClick={() => openDecision(entry, 'REJECT')}
                                className="portal-secondary-button h-8 whitespace-nowrap disabled:opacity-50"
                              >
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

              <div className="flex flex-col gap-3 border-t border-outline-variant/70 p-4 md:flex-row md:items-center md:justify-between">
                <p className="text-xs text-on-surface-variant">
                  Trang {currentPage + 1} / {totalPages}
                </p>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setPage((value) => Math.max(0, value - 1))}
                    disabled={currentPage === 0}
                    className="portal-secondary-button"
                  >
                    <span className="material-symbols-outlined text-base">chevron_left</span>
                    Trước
                  </button>
                  <button
                    type="button"
                    onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))}
                    disabled={currentPage >= totalPages - 1}
                    className="portal-secondary-button"
                  >
                    Sau
                    <span className="material-symbols-outlined text-base">chevron_right</span>
                  </button>
                </div>
              </div>
            </>
          )}
        </section>
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
