import { Navigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';

// Retained for backward-compatible imports; the router no longer exposes a bare moderator route.
export default function ModeratorIndexRedirect() {
  const { hasRole } = useAuth();
  const target = hasRole('SYSTEM_ADMIN') ? '/admin/moderator-dashboard' : '/admin/reports';
  return <Navigate to={target} replace />;
}
