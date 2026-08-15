import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_state.dart';
import '../../directChat/services/direct_chat_service.dart';
import '../../consultation/screens/consultation_request_form_screen.dart';
import '../models/expert_availability_slot.dart';
import '../services/expert_availability_service.dart';

class ExpertPublicProfileScreen extends StatefulWidget {
  final String expertProfileId;
  final ExpertAvailabilityService? availabilityService;

  const ExpertPublicProfileScreen({
    super.key,
    required this.expertProfileId,
    this.availabilityService,
  });

  @override
  State<ExpertPublicProfileScreen> createState() =>
      _ExpertPublicProfileScreenState();
}

class _StaleExpertProfileResponse implements Exception {
  const _StaleExpertProfileResponse();
}

class _AvailabilityLoadResult {
  final List<ExpertAvailabilitySlot> slots;
  final bool failed;

  const _AvailabilityLoadResult(this.slots, {this.failed = false});
}

class _ExpertPublicProfileScreenState extends State<ExpertPublicProfileScreen> {
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  late Future<Map<String, dynamic>> _future;
  late Future<_AvailabilityLoadResult> _availabilityFuture;
  int _generation = 0;
  late String? _accountId;
  bool _accountChanged = false;

  @override
  void initState() {
    super.initState();
    _accountId = AuthState.instance.userId;
    AuthState.instance.addListener(_handleAuthChanged);
    _future = _loadProfile();
    _availabilityFuture = _loadAvailability();
  }

  @override
  void didUpdateWidget(covariant ExpertPublicProfileScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    final profileChanged =
        oldWidget.expertProfileId != widget.expertProfileId;
    final availabilityServiceChanged =
        !identical(oldWidget.availabilityService, widget.availabilityService);
    if (profileChanged) {
      _accountChanged = false;
      _future = _loadProfile();
    }
    if (profileChanged || availabilityServiceChanged) {
      _availabilityFuture = _loadAvailability();
    }
  }

  @override
  void dispose() {
    AuthState.instance.removeListener(_handleAuthChanged);
    super.dispose();
  }

  void _handleAuthChanged() {
    final current = AuthState.instance.userId;
    if (current == _accountId) return;
    _accountId = current;
    _generation++;
    if (!mounted) return;
    setState(() {
      _accountChanged = true;
    });
  }

  Future<_AvailabilityLoadResult> _loadAvailability() async {
    try {
      final slots =
          await (widget.availabilityService ??
                  ExpertAvailabilityService.instance)
              .getPublicAvailability(widget.expertProfileId);
      return _AvailabilityLoadResult(slots);
    } catch (_) {
      return const _AvailabilityLoadResult([], failed: true);
    }
  }

  Future<Map<String, dynamic>> _loadProfile() async {
    final generation = ++_generation;
    final requestAccountId = AuthState.instance.userId;
    final profile = await DirectChatService.instance.getExpertProfile(
      widget.expertProfileId,
    );
    if (!mounted ||
        generation != _generation ||
        AuthState.instance.userId != requestAccountId) {
      throw const _StaleExpertProfileResponse();
    }
    final expertUserId = (profile['userId'] ?? profile['expertUserId'])
        ?.toString();
    if (expertUserId != null && expertUserId.isNotEmpty) {
      try {
        final conversations = await DirectChatService.instance
            .listMyConversations();
        final existing = conversations.where(
          (item) => item.counterpartUserId == expertUserId,
        );
        if (existing.isNotEmpty) {
          profile['_conversationId'] = existing.first.conversationId;
        }
      } catch (_) {
        // Consultation stays available if the optional conversation lookup fails.
      }
    }
    return profile;
  }

  Future<void> _openChat(String? conversationId) async {
    if (conversationId != null && conversationId.isNotEmpty) {
      if (mounted) context.push('/direct-chat/$conversationId');
      return;
    }
    try {
      final conversation = await DirectChatService.instance
          .findOrCreateConversation(widget.expertProfileId);
      if (mounted) context.push('/direct-chat/${conversation.conversationId}');
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể mở cuộc trò chuyện.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _surface,
        elevation: 0.5,
        leading: IconButton(
          tooltip: 'Quay lại',
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: const Text(
          'Hồ sơ chuyên gia',
          style: TextStyle(
            color: _primary,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
        centerTitle: true,
      ),
      body: _accountChanged
          ? const Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Text(
                  'Phiên đăng nhập đã thay đổi. Hồ sơ của tài khoản trước đã được xóa khỏi màn hình.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: _onSurface,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            )
          : FutureBuilder<Map<String, dynamic>>(
              future: _future,
              builder: (context, snapshot) {
                if (snapshot.connectionState != ConnectionState.done) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snapshot.hasError) {
                  return Center(
                    child: Text('Lỗi tải hồ sơ: ${snapshot.error}'),
                  );
                }
                final profile = snapshot.data!;
                final isConsultationEligible =
                    (profile['consultationEligible'] ??
                        profile['isConsultationEligible']) ==
                    true;

                final displayName = profile['displayName'] as String?;
                final professionalTitle =
                    profile['professionalTitle'] as String?;
                final expertDisplayName =
                    displayName ?? professionalTitle ?? 'Chuyên gia';
                final avatarUrl = profile['avatarUrl'] as String?;
                final specialty = profile['specialty'] as String?;
                final workplace = profile['workplace'] as String?;
                final experienceYears = profile['experienceYears']?.toString();
                final ratingAvg = profile['ratingAvg']?.toString();
                final email = (profile['email'] as String?)?.trim();
                final phone =
                    ((profile['phoneNumber'] ?? profile['phone']) as String?)
                        ?.trim();
                final consultationScope =
                    profile['consultationScope'] as String?;
                final isVerified = profile['verificationStatus'] == 'APPROVED';
                final conversationId = profile['_conversationId'] as String?;

                return SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    children: [
                      // Header Card
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: _surface,
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: Column(
                          children: [
                            CircleAvatar(
                              radius: 48,
                              backgroundImage: avatarUrl != null
                                  ? NetworkImage(avatarUrl)
                                  : null,
                              child: avatarUrl == null
                                  ? Text(
                                      expertDisplayName.isNotEmpty
                                          ? expertDisplayName[0].toUpperCase()
                                          : '?',
                                      style: const TextStyle(fontSize: 32),
                                    )
                                  : null,
                            ),
                            const SizedBox(height: 16),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Flexible(
                                  child: Text(
                                    expertDisplayName,
                                    textAlign: TextAlign.center,
                                    style: const TextStyle(
                                      color: _onSurface,
                                      fontSize: 22,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                ),
                                if (isVerified) ...[
                                  const SizedBox(width: 8),
                                  const Icon(
                                    Icons.verified,
                                    color: Color(0xFFC98C7B),
                                    size: 24,
                                  ),
                                ],
                              ],
                            ),
                            if (professionalTitle != null &&
                                professionalTitle != displayName) ...[
                              const SizedBox(height: 4),
                              Text(
                                professionalTitle,
                                style: const TextStyle(
                                  color: _primary,
                                  fontSize: 15,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                            if ((email != null && email.isNotEmpty) ||
                                (phone != null && phone.isNotEmpty)) ...[
                              const SizedBox(height: 8),
                              Wrap(
                                alignment: WrapAlignment.center,
                                crossAxisAlignment: WrapCrossAlignment.center,
                                spacing: 16,
                                runSpacing: 6,
                                children: [
                                  if (email != null && email.isNotEmpty)
                                    Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        const Icon(
                                          Icons.email_outlined,
                                          size: 15,
                                          color: _onSurfaceVariant,
                                        ),
                                        const SizedBox(width: 4),
                                        Text(
                                          email,
                                          style: const TextStyle(
                                            color: _onSurfaceVariant,
                                            fontSize: 13,
                                            fontWeight: FontWeight.w500,
                                          ),
                                        ),
                                      ],
                                    ),
                                  if (phone != null && phone.isNotEmpty)
                                    Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        const Icon(
                                          Icons.phone_outlined,
                                          size: 15,
                                          color: _onSurfaceVariant,
                                        ),
                                        const SizedBox(width: 4),
                                        Text(
                                          phone,
                                          style: const TextStyle(
                                            color: _onSurfaceVariant,
                                            fontSize: 13,
                                            fontWeight: FontWeight.w500,
                                          ),
                                        ),
                                      ],
                                    ),
                                ],
                              ),
                            ],
                            const SizedBox(height: 16),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                              children: [
                                _buildStatColumn(
                                  icon: Icons.star_rounded,
                                  value: ratingAvg ?? 'Chưa có',
                                  label: 'Đánh giá',
                                  iconColor: Colors.amber,
                                ),
                                _buildStatColumn(
                                  icon: Icons.work_history_rounded,
                                  value: experienceYears != null
                                      ? '$experienceYears năm'
                                      : 'Chưa có',
                                  label: 'Kinh nghiệm',
                                  iconColor: _primary,
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      _buildActionButtons(
                        context: context,
                        isConsultationEligible: isConsultationEligible,
                        conversationId: conversationId,
                        expertDisplayName: expertDisplayName,
                      ),
                      const SizedBox(height: 16),

                      _buildAvailabilityCard(),
                      const SizedBox(height: 16),

                      // Details Card
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: _surface,
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Thông tin công tác',
                              style: TextStyle(
                                color: _onSurface,
                                fontSize: 18,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const SizedBox(height: 16),
                            _buildInfoRow(
                              Icons.medical_services_outlined,
                              'Chuyên khoa',
                              specialty ?? 'Đang cập nhật',
                            ),
                            const SizedBox(height: 12),
                            _buildInfoRow(
                              Icons.local_hospital_outlined,
                              'Nơi công tác',
                              workplace ?? 'Đang cập nhật',
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // Consultation Scope
                      if (consultationScope != null &&
                          consultationScope.isNotEmpty)
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(24),
                          decoration: BoxDecoration(
                            color: _surface,
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Phạm vi tư vấn',
                                style: TextStyle(
                                  color: _onSurface,
                                  fontSize: 18,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              const SizedBox(height: 12),
                              Text(
                                consultationScope,
                                style: const TextStyle(
                                  color: _onSurfaceVariant,
                                  fontSize: 14,
                                  height: 1.5,
                                ),
                              ),
                            ],
                          ),
                        ),

                      const SizedBox(height: 32),
                      if (!isConsultationEligible) ...[
                        const SizedBox(height: 12),
                        const Text(
                          'Chuyên gia hiện chưa thể nhận tương tác tư vấn mới.',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: Colors.redAccent,
                            fontSize: 13,
                          ),
                        ),
                      ],
                      const SizedBox(height: 24),
                    ],
                  ),
                );
              },
            ),
    );
  }

  Widget _buildActionButtons({
    required BuildContext context,
    required bool isConsultationEligible,
    required String? conversationId,
    required String expertDisplayName,
  }) {
    final hasConversation = conversationId != null && conversationId.isNotEmpty;

    if (hasConversation) {
      return SizedBox(
        width: double.infinity,
        height: 52,
        child: FilledButton(
          onPressed: () => _openChat(conversationId),
          style: FilledButton.styleFrom(
            backgroundColor: _primary,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
          child: const Text(
            'Trò chuyện',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          ),
        ),
      );
    }

    return SizedBox(
      width: double.infinity,
      height: 52,
      child: OutlinedButton(
        onPressed: isConsultationEligible
            ? () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => ConsultationRequestFormScreen(
                    expertProfileId: widget.expertProfileId,
                    expertDisplayName: expertDisplayName,
                  ),
                ),
              )
            : null,
        style: OutlinedButton.styleFrom(
          foregroundColor: _primary,
          side: const BorderSide(color: _primary),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
        child: const Text(
          'Yêu cầu tư vấn',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),
    );
  }

  Widget _buildAvailabilityCard() {
    return Container(
      key: const Key('expert-public-availability'),
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 24,
            offset: Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              CircleAvatar(
                radius: 20,
                backgroundColor: Color(0xFFF8EEE9),
                child: Icon(Icons.calendar_month_rounded, color: _primary),
              ),
              SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Lịch tư vấn sắp tới',
                      style: TextStyle(
                        color: _onSurface,
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      'Chọn ca phù hợp khi gửi yêu cầu tư vấn',
                      style: TextStyle(color: _onSurfaceVariant, fontSize: 13),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          FutureBuilder<_AvailabilityLoadResult>(
            future: _availabilityFuture,
            builder: (context, snapshot) {
              if (snapshot.connectionState != ConnectionState.done) {
                return const Center(
                  child: Padding(
                    padding: EdgeInsets.all(12),
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                );
              }
              if (snapshot.data?.failed ?? false) {
                return _availabilityMessage(
                  Icons.cloud_off_rounded,
                  'Chưa thể tải lịch. Bạn vẫn có thể gửi yêu cầu và chọn thời gian sau.',
                );
              }
              final grouped = groupAvailabilityByLocalDate(
                snapshot.data?.slots ?? const [],
              );
              if (grouped.isEmpty) {
                return _availabilityMessage(
                  Icons.event_busy_rounded,
                  'Chuyên gia chưa công bố ca tư vấn sắp tới.',
                );
              }
              final entries = grouped.entries.take(7).toList();
              return Column(
                children: entries.map((entry) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SizedBox(
                          width: 82,
                          child: Text(
                            _formatAvailabilityDate(entry.key),
                            style: const TextStyle(
                              color: _onSurface,
                              fontWeight: FontWeight.w700,
                              fontSize: 13,
                            ),
                          ),
                        ),
                        Expanded(
                          child: Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: entry.value.map((slot) {
                              return Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 8,
                                ),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFF8EEE9),
                                  borderRadius: BorderRadius.circular(99),
                                ),
                                child: Text(
                                  _formatSlotTime(slot),
                                  style: const TextStyle(
                                    color: _primary,
                                    fontWeight: FontWeight.w700,
                                    fontSize: 12,
                                  ),
                                ),
                              );
                            }).toList(),
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _availabilityMessage(IconData icon, String message) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF8EEE9),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(icon, color: _primary),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(color: _onSurfaceVariant, height: 1.35),
            ),
          ),
        ],
      ),
    );
  }

  String _formatAvailabilityDate(DateTime date) {
    final today = DateTime.now();
    final tomorrow = today.add(const Duration(days: 1));
    if (_sameLocalDate(date, today)) return 'Hôm nay';
    if (_sameLocalDate(date, tomorrow)) return 'Ngày mai';
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}';
  }

  String _formatSlotTime(ExpertAvailabilitySlot slot) =>
      '${slot.startAt.hour.toString().padLeft(2, '0')}:00–'
      '${slot.endAt.hour.toString().padLeft(2, '0')}:00';

  bool _sameLocalDate(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  Widget _buildStatColumn({
    required IconData icon,
    required String value,
    required String label,
    required Color iconColor,
  }) {
    return Column(
      children: [
        Icon(icon, color: iconColor, size: 28),
        const SizedBox(height: 8),
        Text(
          value,
          style: const TextStyle(
            color: _onSurface,
            fontSize: 16,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: const TextStyle(color: _onSurfaceVariant, fontSize: 12),
        ),
      ],
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: _primary, size: 20),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: const TextStyle(color: _onSurfaceVariant, fontSize: 12),
              ),
              const SizedBox(height: 2),
              Text(
                value,
                style: const TextStyle(
                  color: _onSurface,
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  height: 1.4,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
