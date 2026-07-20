import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';

typedef CreateJourneyCallback =
    Future<CreateJourneyResponse> Function(CreateJourneyRequest request);
typedef LoadJourneyDashboardCallback = Future<JourneyDashboard> Function();

/// Creates a self-reported postpartum journey without pregnancy or baby data.
class PostpartumRecoverySetupScreen extends StatefulWidget {
  const PostpartumRecoverySetupScreen({
    super.key,
    this.initialRecoveryStartDate,
    this.now,
    this.createJourney,
    this.loadDashboard,
  });

  final DateTime? initialRecoveryStartDate;
  final DateTime Function()? now;
  final CreateJourneyCallback? createJourney;
  final LoadJourneyDashboardCallback? loadDashboard;

  @override
  State<PostpartumRecoverySetupScreen> createState() =>
      _PostpartumRecoverySetupScreenState();
}

class _PostpartumRecoverySetupScreenState
    extends State<PostpartumRecoverySetupScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _canvas = Color(0xFFF6F1EC);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF7D6961);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  late DateTime? _recoveryStartDate;
  bool _isExact = true;
  bool _loading = false;
  String? _dateError;
  String? _submitError;

  DateTime get _now => (widget.now ?? DateTime.now).call();

  @override
  void initState() {
    super.initState();
    _recoveryStartDate = widget.initialRecoveryStartDate;
  }

  DateTime _dateOnly(DateTime date) =>
      DateTime(date.year, date.month, date.day);

  String _apiDate(DateTime date) {
    final year = date.year.toString().padLeft(4, '0');
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }

  String _displayDate(DateTime date) =>
      '${date.day.toString().padLeft(2, '0')}/'
      '${date.month.toString().padLeft(2, '0')}/${date.year}';

  Future<void> _pickDate() async {
    final today = _dateOnly(_now);
    final current = _recoveryStartDate;
    final initialDate = current == null || _dateOnly(current).isAfter(today)
        ? today
        : _dateOnly(current);
    final selected = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(1900),
      lastDate: today,
      helpText: 'Chọn ngày bắt đầu hồi phục',
      cancelText: 'Hủy',
      confirmText: 'Chọn',
    );
    if (selected == null || !mounted) return;
    setState(() {
      _recoveryStartDate = selected;
      _dateError = null;
      _submitError = null;
    });
  }

  bool _validateDate() {
    final selected = _recoveryStartDate;
    if (selected == null) {
      setState(() => _dateError = 'Vui lòng chọn ngày bắt đầu hồi phục.');
      return false;
    }
    if (_dateOnly(selected).isAfter(_dateOnly(_now))) {
      setState(() => _dateError = 'Ngày bắt đầu không thể ở trong tương lai.');
      return false;
    }
    setState(() => _dateError = null);
    return true;
  }

  Future<void> _submit() async {
    if (_loading || !_validateDate()) return;
    setState(() {
      _loading = true;
      _submitError = null;
    });
    final request = CreateJourneyRequest(
      journeyType: JourneyType.postpartum,
      startDate: _apiDate(_recoveryStartDate!),
      dateSource: 'SELF_REPORTED',
      dateConfidence: _isExact ? 'CONFIRMED' : 'ESTIMATED',
      changeReason: 'INITIAL_SETUP',
      effectiveAt: _now.toUtc().toIso8601String(),
      notes: 'Direct postpartum recovery setup',
    );
    final service = JourneyService();
    try {
      final create = widget.createJourney ?? service.createJourney;
      await create(request);
      if (!mounted) return;
      context.go('/mother-home?tab=1');
    } on ApiException catch (error) {
      if (!mounted) return;
      if (error.statusCode == 409) {
        try {
          final loadDashboard = widget.loadDashboard ?? service.getDashboard;
          final dashboard = await loadDashboard();
          if (!mounted) return;
          if (dashboard.isPostpartum &&
              dashboard.status == 'ACTIVE_POSTPARTUM') {
            context.go('/mother-home?tab=1');
            return;
          }
        } catch (_) {
          if (!mounted) return;
        }
        setState(() {
          _submitError = 'Bạn đã có một hành trình đang hoạt động.';
        });
      } else {
        setState(() {
          _submitError = 'Chưa thể tạo hành trình. Vui lòng thử lại.';
        });
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _submitError = 'Không thể kết nối đến máy chủ. Vui lòng thử lại.';
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        foregroundColor: _primaryDark,
        elevation: 0,
        title: const Text(
          'Hành trình sau sinh',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Chăm sóc chính bạn trong giai đoạn hồi phục',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 28,
                  fontWeight: FontWeight.w800,
                  height: 1.2,
                  color: _primaryDark,
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                'Bạn có thể bắt đầu mà không cần nhập thông tin thai kỳ hoặc tạo hồ sơ em bé.',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  height: 1.5,
                  color: _text,
                ),
              ),
              const SizedBox(height: 28),
              _buildDateCard(),
              if (_submitError != null) ...[
                const SizedBox(height: 16),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: _errorBg,
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: Text(
                    _submitError!,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      color: _errorText,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 56,
                child: FilledButton(
                  key: const Key('postpartum-submit'),
                  onPressed: _loading ? null : _submit,
                  style: FilledButton.styleFrom(
                    backgroundColor: _primary,
                    foregroundColor: Colors.white,
                    shape: const StadiumBorder(),
                  ),
                  child: _loading
                      ? const SizedBox(
                          width: 22,
                          height: 22,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Text(
                          'Bắt đầu hành trình',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDateCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(28),
        boxShadow: const [
          BoxShadow(
            color: Color(0x145A463F),
            blurRadius: 28,
            offset: Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Ngày bắt đầu hồi phục',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: _text,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Đây không nhất thiết là ngày sinh.',
            style: TextStyle(fontFamily: 'Lexend', color: _muted),
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            height: 56,
            child: OutlinedButton.icon(
              key: const Key('postpartum-start-date'),
              onPressed: _loading ? null : _pickDate,
              icon: const Icon(Icons.calendar_today_rounded),
              label: Text(
                _recoveryStartDate == null
                    ? 'Chọn ngày'
                    : _displayDate(_recoveryStartDate!),
              ),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primaryDark,
                side: const BorderSide(color: _primary),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
            ),
          ),
          if (_dateError != null) ...[
            const SizedBox(height: 10),
            Text(
              _dateError!,
              key: const Key('postpartum-date-error'),
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _errorText,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
          const SizedBox(height: 24),
          const Text(
            'Mức độ chính xác',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: _text,
            ),
          ),
          const SizedBox(height: 12),
          SegmentedButton<bool>(
            segments: const [
              ButtonSegment(
                value: true,
                label: Text('Chính xác'),
                icon: Icon(Icons.check_circle_outline_rounded),
              ),
              ButtonSegment(
                value: false,
                label: Text(
                  'Ước tính',
                  key: Key('postpartum-confidence-estimated'),
                ),
                icon: Icon(Icons.schedule_rounded),
              ),
            ],
            selected: {_isExact},
            onSelectionChanged: _loading
                ? null
                : (selection) => setState(() {
                    _isExact = selection.first;
                    _submitError = null;
                  }),
            style: const ButtonStyle(
              minimumSize: WidgetStatePropertyAll(Size.fromHeight(52)),
              visualDensity: VisualDensity.comfortable,
            ),
          ),
        ],
      ),
    );
  }
}
