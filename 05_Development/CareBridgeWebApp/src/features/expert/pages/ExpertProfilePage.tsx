import { useState, useEffect } from 'react';
import { getMyProfile, updateMyProfile, searchTrackAsiaHospitals, uploadExpertAvatar, getProvinces } from '../services/expertApi';
import { updateUserProfile } from '../../auth/services/authApi';
import type { UserProfile } from '../../auth/models/user';

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
  'Chuyên gia Y tế khác',
];

function getInitials(name: string | null | undefined): string {
  if (!name) return 'EX';
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
    workplaceProvinceId: '',
    consultationScope: '',
  });

  const [avatarSaving, setAvatarSaving] = useState(false);
  const [avatarSuccess, setAvatarSuccess] = useState(false);

  const [trackAsiaQuery, setTrackAsiaQuery] = useState('');
  const [trackAsiaResults, setTrackAsiaResults] = useState<any[]>([]);
  const [searchingHospitals, setSearchingHospitals] = useState(false);
  // The last value that came from the profile or from picking a suggestion. Typing
  // must not be compared against form.workplace any more - that now tracks the text
  // as it is typed, so comparing the two would suppress every search.
  const [settledWorkplace, setSettledWorkplace] = useState('');
  const [provinces, setProvinces] = useState<{ provinceId: string; name: string }[]>([]);

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
        workplaceProvinceId: profileData.workplaceProvinceId ?? '',
        consultationScope: profileData.consultationScope ?? '',
      });
      setTrackAsiaQuery(profileData.workplace ?? '');
      setSettledWorkplace(profileData.workplace ?? '');
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Không thể tải hồ sơ chuyên môn');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    getProvinces()
      .then((list) => setProvinces(list ?? []))
      .catch(() => setProvinces([]));
  }, []);

  useEffect(() => {
    const hasProvince = Boolean(form.workplaceProvinceId);
    // A province on its own is a valid search: the server browses that province when no
    // name has been typed, which is what turns the picker into a list to choose from.
    if (!hasProvince && (trackAsiaQuery.trim().length < 2 || trackAsiaQuery === settledWorkplace)) {
      setTrackAsiaResults([]);
      return;
    }
    if (hasProvince && trackAsiaQuery === settledWorkplace && trackAsiaQuery.trim().length >= 2) {
      setTrackAsiaResults([]);
      return;
    }
    const timer = setTimeout(() => {
      setSearchingHospitals(true);
      searchTrackAsiaHospitals(trackAsiaQuery, form.workplaceProvinceId || undefined)
        .then((res) => setTrackAsiaResults(res || []))
        .catch(() => setTrackAsiaResults([]))
        .finally(() => setSearchingHospitals(false));
      // 250ms, not 500. Measured end to end the request itself costs ~300ms, so half a
      // second of waiting before it even starts was most of what felt slow.
    }, 250);
    return () => clearTimeout(timer);
  }, [trackAsiaQuery, settledWorkplace, form.workplaceProvinceId]);

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
      alert(err.response?.data?.message ?? 'Lưu ảnh đại diện thất bại');
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
        workplaceProvinceId: form.workplaceProvinceId || undefined,
        consultationScope: form.consultationScope,
      };
      if (form.experienceYears) body.experienceYears = parseInt(form.experienceYears);
      const updated = await updateMyProfile(body);
      setProfile(updated);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (e: any) {
      alert(e.response?.data?.message ?? 'Lưu hồ sơ thất bại');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="py-12 text-center text-outline">Đang tải hồ sơ chuyên môn...</div>;
  }

  const getStatusBadge = (status: string) => {
    if (status === 'APPROVED' || status === 'VERIFIED')
      return { label: 'Đã xác minh', className: 'bg-[#E6F4EA] text-[#137333]' };
    if (status === 'REJECTED')
      return { label: 'Từ chối xác minh', className: 'bg-error-container text-error' };
    return { label: 'Đang chờ xét duyệt', className: 'bg-[#FFF3E0] text-[#E65100]' };
  };

  const statusInfo = getStatusBadge(profile?.verificationStatus);

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Hồ sơ chuyên môn</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Quản lý thông tin cá nhân, bằng cấp, nơi công tác và phạm vi tư vấn sức khỏe
          </p>
        </div>
        <span className={`py-1.5 px-4 rounded-full text-xs font-semibold ${statusInfo.className}`}>
          ✓ {statusInfo.label}
        </span>
      </div>

      {error && (
        <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{error}</div>
      )}
      {success && (
        <div className="bg-[#E6F4EA] rounded-2xl p-4 mb-4 text-[#137333] text-sm">
          ✓ Đã cập nhật thông tin hồ sơ thành công!
        </div>
      )}
      {avatarSuccess && (
        <div className="bg-[#E6F4EA] rounded-2xl p-4 mb-4 text-[#137333] text-sm">
          ✓ Đã cập nhật ảnh đại diện thành công!
        </div>
      )}

      {/* Avatar Card */}
      <div className="bg-surface rounded-2xl p-6 shadow-md mb-6 flex items-center gap-6">
        <div className="relative">
          {profile?.avatarUrl ? (
            <img
              src={profile.avatarUrl}
              alt="Avatar"
              className="w-20 h-20 rounded-full object-cover border-2 border-primary/20"
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = 'none';
              }}
            />
          ) : (
            <div className="w-20 h-20 rounded-full bg-primary-container text-primary font-bold text-2xl flex items-center justify-center">
              {getInitials(user?.name ?? null)}
            </div>
          )}
        </div>

        <div className="flex-1">
          <h3 className="text-base font-bold text-on-surface mb-0.5">{user?.name || 'Chuyên gia CareBridge'}</h3>
          <p className="text-xs text-outline">{form.professionalTitle || 'Chuyên gia Y tế'} {form.specialty ? `• ${form.specialty}` : ''}</p>
          <p className="text-xs text-on-surface-variant mt-1">{user?.phone}</p>
        </div>

        <label
          className={`flex items-center gap-2 py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-primary text-xs font-semibold transition hover:bg-surface-container-low ${
            avatarSaving ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
          }`}
        >
          <span className="material-symbols-outlined text-base">photo_camera</span>
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

      {/* Main Form */}
      <form onSubmit={onSubmit} className="space-y-6">
        {/* Section 1: Professional Information */}
        <div className="bg-surface rounded-2xl p-6 shadow-md">
          <h2 className="text-base font-bold text-on-surface mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-xl">medical_information</span>
            Thông tin chuyên môn &amp; Chức danh
          </h2>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
                Chuyên khoa <span className="text-error">*</span>
              </label>
              <input
                required
                className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans"
                value={form.specialty}
                onChange={(e) => setForm({ ...form, specialty: e.target.value })}
                placeholder="VD: Sản khoa, Nhi khoa, Dinh dưỡng..."
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
                Chức danh chuyên môn
              </label>
              <select
                className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans cursor-pointer"
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
            </div>

            <div>
              <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
                Số năm kinh nghiệm
              </label>
              <input
                type="number"
                min="0"
                className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans"
                value={form.experienceYears}
                onChange={(e) => setForm({ ...form, experienceYears: e.target.value })}
                placeholder="Số năm..."
              />
            </div>
          </div>
        </div>

        {/* Section 2: Workplace */}
        <div className="bg-surface rounded-2xl p-6 shadow-md">
          <h2 className="text-base font-bold text-on-surface mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-xl">domain</span>
            Nơi công tác &amp; Cơ sở y tế
          </h2>

          <div className="mb-5">
            <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
              Tỉnh / Thành phố
            </label>
            <select
              value={form.workplaceProvinceId}
              onChange={(e) => {
                setForm({ ...form, workplaceProvinceId: e.target.value });
                setTrackAsiaResults([]);
              }}
              className="w-full p-3.5 rounded-xl border border-outline-variant bg-surface text-on-surface"
            >
              <option value="">— Chọn tỉnh/thành để xem bệnh viện —</option>
              {provinces.map((p) => (
                <option key={p.provinceId} value={p.provinceId}>{p.name}</option>
              ))}
            </select>
            <p className="mt-1 text-xs text-on-surface-variant">
              Chọn tỉnh/thành rồi bấm vào ô bên dưới để xem danh sách bệnh viện trong tỉnh đó.
            </p>
          </div>

          <div className="relative">
            <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
              Bệnh viện / Phòng khám
            </label>
            <input
              className="w-full py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans"
              value={trackAsiaQuery}
              onChange={(e) => {
                const typed = e.target.value;
                setTrackAsiaQuery(typed);
                // Keep what was typed as the value to save. Only picking a suggestion
                // used to set form.workplace, so anyone who typed the hospital name and
                // pressed update sent an empty string and saw the field come back blank.
                setForm((prev) => ({ ...prev, workplace: typed }));
              }}
              placeholder="Gõ tên bệnh viện/phòng khám (VD: Bệnh viện Từ Dũ, Bệnh viện Chợ Rẫy)..."
            />
            {searchingHospitals && (
              <div className="absolute right-4 top-10">
                <span className="material-symbols-outlined animate-spin text-primary text-lg">progress_activity</span>
              </div>
            )}
            {trackAsiaResults.length > 0 && (
              <div className="absolute top-[72px] z-10 max-h-60 w-full overflow-y-auto rounded-2xl border border-outline-variant bg-surface shadow-xl py-1">
                {trackAsiaResults.map((r, i) => (
                  <button
                    key={i}
                    type="button"
                    className="w-full p-3.5 text-left hover:bg-surface-container-low border-b border-surface-container-highest last:border-0 cursor-pointer"
                    onClick={() => {
                      setForm({ ...form, workplace: r.name });
                      setTrackAsiaQuery(r.name);
                      setSettledWorkplace(r.name);
                      setTrackAsiaResults([]);
                    }}
                  >
                    <p className="font-semibold text-sm text-on-surface">{r.name}</p>
                    <p className="text-xs text-outline">{r.address}</p>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Section 3: Consultation Scope */}
        <div className="bg-surface rounded-2xl p-6 shadow-md">
          <h2 className="text-base font-bold text-on-surface mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-xl">description</span>
            Lĩnh vực &amp; Phạm vi tư vấn
          </h2>

          <div>
            <label className="block text-xs font-semibold text-outline uppercase tracking-wider mb-2">
              Mô tả chi tiết lĩnh vực có thể tư vấn
            </label>
            <textarea
              rows={4}
              className="w-full py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans leading-relaxed resize-none"
              value={form.consultationScope}
              onChange={(e) => setForm({ ...form, consultationScope: e.target.value })}
              placeholder="Nhập phạm vi tư vấn sức khỏe thai kỳ, dinh dưỡng mẹ & bé, tư vấn tâm lý..."
            />
          </div>

          {profile?.verifiedAt && (
            <p className="text-xs text-outline mt-3">
              Đã xác minh lần cuối: {new Date(profile.verifiedAt).toLocaleString('vi-VN')}
            </p>
          )}
        </div>

        {/* Action button */}
        <div className="flex justify-end pt-2">
          <button
            type="submit"
            disabled={saving}
            className="flex items-center gap-2 py-3 px-8 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer hover:brightness-110 disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-lg">save</span>
            {saving ? 'Đang lưu...' : 'Lưu hồ sơ chuyên môn'}
          </button>
        </div>
      </form>
    </div>
  );
}

