/// Typed read models for the baby-scoped care composite endpoints.
///
/// These models intentionally mirror the backend DTO field names.  A malformed
/// response is treated as a load error by the service instead of being rendered
/// as fabricated counts or placeholder events.
class BabyCareOverview {
  final String babyId;
  final String nickname;
  final int journalCount;
  final int growthMeasurementCount;
  final int milestoneCount;
  final int vaccinationRecordCount;
  final String? notice;

  const BabyCareOverview({
    required this.babyId,
    required this.nickname,
    required this.journalCount,
    required this.growthMeasurementCount,
    required this.milestoneCount,
    required this.vaccinationRecordCount,
    this.notice,
  });

  factory BabyCareOverview.fromJson(Map<String, dynamic> json) {
    return BabyCareOverview(
      babyId: _requiredString(json, 'babyId'),
      nickname: _requiredString(json, 'nickname'),
      journalCount: _count(json['journalCount']),
      growthMeasurementCount: _count(json['growthMeasurementCount']),
      milestoneCount: _count(json['milestoneCount']),
      vaccinationRecordCount: _count(json['vaccinationRecordCount']),
      notice: _optionalString(json['notice']),
    );
  }
}

class BabyCareTimelineEvent {
  final String sourceType;
  final String sourceId;
  final DateTime occurredAt;
  final String displayLabel;

  const BabyCareTimelineEvent({
    required this.sourceType,
    required this.sourceId,
    required this.occurredAt,
    required this.displayLabel,
  });

  factory BabyCareTimelineEvent.fromJson(Map<String, dynamic> json) {
    final occurredAt = DateTime.tryParse(_requiredString(json, 'occurredAt'));
    if (occurredAt == null) {
      throw const FormatException('Invalid baby care timeline occurredAt');
    }
    return BabyCareTimelineEvent(
      sourceType: _requiredString(json, 'sourceType'),
      sourceId: _requiredString(json, 'sourceId'),
      occurredAt: occurredAt.toLocal(),
      displayLabel: _requiredString(json, 'displayLabel'),
    );
  }
}

class BabyCareTimeline {
  final String babyId;
  final List<BabyCareTimelineEvent> events;
  final String? nextCursor;

  const BabyCareTimeline({
    required this.babyId,
    required this.events,
    this.nextCursor,
  });

  factory BabyCareTimeline.fromJson(Map<String, dynamic> json) {
    final rawEvents = json['events'];
    if (rawEvents is! List) {
      throw const FormatException('Invalid baby care timeline events');
    }
    return BabyCareTimeline(
      babyId: _requiredString(json, 'babyId'),
      events: rawEvents
          .map((item) {
            if (item is! Map) {
              throw const FormatException('Invalid baby care timeline event');
            }
            return BabyCareTimelineEvent.fromJson(
              Map<String, dynamic>.from(item),
            );
          })
          .toList(growable: false),
      nextCursor: _optionalString(json['nextCursor']),
    );
  }
}

class AppointmentPreparationSummary {
  final String babyId;
  final List<String> facts;
  final List<String> dueItems;
  final String? notice;

  const AppointmentPreparationSummary({
    required this.babyId,
    required this.facts,
    required this.dueItems,
    this.notice,
  });

  factory AppointmentPreparationSummary.fromJson(Map<String, dynamic> json) {
    return AppointmentPreparationSummary(
      babyId: _requiredString(json, 'babyId'),
      facts: _stringList(json['facts'], 'facts'),
      dueItems: _stringList(json['dueItems'], 'dueItems'),
      notice: _optionalString(json['notice']),
    );
  }
}

String _requiredString(Map<String, dynamic> json, String key) {
  final raw = json[key];
  final value = raw is String ? raw.trim() : null;
  if (value == null || value.isEmpty) {
    throw FormatException('Missing baby care field: $key');
  }
  return value;
}

String? _optionalString(dynamic value) {
  final text = value is String ? value.trim() : null;
  return text == null || text.isEmpty ? null : text;
}

int _count(dynamic value) {
  if (value is! num || !value.isFinite || value < 0) {
    throw const FormatException('Invalid baby care count');
  }
  final count = value.toInt();
  if (value != count) {
    throw const FormatException('Invalid baby care count');
  }
  return count;
}

List<String> _stringList(dynamic value, String field) {
  if (value is! List) {
    throw FormatException('Invalid baby care field: $field');
  }
  final values = <String>[];
  for (final item in value) {
    final text = item is String ? item.trim() : null;
    if (text == null || text.isEmpty) {
      throw FormatException('Invalid baby care item in $field');
    }
    values.add(text);
  }
  return List.unmodifiable(values);
}
