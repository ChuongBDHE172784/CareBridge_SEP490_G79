import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/constants/content_stages.dart';
import '../../checklist/models/user_checklist_item_model.dart';
import '../../checklist/screens/checklist_detail_screen.dart';
import '../../reminder/screens/today_tasks_screen.dart';
import '../../checklist/services/user_checklist_service.dart';
import '../../journey/models/journey_model.dart';
import '../../journey/screens/mother_journey_screen.dart';
import '../../journey/services/journey_service.dart';
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
  const ViewContentScreen({
    super.key,
    this.mode = ContentBrowseMode.lifecycle,
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
  final _journeyService = JourneyService();
  final _userChecklistService = UserChecklistService.instance;
  final _searchController = TextEditingController();

  // ── State ──
  bool _loading = true;
  String? _loadError;
  bool _journeyLoadFailed = false;
  bool _articleLoadFailed = false;
  bool _faqLoadFailed = false;
  bool _checklistLoadFailed = false;
  int _loadGeneration = 0;
  String? _resolvedStage;
  String? _observedAccountId;
  int _selectedTypeIndex = 0; // 0=All, 1=Article, 2=FAQ, 3=Checklist
  int _selectedStageIndex = -1;
  JourneyDashboard? _dashboard;
  List<CommunityTopic> _topics = [];
  List<CommunityTopic> _tags = [];
  List<ContentListItem> _articles = [];
  List<ContentListItem> _faqs = [];
  List<ChecklistTemplate> _checklists = [];
  List<UserChecklistItem> _userChecklistItems = [];
  String _searchKeyword = '';
  String? _selectedTopicId;
  final Set<String> _selectedTagIds = <String>{};
  String? _featuredImageUrl;
  String? _featuredImageContentId;

  List<String> get _typeLabels => widget.mode == ContentBrowseMode.family
      ? const ['Tất cả', 'Bài viết', 'FAQ']
      : const ['Tất cả', 'Bài viết', 'FAQ', 'Checklist'];
  static final _stageLabels = contentStageOptions
      .map((stage) => stage.label)
      .toList(growable: false);
  static final _stageValues = contentStageOptions
      .map((stage) => stage.value)
      .toList(growable: false);

  @override
  void initState() {
    super.initState();
    _contentService = widget.contentService ?? ContentService.instance;
    _communityService = widget.communityService ?? CommunityService.instance;
    _observedAccountId = AuthState.instance.userId;
    AuthState.instance.addListener(_onAccountChanged);
    _load(syncStageToJourney: widget.mode == ContentBrowseMode.generic);
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

  List<ContentListItem> _filterContent(List<ContentListItem> items) {
    final keyword = _searchKeyword.trim().toLowerCase();
    return items
        .where((item) {
          final matchesTopic =
              _selectedTopicId == null || item.topicId == _selectedTopicId;
          final matchesTags =
              _selectedTagIds.isEmpty ||
              item.tagIds.any(_selectedTagIds.contains);
          final tagNames = item.tagIds
              .map(_tagNameForId)
              .whereType<String>()
              .join(' ');
          final matchesKeyword =
              keyword.isEmpty ||
              '${item.title} ${item.summary ?? ''} $tagNames'
                  .toLowerCase()
                  .contains(keyword);
          return matchesTopic && matchesTags && matchesKeyword;
        })
        .toList(growable: false);
  }

  String? _tagNameForId(String id) =>
      _tags.where((tag) => tag.id == id).firstOrNull?.name;

  Future<(T?, bool)> _capture<T>(Future<T> request) async {
    try {
      return (await request, false);
    } catch (_) {
      return (null, true);
    }
  }

  Future<void> _load({bool syncStageToJourney = false}) async {
    final generation = ++_loadGeneration;
    final accountId = AuthState.instance.userId;
    if (!mounted) return;
    setState(() {
      _loading = true;
      _loadError = null;
      _resolvedStage = null;
      if (widget.mode != ContentBrowseMode.generic) {
        _topics = [];
        _tags = [];
        _articles = [];
        _faqs = [];
        _checklists = [];
        _userChecklistItems = [];
      }
    });
    try {
      if (widget.mode == ContentBrowseMode.lifecycle) {
        final userItemsFuture = _capture(_userChecklistService.listItems());
        final results = await Future.wait<Object>([
          _contentService.getAllLifecycleContent(
            shouldContinue: () => _canApply(generation, accountId),
          ),
          _contentService.getLifecycleChecklists(),
        ]);
        if (!_canApply(generation, accountId)) return;

        final userItemsResult = await userItemsFuture;
        final content = results[0] as LifecycleEnvelope<List<ContentListItem>>;
        final checklists =
            results[1] as LifecycleEnvelope<List<ChecklistTemplate>>;
        final stageMismatch =
            content.stage != checklists.stage ||
            content.payload.any((item) => item.stage != content.stage) ||
            checklists.payload.any((item) => item.stage != content.stage);
        if (stageMismatch) {
          throw const FormatException('Lifecycle stage mismatch');
        }

        final articles = content.payload
            .where((item) => item.type == 'ARTICLE')
            .toList(growable: false);
        setState(() {
          _resolvedStage = content.stage;
          _topics = [];
          _tags = [];
          _articles = articles;
          _faqs = content.payload
              .where((item) => item.type == 'FAQ')
              .toList(growable: false);
          _checklists = checklists.payload;
          _userChecklistItems = userItemsResult.$1 ?? <UserChecklistItem>[];
          _articleLoadFailed = false;
          _faqLoadFailed = false;
          _checklistLoadFailed = false;
          _loading = false;
        });
        return;
      }

      JourneyDashboard? dashboard;
      var journeyLoadFailed = false;
      if (widget.mode == ContentBrowseMode.generic) {
        try {
          dashboard = await _journeyService.getDashboard();
        } catch (_) {
          journeyLoadFailed = true;
        }
      }
      if (!_canApply(generation, accountId)) return;
      final journeyStageIndex = dashboard == null
          ? -1
          : _stageIndexForDashboard(dashboard);
      final stageIndex = widget.mode == ContentBrowseMode.family
          ? _selectedStageIndex
          : syncStageToJourney || _selectedStageIndex < 0
          ? journeyStageIndex
          : _selectedStageIndex;
      final stage = stageIndex >= 0 ? _stageValues[stageIndex] : null;
      final topicsFuture = _capture(_communityService.getTopics(type: 'TOPIC'));
      final tagsFuture = _capture(_communityService.getTopics(type: 'TAG'));
      final articlesFuture = _capture(
        _contentService.getAllContent(type: 'ARTICLE', stage: stage),
      );
      final faqsFuture = _capture(
        _contentService.getAllContent(type: 'FAQ', stage: stage),
      );
      final checklistsFuture = widget.mode == ContentBrowseMode.family
          ? Future.value((<ChecklistTemplate>[], false))
          : _capture(_contentService.getChecklists(stage: stage));
      final journeyId = dashboard?.journeyId;
      final userItemsFuture =
          widget.mode == ContentBrowseMode.family ||
              journeyId == null ||
              journeyId.isEmpty
          ? Future.value((<UserChecklistItem>[], false))
          : _capture(_userChecklistService.listItems(journeyId: journeyId));
      final topicsResult = await topicsFuture;
      final tagsResult = await tagsFuture;
      final articlesResult = await articlesFuture;
      final faqsResult = await faqsFuture;
      final checklistsResult = await checklistsFuture;
      final userItemsResult = await userItemsFuture;
      if (_canApply(generation, accountId)) {
        final articles = articlesResult.$1 ?? <ContentListItem>[];
        setState(() {
          _dashboard = dashboard;
          _journeyLoadFailed = journeyLoadFailed;
          _selectedStageIndex = stageIndex;
          _topics = topicsResult.$1 ?? <CommunityTopic>[];
          _tags = tagsResult.$1 ?? <CommunityTopic>[];
          _articles = articles;
          _faqs = faqsResult.$1 ?? <ContentListItem>[];
          _checklists = checklistsResult.$1 ?? <ChecklistTemplate>[];
          _userChecklistItems = userItemsResult.$1 ?? <UserChecklistItem>[];
          _articleLoadFailed = articlesResult.$2;
          _faqLoadFailed = faqsResult.$2;
          _checklistLoadFailed = checklistsResult.$2;
          _loading = false;
        });
        _loadFeaturedImage(
          articles,
          generation: generation,
          accountId: accountId,
        );
      }
    } catch (_) {
      if (_canApply(generation, accountId)) {
        setState(() {
          _topics = [];
          _tags = [];
          _articles = [];
          _faqs = [];
          _checklists = [];
          _userChecklistItems = [];
          _loading = false;
          _loadError = widget.mode == ContentBrowseMode.lifecycle
              ? 'Không thể tải nội dung theo giai đoạn hiện tại.'
              : 'Không tải được nội dung. Vui lòng thử lại.';
        });
      }
    }
  }

  int _stageIndexForDashboard(JourneyDashboard dashboard) {
    return contentStageIndexForJourneyType(dashboard.journeyType);
  }

  Future<void> _loadFeaturedImage(
    List<ContentListItem> articles, {
    required int generation,
    required String? accountId,
  }) async {
    if (articles.isEmpty) {
      if (_canApply(generation, accountId)) {
        setState(() {
          _featuredImageUrl = null;
          _featuredImageContentId = null;
        });
      }
      return;
    }
    try {
      final detail = await _contentService.getContentDetail(articles.first.id);
      if (_canApply(generation, accountId) &&
          _articles.isNotEmpty &&
          _articles.first.id == articles.first.id) {
        setState(() {
          _featuredImageUrl = detail.imageUrls.firstOrNull;
          _featuredImageContentId = articles.first.id;
        });
      }
    } catch (_) {
      if (_canApply(generation, accountId)) {
        setState(() {
          _featuredImageUrl = null;
          _featuredImageContentId = null;
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
              if (widget.mode == ContentBrowseMode.generic ||
                  widget.mode == ContentBrowseMode.family)
                SliverToBoxAdapter(child: _buildStageChips()),
              SliverToBoxAdapter(child: _buildSearchInput()),
              SliverToBoxAdapter(child: _buildTopicRow()),
              if (_loading)
                const SliverFillRemaining(
                  key: Key('lifecycle-content-loading'),
                  child: Center(
                    child: CircularProgressIndicator(color: _primaryContainer),
                  ),
                )
              else if (_loadError != null)
                SliverFillRemaining(child: _buildErrorState())
              else if ((widget.mode == ContentBrowseMode.lifecycle ||
                      widget.mode == ContentBrowseMode.family) &&
                  _selectedTypeIsEmpty)
                SliverFillRemaining(child: _buildEmptyState())
              else ...[
                // Show sections based on selected type
                if (_selectedTypeIndex == 0 || _selectedTypeIndex == 1)
                  SliverToBoxAdapter(child: _buildFeaturedArticle()),
                if (_selectedTypeIndex == 1)
                  SliverToBoxAdapter(child: _buildArticleList()),
                if (_selectedTypeIndex == 0 || _selectedTypeIndex == 2)
                  SliverToBoxAdapter(child: _buildFaqSection()),
                if (widget.mode != ContentBrowseMode.family &&
                    (_selectedTypeIndex == 0 || _selectedTypeIndex == 3))
                  SliverToBoxAdapter(child: _buildChecklistSection()),
              ],
              if (!_loading && _loadError == null)
                SliverToBoxAdapter(child: _buildSafetyDisclaimer()),
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
                _loadError!,
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

  Widget _buildSectionLoadError(String label) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: _onSurfaceVariant),
            const SizedBox(height: 8),
            Text(
              'Không tải được $label. Vui lòng thử lại.',
              textAlign: TextAlign.center,
            ),
            TextButton(onPressed: _load, child: const Text('Thử lại')),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    final message = widget.mode == ContentBrowseMode.family
        ? 'Không có nội dung phù hợp với bộ lọc hiện tại.'
        : 'Chưa có nội dung đã kiểm duyệt cho giai đoạn này.';
    return Center(
      key: const Key('lifecycle-content-empty'),
      child: Semantics(
        liveRegion: true,
        label: message,
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.menu_book_rounded,
                color: _primaryContainer,
                size: 48,
              ),
              const SizedBox(height: 16),
              Text(
                message,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 16, color: _onSurface),
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
          Expanded(
            child: Text(
              widget.mode == ContentBrowseMode.family
                  ? 'Nội dung & FAQ'
                  : 'Nội dung dành cho bạn',
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
    if (widget.mode == ContentBrowseMode.lifecycle) {
      return _buildLifecycleContextStrip();
    }
    if (widget.mode == ContentBrowseMode.family) {
      return const SizedBox.shrink();
    }

    final dashboard = _dashboard;
    final statusLabel = _journeyLoadFailed
        ? 'Không tải được hành trình · Chạm để thử lại'
        : dashboard == null
        ? 'Đang tải hành trình...'
        : !dashboard.hasActiveJourney
        ? 'Chưa thiết lập hành trình'
        : dashboard.isPregnancy && dashboard.displayPregnancyWeek != null
        ? '${dashboard.phaseLabel} · Tuần ${dashboard.displayPregnancyWeek}'
        : dashboard.phaseLabel;
    final statusIcon = dashboard?.isPregnancy == true
        ? Icons.pregnant_woman
        : dashboard?.isPostpartum == true
        ? Icons.favorite_outline
        : Icons.timeline;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () async {
          await Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => const MotherJourneyScreen()),
          );
          if (mounted) await _load(syncStageToJourney: true);
        },
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: const Color(0xFFF6F1EC),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: const BoxDecoration(
                  color: _primaryContainer,
                  shape: BoxShape.circle,
                ),
                child: Icon(statusIcon, color: Colors.white, size: 26),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Trạng thái hiện tại',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      statusLabel,
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
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: _surface,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: _surfaceContainerHigh, width: 1),
                ),
                child: const Icon(
                  Icons.edit_calendar,
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

  // ── Type selector tabs ──
  Widget _buildLifecycleContextStrip() {
    final stageLabel = _resolvedStage == null
        ? 'Đang xác định từ máy chủ'
        : _stageLabelFromValue(_resolvedStage!);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      child: Semantics(
        key: const Key('lifecycle-content-stage'),
        label: 'Giai đoạn do CareBridge xác định: $stageLabel. Đã khóa.',
        readOnly: true,
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
          child: Column(
            children: [
              Row(
                children: [
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
                        const SizedBox(height: 2),
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
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

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
          itemCount: _stageLabels.length + 1,
          itemBuilder: (_, i) {
            final stageIndex = i - 1;
            final isActive = _selectedStageIndex == stageIndex;
            return GestureDetector(
              onTap: () {
                setState(() => _selectedStageIndex = stageIndex);
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
                  i == 0 ? 'Tất cả giai đoạn' : _stageLabels[stageIndex],
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
                  hintText: 'Tìm bài viết, FAQ hoặc tag...',
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

  Widget _buildTopicRow() {
    if (widget.mode == ContentBrowseMode.lifecycle)
      return const SizedBox.shrink();
    final topics = _topics.where((item) => !item.isHidden).toList();
    final tags = _tags.where((item) => !item.isHidden).toList();
    if (topics.isEmpty && tags.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Lọc nội dung',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          if (topics.isNotEmpty)
            _filterDropdown(
              'Chủ đề',
              topics,
              _selectedTopicId,
              (id) => setState(() => _selectedTopicId = id),
            ),
          if (tags.isNotEmpty)
            _filterDropdown(
              'Tag',
              tags,
              _selectedTagIds.length == 1 ? _selectedTagIds.first : null,
              (id) => setState(() {
                _selectedTagIds
                  ..clear()
                  ..addAll(id == null ? const <String>[] : [id]);
              }),
            ),
        ],
      ),
    );
  }

  Widget _filterDropdown(
    String label,
    List<CommunityTopic> items,
    String? selectedId,
    ValueChanged<String?> onSelect,
  ) => Padding(
    padding: const EdgeInsets.only(top: 10),
    child: DropdownButtonFormField<String>(
      initialValue: selectedId ?? '',
      isExpanded: true,
      decoration: InputDecoration(
        labelText: label,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      ),
      items: [
        const DropdownMenuItem(value: '', child: Text('Tất cả')),
        ...items.map(
          (item) => DropdownMenuItem(
            value: item.id,
            child: Text(item.name, overflow: TextOverflow.ellipsis),
          ),
        ),
      ],
      onChanged: (value) =>
          onSelect(value == null || value.isEmpty ? null : value),
    ),
  );

  // ── Featured article section ("Gợi ý hôm nay") ──
  Widget _buildFeaturedArticle() {
    if (_articleLoadFailed) return _buildSectionLoadError('bài viết');
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
                      if (article.summary?.trim().isNotEmpty ?? false) ...[
                        const SizedBox(height: 8),
                        Text(
                          article.summary!,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 13,
                            color: _onSurfaceVariant,
                            height: 1.4,
                          ),
                        ),
                      ],
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
                subtitle: article.summary?.trim().isNotEmpty ?? false
                    ? Text(
                        article.summary!,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      )
                    : null,
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
    if (_faqLoadFailed) {
      return _buildSectionLoadError('FAQ');
    }
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

    final visibleFaqs = _selectedTypeIndex == 0
        ? faqs.take(3).toList(growable: false)
        : faqs;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
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
              if (_selectedTypeIndex == 0 && faqs.length > visibleFaqs.length)
                TextButton(
                  onPressed: () => setState(() => _selectedTypeIndex = 2),
                  child: const Text('Xem tất cả'),
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
              children: List.generate(visibleFaqs.length, (i) {
                final faq = visibleFaqs[i];
                final isLast = i == visibleFaqs.length - 1;
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
  Set<String> get _importedTemplateItemIds => _userChecklistItems
      .map((item) => item.templateItemId)
      .whereType<String>()
      .toSet();

  Future<void> _openChecklistTemplate(ChecklistTemplate template) async {
    final importedIds = _importedTemplateItemIds;
    final rawJourneyId = _dashboard?.journeyId;
    final journeyId = rawJourneyId == null || rawJourneyId.isEmpty
        ? null
        : rawJourneyId;

    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => ChecklistDetailScreen(
          template: template,
          importedItemIds: importedIds,
          journeyId: journeyId,
          isLifecycleMode: widget.mode == ContentBrowseMode.lifecycle,
          userChecklistService: _userChecklistService,
        ),
      ),
    );

    if (mounted && journeyId != null) {
      try {
        final items = await _userChecklistService.listItems(
          journeyId: journeyId,
        );
        if (mounted) {
          setState(() => _userChecklistItems = items);
        }
      } catch (_) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Khong the tai lai danh sach checklist.')),
        );
      }
    }
  }

  // Legacy route retained for older content links; canonical checklist entry
  // now opens ChecklistDetailScreen above.
  // ignore: unused_element
  Future<void> _openMyChecklist({String? journeyId}) async {
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => TodayTasksScreen(journeyId: journeyId)),
    );
    if (mounted) {
      try {
        final items = await _userChecklistService.listItems(
          journeyId: journeyId,
        );
        if (mounted) setState(() => _userChecklistItems = items);
      } catch (_) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Không thể làm mới checklist. Dữ liệu hiện tại được giữ nguyên.',
            ),
            backgroundColor: _error,
          ),
        );
      }
    }
  }

  Widget _buildChecklistSection() {
    if (_checklistLoadFailed) {
      return _buildSectionLoadError('checklist');
    }
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
              child: InkWell(
                borderRadius: BorderRadius.circular(16),
                onTap: () => _openChecklistTemplate(cl),
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
                                    '${cl.items.where((item) => _importedTemplateItemIds.contains(item.id)).length}/${cl.items.length} mục đã thêm',
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
                          child: Icon(
                            Icons.chevron_right,
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

  String _stageLabelFromValue(String value) =>
      contentStageLabel(value).toUpperCase();
}
