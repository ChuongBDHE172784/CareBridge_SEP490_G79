import 'package:flutter/material.dart';
import '../models/exercise_model.dart';
import 'exercise_history_screen.dart';

class ExerciseSessionResultScreen extends StatelessWidget {
  final SessionResult result;

  const ExerciseSessionResultScreen({super.key, required this.result});

  static const _canvas = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(context),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
                child: Column(
                  children: [
                    _buildSuccessCard(),
                    const SizedBox(height: 20),
                    _buildStatsGrid(),
                    const SizedBox(height: 32),
                    _buildActions(context),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      color: _canvas,
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back, color: _onSurface),
              padding: EdgeInsets.zero,
            ),
          ),
          const Expanded(
            child: Text(
              'Kết quả',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ),
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
              icon: const Icon(Icons.close, color: _onSurface),
              padding: EdgeInsets.zero,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSuccessCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surfaceLowest,
        borderRadius: BorderRadius.circular(20),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.08),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            width: 64,
            height: 64,
            decoration: BoxDecoration(
              color: _primaryContainer,
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.check_circle_outline,
              color: Colors.white,
              size: 32,
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            'Tuyệt vời, Mẹ!',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.w700,
              color: _primary,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Mẹ đã hoàn thành xuất sắc bài tập "${result.exerciseTitle}".',
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatsGrid() {
    final completionInt = result.completionPercent.round();
    final postureCorrections = result.postureScore != null
        ? (result.postureScore! / 10).round()
        : 0;

    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: _statCell(
                icon: Icons.timer_outlined,
                label: 'THỜI GIAN',
                value: '${result.actualDurationMinutes}',
                unit: 'phút',
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _statCell(
                icon: Icons.loop_outlined,
                label: 'HOÀN THÀNH',
                value: '$completionInt',
                unit: '%',
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _statCell(
                icon: Icons.accessibility_new_outlined,
                label: 'CHỈNH TƯ THẾ',
                value: '$postureCorrections',
                unit: 'lần',
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _statCell(
                icon: Icons.warning_amber_outlined,
                label: 'CẢNH BÁO AN TOÀN',
                value: '${result.warningCount}',
                unit: 'lần',
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _statCell({
    required IconData icon,
    required String label,
    required String value,
    required String unit,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _outlineVariant.withValues(alpha: 0.4)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: _onSurfaceVariant, size: 14),
              const SizedBox(width: 4),
              Text(
                label,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 0.6,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                value,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 28,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
              const SizedBox(width: 4),
              Text(
                unit,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildActions(BuildContext context) {
    return Column(
      children: [
        SizedBox(
          width: double.infinity,
          height: 52,
          child: FilledButton(
            onPressed: () => Navigator.of(context).pop(),
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              shape: const StadiumBorder(),
            ),
            child: const Text(
              'Luyện tập lại',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.white,
              ),
            ),
          ),
        ),
        const SizedBox(height: 12),
        SizedBox(
          width: double.infinity,
          height: 52,
          child: OutlinedButton(
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => const ExerciseHistoryScreen(),
                ),
              );
            },
            style: OutlinedButton.styleFrom(
              side: const BorderSide(color: _primary),
              shape: const StadiumBorder(),
            ),
            child: const Text(
              'Xem lịch sử',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
        ),
        const SizedBox(height: 16),
        TextButton(
          onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
          child: const Text(
            'Quay lại danh sách',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
        ),
      ],
    );
  }
}
