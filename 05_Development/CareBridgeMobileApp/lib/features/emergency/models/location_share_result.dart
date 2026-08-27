class LocationShareResult {
  final String shareId;
  final int recipientCount;
  final int pushDeliveredCount;
  final DateTime sharedAt;

  const LocationShareResult({
    required this.shareId,
    required this.recipientCount,
    required this.pushDeliveredCount,
    required this.sharedAt,
  });

  factory LocationShareResult.fromJson(Map<String, dynamic> json) {
    return LocationShareResult(
      shareId: json['shareId'] as String,
      recipientCount: json['recipientCount'] as int? ?? 0,
      pushDeliveredCount: json['pushDeliveredCount'] as int? ?? 0,
      sharedAt: DateTime.parse(json['sharedAt'] as String),
    );
  }
}
