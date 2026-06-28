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
  { id: '1', question: 'Khi nao can gap bac si san khoa khan cap?', answer: 'Khi co cac trieu chung nhu ra mau nhieu, dau bung du doi, mat thi luc, phu mat va tay...', topic: 'Thai ky', topicColor: '#C98C7B', source: 'WHO Guidelines', isVerified: true, publishedAt: '2026-06-25T10:00:00Z' },
  { id: '2', question: 'Bo sung sat nhu the nao trong thai ky?', answer: 'Me bau nen bo sung 27mg sat moi ngay. Uong sat cung voi vitamin C de tang hap thu...', topic: 'Dinh duong', topicColor: '#137333', source: 'Bo Y Te VN', isVerified: true, publishedAt: '2026-06-22T09:15:00Z' },
  { id: '3', question: 'Tre so sinh can nang bao nhieu la binh thuong?', answer: 'Tre so sinh du thang thuong nang tu 2.5kg den 4kg. Can nang duoi 2.5kg duoc coi la nhe can...', topic: 'Cham be', topicColor: '#845143', source: 'UNICEF', isVerified: true, publishedAt: '2026-06-20T14:00:00Z' },
  { id: '4', question: 'Me bau co nen tap the duc khong?', answer: 'Co, tap the duc nhe nhang rat tot cho suc khoe me bau. Cac bai tap duoc khuyen khich: di bo, yoga...', topic: 'Thai ky', topicColor: '#C98C7B', source: 'ACOG', isVerified: true, publishedAt: null },
  { id: '5', question: 'Lam sao de tang luong sua me?', answer: 'De tang luong sua me, me nen cho con bu thuong xuyen, uong nhieu nuoc, an du chat dinh duong...', topic: 'Sau sinh', topicColor: '#E65100', source: 'WHO', isVerified: false, publishedAt: null },
  { id: '6', question: 'Khi nao nen cat sua me cho be?', answer: 'WHO khuyen cao nuoi con hoan toan bang sua me trong 6 thang dau va tiep tuc cho bu den 2 tuoi...', topic: 'Cham be', topicColor: '#845143', source: 'WHO Guidelines', isVerified: true, publishedAt: '2026-06-18T11:00:00Z' },
  { id: '7', question: 'Thuc pham nao me bau nen tranh?', answer: 'Me bau nen tranh do song, ca co ham luong thuy ngan cao, ruou bia, caffeine qua nhieu...', topic: 'Dinh duong', topicColor: '#137333', source: 'FDA', isVerified: true, publishedAt: '2026-06-15T08:30:00Z' },
  { id: '8', question: 'Tiem vac xin gi truoc khi mang thai?', answer: 'Truoc khi mang thai, phu nu nen tiem day du: Rubella, Viem gan B, Thuy dau, Cum mua...', topic: 'Chuan bi', topicColor: '#6e5a52', source: 'CDC', isVerified: true, publishedAt: '2026-06-10T10:00:00Z' },
];

const TABS = [
  { key: 'all', label: 'Tat ca', count: 124 },
  { key: 'approved', label: 'Da duyet', count: 87 },
  { key: 'pending', label: 'Cho duyet', count: 25 },
  { key: 'draft', label: 'Ban nhap', count: 12 },
];

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function statusBadge(publishedAt: string | null): { label: string; bg: string; color: string } {
  if (publishedAt) return { label: 'Da duyet', bg: '#E6F4EA', color: '#137333' };
  return { label: 'Cho duyet', bg: '#FFF3E0', color: '#E65100' };
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
      const mapped: FaqDisplay[] = data.content.map(item => ({
        id: item.id,
        question: item.title,
        answer: '(Xem chi tiet de doc cau tra loi day du...)',
        topic: 'Thai ky',
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
    <div style={{ padding: 32, fontFamily: "'Lexend', sans-serif" }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 26, fontWeight: 700, color: '#271812', margin: 0 }}>Quan ly FAQ</h1>
          <p style={{ color: '#524440', fontSize: 14, marginTop: 4 }}>Quan ly cac cau hoi thuong gap va cau tra loi cho nguoi dung</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '12px 20px', borderRadius: 9999, border: '1px solid #d6c2bd', background: 'transparent', color: '#845143', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>download</span>
            Xuat du lieu
          </button>
          <button
            onClick={() => navigate('/content/create?type=FAQ')}
            style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 24px', borderRadius: 9999, background: '#C98C7B', color: '#fff', border: 'none', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
            Tao FAQ Moi
          </button>
        </div>
      </div>

      {/* Filter tabs + topic filter */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          {TABS.map(tab => (
            <button
              key={tab.key}
              onClick={() => { setActiveTab(tab.key); setPage(0); }}
              style={{
                display: 'flex', alignItems: 'center', gap: 6,
                padding: '8px 18px', borderRadius: 9999,
                border: activeTab === tab.key ? '2px solid #845143' : '1px solid #d6c2bd',
                background: activeTab === tab.key ? '#FFF1EC' : 'transparent',
                color: activeTab === tab.key ? '#845143' : '#524440',
                fontSize: 13, fontWeight: 600, cursor: 'pointer',
              }}
            >
              {tab.label}
              <span style={{
                padding: '1px 8px', borderRadius: 9999, fontSize: 11,
                background: activeTab === tab.key ? '#845143' : '#F5F5F5',
                color: activeTab === tab.key ? '#fff' : '#616161',
              }}>
                {tab.count}
              </span>
            </button>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <select style={{ padding: '8px 16px', borderRadius: 16, border: '1px solid #d6c2bd', background: '#fff', fontSize: 13, color: '#524440', cursor: 'pointer', fontFamily: "'Lexend', sans-serif" }}>
            <option>Tat ca chu de</option>
            <option>Thai ky</option>
            <option>Dinh duong</option>
            <option>Cham be</option>
            <option>Sau sinh</option>
          </select>
          <button style={{ padding: '8px 16px', borderRadius: 16, border: '1px solid #d6c2bd', background: 'transparent', color: '#524440', fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}>
            <span className="material-symbols-outlined" style={{ fontSize: 16 }}>tune</span>
            Loc them
          </button>
        </div>
      </div>

      {/* Data table */}
      <div style={{ background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
        {isLoading ? (
          <div style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Dang tai...</div>
        ) : (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #fadcd3', textAlign: 'left' }}>
                  <th style={{ padding: '12px 8px', width: 40 }}>
                    <input type="checkbox" checked={selectedIds.size === faqs.length && faqs.length > 0} onChange={toggleSelectAll} style={{ cursor: 'pointer', accentColor: '#845143' }} />
                  </th>
                  {['CAU HOI & TRA LOI', 'CHU DE', 'NGUON XAC MINH', 'TRANG THAI', 'HANH DONG'].map(h => (
                    <th key={h} style={{ padding: '12px 8px', fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {faqs.map(faq => {
                  const status = statusBadge(faq.publishedAt);
                  return (
                    <tr
                      key={faq.id}
                      style={{ borderBottom: '1px solid #fadcd3' }}
                      onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = '#FFF8F6'; }}
                      onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
                    >
                      <td style={{ padding: '14px 8px', width: 40 }}>
                        <input type="checkbox" checked={selectedIds.has(faq.id)} onChange={() => toggleSelect(faq.id)} style={{ cursor: 'pointer', accentColor: '#845143' }} />
                      </td>
                      <td style={{ padding: '14px 8px', maxWidth: 400 }}>
                        <div style={{ fontWeight: 600, fontSize: 14, color: '#271812', marginBottom: 4 }}>{faq.question}</div>
                        <div style={{ fontSize: 13, color: '#84736f', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 380 }}>{faq.answer}</div>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span style={{ width: 8, height: 8, borderRadius: '50%', background: faq.topicColor, flexShrink: 0 }} />
                          <span style={{ fontSize: 13, color: '#524440' }}>{faq.topic}</span>
                        </div>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                          {faq.isVerified && <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#137333' }}>verified</span>}
                          <span style={{ fontSize: 13, color: '#524440' }}>{faq.source}</span>
                        </div>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        <span style={{ padding: '4px 14px', borderRadius: 9999, background: status.bg, color: status.color, fontSize: 12, fontWeight: 600 }}>
                          {status.label}
                        </span>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        <div style={{ display: 'flex', gap: 4 }}>
                          <button
                            onClick={() => navigate(`/content/${faq.id}`)}
                            style={{ width: 32, height: 32, borderRadius: 8, border: '1px solid #d6c2bd', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                            title="Chinh sua"
                          >
                            <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#845143' }}>edit</span>
                          </button>
                          <button
                            style={{ width: 32, height: 32, borderRadius: 8, border: '1px solid #d6c2bd', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                            title="Xoa"
                          >
                            <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#BA1A1A' }}>delete</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {faqs.length === 0 && (
                  <tr><td colSpan={6} style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Khong co FAQ nao.</td></tr>
                )}
              </tbody>
            </table>

            {/* Pagination */}
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', marginTop: 20, paddingTop: 16, borderTop: '1px solid #fadcd3', gap: 4 }}>
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                style={{ width: 36, height: 36, borderRadius: '50%', border: '1px solid #d6c2bd', background: '#fff', cursor: page === 0 ? 'default' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: page === 0 ? 0.4 : 1 }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#845143' }}>chevron_left</span>
              </button>
              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
                const p = startPage + i;
                if (p >= totalPages) return null;
                return (
                  <button
                    key={p}
                    onClick={() => setPage(p)}
                    style={{ width: 36, height: 36, borderRadius: '50%', border: page === p ? 'none' : '1px solid #d6c2bd', background: page === p ? '#845143' : '#fff', color: page === p ? '#fff' : '#524440', fontSize: 14, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                  >
                    {p + 1}
                  </button>
                );
              })}
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                style={{ width: 36, height: 36, borderRadius: '50%', border: '1px solid #d6c2bd', background: '#fff', cursor: page >= totalPages - 1 ? 'default' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: page >= totalPages - 1 ? 0.4 : 1 }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#845143' }}>chevron_right</span>
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
