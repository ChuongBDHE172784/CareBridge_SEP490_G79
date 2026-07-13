import 'package:flutter/material.dart';
import '../models/community_model.dart';
import '../services/community_service.dart';
import 'community_search_screen.dart';
import 'create_question_screen.dart';
import 'question_detail_screen.dart';
import 'topic_directory_screen.dart';
import 'verified_content_search_screen.dart';
import 'bookmarked_questions_screen.dart';

/// CB-014 — Community Feed (UC-54..UC-59, UC-198..UC-201)
/// Displays topic filter chips and a scrollable list of community posts.
/// Rendered as tab index 2 inside HomeShell's IndexedStack (no own bottom nav).
/// Accepts [initialTopicId] when navigated from TopicDirectoryScreen (UC-163).
class CommunityFeedScreen extends StatefulWidget {
  final String? initialTopicId;
  const CommunityFeedScreen({super.key, this.initialTopicId});

  @override
  State<CommunityFeedScreen> createState() => _CommunityFeedScreenState();
}

class _CommunityFeedScreenState extends State<CommunityFeedScreen> {
  // ── Design tokens (Warm Claymorphism palette) ──
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  // ignore: unused_field
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  // ignore: unused_field
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  // ignore: unused_field
  static const _error = Color(0xFFBA1A1A);

  final _service = CommunityService.instance;
  final _scrollController = ScrollController();

  List<CommunityTopic> _topics = [];
  List<CommunityFeedItem> _items = [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 0;
  String? _selectedTopicId; // null = "Tất cả"

  @override
  void initState() {
    super.initState();
    _selectedTopicId = widget.initialTopicId;
    _scrollController.addListener(_onScroll);
    _load();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 240) {
      _loadMore();
    }
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final results = await Future.wait([
        _service.getTopics(),
        _service.getFeed(topicId: _selectedTopicId, page: 0),
      ]);
      final feed = results[1] as List<CommunityFeedItem>;
      if (mounted) {
        setState(() {
          _topics = results[0] as List<CommunityTopic>;
          _items = feed;
          _page = 1;
          _hasMore = feed.length >= 20;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _topics = [];
          _items = [];
          _hasMore = false;
          _loading = false;
        });
      }
    }
  }

  Future<void> _loadMore() async {
    if (_loading || _loadingMore || !_hasMore) return;
    setState(() => _loadingMore = true);
    try {
      final feed = await _service.getFeed(
        topicId: _selectedTopicId,
        page: _page,
      );
      if (mounted) {
        setState(() {
          _items.addAll(feed);
          _page++;
          _hasMore = feed.length >= 20;
        });
      }
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  Future<void> _onTopicSelected(String? topicId) async {
    if (topicId == _selectedTopicId) return;
    setState(() {
      _selectedTopicId = topicId;
      _loading = true;
    });
    try {
      final feed = await _service.getFeed(topicId: topicId, page: 0);
      if (mounted) {
        setState(() {
          _items = feed;
          _page = 1;
          _hasMore = feed.length >= 20;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _items = [];
          _hasMore = false;
          _loading = false;
        });
      }
    }
  }

  // ── Relative time formatting ──
  String _timeAgo(String isoDate) {
    try {
      final dt = DateTime.parse(isoDate);
      final diff = DateTime.now().difference(dt);
      if (diff.inMinutes < 1) return 'Vừa xong';
      if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
      if (diff.inHours < 24) return '${diff.inHours} giờ trước';
      if (diff.inDays < 7) return '${diff.inDays} ngày trước';
      return '${(diff.inDays / 7).floor()} tuần trước';
    } catch (_) {
      return '';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      floatingActionButton: _buildFab(),
      body: SafeArea(
        child: RefreshIndicator(
          color: _primaryContainer,
          onRefresh: _load,
          child: CustomScrollView(
            controller: _scrollController,
            slivers: [
              SliverToBoxAdapter(child: _buildTopBar()),
              SliverToBoxAdapter(child: _buildTopicChips()),
              const SliverToBoxAdapter(child: SizedBox(height: 8)),
              if (_loading)
                const SliverFillRemaining(
                  child: Center(
                    child: CircularProgressIndicator(color: _primaryContainer),
                  ),
                )
              else if (_items.isEmpty)
                const SliverFillRemaining(
                  child: Center(
                    child: Text(
                      'Chưa có bài viết nào.',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ),
                )
              else
                SliverPadding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  sliver: SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, index) => Padding(
                        padding: const EdgeInsets.only(bottom: 16),
                        child: _buildPostCard(_items[index]),
                      ),
                      childCount: _items.length,
                    ),
                  ),
                ),
              if (_loadingMore)
                const SliverToBoxAdapter(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    ),
                  ),
                ),
              // Extra space for FAB
              const SliverToBoxAdapter(child: SizedBox(height: 80)),
            ],
          ),
        ),
      ),
    );
  }

  // ── Top bar: title + search + topic library + article ──
  Widget _buildTopBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 12, 16, 8),
      child: Row(
        children: [
          const Expanded(
            child: Text(
              'Cộng đồng',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: _primary,
              ),
            ),
          ),
          // Search button (UC-162)
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(
              color: _surfaceContainer,
              shape: BoxShape.circle,
            ),
            child: IconButton(
              icon: const Icon(
                Icons.search,
                color: _onSurfaceVariant,
                size: 22,
              ),
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => const CommunitySearchScreen(),
                ),
              ),
            ),
          ),
          const SizedBox(width: 8),
          // Topic directory button (UC-163 / CB-118)
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(
              color: _surfaceContainer,
              shape: BoxShape.circle,
            ),
            child: IconButton(
              icon: const Icon(
                Icons.bookmarks_outlined,
                color: _onSurfaceVariant,
                size: 22,
              ),
              tooltip: 'Thư viện chủ đề',
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const TopicDirectoryScreen()),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(color: _surfaceContainer, shape: BoxShape.circle),
            child: IconButton(
              icon: const Icon(Icons.bookmark_outline, color: _onSurfaceVariant, size: 22),
              tooltip: 'Bài viết đã lưu',
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const BookmarkedQuestionsScreen())),
            ),
          ),
          const SizedBox(width: 8),
          // Verified content search button (UC-224)
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(
              color: _surfaceContainer,
              shape: BoxShape.circle,
            ),
            child: IconButton(
              icon: const Icon(
                Icons.verified_outlined,
                color: _onSurfaceVariant,
                size: 22,
              ),
              tooltip: 'Nội dung đã kiểm duyệt',
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => const VerifiedContentSearchScreen(),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ── Topic filter chips ──
  Widget _buildTopicChips() {
    return SizedBox(
      height: 48,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        children: [
          _topicChip(
            label: 'Tất cả',
            isActive: _selectedTopicId == null,
            onTap: () => _onTopicSelected(null),
          ),
          ..._topics
              .where((t) => !t.isHidden)
              .map(
                (t) => _topicChip(
                  label: t.name,
                  isActive: _selectedTopicId == t.id,
                  onTap: () => _onTopicSelected(t.id),
                ),
              ),
        ],
      ),
    );
  }

  Widget _topicChip({
    required String label,
    required bool isActive,
    required VoidCallback onTap,
  }) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
          decoration: BoxDecoration(
            color: isActive ? _primaryContainer : _surfaceContainer,
            borderRadius: BorderRadius.circular(99),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w500,
              color: isActive ? Colors.white : _onSurfaceVariant,
            ),
          ),
        ),
      ),
    );
  }

  // UC-199: navigate to full question detail instead of jumping straight to answer
  void _navigateToQuestionDetail(CommunityFeedItem item) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => QuestionDetailScreen(questionId: item.id),
      ),
    );
  }

  // UC-58: toggle bookmark with optimistic UI — hydrated from the server's `bookmarked`
  // field on load (see CommunityFeedItem), not tracked purely client-side anymore.
  Future<void> _toggleBookmark(String questionId) async {
    final index = _items.indexWhere((i) => i.id == questionId);
    if (index == -1) return;
    final was = _items[index].bookmarked;
    setState(() => _items[index] = _items[index].copyWith(bookmarked: !was));
    try {
      final result = await _service.toggleBookmark(questionId);
      if (mounted) {
        setState(
          () => _items[index] = _items[index].copyWith(
            bookmarked: result.bookmarked,
          ),
        );
      }
    } catch (_) {
      // Rollback on error
      if (mounted) {
        setState(() => _items[index] = _items[index].copyWith(bookmarked: was));
      }
    }
  }

  // Toggle like on a question with optimistic UI — hydrated from the server's `liked`
  // field on load (see CommunityFeedItem).
  Future<void> _toggleQuestionLike(String questionId) async {
    final index = _items.indexWhere((i) => i.id == questionId);
    if (index == -1) return;
    final wasLiked = _items[index].liked;
    final prevCount = _items[index].likeCount;
    setState(
      () => _items[index] = _items[index].copyWith(
        liked: !wasLiked,
        likeCount: wasLiked ? prevCount - 1 : prevCount + 1,
      ),
    );
    try {
      final result = await _service.toggleQuestionLike(questionId);
      if (mounted) {
        setState(
          () => _items[index] = _items[index].copyWith(
            liked: result.liked,
            likeCount: result.likeCount,
          ),
        );
      }
    } catch (_) {
      // Rollback on error
      if (mounted) {
        setState(
          () => _items[index] = _items[index].copyWith(
            liked: wasLiked,
            likeCount: prevCount,
          ),
        );
      }
    }
  }

  // ── Post card ──
  Widget _buildPostCard(CommunityFeedItem item) {
    final bookmarked = item.bookmarked;
    return GestureDetector(
      onTap: () => _navigateToQuestionDetail(item),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: const Color(0x0F5A463F),
              blurRadius: 20,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Author row
            Row(
              children: [
                // Avatar
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: _surfaceContainerHigh,
                    border: Border.all(
                      color: _surfaceContainerHighest,
                      width: 1,
                    ),
                  ),
                  child: Center(
                    child: Text(
                      item.authorDisplay.isNotEmpty
                          ? item.authorDisplay[0]
                          : '?',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: _primary,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.authorDisplay,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                      Text(
                        _timeAgo(item.createdAt),
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                // UC-58: bookmark toggle
                GestureDetector(
                  onTap: () => _toggleBookmark(item.id),
                  child: Padding(
                    padding: const EdgeInsets.all(4),
                    child: Icon(
                      bookmarked ? Icons.bookmark : Icons.bookmark_border,
                      size: 22,
                      color: bookmarked ? _primary : _onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            // Title
            Text(
              item.title,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: _primary,
                height: 1.3,
              ),
            ),
            const SizedBox(height: 8),
            // Body preview (3 lines)
            Text(
              _mockBodyForItem(item),
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
            ),
            // Expert answer badge
            if (item.hasExpertAnswer) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: _secondaryContainer.withAlpha(77),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 32,
                      height: 32,
                      decoration: const BoxDecoration(
                        color: _secondaryContainer,
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.verified,
                        size: 18,
                        color: _primary,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Bác sĩ Nhi khoa đã trả lời',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: _onSurface,
                            ),
                          ),
                          Text(
                            '"Mẹ đừng quá lo lắng, giai đoạn...',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 12,
                              color: _onSurfaceVariant.withAlpha(178),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 16),
            // Footer: likes, comments, topic tag
            Row(
              children: [
                GestureDetector(
                  onTap: () => _toggleQuestionLike(item.id),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        item.liked ? Icons.favorite : Icons.favorite_border,
                        size: 20,
                        color: item.liked ? _primary : _onSurfaceVariant,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '${item.likeCount}',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                const Icon(
                  Icons.chat_bubble_outline,
                  size: 20,
                  color: _onSurfaceVariant,
                ),
                const SizedBox(width: 4),
                Text(
                  '${item.answerCount}',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                  ),
                ),
                const Spacer(),
                if (item.topicName.isNotEmpty)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: _surfaceContainerLow,
                      borderRadius: BorderRadius.circular(99),
                      border: Border.all(
                        color: _surfaceContainerHigh,
                        width: 1,
                      ),
                    ),
                    child: Text(
                      item.topicName,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            // UC-199: view detail + UC-56: reply CTA
            GestureDetector(
              onTap: () => _navigateToQuestionDetail(item),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 10),
                decoration: BoxDecoration(
                  color: _surfaceContainerLow,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.open_in_new, size: 16, color: _primary),
                    const SizedBox(width: 6),
                    Text(
                      'Xem chi tiết (${item.answerCount} câu trả lời)',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: _primary,
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

  // Provides a mock body for items that don't carry body text
  String _mockBodyForItem(CommunityFeedItem item) {
    switch (item.id) {
      case 'p1':
        return 'Chào các mẹ, bé nhà mình được 6 tháng rưỡi rồi, mình bắt đầu cho bé ăn dặm kiểu Nhật nhưng bé có vẻ không hợp tác, hay nhè ra...';
      case 'p2':
        return 'Mình thấy nhiều mẹ hỏi về bỉm cho bé da nhạy cảm nên ngồi lên review chút về dòng...';
      default:
        return item.title;
    }
  }

  // ── FAB — UC-54: create new question ──
  Widget _buildFab() {
    return Container(
      height: 48,
      decoration: BoxDecoration(
        color: _primaryContainer,
        borderRadius: BorderRadius.circular(99),
        boxShadow: [
          BoxShadow(
            color: _primaryContainer.withAlpha(77),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(99),
          onTap: () => Navigator.of(context)
              .push(
                MaterialPageRoute(builder: (_) => const CreateQuestionScreen()),
              )
              .then((_) => _load()),
          child: const Padding(
            padding: EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.edit, size: 20, color: Colors.white),
                SizedBox(width: 8),
                Text(
                  'Đặt câu hỏi',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
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
