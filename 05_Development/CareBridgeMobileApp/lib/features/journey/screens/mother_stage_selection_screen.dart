import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../auth/services/auth_service.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';

/// First mother-specific onboarding gate before selecting the concrete setup flow.
class MotherStageSelectionScreen extends StatefulWidget {
  const MotherStageSelectionScreen({super.key});

  @override
  State<MotherStageSelectionScreen> createState() =>
      _MotherStageSelectionScreenState();
}

enum _MotherStage { planning, pregnant, babyCare }

class _MotherStageSelectionScreenState
    extends State<MotherStageSelectionScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _canvas = Color(0xFFF6F1EC);
  static const _text = Color(0xFF5A463F);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  final _journeyService = JourneyService();

  _MotherStage? _selectedStage;
  bool _loading = false;
  String? _error;

  bool get _canContinue => _selectedStage != null && !_loading;

  String _formatApiDate(DateTime date) {
    final y = date.year.toString().padLeft(4, '0');
    final m = date.month.toString().padLeft(2, '0');
    final d = date.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }

  String get _buttonLabel {
    switch (_selectedStage) {
      case _MotherStage.planning:
        return 'Tạo hành trình chuẩn bị';
      case _MotherStage.pregnant:
        return 'Tiếp tục tính thai kỳ';
      case _MotherStage.babyCare:
        return 'Thiết lập hồ sơ bé';
      case null:
        return 'Tiếp tục';
    }
  }

  Future<void> _continue() async {
    final stage = _selectedStage;
    if (stage == null) return;

    if (stage == _MotherStage.pregnant) {
      context.go('/journey-setup');
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });

    final now = DateTime.now();
    final journeyType = stage == _MotherStage.planning
        ? JourneyType.prePregnancy
        : JourneyType.babyCare;
    final notes = stage == _MotherStage.planning
        ? 'Selected mother stage: planning pregnancy'
        : 'Selected mother stage: caring for baby';

    try {
      await _journeyService.createJourney(
        CreateJourneyRequest(
          journeyType: journeyType,
          startDate: _formatApiDate(DateTime(now.year, now.month, now.day)),
          notes: notes,
        ),
      );
      await AuthService.instance.refreshSession();
      if (!mounted) return;
      context.go(
        stage == _MotherStage.planning ? '/' : '/babies/add?entry=onboarding',
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.statusCode == 409
            ? 'Bạn đã có hành trình đang hoạt động cho lựa chọn này.'
            : 'Không thể tạo hành trình. Vui lòng thử lại.';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _error = 'Không thể kết nối đến máy chủ.');
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
            SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(24, 28, 24, 148),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildHeader(),
                  const SizedBox(height: 28),
                  _StageCard(
                    icon: Icons.spa_rounded,
                    title: 'Muốn mang thai',
                    subtitle:
                        'Nhận nội dung chuẩn bị sức khỏe, dinh dưỡng và nhắc việc trước thai kỳ.',
                    selected: _selectedStage == _MotherStage.planning,
                    onTap: () => setState(() {
                      _selectedStage = _MotherStage.planning;
                      _error = null;
                    }),
                  ),
                  const SizedBox(height: 16),
                  _StageCard(
                    icon: Icons.favorite_rounded,
                    title: 'Đang mang thai',
                    subtitle:
                        'Tính tuổi thai, ngày dự sinh và cá nhân hóa hành trình theo tuần thai.',
                    selected: _selectedStage == _MotherStage.pregnant,
                    onTap: () => setState(() {
                      _selectedStage = _MotherStage.pregnant;
                      _error = null;
                    }),
                  ),
                  const SizedBox(height: 16),
                  _StageCard(
                    icon: Icons.child_care_rounded,
                    title: 'Đang nuôi bé',
                    subtitle:
                        'Tạo hồ sơ bé để theo dõi tăng trưởng, cột mốc và lịch chăm sóc hằng ngày.',
                    selected: _selectedStage == _MotherStage.babyCare,
                    onTap: () => setState(() {
                      _selectedStage = _MotherStage.babyCare;
                      _error = null;
                    }),
                  ),
                ],
              ),
            ),
            Positioned(left: 0, right: 0, bottom: 0, child: _buildBottomBar()),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: _primary.withAlpha(38),
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.auto_awesome_rounded, color: _primary),
        ),
        const SizedBox(height: 20),
        const Text(
          'Bạn đang ở giai đoạn nào?',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 31,
            fontWeight: FontWeight.w800,
            color: _primaryDark,
            height: 1.18,
          ),
        ),
        const SizedBox(height: 12),
        const Text(
          'CareBridge sẽ mở đúng hành trình chăm sóc cho mẹ và gia đình.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            color: _text,
            height: 1.5,
          ),
        ),
      ],
    );
  }

  Widget _buildBottomBar() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
          colors: [_canvas, _canvas, Color(0x00F6F1EC)],
          stops: [0.0, 0.72, 1.0],
        ),
      ),
      padding: const EdgeInsets.fromLTRB(24, 48, 24, 28),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (_error != null) ...[
            Container(
              width: double.infinity,
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: _errorBg,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _errorText,
                  height: 1.4,
                ),
              ),
            ),
          ],
          SizedBox(
            width: double.infinity,
            height: 56,
            child: FilledButton(
              onPressed: _canContinue ? _continue : null,
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                disabledBackgroundColor: _primary.withAlpha(112),
                shape: const StadiumBorder(),
                elevation: 0,
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
                  : Text(
                      _buttonLabel,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class _StageCard extends StatelessWidget {
  const _StageCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.selected,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final bool selected;
  final VoidCallback onTap;

  static const _primary = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);
  static const _border = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      decoration: BoxDecoration(
        color: selected ? _surfaceLow : _surface,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(
          color: selected ? _primary : _border.withAlpha(160),
          width: 2,
        ),
        boxShadow: [
          BoxShadow(
            color: _text.withAlpha(15),
            blurRadius: 30,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(28),
        child: InkWell(
          borderRadius: BorderRadius.circular(28),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    color: selected ? _primary : _surfaceLow,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    icon,
                    color: selected ? Colors.white : _primary,
                    size: 28,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 20,
                          fontWeight: FontWeight.w800,
                          color: _text,
                          height: 1.25,
                        ),
                      ),
                      const SizedBox(height: 7),
                      Text(
                        subtitle,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: _muted,
                          height: 1.45,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                AnimatedOpacity(
                  opacity: selected ? 1 : 0,
                  duration: const Duration(milliseconds: 160),
                  child: const Icon(
                    Icons.check_circle_rounded,
                    color: _primary,
                    size: 24,
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
