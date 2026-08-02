// Stub for TrackAsia GL on platforms where native trackasia_gl cannot compile (e.g. Web).

import 'package:flutter/widgets.dart';

class LatLng {
  final double latitude;
  final double longitude;
  const LatLng(this.latitude, this.longitude);
}

class CameraPosition {
  final LatLng target;
  final double zoom;
  const CameraPosition({required this.target, this.zoom = 0.0});
}

class CameraUpdate {
  static CameraUpdate newLatLngZoom(LatLng target, double zoom) => CameraUpdate();
  static CameraUpdate newLatLngBounds(LatLngBounds bounds, {double left = 0, double top = 0, double right = 0, double bottom = 0}) => CameraUpdate();
}

class LatLngBounds {
  final LatLng southwest;
  final LatLng northeast;
  const LatLngBounds({required this.southwest, required this.northeast});
}

class LineOptions {
  final List<LatLng>? geometry;
  final String? lineColor;
  final double? lineWidth;
  final double? lineOpacity;
  final String? lineJoin;
  const LineOptions({this.geometry, this.lineColor, this.lineWidth, this.lineOpacity, this.lineJoin});
}

class Line {}

class CircleOptions {
  final LatLng? geometry;
  final double? circleRadius;
  final String? circleColor;
  final String? circleStrokeColor;
  final double? circleStrokeWidth;
  const CircleOptions({
    this.geometry,
    this.circleRadius,
    this.circleColor,
    this.circleStrokeColor,
    this.circleStrokeWidth,
  });
}

class Circle {
  final Map<String, dynamic>? data;
  const Circle({this.data});
}

class OnCircleTappedHandler {
  void add(void Function(Circle circle) listener) {}
  void remove(void Function(Circle circle) listener) {}
}

class SymbolOptions {
  final LatLng? geometry;
  final String? iconImage;
  final double? iconSize;
  final String? textField;
  final String? textColor;
  final double? textSize;
  final List<double>? textOffset;
  final String? textAnchor;
  const SymbolOptions({
    this.geometry,
    this.iconImage,
    this.iconSize,
    this.textField,
    this.textColor,
    this.textSize,
    this.textOffset,
    this.textAnchor,
  });
}

class Symbol {}

enum MyLocationTrackingMode { none, trackingGps }
enum MyLocationRenderMode { normal, gps }

class TrackAsiaMapController {
  final OnCircleTappedHandler onCircleTapped = OnCircleTappedHandler();

  Future<void> animateCamera(CameraUpdate cameraUpdate) async {}
  Future<void> clearLines() async {}
  Future<Line?> addLine(LineOptions options) async => null;
  Future<void> clearCircles() async {}
  Future<Circle?> addCircle(CircleOptions options, [Map<String, dynamic>? data]) async => null;
  Future<void> clearSymbols() async {}
  Future<Symbol?> addSymbol(SymbolOptions options) async => null;
}

class TrackAsiaMap extends StatelessWidget {
  final String? styleString;
  final CameraPosition initialCameraPosition;
  final bool myLocationEnabled;
  final MyLocationTrackingMode myLocationTrackingMode;
  final MyLocationRenderMode myLocationRenderMode;
  final void Function(TrackAsiaMapController controller)? onMapCreated;
  final VoidCallback? onStyleLoadedCallback;

  const TrackAsiaMap({
    super.key,
    this.styleString,
    required this.initialCameraPosition,
    this.myLocationEnabled = false,
    this.myLocationTrackingMode = MyLocationTrackingMode.none,
    this.myLocationRenderMode = MyLocationRenderMode.normal,
    this.onMapCreated,
    this.onStyleLoadedCallback,
  });

  @override
  Widget build(BuildContext context) {
    return Container();
  }
}
