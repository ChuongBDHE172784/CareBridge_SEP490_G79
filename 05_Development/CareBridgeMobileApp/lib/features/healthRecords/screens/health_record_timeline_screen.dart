import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/health_record_model.dart';
import '../services/health_record_service.dart';
import 'health_record_attachment_detail_screen.dart';

/// CB-012 - Health Record Timeline (UC-39, UC-40, UC-41, UC-42, UC-211)
/// Shows health record attachments grouped by posted month with search controls.
class HealthRecordTimelineScreen extends StatefulWidget {
  const HealthRecordTimelineScreen({super.key});

  @override
  State<HealthRecordTimelineScreen> createState() =>
      _HealthRecordTimelineScreenState();
}

class _HealthRecordTimelineScreenState
    extends State<HealthRecordTimelineScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = HealthRecordService();
  final _searchController = TextEditingController();

  List<HealthRecord> _records = [];
  String _searchQuery = '';
  DateTime? _postedDate;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await _service.listHealthRecords();
      if (!mounted) return;
      setState(() {
        _records = list;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Không thể tải dữ liệu.';
        _loading = false;
      });
    }
  }

  Future<void> _openRecordAttachments(HealthRecord record) async {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(
        child: CircularProgressIndicator(color: _primaryContainer),
      ),
    );

    HealthRecordDetail detail;
    try {
      detail = await _service.getHealthRecord(record.id);
    } catch (_) {
      if (mounted) {
        Navigator.of(context, rootNavigator: true).pop();
        _showSnack('Không thể tải chi tiết hồ sơ.');
      }
      return;
    }

    if (!mounted) return;
    Navigator.of(context, rootNavigator: true).pop();

    final result = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => HealthRecordAttachmentDetailScreen(
          record: detail,
        ),
      ),
    );
    if (result == true && mounted) {
      _load();
    }
  }

  Future<void> _pickPostedDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _postedDate ?? now,
      firstDate: DateTime(2020),
      lastDate: now,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: _primaryContainer,
              onPrimary: Colors.white,
              onSurface: Color(0xFF271812),
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked == null || !mounted) return;
    setState(() => _postedDate = picked);
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message), backgroundColor: _primary));
  }

  DateTime _postedAt(HealthRecord record) => record.recordDate;

  bool _isSameDay(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  List<HealthRecord> get _visibleRecords {
    final query = _searchQuery.trim().toLowerCase();
    return _records.where((record) {
      final matchesName =
          query.isEmpty || record.title.toLowerCase().contains(query);
      final matchesDate =
          _postedDate == null || _isSameDay(_postedAt(record), _postedDate!);
      return matchesName && matchesDate;
    }).toList();
  }

  Map<String, List<HealthRecord>> _groupByMonth() {
    final map = <String, List<HealthRecord>>{};
    for (final record in _visibleRecords) {
      final postedAt = _postedAt(record);
      final key = 'Tháng ${postedAt.month}, ${postedAt.year}';
      (map[key] ??= []).add(record);
    }
    return map;
  }

  String _formatFullDate(DateTime date) =>
      '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            _buildSearchControls(),
            Expanded(
              child: _loading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
                  : _error != null
                  ? _buildError()
                  : _buildTimeline(),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: _primaryContainer,
        foregroundColor: Colors.white,
        shape: const CircleBorder(),
        onPressed: () async {
          final created = await context.push<bool>('/health-records/add');
          if (created == true && mounted) {
            _load();
          }
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildAppBar() {
    return SizedBox(
      height: 48,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
          ),
          const Expanded(
            child: Text(
              'Lịch sử sức khỏe',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildSearchControls() {
    final hasPostedDate = _postedDate != null;
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
      child: Column(
        children: [
          TextField(
            controller: _searchController,
            onChanged: (value) => setState(() => _searchQuery = value),
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(
              hintText: 'Tìm theo tên hồ sơ',
              prefixIcon: const Icon(
                Icons.search_rounded,
                color: _onSurfaceVariant,
              ),
              suffixIcon: _searchQuery.isEmpty
                  ? null
                  : IconButton(
                      tooltip: 'Xóa tìm kiếm',
                      onPressed: () {
                        _searchController.clear();
                        setState(() => _searchQuery = '');
                      },
                      icon: const Icon(
                        Icons.close_rounded,
                        color: _onSurfaceVariant,
                      ),
                    ),
              filled: true,
              fillColor: Colors.white,
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 16,
                vertical: 14,
              ),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(18),
                borderSide: const BorderSide(color: Color(0xFFE8DAD6)),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(18),
                borderSide: const BorderSide(color: Color(0xFFE8DAD6)),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(18),
                borderSide: const BorderSide(color: _primaryContainer),
              ),
            ),
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: Color(0xFF271812),
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _pickPostedDate,
                  icon: const Icon(Icons.calendar_today_rounded, size: 18),
                  label: Text(
                    hasPostedDate
                        ? 'Ngày đăng ${_formatFullDate(_postedDate!)}'
                        : 'Lọc theo ngày đăng',
                    overflow: TextOverflow.ellipsis,
                  ),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _onSurfaceVariant,
                    backgroundColor: Colors.white,
                    side: const BorderSide(color: Color(0xFFE8DAD6)),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(18),
                    ),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 13,
                    ),
                    textStyle: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ),
              if (hasPostedDate) ...[
                const SizedBox(width: 8),
                IconButton.filledTonal(
                  tooltip: 'Bỏ lọc ngày',
                  onPressed: () => setState(() => _postedDate = null),
                  icon: const Icon(Icons.close_rounded),
                  style: IconButton.styleFrom(
                    foregroundColor: _primary,
                    backgroundColor: const Color(0xFFFFE2D9),
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTimeline() {
    final groups = _groupByMonth();
    if (groups.isEmpty) {
      final hasSearch = _searchQuery.trim().isNotEmpty || _postedDate != null;
      return Center(
        child: Text(
          hasSearch ? 'Không tìm thấy hồ sơ phù hợp.' : 'Chưa có hồ sơ nào.',
          style: const TextStyle(
            fontFamily: 'Lexend',
            color: _onSurfaceVariant,
          ),
        ),
      );
    }
    return RefreshIndicator(
      color: _primaryContainer,
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(24, 8, 24, 100),
        children: groups.entries.map((entry) {
          return _MonthGroup(
            month: entry.key,
            records: entry.value,
            onTap: _openRecordAttachments,
          );
        }).toList(),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: Color(0xFFBA1A1A)),
          const SizedBox(height: 12),
          Text(
            _error!,
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          TextButton(
            onPressed: _load,
            child: const Text(
              'Thử lại',
              style: TextStyle(fontFamily: 'Lexend', color: _primary),
            ),
          ),
        ],
      ),
    );
  }
}

class _MonthGroup extends StatelessWidget {
  final String month;
  final List<HealthRecord> records;
  final void Function(HealthRecord) onTap;

  const _MonthGroup({
    required this.month,
    required this.records,
    required this.onTap,
  });

  Widget _dot(bool isFirst) => Container(
    width: 20,
    height: 20,
    decoration: BoxDecoration(
      color: isFirst ? const Color(0xFFC98C7B) : const Color(0xFFFADCD3),
      shape: BoxShape.circle,
      border: Border.all(color: const Color(0xFFFFF8F6), width: 3),
    ),
  );

  @override
  Widget build(BuildContext context) {
    final items = <Widget>[];
    for (int i = 0; i < records.length; i++) {
      final record = records[i];
      items.add(
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 40,
              child: Align(alignment: Alignment.topCenter, child: _dot(i == 0)),
            ),
            Expanded(
              child: _RecordCard(record: record, onTap: () => onTap(record)),
            ),
          ],
        ),
      );
      if (i < records.length - 1) {
        items.add(
          SizedBox(
            height: 16,
            child: Row(
              children: [
                SizedBox(
                  width: 40,
                  child: Center(
                    child: Container(width: 2, color: const Color(0xFFFFE2D9)),
                  ),
                ),
                const Expanded(child: SizedBox.shrink()),
              ],
            ),
          ),
        );
      }
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 12, top: 8),
          child: Text(
            month,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: Color(0xFF271812),
            ),
          ),
        ),
        ...items,
        const SizedBox(height: 24),
      ],
    );
  }
}

class _RecordCard extends StatelessWidget {
  final HealthRecord record;
  final VoidCallback onTap;

  const _RecordCard({required this.record, required this.onTap});

  DateTime get _postedAt => record.recordDate;

  String _formatDate(DateTime date) =>
      '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFFFFF8F6),
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF5A463F).withAlpha(15),
                blurRadius: 20,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Flexible(
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(
                          Icons.upload_file_rounded,
                          size: 16,
                          color: Color(0xFF524440),
                        ),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            'Đăng ${_formatDate(_postedAt)}',
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 12,
                              color: Color(0xFF524440),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (record.isShared)
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFE2D9),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.share, size: 14, color: Color(0xFF845143)),
                          SizedBox(width: 4),
                          Text(
                            'Đã chia sẻ',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 10,
                              color: Color(0xFF845143),
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                record.title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF271812),
                ),
              ),
              if (record.facilityName != null) ...[
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF1EC),
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.local_hospital,
                        size: 14,
                        color: Color(0xFF6E5A52),
                      ),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          record.facilityName!,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            color: Color(0xFF6E5A52),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
