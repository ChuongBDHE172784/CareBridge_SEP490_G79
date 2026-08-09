import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';
import 'dart:convert';

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODEL
// ─────────────────────────────────────────────────────────────────────────────

enum HealthDataType { heartRate, spo2, stress }

class HealthData {
  final HealthDataType type;
  final int value;
  final DateTime timestamp;
  final String deviceName;

  HealthData({
    required this.type,
    required this.value,
    required this.timestamp,
    required this.deviceName,
  });

  factory HealthData.fromMap(Map<dynamic, dynamic> map) {
    final typeStr = map['type'] as String? ?? '';
    final type = _parseType(typeStr);
    return HealthData(
      type: type,
      value: map['value'] as int? ?? 0,
      timestamp: DateTime.fromMillisecondsSinceEpoch(
        (map['timestamp'] as int?) ?? DateTime.now().millisecondsSinceEpoch,
      ),
      deviceName: map['deviceName'] as String? ?? 'Unknown Device',
    );
  }

  static HealthDataType _parseType(String type) {
    switch (type) {
      case 'HEART_RATE':
        return HealthDataType.heartRate;
      case 'SPO2':
        return HealthDataType.spo2;
      case 'STRESS':
        return HealthDataType.stress;
      default:
        return HealthDataType.heartRate;
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// HEALTH DATA SERVICE — EventChannel bridge
// ─────────────────────────────────────────────────────────────────────────────

class HealthDataService {
  static const _channel = EventChannel(
    'com.carebridge.healthtest/health_stream',
  );

  Stream<HealthData>? _stream;

  Stream<HealthData> get stream {
    _stream ??= _channel.receiveBroadcastStream().map((event) {
      debugPrint('[HealthTest] Raw event: $event');
      return HealthData.fromMap(event as Map<dynamic, dynamic>);
    });
    return _stream!;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN
// ─────────────────────────────────────────────────────────────────────────────

void main() {
  runApp(const HealthTestApp());
}

class HealthTestApp extends StatelessWidget {
  const HealthTestApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Health Test',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF6C63FF),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
        fontFamily: 'SF Pro Display',
      ),
      home: const HealthDashboard(),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD
// ─────────────────────────────────────────────────────────────────────────────

class HealthDashboard extends StatefulWidget {
  const HealthDashboard({super.key});

  @override
  State<HealthDashboard> createState() => _HealthDashboardState();
}

class _HealthDashboardState extends State<HealthDashboard>
    with TickerProviderStateMixin {

  final _service = HealthDataService();
  StreamSubscription<HealthData>? _subscription;

  // Latest values (null = not yet received)
  int? _heartRate;
  int? _spo2;
  int? _stress;
  DateTime? _heartRateTime;
  DateTime? _spo2Time;
  DateTime? _stressTime;
  String _deviceName = '–';

  // Animation controllers for pulse effect on update
  late AnimationController _hrPulse;
  late AnimationController _spo2Pulse;
  late AnimationController _stressPulse;

  @override
  void initState() {
    super.initState();
    _hrPulse    = AnimationController(vsync: this, duration: const Duration(milliseconds: 400));
    _spo2Pulse  = AnimationController(vsync: this, duration: const Duration(milliseconds: 400));
    _stressPulse = AnimationController(vsync: this, duration: const Duration(milliseconds: 400));
    _startListening();
  }

  void _startListening() {
    _subscription = _service.stream.listen(
      (data) {
        debugPrint('[HealthTest] Received ${data.type.name}: ${data.value}');
        setState(() {
          _deviceName = data.deviceName;
          switch (data.type) {
            case HealthDataType.heartRate:
              _heartRate     = data.value;
              _heartRateTime = data.timestamp;
              _hrPulse.forward(from: 0);
              break;
            case HealthDataType.spo2:
              _spo2     = data.value;
              _spo2Time = data.timestamp;
              _spo2Pulse.forward(from: 0);
              break;
            case HealthDataType.stress:
              _stress     = data.value;
              _stressTime = data.timestamp;
              _stressPulse.forward(from: 0);
              break;
          }
        });
      },
      onError: (error) {
        debugPrint('[HealthTest] Stream error: $error');
      },
    );
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _hrPulse.dispose();
    _spo2Pulse.dispose();
    _stressPulse.dispose();
    super.dispose();
  }

  String _formatTime(DateTime? dt) {
    if (dt == null) return '–';
    return '${dt.hour.toString().padLeft(2, '0')}:'
        '${dt.minute.toString().padLeft(2, '0')}:'
        '${dt.second.toString().padLeft(2, '0')}';
  }

  String _stressLabel(int? value) {
    if (value == null) return '';
    if (value < 40) return 'Relaxed';
    if (value < 60) return 'Mild';
    if (value < 80) return 'Moderate';
    return 'High';
  }

  Color _stressColor(int? value) {
    if (value == null) return const Color(0xFF6C63FF);
    if (value < 40) return const Color(0xFF4CAF50);
    if (value < 60) return const Color(0xFFFFEB3B);
    if (value < 80) return const Color(0xFFFF9800);
    return const Color(0xFFF44336);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              _buildHeader(),
              const SizedBox(height: 8),
              // Device info
              _buildDeviceChip(),
              const SizedBox(height: 28),
              // Health cards
              _buildHeartRateCard(),
              const SizedBox(height: 16),
              _buildSpo2Card(),
              const SizedBox(height: 16),
              _buildStressCard(),
              const SizedBox(height: 24),
              // Connection status
              _buildStatusBar(),
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
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF6C63FF), Color(0xFF9C8FFF)],
            ),
            borderRadius: BorderRadius.circular(12),
          ),
          child: const Icon(Icons.monitor_heart_rounded, color: Colors.white, size: 24),
        ),
        const SizedBox(width: 14),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Health Test',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Colors.white,
                letterSpacing: -0.5,
              ),
            ),
            Text(
              'CareBridge × Gadgetbridge',
              style: TextStyle(
                fontSize: 12,
                color: Colors.white.withOpacity(0.5),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildDeviceChip() {
    final hasDevice = _heartRate != null || _spo2 != null || _stress != null;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: hasDevice
            ? const Color(0xFF1A3A2A)
            : const Color(0xFF1A1A2E),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: hasDevice
              ? const Color(0xFF4CAF50).withOpacity(0.4)
              : Colors.white.withOpacity(0.1),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: hasDevice ? const Color(0xFF4CAF50) : Colors.grey,
            ),
          ),
          const SizedBox(width: 8),
          Text(
            hasDevice ? _deviceName : 'Waiting for Gadgetbridge...',
            style: TextStyle(
              fontSize: 12,
              color: hasDevice
                  ? const Color(0xFF4CAF50)
                  : Colors.white.withOpacity(0.5),
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeartRateCard() {
    return _HealthCard(
      pulseController: _hrPulse,
      icon: '❤️',
      label: 'Heart Rate',
      value: _heartRate != null ? '${_heartRate}' : '––',
      unit: 'BPM',
      updatedAt: _formatTime(_heartRateTime),
      accentColor: const Color(0xFFFF4B6E),
      gradientColors: const [Color(0xFF2D1A2A), Color(0xFF1A0D1A)],
      hasData: _heartRate != null,
    );
  }

  Widget _buildSpo2Card() {
    return _HealthCard(
      pulseController: _spo2Pulse,
      icon: '🫁',
      label: 'SpO₂',
      value: _spo2 != null ? '${_spo2}' : '––',
      unit: '%',
      updatedAt: _formatTime(_spo2Time),
      accentColor: const Color(0xFF00C6FF),
      gradientColors: const [Color(0xFF0D1F2D), Color(0xFF0A1520)],
      hasData: _spo2 != null,
    );
  }

  Widget _buildStressCard() {
    final color = _stressColor(_stress);
    return _HealthCard(
      pulseController: _stressPulse,
      icon: '😌',
      label: 'Stress',
      value: _stress != null ? '${_stress}' : '––',
      unit: _stress != null ? _stressLabel(_stress) : '',
      updatedAt: _formatTime(_stressTime),
      accentColor: color,
      gradientColors: [
        color.withOpacity(0.15),
        const Color(0xFF0A0A1A),
      ],
      hasData: _stress != null,
    );
  }

  Widget _buildStatusBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF111122),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.06)),
      ),
      child: Row(
        children: [
          const Icon(Icons.info_outline_rounded, color: Color(0xFF6C63FF), size: 16),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              'Listening for broadcasts from Gadgetbridge\n'
              'Actions: HEART_RATE • SPO₂ • STRESS',
              style: TextStyle(
                fontSize: 11,
                color: Colors.white.withOpacity(0.4),
                height: 1.6,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// HEALTH CARD WIDGET
// ─────────────────────────────────────────────────────────────────────────────

class _HealthCard extends StatelessWidget {
  final AnimationController pulseController;
  final String icon;
  final String label;
  final String value;
  final String unit;
  final String updatedAt;
  final Color accentColor;
  final List<Color> gradientColors;
  final bool hasData;

  const _HealthCard({
    required this.pulseController,
    required this.icon,
    required this.label,
    required this.value,
    required this.unit,
    required this.updatedAt,
    required this.accentColor,
    required this.gradientColors,
    required this.hasData,
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: pulseController,
      builder: (context, child) {
        final scale = 1.0 + (pulseController.value * 0.02 * (1 - pulseController.value));
        return Transform.scale(
          scale: scale,
          child: child,
        );
      },
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: gradientColors,
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: hasData
                ? accentColor.withOpacity(0.3)
                : Colors.white.withOpacity(0.08),
          ),
          boxShadow: hasData
              ? [
                  BoxShadow(
                    color: accentColor.withOpacity(0.15),
                    blurRadius: 20,
                    offset: const Offset(0, 4),
                  ),
                ]
              : [],
        ),
        child: Row(
          children: [
            // Left: value display
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Icon + label
                  Row(
                    children: [
                      Text(icon, style: const TextStyle(fontSize: 20)),
                      const SizedBox(width: 8),
                      Text(
                        label,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Colors.white.withOpacity(0.7),
                          letterSpacing: 0.3,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Value
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        value,
                        style: TextStyle(
                          fontSize: 48,
                          fontWeight: FontWeight.w700,
                          color: hasData ? Colors.white : Colors.white.withOpacity(0.25),
                          letterSpacing: -2,
                          height: 1.0,
                        ),
                      ),
                      if (unit.isNotEmpty) ...[
                        const SizedBox(width: 6),
                        Padding(
                          padding: const EdgeInsets.only(bottom: 6),
                          child: Text(
                            unit,
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w500,
                              color: hasData
                                  ? accentColor
                                  : Colors.white.withOpacity(0.2),
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 8),
                  // Last updated
                  Row(
                    children: [
                      Icon(
                        Icons.access_time_rounded,
                        size: 12,
                        color: Colors.white.withOpacity(0.3),
                      ),
                      const SizedBox(width: 4),
                      Text(
                        hasData ? 'Updated $updatedAt' : 'Waiting for data',
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.white.withOpacity(0.3),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            // Right: accent indicator
            if (hasData)
              Container(
                width: 4,
                height: 60,
                decoration: BoxDecoration(
                  color: accentColor,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
