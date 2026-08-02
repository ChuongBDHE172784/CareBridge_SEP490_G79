import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:untitled/features/emergency/models/care_facility_model.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/screens/emergency_map_screen.dart';
import 'package:untitled/features/emergency/services/care_facility_service.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';
import 'package:untitled/features/safety/services/safety_permission_service.dart';

class _EmergencyStub extends EmergencyService {
  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) async => EmergencySession(
    sessionId: 'session-1',
    userId: 'mother-1',
    status: 'ACTIVE',
    triggerSource: triggerSource,
  );
}

class _RetryEmergencyStub extends EmergencyService {
  var calls = 0;

  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) async {
    calls++;
    if (calls == 1) throw StateError('temporary failure');
    return EmergencySession(
      sessionId: 'session-1',
      userId: 'mother-1',
      status: 'ACTIVE',
      triggerSource: triggerSource,
    );
  }
}

class _FacilityStub extends CareFacilityService {
  _FacilityStub(this.results);

  final List<CareFacility> results;
  int searchCalls = 0;

  @override
  Future<List<CareFacility>> searchNearby({
    required double latitude,
    required double longitude,
    int radiusMeters = 5000,
    String type = 'hospital',
  }) async {
    searchCalls++;
    return results;
  }

  @override
  Future<CareFacility> getFacility(String facilityId) async =>
      results.firstWhere((facility) => facility.facilityId == facilityId);

  @override
  Future<CareRoute> getRoute({
    required double fromLatitude,
    required double fromLongitude,
    required double toLatitude,
    required double toLongitude,
    String transportMode = 'DRIVING',
  }) async => const CareRoute(
    distanceMeters: 1200,
    etaMinutes: 5,
    transportMode: 'DRIVING',
  );
}

Position _position() => Position(
  longitude: 106.66,
  latitude: 10.76,
  timestamp: DateTime(2026),
  accuracy: 5,
  altitude: 0,
  altitudeAccuracy: 0,
  heading: 0,
  headingAccuracy: 0,
  speed: 0,
  speedAccuracy: 0,
);

void main() {
  test('decodes TrackAsia encoded polyline without Google Maps', () {
    final points = CareRoute.decodePolyline(
      '_p~iF~ps|U_ulLnnqC_mqNvxq`@',
      precision: 5,
    );

    expect(points, hasLength(3));
    expect(points.first.latitude, closeTo(38.5, 0.00001));
    expect(points.first.longitude, closeTo(-120.2, 0.00001));
    expect(points.last.latitude, closeTo(43.252, 0.00001));
    expect(points.last.longitude, closeTo(-126.453, 0.00001));
  });

  test('route contract decodes TrackAsia polyline6 geometry', () {
    final route = CareRoute.fromJson({
      'distanceMeters': 100,
      'etaMinutes': 1,
      'durationSeconds': 30,
      'encodedPolyline': '_p~iF~ps|U_ulLnnqC_mqNvxq`@',
      'steps': const [],
      'transportMode': 'DRIVING',
    });

    expect(route.coordinates.first.latitude, closeTo(3.85, 0.000001));
    expect(route.coordinates.first.longitude, closeTo(-12.02, 0.000001));
  });

  test(
    'facility parser accepts canonical, nullable external, and legacy IDs',
    () {
      final external = CareFacility.fromJson({
        'name': 'Provider POI',
        'sourceType': 'TRACKASIA',
        'verificationStatus': 'UNVERIFIED',
        'latitude': 10.7,
        'longitude': 106.6,
      });
      final legacy = CareFacility.fromJson({
        'hospitalId': 'old-id',
        'name': 'Legacy hospital',
      });

      expect(external.facilityId, isNull);
      expect(external.sourceLabel, 'Nguồn TrackAsia');
      expect(external.verificationLabel, 'Chưa được CareBridge xác minh');
      expect(legacy.facilityId, 'old-id');
    },
  );

  test('facility parser rejects missing name and does not invent source', () {
    expect(
      () => CareFacility.fromJson({'sourceType': 'TRACKASIA'}),
      throwsFormatException,
    );
    final unknown = CareFacility.fromJson({'name': 'Valid facility'});
    final verified = CareFacility.fromJson({
      'name': 'Verified facility',
      'verificationStatus': 'VERIFIED',
    });
    expect(unknown.sourceType, 'UNKNOWN');
    expect(unknown.sourceLabel, 'Nguồn dữ liệu chưa xác định');
    expect(verified.isVerified, isTrue);
    expect(verified.verificationLabel, 'Đã được CareBridge xác minh');
  });

  testWidgets('denied location keeps emergency call available', (tester) async {
    final launched = <Uri>[];
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          facilityService: _FacilityStub(const []),
          permissionService: SafetyPermissionService(
            locationReader: () async => null,
          ),
          emergencyService: _EmergencyStub(),
          locationConsentProbe: () async => true,
          uriLauncher: (uri) async {
            launched.add(uri);
            return true;
          },
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('nearby-notice')), findsOneWidget);
    expect(find.text('Gọi cấp cứu 115'), findsOneWidget);
    await tester.tap(find.byKey(const Key('emergency-call')));
    expect(launched.single.toString(), 'tel:115');
  });

  testWidgets('missing location consent does not request device location', (
    tester,
  ) async {
    var locationRead = false;
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          facilityService: _FacilityStub(const []),
          permissionService: SafetyPermissionService(
            locationReader: () async {
              locationRead = true;
              return _position();
            },
          ),
          emergencyService: _EmergencyStub(),
          locationConsentProbe: () async => false,
          uriLauncher: (_) async => true,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(locationRead, isFalse);
    expect(find.byKey(const Key('location-consent-action')), findsOneWidget);
    expect(find.text('Gọi cấp cứu 115'), findsOneWidget);
  });

  testWidgets(
    'accepted disclosure grants scoped consent before location read',
    (tester) async {
      var consentActive = false;
      var locationRead = false;
      final facilityService = _FacilityStub(const []);
      Map<String, String>? grantedConsent;
      await tester.pumpWidget(
        MaterialApp(
          home: EmergencyMapScreen(
            facilityService: facilityService,
            permissionService: SafetyPermissionService(
              locationReader: () async {
                locationRead = true;
                return _position();
              },
            ),
            emergencyService: _EmergencyStub(),
            locationConsentProbe: () async => consentActive,
            locationConsentGrant:
                ({
                  required dataType,
                  required purpose,
                  required recipient,
                  required scope,
                }) async {
                  expect(locationRead, isFalse);
                  grantedConsent = {
                    'dataType': dataType,
                    'purpose': purpose,
                    'recipient': recipient,
                    'scope': scope,
                  };
                  consentActive = true;
                },
            uriLauncher: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(locationRead, isFalse);
      await tester.tap(find.byKey(const Key('location-consent-action')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('location-consent-dialog')), findsOneWidget);
      expect(
        find.byKey(const Key('location-consent-disclosure')),
        findsOneWidget,
      );

      await tester.tap(find.byKey(const Key('location-consent-confirm')));
      await tester.pumpAndSettle();

      expect(grantedConsent, {
        'dataType': 'LOCATION',
        'purpose': 'SHARE',
        'recipient': 'CAREBRIDGE_SAFETY',
        'scope': 'SAFETY_EMERGENCY_ALERT',
      });
      expect(locationRead, isTrue);
      expect(facilityService.searchCalls, 1);
    },
  );

  testWidgets(
    'cancelled disclosure neither grants consent nor reads location',
    (tester) async {
      var grantCalls = 0;
      var locationRead = false;
      await tester.pumpWidget(
        MaterialApp(
          home: EmergencyMapScreen(
            facilityService: _FacilityStub(const []),
            permissionService: SafetyPermissionService(
              locationReader: () async {
                locationRead = true;
                return _position();
              },
            ),
            emergencyService: _EmergencyStub(),
            locationConsentProbe: () async => false,
            locationConsentGrant:
                ({
                  required dataType,
                  required purpose,
                  required recipient,
                  required scope,
                }) async {
                  grantCalls++;
                },
            uriLauncher: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('location-consent-action')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('location-consent-cancel')));
      await tester.pumpAndSettle();

      expect(grantCalls, 0);
      expect(locationRead, isFalse);
      expect(find.byKey(const Key('location-consent-action')), findsOneWidget);
    },
  );

  testWidgets(
    'failed consent grant remains retryable without reading location',
    (tester) async {
      var grantCalls = 0;
      var locationRead = false;
      await tester.pumpWidget(
        MaterialApp(
          home: EmergencyMapScreen(
            facilityService: _FacilityStub(const []),
            permissionService: SafetyPermissionService(
              locationReader: () async {
                locationRead = true;
                return _position();
              },
            ),
            emergencyService: _EmergencyStub(),
            locationConsentProbe: () async => false,
            locationConsentGrant:
                ({
                  required dataType,
                  required purpose,
                  required recipient,
                  required scope,
                }) async {
                  grantCalls++;
                  throw StateError('offline');
                },
            uriLauncher: (_) async => true,
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('location-consent-action')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('location-consent-confirm')));
      await tester.pumpAndSettle();

      expect(grantCalls, 1);
      expect(locationRead, isFalse);
      expect(find.textContaining('Không thể lưu đồng ý'), findsOneWidget);
      expect(find.byKey(const Key('location-consent-action')), findsOneWidget);
    },
  );

  testWidgets('failed initial family alert exposes independent retry', (
    tester,
  ) async {
    final emergency = _RetryEmergencyStub();
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          facilityService: _FacilityStub(const []),
          permissionService: SafetyPermissionService(
            locationReader: () async => null,
          ),
          emergencyService: emergency,
          locationConsentProbe: () async => false,
          uriLauncher: (_) async => true,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Thử gửi lại báo động gia đình'), findsOneWidget);
    await tester.tap(find.byKey(const Key('family-alert')));
    await tester.pumpAndSettle();
    expect(emergency.calls, 2);
    expect(find.text('Báo động gia đình'), findsOneWidget);
    expect(
      find.text(
        'Yêu cầu hỗ trợ đã được gửi; thông báo gia đình đang được xử lý.',
      ),
      findsOneWidget,
    );
    expect(find.text('Đã gửi báo động đến người thân'), findsNothing);
  });

  testWidgets('renders provider labels and route for nullable facility ID', (
    tester,
  ) async {
    const facility = CareFacility(
      name: 'Phòng khám gần nhất',
      address: 'Hà Nội',
      latitude: 21.03,
      longitude: 105.85,
      phone: '0241234567',
      sourceType: 'TRACKASIA',
      verificationStatus: 'VERIFIED',
    );
    const otherFacility = CareFacility(
      name: 'Cơ sở xa hơn',
      distanceMeters: 9000,
      sourceType: 'MANUAL',
      verificationStatus: 'UNVERIFIED',
    );
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          facilityService: _FacilityStub(const [facility, otherFacility]),
          permissionService: SafetyPermissionService(
            locationReader: () async => _position(),
          ),
          emergencyService: _EmergencyStub(),
          locationConsentProbe: () async => true,
          uriLauncher: (_) async => true,
        ),
      ),
    );
    await tester.pumpAndSettle();
    final facilityTile = find.byKey(const Key('facility-0'));
    await tester.ensureVisible(facilityTile);
    await tester.tap(facilityTile);
    await tester.pumpAndSettle();

    expect(find.text('Phòng khám gần nhất'), findsWidgets);
    expect(find.textContaining('Nguồn TrackAsia'), findsOneWidget);
    expect(find.textContaining('Đã được CareBridge xác minh'), findsOneWidget);
    expect(find.textContaining('ETA 5 phút'), findsOneWidget);
    await tester.drag(
      find.byKey(const Key('nearby-list')),
      const Offset(0, -240),
    );
    await tester.pumpAndSettle();
    expect(find.textContaining('9.0 km'), findsOneWidget);
    expect(find.byKey(const Key('facility-navigate')), findsOneWidget);
  });
}
