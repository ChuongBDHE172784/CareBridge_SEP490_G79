import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/emergency_alert_model.dart';
import '../services/emergency_service.dart';

class FamilyAlertDetailScreen extends StatefulWidget {
  final String sessionId;
  final EmergencyAlert? initialAlert;

  const FamilyAlertDetailScreen({
    super.key,
    required this.sessionId,
    this.initialAlert,
  });

  @override
  State<FamilyAlertDetailScreen> createState() =>
      _FamilyAlertDetailScreenState();
}

class _FamilyAlertDetailScreenState extends State<FamilyAlertDetailScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFFFFF);
  static const _surfaceMuted = Color(0xFFF2EAE4);
  static const _accent = Color(0xFFC98C7B);
  static const _accentDark = Color(0xFF845143);
  static const _text = Color(0xFF5A463F);
  static const _textMuted = Color(0xFF9C857C);
  static const _outline = Color(0xFFE8DDD6);
  static const _danger = Color(0xFFC63C49);
  static const _dangerSoft = Color(0xFFFFE2E0);

  final _service = EmergencyService();
  EmergencyAlert? _alert;
  bool _loading = true;
  bool _acknowledging = false;
  String? _errorText;

  bool get _hasLocation =>
      _alert?.latitude != null && _alert?.longitude != null;
  bool get _hasPhone => _alert?.phoneNumber?.trim().isNotEmpty == true;

  @override
  void initState() {
    super.initState();
    final initialAlert = widget.initialAlert;
    if (initialAlert != null) {
      _alert = initialAlert;
      _loading = false;
    } else {
      _load();
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _errorText = null;
    });
    try {
      final alert = await _service.getAlertDetail(widget.sessionId);
      if (!mounted) return;
      setState(() => _alert = alert);
    } catch (error) {
      if (!mounted) return;
      setState(() => _errorText = 'Không thể tải chi tiết cảnh báo: $error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _acknowledge() async {
    if (_acknowledging || _alert?.acknowledged == true) return;
    setState(() => _acknowledging = true);
    try {
      final alert = await _service.acknowledgeAlert(widget.sessionId);
      if (!mounted) return;
      setState(() => _alert = alert);
      _showMessage('Đã ghi nhận bạn tiếp nhận cảnh báo.');
    } catch (error) {
      if (mounted) _showMessage('Chưa thể tiếp nhận cảnh báo: $error');
    } finally {
      if (mounted) setState(() => _acknowledging = false);
    }
  }

  Future<void> _callMother() async {
    final phone = _alert?.phoneNumber?.trim();
    if (phone == null || phone.isEmpty) {
      _showMessage('Mother chưa đăng ký số điện thoại.');
      return;
    }
    try {
      final launched = await launchUrl(
        Uri(scheme: 'tel', path: phone),
        mode: LaunchMode.externalApplication,
      );
      if (!launched && mounted) {
        _showMessage('Không thể mở ứng dụng gọi. Hãy tự nhập số $phone.');
      }
    } catch (_) {
      if (mounted) {
        _showMessage('Không thể mở ứng dụng gọi. Hãy tự nhập số $phone.');
      }
    }
  }

  Future<void> _openDirections() async {
    final alert = _alert;
    if (alert?.latitude == null || alert?.longitude == null) {
      _showMessage('Cảnh báo này không có dữ liệu vị trí.');
      return;
    }
    final uri = Uri.parse(
      'https://www.google.com/maps/dir/?api=1&destination='
      '${alert!.latitude},${alert.longitude}',
    );
    try {
      final launched = await launchUrl(
        uri,
        mode: LaunchMode.externalApplication,
      );
      if (!launched && mounted) {
        _showMessage('Không thể mở ứng dụng bản đồ trên thiết bị.');
      }
    } catch (_) {
      if (mounted) {
        _showMessage('Không thể mở ứng dụng bản đồ trên thiết bị.');
      }
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: _text,
          behavior: SnackBarBehavior.floating,
          shape: const StadiumBorder(),
        ),
      );
  }

  String _formatTime(DateTime value) {
    final local = value.toLocal();
    final difference = DateTime.now().difference(local);
    if (difference.inMinutes < 1) return 'Vừa xong';
    if (difference.inMinutes < 60) return '${difference.inMinutes} phút trước';
    if (difference.inHours < 24) return '${difference.inHours} giờ trước';
    return '${local.hour.toString().padLeft(2, '0')}:'
        '${local.minute.toString().padLeft(2, '0')} · '
        '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/${local.year}';
  }

  String get _alertTitle => switch (_alert?.alertType) {
    'FALL_DETECTED' => 'Phát hiện ngã',
    'SOS' => 'Yêu cầu trợ giúp khẩn cấp',
    _ => 'Cảnh báo khẩn cấp',
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          onPressed: () => Navigator.of(context).pop(_alert?.acknowledged),
          icon: const Icon(Icons.arrow_back, color: _accentDark),
        ),
        title: const Text(
          'Chi tiết cảnh báo',
          style: TextStyle(
            color: _text,
            fontSize: 20,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: _buildBody(),
      bottomNavigationBar: _alert == null || _loading || _errorText != null
          ? null
          : _buildBottomActions(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: _accent));
    }
    if (_errorText != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_rounded, color: _textMuted, size: 48),
              const SizedBox(height: 16),
              Text(
                _errorText!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: _text, fontSize: 16),
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: _load,
                icon: const Icon(Icons.refresh),
                label: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }

    return RefreshIndicator(
      color: _accent,
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 180),
        children: [
          _buildEmergencyHeader(),
          const SizedBox(height: 16),
          _buildContactCard(),
          const SizedBox(height: 16),
          _buildLocationCard(),
          if (_hasLocation) ...[
            const SizedBox(height: 14),
            const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.lock_outline, size: 16, color: _textMuted),
                SizedBox(width: 7),
                Flexible(
                  child: Text(
                    'Vị trí chỉ hiển thị cho thành viên nhóm gia đình.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: _textMuted, fontSize: 13),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildEmergencyHeader() {
    final acknowledged = _alert!.acknowledged;
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: _dangerSoft,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: _danger.withValues(alpha: 0.14)),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 28,
            offset: Offset(0, 10),
          ),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 58,
            height: 58,
            decoration: const BoxDecoration(
              color: _danger,
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.warning_rounded,
              color: Colors.white,
              size: 32,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  acknowledged ? 'ĐÃ TIẾP NHẬN' : 'CẦN KIỂM TRA NGAY',
                  style: TextStyle(
                    color: acknowledged ? _accentDark : _danger,
                    fontSize: 12,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 0.7,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  _alertTitle,
                  style: const TextStyle(
                    color: _text,
                    fontSize: 25,
                    fontWeight: FontWeight.w800,
                    height: 1.15,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    const Icon(Icons.schedule, size: 16, color: _textMuted),
                    const SizedBox(width: 6),
                    Text(
                      _formatTime(_alert!.createdAt),
                      style: const TextStyle(
                        color: _textMuted,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContactCard() {
    final phone = _alert!.phoneNumber?.trim();
    return _card(
      child: Row(
        children: [
          _iconBubble(Icons.person_rounded),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _alert!.personName,
                  style: const TextStyle(
                    color: _text,
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  phone?.isNotEmpty == true ? phone! : 'Chưa có số điện thoại',
                  style: const TextStyle(
                    color: _textMuted,
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          if (_hasPhone)
            IconButton.filled(
              onPressed: _callMother,
              style: IconButton.styleFrom(
                backgroundColor: _accent,
                foregroundColor: Colors.white,
              ),
              icon: const Icon(Icons.call_rounded),
              tooltip: 'Gọi cho mother',
            ),
        ],
      ),
    );
  }

  Widget _buildLocationCard() {
    if (!_hasLocation) {
      return _card(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _iconBubble(Icons.location_off_rounded),
            const SizedBox(width: 14),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Không có vị trí',
                    style: TextStyle(
                      color: _text,
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  SizedBox(height: 6),
                  Text(
                    'Cảnh báo này không thu được tọa độ. Mother cần bật chia sẻ vị trí và cấp quyền cho các cảnh báo mới.',
                    style: TextStyle(
                      color: _textMuted,
                      fontSize: 14,
                      height: 1.45,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    }

    final latitude = _alert!.latitude!;
    final longitude = _alert!.longitude!;
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _iconBubble(Icons.location_on_rounded),
              const SizedBox(width: 14),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Vị trí khi cảnh báo',
                      style: TextStyle(
                        color: _text,
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    SizedBox(height: 3),
                    Text(
                      'Tọa độ do điện thoại mother gửi',
                      style: TextStyle(color: _textMuted, fontSize: 13),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _surfaceMuted,
              borderRadius: BorderRadius.circular(22),
              boxShadow: const [
                BoxShadow(
                  color: Color.fromRGBO(90, 70, 63, 0.05),
                  blurRadius: 8,
                  offset: Offset(0, 4),
                  blurStyle: BlurStyle.inner,
                ),
              ],
            ),
            child: Row(
              children: [
                Expanded(
                  child: _coordinate('VĨ ĐỘ', latitude.toStringAsFixed(6)),
                ),
                Container(width: 1, height: 38, color: _outline),
                Expanded(
                  child: _coordinate('KINH ĐỘ', longitude.toStringAsFixed(6)),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: FilledButton.icon(
              onPressed: _openDirections,
              icon: const Icon(Icons.directions_rounded),
              label: const Text('Mở chỉ đường'),
              style: FilledButton.styleFrom(
                backgroundColor: _surfaceMuted,
                foregroundColor: _accentDark,
                elevation: 0,
                shape: const StadiumBorder(),
                textStyle: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomActions() {
    final acknowledged = _alert!.acknowledged;
    return Container(
      padding: EdgeInsets.fromLTRB(
        20,
        14,
        20,
        14 + MediaQuery.paddingOf(context).bottom,
      ),
      decoration: const BoxDecoration(
        color: _surface,
        border: Border(top: BorderSide(color: _outline)),
        boxShadow: [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.08),
            blurRadius: 26,
            offset: Offset(0, -8),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton.icon(
              onPressed: _hasPhone ? _callMother : null,
              icon: const Icon(Icons.call_rounded),
              label: Text(
                _hasPhone
                    ? 'Gọi ngay cho ${_alert!.personName}'
                    : 'Không có số để gọi',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              style: FilledButton.styleFrom(
                backgroundColor: _danger,
                foregroundColor: Colors.white,
                disabledBackgroundColor: _surfaceMuted,
                disabledForegroundColor: _textMuted,
                shape: const StadiumBorder(),
                elevation: 2,
                textStyle: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: OutlinedButton.icon(
              onPressed: acknowledged || _acknowledging ? null : _acknowledge,
              icon: _acknowledging
                  ? const SizedBox(
                      width: 19,
                      height: 19,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Icon(
                      acknowledged
                          ? Icons.check_circle
                          : Icons.check_circle_outline,
                    ),
              label: Text(
                acknowledged ? 'Đã tiếp nhận cảnh báo' : 'Tiếp nhận cảnh báo',
              ),
              style: OutlinedButton.styleFrom(
                foregroundColor: _accentDark,
                disabledForegroundColor: _accentDark,
                side: const BorderSide(color: _outline),
                shape: const StadiumBorder(),
                textStyle: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _card({required Widget child}) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: _outline.withValues(alpha: 0.7)),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 28,
            offset: Offset(0, 10),
          ),
        ],
      ),
      child: child,
    );
  }

  Widget _iconBubble(IconData icon) {
    return Container(
      width: 46,
      height: 46,
      decoration: BoxDecoration(
        color: _accent.withValues(alpha: 0.16),
        shape: BoxShape.circle,
      ),
      child: Icon(icon, color: _accentDark, size: 24),
    );
  }

  Widget _coordinate(String label, String value) {
    return Column(
      children: [
        Text(
          label,
          style: const TextStyle(
            color: _textMuted,
            fontSize: 11,
            fontWeight: FontWeight.w800,
            letterSpacing: 0.7,
          ),
        ),
        const SizedBox(height: 5),
        Text(
          value,
          style: const TextStyle(
            color: _text,
            fontSize: 15,
            fontWeight: FontWeight.w800,
          ),
        ),
      ],
    );
  }
}
