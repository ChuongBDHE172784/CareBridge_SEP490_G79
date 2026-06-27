import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';

interface Note { id: number; content: string; authorId: string; createdAt: string; }
interface TimelineEntry { id: number; eventType: string; details: string; severity: string; occurredAt: string; ipAddress: string; }

export default function SecurityIncidentInvestigationPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const [notes, setNotes] = useState<Note[]>([]);
  const [timeline, setTimeline] = useState<TimelineEntry[]>([]);
  const [newNote, setNewNote] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [notesRes] = await Promise.all([
          apiClient.get(`/api/v1/admin/security-events/${eventId}/notes`),
        ]);
        setNotes(notesRes.data.data);
      } catch { /* */ } finally { setIsLoading(false); }
    })();
  }, [eventId]);

  const addNote = async () => {
    if (!newNote.trim()) return;
    try {
      const res = await apiClient.post(`/api/v1/admin/security-events/${eventId}/notes`, { content: newNote });
      setNotes(prev => [res.data.data, ...prev]);
      setNewNote('');
    } catch { /* */ }
  };

  return (
    <div style={{ fontFamily: "'Lexend', sans-serif" }}>
      <div style={{ fontSize: 14, color: '#524440', marginBottom: 8 }}>An toàn &gt; Quản lý sự cố &gt; CB-{eventId}</div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
        <span className="material-symbols-outlined" style={{ fontSize: 32, color: '#ba1a1a', background: '#ffdad6', borderRadius: '50%', padding: 8 }}>warning</span>
        <div>
          <h1 style={{ fontSize: 22, fontWeight: 700, color: '#271812', margin: 0 }}>Sự cố CB-{eventId}</h1>
          <p style={{ fontSize: 14, color: '#524440', margin: '4px 0 0' }}>Điều tra sự cố bảo mật</p>
        </div>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 12 }}>
          <button style={{ padding: '10px 20px', borderRadius: 9999, background: '#845143', color: '#fff', border: 'none', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>Phân công</button>
          <button style={{ padding: '10px 20px', borderRadius: 9999, background: '#fff', color: '#845143', border: '1px solid #d6c2bd', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>Xuất báo cáo</button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24 }}>
        <div>
          <div style={cardStyle}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h2 style={{ fontSize: 18, fontWeight: 600, color: '#271812', margin: 0 }}>Lịch trình bằng chứng</h2>
              <button onClick={addNote} style={{ padding: '6px 16px', borderRadius: 9999, border: '1px solid #845143', background: '#fff', color: '#845143', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}>+ Thêm bằng chứng</button>
            </div>

            {isLoading ? <p style={{ color: '#84736f' }}>Đang tải...</p> : notes.length === 0 ? <p style={{ color: '#84736f' }}>Chưa có ghi chú.</p> : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {notes.map(n => (
                  <div key={n.id} style={{ borderLeft: '3px solid #c98c7b', paddingLeft: 16, paddingBottom: 8 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#84736f', marginBottom: 4 }}>
                      <span>{n.authorId}</span>
                      <span>{new Date(n.createdAt).toLocaleTimeString('vi-VN')}</span>
                    </div>
                    <p style={{ fontSize: 14, color: '#271812', margin: 0 }}>{n.content}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div style={{ ...cardStyle, marginTop: 24 }}>
            <h2 style={{ fontSize: 18, fontWeight: 600, color: '#271812', margin: '0 0 16px' }}>Giả thuyết & Ghi chú</h2>
            <textarea value={newNote} onChange={e => setNewNote(e.target.value)} rows={3} placeholder="Thêm ghi chú điều tra..."
              style={{ width: '100%', padding: 16, borderRadius: 12, border: '1px solid #d6c2bd', fontSize: 14, fontFamily: "'Lexend'", resize: 'vertical' }} />
            <button onClick={addNote} style={{ marginTop: 12, padding: '10px 24px', borderRadius: 9999, background: '#845143', color: '#fff', border: 'none', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
              Ghi chú mới
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <div style={cardStyle}>
            <h3 style={{ fontSize: 16, fontWeight: 600, color: '#271812', margin: '0 0 16px' }}>Trạng thái khoanh vùng</h3>
            {['Khóa tài khoản liên quan', 'Chặn IP nguồn', 'Đặt lại toàn bộ Session'].map((action, i) => (
              <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', borderBottom: i < 2 ? '1px solid #fadcd3' : 'none' }}>
                <span style={{ fontSize: 14, color: '#271812' }}>{action}</span>
                <span style={{ padding: '4px 12px', borderRadius: 9999, fontSize: 12, fontWeight: 600, background: i < 2 ? '#845143' : '#fff1ec', color: i < 2 ? '#fff' : '#845143' }}>
                  {i < 2 ? 'ĐÃ THỰC HIỆN' : 'CHƯA XỬ LÝ'}
                </span>
              </div>
            ))}
          </div>

          <div style={cardStyle}>
            <h3 style={{ fontSize: 16, fontWeight: 600, color: '#271812', margin: '0 0 16px' }}>Tài khoản bị ảnh hưởng</h3>
            <p style={{ fontSize: 14, color: '#84736f' }}>Dữ liệu sẽ hiển thị khi có correlationId.</p>
          </div>
        </div>
      </div>
    </div>
  );
}

const cardStyle: React.CSSProperties = { background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' };
