import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';

class TestScreen extends StatefulWidget {
  const TestScreen({super.key});
  @override
  State<TestScreen> createState() => _TestScreenState();
}

class _TestScreenState extends State<TestScreen> {
  bool _loading = false;
  String? _error;

  Future<void> _runAiVerify() async {
    setState(() { _loading = true; _error = null; });
    try {
      Uri uri = Uri.parse("http://localhost:8080/api/v1/expert/verify-face");
      var request = http.MultipartRequest("POST", uri);
      request.headers["Authorization"] = "Bearer ${AuthState.instance.accessToken ?? ""}";
      request.files.add(http.MultipartFile.fromBytes("selfie", _selfie!.bytes,
          filename: _selfie!.fileName, contentType: MediaType.parse(_selfie!.mimeType)));
      final streamed = await request.send();
      final resp = await http.Response.fromStream(streamed);
      if (resp.statusCode >= 200 && resp.statusCode < 300) {
        final data = jsonDecode(utf8.decode(resp.bodyBytes));
        final similar = (data["data"]["status"] ?? "").toString() == "MATCHED";
        final similarity = (data["data"]["similarity"] ?? 0) as double;
        if (mounted) {
          setState(() {
            _loading = false;
            _similarity = similarity;
            _aiStatus = similar ? "MATCHED" : "NOT_MATCHED";
            _aiColor = similar ? Colors.green : Colors.red;
          });
        }
      } else {
        if (mounted) setState(() { _loading = false; _error = "HTTP ${resp.statusCode}"; });
      }
    } catch (e) {
      if (mounted) setState(() { _loading = false; _error = e.toString(); });
    }
  }
}
