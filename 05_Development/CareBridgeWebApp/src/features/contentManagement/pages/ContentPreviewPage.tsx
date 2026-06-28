import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchContentDetail } from '../services/contentApi';
import type { ContentDetail } from '../models/content';

/* ------------------------------------------------------------------ */
/*  Mock data fallback                                                 */
/* ------------------------------------------------------------------ */
const MOCK_DETAIL: ContentDetail = {
  id: '1',
  type: 'ARTICLE',
  title: 'Dinh duong trong 3 thang dau thai ky',
  body: '<h2>Gioi thieu</h2><p>Trong 3 thang dau thai ky, che do dinh duong dong vai tro vo cung quan trong doi voi su phat trien cua thai nhi va suc khoe cua nguoi me. Day la giai doan hinh thanh cac co quan quan trong cua be.</p><h2>Cac duong chat can thiet</h2><p><strong>Axit folic:</strong> Day la duong chat thiet yeu giup ngan ngua di tat ong than kinh. Me bau nen bo sung 400-800mcg axit folic moi ngay.</p><p><strong>Sat:</strong> Giup tang luong mau va phong ngua thieu mau. Can bo sung 27mg sat moi ngay thong qua thuc pham va vien uong.</p><p><strong>Canxi:</strong> Can thiet cho su phat trien xuong va rang cua be. Me bau can 1000mg canxi moi ngay.</p><h2>Thuc pham nen an</h2><ul><li>Rau xanh dam (cai bo xoi, bong cai xanh)</li><li>Trai cay tuoi (cam, buoi, chuoi)</li><li>Ngu coc nguyen hat</li><li>Thit nac, ca, trung</li><li>Sua va cac san pham tu sua</li></ul>',
  stage: 'PREGNANCY',
  topicId: 't1',
  version: 3,
  publishedAt: '2026-06-25T10:00:00Z',
  status: 'APPROVED',
  createdAt: '2026-06-01T08:00:00Z',
};

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function ContentPreviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<ContentDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [viewMode, setViewMode] = useState<'mobile' | 'desktop'>('mobile');

  const loadDetail = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    try {
      const data = await fetchContentDetail(id);
      setDetail(data);
    } catch {
      setDetail({ ...MOCK_DETAIL, id });
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => { loadDetail(); }, [loadDetail]);

  if (isLoading) {
    return <div style={{ padding: 48, textAlign: 'center', color: '#84736f', fontFamily: "'Lexend', sans-serif" }}>Dang tai...</div>;
  }

  if (!detail) {
    return (
      <div style={{ padding: 48, textAlign: 'center', fontFamily: "'Lexend', sans-serif" }}>
        <p style={{ color: '#BA1A1A', marginBottom: 16 }}>Khong tim thay noi dung.</p>
        <button onClick={() => navigate(-1)} style={{ padding: '10px 24px', borderRadius: 9999, border: '1px solid #d6c2bd', background: 'transparent', color: '#845143', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
          Quay lai
        </button>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', background: 'radial-gradient(ellipse at 50% 30%, #ffe9e3 0%, #F6F1EC 70%)', fontFamily: "'Lexend', sans-serif" }}>
      {/* Toolbar */}
      <div style={{ background: '#fff', borderBottom: '1px solid #FADCD3', padding: '12px 32px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'sticky', top: 0, zIndex: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <button
            onClick={() => navigate(-1)}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '8px 20px', borderRadius: 9999, border: '1px solid #d6c2bd', background: 'transparent', color: '#845143', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 16 }}>arrow_back</span>
            Back to Edit
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ padding: '4px 12px', borderRadius: 9999, background: '#FFE9E3', color: '#845143', fontSize: 12, fontWeight: 600, textTransform: 'uppercase' as const }}>
              DANG XEM TRUOC
            </span>
            <span style={{ fontSize: 14, fontWeight: 600, color: '#271812', maxWidth: 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {detail.title}
            </span>
          </div>
        </div>

        {/* Mobile / Desktop toggle */}
        <div style={{ display: 'flex', borderRadius: 9999, border: '1px solid #d6c2bd', overflow: 'hidden' }}>
          <button
            onClick={() => setViewMode('mobile')}
            style={{ padding: '8px 16px', border: 'none', background: viewMode === 'mobile' ? '#845143' : '#fff', color: viewMode === 'mobile' ? '#fff' : '#524440', fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 16 }}>smartphone</span>
            Mobile
          </button>
          <button
            onClick={() => setViewMode('desktop')}
            style={{ padding: '8px 16px', border: 'none', borderLeft: '1px solid #d6c2bd', background: viewMode === 'desktop' ? '#845143' : '#fff', color: viewMode === 'desktop' ? '#fff' : '#524440', fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 16 }}>desktop_windows</span>
            Desktop
          </button>
        </div>
      </div>

      {/* Preview area */}
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start', padding: '48px 32px', minHeight: 'calc(100vh - 60px)' }}>
        {viewMode === 'mobile' ? (
          /* Mobile device frame */
          <div style={{
            width: 390,
            height: 844,
            borderRadius: 40,
            border: '8px solid #FADCD3',
            background: '#fff',
            boxShadow: '0 24px 80px rgba(90,70,63,0.18), 0 8px 32px rgba(90,70,63,0.08)',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
          }}>
            {/* Status bar mockup */}
            <div style={{ height: 48, background: '#FFF8F6', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px', flexShrink: 0 }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: '#271812' }}>9:41</span>
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#524440' }}>signal_cellular_alt</span>
                <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#524440' }}>wifi</span>
                <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#524440' }}>battery_full</span>
              </div>
            </div>

            {/* App header mockup */}
            <div style={{ height: 52, background: '#FFF8F6', borderBottom: '1px solid #FADCD3', display: 'flex', alignItems: 'center', padding: '0 16px', gap: 12, flexShrink: 0 }}>
              <span className="material-symbols-outlined" style={{ fontSize: 22, color: '#845143' }}>arrow_back</span>
              <span style={{ fontSize: 15, fontWeight: 600, color: '#271812', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>Chi tiet</span>
            </div>

            {/* Scrollable content */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '0' }}>
              {/* Hero image */}
              <div style={{ width: '100%', height: 200, background: 'linear-gradient(135deg, #FFE9E3 0%, #F6DACF 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <span className="material-symbols-outlined" style={{ fontSize: 40, color: '#C98C7B' }}>image</span>
              </div>

              {/* Article content */}
              <div style={{ padding: 20 }}>
                <h1 style={{ fontSize: 20, fontWeight: 700, color: '#271812', margin: '0 0 12px 0', lineHeight: 1.4 }}>{detail.title}</h1>
                <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                  <span style={{ padding: '3px 10px', borderRadius: 9999, background: '#FFE9E3', color: '#845143', fontSize: 11, fontWeight: 500 }}>Thai ky</span>
                  <span style={{ padding: '3px 10px', borderRadius: 9999, background: '#E6F4EA', color: '#137333', fontSize: 11, fontWeight: 500 }}>Dinh duong</span>
                </div>
                <div
                  style={{ fontSize: 14, lineHeight: 1.7, color: '#271812' }}
                  dangerouslySetInnerHTML={{ __html: detail.body }}
                />
              </div>
            </div>
          </div>
        ) : (
          /* Desktop frame */
          <div style={{
            width: '100%',
            maxWidth: 900,
            background: '#fff',
            borderRadius: 16,
            boxShadow: '0 24px 80px rgba(90,70,63,0.12)',
            overflow: 'hidden',
          }}>
            {/* Browser chrome mockup */}
            <div style={{ height: 36, background: '#F6F1EC', borderBottom: '1px solid #FADCD3', display: 'flex', alignItems: 'center', padding: '0 12px', gap: 8 }}>
              <div style={{ width: 12, height: 12, borderRadius: '50%', background: '#FF5F57' }} />
              <div style={{ width: 12, height: 12, borderRadius: '50%', background: '#FFBD2E' }} />
              <div style={{ width: 12, height: 12, borderRadius: '50%', background: '#28CA42' }} />
              <div style={{ flex: 1, margin: '0 12px', height: 20, borderRadius: 4, background: '#fff', border: '1px solid #d6c2bd', display: 'flex', alignItems: 'center', padding: '0 8px' }}>
                <span style={{ fontSize: 11, color: '#84736f' }}>carebridge.vn/content/{detail.id}</span>
              </div>
            </div>

            {/* Desktop content */}
            <div style={{ padding: 40 }}>
              {/* Hero image */}
              <div style={{ width: '100%', height: 300, borderRadius: 12, background: 'linear-gradient(135deg, #FFE9E3 0%, #F6DACF 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 32 }}>
                <span className="material-symbols-outlined" style={{ fontSize: 56, color: '#C98C7B' }}>image</span>
              </div>

              <h1 style={{ fontSize: 28, fontWeight: 700, color: '#271812', margin: '0 0 16px 0', lineHeight: 1.3 }}>{detail.title}</h1>
              <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                <span style={{ padding: '4px 14px', borderRadius: 9999, background: '#FFE9E3', color: '#845143', fontSize: 12, fontWeight: 500 }}>Thai ky</span>
                <span style={{ padding: '4px 14px', borderRadius: 9999, background: '#E6F4EA', color: '#137333', fontSize: 12, fontWeight: 500 }}>Dinh duong</span>
              </div>
              <div
                style={{ fontSize: 16, lineHeight: 1.8, color: '#271812' }}
                dangerouslySetInnerHTML={{ __html: detail.body }}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
