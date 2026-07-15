class ConversationCall {
  final String callId;
  final String conversationId;
  final String initiatedByUserId;
  final String callType;
  final String callStatus;
  final int? durationSeconds;
  final String? zegoRoomId;
  final String? zegoToken;

  const ConversationCall({
    required this.callId,
    required this.conversationId,
    required this.initiatedByUserId,
    required this.callType,
    required this.callStatus,
    this.durationSeconds,
    this.zegoRoomId,
    this.zegoToken,
  });

  factory ConversationCall.fromJson(Map<String, dynamic> json) {
    return ConversationCall(
      callId: json['callId'] as String,
      conversationId: json['conversationId'] as String,
      initiatedByUserId: json['initiatedByUserId'] as String,
      callType: json['callType'] as String,
      callStatus: json['callStatus'] as String,
      durationSeconds: json['durationSeconds'] as int?,
      zegoRoomId: json['zegoRoomId'] as String?,
      zegoToken: json['zegoToken'] as String?,
    );
  }
}
