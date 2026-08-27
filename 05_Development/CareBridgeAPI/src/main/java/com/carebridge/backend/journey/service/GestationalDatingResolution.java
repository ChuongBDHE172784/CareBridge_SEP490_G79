package com.carebridge.backend.journey.service;

import com.carebridge.backend.journey.entity.GestationalDatingBasis;

import java.time.LocalDate;

/**
 * =========================================================================================
 * DỮ LIỆU KẾT QUẢ TÍNH TOÁN TUỔI THAI & ĐỊNH THỜI GIAN THAI KỲ (GESTATIONAL DATING RESOLUTION)
 * =========================================================================================
 * 
 * Record bất biến (Immutable) chứa đầy đủ các chỉ số tính toán tuổi thai:
 * @param basis Cơ sở định thời gian (LMP hoặc EDD)
 * @param lastMenstrualDate Ngày đầu kỳ kinh cuối do người dùng nhập (nếu có)
 * @param estimatedDueDate Ngày dự sinh (do người dùng nhập hoặc suy diễn)
 * @param canonicalLmp Ngày kinh cuối chuẩn hóa (điểm neo tính toán trung tâm)
 * @param resolved Đã xác định được thẩm quyền tính toán tuổi thai hay chưa
 * @param semanticNoOp Cờ đánh dấu dữ liệu tính toán không thay đổi so với bản ghi trước
 * @param datingScope Phạm vi request có chứa thông tin cập nhật ngày thai kỳ không
 * @param completedGestationalWeek Số tuần thai trọn vẹn đã hoàn thành (0-based: 0..42+)
 * @param completedGestationalDays Số ngày lẻ trong tuần thai hiện tại (0..6)
 * @param sourceWeekNumber Tuần thai hiển thị cho người dùng và đối soát lâm sàng (1-based)
 * @param plan Chỉ số giai đoạn chăm sóc thai sản theo chuẩn WHO (1..8)
 */
public record GestationalDatingResolution(
        GestationalDatingBasis basis,
        LocalDate lastMenstrualDate,
        LocalDate estimatedDueDate,
        LocalDate canonicalLmp,
        boolean resolved,
        boolean semanticNoOp,
        boolean datingScope,
        int completedGestationalWeek,
        int completedGestationalDays,
        int sourceWeekNumber,
        Integer plan) {

    /**
     * Tạo đối tượng kết quả cho trường hợp chưa thể xác định được tuổi thai (chưa đủ dữ liệu LMP/EDD)
     */
    public static GestationalDatingResolution unresolved(
            LocalDate lastMenstrualDate,
            LocalDate estimatedDueDate,
            boolean datingScope) {
        return new GestationalDatingResolution(
                null,
                lastMenstrualDate,
                estimatedDueDate,
                null,
                false,
                false,
                datingScope,
                -1,
                -1,
                -1,
                null);
    }

    /**
     * Tạo đối tượng kết quả khi dữ liệu tuổi thai không thay đổi (Idempotent No-Op)
     */
    public static GestationalDatingResolution noOp(
            GestationalDatingBasis basis,
            LocalDate lastMenstrualDate,
            LocalDate estimatedDueDate,
            LocalDate canonicalLmp,
            int completedGestationalWeek,
            int completedGestationalDays,
            int sourceWeekNumber,
            Integer plan) {
        return noOp(basis, lastMenstrualDate, estimatedDueDate, canonicalLmp,
                completedGestationalWeek, completedGestationalDays, sourceWeekNumber, plan, true);
    }

    /**
     * Tạo đối tượng kết quả khi dữ liệu tuổi thai không thay đổi với tùy chọn datingScope
     */
    public static GestationalDatingResolution noOp(
            GestationalDatingBasis basis,
            LocalDate lastMenstrualDate,
            LocalDate estimatedDueDate,
            LocalDate canonicalLmp,
            int completedGestationalWeek,
            int completedGestationalDays,
            int sourceWeekNumber,
            Integer plan,
            boolean datingScope) {
        return new GestationalDatingResolution(
                basis,
                lastMenstrualDate,
                estimatedDueDate,
                canonicalLmp,
                true,
                true,
                datingScope,
                completedGestationalWeek,
                completedGestationalDays,
                sourceWeekNumber,
                plan);
    }
}

