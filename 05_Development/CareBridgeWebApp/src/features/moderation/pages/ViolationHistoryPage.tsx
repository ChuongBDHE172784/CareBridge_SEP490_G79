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

type AccountAction = Extract<ModerationActionType, 'WARN' | 'SUSPEND' | 'RESTRICT' | 'ESCALATE'>;
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
    return { active, repeat };
  }, [items]);

  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
  const hasNext = (page + 1) * pageSize < totalElements;
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
        <div className="mx-auto w-full max-w-[1440px] px-5 py-6 md:px-8 md:py-8">
          <header className="mb-6 flex flex-col gap-4 border-b border-surface-container-highest pb-5 md:flex-row md:items-end md:justify-between">
            <div>
              <div className="mb-2 flex items-center gap-2 text-xs font-medium text-outline">
                <span>Kiểm duyệt</span><span>/</span><span className="text-on-surface-variant">Vi phạm tài khoản</span>
              </div>
              <h1 className="m-0 text-[26px] font-bold leading-tight text-on-surface">Hồ sơ vi phạm tài khoản</h1>
            </div>
            <button
              type="button"
              onClick={() => void load()}
              disabled={loading}
              title="Làm mới dữ liệu"
              className="inline-flex h-10 items-center gap-2 self-start rounded-md border border-outline-variant bg-surface px-4 text-sm font-semibold text-on-surface-variant transition-colors hover:bg-surface-container-low focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary disabled:opacity-50 md:self-auto"
            >
              <span className="material-symbols-outlined text-lg">refresh</span>
              Làm mới
            </button>
          </header>

          <section className="mb-5 grid border-y border-surface-container-highest bg-surface md:grid-cols-3" aria-label="Tổng quan trang hiện tại">
            {[
              { label: 'Tổng tài khoản', value: totalElements, icon: 'group' },
              { label: 'Đang có hiệu lực', value: pageSummary.active, icon: 'verified_user' },
              { label: 'Vi phạm nhiều lần', value: pageSummary.repeat, icon: 'history' },
            ].map((metric, index) => (
              <div key={metric.label} className={`flex items-center gap-3 px-4 py-4 ${index > 0 ? 'border-t border-surface-container-highest md:border-l md:border-t-0' : ''}`}>
                <span className="material-symbols-outlined text-[22px] text-primary">{metric.icon}</span>
                <div><p className="m-0 text-xl font-bold tabular-nums text-on-surface">{metric.value}</p><p className="m-0 text-xs font-medium text-outline">{metric.label}</p></div>
              </div>
            ))}
          </section>

          <section className="mb-4 grid gap-3 rounded-md bg-surface-container-low p-3 lg:grid-cols-[1fr_auto_auto_auto]" aria-label="Bộ lọc hồ sơ">
            <label className="relative block">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-xl text-outline">search</span>
              <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tên tài khoản, ID, lý do..." className="h-10 w-full rounded-md border border-outline-variant bg-surface pl-10 pr-3 text-sm text-on-surface outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </label>
            <select value={actionFilter} onChange={(event) => setActionFilter(event.target.value as ActionFilter)} aria-label="Lọc theo hành động mới nhất" className="h-10 rounded-md border border-outline-variant bg-surface px-3 text-sm text-on-surface-variant outline-none focus:border-primary">
              <option value="ALL">Tất cả hành động</option><option value="WARN">Cảnh cáo</option><option value="SUSPEND">Đình chỉ</option><option value="RESTRICT">Hạn chế đăng</option><option value="ESCALATE">Chuyển cấp</option>
            </select>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as StatusFilter)} aria-label="Lọc theo trạng thái" className="h-10 rounded-md border border-outline-variant bg-surface px-3 text-sm text-on-surface-variant outline-none focus:border-primary">
              <option value="ALL">Tất cả trạng thái</option><option value="ACTIVE">Đang hiệu lực</option><option value="INDEFINITE">Không thời hạn</option><option value="EXPIRED">Đã hết hiệu lực</option><option value="ESCALATED">Đã chuyển cấp</option>
            </select>
            {filtersApplied ? <button type="button" onClick={resetFilters} className="inline-flex h-10 items-center justify-center gap-1 rounded-md px-3 text-sm font-semibold text-primary transition-colors hover:bg-primary-container focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"><span className="material-symbols-outlined text-lg">filter_alt_off</span>Xóa lọc</button> : <div />}
          </section>

          <section className="overflow-hidden rounded-md border border-surface-container-highest bg-surface" aria-label="Danh sách hồ sơ vi phạm">
            {loading ? (
              <div className="space-y-3 p-5" aria-label="Đang tải"><div className="h-11 animate-pulse rounded bg-surface-container-high" /><div className="h-14 animate-pulse rounded bg-surface-container-low" /><div className="h-14 animate-pulse rounded bg-surface-container-low" /></div>
            ) : error ? (
              <div className="flex flex-col items-center py-14 text-center"><span className="material-symbols-outlined mb-2 text-4xl text-error">cloud_off</span><p className="m-0 text-sm font-semibold text-on-surface">{error}</p><button type="button" onClick={() => void load()} className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-on-primary">Thử lại</button></div>
            ) : items.length === 0 ? (
              <div className="flex flex-col items-center py-16 text-center"><span className="material-symbols-outlined mb-3 text-5xl text-outline">fact_check</span><h2 className="m-0 text-base font-semibold text-on-surface">Chưa có hồ sơ vi phạm</h2><p className="mb-0 mt-1 text-sm text-on-surface-variant">Các hành động kỷ luật tài khoản sẽ xuất hiện tại đây.</p></div>
            ) : (
              <>
                <div className="divide-y divide-surface-container-highest md:hidden">
                  {filteredItems.map((item) => {
                    const latest = item.latestAction;
                    const status = getViolationStatus(latest);
                    const actionMeta = ACCOUNT_ACTION_META[latest.actionType];
                    return (
                      <article key={item.targetUserId} className="p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0"><h2 className="m-0 truncate text-sm font-bold text-on-surface">{item.targetUserName}</h2><p className="m-0 mt-1 truncate font-mono text-[11px] text-outline">{item.targetUserId}</p></div>
                          <span className="shrink-0 text-sm font-bold tabular-nums text-on-surface">{item.violationCount} lần</span>
                        </div>
                        <div className="mt-4 flex flex-wrap items-center gap-2"><span className={`inline-flex items-center gap-1 rounded px-2 py-1 text-xs font-semibold ${actionMeta.badgeClass}`}><span className="material-symbols-outlined text-[15px]">{actionMeta.icon}</span>{ACTION_TYPE_LABELS[latest.actionType]}</span><span className={`text-xs font-semibold ${status.muted ? 'text-outline' : 'text-[#137333]'}`}>{status.label}</span></div>
                        <p className="mb-0 mt-3 line-clamp-2 text-sm leading-5 text-on-surface-variant">{latest.reason}</p>
                        <div className="mt-4 flex items-center justify-between border-t border-surface-container-highest pt-3"><div><p className="m-0 text-xs text-on-surface-variant">{latest.moderatorName}</p><p className="m-0 mt-0.5 text-[11px] tabular-nums text-outline">{formatViolationDateTime(latest.actionAt)}</p></div><button type="button" onClick={() => navigate(`/moderator/violations/${item.targetUserId}`, { state: { summary: item } })} className="inline-flex h-9 items-center gap-1 rounded-md bg-primary-container px-3 text-xs font-semibold text-on-primary-container"><span className="material-symbols-outlined text-lg">arrow_forward</span>Xem hồ sơ</button></div>
                      </article>
                    );
                  })}
                  {filteredItems.length === 0 && <div className="py-14 text-center text-sm text-outline">Không có hồ sơ phù hợp bộ lọc trên trang này.</div>}
                </div>
                <div className="hidden overflow-x-auto md:block">
                  <table className="w-full min-w-[980px] border-collapse">
                    <thead className="bg-surface-container-low"><tr>{['Tài khoản', 'Số lần', 'Xử lý mới nhất', 'Hiệu lực', 'Kiểm duyệt viên', 'Thời gian', ''].map((heading) => <th key={heading} className="border-b border-surface-container-highest px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">{heading}</th>)}</tr></thead>
                    <tbody>
                      {filteredItems.map((item) => {
                        const latest = item.latestAction;
                        const status = getViolationStatus(latest);
                        const actionMeta = ACCOUNT_ACTION_META[latest.actionType];
                        return (
                          <tr key={item.targetUserId} className="group border-b border-surface-container-highest last:border-b-0 hover:bg-surface-bright">
                            <td className="px-4 py-4"><p className="m-0 text-sm font-semibold text-on-surface">{item.targetUserName}</p><p className="m-0 mt-1 font-mono text-[11px] text-outline">{item.targetUserId}</p></td>
                            <td className="px-4 py-4"><span className="text-sm font-bold tabular-nums text-on-surface">{item.violationCount}</span></td>
                            <td className="px-4 py-4"><span className={`inline-flex items-center gap-1.5 rounded px-2 py-1 text-xs font-semibold ${actionMeta.badgeClass}`}><span className="material-symbols-outlined text-[15px]">{actionMeta.icon}</span>{ACTION_TYPE_LABELS[latest.actionType]}</span><p className="m-0 mt-1.5 max-w-[260px] truncate text-xs text-on-surface-variant">{latest.reason}</p></td>
                            <td className="px-4 py-4"><span className={`text-xs font-semibold ${status.muted ? 'text-outline' : 'text-[#137333]'}`}>{status.label}</span></td>
                            <td className="px-4 py-4 text-sm text-on-surface-variant">{latest.moderatorName}</td>
                            <td className="whitespace-nowrap px-4 py-4 text-xs tabular-nums text-outline">{formatViolationDateTime(latest.actionAt)}</td>
                            <td className="px-4 py-4 text-right"><button type="button" onClick={() => navigate(`/moderator/violations/${item.targetUserId}`, { state: { summary: item } })} className="inline-flex h-9 items-center gap-1 rounded-md px-3 text-xs font-semibold text-primary transition-colors hover:bg-primary-container focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"><span className="material-symbols-outlined text-lg">arrow_forward</span>Xem hồ sơ</button></td>
                          </tr>
                        );
                      })}
                      {filteredItems.length === 0 && <tr><td colSpan={7} className="py-14 text-center text-sm text-outline">Không có hồ sơ phù hợp bộ lọc trên trang này.</td></tr>}
                    </tbody>
                  </table>
                </div>
                <footer className="flex flex-col gap-3 border-t border-surface-container-highest bg-surface-container-low px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-center gap-2 text-xs text-outline"><span>Trang {page + 1}/{totalPages}</span><span>·</span><select value={pageSize} onChange={(event) => setPageSize(Number(event.target.value) as typeof pageSize)} aria-label="Số tài khoản mỗi trang" className="rounded border border-outline-variant bg-surface px-2 py-1 text-xs text-on-surface-variant">{PAGE_SIZE_OPTIONS.map((size) => <option key={size} value={size}>{size} tài khoản</option>)}</select></div>
                  <div className="flex gap-1"><button type="button" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))} title="Trang trước" className="flex h-9 w-9 items-center justify-center rounded-md border border-outline-variant bg-surface text-primary disabled:opacity-40"><span className="material-symbols-outlined">chevron_left</span></button><button type="button" disabled={!hasNext} onClick={() => setPage((current) => current + 1)} title="Trang sau" className="flex h-9 w-9 items-center justify-center rounded-md border border-outline-variant bg-surface text-primary disabled:opacity-40"><span className="material-symbols-outlined">chevron_right</span></button></div>
                </footer>
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
