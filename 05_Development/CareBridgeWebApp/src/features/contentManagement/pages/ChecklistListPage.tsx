import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type {
  AdminChecklistTemplate,
  ChecklistTemplateStatus,
  ContentStage,
} from '../models/content';
import {
  CHECKLIST_STATUS_LABELS,
  STAGE_LABELS,
} from '../models/content';
import { archiveChecklistTemplate, fetchAdminChecklists } from '../services/contentApi';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';
import { SortableTableHeader, type SortDirection } from '../components/SortableTableHeader';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { nextSortDirection, sortRows } from '../utils/tableSorting';

const PAGE_SIZE = 10;

function stageBadgeClass(stage: ContentStage | null): string {
  switch (stage) {
    case 'PRE_PREGNANCY': return 'bg-surface-container-high text-on-surface-variant';
    case 'PREGNANCY': return 'bg-secondary-container text-on-secondary-container';
    case 'POSTPARTUM': return 'bg-[#E6F4EA] text-[#137333]';
    case 'BABY_CARE': return 'bg-[#FFF3E0] text-[#E65100]';
    case null: return 'bg-surface-container-high text-on-surface-variant';
  }
}

function statusBadgeClass(status: ChecklistTemplateStatus, returned: boolean): string {
  if (returned) return 'bg-error-container text-error';
  switch (status) {
    case 'APPROVED': return 'bg-[#E6F4EA] text-[#137333]';
    case 'REJECTED': return 'bg-[#FDE8E5] text-[#9F3A32]';
    case 'PENDING_REVIEW': return 'bg-[#FFF3E0] text-[#E65100]';
    case 'ARCHIVED': return 'bg-[#F5F5F5] text-[#616161]';
    case 'DRAFT': return 'bg-surface-container-highest text-primary';
  }
}

function formatUpdatedAt(value: string | null): string {
  if (!value) return 'Chưa cập nhật';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? 'Chưa cập nhật'
    : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(date);
}

const STATUS_TABS: { key: string; label: string; status?: ChecklistTemplateStatus }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'approved', label: 'Đã duyệt', status: 'APPROVED' },
  { key: 'pending', label: 'Chờ duyệt', status: 'PENDING_REVIEW' },
  { key: 'draft', label: 'Bản nháp', status: 'DRAFT' },
];

export default function ChecklistListPage() {
  const navigate = useNavigate();
  const [checklists, setChecklists] = useState<AdminChecklistTemplate[]>([]);
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
  const [sortKey, setSortKey] = useState<'name' | 'stage' | 'itemCount' | 'status' | 'updatedAt'>('updatedAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
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
      const result = await fetchAdminChecklists({
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

  const handleDelete = async (checklist: AdminChecklistTemplate) => {
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
      case 'itemCount': return checklist.itemCount;
      case 'status': return checklist.latestReviewFeedback ? 'Cần chỉnh sửa' : CHECKLIST_STATUS_LABELS[checklist.status];
      case 'updatedAt': {
        const timestamp = new Date(checklist.updatedAt ?? 0).getTime();
        return Number.isNaN(timestamp) ? 0 : timestamp;
      }
    }
  }), [checklists, sortDirection, sortKey]);

  const changeSort = (key: typeof sortKey) => {
    setSortDirection(nextSortDirection(sortKey, key, sortDirection));
    setSortKey(key);
  };

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6 flex-col md:flex-row gap-4">
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
          className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer whitespace-nowrap self-start md:self-auto"
        >
          <span aria-hidden="true" className="material-symbols-outlined text-lg">add</span>
          Tạo Checklist
        </button>
      </div>

      {actionError && (
        <div role="alert" className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">
          {actionError}
        </div>
      )}

      {/* Status tabs + Filter controls */}
      <div className="flex justify-between items-center mb-5 flex-wrap gap-3">
        {/* Status Tab buttons */}
        <div className="flex gap-2 flex-wrap">
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
                className={`flex items-center gap-1.5 py-2 px-[18px] rounded-full text-[13px] font-semibold cursor-pointer transition-colors ${
                  isActive
                    ? 'border-2 border-primary bg-surface-container-low text-primary'
                    : 'border border-outline-variant bg-transparent text-on-surface-variant hover:bg-surface-container-low'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Filters */}
        <div className="flex gap-2 flex-wrap items-center">
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
            className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
          >
            <option value="">Tất cả giai đoạn</option>
            <option value="PRE_PREGNANCY">Chuẩn bị</option>
            <option value="PREGNANCY">Thai kỳ</option>
            <option value="POSTPARTUM">Sau sinh</option>
            <option value="BABY_CARE">Chăm bé</option>
          </select>

          <select
            aria-label="Lọc checklist theo trạng thái duyệt"
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value as ChecklistTemplateStatus | '');
              setPage(0);
            }}
            className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
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
              className="rounded-full bg-primary px-5 py-2 text-sm font-semibold text-on-primary border-0 cursor-pointer"
            >
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
                    {[
                      ['name', 'TIÊU ĐỀ'],
                      ['stage', 'GIAI ĐOẠN'],
                      ['itemCount', 'SỐ MỤC'],
                      ['status', 'TRẠNG THÁI'],
                      ['updatedAt', 'CẬP NHẬT'],
                    ].map(([key, label]) => (
                      <SortableTableHeader
                        key={key}
                        label={label}
                        active={sortKey === key}
                        direction={sortDirection}
                        onClick={() => changeSort(key as typeof sortKey)}
                      />
                    ))}
                    <th scope="col" className="px-2 py-3 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">THAO TÁC</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedChecklists.map((checklist) => (
                    <tr key={checklist.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                      <td className="py-3.5 px-2 max-w-[340px]">
                        <div className="font-semibold text-sm text-on-surface">{checklist.name}</div>
                        <ReviewFeedbackNotice feedback={checklist.latestReviewFeedback} compact />
                        {checklist.description && (
                          <div className="mt-0.5 line-clamp-2 text-xs text-on-surface-variant">{checklist.description}</div>
                        )}
                      </td>
                      <td className="py-3.5 px-2 text-[13px] text-on-surface-variant font-medium">
                        <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${stageBadgeClass(checklist.stage)}`}>
                          {checklist.stage ? STAGE_LABELS[checklist.stage] : 'Không xác định'}
                        </span>
                      </td>
                      <td className="py-3.5 px-2">
                        <span className="font-bold text-sm text-on-surface">{checklist.itemCount}</span>
                        <span className="ml-1 text-xs text-on-surface-variant font-medium">mục</span>
                      </td>
                      <td className="py-3.5 px-2">
                        <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${statusBadgeClass(checklist.status, Boolean(checklist.latestReviewFeedback))}`}>
                          {checklist.latestReviewFeedback ? 'Cần chỉnh sửa' : CHECKLIST_STATUS_LABELS[checklist.status]}
                        </span>
                      </td>
                      <td className="py-3.5 px-2 text-[13px] text-outline">{formatUpdatedAt(checklist.updatedAt)}</td>
                      <td className="py-3.5 px-2">
                        <div className="flex gap-1">
                          <button
                            type="button"
                            aria-label={`Xem checklist ${checklist.name}`}
                            onClick={() => navigate(`/content/checklists/${checklist.id}`)}
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center hover:bg-surface-container-low"
                            title="Xem chi tiết"
                          >
                            <span aria-hidden="true" className="material-symbols-outlined text-base">visibility</span>
                          </button>
                          <button
                            type="button"
                            aria-label={`Chỉnh sửa checklist ${checklist.name}`}
                            onClick={() => navigate(`/content/checklists/${checklist.id}/edit`)}
                            disabled={checklist.status !== 'DRAFT' && checklist.status !== 'PENDING_REVIEW'}
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center hover:bg-surface-container-low disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent"
                            title={checklist.status === 'DRAFT' || checklist.status === 'PENDING_REVIEW'
                              ? 'Chỉnh sửa'
                              : 'Checklist ở trạng thái này không thể chỉnh sửa trực tiếp'}
                          >
                            <span aria-hidden="true" className="material-symbols-outlined text-primary text-base">edit</span>
                          </button>
                          <button
                            type="button"
                            aria-label={`Xóa checklist ${checklist.name}`}
                            onClick={() => void handleDelete(checklist)}
                            disabled={checklist.status === 'ARCHIVED' || archivingId !== null}
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center hover:bg-surface-container-low disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent"
                            title={checklist.status === 'ARCHIVED'
                              ? 'Checklist đã xóa không thể xóa lại'
                              : archivingId === checklist.id
                                ? 'Đang xóa...'
                                : archivingId !== null ? 'Đang xử lý checklist khác...' : 'Xóa'}
                          >
                            <span aria-hidden="true" className="material-symbols-outlined text-error text-base">delete</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {checklists.length === 0 && (
                    <tr>
                      <td colSpan={6} className="py-12 text-center text-outline text-sm">Không có checklist phù hợp.</td>
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
                  className="w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center text-primary disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
                >
                  <span aria-hidden="true" className="material-symbols-outlined text-lg">chevron_left</span>
                </button>
                <span className="min-w-24 text-center text-sm font-semibold text-on-surface">Trang {totalPages === 0 ? 0 : page + 1}/{totalPages}</span>
                <button
                  type="button"
                  aria-label="Trang checklist sau"
                  onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                  disabled={totalPages === 0 || page >= totalPages - 1}
                  className="w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center text-primary disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
                >
                  <span aria-hidden="true" className="material-symbols-outlined text-lg">chevron_right</span>
                </button>
              </nav>
            </footer>
          </>
        )}
      </section>
    </div>
  );
}
