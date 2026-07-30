import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchVisibleCommunityContent } from '../services/moderationApi';
import type { CommunityContentMonitorItem, ReportTargetType } from '../models/moderation';

type MonitorType = Extract<ReportTargetType, 'QUESTION' | 'ANSWER'>;
const PAGE_SIZE = 20;

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' });
}

export default function CommunityContentMonitorPage() {
  const navigate = useNavigate();
  const [targetType, setTargetType] = useState<MonitorType>('QUESTION');
  const [items, setItems] = useState<CommunityContentMonitorItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const result = await fetchVisibleCommunityContent({ targetType, page, size: PAGE_SIZE });
      setItems(result.content);
      setTotalElements(result.totalElements);
    } catch {
      setItems([]);
      setTotalElements(0);
      setError('Không tải được nội dung cộng đồng đang hiển thị. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [page, targetType]);

  useEffect(() => { void load(); }, [load]);
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));

  const switchType = (type: MonitorType) => {
    setTargetType(type);
    setPage(0);
  };

  return (
    <div className="portal-page font-sans">
      <main className="p-5 md:p-8">
        <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="m-0 text-[26px] font-bold text-on-surface">Theo dõi nội dung cộng đồng</h1>
            <p className="mt-1 text-sm text-on-surface-variant">Danh sách chỉ gồm bài viết đang được hiển thị công khai cho cộng đồng.</p>
          </div>
          <button type="button" onClick={() => void load()} disabled={loading} className="inline-flex w-fit items-center gap-2 rounded-full border border-outline-variant bg-surface px-5 py-2.5 text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low disabled:opacity-50">
            <span className="material-symbols-outlined text-lg">refresh</span>Làm mới
          </button>
        </div>

        <div className="mb-5 flex flex-wrap gap-2 border-b border-surface-container-highest pb-4">
          {([
            ['QUESTION', 'Câu hỏi đang hiển thị', 'forum'],
            ['ANSWER', 'Câu trả lời đang hiển thị', 'quickreply'],
          ] as const).map(([type, label, icon]) => (
            <button key={type} type="button" onClick={() => switchType(type)} className={`inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold ${targetType === type ? 'bg-primary text-on-primary' : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container-highest'}`}>
              <span className="material-symbols-outlined text-base">{icon}</span>{label}
            </button>
          ))}
        </div>

        <section className="overflow-hidden rounded-2xl border border-surface-container-highest bg-surface shadow-sm">
          {loading ? <div className="p-12 text-center text-on-surface-variant">Đang tải nội dung cộng đồng...</div>
            : error ? <div className="p-8 text-center text-error">{error}</div>
              : <>
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[760px] border-collapse text-left">
                    <thead><tr className="border-b-2 border-surface-container-highest text-[11px] font-semibold tracking-[.05em] text-outline">
                      <th className="px-5 py-4">NỘI DUNG</th><th className="px-4 py-4">NGƯỜI ĐĂNG</th><th className="px-4 py-4">THỜI GIAN</th><th className="px-4 py-4">ẢNH</th><th className="px-5 py-4 text-right">THAO TÁC</th>
                    </tr></thead>
                    <tbody>{items.map((item) => <tr key={item.targetId} className="border-b border-surface-container-highest last:border-0 hover:bg-surface-bright">
                      <td className="max-w-[520px] px-5 py-4"><p className="m-0 truncate text-sm font-bold text-on-surface">{item.title ?? 'Câu trả lời cộng đồng'}</p><p className="mb-0 mt-1 line-clamp-2 text-sm text-on-surface-variant">{item.contentPreview}</p></td>
                      <td className="whitespace-nowrap px-4 py-4 text-sm text-on-surface">{item.authorName ?? 'Không xác định'}</td>
                      <td className="whitespace-nowrap px-4 py-4 text-sm text-on-surface-variant">{formatDateTime(item.createdAt)}</td>
                      <td className="px-4 py-4 text-sm text-on-surface-variant">{item.imageCount > 0 ? `${item.imageCount} ảnh` : '—'}</td>
                      <td className="px-5 py-4 text-right"><button type="button" onClick={() => navigate(`/moderator/pending-content/${item.targetType}/${item.targetId}`, { state: { returnTo: '/moderator/community-content' } })} className="inline-flex items-center gap-1 rounded-lg border border-outline-variant px-3 py-1.5 text-xs font-bold text-primary hover:bg-surface-container-low"><span className="material-symbols-outlined text-base">visibility</span>Xem đầy đủ</button></td>
                    </tr>)}</tbody>
                  </table>
                </div>
                {items.length === 0 && <div className="p-12 text-center text-on-surface-variant">Chưa có {targetType === 'QUESTION' ? 'câu hỏi' : 'câu trả lời'} nào đang hiển thị.</div>}
                <footer className="flex items-center justify-between border-t border-surface-container-highest px-5 py-4 text-sm text-on-surface-variant">
                  <span>Trang {page + 1}/{totalPages} · {totalElements} nội dung</span>
                  <div className="flex gap-2"><button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="rounded-lg border border-outline-variant px-3 py-1.5 disabled:opacity-40">Trước</button><button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)} className="rounded-lg border border-outline-variant px-3 py-1.5 disabled:opacity-40">Sau</button></div>
                </footer>
              </>}
        </section>
      </main>
    </div>
  );
}
