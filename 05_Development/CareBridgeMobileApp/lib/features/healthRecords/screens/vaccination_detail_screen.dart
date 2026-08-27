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
  static const _surfaceContainerLow = Color(0xFFF9F2EE);
  static const _surfaceVariant = Color(0xFFE7E1DD);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onErrorContainer = Color(0xFF93000A);
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

  Future<void> _reschedule() async {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text(
          'Dời lịch mũi tiêm từ hồ sơ chưa được hỗ trợ. Bạn có thể tạo nhắc tiêm riêng.',
        ),
      ),
    );
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
      child: Column(
        children: [
          _buildAppBar(),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 32),
            child: Column(
              children: [
                _buildHeader(r),
                const SizedBox(height: 24),
                _buildScheduleCard(r),
                const SizedBox(height: 16),
                _buildFacilityCard(r),
                const SizedBox(height: 16),
                _buildChildCard(r),
                if (note?.trim().isNotEmpty == true) ...[
                  const SizedBox(height: 16),
                  _buildNoteCard(note!),
                ],
                const SizedBox(height: 32),
                _buildActions(r),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAppBar() {
    return Container(
      height: 64,
      color: _canvas,
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back, color: _primary),
          ),
          const Expanded(
            child: Text(
              'CareBridge',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w700,
                color: _primary,
              ),
            ),
          ),
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: _surfaceVariant,
            ),
            child: const Icon(Icons.person, size: 20, color: _onSurfaceVariant),
          ),
          const SizedBox(width: 8),
        ],
      ),
    );
  }

  Widget _buildHeader(VaccinationRecord r) {
    return Column(
      children: [
        Container(
          width: 64,
          height: 64,
          decoration: const BoxDecoration(
            shape: BoxShape.circle,
            color: _primaryContainer,
          ),
          child: const Icon(Icons.vaccines, size: 32, color: Colors.white),
        ),
        const SizedBox(height: 16),
        Text(
          r.vaccineName,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: _textHeading,
          ),
        ),
        const SizedBox(height: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          decoration: BoxDecoration(
            color: _primaryContainer.withAlpha(26),
            borderRadius: BorderRadius.circular(99),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.check_circle, size: 16, color: _primary),
              const SizedBox(width: 6),
              Text(
                r.status.displayLabel,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: _primary,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildScheduleCard(VaccinationRecord r) {
    return _InfoCard(
      icon: Icons.calendar_month,
      title: 'Lịch trình',
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Ngày dự kiến',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.5,
                    color: _onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  r.plannedDateLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _textHeading,
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Ngày tiêm thực tế',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.5,
                    color: _onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  r.actualDateLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _textHeading,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFacilityCard(VaccinationRecord r) {
    return _InfoCard(
      icon: Icons.local_hospital,
      title: 'Cơ sở y tế',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            r.facilityName ?? '—',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: _textHeading,
            ),
          ),
          if (r.facilityAddress != null) ...[
            const SizedBox(height: 8),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(
                  Icons.location_on,
                  size: 18,
                  color: _onSurfaceVariant,
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    r.facilityAddress!,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildChildCard(VaccinationRecord r) {
    return _InfoCard(
      icon: Icons.child_care,
      title: 'Hồ sơ trẻ em',
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: _surfaceVariant,
            ),
            child: const Icon(Icons.face, size: 28, color: _onSurfaceVariant),
          ),
          const SizedBox(width: 16),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                r.childName ?? '—',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: _textHeading,
                ),
              ),
              if (r.childBirthLabel.isNotEmpty)
                Text(
                  r.childBirthLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildNoteCard(String note) {
    return _InfoCard(
      icon: Icons.edit_note,
      title: 'Ghi chú',
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: _surfaceContainerLow,
          borderRadius: BorderRadius.circular(12),
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
    final canPostpone =
        r.status != VaccinationStatus.completed &&
        r.status != VaccinationStatus.deleted &&
        r.status != VaccinationStatus.unknown;
    return Column(
      children: [
        // Primary: Cập nhật thông tin
        SizedBox(
          width: double.infinity,
          height: 56,
          child: FilledButton.icon(
            onPressed: canEdit ? _edit : null,
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              shape: const StadiumBorder(),
              elevation: 2,
            ),
            icon: const Icon(Icons.edit, size: 20),
            label: const Text(
              'Cập nhật thông tin',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
        const SizedBox(height: 12),
        SizedBox(
          width: double.infinity,
          height: 50,
          child: OutlinedButton.icon(
            onPressed:
                r.status == VaccinationStatus.deleted ||
                    r.status == VaccinationStatus.unknown
                ? null
                : _createReminder,
            icon: const Icon(Icons.notifications_active_outlined),
            label: const Text(
              'Nhắc mũi tiếp theo',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w600,
              ),
            ),
            style: OutlinedButton.styleFrom(
              foregroundColor: _primary,
              side: const BorderSide(color: _primaryContainer),
              shape: const StadiumBorder(),
            ),
          ),
        ),
        const SizedBox(height: 12),
        // Secondary row: Dời lịch + Xóa hồ sơ
        Row(
          children: [
            Expanded(
              child: SizedBox(
                height: 48,
                child: OutlinedButton(
                  onPressed: canPostpone ? _reschedule : null,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _textHeading,
                    side: BorderSide(color: _surfaceVariant, width: 2),
                    shape: const StadiumBorder(),
                  ),
                  child: const Text(
                    'Dời lịch',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: SizedBox(
                height: 48,
                child: FilledButton(
                  onPressed: canDelete ? _delete : null,
                  style: FilledButton.styleFrom(
                    backgroundColor: _errorContainer,
                    foregroundColor: _onErrorContainer,
                    shape: const StadiumBorder(),
                  ),
                  child: const Text(
                    'Xóa hồ sơ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
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
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFC98C7B).withAlpha(20),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 20, color: const Color(0xFF845143)),
              const SizedBox(width: 8),
              Text(
                title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF2D2A28),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }
}
