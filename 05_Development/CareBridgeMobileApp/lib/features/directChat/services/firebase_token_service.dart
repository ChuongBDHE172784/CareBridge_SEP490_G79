import '../../../core/network/api_client.dart';

// BR-DCC-013: server derives the uid strictly from the caller's own JWT — this client
// never sends a target user id, there is no field to send one in.
class FirebaseTokenService {
  static final FirebaseTokenService instance = FirebaseTokenService._();
  FirebaseTokenService._();

  Future<String> fetchCustomToken() async {
    final response = await apiPost('/api/v1/firebase/custom-token', {});
    final data = response['data'] as Map<String, dynamic>;
    return data['firebaseCustomToken'] as String;
  }
}
