import { useState, useEffect, useRef } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface Topic {
  id: number;
  name: string;
  description: string;
  icon: string;
  sortOrder: number;
  isHidden: boolean;
}

const s = {
  page: { minHeight: '100vh', background: '#F6F1EC', padding: '32px 24px', fontFamily: '"Nunito", system-ui, sans-serif' } as React.CSSProperties,
  header: { fontSize: 24, fontWeight: 900, color: '#5A463F', marginBottom: 24 } as React.CSSProperties,
  card: { background: '#FFFFFF', border: '1px solid rgba(232,221,214,0.5)', borderRadius: 32, boxShadow: '0 12px 32px rgba(90,70,63,0.06)', padding: 24, marginBottom: 20 } as React.CSSProperties,
  formTitle: { fontSize: 16, fontWeight: 700, color: '#5A463F', marginBottom: 16 } as React.CSSProperties,
  grid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 } as React.CSSProperties,
  field: { display: 'flex', flexDirection: 'column' as const, gap: 4 },
  label: { fontSize: 11, fontWeight: 700, color: '#9C857C', textTransform: 'uppercase' as const, letterSpacing: 1 } as React.CSSProperties,
  input: { padding: '10px 16px', borderRadius: 20, border: '2px solid transparent', background: '#F6F1EC', fontSize: 14, outline: 'none', color: '#5A463F' } as React.CSSProperties,
  btn: { padding: '10px 24px', borderRadius: 9999, border: 'none', background: '#C98C7B', color: 'white', fontWeight: 700, cursor: 'pointer', fontSize: 14, marginTop: 16, boxShadow: '0 8px 24px rgba(201,140,123,0.25)', transition: 'all 0.3s' } as React.CSSProperties,
  topicRow: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid rgba(232,221,214,0.6)' } as React.CSSProperties,
  topicName: { fontWeight: 700, color: '#5A463F', fontSize: 15 } as React.CSSProperties,
  topicDesc: { fontSize: 13, color: '#9C857C', marginTop: 2 } as React.CSSProperties,
  toggleBtn: (hidden: boolean) => ({
    padding: '6px 18px', borderRadius: 9999, border: 'none', fontSize: 13, fontWeight: 700, cursor: 'pointer',
    background: hidden ? 'rgba(201,140,123,0.12)' : 'rgba(90,70,63,0.08)',
    color: hidden ? '#C98C7B' : '#5A463F',
    transition: 'all 0.3s',
  } as React.CSSProperties),
  toast: (ok: boolean) => ({
    position: 'fixed' as const, top: 20, right: 20, zIndex: 1000,
    background: ok ? '#5A463F' : '#C98C7B',
    color: 'white',
    padding: '12px 24px', borderRadius: 9999, fontWeight: 700, fontSize: 14,
    boxShadow: '0 16px 36px rgba(90,70,63,0.15)',
  } as React.CSSProperties),
  error: { background: '#F2EAE4', border: '1px solid rgba(201,140,123,0.3)', borderRadius: 20, padding: '12px 16px', color: '#C98C7B', marginBottom: 16, fontSize: 14, fontWeight: 600 } as React.CSSProperties,
};

export default function ManageTopicsPage() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [error, setError] = useState('');
  const [toast, setToast] = useState<{ msg: string; ok: boolean } | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  const showToast = (msg: string, ok: boolean) => {
    setToast({ msg, ok });
    setTimeout(() => setToast(null), 3000);
  };

  const loadTopics = async () => {
    try {
      const { data } = await apiClient.get<Topic[]>('/api/v1/community/topics');
      setTopics(data);
    } catch {
      setError('Không thể tải danh sách chủ đề.');
    }
  };

  useEffect(() => { loadTopics(); }, []);

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      await apiClient.post('/api/v1/community/topics', {
        name: fd.get('name'),
        description: fd.get('description'),
        icon: fd.get('icon'),
        sortOrder: Number(fd.get('sortOrder')) || 0,
      });
      showToast('Tạo chủ đề thành công.', true);
      formRef.current?.reset();
      loadTopics();
    } catch {
      showToast('Tạo chủ đề thất bại.', false);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleHidden = async (topic: Topic) => {
    try {
      await apiClient.patch(`/api/v1/community/topics/${topic.id}`, { isHidden: !topic.isHidden });
      showToast(`Chủ đề đã ${topic.isHidden ? 'hiển thị' : 'ẩn'}.`, true);
      loadTopics();
    } catch {
      showToast('Cập nhật chủ đề thất bại.', false);
    }
  };

  return (
    <div style={s.page}>
      {toast && <div style={s.toast(toast.ok)}>{toast.msg}</div>}
      <div style={s.header}>Quản lý chủ đề cộng đồng</div>
      {error && <div style={s.error}>{error}</div>}

      <div style={s.card}>
        <div style={s.formTitle}>Tạo chủ đề mới</div>
        <form ref={formRef} onSubmit={handleCreate}>
          <div style={s.grid}>
            <div style={s.field}>
              <label style={s.label}>Tên *</label>
              <input name="name" required style={s.input} placeholder="Tên chủ đề" />
            </div>
            <div style={s.field}>
              <label style={s.label}>Biểu tượng</label>
              <input name="icon" style={s.input} placeholder="Ví dụ: 🍼" />
            </div>
            <div style={{ ...s.field, gridColumn: '1 / -1' }}>
              <label style={s.label}>Mô tả</label>
              <input name="description" style={s.input} placeholder="Mô tả ngắn" />
            </div>
            <div style={s.field}>
              <label style={s.label}>Thứ tự</label>
              <input name="sortOrder" type="number" style={s.input} placeholder="0" defaultValue={0} />
            </div>
          </div>
          <button type="submit" style={s.btn} disabled={submitting}>{submitting ? 'Đang tạo...' : 'Tạo chủ đề'}</button>
        </form>
      </div>

      <div style={s.card}>
        <div style={s.formTitle}>Tất cả chủ đề ({topics.length})</div>
        {topics.length === 0 ? (
          <div style={{ color: '#9C857C', fontSize: 14 }}>Không tìm thấy chủ đề nào.</div>
        ) : (
          topics.map((t) => (
            <div key={t.id} style={s.topicRow}>
              <div>
                <div style={s.topicName}>{t.icon} {t.name} <span style={{ fontWeight: 400, color: '#9C857C', fontSize: 13 }}>#{t.sortOrder}</span></div>
                <div style={s.topicDesc}>{t.description}</div>
              </div>
              <button style={s.toggleBtn(t.isHidden)} onClick={() => toggleHidden(t)}>
                {t.isHidden ? 'Hiện' : 'Ẩn'}
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
