-- =============================================================================
-- V20260722000000__create_wards_table.sql
-- Purpose: Create wards (phường, xã) table for Vietnamese administrative units
--          As of 2025-07-01 administrative reform
-- =============================================================================

CREATE TABLE public.wards (
    ward_id        varchar(6)  NOT NULL,
    district_id    varchar(4)  NOT NULL,
    province_id    varchar(2)  NOT NULL,
    name           varchar(100) NOT NULL,
    name_en        varchar(100),
    is_active      boolean     NOT NULL DEFAULT true,
    CONSTRAINT wards_pkey PRIMARY KEY (ward_id)
);

CREATE INDEX idx_wards_district ON public.wards (district_id);
CREATE INDEX idx_wards_province ON public.wards (province_id);

-- Sample wards data (key wards for major cities)
-- Full data can be added in future migration

-- Hanoi wards (sample)
INSERT INTO public.wards (ward_id, district_id, province_id, name, name_en, is_active) VALUES
('01001', '0101', '01', 'Phúc Xá', 'Phuc Xa', TRUE),
('01002', '0101', '01', 'Trúc Bạch', 'Truc Bach', TRUE),
('01003', '0101', '01', 'Vĩnh Phúc', 'Vinh Phuc', TRUE),
('01004', '0101', '01', 'Cống Vị', 'Cong Vi', TRUE),
('01005', '0101', '01', 'Liễu Giai', 'Lieu Giai', TRUE),
('01006', '0101', '01', 'Nguyễn Trung Trực', 'Nguyen Trung Truc', TRUE),
('01007', '0101', '01', 'Quán Thánh', 'Quan Thanh', TRUE),
('01008', '0101', '01', 'Ngọc Hà', 'Ngoc Ha', TRUE),
('01009', '0101', '01', 'Điện Biên', 'Dien Bien', TRUE),
('01010', '0101', '01', 'Đội Cấn', 'Doi Can', TRUE),

-- Hanoi - Hoàn Kiếm
('01011', '0102', '01', 'Phúc Tân', 'Phuc Tan', TRUE),
('01012', '0102', '01', 'Đồng Xuân', 'Dong Xuan', TRUE),
('01013', '0102', '01', 'Hàng Mã', 'Hang Ma', TRUE),
('01014', '0102', '01', 'Hàng Bồ', 'Hang Bo', TRUE),
('01015', '0102', '01', 'Cửa Đông', 'Cua Dong', TRUE),
('01016', '0102', '01', 'Lý Thái Tổ', 'Ly Thai To', TRUE),
('01017', '0102', '01', 'Hàng Bạc', 'Hang Bac', TRUE),
('01018', '0102', '01', 'Hàng Gai', 'Hang Gai', TRUE),
('01019', '0102', '01', 'Tràng Tiền', 'Trang Tien', TRUE),
('01020', '0102', '01', 'Hoàn Kiếm', 'Hoan Kiem', TRUE),

-- HCMC wards (sample)
('02001', '0201', '02', 'Bến Nghé', 'Ben Nghe', TRUE),
('02002', '0201', '02', 'Bến Thành', 'Ben Thanh', TRUE),
('02003', '0201', '02', 'Cầu Kho', 'Cau Kho', TRUE),
('02004', '0201', '02', 'Cầu Ông Lãnh', 'Cau Ong Lanh', TRUE),
('02005', '0201', '02', 'Đa Kao', 'Da Kao', TRUE),
('02006', '0201', '02', 'Nguyễn Thái Bình', 'Nguyen Thai Binh', TRUE),
('02007', '0201', '02', 'Nguyễn Cư Trinh', 'Nguyen Cu Trinh', TRUE),
('02008', '0201', '02', 'Phạm Ngự Lao', 'Pham Ngu Lao', TRUE),

-- Hải Phòng wards (sample)
('03001', '0301', '03', 'Hà Bàng', 'Ha Bang', TRUE),
('03002', '0301', '03', 'Phú Đô', 'Phu Do', TRUE),
('03003', '0301', '03', 'Minh Khai', 'Minh Khai', TRUE),

-- Đà Nẵng wards (sample)
('04001', '0401', '04', 'Hải Châu 1', 'Hai Chau 1', TRUE),
('04002', '0401', '04', 'Hải Châu 2', 'Hai Chau 2', TRUE),

-- Cần Thơ wards (sample)
('05001', '0501', '05', 'An Khánh', 'An Khanh', TRUE),
('05002', '0501', '05', 'An Lạc', 'An Lac', TRUE)
ON CONFLICT (ward_id) DO UPDATE SET
    district_id = EXCLUDED.district_id,
    province_id = EXCLUDED.province_id,
    name = EXCLUDED.name,
    name_en = EXCLUDED.name_en,
    is_active = EXCLUDED.is_active;

-- Note: This is a sample migration for key wards.
-- A full data migration with all ~11,000 wards can be added if needed.
-- For now, this provides the table structure and key wards for major cities.
