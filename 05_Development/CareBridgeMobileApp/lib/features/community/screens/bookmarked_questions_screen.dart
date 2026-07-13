import 'package:flutter/material.dart';
import '../models/community_model.dart';
import '../services/community_service.dart';
import 'question_detail_screen.dart';

class BookmarkedQuestionsScreen extends StatefulWidget {
  const BookmarkedQuestionsScreen({super.key});
  @override State<BookmarkedQuestionsScreen> createState() => _BookmarkedQuestionsScreenState();
}

class _BookmarkedQuestionsScreenState extends State<BookmarkedQuestionsScreen> {
  late Future<List<CommunityFeedItem>> _items;
  @override void initState() { super.initState(); _items = CommunityService.instance.getBookmarks(); }
  void _reload() => setState(() => _items = CommunityService.instance.getBookmarks());
  @override Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Bài viết đã lưu')),
    body: FutureBuilder<List<CommunityFeedItem>>(
      future: _items,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
        if (snapshot.hasError) return Center(child: TextButton(onPressed: _reload, child: const Text('Không thể tải. Thử lại')));
        final items = snapshot.data ?? [];
        if (items.isEmpty) return const Center(child: Text('Chưa có bài viết đã lưu.'));
        return RefreshIndicator(onRefresh: () async => _reload(), child: ListView.separated(
          itemCount: items.length, separatorBuilder: (_, __) => const Divider(height: 1),
          itemBuilder: (context, index) { final item = items[index]; return ListTile(
            title: Text(item.title), subtitle: Text(item.topicName), trailing: const Icon(Icons.bookmark),
            onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => QuestionDetailScreen(questionId: item.id))),
          ); },
        ));
      },
    ),
  );
}
