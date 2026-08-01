import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/auth/auth_state.dart';
import '../models/expert_directory_item.dart';
import '../services/direct_chat_service.dart';

/// UC-144D: "Mother chọn Verified Expert, nhấn Trò chuyện". Directory search/pagination now
/// actually reach the backend query (ADR-MEDI-001). No online/availability indicator — no real
/// data source for it (TDS §13.3).
class ExpertDirectoryScreen extends StatefulWidget {
  const ExpertDirectoryScreen({super.key});

  @override
  State<ExpertDirectoryScreen> createState() => _ExpertDirectoryScreenState();
}

class _ExpertDirectoryScreenState extends State<ExpertDirectoryScreen> {
  final TextEditingController _searchController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  Timer? _debounce;

  List<ExpertDirectoryItem> _experts = [];
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;
  int _page = 0;
  bool _hasMore = false;
  bool _loadMoreFailed = false;
  int _requestGeneration = 0;
  String? _selectedSpecialty;
  List<String> _specialties = const [];
  late String? _accountId;

  @override
  void initState() {
    super.initState();
    _accountId = AuthState.instance.userId;
    AuthState.instance.addListener(_handleAuthChanged);
    _scrollController.addListener(_onScroll);
    _load(reset: true);
  }

  @override
  void dispose() {
    AuthState.instance.removeListener(_handleAuthChanged);
    _debounce?.cancel();
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _handleAuthChanged() {
    final current = AuthState.instance.userId;
    if (current == _accountId) return;
    _accountId = current;
    _requestGeneration++;
    if (!mounted) return;
    setState(() {
      _experts = const [];
      _loading = false;
      _loadingMore = false;
      _loadMoreFailed = false;
      _error =
          'Phiên đăng nhập đã thay đổi. Danh sách của tài khoản trước đã được xóa.';
    });
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
            _scrollController.position.maxScrollExtent - 200 &&
        _hasMore &&
        !_loadingMore &&
        !_loading) {
      _loadMore();
    }
  }

  void _onSearchChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(
      const Duration(milliseconds: 400),
      () => _load(reset: true),
    );
  }

  Future<void> _load({required bool reset}) async {
    final generation = ++_requestGeneration;
    final requestAccountId = AuthState.instance.userId;
    final query = _searchController.text.trim();
    final specialty = _selectedSpecialty;
    setState(() {
      _loading = true;
      _loadingMore = false;
      _error = null;
      _loadMoreFailed = false;
    });
    try {
      final page = await DirectChatService.instance.getExpertDirectory(
        q: query,
        specialty: specialty,
        page: 0,
        size: 20,
      );
      if (!mounted ||
          generation != _requestGeneration ||
          AuthState.instance.userId != requestAccountId) {
        return;
      }
      setState(() {
        _experts = page.experts;
        _page = page.currentPage;
        _hasMore = page.hasMore;
        _specialties = page.specialties;
        _loading = false;
      });
      _fillViewportIfNeeded(generation);
    } catch (e) {
      if (!mounted ||
          generation != _requestGeneration ||
          AuthState.instance.userId != requestAccountId) {
        return;
      }
      setState(() {
        _error = 'Không thể tải danh sách chuyên gia.';
        _loading = false;
      });
    }
  }

  Future<void> _loadMore() async {
    final generation = _requestGeneration;
    final requestAccountId = AuthState.instance.userId;
    final query = _searchController.text.trim();
    final specialty = _selectedSpecialty;
    final nextPage = _page + 1;
    setState(() {
      _loadingMore = true;
      _loadMoreFailed = false;
    });
    try {
      final page = await DirectChatService.instance.getExpertDirectory(
        q: query,
        specialty: specialty,
        page: nextPage,
        size: 20,
      );
      if (!mounted ||
          generation != _requestGeneration ||
          AuthState.instance.userId != requestAccountId) {
        return;
      }
      setState(() {
        _experts = [..._experts, ...page.experts];
        _page = page.currentPage;
        _hasMore = page.hasMore;
        _specialties = page.specialties;
      });
      _fillViewportIfNeeded(generation);
    } catch (_) {
      if (mounted &&
          generation == _requestGeneration &&
          AuthState.instance.userId == requestAccountId) {
        setState(() => _loadMoreFailed = true);
      }
    } finally {
      if (mounted &&
          generation == _requestGeneration &&
          AuthState.instance.userId == requestAccountId) {
        setState(() => _loadingMore = false);
      }
    }
  }

  void _fillViewportIfNeeded(int generation) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted ||
          generation != _requestGeneration ||
          !_hasMore ||
          _loadingMore) {
        return;
      }
      if (_scrollController.hasClients &&
          _scrollController.position.maxScrollExtent <= 0) {
        _loadMore();
      }
    });
  }

  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _surface = Color(0xFFF6F1EC);
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onVariant = Color(0xFF524440);
  static const _outline = Color(0xFFD6C2BD);

  void _selectSpecialty(String? specialty) {
    if (_selectedSpecialty == specialty) return;
    setState(() => _selectedSpecialty = specialty);
    _load(reset: true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        backgroundColor: _primaryDark,
        elevation: 0,
        foregroundColor: Colors.white,
        centerTitle: true,
        title: const Text(
          'Chuyên Gia Y Tế',
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
        ),
      ),
      body: Column(
        children: [
          Container(
            color: Colors.white,
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            child: TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              style: const TextStyle(color: _onSurface, fontSize: 14),
              decoration: InputDecoration(
                hintText: 'Tìm theo tên bác sĩ, chuyên khoa...',
                hintStyle: const TextStyle(color: _onVariant, fontSize: 14),
                prefixIcon: const Icon(Icons.search_rounded, color: _primary),
                filled: true,
                fillColor: _surface,
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(24),
                  borderSide: BorderSide.none,
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(24),
                  borderSide: BorderSide(color: _outline.withValues(alpha: 0.4)),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(24),
                  borderSide: const BorderSide(color: _primary, width: 1.5),
                ),
                isDense: true,
              ),
            ),
          ),
          if (_specialties.isNotEmpty)
            Container(
              color: Colors.white,
              padding: const EdgeInsets.only(bottom: 12),
              child: SizedBox(
                height: 38,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  children: [
                    ChoiceChip(
                      label: const Text('Tất cả'),
                      selected: _selectedSpecialty == null,
                      onSelected: (_) => _selectSpecialty(null),
                      selectedColor: _primaryDark,
                      labelStyle: TextStyle(
                        color: _selectedSpecialty == null ? Colors.white : _onVariant,
                        fontSize: 13,
                        fontWeight: _selectedSpecialty == null ? FontWeight.bold : FontWeight.normal,
                      ),
                      backgroundColor: _surface,
                      side: BorderSide(
                        color: _selectedSpecialty == null ? Colors.transparent : _outline,
                      ),
                      shape: StadiumBorder(),
                    ),
                    const SizedBox(width: 8),
                    for (final specialty in _specialties) ...[
                      ChoiceChip(
                        label: Text(specialty),
                        selected: _selectedSpecialty == specialty,
                        onSelected: (_) => _selectSpecialty(specialty),
                        selectedColor: _primaryDark,
                        labelStyle: TextStyle(
                          color: _selectedSpecialty == specialty ? Colors.white : _onVariant,
                          fontSize: 13,
                          fontWeight: _selectedSpecialty == specialty ? FontWeight.bold : FontWeight.normal,
                        ),
                        backgroundColor: _surface,
                        side: BorderSide(
                          color: _selectedSpecialty == specialty ? Colors.transparent : _outline,
                        ),
                        shape: const StadiumBorder(),
                      ),
                      const SizedBox(width: 8),
                    ],
                  ],
                ),
              ),
            ),
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: _primary));
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: _primaryDark),
            const SizedBox(height: 12),
            Text(_error!, style: const TextStyle(color: _onVariant)),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: () => _load(reset: true),
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
              style: FilledButton.styleFrom(backgroundColor: _primaryDark),
            ),
          ],
        ),
      );
    }
    if (_experts.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.person_search_rounded, size: 56, color: _primary.withValues(alpha: 0.5)),
            const SizedBox(height: 12),
            const Text(
              'Không tìm thấy chuyên gia phù hợp',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: _onSurface),
            ),
          ],
        ),
      );
    }
    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      itemCount: _experts.length + (_hasMore ? 1 : 0),
      itemBuilder: (context, index) {
        if (index >= _experts.length) {
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: Center(
              child: _loadMoreFailed
                  ? OutlinedButton.icon(
                      onPressed: _loadingMore ? null : _loadMore,
                      icon: const Icon(Icons.refresh),
                      label: const Text('Tải lại'),
                    )
                  : const CircularProgressIndicator(strokeWidth: 2, color: _primary),
            ),
          );
        }
        final expert = _experts[index];
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: _outline.withValues(alpha: 0.5)),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0C5A463F),
                  blurRadius: 10,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: Row(
              children: [
                Container(
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(color: _primary.withValues(alpha: 0.4), width: 2),
                  ),
                  child: CircleAvatar(
                    radius: 26,
                    backgroundColor: _surfaceLow,
                    backgroundImage: expert.avatarUrl != null
                        ? NetworkImage(expert.avatarUrl!)
                        : null,
                    child: expert.avatarUrl == null
                        ? Text(
                            (expert.displayName?.isNotEmpty == true
                                    ? expert.displayName![0]
                                    : 'B')
                                .toUpperCase(),
                            style: const TextStyle(
                              color: _primaryDark,
                              fontWeight: FontWeight.w700,
                              fontSize: 18,
                            ),
                          )
                        : null,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Flexible(
                            child: Text(
                              expert.displayName ??
                                  expert.professionalTitle ??
                                  'Chuyên gia',
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.bold,
                                color: _onSurface,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          const SizedBox(width: 6),
                          if (expert.verificationStatus == 'APPROVED')
                            const Icon(
                              Icons.verified_rounded,
                              size: 18,
                              color: _primary,
                              semanticLabel: 'Đã xác thực',
                            ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        [
                          if (expert.specialty != null) expert.specialty!,
                          if (expert.experienceYears != null)
                            '${expert.experienceYears} năm kinh nghiệm',
                        ].join(' · '),
                        style: const TextStyle(fontSize: 13, color: _onVariant),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      if (expert.ratingAvg != null) ...[
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            const Icon(Icons.star_rounded, size: 16, color: Color(0xFFF59E0B)),
                            const SizedBox(width: 4),
                            Text(
                              expert.ratingAvg!.toStringAsFixed(1),
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: _onSurface,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                OutlinedButton(
                  onPressed: () => context.push('/expert/public/${expert.expertProfileId}'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _primaryDark,
                    side: const BorderSide(color: _primary),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                  child: const Text('Hồ sơ', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

