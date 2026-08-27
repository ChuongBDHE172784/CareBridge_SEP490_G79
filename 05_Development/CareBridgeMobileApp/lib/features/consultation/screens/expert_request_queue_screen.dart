import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/components/app_user_avatar.dart';
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
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceLow = Color(0xFFF8EEE9);
  static const _surfaceHigh = Color(0xFFF1E6E0);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFE5D3CA);
  static const _errColor = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  static const _filterOptions = <_FilterItem>[
    _FilterItem('Đang chờ', 'PENDING', Icons.hourglass_top_rounded),
    _FilterItem('Đã nhận', 'ACCEPTED', Icons.check_circle_outline_rounded),
    _FilterItem('Bị từ chối', 'REJECTED', Icons.highlight_off_rounded),
    _FilterItem('Đã hủy', 'CANCELLED', Icons.cancel_outlined),
    _FilterItem('Hết hạn', 'EXPIRED', Icons.schedule_outlined),
    _FilterItem('Tất cả', null, Icons.grid_view_rounded),
  ];

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
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: const Text(
          'Từ chối yêu cầu',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
        ),
        content: TextField(
          controller: controller,
          maxLength: 500,
          maxLines: 4,
          decoration: InputDecoration(
            hintText: 'Nhập lý do từ chối (không bắt buộc)...',
            hintStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy', style: TextStyle(color: _outline)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: _errColor,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(18),
              ),
            ),
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
          // Filter Bar
          SizedBox(
            height: 56,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              itemCount: _filterOptions.length,
              itemBuilder: (context, index) {
                final opt = _filterOptions[index];
                final isSelected = _status == opt.value;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: InkWell(
                    onTap: () {
                      setState(() => _status = opt.value);
                      _load(refresh: true);
                    },
                    borderRadius: BorderRadius.circular(20),
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: isSelected ? _primary : _surface,
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(
                          color: isSelected ? _primary : _outlineVariant,
                          width: 1,
                        ),
                        boxShadow: isSelected
                            ? [
                                BoxShadow(
                                  color: _primary.withValues(alpha: 0.2),
                                  blurRadius: 8,
                                  offset: const Offset(0, 2),
                                ),
                              ]
                            : null,
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            opt.icon,
                            size: 16,
                            color: isSelected ? Colors.white : _outline,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            opt.label,
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              fontWeight: isSelected
                                  ? FontWeight.w700
                                  : FontWeight.w500,
                              color: isSelected ? Colors.white : _onSurface,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          ),

          // Count Subheader Bar
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 6, 20, 10),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: _surfaceHigh,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.assignment_outlined,
                        color: _primary,
                        size: 14,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        _loading
                            ? 'Đang tải...'
                            : '${_items.length} yêu cầu',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          color: _primary,
                          fontWeight: FontWeight.w700,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // Content List
          Expanded(
            child: _loading && _items.isEmpty
                ? const Center(
                    child: CircularProgressIndicator(color: _primaryContainer),
                  )
                : _error != null && _items.isEmpty
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.error_outline_rounded,
                          size: 40,
                          color: _errColor,
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'Không thể tải danh sách yêu cầu',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            color: _onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 12),
                        OutlinedButton(
                          onPressed: () => _load(refresh: true),
                          child: const Text('Thử lại'),
                        ),
                      ],
                    ),
                  )
                : _items.isEmpty
                ? _buildEmptyState()
                : RefreshIndicator(
                    color: _primaryContainer,
                    onRefresh: () => _load(refresh: true),
                    child: ListView.builder(
                      controller: _scrollController,
                      padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
                      itemCount: _items.length + (_hasMore ? 1 : 0),
                      itemBuilder: (context, index) {
                        if (index == _items.length) {
                          return const Padding(
                            padding: EdgeInsets.all(16),
                            child: Center(
                              child: CircularProgressIndicator(
                                color: _primaryContainer,
                              ),
                            ),
                          );
                        }
                        final request = _items[index];
                        final busy = _busyIds.contains(request.id);
                        return _buildRequestCard(request, busy);
                      },
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: const BoxDecoration(
                color: _surfaceHigh,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.inbox_outlined,
                size: 36,
                color: _primary,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Chưa có yêu cầu tư vấn được giao',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 6),
            const Text(
              'Các yêu cầu tư vấn được giao cho bạn sẽ xuất hiện ở đây.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: _outline,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRequestCard(ConsultationRequestSummary request, bool busy) {
    return Container(
      key: Key('consultation-${request.id}'),
      margin: const EdgeInsets.only(bottom: 14),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Row: Avatar + Patient Name + Status Badge
            Row(
              children: [
                AppUserAvatar(
                  radius: 20,
                  backgroundColor: _surfaceLow,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        request.counterpartDisplayName ??
                            'Người dùng CareBridge',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          color: _onSurface,
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Row(
                        children: [
                          const Icon(
                            Icons.schedule_outlined,
                            size: 13,
                            color: _outline,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            _timeAgo(request.createdAt),
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              color: _outline,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                _buildStatusBadge(request.status),
              ],
            ),
            const SizedBox(height: 14),

            // Topic Box
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: _surfaceLow,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.chat_bubble_outline_rounded,
                    size: 18,
                    color: _primary,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      request.topic,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: _onSurface,
                        height: 1.3,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 14),

            // Actions Row
            if (request.status == 'PENDING') ...[
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton.icon(
                      key: Key('accept-${request.id}'),
                      onPressed: busy ? null : () => _accept(request),
                      icon: busy
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.check_circle_outline, size: 18),
                      label: const Text('Chấp nhận'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _primary,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(20),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: OutlinedButton.icon(
                      key: Key('reject-${request.id}'),
                      onPressed: busy ? null : () => _reject(request),
                      icon: const Icon(Icons.close_rounded, size: 18),
                      label: const Text('Từ chối'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: _errColor,
                        side: const BorderSide(color: _errorContainer),
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(20),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
            ],

            // View Details Button
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                key: Key('view-details-${request.id}'),
                onPressed: busy
                    ? null
                    : () => context.push(
                        '/consultation-requests/${request.id}',
                      ),
                icon: const Icon(
                  Icons.chevron_right_rounded,
                  size: 20,
                  color: _primary,
                ),
                label: const Text(
                  'Xem chi tiết',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w700,
                    fontSize: 13,
                    color: _primary,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusBadge(String status) {
    late String label;
    late Color bg;
    late Color fg;
    late IconData icon;

    switch (status) {
      case 'PENDING':
        label = 'Đang chờ';
        bg = const Color(0xFFFFF3E0);
        fg = const Color(0xFFE65100);
        icon = Icons.schedule_rounded;
        break;
      case 'ACCEPTED':
        label = 'Đã nhận';
        bg = const Color(0xFFE6F4EA);
        fg = const Color(0xFF137333);
        icon = Icons.check_circle_rounded;
        break;
      case 'REJECTED':
        label = 'Bị từ chối';
        bg = _errorContainer;
        fg = _errColor;
        icon = Icons.highlight_off_rounded;
        break;
      case 'CANCELLED':
        label = 'Đã hủy';
        bg = const Color(0xFFF2EAE4);
        fg = _outline;
        icon = Icons.cancel_outlined;
        break;
      case 'EXPIRED':
        label = 'Hết hạn';
        bg = const Color(0xFFF2EAE4);
        fg = _outline;
        icon = Icons.timer_off_outlined;
        break;
      default:
        label = status;
        bg = _surfaceLow;
        fg = _onSurface;
        icon = Icons.info_outline;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: fg),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: fg,
            ),
          ),
        ],
      ),
    );
  }
}

class _FilterItem {
  final String label;
  final String? value;
  final IconData icon;

  const _FilterItem(this.label, this.value, this.icon);
}

String _timeAgo(DateTime value) {
  final diff = DateTime.now().difference(value);
  if (diff.inMinutes < 60) return 'Vừa gửi';
  if (diff.inHours < 24) return '${diff.inHours} giờ trước';
  return '${diff.inDays} ngày trước';
}

