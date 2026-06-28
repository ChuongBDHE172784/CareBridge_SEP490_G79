import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchContentDetail } from '../services/contentApi';
import type { ContentDetail } from '../models/content';
import { TYPE_LABELS, STAGE_LABELS, STATUS_LABELS } from '../models/content';

/* ------------------------------------------------------------------ */
/*  Mock data fallback                                                 */
/* ------------------------------------------------------------------ */
const MOCK_DETAIL: ContentDetail = {
  id: '1',
  type: 'ARTICLE',
  title: 'Dinh duong trong 3 thang dau thai ky',
  body: '<h2>Gioi thieu</h2><p>Trong 3 thang dau thai ky, che do dinh duong dong vai tro vo cung quan trong doi voi su phat trien cua thai nhi va suc khoe cua nguoi me. Day la giai doan hinh thanh cac co quan quan trong cua be.</p><h2>Cac duong chat can thiet</h2><p><strong>Axit folic:</strong> Day la duong chat thiet yeu giup ngan ngua di tat ong than kinh. Me bau nen bo sung 400-800mcg axit folic moi ngay.</p><p><strong>Sat:</strong> Giup tang luong mau va phong ngua thieu mau. Can bo sung 27mg sat moi ngay thong qua thuc pham va vien uong.</p><p><strong>Canxi:</strong> Can thiet cho su phat trien xuong va rang cua be. Me bau can 1000mg canxi moi ngay.</p><h2>Thuc pham nen an</h2><ul><li>Rau xanh dam (cai bo xoi, bong cai xanh)</li><li>Trai cay tuoi (cam, buoi, chuoi)</li><li>Ngu coc nguyen hat</li><li>Thit nac, ca, trung</li><li>Sua va cac san pham tu sua</li></ul><h2>Thuc pham nen tranh</h2><ul><li>Do song hoac tai (sushi, thit tai)</li><li>Ca co ham luong thuy ngan cao</li><li>Ruou bia va do uong co con</li><li>Caffeine qua nhieu (gioi han 200mg/ngay)</li></ul>',
  stage: 'PREGNANCY',
  topicId: 't1',
  version: 3,
  publishedAt: '2026-06-25T10:00:00Z',
  status: 'APPROVED',
  createdAt: '2026-06-01T08:00:00Z',
};

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function statusDot(status: string): string {
  if (status === 'APPROVED') return '#137333';
  if (status === 'DRAFT') return '#616161';
  return '#BA1A1A';
}

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

/* ------------------------------------------------------------------ */
/*  Version timeline mock                                              */
/* ------------------------------------------------------------------ */
const MOCK_VERSIONS = [
  { version: 3, date: '25/06/2026', author: 'Content Admin', note: 'Cap nhat noi dung dinh duong' },
  { version: 2, date: '15/06/2026', author: 'Content Admin', note: 'Bo sung phan thuc pham nen tranh' },
  { version: 1, date: '01/06/2026', author: 'Content Admin', note: 'Tao bai viet moi' },
];

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function ContentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<ContentDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDetail = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError('');
    try {
      const data = await fetchContentDetail(id);
      setDetail(data);
    } catch {
      // Fallback to mock
      setDetail({ ...MOCK_DETAIL, id });
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

  if (isLoading) {
    return <div style={{ padding: 48, textAlign: 'center', color: '#84736f', fontFamily: "'Lexend', sans-serif" }}>Dang tai...</div>;
  }

  if (error || !detail) {
    return (
      <div style={{ padding: 48, textAlign: 'center', fontFamily: "'Lexend', sans-serif" }}>
        <p style={{ color: '#BA1A1A', marginBottom: 16 }}>{error || 'Khong tim thay noi dung.'}</p>
        <button onClick={() => navigate(-1)} style={{ padding: '10px 24px', borderRadius: 9999, border: '1px solid #d6c2bd', background: 'transparent', color: '#845143', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
          Quay lai
        </button>
      </div>
    );
  }

  return (
    <div style={{ padding: 32, fontFamily: "'Lexend', sans-serif" }}>
      {/* Breadcrumbs */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#84736f', marginBottom: 16 }}>
        <span style={{ cursor: 'pointer' }} onClick={() => navigate('/content')}>Thu vien</span>
        <span className="material-symbols-outlined" style={{ fontSize: 16 }}>chevron_right</span>
        <span style={{ cursor: 'pointer' }} onClick={() => navigate('/content/list')}>{STAGE_LABELS[detail.stage]}</span>
        <span className="material-symbols-outlined" style={{ fontSize: 16 }}>chevron_right</span>
        <span style={{ color: '#524440' }}>Chi tiet bai viet</span>
      </div>

      {/* Back button */}
      <button
        onClick={() => navigate(-1)}
        style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '8px 20px', borderRadius: 9999, border: '1px solid #d6c2bd', background: 'transparent', color: '#845143', fontSize: 14, fontWeight: 600, cursor: 'pointer', marginBottom: 24 }}
      >
        <span className="material-symbols-outlined" style={{ fontSize: 18 }}>arrow_back</span>
        Quay lai
      </button>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 24 }}>
        {/* Main content area */}
        <div>
          {/* Title */}
          <h1 style={{ fontSize: 28, fontWeight: 700, color: '#271812', margin: '0 0 20px 0', lineHeight: 1.3 }}>{detail.title}</h1>

          {/* Metadata card */}
          <div style={{ background: '#fff', borderRadius: 16, padding: 20, boxShadow: '0 4px 20px rgba(90,70,63,0.06)', marginBottom: 24, display: 'flex', gap: 32, flexWrap: 'wrap' }}>
            <div>
              <div style={{ fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em', marginBottom: 4 }}>TAC GIA</div>
              <div style={{ fontSize: 14, color: '#271812', fontWeight: 500 }}>Content Admin</div>
            </div>
            <div>
              <div style={{ fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em', marginBottom: 4 }}>NGAY TAO</div>
              <div style={{ fontSize: 14, color: '#271812', fontWeight: 500 }}>{formatDate(detail.createdAt)}</div>
            </div>
            <div>
              <div style={{ fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em', marginBottom: 4 }}>CHUYEN MUC</div>
              <div style={{ fontSize: 14, color: '#271812', fontWeight: 500 }}>{STAGE_LABELS[detail.stage]}</div>
            </div>
            <div>
              <div style={{ fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em', marginBottom: 4 }}>DOI TUONG MUC TIEU</div>
              <div style={{ display: 'flex', gap: 6, marginTop: 2 }}>
                <span style={{ padding: '3px 10px', borderRadius: 9999, background: '#FFE9E3', color: '#845143', fontSize: 12, fontWeight: 500 }}>Me bau</span>
                <span style={{ padding: '3px 10px', borderRadius: 9999, background: '#FFE9E3', color: '#845143', fontSize: 12, fontWeight: 500 }}>Gia dinh</span>
              </div>
            </div>
          </div>

          {/* Article canvas */}
          <div style={{ background: '#fff', borderRadius: 16, padding: 32, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
            {/* Hero image placeholder */}
            <div style={{ width: '100%', height: 220, borderRadius: 12, background: 'linear-gradient(135deg, #FFE9E3 0%, #F6DACF 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 24 }}>
              <span className="material-symbols-outlined" style={{ fontSize: 48, color: '#C98C7B' }}>image</span>
            </div>

            {/* Body content */}
            <div
              style={{ fontSize: 15, lineHeight: 1.75, color: '#271812' }}
              dangerouslySetInnerHTML={{ __html: detail.body }}
            />
          </div>
        </div>

        {/* Right sidebar */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Status widget */}
          <div style={{ background: '#fff', borderRadius: 16, padding: 20, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
            <div style={{ fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em', marginBottom: 12 }}>TRANG THAI</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
              <span style={{ width: 10, height: 10, borderRadius: '50%', background: statusDot(detail.status) }} />
              <span style={{ fontSize: 14, fontWeight: 600, color: '#271812' }}>{STATUS_LABELS[detail.status]}</span>
            </div>
            <div style={{ fontSize: 12, color: '#84736f', marginBottom: 4 }}>Phien ban: v{detail.version}</div>
            <div style={{ fontSize: 12, color: '#84736f' }}>Xuat ban: {formatDate(detail.publishedAt)}</div>
          </div>

          {/* Action buttons */}
          <button
            onClick={() => navigate(`/content/${detail.id}/edit`)}
            style={{ width: '100%', padding: '14px 0', borderRadius: 16, background: '#C98C7B', color: '#fff', border: 'none', fontSize: 14, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>edit</span>
            Chinh sua noi dung
          </button>
          <button
            onClick={() => navigate(`/content/${detail.id}/preview`)}
            style={{ width: '100%', padding: '14px 0', borderRadius: 16, background: 'transparent', color: '#845143', border: '1px solid #d6c2bd', fontSize: 14, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>send</span>
            Gui phe duyet
          </button>

          {/* Version history */}
          <div style={{ background: '#fff', borderRadius: 16, padding: 20, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
            <div style={{ fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em', marginBottom: 16 }}>LICH SU PHIEN BAN</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
              {MOCK_VERSIONS.map((v, idx) => (
                <div key={v.version} style={{ display: 'flex', gap: 12, paddingBottom: idx < MOCK_VERSIONS.length - 1 ? 16 : 0 }}>
                  {/* Timeline dot and line */}
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: 16 }}>
                    <div style={{ width: 10, height: 10, borderRadius: '50%', background: idx === 0 ? '#C98C7B' : '#d6c2bd', flexShrink: 0 }} />
                    {idx < MOCK_VERSIONS.length - 1 && (
                      <div style={{ width: 2, flex: 1, background: '#FADCD3', marginTop: 4 }} />
                    )}
                  </div>
                  <div style={{ paddingBottom: idx < MOCK_VERSIONS.length - 1 ? 0 : 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 600, color: '#271812' }}>v{v.version} - {v.date}</div>
                    <div style={{ fontSize: 12, color: '#84736f', marginTop: 2 }}>{v.author}</div>
                    <div style={{ fontSize: 12, color: '#524440', marginTop: 2 }}>{v.note}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
