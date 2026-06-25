import { useAuthStore } from './authStore';
import type { UserRole } from './authStore';

export function useAuth() {
  const { accessToken, user, setTokens, setUser, logout, isAuthenticated } =
    useAuthStore();

  const hasRole = (role: UserRole) => user?.role === role;
  const hasAnyRole = (...roles: UserRole[]) =>
    user ? roles.includes(user.role) : false;

  return {
    accessToken,
    user,
    isAuthenticated: isAuthenticated(),
    hasRole,
    hasAnyRole,
    setTokens,
    setUser,
    logout,
  };
}
