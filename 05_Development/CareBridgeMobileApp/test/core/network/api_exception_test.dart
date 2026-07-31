import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';

void main() {
  test('reads a standard top-level error code', () {
    final error = ApiException(
      409,
      '{"success":false,"error":"AUTH_ACCOUNT_EXISTS",'
      '"message":"Account already exists"}',
    );

    expect(error.errorCode, 'AUTH_ACCOUNT_EXISTS');
  });

  test('reads compatible nested and top-level code shapes', () {
    expect(
      ApiException(409, '{"error":{"code":"NESTED_CODE"}}').errorCode,
      'NESTED_CODE',
    );
    expect(
      ApiException(409, '{"code":"TOP_LEVEL_CODE"}').errorCode,
      'TOP_LEVEL_CODE',
    );
  });

  test('returns null for malformed or code-less response bodies', () {
    expect(ApiException(400, 'not-json').errorCode, isNull);
    expect(
      ApiException(400, '{"message":"Validation failed"}').errorCode,
      isNull,
    );
  });
}
