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
    expect(template).toContain('giai_đoạn (*Bắt buộc: PRE_PREGNANCY / PREGNANCY / POSTPARTUM)');
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

  it('should report errors for missing mandatory fields or invalid stage', async () => {
    const csvContent =
      'tiêu_đề,giai_đoạn,nội_dung\n' +
      '"","INVALID_STAGE",""';

    const file = new File([csvContent], 'test_invalid.csv', { type: 'text/csv' });
    const rows = await parseImportFile(file, mockTopics);

    expect(rows.length).toBe(1);
    expect(rows[0].isValid).toBe(false);
    expect(rows[0].errors).toContain('Tiêu đề không được để trống.');
    expect(rows[0].errors).toContain('Nội dung không được để trống.');
    expect(rows[0].errors.some((e) => e.includes('Giai đoạn'))).toBe(true);
  });
});
