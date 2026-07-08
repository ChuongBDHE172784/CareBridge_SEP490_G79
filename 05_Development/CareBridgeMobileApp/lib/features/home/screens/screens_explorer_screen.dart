import 'package:flutter/material.dart';

// Auth & profile
import '../../auth/screens/welcome_screen.dart';
import '../../auth/screens/login_screen.dart';
import '../../auth/screens/register_screen.dart';
import '../../auth/screens/forgot_password_screen.dart';
import '../../auth/screens/reset_password_screen.dart';
import '../../auth/screens/otp_verification_screen.dart';
import '../../auth/screens/change_password_screen.dart';
import '../../auth/screens/account_profile_screen.dart';
import '../../auth/screens/edit_profile_screen.dart';
import '../../auth/screens/deactivate_account_screen.dart';
import '../../auth/screens/blocked_account_screen.dart';
import '../../auth/screens/logout_confirmation_screen.dart';

// Journey
import '../../journey/screens/mother_journey_screen.dart';
import '../../journey/screens/journey_setup_screen.dart';

// Community
import '../../community/screens/community_feed_screen.dart';
import '../../community/screens/community_search_screen.dart';
import '../../community/screens/community_topic_search_screen.dart';
import '../../community/screens/topic_directory_screen.dart';
import '../../community/screens/verified_content_search_screen.dart';
import '../../community/screens/create_question_screen.dart';
import '../../community/screens/edit_question_screen.dart';
import '../../community/screens/question_detail_screen.dart';
import '../../community/screens/post_answer_screen.dart';
import '../../community/screens/expert_question_queue_screen.dart';

// Session
import '../../session/screens/login_sessions_screen.dart';

// Reminder & Tasks
import '../../reminder/screens/today_tasks_screen.dart';
import '../../reminder/screens/reminder_detail_screen.dart';

// Privacy
import '../../privacy/screens/privacy_settings_screen.dart';

// Notification
import '../../notification/screens/notification_preferences_screen.dart';
import '../../notification/screens/notification_detail_screen.dart';
import '../../notification/screens/notification_center_screen.dart';
import '../../notification/screens/notifications_screen.dart';
import '../../notification/models/notification_model.dart';

// Health Records
import '../../healthRecords/screens/health_record_timeline_screen.dart';
import '../../healthRecords/screens/maternal_health_metric_screen.dart';
import '../../healthRecords/screens/search_content_screen.dart';
import '../../healthRecords/screens/view_content_screen.dart';
import '../../healthRecords/screens/vaccination_detail_screen.dart';

// FileManager
import '../../fileManager/screens/file_manager_screen.dart';
import '../../fileManager/screens/upload_file_screen.dart';

// FamilySync
import '../../familySync/screens/care_groups_screen.dart';
import '../../familySync/screens/my_care_groups_screen.dart';
import '../../familySync/screens/care_group_detail_screen.dart';
import '../../familySync/screens/care_group_members_screen.dart';
import '../../familySync/models/care_group_model.dart';

// Exercise
import '../../exercise/screens/pre_exercise_safety_check_screen.dart';
import '../../exercise/screens/exercise_session_screen.dart';
import '../../exercise/screens/exercise_session_result_screen.dart';
import '../../exercise/screens/exercise_history_screen.dart';
import '../../exercise/models/exercise_model.dart';

class ScreensExplorerScreen extends StatefulWidget {
  const ScreensExplorerScreen({super.key});

  @override
  State<ScreensExplorerScreen> createState() => _ScreensExplorerScreenState();
}

class _ScreensExplorerScreenState extends State<ScreensExplorerScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceContainer = Color(0xFFFFE9E3);

  final _searchController = TextEditingController();
  String _searchQuery = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final categories = _getScreenCategories(context);

    // Filter categories based on search query
    final filteredCategories = categories.map((category) {
      final filteredScreens = category.screens.where((screen) {
        return screen.name.toLowerCase().contains(_searchQuery.toLowerCase()) ||
               screen.description.toLowerCase().contains(_searchQuery.toLowerCase());
      }).toList();
      return _ScreenCategory(
        name: category.name,
        icon: category.icon,
        screens: filteredScreens,
      );
    }).where((category) => category.screens.isNotEmpty).toList();

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            _buildSearchBox(),
            Expanded(
              child: filteredCategories.isEmpty
                  ? _buildEmptyState()
                  : ListView.builder(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 80),
                      itemCount: filteredCategories.length,
                      itemBuilder: (context, index) {
                        final cat = filteredCategories[index];
                        return _buildCategorySection(cat);
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Thư mục Màn hình',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: _primary,
            ),
          ),
          const SizedBox(height: 4),
          const Text(
            'Danh sách tất cả các màn hình đã được triển khai phục vụ kiểm thử và phát triển.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSearchBox() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withValues(alpha: 0.08),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: TextField(
        controller: _searchController,
        onChanged: (val) {
          setState(() {
            _searchQuery = val;
          });
        },
        decoration: InputDecoration(
          hintText: 'Tìm kiếm màn hình...',
          hintStyle: const TextStyle(fontFamily: 'Lexend', color: Colors.grey),
          prefixIcon: const Icon(Icons.search, color: _primary),
          suffixIcon: _searchQuery.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear, color: Colors.grey),
                  onPressed: () {
                    _searchController.clear();
                    setState(() {
                      _searchQuery = '';
                    });
                  },
                )
              : null,
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        ),
        style: const TextStyle(fontFamily: 'Lexend', color: _onSurface),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.search_off_outlined, size: 64, color: _primaryContainer.withValues(alpha: 0.5)),
          const SizedBox(height: 16),
          const Text(
            'Không tìm thấy màn hình nào phù hợp',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCategorySection(_ScreenCategory category) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Row(
            children: [
              Icon(category.icon, color: _primary, size: 20),
              const SizedBox(width: 8),
              Text(
                category.name,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: _primary,
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: _surfaceContainer,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  '${category.screens.length}',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: _primary,
                  ),
                ),
              ),
            ],
          ),
        ),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.4,
          ),
          itemCount: category.screens.length,
          itemBuilder: (context, index) {
            final screen = category.screens[index];
            return _buildScreenCard(screen);
          },
        ),
        const SizedBox(height: 8),
      ],
    );
  }

  Widget _buildScreenCard(_ScreenItem item) {
    return InkWell(
      onTap: () {
        Navigator.of(context).push(
          MaterialPageRoute(builder: item.builder),
        );
      },
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF5A463F).withValues(alpha: 0.05),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
          border: Border.all(
            color: const Color(0xFFFFEAE4),
            width: 1,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.bold,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Expanded(
                    child: Text(
                      item.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 10,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                if (item.code != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF1EC),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      item.code!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 9,
                        fontWeight: FontWeight.bold,
                        color: _primaryContainer,
                      ),
                    ),
                  )
                else
                  const SizedBox.shrink(),
                const Icon(
                  Icons.arrow_forward_ios,
                  size: 10,
                  color: _primaryContainer,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  List<_ScreenCategory> _getScreenCategories(BuildContext context) {
    return [
      _ScreenCategory(
        name: 'Tài khoản & Xác thực',
        icon: Icons.person_outline,
        screens: [
          _ScreenItem(
            name: 'Chào mừng (Welcome)',
            description: 'Màn hình khởi đầu ứng dụng',
            code: 'CB-001',
            builder: (_) => const WelcomeScreen(),
          ),
          _ScreenItem(
            name: 'Đăng nhập (Login)',
            description: 'Màn hình đăng nhập',
            code: 'CB-002',
            builder: (_) => const LoginScreen(),
          ),
          _ScreenItem(
            name: 'Đăng ký (Register)',
            description: 'Màn hình tạo tài khoản mới',
            code: 'CB-003',
            builder: (_) => const RegisterScreen(),
          ),
          _ScreenItem(
            name: 'Quên mật khẩu',
            description: 'Yêu cầu đặt lại mật khẩu',
            code: 'CB-004',
            builder: (_) => const ForgotPasswordScreen(),
          ),
          _ScreenItem(
            name: 'Đặt lại mật khẩu',
            description: 'Thiết lập mật khẩu mới',
            code: 'CB-006',
            builder: (_) => const ResetPasswordScreen(token: 'mock-token'),
          ),
          _ScreenItem(
            name: 'Xác thực OTP',
            description: 'Xác minh OTP gửi qua email',
            code: 'CB-005',
            builder: (_) => const OtpVerificationScreen(identifier: 'test@example.com', isEmail: true),
          ),
          _ScreenItem(
            name: 'Đổi mật khẩu',
            description: 'Đổi mật khẩu trong cài đặt',
            code: 'CB-103',
            builder: (_) => const ChangePasswordScreen(),
          ),
          _ScreenItem(
            name: 'Hồ sơ tài khoản',
            description: 'Thông tin tài khoản cá nhân',
            code: 'CB-010',
            builder: (_) => const AccountProfileScreen(),
          ),
          _ScreenItem(
            name: 'Chỉnh sửa hồ sơ',
            description: 'Cập nhật thông tin cá nhân',
            code: 'CB-012',
            builder: (_) => const EditProfileScreen(),
          ),
          _ScreenItem(
            name: 'Vô hiệu hóa tài khoản',
            description: 'Tạm khóa hoặc hủy tài khoản',
            code: 'CB-104',
            builder: (_) => const DeactivateAccountScreen(),
          ),
          _ScreenItem(
            name: 'Tài khoản bị khóa',
            description: 'Thông báo tài khoản bị khóa',
            code: 'CB-115',
            builder: (_) => const BlockedAccountScreen(),
          ),
          _ScreenItem(
            name: 'Xác nhận Đăng xuất',
            description: 'Hộp thoại xác nhận đăng xuất',
            code: 'CB-011',
            builder: (_) => const LogoutConfirmationScreenWrapper(),
          ),
          _ScreenItem(
            name: 'Phiên đăng nhập',
            description: 'Quản lý các thiết bị đã đăng nhập',
            code: 'CB-101',
            builder: (_) => const LoginSessionsScreen(),
          ),
        ],
      ),
      _ScreenCategory(
        name: 'Hành trình & Sự kiện',
        icon: Icons.calendar_today_outlined,
        screens: [
          _ScreenItem(
            name: 'Hành trình của Mẹ',
            description: 'Dòng thời gian thai kỳ/sau sinh',
            code: 'CB-023',
            builder: (_) => const MotherJourneyScreen(),
          ),
          _ScreenItem(
            name: 'Thiết lập Hành trình',
            description: 'Cài đặt ngày dự sinh / thông tin bé',
            code: 'CB-022',
            builder: (_) => const JourneySetupScreen(),
          ),
        ],
      ),
      _ScreenCategory(
        name: 'Cộng đồng & Hỏi đáp',
        icon: Icons.forum_outlined,
        screens: [
          _ScreenItem(
            name: 'Bảng tin Cộng đồng',
            description: 'Danh sách câu hỏi từ cộng đồng',
            code: 'CB-013',
            builder: (_) => const CommunityFeedScreen(),
          ),
          _ScreenItem(
            name: 'Tìm kiếm Cộng đồng',
            description: 'Tìm kiếm câu hỏi và chủ đề',
            code: 'CB-014',
            builder: (_) => const CommunitySearchScreen(),
          ),
          _ScreenItem(
            name: 'Tìm câu hỏi theo Chủ đề',
            description: 'Bộ lọc câu hỏi theo chủ đề',
            code: 'CB-151',
            builder: (_) => const CommunityTopicSearchScreen(),
          ),
          _ScreenItem(
            name: 'Thư mục Chủ đề',
            description: 'Danh sách các chủ đề cộng đồng',
            code: 'CB-153',
            builder: (_) => const TopicDirectoryScreen(),
          ),
          _ScreenItem(
            name: 'Nội dung kiểm chứng',
            description: 'Bài viết và câu hỏi được kiểm chứng',
            code: 'CB-152',
            builder: (_) => const VerifiedContentSearchScreen(),
          ),
          _ScreenItem(
            name: 'Màn hình Đặt câu hỏi',
            description: 'Đặt câu hỏi mới lên cộng đồng',
            code: 'CB-015',
            builder: (_) => const CreateQuestionScreen(),
          ),
          _ScreenItem(
            name: 'Màn hình Sửa câu hỏi',
            description: 'Chỉnh sửa câu hỏi hiện tại',
            code: 'CB-146',
            builder: (_) => const EditQuestionScreen(
              questionId: '00000000-0000-0000-0000-000000000000',
              initialTitle: 'Câu hỏi mẫu',
              initialBody: 'Nội dung câu hỏi mẫu',
              initialIsAnonymous: false,
              initialUrgency: 'NORMAL',
            ),
          ),
          _ScreenItem(
            name: 'Chi tiết Câu hỏi',
            description: 'Chi tiết câu hỏi và các câu trả lời',
            code: 'CB-016',
            builder: (_) => const QuestionDetailScreen(questionId: '00000000-0000-0000-0000-000000000000'),
          ),
          _ScreenItem(
            name: 'Đăng câu trả lời',
            description: 'Màn hình viết câu trả lời',
            code: 'CB-150',
            builder: (_) => const PostAnswerScreen(
              questionId: '00000000-0000-0000-0000-000000000000',
              questionTitle: 'Câu hỏi mẫu để trả lời',
              authorName: 'Nguyễn Thị A',
              topicName: 'Dinh dưỡng thai kỳ',
              timeAgo: '2 giờ trước',
            ),
          ),
          _ScreenItem(
            name: 'Hàng chờ chuyên gia',
            description: 'Danh sách câu hỏi chờ chuyên gia trả lời',
            code: 'CB-149',
            builder: (_) => const ExpertQuestionQueueScreen(expertName: 'Bác sĩ Mai Anh'),
          ),
        ],
      ),
      _ScreenCategory(
        name: 'Nhắc nhở & Công việc',
        icon: Icons.alarm_on_outlined,
        screens: [
          _ScreenItem(
            name: 'Việc cần làm hôm nay',
            description: 'Danh sách công việc và lịch nhắc nhở',
            code: 'CB-017',
            builder: (_) => const TodayTasksScreen(),
          ),
          _ScreenItem(
            name: 'Chi tiết Lịch nhắc',
            description: 'Chi tiết và cấu hình nhắc nhở',
            code: 'CB-018',
            builder: (_) => const ReminderDetailScreen(reminderId: 'r-1'),
          ),
        ],
      ),
      _ScreenCategory(
        name: 'Hồ sơ Sức khỏe & Kiến thức',
        icon: Icons.health_and_safety_outlined,
        screens: [
          _ScreenItem(
            name: 'Dòng thời gian Sức khỏe',
            description: 'Dòng thời gian hồ sơ y tế',
            code: 'CB-171',
            builder: (_) => const HealthRecordTimelineScreen(),
          ),
          _ScreenItem(
            name: 'Chỉ số Sức khỏe Mẹ',
            description: 'Theo dõi cân nặng, huyết áp...',
            code: 'CB-025',
            builder: (_) => const MaternalHealthMetricScreen(metricId: 'weight'),
          ),
          _ScreenItem(
            name: 'Tìm kiếm Kiến thức',
            description: 'Tra cứu bài viết, cẩm nang y tế',
            code: 'CB-177',
            builder: (_) => const SearchContentScreen(),
          ),
          _ScreenItem(
            name: 'Kiến thức Sức khỏe',
            description: 'Bài viết và checklist theo giai đoạn',
            code: 'CB-175',
            builder: (_) => const ViewContentScreen(),
          ),
          _ScreenItem(
            name: 'Chi tiết Tiêm chủng',
            description: 'Hồ sơ tiêm chủng của bé',
            code: 'CB-173',
            builder: (_) => const VaccinationDetailScreen(vaccinationId: 'v-1'),
          ),
        ],
      ),
      _ScreenCategory(
        name: 'Quản lý Tệp tin',
        icon: Icons.folder_open_outlined,
        screens: [
          _ScreenItem(
            name: 'Kho tài liệu',
            description: 'Quản lý hình ảnh, kết quả xét nghiệm',
            code: 'CB-033',
            builder: (_) => const FileManagerScreen(),
          ),
          _ScreenItem(
            name: 'Tải lên tài liệu',
            description: 'Tải lên hình ảnh hoặc PDF',
            code: 'CB-034',
            builder: (_) => const UploadFileScreen(),
          ),
        ],
      ),
      _ScreenCategory(
        name: 'Đồng bộ Gia đình',
        icon: Icons.people_outline,
        screens: [
          _ScreenItem(
            name: 'Nhóm chăm sóc',
            description: 'Tạo hoặc tham gia nhóm gia đình',
            code: 'CB-026',
            builder: (_) => const CareGroupsScreen(),
          ),
          _ScreenItem(
            name: 'Nhóm của tôi',
            description: 'Danh sách các nhóm đang tham gia',
            code: 'CB-028',
            builder: (_) => const MyCareGroupsScreen(),
          ),
          _ScreenItem(
            name: 'Chi tiết Nhóm',
            description: 'Thông tin nhóm và bảng điều khiển chung',
            code: 'CB-027',
            builder: (_) => const CareGroupDetailScreen(groupId: 'group-1', groupName: 'Gia đình nhỏ'),
          ),
          _ScreenItem(
            name: 'Thành viên Nhóm',
            description: 'Quản lý thành viên trong nhóm chăm sóc',
            code: 'CB-168',
            builder: (_) => CareGroupMembersScreen(
              groupId: 'group-1',
              groupName: 'Gia đình nhỏ',
              members: [
                CareGroupMember(
                  memberId: 'm-1',
                  displayName: 'Mẹ Linh',
                  memberRole: 'ADMIN',
                  inviteStatus: 'ACCEPTED',
                  joinedAt: DateTime(2024, 1, 5),
                ),
                CareGroupMember(
                  memberId: 'm-2',
                  displayName: 'Bố Tuấn',
                  memberRole: 'MEMBER',
                  inviteStatus: 'ACCEPTED',
                  joinedAt: DateTime(2024, 1, 10),
                ),
              ],
            ),
          ),
        ],
      ),
      _ScreenCategory(
        name: 'Luyện tập (Exercise)',
        icon: Icons.sports_gymnastics_outlined,
        screens: [
          _ScreenItem(
            name: 'Kiểm tra An toàn',
            description: 'Đánh giá sức khỏe trước tập luyện',
            code: 'CB-183',
            builder: (_) => const PreExerciseSafetyCheckScreen(),
          ),
          _ScreenItem(
            name: 'Màn hình Tập luyện',
            description: 'Trình phát bài tập có phân tích tư thế',
            code: 'CB-184',
            builder: (_) => const ExerciseSessionScreen(
              exerciseId: 'ex-1',
              exerciseTitle: 'Yoga dưỡng thai',
              instruction: 'Tập trung hít thở sâu và duỗi người nhẹ nhàng.',
              durationMinutes: 5,
              sessionId: 'sess-1',
            ),
          ),
          _ScreenItem(
            name: 'Kết quả Tập luyện',
            description: 'Thống kê kết quả sau bài tập',
            code: 'CB-185',
            builder: (_) => ExerciseSessionResultScreen(
              result: SessionResult(
                sessionId: 'sess-1',
                exerciseId: 'ex-1',
                exerciseTitle: 'Yoga dưỡng thai',
                status: 'COMPLETED',
                actualDurationSeconds: 300,
                completionPercent: 100.0,
                postureScore: 90.0,
                warningCount: 0,
                startedAt: DateTime.now().subtract(const Duration(minutes: 5)),
                endedAt: DateTime.now(),
              ),
            ),
          ),
          _ScreenItem(
            name: 'Lịch sử Tập luyện',
            description: 'Nhật ký các buổi tập đã thực hiện',
            code: 'CB-186',
            builder: (_) => const ExerciseHistoryScreen(),
          ),
        ],
      ),
      _ScreenCategory(
        name: 'Thông báo & Cài đặt',
        icon: Icons.notifications_none_outlined,
        screens: [
          _ScreenItem(
            name: 'Trung tâm Thông báo',
            description: 'Danh sách các thông báo hệ thống',
            code: 'CB-021',
            builder: (_) => const NotificationCenterScreen(),
          ),
          _ScreenItem(
            name: 'Thông báo của bạn',
            description: 'Thông báo cụ thể cho tài khoản',
            code: 'CB-158',
            builder: (_) => const NotificationsScreen(),
          ),
          _ScreenItem(
            name: 'Chi tiết Thông báo',
            description: 'Xem chi tiết một thông báo',
            code: 'CB-159',
            builder: (_) => NotificationDetailScreen(
              notification: NotificationRecord(
                id: 'n-1',
                userId: 'user-1',
                title: 'Nhắc nhở tiêm chủng',
                body: 'Bé có lịch tiêm chủng vắc xin 6 trong 1 vào ngày mai.',
                type: 'SYSTEM',
                status: 'SENT',
                createdAt: DateTime.now(),
              ),
            ),
          ),
          _ScreenItem(
            name: 'Tùy chọn Thông báo',
            description: 'Cấu hình bật tắt các loại thông báo',
            code: 'CB-102',
            builder: (_) => const NotificationPreferencesScreen(),
          ),
          _ScreenItem(
            name: 'Cài đặt Quyền riêng tư',
            description: 'Cấu hình quyền riêng tư dữ liệu',
            code: 'CB-105',
            builder: (_) => const PrivacySettingsScreen(),
          ),
        ],
      ),
    ];
  }
}

class _ScreenCategory {
  final String name;
  final IconData icon;
  final List<_ScreenItem> screens;

  _ScreenCategory({
    required this.name,
    required this.icon,
    required this.screens,
  });
}

class _ScreenItem {
  final String name;
  final String description;
  final String? code;
  final WidgetBuilder builder;

  _ScreenItem({
    required this.name,
    required this.description,
    this.code,
    required this.builder,
  });
}

class LogoutConfirmationScreenWrapper extends StatelessWidget {
  const LogoutConfirmationScreenWrapper({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF8F6),
      appBar: AppBar(
        title: const Text('Đăng xuất', style: TextStyle(fontFamily: 'Lexend')),
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: const Center(
        child: LogoutConfirmationSheet(),
      ),
    );
  }
}
