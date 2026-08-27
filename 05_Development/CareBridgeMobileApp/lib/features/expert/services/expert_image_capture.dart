import 'package:image_picker/image_picker.dart';
import 'package:mime/mime.dart';

import '../models/expert_onboarding_model.dart';

enum ExpertEvidenceKind { selfie, identityFront, identityBack, credential }

abstract class ExpertImageCapture {
  Future<ExpertEvidenceImage?> capture(
    ExpertEvidenceKind kind, {
    required ImageSource source,
  });
}

class ImagePickerExpertCapture implements ExpertImageCapture {
  final ImagePicker picker;

  ImagePickerExpertCapture({ImagePicker? picker}) : picker = picker ?? ImagePicker();

  @override
  Future<ExpertEvidenceImage?> capture(
    ExpertEvidenceKind kind, {
    required ImageSource source,
  }) async {
    final picked = await picker.pickImage(
      source: source,
      preferredCameraDevice: kind == ExpertEvidenceKind.selfie
          ? CameraDevice.front
          : CameraDevice.rear,
      maxWidth: 2400,
      imageQuality: 90,
    );
    if (picked == null) return null;
    final bytes = await picked.readAsBytes();
    return ExpertEvidenceImage(
      bytes: bytes,
      fileName: picked.name,
      mimeType: lookupMimeType(picked.name, headerBytes: bytes) ?? 'image/jpeg',
    );
  }
}
