import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';
import { getMyProfile, getMyAvailability } from '../services/expertApi';

/* ------------------------------------------------------------------ */
/*  Helpers & Interfaces                                              */
/* ------------------------------------------------------------------ */
interface ConsultationSummary {
  id: string;
  counterpartDisplayName: string;
  topic: string;
  status: string;
  createdAt: string;
}

interface CommunityQuestionSummary {
  id: string;
  title: string;
  topicName: string;
  urgency: string;
  createdAt: string;
}

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  PENDING: { label: 'Chờ phản hồi', className: 'bg-[#FFF3E0] text-[#E65100]' },
  ACCEPTED: { label: 'Đã chấp nhận', className: 'bg-[#E6F4EA] text-[#137333]' },
  REJECTED: { label: 'Đã từ chối', className: 'bg-error-container text-error' },
  CANCELLED: { label: 'Đã hủy', className: 'bg-[#F5F5F5] text-[#616161]' },
};

function timeAgo(iso: string | null | undefined): string {
  if (!iso) return '—';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

/* ------------------------------------------------------------------ */
/*  StatCard Component (MD3 standard)                                 */
/* ------------------------------------------------------------------ */
function StatCard({
  icon,
  label,
  value,
  accent,
  onClick,
  isActive,
  subtext,
}: {
  icon: string;
  label: string;
  value: string | number;
  accent: string;
  onClick?: () => void;
  isActive?: boolean;
  subtext?: string;
}) {
  return (
    <div
      onClick={onClick}
      className={`bg-surface rounded-3xl p-6 shadow-md flex items-center gap-4 ${
        onClick ? 'cursor-pointer hover:shadow-lg transition-all' : ''
      } ${isActive ? 'ring-2 ring-primary bg-surface-container-low' : ''}`}
    >
      <div
        className="w-12 h-12 rounded-full flex items-center justify-center shrink-0"
        style={{ background: accent + '18' }}
      >
        <span className="material-symbols-outlined text-2xl" style={{ color: accent }}>
          {icon}
        </span>
      </div>
      <div className="overflow-hidden">
        <div className="text-[26px] font-bold text-on-surface truncate">{value}</div>
        <div className="text-[13px] text-outline mt-0.5 truncate">{label}</div>
        {subtext && <div className="text-[11px] text-on-surface-variant font-medium mt-0.5">{subtext}</div>}
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Quick Hub Items                                                   */
/* ------------------------------------------------------------------ */
const QUICK_HUB_ITEMS = [
  {
    title: 'Yêu cầu tư vấn',
    desc: 'Quản lý lịch hẹn & yêu cầu tư vấn từ mẹ bầu',
    icon: 'contact_support',
    path: '/expert/consultation-requests',
    createPath: undefined,
    accent: '#E65100',
  },
  {
    title: 'Hàng đợi câu hỏi',
    desc: 'Trả lời câu hỏi chuyên môn từ cộng đồng CareBridge',
    icon: 'forum',
    path: '/expert/question-queue',
    createPath: undefined,
    accent: '#7C3AED',
  },
  {
    title: 'Lịch rảnh làm việc',
    desc: 'Thiết lập các ca tư vấn rảnh theo tuần',
    icon: 'calendar_month',
    path: '/expert/calendar',
    createPath: '/expert/calendar',
    accent: '#137333',
  },
  {
    title: 'Trò chuyện trực tiếp',
    desc: 'Kênh chat & gọi trực tiếp với mẹ bầu',
    icon: 'chat',
    path: '/expert/direct-chats',
    createPath: undefined,
    accent: '#0061A4',
  },
  {
    title: 'Hồ sơ chuyên môn',
    desc: 'Cập nhật chuyên khoa, chức danh & nơi công tác',
    icon: 'person',
    path: '/expert/profile',
    createPath: undefined,
    accent: '#4F46E5',
  },
  {
    title: 'Chứng chỉ & Giấy tờ',
    desc: 'Theo dõi trạng thái duyệt CCHN & bằng cấp',
    icon: 'description',
    path: '/expert/credentials',
    createPath: '/expert/credentials',
    accent: '#0D9488',
  },
];

/* ------------------------------------------------------------------ */
/*  Main Component                                                    */
/* ------------------------------------------------------------------ */
export default function ExpertDashboardPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'consultations' | 'questions'>('consultations');
  const [consultations, setConsultations] = useState<ConsultationSummary[]>([]);
  const [questions, setQuestions] = useState<CommunityQuestionSummary[]>([]);
  const [isLoadingRecent, setIsLoadingRecent] = useState(true);

  const [stats, setStats] = useState({
    pendingRequests: 0,
    acceptedRequests: 0,
    openQuestions: 0,
    availableSlots: 0,
    verificationStatus: 'PENDING',
    specialty: 'Chưa cập nhật',
  });

  // Load overview stats
  const loadDashboardStats = useCallback(async () => {
    try {
      const [requestsRes, profileRes, slotsRes, questionsRes] = await Promise.allSettled([
        apiClient.get('/api/v1/consultation-requests/assigned?size=20&page=0'),
        getMyProfile(),
        getMyAvailability(),
        apiClient.get('/api/v1/community/questions?size=20&page=0'),
      ]);

      let pendingReqs = 0;
      let acceptedReqs = 0;
      if (requestsRes.status === 'fulfilled') {
        const list: ConsultationSummary[] = requestsRes.value.data?.data?.content ?? requestsRes.value.data?.data ?? [];
        pendingReqs = list.filter((r) => r.status === 'PENDING').length;
        acceptedReqs = list.filter((r) => r.status === 'ACCEPTED').length;
        setConsultations(list.slice(0, 8));
      }

      let openQs = 0;
      if (questionsRes.status === 'fulfilled') {
        const qList: CommunityQuestionSummary[] = questionsRes.value.data?.data?.content ?? questionsRes.value.data?.data ?? [];
        openQs = qList.length;
        setQuestions(qList.slice(0, 8));
      }

      let availCount = 0;
      if (slotsRes.status === 'fulfilled' && Array.isArray(slotsRes.value)) {
        availCount = slotsRes.value.filter((s: any) => new Date(s.endAt) > new Date()).length;
      }

      let vStatus = 'PENDING';
      let spec = 'Chưa cập nhật';
      if (profileRes.status === 'fulfilled' && profileRes.value) {
        vStatus = profileRes.value.verificationStatus || 'PENDING';
        spec = profileRes.value.specialty || 'Chuyên gia Y tế';
      }

      setStats({
        pendingRequests: pendingReqs,
        acceptedRequests: acceptedReqs,
        openQuestions: openQs,
        availableSlots: availCount,
        verificationStatus: vStatus,
        specialty: spec,
      });
    } catch (err) {
      console.error('Failed to load expert dashboard data', err);
    } finally {
      setIsLoadingRecent(false);
    }
  }, []);

  useEffect(() => {
    loadDashboardStats();
  }, [loadDashboardStats]);

  const getVerificationLabel = (status: string) => {
    if (status === 'APPROVED' || status === 'VERIFIED') return { text: 'Đã xác minh', cls: 'text-[#137333]' };
    if (status === 'REJECTED') return { text: 'Bị từ chối', cls: 'text-error' };
    return { text: 'Chờ xét duyệt', cls: 'text-[#E65100]' };
  };

  const vInfo = getVerificationLabel(stats.verificationStatus);

  return (
    <div className="p-8 font-sans space-y-8">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Tổng quan Chuyên gia</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Quản lý yêu cầu tư vấn, lịch rảnh làm việc và giải đáp câu hỏi từ cộng đồng CareBridge
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/expert/calendar')}
            className="py-3 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-[13px] font-semibold cursor-pointer hover:bg-surface-container-low"
          >
            Lịch rảnh làm việc
          </button>
          <button
            onClick={() => navigate('/expert/consultation-requests')}
            className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer whitespace-nowrap hover:brightness-110"
          >
            <span className="material-symbols-outlined text-lg">contact_support</span>
            Yêu cầu tư vấn
          </button>
        </div>
      </div>

      {/* Summary cards row */}
      <div className="grid grid-cols-4 gap-4 mb-7">
        <StatCard
          icon="contact_support"
          label="Yêu cầu chờ tư vấn"
          value={stats.pendingRequests}
          subtext={`${stats.acceptedRequests} cuộc hẹn đã nhận`}
          accent="#E65100"
          onClick={() => navigate('/expert/consultation-requests')}
        />
        <StatCard
          icon="forum"
          label="Câu hỏi cộng đồng"
          value={stats.openQuestions}
          subtext="Cần chuyên gia giải đáp"
          accent="#7C3AED"
          onClick={() => navigate('/expert/question-queue')}
        />
        <StatCard
          icon="calendar_month"
          label="Khung giờ rảnh khả dụng"
          value={stats.availableSlots}
          subtext="Đang mở đặt lịch"
          accent="#137333"
          onClick={() => navigate('/expert/calendar')}
        />
        <StatCard
          icon="verified"
          label="Trạng thái hồ sơ"
          value={vInfo.text}
          subtext={stats.specialty}
          accent="#0061A4"
          onClick={() => navigate('/expert/profile')}
        />
      </div>

      {/* Quick Hub Section */}
      <div className="mb-7">
        <div className="text-xs font-semibold text-outline uppercase tracking-wider mb-3">
          Lối truy cập nhanh
        </div>
        <div className="grid grid-cols-3 gap-4">
          {QUICK_HUB_ITEMS.map((item) => (
            <div
              key={item.path}
              className="bg-surface rounded-2xl p-5 shadow-md flex flex-col justify-between hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => navigate(item.path)}
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center"
                    style={{ background: item.accent + '18' }}
                  >
                    <span className="material-symbols-outlined text-xl" style={{ color: item.accent }}>
                      {item.icon}
                    </span>
                  </div>
                  {item.createPath && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(item.createPath!);
                      }}
                      className="py-1 px-3 rounded-full border border-outline-variant bg-transparent text-primary text-xs font-semibold hover:bg-surface-container-low cursor-pointer"
                    >
                      + Thiết lập
                    </button>
                  )}
                </div>
                <div className="font-bold text-base text-on-surface mb-1">{item.title}</div>
                <div className="text-xs text-on-surface-variant leading-relaxed">{item.desc}</div>
              </div>
              <div className="mt-4 pt-3 border-t border-surface-container-highest flex items-center justify-between text-xs font-semibold text-primary">
                <span>Truy cập</span>
                <span className="material-symbols-outlined text-sm">arrow_forward</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent Activity Table section */}
      <div className="bg-surface rounded-2xl p-6 shadow-md">
        <div className="flex justify-between items-center mb-5 flex-wrap gap-3">
          <div>
            <h2 className="text-lg font-bold text-on-surface m-0">Hoạt động gần đây</h2>
            <p className="text-xs text-outline mt-0.5">Danh sách các yêu cầu tư vấn và câu hỏi mới gửi tới chuyên gia</p>
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => setActiveTab('consultations')}
              className={`flex items-center gap-1.5 py-2 px-[18px] rounded-full text-[13px] font-semibold cursor-pointer ${
                activeTab === 'consultations'
                  ? 'border-2 border-primary bg-surface-container-low text-primary'
                  : 'border border-outline-variant bg-transparent text-on-surface-variant'
              }`}
            >
              Yêu cầu tư vấn ({consultations.length})
            </button>
            <button
              onClick={() => setActiveTab('questions')}
              className={`flex items-center gap-1.5 py-2 px-[18px] rounded-full text-[13px] font-semibold cursor-pointer ${
                activeTab === 'questions'
                  ? 'border-2 border-primary bg-surface-container-low text-primary'
                  : 'border border-outline-variant bg-transparent text-on-surface-variant'
              }`}
            >
              Câu hỏi cộng đồng ({questions.length})
            </button>
          </div>
        </div>

        {isLoadingRecent ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : activeTab === 'consultations' ? (
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b-2 border-surface-container-highest text-left">
                {['NGƯỜI GỬI', 'CHỦ ĐỀ TƯ VẤN', 'TRẠNG THÁI', 'THỜI GIAN', 'THAO TÁC'].map((h) => (
                  <th key={h} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {consultations.map((item) => {
                const status = STATUS_MAP[item.status] || { label: item.status, className: 'bg-surface-container-highest text-primary' };
                return (
                  <tr key={item.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                    <td className="py-3.5 px-2 font-semibold text-sm text-on-surface">
                      <div className="flex items-center gap-2.5">
                        <div className="w-7 h-7 rounded-full bg-primary-container text-primary flex items-center justify-center text-xs font-bold">
                          {(item.counterpartDisplayName || 'M')[0].toUpperCase()}
                        </div>
                        {item.counterpartDisplayName || 'Người dùng ẩn danh'}
                      </div>
                    </td>
                    <td className="py-3.5 px-2 text-sm text-on-surface-variant max-w-[280px] truncate">
                      {item.topic}
                    </td>
                    <td className="py-3.5 px-2">
                      <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${status.className}`}>
                        {status.label}
                      </span>
                    </td>
                    <td className="py-3.5 px-2 text-[13px] text-outline">{timeAgo(item.createdAt)}</td>
                    <td className="py-3.5 px-2">
                      <button
                        onClick={() => navigate('/expert/consultation-requests')}
                        className="py-1.5 px-3 rounded-lg border border-outline-variant bg-transparent text-primary text-xs font-semibold hover:bg-surface-container-low cursor-pointer flex items-center gap-1"
                      >
                        <span className="material-symbols-outlined text-sm">visibility</span>
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                );
              })}
              {consultations.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-outline">
                    Chưa có yêu cầu tư vấn nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b-2 border-surface-container-highest text-left">
                {['TIÊU ĐỀ CÂU HỎI', 'CHỦ ĐỀ', 'MỨC ĐỘ KHẨN CẤP', 'THỜI GIAN', 'THAO TÁC'].map((h) => (
                  <th key={h} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {questions.map((q) => (
                <tr key={q.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                  <td className="py-3.5 px-2 font-semibold text-sm text-on-surface max-w-[360px] truncate">
                    {q.title}
                  </td>
                  <td className="py-3.5 px-2">
                    <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                      {q.topicName || 'Chung'}
                    </span>
                  </td>
                  <td className="py-3.5 px-2">
                    <span className={`py-1 px-3 rounded-full text-xs font-semibold ${q.urgency === 'HIGH' ? 'bg-error-container text-error' : 'bg-[#FFF3E0] text-[#E65100]'}`}>
                      {q.urgency === 'HIGH' ? 'Khẩn cấp' : 'Thường'}
                    </span>
                  </td>
                  <td className="py-3.5 px-2 text-[13px] text-outline">{timeAgo(q.createdAt)}</td>
                  <td className="py-3.5 px-2">
                    <button
                      onClick={() => navigate('/expert/question-queue')}
                      className="py-1.5 px-3 rounded-lg border border-outline-variant bg-transparent text-primary text-xs font-semibold hover:bg-surface-container-low cursor-pointer flex items-center gap-1"
                    >
                      <span className="material-symbols-outlined text-sm">reply</span>
                      Trả lời
                    </button>
                  </td>
                </tr>
              ))}
              {questions.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-outline">
                    Chưa có câu hỏi nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

