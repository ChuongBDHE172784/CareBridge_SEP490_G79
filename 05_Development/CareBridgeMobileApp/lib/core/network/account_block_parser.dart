import 'dart:convert';
import 'package:http/http.dart' as http;
import '../auth/blocked_account_state.dart';

const _blockedCodes = <String>{
  'ACCOUNT_ADMIN_LOCKED',
  'ACCOUNT_TEMPORARILY_LOCKED',
  'ACCOUNT_LOCKED',
  'ACCOUNT_DISABLED',
  'ACCOUNT_SUSPENDED',
};

/// Parses a structured account-state response. The backend only returns
/// administrative reasons and appeal authorization after password proof.
BlockedAccountState? parseAccountBlockedState(http.Response response) {
  if (response.statusCode != 403) return null;
  try {
    final body =
        jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    final code = body['error'] as String?;
    if (code == null || !_blockedCodes.contains(code)) return null;
    final metadata = body['metadata'] is Map<String, dynamic>
        ? body['metadata'] as Map<String, dynamic>
        : const <String, dynamic>{};
    final retryAtValue = metadata['retryAt'] as String?;
    return BlockedAccountState(
      code: code,
      lockType: metadata['lockType'] as String?,
      reason: metadata['reason'] as String?,
      retryAt: retryAtValue == null ? null : DateTime.tryParse(retryAtValue),
      appealAllowed: metadata['appealAllowed'] == true,
      appealToken: metadata['appealToken'] as String?,
      appealPending: metadata['appealPending'] == true,
      appealStatus: metadata['appealStatus'] as String?,
    );
  } catch (_) {
    return null;
  }
}

/// Compatibility helper for existing callers and tests.
String? parseAccountBlockedCode(http.Response response) =>
    parseAccountBlockedState(response)?.code;
