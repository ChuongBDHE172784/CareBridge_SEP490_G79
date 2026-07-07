import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../models/exercise_model.dart';
import '../services/exercise_service.dart';

class ExerciseHistoryScreen extends StatefulWidget {
  const ExerciseHistoryScreen({super.key});

  @override
  State<ExerciseHistoryScreen> createState() => _ExerciseHistoryScreenState();
}

class _ExerciseHistoryScreenState extends State<ExerciseHistoryScreen> {
  static const _canvas = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _secondaryContainer = Color(0xFFF6DACF);

  static const _filters = [
    _Filter('Tháng này', null),
    _Filter('Yoga', 'YOGA'),
    _Filter('Đi bộ', 'WALKING'),
    _Filter('Kegel', 'KEGEL'),
  ];

  int _activeFilter = 0;
  List<ExerciseHistoryItem> _history = [];
  bool _isLoading = true;
  String? _error;

  int get _totalMinutes => _history.fold(0, (s, h) => s + h.durationMinutes);
  int get _sessionCount => _history.length;

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
      final trimesterScope = _activeFilter == 0
          ? null
          : _filters[_activeFilter].scope;
      final result = await ExerciseService.instance.getHistory(
        trimesterScope: trimesterScope,
      );
      if (!mounted) return;
      setState(() {
        _history = result;
        _isLoading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Lỗi tải lịch sử (${e.statusCode})';
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            _buildFilters(),
            Expanded(
              child: _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
                  : _error != null
                  ? _buildError()
                  : RefreshIndicator(
                      color: _primaryContainer,
                      onRefresh: _load,
                      child: ListView(
                        padding: const EdgeInsets.fromLTRB(24, 8, 24, 80),
                        children: [
                          _buildStats(),
                          const SizedBox(height: 24),
                          if (_history.isEmpty)
                            _buildEmpty()
                          else ...[
                            const Text(
                              'Gần đây',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 20,
                                fontWeight: FontWeight.w700,
                                color: _onSurface,
                              ),
                            ),
                            const SizedBox(height: 12),
                            ..._history.map(_buildHistoryCard),
                          ],
                        ],
                      ),
                    ),
            ),
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
              'Lịch sử Bài tập',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    return SizedBox(
      height: 56,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        scrollDirection: Axis.horizontal,
        itemCount: _filters.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (_, i) {
          final active = _activeFilter == i;
          final f = _filters[i];
          return GestureDetector(
            onTap: () {
              setState(() => _activeFilter = i);
              _load();
            },
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 150),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: active ? _primary : _secondaryContainer,
                borderRadius: BorderRadius.circular(9999),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (active) ...[
                    const Icon(
                      Icons.calendar_today_outlined,
                      color: Colors.white,
                      size: 14,
                    ),
                    const SizedBox(width: 6),
                  ] else if (f.scope == 'YOGA') ...[
                    const Icon(
                      Icons.self_improvement_outlined,
                      color: _primary,
                      size: 14,
                    ),
                    const SizedBox(width: 6),
                  ] else if (f.scope == 'WALKING') ...[
                    const Icon(
                      Icons.directions_walk_outlined,
                      color: _primary,
                      size: 14,
                    ),
                    const SizedBox(width: 6),
                  ],
                  Text(
                    f.label,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: active ? Colors.white : _primary,
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildStats() {
    return Row(
      children: [
        Expanded(child: _statCard('Tổng thời gian', '$_totalMinutes phút')),
        const SizedBox(width: 12),
        Expanded(child: _statCard('Số bài tập', '$_sessionCount buổi')),
      ],
    );
  }

  Widget _statCard(String label, String value) {
    return Container(
      padding: const EdgeInsets.all(16),
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
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 22,
              fontWeight: FontWeight.w700,
              color: _primary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHistoryCard(ExerciseHistoryItem item) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.04),
            blurRadius: 16,
            offset: Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Exercise type icon circle
          Container(
            width: 48,
            height: 48,
            decoration: const BoxDecoration(
              color: Color(0xFFFFE2D9),
              shape: BoxShape.circle,
            ),
            child: Icon(
              _exerciseIcon(item.exerciseTitle),
              color: _primaryContainer,
              size: 22,
            ),
          ),
          const SizedBox(width: 16),

          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(
                        item.exerciseTitle,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                    ),
                    if (item.startedAt != null)
                      Text(
                        _formatDate(item.startedAt!),
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          color: _onSurfaceVariant,
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 4,
                  children: [
                    _tag('Tam cá nguyệt 2'),
                    _tag(
                      item.isCompleted
                          ? '${item.durationMinutes} Phút'
                          : '${item.durationMinutes}/${item.durationMinutes} Phút',
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(
                      item.isCompleted
                          ? Icons.check_circle_outline
                          : Icons.timelapse_outlined,
                      size: 14,
                      color: item.isCompleted
                          ? Colors.green.shade600
                          : _onSurfaceVariant,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      item.isCompleted ? 'Hoàn thành' : 'Chưa hoàn thành',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        color: item.isCompleted
                            ? Colors.green.shade600
                            : _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _tag(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE2D9),
        borderRadius: BorderRadius.circular(9999),
      ),
      child: Text(
        text,
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: _primary,
        ),
      ),
    );
  }

  IconData _exerciseIcon(String title) {
    final t = title.toLowerCase();
    if (t.contains('yoga') || t.contains('thiền'))
      return Icons.self_improvement;
    if (t.contains('đi bộ') || t.contains('walking'))
      return Icons.directions_walk;
    if (t.contains('kegel') || t.contains('sàn chậu'))
      return Icons.fitness_center;
    return Icons.sports_gymnastics;
  }

  String _formatDate(DateTime dt) {
    return '${dt.day} THG ${dt.month}';
  }

  Widget _buildEmpty() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 48),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.history_outlined, color: _outlineVariant, size: 64),
            const SizedBox(height: 16),
            const Text(
              'Chưa có lịch sử bài tập',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                color: _onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: _primary, size: 48),
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
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Filter {
  final String label;
  final String? scope;
  const _Filter(this.label, this.scope);
}
