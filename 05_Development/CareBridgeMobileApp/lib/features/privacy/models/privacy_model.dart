class PrivacySettings {
  final String? id;
  final String? userId;
  final String profileVisibility;
  final bool locationSharingEnabled;
  final bool analyticsConsent;
  final bool dataExportOptOut;

  PrivacySettings({
    this.id,
    this.userId,
    required this.profileVisibility,
    required this.locationSharingEnabled,
    required this.analyticsConsent,
    required this.dataExportOptOut,
  });

  factory PrivacySettings.fromJson(Map<String, dynamic> json) {
    return PrivacySettings(
      id: json['id'] as String?,
      userId: json['userId'] as String?,
      profileVisibility: json['profileVisibility'] as String? ?? 'PUBLIC',
      locationSharingEnabled: json['locationSharingEnabled'] as bool? ?? false,
      analyticsConsent: json['analyticsConsent'] as bool? ?? false,
      dataExportOptOut: json['dataExportOptOut'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toUpdateJson() => {
    'profileVisibility': profileVisibility,
    'locationSharingEnabled': locationSharingEnabled,
    'analyticsConsent': analyticsConsent,
    'dataExportOptOut': dataExportOptOut,
  };

  PrivacySettings copyWith({
    String? profileVisibility,
    bool? locationSharingEnabled,
    bool? analyticsConsent,
    bool? dataExportOptOut,
  }) {
    return PrivacySettings(
      id: id,
      userId: userId,
      profileVisibility: profileVisibility ?? this.profileVisibility,
      locationSharingEnabled:
          locationSharingEnabled ?? this.locationSharingEnabled,
      analyticsConsent: analyticsConsent ?? this.analyticsConsent,
      dataExportOptOut: dataExportOptOut ?? this.dataExportOptOut,
    );
  }
}

class ConsentGrant {
  final int id;
  final String? userId;
  final String dataType;
  final String purpose;
  final String recipient;
  final String? scope;
  final DateTime? consentGivenAt;
  final DateTime? expiryAt;
  final DateTime? revokedAt;

  ConsentGrant({
    required this.id,
    this.userId,
    required this.dataType,
    required this.purpose,
    required this.recipient,
    this.scope,
    this.consentGivenAt,
    this.expiryAt,
    this.revokedAt,
  });

  factory ConsentGrant.fromJson(Map<String, dynamic> json) {
    return ConsentGrant(
      id: json['id'] as int,
      userId: json['userId'] as String?,
      dataType: json['dataType'] as String? ?? '',
      purpose: json['purpose'] as String? ?? '',
      recipient: json['recipient'] as String? ?? '',
      scope: json['scope'] as String?,
      consentGivenAt: json['consentGivenAt'] != null
          ? DateTime.parse(json['consentGivenAt'] as String)
          : null,
      expiryAt: json['expiryAt'] != null
          ? DateTime.parse(json['expiryAt'] as String)
          : null,
      revokedAt: json['revokedAt'] != null
          ? DateTime.parse(json['revokedAt'] as String)
          : null,
    );
  }

  bool get isActive =>
      revokedAt == null &&
      (expiryAt == null || expiryAt!.isAfter(DateTime.now().toUtc()));

  String get scopeLabel {
    switch (scope?.toUpperCase()) {
      case 'MEDICAL':
        return 'Truy cập Y tế';
      case 'DIARY':
        return 'Truy cập Nhật ký';
      default:
        return scope ?? 'Truy cập dữ liệu';
    }
  }
}
