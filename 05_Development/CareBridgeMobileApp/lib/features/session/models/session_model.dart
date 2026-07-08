class SessionInfo {
  final String sessionId;
  final String deviceName;
  final String? browser;
  final String? ipAddress;
  final String? location;
  final DateTime? lastActivityAt;
  final bool isCurrent;
  final String status;

  SessionInfo({
    required this.sessionId,
    required this.deviceName,
    this.browser,
    this.ipAddress,
    this.location,
    this.lastActivityAt,
    required this.isCurrent,
    required this.status,
  });

  factory SessionInfo.fromJson(Map<String, dynamic> json) {
    return SessionInfo(
      sessionId: json['sessionId'] as String,
      deviceName: json['deviceName'] as String? ?? 'Unknown Device',
      browser: json['browser'] as String?,
      ipAddress: json['ipAddress'] as String?,
      location: json['location'] as String?,
      lastActivityAt: json['lastActivityAt'] != null
          ? DateTime.parse(json['lastActivityAt'] as String)
          : null,
      isCurrent: json['isCurrent'] as bool? ?? false,
      status: json['status'] as String? ?? 'active',
    );
  }

  String get displayName {
    if (browser != null && browser!.isNotEmpty) {
      return '$deviceName - $browser';
    }
    return deviceName;
  }
}
