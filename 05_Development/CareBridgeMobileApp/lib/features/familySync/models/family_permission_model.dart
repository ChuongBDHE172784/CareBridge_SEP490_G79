class FamilyPermission {
  final String memberId;
  final String careGroupId;
  final bool calendar;
  final bool logs;
  final bool alerts;
  final bool records;
  final DateTime updatedAt;

  FamilyPermission({
    required this.memberId,
    required this.careGroupId,
    required this.calendar,
    required this.logs,
    required this.alerts,
    required this.records,
    required this.updatedAt,
  });

  factory FamilyPermission.fromJson(Map<String, dynamic> json) {
    return FamilyPermission(
      memberId: json['memberId'] ?? '',
      careGroupId: json['careGroupId'] ?? '',
      calendar: json['calendar'] ?? false,
      logs: json['logs'] ?? false,
      alerts: json['alerts'] ?? false,
      records: json['records'] ?? false,
      updatedAt: json['updatedAt'] != null ? DateTime.parse(json['updatedAt']) : DateTime.now(),
    );
  }
}
