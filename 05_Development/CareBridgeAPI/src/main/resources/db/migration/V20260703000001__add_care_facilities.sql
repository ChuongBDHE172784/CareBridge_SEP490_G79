-- Seed care facilities for Sprint 0 demo
INSERT INTO care_facilities (facility_id, name, facility_type, address, latitude, longitude, phone, verification_status, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000101', 'Bệnh viện Phụ sản Trung ương Cần Thơ', 'HOSPITAL', '360 Đ. Nguyễn Văn Cừ, An Khánh, Ninh Kiều, Cần Thơ', 10.0186, 105.7878, '02923888888', 'VERIFIED', now(), now()),
('00000000-0000-0000-0000-000000000102', 'Phòng khám sản phụ khoa Hồng Hạc', 'CLINIC', '45B Đ. Lê Lợi, Tân An, Ninh Kiều, Cần Thơ', 10.0123, 105.7856, '0292123456', 'VERIFIED', now(), now()),
('00000000-0000-0000-0000-000000000103', 'Bệnh viện Đa khoa Trung ương Cần Thơ', 'HOSPITAL', '5 Đ. Nguyễn Văn Cừ, Hưng Lợi, Ninh Kiều, Cần Thơ', 10.0156, 105.7867, '02923868888', 'VERIFIED', now(), now()),
('00000000-0000-0000-0000-000000000104', 'Phòng khám Nhi Cửu Long', 'CLINIC', '12 Đ. Nguyễn Trãi, Xuân Khánh, Ninh Kiều, Cần Thơ', 10.0190, 105.7890, '0292765432', 'VERIFIED', now(), now()),
('00000000-0000-0000-0000-000000000105', 'Trạm y tế phường An Khánh', 'HEALTH_STATION', '88 Đ. Mậu Thân, An Khánh, Ninh Kiều, Cần Thơ', 10.0170, 105.7840, '0292111222', 'VERIFIED', now(), now());
