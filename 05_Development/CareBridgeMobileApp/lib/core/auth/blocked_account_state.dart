/// Why the current account cannot be used.
///
/// The in-app lock-appeal workflow was retired: the backend no longer issues
/// appeal tokens or reports appeal status, and a locked user is directed to
/// customer support instead.
class BlockedAccountState {
  final String code;
  final String? lockType;
  final String? reason;
  final DateTime? retryAt;

  const BlockedAccountState({
    required this.code,
    this.lockType,
    this.reason,
    this.retryAt,
  });

  /// A temporary lock clears by itself once [retryAt] passes; every other
  /// blocked state needs customer support to intervene.
  bool get needsSupportContact => code != 'ACCOUNT_TEMPORARILY_LOCKED';
}
