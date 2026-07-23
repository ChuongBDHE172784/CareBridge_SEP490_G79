import 'package:flutter/material.dart';
import '../models/community_model.dart';
import '../models/content_model.dart';
import '../services/community_service.dart';
import '../services/content_service.dart';
import '../widgets/verified_content_body.dart';
import 'verified_content_detail_screen.dart';

/// CB-223 — View Content and Checklist (UC-82)
/// Displays curated articles, FAQs, and checklists filtered by pregnancy
/// stage and topic. Navigated to via Navigator.push from Home or Community.
class ViewContentScreen extends StatefulWidget {
  const ViewContentScreen({super.key});

  @override
  State<ViewContentScreen> createState() => _ViewContentScreenState();
}

class _ViewContentScreenState extends State<ViewContentScreen> {
  // ── Design tokens (Warm Claymorphism palette) ──
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  // ignore: unused_field
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  // ignore: unused_field
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  // ignore: unused_field
  static const _error = Color(0xFFBA1A1A);

  final _contentService = ContentService.instance;
  final _communityService = CommunityService.instance;
  final _searchController = TextEditingController();

  // ── State ──
  bool _loading = true;
  String? _loadError;
  int _selectedTypeIndex = 0; // 0=All, 1=Article, 2=FAQ, 3=Checklist
  int _selectedStageIndex = 1; // 0=Prep, 1=Pregnancy, 2=Postpartum, 3=BabyCare
  List<CommunityTopic> _topics = [];
  List<ContentListItem> _articles = [];
  List<ContentListItem> _faqs = [];
  List<ChecklistTemplate> _checklists = [];
  String _searchKeyword = '';
  String? _selectedTopicId;
  String? _featuredImageUrl;
  String? _featuredImageContentId;

  static const _typeLabels = ['Tất cả', 'Bài viết', 'FAQ', 'Checklist'];
  static const _stageLabels = ['Chuẩn bị', 'Thai kỳ', 'Sau sinh', 'Chăm bé'];
  static const _stageValues = [
    'PREPARATION',
    'PREGNANCY',
    'POSTPARTUM',
    'BABY_CARE',
  ];

  static const _topicIcons = <String, IconData>{
    'restaurant': Icons.restaurant,
    'bedtime': Icons.bedtime,
    'vaccines': Icons.vaccines,
    'psychology': Icons.psychology,
    'health_and_safety': Icons.health_and_safety,
    'topic': Icons.topic,
  };

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _openContentDetail(String contentId) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => VerifiedContentDetailScreen(contentId: contentId),
      ),
    );
  }

  List<ContentListItem> _filterContent(List<ContentListItem> items) {
    final keyword = _searchKeyword.trim().toLowerCase();
    return items
        .where((item) {
          final matchesTopic =
              _selectedTopicId == null || item.topicId == _selectedTopicId;
          final matchesKeyword =
              keyword.isEmpty || item.title.toLowerCase().contains(keyword);
          return matchesTopic && matchesKeyword;
        })
        .toList(growable: false);
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _loadError = null;
    });
    final stage = _stageValues[_selectedStageIndex];
    try {
      final results = await Future.wait([
        _communityService.getTopics(),
        _contentService.getContent(type: 'ARTICLE', stage: stage, size: 100),
        _contentService.getContent(type: 'FAQ', stage: stage, size: 100),
        _contentService.getChecklists(stage: stage),
      ]);
      if (mounted) {
        final articles = results[1] as List<ContentListItem>;
        setState(() {
          _topics = results[0] as List<CommunityTopic>;
          _articles = articles;
          _faqs = results[2] as List<ContentListItem>;
          _checklists = results[3] as List<ChecklistTemplate>;
          _loading = false;
        });
        _loadFeaturedImage(articles);
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _topics = [];
          _articles = [];
          _faqs = [];
          _checklists = [];
          _loading = false;
          _loadError = 'Không tải được nội dung. Vui lòng thử lại.';
        });
      }
    }
  }

  Future<void> _loadFeaturedImage(List<ContentListItem> articles) async {
    if (articles.isEmpty) {
      if (mounted) {
        setState(() {
          _featuredImageUrl = null;
          _featuredImageContentId = null;
        });
      }
      return;
    }
    try {
      final detail = await _contentService.getContentDetail(articles.first.id);
      if (mounted &&
          _articles.isNotEmpty &&
          _articles.first.id == articles.first.id) {
        setState(() {
          _featuredImageUrl = detail.imageUrls.firstOrNull;
          _featuredImageContentId = articles.first.id;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _featuredImageUrl = null;
          _featuredImageContentId = null;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: RefreshIndicator(
          color: _primaryContainer,
          onRefresh: _load,
          child: CustomScrollView(
            slivers: [
              SliverToBoxAdapter(child: _buildAppBar()),
              SliverToBoxAdapter(child: _buildContextStrip()),
              SliverToBoxAdapter(child: _buildTypeTabs()),
              SliverToBoxAdapter(child: _buildStageChips()),
              SliverToBoxAdapter(child: _buildSearchInput()),
              SliverToBoxAdapter(child: _buildTopicRow()),
              if (_loading)
                const SliverFillRemaining(
                  child: Center(
                    child: CircularProgressIndicator(color: _primaryContainer),
                  ),
                )
              else if (_loadError != null)
                SliverFillRemaining(child: _buildLoadError())
              else ...[
                // Show sections based on selected type
                if (_selectedTypeIndex == 0 || _selectedTypeIndex == 1)
                  SliverToBoxAdapter(child: _buildFeaturedArticle()),
                if (_selectedTypeIndex == 1)
                  SliverToBoxAdapter(child: _buildArticleList()),
                if (_selectedTypeIndex == 0 || _selectedTypeIndex == 2)
                  SliverToBoxAdapter(child: _buildFaqSection()),
                if (_selectedTypeIndex == 0 || _selectedTypeIndex == 3)
                  SliverToBoxAdapter(child: _buildChecklistSection()),
                SliverToBoxAdapter(child: _buildSafetyDisclaimer()),
              ],
              const SliverToBoxAdapter(child: SizedBox(height: 32)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildLoadError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: _onSurfaceVariant),
            const SizedBox(height: 12),
            Text(
              _loadError!,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: _load, child: const Text('Thử lại')),
          ],
        ),
      ),
    );
  }

  // ── App bar ──
  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 4, 8, 0),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back, color: _onSurface),
            onPressed: () => Navigator.of(context).pop(),
          ),
          const Expanded(
            child: Text(
              'Nội dung dành cho bạn',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(
              Icons.notifications_outlined,
              color: _onSurfaceVariant,
            ),
            onPressed: () {
              // TODO: navigate to notification center
            },
          ),
        ],
      ),
    );
  }

  // ── Personal context strip ──
  Widget _buildContextStrip() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF6F1EC),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          children: [
            // Pregnant icon
            Container(
              width: 48,
              height: 48,
              decoration: const BoxDecoration(
                color: _primaryContainer,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.pregnant_woman,
                color: Colors.white,
                size: 26,
              ),
            ),
            const SizedBox(width: 14),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Trạng thái hiện tại',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  SizedBox(height: 2),
                  Text(
                    'Thai kỳ · Tuần 24',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: _onSurface,
                    ),
                  ),
                ],
              ),
            ),
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: _surfaceContainerHigh, width: 1),
              ),
              child: const Icon(Icons.edit_calendar, size: 20, color: _primary),
            ),
          ],
        ),
      ),
    );
  }

  // ── Type selector tabs ──
  Widget _buildTypeTabs() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: List.generate(_typeLabels.length, (i) {
          final isActive = _selectedTypeIndex == i;
          return Expanded(
            child: GestureDetector(
              onTap: () => setState(() => _selectedTypeIndex = i),
              child: Container(
                padding: const EdgeInsets.only(bottom: 10, top: 12),
                decoration: BoxDecoration(
                  border: Border(
                    bottom: BorderSide(
                      color: isActive ? _primaryContainer : Colors.transparent,
                      width: 3,
                    ),
                  ),
                ),
                child: Text(
                  _typeLabels[i],
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: isActive ? FontWeight.w600 : FontWeight.w400,
                    color: isActive ? _primary : _onSurfaceVariant,
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }

  // ── Stage filter chips ──
  Widget _buildStageChips() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(0, 12, 0, 4),
      child: SizedBox(
        height: 44,
        child: ListView.separated(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          separatorBuilder: (context, i) => const SizedBox(width: 8),
          itemCount: _stageLabels.length,
          itemBuilder: (_, i) {
            final isActive = _selectedStageIndex == i;
            return GestureDetector(
              onTap: () {
                setState(() => _selectedStageIndex = i);
                _load();
              },
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: isActive ? _primaryContainer : _surfaceContainer,
                  borderRadius: BorderRadius.circular(99),
                  boxShadow: isActive
                      ? [
                          BoxShadow(
                            color: _primaryContainer.withAlpha(77),
                            blurRadius: 8,
                            offset: const Offset(0, 2),
                          ),
                        ]
                      : null,
                ),
                child: Text(
                  _stageLabels[i],
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: isActive ? Colors.white : _onSurfaceVariant,
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  // ── Search input ──
  Widget _buildSearchInput() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      child: Container(
        height: 48,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(99),
          border: Border.all(color: const Color(0xFFF2EAE4), width: 2),
        ),
        child: Row(
          children: [
            const SizedBox(width: 16),
            const Icon(Icons.search, color: _onSurfaceVariant, size: 22),
            const SizedBox(width: 10),
            Expanded(
              child: TextField(
                controller: _searchController,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _onSurface,
                ),
                decoration: const InputDecoration(
                  hintText: 'Tìm kiếm bài viết, chủ đề...',
                  hintStyle: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                  ),
                  border: InputBorder.none,
                  isDense: true,
                  contentPadding: EdgeInsets.zero,
                ),
                onChanged: (keyword) =>
                    setState(() => _searchKeyword = keyword),
              ),
            ),
            const SizedBox(width: 16),
          ],
        ),
      ),
    );
  }

  // ── Topic icon row ──
  Widget _buildTopicRow() {
    final displayTopics = _topics.where((t) => !t.isHidden).take(5).toList();
    if (displayTopics.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: displayTopics.map((t) {
          final icon = _topicIcons[t.icon] ?? Icons.topic;
          return GestureDetector(
            onTap: () {
              setState(() {
                _selectedTopicId = _selectedTopicId == t.id ? null : t.id;
              });
            },
            child: Column(
              children: [
                Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    color: _surfaceContainerLow,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Icon(icon, color: _primary, size: 26),
                ),
                const SizedBox(height: 6),
                Text(
                  t.name,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
          );
        }).toList(),
      ),
    );
  }

  // ── Featured article section ("Gợi ý hôm nay") ──
  Widget _buildFeaturedArticle() {
    final articles = _filterContent(_articles);
    if (articles.isEmpty) {
      return _selectedTypeIndex == 1
          ? const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 24),
              child: Center(child: Text('Không có bài viết phù hợp.')),
            )
          : const SizedBox.shrink();
    }
    final article = articles.first;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Gợi ý hôm nay',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
              GestureDetector(
                onTap: () => setState(() => _selectedTypeIndex = 1),
                child: const Text(
                  'Xem tất cả',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                    color: _primary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: const Color(0x0F5A463F),
                  blurRadius: 20,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Stack(
                  children: [
                    Container(
                      height: 176,
                      decoration: BoxDecoration(
                        color: _surfaceContainerHigh,
                        borderRadius: const BorderRadius.vertical(
                          top: Radius.circular(16),
                        ),
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: [
                            _primaryContainer.withAlpha(51),
                            _surfaceContainerHigh,
                          ],
                        ),
                      ),
                      child:
                          _featuredImageUrl == null ||
                              _featuredImageContentId != article.id
                          ? const Center(
                              child: Icon(
                                Icons.article_outlined,
                                size: 48,
                                color: _primaryContainer,
                              ),
                            )
                          : ClipRRect(
                              borderRadius: const BorderRadius.vertical(
                                top: Radius.circular(16),
                              ),
                              child: Image.network(
                                resolveVerifiedContentImageUrl(
                                  _featuredImageUrl!,
                                ),
                                width: double.infinity,
                                height: 176,
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => const Center(
                                  child: Icon(
                                    Icons.broken_image_outlined,
                                    size: 40,
                                    color: _primaryContainer,
                                  ),
                                ),
                              ),
                            ),
                    ),
                    // "Đã kiểm duyệt" badge
                    Positioned(
                      top: 12,
                      left: 12,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 5,
                        ),
                        decoration: BoxDecoration(
                          color: Colors.white.withAlpha(204),
                          borderRadius: BorderRadius.circular(99),
                        ),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.verified, size: 14, color: _primary),
                            SizedBox(width: 4),
                            Text(
                              'Đã kiểm duyệt',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 11,
                                fontWeight: FontWeight.w500,
                                color: _primary,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Stage tag + date
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 3,
                            ),
                            decoration: BoxDecoration(
                              color: _surfaceContainerLow,
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              _stageLabels[_selectedStageIndex].toUpperCase(),
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 10,
                                fontWeight: FontWeight.w600,
                                color: _primary,
                                letterSpacing: 0.5,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          if (article.publishedAt != null)
                            Text(
                              article.publishedAt!,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 12,
                                color: _onSurfaceVariant,
                              ),
                            ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      Text(
                        article.title,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                          height: 1.3,
                        ),
                      ),
                      const SizedBox(height: 16),
                      // CTA button
                      SizedBox(
                        width: double.infinity,
                        height: 48,
                        child: ElevatedButton(
                          onPressed: () => _openContentDetail(article.id),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: _primary,
                            foregroundColor: Colors.white,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(99),
                            ),
                            elevation: 0,
                          ),
                          child: const Text(
                            'Xem chi tiết',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildArticleList() {
    final articles = _filterContent(_articles).skip(1).toList(growable: false);
    if (articles.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Bài viết khác',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          ...articles.map(
            (article) => Card(
              child: ListTile(
                leading: const Icon(Icons.article_outlined, color: _primary),
                title: Text(
                  article.title,
                  style: const TextStyle(fontFamily: 'Lexend'),
                ),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => _openContentDetail(article.id),
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ── FAQ section ──
  Widget _buildFaqSection() {
    final faqs = _filterContent(_faqs);
    if (faqs.isEmpty && _selectedTypeIndex == 2) {
      return const Padding(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 24),
        child: Center(
          child: Text(
            'Không có câu hỏi nào cho giai đoạn này.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
        ),
      );
    }
    if (faqs.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Câu hỏi thường gặp',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          Container(
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: const Color(0x0F5A463F),
                  blurRadius: 20,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              children: List.generate(faqs.length, (i) {
                final faq = faqs[i];
                final isLast = i == faqs.length - 1;
                return Column(
                  children: [
                    InkWell(
                      borderRadius: i == 0
                          ? const BorderRadius.vertical(
                              top: Radius.circular(16),
                            )
                          : isLast
                          ? const BorderRadius.vertical(
                              bottom: Radius.circular(16),
                            )
                          : BorderRadius.zero,
                      onTap: () => _openContentDetail(faq.id),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20,
                          vertical: 16,
                        ),
                        child: Row(
                          children: [
                            Expanded(
                              child: Text(
                                faq.title,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 14,
                                  color: _onSurface,
                                  height: 1.3,
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            const Icon(
                              Icons.chevron_right,
                              color: _onSurfaceVariant,
                              size: 22,
                            ),
                          ],
                        ),
                      ),
                    ),
                    if (!isLast)
                      const Divider(
                        height: 1,
                        thickness: 1,
                        indent: 20,
                        endIndent: 20,
                        color: Color(0xFFF2EAE4),
                      ),
                  ],
                );
              }),
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  // ── Checklist section ──
  Widget _buildChecklistSection() {
    if (_checklists.isEmpty && _selectedTypeIndex == 3) {
      return const Padding(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 24),
        child: Center(
          child: Text(
            'Không có checklist nào cho giai đoạn này.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
        ),
      );
    }
    if (_checklists.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Checklist theo giai đoạn',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          ..._checklists.map(
            (cl) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  border: const Border(
                    left: BorderSide(color: _primaryContainer, width: 4),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0x0F5A463F),
                      blurRadius: 20,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 3,
                                  ),
                                  decoration: BoxDecoration(
                                    color: _surfaceContainerLow,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    _stageLabelFromValue(cl.stage),
                                    style: const TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 10,
                                      fontWeight: FontWeight.w600,
                                      color: _primary,
                                      letterSpacing: 0.5,
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  '${cl.items.length} mục cần chuẩn bị',
                                  style: const TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 12,
                                    color: _onSurfaceVariant,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text(
                              cl.name,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                                color: _onSurface,
                              ),
                            ),
                            if (cl.description.isNotEmpty) ...[
                              const SizedBox(height: 4),
                              Row(
                                children: [
                                  const Icon(
                                    Icons.verified_user,
                                    size: 14,
                                    color: _onSurfaceVariant,
                                  ),
                                  const SizedBox(width: 4),
                                  Flexible(
                                    child: Text(
                                      cl.description,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        fontFamily: 'Lexend',
                                        fontSize: 12,
                                        color: _onSurfaceVariant,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        width: 44,
                        height: 44,
                        decoration: const BoxDecoration(
                          color: _secondaryContainer,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.checklist,
                          size: 22,
                          color: _primary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  // ── Safety disclaimer ──
  Widget _buildSafetyDisclaimer() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: const Color(0xFFFFFBE6),
          borderRadius: BorderRadius.circular(12),
        ),
        child: const Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.info_outline, size: 20, color: Color(0xFFB89A00)),
            SizedBox(width: 10),
            Expanded(
              child: Text(
                'Lưu ý: Các nội dung trên mang tính chất tham khảo. '
                'Nếu dấu hiệu nặng lên, hãy liên hệ cơ sở y tế phù hợp ngay lập tức.',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  fontStyle: FontStyle.italic,
                  color: Color(0xFF6D5B00),
                  height: 1.4,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _stageLabelFromValue(String value) {
    final idx = _stageValues.indexOf(value);
    if (idx >= 0) return _stageLabels[idx].toUpperCase();
    return value;
  }
}
