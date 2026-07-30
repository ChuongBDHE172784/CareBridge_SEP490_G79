import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/constants/content_stages.dart';
import '../models/community_model.dart';
import '../models/content_model.dart';
import '../services/community_service.dart';
import 'create_question_screen.dart';
import 'question_detail_screen.dart';
import 'bookmarked_questions_screen.dart';
import 'my_questions_screen.dart';
import 'view_content_screen.dart';

/// CB-014 — Community Feed with Realtime Search & Dropdown Filters (UC-54..UC-59, UC-198..UC-201)
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
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLowest = Colors.white;

  final _service = CommunityService.instance;
  final _scrollController = ScrollController();
  final _searchCtrl = TextEditingController();

  Timer? _debounce;

  List<CommunityTopic> _topics = [];
  List<CommunityFeedItem> _items = [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 0;

  // Filter states
  String? _selectedTopicId; // null = "Tất cả chủ đề"
  String? _selectedStage; // null = "Tất cả giai đoạn"
  bool _verifiedOnly = false;

  bool get _canCreateQuestion => AuthState.instance.role == 'MOTHER';

  @override
  void initState() {
    super.initState();
    _selectedTopicId = widget.initialTopicId;
    _scrollController.addListener(_onScroll);
    _loadTopicsAndFeed();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _debounce?.cancel();
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
            _scrollController.position.maxScrollExtent - 240 &&
        !_loading &&
        !_loadingMore &&
        _hasMore) {
      _loadMore();
    }
  }

  void _onSearchChanged(String v) {
    _debounce?.cancel();
    _debounce = Timer(
      const Duration(milliseconds: 400),
      () => _search(refresh: true),
    );
  }

  Future<void> _loadTopicsAndFeed() async {
    setState(() => _loading = true);
    try {
      final topics = await _service.getQuestionTopics();
      if (mounted) {
        setState(() => _topics = topics);
      }
      await _search(refresh: true);
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

  Future<void> _search({bool refresh = false}) async {
    if (refresh) {
      _page = 0;
      _hasMore = true;
    }
    if (!_hasMore) return;
    if (refresh) {
      setState(() => _loading = true);
    }

    try {
      final items = await _service.searchQuestions(
        keyword: _searchCtrl.text.trim().isEmpty
            ? null
            : _searchCtrl.text.trim(),
        topicId: _selectedTopicId,
        stage: _selectedStage,
        hasExpertAnswer: _verifiedOnly ? true : null,
        page: _page,
      );

      if (mounted) {
        setState(() {
          if (refresh) {
            _items = items;
          } else {
            _items.addAll(items);
          }
          _page++;
          _hasMore = items.length >= 20;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          if (refresh) _items = [];
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
      await _search(refresh: false);
    } finally {
      if (mounted) setState(() => _loadingMore = false);
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
          onRefresh: () => _search(refresh: true),
          child: CustomScrollView(
            controller: _scrollController,
            slivers: [
              SliverToBoxAdapter(child: _buildTopBar()),
              SliverToBoxAdapter(child: _buildSearchBar()),
              SliverToBoxAdapter(child: _buildDropdownFilterBar()),
              const SliverToBoxAdapter(child: SizedBox(height: 12)),

              if (_loading)
                const SliverFillRemaining(
                  child: Center(
                    child: CircularProgressIndicator(color: _primaryContainer),
                  ),
                )
              else if (_items.isEmpty)
                SliverFillRemaining(
                  child: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.search_off, size: 48, color: _outline),
                        const SizedBox(height: 12),
                        Text(
                          _searchCtrl.text.isNotEmpty ||
                                  _selectedTopicId != null ||
                                  _selectedStage != null ||
                                  _verifiedOnly
                              ? 'Không tìm thấy câu hỏi phù hợp.'
                              : 'Chưa có bài viết nào.',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                )
              else
                SliverPadding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  sliver: SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, index) {
                        // Insert Bento Section after 2nd post card
                        if (_canCreateQuestion && index == 2) {
                          return _buildBentoSection();
                        }
                        final actualIdx = _canCreateQuestion && index > 2
                            ? index - 1
                            : index;
                        if (actualIdx >= _items.length) return null;
                        final item = _items[actualIdx];
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 14),
                          child: _buildPostCard(item),
                        );
                      },
                      childCount: _items.isEmpty
                          ? 0
                          : _items.length + (_canCreateQuestion ? 1 : 0),
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

  // ── Top Bar: Title & Navigation Shortcuts ──
  Widget _buildTopBar() {
    final canPop = Navigator.of(context).canPop();
    final isMother = AuthState.instance.role == 'MOTHER';
    return Padding(
      padding: EdgeInsets.fromLTRB(canPop ? 12 : 20, 12, 20, 8),
      child: Row(
        children: [
          if (canPop) ...[
            IconButton(
              icon: const Icon(Icons.arrow_back, color: _primary),
              onPressed: () => Navigator.of(context).pop(),
            ),
            const SizedBox(width: 4),
          ],
          const Expanded(
            child: Text(
              'Cộng đồng',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 22,
                fontWeight: FontWeight.w700,
                color: _primary,
              ),
            ),
          ),
          // Bookmarked posts
          _buildTopCircleButton(
            icon: Icons.bookmark_outline,
            tooltip: 'Bài viết đã lưu',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => const BookmarkedQuestionsScreen(),
              ),
            ),
          ),
          const SizedBox(width: 8),
          // My Questions / Verified content
          _buildTopCircleButton(
            icon: isMother ? Icons.article_outlined : Icons.verified_outlined,
            tooltip: isMother ? 'Câu hỏi của tôi' : 'Nội dung đã kiểm duyệt',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => isMother
                    ? const MyQuestionsScreen()
                    : const ViewContentScreen(mode: ContentBrowseMode.family),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTopCircleButton({
    required IconData icon,
    required String tooltip,
    required VoidCallback onTap,
  }) {
    return Container(
      width: 40,
      height: 40,
      decoration: const BoxDecoration(
        color: _surfaceContainer,
        shape: BoxShape.circle,
      ),
      child: IconButton(
        padding: EdgeInsets.zero,
        icon: Icon(icon, color: _onSurfaceVariant, size: 20),
        tooltip: tooltip,
        onPressed: onTap,
      ),
    );
  }

  // ── Realtime Search Input Bar ──
  Widget _buildSearchBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 8),
      child: Container(
        decoration: BoxDecoration(
          color: _surfaceContainerLowest,
          borderRadius: BorderRadius.circular(99),
          border: Border.all(color: _outlineVariant),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.03),
              blurRadius: 10,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            const SizedBox(width: 14),
            const Icon(Icons.search, color: _primary, size: 20),
            const SizedBox(width: 8),
            Expanded(
              child: TextField(
                controller: _searchCtrl,
                onChanged: _onSearchChanged,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _onSurface,
                ),
                decoration: const InputDecoration(
                  hintText: 'Tìm câu hỏi, chủ đề hoặc chuyên gia...',
                  hintStyle: TextStyle(
                    fontFamily: 'Lexend',
                    color: _outline,
                    fontSize: 13,
                  ),
                  border: InputBorder.none,
                  contentPadding: EdgeInsets.symmetric(vertical: 12),
                ),
              ),
            ),
            if (_searchCtrl.text.isNotEmpty)
              IconButton(
                icon: const Icon(Icons.clear, color: _outline, size: 18),
                onPressed: () {
                  _searchCtrl.clear();
                  _search(refresh: true);
                },
              )
            else
              const Padding(
                padding: EdgeInsets.only(right: 14),
                child: Icon(Icons.tune, color: _outline, size: 18),
              ),
          ],
        ),
      ),
    );
  }

  // ── Dropdown Filters: Topic & Stage & Verified ──
  Widget _buildDropdownFilterBar() {
    final selectedTopic = _topics.firstWhere(
      (t) => t.id == _selectedTopicId,
      orElse: () => CommunityTopic(
        id: '',
        name: 'Tất cả chủ đề',
        description: '',
        icon: '',
        isHidden: false,
        sortOrder: 0,
      ),
    );

    final selectedStageOption = contentStageOptions.firstWhere(
      (s) => s.value == _selectedStage,
      orElse: () =>
          const ContentStageOption(value: '', label: 'Tất cả giai đoạn'),
    );

    return SizedBox(
      height: 42,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 20),
        children: [
          // Dropdown 1: Topic Filter (matches question creation topics)
          PopupMenuButton<String>(
            initialValue: _selectedTopicId ?? '',
            onSelected: (val) {
              setState(() => _selectedTopicId = val.isEmpty ? null : val);
              _search(refresh: true);
            },
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
            itemBuilder: (context) => [
              const PopupMenuItem<String>(
                value: '',
                child: Text(
                  'Tất cả chủ đề',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 13),
                ),
              ),
              ..._topics
                  .where((t) => !t.isHidden)
                  .map(
                    (t) => PopupMenuItem<String>(
                      value: t.id,
                      child: Text(
                        t.name,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                        ),
                      ),
                    ),
                  ),
            ],
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: _selectedTopicId != null
                    ? _primary
                    : _surfaceContainerLowest,
                borderRadius: BorderRadius.circular(99),
                border: _selectedTopicId != null
                    ? null
                    : Border.all(color: _outlineVariant),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.category_outlined,
                    size: 15,
                    color: _selectedTopicId != null
                        ? Colors.white
                        : _onSurfaceVariant,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    _selectedTopicId != null ? selectedTopic.name : 'Chủ đề',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: _selectedTopicId != null
                          ? Colors.white
                          : _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(width: 2),
                  Icon(
                    Icons.arrow_drop_down,
                    size: 18,
                    color: _selectedTopicId != null
                        ? Colors.white
                        : _onSurfaceVariant,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 8),

          // Dropdown 2: Stage Filter
          PopupMenuButton<String>(
            initialValue: _selectedStage ?? '',
            onSelected: (val) {
              setState(() => _selectedStage = val.isEmpty ? null : val);
              _search(refresh: true);
            },
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
            itemBuilder: (context) => [
              const PopupMenuItem<String>(
                value: '',
                child: Text(
                  'Tất cả giai đoạn',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 13),
                ),
              ),
              ...contentStageOptions.map(
                (stage) => PopupMenuItem<String>(
                  value: stage.value,
                  child: Text(
                    stage.label,
                    style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                  ),
                ),
              ),
            ],
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: _selectedStage != null
                    ? _primary
                    : _surfaceContainerLowest,
                borderRadius: BorderRadius.circular(99),
                border: _selectedStage != null
                    ? null
                    : Border.all(color: _outlineVariant),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.timeline_outlined,
                    size: 15,
                    color: _selectedStage != null
                        ? Colors.white
                        : _onSurfaceVariant,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    _selectedStage != null
                        ? selectedStageOption.label
                        : 'Giai đoạn',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: _selectedStage != null
                          ? Colors.white
                          : _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(width: 2),
                  Icon(
                    Icons.arrow_drop_down,
                    size: 18,
                    color: _selectedStage != null
                        ? Colors.white
                        : _onSurfaceVariant,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 8),

          // Filter Chip: Expert verified
          GestureDetector(
            onTap: () {
              setState(() => _verifiedOnly = !_verifiedOnly);
              _search(refresh: true);
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: _verifiedOnly ? _primary : _surfaceContainerLowest,
                borderRadius: BorderRadius.circular(99),
                border: _verifiedOnly
                    ? null
                    : Border.all(color: _outlineVariant),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Chuyên gia',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: _verifiedOnly ? Colors.white : _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(width: 4),
                  Icon(
                    Icons.verified,
                    size: 14,
                    color: _verifiedOnly ? Colors.white : _onSurfaceVariant,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ── Bento Section (Full-width Create Question Callout) ──
  Widget _buildBentoSection() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: _primaryContainer,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(Icons.live_help, color: Colors.white, size: 26),
            const SizedBox(height: 8),
            const Text(
              'Chưa tìm thấy câu trả lời?',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 4),
            const Text(
              'Đặt câu hỏi để nhận phản hồi từ cộng đồng và chuyên gia.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: Colors.white70,
              ),
            ),
            const SizedBox(height: 12),
            GestureDetector(
              onTap: () => Navigator.of(context)
                  .push(
                    MaterialPageRoute(
                      builder: (_) => const CreateQuestionScreen(),
                    ),
                  )
                  .then((_) => _search(refresh: true)),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFF51271B),
                  borderRadius: BorderRadius.circular(99),
                ),
                child: const Text(
                  'Đặt câu hỏi ngay',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: _primaryContainer,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ── Question Feed Post Card ──
  Widget _buildPostCard(CommunityFeedItem item) {
    final hasExpert = item.hasExpertAnswer;
    final bookmarked = item.bookmarked;
    final liked = item.liked;

    return GestureDetector(
      onTap: () => _navigateToQuestionDetail(item),
      behavior: HitTestBehavior.opaque,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.05),
              blurRadius: 16,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top author & bookmark row
            Row(
              children: [
                // Author avatar
                Container(
                  width: 36,
                  height: 36,
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    color: _surfaceContainerHigh,
                  ),
                  child: Center(
                    child: Text(
                      item.authorDisplay.isNotEmpty
                          ? item.authorDisplay[0].toUpperCase()
                          : '?',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: _primary,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.authorDisplay,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                      Text(
                        _timeAgo(item.createdAt),
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                // Bookmark toggle button
                GestureDetector(
                  onTap: () => _toggleBookmark(item.id),
                  behavior: HitTestBehavior.opaque,
                  child: Padding(
                    padding: const EdgeInsets.all(6),
                    child: Icon(
                      bookmarked ? Icons.bookmark : Icons.bookmark_border,
                      size: 22,
                      color: bookmarked ? _primary : _outline,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),

            // Badges row (Topic & Stage)
            if (item.topicName.isNotEmpty || item.stage.isNotEmpty) ...[
              Wrap(
                spacing: 6,
                runSpacing: 4,
                children: [
                  if (item.topicName.isNotEmpty) _TagBadge(item.topicName),
                  if (item.stage.isNotEmpty)
                    _TagBadge(contentStageLabel(item.stage)),
                ],
              ),
              const SizedBox(height: 8),
            ],

            // Question Title
            Text(
              item.title,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: _onSurface,
                height: 1.35,
              ),
            ),
            const SizedBox(height: 10),
            const Divider(color: _surfaceContainerHighest, height: 1),
            const SizedBox(height: 10),

            // Bottom info bar: Status pill, like button, comment count, avatar stack
            Row(
              children: [
                // Expert/Discussion Status Pill
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: hasExpert
                        ? _primary.withValues(alpha: 0.1)
                        : _surfaceContainerHigh.withValues(alpha: 0.5),
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        hasExpert ? Icons.verified : Icons.forum_outlined,
                        size: 13,
                        color: hasExpert ? _primary : _onSurfaceVariant,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        hasExpert ? 'Bác sĩ chuyên gia' : 'Thảo luận',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 10,
                          fontWeight: FontWeight.w500,
                          color: hasExpert ? _primary : _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),

                // Interactive Like Button & Count
                GestureDetector(
                  onTap: () => _toggleQuestionLike(item.id),
                  behavior: HitTestBehavior.opaque,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 6,
                      vertical: 4,
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          liked ? Icons.favorite : Icons.favorite_border,
                          size: 18,
                          color: liked
                              ? const Color(0xFFBA1A1A)
                              : _onSurfaceVariant,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          '${item.likeCount}',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            fontWeight: liked
                                ? FontWeight.bold
                                : FontWeight.w500,
                            color: liked
                                ? const Color(0xFFBA1A1A)
                                : _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 8),

                // Answer count text
                Text(
                  '• ${item.answerCount} trả lời',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    color: _onSurfaceVariant,
                  ),
                ),

                const Spacer(),

                // Avatar stack preview
                Row(
                  children: List.generate(
                    item.answerCount > 0 ? (item.answerCount > 3 ? 2 : 1) : 0,
                    (i) => Align(
                      widthFactor: 0.65,
                      child: CircleAvatar(
                        radius: 11,
                        backgroundColor: const Color(0xFFFFE9E3),
                        child: const Icon(
                          Icons.person,
                          size: 11,
                          color: _primaryContainer,
                        ),
                      ),
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

  void _navigateToQuestionDetail(CommunityFeedItem item) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => QuestionDetailScreen(questionId: item.id),
      ),
    );
  }

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
      if (mounted) {
        setState(() => _items[index] = _items[index].copyWith(bookmarked: was));
      }
    }
  }

  Future<void> _toggleQuestionLike(String questionId) async {
    final index = _items.indexWhere((i) => i.id == questionId);
    if (index == -1) return;
    final wasLiked = _items[index].liked;
    final prevCount = _items[index].likeCount;
    setState(
      () => _items[index] = _items[index].copyWith(
        liked: !wasLiked,
        likeCount: wasLiked
            ? (prevCount > 0 ? prevCount - 1 : 0)
            : prevCount + 1,
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

  // ── Floating Action Button: Create New Question ──
  Widget _buildFab() {
    if (!_canCreateQuestion) return const SizedBox.shrink();
    return Container(
      key: const Key('community-create-question-fab'),
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
              .then((_) => _search(refresh: true)),
          child: const Padding(
            padding: EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.edit, size: 18, color: Colors.white),
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

class _TagBadge extends StatelessWidget {
  final String label;
  const _TagBadge(this.label);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE2D9),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 11,
          color: Color(0xFF845143),
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}
