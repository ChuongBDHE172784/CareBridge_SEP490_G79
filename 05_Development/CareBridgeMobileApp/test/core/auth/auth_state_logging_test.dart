import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';

void main() {
  test(
    'auth state logs never expose token material or raw account id',
    () async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      const accessToken = 'sensitive-access-token-prefix-and-body';
      const refreshToken = 'sensitive-refresh-token';
      const userId = 'private-account-id';
      final messages = <String>[];
      final previousDebugPrint = debugPrint;
      debugPrint = (message, {wrapWidth}) {
        if (message != null) messages.add(message);
      };
      addTearDown(() {
        debugPrint = previousDebugPrint;
        AuthState.instance.clearState();
      });

      await AuthState.instance.setTokens(
        accessToken: accessToken,
        refreshToken: refreshToken,
        userId: userId,
        role: 'MOTHER',
      );

      final output = messages.join('\n');
      expect(output, isNot(contains(accessToken)));
      expect(output, isNot(contains(accessToken.substring(0, 20))));
      expect(output, isNot(contains(refreshToken)));
      expect(output, isNot(contains(userId)));
    },
  );
}
