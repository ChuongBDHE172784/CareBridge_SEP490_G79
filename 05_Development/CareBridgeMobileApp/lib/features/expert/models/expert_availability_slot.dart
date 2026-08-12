class ExpertAvailabilitySlot {
  final String availabilityId;
  final String expertProfileId;
  final DateTime startAt;
  final DateTime endAt;
  final String channelType;
  final String status;

  const ExpertAvailabilitySlot({
    required this.availabilityId,
    required this.expertProfileId,
    required this.startAt,
    required this.endAt,
    required this.channelType,
    required this.status,
  });

  bool get isAvailable =>
      status.toUpperCase() == 'AVAILABLE' && endAt.isAfter(DateTime.now());

  factory ExpertAvailabilitySlot.fromJson(Map<String, dynamic> json) {
    return ExpertAvailabilitySlot(
      availabilityId: '${json['availabilityId'] ?? json['id'] ?? ''}',
      expertProfileId: '${json['expertProfileId'] ?? ''}',
      startAt: DateTime.parse('${json['startAt']}').toLocal(),
      endAt: DateTime.parse('${json['endAt']}').toLocal(),
      channelType: '${json['channelType'] ?? 'VIDEO'}',
      status: '${json['status'] ?? 'AVAILABLE'}',
    );
  }
}

Map<DateTime, List<ExpertAvailabilitySlot>> groupAvailabilityByLocalDate(
  Iterable<ExpertAvailabilitySlot> slots,
) {
  final grouped = <DateTime, List<ExpertAvailabilitySlot>>{};
  final sorted = slots.where((slot) => slot.isAvailable).toList()
    ..sort((a, b) => a.startAt.compareTo(b.startAt));
  for (final slot in sorted) {
    final date = DateTime(
      slot.startAt.year,
      slot.startAt.month,
      slot.startAt.day,
    );
    grouped.putIfAbsent(date, () => []).add(slot);
  }
  return grouped;
}
