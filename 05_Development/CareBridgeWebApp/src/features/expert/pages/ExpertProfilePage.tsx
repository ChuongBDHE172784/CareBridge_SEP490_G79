import { useState, useEffect } from 'react';
import { getMyProfile, updateMyProfile } from '../services/expertApi';
import { updateUserProfile } from '../../auth/services/authApi';
import type { UserProfile } from '../../auth/models/user';
import { searchTrackAsiaHospitals, uploadExpertAvatar } from '../services/expertApi';
import { RefreshCw, Camera } from 'lucide-react';

const TITLES = [
  'Bác sĩ',
  'Thạc sĩ - Bác sĩ',
  'Tiến sĩ - Bác sĩ',
  'BS.CKI',
  'BS.CKII',
  'PGS.TS.BS',
  'GS.TS.BS',
  'Chuyên gia Tâm lý',
  'Chuyên gia Dinh dưỡng',
  'Điều dưỡng',
  'Kỹ thuật viên',
  'Chuyên gia Y tế khác'
];
import './ExpertProfilePage.css';

function getInitials(name: string | null | undefined): string {
  if (!name) return 'ED';
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return parts[0].slice(0, 2).toUpperCase();
}

export default function ExpertProfilePage() {
  const [profile, setProfile] = useState<any>(null);
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const [form, setForm] = useState({
    specialty: '',
    professionalTitle: '',
    experienceYears: '',
    workplace: '',
    consultationScope: '',
  });

  const [avatarSaving, setAvatarSaving] = useState(false);
  const [avatarSuccess, setAvatarSuccess] = useState(false);

  const [trackAsiaQuery, setTrackAsiaQuery] = useState('');
  const [trackAsiaResults, setTrackAsiaResults] = useState<any[]>([]);
  const [searchingHospitals, setSearchingHospitals] = useState(false);

  const load = async () => {
    try {
      setError(null);
      const [profileData, userData] = await Promise.all([
        getMyProfile(),
        import('../../auth/services/authApi').then((m) => m.fetchProfile()),
      ]);
      setProfile(profileData);
      setUser(userData);
      setForm({
        specialty: profileData.specialty ?? '',
        professionalTitle: profileData.professionalTitle ?? '',
        experienceYears: profileData.experienceYears?.toString() ?? '',
        workplace: profileData.workplace ?? '',
        consultationScope: profileData.consultationScope ?? '',
      });
      setTrackAsiaQuery(profileData.workplace ?? '');
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Không thể tải hồ sơ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  useEffect(() => {
    if (trackAsiaQuery.trim().length < 2 || trackAsiaQuery === form.workplace) {
      setTrackAsiaResults([]);
      return;
    }
    const timer = setTimeout(() => {
      setSearchingHospitals(true);
      searchTrackAsiaHospitals(trackAsiaQuery)
        .then(res => setTrackAsiaResults(res || []))
        .catch(() => setTrackAsiaResults([]))
        .finally(() => setSearchingHospitals(false));
    }, 500);
    return () => clearTimeout(timer);
  }, [trackAsiaQuery, form.workplace]);

  const handleAvatarSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      alert('Chỉ chấp nhận ảnh JPEG hoặc PNG.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      alert('Ảnh phải có dung lượng tối đa 5 MB.');
      return;
    }
    setAvatarSaving(true);
    try {
      const url = await uploadExpertAvatar(file);
      const updated = await updateUserProfile({ avatarUrl: url });
      setProfile((prev: any) => ({ ...prev, avatarUrl: updated.avatarUrl }));
      setUser((prev: any) => ({ ...prev, avatarUrl: updated.avatarUrl }));
      setAvatarSuccess(true);
      setTimeout(() => setAvatarSuccess(false), 3000);
    } catch (err: any) {
      alert(err.response?.data?.message ?? 'Lưu ảnh thất bại');
    } finally {
      setAvatarSaving(false);
      e.target.value = '';
    }
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSuccess(false);
    try {
      const body: any = {
        specialty: form.specialty,
        professionalTitle: form.professionalTitle,
        workplace: form.workplace,
        consultationScope: form.consultationScope,
      };
      if (form.experienceYears) body.experienceYears = parseInt(form.experienceYears);
      const updated = await updateMyProfile(body);
      setProfile(updated);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (e: any) {
      alert(e.response?.data?.message ?? 'Lưu thất bại');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-2 border-primary border-t-transparent" />
      </div>
    );
  }

  const statusColor: Record<string, string> = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    UNDER_REVIEW: 'bg-blue-100 text-blue-800',
    APPROVED: 'bg-green-100 text-green-800',
    VERIFIED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
    SUSPENDED: 'bg-gray-100 text-gray-800',
    EXPIRED: 'bg-gray-100 text-gray-800',
  };

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-on-surface">Hồ sơ chuyên môn</h1>
        <span className={`px-3 py-1 rounded-full text-sm font-medium ${statusColor[profile?.verificationStatus] || 'bg-gray-100'}`}>
          {profile?.verificationStatus ?? 'N/A'}
        </span>
      </div>

      {error && (
        <div className="mb-4 p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
      )}
      {success && (
        <div className="mb-4 p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">Đã lưu thành công!</div>
      )}
      {avatarSuccess && (
        <div className="mb-4 p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">Đã lưu ảnh thành công!</div>
      )}

      <div className="expert-avatar-section">
        {profile?.avatarUrl ? (
          <img
            src={profile.avatarUrl}
            alt="Avatar"
            className="expert-avatar-img"
            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
        ) : (
          <div className="expert-avatar-placeholder">
            {getInitials(user?.name ?? null)}
          </div>
        )}
        <label className={`expert-avatar-edit-btn ${avatarSaving ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}>
          <Camera size={16} className="mr-2 inline-block" />
          {avatarSaving ? 'Đang lưu...' : 'Cập nhật ảnh'}
          <input
            type="file"
            accept="image/jpeg,image/png"
            className="hidden"
            onChange={handleAvatarSelect}
            disabled={avatarSaving}
          />
        </label>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm divide-y divide-gray-100">
        <form onSubmit={onSubmit} className="p-6 space-y-5">
          <Field label="Chuyên khoa" required>
            <input
              className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
              value={form.specialty}
              onChange={(e) => setForm({ ...form, specialty: e.target.value })}
              placeholder="VD: Sản khoa, Nhi khoa..."
            />
          </Field>

          <Field label="Chức danh chuyên môn">
            <select
              className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 bg-white focus:border-primary focus:ring-1 focus:ring-primary"
              value={form.professionalTitle}
              onChange={(e) => setForm({ ...form, professionalTitle: e.target.value })}
            >
              <option value="">-- Chọn chức danh --</option>
              {TITLES.map((title) => (
                <option key={title} value={title}>
                  {title}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Số năm kinh nghiệm">
            <input
              type="number"
              min="0"
              className="mt-1 block w-32 rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
              value={form.experienceYears}
              onChange={(e) => setForm({ ...form, experienceYears: e.target.value })}
            />
          </Field>

          <Field label="Nơi công tác">
            <div className="relative">
              <input
                className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
                value={trackAsiaQuery}
                onChange={(e) => setTrackAsiaQuery(e.target.value)}
                placeholder="Gõ tên bệnh viện/phòng khám (VD: Bệnh viện Chợ Rẫy)..."
              />
              {searchingHospitals && (
                <div className="absolute right-3 top-4">
                  <RefreshCw className="animate-spin text-primary" size={16} />
                </div>
              )}
              {trackAsiaResults.length > 0 && (
                <div className="absolute top-12 z-10 max-h-60 w-full overflow-y-auto rounded-md border border-gray-200 bg-white shadow-lg">
                  {trackAsiaResults.map((r, i) => (
                    <button
                      key={i}
                      type="button"
                      className="w-full border-b p-3 text-left hover:bg-gray-50 last:border-0"
                      onClick={() => {
                        setForm({ ...form, workplace: r.name });
                        setTrackAsiaQuery(r.name);
                        setTrackAsiaResults([]);
                      }}
                    >
                      <p className="font-semibold text-sm">{r.name}</p>
                      <p className="text-xs text-gray-500">{r.address}</p>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </Field>

          <Field label="Lĩnh vực tư vấn">
            <textarea
              rows={4}
              className="mt-1 block w-full rounded border border-gray-300 px-3 py-2 focus:border-primary focus:ring-1 focus:ring-primary"
              value={form.consultationScope}
              onChange={(e) => setForm({ ...form, consultationScope: e.target.value })}
              placeholder="Mô tả lĩnh vực bạn có thể tư vấn..."
            />
          </Field>

          {profile?.verifiedAt && (
            <p className="text-sm text-gray-500">
              Đã xác minh lúc: {new Date(profile.verifiedAt).toLocaleString('vi-VN')}
            </p>
          )}

          <div className="pt-2">
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2.5 rounded bg-primary text-white font-medium hover:bg-primary/90 disabled:opacity-50"
            >
              {saving ? 'Đang lưu...' : 'Lưu hồ sơ'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function Field({ label, children, required }: { label: string; children: React.ReactNode; required?: boolean }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      {children}
    </div>
  );
}
