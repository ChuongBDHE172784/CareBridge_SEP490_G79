import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/timeline_page.dart';

void main() {
  group('DirectConversation.fromJson', () {
    test('parses expertAvailable=true (ADR-DCC-007 §4 UI signal)', () {
      final conversation = DirectConversation.fromJson({
        'conversationId': 'c1',
        'motherUserId': 'm1',
        'expertUserId': 'e1',
        'status': 'ACTIVE',
        'createdAt': '2026-07-15T08:00:00Z',
        'lastActivityAt': null,
        'expertAvailable': true,
      });

      expect(conversation.expertAvailable, isTrue);
      expect(conversation.lastActivityAt, isNull);
    });

    test('defaults expertAvailable to false when the field is missing (fail closed)', () {
      final conversation = DirectConversation.fromJson({
        'conversationId': 'c1',
        'motherUserId': 'm1',
        'expertUserId': 'e1',
        'status': 'ACTIVE',
        'createdAt': '2026-07-15T08:00:00Z',
      });

      expect(conversation.expertAvailable, isFalse);
    });
  });

  group('DirectConversationSummary.fromJson', () {
    test('parses counterpart role and id', () {
      final summary = DirectConversationSummary.fromJson({
        'conversationId': 'c1',
        'counterpartUserId': 'e1',
        'counterpartRole': 'EXPERT',
        'lastActivityAt': '2026-07-15T08:00:00Z',
        'expertAvailable': true,
      });

      expect(summary.counterpartRole, 'EXPERT');
      expect(summary.counterpartUserId, 'e1');
    });
  });

  group('TimelinePage.fromJson', () {
    test('parses cursors and hasMore flags — reopen conversation (no cursor) shape', () {
      final page = TimelinePage.fromJson({
        'items': [
          {
            'kind': 'MESSAGE',
            'messageId': 'm1',
            'senderUserId': 'u1',
            'messageType': 'TEXT',
            'messageBody': 'hi',
            'createdAt': '2026-07-15T08:00:00Z',
          }
        ],
        'nextCursor': 'abc',
        'hasMoreNewer': false,
        'previousCursor': 'def',
        'hasMoreOlder': true,
      });

      expect(page.items, hasLength(1));
      expect(page.hasMoreOlder, isTrue);
      expect(page.hasMoreNewer, isFalse);
      expect(page.previousCursor, 'def');
    });

    test('parses an empty page safely', () {
      final page = TimelinePage.fromJson({
        'items': [],
        'nextCursor': null,
        'hasMoreNewer': false,
        'previousCursor': null,
        'hasMoreOlder': false,
      });

      expect(page.items, isEmpty);
    });
  });
}
