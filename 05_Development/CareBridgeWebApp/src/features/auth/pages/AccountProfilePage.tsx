import { useEffect, useState } from 'react';
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
import { fetchProfile } from '../services/authApi';
import type { UserProfile } from '../models/user';
import { ROLE_LABELS } from '../models/user';
import './AccountProfilePage.css';

export default function AccountProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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

  return (
    <div className="profile-page">
      <div className="profile-container">
        {/* ── Profile Card ── */}
        <div className="profile-card">
          {loading ? (
            <div className="profile-skeleton">
              <div className="skeleton-pulse skeleton-circle" style={{ width: 96, height: 96 }} />
              <div className="skeleton-pulse" style={{ width: 160, height: 24 }} />
              <div className="skeleton-pulse" style={{ width: 100, height: 20 }} />
              <div className="skeleton-pulse" style={{ width: '100%', height: 56 }} />
              <div className="skeleton-pulse" style={{ width: '100%', height: 56 }} />
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
                {ROLE_LABELS[profile.role] ?? profile.role}
              </span>

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
              <button className="profile-edit-btn">
                <Pencil /> Edit Profile
              </button>
            </>
          ) : null}
        </div>

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
