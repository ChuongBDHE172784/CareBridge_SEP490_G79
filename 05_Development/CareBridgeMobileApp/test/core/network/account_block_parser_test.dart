import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:untitled/core/network/account_block_parser.dart';

void main() {
  http.Response makeResponse(int status, Map<String, dynamic> body) {
    return http.Response(
      jsonEncode(body),
      status,
      headers: {'content-type': 'application/json'},
    );
  }

  group('parseAccountBlockedCode', () {
    test('parses administrative lock metadata', () {
      final response = makeResponse(403, {
        'error': 'ACCOUNT_ADMIN_LOCKED',
        'status': 403,
        'metadata': {
          'lockType': 'ADMIN',
          'reason': 'Vi phạm quy định cộng đồng',
        },
      });

      final state = parseAccountBlockedState(response);
      expect(state?.code, 'ACCOUNT_ADMIN_LOCKED');
      expect(state?.lockType, 'ADMIN');
      expect(state?.reason, 'Vi phạm quy định cộng đồng');
      // An admin lock only clears through customer support.
      expect(state?.needsSupportContact, isTrue);
    });

    test('parses temporary-lock retry time and clears without support', () {
      final response = makeResponse(403, {
        'error': 'ACCOUNT_TEMPORARILY_LOCKED',
        'status': 403,
        'metadata': {
          'lockType': 'TEMPORARY',
          'retryAt': '2026-07-29T02:00:00Z',
        },
      });

      final state = parseAccountBlockedState(response);
      expect(state?.retryAt, DateTime.parse('2026-07-29T02:00:00Z'));
      expect(state?.needsSupportContact, isFalse);
    });

    test('ignores leftover appeal metadata from an older server build', () {
      final response = makeResponse(403, {
        'error': 'ACCOUNT_ADMIN_LOCKED',
        'metadata': {
          'appealAllowed': true,
          'appealToken': 'appeal-token',
          'appealStatus': 'PENDING',
        },
      });

      final state = parseAccountBlockedState(response);
      expect(state?.code, 'ACCOUNT_ADMIN_LOCKED');
      expect(state?.needsSupportContact, isTrue);
    });

    test('returns ACCOUNT_DISABLED for 403 with matching error code', () {
      final response = makeResponse(403, {
        'error': 'ACCOUNT_DISABLED',
        'status': 403,
      });
      expect(parseAccountBlockedCode(response), 'ACCOUNT_DISABLED');
    });

    test('returns ACCOUNT_LOCKED for 403 with matching error code', () {
      final response = makeResponse(403, {
        'error': 'ACCOUNT_LOCKED',
        'status': 403,
      });
      expect(parseAccountBlockedCode(response), 'ACCOUNT_LOCKED');
    });

    test(
      'returns null for 403 with ACCESS_DENIED (role mismatch — must not logout)',
      () {
        final response = makeResponse(403, {
          'error': 'ACCESS_DENIED',
          'status': 403,
        });
        expect(parseAccountBlockedCode(response), isNull);
      },
    );

    test('returns null for 401', () {
      final response = makeResponse(401, {
        'error': 'AUTHENTICATION_FAILED',
        'status': 401,
      });
      expect(parseAccountBlockedCode(response), isNull);
    });

    test('returns null for 200', () {
      final response = http.Response('{"id":"abc"}', 200);
      expect(parseAccountBlockedCode(response), isNull);
    });

    test('returns null for 403 with empty body', () {
      final response = http.Response('', 403);
      expect(parseAccountBlockedCode(response), isNull);
    });

    test('returns null for 403 with non-JSON body', () {
      final response = http.Response('not-json', 403);
      expect(parseAccountBlockedCode(response), isNull);
    });

    test('returns null for 403 with missing error field', () {
      final response = makeResponse(403, {'message': 'forbidden'});
      expect(parseAccountBlockedCode(response), isNull);
    });
  });
}
