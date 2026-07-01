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
    return <div className="py-12 text-center text-outline font-sans">Đang tải...</div>;
  }

  if (!detail) {
    return (
      <div className="py-12 text-center font-sans">
        <p className="text-error mb-4">Không tìm thấy nội dung.</p>
        <button onClick={() => navigate(-1)} className="py-2.5 px-6 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer">
          Quay lại
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen font-sans bg-[radial-gradient(ellipse_at_50%_30%,#ffe9e3_0%,#F6F1EC_70%)]">
      {/* Toolbar */}
      <div className="bg-surface border-b border-surface-container-highest py-3 px-8 flex items-center justify-between sticky top-0 z-10">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center gap-1.5 py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-[13px] font-semibold cursor-pointer"
          >
            <span className="material-symbols-outlined text-base">arrow_back</span>
            Back to Edit
          </button>

          <div className="flex items-center gap-2">
            <span className="py-1 px-3 rounded-full bg-[#FFE9E3] text-primary text-xs font-semibold uppercase">
              ĐANG XEM TRƯỚC
            </span>
            <span className="text-sm font-semibold text-on-surface max-w-[400px] overflow-hidden text-ellipsis whitespace-nowrap">
              {detail.title}
            </span>
          </div>
        </div>

        {/* Mobile / Desktop toggle */}
        <div className="flex rounded-full border border-outline-variant overflow-hidden">
          <button
            onClick={() => setViewMode('mobile')}
            className={`py-2 px-4 border-0 text-[13px] font-semibold cursor-pointer flex items-center gap-1 ${viewMode === 'mobile' ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant'}`}
          >
            <span className="material-symbols-outlined text-base">smartphone</span>
            Mobile
          </button>
          <button
            onClick={() => setViewMode('desktop')}
            className={`py-2 px-4 border-0 border-l border-outline-variant text-[13px] font-semibold cursor-pointer flex items-center gap-1 ${viewMode === 'desktop' ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant'}`}
          >
            <span className="material-symbols-outlined text-base">desktop_windows</span>
            Desktop
          </button>
        </div>
      </div>

      {/* Preview area */}
      <div className="flex justify-center items-start py-12 px-8 min-h-[calc(100vh-60px)]">
        {viewMode === 'mobile' ? (
          /* Mobile device frame */
          <div
            className="w-[390px] h-[844px] rounded-[40px] border-8 border-[#FADCD3] bg-surface overflow-hidden flex flex-col shadow-[0_24px_80px_rgba(90,70,63,0.18),0_8px_32px_rgba(90,70,63,0.08)]"
          >
            {/* Status bar mockup */}
            <div className="h-12 bg-surface-bright flex items-center justify-between px-6 shrink-0">
              <span className="text-[13px] font-semibold text-on-surface">9:41</span>
              <div className="flex gap-1.5 items-center">
                <span className="material-symbols-outlined text-on-surface-variant text-base">signal_cellular_alt</span>
                <span className="material-symbols-outlined text-on-surface-variant text-base">wifi</span>
                <span className="material-symbols-outlined text-on-surface-variant text-base">battery_full</span>
              </div>
            </div>

            {/* App header mockup */}
            <div className="h-[52px] bg-surface-bright border-b border-surface-container-highest flex items-center px-4 gap-3 shrink-0">
              <span className="material-symbols-outlined text-primary text-[22px]">arrow_back</span>
              <span className="text-[15px] font-semibold text-on-surface overflow-hidden text-ellipsis whitespace-nowrap">Chi tiết</span>
            </div>

            {/* Scrollable content */}
            <div className="flex-1 overflow-y-auto">
              {/* Hero image */}
              <div className="w-full h-[200px] flex items-center justify-center bg-[linear-gradient(135deg,#FFE9E3_0%,#F6DACF_100%)]">
                <span className="material-symbols-outlined text-[#C98C7B] text-[40px]">image</span>
              </div>

              {/* Article content */}
              <div className="p-5">
                <h1 className="text-xl font-bold text-on-surface mt-0 mb-3 leading-[1.4]">{detail.title}</h1>
                <div className="flex gap-2 mb-4">
                  <span className="py-[3px] px-2.5 rounded-full bg-[#FFE9E3] text-primary text-[11px] font-medium">Thai kỳ</span>
                  <span className="py-[3px] px-2.5 rounded-full bg-[#E6F4EA] text-[#137333] text-[11px] font-medium">Dinh dưỡng</span>
                </div>
                <div
                  className="text-sm leading-[1.7] text-on-surface"
                  dangerouslySetInnerHTML={{ __html: detail.body }}
                />
              </div>
            </div>
          </div>
        ) : (
          /* Desktop frame */
          <div
            className="w-full max-w-[900px] bg-surface rounded-2xl overflow-hidden shadow-[0_24px_80px_rgba(90,70,63,0.12)]"
          >
            {/* Browser chrome mockup */}
            <div className="h-9 bg-surface-dim border-b border-surface-container-highest flex items-center px-3 gap-2">
              <div className="w-3 h-3 rounded-full bg-[#FF5F57]" />
              <div className="w-3 h-3 rounded-full bg-[#FFBD2E]" />
              <div className="w-3 h-3 rounded-full bg-[#28CA42]" />
              <div className="flex-1 mx-3 h-5 rounded-[4px] bg-surface border border-outline-variant flex items-center px-2">
                <span className="text-[11px] text-outline">carebridge.vn/content/{detail.id}</span>
              </div>
            </div>

            {/* Desktop content */}
            <div className="p-10">
              {/* Hero image */}
              <div className="w-full h-[300px] rounded-xl flex items-center justify-center mb-8 bg-[linear-gradient(135deg,#FFE9E3_0%,#F6DACF_100%)]">
                <span className="material-symbols-outlined text-[#C98C7B] text-[56px]">image</span>
              </div>

              <h1 className="text-[28px] font-bold text-on-surface mt-0 mb-4 leading-[1.3]">{detail.title}</h1>
              <div className="flex gap-2 mb-6">
                <span className="py-1 px-3.5 rounded-full bg-[#FFE9E3] text-primary text-xs font-medium">Thai kỳ</span>
                <span className="py-1 px-3.5 rounded-full bg-[#E6F4EA] text-[#137333] text-xs font-medium">Dinh dưỡng</span>
              </div>
              <div
                className="text-base leading-[1.8] text-on-surface"
                dangerouslySetInnerHTML={{ __html: detail.body }}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
