import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class SearchContentScreen extends StatefulWidget {
  const SearchContentScreen({super.key});

  @override
  State<SearchContentScreen> createState() => _SearchContentScreenState();
}

class _SearchContentScreenState extends State<SearchContentScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _accent = Color(0xFFD4A895);

  final _searchCtrl = TextEditingController();
  Timer? _debounce;

  List<dynamic> _results = [];
  String? _type;
  String? _stage;
  bool _loading = false;
  bool _searched = false;

  @override
  void dispose() {
    _searchCtrl.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _onSearchChanged(String _) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), _search);
  }

  Future<void> _search() async {
    setState(() { _loading = true; _searched = true; });
    try {
      final params = StringBuffer('/api/v1/content/search?');
      final kw = _searchCtrl.text.trim();
      if (kw.isNotEmpty) params.write('keyword=${Uri.encodeComponent(kw)}&');
      if (_type != null) params.write('type=$_type&');
      if (_stage != null) params.write('stage=$_stage&');
      params.write('page=0&size=20');

      final data = await apiGet(params.toString());
      setState(() {
        _results = (data['data'] as List? ?? []);
        _loading = false;
      });
    } catch (_) {
      setState(() { _results = []; _loading = false; });
    }
  }

  static const _typeColors = {
    'ARTICLE': Color(0xFF1976D2),
    'FAQ': Color(0xFF388E3C),
    'CHECKLIST': Color(0xFFC98C7B),
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        title: const Text('Tìm kiếm nội dung', style: TextStyle(fontWeight: FontWeight.bold)),
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
                hintText: 'Tìm kiếm bài viết, hỏi đáp, danh sách...',
                prefixIcon: const Icon(Icons.search, color: Colors.grey),
                suffixIcon: _searchCtrl.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear, color: Colors.grey),
                        onPressed: () {
                          _searchCtrl.clear();
                          setState(() { _results = []; _searched = false; });
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
                    initialValue: _type,
                    decoration: InputDecoration(
                      labelText: 'Loại',
                      labelStyle: const TextStyle(color: _accent, fontSize: 12),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    items: const [
                      DropdownMenuItem(value: null, child: Text('Tất cả loại', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'ARTICLE', child: Text('Bài viết', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'FAQ', child: Text('Hỏi đáp', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'CHECKLIST', child: Text('Danh sách', style: TextStyle(fontSize: 13))),
                    ],
                    onChanged: (v) { setState(() => _type = v); _search(); },
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: DropdownButtonFormField<String>(
                    initialValue: _stage,
                    decoration: InputDecoration(
                      labelText: 'Giai đoạn',
                      labelStyle: const TextStyle(color: _accent, fontSize: 12),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    items: const [
                      DropdownMenuItem(value: null, child: Text('Tất cả giai đoạn', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'PREGNANCY', child: Text('Thai kỳ', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'POSTPARTUM', child: Text('Sau sinh', style: TextStyle(fontSize: 13))),
                      DropdownMenuItem(value: 'BABY_CARE', child: Text('Chăm sóc bé', style: TextStyle(fontSize: 13))),
                    ],
                    onChanged: (v) { setState(() => _stage = v); _search(); },
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator(color: _primary))
                : !_searched
                    ? Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: const [
                            Icon(Icons.search, size: 64, color: Color(0xFFFFCCB3)),
                            SizedBox(height: 12),
                            Text('Tìm kiếm nội dung sức khoẻ', style: TextStyle(color: Colors.grey)),
                          ],
                        ),
                      )
                    : _results.isEmpty
                        ? const Center(child: Text('Không tìm thấy kết quả nào', style: TextStyle(color: Colors.grey)))
                        : ListView.builder(
                            padding: const EdgeInsets.all(12),
                            itemCount: _results.length,
                            itemBuilder: (ctx, i) {
                              final r = _results[i];
                              final typeColor = _typeColors[r['type']] ?? _primary;
                              return Card(
                                margin: const EdgeInsets.only(bottom: 8),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
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
                                              color: typeColor.withValues(alpha: 0.12),
                                              borderRadius: BorderRadius.circular(6),
                                            ),
                                            child: Text(r['type'] ?? '',
                                                style: TextStyle(color: typeColor, fontSize: 11, fontWeight: FontWeight.w600)),
                                          ),
                                          const SizedBox(width: 8),
                                          if (r['stage'] != null)
                                            Container(
                                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                              decoration: BoxDecoration(
                                                color: _accent.withValues(alpha: 0.12),
                                                borderRadius: BorderRadius.circular(6),
                                              ),
                                              child: Text(r['stage'],
                                                  style: const TextStyle(color: _accent, fontSize: 11, fontWeight: FontWeight.w600)),
                                            ),
                                        ],
                                      ),
                                      const SizedBox(height: 8),
                                      Text(r['title'] ?? '',
                                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis),
                                      if (r['publishedAt'] != null) ...[
                                        const SizedBox(height: 4),
                                        Text(r['publishedAt'],
                                            style: const TextStyle(fontSize: 11, color: Colors.grey)),
                                      ],
                                    ],
                                  ),
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
