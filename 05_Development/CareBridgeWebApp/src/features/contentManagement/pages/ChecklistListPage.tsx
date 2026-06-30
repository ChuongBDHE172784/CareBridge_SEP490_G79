import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchChecklists } from '../services/contentApi';
import type { ChecklistTemplate, ContentStage } from '../models/content';
import { STAGE_LABELS } from '../models/content';


/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function stageBadgeClass(stage: ContentStage): string {
  switch (stage) {
    case 'PRE_PREGNANCY': return 'bg-[#F5F5F5] text-[#616161]';
    case 'PREGNANCY': return 'bg-surface-container-low text-primary';
    case 'POSTPARTUM': return 'bg-[#E6F4EA] text-[#137333]';
    case 'BABY_CARE': return 'bg-[#FFE9E3] text-[#C98C7B]';
    default: return 'bg-[#F5F5F5] text-[#616161]';
  }
}

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function ChecklistListPage() {
  const navigate = useNavigate();
  const [checklists, setChecklists] = useState<ChecklistTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [stageFilter, setStageFilter] = useState<ContentStage | ''>('');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await fetchChecklists(stageFilter || undefined);
      setChecklists(data);
    } catch {
      setChecklists([]);
    } finally {
      setIsLoading(false);
    }
  }, [stageFilter]);

  useEffect(() => { loadData(); }, [loadData]);

  const total = checklists.length;
  const totalPages = Math.ceil(total / pageSize);
  const pagedItems = checklists.slice(page * pageSize, (page + 1) * pageSize);

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Quản lý Checklist</h1>
          <p className="text-on-surface-variant text-sm mt-1">Tạo và quản lý các checklist hướng dẫn cho mẹ và gia đình</p>
        </div>
        <div className="flex gap-2.5">
          <select
            value={stageFilter}
            onChange={e => { setStageFilter(e.target.value as ContentStage | ''); setPage(0); }}
            className="py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
          >
            <option value="">Lọc</option>
            <option value="PRE_PREGNANCY">Chuẩn bị</option>
            <option value="PREGNANCY">Thai kỳ</option>
            <option value="POSTPARTUM">Sau sinh</option>
            <option value="BABY_CARE">Chăm bé</option>
          </select>
          <button
            onClick={() => navigate('/content/create?type=CHECKLIST')}
            className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary-container text-on-primary border-0 text-sm font-semibold cursor-pointer"
          >
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
            Tạo Checklist
          </button>
        </div>
      </div>

      {/* Data table */}
      <div className="bg-surface rounded-2xl p-6 shadow-md">
        {isLoading ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : (
          <>
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left">
                  {['TIÊU ĐỀ', 'GIAI ĐOẠN', 'SỐ MỤC', 'TRẠNG THÁI', 'CẬP NHẬT LẦN CUỐI', 'THAO TÁC'].map(h => (
                    <th key={h} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {pagedItems.map(cl => {
                  // Derive published status from whether items exist
                  const isPublished = cl.items.length > 0;
                  return (
                    <tr
                      key={cl.id}
                      className="border-b border-surface-container-highest hover:bg-surface-bright"
                    >
                      <td className="py-3.5 px-2 max-w-[320px]">
                        <div className="font-semibold text-sm text-on-surface">{cl.name}</div>
                        <div className="text-xs text-outline mt-0.5">{cl.description}</div>
                      </td>
                      <td className="py-3.5 px-2">
                        <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${stageBadgeClass(cl.stage)}`}>
                          {STAGE_LABELS[cl.stage]}
                        </span>
                      </td>
                      <td className="py-3.5 px-2">
                        <div className="flex items-center gap-1">
                          <span className="material-symbols-outlined text-primary" style={{ fontSize: 16 }}>checklist</span>
                          <span className="text-sm font-semibold text-on-surface">{cl.items.length}</span>
                          <span className="text-xs text-outline">mục</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-2">
                        {isPublished ? (
                          <span className="py-1 px-3.5 rounded-full bg-[#E6F4EA] text-[#137333] text-xs font-semibold">Published</span>
                        ) : (
                          <span className="py-1 px-3.5 rounded-full bg-[#F5F5F5] text-[#616161] text-xs font-semibold">Draft</span>
                        )}
                      </td>
                      <td className="py-3.5 px-2 text-[13px] text-outline">—</td>
                      <td className="py-3.5 px-2">
                        <div className="flex gap-1">
                          <button
                            onClick={() => navigate(`/content/${cl.id}`)}
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center"
                            title="Xem chi tiết"
                          >
                            <span className="material-symbols-outlined text-primary" style={{ fontSize: 16 }}>visibility</span>
                          </button>
                          <button
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center"
                            title="Chỉnh sửa"
                          >
                            <span className="material-symbols-outlined text-primary" style={{ fontSize: 16 }}>edit</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {pagedItems.length === 0 && (
                  <tr><td colSpan={6} className="py-12 text-center text-outline">Không có checklist nào.</td></tr>
                )}
              </tbody>
            </table>

            {/* Pagination */}
            <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
              <span className="text-[13px] text-outline">
                Hiển thị {total === 0 ? 0 : page * pageSize + 1}-{Math.min((page + 1) * pageSize, total)} trên {total} kết quả
              </span>
              <div className="flex gap-1">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                >
                  <span className="material-symbols-outlined text-primary" style={{ fontSize: 18 }}>chevron_left</span>
                </button>
                {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                  const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
                  const p = startPage + i;
                  if (p >= totalPages) return null;
                  return (
                    <button
                      key={p}
                      onClick={() => setPage(p)}
                      className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${page === p ? 'border-0 bg-primary-container text-on-primary' : 'border border-outline-variant bg-surface text-on-surface-variant'}`}
                    >
                      {p + 1}
                    </button>
                  );
                })}
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page >= totalPages - 1 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                >
                  <span className="material-symbols-outlined text-primary" style={{ fontSize: 18 }}>chevron_right</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
