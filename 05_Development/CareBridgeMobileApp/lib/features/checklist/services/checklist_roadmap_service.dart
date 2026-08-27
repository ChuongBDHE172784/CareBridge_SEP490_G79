import '../../../core/network/api_client.dart';
import '../models/checklist_roadmap_model.dart';

class ChecklistRoadmapService {
  ChecklistRoadmapService();

  static final ChecklistRoadmapService instance = ChecklistRoadmapService();

  /// Tải toàn bộ lộ trình checklist (quá khứ, hiện tại và tương lai) dựa trên giai đoạn và tuần thai
  Future<List<ChecklistRoadmapMilestone>> loadRoadmap({
    int currentWeek = 24,
    String stage = 'PREGNANCY',
  }) async {
    try {
      final res = await apiGet('/api/v1/content/checklists', queryParams: {
        'stage': stage,
      });

      final rawList = (res['data'] as List? ?? []).cast<Map<String, dynamic>>();

      if (rawList.isNotEmpty) {
        final milestones = <ChecklistRoadmapMilestone>[];
        for (final item in rawList) {
          final startWeek = (item['eligibilityStartInclusive'] as num?)?.toInt() ?? 1;
          final endWeek = (item['eligibilityEndInclusive'] as num?)?.toInt() ?? startWeek;
          final rawTasks = (item['items'] as List? ?? []).cast<Map<String, dynamic>>();

          ChecklistMilestoneStatus status = ChecklistMilestoneStatus.upcoming;
          if (endWeek < currentWeek) {
            status = ChecklistMilestoneStatus.completed;
          } else if (startWeek <= currentWeek && currentWeek <= endWeek) {
            status = ChecklistMilestoneStatus.current;
          }

          final tasks = rawTasks.map((t) {
            return ChecklistRoadmapTask(
              id: t['id'] as String? ?? t['itemId'] as String? ?? '',
              title: t['title'] as String? ?? t['name'] as String? ?? t['itemText'] as String? ?? '',
              description: t['description'] as String?,
              category: t['category'] as String? ?? 'Chung',
              isRequired: t['isRequired'] as bool? ?? t['required'] as bool? ?? false,
              completed: status == ChecklistMilestoneStatus.completed,
              dueWeek: startWeek,
            );
          }).toList();

          milestones.add(ChecklistRoadmapMilestone(
            id: item['id'] as String? ?? '',
            title: item['name'] as String? ?? 'Checklist thai kỳ',
            description: item['description'] as String?,
            stage: stage,
            startWeek: startWeek,
            endWeek: endWeek,
            status: status,
            tasks: tasks,
          ));
        }

        milestones.sort((a, b) => a.startWeek.compareTo(b.startWeek));
        return milestones;
      }
    } catch (_) {
      // Fallback sang danh mục lộ trình chuẩn CareBridge
    }

    return _getDefaultRoadmap(currentWeek: currentWeek, stage: stage);
  }

  /// Trả về danh sách nhiệm vụ phân loại theo: Lịch sử, Hiện tại, Tương lai
  Future<Map<String, List<ChecklistRoadmapTask>>> loadCategorizedTasks({
    int currentWeek = 24,
    String stage = 'PREGNANCY',
  }) async {
    final roadmap = await loadRoadmap(currentWeek: currentWeek, stage: stage);
    final historyTasks = <ChecklistRoadmapTask>[];
    final currentTasks = <ChecklistRoadmapTask>[];
    final futureTasks = <ChecklistRoadmapTask>[];

    for (final milestone in roadmap) {
      if (milestone.status == ChecklistMilestoneStatus.completed) {
        historyTasks.addAll(milestone.tasks);
      } else if (milestone.status == ChecklistMilestoneStatus.current) {
        currentTasks.addAll(milestone.tasks);
      } else {
        futureTasks.addAll(milestone.tasks);
      }
    }

    return {
      'history': historyTasks,
      'current': currentTasks,
      'future': futureTasks,
    };
  }

  List<ChecklistRoadmapMilestone> _getDefaultRoadmap({
    required int currentWeek,
    required String stage,
  }) {
    final targetStage = stage.toUpperCase();
    final defs = _catalog.where((c) => c.stage == targetStage || (targetStage.isEmpty && c.stage == 'PREGNANCY')).toList();
    final activeDefs = defs.isNotEmpty ? defs : _catalog.where((c) => c.stage == 'PREGNANCY').toList();

    return activeDefs.map((def) {
      ChecklistMilestoneStatus status;
      if (def.endWeek < currentWeek) {
        status = ChecklistMilestoneStatus.completed;
      } else if (def.startWeek <= currentWeek && currentWeek <= def.endWeek) {
        status = ChecklistMilestoneStatus.current;
      } else {
        status = ChecklistMilestoneStatus.upcoming;
      }

      return ChecklistRoadmapMilestone(
        id: def.code,
        title: def.name,
        description: def.description,
        stage: def.stage,
        startWeek: def.startWeek,
        endWeek: def.endWeek,
        status: status,
        tasks: def.items.asMap().entries.map((e) {
          final item = e.value;
          return ChecklistRoadmapTask(
            id: '${def.code}-t-${e.key}',
            title: item.text,
            category: item.category ?? def.defaultCategory,
            isRequired: item.isRequired,
            completed: status == ChecklistMilestoneStatus.completed,
            dueWeek: def.startWeek,
          );
        }).toList(),
      );
    }).toList();
  }

  static final List<_CatalogMilestone> _catalog = [
    _CatalogMilestone(
      code: 'PRE_PREG_01',
      name: 'Đánh giá sức khỏe & yếu tố nguy cơ',
      description: 'Xem xét tiền sử mang thai, bệnh lý bản thân, gia đình và môi trường sống',
      stage: 'PRE_PREGNANCY',
      startWeek: 1,
      endWeek: 1,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Xem xét tiền sử mang thai và sinh con trước', isRequired: true, category: null),
        _CatalogItem(text: 'Xem xét tiền sử bệnh bản thân và gia đình', isRequired: true, category: null),
        _CatalogItem(text: 'Đánh giá nguy cơ bệnh mạn tính và di truyền', isRequired: true, category: null),
        _CatalogItem(text: 'Rà soát yếu tố nghề nghiệp và môi trường', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PRE_PREG_02',
      name: 'Khám, xét nghiệm & điều trị trước thai kỳ',
      description: 'Khám phụ khoa, kiểm soát bệnh mạn tính và rà soát thuốc đang dùng',
      stage: 'PRE_PREGNANCY',
      startWeek: 1,
      endWeek: 1,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám sức khỏe và khám phụ khoa định kỳ', isRequired: true, category: null),
        _CatalogItem(text: 'Điều trị bệnh phụ khoa và nhiễm khuẩn (nếu có)', isRequired: true, category: null),
        _CatalogItem(text: 'Kiểm soát bệnh mạn tính tiền thai kỳ', isRequired: true, category: null),
        _CatalogItem(text: 'Rà soát thuốc và thực phẩm chức năng đang dùng', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PRE_PREG_03',
      name: 'Điều chỉnh dinh dưỡng và lối sống',
      description: 'Chế độ ăn đa dạng, duy trì cân nặng hợp lý và vận động thể lực',
      stage: 'PRE_PREGNANCY',
      startWeek: 1,
      endWeek: 1,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Ăn uống đa dạng, đủ chất và sử dụng muối iod', isRequired: true, category: null),
        _CatalogItem(text: 'Duy trì cân nặng và chỉ số BMI hợp lý', isRequired: true, category: null),
        _CatalogItem(text: 'Tập thể dục thường xuyên, nghỉ ngơi hợp lý', isRequired: false, category: null),
        _CatalogItem(text: 'Tránh rượu bia, thuốc lá và chất kích thích', isRequired: true, category: null),
        _CatalogItem(text: 'Tránh tiếp xúc hóa chất độc hại', isRequired: true, category: null),
        _CatalogItem(text: 'Giữ vệ sinh và tẩy giun định kỳ', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PRE_PREG_04',
      name: 'Bổ sung vi chất & hoàn thành tiêm chủng',
      description: 'Bổ sung sắt, axit folic và tiêm các vắc-xin cần thiết trước mang thai',
      stage: 'PRE_PREGNANCY',
      startWeek: 1,
      endWeek: 1,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Bổ sung Sắt và Axit Folic trước thai kỳ', isRequired: true, category: null),
        _CatalogItem(text: 'Tư vấn Axit Folic liều cao nếu có tiền sử dị tật', isRequired: false, category: null),
        _CatalogItem(text: 'Rà soát lịch sử tiêm chủng cá nhân', isRequired: true, category: null),
        _CatalogItem(text: 'Tiêm các vắc-xin thiết yếu trước mang thai', isRequired: true, category: null),
        _CatalogItem(text: 'Tuân thủ khoảng cách sau tiêm MMR và thủy đậu', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PRE_PREG_05',
      name: 'Chuẩn bị sẵn sàng để thụ thai',
      description: 'Theo dõi chu kỳ kinh nguyệt và trang bị kiến thức tiền sản',
      stage: 'PRE_PREGNANCY',
      startWeek: 1,
      endWeek: 1,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Trang bị kiến thức làm mẹ và chăm sóc trẻ', isRequired: true, category: null),
        _CatalogItem(text: 'Theo dõi chu kỳ kinh nguyệt để nhận biết ngày rụng trứng', isRequired: true, category: null),
        _CatalogItem(text: 'Khuyến khích bạn đời duy trì lối sống lành mạnh', isRequired: false, category: null),
        _CatalogItem(text: 'Hoàn tất sàng lọc và chuẩn bị tâm lý', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_ONCE_01',
      name: 'Khám và xét nghiệm 20 tuần đầu',
      description: 'Khám thai lần đầu, xét nghiệm máu, sàng lọc dị tật và bệnh truyền nhiễm',
      stage: 'PREGNANCY',
      startWeek: 1,
      endWeek: 20,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Đi khám thai lần đầu', isRequired: true, category: null),
        _CatalogItem(text: 'Xét nghiệm haemoglobin phát hiện thiếu máu', isRequired: true, category: null),
        _CatalogItem(text: 'Xác định nhóm máu và tình trạng Rh', isRequired: true, category: null),
        _CatalogItem(text: 'Sàng lọc HIV, giang mai, viêm gan B', isRequired: true, category: null),
        _CatalogItem(text: 'Sàng lọc dị tật bẩm sinh', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_ONCE_02',
      name: 'Theo dõi và siêu âm tuần 21 - 25',
      description: 'Thực hiện siêu âm hình thái học trước tuần 24 và hoàn thành xét nghiệm còn thiếu',
      stage: 'PREGNANCY',
      startWeek: 21,
      endWeek: 25,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Thực hiện siêu âm hình thái học trước tuần 24', isRequired: true, category: null),
        _CatalogItem(text: 'Hoàn thành xét nghiệm/sàng lọc còn thiếu', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_ONCE_03',
      name: 'Sàng lọc đái tháo đường & Rh tuần 26 - 29',
      description: 'Xét nghiệm đường huyết thai kỳ và tiêm Anti-D nếu mẹ có Rh âm',
      stage: 'PREGNANCY',
      startWeek: 26,
      endWeek: 29,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Xét nghiệm đường huyết thai kỳ (OGTT)', isRequired: true, category: null),
        _CatalogItem(text: 'Kiểm tra lại kết quả nhóm máu & Rh', isRequired: true, category: null),
        _CatalogItem(text: 'Lên lịch tiêm Anti-D (nếu mẹ có Rh âm)', isRequired: false, category: null),
        _CatalogItem(text: 'Theo dõi hướng dẫn y tế cho mẹ Rh âm', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_ONCE_04',
      name: 'Kế hoạch sinh & chuẩn bị tuần 30 - 33',
      description: 'Tư vấn kế hoạch sinh, chọn nơi sinh và phương án cấp cứu',
      stage: 'PREGNANCY',
      startWeek: 30,
      endWeek: 33,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Tư vấn chi tiết về kế hoạch sinh', isRequired: true, category: null),
        _CatalogItem(text: 'Xác định cơ sở dự kiến sinh', isRequired: true, category: null),
        _CatalogItem(text: 'Lên kế hoạch xử trí tình huống khẩn cấp', isRequired: true, category: null),
        _CatalogItem(text: 'Tư vấn kế hoạch hóa gia đình sau sinh', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_ONCE_05',
      name: 'Sàng lọc GBS & nuôi con sữa mẹ tuần 36 - 37',
      description: 'Sàng lọc liên cầu khuẩn nhóm B (GBS) và tư vấn nuôi con bằng sữa mẹ',
      stage: 'PREGNANCY',
      startWeek: 36,
      endWeek: 37,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Sàng lọc liên cầu khuẩn nhóm B (GBS)', isRequired: true, category: null),
        _CatalogItem(text: 'Ghi nhận kết quả GBS và phác đồ xử trí', isRequired: true, category: null),
        _CatalogItem(text: 'Tư vấn nuôi con bằng sữa mẹ', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_ONCE_06',
      name: 'Chuẩn bị chuyển dạ tuần 38 - 39',
      description: 'Xác nhận nơi sinh, phương tiện di chuyển và nhận biết dấu hiệu chuyển dạ',
      stage: 'PREGNANCY',
      startWeek: 38,
      endWeek: 39,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Xác nhận cơ sở dự kiến sinh lần cuối', isRequired: true, category: null),
        _CatalogItem(text: 'Xác nhận phương tiện di chuyển khi chuyển dạ', isRequired: true, category: null),
        _CatalogItem(text: 'Xác nhận người hỗ trợ khi chuyển dạ', isRequired: true, category: null),
        _CatalogItem(text: 'Tìm hiểu các dấu hiệu chuyển dạ cần đến viện', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_ONCE_07',
      name: 'Chờ sinh & theo dõi từ tuần 40',
      description: 'Rà soát lần cuối kế hoạch sinh và kế hoạch theo dõi y tế nếu quá ngày dự sinh',
      stage: 'PREGNANCY',
      startWeek: 40,
      endWeek: 42,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Rà soát lần cuối kế hoạch sinh', isRequired: true, category: null),
        _CatalogItem(text: 'Rà soát phương án đi lại và hỗ trợ', isRequired: true, category: null),
        _CatalogItem(text: 'Trao đổi kế hoạch theo dõi nếu chưa sinh', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_WEEKLY_01',
      name: 'Theo dõi chỉ số mẹ bầu hàng tuần',
      description: 'Đo huyết áp, cân nặng và cập nhật BMI hàng tuần trong suốt thai kỳ',
      stage: 'PREGNANCY',
      startWeek: 1,
      endWeek: 42,
      repeatMode: 'WEEKLY',
      items: [
        _CatalogItem(text: 'Đo huyết áp hàng tuần', isRequired: true, category: null),
        _CatalogItem(text: 'Đo cân nặng và cập nhật chỉ số BMI', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_WEEKLY_02',
      name: 'Sàng lọc tiền sản giật hàng tuần (từ tuần 21)',
      description: 'Kiểm tra protein niệu để sàng lọc nguy cơ tiền sản giật từ tuần 21',
      stage: 'PREGNANCY',
      startWeek: 21,
      endWeek: 42,
      repeatMode: 'WEEKLY',
      items: [
        _CatalogItem(text: 'Kiểm tra protein niệu sàng lọc tiền sản giật', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_WEEKLY_03',
      name: 'Đếm cử động thai hàng tuần (từ tuần 30)',
      description: 'Theo dõi và ghi nhận cử động của thai nhi từ tuần 30',
      stage: 'PREGNANCY',
      startWeek: 30,
      endWeek: 42,
      repeatMode: 'WEEKLY',
      items: [
        _CatalogItem(text: 'Theo dõi và đếm cử động của thai nhi', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_DAILY_01',
      name: 'Uống Axit Folic hàng ngày (20 tuần đầu)',
      description: 'Bổ sung Axit Folic 400mcg/ngày trong 20 tuần đầu thai kỳ',
      stage: 'PREGNANCY',
      startWeek: 1,
      endWeek: 20,
      repeatMode: 'DAILY',
      items: [
        _CatalogItem(text: 'Bổ sung Axit Folic 400mcg/ngày', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'PREG_DAILY_02',
      name: 'Uống Axit Folic hàng ngày (từ tuần 21)',
      description: 'Bổ sung Axit Folic 600mcg/ngày từ tuần 21 đến khi sinh',
      stage: 'PREGNANCY',
      startWeek: 21,
      endWeek: 42,
      repeatMode: 'DAILY',
      items: [
        _CatalogItem(text: 'Bổ sung Axit Folic 600mcg/ngày', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'POST_WEEK_01',
      name: 'Chăm sóc mẹ sau sinh — Tuần 1',
      description: 'Sàng lọc trầm cảm, theo dõi hồi phục vết may/mổ và tư vấn tránh thai',
      stage: 'POSTPARTUM',
      startWeek: 1,
      endWeek: 1,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Đánh giá tâm trạng & sàng lọc trầm cảm sau sinh', isRequired: true, category: null),
        _CatalogItem(text: 'Theo dõi hồi phục vết may tầng sinh môn / vết mổ', isRequired: true, category: null),
        _CatalogItem(text: 'Tư vấn kế hoạch hóa gia đình sau sinh', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'POST_WEEK_02',
      name: 'Chăm sóc mẹ sau sinh — Tuần 2',
      description: 'Đánh giá dấu hiệu sinh tồn, co hồi tử cung, kiểm tra nhiễm trùng và dinh dưỡng',
      stage: 'POSTPARTUM',
      startWeek: 2,
      endWeek: 2,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Đánh giá sức khỏe thể chất của mẹ', isRequired: true, category: null),
        _CatalogItem(text: 'Sàng lọc sức khỏe tinh thần và trầm cảm', isRequired: true, category: null),
        _CatalogItem(text: 'Kiểm tra dấu hiệu nhiễm trùng sau sinh', isRequired: true, category: null),
        _CatalogItem(text: 'Tư vấn dinh dưỡng, vệ sinh & cho con bú', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'POST_WEEK_06',
      name: 'Khám kiểm tra sau sinh — Tuần 6',
      description: 'Khám sức khỏe toàn diện, đánh giá tâm thần và chăm sóc sức khỏe dài hạn',
      stage: 'POSTPARTUM',
      startWeek: 6,
      endWeek: 6,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám sức khỏe toàn diện cho mẹ mốc 6 tuần', isRequired: true, category: null),
        _CatalogItem(text: 'Đánh giá sức khỏe tâm thần mốc 6 tuần', isRequired: true, category: null),
        _CatalogItem(text: 'Tư vấn biện pháp tránh thai phù hợp', isRequired: false, category: null),
        _CatalogItem(text: 'Tìm hiểu chăm sóc sức khỏe dài hạn', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_0_28D',
      name: 'Chăm sóc trẻ sơ sinh 0–28 ngày',
      description: 'Khám sơ sinh, bú mẹ, giữ ấm, tiêm chủng viêm gan B/BCG và theo dõi rốn/vàng da',
      stage: 'BABY_CARE',
      startWeek: 1,
      endWeek: 4,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám và theo dõi sơ sinh', isRequired: true, category: null),
        _CatalogItem(text: 'Bú mẹ và giữ ấm', isRequired: true, category: null),
        _CatalogItem(text: 'Tiêm chủng sơ sinh (Viêm gan B, BCG)', isRequired: true, category: null),
        _CatalogItem(text: 'Theo dõi tăng trưởng, vàng da và rốn', isRequired: true, category: null),
        _CatalogItem(text: 'Tương tác sớm cùng bé', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_1_2M',
      name: 'Chăm sóc trẻ 1–<2 tháng',
      description: 'Khám mốc 6 tuần, duy trì bú mẹ hoàn toàn và chuẩn bị tiêm chủng mốc 2 tháng',
      stage: 'BABY_CARE',
      startWeek: 5,
      endWeek: 8,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám mốc 6 tuần', isRequired: true, category: null),
        _CatalogItem(text: 'Duy trì bú mẹ hoàn toàn', isRequired: true, category: null),
        _CatalogItem(text: 'Chuẩn bị mốc 2 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Giao tiếp và phát triển', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_2_3M',
      name: 'Chăm sóc trẻ 2–3 tháng',
      description: 'Khám sức khỏe, tiêm chủng vắc xin phối hợp liều 1/Rota/bại liệt và theo dõi tăng trưởng',
      stage: 'BABY_CARE',
      startWeek: 9,
      endWeek: 13,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám sức khỏe 2–3 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Tiêm chủng liều cơ bản mốc 2 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Bú mẹ hoàn toàn', isRequired: true, category: null),
        _CatalogItem(text: 'Theo dõi tăng trưởng và tương tác', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_4_6M',
      name: 'Chăm sóc trẻ 4–6 tháng',
      description: 'Tiêm chủng cơ bản liều tiếp theo, bú mẹ hoàn toàn và chuẩn bị ăn dặm mốc 6 tháng',
      stage: 'BABY_CARE',
      startWeek: 14,
      endWeek: 26,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám sức khỏe 4–6 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Hoàn thiện tiêm chủng giai đoạn đầu', isRequired: true, category: null),
        _CatalogItem(text: 'Bú mẹ hoàn toàn đến đủ 6 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Chuẩn bị ăn bổ sung (ăn dặm)', isRequired: false, category: null),
        _CatalogItem(text: 'Bắt đầu ăn bổ sung khi đủ 6 tháng', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_7_9M',
      name: 'Chăm sóc trẻ 7–9 tháng',
      description: 'Khám sức khỏe, ăn bổ sung 2-3 bữa/ngày, tiêm sởi/IPV2 và chuyển kết cấu thức ăn',
      stage: 'BABY_CARE',
      startWeek: 27,
      endWeek: 39,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám sức khỏe 7–9 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Ăn bổ sung 6–8 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Tăng kết cấu và tự ăn có giám sát', isRequired: false, category: null),
        _CatalogItem(text: 'Tiêm chủng mốc 9 tháng (Sởi, IPV2)', isRequired: true, category: null),
        _CatalogItem(text: 'Chuyển tần suất ăn sau 9 tháng', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_10_12M',
      name: 'Chăm sóc trẻ 10–12 tháng',
      description: 'Khám sức khỏe, rà soát lịch tiêm chủng và chuẩn bị kiểm tra toàn diện mốc 12 tháng',
      stage: 'BABY_CARE',
      startWeek: 40,
      endWeek: 52,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám sức khỏe 10–12 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Dinh dưỡng và bú mẹ mốc 10–12 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Chuyển dần sang thức ăn gia đình', isRequired: false, category: null),
        _CatalogItem(text: 'Rà soát lịch sử tiêm chủng', isRequired: true, category: null),
        _CatalogItem(text: 'Chuẩn bị kiểm tra mốc 12 tháng', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_12M',
      name: 'Mốc 12 tháng (1 tuổi)',
      description: 'Khám mốc 1 tuổi, tiêm Viêm não Nhật Bản B liều 1 và dinh dưỡng sau 1 tuổi',
      stage: 'BABY_CARE',
      startWeek: 52,
      endWeek: 52,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám mốc 12 tháng (1 tuổi)', isRequired: true, category: null),
        _CatalogItem(text: 'Tiêm Viêm não Nhật Bản B liều 1', isRequired: true, category: null),
        _CatalogItem(text: 'Dinh dưỡng sau 1 tuổi', isRequired: true, category: null),
        _CatalogItem(text: 'Vận động, giấc ngủ và tương tác', isRequired: false, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_13_18M',
      name: 'Chăm sóc trẻ 13–18 tháng',
      description: 'Khám sức khỏe, chăm sóc răng miệng, phát triển ngôn ngữ và tiêm nhắc mốc 18 tháng',
      stage: 'BABY_CARE',
      startWeek: 52,
      endWeek: 52,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám sức khỏe 13–18 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Ăn uống và tự ăn độc lập', isRequired: true, category: null),
        _CatalogItem(text: 'Vận động và giấc ngủ đều đặn', isRequired: false, category: null),
        _CatalogItem(text: 'Ngôn ngữ, chơi và phát triển giao tiếp', isRequired: false, category: null),
        _CatalogItem(text: 'Chăm sóc răng miệng với kem có fluor', isRequired: true, category: null),
        _CatalogItem(text: 'Tiêm chủng mốc 18 tháng (Sởi-Rubella, DPT nhắc lại)', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_19_24M',
      name: 'Chăm sóc trẻ 19–24 tháng',
      description: 'Khám sức khỏe, tiêm Viêm não Nhật Bản B liều 3 và chuẩn bị kiểm tra mốc 24 tháng',
      stage: 'BABY_CARE',
      startWeek: 52,
      endWeek: 52,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Khám sức khỏe 19–<24 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Dinh dưỡng và bú mẹ đến 2 tuổi', isRequired: true, category: null),
        _CatalogItem(text: 'Vận động, chơi và mở rộng giao tiếp', isRequired: false, category: null),
        _CatalogItem(text: 'Chăm sóc răng miệng hàng ngày', isRequired: true, category: null),
        _CatalogItem(text: 'Chuẩn bị và thực hiện kiểm tra 24 tháng', isRequired: true, category: null),
        _CatalogItem(text: 'Tiêm Viêm não Nhật Bản B liều 3', isRequired: true, category: null),
      ],
    ),
    _CatalogMilestone(
      code: 'BABY_SAFETY_0_24M',
      name: 'Dấu hiệu nguy hiểm cần cấp cứu (0–24 tháng)',
      description: 'Nhận biết các dấu hiệu nguy kịch để đưa trẻ đi cấp cứu/khám ngay lập tức',
      stage: 'BABY_CARE',
      startWeek: 1,
      endWeek: 52,
      repeatMode: 'NONE',
      items: [
        _CatalogItem(text: 'Dấu hiệu cần đưa trẻ đi khám/cấp cứu ngay', isRequired: true, category: null),
      ],
    ),
  ];
}

class _CatalogMilestone {
  final String code;
  final String name;
  final String description;
  final String stage;
  final int startWeek;
  final int endWeek;
  final String repeatMode;
  final List<_CatalogItem> items;

  const _CatalogMilestone({
    required this.code,
    required this.name,
    required this.description,
    required this.stage,
    required this.startWeek,
    required this.endWeek,
    required this.repeatMode,
    required this.items,
  });

  String get defaultCategory {
    if (stage == 'PRE_PREGNANCY') return 'Chuẩn bị mang thai';
    if (stage == 'POSTPARTUM') return 'Chăm sóc sau sinh';
    if (stage == 'BABY_CARE') return 'Chăm sóc em bé';
    if (code.contains('WEEKLY')) return 'Theo dõi định kỳ';
    if (code.contains('DAILY')) return 'Bổ sung vi chất';
    if (startWeek >= 38) return 'Chuyển dạ & Sinh';
    if (startWeek >= 30) return 'Kế hoạch sinh';
    if (startWeek >= 21) return 'Siêu âm & Xét nghiệm';
    return 'Khám thai & Sàng lọc';
  }
}

class _CatalogItem {
  final String text;
  final bool isRequired;
  final String? category;

  const _CatalogItem({
    required this.text,
    required this.isRequired,
    this.category,
  });
}