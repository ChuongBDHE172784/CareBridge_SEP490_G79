import 'package:flutter/material.dart';
import '../../../core/constants/content_stages.dart';
import '../../../core/network/api_client.dart';

class ViewContentScreen extends StatefulWidget {
  const ViewContentScreen({super.key});

  @override
  State<ViewContentScreen> createState() => _ViewContentScreenState();
}

class _ViewContentScreenState extends State<ViewContentScreen>
    with SingleTickerProviderStateMixin {
  static const _primary = Color(0xFFC98C7B);

  late TabController _tabCtrl;
  String _stage = 'PREGNANCY';

  List<dynamic> _articles = [];
  List<dynamic> _checklists = [];
  bool _loadingArticles = false;
  bool _loadingChecklists = false;
  String? _articlesError;
  String? _checklistsError;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _loadAll();
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    _loadArticles();
    _loadChecklists();
  }

  Future<void> _loadArticles() async {
    setState(() {
      _loadingArticles = true;
      _articlesError = null;
    });
    try {
      final data = await apiGet(
        '/api/v1/content?stage=$_stage&type=ARTICLE&page=0&size=20',
      );
      setState(() {
        _articles = (data['data'] as List? ?? []);
        _loadingArticles = false;
      });
    } catch (e) {
      setState(() {
        _articlesError = e.toString();
        _loadingArticles = false;
      });
    }
  }

  Future<void> _loadChecklists() async {
    setState(() {
      _loadingChecklists = true;
      _checklistsError = null;
    });
    try {
      final data = await apiGet('/api/v1/content/checklists?stage=$_stage');
      setState(() {
        _checklists = (data['data'] as List? ?? []);
        _loadingChecklists = false;
      });
    } catch (e) {
      setState(() {
        _checklistsError = e.toString();
        _loadingChecklists = false;
      });
    }
  }

  void _onStageChanged(String? stage) {
    if (stage == null || stage == _stage) return;
    setState(() => _stage = stage);
    _loadAll();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        title: const Text(
          'Kiến thức sức khoẻ',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        elevation: 0,
        bottom: TabBar(
          controller: _tabCtrl,
          indicatorColor: Colors.white,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          tabs: const [
            Tab(text: 'Bài viết'),
            Tab(text: 'Danh sách cần làm'),
          ],
        ),
      ),
      body: Column(
        children: [
          Container(
            color: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            child: Row(
              children: [
                const Text(
                  'Giai đoạn: ',
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: DropdownButtonFormField<String>(
                    initialValue: _stage,
                    decoration: InputDecoration(
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                        borderSide: const BorderSide(color: Color(0xFFE0E0E0)),
                      ),
                    ),
                    items: contentStageOptions
                        .map(
                          (stage) => DropdownMenuItem(
                            value: stage.value,
                            child: Text(stage.label),
                          ),
                        )
                        .toList(growable: false),
                    onChanged: _onStageChanged,
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: TabBarView(
              controller: _tabCtrl,
              children: [
                _ArticlesTab(
                  articles: _articles,
                  loading: _loadingArticles,
                  error: _articlesError,
                  onRetry: _loadArticles,
                ),
                _ChecklistsTab(
                  checklists: _checklists,
                  loading: _loadingChecklists,
                  error: _checklistsError,
                  onRetry: _loadChecklists,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ArticlesTab extends StatelessWidget {
  final List<dynamic> articles;
  final bool loading;
  final String? error;
  final VoidCallback onRetry;

  const _ArticlesTab({
    required this.articles,
    required this.loading,
    required this.error,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const Center(
        child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
      );
    }
    if (error != null) return _retryView(error!, onRetry);
    if (articles.isEmpty) {
      return const Center(
        child: Text(
          'Không tìm thấy bài viết nào',
          style: TextStyle(color: Colors.grey),
        ),
      );
    }
    return RefreshIndicator(
      color: const Color(0xFFC98C7B),
      onRefresh: () async => onRetry(),
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: articles.length,
        itemBuilder: (ctx, i) {
          final a = articles[i];
          return Card(
            margin: const EdgeInsets.only(bottom: 10),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            child: ListTile(
              contentPadding: const EdgeInsets.all(14),
              title: Text(
                a['title'] ?? '',
                style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 14,
                ),
              ),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(height: 4),
                  if (a['summary'] != null)
                    Text(
                      a['summary'],
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12,
                        color: Colors.black54,
                      ),
                    ),
                  const SizedBox(height: 6),
                  Text(
                    a['publishedAt'] ?? '',
                    style: const TextStyle(fontSize: 11, color: Colors.grey),
                  ),
                ],
              ),
              trailing: const Icon(
                Icons.article_outlined,
                color: Color(0xFFC98C7B),
              ),
            ),
          );
        },
      ),
    );
  }
}

class _ChecklistsTab extends StatelessWidget {
  final List<dynamic> checklists;
  final bool loading;
  final String? error;
  final VoidCallback onRetry;

  const _ChecklistsTab({
    required this.checklists,
    required this.loading,
    required this.error,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const Center(
        child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
      );
    }
    if (error != null) return _retryView(error!, onRetry);
    if (checklists.isEmpty) {
      return const Center(
        child: Text(
          'Không tìm thấy danh sách nào',
          style: TextStyle(color: Colors.grey),
        ),
      );
    }
    return RefreshIndicator(
      color: const Color(0xFFC98C7B),
      onRefresh: () async => onRetry(),
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: checklists.length,
        itemBuilder: (ctx, i) {
          final cl = checklists[i];
          final items = (cl['items'] ?? []) as List;
          return Card(
            margin: const EdgeInsets.only(bottom: 10),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            child: ExpansionTile(
              iconColor: const Color(0xFFC98C7B),
              collapsedIconColor: const Color(0xFFD4A895),
              title: Row(
                children: [
                  Icon(
                    _getChecklistIconData(cl['targetSubject'], cl['stage'], cl['title'] ?? cl['name']),
                    color: const Color(0xFFC98C7B),
                    size: 20,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      cl['title'] ?? cl['name'] ?? '',
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 14,
                      ),
                    ),
                  ),
                ],
              ),
              subtitle: Text(
                '${items.length} mục',
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
              children: items
                  .map<Widget>(
                    (item) => ListTile(
                      dense: true,
                      leading: const Icon(
                        Icons.check_circle_outline,
                        color: Color(0xFFD4A895),
                        size: 18,
                      ),
                      title: Text(
                        item['content'] ?? item['title'] ?? '',
                        style: const TextStyle(fontSize: 13),
                      ),
                    ),
                  )
                  .toList(),
            ),
          );
        },
      ),
    );
  }

  IconData _getChecklistIconData(dynamic targetSubject, dynamic stage, dynamic title) {
    final nameLower = (title?.toString() ?? '').toLowerCase();
    if (targetSubject?.toString() == 'BABY' ||
        stage?.toString() == 'BABY_CARE' ||
        stage?.toString() == 'INFANT' ||
        nameLower.contains('bé') ||
        nameLower.contains('trẻ') ||
        nameLower.contains('sơ sinh')) {
      return Icons.child_care_rounded;
    }
    return Icons.pregnant_woman_rounded;
  }
}

Widget _retryView(String message, VoidCallback onRetry) {
  return Center(
    child: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Icon(Icons.error_outline, size: 40, color: Color(0xFFC98C7B)),
        const SizedBox(height: 12),
        Text(
          message,
          textAlign: TextAlign.center,
          style: const TextStyle(color: Colors.black54),
        ),
        const SizedBox(height: 12),
        ElevatedButton(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFFC98C7B),
          ),
          onPressed: onRetry,
          child: const Text(
            'Thử lại',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
          ),
        ),
      ],
    ),
  );
}
