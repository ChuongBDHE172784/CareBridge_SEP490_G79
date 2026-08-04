import 'package:flutter/material.dart';
import '../services/health_summary_service.dart';

class ShareHealthSummaryScreen extends StatefulWidget {
  final String summaryId;

  const ShareHealthSummaryScreen({super.key, required this.summaryId});

  @override
  State<ShareHealthSummaryScreen> createState() =>
      _ShareHealthSummaryScreenState();
}

class _ShareHealthSummaryScreenState extends State<ShareHealthSummaryScreen> {
  final _service = HealthSummaryService();
  bool _isLoading = false;

  final List<String> _selectedPurposes = ['Khám bệnh'];
  final List<String> _selectedScopes = [
    'Huyết áp',
    'Xét nghiệm máu',
    'Đơn thuốc',
    'Vận động',
  ];

  Future<void> _shareSummary() async {
    setState(() => _isLoading = true);
    try {
      // Mock booking ID since we are just mocking the flow
      const mockBookingId = '00000000-0000-0000-0000-000000000000';

      if (!widget.summaryId.startsWith('mock')) {
        await _service.shareSummary(widget.summaryId, mockBookingId);
      } else {
        await Future.delayed(const Duration(milliseconds: 1000));
      }

      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Chia sẻ dữ liệu thành công!')),
        );
        Navigator.popUntil(context, (route) => route.isFirst);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi chia sẻ: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'CareBridge',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 24,
            fontWeight: FontWeight.bold,
            fontFamily: 'Lexend',
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.close, color: Color(0xFF845143)),
            onPressed: () =>
                Navigator.popUntil(context, (route) => route.isFirst),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Expert Recipient Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(24),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x1FC98C7B),
                    blurRadius: 16,
                    offset: Offset(8, 8),
                  ),
                ],
              ),
              child: Row(
                children: [
                  Stack(
                    children: [
                      Container(
                        width: 64,
                        height: 64,
                        decoration: BoxDecoration(
                          color: const Color(0xFFE9E1DB),
                          borderRadius: BorderRadius.circular(32),
                          border: Border.all(
                            color: const Color(0xFFF3EDE8),
                            width: 4,
                          ),
                        ),
                        child: const Icon(
                          Icons.person,
                          color: Color(0xFF84736F),
                          size: 32,
                        ),
                      ),
                      Positioned(
                        bottom: -2,
                        right: -2,
                        child: Container(
                          padding: const EdgeInsets.all(2),
                          decoration: const BoxDecoration(
                            color: Color(0xFF845143),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.verified,
                            color: Colors.white,
                            size: 14,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(width: 16),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'TS. BS. Nguyễn Minh Thư',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF2D2A28),
                            fontFamily: 'Lexend',
                          ),
                        ),
                        Text(
                          'Bác sĩ chuyên khoa Nội tiết & Tim mạch',
                          style: TextStyle(
                            fontSize: 14,
                            fontStyle: FontStyle.italic,
                            color: Color(0xFF524440),
                            fontFamily: 'Lexend',
                          ),
                        ),
                        SizedBox(height: 8),
                        Row(
                          children: [
                            Icon(
                              Icons.medical_services,
                              color: Color(0xFF845143),
                              size: 14,
                            ),
                            SizedBox(width: 4),
                            Text(
                              'Tư vấn sức khỏe định kỳ',
                              style: TextStyle(
                                fontSize: 12,
                                color: Color(0xFF845143),
                                fontFamily: 'Lexend',
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 32),

            // Scope Selection
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Phạm vi chia sẻ',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF2D2A28),
                    fontFamily: 'Lexend',
                  ),
                ),
                TextButton(
                  onPressed: () {
                    setState(() {
                      if (_selectedScopes.length == 4) {
                        _selectedScopes.clear();
                      } else {
                        _selectedScopes.clear();
                        _selectedScopes.addAll([
                          'Huyết áp',
                          'Xét nghiệm máu',
                          'Đơn thuốc',
                          'Vận động',
                        ]);
                      }
                    });
                  },
                  child: const Text(
                    'Chọn tất cả',
                    style: TextStyle(
                      color: Color(0xFF845143),
                      fontWeight: FontWeight.bold,
                      fontFamily: 'Lexend',
                    ),
                  ),
                ),
              ],
            ),

            const SizedBox(height: 12),

            GridView.count(
              crossAxisCount: 2,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              children: [
                _buildScopeCard(
                  'Huyết áp',
                  '7 ngày gần nhất',
                  Icons.monitor_heart,
                  const Color(0xFFF9EAE1),
                  const Color(0xFF845143),
                ),
                _buildScopeCard(
                  'Xét nghiệm máu',
                  'Kết quả mới nhất',
                  Icons.bloodtype,
                  const Color(0xFFFFDBD1),
                  const Color(0xFF845143),
                ),
                _buildScopeCard(
                  'Đơn thuốc',
                  'Đang sử dụng',
                  Icons.medication,
                  const Color(0xFFEFF6FF),
                  const Color(0xFF2563EB),
                ),
                _buildScopeCard(
                  'Vận động',
                  'Lịch sử 30 ngày',
                  Icons.directions_run,
                  const Color(0xFFF0FDF4),
                  const Color(0xFF16A34A),
                ),
              ],
            ),

            const SizedBox(height: 32),

            // Purpose
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(24),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x1FC98C7B),
                    blurRadius: 16,
                    offset: Offset(8, 8),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Mục đích chia sẻ',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF2D2A28),
                      fontFamily: 'Lexend',
                    ),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      _buildPurposeChip('Khám bệnh'),
                      _buildPurposeChip('Theo dõi'),
                      _buildPurposeChip('Hội chẩn'),
                      _buildPurposeChip('Cá nhân'),
                    ],
                  ),
                  const SizedBox(height: 24),
                  const Text(
                    'Thời gian hết hạn',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF2D2A28),
                      fontFamily: 'Lexend',
                    ),
                  ),
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 12,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF6F1EC),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: const Color(0xFFF2EAE4)),
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          '18:00, 20/05/2024',
                          style: TextStyle(fontSize: 16, fontFamily: 'Lexend'),
                        ),
                        Icon(Icons.event, color: Color(0xFF84736F)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Quyền truy cập sẽ tự động bị thu hồi sau thời điểm này.',
                    style: TextStyle(
                      fontSize: 12,
                      color: Color(0xFF524440),
                      fontFamily: 'Lexend',
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 32),

            // Consent
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: const Color(0xFFF9F2EE),
                borderRadius: BorderRadius.circular(32),
                border: Border.all(
                  color: const Color(0xFFD6C2BD),
                  style: BorderStyle.solid,
                ), // Dart flutter doesn't support dashed easily without package, using solid
              ),
              child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.security, color: Color(0xFF845143)),
                      SizedBox(width: 8),
                      Text(
                        'Cam kết bảo mật',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF845143),
                          fontFamily: 'Lexend',
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 12),
                  Text(
                    '• Chuyên gia chỉ có thể xem, không có quyền chỉnh sửa dữ liệu của bạn.\n• Mọi lượt truy cập đều được ghi lại trong nhật ký hoạt động.\n• Bạn có thể thu hồi quyền bất kỳ lúc nào từ màn hình quản lý.',
                    style: TextStyle(
                      fontSize: 14,
                      color: Color(0xFF524F4C),
                      height: 1.5,
                      fontFamily: 'Lexend',
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 40),

            // CTA Action
            ElevatedButton.icon(
              onPressed: _isLoading ? null : _shareSummary,
              icon: _isLoading
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                        color: Colors.white,
                        strokeWidth: 2,
                      ),
                    )
                  : const Icon(Icons.send),
              label: const Text(
                'Cấp quyền chia sẻ',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  fontFamily: 'Lexend',
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFC98C7B),
                foregroundColor: Colors.white,
                minimumSize: const Size(double.infinity, 56),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(999),
                ),
                elevation: 6,
              ),
            ),
            const SizedBox(height: 16),
            const Center(
              child: Text(
                'Bằng cách nhấn nút, bạn đồng ý với Điều khoản chia sẻ dữ liệu y tế.',
                style: TextStyle(
                  fontSize: 12,
                  color: Color(0xFF524440),
                  fontFamily: 'Lexend',
                ),
                textAlign: TextAlign.center,
              ),
            ),

            const SizedBox(height: 48),
          ],
        ),
      ),
    );
  }

  Widget _buildScopeCard(
    String title,
    String subtitle,
    IconData icon,
    Color iconBgColor,
    Color iconColor,
  ) {
    final isSelected = _selectedScopes.contains(title);
    return GestureDetector(
      onTap: () {
        setState(() {
          if (isSelected) {
            _selectedScopes.remove(title);
          } else {
            _selectedScopes.add(title);
          }
        });
      },
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFFFFDBD1) : Colors.white,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: isSelected ? const Color(0xFF845143) : Colors.transparent,
            width: 2,
          ),
          boxShadow: const [
            BoxShadow(
              color: Color(0x1FC98C7B),
              blurRadius: 16,
              offset: Offset(4, 4),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: iconBgColor,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Icon(icon, color: iconColor),
            ),
            const SizedBox(height: 12),
            Text(
              title,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
                color: isSelected
                    ? const Color(0xFF845143)
                    : const Color(0xFF2D2A28),
                fontFamily: 'Lexend',
              ),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              style: const TextStyle(
                fontSize: 12,
                color: Color(0xFF524440),
                fontFamily: 'Lexend',
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPurposeChip(String label) {
    final isSelected = _selectedPurposes.contains(label);
    return GestureDetector(
      onTap: () {
        setState(() {
          if (isSelected) {
            _selectedPurposes.remove(label);
          } else {
            _selectedPurposes.add(label);
          }
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFF845143) : const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(16),
          boxShadow: const [
            BoxShadow(
              color: Color(0x14C98C7B),
              blurRadius: 6,
              offset: Offset(3, 3),
            ),
          ],
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 14,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
            color: isSelected ? Colors.white : const Color(0xFF524440),
            fontFamily: 'Lexend',
          ),
        ),
      ),
    );
  }
}
