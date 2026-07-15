import 'package:flutter/material.dart';
import '../models/growth_measurement_model.dart';
import '../services/growth_measurement_service.dart';
import 'growth_measurement_detail_screen.dart';

class GrowthMeasurementHistoryScreen extends StatefulWidget {
  final String babyId;
  const GrowthMeasurementHistoryScreen({super.key, required this.babyId});

  @override
  State<GrowthMeasurementHistoryScreen> createState() =>
      _GrowthMeasurementHistoryScreenState();
}

class _GrowthMeasurementHistoryScreenState
    extends State<GrowthMeasurementHistoryScreen> {
  final _service = GrowthMeasurementService();
  bool _isLoading = true;
  List<GrowthMeasurement> _records = [];
  String _selectedTab = 'Tất cả';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final records = await _service.getGrowthHistory(widget.babyId);
      if (mounted) {
        setState(() {
          _records = records;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: const Key('growth-history-screen'),
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Lịch sử đo lường',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: CircleAvatar(
              backgroundColor: Colors.grey[200],
              backgroundImage: const NetworkImage(
                'https://lh3.googleusercontent.com/aida-public/AB6AXuAfIClE2XrhchB2YXUkxFhAgxyNB_KbnEEMYJ4bx0o5HUbpNys1-ji6CyZ5aWHqhu3JGN8u8GaSCe4rVuqhYMcKH51eLp5ldXo3u0DNdTmslCM9E-ZiehGW0INPsFz2BdM8cC49wt0bMy2Hd2l4efLVevsxb0e1Ap5dLZGaDMteb5V9Yk4GZQJeHW4XmmFXFCVckYCNM2wvz4UG2ZZRm4O2rSlUNHGNBCptOBaXxWOlpnTZc5DV2faJg_uuFgv71Y2vkyhfxvgahQ0',
              ),
            ),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
            )
          : RefreshIndicator(
              color: const Color(0xFFC98C7B),
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildTabs(),
                  const SizedBox(height: 24),
                  _buildChartCard(),
                  const SizedBox(height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Lịch sử ghi nhận',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF2D2A28),
                        ),
                      ),
                      TextButton(
                        onPressed: () {},
                        child: const Text(
                          'Sắp xếp',
                          style: TextStyle(
                            color: Color(0xFF845143),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  if (_records.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(24.0),
                      child: Center(
                        child: Text(
                          'Chưa có dữ liệu',
                          style: TextStyle(color: Colors.grey),
                        ),
                      ),
                    )
                  else
                    ..._records.map((r) => _buildRecordCard(r)),
                ],
              ),
            ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: const Color(0xFFC98C7B),
        onPressed: () {
          // TODO: Navigate to Add Growth Measurement Screen (not yet created but we can use detail screen as edit, wait we need an add form)
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Tính năng đang phát triển')),
          );
        },
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  Widget _buildTabs() {
    final tabs = ['Tất cả', 'Chiều cao', 'Cân nặng', 'Vòng đầu'];
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: tabs.map((t) {
          final isSelected = t == _selectedTab;
          return Padding(
            padding: const EdgeInsets.only(right: 8.0),
            child: ChoiceChip(
              label: Text(t),
              selected: isSelected,
              selectedColor: const Color(0xFFC98C7B),
              backgroundColor: Colors.white,
              labelStyle: TextStyle(
                color: isSelected ? Colors.white : const Color(0xFF524F4C),
                fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
              ),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(999),
                side: BorderSide(
                  color: isSelected
                      ? Colors.transparent
                      : const Color(0xFFE7E1DD),
                ),
              ),
              onSelected: (val) {
                if (val) setState(() => _selectedTab = t);
              },
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildChartCard() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color(0x14C98C7B),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Biểu đồ tăng trưởng',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF2D2A28),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.fullscreen, color: Color(0xFF524F4C)),
                onPressed: () {},
              ),
            ],
          ),
          const SizedBox(height: 16),
          Container(
            height: 192,
            width: double.infinity,
            decoration: BoxDecoration(
              color: const Color(0xFFF6F1EC),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: const Color(0xFFE7E1DD)),
            ),
            child: const Center(
              child: Text(
                'Chart Visualization Area',
                style: TextStyle(color: Colors.grey),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _buildLegend(const Color(0xFFC98C7B), 'Chỉ số bé'),
              const SizedBox(width: 24),
              _buildLegend(const Color(0xFFCCC5C0), 'Tiêu chuẩn WHO'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildLegend(Color color, String label) {
    return Row(
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 8),
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.bold,
            color: Color(0xFF524F4C),
          ),
        ),
      ],
    );
  }

  Widget _buildRecordCard(GrowthMeasurement record) {
    final dateStr =
        '${record.measuredAt.day.toString().padLeft(2, '0')} Th${record.measuredAt.month}, ${record.measuredAt.year}';
    final ageStr = record.ageInMonths != null
        ? '${record.ageInMonths} tháng tuổi'
        : '';

    return GestureDetector(
      onTap: () async {
        final changed = await Navigator.of(context).push<bool>(
          MaterialPageRoute(
            builder: (_) => GrowthMeasurementDetailScreen(
              babyId: widget.babyId,
              measurement: record,
            ),
          ),
        );
        if (changed == true && mounted) await _loadData();
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 16),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(24),
          boxShadow: const [
            BoxShadow(
              color: Color(0x14C98C7B),
              blurRadius: 20,
              offset: Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: const BoxDecoration(
                        color: Color(0xFFF2EAE4),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.calendar_month,
                        color: Color(0xFF845143),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          dateStr,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF2D2A28),
                          ),
                        ),
                        if (ageStr.isNotEmpty)
                          Text(
                            ageStr,
                            style: const TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFF524F4C),
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
                const Icon(Icons.more_vert, color: Color(0xFF9E9A96)),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: _buildStatBox(
                    Icons.scale,
                    record.weightKg?.toStringAsFixed(1) ?? '--',
                    'kg',
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _buildStatBox(
                    Icons.height,
                    record.heightCm?.toStringAsFixed(1) ?? '--',
                    'cm',
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _buildStatBox(
                    Icons.face,
                    record.headCircumferenceCm?.toStringAsFixed(1) ?? '--',
                    'cm',
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatBox(IconData icon, String value, String unit) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF6F1EC),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        children: [
          Icon(icon, color: const Color(0xFF605E5A), size: 20),
          const SizedBox(height: 4),
          Text(
            value,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Color(0xFF2D2A28),
            ),
          ),
          Text(
            unit,
            style: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: Color(0xFF524F4C),
            ),
          ),
        ],
      ),
    );
  }
}
