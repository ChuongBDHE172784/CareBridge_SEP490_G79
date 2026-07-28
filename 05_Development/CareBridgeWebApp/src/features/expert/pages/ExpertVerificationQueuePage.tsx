import { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  Award,
  BadgeCheck,
  Building2,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Download,
  Eye,
  FileCheck,
  FileText,
  Filter,
  Info,
  Loader2,
  RefreshCw,
  Search,
  ShieldCheck,
  Sparkles,
  User,
  X,
  XCircle,
} from 'lucide-react';
import {
  approveExpert,
  getCredentialDocumentPreview,
  getCredentialFileUrl,
  getExpertReviewCases,
  getIdentityFileUrl,
  rejectExpert,
  reviewCredential,
  reviewIdentity,
  setExpertTrust,
  type CredentialDocumentPreviewResponse,
  type DocumentReviewResponse,
  type ExpertReviewCaseResponse,
} from '../services/expertApi';

type TrustStatus = 'ACTIVE' | 'SUSPENDED' | 'REVOKED';
type ReviewDecision = 'APPROVED' | 'REJECTED';

const statusConfig: Record<
  string,
  { label: string; bg: string; text: string; border: string; dot: string }
> = {
  ACTIVE: {
    label: 'Hoạt động',
    bg: 'bg-[#d6f0ec]',
    text: 'text-[#0f5a53]',
    border: 'border-[#95d2ca]',
    dot: 'bg-[#0f766e]',
  },
  APPROVED: {
    label: 'Đã duyệt',
    bg: 'bg-[#d6f0ec]',
    text: 'text-[#0f5a53]',
    border: 'border-[#95d2ca]',
    dot: 'bg-[#0f766e]',
  },
  MANUAL_REVIEW_REQUIRED: {
    label: 'Cần duyệt thủ công',
    bg: 'bg-[#fef3c7]',
    text: 'text-[#92400e]',
    border: 'border-[#fde68a]',
    dot: 'bg-[#d97706]',
  },
  MISSING: {
    label: 'Chưa nộp',
    bg: 'bg-[#f1f5f9]',
    text: 'text-[#475569]',
    border: 'border-[#cbd5e1]',
    dot: 'bg-[#94a3b8]',
  },
  PENDING: {
    label: 'Chờ duyệt',
    bg: 'bg-[#fef3c7]',
    text: 'text-[#92400e]',
    border: 'border-[#fde68a]',
    dot: 'bg-[#d97706] animate-pulse',
  },
  PENDING_REVIEW: {
    label: 'Chờ duyệt',
    bg: 'bg-[#fef3c7]',
    text: 'text-[#92400e]',
    border: 'border-[#fde68a]',
    dot: 'bg-[#d97706] animate-pulse',
  },
  REJECTED: {
    label: 'Từ chối',
    bg: 'bg-[#ffe4e6]',
    text: 'text-[#9f1239]',
    border: 'border-[#fecdd3]',
    dot: 'bg-[#e11d48]',
  },
  REVOKED: {
    label: 'Thu hồi',
    bg: 'bg-[#ffe4e6]',
    text: 'text-[#9f1239]',
    border: 'border-[#fecdd3]',
    dot: 'bg-[#e11d48]',
  },
  SUSPENDED: {
    label: 'Tạm ngưng',
    bg: 'bg-[#ffedd5]',
    text: 'text-[#9a3412]',
    border: 'border-[#fed7aa]',
    dot: 'bg-[#ea580c]',
  },
};

const credentialLabels: Record<string, string> = {
  CERTIFICATE: 'Chứng chỉ đào tạo chuyên môn',
  DEGREE: 'Bằng cấp học vị / Đại học',
  IDENTITY_DOCUMENT: 'Giấy tờ định danh cá nhân',
  MEDICAL_LICENSE: 'Chứng chỉ hành nghề y tế',
  PROFESSIONAL_LICENSE: 'Giấy phép hành nghề chuyên môn',
};

function StatusBadge({ value }: { value?: string | null }) {
  const normalized = value || 'MISSING';
  const cfg = statusConfig[normalized] || {
    label: normalized.replaceAll('_', ' '),
    bg: 'bg-[#f1f5f9]',
    text: 'text-[#475569]',
    border: 'border-[#cbd5e1]',
    dot: 'bg-[#94a3b8]',
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold shadow-2xs transition-colors ${cfg.bg} ${cfg.text} ${cfg.border}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${cfg.dot}`} />
      {cfg.label}
    </span>
  );
}

// Lightbox modal for high-res photo viewing
function ImageLightbox({
  src,
  title,
  onClose,
}: {
  src: string;
  title: string;
  onClose: () => void;
}) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 transition-all duration-200 animate-in fade-in"
      onClick={onClose}
    >
      <div
        className="relative max-h-[90vh] max-w-4xl overflow-hidden rounded-2xl bg-white border border-[#d5dde2] shadow-2xl p-2"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-4 py-2 border-b border-[#eef3f6] text-[#172126]">
          <span className="font-bold text-sm flex items-center gap-2 text-[#0f766e]">
            <Eye className="w-4 h-4 text-[#0f766e]" />
            {title}
          </span>
          <button
            onClick={onClose}
            className="rounded-full p-1 text-[#6b7882] hover:text-[#172126] hover:bg-[#eef3f6] transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-4 flex items-center justify-center bg-[#f7f9fb]">
          <img
            src={src}
            alt={title}
            className="max-h-[75vh] max-w-full rounded-xl object-contain shadow-md"
          />
        </div>
      </div>
    </div>
  );
}

function DocumentModal({
  credential,
  onClose,
}: {
  credential: DocumentReviewResponse;
  onClose: () => void;
}) {
  const [preview, setPreview] = useState<CredentialDocumentPreviewResponse | null>(null);
  const [fileUrl, setFileUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const mime = credential.mimeType ?? '';
  const isImage = mime.startsWith('image/');
  const isPdf = mime === 'application/pdf';

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    setFileUrl(null);
    const tasks: Promise<unknown>[] = [
      getCredentialFileUrl(credential.credentialId)
        .then((url) => active && setFileUrl(url))
        .catch(() => active && setError('Không thể tải bản gốc của tài liệu.')),
    ];
    if (!isImage && !isPdf) {
      tasks.push(
        getCredentialDocumentPreview(credential.credentialId)
          .then((result) => active && setPreview(result))
          .catch(() =>
            active && setError('Không thể trích xuất nội dung. Vui lòng tải bản gốc để kiểm tra.'),
          ),
      );
    }
    Promise.allSettled(tasks).finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [credential.credentialId, isImage, isPdf]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4 animate-in fade-in"
      onClick={onClose}
    >
      <div
        className="flex max-h-[92vh] w-full max-w-5xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl border border-[#d5dde2]"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="flex items-center justify-between gap-4 border-b border-[#eef3f6] px-6 py-4 bg-[#f8fa0]">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#d6f0ec] text-[#0f766e]">
              <FileText className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <h2 className="truncate font-bold text-[#172126] text-base">
                {credential.fileName || 'Tài liệu chứng chỉ'}
              </h2>
              <p className="text-xs text-[#42515a] flex items-center gap-2">
                <span>{credentialLabels[credential.credentialType] || credential.credentialType}</span>
                <span>•</span>
                <span>{mime || 'Định dạng không rõ'}</span>
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {fileUrl && (
              <a
                className="inline-flex items-center gap-1.5 rounded-xl bg-[#0f766e] px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-[#0d665f] transition"
                href={fileUrl}
                download={credential.fileName ?? true}
              >
                <Download className="w-3.5 h-3.5" />
                Tải bản gốc
              </a>
            )}
            <button
              className="flex h-8 w-8 items-center justify-center rounded-full text-[#6b7882] hover:bg-[#eef3f6] transition"
              onClick={onClose}
              aria-label="Đóng"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </header>

        <div className="flex-1 overflow-auto bg-[#f7f9fb] p-6">
          {loading && (
            <div className="flex flex-col items-center justify-center py-20 text-[#6b7882]">
              <Loader2 className="w-8 h-8 animate-spin text-[#0f766e] mb-2" />
              <p className="text-sm">Đang tải xem trước tài liệu…</p>
            </div>
          )}

          {!loading && isImage && fileUrl && (
            <div className="flex justify-center">
              <img
                className="max-h-[72vh] max-w-full rounded-xl object-contain shadow-lg border border-[#d5dde2]"
                src={fileUrl}
                alt="Chứng chỉ"
              />
            </div>
          )}

          {!loading && isPdf && fileUrl && (
            <iframe
              className="h-[68vh] w-full rounded-xl border border-[#d5dde2] bg-white shadow-inner"
              src={fileUrl}
              title="PDF chứng chỉ"
            />
          )}

          {!loading && !isImage && !isPdf && (
            <section className="rounded-2xl border border-[#d5dde2] bg-white p-6 shadow-xs">
              <h3 className="mb-4 font-bold text-[#172126] flex items-center gap-2">
                <FileCheck className="w-5 h-5 text-[#0f766e]" />
                Nội dung tài liệu trích xuất (Tika Isolation)
              </h3>
              {error && (
                <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700 mb-4 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  {error}
                </div>
              )}
              {preview && (
                <>
                  <pre className="whitespace-pre-wrap break-words rounded-xl bg-[#f8fafc] p-4 font-mono text-xs leading-relaxed text-[#172126] max-h-[55vh] overflow-y-auto border border-[#d5dde2]">
                    {preview.content || 'Tài liệu không chứa nội dung văn bản có thể trích xuất.'}
                  </pre>
                  {preview.truncated && (
                    <p className="mt-3 text-xs text-amber-700 flex items-center gap-1.5">
                      <Info className="w-4 h-4" />
                      Nội dung văn bản quá dài nên đã được thu gọn bản xem trước.
                    </p>
                  )}
                </>
              )}
            </section>
          )}
        </div>
      </div>
    </div>
  );
}

export default function ExpertVerificationQueuePage() {
  const [cases, setCases] = useState<ExpertReviewCaseResponse[]>([]);
  const [selectedProfileId, setSelectedProfileId] = useState<string | null>(null);
  const [identityUrls, setIdentityUrls] = useState<Record<string, string>>({});
  const [selectedDocument, setSelectedDocument] = useState<DocumentReviewResponse | null>(null);
  const [lightbox, setLightbox] = useState<{ src: string; title: string } | null>(null);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [reasons, setReasons] = useState({
    identity: '',
    credential: '',
    profile: '',
  });
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadCases = useCallback(
    async (keepProfileId?: string | null) => {
      setLoading(true);
      setError(null);
      try {
        const result = await getExpertReviewCases(keyword, statusFilter, page, 10);
        const caseList = Array.isArray(result)
          ? result
          : (result?.content ?? []);
        const total =
          result?.page?.totalPages ??
          (result as unknown as { totalPages?: number })?.totalPages ??
          1;
        setCases(caseList);
        setTotalPages(total);
        const requested = keepProfileId ?? selectedProfileId;
        const nextSelected = caseList.some(
          (item) => item?.profile?.expertProfileId === requested,
        )
          ? requested
          : caseList[0]?.profile?.expertProfileId ?? null;
        setSelectedProfileId(nextSelected);
      } catch (err) {
        console.error('Error loading cases:', err);
        setError('Không thể tải trung tâm xét duyệt chuyên gia.');
      } finally {
        setLoading(false);
      }
    },
    [keyword, statusFilter, page, selectedProfileId],
  );

  useEffect(() => {
    void loadCases(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, statusFilter]);

  const filtered = cases;
  const selected = cases.find((item) => item.profile?.expertProfileId === selectedProfileId) ?? null;

  useEffect(() => {
    if (
      filtered.length > 0 &&
      !filtered.some((item) => item.profile?.expertProfileId === selectedProfileId)
    ) {
      setSelectedProfileId(filtered[0]?.profile?.expertProfileId ?? null);
    }
  }, [filtered, selectedProfileId]);

  useEffect(() => {
    if (window.location.pathname.endsWith('/expert-identity-queue')) {
      const el = window.document.getElementById('identity-verification-section');
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    } else if (window.location.pathname.endsWith('/expert-trust-management')) {
      const el = window.document.getElementById('trust-management-section');
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    }
  }, [selectedProfileId]);

  useEffect(() => {
    setSelectedDocument(null);
    setReasons({ identity: '', credential: '', profile: '' });
  }, [selectedProfileId]);

  useEffect(() => {
    const identity = selected?.latestIdentity;
    setIdentityUrls({});
    if (!identity) {
      return;
    }
    const entries = [
      ['selfie', identity.selfieFileId],
      ['front', identity.identityFrontFileId],
      ['back', identity.identityBackFileId],
    ].filter((entry): entry is [string, string] => Boolean(entry[1]));
    let active = true;
    Promise.allSettled(
      entries.map(
        async ([key, fileId]) => [key, await getIdentityFileUrl(fileId)] as const,
      ),
    ).then((results) => {
      if (!active) return;
      const urls = results
        .filter(
          (result): result is PromiseFulfilledResult<readonly [string, string]> =>
            result.status === 'fulfilled',
        )
        .map((result) => result.value);
      setIdentityUrls(Object.fromEntries(urls));
    });
    return () => {
      active = false;
    };
  }, [selected?.latestIdentity]);

  const requireRejectReason = (decision: ReviewDecision, reason: string) => {
    if (decision === 'REJECTED' && !reason.trim()) {
      setError('Vui lòng nhập lý do từ chối cụ thể trước khi thực hiện.');
      return false;
    }
    return true;
  };

  const runAction = async (key: string, work: () => Promise<unknown>) => {
    setAction(key);
    setError(null);
    try {
      await work();
      setReasons({ identity: '', credential: '', profile: '' });
      await loadCases(selectedProfileId);
    } catch (caught: unknown) {
      const apiError = caught as {
        response?: { status?: number; data?: { message?: string } };
      };
      if (apiError.response?.status === 409) {
        setError('Hồ sơ đã được xử lý bởi một quản trị viên khác. Vui lòng làm mới danh sách.');
      } else {
        setError(apiError.response?.data?.message ?? 'Thao tác không thành công.');
      }
    } finally {
      setAction(null);
    }
  };

  const decideIdentity = (decision: ReviewDecision) => {
    const identityId =
      selected?.latestIdentity?.identityVerificationId ?? selected?.latestIdentity?.attemptId;
    if (!identityId || !requireRejectReason(decision, reasons.identity)) return;
    void runAction(`identity-${decision}`, () =>
      reviewIdentity(identityId, decision, reasons.identity.trim()),
    );
  };

  const decideCredential = (credentialId: string, decision: ReviewDecision) => {
    if (!requireRejectReason(decision, reasons.credential)) return;
    void runAction(`credential-${credentialId}`, () =>
      reviewCredential(credentialId, {
        reviewStatus: decision,
        reviewNote: reasons.credential.trim() || undefined,
      }),
    );
  };

  const decideProfile = (decision: ReviewDecision) => {
    if (!selected || !requireRejectReason(decision, reasons.profile)) return;
    void runAction(`profile-${decision}`, () =>
      decision === 'APPROVED'
        ? approveExpert(selected.profile.expertProfileId)
        : rejectExpert(selected.profile.expertProfileId, reasons.profile.trim()),
    );
  };

  const changeTrust = (status: TrustStatus) => {
    if (!selected) return;
    if (
      (status === 'SUSPENDED' || status === 'REVOKED') &&
      !window.confirm(`Xác nhận chuyển trạng thái tin cậy sang ${statusConfig[status]?.label}?`)
    ) {
      return;
    }
    void runAction(`trust-${status}`, () =>
      setExpertTrust(selected.profile.expertProfileId, status),
    );
  };

  return (
    <main className="min-h-screen bg-[#f7f9fb] p-4 lg:p-8 font-sans text-[#172126]">
      <div className="mx-auto max-w-[1600px] space-y-6">
        {/* Banner Header - Light CareBridge Theme */}
        <header className="relative overflow-hidden rounded-2xl bg-white border border-[#d5dde2] p-6 md:p-8 shadow-xs">
          <div className="relative z-10 flex flex-col md:flex-row md:items-center md:justify-between gap-6">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full bg-[#d6f0ec] px-3 py-1 text-xs font-semibold text-[#0f5a53] border border-[#95d2ca] mb-3">
                <Sparkles className="w-3.5 h-3.5 text-[#0f766e]" />
                Cổng quản trị hệ thống CareBridge
              </div>
              <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-[#172126]">
                Trung tâm xét duyệt chuyên gia
              </h1>
              <p className="mt-1 text-sm text-[#42515a] max-w-2xl leading-relaxed">
                Định danh, chứng chỉ, quyết định cuối và trạng thái tin cậy trong cùng một màn hình.
              </p>
            </div>

            {/* Quick Metrics */}
            <div className="flex items-center gap-3">
              <div className="rounded-xl bg-[#f6f8fa] border border-[#d5dde2] px-4 py-3 text-center min-w-[110px]">
                <p className="text-2xl font-bold text-[#0f766e]">{filtered.length}</p>
                <p className="text-xs text-[#6b7882] font-medium">Hồ sơ hiển thị</p>
              </div>
              <div className="rounded-xl bg-[#f6f8fa] border border-[#d5dde2] px-4 py-3 text-center min-w-[110px]">
                <p className="text-2xl font-bold text-[#d97706]">
                  {totalPages > 0 ? `${page + 1}/${totalPages}` : '1'}
                </p>
                <p className="text-xs text-[#6b7882] font-medium">Trang hiện tại</p>
              </div>
            </div>
          </div>
        </header>

        {/* Filter Controls Bar */}
        <div className="flex flex-col md:flex-row items-center justify-between gap-3 rounded-2xl border border-[#d5dde2] bg-white p-3.5 shadow-xs">
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-[#6b7882]" />
            <input
              className="w-full rounded-xl border border-[#d5dde2] bg-[#f7f9fb] pl-10 pr-4 py-2.5 text-sm text-[#172126] placeholder-[#6b7882] focus:border-[#0f766e] focus:outline-none focus:ring-2 focus:ring-[#0f766e]/10 transition"
              placeholder="Tìm theo tên chuyên gia, chuyên khoa, chức danh…"
              value={keyword}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  setPage(0);
                  loadCases(selectedProfileId);
                }
              }}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>

          <div className="flex items-center gap-2 w-full md:w-auto">
            <div className="relative flex-1 md:w-56">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#6b7882]" />
              <select
                className="w-full appearance-none rounded-xl border border-[#d5dde2] bg-[#f7f9fb] pl-9 pr-8 py-2.5 text-sm font-medium text-[#172126] focus:border-[#0f766e] focus:outline-none focus:ring-2 focus:ring-[#0f766e]/10 transition cursor-pointer"
                value={statusFilter}
                onChange={(event) => {
                  setPage(0);
                  setStatusFilter(event.target.value);
                }}
              >
                <option value="PENDING">Đang chờ xét duyệt</option>
                <option value="">Tất cả chuyên gia</option>
                <option value="APPROVED">Đã duyệt</option>
                <option value="REJECTED">Đã từ chối</option>
                <option value="SUSPENDED">Đang tạm ngưng</option>
                <option value="REVOKED">Đã thu hồi</option>
              </select>
            </div>

            <button
              className="inline-flex items-center gap-2 rounded-xl bg-[#0f766e] px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-[#0d665f] active:scale-95 transition"
              onClick={() => {
                setPage(0);
                loadCases(selectedProfileId);
              }}
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              <span>Làm mới</span>
            </button>
          </div>
        </div>

        {/* Global Error Banner */}
        {error && (
          <div className="flex items-center gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm font-medium text-rose-800 animate-in fade-in">
            <AlertCircle className="w-5 h-5 shrink-0 text-rose-600" />
            <span className="flex-1">{error}</span>
            <button onClick={() => setError(null)} className="text-rose-500 hover:text-rose-700">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Main Grid Content */}
        <div className="grid min-h-[700px] gap-6 xl:grid-cols-[370px_1fr]">
          {/* Left Column: List of Profiles */}
          <aside className="flex flex-col rounded-2xl border border-[#d5dde2] bg-white shadow-xs overflow-hidden">
            <div className="flex items-center justify-between border-b border-[#eef3f6] px-5 py-4 bg-[#f6f8fa]">
              <div className="flex items-center gap-2">
                <User className="w-4 h-4 text-[#6b7882]" />
                <span className="font-bold text-[#172126] text-sm">
                  Danh sách hồ sơ ({filtered.length})
                </span>
              </div>
              {/* Pagination controls */}
              <div className="flex items-center gap-1.5">
                <button
                  className="flex h-7 w-7 items-center justify-center rounded-lg border border-[#d5dde2] bg-white text-[#172126] hover:bg-[#f6f8fa] disabled:opacity-40 transition"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  title="Trang trước"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <span className="text-xs font-semibold text-[#42515a] px-1">
                  {page + 1}/{totalPages || 1}
                </span>
                <button
                  className="flex h-7 w-7 items-center justify-center rounded-lg border border-[#d5dde2] bg-white text-[#172126] hover:bg-[#f6f8fa] disabled:opacity-40 transition"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  title="Trang sau"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-3 space-y-2.5 max-h-[78vh]">
              {loading && (
                <div className="flex flex-col items-center justify-center py-20 text-[#6b7882]">
                  <Loader2 className="w-8 h-8 animate-spin text-[#0f766e] mb-2" />
                  <p className="text-xs">Đang tải danh sách chuyên gia…</p>
                </div>
              )}

              {!loading && filtered.length === 0 && (
                <div className="flex flex-col items-center justify-center py-20 text-[#6b7882]">
                  <Info className="w-10 h-10 text-[#d5dde2] mb-2" />
                  <p className="text-sm font-medium text-[#172126]">
                    Không có hồ sơ nào phù hợp
                  </p>
                  <p className="text-xs text-[#6b7882] mt-1">Thử thay đổi bộ lọc hoặc từ khoá tìm kiếm</p>
                </div>
              )}

              {!loading &&
                filtered.map((item) => {
                  const isSelected = item.profile?.expertProfileId === selectedProfileId;
                  return (
                    <button
                      key={item.profile?.expertProfileId}
                      onClick={() => {
                        setSelectedProfileId(item.profile?.expertProfileId ?? null);
                        setReasons({ identity: '', credential: '', profile: '' });
                      }}
                      className={`w-full text-left rounded-xl p-4 transition-all duration-150 border ${
                        isSelected
                          ? 'border-[#0f766e] bg-[#d6f0ec]/40 shadow-xs border-l-4 border-l-[#0f766e]'
                          : 'border-[#d5dde2] bg-white hover:border-[#0f766e]/50 hover:bg-[#f6f8fa]'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          {item.profile?.avatarUrl ? (
                            <img
                              src={item.profile.avatarUrl}
                              alt=""
                              className="h-10 w-10 rounded-full object-cover shrink-0 border border-[#d5dde2]"
                            />
                          ) : (
                            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#d6f0ec] text-[#0f766e] font-bold text-sm">
                              {(item.profile?.displayName || 'C').slice(0, 1).toUpperCase()}
                            </div>
                          )}
                          <div className="min-w-0">
                            <p className="truncate font-bold text-[#172126] text-sm">
                              {item.profile?.displayName || 'Chuyên gia chưa cập nhật tên'}
                            </p>
                            <p className="mt-0.5 truncate text-xs text-[#42515a] flex items-center gap-1">
                              <Award className="w-3.5 h-3.5 text-[#0f766e]" />
                              {item.profile?.specialty ||
                                item.profile?.professionalTitle ||
                                'Chưa có chuyên khoa'}
                            </p>
                          </div>
                        </div>
                      </div>

                      <div className="mt-3 flex items-center justify-between border-t border-[#eef3f6] pt-2.5">
                        <StatusBadge value={item.profile?.verificationStatus} />

                        <div className="flex items-center gap-2.5 text-[11px] text-[#42515a] font-medium">
                          <span
                            className="flex items-center gap-1"
                            title={`Định danh: ${item.identityStatus}`}
                          >
                            <ShieldCheck
                              className={`w-3.5 h-3.5 ${
                                item.identityStatus === 'APPROVED'
                                  ? 'text-[#0f766e]'
                                  : 'text-[#d97706]'
                              }`}
                            />
                            {statusConfig[item.identityStatus]?.label ?? item.identityStatus}
                          </span>
                          <span>•</span>
                          <span
                            className="flex items-center gap-1"
                            title={`Chứng chỉ: ${item.credentialStatus}`}
                          >
                            <FileText
                              className={`w-3.5 h-3.5 ${
                                item.credentialStatus === 'APPROVED'
                                  ? 'text-[#0f766e]'
                                  : 'text-[#d97706]'
                              }`}
                            />
                            {statusConfig[item.credentialStatus]?.label ?? item.credentialStatus}
                          </span>
                        </div>
                      </div>
                    </button>
                  );
                })}
            </div>
          </aside>

          {/* Right Column: Detailed Expert Review Panel */}
          <section className="rounded-2xl border border-[#d5dde2] bg-white p-6 md:p-8 shadow-xs">
            {!selected && (
              <div className="flex flex-col items-center justify-center h-full min-h-[500px] text-[#6b7882]">
                <User className="w-16 h-16 text-[#d5dde2] mb-3 stroke-[1.5]" />
                <p className="text-base font-semibold text-[#172126]">
                  Chọn một chuyên gia từ danh sách bên trái
                </p>
                <p className="text-xs text-[#6b7882] mt-1">
                  Thông tin định danh và chứng chỉ sẽ được hiển thị chi tiết tại đây.
                </p>
              </div>
            )}

            {selected && (
              <div className="space-y-8 animate-in fade-in duration-200">
                {/* 1. Header Profile Banner Card */}
                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 border-b border-[#eef3f6] pb-6">
                  <div className="flex items-start gap-4">
                    {selected.profile?.avatarUrl ? (
                      <img
                        className="h-20 w-20 rounded-2xl object-cover border border-[#d5dde2] shadow-xs"
                        src={selected.profile.avatarUrl}
                        alt=""
                      />
                    ) : (
                      <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-[#0f766e] text-2xl font-bold text-white shadow-xs">
                        {(selected.profile?.displayName || 'C').slice(0, 1).toUpperCase()}
                      </div>
                    )}
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="text-2xl font-bold text-[#172126]">
                          {selected.profile?.displayName || 'Hồ sơ chuyên gia'}
                        </h2>
                        <BadgeCheck className="w-5 h-5 text-[#0f766e]" />
                      </div>
                      <p className="text-sm font-medium text-[#0f766e] mt-1 flex items-center gap-2">
                        <span>{selected.profile?.professionalTitle || 'Chưa cập nhật chức danh'}</span>
                        <span>•</span>
                        <span>{selected.profile?.specialty || 'Chưa cập nhật chuyên khoa'}</span>
                        {selected.profile?.experienceYears != null && (
                          <>
                            <span>•</span>
                            <span>{selected.profile.experienceYears} năm kinh nghiệm</span>
                          </>
                        )}
                      </p>
                      <p className="mt-1 text-xs text-[#6b7882] flex items-center gap-1.5">
                        <Building2 className="w-3.5 h-3.5 text-[#6b7882]" />
                        {selected.profile?.workplace || 'Chưa cập nhật nơi công tác'}
                      </p>
                    </div>
                  </div>

                  {/* Trust & Overall Status Badges */}
                  <div className="flex flex-wrap items-center gap-4">
                    <div className="flex flex-col items-end gap-1">
                      <span className="text-[11px] font-bold text-[#6b7882] uppercase tracking-wider">
                        Trạng thái hồ sơ
                      </span>
                      <StatusBadge value={selected.profile?.verificationStatus} />
                    </div>

                    <div id="trust-management-section" className="flex flex-col items-end gap-1">
                      <span className="text-[11px] font-bold text-[#6b7882] uppercase tracking-wider">
                        Độ tin cậy (Trust)
                      </span>
                      <select
                        className="rounded-xl border border-[#d5dde2] bg-[#f7f9fb] px-3 py-1.5 text-xs font-semibold text-[#172126] focus:ring-2 focus:ring-[#0f766e]/10 cursor-pointer"
                        value={selected.profile?.trustStatus || ''}
                        disabled={Boolean(action)}
                        onChange={(event) => changeTrust(event.target.value as TrustStatus)}
                      >
                        {!selected.profile?.trustStatus && (
                          <option value="" disabled>
                            Trust: Chưa lập
                          </option>
                        )}
                        <option value="ACTIVE">🟢 Trust: Hoạt động</option>
                        <option value="SUSPENDED">🟠 Trust: Tạm ngưng</option>
                        <option value="REVOKED">🔴 Trust: Thu hồi</option>
                      </select>
                    </div>
                  </div>
                </div>

                {/* 2. Step 1: Identity Verification Section */}
                <section id="identity-verification-section" className="rounded-2xl border border-[#d5dde2] bg-[#f6f8fa] p-6 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-[#0f766e] text-xs font-bold text-white shadow-xs">
                        1
                      </span>
                      <h3 className="font-bold text-[#172126] text-base">
                        1. Định danh chuyên gia (CCCD & AI Face Matching)
                      </h3>
                    </div>
                    <StatusBadge value={selected.identityStatus} />
                  </div>

                  {selected.latestIdentity ? (
                    <div className="space-y-4">
                      {/* Photo Grid */}
                      <div className="grid gap-4 md:grid-cols-3">
                        {[
                          ['Ảnh chân dung (Selfie)', identityUrls.selfie],
                          ['CCCD mặt trước', identityUrls.front],
                          ['CCCD mặt sau', identityUrls.back],
                        ].map(([label, url]) => (
                          <div
                            key={label}
                            className="group relative overflow-hidden rounded-xl border border-[#d5dde2] bg-white p-3 shadow-2xs transition hover:shadow-sm"
                          >
                            <p className="mb-2 text-xs font-bold text-[#172126] flex items-center justify-between">
                              <span>{label}</span>
                              {url && (
                                <button
                                  onClick={() => setLightbox({ src: url, title: label })}
                                  className="text-[#0f766e] hover:underline flex items-center gap-1 text-[11px]"
                                >
                                  <Eye className="w-3.5 h-3.5" /> Xem lớn
                                </button>
                              )}
                            </p>
                            {url ? (
                              <div
                                className="relative aspect-4/3 w-full overflow-hidden rounded-lg bg-[#f7f9fb] cursor-pointer"
                                onClick={() => setLightbox({ src: url, title: label })}
                              >
                                <img
                                  className="h-full w-full object-contain transition duration-200 group-hover:scale-105"
                                  src={url}
                                  alt={label}
                                />
                              </div>
                            ) : (
                              <div className="flex h-36 items-center justify-center rounded-lg bg-[#f7f9fb] text-xs text-[#6b7882]">
                                Không thể tải ảnh
                              </div>
                            )}
                          </div>
                        ))}
                      </div>

                      {/* AI Face Match Status Box */}
                      <div className="rounded-xl border border-[#d5dde2] bg-white p-4 flex flex-col md:flex-row items-center justify-between gap-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#d6f0ec] text-[#0f766e]">
                            <ShieldCheck className="w-5 h-5" />
                          </div>
                          <div>
                            <p className="text-xs text-[#42515a] font-medium">
                              Kết quả đối sánh khuôn mặt (CompreFace AI)
                            </p>
                            <div className="flex items-center gap-3 mt-0.5">
                              <span className="text-sm font-bold text-[#172126]">
                                {selected.latestIdentity.faceStatus || 'Chưa đối sánh'}
                              </span>
                              {selected.latestIdentity.faceSimilarity != null && (
                                <div className="flex items-center gap-2">
                                  <div className="h-2 w-24 rounded-full bg-[#eef3f6] overflow-hidden">
                                    <div
                                      className="h-full bg-[#0f766e]"
                                      style={{
                                        width: `${Math.min(
                                          100,
                                          selected.latestIdentity.faceSimilarity * 100,
                                        )}%`,
                                      }}
                                    />
                                  </div>
                                  <span className="text-xs font-bold text-[#0f766e]">
                                    {(selected.latestIdentity.faceSimilarity * 100).toFixed(1)}%
                                  </span>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>

                        {selected.latestIdentity.reviewReason && (
                          <p className="text-xs text-[#42515a] bg-[#f7f9fb] px-3 py-1.5 rounded-lg border border-[#d5dde2]">
                            Ghi chú: <strong>{selected.latestIdentity.reviewReason}</strong>
                          </p>
                        )}
                      </div>

                      {/* Action buttons for Identity */}
                      {!['APPROVED', 'REJECTED'].includes(selected.identityStatus) && (
                        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 space-y-3">
                          <textarea
                            className="w-full rounded-xl border border-[#d5dde2] bg-white p-3 text-sm placeholder-[#6b7882] focus:ring-2 focus:ring-[#0f766e]/10 focus:outline-none"
                            rows={2}
                            value={reasons.identity}
                            onChange={(event) =>
                              setReasons((current) => ({
                                ...current,
                                identity: event.target.value,
                              }))
                            }
                            placeholder="Nhập lý do chi tiết nếu bạn từ chối định danh này…"
                          />
                          <div className="flex items-center gap-3">
                            <button
                              disabled={Boolean(action)}
                              className="inline-flex items-center gap-1.5 rounded-xl bg-[#0f766e] px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-[#0d665f] active:scale-95 disabled:opacity-50 transition"
                              onClick={() => decideIdentity('APPROVED')}
                            >
                              <CheckCircle2 className="w-4 h-4" />
                              Phê duyệt định danh
                            </button>
                            <button
                              disabled={Boolean(action)}
                              className="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-rose-500 active:scale-95 disabled:opacity-50 transition"
                              onClick={() => decideIdentity('REJECTED')}
                            >
                              <XCircle className="w-4 h-4" />
                              Từ chối định danh
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  ) : (
                    <p className="text-sm text-[#6b7882] italic py-2">
                      Chuyên gia chưa gửi hồ sơ ảnh định danh CCCD.
                    </p>
                  )}
                </section>

                {/* 3. Step 2: Professional Credentials Section */}
                <section className="rounded-2xl border border-[#d5dde2] bg-[#f6f8fa] p-6 space-y-5">
                  <div className="flex items-center justify-between border-b border-[#eef3f6] pb-4">
                    <div className="flex items-center gap-2.5">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-[#0f766e] text-xs font-bold text-white shadow-xs">
                        2
                      </span>
                      <h3 className="font-bold text-[#172126] text-base">
                        2. Chứng chỉ chuyên môn & Bằng cấp
                      </h3>
                    </div>
                    <StatusBadge value={selected.credentialStatus} />
                  </div>

                  <div className="space-y-4">
                    <div className="space-y-1.5">
                      <label className="block text-xs font-bold text-[#172126]">
                        Ghi chú / Lý do nếu từ chối chứng chỉ
                      </label>
                      <textarea
                        className="w-full rounded-xl border border-[#d5dde2] bg-white p-3 text-sm placeholder-[#6b7882] focus:ring-2 focus:ring-[#0f766e]/10 focus:outline-none"
                        rows={2}
                        value={reasons.credential}
                        onChange={(event) =>
                          setReasons((current) => ({
                            ...current,
                            credential: event.target.value,
                          }))
                        }
                        placeholder="Bắt buộc nhập lý do khi từ chối từng bằng cấp/chứng chỉ…"
                      />
                    </div>

                    {selected.credentials.length === 0 && (
                      <p className="text-sm text-[#6b7882] italic py-2">
                        Chưa có chứng chỉ chuyên môn nào được tải lên.
                      </p>
                    )}

                    {selected.credentials.map((credential) => (
                      <article
                        key={credential.credentialId}
                        className="rounded-xl border border-[#d5dde2] bg-white p-5 shadow-2xs space-y-4"
                      >
                        {/* Header: Type Title & Review Badge */}
                        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#f1f5f9] pb-3">
                          <div className="flex items-center gap-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#d6f0ec] text-[#0f766e] shrink-0">
                              <Award className="w-5 h-5" />
                            </div>
                            <div>
                              <h4 className="font-bold text-[#172126] text-base">
                                {credentialLabels[credential.credentialType] ??
                                  credential.credentialType}
                              </h4>
                            </div>
                          </div>
                          <StatusBadge value={credential.reviewStatus} />
                        </div>

                        {/* Detail Rows stacked cleanly */}
                        <div className="grid gap-2.5 text-xs text-[#42515a] bg-[#f8fafc] p-3.5 rounded-xl border border-[#e2e8f0]">
                          <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-6">
                            <span className="min-w-[100px] text-[#6b7882] font-semibold">Số hiệu:</span>
                            <span className="font-bold text-[#172126]">{credential.credentialNumber || '—'}</span>
                          </div>
                          <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-6">
                            <span className="min-w-[100px] text-[#6b7882] font-semibold">Cơ quan cấp:</span>
                            <span className="font-semibold text-[#172126]">{credential.issuer || '—'}</span>
                          </div>
                          <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-6">
                            <span className="min-w-[100px] text-[#6b7882] font-semibold">Ngày cấp:</span>
                            <span className="font-medium text-[#172126]">
                              {credential.issuedDate ? new Date(credential.issuedDate).toLocaleDateString('vi-VN') : '—'}
                            </span>
                          </div>
                          <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-6">
                            <span className="min-w-[100px] text-[#6b7882] font-semibold">Hạn sử dụng:</span>
                            <span className="font-medium text-[#172126] flex items-center gap-2">
                              {credential.expiryDate ? (
                                <>
                                  <span>{new Date(credential.expiryDate).toLocaleDateString('vi-VN')}</span>
                                  {new Date(credential.expiryDate) < new Date() && (
                                    <span className="inline-flex items-center gap-1 rounded-md bg-rose-100 px-2 py-0.5 text-[11px] font-bold text-rose-700 border border-rose-200">
                                      ⚠️ Đã hết hạn
                                    </span>
                                  )}
                                </>
                              ) : (
                                <span className="text-emerald-700 font-semibold">Vĩnh viễn (Không hết hạn)</span>
                              )}
                            </span>
                          </div>
                          <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-6">
                            <span className="min-w-[100px] text-[#6b7882] font-semibold">Tệp tài liệu:</span>
                            <span className="font-mono text-[#0f766e] flex items-center gap-2">
                              <span>{credential.fileName || 'Tài liệu không tên'}</span>
                              <span className="text-[#94a3b8]">•</span>
                              <span className="text-[#64748b]">{credential.mimeType || 'Không rõ MIME'}</span>
                            </span>
                          </div>
                        </div>

                        {/* Action buttons row */}
                        <div className="flex flex-wrap items-center gap-2.5 pt-1">
                          {credential.fileId && (
                            <button
                              className="inline-flex items-center gap-1.5 rounded-xl border border-[#d5dde2] bg-[#f7f9fb] px-4 py-2 text-xs font-semibold text-[#172126] hover:bg-[#eef3f6] transition"
                              onClick={() => setSelectedDocument(credential)}
                            >
                              <Eye className="w-3.5 h-3.5 text-[#0f766e]" />
                              Đọc / Xem trước tài liệu
                            </button>
                          )}

                          {credential.reviewStatus === 'PENDING' && (
                            <>
                              <button
                                disabled={Boolean(action)}
                                className="inline-flex items-center gap-1.5 rounded-xl bg-[#0f766e] px-4 py-2 text-xs font-semibold text-white hover:bg-[#0d665f] transition"
                                onClick={() =>
                                  decideCredential(credential.credentialId, 'APPROVED')
                                }
                              >
                                <CheckCircle2 className="w-3.5 h-3.5" /> Duyệt chứng chỉ
                              </button>
                              <button
                                disabled={Boolean(action)}
                                className="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-500 transition"
                                onClick={() =>
                                  decideCredential(credential.credentialId, 'REJECTED')
                                }
                              >
                                <XCircle className="w-3.5 h-3.5" /> Từ chối
                              </button>
                            </>
                          )}
                        </div>
                      </article>
                    ))}
                  </div>
                </section>

                {/* 4. Step 3: Final Expert Approval Decision Card */}
                <section className="rounded-2xl border border-[#95d2ca] bg-[#d6f0ec]/20 p-6 space-y-5">
                  {/* Row 1: Title */}
                  <div className="flex items-center gap-2.5 border-b border-[#95d2ca]/40 pb-4">
                    <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-[#0f766e] text-xs font-bold text-white shadow-xs">
                      3
                    </span>
                    <h3 className="font-bold text-[#172126] text-base">
                      3. Quyết định hồ sơ chuyên gia
                    </h3>
                  </div>

                  {/* Row 2: Readiness Status Banner */}
                  <div
                    className={`rounded-xl p-4 border text-xs font-semibold flex items-center gap-3 ${
                      selected.readyForFinalApproval
                        ? 'bg-[#d6f0ec] text-[#0f5a53] border-[#95d2ca]'
                        : 'bg-[#fef3c7] text-[#92400e] border-[#fde68a]'
                    }`}
                  >
                    <Info className="w-5 h-5 shrink-0" />
                    <span>
                      {selected.readyForFinalApproval
                        ? 'Đã đủ điều kiện phê duyệt (Định danh CCCD và ít nhất 1 chứng chỉ đã được xác nhận).'
                        : 'Chưa đủ điều kiện phê duyệt cuối (Cần duyệt thành công cả định danh và tối thiểu 1 chứng chỉ).'}
                    </span>
                  </div>

                  {/* Row 3: Note / Reason textarea */}
                  <div className="space-y-1.5">
                    <label className="block text-xs font-bold text-[#172126]">
                      Ghi chú / Lý do quyết định duyệt hoặc từ chối
                    </label>
                    <textarea
                      className="w-full rounded-xl border border-[#d5dde2] bg-white p-3 text-sm placeholder-[#6b7882] focus:ring-2 focus:ring-[#0f766e]/10 focus:outline-none"
                      rows={3}
                      value={reasons.profile}
                      onChange={(event) =>
                        setReasons((current) => ({
                          ...current,
                          profile: event.target.value,
                        }))
                      }
                      placeholder="Bắt buộc nhập lý do khi từ chối hồ sơ chuyên gia…"
                    />
                  </div>

                  {/* Row 4: Final Action Buttons */}
                  {selected.profile?.verificationStatus !== 'APPROVED' && (
                    <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-[#95d2ca]/30">
                      <button
                        disabled={!selected.readyForFinalApproval || Boolean(action)}
                        className="inline-flex items-center gap-2 rounded-xl bg-[#0f766e] px-6 py-3 text-sm font-bold text-white shadow-xs hover:bg-[#0d665f] disabled:opacity-40 transition"
                        onClick={() => decideProfile('APPROVED')}
                      >
                        <CheckCircle2 className="w-4 h-4" />
                        Phê duyệt chuyên gia
                      </button>
                      <button
                        disabled={Boolean(action)}
                        className="inline-flex items-center gap-2 rounded-xl border border-rose-300 bg-white px-6 py-3 text-sm font-bold text-rose-700 hover:bg-rose-50 transition"
                        onClick={() => decideProfile('REJECTED')}
                      >
                        <XCircle className="w-4 h-4" />
                        Từ chối hồ sơ
                      </button>
                    </div>
                  )}
                </section>
              </div>
            )}
          </section>
        </div>
      </div>

      {/* Lightbox for Image zoom */}
      {lightbox && (
        <ImageLightbox
          src={lightbox.src}
          title={lightbox.title}
          onClose={() => setLightbox(null)}
        />
      )}

      {/* Document Modal Preview */}
      {selectedDocument && (
        <DocumentModal credential={selectedDocument} onClose={() => setSelectedDocument(null)} />
      )}
    </main>
  );
}


