import { useState } from 'react';
import apiClient from '../../../shared/api/apiClient';

const s = {
  page: { minHeight: '100vh', background: '#F6F1EC', padding: '32px 24px', fontFamily: '"Nunito", system-ui, sans-serif' } as React.CSSProperties,
  header: { fontSize: 24, fontWeight: 900, color: '#5A463F', marginBottom: 24 } as React.CSSProperties,
  card: { background: '#FFFFFF', border: '1px solid rgba(232,221,214,0.5)', borderRadius: 32, boxShadow: '0 12px 32px rgba(90,70,63,0.06)', padding: 28, maxWidth: 680 } as React.CSSProperties,
  grid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 } as React.CSSProperties,
  field: { display: 'flex', flexDirection: 'column' as const, gap: 6, marginBottom: 16 } as React.CSSProperties,
  label: { fontSize: 11, fontWeight: 700, color: '#9C857C', textTransform: 'uppercase' as const, letterSpacing: 1 } as React.CSSProperties,
  input: { padding: '12px 16px', borderRadius: 20, border: '2px solid transparent', background: '#F6F1EC', fontSize: 14, outline: 'none', color: '#5A463F', width: '100%', boxSizing: 'border-box' as const } as React.CSSProperties,
  select: { padding: '12px 16px', borderRadius: 20, border: '2px solid transparent', background: '#F6F1EC', fontSize: 14, outline: 'none', color: '#5A463F' } as React.CSSProperties,
  textarea: { padding: '12px 16px', borderRadius: 20, border: '2px solid transparent', background: '#F6F1EC', fontSize: 14, outline: 'none', color: '#5A463F', width: '100%', boxSizing: 'border-box' as const, minHeight: 140, resize: 'vertical' as const } as React.CSSProperties,
  btn: { padding: '12px 32px', borderRadius: 9999, border: 'none', background: '#C98C7B', color: 'white', fontWeight: 700, cursor: 'pointer', fontSize: 15, boxShadow: '0 8px 24px rgba(201,140,123,0.25)', transition: 'all 0.3s' } as React.CSSProperties,
  btnDisabled: { padding: '12px 32px', borderRadius: 9999, border: 'none', background: '#F2EAE4', color: '#9C857C', fontWeight: 700, cursor: 'not-allowed', fontSize: 15 } as React.CSSProperties,
  success: { background: '#F2EAE4', border: '1px solid rgba(90,70,63,0.2)', borderRadius: 20, padding: '14px 20px', color: '#5A463F', fontSize: 14, fontWeight: 700, marginBottom: 20 } as React.CSSProperties,
  error: { background: '#F2EAE4', border: '1px solid rgba(201,140,123,0.3)', borderRadius: 20, padding: '14px 20px', color: '#C98C7B', fontSize: 14, fontWeight: 700, marginBottom: 20 } as React.CSSProperties,
};

export default function CreateContentPage() {
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState<{ id: string } | null>(null);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setSuccess(null);
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      const { data } = await apiClient.post<{ id: string }>('/api/v1/admin/content', {
        type: fd.get('type'),
        title: fd.get('title'),
        body: fd.get('body'),
        stage: fd.get('stage'),
      });
      setSuccess({ id: data.id });
      (e.target as HTMLFormElement).reset();
    } catch {
      setError('Tạo nội dung thất bại. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={s.page}>
      <div style={s.header}>Tạo nội dung</div>
      {success && <div style={s.success}>Tạo nội dung thành công! Mã: <strong>{success.id}</strong></div>}
      {error && <div style={s.error}>{error}</div>}
      <div style={s.card}>
        <form onSubmit={handleSubmit}>
          <div style={s.grid}>
            <div style={s.field}>
              <label style={s.label}>Loại *</label>
              <select name="type" required style={s.select}>
                <option value="">Chọn loại</option>
                <option value="ARTICLE">Bài viết</option>
                <option value="FAQ">Hỏi đáp</option>
                <option value="CHECKLIST">Danh sách cần làm</option>
              </select>
            </div>
            <div style={s.field}>
              <label style={s.label}>Giai đoạn *</label>
              <select name="stage" required style={s.select}>
                <option value="">Chọn giai đoạn</option>
                <option value="PREGNANCY">Thai kỳ</option>
                <option value="POSTPARTUM">Sau sinh</option>
                <option value="BABY_CARE">Chăm sóc bé</option>
                <option value="GENERAL">Chung</option>
              </select>
            </div>
          </div>
          <div style={s.field}>
            <label style={s.label}>Tiêu đề *</label>
            <input name="title" required style={s.input} placeholder="Tiêu đề nội dung" />
          </div>
          <div style={s.field}>
            <label style={s.label}>Nội dung *</label>
            <textarea name="body" required style={s.textarea} placeholder="Nhập nội dung tại đây..." />
          </div>
          <button type="submit" style={submitting ? s.btnDisabled : s.btn} disabled={submitting}>
            {submitting ? 'Đang tạo...' : 'Tạo nội dung'}
          </button>
        </form>
      </div>
    </div>
  );
}
