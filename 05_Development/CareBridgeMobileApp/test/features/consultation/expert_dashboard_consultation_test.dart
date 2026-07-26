import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/expert/services/expert_home_service.dart';

class _DashboardApi implements ExpertHomeApi {
  final List<String> paths = [];

  @override
  Future<dynamic> get(String path) async {
    paths.add(path);
    if (path.contains('pending-summary')) {
      return {
        'data': {'pendingCount': 4},
      };
    }
    if (path.contains('/assigned')) {
      return {
        'content': [
          {
            'id': 'request-1',
            'counterpartDisplayName': 'Mẹ An',
            'topic': 'Dinh dưỡng',
            'status': 'PENDING',
            'createdAt': '2026-07-16T12:00:00Z',
          },
        ],
        'page': 0,
        'size': 5,
        'totalElements': 1,
        'totalPages': 1,
      };
    }
    throw Exception('optional dashboard dependency unavailable');
  }
}

void main() {
  // CONREQ-FL-10
  test(
    'dashboard uses real consultation-request endpoints without fake fallback',
    () async {
      final api = _DashboardApi();
      final service = ExpertHomeService(api: api);

      final snapshot = await service.loadSnapshot();

      expect(snapshot.requestCount, 4);
      expect(snapshot.nextConsultation?.motherName, 'Mẹ An');
      expect(
        api.paths,
        contains('/api/v1/consultation-requests/pending-summary'),
      );
      expect(
        api.paths,
        contains(
          '/api/v1/consultation-requests/assigned?status=PENDING&page=0&size=5',
        ),
      );
    },
  );
}
