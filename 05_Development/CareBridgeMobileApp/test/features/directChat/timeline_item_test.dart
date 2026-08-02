import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/models/timeline_item.dart';

void main() {
  group('TimelineItem.fromJson', () {
    test('parses a MESSAGE item', () {
      final item = TimelineItem.fromJson({
        'kind': 'MESSAGE',
        'messageId': 'm1',
        'clientMessageId': 'c1',
        'senderUserId': 'u1',
        'messageType': 'TEXT',
        'messageBody': 'Chào bác sĩ',
        'createdAt': '2026-07-15T08:00:00Z',
      });

      expect(item.kind, 'MESSAGE');
      expect(item.messageId, 'm1');
      expect(item.messageBody, 'Chào bác sĩ');
      expect(item.sendStatus, ChatSendStatus.sent);
    });

    test('parses a CALL_EVENT item', () {
      final item = TimelineItem.fromJson({
        'kind': 'CALL_EVENT',
        'callId': 'call1',
        'callType': 'VOICE',
        'callStatus': 'ENDED',
        'initiatedByUserId': 'u1',
        'durationSeconds': 184,
        'initiatedAt': '2026-07-15T08:00:00Z',
        'answeredAt': '2026-07-15T08:00:05Z',
        'endedAt': '2026-07-15T08:03:09Z',
      });

      expect(item.kind, 'CALL_EVENT');
      expect(item.callId, 'call1');
      expect(item.durationSeconds, 184);
    });

    test('parses attachment and recall metadata for a message', () {
      final item = TimelineItem.fromJson({
        'kind': 'MESSAGE',
        'messageId': 'm-image',
        'senderUserId': 'u1',
        'messageType': 'IMAGE',
        'attachmentId': 'file-1',
        'recalledAt': '2026-08-02T08:00:00Z',
        'createdAt': '2026-08-02T07:00:00Z',
      });
      expect(item.attachmentId, 'file-1');
      expect(item.recalledAt, isNotNull);
    });
  });

  group('TimelineItem.optimisticMessage', () {
    test('starts in sending state with the given clientMessageId', () {
      final item = TimelineItem.optimisticMessage(
        clientMessageId: 'c1',
        senderUserId: 'u1',
        messageBody: 'hello',
      );

      expect(item.sendStatus, ChatSendStatus.sending);
      expect(item.clientMessageId, 'c1');
      expect(item.messageId, isNull);
    });
  });

  group('mergeTimelineItems', () {
    test(
      'reconciles an optimistic entry with the server-confirmed message, no duplicate',
      () {
        final optimistic = TimelineItem.optimisticMessage(
          clientMessageId: 'c1',
          senderUserId: 'u1',
          messageBody: 'hello',
        );
        final confirmed = TimelineItem.fromJson({
          'kind': 'MESSAGE',
          'messageId': 'm1',
          'clientMessageId': 'c1',
          'senderUserId': 'u1',
          'messageType': 'TEXT',
          'messageBody': 'hello',
          'createdAt': '2026-07-15T08:00:00Z',
        });

        final merged = mergeTimelineItems([optimistic], [confirmed]);

        expect(merged, hasLength(1));
        expect(merged.single.messageId, 'm1');
        expect(merged.single.sendStatus, ChatSendStatus.sent);
      },
    );

    test(
      'does not duplicate the same server message fetched twice (idempotent merge)',
      () {
        final message = TimelineItem.fromJson({
          'kind': 'MESSAGE',
          'messageId': 'm1',
          'senderUserId': 'u1',
          'messageType': 'TEXT',
          'messageBody': 'hi',
          'createdAt': '2026-07-15T08:00:00Z',
        });

        final firstMerge = mergeTimelineItems(const [], [message]);
        final secondMerge = mergeTimelineItems(firstMerge, [message]);

        expect(secondMerge, hasLength(1));
      },
    );

    test(
      'keeps a failed optimistic entry distinct from unrelated confirmed messages',
      () {
        final failed = TimelineItem.optimisticMessage(
          clientMessageId: 'c1',
          senderUserId: 'u1',
          messageBody: 'will fail',
        ).copyWith(sendStatus: ChatSendStatus.failed);
        final otherConfirmed = TimelineItem.fromJson({
          'kind': 'MESSAGE',
          'messageId': 'm2',
          'clientMessageId': 'c2',
          'senderUserId': 'u2',
          'messageType': 'TEXT',
          'messageBody': 'unrelated',
          'createdAt': '2026-07-15T08:00:01Z',
        });

        final merged = mergeTimelineItems([failed], [otherConfirmed]);

        expect(merged, hasLength(2));
        expect(
          merged.any((i) => i.sendStatus == ChatSendStatus.failed),
          isTrue,
        );
      },
    );

    test(
      'sorts merged items by sortTimestamp regardless of MESSAGE/CALL_EVENT kind',
      () {
        final call = TimelineItem.fromJson({
          'kind': 'CALL_EVENT',
          'callId': 'call1',
          'callType': 'VOICE',
          'callStatus': 'ENDED',
          'initiatedByUserId': 'u1',
          'durationSeconds': 10,
          'initiatedAt': '2026-07-15T08:00:00Z',
        });
        final laterMessage = TimelineItem.fromJson({
          'kind': 'MESSAGE',
          'messageId': 'm1',
          'senderUserId': 'u1',
          'messageType': 'TEXT',
          'messageBody': 'after the call',
          'createdAt': '2026-07-15T08:05:00Z',
        });
        final earlierMessage = TimelineItem.fromJson({
          'kind': 'MESSAGE',
          'messageId': 'm0',
          'senderUserId': 'u1',
          'messageType': 'TEXT',
          'messageBody': 'before the call',
          'createdAt': '2026-07-15T07:59:00Z',
        });

        final merged = mergeTimelineItems(const [], [
          laterMessage,
          call,
          earlierMessage,
        ]);

        expect(merged.map((i) => i.dedupKey).toList(), [
          earlierMessage.dedupKey,
          call.dedupKey,
          laterMessage.dedupKey,
        ]);
      },
    );
  });
}
