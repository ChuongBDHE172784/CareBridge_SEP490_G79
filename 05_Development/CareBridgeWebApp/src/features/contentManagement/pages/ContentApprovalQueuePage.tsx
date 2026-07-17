import { useEffect, useState } from 'react';
import { decideContent, fetchStaffContentList } from '../services/contentApi';
import type { ContentDetail } from '../models/content';

/** System Admin review queue.  Drafts are deliberately excluded by the API filter. */
export default function ContentApprovalQueuePage() {
  const [items, setItems] = useState<ContentDetail[]>([]);
  const [error, setError] = useState('');
  const [working, setWorking] = useState<string | null>(null);
  const load = async () => {
    try { setItems((await fetchStaffContentList({ status: 'PENDING_REVIEW' })).content); setError(''); }
    catch { setError('Không tải được hàng đợi phê duyệt.'); }
  };
  useEffect(() => { void load(); }, []);
  const decide = async (id: string, decision: 'APPROVE' | 'REJECT') => {
    setWorking(id);
    try { await decideContent(id, decision, decision === 'REJECT' ? 'Cần chỉnh sửa trước khi xuất bản' : undefined); await load(); }
    catch { setError('Không thể ghi nhận quyết định.'); }
    finally { setWorking(null); }
  };
  return <div className="p-8 font-sans">
    <h1 className="m-0 text-2xl font-bold text-on-surface">Hàng đợi phê duyệt</h1>
    <p className="mt-1 mb-6 text-sm text-outline">Chỉ System Admin có thể xuất bản hoặc trả về nháp.</p>
    {error && <div className="mb-4 rounded-xl bg-error-container p-3 text-sm text-error">{error}</div>}
    <div className="rounded-2xl bg-surface p-6 shadow-md">
      {items.length === 0 ? <p className="py-8 text-center text-outline">Không có nội dung chờ duyệt.</p> :
        <div className="flex flex-col gap-3">{items.map(item => <div key={item.id} className="rounded-xl border border-outline-variant p-4">
          <div className="font-semibold text-on-surface">{item.title}</div>
          <div className="mt-1 text-xs text-outline">{item.type} · {item.stage} · phiên bản {item.version}</div>
          <div className="mt-3 flex gap-2"><button disabled={working === item.id} onClick={() => void decide(item.id, 'APPROVE')} className="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-on-primary disabled:opacity-50">Xuất bản</button>
          <button disabled={working === item.id} onClick={() => void decide(item.id, 'REJECT')} className="rounded-full border border-outline-variant px-4 py-2 text-sm font-semibold disabled:opacity-50">Trả về nháp</button></div>
        </div>)}</div>}
    </div>
  </div>;
}
