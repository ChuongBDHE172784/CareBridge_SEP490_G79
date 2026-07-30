import { useEffect, useState, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { fetchCommunityDashboard } from '../services/dashboardApi';
import {
  fetchPendingContentQueue,
  fetchAccountViolationHistory,
} from '../../moderation/services/moderationApi';
import type { CommunityDashboardResponse } from '../models/dashboard';
import type { AccountViolationSummaryItem } from '../../moderation/models/moderation';
import { ACTION_TYPE_LABELS } from '../../moderation/models/moderation';

function formatNumber(n: number): string {
  return n.toLocaleString('vi-VN');
}

function formatSeconds(s: number | null): string {
  if (s == null || Number.isNaN(s)) return '—';
  const hours = s / 3600;
  if (hours >= 1) return `${hours.toFixed(1)} giờ`;
  const mins = Math.round(s / 60);
  return `${mins} phút`;
}

function formatActionDate(iso: string | null): string {
  if (!iso) return '—';
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('vi-VN');
}

function getDashboardErrorMessage(error: unknown): string {
  const err = error as { response?: { status?: number; data?: { message?: string; error?: string } } };
  const status = err.response?.status;
  const message = err.response?.data?.message;
  const code = err.response?.data?.error;
  if (status === 403) return 'Tài khoản hiện tại không có quyền xem tổng quan hệ thống kiểm duyệt.';
  if (status === 500) return `Máy chủ gặp lỗi khi tổng hợp dữ liệu kiểm duyệt${code ? ` (${code})` : ''}.`;
  if (status) return message ?? `Không tải được dữ liệu tổng quan kiểm duyệt (${status}).`;
  return 'Không kết nối được máy chủ khi tải dữ liệu tổng quan kiểm duyệt.';
}

/* ------------------------------------------------------------------ */
/*  Donut Chart Component for Report Status Breakdown                  */
/* ------------------------------------------------------------------ */
function ReportStatusDonutChart({
  pending,
  resolved,
  dismissed,
}: {
  pending: number;
  resolved: number;
  dismissed: number;
}) {
  const [activeKey, setActiveKey] = useState<string | null>(null);

  const total = pending + resolved + dismissed;
  const radius = 38;
  const circumference = 2 * Math.PI * radius; // ~238.76

  // Calculate gaps between segments if total > 1
  const gap = total > 1 ? 4 : 0;
  const activeSegmentsCount = [pending, resolved, dismissed].filter((c) => c > 0).length;
  const totalGap = activeSegmentsCount > 1 ? gap * activeSegmentsCount : 0;
  const availableCircumference = Math.max(0, circumference - totalGap);

  const items = [
    {
      key: 'PENDING',
      label: 'Đang chờ xử lý',
      count: pending,
      color: '#f59e0b',
      bgColor: 'bg-amber-500',
      badgeClass: 'bg-amber-50 text-amber-700 border-amber-200',
    },
    {
      key: 'RESOLVED',
      label: 'Đã xử lý thành công',
      count: resolved,
      color: '#10b981',
      bgColor: 'bg-emerald-600',
      badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    },
    {
      key: 'DISMISSED',
      label: 'Đã bỏ qua (Spam / Không hợp lệ)',
      count: dismissed,
      color: '#94a3b8',
      bgColor: 'bg-slate-400',
      badgeClass: 'bg-slate-50 text-slate-700 border-slate-200',
    },
  ];

  let currentOffset = 0;
  const segments = items.map((item) => {
    const percent = total > 0 ? item.count / total : 0;
    const len = total > 0 && item.count > 0 ? percent * availableCircumference : 0;
    const strokeDasharray = `${len} ${circumference - len}`;
    const strokeDashoffset = -currentOffset;
    if (total > 0 && item.count > 0) {
      currentOffset += len + gap;
    }
    return {
      ...item,
      percentVal: total > 0 ? Math.round(percent * 100) : 0,
      strokeDasharray,
      strokeDashoffset,
    };
  });

  return (
    <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center pt-2">
      {/* Left: Interactive Donut Chart SVG (5 Cols) */}
      <div className="md:col-span-5 flex flex-col items-center justify-center p-2">
        <div className="relative w-48 h-48 flex items-center justify-center">
          <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
            {/* Track Circle */}
            <circle
              cx="50"
              cy="50"
              r={radius}
              fill="transparent"
              stroke="#f1f5f9"
              strokeWidth="9"
            />
            {total > 0 &&
              segments.map((seg) => {
                if (seg.count === 0) return null;
                const isActive = activeKey === seg.key;
                const isDimmed = activeKey !== null && !isActive;
                return (
                  <circle
                    key={seg.key}
                    cx="50"
                    cy="50"
                    r={radius}
                    fill="transparent"
                    stroke={seg.color}
                    strokeWidth={isActive ? '12' : '9'}
                    strokeDasharray={seg.strokeDasharray}
                    strokeDashoffset={seg.strokeDashoffset}
                    strokeLinecap="round"
                    onMouseEnter={() => setActiveKey(seg.key)}
                    onMouseLeave={() => setActiveKey(null)}
                    className="transition-all duration-300 cursor-pointer"
                    style={{
                      opacity: isDimmed ? 0.35 : 1,
                      filter: isActive ? `drop-shadow(0 2px 6px ${seg.color}66)` : 'none',
                    }}
                  />
                );
              })}
          </svg>
          {/* Donut Center Label - Dynamic percentage on hover without text */}
          {(() => {
            const activeSeg = segments.find((s) => s.key === activeKey);
            const activePercentVal = activeSeg
              ? total > 0
                ? ((activeSeg.count / total) * 100).toFixed(1)
                : '0'
              : total > 0
                ? ((resolved / total) * 100).toFixed(1)
                : '100.0';
            const activeColor = activeSeg ? activeSeg.color : '#10b981';

            return (
              <div className="absolute inset-0 flex items-center justify-center text-center pointer-events-none p-3">
                <span
                  className="text-3xl font-extrabold tracking-tight leading-none transition-colors duration-300"
                  style={{ color: activeColor }}
                >
                  {activePercentVal}%
                </span>
              </div>
            );
          })()}
        </div>
      </div>

      {/* Right: Modern Status Breakdown Legend Cards (7 Cols) */}
      <div className="md:col-span-7 space-y-3">
        {segments.map((seg) => {
          const isActive = activeKey === seg.key;
          return (
            <div
              key={seg.key}
              onMouseEnter={() => setActiveKey(seg.key)}
              onMouseLeave={() => setActiveKey(null)}
              className={`group relative p-4 rounded-xl border transition-all cursor-pointer ${
                isActive
                  ? 'bg-surface border-primary/40 shadow-sm translate-x-1'
                  : 'bg-surface-container-low/60 border-outline-variant/40 hover:bg-surface hover:border-outline-variant/80'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2.5">
                  <span
                    className="w-3 h-3 rounded-full shrink-0 transition-transform group-hover:scale-125"
                    style={{ backgroundColor: seg.color }}
                  />
                  <span className="text-sm font-semibold text-on-surface">
                    {seg.label}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-bold text-on-surface">
                    {formatNumber(seg.count)} ca
                  </span>
                  <span className={`text-xs font-bold px-2 py-0.5 rounded-md border ${seg.badgeClass}`}>
                    {seg.percentVal}%
                  </span>
                </div>
              </div>

              {/* Progress indicator */}
              <div className="w-full h-1.5 rounded-full bg-surface-container-high overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${seg.bgColor}`}
                  style={{ width: `${seg.percentVal}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Quick Hub Items Data                                              */
/* ------------------------------------------------------------------ */
const QUICK_HUB_ITEMS = [
  {
    title: 'Nội dung mới chờ duyệt',
    description: 'Kho bài viết, câu hỏi và bình luận mới tạo chưa qua kiểm duyệt lần đầu.',
    icon: 'fact_check',
    path: '/moderator/pending-content',
    badgeText: 'Nội dung mới',
    iconBg: 'bg-blue-100 text-blue-700',
    badgeTone: 'bg-blue-50 text-blue-700 border-blue-200',
    actionText: 'Mở danh sách chờ duyệt',
  },
  {
    title: 'Báo cáo vi phạm',
    description: 'Hàng chờ xử lý báo cáo vi phạm từ người dùng và bộ lọc tự động.',
    icon: 'flag',
    path: '/moderator/reports',
    badgeText: 'Báo cáo vi phạm',
    iconBg: 'bg-amber-100 text-amber-800',
    badgeTone: 'bg-amber-50 text-amber-800 border-amber-200',
    actionText: 'Mở danh sách báo cáo',
  },
  {
    title: 'Xử lý Kỷ luật & Vi phạm',
    description: 'Nhật ký xử lý vi phạm, ẩn bài viết, cảnh cáo và hạn chế tài khoản.',
    icon: 'gavel',
    path: '/moderator/violations',
    badgeText: 'Kỷ luật tài khoản',
    iconBg: 'bg-purple-100 text-purple-700',
    badgeTone: 'bg-purple-50 text-purple-700 border-purple-200',
    actionText: 'Xem lịch sử vi phạm',
  },
];

export default function CommunityDashboardPage() {
  const navigate = useNavigate();
  const [data, setData] = useState<CommunityDashboardResponse | null>(null);
  const [pendingContentCount, setPendingContentCount] = useState(0);
  const [violationSummary, setViolationSummary] = useState<{
    items: AccountViolationSummaryItem[];
    total: number;
  }>({ items: [], total: 0 });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [dashRes, pendingContentRes, violationsRes] = await Promise.allSettled([
        fetchCommunityDashboard({}),
        fetchPendingContentQueue({ targetType: 'QUESTION', size: 1 }),
        fetchAccountViolationHistory({ page: 0, size: 5 }),
      ]);

      if (dashRes.status === 'fulfilled') {
        setData(dashRes.value);
      } else {
        setError(getDashboardErrorMessage(dashRes.reason));
      }

      if (pendingContentRes.status === 'fulfilled') {
        setPendingContentCount(pendingContentRes.value.totalElements);
      }

      if (violationsRes.status === 'fulfilled') {
        setViolationSummary({
          items: violationsRes.value.content,
          total: violationsRes.value.totalElements,
        });
      }
    } catch (err) {
      setError(getDashboardErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const pendingReports = data?.reportMetrics.byStatus['PENDING'] ?? 0;
  const resolvedReports = data?.reportMetrics.byStatus['RESOLVED'] ?? 0;
  const dismissedReports = data?.reportMetrics.byStatus['DISMISSED'] ?? 0;
  const totalReports = pendingReports + resolvedReports + dismissedReports;
  const resolutionRate = totalReports > 0 ? ((resolvedReports / totalReports) * 100).toFixed(1) : '100.0';

  return (
    <div className="p-8 font-sans space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Trung tâm Điều hành Kiểm duyệt</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Theo dõi chỉ số vận hành, hàng chờ duyệt và nhật ký xử lý vi phạm cộng đồng.
          </p>
        </div>

        <button
          type="button"
          onClick={loadAll}
          className="flex items-center gap-2 py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-on-surface-variant text-[13px] font-semibold cursor-pointer hover:bg-surface-container-low transition-colors self-start md:self-auto shadow-sm"
        >
          <span className="material-symbols-outlined text-lg">refresh</span> Làm mới dữ liệu
        </button>
      </div>

      {loading ? (
        <div className="py-16 text-center text-outline flex flex-col items-center justify-center gap-2 bg-surface rounded-2xl border border-outline-variant/60 shadow-sm">
          <span className="material-symbols-outlined text-3xl animate-spin text-primary">progress_activity</span>
          <span className="text-xs">Đang tổng hợp dữ liệu kiểm duyệt từ hệ thống...</span>
        </div>
      ) : error && !data ? (
        <div className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error shadow-sm">
          {error}
        </div>
      ) : (
        <>
          {/* Operational KPI Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* KPI 1: Pending Content */}
            <div
              onClick={() => navigate('/moderator/pending-content')}
              className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60 flex items-center justify-between cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all"
            >
              <div>
                <div className="text-[13px] text-outline font-semibold mb-1">Nội dung mới chờ duyệt</div>
                <div className="text-[28px] font-bold text-primary leading-tight">
                  {formatNumber(pendingContentCount)}
                </div>
                <div className="text-xs text-outline mt-1">Nội dung chưa duyệt lần đầu</div>
              </div>
              <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-2xl text-primary">pending_actions</span>
              </div>
            </div>

            {/* KPI 2: Pending Reports Backlog */}
            <div
              onClick={() => navigate('/moderator/reports')}
              className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60 flex items-center justify-between cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all"
            >
              <div>
                <div className="text-[13px] text-outline font-semibold mb-1">Báo cáo chờ xử lý</div>
                <div className="text-[28px] font-bold text-error leading-tight">
                  {formatNumber(pendingReports)}
                </div>
                <div className="text-xs text-outline mt-1">
                  TB xử lý: <span className="font-semibold text-on-surface">{formatSeconds(data?.reportMetrics.avgHandlingTimeSeconds ?? null)}</span>
                </div>
              </div>
              <div className="w-12 h-12 rounded-full bg-error-container/40 flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-2xl text-error">report</span>
              </div>
            </div>

            {/* KPI 3: Resolved Reports */}
            <div
              onClick={() => navigate('/moderator/reports')}
              className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60 flex items-center justify-between cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all"
            >
              <div>
                <div className="text-[13px] text-outline font-semibold mb-1">Đã xử lý vi phạm</div>
                <div className="text-[28px] font-bold text-[#137333] leading-tight">
                  {formatNumber(resolvedReports)}
                </div>
                <div className="text-xs text-outline mt-1">
                  Tỷ lệ hoàn thành: <span className="font-bold text-[#137333]">{resolutionRate}%</span>
                </div>
              </div>
              <div className="w-12 h-12 rounded-full bg-[#E6F4EA] flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-2xl text-[#137333]">check_circle</span>
              </div>
            </div>

            {/* KPI 4: Violation History */}
            <div
              onClick={() => navigate('/moderator/violations')}
              className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60 flex items-center justify-between cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all"
            >
              <div>
                <div className="text-[13px] text-outline font-semibold mb-1">Tài khoản có vi phạm</div>
                <div className="text-[28px] font-bold text-[#E65100] leading-tight">
                  {formatNumber(violationSummary.total)}
                </div>
                <div className="text-xs text-outline mt-1">Trường hợp đã xử lý kỷ luật</div>
              </div>
              <div className="w-12 h-12 rounded-full bg-[#FFF3E0] flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-2xl text-[#E65100]">gavel</span>
              </div>
            </div>
          </div>

          {/* Quick Access Hub */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {QUICK_HUB_ITEMS.map((item) => (
              <div
                key={item.title}
                onClick={() => navigate(item.path)}
                className="group bg-surface rounded-2xl p-5 shadow-sm border border-outline-variant/60 flex flex-col justify-between hover:shadow-md hover:border-primary/40 transition-all cursor-pointer"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${item.iconBg}`}>
                      <span className="material-symbols-outlined text-xl">{item.icon}</span>
                    </div>
                    <span className={`text-xs font-bold px-2.5 py-1 rounded-full border ${item.badgeTone}`}>
                      {item.badgeText}
                    </span>
                  </div>
                  <h3 className="text-base font-bold text-on-surface m-0 mb-1">{item.title}</h3>
                  <p className="text-xs text-on-surface-variant leading-relaxed m-0">{item.description}</p>
                </div>
                <div className="mt-4 pt-3 border-t border-outline-variant/40 flex items-center justify-between text-xs font-bold text-primary group-hover:text-primary-hover">
                  <span>{item.actionText}</span>
                  <span className="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">
                    arrow_forward
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Analytics Breakdown & Recent Violation Log */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left 2 Columns: Report status SVG Donut & Recent Violation Table */}
            <div className="lg:col-span-2 space-y-6">
              {/* Report Status Breakdown with SVG Donut Chart */}
              <div className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h2 className="text-base font-bold text-on-surface m-0">Phân tích Trạng thái Báo cáo</h2>
                    <p className="text-xs text-outline mt-0.5">Số lượng và tỷ lệ phân bổ các báo cáo vi phạm trong hệ thống</p>
                  </div>
                </div>

                <ReportStatusDonutChart
                  pending={pendingReports}
                  resolved={resolvedReports}
                  dismissed={dismissedReports}
                />
              </div>

              {/* Sample Table: Recent Account Violations Log */}
              <div className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h2 className="text-base font-bold text-on-surface m-0">Lịch sử Vi phạm gần nhất</h2>
                    <p className="text-xs text-outline mt-0.5">
                      Danh sách các ca vi phạm và hành động xử lý kỷ luật vừa thực hiện
                    </p>
                  </div>
                  <Link
                    to="/moderator/violations"
                    className="text-xs font-bold text-primary flex items-center gap-1 no-underline hover:underline"
                  >
                    Xem tất cả
                    <span className="material-symbols-outlined text-sm">arrow_forward</span>
                  </Link>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b border-outline-variant/60 text-left">
                        <th className="py-3 px-3 text-[11px] font-bold text-outline uppercase tracking-wider">Tài khoản vi phạm</th>
                        <th className="py-3 px-3 text-[11px] font-bold text-outline uppercase tracking-wider text-center">Số lượt vi phạm</th>
                        <th className="py-3 px-3 text-[11px] font-bold text-outline uppercase tracking-wider">Hành động gần nhất</th>
                        <th className="py-3 px-3 text-[11px] font-bold text-outline uppercase tracking-wider">Ngày xử lý</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-outline-variant/30">
                      {violationSummary.items.map((item) => (
                        <tr
                          key={item.targetUserId}
                          onClick={() => navigate(`/moderator/violations/${item.targetUserId}`)}
                          className="hover:bg-surface-container-low/60 transition-colors cursor-pointer"
                        >
                          <td className="py-3.5 px-3">
                            <div className="font-bold text-sm text-on-surface hover:text-primary">
                              {item.targetUserName || 'Người dùng CareBridge'}
                            </div>
                            <div className="text-[11px] text-outline font-mono">{item.targetUserId.slice(0, 8)}...</div>
                          </td>
                          <td className="py-3.5 px-3 text-center">
                            <span className="px-2.5 py-0.5 rounded-full bg-error-container/40 text-error text-xs font-bold">
                              {item.violationCount} lần
                            </span>
                          </td>
                          <td className="py-3.5 px-3">
                            <div className="text-xs font-semibold text-on-surface">
                              {ACTION_TYPE_LABELS[item.latestAction.actionType] ?? item.latestAction.actionType}
                            </div>
                            {item.latestAction.reason && (
                              <div className="text-[11px] text-outline line-clamp-1 mt-0.5">
                                {item.latestAction.reason}
                              </div>
                            )}
                          </td>
                          <td className="py-3.5 px-3 text-xs text-outline whitespace-nowrap">
                            {formatActionDate(item.latestAction.actionAt)}
                          </td>
                        </tr>
                      ))}
                      {violationSummary.items.length === 0 && (
                        <tr>
                          <td colSpan={4} className="py-8 text-center text-outline text-xs">
                            Chưa có dữ liệu lịch sử vi phạm tài khoản.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            {/* Right 1 Column: Trending topics & System timestamp */}
            <div className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60 flex flex-col justify-between">
              <div>
                <div className="mb-4">
                  <h2 className="text-base font-bold text-on-surface m-0">Chủ đề Nổi bật</h2>
                  <p className="text-xs text-outline mt-0.5">Các chủ đề cộng đồng nhận được nhiều câu hỏi nhất</p>
                </div>

                {!data || data.trendingTopics.length === 0 ? (
                  <div className="py-8 text-center text-outline text-xs">Chưa có dữ liệu chủ đề nổi bật.</div>
                ) : (
                  <div className="space-y-3">
                    {data.trendingTopics.map((t, idx) => (
                      <div
                        key={t.topicId}
                        className="flex items-center justify-between p-3 rounded-xl bg-surface-container-low border border-outline-variant/40"
                      >
                        <div className="flex items-center gap-2.5">
                          <span className="w-6 h-6 rounded-full bg-primary/10 text-primary text-xs font-bold flex items-center justify-center shrink-0">
                            {idx + 1}
                          </span>
                          <span className="text-sm font-semibold text-on-surface line-clamp-1">{t.topicName}</span>
                        </div>
                        <span className="rounded-full bg-surface-container-high text-primary px-2.5 py-0.5 text-xs font-bold whitespace-nowrap">
                          {formatNumber(t.questionCount)} câu hỏi
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="mt-6 pt-4 border-t border-outline-variant/40 text-[11px] text-outline flex items-center justify-between">
                <span>Trạng thái máy chủ: Bình thường</span>
                <span>
                  {data?.generatedAt ? new Date(data.generatedAt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '—'}
                </span>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
