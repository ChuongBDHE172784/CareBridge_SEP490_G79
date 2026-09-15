import 'package:flutter_test/flutter_test.dart';

import 'package:untitled/features/expert/services/expert_content_approval_service.dart';

class MockExpertContentApprovalApi implements ExpertContentApprovalApi {
  String? lastGetPath;
  Map<String, dynamic>? lastGetParams;
  String? lastPostPath;
  Map<String, dynamic>? lastPostBody;

  dynamic mockGetResponse;
  dynamic mockPostResponse;

  @override
  Future<dynamic> get(String path, {Map<String, dynamic>? queryParams}) async {
    lastGetPath = path;
    lastGetParams = queryParams;
    return mockGetResponse ?? {};
  }

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) async {
    lastPostPath = path;
    lastPostBody = body;
    return mockPostResponse ?? {'status': 'OK'};
  }
}

void main() {
  group('ExpertContentApprovalService Tests', () {
    late MockExpertContentApprovalApi mockApi;
    late ExpertContentApprovalService service;

    setUp(() {
      mockApi = MockExpertContentApprovalApi();
      service = ExpertContentApprovalService(api: mockApi);
    });

    test('fetchQueue sends correct parameters and parses PaginatedApprovalQueue', () async {
      mockApi.mockGetResponse = {
        'data': {
          'content': [
            {
              'id': 'art-1',
              'kind': 'CONTENT',
              'type': 'ARTICLE',
              'title': 'Dinh dưỡng 3 tháng đầu',
              'stage': 'PREGNANCY',
              'status': 'PENDING_REVIEW',
              'summary': 'Tóm tắt bài viết dinh dưỡng',
              'createdAt': '2026-09-15T08:00:00.000Z',
            },
            {
              'id': 'chk-1',
              'kind': 'CHECKLIST',
              'type': 'CHECKLIST',
              'name': 'Checklist tuần 12',
              'stage': 'PREGNANCY',
              'status': 'PENDING_REVIEW',
              'itemCount': 5,
              'createdAt': '2026-09-15T09:00:00.000Z',
            },
          ],
          'totalElements': 2,
          'totalPages': 1,
          'size': 20,
          'number': 0,
        },
      };

      final res = await service.fetchQueue(
        type: 'ARTICLE',
        stage: 'PREGNANCY',
        keyword: 'Dinh dưỡng',
        page: 0,
        size: 20,
      );

      expect(mockApi.lastGetPath, '/api/v1/expert/content-approval/queue');
      expect(mockApi.lastGetParams?['type'], 'ARTICLE');
      expect(mockApi.lastGetParams?['stage'], 'PREGNANCY');
      expect(mockApi.lastGetParams?['keyword'], 'Dinh dưỡng');
      expect(res.content.length, 2);
      expect(res.content[0].title, 'Dinh dưỡng 3 tháng đầu');
      expect(res.content[0].type, 'ARTICLE');
      expect(res.content[1].title, 'Checklist tuần 12');
      expect(res.content[1].kind, 'CHECKLIST');
    });

    test('fetchContentDetail fetches from admin endpoint and parses details', () async {
      mockApi.mockGetResponse = {
        'data': {
          'id': 'art-1',
          'type': 'ARTICLE',
          'title': 'Dinh dưỡng 3 tháng đầu',
          'body': '<p>Nội dung bài viết y khoa</p>',
          'summary': 'Tóm tắt',
          'stage': 'PREGNANCY',
          'status': 'PENDING_REVIEW',
          'version': 1,
          'latestReviewFeedback': {
            'reason': 'Cần bổ sung thêm thông tin về acid folic',
            'requestedAt': '2026-09-14T10:00:00.000Z',
          },
        },
      };

      final detail = await service.fetchContentDetail('art-1');

      expect(mockApi.lastGetPath, '/api/v1/admin/content/art-1');
      expect(detail.id, 'art-1');
      expect(detail.title, 'Dinh dưỡng 3 tháng đầu');
      expect(detail.body, '<p>Nội dung bài viết y khoa</p>');
      expect(detail.latestReviewFeedback, isNotNull);
      expect(detail.latestReviewFeedback!.reason, contains('acid folic'));
    });

    test('fetchChecklistDetail fetches checklist template and items', () async {
      mockApi.mockGetResponse = {
        'data': {
          'id': 'chk-1',
          'name': 'Checklist tuần 12',
          'stage': 'PREGNANCY',
          'status': 'PENDING_REVIEW',
          'description': 'Mô tả checklist',
          'items': [
            {
              'id': 'item-1',
              'itemText': 'Đo độ mờ da gáy',
              'order': 1,
              'isRequired': true,
              'targetSubject': 'BABY',
              'supportFunction': 'APPOINTMENTS',
            },
          ],
        },
      };

      final detail = await service.fetchChecklistDetail('chk-1');

      expect(mockApi.lastGetPath, '/api/v1/admin/checklist-templates/chk-1');
      expect(detail.id, 'chk-1');
      expect(detail.name, 'Checklist tuần 12');
      expect(detail.items.length, 1);
      expect(detail.items.first.itemText, 'Đo độ mờ da gáy');
      expect(detail.items.first.isRequired, isTrue);
    });

    test('decideContent sends decision to content endpoint', () async {
      mockApi.mockPostResponse = {'id': 'art-1', 'newStatus': 'APPROVED'};

      final res = await service.decideContent('art-1', 'APPROVE');

      expect(mockApi.lastPostPath, '/api/v1/expert/content-approval/content/art-1/decision');
      expect(mockApi.lastPostBody?['decision'], 'APPROVE');
      expect(mockApi.lastPostBody?.containsKey('reason'), isFalse);
      expect(res['newStatus'], 'APPROVED');
    });

    test('decideContent sends REJECT with reason', () async {
      mockApi.mockPostResponse = {'id': 'art-1', 'newStatus': 'REJECTED'};

      final res = await service.decideContent(
        'art-1',
        'REJECT',
        reason: 'Nội dung thiếu dẫn chứng y khoa',
      );

      expect(mockApi.lastPostPath, '/api/v1/expert/content-approval/content/art-1/decision');
      expect(mockApi.lastPostBody?['decision'], 'REJECT');
      expect(mockApi.lastPostBody?['reason'], 'Nội dung thiếu dẫn chứng y khoa');
      expect(res['newStatus'], 'REJECTED');
    });

    test('decideChecklist sends decision to checklists endpoint', () async {
      mockApi.mockPostResponse = {'id': 'chk-1', 'newStatus': 'APPROVED'};

      final res = await service.decideChecklist('chk-1', 'APPROVE');

      expect(mockApi.lastPostPath, '/api/v1/expert/content-approval/checklists/chk-1/decision');
      expect(mockApi.lastPostBody?['decision'], 'APPROVE');
      expect(res['newStatus'], 'APPROVED');
    });
  });
}
