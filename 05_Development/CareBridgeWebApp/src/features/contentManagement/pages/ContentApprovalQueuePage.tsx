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
  return <div className="portal-page px-5 py-5 md:px-6 md:py-6">
    <div className="portal-contained">
      <div className="portal-header">
        <div>
          <p className="portal-eyebrow">Quản trị nội dung</p>
          <h1 className="portal-title">Hàng đợi phê duyệt</h1>
          <p className="portal-subtitle">Chỉ System Admin có thể xuất bản hoặc trả về nháp.</p>
        </div>
      </div>
      {error && <div className="portal-error mb-4">{error}</div>}
      <div className="portal-card-padded">
        {items.length === 0 ? <p className="portal-empty">Không có nội dung chờ duyệt.</p> :
          <div className="flex flex-col gap-3">{items.map(item => <div key={item.id} className="rounded-md border border-outline-variant bg-surface-container-lowest p-4">
          <div className="font-semibold text-on-surface">{item.title}</div>
          <div className="mt-1 text-xs text-outline">{item.type} · {item.stage} · phiên bản {item.version}</div>
          <div className="mt-3 flex gap-2"><button disabled={working === item.id} onClick={() => void decide(item.id, 'APPROVE')} className="portal-primary-button disabled:opacity-50">Xuất bản</button>
          <button disabled={working === item.id} onClick={() => void decide(item.id, 'REJECT')} className="portal-secondary-button disabled:opacity-50">Trả về nháp</button></div>
        </div>)}</div>}
      </div>
    </div>
  </div>;
}
