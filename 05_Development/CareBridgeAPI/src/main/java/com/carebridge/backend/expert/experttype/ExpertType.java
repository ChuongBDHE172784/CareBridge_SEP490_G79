package com.carebridge.backend.expert.experttype;

/**
 * Phân loại hai nhóm chuyên gia (docs/expert-two-tier-flow.md).
 *
 * <p>Đây là một máy trạng thái nằm gọn trong một cột, không phải hai sự kiện độc lập:
 * <pre>
 * NULL ──chọn hình thức + admin duyệt──▶ PENDING_CONTRACT ──admin xếp xuống──▶ COMMUNITY
 *                                              │
 *                                              └──chuyên gia ký──▶ CONTRACTED ──admin hạ──▶ COMMUNITY
 * </pre>
 *
 * <p>{@link #PENDING_CONTRACT} tồn tại vì luồng chốt là ký SAU khi admin duyệt: có một
 * trạng thái hợp lệ, kéo dài nhiều ngày, trong đó chuyên gia đã được duyệt chuyên môn và
 * đã được phát hành đề nghị nhưng chưa bấm ký. Nếu enum chỉ có hai giá trị thì trạng thái
 * đó buộc phải chọn một trong hai câu trả lời sai — ghi CONTRACTED thì huy hiệu bật khi
 * chưa có bản ký nào, ghi COMMUNITY thì hệ thống quên mất phải đẩy họ vào trang ký.
 */
public enum ExpertType {

    /** Chuyên gia Y tế Cộng đồng — tư vấn tình nguyện, không cam kết lịch cố định. */
    COMMUNITY,

    /** Đã xin hợp tác và đã được admin duyệt chuyên môn, đang chờ ký Thoả thuận. */
    PENDING_CONTRACT,

    /** Chuyên gia Hệ thống — đã ký Thoả thuận hợp tác, cam kết duy trì lịch trực. */
    CONTRACTED
}
