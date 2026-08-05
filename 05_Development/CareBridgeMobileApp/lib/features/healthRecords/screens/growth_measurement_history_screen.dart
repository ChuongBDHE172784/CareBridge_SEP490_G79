import 'dart:async';

import 'package:flutter/material.dart';
import '../../baby/models/baby_model.dart';
import '../../baby/services/baby_service.dart';
import '../models/growth_measurement_model.dart';
import '../services/growth_measurement_service.dart';
import '../widgets/growth_trend_chart.dart';
import 'growth_measurement_detail_screen.dart';
import 'growth_measurement_form_screen.dart';

class GrowthMeasurementHistoryScreen extends StatefulWidget {
  final String babyId;
  final Future<List<GrowthMeasurement>> Function(String babyId)? historyLoader;
  final Future<BabyProfile> Function(String babyId)? profileLoader;
  final bool loadAvatarImage;

  const GrowthMeasurementHistoryScreen({
    super.key,
    required this.babyId,
    this.historyLoader,
    this.profileLoader,
    this.loadAvatarImage = true,
  });

  @override
  State<GrowthMeasurementHistoryScreen> createState() =>
      _GrowthMeasurementHistoryScreenState();
}

class _GrowthMeasurementHistoryScreenState
    extends State<GrowthMeasurementHistoryScreen> {
  final _service = GrowthMeasurementService();
  final _babyService = BabyService();
  bool _isLoading = true;
  List<GrowthMeasurement> _records = [];
  BabyProfile? _babyProfile;
  bool _profileLoadFailed = false;
  String _selectedTab = 'Tất cả';
  int _loadGeneration = 0;
  Future<void>? _profileLoadFuture;
  bool _isOpeningFullscreen = false;
  bool _isOpeningAdd = false;
  final ScrollController _historyScrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _historyScrollController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    final generation = ++_loadGeneration;
    setState(() => _isLoading = true);
    final profileLoadFuture = _loadProfile(generation);
    _profileLoadFuture = profileLoadFuture;
    unawaited(profileLoadFuture);
    try {
      final records =
          await (widget.historyLoader?.call(widget.babyId) ??
              _service.getGrowthHistory(widget.babyId));
      if (mounted && generation == _loadGeneration) {
        setState(() {
          _records = records;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted && generation == _loadGeneration) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  Future<void> _loadProfile(int generation) async {
    try {
      final profile =
          await (widget.profileLoader?.call(widget.babyId) ??
              _babyService.getBabyProfile(widget.babyId));
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _babyProfile = profile;
        _profileLoadFailed = false;
      });
    } catch (_) {
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _babyProfile = null;
        _profileLoadFailed = true;
      });
    }
  }

  Future<void> _openAddForm() async {
    if (_isOpeningAdd) return;
    _isOpeningAdd = true;
    try {
      final changed = await Navigator.of(context).push<bool>(
        MaterialPageRoute(
          builder: (_) => GrowthMeasurementFormScreen(babyId: widget.babyId),
        ),
      );
      if (changed == true && mounted) await _loadData();
    } finally {
      _isOpeningAdd = false;
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
              backgroundImage: widget.loadAvatarImage
                  ? const NetworkImage(
                      'https://lh3.googleusercontent.com/aida-public/AB6AXuAfIClE2XrhchB2YXUkxFhAgxyNB_KbnEEMYJ4bx0o5HUbpNys1-ji6CyZ5aWHqhu3JGN8u8GaSCe4rVuqhYMcKH51eLp5ldXo3u0DNdTmslCM9E-ZiehGW0INPsFz2BdM8cC49wt0bMy2Hd2l4efLVevsxb0e1Ap5dLZGaDMteb5V9Yk4GZQJeHW4XmmFXFCVckYCNM2wvz4UG2ZZRm4O2rSlUNHGNBCptOBaXxWOlpnTZc5DV2faJg_uuFgv71Y2vkyhfxvgahQ0',
                    )
                  : null,
              child: widget.loadAvatarImage
                  ? null
                  : const Icon(Icons.child_care, color: Color(0xFF845143)),
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
                  _buildChartCard(),
                  const SizedBox(height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Expanded(
                        child: Text(
                          'Lịch sử ghi nhận',
                          maxLines: 2,
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF2D2A28),
                          ),
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
                    _buildHistoryList(),
                ],
              ),
            ),
      floatingActionButton: FloatingActionButton(
        key: const Key('growth-add-button'),
        tooltip: 'Thêm số đo',
        backgroundColor: const Color(0xFFC98C7B),
        onPressed: _openAddForm,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  Widget _buildTabs({
    required String selectedTab,
    required ValueChanged<String> onSelected,
  }) {
    return _GrowthMetricSelector(
      selectedTab: selectedTab,
      onSelected: onSelected,
    );
  }

  Widget _buildChartCard() {
    return Container(
      key: const Key('growth-chart-card'),
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
              const Expanded(
                child: Text(
                  'Biểu đồ tăng trưởng',
                  maxLines: 2,
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF2D2A28),
                  ),
                ),
              ),
              IconButton(
                key: const Key('growth-chart-fullscreen-button'),
                icon: const Icon(Icons.fullscreen, color: Color(0xFF524F4C)),
                tooltip: 'Mở biểu đồ toàn màn hình',
                onPressed: _openFullscreenChart,
              ),
            ],
          ),
          _buildTabs(
            selectedTab: _selectedTab,
            onSelected: (tab) => setState(() => _selectedTab = tab),
          ),
          const SizedBox(height: 16),
          GrowthTrendChart(
            measurements: _records,
            metric: _metricForTab(_selectedTab),
            birthDate: _babyProfile?.birthDate,
            gender: _babyProfile?.gender,
            profileLoadFailed: _profileLoadFailed,
          ),
        ],
      ),
    );
  }

  Future<void> _openFullscreenChart() async {
    if (_isOpeningFullscreen) return;
    _isOpeningFullscreen = true;
    try {
      while (true) {
        final pendingProfileLoad = _profileLoadFuture;
        if (pendingProfileLoad == null) break;
        await pendingProfileLoad;
        if (identical(pendingProfileLoad, _profileLoadFuture)) break;
      }
      if (!mounted) return;

      await Navigator.of(context).push<void>(
        MaterialPageRoute(
          builder: (_) => _FullscreenGrowthChartScreen(
            measurements: _records,
            birthDate: _babyProfile?.birthDate,
            gender: _babyProfile?.gender,
            profileLoadFailed: _profileLoadFailed,
            initialTab: _selectedTab,
            onMetricChanged: (tab) {
              if (mounted) setState(() => _selectedTab = tab);
            },
          ),
        ),
      );
    } finally {
      _isOpeningFullscreen = false;
    }
  }

  Widget _buildHistoryList() {
    return ConstrainedBox(
      constraints: const BoxConstraints(maxHeight: 480),
      child: Scrollbar(
        key: const Key('growth-history-scrollbar'),
        controller: _historyScrollController,
        thumbVisibility: true,
        child: ListView.builder(
          key: const Key('growth-history-record-list'),
          controller: _historyScrollController,
          primary: false,
          shrinkWrap: true,
          itemCount: _records.length,
          itemBuilder: (context, index) => _buildRecordCard(_records[index]),
        ),
      ),
    );
  }

  Widget _buildRecordCard(GrowthMeasurement record) {
    final dateStr =
        '${record.measuredAt.day.toString().padLeft(2, '0')} Th${record.measuredAt.month}, ${record.measuredAt.year}';
    final ageStr = record.ageInMonths != null
        ? '${record.ageInMonths} tháng tuổi'
        : '';

    return GestureDetector(
      key: ValueKey('growth-history-record-${record.id}'),
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

GrowthTrendMetric _metricForTab(String tab) {
  return switch (tab) {
    'Chiều cao' => GrowthTrendMetric.height,
    'Cân nặng' => GrowthTrendMetric.weight,
    'Vòng đầu' => GrowthTrendMetric.headCircumference,
    _ => GrowthTrendMetric.automatic,
  };
}

class _GrowthMetricSelector extends StatelessWidget {
  static const _tabs = ['Tất cả', 'Chiều cao', 'Cân nặng', 'Vòng đầu'];

  final String selectedTab;
  final ValueChanged<String> onSelected;

  const _GrowthMetricSelector({
    required this.selectedTab,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      key: const Key('growth-chart-filters'),
      scrollDirection: Axis.horizontal,
      child: Row(
        children: _tabs
            .map((tab) {
              final isSelected = tab == selectedTab;
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(tab),
                  selected: isSelected,
                  selectedColor: const Color(0xFFC98C7B),
                  backgroundColor: const Color(0xFFF2EAE4),
                  labelStyle: TextStyle(
                    color: isSelected ? Colors.white : const Color(0xFF5A463F),
                    fontWeight: isSelected ? FontWeight.bold : FontWeight.w600,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(999),
                    side: BorderSide(
                      color: isSelected
                          ? Colors.transparent
                          : const Color(0xFFE7E1DD),
                    ),
                  ),
                  onSelected: (selected) {
                    if (selected) onSelected(tab);
                  },
                ),
              );
            })
            .toList(growable: false),
      ),
    );
  }
}

class _FullscreenGrowthChartScreen extends StatefulWidget {
  final List<GrowthMeasurement> measurements;
  final DateTime? birthDate;
  final BabyGender? gender;
  final bool profileLoadFailed;
  final String initialTab;
  final ValueChanged<String> onMetricChanged;

  const _FullscreenGrowthChartScreen({
    required this.measurements,
    required this.birthDate,
    required this.gender,
    required this.profileLoadFailed,
    required this.initialTab,
    required this.onMetricChanged,
  });

  @override
  State<_FullscreenGrowthChartScreen> createState() =>
      _FullscreenGrowthChartScreenState();
}

class _FullscreenGrowthChartScreenState
    extends State<_FullscreenGrowthChartScreen> {
  late String _selectedTab;

  @override
  void initState() {
    super.initState();
    _selectedTab = widget.initialTab;
  }

  void _selectMetric(String tab) {
    setState(() => _selectedTab = tab);
    widget.onMetricChanged(tab);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: const Key('growth-chart-fullscreen-screen'),
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: const Color(0xFFF6F1EC),
        elevation: 0,
        leading: IconButton(
          key: const Key('growth-chart-fullscreen-close'),
          tooltip: 'Đóng biểu đồ toàn màn hình',
          onPressed: () => Navigator.of(context).pop(),
          icon: const Icon(Icons.close, color: Color(0xFF5A463F)),
        ),
        title: const Text(
          'Biểu đồ tăng trưởng',
          style: TextStyle(
            color: Color(0xFF5A463F),
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
          child: Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: const Color(0x80E8DDD6)),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 32,
                  offset: Offset(0, 12),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _GrowthMetricSelector(
                  selectedTab: _selectedTab,
                  onSelected: _selectMetric,
                ),
                const SizedBox(height: 20),
                GrowthTrendChart(
                  measurements: widget.measurements,
                  metric: _metricForTab(_selectedTab),
                  birthDate: widget.birthDate,
                  gender: widget.gender,
                  profileLoadFailed: widget.profileLoadFailed,
                  chartHeight: 360,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
