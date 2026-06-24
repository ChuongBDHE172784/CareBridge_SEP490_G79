import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class SearchQuestionsScreen extends StatefulWidget {
  const SearchQuestionsScreen({super.key});

  @override
  State<SearchQuestionsScreen> createState() => _SearchQuestionsScreenState();
}

class _SearchQuestionsScreenState extends State<SearchQuestionsScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _accent = Color(0xFFD4A895);

  final _searchCtrl = TextEditingController();
  Timer? _debounce;

  List<dynamic> _results = [];
  List<dynamic> _topics = [];
  String? _selectedTopicId;
  String? _selectedStage;
  bool _loading = false;
  bool _hasMore = true;
  int _page = 0;
  final ScrollController _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _loadTopics();
    _search(refresh: true);
    _scroll.addListener(_onScroll);
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
        !_loading && _hasMore) {
      _search();
    }
  }

  void _onSearchChanged(String _) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), () => _search(refresh: true));
  }

  Future<void> _loadTopics() async {
    try {
      final data = await apiGet('/api/v1/community/topics');
      setState(() => _topics = (data['data'] as List? ?? []));
    } catch (_) {}
  }

  Future<void> _search({bool refresh = false}) async {
    if (_loading) return;
    setState(() {
      _loading = true;
      if (refresh) {
        _page = 0;
        _hasMore = true;
      }
    });
    try {
      final params = StringBuffer('/api/v1/community/questions?page=$_page&size=20');
      final kw = _searchCtrl.text.trim();
      if (kw.isNotEmpty) params.write('&keyword=${Uri.encodeComponent(kw)}');
      if (_selectedTopicId != null) params.write('&topicId=$_selectedTopicId');
      if (_selectedStage != null) params.write('&stage=$_selectedStage');

      final data = await apiGet(params.toString());
      final list = (data['data'] as List? ?? []);
      setState(() {
        if (refresh) _results = list;
        else _results.addAll(list);
        _hasMore = list.length == 20;
        _page++;
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        title: const Text('Tìm câu hỏi', style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0,
      ),
      body: Column(
        children: [
          Container(
            color: _primary,
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
            child: TextField(
              controller: _searchCtrl,
              onChanged: _onSearchChanged,
              decoration: InputDecoration(
                hintText: 'Tìm kiếm câu hỏi...',
                prefixIcon: const Icon(Icons.search, color: Colors.grey),
                suffixIcon: _searchCtrl.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear, color: Colors.grey),
                        onPressed: () {
                          _searchCtrl.clear();
                          _search(refresh: true);
                        })
                    : null,
                filled: true,
                fillColor: Colors.white,
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: BorderSide.none,
                ),
              ),
              style: const TextStyle(color: Colors.black87),
            ),
          ),
          Container(
            color: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<String>(
                    value: _selectedTopicId,
                    decoration: InputDecoration(
                      labelText: 'Chủ đề',
                      labelStyle: const TextStyle(color: _accent, fontSize: 12),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    items: [
                      const DropdownMenuItem(value: null, child: Text('Tất cả', style: TextStyle(fontSize: 13))),
                      ..._topics.map((t) => DropdownMenuItem(
                          value: t['id'].toString(),
                          child: Text(t['name'] ?? '', style: const TextStyle(fontSize: 13)))),
                    ],
                    onChanged: (v) {
                      setState(() => _selectedTopicId = v);
                      _search(refresh: true);
                    },
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: DropdownButtonFormField<String>(
                    value: _selectedStage,
                    decoration: InputDecoration(
                      labelText: 'Giai đoạn',
                      labelStyle: const TextStyle(color: _accent, fontSize: 12),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    items: const [
                      DropdownMenuItem(value: null, child: Text('Tất cả', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'PREGNANCY', child: Text('Thai kỳ', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'POSTPARTUM', child: Text('Sau sinh', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'BABY_CARE', child: Text('Chăm sóc bé', style: TextStyle(fontSize: 13))),
                    ],
                    onChanged: (v) {
                      setState(() => _selectedStage = v);
                      _search(refresh: true);
                    },
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: _results.isEmpty && _loading
                ? const Center(child: CircularProgressIndicator(color: _primary))
                : _results.isEmpty
                    ? const Center(
                        child: Text('Không tìm thấy câu hỏi nào', style: TextStyle(color: Colors.grey)))
                    : ListView.builder(
                        controller: _scroll,
                        padding: const EdgeInsets.all(12),
                        itemCount: _results.length + (_hasMore ? 1 : 0),
                        itemBuilder: (ctx, i) {
                          if (i == _results.length) {
                            return const Padding(
                              padding: EdgeInsets.all(16),
                              child: Center(child: CircularProgressIndicator(color: _primary)),
                            );
                          }
                          final q = _results[i];
                          return Card(
                            margin: const EdgeInsets.only(bottom: 8),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                            child: ListTile(
                              title: Text(q['title'] ?? '',
                                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis),
                              subtitle: Row(
                                children: [
                                  if (q['stage'] != null)
                                    Container(
                                      margin: const EdgeInsets.only(top: 4),
                                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                      decoration: BoxDecoration(
                                        color: _accent.withOpacity(0.15),
                                        borderRadius: BorderRadius.circular(4),
                                      ),
                                      child: Text(q['stage'],
                                          style: const TextStyle(color: _accent, fontSize: 11)),
                                    ),
                                  const SizedBox(width: 8),
                                  Text('${q['answerCount'] ?? 0} câu trả lời',
                                      style: const TextStyle(fontSize: 12, color: Colors.grey)),
                                ],
                              ),
                              trailing: const Icon(Icons.chevron_right, color: _primary),
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
