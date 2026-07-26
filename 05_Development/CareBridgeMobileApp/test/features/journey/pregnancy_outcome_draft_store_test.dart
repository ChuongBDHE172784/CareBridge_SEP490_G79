import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/journey/services/pregnancy_outcome_draft_store.dart';

void main() {
  test('secure draft index encodes a deterministic JSON array', () {
    final encoded = SecurePregnancyOutcomeDraftStore.encodeIndexForStorage({
      'draft-b',
      'draft-a',
    });

    expect(jsonDecode(encoded), <String>['draft-a', 'draft-b']);
  });
}
