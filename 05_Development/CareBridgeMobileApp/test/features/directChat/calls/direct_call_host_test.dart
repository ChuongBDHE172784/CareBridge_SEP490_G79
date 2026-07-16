import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/calls/direct_call_api.dart';
import 'package:untitled/features/directChat/calls/direct_call_coordinator.dart';
import 'package:untitled/features/directChat/calls/direct_call_host.dart';
import 'package:untitled/features/directChat/models/conversation_call.dart';
import 'package:untitled/features/directChat/models/zego_join_credentials.dart';
import 'package:untitled/integrations/firebaseRealtime/conversation_event_signal.dart';

class _HostCallApi implements DirectCallApiPort {
  _HostCallApi(this.activeCall);

  final ConversationCall activeCall;
  final credentialsCompleter = Completer<ZegoJoinCredentials>();

  @override
  Future<List<ConversationCall>> listActiveCalls() async => [activeCall];

  @override
  Future<ConversationCall> getCall(
    String conversationId,
    String callId,
  ) async => activeCall;

  @override
  Future<ConversationCall> markRinging(
    String conversationId,
    String callId,
  ) async => _call('RINGING');

  @override
  Future<ZegoJoinCredentials> issueJoinCredentials(
    String conversationId,
    String callId,
  ) => credentialsCompleter.future;

  @override
  Future<ConversationCall> initiate(String conversationId, String callType) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> answer(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> decline(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> end(String conversationId, String callId) =>
      throw UnimplementedError();
}

ConversationCall _call(String status) => ConversationCall(
  callId: 'call-1',
  conversationId: 'conversation-1',
  initiatedByUserId: 'mother-1',
  callType: 'VOICE',
  callStatus: status,
  initiatedAt: DateTime.utc(2026, 7, 16),
  answeredAt: status == 'ANSWERED' ? DateTime.utc(2026, 7, 16, 0, 1) : null,
);

ConversationEventSignal _signal(String eventId) => ConversationEventSignal(
  eventId: eventId,
  eventType: 'CALL_STATE_CHANGED',
  conversationId: 'conversation-1',
  resourceId: 'call-1',
  occurredAt: DateTime.utc(2026, 7, 16),
);

void main() {
  testWidgets('duplicate signals never create duplicate incoming overlays', (
    tester,
  ) async {
    final api = _HostCallApi(_call('RINGING'));
    final signals = StreamController<ConversationEventSignal>.broadcast();
    final coordinator = DirectCallCoordinator(
      api: api,
      signals: signals.stream,
      currentUserId: () => 'expert-1',
    );
    await coordinator.start();

    await tester.pumpWidget(
      MaterialApp(
        home: DirectCallHost(
          coordinator: coordinator,
          manageAuthenticatedSession: false,
          child: const Scaffold(body: Text('Chat')),
        ),
      ),
    );
    signals
      ..add(_signal('event-1'))
      ..add(_signal('event-1'));
    await tester.pump();
    await tester.pump();

    expect(find.text('Cuộc gọi đến'), findsOneWidget);

    await tester.pumpWidget(const SizedBox.shrink());
    unawaited(coordinator.dispose());
    unawaited(signals.close());
  });

  testWidgets('credential completion after host dispose is ignored', (
    tester,
  ) async {
    final api = _HostCallApi(_call('ANSWERED'));
    final signals = StreamController<ConversationEventSignal>.broadcast();
    final coordinator = DirectCallCoordinator(
      api: api,
      signals: signals.stream,
      currentUserId: () => 'mother-1',
    );
    await coordinator.start();

    await tester.pumpWidget(
      MaterialApp(
        home: DirectCallHost(
          coordinator: coordinator,
          manageAuthenticatedSession: false,
          child: const Scaffold(body: Text('Chat')),
        ),
      ),
    );
    await tester.tap(find.text('Tham gia cuộc gọi'));
    await tester.pumpWidget(const SizedBox.shrink());
    api.credentialsCompleter.complete(
      ZegoJoinCredentials(
        appId: 1,
        roomId: 'cb_room',
        userId: 'u_mother',
        displayName: 'Mother',
        token: 'token',
        expiresAt: DateTime.now().toUtc().add(const Duration(hours: 1)),
      ),
    );
    await tester.pump();

    expect(tester.takeException(), isNull);

    unawaited(coordinator.dispose());
    unawaited(signals.close());
  });

  testWidgets(
    'rejected join credentials show setup error without permission guidance',
    (tester) async {
      final api = _HostCallApi(_call('ANSWERED'));
      final signals = StreamController<ConversationEventSignal>.broadcast();
      final coordinator = DirectCallCoordinator(
        api: api,
        signals: signals.stream,
        currentUserId: () => 'mother-1',
      );
      await coordinator.start();

      await tester.pumpWidget(
        MaterialApp(
          home: DirectCallHost(
            coordinator: coordinator,
            manageAuthenticatedSession: false,
            child: const Scaffold(body: Text('Chat')),
          ),
        ),
      );
      await tester.tap(find.text('Tham gia cuộc gọi'));
      api.credentialsCompleter.completeError(
        StateError('join credentials unavailable'),
      );
      await tester.pump();

      expect(
        find.text('Không thể thiết lập kết nối cuộc gọi. Vui lòng thử lại.'),
        findsOneWidget,
      );
      expect(
        find.textContaining(RegExp(r'camera|micro', caseSensitive: false)),
        findsNothing,
      );

      await tester.pumpWidget(const SizedBox.shrink());
      unawaited(coordinator.dispose());
      unawaited(signals.close());
    },
  );
}
