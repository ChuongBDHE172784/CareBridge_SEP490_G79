import 'package:flutter/foundation.dart';

import '../models/expert_onboarding_model.dart';

class ExpertOnboardingStore extends ChangeNotifier {
  ExpertOnboardingStore._();

  static final ExpertOnboardingStore instance = ExpertOnboardingStore._();

  String? _userId;
  ExpertOnboardingState? _state;

  ExpertOnboardingState? get state => _state;
  bool get approved => _state?.approved == true;
  bool approvedFor(String? userId) => _userId == userId && approved;

  void bindUser(String? userId) {
    if (_userId == userId) return;
    _userId = userId;
    _state = null;
    notifyListeners();
  }

  void update(ExpertOnboardingState state) {
    _state = state;
    notifyListeners();
  }

  void invalidate() {
    _state = null;
    notifyListeners();
  }
}
