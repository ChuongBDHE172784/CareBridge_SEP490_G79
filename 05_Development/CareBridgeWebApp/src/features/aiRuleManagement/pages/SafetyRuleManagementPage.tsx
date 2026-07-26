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
import {
  fetchAiPolicies,
  createAiPolicy,
  updateAiPolicy,
  updateAiPolicyStatus,
  fetchAiModerationStatus,
  testAiPolicy,
} from '../services/aiModerationPolicyApi';
import {
  AI_VIOLATION_CATEGORY_LABELS,
  AI_POLICY_SEVERITY_LABELS,
  AI_POLICY_SEVERITY_STYLES,
  REPORT_CATEGORY_LABELS,
  POLICY_TARGET_TYPE_LABELS,
  type AiPolicy,
  type AiViolationCategory,
  type AiPolicySeverity,
  type ReportCategory,
  type PolicyTargetType,
  type AiModerationStatus,
  type AiPolicyTestResult,
} from '../models/aiModerationPolicy';

type TabKey = 'AI_CONTENT' | 'MEDICAL';

const TABS: { value: TabKey; label: string }[] = [
  { value: 'AI_CONTENT', label: 'Chính sách kiểm duyệt nội dung AI' },
  { value: 'MEDICAL', label: 'Cảnh báo khẩn cấp y tế' },
];

interface RuleFormState {
  keyword: string;
  severity: RedFlagSeverity;
  action: RedFlagAction;
}

const DEFAULT_FORM: RuleFormState = { keyword: '', severity: 'YELLOW', action: 'WARN' };

interface PolicyFormState {
  policyCode: string;
  name: string;
  detectionGuidance: string;
  violationCategory: AiViolationCategory;
  reportCategory: ReportCategory;
  severity: AiPolicySeverity;
  applicableTargetTypes: PolicyTargetType[];
  confidenceThreshold: string;
}

const DEFAULT_POLICY_FORM: PolicyFormState = {
  policyCode: '',
  name: '',
  detectionGuidance: '',
  violationCategory: 'SPAM_ADVERTISING',
  reportCategory: 'OTHER',
  severity: 'MEDIUM',
  applicableTargetTypes: ['QUESTION', 'ANSWER'],
  confidenceThreshold: '0.7',
};

const AI_STATE_META: Record<AiModerationStatus['state'], { label: string; style: string }> = {
  READY: { label: 'Sẵn sàng', style: 'bg-emerald-100 text-emerald-700' },
  NOT_CONFIGURED: { label: 'Thiếu API key', style: 'bg-amber-100 text-amber-700' },
  DISABLED: { label: 'Đang tắt', style: 'bg-surface-container-high text-on-surface-variant' },
};

const CLASSIFICATION_META: Record<AiPolicyTestResult['classification'], { label: string; style: string }> = {
  SAFE: { label: 'An toàn', style: 'bg-emerald-100 text-emerald-700' },
  VIOLATION: { label: 'Nghi vấn vi phạm', style: 'bg-error-container text-error' },
  UNCERTAIN: { label: 'Chưa chắc chắn', style: 'bg-amber-100 text-amber-700' },
};

const PRIORITY_LABELS: Record<'NORMAL' | 'HIGH' | 'URGENT', string> = {
  NORMAL: 'Bình thường',
  HIGH: 'Cao',
  URGENT: 'Khẩn cấp',
};

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export default function SafetyRuleManagementPage() {
  const [tab, setTab] = useState<TabKey>('AI_CONTENT');

  // ── Medical red-flag rules (existing) ─────────────────────────────────────
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

  // ── AI content moderation policies ────────────────────────────────────────
  const [policies, setPolicies] = useState<AiPolicy[]>([]);
  const [policiesTotal, setPoliciesTotal] = useState(0);
  const [policiesLoading, setPoliciesLoading] = useState(true);
  const [policiesError, setPoliciesError] = useState('');
  const [policyActionError, setPolicyActionError] = useState('');

  const [aiStatus, setAiStatus] = useState<AiModerationStatus | null>(null);
  const [statusError, setStatusError] = useState('');

  const [policyModalOpen, setPolicyModalOpen] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<AiPolicy | null>(null);
  const [policyForm, setPolicyForm] = useState<PolicyFormState>(DEFAULT_POLICY_FORM);
  const [policyFormError, setPolicyFormError] = useState('');
  const [policySubmitting, setPolicySubmitting] = useState(false);

  const [testTargetType, setTestTargetType] = useState<PolicyTargetType>('QUESTION');
  const [testText, setTestText] = useState('');
  const [testLoading, setTestLoading] = useState(false);
  const [testError, setTestError] = useState('');
  const [testResult, setTestResult] = useState<AiPolicyTestResult | null>(null);

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

  const loadPolicies = useCallback(async () => {
    setPoliciesLoading(true);
    setPoliciesError('');
    try {
      const page = await fetchAiPolicies({ page: 0, size: 50 });
      setPolicies(page.content);
      setPoliciesTotal(page.totalElements);
    } catch {
      setPoliciesError('Không tải được danh sách chính sách.');
      setPolicies([]);
    } finally {
      setPoliciesLoading(false);
    }
  }, []);

  const loadAiStatus = useCallback(async () => {
    setStatusError('');
    try {
      setAiStatus(await fetchAiModerationStatus());
    } catch {
      setStatusError('Không tải được trạng thái Gemini.');
      setAiStatus(null);
    }
  }, []);

  useEffect(() => { loadPolicies(); loadAiStatus(); }, [loadPolicies, loadAiStatus]);

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

  // ── AI policy handlers ────────────────────────────────────────────────────
  const openCreatePolicyModal = () => {
    setEditingPolicy(null);
    setPolicyForm(DEFAULT_POLICY_FORM);
    setPolicyFormError('');
    setPolicyModalOpen(true);
  };

  const openEditPolicyModal = (policy: AiPolicy) => {
    setEditingPolicy(policy);
    setPolicyForm({
      policyCode: policy.policyCode,
      name: policy.name,
      detectionGuidance: policy.detectionGuidance,
      violationCategory: policy.violationCategory,
      reportCategory: policy.reportCategory,
      severity: policy.severity,
      applicableTargetTypes: [...policy.applicableTargetTypes],
      confidenceThreshold: String(policy.confidenceThreshold),
    });
    setPolicyFormError('');
    setPolicyModalOpen(true);
  };

  const closePolicyModal = () => {
    if (policySubmitting) return;
    setPolicyModalOpen(false);
  };

  const togglePolicyTargetType = (target: PolicyTargetType) => {
    setPolicyForm((f) => ({
      ...f,
      applicableTargetTypes: f.applicableTargetTypes.includes(target)
        ? f.applicableTargetTypes.filter((t) => t !== target)
        : [...f.applicableTargetTypes, target],
    }));
  };

  const handlePolicySubmit = async () => {
    const code = policyForm.policyCode.trim();
    if (!editingPolicy && !/^[A-Z0-9_]{3,60}$/.test(code)) {
      setPolicyFormError('Mã chính sách phải gồm 3-60 ký tự A-Z, 0-9 hoặc dấu gạch dưới.');
      return;
    }
    if (!policyForm.name.trim()) {
      setPolicyFormError('Vui lòng nhập tên chính sách.');
      return;
    }
    if (!policyForm.detectionGuidance.trim()) {
      setPolicyFormError('Vui lòng nhập hướng dẫn nhận diện.');
      return;
    }
    if (policyForm.applicableTargetTypes.length === 0) {
      setPolicyFormError('Chọn ít nhất một loại nội dung áp dụng.');
      return;
    }
    const threshold = Number(policyForm.confidenceThreshold);
    if (!Number.isFinite(threshold) || threshold < 0 || threshold > 1) {
      setPolicyFormError('Ngưỡng tin cậy phải nằm trong khoảng 0 đến 1.');
      return;
    }
    setPolicySubmitting(true);
    setPolicyFormError('');
    try {
      if (editingPolicy) {
        await updateAiPolicy(editingPolicy.id, {
          name: policyForm.name.trim(),
          detectionGuidance: policyForm.detectionGuidance.trim(),
          violationCategory: policyForm.violationCategory,
          reportCategory: policyForm.reportCategory,
          severity: policyForm.severity,
          applicableTargetTypes: policyForm.applicableTargetTypes,
          confidenceThreshold: threshold,
        });
      } else {
        await createAiPolicy({
          policyCode: code,
          name: policyForm.name.trim(),
          detectionGuidance: policyForm.detectionGuidance.trim(),
          violationCategory: policyForm.violationCategory,
          reportCategory: policyForm.reportCategory,
          severity: policyForm.severity,
          applicableTargetTypes: policyForm.applicableTargetTypes,
          confidenceThreshold: threshold,
          active: true,
        });
      }
      setPolicyModalOpen(false);
      await loadPolicies();
      await loadAiStatus();
    } catch (err: any) {
      const errCode = err?.response?.data?.error;
      if (errCode === 'AIM-002') setPolicyFormError('Mã chính sách đã tồn tại.');
      else setPolicyFormError(err?.response?.data?.message ?? 'Có lỗi xảy ra, vui lòng thử lại.');
    } finally {
      setPolicySubmitting(false);
    }
  };

  const handleTogglePolicyActive = async (policy: AiPolicy) => {
    setPolicyActionError('');
    try {
      await updateAiPolicyStatus(policy.id, !policy.active);
      await loadPolicies();
      await loadAiStatus();
    } catch (err: any) {
      setPolicyActionError(err?.response?.data?.message ?? 'Có lỗi xảy ra khi cập nhật trạng thái.');
    }
  };

  const handleRunTest = async () => {
    if (!testText.trim()) {
      setTestError('Vui lòng nhập văn bản mẫu.');
      return;
    }
    setTestLoading(true);
    setTestError('');
    setTestResult(null);
    try {
      setTestResult(await testAiPolicy({ targetType: testTargetType, sampleText: testText }));
    } catch (err: any) {
      const errCode = err?.response?.data?.error;
      if (errCode === 'AIM-009') setTestError('Gemini chưa được bật hoặc chưa cấu hình API key.');
      else setTestError(err?.response?.data?.message ?? 'Có lỗi xảy ra, vui lòng thử lại.');
    } finally {
      setTestLoading(false);
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
            <h2 className="portal-title">AI & Chính sách an toàn</h2>
            <p className="portal-subtitle">Quản lý chính sách kiểm duyệt nội dung AI và từ khóa cảnh báo khẩn cấp y tế. AI chỉ hỗ trợ đánh giá và ưu tiên — quyết định cuối cùng luôn thuộc về con người.</p>
          </div>
        </div>

        {/* Tabs */}
        <div className="portal-toolbar">
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => setTab(t.value)}
              className={`rounded-md px-3 py-2 text-sm font-semibold transition-colors ${
                tab === t.value ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {tab === 'AI_CONTENT' && (
        <>
        {/* Gemini status */}
        <div className="mb-5">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-on-surface">Trạng thái Gemini</h3>
            <button onClick={loadAiStatus} className="flex items-center gap-1 text-xs font-semibold text-primary hover:underline">
              <span className="material-symbols-outlined text-base">refresh</span>
              Làm mới
            </button>
          </div>
          {statusError ? (
            <div className="portal-error">{statusError}</div>
          ) : (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
              <div className="portal-stat-card">
                <div className="flex items-center gap-3 mb-3">
                  <div className="portal-icon">
                    <span className="material-symbols-outlined text-lg">smart_toy</span>
                  </div>
                  <h3 className="text-sm font-semibold text-on-surface">Trạng thái</h3>
                </div>
                {aiStatus ? (
                  <>
                    <span className={`inline-flex items-center rounded px-2 py-1 text-xs font-medium ${AI_STATE_META[aiStatus.state].style}`}>
                      {AI_STATE_META[aiStatus.state].label}
                    </span>
                    <p className="mt-2 text-xs text-outline">Model: {aiStatus.model || '—'}</p>
                  </>
                ) : (
                  <p className="text-sm text-on-surface-variant">Đang tải...</p>
                )}
              </div>
              <div className="portal-stat-card">
                <div className="flex items-center gap-3 mb-3">
                  <div className="portal-icon">
                    <span className="material-symbols-outlined text-lg">toggle_on</span>
                  </div>
                  <h3 className="text-sm font-semibold text-on-surface">Kiểm duyệt tự động AI</h3>
                </div>
                {aiStatus ? (
                  <>
                    <div className="flex items-center gap-2 text-sm text-on-surface">
                      <div className={`h-2 w-2 rounded-full ${aiStatus.businessToggleEnabled ? 'bg-emerald-500' : 'bg-outline-variant'}`} />
                      {aiStatus.businessToggleEnabled ? 'Đang bật' : 'Đang tắt'}
                    </div>
                    <p className="mt-2 text-xs text-outline">Theo cấu hình hệ thống</p>
                  </>
                ) : (
                  <p className="text-sm text-on-surface-variant">Đang tải...</p>
                )}
              </div>
              <div className="portal-stat-card">
                <div className="flex items-center gap-3 mb-3">
                  <div className="portal-icon">
                    <span className="material-symbols-outlined text-lg">pending_actions</span>
                  </div>
                  <h3 className="text-sm font-semibold text-on-surface">Job đang chờ</h3>
                </div>
                {aiStatus ? (
                  <>
                    <p className="portal-metric text-primary">{aiStatus.queuedJobs}</p>
                    <p className="mt-1 text-xs text-outline">Đang xử lý: {aiStatus.processingJobs} · Thất bại: {aiStatus.failedJobs}</p>
                    <p className="mt-0.5 text-xs text-outline">Hoàn tất gần nhất: {aiStatus.lastCompletedAt ? formatDateTime(aiStatus.lastCompletedAt) : '—'}</p>
                  </>
                ) : (
                  <p className="text-sm text-on-surface-variant">Đang tải...</p>
                )}
              </div>
              <div className="portal-stat-card">
                <div className="flex items-center gap-3 mb-3">
                  <div className="portal-icon">
                    <span className="material-symbols-outlined text-lg">policy</span>
                  </div>
                  <h3 className="text-sm font-semibold text-on-surface">Chính sách đang bật</h3>
                </div>
                {aiStatus ? (
                  <>
                    <p className="portal-metric text-primary">{aiStatus.activePolicies}</p>
                    <p className="mt-1 text-xs text-outline">Áp dụng khi phân loại nội dung</p>
                  </>
                ) : (
                  <p className="text-sm text-on-surface-variant">Đang tải...</p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Policy list */}
        <div className="portal-table-card mb-5">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-outline-variant/70 p-4">
            <h3 className="text-sm font-semibold text-on-surface">Danh sách chính sách ({policiesTotal})</h3>
            <div className="flex items-center gap-3">
              <button onClick={loadPolicies} className="flex items-center gap-1 text-xs font-semibold text-primary hover:underline">
                <span className="material-symbols-outlined text-base">refresh</span>
                Làm mới
              </button>
              <button onClick={openCreatePolicyModal} className="portal-primary-button">
                <span className="material-symbols-outlined text-base">add</span>
                Tạo chính sách
              </button>
            </div>
          </div>

          {policyActionError && <div className="m-4 portal-error">{policyActionError}</div>}

          {policiesLoading ? (
            <div className="flex justify-center py-16">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          ) : policiesError ? (
            <div className="m-4 portal-error">{policiesError}</div>
          ) : policies.length === 0 ? (
            <div className="portal-empty m-4">
              <span className="material-symbols-outlined mb-2 block text-4xl">policy</span>
              <p className="font-medium">Chưa có chính sách nào</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table>
                <thead>
                  <tr>
                    <th className="pl-4">Mã</th>
                    <th>Tên chính sách</th>
                    <th>Danh mục</th>
                    <th>Mức độ</th>
                    <th>Ngưỡng tin cậy</th>
                    <th>Phiên bản</th>
                    <th>Trạng thái</th>
                    <th className="pr-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {policies.map((policy) => (
                    <tr key={policy.id}>
                      <td className="pl-4">
                        <span className="font-mono text-xs font-medium text-on-surface">{policy.policyCode}</span>
                      </td>
                      <td>
                        <p className="font-medium text-on-surface">{policy.name}</p>
                        {policy.systemDefault && (
                          <p className="mt-0.5 flex items-center gap-1 text-xs text-outline">
                            <span className="material-symbols-outlined text-[14px]">lock</span> Mặc định hệ thống
                          </p>
                        )}
                      </td>
                      <td className="text-on-surface-variant">{AI_VIOLATION_CATEGORY_LABELS[policy.violationCategory]}</td>
                      <td>
                        <span className={`inline-flex items-center rounded px-2 py-1 text-xs font-medium ${AI_POLICY_SEVERITY_STYLES[policy.severity]}`}>
                          {AI_POLICY_SEVERITY_LABELS[policy.severity]}
                        </span>
                      </td>
                      <td className="text-on-surface-variant">{Math.round(policy.confidenceThreshold * 100)}%</td>
                      <td className="text-on-surface-variant">v{policy.version}</td>
                      <td>
                        <button
                          onClick={() => handleTogglePolicyActive(policy)}
                          className="flex cursor-pointer items-center gap-2 text-sm text-on-surface-variant"
                          title="Bấm để bật/tắt"
                        >
                          <div className={`h-2 w-2 rounded-full ${policy.active ? 'bg-emerald-500' : 'bg-outline-variant'}`} />
                          {policy.active ? 'Đang bật' : 'Đã tắt'}
                        </button>
                      </td>
                      <td className="pr-4 text-right">
                        <button
                          onClick={() => openEditPolicyModal(policy)}
                          className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold text-primary hover:bg-surface-container-low"
                        >
                          <span className="material-symbols-outlined text-base">edit</span>
                          Sửa
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Sandbox */}
        <div className="portal-card-padded">
          <h3 className="mb-1 text-sm font-semibold text-on-surface">Thử nghiệm chính sách</h3>
          <p className="mb-3 text-xs text-on-surface-variant">Chạy phân loại thử trên văn bản mẫu với bộ chính sách hiện tại.</p>
          <div className="flex flex-col gap-3">
            <select
              value={testTargetType}
              onChange={(e) => setTestTargetType(e.target.value as PolicyTargetType)}
              className="portal-field w-full max-w-xs"
            >
              {(Object.keys(POLICY_TARGET_TYPE_LABELS) as PolicyTargetType[]).map((t) => (
                <option key={t} value={t}>{POLICY_TARGET_TYPE_LABELS[t]}</option>
              ))}
            </select>
            <textarea
              value={testText}
              onChange={(e) => setTestText(e.target.value)}
              maxLength={5000}
              rows={4}
              placeholder="Dán văn bản mẫu để phân loại thử. Không dán thông tin cá nhân thật."
              className="portal-field w-full"
            />
            <div>
              <button onClick={handleRunTest} disabled={testLoading} className="portal-primary-button">
                <span className="material-symbols-outlined text-base">science</span>
                {testLoading ? 'Đang phân loại...' : 'Chạy phân loại thử'}
              </button>
            </div>
          </div>

          {testError && <div className="mt-3 portal-error">{testError}</div>}

          {testResult && (
            <div className="mt-4 rounded-lg border border-outline-variant/70 bg-surface-container-low p-4">
              <div className="flex flex-wrap items-center gap-3">
                <span className={`inline-flex items-center rounded px-2 py-1 text-xs font-medium ${CLASSIFICATION_META[testResult.classification].style}`}>
                  {CLASSIFICATION_META[testResult.classification].label}
                </span>
                {testResult.overallSeverity && (
                  <span className={`inline-flex items-center rounded px-2 py-1 text-xs font-medium ${AI_POLICY_SEVERITY_STYLES[testResult.overallSeverity]}`}>
                    {AI_POLICY_SEVERITY_LABELS[testResult.overallSeverity]}
                  </span>
                )}
                {testResult.confidence != null && (
                  <span className="text-sm text-on-surface">Độ tin cậy: {Math.round(testResult.confidence * 100)}%</span>
                )}
              </div>
              {testResult.explanation && <p className="mt-2 text-sm text-on-surface-variant">{testResult.explanation}</p>}
              {testResult.recommendedAction && <p className="mt-1 text-xs text-outline">Đề xuất: {testResult.recommendedAction}</p>}
              {testResult.matches.length > 0 && (
                <div className="mt-3 space-y-3">
                  {testResult.matches.map((match, i) => (
                    <div key={`${match.policyCode}-${i}`} className="rounded-md border border-outline-variant/70 bg-surface p-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-on-surface">{match.policyCode}</span>
                        <span className={`inline-flex items-center rounded px-2 py-0.5 text-xs font-medium ${AI_POLICY_SEVERITY_STYLES[match.severity]}`}>
                          {AI_POLICY_SEVERITY_LABELS[match.severity]}
                        </span>
                        <span className="text-xs text-on-surface-variant">Độ tin cậy: {Math.round(match.confidence * 100)}%</span>
                      </div>
                      {match.evidence.length > 0 && (
                        <ul className="mt-2 space-y-1">
                          {match.evidence.map((quote, qi) => (
                            <li key={qi} className="border-l-2 border-outline-variant pl-2 text-xs italic text-on-surface-variant">
                              “{quote}”
                            </li>
                          ))}
                        </ul>
                      )}
                      {match.explanation && <p className="mt-2 text-xs text-outline">{match.explanation}</p>}
                    </div>
                  ))}
                </div>
              )}
              <p className="mt-3 text-sm font-medium text-on-surface">
                {testResult.wouldCreateCase
                  ? `Sẽ tạo case xem xét (ưu tiên: ${testResult.wouldCreatePriority ? PRIORITY_LABELS[testResult.wouldCreatePriority] : '—'})`
                  : 'Không tạo case'}
              </p>
              <p className="mt-1 text-xs text-outline">Model: {testResult.model} · Độ trễ: {testResult.latencyMs}ms</p>
            </div>
          )}

          <p className="mt-3 text-xs text-outline">Kết quả chỉ mang tính hỗ trợ. Không lưu lại nội dung mẫu.</p>
        </div>
        </>
        )}

        {tab === 'MEDICAL' && (
        <>
        {/* Section intro */}
        <div className="mb-4 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h3 className="text-base font-semibold text-on-surface">Cảnh báo khẩn cấp y tế (Triage)</h3>
            <p className="mt-1 max-w-2xl text-sm text-on-surface-variant">Từ khóa nhận diện tình huống khẩn cấp y tế để điều hướng cấp cứu kịp thời — KHÔNG phải bộ lọc vi phạm nội dung. Người dùng mô tả triệu chứng của chính mình không phải là người vi phạm.</p>
          </div>
          <button
            onClick={openCreateModal}
            className="portal-primary-button shrink-0"
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
        </>
        )}
        </div>
      </main>

      {/* Create / Edit Modal (red-flag rules) */}
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

      {/* Create / Edit Modal (AI policies) */}
      {policyModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 p-4 backdrop-blur-[3px]" onClick={closePolicyModal}>
          <div
            className="portal-modal-panel max-h-[90vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="mb-4 text-sm font-semibold text-on-surface">
              {editingPolicy ? 'Chỉnh sửa chính sách' : 'Tạo chính sách mới'}
            </h3>

            <label className="portal-label">Mã chính sách</label>
            <input
              value={policyForm.policyCode}
              onChange={(e) => setPolicyForm((f) => ({ ...f, policyCode: e.target.value.toUpperCase() }))}
              disabled={!!editingPolicy}
              placeholder="Ví dụ: SPAM_LINK_BAIT"
              className={`portal-field mb-4 w-full font-mono ${editingPolicy ? 'cursor-not-allowed opacity-70' : ''}`}
            />

            <label className="portal-label">Tên chính sách</label>
            <input
              value={policyForm.name}
              onChange={(e) => setPolicyForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="Ví dụ: Spam liên kết quảng cáo"
              className="portal-field mb-4 w-full"
            />

            <label className="portal-label">Hướng dẫn nhận diện</label>
            <textarea
              value={policyForm.detectionGuidance}
              onChange={(e) => setPolicyForm((f) => ({ ...f, detectionGuidance: e.target.value }))}
              maxLength={2000}
              rows={4}
              className="portal-field mb-1 w-full"
            />
            <p className="mb-4 text-xs text-outline">Đây là dữ liệu có kiểm soát, không phải system prompt tự do.</p>

            <label className="portal-label">Danh mục vi phạm</label>
            <select
              value={policyForm.violationCategory}
              onChange={(e) => setPolicyForm((f) => ({ ...f, violationCategory: e.target.value as AiViolationCategory }))}
              className="portal-field mb-4 w-full"
            >
              {(Object.keys(AI_VIOLATION_CATEGORY_LABELS) as AiViolationCategory[]).map((c) => (
                <option key={c} value={c}>{AI_VIOLATION_CATEGORY_LABELS[c]}</option>
              ))}
            </select>

            <label className="portal-label">Danh mục báo cáo</label>
            <select
              value={policyForm.reportCategory}
              onChange={(e) => setPolicyForm((f) => ({ ...f, reportCategory: e.target.value as ReportCategory }))}
              className="portal-field mb-4 w-full"
            >
              {(Object.keys(REPORT_CATEGORY_LABELS) as ReportCategory[]).map((c) => (
                <option key={c} value={c}>{REPORT_CATEGORY_LABELS[c]}</option>
              ))}
            </select>

            <label className="portal-label">Mức độ</label>
            <select
              value={policyForm.severity}
              onChange={(e) => setPolicyForm((f) => ({ ...f, severity: e.target.value as AiPolicySeverity }))}
              className="portal-field mb-4 w-full"
            >
              {(Object.keys(AI_POLICY_SEVERITY_LABELS) as AiPolicySeverity[]).map((s) => (
                <option key={s} value={s}>{AI_POLICY_SEVERITY_LABELS[s]}</option>
              ))}
            </select>

            <label className="portal-label">Áp dụng cho</label>
            <div className="mb-4 flex flex-wrap gap-4">
              {(Object.keys(POLICY_TARGET_TYPE_LABELS) as PolicyTargetType[]).map((t) => (
                <label key={t} className="flex items-center gap-2 text-sm text-on-surface">
                  <input
                    type="checkbox"
                    checked={policyForm.applicableTargetTypes.includes(t)}
                    onChange={() => togglePolicyTargetType(t)}
                  />
                  {POLICY_TARGET_TYPE_LABELS[t]}
                </label>
              ))}
            </div>

            <label className="portal-label">Ngưỡng tin cậy (0 – 1)</label>
            <input
              type="number"
              min={0}
              max={1}
              step={0.05}
              value={policyForm.confidenceThreshold}
              onChange={(e) => setPolicyForm((f) => ({ ...f, confidenceThreshold: e.target.value }))}
              className="portal-field mb-4 w-full"
            />

            {policyFormError && <p className="mb-3 text-sm text-error">{policyFormError}</p>}

            <div className="flex gap-3 mt-2">
              <button
                onClick={closePolicyModal}
                disabled={policySubmitting}
                className="portal-secondary-button flex-1"
              >
                Hủy
              </button>
              <button
                onClick={handlePolicySubmit}
                disabled={policySubmitting}
                className="portal-primary-button flex-1"
              >
                {policySubmitting ? 'Đang lưu...' : editingPolicy ? 'Lưu thay đổi' : 'Tạo chính sách'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
