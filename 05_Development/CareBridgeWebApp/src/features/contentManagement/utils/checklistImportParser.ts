import * as XLSX from 'xlsx';
import type {
  ChecklistSupportFunction,
  ChecklistSubstage,
  ChecklistTemplateType,
  ContentStage,
  CreateChecklistTemplatePayload,
} from '../models/content';

export const CHECKLIST_SHEET_NAME = 'Checklists';
export const CHECKLIST_ITEMS_SHEET_NAME = 'Checklist_Items';
const OPEN_ENDED_OFFSET = 2_147_483_647;
const MAX_SOURCE_URL_LENGTH = 2_048;

const ROOT_HEADERS = [
  'checklist_code', 'name', 'description', 'stage', 'template_type',
  'window_start', 'window_end', 'end_at_stage_exit', 'display_order', 'repeat_mode',
] as const;
const ITEM_HEADERS = [
  'checklist_code', 'order', 'item_text', 'description', 'is_required',
  'support_function', 'source_url',
] as const;

const STAGES = new Set<ContentStage>(['PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM', 'BABY_CARE']);
const TEMPLATE_TYPES = new Set<ChecklistTemplateType>(['MANDATORY', 'OPTIONAL']);
const SUPPORT_FUNCTIONS = new Set<ChecklistSupportFunction>([
  'HEALTH_RECORDS', 'MATERNAL_HEALTH_METRICS', 'MATERNAL_EXERCISES', 'APPOINTMENTS',
  'REMINDERS', 'JOURNEY', 'BABY_CARE', 'EXPERT_CONSULTATION', 'CONTENT_LIBRARY', 'AI_TRIAGE',
]);

type RepeatMode = 'NONE' | 'WEEKLY' | 'DAILY';

export interface ParsedChecklistImportGroup {
  rowIndex: number;
  checklistCode: string;
  name: string;
  stage: ContentStage | '';
  itemCount: number;
  isValid: boolean;
  errors: string[];
  template?: CreateChecklistTemplatePayload;
}

interface RootDraft {
  rowIndex: number;
  checklistCode: string;
  name: string;
  description: string;
  stage: ContentStage | '';
  templateType: ChecklistTemplateType | '';
  windowStart: number | null;
  windowEnd: number | null;
  endAtStageExit: boolean;
  displayOrder: number | null;
  repeatMode: RepeatMode | '';
  errors: string[];
}

interface ItemDraft {
  rowIndex: number;
  checklistCode: string;
  order: number | null;
  itemText: string;
  description: string;
  isRequired: boolean | null;
  supportFunction: ChecklistSupportFunction | null;
  sourceUrl: string;
  errors: string[];
}

export function parseChecklistWorkbook(data: ArrayBuffer | Uint8Array): ParsedChecklistImportGroup[] {
  const workbook = XLSX.read(data, { type: 'array' });
  const rootSheet = workbook.Sheets[CHECKLIST_SHEET_NAME];
  const itemSheet = workbook.Sheets[CHECKLIST_ITEMS_SHEET_NAME];
  if (!rootSheet) throw new Error(`Thiếu sheet bắt buộc "${CHECKLIST_SHEET_NAME}".`);
  if (!itemSheet) throw new Error(`Thiếu sheet bắt buộc "${CHECKLIST_ITEMS_SHEET_NAME}".`);

  const rootMatrix = sheetMatrix(rootSheet);
  const itemMatrix = sheetMatrix(itemSheet);
  assertHeaders(rootMatrix[0], ROOT_HEADERS, CHECKLIST_SHEET_NAME);
  assertHeaders(itemMatrix[0], ITEM_HEADERS, CHECKLIST_ITEMS_SHEET_NAME);

  const roots = rootMatrix.slice(1)
    .map((row, index) => ({ row, rowIndex: index + 2 }))
    .filter(({ row }) => !isEmptyRow(row))
    .map(({ row, rowIndex }) => parseRoot(row, rowIndex));
  const items = itemMatrix.slice(1)
    .map((row, index) => ({ row, rowIndex: index + 2 }))
    .filter(({ row }) => !isEmptyRow(row))
    .map(({ row, rowIndex }) => parseItem(row, rowIndex));

  const rootsByCode = new Map<string, RootDraft[]>();
  for (const root of roots) {
    const key = normalizeChecklistCode(root.checklistCode) || `__empty_root_${root.rowIndex}`;
    rootsByCode.set(key, [...(rootsByCode.get(key) ?? []), root]);
  }
  const itemsByCode = new Map<string, ItemDraft[]>();
  for (const item of items) {
    const key = normalizeChecklistCode(item.checklistCode) || `__empty_item_${item.rowIndex}`;
    itemsByCode.set(key, [...(itemsByCode.get(key) ?? []), item]);
  }

  const groups: ParsedChecklistImportGroup[] = [];
  for (const [key, duplicateRoots] of rootsByCode) {
    const root = duplicateRoots[0];
    const normalizedRootCode = normalizeChecklistCode(root.checklistCode);
    const groupItems = itemsByCode.get(normalizedRootCode) ?? [];
    const errors = [...root.errors];
    if (duplicateRoots.length > 1) {
      errors.push(`Mã checklist "${root.checklistCode}" bị trùng tại các dòng ${duplicateRoots.map((entry) => entry.rowIndex).join(', ')} của sheet Checklists.`);
    }
    if (groupItems.length === 0) errors.push('Checklist phải có ít nhất một mục trong sheet Checklist_Items.');
    for (const item of groupItems) errors.push(...item.errors);

    const validOrders = groupItems.filter((item) => item.order !== null).map((item) => item.order as number);
    const duplicateOrders = [...new Set(validOrders.filter((order, index) => validOrders.indexOf(order) !== index))];
    for (const order of duplicateOrders) errors.push(`Thứ tự mục ${order} bị trùng trong checklist "${root.checklistCode}".`);

    const template = errors.length === 0 ? buildTemplate(root, groupItems) : undefined;
    groups.push({
      rowIndex: root.rowIndex,
      checklistCode: root.checklistCode || key,
      name: root.name,
      stage: root.stage,
      itemCount: groupItems.length,
      isValid: Boolean(template),
      errors,
      template,
    });
    itemsByCode.delete(normalizedRootCode);
  }

  for (const orphanItems of itemsByCode.values()) {
    const checklistCode = orphanItems[0].checklistCode;
    groups.push({
      rowIndex: orphanItems[0].rowIndex,
      checklistCode,
      name: '',
      stage: '',
      itemCount: orphanItems.length,
      isValid: false,
      errors: [
        `Mã checklist "${checklistCode}" ở sheet Checklist_Items không có trong sheet Checklists.`,
        ...orphanItems.flatMap((item) => item.errors),
      ],
    });
  }

  return groups.sort((left, right) => left.rowIndex - right.rowIndex);
}

export function parseChecklistImportFile(file: File): Promise<ParsedChecklistImportGroup[]> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      try {
        resolve(parseChecklistWorkbook(reader.result as ArrayBuffer));
      } catch (error) {
        reject(error);
      }
    };
    reader.onerror = () => reject(new Error('Không thể đọc file Excel.'));
    reader.readAsArrayBuffer(file);
  });
}

function buildTemplate(root: RootDraft, items: ItemDraft[]): CreateChecklistTemplatePayload {
  const stage = root.stage as ContentStage;
  const repeatMode = root.repeatMode as RepeatMode;
  const isTargetless = stage === 'PRE_PREGNANCY' || stage === 'PREGNANCY';
  const anchor: ChecklistSubstage['anchor'] = stage === 'PREGNANCY' ? 'LMP' : stage === 'POSTPARTUM' ? 'DELIVERY_DATE' : 'BIRTH_DATE';
  const substage = stage === 'PRE_PREGNANCY' ? null : {
    code: `${stage}_${anchor}_WEEK_${(root.windowStart as number) - 1}_${root.endAtStageExit ? OPEN_ENDED_OFFSET : (root.windowEnd as number) - 1}`,
    anchor,
    startInclusive: (root.windowStart as number) - 1,
    endInclusive: root.endAtStageExit ? OPEN_ENDED_OFFSET : (root.windowEnd as number) - 1,
    unit: 'WEEK' as const,
  };
  const hasWeeklyRepeat = repeatMode === 'WEEKLY';
  const hasDailyRepeat = repeatMode === 'DAILY';
  const scheduleType = hasDailyRepeat ? 'DAILY' : stage === 'PRE_PREGNANCY' && !hasWeeklyRepeat ? 'SET' : 'WEEKLY';
  const materializationPolicy = hasWeeklyRepeat
    ? 'EACH_WEEK'
    : hasDailyRepeat
      ? 'EACH_DAY'
      : stage === 'PRE_PREGNANCY' ? 'SEQUENCE_STEP' : 'ONCE_PER_WINDOW';

  return {
    name: root.name,
    ...(root.description ? { description: root.description } : {}),
    templateType: root.templateType as ChecklistTemplateType,
    checklistContractVersion: isTargetless ? 2 : 1,
    recipientRoles: ['MOTHER'],
    stage,
    substage,
    displayOrder: stage === 'PRE_PREGNANCY' && root.templateType === 'MANDATORY' ? root.displayOrder as number : 0,
    scheduleType,
    materializationPolicy,
    scheduleGroupKey: null,
    scheduleContextType: stage === 'POSTPARTUM' ? 'JOURNEY' : stage === 'BABY_CARE' ? 'BABY' : null,
    scheduleEndMode: stage === 'PRE_PREGNANCY' ? null : root.endAtStageExit ? 'STAGE_EXIT' : 'FIXED_OFFSET',
    weekBoundaryRule: hasWeeklyRepeat ? 'ANCHOR_RELATIVE_7D' : 'NONE',
    items: items
      .slice()
      .sort((left, right) => (left.order as number) - (right.order as number))
      .map((item) => ({
        itemText: item.itemText,
        order: item.order as number,
        isRequired: item.isRequired as boolean,
        targetSubject: isTargetless ? null : stage === 'BABY_CARE' ? 'BABY' : 'MOTHER',
        ...(item.description ? { description: item.description } : {}),
        ...(item.sourceUrl ? { sourceUrl: item.sourceUrl } : {}),
        ...(item.supportFunction ? { supportFunction: item.supportFunction } : {}),
        repeatWeekly: hasWeeklyRepeat,
        repeatDaily: hasDailyRepeat,
      })),
  };
}

function parseRoot(row: string[], rowIndex: number): RootDraft {
  const checklistCode = cell(row, 0);
  const name = cell(row, 1);
  const description = cell(row, 2);
  const rawStage = enumCell(row, 3);
  const rawType = enumCell(row, 4);
  const rawEndAtStageExit = cell(row, 7);
  const rawRepeatMode = normalizeRepeatMode(cell(row, 9));
  const errors: string[] = [];
  if (!checklistCode) errors.push(`Dòng ${rowIndex}: checklist_code không được để trống.`);
  if (!name) errors.push(`Dòng ${rowIndex}: name không được để trống.`);
  const stage = STAGES.has(rawStage as ContentStage) ? rawStage as ContentStage : '';
  if (!stage) errors.push(`Dòng ${rowIndex}: Giai đoạn "${rawStage || '(trống)'}" không hợp lệ.`);
  const templateType = TEMPLATE_TYPES.has(rawType as ChecklistTemplateType) ? rawType as ChecklistTemplateType : '';
  if (!templateType) errors.push(`Dòng ${rowIndex}: Loại checklist "${rawType || '(trống)'}" không hợp lệ.`);
  const endAtStageExit = parseBoolean(rawEndAtStageExit);
  if (endAtStageExit === null) errors.push(`Dòng ${rowIndex}: end_at_stage_exit phải là TRUE hoặc FALSE.`);
  if (!rawRepeatMode) errors.push(`Dòng ${rowIndex}: Nhịp lặp "${cell(row, 9) || '(trống)'}" không hợp lệ.`);
  if (stage === 'PRE_PREGNANCY' && rawRepeatMode === 'WEEKLY') {
    errors.push(`Dòng ${rowIndex}: PRE_PREGNANCY không hỗ trợ nhịp lặp WEEKLY.`);
  }

  const windowStart = stage === 'PRE_PREGNANCY' && !cell(row, 5) ? null : parseInteger(cell(row, 5));
  const windowEnd = stage === 'PRE_PREGNANCY' && !cell(row, 6) ? null : parseInteger(cell(row, 6));
  if (stage && stage !== 'PRE_PREGNANCY') {
    if (windowStart === null || windowStart < 1 || windowStart > 52) errors.push(`Dòng ${rowIndex}: window_start phải là tuần từ 1 đến 52.`);
    if (endAtStageExit === false && (windowEnd === null || windowEnd < 1 || windowEnd > 52)) errors.push(`Dòng ${rowIndex}: window_end phải là tuần từ 1 đến 52.`);
    if (windowStart !== null && windowEnd !== null && endAtStageExit === false && windowEnd < windowStart) errors.push(`Dòng ${rowIndex}: window_end không được nhỏ hơn window_start.`);
  }

  const displayOrder = parseInteger(cell(row, 8));
  if (displayOrder === null || displayOrder < 0) errors.push(`Dòng ${rowIndex}: display_order phải là số nguyên không âm.`);
  if (stage === 'PRE_PREGNANCY' && templateType === 'MANDATORY' && (displayOrder === null || displayOrder < 1)) {
    errors.push(`Dòng ${rowIndex}: checklist PRE_PREGNANCY bắt buộc cần display_order từ 1.`);
  }

  return {
    rowIndex, checklistCode, name, description, stage, templateType, windowStart, windowEnd,
    endAtStageExit: endAtStageExit ?? false, displayOrder, repeatMode: rawRepeatMode, errors,
  };
}

function parseItem(row: string[], rowIndex: number): ItemDraft {
  const checklistCode = cell(row, 0);
  const order = parseInteger(cell(row, 1));
  const itemText = cell(row, 2);
  const description = cell(row, 3);
  const rawRequired = cell(row, 4);
  const isRequired = parseBoolean(rawRequired);
  const rawSupportFunction = enumCell(row, 5);
  const supportFunction = rawSupportFunction && SUPPORT_FUNCTIONS.has(rawSupportFunction as ChecklistSupportFunction)
    ? rawSupportFunction as ChecklistSupportFunction
    : null;
  const sourceUrl = cell(row, 6);
  const errors: string[] = [];
  if (!checklistCode) errors.push(`Dòng mục ${rowIndex}: checklist_code không được để trống.`);
  if (order === null || order < 1) errors.push(`Dòng mục ${rowIndex}: order phải là số nguyên từ 1.`);
  if (!itemText) errors.push(`Dòng mục ${rowIndex}: item_text không được để trống.`);
  if (isRequired === null) errors.push(`Dòng mục ${rowIndex}: is_required phải là TRUE hoặc FALSE.`);
  if (rawSupportFunction && !supportFunction) errors.push(`Dòng mục ${rowIndex}: Chức năng hỗ trợ "${rawSupportFunction}" không hợp lệ.`);
  const sourceUrlError = validateSourceUrl(sourceUrl);
  if (sourceUrlError) errors.push(`Dòng mục ${rowIndex}: ${sourceUrlError}`);
  return { rowIndex, checklistCode, order, itemText, description, isRequired, supportFunction, sourceUrl, errors };
}

function sheetMatrix(sheet: XLSX.WorkSheet): string[][] {
  const rows = XLSX.utils.sheet_to_json<unknown[]>(sheet, { header: 1, defval: '', raw: false });
  return rows.map((row) => row.map((value) => String(value ?? '').trim()));
}

function assertHeaders(actual: string[] | undefined, expected: readonly string[], sheetName: string): void {
  const normalized = (actual ?? []).map((header) => header.trim().toLowerCase());
  const missing = expected.filter((header) => !normalized.includes(header));
  if (missing.length > 0) throw new Error(`Sheet "${sheetName}" thiếu cột bắt buộc: ${missing.join(', ')}.`);
  const hasUnexpectedOrder = expected.some((header, index) => normalized[index] !== header);
  if (hasUnexpectedOrder) {
    throw new Error(`Sheet "${sheetName}" phải giữ đúng thứ tự cột: ${expected.join(', ')}.`);
  }
}

function isEmptyRow(row: string[]): boolean {
  return row.every((value) => !String(value ?? '').trim());
}

function cell(row: string[], index: number): string {
  return String(row[index] ?? '').trim();
}

function enumCell(row: string[], index: number): string {
  return cell(row, index).toUpperCase();
}

function normalizeChecklistCode(value: string): string {
  return value.trim().toUpperCase();
}

function parseInteger(value: string): number | null {
  if (!/^-?\d+$/.test(value)) return null;
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) ? parsed : null;
}

function parseBoolean(value: string): boolean | null {
  const normalized = value.trim().toUpperCase();
  if (['TRUE', '1', 'YES', 'CÓ', 'CO'].includes(normalized)) return true;
  if (['FALSE', '0', 'NO', 'KHÔNG', 'KHONG'].includes(normalized)) return false;
  return null;
}

function normalizeRepeatMode(value: string): RepeatMode | '' {
  const normalized = value.trim().toUpperCase();
  if (['NONE', 'KHONG_LAP', 'KHÔNG_LẶP'].includes(normalized)) return 'NONE';
  if (['WEEKLY', 'HANG_TUAN', 'HÀNG_TUẦN'].includes(normalized)) return 'WEEKLY';
  if (['DAILY', 'HANG_NGAY', 'HÀNG_NGÀY'].includes(normalized)) return 'DAILY';
  return '';
}

function validateSourceUrl(value: string): string | null {
  if (!value) return null;
  if (value.length > MAX_SOURCE_URL_LENGTH) return 'Link nguồn không được vượt quá 2.048 ký tự.';
  try {
    const url = new URL(value);
    if ((url.protocol === 'http:' || url.protocol === 'https:') && url.hostname && !url.username && !url.password) return null;
  } catch {
    // Shared validation message below.
  }
  return 'Link nguồn phải là URL đầy đủ bắt đầu bằng http:// hoặc https:// và không chứa thông tin đăng nhập.';
}
