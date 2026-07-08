enum FileCategory { ultrasound, medicalRecord, photo, other }

extension FileCategoryExtension on FileCategory {
  String get displayLabel {
    switch (this) {
      case FileCategory.ultrasound: return 'Siêu âm';
      case FileCategory.medicalRecord: return 'Hồ sơ bệnh án';
      case FileCategory.photo: return 'Hình ảnh';
      case FileCategory.other: return 'Khác';
    }
  }

  static FileCategory fromLabel(String? v) {
    switch (v) {
      case 'Siêu âm': return FileCategory.ultrasound;
      case 'Hồ sơ bệnh án': return FileCategory.medicalRecord;
      case 'Hình ảnh': return FileCategory.photo;
      default: return FileCategory.other;
    }
  }
}

enum FileOwner { mother, baby }

extension FileOwnerExtension on FileOwner {
  String get displayLabel => this == FileOwner.mother ? 'Mẹ' : 'Bé';
  static FileOwner fromLabel(String? v) => v == 'Mẹ' ? FileOwner.mother : FileOwner.baby;
}

enum FileVisibility { private, shared }

extension FileVisibilityExtension on FileVisibility {
  String get displayLabel => this == FileVisibility.private ? 'Riêng tư' : 'Đã chia sẻ';
}

class UserFile {
  final String fileId;
  final String originalName;
  final String? mimeType;
  final int? fileSizeBytes;
  final DateTime createdAt;
  final FileCategory category;
  final FileOwner owner;
  final FileVisibility visibility;

  const UserFile({
    required this.fileId,
    required this.originalName,
    this.mimeType,
    this.fileSizeBytes,
    required this.createdAt,
    this.category = FileCategory.other,
    this.owner = FileOwner.mother,
    this.visibility = FileVisibility.private,
  });

  factory UserFile.fromJson(Map<String, dynamic> json) {
    return UserFile(
      fileId: json['fileId'] as String,
      originalName: json['originalName'] as String,
      mimeType: json['mimeType'] as String?,
      fileSizeBytes: json['fileSizeBytes'] as int?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  String get sizeLabel {
    if (fileSizeBytes == null) return '';
    final kb = fileSizeBytes! / 1024;
    if (kb < 1024) return '${kb.toStringAsFixed(1)} KB';
    return '${(kb / 1024).toStringAsFixed(1)} MB';
  }

  String get dateLabel {
    final d = createdAt.day.toString().padLeft(2, '0');
    final m = createdAt.month.toString().padLeft(2, '0');
    final y = createdAt.year;
    return '$d/$m/$y';
  }
}

// UC-168: View file response from GET /api/v1/files/{fileId}
class ViewFileResponse {
  final String fileId;
  final String originalName;
  final String? mimeType;
  final int? fileSizeBytes;
  final String? presignedUrl;
  final String? status;
  final DateTime? createdAt;

  const ViewFileResponse({
    required this.fileId,
    required this.originalName,
    this.mimeType,
    this.fileSizeBytes,
    this.presignedUrl,
    this.status,
    this.createdAt,
  });

  factory ViewFileResponse.fromJson(Map<String, dynamic> json) {
    return ViewFileResponse(
      fileId: json['fileId']?.toString() ?? '',
      originalName: json['originalName'] as String? ?? '',
      mimeType: json['mimeType'] as String?,
      fileSizeBytes: json['fileSizeBytes'] as int?,
      presignedUrl: json['presignedUrl'] as String?,
      status: json['status'] as String?,
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt'] as String) : null,
    );
  }

  bool get isPdf => mimeType == 'application/pdf' || originalName.toLowerCase().endsWith('.pdf');
  bool get isImage => mimeType?.startsWith('image/') == true;

  String get sizeLabel {
    if (fileSizeBytes == null) return '';
    final kb = fileSizeBytes! / 1024;
    if (kb < 1024) return '${kb.toStringAsFixed(1)} KB';
    return '${(kb / 1024).toStringAsFixed(1)} MB';
  }

  String get dateLabel {
    if (createdAt == null) return '';
    final d = createdAt!.day.toString().padLeft(2, '0');
    final m = createdAt!.month.toString().padLeft(2, '0');
    final y = createdAt!.year;
    return '$d/$m/$y';
  }
}
