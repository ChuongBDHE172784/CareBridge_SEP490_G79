from carebridge_evaluation.assertions import has_required_disclaimer


def test_disclaimer_accepts_canonical_medical_professional_wording():
    assert has_required_disclaimer(
        "CareBridge không chẩn đoán, không kê thuốc và không thay thế nhân viên y tế."
    )


def test_disclaimer_keeps_legacy_doctor_wording_compatible():
    assert has_required_disclaimer(
        "CareBridge không chẩn đoán, không kê thuốc và không thay thế bác sĩ."
    )


def test_disclaimer_rejects_missing_professional_boundary():
    assert not has_required_disclaimer("CareBridge không chẩn đoán và không kê thuốc.")
