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

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerHigh = Color(0xFFF1E6E0);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outlineVariant = Color(0xFFE5D3CA);

  void _selectSpecialty(String? specialty) {
    if (_selectedSpecialty == specialty) return;
    setState(() => _selectedSpecialty = specialty);
    _load(reset: true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            _buildHeader(context),
            if (_specialties.isNotEmpty) _buildSpecialtyFilterBar(),
            Expanded(child: _buildBody()),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    final topInset = MediaQuery.of(context).padding.top;
    final canPop = ModalRoute.of(context)?.canPop ?? false;
    return Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(24)),
        boxShadow: [
          BoxShadow(
            color: Color(0x0A845143),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      padding: EdgeInsets.fromLTRB(20, topInset + 12, 20, 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              if (canPop) ...[
                IconButton(
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                  icon: const Icon(Icons.arrow_back_ios_new_rounded, color: _onSurface, size: 20),
                  onPressed: () => Navigator.of(context).pop(),
                ),
                const SizedBox(width: 12),
              ],
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: _surfaceContainerLow,
                  shape: BoxShape.circle,
                  border: Border.all(color: _outlineVariant, width: 1),
                ),
                child: const Icon(
                  Icons.medical_services_rounded,
                  color: _primary,
                  size: 22,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Đội ngũ Chuyên gia',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                        letterSpacing: -0.3,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Row(
                      children: [
                        const Icon(
                          Icons.verified_rounded,
                          size: 14,
                          color: Color(0xFF10B981),
                        ),
                        const SizedBox(width: 4),
                        Text(
                          '100% Đã xác thực chứng chỉ y khoa',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                            color: _onSurfaceVariant.withValues(alpha: 0.9),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Container(
            decoration: BoxDecoration(
              color: _surfaceContainerLow,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: _outlineVariant, width: 1),
            ),
            child: TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurface,
                fontSize: 14,
              ),
              decoration: InputDecoration(
                hintText: 'Tìm theo tên bác sĩ, chuyên khoa, bệnh viện...',
                hintStyle: TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant.withValues(alpha: 0.7),
                  fontSize: 13,
                ),
                prefixIcon: const Icon(
                  Icons.search_rounded,
                  color: _primary,
                  size: 22,
                ),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear_rounded, size: 18, color: _onSurfaceVariant),
                        onPressed: () {
                          _searchController.clear();
                          _load(reset: true);
                        },
                      )
                    : null,
                filled: false,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                isDense: true,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSpecialtyFilterBar() {
    return Container(
      margin: const EdgeInsets.only(top: 12, bottom: 4),
      height: 38,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        children: [
          ChoiceChip(
            label: const Text('Tất cả chuyên khoa'),
            selected: _selectedSpecialty == null,
            onSelected: (_) => _selectSpecialty(null),
            selectedColor: _primary,
            backgroundColor: _surface,
            elevation: _selectedSpecialty == null ? 1 : 0,
            shadowColor: const Color(0x1F845143),
            labelStyle: TextStyle(
              fontFamily: 'Lexend',
              color: _selectedSpecialty == null ? Colors.white : _onSurfaceVariant,
              fontSize: 13,
              fontWeight: _selectedSpecialty == null ? FontWeight.w600 : FontWeight.w400,
            ),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            side: BorderSide(
              color: _selectedSpecialty == null ? Colors.transparent : _outlineVariant,
            ),
            shape: StadiumBorder(),
          ),
          const SizedBox(width: 8),
          for (final specialty in _specialties) ...[
            ChoiceChip(
              label: Text(specialty),
              selected: _selectedSpecialty == specialty,
              onSelected: (_) => _selectSpecialty(specialty),
              selectedColor: _primary,
              backgroundColor: _surface,
              elevation: _selectedSpecialty == specialty ? 1 : 0,
              shadowColor: const Color(0x1F845143),
              labelStyle: TextStyle(
                fontFamily: 'Lexend',
                color: _selectedSpecialty == specialty ? Colors.white : _onSurfaceVariant,
                fontSize: 13,
                fontWeight: _selectedSpecialty == specialty ? FontWeight.w600 : FontWeight.w400,
              ),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              side: BorderSide(
                color: _selectedSpecialty == specialty ? Colors.transparent : _outlineVariant,
              ),
              shape: const StadiumBorder(),
            ),
            const SizedBox(width: 8),
          ],
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return _buildSkeletonLoader();
    }
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding: const EdgeInsets.all(16),
                decoration: const BoxDecoration(
                  color: _surfaceContainerLow,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.cloud_off_rounded, size: 40, color: _primary),
              ),
              const SizedBox(height: 16),
              Text(
                _error!,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurface,
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: () => _load(reset: true),
                icon: const Icon(Icons.refresh_rounded, size: 18),
                label: const Text('Thử lại', style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600)),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                ),
              ),
            ],
          ),
        ),
      );
    }
    if (_experts.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding: const EdgeInsets.all(20),
                decoration: const BoxDecoration(
                  color: _surfaceContainerLow,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.person_search_rounded,
                  size: 48,
                  color: _primaryContainer,
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'Không tìm thấy bác sĩ phù hợp',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                'Thử tìm kiếm với từ khóa khác hoặc bỏ lọc chuyên khoa để xem danh sách bác sĩ nhé.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant.withValues(alpha: 0.8),
                ),
              ),
            ],
          ),
        ),
      );
    }
    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      itemCount: _experts.length + (_hasMore ? 1 : 0),
      itemBuilder: (context, index) {
        if (index >= _experts.length) {
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Center(
              child: _loadMoreFailed
                  ? OutlinedButton.icon(
                      onPressed: _loadingMore ? null : _loadMore,
                      icon: const Icon(Icons.refresh_rounded, size: 18),
                      label: const Text('Tải thêm', style: TextStyle(fontFamily: 'Lexend')),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: _primary,
                        side: const BorderSide(color: _primary),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                      ),
                    )
                  : const CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: _primary,
                    ),
            ),
          );
        }
        final expert = _experts[index];
        return _buildExpertCard(context, expert);
      },
    );
  }

  Widget _buildExpertCard(BuildContext context, ExpertDirectoryItem expert) {
    final title = expert.displayName ?? expert.professionalTitle ?? 'Chuyên gia Y tế';
    final isApproved = expert.verificationStatus == 'APPROVED';

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _outlineVariant, width: 1),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A845143),
            blurRadius: 12,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: () => context.push('/expert/public/${expert.expertProfileId}'),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Stack(
                      children: [
                        Container(
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: _primaryContainer.withValues(alpha: 0.5),
                              width: 2,
                            ),
                          ),
                          child: CircleAvatar(
                            radius: 30,
                            backgroundColor: _surfaceContainerLow,
                            backgroundImage: expert.avatarUrl != null
                                ? NetworkImage(expert.avatarUrl!)
                                : null,
                            child: expert.avatarUrl == null
                                ? Text(
                                    (title.isNotEmpty ? title[0] : 'B').toUpperCase(),
                                    style: const TextStyle(
                                      fontFamily: 'Lexend',
                                      color: _primary,
                                      fontWeight: FontWeight.w700,
                                      fontSize: 20,
                                    ),
                                  )
                                : null,
                          ),
                        ),
                        if (isApproved)
                          Positioned(
                            bottom: 0,
                            right: 0,
                            child: Container(
                              padding: const EdgeInsets.all(2),
                              decoration: const BoxDecoration(
                                color: Colors.white,
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(
                                Icons.verified_rounded,
                                size: 18,
                                color: Color(0xFF10B981),
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  title,
                                  style: const TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 16,
                                    fontWeight: FontWeight.w700,
                                    color: _onSurface,
                                    letterSpacing: -0.2,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                          if (expert.specialty != null && expert.specialty!.isNotEmpty) ...[
                            const SizedBox(height: 3),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                              decoration: BoxDecoration(
                                color: _surfaceContainerLow,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                expert.specialty!,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                  color: _primary,
                                ),
                              ),
                            ),
                          ],
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              if (expert.experienceYears != null) ...[
                                const Icon(Icons.work_history_rounded, size: 14, color: _onSurfaceVariant),
                                const SizedBox(width: 4),
                                Text(
                                  '${expert.experienceYears} năm kinh nghiệm',
                                  style: const TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 12,
                                    color: _onSurfaceVariant,
                                  ),
                                ),
                              ],
                              if (expert.experienceYears != null && expert.ratingAvg != null)
                                const Text(' · ', style: TextStyle(color: _onSurfaceVariant)),
                              if (expert.ratingAvg != null) ...[
                                const Icon(
                                  Icons.star_rounded,
                                  size: 15,
                                  color: Color(0xFFF59E0B),
                                ),
                                const SizedBox(width: 2),
                                Text(
                                  expert.ratingAvg!.toStringAsFixed(1),
                                  style: const TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 12,
                                    fontWeight: FontWeight.w700,
                                    color: _onSurface,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    const Icon(
                      Icons.chevron_right_rounded,
                      color: _onSurfaceVariant,
                      size: 22,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSkeletonLoader() {
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      itemCount: 4,
      itemBuilder: (context, index) {
        return Container(
          margin: const EdgeInsets.only(bottom: 14),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: _outlineVariant),
          ),
          child: Row(
            children: [
              Container(
                width: 60,
                height: 60,
                decoration: const BoxDecoration(
                  color: _surfaceContainerHigh,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 140,
                      height: 16,
                      decoration: BoxDecoration(
                        color: _surfaceContainerHigh,
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      width: 90,
                      height: 12,
                      decoration: BoxDecoration(
                        color: _surfaceContainerLow,
                        borderRadius: BorderRadius.circular(6),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      width: 160,
                      height: 12,
                      decoration: BoxDecoration(
                        color: _surfaceContainerLow,
                        borderRadius: BorderRadius.circular(6),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

