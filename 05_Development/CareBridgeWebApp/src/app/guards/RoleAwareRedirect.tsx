import { Navigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';
import { getDefaultRouteForRole } from '../../shared/auth/roleRoutes';

export default function RoleAwareRedirect() {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <Navigate to={getDefaultRouteForRole(user?.role)} replace />;
}
