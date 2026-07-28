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
  const [copied, setCopied] = useState(false);
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

  const actionCounts = useMemo(() => items.reduce<Record<string, number>>((counts, item) => {
    counts[item.actionType] = (counts[item.actionType] ?? 0) + 1;
    return counts;
  }, {}), [items]);

  const handleCopyId = () => {
    if (!targetUserId) return;
    void navigator.clipboard.writeText(targetUserId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content font-sans">
        <div className="p-8">
          {/* Top Bar Navigation */}
          <div className="mb-4">
            <button
              type="button"
              onClick={() => navigate('/admin/violations')}
              className="inline-flex items-center gap-1.5 py-2 px-4 rounded-full bg-surface border border-outline-variant text-xs font-semibold text-on-surface-variant hover:bg-surface-container-low transition-colors cursor-pointer"
            >
              <span className="material-symbols-outlined text-base">arrow_back</span>
              Trở lại danh sách vi phạm
            </button>
          </div>

          {/* Account Overview Header */}
          <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-14 h-14 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center text-2xl font-bold">
                <span className="material-symbols-outlined text-3xl">person</span>
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-2xl font-bold text-on-surface m-0">{accountName}</h1>
                  <span className="rounded-full bg-error-container/80 px-3 py-0.5 text-xs font-bold text-error">
                    {totalElements} lần xử lý
                  </span>
                </div>
                <p className="text-outline text-xs font-mono mt-1 m-0 flex items-center gap-2">
                  <span>ID: {targetUserId}</span>
                  <button
                    type="button"
                    onClick={handleCopyId}
                    className="inline-flex items-center text-primary hover:underline text-xs bg-transparent border-0 cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-sm">{copied ? 'check' : 'content_copy'}</span>
                    {copied ? 'Đã sao chép' : 'Sao chép'}
                  </button>
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={() => void load()}
              disabled={loading}
              className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50 self-start md:self-auto"
            >
              <span className="material-symbols-outlined text-lg">refresh</span>
              Làm mới
            </button>
          </div>

          {loading ? (
            <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
              <div className="space-y-4">
                <div className="h-40 animate-pulse rounded-2xl bg-surface-container-low" />
                <div className="h-40 animate-pulse rounded-2xl bg-surface-container-low" />
              </div>
              <div className="h-64 animate-pulse rounded-2xl bg-surface-container-low" />
            </div>
          ) : error ? (
            <div className="bg-surface rounded-2xl p-12 text-center border border-surface-container-highest">
              <span className="material-symbols-outlined mb-3 text-5xl text-error">error</span>
              <h2 className="m-0 text-lg font-semibold text-on-surface">Không thể mở hồ sơ</h2>
              <p className="mb-0 mt-2 max-w-lg mx-auto text-sm text-on-surface-variant">{error}</p>
              <div className="mt-5 flex flex-wrap justify-center gap-2">
                <button type="button" onClick={() => void load()} className="rounded-full bg-primary px-5 py-2 text-sm font-semibold text-on-primary">Thử lại</button>
                <button type="button" onClick={() => navigate('/admin/violations')} className="rounded-full border border-outline-variant bg-surface px-5 py-2 text-sm font-semibold text-on-surface-variant">Về danh sách</button>
              </div>
            </div>
          ) : items.length === 0 ? (
            <div className="bg-surface rounded-2xl p-12 text-center border border-surface-container-highest">
              <span className="material-symbols-outlined mb-3 text-5xl text-outline">history_toggle_off</span>
              <h2 className="m-0 text-lg font-semibold text-on-surface">Không có lần xử lý nào</h2>
              <button type="button" onClick={() => navigate('/admin/violations')} className="mt-5 rounded-full border border-outline-variant px-5 py-2 text-sm font-semibold text-primary">Về danh sách</button>
            </div>
          ) : (
            <div className="grid items-start gap-6 lg:grid-cols-[1fr_360px]">
              {/* Left Column: Timeline */}
              <section aria-labelledby="violation-timeline-title">
                <div className="mb-4 flex items-center justify-between">
                  <div>
                    <h2 id="violation-timeline-title" className="m-0 text-lg font-bold text-on-surface">Lịch sử kỷ luật & xử lý</h2>
                    <p className="mb-0 mt-0.5 text-xs text-outline">Hiển thị bản ghi từ mới nhất · Tổng cộng {totalElements} bản ghi</p>
                  </div>
                  <span className="text-xs text-outline font-medium">Trang {page + 1}/{totalPages}</span>
                </div>

                <div className="space-y-4">
                  {items.map((action, index) => {
                    const meta = ACCOUNT_ACTION_META[action.actionType];
                    const status = getViolationStatus(action);
                    return (
                      <article key={action.actionId} className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
                        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 border-b border-surface-container-highest pb-4 mb-4">
                          <div className="flex items-center gap-3">
                            <span className={`material-symbols-outlined flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-xl ${meta.badgeClass}`}>
                              {meta.icon}
                            </span>
                            <div>
                              <div className="flex flex-wrap items-center gap-2">
                                <h3 className="m-0 text-base font-bold text-on-surface">{ACTION_TYPE_LABELS[action.actionType]}</h3>
                                {index === 0 && page === 0 && (
                                  <span className="rounded-full bg-primary-container px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-on-primary-container">
                                    Mới nhất
                                  </span>
                                )}
                              </div>
                              <p className="mb-0 mt-0.5 text-xs text-outline">Thực hiện bởi: <span className="font-medium text-on-surface-variant">{action.moderatorName}</span></p>
                            </div>
                          </div>

                          <div className="text-right">
                            <time className="text-xs text-outline whitespace-nowrap block">{formatViolationDateTime(action.actionAt)}</time>
                            <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 mt-1 text-xs font-semibold ${
                              status.value === 'ACTIVE' || status.value === 'INDEFINITE'
                                ? 'bg-[#E6F4EA] text-[#137333]'
                                : status.value === 'ESCALATED'
                                  ? 'bg-secondary-container text-on-secondary-container'
                                  : 'bg-surface-container-high text-on-surface-variant'
                            }`}>
                              {status.label}
                            </span>
                          </div>
                        </div>

                        {/* Reason Box */}
                        <div>
                          <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1.5">Lý do xử lý</span>
                          <div className="bg-surface-container-low p-4 rounded-xl text-sm leading-relaxed text-on-surface whitespace-pre-wrap border border-surface-container-high/50">
                            {action.reason || 'Không có lý do chi tiết nào được ghi nhận.'}
                          </div>
                        </div>

                        {/* Metadata Footer */}
                        <div className="mt-4 pt-3 border-t border-surface-container-highest flex flex-wrap items-center justify-between gap-2 text-xs text-outline">
                          <span className="font-mono">Mã hành động: #{shortId(action.actionId)}</span>
                          {action.reportId && (
                            <button
                              type="button"
                              onClick={() => navigate(`/admin/reports/${action.reportId}`)}
                              className="inline-flex items-center gap-1 text-primary font-semibold hover:underline bg-transparent border-0 cursor-pointer"
                            >
                              <span className="material-symbols-outlined text-sm">flag</span>
                              Báo cáo liên quan (#{shortId(action.reportId)})
                            </button>
                          )}
                        </div>
                      </article>
                    );
                  })}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="mt-6 flex items-center justify-end gap-2">
                    <button
                      type="button"
                      disabled={page === 0}
                      onClick={() => setPage((current) => Math.max(0, current - 1))}
                      className="inline-flex h-9 items-center gap-1 rounded-full border border-outline-variant bg-surface px-4 text-xs font-semibold text-on-surface-variant disabled:opacity-40 cursor-pointer hover:bg-surface-container-low"
                    >
                      <span className="material-symbols-outlined text-base">chevron_left</span>
                      Trang trước
                    </button>
                    <button
                      type="button"
                      disabled={(page + 1) * DETAIL_PAGE_SIZE >= totalElements}
                      onClick={() => setPage((current) => current + 1)}
                      className="inline-flex h-9 items-center gap-1 rounded-full border border-outline-variant bg-surface px-4 text-xs font-semibold text-on-surface-variant disabled:opacity-40 cursor-pointer hover:bg-surface-container-low"
                    >
                      Trang sau
                      <span className="material-symbols-outlined text-base">chevron_right</span>
                    </button>
                  </div>
                )}
              </section>

              {/* Right Column: Sidebar */}
              <aside className="space-y-5 lg:sticky lg:top-6" aria-label="Tóm tắt hồ sơ">
                {/* Summary Card */}
                <section className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
                  <h2 className="m-0 text-base font-bold text-on-surface border-b border-surface-container-highest pb-3">Tóm tắt hồ sơ</h2>
                  <dl className="mb-0 mt-3 divide-y divide-surface-container-highest">
                    <div className="flex items-center justify-between py-3">
                      <dt className="text-xs text-outline">Tổng lần xử lý</dt>
                      <dd className="m-0 text-lg font-bold tabular-nums text-on-surface">{totalElements}</dd>
                    </div>
                    <div className="flex items-center justify-between py-3">
                      <dt className="text-xs text-outline">Cảnh cáo (Warn)</dt>
                      <dd className="m-0 text-sm font-semibold tabular-nums text-on-surface">{actionCounts.WARN ?? 0}</dd>
                    </div>
                    <div className="flex items-center justify-between py-3">
                      <dt className="text-xs text-outline">Hạn chế đăng (Restrict)</dt>
                      <dd className="m-0 text-sm font-semibold tabular-nums text-on-surface">{actionCounts.RESTRICT ?? 0}</dd>
                    </div>
                    <div className="flex items-center justify-between py-3">
                      <dt className="text-xs text-outline">Đình chỉ (Suspend)</dt>
                      <dd className="m-0 text-sm font-semibold tabular-nums text-on-surface">{actionCounts.SUSPEND ?? 0}</dd>
                    </div>
                  </dl>
                </section>

                {/* Latest Action Highlight */}
                {latestAction && (
                  <section className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
                    <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-3">Xử lý gần đây nhất</span>
                    <div className="flex items-center gap-3">
                      <span className={`material-symbols-outlined flex h-9 w-9 items-center justify-center rounded-full text-lg ${ACCOUNT_ACTION_META[latestAction.actionType].badgeClass}`}>
                        {ACCOUNT_ACTION_META[latestAction.actionType].icon}
                      </span>
                      <div>
                        <p className="m-0 text-sm font-bold text-on-surface">{ACTION_TYPE_LABELS[latestAction.actionType]}</p>
                        <p className="m-0 text-xs text-outline">{formatViolationDateTime(latestAction.actionAt)}</p>
                      </div>
                    </div>
                    <p className="mb-0 mt-3 line-clamp-3 text-xs leading-relaxed text-on-surface-variant bg-surface-container-low p-3 rounded-xl border border-surface-container-high/40">
                      {latestAction.reason}
                    </p>
                  </section>
                )}

                {/* Account Identifier Card */}
                <section className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
                  <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-2">Định danh tài khoản</span>
                  <div className="flex items-center justify-between bg-surface-container-low p-3 rounded-xl border border-surface-container-high/40">
                    <p className="m-0 break-all font-mono text-xs text-on-surface-variant select-all">{targetUserId}</p>
                    <button
                      type="button"
                      onClick={handleCopyId}
                      className="p-1 rounded text-outline hover:text-primary bg-transparent border-0 cursor-pointer"
                      title="Sao chép ID"
                    >
                      <span className="material-symbols-outlined text-base">{copied ? 'check' : 'content_copy'}</span>
                    </button>
                  </div>
                </section>
              </aside>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
