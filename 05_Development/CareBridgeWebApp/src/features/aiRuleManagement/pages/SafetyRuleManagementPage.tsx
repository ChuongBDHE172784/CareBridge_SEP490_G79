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
    <div className="min-h-screen bg-[#F6F1EC]">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        {/* Page Header */}
        <div className="flex justify-between items-end mb-6 gap-4 flex-wrap">
          <div>
            <h2 className="text-3xl font-bold text-[#271812] mb-2">Quản lý quy tắc an toàn</h2>
            <p className="text-base text-[#524440]">Thiết lập và quản lý các quy tắc phân loại và ngăn chặn nội dung vi phạm.</p>
          </div>
          <button
            onClick={openCreateModal}
            className="bg-[#C98C7B] text-white h-[52px] px-6 rounded-full font-semibold flex items-center gap-2 hover:opacity-90 transition-opacity shadow-md"
          >
            <span className="material-symbols-outlined">add</span>
            Tạo quy tắc mới
          </button>
        </div>

        {/* Search + Filters */}
        <div className="flex flex-wrap items-center gap-3 mb-6">
          <div className="relative flex-1 min-w-[240px]">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#84736F]">search</span>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm kiếm quy tắc theo từ khóa..."
              className="pl-10 pr-4 py-2 bg-white border border-[#D6C2BD] rounded-full text-sm w-full h-[48px] focus:outline-none focus:ring-2 focus:ring-[#845143]/30"
            />
          </div>
          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value as RedFlagSeverity | '')}
            className="h-[48px] px-4 rounded-full border border-[#D6C2BD] bg-white text-sm text-[#524440]"
          >
            <option value="">Tất cả mức độ</option>
            <option value="RED">Nghiêm trọng</option>
            <option value="YELLOW">Cảnh báo</option>
            <option value="GREEN">Bình thường</option>
          </select>
          <select
            value={activeFilter}
            onChange={(e) => setActiveFilter(e.target.value as 'all' | 'active' | 'inactive')}
            className="h-[48px] px-4 rounded-full border border-[#D6C2BD] bg-white text-sm text-[#524440]"
          >
            <option value="all">Tất cả trạng thái</option>
            <option value="active">Đang chạy</option>
            <option value="inactive">Đã tắt</option>
          </select>
        </div>

        {/* Bento Grid — stats + recent activity */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 mb-6">
          <div className="col-span-1 lg:col-span-8 grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-full bg-[#C98C7B]/20 flex items-center justify-center text-[#845143]">
                  <span className="material-symbols-outlined">rule</span>
                </div>
                <h3 className="text-base font-semibold text-[#271812]">Đang hoạt động</h3>
              </div>
              <p className="text-3xl font-bold text-[#845143]">{stats.activeCount}</p>
              <p className="text-xs text-[#84736F] mt-2">/ {totalElements} quy tắc</p>
            </div>
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-full bg-[#ffdad6] flex items-center justify-center text-[#ba1a1a]">
                  <span className="material-symbols-outlined">warning</span>
                </div>
                <h3 className="text-base font-semibold text-[#271812]">Mức nghiêm trọng</h3>
              </div>
              <p className="text-3xl font-bold text-[#271812]">{stats.criticalCount}</p>
              <p className="text-xs text-[#84736F] mt-2">Cần xem xét đánh giá</p>
            </div>
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-[#FFE2D9]">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-full bg-[#F6DACF] flex items-center justify-center text-[#735E56]">
                  <span className="material-symbols-outlined">verified_user</span>
                </div>
                <h3 className="text-base font-semibold text-[#271812]">Mặc định hệ thống</h3>
              </div>
              <p className="text-3xl font-bold text-[#271812]">{stats.systemDefaultCount}</p>
              <p className="text-xs text-[#84736F] mt-2">Không thể xóa/tắt</p>
            </div>
          </div>
          <div className="col-span-1 lg:col-span-4 bg-white rounded-2xl p-6 shadow-sm border border-[#FFE2D9]">
            <h3 className="text-base font-semibold text-[#271812] mb-4">Cập nhật gần đây</h3>
            <div className="space-y-4">
              {recentRules.length === 0 && <p className="text-sm text-[#84736F]">Chưa có quy tắc nào.</p>}
              {recentRules.map((r) => (
                <div key={r.id} className="flex gap-3">
                  <span className="material-symbols-outlined text-[#845143] mt-1 text-lg">edit_document</span>
                  <div>
                    <p className="text-sm text-[#271812] font-medium">{r.keyword}</p>
                    <p className="text-xs text-[#84736F]">{formatDateTime(r.updatedAt)}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Rule List */}
        <div className="bg-white rounded-2xl shadow-sm border border-[#FFE2D9] overflow-hidden">
          <div className="p-6 border-b border-[#FFE9E3] flex justify-between items-center">
            <h3 className="text-xl font-semibold text-[#271812]">Danh sách quy tắc</h3>
            <button onClick={load} className="text-sm text-[#845143] hover:underline flex items-center gap-1">
              <span className="material-symbols-outlined text-base">refresh</span>
              Làm mới
            </button>
          </div>

          {loading ? (
            <div className="flex justify-center py-16">
              <div className="animate-spin rounded-full h-10 w-10 border-4 border-[#845143] border-t-transparent" />
            </div>
          ) : error ? (
            <div className="p-6 text-sm text-red-600">{error}</div>
          ) : visibleRules.length === 0 ? (
            <div className="flex flex-col items-center py-16 text-[#84736F]">
              <span className="material-symbols-outlined text-5xl mb-3">rule</span>
              <p className="font-medium">Không có quy tắc nào phù hợp</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-[#FFF1EC] border-b border-[#FFE9E3] text-[#84736F] text-xs uppercase tracking-wider">
                    <th className="p-4 pl-6 font-medium">Tên quy tắc</th>
                    <th className="p-4 font-medium">Mức độ</th>
                    <th className="p-4 font-medium">Hành động xử lý</th>
                    <th className="p-4 font-medium">Trạng thái</th>
                    <th className="p-4 font-medium">Cập nhật lúc</th>
                    <th className="p-4 pr-6 font-medium text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#FFE9E3]">
                  {visibleRules.map((rule) => (
                    <tr key={rule.id} className="hover:bg-[#FFF8F6] transition-colors">
                      <td className="p-4 pl-6">
                        <p className="font-medium text-[#271812]">{rule.keyword}</p>
                        {rule.isSystemDefault && (
                          <p className="text-xs text-[#84736F] flex items-center gap-1 mt-0.5">
                            <span className="material-symbols-outlined text-[14px]">lock</span> Mặc định hệ thống
                          </p>
                        )}
                      </td>
                      <td className="p-4">
                        <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${SEVERITY_STYLES[rule.severity]}`}>
                          {SEVERITY_LABELS[rule.severity]}
                        </span>
                      </td>
                      <td className="p-4 text-sm text-[#524440]">{ACTION_LABELS[rule.action]}</td>
                      <td className="p-4">
                        <button
                          onClick={() => handleToggleActive(rule)}
                          disabled={rule.isSystemDefault}
                          className={`flex items-center gap-2 text-sm ${rule.isSystemDefault ? 'cursor-not-allowed opacity-70' : 'cursor-pointer'}`}
                          title={rule.isSystemDefault ? 'Quy tắc mặc định luôn hoạt động' : 'Bấm để bật/tắt'}
                        >
                          <div className={`w-2 h-2 rounded-full ${rule.isActive ? 'bg-[#10b981]' : 'bg-[#D6C2BD]'}`} />
                          {rule.isActive ? 'Đang chạy' : 'Đã tắt'}
                        </button>
                      </td>
                      <td className="p-4 text-sm text-[#84736F]">{formatDateTime(rule.updatedAt)}</td>
                      <td className="p-4 pr-6 text-right">
                        <button
                          onClick={() => openEditModal(rule)}
                          className="w-10 h-10 inline-flex items-center justify-center text-[#84736F] hover:bg-[#FFF1EC] rounded-full transition-colors"
                        >
                          <span className="material-symbols-outlined">edit</span>
                        </button>
                        <button
                          onClick={() => handleDelete(rule)}
                          disabled={rule.isSystemDefault}
                          title={rule.isSystemDefault ? 'Không thể xóa quy tắc mặc định' : 'Xóa quy tắc'}
                          className={`w-10 h-10 inline-flex items-center justify-center rounded-full transition-colors ${
                            rule.isSystemDefault ? 'text-[#D6C2BD] cursor-not-allowed' : 'text-[#84736F] hover:bg-red-50 hover:text-red-600'
                          }`}
                        >
                          <span className="material-symbols-outlined">delete</span>
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

      {/* Create / Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={closeModal}>
          <div
            className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-bold text-[#271812] mb-4">
              {editingRule ? 'Chỉnh sửa quy tắc' : 'Tạo quy tắc mới'}
            </h3>

            <label className="block text-sm font-medium text-[#524440] mb-1">Từ khóa</label>
            <input
              value={form.keyword}
              onChange={(e) => setForm((f) => ({ ...f, keyword: e.target.value }))}
              placeholder="Ví dụ: chảy máu nhiều"
              className="w-full border border-[#D6C2BD] rounded-xl px-3 py-2 text-sm mb-4 focus:outline-none focus:ring-2 focus:ring-[#845143]/30"
            />

            <label className="block text-sm font-medium text-[#524440] mb-1">Mức độ</label>
            <select
              value={form.severity}
              onChange={(e) => setForm((f) => ({ ...f, severity: e.target.value as RedFlagSeverity }))}
              className="w-full border border-[#D6C2BD] rounded-xl px-3 py-2 text-sm mb-4"
            >
              <option value="GREEN">Bình thường</option>
              <option value="YELLOW">Cảnh báo</option>
              <option value="RED">Nghiêm trọng</option>
            </select>

            <label className="block text-sm font-medium text-[#524440] mb-1">Hành động xử lý</label>
            <select
              value={form.action}
              onChange={(e) => setForm((f) => ({ ...f, action: e.target.value as RedFlagAction }))}
              className="w-full border border-[#D6C2BD] rounded-xl px-3 py-2 text-sm mb-4"
            >
              <option value="WARN">Cảnh báo</option>
              <option value="BLOCK">Chặn</option>
              <option value="ESCALATE">Leo thang</option>
            </select>

            {formError && <p className="text-sm text-red-600 mb-3">{formError}</p>}

            <div className="flex gap-3 mt-2">
              <button
                onClick={closeModal}
                disabled={submitting}
                className="flex-1 h-11 rounded-full border border-[#D6C2BD] text-[#524440] font-medium hover:bg-[#FFF1EC] transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={handleSubmit}
                disabled={submitting}
                className="flex-1 h-11 rounded-full bg-[#845143] text-white font-semibold hover:opacity-90 disabled:opacity-50 transition-opacity"
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
