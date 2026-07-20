import 'dart:convert';

import '../../../core/storage/token_storage.dart';

class PostpartumLogDraftStore {
  const PostpartumLogDraftStore();

  Future<Map<String, dynamic>?> read(String userId, String journeyId) async {
    final raw = await PostpartumDraftStorageCoordinator.read(
      userId,
      key(userId, journeyId),
    );
    if (raw == null) return null;
    try {
      return jsonDecode(raw) as Map<String, dynamic>;
    } catch (_) {
      await delete(userId, journeyId);
      return null;
    }
  }

  Future<void> write(
    String userId,
    String journeyId,
    Map<String, dynamic> value,
  ) {
    final generation = PostpartumDraftStorageCoordinator.generationFor(userId);
    return PostpartumDraftStorageCoordinator.write(
      userId: userId,
      key: key(userId, journeyId),
      value: jsonEncode(value),
      generation: generation,
    );
  }

  Future<void> delete(String userId, String journeyId) =>
      PostpartumDraftStorageCoordinator.delete(userId, key(userId, journeyId));

  static String key(String userId, String journeyId) =>
      'cb_postpartum_log_draft_${userId}_$journeyId';
}
