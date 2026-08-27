import type { ContentStage, ContentType, RecommendationTag } from '../models/content';

export const RECOMMENDATION_TAG_VIETNAMESE_LABELS: Record<string, string> = {
  'rec-activity-high': 'Vận động: Cao',
  'rec-activity-low': 'Vận động: Thấp',
  'rec-activity-moderate': 'Vận động: Trung bình',
  'rec-age-18-24': 'Độ tuổi: 18 - 24',
  'rec-age-25-34': 'Độ tuổi: 25 - 34',
  'rec-age-35-39': 'Độ tuổi: 35 - 39',
  'rec-age-40-plus': 'Độ tuổi: 40 trở lên',
  'rec-age-under-18': 'Độ tuổi: Dưới 18',
  'rec-alcohol-less-than-weekly': 'Rượu bia: Dưới 1 lần/tuần',
  'rec-alcohol-none': 'Rượu bia: Không sử dụng',
  'rec-alcohol-use': 'Rượu bia: Có sử dụng',
  'rec-alcohol-weekly-or-more': 'Rượu bia: Từ 1 lần/tuần trở lên',
  'rec-bmi-healthy-range': 'Chỉ số BMI: Bình thường (18.5 - 22.9)',
  'rec-bmi-obesity': 'Chỉ số BMI: Béo phì (≥ 25)',
  'rec-bmi-overweight': 'Chỉ số BMI: Thừa cân (23 - 24.9)',
  'rec-bmi-underweight': 'Chỉ số BMI: Thiếu cân (< 18.5)',
  'rec-condition-anemia': 'Bệnh nền: Thiếu máu',
  'rec-condition-asthma': 'Bệnh nền: Hen suyễn',
  'rec-condition-autoimmune-disease': 'Bệnh nền: Bệnh tự miễn',
  'rec-condition-cardiovascular-disease': 'Bệnh nền: Bệnh tim mạch',
  'rec-condition-diabetes': 'Bệnh nền: Đái tháo đường',
  'rec-condition-endometriosis': 'Bệnh nền: Lạc nội mạc tử cung',
  'rec-condition-epilepsy': 'Bệnh nền: Động kinh',
  'rec-condition-hypertension': 'Bệnh nền: Tăng huyết áp',
  'rec-condition-infertility': 'Bệnh nền: Vô sinh / hiếm muộn',
  'rec-condition-kidney-disease': 'Bệnh nền: Bệnh thận',
  'rec-condition-lupus': 'Bệnh nền: Lupus ban đỏ',
  'rec-condition-mental-health': 'Bệnh nền: Sức khỏe tâm thần',
  'rec-condition-other-clinician-confirmed': 'Bệnh nền: Bệnh lý khác (bác sĩ chẩn đoán)',
  'rec-condition-pcos': 'Bệnh nền: Buồng trứng đa nang (PCOS)',
  'rec-condition-thyroid-disorder': 'Bệnh nền: Rối loạn tuyến giáp',
  'rec-lifestyle-stress': 'Lối sống: Căng thẳng / Stress',
  'rec-lifestyle-substance-use': 'Lối sống: Sử dụng chất kích thích',
  'rec-lifestyle-unhealthy-diet': 'Lối sống: Chế độ ăn chưa lành mạnh',
  'rec-medication-adjustment-needed': 'Thuốc: Cần điều chỉnh đơn thuốc',
  'rec-medication-anticoagulant': 'Thuốc: Thuốc chống đông',
  'rec-medication-antiepileptic': 'Thuốc: Thuốc chống động kinh',
  'rec-medication-antihypertensive': 'Thuốc: Thuốc hạ huyết áp',
  'rec-medication-diabetes': 'Thuốc: Thuốc trị đái tháo đường',
  'rec-medication-folic-acid': 'Thuốc: Đang uống Axit Folic',
  'rec-medication-high-risk-or-contraindicated': 'Thuốc: Thuốc nguy cơ cao / chống chỉ định',
  'rec-medication-iron': 'Thuốc: Đang bổ sung Sắt',
  'rec-medication-mental-health': 'Thuốc: Thuốc điều trị tâm thần',
  'rec-medication-other-prescribed': 'Thuốc: Thuốc kê đơn khác',
  'rec-medication-prenatal-vitamin': 'Thuốc: Vitamin tổng hợp thai kỳ',
  'rec-medication-thyroid': 'Thuốc: Thuốc điều trị tuyến giáp',
  'rec-nutrition-calcium-review': 'Dinh dưỡng: Đánh giá bổ sung Canxi',
  'rec-nutrition-folic-acid-needed': 'Dinh dưỡng: Cần bổ sung Axit Folic',
  'rec-nutrition-food-insecurity': 'Dinh dưỡng: Thiếu hụt nguồn thực phẩm',
  'rec-nutrition-iodine-review': 'Dinh dưỡng: Đánh giá bổ sung I-ốt',
  'rec-nutrition-iron-or-folate-concern': 'Dinh dưỡng: Thiếu máu do thiếu Sắt/Folate',
  'rec-nutrition-iron-review': 'Dinh dưỡng: Đánh giá bổ sung Sắt',
  'rec-nutrition-low-appetite': 'Dinh dưỡng: Kém ăn / chán ăn',
  'rec-nutrition-nausea-or-vomiting': 'Dinh dưỡng: Buồn nôn / nôn nghén',
  'rec-nutrition-other-concern': 'Dinh dưỡng: Vấn đề ăn uống khác',
  'rec-nutrition-vegan': 'Dinh dưỡng: Chế độ ăn thuần chay',
  'rec-nutrition-vegetarian': 'Dinh dưỡng: Chế độ ăn chay',
  'rec-nutrition-vitamin-d-review': 'Dinh dưỡng: Đánh giá bổ sung Vitamin D',
  'rec-preference-appointment-reminders': 'Nhu cầu: Nhắc lịch khám thai',
  'rec-preference-mental-wellbeing': 'Nhu cầu: Chăm sóc sức khỏe tinh thần',
  'rec-preference-nutrition': 'Nhu cầu: Tư vấn chế độ dinh dưỡng',
  'rec-preference-physical-activity': 'Nhu cầu: Hướng dẫn vận động thể chất',
  'rec-reproductive-ectopic-pregnancy': 'Tiền sử sinh sản: Thai ngoài tử cung',
  'rec-reproductive-gestational-diabetes': 'Tiền sử sinh sản: Tiểu đường thai kỳ',
  'rec-reproductive-no-prior-pregnancy': 'Tiền sử sinh sản: Mang thai lần đầu',
  'rec-reproductive-other-history': 'Tiền sử sinh sản: Bệnh sử sản khoa khác',
  'rec-reproductive-preeclampsia': 'Tiền sử sinh sản: Tiền sản giật',
  'rec-reproductive-prior-live-birth': 'Tiền sử sinh sản: Đã từng sinh con khỏe mạnh',
  'rec-reproductive-prior-multiple-pregnancy': 'Tiền sử sinh sản: Đã từng mang đa thai',
  'rec-reproductive-prior-pregnancy-loss': 'Tiền sử sinh sản: Đã từng sảy thai',
  'rec-reproductive-prior-preterm-birth': 'Tiền sử sinh sản: Đã từng sinh non',
  'rec-reproductive-prior-stillbirth': 'Tiền sử sinh sản: Đã từng thai lưu',
  'rec-reproductive-recurrent-pregnancy-loss': 'Tiền sử sinh sản: Sảy thai liên tiếp',
  'rec-sexual-health-contraception-or-fertility': 'Sức khỏe tình dục: Tránh thai & khả năng thụ thai',
  'rec-sexual-health-general-information': 'Sức khỏe tình dục: Thông tin chung',
  'rec-sexual-health-intimacy-during-lifecycle': 'Sức khỏe tình dục: Sinh hoạt theo giai đoạn thai kỳ',
  'rec-sexual-health-no-pregnancy-plan': 'Sức khỏe tình dục: Chưa có kế hoạch mang thai',
  'rec-sexual-health-other-non-urgent': 'Sức khỏe tình dục: Vấn đề khác',
  'rec-sexual-health-reproductive-tract-infection': 'Sức khỏe tình dục: Viêm nhiễm phụ khoa',
  'rec-sexual-health-safe-sex-counseling': 'Sức khỏe tình dục: Tư vấn quan hệ an toàn',
  'rec-sexual-health-sti-risk': 'Sức khỏe tình dục: Nguy cơ mắc bệnh lây truyền qua đường tình dục',
  'rec-sexual-health-sti-suspected-or-known': 'Sức khỏe tình dục: Nghi ngờ hoặc đã mắc STI',
  'rec-sleep-concern': 'Giấc ngủ: Rối loạn / mất ngủ / lo ngại',
  'rec-sleep-no-concern': 'Giấc ngủ: Bình thường / không lo ngại',
  'rec-smoking-current': 'Hút thuốc: Đang hút thuốc',
  'rec-smoking-former': 'Hút thuốc: Đã cai thuốc',
  'rec-smoking-never': 'Hút thuốc: Chưa từng hút',
  'rec-sti-chlamydia': 'STI: Bệnh Chlamydia',
  'rec-sti-current-or-treatment': 'STI: Đang mắc / đang điều trị',
  'rec-sti-gonorrhea': 'STI: Bệnh Lậu',
  'rec-sti-hepatitis-b': 'STI: Viêm gan B',
  'rec-sti-hepatitis-c': 'STI: Viêm gan C',
  'rec-sti-herpes': 'STI: Mụn rộp sinh dục (Herpes)',
  'rec-sti-hiv': 'STI: Nhiễm HIV',
  'rec-sti-hpv': 'STI: Nhiễm HPV',
  'rec-sti-other': 'STI: Bệnh lây qua đường tình dục khác',
  'rec-sti-past-history': 'STI: Tiền sử từng mắc',
  'rec-sti-risk': 'STI: Có nguy cơ lây nhiễm',
  'rec-sti-screening-information': 'STI: Thông tin xét nghiệm tầm soát',
  'rec-sti-suspected-or-known': 'STI: Nghi ngờ hoặc đã xác định mắc bệnh',
  'rec-sti-syphilis': 'STI: Bệnh Giang mai',
  'rec-vaccination-assessment-needed': 'Tiêm chủng: Cần đánh giá lịch tiêm',
  'rec-vaccination-covid-19-due': 'Tiêm chủng: Đến lịch tiêm COVID-19',
  'rec-vaccination-hepatitis-b-due': 'Tiêm chủng: Đến lịch tiêm Viêm gan B',
  'rec-vaccination-influenza-due': 'Tiêm chủng: Đến lịch tiêm Cúm',
  'rec-vaccination-rubella-immunity-review': 'Tiêm chủng: Đánh giá miễn dịch Rubella',
  'rec-vaccination-tdap-due': 'Tiêm chủng: Đến lịch tiêm Uốn ván - Bạch hầu - Ho gà (Tdap)',
};

export function formatRecommendationTagLabel(tag: { slug?: string; label?: string; name?: string }): string {
  if (tag.slug && RECOMMENDATION_TAG_VIETNAMESE_LABELS[tag.slug]) {
    return RECOMMENDATION_TAG_VIETNAMESE_LABELS[tag.slug];
  }
  return tag.label || tag.name || tag.slug || '';
}

export interface TagGroup {
  id: string;
  name: string;
  prefix: string;
}

export const TAG_GROUPS: TagGroup[] = [
  { id: 'BMI', name: 'Chỉ số BMI', prefix: 'rec-bmi-' },
  { id: 'AGE', name: 'Độ tuổi', prefix: 'rec-age-' },
  { id: 'ACTIVITY', name: 'Vận động thể chất', prefix: 'rec-activity-' },
  { id: 'NUTRITION', name: 'Chế độ dinh dưỡng', prefix: 'rec-nutrition-' },
  { id: 'CONDITION', name: 'Bệnh lý nền', prefix: 'rec-condition-' },
  { id: 'MEDICATION', name: 'Thuốc đang sử dụng', prefix: 'rec-medication-' },
  { id: 'REPRODUCTIVE', name: 'Tiền sử sinh sản', prefix: 'rec-reproductive-' },
  { id: 'VACCINATION', name: 'Tiêm chủng', prefix: 'rec-vaccination-' },
  { id: 'SMOKING', name: 'Hút thuốc lá', prefix: 'rec-smoking-' },
  { id: 'ALCOHOL', name: 'Rượu bia', prefix: 'rec-alcohol-' },
  { id: 'SLEEP', name: 'Giấc ngủ', prefix: 'rec-sleep-' },
  { id: 'LIFESTYLE', name: 'Lối sống khác', prefix: 'rec-lifestyle-' },
  { id: 'STI', name: 'Bệnh STI', prefix: 'rec-sti-' },
  { id: 'SEXUAL_HEALTH', name: 'Sức khỏe tình dục', prefix: 'rec-sexual-health-' },
  { id: 'PREFERENCE', name: 'Nhu cầu quan tâm', prefix: 'rec-preference-' },
];

export function getGroupForTag(tag: { slug?: string }): TagGroup | undefined {
  if (!tag.slug) return undefined;
  return TAG_GROUPS.find((g) => tag.slug?.startsWith(g.prefix));
}

export function formatTagOptionLabel(tag: { slug?: string; label?: string; name?: string }): string {
  const fullLabel = formatRecommendationTagLabel(tag);
  const colonIdx = fullLabel.indexOf(':');
  if (colonIdx !== -1) {
    return fullLabel.slice(colonIdx + 1).trim();
  }
  return fullLabel;
}

const EXCLUSIVE_GROUPS: Array<[string, string]> = [
  ['rec-age-', 'AGE'],
  ['rec-bmi-', 'BMI'],
  ['rec-smoking-', 'SMOKING'],
  ['rec-alcohol-', 'ALCOHOL'],
  ['rec-activity-', 'ACTIVITY'],
  ['rec-sleep-', 'SLEEP'],
  ['rec-sti-screening-information', 'STI_STATUS'],
  ['rec-sti-past-history', 'STI_STATUS'],
  ['rec-sti-current-or-treatment', 'STI_STATUS'],
  ['rec-sti-risk', 'STI_STATUS'],
  ['rec-sti-suspected-or-known', 'STI_STATUS'],
];

function groupForSlug(slug: string): string | null {
  return EXCLUSIVE_GROUPS.find(([prefix]) => slug.startsWith(prefix))?.[1] ?? null;
}

export function recommendationMetadataError(params: {
  type: ContentType;
  stage: ContentStage | '';
  from: number | null;
  to: number | null;
  priority: number;
  selectedTagIds: string[];
  catalog: RecommendationTag[];
}): string | null {
  if (params.type !== 'ARTICLE') return null;
  if (params.stage === '') return 'Vui lòng chọn giai đoạn trước khi cấu hình đối tượng gợi ý (stage required).';
  if (!Number.isInteger(params.priority) || params.priority < 0 || params.priority > 100) {
    return 'Độ ưu tiên (priority) phải là số nguyên từ 0 đến 100.';
  }
  const boundsAreEmpty = params.from === null && params.to === null;
  const boundsAreValid = params.from !== null && params.to !== null
    && Number.isInteger(params.from) && Number.isInteger(params.to)
    && params.from >= 0 && params.to <= 42 && params.from <= params.to;
  if (params.stage !== 'PREGNANCY' && !boundsAreEmpty) {
    return 'Bài viết giai đoạn Chuẩn bị mang thai và Sau sinh áp dụng cho toàn bộ giai đoạn (stage-wide).';
  }
  if (params.stage === 'PREGNANCY' && !boundsAreEmpty && !boundsAreValid) {
    return 'Khoảng tuần thai phải là số nguyên từ 0 đến 42 (bounds required).';
  }
  const catalogIds = new Set(params.catalog.map((tag) => tag.id));
  if (params.selectedTagIds.some((id) => !catalogIds.has(id))) {
    return 'Danh mục tag đã thay đổi (catalog changed). Vui lòng tải lại và kiểm tra đối tượng đã chọn.';
  }
  const groups = new Set<string>();
  for (const id of params.selectedTagIds) {
    const tag = params.catalog.find((candidate) => candidate.id === id);
    const group = tag ? groupForSlug(tag.slug) : null;
    if (group && groups.has(group)) {
      return 'Mỗi nhóm đối tượng chỉ được chọn tối đa 1 giá trị (exclusive group).';
    }
    if (group) groups.add(group);
  }
  return null;
}

export function recommendationWindowLabel(
  stage: ContentStage | '',
  from: number | null,
  to: number | null,
): string {
  if (stage !== 'PREGNANCY') return 'Toàn bộ giai đoạn này';
  if (from === null && to === null) return 'Tất cả các tuần thai';
  return `Tuần thai ${from}–${to}`;
}

export function recommendationClassification(selectedTagIds: string[]): 'TARGETED' | 'FALLBACK' {
  return selectedTagIds.length > 0 ? 'TARGETED' : 'FALLBACK';
}

export function recommendationApiErrorCode(error: unknown): string | undefined {
  const response = (error as { response?: { data?: { error?: unknown } } })?.response;
  return typeof response?.data?.error === 'string' ? response.data.error : undefined;
}

export function recommendationApiErrorMessage(error: unknown): string {
  switch (recommendationApiErrorCode(error)) {
    case 'RECOMMENDATION_TAG_INVALID':
    case 'RECOMMENDATION_TAG_CONFLICT':
      return 'Thẻ tag đối tượng gợi ý không còn hợp lệ. Vui lòng tải lại trang và chọn lại.';
    case 'RECOMMENDATION_WEEK_RANGE_INVALID':
      return 'Vui lòng kiểm tra lại khoảng tuần thai: Cả 2 mốc tuần đều bắt buộc và phải từ 0 đến 42.';
    case 'RECOMMENDATION_PRIORITY_INVALID':
      return 'Độ ưu tiên gợi ý phải là số nguyên từ 0 đến 100.';
    default:
      return 'Không thể lưu nội dung. Vui lòng kiểm tra lại các trường thông tin và thử lại.';
  }
}
