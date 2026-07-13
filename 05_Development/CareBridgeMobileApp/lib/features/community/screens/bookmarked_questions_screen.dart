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
    if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 160)
      _loadMore();
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _page = 0;
      _hasMore = true;
    });
    try {
      final page = await CommunityService.instance.getBookmarks(page: 0);
      if (mounted)
        setState(() {
          _items
            ..clear()
            ..addAll(page);
          _page = 1;
          _hasMore = page.length >= 20;
        });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadMore() async {
    if (_loading || _loadingMore || !_hasMore) return;
    setState(() => _loadingMore = true);
    try {
      final page = await CommunityService.instance.getBookmarks(page: _page);
      if (mounted)
        setState(() {
          _items.addAll(page);
          _page++;
          _hasMore = page.length >= 20;
        });
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Bài viết đã lưu')),
    body: _loading
        ? const Center(child: CircularProgressIndicator())
        : _items.isEmpty
        ? const Center(child: Text('Chưa có bài viết đã lưu.'))
        : RefreshIndicator(
            onRefresh: _reload,
            child: ListView.separated(
              controller: _scroll,
              itemCount: _items.length + (_loadingMore ? 1 : 0),
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, index) {
                if (index == _items.length)
                  return const Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(child: CircularProgressIndicator()),
                  );
                final item = _items[index];
                return ListTile(
                  title: Text(item.title),
                  subtitle: Text(item.topicName),
                  trailing: const Icon(Icons.bookmark),
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => QuestionDetailScreen(questionId: item.id),
                    ),
                  ),
                );
              },
            ),
          ),
  );
}
