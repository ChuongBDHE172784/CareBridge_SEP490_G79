package com.carebridge.backend.expertverification.registry;

/**
 * One row of the registry result table.
 *
 * <p>Column order at the source (MF-05 Spec 05 §3.1):
 * {@code STT | Họ tên | Quốc tịch | Số chứng chỉ | Phạm vi hành nghề | Tình trạng | Mã chứng chỉ}.
 */
public record RegistryRow(
        String fullName,
        String nationality,
        String licenseNo,
        String practiceScope,
        String statusText,
        String sourceRecordId,
        String rowHtml) {
}
