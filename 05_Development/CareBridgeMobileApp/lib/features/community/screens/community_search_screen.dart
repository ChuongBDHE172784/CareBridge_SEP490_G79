import 'dart:async';
import 'package:flutter/material.dart';
import '../services/community_service.dart';
import '../models/community_model.dart';
import 'create_question_screen.dart';
import 'question_detail_screen.dart';
import 'topic_directory_screen.dart';

class CommunitySearchScreen extends StatefulWidget {
  const CommunitySearchScreen({super.key});

  @override
  State<CommunitySearchScreen> createState() => _CommunitySearchScreenState();
}

class _CommunitySearchScreenState extends State<CommunitySearchScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surfaceContainerLowest = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);

  static const _stageFilters = [
    'Tất cả',
    'Mang thai',
    'Sau sinh',
    'Chăm sóc bé',
  ];

  final _searchCtrl = TextEditingController();
  Timer? _debounce;

  int _selectedStage = 0;
  List<CommunityFeedItem> _results = [];
  bool _loading = false;
  bool _hasSearched = false;
  int _page = 0;
  bool _hasMore = true;
  bool _verifiedOnly = false;
  final _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _search(refresh: true);
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
      _search();
    }
  }

  void _onSearchChanged(String v) {
    _debounce?.cancel();
    _debounce = Timer(
      const Duration(milliseconds: 400),
      () => _search(refresh: true),
    );
  }

  Future<void> _search({bool refresh = false}) async {
    if (refresh) {
      _page = 0;
      _hasMore = true;
    }
    if (!_hasMore || _loading) return;
    setState(() => _loading = true);
    try {
      final stage = switch (_selectedStage) {
        1 => 'PREGNANCY',
        2 => 'POSTPARTUM',
        3 => 'BABY_CARE',
        _ => null,
      };
      final items = await CommunityService.instance.searchQuestions(
        keyword: _searchCtrl.text.trim().isEmpty
            ? null
            : _searchCtrl.text.trim(),
        stage: stage,
        hasExpertAnswer: _verifiedOnly ? true : null,
        page: _page,
      );
      if (mounted) {
        setState(() {
          if (refresh) {
            _results = items;
          } else {
            _results.addAll(items);
          }
          _hasMore = items.length >= 20;
          _page++;
          _hasSearched = true;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _hasSearched = true);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: CustomScrollView(
        controller: _scroll,
        slivers: [
          SliverAppBar(
            backgroundColor: _canvas.withValues(alpha: 0.9),
            pinned: true,
            floating: true,
            elevation: 0,
            leading: IconButton(
              icon: const Icon(Icons.arrow_back, color: _primary),
              onPressed: () => Navigator.of(context).pop(),
            ),
            title: const Text(
              'Tìm kiếm cộng đồng',
              style: TextStyle(
                color: _primary,
                fontWeight: FontWeight.bold,
                fontSize: 17,
              ),
            ),
            bottom: PreferredSize(
              preferredSize: const Size.fromHeight(112),
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
                    child: Container(
                      decoration: BoxDecoration(
                        color: _surfaceContainerLowest,
                        borderRadius: BorderRadius.circular(99),
                        border: Border.all(color: _outlineVariant),
                      ),
                      child: Row(
                        children: [
                          const SizedBox(width: 12),
                          const Icon(Icons.search, color: _outline, size: 20),
                          const SizedBox(width: 8),
                          Expanded(
                            child: TextField(
                              controller: _searchCtrl,
                              onChanged: _onSearchChanged,
                              decoration: const InputDecoration(
                                hintText:
                                    'Tìm câu hỏi, chủ đề hoặc chuyên gia...',
                                hintStyle: TextStyle(
                                  color: _outline,
                                  fontSize: 14,
                                ),
                                border: InputBorder.none,
                                contentPadding: EdgeInsets.symmetric(
                                  vertical: 12,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          const Icon(Icons.tune, color: _outline, size: 20),
                          const SizedBox(width: 12),
                        ],
                      ),
                    ),
                  ),
                  // Filter chips
                  SizedBox(
                    height: 44,
                    child: ListView(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(horizontal: 20),
                      children: [
                        for (var i = 0; i < _stageFilters.length; i++) ...[
                          _buildFilterChip(
                            _stageFilters[i],
                            i,
                            icon: i == 0 ? Icons.tune : null,
                          ),
                          const SizedBox(width: 8),
                        ],
                        const SizedBox(width: 8),
                        _buildVerifiedChip('Chuyên gia'),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          // Results count
          if (_hasSearched)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 4),
                child: Text(
                  'Kết quả tìm kiếm (${_results.length})',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: _primary,
                  ),
                ),
              ),
            ),
          // Question result cards
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            sliver: SliverList(
              delegate: SliverChildBuilderDelegate((context, i) {
                // Insert bento section after 2nd card
                if (i == 2) return _buildBentoSection();
                final idx = i > 2 ? i - 1 : i;
                if (idx >= _results.length) return null;
                final q = _results[idx];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: GestureDetector(
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => QuestionDetailScreen(questionId: q.id),
                      ),
                    ),
                    child: _QuestionResultCard(item: q),
                  ),
                );
              }, childCount: _results.isEmpty ? 0 : _results.length + 1),
            ),
          ),
          // Loading indicator
          if (_loading)
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: Center(
                  child: CircularProgressIndicator(color: _primary),
                ),
              ),
            ),
          // Expert recommendations
          if (!_loading && _results.isNotEmpty) ...[
            const SliverToBoxAdapter(child: SizedBox(height: 20)),
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.only(left: 20, bottom: 12),
                child: Text(
                  'Chuyên gia được đề xuất',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: _primary,
                  ),
                ),
              ),
            ),
            SliverToBoxAdapter(child: _buildExpertSection()),
          ],
          const SliverToBoxAdapter(child: SizedBox(height: 80)),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        onPressed: () => Navigator.of(
          context,
        ).push(MaterialPageRoute(builder: (_) => const CreateQuestionScreen())),
        child: const Icon(Icons.add_comment),
      ),
    );
  }

  Widget _buildFilterChip(String label, int index, {IconData? icon}) {
    final selected = _selectedStage == index;
    return GestureDetector(
      onTap: () {
        setState(() => _selectedStage = index);
        _search(refresh: true);
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? _primary : _surfaceContainerLowest,
          borderRadius: BorderRadius.circular(99),
          border: selected ? null : Border.all(color: _outlineVariant),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(
                icon,
                size: 16,
                color: selected ? Colors.white : _onSurfaceVariant,
              ),
              const SizedBox(width: 4),
            ],
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                color: selected ? Colors.white : _onSurfaceVariant,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildVerifiedChip(String label) {
    return GestureDetector(
      onTap: () {
        setState(() => _verifiedOnly = !_verifiedOnly);
        _search(refresh: true);
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: _verifiedOnly ? _primary : _surfaceContainerLowest,
          borderRadius: BorderRadius.circular(99),
          border: _verifiedOnly ? null : Border.all(color: _outlineVariant),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                color: _verifiedOnly ? Colors.white : _onSurfaceVariant,
              ),
            ),
            const SizedBox(width: 4),
            Icon(
              Icons.verified,
              size: 14,
              color: _verifiedOnly ? Colors.white : _onSurfaceVariant,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBentoSection() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        children: [
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _primaryContainer,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.live_help, color: Colors.white, size: 28),
                  const SizedBox(height: 8),
                  const Text(
                    'Chưa tìm thấy câu trả lời?',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    'Đặt câu hỏi cho cộng đồng và nhận phản hồi từ chuyên gia.',
                    style: TextStyle(fontSize: 11, color: Colors.white70),
                  ),
                  const SizedBox(height: 12),
                  GestureDetector(
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => const CreateQuestionScreen(),
                      ),
                    ),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 7,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFF51271B),
                        borderRadius: BorderRadius.circular(99),
                      ),
                      child: const Text(
                        'Đặt câu hỏi',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: _primaryContainer,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _surfaceContainerLowest,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: _primaryContainer.withValues(alpha: 0.15),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: const BoxDecoration(
                      color: Color(0xFFF6DACF),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.groups, color: _primary, size: 20),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Tham gia nhóm',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    'Kết nối với các mẹ có cùng giai đoạn phát triển.',
                    style: TextStyle(fontSize: 11, color: _onSurfaceVariant),
                  ),
                  const SizedBox(height: 12),
                  GestureDetector(
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => const TopicDirectoryScreen(),
                      ),
                    ),
                    child: Row(
                      children: const [
                        Text(
                          'Khám phá',
                          style: TextStyle(
                            fontSize: 12,
                            color: _primary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Icon(Icons.chevron_right, size: 14, color: _primary),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildExpertSection() {
    // TODO: wire to expert recommendation API when available
    final experts = [
      {'name': 'BS. Minh Anh', 'spec': 'Chuyên khoa Nhi'},
      {'name': 'ThS. Lan Phương', 'spec': 'Dinh dưỡng Mẹ & Bé'},
      {'name': 'Chuyên gia Hà Lê', 'spec': 'Tâm lý Sơ sinh'},
    ];
    return SizedBox(
      height: 160,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 20),
        itemCount: experts.length,
        separatorBuilder: (_, _) => const SizedBox(width: 12),
        itemBuilder: (_, i) => Container(
          width: 140,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: const Color(0xFFFFF1EC),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            children: [
              CircleAvatar(
                radius: 28,
                backgroundColor: const Color(0xFFFFE9E3),
                child: const Icon(
                  Icons.person,
                  color: _primaryContainer,
                  size: 28,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                experts[i]['name']!,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: _onSurface,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 2),
              Text(
                experts[i]['spec']!,
                style: const TextStyle(fontSize: 10, color: _onSurfaceVariant),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _primary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 5),
                    shape: const StadiumBorder(),
                    textStyle: const TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                    ),
                    minimumSize: Size.zero,
                  ),
                  onPressed: () {},
                  child: const Text('Nhắn tin'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _QuestionResultCard extends StatelessWidget {
  final CommunityFeedItem item;

  const _QuestionResultCard({required this.item});

  @override
  Widget build(BuildContext context) {
    final hasExpert = item.hasExpertAnswer;
    return Container(
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
            children: [
              Wrap(
                spacing: 6,
                children: [
                  if (item.topicName.isNotEmpty) _TagBadge(item.topicName),
                  if (item.stage.isNotEmpty) _TagBadge(_stageLabel(item.stage)),
                ],
              ),
              const Spacer(),
              const Icon(
                Icons.bookmark_border,
                color: Color(0xFF84736F),
                size: 20,
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            item.title,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w600,
              color: Color(0xFF271812),
              height: 1.3,
            ),
          ),
          const SizedBox(height: 10),
          const Divider(color: Color(0xFFFADCD3), height: 1),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: hasExpert
                            ? const Color(0xFF845143).withValues(alpha: 0.1)
                            : const Color(0xFFFFE2D9).withValues(alpha: 0.5),
                        borderRadius: BorderRadius.circular(99),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            hasExpert ? Icons.verified : Icons.forum,
                            size: 14,
                            color: hasExpert
                                ? const Color(0xFF845143)
                                : const Color(0xFF524440),
                          ),
                          const SizedBox(width: 4),
                          Text(
                            hasExpert
                                ? 'Bác sĩ chuyên gia'
                                : 'Thảo luận sôi nổi',
                            style: TextStyle(
                              fontSize: 11,
                              color: hasExpert
                                  ? const Color(0xFF845143)
                                  : const Color(0xFF524440),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      '• ${item.answerCount} câu trả lời',
                      style: const TextStyle(
                        fontSize: 11,
                        color: Color(0xFF524440),
                      ),
                    ),
                  ],
                ),
              ),
              // Avatar stack placeholder
              Row(
                children: List.generate(
                  item.answerCount > 0 ? (item.answerCount > 3 ? 2 : 1) : 0,
                  (i) => Align(
                    widthFactor: 0.7,
                    child: CircleAvatar(
                      radius: 12,
                      backgroundColor: const Color(0xFFFFE9E3),
                      child: const Icon(
                        Icons.person,
                        size: 12,
                        color: Color(0xFFC98C7B),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  String _stageLabel(String stage) {
    switch (stage) {
      case 'NEWBORN':
        return 'Sơ sinh';
      case 'PRE_PREGNANCY':
        return 'Chuẩn bị mang thai';
      case 'PREGNANCY':
        return 'Mang thai';
      case 'POSTPARTUM':
        return 'Sau sinh';
      case 'BABY_CARE':
        return 'Chăm sóc bé';
      default:
        return stage;
    }
  }
}

class _TagBadge extends StatelessWidget {
  final String label;
  const _TagBadge(this.label);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE2D9),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontSize: 11,
          color: Color(0xFF845143),
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}
