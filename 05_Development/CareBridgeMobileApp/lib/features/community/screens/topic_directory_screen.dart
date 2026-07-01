import 'dart:async';
import 'package:flutter/material.dart';
import '../models/community_model.dart';
import '../services/community_service.dart';
import 'community_topic_search_screen.dart';
import 'community_feed_screen.dart';

/// CB-118 — Topic Directory (UC-163)
/// Displays a library of community topics with featured banner,
/// 2-column bento grid, and a community suggestion row.
/// Supports keyword search (navigates to CommunityTopicSearchScreen).
class TopicDirectoryScreen extends StatefulWidget {
  const TopicDirectoryScreen({super.key});

  @override
  State<TopicDirectoryScreen> createState() => _TopicDirectoryScreenState();
}

class _TopicDirectoryScreenState extends State<TopicDirectoryScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);

  // Stage filter chips
  static const _stages = ['Tất cả', 'Mang thai', 'Sau sinh', 'Chăm bé'];

  final _service = CommunityService.instance;
  List<CommunityTopic> _topics = [];
  bool _loading = true;
  int _selectedStage = 0;
  // UC-171: follow state mirrors backend; updated optimistically then reconciled with API response
  final Set<String> _followedTopics = {};


  @override
  void initState() {
    super.initState();
    _loadTopics();
  }

  Future<void> _loadTopics() async {
    setState(() => _loading = true);
    try {
      final topics = await _service.getTopics();
      if (mounted) setState(() { _topics = topics; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  List<CommunityTopic> get _filteredTopics {
    if (_selectedStage == 0) return _topics;
    // Client-side stage filter based on topic name keywords
    final stageKw = [null, 'thai', 'sinh', 'bé'][_selectedStage];
    if (stageKw == null) return _topics;
    return _topics.where((t) => t.name.toLowerCase().contains(stageKw)).toList();
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

  Future<void> _toggleFollow(String topicId) async {
    final wasFollowed = _followedTopics.contains(topicId);
    // Optimistic update
    setState(() {
      if (wasFollowed) {
        _followedTopics.remove(topicId);
      } else {
        _followedTopics.add(topicId);
      }
    });
    try {
      final followed = await _service.toggleFollowTopic(topicId);
      if (mounted) {
        setState(() {
          if (followed) {
            _followedTopics.add(topicId);
          } else {
            _followedTopics.remove(topicId);
          }
        });
      }
    } catch (_) {
      // Rollback on error
      if (mounted) {
        setState(() {
          if (wasFollowed) {
            _followedTopics.add(topicId);
          } else {
            _followedTopics.remove(topicId);
          }
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: CustomScrollView(
        slivers: [
          // App bar with search
          SliverAppBar(
            backgroundColor: _canvas.withValues(alpha: 0.95),
            pinned: true,
            elevation: 0,
            shadowColor: Colors.black12,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back, color: _primary),
              onPressed: () => Navigator.of(context).pop(),
            ),
            title: const Text(
              'Thư viện chủ đề',
              style: TextStyle(color: _primary, fontWeight: FontWeight.bold, fontSize: 18),
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.notifications_none, color: _onSurfaceVariant),
                onPressed: () {},
              ),
            ],
            bottom: PreferredSize(
              preferredSize: const Size.fromHeight(100),
              child: Column(
                children: [
                  // Search bar — taps open CommunityTopicSearchScreen
                  GestureDetector(
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(
                      builder: (_) => const CommunityTopicSearchScreen(),
                    )),
                    child: Container(
                      margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      decoration: BoxDecoration(
                        color: _surface,
                        borderRadius: BorderRadius.circular(99),
                        border: Border.all(color: _outlineVariant),
                      ),
                      child: Row(
                        children: const [
                          Icon(Icons.search, color: _outline, size: 20),
                          SizedBox(width: 8),
                          Text(
                            'Tìm kiếm chủ đề bạn quan tâm...',
                            style: TextStyle(color: _outline, fontSize: 14),
                          ),
                        ],
                      ),
                    ),
                  ),
                  // Stage filter chips
                  SizedBox(
                    height: 44,
                    child: ListView.separated(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      itemCount: _stages.length,
                      separatorBuilder: (_, __) => const SizedBox(width: 8),
                      itemBuilder: (_, i) {
                        final selected = _selectedStage == i;
                        return GestureDetector(
                          onTap: () => setState(() => _selectedStage = i),
                          child: AnimatedContainer(
                            duration: const Duration(milliseconds: 200),
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                            decoration: BoxDecoration(
                              color: selected ? _primary : _surface,
                              borderRadius: BorderRadius.circular(99),
                              border: selected ? null : Border.all(color: _outlineVariant),
                            ),
                            child: Text(
                              _stages[i],
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
                                color: selected ? Colors.white : _onSurfaceVariant,
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

          if (_loading)
            const SliverFillRemaining(
              child: Center(child: CircularProgressIndicator(color: _primary)),
            )
          else ...[
            // Topic grid header
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 20, 16, 10),
                child: const Text(
                  'Khám phá thư viện',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: _onSurface),
                ),
              ),
            ),

            // 2-column bento grid
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
              sliver: _filteredTopics.isEmpty
                  ? SliverToBoxAdapter(
                      child: Center(
                        child: Padding(
                          padding: const EdgeInsets.all(32),
                          child: Text('Không có chủ đề nào', style: TextStyle(color: _outline)),
                        ),
                      ),
                    )
                  : SliverGrid(
                      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 2,
                        mainAxisSpacing: 12,
                        crossAxisSpacing: 12,
                        childAspectRatio: 0.82,
                      ),
                      delegate: SliverChildBuilderDelegate(
                        (context, i) {
                          final topic = _filteredTopics[i];
                          final followed = _followedTopics.contains(topic.id);
                          return _TopicGridCard(
                            topic: topic,
                            icon: _topicIcon(topic.icon),
                            followed: followed,
                            onToggleFollow: () => _toggleFollow(topic.id),
                            onTap: () => Navigator.of(context).push(MaterialPageRoute(
                              builder: (_) => CommunityFeedScreen(initialTopicId: topic.id),
                            )),
                          );
                        },
                        childCount: _filteredTopics.length,
                      ),
                    ),
            ),

          ],
        ],
      ),
    );
  }
}

class _TopicGridCard extends StatelessWidget {
  final CommunityTopic topic;
  final IconData icon;
  final bool followed;
  final VoidCallback onToggleFollow;
  final VoidCallback onTap;

  const _TopicGridCard({
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
  static const _outline = Color(0xFF84736F);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(24),
          boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.06), blurRadius: 16, offset: const Offset(0, 4))],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(color: _secondaryContainer, borderRadius: BorderRadius.circular(12)),
              child: Icon(icon, color: _primary, size: 22),
            ),
            const SizedBox(height: 12),
            Text(
              topic.name,
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: _onSurface, height: 1.2),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 4),
            Text(
              topic.description.isEmpty ? 'Chủ đề cộng đồng' : topic.description,
              style: const TextStyle(fontSize: 11, color: _onSurfaceVariant, height: 1.3),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            const Spacer(),
            Row(
              children: [
                Expanded(
                  child: Text(
                    '${topic.sortOrder * 100} câu hỏi',
                    style: const TextStyle(fontSize: 11, color: _outline),
                  ),
                ),
                GestureDetector(
                  onTap: onToggleFollow,
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    width: 32,
                    height: 32,
                    decoration: BoxDecoration(
                      color: followed ? _primary : _secondaryContainer,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      followed ? Icons.check : Icons.add,
                      size: 16,
                      color: followed ? Colors.white : _primary,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

