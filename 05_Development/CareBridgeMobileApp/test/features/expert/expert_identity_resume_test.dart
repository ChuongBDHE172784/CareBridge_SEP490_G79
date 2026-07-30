import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/expert/models/expert_onboarding_model.dart';
import 'package:untitled/features/expert/screens/expert_identity_capture_screen.dart';
import 'package:untitled/features/expert/services/expert_image_capture.dart';
import 'package:untitled/features/expert/services/expert_onboarding_service.dart';

void main() {
  testWidgets(
    'identity resubmission returns to the server-driven onboarding gate',
    (tester) async {
      await tester.binding.setSurfaceSize(const Size(800, 2400));
      addTearDown(() => tester.binding.setSurfaceSize(null));
      final api = _FakeExpertApi();
      final service = ExpertOnboardingService(api: api);
      final router = GoRouter(
        initialLocation: '/expert/identity',
        routes: [
          GoRoute(
            path: '/expert/identity',
            builder: (_, _) => ExpertIdentityCaptureScreen(
              capture: _FakeCapture(),
              service: service,
            ),
          ),
          GoRoute(
            path: '/expert-onboarding',
            builder: (_, _) =>
                const Scaffold(body: Text('Onboarding gate reached')),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));

      for (var index = 0; index < 3; index++) {
        final captureButton = find.byIcon(Icons.camera_alt_outlined).at(index);
        await tester.tap(captureButton);
        await tester.pumpAndSettle();
      }

      final submitButton = find.text('Gửi ảnh xác minh');
      await tester.tap(submitButton);
      await tester.pumpAndSettle();

      expect(api.submittedIdentity, isTrue);
      expect(find.text('Onboarding gate reached'), findsOneWidget);
    },
  );
}

class _FakeCapture implements ExpertImageCapture {
  static final _pngBytes = base64Decode(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
  );

  @override
  Future<ExpertEvidenceImage?> capture(
    ExpertEvidenceKind kind, {
    required ImageSource source,
  }) async => ExpertEvidenceImage(
    bytes: _pngBytes,
    fileName: '${kind.name}.png',
    mimeType: 'image/png',
  );
}

class _FakeExpertApi implements ExpertOnboardingApi {
  bool submittedIdentity = false;

  @override
  Future<dynamic> get(String path) async => const {
    'data': {'nextStep': 'UNDER_REVIEW'},
  };

  @override
  Future<dynamic> multipart(
    String path,
    Map<String, String> fields,
    List<MultipartUploadFile> files,
  ) async {
    if (path == '/api/v1/expert/verify-face') {
      return const {
        'data': {'status': 'MATCHED', 'similarity': 0.95, 'threshold': 0.8},
      };
    }
    if (path == '/api/v1/expert/identity') {
      submittedIdentity = true;
    }
    return const {'data': <String, dynamic>{}};
  }

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) async => const {
    'data': <String, dynamic>{},
  };
}
