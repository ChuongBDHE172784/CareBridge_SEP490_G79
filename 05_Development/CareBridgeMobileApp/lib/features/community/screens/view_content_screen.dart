import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';
import '../models/community_model.dart';
import '../models/content_model.dart';
import '../services/community_service.dart';
import '../services/content_service.dart';
import 'verified_content_detail_screen.dart';
import 'verified_content_search_screen.dart';

/// CB-223 — View Content and Checklist (UC-82)
/// Displays curated articles, FAQs, and checklists filtered by pregnancy
/// stage and topic. Navigated to via Navigator.push from Home or Community.
class ViewContentScreen extends StatefulWidget {
  const ViewContentScreen({
    super.key,
    this.mode = ContentBrowseMode.generic,
    this.contentService,
    this.communityService,
  });

  final ContentBrowseMode mode;
  final ContentService? contentService;
  final CommunityService? communityService;

  @override
  State<ViewContentScreen> createState() => _ViewContentScreenState();
}

class _ViewContentScreenState extends State<ViewContentScreen> {
  // ── Design tokens (Warm Claymorphism palette) ──
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
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

  late ContentService _contentService;
  late CommunityService _communityService;
  final _searchController = TextEditingController();

  // ── State ──
  bool _loading = true;
  String? _errorMessage;
  String? _resolvedStage;
  int _loadGeneration = 0;
  String? _observedAccountId;
  int _selectedTypeIndex = 0; // 0=All, 1=Article, 2=FAQ, 3=Checklist
  int _selectedStageIndex = 1; // 0=Prep, 1=Pregnancy, 2=Postpartum, 3=BabyCare
  List<CommunityTopic> _topics = [];
  List<ContentListItem> _articles = [];
  List<ContentListItem> _faqs = [];
  List<ChecklistTemplate> _checklists = [];

  static const _typeLabels = ['Tất cả', 'Bài viết', 'FAQ', 'Checklist'];
  static const _stageLabels = ['Chuẩn bị', 'Thai kỳ', 'Sau sinh', 'Chăm bé'];
  static const _stageValues = [
    'PRE_PREGNANCY',
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
    _contentService = widget.contentService ?? ContentService.instance;
    _communityService = widget.communityService ?? CommunityService.instance;
    _observedAccountId = AuthState.instance.userId;
    AuthState.instance.addListener(_onAccountChanged);
    _load();
  }

  @override
  void didUpdateWidget(covariant ViewContentScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.mode != widget.mode ||
        oldWidget.contentService != widget.contentService ||
        oldWidget.communityService != widget.communityService) {
      _contentService = widget.contentService ?? ContentService.instance;
      _communityService = widget.communityService ?? CommunityService.instance;
      _load();
    }
  }

  @override
  void dispose() {
    AuthState.instance.removeListener(_onAccountChanged);
    _searchController.dispose();
    super.dispose();
  }

  void _onAccountChanged() {
    final accountId = AuthState.instance.userId;
    if (accountId == _observedAccountId) return;
    _observedAccountId = accountId;
    _load();
  }

  void _openContentDetail(String contentId) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => VerifiedContentDetailScreen(
          contentId: contentId,
          mode: widget.mode,
          contentService: _contentService,
        ),
      ),
    );
  }

  Future<void> _load() async {
    final loadGeneration = ++_loadGeneration;
    final accountId = AuthState.instance.userId;
    setState(() {
      _loading = true;
      _errorMessage = null;
      _resolvedStage = null;
      _topics = [];
      _articles = [];
      _faqs = [];
      _checklists = [];
    });
    try {
      if (widget.mode == ContentBrowseMode.lifecycle) {
        final results = await Future.wait<Object>([
          _contentService.getLifecycleContent(size: 50),
          _contentService.getLifecycleChecklists(),
        ]);
        if (!_canApply(loadGeneration, accountId)) return;
        final content = results[0] as LifecycleEnvelope<PaginatedContent>;
        final checklists =
            results[1] as LifecycleEnvelope<List<ChecklistTemplate>>;
        if (content.stage != checklists.stage ||
            content.payload.data.any((item) => item.stage != content.stage) ||
            checklists.payload.any((item) => item.stage != content.stage)) {
          throw const FormatException('Lifecycle stage mismatch');
        }
        setState(() {
          _resolvedStage = content.stage;
          _articles = content.payload.data
              .where((item) => item.type == 'ARTICLE')
              .toList(growable: false);
          _faqs = content.payload.data
              .where((item) => item.type == 'FAQ')
              .toList(growable: false);
          _checklists = checklists.payload;
          _loading = false;
        });
        return;
      }

      final stage = _stageValues[_selectedStageIndex];
      final results = await Future.wait([
        _communityService.getTopics(),
        _contentService.getContent(type: 'ARTICLE', stage: stage),
        _contentService.getContent(type: 'FAQ', stage: stage),
        _contentService.getChecklists(stage: stage),
      ]);
      if (!_canApply(loadGeneration, accountId)) return;
      setState(() {
        _topics = results[0] as List<CommunityTopic>;
        _articles = results[1] as List<ContentListItem>;
        _faqs = results[2] as List<ContentListItem>;
        _checklists = results[3] as List<ChecklistTemplate>;
        _resolvedStage = stage;
        _loading = false;
      });
    } catch (_) {
      if (_canApply(loadGeneration, accountId)) {
        setState(() {
          _errorMessage = widget.mode == ContentBrowseMode.lifecycle
              ? 'Không thể tải nội dung theo giai đoạn hiện tại.'
              : 'Không thể tải nội dung. Vui lòng thử lại.';
          _loading = false;
        });
      }
    }
  }

  bool _canApply(int requestGeneration, String? accountId) =>
      mounted &&
      requestGeneration == _loadGeneration &&
      AuthState.instance.userId == accountId;

  bool get _selectedTypeIsEmpty => switch (_selectedTypeIndex) {
    1 => _articles.isEmpty,
    2 => _faqs.isEmpty,
    3 => _checklists.isEmpty,
    _ => _articles.isEmpty && _faqs.isEmpty && _checklists.isEmpty,
  };

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
              if (widget.mode == ContentBrowseMode.generic)
                SliverToBoxAdapter(child: _buildStageChips()),
              if (widget.mode == ContentBrowseMode.generic)
                SliverToBoxAdapter(child: _buildSearchInput())
              else
                SliverToBoxAdapter(child: _buildGenericBrowseEntry()),
              SliverToBoxAdapter(child: _buildTopicRow()),
              if (_loading)
                const SliverFillRemaining(
                  key: Key('lifecycle-content-loading'),
                  child: Center(
                    child: CircularProgressIndicator(color: _primaryContainer),
                  ),
                )
              else if (_errorMessage != null)
                SliverFillRemaining(child: _buildErrorState())
              else if (_selectedTypeIsEmpty)
                SliverFillRemaining(child: _buildEmptyState())
              else ...[
                // Show sections based on selected type
                if (_selectedTypeIndex == 0 || _selectedTypeIndex == 1)
                  SliverToBoxAdapter(child: _buildFeaturedArticle()),
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

  Widget _buildErrorState() {
    return Center(
      key: const Key('lifecycle-content-error'),
      child: Semantics(
        liveRegion: true,
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_rounded, color: _primary, size: 48),
              const SizedBox(height: 16),
              Text(
                _errorMessage!,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 16, color: _onSurface),
              ),
              const SizedBox(height: 16),
              Semantics(
                button: true,
                label: 'Thử tải lại nội dung',
                child: FilledButton.icon(
                  key: const Key('lifecycle-content-retry'),
                  onPressed: _load,
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Thử lại'),
                  style: FilledButton.styleFrom(
                    minimumSize: const Size(48, 48),
                    backgroundColor: _primaryContainer,
                    shape: const StadiumBorder(),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      key: const Key('lifecycle-content-empty'),
      child: Semantics(
        liveRegion: true,
        label: 'Chưa có nội dung đã kiểm duyệt cho giai đoạn này',
        child: const Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.menu_book_rounded, color: _primaryContainer, size: 48),
              SizedBox(height: 16),
              Text(
                'Chưa có nội dung đã kiểm duyệt cho giai đoạn này.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 16, color: _onSurface),
              ),
            ],
          ),
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
    final lifecycle = widget.mode == ContentBrowseMode.lifecycle;
    final stage = lifecycle
        ? _resolvedStage
        : (_resolvedStage ?? _stageValues[_selectedStageIndex]);
    final stageLabel = stage == null
        ? 'Đang xác định từ máy chủ'
        : _stageLabelFromValue(stage);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      child: Semantics(
        key: const Key('lifecycle-content-stage'),
        label: lifecycle
            ? 'Giai đoạn do CareBridge xác định: $stageLabel. Đã khóa.'
            : 'Bộ lọc giai đoạn: $stageLabel',
        readOnly: lifecycle,
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: const Color(0xFFF2EAE4),
            borderRadius: BorderRadius.circular(24),
            boxShadow: const [
              BoxShadow(
                color: Color(0x0F5A463F),
                blurRadius: 20,
                offset: Offset(0, 6),
              ),
            ],
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
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Giai đoạn nội dung hiện tại',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      stageLabel,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                  ],
                ),
              ),
              if (lifecycle)
                Container(
                  key: const Key('lifecycle-content-stage-locked'),
                  constraints: const BoxConstraints(
                    minWidth: 48,
                    minHeight: 48,
                  ),
                  decoration: const BoxDecoration(
                    color: Colors.white,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.lock_rounded, color: _primary),
                )
              else
                Container(
                  constraints: const BoxConstraints(
                    minWidth: 48,
                    minHeight: 48,
                  ),
                  decoration: const BoxDecoration(
                    color: _surface,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.tune_rounded,
                    size: 20,
                    color: _primary,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildGenericBrowseEntry() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
      child: Semantics(
        button: true,
        label: 'Mở tìm kiếm nội dung theo lựa chọn của bạn',
        child: OutlinedButton.icon(
          onPressed: () => Navigator.of(context).push(
            MaterialPageRoute(
              builder: (_) =>
                  VerifiedContentSearchScreen(contentService: _contentService),
            ),
          ),
          icon: const Icon(Icons.manage_search_rounded),
          label: const Text('Khám phá nội dung theo lựa chọn'),
          style: OutlinedButton.styleFrom(
            minimumSize: const Size(double.infinity, 48),
            foregroundColor: _primary,
            backgroundColor: Colors.white,
            side: const BorderSide(color: Color(0xFFD6C2BD)),
            shape: const StadiumBorder(),
            textStyle: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ),
    );
  }

  // ── Type selector tabs ──
  Widget _buildTypeTabs() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Row(
          children: List.generate(_typeLabels.length, (i) {
            final isActive = _selectedTypeIndex == i;
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text(_typeLabels[i]),
                selected: isActive,
                onSelected: (_) => setState(() => _selectedTypeIndex = i),
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 10,
                ),
                labelStyle: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: isActive ? Colors.white : _onSurfaceVariant,
                ),
                selectedColor: _primaryContainer,
                backgroundColor: _surfaceContainer,
                side: BorderSide.none,
                shape: const StadiumBorder(),
              ),
            );
          }),
        ),
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
                onSubmitted: (keyword) {
                  // TODO: trigger search with _contentService.searchContent
                },
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
              // TODO: filter content by topic
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
    if (_articles.isEmpty) return const SizedBox.shrink();
    final article = _articles.first;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            spacing: 16,
            runSpacing: 8,
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
                onTap: () {
                  // TODO: navigate to full article list
                },
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
                // Hero image placeholder
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
                      child: const Center(
                        child: Icon(
                          Icons.image,
                          size: 48,
                          color: _primaryContainer,
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
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
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
                              _stageLabelFromValue(article.stage),
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 10,
                                fontWeight: FontWeight.w600,
                                color: _primary,
                                letterSpacing: 0.5,
                              ),
                            ),
                          ),
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

  // ── FAQ section ──
  Widget _buildFaqSection() {
    if (_faqs.isEmpty && _selectedTypeIndex == 2) {
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
    if (_faqs.isEmpty) return const SizedBox.shrink();

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
              children: List.generate(_faqs.length.clamp(0, 5), (i) {
                final faq = _faqs[i];
                final isLast = i == _faqs.length.clamp(0, 5) - 1;
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
                            Wrap(
                              spacing: 8,
                              runSpacing: 8,
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
