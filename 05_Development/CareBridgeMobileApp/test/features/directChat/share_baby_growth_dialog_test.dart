import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/directChat/widgets/baby_growth_message_card.dart';
import 'package:untitled/features/directChat/widgets/share_baby_growth_dialog.dart';

import 'baby_growth_test_factory.dart';

class _DialogHarness {
  BabyGrowthShareData? result;
  final loadedBabyIds = <String>[];
}

Future<_DialogHarness> _openDialog(
  WidgetTester tester, {
  required List<BabyProfile> babies,
  String? lastOpenedId,
}) async {
  tester.view.physicalSize = const Size(400 * 2, 1400 * 2);
  tester.view.devicePixelRatio = 2.0;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);

  final harness = _DialogHarness();
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () async {
              harness.result = await ShareBabyGrowthDialog.show(
                context,
                babyLoader: () async => babies,
                lastOpenedBabyIdReader: () async => lastOpenedId,
                measurementsLoader: (babyId) async {
                  harness.loadedBabyIds.add(babyId);
                  return makeGrowthMeasurements();
                },
              );
            },
            child: const Text('open'),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('open'));
  await tester.pumpAndSettle();
  return harness;
}

void main() {
  testWidgets('SBG-TC-008 preselects last opened baby among several', (
    tester,
  ) async {
    final harness = await _openDialog(
      tester,
      babies: [
        makeBabyProfile(),
        makeBabyProfile(id: 'baby-b', nickname: 'Bé Bình'),
      ],
      lastOpenedId: 'baby-b',
    );

    expect(find.text('Chia sẻ phát triển của bé'), findsOneWidget);
    await tester.tap(find.byKey(const Key('share-baby-growth-send')));
    await tester.pumpAndSettle();

    expect(harness.result, isNotNull);
    expect(harness.result!.babyId, 'baby-b');
    expect(harness.result!.measurementCount, 3);
    expect(harness.loadedBabyIds, contains('baby-b'));
  });

  testWidgets('SBG-TC-008 auto-selects the only baby', (tester) async {
    final harness = await _openDialog(tester, babies: [makeBabyProfile()]);

    await tester.tap(find.byKey(const Key('share-baby-growth-send')));
    await tester.pumpAndSettle();

    expect(harness.result!.babyId, 'baby-a');
  });

  testWidgets('SBG-TC-008 shows empty state and disables send with no baby', (
    tester,
  ) async {
    await _openDialog(tester, babies: const []);

    expect(find.text('Chưa có hồ sơ bé'), findsOneWidget);
    final send = tester.widget<FilledButton>(
      find.byKey(const Key('share-baby-growth-send')),
    );
    expect(send.onPressed, isNull);
  });
}
