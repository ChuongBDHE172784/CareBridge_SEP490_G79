import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  User,
  Mail,
  Phone,
  Shield,
  Pencil,
  Lock,
  Bell,
  Monitor,
  ChevronRight,
  AlertCircle,
  ShieldCheck,
} from 'lucide-react';
import { fetchProfile, refreshAccessToken, selectRole, updateUserProfile } from '../services/authApi';
import type { UserProfile } from '../models/user';
import { ROLE_LABELS } from '../models/user';
import type { UserRole } from '../../../shared/auth/authStore';
import { useAuthStore } from '../../../shared/auth/authStore';
import { getDefaultRouteForRole } from '../../../shared/auth/roleRoutes';
import './AccountProfilePage.css';

export default function AccountProfilePage() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState('');
  const [saving, setSaving] = useState(false);
  const [roleSaving, setRoleSaving] = useState(false);
  const [roleError, setRoleError] = useState<string | null>(null);

  const loadProfile = () => {
    setLoading(true);
    setError(null);
    fetchProfile()
      .then(setProfile)
      .catch(() => setError('Unable to load profile'))
      .finally(() => setLoading(false));
  };

  useEffect(loadProfile, []);

  const getInitials = (name: string | null): string => {
    if (!name) return '?';
    return name
      .split(' ')
      .map((w) => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  const handleEditClick = () => {
    setEditName(profile?.name || '');
    setIsEditing(true);
  };

  const handleSaveProfile = async () => {
    setSaving(true);
    try {
      const updated = await updateUserProfile({ displayName: editName });
      setProfile(updated);
      setIsEditing(false);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể lưu thông tin');
    } finally {
      setSaving(false);
    }
  };

  const handleRoleSelect = async (role: Extract<UserRole, 'MOTHER' | 'FAMILY' | 'EXPERT'>) => {
    if (roleSaving) return;
    setRoleSaving(true);
    setRoleError(null);
    try {
      let updated: UserProfile;
      try {
        updated = await selectRole(role);
      } catch (selectionError) {
        // The role write can commit even when the response is lost. Re-read the
        // profile before surfacing an error so a retry is idempotent instead of
        // leaving the user stranded behind a "role already assigned" response.
        const recovered = await fetchProfile().catch(() => null);
        if (!recovered?.role) throw selectionError;
        updated = recovered;
      }

      const refreshToken = useAuthStore.getState().refreshToken;
      if (!refreshToken) throw new Error('Missing refresh token after role selection');
      const refreshed = await refreshAccessToken({ refreshToken });
      if (!refreshed.user.role) {
        throw new Error('Role assignment was not reflected in the refreshed session');
      }
      const refreshedProfile: UserProfile = {
        id: refreshed.user.id,
        phone: refreshed.user.phone ?? updated.phone,
        email: refreshed.user.email,
        name: refreshed.user.name,
        avatarUrl: refreshed.user.avatarUrl,
        role: refreshed.user.role,
        emailVerified: refreshed.user.emailVerified,
        phoneVerified: refreshed.user.phoneVerified,
      };
      setProfile(refreshedProfile);
      useAuthStore.getState().setUser({
        id: refreshedProfile.id,
        phone: refreshedProfile.phone,
        name: refreshedProfile.name,
        avatarUrl: refreshedProfile.avatarUrl,
        role: refreshedProfile.role,
      });
      navigate(getDefaultRouteForRole(refreshedProfile.role), { replace: true });
    } catch (caught: unknown) {
      const message = (caught as { response?: { data?: { message?: string } } })
        .response?.data?.message;
      setRoleError(message ?? 'Không thể lưu vai trò. Vui lòng thử lại.');
    } finally {
      setRoleSaving(false);
    }
  };

  return (
    <div className="profile-page">
      <div className="profile-container">
        {/* ── Profile Card ── */}
        <div className="profile-card">
          {loading ? (
            <div className="profile-skeleton">
              <div className="skeleton-pulse skeleton-circle w-24 h-24" />
              <div className="skeleton-pulse w-40 h-6" />
              <div className="skeleton-pulse w-[100px] h-5" />
              <div className="skeleton-pulse w-full h-14" />
              <div className="skeleton-pulse w-full h-14" />
            </div>
          ) : error ? (
            <div className="profile-error">
              <div className="profile-error-icon">
                <AlertCircle />
              </div>
              <p className="profile-error-msg">{error}</p>
              <button className="profile-retry-btn" onClick={loadProfile}>
                Try again
              </button>
            </div>
          ) : profile ? (
            <>
              {/* Avatar */}
              <div className="profile-avatar-wrapper">
                {profile.avatarUrl ? (
                  <img
                    src={profile.avatarUrl}
                    alt={profile.name ?? 'Avatar'}
                    className="profile-avatar"
                  />
                ) : (
                  <div className="profile-avatar-placeholder">
                    {getInitials(profile.name)}
                  </div>
                )}
                <div className="profile-status-dot" />
              </div>

              {/* Name */}
              <h1 className="profile-name">
                {profile.name || 'CareBridge User'}
              </h1>

              {/* Role Badge */}
              <span className="profile-role-badge">
                <Shield />
                {profile.role ? (ROLE_LABELS[profile.role] ?? profile.role) : 'Chưa chọn vai trò'}
              </span>

              {!profile.role && (
                <div className="mt-6 w-full rounded-2xl border border-primary/20 bg-primary/5 p-5 text-left">
                  <h2 className="m-0 text-lg font-bold text-on-surface">Hoàn thiện tài khoản</h2>
                  <p className="mt-2 text-sm leading-6 text-on-surface-variant">
                    Chọn vai trò để CareBridge mở đúng hành trình chăm sóc cho bạn.
                  </p>
                  {roleError && <p className="mt-3 text-sm font-semibold text-error" role="alert">{roleError}</p>}
                  <div className="mt-4 grid gap-3 sm:grid-cols-3">
                    {([
                      ['MOTHER', 'Mẹ bầu'],
                      ['FAMILY', 'Người thân'],
                      ['EXPERT', 'Chuyên gia'],
                    ] as const).map(([role, label]) => (
                      <button
                        key={role}
                        type="button"
                        disabled={roleSaving || (role === 'EXPERT' && profile.emailVerified !== true)}
                        onClick={() => handleRoleSelect(role)}
                        className="min-h-12 rounded-xl border border-outline-variant bg-white px-3 text-sm font-semibold text-primary transition hover:border-primary focus:outline-none focus:ring-2 focus:ring-primary disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        {role === 'EXPERT' && profile.emailVerified !== true ? 'Chuyên gia (cần Email)' : roleSaving ? 'Đang lưu…' : label}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Info Fields */}
              <div className="profile-info-section">
                <div className="profile-info-row">
                  <div className="profile-info-icon"><Mail /></div>
                  <div className="profile-info-content">
                    <span className="profile-info-label">Email</span>
                    <span className={`profile-info-value${!profile.email ? ' empty' : ''}`}>
                      {profile.email || 'Not provided'}
                    </span>
                  </div>
                  {profile.email && (
                    <span className="profile-verified-badge">
                      <ShieldCheck /> Verified
                    </span>
                  )}
                </div>

                <div className="profile-info-row">
                  <div className="profile-info-icon"><Phone /></div>
                  <div className="profile-info-content">
                    <span className="profile-info-label">Phone</span>
                    <span className="profile-info-value">{profile.phone}</span>
                  </div>
                  <span className="profile-verified-badge">
                    <ShieldCheck /> Verified
                  </span>
                </div>

                <div className="profile-info-row">
                  <div className="profile-info-icon"><User /></div>
                  <div className="profile-info-content">
                    <span className="profile-info-label">Account ID</span>
                    <span className="profile-info-value">#{profile.id}</span>
                  </div>
                </div>
              </div>

              {/* Edit Button */}
              <button className="profile-edit-btn" onClick={handleEditClick}>
                <Pencil /> Edit Profile
              </button>
            </>
          ) : null}
        </div>

        {/* ── Edit Modal ── */}
        {isEditing && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true">
            <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
              <h3 className="text-xl font-bold mb-4">Chỉnh sửa hồ sơ</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Họ và tên</label>
                  <input
                    type="text"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    className="w-full rounded-xl border border-gray-300 px-4 py-2 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none"
                    placeholder="Nhập họ và tên..."
                  />
                </div>
              </div>
              <div className="mt-6 flex justify-end gap-3">
                <button
                  onClick={() => setIsEditing(false)}
                  className="px-5 py-2.5 rounded-full border border-gray-300 font-medium text-gray-700 hover:bg-gray-50"
                >
                  Hủy
                </button>
                <button
                  disabled={saving}
                  onClick={handleSaveProfile}
                  className="px-5 py-2.5 rounded-full bg-primary font-semibold text-white hover:bg-primary/90 disabled:opacity-50"
                >
                  {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ── Settings Shortcuts ── */}
        {!loading && !error && profile && (
          <div className="settings-card">
            <span className="settings-title">Account Settings</span>

            <button className="settings-item">
              <div className="settings-item-icon"><Shield /></div>
              <div className="settings-item-text">
                <span className="settings-item-label">Privacy Settings</span>
                <span className="settings-item-desc">Manage data sharing and consent</span>
              </div>
              <span className="settings-item-chevron"><ChevronRight /></span>
            </button>

            <button className="settings-item">
              <div className="settings-item-icon"><Bell /></div>
              <div className="settings-item-text">
                <span className="settings-item-label">Notifications</span>
                <span className="settings-item-desc">Channels, categories, quiet hours</span>
              </div>
              <span className="settings-item-chevron"><ChevronRight /></span>
            </button>

            <button className="settings-item">
              <div className="settings-item-icon"><Lock /></div>
              <div className="settings-item-text">
                <span className="settings-item-label">Change Password</span>
                <span className="settings-item-desc">Update your account password</span>
              </div>
              <span className="settings-item-chevron"><ChevronRight /></span>
            </button>

            <button className="settings-item">
              <div className="settings-item-icon"><Monitor /></div>
              <div className="settings-item-text">
                <span className="settings-item-label">Login Sessions</span>
                <span className="settings-item-desc">View and revoke active devices</span>
              </div>
              <span className="settings-item-chevron"><ChevronRight /></span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
