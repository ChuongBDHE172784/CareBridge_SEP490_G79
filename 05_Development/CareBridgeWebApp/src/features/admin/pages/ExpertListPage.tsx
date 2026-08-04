import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAdminExpertProfiles, type ExpertProfileResponse } from '../../expert/services/expertApi';

const PAGE_SIZE = 10;

const STATUS: Record<string, { label: string; className: string }> = {
  APPROVED: { label: 'Đã duyệt', className: 'bg-emerald-100 text-emerald-700' },
  PENDING: { label: 'Chờ xét duyệt', className: 'bg-amber-100 text-amber-800' },
  UNDER_REVIEW: { label: 'Đang xét duyệt', className: 'bg-amber-100 text-amber-800' },
  REJECTED: { label: 'Từ chối', className: 'bg-rose-100 text-rose-700' },
  SUSPENDED: { label: 'Tạm ngưng', className: 'bg-orange-100 text-orange-700' },
  REVOKED: { label: 'Thu hồi', className: 'bg-rose-100 text-rose-700' },
};

function StatusBadge({ status }: { status?: string | null }) {
  const value = status || 'PENDING';
  const config = STATUS[value] ?? { label: value.replaceAll('_', ' '), className: 'bg-slate-100 text-slate-700' };
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${config.className}`}>{config.label}</span>;
}

function formatDate(value?: string | null) {
  if (!value) return 'Chưa có';
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(value));
}

export default function ExpertListPage() {
  const navigate = useNavigate();
  const [experts, setExperts] = useState<ExpertProfileResponse[]>([]);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setExperts(await getAdminExpertProfiles());
    } catch (caught: unknown) {
      const status = (caught as { response?: { status?: number } }).response?.status;
      setError(
        status === 403
          ? 'Bạn không có quyền System Admin để xem danh sách chuyên gia.'
          : status === 404
            ? 'Backend chưa có API danh sách chuyên gia. Hãy khởi động lại backend từ mã nguồn mới nhất.'
            : status === 401
              ? 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
              : status
                ? `Không thể tải danh sách chuyên gia (mã lỗi ${status}). Vui lòng thử lại.`
                : 'Không thể kết nối đến backend. Kiểm tra API tại http://localhost:8080 rồi thử lại.',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    const query = keyword.trim().toLocaleLowerCase('vi-VN');
    return experts.filter((expert) => {
      const matchedStatus = !status || expert.verificationStatus === status || expert.trustStatus === status;
      const haystack = [expert.displayName, expert.specialty, expert.professionalTitle, expert.workplace]
        .filter(Boolean).join(' ').toLocaleLowerCase('vi-VN');
      return matchedStatus && (!query || haystack.includes(query));
    });
  }, [experts, keyword, status]);
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const visible = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const approved = experts.filter((item) => item.verificationStatus === 'APPROVED').length;
  const pending = experts.filter((item) => ['PENDING', 'UNDER_REVIEW'].includes(item.verificationStatus)).length;
  const restricted = experts.filter((item) => ['SUSPENDED', 'REVOKED'].includes(item.trustStatus ?? '')).length;

  return (
    <main className="min-h-screen bg-[#f7f9fb] p-4 font-sans text-[#172126] lg:p-8">
      <div className="mx-auto max-w-[1600px] space-y-6">
        <header className="rounded-2xl border border-[#d5dde2] bg-white p-6 shadow-xs md:p-8">
          <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#95d2ca] bg-[#d6f0ec] px-3 py-1 text-xs font-semibold text-[#0f5a53]">
                <span className="material-symbols-outlined text-sm">medical_services</span> Cổng quản trị CareBridge
              </div>
              <h1 className="m-0 text-2xl font-bold tracking-tight md:text-3xl">Danh sách chuyên gia</h1>
              <p className="mt-2 max-w-2xl text-sm leading-relaxed text-[#42515a]">Theo dõi toàn bộ hồ sơ chuyên gia, tiến độ xét duyệt và trạng thái hoạt động. Chọn một hồ sơ để xem thông tin đăng ký, định danh và chứng chỉ đã nộp.</p>
            </div>
            <button type="button" onClick={() => void load()} disabled={loading} className="inline-flex items-center justify-center gap-2 rounded-xl border border-[#d5dde2] bg-white px-4 py-2.5 text-sm font-semibold text-[#42515a] hover:bg-[#f6f8fa] disabled:opacity-50">
              <span className={`material-symbols-outlined text-lg ${loading ? 'animate-spin' : ''}`}>refresh</span> Làm mới
            </button>
          </div>
        </header>

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[
            ['group', 'Tổng chuyên gia', experts.length, 'Toàn bộ hồ sơ đã tạo', 'text-[#0f766e]'],
            ['verified', 'Đã duyệt', approved, 'Được phép cung cấp tư vấn', 'text-emerald-600'],
            ['pending_actions', 'Chờ xét duyệt', pending, 'Cần hoàn tất hoặc được duyệt', 'text-amber-600'],
            ['shield', 'Hạn chế hoạt động', restricted, 'Tạm ngưng hoặc đã thu hồi', 'text-rose-600'],
          ].map(([icon, label, count, note, color]) => <div key={String(label)} className="flex items-center justify-between rounded-2xl border border-[#d5dde2] bg-white p-5 shadow-xs"><div><p className="m-0 text-xs font-semibold uppercase tracking-wider text-[#6b7882]">{label}</p><p className="mt-1 text-2xl font-bold">{count}</p><p className="m-0 text-xs text-[#6b7882]">{note}</p></div><span className={`material-symbols-outlined text-3xl ${color}`}>{icon}</span></div>)}
        </section>

        <section className="rounded-2xl border border-[#d5dde2] bg-white p-4 shadow-xs">
          <div className="flex flex-col gap-3 md:flex-row">
            <label className="relative flex-1"><span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg text-[#6b7882]">search</span><input value={keyword} onChange={(event) => { setKeyword(event.target.value); setPage(0); }} placeholder="Tìm theo tên, chuyên khoa, chức danh hoặc nơi làm việc..." className="w-full rounded-xl border border-[#d5dde2] bg-[#f7f9fb] py-2.5 pl-10 pr-4 text-sm outline-none focus:border-[#0f766e]" /></label>
            <select value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }} className="rounded-xl border border-[#d5dde2] bg-[#f7f9fb] px-4 py-2.5 text-sm outline-none focus:border-[#0f766e]"><option value="">Mọi trạng thái</option><option value="PENDING">Chờ xét duyệt</option><option value="UNDER_REVIEW">Đang xét duyệt</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Từ chối</option><option value="SUSPENDED">Tạm ngưng</option><option value="REVOKED">Thu hồi</option></select>
          </div>
        </section>

        {error && <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm font-medium text-rose-800">{error}</div>}
        <section className="overflow-hidden rounded-2xl border border-[#d5dde2] bg-white shadow-xs">
          <div className="flex items-center justify-between border-b border-[#eef3f6] px-5 py-4"><h2 className="m-0 text-base font-bold">Hồ sơ chuyên gia ({filtered.length})</h2><span className="text-xs text-[#6b7882]">Trang {page + 1}/{totalPages}</span></div>
          <div className="overflow-x-auto"><table className="w-full min-w-[850px] text-left"><thead className="bg-[#f6f8fa] text-xs uppercase tracking-wide text-[#6b7882]"><tr><th className="px-5 py-3 font-semibold">Chuyên gia</th><th className="px-5 py-3 font-semibold">Chuyên môn</th><th className="px-5 py-3 font-semibold">Xét duyệt</th><th className="px-5 py-3 font-semibold">Tin cậy</th><th className="px-5 py-3 font-semibold">Đăng ký</th><th className="px-5 py-3" /></tr></thead><tbody className="divide-y divide-[#eef3f6]">
            {loading ? <tr><td colSpan={6} className="px-5 py-16 text-center text-sm text-[#6b7882]">Đang tải hồ sơ chuyên gia…</td></tr> : visible.length === 0 ? <tr><td colSpan={6} className="px-5 py-16 text-center text-sm text-[#6b7882]">Không tìm thấy hồ sơ phù hợp.</td></tr> : visible.map((expert) => <tr key={expert.expertProfileId} className="hover:bg-[#f7f9fb]"><td className="px-5 py-4"><div className="flex items-center gap-3">{expert.avatarUrl ? <img className="h-10 w-10 rounded-full object-cover" src={expert.avatarUrl} alt="" /> : <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#d6f0ec] font-bold text-[#0f766e]">{(expert.displayName || 'C').slice(0, 1)}</div>}<div><p className="m-0 font-bold text-sm">{expert.displayName || 'Chuyên gia chưa cập nhật tên'}</p><p className="m-0 text-xs text-[#6b7882]">{expert.workplace || 'Chưa cập nhật nơi làm việc'}</p></div></div></td><td className="px-5 py-4"><p className="m-0 text-sm font-medium">{expert.specialty || 'Chưa cập nhật'}</p><p className="m-0 text-xs text-[#6b7882]">{expert.professionalTitle || 'Chưa cập nhật chức danh'}{expert.experienceYears !== null ? ` · ${expert.experienceYears} năm` : ''}</p></td><td className="px-5 py-4"><StatusBadge status={expert.verificationStatus} /></td><td className="px-5 py-4"><StatusBadge status={expert.trustStatus} /></td><td className="px-5 py-4 text-sm text-[#42515a]">{formatDate(expert.createdAt)}</td><td className="px-5 py-4 text-right"><button type="button" onClick={() => navigate(`/admin/experts/${expert.expertProfileId}`)} className="inline-flex items-center gap-1 rounded-lg px-3 py-2 text-sm font-semibold text-[#0f766e] hover:bg-[#d6f0ec]"><span className="material-symbols-outlined text-base">visibility</span> Xem chi tiết</button></td></tr>)}
          </tbody></table></div>
          <div className="flex items-center justify-end gap-2 border-t border-[#eef3f6] px-5 py-3"><button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="rounded-lg border border-[#d5dde2] px-3 py-1.5 text-sm disabled:opacity-40">Trước</button><button type="button" disabled={page >= totalPages - 1} onClick={() => setPage((value) => value + 1)} className="rounded-lg border border-[#d5dde2] px-3 py-1.5 text-sm disabled:opacity-40">Sau</button></div>
        </section>
      </div>
    </main>
  );
}
