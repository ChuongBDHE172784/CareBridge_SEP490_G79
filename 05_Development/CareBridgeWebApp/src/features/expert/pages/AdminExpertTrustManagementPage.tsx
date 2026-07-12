import { useState, useEffect } from 'react';
import apiClient from '../../../shared/api/apiClient';

type TrustStatus = 'ACTIVE' | 'SUSPENDED' | 'REVOKED';

interface ExpertRow {
  expertProfileId: string;
  specialty: string;
  professionalTitle: string;
  verificationStatus: string;
  trustStatus?: string;
}

export default function AdminExpertTrustManagementPage() {
  const [experts, setExperts] = useState<ExpertRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionId, setActionId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [keyword, setKeyword] = useState('');

  const fetchExperts = async () => {
    try {
      setLoading(true);
      const params: Record<string, string> = { page: '0', size: '50' };
      if (statusFilter) params.status = statusFilter;
      if (keyword.trim()) params.keyword = keyword.trim();
      const qs = new URLSearchParams(params).toString();
      const { data } = await apiClient.get(`/api/v1/expert/directory?${qs}`);
      const items: ExpertRow[] = data.data?.experts ?? data.data ?? [];
      setExperts(items);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Không thể tải danh sách chuyên gia');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchExperts(); }, [statusFilter]);

  const setTrust = async (profileId: string, status: TrustStatus) => {
    setActionId(profileId);
    try {
      await apiClient.patch(`/api/v1/expert/profiles/${profileId}/trust`, {}, { params: { status } });
      setExperts((prev) =>
        prev.map((e) => (e.expertProfileId === profileId ? { ...e, trustStatus: status } : e)),
      );
    } catch {
      alert('Cập nhật trạng thái thất bại');
    } finally {
      setActionId(null);
    }
  };

  const statusBadge = (status: string) => {
    const map: Record<string, string> = {
      APPROVED: 'bg-green-100 text-green-700',
      PENDING: 'bg-amber-100 text-amber-700',
      UNDER_REVIEW: 'bg-blue-100 text-blue-700',
      REJECTED: 'bg-red-100 text-red-700',
      SUSPENDED: 'bg-orange-100 text-orange-700',
      EXPIRED: 'bg-gray-200 text-gray-600',
      ACTIVE: 'bg-green-100 text-green-700',
      REVOKED: 'bg-red-100 text-red-700',
    };
    return (
      <span className={`rounded-full px-2 py-0.5 text-xs font-bold ${map[status] ?? 'bg-gray-100 text-gray-600'}`}>
        {status}
      </span>
    );
  };

  return (
    <div className="p-6 space-y-4 max-w-6xl mx-auto">
      <h1 className="text-2xl font-bold text-on-surface">Quản lý trạng thái tin cậy chuyên gia</h1>

      <div className="flex gap-3">
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-full border border-outline-variant bg-surface px-4 py-2 text-sm"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="APPROVED">Đã duyệt</option>
          <option value="PENDING">Chờ duyệt</option>
          <option value="REJECTED">Từ chối</option>
          <option value="SUSPENDED">Tạm ngưng</option>
        </select>
        <input
          type="text"
          placeholder="Tìm theo chuyên khoa hoặc tên…"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && fetchExperts()}
          className="flex-1 rounded-full border border-outline-variant bg-surface px-4 py-2 text-sm"
        />
        <button
          onClick={fetchExperts}
          className="rounded-full px-5 py-2 text-sm font-medium bg-primary text-white hover:brightness-110 transition"
        >
          Tìm
        </button>
      </div>

      {loading && (
        <div className="flex justify-center py-10">
          <span className="material-symbols-outlined animate-spin text-[36px] text-primary">progress_activity</span>
        </div>
      )}
      {error && (
        <div className="p-4 rounded-2xl bg-error-container text-on-error-container text-sm">
          {error} <button onClick={fetchExperts} className="ml-3 underline font-semibold text-on-error">Thử lại</button>
        </div>
      )}

      {!loading && experts.length === 0 && !error && (
        <div className="py-16 text-center text-on-surface-variant">
          <span className="material-symbols-outlined text-[48px] block mb-3 opacity-40">groups</span>
          <p className="text-base font-medium">Không tìm thấy chuyên gia</p>
        </div>
      )}

      <div className="overflow-x-auto rounded-2xl border border-outline-variant/60 bg-surface">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-surface-container-low text-on-surface-variant">
              <th className="text-left px-5 py-3 font-semibold">Chuyên khoa</th>
              <th className="text-left px-5 py-3 font-semibold">Chức danh</th>
              <th className="text-left px-5 py-3 font-semibold">Xác minh hồ sơ</th>
              <th className="text-left px-5 py-3 font-semibold">Trust status</th>
              <th className="px-5 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {experts.map((exp) => (
              <tr key={exp.expertProfileId} className="border-t border-outline-variant/40">
                <td className="px-5 py-3 font-medium text-on-surface">{exp.specialty ?? '—'}</td>
                <td className="px-5 py-3 text-on-surface-variant">{exp.professionalTitle ?? '—'}</td>
                <td className="px-5 py-3">{statusBadge(exp.verificationStatus)}</td>
                <td className="px-5 py-3">{statusBadge(exp.trustStatus ?? '—')}</td>
                <td className="px-5 py-3">
                  <select
                    value={exp.trustStatus ?? ''}
                    onChange={(e) => {
                      const v = e.target.value as TrustStatus;
                      if (v) setTrust(exp.expertProfileId, v);
                    }}
                    disabled={actionId === exp.expertProfileId}
                    className="rounded-full border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-xs"
                  >
                    <option value="">Đặt trust…</option>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="SUSPENDED">SUSPENDED</option>
                    <option value="REVOKED">REVOKED</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
