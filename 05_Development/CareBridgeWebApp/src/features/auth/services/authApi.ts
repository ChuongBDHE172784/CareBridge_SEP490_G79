import axios from 'axios';
import type { ApiResponse, UserProfile } from '../models/user';
import { useAuthStore } from '../../../shared/auth/authStore';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function fetchProfile(): Promise<UserProfile> {
  const { data } = await api.get<ApiResponse<UserProfile>>('/api/v1/auth/profile');
  return data.data;
}
