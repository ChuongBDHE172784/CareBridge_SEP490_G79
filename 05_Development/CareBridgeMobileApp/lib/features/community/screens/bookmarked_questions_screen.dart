import 'package:flutter/material.dart';
import '../models/community_model.dart';
import '../services/community_service.dart';
import 'question_detail_screen.dart';

class BookmarkedQuestionsScreen extends StatefulWidget {
  const BookmarkedQuestionsScreen({super.key});
  @override
  State<BookmarkedQuestionsScreen> createState() =>
      _BookmarkedQuestionsScreenState();
}

class _BookmarkedQuestionsScreenState extends State<BookmarkedQuestionsScreen> {
  final _scroll = ScrollController();
  final _items = <CommunityFeedItem>[];
  bool _loading = true, _loadingMore = false, _hasMore = true;
  int _page = 0;
  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _reload();
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 160) {
      _loadMore();
    }
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _page = 0;
      _hasMore = true;
    });
    try {
      final page = await CommunityService.instance.getBookmarks(page: 0);
      if (mounted) {
        setState(() {
          _items
            ..clear()
            ..addAll(page);
          _page = 1;
          _hasMore = page.length >= 20;
        });
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadMore() async {
    if (_loading || _loadingMore || !_hasMore) return;
    setState(() => _loadingMore = true);
    try {
      final page = await CommunityService.instance.getBookmarks(page: _page);
      if (mounted) {
        setState(() {
          _items.addAll(page);
          _page++;
          _hasMore = page.length >= 20;
        });
      }
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    const accent = Color(0xFFC98C7B);
    const background = Color(0xFFF6F1EC);
    const primaryText = Color(0xFF5A463F);
    const secondaryText = Color(0xFF74615A);
    const accentText = Color(0xFF925B4E);

    return Scaffold(
      backgroundColor: background,
      appBar: AppBar(
        backgroundColor: background,
        foregroundColor: primaryText,
        elevation: 0,
        scrolledUnderElevation: 0,
        titleSpacing: 4,
        title: const Text(
          'Bài viết đã lưu',
          style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: -0.3),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: accent))
          : _items.isEmpty
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(32),
                child: Container(
                  padding: const EdgeInsets.all(28),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(28),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x0F5A463F),
                        blurRadius: 28,
                        offset: Offset(0, 12),
                      ),
                    ],
                  ),
                  child: const Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      CircleAvatar(
                        radius: 30,
                        backgroundColor: Color(0x26C98C7B),
                        child: Icon(
                          Icons.bookmark_outline,
                          color: accent,
                          size: 30,
                        ),
                      ),
                      SizedBox(height: 18),
                      Text(
                        'Chưa có bài viết đã lưu',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: primaryText,
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      SizedBox(height: 8),
                      Text(
                        'Lưu những bài viết hữu ích để xem lại bất cứ lúc nào.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: secondaryText, height: 1.45),
                      ),
                    ],
                  ),
                ),
              ),
            )
          : RefreshIndicator(
              color: accent,
              backgroundColor: Colors.white,
              onRefresh: _reload,
              child: ListView.separated(
                controller: _scroll,
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
                itemCount: _items.length + (_loadingMore ? 1 : 0),
                separatorBuilder: (_, _) => const SizedBox(height: 12),
                itemBuilder: (context, index) {
                  if (index == _items.length) {
                    return const Padding(
                      padding: EdgeInsets.all(16),
                      child: Center(
                        child: CircularProgressIndicator(color: accent),
                      ),
                    );
                  }
                  final item = _items[index];
                  return Material(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(24),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(24),
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) =>
                              QuestionDetailScreen(questionId: item.id),
                        ),
                      ),
                      child: Container(
                        padding: const EdgeInsets.all(18),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(24),
                          boxShadow: const [
                            BoxShadow(
                              color: Color(0x0F5A463F),
                              blurRadius: 22,
                              offset: Offset(0, 8),
                            ),
                          ],
                        ),
                        child: Row(
                          children: [
                            Container(
                              width: 44,
                              height: 44,
                              decoration: const BoxDecoration(
                                color: Color(0x26C98C7B),
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(Icons.bookmark, color: accent),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    item.topicName.isEmpty
                                        ? 'Cộng đồng'
                                        : item.topicName,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                      color: accentText,
                                      fontWeight: FontWeight.w700,
                                      fontSize: 13,
                                    ),
                                  ),
                                  const SizedBox(height: 5),
                                  Text(
                                    item.title,
                                    style: const TextStyle(
                                      color: primaryText,
                                      fontSize: 16,
                                      fontWeight: FontWeight.w800,
                                      height: 1.3,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 8),
                            const Icon(
                              Icons.chevron_right_rounded,
                              color: secondaryText,
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
    );
  }
}
