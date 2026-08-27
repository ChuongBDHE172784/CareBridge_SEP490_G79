import { describe, expect, it } from 'vitest';
import { generateContentTemplate, parseImportFile } from './contentImportParser';
import type { CommunityTopic } from '../models/content';

const mockTopics: CommunityTopic[] = [
  {
    id: 'topic-1',
    name: 'Dinh dưỡng thai kỳ',
    slug: 'dinh-duong-thai-ky',
    description: '',
    icon: '',
    type: 'TOPIC',
    parentId: null,
    questionCount: 0,
    isHidden: false,
    sortOrder: 1,
    createdAt: '',
    updatedAt: '',
  },
];

describe('contentImportParser', () => {
  it('should generate content template with UTF-8 BOM and sample CSV rows', () => {
    const template = generateContentTemplate('ARTICLE');
    expect(template.startsWith('\uFEFF')).toBe(true);
    expect(template).toContain('tiêu_đề (*Bắt buộc)');
    expect(template).toContain('giai_đoạn (*Bắt buộc: Chuẩn bị mang thai [PRE_PREGNANCY]');
    expect(template).toContain('nội_dung (*Bắt buộc)');
  });

  it('should parse CSV file with Vietnamese UTF-8 text and Rich HTML content', async () => {
    const csvContent =
      '\uFEFF"tiêu_đề","giai_đoạn","nội_dung","danh_mục"\n' +
      '"Chuẩn bị thai kỳ","PRE_PREGNANCY","<h2>Tiêu đề</h2><p>Nội dung <strong>quan trọng</strong>.</p>","Dinh dưỡng thai kỳ"';

    const file = new File([csvContent], 'test_article.csv', { type: 'text/csv;charset=utf-8' });
    const rows = await parseImportFile(file, mockTopics);

    expect(rows.length).toBe(1);
    expect(rows[0].isValid).toBe(true);
    expect(rows[0].title).toBe('Chuẩn bị thai kỳ');
    expect(rows[0].stage).toBe('PRE_PREGNANCY');
    expect(rows[0].body).toContain('<h2>Tiêu đề</h2>');
    expect(rows[0].body).toContain('<strong>quan trọng</strong>');
    expect(rows[0].topicId).toBe('topic-1');
  });

  it('should parse TSV TXT file properly', async () => {
    const txtContent =
      'tiêu_đề\tgiai_đoạn\tnội_dung\n' +
      'Chăm sóc mẹ sau sinh\tPOSTPARTUM\t<p>Nội dung <em>sau sinh</em>.</p>';

    const file = new File([txtContent], 'test_article.txt', { type: 'text/plain;charset=utf-8' });
    const rows = await parseImportFile(file, mockTopics);

    expect(rows.length).toBe(1);
    expect(rows[0].isValid).toBe(true);
    expect(rows[0].title).toBe('Chăm sóc mẹ sau sinh');
    expect(rows[0].stage).toBe('POSTPARTUM');
    expect(rows[0].body).toBe('<p>Nội dung <em>sau sinh</em>.</p>');
  });

  it('should unescape HTML entities if CSV parser escaped them once', async () => {
    const csvContent =
      'tiêu_đề,giai_đoạn,nội_dung\n' +
      '"Thai kỳ khỏe mạnh","PREGNANCY","&lt;h2&gt;Thai kỳ&lt;/h2&gt;&lt;p&gt;Nội dung...&lt;/p&gt;"';

    const file = new File([csvContent], 'test_escaped.csv', { type: 'text/csv' });
    const rows = await parseImportFile(file, mockTopics);

    expect(rows.length).toBe(1);
    expect(rows[0].isValid).toBe(true);
    expect(rows[0].body).toBe('<h2>Thai kỳ</h2><p>Nội dung...</p>');
  });

  it('should include recommendation audience columns in ARTICLE template and omit in FAQ template', () => {
    const articleTemplate = generateContentTemplate('ARTICLE');
    expect(articleTemplate).toContain('đối_tượng_gợi_ý');
    expect(articleTemplate).toContain('từ_tuần');
    expect(articleTemplate).toContain('đến_tuần');
    expect(articleTemplate).toContain('độ_ưu_tiên');

    const faqTemplate = generateContentTemplate('FAQ');
    expect(faqTemplate).not.toContain('đối_tượng_gợi_ý');
    expect(faqTemplate).not.toContain('từ_tuần');
  });

  it('should parse recommendation audience tags, week bounds, and priority for ARTICLE', async () => {
    const mockRecCatalog = [
      { id: 'tag-bmi-obesity', slug: 'rec-bmi-obesity', description: 'CATALOG_V1|rec-bmi-obesity' },
      { id: 'tag-age-35', slug: 'rec-age-35-39', description: 'CATALOG_V1|rec-age-35-39' },
    ];

    const csvContent =
      'tiêu_đề,giai_đoạn,nội_dung,đối_tượng_gợi_ý,từ_tuần,đến_tuần,độ_ưu_tiên\n' +
      '"Chăm sóc thai phụ lớn tuổi","PREGNANCY","<p>Nội dung</p>","Độ tuổi: 35 - 39, rec-bmi-obesity",1,12,60';

    const file = new File([csvContent], 'test_rec.csv', { type: 'text/csv' });
    const rows = await parseImportFile(file, mockTopics, mockRecCatalog as any);

    expect(rows.length).toBe(1);
    expect(rows[0].isValid).toBe(true);
    expect(rows[0].tagIds).toEqual(['tag-age-35', 'tag-bmi-obesity']);
    expect(rows[0].eligibleFromWeek).toBe(1);
    expect(rows[0].eligibleToWeek).toBe(12);
    expect(rows[0].recommendationPriority).toBe(60);
  });

  it('should reject exclusive group tag collision in import row', async () => {
    const mockRecCatalog = [
      { id: 'tag-bmi-obesity', slug: 'rec-bmi-obesity', description: 'CATALOG_V1|rec-bmi-obesity' },
      { id: 'tag-bmi-underweight', slug: 'rec-bmi-underweight', description: 'CATALOG_V1|rec-bmi-underweight' },
    ];

    const csvContent =
      'tiêu_đề,giai_đoạn,nội_dung,đối_tượng_gợi_ý\n' +
      '"Bài viết BMI xung đột","PREGNANCY","<p>Nội dung</p>","rec-bmi-obesity, rec-bmi-underweight"';

    const file = new File([csvContent], 'test_exclusive.csv', { type: 'text/csv' });
    const rows = await parseImportFile(file, mockTopics, mockRecCatalog as any);

    expect(rows.length).toBe(1);
    expect(rows[0].isValid).toBe(false);
    expect(rows[0].errors.some(e => e.includes('exclusive group'))).toBe(true);
  });

  it('should reject week bounds for non-pregnancy stages', async () => {
    const csvContent =
      'tiêu_đề,giai_đoạn,nội_dung,từ_tuần,đến_tuần\n' +
      '"Chuẩn bị thai kỳ lỗi tuần","PRE_PREGNANCY","<p>Nội dung</p>",5,10';

    const file = new File([csvContent], 'test_stage_bounds.csv', { type: 'text/csv' });
    const rows = await parseImportFile(file, mockTopics, []);

    expect(rows.length).toBe(1);
    expect(rows[0].isValid).toBe(false);
    expect(rows[0].errors.some(e => e.includes('stage-wide'))).toBe(true);
  });

  it('should successfully parse reference file CareBridge_30_Bai_Viet_Rich_Content.csv with all 30 rows valid', async () => {
    const fs = await import('fs');
    const path = await import('path');
    const csvPath = path.resolve('../../../08_References/CareBridge_30_Bai_Viet_Rich_Content.csv');
    if (fs.existsSync(csvPath)) {
      const csvRaw = fs.readFileSync(csvPath, 'utf8');
      const { RECOMMENDATION_TAG_VIETNAMESE_LABELS } = await import('../pages/recommendationMetadata');
      const fullCatalog = Object.keys(RECOMMENDATION_TAG_VIETNAMESE_LABELS).map((slug) => ({
        id: `tag-${slug}`,
        slug,
        description: `CATALOG_V1|${slug}`,
      }));

      const file = new File([csvRaw], 'CareBridge_30_Bai_Viet_Rich_Content.csv', { type: 'text/csv' });
      const rows = await parseImportFile(file, mockTopics, fullCatalog as any);

      expect(rows.length).toBe(30);
      const invalidRows = rows.filter((r) => !r.isValid);
      if (invalidRows.length > 0) {
        console.error('Invalid reference rows:', invalidRows.map((r) => ({ row: r.rowIndex, title: r.title, errors: r.errors })));
      }
      expect(invalidRows.length).toBe(0);
      expect(rows.every((r) => r.isValid)).toBe(true);
    }
  });
});
