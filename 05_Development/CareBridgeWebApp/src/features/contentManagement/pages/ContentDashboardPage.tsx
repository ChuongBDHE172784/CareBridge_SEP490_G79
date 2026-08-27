import { useEffect, useState, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  fetchStaffContentList,
  fetchAdminChecklists,
  fetchTopics,
} from '../services/contentApi';
import type { ContentDetail, ContentStatus, ContentType } from '../models/content';
import { TYPE_LABELS, STAGE_LABELS, STATUS_LABELS } from '../models/content';

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
const STATUS_TABS: { key: string; label: string; status?: ContentStatus }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'approved', label: 'Đã duyệt', status: 'APPROVED' },
  { key: 'pending', label: 'Chờ duyệt', status: 'PENDING_REVIEW' },
  { key: 'draft', label: 'Bản nháp', status: 'DRAFT' },
];

function statusBadge(item: ContentDetail): { label: string; className: string } {
  if (item.latestReviewFeedback) return { label: 'Cần chỉnh sửa', className: 'bg-error-container text-error' };
  if (item.status === 'APPROVED') return { label: STATUS_LABELS.APPROVED, className: 'bg-[#E6F4EA] text-[#137333]' };
  if (item.status === 'PENDING_REVIEW') return { label: STATUS_LABELS.PENDING_REVIEW, className: 'bg-[#FFF3E0] text-[#E65100]' };
  if (item.status === 'ARCHIVED') return { label: STATUS_LABELS.ARCHIVED, className: 'bg-[#F5F5F5] text-[#616161]' };
  return { label: STATUS_LABELS[item.status] ?? 'Bản nháp', className: 'bg-surface-container-highest text-primary' };
}

function typeIcon(type: ContentType): string {
  if (type === 'ARTICLE') return 'article';
  if (type === 'FAQ') return 'quiz';
  return 'fact_check';
}

function timeAgo(iso: string | null | undefined): string {
  if (!iso) return '—';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

/* ------------------------------------------------------------------ */
/*  StatCard Component                                                */
/* ------------------------------------------------------------------ */
function StatCard({
  icon,
  label,
  value,
  accent,
  onClick,
  isActive,
}: {
  icon: string;
  label: string;
  value: number;
  accent: string;
  onClick?: () => void;
  isActive?: boolean;
}) {
  return (
    <div
      onClick={onClick}
      className={`bg-surface rounded-3xl p-6 shadow-md flex items-center gap-4 ${
        onClick ? 'cursor-pointer hover:shadow-lg transition-all' : ''
      } ${isActive ? 'ring-2 ring-primary bg-surface-container-low' : ''}`}
    >
      <div
        className="w-12 h-12 rounded-full flex items-center justify-center shrink-0"
        style={{ background: accent + '18' }}
      >
        <span className="material-symbols-outlined text-2xl" style={{ color: accent }}>
          {icon}
        </span>
      </div>
      <div>
        <div className="text-[28px] font-bold text-on-surface">{value}</div>
        <div className="text-[13px] text-outline mt-0.5">{label}</div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Quick Hub Items                                                   */
/* ------------------------------------------------------------------ */
const QUICK_HUB_ITEMS = [
  {
    title: 'Quản lý Bài viết',
    desc: 'Bài viết kiến thức y khoa, tư vấn sức khỏe mẹ & bé',
    icon: 'article',
    path: '/content/articles',
    createPath: '/content/articles/create',
    accent: '#0061A4',
  },
  {
    title: 'Câu hỏi FAQ',
    desc: 'Bộ câu hỏi thường gặp và hướng dẫn giải đáp',
    icon: 'quiz',
    path: '/content/faq',
    createPath: '/content/faq/create',
    accent: '#7C3AED',
  },
  {
    title: 'Checklist thai kỳ',
    desc: 'Danh mục kiểm tra mốc chăm sóc theo từng tuần',
    icon: 'checklist',
    path: '/content/checklists',
    createPath: '/content/checklists/create',
    accent: '#0D9488',
  },
  {
    title: 'Bài tập thai kỳ',
    desc: 'Thư viện bài tập vận động, yoga & thai giáo',
    icon: 'fitness_center',
    path: '/content/exercises',
    createPath: '/content/exercises/create',
    accent: '#C05621',
  },
  {
    title: 'Chủ đề & Tag',
    desc: 'Phân loại danh mục và thẻ chủ đề nội dung',
    icon: 'topic',
    path: '/content/topics',
    createPath: undefined,
    accent: '#4F46E5',
  },
  {
    title: 'Thông báo hệ thống',
    desc: 'Bản tin & thông báo cập nhật tới người dùng',
    icon: 'notifications',
    path: '/content/notifications',
    createPath: undefined,
    accent: '#D97706',
  },
];

/* ------------------------------------------------------------------ */
/*  Main Component                                                    */
/* ------------------------------------------------------------------ */
export default function ContentDashboardPage() {
  const navigate = useNavigate();
  const [recentItems, setRecentItems] = useState<ContentDetail[]>([]);
  const [isLoadingRecent, setIsLoadingRecent] = useState(true);
  const [isCreateMenuOpen, setIsCreateMenuOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('all');
  const dropdownRef = useRef<HTMLDivElement>(null);

  const [stats, setStats] = useState({
    drafts: 0,
    pending: 0,
    published: 0,
    articles: 0,
    faqs: 0,
    checklists: 0,
    topics: 0,
  });

  // Close dropdown menu on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsCreateMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Fetch real statistics
  const loadDashboardStats = useCallback(async () => {
    try {
      const [draftsRes, pendingRes, pubRes, artRes, faqRes, chkRes, topRes] = await Promise.allSettled([
        fetchStaffContentList({ status: 'DRAFT', size: 1 }),
        fetchStaffContentList({ status: 'PENDING_REVIEW', size: 1 }),
        fetchStaffContentList({ status: 'APPROVED', size: 1 }),
        fetchStaffContentList({ type: 'ARTICLE', size: 1 }),
        fetchStaffContentList({ type: 'FAQ', size: 1 }),
        fetchAdminChecklists({ size: 1 }),
        fetchTopics(true),
      ]);

      setStats({
        drafts: draftsRes.status === 'fulfilled' ? draftsRes.value.totalElements : 0,
        pending: pendingRes.status === 'fulfilled' ? pendingRes.value.totalElements : 0,
        published: pubRes.status === 'fulfilled' ? pubRes.value.totalElements : 0,
        articles: artRes.status === 'fulfilled' ? artRes.value.totalElements : 0,
        faqs: faqRes.status === 'fulfilled' ? faqRes.value.totalElements : 0,
        checklists: chkRes.status === 'fulfilled' ? chkRes.value.totalElements : 0,
        topics: topRes.status === 'fulfilled' ? topRes.value.length : 0,
      });
    } catch (err) {
      console.error('Failed to load dashboard stats', err);
    }
  }, []);

  // Fetch recent contents filtered by status tab
  const loadRecentContent = useCallback(async (tabKey: string) => {
    setIsLoadingRecent(true);
    try {
      const tabObj = STATUS_TABS.find((t) => t.key === tabKey);
      const data = await fetchStaffContentList({
        status: tabObj?.status,
        page: 0,
        size: 8,
      });
      setRecentItems(data.content);
    } catch {
      setRecentItems([]);
    } finally {
      setIsLoadingRecent(false);
    }
  }, []);

  useEffect(() => {
    loadDashboardStats();
  }, [loadDashboardStats]);

  useEffect(() => {
    loadRecentContent(activeTab);
  }, [activeTab, loadRecentContent]);

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Tổng quan nội dung</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Quản lý, theo dõi chỉ số và truy cập nhanh các module thuộc thư viện CareBridge
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/content/list')}
            className="py-3 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-[13px] font-semibold cursor-pointer hover:bg-surface-container-low"
          >
            Thư viện nội dung
          </button>

          <div className="relative" ref={dropdownRef}>
            <button
              type="button"
              onClick={() => setIsCreateMenuOpen((open) => !open)}
              aria-haspopup="menu"
              aria-expanded={isCreateMenuOpen}
              className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer whitespace-nowrap"
            >
              <span className="material-symbols-outlined text-lg">add</span>
              Tạo nội dung mới
              <span className="material-symbols-outlined text-lg">arrow_drop_down</span>
            </button>

            {isCreateMenuOpen && (
              <div
                role="menu"
                className="absolute right-0 z-10 mt-2 w-56 overflow-hidden rounded-xl border border-outline-variant bg-surface py-1 shadow-lg"
              >
                <button
                  type="button"
                  onClick={() => {
                    setIsCreateMenuOpen(false);
                    navigate('/content/articles/create');
                  }}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-on-surface hover:bg-surface-container-low border-0 bg-transparent cursor-pointer"
                >
                  <span className="material-symbols-outlined text-lg text-primary">article</span>
                  Tạo bài viết y khoa
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsCreateMenuOpen(false);
                    navigate('/content/faq/create');
                  }}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-on-surface hover:bg-surface-container-low border-0 bg-transparent cursor-pointer"
                >
                  <span className="material-symbols-outlined text-lg text-primary">quiz</span>
                  Tạo câu hỏi FAQ
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsCreateMenuOpen(false);
                    navigate('/content/checklists/create');
                  }}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-on-surface hover:bg-surface-container-low border-0 bg-transparent cursor-pointer"
                >
                  <span className="material-symbols-outlined text-lg text-primary">checklist</span>
                  Tạo checklist thai kỳ
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsCreateMenuOpen(false);
                    navigate('/content/exercises/create');
                  }}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-on-surface hover:bg-surface-container-low border-0 bg-transparent cursor-pointer"
                >
                  <span className="material-symbols-outlined text-lg text-primary">fitness_center</span>
                  Tạo bài tập thai kỳ
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Summary cards row 1 */}
      <div className="grid grid-cols-4 gap-4 mb-4">
        <StatCard
          icon="edit_note"
          label="Bản nháp"
          value={stats.drafts}
          accent="#845143"
          onClick={() => setActiveTab('draft')}
          isActive={activeTab === 'draft'}
        />
        <StatCard
          icon="hourglass_top"
          label="Chờ duyệt"
          value={stats.pending}
          accent="#E65100"
          onClick={() => setActiveTab('pending')}
          isActive={activeTab === 'pending'}
        />
        <StatCard
          icon="check_circle"
          label="Đã xuất bản"
          value={stats.published}
          accent="#137333"
          onClick={() => setActiveTab('approved')}
          isActive={activeTab === 'approved'}
        />
        <StatCard
          icon="article"
          label="Bài viết y khoa"
          value={stats.articles}
          accent="#0061A4"
          onClick={() => navigate('/content/articles')}
        />
      </div>

      {/* Summary cards row 2 */}
      <div className="grid grid-cols-3 gap-4 mb-7">
        <StatCard
          icon="quiz"
          label="FAQ"
          value={stats.faqs}
          accent="#7C3AED"
          onClick={() => navigate('/content/faq')}
        />
        <StatCard
          icon="fact_check"
          label="Checklist thai kỳ"
          value={stats.checklists}
          accent="#0D9488"
          onClick={() => navigate('/content/checklists')}
        />
        <StatCard
          icon="topic"
          label="Chủ đề & Thẻ phân loại"
          value={stats.topics}
          accent="#4F46E5"
          onClick={() => navigate('/content/topics')}
        />
      </div>

      {/* Quick Hub Section */}
      <div className="mb-7">
        <div className="text-xs font-semibold text-outline uppercase tracking-wider mb-3">
          Lối truy cập nhanh
        </div>
        <div className="grid grid-cols-3 gap-4">
          {QUICK_HUB_ITEMS.map((item) => (
            <div
              key={item.path}
              className="bg-surface rounded-2xl p-5 shadow-md flex flex-col justify-between hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => navigate(item.path)}
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center"
                    style={{ background: item.accent + '18' }}
                  >
                    <span className="material-symbols-outlined text-xl" style={{ color: item.accent }}>
                      {item.icon}
                    </span>
                  </div>
                  {item.createPath && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(item.createPath!);
                      }}
                      className="py-1 px-3 rounded-full border border-outline-variant bg-transparent text-primary text-xs font-semibold hover:bg-surface-container-low cursor-pointer"
                    >
                      + Tạo mới
                    </button>
                  )}
                </div>
                <div className="font-bold text-base text-on-surface mb-1">{item.title}</div>
                <div className="text-xs text-on-surface-variant leading-relaxed">{item.desc}</div>
              </div>
              <div className="mt-4 pt-3 border-t border-surface-container-highest flex items-center justify-between text-xs font-semibold text-primary">
                <span>Quản lý</span>
                <span className="material-symbols-outlined text-sm">arrow_forward</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent content table section */}
      <div className="bg-surface rounded-2xl p-6 shadow-md">
        <div className="flex justify-between items-center mb-5 flex-wrap gap-3">
          <div>
            <h2 className="text-lg font-bold text-on-surface m-0">Nội dung gần đây</h2>
            <p className="text-xs text-outline mt-0.5">Danh sách các bài viết, FAQ và tài liệu mới cập nhật</p>
          </div>

          {/* Status Tabs matching ContentTypeListPage */}
          <div className="flex gap-2">
            {STATUS_TABS.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`flex items-center gap-1.5 py-2 px-[18px] rounded-full text-[13px] font-semibold cursor-pointer ${
                  activeTab === tab.key
                    ? 'border-2 border-primary bg-surface-container-low text-primary'
                    : 'border border-outline-variant bg-transparent text-on-surface-variant'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {isLoadingRecent ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b-2 border-surface-container-highest text-left">
                {['TIÊU ĐỀ', 'PHÂN LOẠI', 'GIAI ĐOẠN', 'TRẠNG THÁI', 'CẬP NHẬT', 'THAO TÁC'].map((h) => (
                  <th key={h} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {recentItems.map((item) => {
                const status = statusBadge(item);
                return (
                  <tr key={item.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                    <td className="py-3.5 px-2 max-w-[320px]">
                      <div
                        onClick={() => navigate(`/content/${item.id}`)}
                        className="font-semibold text-sm text-on-surface hover:text-primary cursor-pointer"
                      >
                        {item.title}
                      </div>
                      {item.summary && <div className="text-xs text-outline line-clamp-1 mt-0.5">{item.summary}</div>}
                    </td>
                    <td className="py-3.5 px-2">
                      <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                        <span className="material-symbols-outlined text-sm">{typeIcon(item.type)}</span>
                        {TYPE_LABELS[item.type]}
                      </span>
                    </td>
                    <td className="py-3.5 px-2 text-[13px] text-on-surface-variant">
                      {STAGE_LABELS[item.stage]}
                    </td>
                    <td className="py-3.5 px-2">
                      <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${status.className}`}>
                        {status.label}
                      </span>
                    </td>
                    <td className="py-3.5 px-2 text-[13px] text-outline">
                      {timeAgo(item.updatedAt ?? item.createdAt ?? item.publishedAt)}
                    </td>
                    <td className="py-3.5 px-2">
                      <div className="flex gap-1">
                        <button
                          onClick={() => navigate(`/content/${item.id}`)}
                          className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center hover:bg-surface-container-low"
                          title="Xem chi tiết"
                        >
                          <span className="material-symbols-outlined text-primary text-base">visibility</span>
                        </button>
                        <button
                          onClick={() => navigate(`/content/${item.id}/edit`)}
                          className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center hover:bg-surface-container-low"
                          title="Chỉnh sửa"
                        >
                          <span className="material-symbols-outlined text-primary text-base">edit</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {recentItems.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-outline">
                    Không tìm thấy nội dung nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

