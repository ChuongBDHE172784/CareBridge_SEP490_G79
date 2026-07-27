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
      <main className="portal-content">
        <div className="portal-contained">
          <div className="portal-header">
            <div>
              <p className="portal-eyebrow">Kiểm duyệt</p>
              <h1 className="portal-title">Lịch sử vi phạm</h1>
              <p className="portal-subtitle max-w-3xl">
                Theo dõi cảnh cáo, hạn chế, đình chỉ và các trường hợp chuyển cấp để nắm tình trạng kỷ luật tài khoản.
              </p>
            </div>
            <div className="flex gap-2">
              <button type="button" onClick={() => void load()} className="portal-secondary-button" disabled={loading}>
                <span className="material-symbols-outlined text-base">refresh</span>
                Làm mới
              </button>
              <button type="button" onClick={() => navigate(-1)} className="portal-secondary-button">
                <span className="material-symbols-outlined text-lg">arrow_back</span>
                Quay lại
              </button>
            </div>
          </div>

          <section className="mb-5 grid gap-3 md:grid-cols-4">
            {[
              { label: 'Tổng bản ghi', value: totalElements, icon: 'database' },
              { label: 'Đang hiệu lực', value: stats.active, icon: 'verified' },
              { label: 'Hết hiệu lực', value: stats.expired, icon: 'event_busy' },
              { label: 'Chuyển cấp', value: stats.escalated, icon: 'upgrade' },
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
            <div className="grid gap-3 lg:grid-cols-[1.2fr_0.8fr_0.8fr_0.5fr_auto] lg:items-end">
              <label>
                <span className="portal-label">Tìm kiếm</span>
                <div className="relative">
                  <span className="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-outline">search</span>
                  <input value={search} onChange={(event) => setSearch(event.target.value)} className="portal-field w-full pl-9" placeholder="Tìm tài khoản, lý do, người xử lý..." />
                </div>
              </label>
              <label>
                <span className="portal-label">Hành động</span>
                <select value={actionFilter} onChange={(event) => setActionFilter(event.target.value as ActionFilter)} className="portal-field w-full">
                  <option value="ALL">Tất cả</option>
                  <option value="WARN">Cảnh cáo</option>
                  <option value="SUSPEND">Đình chỉ</option>
                  <option value="RESTRICT">Hạn chế đăng</option>
                  <option value="ESCALATE">Chuyển cấp</option>
                </select>
              </label>
              <label>
                <span className="portal-label">Trạng thái</span>
                <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as StatusFilter)} className="portal-field w-full">
                  <option value="ALL">Tất cả</option>
                  <option value="ACTIVE">Đang hiệu lực</option>
                  <option value="INDEFINITE">Không thời hạn</option>
                  <option value="EXPIRED">Đã hết hiệu lực</option>
                  <option value="ESCALATED">Đã chuyển cấp</option>
                </select>
              </label>
              <label>
                <span className="portal-label">Mỗi trang</span>
                <select value={pageSize} onChange={(event) => setPageSize(Number(event.target.value) as typeof pageSize)} className="portal-field w-full">
                  {PAGE_SIZE_OPTIONS.map((size) => <option key={size} value={size}>{size}</option>)}
                </select>
              </label>
              <button type="button" onClick={resetFilters} className="portal-secondary-button">
                <span className="material-symbols-outlined text-base">filter_alt_off</span>
                Xóa lọc
              </button>
            </div>
          </section>

          {loading ? (
            <div className="portal-empty">Đang tải lịch sử vi phạm...</div>
          ) : error ? (
            <div className="portal-error text-center">
              <p className="text-sm text-error">{error}</p>
              <button type="button" onClick={() => void load()} className="mt-3 rounded-md bg-error px-4 py-2 text-sm font-semibold text-on-error">Thử lại</button>
            </div>
          ) : items.length === 0 ? (
            <div className="portal-card-padded text-center">
              <span className="material-symbols-outlined text-5xl text-outline">gavel</span>
              <h2 className="mt-4 text-base font-semibold text-on-surface">Chưa có dữ liệu lịch sử vi phạm</h2>
              <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">Chưa ghi nhận hành động kỷ luật nào đối với tài khoản.</p>
            </div>
          ) : (
            <section className="portal-table-card">
              <div className="flex flex-col gap-2 border-b border-outline-variant/70 p-4 md:flex-row md:items-center md:justify-between">
                <div>
                  <h2 className="text-sm font-semibold text-on-surface">Bảng lịch sử kỷ luật</h2>
                  <p className="mt-1 text-xs text-on-surface-variant">
                    {filteredItems.length} mục phù hợp trong trang dữ liệu hiện tại. Tổng hệ thống: {totalElements} bản ghi.
                  </p>
                </div>
                <span className="rounded-md bg-surface-container-low px-2.5 py-1 text-xs font-semibold text-on-surface-variant">
                  Trang {page + 1} / {totalPages}
                </span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[1080px]">
                  <thead>
                    <tr>
                      {['Tài khoản', 'Hành động', 'Lý do', 'Trạng thái hiệu lực', 'Người xử lý', 'Thời gian'].map((heading) => <th key={heading}>{heading}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {filteredItems.map((item) => {
                      const status = getEnforcementStatus(item);
                      return (
                        <tr key={item.actionId}>
                          <td>
                            <div className="font-semibold text-on-surface">{item.targetUserName}</div>
                            <div className="mt-1 text-[11px] text-outline">{item.targetUserId}</div>
                          </td>
                          <td><span className="rounded-md bg-error-container px-2.5 py-1 text-xs font-semibold text-error">{ACTION_TYPE_LABELS[item.actionType]}</span></td>
                          <td className="max-w-[320px] text-on-surface-variant">{item.reason}</td>
                          <td className={status.muted ? 'text-outline' : 'text-on-surface-variant'}>{status.label}</td>
                          <td className="text-on-surface-variant">{item.moderatorName}</td>
                          <td className="whitespace-nowrap text-on-surface-variant">{formatDateTime(item.actionAt)}</td>
                        </tr>
                      );
                    })}
                    {filteredItems.length === 0 && <tr><td colSpan={6} className="text-center text-outline">Không có bản ghi nào phù hợp bộ lọc trong trang hiện tại.</td></tr>}
                  </tbody>
                </table>
              </div>
              <div className="flex flex-col gap-3 border-t border-outline-variant/70 p-4 md:flex-row md:items-center md:justify-between">
                <span className="text-xs text-on-surface-variant">Đang xem tối đa {pageSize} bản ghi mỗi trang.</span>
                <div className="flex gap-2">
                  <button type="button" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))} className="portal-secondary-button">Trước</button>
                  <button type="button" disabled={!hasNext} onClick={() => setPage((current) => current + 1)} className="portal-secondary-button">Sau</button>
                </div>
              </div>
            </section>
          )}
        </div>
      </main>
    </div>
  );
}
