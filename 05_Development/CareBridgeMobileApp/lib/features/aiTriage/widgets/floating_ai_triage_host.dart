import 'package:flutter/material.dart';

final floatingAiTriageRouteObserver = FloatingAiTriageRouteObserver();

class FloatingAiTriageRouteObserver extends NavigatorObserver
    with ChangeNotifier {
  int _popupDepth = 0;

  bool get hasPopupRoute => _popupDepth > 0;

  bool _isPopup(Route<dynamic>? route) => route is PopupRoute<dynamic>;

  void _updatePopupDepth(int delta) {
    final next = (_popupDepth + delta).clamp(0, 1 << 20);
    if (next == _popupDepth) return;
    _popupDepth = next;
    notifyListeners();
  }

  void _notifyNavigationChanged() => notifyListeners();

  @override
  void didPush(Route<dynamic> route, Route<dynamic>? previousRoute) {
    super.didPush(route, previousRoute);
    if (_isPopup(route)) {
      _updatePopupDepth(1);
    } else {
      _popupDepth = 0;
      _notifyNavigationChanged();
    }
  }

  @override
  void didPop(Route<dynamic> route, Route<dynamic>? previousRoute) {
    super.didPop(route, previousRoute);
    if (_isPopup(route)) {
      _updatePopupDepth(-1);
    } else {
      _popupDepth = 0;
      _notifyNavigationChanged();
    }
  }

  @override
  void didRemove(Route<dynamic> route, Route<dynamic>? previousRoute) {
    super.didRemove(route, previousRoute);
    if (_isPopup(route)) {
      _updatePopupDepth(-1);
    } else {
      _notifyNavigationChanged();
    }
  }

  @override
  void didReplace({Route<dynamic>? newRoute, Route<dynamic>? oldRoute}) {
    super.didReplace(newRoute: newRoute, oldRoute: oldRoute);
    if (_isPopup(oldRoute)) _updatePopupDepth(-1);
    if (_isPopup(newRoute)) _updatePopupDepth(1);
    if (!_isPopup(oldRoute) && !_isPopup(newRoute)) {
      _popupDepth = 0;
      _notifyNavigationChanged();
    }
  }
}

class FloatingAiTriageHost extends StatefulWidget {
  const FloatingAiTriageHost({
    super.key,
    required this.child,
    required this.authListenable,
    required this.navigationListenable,
    this.modalListenable,
    required this.isAuthenticated,
    required this.currentRole,
    required this.currentPath,
    this.hasModal = _neverHasModal,
    required this.onOpen,
  });

  final Widget child;
  final Listenable authListenable;
  final Listenable navigationListenable;
  final Listenable? modalListenable;
  final bool Function() isAuthenticated;
  final String? Function() currentRole;
  final String Function() currentPath;
  final bool Function() hasModal;
  final VoidCallback onOpen;

  static bool _neverHasModal() => false;

  @override
  State<FloatingAiTriageHost> createState() => _FloatingAiTriageHostState();
}

class _FloatingAiTriageHostState extends State<FloatingAiTriageHost> {
  static const _size = 62.0;
  static const _edgeMargin = 12.0;
  static const _bottomClearance = 84.0;
  static const _accent = Color(0xFFC98C7B);
  static const _deepCocoa = Color(0xFF5A463F);

  Offset? _position;

  @override
  void initState() {
    super.initState();
    _subscribe();
  }

  @override
  void didUpdateWidget(covariant FloatingAiTriageHost oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.authListenable != widget.authListenable ||
        oldWidget.navigationListenable != widget.navigationListenable ||
        oldWidget.modalListenable != widget.modalListenable) {
      oldWidget.authListenable.removeListener(_refresh);
      oldWidget.navigationListenable.removeListener(_refresh);
      oldWidget.modalListenable?.removeListener(_refresh);
      _subscribe();
    }
  }

  @override
  void dispose() {
    widget.authListenable.removeListener(_refresh);
    widget.navigationListenable.removeListener(_refresh);
    widget.modalListenable?.removeListener(_refresh);
    super.dispose();
  }

  void _subscribe() {
    widget.authListenable.addListener(_refresh);
    widget.navigationListenable.addListener(_refresh);
    widget.modalListenable?.addListener(_refresh);
  }

  void _refresh() {
    if (mounted) setState(() {});
  }

  bool get _shouldShow {
    if (!widget.isAuthenticated()) return false;
    if (widget.hasModal()) return false;
    final role = (widget.currentRole() ?? '').trim().toUpperCase();
    if (role != 'MOTHER' && role != 'FAMILY') return false;
    return !_isExcludedPath(widget.currentPath());
  }

  bool _isExcludedPath(String path) {
    const exactPaths = {
      '/welcome',
      '/login',
      '/blocked',
      '/role-selection',
      '/auth-landing',
      '/journey-onboarding',
      '/journey-setup',
      '/mother-stage-selection',
      '/postpartum-recovery-setup',
      '/postpartum-safety-help',
    };
    return exactPaths.contains(path) ||
        path.startsWith('/triage') ||
        path.startsWith('/emergency') ||
        path.startsWith('/family-alert') ||
        path.startsWith('/safety');
  }

  Offset _clampPosition(
    Offset value,
    BoxConstraints constraints,
    EdgeInsets safePadding,
    double keyboardInset,
  ) {
    final minX = safePadding.left + _edgeMargin;
    final maxX =
        (constraints.maxWidth - safePadding.right - _size - _edgeMargin).clamp(
          minX,
          double.infinity,
        );
    final minY = safePadding.top + _edgeMargin;
    final maxY =
        (constraints.maxHeight -
                safePadding.bottom -
                keyboardInset -
                _size -
                _bottomClearance)
            .clamp(minY, double.infinity);
    return Offset(value.dx.clamp(minX, maxX), value.dy.clamp(minY, maxY));
  }

  Offset _defaultPosition(
    BoxConstraints constraints,
    EdgeInsets safePadding,
    double keyboardInset,
  ) => _clampPosition(
    Offset(constraints.maxWidth - _size - 20, constraints.maxHeight * 0.62),
    constraints,
    safePadding,
    keyboardInset,
  );

  void _move(
    DragUpdateDetails details,
    BoxConstraints constraints,
    EdgeInsets safePadding,
    double keyboardInset,
  ) {
    final current =
        _position ?? _defaultPosition(constraints, safePadding, keyboardInset);
    setState(() {
      _position = _clampPosition(
        current + details.delta,
        constraints,
        safePadding,
        keyboardInset,
      );
    });
  }

  void _snapToNearestEdge(
    BoxConstraints constraints,
    EdgeInsets safePadding,
    double keyboardInset,
  ) {
    final current = _clampPosition(
      _position ?? _defaultPosition(constraints, safePadding, keyboardInset),
      constraints,
      safePadding,
      keyboardInset,
    );
    final minX = safePadding.left + _edgeMargin;
    final maxX = constraints.maxWidth - safePadding.right - _size - _edgeMargin;
    final midpoint = (minX + maxX) / 2;
    setState(() {
      _position = _clampPosition(
        Offset(current.dx <= midpoint ? minX : maxX, current.dy),
        constraints,
        safePadding,
        keyboardInset,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        widget.child,
        if (_shouldShow)
          LayoutBuilder(
            builder: (context, constraints) {
              final safePadding = MediaQuery.paddingOf(context);
              final keyboardInset = MediaQuery.viewInsetsOf(context).bottom;
              final position = _clampPosition(
                _position ??
                    _defaultPosition(constraints, safePadding, keyboardInset),
                constraints,
                safePadding,
                keyboardInset,
              );
              return Stack(
                children: [
                  Positioned(
                    left: position.dx,
                    top: position.dy,
                    child: Semantics(
                      button: true,
                      label: 'Mở trợ lý AI Triage',
                      child: GestureDetector(
                        key: const Key('floating-ai-triage-robot'),
                        behavior: HitTestBehavior.opaque,
                        onTap: widget.onOpen,
                        onPanUpdate: (details) => _move(
                          details,
                          constraints,
                          safePadding,
                          keyboardInset,
                        ),
                        onPanEnd: (_) => _snapToNearestEdge(
                          constraints,
                          safePadding,
                          keyboardInset,
                        ),
                        child: Container(
                            width: _size,
                            height: _size,
                            decoration: BoxDecoration(
                              color: _accent,
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: Colors.white.withValues(alpha: 0.9),
                                width: 3,
                              ),
                              boxShadow: const [
                                BoxShadow(
                                  color: Color(0x4DC98C7B),
                                  blurRadius: 24,
                                  offset: Offset(0, 8),
                                ),
                                BoxShadow(
                                  color: Color(0x1A5A463F),
                                  blurRadius: 8,
                                  offset: Offset(0, 2),
                                ),
                              ],
                            ),
                            child: const Stack(
                              clipBehavior: Clip.none,
                              children: [
                                Center(
                                  child: Icon(
                                    Icons.smart_toy_rounded,
                                    color: Colors.white,
                                    size: 31,
                                  ),
                                ),
                                Positioned(
                                  right: -2,
                                  bottom: -2,
                                  child: DecoratedBox(
                                    decoration: BoxDecoration(
                                      color: Colors.white,
                                      shape: BoxShape.circle,
                                    ),
                                    child: Padding(
                                      padding: EdgeInsets.all(5),
                                      child: Text(
                                        'AI',
                                        style: TextStyle(
                                          color: _deepCocoa,
                                          fontSize: 9,
                                          fontWeight: FontWeight.w800,
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                        ),
                      ),
                    ),
                  ),
                ],
              );
            },
          ),
      ],
    );
  }
}
