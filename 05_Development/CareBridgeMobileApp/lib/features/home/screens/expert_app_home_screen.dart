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
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _surfaceHigh = Color(0xFFFFE2D9);
  static const _surfaceHighest = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

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

  @override
  Widget build(BuildContext context) {
    final snapshot = _snapshot;
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          color: _primaryContainer,
          onRefresh: _load,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
            children: [
              _buildHeader(snapshot),
              const SizedBox(height: 28),
              if (_loading)
                const Padding(
                  padding: EdgeInsets.only(top: 110),
                  child: Center(
                    child: CircularProgressIndicator(color: _primary),
                  ),
                )
              else ...[
                _buildMetricGrid(snapshot),
                const SizedBox(height: 18),
                if (snapshot?.nextConsultation != null) ...[
                  _buildNextConsultation(snapshot!.nextConsultation!),
                  const SizedBox(height: 18),
                ],
                _buildCommunityCard(),
                const SizedBox(height: 24),
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
    return Row(
      children: [
        AppUserAvatar(
          avatarUrl: profile.avatarUrl,
          radius: 28,
          backgroundColor: _surfaceHigh,
          onTap: () => context.push('/profile'),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                profile.displayName,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 31,
                  fontWeight: FontWeight.w700,
                  color: _primary,
                  height: 1.05,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                profile.subtitle,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: _outline,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMetricGrid(ExpertHomeSnapshot? snapshot) {
    return Row(
      children: [
        Expanded(
          child: _MetricCard(
            icon: Icons.assignment_outlined,
            count: snapshot?.requestCount ?? 0,
            title: 'Yêu cầu',
            subtitle: 'Đang chờ',
            onTap: _openRequests,
          ),
        ),
      ],
    );
  }

  Widget _buildNextConsultation(ExpertConsultation consultation) {
    return InkWell(
      onTap: _openRequests,
      borderRadius: BorderRadius.circular(24),
      child: Container(
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          color: _surfaceHigh,
          borderRadius: BorderRadius.circular(24),
        ),
        child: Row(
          children: [
            const CircleAvatar(
              backgroundColor: _primary,
              child: Icon(Icons.schedule_outlined, color: Colors.white),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Yêu cầu cần xem trước',
                    style: TextStyle(color: _onSurfaceVariant, fontSize: 12),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    consultation.topic,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w800,
                      color: _onSurface,
                    ),
                  ),
                  Text(
                    '${consultation.motherName} · ${consultation.timeLabel}',
                    style: const TextStyle(
                      color: _onSurfaceVariant,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: _primary),
          ],
        ),
      ),
    );
  }

  // ADR-MEDI-005 — Community bumped off the EXPERT bottom nav (no "find/message Mother" CTA
  // anywhere in the EXPERT shell, BR-MEDI-005); this card is its replacement entry point.
  Widget _buildCommunityCard() {
    return InkWell(
      onTap: () => Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (_) => const CommunityFeedScreen())),
      borderRadius: BorderRadius.circular(28),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(28),
          boxShadow: _softShadow,
        ),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: const BoxDecoration(
                color: _surfaceHighest,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.group_outlined,
                color: _primary,
                size: 24,
              ),
            ),
            const SizedBox(width: 16),
            const Expanded(
              child: Text(
                'Cộng đồng',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
            ),
            const Icon(Icons.chevron_right, color: _outline),
          ],
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
              'Câu hỏi mới cần giải đáp',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
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
                  Icon(Icons.chevron_right, size: 18, color: _primary),
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
              boxShadow: _softShadow,
            ),
            child: const Center(
              child: Text(
                'Hiện chưa có câu hỏi mới cần giải đáp.',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _outline,
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
        boxShadow: _softShadow,
      ),
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
                        horizontal: 10,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: _surfaceHigh,
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
                      color: _outline,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                item.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                  height: 1.3,
                ),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  const Icon(
                    Icons.chat_bubble_outline,
                    size: 16,
                    color: _outline,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    '${item.answerCount} câu trả lời',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _outline,
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
                  const Icon(Icons.chevron_right, size: 16, color: _primary),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openRequests() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => const ExpertRequestsTabScreen(showBackButton: true),
      ),
    );
  }

  static List<BoxShadow> get _softShadow => [
    BoxShadow(
      color: const Color(0xFF5A463F).withValues(alpha: 0.06),
      blurRadius: 22,
      offset: const Offset(0, 4),
    ),
  ];
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
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(28),
      child: Container(
        padding: const EdgeInsets.fromLTRB(18, 24, 18, 22),
        decoration: BoxDecoration(
          color: _ExpertAppHomeScreenState._surface,
          borderRadius: BorderRadius.circular(28),
          boxShadow: _ExpertAppHomeScreenState._softShadow,
        ),
        child: Column(
          children: [
            Stack(
              clipBehavior: Clip.none,
              children: [
                Container(
                  width: 58,
                  height: 58,
                  decoration: const BoxDecoration(
                    color: _ExpertAppHomeScreenState._surfaceHighest,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    icon,
                    color: _ExpertAppHomeScreenState._primary,
                    size: 30,
                  ),
                ),
                if (count > 0)
                  Positioned(
                    top: -5,
                    right: -5,
                    child: Container(
                      width: 24,
                      height: 24,
                      decoration: const BoxDecoration(
                        color: _ExpertAppHomeScreenState._error,
                        shape: BoxShape.circle,
                      ),
                      child: Center(
                        child: Text(
                          count > 99 ? '99' : '$count',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 18),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: _ExpertAppHomeScreenState._onSurface,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w500,
                color: _ExpertAppHomeScreenState._onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

