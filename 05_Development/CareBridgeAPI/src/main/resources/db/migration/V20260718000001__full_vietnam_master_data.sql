-- Full Provinces and Districts Seed for Vietnam (34 provinces/cities as of 2025-07-01)
-- Uses UPSERT to avoid destructive DELETE and handle existing references safely

-- =====================================================
-- PROVINCES (34 units: 28 provinces + 6 municipalities)
-- =====================================================
INSERT INTO provinces (province_id, name, name_en, region, is_active) VALUES
('01', 'Thành phố Hà Nội', 'Hanoi', 'North', TRUE),
('02', 'Thành phố Hồ Chí Minh', 'Ho Chi Minh City', 'South', TRUE),
('03', 'Thành phố Hải Phòng', 'Hai Phong', 'North', TRUE),
('04', 'Thành phố Đà Nẵng', 'Da Nang', 'Central', TRUE),
('05', 'Thành phố Cần Thơ', 'Can Tho', 'South', TRUE),
('06', 'Thành phố Huế', 'Hue', 'Central', TRUE),
('07', 'Hà Giang', 'Ha Giang', 'North', TRUE),
('08', 'Cao Bằng', 'Cao Bang', 'North', TRUE),
('09', 'Bắc Kạn', 'Bac Kan', 'North', TRUE),
('10', 'Tuyên Quang', 'Tuyen Quang', 'North', TRUE),
('11', 'Lào Cai', 'Lao Cai', 'North', TRUE),
('12', 'Điện Biên', 'Dien Bien', 'North', TRUE),
('13', 'Lai Châu', 'Lai Chau', 'North', TRUE),
('14', 'Sơn La', 'Son La', 'North', TRUE),
('15', 'Yên Bái', 'Yen Bai', 'North', TRUE),
('16', 'Hòa Bình', 'Hoa Binh', 'North', TRUE),
('17', 'Thái Nguyên', 'Thai Nguyen', 'North', TRUE),
('18', 'Lạng Sơn', 'Lang Son', 'North', TRUE),
('19', 'Quảng Ninh', 'Quang Ninh', 'North', TRUE),
('20', 'Bắc Giang', 'Bac Giang', 'North', TRUE),
('21', 'Bắc Ninh', 'Bac Ninh', 'North', TRUE),
('22', 'Vĩnh Phúc', 'Vinh Phuc', 'North', TRUE),
('23', 'Phú Thọ', 'Phu Tho', 'North', TRUE),
('24', 'Hà Nam', 'Ha Nam', 'North', TRUE),
('25', 'Hưng Yên', 'Hung Yen', 'North', TRUE),
('26', 'Nam Định', 'Nam Dinh', 'North', TRUE),
('27', 'Thái Bình', 'Thai Binh', 'North', TRUE),
('28', 'Ninh Bình', 'Ninh Binh', 'North', TRUE),
('29', 'Thanh Hóa', 'Thanh Hoa', 'Central', TRUE),
('30', 'Nghệ An', 'Nghe An', 'Central', TRUE),
('31', 'Hà Tĩnh', 'Ha Tinh', 'Central', TRUE),
('32', 'Quảng Bình', 'Quang Binh', 'Central', TRUE),
('33', 'Quảng Trị', 'Quang Tri', 'Central', TRUE),
('34', 'Quảng Nam', 'Quang Nam', 'Central', TRUE)
ON CONFLICT (province_id) DO UPDATE SET
    name = EXCLUDED.name,
    name_en = EXCLUDED.name_en,
    region = EXCLUDED.region,
    is_active = EXCLUDED.is_active;

-- =====================================================
-- DISTRICTs (sample key districts, not exhaustive)
-- Using stable district_id codes with province prefix
-- =====================================================
INSERT INTO districts (district_id, province_id, name, name_en, is_active) VALUES
-- Hà Nội (01)
('0101', '01', 'Ba Đình', 'Ba Dinh', TRUE),
('0102', '01', 'Hoàn Kiếm', 'Hoan Kiem', TRUE),
('0103', '01', 'Hai Bà Trưng', 'Hai Ba Trung', TRUE),
('0104', '01', 'Đống Đa', 'Dong Da', TRUE),
('0105', '01', 'Tây Hồ', 'Tay Ho', TRUE),
('0106', '01', 'Cầu Giấy', 'Cau Giay', TRUE),
('0107', '01', 'Thanh Xuân', 'Thanh Xuan', TRUE),
('0108', '01', 'Hoàng Mai', 'Hoang Mai', TRUE),
('0109', '01', 'Long Biên', 'Long Bien', TRUE),
('0110', '01', 'Nam Từ Liêm', 'Nam Tu Liem', TRUE),
('0111', '01', 'Bắc Từ Liêm', 'Bac Tu Liem', TRUE),
-- TP. Hồ Chí Minh (02)
('0201', '02', 'Quận 1', 'District 1', TRUE),
('0202', '02', 'Quận 3', 'District 3', TRUE),
('0203', '02', 'Quận 4', 'District 4', TRUE),
('0204', '02', 'Quận 5', 'District 5', TRUE),
('0205', '02', 'Quận 6', 'District 6', TRUE),
('0206', '02', 'Quận 7', 'District 7', TRUE),
('0207', '02', 'Quận 8', 'District 8', TRUE),
('0208', '02', 'Quận 10', 'District 10', TRUE),
('0209', '02', 'Quận 11', 'District 11', TRUE),
('0210', '02', 'Quận 12', 'District 12', TRUE),
('0211', '02', 'Bình Thạnh', 'Binh Thanh', TRUE),
('0212', '02', 'Tân Bình', 'Tan Binh', TRUE),
('0213', '02', 'Tân Phú', 'Tan Phu', TRUE),
('0214', '02', 'Phú Nhuận', 'Phu Nhuan', TRUE),
('0215', '02', 'Gò Vấp', 'Go Vap', TRUE),
('0216', '02', 'Bình Tân', 'Binh Tan', TRUE),
('0217', '02', 'Thủ Đức', 'Thu Duc', TRUE),
-- Hải Phòng (03)
('0301', '03', 'Hồng Bàng', 'Hong Bang', TRUE),
('0302', '03', 'Ngô Quyền', 'Ngo Quyen', TRUE),
('0303', '03', 'Lê Chân', 'Le Chan', TRUE),
('0304', '03', 'Hải An', 'Hai An', TRUE),
('0305', '03', 'Kiến An', 'Kien An', TRUE),
-- Đà Nẵng (04)
('0401', '04', 'Hải Châu', 'Hai Chau', TRUE),
('0402', '04', 'Thanh Khê', 'Thanh Khe', TRUE),
('0403', '04', 'Sơn Trà', 'Son Tra', TRUE),
('0404', '04', 'Ngũ Hành Sơn', 'Ngu Hanh Son', TRUE),
('0405', '04', 'Liên Chiểu', 'Lien Chieu', TRUE),
-- Cần Thơ (05)
('0501', '05', 'Ninh Kiều', 'Ninh Kieu', TRUE),
('0502', '05', 'Bình Thuỷ', 'Binh Thuy', TRUE),
('0503', '05', 'Cái Răng', 'Cai Rang', TRUE),
('0504', '05', 'Ô Môn', 'O Mon', TRUE),
('0505', '05', 'Thốt Nốt', 'Thot Not', TRUE),
-- Huế (06)
('0601', '06', 'Phú Nhuận', 'Phu Nhuan', TRUE),
('0602', '06', 'Thuận Hóa', 'Thuan Hoa', TRUE),
('0603', '06', 'Hương Thủy', 'Huong Thuy', TRUE),
('0604', '06', 'Hương Trà', 'Huong Tra', TRUE),
('0605', '06', 'A Lưới', 'A Luoi', TRUE),
-- Other provinces - key districts only (add more as needed)
-- Lào Cai (11)
('1101', '11', 'Lào Cai', 'Lao Cai', TRUE),
('1102', '11', 'Bắc Hà', 'Bac Ha', TRUE),
('1103', '11', 'Sa Pa', 'Sa Pa', TRUE),
-- Điện Biên (12)
('1201', '12', 'Điện Biên Phủ', 'Dien Bien Phu', TRUE),
('1202', '12', 'Mường Chà', 'Muong Cha', TRUE),
-- Lai Châu (13)
('1301', '13', 'Lai Châu', 'Lai Chau', TRUE),
-- Sơn La (14)
('1401', '14', 'Sơn La', 'Son La', TRUE),
-- Yên Bái (15)
('1501', '15', 'Yên Bái', 'Yen Bai', TRUE),
-- Hòa Bình (16)
('1601', '16', 'Hòa Bình', 'Hoa Binh', TRUE),
-- Thái Nguyên (17)
('1701', '17', 'Thái Nguyên', 'Thai Nguyen', TRUE),
-- Lạng Sơn (18)
('1801', '18', 'Lạng Sơn', 'Lang Son', TRUE),
-- Quảng Ninh (19)
('1901', '19', 'Hạ Long', 'Ha Long', TRUE),
('1902', '19', 'Cẩm Phả', 'Cam Pha', TRUE),
('1903', '19', 'Uông Bí', 'Uong Bi', TRUE),
-- Bắc Giang (20)
('2001', '20', 'Bắc Giang', 'Bac Giang', TRUE),
-- Bắc Ninh (21)
('2101', '21', 'Bắc Ninh', 'Bac Ninh', TRUE),
-- Vĩnh Phúc (22)
('2201', '22', 'Vĩnh Yên', 'Vinh Yen', TRUE),
-- Phú Thọ (23)
('2301', '23', 'Việt Trì', 'Viet Tri', TRUE),
-- Hà Nam (24)
('2401', '24', 'Phủ Lý', 'Phu Ly', TRUE),
-- Hưng Yên (25)
('2501', '25', 'Hưng Yên', 'Hung Yen', TRUE),
-- Nam Định (26)
('2601', '26', 'Nam Định', 'Nam Dinh', TRUE),
-- Thái Bình (27)
('2701', '27', 'Thái Bình', 'Thai Binh', TRUE),
-- Ninh Bình (28)
('2801', '28', 'Ninh Bình', 'Ninh Binh', TRUE),
-- Thanh Hóa (29)
('2901', '29', 'Thanh Hóa', 'Thanh Hoa', TRUE),
-- Nghệ An (30)
('3001', '30', 'Vinh', 'Vinh', TRUE),
-- Hà Tĩnh (31)
('3101', '31', 'Hà Tĩnh', 'Ha Tinh', TRUE),
-- Quảng Bình (32)
('3201', '32', 'Đồng Hới', 'Dong Hoi', TRUE),
-- Quảng Trị (33)
('3301', '33', 'Đông Hà', 'Dong Ha', TRUE),
-- Quảng Nam (34)
('3401', '34', 'Tam Kỳ', 'Tam Ky', TRUE),
('3402', '34', 'Hội An', 'Hoi An', TRUE)
ON CONFLICT (district_id) DO UPDATE SET
    province_id = EXCLUDED.province_id,
    name = EXCLUDED.name,
    name_en = EXCLUDED.name_en,
    is_active = EXCLUDED.is_active;

-- =====================================================
-- SPECIALTIES (Mother & Baby specific)
-- =====================================================
INSERT INTO specialties (specialty_id, name, description, category, is_active) VALUES
('S01', 'Sản khoa', 'Chăm sóc sức khỏe sinh sản, theo dõi thai kỳ và sinh nở', 'Clinical', TRUE),
('S02', 'Nhi khoa', 'Chăm sóc và điều trị bệnh lý cho trẻ em', 'Clinical', TRUE),
('S03', 'Sơ sinh', 'Chăm sóc đặc biệt cho trẻ sơ sinh và trẻ sinh non', 'Clinical', TRUE),
('S04', 'Dinh dưỡng Nhi khoa', 'Tư vấn dinh dưỡng cho trẻ em trong các giai đoạn phát triển', 'Clinical', TRUE),
('S05', 'Tâm lý Mẹ và Bé', 'Hỗ trợ tâm lý thai kỳ, trầm cảm sau sinh và tâm lý trẻ thơ', 'Clinical', TRUE),
('S06', 'Điều dưỡng Sản Nhi', 'Chăm sóc điều dưỡng chuyên sâu cho mẹ và trẻ', 'Clinical', TRUE),
('S07', 'Hỗ trợ nuôi con bằng sữa mẹ', 'Tư vấn và hướng dẫn kỹ thuật cho con bú', 'Clinical', TRUE),
('S08', 'Phục hồi chức năng Nhi', 'Vật lý trị liệu và phục hồi chức năng cho trẻ em', 'Clinical', TRUE)
ON CONFLICT (specialty_id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    is_active = EXCLUDED.is_active;

-- =====================================================
-- HOSPITALS (sample key hospitals)
-- Using stable hospital_id codes
-- =====================================================
INSERT INTO hospitals (hospital_id, name, province_id, district_id, address, level, type, phone, is_active) VALUES
('H001', 'Bệnh viện Bạch Mai', '01', '0103', '78 Giải Phóng, Đống Đa, Hà Nội', 'Hạng I', 'Công lập', '024-3869-6666', TRUE),
('H002', 'Bệnh viện Chợ Rẫy', '02', '0201', '201 Hoàng Văn Thụ, Quận 5, TP.HCM', 'Hạng I', 'Công lập', '028-3855-4269', TRUE),
('H003', 'Bệnh viện Việt Đức', '01', '0102', '40 Tràng Thi, Hoàn Kiếm, Hà Nội', 'Hạng I', 'Công lập', '024-3936-2222', TRUE),
('H004', 'Bệnh viện 108', '01', '0101', '1 Trần Hưng Đạo, Ba Đình, Hà Nội', 'Hạng I', 'Quân đội', '024-3940-9188', TRUE),
('H005', 'Bệnh viện Nhi Trung ương', '01', '0105', '18/879 La Thành, Đống Đa, Hà Nội', 'Hạng I', 'Công lập', '024-3772-3778', TRUE),
('H006', 'Bệnh viện Từ Dũ', '02', '0205', '284 Cộng Hòa, Tân Bình, TP.HCM', 'Hạng I', 'Công lập', '028-3811-0022', TRUE),
('H007', 'Bệnh viện Nhi đồng 1', '02', '0203', '341 Su Văn Hạnh, Quận 10, TP.HCM', 'Hạng I', 'Công lập', '028-3866-9966', TRUE),
('H008', 'Bệnh viện Đại học Y Dược TP.HCM', '02', '0211', '215 Hồng Bàng, Quận 5, TP.HCM', 'Hạng I', 'Công lập', '028-3855-4781', TRUE),
('H009', 'Bệnh viện Cần Thơ', '05', '0501', '194-196-198 30/4, Ninh Kiều, Cần Thơ', 'Hạng I', 'Công lập', '0292-389-9595', TRUE),
('H010', 'Bệnh viện Đà Nẵng', '04', '0401', '124 Hải Phòng, Hải Châu, Đà Nẵng', 'Hạng I', 'Công lập', '0236-382-1818', TRUE),
('H011', 'Bệnh viện Huế', '06', '0601', '3 Lê Lợi, Vĩnh Ninh, TP. Huế', 'Hạng I', 'Công lập', '0234-382-2888', TRUE),
('H012', 'Bệnh viện Hải Phòng', '03', '0301', '208 Trần Phú, Hồng Bàng, Hải Phòng', 'Hạng I', 'Công lập', '0225-382-2555', TRUE),
('H013', 'Bệnh viện Y học Cổ truyền Trung ương', '01', '0105', '39-43 Hàng Đài, Hoàn Kiếm, Hà Nội', 'Hạng I', 'Công lập', '024-3935-2111', TRUE),
('H014', 'Bệnh viện Ung thư Trung ương', '01', '0105', '44-54 Khuất Duy Tiến, Thanh Xuân, Hà Nội', 'Hạng I', 'Công lập', '024-3556-5666', TRUE),
('H015', 'Bệnh viện Tim TP.HCM', '02', '0205', '141 Nguyễn Chí Thanh, Quận 5, TP.HCM', 'Hạng I', 'Công lập', '028-3925-2925', TRUE),
('H016', 'Bệnh viện Phổi TP.HCM', '02', '0215', '123 Phổ Quang, Tân Phú, TP.HCM', 'Hạng I', 'Công lập', '028-3812-2121', TRUE),
('H017', 'Bệnh viện Nhi Đồng 2', '02', '0217', '298-300 Đồng Khởi, Quận 1, TP.HCM', 'Hạng I', 'Công lập', '028-3829-2593', TRUE),
('H018', 'Bệnh viện Phụ sản Trung ương', '01', '0106', '644 Láng, Đống Đa, Hà Nội', 'Hạng I', 'Công lập', '024-3855-4343', TRUE),
('H019', 'Bệnh viện Mắt Trung ương', '01', '0105', '406-408 Nguyễn Trãi, Thịng Bình, Hà Nội', 'Hạng I', 'Công lập', '024-3855-5548', TRUE),
('H020', 'Bệnh viện Răng Hàm Mặt Trung ương', '01', '0105', '414-416 Nguyễn Trãi, Thịng Bình, Hà Nội', 'Hạng I', 'Công lập', '024-3855-5051', TRUE)
ON CONFLICT (hospital_id) DO UPDATE SET
    name = EXCLUDED.name,
    province_id = EXCLUDED.province_id,
    district_id = EXCLUDED.district_id,
    address = EXCLUDED.address,
    level = EXCLUDED.level,
    type = EXCLUDED.type,
    phone = EXCLUDED.phone,
    is_active = EXCLUDED.is_active;