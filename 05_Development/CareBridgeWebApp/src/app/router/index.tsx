/* eslint-disable react-refresh/only-export-components */
import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthLayout from '../layouts/AuthLayout';
import AdminLayout from '../layouts/AdminLayout';
import ContentLayout from '../layouts/ContentLayout';
import ProtectedRoute from '../guards/ProtectedRoute';
import RoleAwareRedirect from '../guards/RoleAwareRedirect';
import ModeratorIndexRedirect from '../guards/ModeratorIndexRedirect';
import ExpertOnboardingGuard from '../guards/ExpertOnboardingGuard';

// Auth screens
import LoginPage from '../../features/auth/pages/LoginPage';
import FederatedRegisterPage from '../../features/auth/pages/FederatedRegisterPage';
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
import ExpertOnboardingPage from '../../features/expert/pages/ExpertOnboardingPage';

// Contribution screens
import ContributionListPage from '../../features/expert/pages/ContributionListPage';
import ContributionDraftPage from '../../features/expert/pages/ContributionDraftPage';
import ContributionDetailPage from '../../features/expert/pages/ContributionDetailPage';
import AdminContributionReviewQueuePage from '../../features/expert/pages/AdminContributionReviewQueuePage';

// Expert verification queue (admin side, UC-70)
import ExpertVerificationQueuePage from '../../features/expert/pages/ExpertVerificationQueuePage';
import AdminExpertIdentityReviewPage from '../../features/expert/pages/AdminExpertIdentityReviewPage';

// UC-144 (redesign, CB-CHAT-IMP-144D) — Direct Consult Chat & Call, Expert Portal side
import ConversationListPage from '../../features/directChat/pages/ConversationListPage';
import ConversationRoomPage from '../../features/directChat/pages/ConversationRoomPage';
import AdminExpertTrustManagementPage from '../../features/expert/pages/AdminExpertTrustManagementPage';

// Admin portal screens
import AdminDashboardPage from '../../features/admin/pages/AdminDashboardPage';

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
import ArticleListPage from '../../features/contentManagement/pages/ArticleListPage';
import FaqListPage from '../../features/contentManagement/pages/FaqListPage';
import ChecklistListPage from '../../features/contentManagement/pages/ChecklistListPage';
import ChecklistDetailPage from '../../features/contentManagement/pages/ChecklistDetailPage';
import ChecklistFormPage from '../../features/contentManagement/pages/ChecklistFormPage';
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
import PartnerLandingPage from '../../features/partnerGovernance/pages/PartnerLandingPage';
import RegisterPartnerPage from '../../features/partnerGovernance/pages/RegisterPartnerPage';
import CreatePartnerProfilePage from '../../features/partnerGovernance/pages/CreatePartnerProfilePage';
import PartnerProfilePage from '../../features/partnerGovernance/pages/PartnerProfilePage';
import PartnerVerificationQueuePage from '../../features/partnerGovernance/pages/PartnerVerificationQueuePage';
import UnpublishContentPage from '../../features/contentManagement/pages/UnpublishContentPage';

// Moderation screens (CB-068, 069, 070, 071)
import ReportsQueuePage from '../../features/moderation/pages/ReportsQueuePage';
import ContentReportDetailPage from '../../features/moderation/pages/ContentReportDetailPage';
import AccountReportDetailPage from '../../features/moderation/pages/AccountReportDetailPage';
import ViolationHistoryPage from '../../features/moderation/pages/ViolationHistoryPage';
import ViolationDetailPage from '../../features/moderation/pages/ViolationDetailPage';
// CB-MOD-IMP-004: pending-content queue (first-time moderation, no ContentReport required)
import PendingContentQueuePage from '../../features/moderation/pages/PendingContentQueuePage';

// SYSTEM_ADMIN-only ModPortal screens (CB-066, 090, 091)
import CommunityDashboardPage from '../../features/dashboard/pages/CommunityDashboardPage';
import SafetyRuleManagementPage from '../../features/aiRuleManagement/pages/SafetyRuleManagementPage';
import SystemConfigurationPage from '../../features/aiRuleManagement/pages/SystemConfigurationPage';

const ForbiddenPage = () => (
  <div className="p-12 text-center font-sans text-on-surface-variant">
    <h2 className="text-primary">Truy cập bị từ chối</h2>
    <p>Bạn không có quyền xem trang này.</p>
  </div>
);

export const router = createBrowserRouter([
  { path: '/register', element: <FederatedRegisterPage /> },
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
  { path: '/expert/register', element: <ExpertRegisterPage /> },
  { path: '/mother/baby-care', element: <BabyCareHubPage /> },
  { path: '/mother/babies/:babyId/daily-logs/:logId', element: <BabyCareResourceNotFoundPage /> },

  // Partner Portal — public pages (no auth required)
  { path: '/partner', element: <PartnerLandingPage /> },
  { path: '/partner/register', element: <RegisterPartnerPage /> },

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
              { path: '/admin/expert-verification-queue', element: <ExpertVerificationQueuePage /> },
              { path: '/admin/expert-identity-queue', element: <AdminExpertIdentityReviewPage /> },
{ path: '/admin/expert-trust-management', element: <AdminExpertTrustManagementPage /> },
              { path: '/admin/content-approval-queue', element: <ContentApprovalQueuePage /> },
              // Read-only review routes for the approval queue's "Xem chi tiết" links — System Admin
              // lacks CONTENT_ADMIN, so it cannot use /content/:id or /content/checklists/:id directly
              // (those are gated to CONTENT_ADMIN below); ContentDetailPage/ChecklistDetailPage already
              // hide all write actions when the viewer lacks CONTENT_ADMIN (see canManage in each page).
              { path: '/admin/content-review/:id', element: <ContentDetailPage /> },
              { path: '/admin/content-review/checklists/:id', element: <ChecklistDetailPage /> },
              { path: '/admin/contribution-review-queue', element: <AdminContributionReviewQueuePage /> },
{ path: '/security/incidents', element: <SecurityIncidentListPage /> },
              { path: '/security/events', element: <SecurityEventsPage /> },
              { path: '/security/events/:eventId', element: <SecurityEventDetailPage /> },
              { path: '/security/incidents/:eventId/investigate', element: <SecurityIncidentInvestigationPage /> },
              { path: '/security/incidents/:eventId/resolve', element: <SecurityIncidentResolutionPage /> },
              { path: '/notifications', element: <NotificationCenterPage /> },
              { path: '/settings/privacy', element: <PrivacySettingsPage /> },
              { path: '/posture-configs', element: <PostureConfigListPage /> },
              { path: '/posture-configs/new', element: <EditPostureConfigPage /> },
              { path: '/posture-configs/:exerciseId', element: <PostureConfigDetailPage /> },
              { path: '/posture-configs/:exerciseId/edit', element: <EditPostureConfigPage /> },
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
                  { path: '/content/create', element: <CreateContentPage /> },
                  { path: '/content/list', element: <ContentListPage /> },
                  { path: '/content/articles', element: <ArticleListPage /> },
                  { path: '/content/:id', element: <ContentDetailPage /> },
                  { path: '/content/:id/edit', element: <EditContentPage /> },
                  { path: '/content/:id/versions', element: <ContentVersionHistoryPage /> },
                  { path: '/content/faq', element: <FaqListPage /> },
                  { path: '/content/checklists', element: <ChecklistListPage /> },
                  { path: '/content/checklists/create', element: <ChecklistFormPage /> },
                  { path: '/content/checklists/:id', element: <ChecklistDetailPage /> },
                  { path: '/content/checklists/:id/edit', element: <ChecklistFormPage /> },
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
                  // UC-144D: Direct Consult Chat & Call
                  { path: '/expert/direct-chats', element: <ConversationListPage /> },
                  { path: '/expert/direct-chats/:conversationId', element: <ConversationRoomPage /> },
                  // Contribution (Medical Knowledge)
                  { path: '/expert/contributions', element: <ContributionListPage /> },
                  { path: '/expert/contributions/new', element: <ContributionDraftPage /> },
                  { path: '/expert/contributions/:id/edit', element: <ContributionDraftPage /> },
                  { path: '/expert/contributions/:id', element: <ContributionDetailPage /> },
                ],
              },
            ],
          },
          {
            element: <ProtectedRoute requiredRoles={['PARTNER']} />,
            children: [
              { path: '/partner/dashboard', element: <AdminDashboardPage /> },
              { path: '/partner/profile-setup', element: <CreatePartnerProfilePage /> },
              { path: '/partner/profile', element: <PartnerProfilePage /> },
            ],
          },
          {
            // ModerationController is @PreAuthorize hasRole('MODERATOR') on every endpoint
            // (queue, pending-content, reports, violations, resolve/actions) —
            // SYSTEM_ADMIN is NOT accepted by the backend here, so it must not be granted
            // frontend access either (it would 403 on every data call).
            element: <ProtectedRoute requiredRoles={['MODERATOR']} />,
            children: [
              // /moderator/queue (UC-99 View Moderation Queue) is served by ReportsQueuePage —
              // redirect the old path so bookmarks/links keep working without a duplicate sidebar entry.
              { path: '/moderator/queue', element: <Navigate to="/moderator/reports" replace /> },
              { path: '/moderator/pending-content', element: <PendingContentQueuePage /> },
              { path: '/moderator/reports', element: <ReportsQueuePage /> },
              { path: '/moderator/reports/account/:reportId', element: <AccountReportDetailPage /> },
              { path: '/moderator/reports/:reportId', element: <ContentReportDetailPage /> },
              { path: '/moderator/violations', element: <ViolationHistoryPage /> },
              { path: '/moderator/violations/:targetUserId', element: <ViolationDetailPage /> },
            ],
          },
          {
            // RedFlagRuleController / CommunityDashboardController
            element: <ProtectedRoute requiredRoles={['SYSTEM_ADMIN']} />,
            children: [
              { path: '/moderator/dashboard', element: <CommunityDashboardPage /> },
              { path: '/moderator/safety-rules', element: <SafetyRuleManagementPage /> },
              { path: '/moderator/system-configuration', element: <SystemConfigurationPage /> },
              { path: '/admin/partners/verification', element: <PartnerVerificationQueuePage /> },
            ],
          },
          {
            // Role-aware landing for the bare '/moderator' entry point — MODERATOR and
            // SYSTEM_ADMIN each only have backend access to a disjoint subset of ModPortal
            // pages (see the two guards above), so a single hardcoded redirect can't serve both.
            element: <ProtectedRoute requiredRoles={['MODERATOR', 'SYSTEM_ADMIN']} />,
            children: [{ path: '/moderator', element: <ModeratorIndexRedirect /> }],
          },
        ],
      },
    ],
  },

  { path: '*', element: <Navigate to="/login" replace /> },
]);
