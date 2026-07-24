import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type {
  AdminChecklistTemplate,
  ChecklistTemplateStatus,
  ContentStage,
} from '../models/content';
import {
  CHECKLIST_STATUS_LABELS,
  STAGE_LABELS,
} from '../models/content';
import { fetchAdminChecklists } from '../services/contentApi';

const PAGE_SIZE = 10;

function stageBadgeClass(stage: ContentStage | null): string {
  switch (stage) {
    case 'PRE_PREGNANCY': return 'bg-[#F2EAE4] text-[#5A463F]';
    case 'PREGNANCY': return 'bg-[#C98C7B]/15 text-[#845143]';
    case 'POSTPARTUM': return 'bg-[#E6F4EA] text-[#137333]';
    case 'BABY_CARE': return 'bg-[#FFE9E3] text-[#845143]';
    case null: return 'bg-[#F2EAE4] text-[#5A463F]';
  }
}

function statusBadgeClass(status: ChecklistTemplateStatus): string {
  switch (status) {
    case 'APPROVED': return 'bg-[#E6F4EA] text-[#137333]';
    case 'REJECTED': return 'bg-[#FDE8E5] text-[#9F3A32]';
    case 'PENDING_REVIEW': return 'bg-[#FFF3D6] text-[#7A5713]';
    case 'ARCHIVED': return 'bg-[#E8DDD6] text-[#5A463F]';
    case 'DRAFT': return 'bg-[#F2EAE4] text-[#5A463F]';
  }
}

function formatUpdatedAt(value: string | null): string {
  if (!value) return 'Chưa cập nhật';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? 'Chưa cập nhật'
    : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(date);
}

export default function ChecklistListPage() {
  const navigate = useNavigate();
  const [checklists, setChecklists] = useState<AdminChecklistTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [stageFilter, setStageFilter] = useState<ContentStage | ''>('');
  const [statusFilter, setStatusFilter] = useState<ChecklistTemplateStatus | ''>('');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const latestRequestId = useRef(0);

  const loadData = useCallback(async () => {
    const requestId = latestRequestId.current + 1;
    latestRequestId.current = requestId;
    let isCorrectingPage = false;
    setIsLoading(true);
    setError('');
    try {
      const result = await fetchAdminChecklists({
        stage: stageFilter || undefined,
        status: statusFilter || undefined,
        page,
        size: PAGE_SIZE,
      });
      if (requestId !== latestRequestId.current) return;
      const lastAvailablePage = result.totalPages === 0 ? 0 : result.totalPages - 1;
      if (page > lastAvailablePage) {
        isCorrectingPage = true;
        setPage(lastAvailablePage);
        return;
      }
      setChecklists(result.content);
      setTotalElements(result.totalElements);
      setTotalPages(result.totalPages);
    } catch {
      if (requestId !== latestRequestId.current) return;
      setChecklists([]);
      setTotalElements(0);
      setTotalPages(0);
      setError('Không thể tải danh sách checklist. Vui lòng thử lại.');
    } finally {
      if (requestId === latestRequestId.current && !isCorrectingPage) {
        setIsLoading(false);
      }
    }
  }, [page, stageFilter, statusFilter]);

  useEffect(() => {
    void loadData();
    return () => {
      latestRequestId.current += 1;
    };
  }, [loadData]);

  const from = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const to = Math.min((page + 1) * PAGE_SIZE, totalElements);

  return (
    <main className="min-h-screen bg-[#F6F1EC] p-4 font-sans text-[#5A463F] md:p-8">
      <header className="mb-6 flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <h1 className="m-0 text-[26px] font-black text-[#5A463F]">Quản lý Checklist</h1>
          <p className="mt-2 text-base font-semibold text-[#9C857C]">
            Theo dõi đúng trạng thái duyệt và số mục của từng checklist.
          </p>
        </div>
        <div className="flex flex-wrap items-end gap-3">
          <label className="flex min-w-44 flex-col gap-2 text-sm font-black uppercase tracking-wider text-[#9C857C]">
            Giai đoạn
            <select
              aria-label="Lọc checklist theo giai đoạn"
              value={stageFilter}
              onChange={(event) => {
                setStageFilter(event.target.value as ContentStage | '');
                setPage(0);
              }}
              className="h-12 rounded-xl border-2 border-transparent bg-white px-4 text-base font-bold normal-case tracking-normal text-[#5A463F] shadow-[inset_0_4px_8px_rgba(90,70,63,0.05)] focus:border-[#C98C7B]/40 focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20"
            >
              <option value="">Tất cả</option>
              <option value="PRE_PREGNANCY">Chuẩn bị</option>
              <option value="PREGNANCY">Thai kỳ</option>
              <option value="POSTPARTUM">Sau sinh</option>
              <option value="BABY_CARE">Chăm bé</option>
            </select>
          </label>
          <label className="flex min-w-44 flex-col gap-2 text-sm font-black uppercase tracking-wider text-[#9C857C]">
            Trạng thái
            <select
              aria-label="Lọc checklist theo trạng thái duyệt"
              value={statusFilter}
              onChange={(event) => {
                setStatusFilter(event.target.value as ChecklistTemplateStatus | '');
                setPage(0);
              }}
              className="h-12 rounded-xl border-2 border-transparent bg-white px-4 text-base font-bold normal-case tracking-normal text-[#5A463F] shadow-[inset_0_4px_8px_rgba(90,70,63,0.05)] focus:border-[#C98C7B]/40 focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20"
            >
              <option value="">Tất cả</option>
              {Object.entries(CHECKLIST_STATUS_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          <button
            type="button"
            aria-label="Tạo checklist mới"
            onClick={() => navigate('/content/create?type=CHECKLIST')}
            className="inline-flex h-12 items-center gap-2 rounded-full border-0 bg-[#C98C7B] px-6 text-base font-bold text-white shadow-[0_8px_24px_rgba(201,140,123,0.3)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#B67868] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 active:scale-95"
          >
            <span aria-hidden="true" className="material-symbols-outlined text-lg">add</span>
            Tạo Checklist
          </button>
        </div>
      </header>

      <section
        aria-busy={isLoading}
        aria-live="polite"
        className="rounded-[32px] border border-[#E8DDD6]/50 bg-white p-4 shadow-[0_12px_32px_rgba(90,70,63,0.06)] md:p-6"
      >
        {isLoading ? (
          <div className="py-14 text-center text-base font-semibold text-[#9C857C]">Đang tải...</div>
        ) : error ? (
          <div className="flex flex-col items-center gap-4 py-14 text-center">
            <p role="alert" className="m-0 text-base font-semibold text-[#5A463F]">{error}</p>
            <button
              type="button"
              onClick={() => void loadData()}
              className="h-12 rounded-full bg-[#5A463F] px-6 text-base font-bold text-white transition-all duration-300 focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 active:scale-95"
            >
              Thử lại
            </button>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto rounded-2xl focus-within:ring-4 focus-within:ring-[#C98C7B]/10">
              <table className="w-full min-w-[920px] border-collapse">
                <caption className="sr-only">Danh sách checklist quản trị theo trạng thái duyệt thật</caption>
                <thead>
                  <tr className="border-b-2 border-[#E8DDD6] text-left">
                    {['Tiêu đề', 'Giai đoạn', 'Số mục', 'Trạng thái', 'Cập nhật', 'Thao tác'].map((heading) => (
                      <th key={heading} scope="col" className="px-3 py-4 text-sm font-black uppercase tracking-wider text-[#9C857C]">{heading}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {checklists.map((checklist) => (
                    <tr key={checklist.id} className="border-b border-[#E8DDD6]/70 transition-colors hover:bg-[#F6F1EC]/70">
                      <td className="max-w-[340px] px-3 py-4">
                        <div className="font-bold text-[#5A463F]">{checklist.name}</div>
                        {checklist.description && <div className="mt-1 line-clamp-2 text-sm font-semibold text-[#9C857C]">{checklist.description}</div>}
                      </td>
                      <td className="px-3 py-4">
                        <span className={`inline-flex rounded-full px-4 py-2 text-sm font-bold ${stageBadgeClass(checklist.stage)}`}>
                          {checklist.stage ? STAGE_LABELS[checklist.stage] : 'Không xác định'}
                        </span>
                      </td>
                      <td className="px-3 py-4">
                        <span className="font-black text-[#5A463F]">{checklist.itemCount}</span>
                        <span className="ml-1 text-sm font-semibold text-[#9C857C]">mục</span>
                      </td>
                      <td className="px-3 py-4">
                        <span className={`inline-flex rounded-full px-4 py-2 text-sm font-black ${statusBadgeClass(checklist.status)}`}>
                          {checklist.status} · {CHECKLIST_STATUS_LABELS[checklist.status]}
                        </span>
                      </td>
                      <td className="px-3 py-4 text-sm font-semibold text-[#9C857C]">{formatUpdatedAt(checklist.updatedAt)}</td>
                      <td className="px-3 py-4">
                        <div className="flex gap-2">
                          <button
                            type="button"
                            aria-label={`Xem checklist ${checklist.name}`}
                            onClick={() => navigate(`/content/${checklist.id}`)}
                            className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-[#E8DDD6] bg-white text-[#845143] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#F2EAE4] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 active:scale-95"
                          >
                            <span aria-hidden="true" className="material-symbols-outlined">visibility</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {checklists.length === 0 && (
                    <tr><td colSpan={6} className="px-3 py-14 text-center text-base font-semibold text-[#9C857C]">Không có checklist phù hợp.</td></tr>
                  )}
                </tbody>
              </table>
            </div>

            <footer className="mt-5 flex flex-col gap-4 border-t border-[#E8DDD6] pt-5 sm:flex-row sm:items-center sm:justify-between">
              <span className="text-sm font-semibold text-[#9C857C]">Hiển thị {from}-{to} trên {totalElements} kết quả</span>
              <nav aria-label="Phân trang checklist" className="flex items-center gap-2">
                <button
                  type="button"
                  aria-label="Trang checklist trước"
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  disabled={page === 0}
                  className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-[#E8DDD6] bg-white text-[#845143] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  <span aria-hidden="true" className="material-symbols-outlined">chevron_left</span>
                </button>
                <span className="min-w-24 text-center text-base font-black text-[#5A463F]">Trang {totalPages === 0 ? 0 : page + 1}/{totalPages}</span>
                <button
                  type="button"
                  aria-label="Trang checklist sau"
                  onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                  disabled={totalPages === 0 || page >= totalPages - 1}
                  className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-[#E8DDD6] bg-white text-[#845143] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  <span aria-hidden="true" className="material-symbols-outlined">chevron_right</span>
                </button>
              </nav>
            </footer>
          </>
        )}
      </section>
    </main>
  );
}
