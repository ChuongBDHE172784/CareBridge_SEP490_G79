import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../services/exercise_service.dart';
import '../models/exercise_model.dart';
import 'exercise_session_result_screen.dart';

class ExerciseSessionScreen extends StatefulWidget {
  final String exerciseId;
  final String exerciseTitle;
  final String instruction;
  final String? mediaUrl;
  final String? safetyWarning;
  final int durationMinutes;
  final String sessionId;

  const ExerciseSessionScreen({
    super.key,
    required this.exerciseId,
    required this.exerciseTitle,
    required this.instruction,
    this.mediaUrl,
    this.safetyWarning,
    required this.durationMinutes,
    required this.sessionId,
  });

  @override
  State<ExerciseSessionScreen> createState() => _ExerciseSessionScreenState();
}

class _ExerciseSessionScreenState extends State<ExerciseSessionScreen> {
  static const _canvas = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _surfaceVariant = Color(0xFFFADCD3);

  Timer? _timer;
  int _elapsedSeconds = 0;
  bool _isPaused = false;
  bool _isCompleting = false;
  String _postureStatus = 'Tư thế chuẩn';
  bool _postureGood = true;

  int get _totalSeconds => widget.durationMinutes * 60;

  String _fmt(int s) {
    final m = (s ~/ 60).toString().padLeft(2, '0');
    final sec = (s % 60).toString().padLeft(2, '0');
    return '$m:$sec';
  }

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (_isPaused) return;
      setState(() => _elapsedSeconds++);
      if (_elapsedSeconds >= _totalSeconds) {
        _timer?.cancel();
        _completeSession();
      }
    });
  }

  Future<void> _togglePause() async {
    setState(() => _isPaused = !_isPaused);
    try {
      if (_isPaused) {
        await ExerciseService.instance.pauseSession(widget.sessionId);
      } else {
        await ExerciseService.instance.resumeSession(widget.sessionId);
      }
    } on ApiException {
      if (mounted) {
        setState(() => _isPaused = !_isPaused);
      }
    }
  }

  Future<void> _stopSession() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Dừng bài tập?',
            style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600)),
        content: const Text('Bài tập sẽ được tính là chưa hoàn thành.',
            style: TextStyle(fontFamily: 'Lexend')),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Tiếp tục tập')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: _primary),
            child: const Text('Dừng lại',
                style: TextStyle(color: Colors.white, fontFamily: 'Lexend')),
          ),
        ],
      ),
    );
    if (confirm != true) return;
    _timer?.cancel();
    await _completeSession();
  }

  Future<void> _completeSession() async {
    if (_isCompleting) return;
    setState(() => _isCompleting = true);
    try {
      final result =
          await ExerciseService.instance.completeSession(widget.sessionId);
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => ExerciseSessionResultScreen(result: result),
        ),
      );
    } on ApiException {
      if (mounted) {
        setState(() => _isCompleting = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể hoàn thành bài tập. Thử lại.')),
        );
      }
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final progress = (_totalSeconds > 0)
        ? (_elapsedSeconds / _totalSeconds).clamp(0.0, 1.0)
        : 0.0;

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Column(
                  children: [
                    const SizedBox(height: 12),
                    _buildProgressCard(progress),
                    const SizedBox(height: 16),
                    _buildMediaCard(),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),
            _buildControls(),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
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
              icon: const Icon(Icons.arrow_back, color: _primary),
              padding: EdgeInsets.zero,
            ),
          ),
          const Expanded(
            child: Text(
              'Ứng dụng Mẹ và Bé',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: _stopSession,
              icon: const Icon(Icons.close, color: _onSurfaceVariant),
              padding: EdgeInsets.zero,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProgressCard(double progress) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            widget.exerciseTitle,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            widget.instruction,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          Center(
            child: Text(
              _fmt(_elapsedSeconds),
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 40,
                fontWeight: FontWeight.w700,
                color: _primaryContainer,
              ),
            ),
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 6,
              backgroundColor: const Color(0xFFEDE0DB),
              valueColor:
                  const AlwaysStoppedAnimation<Color>(_primaryContainer),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(_fmt(_elapsedSeconds),
                  style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _onSurfaceVariant)),
              Text(_fmt(_totalSeconds),
                  style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _onSurfaceVariant)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMediaCard() {
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: Stack(
        children: [
          // Background image / placeholder
          Container(
            width: double.infinity,
            height: 360,
            color: const Color(0xFFD6C2BD),
            child: widget.mediaUrl != null
                ? Image.network(
                    widget.mediaUrl!,
                    fit: BoxFit.cover,
                    errorBuilder: (_, __, ___) => const Center(
                      child: Icon(Icons.image_not_supported,
                          color: Colors.white54, size: 64),
                    ),
                  )
                : const Center(
                    child: Icon(Icons.sports_gymnastics,
                        color: Colors.white54, size: 80),
                  ),
          ),

          // Posture status badge
          Positioned(
            top: 16,
            left: 16,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.9),
                borderRadius: BorderRadius.circular(9999),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    _postureGood
                        ? Icons.check_circle_outline
                        : Icons.warning_amber_outlined,
                    color: _postureGood ? Colors.green : Colors.orange,
                    size: 16,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    _postureStatus,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: _postureGood ? _primary : Colors.orange.shade800,
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Camera flip button
          Positioned(
            top: 16,
            right: 16,
            child: Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.9),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.flip_camera_ios_outlined,
                  color: _onSurface, size: 20),
            ),
          ),

          // Safety tip overlay at bottom
          if (widget.safetyWarning != null && widget.safetyWarning!.isNotEmpty)
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [
                      Colors.black.withOpacity(0.5),
                      Colors.transparent,
                    ],
                  ),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.info_outline,
                        color: Colors.white70, size: 16),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        widget.safetyWarning!,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          color: Colors.white,
                          height: 1.4,
                        ),
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

  Widget _buildControls() {
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
      color: _canvas,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          // Stop button
          SizedBox(
            width: 52,
            height: 52,
            child: Material(
              color: _surfaceVariant,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: _stopSession,
                child: const Icon(Icons.stop_rounded,
                    color: Color(0xFFBA1A1A), size: 24),
              ),
            ),
          ),

          // Pause/Resume button (large center)
          SizedBox(
            width: 72,
            height: 72,
            child: Material(
              color: _primaryContainer,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: _isCompleting ? null : _togglePause,
                child: _isCompleting
                    ? const Padding(
                        padding: EdgeInsets.all(20),
                        child: CircularProgressIndicator(
                            strokeWidth: 2, color: Colors.white),
                      )
                    : Icon(
                        _isPaused ? Icons.play_arrow_rounded : Icons.pause_rounded,
                        color: Colors.white,
                        size: 36,
                      ),
              ),
            ),
          ),

          // Skip button
          SizedBox(
            width: 52,
            height: 52,
            child: Material(
              color: _surfaceLowest,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: _completeSession,
                child: const Icon(Icons.skip_next_rounded,
                    color: _onSurface, size: 24),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
