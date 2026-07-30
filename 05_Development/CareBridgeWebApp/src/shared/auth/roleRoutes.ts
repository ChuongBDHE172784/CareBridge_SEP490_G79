import type { UserRole } from './authStore';

/**
 * Canonical role → default landing route mapping for the web portal.
 *
 * Priority order (used when SYSTEM_ADMIN is not the primary concern):
 *   SYSTEM_ADMIN > MODERATOR > CONTENT_ADMIN > PARTNER > EXPERT > MOTHER / FAMILY
 *
 * Stale aliases (ADMIN, PARTNER_REPRESENTATIVE) never appear in JWTs — the backend
 * always emits the 7 canonical roles defined in Role.java. No alias handling needed.
 *
 * MOTHER web access is intentionally limited to Direct Consult Chat.
 * FAMILY has no web portal access.
 */
export const ROLE_DEFAULT_ROUTES: Record<UserRole, string> = {
  SYSTEM_ADMIN: '/admin/dashboard',
  // The moderation dashboard is available at /moderator/moderator-dashboard; reports remain
  // the role's default landing page so existing moderator workflows are preserved.
  MODERATOR: '/moderator/reports',
  CONTENT_ADMIN: '/content/dashboard',
  PARTNER: '/partner/dashboard',
  // The server-owned onboarding aggregate decides whether an expert may enter the portal.
  EXPERT: '/expert/onboarding',
  MOTHER: '/direct-chats',
  FAMILY: '/no-web-access',
};

export function getDefaultRouteForRole(role: UserRole | null | undefined): string {
  if (!role) return '/login';
  return ROLE_DEFAULT_ROUTES[role] ?? '/no-web-access';
}
