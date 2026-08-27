import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/expert_onboarding_model.dart';
import '../services/expert_image_capture.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

class ExpertIdentityCaptureScreen extends StatefulWidget {
  const ExpertIdentityCaptureScreen({super.key, this.capture, this.service});

  final ExpertImageCapture? capture;
  final ExpertOnboardingService? service;

  @override
  State<ExpertIdentityCaptureScreen> createState() =>
      _ExpertIdentityCaptureScreenState();
}

class _ExpertIdentityCaptureScreenState
    extends State<ExpertIdentityCaptureScreen> {
  ExpertEvidenceImage? _selfie;
  ExpertEvidenceImage? _front;
  ExpertEvidenceImage? _back;
  bool _submitting = false;
  String? _error;
  bool _verifying = false;
  double? _similarity;
  String? _aiStatus;
  Color? _aiColor;

  ExpertImageCapture get _capture =>
      widget.capture ?? ImagePickerExpertCapture();

  Future<void> _returnToLogin() async {
    await AuthState.instance.clear();
    if (mounted) context.go('/login');
  }

  Future<void> _pick(ExpertEvidenceKind kind, ImageSource source) async {
    try {
      final image = await _capture.capture(kind, source: source);
      if (image == null || !mounted) return;
      setState(() {
        switch (kind) {
          case ExpertEvidenceKind.selfie:
            _selfie = image;
          case ExpertEvidenceKind.identityFront:
            _front = image;
          case ExpertEvidenceKind.identityBack:
            _back = image;
          case ExpertEvidenceKind.credential:
            break;
        }
        _error = null;
      });
      await _runAiVerify();
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể mở camera hoặc thư viện ảnh.');
      }
    }
  }

  Future<void> _runAiVerify() async {
    if (_selfie == null || _front == null) return;
    setState(() {
      _verifying = true;
      _aiStatus = null;
      _similarity = null;
      _aiColor = null;
    });
    try {
      final result = await (widget.service ?? ExpertOnboardingService.instance)
          .previewFace(selfie: _selfie!, identityFront: _front!);
      if (!mounted) return;
      setState(() {
        _verifying = false;
        _similarity = result.similarity;
        _aiStatus = result.status;
        _aiColor = result.matched ? Colors.green : Colors.orange;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _verifying = false;
          _aiStatus = 'ERROR';
          _aiColor = Colors.orange;
        });
      }
    }
  }

  Future<void> _submit() async {
    if (_selfie == null || _front == null || _back == null) {
      setState(() => _error = 'Cần đủ ảnh chân dung và hai mặt CCCD.');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await (widget.service ?? ExpertOnboardingService.instance).submitIdentity(
        selfie: _selfie!,
        identityFront: _front!,
        identityBack: _back!,
      );
      ExpertOnboardingStore.instance.invalidate();
      if (mounted) context.go('/expert-onboarding');
    } on ArgumentError catch (e) {
      setState(() => _error = e.message.toString());
    } on ApiException catch (e) {
      setState(
        () => _error = e.statusCode == 413
            ? 'Một ảnh vượt quá giới hạn 5 MB.'
            : 'Không thể gửi ảnh xác minh. Vui lòng thử lại.',
      );
    } catch (_) {
      setState(() => _error = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F1ED),
      appBar: AppBar(
        leading: IconButton(
          tooltip: 'Quay lại đăng nhập',
          icon: const Icon(Icons.arrow_back),
          onPressed: _submitting ? null : _returnToLogin,
        ),
        title: const Text('Xác minh danh tính'),
        backgroundColor: Colors.transparent,
        foregroundColor: const Color(0xFF2C221E),
        surfaceTintColor: Colors.transparent,
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
          children: [
            const _StepHeader(
              step: 'Bước 2/3',
              title: 'Chụp ảnh và CCCD',
              description:
                  'Ảnh được lưu riêng tư. CompreFace chỉ hỗ trợ so khớp khuôn mặt; quản trị viên vẫn là người quyết định cuối.',
            ),
            const SizedBox(height: 20),
            _EvidenceCard(
              title: 'Ảnh chân dung trực diện',
              hint: 'Không đeo khẩu trang, đủ sáng, chỉ có một khuôn mặt.',
              image: _selfie,
              onCamera: () =>
                  _pick(ExpertEvidenceKind.selfie, ImageSource.camera),
              onGallery: () =>
                  _pick(ExpertEvidenceKind.selfie, ImageSource.gallery),
            ),
            _EvidenceCard(
              title: 'CCCD mặt trước',
              hint: 'Chụp trọn bốn góc, rõ ảnh chân dung và thông tin.',
              image: _front,
              onCamera: () =>
                  _pick(ExpertEvidenceKind.identityFront, ImageSource.camera),
              onGallery: () =>
                  _pick(ExpertEvidenceKind.identityFront, ImageSource.gallery),
            ),
            _EvidenceCard(
              title: 'CCCD mặt sau',
              hint: 'Không lóa sáng, không che mã hoặc ngày cấp.',
              image: _back,
              onCamera: () =>
                  _pick(ExpertEvidenceKind.identityBack, ImageSource.camera),
              onGallery: () =>
                  _pick(ExpertEvidenceKind.identityBack, ImageSource.gallery),
            ),
            if (_verifying || _aiStatus != null)
              _FacePreviewBanner(
                verifying: _verifying,
                status: _aiStatus,
                similarity: _similarity,
                color: _aiColor,
              ),
            if (_error != null) _ErrorBanner(_error!),
            const SizedBox(height: 8),
            SizedBox(
              height: 54,
              child: FilledButton(
                onPressed: _submitting ? null : _submit,
                style: FilledButton.styleFrom(
                  backgroundColor: const Color(0xFFC98C7B),
                  shape: const StadiumBorder(),
                ),
                child: _submitting
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text(
                        'Gửi ảnh xác minh',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FacePreviewBanner extends StatelessWidget {
  const _FacePreviewBanner({
    required this.verifying,
    required this.status,
    required this.similarity,
    required this.color,
  });

  final bool verifying;
  final String? status;
  final double? similarity;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final effectiveColor = color ?? const Color(0xFFC06F5A);
    final similarityText = similarity == null
        ? ''
        : ' • ${(similarity! * 100).clamp(0, 100).toStringAsFixed(1)}%';
    final message = switch (status) {
      'MATCHED' => 'Ảnh khuôn mặt có độ tương đồng phù hợp$similarityText',
      'NOT_MATCHED' => 'Chưa đạt ngưỡng tương đồng$similarityText',
      'NO_FACE' => 'Không tìm thấy khuôn mặt rõ ràng trong ảnh',
      'MULTIPLE_FACES' => 'Ảnh có nhiều hơn một khuôn mặt',
      'DISABLED' || 'RETRYABLE_ERROR' || 'ERROR' =>
        'Chưa thể đối chiếu tự động. Hồ sơ vẫn có thể gửi để xét duyệt.',
      _ => 'Đã nhận kết quả đối chiếu tự động',
    };
    return Container(
      key: const Key('expert-face-preview'),
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: effectiveColor.withValues(alpha: .1),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          if (verifying)
            const SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          else
            Icon(
              status == 'MATCHED'
                  ? Icons.verified_outlined
                  : Icons.info_outline_rounded,
              color: effectiveColor,
            ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              verifying ? 'Đang đối chiếu khuôn mặt…' : message,
              style: TextStyle(color: effectiveColor),
            ),
          ),
        ],
      ),
    );
  }
}

class _EvidenceCard extends StatelessWidget {
  const _EvidenceCard({
    required this.title,
    required this.hint,
    required this.image,
    required this.onCamera,
    required this.onGallery,
  });

  final String title;
  final String hint;
  final ExpertEvidenceImage? image;
  final VoidCallback onCamera;
  final VoidCallback onGallery;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFE8DDD6)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                image == null ? Icons.add_a_photo_outlined : Icons.check_circle,
                color: const Color(0xFFC98C7B),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(hint, style: const TextStyle(color: Color(0xFF75635C))),
          if (image != null) ...[
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(14),
              child: Image.memory(
                Uint8List.fromList(image!.bytes),
                height: 150,
                width: double.infinity,
                fit: BoxFit.cover,
              ),
            ),
          ],
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: onCamera,
                  icon: const Icon(Icons.camera_alt_outlined),
                  label: Text(image == null ? 'Chụp ảnh' : 'Chụp lại'),
                ),
              ),
              const SizedBox(width: 8),
              IconButton.filledTonal(
                tooltip: 'Chọn từ thư viện',
                onPressed: onGallery,
                icon: const Icon(Icons.photo_library_outlined),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _StepHeader extends StatelessWidget {
  const _StepHeader({
    required this.step,
    required this.title,
    required this.description,
  });
  final String step;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        step,
        style: const TextStyle(
          color: Color(0xFFC06F5A),
          fontWeight: FontWeight.w700,
        ),
      ),
      const SizedBox(height: 6),
      Text(
        title,
        style: const TextStyle(
          fontSize: 28,
          fontWeight: FontWeight.w800,
          color: Color(0xFF5A463F),
        ),
      ),
      const SizedBox(height: 8),
      Text(
        description,
        style: const TextStyle(height: 1.5, color: Color(0xFF75635C)),
      ),
    ],
  );
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner(this.message);
  final String message;

  @override
  Widget build(BuildContext context) => Container(
    margin: const EdgeInsets.only(bottom: 12),
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: const Color(0xFFFFDAD6),
      borderRadius: BorderRadius.circular(14),
    ),
    child: Row(
      children: [
        const Icon(Icons.error_outline, color: Color(0xFF93000A)),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            message,
            style: const TextStyle(color: Color(0xFF93000A)),
          ),
        ),
      ],
    ),
  );
}
