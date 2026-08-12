/* eslint-disable react-refresh/only-export-components */
import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthLayout from '../layouts/AuthLayout';
import AdminLayout from '../layouts/AdminLayout';
import ContentLayout from '../layouts/ContentLayout';
import ProtectedRoute from '../guards/ProtectedRoute';
import RoleAwareRedirect from '../guards/RoleAwareRedirect';
import ExpertOnboardingGuard from '../guards/ExpertOnboardingGuard';
import ExpertLayout from '../layouts/ExpertLayout';

// Auth screens
import LoginPage from '../../features/auth/pages/LoginPage';
import FederatedRegisterPage from '../../features/auth/pages/FederatedRegisterPage';
import RegistrationVerificationPage from '../../features/auth/pages/RegistrationVerificationPage';
import OtpPage from '../../features/auth/pages/OtpPage';
import BlockedAccountPage from '../../features/auth/pages/BlockedAccountPage';
import NoWebAccessPage from '../../features/auth/pages/NoWebAccessPage';
import ExpertRegisterPage from '../../features/auth/pages/ExpertRegisterPage';
import AccountProfilePage from '../../features/auth/pages/AccountProfilePage';
import BabyCareHubPage from '../../features/babyCare/pages/BabyCareHubPage';
import BabyCareResourceNotFoundPage from '../../features/babyCare/pages/BabyCareResourceNotFoundPage';

// Expert Portal screens (CB-054, 055, 056, 057, 063)
import ExpertDashboardPage from '../../features/expert/pages/ExpertDashboardPage';
import ExpertProfilePage from '../../features/expert/pages/ExpertProfilePage';
import VerificationDocumentsPage from '../../features/expert/pages/VerificationDocumentsPage';
import AvailabilityCalendarPage from '../../features/expert/pages/AvailabilityCalendarPage';
import ExpertQuestionQueuePage from '../../features/expert/pages/ExpertQuestionQueuePage';
import ExpertConsultationRequestsPage from '../../features/expert/pages/ExpertConsultationRequestsPage';
import ExpertOnboardingPage from '../../features/expert/pages/ExpertOnboardingPage';

// Contribution screens

// Expert verification queue (admin side, UC-70)
import ExpertVerificationQueuePage from '../../features/expert/pages/ExpertVerificationQueuePage';

// UC-144 (redesign, CB-CHAT-IMP-144D) — Direct Consult Chat & Call, Expert Portal side
import ConversationListPage from '../../features/directChat/pages/ConversationListPage';
import ConversationRoomPage from '../../features/directChat/pages/ConversationRoomPage';

// Admin portal screens
import AdminDashboardPage from '../../features/admin/pages/AdminDashboardPage';
import UserListPage from '../../features/admin/pages/UserListPage';
import UserDetailPage from '../../features/admin/pages/UserDetailPage';
import UpdateUserRolePage from '../../features/admin/pages/UpdateUserRolePage';
import CreateStaffAccountPage from '../../features/admin/pages/CreateStaffAccountPage';
import AccountLockAppealsPage from '../../features/admin/pages/AccountLockAppealsPage';
import AccountLockAppealDetailPage from '../../features/admin/pages/AccountLockAppealDetailPage';
import ExpertListPage from '../../features/admin/pages/ExpertListPage';
import ExpertDetailPage from '../../features/admin/pages/ExpertDetailPage';

// Security & admin screens (TV1 Sprint 0)
import SecurityIncidentListPage from '../../features/security/pages/SecurityIncidentListPage';
import SecurityEventsPage from '../../features/security/pages/SecurityEventsPage';
import SecurityEventDetailPage from '../../features/security/pages/SecurityEventDetailPage';
import SecurityIncidentInvestigationPage from '../../features/security/pages/SecurityIncidentInvestigationPage';
import SecurityIncidentResolutionPage from '../../features/security/pages/SecurityIncidentResolutionPage';
import NotificationCenterPage from '../../features/notification/pages/NotificationCenterPage';

// Content Management screens (CB-073..081)
import ContentDashboardPage from '../../features/contentManagement/pages/ContentDashboardPage';
import ContentListPage from '../../features/contentManagement/pages/ContentListPage';
import ContentDetailPage from '../../features/contentManagement/pages/ContentDetailPage';
import ArticleListPage from '../../features/contentManagement/pages/ArticleListPage';
import FaqListPage from '../../features/contentManagement/pages/FaqListPage';
import ChecklistListPage from '../../features/contentManagement/pages/ChecklistListPage';
import ChecklistDetailPage from '../../features/contentManagement/pages/ChecklistDetailPage';
import ChecklistFormPage from '../../features/contentManagement/pages/ChecklistFormPage';
import ChecklistVersionHistoryPage from '../../features/contentManagement/pages/ChecklistVersionHistoryPage';
import ManageTopicsPage from '../../features/contentManagement/pages/ManageTopicsPage';

// Content Management screens (CB-076, 077, 079, 087)
import CreateContentPage from '../../features/contentManagement/pages/CreateContentPage';
import EditContentPage from '../../features/contentManagement/pages/EditContentPage';
import ContentVersionHistoryPage from '../../features/contentManagement/pages/ContentVersionHistoryPage';
import ContentApprovalQueuePage from '../../features/contentManagement/pages/ContentApprovalQueuePage';
import PregnancyExerciseListPage from '../../features/contentManagement/pages/PregnancyExerciseListPage';
import PregnancyExerciseDetailPage from '../../features/contentManagement/pages/PregnancyExerciseDetailPage';
import CreatePregnancyExercisePage from '../../features/contentManagement/pages/CreatePregnancyExercisePage';
import EditPregnancyExercisePage from '../../features/contentManagement/pages/EditPregnancyExercisePage';
import ExercisePreviewPage from '../../features/contentManagement/pages/ExercisePreviewPage';
import PostureConfigListPage from '../../features/postureConfiguration/pages/PostureConfigListPage';
import PostureConfigDetailPage from '../../features/postureConfiguration/pages/PostureConfigDetailPage';
import EditPostureConfigPage from '../../features/postureConfiguration/pages/EditPostureConfigPage';

// Partner Portal screens (CB-096, 097, 099)
import UnpublishContentPage from '../../features/contentManagement/pages/UnpublishContentPage';

// Moderation screens (CB-068, 069, 070, 071)
import ReportsQueuePage from '../../features/moderation/pages/ReportsQueuePage';
import ContentReportDetailPage from '../../features/moderation/pages/ContentReportDetailPage';
import AccountReportDetailPage from '../../features/moderation/pages/AccountReportDetailPage';
import ViolationHistoryPage from '../../features/moderation/pages/ViolationHistoryPage';
import ViolationDetailPage from '../../features/moderation/pages/ViolationDetailPage';
// CB-MOD-IMP-004: pending-content queue (first-time moderation, no ContentReport required)
import PendingContentQueuePage from '../../features/moderation/pages/PendingContentQueuePage';
import ModerationContentDetailPage from '../../features/moderation/pages/ModerationContentDetailPage';
import CommunityContentMonitorPage from '../../features/moderation/pages/CommunityContentMonitorPage';

// SYSTEM_ADMIN-only ModPortal screens (CB-066, 090, 091)
import CommunityDashboardPage from '../../features/dashboard/pages/CommunityDashboardPage';
import SafetyRuleManagementPage from '../../features/aiRuleManagement/pages/SafetyRuleManagementPage';
import SystemConfigurationPage from '../../features/aiRuleManagement/pages/SystemConfigurationPage';
import MaintenancePage from '../../features/system/pages/MaintenancePage';

const ForbiddenPage = () => (
  <div className="p-12 text-center font-sans text-on-surface-variant">
    <h2 className="text-primary">Truy cập bị từ chối</h2>
    <p>Bạn không có quyền xem trang này.</p>
  </div>
);

export const router = createBrowserRouter([
  { path: '/register', element: <FederatedRegisterPage /> },
  { path: '/register/verify', element: <RegistrationVerificationPage /> },
  {
    path: '/login',
    element: <AuthLayout />,
    children: [
      { index: true, element: <LoginPage /> },
      { path: 'otp', element: <OtpPage /> },
    ],
  },
  { path: '/forbidden', element: <ForbiddenPage /> },
  { path: '/maintenance', element: <MaintenancePage /> },
  { path: '/account-blocked', element: <BlockedAccountPage /> },
  { path: '/no-web-access', element: <NoWebAccessPage /> },
  { path: '/expert/register', element: <ExpertRegisterPage /> },
  { path: '/mother/baby-care', element: <BabyCareHubPage /> },
  { path: '/mother/babies/:babyId/daily-logs/:logId', element: <BabyCareResourceNotFoundPage /> },

  // Role-aware root redirect
  { path: '/', element: <RoleAwareRedirect /> },

  {
    element: <ProtectedRoute requiredRoles={['MOTHER', 'EXPERT']} />,
    children: [
      { path: '/direct-chats', element: <ConversationListPage /> },
      { path: '/direct-chats/:conversationId', element: <ConversationRoomPage /> },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [{ path: '/account/profile', element: <AccountProfilePage /> }],
  },

  {
    element: (
      <ProtectedRoute
        requiredRoles={['SYSTEM_ADMIN', 'EXPERT', 'CONTENT_ADMIN', 'MODERATOR']}
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
              { path: '/admin/users', element: <UserListPage /> },
              { path: '/admin/users/:userId', element: <UserDetailPage /> },
              { path: '/admin/users/:userId/role', element: <UpdateUserRolePage /> },
              { path: '/admin/staff-accounts/create', element: <CreateStaffAccountPage /> },
              { path: '/admin/account-lock-appeals', element: <AccountLockAppealsPage /> },
              { path: '/admin/account-lock-appeals/:appealId', element: <AccountLockAppealDetailPage /> },
              { path: '/admin/experts', element: <ExpertListPage /> },
              { path: '/admin/experts/:expertProfileId', element: <ExpertDetailPage /> },
              { path: '/admin/expert-verification-queue', element: <ExpertVerificationQueuePage /> },
              { path: '/admin/content-approval-queue', element: <ContentApprovalQueuePage /> },
              // Read-only review routes for the approval queue's "Xem chi tiết" links — System Admin
              // lacks CONTENT_ADMIN, so it cannot use /content/:id or /content/checklists/:id directly
              // (those are gated to CONTENT_ADMIN below); ContentDetailPage/ChecklistDetailPage already
              // hide all write actions when the viewer lacks CONTENT_ADMIN (see canManage in each page).
              { path: '/admin/content-review/:id', element: <ContentDetailPage /> },
              { path: '/admin/content-review/checklists/:id', element: <ChecklistDetailPage /> },
              { path: '/admin/security/incidents', element: <SecurityIncidentListPage /> },
              { path: '/admin/security/events', element: <SecurityEventsPage /> },
              { path: '/admin/security/events/:eventId', element: <SecurityEventDetailPage /> },
              { path: '/admin/security/incidents/:eventId/investigate', element: <SecurityIncidentInvestigationPage /> },
              { path: '/admin/security/incidents/:eventId/resolve', element: <SecurityIncidentResolutionPage /> },
              { path: '/admin/notifications', element: <NotificationCenterPage /> },
              { path: '/admin/posture-configs', element: <PostureConfigListPage /> },
              { path: '/admin/posture-configs/new', element: <EditPostureConfigPage /> },
              { path: '/admin/posture-configs/:exerciseId', element: <PostureConfigDetailPage /> },
              { path: '/admin/posture-configs/:exerciseId/edit', element: <EditPostureConfigPage /> },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['CONTENT_ADMIN']} />,
            children: [
              {
                element: <ContentLayout />,
                children: [
                  { path: '/content/dashboard', element: <ContentDashboardPage /> },
                  { path: '/content', element: <Navigate to="/content/dashboard" replace /> },
                  { path: '/content/create', element: <Navigate to="/content/list" replace /> },
                  { path: '/content/list', element: <ContentListPage /> },
                  { path: '/content/articles', element: <ArticleListPage /> },
                  { path: '/content/articles/create', element: <CreateContentPage contentType="ARTICLE" /> },
                  { path: '/content/:id', element: <ContentDetailPage /> },
                  { path: '/content/:id/edit', element: <EditContentPage /> },
                  { path: '/content/:id/versions', element: <ContentVersionHistoryPage /> },
                  { path: '/content/faq', element: <FaqListPage /> },
                  { path: '/content/faq/create', element: <CreateContentPage contentType="FAQ" /> },
                  { path: '/content/checklists', element: <ChecklistListPage /> },
                  { path: '/content/checklists/create', element: <ChecklistFormPage /> },
                  { path: '/content/checklists/:id', element: <ChecklistDetailPage /> },
                  { path: '/content/checklists/:id/edit', element: <ChecklistFormPage /> },
                  { path: '/content/checklists/:id/versions', element: <ChecklistVersionHistoryPage /> },
                  { path: '/content/notifications', element: <NotificationCenterPage /> },
                  { path: '/content/:id/unpublish', element: <UnpublishContentPage /> },
                  { path: '/content/exercises', element: <PregnancyExerciseListPage /> },
                  { path: '/content/exercises/create', element: <CreatePregnancyExercisePage /> },
                  { path: '/content/exercises/:exerciseId', element: <PregnancyExerciseDetailPage /> },
                  { path: '/content/exercises/:exerciseId/edit', element: <EditPregnancyExercisePage /> },
                  { path: '/content/exercises/:exerciseId/preview', element: <ExercisePreviewPage /> },
                  { path: '/content/exercises/preview', element: <ExercisePreviewPage /> },
                ],
              },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['MODERATOR', 'CONTENT_ADMIN']} />,
            children: [
              {
                element: <ContentLayout />,
                children: [{ path: '/content/topics', element: <ManageTopicsPage /> }],
              },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['EXPERT']} />,
            children: [
              {
                element: <ExpertLayout />,
                children: [
                  { path: '/expert/onboarding', element: <ExpertOnboardingPage /> },
                  {
                    element: <ExpertOnboardingGuard />,
                    children: [
                      { path: '/expert/dashboard', element: <ExpertDashboardPage /> },
                      { path: '/expert', element: <Navigate to="/expert/dashboard" replace /> },
                      // CB-055: Expert Profile
                      { path: '/expert/profile', element: <ExpertProfilePage /> },
                      // CB-056: Verification Documents
                      { path: '/expert/credentials', element: <VerificationDocumentsPage /> },
                      // CB-057: Availability Calendar
                      { path: '/expert/calendar', element: <AvailabilityCalendarPage /> },
                      // CB-063: Expert Question Queue
                      { path: '/expert/question-queue', element: <ExpertQuestionQueuePage /> },
                      // CB-064: Consultation Requests
                      { path: '/expert/consultation-requests', element: <ExpertConsultationRequestsPage /> },
                      // UC-144D: Direct Consult Chat & Call
                      { path: '/expert/direct-chats', element: <ConversationListPage /> },
                      { path: '/expert/direct-chats/:conversationId', element: <ConversationRoomPage /> },
                    ],
                  },
                ],
              },
            ],
          },
          {
            // ModerationController is @PreAuthorize hasRole('MODERATOR') on every endpoint
            // (queue, pending-content, reports, violations, resolve/actions) —
            // SYSTEM_ADMIN is NOT accepted by the backend here, so it must not be granted
            // frontend access either (it would 403 on every data call).
            element: <ProtectedRoute requiredRoles={['MODERATOR']} />,
            children: [
              { path: '/moderator', element: <Navigate to="/moderator/reports" replace /> },
              { path: '/moderator/queue', element: <Navigate to="/moderator/reports" replace /> },
              { path: '/moderator/moderator-dashboard', element: <CommunityDashboardPage /> },
              { path: '/moderator/pending-content', element: <PendingContentQueuePage /> },
              { path: '/moderator/pending-content/:targetType/:targetId', element: <ModerationContentDetailPage /> },
              { path: '/moderator/community-content', element: <CommunityContentMonitorPage /> },
              { path: '/moderator/reports', element: <ReportsQueuePage /> },
              { path: '/moderator/reports/account/:reportId', element: <AccountReportDetailPage /> },
              { path: '/moderator/reports/:reportId', element: <ContentReportDetailPage /> },
              { path: '/moderator/violations', element: <ViolationHistoryPage /> },
              { path: '/moderator/violations/:targetUserId', element: <ViolationDetailPage /> },
            ],
          },
          {
            // RedFlagRuleController
            element: <ProtectedRoute requiredRoles={['SYSTEM_ADMIN']} />,
            children: [
              { path: '/admin/safety-rules', element: <SafetyRuleManagementPage /> },
              { path: '/admin/system-configuration', element: <SystemConfigurationPage /> },
            ],
          },
        ],
      },
    ],
  },

  { path: '*', element: <Navigate to="/login" replace /> },
]);
