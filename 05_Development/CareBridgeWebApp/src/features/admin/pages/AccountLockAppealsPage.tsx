import { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import type { AccountLockAppeal, AccountLockAppealStatus } from '../models/adminUser';
import { getAccountLockAppeals } from '../services/adminUserApi';
import { SortableTableHeader, type SortDirection } from '../../contentManagement/components/SortableTableHeader';
import { nextSortDirection, sortRows } from '../../contentManagement/utils/tableSorting';

const STATUS_TABS: Array<{ value: AccountLockAppealStatus; label: string; icon: string }> = [
  { value: 'PENDING', label: 'Chờ xử lý', icon: 'pending_actions' },
  { value: 'APPROVED', label: 'Đã duyệt', icon: 'lock_open' },
  { value: 'REJECTED', label: 'Từ chối', icon: 'block' },
  { value: 'CANCELLED', label: 'Đã đóng', icon: 'cancel' },
];

type AppealSortKey = 'userName' | 'lockReason' | 'reason' | 'submittedAt';

function timeAgo(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return 'Vừa xong';
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
}

export default function AccountLockAppealsPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<AccountLockAppealStatus>('PENDING');
  const [appeals, setAppeals] = useState<AccountLockAppeal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Search filter state
  const [searchQuery, setSearchQuery] = useState('');

  // Pagination states
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);

  // Sorting state (default: submittedAt desc - mới nhất lên đầu)
  const [sortKey, setSortKey] = useState<AppealSortKey>('submittedAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  // General statistics counters
  const [stats, setStats] = useState<{
    pending: number;
    approved: number;
    rejected: number;
    cancelled: number;
  }>({ pending: 0, approved: 0, rejected: 0, cancelled: 0 });

  const loadStats = useCallback(async () => {
    try {
      const [pendingRes, approvedRes, rejectedRes, cancelledRes] = await Promise.all([
        getAccountLockAppeals('PENDING', 0, 1),
        getAccountLockAppeals('APPROVED', 0, 1),
        getAccountLockAppeals('REJECTED', 0, 1),
        getAccountLockAppeals('CANCELLED', 0, 1),
      ]);
      setStats({
        pending: pendingRes.totalElements,
        approved: approvedRes.totalElements,
        rejected: rejectedRes.totalElements,
        cancelled: cancelledRes.totalElements,
      });
    } catch {
      // fallback silently
    }
  }, []);

  const loadAppeals = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Fetch maximum available items for client-side realtime filtering and sorting
      const result = await getAccountLockAppeals(status, 0, 100);
      setAppeals(result.content);
    } catch {
      setError('Không thể tải danh sách khiếu nại. Vui lòng thử lại.');
      setAppeals([]);
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    void loadStats();
  }, [loadStats]);

  useEffect(() => {
    void loadAppeals();
  }, [loadAppeals]);

  const handleTabChange = (newStatus: AccountLockAppealStatus) => {
    setStatus(newStatus);
    setSearchQuery('');
    setPage(0);
  };

  // Realtime search filtering by userName, userEmail, lockReason, or reason
  const filteredAppeals = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return appeals;
    return appeals.filter((item) => {
      const nameMatch = (item.userName || '').toLowerCase().includes(q);
      const emailMatch = (item.userEmail || '').toLowerCase().includes(q);
      const lockReasonMatch = (item.lockReason || '').toLowerCase().includes(q);
      const reasonMatch = (item.reason || '').toLowerCase().includes(q);
      return nameMatch || emailMatch || lockReasonMatch || reasonMatch;
    });
  }, [appeals, searchQuery]);

  // Sorting logic on filtered results
  const sortedAppeals = useMemo(() => {
    return sortRows(filteredAppeals, sortDirection, (item) => {
      switch (sortKey) {
        case 'userName':
          return item.userName || item.userEmail || '';
        case 'lockReason':
          return item.lockReason || '';
        case 'reason':
          return item.reason || '';
        case 'submittedAt': {
          const timestamp = new Date(item.submittedAt || 0).getTime();
          return Number.isNaN(timestamp) ? 0 : timestamp;
        }
      }
    });
  }, [filteredAppeals, sortDirection, sortKey]);

  // Paged items for current page
  const pagedAppeals = useMemo(() => {
    const start = page * pageSize;
    return sortedAppeals.slice(start, start + pageSize);
  }, [sortedAppeals, page, pageSize]);

  const changeSort = (key: AppealSortKey) => {
    setSortDirection(nextSortDirection(sortKey, key, sortDirection));
    setSortKey(key);
  };

  const totalElements = filteredAppeals.length;
  const totalPages = Math.ceil(totalElements / pageSize);
  const pageStart = totalElements === 0 ? 0 : page * pageSize + 1;
  const pageEnd = Math.min((page + 1) * pageSize, totalElements);

  return (
    <div className="p-6 md:p-8 font-sans">
      {/* Header */}
      <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Khiếu nại khóa tài khoản</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Xem xét và giải quyết các yêu cầu mở khóa tài khoản từ người dùng bị System Admin tạm dừng truy cập.
          </p>
        </div>
        <div className="flex items-center gap-2 self-start md:self-auto">
          <button
            type="button"
            onClick={() => { void loadAppeals(); void loadStats(); }}
            disabled={loading}
            className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-lg">refresh</span>
            Làm mới
          </button>
        </div>
      </div>

      {/* Stats Bar */}
      <div className="mb-6 grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
        <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Chờ xử lý</span>
            <p className="text-2xl font-bold text-amber-700 m-0">{stats.pending}</p>
            <p className="mt-0.5 text-xs text-outline m-0">Cần xem xét</p>
          </div>
          <span className="material-symbols-outlined text-3xl text-amber-600/70">pending_actions</span>
        </div>

        <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Đã chấp thuận</span>
            <p className="text-2xl font-bold text-emerald-700 m-0">{stats.approved}</p>
            <p className="mt-0.5 text-xs text-outline m-0">Đã mở khóa tài khoản</p>
          </div>
          <span className="material-symbols-outlined text-3xl text-emerald-600/70">lock_open</span>
        </div>

        <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Đã từ chối</span>
            <p className="text-2xl font-bold text-error m-0">{stats.rejected}</p>
            <p className="mt-0.5 text-xs text-outline m-0">Giữ nguyên bản án</p>
          </div>
          <span className="material-symbols-outlined text-3xl text-error/70">block</span>
        </div>

        <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Tổng đơn khiếu nại</span>
            <p className="text-2xl font-bold text-primary m-0">
              {stats.pending + stats.approved + stats.rejected + stats.cancelled}
            </p>
            <p className="mt-0.5 text-xs text-outline m-0">Toàn hệ thống</p>
          </div>
          <span className="material-symbols-outlined text-3xl text-primary/70">gavel</span>
        </div>
      </div>

      {/* Realtime Search Bar */}
      <div className="bg-surface rounded-2xl p-4 shadow-sm border border-surface-container-highest mb-6">
        <div className="relative w-full">
          <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(0);
            }}
            placeholder="Tìm kiếm realtime theo tên tài khoản, email, lý do khóa hoặc nội dung giải trình..."
            className="w-full pl-11 pr-4 py-2.5 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans"
          />
        </div>
      </div>

      {/* Filter Pill Tabs Bar */}
      <div className="mb-6 flex items-center gap-2 border-b border-surface-container-highest pb-3">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.value}
            type="button"
            onClick={() => handleTabChange(tab.value)}
            className={`py-2 px-5 rounded-full text-sm font-semibold cursor-pointer transition-colors flex items-center gap-2 ${
              status === tab.value
                ? 'bg-primary text-on-primary shadow-sm'
                : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'
            }`}
          >
            <span className="material-symbols-outlined text-lg">{tab.icon}</span>
            {tab.label}
          </button>
        ))}
      </div>

      {error && (
        <div className="mb-6 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
          {error}
        </div>
      )}

      {/* Appeals Table Card */}
      <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
        {loading ? (
          <div className="py-16 text-center text-outline">Đang tải danh sách khiếu nại...</div>
        ) : pagedAppeals.length === 0 ? (
          <div className="py-16 text-center text-outline">
            <span className="material-symbols-outlined text-4xl block mb-2">gavel</span>
            {searchQuery ? 'Không tìm thấy khiếu nại nào phù hợp với từ khóa.' : 'Không có đơn khiếu nại nào ở trạng thái này.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left">
                  {(
                    [
                      ['userName', 'TÀI KHOẢN KHIẾU NẠI'],
                      ['lockReason', 'LÝ DO KHÓA'],
                      ['reason', 'NỘI DUNG GIẢI TRÌNH'],
                      ['submittedAt', 'THỜI GIAN GỬI'],
                    ] as const
                  ).map(([key, label]) => (
                    <SortableTableHeader
                      key={key}
                      label={label}
                      active={sortKey === key}
                      direction={sortDirection}
                      onClick={() => changeSort(key)}
                    />
                  ))}
                  <th scope="col" className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] text-center">
                    THAO TÁC
                  </th>
                </tr>
              </thead>
              <tbody>
                {pagedAppeals.map((appeal) => (
                  <tr key={appeal.id} className="border-b border-surface-container-highest hover:bg-surface-bright transition-colors">
                    <td className="py-3.5 px-3">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-container text-on-primary-container font-bold text-sm">
                          {(appeal.userName || 'U').charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <div className="font-semibold text-sm text-on-surface">{appeal.userName ?? 'Người dùng'}</div>
                          <div className="text-xs text-on-surface-variant mt-0.5">{appeal.userEmail || 'Không có email'}</div>
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 px-3 max-w-[220px]">
                      <span className="text-xs text-error font-medium line-clamp-2">
                        {appeal.lockReason || 'Không có ghi chú lý do khóa.'}
                      </span>
                    </td>
                    <td className="py-3.5 px-3 max-w-[280px]">
                      <p className="text-xs text-on-surface-variant line-clamp-2 m-0 leading-relaxed">
                        {appeal.reason}
                      </p>
                    </td>
                    <td className="py-3.5 px-3 text-xs text-on-surface-variant whitespace-nowrap">
                      {timeAgo(appeal.submittedAt)}
                    </td>
                    <td className="py-3.5 px-3 text-center">
                      <div className="flex items-center justify-center">
                        <button
                          type="button"
                          onClick={() => navigate(`/admin/account-lock-appeals/${appeal.id}`)}
                          className="h-8 py-1 px-4 rounded-full bg-primary text-on-primary text-xs font-semibold cursor-pointer inline-flex items-center gap-1 hover:bg-primary/90"
                        >
                          <span className="material-symbols-outlined text-base">visibility</span>
                          Xem xét
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
          <span className="text-[13px] text-outline">
            Hiển thị {totalElements === 0 ? 0 : pageStart}-{pageEnd} trong {totalElements} kết quả
          </span>
          <div className="flex gap-1">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((v) => Math.max(0, v - 1))}
              className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${
                page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'
              }`}
            >
              <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
            </button>

            {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
              const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
              const p = startPage + i;
              if (p >= totalPages || p < 0) return null;
              return (
                <button
                  key={p}
                  type="button"
                  onClick={() => setPage(p)}
                  className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${
                    page === p
                      ? 'border-0 bg-primary text-on-primary'
                      : 'border border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-container-low'
                  }`}
                >
                  {p + 1}
                </button>
              );
            })}

            <button
              type="button"
              disabled={page >= totalPages - 1 || totalPages === 0}
              onClick={() => setPage((v) => Math.min(totalPages - 1, v + 1))}
              className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${
                page >= totalPages - 1 || totalPages === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'
              }`}
            >
              <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

