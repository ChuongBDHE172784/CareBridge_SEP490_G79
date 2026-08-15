class TriageHistoryItem {
  const TriageHistoryItem({
    required this.sessionId,
    required this.stage,
    required this.status,
    this.riskLevel,
    this.createdAt,
    this.completedAt,
    this.journeyId,
    this.originDashboard,
    this.originReferenceId,
  });

  final String sessionId;
  final String stage;
  final String status;
  final String? riskLevel;
  final DateTime? createdAt;
  final DateTime? completedAt;
  final String? journeyId;
  final String? originDashboard;
  final String? originReferenceId;

  factory TriageHistoryItem.fromJson(Map<String, dynamic> json) {
    final sessionId = json['sessionId']?.toString() ?? '';
    if (sessionId.isEmpty) {
      throw const FormatException('Invalid triage history session identity');
    }
    return TriageHistoryItem(
      sessionId: sessionId,
      stage: json['stage']?.toString() ?? 'INFANT',
      status: json['status']?.toString() ?? 'PROCESSING',
      riskLevel: json['riskLevel']?.toString(),
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
      completedAt: DateTime.tryParse(json['completedAt']?.toString() ?? ''),
      journeyId: json['journeyId']?.toString(),
      originDashboard: json['originDashboard']?.toString(),
      originReferenceId: json['originReferenceId']?.toString(),
    );
  }

  String get stageLabel => switch (stage) {
    'PRECONCEPTION' => 'Chuẩn bị mang thai',
    'PREGNANCY' => 'Đang mang thai',
    'POSTPARTUM' => 'Sau sinh',
    'TODDLER' => 'Bé 12–24 tháng',
    _ => 'Bé 0–12 tháng',
  };

  String get statusLabel => switch (status) {
    'COMPLETED' =>
      riskLevel == null ? 'Đã hoàn tất' : 'Kết quả ${_riskLabel(riskLevel)}',
    'FAILED' => 'Chưa hoàn tất',
    'NEED_MORE_INFO' || 'ASK_MORE' => 'Cần thêm thông tin',
    _ => 'Đang xử lý',
  };

  static String _riskLabel(String? risk) => switch (risk) {
    'RED' => 'Đỏ',
    'YELLOW' => 'Vàng',
    'GREEN' => 'Xanh',
    'NEEDS_MORE_INFO' => 'Cần thêm thông tin',
    'OUT_OF_SCOPE' => 'Ngoài phạm vi hỗ trợ',
    _ => 'Chưa xác định',
  };

  bool get isCompleted => status == 'COMPLETED';
}
