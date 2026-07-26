enum ExpertOnboardingStep { profile, identity, credential, review, complete }

class ExpertOnboardingState {
  final bool profileComplete;
  final bool identityComplete;
  final bool credentialComplete;
  final String verificationStatus;
  final String identityStatus;
  final String? rejectionReason;
  final ExpertOnboardingStep nextStep;

  const ExpertOnboardingState({
    required this.profileComplete,
    required this.identityComplete,
    required this.credentialComplete,
    required this.verificationStatus,
    required this.identityStatus,
    required this.nextStep,
    this.rejectionReason,
  });

  bool get approved => verificationStatus == 'APPROVED';
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

    final explicit = _parseStep(root['nextStep']?.toString());
    final calculated = status == 'APPROVED'
        ? ExpertOnboardingStep.complete
        : !profileComplete
        ? ExpertOnboardingStep.profile
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
