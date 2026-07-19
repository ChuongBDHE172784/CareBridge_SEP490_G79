import { useState, useEffect, useCallback } from 'react';
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

  const fetchExperts = useCallback(async () => {
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
  }, [statusFilter, keyword]);

  useEffect(() => { fetchExperts(); }, [fetchExperts]);

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
      <span className={`rounded px-2 py-0.5 text-xs font-semibold ${map[status] ?? 'bg-gray-100 text-gray-600'}`}>
        {statusLabels[status] ?? 'Chưa thiết lập'}
      </span>
    );
  };

  return (
    <main className="portal-page px-5 py-5 md:px-6 md:py-6">
      <div className="portal-contained">
        <header className="portal-header">
          <div>
            <p className="portal-eyebrow">Quản trị chuyên gia</p>
            <h1 className="portal-title">Quản lý trạng thái tin cậy</h1>
            <p className="portal-subtitle">Theo dõi trạng thái xác minh và cập nhật quyền tin cậy của chuyên gia.</p>
          </div>
        </header>

        <section className="portal-toolbar">
          <div className="flex flex-col gap-3 md:flex-row">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="portal-field md:w-52"
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
              className="portal-field min-w-0 flex-1"
            />
            <button
              onClick={fetchExperts}
              className="portal-primary-button"
            >
              Tìm kiếm
            </button>
          </div>
        </section>

      {loading && (
        <div className="flex justify-center rounded-lg border border-outline-variant/70 bg-surface py-16">
          <span className="material-symbols-outlined animate-spin text-[32px] text-primary">progress_activity</span>
        </div>
      )}
      {error && (
        <div className="portal-error">
          {error} <button onClick={fetchExperts} className="ml-3 underline font-semibold text-on-error">Thử lại</button>
        </div>
      )}

      {!loading && experts.length === 0 && !error && (
        <div className="portal-empty">
          <span className="material-symbols-outlined text-[48px] block mb-3 opacity-40">groups</span>
          <p className="text-base font-medium">Không tìm thấy chuyên gia</p>
        </div>
      )}

      <section className="portal-table-card overflow-x-auto">
        <table>
          <thead>
            <tr>
              <th>Chuyên khoa</th>
              <th>Chức danh</th>
              <th>Xác minh hồ sơ</th>
              <th>Trạng thái tin cậy</th>
              <th>Cập nhật</th>
            </tr>
          </thead>
          <tbody>
            {experts.map((exp) => (
              <tr key={exp.expertProfileId}>
                <td className="font-medium text-on-surface">{exp.specialty ?? '—'}</td>
                <td className="text-on-surface-variant">{exp.professionalTitle ?? '—'}</td>
                <td>{statusBadge(exp.verificationStatus)}</td>
                <td>{statusBadge(exp.trustStatus ?? '—')}</td>
                <td>
                  <select
                    value={exp.trustStatus ?? ''}
                    onChange={(e) => {
                      const v = e.target.value as TrustStatus;
                      if (v) setTrust(exp.expertProfileId, v);
                    }}
                    disabled={actionId === exp.expertProfileId}
                    className="rounded-md border border-outline-variant bg-surface px-2 py-1.5 text-xs"
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
