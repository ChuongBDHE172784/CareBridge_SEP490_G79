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
      // CompreFace downscales to IMG_LENGTH_LIMIT=640 before detection, so the
      // extra pixels never reach the model — they only cost upload time on
      // mobile data. 1600/85 still leaves the cropped face well above what
      // verification needs.
      maxWidth: 1600,
      imageQuality: 85,
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
