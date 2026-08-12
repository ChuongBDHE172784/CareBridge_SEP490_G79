import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type UserRole =
  | 'MOTHER'
  | 'FAMILY'
  | 'EXPERT'
  | 'MODERATOR'
  | 'CONTENT_ADMIN'
  | 'SYSTEM_ADMIN'

export interface AuthUser {
  id: string;
  phone: string;
  name: string | null;
  avatarUrl: string | null;
  role: UserRole | null;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: AuthUser | null) => void;
  logout: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setTokens: (accessToken, refreshToken) =>
        set({ accessToken, refreshToken }),
      setUser: (user) => set({ user }),
      logout: () => set({ accessToken: null, refreshToken: null, user: null }),
      isAuthenticated: () => {
        const { accessToken } = get();
        if (!accessToken) return false;
        try {
          const [, payloadB64] = accessToken.split('.');
          if (!payloadB64) return false;
          const normalized = payloadB64.replace(/-/g, '+').replace(/_/g, '/');
          const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
          const payload = JSON.parse(atob(padded));
          return payload.exp * 1000 > Date.now();
        } catch {
          return false;
        }
      },
    }),
    { name: 'carebridge-auth' }
  )
);
