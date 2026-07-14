import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import { ACTION_TYPE_LABELS, type AccountViolationHistoryItem } from '../models/moderation';
import { fetchAccountViolationHistory } from '../services/moderationApi';

const PAGE_SIZE = 20;

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

function enforcementStatus(item: AccountViolationHistoryItem): string {
  if (!item.expiresAt) return item.actionType === 'ESCALATE' ? 'Đã chuyển cấp' : 'Không thời hạn';
  return new Date(item.expiresAt).getTime() > Date.now()
    ? `Còn hiệu lực đến ${formatDateTime(item.expiresAt)}`
    : 'Đã hết hiệu lực';
}

export default function ViolationHistoryPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<AccountViolationHistoryItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const latestRequest = useRef(0);

  const load = useCallback(async () => {
    const requestId = ++latestRequest.current;
    setLoading(true);
    setError('');
    try {
      const result = await fetchAccountViolationHistory({ page, size: PAGE_SIZE });
      if (requestId !== latestRequest.current) return;
      setItems(result.content);
      setTotalElements(result.totalElements);
    } catch {
      if (requestId !== latestRequest.current) return;
      setItems([]);
      setError('Không tải được lịch sử vi phạm. Vui lòng thử lại.');
    } finally {
      if (requestId === latestRequest.current) setLoading(false);
    }
  }, [page]);

  useEffect(() => { void load(); }, [load]);
  const hasNext = (page + 1) * PAGE_SIZE < totalElements;

  return (
    <div className="min-h-screen bg-background">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <button onClick={() => navigate(-1)} className="mb-4 inline-flex cursor-pointer items-center gap-1.5 text-sm font-semibold text-on-surface">
          <span className="material-symbols-outlined text-lg">arrow_back</span>Quay lại
        </button>
        <div className="mb-5">
          <h1 className="m-0 text-2xl font-bold text-on-surface">Lịch sử vi phạm</h1>
          <p className="mt-1 text-sm text-outline">Ghi nhận các hành động kỷ luật đối với tài khoản.</p>
        </div>

        {loading ? (
          <div className="rounded-2xl bg-surface p-10 text-center text-outline shadow-md">Đang tải lịch sử vi phạm...</div>
        ) : error ? (
          <div className="rounded-2xl bg-error-container p-8 text-center shadow-md">
            <p className="text-sm text-error">{error}</p>
            <button type="button" onClick={() => void load()} className="mt-3 rounded-xl bg-error px-4 py-2 text-sm font-semibold text-on-error">Thử lại</button>
          </div>
        ) : items.length === 0 ? (
          <div className="rounded-2xl bg-surface p-10 text-center shadow-md">
            <span className="material-symbols-outlined text-5xl text-outline">gavel</span>
            <h2 className="mt-4 text-xl font-bold text-on-surface">Chưa có dữ liệu lịch sử vi phạm</h2>
            <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">Chưa ghi nhận hành động kỷ luật nào đối với tài khoản.</p>
          </div>
        ) : (
          <div className="overflow-hidden rounded-2xl bg-surface shadow-md">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1000px] border-collapse">
                <thead><tr className="border-b-2 border-surface-container-highest bg-surface-container-low text-left">
                  {['TÀI KHOẢN', 'HÀNH ĐỘNG', 'LÝ DO', 'TRẠNG THÁI', 'NGƯỜI XỬ LÝ', 'THỜI GIAN'].map((heading) => <th key={heading} className="px-4 py-3 text-[11px] font-semibold tracking-[.05em] text-outline">{heading}</th>)}
                </tr></thead>
                <tbody>{items.map((item) => <tr key={item.actionId} className="border-b border-surface-container-highest last:border-0 hover:bg-surface-container-low">
                  <td className="px-4 py-3.5 text-sm font-medium text-on-surface">{item.targetUserName}</td>
                  <td className="px-4 py-3.5"><span className="rounded-full bg-error-container px-3 py-1 text-xs font-semibold text-error">{ACTION_TYPE_LABELS[item.actionType]}</span></td>
                  <td className="max-w-[260px] px-4 py-3.5 text-sm text-on-surface-variant">{item.reason}</td>
                  <td className={`px-4 py-3.5 text-sm ${enforcementStatus(item) === 'Đã hết hiệu lực' ? 'text-outline' : 'text-on-surface-variant'}`}>{enforcementStatus(item)}</td>
                  <td className="px-4 py-3.5 text-sm text-on-surface-variant">{item.moderatorName}</td>
                  <td className="whitespace-nowrap px-4 py-3.5 text-sm text-on-surface-variant">{formatDateTime(item.actionAt)}</td>
                </tr>)}</tbody>
              </table>
            </div>
            <div className="flex items-center justify-between border-t border-surface-container-highest px-4 py-3 text-sm text-on-surface-variant">
              <span>{totalElements} bản ghi</span>
              <div className="flex gap-2"><button type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="rounded-lg px-3 py-1.5 disabled:opacity-40">Trước</button><button type="button" disabled={!hasNext} onClick={() => setPage((current) => current + 1)} className="rounded-lg px-3 py-1.5 disabled:opacity-40">Sau</button></div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
