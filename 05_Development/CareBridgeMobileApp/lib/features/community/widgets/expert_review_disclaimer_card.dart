import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import '../models/expert_reviewer_model.dart';

/// CB-181 / UC-225 / Checklist: Medical Content Disclaimer Banner
/// Warm, clinical advisory callout matching CareBridge design palette.
class MedicalContentDisclaimerBanner extends StatelessWidget {
  final String contentType;
  final VoidCallback? onReadMore;

  const MedicalContentDisclaimerBanner({
    super.key,
    this.contentType = 'bài viết',
    this.onReadMore,
  });

  void _showDisclaimerModal(BuildContext context) {
    if (onReadMore != null) {
      onReadMore!();
      return;
    }

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: const Color(0xFFE0D5CE),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFEEE8),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(
                      Icons.shield_outlined,
                      color: Color(0xFF845143),
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Text(
                      'Khuyến cáo y khoa',
                      style: TextStyle(
                        fontFamily: 'Quicksand',
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                        color: Color(0xFF4A3831),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              const Text(
                'Thông tin và sản phẩm gợi ý được cung cấp trên nền tảng chỉ mang tính chất '
                'tham khảo và hỗ trợ nâng cao kiến thức chăm sóc sức khỏe.',
                style: TextStyle(
                  fontFamily: 'Quicksand',
                  fontSize: 14,
                  color: Color(0xFF5A463F),
                  height: 1.5,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 12),
              _buildBulletItem(
                'Nội dung không thể thay thế cho chẩn đoán, điều trị hoặc phác đồ từ bác sĩ chuyên khoa.',
              ),
              _buildBulletItem(
                'Không tự ý sử dụng thuốc, điều chỉnh liều lượng hoặc can thiệp y tế mà chưa có chỉ định chuyên môn.',
              ),
              _buildBulletItem(
                'Trong tình huống cấp cứu hoặc có dấu hiệu chuyển nặng, vui lòng đến ngay cơ sở y tế gần nhất hoặc gọi cấp cứu 115.',
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: FilledButton(
                  onPressed: () => Navigator.of(ctx).pop(),
                  style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFF845143),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  child: const Text(
                    'Đã hiểu',
                    style: TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBulletItem(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '• ',
            style: TextStyle(
              color: Color(0xFFC98C7B),
              fontSize: 15,
              fontWeight: FontWeight.bold,
            ),
          ),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(
                fontFamily: 'Quicksand',
                fontSize: 13.5,
                color: Color(0xFF765F55),
                height: 1.45,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final textPrefix = contentType == 'mục checklist'
        ? 'Thông tin và hướng dẫn trong mục checklist này chỉ mang tính chất tham khảo, vui lòng liên hệ với Bác sĩ, Dược sĩ hoặc chuyên viên y tế để được tư vấn cụ thể. '
        : 'Thông tin và sản phẩm gợi ý trong bài viết chỉ mang tính chất tham khảo, vui lòng liên hệ với Bác sĩ, Dược sĩ hoặc chuyên viên y tế để được tư vấn cụ thể. ';

    return GestureDetector(
      onTap: () => _showDisclaimerModal(context),
      behavior: HitTestBehavior.opaque,
      child: Container(
        decoration: BoxDecoration(
          color: const Color(0xFFFFF9F5),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: const Color(0xFFEADBCE),
            width: 0.8,
          ),
        ),
        clipBehavior: Clip.antiAlias,
        child: IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                width: 4.5,
                color: const Color(0xFFC98C7B),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
                  child: Text.rich(
                    TextSpan(
                      children: [
                        TextSpan(
                          text: textPrefix,
                          style: const TextStyle(
                            fontFamily: 'Quicksand',
                            color: Color(0xFF5A463F),
                            fontSize: 13.5,
                            height: 1.45,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        TextSpan(
                          text: 'Xem thêm',
                          style: const TextStyle(
                            fontFamily: 'Quicksand',
                            color: Color(0xFF845143),
                            fontSize: 13.5,
                            fontWeight: FontWeight.w700,
                            decoration: TextDecoration.underline,
                            decorationColor: Color(0xFF845143),
                          ),
                          recognizer: TapGestureRecognizer()
                            ..onTap = () => _showDisclaimerModal(context),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Expert Reviewer Card
/// Matches CareBridge's warm, clinical design system.
/// Dynamically consumes real database expert reviewer information via [reviewer].
/// Avoids hardcoded avatar images.
class ExpertReviewerCard extends StatelessWidget {
  final ExpertReviewer? reviewer;
  final String? doctorTitle;
  final String? doctorName;
  final String? verificationLabel;
  final String? doctorBio;
  final String? specialty;
  final String? workplace;
  final String? avatarAssetPath;
  final VoidCallback? onMoreInfo;

  const ExpertReviewerCard({
    super.key,
    this.reviewer,
    this.doctorTitle,
    this.doctorName,
    this.verificationLabel,
    this.doctorBio,
    this.specialty,
    this.workplace,
    this.avatarAssetPath,
    this.onMoreInfo,
  });

  String get _resolvedTitle =>
      doctorTitle ??
      reviewer?.professionalTitle ??
      'Bác sĩ';

  String get _resolvedName =>
      doctorName ??
      reviewer?.name ??
      'Nguyễn Huỳnh Kim Ngân';

  String get _resolvedVerification =>
      verificationLabel ??
      reviewer?.verificationStatus ??
      'Đã kiểm duyệt nội dung';

  String get _resolvedSpecialty =>
      specialty ??
      reviewer?.specialty ??
      'Nội khoa tổng quát';

  String get _resolvedWorkplace =>
      workplace ??
      reviewer?.workplace ??
      'Bệnh viện Thống Nhất TP.HCM';

  String get _resolvedBio {
    if (doctorBio != null && doctorBio!.trim().isNotEmpty) {
      return doctorBio!.trim();
    }
    if (reviewer?.bio != null && reviewer!.bio!.trim().isNotEmpty) {
      return reviewer!.bio!.trim();
    }
    if (reviewer != null) {
      final spec = reviewer?.specialty ?? 'chuyên khoa';
      final wp = reviewer?.workplace ?? 'bệnh viện';
      return 'Bác sĩ chuyên môn về $spec với nhiều năm kinh nghiệm thực hành lâm sàng tại $wp. '
          'Bác sĩ tham gia thẩm định và chuẩn hóa các thông tin y khoa, phác đồ và tài liệu hướng dẫn '
          'chăm sóc sức khỏe cộng đồng trên CareBridge.';
    }
    return 'Bác sĩ có nền tảng chuyên môn về Nội khoa cùng kinh nghiệm thực hành lâm sàng tại '
        'Bệnh viện Thống Nhất. Bên cạnh công tác khám chữa bệnh, bác sĩ còn tham gia giảng dạy '
        'và đào tạo sinh viên y khoa. Với định hướng kết hợp giữa thực hành lâm sàng và giáo dục y khoa, '
        'bác sĩ luôn nỗ lực cập nhật kiến thức chuyên môn nhằm mang đến những thông tin sức khỏe chính xác '
        'và hữu ích cho cộng đồng.';
  }

  Widget _buildAvatar({required double size, required double iconSize}) {
    final avatarUrl = reviewer?.avatarUrl?.trim();
    if (avatarUrl != null && avatarUrl.startsWith('http')) {
      return Image.network(
        avatarUrl,
        width: size,
        height: size,
        fit: BoxFit.cover,
        errorBuilder: (context, error, stackTrace) =>
            _buildFallbackAvatar(size: size, iconSize: iconSize),
      );
    }

    if (avatarAssetPath != null && avatarAssetPath!.trim().isNotEmpty) {
      return Image.asset(
        avatarAssetPath!,
        width: size,
        height: size,
        fit: BoxFit.cover,
        errorBuilder: (context, error, stackTrace) =>
            _buildFallbackAvatar(size: size, iconSize: iconSize),
      );
    }

    return _buildFallbackAvatar(size: size, iconSize: iconSize);
  }

  Widget _buildFallbackAvatar({required double size, required double iconSize}) {
    final name = _resolvedName.trim();
    String initials = 'BS';
    if (name.isNotEmpty) {
      final parts = name.split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
      if (parts.length >= 2) {
        initials = '${parts[parts.length - 2][0]}${parts[parts.length - 1][0]}'.toUpperCase();
      } else if (parts.isNotEmpty && parts[0].isNotEmpty) {
        initials = parts[0].substring(0, parts[0].length >= 2 ? 2 : 1).toUpperCase();
      }
    }

    return Container(
      width: size,
      height: size,
      decoration: const BoxDecoration(
        color: Color(0xFFFFEEE8),
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Text(
        initials,
        style: TextStyle(
          fontFamily: 'Quicksand',
          fontSize: iconSize * 0.48,
          fontWeight: FontWeight.w800,
          color: const Color(0xFF845143),
        ),
      ),
    );
  }

  void _showExpertDetailsModal(BuildContext context) {
    if (onMoreInfo != null) {
      onMoreInfo!();
      return;
    }

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: const Color(0xFFE0D5CE),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Container(
                    width: 64,
                    height: 64,
                    decoration: const BoxDecoration(
                      shape: BoxShape.circle,
                      color: Color(0xFFFFEEE8),
                    ),
                    clipBehavior: Clip.antiAlias,
                    child: _buildAvatar(size: 64, iconSize: 36),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _resolvedTitle,
                          style: const TextStyle(
                            fontFamily: 'Quicksand',
                            fontSize: 14,
                            color: Color(0xFF765F55),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          _resolvedName,
                          style: const TextStyle(
                            fontFamily: 'Quicksand',
                            fontSize: 19,
                            fontWeight: FontWeight.w800,
                            color: Color(0xFF4A3831),
                          ),
                        ),
                        const SizedBox(height: 5),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(
                            color: const Color(0xFFE8F5E9),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(
                                Icons.check_circle_rounded,
                                size: 14,
                                color: Color(0xFF2E7D32),
                              ),
                              const SizedBox(width: 4),
                              Text(
                                _resolvedVerification,
                                style: const TextStyle(
                                  fontFamily: 'Quicksand',
                                  fontSize: 12,
                                  color: Color(0xFF2E7D32),
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              const Divider(color: Color(0xFFEADBCE), height: 1),
              const SizedBox(height: 16),
              const Text(
                'Thông tin chuyên môn & Công tác',
                style: TextStyle(
                  fontFamily: 'Quicksand',
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF4A3831),
                ),
              ),
              const SizedBox(height: 12),
              _buildInfoRow('Chuyên khoa:', _resolvedSpecialty),
              _buildInfoRow('Nơi công tác:', _resolvedWorkplace),
              if (reviewer?.approvedAt != null)
                _buildInfoRow(
                  'Thời gian duyệt:',
                  '${reviewer!.approvedAt!.day.toString().padLeft(2, '0')}/'
                  '${reviewer!.approvedAt!.month.toString().padLeft(2, '0')}/'
                  '${reviewer!.approvedAt!.year}',
                ),
              const SizedBox(height: 16),
              const Divider(color: Color(0xFFEADBCE), height: 1),
              const SizedBox(height: 16),
              const Text(
                'Tiểu sử chuyên gia',
                style: TextStyle(
                  fontFamily: 'Quicksand',
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF4A3831),
                ),
              ),
              const SizedBox(height: 8),
              Text(
                _resolvedBio,
                style: const TextStyle(
                  fontFamily: 'Quicksand',
                  fontSize: 13.5,
                  color: Color(0xFF5A463F),
                  height: 1.55,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: FilledButton(
                  onPressed: () => Navigator.of(ctx).pop(),
                  style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFF845143),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  child: const Text(
                    'Đóng',
                    style: TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  static Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              label,
              style: const TextStyle(
                fontFamily: 'Quicksand',
                fontSize: 13.5,
                color: Color(0xFF765F55),
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                fontFamily: 'Quicksand',
                fontSize: 13.5,
                color: Color(0xFF4A3831),
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: const Color(0xFFE7DCD5),
          width: 1.0,
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF4A3831).withValues(alpha: 0.05),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Container(
                width: 56,
                height: 56,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: Color(0xFFFFEEE8),
                ),
                clipBehavior: Clip.antiAlias,
                child: _buildAvatar(size: 56, iconSize: 32),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _resolvedTitle,
                      style: const TextStyle(
                        fontFamily: 'Quicksand',
                        fontSize: 13.5,
                        color: Color(0xFF765F55),
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      _resolvedName,
                      style: const TextStyle(
                        fontFamily: 'Quicksand',
                        fontSize: 17.5,
                        fontWeight: FontWeight.w800,
                        color: Color(0xFF4A3831),
                        height: 1.25,
                      ),
                    ),
                    const SizedBox(height: 5),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: const Color(0xFFE8F5E9),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.check_circle_rounded,
                            size: 14,
                            color: Color(0xFF2E7D32),
                          ),
                          const SizedBox(width: 4),
                          Text(
                            _resolvedVerification,
                            style: const TextStyle(
                              fontFamily: 'Quicksand',
                              fontSize: 12,
                              color: Color(0xFF2E7D32),
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            _resolvedBio,
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 13.5,
              color: Color(0xFF5A463F),
              height: 1.55,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 12),
          InkWell(
            onTap: () => _showExpertDetailsModal(context),
            borderRadius: BorderRadius.circular(8),
            child: const Padding(
              padding: EdgeInsets.symmetric(vertical: 4),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Xem thêm thông tin',
                    style: TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: Color(0xFF845143),
                    ),
                  ),
                  SizedBox(width: 4),
                  Icon(
                    Icons.chevron_right_rounded,
                    size: 18,
                    color: Color(0xFF845143),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
