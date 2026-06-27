import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class CommunityFeedScreen extends StatefulWidget {
  const CommunityFeedScreen({super.key});

  @override
  State<CommunityFeedScreen> createState() => _CommunityFeedScreenState();
}

class _CommunityFeedScreenState extends State<CommunityFeedScreen> {
  static const _primary = Color(0xFFC98C7B);

  List<dynamic> _items = [];
  bool _loading = false;
  bool _hasMore = true;
  String? _error;
  int _page = 0;
  final ScrollController _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _loadFeed(refresh: true);
    _scroll.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 200 &&
        !_loading && _hasMore) {
      _loadFeed();
    }
  }

  Future<void> _loadFeed({bool refresh = false}) async {
    if (_loading) return;
    setState(() {
      _loading = true;
      if (refresh) {
        _page = 0;
        _hasMore = true;
        _error = null;
      }
    });
    try {
      final data = await apiGet('/api/v1/community/feed?page=$_page&size=20');
      final list = (data['data'] as List? ?? []);
      setState(() {
        if (refresh) {
          _items = list;
        } else {
          _items.addAll(list);
        }
        _hasMore = list.length == 20;
        _page++;
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        title: const Text('Bảng tin cộng đồng', style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0,
      ),
      body: _error != null && _items.isEmpty
          ? _ErrorView(message: _error!, onRetry: () => _loadFeed(refresh: true))
          : RefreshIndicator(
              color: _primary,
              onRefresh: () => _loadFeed(refresh: true),
              child: _items.isEmpty && _loading
                  ? const Center(child: CircularProgressIndicator(color: _primary))
                  : ListView.builder(
                      controller: _scroll,
                      padding: const EdgeInsets.all(12),
                      itemCount: _items.length + (_hasMore ? 1 : 0),
                      itemBuilder: (ctx, i) {
                        if (i == _items.length) {
                          return const Padding(
                            padding: EdgeInsets.all(16),
                            child: Center(child: CircularProgressIndicator(color: _primary)),
                          );
                        }
                        return _FeedCard(item: _items[i]);
                      },
                    ),
            ),
    );
  }
}

class _FeedCard extends StatelessWidget {
  final Map<String, dynamic> item;
  const _FeedCard({required this.item});

  static const _urgencyColors = {
    'EMERGENCY': Color(0xFFD32F2F),
    'HIGH': Color(0xFFC98C7B),
    'NORMAL': Color(0xFF9C857C),
  };

  @override
  Widget build(BuildContext context) {
    final urgency = item['urgency'] as String? ?? 'NORMAL';
    final urgencyColor = _urgencyColors[urgency] ?? const Color(0xFF757575);
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: urgencyColor.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(urgency,
                      style: TextStyle(color: urgencyColor, fontSize: 11, fontWeight: FontWeight.w600)),
                ),
                const SizedBox(width: 8),
                if (item['topicName'] != null)
                  Flexible(
                    child: Text('${item['topicName']}',
                        style: const TextStyle(color: Color(0xFFD4A895), fontSize: 12),
                        overflow: TextOverflow.ellipsis),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            Text(item['title'] ?? '',
                style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15),
                maxLines: 2,
                overflow: TextOverflow.ellipsis),
            const SizedBox(height: 6),
            Row(
              children: [
                const Icon(Icons.person_outline, size: 14, color: Color(0xFF9E9E9E)),
                const SizedBox(width: 4),
                Text(item['authorDisplay'] ?? 'Ẩn danh',
                    style: const TextStyle(color: Color(0xFF9E9E9E), fontSize: 12)),
                const Spacer(),
                const Icon(Icons.thumb_up_outlined, size: 14, color: Color(0xFFC98C7B)),
                const SizedBox(width: 4),
                Text('${item['likeCount'] ?? 0}',
                    style: const TextStyle(color: Color(0xFF757575), fontSize: 12)),
                const SizedBox(width: 12),
                const Icon(Icons.chat_bubble_outline, size: 14, color: Color(0xFFD4A895)),
                const SizedBox(width: 4),
                Text('${item['answerCount'] ?? 0}',
                    style: const TextStyle(color: Color(0xFF757575), fontSize: 12)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ErrorView extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const _ErrorView({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Color(0xFFC98C7B)),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center, style: const TextStyle(color: Colors.black54)),
            const SizedBox(height: 16),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFC98C7B)),
              onPressed: onRetry,
              child: const Text('Thử lại', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }
}
