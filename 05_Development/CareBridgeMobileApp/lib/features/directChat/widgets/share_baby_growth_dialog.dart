import 'package:flutter/material.dart';
import '../../baby/models/baby_model.dart';
import '../../baby/services/baby_profile_selection_storage.dart';
import '../../baby/services/baby_service.dart';
import '../../healthRecords/models/growth_measurement_model.dart';
import '../../healthRecords/services/growth_measurement_service.dart';
import 'baby_growth_message_card.dart';

/// Bottom sheet that lets a mother pick a baby and share all of its growth
/// trends in a direct conversation (ShareBabyGrowthInDirectChat UD-04).
class ShareBabyGrowthDialog extends StatefulWidget {
  const ShareBabyGrowthDialog({
    super.key,
    this.babyLoader,
    this.lastOpenedBabyIdReader,
    this.measurementsLoader,
  });

  final Future<List<BabyProfile>> Function()? babyLoader;
  final Future<String?> Function()? lastOpenedBabyIdReader;
  final GrowthMeasurementsLoader? measurementsLoader;

  static Future<BabyGrowthShareData?> show(
    BuildContext context, {
    Future<List<BabyProfile>> Function()? babyLoader,
    Future<String?> Function()? lastOpenedBabyIdReader,
    GrowthMeasurementsLoader? measurementsLoader,
  }) {
    return showModalBottomSheet<BabyGrowthShareData>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => ShareBabyGrowthDialog(
        babyLoader: babyLoader,
        lastOpenedBabyIdReader: lastOpenedBabyIdReader,
        measurementsLoader: measurementsLoader,
      ),
    );
  }

  @override
  State<ShareBabyGrowthDialog> createState() => _ShareBabyGrowthDialogState();
}

class _ShareBabyGrowthDialogState extends State<ShareBabyGrowthDialog> {
  static const _primary = Color(0xFFC98C7B);
  static const _textDark = Color(0xFF2C2523);
  static const _textMuted = Color(0xFF7A6F6C);

  final TextEditingController _noteController = TextEditingController();
  bool _loadingBabies = true;
  List<BabyProfile> _babies = const [];
  String? _selectedBabyId;
  List<GrowthMeasurement>? _measurements;
  bool _loadingMeasurements = false;
  int _measurementRequest = 0;

  @override
  void initState() {
    super.initState();
    _init();
  }

  @override
  void dispose() {
    _noteController.dispose();
    super.dispose();
  }

  Future<void> _init() async {
    var babies = <BabyProfile>[];
    try {
      babies = await (widget.babyLoader ?? BabyService().listBabyProfiles)();
    } catch (_) {}
    String? lastOpenedId;
    try {
      lastOpenedId = await (widget.lastOpenedBabyIdReader ??
          BabyProfileSelectionStorage().readLastOpenedBabyProfileId)();
    } catch (_) {}
    if (!mounted) return;

    final selected = babies.any((b) => b.id == lastOpenedId)
        ? lastOpenedId
        : (babies.isNotEmpty ? babies.first.id : null);
    setState(() {
      _babies = babies;
      _loadingBabies = false;
    });
    if (selected != null) _selectBaby(selected);
  }

  Future<void> _selectBaby(String babyId) async {
    final request = ++_measurementRequest;
    setState(() {
      _selectedBabyId = babyId;
      _measurements = null;
      _loadingMeasurements = true;
    });
    final loader =
        widget.measurementsLoader ??
        GrowthMeasurementService().getGrowthChartMeasurements;
    List<GrowthMeasurement>? result;
    try {
      result = List<GrowthMeasurement>.from(await loader(babyId))
        ..sort((a, b) => a.measuredAt.compareTo(b.measuredAt));
    } catch (_) {
      result = null;
    }
    if (!mounted || request != _measurementRequest) return;
    setState(() {
      _measurements = result;
      _loadingMeasurements = false;
    });
  }

  void _onConfirm() {
    final babyId = _selectedBabyId;
    if (babyId == null) return;
    final baby = _babies.firstWhere((b) => b.id == babyId);
    final measurements = _measurements ?? const <GrowthMeasurement>[];
    final note = _noteController.text.trim();
    final data = BabyGrowthShareData(
      babyId: baby.id,
      babyNickname: baby.nickname,
      birthDate: baby.birthDate,
      measurementCount: measurements.length,
      latest: measurements.isEmpty
          ? null
          : BabyGrowthLatestSnapshot.fromMeasurement(measurements.last),
      note: note.isEmpty ? null : note,
    );
    if (!BabyGrowthShareData.fitsMessageLimit(data.serialize())) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Ghi chú quá dài, vui lòng rút gọn.')),
      );
      return;
    }
    Navigator.of(context).pop(data);
  }

  @override
  Widget build(BuildContext context) {
    final canSend = _selectedBabyId != null && !_loadingMeasurements;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
        left: 20,
        right: 20,
        top: 20,
      ),
      child: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.grey.shade300,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              _buildHeader(),
              const SizedBox(height: 16),
              if (_loadingBabies)
                const Padding(
                  padding: EdgeInsets.all(24),
                  child: Center(
                    child: CircularProgressIndicator(color: _primary),
                  ),
                )
              else if (_babies.isEmpty)
                _buildEmptyBabies()
              else ...[
                ..._babies.map(_buildBabyOption),
                const SizedBox(height: 8),
                _buildPreview(),
                const SizedBox(height: 12),
                _buildNoteField(),
              ],
              const SizedBox(height: 16),
              FilledButton.icon(
                key: const Key('share-baby-growth-send'),
                onPressed: canSend ? _onConfirm : null,
                icon: const Icon(Icons.send_rounded, size: 18),
                label: const Text(
                  'Gửi cho chuyên gia',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                ),
                style: FilledButton.styleFrom(
                  backgroundColor: _primary,
                  minimumSize: const Size.fromHeight(48),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: _primary.withValues(alpha: 0.15),
            borderRadius: BorderRadius.circular(10),
          ),
          child: const Icon(Icons.child_care_rounded, color: _primary, size: 22),
        ),
        const SizedBox(width: 12),
        const Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Chia sẻ phát triển của bé',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 17,
                  fontWeight: FontWeight.bold,
                  color: _textDark,
                ),
              ),
              Text(
                'Toàn bộ biểu đồ cân nặng, chiều cao, vòng đầu',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: _textMuted,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyBabies() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFFAF7F6),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFE8D5CE)),
      ),
      child: const Column(
        children: [
          Text(
            'Chưa có hồ sơ bé',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: _textDark,
            ),
          ),
          SizedBox(height: 4),
          Text(
            'Hãy tạo hồ sơ bé và ghi nhận số đo để chia sẻ.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              color: _textMuted,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBabyOption(BabyProfile baby) {
    final selected = baby.id == _selectedBabyId;
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Material(
        color: selected ? _primary.withValues(alpha: 0.1) : const Color(0xFFFAF7F6),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(
            color: selected ? _primary : const Color(0xFFE8D5CE),
          ),
        ),
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: selected ? null : () => _selectBaby(baby.id),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: Row(
              children: [
                Icon(
                  selected
                      ? Icons.radio_button_checked_rounded
                      : Icons.radio_button_unchecked_rounded,
                  color: selected ? _primary : const Color(0xFF9E8E8A),
                  size: 20,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    baby.nickname,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: _textDark,
                    ),
                  ),
                ),
                Text(
                  baby.ageLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    color: _textMuted,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPreview() {
    if (_loadingMeasurements) {
      return const LinearProgressIndicator(color: _primary, minHeight: 2);
    }
    final measurements = _measurements;
    final String text;
    if (measurements == null) {
      text = 'Chưa tải được số đo — chuyên gia vẫn xem được dữ liệu mới nhất.';
    } else if (measurements.isEmpty) {
      text = 'Bé chưa có lần đo nào.';
    } else {
      final latest = measurements.last;
      final date =
          '${latest.measuredAt.day.toString().padLeft(2, '0')}/${latest.measuredAt.month.toString().padLeft(2, '0')}/${latest.measuredAt.year}';
      text = '${measurements.length} lần đo · Gần nhất $date';
    }
    return Text(
      text,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 12,
        color: _textMuted,
      ),
    );
  }

  Widget _buildNoteField() {
    return TextField(
      controller: _noteController,
      maxLines: 2,
      maxLength: BabyGrowthShareData.maxNoteLength,
      style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
      decoration: InputDecoration(
        hintText: 'Thêm câu hỏi hoặc ghi chú cho chuyên gia (tùy chọn)...',
        hintStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 12,
          color: Color(0xFF9E8E8A),
        ),
        contentPadding: const EdgeInsets.all(12),
        filled: true,
        fillColor: const Color(0xFFFAF7F6),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFE8D5CE)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFE8D5CE)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: _primary, width: 1.5),
        ),
      ),
    );
  }
}
