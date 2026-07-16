import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
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
  String? _startingChatWithExpertProfileId;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _load(reset: true);
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >= _scrollController.position.maxScrollExtent - 200 &&
        _hasMore &&
        !_loadingMore &&
        !_loading) {
      _loadMore();
    }
  }

  void _onSearchChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () => _load(reset: true));
  }

  Future<void> _load({required bool reset}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await DirectChatService.instance.getExpertDirectory(
        q: _searchController.text.trim(),
        page: 0,
        size: 20,
      );
      if (!mounted) return;
      setState(() {
        _experts = page.experts;
        _page = page.currentPage;
        _hasMore = page.hasMore;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Không thể tải danh sách chuyên gia.';
        _loading = false;
      });
    }
  }

  Future<void> _loadMore() async {
    setState(() => _loadingMore = true);
    try {
      final page = await DirectChatService.instance.getExpertDirectory(
        q: _searchController.text.trim(),
        page: _page + 1,
        size: 20,
      );
      if (!mounted) return;
      setState(() {
        _experts = [..._experts, ...page.experts];
        _page = page.currentPage;
        _hasMore = page.hasMore;
      });
    } catch (_) {
      // best-effort — user can keep scrolling to retry via the scroll trigger
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  Future<void> _startChat(ExpertDirectoryItem expert) async {
    setState(() => _startingChatWithExpertProfileId = expert.expertProfileId);
    try {
      final conversation =
          await DirectChatService.instance.findOrCreateConversation(expert.expertProfileId);
      if (!mounted) return;
      context.push('/direct-chat/${conversation.conversationId}');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể mở cuộc trò chuyện: $e')),
      );
    } finally {
      if (mounted) setState(() => _startingChatWithExpertProfileId = null);
    }
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
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                isDense: true,
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
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!),
            const SizedBox(height: 12),
            FilledButton(onPressed: () => _load(reset: true), child: const Text('Thử lại')),
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
      separatorBuilder: (_, __) => const Divider(height: 1),
      itemBuilder: (context, index) {
        if (index >= _experts.length) {
          return const Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
          );
        }
        final expert = _experts[index];
        final isStarting = _startingChatWithExpertProfileId == expert.expertProfileId;
        return ListTile(
          leading: CircleAvatar(
            backgroundImage:
                expert.avatarUrl != null ? NetworkImage(expert.avatarUrl!) : null,
            child: expert.avatarUrl == null
                ? Text((expert.displayName?.isNotEmpty == true ? expert.displayName![0] : '?').toUpperCase())
                : null,
          ),
          title: Row(
            children: [
              Flexible(
                child: Text(
                  expert.displayName ?? expert.professionalTitle ?? 'Chuyên gia',
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 6),
              // Directory only ever returns APPROVED experts (ADR-MEDI-001) — always true here.
              const Icon(Icons.verified, size: 16, color: Colors.green),
            ],
          ),
          subtitle: Text([
            if (expert.specialty != null) expert.specialty!,
            if (expert.experienceYears != null) '${expert.experienceYears} năm kinh nghiệm',
            if (expert.ratingAvg != null) '★ ${expert.ratingAvg!.toStringAsFixed(1)}',
          ].join(' · ')),
          trailing: FilledButton(
            onPressed: isStarting ? null : () => _startChat(expert),
            child: isStarting
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Trò chuyện'),
          ),
          onTap: () => context.push('/expert/public/${expert.expertProfileId}'),
        );
      },
    );
  }
}
