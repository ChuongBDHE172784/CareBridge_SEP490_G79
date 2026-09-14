import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/vaccination_model.dart';
import '../services/vaccination_service.dart';
import '../../../core/network/api_client.dart';

/// CB-173 — Vaccination Detail (UC-228, UC-230, UC-231, UC-232, UC-233)
/// Shows full vaccination record: header with vaccine name + status badge,
/// 4 info cards (schedule, facility, child profile, notes), action buttons.
/// Calls GET /api/v1/vaccinations/{vaccinationId}.
class VaccinationDetailScreen extends StatefulWidget {
  final String babyId;
  final String vaccinationId;
  final VaccinationRecord? initialRecord;

  const VaccinationDetailScreen({
    super.key,
    required this.babyId,
    required this.vaccinationId,
    this.initialRecord,
  });

  @override
  State<VaccinationDetailScreen> createState() =>
      _VaccinationDetailScreenState();
}

class _VaccinationDetailScreenState extends State<VaccinationDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFEF8F4);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _textHeading = Color(0xFF2D2A28);
  static const _error = Color(0xFFBA1A1A);

  final _service = VaccinationService();
  VaccinationRecord? _record;
  bool _loading = true;
  String? _error2;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load({bool forceRefresh = false}) async {
    setState(() {
      _loading = true;
      _error2 = null;
    });
    try {
      final r = !forceRefresh && _record == null && widget.initialRecord != null
          ? widget.initialRecord!
          : await _service.getVaccination(widget.babyId, widget.vaccinationId);
      if (mounted) {
        setState(() {
          _record = r;
          _loading = false;
        });
      }
    } on ApiException catch (_) {
      if (mounted) {
        setState(() {
          _error2 = 'Không thể tải dữ liệu tiêm chủng.';
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error2 = 'Không thể tải dữ liệu.';
          _loading = false;
        });
      }
    }
  }

  Future<void> _edit() async {
    final changed = await context.push<bool>(
      '/babies/${widget.babyId}/vaccinations/${widget.vaccinationId}/edit',
      extra: _record,
    );
    if (changed == true && mounted) _load(forceRefresh: true);
  }

  Future<void> _createReminder() async {
    final record = _record;
    if (record == null ||
        record.status == VaccinationStatus.deleted ||
        record.status == VaccinationStatus.unknown) {
      return;
    }
    final suggestion = <String, dynamic>{
      'vaccinationRecordId': record.vaccinationId,
      'vaccineName': record.vaccineName,
      if (record.doseNumber != null) 'doseNumber': record.doseNumber,
      if (record.plannedDate != null)
        'scheduledDate': _dateOnlyString(record.plannedDate!),
    };
    final created = await context.push<bool>(
      '/reminders/vaccination/add?babyId=${Uri.encodeComponent(widget.babyId)}',
      extra: suggestion,
    );
    if (created == true && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã tạo nhắc tiêm riêng cho bé.')),
      );
    }
  }

  Future<void> _delete() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Xóa hồ sơ?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
        ),
        content: const Text(
          'Hành động này không thể khôi phục.',
          style: TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: _error),
            child: const Text('Xóa', style: TextStyle(fontFamily: 'Lexend')),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    try {
      await _service.deleteVaccination(widget.babyId, widget.vaccinationId);
      if (!mounted) return;
      Navigator.pop(context, true);
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể xóa. Vui lòng thử lại.')),
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
        scrolledUnderElevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _textHeading),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Chi tiết tiêm chủng',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: _textHeading,
          ),
        ),
      ),
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
            : _error2 != null
            ? _buildError()
            : _buildContent(_record!),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: _error),
          const SizedBox(height: 12),
          Text(
            _error2!,
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: _onSurfaceVariant,
            ),
          ),
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

  Widget _buildContent(VaccinationRecord r) {
    final note = r.note?.trim().isNotEmpty == true ? r.note : r.postponeReason;
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 36),
      child: Column(
        children: [
          _buildHeader(r),
          const SizedBox(height: 20),
          _buildScheduleCard(r),
          const SizedBox(height: 16),
          _buildFacilityCard(r),
          if (note?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 16),
            _buildNoteCard(note!),
          ],
          const SizedBox(height: 32),
          _buildActions(r),
        ],
      ),
    );
  }

  Widget _buildHeader(VaccinationRecord r) {
    final statusColor = switch (r.status) {
      VaccinationStatus.completed => const Color(0xFF2E7D32),
      VaccinationStatus.scheduled => const Color(0xFFE65100),
      VaccinationStatus.overdue => _error,
      _ => _primary,
    };
    final statusBg = switch (r.status) {
      VaccinationStatus.completed => const Color(0xFFE8F5E9),
      VaccinationStatus.scheduled => const Color(0xFFFFF3E0),
      VaccinationStatus.overdue => const Color(0xFFFFEBEE),
      _ => const Color(0xFFF2EAE4),
    };
    final statusIcon = switch (r.status) {
      VaccinationStatus.completed => Icons.check_circle_rounded,
      VaccinationStatus.scheduled => Icons.schedule_rounded,
      VaccinationStatus.overdue => Icons.error_outline_rounded,
      _ => Icons.info_outline_rounded,
    };

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFF0EAE6)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A000000),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            width: 72,
            height: 72,
            decoration: BoxDecoration(
              color: const Color(0xFFF7F1EE),
              shape: BoxShape.circle,
              border: Border.all(color: const Color(0xFFE8DDD6), width: 1.5),
            ),
            child: const Icon(
              Icons.vaccines_rounded,
              size: 36,
              color: _primary,
            ),
          ),
          const SizedBox(height: 14),
          Text(
            r.vaccineName,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: _textHeading,
            ),
          ),
          if (r.doseNumber != null) ...[
            const SizedBox(height: 4),
            Text(
              'Mũi tiêm số ${r.doseNumber}',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: _onSurfaceVariant,
              ),
            ),
          ],
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
            decoration: BoxDecoration(
              color: statusBg,
              borderRadius: BorderRadius.circular(99),
              border: Border.all(color: statusColor.withValues(alpha: 0.2)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(statusIcon, size: 16, color: statusColor),
                const SizedBox(width: 6),
                Text(
                  r.status.displayLabel,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: statusColor,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildScheduleCard(VaccinationRecord r) {
    return _InfoCard(
      icon: Icons.calendar_month_rounded,
      title: 'Lịch trình tiêm',
      child: Row(
        children: [
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFFAF7F5),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: const Color(0xFFE7E1DD)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'NGÀY DỰ KIẾN',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 0.5,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    r.plannedDateLabel,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: _textHeading,
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFFAF7F5),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: const Color(0xFFE7E1DD)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'NGÀY TIÊM THỰC TẾ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 0.5,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    r.actualDateLabel,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: _textHeading,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFacilityCard(VaccinationRecord r) {
    return _InfoCard(
      icon: Icons.local_hospital_rounded,
      title: 'Cơ sở tiêm chủng',
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFFAF7F5),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFFE7E1DD)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(
                  Icons.store_rounded,
                  size: 20,
                  color: _primary,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    r.facilityName ?? 'Chưa ghi nhận cơ sở',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: _textHeading,
                    ),
                  ),
                ),
              ],
            ),
            if (r.facilityAddress != null && r.facilityAddress!.trim().isNotEmpty) ...[
              const SizedBox(height: 10),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.location_on_outlined,
                    size: 18,
                    color: _onSurfaceVariant,
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      r.facilityAddress!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        color: _onSurfaceVariant,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildNoteCard(String note) {
    return _InfoCard(
      icon: Icons.edit_note_rounded,
      title: 'Ghi chú',
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFFAF7F5),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFFE7E1DD)),
        ),
        child: Text(
          note,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurfaceVariant,
            height: 1.5,
          ),
        ),
      ),
    );
  }

  Widget _buildActions(VaccinationRecord r) {
    final canEdit =
        r.status != VaccinationStatus.deleted &&
        r.status != VaccinationStatus.unknown;
    final canDelete = canEdit;
    return Column(
      children: [
        // Primary: Cập nhật thông tin
        ElevatedButton.icon(
          onPressed: canEdit ? _edit : null,
          icon: const Icon(Icons.edit_outlined, size: 20),
          label: const Text(
            'Cập nhật thông tin',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              fontWeight: FontWeight.bold,
            ),
          ),
          style: ElevatedButton.styleFrom(
            backgroundColor: _primary,
            foregroundColor: Colors.white,
            disabledBackgroundColor: _primary.withValues(alpha: 0.6),
            minimumSize: const Size.fromHeight(52),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
            elevation: 2,
            shadowColor: const Color(0x33845143),
          ),
        ),
        const SizedBox(height: 12),
        // Secondary: Nhắc mũi tiếp theo
        OutlinedButton.icon(
          onPressed:
              r.status == VaccinationStatus.deleted ||
                  r.status == VaccinationStatus.unknown
              ? null
              : _createReminder,
          icon: const Icon(Icons.notifications_active_outlined, size: 20),
          label: const Text(
            'Nhắc mũi tiếp theo',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              fontWeight: FontWeight.w600,
            ),
          ),
          style: OutlinedButton.styleFrom(
            foregroundColor: _primary,
            side: const BorderSide(color: _primaryContainer),
            minimumSize: const Size.fromHeight(50),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
        ),
        if (canDelete) ...[
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _delete,
            icon: const Icon(Icons.delete_outline, size: 20, color: _error),
            label: const Text(
              'Xóa hồ sơ tiêm chủng',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: _error,
              ),
            ),
            style: OutlinedButton.styleFrom(
              foregroundColor: _error,
              side: BorderSide(color: _error.withValues(alpha: 0.3)),
              minimumSize: const Size.fromHeight(50),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
        ],
      ],
    );
  }

  static String _dateOnlyString(DateTime value) {
    final local = value.toLocal();
    return '${local.year.toString().padLeft(4, '0')}-'
        '${local.month.toString().padLeft(2, '0')}-'
        '${local.day.toString().padLeft(2, '0')}';
  }
}

// ─── Shared info card ─────────────────────────────────────────────────────────
class _InfoCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final Widget child;

  const _InfoCard({
    required this.icon,
    required this.title,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFF0EAE6)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A000000),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: const Color(0xFFF7F1EE),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, size: 22, color: const Color(0xFF845143)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF1D1B19),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          child,
        ],
      ),
    );
  }
}
