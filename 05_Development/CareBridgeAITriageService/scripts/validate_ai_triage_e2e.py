import json
from app.rules.registry import get_registry
from app.triage.api import TriageTurnRequest, execute_turn

registry = get_registry()

test_cases = [
    {
        'name': '1. PRECONCEPTION - Possible pregnancy with pain and bleeding',
        'target': 'MOTHER',
        'stage': 'POSSIBLE_PREGNANCY',
        'context': {'stage': 'POSSIBLE_PREGNANCY', 'possiblePregnancy': 'UNKNOWN'},
        'message': 'Tôi bị chậm kinh 2 tuần và đau bụng dưới âm ỉ',
        'expected_outcome': 'NEEDS_MORE_INFO',
    },
    {
        'name': '2. PREGNANCY - RED danger flag (Heavy bleeding)',
        'target': 'MOTHER',
        'stage': 'PREGNANCY',
        'context': {'stage': 'PREGNANCY', 'gestationalWeek': 32},
        'message': 'Thai 32 tuần bị ra máu âm đạo ướt đẫm băng',
        'expected_outcome': 'RED',
    },
    {
        'name': '3. POSTPARTUM - RED danger flag (Heavy bleeding with large clots)',
        'target': 'MOTHER',
        'stage': 'POSTPARTUM_MOTHER',
        'context': {'stage': 'POSTPARTUM_MOTHER', 'postpartumDay': 5},
        'message': 'Tôi sinh được 5 ngày, hôm nay bị ra máu nhiều ướt đẫm 2 băng vệ sinh và có nhiều cục máu đông lớn',
        'expected_outcome': 'RED',
    },
    {
        'name': '4. INFANT (0-12m) - Fever under 3 months (RED)',
        'target': 'BABY',
        'stage': 'INFANT_0_12M',
        'context': {'stage': 'INFANT_0_12M', 'babyAgeMonths': 2},
        'message': 'Bé 2 tháng tuổi bị sốt cao 39 độ và li bì',
        'expected_outcome': 'RED',
    },
    {
        'name': '5. TODDLER (12-24m) - Hand Foot Mouth rash with feeding refusal',
        'target': 'BABY',
        'stage': 'TODDLER_12_24M',
        'context': {'stage': 'TODDLER_12_24M', 'babyAgeMonths': 20},
        'message': 'Bé 20 tháng bị nổi các nốt ban đỏ ở lòng bàn tay, bàn chân và trong miệng bé bị loét, quấy khóc không chịu ăn',
        'expected_outcome': 'NEEDS_MORE_INFO',
    }
]

print('=' * 80)
print('AUTOMATED AI TRIAGE (LangGraph + RAG) VALIDATION SUITE')
print('=' * 80)

passed = 0
for idx, tc in enumerate(test_cases, 1):
    req = TriageTurnRequest(
        sessionId=f'20000000-0000-0000-0000-00000000000{idx}',
        stateVersion=0,
        expectedStateVersion=0,
        requestId=f'req_auto_test_0000000{idx}',
        messageId=f'msg_auto_test_0000000{idx}',
        latestUserMessage=tc['message'],
        selectedTarget=tc['target'],
        journeyContext=tc['context'],
        expectedRulesetHash=registry.ruleset_sha256,
    )
    res = execute_turn(req)
    state = res.model_dump()['state']
    outcome = state.get('triageOutcome')
    citations = state.get('citations', [])
    questions = state.get('questions', [])
    decisive_rules = state.get('decisiveRuleIds', [])
    
    is_ok = (outcome == tc['expected_outcome'])
    if is_ok:
        passed += 1
        status = 'PASS'
    else:
        status = 'FAIL'
        
    print(f"[{status}] {tc['name']}")
    print(f"   - Outcome: {outcome} (Expected: {tc['expected_outcome']})")
    print(f"   - Decisive Rules: {decisive_rules}")
    print(f"   - Questions Asked: {len(questions)}")
    print(f"   - Evidence Citations: {len(citations)}")
    print('-' * 80)

print(f"Summary: {passed}/{len(test_cases)} tests passed perfectly!")
