import { useEffect, useState, useCallback, useMemo } from 'react';
import ModPortalSidebar from '../../moderation/components/ModPortalSidebar';
import {
  fetchRedFlagRules,
  createRedFlagRule,
  updateRedFlagRule,
  deleteRedFlagRule,
} from '../services/redFlagRuleApi';
import {
  SEVERITY_LABELS,
  SEVERITY_STYLES,
  ACTION_LABELS,
  type RedFlagRule,
  type RedFlagSeverity,
  type RedFlagAction,
} from '../models/redFlagRule';

interface RuleFormState {
  keyword: string;
  severity: RedFlagSeverity;
  action: RedFlagAction;
}

const DEFAULT_FORM: RuleFormState = { keyword: '', severity: 'YELLOW', action: 'WARN' };

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export default function SafetyRuleManagementPage() {
  const [rules, setRules] = useState<RedFlagRule[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [severityFilter, setSeverityFilter] = useState<RedFlagSeverity | ''>('');
  const [activeFilter, setActiveFilter] = useState<'all' | 'active' | 'inactive'>('all');

  const [modalOpen, setModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<RedFlagRule | null>(null);
  const [form, setForm] = useState<RuleFormState>(DEFAULT_FORM);
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const page = await fetchRedFlagRules({
        severity: severityFilter || undefined,
        isActive: activeFilter === 'all' ? undefined : activeFilter === 'active',
        page: 0,
        size: 50,
      });
      setRules(page.content);
      setTotalElements(page.totalElements);
    } catch {
      setError('Không tải được danh sách quy tắc.');
      setRules([]);
    } finally {
      setLoading(false);
    }
  }, [severityFilter, activeFilter]);

  useEffect(() => { load(); }, [load]);

  const visibleRules = useMemo(() => {
    if (!search.trim()) return rules;
    const q = search.trim().toLowerCase();
    return rules.filter((r) => r.keyword.toLowerCase().includes(q));
  }, [rules, search]);

  const stats = useMemo(() => {
    const activeCount = rules.filter((r) => r.isActive).length;
    const criticalCount = rules.filter((r) => r.severity === 'RED').length;
    const systemDefaultCount = rules.filter((r) => r.isSystemDefault).length;
    return { activeCount, criticalCount, systemDefaultCount };
  }, [rules]);

  const recentRules = useMemo(
    () => [...rules].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()).slice(0, 4),
    [rules],
  );

  const openCreateModal = () => {
    setEditingRule(null);
    setForm(DEFAULT_FORM);
    setFormError('');
    setModalOpen(true);
  };

  const openEditModal = (rule: RedFlagRule) => {
    setEditingRule(rule);
    setForm({ keyword: rule.keyword, severity: rule.severity, action: rule.action });
    setFormError('');
    setModalOpen(true);
  };

  const closeModal = () => {
    if (submitting) return;
    setModalOpen(false);
  };

  const handleSubmit = async () => {
    if (!form.keyword.trim()) {
      setFormError('Vui lòng nhập từ khóa quy tắc.');
      return;
    }
    setSubmitting(true);
    setFormError('');
    try {
      if (editingRule) {
        await updateRedFlagRule(editingRule.id, form);
      } else {
        await createRedFlagRule(form);
      }
      setModalOpen(false);
      await load();
    } catch (err: any) {
      const code = err?.response?.data?.error;
      if (code === 'MOD-025') setFormError('Từ khóa này đã tồn tại trong hệ thống.');
      else setFormError(err?.response?.data?.message ?? 'Có lỗi xảy ra, vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleActive = async (rule: RedFlagRule) => {
    try {
      await updateRedFlagRule(rule.id, { isActive: !rule.isActive });
      await load();
    } catch (err: any) {
      const code = err?.response?.data?.error;
      alert(code === 'MOD-027' ? 'Không thể tắt quy tắc mặc định của hệ thống.' : 'Có lỗi xảy ra khi cập nhật trạng thái.');
    }
  };

  const handleDelete = async (rule: RedFlagRule) => {
    if (rule.isSystemDefault) return;
    if (!window.confirm(`Xóa quy tắc "${rule.keyword}"? Hành động này không thể hoàn tác.`)) return;
    try {
      await deleteRedFlagRule(rule.id);
      await load();
    } catch {
      alert('Có lỗi xảy ra khi xóa quy tắc.');
    }
  };

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
        {/* Page Header */}
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">ModPortal</p>
            <h2 className="portal-title">Quản lý quy tắc an toàn</h2>
            <p className="portal-subtitle">Thiết lập và quản lý các quy tắc phân loại, ngăn chặn nội dung vi phạm.</p>
          </div>
          <button
            onClick={openCreateModal}
            className="portal-primary-button"
          >
            <span className="material-symbols-outlined text-base">add</span>
            Tạo quy tắc mới
          </button>
        </div>

        {/* Search + Filters */}
        <div className="portal-toolbar">
          <div className="relative flex-1 min-w-[240px]">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm kiếm quy tắc theo từ khóa..."
              className="portal-field w-full pl-10"
            />
          </div>
          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value as RedFlagSeverity | '')}
            className="portal-field"
          >
            <option value="">Tất cả mức độ</option>
            <option value="RED">Nghiêm trọng</option>
            <option value="YELLOW">Cảnh báo</option>
            <option value="GREEN">Bình thường</option>
          </select>
          <select
            value={activeFilter}
            onChange={(e) => setActiveFilter(e.target.value as 'all' | 'active' | 'inactive')}
            className="portal-field"
          >
            <option value="all">Tất cả trạng thái</option>
            <option value="active">Đang chạy</option>
            <option value="inactive">Đã tắt</option>
          </select>
        </div>

        {/* Bento Grid — stats + recent activity */}
        <div className="mb-5 grid grid-cols-1 gap-4 lg:grid-cols-12">
          <div className="col-span-1 lg:col-span-8 grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="portal-stat-card">
              <div className="flex items-center gap-3 mb-3">
                <div className="portal-icon">
                  <span className="material-symbols-outlined text-lg">rule</span>
                </div>
                <h3 className="text-sm font-semibold text-on-surface">Đang hoạt động</h3>
              </div>
              <p className="portal-metric text-primary">{stats.activeCount}</p>
              <p className="mt-1 text-xs text-outline">/ {totalElements} quy tắc</p>
            </div>
            <div className="portal-stat-card">
              <div className="flex items-center gap-3 mb-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-md bg-error-container text-error">
                  <span className="material-symbols-outlined text-lg">warning</span>
                </div>
                <h3 className="text-sm font-semibold text-on-surface">Mức nghiêm trọng</h3>
              </div>
              <p className="portal-metric">{stats.criticalCount}</p>
              <p className="mt-1 text-xs text-outline">Cần xem xét đánh giá</p>
            </div>
            <div className="portal-stat-card">
              <div className="flex items-center gap-3 mb-3">
                <div className="portal-icon">
                  <span className="material-symbols-outlined text-lg">verified_user</span>
                </div>
                <h3 className="text-sm font-semibold text-on-surface">Mặc định hệ thống</h3>
              </div>
              <p className="portal-metric">{stats.systemDefaultCount}</p>
              <p className="mt-1 text-xs text-outline">Không thể xóa/tắt</p>
            </div>
          </div>
          <div className="portal-card-padded col-span-1 lg:col-span-4">
            <h3 className="mb-3 text-sm font-semibold text-on-surface">Cập nhật gần đây</h3>
            <div className="space-y-3">
              {recentRules.length === 0 && <p className="text-sm text-on-surface-variant">Chưa có quy tắc nào.</p>}
              {recentRules.map((r) => (
                <div key={r.id} className="flex gap-3">
                  <span className="material-symbols-outlined mt-0.5 text-lg text-primary">edit_document</span>
                  <div>
                    <p className="text-sm font-medium text-on-surface">{r.keyword}</p>
                    <p className="text-xs text-outline">{formatDateTime(r.updatedAt)}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Rule List */}
        <div className="portal-table-card">
          <div className="flex items-center justify-between border-b border-outline-variant/70 p-4">
            <h3 className="text-sm font-semibold text-on-surface">Danh sách quy tắc</h3>
            <button onClick={load} className="flex items-center gap-1 text-xs font-semibold text-primary hover:underline">
              <span className="material-symbols-outlined text-base">refresh</span>
              Làm mới
            </button>
          </div>

          {loading ? (
            <div className="flex justify-center py-16">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          ) : error ? (
            <div className="m-4 portal-error">{error}</div>
          ) : visibleRules.length === 0 ? (
            <div className="portal-empty m-4">
              <span className="material-symbols-outlined mb-2 block text-4xl">rule</span>
              <p className="font-medium">Không có quy tắc nào phù hợp</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table>
                <thead>
                  <tr>
                    <th className="pl-4">Tên quy tắc</th>
                    <th>Mức độ</th>
                    <th>Hành động xử lý</th>
                    <th>Trạng thái</th>
                    <th>Cập nhật lúc</th>
                    <th className="pr-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleRules.map((rule) => (
                    <tr key={rule.id}>
                      <td className="pl-4">
                        <p className="font-medium text-on-surface">{rule.keyword}</p>
                        {rule.isSystemDefault && (
                          <p className="mt-0.5 flex items-center gap-1 text-xs text-outline">
                            <span className="material-symbols-outlined text-[14px]">lock</span> Mặc định hệ thống
                          </p>
                        )}
                      </td>
                      <td>
                        <span className={`inline-flex items-center rounded px-2 py-1 text-xs font-medium ${SEVERITY_STYLES[rule.severity]}`}>
                          {SEVERITY_LABELS[rule.severity]}
                        </span>
                      </td>
                      <td className="text-on-surface-variant">{ACTION_LABELS[rule.action]}</td>
                      <td>
                        <button
                          onClick={() => handleToggleActive(rule)}
                          disabled={rule.isSystemDefault}
                          className={`flex items-center gap-2 text-sm text-on-surface-variant ${rule.isSystemDefault ? 'cursor-not-allowed opacity-70' : 'cursor-pointer'}`}
                          title={rule.isSystemDefault ? 'Quy tắc mặc định luôn hoạt động' : 'Bấm để bật/tắt'}
                        >
                          <div className={`h-2 w-2 rounded-full ${rule.isActive ? 'bg-emerald-500' : 'bg-outline-variant'}`} />
                          {rule.isActive ? 'Đang chạy' : 'Đã tắt'}
                        </button>
                      </td>
                      <td className="text-outline">{formatDateTime(rule.updatedAt)}</td>
                      <td className="pr-4 text-right">
                        <button
                          onClick={() => openEditModal(rule)}
                          className="inline-flex h-8 w-8 items-center justify-center rounded-md text-outline hover:bg-surface-container-low hover:text-primary"
                        >
                          <span className="material-symbols-outlined text-lg">edit</span>
                        </button>
                        <button
                          onClick={() => handleDelete(rule)}
                          disabled={rule.isSystemDefault}
                          title={rule.isSystemDefault ? 'Không thể xóa quy tắc mặc định' : 'Xóa quy tắc'}
                          className={`inline-flex h-8 w-8 items-center justify-center rounded-md transition-colors ${
                            rule.isSystemDefault ? 'cursor-not-allowed text-outline-variant' : 'text-outline hover:bg-error-container hover:text-error'
                          }`}
                        >
                          <span className="material-symbols-outlined text-lg">delete</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        </div>
      </main>

      {/* Create / Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-[3px]" onClick={closeModal}>
          <div
            className="portal-modal-panel"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="mb-4 text-sm font-semibold text-on-surface">
              {editingRule ? 'Chỉnh sửa quy tắc' : 'Tạo quy tắc mới'}
            </h3>

            <label className="portal-label">Từ khóa</label>
            <input
              value={form.keyword}
              onChange={(e) => setForm((f) => ({ ...f, keyword: e.target.value }))}
              placeholder="Ví dụ: chảy máu nhiều"
              className="portal-field mb-4 w-full"
            />

            <label className="portal-label">Mức độ</label>
            <select
              value={form.severity}
              onChange={(e) => setForm((f) => ({ ...f, severity: e.target.value as RedFlagSeverity }))}
              className="portal-field mb-4 w-full"
            >
              <option value="GREEN">Bình thường</option>
              <option value="YELLOW">Cảnh báo</option>
              <option value="RED">Nghiêm trọng</option>
            </select>

            <label className="portal-label">Hành động xử lý</label>
            <select
              value={form.action}
              onChange={(e) => setForm((f) => ({ ...f, action: e.target.value as RedFlagAction }))}
              className="portal-field mb-4 w-full"
            >
              <option value="WARN">Cảnh báo</option>
              <option value="BLOCK">Chặn</option>
              <option value="ESCALATE">Leo thang</option>
            </select>

            {formError && <p className="mb-3 text-sm text-error">{formError}</p>}

            <div className="flex gap-3 mt-2">
              <button
                onClick={closeModal}
                disabled={submitting}
                className="portal-secondary-button flex-1"
              >
                Hủy
              </button>
              <button
                onClick={handleSubmit}
                disabled={submitting}
                className="portal-primary-button flex-1"
              >
                {submitting ? 'Đang lưu...' : editingRule ? 'Lưu thay đổi' : 'Tạo quy tắc'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
