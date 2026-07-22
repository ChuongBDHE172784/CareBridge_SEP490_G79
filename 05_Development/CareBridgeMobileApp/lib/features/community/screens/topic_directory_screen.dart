import 'dart:async';
import 'package:flutter/material.dart';
import '../models/community_model.dart';
import '../services/community_service.dart';
import 'community_feed_screen.dart';

/// CB-118 — Topic Directory (UC-163)
/// Displays a library of community topics with featured banner,
/// 2-column bento grid, and a community suggestion row.
/// Supports inline keyword search and CATEGORY-based filtering.
class TopicDirectoryScreen extends StatefulWidget {
  const TopicDirectoryScreen({super.key});

  @override
  State<TopicDirectoryScreen> createState() => _TopicDirectoryScreenState();
}

class _TopicDirectoryScreenState extends State<TopicDirectoryScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);

  final _service = CommunityService.instance;
  final _searchController = TextEditingController();
  List<CommunityTopic> _topics = [];
  List<CommunityTopic> _categories = [];
  bool _loading = true;
  String? _selectedCategoryId;
  Timer? _searchDebounce;
  int _searchGeneration = 0;

  @override
  void initState() {
    super.initState();
    _loadDirectory();
  }

  @override
  void dispose() {
    _searchDebounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadDirectory() async {
    final searchGeneration = _searchGeneration;
    setState(() => _loading = true);
    try {
      final results = await Future.wait([
        _service.getTopics(type: 'TOPIC'),
        _service.getTopicCategories(),
      ]);
      if (mounted) {
        setState(() {
          if (searchGeneration == _searchGeneration) {
            _topics = results[0];
          }
          _categories = results[1];
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _onSearchChanged(String value) {
    _searchDebounce?.cancel();
    _searchDebounce = Timer(
      const Duration(milliseconds: 350),
      () => _searchTopics(value),
    );
  }

  Future<void> _searchTopics(String keyword) async {
    final generation = ++_searchGeneration;
    try {
      final topics = await _service.getTopics(
        keyword: keyword.trim(),
        type: 'TOPIC',
      );
      if (mounted && generation == _searchGeneration) {
        setState(() => _topics = topics);
      }
    } catch (_) {
      // Keep the last successful result visible when an inline search fails.
    }
  }

  List<CommunityTopic> get _filteredTopics => filterDirectoryTopics(
    _topics,
    selectedCategoryId: _selectedCategoryId,
    keyword: _searchController.text,
  );

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
      final followed = await _service.toggleFollowTopic(topicId);
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
              style: TextStyle(
                color: _primary,
                fontWeight: FontWeight.bold,
                fontSize: 18,
              ),
            ),
            actions: [
              IconButton(
                icon: const Icon(
                  Icons.notifications_none,
                  color: _onSurfaceVariant,
                ),
                onPressed: () {},
              ),
            ],
            bottom: PreferredSize(
              preferredSize: const Size.fromHeight(100),
              child: Column(
                children: [
                  Container(
                    margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
                    decoration: BoxDecoration(
                      color: _surface,
                      borderRadius: BorderRadius.circular(99),
                      border: Border.all(color: _outlineVariant),
                    ),
                    child: TextField(
                      controller: _searchController,
                      onChanged: _onSearchChanged,
                      textInputAction: TextInputAction.search,
                      decoration: const InputDecoration(
                        hintText: 'Tìm kiếm chủ đề bạn quan tâm...',
                        hintStyle: TextStyle(color: _outline, fontSize: 14),
                        prefixIcon: Icon(
                          Icons.search,
                          color: _outline,
                          size: 20,
                        ),
                        border: InputBorder.none,
                        contentPadding: EdgeInsets.symmetric(vertical: 12),
                      ),
                    ),
                  ),
                  // CATEGORY filter chips from the community taxonomy API.
                  SizedBox(
                    height: 44,
                    child: Builder(
                      builder: (context) {
                        final chips = buildCategoryChips(_categories);
                        return ListView.separated(
                          scrollDirection: Axis.horizontal,
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          itemCount: chips.length,
                          separatorBuilder: (_, _) => const SizedBox(width: 8),
                          itemBuilder: (_, i) {
                            final chip = chips[i];
                            final selected =
                                _selectedCategoryId == chip.categoryId;
                            return GestureDetector(
                              onTap: () => setState(
                                () => _selectedCategoryId = chip.categoryId,
                              ),
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
                                      : Border.all(color: _outlineVariant),
                                ),
                                child: Text(
                                  chip.label,
                                  style: TextStyle(
                                    fontSize: 13,
                                    fontWeight: selected
                                        ? FontWeight.w600
                                        : FontWeight.normal,
                                    color: selected
                                        ? Colors.white
                                        : _onSurfaceVariant,
                                  ),
                                ),
                              ),
                            );
                          },
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
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
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
                          child: Text(
                            'Không có chủ đề nào',
                            style: TextStyle(color: _outline),
                          ),
                        ),
                      ),
                    )
                  : SliverGrid(
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 2,
                            mainAxisSpacing: 12,
                            crossAxisSpacing: 12,
                            childAspectRatio: 0.82,
                          ),
                      delegate: SliverChildBuilderDelegate((context, i) {
                        final topic = _filteredTopics[i];
                        return _TopicGridCard(
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
                        );
                      }, childCount: _filteredTopics.length),
                    ),
            ),
          ],
        ],
      ),
    );
  }
}

// MOB-TC-001 (CommunityTopicManagement_Test-Spec.md): pulled out as a standalone, testable
// function so a widget test can assert the badge is driven by the real questionCount field and
// not the previous `sortOrder * 100` placeholder — CommunityService.instance being a private
// singleton constructor makes full widget-pump testing of TopicDirectoryScreen impractical here.
String questionCountLabel(CommunityTopic topic) =>
    '${topic.questionCount} câu hỏi';

class TopicCategoryChip {
  final String? categoryId;
  final String label;

  const TopicCategoryChip({required this.categoryId, required this.label});
}

// MOB-TC-002/003 follow the MOB-TC-001 precedent: these are pure top-level helpers rather than
// widget/service mocks because CommunityService remains a private singleton by design.
List<TopicCategoryChip> buildCategoryChips(
  Iterable<CommunityTopic> taxonomyRows,
) {
  final categories =
      taxonomyRows.where((row) => row.type == 'CATEGORY').toList()
        ..sort((a, b) {
          final order = a.sortOrder.compareTo(b.sortOrder);
          return order != 0 ? order : a.name.compareTo(b.name);
        });
  return [
    const TopicCategoryChip(categoryId: null, label: 'Tất cả'),
    ...categories.map(
      (category) =>
          TopicCategoryChip(categoryId: category.id, label: category.name),
    ),
  ];
}

List<CommunityTopic> filterDirectoryTopics(
  Iterable<CommunityTopic> topics, {
  required String? selectedCategoryId,
  required String keyword,
}) {
  final normalizedKeyword = keyword.trim().toLowerCase();
  return topics.where((topic) {
    if (topic.type != 'TOPIC') return false;
    if (selectedCategoryId != null && topic.parentId != selectedCategoryId) {
      return false;
    }
    if (normalizedKeyword.isEmpty) return true;
    return topic.name.toLowerCase().contains(normalizedKeyword) ||
        topic.description.toLowerCase().contains(normalizedKeyword);
  }).toList();
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
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.06),
              blurRadius: 16,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: _secondaryContainer,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: _primary, size: 22),
            ),
            const SizedBox(height: 12),
            Text(
              topic.name,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: _onSurface,
                height: 1.2,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 4),
            Text(
              topic.description.isEmpty
                  ? 'Chủ đề cộng đồng'
                  : topic.description,
              style: const TextStyle(
                fontSize: 11,
                color: _onSurfaceVariant,
                height: 1.3,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            const Spacer(),
            Row(
              children: [
                Expanded(
                  child: Text(
                    questionCountLabel(topic),
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
