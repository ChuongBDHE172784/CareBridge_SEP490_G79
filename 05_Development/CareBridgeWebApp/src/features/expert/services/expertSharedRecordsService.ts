import apiClient from '../../../shared/api/apiClient';
import { listMyConversations, getTimeline } from '../../directChat/services/directChatApi';

export interface HealthMetricMeasurementRecord {
  measuredAt: string;
  value: string;
  unit: string;
  status: 'NORMAL' | 'WARNING' | 'CRITICAL';
  note?: string;
}

export interface HealthMetricItem {
  code: string;
  name: string;
  value: string;
  unit: string;
  status: 'NORMAL' | 'WARNING' | 'CRITICAL';
  icon?: string;
  measuredTime?: string;
  history?: HealthMetricMeasurementRecord[];
}

export interface HealthMetricsShareData {
  title: string;
  gestationalWeek?: number;
  measuredDate?: string;
  timeRangeLabel?: string;
  journeyId?: string;
  isLiveSync?: boolean;
  note?: string;
  metrics: HealthMetricItem[];
}

export interface ChecklistItemShareData {
  text: string;
  completed: boolean;
  category?: string;
  timeLabel?: string;
  origin?: 'SYSTEM' | 'USER' | 'EXPERT';
  createdBy?: 'SYSTEM' | 'USER' | 'EXPERT';
  isExpertCustom?: boolean;
  doctorNote?: string;
  sourceUrl?: string;
  supportFunction?: string;
}

export interface ChecklistShareData {
  title: string;
  gestationalWeek?: number;
  journeyId?: string;
  isLiveSync?: boolean;
  completedCount: number;
  totalCount: number;
  progressPercent: number;
  note?: string;
  historyItems?: ChecklistItemShareData[];
  currentItems?: ChecklistItemShareData[];
  futureItems?: ChecklistItemShareData[];
  items?: ChecklistItemShareData[];
}

export interface SharedRecordEntry {
  id: string;
  conversationId: string;
  motherUserId: string;
  motherName: string;
  motherAvatar?: string;
  motherPhone?: string;
  createdAt: string;
  type: 'HEALTH_METRICS' | 'CHECKLIST';
  healthData?: HealthMetricsShareData;
  checklistData?: ChecklistShareData;
  alertLevel: 'CRITICAL' | 'WARNING' | 'NORMAL';
  status: 'REVIEWED' | 'PENDING_REVIEW';
  expertFeedback?: string;
}

export const HEALTH_SHARE_TAG = '[CAREBRIDGE_HEALTH_SHARE]';
export const CHECKLIST_SHARE_TAG = '[CAREBRIDGE_CHECKLIST_SHARE]';

export function parseHealthMetricsShare(messageBody?: string): HealthMetricsShareData | null {
  if (!messageBody || !messageBody.trim().startsWith(HEALTH_SHARE_TAG)) return null;
  try {
    const jsonStr = messageBody.replace(HEALTH_SHARE_TAG, '').trim();
    return JSON.parse(jsonStr) as HealthMetricsShareData;
  } catch {
    return null;
  }
}

export const CAREBRIDGE_ROADMAP_TITLES = new Set([
  'xem xét tiền sử mang thai và sinh con trước',
  'xem xét tiền sử bệnh bản thân và gia đình',
  'đánh giá nguy cơ bệnh mạn tính và di truyền',
  'rà soát yếu tố nghề nghiệp và môi trường',
  'khám sức khỏe và khám phụ khoa định kỳ',
  'điều trị bệnh phụ khoa và nhiễm khuẩn (nếu có)',
  'kiểm soát bệnh mạn tính tiền thai kỳ',
  'rà soát thuốc và thực phẩm chức năng đang dùng',
  'ăn uống đa dạng, đủ chất và sử dụng muối iod',
  'duy trì cân nặng và chỉ số bmi hợp lý',
  'tập thể dục thường xuyên, nghỉ ngơi hợp lý',
  'tránh rượu bia, thuốc lá và chất kích thích',
  'tránh tiếp xúc hóa chất độc hại',
  'giữ vệ sinh và tẩy giun định kỳ',
  'bổ sung sắt và axit folic trước thai kỳ',
  'tư vấn axit folic liều cao nếu có tiền sử dị tật',
  'rà soát lịch sử tiêm chủng cá nhân',
  'tiêm các vắc-xin thiết yếu trước mang thai',
  'tuân thủ khoảng cách sau tiêm mmr và thủy đậu',
  'trang bị kiến thức làm mẹ và chăm sóc trẻ',
  'theo dõi chu kỳ kinh nguyệt để nhận biết ngày rụng trứng',
  'khuyến khích bạn đời duy trì lối sống lành mạnh',
  'hoàn tất sàng lọc và chuẩn bị tâm lý',
  'đi khám thai lần đầu',
  'xét nghiệm haemoglobin phát hiện thiếu máu',
  'xác định nhóm máu và tình trạng rh',
  'sàng lọc hiv, giang mai, viêm gan b',
  'sàng lọc dị tật bẩm sinh',
  'thực hiện siêu âm hình thái học trước tuần 24',
  'hoàn thành xét nghiệm/sàng lọc còn thiếu',
  'xét nghiệm đường huyết thai kỳ (ogtt)',
  'kiểm tra lại kết quả nhóm máu & rh',
  'lên lịch tiêm anti-d (nếu mẹ có rh âm)',
  'theo dõi hướng dẫn y tế cho mẹ rh âm',
  'tư vấn chi tiết về kế hoạch sinh',
  'xác định cơ sở dự kiến sinh',
  'lên kế hoạch xử trí tình huống khẩn cấp',
  'tư vấn kế hoạch hóa gia đình sau sinh',
  'sàng lọc liên cầu khuẩn nhóm b (gbs)',
  'ghi nhận kết quả gbs và phác đồ xử trí',
  'tư vấn nuôi con bằng sữa mẹ',
  'xác nhận cơ sở dự kiến sinh lần cuối',
  'xác nhận phương tiện di chuyển khi chuyển dạ',
  'xác nhận người hỗ trợ khi chuyển dạ',
  'tìm hiểu các dấu hiệu chuyển dạ cần đến viện',
  'rà soát lần cuối kế hoạch sinh',
  'rà soát phương án đi lại và hỗ trợ',
  'trao đổi kế hoạch theo dõi nếu chưa sinh',
  'đo huyết áp hàng tuần',
  'đo cân nặng và cập nhật chỉ số bmi',
  'kiểm tra protein niệu sàng lọc tiền sản giật',
  'theo dõi và đếm cử động của thai nhi',
  'bổ sung axit folic 400mcg/ngày',
  'bổ sung axit folic 600mcg/ngày',
  'đánh giá tâm trạng & sàng lọc trầm cảm sau sinh',
  'theo dõi hồi phục vết may tầng sinh môn / vết mổ',
  'đánh giá sức khỏe thể chất của mẹ',
  'sàng lọc sức khỏe tinh thần và trầm cảm',
  'kiểm tra dấu hiệu nhiễm trùng sau sinh',
  'tư vấn dinh dưỡng, vệ sinh & cho con bú',
  'khám sức khỏe toàn diện cho mẹ mốc 6 tuần',
  'đánh giá sức khỏe tâm thần mốc 6 tuần',
  'tìm hiểu chăm sóc sức khỏe dài hạn',
  'khám và theo dõi sơ sinh',
  'bú mẹ và giữ ấm',
  'tiêm chủng sơ sinh (viêm gan b, bcg)',
  'theo dõi tăng trưởng, vàng da và rốn',
  'tương tác sớm cùng bé',
  'khám mốc 6 tuần',
  'duy trì bú mẹ hoàn toàn',
  'chuẩn bị mốc 2 tháng',
  'giao tiếp và phát triển',
  'khám sức khỏe 2–3 tháng',
  'tiêm chủng liều cơ bản mốc 2 tháng',
  'theo dõi tăng trưởng và tương tác',
  'khám sức khỏe 4–6 tháng',
  'hoàn thiện tiêm chủng giai đoạn đầu',
  'bú mẹ hoàn toàn đến đủ 6 tháng',
  'chuẩn bị ăn bổ sung (ăn dặm)',
  'bắt đầu ăn bổ sung khi đủ 6 tháng',
  'khám sức khỏe 7–9 tháng',
  'ăn bổ sung 6–8 tháng',
  'tăng kết cấu và tự ăn có giám sát',
  'tiêm chủng mốc 9 tháng (sởi, ipv2)',
  'chuyển tần suất ăn sau 9 tháng',
  'khám sức khỏe 10–12 tháng',
  'dinh dưỡng và bú mẹ mốc 10–12 tháng',
  'chuyển dần sang thức ăn gia đình',
  'rà soát lịch sử tiêm chủng',
  'chuẩn bị kiểm tra mốc 12 tháng',
  'khám mốc 12 tháng (1 tuổi)',
  'tiêm viêm não nhật bản b liều 1',
  'dinh dưỡng sau 1 tuổi',
  'vận động, giấc ngủ và tương tác',
  'khám sức khỏe 13–18 tháng',
  'tiêm chủng mốc 18 tháng (sởi-rubella, dpt nhắc lại)',
  'chăm sóc răng miệng với kem có fluor',
  'khám sức khỏe 19–<24 tháng',
  'chuẩn bị và thực hiện kiểm tra 24 tháng',
  'tiêm viêm não nhật bản b liều 3',
  'dấu hiệu cần đưa trẻ đi khám/cấp cứu ngay',
]);

export function getTaskOriginCategory(item: {
  text?: string;
  origin?: string;
  createdBy?: string;
  isExpertCustom?: boolean;
}): 'USER' | 'CAREBRIDGE' {
  // 1. Do Expert chỉ định / thêm tùy biến -> Xếp vào Gợi ý CareBridge
  if (
    item.isExpertCustom ||
    item.origin === 'EXPERT' ||
    (item.origin as string) === 'EXPERT_CUSTOM' ||
    item.createdBy === 'EXPERT'
  ) {
    return 'CAREBRIDGE';
  }

  // 2. Do Mẹ / Người dùng tự tạo -> Việc cá nhân
  if (
    item.origin === 'USER' ||
    (item.origin as string) === 'USER_CREATED' ||
    item.createdBy === 'USER'
  ) {
    return 'USER';
  }

  // 3. Nếu nằm trong bộ 34 danh mục lộ trình chuẩn CareBridge -> Gợi ý CareBridge
  if (item.text && CAREBRIDGE_ROADMAP_TITLES.has(item.text.trim().toLowerCase())) {
    return 'CAREBRIDGE';
  }

  // 4. Mặc định các việc tự tạo khác không thuộc lộ trình chuẩn -> Việc cá nhân
  return 'USER';
}

export function parseChecklistShare(messageBody?: string): ChecklistShareData | null {
  if (!messageBody || !messageBody.trim().startsWith(CHECKLIST_SHARE_TAG)) return null;
  try {
    const jsonStr = messageBody.replace(CHECKLIST_SHARE_TAG, '').trim();
    const parsed = JSON.parse(jsonStr) as ChecklistShareData;
    const historyList = (parsed.historyItems || []).map((h) => {
      const isExp = h.isExpertCustom || h.origin === 'EXPERT' || h.createdBy === 'EXPERT';
      const originCat = getTaskOriginCategory(h);
      return {
        ...h,
        origin: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
        createdBy: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
        isExpertCustom: isExp,
      };
    });
    const futureList = (parsed.futureItems || []).map((f) => {
      const isExp = f.isExpertCustom || f.origin === 'EXPERT' || f.createdBy === 'EXPERT';
      const originCat = getTaskOriginCategory(f);
      return {
        ...f,
        origin: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
        createdBy: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
        isExpertCustom: isExp,
      };
    });
    let currentList = (parsed.currentItems || parsed.items || []).map((c) => {
      const isExp = c.isExpertCustom || c.origin === 'EXPERT' || c.createdBy === 'EXPERT';
      const originCat = getTaskOriginCategory(c);
      return {
        ...c,
        origin: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
        createdBy: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
        isExpertCustom: isExp,
      };
    });

    // Eliminate duplicate / history items from currentItems
    const historyTextSet = new Set(historyList.map((h) => h.text.trim().toLowerCase()));
    currentList = currentList.filter((c) => !historyTextSet.has(c.text.trim().toLowerCase()));

    // Deduplicate within currentItems
    const seenCurrent = new Set<string>();
    currentList = currentList.filter((c) => {
      const key = c.text.trim().toLowerCase();
      if (seenCurrent.has(key)) return false;
      seenCurrent.add(key);
      return true;
    });

    parsed.historyItems = historyList;
    parsed.futureItems = futureList;
    parsed.currentItems = currentList;
    parsed.items = currentList;

    const allItems = [...historyList, ...currentList, ...futureList];
    parsed.totalCount = allItems.length > 0 ? allItems.length : parsed.totalCount;
    parsed.completedCount = allItems.filter((i) => i.completed).length;
    parsed.progressPercent =
      parsed.totalCount > 0 ? Math.round((parsed.completedCount / parsed.totalCount) * 100) : 0;

    return parsed;
  } catch {
    return null;
  }
}

export function evaluateMetricStatus(
  code: string,
  valNumeric?: number,
  valSecondary?: number
): 'NORMAL' | 'WARNING' | 'CRITICAL' {
  if (valNumeric == null) return 'NORMAL';
  switch (code) {
    case 'BLOOD_PRESSURE':
      if (valNumeric >= 160 || (valSecondary != null && valSecondary >= 110)) return 'CRITICAL';
      if (valNumeric >= 140 || (valSecondary != null && valSecondary >= 90)) return 'WARNING';
      if (valNumeric < 90 || (valSecondary != null && valSecondary < 60)) return 'WARNING';
      return 'NORMAL';
    case 'BLOOD_GLUCOSE':
      if (valNumeric >= 11.1) return 'CRITICAL';
      if (valNumeric >= 7.0 || valNumeric < 3.9) return 'WARNING';
      return 'NORMAL';
    case 'TEMPERATURE':
      if (valNumeric >= 39.0) return 'CRITICAL';
      if (valNumeric >= 38.0 || valNumeric < 35.5) return 'WARNING';
      return 'NORMAL';
    case 'MATERNAL_HEART_RATE':
    case 'HEART_RATE':
      if (valNumeric >= 120) return 'CRITICAL';
      if (valNumeric >= 100 || valNumeric < 50) return 'WARNING';
      return 'NORMAL';
    default:
      return 'NORMAL';
  }
}

export async function syncLiveHealthMetrics(healthData: HealthMetricsShareData): Promise<HealthMetricsShareData> {
  if (!healthData.journeyId || healthData.isLiveSync === false) {
    return healthData;
  }

  try {
    const updatedMetrics = await Promise.all(
      healthData.metrics.map(async (metric) => {
        try {
          const res = await apiClient.get<{
            data: {
              unit?: string;
              dataPoints: Array<{
                measuredAt: string;
                valueNumeric: number;
                valueSecondary?: number;
                note?: string;
              }>;
            };
          }>(`/api/v1/journeys/${healthData.journeyId}/metrics`, {
            params: { metricType: metric.code },
          });

          const dataPoints = res.data?.data?.dataPoints || [];
          if (dataPoints.length === 0) return metric;

          const sorted = [...dataPoints].sort(
            (a, b) => new Date(b.measuredAt).getTime() - new Date(a.measuredAt).getTime()
          );
          const latest = sorted[0];
          const latestVal =
            latest.valueSecondary != null
              ? `${latest.valueNumeric}/${latest.valueSecondary}`
              : `${latest.valueNumeric}`;

          const history: HealthMetricMeasurementRecord[] = sorted.map((p) => {
            const valStr =
              p.valueSecondary != null ? `${p.valueNumeric}/${p.valueSecondary}` : `${p.valueNumeric}`;
            const dt = new Date(p.measuredAt);
            const timeStr = `${dt.getDate().toString().padStart(2, '0')}/${(dt.getMonth() + 1)
              .toString()
              .padStart(2, '0')} ${dt.getHours().toString().padStart(2, '0')}:${dt
              .getMinutes()
              .toString()
              .padStart(2, '0')}`;

            return {
              measuredAt: timeStr,
              value: valStr,
              unit: res.data?.data?.unit || metric.unit,
              status: evaluateMetricStatus(metric.code, p.valueNumeric, p.valueSecondary),
              note: p.note,
            };
          });

          const latestDt = new Date(latest.measuredAt);
          const measuredTime = `${latestDt.getDate().toString().padStart(2, '0')}/${(latestDt.getMonth() + 1)
            .toString()
            .padStart(2, '0')} ${latestDt.getHours().toString().padStart(2, '0')}:${latestDt
            .getMinutes()
            .toString()
            .padStart(2, '0')}`;

          return {
            ...metric,
            value: latestVal,
            unit: res.data?.data?.unit || metric.unit,
            measuredTime,
            status: evaluateMetricStatus(metric.code, latest.valueNumeric, latest.valueSecondary),
            history,
          };
        } catch {
          return metric;
        }
      })
    );

    return {
      ...healthData,
      metrics: updatedMetrics,
    };
  } catch {
    return healthData;
  }
}

export async function syncLiveChecklist(
  checklistData: ChecklistShareData,
  motherUserId?: string
): Promise<ChecklistShareData> {
  try {
    let res: { data?: { sections?: { overdue?: any[]; today?: any[]; upcoming?: any[]; unscheduled?: any[] } } } | null = null;

    if (checklistData.journeyId) {
      res = await apiClient.get(`/api/v1/checklists/journeys/${checklistData.journeyId}/tasks`);
    } else if (motherUserId) {
      res = await apiClient.get(`/api/v1/checklists/users/${motherUserId}/tasks`);
    }

    if (res?.data?.sections) {
      const allTasks = [
        ...(res.data.sections.overdue || []),
        ...(res.data.sections.today || []),
        ...(res.data.sections.upcoming || []),
        ...(res.data.sections.unscheduled || []),
      ];

      if (allTasks.length > 0) {
        const taskStatusMap = new Map<string, boolean>();
        for (const t of allTasks) {
          const isDone =
            t.taskStatus === 'COMPLETED' ||
            t.status === 'COMPLETED' ||
            t.status === 'DONE';
          taskStatusMap.set(t.title.trim().toLowerCase(), isDone);
        }

        // Update currentItems with live task status
        const updatedCurrent = (checklistData.currentItems || checklistData.items || []).map((item) => {
          const key = item.text.trim().toLowerCase();
          const isExp = item.isExpertCustom || item.origin === 'EXPERT' || item.createdBy === 'EXPERT';
          const originCat = getTaskOriginCategory(item);
          return {
            ...item,
            completed: taskStatusMap.has(key) ? taskStatusMap.get(key)! : item.completed,
            origin: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
            createdBy: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
            isExpertCustom: isExp,
          };
        });

        const historyList = checklistData.historyItems || [];
        const futureList = checklistData.futureItems || [];
        const historyTextSet = new Set(historyList.map((h) => h.text.trim().toLowerCase()));
        const filteredCurrent = updatedCurrent.filter((c) => !historyTextSet.has(c.text.trim().toLowerCase()));

        const allItems = [...historyList, ...filteredCurrent, ...futureList];
        const completedCount = allItems.filter((i) => i.completed).length;
        const totalCount = allItems.length;
        const progressPercent = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

        return {
          ...checklistData,
          currentItems: filteredCurrent,
          historyItems: historyList,
          futureItems: futureList,
          items: filteredCurrent,
          completedCount,
          totalCount,
          progressPercent,
          isLiveSync: true,
        };
      }
    }
  } catch (err) {
    console.warn('Failed to fetch live checklist status:', err);
  }
  return checklistData;
}

export async function fetchExpertSharedRecords(): Promise<SharedRecordEntry[]> {
  try {
    const conversations = await listMyConversations();
    const records: SharedRecordEntry[] = [];

    // Fetch messages from conversations in parallel
    const timelineResults = await Promise.allSettled(
      conversations.map(async (c) => {
        const timeline = await getTimeline(c.conversationId, { limit: 50 });
        return { conversation: c, timeline };
      })
    );

    for (const res of timelineResults) {
      if (res.status !== 'fulfilled') continue;
      const { conversation, timeline } = res.value;
      const counterpartId = conversation.counterpartUserId;
      const motherDisplayName = `Mẹ bầu (${counterpartId.slice(0, 8)})`;

      for (const item of timeline.items) {
        if (item.kind !== 'MESSAGE' || item.recalledAt || !item.messageBody) continue;

        const rawHealthData = parseHealthMetricsShare(item.messageBody);
        if (rawHealthData) {
          // Perform live sync from backend
          const healthData = await syncLiveHealthMetrics(rawHealthData);

          let alertLevel: 'CRITICAL' | 'WARNING' | 'NORMAL' = 'NORMAL';
          for (const m of healthData.metrics) {
            if (m.status === 'CRITICAL') {
              alertLevel = 'CRITICAL';
              break;
            }
            if (m.status === 'WARNING') {
              alertLevel = 'WARNING';
            }
          }

          records.push({
            id: item.messageId || item.clientMessageId || `rec-${Date.now()}`,
            conversationId: conversation.conversationId,
            motherUserId: counterpartId,
            motherName: motherDisplayName,
            createdAt: item.createdAt || new Date().toISOString(),
            type: 'HEALTH_METRICS',
            healthData,
            alertLevel,
            status: 'PENDING_REVIEW',
          });
          continue;
        }

        const rawChecklistData = parseChecklistShare(item.messageBody);
        if (rawChecklistData) {
          const checklistData = await syncLiveChecklist(rawChecklistData, counterpartId);
          records.push({
            id: item.messageId || item.clientMessageId || `rec-${Date.now()}`,
            conversationId: conversation.conversationId,
            motherUserId: counterpartId,
            motherName: motherDisplayName,
            createdAt: item.createdAt || new Date().toISOString(),
            type: 'CHECKLIST',
            checklistData,
            alertLevel: checklistData.progressPercent < 50 ? 'WARNING' : 'NORMAL',
            status: 'PENDING_REVIEW',
          });
        }
      }
    }

    // Sort by newest first
    return records.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  } catch (error) {
    console.error('Failed to fetch expert shared records', error);
    return [];
  }
}

export async function savePersonalizedChecklist(
  conversationId: string,
  updatedChecklist: ChecklistShareData,
  doctorActionNote?: string
): Promise<ChecklistShareData> {
  const historyList = (updatedChecklist.historyItems || []).map((h) => {
    const isExp = h.isExpertCustom || h.origin === 'EXPERT' || h.createdBy === 'EXPERT';
    const originCat = getTaskOriginCategory(h);
    return {
      ...h,
      origin: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
      createdBy: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
      isExpertCustom: isExp,
    };
  });
  const futureList = (updatedChecklist.futureItems || []).map((f) => {
    const isExp = f.isExpertCustom || f.origin === 'EXPERT' || f.createdBy === 'EXPERT';
    const originCat = getTaskOriginCategory(f);
    return {
      ...f,
      origin: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
      createdBy: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
      isExpertCustom: isExp,
    };
  });
  let currentList = (updatedChecklist.currentItems || updatedChecklist.items || []).map((c) => {
    const isExp = c.isExpertCustom || c.origin === 'EXPERT' || c.createdBy === 'EXPERT';
    const originCat = getTaskOriginCategory(c);
    return {
      ...c,
      origin: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
      createdBy: (originCat === 'CAREBRIDGE' ? (isExp ? 'EXPERT' : 'SYSTEM') : 'USER') as 'SYSTEM' | 'USER' | 'EXPERT',
      isExpertCustom: isExp,
    };
  });

  const historyTextSet = new Set(historyList.map((h) => h.text.trim().toLowerCase()));
  currentList = currentList.filter((c) => !historyTextSet.has(c.text.trim().toLowerCase()));

  const seenCurrent = new Set<string>();
  currentList = currentList.filter((c) => {
    const key = c.text.trim().toLowerCase();
    if (seenCurrent.has(key)) return false;
    seenCurrent.add(key);
    return true;
  });

  const allItems = [...currentList, ...historyList, ...futureList];
  const completedCount = allItems.filter((i) => i.completed).length;
  const totalCount = allItems.length;
  const progressPercent = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

  const payload: ChecklistShareData = {
    ...updatedChecklist,
    currentItems: currentList,
    historyItems: historyList,
    futureItems: futureList,
    items: currentList,
    completedCount,
    totalCount,
    progressPercent,
    isLiveSync: true,
    note: doctorActionNote || updatedChecklist.note,
  };

  const messageBody = `${CHECKLIST_SHARE_TAG}\n${JSON.stringify(payload, null, 2)}`;
  const clientMessageId =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
          const r = (Math.random() * 16) | 0;
          const v = c === 'x' ? r : (r & 0x3) | 0x8;
          return v.toString(16);
        });

  // Import sendMessage dynamically or directly to avoid circular dependency
  const { sendMessage } = await import('../../directChat/services/directChatApi');
  await sendMessage(conversationId, clientMessageId, messageBody, 'TEXT');

  return payload;
}

export async function addChecklistItemToSharedRecord(
  conversationId: string,
  currentChecklist: ChecklistShareData,
  newItem: ChecklistItemShareData,
  targetGroup: 'CURRENT' | 'FUTURE' | 'HISTORY',
  doctorNote?: string
): Promise<ChecklistShareData> {
  const itemToSave: ChecklistItemShareData = {
    ...newItem,
    origin: 'EXPERT',
    createdBy: 'EXPERT',
    isExpertCustom: true,
    doctorNote: doctorNote || newItem.doctorNote,
  };

  const updated: ChecklistShareData = {
    ...currentChecklist,
    currentItems: [...(currentChecklist.currentItems || currentChecklist.items || [])],
    historyItems: [...(currentChecklist.historyItems || [])],
    futureItems: [...(currentChecklist.futureItems || [])],
  };

  if (targetGroup === 'CURRENT') {
    updated.currentItems!.push(itemToSave);
  } else if (targetGroup === 'FUTURE') {
    updated.futureItems!.push(itemToSave);
  } else {
    updated.historyItems!.push(itemToSave);
  }

  return await savePersonalizedChecklist(conversationId, updated, doctorNote);
}

export async function editChecklistItemInSharedRecord(
  conversationId: string,
  currentChecklist: ChecklistShareData,
  targetGroup: 'CURRENT' | 'FUTURE' | 'HISTORY',
  itemIndex: number,
  updatedItem: ChecklistItemShareData,
  doctorNote?: string,
  originalItemText?: string
): Promise<ChecklistShareData> {
  const itemToSave: ChecklistItemShareData = {
    ...updatedItem,
    origin: 'EXPERT',
    createdBy: 'EXPERT',
    isExpertCustom: true,
    doctorNote: doctorNote || updatedItem.doctorNote,
  };

  const updated: ChecklistShareData = {
    ...currentChecklist,
    currentItems: [...(currentChecklist.currentItems || currentChecklist.items || [])],
    historyItems: [...(currentChecklist.historyItems || [])],
    futureItems: [...(currentChecklist.futureItems || [])],
  };

  const targetList =
    targetGroup === 'CURRENT'
      ? updated.currentItems!
      : targetGroup === 'FUTURE'
      ? updated.futureItems!
      : updated.historyItems!;

  let targetIdx = itemIndex;
  if (originalItemText) {
    const foundIdx = targetList.findIndex(
      (i) => i.text.trim().toLowerCase() === originalItemText.trim().toLowerCase()
    );
    if (foundIdx >= 0) targetIdx = foundIdx;
  }

  if (targetIdx >= 0 && targetIdx < targetList.length) {
    targetList[targetIdx] = itemToSave;
  } else {
    targetList.push(itemToSave);
  }

  return await savePersonalizedChecklist(conversationId, updated, doctorNote);
}

export async function deleteChecklistItemFromSharedRecord(
  conversationId: string,
  currentChecklist: ChecklistShareData,
  targetGroup: 'CURRENT' | 'FUTURE' | 'HISTORY',
  itemIndex: number,
  doctorNote?: string,
  itemText?: string
): Promise<ChecklistShareData> {
  const updated: ChecklistShareData = {
    ...currentChecklist,
    currentItems: [...(currentChecklist.currentItems || currentChecklist.items || [])],
    historyItems: [...(currentChecklist.historyItems || [])],
    futureItems: [...(currentChecklist.futureItems || [])],
  };

  const targetList =
    targetGroup === 'CURRENT'
      ? updated.currentItems!
      : targetGroup === 'FUTURE'
      ? updated.futureItems!
      : updated.historyItems!;

  let targetIdx = itemIndex;
  if (itemText) {
    const foundIdx = targetList.findIndex(
      (i) => i.text.trim().toLowerCase() === itemText.trim().toLowerCase()
    );
    if (foundIdx >= 0) targetIdx = foundIdx;
  }

  if (targetIdx >= 0 && targetIdx < targetList.length) {
    targetList.splice(targetIdx, 1);
  }

  return await savePersonalizedChecklist(conversationId, updated, doctorNote);
}

