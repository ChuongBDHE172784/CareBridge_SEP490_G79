import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type {
  AdminChecklistTemplate,
  AdminChecklistTemplateDetail,
  ChecklistTemplateStatus,
  ContentStage,
} from '../models/content';
import {
  CHECKLIST_STATUS_LABELS,
  STAGE_LABELS,
  STAGE_OPTIONS,
} from '../models/content';
import { archiveChecklistTemplate, fetchAdminChecklistTemplates } from '../services/contentApi';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';
import { SortableTableHeader, type SortDirection } from '../components/SortableTableHeader';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { nextSortDirection, sortRows } from '../utils/tableSorting';

const PAGE_SIZE = 10;

function stageBadgeClass(stage: ContentStage | null): string {
  switch (stage) {
    case 'PRE_PREGNANCY': return 'bg-emerald-100 text-emerald-800';
    case 'PREGNANCY': return 'bg-amber-100 text-amber-800';
    case 'POSTPARTUM': return 'bg-rose-100 text-rose-800';
    case null: return 'bg-surface-container-low text-on-surface-variant';
  }
}

function statusBadgeClass(status: ChecklistTemplateStatus, returned: boolean): string {
  if (returned) return 'bg-error-container text-error';
  switch (status) {
    case 'APPROVED': return 'bg-emerald-100 text-emerald-800';
    case 'REJECTED': return 'bg-error-container text-error';
    case 'PENDING_REVIEW': return 'bg-amber-100 text-amber-800';
    case 'ARCHIVED': return 'bg-surface-container-low text-outline';
    case 'DRAFT': return 'bg-surface-container-low text-on-surface-variant';
  }
}

type ChecklistListRow = AdminChecklistTemplateDetail & Pick<Partial<AdminChecklistTemplate>, 'itemCount'>;

const recipientLabel = (role: 'MOTHER' | 'FAMILY') => (role === 'MOTHER' ? 'Mẹ' : 'Gia đình');
const targetLabel = (target: 'MOTHER' | 'BABY') => (target === 'MOTHER' ? 'Mẹ' : 'Em bé');
const warmBadge = 'inline-flex shrink-0 items-center rounded-full bg-surface-container-low px-3 py-1 text-xs font-semibold text-primary';

const STATUS_TABS: { key: string; label: string; status?: ChecklistTemplateStatus }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'approved', label: 'Đã duyệt', status: 'APPROVED' },
  { key: 'pending', label: 'Chờ duyệt', status: 'PENDING_REVIEW' },
  { key: 'draft', label: 'Bản nháp', status: 'DRAFT' },
];

export default function ChecklistListPage() {
  const navigate = useNavigate();
  const [checklists, setChecklists] = useState<ChecklistListRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [archivingId, setArchivingId] = useState<string | null>(null);
  const [stageFilter, setStageFilter] = useState<ContentStage | ''>('');
  const [statusFilter, setStatusFilter] = useState<ChecklistTemplateStatus | ''>('');
  const [searchInput, setSearchInput] = useState('');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [sortKey, setSortKey] = useState<'name' | 'stage' | 'itemCount' | 'status'>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const latestRequestId = useRef(0);
  const archivingIdRef = useRef<string | null>(null);
  const debouncedKeyword = useDebouncedValue(searchInput.trim());

  useEffect(() => {
    setPage(0);
  }, [debouncedKeyword]);

  const loadData = useCallback(async () => {
    const requestId = latestRequestId.current + 1;
    latestRequestId.current = requestId;
    let isCorrectingPage = false;
    setIsLoading(true);
    setError('');
    try {
      const result = await fetchAdminChecklistTemplates({
        stage: stageFilter || undefined,
        status: statusFilter || undefined,
        ...(debouncedKeyword ? { keyword: debouncedKeyword } : {}),
        page,
        size: PAGE_SIZE,
      });
      if (requestId !== latestRequestId.current) return;
      const lastAvailablePage = result.totalPages === 0 ? 0 : result.totalPages - 1;
      if (page > lastAvailablePage) {
        isCorrectingPage = true;
        setPage(lastAvailablePage);
        return;
      }
      setChecklists(result.content);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
    } catch {
      if (requestId !== latestRequestId.current) return;
      setChecklists([]);
      setTotalElements(0);
      setTotalPages(0);
      setError('Không thể tải danh sách checklist. Vui lòng thử lại.');
    } finally {
      if (requestId === latestRequestId.current && !isCorrectingPage) {
        setIsLoading(false);
      }
    }
  }, [page, stageFilter, statusFilter, debouncedKeyword]);

  const latestLoadData = useRef(loadData);
  useEffect(() => {
    latestLoadData.current = loadData;
  }, [loadData]);

  useEffect(() => {
    void loadData();
    return () => {
      latestRequestId.current += 1;
    };
  }, [loadData]);

  useEffect(() => {
    setActionError('');
  }, [page, stageFilter, statusFilter, debouncedKeyword]);

  const handleDelete = async (checklist: ChecklistListRow) => {
    if (checklist.status === 'ARCHIVED' || archivingIdRef.current !== null) return;

    setActionError('');
    const reason = window.prompt(`Nhập lý do xóa (lưu trữ) "${checklist.name}":`);
    if (reason === null) return;
    if (!reason.trim()) {
      setActionError('Vui lòng nhập lý do trước khi xóa.');
      return;
    }

    archivingIdRef.current = checklist.id;
    setArchivingId(checklist.id);
    try {
      await archiveChecklistTemplate(checklist.id, reason.trim());
      await latestLoadData.current();
    } catch {
      setActionError('Không thể xóa checklist. Vui lòng thử lại.');
    } finally {
      archivingIdRef.current = null;
      setArchivingId(null);
    }
  };

  const from = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const to = Math.min((page + 1) * PAGE_SIZE, totalElements);
  const sortedChecklists = useMemo(() => sortRows(checklists, sortDirection, (checklist) => {
    switch (sortKey) {
      case 'name': return checklist.name;
      case 'stage': return checklist.stage ? STAGE_LABELS[checklist.stage] : '';
      case 'itemCount': return checklist.items?.length ?? checklist.itemCount ?? 0;
      case 'status': return checklist.latestReviewFeedback ? 'Cần chỉnh sửa' : CHECKLIST_STATUS_LABELS[checklist.status];
    }
  }), [checklists, sortDirection, sortKey]);

  const changeSort = (key: typeof sortKey) => {
    setSortDirection(nextSortDirection(sortKey, key, sortDirection));
    setSortKey(key);
  };

  return (
    <main data-testid="checklist-list-page" className="min-h-screen bg-background p-8 font-sans">
      {/* Header */}
      <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Quản lý Checklist</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Theo dõi đúng trạng thái duyệt và số mục của từng checklist.
          </p>
        </div>
        <button
          type="button"
          aria-label="Tạo checklist mới"
          onClick={() => navigate('/content/checklists/create')}
          className="inline-flex items-center gap-2 py-2.5 px-6 rounded-full bg-primary text-on-primary text-sm font-semibold shadow-md hover:bg-primary/90 cursor-pointer transition-all active:scale-95 self-start md:self-auto"
        >
          <span aria-hidden="true" className="material-symbols-outlined text-lg">add</span>
          Tạo Checklist
        </button>
      </div>

      {actionError && (
        <div role="alert" className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm font-semibold text-error">
          {actionError}
        </div>
      )}

      {/* Action & Filter Bar */}
      <div className="bg-surface rounded-2xl p-4 shadow-sm border border-surface-container-highest mb-6 flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Status Tab buttons */}
        <div className="flex gap-2 flex-wrap w-full md:w-auto">
          {STATUS_TABS.map((tab) => {
            const isActive = tab.status === undefined ? statusFilter === '' : statusFilter === tab.status;
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => {
                  setStatusFilter(tab.status ?? '');
                  setPage(0);
                }}
                className={`py-2 px-4 rounded-full text-xs font-semibold cursor-pointer transition-all ${
                  isActive
                    ? 'bg-primary text-on-primary shadow-sm'
                    : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-bright'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Filters */}
        <div className="flex gap-2 flex-wrap items-center w-full md:w-auto justify-end">
          <div className="relative min-w-[240px] flex-1 sm:flex-none">
            <span aria-hidden="true" className="material-symbols-outlined absolute left-[14px] top-1/2 -translate-y-1/2 text-xl text-outline">search</span>
            <input
              aria-label="Tìm kiếm checklist"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Tìm kiếm checklist..."
              className="w-full rounded-2xl border border-outline-variant bg-surface py-2.5 pl-[42px] pr-[14px] text-sm text-on-surface outline-none font-sans"
            />
          </div>
          <select
            aria-label="Lọc checklist theo giai đoạn"
            value={stageFilter}
            onChange={(event) => {
              setStageFilter(event.target.value as ContentStage | '');
              setPage(0);
            }}
            className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans focus:outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option value="">Tất cả giai đoạn</option>
            {STAGE_OPTIONS.map(({ value, label }) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>

          <select
            aria-label="Lọc checklist theo trạng thái duyệt"
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value as ChecklistTemplateStatus | '');
              setPage(0);
            }}
            className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans focus:outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option value="">Tất cả trạng thái</option>
            {Object.entries(CHECKLIST_STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Data Table */}
      <section
        aria-busy={isLoading}
        aria-live="polite"
        className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest"
      >
        {isLoading ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : error ? (
          <div className="flex flex-col items-center gap-4 py-12 text-center">
            <p role="alert" className="m-0 text-sm font-semibold text-error">{error}</p>
            <button
              type="button"
              onClick={() => void loadData()}
              className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-primary text-on-primary text-sm font-semibold hover:bg-primary/90 cursor-pointer"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-lg">refresh</span>
              Thử lại
            </button>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse">
                <caption className="sr-only">Danh sách checklist quản trị theo trạng thái duyệt thật</caption>
                <thead>
                  <tr className="border-b-2 border-surface-container-highest text-left">
                    <SortableTableHeader label="TIÊU ĐỀ" active={sortKey === 'name'} direction={sortDirection} onClick={() => changeSort('name')} />
                    <th scope="col" className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">NGƯỜI NHẬN</th>
                    <SortableTableHeader label="GIAI ĐOẠN / CỬA SỔ" active={sortKey === 'stage'} direction={sortDirection} onClick={() => changeSort('stage')} />
                    <SortableTableHeader label="MỤC / ĐỐI TƯỢNG" active={sortKey === 'itemCount'} direction={sortDirection} onClick={() => changeSort('itemCount')} />
                    <SortableTableHeader label="TRẠNG THÁI" active={sortKey === 'status'} direction={sortDirection} onClick={() => changeSort('status')} />
                    <th scope="col" className="px-2 py-3 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">THAO TÁC</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedChecklists.map((checklist) => (
                    <tr key={checklist.id} className="border-b border-surface-container-highest hover:bg-surface-bright transition-colors">
                      <td className="py-3.5 px-2 max-w-[340px]">
                        <div className="text-sm font-semibold text-on-surface">{checklist.name}</div>
                        <ReviewFeedbackNotice feedback={checklist.latestReviewFeedback} compact />
                        {checklist.description && (
                          <div className="mt-0.5 line-clamp-2 text-xs text-outline">{checklist.description}</div>
                        )}
                      </td>
                      <td className="py-3.5 px-2">
                        <div className="flex flex-wrap gap-1.5">
                          {(checklist.recipientRoles ?? []).map((role) => (
                            <span key={role} aria-label={`Người nhận: ${recipientLabel(role)}`} className={warmBadge}>
                              {recipientLabel(role)}
                            </span>
                          ))}
                          {(checklist.recipientRoles ?? []).length === 0 && (
                            <span className="text-xs font-medium text-outline">Chưa cấu hình</span>
                          )}
                        </div>
                      </td>
                      <td className="px-2 py-3.5 text-[13px] font-medium text-on-surface-variant">
                        <div className="flex flex-col items-start gap-1.5">
                          <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${stageBadgeClass(checklist.stage)}`}>
                            {checklist.stage ? STAGE_LABELS[checklist.stage] : 'Không áp dụng'}
                          </span>
                          <span className="inline-flex items-center rounded-full bg-surface-container-low px-2.5 py-0.5 text-xs text-on-surface-variant font-medium">{checklist.substage?.code ?? 'Không có cửa sổ'}</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-2">
                        <div className="text-sm font-bold text-on-surface">
                          {checklist.items?.length ?? checklist.itemCount ?? 0}
                          <span className="ml-1 text-xs font-medium text-outline">mục</span>
                        </div>
                        <div className="mt-1.5 flex flex-wrap gap-1.5">
                          {(checklist.items ?? []).slice().sort((a, b) => a.order - b.order).map((item) => (
                            <span
                              key={item.id}
                              aria-label={`Mục ${item.order}: ${targetLabel(item.targetSubject)}`}
                              className="inline-flex items-center rounded-full bg-surface-container-low px-2.5 py-0.5 text-xs font-medium text-primary"
                            >
                              Mục {item.order} · {targetLabel(item.targetSubject)}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="py-3.5 px-2 whitespace-nowrap">
                        <span className={`inline-flex items-center whitespace-nowrap py-1 px-3 rounded-full text-xs font-semibold ${statusBadgeClass(checklist.status, Boolean(checklist.latestReviewFeedback))}`}>
                          {checklist.latestReviewFeedback ? 'Cần chỉnh sửa' : CHECKLIST_STATUS_LABELS[checklist.status]}
                        </span>
                      </td>
                      <td className="py-3.5 px-2 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <button
                            type="button"
                            aria-label={`Xem checklist ${checklist.name}`}
                            onClick={() => navigate(`/content/checklists/${checklist.id}`)}
                              className="min-h-12 min-w-12 py-2.5 px-3 rounded-full border border-outline-variant bg-transparent cursor-pointer text-xs font-semibold text-primary flex items-center justify-center gap-1 hover:bg-surface-container-low"
                            title="Xem chi tiết"
                          >
                            <span aria-hidden="true" className="material-symbols-outlined text-base">visibility</span>
                            Xem
                          </button>
                          <button
                            type="button"
                            aria-label={`Chỉnh sửa checklist ${checklist.name}`}
                            onClick={() => navigate(`/content/checklists/${checklist.id}/edit`)}
                            disabled={checklist.status !== 'DRAFT' && checklist.status !== 'PENDING_REVIEW'}
                              className="min-h-12 min-w-12 py-2.5 px-3 rounded-full border border-outline-variant bg-surface text-on-surface-variant text-xs font-semibold cursor-pointer flex items-center justify-center gap-1 hover:bg-surface-container-low disabled:opacity-40"
                            title={checklist.status === 'DRAFT' || checklist.status === 'PENDING_REVIEW'
                              ? 'Chỉnh sửa'
                              : 'Checklist ở trạng thái này không thể chỉnh sửa trực tiếp'}
                          >
                            <span aria-hidden="true" className="material-symbols-outlined text-base">edit</span>
                            Sửa
                          </button>
                          <button
                            type="button"
                            aria-label={`Xóa checklist ${checklist.name}`}
                            onClick={() => void handleDelete(checklist)}
                            disabled={checklist.status === 'ARCHIVED' || archivingId !== null}
                              className="min-h-12 min-w-12 py-2.5 px-3 rounded-full border border-error-container bg-surface text-error text-xs font-semibold cursor-pointer flex items-center justify-center gap-1 hover:bg-error-container/20 disabled:opacity-40"
                            title={checklist.status === 'ARCHIVED'
                              ? 'Checklist đã xóa không thể xóa lại'
                              : archivingId === checklist.id
                                ? 'Đang xóa...'
                                : archivingId !== null ? 'Đang xử lý checklist khác...' : 'Xóa'}
                          >
                            <span aria-hidden="true" className="material-symbols-outlined text-base">delete</span>
                            Xóa
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {checklists.length === 0 && (
                    <tr>
                      <td colSpan={6} className="py-12 text-center text-sm text-outline">Không có checklist phù hợp.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            <footer className="mt-5 flex flex-col gap-4 border-t border-surface-container-highest pt-4 sm:flex-row sm:items-center sm:justify-between">
              <span className="text-[13px] text-outline">Hiển thị {from}-{to} trên {totalElements} kết quả</span>
              <nav aria-label="Phân trang checklist" className="flex items-center gap-2">
                <button
                  type="button"
                  aria-label="Trang checklist trước"
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  disabled={page === 0}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer hover:bg-surface-container-low'}`}
                >
                  <span aria-hidden="true" className="material-symbols-outlined text-primary text-lg">chevron_left</span>
                </button>
                <span className="min-w-24 text-center text-sm font-semibold text-on-surface">Trang {totalPages === 0 ? 0 : page + 1}/{totalPages}</span>
                <button
                  type="button"
                  aria-label="Trang checklist sau"
                  onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                  disabled={totalPages === 0 || page >= totalPages - 1}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${totalPages === 0 || page >= totalPages - 1 ? 'opacity-40 cursor-default' : 'cursor-pointer hover:bg-surface-container-low'}`}
                >
                  <span aria-hidden="true" className="material-symbols-outlined text-primary text-lg">chevron_right</span>
                </button>
              </nav>
            </footer>
          </>
        )}
      </section>
    </main>
  );
}
