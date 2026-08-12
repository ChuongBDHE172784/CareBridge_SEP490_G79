import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  addSecurityIncidentNote,
  getSecurityIncident,
  getSecurityIncidentNotes,
  getSecurityIncidentTimeline,
  startSecurityIncidentReview,
  type SecurityEvent,
  type SecurityEventNote,
} from '../services/securityIncidentApi';

export default function SecurityIncidentInvestigationPage() {
  const { eventId = '' } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const [incident, setIncident] = useState<SecurityEvent | null>(null);
  const [timeline, setTimeline] = useState<SecurityEvent[]>([]);
  const [notes, setNotes] = useState<SecurityEventNote[]>([]);
  const [newNote, setNewNote] = useState('');
  const [error, setError] = useState('');
  const [noteError, setNoteError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    let active = true;
    (async () => {
      setIsLoading(true); setError('');
      try {
        const detail = await getSecurityIncident(eventId);
        const [loadedNotes, loadedTimeline] = await Promise.all([
          getSecurityIncidentNotes(eventId),
          detail.correlationId ? getSecurityIncidentTimeline(detail.correlationId) : Promise.resolve([detail]),
        ]);
        if (active) { setIncident(detail); setNotes(loadedNotes); setTimeline(loadedTimeline); }
      } catch { if (active) setError('Không thể tải hồ sơ sự cố hoặc sự cố không tồn tại.'); }
      finally { if (active) setIsLoading(false); }
    })();
    return () => { active = false; };
  }, [eventId]);

  const addNote = async () => {
    const text = newNote.trim();
    if (!text) { setNoteError('Vui lòng nhập nội dung ghi chú.'); return; }
    setIsSaving(true); setNoteError('');
    try {
      const saved = await addSecurityIncidentNote(eventId, text);
      setNotes((current) => [...current, saved]); setNewNote('');
    } catch { setNoteError('Không thể lưu ghi chú. Vui lòng thử lại.'); }
    finally { setIsSaving(false); }
  };

  const startReview = async () => {
    setIsSaving(true); setError('');
    try { setIncident(await startSecurityIncidentReview(eventId)); }
    catch { setError('Không thể chuyển sự cố sang trạng thái điều tra.'); }
    finally { setIsSaving(false); }
  };

  if (isLoading) return <div className="portal-page p-6"><div className="portal-empty" role="status">Đang tải hồ sơ điều tra...</div></div>;
  if (!incident) return <div className="portal-page p-6"><div className="portal-error">{error}<button className="portal-secondary-button ml-3" onClick={() => navigate('/admin/security/incidents')}>Quay lại</button></div></div>;

  const closed = incident.status === 'RESOLVED' || incident.status === 'FALSE_POSITIVE';
  return (
    <div className="portal-page px-5 py-5 md:px-6 md:py-6"><div className="portal-contained">
      <button className="mb-4 inline-flex items-center gap-1 text-sm font-semibold text-primary" onClick={() => navigate('/admin/security/incidents')}><span className="material-symbols-outlined">arrow_back</span>Danh sách sự cố</button>
      <div className="portal-header">
        <div><p className="portal-eyebrow">SEC-{eventId} · {humanize(incident.eventType)}</p><h1 className="portal-title">Điều tra sự cố bảo mật</h1><p className="portal-subtitle">Bằng chứng chỉ hiển thị metadata vận hành, không hiển thị dữ liệu sức khỏe nhạy cảm.</p></div>
        <div className="flex flex-wrap gap-2">
          {incident.status === 'OPEN' && <button className="portal-primary-button" disabled={isSaving} onClick={() => void startReview()}><span className="material-symbols-outlined text-lg">person_add</span>Bắt đầu điều tra</button>}
          {incident.status === 'UNDER_REVIEW' && <button className="portal-primary-button" onClick={() => navigate(`/admin/security/incidents/${eventId}/resolve`)}><span className="material-symbols-outlined text-lg">task_alt</span>Chuyển sang giải quyết</button>}
        </div>
      </div>
      {error && <div className="portal-error mb-4">{error}</div>}

      <div className="grid gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(280px,1fr)]">
        <div className="space-y-5">
          <section className="portal-card-padded"><div className="mb-4 flex items-center justify-between"><h2 className="font-semibold">Timeline bằng chứng</h2><span className="text-xs text-outline">{timeline.length} sự kiện liên quan</span></div>
            <div className="space-y-4">{timeline.map((event) => <div key={event.id} className="border-l-2 border-primary-container pl-4"><div className="flex flex-wrap justify-between gap-2"><strong className="text-sm">{humanize(event.eventType)}</strong><time className="text-xs text-outline">{formatDateTime(event.occurredAt)}</time></div><p className="mt-1 text-sm text-on-surface-variant">{event.details || 'Không có mô tả bổ sung.'}</p><p className="mt-1 text-xs text-outline">Nguồn: {event.ipAddress || 'Nội bộ / đã ẩn'}</p></div>)}</div>
          </section>
          <section className="portal-card-padded"><h2 className="mb-3 font-semibold">Ghi chú điều tra</h2>
            {notes.length === 0 ? <p className="text-sm text-outline">Chưa có ghi chú điều tra.</p> : <div className="mb-5 space-y-3">{notes.map((note) => <article key={note.noteId} className="rounded-lg bg-surface-container-low p-3"><div className="mb-1 flex justify-between gap-2 text-xs text-outline"><span>Điều tra viên {shortId(note.authorId)}</span><time>{formatDateTime(note.createdAt)}</time></div><p className="whitespace-pre-wrap text-sm">{note.noteText}</p></article>)}</div>}
            {!closed && <><label className="portal-label" htmlFor="investigation-note">Ghi chú mới</label><textarea id="investigation-note" value={newNote} onChange={(e) => setNewNote(e.target.value)} maxLength={5000} rows={4} className="w-full rounded-md border border-outline-variant p-3 text-sm outline-none focus:border-primary" placeholder="Ghi nhận giả thuyết, bằng chứng hoặc hành động khoanh vùng..." />{noteError && <p className="mt-1 text-xs text-error" role="alert">{noteError}</p>}<button className="portal-primary-button mt-3" disabled={isSaving} onClick={() => void addNote()}>{isSaving ? 'Đang lưu...' : 'Lưu ghi chú'}</button></>}
          </section>
        </div>
        <aside className="space-y-5">
          <section className="portal-card-padded"><h2 className="mb-4 font-semibold">Tổng quan case</h2>{[['Trạng thái', statusLabel(incident.status)], ['Mức độ', incident.severity], ['IP nguồn', incident.ipAddress || 'Không xác định'], ['Người bị ảnh hưởng', incident.userId ? `Tài khoản ${shortId(incident.userId)}` : 'Chưa xác định'], ['Người rà soát', incident.reviewedBy ? shortId(incident.reviewedBy) : 'Chưa phân công']].map(([label, value]) => <div key={label} className="flex justify-between gap-4 border-b border-outline-variant/50 py-2 text-sm"><span className="text-outline">{label}</span><strong className="text-right">{value}</strong></div>)}</section>
          <section className="portal-card-padded"><h2 className="mb-3 font-semibold">Khoanh vùng</h2><p className="text-sm text-on-surface-variant">Các hành động khóa tài khoản, thu hồi session hoặc chặn nguồn phải thực hiện trong module quản trị tương ứng và sẽ được ghi audit riêng.</p></section>
        </aside>
      </div>
    </div></div>
  );
}

function shortId(value: string) { return `${value.slice(0, 8)}…`; }
function humanize(value: string) { return value.split('_').map((p) => p.charAt(0) + p.slice(1).toLowerCase()).join(' '); }
function formatDateTime(value: string) { return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)); }
function statusLabel(value: string) { return ({ OPEN: 'Đang mở', UNDER_REVIEW: 'Đang điều tra', RESOLVED: 'Đã giải quyết', FALSE_POSITIVE: 'Không phải sự cố' } as Record<string, string>)[value] ?? value; }
