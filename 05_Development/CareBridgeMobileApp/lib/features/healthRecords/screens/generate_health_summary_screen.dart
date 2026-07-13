import 'package:flutter/material.dart';
import 'dart:convert';
import '../services/health_summary_service.dart';
import 'share_health_summary_screen.dart';

class GenerateHealthSummaryScreen extends StatefulWidget {
  const GenerateHealthSummaryScreen({super.key});

  @override
  State<GenerateHealthSummaryScreen> createState() =>
      _GenerateHealthSummaryScreenState();
}

class _GenerateHealthSummaryScreenState
    extends State<GenerateHealthSummaryScreen> {
  final _service = HealthSummaryService();
  String _selectedPeriod = '24H'; // 24H, 7D, CONSULTATION
  bool _includeMetrics = true;
  bool _includeLogs = true;
  bool _includeVaccines = false;
  bool _includeRecords = false;
  bool _saveToDevice = false;
  bool _disableAutoShare = false;
  bool _isLoading = false;

  Future<void> _generateSummary() async {
    setState(() => _isLoading = true);
    try {
      final summaryJson = jsonEncode({
        'metrics': _includeMetrics,
        'logs': _includeLogs,
        'vaccines': _includeVaccines,
        'records': _includeRecords,
      });

      final response = await _service.generateSummary(
        _selectedPeriod,
        summaryJson,
      );
      final summaryId = response['summaryId'] as String;

      if (mounted) {
        setState(() => _isLoading = false);
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ShareHealthSummaryScreen(summaryId: summaryId),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi tạo tóm tắt: $e')));

        // Mock navigate for UI testing if backend fails
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) =>
                const ShareHealthSummaryScreen(summaryId: 'mock-summary-123'),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xCCFEF8F4), // Glass effect mock
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Tạo tóm tắt',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 20,
            fontWeight: FontWeight.bold,
            fontFamily: 'Lexend',
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.close, color: Color(0xFF845143)),
            onPressed: () => Navigator.pop(context),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Period Selector
            const Text(
              'Khoảng thời gian',
              style: TextStyle(
                color: Color(0xFF845143),
                fontSize: 18,
                fontWeight: FontWeight.bold,
                fontFamily: 'Lexend',
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                _buildPeriodButton('24h', '24H'),
                const SizedBox(width: 12),
                _buildPeriodButton('7 ngày', '7D'),
                const SizedBox(width: 12),
                _buildPeriodButton('Phiên tư vấn', 'CONSULTATION'),
              ],
            ),

            const SizedBox(height: 32),

            // Categories Selector
            const Text(
              'Hạng mục bao gồm',
              style: TextStyle(
                color: Color(0xFF845143),
                fontSize: 18,
                fontWeight: FontWeight.bold,
                fontFamily: 'Lexend',
              ),
            ),
            const SizedBox(height: 16),
            GridView.count(
              crossAxisCount: 2,
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              childAspectRatio: 2.5,
              children: [
                _buildCategoryToggle(
                  'Chỉ số',
                  Icons.analytics,
                  _includeMetrics,
                  (val) => setState(() => _includeMetrics = val),
                ),
                _buildCategoryToggle(
                  'Nhật ký',
                  Icons.menu_book,
                  _includeLogs,
                  (val) => setState(() => _includeLogs = val),
                ),
                _buildCategoryToggle(
                  'Vaccine',
                  Icons.vaccines,
                  _includeVaccines,
                  (val) => setState(() => _includeVaccines = val),
                ),
                _buildCategoryToggle(
                  'Hồ sơ',
                  Icons.folder_shared,
                  _includeRecords,
                  (val) => setState(() => _includeRecords = val),
                ),
              ],
            ),

            const SizedBox(height: 32),

            // Preview Card
            const Text(
              'Xem trước nội dung',
              style: TextStyle(
                color: Color(0xFF845143),
                fontSize: 18,
                fontWeight: FontWeight.bold,
                fontFamily: 'Lexend',
              ),
            ),
            const SizedBox(height: 16),
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
                border: Border.all(color: Colors.white.withValues(alpha: 0.4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'BÁO CÁO SỨC KHỎE',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 2,
                      color: Color(0x99845143),
                      fontFamily: 'Lexend',
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Bé Mỡ',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF845143),
                          fontFamily: 'Lexend',
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0x0D845143),
                          borderRadius: BorderRadius.circular(999),
                          border: Border.all(color: const Color(0x1A845143)),
                        ),
                        child: const Text(
                          'ID: #CB-2904',
                          style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF845143),
                            fontFamily: 'Lexend',
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  _buildPreviewLine(Icons.favorite, 0.75, 0.5),
                  const SizedBox(height: 16),
                  _buildPreviewLine(Icons.bedtime, 0.66, 0.33),
                  const SizedBox(height: 24),
                  const Divider(color: Color(0x4DC98C7B)),
                  const SizedBox(height: 16),
                  const Text(
                    'Cập nhật: 15:30, Hôm nay',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF524440),
                      fontFamily: 'Lexend',
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 32),

            // Options
            _buildSwitchTile(
              'Lưu vào thiết bị',
              Icons.save,
              _saveToDevice,
              (val) => setState(() => _saveToDevice = val),
            ),
            const SizedBox(height: 16),
            _buildSwitchTile(
              'Không tự động chia sẻ',
              Icons.lock_reset,
              _disableAutoShare,
              (val) => setState(() => _disableAutoShare = val),
            ),

            const SizedBox(height: 40),

            // Action Button
            ElevatedButton.icon(
              onPressed: _isLoading ? null : _generateSummary,
              icon: _isLoading
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                        color: Colors.white,
                        strokeWidth: 2,
                      ),
                    )
                  : const Icon(Icons.auto_awesome),
              label: const Text(
                'Tạo tóm tắt',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  fontFamily: 'Lexend',
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF845143),
                foregroundColor: Colors.white,
                minimumSize: const Size(double.infinity, 56),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(999),
                ),
                elevation: 4,
              ),
            ),

            const SizedBox(height: 48),
          ],
        ),
      ),
    );
  }

  Widget _buildPeriodButton(String label, String value) {
    final isActive = _selectedPeriod == value;
    return Expanded(
      child: GestureDetector(
        onTap: () => setState(() => _selectedPeriod = value),
        child: Container(
          height: 64,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: isActive ? const Color(0xFFC98C7B) : const Color(0xFFF3EDE8),
            borderRadius: BorderRadius.circular(16),
            boxShadow: isActive
                ? const [
                    BoxShadow(
                      color: Color(0x1A000000),
                      offset: Offset(-2, -2),
                      blurRadius: 6,
                    ),
                  ]
                : null,
          ),
          child: Text(
            label,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.bold,
              color: isActive ? Colors.white : const Color(0xFF524440),
              fontFamily: 'Lexend',
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCategoryToggle(
    String label,
    IconData icon,
    bool value,
    Function(bool) onChanged,
  ) {
    return GestureDetector(
      onTap: () => onChanged(!value),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: const [
            BoxShadow(
              color: Color(0x1FC98C7B),
              blurRadius: 16,
              offset: Offset(4, 4),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: const BoxDecoration(
                color: Color(0x1A845143),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: const Color(0xFF845143), size: 16),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                label,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF271812),
                  fontFamily: 'Lexend',
                ),
              ),
            ),
            SizedBox(
              width: 24,
              height: 24,
              child: Checkbox(
                value: value,
                onChanged: (v) => onChanged(v ?? false),
                activeColor: const Color(0xFF845143),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPreviewLine(IconData icon, double width1, double width2) {
    return Row(
      children: [
        Container(
          width: 48,
          height: 48,
          decoration: const BoxDecoration(
            color: Color(0x33C98C7B),
            borderRadius: BorderRadius.all(Radius.circular(16)),
          ),
          child: Icon(icon, color: const Color(0xFF845143)),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              FractionallySizedBox(
                widthFactor: width1,
                child: Container(
                  height: 8,
                  decoration: BoxDecoration(
                    color: const Color(0x33C98C7B),
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
              ),
              const SizedBox(height: 8),
              FractionallySizedBox(
                widthFactor: width2,
                child: Container(
                  height: 8,
                  decoration: BoxDecoration(
                    color: const Color(0x1AC98C7B),
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSwitchTile(
    String title,
    IconData icon,
    bool value,
    Function(bool) onChanged,
  ) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color(0x1FC98C7B),
            blurRadius: 16,
            offset: Offset(4, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Icon(icon, color: const Color(0xFF845143)),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
                color: Color(0xFF271812),
                fontFamily: 'Lexend',
              ),
            ),
          ),
          Switch(
            value: value,
            onChanged: onChanged,
            activeThumbColor: Colors.white,
            activeTrackColor: const Color(0xFFC98C7B),
            inactiveThumbColor: Colors.white,
            inactiveTrackColor: const Color(0xFFD6C2BD),
          ),
        ],
      ),
    );
  }
}
