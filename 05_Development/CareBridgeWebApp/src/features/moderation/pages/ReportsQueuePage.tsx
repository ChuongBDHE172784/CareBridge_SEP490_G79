import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import { useAuthStore } from '../../../shared/auth/authStore';
import { claimReport, fetchModerationQueue, releaseReport, revertReport } from '../services/moderationApi';
import type { ModerationQueueItem, ReportSource } from '../models/moderation';
import {
  CASE_PRIORITY_LABELS,
  CASE_PRIORITY_STYLES,
  formatReportReason,
  REPORT_SOURCE_LABELS,
  REPORT_STATUS_LABELS,
  TARGET_TYPE_LABELS,
} from '../models/moderation';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

type Tab = 'PENDING' | 'PROCESSED';

const TABS: { label: string; value: Tab }[] = [
  { label: 'Báo cáo', value: 'PENDING' },
  { label: 'Đã xử lý', value: 'PROCESSED' },
];

type SourceFilter = 'ALL' | ReportSource;

export default function ReportsQueuePage() {
  const navigate = useNavigate();
  const currentUserId = useAuthStore((s) => s.user?.id ?? null);
  const [tab, setTab] = useState<Tab>('PENDING');
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>('ALL');
  const [items, setItems] = useState<ModerationQueueItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [claimingId, setClaimingId] = useState<string | null>(null);

  const [revertTarget, setRevertTarget] = useState<ModerationQueueItem | null>(null);
  const [revertSubmitting, setRevertSubmitting] = useState(false);
  const [revertError, setRevertError] = useState('');

  const loadReports = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const source = sourceFilter === 'ALL' ? undefined : sourceFilter;
      if (tab === 'PENDING') {
        // CB-MOD-IMP-016: the open queue is PENDING + IN_REVIEW (claimed) — GET /queue accepts a
        // single status per call (ADR-003, CB-MOD-IMP-015), so fetch both and merge client-side.
        const [pending, inReview] = await Promise.all([
          fetchModerationQueue({ status: 'PENDING', source, size: 50 }),
          fetchModerationQueue({ status: 'IN_REVIEW', source, size: 50 }),
        ]);
        const merged = [...pending.content, ...inReview.content].sort(
          (a, b) => new Date(b.reportedAt).getTime() - new Date(a.reportedAt).getTime(),
        );
        setItems(merged);
      } else {
        const [resolved, dismissed] = await Promise.all([
          fetchModerationQueue({ status: 'RESOLVED', source, size: 50 }),
          fetchModerationQueue({ status: 'DISMISSED', source, size: 50 }),
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
  }, [tab, sourceFilter]);

  useEffect(() => { loadReports(); }, [loadReports]);

  const goToDetail = (item: ModerationQueueItem) => {
    if (item.targetType === 'ACCOUNT' || item.targetType === 'USER' || item.targetType === 'EXPERT') {
      navigate(`/moderator/reports/account/${item.id}`);
    } else {
      navigate(`/moderator/reports/${item.id}`);
    }
  };

  // CB-MOD-IMP-016: atomic claim — the backend guarantees only one moderator wins (MOD-036 on race)
  const handleClaim = async (item: ModerationQueueItem) => {
    setActionError('');
    setClaimingId(item.id);
    try {
      await claimReport(item.id);
      await loadReports();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Không thể nhận xử lý báo cáo này (có thể đã có người nhận).');
      await loadReports();
    } finally {
      setClaimingId(null);
    }
  };

  const handleRelease = async (item: ModerationQueueItem) => {
    setActionError('');
    setClaimingId(item.id);
    try {
      await releaseReport(item.id);
      await loadReports();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Không thể trả lại báo cáo này.');
    } finally {
      setClaimingId(null);
    }
  };

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
            <p className="portal-subtitle">
              Theo dõi các báo cáo do người dùng gửi và các trường hợp do AI phát hiện. AI chỉ hỗ trợ
              đánh giá — quyết định cuối cùng luôn thuộc về kiểm duyệt viên.
            </p>
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
          <select
            value={sourceFilter}
            onChange={(e) => setSourceFilter(e.target.value as SourceFilter)}
            className="ml-auto rounded-md border border-outline-variant bg-surface px-3 py-2 text-sm text-on-surface"
            aria-label="Lọc theo nguồn báo cáo"
          >
            <option value="ALL">Tất cả nguồn</option>
            <option value="USER">Người dùng báo cáo</option>
            <option value="AUTOMATED">AI phát hiện</option>
          </select>
        </div>

        {actionError && <div className="portal-error">{actionError}</div>}

        {isLoading ? (
          <div className="portal-empty">Đang tải...</div>
        ) : error ? (
          <div className="portal-error">{error}</div>
        ) : (
          <div className="portal-table-card">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[980px]">
                <thead>
                  <tr>
                    {(tab === 'PENDING'
                      ? ['LÝ DO', 'NGUỒN', 'LOẠI', 'NỘI DUNG XEM TRƯỚC', 'SỐ LƯỢT', 'THỜI GIAN', '']
                      : ['LÝ DO', 'NGUỒN', 'LOẠI', 'NỘI DUNG XEM TRƯỚC', 'TRẠNG THÁI', 'THỜI GIAN', '']
                    ).map((h) => (
                      <th key={h}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.id}>
                      <td className="cursor-pointer" onClick={() => goToDetail(item)}>
                        <div className="flex flex-wrap items-center gap-1.5">
                          <span className="rounded-md bg-error-container px-2.5 py-1 text-xs font-semibold text-error">
                            {formatReportReason(item.reportReason)}
                          </span>
                          {item.priority && item.priority !== 'NORMAL' && (
                            <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${CASE_PRIORITY_STYLES[item.priority]}`}>
                              {CASE_PRIORITY_LABELS[item.priority]}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="cursor-pointer" onClick={() => goToDetail(item)}>
                        {item.reportSource === 'AUTOMATED' ? (
                          <span className="inline-flex items-center gap-1 rounded-md bg-secondary-container px-2 py-0.5 text-xs font-semibold text-on-secondary-container">
                            <span className="material-symbols-outlined text-sm leading-none">smart_toy</span>
                            AI phát hiện
                          </span>
                        ) : (
                          <span className="text-xs text-on-surface-variant">{REPORT_SOURCE_LABELS.USER}</span>
                        )}
                      </td>
                      <td className="cursor-pointer text-on-surface-variant" onClick={() => goToDetail(item)}>
                        {TARGET_TYPE_LABELS[item.targetType]}
                      </td>
                      <td
                        className="max-w-[320px] cursor-pointer truncate text-on-surface"
                        onClick={() => goToDetail(item)}
                      >
                        {item.contentPreview}
                      </td>
                      {tab === 'PENDING' ? (
                        <td className="cursor-pointer text-on-surface-variant" onClick={() => goToDetail(item)}>
                          {item.status === 'IN_REVIEW' ? (
                            <span className="rounded-md bg-tertiary-container px-2 py-0.5 text-xs font-semibold text-on-tertiary-container">
                              {item.assignedModeratorId === currentUserId ? 'Bạn đang xem xét' : 'Đang xem xét'}
                            </span>
                          ) : (
                            item.reportCount
                          )}
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
                          {tab === 'PENDING' && item.status === 'PENDING' && (
                            <button
                              type="button"
                              disabled={claimingId === item.id}
                              onClick={() => handleClaim(item)}
                              className="whitespace-nowrap rounded-md bg-primary px-3 py-1.5 text-xs font-semibold text-on-primary disabled:opacity-60"
                            >
                              {claimingId === item.id ? 'Đang nhận...' : 'Nhận xử lý'}
                            </button>
                          )}
                          {tab === 'PENDING'
                            && item.status === 'IN_REVIEW'
                            && item.assignedModeratorId === currentUserId && (
                            <button
                              type="button"
                              disabled={claimingId === item.id}
                              onClick={() => handleRelease(item)}
                              className="whitespace-nowrap rounded-md bg-surface-container-highest px-3 py-1.5 text-xs font-semibold text-on-surface disabled:opacity-60"
                            >
                              Trả lại
                            </button>
                          )}
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
                      <td colSpan={7} className="text-center text-outline">
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
