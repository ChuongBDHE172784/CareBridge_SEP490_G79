import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/components/app_user_avatar.dart';
import '../../community/screens/community_feed_screen.dart';
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
  bool _loading = true;
  bool _onlineBusy = false;
  int _onlineMutationGeneration = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final onlineGeneration = _onlineMutationGeneration;
    setState(() => _loading = true);
    final snapshot = await ExpertHomeService.instance.loadSnapshot();
    if (!mounted) return;
    setState(() {
      final current = _snapshot;
      _snapshot =
          current != null && onlineGeneration != _onlineMutationGeneration
          ? snapshot.copyWith(online: current.online)
          : snapshot;
      _loading = false;
    });
  }

  Future<void> _toggleOnline() async {
    final snapshot = _snapshot;
    if (snapshot == null || _onlineBusy) return;
    final nextOnline = !snapshot.online;
    _onlineMutationGeneration++;
    setState(() => _onlineBusy = true);
    final result = await ExpertHomeService.instance.setOnline(nextOnline);
    if (!mounted) return;
    setState(() {
      _onlineBusy = false;
      if (result.success) {
        _snapshot = (_snapshot ?? snapshot).copyWith(
          online: result.online ?? nextOnline,
        );
      }
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          result.message,
          style: const TextStyle(fontFamily: 'Lexend'),
        ),
        duration: Duration(seconds: result.success ? 2 : 4),
      ),
    );
    if (!result.success) {
      return;
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
      key: const Key('expert-online-toggle'),
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

