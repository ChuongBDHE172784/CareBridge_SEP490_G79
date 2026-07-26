import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { decideContent, decideChecklistTemplate, fetchStaffContentList, fetchAdminChecklistTemplates } from '../services/contentApi';
import type { ContentDetail, ChecklistTemplate } from '../models/content';
import { TYPE_LABELS, STAGE_LABELS } from '../models/content';

type QueueEntry =
  | { kind: 'CONTENT'; id: string; title: string; typeLabel: string; stageLabel: string; extra: string }
  | { kind: 'CHECKLIST'; id: string; title: string; typeLabel: string; stageLabel: string; extra: string };

function toContentEntry(item: ContentDetail): QueueEntry {
  return {
    kind: 'CONTENT',
    id: item.id,
    title: item.title,
    typeLabel: TYPE_LABELS[item.type],
    stageLabel: STAGE_LABELS[item.stage],
    extra: `Phiên bản ${item.version}`,
  };
}

function toChecklistEntry(item: ChecklistTemplate): QueueEntry {
  return {
    kind: 'CHECKLIST',
    id: item.id,
    title: item.name,
    typeLabel: 'Checklist',
    stageLabel: STAGE_LABELS[item.stage],
    extra: `${item.items.length} mục`,
  };
}

// System Admin lacks CONTENT_ADMIN, so it must use the read-only review routes rather than
// /content/:id or /content/checklists/:id (both gated to CONTENT_ADMIN — see app/router/index.tsx).
const DETAIL_PATH: Record<QueueEntry['kind'], (id: string) => string> = {
  CONTENT: (id) => `/admin/content-review/${id}`,
  CHECKLIST: (id) => `/admin/content-review/checklists/${id}`,
};

/** System Admin review queue. Drafts are deliberately excluded by the API filter. */
export default function ContentApprovalQueuePage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<QueueEntry[]>([]);
  const [error, setError] = useState('');
  const [working, setWorking] = useState<string | null>(null);

  const load = async () => {
    try {
      const [contentPage, checklistPage] = await Promise.all([
        fetchStaffContentList({ status: 'PENDING_REVIEW' }),
        fetchAdminChecklistTemplates({ status: 'PENDING_REVIEW', size: 50 }),
      ]);
      setItems([
        ...contentPage.content.map(toContentEntry),
        ...checklistPage.content.map(toChecklistEntry),
      ]);
      setError('');
    } catch {
      setError('Không tải được hàng đợi phê duyệt.');
    }
  };
  useEffect(() => { void load(); }, []);

  const decide = useCallback(async (entry: QueueEntry, decision: 'APPROVE' | 'REJECT') => {
    let reason: string | undefined;
    if (decision === 'REJECT') {
      const input = window.prompt(`Nhập lý do từ chối "${entry.title}":`);
      if (input === null) return;
      if (!input.trim()) {
        setError('Vui lòng nhập lý do trước khi từ chối.');
        return;
      }
      reason = input.trim();
    }
    setWorking(entry.id);
    setError('');
    try {
      if (entry.kind === 'CONTENT') {
        await decideContent(entry.id, decision, reason);
      } else {
        await decideChecklistTemplate(entry.id, decision, reason);
      }
      await load();
    } catch {
      setError('Không thể ghi nhận quyết định. Vui lòng thử lại.');
    } finally {
      setWorking(null);
    }
  }, []);

  return <div className="portal-page px-5 py-5 md:px-6 md:py-6">
    <div className="portal-contained">
      <div className="portal-header">
        <div>
          <p className="portal-eyebrow">Quản trị nội dung</p>
          <h1 className="portal-title">Hàng đợi phê duyệt</h1>
          <p className="portal-subtitle">
            Bao gồm bài viết, FAQ và checklist đang chờ duyệt. Chỉ System Admin có thể xuất bản hoặc trả về nháp.
          </p>
        </div>
      </div>
      {error && <div className="portal-error mb-4">{error}</div>}
      <div className="portal-card-padded">
        {items.length === 0 ? <p className="portal-empty">Không có nội dung chờ duyệt.</p> :
          <div className="flex flex-col gap-3">{items.map(entry => (
            <div key={`${entry.kind}-${entry.id}`} className="rounded-md border border-outline-variant bg-surface-container-lowest p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="font-semibold text-on-surface">{entry.title}</div>
                  <div className="mt-1 text-xs text-outline">{entry.typeLabel} · {entry.stageLabel} · {entry.extra}</div>
                </div>
                <button
                  onClick={() => navigate(DETAIL_PATH[entry.kind](entry.id))}
                  className="portal-secondary-button whitespace-nowrap"
                >
                  Xem chi tiết
                </button>
              </div>
              <div className="mt-3 flex gap-2">
                <button
                  disabled={working === entry.id}
                  onClick={() => void decide(entry, 'APPROVE')}
                  className="portal-primary-button disabled:opacity-50"
                >
                  Xuất bản
                </button>
                <button
                  disabled={working === entry.id}
                  onClick={() => void decide(entry, 'REJECT')}
                  className="portal-secondary-button disabled:opacity-50"
                >
                  Trả về nháp
                </button>
              </div>
            </div>
          ))}</div>}
      </div>
    </div>
  </div>;
}
