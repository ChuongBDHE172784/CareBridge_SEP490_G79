import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import { ACTION_TYPE_LABELS, type AccountViolationHistoryItem, type ModerationActionType } from '../models/moderation';
import { fetchAccountViolationHistory } from '../services/moderationApi';

type ActionFilter = 'ALL' | Extract<ModerationActionType, 'WARN' | 'SUSPEND' | 'RESTRICT' | 'ESCALATE'>;
type StatusFilter = 'ALL' | 'ACTIVE' | 'EXPIRED' | 'INDEFINITE' | 'ESCALATED';

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

function getEnforcementStatus(item: AccountViolationHistoryItem): { label: string; value: Exclude<StatusFilter, 'ALL'>; muted: boolean } {
  if (item.actionType === 'ESCALATE') return { label: 'Đã chuyển cấp', value: 'ESCALATED', muted: false };
  if (!item.expiresAt) return { label: 'Không thời hạn', value: 'INDEFINITE', muted: false };
  return new Date(item.expiresAt).getTime() > Date.now()
    ? { label: `Còn hiệu lực đến ${formatDateTime(item.expiresAt)}`, value: 'ACTIVE', muted: false }
    : { label: 'Đã hết hiệu lực', value: 'EXPIRED', muted: true };
}

function matchesText(value: string | null | undefined, query: string): boolean {
  return (value ?? '').toLowerCase().includes(query);
}

export default function ViolationHistoryPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<AccountViolationHistoryItem[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<(typeof PAGE_SIZE_OPTIONS)[number]>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState<ActionFilter>('ALL');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
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
      setError('Không tải được lịch sử vi phạm. Vui lòng thử lại.');
    } finally {
      if (requestId === latestRequest.current) setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { setPage(0); }, [pageSize]);

  const stats = useMemo(() => {
    const statuses = items.map(getEnforcementStatus);
    return {
      loaded: items.length,
      active: statuses.filter((status) => status.value === 'ACTIVE' || status.value === 'INDEFINITE').length,
      expired: statuses.filter((status) => status.value === 'EXPIRED').length,
      escalated: statuses.filter((status) => status.value === 'ESCALATED').length,
    };
  }, [items]);

  const filteredItems = useMemo(() => {
    const query = search.trim().toLowerCase();
    return items.filter((item) => {
      const status = getEnforcementStatus(item);
      const matchesAction = actionFilter === 'ALL' || item.actionType === actionFilter;
      const matchesStatus = statusFilter === 'ALL' || status.value === statusFilter;
      const matchesQuery = query.length === 0
        || matchesText(item.targetUserName, query)
        || matchesText(item.moderatorName, query)
        || matchesText(item.reason, query)
        || matchesText(ACTION_TYPE_LABELS[item.actionType], query)
        || matchesText(status.label, query);
      return matchesAction && matchesStatus && matchesQuery;
    });
  }, [actionFilter, items, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
  const hasNext = (page + 1) * pageSize < totalElements;

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
              <h1 className="text-[26px] font-bold text-on-surface m-0">Lịch sử vi phạm tài khoản</h1>
              <p className="text-on-surface-variant text-sm mt-1">
                Theo dõi cảnh cáo, hạn chế, đình chỉ và các trường hợp chuyển cấp để nắm tình trạng kỷ luật tài khoản.
              </p>
            </div>
            <div className="flex items-center gap-2 self-start md:self-auto">
              <button
                type="button"
                onClick={() => void load()}
                disabled={loading}
                className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-lg">refresh</span>
                Làm mới
              </button>
              <button
                type="button"
                onClick={() => navigate(-1)}
                className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low"
              >
                <span className="material-symbols-outlined text-lg">arrow_back</span>
                Quay lại
              </button>
            </div>
          </div>

          {/* Stats Bar */}
          <div className="mb-6 grid gap-4 md:grid-cols-4">
            {[
              { label: 'Tổng bản ghi', value: totalElements, icon: 'database' },
              { label: 'Đang hiệu lực', value: stats.active, icon: 'verified' },
              { label: 'Hết hiệu lực', value: stats.expired, icon: 'event_busy' },
              { label: 'Chuyển cấp', value: stats.escalated, icon: 'upgrade' },
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
                  placeholder="Tìm tài khoản, lý do, người xử lý..."
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

                {(search || actionFilter !== 'ALL' || statusFilter !== 'ALL') && (
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

          {/* Table Container */}
          <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
            {loading ? (
              <div className="py-12 text-center text-outline">Đang tải lịch sử vi phạm...</div>
            ) : error ? (
              <div className="py-12 text-center text-error">
                <p className="text-sm">{error}</p>
                <button
                  type="button"
                  onClick={() => void load()}
                  className="mt-3 py-2 px-5 rounded-full bg-error text-on-error border-0 text-xs font-semibold cursor-pointer hover:bg-error/90"
                >
                  Thử lại
                </button>
              </div>
            ) : items.length === 0 ? (
              <div className="py-12 text-center">
                <span className="material-symbols-outlined text-5xl text-outline mb-2">gavel</span>
                <h2 className="text-base font-semibold text-on-surface m-0">Chưa có dữ liệu lịch sử vi phạm</h2>
                <p className="mt-1 text-sm text-on-surface-variant">Chưa ghi nhận hành động kỷ luật nào đối với tài khoản.</p>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b-2 border-surface-container-highest text-left">
                        {['TÀI KHOẢN', 'HÀNH ĐỘNG', 'LÝ DO', 'TRẠNG THÁI HIỆU LỰC', 'NGƯỜI XỬ LÝ', 'THỜI GIAN'].map((heading) => (
                          <th key={heading} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{heading}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {filteredItems.map((item) => {
                        const status = getEnforcementStatus(item);
                        return (
                          <tr key={item.actionId} className="border-b border-surface-container-highest hover:bg-surface-bright">
                            <td className="py-3.5 px-2">
                              <div className="font-semibold text-sm text-on-surface">{item.targetUserName}</div>
                              <div className="text-xs text-outline mt-0.5">{item.targetUserId}</div>
                            </td>
                            <td className="py-3.5 px-2">
                              <span className="inline-flex items-center py-1 px-3 rounded-full bg-[#FCE8E6] text-[#C5221F] text-xs font-semibold">
                                {ACTION_TYPE_LABELS[item.actionType]}
                              </span>
                            </td>
                            <td className="py-3.5 px-2 max-w-[320px] text-sm text-on-surface-variant">
                              {item.reason}
                            </td>
                            <td className="py-3.5 px-2">
                              <span className={`inline-flex items-center py-1 px-3 rounded-full text-xs font-semibold ${
                                status.muted
                                  ? 'bg-surface-container-high text-on-surface-variant'
                                  : 'bg-[#E6F4EA] text-[#137333]'
                              }`}>
                                {status.label}
                              </span>
                            </td>
                            <td className="py-3.5 px-2 text-[13px] text-on-surface-variant whitespace-nowrap">{item.moderatorName}</td>
                            <td className="py-3.5 px-2 text-[13px] text-outline whitespace-nowrap">{formatDateTime(item.actionAt)}</td>
                          </tr>
                        );
                      })}
                      {filteredItems.length === 0 && (
                        <tr>
                          <td colSpan={6} className="py-12 text-center text-outline">
                            Không có bản ghi nào phù hợp bộ lọc trong trang hiện tại.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
                  <span className="text-[13px] text-outline">
                    Hiển thị trang {page + 1} / {totalPages} ({totalElements} tổng số bản ghi)
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
                      disabled={!hasNext}
                      onClick={() => setPage((current) => current + 1)}
                      className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${!hasNext ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
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
