import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';

// MOCK — no backend endpoint exposes per-account moderation-action history yet
// (ModerationActionRepository exists but is not surfaced via any controller).
interface ViolationRow {
  id: string;
  date: string;
  action: string;
  actionClass: string;
  violation: string;
  detail: string;
  modId: string;
  status: string;
}

const MOCK_VIOLATIONS: ViolationRow[] = [
  {
    id: '1',
    date: '15/10/2023 14:30',
    action: 'Đình chỉ (3 ngày)',
    actionClass: 'bg-error-container text-error',
    violation: 'Ngôn từ thù ghét',
    detail: 'Bình luận trong bài viết #P-...',
    modId: 'MOD-042',
    status: 'Hết hiệu lực',
  },
  {
    id: '2',
    date: '02/10/2023 09:15',
    action: 'Cảnh cáo',
    actionClass: 'bg-surface-container text-primary',
    violation: 'Spam / Quảng cáo',
    detail: 'Đăng liên kết rác nhiều lần',
    modId: 'MOD-019',
    status: 'Đang kích hoạt',
  },
  {
    id: '3',
    date: '15/08/2023 16:45',
    action: 'Cảnh cáo',
    actionClass: 'bg-surface-container text-primary',
    violation: 'Ảnh đại diện không phù hợp',
    detail: 'Vi phạm tiêu chuẩn cộng đồng',
    modId: 'Hệ thống AutoMod',
    status: 'Hết hiệu lực',
  },
];

export default function ViolationHistoryPage() {
  const navigate = useNavigate();
  const [timeRange, setTimeRange] = useState('all');
  const [actionTypes, setActionTypes] = useState<Set<string>>(new Set(['WARN', 'RESTRICT']));

  const toggleActionType = (key: string) => {
    setActionTypes((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
  };

  return (
    <div className="min-h-screen bg-background">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <div className="bg-error-container rounded-2xl p-4 mb-6 text-error text-sm flex items-center gap-2">
          <span className="material-symbols-outlined text-lg">info</span>
          Dữ liệu mẫu (MOCK) — backend chưa có endpoint liệt kê lịch sử vi phạm theo tài khoản.
        </div>

        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-on-surface cursor-pointer mb-4"
        >
          <span className="material-symbols-outlined text-lg">arrow_back</span>
          Tài khoản: #USR-98241
        </button>

        <div className="flex items-center justify-between mb-1">
          <h1 className="text-2xl font-bold text-on-surface m-0">Lịch sử vi phạm</h1>
          <button
            disabled
            title="Backend chưa hỗ trợ xuất báo cáo vi phạm"
            className="flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface-container text-primary text-sm font-semibold opacity-50 cursor-not-allowed"
          >
            <span className="material-symbols-outlined text-lg">download</span>
            Xuất báo cáo
          </button>
        </div>
        <p className="text-sm text-outline mb-6">Ghi nhận các hành động kỷ luật đối với tài khoản Nguyễn Văn A.</p>

        <div className="grid grid-cols-[280px_1fr] gap-6">
          <div className="flex flex-col gap-4">
            <div className="bg-surface rounded-2xl p-5 shadow-md">
              <div className="flex items-center gap-2 mb-4">
                <span className="material-symbols-outlined text-primary text-xl">filter_alt</span>
                <p className="text-sm font-bold text-on-surface m-0">Bộ lọc</p>
              </div>

              <label className="block text-xs font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
                Thời gian
              </label>
              <select
                value={timeRange}
                onChange={(e) => setTimeRange(e.target.value)}
                className="w-full py-2.5 px-3 rounded-xl border border-outline-variant text-sm mb-4"
              >
                <option value="all">Tất cả thời gian</option>
                <option value="30d">30 ngày qua</option>
                <option value="90d">90 ngày qua</option>
              </select>

              <label className="block text-xs font-semibold text-outline uppercase tracking-[0.05em] mb-1.5">
                Loại hành động
              </label>
              {[
                { key: 'WARN', label: 'Cảnh cáo' },
                { key: 'RESTRICT', label: 'Đình chỉ tạm thời' },
                { key: 'SUSPEND', label: 'Cấm vĩnh viễn' },
              ].map((opt) => (
                <label key={opt.key} className="flex items-center gap-2 py-1.5 text-sm text-on-surface cursor-pointer">
                  <input
                    type="checkbox"
                    checked={actionTypes.has(opt.key)}
                    onChange={() => toggleActionType(opt.key)}
                    className="accent-primary"
                  />
                  {opt.label}
                </label>
              ))}

              <button className="w-full mt-4 py-2.5 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer">
                Áp dụng
              </button>
            </div>

            <div className="bg-surface rounded-2xl p-5 shadow-md">
              <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
                Trạng thái tài khoản
              </p>
              <div className="flex items-center gap-2 mb-2">
                <span className="w-2.5 h-2.5 rounded-full bg-error" />
                <span className="text-sm font-bold text-error">Nguy cơ cao</span>
              </div>
              <p className="text-xs text-outline">
                Tài khoản này đã nhận 2 cảnh cáo trong 30 ngày qua. Vi phạm tiếp theo có thể dẫn đến đình chỉ.
              </p>
            </div>
          </div>

          <div className="bg-surface rounded-2xl shadow-md overflow-hidden">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left bg-surface-container-low">
                  {['NGÀY XỬ LÝ', 'HÀNH ĐỘNG', 'VI PHẠM', 'MOD ID', 'TRẠNG THÁI'].map((h) => (
                    <th key={h} className="py-3 px-4 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {MOCK_VIOLATIONS.map((row) => (
                  <tr key={row.id} className="border-b border-surface-container-highest">
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant whitespace-nowrap">{row.date}</td>
                    <td className="py-3.5 px-4">
                      <span className={`py-1 px-3 rounded-full text-xs font-semibold ${row.actionClass}`}>{row.action}</span>
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="text-sm font-semibold text-on-surface">{row.violation}</div>
                      <div className="text-xs text-outline">{row.detail}</div>
                    </td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant">{row.modId}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant">{row.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="flex justify-between items-center px-4 py-3 border-t border-surface-container-highest">
              <span className="text-[13px] text-outline">Hiển thị 1-{MOCK_VIOLATIONS.length} trong {MOCK_VIOLATIONS.length} kết quả</span>
              <div className="flex gap-1">
                <button disabled className="w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center opacity-40 cursor-not-allowed">
                  <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
                </button>
                <button disabled className="w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center opacity-40 cursor-not-allowed">
                  <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
