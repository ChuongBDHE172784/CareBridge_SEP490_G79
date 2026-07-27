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
    <div className="portal-page font-sans">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="p-8">
          <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <h1 className="text-[26px] font-bold text-on-surface m-0">Tổng quan cộng đồng</h1>
              <p className="text-on-surface-variant text-sm mt-1">Số liệu tổng hợp về người dùng, nội dung và báo cáo trong hệ thống.</p>
            </div>
            <button
              onClick={load}
              className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low self-start md:self-auto"
            >
              <span className="material-symbols-outlined text-lg">refresh</span> Làm mới
            </button>
          </div>

          {loading ? (
            <div className="py-12 text-center text-outline">Đang tải dữ liệu tổng quan...</div>
          ) : error || !data ? (
            <div className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">{error}</div>
          ) : (
            <>
              {/* KPI Grid */}
              <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Tổng người dùng</span>
                    <p className="text-2xl font-bold text-on-surface m-0">{formatNumber(data.userMetrics.total)}</p>
                    <p className="mt-1 text-xs text-outline">{formatNumber(data.userMetrics.active)} đang hoạt động</p>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-primary/70">group</span>
                </div>
                <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Câu hỏi cộng đồng</span>
                    <p className="text-2xl font-bold text-on-surface m-0">{formatNumber(data.questionMetrics.total)}</p>
                    <p className="mt-1 text-xs text-outline">+{formatNumber(data.questionMetrics.newInPeriod)} trong kỳ</p>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-primary/70">forum</span>
                </div>
                <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Câu trả lời</span>
                    <p className="text-2xl font-bold text-on-surface m-0">{formatNumber(data.answerMetrics.total)}</p>
                    <p className="mt-1 text-xs text-outline">+{formatNumber(data.answerMetrics.newInPeriod)} trong kỳ</p>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-primary/70">chat_bubble</span>
                </div>
                <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Báo cáo chờ xử lý</span>
                    <p className="text-2xl font-bold text-error m-0">{formatNumber(pendingReports)}</p>
                    <p className="mt-1 text-xs text-outline">TB xử lý: {formatSeconds(data.reportMetrics.avgHandlingTimeSeconds)}</p>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-error/80">report</span>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                {/* Report status breakdown & roles */}
                <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest lg:col-span-2 space-y-6">
                  <div>
                    <h3 className="mb-4 text-base font-bold text-on-surface m-0">Trạng thái báo cáo</h3>
                    <div className="overflow-x-auto">
                      <table className="w-full border-collapse">
                        <thead>
                          <tr className="border-b-2 border-surface-container-highest text-left">
                            <th className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">TRẠNG THÁI</th>
                            <th className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] text-right">SỐ LƯỢNG</th>
                          </tr>
                        </thead>
                        <tbody>
                          {Object.entries(data.reportMetrics.byStatus).map(([status, count]) => (
                            <tr key={status} className="border-b border-surface-container-highest hover:bg-surface-bright">
                              <td className="py-3.5 px-2 text-sm text-on-surface font-medium">{REPORT_STATUS_LABELS[status] ?? status}</td>
                              <td className="py-3.5 px-2 text-sm font-bold text-on-surface text-right">{formatNumber(count)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <div>
                    <h3 className="mb-4 text-base font-bold text-on-surface m-0">Người dùng theo vai trò</h3>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                      {Object.entries(data.userMetrics.byRole).map(([role, count]) => (
                        <div key={role} className="rounded-2xl border border-surface-container-highest bg-surface-container-low p-4">
                          <p className="text-xs font-semibold text-outline uppercase tracking-wider mb-1">{role}</p>
                          <p className="text-xl font-bold text-on-surface m-0">{formatNumber(count)}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Trending topics */}
                <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest flex flex-col justify-between">
                  <div>
                    <h3 className="mb-4 text-base font-bold text-on-surface m-0">Chủ đề nổi bật</h3>
                    {data.trendingTopics.length === 0 ? (
                      <p className="text-sm text-on-surface-variant">Chưa có dữ liệu chủ đề nổi bật.</p>
                    ) : (
                      <div className="space-y-4">
                        {data.trendingTopics.map((t) => (
                          <div key={t.topicId} className="flex justify-between items-center p-3 rounded-2xl bg-surface-container-low border border-surface-container-highest">
                            <p className="text-sm font-semibold text-on-surface m-0">{t.topicName}</p>
                            <span className="rounded-full bg-surface-container text-primary px-3 py-1 text-xs font-semibold">
                              {formatNumber(t.questionCount)} câu hỏi
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  <p className="mt-6 border-t border-surface-container-highest pt-3 text-xs text-outline m-0">
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
