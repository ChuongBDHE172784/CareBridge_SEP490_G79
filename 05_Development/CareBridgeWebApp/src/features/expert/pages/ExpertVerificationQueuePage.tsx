import { useCallback, useEffect, useMemo, useState } from 'react';
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

const statusLabels: Record<string, string> = {
  ACTIVE: 'Hoạt động',
  APPROVED: 'Đã duyệt',
  MANUAL_REVIEW_REQUIRED: 'Cần duyệt thủ công',
  MISSING: 'Chưa nộp',
  PENDING: 'Chờ duyệt',
  PENDING_REVIEW: 'Chờ duyệt',
  REJECTED: 'Từ chối',
  REVOKED: 'Thu hồi',
  SUSPENDED: 'Tạm ngưng',
};

const credentialLabels: Record<string, string> = {
  CERTIFICATE: 'Chứng chỉ đào tạo',
  DEGREE: 'Bằng cấp chuyên môn',
  IDENTITY_DOCUMENT: 'Giấy tờ định danh',
  MEDICAL_LICENSE: 'Giấy phép hành nghề',
  PROFESSIONAL_LICENSE: 'Giấy phép chuyên môn',
};

const statusClass = (status?: string | null) => {
  if (status === 'APPROVED' || status === 'ACTIVE') return 'bg-green-100 text-green-800';
  if (status === 'REJECTED' || status === 'REVOKED') return 'bg-red-100 text-red-800';
  if (status === 'SUSPENDED') return 'bg-orange-100 text-orange-800';
  if (status === 'MISSING') return 'bg-gray-100 text-gray-700';
  return 'bg-amber-100 text-amber-800';
};

function StatusBadge({ value }: { value?: string | null }) {
  const normalized = value || 'MISSING';
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusClass(normalized)}`}>
      {statusLabels[normalized] ?? normalized.replaceAll('_', ' ')}
    </span>
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
          .catch(() => active && setError('Không thể trích xuất nội dung. Vui lòng tải bản gốc để kiểm tra.')),
      );
    }
    Promise.allSettled(tasks).finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [credential.credentialId, isImage, isPdf]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={onClose}>
      <div
        className="flex max-h-[92vh] w-full max-w-5xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="flex items-center justify-between gap-4 border-b px-5 py-4">
          <div className="min-w-0">
            <h2 className="truncate font-bold text-gray-900">{credential.fileName || 'Tài liệu chứng chỉ'}</h2>
            <p className="text-xs text-gray-500">{mime || 'Không rõ định dạng'}</p>
          </div>
          <div className="flex items-center gap-2">
            {fileUrl && (
              <a className="portal-primary-button" href={fileUrl} download={credential.fileName ?? true}>
                Tải bản gốc
              </a>
            )}
            <button className="h-9 w-9 rounded-full hover:bg-gray-100" onClick={onClose} aria-label="Đóng">×</button>
          </div>
        </header>
        <div className="flex-1 overflow-auto bg-gray-50 p-5">
          {isImage && fileUrl && (
            <img className="mx-auto max-h-[75vh] max-w-full rounded-xl object-contain" src={fileUrl} alt="Chứng chỉ" />
          )}
          {isPdf && fileUrl && (
            <iframe className="mb-5 h-[65vh] w-full rounded-xl border bg-white" src={fileUrl} title="PDF chứng chỉ" />
          )}
          {!isImage && !isPdf && (
            <section className="rounded-xl border bg-white p-5">
              <h3 className="mb-3 font-semibold text-gray-900">
                {isPdf ? 'Nội dung văn bản dự phòng' : 'Nội dung tài liệu'}
              </h3>
              {loading && <p className="text-sm text-gray-500">Đang đọc tài liệu…</p>}
              {error && <p className="text-sm text-red-700">{error}</p>}
              {preview && (
                <>
                  <pre className="whitespace-pre-wrap break-words font-sans text-sm leading-6 text-gray-800">
                    {preview.content || 'Tài liệu không chứa nội dung văn bản có thể trích xuất.'}
                  </pre>
                  {preview.truncated && <p className="mt-4 text-xs text-amber-700">Nội dung dài đã được rút gọn khi xem trước.</p>}
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
  const [document, setDocument] = useState<DocumentReviewResponse | null>(null);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [reasons, setReasons] = useState({
    identity: '',
    credential: '',
    profile: '',
  });
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadCases = useCallback(async (keepProfileId?: string | null) => {
    setLoading(true);
    setError(null);
    try {
      const result = await getExpertReviewCases();
      setCases(result);
      const requested = keepProfileId ?? selectedProfileId;
      const nextSelected = result.some((item) => item.profile.expertProfileId === requested)
        ? requested
        : result[0]?.profile.expertProfileId ?? null;
      setSelectedProfileId(nextSelected);
    } catch {
      setError('Không thể tải trung tâm xét duyệt chuyên gia.');
    } finally {
      setLoading(false);
    }
  }, [selectedProfileId]);

  useEffect(() => {
    void loadCases(null);
    // Initial load only; actions explicitly refresh while preserving selection.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtered = useMemo(() => {
    const query = keyword.trim().toLocaleLowerCase('vi');
    return cases.filter((item) => {
      const profile = item.profile;
      const pending = profile.verificationStatus !== 'APPROVED';
      const matchesStatus =
        !statusFilter
        || (statusFilter === 'PENDING' && pending)
        || profile.verificationStatus === statusFilter
        || profile.trustStatus === statusFilter;
      const haystack = [profile.displayName, profile.specialty, profile.professionalTitle, profile.workplace]
        .filter(Boolean)
        .join(' ')
        .toLocaleLowerCase('vi');
      return matchesStatus && (!query || haystack.includes(query));
    });
  }, [cases, keyword, statusFilter]);

  const selected = cases.find((item) => item.profile.expertProfileId === selectedProfileId) ?? null;

  useEffect(() => {
    if (filtered.length > 0
        && !filtered.some((item) => item.profile.expertProfileId === selectedProfileId)) {
      setSelectedProfileId(filtered[0].profile.expertProfileId);
    }
  }, [filtered, selectedProfileId]);

  useEffect(() => {
    setDocument(null);
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
    Promise.allSettled(entries.map(async ([key, fileId]) =>
      [key, await getIdentityFileUrl(fileId)] as const))
      .then((results) => {
        if (!active) return;
        const urls = results
          .filter((result): result is PromiseFulfilledResult<readonly [string, string]> =>
            result.status === 'fulfilled')
          .map((result) => result.value);
        setIdentityUrls(Object.fromEntries(urls));
      });
    return () => {
      active = false;
    };
  }, [selected?.latestIdentity]);

  const requireRejectReason = (decision: ReviewDecision, reason: string) => {
    if (decision === 'REJECTED' && !reason.trim()) {
      setError('Vui lòng nhập lý do trước khi từ chối.');
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
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Thao tác không thành công.');
    } finally {
      setAction(null);
    }
  };

  const decideIdentity = (decision: ReviewDecision) => {
    const identityId = selected?.latestIdentity?.identityVerificationId ?? selected?.latestIdentity?.attemptId;
    if (!identityId || !requireRejectReason(decision, reasons.identity)) return;
    void runAction(`identity-${decision}`, () =>
      reviewIdentity(identityId, decision, reasons.identity.trim()));
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
    if ((status === 'SUSPENDED' || status === 'REVOKED')
        && !window.confirm(`Xác nhận chuyển trust sang ${statusLabels[status]}?`)) {
      return;
    }
    void runAction(`trust-${status}`, () => setExpertTrust(selected.profile.expertProfileId, status));
  };

  return (
    <main className="p-5 font-sans lg:p-8">
      <div className="mx-auto max-w-[1500px]">
        <header className="mb-6">
          <h1 className="text-[26px] font-bold text-on-surface">Trung tâm xét duyệt chuyên gia</h1>
          <p className="mt-1 text-sm text-on-surface-variant">
            Định danh, chứng chỉ, quyết định cuối và trạng thái tin cậy trong cùng một màn hình.
          </p>
        </header>

        <div className="mb-5 grid gap-3 rounded-2xl border bg-surface p-4 shadow-sm md:grid-cols-[1fr_220px_auto]">
          <input
            className="rounded-xl border border-outline-variant px-4 py-2.5 text-sm"
            placeholder="Tìm tên, chuyên khoa, chức danh…"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
          <select className="rounded-xl border border-outline-variant px-3 py-2.5 text-sm" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="PENDING">Đang chờ xét duyệt</option>
            <option value="">Tất cả chuyên gia</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="REJECTED">Đã từ chối</option>
            <option value="SUSPENDED">Đang tạm ngưng</option>
            <option value="REVOKED">Đã thu hồi</option>
          </select>
          <button className="portal-primary-button" onClick={() => void loadCases(selectedProfileId)}>Làm mới</button>
        </div>

        {error && <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">{error}</div>}

        <div className="grid min-h-[650px] gap-5 xl:grid-cols-[370px_1fr]">
          <aside className="overflow-hidden rounded-2xl border bg-surface shadow-sm">
            <div className="border-b px-4 py-3 text-sm font-bold">Hồ sơ ({filtered.length})</div>
            <div className="max-h-[75vh] space-y-2 overflow-y-auto p-3">
              {loading && <p className="py-10 text-center text-sm text-gray-500">Đang tải…</p>}
              {!loading && filtered.length === 0 && <p className="py-10 text-center text-sm text-gray-500">Không có hồ sơ phù hợp.</p>}
              {filtered.map((item) => {
                const active = item.profile.expertProfileId === selectedProfileId;
                return (
                  <button
                    key={item.profile.expertProfileId}
                    onClick={() => {
                      setSelectedProfileId(item.profile.expertProfileId);
                      setReasons({ identity: '', credential: '', profile: '' });
                    }}
                    className={`w-full rounded-xl border p-4 text-left transition ${active ? 'border-primary bg-primary/10' : 'border-gray-200 hover:border-primary/40'}`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="truncate font-semibold text-gray-900">{item.profile.displayName || 'Chuyên gia chưa cập nhật tên'}</p>
                        <p className="mt-1 truncate text-xs text-gray-500">{item.profile.specialty || item.profile.professionalTitle || 'Chưa có chuyên khoa'}</p>
                      </div>
                      <StatusBadge value={item.profile.verificationStatus} />
                    </div>
                    <div className="mt-3 flex gap-2 text-[11px]">
                      <span>Định danh: {statusLabels[item.identityStatus] ?? item.identityStatus}</span>
                      <span>•</span>
                      <span>Chứng chỉ: {statusLabels[item.credentialStatus] ?? item.credentialStatus}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          </aside>

          <section className="rounded-2xl border bg-surface p-5 shadow-sm">
            {!selected && <div className="grid h-full place-items-center text-sm text-gray-500">Chọn một hồ sơ để xét duyệt.</div>}
            {selected && (
              <div className="space-y-6">
                <div className="flex flex-wrap items-start justify-between gap-4 border-b pb-5">
                  <div className="flex items-center gap-4">
                    {selected.profile.avatarUrl ? (
                      <img className="h-16 w-16 rounded-full object-cover" src={selected.profile.avatarUrl} alt="" />
                    ) : (
                      <div className="grid h-16 w-16 place-items-center rounded-full bg-primary/10 text-xl font-bold text-primary">
                        {(selected.profile.displayName || 'C').slice(0, 1)}
                      </div>
                    )}
                    <div>
                      <h2 className="text-xl font-bold">{selected.profile.displayName || 'Hồ sơ chuyên gia'}</h2>
                      <p className="text-sm text-gray-600">{selected.profile.professionalTitle || '—'} · {selected.profile.specialty || '—'}</p>
                      <p className="mt-1 text-xs text-gray-500">{selected.profile.workplace || 'Chưa cập nhật nơi công tác'}</p>
                    </div>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge value={selected.profile.verificationStatus} />
                    <select
                      className="rounded-lg border px-3 py-2 text-xs"
                      value={selected.profile.trustStatus || ''}
                      disabled={Boolean(action)}
                      onChange={(event) => changeTrust(event.target.value as TrustStatus)}
                    >
                      {!selected.profile.trustStatus && <option value="" disabled>Trust: Chưa thiết lập</option>}
                      <option value="ACTIVE">Trust: Hoạt động</option>
                      <option value="SUSPENDED">Trust: Tạm ngưng</option>
                      <option value="REVOKED">Trust: Thu hồi</option>
                    </select>
                  </div>
                </div>

                <section className="rounded-xl border p-4">
                  <div className="mb-4 flex items-center justify-between">
                    <h3 className="font-bold">1. Định danh chuyên gia</h3>
                    <StatusBadge value={selected.identityStatus} />
                  </div>
                  {selected.latestIdentity ? (
                    <>
                      <div className="grid gap-3 md:grid-cols-3">
                        {[
                          ['Ảnh chân dung', identityUrls.selfie],
                          ['CCCD mặt trước', identityUrls.front],
                          ['CCCD mặt sau', identityUrls.back],
                        ].map(([label, url]) => (
                          <div key={label} className="rounded-xl border bg-gray-50 p-3">
                            <p className="mb-2 text-xs font-semibold">{label}</p>
                            {url ? <img className="h-48 w-full rounded-lg object-contain" src={url} alt={label} /> : <div className="grid h-48 place-items-center text-xs text-gray-400">Không tải được ảnh</div>}
                          </div>
                        ))}
                      </div>
                      <div className="mt-3 grid gap-2 text-sm md:grid-cols-3">
                        <p>Đối sánh: <strong>{selected.latestIdentity.faceStatus || '—'}</strong></p>
                        <p>Tương đồng: <strong>{selected.latestIdentity.faceSimilarity == null ? '—' : `${(selected.latestIdentity.faceSimilarity * 100).toFixed(1)}%`}</strong></p>
                        <p>Lý do: <strong>{selected.latestIdentity.reviewReason || '—'}</strong></p>
                      </div>
                      {!['APPROVED', 'REJECTED'].includes(selected.identityStatus) && (
                        <div className="mt-4">
                          <textarea
                            className="mb-3 min-h-16 w-full rounded-xl border bg-white p-3 text-sm"
                            value={reasons.identity}
                            onChange={(event) => setReasons((current) => ({ ...current, identity: event.target.value }))}
                            placeholder="Lý do từ chối định danh (bắt buộc khi từ chối)"
                          />
                          <div className="flex gap-2">
                            <button disabled={Boolean(action)} className="rounded-full bg-green-700 px-4 py-2 text-sm font-semibold text-white" onClick={() => decideIdentity('APPROVED')}>Duyệt định danh</button>
                            <button disabled={Boolean(action)} className="rounded-full bg-red-700 px-4 py-2 text-sm font-semibold text-white" onClick={() => decideIdentity('REJECTED')}>Từ chối</button>
                          </div>
                        </div>
                      )}
                    </>
                  ) : (
                    <p className="text-sm text-gray-500">Chuyên gia chưa nộp bộ ảnh định danh.</p>
                  )}
                </section>

                <section className="rounded-xl border p-4">
                  <div className="mb-4 flex items-center justify-between">
                    <h3 className="font-bold">2. Chứng chỉ chuyên môn</h3>
                    <StatusBadge value={selected.credentialStatus} />
                  </div>
                  <div className="space-y-3">
                    <textarea
                      className="min-h-16 w-full rounded-xl border bg-white p-3 text-sm"
                      value={reasons.credential}
                      onChange={(event) => setReasons((current) => ({ ...current, credential: event.target.value }))}
                      placeholder="Lý do từ chối chứng chỉ (bắt buộc khi từ chối)"
                    />
                    {selected.credentials.length === 0 && <p className="text-sm text-gray-500">Chưa có chứng chỉ chuyên môn.</p>}
                    {selected.credentials.map((credential) => (
                      <article key={credential.credentialId} className="rounded-xl border bg-gray-50 p-4">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <h4 className="font-semibold">{credentialLabels[credential.credentialType] ?? credential.credentialType}</h4>
                            <p className="mt-1 text-sm text-gray-600">Số: {credential.credentialNumber || '—'} · Cấp bởi: {credential.issuer || '—'}</p>
                            <p className="mt-1 text-xs text-gray-500">{credential.fileName || 'Tài liệu'} · {credential.mimeType || 'Không rõ MIME'}</p>
                          </div>
                          <StatusBadge value={credential.reviewStatus} />
                        </div>
                        <div className="mt-3 flex flex-wrap gap-2">
                          {credential.fileId && (
                            <button className="portal-secondary-button" onClick={() => setDocument(credential)}>Đọc tài liệu</button>
                          )}
                          {credential.reviewStatus === 'PENDING' && (
                            <>
                              <button disabled={Boolean(action)} className="rounded-full bg-green-700 px-4 py-2 text-xs font-semibold text-white" onClick={() => decideCredential(credential.credentialId, 'APPROVED')}>Duyệt chứng chỉ</button>
                              <button disabled={Boolean(action)} className="rounded-full bg-red-700 px-4 py-2 text-xs font-semibold text-white" onClick={() => decideCredential(credential.credentialId, 'REJECTED')}>Từ chối</button>
                            </>
                          )}
                        </div>
                      </article>
                    ))}
                  </div>
                </section>

                <section className="rounded-xl border border-primary/30 bg-primary/5 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-4">
                    <div>
                      <h3 className="font-bold">3. Quyết định hồ sơ chuyên gia</h3>
                      <p className="mt-1 text-sm text-gray-600">
                        {selected.readyForFinalApproval
                          ? 'Đã đủ định danh và chứng chỉ hợp lệ.'
                          : 'Chỉ có thể phê duyệt cuối khi định danh và ít nhất một chứng chỉ còn hiệu lực đã được duyệt.'}
                      </p>
                    </div>
                    {selected.profile.verificationStatus !== 'APPROVED' && (
                      <div className="flex gap-2">
                        <button disabled={!selected.readyForFinalApproval || Boolean(action)} className="rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-40" onClick={() => decideProfile('APPROVED')}>Phê duyệt chuyên gia</button>
                        <button disabled={Boolean(action)} className="rounded-full border border-red-300 px-5 py-2.5 text-sm font-semibold text-red-700" onClick={() => decideProfile('REJECTED')}>Từ chối hồ sơ</button>
                      </div>
                    )}
                  </div>
                  <label className="mt-4 block text-sm font-medium">
                    Lý do/ghi chú quyết định hồ sơ
                    <textarea
                      className="mt-2 min-h-20 w-full rounded-xl border bg-white p-3 font-normal"
                      value={reasons.profile}
                      onChange={(event) => setReasons((current) => ({ ...current, profile: event.target.value }))}
                      placeholder="Bắt buộc khi từ chối hồ sơ"
                    />
                  </label>
                </section>
              </div>
            )}
          </section>
        </div>
      </div>
      {document && <DocumentModal credential={document} onClose={() => setDocument(null)} />}
    </main>
  );
}
