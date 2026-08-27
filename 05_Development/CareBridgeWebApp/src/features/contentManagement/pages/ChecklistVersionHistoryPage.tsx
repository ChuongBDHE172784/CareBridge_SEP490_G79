import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchChecklistTemplateDetail, fetchChecklistVersionHistory } from '../services/contentApi';
import type { ChecklistTemplateVersionSnapshot } from '../models/content';

import { STAGE_LABELS } from '../models/content';

const formatDate = (value: string) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
const statusLabel: Record<string, string> = { DRAFT: 'Bản nháp', PENDING_REVIEW: 'Chờ duyệt', APPROVED: 'Đã duyệt', PUBLISHED: 'Đã xuất bản', ARCHIVED: 'Đã lưu trữ' };

export default function ChecklistVersionHistoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [versions, setVersions] = useState<ChecklistTemplateVersionSnapshot[]>([]);
  const [error, setError] = useState('');

  useEffect(() => { if (!id) return; Promise.all([fetchChecklistTemplateDetail(id), fetchChecklistVersionHistory(id)])
    .then(([detail, history]) => { setName(detail.name); setVersions(history); })
    .catch(() => setError('Không thể tải lịch sử phiên bản checklist. Vui lòng thử lại.')); }, [id]);

  return <div className="p-8 font-sans">
    <div className="mb-2 flex items-center gap-2 text-[13px] text-outline"><span className="cursor-pointer" onClick={() => navigate('/content/checklists')}>Checklist</span><span className="material-symbols-outlined text-base">chevron_right</span><span className="cursor-pointer" onClick={() => id && navigate(`/content/checklists/${id}/edit`)}>{name || id}</span><span className="material-symbols-outlined text-base">chevron_right</span><span className="text-on-surface-variant">Lịch sử phiên bản</span></div>
    <div className="mb-6"><h1 className="m-0 text-2xl font-bold text-on-surface">Lịch sử phiên bản Checklist</h1><p className="mt-1 text-sm text-on-surface-variant">Mỗi lần lưu checklist sẽ tạo một bản lưu độc lập.</p></div>
    {versions.length > 0 && versions.at(-1)?.versionNo !== 1 && <div className="mb-4 rounded-xl bg-surface-container p-4 text-sm text-on-surface-variant">Các bản trước v{versions.at(-1)?.versionNo} được tạo trước khi hệ thống lưu snapshot nên không thể khôi phục. Những lần lưu mới sẽ có lịch sử đầy đủ.</div>}
    {error ? <div role="alert" className="rounded-2xl bg-error-container p-6 text-sm text-error">{error}</div> : versions.length === 0 ? <div className="rounded-2xl bg-surface p-10 text-center text-sm text-on-surface-variant shadow-md">Chưa có bản lưu nào cho checklist này.</div> : <ol className="m-0 list-none space-y-3 p-0">{versions.map((version, index) => <li key={version.versionNo} className="rounded-2xl bg-surface p-5 shadow-md"><div className="flex items-start justify-between gap-4"><div><div className="flex items-center gap-2"><h2 className="m-0 text-lg font-bold text-on-surface">v{version.versionNo}</h2>{index === 0 && <span className="rounded-full bg-primary-container px-2.5 py-1 text-xs font-semibold text-on-primary-container">Hiện hành</span>}</div><p className="mt-2 font-medium text-on-surface">{version.name}</p><p className="mt-1 text-sm text-on-surface-variant">{version.itemCount} mục · {(version.stage && STAGE_LABELS[version.stage as keyof typeof STAGE_LABELS]) ?? version.stage ?? 'Chưa xác định giai đoạn'} · {statusLabel[version.status] ?? version.status}</p></div><time className="shrink-0 text-right text-xs text-on-surface-variant">{formatDate(version.createdAt)}</time></div></li>)}</ol>}
  </div>;
}
