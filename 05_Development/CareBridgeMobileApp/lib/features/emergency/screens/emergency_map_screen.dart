import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../privacy/services/privacy_service.dart';
import '../../safety/services/safety_permission_service.dart';
import '../models/care_facility_model.dart';
import '../services/care_facility_service.dart';
import '../services/emergency_service.dart';

typedef EmergencyUriLauncher = Future<bool> Function(Uri uri);
typedef LocationConsentProbe = Future<bool> Function();

/// Emergency help remains available when location or the route provider is
/// unavailable. Nearby results are informational and never delay emergency
/// calling or opening the emergency session.
class EmergencyMapScreen extends StatefulWidget {
  const EmergencyMapScreen({
    super.key,
    this.facilityService,
    this.permissionService,
    this.emergencyService,
    this.uriLauncher,
    this.locationConsentProbe,
  });

  final CareFacilityService? facilityService;
  final SafetyPermissionService? permissionService;
  final EmergencyService? emergencyService;
  final EmergencyUriLauncher? uriLauncher;
  final LocationConsentProbe? locationConsentProbe;

  @override
  State<EmergencyMapScreen> createState() => _EmergencyMapScreenState();
}

class _EmergencyMapScreenState extends State<EmergencyMapScreen> {
  static const _primary = Color(0xFF845143);
  static const _surface = Color(0xFFFFF8F6);
  static const _emergencyNumber = '115';

  late final CareFacilityService _facilities =
      widget.facilityService ?? CareFacilityService();
  late final SafetyPermissionService _permissions =
      widget.permissionService ?? SafetyPermissionService();
  late final EmergencyService _emergency =
      widget.emergencyService ?? EmergencyService();
  late final EmergencyUriLauncher _launch =
      widget.uriLauncher ?? ((uri) => launchUrl(uri));
  late final LocationConsentProbe _hasLocationConsent =
      widget.locationConsentProbe ?? _defaultLocationConsentProbe;

  Position? _position;
  List<CareFacility> _results = const [];
  CareFacility? _selected;
  CareRoute? _route;
  bool _loading = true;
  bool _sendingFamilyAlert = false;
  bool _familyAlertFailed = false;
  String? _notice;
  int _loadGeneration = 0;
  int _selectionGeneration = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final generation = ++_loadGeneration;
    ++_selectionGeneration;
    if (mounted) {
      setState(() {
        _loading = true;
        _notice = null;
        _route = null;
      });
    }
    await _sendFamilyAlert(showFeedback: false);
    if (!mounted || generation != _loadGeneration) return;

    try {
      if (!await _hasLocationConsent()) {
        if (mounted && generation == _loadGeneration) {
          setState(() {
            _loading = false;
            _notice =
                'Chưa có consent chia sẻ vị trí. Bạn vẫn có thể gọi cấp cứu.';
          });
        }
        return;
      }
      if (!mounted || generation != _loadGeneration) return;
      final position = await _permissions.readConsentedLocation();
      if (!mounted || generation != _loadGeneration) return;
      if (position == null) {
        if (mounted) {
          setState(() {
            _loading = false;
            _notice =
                'Không có quyền vị trí. Bạn vẫn có thể gọi cấp cứu hoặc thử lại.';
          });
        }
        return;
      }
      final results = await _facilities.searchNearby(
        latitude: position.latitude,
        longitude: position.longitude,
      );
      if (!mounted || generation != _loadGeneration) return;
      final selectionGeneration = ++_selectionGeneration;
      setState(() {
        _position = position;
        _results = results;
        _selected = results.isEmpty ? null : results.first;
        _loading = false;
        _notice = results.isEmpty
            ? 'Không tìm thấy cơ sở phù hợp. Hãy gọi cấp cứu khi cần.'
            : null;
      });
      if (_selected?.hasCoordinates == true) {
        await _loadRoute(_selected!, selectionGeneration);
      }
    } catch (_) {
      if (mounted && generation == _loadGeneration) {
        setState(() {
          _loading = false;
          _notice = 'Không thể tải cơ sở gần đây. Bạn vẫn có thể gọi cấp cứu.';
        });
      }
    }
  }

  Future<bool> _defaultLocationConsentProbe() async {
    final grants = await PrivacyService.instance.listConsents();
    return grants.any(
      (grant) =>
          grant.isActive &&
          grant.dataType == 'LOCATION' &&
          grant.purpose == 'SHARE' &&
          grant.recipient == 'CAREBRIDGE_SAFETY' &&
          grant.scope == 'SAFETY_EMERGENCY_ALERT',
    );
  }

  Future<void> _loadRoute(
    CareFacility facility,
    int selectionGeneration,
  ) async {
    final position = _position;
    if (position == null || !facility.hasCoordinates) return;
    try {
      final route = await _facilities.getRoute(
        fromLatitude: position.latitude,
        fromLongitude: position.longitude,
        toLatitude: facility.latitude!,
        toLongitude: facility.longitude!,
      );
      if (mounted &&
          selectionGeneration == _selectionGeneration &&
          identical(_selected, facility)) {
        setState(() => _route = route);
      }
    } catch (_) {
      // Directions can still be delegated to the installed navigation app.
    }
  }

  Future<void> _select(CareFacility facility) async {
    final selectionGeneration = ++_selectionGeneration;
    setState(() {
      _selected = facility;
      _route = null;
    });
    var detail = facility;
    if (facility.facilityId != null) {
      try {
        detail = await _facilities.getFacility(facility.facilityId!);
        if (!mounted || selectionGeneration != _selectionGeneration) return;
        setState(() {
          _selected = detail;
          final index = _results.indexOf(facility);
          if (index >= 0) {
            _results = [..._results]..[index] = detail;
          }
        });
      } catch (_) {
        // The nearby item remains usable when detail refresh is unavailable.
      }
    }
    if (selectionGeneration == _selectionGeneration) {
      await _loadRoute(detail, selectionGeneration);
    }
  }

  Future<void> _sendFamilyAlert({bool showFeedback = true}) async {
    if (_sendingFamilyAlert) return;
    if (mounted) setState(() => _sendingFamilyAlert = true);
    try {
      await _emergency.openFlow(triggerSource: 'MANUAL');
      if (!mounted) return;
      setState(() => _familyAlertFailed = false);
      if (showFeedback) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã gửi báo động đến người thân')),
        );
      }
    } catch (_) {
      if (!mounted) return;
      setState(() => _familyAlertFailed = true);
      if (showFeedback) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể gửi báo động. Hãy thử lại.')),
        );
      }
    } finally {
      if (mounted) setState(() => _sendingFamilyAlert = false);
    }
  }

  Future<void> _call(String? phone) async {
    await _launch(Uri(scheme: 'tel', path: phone ?? _emergencyNumber));
  }

  Future<void> _navigate(CareFacility facility) async {
    if (!facility.hasCoordinates) return;
    await _launch(
      Uri.parse(
        'https://www.google.com/maps/dir/?api=1&destination='
        '${facility.latitude},${facility.longitude}',
      ),
    );
  }

  String _distance(CareFacility facility) {
    final meters = identical(_selected, facility) && _route != null
        ? _route!.distanceMeters
        : facility.distanceMeters?.toDouble();
    if (meters == null) return 'Khoảng cách chưa xác định';
    return meters >= 1000
        ? '${(meters / 1000).toStringAsFixed(1)} km'
        : '${meters.round()} m';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        title: const Text('Cơ sở y tế gần đây'),
        backgroundColor: _surface,
        foregroundColor: _primary,
      ),
      body: Column(
        children: [
          if (_loading)
            const LinearProgressIndicator(key: Key('nearby-loading')),
          if (_notice != null)
            MaterialBanner(
              key: const Key('nearby-notice'),
              content: Text(_notice!),
              actions: [
                TextButton(onPressed: _load, child: const Text('Thử lại')),
              ],
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                key: const Key('emergency-call'),
                onPressed: () => _call(null),
                style: FilledButton.styleFrom(backgroundColor: Colors.red),
                icon: const Icon(Icons.call),
                label: const Text('Gọi cấp cứu 115'),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                key: const Key('family-alert'),
                onPressed: _sendingFamilyAlert
                    ? null
                    : () => _sendFamilyAlert(),
                icon: _sendingFamilyAlert
                    ? const SizedBox.square(
                        dimension: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : Icon(_familyAlertFailed ? Icons.refresh : Icons.campaign),
                label: Text(
                  _familyAlertFailed
                      ? 'Thử gửi lại báo động gia đình'
                      : 'Báo động gia đình',
                ),
              ),
            ),
          ),
          Expanded(
            child: _results.isEmpty
                ? const Center(
                    child: Icon(Icons.local_hospital_outlined, size: 72),
                  )
                : ListView.builder(
                    key: const Key('nearby-list'),
                    padding: const EdgeInsets.all(12),
                    itemCount: _results.length,
                    itemBuilder: (_, index) {
                      final facility = _results[index];
                      return Card(
                        child: ListTile(
                          key: Key('facility-$index'),
                          selected: identical(_selected, facility),
                          title: Text(facility.name),
                          subtitle: Text(
                            [
                              if (facility.address?.isNotEmpty == true)
                                facility.address!,
                              _distance(facility),
                              facility.sourceLabel,
                              facility.verificationLabel,
                            ].join('\n'),
                          ),
                          isThreeLine: true,
                          onTap: () => _select(facility),
                        ),
                      );
                    },
                  ),
          ),
          if (_selected != null) _buildSelected(_selected!),
        ],
      ),
    );
  }

  Widget _buildSelected(CareFacility facility) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(facility.name, style: Theme.of(context).textTheme.titleMedium),
            if (_route != null)
              Text('ETA ${_route!.etaMinutes} phút · ${_distance(facility)}'),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    key: const Key('facility-call'),
                    onPressed: facility.phone == null
                        ? null
                        : () => _call(facility.phone),
                    icon: const Icon(Icons.call),
                    label: const Text('Gọi cơ sở'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: FilledButton.icon(
                    key: const Key('facility-navigate'),
                    onPressed: facility.hasCoordinates
                        ? () => _navigate(facility)
                        : null,
                    icon: const Icon(Icons.directions),
                    label: const Text('Chỉ đường'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
