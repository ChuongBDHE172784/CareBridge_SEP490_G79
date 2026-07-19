import { useEffect, useState, useCallback } from 'react';
import ModPortalSidebar from '../../moderation/components/ModPortalSidebar';
import { fetchImpactReport } from '../services/dashboardApi';
import type { ImpactReportResponse } from '../models/dashboard';

function formatNumber(n: number): string {
  return n.toLocaleString('vi-VN');
}

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN');
}

function toCsv(data: ImpactReportResponse): string {
  const rows = [
    ['Chỉ số', 'Giá trị'],
    ['Mẹ được hỗ trợ', String(data.mothersServed)],
    ['Buổi tư vấn đã thực hiện', String(data.consultationsDelivered)],
    ['Đối tác đang hoạt động', String(data.activePartnerOrganizations)],
    ['Nội dung đã xuất bản', String(data.publishedContentItems)],
    ['Từ ngày', formatDate(data.periodFrom)],
    ['Đến ngày', formatDate(data.periodTo)],
  ];
  return rows.map((r) => r.join(',')).join('\n');
}

export default function ImpactReportDashboardPage() {
  const [data, setData] = useState<ImpactReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetchImpactReport({ from: from || undefined, to: to || undefined });
      setData(res);
    } catch (err: any) {
      const code = err?.response?.data?.error;
      setError(code === 'MOD-022' ? 'Khoảng thời gian không hợp lệ: "Từ ngày" phải trước "Đến ngày".' : 'Không tải được báo cáo tác động.');
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  useEffect(() => { load(); }, [load]);

  const handleExport = () => {
    if (!data) return;
    const blob = new Blob([toCsv(data)], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `bao-cao-tac-dong-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">ModPortal</p>
            <h2 className="portal-title">Tác động &amp; vận hành</h2>
            <p className="portal-subtitle">Tổng hợp số liệu tác động của nền tảng theo khoảng thời gian.</p>
          </div>
          <div className="flex items-center gap-3 flex-wrap">
            <input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="portal-field"
            />
            <span className="text-xs text-outline">đến</span>
            <input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="portal-field"
            />
            <button
              onClick={load}
              className="portal-secondary-button"
            >
              Áp dụng
            </button>
            <button
              onClick={handleExport}
              disabled={!data}
              className="portal-primary-button"
            >
              <span className="material-symbols-outlined text-base">download</span>
              Xuất báo cáo
            </button>
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-24">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : error || !data ? (
          <div className="portal-error">{error}</div>
        ) : (
          <>
            <div className="mb-5 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
              <div className="portal-stat-card">
                <div className="portal-icon mb-3">
                  <span className="material-symbols-outlined text-lg">favorite</span>
                </div>
                <p className="mb-1 text-xs font-medium text-on-surface-variant">Mẹ được hỗ trợ</p>
                <h3 className="text-2xl font-semibold text-on-surface">{formatNumber(data.mothersServed)}</h3>
              </div>
              <div className="portal-stat-card">
                <div className="portal-icon mb-3">
                  <span className="material-symbols-outlined text-lg">stethoscope</span>
                </div>
                <p className="mb-1 text-xs font-medium text-on-surface-variant">Buổi tư vấn đã thực hiện</p>
                <h3 className="text-2xl font-semibold text-on-surface">{formatNumber(data.consultationsDelivered)}</h3>
              </div>
              <div className="portal-stat-card">
                <div className="portal-icon mb-3">
                  <span className="material-symbols-outlined text-lg">handshake</span>
                </div>
                <p className="mb-1 text-xs font-medium text-on-surface-variant">Đối tác đang hoạt động</p>
                <h3 className="text-2xl font-semibold text-on-surface">{formatNumber(data.activePartnerOrganizations)}</h3>
              </div>
              <div className="portal-stat-card">
                <div className="portal-icon mb-3">
                  <span className="material-symbols-outlined text-lg">menu_book</span>
                </div>
                <p className="mb-1 text-xs font-medium text-on-surface-variant">Nội dung đã xuất bản</p>
                <h3 className="text-2xl font-semibold text-on-surface">{formatNumber(data.publishedContentItems)}</h3>
              </div>
            </div>

            <div className="portal-card-padded">
              <div className="mb-2 flex items-center gap-2 text-sm text-on-surface-variant">
                <span className="material-symbols-outlined text-[18px]">info</span>
                <span>
                  Kỳ báo cáo: {formatDate(data.periodFrom)} — {formatDate(data.periodTo)}
                </span>
              </div>
              <p className="text-xs text-outline">{data.anonymizationNote}</p>
              <p className="mt-1 text-xs text-outline">
                Cập nhật lúc {new Date(data.generatedAt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })}
              </p>
            </div>
          </>
        )}
        </div>
      </main>
    </div>
  );
}
