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
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">Kiểm duyệt</p>
            <h1 className="portal-title">Lịch sử vi phạm</h1>
            <p className="portal-subtitle">Ghi nhận các hành động kỷ luật đối với tài khoản.</p>
          </div>
          <button onClick={() => navigate(-1)} className="portal-secondary-button">
            <span className="material-symbols-outlined text-lg">arrow_back</span>Quay lại
          </button>
        </div>

        {loading ? (
          <div className="portal-empty">Đang tải lịch sử vi phạm...</div>
        ) : error ? (
          <div className="portal-error text-center">
            <p className="text-sm text-error">{error}</p>
            <button type="button" onClick={() => void load()} className="mt-3 rounded-md bg-error px-4 py-2 text-sm font-semibold text-on-error">Thử lại</button>
          </div>
        ) : items.length === 0 ? (
          <div className="portal-card-padded text-center">
            <span className="material-symbols-outlined text-5xl text-outline">gavel</span>
            <h2 className="mt-4 text-base font-semibold text-on-surface">Chưa có dữ liệu lịch sử vi phạm</h2>
            <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">Chưa ghi nhận hành động kỷ luật nào đối với tài khoản.</p>
          </div>
        ) : (
          <div className="portal-table-card">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1000px]">
                <thead><tr>
                  {['TÀI KHOẢN', 'HÀNH ĐỘNG', 'LÝ DO', 'TRẠNG THÁI', 'NGƯỜI XỬ LÝ', 'THỜI GIAN'].map((heading) => <th key={heading}>{heading}</th>)}
                </tr></thead>
                <tbody>{items.map((item) => <tr key={item.actionId}>
                  <td className="font-medium text-on-surface">{item.targetUserName}</td>
                  <td><span className="rounded-md bg-error-container px-2.5 py-1 text-xs font-semibold text-error">{ACTION_TYPE_LABELS[item.actionType]}</span></td>
                  <td className="max-w-[260px] text-on-surface-variant">{item.reason}</td>
                  <td className={enforcementStatus(item) === 'Đã hết hiệu lực' ? 'text-outline' : 'text-on-surface-variant'}>{enforcementStatus(item)}</td>
                  <td className="text-on-surface-variant">{item.moderatorName}</td>
                  <td className="whitespace-nowrap text-on-surface-variant">{formatDateTime(item.actionAt)}</td>
                </tr>)}</tbody>
              </table>
            </div>
            <div className="flex items-center justify-between border-t border-surface-container-highest px-4 py-3 text-sm text-on-surface-variant">
              <span>{totalElements} bản ghi</span>
              <div className="flex gap-2"><button type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="rounded-md px-3 py-1.5 disabled:opacity-40">Trước</button><button type="button" disabled={!hasNext} onClick={() => setPage((current) => current + 1)} className="rounded-md px-3 py-1.5 disabled:opacity-40">Sau</button></div>
            </div>
          </div>
        )}
        </div>
      </main>
    </div>
  );
}
