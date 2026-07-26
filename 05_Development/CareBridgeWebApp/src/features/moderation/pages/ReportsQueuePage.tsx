import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import { fetchModerationQueue, revertReport } from '../services/moderationApi';
import type { ModerationQueueItem } from '../models/moderation';
import { formatReportReason, REPORT_STATUS_LABELS, TARGET_TYPE_LABELS } from '../models/moderation';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

type Tab = 'PENDING' | 'PROCESSED';

const TABS: { label: string; value: Tab }[] = [
  { label: 'Báo cáo', value: 'PENDING' },
  { label: 'Đã xử lý', value: 'PROCESSED' },
];

export default function ReportsQueuePage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('PENDING');
  const [items, setItems] = useState<ModerationQueueItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const [revertTarget, setRevertTarget] = useState<ModerationQueueItem | null>(null);
  const [revertSubmitting, setRevertSubmitting] = useState(false);
  const [revertError, setRevertError] = useState('');

  const loadReports = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      if (tab === 'PENDING') {
        const page = await fetchModerationQueue({ status: 'PENDING', size: 50 });
        setItems(page.content);
      } else {
        // ADR-003 (CB-MOD-IMP-015): GET /queue only accepts a single status per call — fetch
        // RESOLVED and DISMISSED separately and merge client-side, newest first.
        const [resolved, dismissed] = await Promise.all([
          fetchModerationQueue({ status: 'RESOLVED', size: 50 }),
          fetchModerationQueue({ status: 'DISMISSED', size: 50 }),
        ]);
        const merged = [...resolved.content, ...dismissed.content].sort(
          (a, b) => new Date(b.reportedAt).getTime() - new Date(a.reportedAt).getTime(),
        );
        setItems(merged);
      }
    } catch {
      setError('Không tải được danh sách báo cáo.');
      setItems([]);
    } finally {
      setIsLoading(false);
    }
  }, [tab]);

  useEffect(() => { loadReports(); }, [loadReports]);

  const goToDetail = (item: ModerationQueueItem) => {
    if (item.targetType === 'ACCOUNT' || item.targetType === 'USER' || item.targetType === 'EXPERT') {
      navigate(`/moderator/reports/account/${item.id}`);
    } else {
      navigate(`/moderator/reports/${item.id}`);
    }
  };

  // CB-MOD-IMP-015: reverts a RESOLVED/DISMISSED report back to PENDING. Backend rejects (400/409)
  // if the outcome was account-level, or the underlying content action was superseded — surface
  // whatever message the API returns rather than a single generic string.
  const confirmRevert = async (reason?: string) => {
    if (!revertTarget) return;
    setRevertSubmitting(true);
    setRevertError('');
    try {
      await revertReport(revertTarget.id, reason);
      setRevertTarget(null);
      await loadReports();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setRevertError(message || 'Hoàn tác thất bại, vui lòng thử lại.');
    } finally {
      setRevertSubmitting(false);
    }
  };

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">Kiểm duyệt</p>
            <h1 className="portal-title">Báo cáo</h1>
            <p className="portal-subtitle">Theo dõi các báo cáo nội dung và hoạt động cần kiểm duyệt.</p>
          </div>
        </div>

        <div className="portal-toolbar">
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => setTab(t.value)}
              className={`rounded-md px-3 py-2 text-sm font-semibold transition-colors ${
                tab === t.value ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="portal-empty">Đang tải...</div>
        ) : error ? (
          <div className="portal-error">{error}</div>
        ) : (
          <div className="portal-table-card">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[860px]">
                <thead>
                  <tr>
                    {(tab === 'PENDING'
                      ? ['LÝ DO', 'LOẠI', 'NỘI DUNG XEM TRƯỚC', 'SỐ LƯỢT', 'THỜI GIAN', '']
                      : ['LÝ DO', 'LOẠI', 'NỘI DUNG XEM TRƯỚC', 'TRẠNG THÁI', 'THỜI GIAN', '']
                    ).map((h) => (
                      <th key={h}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.id}>
                      <td className="cursor-pointer" onClick={() => goToDetail(item)}>
                        <span className="rounded-md bg-error-container px-2.5 py-1 text-xs font-semibold text-error">
                          {formatReportReason(item.reportReason)}
                        </span>
                      </td>
                      <td className="cursor-pointer text-on-surface-variant" onClick={() => goToDetail(item)}>
                        {TARGET_TYPE_LABELS[item.targetType]}
                      </td>
                      <td
                        className="max-w-[360px] cursor-pointer truncate text-on-surface"
                        onClick={() => goToDetail(item)}
                      >
                        {item.contentPreview}
                      </td>
                      {tab === 'PENDING' ? (
                        <td className="cursor-pointer text-on-surface-variant" onClick={() => goToDetail(item)}>
                          {item.reportCount}
                        </td>
                      ) : (
                        <td className="cursor-pointer" onClick={() => goToDetail(item)}>
                          <span
                            className={`rounded-md px-2.5 py-1 text-xs font-semibold ${
                              item.status === 'RESOLVED'
                                ? 'bg-primary-container text-on-primary-container'
                                : 'bg-surface-container-high text-on-surface-variant'
                            }`}
                          >
                            {REPORT_STATUS_LABELS[item.status]}
                          </span>
                        </td>
                      )}
                      <td
                        className="cursor-pointer whitespace-nowrap text-on-surface-variant"
                        onClick={() => goToDetail(item)}
                      >
                        {formatDateTime(item.reportedAt)}
                      </td>
                      <td>
                        <div className="flex flex-nowrap gap-2">
                          <button
                            type="button"
                            onClick={() => goToDetail(item)}
                            className="whitespace-nowrap text-xs font-semibold text-primary"
                          >
                            Xem chi tiết
                          </button>
                          {tab === 'PROCESSED' && (
                            <button
                              type="button"
                              onClick={() => { setRevertError(''); setRevertTarget(item); }}
                              className="whitespace-nowrap rounded-md bg-surface-container-highest px-3 py-1.5 text-xs font-semibold text-on-surface"
                            >
                              Hoàn tác
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {items.length === 0 && (
                    <tr>
                      <td colSpan={6} className="text-center text-outline">
                        {tab === 'PENDING' ? 'Không có báo cáo nào đang chờ xử lý.' : 'Chưa có báo cáo nào được xử lý.'}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
        </div>
      </main>

      <ConfirmDialog
        key={revertTarget ? revertTarget.id : 'none'}
        open={revertTarget !== null}
        title="Hoàn tác báo cáo này?"
        description={
          revertTarget
            ? `Báo cáo sẽ quay lại hàng đợi "Báo cáo" để xử lý lại (${TARGET_TYPE_LABELS[revertTarget.targetType]} — ${REPORT_STATUS_LABELS[revertTarget.status]}).`
            : undefined
        }
        icon="undo"
        tone="default"
        confirmLabel="Hoàn tác"
        submitting={revertSubmitting}
        errorText={revertError}
        onConfirm={confirmRevert}
        onCancel={() => setRevertTarget(null)}
      />
    </div>
  );
}
