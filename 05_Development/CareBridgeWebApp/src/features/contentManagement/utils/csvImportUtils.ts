import type { CommunityTopic, ContentStage, ContentType } from '../models/content';

export interface ParsedImportRow {
  rowIndex: number;
  title: string;
  body: string;
  stage: ContentStage | '';
  summary?: string;
  topicId?: string;
  topicName?: string;
  sourceLabel?: string;
  sourceUrl?: string;
  sourcePublisher?: string;
  isValid: boolean;
  errors: string[];
}

const STAGE_MAP: Record<string, ContentStage> = {
  PRE_PREGNANCY: 'PRE_PREGNANCY',
  PREGNANCY: 'PREGNANCY',
  POSTPARTUM: 'POSTPARTUM',
  'CHUẨN BỊ MANG THAI': 'PRE_PREGNANCY',
  'MANG THAI': 'PREGNANCY',
  'SAU SINH': 'POSTPARTUM',
  'CHUA_BI_MANG_THAI': 'PRE_PREGNANCY',
  'MANG_THAI': 'PREGNANCY',
  'SAU_SINH': 'POSTPARTUM',
};

/**
 * Generates a UTF-8 BOM encoded CSV template with instructions, headers, and sample data.
 * Standard Excel opens UTF-8 BOM CSV files directly with correct Vietnamese diacritics.
 */
export function generateContentTemplate(type: ContentType): string {
  const isArticle = type === 'ARTICLE';

  const headers = [
    'tiêu_đề (*Bắt buộc)',
    'giai_đoạn (*Bắt buộc: PRE_PREGNANCY / PREGNANCY / POSTPARTUM)',
    'nội_dung (*Bắt buộc)',
    'tóm_tắt (Không bắt buộc)',
    'danh_mục (Không bắt buộc - Tên danh mục)',
    'tên_nguồn (Không bắt buộc)',
    'link_nguồn (Không bắt buộc)',
    'nhà_xuất_bản (Không bắt buộc)',
  ];

  const sampleRow1 = [
    isArticle ? 'Dinh dưỡng an toàn trong 3 tháng đầu thai kỳ' : 'Mẹ bầu nên ăn gì trong 3 tháng đầu?',
    'PREGNANCY',
    isArticle
      ? '<p>Dinh dưỡng giai đoạn 3 tháng đầu đóng vai trò rất quan trọng cho sự phát triển của thai nhi. Mẹ nên bổ sung <strong>Acid Folic</strong> và sắt đầy đủ.</p>'
      : '<p>Trong 3 tháng đầu, mẹ nên bổ sung Acid Folic, các thực phẩm giàu sắt và protein, hạn chế thực phẩm tái sống.</p>',
    isArticle ? 'Hướng dẫn dinh dưỡng thai kỳ 3 tháng đầu cho mẹ bầu.' : 'Giải đáp thắc mắc về thực đơn 3 tháng đầu.',
    'Dinh dưỡng thai kỳ',
    'Bộ Y tế',
    'https://moh.gov.vn',
    'Bộ Y tế Việt Nam',
  ];

  const sampleRow2 = [
    isArticle ? 'Những lưu ý khi chăm sóc trẻ sơ sinh 1 tháng tuổi' : 'Bé sơ sinh ngủ bao nhiêu tiếng một ngày?',
    'POSTPARTUM',
    isArticle
      ? '<p>Trẻ sơ sinh cần được theo dõi giấc ngủ, lịch tiêm chủng và vệ sinh rốn hàng ngày đúng cách.</p>'
      : '<p>Trẻ sơ sinh thông thường ngủ từ 14 đến 17 tiếng mỗi ngày, chia làm nhiều giấc ngắn.</p>',
    'Chăm sóc trẻ sơ sinh tháng đầu tiên.',
    'Chăm sóc bé',
    'WHO',
    'https://www.who.int',
    'WHO',
  ];

  const csvRows = [
    headers.map(escapeCsvCell).join(','),
    sampleRow1.map(escapeCsvCell).join(','),
    sampleRow2.map(escapeCsvCell).join(','),
  ];

  // Include UTF-8 Byte Order Mark (\uFEFF) so Excel opens UTF-8 properly
  return '\uFEFF' + csvRows.join('\r\n');
}

function escapeCsvCell(val: string): string {
  if (val == null) return '""';
  const str = String(val);
  if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

/**
 * Robust CSV parser handling multi-line quoted fields, escaped quotes, and flexible headers.
 */
export function parseContentCsv(
  csvText: string,
  topics: CommunityTopic[],
): ParsedImportRow[] {
  // Strip BOM if present
  const cleanText = csvText.replace(/^\uFEFF/, '').trim();
  if (!cleanText) return [];

  const matrix = parseCsvMatrix(cleanText);
  if (matrix.length < 2) return []; // Need at least header + 1 data row

  const rawHeaders = matrix[0].map(h => normalizeHeader(h));
  
  // Find column indices
  const titleIdx = findHeaderIndex(rawHeaders, ['tiêu_đề', 'title', 'tieude', 'tiêu đề', 'heading']);
  const stageIdx = findHeaderIndex(rawHeaders, ['giai_đoạn', 'stage', 'giaidoan', 'giai đoạn', 'thời kỳ']);
  const bodyIdx = findHeaderIndex(rawHeaders, ['nội_dung', 'body', 'noidung', 'nội dung', 'content', 'câu hỏi / trả lời']);
  const summaryIdx = findHeaderIndex(rawHeaders, ['tóm_tắt', 'summary', 'tomtat', 'tóm tắt', 'description']);
  const topicIdx = findHeaderIndex(rawHeaders, ['danh_mục', 'topic', 'danhmuc', 'danh mục', 'chuyên mục', 'category']);
  const sourceLabelIdx = findHeaderIndex(rawHeaders, ['tên_nguồn', 'source_label', 'tennguon', 'tên nguồn', 'nguồn tham khảo']);
  const sourceUrlIdx = findHeaderIndex(rawHeaders, ['link_nguồn', 'source_url', 'linknguon', 'link nguồn', 'url']);
  const sourcePublisherIdx = findHeaderIndex(rawHeaders, ['nhà_xuất_bản', 'source_publisher', 'nhaxuatban', 'nhà xuất bản', 'publisher']);

  const topicNameMap = new Map<string, string>();
  for (const t of topics) {
    topicNameMap.set(t.name.toLowerCase().trim(), t.id);
    topicNameMap.set(t.id, t.id);
  }

  const result: ParsedImportRow[] = [];

  for (let i = 1; i < matrix.length; i++) {
    const row = matrix[i];
    // Skip empty lines
    if (row.length === 0 || (row.length === 1 && !row[0].trim())) continue;

    const errors: string[] = [];
    const rawTitle = titleIdx >= 0 ? (row[titleIdx] || '').trim() : '';
    const rawStage = stageIdx >= 0 ? (row[stageIdx] || '').trim() : '';
    const rawBody = bodyIdx >= 0 ? (row[bodyIdx] || '').trim() : '';
    const rawSummary = summaryIdx >= 0 ? (row[summaryIdx] || '').trim() : '';
    const rawTopic = topicIdx >= 0 ? (row[topicIdx] || '').trim() : '';
    const rawSourceLabel = sourceLabelIdx >= 0 ? (row[sourceLabelIdx] || '').trim() : '';
    const rawSourceUrl = sourceUrlIdx >= 0 ? (row[sourceUrlIdx] || '').trim() : '';
    const rawSourcePublisher = sourcePublisherIdx >= 0 ? (row[sourcePublisherIdx] || '').trim() : '';

    // Validate Title
    if (!rawTitle) {
      errors.push('Tiêu đề không được để trống');
    }

    // Validate & Map Stage
    let parsedStage: ContentStage | '' = '';
    if (!rawStage) {
      errors.push('Giai đoạn không được để trống');
    } else {
      const normalizedStageKey = rawStage.toUpperCase().replace(/\s+/g, ' ');
      if (STAGE_MAP[normalizedStageKey]) {
        parsedStage = STAGE_MAP[normalizedStageKey];
      } else {
        errors.push(`Giai đoạn "${rawStage}" không hợp lệ. Phải là: PRE_PREGNANCY, PREGNANCY, hoặc POSTPARTUM`);
      }
    }

    // Validate Body
    if (!rawBody) {
      errors.push('Nội dung không được để trống');
    }

    // Map Topic if provided
    let matchedTopicId: string | undefined = undefined;
    if (rawTopic) {
      const matched = topicNameMap.get(rawTopic.toLowerCase());
      if (matched) {
        matchedTopicId = matched;
      }
      // Note: If topic is provided but not found, we accept it as optional without hard erroring, but set matchedTopicId if available.
    }

    result.push({
      rowIndex: i + 1,
      title: rawTitle,
      body: rawBody,
      stage: parsedStage,
      summary: rawSummary || undefined,
      topicId: matchedTopicId,
      topicName: rawTopic || undefined,
      sourceLabel: rawSourceLabel || undefined,
      sourceUrl: rawSourceUrl || undefined,
      sourcePublisher: rawSourcePublisher || undefined,
      isValid: errors.length === 0,
      errors,
    });
  }

  return result;
}

function normalizeHeader(str: string): string {
  return str.toLowerCase().replace(/\(\*[^)]*\)/g, '').trim();
}

function findHeaderIndex(headers: string[], candidates: string[]): number {
  for (let i = 0; i < headers.length; i++) {
    const h = headers[i];
    for (const c of candidates) {
      if (h.includes(c)) return i;
    }
  }
  return -1;
}

/**
 * Parses raw CSV into a 2D array of string cells, correctly managing double quotes and newlines.
 */
function parseCsvMatrix(text: string): string[][] {
  const lines: string[][] = [];
  let currentRow: string[] = [];
  let currentCell = '';
  let insideQuote = false;

  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    const nextChar = text[i + 1];

    if (insideQuote) {
      if (char === '"' && nextChar === '"') {
        currentCell += '"';
        i++; // skip escaped quote
      } else if (char === '"') {
        insideQuote = false;
      } else {
        currentCell += char;
      }
    } else {
      if (char === '"') {
        insideQuote = true;
      } else if (char === ',') {
        currentRow.push(currentCell);
        currentCell = '';
      } else if (char === '\r') {
        if (nextChar === '\n') {
          i++;
        }
        currentRow.push(currentCell);
        lines.push(currentRow);
        currentRow = [];
        currentCell = '';
      } else if (char === '\n') {
        currentRow.push(currentCell);
        lines.push(currentRow);
        currentRow = [];
        currentCell = '';
      } else {
        currentCell += char;
      }
    }
  }

  if (currentCell || currentRow.length > 0) {
    currentRow.push(currentCell);
    lines.push(currentRow);
  }

  return lines;
}
