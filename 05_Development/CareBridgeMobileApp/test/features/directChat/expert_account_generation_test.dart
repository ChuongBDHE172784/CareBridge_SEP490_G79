import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/screens/expert_directory_screen.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/expert/screens/expert_public_profile_screen.dart';
import 'package:untitled/features/expert/models/expert_availability_slot.dart';
import 'package:untitled/features/expert/services/expert_availability_service.dart';

class _DelayedExpertService extends DirectChatService {
  final directory = Completer<ExpertDirectoryPage>();
  final profile = Completer<Map<String, dynamic>>();

  @override
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) => directory.future;

  @override
  Future<Map<String, dynamic>> getExpertProfile(String expertProfileId) =>
      profile.future;
}

class _EmptyAvailabilityService extends ExpertAvailabilityService {
  @override
  Future<List<ExpertAvailabilitySlot>> getPublicAvailability(
    String expertProfileId,
  ) async => [];
}

void main() {
  late DirectChatService original;

  setUp(() async {
    original = DirectChatService.instance;
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.clear();
    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access-a',
      refreshToken: 'synthetic-refresh-a',
      userId: 'mother-a',
      role: 'MOTHER',
    );
  });

  tearDown(() async {
    DirectChatService.instance = original;
    await AuthState.instance.clear();
  });

  test('directory parser accepts backend canonical eligibility key', () {
    final item = ExpertDirectoryItem.fromJson({
      'expertProfileId': 'expert-canonical',
      'verificationStatus': 'APPROVED',
      'consultationEligible': true,
    });

    expect(item.isConsultationEligible, isTrue);
    expect(item.isEligibleForTriageHandoff, isTrue);
  });

  test('directory parser keeps the legacy eligibility alias compatible', () {
    final item = ExpertDirectoryItem.fromJson({
      'expertProfileId': 'expert-legacy',
      'verificationStatus': 'APPROVED',
      'isConsultationEligible': true,
    });

    expect(item.isEligibleForTriageHandoff, isTrue);
  });

  testWidgets('late account-A directory response cannot render for B', (
    tester,
  ) async {
    final service = _DelayedExpertService();
    DirectChatService.instance = service;
    await tester.pumpWidget(const MaterialApp(home: ExpertDirectoryScreen()));
    await tester.pump();

    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access-b',
      refreshToken: 'synthetic-refresh-b',
      userId: 'mother-b',
      role: 'MOTHER',
    );
    service.directory.complete(
      const ExpertDirectoryPage(
        experts: [
          ExpertDirectoryItem(
            expertProfileId: 'expert-a',
            displayName: 'Account A expert',
          ),
        ],
        currentPage: 0,
        pageSize: 20,
        totalElements: 1,
        totalPages: 1,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Account A expert'), findsNothing);
    expect(find.textContaining('Phiên đăng nhập'), findsOneWidget);
  });

  testWidgets('late account-A public profile cannot render for B', (
    tester,
  ) async {
    final service = _DelayedExpertService();
    DirectChatService.instance = service;
    await tester.pumpWidget(
      MaterialApp(
        home: ExpertPublicProfileScreen(
          expertProfileId: 'expert-a',
          availabilityService: _EmptyAvailabilityService(),
        ),
      ),
    );
    await tester.pump();

    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access-b',
      refreshToken: 'synthetic-refresh-b',
      userId: 'mother-b',
      role: 'MOTHER',
    );
    service.profile.complete({
      'expertProfileId': 'expert-a',
      'displayName': 'Account A private profile',
      'verificationStatus': 'APPROVED',
      'consultationEligible': true,
    });
    await tester.pumpAndSettle();

    expect(find.text('Account A private profile'), findsNothing);
    expect(find.textContaining('Phiên đăng nhập'), findsOneWidget);
  });
}
