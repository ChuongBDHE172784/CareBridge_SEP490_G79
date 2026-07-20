import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/services/postpartum_log_draft_store.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() => FlutterSecureStorage.setMockInitialValues({}));

  test('draft is scoped by account and journey and can be cleared', () async {
    const store = PostpartumLogDraftStore();
    await store.write('account-a', 'journey-a', {
      'submissionId': 'submission-a',
      'pain': '3',
    });

    expect(
      await store.read('account-a', 'journey-a'),
      containsPair('submissionId', 'submission-a'),
    );
    expect(await store.read('account-a', 'journey-b'), isNull);
    expect(await store.read('account-b', 'journey-a'), isNull);

    await store.delete('account-a', 'journey-a');
    expect(await store.read('account-a', 'journey-a'), isNull);
  });
}
