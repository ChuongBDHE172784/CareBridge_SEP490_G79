import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_care_hub_screen.dart';
import 'package:untitled/features/baby/services/baby_service.dart';

class _ProfilesService extends BabyService {
  _ProfilesService(this.profile);

  final BabyProfile profile;

  @override
  Future<List<BabyProfile>> listBabyProfiles() async => [profile];
}

void main() {
  testWidgets('does not render a fabricated baby when profiles are disabled', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: BabyCareHubScreen(loadProfiles: false)),
    );

    expect(find.text('Baby A'), findsNothing);
    expect(find.text('Baby B'), findsNothing);
    expect(
      find.text('Chưa có hồ sơ bé để hiển thị dữ liệu chăm sóc.'),
      findsOneWidget,
    );
  });

  testWidgets(
    'does not fall back to another baby for an invalid deep-link id',
    (tester) async {
      final profile = BabyProfile(
        id: 'baby-real',
        nickname: 'Bông',
        birthDate: DateTime(2026, 7, 1),
        gender: BabyGender.unknown,
        isActive: true,
      );
      await tester.pumpWidget(
        MaterialApp(
          home: BabyCareHubScreen(
            initialBabyId: 'baby-missing',
            babyService: _ProfilesService(profile),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.text(
          'Bé được chọn không tồn tại hoặc bạn không có quyền truy cập.',
        ),
        findsOneWidget,
      );
      expect(find.text('Bông'), findsNothing);
    },
  );
}
