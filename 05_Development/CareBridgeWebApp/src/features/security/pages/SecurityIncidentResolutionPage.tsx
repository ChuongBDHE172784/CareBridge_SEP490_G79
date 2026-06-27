import { useState } from 'react';
import { useParams } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';

const ROOT_CAUSES = [
  { id: 'software', icon: 'bug_report', label: 'Lỗi phần mềm', description: 'Lỗ hổng mã nguồn' },
  { id: 'auth', icon: 'vpn_key', label: 'Xác thực yếu', description: 'Rò rỉ mật khẩu/token' },
  { id: 'internal', icon: 'admin_panel_settings', label: 'Nội bộ', description: 'Thao tác sai từ admin' },
];

const REMEDIATION_TASKS = [
  { label: 'Thu hồi toàn bộ Token truy cập bị ảnh hưởng', tag: 'SECURITY', done: true },
  { label: 'Cập nhật bản vá định danh hệ thống', tag: 'DEVOPS', done: true },
  { label: 'Kiểm tra log truy cập trong 48h gần nhất', tag: 'AUDIT', done: false },
];

export default function SecurityIncidentResolutionPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const [rootCause, setRootCause] = useState('software');
  const [summary, setSummary] = useState('');
  const [tasks, setTasks] = useState(REMEDIATION_TASKS.map(t => ({ ...t })));
  const [isSaving, setIsSaving] = useState(false);

  const handleResolve = async () => {
    setIsSaving(true);
    try {
      await apiClient.put(`/api/v1/admin/security-events/${eventId}/review`, {
        status: 'RESOLVED',
        notes: `Root cause: ${rootCause}. Summary: ${summary}`,
      });
      alert('Sự cố đã được giải quyết!');
    } catch { alert('Lỗi khi giải quyết sự cố.'); }
    finally { setIsSaving(false); }
  };

  return (
    <div style={{ fontFamily: "'Lexend', sans-serif" }}>
      <div style={{ fontSize: 14, color: '#524440', marginBottom: 8 }}>An toàn &gt; Quản lý sự cố &gt; CB-{eventId}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, color: '#271812', margin: 0 }}>Giải quyết sự cố CB-{eventId}</h1>
        <span style={{ padding: '4px 12px', borderRadius: 9999, background: '#ffdad6', color: '#ba1a1a', fontSize: 12, fontWeight: 700 }}>! Mức độ: Cao</span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <div style={cardStyle}>
            <h2 style={sectionTitle}>1. Phân loại nguyên nhân gốc rễ (Root Cause)</h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              {ROOT_CAUSES.map(rc => (
                <label key={rc.id} onClick={() => setRootCause(rc.id)}
                  style={{ padding: 16, borderRadius: 12, border: `2px solid ${rootCause === rc.id ? '#845143' : '#d6c2bd'}`, background: rootCause === rc.id ? '#fff1ec' : '#fff', cursor: 'pointer', textAlign: 'center' }}>
                  <span className="material-symbols-outlined" style={{ fontSize: 24, color: '#845143', display: 'block', marginBottom: 8 }}>{rc.icon}</span>
                  <div style={{ fontSize: 14, fontWeight: 600, color: '#271812' }}>{rc.label}</div>
                  <div style={{ fontSize: 12, color: '#524440', marginTop: 4 }}>{rc.description}</div>
                </label>
              ))}
            </div>
          </div>

          <div style={cardStyle}>
            <h2 style={sectionTitle}>2. Tóm tắt quá trình giải quyết</h2>
            <textarea value={summary} onChange={e => setSummary(e.target.value)} rows={4} placeholder="Mô tả chi tiết các bước đã thực hiện để khắc phục và ngăn chặn sự cố tái diễn..."
              style={{ width: '100%', padding: 16, borderRadius: 12, border: '1px solid #d6c2bd', fontSize: 14, fontFamily: "'Lexend'", resize: 'vertical' }} />
          </div>

          <div style={cardStyle}>
            <h2 style={sectionTitle}>3. Danh mục nhiệm vụ khắc phục (Remediation)</h2>
            {tasks.map((t, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0', borderBottom: i < tasks.length - 1 ? '1px solid #fadcd3' : 'none' }}>
                <span onClick={() => { const next = [...tasks]; next[i].done = !next[i].done; setTasks(next); }}
                  className="material-symbols-outlined" style={{ fontSize: 24, color: t.done ? '#845143' : '#d6c2bd', cursor: 'pointer' }}>
                  {t.done ? 'check_circle' : 'circle'}
                </span>
                <span style={{ flex: 1, fontSize: 14, color: '#271812', textDecoration: t.done ? 'none' : 'none' }}>{t.label}</span>
                <span style={{ padding: '3px 10px', borderRadius: 9999, border: '1px solid #d6c2bd', fontSize: 11, fontWeight: 600, color: '#845143' }}>{t.tag}</span>
              </div>
            ))}
          </div>

          <button onClick={handleResolve} disabled={isSaving}
            style={{ padding: '14px 32px', borderRadius: 9999, background: '#845143', color: '#fff', border: 'none', fontSize: 16, fontWeight: 600, cursor: 'pointer', alignSelf: 'flex-start' }}>
            {isSaving ? 'Đang lưu...' : 'Giải quyết sự cố'}
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <div style={cardStyle}>
            <h3 style={{ fontSize: 16, fontWeight: 600, color: '#271812', margin: '0 0 16px' }}>Chi tiết Case</h3>
            {[['Mã sự cố', `CB-${eventId}`], ['Bắt đầu', '—'], ['Thời gian xử lý', '—'], ['Người phụ trách', '—']].map(([k, v]) => (
              <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', fontSize: 14 }}>
                <span style={{ color: '#84736f' }}>{k}</span>
                <span style={{ fontWeight: 600, color: '#271812' }}>{v}</span>
              </div>
            ))}
          </div>

          <div style={cardStyle}>
            <h3 style={{ fontSize: 16, fontWeight: 600, color: '#271812', margin: '0 0 16px' }}>Nhật ký hoạt động</h3>
            <div style={{ borderLeft: '2px solid #d6c2bd', paddingLeft: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div>
                <div style={{ fontSize: 12, color: '#84736f' }}>Hiện tại</div>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#271812' }}>Đang điền form giải quyết</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

const cardStyle: React.CSSProperties = { background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' };
const sectionTitle: React.CSSProperties = { fontSize: 16, fontWeight: 600, color: '#271812', margin: '0 0 16px' };
