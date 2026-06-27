import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthLayout from '../layouts/AuthLayout';
import AdminLayout from '../layouts/AdminLayout';
import ProtectedRoute from '../guards/ProtectedRoute';
import RoleAwareRedirect from '../guards/RoleAwareRedirect';

// Auth screens
import LoginPage from '../../features/auth/pages/LoginPage';
import OtpPage from '../../features/auth/pages/OtpPage';
import BlockedAccountPage from '../../features/auth/pages/BlockedAccountPage';
import NoWebAccessPage from '../../features/auth/pages/NoWebAccessPage';

// Admin portal screens (placeholders — replace via /build-screen)
import AdminDashboardPage from '../../features/admin/pages/AdminDashboardPage';
import ModerationQueuePage from '../../features/moderation/pages/ModerationQueuePage';
import ManageTopicsPage from '../../features/community/pages/ManageTopicsPage';
import CreateContentPage from '../../features/contentManagement/pages/CreateContentPage';
import CreatePartnerProfilePage from '../../features/partnerGovernance/pages/CreatePartnerProfilePage';
import ExpertDashboardPage from '../../features/expert/pages/ExpertDashboardPage';

const ForbiddenPage = () => (
  <div style={{ padding: 48, textAlign: 'center', fontFamily: "'Lexend', sans-serif", color: '#524440' }}>
    <h2 style={{ color: '#845143' }}>Truy cập bị từ chối</h2>
    <p>Bạn không có quyền xem trang này.</p>
  </div>
);

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <AuthLayout />,
    children: [
      { index: true, element: <LoginPage /> },
      { path: 'otp', element: <OtpPage /> },
    ],
  },
  { path: '/forbidden', element: <ForbiddenPage /> },
  { path: '/account-blocked', element: <BlockedAccountPage /> },
  { path: '/no-web-access', element: <NoWebAccessPage /> },

  // Role-aware root redirect — sits outside ProtectedRoute so all roles are handled,
  // including MOTHER/FAMILY who land on /no-web-access without hitting /forbidden.
  { path: '/', element: <RoleAwareRedirect /> },

  {
    element: (
      <ProtectedRoute
        requiredRoles={['MODERATOR', 'CONTENT_ADMIN', 'SYSTEM_ADMIN', 'PARTNER', 'EXPERT']}
      />
    ),
    children: [
      {
        element: <AdminLayout />,
        children: [
          {
            element: <ProtectedRoute requiredRoles={['MODERATOR', 'SYSTEM_ADMIN']} />,
            children: [
              { path: '/moderator/dashboard', element: <ModerationQueuePage /> },
              { path: '/moderator', element: <Navigate to="/moderator/dashboard" replace /> },
              { path: '/admin/topics', element: <ManageTopicsPage /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['CONTENT_ADMIN', 'SYSTEM_ADMIN']} />,
            children: [
              { path: '/content/dashboard', element: <CreateContentPage /> },
              { path: '/content', element: <Navigate to="/content/dashboard" replace /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['PARTNER', 'SYSTEM_ADMIN']} />,
            children: [
              { path: '/partner/dashboard', element: <CreatePartnerProfilePage /> },
              { path: '/partner', element: <Navigate to="/partner/dashboard" replace /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['SYSTEM_ADMIN']} />,
            children: [
              { path: '/admin/dashboard', element: <AdminDashboardPage /> },
              { path: '/admin', element: <Navigate to="/admin/dashboard" replace /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['EXPERT']} />,
            children: [
              { path: '/expert/dashboard', element: <ExpertDashboardPage /> },
              { path: '/expert', element: <Navigate to="/expert/dashboard" replace /> },
            ],
          },
        ],
      },
    ],
  },

  { path: '*', element: <Navigate to="/login" replace /> },
]);
