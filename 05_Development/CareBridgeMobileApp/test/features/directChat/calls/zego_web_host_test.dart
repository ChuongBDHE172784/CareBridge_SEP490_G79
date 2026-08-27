import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('Flutter Web host loads Zego wrappers in official package order', () {
    final html = File('web/index.html').readAsStringSync();
    const copyrightedMusic =
        'assets/packages/zego_express_engine/assets/copyrighted-music.js';
    const flutterWrapper =
        'assets/packages/zego_express_engine/assets/'
        'ZegoExpressWebFlutterWrapper.js';

    final copyrightedMusicIndex = html.indexOf(copyrightedMusic);
    final flutterWrapperIndex = html.indexOf(flutterWrapper);
    final headEndIndex = html.indexOf('</head>');
    final flutterBootstrapIndex = html.indexOf('flutter_bootstrap.js');

    expect(copyrightedMusicIndex, greaterThanOrEqualTo(0));
    expect(flutterWrapperIndex, greaterThan(copyrightedMusicIndex));
    expect(headEndIndex, greaterThan(flutterWrapperIndex));
    expect(flutterBootstrapIndex, greaterThan(flutterWrapperIndex));
  });
}
