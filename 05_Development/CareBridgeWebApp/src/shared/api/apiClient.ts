import axios from 'axios';
import { useAuthStore } from '../auth/authStore';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Clear session on 401, or 403 with ACCOUNT_DISABLED/ACCOUNT_LOCKED.
// Plain 403 (role mismatch) must NOT trigger logout.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const errorCode = error.response?.data?.error;

    if (status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    } else if (
      status === 403 &&
      (errorCode === 'ACCOUNT_DISABLED' || errorCode === 'ACCOUNT_LOCKED')
    ) {
      useAuthStore.getState().logout();
      const reason = errorCode === 'ACCOUNT_DISABLED' ? 'disabled' : 'locked';
      window.location.href = `/account-blocked?reason=${reason}`;
    }
    return Promise.reject(error);
  }
);

export default apiClient;
