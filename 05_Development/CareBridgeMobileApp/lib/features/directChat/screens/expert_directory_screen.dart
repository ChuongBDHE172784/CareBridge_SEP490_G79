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

  void _selectSpecialty(String? specialty) {
    if (_selectedSpecialty == specialty) return;
    setState(() => _selectedSpecialty = specialty);
    _load(reset: true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Chuyên gia đã xác thực')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              decoration: InputDecoration(
                hintText: 'Tìm theo tên, chuyên khoa...',
                prefixIcon: const Icon(Icons.search),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                isDense: true,
              ),
            ),
          ),
          if (_specialties.isNotEmpty)
            SizedBox(
              height: 44,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                children: [
                  ChoiceChip(
                    label: const Text('Tất cả'),
                    selected: _selectedSpecialty == null,
                    onSelected: (_) => _selectSpecialty(null),
                  ),
                  const SizedBox(width: 8),
                  for (final specialty in _specialties) ...[
                    ChoiceChip(
                      label: Text(specialty),
                      selected: _selectedSpecialty == specialty,
                      onSelected: (_) => _selectSpecialty(specialty),
                    ),
                    const SizedBox(width: 8),
                  ],
                ],
              ),
            ),
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: () => _load(reset: true),
              child: const Text('Thử lại'),
            ),
          ],
        ),
      );
    }
    if (_experts.isEmpty) {
      return const Center(child: Text('Không tìm thấy chuyên gia phù hợp'));
    }
    return ListView.separated(
      controller: _scrollController,
      itemCount: _experts.length + (_hasMore ? 1 : 0),
      separatorBuilder: (_, _) => const Divider(height: 1),
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
                  : const CircularProgressIndicator(strokeWidth: 2),
            ),
          );
        }
        final expert = _experts[index];
        return ListTile(
          leading: CircleAvatar(
            backgroundImage: expert.avatarUrl != null
                ? NetworkImage(expert.avatarUrl!)
                : null,
            child: expert.avatarUrl == null
                ? Text(
                    (expert.displayName?.isNotEmpty == true
                            ? expert.displayName![0]
                            : '?')
                        .toUpperCase(),
                  )
                : null,
          ),
          title: Row(
            children: [
              Flexible(
                child: Text(
                  expert.displayName ??
                      expert.professionalTitle ??
                      'Chuyên gia',
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 6),
              if (expert.verificationStatus == 'APPROVED')
                const Icon(
                  Icons.verified,
                  size: 18,
                  color: Color(0xFFC98C7B),
                  semanticLabel: 'Đã xác thực',
                ),
            ],
          ),
          subtitle: Text(
            [
              if (expert.specialty != null) expert.specialty!,
              if (expert.experienceYears != null)
                '${expert.experienceYears} năm kinh nghiệm',
              if (expert.ratingAvg != null)
                '★ ${expert.ratingAvg!.toStringAsFixed(1)}',
            ].join(' · '),
          ),
          trailing: OutlinedButton(
            onPressed: () => context.push('/expert/public/${expert.expertProfileId}'),
            child: const Text('Xem hồ sơ'),
          ),
          onTap: () => context.push('/expert/public/${expert.expertProfileId}'),
        );
      },
    );
  }
}
