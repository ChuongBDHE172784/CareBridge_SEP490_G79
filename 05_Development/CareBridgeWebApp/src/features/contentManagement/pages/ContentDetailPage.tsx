import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchContentDetail } from '../services/contentApi';
import type { ContentDetail } from '../models/content';
import { STAGE_LABELS, STATUS_LABELS } from '../models/content';

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
function statusDotClass(status: string): string {
  if (status === 'APPROVED') return 'bg-[#137333]';
  if (status === 'DRAFT') return 'bg-[#616161]';
  return 'bg-[#BA1A1A]';
}

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

/* ------------------------------------------------------------------ */
/*  Version timeline mock                                              */
/* ------------------------------------------------------------------ */
const MOCK_VERSIONS = [
  { version: 3, date: '25/06/2026', author: 'Content Admin', note: 'Cập nhật nội dung dinh dưỡng' },
  { version: 2, date: '15/06/2026', author: 'Content Admin', note: 'Bổ sung phần thực phẩm nên tránh' },
  { version: 1, date: '01/06/2026', author: 'Content Admin', note: 'Tạo bài viết mới' },
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
    return <div className="py-12 text-center text-outline font-sans">Đang tải...</div>;
  }

  if (error || !detail) {
    return (
      <div className="py-12 text-center font-sans">
        <p className="text-error mb-4">{error || 'Không tìm thấy nội dung.'}</p>
        <button onClick={() => navigate(-1)} className="py-2.5 px-6 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer">
          Quay lại
        </button>
      </div>
    );
  }

  return (
    <div className="p-8 font-sans">
      {/* Breadcrumbs */}
      <div className="flex items-center gap-2 text-[13px] text-outline mb-4">
        <span className="cursor-pointer" onClick={() => navigate('/content')}>Thư viện</span>
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span className="cursor-pointer" onClick={() => navigate('/content/list')}>{STAGE_LABELS[detail.stage]}</span>
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span className="text-on-surface-variant">Chi tiết bài viết</span>
      </div>

      {/* Back button */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-sm font-semibold cursor-pointer mb-6"
      >
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Quay lại
      </button>

      <div className="grid grid-cols-[1fr_340px] gap-6">
        {/* Main content area */}
        <div>
          {/* Title */}
          <h1 className="text-[28px] font-bold text-on-surface mt-0 mb-5 leading-[1.3]">{detail.title}</h1>

          {/* Metadata card */}
          <div className="bg-surface rounded-2xl p-5 shadow-md mb-6 flex gap-8 flex-wrap">
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">TÁC GIẢ</div>
              <div className="text-sm text-on-surface font-medium">Content Admin</div>
            </div>
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">NGÀY TẠO</div>
              <div className="text-sm text-on-surface font-medium">{formatDate(detail.createdAt)}</div>
            </div>
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">CHUYÊN MỤC</div>
              <div className="text-sm text-on-surface font-medium">{STAGE_LABELS[detail.stage]}</div>
            </div>
            <div>
              <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">ĐỐI TƯỢNG MỤC TIÊU</div>
              <div className="flex gap-1.5 mt-0.5">
                <span className="py-[3px] px-2.5 rounded-full bg-[#FFE9E3] text-primary text-xs font-medium">Mẹ bầu</span>
                <span className="py-[3px] px-2.5 rounded-full bg-[#FFE9E3] text-primary text-xs font-medium">Gia đình</span>
              </div>
            </div>
          </div>

          {/* Article canvas */}
          <div className="bg-surface rounded-2xl p-8 shadow-md">
            {/* Hero image placeholder */}
            <div
              className="w-full h-[220px] rounded-xl flex items-center justify-center mb-6 bg-[linear-gradient(135deg,#FFE9E3_0%,#F6DACF_100%)]"
            >
              <span className="material-symbols-outlined text-[#C98C7B] text-5xl">image</span>
            </div>

            {/* Body content */}
            <div
              className="text-[15px] leading-7 text-on-surface"
              dangerouslySetInnerHTML={{ __html: detail.body }}
            />
          </div>
        </div>

        {/* Right sidebar */}
        <div className="flex flex-col gap-4">
          {/* Status widget */}
          <div className="bg-surface rounded-2xl p-5 shadow-md">
            <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">TRẠNG THÁI</div>
            <div className="flex items-center gap-2 mb-2">
              <span className={`w-2.5 h-2.5 rounded-full ${statusDotClass(detail.status)}`} />
              <span className="text-sm font-semibold text-on-surface">{STATUS_LABELS[detail.status]}</span>
            </div>
            <div className="text-xs text-outline mb-1">Phiên bản: v{detail.version}</div>
            <div className="text-xs text-outline">Xuất bản: {formatDate(detail.publishedAt)}</div>
          </div>

          {/* Action buttons */}
          <button
            onClick={() => navigate(`/content/${detail.id}/edit`)}
            className="w-full py-3.5 rounded-2xl bg-primary-container text-on-primary border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-lg">edit</span>
            Chỉnh sửa nội dung
          </button>
          <button
            onClick={() => navigate(`/content/${detail.id}/preview`)}
            className="w-full py-3.5 rounded-2xl bg-transparent text-primary border border-outline-variant text-sm font-semibold cursor-pointer flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-lg">send</span>
            Gửi phê duyệt
          </button>

          {/* Version history */}
          <div className="bg-surface rounded-2xl p-5 shadow-md">
            <div className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-4">LỊCH SỬ PHIÊN BẢN</div>
            <div className="flex flex-col">
              {MOCK_VERSIONS.map((v, idx) => (
                <div key={v.version} className={`flex gap-3 ${idx < MOCK_VERSIONS.length - 1 ? 'pb-4' : ''}`}>
                  {/* Timeline dot and line */}
                  <div className="flex flex-col items-center min-w-4">
                    <div className={`w-2.5 h-2.5 rounded-full shrink-0 ${idx === 0 ? 'bg-[#C98C7B]' : 'bg-outline-variant'}`} />
                    {idx < MOCK_VERSIONS.length - 1 && (
                      <div className="w-0.5 flex-1 bg-surface-container-highest mt-1" />
                    )}
                  </div>
                  <div>
                    <div className="text-[13px] font-semibold text-on-surface">v{v.version} - {v.date}</div>
                    <div className="text-xs text-outline mt-0.5">{v.author}</div>
                    <div className="text-xs text-on-surface-variant mt-0.5">{v.note}</div>
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
