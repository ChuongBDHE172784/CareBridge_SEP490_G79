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

const card = 'bg-surface rounded-2xl p-6 shadow-sm';

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

  if (isLoading) return <div className="p-12 text-center text-outline">Đang tải...</div>;
  if (!event) return <div className="p-12 text-center text-error">Không tìm thấy sự cố.</div>;

  const sevBadgeCls = event.severity === 'HIGH'
    ? 'bg-error-container text-error'
    : 'bg-surface-container-high text-primary';

  return (
    <div>
      <div className="text-sm text-on-surface-variant mb-2">Bảo mật &gt; Sự cố CB-{event.id}</div>
      <h1 className="text-xl font-semibold text-on-surface mt-0 mb-6">Chi tiết sự cố: {event.eventType}</h1>

      <div className="grid grid-cols-2 gap-6 mb-6">
        <div className={card}>
          <div className="flex justify-between items-center mb-4">
            <span className="text-sm text-outline font-medium">MỨC ĐỘ NGUY HIỂM</span>
            <span className={`px-3 py-1 rounded-lg text-xs font-bold ${sevBadgeCls}`}>
              {event.severity === 'HIGH' ? 'High Risk' : event.severity}
            </span>
          </div>
          <div className="text-sm text-on-surface-variant mb-2">Mã sự cố: <strong>CB-{event.id}</strong></div>
          <div className="text-sm text-on-surface-variant mb-2">Thời gian: <strong>{new Date(event.occurredAt).toLocaleString('vi-VN')}</strong></div>
          <div className="text-sm text-on-surface-variant">Loại hình: <strong>{event.eventType}</strong></div>
        </div>

        <div className={card}>
          <h3 className="text-base font-semibold text-on-surface mt-0 mb-4">Đối tượng bị ảnh hưởng &amp; Chỉ số rủi ro</h3>
          <div className="text-sm text-on-surface-variant">Tài khoản: <strong>{event.userId || '—'}</strong></div>
          <div className="text-sm text-on-surface-variant mt-2">Nguồn (IP): <strong>{event.ipAddress}</strong></div>
        </div>
      </div>

      <div className={card}>
        <h3 className="text-base font-semibold text-on-surface mt-0 mb-4">Dòng thời gian bằng chứng (Log entries)</h3>
        {timeline.length === 0 ? (
          <p className="text-outline text-sm">Không có dữ liệu timeline.</p>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b border-surface-container-highest text-left">
                {['Timestamp', 'Hoạt động', 'Nguồn', 'Kết quả', 'Chi tiết'].map(h => (
                  <th key={h} className="px-2 py-2.5 text-xs font-semibold text-outline">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {timeline.map(t => (
                <tr key={t.id} className="border-b border-surface-container-highest">
                  <td className="px-2 py-3 text-[13px] text-on-surface">{new Date(t.occurredAt).toLocaleTimeString('vi-VN')}</td>
                  <td className={`px-2 py-3 text-[13px] font-semibold ${t.status === 'RESOLVED' ? 'text-primary' : 'text-on-surface'}`}>{t.eventType}</td>
                  <td className="px-2 py-3 text-[13px] text-on-surface-variant">{t.ipAddress}</td>
                  <td className="px-2 py-3">
                    <span className={`text-xs font-medium ${t.status === 'RESOLVED' ? 'text-primary' : 'text-error'}`}>
                      {t.status === 'RESOLVED' ? 'Thành công' : 'Thất bại'}
                    </span>
                  </td>
                  <td className="px-2 py-3 text-xs text-on-surface-variant">{t.details?.substring(0, 40)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
