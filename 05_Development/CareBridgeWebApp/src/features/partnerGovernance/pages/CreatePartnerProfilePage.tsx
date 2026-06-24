import { useState } from 'react';
import apiClient from '../../../shared/api/apiClient';

const s = {
  page: { minHeight: '100vh', background: '#F6F1EC', padding: '32px 24px', fontFamily: '"Nunito", system-ui, sans-serif' } as React.CSSProperties,
  header: { fontSize: 24, fontWeight: 900, color: '#5A463F', marginBottom: 24 } as React.CSSProperties,
  card: { background: '#FFFFFF', border: '1px solid rgba(232,221,214,0.5)', borderRadius: 32, boxShadow: '0 12px 32px rgba(90,70,63,0.06)', padding: 28, maxWidth: 680 } as React.CSSProperties,
  grid2: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 } as React.CSSProperties,
  field: { display: 'flex', flexDirection: 'column' as const, gap: 6, marginBottom: 16 } as React.CSSProperties,
  label: { fontSize: 11, fontWeight: 700, color: '#9C857C', textTransform: 'uppercase' as const, letterSpacing: 1 } as React.CSSProperties,
  input: { padding: '12px 16px', borderRadius: 20, border: '2px solid transparent', background: '#F6F1EC', fontSize: 14, outline: 'none', color: '#5A463F', width: '100%', boxSizing: 'border-box' as const } as React.CSSProperties,
  select: { padding: '12px 16px', borderRadius: 20, border: '2px solid transparent', background: '#F6F1EC', fontSize: 14, outline: 'none', color: '#5A463F', width: '100%' } as React.CSSProperties,
  textarea: { padding: '12px 16px', borderRadius: 20, border: '2px solid transparent', background: '#F6F1EC', fontSize: 14, outline: 'none', color: '#5A463F', width: '100%', boxSizing: 'border-box' as const, minHeight: 90, resize: 'vertical' as const } as React.CSSProperties,
  optionalTag: { fontSize: 11, fontWeight: 400, color: '#9C857C', marginLeft: 4 } as React.CSSProperties,
  btn: { padding: '12px 32px', borderRadius: 9999, border: 'none', background: '#C98C7B', color: 'white', fontWeight: 700, cursor: 'pointer', fontSize: 15, boxShadow: '0 8px 24px rgba(201,140,123,0.25)', transition: 'all 0.3s' } as React.CSSProperties,
  btnDisabled: { padding: '12px 32px', borderRadius: 9999, border: 'none', background: '#F2EAE4', color: '#9C857C', fontWeight: 700, cursor: 'not-allowed', fontSize: 15 } as React.CSSProperties,
  success: { background: '#F2EAE4', border: '1px solid rgba(90,70,63,0.2)', borderRadius: 20, padding: '14px 20px', color: '#5A463F', fontSize: 14, fontWeight: 700, marginBottom: 20 } as React.CSSProperties,
  error: { background: '#F2EAE4', border: '1px solid rgba(201,140,123,0.3)', borderRadius: 20, padding: '14px 20px', color: '#C98C7B', fontSize: 14, fontWeight: 700, marginBottom: 20 } as React.CSSProperties,
  sectionLabel: { fontSize: 11, fontWeight: 700, color: '#C98C7B', textTransform: 'uppercase' as const, letterSpacing: 1.5, marginBottom: 12, marginTop: 8 } as React.CSSProperties,
};

export default function CreatePartnerProfilePage() {
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    const fd = new FormData(e.currentTarget);
    const payload: Record<string, string | undefined> = {
      name: fd.get('name') as string,
      type: fd.get('type') as string,
      address: fd.get('address') as string,
      city: fd.get('city') as string,
      phone: fd.get('phone') as string,
      email: fd.get('email') as string,
    };
    const website = fd.get('website') as string;
    const description = fd.get('description') as string;
    if (website) payload.website = website;
    if (description) payload.description = description;

    setSubmitting(true);
    try {
      await apiClient.post('/api/v1/partner/profile', payload);
      setSuccess('Đăng ký đối tác thành công.');
      (e.target as HTMLFormElement).reset();
    } catch {
      setError('Đăng ký thất bại. Vui lòng kiểm tra lại thông tin và thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={s.page}>
      <div style={s.header}>Đăng ký đối tác y tế</div>
      {success && <div style={s.success}>{success}</div>}
      {error && <div style={s.error}>{error}</div>}
      <div style={s.card}>
        <form onSubmit={handleSubmit}>
          <div style={s.sectionLabel}>Thông tin cơ bản</div>
          <div style={s.grid2}>
            <div style={s.field}>
              <label style={s.label}>Tên cơ sở *</label>
              <input name="name" required style={s.input} placeholder="Tên tổ chức / cơ sở" />
            </div>
            <div style={s.field}>
              <label style={s.label}>Loại *</label>
              <select name="type" required style={s.select}>
                <option value="">Chọn loại</option>
                <option value="CLINIC">Phòng khám</option>
                <option value="HOSPITAL">Bệnh viện</option>
                <option value="PHARMACY">Nhà thuốc</option>
                <option value="LAB">Phòng xét nghiệm</option>
                <option value="WELLNESS_CENTER">Trung tâm sức khỏe</option>
              </select>
            </div>
          </div>

          <div style={s.sectionLabel}>Liên hệ</div>
          <div style={s.grid2}>
            <div style={s.field}>
              <label style={s.label}>Số điện thoại *</label>
              <input name="phone" required style={s.input} placeholder="+84..." />
            </div>
            <div style={s.field}>
              <label style={s.label}>Email *</label>
              <input name="email" type="email" required style={s.input} placeholder="lienhe@doitac.com" />
            </div>
            <div style={s.field}>
              <label style={s.label}>Website <span style={s.optionalTag}>(tuỳ chọn)</span></label>
              <input name="website" style={s.input} placeholder="https://..." />
            </div>
          </div>

          <div style={s.sectionLabel}>Địa điểm</div>
          <div style={s.grid2}>
            <div style={{ ...s.field, gridColumn: '1 / -1' }}>
              <label style={s.label}>Địa chỉ *</label>
              <input name="address" required style={s.input} placeholder="Số nhà, tên đường" />
            </div>
            <div style={s.field}>
              <label style={s.label}>Thành phố *</label>
              <input name="city" required style={s.input} placeholder="Thành phố" />
            </div>
          </div>

          <div style={s.field}>
            <label style={s.label}>Mô tả <span style={s.optionalTag}>(tuỳ chọn)</span></label>
            <textarea name="description" style={s.textarea} placeholder="Mô tả ngắn về cơ sở đối tác..." />
          </div>

          <button type="submit" style={submitting ? s.btnDisabled : s.btn} disabled={submitting}>
            {submitting ? 'Đang tạo...' : 'Đăng ký đối tác'}
          </button>
        </form>
      </div>
    </div>
  );
}
