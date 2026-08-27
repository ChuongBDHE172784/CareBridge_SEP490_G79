import { useEffect, useState, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';

// Admin & System Services
import { searchUsers } from '../services/adminUserApi';
import { searchAuditLogs } from '../services/auditLogApi';
import { fetchSystemConfiguration, type SystemConfiguration } from '../../aiRuleManagement/services/systemConfigurationApi';
import { fetchStaffContentList, fetchAdminChecklistTemplates } from '../../contentManagement/services/contentApi';

// Models
import {
  formatAuditDetails,
  getAuditActionLabel,
  type AuditLogEntry,
} from '../models/auditLog';

function formatNumber(n: number): string {
  return n.toLocaleString('vi-VN');
}

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  const date = new Date(iso);
  return Number.isNaN(date.getTime())
    ? '—'
    : date.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
}

function getDashboardErrorMessage(error: unknown): string {
  const err = error as { response?: { status?: number; data?: { message?: string; error?: string } } };
  const status = err.response?.status;
  const message = err.response?.data?.message;
  if (status === 403) return 'Tài khoản hiện tại không có quyền System Admin để xem trung tâm điều hành.';
  if (status === 500) return 'Máy chủ gặp lỗi khi tổng hợp dữ liệu điều hành hệ thống.';
  if (status) return message ?? `Không tải được dữ liệu quản trị (${status}).`;
  return 'Không kết nối được máy chủ khi tải dữ liệu tổng quan quản trị.';
}

/* ------------------------------------------------------------------ */
/*  Admin Operational Task Status Panel Component                     */
/* ------------------------------------------------------------------ */
function AdminTaskStatusPanel({
  lockedAccounts,
  pendingContent,
}: {
  lockedAccounts: number;
  pendingContent: number;
}) {
  const navigate = useNavigate();

  const tasks = [
    {
      // The in-app appeal queue was retired; users now reach support directly, so
      // what the admin needs to see is the locked accounts themselves.
      title: 'Tài khoản đang bị khóa',
      detail: 'Tài khoản bị khóa, chờ chăm sóc khách hàng xác minh và mở lại',
      count: lockedAccounts,
      unit: 'tài khoản',
      icon: 'lock_person',
      iconTone: 'bg-amber-100 text-amber-700',
      badge: lockedAccounts > 0 ? `${lockedAccounts} đang khóa` : 'Bình thường',
      badgeTone: lockedAccounts > 0 ? 'bg-amber-50 text-amber-800 border-amber-300' : 'bg-slate-100 text-slate-600 border-slate-300',
      path: '/admin/users?locked=true',
    },
    {
      title: 'Duyệt nội dung y tế chờ xuất bản',
      detail: 'Bài viết chuyên môn & bộ kiểm tra checklist chờ xuất bản',
      count: pendingContent,
      unit: 'bài/checklist',
      icon: 'fact_check',
      iconTone: 'bg-emerald-100 text-emerald-700',
      badge: pendingContent > 0 ? `${pendingContent} bài chờ` : 'Hoàn thành',
      badgeTone: pendingContent > 0 ? 'bg-emerald-50 text-emerald-800 border-emerald-300' : 'bg-slate-100 text-slate-600 border-slate-300',
      path: '/admin/content-approval-queue',
    },
  ];

  return (
    <div className="space-y-3 pt-1">
      {tasks.map((task) => (
        <div
          key={task.title}
          onClick={() => navigate(task.path)}
          className="group flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-xl border border-outline-variant/40 bg-surface-container-low/60 hover:bg-surface hover:border-primary/40 hover:shadow-sm transition-all cursor-pointer gap-3"
        >
          <div className="flex items-start sm:items-center gap-3.5 min-w-0">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${task.iconTone}`}>
              <span className="material-symbols-outlined text-xl">{task.icon}</span>
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold text-on-surface truncate">{task.title}</span>
                <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${task.badgeTone}`}>
                  {task.badge}
                </span>
              </div>
              <p className="text-xs text-on-surface-variant line-clamp-1 mt-0.5 m-0">{task.detail}</p>
            </div>
          </div>

          <div className="flex items-center justify-between sm:justify-end gap-4 shrink-0 border-t sm:border-t-0 pt-2 sm:pt-0 border-outline-variant/30">
            <div className="text-right">
              <span className="text-base font-extrabold text-on-surface block leading-tight">{formatNumber(task.count)}</span>
              <span className="text-[10px] text-outline uppercase font-semibold">{task.unit}</span>
            </div>
            <span className="material-symbols-outlined text-outline text-lg group-hover:translate-x-1 group-hover:text-primary transition-all">
              chevron_right
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Quick Hub Cards Data                                              */
/* ------------------------------------------------------------------ */
const QUICK_HUB_ITEMS = [
  {
    title: 'Xét duyệt Chuyên gia & Đối tác',
    description: 'Rà soát bằng cấp y tế, giấy phép hành nghề, eKYC định danh và hồ sơ đối tác y tế.',
    icon: 'medical_services',
    path: '/admin/expert-verification-queue',
    badgeText: 'Chuyên gia & Đối tác',
    iconBg: 'bg-purple-100 text-purple-700',
    badgeTone: 'bg-purple-50 text-purple-700 border-purple-200',
    actionText: 'Mở hàng chờ xét duyệt',
  },
  {
    title: 'Quản lý Tài khoản & Phân quyền',
    description: 'Quản lý người dùng toàn hệ thống, tạo tài khoản staff vận hành và xử lý khiếu nại.',
    icon: 'manage_accounts',
    path: '/admin/users',
    badgeText: 'Tài khoản & Quản trị',
    iconBg: 'bg-blue-100 text-blue-700',
    badgeTone: 'bg-blue-50 text-blue-700 border-blue-200',
    actionText: 'Quản lý danh sách tài khoản',
  },
  {
    title: 'Chính sách kiểm duyệt AI',
    description: 'Cấu hình chính sách tự động kiểm duyệt AI và tham số bài tập tư thế.',
    icon: 'shield',
    path: '/admin/safety-rules',
    badgeText: 'AI & Quy tắc An toàn',
    iconBg: 'bg-amber-100 text-amber-800',
    badgeTone: 'bg-amber-50 text-amber-800 border-amber-200',
    actionText: 'Cấu hình quy tắc an toàn',
  },
];

export default function AdminDashboardPage() {
  const navigate = useNavigate();

  // Stats & State
  const [totalUsers, setTotalUsers] = useState<number>(0);
  const [lockedUsersCount, setLockedUsersCount] = useState<number>(0);
  const [pendingContentCount, setPendingContentCount] = useState<number>(0);
  const [recentAuditLogs, setRecentAuditLogs] = useState<AuditLogEntry[]>([]);
  const [totalAuditLogs, setTotalAuditLogs] = useState<number>(0);
  const [systemConfig, setSystemConfig] = useState<SystemConfiguration | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const [
        usersRes,
        lockedUsersRes,
        contentRes,
        checklistRes,
        auditLogsRes,
        systemConfigRes,
      ] = await Promise.allSettled([
        searchUsers({ page: 0, size: 1 }),
        searchUsers({ locked: true, page: 0, size: 1 }),
        fetchStaffContentList({ status: 'PENDING_REVIEW', size: 1 }),
        fetchAdminChecklistTemplates({ status: 'PENDING_REVIEW', size: 1 }),
        searchAuditLogs({ page: 0, size: 5 }),
        fetchSystemConfiguration(),
      ]);

      if (usersRes.status === 'fulfilled') {
        setTotalUsers(usersRes.value.totalElements);
      }
      if (lockedUsersRes.status === 'fulfilled') {
        setLockedUsersCount(lockedUsersRes.value.totalElements);
      }

      let contentCount = 0;
      if (contentRes.status === 'fulfilled') {
        contentCount += contentRes.value.totalElements ?? 0;
      }
      if (checklistRes.status === 'fulfilled') {
        contentCount += checklistRes.value.totalElements ?? 0;
      }
      setPendingContentCount(contentCount);

      if (auditLogsRes.status === 'fulfilled') {
        setRecentAuditLogs(auditLogsRes.value.content);
        setTotalAuditLogs(auditLogsRes.value.totalElements);
      }
      if (systemConfigRes.status === 'fulfilled') {
        setSystemConfig(systemConfigRes.value);
      }
    } catch (err) {
      setError(getDashboardErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  return (
    <div className="p-8 font-sans space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-[26px] font-bold text-on-surface m-0">Trung tâm Điều hành System Admin</h1>
            {systemConfig && (
              <div className="flex items-center gap-2">
                <span
                  className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ${
                    systemConfig.maintenanceModeEnabled
                      ? 'bg-amber-50 text-amber-800 border-amber-300'
                      : 'bg-emerald-50 text-emerald-700 border-emerald-300'
                  }`}
                >
                  <span className={`w-2 h-2 rounded-full ${systemConfig.maintenanceModeEnabled ? 'bg-amber-500 animate-pulse' : 'bg-emerald-500'}`} />
                  {systemConfig.maintenanceModeEnabled ? 'Hệ thống đang Bảo trì' : 'Hệ thống Bình thường'}
                </span>
                <span
                  className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold border ${
                    systemConfig.aiModerationEnabled
                      ? 'bg-blue-50 text-blue-700 border-blue-200'
                      : 'bg-slate-100 text-slate-600 border-slate-300'
                  }`}
                >
                  <span className="material-symbols-outlined text-sm">smart_toy</span>
                  {systemConfig.aiModerationEnabled ? 'AI Engine: Bật' : 'AI Engine: Tắt'}
                </span>
              </div>
            )}
          </div>
          <p className="text-on-surface-variant text-sm mt-1">
            Tổng quan vận hành, bảo mật, quản trị tài khoản, xét duyệt chuyên gia và quy tắc an toàn toàn hệ thống CareBridge.
          </p>
        </div>

        <div className="flex items-center gap-2.5 self-start md:self-auto">
          <button
            type="button"
            onClick={loadAll}
            className="flex items-center gap-2 py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-on-surface-variant text-[13px] font-semibold cursor-pointer hover:bg-surface-container-low transition-colors shadow-sm"
          >
            <span className={`material-symbols-outlined text-lg ${loading ? 'animate-spin' : ''}`}>refresh</span> Làm mới dữ liệu
          </button>
        </div>
      </div>

      {/* Warning Banner if System in Maintenance Mode */}
      {systemConfig?.maintenanceModeEnabled && (
        <div className="rounded-2xl border border-amber-300 bg-amber-50 p-4 flex items-center justify-between text-amber-900 shadow-sm">
          <div className="flex items-center gap-3">
            <span className="material-symbols-outlined text-amber-600 text-2xl">warning</span>
            <div>
              <div className="text-sm font-bold">Hệ thống đang ở chế độ bảo trì</div>
              <div className="text-xs text-amber-800 mt-0.5">
                Các thao tác sửa đổi dữ liệu từ phía người dùng cuối tạm thời bị giới hạn. Vui lòng kiểm tra lại cấu hình trước khi mở lại.
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => navigate('/admin/system-configuration')}
            className="px-4 py-2 rounded-xl bg-amber-600 text-white text-xs font-bold hover:bg-amber-700 transition-colors shrink-0"
          >
            Quản lý chế độ bảo trì
          </button>
        </div>
      )}

      {loading && !totalUsers ? (
        <div className="py-16 text-center text-outline flex flex-col items-center justify-center gap-2 bg-surface rounded-2xl border border-outline-variant/60 shadow-sm">
          <span className="material-symbols-outlined text-3xl animate-spin text-primary">progress_activity</span>
          <span className="text-xs font-semibold">Đang đồng bộ dữ liệu quản trị toàn hệ thống...</span>
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error shadow-sm">
          {error}
        </div>
      ) : (
        <>
          {/* Operational KPI Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* KPI 1: Users & locked accounts */}
            <div
              onClick={() => navigate('/admin/users')}
              className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60 flex items-center justify-between cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all"
            >
              <div>
                <div className="text-[13px] text-outline font-semibold mb-1">Quản lý Người dùng</div>
                <div className="text-[28px] font-bold text-primary leading-tight">
                  {formatNumber(totalUsers)}
                </div>
                <div className="text-xs text-outline mt-1">
                  Tài khoản đang bị khóa: <span className="font-bold text-amber-600">{formatNumber(lockedUsersCount)}</span>
                </div>
              </div>
              <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-2xl text-primary">manage_accounts</span>
              </div>
            </div>

            {/* KPI 2: Medical Content Approval */}
            <div
              onClick={() => navigate('/admin/content-approval-queue')}
              className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60 flex items-center justify-between cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-all"
            >
              <div>
                <div className="text-[13px] text-outline font-semibold mb-1">Duyệt Nội dung Y tế</div>
                <div className="text-[28px] font-bold text-[#10b981] leading-tight">
                  {formatNumber(pendingContentCount)}
                </div>
                <div className="text-xs text-outline mt-1">Bài viết & bộ kiểm tra chờ xuất bản</div>
              </div>
              <div className="w-12 h-12 rounded-full bg-emerald-100 flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-2xl text-[#10b981]">fact_check</span>
              </div>
            </div>

          </div>

          {/* Quick Hub Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {QUICK_HUB_ITEMS.map((item) => (
              <div
                key={item.title}
                onClick={() => navigate(item.path)}
                className="group bg-surface rounded-2xl p-5 shadow-sm border border-outline-variant/60 flex flex-col justify-between hover:shadow-md hover:border-primary/40 transition-all cursor-pointer"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${item.iconBg}`}>
                      <span className="material-symbols-outlined text-xl">{item.icon}</span>
                    </div>
                    <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full border ${item.badgeTone}`}>
                      {item.badgeText}
                    </span>
                  </div>
                  <h3 className="text-base font-bold text-on-surface m-0 mb-1">{item.title}</h3>
                  <p className="text-xs text-on-surface-variant leading-relaxed m-0">{item.description}</p>
                </div>
                <div className="mt-4 pt-3 border-t border-outline-variant/40 flex items-center justify-between text-xs font-bold text-primary group-hover:text-primary-hover">
                  <span>{item.actionText}</span>
                  <span className="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">
                    arrow_forward
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Task Status Panel & Engine Controls (2 Columns) */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left 2 Columns: Admin Operational Task Status Panel */}
            <div className="lg:col-span-2 bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60">
              <div className="flex items-center justify-between mb-4 pb-3 border-b border-outline-variant/40">
                <div>
                  <h2 className="text-base font-bold text-on-surface m-0">Trạng thái Hàng chờ & Tác vụ Quản trị</h2>
                  <p className="text-xs text-outline mt-0.5">
                    Chi tiết khối lượng công việc và tiến độ xử lý độc lập từng mảng quản trị hệ thống
                  </p>
                </div>
              </div>

              <AdminTaskStatusPanel
                lockedAccounts={lockedUsersCount}
                pendingContent={pendingContentCount}
              />
            </div>

            {/* Right 1 Column: System Engine Status & Safety Rule Summary */}
            <div className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60 flex flex-col justify-between">
              <div>
                <div className="mb-4 border-b border-outline-variant/40 pb-3">
                  <h2 className="text-base font-bold text-on-surface m-0">Trạng thái Động cơ System Engine</h2>
                  <p className="text-xs text-outline mt-0.5">Thông số vận hành quy tắc AI và cấu hình môi trường</p>
                </div>

                <div className="space-y-3">
                  <div className="p-3.5 rounded-xl bg-surface-container-low border border-outline-variant/40 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <span className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center shrink-0">
                        <span className="material-symbols-outlined text-lg">smart_toy</span>
                      </span>
                      <div>
                        <div className="text-xs font-bold text-on-surface">Động cơ AI Moderation</div>
                        <div className="text-[11px] text-outline">Tự động gắn cờ vi phạm</div>
                      </div>
                    </div>
                    <span className={`text-xs font-bold px-2.5 py-0.5 rounded-full border ${systemConfig?.aiModerationEnabled ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-600 border-slate-300'}`}>
                      {systemConfig?.aiModerationEnabled ? 'Hoạt động' : 'Tắt'}
                    </span>
                  </div>

                  <div className="p-3.5 rounded-xl bg-surface-container-low border border-outline-variant/40 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <span className="w-8 h-8 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center shrink-0">
                        <span className="material-symbols-outlined text-lg">build</span>
                      </span>
                      <div>
                        <div className="text-xs font-bold text-on-surface">Chế độ Bảo trì Hệ thống</div>
                        <div className="text-[11px] text-outline">Maintenance Mode</div>
                      </div>
                    </div>
                    <span className={`text-xs font-bold px-2.5 py-0.5 rounded-full border ${systemConfig?.maintenanceModeEnabled ? 'bg-amber-50 text-amber-800 border-amber-300' : 'bg-slate-100 text-slate-600 border-slate-300'}`}>
                      {systemConfig?.maintenanceModeEnabled ? 'Đang bật' : 'Bình thường'}
                    </span>
                  </div>

                  <div className="p-3.5 rounded-xl bg-surface-container-low border border-outline-variant/40 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <span className="w-8 h-8 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center shrink-0">
                        <span className="material-symbols-outlined text-lg">rule</span>
                      </span>
                      <div>
                        <div className="text-xs font-bold text-on-surface">Quy tắc An toàn Red Flag</div>
                        <div className="text-[11px] text-outline">AI Safety Policies</div>
                      </div>
                    </div>
                    <Link
                      to="/admin/safety-rules"
                      className="text-xs font-bold text-[#00668c] flex items-center gap-1 hover:underline no-underline"
                    >
                      Quản lý quy tắc <span className="material-symbols-outlined text-sm">arrow_forward</span>
                    </Link>
                  </div>
                </div>
              </div>

              <div className="mt-6 pt-4 border-t border-outline-variant/40 text-[11px] text-outline flex items-center justify-between">
                <span>Trạng thái kết nối: Ổn định</span>
                <span>
                  {systemConfig?.updatedAt ? formatDate(systemConfig.updatedAt) : 'Vừa cập nhật'}
                </span>
              </div>
            </div>
          </div>

          {/* Recent Audit Logs Table */}
          <div className="bg-surface rounded-2xl p-6 shadow-sm border border-outline-variant/60">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4 pb-3 border-b border-outline-variant/40">
              <div>
                <h2 className="text-base font-bold text-on-surface m-0">Nhật ký Thao tác Gần nhất</h2>
                <p className="text-xs text-outline mt-0.5">Truy vết thao tác audit log hệ thống, bao gồm khóa và mở khóa tài khoản</p>
              </div>
              <span className="self-start md:self-auto rounded-xl border border-outline-variant/40 bg-surface-container-low px-4 py-1.5 text-xs font-bold text-outline">
                {formatNumber(totalAuditLogs)} bản ghi
              </span>
            </div>

            {(
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b border-outline-variant/60 text-left">
                      <th className="py-3 px-3 text-[11px] font-bold text-outline uppercase tracking-wider">Hành động</th>
                      <th className="py-3 px-3 text-[11px] font-bold text-outline uppercase tracking-wider">Thực hiện bởi</th>
                      <th className="py-3 px-3 text-[11px] font-bold text-outline uppercase tracking-wider">Thời gian</th>
                      <th className="py-3 px-3 text-[11px] font-bold text-outline uppercase tracking-wider">Chi tiết / Ghi chú</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant/30">
                    {recentAuditLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-surface-container-low/60 transition-colors">
                        <td className="py-3.5 px-3">
                          <span className="inline-flex text-xs font-bold text-primary px-2.5 py-1 rounded-lg bg-primary/10">
                            {getAuditActionLabel(log.action)}
                          </span>
                        </td>
                        <td className="py-3.5 px-3 text-xs text-on-surface">
                          {log.actorName || log.actorEmail ? (
                            <div className="min-w-0">
                              <div className="font-semibold text-on-surface truncate">
                                {log.actorName || log.actorEmail}
                              </div>
                              {log.actorEmail && log.actorEmail !== log.actorName && (
                                <div className="text-[11px] text-outline truncate">{log.actorEmail}</div>
                              )}
                            </div>
                          ) : log.userId ? (
                            <span className="text-on-surface-variant">Tài khoản không xác định</span>
                          ) : (
                            <span className="text-outline">Hệ thống tự động</span>
                          )}
                        </td>
                        <td className="py-3.5 px-3 text-xs text-outline whitespace-nowrap">
                          {formatDate(log.timestamp)}
                        </td>
                        <td className="py-3.5 px-3 text-xs text-on-surface-variant max-w-md leading-relaxed">
                          {formatAuditDetails(log.action, log.details)}
                        </td>
                      </tr>
                    ))}
                    {recentAuditLogs.length === 0 && (
                      <tr>
                        <td colSpan={4} className="py-8 text-center text-outline text-xs">
                          Chưa ghi nhận nhật ký audit hệ thống gần đây.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
