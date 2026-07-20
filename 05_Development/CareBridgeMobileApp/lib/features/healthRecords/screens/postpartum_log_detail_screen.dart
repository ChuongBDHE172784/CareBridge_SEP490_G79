import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/postpartum_log_model.dart';
import '../services/postpartum_log_service.dart';

class PostpartumLogDetailScreen extends StatefulWidget {
  const PostpartumLogDetailScreen({
    super.key,
    required this.logId,
    this.service,
  });

  final String logId;
  final PostpartumLogService? service;

  @override
  State<PostpartumLogDetailScreen> createState() =>
      _PostpartumLogDetailScreenState();
}

class _PostpartumLogDetailScreenState extends State<PostpartumLogDetailScreen> {
  static const _primary = Color(0xFF845143);
  late final PostpartumLogService _service;
  PostpartumLog? _log;
  bool _loading = true;
  bool _deleting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? PostpartumLogService();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final value = await _service.detail(widget.logId);
      if (mounted) setState(() => _log = value);
    } catch (_) {
      if (mounted) setState(() => _error = 'Chưa thể tải chi tiết nhật ký.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _delete() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Xóa nhật ký?'),
        content: const Text('Bản ghi sẽ không còn xuất hiện trong nhật ký.'),
        actions: [
          TextButton(
            onPressed: () => context.pop(false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => context.pop(true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (confirmed != true || _deleting) return;
    setState(() => _deleting = true);
    try {
      await _service.delete(widget.logId);
      if (mounted) context.pop(true);
    } catch (_) {
      if (mounted) setState(() => _error = 'Chưa thể xóa. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _deleting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF8F6),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFFF8F6),
        foregroundColor: _primary,
        title: const Text('Chi tiết hồi phục'),
        actions: [
          IconButton(
            tooltip: 'Chỉnh sửa',
            onPressed: _log == null
                ? null
                : () async {
                    final changed = await context.push<bool>(
                      '/postpartum-logs/${widget.logId}/edit',
                      extra: _log,
                    );
                    if (changed == true && mounted) await _load();
                  },
            icon: const Icon(Icons.edit_outlined),
          ),
          IconButton(
            tooltip: 'Xóa nhật ký',
            onPressed: _log == null || _deleting ? null : _delete,
            icon: const Icon(Icons.delete_outline_rounded),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _log == null
          ? Center(
              child: TextButton(
                onPressed: _load,
                child: Text('$_error Thử lại'),
              ),
            )
          : _buildDetail(),
    );
  }

  Widget _buildDetail() {
    final log = _log!;
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        if (_error != null) _message(_error!, const Color(0xFFFFDAD6)),
        if (log.redFlagAlert)
          _message(
            'Một số thông tin cần được đánh giá sớm. Hãy tìm hỗ trợ y tế nếu bạn thấy không an toàn.',
            const Color(0xFFFFDAD6),
          ),
        Card(
          elevation: 0,
          color: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
          ),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                _row('Ngày ghi nhận', _date(log.logDate)),
                _row(
                  'Mức đau',
                  log.painLevel == null ? 'Không ghi' : '${log.painLevel}/10',
                ),
                _row('Mức chảy máu', log.bleedingLevel ?? 'Không ghi'),
                _row(
                  'Tâm trạng',
                  log.moodLevel == null ? 'Không ghi' : '${log.moodLevel}/10',
                ),
                _row(
                  'Giấc ngủ',
                  log.sleepHours == null
                      ? 'Không ghi'
                      : '${log.sleepHours} giờ',
                ),
                _row('Ghi chú triệu chứng', log.symptomNote ?? 'Không ghi'),
                _row(
                  'Ghi chú cho con bú',
                  log.breastfeedingNote ?? 'Không ghi',
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        OutlinedButton.icon(
          onPressed: () => context.push('/postpartum-safety-help'),
          icon: const Icon(Icons.health_and_safety_outlined),
          label: const Text('Xem dấu hiệu cần hỗ trợ khẩn cấp'),
          style: OutlinedButton.styleFrom(
            minimumSize: const Size.fromHeight(56),
          ),
        ),
      ],
    );
  }

  Widget _row(String label, String value) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 10),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Text(label, style: const TextStyle(color: Color(0xFF7D6961))),
        ),
        Expanded(child: Text(value, textAlign: TextAlign.end)),
      ],
    ),
  );

  Widget _message(String text, Color color) => Semantics(
    liveRegion: true,
    child: Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Text(text),
    ),
  );

  String _date(DateTime value) =>
      '${value.day.toString().padLeft(2, '0')}/'
      '${value.month.toString().padLeft(2, '0')}/${value.year}';
}
