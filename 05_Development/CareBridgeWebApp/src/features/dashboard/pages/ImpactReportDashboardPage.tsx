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
    <div className="min-h-screen bg-[#F6F1EC]">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <div className="flex flex-col md:flex-row md:items-center justify-between mb-8 gap-4">
          <div>
            <h2 className="text-3xl font-bold text-[#271812]">Tác động &amp; vận hành</h2>
            <p className="text-[#524440] mt-1">Tổng hợp số liệu tác động của nền tảng theo khoảng thời gian</p>
          </div>
          <div className="flex items-center gap-3 flex-wrap">
            <input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="bg-white border border-[#D6C2BD] rounded-lg px-4 py-2 text-sm h-[48px]"
            />
            <span className="text-[#84736F] text-sm">đến</span>
            <input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="bg-white border border-[#D6C2BD] rounded-lg px-4 py-2 text-sm h-[48px]"
            />
            <button
              onClick={load}
              className="h-[48px] px-4 rounded-lg bg-white border border-[#D6C2BD] text-[#524440] text-sm font-medium hover:bg-[#FFF1EC] transition-colors"
            >
              Áp dụng
            </button>
            <button
              onClick={handleExport}
              disabled={!data}
              className="bg-[#F6DACF] text-[#735E56] rounded-lg px-4 h-[48px] font-semibold flex items-center gap-2 hover:bg-[#FFE2D9] transition-colors disabled:opacity-50"
            >
              <span className="material-symbols-outlined text-[20px]">download</span>
              Xuất báo cáo
            </button>
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-24">
            <div className="animate-spin rounded-full h-10 w-10 border-4 border-[#845143] border-t-transparent" />
          </div>
        ) : error || !data ? (
          <div className="bg-red-50 border border-red-200 rounded-2xl p-6 text-red-600 text-sm">{error}</div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
                <div className="w-10 h-10 rounded-full bg-[#FFDBD1] flex items-center justify-center mb-4">
                  <span className="material-symbols-outlined text-[#341006]">favorite</span>
                </div>
                <p className="text-[#84736F] text-xs uppercase tracking-wider mb-1">Mẹ được hỗ trợ</p>
                <h3 className="text-3xl font-bold text-[#271812]">{formatNumber(data.mothersServed)}</h3>
              </div>
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
                <div className="w-10 h-10 rounded-full bg-[#F8DDD2] flex items-center justify-center mb-4">
                  <span className="material-symbols-outlined text-[#271812]">stethoscope</span>
                </div>
                <p className="text-[#84736F] text-xs uppercase tracking-wider mb-1">Buổi tư vấn đã thực hiện</p>
                <h3 className="text-3xl font-bold text-[#271812]">{formatNumber(data.consultationsDelivered)}</h3>
              </div>
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
                <div className="w-10 h-10 rounded-full bg-[#E9E1DB] flex items-center justify-center mb-4">
                  <span className="material-symbols-outlined text-[#1E1B18]">handshake</span>
                </div>
                <p className="text-[#84736F] text-xs uppercase tracking-wider mb-1">Đối tác đang hoạt động</p>
                <h3 className="text-3xl font-bold text-[#271812]">{formatNumber(data.activePartnerOrganizations)}</h3>
              </div>
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
                <div className="w-10 h-10 rounded-full bg-[#FFE2D9] flex items-center justify-center mb-4">
                  <span className="material-symbols-outlined text-[#845143]">menu_book</span>
                </div>
                <p className="text-[#84736F] text-xs uppercase tracking-wider mb-1">Nội dung đã xuất bản</p>
                <h3 className="text-3xl font-bold text-[#271812]">{formatNumber(data.publishedContentItems)}</h3>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#FFE2D9]">
              <div className="flex items-center gap-2 text-[#84736F] text-sm mb-2">
                <span className="material-symbols-outlined text-[18px]">info</span>
                <span>
                  Kỳ báo cáo: {formatDate(data.periodFrom)} — {formatDate(data.periodTo)}
                </span>
              </div>
              <p className="text-xs text-[#84736F]">{data.anonymizationNote}</p>
              <p className="text-xs text-[#84736F] mt-1">
                Cập nhật lúc {new Date(data.generatedAt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })}
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
