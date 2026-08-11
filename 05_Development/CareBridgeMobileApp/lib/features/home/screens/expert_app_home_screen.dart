import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/components/app_user_avatar.dart';
import '../../community/models/community_model.dart';
import '../../community/screens/community_feed_screen.dart';
import '../../community/screens/question_detail_screen.dart';
import '../../community/services/community_service.dart';
import '../../consultation/screens/expert_requests_tab_screen.dart';
import '../../expert/services/expert_home_service.dart';

class ExpertAppHomeScreen extends StatefulWidget {
  const ExpertAppHomeScreen({super.key});

  @override
  State<ExpertAppHomeScreen> createState() => _ExpertAppHomeScreenState();
}

class _ExpertAppHomeScreenState extends State<ExpertAppHomeScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outlineVariant = Color(0xFFE5D3CA);

  ExpertHomeSnapshot? _snapshot;
  List<CommunityFeedItem> _unansweredQuestions = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final snapshot = await ExpertHomeService.instance.loadSnapshot();
    _loadUnansweredQuestions();
    if (!mounted) return;
    setState(() {
      _snapshot = snapshot;
      _loading = false;
    });
  }

  Future<void> _loadUnansweredQuestions() async {
    try {
      final list = await CommunityService.instance.searchQuestions(
        hasExpertAnswer: false,
        size: 5,
      );
      if (mounted) {
        setState(() {
          _unansweredQuestions = list;
        });
      }
    } catch (_) {
      // Best-effort
    }
  }

  String _getTimeBasedGreeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Buổi sáng tốt lành,';
    if (hour < 18) return 'Buổi chiều tốt lành,';
    return 'Buổi tối tốt lành,';
  }

  @override
  Widget build(BuildContext context) {
    final snapshot = _snapshot;
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          color: _primary,
          onRefresh: _load,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
            children: [
              _buildHeader(snapshot),
              const SizedBox(height: 20),
              if (_loading)
                _buildSkeletonLoader()
              else ...[
                _buildMetricGrid(snapshot),
                const SizedBox(height: 18),
                if (snapshot?.nextConsultation != null) ...[
                  _buildNextConsultation(snapshot!.nextConsultation!),
                  const SizedBox(height: 18),
                ],
                _buildCommunityCard(),
                const SizedBox(height: 22),
                _buildUnansweredQuestionsSection(),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader(ExpertHomeSnapshot? snapshot) {
    final profile = snapshot?.profile ?? const ExpertHomeProfile();
    final greeting = _getTimeBasedGreeting();

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _outlineVariant, width: 1),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A845143),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: _primaryContainer, width: 2),
            ),
            child: AppUserAvatar(
              avatarUrl: profile.avatarUrl,
              radius: 28,
              backgroundColor: _surfaceContainerLow,
              onTap: () => context.push('/profile'),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  greeting,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                    color: _onSurfaceVariant.withValues(alpha: 0.8),
                  ),
                ),
                const SizedBox(height: 2),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        profile.displayName.isNotEmpty ? profile.displayName : 'Bác sĩ',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                          letterSpacing: -0.2,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 4),
                    const Icon(
                      Icons.verified_rounded,
                      size: 16,
                      color: Color(0xFF10B981),
                    ),
                  ],
                ),
                if (profile.subtitle.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    profile.subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: _primary,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricGrid(ExpertHomeSnapshot? snapshot) {
    return Row(
      children: [
        Expanded(
          child: _MetricCard(
            icon: Icons.assignment_rounded,
            count: snapshot?.requestCount ?? 0,
            title: 'Yêu cầu tư vấn',
            subtitle: 'Đang chờ xử lý',
            onTap: _openRequests,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _MetricCard(
            icon: Icons.question_answer_rounded,
            count: _unansweredQuestions.length,
            title: 'Câu hỏi mới',
            subtitle: 'Cần giải đáp',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const CommunityFeedScreen()),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildNextConsultation(ExpertConsultation consultation) {
    return Container(
      decoration: BoxDecoration(
        color: _surfaceContainerLow,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _primaryContainer.withValues(alpha: 0.4), width: 1),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A845143),
            blurRadius: 12,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          onTap: _openRequests,
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: const BoxDecoration(
                    color: _primary,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.notification_important_rounded,
                    color: Colors.white,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'ƯU TIÊN XỬ LÝ',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          color: _primary,
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.5,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        consultation.topic,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${consultation.motherName} · ${consultation.timeLabel}',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          color: _onSurfaceVariant,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right_rounded, color: _primary, size: 22),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCommunityCard() {
    return Container(
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _outlineVariant, width: 1),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A845143),
            blurRadius: 12,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => const CommunityFeedScreen()),
          ),
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: const BoxDecoration(
                    color: _surfaceContainerLow,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.groups_rounded,
                    color: _primary,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 14),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Cộng đồng Mẹ & Chuyên gia',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                        ),
                      ),
                      SizedBox(height: 2),
                      Text(
                        'Giải đáp thắc mắc sức khỏe cộng đồng',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right_rounded, color: _onSurfaceVariant, size: 22),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildUnansweredQuestionsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'Câu hỏi mới từ các Mẹ',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
            InkWell(
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const CommunityFeedScreen()),
              ),
              child: const Row(
                children: [
                  Text(
                    'Xem tất cả',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: _primary,
                    ),
                  ),
                  Icon(Icons.chevron_right_rounded, size: 18, color: _primary),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (_unansweredQuestions.isEmpty)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: _surface,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: _outlineVariant),
            ),
            child: const Center(
              child: Text(
                'Hiện chưa có câu hỏi mới cần giải đáp.',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant,
                ),
              ),
            ),
          )
        else
          Column(
            children:
                _unansweredQuestions.map((q) => _buildQuestionItem(q)).toList(),
          ),
      ],
    );
  }

  Widget _buildQuestionItem(CommunityFeedItem item) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _outlineVariant, width: 1),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A845143),
            blurRadius: 12,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          onTap: () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => QuestionDetailScreen(questionId: item.id),
              ),
            );
          },
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    if (item.topicName.isNotEmpty)
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 3,
                        ),
                        decoration: BoxDecoration(
                          color: _surfaceContainerLow,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          item.topicName,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: _primary,
                          ),
                        ),
                      ),
                    const Spacer(),
                    Text(
                      item.authorDisplay,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Text(
                  item.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                    height: 1.3,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    const Icon(
                      Icons.chat_bubble_outline_rounded,
                      size: 15,
                      color: _onSurfaceVariant,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      '${item.answerCount} câu trả lời',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                    const Spacer(),
                    const Text(
                      'Trả lời ngay',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: _primary,
                      ),
                    ),
                    const Icon(Icons.chevron_right_rounded, size: 16, color: _primary),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSkeletonLoader() {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: Container(
                height: 110,
                decoration: BoxDecoration(
                  color: _surface,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: _outlineVariant),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Container(
                height: 110,
                decoration: BoxDecoration(
                  color: _surface,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: _outlineVariant),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 18),
        Container(
          height: 80,
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: _outlineVariant),
          ),
        ),
      ],
    );
  }

  void _openRequests() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => const ExpertRequestsTabScreen(showBackButton: true),
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  final IconData icon;
  final int count;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _MetricCard({
    required this.icon,
    required this.count,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: _ExpertAppHomeScreenState._surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _ExpertAppHomeScreenState._outlineVariant, width: 1),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A845143),
            blurRadius: 12,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Container(
                      width: 44,
                      height: 44,
                      decoration: const BoxDecoration(
                        color: _ExpertAppHomeScreenState._surfaceContainerLow,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        icon,
                        color: _ExpertAppHomeScreenState._primary,
                        size: 22,
                      ),
                    ),
                    if (count > 0)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: _ExpertAppHomeScreenState._primary,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          count > 99 ? '99+' : '$count',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 14),
                Text(
                  title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: _ExpertAppHomeScreenState._onSurface,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _ExpertAppHomeScreenState._onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}


