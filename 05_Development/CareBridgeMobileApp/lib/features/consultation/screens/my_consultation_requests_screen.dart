import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/consultation_request.dart';
import '../services/consultation_request_service.dart';

class MyConsultationRequestsScreen extends StatefulWidget {
  const MyConsultationRequestsScreen({super.key});

  @override
  State<MyConsultationRequestsScreen> createState() =>
      _MyConsultationRequestsScreenState();
}

class _MyConsultationRequestsScreenState
    extends State<MyConsultationRequestsScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerHigh = Color(0xFFF1E6E0);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outlineVariant = Color(0xFFE5D3CA);

  static const _filters = <String, String?>{
    'Tất cả': null,
    'Đang chờ': 'PENDING',
    'Đã nhận': 'ACCEPTED',
    'Bị từ chối': 'REJECTED',
    'Đã hủy': 'CANCELLED',
    'Hết hạn': 'EXPIRED',
  };

  String? _status;
  ConsultationRequestPage? _page;
  Object? _error;
  bool _loading = false;
  int _generation = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final generation = ++_generation;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await ConsultationRequestService.instance.listMine(
        status: _status,
      );
      if (!mounted || generation != _generation) return;
      setState(() => _page = page);
    } catch (error) {
      if (!mounted || generation != _generation) return;
      setState(() => _error = error);
    } finally {
      if (mounted && generation == _generation) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = _page?.items ?? const <ConsultationRequestSummary>[];
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _surface,
        elevation: 0,
        centerTitle: true,
        iconTheme: const IconThemeData(color: _onSurface),
        title: const Text(
          'Yêu cầu tư vấn của tôi',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
      ),
      body: Column(
        children: [
          Container(
            color: _surface,
            padding: const EdgeInsets.only(bottom: 12),
            child: SizedBox(
              height: 38,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                children: _filters.entries.map((entry) {
                  final selected = _status == entry.value;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(entry.key),
                      selected: selected,
                      onSelected: (_) {
                        setState(() => _status = entry.value);
                        _load();
                      },
                      selectedColor: _primary,
                      backgroundColor: _canvas,
                      elevation: selected ? 1 : 0,
                      shadowColor: const Color(0x1F845143),
                      labelStyle: TextStyle(
                        fontFamily: 'Lexend',
                        color: selected ? Colors.white : _onSurfaceVariant,
                        fontSize: 13,
                        fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      side: BorderSide(
                        color: selected ? Colors.transparent : _outlineVariant,
                      ),
                      shape: const StadiumBorder(),
                    ),
                  );
                }).toList(),
              ),
            ),
          ),
          Expanded(
            child: _loading && _page == null
                ? _buildSkeleton()
                : _error != null
                    ? _ErrorState(onRetry: _load)
                    : items.isEmpty
                        ? _buildEmptyState()
                        : RefreshIndicator(
                            color: _primary,
                            onRefresh: _load,
                            child: ListView.builder(
                              padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                              itemCount: items.length,
                              itemBuilder: (context, index) {
                                final item = items[index];
                                return _buildRequestCard(context, item);
                              },
                            ),
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildRequestCard(BuildContext context, ConsultationRequestSummary item) {
    final statusColor = _statusColor(item.status);
    final statusBg = _statusBgColor(item.status);

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
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
          key: Key('consultation-${item.id}'),
          borderRadius: BorderRadius.circular(20),
          onTap: () => context.push('/consultation-requests/${item.id}'),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: const BoxDecoration(
                    color: _surfaceContainerLow,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.question_answer_rounded,
                    color: _primary,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.topic,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                          letterSpacing: -0.2,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Chuyên gia: ${item.counterpartDisplayName ?? "Hệ thống"}',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          color: _onSurfaceVariant,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: statusBg,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          _statusLabel(item.status),
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: statusColor,
                          ),
                        ),
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
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
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
                Icons.assignment_late_rounded,
                size: 48,
                color: _primaryContainer,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Chưa có yêu cầu tư vấn nào',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Các yêu cầu tư vấn với bác sĩ và chuyên gia của bạn sẽ xuất hiện tại đây.',
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

  Widget _buildSkeleton() {
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      itemCount: 4,
      itemBuilder: (context, index) {
        return Container(
          margin: const EdgeInsets.only(bottom: 12),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: _outlineVariant),
          ),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
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
                      width: 100,
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

class _ErrorState extends StatelessWidget {
  final VoidCallback onRetry;
  const _ErrorState({required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: const BoxDecoration(
                color: Color(0xFFF8EEE9),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.cloud_off_rounded, size: 40, color: Color(0xFF845143)),
            ),
            const SizedBox(height: 16),
            const Text(
              'Không thể tải danh sách yêu cầu',
              style: TextStyle(
                fontFamily: 'Lexend',
                color: Color(0xFF2A211D),
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh_rounded, size: 18),
              label: const Text('Thử lại', style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600)),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF845143),
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
}

String _statusLabel(String status) => switch (status) {
      'PENDING' => 'Đang chờ',
      'ACCEPTED' => 'Đã nhận',
      'REJECTED' => 'Bị từ chối',
      'CANCELLED' => 'Đã hủy',
      'EXPIRED' => 'Hết hạn',
      _ => status,
    };

Color _statusColor(String status) => switch (status) {
      'PENDING' => const Color(0xFFD97706),
      'ACCEPTED' => const Color(0xFF10B981),
      'REJECTED' => const Color(0xFFEF4444),
      'CANCELLED' => const Color(0xFF6B7280),
      'EXPIRED' => const Color(0xFF9CA3AF),
      _ => const Color(0xFF845143),
    };

Color _statusBgColor(String status) => switch (status) {
      'PENDING' => const Color(0xFFFEF3C7),
      'ACCEPTED' => const Color(0xFFD1FAE5),
      'REJECTED' => const Color(0xFFFEE2E2),
      'CANCELLED' => const Color(0xFFF3F4F6),
      'EXPIRED' => const Color(0xFFF3F4F6),
      _ => const Color(0xFFF8EEE9),
    };

