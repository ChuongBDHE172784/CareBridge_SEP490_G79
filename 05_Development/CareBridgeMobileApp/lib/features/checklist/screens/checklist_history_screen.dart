import 'package:flutter/material.dart';

import '../../../core/network/api_client.dart';
import '../models/checklist_history_model.dart';
import '../services/checklist_history_service.dart';

class ChecklistHistoryScreen extends StatefulWidget {
  const ChecklistHistoryScreen({super.key, this.service, this.careGroupId});

  final ChecklistHistoryService? service;
  final String? careGroupId;

  @override
  State<ChecklistHistoryScreen> createState() => _ChecklistHistoryScreenState();
}

class _ChecklistHistoryScreenState extends State<ChecklistHistoryScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _muted = Color(0xFF5A463F);
  static const _subtle = Color(0xFF9C857C);

  late ChecklistHistoryService _service;
  ChecklistHistoryTargetSubject? _filter;
  List<ChecklistHistoryItem> _items = [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = false;
  String? _error;
  int _page = 0;
  int _requestGeneration = 0;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? ChecklistHistoryService.instance;
    _load(reset: true);
  }

  @override
  void didUpdateWidget(covariant ChecklistHistoryScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.service != widget.service) {
      _service = widget.service ?? ChecklistHistoryService.instance;
      _load(reset: true);
    }
    if (oldWidget.careGroupId != widget.careGroupId) {
      _load(reset: true);
    }
  }

  Future<void> _load({required bool reset}) async {
    final requestGeneration = ++_requestGeneration;
    if (reset) {
      setState(() {
        _loading = true;
        _error = null;
        _page = 0;
        _hasMore = false;
        _items = [];
      });
    }
    try {
      final pageToLoad = reset ? 0 : _page + 1;
      final page = await _service.loadHistory(
        page: pageToLoad,
        size: 20,
        targetSubject: _filter,
        careGroupId: widget.careGroupId,
      );
      if (!mounted || requestGeneration != _requestGeneration) return;
      setState(() {
        _items = reset ? page.items : [..._items, ...page.items];
        _page = page.page;
        _hasMore = page.hasNextPage;
        _loading = false;
        _loadingMore = false;
        _error = null;
      });
    } catch (error) {
      if (!mounted || requestGeneration != _requestGeneration) return;
      setState(() {
        _loading = false;
        _loadingMore = false;
        _error = error.toString();
        if (error is ApiException &&
            (error.statusCode == 401 ||
                error.statusCode == 403 ||
                error.statusCode == 404)) {
          _items = [];
          _hasMore = false;
        }
      });
    }
  }

  Future<void> _refresh() => _load(reset: true);

  Future<void> _loadMore() async {
    if (_loadingMore || !_hasMore) return;
    setState(() => _loadingMore = true);
    await _load(reset: false);
  }

  void _setFilter(ChecklistHistoryTargetSubject? filter) {
    if (_filter == filter) return;
    setState(() => _filter = filter);
    _load(reset: true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _muted,
        elevation: 0,
        title: const Text(
          'Lịch sử checklist',
          style: TextStyle(
            fontFamily: 'Quicksand',
            fontWeight: FontWeight.w800,
            color: _muted,
          ),
        ),
      ),
      body: SafeArea(
        top: false,
        child: RefreshIndicator(
          color: _primary,
          onRefresh: _refresh,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
            children: [
              _buildFilterBar(),
              const SizedBox(height: 16),
              if (_loading && _items.isEmpty)
                const _LoadingState()
              else if (_error != null && _items.isEmpty)
                _ErrorState(error: _error!, onRetry: _refresh)
              else ...[
                if (_error != null)
                  _InfoBanner(
                    text: 'Không tải thêm được lịch sử. Vuốt để thử lại.',
                  ),
                if (_items.isEmpty)
                  const _EmptyState()
                else
                  ..._items.map(_buildItemCard),
                if (_hasMore) ...[
                  const SizedBox(height: 16),
                  OutlinedButton.icon(
                    key: const Key('checklist-history-load-more'),
                    onPressed: _loadingMore ? null : _loadMore,
                    icon: _loadingMore
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.expand_more_rounded),
                    label: Text(_loadingMore ? 'Đang tải...' : 'Tải thêm'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: _primary,
                      side: const BorderSide(color: Color(0xFFD6C2BD)),
                      shape: const StadiumBorder(),
                      minimumSize: const Size.fromHeight(48),
                    ),
                  ),
                ],
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFilterBar() {
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: [
        ChoiceChip(
          label: const Text('Tất cả'),
          selected: _filter == null,
          onSelected: (_) => _setFilter(null),
          selectedColor: const Color(0xFFC98C7B),
          labelStyle: TextStyle(
            color: _filter == null ? Colors.white : _muted,
            fontWeight: FontWeight.w700,
          ),
        ),
        ...ChecklistHistoryTargetSubject.values.map(
          (subject) => ChoiceChip(
            label: Text(subject.label),
            selected: _filter == subject,
            onSelected: (_) => _setFilter(subject),
            selectedColor: const Color(0xFFC98C7B),
            labelStyle: TextStyle(
              color: _filter == subject ? Colors.white : _muted,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildItemCard(ChecklistHistoryItem item) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Semantics(
        container: true,
        label:
            '${item.templateName ?? item.stageLabel} - ${item.subjectLabel} - ${item.tasks.length} việc',
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF5A463F).withAlpha(12),
                blurRadius: 16,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Icon(
                                item.targetSubject ==
                                        ChecklistHistoryTargetSubject.baby
                                    ? Icons.child_care_rounded
                                    : Icons.pregnant_woman_rounded,
                                color: _primary,
                                size: 22,
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  item.templateName ?? item.stageLabel,
                                  style: const TextStyle(
                                    fontFamily: 'Quicksand',
                                    fontSize: 18,
                                    fontWeight: FontWeight.w800,
                                    color: _muted,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            [
                              item.subjectLabel,
                              if (item.careContextLabel != null)
                                item.careContextLabel!,
                              if (item.historicalAt != null)
                                _formatDate(item.historicalAt!),
                            ].join(' • '),
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              color: _subtle,
                              height: 1.35,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 12),
                    _StageBadge(label: item.stageLabel),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  item.historyReasonCode ?? 'Lịch sử checklist',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: _subtle,
                  ),
                ),
                const SizedBox(height: 12),
                ...item.tasks.map(
                  (task) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: _TaskHistoryRow(task: task),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _formatDate(DateTime value) {
    final local = value.toLocal();
    final day = local.day.toString().padLeft(2, '0');
    final month = local.month.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$hour:$minute $day/$month/${local.year}';
  }
}

class _StageBadge extends StatelessWidget {
  const _StageBadge({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFF2EAE4),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 12,
          fontWeight: FontWeight.w700,
          color: Color(0xFF845143),
        ),
      ),
    );
  }
}

class _TaskHistoryRow extends StatelessWidget {
  const _TaskHistoryRow({required this.task});

  final ChecklistHistoryTask task;

  @override
  Widget build(BuildContext context) {
    final completed = task.isCompleted;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(
          completed ? Icons.check_circle_rounded : Icons.radio_button_unchecked,
          color: completed ? const Color(0xFF2E7D32) : const Color(0xFF9C857C),
          size: 18,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                task.title,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: completed
                      ? const Color(0xFF271812)
                      : const Color(0xFF524440),
                ),
              ),
              const SizedBox(height: 2),
              Text(
                task.statusLabel,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: Color(0xFF9C857C),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _LoadingState extends StatelessWidget {
  const _LoadingState();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 48),
      child: Center(child: CircularProgressIndicator()),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 48),
      child: Center(
        child: Text(
          'Chưa có lịch sử checklist.',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 15,
            color: Color(0xFF9C857C),
          ),
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.error, required this.onRetry});

  final String error;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 36),
      child: Column(
        children: [
          Text(
            error,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: Color(0xFF845143),
            ),
          ),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    );
  }
}

class _InfoBanner extends StatelessWidget {
  const _InfoBanner({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Text(
          text,
          style: const TextStyle(
            fontFamily: 'Lexend',
            color: Color(0xFF845143),
          ),
        ),
      ),
    );
  }
}
