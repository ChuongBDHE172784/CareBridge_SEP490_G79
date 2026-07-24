import 'package:flutter/material.dart';

import '../../community/screens/community_feed_screen.dart';
import '../../community/screens/expert_question_queue_screen.dart';
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
  bool _loading = true;
  bool _onlineBusy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final snapshot = await ExpertHomeService.instance.loadSnapshot();
    if (!mounted) return;
    setState(() {
      _snapshot = snapshot;
      _loading = false;
    });
  }

  Future<void> _toggleOnline() async {
    final snapshot = _snapshot;
    if (snapshot == null || _onlineBusy) return;
    final nextOnline = !snapshot.online;
    setState(() {
      _onlineBusy = true;
      _snapshot = ExpertHomeSnapshot(
        profile: snapshot.profile,
        online: nextOnline,
        questionCount: snapshot.questionCount,
        supportRequests: snapshot.supportRequests,
      );
    });
    final ok = await ExpertHomeService.instance.setOnline(nextOnline);
    if (!mounted) return;
    setState(() => _onlineBusy = false);
    if (!ok) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Trạng thái đã đổi tạm thời. Backend UC-142 chưa sẵn sàng.',
            style: TextStyle(fontFamily: 'Lexend'),
          ),
        ),
      );
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
                _buildCommunityCard(),
                const SizedBox(height: 30),
                _buildSupportSection(snapshot),
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
        Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: _surfaceHigh,
            shape: BoxShape.circle,
            boxShadow: _softShadow,
          ),
          child: const Icon(Icons.badge_outlined, color: _primary, size: 31),
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
        const SizedBox(width: 8),
        Text(
          'Trực tuyến',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w700,
            color: snapshot?.online == true ? _primary : _outline,
          ),
        ),
        const SizedBox(width: 8),
        _OnlineToggle(
          online: snapshot?.online ?? false,
          busy: _onlineBusy,
          onTap: _toggleOnline,
        ),
      ],
    );
  }

  Widget _buildMetricGrid(ExpertHomeSnapshot? snapshot) {
    return _MetricCard(
      icon: Icons.forum_outlined,
      count: snapshot?.questionCount ?? 0,
      title: 'Hàng đợi Q&A',
      subtitle: 'Chờ trả lời',
      onTap: _openQuestions,
    );
  }

  Widget _buildSupportSection(ExpertHomeSnapshot? snapshot) {
    final items = snapshot?.supportRequests ?? const <ExpertSupportRequest>[];
    return Column(
      children: [
        Row(
          children: [
            const Expanded(
              child: Text(
                'Yêu cầu hỗ trợ gần đây',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 25,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
            ),
            TextButton(
              onPressed: () {},
              child: const Row(
                children: [
                  Text(
                    'Xem tất cả',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      color: _primary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Icon(Icons.chevron_right, color: _primary, size: 18),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (items.isEmpty)
          const _SupportCard(
            item: ExpertSupportRequest(
              title: 'Chưa có yêu cầu hỗ trợ',
              subtitle: 'Danh sách sẽ tự cập nhật khi có yêu cầu mới',
            ),
          )
        else
          ...items.map(
            (item) => Padding(
              padding: const EdgeInsets.only(bottom: 14),
              child: _SupportCard(item: item),
            ),
          ),
      ],
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

  void _openQuestions() {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const ExpertQuestionQueueScreen()),
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

class _OnlineToggle extends StatelessWidget {
  final bool online;
  final bool busy;
  final VoidCallback onTap;

  const _OnlineToggle({
    required this.online,
    required this.busy,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: busy ? null : onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        width: 64,
        height: 34,
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          color: online
              ? _ExpertAppHomeScreenState._primaryContainer
              : _ExpertAppHomeScreenState._surfaceHighest,
          borderRadius: BorderRadius.circular(24),
        ),
        child: AnimatedAlign(
          duration: const Duration(milliseconds: 180),
          alignment: online ? Alignment.centerRight : Alignment.centerLeft,
          child: Container(
            width: 26,
            height: 26,
            decoration: const BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
            ),
            child: busy
                ? const Padding(
                    padding: EdgeInsets.all(7),
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Icon(
                    online ? Icons.check_rounded : Icons.close_rounded,
                    size: 18,
                    color: online
                        ? _ExpertAppHomeScreenState._primaryContainer
                        : _ExpertAppHomeScreenState._outline,
                  ),
          ),
        ),
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

class _SupportCard extends StatelessWidget {
  final ExpertSupportRequest item;

  const _SupportCard({required this.item});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _ExpertAppHomeScreenState._surface,
        borderRadius: BorderRadius.circular(28),
        boxShadow: _ExpertAppHomeScreenState._softShadow,
      ),
      child: Row(
        children: [
          Container(
            width: 54,
            height: 54,
            decoration: BoxDecoration(
              color: item.urgent
                  ? _ExpertAppHomeScreenState._errorContainer
                  : _ExpertAppHomeScreenState._surfaceHighest,
              shape: BoxShape.circle,
            ),
            child: Icon(
              item.urgent ? Icons.priority_high : Icons.help_outline,
              color: item.urgent
                  ? _ExpertAppHomeScreenState._error
                  : _ExpertAppHomeScreenState._primary,
              size: 29,
            ),
          ),
          const SizedBox(width: 18),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _ExpertAppHomeScreenState._onSurface,
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  item.subtitle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _ExpertAppHomeScreenState._onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: _ExpertAppHomeScreenState._surfaceHighest,
              foregroundColor: _ExpertAppHomeScreenState._primary,
              shape: const StadiumBorder(),
              padding: const EdgeInsets.symmetric(horizontal: 18),
            ),
            onPressed: () {},
            child: const Text(
              'Phản hồi',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
