import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/models/triage_v2_session.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';

void main() {
  group('partial cutover to V2', () {
    test('nothing hands off yet: V2 has no chat experience to hand off to', () {
      // V2's screen is an internal test harness, not the chat this app offers. Bouncing a user
      // out of the chat mid-journey is a downgrade, so the hand-off stays closed for every stage.
      for (final stage in [...maternalTriageStages, 'INFANT', 'TODDLER']) {
        expect(shouldHandOffToTriageV2(stage), isFalse, reason: stage);
      }
    });

    test('paediatric stages are never even cutover candidates', () {
      // Separate from the experience gate. The original reason was that V2 declared no
      // paediatric rule at all — that stopped being true once PED_RED_001..006 and
      // PED_YELLOW_001 were ported, so V2 now carries 13 paediatric rules. The gate stays
      // maternal-only as a scoping decision about what to cut over first, not because a
      // baby has nowhere to land. Widening it is therefore a product call, not a blocked one.
      for (final stage in ['INFANT', 'TODDLER']) {
        expect(isTriageV2CutoverCandidate(stage), isFalse, reason: stage);
      }
    });

    test('only maternal stages are candidates for the cutover', () {
      expect(maternalTriageStages, {'PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'});
      expect(maternalTriageStages, isNot(contains('INFANT')));
      expect(maternalTriageStages, isNot(contains('TODDLER')));
    });
  });

  group('V2 question catalogue', () {
    test('exposes the deterministic danger and safety screens', () {
      for (final id in ['Q_GLOBAL_DANGER', 'Q_SAFETY_SELF_HARM', 'Q_HEADACHE_SEVERITY']) {
        expect(TriageV2Question.fromId(id).optionCodes, isNotEmpty, reason: id);
      }
    });

    test('every global danger sign is offered as an explicit option', () {
      // These signals were previously reachable only through free-text extraction, so an LLM
      // outage left the engine unable to detect any emergency.
      expect(TriageV2Question.fromId('Q_GLOBAL_DANGER').optionCodes, containsAll(<String>[
        'DANGER_NONE',
        'DANGER_SEIZURE',
        'DANGER_UNCONSCIOUS',
        'DANGER_BREATHING',
        'DANGER_CYANOSIS',
      ]));
    });

    test('the self-harm screen offers a decline option distinct from a denial', () {
      final codes = TriageV2Question.fromId('Q_SAFETY_SELF_HARM').optionCodes;
      expect(codes, contains('SELF_HARM_NONE'));
      expect(codes, contains('UNSURE'));
    });
  });
}
