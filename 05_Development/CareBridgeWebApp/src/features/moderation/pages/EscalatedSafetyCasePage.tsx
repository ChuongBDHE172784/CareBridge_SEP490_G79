import { useState, useEffect, useCallback } from 'react';
import { useParams, NavLink } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';

// ─── Types ────────────────────────────────────────────────────────────────────

interface SafetyCaseDetail {
  id: string;
  caseCode: string;
  riskLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  summary: string;
  description: string;
  assignedTo: string;
  reportedBy: string;
  slaDeadline: string;
  createdAt: string;
  status: string;
  linkedReports: LinkedReport[];
  evidenceUrls: string[];
}

interface LinkedReport {
  id: string;
  type: string;
  summary: string;
  createdAt: string;
}

// ─── Sidebar ──────────────────────────────────────────────────────────────────

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

// ─── SLA countdown ────────────────────────────────────────────────────────────

function useSlaCountdown(deadline: string | undefined) {
  const [remaining, setRemaining] = useState('');

  useEffect(() => {
    if (!deadline) return;
    const tick = () => {
      const diff = new Date(deadline).getTime() - Date.now();
      if (diff <= 0) { setRemaining('Quá hạn'); return; }
      const m = Math.floor(diff / 60000);
      const s = Math.floor((diff % 60000) / 1000);
      setRemaining(`${m} phút ${s} giây`);
    };
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [deadline]);

  return remaining;
}

// ─── Risk badge ───────────────────────────────────────────────────────────────

function RiskBadge({ level }: { level: string }) {
  const map: Record<string, string> = {
    CRITICAL: 'bg-red-100 text-red-700 border-red-300',
    HIGH: 'bg-orange-100 text-orange-700 border-orange-300',
    MEDIUM: 'bg-yellow-100 text-yellow-700 border-yellow-300',
    LOW: 'bg-green-100 text-green-700 border-green-300',
  };
  const labels: Record<string, string> = {
    CRITICAL: 'Cực kỳ nghiêm trọng',
    HIGH: 'Nghiêm trọng',
    MEDIUM: 'Trung bình',
    LOW: 'Thấp',
  };
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold border ${map[level] ?? ''}`}>
      {level === 'CRITICAL' && <span className="inline-block w-2 h-2 rounded-full bg-red-500 animate-pulse" />}
      {labels[level] ?? level}
    </span>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function EscalatedSafetyCasePage() {
  const { caseId } = useParams<{ caseId: string }>();
  const [caseDetail, setCaseDetail] = useState<SafetyCaseDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [resolution, setResolution] = useState('');
  const [resolutionNote, setResolutionNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // TODO: replace with dedicated case detail endpoint when available (CB-072 backend not yet implemented)
  const fetchCase = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      // Stub: fetch from moderation queue and find matching case by ID
      const res = await apiClient.get('/api/v1/admin/moderation/queue', {
        params: { page: 0, size: 50 },
      });
      const items: SafetyCaseDetail[] = res.data?.data?.content ?? [];
      const found = items.find((i) => i.id === caseId);
      if (found) {
        setCaseDetail(found);
      } else {
        setError('Không tìm thấy ca an toàn này trong hàng đợi.');
      }
    } catch {
      setError('Không tải được thông tin ca an toàn.');
    } finally {
      setLoading(false);
    }
  }, [caseId]);

  useEffect(() => { fetchCase(); }, [fetchCase]);

  const slaCountdown = useSlaCountdown(caseDetail?.slaDeadline);

  const handleResolve = async () => {
    if (!resolution) return;
    setSubmitting(true);
    try {
      // TODO: wire to resolution endpoint when backend implements it (CB-072)
      console.log('Resolve case', caseId, { resolution, resolutionNote });
    } catch {
      alert('Có lỗi xảy ra khi đóng ca.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F6F1EC]">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen">
        {loading ? (
          <div className="flex items-center justify-center h-screen">
            <div className="animate-spin rounded-full h-10 w-10 border-4 border-[#845143] border-t-transparent" />
          </div>
        ) : error ? (
          <div className="p-8 text-red-600 font-medium">{error}</div>
        ) : caseDetail ? (
          <div className="p-8">
            {/* Breadcrumb */}
            <nav className="text-sm text-[#84736F] mb-4 flex items-center gap-2">
              <NavLink to="/moderator/safety-cases" className="hover:text-[#845143]">Ca an toàn</NavLink>
              <span className="material-symbols-outlined text-base">chevron_right</span>
              <span className="text-[#271812] font-medium">{caseDetail.caseCode}</span>
            </nav>

            {/* Urgent header */}
            <div className="bg-red-50 border-2 border-red-400 rounded-2xl p-5 mb-6 flex items-start gap-4">
              <span className="material-symbols-outlined text-red-600 text-3xl animate-pulse">emergency</span>
              <div className="flex-1">
                <div className="flex items-center gap-3 flex-wrap">
                  <h1 className="text-xl font-bold text-red-700">CA AN TOÀN CẤP BÁCH — {caseDetail.caseCode}</h1>
                  <RiskBadge level={caseDetail.riskLevel} />
                </div>
                <p className="text-sm text-red-600 mt-1">{caseDetail.summary}</p>
              </div>
              <div className="text-right flex-shrink-0">
                <p className="text-xs text-[#84736F] uppercase tracking-wider mb-1">Còn lại (SLA)</p>
                <p className="text-2xl font-bold text-red-600 tabular-nums">{slaCountdown}</p>
              </div>
            </div>

            {/* Bento grid */}
            <div className="grid grid-cols-3 gap-6">
              {/* Left 2/3: summary + evidence */}
              <div className="col-span-2 space-y-6">
                {/* Summary card */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#FFE9E3]">
                  <h2 className="text-base font-semibold text-[#271812] mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-[#845143] text-xl">summarize</span>
                    Tóm tắt ca
                  </h2>
                  <p className="text-sm text-[#524440] leading-relaxed">{caseDetail.description}</p>
                  <div className="grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-[#FFE9E3]">
                    <div>
                      <p className="text-xs text-[#84736F] uppercase tracking-wider mb-1">Phụ trách</p>
                      <p className="text-sm font-medium text-[#271812]">{caseDetail.assignedTo}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#84736F] uppercase tracking-wider mb-1">Báo cáo bởi</p>
                      <p className="text-sm font-medium text-[#271812]">{caseDetail.reportedBy}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#84736F] uppercase tracking-wider mb-1">Trạng thái</p>
                      <span className="inline-block px-2.5 py-1 rounded-full bg-[#FFF1EC] text-[#845143] text-xs font-bold">
                        {caseDetail.status}
                      </span>
                    </div>
                    <div>
                      <p className="text-xs text-[#84736F] uppercase tracking-wider mb-1">Thời gian mở</p>
                      <p className="text-sm text-[#524440]">
                        {new Date(caseDetail.createdAt).toLocaleString('vi-VN')}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Evidence card */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#FFE9E3]">
                  <h2 className="text-base font-semibold text-[#271812] mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-[#845143] text-xl">photo_library</span>
                    Bằng chứng
                  </h2>
                  {caseDetail.evidenceUrls.length === 0 ? (
                    <div className="flex flex-col items-center py-8 text-[#84736F]">
                      <span className="material-symbols-outlined text-4xl mb-2">image_not_supported</span>
                      <p className="text-sm">Chưa có bằng chứng đính kèm</p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-3 gap-3">
                      {caseDetail.evidenceUrls.map((url, i) => (
                        <img key={i} src={url} alt={`Bằng chứng ${i + 1}`} className="rounded-xl w-full aspect-video object-cover border border-[#FFE2D9]" />
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Right 1/3: actions + linked reports */}
              <div className="space-y-6">
                {/* Action panel */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#FFE9E3]">
                  <h2 className="text-base font-semibold text-[#271812] mb-4 flex items-center gap-2">
                    <span className="material-symbols-outlined text-[#845143] text-xl">build</span>
                    Hành động
                  </h2>
                  <div className="space-y-3">
                    {['CLOSE_SAFE', 'ESCALATE', 'REQUEST_INFO', 'SUSPEND_CONTENT'].map((opt) => {
                      const labels: Record<string, string> = {
                        CLOSE_SAFE: 'Đóng ca — An toàn',
                        ESCALATE: 'Leo thang lên cấp trên',
                        REQUEST_INFO: 'Yêu cầu thêm thông tin',
                        SUSPEND_CONTENT: 'Tạm ẩn nội dung',
                      };
                      return (
                        <label key={opt} className="flex items-center gap-3 p-3 rounded-xl border border-[#FFE9E3] cursor-pointer hover:bg-[#FFF8F6] transition-colors">
                          <input
                            type="radio"
                            name="resolution"
                            value={opt}
                            checked={resolution === opt}
                            onChange={(e) => setResolution(e.target.value)}
                            className="accent-[#845143]"
                          />
                          <span className="text-sm text-[#271812]">{labels[opt]}</span>
                        </label>
                      );
                    })}
                  </div>
                  <textarea
                    placeholder="Ghi chú (tuỳ chọn)..."
                    value={resolutionNote}
                    onChange={(e) => setResolutionNote(e.target.value)}
                    rows={3}
                    className="mt-4 w-full text-sm border border-[#D6C2BD] rounded-xl p-3 resize-none focus:outline-none focus:ring-2 focus:ring-[#845143]/40 placeholder:text-[#84736F]"
                  />
                  <button
                    onClick={handleResolve}
                    disabled={!resolution || submitting}
                    className="mt-4 w-full py-2.5 rounded-xl bg-[#845143] text-white text-sm font-semibold hover:bg-[#6B3D2E] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    {submitting ? 'Đang xử lý...' : 'Xác nhận & Đóng ca'}
                  </button>
                </div>

                {/* Linked reports */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-[#FFE9E3]">
                  <h2 className="text-base font-semibold text-[#271812] mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-[#845143] text-xl">link</span>
                    Báo cáo liên quan ({caseDetail.linkedReports.length})
                  </h2>
                  <div className="space-y-3">
                    {caseDetail.linkedReports.length === 0 ? (
                      <p className="text-sm text-[#84736F]">Không có báo cáo liên quan.</p>
                    ) : (
                      caseDetail.linkedReports.map((r) => (
                        <div key={r.id} className="p-3 rounded-xl bg-[#FFF8F6] border border-[#FFE2D9]">
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-xs font-bold text-[#845143]">{r.id}</span>
                            <span className="text-xs text-[#84736F]">{new Date(r.createdAt).toLocaleDateString('vi-VN')}</span>
                          </div>
                          <p className="text-xs text-[#524440]">{r.type} — {r.summary}</p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
}
