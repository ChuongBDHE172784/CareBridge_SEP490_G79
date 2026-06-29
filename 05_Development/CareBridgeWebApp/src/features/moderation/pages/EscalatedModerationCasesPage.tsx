import { useState, useEffect, useCallback } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ModerationCase {
  id: string;
  targetType: string;
  status: string;
  riskLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  assignedTo: string;
  deadline: string;
  createdAt: string;
  description: string;
}

// ─── Sidebar (shared ModPortal pattern) ──────────────────────────────────────

const NAV_ITEMS = [
  { label: 'Tổng quan', icon: 'dashboard', path: '/moderator/dashboard' },
  { label: 'Hàng đợi', icon: 'queue', path: '/moderator/queue' },
  { label: 'Báo cáo', icon: 'flag', path: '/moderator/reports' },
  { label: 'Vi phạm', icon: 'gavel', path: '/moderator/violations' },
  { label: 'Ca an toàn', icon: 'health_and_safety', path: '/moderator/safety-cases' },
];

function ModPortalSidebar() {
  return (
    <aside className="w-64 fixed left-0 top-0 h-screen bg-white border-r border-[#FFE2D9] flex flex-col z-20">
      <div className="p-6 border-b border-[#FFE2D9]">
        <div className="flex items-center gap-2 mb-1">
          <span className="material-symbols-outlined text-[#845143] text-2xl">shield</span>
          <span className="font-bold text-[#845143] text-lg leading-none">ModPortal</span>
        </div>
        <p className="text-xs text-[#84736F] ml-8">Hệ thống kiểm duyệt</p>
      </div>
      <nav className="flex-1 p-4 space-y-1">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-[#FFF1EC] text-[#845143]'
                  : 'text-[#524440] hover:bg-[#FFF8F6] hover:text-[#845143]'
              }`
            }
          >
            <span className="material-symbols-outlined text-[20px]">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="p-4 border-t border-[#FFE2D9]">
        <p className="text-xs text-[#84736F] text-center">CareBridge © 2025</p>
      </div>
    </aside>
  );
}

// ─── Stat card ────────────────────────────────────────────────────────────────

function StatCard({ icon, label, value, accent }: { icon: string; label: string; value: number; accent: string }) {
  return (
    <div className="bg-white rounded-2xl p-5 border border-[#FFE9E3] shadow-sm flex items-center gap-4">
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${accent}`}>
        <span className="material-symbols-outlined text-xl text-white">{icon}</span>
      </div>
      <div>
        <p className="text-2xl font-bold text-[#271812]">{value}</p>
        <p className="text-xs text-[#84736F] mt-0.5">{label}</p>
      </div>
    </div>
  );
}

// ─── Risk badge ───────────────────────────────────────────────────────────────

function RiskBadge({ level }: { level: string }) {
  const map: Record<string, { cls: string; label: string }> = {
    CRITICAL: { cls: 'bg-red-100 text-red-700', label: 'Cực nghiêm trọng' },
    HIGH: { cls: 'bg-orange-100 text-orange-700', label: 'Nghiêm trọng' },
    MEDIUM: { cls: 'bg-yellow-100 text-yellow-700', label: 'Trung bình' },
    LOW: { cls: 'bg-green-100 text-green-700', label: 'Thấp' },
  };
  const { cls, label } = map[level] ?? { cls: 'bg-gray-100 text-gray-600', label: level };
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold ${cls}`}>
      {level === 'CRITICAL' && <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />}
      {label}
    </span>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function EscalatedModerationCasesPage() {
  const navigate = useNavigate();
  const [cases, setCases] = useState<ModerationCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Resolution panel state
  const [selectedCase, setSelectedCase] = useState<ModerationCase | null>(null);
  const [resolution, setResolution] = useState('');
  const [resolutionNote, setResolutionNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchCases = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await apiClient.get('/api/v1/admin/moderation/queue', {
        params: { page: 0, size: 50 },
      });
      const content = res.data?.data?.content ?? [];
      setCases(content);
    } catch {
      setError('Không tải được danh sách ca an toàn.');
      setCases([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchCases(); }, [fetchCases]);

  const stats = {
    total: cases.length,
    critical: cases.filter((c) => c.riskLevel === 'CRITICAL').length,
    pending: cases.filter((c) => c.status === 'PENDING').length,
  };

  const handleResolve = async () => {
    if (!selectedCase || !resolution) return;
    setSubmitting(true);
    try {
      // TODO: wire to resolution endpoint when backend implements it
      console.log('Resolve case', selectedCase.id, { resolution, resolutionNote });
      setCases((prev) => prev.filter((c) => c.id !== selectedCase.id));
      setSelectedCase(null);
      setResolution('');
      setResolutionNote('');
    } catch {
      alert('Có lỗi xảy ra khi xử lý ca.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F6F1EC]">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen">
        <div className="p-8">
          {/* Page header */}
          <div className="mb-6">
            <div className="flex items-center gap-2 mb-1">
              <span className="material-symbols-outlined text-[#845143] text-2xl">health_and_safety</span>
              <h1 className="text-2xl font-bold text-[#271812]">Ca An Toàn Cấp Bách</h1>
            </div>
            <p className="text-sm text-[#84736F] ml-8">Các ca an toàn cần xử lý khẩn cấp trong vòng SLA cho phép</p>
          </div>

          {/* Stat cards */}
          <div className="grid grid-cols-3 gap-4 mb-6">
            <StatCard icon="health_and_safety" label="Tổng ca đang mở" value={stats.total} accent="bg-[#845143]" />
            <StatCard icon="emergency" label="Cực kỳ nghiêm trọng" value={stats.critical} accent="bg-red-500" />
            <StatCard icon="pending_actions" label="Chờ xử lý" value={stats.pending} accent="bg-orange-400" />
          </div>

          {/* Main content */}
          {loading ? (
            <div className="flex justify-center py-16">
              <div className="animate-spin rounded-full h-10 w-10 border-4 border-[#845143] border-t-transparent" />
            </div>
          ) : error ? (
            <div className="bg-red-50 border border-red-200 rounded-2xl p-6 text-red-600 text-sm">{error}</div>
          ) : (
            <div className={`grid gap-6 ${selectedCase ? 'grid-cols-3' : 'grid-cols-1'}`}>
              {/* Table */}
              <div className={selectedCase ? 'col-span-2' : 'col-span-1'}>
                <div className="bg-white rounded-2xl shadow-sm border border-[#FFE9E3] overflow-hidden">
                  <div className="px-6 py-4 border-b border-[#FFE9E3] flex items-center justify-between">
                    <h2 className="font-semibold text-[#271812]">Danh sách ca an toàn ({cases.length})</h2>
                    <button onClick={fetchCases} className="text-sm text-[#845143] hover:underline flex items-center gap-1">
                      <span className="material-symbols-outlined text-base">refresh</span>
                      Làm mới
                    </button>
                  </div>

                  {cases.length === 0 ? (
                    <div className="flex flex-col items-center py-16 text-[#84736F]">
                      <span className="material-symbols-outlined text-5xl mb-3">check_circle</span>
                      <p className="font-medium">Không có ca nào đang mở</p>
                      <p className="text-sm mt-1">Mọi ca an toàn đã được xử lý.</p>
                    </div>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="bg-[#FFF8F6] border-b border-[#FFE9E3]">
                            {['ID Ca', 'Mức độ', 'Loại', 'Người phụ trách', 'Hạn chót', 'Chi tiết'].map((h) => (
                              <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-[#84736F] uppercase tracking-wider">
                                {h}
                              </th>
                            ))}
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-[#FFE9E3]">
                          {cases.map((c) => (
                            <tr
                              key={c.id}
                              className={`hover:bg-[#FFF8F6] transition-colors cursor-pointer ${selectedCase?.id === c.id ? 'bg-[#FFF1EC]' : ''}`}
                              onClick={() => {
                                setSelectedCase(c);
                                setResolution('');
                                setResolutionNote('');
                              }}
                            >
                              <td className="px-4 py-3 font-mono text-xs text-[#845143] font-bold">{c.id.slice(0, 8).toUpperCase()}</td>
                              <td className="px-4 py-3"><RiskBadge level={c.riskLevel} /></td>
                              <td className="px-4 py-3 text-[#524440]">{c.targetType || '—'}</td>
                              <td className="px-4 py-3 text-[#524440]">{c.assignedTo || 'Chưa gán'}</td>
                              <td className="px-4 py-3 text-[#524440] tabular-nums">
                                {c.deadline ? new Date(c.deadline).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '—'}
                              </td>
                              <td className="px-4 py-3">
                                <button
                                  onClick={(e) => { e.stopPropagation(); navigate(`/moderator/safety-cases/${c.id}`); }}
                                  className="text-[#845143] hover:underline font-medium"
                                >
                                  Xem chi tiết
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>

              {/* Resolution panel (slides in when a case is selected) */}
              {selectedCase && (
                <div className="col-span-1">
                  <div className="bg-white rounded-2xl shadow-sm border border-[#FFE9E3] p-6 sticky top-8">
                    <div className="flex items-start justify-between mb-4">
                      <h2 className="font-semibold text-[#271812] flex items-center gap-2">
                        <span className="material-symbols-outlined text-[#845143] text-xl">task_alt</span>
                        Xử lý ca
                      </h2>
                      <button onClick={() => setSelectedCase(null)} className="text-[#84736F] hover:text-[#271812]">
                        <span className="material-symbols-outlined">close</span>
                      </button>
                    </div>

                    <div className="mb-4 p-3 rounded-xl bg-[#FFF8F6] border border-[#FFE9E3]">
                      <p className="text-xs text-[#84736F] mb-1">Ca đang xử lý</p>
                      <p className="text-sm font-bold text-[#845143] font-mono">{selectedCase.id.slice(0, 8).toUpperCase()}</p>
                      <p className="text-xs text-[#524440] mt-1 line-clamp-2">{selectedCase.description || selectedCase.targetType}</p>
                    </div>

                    <p className="text-xs font-semibold text-[#84736F] uppercase tracking-wider mb-3">Hành động</p>
                    <div className="space-y-2 mb-4">
                      {[
                        { value: 'CLOSE_SAFE', label: 'Đóng ca — Không vi phạm' },
                        { value: 'CLOSE_VIOLATION', label: 'Đóng ca — Xác nhận vi phạm' },
                        { value: 'ESCALATE', label: 'Leo thang lên cấp cao hơn' },
                        { value: 'SUSPEND_CONTENT', label: 'Tạm ẩn nội dung liên quan' },
                      ].map((opt) => (
                        <label
                          key={opt.value}
                          className="flex items-start gap-3 p-3 rounded-xl border border-[#FFE9E3] cursor-pointer hover:bg-[#FFF8F6] transition-colors"
                        >
                          <input
                            type="radio"
                            name="case-resolution"
                            value={opt.value}
                            checked={resolution === opt.value}
                            onChange={(e) => setResolution(e.target.value)}
                            className="accent-[#845143] mt-0.5"
                          />
                          <span className="text-sm text-[#271812]">{opt.label}</span>
                        </label>
                      ))}
                    </div>

                    <textarea
                      placeholder="Ghi chú lý do hoặc hành động đã thực hiện..."
                      value={resolutionNote}
                      onChange={(e) => setResolutionNote(e.target.value)}
                      rows={4}
                      className="w-full text-sm border border-[#D6C2BD] rounded-xl p-3 resize-none focus:outline-none focus:ring-2 focus:ring-[#845143]/40 placeholder:text-[#84736F] mb-4"
                    />

                    <button
                      onClick={handleResolve}
                      disabled={!resolution || submitting}
                      className="w-full py-3 rounded-xl bg-[#845143] text-white font-semibold text-sm hover:bg-[#6B3D2E] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      {submitting ? 'Đang xử lý...' : 'Xác nhận & Đóng ca'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
