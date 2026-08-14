import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/models/triage_session.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';

void main() {
  group('canonical engine cutover', () {
    test('all stages map to one deterministic target and canonical stage', () {
      expect(canonicalTriageTargetForStage('PREGNANCY'), 'MOTHER');
      expect(canonicalTriageTargetForStage('POSTPARTUM'), 'MOTHER');
      expect(canonicalTriageTargetForStage('INFANT'), 'BABY');
      expect(canonicalTriageTargetForStage('TODDLER'), 'BABY');
      expect(canonicalTriageStage('POSTPARTUM'), 'POSTPARTUM_MOTHER');
      expect(canonicalTriageStage('INFANT'), 'INFANT_0_12M');
      expect(canonicalTriageStage('TODDLER'), 'TODDLER_12_24M');
    });

    test(
      'supported-stage metadata includes maternal and paediatric stages',
      () {
        expect(maternalTriageStages, {
          'PRECONCEPTION',
          'PREGNANCY',
          'POSTPARTUM',
        });
        expect(canonicalTriageStages, containsAll({'INFANT', 'TODDLER'}));
      },
    );
  });

  group('Canonical question transport', () {
    test('parses machine codes together with Vietnamese display labels', () {
      final question = TriageQuestion.fromJson(const {
        'questionId': 'Q_GLOBAL_DANGER',
        'text': 'Hiện tại có dấu hiệu nào sau đây không?',
        'answerType': 'SINGLE_CHOICE',
        'options': [
          {'optionCode': 'DANGER_NONE', 'displayText': 'Không có dấu hiệu nào'},
          {'optionCode': 'DANGER_SEIZURE', 'displayText': 'Co giật'},
        ],
      });

      expect(question.optionCodes, ['DANGER_NONE', 'DANGER_SEIZURE']);
      expect(question.options.last.displayText, 'Co giật');
    });

    test('rejects an option without a Vietnamese display label', () {
      expect(
        () => TriageQuestion.fromJson(const {
          'questionId': 'Q_GLOBAL_DANGER',
          'text': 'Hiện tại có dấu hiệu nào sau đây không?',
          'answerType': 'SINGLE_CHOICE',
          'options': [
            {'optionCode': 'DANGER_NONE', 'displayText': ''},
          ],
        }),
        throwsFormatException,
      );
    });
  });
}
