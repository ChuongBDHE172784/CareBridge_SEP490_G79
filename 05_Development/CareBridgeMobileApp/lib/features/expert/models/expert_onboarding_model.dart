enum ExpertOnboardingStep {
  profile,
  expertType,
  identity,
  credential,
  review,
  contract,
  availability,
  complete,
}

/// Nhóm chuyên gia — xem docs/expert-two-tier-flow.md.
/// null = chưa chọn hình thức; PENDING_CONTRACT hiển thị như cộng đồng cho tới khi ký.
enum ExpertKind { community, pendingContract, contracted }

ExpertKind? expertKindFromName(String? value) {
  switch ((value ?? '').toUpperCase()) {
    case 'COMMUNITY':
      return ExpertKind.community;
    case 'PENDING_CONTRACT':
      return ExpertKind.pendingContract;
    case 'CONTRACTED':
      return ExpertKind.contracted;
    default:
      return null;
  }
}

class ExpertOnboardingState {
  final bool profileComplete;
  final bool identityComplete;
  final bool credentialComplete;
  final String verificationStatus;
  final String identityStatus;
  final String credentialStatus;
  final String? rejectionReason;
  final ExpertKind? expertType;
  final ExpertOnboardingStep nextStep;

  const ExpertOnboardingState({
    required this.profileComplete,
    required this.identityComplete,
    required this.credentialComplete,
    required this.verificationStatus,
    required this.identityStatus,
    required this.credentialStatus,
    required this.nextStep,
    this.rejectionReason,
    this.expertType,
  });

  bool get approved => verificationStatus == 'APPROVED';
  bool get isContracted => expertType == ExpertKind.contracted;
  bool get awaitingContract => expertType == ExpertKind.pendingContract;
  bool get rejected => verificationStatus == 'REJECTED';

  factory ExpertOnboardingState.fromJson(Map<String, dynamic> json) {
    final root = json['data'] is Map<String, dynamic>
        ? json['data'] as Map<String, dynamic>
        : json;
    final profile = root['profile'];
    final identity = root['identity'] is Map<String, dynamic>
        ? root['identity'] as Map<String, dynamic>
        : const <String, dynamic>{};
    final credentials = root['credentials'];
    final verification = root['verification'] is Map<String, dynamic>
        ? root['verification'] as Map<String, dynamic>
        : const <String, dynamic>{};

    final profileComplete =
        root['profileComplete'] == true ||
        root['profileExists'] == true ||
        root['hasProfile'] == true ||
        profile != null;
    final rawIdentityStatus =
        root['identityStatus'] ??
        identity['reviewStatus'] ??
        identity['status'];
    final identityStatus = (rawIdentityStatus ?? 'NOT_SUBMITTED')
        .toString()
        .toUpperCase();
    const incompleteStatuses = {
      '',
      'NOT_SUBMITTED',
      'REQUIRED',
      'MISSING',
      'REJECTED',
      'RETRYABLE',
      'RETRYABLE_ERROR',
    };
    final identityComplete = rawIdentityStatus != null
        ? !incompleteStatuses.contains(identityStatus)
        : root['identityComplete'] == true || identity['submittedAt'] != null;
    final credentialStatus = (root['credentialStatus'] ?? '')
        .toString()
        .toUpperCase();
    final credentialComplete = credentialStatus.isNotEmpty
        ? !incompleteStatuses.contains(credentialStatus)
        : root['credentialComplete'] == true ||
              root['hasCredential'] == true ||
              (credentials is List && credentials.isNotEmpty);
    final status =
        (root['verificationStatus'] ??
                verification['status'] ??
                root['status'] ??
                'PENDING')
            .toString()
            .toUpperCase();

    // Backend cũ chưa biết hai nhóm chuyên gia thì KHÔNG có key này (Jackson để mặc định
    // ALWAYS nên backend mới luôn gửi, kể cả khi giá trị null). Phân biệt bằng sự hiện
    // diện của key để mobile cập nhật trước backend vẫn chạy đúng thứ tự bước cũ.
    final twoTierAware = root.containsKey('expertType');
    final expertType = expertKindFromName(root['expertType']?.toString());
    final explicit = _parseStep(root['nextStep']?.toString());
    final calculated = status == 'APPROVED'
        ? (expertType == ExpertKind.pendingContract
              ? ExpertOnboardingStep.contract
              : ExpertOnboardingStep.complete)
        : !profileComplete
        ? ExpertOnboardingStep.profile
        : (twoTierAware && expertType == null)
        ? ExpertOnboardingStep.expertType
        : !identityComplete
        ? ExpertOnboardingStep.identity
        : !credentialComplete
        ? ExpertOnboardingStep.credential
        : ExpertOnboardingStep.review;

    return ExpertOnboardingState(
      profileComplete: profileComplete,
      identityComplete: identityComplete,
      credentialComplete: credentialComplete,
      verificationStatus: status,
      identityStatus: identityStatus,
      credentialStatus: credentialStatus.isEmpty
          ? (credentialComplete ? 'PENDING' : 'NOT_SUBMITTED')
          : credentialStatus,
      expertType: expertType,
      rejectionReason: _firstNonBlank([
        root['rejectionReason'],
        verification['rejectionReason'],
        identity['reviewReason'],
        root['latestIdentityAttempt'] is Map
            ? (root['latestIdentityAttempt'] as Map)['reviewReason']
            : null,
      ]),
      nextStep: explicit ?? calculated,
    );
  }

  static ExpertOnboardingStep? _parseStep(String? value) {
    switch ((value ?? '').toUpperCase()) {
      case 'PROFILE':
      case 'CREATE_PROFILE':
        return ExpertOnboardingStep.profile;
      case 'IDENTITY':
      case 'SUBMIT_IDENTITY':
        return ExpertOnboardingStep.identity;
      case 'CREDENTIAL':
      case 'CREDENTIALS':
      case 'SUBMIT_CREDENTIAL':
        return ExpertOnboardingStep.credential;
      case 'REVIEW':
      case 'UNDER_REVIEW':
      case 'WAIT_FOR_REVIEW':
      case 'MANUAL_REVIEW_REQUIRED':
        return ExpertOnboardingStep.review;
      case 'EXPERT_TYPE':
      case 'CHOOSE_TYPE':
        return ExpertOnboardingStep.expertType;
      case 'CONTRACT':
      case 'SIGN_CONTRACT':
        return ExpertOnboardingStep.contract;
      case 'AVAILABILITY':
      case 'CALENDAR':
        return ExpertOnboardingStep.availability;
      case 'COMPLETE':
      case 'APPROVED':
        return ExpertOnboardingStep.complete;
      default:
        return null;
    }
  }

  static String? _firstNonBlank(List<dynamic> values) {
    for (final value in values) {
      final text = value?.toString().trim();
      if (text != null && text.isNotEmpty) return text;
    }
    return null;
  }
}

class ExpertEvidenceImage {
  final List<int> bytes;
  final String fileName;
  final String mimeType;

  const ExpertEvidenceImage({
    required this.bytes,
    required this.fileName,
    required this.mimeType,
  });
}

/// Bản đề nghị Thoả thuận hợp tác trả từ `GET /api/v1/expert/contract/offer`.
class ExpertContractOffer {
  final String termsVersion;
  final String termsHash;
  final String content;
  final String contractNumber;
  final String issuedDate;
  final int termMonths;
  final int minSlotsPerWeek;

  /// Họ tên trên hồ sơ đã duyệt — trang ký yêu cầu gõ lại đúng chuỗi này.
  final String expectedFullName;
  final bool alreadyAccepted;

  const ExpertContractOffer({
    required this.termsVersion,
    required this.termsHash,
    required this.content,
    this.contractNumber = '',
    this.issuedDate = '',
    this.termMonths = 12,
    this.minSlotsPerWeek = 10,
    this.expectedFullName = '',
    this.alreadyAccepted = false,
  });

  factory ExpertContractOffer.fromJson(Map<String, dynamic> json) {
    return ExpertContractOffer(
      termsVersion: json['termsVersion']?.toString() ?? '',
      termsHash: json['termsHash']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      contractNumber: json['contractNumber']?.toString() ?? '',
      issuedDate: json['issuedDate']?.toString() ?? '',
      termMonths: (json['termMonths'] as num?)?.toInt() ?? 12,
      minSlotsPerWeek: (json['minSlotsPerWeek'] as num?)?.toInt() ?? 10,
      expectedFullName: json['expectedFullName']?.toString() ?? '',
      alreadyAccepted: json['alreadyAccepted'] == true,
    );
  }
}
