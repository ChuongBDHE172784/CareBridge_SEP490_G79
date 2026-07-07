import 'dart:async';
import 'package:flutter/material.dart';
import '../models/community_model.dart';
import '../services/community_service.dart';
import 'community_feed_screen.dart';

/// CB-148 — Search Community Topics (UC-163)
/// Lets users search topics by keyword and browse them in a list.
/// "Theo dõi" (follow) is wired to the UC-171 follow/unfollow API (CommunityService.toggleFollowTopic).
/// Tapping a topic navigates to CommunityFeedScreen filtered by that topic.
class CommunityTopicSearchScreen extends StatefulWidget {
  const CommunityTopicSearchScreen({super.key});

  @override
  State<CommunityTopicSearchScreen> createState() =>
      _CommunityTopicSearchScreenState();
}

class _CommunityTopicSearchScreenState
    extends State<CommunityTopicSearchScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);

  // Specialty filter chips matching CB-148 design
  static const _specialties = [
    'Tất cả',
    'Nhi khoa',
    'Sản khoa',
    'Dinh dưỡng',
    'Tâm lý',
    'An toàn',
  ];

  final _searchCtrl = TextEditingController();
  Timer? _debounce;
  int _selectedSpecialty = 0;
  List<CommunityTopic> _topics = [];
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _loadTopics();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  Future<void> _loadTopics({String? keyword}) async {
    setState(() => _loading = true);
    try {
      final topics = await CommunityService.instance.getTopics(
        keyword: keyword,
      );
      if (mounted)
        setState(() {
          _topics = topics;
          _loading = false;
        });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _onSearchChanged(String v) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      _loadTopics(keyword: v.trim().isEmpty ? null : v.trim());
    });
  }

  List<CommunityTopic> get _filtered {
    if (_selectedSpecialty == 0) return _topics;
    final kw = _specialties[_selectedSpecialty].toLowerCase();
    return _topics
        .where(
          (t) =>
              t.name.toLowerCase().contains(kw) ||
              t.description.toLowerCase().contains(kw),
        )
        .toList();
  }

  // UC-171: follow state is hydrated from the server's `isFollowed` field on load
  // (see CommunityTopic), not tracked purely client-side anymore.
  Future<void> _toggleFollow(String topicId) async {
    final index = _topics.indexWhere((t) => t.id == topicId);
    if (index == -1) return;
    final wasFollowed = _topics[index].isFollowed;
    setState(
      () => _topics[index] = _topics[index].copyWith(isFollowed: !wasFollowed),
    );
    try {
      final followed = await CommunityService.instance.toggleFollowTopic(
        topicId,
      );
      if (mounted) {
        setState(
          () => _topics[index] = _topics[index].copyWith(isFollowed: followed),
        );
      }
    } catch (_) {
      // Rollback on error
      if (mounted) {
        setState(
          () =>
              _topics[index] = _topics[index].copyWith(isFollowed: wasFollowed),
        );
      }
    }
  }

  IconData _topicIcon(String iconName) {
    const map = <String, IconData>{
      'restaurant': Icons.restaurant,
      'bedtime': Icons.bedtime,
      'vaccines': Icons.vaccines,
      'psychology': Icons.psychology,
      'health_and_safety': Icons.health_and_safety,
      'child_care': Icons.child_care,
      'pregnant_woman': Icons.pregnant_woman,
      'forum': Icons.forum,
      'topic': Icons.topic,
    };
    return map[iconName] ?? Icons.topic;
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _filtered;

    return Scaffold(
      backgroundColor: _canvas,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            backgroundColor: _canvas.withValues(alpha: 0.95),
            pinned: true,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back, color: _primary),
              onPressed: () => Navigator.of(context).pop(),
            ),
            title: const Text(
              'Chủ đề Cộng đồng',
              style: TextStyle(
                color: _primary,
                fontWeight: FontWeight.bold,
                fontSize: 17,
              ),
            ),
            actions: [
              Padding(
                padding: const EdgeInsets.only(right: 12),
                child: CircleAvatar(
                  radius: 18,
                  backgroundColor: _secondaryContainer,
                  child: const Icon(Icons.person, size: 18, color: _primary),
                ),
              ),
            ],
            bottom: PreferredSize(
              preferredSize: const Size.fromHeight(108),
              child: Column(
                children: [
                  // Search bar
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
                    child: Container(
                      decoration: BoxDecoration(
                        color: _surface,
                        borderRadius: BorderRadius.circular(99),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.05),
                            blurRadius: 12,
                            offset: const Offset(0, 2),
                          ),
                        ],
                      ),
                      child: Row(
                        children: [
                          const Padding(
                            padding: EdgeInsets.symmetric(horizontal: 14),
                            child: Icon(
                              Icons.search,
                              color: _outline,
                              size: 20,
                            ),
                          ),
                          Expanded(
                            child: TextField(
                              controller: _searchCtrl,
                              onChanged: _onSearchChanged,
                              decoration: const InputDecoration(
                                hintText: 'Tìm kiếm chủ đề...',
                                hintStyle: TextStyle(
                                  color: _outline,
                                  fontSize: 14,
                                ),
                                border: InputBorder.none,
                                contentPadding: EdgeInsets.symmetric(
                                  vertical: 13,
                                ),
                              ),
                              style: const TextStyle(
                                fontSize: 14,
                                color: _onSurface,
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                        ],
                      ),
                    ),
                  ),
                  // Specialty filter chips
                  SizedBox(
                    height: 44,
                    child: ListView.separated(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      itemCount: _specialties.length,
                      separatorBuilder: (_, _) => const SizedBox(width: 8),
                      itemBuilder: (_, i) {
                        final selected = _selectedSpecialty == i;
                        return GestureDetector(
                          onTap: () => setState(() => _selectedSpecialty = i),
                          child: AnimatedContainer(
                            duration: const Duration(milliseconds: 200),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 8,
                            ),
                            decoration: BoxDecoration(
                              color: selected ? _primary : _surface,
                              borderRadius: BorderRadius.circular(99),
                              border: selected
                                  ? null
                                  : Border.all(
                                      color: _primaryContainer.withValues(
                                        alpha: 0.4,
                                      ),
                                    ),
                            ),
                            child: Text(
                              _specialties[i],
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: selected
                                    ? FontWeight.w600
                                    : FontWeight.normal,
                                color: selected ? Colors.white : _primary,
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                  const SizedBox(height: 8),
                ],
              ),
            ),
          ),

          // Loading
          if (_loading)
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.all(40),
                child: Center(
                  child: CircularProgressIndicator(color: _primary),
                ),
              ),
            )
          // Empty state
          else if (filtered.isEmpty)
            SliverFillRemaining(
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.topic_outlined,
                      color: _primaryContainer,
                      size: 48,
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Không tìm thấy chủ đề nào',
                      style: TextStyle(color: _onSurfaceVariant, fontSize: 14),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      'Thử tìm từ khóa khác',
                      style: TextStyle(color: _outline, fontSize: 12),
                    ),
                  ],
                ),
              ),
            )
          // Topic list
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 80),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate((context, i) {
                  final topic = filtered[i];
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _TopicListCard(
                      topic: topic,
                      icon: _topicIcon(topic.icon),
                      followed: topic.isFollowed,
                      onToggleFollow: () => _toggleFollow(topic.id),
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) =>
                              CommunityFeedScreen(initialTopicId: topic.id),
                        ),
                      ),
                    ),
                  );
                }, childCount: filtered.length),
              ),
            ),
        ],
      ),
      bottomNavigationBar: _BottomNavBar(),
    );
  }
}

class _TopicListCard extends StatelessWidget {
  final CommunityTopic topic;
  final IconData icon;
  final bool followed;
  final VoidCallback onToggleFollow;
  final VoidCallback onTap;

  const _TopicListCard({
    required this.topic,
    required this.icon,
    required this.followed,
    required this.onToggleFollow,
    required this.onTap,
  });

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(16),
          border: followed
              ? Border.all(color: _primaryContainer.withValues(alpha: 0.2))
              : null,
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.05),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: _secondaryContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: _primary, size: 24),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    topic.name,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '${topic.sortOrder * 100} câu hỏi • ${(topic.sortOrder * 3).clamp(1, 20)} chuyên gia',
                    style: const TextStyle(
                      fontSize: 12,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            GestureDetector(
              onTap: onToggleFollow,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: followed ? _primaryContainer : Colors.transparent,
                  borderRadius: BorderRadius.circular(99),
                  border: followed
                      ? null
                      : Border.all(color: _primaryContainer, width: 1.5),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (followed) ...[
                      const Icon(Icons.check, size: 14, color: Colors.white),
                      const SizedBox(width: 4),
                      const Text(
                        'Đã theo dõi',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ] else
                      Text(
                        'Theo dõi',
                        style: TextStyle(
                          fontSize: 12,
                          color: _primaryContainer,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BottomNavBar extends StatelessWidget {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 64,
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.08),
            blurRadius: 16,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _NavItem(
            icon: Icons.home_outlined,
            label: 'Trang chủ',
            active: false,
            onTap: () => Navigator.of(context).popUntil((r) => r.isFirst),
          ),
          _NavItem(
            icon: Icons.assignment_outlined,
            label: 'Yêu cầu',
            active: false,
            onTap: () {},
          ),
          _NavItem(
            icon: Icons.forum,
            label: 'Cộng đồng',
            active: true,
            onTap: () => Navigator.of(context).pop(),
          ),
          _NavItem(
            icon: Icons.person_outlined,
            label: 'Tài khoản',
            active: false,
            onTap: () {},
          ),
        ],
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool active;
  final VoidCallback onTap;

  const _NavItem({
    required this.icon,
    required this.label,
    required this.active,
    required this.onTap,
  });

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: active
            ? const EdgeInsets.symmetric(horizontal: 16, vertical: 8)
            : const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: active ? _primaryContainer : Colors.transparent,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 22,
              color: active ? Colors.white : _onSurfaceVariant,
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: TextStyle(
                fontSize: 10,
                color: active ? Colors.white : _onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
