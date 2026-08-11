import * as XLSX from 'xlsx';
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
  'CHUẨN_BỊ_MANG_THAI': 'PRE_PREGNANCY',
  'CHUA_BI_MANG_THAI': 'PRE_PREGNANCY',
  'MANG_THAI': 'PREGNANCY',
  'SAU_SINH': 'POSTPARTUM',
};

/**
 * Generates a UTF-8 BOM encoded CSV template with instructions, headers, and Rich HTML sample data.
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
    isArticle ? 'Chuẩn bị sức khỏe trọn vẹn cho thai kỳ' : 'Cần chuẩn bị gì trước khi mang thai?',
    'PRE_PREGNANCY',
    isArticle
      ? `<h2>Chuẩn bị sức khỏe trước thai kỳ</h2><p>Chuẩn bị sức khỏe trước mang thai là bước quan trọng giúp tạo nền tảng vững chắc cho mẹ và bé.</p><h3>Những điều nên chuẩn bị:</h3><ul><li><strong>Khám sức khỏe:</strong> Trao đổi với bác sĩ phụ khoa.</li><li><strong>Axit folic:</strong> Bổ sung 400mcg mỗi ngày.</li></ul><blockquote>Việc chuẩn bị kỹ lưỡng giúp mẹ bầu tự tin hơn.</blockquote><table><thead><tr><th>Dưỡng chất</th><th>Vai trò</th></tr></thead><tbody><tr><td>Axit Folic</td><td>Ngăn ngừa dị tật ống thần kinh</td></tr></tbody></table>`
      : `<p>Mẹ nên <strong>khám sức khỏe tổng quát</strong>, bổ sung <em>Axit Folic</em> ít nhất 3 tháng trước khi mang thai và tiêm phòng đầy đủ.</p>`,
    isArticle ? 'Hướng dẫn các bước chuẩn bị trước khi mang thai.' : 'Giải đáp lưu ý chuẩn bị trước thai kỳ.',
    'Dinh dưỡng thai kỳ',
    'Bộ Y tế',
    'https://moh.gov.vn',
    'Bộ Y tế Việt Nam',
  ];

  const sampleRow2 = [
    isArticle ? 'Chế độ dinh dưỡng 3 tháng đầu thai kỳ' : 'Mẹ bầu nên ăn gì trong 3 tháng đầu?',
    'PREGNANCY',
    isArticle
      ? `<h2>Dinh dưỡng thai kỳ 3 tháng đầu</h2><p>Giai đoạn 3 tháng đầu đóng vai trò then chốt cho sự hình thành mầm sống.</p><h3>1. Thực phẩm khuyến khích:</h3><ol><li>Rau xanh đậm màu</li><li>Trứng và thịt nạc</li></ol>`
      : `<p>Mẹ bầu nên ăn thực phẩm giàu sắt, canxi, Axit Folic và hạn chế đồ ăn tươi sống.</p>`,
    isArticle ? 'Cẩm nang thực đơn và dưỡng chất cho mẹ bầu 3 tháng đầu.' : 'Giải đáp thực đơn 3 tháng đầu thai kỳ.',
    'Dinh dưỡng thai kỳ',
    'WHO',
    'https://www.who.int',
    'WHO',
  ];

  const csvRows = [
    headers.map(escapeCsvCell).join(','),
    sampleRow1.map(escapeCsvCell).join(','),
    sampleRow2.map(escapeCsvCell).join(','),
  ];

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
 * Universal file parser supporting .csv, .txt, .xlsx, .xls with Vietnamese UTF-8 encoding.
 */
export async function parseImportFile(
  file: File,
  topics: CommunityTopic[],
): Promise<ParsedImportRow[]> {
  const fileName = file.name.toLowerCase();

  if (fileName.endsWith('.xlsx') || fileName.endsWith('.xls')) {
    return parseExcelFile(file, topics);
  }

  // Handle CSV & TXT text files
  const text = await readTextFile(file);
  return parseTextFileContent(text, fileName.endsWith('.txt'), topics);
}

function readTextFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => resolve((e.target?.result as string) || '');
    reader.onerror = (e) => reject(e);
    reader.readAsText(file, 'UTF-8');
  });
}

function parseExcelFile(file: File, topics: CommunityTopic[]): Promise<ParsedImportRow[]> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = new Uint8Array(e.target?.result as ArrayBuffer);
        const workbook = XLSX.read(data, { type: 'array' });
        if (!workbook.SheetNames.length) {
          resolve([]);
          return;
        }
        const firstSheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[firstSheetName];
        const rawMatrix: any[][] = XLSX.utils.sheet_to_json(worksheet, { header: 1, defval: '' });
        
        const matrix: string[][] = rawMatrix.map(row => 
          Array.isArray(row) ? row.map(cell => (cell == null ? '' : String(cell))) : []
        );

        resolve(processImportMatrix(matrix, topics));
      } catch (err) {
        reject(err);
      }
    };
    reader.onerror = (err) => reject(err);
    reader.readAsArrayBuffer(file);
  });
}

function parseTextFileContent(
  text: string,
  isTxtFile: boolean,
  topics: CommunityTopic[],
): ParsedImportRow[] {
  // Strip BOM if present
  let cleanText = text.replace(/^\uFEFF/, '').trim();
  if (!cleanText) return [];

  let matrix: string[][];

  if (isTxtFile && (cleanText.includes('\t') || !cleanText.includes(','))) {
    // Tab-separated values (TSV) for .txt
    matrix = cleanText.split(/\r?\n/).map((line) => line.split('\t'));
  } else {
    // Standard RFC-4180 CSV parser
    matrix = parseCsvMatrix(cleanText);
  }

  return processImportMatrix(matrix, topics);
}

function processImportMatrix(
  matrix: string[][],
  topics: CommunityTopic[],
): ParsedImportRow[] {
  if (matrix.length < 2) return [];

  // Strip BOM from header row if present
  const rawHeaders = matrix[0].map((h) => normalizeHeader(h));

  const titleIdx = findHeaderIndex(rawHeaders, ['tiêu_đề', 'title', 'tieude', 'tiêu đề', 'heading']);
  const stageIdx = findHeaderIndex(rawHeaders, ['giai_đoạn', 'stage', 'giaidoan', 'giai đoạn', 'thời kỳ']);
  const bodyIdx = findHeaderIndex(rawHeaders, ['nội_dung', 'body', 'noidung', 'nội dung', 'content']);
  const summaryIdx = findHeaderIndex(rawHeaders, ['tóm_tắt', 'summary', 'tomtat', 'tóm tắt']);
  const topicIdx = findHeaderIndex(rawHeaders, ['danh_mục', 'category', 'topic', 'danhmuc', 'danh mục', 'chuyên mục']);
  const sourceLabelIdx = findHeaderIndex(rawHeaders, ['tên_nguồn', 'source_label', 'tennguon', 'tên nguồn']);
  const sourceUrlIdx = findHeaderIndex(rawHeaders, ['link_nguồn', 'source_url', 'linknguon', 'link nguồn', 'url']);
  const sourcePublisherIdx = findHeaderIndex(rawHeaders, ['nhà_xuất_bản', 'source_publisher', 'nhaxuatban', 'nhà xuất bản', 'publisher']);

  const topicNameMap = new Map<string, string>();
  for (const t of topics) {
    if (t.name) {
      topicNameMap.set(t.name.toLowerCase().trim(), t.id);
    }
    topicNameMap.set(t.id, t.id);
  }

  const result: ParsedImportRow[] = [];

  for (let i = 1; i < matrix.length; i++) {
    const row = matrix[i];
    if (!row || row.length === 0 || (row.length === 1 && !row[0].trim())) continue;

    const errors: string[] = [];
    const rawTitle = titleIdx >= 0 ? (row[titleIdx] || '').trim() : '';
    const rawStage = stageIdx >= 0 ? (row[stageIdx] || '').trim() : '';
    let rawBody = bodyIdx >= 0 ? (row[bodyIdx] || '').trim() : '';
    const rawSummary = summaryIdx >= 0 ? (row[summaryIdx] || '').trim() : '';
    const rawTopic = topicIdx >= 0 ? (row[topicIdx] || '').trim() : '';
    const rawSourceLabel = sourceLabelIdx >= 0 ? (row[sourceLabelIdx] || '').trim() : '';
    const rawSourceUrl = sourceUrlIdx >= 0 ? (row[sourceUrlIdx] || '').trim() : '';
    const rawSourcePublisher = sourcePublisherIdx >= 0 ? (row[sourcePublisherIdx] || '').trim() : '';

    // Unescape HTML entities if parser escaped them once (e.g. &lt;h2&gt; -> <h2>)
    if (rawBody.includes('&lt;') || rawBody.includes('&gt;')) {
      rawBody = unescapeHtmlEntities(rawBody);
    }

    // Validate Title
    if (!rawTitle) {
      errors.push('Tiêu đề không được để trống.');
    }

    // Validate & Map Stage
    let parsedStage: ContentStage | '' = '';
    if (!rawStage) {
      errors.push('Giai đoạn không được để trống.');
    } else {
      const normalizedStageKey = rawStage.toUpperCase().replace(/\s+/g, ' ').trim();
      if (STAGE_MAP[normalizedStageKey]) {
        parsedStage = STAGE_MAP[normalizedStageKey];
      } else {
        errors.push(`Giai đoạn "${rawStage}" không hợp lệ. Phải là: PRE_PREGNANCY, PREGNANCY, hoặc POSTPARTUM.`);
      }
    }

    // Validate Body
    if (!rawBody) {
      errors.push('Nội dung không được để trống.');
    }

    // Map Topic if provided
    let matchedTopicId: string | undefined = undefined;
    if (rawTopic) {
      const matched = topicNameMap.get(rawTopic.toLowerCase().trim());
      if (matched) {
        matchedTopicId = matched;
      }
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

function unescapeHtmlEntities(str: string): string {
  return str
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&');
}

function normalizeHeader(str: string): string {
  return str
    .replace(/^\uFEFF/, '')
    .toLowerCase()
    .replace(/\(\*[^)]*\)/g, '')
    .trim();
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
 * Parses raw CSV text into a 2D string matrix respecting quotes and multiline cells.
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
        i++;
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
