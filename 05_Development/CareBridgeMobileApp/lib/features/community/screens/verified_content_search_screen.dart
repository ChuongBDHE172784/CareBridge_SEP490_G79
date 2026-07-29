import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/constants/content_stages.dart';
import '../services/content_service.dart';
import '../models/content_model.dart';
import 'verified_content_detail_screen.dart';

class VerifiedContentSearchScreen extends StatefulWidget {
  const VerifiedContentSearchScreen({super.key, this.contentService});

  final ContentService? contentService;

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
  static const _typeFilters = [
    ('ARTICLE', 'Bài viết', Icons.article_outlined),
    ('FAQ', 'Hỏi đáp', Icons.quiz_outlined),
    ('CHECKLIST', 'Danh sách', Icons.checklist_outlined),
  ];

  final _searchCtrl = TextEditingController();
  Timer? _debounce;
  late ContentService _contentService;
  int _requestGeneration = 0;
  String? _observedAccountId;

  int? _selectedAge;
  int? _selectedCategory;
  String? _selectedType;
  List<ContentListItem> _results = [];
  bool _loading = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _contentService = widget.contentService ?? ContentService.instance;
    _observedAccountId = AuthState.instance.userId;
    AuthState.instance.addListener(_onAccountChanged);
    _search();
  }

  @override
  void didUpdateWidget(covariant VerifiedContentSearchScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.contentService != widget.contentService) {
      _contentService = widget.contentService ?? ContentService.instance;
      _debounce?.cancel();
      _search(clearResults: true);
    }
  }

  @override
  void dispose() {
    _requestGeneration += 1;
    _debounce?.cancel();
    AuthState.instance.removeListener(_onAccountChanged);
    _searchCtrl.dispose();
    super.dispose();
  }

  void _onAccountChanged() {
    final accountId = AuthState.instance.userId;
    if (accountId == _observedAccountId) return;
    _observedAccountId = accountId;
    _debounce?.cancel();
    _search(clearResults: true);
  }

  void _onSearchChanged(String v) {
    _requestGeneration += 1;
    _debounce?.cancel();
    setState(() {
      _loading = true;
      _errorMessage = null;
    });
    _debounce = Timer(const Duration(milliseconds: 400), _search);
  }

  Future<void> _search({bool clearResults = false}) async {
    final requestGeneration = ++_requestGeneration;
    final accountId = AuthState.instance.userId;
    final selectedAge = _selectedAge;
    final selectedType = _selectedType;
    final keyword = _searchCtrl.text.trim();
    final stage = selectedAge != null ? _ageStages[selectedAge] : null;
    setState(() {
      _loading = true;
      _errorMessage = null;
      if (clearResults) _results = [];
    });
    try {
      // The backend search endpoint requires a non-blank keyword — when the user
      // hasn't typed anything yet (e.g. on first open), browse via the plain list
      // endpoint instead so the screen never shows a false "no results" state.
      final results = keyword.isEmpty
          ? await _contentService.getContent(
              type: selectedType,
              stage: stage,
              size: 20,
            )
          : await _contentService.searchContent(
              keyword,
              type: selectedType,
              stage: stage,
            );
      if (_canApply(
        requestGeneration,
        accountId,
        keyword,
        selectedAge,
        selectedType,
      )) {
        setState(() {
          _results = results;
          _errorMessage = null;
        });
      }
    } catch (_) {
      if (_canApply(
        requestGeneration,
        accountId,
        keyword,
        selectedAge,
        selectedType,
      )) {
        setState(() {
          _results = [];
          _errorMessage =
              'KhÃ´ng thá»ƒ táº£i ná»™i dung. Vui lÃ²ng thá»­ láº¡i.';
        });
      }
    } finally {
      if (_canApply(
        requestGeneration,
        accountId,
        keyword,
        selectedAge,
        selectedType,
      )) {
        setState(() => _loading = false);
      }
    }
  }

  bool _canApply(
    int requestGeneration,
    String? accountId,
    String keyword,
    int? selectedAge,
    String? selectedType,
  ) =>
      mounted &&
      requestGeneration == _requestGeneration &&
      AuthState.instance.userId == accountId &&
      _searchCtrl.text.trim() == keyword &&
      _selectedAge == selectedAge &&
      _selectedType == selectedType;

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
                    separatorBuilder: (_, _) => const SizedBox(width: 8),
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
                // CB-180_2 — content-format filters are part of the same
                // search flow, not a separate screen.
                SizedBox(
                  height: 40,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: _typeFilters.length,
                    separatorBuilder: (_, _) => const SizedBox(width: 8),
                    itemBuilder: (_, i) {
                      final filter = _typeFilters[i];
                      return _ContentTypeChip(
                        label: filter.$2,
                        icon: filter.$3,
                        selected: _selectedType == filter.$1,
                        onTap: () {
                          setState(
                            () => _selectedType = _selectedType == filter.$1
                                ? null
                                : filter.$1,
                          );
                          _search();
                        },
                      );
                    },
                  ),
                ),
                const SizedBox(height: 8),
                // Category filter row
                SizedBox(
                  height: 36,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    itemCount: _categoryFilters.length,
                    separatorBuilder: (_, _) => const SizedBox(width: 8),
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
                : _errorMessage != null
                ? _buildErrorState()
                : _results.isEmpty
                ? _buildEmptyState()
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                    itemCount: _results.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (_, i) => _ContentCard(item: _results[i]),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      key: const Key('generic-content-search-error'),
      child: Semantics(
        liveRegion: true,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_rounded, size: 56, color: _primary),
            const SizedBox(height: 12),
            Text(
              _errorMessage!,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 16, color: _onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              key: const Key('generic-content-search-retry'),
              onPressed: _search,
              icon: const Icon(Icons.refresh_rounded),
              label: const Text('Thá»­ láº¡i'),
              style: FilledButton.styleFrom(
                minimumSize: const Size(48, 48),
                backgroundColor: _primaryContainer,
                shape: const StadiumBorder(),
              ),
            ),
          ],
        ),
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

class _ContentTypeChip extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  const _ContentTypeChip({
    required this.label,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onTap,
      icon: Icon(icon, size: 18),
      label: Text(label),
      style: OutlinedButton.styleFrom(
        foregroundColor: selected ? Colors.white : const Color(0xFF524440),
        backgroundColor: selected ? const Color(0xFF845143) : Colors.white,
        side: const BorderSide(color: Color(0xFFD6C2BD)),
        shape: const StadiumBorder(),
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

  String _stageLabel(String stage) => contentStageLabel(stage);
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
