import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/triage_history_model.dart';
import '../services/triage_service.dart';

class TriageHistoryScreen extends StatefulWidget {
  const TriageHistoryScreen({super.key, this.historyLoader});

  final Future<List<TriageHistoryItem>> Function()? historyLoader;

  @override
  State<TriageHistoryScreen> createState() => _TriageHistoryScreenState();
}

class _TriageHistoryScreenState extends State<TriageHistoryScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _surface = Color(0xFFF6F1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onVariant = Color(0xFF6F5A52);

  List<TriageHistoryItem> _items = const [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final items =
          await (widget.historyLoader?.call() ?? TriageService().listHistory());
      if (!mounted) return;
      setState(() {
        _items = items;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Không thể tải lịch sử kiểm tra. Vui lòng thử lại.';
      });
    }
  }

  String _formatDate(DateTime? value) {
    if (value == null) return 'Chưa có thời gian';
    final local = value.toLocal();
    final day = local.day.toString().padLeft(2, '0');
    final month = local.month.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$day/$month/${local.year} · $hour:$minute';
  }

  Color _riskColor(String? risk) => switch (risk) {
    'RED' => const Color(0xFFB3261E),
    'YELLOW' => const Color(0xFF9A6500),
    'GREEN' => const Color(0xFF2D6A4F),
    _ => _onVariant,
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        backgroundColor: _primaryDark,
        foregroundColor: Colors.white,
        title: const Text(
          'Lịch sử AI Triage',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
        actions: [
          IconButton(
            key: const Key('triage-history-refresh'),
            tooltip: 'Tải lại lịch sử',
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: _primary));
    }
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_rounded, color: _primary, size: 48),
              const SizedBox(height: 14),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: _onVariant, height: 1.4),
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _load,
                style: FilledButton.styleFrom(
                  backgroundColor: _primaryDark,
                  shape: const StadiumBorder(),
                ),
                child: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }
    if (_items.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.history_rounded, color: _primary, size: 56),
              const SizedBox(height: 16),
              const Text(
                'Chưa có lịch sử kiểm tra',
                style: TextStyle(
                  color: _onSurface,
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Các kết quả AI Triage sau khi hoàn tất sẽ được lưu tại đây.',
                textAlign: TextAlign.center,
                style: TextStyle(color: _onVariant, height: 1.4),
              ),
            ],
          ),
        ),
      );
    }
    return RefreshIndicator(
      color: _primary,
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 18, 16, 32),
        itemCount: _items.length,
        separatorBuilder: (_, _) => const SizedBox(height: 12),
        itemBuilder: (context, index) => _buildHistoryCard(_items[index]),
      ),
    );
  }

  Widget _buildHistoryCard(TriageHistoryItem item) {
    final risk = _riskColor(item.riskLevel);
    return Semantics(
      button: true,
      label: '${item.stageLabel}, ${item.statusLabel}',
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        child: InkWell(
          key: ValueKey('triage-history-${item.sessionId}'),
          borderRadius: BorderRadius.circular(24),
          onTap: () => context.push('/triage/result/${item.sessionId}'),
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: risk.withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    item.isCompleted
                        ? Icons.health_and_safety_rounded
                        : Icons.pending_actions_rounded,
                    color: risk,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.stageLabel,
                        style: const TextStyle(
                          color: _onSurface,
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        item.statusLabel,
                        style: TextStyle(
                          color: risk,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _formatDate(item.completedAt ?? item.createdAt),
                        style: const TextStyle(color: _onVariant, fontSize: 12),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right_rounded, color: _onVariant),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
