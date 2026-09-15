import '../../../core/network/api_client.dart';
import '../models/expert_content_approval_model.dart';

abstract class ExpertContentApprovalApi {
  Future<dynamic> get(String path, {Map<String, dynamic>? queryParams});
  Future<dynamic> post(String path, Map<String, dynamic> body);
}

class _DefaultExpertContentApprovalApi implements ExpertContentApprovalApi {
  @override
  Future<dynamic> get(String path, {Map<String, dynamic>? queryParams}) =>
      apiGet(path, queryParams: queryParams);

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) =>
      apiPost(path, body);
}

class ExpertContentApprovalService {
  static ExpertContentApprovalService instance = ExpertContentApprovalService();

  final ExpertContentApprovalApi api;

  ExpertContentApprovalService({ExpertContentApprovalApi? api})
      : api = api ?? _DefaultExpertContentApprovalApi();

  /// Fetches the assigned approval queue for the authenticated expert.
  Future<PaginatedApprovalQueue> fetchQueue({
    String? type,
    String? stage,
    String? keyword,
    int page = 0,
    int size = 20,
  }) async {
    final queryParams = <String, dynamic>{
      'page': page,
      'size': size,
    };
    if (type != null && type != 'ALL') {
      queryParams['type'] = type;
    }
    if (stage != null && stage != 'ALL') {
      queryParams['stage'] = stage;
    }
    if (keyword != null && keyword.trim().isNotEmpty) {
      queryParams['keyword'] = keyword.trim();
    }

    final res = await api.get(
      '/api/v1/expert/content-approval/queue',
      queryParams: queryParams,
    );

    Map<String, dynamic> data;
    if (res is Map<String, dynamic>) {
      if (res['data'] is Map<String, dynamic>) {
        data = res['data'] as Map<String, dynamic>;
      } else {
        data = res;
      }
    } else {
      data = {'content': []};
    }

    return PaginatedApprovalQueue.fromJson(data);
  }

  /// Fetches staff content detail (article/FAQ) including pending reviews.
  Future<ContentDetailModel> fetchContentDetail(String id) async {
    final res = await api.get('/api/v1/admin/content/$id');
    Map<String, dynamic> data;
    if (res is Map<String, dynamic>) {
      if (res['data'] is Map<String, dynamic>) {
        data = res['data'] as Map<String, dynamic>;
      } else {
        data = res;
      }
    } else {
      data = {};
    }
    return ContentDetailModel.fromJson(data);
  }

  /// Fetches checklist template detail including items and review feedback.
  Future<ChecklistTemplateDetailModel> fetchChecklistDetail(String id) async {
    final res = await api.get('/api/v1/admin/checklist-templates/$id');
    Map<String, dynamic> data;
    if (res is Map<String, dynamic>) {
      if (res['data'] is Map<String, dynamic>) {
        data = res['data'] as Map<String, dynamic>;
      } else {
        data = res;
      }
    } else {
      data = {};
    }
    return ChecklistTemplateDetailModel.fromJson(data);
  }

  /// Submits an expert decision for an ARTICLE or FAQ content item.
  Future<Map<String, dynamic>> decideContent(
    String id,
    String decision, {
    String? reason,
  }) async {
    final body = <String, dynamic>{
      'decision': decision,
      if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
    };

    final res = await api.post(
      '/api/v1/expert/content-approval/content/$id/decision',
      body,
    );

    if (res is Map<String, dynamic>) {
      if (res['data'] is Map<String, dynamic>) {
        return res['data'] as Map<String, dynamic>;
      }
      return res;
    }
    return {'status': 'OK'};
  }

  /// Submits an expert decision for a CHECKLIST template.
  Future<Map<String, dynamic>> decideChecklist(
    String id,
    String decision, {
    String? reason,
  }) async {
    final body = <String, dynamic>{
      'decision': decision,
      if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
    };

    final res = await api.post(
      '/api/v1/expert/content-approval/checklists/$id/decision',
      body,
    );

    if (res is Map<String, dynamic>) {
      if (res['data'] is Map<String, dynamic>) {
        return res['data'] as Map<String, dynamic>;
      }
      return res;
    }
    return {'status': 'OK'};
  }
}
