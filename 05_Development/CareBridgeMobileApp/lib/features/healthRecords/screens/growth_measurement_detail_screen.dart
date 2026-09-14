import 'package:flutter/material.dart';
import '../models/growth_measurement_model.dart';
import '../services/growth_measurement_service.dart';
import 'growth_measurement_form_screen.dart';

class GrowthMeasurementDetailScreen extends StatefulWidget {
  final String babyId;
  final GrowthMeasurement measurement;

  const GrowthMeasurementDetailScreen({
    super.key,
    required this.babyId,
    required this.measurement,
  });

  @override
  State<GrowthMeasurementDetailScreen> createState() =>
      _GrowthMeasurementDetailScreenState();
}

class _GrowthMeasurementDetailScreenState
    extends State<GrowthMeasurementDetailScreen> {
  final _service = GrowthMeasurementService();
  bool _isLoading = false;

  Future<void> _delete() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xác nhận xóa'),
        content: const Text(
          'Bạn có chắc chắn muốn xóa bản ghi này? Hành động này không thể hoàn tác.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Xóa', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    setState(() => _isLoading = true);
    try {
      await _service.deleteGrowthMeasurement(
        widget.babyId,
        widget.measurement.id,
      );
      if (mounted) {
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _edit() async {
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => GrowthMeasurementFormScreen(
          babyId: widget.babyId,
          measurement: widget.measurement,
        ),
      ),
    );
    if (changed == true && mounted) Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    final m = widget.measurement;
    final dateStr =
        '${m.measuredAt.day.toString().padLeft(2, '0')}/${m.measuredAt.month.toString().padLeft(2, '0')}/${m.measuredAt.year}';
    final DateTime? recordedTime =
        (m.measuredAt.hour != 0 || m.measuredAt.minute != 0)
            ? m.measuredAt
            : m.createdAt;
    final timeStr = recordedTime != null
        ? '${recordedTime.hour.toString().padLeft(2, '0')}:${recordedTime.minute.toString().padLeft(2, '0')}'
        : '--:--';

    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Chi tiết đo lường',
          style: TextStyle(
            fontFamily: 'Lexend',
            color: Color(0xFF845143),
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.all(32),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.9),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.2),
                    ),
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
                      // Featured Value
                      Container(
                        width: 80,
                        height: 80,
                        decoration: const BoxDecoration(
                          color: Color(0xFFF2EAE4),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          m.heightCm != null
                              ? Icons.straighten
                              : (m.weightKg != null
                                  ? Icons.scale
                                  : (m.headCircumferenceCm != null
                                      ? Icons.face
                                      : Icons.straighten)),
                          color: const Color(0xFF845143),
                          size: 40,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.baseline,
                        textBaseline: TextBaseline.alphabetic,
                        children: [
                          Text(
                            m.heightCm != null
                                ? m.heightCm!.toStringAsFixed(1)
                                : (m.weightKg != null
                                    ? m.weightKg!.toStringAsFixed(1)
                                    : (m.headCircumferenceCm != null
                                        ? m.headCircumferenceCm!
                                            .toStringAsFixed(1)
                                        : '--')),
                            style: const TextStyle(
                              fontSize: 48,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF845143),
                            ),
                          ),
                          const SizedBox(width: 4),
                          Text(
                            m.heightCm != null
                                ? 'cm'
                                : (m.weightKg != null
                                    ? 'kg'
                                    : (m.headCircumferenceCm != null
                                        ? 'cm'
                                        : '')),
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF524440),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        m.heightCm != null
                            ? 'Chiều cao ghi nhận'
                            : (m.weightKg != null
                                ? 'Cân nặng ghi nhận'
                                : (m.headCircumferenceCm != null
                                    ? 'Vòng đầu ghi nhận'
                                    : 'Chỉ số đo')),
                        style: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF625D59),
                        ),
                      ),

                      const SizedBox(height: 24),
                      const Divider(color: Color(0xFFF2EAE4)),
                      const SizedBox(height: 24),

                      // Details list
                      _buildDetailRow(
                        Icons.calendar_today,
                        'NGÀY GHI NHẬN',
                        dateStr,
                      ),
                      const SizedBox(height: 16),
                      _buildDetailRow(Icons.schedule, 'THỜI GIAN', timeStr),
                      const SizedBox(height: 16),

                      if (m.heightCm != null &&
                          (m.weightKg != null ||
                              m.headCircumferenceCm != null)) ...[
                        _buildDetailRow(
                          Icons.straighten,
                          'CHIỀU CAO',
                          '${m.heightCm!.toStringAsFixed(1)} cm',
                        ),
                        const SizedBox(height: 16),
                      ],
                      if (m.weightKg != null) ...[
                        _buildDetailRow(
                          Icons.scale,
                          'CÂN NẶNG',
                          '${m.weightKg!.toStringAsFixed(1)} kg',
                        ),
                        const SizedBox(height: 16),
                      ],
                      if (m.headCircumferenceCm != null) ...[
                        _buildDetailRow(
                          Icons.face,
                          'VÒNG ĐẦU',
                          '${m.headCircumferenceCm!.toStringAsFixed(1)} cm',
                        ),
                        const SizedBox(height: 16),
                      ],
                      if (m.note != null && m.note!.isNotEmpty) ...[
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Icon(
                              Icons.edit_note,
                              color: Color(0xFF84736F),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Text(
                                    'GHI CHÚ',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF84736F),
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Container(
                                    padding: const EdgeInsets.all(12),
                                    decoration: BoxDecoration(
                                      color: const Color(0xFFF9F2EE),
                                      borderRadius: BorderRadius.circular(8),
                                      border: Border.all(
                                        color: const Color(0xFFE7E1DD),
                                      ),
                                    ),
                                    child: Text(
                                      m.note!,
                                      style: const TextStyle(
                                        fontSize: 14,
                                        color: Color(0xFF1D1B19),
                                        height: 1.5,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),

                const SizedBox(height: 24),
                ElevatedButton.icon(
                  key: const Key('growth-edit-button'),
                  onPressed: _isLoading ? null : _edit,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF845143),
                    foregroundColor: Colors.white,
                    minimumSize: const Size(double.infinity, 56),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  icon: const Icon(Icons.edit, size: 20),
                  label: const Text(
                    'CHỈNH SỬA THÔNG TIN',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 0.5,
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: _isLoading ? null : _delete,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFBA1A1A),
                    side: const BorderSide(color: Color(0x33BA1A1A)),
                    minimumSize: const Size(double.infinity, 56),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  icon: const Icon(Icons.delete, size: 20),
                  label: const Text(
                    'XÓA BẢN GHI NÀY',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 0.5,
                    ),
                  ),
                ),
                const SizedBox(height: 24),
              ],
            ),
          ),
          if (_isLoading)
            Container(
              color: Colors.black.withValues(alpha: 0.3),
              child: const Center(
                child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(IconData icon, String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: const Color(0xFF84736F)),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: Color(0xFF84736F),
              ),
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: const TextStyle(fontSize: 16, color: Color(0xFF1D1B19)),
            ),
          ],
        ),
      ],
    );
  }
}
