import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import {
  ACTION_TYPE_LABELS,
  type AccountViolationHistoryItem,
  type AccountViolationSummaryItem,
} from '../models/moderation';
import { fetchAccountViolationDetail } from '../services/moderationApi';
import {
  ACCOUNT_ACTION_META,
  formatViolationDateTime,
  getViolationStatus,
} from '../utils/violationPresentation';

const DETAIL_PAGE_SIZE = 20;

type DetailLocationState = {
  summary?: AccountViolationSummaryItem;
};

function shortId(value: string): string {
  return value.slice(0, 8).toUpperCase();
}

export default function ViolationDetailPage() {
  const { targetUserId } = useParams<{ targetUserId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const summary = (location.state as DetailLocationState | null)?.summary;
  const [items, setItems] = useState<AccountViolationHistoryItem[]>([]);
  const [totalElements, setTotalElements] = useState(summary?.violationCount ?? 0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const latestRequest = useRef(0);

  const load = useCallback(async () => {
    if (!targetUserId) {
      setError('Mã tài khoản không hợp lệ.');
      setLoading(false);
      return;
    }
    const requestId = ++latestRequest.current;
    setLoading(true);
    setError('');
    try {
      const result = await fetchAccountViolationDetail(targetUserId, { page, size: DETAIL_PAGE_SIZE });
      if (requestId !== latestRequest.current) return;
      setItems(result.content);
      setTotalElements(result.totalElements);
    } catch (request: unknown) {
      if (requestId !== latestRequest.current) return;
      const response = (request as { response?: { status?: number; data?: { message?: string } } })?.response;
      setItems([]);
      setError(response?.status === 404
        ? 'Không tìm thấy lịch sử xử lý của tài khoản này.'
        : response?.data?.message || 'Không tải được hồ sơ vi phạm.');
    } finally {
      if (requestId === latestRequest.current) setLoading(false);
    }
  }, [page, targetUserId]);

  useEffect(() => { void load(); }, [load]);

  const accountName = summary?.targetUserName ?? items[0]?.targetUserName ?? 'Tài khoản';
  const latestAction = page === 0 ? (items[0] ?? summary?.latestAction) : summary?.latestAction;
  const totalPages = Math.max(1, Math.ceil(totalElements / DETAIL_PAGE_SIZE));
  const hasNext = (page + 1) * DETAIL_PAGE_SIZE < totalElements;

  const actionCounts = useMemo(() => items.reduce<Record<string, number>>((counts, item) => {
    counts[item.actionType] = (counts[item.actionType] ?? 0) + 1;
    return counts;
  }, {}), [items]);

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content font-sans">
        <div className="mx-auto w-full max-w-[1320px] px-5 py-6 md:px-8 md:py-8">
          <header className="mb-6 border-b border-surface-container-highest pb-5">
            <button
              type="button"
              onClick={() => navigate('/moderator/violations')}
              className="mb-4 inline-flex h-9 items-center gap-1 rounded-md px-2 text-sm font-semibold text-primary transition-colors hover:bg-primary-container focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
            >
              <span className="material-symbols-outlined text-lg">arrow_back</span>
              Trở lại hồ sơ vi phạm
            </button>
            <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
              <div className="min-w-0">
                <p className="mb-1 mt-0 text-xs font-medium text-outline">Hồ sơ tài khoản</p>
                <h1 className="m-0 truncate text-[26px] font-bold leading-tight text-on-surface">{accountName}</h1>
                <p className="mb-0 mt-2 break-all font-mono text-xs text-outline">{targetUserId}</p>
              </div>
              <button type="button" onClick={() => void load()} disabled={loading} className="inline-flex h-10 items-center gap-2 self-start rounded-md border border-outline-variant bg-surface px-4 text-sm font-semibold text-on-surface-variant transition-colors hover:bg-surface-container-low disabled:opacity-50 md:self-auto">
                <span className="material-symbols-outlined text-lg">refresh</span>Làm mới
              </button>
            </div>
          </header>

          {loading ? (
            <div className="grid gap-5 lg:grid-cols-[1fr_300px]" aria-label="Đang tải hồ sơ">
              <div className="space-y-4"><div className="h-28 animate-pulse rounded-md bg-surface-container-low" /><div className="h-40 animate-pulse rounded-md bg-surface-container-low" /><div className="h-40 animate-pulse rounded-md bg-surface-container-low" /></div>
              <div className="h-64 animate-pulse rounded-md bg-surface-container-low" />
            </div>
          ) : error ? (
            <section className="flex min-h-[360px] flex-col items-center justify-center rounded-md border border-surface-container-highest bg-surface px-5 text-center">
              <span className="material-symbols-outlined mb-3 text-5xl text-error">error</span>
              <h2 className="m-0 text-lg font-semibold text-on-surface">Không thể mở hồ sơ</h2>
              <p className="mb-0 mt-2 max-w-lg text-sm text-on-surface-variant">{error}</p>
              <div className="mt-5 flex flex-wrap justify-center gap-2"><button type="button" onClick={() => void load()} className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-on-primary">Thử lại</button><button type="button" onClick={() => navigate('/moderator/violations')} className="rounded-md border border-outline-variant bg-surface px-4 py-2 text-sm font-semibold text-on-surface-variant">Về danh sách</button></div>
            </section>
          ) : items.length === 0 ? (
            <section className="flex min-h-[360px] flex-col items-center justify-center rounded-md border border-surface-container-highest bg-surface text-center"><span className="material-symbols-outlined mb-3 text-5xl text-outline">history_toggle_off</span><h2 className="m-0 text-lg font-semibold text-on-surface">Không có lần xử lý nào</h2><button type="button" onClick={() => navigate('/moderator/violations')} className="mt-5 rounded-md border border-outline-variant px-4 py-2 text-sm font-semibold text-primary">Về danh sách</button></section>
          ) : (
            <div className="grid items-start gap-6 lg:grid-cols-[1fr_300px]">
              <section aria-labelledby="violation-timeline-title">
                <div className="mb-4 flex items-center justify-between">
                  <div><h2 id="violation-timeline-title" className="m-0 text-lg font-bold text-on-surface">Lịch sử xử lý</h2><p className="mb-0 mt-1 text-xs text-outline">Mới nhất trước · {totalElements} bản ghi</p></div>
                  <span className="font-mono text-xs text-outline">Trang {page + 1}/{totalPages}</span>
                </div>

                <div className="relative space-y-3 before:absolute before:bottom-6 before:left-[21px] before:top-6 before:w-px before:bg-outline-variant">
                  {items.map((action, index) => {
                    const meta = ACCOUNT_ACTION_META[action.actionType];
                    const status = getViolationStatus(action);
                    return (
                      <article key={action.actionId} className={`relative rounded-md border border-surface-container-highest border-l-4 bg-surface p-4 pl-5 shadow-sm ${meta.railClass}`}>
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                          <div className="flex min-w-0 items-start gap-3">
                            <span className={`material-symbols-outlined relative z-[1] flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-lg ${meta.badgeClass}`}>{meta.icon}</span>
                            <div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h3 className="m-0 text-sm font-bold text-on-surface">{ACTION_TYPE_LABELS[action.actionType]}</h3>{index === 0 && page === 0 && <span className="rounded bg-primary-container px-2 py-0.5 text-[10px] font-semibold text-on-primary-container">Mới nhất</span>}</div><p className="mb-0 mt-1 text-xs text-outline">Bởi {action.moderatorName}</p></div>
                          </div>
                          <time className="whitespace-nowrap text-xs tabular-nums text-outline">{formatViolationDateTime(action.actionAt)}</time>
                        </div>

                        <div className="ml-12 mt-4 border-t border-surface-container-highest pt-4">
                          <p className="mb-1 mt-0 text-[11px] font-semibold text-outline">Lý do xử lý</p>
                          <p className="m-0 whitespace-pre-wrap text-sm leading-6 text-on-surface">{action.reason || 'Không có lý do được ghi nhận.'}</p>
                          <dl className="mb-0 mt-4 grid gap-3 sm:grid-cols-2">
                            <div><dt className="text-[11px] font-medium text-outline">Trạng thái hiệu lực</dt><dd className={`m-0 mt-1 text-xs font-semibold ${status.muted ? 'text-outline' : 'text-[#137333]'}`}>{status.label}</dd></div>
                            <div><dt className="text-[11px] font-medium text-outline">Mã hành động</dt><dd className="m-0 mt-1 font-mono text-xs text-on-surface-variant">#{shortId(action.actionId)}</dd></div>
                            {action.reportId && <div className="sm:col-span-2"><dt className="text-[11px] font-medium text-outline">Báo cáo liên quan</dt><dd className="m-0 mt-1 break-all font-mono text-xs text-on-surface-variant">{action.reportId}</dd></div>}
                          </dl>
                        </div>
                      </article>
                    );
                  })}
                </div>

                {totalPages > 1 && <div className="mt-4 flex items-center justify-end gap-2"><button type="button" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))} className="inline-flex h-9 items-center gap-1 rounded-md border border-outline-variant bg-surface px-3 text-xs font-semibold text-on-surface-variant disabled:opacity-40"><span className="material-symbols-outlined text-lg">chevron_left</span>Trang trước</button><button type="button" disabled={!hasNext} onClick={() => setPage((current) => current + 1)} className="inline-flex h-9 items-center gap-1 rounded-md border border-outline-variant bg-surface px-3 text-xs font-semibold text-on-surface-variant disabled:opacity-40">Trang sau<span className="material-symbols-outlined text-lg">chevron_right</span></button></div>}
              </section>

              <aside className="space-y-4 lg:sticky lg:top-6" aria-label="Tóm tắt hồ sơ">
                <section className="rounded-md border border-surface-container-highest bg-surface p-5">
                  <h2 className="m-0 text-sm font-bold text-on-surface">Tóm tắt hồ sơ</h2>
                  <dl className="mb-0 mt-4 divide-y divide-surface-container-highest">
                    <div className="flex items-center justify-between py-3 first:pt-0"><dt className="text-xs text-outline">Tổng lần xử lý</dt><dd className="m-0 text-lg font-bold tabular-nums text-on-surface">{totalElements}</dd></div>
                    <div className="flex items-center justify-between py-3"><dt className="text-xs text-outline">Cảnh cáo trang này</dt><dd className="m-0 text-sm font-semibold tabular-nums text-on-surface">{actionCounts.WARN ?? 0}</dd></div>
                    <div className="flex items-center justify-between py-3"><dt className="text-xs text-outline">Hạn chế/đình chỉ</dt><dd className="m-0 text-sm font-semibold tabular-nums text-on-surface">{(actionCounts.RESTRICT ?? 0) + (actionCounts.SUSPEND ?? 0)}</dd></div>
                  </dl>
                </section>

                {latestAction && <section className="rounded-md bg-surface-container-low p-5"><p className="m-0 text-[11px] font-semibold text-outline">Xử lý gần nhất</p><div className="mt-3 flex items-center gap-2"><span className={`material-symbols-outlined flex h-8 w-8 items-center justify-center rounded text-base ${ACCOUNT_ACTION_META[latestAction.actionType].badgeClass}`}>{ACCOUNT_ACTION_META[latestAction.actionType].icon}</span><div><p className="m-0 text-sm font-bold text-on-surface">{ACTION_TYPE_LABELS[latestAction.actionType]}</p><p className="m-0 mt-0.5 text-xs text-outline">{formatViolationDateTime(latestAction.actionAt)}</p></div></div><p className="mb-0 mt-3 line-clamp-3 text-xs leading-5 text-on-surface-variant">{latestAction.reason}</p></section>}

                <section className="rounded-md border border-surface-container-highest bg-surface p-5"><p className="m-0 text-[11px] font-semibold text-outline">Định danh tài khoản</p><p className="mb-0 mt-3 break-all font-mono text-xs leading-5 text-on-surface-variant">{targetUserId}</p></section>
              </aside>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
