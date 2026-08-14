import { useEffect, useState, useCallback, useMemo } from 'react';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
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
  uploadPolicyDocument,
} from '../services/aiModerationPolicyApi';
import {
  AI_VIOLATION_CATEGORY_LABELS,
  AI_POLICY_SEVERITY_LABELS,
  AI_POLICY_SEVERITY_STYLES,
  REPORT_CATEGORY_LABELS,
  POLICY_TARGET_TYPE_LABELS,
  formatPolicyName,
  type AiPolicy,
  type AiViolationCategory,
  type AiPolicySeverity,
  type ReportCategory,
  type PolicyTargetType,
  type AiModerationStatus,
  type AiPolicyTestResult,
  type PolicyReferenceLink,
  type PolicyReferenceFile,
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
const MAX_POLICY_GUIDANCE_LENGTH = 3000;

interface PolicyFormState {
  policyCode: string;
  name: string;
  detectionGuidance: string;
  violationCategory: AiViolationCategory;
  reportCategory: ReportCategory;
  severity: AiPolicySeverity;
  applicableTargetTypes: PolicyTargetType[];
  confidenceThreshold: string;
  referenceLinks: PolicyReferenceLink[];
  referenceFiles: PolicyReferenceFile[];
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
  referenceLinks: [],
  referenceFiles: [],
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
  const [policyStatusTarget, setPolicyStatusTarget] = useState<AiPolicy | null>(null);
  const [policyStatusSubmitting, setPolicyStatusSubmitting] = useState(false);

  // Reference links & files state for modal inputs
  const [newLinkTitle, setNewLinkTitle] = useState('');
  const [newLinkUrl, setNewLinkUrl] = useState('');
  const [uploadingPolicyFile, setUploadingPolicyFile] = useState(false);
  const [policyFileError, setPolicyFileError] = useState('');

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
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Không tải được trạng thái Gemini.';
      setStatusError(msg);
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
    setNewLinkTitle('');
    setNewLinkUrl('');
    setPolicyFileError('');
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
      referenceLinks: policy.referenceLinks ? [...policy.referenceLinks] : [],
      referenceFiles: policy.referenceFiles ? [...policy.referenceFiles] : [],
    });
    setPolicyFormError('');
    setNewLinkTitle('');
    setNewLinkUrl('');
    setPolicyFileError('');
    setPolicyModalOpen(true);
  };

  const closePolicyModal = () => {
    if (policySubmitting || uploadingPolicyFile) return;
    setPolicyModalOpen(false);
  };

  const handleAddReferenceLink = () => {
    const url = newLinkUrl.trim();
    if (!url) return;
    try {
      new URL(url);
    } catch {
      setPolicyFormError('Định dạng URL không hợp lệ (ví dụ: https://thuvienphapluat.vn/...).');
      return;
    }
    const title = newLinkTitle.trim() || url;
    setPolicyForm((f) => ({
      ...f,
      referenceLinks: [...f.referenceLinks, { title, url }],
    }));
    setNewLinkTitle('');
    setNewLinkUrl('');
    setPolicyFormError('');
  };

  const handleRemoveReferenceLink = (index: number) => {
    setPolicyForm((f) => ({
      ...f,
      referenceLinks: f.referenceLinks.filter((_, i) => i !== index),
    }));
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingPolicyFile(true);
    setPolicyFileError('');
    try {
      const uploaded = await uploadPolicyDocument(file);
      setPolicyForm((f) => ({
        ...f,
        referenceFiles: [...f.referenceFiles, uploaded],
      }));
    } catch (err: any) {
      setPolicyFileError(err?.response?.data?.message ?? 'Upload tài liệu thất bại, vui lòng thử lại.');
    } finally {
      setUploadingPolicyFile(false);
      e.target.value = '';
    }
  };

  const handleRemoveReferenceFile = (index: number) => {
    setPolicyForm((f) => ({
      ...f,
      referenceFiles: f.referenceFiles.filter((_, i) => i !== index),
    }));
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
          referenceLinks: policyForm.referenceLinks,
          referenceFiles: policyForm.referenceFiles,
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
          referenceLinks: policyForm.referenceLinks,
          referenceFiles: policyForm.referenceFiles,
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
    const nextActive = !policy.active;
    setPolicyActionError('');
    setPolicyStatusSubmitting(true);
    try {
      await updateAiPolicyStatus(policy.id, nextActive);
      setPolicyStatusTarget(null);
      await loadPolicies();
      await loadAiStatus();
    } catch (err: any) {
      setPolicyActionError(err?.response?.data?.message ?? 'Có lỗi xảy ra khi cập nhật trạng thái.');
    } finally {
      setPolicyStatusSubmitting(false);
    }
  };

  const requestTogglePolicyActive = (policy: AiPolicy) => {
    if (policy.active) {
      setPolicyActionError('');
      setPolicyStatusTarget(policy);
      return;
    }
    void handleTogglePolicyActive(policy);
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
      <main className="font-sans">
        <div className="p-8">
          {/* Header */}
          <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <h1 className="text-[26px] font-bold text-on-surface m-0">AI & Chính sách an toàn</h1>
              <p className="text-on-surface-variant text-sm mt-1">
                Quản lý chính sách kiểm duyệt nội dung AI và từ khóa cảnh báo khẩn cấp y tế. AI chỉ hỗ trợ đánh giá và ưu tiên - quyết định xử lý cuối cùng bắt buộc phải do con người phê duyệt.
              </p>
            </div>
            <div className="flex items-center gap-2 self-start md:self-auto">
              {tab === 'AI_CONTENT' ? (
                <>
                  <button
                    type="button"
                    onClick={() => { void loadPolicies(); void loadAiStatus(); }}
                    disabled={policiesLoading}
                    className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-lg">refresh</span>
                    Làm mới
                  </button>
                  <button
                    type="button"
                    onClick={openCreatePolicyModal}
                    className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer hover:bg-primary/90"
                  >
                    <span className="material-symbols-outlined text-lg">add</span>
                    Tạo chính sách
                  </button>
                </>
              ) : (
                <>
                  <button
                    type="button"
                    onClick={() => void load()}
                    disabled={loading}
                    className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-lg">refresh</span>
                    Làm mới
                  </button>
                  <button
                    type="button"
                    onClick={openCreateModal}
                    className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer hover:bg-primary/90"
                  >
                    <span className="material-symbols-outlined text-lg">add</span>
                    Tạo quy tắc mới
                  </button>
                </>
              )}
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="mb-6 flex items-center gap-2 border-b border-surface-container-highest pb-3">
            {TABS.map((t) => (
              <button
                key={t.value}
                type="button"
                onClick={() => setTab(t.value)}
                className={`py-2 px-5 rounded-full text-sm font-semibold cursor-pointer transition-colors flex items-center gap-2 ${tab === t.value
                  ? 'bg-primary text-on-primary shadow-sm'
                  : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'
                  }`}
              >
                <span className="material-symbols-outlined text-lg">
                  {t.value === 'AI_CONTENT' ? 'smart_toy' : 'emergency'}
                </span>
                {t.label}
              </button>
            ))}
          </div>

          {tab === 'AI_CONTENT' && (
            <>
              {/* Gemini status - Stats Bar */}
              <div className="mb-6">
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="text-sm font-bold text-on-surface uppercase tracking-wider">Trạng thái Gemini AI</h3>
                  <button
                    type="button"
                    onClick={() => void loadAiStatus()}
                    className="flex items-center gap-1 text-xs font-semibold text-primary hover:underline cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-base">refresh</span>
                    Làm mới trạng thái
                  </button>
                </div>
                {statusError ? (
                  <div className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
                    {statusError}
                  </div>
                ) : (
                  <div className="grid gap-4 md:grid-cols-4">
                    <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                      <div>
                        <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Trạng thái</span>
                        {aiStatus ? (
                          <>
                            <span className={`inline-flex items-center rounded-full px-3 py-0.5 text-xs font-semibold ${AI_STATE_META[aiStatus.state].style}`}>
                              {AI_STATE_META[aiStatus.state].label}
                            </span>
                            <p className="mt-1 text-xs text-outline m-0">Model: {aiStatus.resolvedModel || aiStatus.model || '—'}</p>
                          </>
                        ) : (
                          <p className="text-sm text-on-surface-variant m-0">Đang tải...</p>
                        )}
                      </div>
                      <span className="material-symbols-outlined text-3xl text-primary/70">smart_toy</span>
                    </div>

                    <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                      <div>
                        <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Kiểm duyệt AI</span>
                        {aiStatus ? (
                          <>
                            <div className="flex items-center gap-2 text-sm font-bold text-on-surface">
                              <div className={`h-2.5 w-2.5 rounded-full ${aiStatus.businessToggleEnabled ? 'bg-emerald-500' : 'bg-outline-variant'}`} />
                              {aiStatus.businessToggleEnabled ? 'Đang bật' : 'Đang tắt'}
                            </div>
                            <p className="mt-1 text-xs text-outline m-0">Cấu hình hệ thống</p>
                          </>
                        ) : (
                          <p className="text-sm text-on-surface-variant m-0">Đang tải...</p>
                        )}
                      </div>
                      <span className="material-symbols-outlined text-3xl text-primary/70">toggle_on</span>
                    </div>

                    <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                      <div>
                        <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Job đang chờ</span>
                        {aiStatus ? (
                          <>
                            <p className="text-2xl font-bold text-on-surface m-0">{aiStatus.queuedJobs}</p>
                            <p className="mt-0.5 text-xs text-outline m-0">Đang xử lý: {aiStatus.processingJobs} · Lỗi: {aiStatus.failedJobs}</p>
                          </>
                        ) : (
                          <p className="text-sm text-on-surface-variant m-0">Đang tải...</p>
                        )}
                      </div>
                      <span className="material-symbols-outlined text-3xl text-primary/70">pending_actions</span>
                    </div>

                    <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                      <div>
                        <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Chính sách bật</span>
                        {aiStatus ? (
                          <>
                            <p className="text-2xl font-bold text-on-surface m-0">{aiStatus.activePolicies}</p>
                            <p className="mt-0.5 text-xs text-outline m-0">Áp dụng tự động</p>
                          </>
                        ) : (
                          <p className="text-sm text-on-surface-variant m-0">Đang tải...</p>
                        )}
                      </div>
                      <span className="material-symbols-outlined text-3xl text-primary/70">policy</span>
                    </div>
                  </div>
                )}
              </div>

              {/* Policy list Card */}
              <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest mb-6">
                <div className="flex items-center justify-between mb-4 pb-3 border-b border-surface-container-highest">
                  <div>
                    <h3 className="text-base font-bold text-on-surface m-0">Danh sách chính sách kiểm duyệt ({policiesTotal})</h3>
                    <p className="text-xs text-on-surface-variant mt-0.5">Các quy tắc AI dùng để quét bài viết, câu hỏi và phản hồi. Chính sách không xóa cứng; dùng Tắt để ngừng áp dụng và giữ lịch sử audit.</p>
                  </div>
                  <button
                    type="button"
                    onClick={openCreatePolicyModal}
                    className="py-2 px-4 rounded-full bg-primary text-on-primary text-xs font-semibold cursor-pointer hover:bg-primary/90 flex items-center gap-1"
                  >
                    <span className="material-symbols-outlined text-base">add</span>
                    Tạo chính sách
                  </button>
                </div>

                {policyActionError && (
                  <div className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
                    {policyActionError}
                  </div>
                )}

                {policiesLoading ? (
                  <div className="py-12 text-center text-outline">Đang tải danh sách chính sách...</div>
                ) : policiesError ? (
                  <div className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
                    {policiesError}
                  </div>
                ) : policies.length === 0 ? (
                  <div className="py-12 text-center text-outline">
                    <span className="material-symbols-outlined text-4xl block mb-2">policy</span>
                    Chưa có chính sách nào.
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full border-collapse">
                      <thead>
                        <tr className="border-b-2 border-surface-container-highest text-left">
                          {['MÃ CHÍNH SÁCH', 'TÊN CHÍNH SÁCH', 'DANH MỤC', 'MỨC ĐỘ', 'NGƯỠNG TIN CẬY', 'PHIÊN BẢN', 'TRẠNG THÁI', 'THAO TÁC'].map((heading, idx) => (
                            <th key={heading} className={`py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] ${idx === 7 ? 'text-right' : ''}`}>
                              {heading}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {policies.map((policy) => (
                          <tr key={policy.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                            <td className="py-3.5 px-2 font-mono text-xs font-semibold text-on-surface">
                              {policy.policyCode}
                            </td>
                            <td className="py-3.5 px-2 max-w-[320px]">
                              <div className="font-semibold text-sm text-on-surface">{policy.name}</div>
                              {policy.systemDefault && (
                                <div className="text-xs text-outline mt-0.5 flex items-center gap-1">
                                  <span className="material-symbols-outlined text-sm">lock</span> Mặc định hệ thống
                                </div>
                              )}
                              {((policy.referenceFiles && policy.referenceFiles.length > 0) ||
                                (policy.referenceLinks && policy.referenceLinks.length > 0)) && (
                                  <div className="flex flex-wrap items-center gap-1.5 mt-2">
                                    {policy.referenceFiles?.map((f, i) => (
                                      <a
                                        key={`file-${i}`}
                                        href={f.fileUrl}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200 transition-colors"
                                        title={`Tài liệu R2: ${f.fileName}`}
                                      >
                                        <span className="material-symbols-outlined text-[13px]">description</span>
                                        <span className="max-w-[120px] truncate">{f.fileName}</span>
                                      </a>
                                    ))}
                                    {policy.referenceLinks?.map((l, i) => (
                                      <a
                                        key={`link-${i}`}
                                        href={l.url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200 transition-colors"
                                        title={`Căn cứ pháp lý: ${l.url}`}
                                      >
                                        <span className="material-symbols-outlined text-[13px]">link</span>
                                        <span className="max-w-[120px] truncate">{l.title || 'Link căn cứ'}</span>
                                      </a>
                                    ))}
                                  </div>
                                )}
                            </td>
                            <td className="py-3.5 px-2 text-[13px] text-on-surface-variant">
                              {AI_VIOLATION_CATEGORY_LABELS[policy.violationCategory]}
                            </td>
                            <td className="py-3.5 px-2">
                              <span className={`inline-flex items-center rounded-full px-3 py-0.5 text-xs font-semibold ${AI_POLICY_SEVERITY_STYLES[policy.severity]}`}>
                                {AI_POLICY_SEVERITY_LABELS[policy.severity]}
                              </span>
                            </td>
                            <td className="py-3.5 px-2 text-[13px] font-semibold text-on-surface">
                              {Math.round(policy.confidenceThreshold * 100)}%
                            </td>
                            <td className="py-3.5 px-2 text-[13px] text-on-surface-variant">
                              v{policy.version}
                            </td>
                            <td className="py-3.5 px-2">
                              <div className="flex items-center gap-2 text-xs font-semibold text-on-surface-variant">
                                <div className={`h-2.5 w-2.5 rounded-full ${policy.active ? 'bg-emerald-500' : 'bg-outline-variant'}`} />
                                {policy.active ? 'Đang bật' : 'Đã tắt'}
                              </div>
                            </td>
                            <td className="py-3.5 px-2 text-right">
                              <div className="flex items-center gap-1.5 justify-end">
                                <button
                                  type="button"
                                  onClick={() => openEditPolicyModal(policy)}
                                  className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-surface text-xs font-semibold text-primary inline-flex items-center gap-1 hover:bg-surface-container-low cursor-pointer"
                                >
                                  <span className="material-symbols-outlined text-base">edit</span>
                                  Sửa
                                </button>
                                <button
                                  type="button"
                                  onClick={() => requestTogglePolicyActive(policy)}
                                  disabled={policyStatusSubmitting}
                                  title={policy.active ? 'Tắt chính sách thay cho xóa cứng' : 'Kích hoạt lại chính sách'}
                                  className={`h-8 py-1 px-3 rounded-lg border text-xs font-semibold inline-flex items-center gap-1 disabled:cursor-not-allowed disabled:opacity-50 ${policy.active
                                    ? 'border-error-container bg-surface text-error hover:bg-error-container cursor-pointer'
                                    : 'border-outline-variant bg-surface text-primary hover:bg-surface-container-low cursor-pointer'
                                    }`}
                                >
                                  <span className="material-symbols-outlined text-base">
                                    {policy.active ? 'block' : 'toggle_on'}
                                  </span>
                                  {policy.active ? 'Tắt' : 'Kích hoạt'}
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Sandbox Card */}
              <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
                <div className="flex items-center gap-2 mb-1">
                  <span className="material-symbols-outlined text-primary text-xl">science</span>
                  <h3 className="text-base font-bold text-on-surface m-0">Thử nghiệm chính sách (Sandbox)</h3>
                </div>
                <p className="text-xs text-on-surface-variant mb-4">Chạy phân loại thử trên văn bản mẫu với bộ chính sách hiện tại.</p>

                <div className="flex flex-col gap-3">
                  <select
                    value={testTargetType}
                    onChange={(e) => setTestTargetType(e.target.value as PolicyTargetType)}
                    className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none max-w-xs font-sans"
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
                    className="w-full p-3.5 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                  />
                  <div>
                    <button
                      type="button"
                      onClick={() => void handleRunTest()}
                      disabled={testLoading}
                      className="py-2.5 px-5 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer hover:bg-primary/90 disabled:opacity-50 inline-flex items-center gap-2"
                    >
                      <span className="material-symbols-outlined text-lg">science</span>
                      {testLoading ? 'Đang phân loại...' : 'Chạy phân loại thử'}
                    </button>
                  </div>
                </div>

                {testError && (
                  <div className="mt-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
                    {testError}
                  </div>
                )}

                {testResult && (
                  <div className="mt-4 rounded-2xl border border-surface-container-highest bg-surface-bright p-5">
                    <div className="flex flex-wrap items-center gap-3">
                      <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${CLASSIFICATION_META[testResult.classification].style}`}>
                        {CLASSIFICATION_META[testResult.classification].label}
                      </span>
                      {testResult.overallSeverity && (
                        <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${AI_POLICY_SEVERITY_STYLES[testResult.overallSeverity]}`}>
                          {AI_POLICY_SEVERITY_LABELS[testResult.overallSeverity]}
                        </span>
                      )}
                      {testResult.confidence != null && (
                        <span className="text-sm font-semibold text-on-surface">Độ tin cậy: {Math.round(testResult.confidence * 100)}%</span>
                      )}
                    </div>
                    {testResult.explanation && <p className="mt-3 text-sm text-on-surface-variant m-0">{testResult.explanation}</p>}
                    {testResult.recommendedAction && <p className="mt-1 text-xs text-outline m-0">Đề xuất: {testResult.recommendedAction}</p>}
                    {testResult.matches.length > 0 && (
                      <div className="mt-3 space-y-3">
                        {testResult.matches.map((match, i) => (
                          <div key={`${match.policyCode}-${i}`} className="rounded-2xl border border-surface-container-highest bg-surface p-4">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="font-mono text-xs font-bold text-on-surface">{formatPolicyName(match.policyCode, match.category)}</span>
                              <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${AI_POLICY_SEVERITY_STYLES[match.severity]}`}>
                                {AI_POLICY_SEVERITY_LABELS[match.severity]}
                              </span>
                              <span className="text-xs text-on-surface-variant">Độ tin cậy: {Math.round(match.confidence * 100)}%</span>
                            </div>
                            {match.evidence.length > 0 && (
                              <ul className="mt-2 space-y-1 pl-0 list-none">
                                {match.evidence.map((quote, qi) => (
                                  <li key={qi} className="border-l-2 border-primary/40 pl-3 py-0.5 text-xs italic text-on-surface-variant">
                                    “{quote}”
                                  </li>
                                ))}
                              </ul>
                            )}
                            {match.explanation && <p className="mt-2 text-xs text-outline m-0">{match.explanation}</p>}
                          </div>
                        ))}
                      </div>
                    )}
                    <p className="mt-3 text-sm font-bold text-on-surface m-0">
                      {testResult.wouldCreateCase
                        ? `Sẽ tạo case xem xét (ưu tiên: ${testResult.wouldCreatePriority ? PRIORITY_LABELS[testResult.wouldCreatePriority] : '—'})`
                        : 'Không tạo case'}
                    </p>
                    <p className="mt-1 text-xs text-outline m-0">Model: {testResult.model} · Độ trễ: {testResult.latencyMs}ms</p>
                  </div>
                )}
              </div>
            </>
          )}

          {tab === 'MEDICAL' && (
            <>
              {/* Action & Filter Bar */}
              <div className="bg-surface rounded-2xl p-4 shadow-sm border border-surface-container-highest mb-6">
                <div className="flex flex-col xl:flex-row items-center gap-3">
                  <div className="flex-1 w-full relative">
                    <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
                    <input
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      placeholder="Tìm kiếm quy tắc theo từ khóa..."
                      className="w-full py-2.5 pr-[14px] pl-[42px] rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                    />
                  </div>

                  <div className="flex flex-wrap md:flex-nowrap items-center gap-2 w-full xl:w-auto">
                    <select
                      value={severityFilter}
                      onChange={(e) => setSeverityFilter(e.target.value as RedFlagSeverity | '')}
                      className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                    >
                      <option value="">Tất cả mức độ</option>
                      <option value="RED">Nghiêm trọng</option>
                      <option value="YELLOW">Cảnh báo</option>
                      <option value="GREEN">Bình thường</option>
                    </select>

                    <select
                      value={activeFilter}
                      onChange={(e) => setActiveFilter(e.target.value as 'all' | 'active' | 'inactive')}
                      className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                    >
                      <option value="all">Tất cả trạng thái</option>
                      <option value="active">Đang chạy</option>
                      <option value="inactive">Đã tắt</option>
                    </select>

                    {(search || severityFilter || activeFilter !== 'all') && (
                      <button
                        type="button"
                        onClick={() => { setSearch(''); setSeverityFilter(''); setActiveFilter('all'); }}
                        className="py-2.5 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-on-surface-variant cursor-pointer hover:bg-surface-container-low flex items-center gap-1 whitespace-nowrap"
                      >
                        <span className="material-symbols-outlined text-base">filter_alt_off</span>
                        Xóa lọc
                      </button>
                    )}
                  </div>
                </div>
              </div>

              {/* Stats Bar */}
              <div className="mb-6 grid gap-4 md:grid-cols-3">
                <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Đang hoạt động</span>
                    <p className="text-2xl font-bold text-on-surface m-0">{stats.activeCount}</p>
                    <p className="text-xs text-outline mt-0.5 m-0">/ {totalElements} quy tắc</p>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-primary/70">rule</span>
                </div>

                <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Mức nghiêm trọng</span>
                    <p className="text-2xl font-bold text-error m-0">{stats.criticalCount}</p>
                    <p className="text-xs text-outline mt-0.5 m-0">Cần xem xét đánh giá</p>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-error/70">warning</span>
                </div>

                <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Mặc định hệ thống</span>
                    <p className="text-2xl font-bold text-on-surface m-0">{stats.systemDefaultCount}</p>
                    <p className="text-xs text-outline mt-0.5 m-0">Không thể xóa/tắt</p>
                  </div>
                  <span className="material-symbols-outlined text-3xl text-primary/70">verified_user</span>
                </div>
              </div>

              {/* Rule List Table Card */}
              <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
                <div className="flex items-center justify-between mb-4 pb-3 border-b border-surface-container-highest">
                  <h3 className="text-base font-bold text-on-surface m-0">Danh sách quy tắc cảnh báo y tế</h3>
                  <button
                    type="button"
                    onClick={() => void load()}
                    className="flex items-center gap-1 text-xs font-semibold text-primary hover:underline cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-base">refresh</span>
                    Làm mới
                  </button>
                </div>

                {loading ? (
                  <div className="py-12 text-center text-outline">Đang tải danh sách quy tắc...</div>
                ) : error ? (
                  <div className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
                    {error}
                  </div>
                ) : visibleRules.length === 0 ? (
                  <div className="py-12 text-center text-outline">
                    <span className="material-symbols-outlined text-4xl block mb-2">rule</span>
                    Không có quy tắc nào phù hợp.
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full border-collapse">
                      <thead>
                        <tr className="border-b-2 border-surface-container-highest text-left">
                          {['TỪ KHÓA QUY TẮC', 'MỨC ĐỘ', 'HÀNH ĐỘNG XỬ LÝ', 'TRẠNG THÁI', 'CẬP NHẬT LÚC', 'THAO TÁC'].map((heading, idx) => (
                            <th key={heading} className={`py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] ${idx === 5 ? 'text-right' : ''}`}>
                              {heading}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {visibleRules.map((rule) => (
                          <tr key={rule.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                            <td className="py-3.5 px-2 max-w-[300px]">
                              <div className="font-semibold text-sm text-on-surface">{rule.keyword}</div>
                              {rule.isSystemDefault && (
                                <div className="text-xs text-outline mt-0.5 flex items-center gap-1">
                                  <span className="material-symbols-outlined text-sm">lock</span> Mặc định hệ thống
                                </div>
                              )}
                            </td>
                            <td className="py-3.5 px-2">
                              <span className={`inline-flex items-center rounded-full px-3 py-0.5 text-xs font-semibold ${SEVERITY_STYLES[rule.severity]}`}>
                                {SEVERITY_LABELS[rule.severity]}
                              </span>
                            </td>
                            <td className="py-3.5 px-2 text-[13px] text-on-surface-variant font-medium">
                              {ACTION_LABELS[rule.action]}
                            </td>
                            <td className="py-3.5 px-2">
                              <button
                                type="button"
                                onClick={() => void handleToggleActive(rule)}
                                disabled={rule.isSystemDefault}
                                className={`flex items-center gap-2 text-xs font-semibold text-on-surface-variant bg-transparent border-0 ${rule.isSystemDefault ? 'cursor-not-allowed opacity-70' : 'cursor-pointer'
                                  }`}
                                title={rule.isSystemDefault ? 'Quy tắc mặc định luôn hoạt động' : 'Bấm để bật/tắt'}
                              >
                                <div className={`h-2.5 w-2.5 rounded-full ${rule.isActive ? 'bg-emerald-500' : 'bg-outline-variant'}`} />
                                {rule.isActive ? 'Đang chạy' : 'Đã tắt'}
                              </button>
                            </td>
                            <td className="py-3.5 px-2 text-[13px] text-outline whitespace-nowrap">
                              {formatDateTime(rule.updatedAt)}
                            </td>
                            <td className="py-3.5 px-2 text-right">
                              <div className="flex items-center gap-1.5 justify-end">
                                <button
                                  type="button"
                                  onClick={() => openEditModal(rule)}
                                  className="h-8 w-8 rounded-full border border-outline-variant bg-surface text-outline flex items-center justify-center hover:bg-surface-container-low hover:text-primary cursor-pointer"
                                  title="Sửa quy tắc"
                                >
                                  <span className="material-symbols-outlined text-base">edit</span>
                                </button>
                                <button
                                  type="button"
                                  onClick={() => void handleDelete(rule)}
                                  disabled={rule.isSystemDefault}
                                  title={rule.isSystemDefault ? 'Không thể xóa quy tắc mặc định' : 'Xóa quy tắc'}
                                  className={`h-8 w-8 rounded-full border border-outline-variant flex items-center justify-center cursor-pointer ${rule.isSystemDefault
                                    ? 'opacity-40 cursor-not-allowed text-outline-variant bg-surface'
                                    : 'bg-surface text-outline hover:bg-error-container hover:text-error hover:border-error-container'
                                    }`}
                                >
                                  <span className="material-symbols-outlined text-base">delete</span>
                                </button>
                              </div>
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm" onClick={closeModal}>
          <div
            className="w-full max-w-md rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-2xl font-sans"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-5 pb-3 border-b border-surface-container-highest">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">rule</span>
                <h3 className="text-lg font-bold text-on-surface m-0">
                  {editingRule ? 'Chỉnh sửa quy tắc y tế' : 'Tạo quy tắc mới'}
                </h3>
              </div>
              <button
                type="button"
                onClick={closeModal}
                disabled={submitting}
                className="w-8 h-8 rounded-full border border-outline-variant bg-surface text-outline flex items-center justify-center hover:bg-surface-container-low cursor-pointer"
              >
                <span className="material-symbols-outlined text-base">close</span>
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Từ khóa quy tắc</label>
                <input
                  value={form.keyword}
                  onChange={(e) => setForm((f) => ({ ...f, keyword: e.target.value }))}
                  placeholder="Ví dụ: chảy máu nhiều, sốt cao liên tục..."
                  className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Mức độ nghiêm trọng</label>
                <select
                  value={form.severity}
                  onChange={(e) => setForm((f) => ({ ...f, severity: e.target.value as RedFlagSeverity }))}
                  className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans cursor-pointer"
                >
                  <option value="GREEN">Bình thường (Green)</option>
                  <option value="YELLOW">Cảnh báo (Yellow)</option>
                  <option value="RED">Nghiêm trọng (Red)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Hành động xử lý</label>
                <select
                  value={form.action}
                  onChange={(e) => setForm((f) => ({ ...f, action: e.target.value as RedFlagAction }))}
                  className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans cursor-pointer"
                >
                  <option value="WARN">Cảnh báo cho người dùng</option>
                  <option value="BLOCK">Chặn xuất bản</option>
                  <option value="ESCALATE">Leo thang xem xét</option>
                </select>
              </div>
            </div>

            {formError && (
              <div className="mt-4 rounded-2xl border border-error-container bg-error-container/60 p-3 text-xs text-error font-medium">
                {formError}
              </div>
            )}

            <div className="flex items-center gap-3 mt-6 pt-4 border-t border-surface-container-highest">
              <button
                type="button"
                onClick={closeModal}
                disabled={submitting}
                className="flex-1 py-2.5 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-on-surface-variant cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={() => void handleSubmit()}
                disabled={submitting}
                className="flex-1 py-2.5 px-4 rounded-full bg-primary text-on-primary text-xs font-semibold cursor-pointer hover:bg-primary/90 disabled:opacity-50"
              >
                {submitting ? 'Đang lưu...' : editingRule ? 'Lưu thay đổi' : 'Tạo quy tắc'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create / Edit Modal (AI policies) */}
      {policyModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4 backdrop-blur-sm" onClick={closePolicyModal}>
          <div
            className="w-full max-w-lg rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-2xl max-h-[90vh] overflow-y-auto font-sans"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-5 pb-3 border-b border-surface-container-highest">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-xl">policy</span>
                <h3 className="text-lg font-bold text-on-surface m-0">
                  {editingPolicy ? 'Chỉnh sửa chính sách AI' : 'Tạo chính sách AI mới'}
                </h3>
              </div>
              <button
                type="button"
                onClick={closePolicyModal}
                disabled={policySubmitting}
                className="w-8 h-8 rounded-full border border-outline-variant bg-surface text-outline flex items-center justify-center hover:bg-surface-container-low cursor-pointer"
              >
                <span className="material-symbols-outlined text-base">close</span>
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Mã chính sách</label>
                <input
                  value={policyForm.policyCode}
                  onChange={(e) => setPolicyForm((f) => ({ ...f, policyCode: e.target.value.toUpperCase() }))}
                  disabled={!!editingPolicy}
                  placeholder="Ví dụ: SPAM_LINK_BAIT"
                  className={`w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-mono ${editingPolicy ? 'cursor-not-allowed opacity-70 bg-surface-container-low' : ''
                    }`}
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Tên chính sách</label>
                <input
                  value={policyForm.name}
                  onChange={(e) => setPolicyForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="Ví dụ: Spam liên kết quảng cáo"
                  className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Hướng dẫn nhận diện (AI Prompt Guidance)</label>
                <textarea
                  value={policyForm.detectionGuidance}
                  onChange={(e) => setPolicyForm((f) => ({ ...f, detectionGuidance: e.target.value }))}
                  maxLength={MAX_POLICY_GUIDANCE_LENGTH}
                  rows={3}
                  className="w-full p-3 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                />
                <div className="mt-1 flex items-center justify-between gap-3 text-xs text-outline">
                  <p className="m-0">Đây là dữ liệu hướng dẫn phân loại AI có kiểm soát.</p>
                  <span aria-live="polite" className="shrink-0 tabular-nums">
                    {policyForm.detectionGuidance.length.toLocaleString('vi-VN')} / {MAX_POLICY_GUIDANCE_LENGTH.toLocaleString('vi-VN')} ký tự
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Danh mục vi phạm</label>
                  <select
                    value={policyForm.violationCategory}
                    onChange={(e) => setPolicyForm((f) => ({ ...f, violationCategory: e.target.value as AiViolationCategory }))}
                    className="w-full py-2.5 px-3 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans cursor-pointer"
                  >
                    {(Object.keys(AI_VIOLATION_CATEGORY_LABELS) as AiViolationCategory[]).map((c) => (
                      <option key={c} value={c}>{AI_VIOLATION_CATEGORY_LABELS[c]}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Danh mục báo cáo</label>
                  <select
                    value={policyForm.reportCategory}
                    onChange={(e) => setPolicyForm((f) => ({ ...f, reportCategory: e.target.value as ReportCategory }))}
                    className="w-full py-2.5 px-3 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans cursor-pointer"
                  >
                    {(Object.keys(REPORT_CATEGORY_LABELS) as ReportCategory[]).map((c) => (
                      <option key={c} value={c}>{REPORT_CATEGORY_LABELS[c]}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Mức độ nghiêm trọng</label>
                  <select
                    value={policyForm.severity}
                    onChange={(e) => setPolicyForm((f) => ({ ...f, severity: e.target.value as AiPolicySeverity }))}
                    className="w-full py-2.5 px-3 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans cursor-pointer"
                  >
                    {(Object.keys(AI_POLICY_SEVERITY_LABELS) as AiPolicySeverity[]).map((s) => (
                      <option key={s} value={s}>{AI_POLICY_SEVERITY_LABELS[s]}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1.5">Ngưỡng tin cậy (0 - 1)</label>
                  <input
                    type="number"
                    min={0}
                    max={1}
                    step={0.05}
                    value={policyForm.confidenceThreshold}
                    onChange={(e) => setPolicyForm((f) => ({ ...f, confidenceThreshold: e.target.value }))}
                    className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-2">Áp dụng cho đối tượng nội dung</label>
                <div className="flex flex-wrap gap-2">
                  {(Object.keys(POLICY_TARGET_TYPE_LABELS) as PolicyTargetType[]).map((t) => {
                    const isChecked = policyForm.applicableTargetTypes.includes(t);
                    return (
                      <button
                        key={t}
                        type="button"
                        onClick={() => togglePolicyTargetType(t)}
                        className={`py-1.5 px-3.5 rounded-full text-xs font-semibold cursor-pointer transition-colors flex items-center gap-1.5 ${isChecked
                          ? 'bg-primary text-on-primary'
                          : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'
                          }`}
                      >
                        <span className="material-symbols-outlined text-base">
                          {isChecked ? 'check_circle' : 'radio_button_unchecked'}
                        </span>
                        {POLICY_TARGET_TYPE_LABELS[t]}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Reference Links & Evidence Documents Section */}
              <div className="pt-4 border-t border-surface-container-highest space-y-4">
                <div className="flex items-center gap-1.5 text-xs font-bold text-on-surface uppercase tracking-wider">
                  <span className="material-symbols-outlined text-base text-primary">verified</span>
                  Căn cứ pháp lý & Tài liệu bằng chứng thực tế
                </div>

                {/* 1. Attached Links */}
                <div>
                  <label className="block text-[11px] font-semibold text-outline mb-1.5">Gắn liên kết tham chiếu (Website / Luật / Nghị định)</label>
                  <div className="flex gap-2 mb-2">
                    <input
                      type="text"
                      placeholder="Tiêu đề (ví dụ: Luật KCB 2023 - Điều 12)"
                      value={newLinkTitle}
                      onChange={(e) => setNewLinkTitle(e.target.value)}
                      className="flex-1 py-2 px-3 rounded-xl border border-outline-variant bg-surface text-xs text-on-surface outline-none"
                    />
                    <input
                      type="url"
                      placeholder="URL (https://thuvienphapluat.vn/...)"
                      value={newLinkUrl}
                      onChange={(e) => setNewLinkUrl(e.target.value)}
                      className="flex-1 py-2 px-3 rounded-xl border border-outline-variant bg-surface text-xs text-on-surface outline-none"
                    />
                    <button
                      type="button"
                      onClick={handleAddReferenceLink}
                      className="py-2 px-3.5 rounded-xl bg-surface-container-high hover:bg-surface-container-highest text-primary font-semibold text-xs flex items-center gap-1 cursor-pointer transition-colors"
                    >
                      <span className="material-symbols-outlined text-sm">add_link</span>
                      Thêm
                    </button>
                  </div>

                  {policyForm.referenceLinks.length > 0 && (
                    <div className="space-y-1.5 max-h-36 overflow-y-auto pr-1">
                      {policyForm.referenceLinks.map((link, idx) => (
                        <div key={idx} className="flex items-center justify-between p-2 rounded-xl bg-surface-container-low border border-outline-variant/60 text-xs">
                          <div className="flex items-center gap-2 truncate mr-2">
                            <span className="material-symbols-outlined text-purple-600 text-sm">link</span>
                            <span className="font-semibold text-on-surface truncate">{link.title}</span>
                            <a href={link.url} target="_blank" rel="noopener noreferrer" className="text-outline hover:underline truncate max-w-[200px]">
                              {link.url}
                            </a>
                          </div>
                          <button
                            type="button"
                            onClick={() => handleRemoveReferenceLink(idx)}
                            className="text-error hover:bg-error-container/50 p-1 rounded-lg transition-colors cursor-pointer"
                          >
                            <span className="material-symbols-outlined text-sm">delete</span>
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* 2. Upload Document Files to Cloudflare R2 */}
                <div>
                  <label className="block text-[11px] font-semibold text-outline mb-1.5">Import tài liệu bằng chứng (.PDF, .DOC, .DOCX, Ảnh)</label>
                  <div className="flex items-center gap-3">
                    <label className={`py-2 px-4 rounded-xl border border-dashed border-primary bg-primary/5 text-primary text-xs font-semibold flex items-center gap-2 cursor-pointer hover:bg-primary/10 transition-colors ${uploadingPolicyFile ? 'opacity-50 cursor-not-allowed' : ''}`}>
                      <span className="material-symbols-outlined text-base">cloud_upload</span>
                      {uploadingPolicyFile ? 'Đang upload lên R2...' : 'Tải tài liệu lên'}
                      <input
                        type="file"
                        accept=".pdf,.doc,.docx,image/*"
                        disabled={uploadingPolicyFile}
                        onChange={(e) => void handleFileUpload(e)}
                        className="hidden"
                      />
                    </label>
                    <span className="text-[11px] text-outline">Tối đa 20MB / file. Lưu trữ bảo mật trên R2.</span>
                  </div>

                  {policyFileError && (
                    <p className="mt-1 text-xs text-error font-medium">{policyFileError}</p>
                  )}

                  {policyForm.referenceFiles.length > 0 && (
                    <div className="mt-2 space-y-1.5 max-h-36 overflow-y-auto pr-1">
                      {policyForm.referenceFiles.map((file, idx) => (
                        <div key={idx} className="flex items-center justify-between p-2 rounded-xl bg-surface-container-low border border-outline-variant/60 text-xs">
                          <div className="flex items-center gap-2 truncate mr-2">
                            <span className="material-symbols-outlined text-blue-600 text-sm">description</span>
                            <span className="font-semibold text-on-surface truncate">{file.fileName}</span>
                            <span className="text-outline text-[11px]">
                              ({Math.round((file.fileSizeBytes || 0) / 1024)} KB)
                            </span>
                            {file.fileUrl && (
                              <a href={file.fileUrl} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline text-[11px] flex items-center gap-0.5">
                                Xem
                                <span className="material-symbols-outlined text-xs">open_in_new</span>
                              </a>
                            )}
                          </div>
                          <button
                            type="button"
                            onClick={() => handleRemoveReferenceFile(idx)}
                            className="text-error hover:bg-error-container/50 p-1 rounded-lg transition-colors cursor-pointer"
                          >
                            <span className="material-symbols-outlined text-sm">delete</span>
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {policyFormError && (
              <div className="mt-4 rounded-2xl border border-error-container bg-error-container/60 p-3 text-xs text-error font-medium">
                {policyFormError}
              </div>
            )}

            <div className="flex items-center gap-3 mt-6 pt-4 border-t border-surface-container-highest">
              <button
                type="button"
                onClick={closePolicyModal}
                disabled={policySubmitting}
                className="flex-1 py-2.5 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-on-surface-variant cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={() => void handlePolicySubmit()}
                disabled={policySubmitting}
                className="flex-1 py-2.5 px-4 rounded-full bg-primary text-on-primary text-xs font-semibold cursor-pointer hover:bg-primary/90 disabled:opacity-50"
              >
                {policySubmitting ? 'Đang lưu...' : editingPolicy ? 'Lưu thay đổi' : 'Tạo chính sách'}
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        key={policyStatusTarget?.id ?? 'ai-policy-status'}
        open={policyStatusTarget !== null}
        title="Tắt chính sách AI?"
        description={
          policyStatusTarget
            ? `Chính sách "${policyStatusTarget.name}" sẽ ngừng tham gia phân loại AI. Hệ thống vẫn giữ lại lịch sử và audit để có thể truy vết hoặc kích hoạt lại sau.`
            : undefined
        }
        icon="block"
        tone="danger"
        confirmLabel="Tắt chính sách"
        submitting={policyStatusSubmitting}
        errorText={policyStatusTarget ? policyActionError : ''}
        onConfirm={() => policyStatusTarget && void handleTogglePolicyActive(policyStatusTarget)}
        onCancel={() => {
          if (policyStatusSubmitting) return;
          setPolicyStatusTarget(null);
          setPolicyActionError('');
        }}
      />
    </div>
  );
}
