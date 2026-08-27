import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

import '../models/exercise_model.dart';

class ExerciseDetailScreen extends StatefulWidget {
  const ExerciseDetailScreen({
    super.key,
    required this.exercise,
    required this.onStart,
  });

  final ExerciseDetail exercise;
  final Future<void> Function() onStart;

  @override
  State<ExerciseDetailScreen> createState() => _ExerciseDetailScreenState();
}

class _ExerciseDetailScreenState extends State<ExerciseDetailScreen> {
  VideoPlayerController? _controller;
  Future<void>? _initialization;
  bool _isStarting = false;

  static const _bg = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _primary = Color(0xFF845143);
  static const _accent = Color(0xFFC98C7B);
  static const _text = Color(0xFF2A211D);
  static const _muted = Color(0xFF655650);

  String? get _mediaSource => widget.exercise.detailMediaUrl;

  @override
  void initState() {
    super.initState();
    final source = _mediaSource;
    if (source != null && source.isNotEmpty) {
      _controller = source.startsWith('http')
          ? VideoPlayerController.networkUrl(Uri.parse(source))
          : VideoPlayerController.asset(source);
      _initialization = _controller!.initialize();
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  Future<void> _start() async {
    if (_isStarting) return;
    setState(() => _isStarting = true);
    try {
      await widget.onStart();
    } finally {
      if (mounted) setState(() => _isStarting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        backgroundColor: _surface,
        foregroundColor: _primary,
        elevation: 0,
        title: const Text(
          'Chi tiết bài tập',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
        ),
      ),
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                children: [
                  _buildMedia(),
                  const SizedBox(height: 20),
                  Text(
                    widget.exercise.title,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 26,
                      fontWeight: FontWeight.w800,
                      color: _text,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    widget.exercise.description,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      height: 1.5,
                      color: _muted,
                    ),
                  ),
                  const SizedBox(height: 14),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      _Chip(
                        icon: Icons.timer_outlined,
                        text: '${widget.exercise.durationMinutes} phút',
                      ),
                      _Chip(
                        icon: Icons.speed_rounded,
                        text: widget.exercise.difficultyLevel,
                      ),
                      _Chip(
                        icon: Icons.pregnant_woman_rounded,
                        text: widget.exercise.trimesterScope,
                      ),
                    ],
                  ),
                  if ((widget.exercise.safetyWarning ?? '')
                      .trim()
                      .isNotEmpty) ...[
                    const SizedBox(height: 18),
                    _buildSafetyWarning(),
                  ],
                  const SizedBox(height: 24),
                  const Text(
                    'Các bước thực hiện',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                      color: _text,
                    ),
                  ),
                  const SizedBox(height: 12),
                  ...widget.exercise.instructionSteps.asMap().entries.map(
                    (entry) => _Step(index: entry.key + 1, text: entry.value),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 18),
              child: SizedBox(
                width: double.infinity,
                height: 52,
                child: FilledButton.icon(
                  key: const Key('exercise-detail-start-button'),
                  onPressed: _isStarting ? null : _start,
                  style: FilledButton.styleFrom(
                    backgroundColor: _accent,
                    shape: const StadiumBorder(),
                  ),
                  icon: _isStarting
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.play_arrow_rounded),
                  label: Text(
                    _isStarting ? 'Đang chuẩn bị...' : 'Bắt đầu bài tập',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMedia() {
    final controller = _controller;
    if (controller == null || _initialization == null) {
      return _mediaPlaceholder('Media hướng dẫn sẽ được bổ sung.');
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(22),
      child: FutureBuilder<void>(
        future: _initialization,
        builder: (context, snapshot) {
          if (snapshot.hasError) {
            return _mediaPlaceholder('Không thể tải media hướng dẫn.');
          }
          if (snapshot.connectionState != ConnectionState.done) {
            return const AspectRatio(
              aspectRatio: 16 / 9,
              child: Center(child: CircularProgressIndicator(color: _accent)),
            );
          }
          return AspectRatio(
            aspectRatio: controller.value.aspectRatio == 0
                ? 16 / 9
                : controller.value.aspectRatio,
            child: Stack(
              alignment: Alignment.center,
              children: [
                VideoPlayer(controller),
                IconButton.filledTonal(
                  key: const Key('exercise-detail-media-play-button'),
                  onPressed: () => setState(
                    () => controller.value.isPlaying
                        ? controller.pause()
                        : controller.play(),
                  ),
                  icon: Icon(
                    controller.value.isPlaying
                        ? Icons.pause_rounded
                        : Icons.play_arrow_rounded,
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _mediaPlaceholder(String message) => Container(
    height: 220,
    decoration: BoxDecoration(
      color: const Color(0xFFE8DAD4),
      borderRadius: BorderRadius.circular(22),
    ),
    child: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.sports_gymnastics, size: 56, color: _primary),
          const SizedBox(height: 10),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(fontFamily: 'Lexend', color: _muted),
          ),
        ],
      ),
    ),
  );

  Widget _buildSafetyWarning() => Container(
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: const Color(0xFFFFE9E4),
      borderRadius: BorderRadius.circular(16),
    ),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Icon(Icons.info_outline_rounded, color: _primary),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            widget.exercise.safetyWarning!,
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: _muted,
              height: 1.45,
            ),
          ),
        ),
      ],
    ),
  );
}

class _Chip extends StatelessWidget {
  const _Chip({required this.icon, required this.text});
  final IconData icon;
  final String text;
  @override
  Widget build(BuildContext context) => Chip(
    avatar: Icon(icon, size: 17, color: const Color(0xFF845143)),
    label: Text(text, style: const TextStyle(fontFamily: 'Lexend')),
  );
}

class _Step extends StatelessWidget {
  const _Step({required this.index, required this.text});
  final int index;
  final String text;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        CircleAvatar(
          radius: 14,
          backgroundColor: const Color(0xFFC98C7B),
          child: Text(
            '$index',
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              height: 1.5,
              color: Color(0xFF2A211D),
            ),
          ),
        ),
      ],
    ),
  );
}
