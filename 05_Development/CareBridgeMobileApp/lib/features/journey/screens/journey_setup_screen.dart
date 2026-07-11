import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';
import '../../../core/network/api_client.dart';

/// CB-007 — Mother Journey Setup (UC-22)
/// Step 1 of 3 onboarding flow. User selects their maternal stage and optionally
/// enters date info. Calls POST /api/v1/journeys on submit.
class JourneySetupScreen extends StatefulWidget {
  const JourneySetupScreen({super.key});

  @override
  State<JourneySetupScreen> createState() => _JourneySetupScreenState();
}

enum _Stage { prePregnancy, pregnancy, postpartum }

class _JourneySetupScreenState extends State<JourneySetupScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _outline = Color(0xFF84736F);
  static const _tertiary = Color(0xFF625D59);
  static const _activeCardBorder = Color(0xFFC98C7B);
  static const _activeCardBg = Color(0x1AC98C7B);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  _Stage? _selectedStage;
  DateTime? _dueDate;
  DateTime? _lmpDate;
  DateTime? _babyBirthDate;
  bool _loading = false;
  String? _error;

  final _service = JourneyService();

  bool get _canSubmit => _selectedStage != null;

  String _formatDate(DateTime date) {
    final y = date.year.toString().padLeft(4, '0');
    final m = date.month.toString().padLeft(2, '0');
    final d = date.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }

  String _displayDate(DateTime date) {
    final d = date.day.toString().padLeft(2, '0');
    final m = date.month.toString().padLeft(2, '0');
    return '$d/$m/${date.year}';
  }

  DateTime _dueDateFromLmp(DateTime lmp) => lmp.add(const Duration(days: 280));

  Future<void> _pickDate({
    required DateTime? current,
    required DateTime firstDate,
    required DateTime lastDate,
    required ValueChanged<DateTime> onPicked,
  }) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: current ?? DateTime.now(),
      firstDate: firstDate,
      lastDate: lastDate,
      builder: (context, child) => Theme(
        data: Theme.of(context).copyWith(
          colorScheme: const ColorScheme.light(
            primary: _primaryContainer,
            onPrimary: Colors.white,
            surface: _canvas,
            onSurface: _onSurface,
          ),
        ),
        child: child!,
      ),
    );
    if (picked != null) onPicked(picked);
  }

  Future<void> _submit() async {
    if (!_canSubmit) return;
    setState(() { _loading = true; _error = null; });
    try {
      final today = _formatDate(DateTime.now());
      late CreateJourneyRequest req;
      switch (_selectedStage!) {
        case _Stage.prePregnancy:
          await JourneyService.clearOptimisticDashboard();
          req = CreateJourneyRequest(journeyType: JourneyType.prePregnancy, startDate: today);
        case _Stage.pregnancy:
          String? dueDate;
          String? lmpDate;
          DateTime? effectiveDueDate;
          if (_dueDate != null) {
            effectiveDueDate = _dueDate;
            dueDate = _formatDate(_dueDate!);
          } else if (_lmpDate != null) {
            lmpDate = _formatDate(_lmpDate!);
            effectiveDueDate = _dueDateFromLmp(_lmpDate!);
            dueDate = _formatDate(effectiveDueDate);
          }
          await JourneyService.cachePregnancyDates(
            estimatedDueDate: effectiveDueDate,
            lastMenstrualDate: _lmpDate,
          );
          req = CreateJourneyRequest(
              journeyType: JourneyType.pregnancy,
              startDate: today,
              lastMenstrualDate: lmpDate,
              estimatedDueDate: dueDate);
        case _Stage.postpartum:
          await JourneyService.clearOptimisticDashboard();
          final start = _babyBirthDate != null ? _formatDate(_babyBirthDate!) : today;
          req = CreateJourneyRequest(journeyType: JourneyType.postpartum, startDate: start);
      }
      await _service.createJourney(req);
      if (!mounted) return;
      // Navigate to Home screen after setting up the journey
      context.go('/');
    } on ApiException catch (e) {
      if (e.statusCode == 409) {
        try {
          final d = await _service.getDashboard();
          if (d.journeyId != null) {
            String? dDate;
            String? lDate;
            if (_dueDate != null) dDate = _formatDate(_dueDate!);
            if (_lmpDate != null) lDate = _formatDate(_lmpDate!);
            await JourneyService.cachePregnancyDates(
              estimatedDueDate:
                  _dueDate ?? (_lmpDate != null ? _dueDateFromLmp(_lmpDate!) : null),
              lastMenstrualDate: _lmpDate,
            );

            final upReq = UpdateJourneyRequest(
              estimatedDueDate: dDate,
              lastMenstrualDate: lDate,
            );
            await _service.updateJourney(d.journeyId!, upReq);
          }
        } catch (_) {}
        if (!mounted) return;
        context.go('/');
        return;
      }
      setState(() {
        _error = 'Không thể tạo hành trình. Vui lòng thử lại.';
      });
    } catch (_) {
      setState(() => _error = 'Lỗi kết nối. Vui lòng kiểm tra đường truyền.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Stack(
          children: [
            Column(
              children: [
                _buildAppBar(),
                _buildProgressBar(),
                Expanded(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.fromLTRB(24, 8, 24, 160),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildHeader(),
                        const SizedBox(height: 32),
                        _buildStageCards(),
                      ],
                    ),
                  ),
                ),
              ],
            ),
            Positioned(left: 0, right: 0, bottom: 0, child: _buildBottomAction()),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return SizedBox(
      height: 48,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.maybePop(context),
            icon: const Icon(Icons.arrow_back),
            color: _onSurface,
          ),
          const Expanded(
            child: Text(
              'BƯỚC 1 / 3',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                letterSpacing: 0.8,
                color: _onSurfaceVariant,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildProgressBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(99),
        child: Stack(
          children: [
            Container(height: 6, color: _surfaceContainerHighest),
            FractionallySizedBox(
              widthFactor: 1 / 3,
              child: Container(height: 6, color: _primaryContainer),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Thiết lập hành trình',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 32,
            fontWeight: FontWeight.w700,
            color: _primary,
            letterSpacing: -0.64,
            height: 1.25,
          ),
        ),
        SizedBox(height: 12),
        Text(
          'Chọn giai đoạn hiện tại của bạn để CareBridge có thể cá nhân hóa thông tin chăm sóc tốt nhất.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w400,
            color: _onSurfaceVariant,
            height: 1.5,
          ),
        ),
      ],
    );
  }

  Widget _buildStageCards() {
    return Column(
      children: [
        _StageCard(
          icon: Icons.favorite,
          title: 'Chuẩn bị mang thai',
          subtitle: 'Theo dõi chu kỳ và tối ưu hóa sức khỏe sinh sản',
          isSelected: _selectedStage == _Stage.prePregnancy,
          onTap: () => setState(() {
            _selectedStage = _Stage.prePregnancy;
            _dueDate = null; _lmpDate = null; _babyBirthDate = null; _error = null;
          }),
        ),
        _buildInlineForm(
          visible: _selectedStage == _Stage.prePregnancy,
          child: const Text(
            'Tuyệt vời! Chúng tôi sẽ chuẩn bị không gian tối ưu cho hành trình sắp tới của bạn.',
            textAlign: TextAlign.center,
            style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant, height: 1.5),
          ),
        ),
        const SizedBox(height: 16),
        _StageCard(
          icon: Icons.pregnant_woman,
          title: 'Đang mang thai',
          subtitle: 'Theo dõi thai kỳ, sự phát triển của bé và lịch khám',
          isSelected: _selectedStage == _Stage.pregnancy,
          onTap: () => setState(() {
            _selectedStage = _Stage.pregnancy;
            _babyBirthDate = null; _error = null;
          }),
        ),
        _buildInlineForm(
          visible: _selectedStage == _Stage.pregnancy,
          child: _buildPregnancyFields(),
        ),
        const SizedBox(height: 16),
        _StageCard(
          icon: Icons.child_care,
          title: 'Sau sinh',
          subtitle: 'Chăm sóc bé sơ sinh và phục hồi sức khỏe mẹ',
          isSelected: _selectedStage == _Stage.postpartum,
          onTap: () => setState(() {
            _selectedStage = _Stage.postpartum;
            _dueDate = null; _lmpDate = null; _error = null;
          }),
        ),
        _buildInlineForm(
          visible: _selectedStage == _Stage.postpartum,
          child: _buildPostpartumFields(),
        ),
      ],
    );
  }

  Widget _buildInlineForm({required bool visible, required Widget child}) {
    return AnimatedSize(
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
      child: visible
          ? Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: _surfaceContainerLow,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: _outlineVariant),
                ),
                child: child,
              ),
            )
          : const SizedBox.shrink(),
    );
  }

  Widget _buildPregnancyFields() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Thông tin thai kỳ',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: _primary,
          ),
        ),
        const SizedBox(height: 16),
        _DateField(
          label: 'Ngày dự sinh (nếu biết)',
          icon: Icons.calendar_today,
          displayValue: _dueDate != null ? _displayDate(_dueDate!) : null,
          onTap: () => _pickDate(
            current: _dueDate,
            firstDate: DateTime.now(),
            lastDate: DateTime.now().add(const Duration(days: 280)),
            onPicked: (d) => setState(() { _dueDate = d; _lmpDate = null; }),
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: const [
            Expanded(child: Divider(color: _outlineVariant)),
            Padding(
              padding: EdgeInsets.symmetric(horizontal: 12),
              child: Text(
                'HOẶC',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 0.5,
                  color: _outline,
                ),
              ),
            ),
            Expanded(child: Divider(color: _outlineVariant)),
          ],
        ),
        const SizedBox(height: 16),
        _DateField(
          label: 'Ngày đầu kỳ kinh cuối (LMP)',
          icon: Icons.calendar_month,
          displayValue: _lmpDate != null ? _displayDate(_lmpDate!) : null,
          onTap: () => _pickDate(
            current: _lmpDate,
            firstDate: DateTime.now().subtract(const Duration(days: 280)),
            lastDate: DateTime.now(),
            onPicked: (d) => setState(() { _lmpDate = d; _dueDate = null; }),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          _lmpDate != null
              ? 'Ngày dự sinh ước tính: ${_displayDate(_dueDateFromLmp(_lmpDate!))}'
              : 'CareBridge sẽ tự động tính toán ngày dự sinh cho bạn.',
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            color: _onSurfaceVariant,
            height: 1.4,
          ),
        ),
      ],
    );
  }

  Widget _buildPostpartumFields() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Thông tin bé yêu',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: _primary,
          ),
        ),
        const SizedBox(height: 16),
        _DateField(
          label: 'Ngày sinh của bé',
          icon: Icons.cake,
          displayValue: _babyBirthDate != null ? _displayDate(_babyBirthDate!) : null,
          onTap: () => _pickDate(
            current: _babyBirthDate,
            firstDate: DateTime.now().subtract(const Duration(days: 365 * 2)),
            lastDate: DateTime.now(),
            onPicked: (d) => setState(() => _babyBirthDate = d),
          ),
        ),
      ],
    );
  }

  Widget _buildBottomAction() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
          colors: [_canvas, _canvas, Colors.transparent],
          stops: [0.0, 0.65, 1.0],
        ),
      ),
      padding: const EdgeInsets.fromLTRB(24, 48, 24, 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (_error != null) ...[
            Container(
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: _errorBg,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _errorText,
                  height: 1.4,
                ),
              ),
            ),
          ],
          Row(
            children: const [
              Icon(Icons.lock, size: 14, color: _tertiary),
              SizedBox(width: 6),
              Expanded(
                child: Text(
                  'Thông tin của bạn được mã hóa và bảo mật an toàn. '
                  'Chúng tôi sử dụng dữ liệu này để cá nhân hóa nội dung y khoa.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    color: _tertiary,
                    height: 1.4,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: _canSubmit && !_loading ? _submit : null,
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                foregroundColor: Colors.white,
                disabledBackgroundColor: _primaryContainer.withAlpha(128),
                disabledForegroundColor: Colors.white.withAlpha(178),
                shape: const StadiumBorder(),
              ),
              child: _loading
                  ? const SizedBox(
                      width: 22, height: 22,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Text(
                      'Bắt đầu hành trình',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Reusable sub-widgets ────────────────────────────────────────────────────

class _StageCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final bool isSelected;
  final VoidCallback onTap;

  const _StageCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.isSelected,
    required this.onTap,
  });

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _activeCardBorder = Color(0xFFC98C7B);
  static const _activeCardBg = Color(0x1AC98C7B);

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
      decoration: BoxDecoration(
        color: isSelected ? _activeCardBg : Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: isSelected ? _activeCardBorder : Colors.transparent,
          width: 2,
        ),
        boxShadow: isSelected
            ? []
            : [
                BoxShadow(
                  color: const Color(0xFF5A463F).withAlpha(15),
                  blurRadius: 20,
                  offset: const Offset(0, 4),
                ),
              ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                AnimatedContainer(
                  duration: const Duration(milliseconds: 300),
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: isSelected ? _primary : _surfaceContainerHighest,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(icon, size: 26,
                      color: isSelected ? Colors.white : _primary),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 20,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                            height: 1.4,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          subtitle,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            fontWeight: FontWeight.w400,
                            color: _onSurfaceVariant,
                            height: 1.43,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Padding(
                  padding: const EdgeInsets.only(top: 10),
                  child: AnimatedOpacity(
                    duration: const Duration(milliseconds: 200),
                    opacity: isSelected ? 1.0 : 0.0,
                    child: const Icon(Icons.check_circle,
                        color: _primaryContainer, size: 24),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _DateField extends StatelessWidget {
  final String label;
  final IconData icon;
  final String? displayValue;
  final VoidCallback onTap;

  const _DateField({
    required this.label,
    required this.icon,
    required this.displayValue,
    required this.onTap,
  });

  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _outline = Color(0xFF84736F);

  @override
  Widget build(BuildContext context) {
    final hasValue = displayValue != null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w500,
            letterSpacing: 0.5,
            color: _onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 8),
        GestureDetector(
          onTap: onTap,
          child: Container(
            height: 56,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: hasValue ? _primaryContainer : _outlineVariant,
                width: hasValue ? 1.5 : 1,
              ),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    displayValue ?? 'DD/MM/YYYY',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w400,
                      color: hasValue ? _onSurface : _outline,
                      height: 1.5,
                    ),
                  ),
                ),
                Icon(icon, size: 22, color: _onSurfaceVariant),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
