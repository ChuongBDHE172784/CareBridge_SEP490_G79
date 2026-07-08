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
    } catch {
      setError('Không tải được dữ liệu tổng quan cộng đồng.');
      setData(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const pendingReports = data?.reportMetrics.byStatus['PENDING'] ?? 0;

  return (
    <div className="min-h-screen bg-[#F6F1EC]">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
          <div>
            <h2 className="text-3xl font-bold text-[#271812]">Tổng quan cộng đồng</h2>
            <p className="text-[#524440] mt-1">Số liệu tổng hợp về người dùng, nội dung và báo cáo trong hệ thống</p>
          </div>
          <button
            onClick={load}
            className="h-[52px] px-6 rounded-full bg-[#C98C7B] text-white font-semibold hover:opacity-90 transition-opacity flex items-center gap-2 shadow-md"
          >
            <span className="material-symbols-outlined">refresh</span> Làm mới
          </button>
        </div>

        {loading ? (
          <div className="flex justify-center py-24">
            <div className="animate-spin rounded-full h-10 w-10 border-4 border-[#845143] border-t-transparent" />
          </div>
        ) : error || !data ? (
          <div className="bg-red-50 border border-red-200 rounded-2xl p-6 text-red-600 text-sm">{error}</div>
        ) : (
          <>
            {/* KPI Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
                <div className="w-12 h-12 rounded-full bg-[#FFDBD1] flex items-center justify-center mb-4">
                  <span className="material-symbols-outlined text-[#845143]">group</span>
                </div>
                <p className="text-[#84736F] text-sm mb-1">Tổng người dùng</p>
                <h3 className="text-3xl font-bold text-[#271812]">{formatNumber(data.userMetrics.total)}</h3>
                <p className="text-xs text-[#84736F] mt-1">{formatNumber(data.userMetrics.active)} đang hoạt động</p>
              </div>
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
                <div className="w-12 h-12 rounded-full bg-[#F8DDD2] flex items-center justify-center mb-4">
                  <span className="material-symbols-outlined text-[#6E5A52]">forum</span>
                </div>
                <p className="text-[#84736F] text-sm mb-1">Câu hỏi cộng đồng</p>
                <h3 className="text-3xl font-bold text-[#271812]">{formatNumber(data.questionMetrics.total)}</h3>
                <p className="text-xs text-[#84736F] mt-1">+{formatNumber(data.questionMetrics.newInPeriod)} trong kỳ</p>
              </div>
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
                <div className="w-12 h-12 rounded-full bg-[#E9E1DB] flex items-center justify-center mb-4">
                  <span className="material-symbols-outlined text-[#625D59]">chat_bubble</span>
                </div>
                <p className="text-[#84736F] text-sm mb-1">Câu trả lời</p>
                <h3 className="text-3xl font-bold text-[#271812]">{formatNumber(data.answerMetrics.total)}</h3>
                <p className="text-xs text-[#84736F] mt-1">+{formatNumber(data.answerMetrics.newInPeriod)} trong kỳ</p>
              </div>
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
                <div className="w-12 h-12 rounded-full bg-[#FFDAD6] flex items-center justify-center mb-4">
                  <span className="material-symbols-outlined text-[#BA1A1A]">report</span>
                </div>
                <p className="text-[#84736F] text-sm mb-1">Báo cáo chờ xử lý</p>
                <h3 className="text-3xl font-bold text-[#271812]">{formatNumber(pendingReports)}</h3>
                <p className="text-xs text-[#84736F] mt-1">TB xử lý: {formatSeconds(data.reportMetrics.avgHandlingTimeSeconds)}</p>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Report status breakdown */}
              <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm border border-[#FFE2D9]">
                <h3 className="text-lg font-semibold text-[#271812] mb-4">Trạng thái báo cáo</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="border-b border-[#FFE9E3] text-[#84736F] text-xs uppercase tracking-wider">
                        <th className="pb-3 font-medium">Trạng thái</th>
                        <th className="pb-3 font-medium text-right">Số lượng</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(data.reportMetrics.byStatus).map(([status, count]) => (
                        <tr key={status} className="border-b border-[#FFE9E3] last:border-0">
                          <td className="py-3 text-[#524440]">{REPORT_STATUS_LABELS[status] ?? status}</td>
                          <td className="py-3 text-right font-semibold text-[#271812]">{formatNumber(count)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <h3 className="text-lg font-semibold text-[#271812] mt-6 mb-4">Người dùng theo vai trò</h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {Object.entries(data.userMetrics.byRole).map(([role, count]) => (
                    <div key={role} className="bg-[#FFF8F6] rounded-xl px-4 py-3">
                      <p className="text-xs text-[#84736F]">{role}</p>
                      <p className="text-lg font-bold text-[#271812]">{formatNumber(count)}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Trending topics */}
              <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#FFE2D9]">
                <h3 className="text-lg font-semibold text-[#271812] mb-4">Chủ đề nổi bật</h3>
                {data.trendingTopics.length === 0 ? (
                  <p className="text-sm text-[#84736F]">Chưa có dữ liệu chủ đề nổi bật.</p>
                ) : (
                  <div className="space-y-4">
                    {data.trendingTopics.map((t) => (
                      <div key={t.topicId} className="flex justify-between items-center">
                        <p className="text-sm text-[#271812] font-medium">{t.topicName}</p>
                        <span className="text-xs bg-[#FFE9E3] text-[#845143] px-2 py-1 rounded-full font-semibold">
                          {formatNumber(t.questionCount)} câu hỏi
                        </span>
                      </div>
                    ))}
                  </div>
                )}
                <p className="text-xs text-[#84736F] mt-6 pt-4 border-t border-[#FFE9E3]">
                  Cập nhật lúc {new Date(data.generatedAt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })}
                </p>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
