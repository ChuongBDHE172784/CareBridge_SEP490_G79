import 'package:flutter/material.dart';

import '../../directChat/models/checklist_share_data.dart';
import '../../directChat/models/health_metrics_share_data.dart';
import '../models/expert_shared_record_model.dart';
import '../services/expert_shared_records_service.dart';
import '../widgets/expert_checklist_form_dialog.dart';

class ExpertSharedRecordsScreen extends StatefulWidget {
  final ExpertSharedRecordsService? service;
  const ExpertSharedRecordsScreen({super.key, this.service});

  @override
  State<ExpertSharedRecordsScreen> createState() => _ExpertSharedRecordsScreenState();
}

class _ExpertSharedRecordsScreenState extends State<ExpertSharedRecordsScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outlineVariant = Color(0xFFE5D3CA);

  List<SharedRecordEntry> _allRecords = [];
  bool _loading = true;
  String? _error;

  SharedRecordTabType _activeTab = SharedRecordTabType.all;
  final SharedRecordAlertFilter _alertFilter = SharedRecordAlertFilter.all;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final records = await (widget.service ?? ExpertSharedRecordsService.instance).fetchExpertSharedRecords();
      if (!mounted) return;
      setState(() {
        _allRecords = records;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Không thể tải danh sách hồ sơ chia sẻ: $e';
      });
    }
  }

  List<MotherSharedCardData> get _filteredMotherCards {
    // Group records by motherUserId
    final map = <String, List<SharedRecordEntry>>{};
    for (final r in _allRecords) {
      map.putIfAbsent(r.motherUserId, () => []).add(r);
    }

    final cards = <MotherSharedCardData>[];
    for (final entry in map.entries) {
      final list = entry.value;
      if (list.isEmpty) continue;

      final first = list.first;
      final healthRecords = list.where((r) => r.type == SharedRecordType.healthMetrics).toList();
      final checklistRecords = list.where((r) => r.type == SharedRecordType.checklist).toList();

      // Tab filter
      if (_activeTab == SharedRecordTabType.healthMetrics && healthRecords.isEmpty) continue;
      if (_activeTab == SharedRecordTabType.checklist && checklistRecords.isEmpty) continue;

      // Overall alert level
      var overallAlert = SharedRecordAlertLevel.normal;
      for (final r in list) {
        if (r.alertLevel == SharedRecordAlertLevel.critical) {
          overallAlert = SharedRecordAlertLevel.critical;
          break;
        }
        if (r.alertLevel == SharedRecordAlertLevel.warning) {
          overallAlert = SharedRecordAlertLevel.warning;
        }
      }

      // Alert filter
      if (_alertFilter == SharedRecordAlertFilter.critical &&
          overallAlert != SharedRecordAlertLevel.critical) {
        continue;
      }
      if (_alertFilter == SharedRecordAlertFilter.warning &&
          overallAlert != SharedRecordAlertLevel.warning) {
        continue;
      }
      if (_alertFilter == SharedRecordAlertFilter.normal &&
          overallAlert != SharedRecordAlertLevel.normal) {
        continue;
      }

      // Search filter
      if (_searchQuery.trim().isNotEmpty) {
        final q = _searchQuery.trim().toLowerCase();
        final nameMatch = first.motherName.toLowerCase().contains(q);
        final idMatch = first.motherUserId.toLowerCase().contains(q);
        if (!nameMatch && !idMatch) continue;
      }

      final latestHealth = healthRecords.isNotEmpty ? healthRecords.first : null;
      final latestChecklist = checklistRecords.isNotEmpty ? checklistRecords.first : null;

      final week = latestChecklist?.checklistData?.gestationalWeek ??
          latestHealth?.healthData?.gestationalWeek;
      final stageLabel = latestChecklist?.checklistData?.stageLabel;

      cards.add(
        MotherSharedCardData(
          motherUserId: entry.key,
          conversationId: first.conversationId,
          conversationStatus: first.conversationStatus,
          motherName: first.motherName,
          motherAvatar: first.motherAvatar,
          motherPhone: first.motherPhone,
          lastActiveAt: first.createdAt,
          gestationalWeek: week,
          stageLabel: stageLabel,
          overallAlertLevel: overallAlert,
          latestHealthRecord: latestHealth,
          latestChecklistRecord: latestChecklist,
          allHealthRecords: healthRecords,
          allChecklistRecords: checklistRecords,
        ),
      );
    }

    cards.sort((a, b) => b.lastActiveAt.compareTo(a.lastActiveAt));
    return cards;
  }

  void _openChecklistInspection(MotherSharedCardData mother) {
    final record = mother.latestChecklistRecord;
    if (record == null || record.checklistData == null) return;

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => _ChecklistInspectionSheet(
        mother: mother,
        record: record,
        onUpdate: () => _loadData(),
      ),
    );
  }

  void _openHealthInspection(MotherSharedCardData mother) {
    final record = mother.latestHealthRecord;
    if (record == null || record.healthData == null) return;

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _HealthInspectionSheet(
        mother: mother,
        record: record,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cards = _filteredMotherCards;

    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _surface,
        elevation: 0,
        title: const Text(
          'Hồ sơ & Checklist chia sẻ',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.w700,
            fontSize: 18,
            color: _onSurface,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: _primary),
            onPressed: _loadData,
          ),
        ],
      ),
      body: Column(
        children: [
          // Search & Filter header
          Container(
            color: _surface,
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
            child: Column(
              children: [
                // Search field
                TextField(
                  onChanged: (val) => setState(() => _searchQuery = val),
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                  decoration: InputDecoration(
                    hintText: 'Tìm mẹ bầu theo tên hoặc mã hồ sơ...',
                    hintStyle: const TextStyle(color: Colors.black38, fontSize: 13),
                    prefixIcon: const Icon(Icons.search_rounded, size: 20, color: _primary),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                    filled: true,
                    fillColor: _canvas,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                // Tabs
                Row(
                  children: [
                    _buildTabChip('Tất cả (${_allRecords.length})', SharedRecordTabType.all),
                    const SizedBox(width: 8),
                    _buildTabChip('Chỉ số', SharedRecordTabType.healthMetrics),
                    const SizedBox(width: 8),
                    _buildTabChip('Checklist', SharedRecordTabType.checklist),
                  ],
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: _outlineVariant),

          // Body
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator(color: _primary))
                : _error != null
                    ? Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(_error!, style: const TextStyle(fontFamily: 'Lexend', color: Colors.red)),
                            const SizedBox(height: 12),
                            ElevatedButton(onPressed: _loadData, child: const Text('Thử lại')),
                          ],
                        ),
                      )
                    : cards.isEmpty
                        ? Center(
                            child: Padding(
                              padding: const EdgeInsets.all(24),
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Container(
                                    padding: const EdgeInsets.all(16),
                                    decoration: const BoxDecoration(
                                      color: _surfaceContainerLow,
                                      shape: BoxShape.circle,
                                    ),
                                    child: const Icon(
                                      Icons.checklist_rtl_rounded,
                                      size: 40,
                                      color: _primaryContainer,
                                    ),
                                  ),
                                  const SizedBox(height: 16),
                                  const Text(
                                    'Không tìm thấy hồ sơ nào',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 16,
                                      fontWeight: FontWeight.w700,
                                      color: _onSurface,
                                    ),
                                  ),
                                  const SizedBox(height: 6),
                                  const Text(
                                    'Khi mẹ bầu chia sẻ checklist hoặc chỉ số sức khỏe qua tin nhắn, hồ sơ sẽ xuất hiện tại đây.',
                                    textAlign: TextAlign.center,
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 12,
                                      color: _onSurfaceVariant,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.fromLTRB(16, 14, 16, 24),
                            itemCount: cards.length,
                            itemBuilder: (ctx, i) {
                              final mother = cards[i];
                              return _MotherSummaryCard(
                                mother: mother,
                                onOpenChecklist: () => _openChecklistInspection(mother),
                                onOpenHealth: () => _openHealthInspection(mother),
                              );
                            },
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildTabChip(String label, SharedRecordTabType tab) {
    final isSelected = _activeTab == tab;
    return InkWell(
      onTap: () => setState(() => _activeTab = tab),
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isSelected ? _primary : _surfaceContainerLow,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 11,
            fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
            color: isSelected ? Colors.white : _onSurface,
          ),
        ),
      ),
    );
  }
}

class _MotherSummaryCard extends StatefulWidget {
  final MotherSharedCardData mother;
  final VoidCallback onOpenChecklist;
  final VoidCallback onOpenHealth;

  const _MotherSummaryCard({
    required this.mother,
    required this.onOpenChecklist,
    required this.onOpenHealth,
  });

  @override
  State<_MotherSummaryCard> createState() => _MotherSummaryCardState();
}

class _MotherSummaryCardState extends State<_MotherSummaryCard> {
  late String _subTab; // 'HEALTH' or 'CHECKLIST'

  @override
  void initState() {
    super.initState();
    if (widget.mother.latestHealthRecord != null) {
      _subTab = 'HEALTH';
    } else {
      _subTab = 'CHECKLIST';
    }
  }

  @override
  Widget build(BuildContext context) {
    const primary = Color(0xFF845143);
    const surface = Color(0xFFFFFCF9);
    const onSurface = Color(0xFF2A211D);
    const outlineVariant = Color(0xFFE5D3CA);

    final checklist = widget.mother.latestChecklistRecord?.checklistData;
    final HealthMetricsShareData? health = widget.mother.latestHealthRecord?.healthData;
    final hasBoth = checklist != null && health != null;

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      decoration: BoxDecoration(
        color: surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: outlineVariant),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A845143),
            blurRadius: 10,
            offset: Offset(0, 3),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Mother Header Row
            Row(
              children: [
                CircleAvatar(
                  radius: 22,
                  backgroundColor: const Color(0xFFF8EEE9),
                  backgroundImage: widget.mother.motherAvatar != null
                      ? NetworkImage(widget.mother.motherAvatar!)
                      : null,
                  child: widget.mother.motherAvatar == null
                      ? const Icon(Icons.person, color: primary, size: 24)
                      : null,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              widget.mother.motherName,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 15,
                                fontWeight: FontWeight.w700,
                                color: onSurface,
                              ),
                            ),
                          ),
                          if (widget.mother.overallAlertLevel == SharedRecordAlertLevel.critical)
                            _buildAlertBadge('Nguy cơ', const Color(0xFFFFEBEE), Colors.red)
                          else if (widget.mother.overallAlertLevel == SharedRecordAlertLevel.warning)
                            _buildAlertBadge('Cảnh báo', const Color(0xFFFFF8E1), Colors.orange)
                          else
                            _buildAlertBadge('Bình thường', const Color(0xFFE8F5E9), const Color(0xFF2E7D32)),
                        ],
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${widget.mother.stageLabel ?? (widget.mother.gestationalWeek != null ? "Tuần ${widget.mother.gestationalWeek}" : "Đang theo dõi")} · Hoạt động ${_timeAgo(widget.mother.lastActiveAt)}',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: Color(0xFF7A6F6C),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),

            // Sub-tab switcher row if either/both available
            if (hasBoth || health != null || checklist != null) ...[
              Row(
                children: [
                  if (health != null)
                    Expanded(
                      child: InkWell(
                        onTap: () => setState(() => _subTab = 'HEALTH'),
                        borderRadius: BorderRadius.circular(10),
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 10),
                          decoration: BoxDecoration(
                            color: _subTab == 'HEALTH' ? const Color(0xFFF2E6E2) : Colors.transparent,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(
                              color: _subTab == 'HEALTH' ? primary : const Color(0xFFECE4E1),
                            ),
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                Icons.monitor_heart_rounded,
                                size: 16,
                                color: _subTab == 'HEALTH' ? primary : const Color(0xFF7A6F6C),
                              ),
                              const SizedBox(width: 6),
                              Text(
                                'Chỉ số sức khỏe',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 11,
                                  fontWeight: _subTab == 'HEALTH' ? FontWeight.w700 : FontWeight.w500,
                                  color: _subTab == 'HEALTH' ? primary : const Color(0xFF7A6F6C),
                                ),
                              ),
                              const SizedBox(width: 4),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                                decoration: BoxDecoration(
                                  color: _subTab == 'HEALTH' ? primary : const Color(0xFFDDD2CD),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Text(
                                  '${health.metrics.length}',
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 9,
                                    fontWeight: FontWeight.bold,
                                    color: _subTab == 'HEALTH' ? Colors.white : const Color(0xFF554440),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  if (hasBoth) const SizedBox(width: 8),
                  if (checklist != null)
                    Expanded(
                      child: InkWell(
                        onTap: () => setState(() => _subTab = 'CHECKLIST'),
                        borderRadius: BorderRadius.circular(10),
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 10),
                          decoration: BoxDecoration(
                            color: _subTab == 'CHECKLIST' ? const Color(0xFFF2E6E2) : Colors.transparent,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(
                              color: _subTab == 'CHECKLIST' ? primary : const Color(0xFFECE4E1),
                            ),
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                Icons.assignment_turned_in_rounded,
                                size: 16,
                                color: _subTab == 'CHECKLIST' ? primary : const Color(0xFF7A6F6C),
                              ),
                              const SizedBox(width: 6),
                              Text(
                                'Việc cần làm',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 11,
                                  fontWeight: _subTab == 'CHECKLIST' ? FontWeight.w700 : FontWeight.w500,
                                  color: _subTab == 'CHECKLIST' ? primary : const Color(0xFF7A6F6C),
                                ),
                              ),
                              const SizedBox(width: 4),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                                decoration: BoxDecoration(
                                  color: _subTab == 'CHECKLIST' ? primary : const Color(0xFFDDD2CD),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Text(
                                  '${checklist.progressPercent}%',
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 9,
                                    fontWeight: FontWeight.bold,
                                    color: _subTab == 'CHECKLIST' ? Colors.white : const Color(0xFF554440),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 12),
            ],

            // Content based on _subTab
            if (_subTab == 'HEALTH' && health != null) ...[
              // Vital Metrics Grid / Cards
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: health.metrics.map((HealthMetricItemData m) {
                  final statusColor = m.status == 'CRITICAL'
                      ? Colors.red
                      : m.status == 'WARNING'
                          ? Colors.orange
                          : const Color(0xFF2E7D32);
                  final statusBg = m.status == 'CRITICAL'
                      ? const Color(0xFFFFEBEE)
                      : m.status == 'WARNING'
                          ? const Color(0xFFFFF8E1)
                          : const Color(0xFFE8F5E9);
                  final statusLabel = m.status == 'CRITICAL'
                      ? 'Nguy hiểm'
                      : m.status == 'WARNING'
                          ? 'Cần lưu ý'
                          : 'Bình thường';

                  return Container(
                    width: (MediaQuery.of(context).size.width - 70) / 2,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFDFBF9),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: const Color(0xFFECE4E1)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(_getMetricIcon(m.code), size: 16, color: primary),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                m.name,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 11,
                                  fontWeight: FontWeight.w600,
                                  color: Color(0xFF7A6F6C),
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.baseline,
                          textBaseline: TextBaseline.alphabetic,
                          children: [
                            Text(
                              m.value,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 16,
                                fontWeight: FontWeight.w800,
                                color: onSurface,
                              ),
                            ),
                            const SizedBox(width: 4),
                            Text(
                              m.unit,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 11,
                                color: Color(0xFF9E8E8A),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                m.measuredTime ??
                                    (m.history.isNotEmpty ? m.history.first.measuredAt : ''),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 9.5,
                                  color: Color(0xFF9E8E8A),
                                ),
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                              decoration: BoxDecoration(
                                color: statusBg,
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Text(
                                statusLabel,
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 8.5,
                                  fontWeight: FontWeight.w700,
                                  color: statusColor,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  );
                }).toList(),
              ),

              if (health.note != null && health.note!.isNotEmpty) ...[
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF8EEE9),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: outlineVariant),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.speaker_notes_outlined, size: 14, color: primary),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          health.note!,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            fontStyle: FontStyle.italic,
                            color: Color(0xFF554440),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: widget.onOpenHealth,
                      icon: const Icon(Icons.history_rounded, size: 16),
                      label: const Text('Lịch sử đo chi tiết'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: primary,
                        side: const BorderSide(color: primary),
                        visualDensity: VisualDensity.compact,
                        textStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w600),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton.icon(
                    onPressed: () {
                      Navigator.of(context).pushNamed('/direct-chats/${widget.mother.conversationId}');
                    },
                    icon: const Icon(Icons.chat_bubble_outline_rounded, size: 15),
                    label: const Text('Tư vấn'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: primary,
                      foregroundColor: Colors.white,
                      visualDensity: VisualDensity.compact,
                      elevation: 0,
                      textStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w600),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                  ),
                ],
              ),
            ] else if (_subTab == 'CHECKLIST' && checklist != null) ...[
              // Checklist Summary Block
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFFDFBF9),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFECE4E1)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.assignment_turned_in_rounded, size: 16, color: primary),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            checklist.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 12,
                              fontWeight: FontWeight.w700,
                              color: onSurface,
                            ),
                          ),
                        ),
                        Text(
                          '${checklist.completedCount}/${checklist.totalCount} (${checklist.progressPercent}%)',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF2E7D32),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: LinearProgressIndicator(
                        value: checklist.totalCount > 0
                            ? checklist.completedCount / checklist.totalCount
                            : 0,
                        backgroundColor: const Color(0xFFECE4E1),
                        valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF2E7D32)),
                        minHeight: 5,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        OutlinedButton.icon(
                          onPressed: widget.onOpenChecklist,
                          icon: const Icon(Icons.edit_note_rounded, size: 16),
                          label: const Text('Xem & Quản lý Checklist'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: primary,
                            side: const BorderSide(color: primary),
                            visualDensity: VisualDensity.compact,
                            textStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w600),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                          ),
                        ),
                        const SizedBox(width: 8),
                        ElevatedButton.icon(
                          onPressed: () {
                            Navigator.of(context).pushNamed('/direct-chats/${widget.mother.conversationId}');
                          },
                          icon: const Icon(Icons.chat_bubble_outline_rounded, size: 15),
                          label: const Text('Tư vấn'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: primary,
                            foregroundColor: Colors.white,
                            visualDensity: VisualDensity.compact,
                            elevation: 0,
                            textStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w600),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                          ),
                        ),
                      ],
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

  Widget _buildAlertBadge(String label, Color bg, Color textCol) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(8)),
      child: Text(
        label,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 10,
          fontWeight: FontWeight.w700,
          color: textCol,
        ),
      ),
    );
  }

  String _timeAgo(DateTime dt) {
    final diff = DateTime.now().difference(dt);
    if (diff.inMinutes < 60) return '${diff.inMinutes}m trước';
    if (diff.inHours < 24) return '${diff.inHours}h trước';
    return '${diff.inDays}d trước';
  }
}

class _ChecklistInspectionSheet extends StatefulWidget {
  final MotherSharedCardData mother;
  final SharedRecordEntry record;
  final VoidCallback onUpdate;

  const _ChecklistInspectionSheet({
    required this.mother,
    required this.record,
    required this.onUpdate,
  });

  @override
  State<_ChecklistInspectionSheet> createState() => _ChecklistInspectionSheetState();
}

class _ChecklistInspectionSheetState extends State<_ChecklistInspectionSheet> {
  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF2A211D);
  static const _outlineVariant = Color(0xFFE5D3CA);

  late ChecklistShareData _data;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _data = widget.record.checklistData!;
  }

  void _openAddModal(String targetGroup) {
    ExpertChecklistFormDialog.show(
      context,
      mode: ExpertChecklistFormMode.add,
      initialTargetGroup: targetGroup,
      onSave: ({
        required text,
        required targetGroup,
        required category,
        required timeLabel,
        required doctorNote,
        required supportFunction,
        required completed,
        required sourceUrl,
      }) async {
        final newItem = ChecklistItemShareData(
          text: text,
          completed: completed,
          category: category,
          timeLabel: timeLabel,
          origin: 'EXPERT',
          createdBy: 'EXPERT',
          isExpertCustom: true,
          doctorNote: doctorNote,
          supportFunction: supportFunction,
          sourceUrl: sourceUrl,
        );

        setState(() => _saving = true);
        try {
          final updated = await ExpertSharedRecordsService.instance
              .addChecklistItemToSharedRecord(
                widget.mother.conversationId,
                _data,
                newItem,
                targetGroup,
                doctorNote: doctorNote,
              );
          if (mounted) {
            setState(() {
              _data = updated;
              _saving = false;
            });
            widget.onUpdate();
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Đã thêm việc chỉ định vào checklist của mẹ')),
            );
          }
        } catch (e) {
          if (mounted) {
            setState(() => _saving = false);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('Lỗi: $e')),
            );
          }
        }
      },
    );
  }

  void _openEditModal(ChecklistItemShareData item, String targetGroup, int index) {
    ExpertChecklistFormDialog.show(
      context,
      mode: ExpertChecklistFormMode.edit,
      initialItem: item,
      initialTargetGroup: targetGroup,
      onSave: ({
        required text,
        required targetGroup,
        required category,
        required timeLabel,
        required doctorNote,
        required supportFunction,
        required completed,
        required sourceUrl,
      }) async {
        final updatedItem = ChecklistItemShareData(
          text: text,
          completed: completed,
          category: category,
          timeLabel: timeLabel,
          origin: 'EXPERT',
          createdBy: 'EXPERT',
          isExpertCustom: true,
          doctorNote: doctorNote,
          supportFunction: supportFunction,
          sourceUrl: sourceUrl,
        );

        setState(() => _saving = true);
        try {
          final updated = await ExpertSharedRecordsService.instance
              .editChecklistItemInSharedRecord(
                widget.mother.conversationId,
                _data,
                targetGroup,
                index,
                updatedItem,
                doctorNote: doctorNote,
                originalItemText: item.text,
              );
          if (mounted) {
            setState(() {
              _data = updated;
              _saving = false;
            });
            widget.onUpdate();
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Đã cập nhật việc cần làm thành công')),
            );
          }
        } catch (e) {
          if (mounted) {
            setState(() => _saving = false);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('Lỗi: $e')),
            );
          }
        }
      },
    );
  }

  void _confirmDelete(ChecklistItemShareData item, String targetGroup, int index) {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xác nhận xóa việc này?', style: TextStyle(fontFamily: 'Lexend', fontSize: 16)),
        content: Text(
          'Bạn có chắc chắn muốn xóa "${item.text}" khỏi lộ trình của mẹ bầu?',
          style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
            onPressed: () async {
              Navigator.of(ctx).pop();
              setState(() => _saving = true);
              try {
                final updated = await ExpertSharedRecordsService.instance
                    .deleteChecklistItemFromSharedRecord(
                      widget.mother.conversationId,
                      _data,
                      targetGroup,
                      index,
                      itemText: item.text,
                    );
                if (mounted) {
                  setState(() {
                    _data = updated;
                    _saving = false;
                  });
                  widget.onUpdate();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Đã xóa việc khỏi checklist của mẹ')),
                  );
                }
              } catch (e) {
                if (mounted) {
                  setState(() => _saving = false);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('Lỗi: $e')),
                  );
                }
              }
            },
            child: const Text('Xóa', style: TextStyle(fontFamily: 'Lexend')),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final historyList = _data.historyItems;
    final currentList = _data.currentItems;
    final futureList = _data.futureItems;

    return DefaultTabController(
      length: 3,
      initialIndex: 1, // Default to Current
      child: Container(
        height: MediaQuery.of(context).size.height * 0.85,
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey.shade300,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                const Icon(Icons.checklist_rtl_rounded, color: _primary, size: 24),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _data.title,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontWeight: FontWeight.w800,
                          fontSize: 16,
                          color: _onSurface,
                        ),
                      ),
                      Text(
                        'Mẹ: ${widget.mother.motherName} · ${_data.stageLabel ?? "Lộ trình"}',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          color: Color(0xFF7A6F6C),
                        ),
                      ),
                    ],
                  ),
                ),
                if (_saving)
                  const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: _primary),
                  ),
              ],
            ),
            const SizedBox(height: 14),

            // Tab bar
            TabBar(
              labelColor: _primary,
              unselectedLabelColor: const Color(0xFF7A6F6C),
              indicatorColor: _primary,
              labelStyle: const TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.bold, fontSize: 12),
              unselectedLabelStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 12),
              tabs: [
                Tab(text: 'Đã làm (${historyList.length})'),
                Tab(text: 'Hiện tại (${currentList.length})'),
                Tab(text: 'Tương lai (${futureList.length})'),
              ],
            ),
            const SizedBox(height: 10),

            // Tab views
            Expanded(
              child: TabBarView(
                children: [
                  _buildInspectionList(historyList, 'HISTORY'),
                  _buildInspectionList(currentList, 'CURRENT'),
                  _buildInspectionList(futureList, 'FUTURE'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInspectionList(List<ChecklistItemShareData> items, String targetGroup) {
    return Column(
      children: [
        // Add action button for Expert
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Row(
            children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: _saving ? null : () => _openAddModal(targetGroup),
                  icon: const Icon(Icons.add_rounded, size: 18),
                  label: const Text('Thêm việc bác sĩ chỉ định'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFE0F2F1),
                    foregroundColor: const Color(0xFF00695C),
                    elevation: 0,
                    textStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.bold),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                      side: const BorderSide(color: Color(0xFF80CBC4)),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),

        // List
        Expanded(
          child: items.isEmpty
              ? Center(
                  child: Text(
                    'Chưa có việc nào trong mục này.',
                    style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Color(0xFF9E8E8A)),
                  ),
                )
              : ListView.separated(
                  itemCount: items.length,
                  separatorBuilder: (_, _) => const Divider(height: 1, color: _outlineVariant),
                  itemBuilder: (ctx, idx) {
                    final item = items[idx];
                    final isExp =
                        item.isExpertCustom || item.origin == 'EXPERT' || item.createdBy == 'EXPERT';

                    return Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 2),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Icon(
                            item.completed
                                ? Icons.check_circle_rounded
                                : Icons.radio_button_unchecked_rounded,
                            size: 18,
                            color: item.completed ? const Color(0xFF2E7D32) : const Color(0xFFC98C7B),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Expanded(
                                      child: Text(
                                        item.text,
                                        style: TextStyle(
                                          fontFamily: 'Lexend',
                                          fontSize: 13,
                                          fontWeight: FontWeight.w600,
                                          color: item.completed ? const Color(0xFF7A6F6C) : _onSurface,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 3),
                                Wrap(
                                  spacing: 4,
                                  runSpacing: 2,
                                  children: [
                                    if (isExp)
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFFE0F2F1),
                                          borderRadius: BorderRadius.circular(4),
                                          border: Border.all(color: const Color(0xFF80CBC4)),
                                        ),
                                        child: const Text(
                                          '🩺 Bác sĩ chỉ định',
                                          style: TextStyle(
                                            fontFamily: 'Lexend',
                                            fontSize: 9,
                                            fontWeight: FontWeight.bold,
                                            color: Color(0xFF00695C),
                                          ),
                                        ),
                                      )
                                    else
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFFE0F2FE),
                                          borderRadius: BorderRadius.circular(4),
                                          border: Border.all(color: const Color(0xFFBAE6FD)),
                                        ),
                                        child: const Text(
                                          'Gợi ý CareBridge',
                                          style: TextStyle(
                                            fontFamily: 'Lexend',
                                            fontSize: 9,
                                            fontWeight: FontWeight.bold,
                                            color: Color(0xFF0369A1),
                                          ),
                                        ),
                                      ),
                                    if (item.timeLabel != null)
                                      Text(
                                        item.timeLabel!,
                                        style: const TextStyle(
                                          fontFamily: 'Lexend',
                                          fontSize: 10,
                                          color: Color(0xFF9E8E8A),
                                        ),
                                      ),
                                  ],
                                ),
                                if (item.doctorNote != null && item.doctorNote!.trim().isNotEmpty)
                                  Padding(
                                    padding: const EdgeInsets.only(top: 3),
                                    child: Text(
                                      '💬 Lời dặn: ${item.doctorNote}',
                                      style: const TextStyle(
                                        fontFamily: 'Lexend',
                                        fontSize: 10,
                                        fontStyle: FontStyle.italic,
                                        color: Color(0xFF00695C),
                                      ),
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          // Edit & Delete actions
                          IconButton(
                            icon: const Icon(Icons.edit_outlined, size: 18, color: Color(0xFF7A6F6C)),
                            onPressed: _saving ? null : () => _openEditModal(item, targetGroup, idx),
                            constraints: const BoxConstraints(),
                            padding: const EdgeInsets.all(6),
                          ),
                          IconButton(
                            icon: const Icon(Icons.delete_outline_rounded, size: 18, color: Colors.redAccent),
                            onPressed: _saving ? null : () => _confirmDelete(item, targetGroup, idx),
                            constraints: const BoxConstraints(),
                            padding: const EdgeInsets.all(6),
                          ),
                        ],
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }
}

IconData _getMetricIcon(String code) {
  switch (code.toUpperCase()) {
    case 'BLOOD_PRESSURE':
      return Icons.speed_rounded;
    case 'HEART_RATE':
      return Icons.favorite_rounded;
    case 'BLOOD_GLUCOSE':
      return Icons.bloodtype_rounded;
    case 'WEIGHT':
      return Icons.monitor_weight_outlined;
    case 'TEMPERATURE':
      return Icons.thermostat_rounded;
    case 'FETAL_MOVEMENT_SESSION':
      return Icons.child_care_rounded;
    case 'SPO2':
      return Icons.air_rounded;
    default:
      return Icons.monitor_heart_rounded;
  }
}

class _HealthInspectionSheet extends StatelessWidget {
  final MotherSharedCardData mother;
  final SharedRecordEntry record;

  const _HealthInspectionSheet({
    required this.mother,
    required this.record,
  });

  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF2A211D);
  static const _outlineVariant = Color(0xFFE5D3CA);

  @override
  Widget build(BuildContext context) {
    final healthData = record.healthData;
    if (healthData == null) {
      return const SizedBox(height: 200, child: Center(child: Text('Không có dữ liệu')));
    }

    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.85,
      ),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Drag Handle
          const SizedBox(height: 12),
          Center(
            child: Container(
              width: 38,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),

          // Header
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 14, 16, 12),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 20,
                  backgroundColor: const Color(0xFFF8EEE9),
                  backgroundImage: mother.motherAvatar != null
                      ? NetworkImage(mother.motherAvatar!)
                      : null,
                  child: mother.motherAvatar == null
                      ? const Icon(Icons.person, color: _primary, size: 20)
                      : null,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              'Chỉ số sức khỏe: ${mother.motherName}',
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 16,
                                fontWeight: FontWeight.w700,
                                color: _onSurface,
                              ),
                            ),
                          ),
                          if (mother.overallAlertLevel == SharedRecordAlertLevel.critical)
                            _buildBadge('Nguy cơ', const Color(0xFFFFEBEE), Colors.red)
                          else if (mother.overallAlertLevel == SharedRecordAlertLevel.warning)
                            _buildBadge('Cảnh báo', const Color(0xFFFFF8E1), Colors.orange)
                          else
                            _buildBadge('Bình thường', const Color(0xFFE8F5E9), const Color(0xFF2E7D32)),
                        ],
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${healthData.gestationalWeek != null ? "Tuần thai ${healthData.gestationalWeek} · " : ""}${healthData.timeRangeLabel ?? "Toàn bộ khoảng thời gian"}',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: Color(0xFF7A6F6C),
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close_rounded, color: Colors.black45),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: _outlineVariant),

          // Scrollable List of Metrics
          Expanded(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 20),
              children: [
                // Metrics Cards with History Timeline
                ...healthData.metrics.map((metric) {
                  final statusColor = metric.status == 'CRITICAL'
                      ? Colors.red
                      : metric.status == 'WARNING'
                          ? Colors.orange
                          : const Color(0xFF2E7D32);
                  final statusBg = metric.status == 'CRITICAL'
                      ? const Color(0xFFFFEBEE)
                      : metric.status == 'WARNING'
                          ? const Color(0xFFFFF8E1)
                          : const Color(0xFFE8F5E9);
                  final statusLabel = metric.status == 'CRITICAL'
                      ? 'Nguy hiểm'
                      : metric.status == 'WARNING'
                          ? 'Cần lưu ý'
                          : 'Bình thường';

                  return Container(
                    margin: const EdgeInsets.only(bottom: 14),
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFDFBF9),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: const Color(0xFFECE4E1)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Metric Header Row
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                color: _primary.withValues(alpha: 0.1),
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Icon(
                                _getMetricIcon(metric.code),
                                size: 20,
                                color: _primary,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    metric.name,
                                    style: const TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 14,
                                      fontWeight: FontWeight.w700,
                                      color: _onSurface,
                                    ),
                                  ),
                                  if (metric.measuredTime != null)
                                    Text(
                                      'Mới nhất: ${metric.measuredTime}',
                                      style: const TextStyle(
                                        fontFamily: 'Lexend',
                                        fontSize: 10,
                                        color: Colors.black45,
                                      ),
                                    ),
                                ],
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Row(
                                  children: [
                                    Text(
                                      metric.value,
                                      style: const TextStyle(
                                        fontFamily: 'Lexend',
                                        fontSize: 17,
                                        fontWeight: FontWeight.w800,
                                        color: _onSurface,
                                      ),
                                    ),
                                    const SizedBox(width: 3),
                                    Text(
                                      metric.unit,
                                      style: const TextStyle(
                                        fontFamily: 'Lexend',
                                        fontSize: 11,
                                        color: Color(0xFF7A6F6C),
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 2),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: statusBg,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    statusLabel,
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 9,
                                      fontWeight: FontWeight.w700,
                                      color: statusColor,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),

                        // History Records
                        if (metric.history.isNotEmpty) ...[
                          const SizedBox(height: 12),
                          const Divider(height: 1, color: Color(0xFFECE4E1)),
                          const SizedBox(height: 8),
                          Text(
                            'Lịch sử các lần đo gần đây (${metric.history.length} lần):',
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFF7A6F6C),
                            ),
                          ),
                          const SizedBox(height: 6),
                          ...metric.history.map((h) {
                            final hStatusColor = h.status == 'CRITICAL'
                                ? Colors.red
                                : h.status == 'WARNING'
                                    ? Colors.orange
                                    : const Color(0xFF2E7D32);
                            final hStatusBg = h.status == 'CRITICAL'
                                ? const Color(0xFFFFEBEE)
                                : h.status == 'WARNING'
                                    ? const Color(0xFFFFF8E1)
                                    : const Color(0xFFE8F5E9);
                            final hStatusLabel = h.status == 'CRITICAL'
                                ? 'Nguy hiểm'
                                : h.status == 'WARNING'
                                    ? 'Cần lưu ý'
                                    : 'Bình thường';

                            return Container(
                              margin: const EdgeInsets.only(bottom: 5),
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(color: const Color(0xFFF0E8E4)),
                              ),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          h.measuredAt,
                                          style: const TextStyle(
                                            fontFamily: 'Lexend',
                                            fontSize: 11,
                                            fontWeight: FontWeight.w500,
                                            color: _onSurface,
                                          ),
                                        ),
                                        if (h.note != null && h.note!.isNotEmpty)
                                          Text(
                                            h.note!,
                                            style: const TextStyle(
                                              fontFamily: 'Lexend',
                                              fontSize: 10,
                                              fontStyle: FontStyle.italic,
                                              color: Colors.black45,
                                            ),
                                          ),
                                      ],
                                    ),
                                  ),
                                  Text(
                                    '${h.value} ${h.unit}',
                                    style: const TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 12,
                                      fontWeight: FontWeight.w700,
                                      color: _onSurface,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1.5),
                                    decoration: BoxDecoration(
                                      color: hStatusBg,
                                      borderRadius: BorderRadius.circular(4),
                                    ),
                                    child: Text(
                                      hStatusLabel,
                                      style: TextStyle(
                                        fontFamily: 'Lexend',
                                        fontSize: 8.5,
                                        fontWeight: FontWeight.w700,
                                        color: hStatusColor,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            );
                          }),
                        ],
                      ],
                    ),
                  );
                }),

                // Mother's Note
                if (healthData.note != null && healthData.note!.isNotEmpty) ...[
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF8EEE9),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: _outlineVariant),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(Icons.speaker_notes_outlined, size: 16, color: _primary),
                            SizedBox(width: 6),
                            Text(
                              'Ghi chú từ mẹ bầu:',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 12,
                                fontWeight: FontWeight.w700,
                                color: _onSurface,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(
                          healthData.note!,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            fontStyle: FontStyle.italic,
                            color: Color(0xFF554440),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),
                ],
              ],
            ),
          ),

          // Footer
          Container(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
            decoration: const BoxDecoration(
              color: Colors.white,
              border: Border(top: BorderSide(color: _outlineVariant)),
            ),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: _onSurface,
                      side: const BorderSide(color: _outlineVariant),
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: const Text('Đóng', style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600)),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  flex: 2,
                  child: ElevatedButton.icon(
                    onPressed: () {
                      Navigator.of(context).pop();
                      Navigator.of(context).pushNamed('/direct-chats/${mother.conversationId}');
                    },
                    icon: const Icon(Icons.chat_bubble_outline_rounded, size: 18),
                    label: const Text('Nhắn tin tư vấn cho mẹ'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _primary,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBadge(String label, Color bg, Color textCol) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(8)),
      child: Text(
        label,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 10,
          fontWeight: FontWeight.w700,
          color: textCol,
        ),
      ),
    );
  }
}
