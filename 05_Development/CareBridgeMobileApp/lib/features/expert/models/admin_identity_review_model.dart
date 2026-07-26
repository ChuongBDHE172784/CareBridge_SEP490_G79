class AdminIdentityReviewItem {
  final String identityVerificationId;
  final String expertProfileId;
  final String? selfieFileId;
  final String? identityFrontFileId;
  final String? identityBackFileId;
  final String? selfieCropFileId;
  final String? idCardCropFileId;
  final String? faceStatus;
  final double? faceSimilarity;
  final double? faceThreshold;
  final String? providerErrorCode;
  final String? reviewReason;
  final String reviewStatus;
  final String createdAt;

  const AdminIdentityReviewItem({
    required this.identityVerificationId,
    required this.expertProfileId,
    this.selfieFileId,
    this.identityFrontFileId,
    this.identityBackFileId,
    this.selfieCropFileId,
    this.idCardCropFileId,
    this.faceStatus,
    this.faceSimilarity,
    this.faceThreshold,
    this.providerErrorCode,
    this.reviewReason,
    required this.reviewStatus,
    required this.createdAt,
  });
}
