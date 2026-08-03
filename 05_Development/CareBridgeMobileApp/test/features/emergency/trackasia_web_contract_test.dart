import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test(
    'web uses pinned official TrackAsia runtime without a stub override',
    () {
      final pubspec = File('pubspec.yaml').readAsStringSync();
      final webIndex = File('web/index.html').readAsStringSync();
      final emergencyMap = File(
        'lib/features/emergency/screens/emergency_map_screen.dart',
      ).readAsStringSync();

      expect(pubspec, isNot(contains('trackasia_gl_web_override')));
      expect(
        File(
          'lib/features/emergency/widgets/trackasia_gl_stub.dart',
        ).existsSync(),
        isFalse,
      );
      expect(webIndex, contains('trackasia-gl@2.0.1/dist/trackasia-gl.js'));
      expect(webIndex, contains('trackasia-gl@2.0.1/dist/trackasia-gl.css'));
      expect(webIndex, isNot(contains('trackasia-gl@latest')));
      expect(emergencyMap, isNot(contains('if (kIsWeb ||')));
      expect(emergencyMap, isNot(contains("if (dart.library.js)")));
      expect(
        emergencyMap,
        contains("import 'package:trackasia_gl/trackasia_gl.dart';"),
      );
      expect(emergencyMap, isNot(contains('không hỗ trợ Flutter Web')));
    },
  );

  test('Dart 3.10 download fallback keeps direct chat downloads on Web', () {
    final directChat = File(
      'lib/features/directChat/screens/direct_chat_screen.dart',
    ).readAsStringSync();

    expect(directChat, contains('if (kIsWeb)'));
    expect(directChat, contains('LaunchMode.externalApplication'));
    expect(directChat, contains('FileSaver.instance.saveAs'));
  });
}
