import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchContentList } from '../services/contentApi';
import type { ContentListItem } from '../models/content';

/* ------------------------------------------------------------------ */
/*  Mock FAQ data                                                      */
/* ------------------------------------------------------------------ */
interface FaqDisplay {
  id: string;
  question: string;
  answer: string;
  topic: string;
  topicColor: string;
  source: string;
  isVerified: boolean;
  publishedAt: string | null;
}

const MOCK_FAQS: FaqDisplay[] = [
  { id: '1', question: 'Khi nao can gap bac si san khoa khan cap?', answer: 'Khi co cac trieu chung nhu ra mau nhieu, dau bung du doi, mat thi luc, phu mat va tay...', topic: 'Thai kỳ', topicColor: '#C98C7B', source: 'WHO Guidelines', isVerified: true, publishedAt: '2026-06-25T10:00:00Z' },
  { id: '2', question: 'Bo sung sat nhu the nao trong thai ky?', answer: 'Me bau nen bo sung 27mg sat moi ngay. Uong sat cung voi vitamin C de tang hap thu...', topic: 'Dinh dưỡng', topicColor: '#137333', source: 'Bộ Y Tế VN', isVerified: true, publishedAt: '2026-06-22T09:15:00Z' },
  { id: '3', question: 'Tre so sinh can nang bao nhieu la binh thuong?', answer: 'Tre so sinh du thang thuong nang tu 2.5kg den 4kg. Can nang duoi 2.5kg duoc coi la nhe can...', topic: 'Chăm bé', topicColor: '#845143', source: 'UNICEF', isVerified: true, publishedAt: '2026-06-20T14:00:00Z' },
  { id: '4', question: 'Me bau co nen tap the duc khong?', answer: 'Co, tap the duc nhe nhang rat tot cho suc khoe me bau. Cac bai tap duoc khuyen khich: di bo, yoga...', topic: 'Thai kỳ', topicColor: '#C98C7B', source: 'ACOG', isVerified: true, publishedAt: null },
  { id: '5', question: 'Lam sao de tang luong sua me?', answer: 'De tang luong sua me, me nen cho con bu thuong xuyen, uong nhieu nuoc, an du chat dinh duong...', topic: 'Sau sinh', topicColor: '#E65100', source: 'WHO', isVerified: false, publishedAt: null },
  { id: '6', question: 'Khi nao nen cat sua me cho be?', answer: 'WHO khuyen cao nuoi con hoan toan bang sua me trong 6 thang dau va tiep tuc cho bu den 2 tuoi...', topic: 'Chăm bé', topicColor: '#845143', source: 'WHO Guidelines', isVerified: true, publishedAt: '2026-06-18T11:00:00Z' },
  { id: '7', question: 'Thuc pham nao me bau nen tranh?', answer: 'Me bau nen tranh do song, ca co ham luong thuy ngan cao, ruou bia, caffeine qua nhieu...', topic: 'Dinh dưỡng', topicColor: '#137333', source: 'FDA', isVerified: true, publishedAt: '2026-06-15T08:30:00Z' },
  { id: '8', question: 'Tiem vac xin gi truoc khi mang thai?', answer: 'Truoc khi mang thai, phu nu nen tiem day du: Rubella, Viem gan B, Thuy dau, Cum mua...', topic: 'Chuẩn bị', topicColor: '#6e5a52', source: 'CDC', isVerified: true, publishedAt: '2026-06-10T10:00:00Z' },
];

const TABS = [
  { key: 'all', label: 'Tất cả', count: 124 },
  { key: 'approved', label: 'Đã duyệt', count: 87 },
  { key: 'pending', label: 'Chờ duyệt', count: 25 },
  { key: 'draft', label: 'Bản nháp', count: 12 },
];

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function statusBadge(publishedAt: string | null): { label: string; className: string } {
  if (publishedAt) return { label: 'Đã duyệt', className: 'bg-[#E6F4EA] text-[#137333]' };
  return { label: 'Chờ duyệt', className: 'bg-[#FFF3E0] text-[#E65100]' };
}

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function FaqListPage() {
  const navigate = useNavigate();
  const [faqs, setFaqs] = useState<FaqDisplay[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('all');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(124);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const pageSize = 10;

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await fetchContentList({ type: 'FAQ', page, size: pageSize });
      // Map API items to FaqDisplay — in production the API would return richer FAQ data
      const mapped: FaqDisplay[] = data.content.map((item: ContentListItem) => ({
        id: item.id,
        question: item.title,
        answer: '(Xem chi tiết để đọc câu trả lời đầy đủ...)',
        topic: 'Thai kỳ',
        topicColor: '#C98C7B',
        source: 'CareBridge',
        isVerified: !!item.publishedAt,
        publishedAt: item.publishedAt,
      }));
      setFaqs(mapped);
      setTotal(data.totalElements);
    } catch {
      // Fallback to mock data
      let filtered = MOCK_FAQS;
      if (activeTab === 'approved') filtered = filtered.filter(f => f.publishedAt);
      else if (activeTab === 'pending') filtered = filtered.filter(f => !f.publishedAt && f.isVerified);
      else if (activeTab === 'draft') filtered = filtered.filter(f => !f.publishedAt && !f.isVerified);
      setFaqs(filtered.slice(page * pageSize, (page + 1) * pageSize));
      setTotal(filtered.length);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, activeTab]);

  useEffect(() => { loadData(); }, [loadData]);

  const totalPages = Math.ceil(total / pageSize);

  const toggleSelect = (id: string) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === faqs.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(faqs.map(f => f.id)));
    }
  };

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Quản lý FAQ</h1>
          <p className="text-on-surface-variant text-sm mt-1">Quản lý các câu hỏi thường gặp và câu trả lời cho người dùng</p>
        </div>
        <div className="flex gap-2.5">
          <button className="flex items-center gap-1.5 py-3 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer">
            <span className="material-symbols-outlined text-lg">download</span>
            Xuất dữ liệu
          </button>
          <button
            onClick={() => navigate('/content/create?type=FAQ')}
            className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary-container text-on-primary border-0 text-sm font-semibold cursor-pointer"
          >
            <span className="material-symbols-outlined text-lg">add</span>
            Tạo FAQ Mới
          </button>
        </div>
      </div>

      {/* Filter tabs + topic filter */}
      <div className="flex justify-between items-center mb-5">
        <div className="flex gap-2">
          {TABS.map(tab => (
            <button
              key={tab.key}
              onClick={() => { setActiveTab(tab.key); setPage(0); }}
              className={`flex items-center gap-1.5 py-2 px-[18px] rounded-full text-[13px] font-semibold cursor-pointer ${
                activeTab === tab.key
                  ? 'border-2 border-primary bg-surface-container-low text-primary'
                  : 'border border-outline-variant bg-transparent text-on-surface-variant'
              }`}
            >
              {tab.label}
              <span className={`py-0.5 px-2 rounded-full text-[11px] ${
                activeTab === tab.key
                  ? 'bg-primary text-on-primary'
                  : 'bg-[#F5F5F5] text-[#616161]'
              }`}>
                {tab.count}
              </span>
            </button>
          ))}
        </div>
        <div className="flex gap-2">
          <select className="py-2 px-4 rounded-2xl border border-outline-variant bg-surface text-[13px] text-on-surface-variant cursor-pointer font-sans">
            <option>Tất cả chủ đề</option>
            <option>Thai kỳ</option>
            <option>Dinh dưỡng</option>
            <option>Chăm bé</option>
            <option>Sau sinh</option>
          </select>
          <button className="py-2 px-4 rounded-2xl border border-outline-variant bg-transparent text-on-surface-variant text-[13px] font-semibold cursor-pointer flex items-center gap-1">
            <span className="material-symbols-outlined text-base">tune</span>
            Lọc thêm
          </button>
        </div>
      </div>

      {/* Data table */}
      <div className="bg-surface rounded-2xl p-6 shadow-md">
        {isLoading ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : (
          <>
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left">
                  <th className="py-3 px-2 w-10">
                    <input
                      type="checkbox"
                      checked={selectedIds.size === faqs.length && faqs.length > 0}
                      onChange={toggleSelectAll}
                      className="cursor-pointer accent-primary"
                    />
                  </th>
                  {['CÂU HỎI & TRẢ LỜI', 'CHỦ ĐỀ', 'NGUỒN XÁC MINH', 'TRẠNG THÁI', 'HÀNH ĐỘNG'].map(h => (
                    <th key={h} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {faqs.map(faq => {
                  const status = statusBadge(faq.publishedAt);
                  return (
                    <tr
                      key={faq.id}
                      className="border-b border-surface-container-highest hover:bg-surface-bright"
                    >
                      <td className="py-3.5 px-2 w-10">
                        <input
                          type="checkbox"
                          checked={selectedIds.has(faq.id)}
                          onChange={() => toggleSelect(faq.id)}
                          className="cursor-pointer accent-primary"
                        />
                      </td>
                      <td className="py-3.5 px-2 max-w-[400px]">
                        <div className="font-semibold text-sm text-on-surface mb-1">{faq.question}</div>
                        <div className="text-[13px] text-outline overflow-hidden text-ellipsis whitespace-nowrap max-w-[380px]">{faq.answer}</div>
                      </td>
                      <td className="py-3.5 px-2">
                        <div className="flex items-center gap-1.5">
                          <span
                            className="w-2 h-2 rounded-full shrink-0"
                            style={{ background: faq.topicColor }}
                          />
                          <span className="text-[13px] text-on-surface-variant">{faq.topic}</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-2">
                        <div className="flex items-center gap-1">
                          {faq.isVerified && <span className="material-symbols-outlined text-[#137333] text-base">verified</span>}
                          <span className="text-[13px] text-on-surface-variant">{faq.source}</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-2">
                        <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${status.className}`}>
                          {status.label}
                        </span>
                      </td>
                      <td className="py-3.5 px-2">
                        <div className="flex gap-1">
                          <button
                            onClick={() => navigate(`/content/${faq.id}`)}
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center"
                            title="Chỉnh sửa"
                          >
                            <span className="material-symbols-outlined text-primary text-base">edit</span>
                          </button>
                          <button
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center"
                            title="Xóa"
                          >
                            <span className="material-symbols-outlined text-error text-base">delete</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {faqs.length === 0 && (
                  <tr><td colSpan={6} className="py-12 text-center text-outline">Không có FAQ nào.</td></tr>
                )}
              </tbody>
            </table>

            {/* Pagination */}
            <div className="flex justify-center items-center mt-5 pt-4 border-t border-surface-container-highest gap-1">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
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
                    className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${page === p ? 'border-0 bg-primary text-on-primary' : 'border border-outline-variant bg-surface text-on-surface-variant'}`}
                  >
                    {p + 1}
                  </button>
                );
              })}
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page >= totalPages - 1 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
              >
                <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
