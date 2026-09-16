import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/directChat/widgets/baby_growth_message_card.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';

// Props Isolation factories for ShareBabyGrowthInDirectChat (SBG Test-Spec §4.1, DATA-01..05).

const kTestBabyId = '11111111-1111-1111-1111-111111111111';

BabyGrowthLatestSnapshot makeLatest() => BabyGrowthLatestSnapshot(
  measuredDate: DateTime(2025, 3, 10),
  weightKg: 5.6,
  heightCm: 58.0,
  headCircumferenceCm: 39.0,
);

BabyGrowthShareData makeShareData({
  String? note,
  String nickname = 'Bé Test',
  int count = 3,
  bool withLatest = true,
}) => BabyGrowthShareData(
  babyId: kTestBabyId,
  babyNickname: nickname,
  birthDate: DateTime(2025, 1, 10),
  measurementCount: count,
  latest: withLatest ? makeLatest() : null,
  note: note,
);

List<GrowthMeasurement> makeGrowthMeasurements({bool includeRecent = false}) => [
  GrowthMeasurement(
    id: 'm1',
    measuredAt: DateTime(2025, 1, 10),
    weightKg: 3.2,
    heightCm: 50.0,
    headCircumferenceCm: 34.0,
    recordedBy: 'mother',
  ),
  GrowthMeasurement(
    id: 'm2',
    measuredAt: DateTime(2025, 2, 10),
    weightKg: 4.5,
    heightCm: 54.5,
    recordedBy: 'mother',
  ),
  GrowthMeasurement(
    id: 'm3',
    measuredAt: DateTime(2025, 3, 10),
    weightKg: 5.6,
    heightCm: 58.0,
    headCircumferenceCm: 39.0,
    recordedBy: 'mother',
  ),
  if (includeRecent)
    GrowthMeasurement(
      id: 'm4',
      measuredAt: DateTime(2026, 9, 1),
      weightKg: 9.8,
      heightCm: 75.0,
      headCircumferenceCm: 45.0,
      recordedBy: 'mother',
    ),
];

BabyProfile makeBabyProfile({String id = 'baby-a', String nickname = 'Bé An'}) =>
    BabyProfile(
      id: id,
      nickname: nickname,
      birthDate: DateTime(2025, 1, 10),
      gender: BabyGender.female,
      isActive: true,
    );
