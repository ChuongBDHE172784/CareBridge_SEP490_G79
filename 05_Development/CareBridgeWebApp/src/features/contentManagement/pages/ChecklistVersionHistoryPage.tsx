import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchChecklistTemplateDetail } from '../services/contentApi';
import type { AdminChecklistTemplateDetail } from '../models/content';

export default function ChecklistVersionHistoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [checklist, setChecklist] = useState<AdminChecklistTemplateDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) {
      setError('Không xác định được checklist.');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError('');
    fetchChecklistTemplateDetail(id)
      .then(setChecklist)
      .catch(() => setError('Không thể tải thông tin phiên bản checklist.'))
      .finally(() => setIsLoading(false));
  }, [id]);

  if (isLoading) {
    return <div className="p-8 py-16 text-center font-sans text-outline">Đang tải...</div>;
  }

  return (
    <div className="p-8 font-sans">
      <div className="mb-2 flex items-center gap-2 text-[13px] text-outline">
        <span className="cursor-pointer" onClick={() => navigate('/content/checklists')}>Checklist</span>
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span
          className="cursor-pointer"
          onClick={() => id && navigate(`/content/checklists/${id}/edit`)}
        >
          {checklist?.name ?? id ?? 'Không xác định'}
        </span>
        <span className="material-symbols-outlined text-base">chevron_right</span>
        <span className="text-on-surface-variant">Lịch sử phiên bản</span>
      </div>

      <div className="mb-5 flex items-center justify-between">
        <h1 className="m-0 text-2xl font-bold text-on-surface">Lịch sử phiên bản Checklist</h1>
        <button
          disabled
          className="flex items-center gap-2 rounded-full bg-surface-container px-5 py-2.5 text-sm font-semibold text-primary opacity-50"
        >
          <span className="material-symbols-outlined text-lg">compare_arrows</span>
          So sánh các phiên bản
        </button>
      </div>

      {error ? (
        <div role="alert" className="rounded-2xl bg-error-container p-6 text-sm text-error shadow-md">
          {error}
        </div>
      ) : (
        <div className="rounded-2xl bg-surface p-10 text-center shadow-md">
          <span className="material-symbols-outlined text-5xl text-outline">history</span>
          <h2 className="mt-4 text-xl font-bold text-on-surface">
            Phiên bản hiện tại: v{checklist?.versionNo ?? '—'}
          </h2>
          <p className="mt-2 text-sm font-semibold text-on-surface">
            {checklist?.name} · {checklist?.items.length ?? 0} mục
          </p>
          <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">
            Hệ thống dùng bộ đếm phiên bản trên checklist hiện hành và chưa lưu snapshot lịch sử.
          </p>
        </div>
      )}
    </div>
  );
}
