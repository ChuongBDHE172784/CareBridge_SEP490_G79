import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/exercise_model.dart';
import '../services/exercise_service.dart';

class ExerciseHistoryScreen extends StatefulWidget {
  const ExerciseHistoryScreen({super.key, this.service, this.authState});

  final ExerciseService? service;
  final AuthState? authState;

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
    _Filter('Tất cả', null),
    _Filter('Tam cá nguyệt 1', 'FIRST'),
    _Filter('Tam cá nguyệt 2', 'SECOND'),
    _Filter('Tam cá nguyệt 3', 'THIRD'),
  ];

  int _activeFilter = 0;
  List<ExerciseHistoryItem> _history = [];
  bool _isLoading = true;
  String? _error;
  int _loadGeneration = 0;
  late AuthState _authState;
  late int _observedSessionGeneration;
  String? _observedUserId;
  late bool _observedAuthenticated;

  ExerciseService get _service {
    final service = widget.service;
    if (service != null) return service;
    if (widget.authState != null) {
      throw StateError(
        'A custom AuthState requires an injected ExerciseService',
      );
    }
    return ExerciseService.instance;
  }

  int get _totalMinutes => _history.fold(0, (s, h) => s + h.durationMinutes);
  int get _sessionCount => _history.length;

  @override
  void initState() {
    super.initState();
    _authState = widget.authState ?? AuthState.instance;
    _captureAuthSnapshot();
    _authState.addListener(_handleAuthStateChanged);
    if (_hasAuthenticatedAccount) {
      _load();
    } else {
      _isLoading = false;
    }
  }

  @override
  void didUpdateWidget(covariant ExerciseHistoryScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    final nextAuthState = widget.authState ?? AuthState.instance;
    final authStateChanged = !identical(nextAuthState, _authState);
    final serviceChanged = !identical(oldWidget.service, widget.service);

    if (authStateChanged) {
      _authState.removeListener(_handleAuthStateChanged);
      _authState = nextAuthState;
      _captureAuthSnapshot();
      _authState.addListener(_handleAuthStateChanged);
    }
    if (authStateChanged || serviceChanged) {
      _invalidateHistoryAndReload();
    }
  }

  @override
  void dispose() {
    _loadGeneration++;
    _authState.removeListener(_handleAuthStateChanged);
    super.dispose();
  }

  bool get _hasAuthenticatedAccount =>
      _authState.isAuthenticated && _authState.userId != null;

  void _captureAuthSnapshot() {
    _observedSessionGeneration = _authState.sessionGeneration;
    _observedUserId = _authState.userId;
    _observedAuthenticated = _authState.isAuthenticated;
  }

  void _handleAuthStateChanged() {
    final sessionChanged =
        _observedSessionGeneration != _authState.sessionGeneration ||
        _observedUserId != _authState.userId ||
        _observedAuthenticated != _authState.isAuthenticated;
    if (!sessionChanged) return;

    _captureAuthSnapshot();
    _invalidateHistoryAndReload();
  }

  void _invalidateHistoryAndReload() {
    _loadGeneration++;
    if (!mounted) return;
    final shouldReload = _hasAuthenticatedAccount;
    setState(() {
      _history = [];
      _error = null;
      _isLoading = shouldReload;
    });
    if (shouldReload) {
      _load();
    }
  }

  Future<void> _load() async {
    final authState = _authState;
    final userId = authState.userId;
    final sessionGeneration = authState.sessionGeneration;
    final loadGeneration = ++_loadGeneration;
    if (!authState.isAuthenticated || userId == null) {
      if (!mounted) return;
      setState(() {
        _history = [];
        _error = null;
        _isLoading = false;
      });
      return;
    }

    final trimesterScope = _filters[_activeFilter].scope;
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final result = await _service.getHistory(trimesterScope: trimesterScope);
      if (!_canApplyLoad(
        authState: authState,
        userId: userId,
        sessionGeneration: sessionGeneration,
        loadGeneration: loadGeneration,
      )) {
        return;
      }
      setState(() {
        _history = result;
        _isLoading = false;
      });
    } on ApiException catch (e) {
      if (!_canApplyLoad(
        authState: authState,
        userId: userId,
        sessionGeneration: sessionGeneration,
        loadGeneration: loadGeneration,
      )) {
        return;
      }
      setState(() {
        _error = 'Lỗi tải lịch sử (${e.statusCode})';
        _isLoading = false;
      });
    } catch (_) {
      if (!_canApplyLoad(
        authState: authState,
        userId: userId,
        sessionGeneration: sessionGeneration,
        loadGeneration: loadGeneration,
      )) {
        return;
      }
      setState(() {
        _error = 'Không thể kết nối. Vui lòng thử lại.';
        _isLoading = false;
      });
    }
  }

  bool _canApplyLoad({
    required AuthState authState,
    required String userId,
    required int sessionGeneration,
    required int loadGeneration,
  }) {
    return mounted &&
        identical(authState, _authState) &&
        loadGeneration == _loadGeneration &&
        authState.isAuthenticated &&
        authState.matchesSession(generation: sessionGeneration, userId: userId);
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
    final chipAnimationStyle = MediaQuery.disableAnimationsOf(context)
        ? ChipAnimationStyle(
            enableAnimation: AnimationStyle.noAnimation,
            selectAnimation: AnimationStyle.noAnimation,
            avatarDrawerAnimation: AnimationStyle.noAnimation,
            deleteDrawerAnimation: AnimationStyle.noAnimation,
          )
        : null;
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      child: Row(
        children: [
          for (var i = 0; i < _filters.length; i++) ...[
            if (i > 0) const SizedBox(width: 8),
            ConstrainedBox(
              constraints: const BoxConstraints(minHeight: 44),
              child: ChoiceChip(
                key: ValueKey(
                  'exercise-history-filter-${_filters[i].scope ?? 'ALL'}',
                ),
                label: Text(_filters[i].label),
                selected: _activeFilter == i,
                onSelected: (_) {
                  if (_activeFilter == i) return;
                  setState(() => _activeFilter = i);
                  _load();
                },
                showCheckmark: false,
                selectedColor: _primary,
                backgroundColor: _secondaryContainer,
                side: BorderSide.none,
                shape: const StadiumBorder(),
                labelStyle: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: _activeFilter == i ? Colors.white : _primary,
                ),
                materialTapTargetSize: MaterialTapTargetSize.padded,
                chipAnimationStyle: chipAnimationStyle,
              ),
            ),
          ],
        ],
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
    if (t.contains('yoga') || t.contains('thiền')) {
      return Icons.self_improvement;
    }
    if (t.contains('đi bộ') || t.contains('walking')) {
      return Icons.directions_walk;
    }
    if (t.contains('kegel') || t.contains('sàn chậu')) {
      return Icons.fitness_center;
    }
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
