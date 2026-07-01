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

const card = 'bg-surface rounded-2xl p-6 shadow-sm';

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
    <div>
      <div className="text-sm text-on-surface-variant mb-2">An toàn &gt; Quản lý sự cố &gt; CB-{eventId}</div>
      <div className="flex items-center gap-3 mb-6">
        <h1 className="text-[22px] font-bold text-on-surface m-0">Giải quyết sự cố CB-{eventId}</h1>
        <span className="px-3 py-1 rounded-full bg-error-container text-error text-xs font-bold">! Mức độ: Cao</span>
      </div>

      <div className="grid grid-cols-[2fr_1fr] gap-6">
        <div className="flex flex-col gap-6">
          <div className={card}>
            <h2 className="text-base font-semibold text-on-surface mt-0 mb-4">1. Phân loại nguyên nhân gốc rễ (Root Cause)</h2>
            <div className="grid grid-cols-3 gap-3">
              {ROOT_CAUSES.map(rc => (
                <label
                  key={rc.id}
                  onClick={() => setRootCause(rc.id)}
                  className={`p-4 rounded-xl border-2 cursor-pointer text-center transition-colors ${rootCause === rc.id ? 'border-primary bg-surface-container-low' : 'border-outline-variant bg-surface'}`}
                >
                  <span className="material-symbols-outlined text-primary block mb-2 text-2xl">{rc.icon}</span>
                  <div className="text-sm font-semibold text-on-surface">{rc.label}</div>
                  <div className="text-xs text-on-surface-variant mt-1">{rc.description}</div>
                </label>
              ))}
            </div>
          </div>

          <div className={card}>
            <h2 className="text-base font-semibold text-on-surface mt-0 mb-4">2. Tóm tắt quá trình giải quyết</h2>
            <textarea
              value={summary}
              onChange={e => setSummary(e.target.value)}
              rows={4}
              placeholder="Mô tả chi tiết các bước đã thực hiện để khắc phục và ngăn chặn sự cố tái diễn..."
              className="w-full px-4 py-4 rounded-xl border border-outline-variant text-sm resize-y"
            />
          </div>

          <div className={card}>
            <h2 className="text-base font-semibold text-on-surface mt-0 mb-4">3. Danh mục nhiệm vụ khắc phục (Remediation)</h2>
            {tasks.map((t, i) => (
              <div
                key={i}
                className={`flex items-center gap-3 py-3 ${i < tasks.length - 1 ? 'border-b border-surface-container-highest' : ''}`}
              >
                <span
                  onClick={() => { const next = [...tasks]; next[i].done = !next[i].done; setTasks(next); }}
                  className={`material-symbols-outlined cursor-pointer text-2xl ${t.done ? 'text-primary' : 'text-outline-variant'}`}
                >
                  {t.done ? 'check_circle' : 'circle'}
                </span>
                <span className="flex-1 text-sm text-on-surface">{t.label}</span>
                <span className="px-2.5 py-0.5 rounded-full border border-outline-variant text-[11px] font-semibold text-primary">{t.tag}</span>
              </div>
            ))}
          </div>

          <button
            onClick={handleResolve}
            disabled={isSaving}
            className="self-start px-8 py-3.5 rounded-full bg-primary text-on-primary border-none text-base font-semibold cursor-pointer disabled:opacity-60"
          >
            {isSaving ? 'Đang lưu...' : 'Giải quyết sự cố'}
          </button>
        </div>

        <div className="flex flex-col gap-6">
          <div className={card}>
            <h3 className="text-base font-semibold text-on-surface mt-0 mb-4">Chi tiết Case</h3>
            {[['Mã sự cố', `CB-${eventId}`], ['Bắt đầu', '—'], ['Thời gian xử lý', '—'], ['Người phụ trách', '—']].map(([k, v]) => (
              <div key={k} className="flex justify-between py-2 text-sm">
                <span className="text-outline">{k}</span>
                <span className="font-semibold text-on-surface">{v}</span>
              </div>
            ))}
          </div>

          <div className={card}>
            <h3 className="text-base font-semibold text-on-surface mt-0 mb-4">Nhật ký hoạt động</h3>
            <div className="border-l-2 border-outline-variant pl-4 flex flex-col gap-4">
              <div>
                <div className="text-xs text-outline">Hiện tại</div>
                <div className="text-sm font-semibold text-on-surface">Đang điền form giải quyết</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
