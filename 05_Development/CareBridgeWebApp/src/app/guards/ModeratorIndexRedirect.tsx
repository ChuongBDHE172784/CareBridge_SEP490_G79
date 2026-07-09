import { Navigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';

// MODERATOR and SYSTEM_ADMIN land on different ModPortal pages by default: ModerationController
// (queue/reports/violations/safety-cases) is MODERATOR-only on the backend, while
// CommunityDashboardController is SYSTEM_ADMIN-only — see router/index.tsx.
export default function ModeratorIndexRedirect() {
  const { hasRole } = useAuth();
  const target = hasRole('SYSTEM_ADMIN') ? '/moderator/dashboard' : '/moderator/safety-cases';
  return <Navigate to={target} replace />;
}
