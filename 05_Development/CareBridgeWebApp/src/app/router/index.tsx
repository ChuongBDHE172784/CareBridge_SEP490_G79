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
import ExpertDashboardPage from '../../features/expert/pages/ExpertDashboardPage';

// Security & admin screens (TV1 Sprint 0)
import SecurityIncidentListPage from '../../features/security/pages/SecurityIncidentListPage';
import SecurityEventsPage from '../../features/security/pages/SecurityEventsPage';
import SecurityEventDetailPage from '../../features/security/pages/SecurityEventDetailPage';
import SecurityIncidentInvestigationPage from '../../features/security/pages/SecurityIncidentInvestigationPage';
import SecurityIncidentResolutionPage from '../../features/security/pages/SecurityIncidentResolutionPage';
import NotificationCenterPage from '../../features/notification/pages/NotificationCenterPage';
import PrivacySettingsPage from '../../features/settings/pages/PrivacySettingsPage';

// Content Management screens (CB-073..081)
import ContentDashboardPage from '../../features/contentManagement/pages/ContentDashboardPage';
import ContentListPage from '../../features/contentManagement/pages/ContentListPage';
import ContentDetailPage from '../../features/contentManagement/pages/ContentDetailPage';
import ContentPreviewPage from '../../features/contentManagement/pages/ContentPreviewPage';
import FaqListPage from '../../features/contentManagement/pages/FaqListPage';
import ChecklistListPage from '../../features/contentManagement/pages/ChecklistListPage';
import ManageTopicsPage from '../../features/contentManagement/pages/ManageTopicsPage';

// Content Management screens (CB-076, CB-077, CB-079, CB-087)
import CreateContentPage from '../../features/contentManagement/pages/CreateContentPage';
import EditContentPage from '../../features/contentManagement/pages/EditContentPage';
import ContentVersionHistoryPage from '../../features/contentManagement/pages/ContentVersionHistoryPage';
import ContentApprovalQueuePage from '../../features/contentManagement/pages/ContentApprovalQueuePage';

// Partner Portal screens (CB-096, CB-097)
import PartnerLandingPage from '../../features/partnerGovernance/pages/PartnerLandingPage';
import RegisterPartnerPage from '../../features/partnerGovernance/pages/RegisterPartnerPage';

// Moderation screens (CB-072, CB-088)
import EscalatedModerationCasesPage from '../../features/moderation/pages/EscalatedModerationCasesPage';
import EscalatedSafetyCasePage from '../../features/moderation/pages/EscalatedSafetyCasePage';

// Moderation screens (CB-068, CB-069, CB-070, CB-071)
import ModerationItemDetailPage from '../../features/moderation/pages/ModerationItemDetailPage';
import ReportsQueuePage from '../../features/moderation/pages/ReportsQueuePage';
import ContentReportDetailPage from '../../features/moderation/pages/ContentReportDetailPage';
import AccountReportDetailPage from '../../features/moderation/pages/AccountReportDetailPage';
import ViolationHistoryPage from '../../features/moderation/pages/ViolationHistoryPage';

const ForbiddenPage = () => (
  <div className="p-12 text-center font-sans text-on-surface-variant">
    <h2 className="text-primary">Truy cập bị từ chối</h2>
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

  // Partner Portal — public pages (no auth required)
  { path: '/partner', element: <PartnerLandingPage /> },
  { path: '/partner/register', element: <RegisterPartnerPage /> },

  // Role-aware root redirect — sits outside ProtectedRoute so all roles are handled,
  // including MOTHER/FAMILY who land on /no-web-access without hitting /forbidden.
  { path: '/', element: <RoleAwareRedirect /> },

  {
    element: (
      <ProtectedRoute
        requiredRoles={['SYSTEM_ADMIN', 'EXPERT', 'CONTENT_ADMIN', 'PARTNER', 'MODERATOR']}
      />
    ),
    children: [
      {
        element: <AdminLayout />,
        children: [
          {
            element: <ProtectedRoute requiredRoles={['SYSTEM_ADMIN']} />,
            children: [
              { path: '/admin/dashboard', element: <AdminDashboardPage /> },
              { path: '/admin', element: <Navigate to="/admin/dashboard" replace /> },
              { path: '/security/incidents', element: <SecurityIncidentListPage /> },
              { path: '/security/events', element: <SecurityEventsPage /> },
              { path: '/security/events/:eventId', element: <SecurityEventDetailPage /> },
              { path: '/security/incidents/:eventId/investigate', element: <SecurityIncidentInvestigationPage /> },
              { path: '/security/incidents/:eventId/resolve', element: <SecurityIncidentResolutionPage /> },
              { path: '/notifications', element: <NotificationCenterPage /> },
              { path: '/settings/privacy', element: <PrivacySettingsPage /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['CONTENT_ADMIN', 'SYSTEM_ADMIN']} />,
            children: [
              { path: '/content/dashboard', element: <ContentDashboardPage /> },
              { path: '/content', element: <Navigate to="/content/dashboard" replace /> },
              { path: '/content/create', element: <CreateContentPage /> },
              { path: '/content/list', element: <ContentListPage /> },
              { path: '/content/:id', element: <ContentDetailPage /> },
              { path: '/content/:id/edit', element: <EditContentPage /> },
              { path: '/content/:id/preview', element: <ContentPreviewPage /> },
              { path: '/content/:id/versions', element: <ContentVersionHistoryPage /> },
              { path: '/content/faq', element: <FaqListPage /> },
              { path: '/content/checklists', element: <ChecklistListPage /> },
            ],
          },
          {
            // ContentApprovalController is @PreAuthorize hasRole('SYSTEM_ADMIN') at class level —
            // guard matches the backend, not the broader CONTENT_ADMIN content group above.
            element: <ProtectedRoute requiredRoles={['SYSTEM_ADMIN']} />,
            children: [
              { path: '/content/approval-queue', element: <ContentApprovalQueuePage /> },
            ],
          },
          {
            // UC-109: topic management is MODERATOR-only per the approved TDS
            // (BR-RBAC: "Chỉ ROLE_MODERATOR được tạo/sửa/ẩn topic") — the backend
            // enforces hasRole('MODERATOR') with no SYSTEM_ADMIN override, so this
            // guard intentionally does not include CONTENT_ADMIN/SYSTEM_ADMIN.
            element: <ProtectedRoute requiredRoles={['MODERATOR']} />,
            children: [{ path: '/content/topics', element: <ManageTopicsPage /> }],
          },
          {
            element: <ProtectedRoute requiredRoles={['EXPERT']} />,
            children: [
              { path: '/expert/dashboard', element: <ExpertDashboardPage /> },
              { path: '/expert', element: <Navigate to="/expert/dashboard" replace /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['PARTNER']} />,
            children: [
              { path: '/partner/dashboard', element: <AdminDashboardPage /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['MODERATOR', 'SYSTEM_ADMIN']} />,
            children: [
              { path: '/moderator/dashboard', element: <EscalatedModerationCasesPage /> },
              { path: '/moderator/queue', element: <EscalatedModerationCasesPage /> },
              { path: '/moderator/queue/:reportId', element: <ModerationItemDetailPage /> },
              { path: '/moderator/reports', element: <ReportsQueuePage /> },
              { path: '/moderator/reports/account/:reportId', element: <AccountReportDetailPage /> },
              { path: '/moderator/reports/:reportId', element: <ContentReportDetailPage /> },
              { path: '/moderator/violations', element: <ViolationHistoryPage /> },
              { path: '/moderator/safety-cases', element: <EscalatedModerationCasesPage /> },
              { path: '/moderator/safety-cases/:caseId', element: <EscalatedSafetyCasePage /> },
              { path: '/moderator', element: <Navigate to="/moderator/safety-cases" replace /> },
            ],
          },
        ],
      },
    ],
  },

  { path: '*', element: <Navigate to="/login" replace /> },
]);
