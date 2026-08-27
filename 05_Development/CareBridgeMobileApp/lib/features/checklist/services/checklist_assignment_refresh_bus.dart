import 'dart:async';

/// Process-local invalidation signal emitted after a checklist template is
/// assigned. Retained Home tabs reconcile their Today snapshot through REST.
class ChecklistAssignmentRefreshBus {
  ChecklistAssignmentRefreshBus._();

  static final StreamController<void> _controller =
      StreamController<void>.broadcast(sync: true);

  static Stream<void> get events => _controller.stream;

  static void notify() => _controller.add(null);
}
