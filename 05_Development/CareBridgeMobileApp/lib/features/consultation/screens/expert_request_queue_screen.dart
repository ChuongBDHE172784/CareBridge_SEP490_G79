import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/consultation_request.dart';
import '../services/consultation_request_refresh_bus.dart';
import '../services/consultation_request_service.dart';

class ExpertRequestQueueScreen extends StatefulWidget {
  const ExpertRequestQueueScreen({super.key});

  @override
  State<ExpertRequestQueueScreen> createState() =>
      _ExpertRequestQueueScreenState();
}

class _ExpertRequestQueueScreenState extends State<ExpertRequestQueueScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFF6F1EC);
  static const _filters = <String, String?>{
    'Đang chờ': 'PENDING',
    'Đã nhận': 'ACCEPTED',
    'Bị từ chối': 'REJECTED',
    'Đã hủy': 'CANCELLED',
    'Hết hạn': 'EXPIRED',
    'Tất cả': null,
  };

  String? _status = 'PENDING';
  List<ConsultationRequestSummary> _items = [];
  Object? _error;
  bool _loading = false;
  bool _hasMore = false;
  int _nextPage = 0;
  int _generation = 0;
  final Set<String> _busyIds = {};
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _load(refresh: true);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
            _scrollController.position.maxScrollExtent - 180 &&
        _hasMore &&
        !_loading) {
      _load();
    }
  }

  Future<void> _load({bool refresh = false}) async {
    if (_loading && !refresh) return;
    final generation = refresh ? ++_generation : _generation;
    final pageNumber = refresh ? 0 : _nextPage;
    setState(() {
      _loading = true;
      if (refresh) _error = null;
    });
    try {
      final page = await ConsultationRequestService.instance.listAssigned(
        status: _status,
        page: pageNumber,
      );
      if (!mounted || generation != _generation) return;
      setState(() {
        _items = refresh ? page.items : [..._items, ...page.items];
        _nextPage = page.page + 1;
        _hasMore = _nextPage < page.totalPages;
      });
    } catch (error) {
      if (!mounted || generation != _generation) return;
      setState(() => _error = error);
    } finally {
      if (mounted && generation == _generation) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _accept(ConsultationRequestSummary request) async {
    if (_busyIds.contains(request.id)) return;
    setState(() => _busyIds.add(request.id));
    try {
      await ConsultationRequestService.instance.accept(request.id);
      _afterTransition(request.id);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể chấp nhận yêu cầu: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _busyIds.remove(request.id));
    }
  }

  Future<void> _reject(ConsultationRequestSummary request) async {
    if (_busyIds.contains(request.id)) return;
    final controller = TextEditingController();
    final reason = await showDialog<String?>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Từ chối yêu cầu'),
        content: TextField(
          controller: controller,
          maxLength: 500,
          maxLines: 4,
          decoration: const InputDecoration(
            labelText: 'Lý do (không bắt buộc)',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Đóng'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Từ chối'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (!mounted || reason == null || _busyIds.contains(request.id)) return;
    setState(() => _busyIds.add(request.id));
    try {
      await ConsultationRequestService.instance.reject(
        request.id,
        reason: reason,
      );
      _afterTransition(request.id);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể từ chối yêu cầu: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _busyIds.remove(request.id));
    }
  }

  void _afterTransition(String id) {
    if (!mounted) return;
    setState(() {
      if (_status == 'PENDING') {
        _items.removeWhere((item) => item.id == id);
      }
    });
    ConsultationRequestRefreshBus.notify();
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: _canvas,
      child: Column(
        children: [
          SizedBox(
            height: 58,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              children: _filters.entries
                  .map(
                    (entry) => Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        label: Text(entry.key),
                        selected: _status == entry.value,
                        selectedColor: const Color(0xFFFFE2D9),
                        labelStyle: TextStyle(
                          color: _status == entry.value
                              ? _primary
                              : const Color(0xFF524440),
                          fontWeight: _status == entry.value
                              ? FontWeight.w700
                              : FontWeight.w500,
                        ),
                        onSelected: (_) {
                          setState(() => _status = entry.value);
                          _load(refresh: true);
                        },
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
            child: Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: const BoxDecoration(
                    color: Color(0xFFFFE2D9),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.assignment_outlined,
                    color: _primary,
                    size: 18,
                  ),
                ),
                const SizedBox(width: 10),
                Text(
                  _loading
                      ? 'Đang cập nhật danh sách...'
                      : '${_items.length} yêu cầu hiển thị',
                  style: const TextStyle(
                    color: Color(0xFF524440),
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: _loading && _items.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : _error != null && _items.isEmpty
                ? Center(
                    child: OutlinedButton(
                      onPressed: () => _load(refresh: true),
                      child: const Text('Thử lại'),
                    ),
                  )
                : _items.isEmpty
                ? const Center(child: Text('Chưa có yêu cầu tư vấn được giao'))
                : RefreshIndicator(
                    onRefresh: () => _load(refresh: true),
                    child: ListView.builder(
                      controller: _scrollController,
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                      itemCount: _items.length + (_hasMore ? 1 : 0),
                      itemBuilder: (context, index) {
                        if (index == _items.length) {
                          return const Padding(
                            padding: EdgeInsets.all(16),
                            child: Center(child: CircularProgressIndicator()),
                          );
                        }
                        final request = _items[index];
                        final busy = _busyIds.contains(request.id);
                        return Container(
                          key: Key('consultation-${request.id}'),
                          margin: const EdgeInsets.only(bottom: 10),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(20),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.06),
                                blurRadius: 14,
                                offset: const Offset(0, 3),
                              ),
                            ],
                          ),
                          child: Padding(
                            padding: const EdgeInsets.all(14),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  request.counterpartDisplayName ??
                                      'Người dùng CareBridge',
                                  style: const TextStyle(
                                    color: Color(0xFF271812),
                                    fontWeight: FontWeight.w800,
                                    fontSize: 16,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(request.topic),
                                const SizedBox(height: 8),
                                Wrap(
                                  spacing: 8,
                                  crossAxisAlignment: WrapCrossAlignment.center,
                                  children: [
                                    Chip(
                                      label: Text(_statusLabel(request.status)),
                                      backgroundColor: const Color(0xFFFFE2D9),
                                      side: BorderSide.none,
                                    ),
                                    Text(
                                      _timeAgo(request.createdAt),
                                      style: const TextStyle(
                                        color: Color(0xFF84736F),
                                        fontSize: 12,
                                      ),
                                    ),
                                  ],
                                ),
                                Align(
                                  alignment: Alignment.centerRight,
                                  child: TextButton.icon(
                                    key: Key('view-details-${request.id}'),
                                    onPressed: busy
                                        ? null
                                        : () => context.push(
                                            '/consultation-requests/${request.id}',
                                          ),
                                    icon: const Icon(Icons.visibility_outlined),
                                    label: const Text('Xem chi tiết'),
                                  ),
                                ),
                                if (request.status == 'PENDING') ...[
                                  const SizedBox(height: 8),
                                  Row(
                                    children: [
                                      Expanded(
                                        child: FilledButton(
                                          key: Key('accept-${request.id}'),
                                          onPressed: busy
                                              ? null
                                              : () => _accept(request),
                                          child: const Text('Chấp nhận'),
                                        ),
                                      ),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: OutlinedButton(
                                          key: Key('reject-${request.id}'),
                                          onPressed: busy
                                              ? null
                                              : () => _reject(request),
                                          child: const Text('Từ chối'),
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}

String _statusLabel(String status) => switch (status) {
  'PENDING' => 'Đang chờ',
  'ACCEPTED' => 'Đã nhận',
  'REJECTED' => 'Bị từ chối',
  'CANCELLED' => 'Đã hủy',
  'EXPIRED' => 'Hết hạn',
  _ => status,
};

String _timeAgo(DateTime value) {
  final diff = DateTime.now().difference(value);
  if (diff.inMinutes < 60) return 'Vừa gửi';
  if (diff.inHours < 24) return '${diff.inHours} giờ trước';
  return '${diff.inDays} ngày trước';
}
