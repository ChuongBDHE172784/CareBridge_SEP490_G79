import 'dart:async';
import 'package:flutter/material.dart';
import '../services/community_service.dart';
import '../models/community_model.dart';
import 'post_answer_screen.dart';

class ExpertQuestionQueueScreen extends StatefulWidget {
  final String? expertName;
  final String? expertAvatarUrl;

  const ExpertQuestionQueueScreen({
    super.key,
    this.expertName,
    this.expertAvatarUrl,
  });

  @override
  State<ExpertQuestionQueueScreen> createState() =>
      _ExpertQuestionQueueScreenState();
}

class _ExpertQuestionQueueScreenState extends State<ExpertQuestionQueueScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _tertiaryFixed = Color(0xFFE9E1DB);

  static const _filters = [
    'Tất cả',
    'Khẩn cấp',
    'Chuyên môn của tôi',
    'Chưa trả lời',
  ];

  final _searchCtrl = TextEditingController();
  Timer? _debounce;
  int _selectedFilter = 0;
  List<CommunityFeedItem> _questions = [];
  bool _loading = false;
  int _page = 0;
  bool _hasMore = true;
  final _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _load(refresh: true);
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _debounce?.cancel();
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 200 &&
        !_loading &&
        _hasMore) {
      _load();
    }
  }

  void _onSearchChanged(String v) {
    _debounce?.cancel();
    _debounce = Timer(
      const Duration(milliseconds: 400),
      () => _load(refresh: true),
    );
  }

  Future<void> _load({bool refresh = false}) async {
    if (refresh) {
      _page = 0;
      _hasMore = true;
    }
    if (!_hasMore || _loading) return;
    setState(() => _loading = true);
    try {
      bool? hasExpertAnswer;
      if (_selectedFilter == 3) hasExpertAnswer = false; // Chưa trả lời
      final items = await CommunityService.instance.searchQuestions(
        keyword: _searchCtrl.text.trim().isEmpty
            ? null
            : _searchCtrl.text.trim(),
        hasExpertAnswer: hasExpertAnswer,
        page: _page,
      );
      if (mounted) {
        setState(() {
          if (refresh) {
            _questions = items;
          } else {
            _questions.addAll(items);
          }
          _hasMore = items.length >= 20;
          _page++;
        });
      }
    } catch (_) {
      if (mounted) {}
    } finally {
      if (mounted) setState(() => _loading = false);
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
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Hỏi đáp Cộng đồng',
          style: TextStyle(
            color: _primary,
            fontWeight: FontWeight.bold,
            fontSize: 17,
          ),
        ),
        centerTitle: true,
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: CircleAvatar(
              radius: 18,
              backgroundColor: const Color(0xFFFFE2D9),
              backgroundImage: widget.expertAvatarUrl != null
                  ? NetworkImage(widget.expertAvatarUrl!)
                  : null,
              child: widget.expertAvatarUrl == null
                  ? const Icon(Icons.person, color: _primaryContainer, size: 20)
                  : null,
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          // Search bar
          Container(
            color: _surface,
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
            child: Row(
              children: [
                Expanded(
                  child: Container(
                    height: 48,
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF1EC),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: _outlineVariant),
                    ),
                    child: Row(
                      children: [
                        const SizedBox(width: 10),
                        const Icon(Icons.search, color: _outline, size: 20),
                        const SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: _searchCtrl,
                            onChanged: _onSearchChanged,
                            decoration: const InputDecoration(
                              hintText: 'Tìm kiếm câu hỏi...',
                              hintStyle: TextStyle(
                                color: _outline,
                                fontSize: 14,
                              ),
                              border: InputBorder.none,
                              contentPadding: EdgeInsets.zero,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFE2D9),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(Icons.tune, color: _primary),
                ),
              ],
            ),
          ),
          // Filter chips
          Container(
            color: _surface,
            padding: const EdgeInsets.symmetric(vertical: 10),
            child: SizedBox(
              height: 36,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: _filters.length,
                separatorBuilder: (_, _) => const SizedBox(width: 8),
                itemBuilder: (_, i) {
                  final selected = _selectedFilter == i;
                  return GestureDetector(
                    onTap: () {
                      setState(() => _selectedFilter = i);
                      _load(refresh: true);
                    },
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 6,
                      ),
                      decoration: BoxDecoration(
                        color: selected ? _primary : _surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(99),
                      ),
                      child: Text(
                        _filters[i],
                        style: TextStyle(
                          fontSize: 13,
                          color: selected ? Colors.white : _onSurfaceVariant,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
          const Divider(height: 1, color: Color(0xFFFFE9E3)),
          // Question list
          Expanded(
            child: _loading && _questions.isEmpty
                ? const Center(
                    child: CircularProgressIndicator(color: _primary),
                  )
                : _questions.isEmpty
                ? _buildEmptyState()
                : ListView.separated(
                    controller: _scroll,
                    padding: const EdgeInsets.all(16),
                    itemCount: _questions.length + (_loading ? 1 : 0),
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (_, i) {
                      if (i >= _questions.length) {
                        return const Center(
                          child: CircularProgressIndicator(color: _primary),
                        );
                      }
                      return _ExpertQuestionCard(item: _questions[i]);
                    },
                  ),
          ),
        ],
      ),
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.question_answer_outlined,
            size: 56,
            color: _primaryContainer.withValues(alpha: 0.4),
          ),
          const SizedBox(height: 12),
          const Text(
            'Không có câu hỏi nào',
            style: TextStyle(color: _onSurfaceVariant, fontSize: 15),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomNav() {
    return Container(
      height: 72,
      decoration: const BoxDecoration(
        color: _surface,
        boxShadow: [
          BoxShadow(
            color: Colors.black12,
            blurRadius: 12,
            offset: Offset(0, -2),
          ),
        ],
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(16),
          topRight: Radius.circular(16),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _NavItem(icon: Icons.home, label: 'Trang chủ', active: false),
          _NavItem(icon: Icons.assignment, label: 'Yêu cầu', active: false),
          _NavItem(
            icon: Icons.forum,
            label: 'Cộng đồng',
            active: true,
            filled: true,
          ),
          _NavItem(icon: Icons.person, label: 'Tài khoản', active: false),
        ],
      ),
    );
  }
}

class _ExpertQuestionCard extends StatelessWidget {
  final CommunityFeedItem item;

  const _ExpertQuestionCard({required this.item});

  @override
  Widget build(BuildContext context) {
    final isPending = !item.hasExpertAnswer && item.answerCount == 0;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.06),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Container(
                    width: 32,
                    height: 32,
                    decoration: const BoxDecoration(
                      color: Color(0xFFF6DACF),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.person_outline,
                      size: 18,
                      color: Color(0xFF524440),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    item.authorDisplay.isEmpty ||
                            item.authorDisplay == 'Ẩn danh'
                        ? 'Ẩn danh'
                        : item.authorDisplay,
                    style: const TextStyle(
                      fontSize: 13,
                      color: Color(0xFF524440),
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: isPending
                      ? const Color(0xFFF6DACF)
                      : const Color(0xFFE9E1DB),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  isPending ? 'CHỜ DUYỆT' : 'CÔNG KHAI',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: isPending
                        ? const Color(0xFF524440)
                        : const Color(0xFF4A4642),
                    letterSpacing: 0.5,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            item.title,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF271812),
              height: 1.3,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              const Icon(Icons.forum, size: 15, color: Color(0xFF845143)),
              const SizedBox(width: 4),
              Text(
                '${item.answerCount} câu trả lời',
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF845143),
                ),
              ),
              const SizedBox(width: 16),
              const Icon(Icons.schedule, size: 15, color: Color(0xFF84736F)),
              const SizedBox(width: 4),
              Text(
                _timeAgo(item.createdAt),
                style: const TextStyle(fontSize: 12, color: Color(0xFF84736F)),
              ),
            ],
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: isPending
                ? OutlinedButton.icon(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFF845143),
                      backgroundColor: const Color(0xFFFFE2D9),
                      side: BorderSide.none,
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => PostAnswerScreen(
                          questionId: item.id,
                          questionTitle: item.title,
                          authorName: item.authorDisplay,
                          topicName: item.topicName.isNotEmpty
                              ? item.topicName
                              : null,
                          timeAgo: _timeAgo(item.createdAt),
                        ),
                      ),
                    ),
                    icon: const Icon(Icons.edit, size: 16),
                    label: const Text(
                      'Trả lời câu hỏi',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                  )
                : OutlinedButton.icon(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFFC98C7B),
                      side: const BorderSide(
                        color: Color(0xFFC98C7B),
                        width: 2,
                      ),
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => PostAnswerScreen(
                          questionId: item.id,
                          questionTitle: item.title,
                          authorName: item.authorDisplay,
                          topicName: item.topicName.isNotEmpty
                              ? item.topicName
                              : null,
                          timeAgo: _timeAgo(item.createdAt),
                        ),
                      ),
                    ),
                    icon: const Icon(Icons.visibility_outlined, size: 16),
                    label: const Text(
                      'Xem chi tiết',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  String _timeAgo(String iso) {
    try {
      final dt = DateTime.parse(iso);
      final diff = DateTime.now().difference(dt);
      if (diff.inHours < 1) return 'Vừa xong';
      if (diff.inHours < 24) return '${diff.inHours} giờ trước';
      return '${diff.inDays} ngày trước';
    } catch (_) {
      return '';
    }
  }
}

class _NavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool active;
  final bool filled;

  const _NavItem({
    required this.icon,
    required this.label,
    required this.active,
    this.filled = false,
  });

  @override
  Widget build(BuildContext context) {
    if (active) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 6),
        decoration: BoxDecoration(
          color: const Color(0xFFC98C7B),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: Colors.white, size: 22),
            const SizedBox(height: 2),
            Text(
              label,
              style: const TextStyle(
                fontSize: 10,
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      );
    }
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: const Color(0xFF6E5A52), size: 22),
        const SizedBox(height: 2),
        Text(
          label,
          style: const TextStyle(fontSize: 10, color: Color(0xFF6E5A52)),
        ),
      ],
    );
  }
}
