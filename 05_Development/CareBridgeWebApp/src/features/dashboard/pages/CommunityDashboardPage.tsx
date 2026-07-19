import { useEffect, useState, useCallback } from 'react';
import ModPortalSidebar from '../../moderation/components/ModPortalSidebar';
import { fetchCommunityDashboard } from '../services/dashboardApi';
import type { CommunityDashboardResponse } from '../models/dashboard';

const REPORT_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Đang chờ xử lý',
  RESOLVED: 'Đã xử lý',
  DISMISSED: 'Đã bỏ qua',
};

function formatNumber(n: number): string {
  return n.toLocaleString('vi-VN');
}

function formatSeconds(s: number | null): string {
  if (s == null) return '—';
  const hours = s / 3600;
  if (hours >= 1) return `${hours.toFixed(1)} giờ`;
  return `${Math.round(s / 60)} phút`;
}

function getDashboardErrorMessage(error: unknown): string {
  const err = error as { response?: { status?: number; data?: { message?: string; error?: string } } };
  const status = err.response?.status;
  const message = err.response?.data?.message;
  const code = err.response?.data?.error;
  if (status === 403) return 'Tài khoản hiện tại không có quyền xem tổng quan cộng đồng.';
  if (status === 500) return `Máy chủ gặp lỗi khi tổng hợp dữ liệu cộng đồng${code ? ` (${code})` : ''}.`;
  if (status) return message ?? `Không tải được dữ liệu tổng quan cộng đồng (${status}).`;
  return 'Không kết nối được máy chủ khi tải dữ liệu tổng quan cộng đồng.';
}

export default function CommunityDashboardPage() {
  const [data, setData] = useState<CommunityDashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetchCommunityDashboard({});
      setData(res);
    } catch (err) {
      setError(getDashboardErrorMessage(err));
      setData(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const pendingReports = data?.reportMetrics.byStatus['PENDING'] ?? 0;

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">ModPortal</p>
            <h2 className="portal-title">Tổng quan cộng đồng</h2>
            <p className="portal-subtitle">Số liệu tổng hợp về người dùng, nội dung và báo cáo trong hệ thống.</p>
          </div>
          <button
            onClick={load}
            className="portal-primary-button"
          >
            <span className="material-symbols-outlined text-base">refresh</span> Làm mới
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center py-24">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : error || !data ? (
          <div className="portal-error">{error}</div>
        ) : (
          <>
            {/* KPI Grid */}
            <div className="mb-5 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
              <div className="portal-stat-card">
                <div className="portal-icon mb-3">
                  <span className="material-symbols-outlined text-lg">group</span>
                </div>
                <p className="mb-1 text-xs font-medium text-on-surface-variant">Tổng người dùng</p>
                <h3 className="text-2xl font-semibold text-on-surface">{formatNumber(data.userMetrics.total)}</h3>
                <p className="mt-1 text-xs text-outline">{formatNumber(data.userMetrics.active)} đang hoạt động</p>
              </div>
              <div className="portal-stat-card">
                <div className="portal-icon mb-3">
                  <span className="material-symbols-outlined text-lg">forum</span>
                </div>
                <p className="mb-1 text-xs font-medium text-on-surface-variant">Câu hỏi cộng đồng</p>
                <h3 className="text-2xl font-semibold text-on-surface">{formatNumber(data.questionMetrics.total)}</h3>
                <p className="mt-1 text-xs text-outline">+{formatNumber(data.questionMetrics.newInPeriod)} trong kỳ</p>
              </div>
              <div className="portal-stat-card">
                <div className="portal-icon mb-3">
                  <span className="material-symbols-outlined text-lg">chat_bubble</span>
                </div>
                <p className="mb-1 text-xs font-medium text-on-surface-variant">Câu trả lời</p>
                <h3 className="text-2xl font-semibold text-on-surface">{formatNumber(data.answerMetrics.total)}</h3>
                <p className="mt-1 text-xs text-outline">+{formatNumber(data.answerMetrics.newInPeriod)} trong kỳ</p>
              </div>
              <div className="portal-stat-card">
                <div className="mb-3 flex h-8 w-8 items-center justify-center rounded-md bg-error-container text-error">
                  <span className="material-symbols-outlined text-lg">report</span>
                </div>
                <p className="mb-1 text-xs font-medium text-on-surface-variant">Báo cáo chờ xử lý</p>
                <h3 className="text-2xl font-semibold text-on-surface">{formatNumber(pendingReports)}</h3>
                <p className="mt-1 text-xs text-outline">TB xử lý: {formatSeconds(data.reportMetrics.avgHandlingTimeSeconds)}</p>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
              {/* Report status breakdown */}
              <div className="portal-card-padded lg:col-span-2">
                <h3 className="mb-3 text-sm font-semibold text-on-surface">Trạng thái báo cáo</h3>
                <div className="overflow-x-auto">
                  <table>
                    <thead>
                      <tr>
                        <th>Trạng thái</th>
                        <th className="text-right">Số lượng</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(data.reportMetrics.byStatus).map(([status, count]) => (
                        <tr key={status}>
                          <td>{REPORT_STATUS_LABELS[status] ?? status}</td>
                          <td className="text-right font-semibold">{formatNumber(count)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <h3 className="mb-3 mt-5 text-sm font-semibold text-on-surface">Người dùng theo vai trò</h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {Object.entries(data.userMetrics.byRole).map(([role, count]) => (
                    <div key={role} className="rounded-md border border-outline-variant/60 bg-surface-container-low px-3 py-2">
                      <p className="text-[11px] text-outline">{role}</p>
                      <p className="text-base font-semibold text-on-surface">{formatNumber(count)}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Trending topics */}
              <div className="portal-card-padded">
                <h3 className="mb-3 text-sm font-semibold text-on-surface">Chủ đề nổi bật</h3>
                {data.trendingTopics.length === 0 ? (
                  <p className="text-sm text-on-surface-variant">Chưa có dữ liệu chủ đề nổi bật.</p>
                ) : (
                  <div className="space-y-4">
                    {data.trendingTopics.map((t) => (
                      <div key={t.topicId} className="flex justify-between items-center">
                        <p className="text-sm font-medium text-on-surface">{t.topicName}</p>
                        <span className="rounded bg-primary-container px-2 py-1 text-xs font-semibold text-primary">
                          {formatNumber(t.questionCount)} câu hỏi
                        </span>
                      </div>
                    ))}
                  </div>
                )}
                <p className="mt-5 border-t border-outline-variant/60 pt-3 text-xs text-outline">
                  Cập nhật lúc {new Date(data.generatedAt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })}
                </p>
              </div>
            </div>
          </>
        )}
        </div>
      </main>
    </div>
  );
}
