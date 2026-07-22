import 'package:flutter/foundation.dart';

class ContributionAttachment {
  const ContributionAttachment({
    required this.id,
    required this.fileId,
    required this.contributionId,
    required this.kind,
    required this.purpose,
    required this.accessMode,
    required this.displayOrder,
    this.originalName,
    required this.mimeType,
    required this.fileSizeBytes,
    this.presignedUrl,
  });

  final String id;
  final String fileId;
  final String contributionId;
  final String kind;
  final String purpose;
  final String accessMode;
  final int displayOrder;
  final String? originalName;
  final String mimeType;
  final int fileSizeBytes;
  final String? presignedUrl;

  factory ContributionAttachment.fromJson(Map<String, dynamic> json) {
    return ContributionAttachment(
      id: json['id'] as String,
      fileId: json['fileId'] as String,
      contributionId: json['contributionId'] as String,
      kind: json['kind'] as String,
      purpose: json['purpose'] as String,
      accessMode: json['accessMode'] as String,
      displayOrder: (json['displayOrder'] as num).toInt(),
      originalName: json['originalName'] as String?,
      mimeType: json['mimeType'] as String,
      fileSizeBytes: (json['fileSizeBytes'] as num).toInt(),
      presignedUrl: json['presignedUrl'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'fileId': fileId,
      'contributionId': contributionId,
      'kind': kind,
      'purpose': purpose,
      'accessMode': accessMode,
      'displayOrder': displayOrder,
      'originalName': originalName,
      'mimeType': mimeType,
      'fileSizeBytes': fileSizeBytes,
      'presignedUrl': presignedUrl,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ContributionAttachment &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          fileId == other.fileId &&
          contributionId == other.contributionId &&
          kind == other.kind &&
          purpose == other.purpose &&
          accessMode == other.accessMode &&
          displayOrder == other.displayOrder &&
          originalName == other.originalName &&
          mimeType == other.mimeType &&
          fileSizeBytes == other.fileSizeBytes &&
          presignedUrl == other.presignedUrl;

  @override
  int get hashCode => Object.hash(
        id,
        fileId,
        contributionId,
        kind,
        purpose,
        accessMode,
        displayOrder,
        originalName,
        mimeType,
        fileSizeBytes,
        presignedUrl,
      );

  @override
  String toString() => 'ContributionAttachment(id: $id, fileId: $fileId, contributionId: $contributionId, kind: $kind, purpose: $purpose, accessMode: $accessMode, displayOrder: $displayOrder, originalName: $originalName, mimeType: $mimeType, fileSizeBytes: $fileSizeBytes, presignedUrl: $presignedUrl)';
}

class Contribution {
  const Contribution({
    required this.id,
    required this.expertUserId,
    required this.title,
    required this.content,
    this.specialtyId,
    this.hospitalId,
    required this.status,
    this.rejectionReason,
    required this.version,
    required this.createdAt,
    required this.updatedAt,
    this.attachments,
  });

  final String id;
  final String expertUserId;
  final String title;
  final String content;
  final String? specialtyId;
  final String? hospitalId;
  final String status;
  final String? rejectionReason;
  final int version;
  final String createdAt;
  final String updatedAt;
  final List<ContributionAttachment>? attachments;

  factory Contribution.fromJson(Map<String, dynamic> json) {
    return Contribution(
      id: json['id'] as String,
      expertUserId: json['expertUserId'] as String,
      title: json['title'] as String,
      content: json['content'] as String,
      specialtyId: json['specialtyId'] as String?,
      hospitalId: json['hospitalId'] as String?,
      status: json['status'] as String,
      rejectionReason: json['rejectionReason'] as String?,
      version: (json['version'] as num).toInt(),
      createdAt: json['createdAt'] as String,
      updatedAt: json['updatedAt'] as String,
      attachments: (json['attachments'] as List<dynamic>?)
          ?.map((e) => ContributionAttachment.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'expertUserId': expertUserId,
      'title': title,
      'content': content,
      'specialtyId': specialtyId,
      'hospitalId': hospitalId,
      'status': status,
      'rejectionReason': rejectionReason,
      'version': version,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
      'attachments': attachments?.map((e) => e.toJson()).toList(),
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Contribution &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          expertUserId == other.expertUserId &&
          title == other.title &&
          content == other.content &&
          specialtyId == other.specialtyId &&
          hospitalId == other.hospitalId &&
          status == other.status &&
          rejectionReason == other.rejectionReason &&
          version == other.version &&
          createdAt == other.createdAt &&
          updatedAt == other.updatedAt &&
          listEquals(attachments, other.attachments);

  @override
  int get hashCode => Object.hash(
        id,
        expertUserId,
        title,
        content,
        specialtyId,
        hospitalId,
        status,
        rejectionReason,
        version,
        createdAt,
        updatedAt,
        attachments,
      );

  @override
  String toString() => 'Contribution(id: $id, expertUserId: $expertUserId, title: $title, content: $content, specialtyId: $specialtyId, hospitalId: $hospitalId, status: $status, rejectionReason: $rejectionReason, version: $version, createdAt: $createdAt, updatedAt: $updatedAt, attachments: $attachments)';
}

class PaginatedContributions {
  const PaginatedContributions({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final List<Contribution> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  factory PaginatedContributions.fromJson(Map<String, dynamic> json) {
    return PaginatedContributions(
      content: (json['content'] as List<dynamic>)
          .map((e) => Contribution.fromJson(e as Map<String, dynamic>))
          .toList(),
      page: (json['page'] as num).toInt(),
      size: (json['size'] as num).toInt(),
      totalElements: (json['totalElements'] as num).toInt(),
      totalPages: (json['totalPages'] as num).toInt(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'content': content.map((e) => e.toJson()).toList(),
      'page': page,
      'size': size,
      'totalElements': totalElements,
      'totalPages': totalPages,
    };
  }
}

class CreateContributionRequest {
  const CreateContributionRequest({
    required this.title,
    required this.content,
    this.specialtyId,
    this.hospitalId,
    this.attachments,
  });

  final String title;
  final String content;
  final String? specialtyId;
  final String? hospitalId;
  final List<AttachmentRequest>? attachments;

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'content': content,
      'specialtyId': specialtyId,
      'hospitalId': hospitalId,
      'attachments': attachments?.map((e) => e.toJson()).toList(),
    };
  }
}

class AttachmentRequest {
  const AttachmentRequest({
    required this.fileId,
    required this.kind,
    required this.purpose,
    required this.accessMode,
    this.displayOrder,
  });

  final String fileId;
  final String kind;
  final String purpose;
  final String accessMode;
  final int? displayOrder;

  Map<String, dynamic> toJson() {
    return {
      'fileId': fileId,
      'kind': kind,
      'purpose': purpose,
      'accessMode': accessMode,
      'displayOrder': displayOrder,
    };
  }
}

class UpdateContributionRequest {
  const UpdateContributionRequest({
    required this.title,
    required this.content,
    this.specialtyId,
    this.hospitalId,
    this.attachments,
  });

  final String title;
  final String content;
  final String? specialtyId;
  final String? hospitalId;
  final List<AttachmentRequest>? attachments;

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'content': content,
      'specialtyId': specialtyId,
      'hospitalId': hospitalId,
      'attachments': attachments?.map((e) => e.toJson()).toList(),
    };
  }
}

enum ContributionStatus {
  draft,
  submitted,
  approved,
  rejected;

  static ContributionStatus fromString(String value) {
    switch (value.toUpperCase()) {
      case 'DRAFT':
        return ContributionStatus.draft;
      case 'SUBMITTED':
        return ContributionStatus.submitted;
      case 'APPROVED':
        return ContributionStatus.approved;
      case 'REJECTED':
        return ContributionStatus.rejected;
      default:
        return ContributionStatus.draft;
    }
  }

  String get label {
    switch (this) {
      case ContributionStatus.draft:
        return 'Bản nháp';
      case ContributionStatus.submitted:
        return 'Đã gửi duyệt';
      case ContributionStatus.approved:
        return 'Đã duyệt';
      case ContributionStatus.rejected:
        return 'Từ chối';
    }
  }
}