/**
 * Chuẩn hoá số di động Việt Nam về dạng E.164 (+84...).
 *
 * Phản chiếu đúng `VietnamesePhoneNumbers` phía backend. Backend vốn đã nhận cả
 * ba dạng `0...`, `84...`, `+84...` rồi tự quy về E.164, nhưng frontend lại chỉ
 * chấp nhận `+84...` — chặt hơn máy chủ, nên chặn oan số `0912345678` mà người
 * Việt vẫn gõ hằng ngày. Giữ hai bên cùng một quy tắc để màn hình không từ chối
 * thứ mà máy chủ sẵn sàng nhận.
 *
 * Trả về `null` khi không phải số di động Việt Nam hợp lệ.
 */

const ALLOWED_INPUT = /^[+0-9\s().-]+$/;
const SEPARATORS = /[\s().-]/g;
const VIETNAMESE_MOBILE_E164 = /^\+84[35789]\d{8}$/;

export function normalizeVietnamesePhone(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed || !ALLOWED_INPUT.test(trimmed)) {
    return null;
  }

  const compact = trimmed.replace(SEPARATORS, '');
  let normalized: string;
  if (compact.startsWith('+84')) {
    normalized = compact;
  } else if (compact.startsWith('84')) {
    normalized = `+${compact}`;
  } else if (compact.startsWith('0')) {
    normalized = `+84${compact.slice(1)}`;
  } else {
    return null;
  }

  return VIETNAMESE_MOBILE_E164.test(normalized) ? normalized : null;
}

/** Thông báo dùng chung, nêu cả hai dạng người dùng thường gõ. */
export const VIETNAMESE_PHONE_ERROR =
  'Số điện thoại cần là số di động Việt Nam, ví dụ 0901234567 hoặc +84901234567.';
