import 'dart:async';
import 'package:flutter/material.dart';
import '../services/content_service.dart';
import '../models/content_model.dart';
import 'verified_content_detail_screen.dart';

class VerifiedContentSearchScreen extends StatefulWidget {
  const VerifiedContentSearchScreen({super.key});

  @override
  State<VerifiedContentSearchScreen> createState() =>
      _VerifiedContentSearchScreenState();
}

class _VerifiedContentSearchScreenState
    extends State<VerifiedContentSearchScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);

  static const _ageFilters = ['Sơ sinh', '1-2 tuổi', '3-5 tuổi', '6+ tuổi'];
  static const _ageStages = ['NEWBORN', 'INFANT', 'TODDLER', 'PRESCHOOL'];
  static const _categoryFilters = [
    'Dinh dưỡng',
    'An toàn',
    'Phát triển',
    'Giấc ngủ',
  ];

  final _searchCtrl = TextEditingController();
  Timer? _debounce;

  int? _selectedAge;
  int? _selectedCategory;
  List<ContentListItem> _results = [];
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _search();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _onSearchChanged(String v) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), _search);
  }

  Future<void> _search() async {
    setState(() => _loading = true);
    try {
      final stage = _selectedAge != null ? _ageStages[_selectedAge!] : null;
      final keyword = _searchCtrl.text.trim();
      // The backend search endpoint requires a non-blank keyword — when the user
      // hasn't typed anything yet (e.g. on first open), browse via the plain list
      // endpoint instead so the screen never shows a false "no results" state.
      final results = keyword.isEmpty
          ? await ContentService.instance.getContent(stage: stage, size: 20)
          : await ContentService.instance.searchContent(keyword, stage: stage);
      if (mounted) setState(() => _results = results);
    } catch (_) {
      if (mounted) setState(() => _results = []);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 1,
        shadowColor: Colors.black12,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'CareBridge',
          style: TextStyle(
            color: _primary,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.close, color: _onSurfaceVariant),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Tìm kiếm nội dung',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF271812),
                  ),
                ),
                const SizedBox(height: 12),
                // Search bar
                Container(
                  decoration: BoxDecoration(
                    color: _surface,
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.06),
                        blurRadius: 16,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: TextField(
                    controller: _searchCtrl,
                    onChanged: _onSearchChanged,
                    decoration: const InputDecoration(
                      prefixIcon: Icon(Icons.search, color: _outline),
                      hintText: 'Tìm kiếm bài viết, hướng dẫn...',
                      hintStyle: TextStyle(color: _outline, fontSize: 15),
                      border: InputBorder.none,
                      contentPadding: EdgeInsets.symmetric(vertical: 14),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                // Age filter row
                SizedBox(
                  height: 36,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: _ageFilters.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 8),
                    itemBuilder: (_, i) => _FilterChip(
                      label: _ageFilters[i],
                      selected: _selectedAge == i,
                      onTap: () {
                        setState(
                          () => _selectedAge = _selectedAge == i ? null : i,
                        );
                        _search();
                      },
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                // Category filter row
                SizedBox(
                  height: 36,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: _categoryFilters.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 8),
                    itemBuilder: (_, i) => _FilterChip(
                      label: _categoryFilters[i],
                      selected: _selectedCategory == i,
                      onTap: () {
                        setState(
                          () => _selectedCategory = _selectedCategory == i
                              ? null
                              : i,
                        );
                        _search();
                      },
                    ),
                  ),
                ),
                const SizedBox(height: 16),
              ],
            ),
          ),
          // Results
          Expanded(
            child: _loading
                ? const Center(
                    child: CircularProgressIndicator(color: _primary),
                  )
                : _results.isEmpty
                ? _buildEmptyState()
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                    itemCount: _results.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (_, i) => _ContentCard(item: _results[i]),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.search_off,
            size: 56,
            color: _primaryContainer.withValues(alpha: 0.5),
          ),
          const SizedBox(height: 12),
          const Text(
            'Không tìm thấy nội dung',
            style: TextStyle(fontSize: 15, color: _onSurfaceVariant),
          ),
          const SizedBox(height: 4),
          const Text(
            'Thử từ khóa khác hoặc bỏ bộ lọc',
            style: TextStyle(fontSize: 13, color: _outline),
          ),
        ],
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFC98C7B) : const Color(0xFFFFE2D9),
          borderRadius: BorderRadius.circular(99),
          boxShadow: [
            const BoxShadow(
              color: Colors.black12,
              blurRadius: 4,
              offset: Offset(0, 2),
            ),
          ],
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w500,
            color: selected ? Colors.white : const Color(0xFF271812),
          ),
        ),
      ),
    );
  }
}

class _ContentCard extends StatelessWidget {
  final ContentListItem item;

  const _ContentCard({required this.item});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => VerifiedContentDetailScreen(contentId: item.id),
        ),
      ),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.06),
              blurRadius: 16,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 6,
                              vertical: 2,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(
                                0xFF845143,
                              ).withValues(alpha: 0.1),
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: const Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(
                                  Icons.verified,
                                  size: 12,
                                  color: Color(0xFF845143),
                                ),
                                SizedBox(width: 4),
                                Text(
                                  'Nguồn tin cậy',
                                  style: TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.w700,
                                    color: Color(0xFF845143),
                                    letterSpacing: 0.5,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(width: 8),
                          if (item.publishedAt != null)
                            Text(
                              _formatDate(item.publishedAt!),
                              style: const TextStyle(
                                fontSize: 11,
                                color: Color(0xFF84736F),
                              ),
                            ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(
                        item.title,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF271812),
                          height: 1.3,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                const Icon(Icons.bookmark_border, color: Color(0xFF84736F)),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                if (item.stage.isNotEmpty)
                  _TagChip(label: _stageLabel(item.stage)),
                if (item.type.isNotEmpty) ...[
                  const SizedBox(width: 6),
                  _TagChip(label: item.type),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatDate(String iso) {
    try {
      final dt = DateTime.parse(iso);
      final diff = DateTime.now().difference(dt);
      if (diff.inDays > 7) return '${diff.inDays} ngày trước';
      if (diff.inDays >= 1) return '${diff.inDays} ngày trước';
      if (diff.inHours >= 1) return '${diff.inHours} giờ trước';
      return 'Vừa xong';
    } catch (_) {
      return '';
    }
  }

  String _stageLabel(String stage) {
    switch (stage) {
      case 'NEWBORN':
        return 'Sơ sinh';
      case 'INFANT':
        return '1-2 tuổi';
      case 'TODDLER':
        return '3-5 tuổi';
      case 'PRESCHOOL':
        return '6+ tuổi';
      case 'PREGNANCY':
        return 'Thai kỳ';
      default:
        return stage;
    }
  }
}

class _TagChip extends StatelessWidget {
  final String label;
  const _TagChip({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE9E3),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Text(
        label,
        style: const TextStyle(fontSize: 11, color: Color(0xFF271812)),
      ),
    );
  }
}
