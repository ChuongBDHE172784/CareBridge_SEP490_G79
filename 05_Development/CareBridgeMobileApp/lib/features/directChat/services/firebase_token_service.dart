import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';

typedef FirebaseTokenRequest =
    Future<dynamic> Function(
      String path,
      Map<String, dynamic> body, {
      String? token,
      String? expectedAccountId,
    });
typedef FirebaseSessionLoader =
    ({String? userId, String? accessToken}) Function();

class FirebaseTokenCapability {
  const FirebaseTokenCapability._({
    required this.firestoreSignalingEnabled,
    this.firebaseCustomToken,
  });

  static const disabled = FirebaseTokenCapability._(
    firestoreSignalingEnabled: false,
  );

  factory FirebaseTokenCapability.enabled(String firebaseCustomToken) {
    final normalizedToken = firebaseCustomToken.trim();
    if (normalizedToken.isEmpty) {
      throw const FirebaseSignalingTerminalException(
        'Enabled Firebase signaling response did not include a token',
      );
    }
    return FirebaseTokenCapability._(
      firestoreSignalingEnabled: true,
      firebaseCustomToken: normalizedToken,
    );
  }

  final bool firestoreSignalingEnabled;
  final String? firebaseCustomToken;
}

/// A deterministic capability/protocol failure that must not be retried for
/// the current signaling session. A new hub session creates a new port and may
/// negotiate again.
class FirebaseSignalingTerminalException implements Exception {
  const FirebaseSignalingTerminalException(this.message);

  final String message;

  @override
  String toString() => 'FirebaseSignalingTerminalException: $message';
}

// BR-DCC-013: the server derives the uid strictly from the caller's own JWT;
// this client never sends a target user id.
class FirebaseTokenService {
  FirebaseTokenService({
    FirebaseTokenRequest? request,
    FirebaseSessionLoader? sessionLoader,
  }) : _request = request ?? _sessionBoundPost,
       _sessionLoader = sessionLoader ?? _currentSession;

  static final FirebaseTokenService instance = FirebaseTokenService();

  static const _firebaseUnavailableCode = 'DCC-012';
  static const _sessionChangedCode = 'AUTH_SESSION_CHANGED';
  final FirebaseTokenRequest _request;
  final FirebaseSessionLoader _sessionLoader;

  static ({String? userId, String? accessToken}) _currentSession() {
    final auth = AuthState.instance;
    return (userId: auth.userId, accessToken: auth.accessToken);
  }

  static Future<dynamic> _sessionBoundPost(
    String path,
    Map<String, dynamic> body, {
    String? token,
    String? expectedAccountId,
  }) => apiPost(path, body, token: token, expectedAccountId: expectedAccountId);

  Future<FirebaseTokenCapability> fetchCapability() async {
    final session = _sessionLoader();
    final userId = session.userId;
    final accessToken = session.accessToken;
    if (userId == null ||
        userId.isEmpty ||
        accessToken == null ||
        accessToken.isEmpty) {
      throw const FirebaseSignalingTerminalException(
        'Firebase signaling requires an active account session',
      );
    }
    try {
      final response = await _request(
        '/api/v1/firebase/custom-token',
        {},
        token: accessToken,
        expectedAccountId: userId,
      );
      if (response is! Map<String, dynamic>) {
        throw const FirebaseSignalingTerminalException(
          'Firebase capability response was not an object',
        );
      }
      final data = response['data'];
      if (data is! Map<String, dynamic>) {
        throw const FirebaseSignalingTerminalException(
          'Firebase capability response did not include data',
        );
      }
      final enabled = data['firestoreSignalingEnabled'];
      if (enabled is! bool) {
        throw const FirebaseSignalingTerminalException(
          'Firebase capability response did not include a boolean flag',
        );
      }
      if (!enabled) {
        if (!data.containsKey('firebaseCustomToken') ||
            data['firebaseCustomToken'] != null) {
          throw const FirebaseSignalingTerminalException(
            'Disabled Firebase signaling response included an invalid token',
          );
        }
        return FirebaseTokenCapability.disabled;
      }

      final token = data['firebaseCustomToken'];
      if (token is! String) {
        throw const FirebaseSignalingTerminalException(
          'Enabled Firebase signaling response did not include a token',
        );
      }
      return FirebaseTokenCapability.enabled(token);
    } on ApiException catch (error) {
      if (error.errorCode == _firebaseUnavailableCode ||
          error.errorCode == _sessionChangedCode) {
        throw const FirebaseSignalingTerminalException(
          'Firebase signaling is unavailable for this session',
        );
      }
      rethrow;
    }
  }
}
