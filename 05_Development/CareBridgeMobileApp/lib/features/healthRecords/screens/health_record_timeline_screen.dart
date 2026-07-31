import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/health_record_model.dart';
import '../services/health_record_service.dart';

/// CB-012 — Health Record Timeline (UC-39, UC-40, UC-41, UC-42, UC-211)
/// Shows all health records grouped by month with a vertical timeline.
/// Filter chips: Tất cả / Tiêm chủng / Chỉ số / Đơn thuốc.
/// Data: mock list (TODO: wire to GET /api/v1/health-records?journeyId=X).
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
  List<HealthRecord> _records = [];
  RecordType? _activeFilter;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await _service.listHealthRecords(filter: _activeFilter);
      if (mounted) {
        setState(() {
          _records = list;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Không thể tải dữ liệu.';
          _loading = false;
        });
      }
    }
  }

  void _setFilter(RecordType? type) {
    setState(() => _activeFilter = type);
    _load();
  }

  Map<String, List<HealthRecord>> _groupByMonth() {
    final map = <String, List<HealthRecord>>{};
    for (final r in _records) {
      final key = 'Tháng ${r.recordDate.month}, ${r.recordDate.year}';
      (map[key] ??= []).add(r);
    }
    return map;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            _buildFilterChips(),
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
        onPressed: () {
          context.push('/health-records/add').then((_) => _load());
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
          Expanded(
            child: Text(
              'Lịch sử sức khỏe',
              textAlign: TextAlign.center,
              style: const TextStyle(
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

  Widget _buildFilterChips() {
    return SizedBox(
      height: 52,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
        children: [
          _FilterChip(
            label: 'Tất cả',
            icon: Icons.check,
            selected: _activeFilter == null,
            onTap: () => _setFilter(null),
          ),
          const SizedBox(width: 8),
          _FilterChip(
            label: 'Tiêm chủng',
            icon: Icons.vaccines,
            selected: _activeFilter == RecordType.vaccination,
            onTap: () => _setFilter(RecordType.vaccination),
          ),
          const SizedBox(width: 8),
          _FilterChip(
            label: 'Chỉ số',
            icon: Icons.monitor_weight,
            selected: _activeFilter == RecordType.metric,
            onTap: () => _setFilter(RecordType.metric),
          ),
          const SizedBox(width: 8),
          _FilterChip(
            label: 'Đơn thuốc',
            icon: Icons.medication,
            selected: _activeFilter == RecordType.prescription,
            onTap: () => _setFilter(RecordType.prescription),
          ),
        ],
      ),
    );
  }

  Widget _buildTimeline() {
    final groups = _groupByMonth();
    if (groups.isEmpty) {
      return Center(
        child: Text(
          'Chưa có hồ sơ nào.',
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
            onTap: (r) {
              context.push('/health-records/detail/${r.id}').then((_) => _load());
            },
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

// ─── Filter chip ──────────────────────────────────────────────────────────────
class _FilterChip extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  const _FilterChip({
    required this.label,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFC98C7B) : const Color(0xFFFFF8F6),
          borderRadius: BorderRadius.circular(99),
          border: selected ? null : Border.all(color: const Color(0xFFD6C2BD)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 18,
              color: selected ? Colors.white : const Color(0xFF524440),
            ),
            const SizedBox(width: 6),
            Text(
              label,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: selected ? Colors.white : const Color(0xFF524440),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Month group with timeline ─────────────────────────────────────────────────
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
      final r = records[i];
      // Record row: dot on the left, card on the right.
      items.add(
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 40,
              child: Align(alignment: Alignment.topCenter, child: _dot(i == 0)),
            ),
            Expanded(
              child: _RecordCard(record: r, onTap: () => onTap(r)),
            ),
          ],
        ),
      );
      // Connector between records: fixed-height 2px line centred under the dot column.
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

  String _formatDate(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')} Th${d.month}';

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFFFFF8F6),
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
                Row(
                  children: [
                    const Icon(
                      Icons.calendar_today,
                      size: 16,
                      color: Color(0xFF524440),
                    ),
                    const SizedBox(width: 4),
                    Text(
                      _formatDate(record.recordDate),
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: Color(0xFF524440),
                      ),
                    ),
                  ],
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
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: const [
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
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
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
                    Text(
                      record.facilityName!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        color: Color(0xFF6E5A52),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
