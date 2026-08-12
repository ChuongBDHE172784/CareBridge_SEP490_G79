class RegistrationDraft {
  const RegistrationDraft({
    required this.name,
    required this.email,
    required this.phone,
    required this.password,
    this.role,
  });

  final String name;
  final String email;
  final String phone;
  final String password;
  final String? role;

  bool get isExpert => role?.trim().toUpperCase() == 'EXPERT';
}

/// Short-lived in-memory handoff for the multi-step registration flow.
///
/// Keeping the draft here lets route widgets transfer ownership without placing
/// the password in Navigator arguments, browser URLs, or durable storage.
class RegistrationDraftStore {
  RegistrationDraftStore._();

  static RegistrationDraft? _active;

  static RegistrationDraft? get active => _active;

  static void set(RegistrationDraft draft) {
    _active = draft;
  }

  static void clear() {
    _active = null;
  }
}
