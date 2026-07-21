import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/postpartum_log_model.dart';
import '../services/postpartum_log_service.dart';

class PostpartumLogListScreen extends StatefulWidget {
  const PostpartumLogListScreen({
    super.key,
    required this.journeyId,
    this.service,
  });

  final String journeyId;
  final PostpartumLogService? service;

  @override
  State<PostpartumLogListScreen> createState() =>
      _PostpartumLogListScreenState();
}

class _PostpartumLogListScreenState extends State<PostpartumLogListScreen> {
  static const _primary = Color(0xFF845143);
  static const _accent = Color(0xFFC98C7B);
  late final PostpartumLogService _service;
  final List<PostpartumLog> _logs = [];
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;
  int _page = 0;
  bool _hasNext = false;
  int _loadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? PostpartumLogService();
    _load();
  }

  Future<void> _load({bool more = false}) async {
    if (more && (!_hasNext || _loading || _loadingMore)) return;
    final generation = more ? _loadGeneration : ++_loadGeneration;
    setState(() {
      if (more) {
        _loadingMore = true;
      } else {
        _loading = true;
        _loadingMore = false;
        _error = null;
      }
    });
    try {
      final nextPage = more ? _page + 1 : 0;
      final result = await _service.list(widget.journeyId, page: nextPage);
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _error = null;
        if (!more) _logs.clear();
        _logs.addAll(result.items);
        _page = result.page;
        _hasNext = result.hasNext;
      });
    } catch (_) {
      if (mounted && generation == _loadGeneration) {
        setState(() => _error = 'Chưa thể tải nhật ký hồi phục.');
      }
    } finally {
      if (mounted && generation == _loadGeneration) {
        setState(() {
          _loading = false;
          _loadingMore = false;
        });
      }
    }
  }

  Future<void> _openCreate() async {
    final changed = await context.push<bool>(
      '/postpartum-logs/new?journeyId=${Uri.encodeComponent(widget.journeyId)}',
    );
    if (changed == true && mounted) await _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF8F6),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFFF8F6),
        foregroundColor: _primary,
        title: const Text('Nhật ký hồi phục'),
        actions: [
          IconButton(
            tooltip: 'Dấu hiệu cần hỗ trợ khẩn cấp',
            onPressed: () => context.push('/postpartum-safety-help'),
            icon: const Icon(Icons.health_and_safety_outlined),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        color: _accent,
        child: _buildBody(),
      ),
      floatingActionButton: FloatingActionButton.extended(
        key: const Key('postpartum-log-add'),
        onPressed: _openCreate,
        backgroundColor: _accent,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: const Text('Thêm nhật ký'),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: _accent));
    }
    if (_error != null && _logs.isEmpty) {
      return ListView(
        children: [
          const SizedBox(height: 120),
          Icon(Icons.cloud_off_rounded, size: 52, color: _primary),
          const SizedBox(height: 12),
          Text(_error!, textAlign: TextAlign.center),
          Center(
            child: TextButton(onPressed: _load, child: const Text('Thử lại')),
          ),
        ],
      );
    }
    if (_logs.isEmpty) {
      return ListView(
        children: const [
          SizedBox(height: 120),
          Icon(Icons.menu_book_outlined, size: 52, color: _primary),
          SizedBox(height: 12),
          Text(
            'Chưa có nhật ký. Bạn có thể bắt đầu khi sẵn sàng.',
            textAlign: TextAlign.center,
          ),
        ],
      );
    }
    final hasPartialError = _error != null;
    final offset = hasPartialError ? 1 : 0;
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 100),
      itemCount: _logs.length + (_hasNext ? 1 : 0) + offset,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        if (hasPartialError && index == 0) {
          return Semantics(
            liveRegion: true,
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF0ED),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  const Expanded(
                    child: Text('Một phần dữ liệu chưa tải được.'),
                  ),
                  TextButton(onPressed: _load, child: const Text('Thử lại')),
                ],
              ),
            ),
          );
        }
        final logIndex = index - offset;
        if (logIndex == _logs.length) {
          return Center(
            child: TextButton(
              onPressed: _loadingMore ? null : () => _load(more: true),
              child: Text(_loadingMore ? 'Đang tải…' : 'Tải thêm'),
            ),
          );
        }
        final log = _logs[logIndex];
        return Semantics(
          button: true,
          label: 'Nhật ký hồi phục ngày ${_date(log.logDate)}',
          child: Card(
            elevation: 0,
            color: Colors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(22),
            ),
            child: ListTile(
              minTileHeight: 72,
              leading: const CircleAvatar(
                backgroundColor: Color(0xFFFFE2D9),
                child: Icon(Icons.self_improvement_rounded, color: _primary),
              ),
              title: Text(_date(log.logDate)),
              subtitle: Text(_summary(log)),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: () async {
                final changed = await context.push<bool>(
                  '/postpartum-logs/${Uri.encodeComponent(log.id)}',
                );
                if (changed == true && mounted) await _load();
              },
            ),
          ),
        );
      },
    );
  }

  String _summary(PostpartumLog log) {
    final parts = <String>[
      if (log.painLevel != null) 'Đau ${log.painLevel}/10',
      if (log.moodLevel != null) 'Tâm trạng ${log.moodLevel}/10',
      if (log.sleepHours != null) 'Ngủ ${log.sleepHours} giờ',
    ];
    return parts.isEmpty ? 'Đã ghi nhận thông tin hồi phục' : parts.join(' • ');
  }

  String _date(DateTime value) =>
      '${value.day.toString().padLeft(2, '0')}/'
      '${value.month.toString().padLeft(2, '0')}/${value.year}';
}
