/// <reference types="node" />

import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import * as XLSX from 'xlsx';
import { parseChecklistWorkbook } from './checklistImportParser';

const ROOT_HEADERS = [
  'checklist_code', 'name', 'description', 'stage', 'template_type',
  'window_start', 'window_end', 'end_at_stage_exit', 'display_order', 'repeat_mode',
];
const ITEM_HEADERS = [
  'checklist_code', 'order', 'item_text', 'description', 'is_required',
  'support_function', 'source_url',
];
const WEB_APP_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');
const REPOSITORY_ROOT = resolve(WEB_APP_ROOT, '../..');
const PUBLIC_TEMPLATE_PATH = resolve(WEB_APP_ROOT, 'public/Form_Mau_Import_Checklist.xlsx');
const REFERENCE_TEMPLATE_PATH = resolve(REPOSITORY_ROOT, '08_References/Form_Mau_Import_Checklist.xlsx');

function workbookBytes(rootRows: unknown[][], itemRows: unknown[][]): Uint8Array {
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([ROOT_HEADERS, ...rootRows]), 'Checklists');
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([ITEM_HEADERS, ...itemRows]), 'Checklist_Items');
  return XLSX.write(workbook, { bookType: 'xlsx', type: 'array' }) as Uint8Array;
}

describe('checklistImportParser', () => {
  it('maps source-facing checklist rows to the canonical create payload', () => {
    const result = parseChecklistWorkbook(workbookBytes([
      ['PREG-01', 'Khám thai định kỳ', 'Theo dõi thai kỳ', 'PREGNANCY', 'MANDATORY', 2, 5, false, 0, 'WEEKLY'],
      ['BABY-01', 'Theo dõi bé', '', 'BABY_CARE', 'OPTIONAL', 1, '', true, 0, 'DAILY'],
    ], [
      ['preg-01', 1, 'Đặt lịch khám', '', true, 'APPOINTMENTS', 'https://moh.gov.vn/guide'],
      ['BABY-01', 1, 'Theo dõi nhiệt độ', 'Ghi mỗi ngày', false, 'HEALTH_RECORDS', ''],
    ]));

    expect(result).toHaveLength(2);
    expect(result[0]).toMatchObject({ checklistCode: 'PREG-01', rowIndex: 2, isValid: true, itemCount: 1 });
    expect(result[0].template).toEqual(expect.objectContaining({
      name: 'Khám thai định kỳ',
      templateType: 'MANDATORY',
      checklistContractVersion: 2,
      recipientRoles: ['MOTHER'],
      stage: 'PREGNANCY',
      substage: expect.objectContaining({ anchor: 'LMP', startInclusive: 1, endInclusive: 4, unit: 'WEEK' }),
      scheduleType: 'WEEKLY',
      materializationPolicy: 'EACH_WEEK',
      scheduleEndMode: 'FIXED_OFFSET',
      weekBoundaryRule: 'ANCHOR_RELATIVE_7D',
      items: [expect.objectContaining({
        order: 1,
        targetSubject: null,
        repeatWeekly: true,
        repeatDaily: false,
        sourceUrl: 'https://moh.gov.vn/guide',
      })],
    }));
    expect(result[1].template).toEqual(expect.objectContaining({
      checklistContractVersion: 1,
      stage: 'BABY_CARE',
      scheduleContextType: 'BABY',
      scheduleType: 'DAILY',
      materializationPolicy: 'EACH_DAY',
      scheduleEndMode: 'STAGE_EXIT',
      substage: expect.objectContaining({ anchor: 'BIRTH_DATE', startInclusive: 0, endInclusive: 2_147_483_647 }),
      items: [expect.objectContaining({ targetSubject: 'BABY', repeatDaily: true })],
    }));
  });

  it('invalidates the complete checklist for duplicate roots, duplicate item order, enum, range, URL and orphan rows', () => {
    const result = parseChecklistWorkbook(workbookBytes([
      ['DUP', 'Bản một', '', 'PREGNANCY', 'MANDATORY', 1, 4, false, 0, 'NONE'],
      ['dup', 'Bản hai', '', 'PREGNANCY', 'MANDATORY', 1, 4, false, 0, 'NONE'],
      ['BAD', 'Sai dữ liệu', '', 'UNKNOWN', 'REQUIRED', 7, 2, false, 0, 'MONTHLY'],
    ], [
      ['DUP', 1, 'Mục một', '', true, 'APPOINTMENTS', ''],
      ['DUP', 1, 'Mục trùng', '', true, 'APPOINTMENTS', ''],
      ['BAD', 1, 'Mục lỗi', '', true, 'NOT_A_FUNCTION', 'https://user:secret@example.com'],
      ['ORPHAN', 1, 'Không có checklist cha', '', true, '', ''],
    ]));

    expect(result.every((group) => !group.isValid)).toBe(true);
    expect(result.find((group) => group.checklistCode === 'DUP')?.errors.join(' ')).toMatch(/trùng/i);
    expect(result.find((group) => group.checklistCode === 'BAD')?.errors.join(' ')).toMatch(/Giai đoạn|Loại checklist|Nhịp lặp|Link nguồn/);
    expect(result.find((group) => group.checklistCode === 'ORPHAN')?.errors.join(' ')).toMatch(/không có trong sheet Checklists/i);
  });

  it('rejects a workbook with missing canonical sheets or headers', () => {
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([['name'], ['Thiếu mã']]), 'Checklists');
    const bytes = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' }) as Uint8Array;

    expect(() => parseChecklistWorkbook(bytes)).toThrow(/Checklist_Items/);
  });

  it('rejects reordered columns instead of silently mapping the wrong fields', () => {
    const workbook = XLSX.utils.book_new();
    const reorderedHeaders = [...ROOT_HEADERS];
    [reorderedHeaders[0], reorderedHeaders[1]] = [reorderedHeaders[1], reorderedHeaders[0]];
    XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([reorderedHeaders]), 'Checklists');
    XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([ITEM_HEADERS]), 'Checklist_Items');
    const bytes = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' }) as Uint8Array;

    expect(() => parseChecklistWorkbook(bytes)).toThrow(/đúng thứ tự cột/i);
  });

  it('keeps the public template synchronized with the canonical reference workbook', () => {
    const publicBytes = readFileSync(PUBLIC_TEMPLATE_PATH);
    const referenceBytes = readFileSync(REFERENCE_TEMPLATE_PATH);
    expect(publicBytes.equals(referenceBytes)).toBe(true);

    const workbook = XLSX.read(publicBytes, { type: 'array' });
    expect(workbook.SheetNames).toEqual(['Checklists', 'Checklist_Items', 'Huong_dan']);

    const rootRows = XLSX.utils.sheet_to_json<unknown[]>(workbook.Sheets.Checklists, { header: 1, defval: '' });
    const itemRows = XLSX.utils.sheet_to_json<unknown[]>(workbook.Sheets.Checklist_Items, { header: 1, defval: '' });
    const guideRows = XLSX.utils.sheet_to_json<unknown[]>(workbook.Sheets.Huong_dan, { header: 1, defval: '' });
    expect(rootRows[0]).toEqual(ROOT_HEADERS);
    expect(itemRows[0]).toEqual(ITEM_HEADERS);
    expect(rootRows).toHaveLength(31);
    expect(itemRows).toHaveLength(111);
    expect(guideRows).toHaveLength(28);
    expect(rootRows[1]?.[0]).toBe('PRE_PREG_01');
    expect(rootRows[30]?.[0]).toBe('BABY_SAFETY_0_24M');
    expect(itemRows[110]?.[0]).toBe('BABY_SAFETY_0_24M');
    expect(guideRows[27]?.[1]).toContain('5MB');

    const parsedGroups = parseChecklistWorkbook(publicBytes);
    expect(parsedGroups).toHaveLength(30);
    expect(parsedGroups.every((group) => group.isValid)).toBe(true);
    expect(parsedGroups.reduce((total, group) => total + group.itemCount, 0)).toBe(110);
  });
});
