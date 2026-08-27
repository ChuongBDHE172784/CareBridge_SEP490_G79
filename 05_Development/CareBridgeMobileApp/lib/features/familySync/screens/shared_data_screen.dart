import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../healthRecords/models/health_record_model.dart';
import '../../healthRecords/screens/health_record_attachment_detail_screen.dart';
import '../../healthRecords/services/health_record_service.dart';

class SharedDataScreen extends StatefulWidget {
  final String groupId;

  const SharedDataScreen({super.key, required this.groupId});

  @override
  State<SharedDataScreen> createState() => _SharedDataScreenState();
}

class _SharedDataScreenState extends State<SharedDataScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceVariant = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = HealthRecordService();
  final _searchCtrl = TextEditingController();

  List<HealthRecord> _allRecords = [];
  List<HealthRecord> _filteredRecords = [];

  bool _isLoading = true;
  String? _errorMessage;

  DateTime? _fromDate;
  DateTime? _toDate;
  bool _filterApplied = false;

  @override
  void initState() {
    super.initState();
    _loadRecords();
    _searchCtrl.addListener(_onSearchChanged);
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadRecords() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final records = await _service.listHealthRecords(
        size: 100,
        careGroupId: widget.groupId,
      );
      setState(() {
        _allRecords = records;
        _isLoading = false;
      });
      _applyFilters();
    } catch (e) {
      setState(() {
        _isLoading = false;
        _errorMessage = 'Không thể tải danh sách hồ sơ sức khỏe: $e';
      });
    }
  }

  void _onSearchChanged() {
    _applyFilters();
  }

  void _applyFilters() {
    final query = _searchCtrl.text.trim().toLowerCase();
    setState(() {
      _filteredRecords = _allRecords.where((record) {
        // Search text matching
        if (query.isNotEmpty && !record.title.toLowerCase().contains(query)) {
          return false;
        }

        // Date range filter matching
        if (_filterApplied && _fromDate != null && _toDate != null) {
          final start = DateTime(
            _fromDate!.year,
            _fromDate!.month,
            _fromDate!.day,
            0,
            0,
            0,
          );
          final end = DateTime(
            _toDate!.year,
            _toDate!.month,
            _toDate!.day,
            23,
            59,
            59,
          );
          final recordDt = record.recordDate;
          if (recordDt.isBefore(start) || recordDt.isAfter(end)) {
            return false;
          }
        }
        return true;
      }).toList();
    });
  }

  void _onFilterClick() {
    if (_fromDate == null || _toDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng chọn cả "Từ ngày" và "Đến ngày" để lọc.'),
          backgroundColor: _primary,
        ),
      );
      return;
    }
    setState(() {
      _filterApplied = true;
    });
    _applyFilters();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Đã lọc hồ sơ từ ${DateFormat('dd/MM/yyyy').format(_fromDate!)} đến ${DateFormat('dd/MM/yyyy').format(_toDate!)}',
        ),
        backgroundColor: _primary,
      ),
    );
  }

  void _resetFilter() {
    setState(() {
      _fromDate = null;
      _toDate = null;
      _filterApplied = false;
      _searchCtrl.clear();
    });
    _applyFilters();
  }

  Future<void> _selectFromDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _fromDate ?? now,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: _primary,
              onPrimary: Colors.white,
              surface: Colors.white,
              onSurface: _onSurface,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      setState(() {
        _fromDate = picked;
        // Constraint check: Max 1 week (7 days) gap between fromDate and toDate
        if (_toDate != null) {
          final diff = _toDate!.difference(_fromDate!).inDays.abs();
          if (diff > 6 || _fromDate!.isAfter(_toDate!)) {
            _toDate = _fromDate!.add(const Duration(days: 6));
            _showWeekConstraintWarning();
          }
        } else {
          _toDate = _fromDate!.add(const Duration(days: 6));
        }
      });
    }
  }

  Future<void> _selectToDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _toDate ?? _fromDate ?? now,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: _primary,
              onPrimary: Colors.white,
              surface: Colors.white,
              onSurface: _onSurface,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      setState(() {
        if (_fromDate != null) {
          if (picked.isBefore(_fromDate!)) {
            _fromDate = picked;
            _toDate = picked.add(const Duration(days: 6));
          } else if (picked.difference(_fromDate!).inDays.abs() > 6) {
            _toDate = picked;
            _fromDate = picked.subtract(const Duration(days: 6));
            _showWeekConstraintWarning();
          } else {
            _toDate = picked;
          }
        } else {
          _toDate = picked;
          _fromDate = picked.subtract(const Duration(days: 6));
        }
      });
    }
  }

  void _showWeekConstraintWarning() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Khoảng thời gian chọn chỉ được tối đa 1 tuần (7 ngày)'),
        backgroundColor: _primary,
        duration: Duration(seconds: 3),
      ),
    );
  }

  Future<void> _openRecordDetail(HealthRecord record) async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) =>
          const Center(child: CircularProgressIndicator(color: _primary)),
    );

    try {
      final detail = await _service.getHealthRecord(record.id);
      if (!mounted) return;
      Navigator.of(context).pop(); // Close loading dialog

      await Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => HealthRecordAttachmentDetailScreen(
            record: detail,
            readOnly: true, // READ-ONLY mode enforced
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      Navigator.of(context).pop(); // Close loading dialog
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Không thể tải chi tiết hồ sơ: $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded, color: _primary),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Hồ sơ sức khỏe chia sẻ',
          style: TextStyle(
            color: _onSurface,
            fontSize: 18,
            fontWeight: FontWeight.bold,
            fontFamily: 'Lexend',
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: _primary),
            onPressed: _loadRecords,
            tooltip: 'Làm mới',
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadRecords,
        color: _primary,
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 12.0),
          children: [
            // Info Header Banner
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _surfaceVariant,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: const Color(0xFFE8DAD6)),
              ),
              child: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: Color(0xFFFFE2D9),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(Icons.folder_shared_rounded, color: _primary),
                  ),
                  SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Dữ liệu hồ sơ sức khỏe',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: _onSurface,
                            fontFamily: 'Lexend',
                          ),
                        ),
                        SizedBox(height: 2),
                        Text(
                          'Các hồ sơ sức khỏe trong nhóm gia đình (chế độ chỉ xem).',
                          style: TextStyle(
                            fontSize: 12,
                            color: _onSurfaceVariant,
                            fontFamily: 'Lexend',
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // Search Bar
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: const Color(0xFFE8DAD6)),
              ),
              child: TextField(
                controller: _searchCtrl,
                decoration: InputDecoration(
                  hintText: 'Tìm theo tên hồ sơ...',
                  hintStyle: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                  ),
                  prefixIcon: const Icon(
                    Icons.search_rounded,
                    color: _primaryContainer,
                  ),
                  suffixIcon: _searchCtrl.text.isNotEmpty
                      ? IconButton(
                          icon: const Icon(
                            Icons.clear_rounded,
                            color: _onSurfaceVariant,
                          ),
                          onPressed: () {
                            _searchCtrl.clear();
                          },
                        )
                      : null,
                  border: InputBorder.none,
                  contentPadding: const EdgeInsets.symmetric(
                    vertical: 14,
                    horizontal: 16,
                  ),
                ),
              ),
            ),

            const SizedBox(height: 14),

            // Date Range Filter Card
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: const Color(0xFFE8DAD6)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.filter_alt_rounded, size: 18, color: _primary),
                      SizedBox(width: 6),
                      Text(
                        'Lọc theo ngày đăng (tối đa 1 tuần)',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      // From Date
                      Expanded(
                        child: InkWell(
                          onTap: _selectFromDate,
                          borderRadius: BorderRadius.circular(12),
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 10,
                            ),
                            decoration: BoxDecoration(
                              color: _surfaceVariant,
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: const Color(0xFFE5D5D0),
                              ),
                            ),
                            child: Row(
                              children: [
                                const Icon(
                                  Icons.calendar_today_rounded,
                                  size: 14,
                                  color: _primary,
                                ),
                                const SizedBox(width: 6),
                                Expanded(
                                  child: Text(
                                    _fromDate != null
                                        ? DateFormat(
                                            'dd/MM/yyyy',
                                          ).format(_fromDate!)
                                        : 'Từ ngày',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 12,
                                      fontWeight: _fromDate != null
                                          ? FontWeight.w600
                                          : FontWeight.normal,
                                      color: _fromDate != null
                                          ? _onSurface
                                          : _onSurfaceVariant,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                      const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 6.0),
                        child: Icon(
                          Icons.arrow_forward_rounded,
                          size: 14,
                          color: _onSurfaceVariant,
                        ),
                      ),
                      // To Date
                      Expanded(
                        child: InkWell(
                          onTap: _selectToDate,
                          borderRadius: BorderRadius.circular(12),
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 10,
                            ),
                            decoration: BoxDecoration(
                              color: _surfaceVariant,
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: const Color(0xFFE5D5D0),
                              ),
                            ),
                            child: Row(
                              children: [
                                const Icon(
                                  Icons.calendar_today_rounded,
                                  size: 14,
                                  color: _primary,
                                ),
                                const SizedBox(width: 6),
                                Expanded(
                                  child: Text(
                                    _toDate != null
                                        ? DateFormat(
                                            'dd/MM/yyyy',
                                          ).format(_toDate!)
                                        : 'Đến ngày',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 12,
                                      fontWeight: _toDate != null
                                          ? FontWeight.w600
                                          : FontWeight.normal,
                                      color: _toDate != null
                                          ? _onSurface
                                          : _onSurfaceVariant,
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
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: _onFilterClick,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: _primary,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 10),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                            elevation: 0,
                          ),
                          icon: const Icon(Icons.tune_rounded, size: 16),
                          label: const Text(
                            'Lọc theo ngày đăng',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ),
                      if (_fromDate != null ||
                          _toDate != null ||
                          _filterApplied) ...[
                        const SizedBox(width: 8),
                        IconButton(
                          onPressed: _resetFilter,
                          icon: const Icon(
                            Icons.restart_alt_rounded,
                            color: _primary,
                          ),
                          tooltip: 'Xóa bộ lọc',
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // Record Count Label
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Danh sách hồ sơ (${_filteredRecords.length})',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                if (_filterApplied)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 3,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFE2D9),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Text(
                      'Đã lọc',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: _primary,
                      ),
                    ),
                  ),
              ],
            ),

            const SizedBox(height: 10),

            // Main List Content
            if (_isLoading)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 40.0),
                child: Center(
                  child: CircularProgressIndicator(color: _primary),
                ),
              )
            else if (_errorMessage != null)
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  children: [
                    const Icon(
                      Icons.error_outline_rounded,
                      size: 40,
                      color: Colors.red,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _errorMessage!,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        color: Colors.red,
                      ),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton(
                      onPressed: _loadRecords,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _primary,
                      ),
                      child: const Text(
                        'Thử lại',
                        style: TextStyle(color: Colors.white),
                      ),
                    ),
                  ],
                ),
              )
            else if (_filteredRecords.isEmpty)
              Container(
                padding: const EdgeInsets.symmetric(
                  vertical: 40,
                  horizontal: 20,
                ),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: const Color(0xFFE8DAD6)),
                ),
                child: Column(
                  children: [
                    Icon(
                      Icons.folder_open_rounded,
                      size: 56,
                      color: _primaryContainer.withAlpha(153),
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Không tìm thấy hồ sơ sức khỏe nào',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 15,
                        fontWeight: FontWeight.bold,
                        color: _onSurface,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      'Thử thay đổi từ khóa tìm kiếm hoặc điều chỉnh ngày lọc.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              )
            else
              ..._filteredRecords.map(
                (record) => _buildHealthRecordTile(record),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildHealthRecordTile(HealthRecord record) {
    IconData iconData = Icons.medical_services_rounded;

    switch (record.recordType) {
      case RecordType.ultrasound:
        iconData = Icons.monitor_heart_rounded;
        break;
      case RecordType.labResult:
        iconData = Icons.science_rounded;
        break;
      case RecordType.prescription:
        iconData = Icons.medication_rounded;
        break;
      case RecordType.vaccination:
        iconData = Icons.vaccines_rounded;
        break;
      case RecordType.examinationResult:
        iconData = Icons.assignment_turned_in_rounded;
        break;
      case RecordType.note:
        iconData = Icons.event_note_rounded;
        break;
    }

    final formattedDate = DateFormat('dd/MM/yyyy').format(record.recordDate);

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFFE8DAD6)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x085A463F),
            blurRadius: 10,
            offset: Offset(0, 2),
          ),
        ],
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        onTap: () => _openRecordDetail(record),
        leading: Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            color: const Color(0xFFFFE2D9),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Icon(iconData, color: _primary, size: 22),
        ),
        title: Text(
          record.title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 15,
            fontWeight: FontWeight.bold,
            color: _onSurface,
          ),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 6,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: _surfaceVariant,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      record.recordType.displayLabel,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: _primary,
                      ),
                    ),
                  ),
                  if (record.facilityName != null &&
                      record.facilityName!.isNotEmpty) ...[
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        record.facilityName!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 4),
              Text(
                'Ngày đăng: $formattedDate',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        trailing: const Icon(
          Icons.chevron_right_rounded,
          color: _onSurfaceVariant,
        ),
      ),
    );
  }
}
