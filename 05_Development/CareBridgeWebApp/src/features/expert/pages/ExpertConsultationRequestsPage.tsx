import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';

interface ConsultationRequest {
  id: string;
  counterpartDisplayName: string;
  topic: string;
  status: string;
  createdAt: string;
  directConversationId?: string | null;
}

const STATUS_TABS: { key: string; label: string; status?: string }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'pending', label: 'Chờ phản hồi', status: 'PENDING' },
  { key: 'accepted', label: 'Đã chấp nhận', status: 'ACCEPTED' },
  { key: 'rejected', label: 'Đã từ chối', status: 'REJECTED' },
  { key: 'cancelled', label: 'Đã hủy', status: 'CANCELLED' },
  // Hết hạn là kết cục thường gặp nhất sau "chờ phản hồi": mẹ đợi 48 giờ không ai
  // trả lời thì yêu cầu tự đóng. Thiếu tab này thì nó chỉ hiện ở "Tất cả", lẫn giữa
  // mọi thứ khác, nên chuyên gia không thấy được mình đã để lỡ bao nhiêu người.
  { key: 'expired', label: 'Đã hết hạn', status: 'EXPIRED' },
];

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  PENDING: { label: 'Chờ phản hồi', className: 'bg-[#FFF3E0] text-[#E65100]' },
  ACCEPTED: { label: 'Đã chấp nhận', className: 'bg-[#E6F4EA] text-[#137333]' },
  REJECTED: { label: 'Đã từ chối', className: 'bg-error-container text-error' },
  CANCELLED: { label: 'Đã hủy', className: 'bg-[#F5F5F5] text-[#616161]' },
  EXPIRED: { label: 'Đã hết hạn', className: 'bg-[#F5F5F5] text-[#616161]' },
};

function timeAgo(iso: string): string {
  if (!iso) return '—';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

export default function ExpertConsultationRequestsPage() {
  const navigate = useNavigate();
  const [requests, setRequests] = useState<ConsultationRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('all');
  const [searchInput, setSearchInput] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const pageSize = 10;

  const fetchRequests = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const tabObj = STATUS_TABS.find((t) => t.key === activeTab);
      const params = new URLSearchParams({
        size: String(pageSize),
        page: String(page),
      });
      if (tabObj?.status) {
        params.append('status', tabObj.status);
      }
      const { data } = await apiClient.get(`/api/v1/consultation-requests/assigned?${params.toString()}`);
      const content = data.data?.content ?? data.data ?? [];
      const totalElements = data.data?.totalElements ?? content.length;
      setRequests(content);
      setTotal(totalElements);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Không thể tải danh sách yêu cầu tư vấn');
    } finally {
      setLoading(false);
    }
  }, [activeTab, page]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleAction = async (id: string, action: 'accept' | 'reject') => {
    try {
      if (action === 'accept') {
        const { data } = await apiClient.patch(`/api/v1/consultation-requests/${id}/accept`);
        const convId = data?.data?.directConversationId;
        if (convId) {
          navigate(`/expert/direct-chats/${convId}`);
          return;
        }
      } else {
        await apiClient.post(`/api/v1/consultation-requests/${id}/reject`, {
          reason: 'Chuyên gia bận lịch công tác',
        });
      }
      await fetchRequests();
    } catch {
      alert('Thao tác thất bại. Vui lòng thử lại.');
    }
  };

  const handleGoToChat = async (req: ConsultationRequest) => {
    if (req.directConversationId) {
      navigate(`/expert/direct-chats/${req.directConversationId}`);
      return;
    }
    try {
      const { data } = await apiClient.get(`/api/v1/consultation-requests/${req.id}`);
      const convId = data?.data?.directConversationId;
      if (convId) {
        navigate(`/expert/direct-chats/${convId}`);
        return;
      }
    } catch {
      // fallback
    }
    navigate('/expert/direct-chats');
  };

  const filteredRequests = useMemo(() => {
    if (!searchInput.trim()) return requests;
    const q = searchInput.trim().toLowerCase();
    return requests.filter(
      (r) =>
        r.counterpartDisplayName?.toLowerCase().includes(q) ||
        r.topic?.toLowerCase().includes(q)
    );
  }, [requests, searchInput]);

  const totalPages = Math.ceil(total / pageSize) || 1;

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Yêu cầu tư vấn</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Quản lý và tiếp nhận các yêu cầu tư vấn sức khỏe mẹ &amp; bé từ người dùng
          </p>
        </div>
        <button
          onClick={() => navigate('/expert/direct-chats')}
          className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer whitespace-nowrap hover:brightness-110"
        >
          <span className="material-symbols-outlined text-lg">chat</span>
          Trò chuyện trực tiếp
        </button>
      </div>

      {error && (
        <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">
          {error}
        </div>
      )}

      {/* Status tabs + search bar */}
      <div className="flex justify-between items-center mb-5 flex-wrap gap-3">
        <div className="flex gap-2">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => {
                setActiveTab(tab.key);
                setPage(0);
              }}
              className={`flex items-center gap-1.5 py-2 px-[18px] rounded-full text-[13px] font-semibold cursor-pointer ${
                activeTab === tab.key
                  ? 'border-2 border-primary bg-surface-container-low text-primary'
                  : 'border border-outline-variant bg-transparent text-on-surface-variant'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="flex gap-2 flex-1 justify-end">
          <div className="relative max-w-[280px] flex-1">
            <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">
              search
            </span>
            <input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Tìm theo tên hoặc chủ đề..."
              className="w-full py-2.5 pr-[14px] pl-[42px] rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
            />
          </div>
        </div>
      </div>

      {/* Table container */}
      <div className="bg-surface rounded-2xl p-6 shadow-md w-full overflow-x-auto">
        {loading ? (
          <div className="py-12 text-center text-outline">Đang tải danh sách...</div>
        ) : (
          <>
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left">
                  <th className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] w-[22%]">NGƯỜI GỬI</th>
                  <th className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] w-[38%]">CHỦ ĐỀ TƯ VẤN</th>
                  <th className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] w-[15%]">TRẠNG THÁI</th>
                  <th className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] w-[13%]">THỜI GIAN GỬI</th>
                  <th className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] w-[12%] text-right">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {filteredRequests.map((req) => {
                  const status = STATUS_MAP[req.status] || {
                    label: req.status,
                    className: 'bg-surface-container-highest text-primary',
                  };
                  return (
                    <tr key={req.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                      <td className="py-3.5 px-3 font-semibold text-sm text-on-surface">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-primary-container text-primary flex items-center justify-center font-bold text-xs">
                            {(req.counterpartDisplayName || 'M')[0].toUpperCase()}
                          </div>
                          <div>
                            <div>{req.counterpartDisplayName || 'Người dùng ẩn danh'}</div>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-3 text-sm text-on-surface-variant">
                        {req.topic}
                      </td>
                      <td className="py-3.5 px-3">
                        <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${status.className}`}>
                          {status.label}
                        </span>
                      </td>
                      <td className="py-3.5 px-3 text-[13px] text-outline">
                        {timeAgo(req.createdAt)}
                      </td>
                      <td className="py-3.5 px-3 text-right">
                        {req.status === 'PENDING' ? (
                          <div className="flex gap-2 justify-end">
                            <button
                              onClick={() => handleAction(req.id, 'reject')}
                              className="py-1.5 px-3 rounded-lg border border-error/30 bg-error-container/40 text-error text-xs font-semibold hover:bg-error-container cursor-pointer"
                            >
                              Từ chối
                            </button>
                            <button
                              onClick={() => handleAction(req.id, 'accept')}
                              className="py-1.5 px-3 rounded-lg border border-primary bg-primary text-on-primary text-xs font-semibold hover:brightness-110 cursor-pointer"
                            >
                              Chấp nhận
                            </button>
                          </div>
                        ) : req.status === 'ACCEPTED' ? (
                          <button
                            onClick={() => handleGoToChat(req)}
                            className="py-1.5 px-3.5 rounded-lg border border-outline-variant bg-surface-container-low text-primary text-xs font-semibold hover:bg-surface-bright cursor-pointer flex items-center gap-1.5 justify-end"
                          >
                            <span className="material-symbols-outlined text-base">chat</span>
                            Vào nhắn tin
                          </button>
                        ) : req.status === 'EXPIRED' ? (
                          <span className="text-xs text-outline">Đã đóng do quá hạn</span>
                        ) : (
                          <span className="text-xs text-outline">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}

                {filteredRequests.length === 0 && (
                  <tr>
                    <td colSpan={5} className="py-12 text-center text-outline">
                      Chưa có yêu cầu tư vấn nào phù hợp.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>

            {/* Pagination */}
            <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
              <span className="text-[13px] text-outline">
                Hiển thị {total === 0 ? 0 : page * pageSize + 1}-{Math.min((page + 1) * pageSize, total)} trong {total} yêu cầu
              </span>
              <div className="flex gap-1">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${
                    page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer hover:bg-surface-container-low'
                  }`}
                >
                  <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
                </button>
                {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                  const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
                  const p = startPage + i;
                  if (p >= totalPages) return null;
                  return (
                    <button
                      key={p}
                      onClick={() => setPage(p)}
                      className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${
                        page === p
                          ? 'border-0 bg-primary text-on-primary'
                          : 'border border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-container-low'
                      }`}
                    >
                      {p + 1}
                    </button>
                  );
                })}
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${
                    page >= totalPages - 1 ? 'opacity-40 cursor-default' : 'cursor-pointer hover:bg-surface-container-low'
                  }`}
                >
                  <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

