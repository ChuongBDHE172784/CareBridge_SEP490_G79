class BlockedAccountState {
  final String code;
  final String? lockType;
  final String? reason;
  final DateTime? retryAt;
  final bool appealAllowed;
  final String? appealToken;
  final bool appealPending;
  final String? appealStatus;

  const BlockedAccountState({
    required this.code,
    this.lockType,
    this.reason,
    this.retryAt,
    this.appealAllowed = false,
    this.appealToken,
    this.appealPending = false,
    this.appealStatus,
  });

  bool get canAppeal =>
      code == 'ACCOUNT_ADMIN_LOCKED' &&
      appealAllowed &&
      !appealPending &&
      appealStatus == null &&
      appealToken != null &&
      appealToken!.isNotEmpty;
}
