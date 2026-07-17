class ZegoJoinCredentials {
  final int appId;
  final String roomId;
  final String userId;
  final String displayName;
  final String token;
  final DateTime expiresAt;

  const ZegoJoinCredentials({
    required this.appId,
    required this.roomId,
    required this.userId,
    required this.displayName,
    required this.token,
    required this.expiresAt,
  });

  factory ZegoJoinCredentials.fromJson(Map<String, dynamic> json) {
    return ZegoJoinCredentials(
      appId: (json['appId'] as num).toInt(),
      roomId: json['roomId'] as String,
      userId: json['userId'] as String,
      displayName: json['displayName'] as String,
      token: json['token'] as String,
      expiresAt: DateTime.parse(json['expiresAt'] as String).toUtc(),
    );
  }
}
