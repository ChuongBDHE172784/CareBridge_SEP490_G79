import 'timeline_item.dart';

class TimelinePage {
  final List<TimelineItem> items;
  final String? nextCursor;
  final bool hasMoreNewer;
  final String? previousCursor;
  final bool hasMoreOlder;

  const TimelinePage({
    required this.items,
    this.nextCursor,
    required this.hasMoreNewer,
    this.previousCursor,
    required this.hasMoreOlder,
  });

  factory TimelinePage.fromJson(Map<String, dynamic> json) {
    return TimelinePage(
      items: (json['items'] as List? ?? [])
          .cast<Map<String, dynamic>>()
          .map(TimelineItem.fromJson)
          .toList(),
      nextCursor: json['nextCursor'] as String?,
      hasMoreNewer: json['hasMoreNewer'] as bool? ?? false,
      previousCursor: json['previousCursor'] as String?,
      hasMoreOlder: json['hasMoreOlder'] as bool? ?? false,
    );
  }
}
