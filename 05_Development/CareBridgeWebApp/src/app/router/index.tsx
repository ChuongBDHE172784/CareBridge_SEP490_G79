import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthLayout from '../layouts/AuthLayout';
import AdminLayout from '../layouts/AdminLayout';
import ProtectedRoute from '../guards/ProtectedRoute';
import LoginPage from '../../features/auth/pages/LoginPage';
import OtpPage from '../../features/auth/pages/OtpPage';
import BlockedAccountPage from '../../features/auth/pages/BlockedAccountPage';
import ModerationQueuePage from '../../features/moderation/pages/ModerationQueuePage';
import ManageTopicsPage from '../../features/community/pages/ManageTopicsPage';
import CreateContentPage from '../../features/contentManagement/pages/CreateContentPage';
import CreatePartnerProfilePage from '../../features/partnerGovernance/pages/CreatePartnerProfilePage';

const ForbiddenPage = () => (
  <div style={{ padding: 48, textAlign: 'center' }}>
    <h2>Access Denied</h2>
    <p>You do not have permission to view this page.</p>
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
          { path: '/', element: <Navigate to="/moderator" replace /> },
          {
            element: <ProtectedRoute requiredRoles={['MODERATOR', 'SYSTEM_ADMIN']} />,
            children: [
              { path: '/moderator', element: <ModerationQueuePage /> },
              { path: '/admin/topics', element: <ManageTopicsPage /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['CONTENT_ADMIN', 'SYSTEM_ADMIN']} />,
            children: [
              { path: '/content', element: <CreateContentPage /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['PARTNER', 'SYSTEM_ADMIN']} />,
            children: [
              { path: '/partner', element: <CreatePartnerProfilePage /> },
            ],
          },
          { path: '/admin', element: <Navigate to="/moderator" replace /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/login" replace /> },
]);
