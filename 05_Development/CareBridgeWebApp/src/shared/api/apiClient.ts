import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../auth/authStore';
import { saveBlockedAccountState } from '../../features/auth/models/blockedAccount';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

export interface ApiRequestSessionIdentity {
  userId: string;
  accessToken: string;
}

export type ApiSessionBoundRequestConfig = AxiosRequestConfig & {
  carebridgeSession: ApiRequestSessionIdentity;
};

type SessionBoundInternalConfig = InternalAxiosRequestConfig & {
  carebridgeSession?: ApiRequestSessionIdentity;
};

export function isApiRequestSessionCurrent(
  session: ApiRequestSessionIdentity | undefined,
): boolean {
  if (!session) return true;
  const current = useAuthStore.getState();
  return current.user?.id === session.userId && current.accessToken === session.accessToken;
}

apiClient.interceptors.request.use((config) => {
  const session = (config as SessionBoundInternalConfig).carebridgeSession;
  const token = session?.accessToken ?? useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Public auth endpoints: a 401/403 here means "this attempt failed" (wrong
// password, locked account trying to log in, etc.), not "your session expired".
// The calling page shows its own inline error — the interceptor must not
// force-logout or redirect over it.
const PUBLIC_AUTH_PATHS = ['/auth/register', '/auth/phone/register', '/auth/login', '/auth/phone/login', '/auth/login-direct', '/auth/verify-otp', '/auth/resend-otp', '/auth/forgot-password', '/auth/reset-password', '/auth/lock-appeals'];

function requestPath(url: string) {
  try {
    const path = new URL(url, 'http://carebridge.local').pathname;
    return path.length > 1 ? path.replace(/\/+$/, '') : path;
  } catch {
    const path = url.split(/[?#]/, 1)[0];
    return path.length > 1 ? path.replace(/\/+$/, '') : path;
  }
}

export function shouldRedirectToMaintenance(status: number | undefined, errorCode: string | undefined, url: string) {
  return status === 503
    && errorCode === 'SYSTEM_MAINTENANCE'
    && requestPath(url) !== '/api/v1/admin/system-configuration';
}

// Clear session on 401, or 403 with ACCOUNT_DISABLED/ACCOUNT_LOCKED.
// Plain 403 (role mismatch) must NOT trigger logout.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestSession = (error.config as SessionBoundInternalConfig | undefined)?.carebridgeSession;
    if (!isApiRequestSessionCurrent(requestSession)) return Promise.reject(error);

    const status = error.response?.status;
    const errorCode = error.response?.data?.error;
    const url: string = error.config?.url ?? '';
    const isPublicAuthCall = PUBLIC_AUTH_PATHS.some((path) => url.includes(path));

    if (shouldRedirectToMaintenance(status, errorCode, url)) {
      if (window.location.pathname !== '/maintenance') {
        window.location.replace('/maintenance');
      }
      return Promise.reject(error);
    }

    if (!isPublicAuthCall) {
      if (status === 401) {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      } else if (
        status === 403 &&
        ['ACCOUNT_DISABLED', 'ACCOUNT_ADMIN_LOCKED', 'ACCOUNT_TEMPORARILY_LOCKED', 'ACCOUNT_SUSPENDED'].includes(errorCode)
      ) {
        useAuthStore.getState().logout();
        saveBlockedAccountState({
          code: errorCode,
          message: error.response?.data?.message,
          ...(error.response?.data?.metadata ?? {}),
        });
        window.location.href = '/account-blocked';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
