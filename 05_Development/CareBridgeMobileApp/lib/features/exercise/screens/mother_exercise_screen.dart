import 'package:flutter/material.dart';

import '../../../core/network/api_client.dart';
import '../models/exercise_model.dart';
import '../services/exercise_service.dart';
import 'exercise_history_screen.dart';
import 'exercise_session_screen.dart';
import 'enable_posture_camera_sheet.dart';
import 'pre_exercise_safety_check_screen.dart';

class MotherExerciseScreen extends StatefulWidget {
  const MotherExerciseScreen({super.key});

  @override
  State<MotherExerciseScreen> createState() => _MotherExerciseScreenState();
}

class _MotherExerciseScreenState extends State<MotherExerciseScreen> {
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceLow = Color(0xFFF8EEE9);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _secondaryContainer = Color(0xFFF1E6E0);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outline = Color(0xFFE5D3CA);

  static const _trimesterFilters = [
    _FilterOption('Tất cả', null),
    _FilterOption('Tam cá 1', 'FIRST'),
    _FilterOption('Tam cá 2', 'SECOND'),
    _FilterOption('Tam cá 3', 'THIRD'),
  ];

  static const _difficultyFilters = [
    _FilterOption('Tất cả', null),
    _FilterOption('Nhẹ', 'EASY'),
    _FilterOption('Vừa', 'MEDIUM'),
    _FilterOption('Khó', 'HARD'),
  ];

  int _trimesterIndex = 0;
  int _difficultyIndex = 0;
  List<ExerciseSummary> _exercises = [];
  bool _isLoading = true;
  String? _error;
  String? _startingExerciseId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final result = await ExerciseService.instance.getExercises(
        trimester: _trimesterFilters[_trimesterIndex].value,
        difficulty: _difficultyFilters[_difficultyIndex].value,
      );
      if (!mounted) return;
      setState(() {
        _exercises = result;
        _isLoading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Lỗi tải bài tập (${e.statusCode})';
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Không thể kết nối. Vui lòng thử lại.';
        _isLoading = false;
      });
    }
  }

  Future<void> _startExercise(ExerciseSummary exercise) async {
    final cleared = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => PreExerciseSafetyCheckScreen(
          exerciseName: exercise.title,
          trimester: _trimesterLabel(exercise.trimesterScope),
          durationMinutes: exercise.durationMinutes,
        ),
      ),
    );
    if (cleared != true || !mounted) return;

    setState(() => _startingExerciseId = exercise.id);
    try {
      final detail = await ExerciseService.instance.getExerciseDetail(
        exercise.id,
      );
      final safetyCheck = await ExerciseService.instance.submitSafetyCheck(
        exercise.id,
      );
      if (!safetyCheck.isCleared) {
        _showMessage(
          safetyCheck.blockedReason ??
              'Bài tập chưa an toàn để bắt đầu lúc này.',
        );
        return;
      }
      final session = await ExerciseService.instance.startSession(
        exercise.id,
        safetyCheck.id,
      );
      if (!mounted) return;

      var enableRealtimePostureCamera = false;
      if (detail.supportsPostureAnalysis) {
        enableRealtimePostureCamera = await EnablePostureCameraSheet.show(
          context,
        );
        if (!mounted) return;
      }

      await Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => ExerciseSessionScreen(
            exerciseId: detail.id,
            exerciseTitle: detail.title,
            instruction: detail.instructionContent.isNotEmpty
                ? detail.instructionContent
                : detail.description,
            mediaUrl: detail.mediaUrl,
            safetyWarning: detail.safetyWarning,
            durationMinutes: detail.durationMinutes,
            sessionId: session.id,
            initialStatus: session.status,
            initialStartedAt: session.startedAt,
            enableRealtimePostureCamera: enableRealtimePostureCamera,
          ),
        ),
      );
    } on ApiException catch (e) {
      _showMessage('Không thể bắt đầu bài tập (${e.statusCode}).');
    } catch (_) {
      _showMessage('Không thể bắt đầu bài tập. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _startingExerciseId = null);
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message, style: const TextStyle(fontFamily: 'Lexend')),
        behavior: SnackBarBehavior.floating,
        backgroundColor: _primary,
        shape: const StadiumBorder(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final topInset = MediaQuery.of(context).padding.top;
    final canPop = ModalRoute.of(context)?.canPop ?? false;

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            Container(
              width: double.infinity,
              decoration: const BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.vertical(bottom: Radius.circular(24)),
                boxShadow: [
                  BoxShadow(
                    color: Color(0x0A845143),
                    blurRadius: 16,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              padding: EdgeInsets.fromLTRB(16, topInset + 12, 16, 16),
              child: Row(
                children: [
                  if (canPop) ...[
                    IconButton(
                      key: const Key('mother-exercise-back-button'),
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                      icon: const Icon(
                        Icons.arrow_back_ios_new_rounded,
                        color: _primary,
                        size: 20,
                      ),
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                    const SizedBox(width: 12),
                  ] else
                    const SizedBox(width: 8),
                  Container(
                    width: 42,
                    height: 42,
                    decoration: BoxDecoration(
                      color: _surfaceLow,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.self_improvement_rounded,
                      color: _primary,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Bài tập cho mẹ',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                            color: _onSurface,
                            letterSpacing: -0.3,
                          ),
                        ),
                        SizedBox(height: 2),
                        Text(
                          'Vận động nhẹ nhàng theo thai kỳ',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Lịch sử',
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints(),
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => const ExerciseHistoryScreen(),
                      ),
                    ),
                    icon: const Icon(
                      Icons.history_rounded,
                      color: _primary,
                      size: 22,
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: RefreshIndicator(
                color: _primaryContainer,
                onRefresh: _load,
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                  children: [
                    _buildTodayCard(),
                    const SizedBox(height: 16),
                    _buildFilters(),
                    const SizedBox(height: 16),
                    if (_isLoading)
                      const Padding(
                        padding: EdgeInsets.only(top: 72),
                        child: Center(
                          child: CircularProgressIndicator(color: _primaryContainer),
                        ),
                      )
                    else if (_error != null)
                      _buildError()
                    else if (_exercises.isEmpty)
                      _buildEmpty()
                    else
                      ..._exercises.map(_buildExerciseCard),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTodayCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 24,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Nhịp tập hôm nay',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  _exercises.isEmpty
                      ? 'Chọn một bài tập phù hợp để bắt đầu an toàn.'
                      : '${_exercises.length} bài tập đang phù hợp với bộ lọc.',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                    height: 1.45,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          Container(
            width: 68,
            height: 68,
            decoration: BoxDecoration(
              color: _secondaryContainer,
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.favorite_rounded,
              color: _primary,
              size: 32,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildFilterRow(
          label: 'Thai kỳ',
          options: _trimesterFilters,
          activeIndex: _trimesterIndex,
          onChanged: (i) {
            setState(() => _trimesterIndex = i);
            _load();
          },
        ),
        const SizedBox(height: 12),
        _buildFilterRow(
          label: 'Cường độ',
          options: _difficultyFilters,
          activeIndex: _difficultyIndex,
          onChanged: (i) {
            setState(() => _difficultyIndex = i);
            _load();
          },
        ),
      ],
    );
  }

  Widget _buildFilterRow({
    required String label,
    required List<_FilterOption> options,
    required int activeIndex,
    required ValueChanged<int> onChanged,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 6),
          child: Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: _onSurfaceVariant,
            ),
          ),
        ),
        SizedBox(
          height: 38,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: options.length,
            separatorBuilder: (_, _) => const SizedBox(width: 8),
            itemBuilder: (_, index) {
              final active = index == activeIndex;
              return InkWell(
                onTap: () => onChanged(index),
                borderRadius: BorderRadius.circular(20),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    color: active ? _primary : _surface,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: active ? _primary : _outline,
                      width: 1,
                    ),
                    boxShadow: active
                        ? [
                            BoxShadow(
                              color: _primary.withValues(alpha: 0.2),
                              blurRadius: 8,
                              offset: const Offset(0, 2),
                            ),
                          ]
                        : null,
                  ),
                  child: Text(
                    options[index].label,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: active ? FontWeight.w700 : FontWeight.w500,
                      color: active ? Colors.white : _onSurface,
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildExerciseCard(ExerciseSummary exercise) {
    final isStarting = _startingExerciseId == exercise.id;
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _outline.withValues(alpha: 0.45)),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.05),
            blurRadius: 18,
            offset: Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: _primaryContainer.withValues(alpha: 0.14),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  _exerciseIcon(exercise.title),
                  color: _primary,
                  size: 24,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      exercise.title,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 17,
                        fontWeight: FontWeight.w800,
                        color: _onSurface,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      exercise.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        color: _onSurfaceVariant,
                        height: 1.45,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _MetaChip(
                icon: Icons.timer_outlined,
                label: '${exercise.durationMinutes} phút',
              ),
              _MetaChip(
                icon: Icons.spa_outlined,
                label: _trimesterLabel(exercise.trimesterScope),
              ),
              _MetaChip(
                icon: Icons.speed_rounded,
                label: _difficultyLabel(exercise.difficultyLevel),
              ),
              if (exercise.supportsPostureAnalysis)
                const _MetaChip(
                  icon: Icons.camera_alt_outlined,
                  label: 'Tư thế AI',
                ),
            ],
          ),
          if ((exercise.safetyWarning ?? '').isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: _surfaceLow,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.info_outline_rounded,
                    color: _primary,
                    size: 18,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      exercise.safetyWarning!,
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
            ),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: FilledButton.icon(
              onPressed: isStarting ? null : () => _startExercise(exercise),
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                disabledBackgroundColor: _primaryContainer.withValues(
                  alpha: 0.45,
                ),
                shape: const StadiumBorder(),
              ),
              icon: isStarting
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.play_arrow_rounded, color: Colors.white),
              label: Text(
                isStarting ? 'Đang chuẩn bị...' : 'Bắt đầu an toàn',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 56),
      child: Column(
        children: [
          Icon(Icons.self_improvement_outlined, color: _outline, size: 64),
          const SizedBox(height: 16),
          const Text(
            'Chưa có bài tập phù hợp',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 17,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Thử đổi bộ lọc hoặc quay lại sau.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 56),
      child: Column(
        children: [
          const Icon(Icons.error_outline_rounded, color: _primary, size: 48),
          const SizedBox(height: 16),
          Text(
            _error!,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _load,
            style: FilledButton.styleFrom(
              backgroundColor: _primaryContainer,
              shape: const StadiumBorder(),
            ),
            child: const Text(
              'Thử lại',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w700,
                color: Colors.white,
              ),
            ),
          ),
        ],
      ),
    );
  }

  IconData _exerciseIcon(String title) {
    final normalized = title.toLowerCase();
    if (normalized.contains('yoga')) return Icons.self_improvement_rounded;
    if (normalized.contains('đi bộ') || normalized.contains('walk')) {
      return Icons.directions_walk_rounded;
    }
    if (normalized.contains('kegel')) return Icons.fitness_center_rounded;
    return Icons.sports_gymnastics_rounded;
  }

  String _trimesterLabel(String scope) {
    switch (scope.toUpperCase()) {
      case 'TRIMESTER_1':
      case 'FIRST':
        return 'Tam cá 1';
      case 'TRIMESTER_2':
      case 'SECOND':
        return 'Tam cá 2';
      case 'TRIMESTER_3':
      case 'THIRD':
        return 'Tam cá 3';
      case 'ALL':
        return 'Mọi giai đoạn';
      default:
        return scope.isEmpty ? 'Theo thai kỳ' : scope;
    }
  }

  String _difficultyLabel(String level) {
    switch (level.toUpperCase()) {
      case 'EASY':
        return 'Nhẹ';
      case 'MEDIUM':
        return 'Vừa';
      case 'HARD':
        return 'Khó';
      default:
        return level.isEmpty ? 'Phù hợp' : level;
    }
  }
}

class _FilterOption {
  final String label;
  final String? value;
  const _FilterOption(this.label, this.value);
}

class _MetaChip extends StatelessWidget {
  final IconData icon;
  final String label;

  const _MetaChip({required this.icon, required this.label});

  static const _surfaceLow = Color(0xFFFFF1EC);
  static const _primary = Color(0xFF845143);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: const ShapeDecoration(
        color: _surfaceLow,
        shape: StadiumBorder(),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15, color: _primary),
          const SizedBox(width: 5),
          Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
