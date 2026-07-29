class TriageConsentStatus {
  const TriageConsentStatus({
    required this.status,
    required this.currentVersion,
    required this.disclaimerText,
    this.reason,
    this.acceptedVersion,
  });

  final String status;
  final String? reason;
  final String currentVersion;
  final String? acceptedVersion;
  final String disclaimerText;

  bool get isAccepted =>
      status == 'ACCEPTED' && acceptedVersion == currentVersion;

  factory TriageConsentStatus.fromJson(Map<String, dynamic> json) {
    final rawStatus = json['status'];
    final rawCurrentVersion = json['currentVersion'];
    final rawDisclaimerText = json['disclaimerText'];
    final rawAcceptedVersion = json['acceptedVersion'];
    if (rawStatus is! String ||
        rawCurrentVersion is! String ||
        rawDisclaimerText is! String ||
        (rawAcceptedVersion != null && rawAcceptedVersion is! String)) {
      throw const FormatException('Invalid triage consent response');
    }
    final status = rawStatus.trim();
    final currentVersion = rawCurrentVersion.trim();
    final disclaimerText = rawDisclaimerText.trim();
    final acceptedVersion = (rawAcceptedVersion as String?)?.trim();
    if (!const {'ACCEPTED', 'REQUIRED'}.contains(status) ||
        currentVersion.isEmpty ||
        disclaimerText.isEmpty ||
        (status == 'ACCEPTED' && acceptedVersion != currentVersion)) {
      throw const FormatException('Invalid triage consent response');
    }
    return TriageConsentStatus(
      status: status,
      reason: json['reason']?.toString(),
      currentVersion: currentVersion,
      acceptedVersion: acceptedVersion,
      disclaimerText: disclaimerText,
    );
  }
}
