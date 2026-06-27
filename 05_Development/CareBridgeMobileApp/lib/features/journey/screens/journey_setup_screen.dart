import 'package:flutter/material.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';
import '../../../core/network/api_client.dart';

// Design tokens from CareBridge design system
const _primary = Color(0xFF845143);
const _primaryContainer = Color(0xFFC98C7B);
const _canvas = Color(0xFFFFF8F6);
const _surfaceContainerHighest = Color(0xFFFADCD3);
const _surfaceContainerLow = Color(0xFFFFF1EC);
const _onSurface = Color(0xFF271812);
const _onSurfaceVariant = Color(0xFF524440);
const _outlineVariant = Color(0xFFD6C2BD);
const _outline = Color(0xFF84736F);
const _tertiary = Color(0xFF625D59);
const _activeCardBorder = Color(0xFFC98C7B);
const _activeCardBg = Color(0x1AC98C7B); // rgba(201,140,123,0.1)

enum _Stage { prePregnancy, pregnancy, postpartum }

class JourneySetupScreen extends StatefulWidget {
  const JourneySetupScreen({super.key});

  @override
  State<JourneySetupScreen> createState() => _JourneySetupScreenState();
}

class _JourneySetupScreenState extends State<JourneySetupScreen> {
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
    final y = date.year.toString();
    return '$d/$m/$y';
  }

  // Calculate estimated due date from LMP: LMP + 280 days
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
      locale: const Locale('vi', 'VN'),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: _primaryContainer,
              onPrimary: Colors.white,
              surface: _canvas,
              onSurface: _onSurface,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) onPicked(picked);
  }

  Future<void> _submit() async {
    if (!_canSubmit) return;
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final today = _formatDate(DateTime.now());
      late CreateJourneyRequest req;

      switch (_selectedStage!) {
        case _Stage.prePregnancy:
          req = CreateJourneyRequest(
            journeyType: JourneyType.prePregnancy,
            startDate: today,
          );
        case _Stage.pregnancy:
          String? dueDate;
          if (_dueDate != null) {
            dueDate = _formatDate(_dueDate!);
          } else if (_lmpDate != null) {
            dueDate = _formatDate(_dueDateFromLmp(_lmpDate!));
          }
          req = CreateJourneyRequest(
            journeyType: JourneyType.pregnancy,
            startDate: today,
            estimatedDueDate: dueDate,
          );
        case _Stage.postpartum:
          final start = _babyBirthDate != null
              ? _formatDate(_babyBirthDate!)
              : today;
          req = CreateJourneyRequest(
            journeyType: JourneyType.postpartum,
            startDate: start,
          );
      }

      await _service.createJourney(req);
      if (!mounted) return;
      // TODO: navigate to step 2/3 of onboarding when that screen is available
      Navigator.of(context).pop();
    } on ApiException catch (e) {
      setState(() {
        _error = e.statusCode == 409
            ? 'Bạn đã có một hành trình đang hoạt động.'
            : 'Không thể tạo hành trình. Vui lòng thử lại.';
      });
    } catch (_) {
      setState(() {
        _error = 'Lỗi kết nối. Vui lòng kiểm tra đường truyền.';
      });
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
                        AnimatedSize(
                          duration: const Duration(milliseconds: 400),
                          curve: Curves.easeInOut,
                          child: _selectedStage != null
                              ? _buildDynamicFields()
                              : const SizedBox.shrink(),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              child: _buildBottomAction(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
      child: SizedBox(
        height: 48,
        child: Row(
          children: [
            _IconButton(
              icon: Icons.arrow_back,
              onTap: () => Navigator.maybePop(context),
            ),
            const Expanded(
              child: Text(
                'BƯỚC 1 / 3',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 0.8,
                  color: _onSurfaceVariant,
                ),
              ),
            ),
            const SizedBox(width: 40),
          ],
        ),
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: const [
        Text(
          'Thiết lập hành trình',
          style: TextStyle(
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
            _dueDate = null;
            _lmpDate = null;
            _babyBirthDate = null;
            _error = null;
          }),
        ),
        const SizedBox(height: 16),
        _StageCard(
          icon: Icons.pregnant_woman,
          title: 'Đang mang thai',
          subtitle: 'Theo dõi thai kỳ, sự phát triển của bé và lịch khám',
          isSelected: _selectedStage == _Stage.pregnancy,
          onTap: () => setState(() {
            _selectedStage = _Stage.pregnancy;
            _babyBirthDate = null;
            _error = null;
          }),
        ),
        const SizedBox(height: 16),
        _StageCard(
          icon: Icons.child_care,
          title: 'Sau sinh',
          subtitle: 'Chăm sóc bé sơ sinh và phục hồi sức khỏe mẹ',
          isSelected: _selectedStage == _Stage.postpartum,
          onTap: () => setState(() {
            _selectedStage = _Stage.postpartum;
            _dueDate = null;
            _lmpDate = null;
            _error = null;
          }),
        ),
      ],
    );
  }

  Widget _buildDynamicFields() {
    return Padding(
      padding: const EdgeInsets.only(top: 24),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: _surfaceContainerLow,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: _outlineVariant),
        ),
        child: switch (_selectedStage) {
          _Stage.prePregnancy => const Text(
              'Tuyệt vời! Chúng tôi sẽ chuẩn bị không gian tối ưu cho hành trình sắp tới của bạn.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
            ),
          _Stage.pregnancy => _buildPregnancyFields(),
          _Stage.postpartum => _buildPostpartumFields(),
          null => const SizedBox.shrink(),
        },
      ),
    );
  }

  Widget _buildPregnancyFields() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Thông tin thai kỳ',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: _primary,
          ),
        ),
        const SizedBox(height: 16),
        _DateField(
          label: 'Ngày dự sinh (nếu biết)',
          icon: Icons.calendar_today,
          value: _dueDate,
          displayValue: _dueDate != null ? _displayDate(_dueDate!) : null,
          onTap: () => _pickDate(
            current: _dueDate,
            firstDate: DateTime.now(),
            lastDate: DateTime.now().add(const Duration(days: 280)),
            onPicked: (d) => setState(() {
              _dueDate = d;
              _lmpDate = null;
            }),
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
          value: _lmpDate,
          displayValue: _lmpDate != null ? _displayDate(_lmpDate!) : null,
          onTap: () => _pickDate(
            current: _lmpDate,
            firstDate: DateTime.now().subtract(const Duration(days: 280)),
            lastDate: DateTime.now(),
            onPicked: (d) => setState(() {
              _lmpDate = d;
              _dueDate = null;
            }),
          ),
        ),
        if (_lmpDate != null) ...[
          const SizedBox(height: 8),
          Text(
            'Ngày dự sinh ước tính: ${_displayDate(_dueDateFromLmp(_lmpDate!))}',
            style: const TextStyle(
              fontSize: 13,
              color: _onSurfaceVariant,
              height: 1.4,
            ),
          ),
        ] else ...[
          const SizedBox(height: 8),
          const Text(
            'CareBridge sẽ tự động tính toán ngày dự sinh cho bạn.',
            style: TextStyle(
              fontSize: 13,
              color: _onSurfaceVariant,
              height: 1.4,
            ),
          ),
        ],
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
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: _primary,
          ),
        ),
        const SizedBox(height: 16),
        _DateField(
          label: 'Ngày sinh của bé',
          icon: Icons.cake,
          value: _babyBirthDate,
          displayValue:
              _babyBirthDate != null ? _displayDate(_babyBirthDate!) : null,
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
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              margin: const EdgeInsets.only(bottom: 12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFDAD6),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 13,
                  color: Color(0xFF93000A),
                  height: 1.4,
                ),
              ),
            ),
          ],
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.lock, size: 14, color: _tertiary),
              const SizedBox(width: 6),
              const Expanded(
                child: Text(
                  'Thông tin của bạn được mã hóa và bảo mật an toàn. '
                  'Chúng tôi sử dụng dữ liệu này để cá nhân hóa nội dung y khoa.',
                  style: TextStyle(
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
            height: 56,
            child: TextButton(
              onPressed: _canSubmit && !_loading ? _submit : null,
              style: TextButton.styleFrom(
                backgroundColor: _canSubmit
                    ? _primaryContainer
                    : _primaryContainer.withOpacity(0.5),
                foregroundColor: Colors.white,
                disabledBackgroundColor: _primaryContainer.withOpacity(0.5),
                disabledForegroundColor: Colors.white.withOpacity(0.7),
                shape: const StadiumBorder(),
                elevation: 0,
                shadowColor: Colors.transparent,
              ),
              child: _loading
                  ? const SizedBox(
                      height: 22,
                      width: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Bắt đầu hành trình',
                      style: TextStyle(
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

class _IconButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;

  const _IconButton({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(99),
      child: InkWell(
        borderRadius: BorderRadius.circular(99),
        onTap: onTap,
        child: SizedBox(
          width: 40,
          height: 40,
          child: Icon(icon, color: _onSurface, size: 24),
        ),
      ),
    );
  }
}

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
                  color: const Color(0xFF5A463F).withOpacity(0.06),
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
                  child: Icon(
                    icon,
                    size: 26,
                    color: isSelected ? Colors.white : _primary,
                  ),
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
                    child: const Icon(
                      Icons.check_circle,
                      color: _primaryContainer,
                      size: 24,
                    ),
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
  final DateTime? value;
  final String? displayValue;
  final VoidCallback onTap;

  const _DateField({
    required this.label,
    required this.icon,
    required this.value,
    required this.displayValue,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
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
                color: value != null ? _primaryContainer : _outlineVariant,
                width: value != null ? 1.5 : 1,
              ),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    displayValue ?? 'DD/MM/YYYY',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w400,
                      color: value != null ? _onSurface : _outline,
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
