import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';
import type { UserRole } from '../../shared/auth/authStore';

interface Props {
  requiredRoles?: UserRole[];
}

export default function ProtectedRoute({ requiredRoles }: Props) {
  const { isAuthenticated, hasAnyRole } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredRoles && requiredRoles.length > 0 && !hasAnyRole(...requiredRoles)) {
    return <Navigate to="/forbidden" replace />;
  }

  return <Outlet />;
}
