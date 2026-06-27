import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';

interface SecurityEvent {
  id: number; eventType: string; userId: string | null; ipAddress: string;
  severity: string; status: string; details: string; occurredAt: string;
  correlationId: string | null; reviewedBy: string | null; reviewedAt: string | null;
}

interface TimelineEntry {
  id: number; eventType: string; ipAddress: string; details: string;
  severity: string; status: string; occurredAt: string;
}

export default function SecurityEventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const [event, setEvent] = useState<SecurityEvent | null>(null);
  const [timeline, setTimeline] = useState<TimelineEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const res = await apiClient.get(`/api/v1/admin/security-events?page=0&size=1`);
        const found = (res.data.data.content as SecurityEvent[]).find(e => e.id === Number(eventId));
        if (found) {
          setEvent(found);
          if (found.correlationId) {
            const tRes = await apiClient.get(`/api/v1/admin/security-events/timeline?correlationId=${found.correlationId}`);
            setTimeline(tRes.data.data);
          }
        }
      } catch { /* */ } finally { setIsLoading(false); }
    })();
  }, [eventId]);

  if (isLoading) return <div style={{ padding: 48, textAlign: 'center', color: '#84736f', fontFamily: "'Lexend'" }}>Đang tải...</div>;
  if (!event) return <div style={{ padding: 48, textAlign: 'center', color: '#ba1a1a', fontFamily: "'Lexend'" }}>Không tìm thấy sự cố.</div>;

  const sevColor = event.severity === 'HIGH' ? '#ba1a1a' : event.severity === 'MEDIUM' ? '#845143' : '#6e5a52';

  return (
    <div style={{ fontFamily: "'Lexend', sans-serif" }}>
      <div style={{ fontSize: 14, color: '#524440', marginBottom: 8 }}>Bảo mật &gt; Sự cố CB-{event.id}</div>
      <h1 style={{ fontSize: 20, fontWeight: 600, color: '#271812', margin: '0 0 24px' }}>Chi tiết sự cố: {event.eventType}</h1>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginBottom: 24 }}>
        <div style={cardStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <span style={{ fontSize: 14, color: '#84736f', fontWeight: 500 }}>MỨC ĐỘ NGUY HIỂM</span>
            <span style={{ padding: '4px 12px', borderRadius: 8, background: sevColor === '#ba1a1a' ? '#ffdad6' : '#ffe9e3', color: sevColor, fontSize: 12, fontWeight: 700 }}>
              {event.severity === 'HIGH' ? 'High Risk' : event.severity}
            </span>
          </div>
          <div style={{ fontSize: 14, color: '#524440', marginBottom: 8 }}>Mã sự cố: <strong>CB-{event.id}</strong></div>
          <div style={{ fontSize: 14, color: '#524440', marginBottom: 8 }}>Thời gian: <strong>{new Date(event.occurredAt).toLocaleString('vi-VN')}</strong></div>
          <div style={{ fontSize: 14, color: '#524440' }}>Loại hình: <strong>{event.eventType}</strong></div>
        </div>

        <div style={cardStyle}>
          <h3 style={{ fontSize: 16, fontWeight: 600, color: '#271812', margin: '0 0 16px' }}>Đối tượng bị ảnh hưởng & Chỉ số rủi ro</h3>
          <div style={{ fontSize: 14, color: '#524440' }}>Tài khoản: <strong>{event.userId || '—'}</strong></div>
          <div style={{ fontSize: 14, color: '#524440', marginTop: 8 }}>Nguồn (IP): <strong>{event.ipAddress}</strong></div>
        </div>
      </div>

      <div style={cardStyle}>
        <h3 style={{ fontSize: 16, fontWeight: 600, color: '#271812', margin: '0 0 16px' }}>Dòng thời gian bằng chứng (Log entries)</h3>
        {timeline.length === 0 ? (
          <p style={{ color: '#84736f', fontSize: 14 }}>Không có dữ liệu timeline.</p>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #fadcd3', textAlign: 'left' }}>
                {['Timestamp', 'Hoạt động', 'Nguồn', 'Kết quả', 'Chi tiết'].map(h => (
                  <th key={h} style={{ padding: '10px 8px', fontSize: 12, fontWeight: 600, color: '#84736f' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {timeline.map(t => (
                <tr key={t.id} style={{ borderBottom: '1px solid #fadcd3' }}>
                  <td style={{ padding: '12px 8px', fontSize: 13, color: '#271812' }}>{new Date(t.occurredAt).toLocaleTimeString('vi-VN')}</td>
                  <td style={{ padding: '12px 8px', fontSize: 13, fontWeight: 600, color: t.status === 'RESOLVED' ? '#845143' : '#271812' }}>{t.eventType}</td>
                  <td style={{ padding: '12px 8px', fontSize: 13, color: '#524440' }}>{t.ipAddress}</td>
                  <td style={{ padding: '12px 8px' }}>
                    <span style={{ color: t.status === 'RESOLVED' ? '#845143' : '#ba1a1a', fontSize: 12, fontWeight: 500 }}>
                      {t.status === 'RESOLVED' ? 'Thành công' : 'Thất bại'}
                    </span>
                  </td>
                  <td style={{ padding: '12px 8px', fontSize: 12, color: '#524440' }}>{t.details?.substring(0, 40)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

const cardStyle: React.CSSProperties = { background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' };
