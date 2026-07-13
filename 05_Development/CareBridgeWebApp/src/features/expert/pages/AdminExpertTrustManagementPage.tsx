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

const statusLabels: Record<string, string> = {
  ACTIVE: 'Hoạt động',
  APPROVED: 'Đã duyệt',
  EXPIRED: 'Hết hạn',
  PENDING: 'Chờ duyệt',
  REJECTED: 'Từ chối',
  REVOKED: 'Đã thu hồi',
  SUSPENDED: 'Tạm ngưng',
  UNDER_REVIEW: 'Đang xem xét',
};

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
        {statusLabels[status] ?? 'Chưa thiết lập'}
      </span>
    );
  };

  return (
    <main className="min-h-screen bg-[#F6F1EC] p-5 md:p-10">
      <div className="mx-auto max-w-7xl space-y-6">
        <header className="rounded-3xl border border-outline-variant/40 bg-surface px-6 py-6 shadow-sm md:px-8">
          <div className="flex items-start gap-4">
            <span className="material-symbols-outlined rounded-2xl bg-primary-container p-3 text-primary">verified_user</span>
            <div>
              <p className="text-sm font-semibold text-primary">Quản trị chuyên gia</p>
              <h1 className="mt-1 text-2xl font-bold text-on-surface">Quản lý trạng thái tin cậy</h1>
              <p className="mt-2 text-sm leading-6 text-on-surface-variant">Theo dõi trạng thái xác minh và cập nhật quyền tin cậy của chuyên gia.</p>
            </div>
          </div>
        </header>

        <section className="rounded-3xl border border-outline-variant/40 bg-surface p-4 shadow-sm md:p-5">
          <div className="flex flex-col gap-3 md:flex-row">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="rounded-full border border-outline-variant bg-surface-container-lowest px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 md:w-52"
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
              className="min-w-0 flex-1 rounded-full border border-outline-variant bg-surface-container-lowest px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
            />
            <button
              onClick={fetchExperts}
              className="rounded-full bg-primary px-6 py-2.5 text-sm font-semibold text-white transition hover:brightness-110"
            >
              Tìm kiếm
            </button>
          </div>
        </section>

      {loading && (
        <div className="flex justify-center rounded-3xl bg-surface py-16 shadow-sm">
          <span className="material-symbols-outlined animate-spin text-[36px] text-primary">progress_activity</span>
        </div>
      )}
      {error && (
        <div className="rounded-3xl bg-error-container p-4 text-sm text-on-error-container">
          {error} <button onClick={fetchExperts} className="ml-3 underline font-semibold text-on-error">Thử lại</button>
        </div>
      )}

      {!loading && experts.length === 0 && !error && (
        <div className="rounded-3xl bg-surface py-16 text-center text-on-surface-variant shadow-sm">
          <span className="material-symbols-outlined text-[48px] block mb-3 opacity-40">groups</span>
          <p className="text-base font-medium">Không tìm thấy chuyên gia</p>
        </div>
      )}

      <section className="overflow-x-auto rounded-3xl border border-outline-variant/40 bg-surface shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-surface-container-low text-on-surface-variant">
              <th className="text-left px-5 py-3 font-semibold">Chuyên khoa</th>
              <th className="text-left px-5 py-3 font-semibold">Chức danh</th>
              <th className="text-left px-5 py-3 font-semibold">Xác minh hồ sơ</th>
              <th className="text-left px-5 py-3 font-semibold">Trạng thái tin cậy</th>
              <th className="px-5 py-3 text-left font-semibold">Cập nhật</th>
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
                    <option value="">Đặt trạng thái…</option>
                    <option value="ACTIVE">Hoạt động</option>
                    <option value="SUSPENDED">Tạm ngưng</option>
                    <option value="REVOKED">Thu hồi</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      </div>
    </main>
  );
}
