import { describe, expect, it } from 'vitest';
import { formatAuditDetails, getAuditActionLabel } from './auditLog';

describe('audit log presentation', () => {
  it('translates known actions and hides unknown technical codes', () => {
    expect(getAuditActionLabel('DIRECT_CALL_STATE_CHANGED')).toBe('Cập nhật trạng thái cuộc gọi');
    expect(getAuditActionLabel('UNMAPPED_INTERNAL_EVENT')).toBe('Thao tác hệ thống');
  });

  it('summarizes audit-log views without exposing filter JSON', () => {
    expect(formatAuditDetails(
      'VIEW_AUDIT_LOG',
      '{"page":0,"size":5,"action":null,"userId":null}',
    )).toBe('Đã mở danh sách nhật ký hệ thống.');
  });

  it('turns direct-call recording metadata into a Vietnamese sentence', () => {
    expect(formatAuditDetails(
      'DIRECT_CALL_STATE_CHANGED',
      '{"recordingStatus":"NONE","recordingDeleted":true}',
    )).toBe('Đã xóa bản ghi cuộc gọi.');
  });

  it('explains notification-token and login metadata in Vietnamese', () => {
    expect(getAuditActionLabel('FIREBASE_CUSTOM_TOKEN_ISSUED'))
      .toBe('Khởi tạo kết nối thông báo');
    expect(formatAuditDetails('FIREBASE_CUSTOM_TOKEN_ISSUED', null))
      .toBe('Đã khởi tạo kết nối nhận thông báo.');
    expect(formatAuditDetails(
      'LOGIN',
      JSON.stringify({
        ipAddress: '0:0:0:0:0:0:0:1',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/151.0.0.0 Safari/537.36',
      }),
    )).toBe('Địa chỉ IP: Máy cục bộ • Trình duyệt: Google Chrome trên macOS');
  });
});
