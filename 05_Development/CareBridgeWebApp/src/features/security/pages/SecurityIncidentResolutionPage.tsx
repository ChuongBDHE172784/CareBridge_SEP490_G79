import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getSecurityIncident, resolveSecurityIncident, type SecurityEvent } from '../services/securityIncidentApi';

const ROOT_CAUSES = [
  { id: 'SOFTWARE_VULNERABILITY', icon: 'bug_report', label: 'Lỗi phần mềm' },
  { id: 'COMPROMISED_CREDENTIAL', icon: 'vpn_key', label: 'Thông tin xác thực' },
  { id: 'MISCONFIGURATION', icon: 'settings_alert', label: 'Sai cấu hình' },
  { id: 'HUMAN_ERROR', icon: 'person_alert', label: 'Thao tác nội bộ' },
  { id: 'FALSE_POSITIVE', icon: 'rule', label: 'Cảnh báo sai' },
];
const INITIAL_TASKS = [
  { text: 'Thu hồi token hoặc session bị ảnh hưởng', done: false },
  { text: 'Rà soát log truy cập liên quan', done: false },
  { text: 'Xác nhận biện pháp ngăn tái diễn', done: false },
];

export default function SecurityIncidentResolutionPage() {
  const { eventId = '' } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const [incident, setIncident] = useState<SecurityEvent | null>(null);
  const [rootCause, setRootCause] = useState('');
  const [summary, setSummary] = useState('');
  const [affectedScope, setAffectedScope] = useState('');
  const [tasks, setTasks] = useState(INITIAL_TASKS);
  const [notifyAffected, setNotifyAffected] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    getSecurityIncident(eventId).then(setIncident).catch(() => setError('Không thể tải sự cố.')).finally(() => setIsLoading(false));
  }, [eventId]);

  const valid = rootCause && summary.trim().length >= 20 && affectedScope.trim() && tasks.some((task) => task.done) && confirmed;
  const handleResolve = async () => {
    setSubmitted(true);
    if (!valid) return;
    setIsSaving(true); setError('');
    try {
      const resolved = await resolveSecurityIncident(eventId, {
        rootCause,
        summary: summary.trim(),
        affectedScope: affectedScope.trim(),
        remediationTasks: tasks.filter((task) => task.done).map((task) => task.text),
        notifyAffected,
        confirmed,
      });
      setIncident(resolved);
    } catch { setError('Không thể đóng sự cố. Dữ liệu chưa được thay đổi, vui lòng thử lại.'); }
    finally { setIsSaving(false); }
  };

  if (isLoading) return <div className="portal-page p-6"><div className="portal-empty" role="status">Đang tải biểu mẫu giải quyết...</div></div>;
  if (!incident) return <div className="portal-page p-6"><div className="portal-error">{error}<button className="portal-secondary-button ml-3" onClick={() => navigate('/admin/security/incidents')}>Quay lại</button></div></div>;
  if (incident.status === 'RESOLVED' || incident.status === 'FALSE_POSITIVE') return <div className="portal-page p-6"><div className="portal-contained"><div className="portal-card-padded text-center"><span className="material-symbols-outlined text-5xl text-primary">task_alt</span><h1 className="mt-2 text-xl font-semibold">Case SEC-{eventId} đã được đóng</h1><p className="mt-2 text-sm text-on-surface-variant">{incident.status === 'FALSE_POSITIVE' ? 'Cảnh báo được kết luận không phải sự cố.' : 'Sự cố đã được giải quyết.'} Kết luận đã được lưu vào timeline audit bất biến.</p><button className="portal-primary-button mt-5" onClick={() => navigate('/admin/security/incidents')}>Về danh sách</button></div></div></div>;

  return <div className="portal-page px-5 py-5 md:px-6 md:py-6"><div className="portal-contained">
    <button className="mb-4 inline-flex items-center gap-1 text-sm font-semibold text-primary" onClick={() => navigate(`/admin/security/incidents/${eventId}/investigate`)}><span className="material-symbols-outlined">arrow_back</span>Quay lại điều tra</button>
    <div className="portal-header"><div><p className="portal-eyebrow">SEC-{eventId} · {incident.severity}</p><h1 className="portal-title">Giải quyết sự cố</h1><p className="portal-subtitle">Xác nhận nguyên nhân, phạm vi và hành động khắc phục trước khi đóng case.</p></div></div>
    {error && <div className="portal-error mb-4">{error}</div>}
    <div className="grid gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(280px,1fr)]">
      <div className="space-y-5">
        <section className="portal-card-padded"><h2 className="mb-4 font-semibold">1. Nguyên nhân gốc rễ</h2><div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{ROOT_CAUSES.map((cause) => <button type="button" key={cause.id} onClick={() => setRootCause(cause.id)} className={`rounded-lg border p-4 text-left ${rootCause === cause.id ? 'border-primary bg-primary-container/30' : 'border-outline-variant'}`}><span className="material-symbols-outlined text-primary">{cause.icon}</span><span className="mt-1 block text-sm font-semibold">{cause.label}</span></button>)}</div>{submitted && !rootCause && <p role="alert" className="mt-2 text-xs text-error">Vui lòng chọn nguyên nhân gốc rễ.</p>}</section>
        <section className="portal-card-padded"><label className="mb-2 block font-semibold" htmlFor="affected-scope">2. Phạm vi bị ảnh hưởng</label><input id="affected-scope" className="portal-field w-full" value={affectedScope} maxLength={500} onChange={(e) => setAffectedScope(e.target.value)} placeholder="Ví dụ: 01 tài khoản quản trị, session phát sinh từ IP nguồn" />{submitted && !affectedScope.trim() && <p role="alert" className="mt-2 text-xs text-error">Vui lòng mô tả phạm vi bị ảnh hưởng.</p>}</section>
        <section className="portal-card-padded"><label className="mb-2 block font-semibold" htmlFor="resolution-summary">3. Kết luận xử lý</label><textarea id="resolution-summary" value={summary} maxLength={5000} onChange={(e) => setSummary(e.target.value)} rows={5} className="w-full rounded-md border border-outline-variant p-3 text-sm outline-none focus:border-primary" placeholder="Mô tả bằng chứng kết luận, hành động đã thực hiện và biện pháp ngăn tái diễn..." /><div className="mt-1 flex justify-between text-xs text-outline"><span>{submitted && summary.trim().length < 20 ? <span className="text-error" role="alert">Tóm tắt cần ít nhất 20 ký tự.</span> : 'Bắt buộc, tối đa 5.000 ký tự'}</span><span>{summary.length}/5000</span></div></section>
        <section className="portal-card-padded"><h2 className="mb-3 font-semibold">4. Hành động khắc phục đã hoàn tất</h2>{tasks.map((task, index) => <label key={task.text} className="flex cursor-pointer items-center gap-3 border-b border-outline-variant/50 py-3 text-sm"><input type="checkbox" checked={task.done} onChange={() => setTasks((current) => current.map((item, i) => i === index ? { ...item, done: !item.done } : item))} />{task.text}</label>)}{submitted && !tasks.some((task) => task.done) && <p role="alert" className="mt-2 text-xs text-error">Xác nhận ít nhất một hành động khắc phục.</p>}</section>
      </div>
      <aside className="space-y-5"><section className="portal-card-padded"><h2 className="mb-3 font-semibold">Thông báo</h2><label className="flex items-start gap-3 text-sm"><input type="checkbox" checked={notifyAffected} onChange={(e) => setNotifyAffected(e.target.checked)} /><span>Đã quyết định cần thông báo cho bên bị ảnh hưởng theo chính sách hiện hành.</span></label></section><section className="portal-card-padded"><h2 className="mb-3 font-semibold">Xác nhận đóng case</h2><p className="mb-3 text-sm text-on-surface-variant">Thao tác không xóa bằng chứng. Kết luận được nối thêm vào audit timeline.</p><label className="flex items-start gap-3 text-sm font-medium"><input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)} />Tôi xác nhận thông tin đầy đủ và chính xác.</label>{submitted && !confirmed && <p role="alert" className="mt-2 text-xs text-error">Cần xác nhận trước khi đóng.</p>}<button className="portal-primary-button mt-5 w-full" disabled={isSaving} onClick={() => void handleResolve()}>{isSaving ? 'Đang đóng sự cố...' : 'Đóng sự cố'}</button></section></aside>
    </div>
  </div></div>;
}
