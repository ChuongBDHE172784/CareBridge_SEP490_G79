import 'dart:convert';
import 'package:http/http.dart' as http;

/// Returns the account-block error code if [response] is a 403 carrying
/// ACCOUNT_DISABLED or ACCOUNT_LOCKED in the `error` field; null otherwise.
String? parseAccountBlockedCode(http.Response response) {
  if (response.statusCode != 403) return null;
  try {
    final body =
        jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    final code = body['error'] as String?;
    if (code == 'ACCOUNT_DISABLED' || code == 'ACCOUNT_LOCKED') return code;
  } catch (_) {}
  return null;
}
