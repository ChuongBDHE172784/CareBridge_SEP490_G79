import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import {
  ACTION_TYPE_LABELS,
  type AccountViolationSummaryItem,
  type ModerationActionType,
} from '../models/moderation';
import { fetchAccountViolationHistory } from '../services/moderationApi';
import {
  ACCOUNT_ACTION_META,
  formatViolationDateTime,
  getViolationStatus,
  type ViolationStatus,
} from '../utils/violationPresentation';
import { SortableTableHeader, type SortDirection } from '../../contentManagement/components/SortableTableHeader';
import { nextSortDirection, sortRows } from '../../contentManagement/utils/tableSorting';

type AccountAction = Extract<ModerationActionType, 'WARN' | 'SUSPEND' | 'RESTRICT' | 'ESCALATE'>;
type ViolationSortKey = 'account' | 'violationCount' | 'latestAction' | 'status' | 'moderatorName' | 'actionAt';
type ActionFilter = 'ALL' | AccountAction;
type StatusFilter = 'ALL' | ViolationStatus;

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;

function matchesText(value: string | null | undefined, query: string): boolean {
  return (value ?? '').toLowerCase().includes(query);
}

export default function ViolationHistoryPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<AccountViolationSummaryItem[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<(typeof PAGE_SIZE_OPTIONS)[number]>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState<ActionFilter>('ALL');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [sortKey, setSortKey] = useState<ViolationSortKey>('actionAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const latestRequest = useRef(0);

  const load = useCallback(async () => {
    const requestId = ++latestRequest.current;
    setLoading(true);
    setError('');
    try {
      const result = await fetchAccountViolationHistory({ page, size: pageSize });
      if (requestId !== latestRequest.current) return;
      setItems(result.content);
      setTotalElements(result.totalElements);
    } catch {
      if (requestId !== latestRequest.current) return;
      setItems([]);
      setError('Không tải được danh sách hồ sơ vi phạm.');
    } finally {
      if (requestId === latestRequest.current) setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { setPage(0); }, [pageSize]);

  const filteredItems = useMemo(() => {
    const query = search.trim().toLowerCase();
    return items.filter((item) => {
      const latest = item.latestAction;
      const status = getViolationStatus(latest);
      return (actionFilter === 'ALL' || latest.actionType === actionFilter)
        && (statusFilter === 'ALL' || status.value === statusFilter)
        && (query.length === 0
          || matchesText(item.targetUserName, query)
          || matchesText(item.targetUserId, query)
          || matchesText(latest.reason, query)
          || matchesText(latest.moderatorName, query));
    });
  }, [actionFilter, items, search, statusFilter]);

  const pageSummary = useMemo(() => {
    const active = items.filter((item) => ['ACTIVE', 'INDEFINITE'].includes(getViolationStatus(item.latestAction).value)).length;
    const repeat = items.filter((item) => item.violationCount > 1).length;
    const disciplined = items.filter((item) => ['SUSPEND', 'RESTRICT'].includes(item.latestAction.actionType)).length;
    return { active, repeat, disciplined };
  }, [items]);

  const sortedItems = useMemo(() => sortRows(filteredItems, sortDirection, (item) => {
    const latest = item.latestAction;
    switch (sortKey) {
      case 'account': return `${item.targetUserName} ${item.targetUserId}`;
      case 'violationCount': return item.violationCount;
      case 'latestAction': return ACTION_TYPE_LABELS[latest.actionType];
      case 'status': return getViolationStatus(latest).label;
      case 'moderatorName': return latest.moderatorName;
      case 'actionAt': return new Date(latest.actionAt).getTime();
    }
  }), [filteredItems, sortDirection, sortKey]);

  const changeSort = (key: ViolationSortKey) => {
    setSortDirection(nextSortDirection(sortKey, key, sortDirection));
    setSortKey(key);
  };

  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
  const pageStart = totalElements === 0 ? 0 : page * pageSize + 1;
  const pageEnd = Math.min((page + 1) * pageSize, totalElements);
  const filtersApplied = Boolean(search || actionFilter !== 'ALL' || statusFilter !== 'ALL');

  const resetFilters = () => {
    setSearch('');
    setActionFilter('ALL');
    setStatusFilter('ALL');
  };

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content font-sans">
        <div className="p-8">
          {/* Header */}
          <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <h1 className="text-[26px] font-bold text-on-surface m-0">Hồ sơ vi phạm tài khoản</h1>
              <p className="text-on-surface-variant text-sm mt-1">
                Theo dõi và quản lý lịch sử kỷ luật, hạn chế và đình chỉ tài khoản người dùng trên hệ thống CareBridge.
              </p>
            </div>
            <button
              type="button"
              onClick={() => void load()}
              disabled={loading}
              title="Làm mới dữ liệu"
              className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50 self-start md:self-auto"
            >
              <span className="material-symbols-outlined text-lg">refresh</span>
              Làm mới
            </button>
          </div>

          {/* Stats Bar */}
          <div className="mb-6 grid gap-4 md:grid-cols-4">
            {[
              { label: 'Tổng tài khoản', value: totalElements, icon: 'group' },
              { label: 'Đang có hiệu lực', value: pageSummary.active, icon: 'verified_user' },
              { label: 'Vi phạm nhiều lần', value: pageSummary.repeat, icon: 'history' },
              { label: 'Đình chỉ / Hạn chế', value: pageSummary.disciplined, icon: 'gavel' },
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
          <div className="bg-surface rounded-2xl p-4 shadow-sm border border-surface-container-highest mb-6 space-y-4">
            <div className="flex flex-col xl:flex-row items-center gap-3">
              <div className="flex-1 w-full relative">
                <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Tìm tên tài khoản, ID, lý do, người xử lý..."
                  className="w-full py-2.5 pr-[14px] pl-[42px] rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                />
              </div>

              <div className="flex flex-wrap md:flex-nowrap items-center gap-2 w-full xl:w-auto">
                <select
                  value={actionFilter}
                  onChange={(event) => setActionFilter(event.target.value as ActionFilter)}
                  className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                >
                  <option value="ALL">Tất cả hành động</option>
                  <option value="WARN">Cảnh cáo</option>
                  <option value="SUSPEND">Đình chỉ</option>
                  <option value="RESTRICT">Hạn chế đăng</option>
                  <option value="ESCALATE">Chuyển cấp</option>
                </select>

                <select
                  value={statusFilter}
                  onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
                  className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                >
                  <option value="ALL">Tất cả trạng thái</option>
                  <option value="ACTIVE">Đang hiệu lực</option>
                  <option value="INDEFINITE">Không thời hạn</option>
                  <option value="EXPIRED">Đã hết hiệu lực</option>
                  <option value="ESCALATED">Đã chuyển cấp</option>
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

                {filtersApplied && (
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

          {/* Data Table Container */}
          <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
            {loading ? (
              <div className="py-12 text-center text-outline">Đang tải danh sách hồ sơ vi phạm...</div>
            ) : error ? (
              <div className="py-12 text-center text-error">{error}</div>
            ) : items.length === 0 ? (
              <div className="flex flex-col items-center py-16 text-center">
                <span className="material-symbols-outlined mb-3 text-5xl text-outline">fact_check</span>
                <h2 className="m-0 text-base font-semibold text-on-surface">Chưa có hồ sơ vi phạm</h2>
                <p className="mb-0 mt-1 text-sm text-on-surface-variant">Các hành động kỷ luật tài khoản sẽ xuất hiện tại đây.</p>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b-2 border-surface-container-highest text-left">
                        {([
                          ['account', 'TÀI KHOẢN'],
                          ['violationCount', 'SỐ LẦN VI PHẠM'],
                          ['latestAction', 'XỬ LÝ MỚI NHẤT'],
                          ['status', 'HIỆU LỰC'],
                          ['moderatorName', 'KIỂM DUYỆT VIÊN'],
                          ['actionAt', 'THỜI GIAN'],
                        ] as const).map(([key, label]) => (
                          <SortableTableHeader
                            key={key}
                            label={label}
                            active={sortKey === key}
                            direction={sortDirection}
                            onClick={() => changeSort(key)}
                            className="px-3"
                          />
                        ))}
                        <th scope="col" className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">THAO TÁC</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sortedItems.map((item) => {
                        const latest = item.latestAction;
                        const status = getViolationStatus(latest);
                        const actionMeta = ACCOUNT_ACTION_META[latest.actionType];
                        return (
                          <tr
                            key={item.targetUserId}
                            className="border-b border-surface-container-highest hover:bg-surface-bright cursor-pointer"
                            onClick={() => navigate(`/admin/violations/${item.targetUserId}`, { state: { summary: item } })}
                          >
                            <td className="py-3.5 px-3">
                              <p className="m-0 text-sm font-semibold text-on-surface">{item.targetUserName}</p>
                              <p className="m-0 mt-0.5 font-mono text-[11px] text-outline">{item.targetUserId}</p>
                            </td>
                            <td className="py-3.5 px-3">
                              <span className="inline-flex items-center justify-center px-2.5 py-0.5 rounded-full bg-surface-container-low text-xs font-bold tabular-nums text-on-surface">
                                {item.violationCount} lần
                              </span>
                            </td>
                            <td className="py-3.5 px-3 max-w-[280px]">
                              <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${actionMeta.badgeClass}`}>
                                <span className="material-symbols-outlined text-[15px]">{actionMeta.icon}</span>
                                {ACTION_TYPE_LABELS[latest.actionType]}
                              </span>
                            </td>
                            <td className="py-3.5 px-3 whitespace-nowrap">
                              <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${
                                status.value === 'ACTIVE' || status.value === 'INDEFINITE'
                                  ? 'bg-[#E6F4EA] text-[#137333]'
                                  : status.value === 'ESCALATED'
                                    ? 'bg-secondary-container text-on-secondary-container'
                                    : 'bg-surface-container-high text-on-surface-variant'
                              }`}>
                                {status.label}
                              </span>
                            </td>
                            <td className="py-3.5 px-3 text-xs text-on-surface-variant font-medium whitespace-nowrap">
                              {latest.moderatorName}
                            </td>
                            <td className="py-3.5 px-3 whitespace-nowrap text-[13px] text-outline">
                              {formatViolationDateTime(latest.actionAt)}
                            </td>
                            <td className="py-3.5 px-3" onClick={(e) => e.stopPropagation()}>
                              <div className="flex items-center justify-end">
                                <button
                                  type="button"
                                  onClick={() => navigate(`/admin/violations/${item.targetUserId}`, { state: { summary: item } })}
                                  className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-transparent cursor-pointer text-xs font-semibold text-primary flex items-center gap-1 hover:bg-surface-container-low"
                                  title="Xem hồ sơ chi tiết"
                                >
                                  <span className="material-symbols-outlined text-base">visibility</span>
                                  Xem hồ sơ
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                      {filteredItems.length === 0 && (
                        <tr>
                          <td colSpan={7} className="py-12 text-center text-outline">
                            Không có hồ sơ vi phạm nào phù hợp bộ lọc.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
                  <span className="text-[13px] text-outline">
                    Hiển thị {filteredItems.length === 0 ? 0 : pageStart}-{pageEnd} trong {totalElements} kết quả
                  </span>
                  <div className="flex gap-1">
                    <button
                      type="button"
                      disabled={page === 0}
                      onClick={() => setPage((current) => Math.max(0, current - 1))}
                      className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                    >
                      <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
                    </button>
                    {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                      const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
                      const p = startPage + i;
                      if (p >= totalPages) return null;
                      return (
                        <button
                          key={p}
                          type="button"
                          onClick={() => setPage(p)}
                          className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${page === p ? 'border-0 bg-primary text-on-primary' : 'border border-outline-variant bg-surface text-on-surface-variant'}`}
                        >
                          {p + 1}
                        </button>
                      );
                    })}
                    <button
                      type="button"
                      disabled={(page + 1) * pageSize >= totalElements}
                      onClick={() => setPage((current) => current + 1)}
                      className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${(page + 1) * pageSize >= totalElements ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                    >
                      <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
