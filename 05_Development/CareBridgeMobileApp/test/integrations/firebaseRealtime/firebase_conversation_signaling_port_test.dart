import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/services/firebase_token_service.dart';
import 'package:untitled/integrations/firebaseRealtime/conversation_event_signal.dart';
import 'package:untitled/integrations/firebaseRealtime/firebase_conversation_signaling_port.dart';

class _FakeListenerStarter {
  int startCount = 0;
  int cancelCount = 0;
  String? uid;
  Completer<FirebaseListenerCancellation>? pendingStart;
  final eventCallbacks = <void Function(ConversationEventSignal)>[];
  final errorCallbacks = <void Function()>[];

  Future<FirebaseListenerCancellation> start({
    required String uid,
    required void Function(ConversationEventSignal event) onEvent,
    required void Function() onError,
  }) async {
    startCount++;
    this.uid = uid;
    eventCallbacks.add(onEvent);
    errorCallbacks.add(onError);
    final pending = pendingStart;
    if (pending != null) return pending.future;
    return _cancellation;
  }

  Future<void> _cancellation() async {
    cancelCount++;
  }
}

class _FakeSigner {
  int signCount = 0;
  final tokens = <String>[];
  Completer<String>? pendingSignIn;
  String uid = 'user-1';

  Future<String> sign(String customToken) async {
    signCount++;
    tokens.add(customToken);
    final pending = pendingSignIn;
    if (pending != null) return pending.future;
    return uid;
  }
}

void main() {
  testWidgets('disabled capability is terminal and never starts Firebase', (
    tester,
  ) async {
    var loadCount = 0;
    final firebase = _FakeListenerStarter();
    final signer = _FakeSigner();
    final port = FirebaseConversationSignalingPort(
      tokenLoader: () async {
        loadCount++;
        return FirebaseTokenCapability.disabled;
      },
      signer: signer.sign,
      listenerStarter: firebase.start,
    );

    await port.connect();
    await tester.pump(const Duration(seconds: 30));
    await port.connect();

    expect(loadCount, 1);
    expect(signer.signCount, 0);
    expect(firebase.startCount, 0);
    await port.dispose();
  });

  testWidgets('DCC-012 terminal failure does not schedule a retry', (
    tester,
  ) async {
    var loadCount = 0;
    final firebase = _FakeListenerStarter();
    final signer = _FakeSigner();
    final port = FirebaseConversationSignalingPort(
      tokenLoader: () async {
        loadCount++;
        throw const FirebaseSignalingTerminalException('DCC-012');
      },
      signer: signer.sign,
      listenerStarter: firebase.start,
    );

    await port.connect();
    await tester.pump(const Duration(seconds: 30));
    await port.connect();

    expect(loadCount, 1);
    expect(signer.signCount, 0);
    expect(firebase.startCount, 0);
    await port.dispose();
  });

  testWidgets('transient failure preserves the five-second reconnect', (
    tester,
  ) async {
    var loadCount = 0;
    final firebase = _FakeListenerStarter();
    final signer = _FakeSigner();
    final port = FirebaseConversationSignalingPort(
      tokenLoader: () async {
        loadCount++;
        if (loadCount == 1) throw StateError('temporary network failure');
        return FirebaseTokenCapability.disabled;
      },
      signer: signer.sign,
      listenerStarter: firebase.start,
    );

    await port.connect();
    await tester.pump(const Duration(seconds: 4));
    expect(loadCount, 1);

    await tester.pump(const Duration(seconds: 1));
    await tester.pump();
    expect(loadCount, 2);
    expect(signer.signCount, 0);
    expect(firebase.startCount, 0);

    await tester.pump(const Duration(seconds: 30));
    expect(loadCount, 2);
    await port.dispose();
  });

  testWidgets('enabled capability starts and disposes the Firebase listener', (
    tester,
  ) async {
    final firebase = _FakeListenerStarter();
    final signer = _FakeSigner();
    final port = FirebaseConversationSignalingPort(
      tokenLoader: () async => FirebaseTokenCapability.enabled('token-1'),
      signer: signer.sign,
      listenerStarter: firebase.start,
    );

    await port.connect();

    expect(firebase.startCount, 1);
    expect(signer.tokens, ['token-1']);
    expect(firebase.uid, 'user-1');
    await port.dispose();
    expect(firebase.cancelCount, 1);
  });

  testWidgets('dispose discards a late capability result', (tester) async {
    final capability = Completer<FirebaseTokenCapability>();
    final firebase = _FakeListenerStarter();
    final signer = _FakeSigner();
    final port = FirebaseConversationSignalingPort(
      tokenLoader: () => capability.future,
      signer: signer.sign,
      listenerStarter: firebase.start,
    );

    final connect = port.connect();
    await tester.pump();
    await port.dispose();
    capability.complete(FirebaseTokenCapability.enabled('late-token'));
    await connect;
    await tester.pump(const Duration(seconds: 30));

    expect(firebase.startCount, 0);
    expect(signer.signCount, 0);
  });

  testWidgets('dispose cancels a listener that finishes attaching late', (
    tester,
  ) async {
    final pendingStart = Completer<FirebaseListenerCancellation>();
    final firebase = _FakeListenerStarter()..pendingStart = pendingStart;
    final signer = _FakeSigner();
    final port = FirebaseConversationSignalingPort(
      tokenLoader: () async => FirebaseTokenCapability.enabled('token-1'),
      signer: signer.sign,
      listenerStarter: firebase.start,
    );

    final connect = port.connect();
    await tester.pump();
    expect(firebase.startCount, 1);

    await port.dispose();
    pendingStart.complete(() async {
      firebase.cancelCount++;
    });
    await connect;

    expect(firebase.cancelCount, 1);
  });

  testWidgets(
    'replacement port waits for stale sign-in and never attaches its listener',
    (tester) async {
      final oldSignIn = Completer<String>();
      final oldSigner = _FakeSigner()..pendingSignIn = oldSignIn;
      final oldFirebase = _FakeListenerStarter();
      final cleanedUids = <String>[];
      final oldPort = FirebaseConversationSignalingPort(
        tokenLoader: () async => FirebaseTokenCapability.enabled('old-token'),
        signer: oldSigner.sign,
        staleSignInCleaner: (uid) async => cleanedUids.add(uid),
        listenerStarter: oldFirebase.start,
      );

      final oldConnect = oldPort.connect();
      await tester.pump();
      expect(oldSigner.signCount, 1);
      await oldPort.dispose();

      final newSigner = _FakeSigner()..uid = 'user-2';
      final newFirebase = _FakeListenerStarter();
      final newPort = FirebaseConversationSignalingPort(
        tokenLoader: () async => FirebaseTokenCapability.enabled('new-token'),
        signer: newSigner.sign,
        listenerStarter: newFirebase.start,
      );
      final newConnect = newPort.connect();
      await tester.pump();
      expect(newSigner.signCount, 0);

      oldSignIn.complete('user-1');
      await oldConnect;
      await newConnect;

      expect(oldFirebase.startCount, 0);
      expect(cleanedUids, ['user-1']);
      expect(newSigner.tokens, ['new-token']);
      expect(newFirebase.startCount, 1);
      expect(newFirebase.uid, 'user-2');
      await newPort.dispose();
    },
  );

  testWidgets('stale listener callbacks cannot affect a reconnected attempt', (
    tester,
  ) async {
    final firebase = _FakeListenerStarter();
    final signer = _FakeSigner();
    final port = FirebaseConversationSignalingPort(
      tokenLoader: () async => FirebaseTokenCapability.enabled('token-1'),
      signer: signer.sign,
      listenerStarter: firebase.start,
    );
    final delivered = <ConversationEventSignal>[];
    final subscription = port.events.listen(delivered.add);
    addTearDown(subscription.cancel);

    await port.connect();
    expect(firebase.startCount, 1);
    firebase.errorCallbacks[0]();
    await tester.pump(const Duration(seconds: 5));
    await tester.pump();
    expect(firebase.startCount, 2);

    firebase.eventCallbacks[0](
      ConversationEventSignal(
        eventId: 'stale-event',
        eventType: 'MESSAGE_SENT',
        conversationId: 'conversation-1',
        resourceId: 'message-1',
        occurredAt: DateTime.utc(2026, 8, 3),
      ),
    );
    firebase.errorCallbacks[0]();
    await tester.pump(const Duration(seconds: 5));

    expect(delivered, isEmpty);
    expect(firebase.startCount, 2);
    await port.dispose();
  });
}
