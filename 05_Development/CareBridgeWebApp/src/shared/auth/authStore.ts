import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type UserRole =
  | 'MOTHER'
  | 'FAMILY'
  | 'EXPERT'
  | 'MODERATOR'
  | 'CONTENT_ADMIN'
  | 'SYSTEM_ADMIN'
  | 'PARTNER';

export interface AuthUser {
  id: string;
  phone: string;
  name: string | null;
  avatarUrl: string | null;
  role: UserRole;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: AuthUser) => void;
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
          const payload = JSON.parse(atob(payloadB64));
          return payload.exp * 1000 > Date.now();
        } catch {
          return false;
        }
      },
    }),
    { name: 'carebridge-auth' }
  )
);
