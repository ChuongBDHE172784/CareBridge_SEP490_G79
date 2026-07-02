import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchContentDetail } from '../services/contentApi';

// MOCK — ContentItem stores only a single current versionNo (int); there is no version-snapshot
// table or endpoint, so history/diff/compare cannot be backed by real data yet.
interface VersionRow {
  version: string;
  status: 'active' | 'archived';
  editorName: string;
  editedAt: string;
  summary: string;
}

const MOCK_VERSIONS: VersionRow[] = [
  { version: 'v2.1', status: 'active', editorName: 'Nguyễn Văn A', editedAt: '14:30 - 24/10/2023', summary: 'Cập nhật lại phần thực đơn tham khảo tuần 2, sửa lỗi chính tả.' },
  { version: 'v2.0', status: 'archived', editorName: 'Hoàng Ngọc', editedAt: '09:15 - 20/10/2023', summary: 'Thay đổi toàn bộ cấu trúc bài viết theo form chuẩn mới.' },
];

export default function ContentVersionHistoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');

  useEffect(() => {
    if (!id) return;
    fetchContentDetail(id).then((d) => setTitle(d.title)).catch(() => setTitle('(không tải được tiêu đề)'));
  }, [id]);

  return (
    <div className="p-8 font-sans">
      <div className="flex items-center gap-2 text-[13px] text-outline mb-2">
        <span className="cursor-pointer" onClick={() => navigate('/content/list')}>Thư viện</span>
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span className="cursor-pointer" onClick={() => navigate(`/content/${id}`)}>Bài viết: {title || id}</span>
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span className="text-on-surface-variant">Lịch sử phiên bản</span>
      </div>

      <div className="bg-error-container rounded-2xl p-4 mb-5 text-error text-sm flex items-center gap-2">
        <span className="material-symbols-outlined text-lg">info</span>
        Dữ liệu mẫu (MOCK) — backend chưa lưu trữ snapshot theo từng phiên bản, chỉ có một số phiên bản hiện tại
        (versionNo) trên mỗi nội dung.
      </div>

      <div className="flex items-center justify-between mb-5">
        <h1 className="text-2xl font-bold text-on-surface m-0">Lịch sử phiên bản</h1>
        <button
          disabled
          title="Backend chưa hỗ trợ so sánh phiên bản"
          className="flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface-container text-primary text-sm font-semibold opacity-50 cursor-not-allowed"
        >
          <span className="material-symbols-outlined text-lg">compare_arrows</span>
          So sánh các phiên bản
        </button>
      </div>

      <div className="bg-surface rounded-2xl shadow-md overflow-hidden">
        <table className="w-full border-collapse">
          <thead>
            <tr className="border-b-2 border-surface-container-highest text-left bg-surface-container-low">
              {['PHIÊN BẢN', 'TRẠNG THÁI', 'NGƯỜI CHỈNH SỬA & THỜI GIAN', 'TÓM TẮT THAY ĐỔI', 'THAO TÁC'].map((h) => (
                <th key={h} className="py-3 px-4 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {MOCK_VERSIONS.map((v) => (
              <tr key={v.version} className="border-b border-surface-container-highest">
                <td className="py-4 px-4 font-semibold text-sm text-on-surface">{v.version}</td>
                <td className="py-4 px-4">
                  <span className={`py-1 px-3 rounded-full text-xs font-semibold ${v.status === 'active' ? 'bg-primary-container text-on-primary-container' : 'bg-surface-container text-on-surface-variant'}`}>
                    {v.status === 'active' ? 'Đang hoạt động' : 'Đã lưu trữ'}
                  </span>
                </td>
                <td className="py-4 px-4">
                  <div className="text-sm font-semibold text-on-surface">{v.editorName}</div>
                  <div className="text-xs text-outline">{v.editedAt}</div>
                </td>
                <td className="py-4 px-4 text-sm text-on-surface-variant max-w-[320px]">{v.summary}</td>
                <td className="py-4 px-4">
                  <div className="flex gap-1">
                    {v.status === 'archived' && (
                      <button disabled title="Chưa hỗ trợ khôi phục phiên bản" className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent flex items-center justify-center opacity-40 cursor-not-allowed">
                        <span className="material-symbols-outlined text-primary text-base">history</span>
                      </button>
                    )}
                    <button disabled title="Chưa hỗ trợ xem chi tiết phiên bản cũ" className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent flex items-center justify-center opacity-40 cursor-not-allowed">
                      <span className="material-symbols-outlined text-primary text-base">visibility</span>
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
