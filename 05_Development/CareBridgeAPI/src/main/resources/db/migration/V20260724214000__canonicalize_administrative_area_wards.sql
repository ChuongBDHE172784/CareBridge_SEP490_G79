-- Canonicalize the approved district/ward reference seed onto the typed
-- administrative_areas hierarchy. This migration deliberately contains no
-- DELETE and never rewrites an existing canonical identity or parent link.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.administrative_areas
    ADD COLUMN IF NOT EXISTS name_en varchar(255);

LOCK TABLE public.administrative_areas IN SHARE ROW EXCLUSIVE MODE;

DO $canonical_administrative_area_wards$
DECLARE
    seed record;
    expected_id uuid;
    parent_id uuid;
    existing_id uuid;
    existing_type varchar(30);
    existing_parent_id uuid;
    existing_legacy_code varchar(80);
    reconciled_id uuid;
    reconciled_type varchar(30);
    reconciled_parent_id uuid;
    reconciled_legacy_code varchar(80);
    reconciled_name varchar(255);
    reconciled_name_en varchar(255);
    district_seed_count integer := 0;
    ward_seed_count integer := 0;
BEGIN
    IF to_regclass('public.wards') IS NOT NULL THEN
        RAISE EXCEPTION
            'CANONICAL_ADMINISTRATIVE_AREA_WARDS: physical public.wards table must not exist';
    END IF;

    FOR seed IN
        SELECT *
          FROM (VALUES
            ('0101', '01', 'Ba Đình', 'Ba Dinh'),
            ('0102', '01', 'Hoàn Kiếm', 'Hoan Kiem'),
            ('0103', '01', 'Hai Bà Trưng', 'Hai Ba Trung'),
            ('0104', '01', 'Đống Đa', 'Dong Da'),
            ('0105', '01', 'Tây Hồ', 'Tay Ho'),
            ('0106', '01', 'Cầu Giấy', 'Cau Giay'),
            ('0107', '01', 'Thanh Xuân', 'Thanh Xuan'),
            ('0108', '01', 'Hoàng Mai', 'Hoang Mai'),
            ('0109', '01', 'Long Biên', 'Long Bien'),
            ('0110', '01', 'Nam Từ Liêm', 'Nam Tu Liem'),
            ('0111', '01', 'Bắc Từ Liêm', 'Bac Tu Liem'),
            ('0112', '01', 'Hà Đông', 'Ha Dong'),
            ('0113', '01', 'Sơn Tây', 'Son Tay'),
            ('0114', '01', 'Ba Vì', 'Ba Vi'),
            ('0115', '01', 'Phúc Thọ', 'Phuc Tho'),
            ('0116', '01', 'Đan Phượng', 'Dan Phuong'),
            ('0117', '01', 'Hoài Đức', 'Hoai Duc'),
            ('0118', '01', 'Quốc Oai', 'Quoc Oai'),
            ('0119', '01', 'Thạch Thất', 'Thach That'),
            ('0120', '01', 'Chương Mỹ', 'Chuong My'),
            ('0121', '01', 'Thanh Oai', 'Thanh Oai'),
            ('0122', '01', 'Thường Tín', 'Thuong Tin'),
            ('0123', '01', 'Phú Xuyên', 'Phu Xuyen'),
            ('0124', '01', 'Mê Linh', 'Me Linh'),
            ('0125', '01', 'Sóc Sơn', 'Soc Son'),
            ('0126', '01', 'Đông Anh', 'Dong Anh'),
            ('0127', '01', 'Gia Lâm', 'Gia Lam'),
            ('0128', '01', 'Thanh Trì', 'Thanh Tri'),
            ('0129', '01', 'Mỹ Đức', 'My Duc'),
            ('0130', '01', 'Ứng Hòa', 'Ung Hoa'),
            ('0201', '02', 'Quận 1', 'District 1'),
            ('0202', '02', 'Quận 3', 'District 3'),
            ('0203', '02', 'Quận 4', 'District 4'),
            ('0204', '02', 'Quận 5', 'District 5'),
            ('0205', '02', 'Quận 6', 'District 6'),
            ('0206', '02', 'Quận 7', 'District 7'),
            ('0207', '02', 'Quận 8', 'District 8'),
            ('0208', '02', 'Quận 10', 'District 10'),
            ('0209', '02', 'Quận 11', 'District 11'),
            ('0210', '02', 'Quận 12', 'District 12'),
            ('0211', '02', 'Bình Thạnh', 'Binh Thanh'),
            ('0212', '02', 'Tân Bình', 'Tan Binh'),
            ('0213', '02', 'Tân Phú', 'Tan Phu'),
            ('0214', '02', 'Phú Nhuận', 'Phu Nhuan'),
            ('0215', '02', 'Gò Vấp', 'Go Vap'),
            ('0216', '02', 'Bình Tân', 'Binh Tan'),
            ('0217', '02', 'Thủ Đức', 'Thu Duc'),
            ('0218', '02', 'Bình Chánh', 'Binh Chanh'),
            ('0219', '02', 'Cần Giờ', 'Can Gio'),
            ('0220', '02', 'Củ Chi', 'Cu Chi'),
            ('0221', '02', 'Hóc Môn', 'Hoc Mon'),
            ('0222', '02', 'Nhà Bè', 'Nha Be'),
            ('0301', '03', 'Hồng Bàng', 'Hong Bang'),
            ('0302', '03', 'Ngô Quyền', 'Ngo Quyen'),
            ('0303', '03', 'Lê Chân', 'Le Chan'),
            ('0304', '03', 'Hải An', 'Hai An'),
            ('0305', '03', 'Kiến An', 'Kien An'),
            ('0306', '03', 'Đồ Sơn', 'Do Son'),
            ('0307', '03', 'Dương Kinh', 'Duong Kinh'),
            ('0308', '03', 'Thuỷ Nguyên', 'Thuy Nguyen'),
            ('0309', '03', 'An Dương', 'An Duong'),
            ('0310', '03', 'An Lão', 'An Lao'),
            ('0311', '03', 'Kiến Thuỵ', 'Kien Thuy'),
            ('0312', '03', 'Tiên Lãng', 'Tien Lang'),
            ('0313', '03', 'Vĩnh Bảo', 'Vinh Bao'),
            ('0314', '03', 'Cát Hải', 'Cat Hai'),
            ('0315', '03', 'Bạch Long Vĩ', 'Bach Long Vi'),
            ('0401', '04', 'Hải Châu', 'Hai Chau'),
            ('0402', '04', 'Thanh Khê', 'Thanh Khe'),
            ('0403', '04', 'Sơn Trà', 'Son Tra'),
            ('0404', '04', 'Ngũ Hành Sơn', 'Ngu Hanh Son'),
            ('0405', '04', 'Liên Chiểu', 'Lien Chieu'),
            ('0406', '04', 'Cẩm Lệ', 'Cam Le'),
            ('0407', '04', 'Hòa Vang', 'Hoa Vang'),
            ('0408', '04', 'Hoàng Sa', 'Hoang Sa'),
            ('0501', '05', 'Ninh Kiều', 'Ninh Kieu'),
            ('0502', '05', 'Bình Thuỷ', 'Binh Thuy'),
            ('0503', '05', 'Cái Răng', 'Cai Rang'),
            ('0504', '05', 'Ô Môn', 'O Mon'),
            ('0505', '05', 'Thốt Nốt', 'Thot Not'),
            ('0506', '05', 'Vĩnh Thạnh', 'Vinh Thanh'),
            ('0507', '05', 'Cờ Đỏ', 'Co Do'),
            ('0508', '05', 'Phóng Điền', 'Phong Dien'),
            ('0509', '05', 'Thới Lai', 'Thoi Lai'),
            ('0601', '06', 'Phú Nhuận', 'Phu Nhuan'),
            ('0602', '06', 'Thuận Hóa', 'Thuan Hoa'),
            ('0603', '06', 'Hương Thủy', 'Huong Thuy'),
            ('0604', '06', 'Hương Trà', 'Huong Tra'),
            ('0605', '06', 'A Lưới', 'A Luoi'),
            ('0606', '06', 'Phú Lộc', 'Phu Loc'),
            ('0607', '06', 'Nam Đông', 'Nam Dong'),
            ('0608', '06', 'Quảng Điền', 'Quang Dien'),
            ('0609', '06', 'Phú Vang', 'Phu Vang'),
            ('0701', '07', 'Hà Giang', 'Ha Giang'),
            ('0702', '07', 'Đồng Văn', 'Dong Van'),
            ('0703', '07', 'Mèo Vạc', 'Meo Vac'),
            ('0704', '07', 'Yên Minh', 'Yen Minh'),
            ('0705', '07', 'Quản Bạ', 'Quan Ba'),
            ('0706', '07', 'Vị Xuyên', 'Vi Xuyen'),
            ('0707', '07', 'Bắc Mê', 'Bac Me'),
            ('0708', '07', 'Hoàng Su Phì', 'Hoang Su Phi'),
            ('0709', '07', 'Xín Mần', 'Xin Man'),
            ('0710', '07', 'Bắc Quang', 'Bac Quang'),
            ('0711', '07', 'Quang Bình', 'Quang Binh'),
            ('0801', '08', 'Cao Bằng', 'Cao Bang'),
            ('0802', '08', 'Bảo Lâm', 'Bao Lam'),
            ('0803', '08', 'Bảo Lạc', 'Bao Lac'),
            ('0804', '08', 'Thông Nông', 'Thong Nong'),
            ('0805', '08', 'Hà Quảng', 'Ha Quang'),
            ('0806', '08', 'Trà Lĩnh', 'Tra Linh'),
            ('0807', '08', 'Trùng Khánh', 'Trung Khanh'),
            ('0808', '08', 'Hạ Lang', 'Ha Lang'),
            ('0809', '08', 'Quang Uyên', 'Quang Uyen'),
            ('0810', '08', 'Phục Hòa', 'Phuc Hoa'),
            ('0811', '08', 'Hòa An', 'Hoa An'),
            ('0812', '08', 'Nguyên Bình', 'Nguyen Binh'),
            ('0813', '08', 'Thạch An', 'Thach An'),
            ('0901', '09', 'Bắc Kạn', 'Bac Kan'),
            ('0902', '09', 'Ba Bể', 'Ba Be'),
            ('0903', '09', 'Ngân Sơn', 'Ngan Son'),
            ('0904', '09', 'Chợ Đồn', 'Cho Don'),
            ('0905', '09', 'Chợ Mới', 'Cho Moi'),
            ('0906', '09', 'Na Rì', 'Na Ri'),
            ('0907', '09', 'Bạch Thông', 'Bach Thong'),
            ('0908', '09', 'Pác Nặm', 'Pac Nam'),
            ('1001', '10', 'Tuyên Quang', 'Tuyen Quang'),
            ('1002', '10', 'Lâm Bình', 'Lam Binh'),
            ('1003', '10', 'Na Hang', 'Na Hang'),
            ('1004', '10', 'Chiêm Hóa', 'Chiem Hoa'),
            ('1005', '10', 'Hàm Yên', 'Ham Yen'),
            ('1006', '10', 'Yên Sơn', 'Yen Son'),
            ('1007', '10', 'Sơn Dương', 'Son Duong'),
            ('1101', '11', 'Lào Cai', 'Lao Cai'),
            ('1102', '11', 'Bắc Hà', 'Bac Ha'),
            ('1103', '11', 'Sa Pa', 'Sa Pa'),
            ('1104', '11', 'Bát Xát', 'Bat Xat'),
            ('1105', '11', 'Mường Khương', 'Muong Khuong'),
            ('1106', '11', 'Si Ma Cai', 'Si Ma Cai'),
            ('1107', '11', 'Bảo Thắng', 'Bao Thang'),
            ('1108', '11', 'Bảo Yên', 'Bao Yen'),
            ('1109', '11', 'Văn Bàn', 'Van Ban'),
            ('1201', '12', 'Điện Biên Phủ', 'Dien Bien Phu'),
            ('1202', '12', 'Mường Lay', 'Muong Lay'),
            ('1203', '12', 'Mường Chà', 'Muong Cha'),
            ('1204', '12', 'Tủa Chùa', 'Tua Chua'),
            ('1205', '12', 'Tuần Giáo', 'Tuan Giao'),
            ('1206', '12', 'Điện Biên', 'Dien Bien'),
            ('1207', '12', 'Điện Biên Đông', 'Dien Bien Dong'),
            ('1208', '12', 'Mường Ảng', 'Muong Ang'),
            ('1209', '12', 'Nậm Pồ', 'Nam Po'),
            ('1210', '12', 'Nậm Nhùn', 'Nam Nhun'),
            ('1301', '13', 'Lai Châu', 'Lai Chau'),
            ('1302', '13', 'Tam Đường', 'Tam Duong'),
            ('1303', '13', 'Mường Tè', 'Muong Te'),
            ('1304', '13', 'Sìn Hồ', 'Sin Ho'),
            ('1305', '13', 'Phong Thổ', 'Phong Tho'),
            ('1306', '13', 'Than Uyên', 'Than Uyen'),
            ('1307', '13', 'Tân Uyên', 'Tan Uyen'),
            ('1308', '13', 'Nậm Nhùn', 'Nam Nhun'),
            ('1401', '14', 'Sơn La', 'Son La'),
            ('1402', '14', 'Mai Sơn', 'Mai Son'),
            ('1403', '14', 'Mường La', 'Muong La'),
            ('1404', '14', 'Yên Châu', 'Yen Chau'),
            ('1405', '14', 'Mộc Châu', 'Moc Chau'),
            ('1406', '14', 'Vân Hồ', 'Van Ho'),
            ('1407', '14', 'Phù Yên', 'Phu Yen'),
            ('1408', '14', 'Bắc Yên', 'Bac Yen'),
            ('1409', '14', 'Sông Mã', 'Song Ma'),
            ('1410', '14', 'Quỳnh Nhai', 'Quynh Nhai'),
            ('1411', '14', 'Thuận Châu', 'Thuan Chau'),
            ('1412', '14', 'Mường Tra', 'Muong Tra'),
            ('1501', '15', 'Yên Bái', 'Yen Bai'),
            ('1502', '15', 'Nghĩa Lộ', 'Nghia Lo'),
            ('1503', '15', 'Lục Yên', 'Luc Yen'),
            ('1504', '15', 'Văn Yên', 'Van Yen'),
            ('1505', '15', 'Mù Căng Chải', 'Mu Cang Chai'),
            ('1506', '15', 'Trấn Yên', 'Tran Yen'),
            ('1507', '15', 'Trạm Tấu', 'Tram Tau'),
            ('1508', '15', 'Văn Chấn', 'Van Chan'),
            ('1509', '15', 'Yên Bình', 'Yen Binh'),
            ('1601', '16', 'Hòa Bình', 'Hoa Binh'),
            ('1602', '16', 'Đà Bắc', 'Da Bac'),
            ('1603', '16', 'Kỳ Sơn', 'Ky Son'),
            ('1604', '16', 'Lương Sơn', 'Luong Son'),
            ('1605', '16', 'Kim Bôi', 'Kim Boi'),
            ('1606', '16', 'Cao Phong', 'Cao Phong'),
            ('1607', '16', 'Tân Lạc', 'Tan Lac'),
            ('1608', '16', 'Mai Châu', 'Mai Chau'),
            ('1609', '16', 'Lạc Sơn', 'Lac Son'),
            ('1610', '16', 'Yên Thủy', 'Yen Thuy'),
            ('1701', '17', 'Thái Nguyên', 'Thai Nguyen'),
            ('1702', '17', 'Sông Công', 'Song Cong'),
            ('1703', '17', 'Định Hóa', 'Dinh Hoa'),
            ('1704', '17', 'Phú Lương', 'Phu Luong'),
            ('1705', '17', 'Đồng Hỷ', 'Dong Hy'),
            ('1706', '17', 'Võ Nhai', 'Vo Nhai'),
            ('1707', '17', 'Đại Từ', 'Dai Tu'),
            ('1708', '17', 'Phổ Yên', 'Pho Yen'),
            ('1709', '17', 'Phú Bình', 'Phu Binh'),
            ('1801', '18', 'Lạng Sơn', 'Lang Son'),
            ('1802', '18', 'Đồi Ngô', 'Doi Ngo'),
            ('1803', '18', 'Tràng Định', 'Trang Dinh'),
            ('1804', '18', 'Bình Gia', 'Binh Gia'),
            ('1805', '18', 'Văn Lãng', 'Van Lang'),
            ('1806', '18', 'Cao Lộc', 'Cao Loc'),
            ('1807', '18', 'Văn Quan', 'Van Quan'),
            ('1808', '18', 'Lộc Bình', 'Loc Binh'),
            ('1809', '18', 'Hữu Lũng', 'Huu Lung'),
            ('1810', '18', 'Chi Lăng', 'Chi Lang'),
            ('1811', '18', 'Bắc Sơn', 'Bac Son'),
            ('1901', '19', 'Hạ Long', 'Ha Long'),
            ('1902', '19', 'Móng Cái', 'Mong Cai'),
            ('1903', '19', 'Cẩm Phả', 'Cam Pha'),
            ('1904', '19', 'Uông Bí', 'Uong Bi'),
            ('1905', '19', 'Đồng Triều', 'Dong Trieu'),
            ('1906', '19', 'Quảng Yên', 'Quang Yen'),
            ('1907', '19', 'Bình Liêu', 'Binh Lieu'),
            ('1908', '19', 'Tiên Yên', 'Tien Yen'),
            ('1909', '19', 'Đầm Hà', 'Dam Ha'),
            ('1910', '19', 'Hải Hà', 'Hai Ha'),
            ('1911', '19', 'Ba Chẽ', 'Ba Che'),
            ('1912', '19', 'Vân Đồn', 'Van Don'),
            ('1913', '19', 'Đông Triều', 'Dong Trieu'),
            ('1914', '19', 'Cô Tô', 'Co To'),
            ('2001', '20', 'Bắc Giang', 'Bac Giang'),
            ('2002', '20', 'Yên Thế', 'Yen The'),
            ('2003', '20', 'Tân Yên', 'Tan Yen'),
            ('2004', '20', 'Lạng Giang', 'Lang Giang'),
            ('2005', '20', 'Lục Nam', 'Luc Nam'),
            ('2006', '20', 'Lục Ngạn', 'Luc Ngan'),
            ('2007', '20', 'Sơn Động', 'Son Dong'),
            ('2008', '20', 'Yên Dũng', 'Yen Dung'),
            ('2009', '20', 'Hiệp Hòa', 'Hiep Hoa'),
            ('2010', '20', 'Việt Yên', 'Viet Yen'),
            ('2101', '21', 'Bắc Ninh', 'Bac Ninh'),
            ('2102', '21', 'Từ Sơn', 'Tu Son'),
            ('2103', '21', 'Quế Võ', 'Que Vo'),
            ('2104', '21', 'Tiên Du', 'Tien Du'),
            ('2105', '21', 'Thuận Thành', 'Thuan Thanh'),
            ('2106', '21', 'Gia Bình', 'Gia Binh'),
            ('2107', '21', 'Lương Tài', 'Luong Tai'),
            ('2108', '21', 'Yên Phong', 'Yen Phong'),
            ('2201', '22', 'Vĩnh Yên', 'Vinh Yen'),
            ('2202', '22', 'Phúc Yên', 'Phuc Yen'),
            ('2203', '22', 'Lập Thạch', 'Lap Thach'),
            ('2204', '22', 'Tam Dương', 'Tam Duong'),
            ('2205', '22', 'Tam Đảo', 'Tam Dao'),
            ('2206', '22', 'Bình Xuyên', 'Binh Xuyen'),
            ('2207', '22', 'Yên Lạc', 'Yen Lac'),
            ('2208', '22', 'Vĩnh Tường', 'Vinh Tuong'),
            ('2301', '23', 'Việt Trì', 'Viet Tri'),
            ('2302', '23', 'Phú Thọ', 'Phu Tho'),
            ('2303', '23', 'Đoan Hùng', 'Doan Hung'),
            ('2304', '23', 'Hạ Hoà', 'Ha Hoa'),
            ('2305', '23', 'Thanh Ba', 'Thanh Ba'),
            ('2306', '23', 'Phù Ninh', 'Phu Ninh'),
            ('2307', '23', 'Yên Lập', 'Yen Lap'),
            ('2308', '23', 'Cẩm Khê', 'Cam Khe'),
            ('2309', '23', 'Tam Nông', 'Tam Nong'),
            ('2310', '23', 'Lâm Thao', 'Lam Thao'),
            ('2311', '23', 'Thanh Sơn', 'Thanh Son'),
            ('2312', '23', 'Thanh Thuỷ', 'Thanh Thuy'),
            ('2313', '23', 'Tân Sơn', 'Tan Son'),
            ('2401', '24', 'Phủ Lý', 'Phu Ly'),
            ('2402', '24', 'Duy Tiên', 'Duy Tien'),
            ('2403', '24', 'Kim Bảng', 'Kim Bang'),
            ('2404', '24', 'Thanh Liêm', 'Thanh Liem'),
            ('2405', '24', 'Bình Lục', 'Binh Luc'),
            ('2406', '24', 'Lý Nhân', 'Ly Nhan'),
            ('2501', '25', 'Hưng Yên', 'Hung Yen'),
            ('2502', '25', 'Mỹ Hào', 'My Hao'),
            ('2503', '25', 'Vân Lâm', 'Van Lam'),
            ('2504', '25', 'Vân Giang', 'Van Giang'),
            ('2505', '25', 'Yên Mỹ', 'Yen My'),
            ('2506', '25', 'Ân Thi', 'An Thi'),
            ('2507', '25', 'Khoái Châu', 'Khoai Chau'),
            ('2508', '25', 'Kim Động', 'Kim Dong'),
            ('2509', '25', 'Tiên Lữ', 'Tien Lu'),
            ('2510', '25', 'Phù Cừ', 'Phu Cu'),
            ('2601', '26', 'Nam Định', 'Nam Dinh'),
            ('2602', '26', 'Mỹ Lộc', 'My Loc'),
            ('2603', '26', 'Vụ Bản', 'Vu Ban'),
            ('2604', '26', 'Ý Yên', 'Y Yen'),
            ('2605', '26', 'Nghĩa Hưng', 'Nghia Hung'),
            ('2606', '26', 'Nam Trực', 'Nam Truc'),
            ('2607', '26', 'Trực Ninh', 'Truc Ninh'),
            ('2608', '26', 'Xuân Trường', 'Xuan Truong'),
            ('2609', '26', 'Giao Thủy', 'Giao Thuy'),
            ('2610', '26', 'Hải Hậu', 'Hai Hau'),
            ('2701', '27', 'Thái Bình', 'Thai Binh'),
            ('2702', '27', 'Quỳnh Phụ', 'Quynh Phu'),
            ('2703', '27', 'Hưng Hà', 'Hung Ha'),
            ('2704', '27', 'Đông Hưng', 'Dong Hung'),
            ('2705', '27', 'Thái Thụy', 'Thai Thuy'),
            ('2706', '27', 'Tiền Hải', 'Tien Hai'),
            ('2707', '27', 'Kiến Xương', 'Kien Xuong'),
            ('2708', '27', 'Vũ Thư', 'Vu Thu'),
            ('2801', '28', 'Ninh Bình', 'Ninh Binh'),
            ('2802', '28', 'Tam Điệp', 'Tam Diep'),
            ('2803', '28', 'Nho Quan', 'Nho Quan'),
            ('2804', '28', 'Gia Viễn', 'Gia Vien'),
            ('2805', '28', 'Hoa Lư', 'Hoa Lu'),
            ('2806', '28', 'Yên Mô', 'Yen Mo'),
            ('2807', '28', 'Kim Sơn', 'Kim Son'),
            ('2808', '28', 'Yên Khánh', 'Yen Khanh'),
            ('2901', '29', 'Thanh Hóa', 'Thanh Hoa'),
            ('2902', '29', 'Bỉm Sơn', 'Bim Son'),
            ('2903', '29', 'Sầm Sơn', 'Sam Son'),
            ('2904', '29', 'Nông Cống', 'Nong Cong'),
            ('2905', '29', 'Thọ Xuân', 'Tho Xuan'),
            ('2906', '29', 'Thường Xuân', 'Thuong Xuan'),
            ('2907', '29', 'Triệu Sơn', 'Trieu Son'),
            ('2908', '29', 'Thiệu Hóa', 'Thieu Hoa'),
            ('2909', '29', 'Hà Trung', 'Ha Trung'),
            ('2910', '29', 'Ngọc Lặc', 'Ngoc Lac'),
            ('2911', '29', 'Cẩm Thủy', 'Cam Thuy'),
            ('2912', '29', 'Thạch Thành', 'Thach Thanh'),
            ('2913', '29', 'Vĩnh Lộc', 'Vinh Loc'),
            ('2914', '29', 'Yên Định', 'Yen Dinh'),
            ('2915', '29', 'Thọ Xuân', 'Tho Xuan'),
            ('2916', '29', 'Bá Thước', 'Ba Thuoc'),
            ('2917', '29', 'Mường Lát', 'Muong Lat'),
            ('2918', '29', 'Quy Châu', 'Quy Chau'),
            ('2919', '29', 'Quy Hợp', 'Quy Hop'),
            ('2920', '29', 'Nghĩa Dân', 'Nghia Dan'),
            ('2921', '29', 'Tân Kỳ', 'Tan Ky'),
            ('2922', '29', 'Hồ Lô', 'Ho Lo'),
            ('2923', '29', 'Hậu Lộc', 'Hau Loc'),
            ('2924', '29', 'Ngư Thổ', 'Ngu Tho'),
            ('2925', '29', 'Hà Quảng', 'Ha Quang'),
            ('3001', '30', 'Vinh', 'Vinh'),
            ('3002', '30', 'Cửa Lò', 'Cua Lo'),
            ('3003', '30', 'Thái Hòa', 'Thai Hoa'),
            ('3004', '30', 'Quỳ Hợp', 'Quynh Hop'),
            ('3005', '30', 'Quỳnh Lưu', 'Quynh Luu'),
            ('3006', '30', 'Kỳ Sơn', 'Ky Son'),
            ('3007', '30', 'Tương Dương', 'Tuong Duong'),
            ('3008', '30', 'Nghĩa Đàn', 'Nghia Dan'),
            ('3009', '30', 'Quỳnh Lưu', 'Quynh Luu'),
            ('3010', '30', 'Thanh Chương', 'Thanh Chuong'),
            ('3011', '30', 'Anh Sơn', 'Anh Son'),
            ('3012', '30', 'Diễn Châu', 'Dien Chau'),
            ('3013', '30', 'Yên Thành', 'Yen Thanh'),
            ('3014', '30', 'Đô Lương', 'Do Luong'),
            ('3015', '30', 'Tân Kỳ', 'Tan Ky'),
            ('3016', '30', 'Nam Đàn', 'Nam Dan'),
            ('3017', '30', 'Hưng Nguyên', 'Hung Nguyen'),
            ('3018', '30', 'Quế Phong', 'Que Phong'),
            ('3019', '30', 'Quỳ Châu', 'Quynh Chau'),
            ('3020', '30', 'Tân Kỳ', 'Tan Ky'),
            ('3021', '30', 'Côn Cuông', 'Con Cuong'),
            ('3101', '31', 'Hà Tĩnh', 'Ha Tinh'),
            ('3102', '31', 'Hồng Lĩnh', 'Hong Linh'),
            ('3103', '31', 'Kỳ Anh', 'Ky Anh'),
            ('3104', '31', 'Kỳ Anh (town)', 'Ky Anh Town'),
            ('3105', '31', 'Hương Khê', 'Huong Khe'),
            ('3106', '31', 'Hương Sơn', 'Huong Son'),
            ('3107', '31', 'Đức Thọ', 'Duc Tho'),
            ('3108', '31', 'Vũ Quang', 'Vu Quang'),
            ('3109', '31', 'Nghi Xuân', 'Nghi Xuan'),
            ('3110', '31', 'Can Lộc', 'Can Loc'),
            ('3111', '31', 'Lộc Hà', 'Loc Ha'),
            ('3112', '31', 'Thạch Hà', 'Thach Ha'),
            ('3113', '31', 'Cẩm Xuyên', 'Cam Xuyen'),
            ('3201', '32', 'Đồng Hới', 'Dong Hoi'),
            ('3202', '32', 'Ba Đồn', 'Ba Don'),
            ('3203', '32', 'Quảng Ninh', 'Quang Ninh'),
            ('3204', '32', 'Quảng Trạch', 'Quang Trach'),
            ('3205', '32', 'Bố Trạch', 'Bo Trach'),
            ('3206', '32', 'Minh Hóa', 'Minh Hoa'),
            ('3207', '32', 'Tuyên Hóa', 'Tuyen Hoa'),
            ('3208', '32', 'Lệ Thủy', 'Le Thuy'),
            ('3301', '33', 'Đông Hà', 'Dong Ha'),
            ('3302', '33', 'Quảng Trị', 'Quang Tri'),
            ('3303', '33', 'Khe Sanh', 'Khe Sanh'),
            ('3304', '33', 'Gio Linh', 'Gio Linh'),
            ('3305', '33', 'Cam Lộ', 'Cam Lo'),
            ('3306', '33', 'Triệu Phong', 'Trieu Phong'),
            ('3307', '33', 'Hải Lăng', 'Hai Lang'),
            ('3308', '33', 'Đa Krông', 'Da Krong'),
            ('3309', '33', 'Hướng Hóa', 'Huong Hoa'),
            ('3310', '33', 'Vĩnh Linh', 'Vinh Linh'),
            ('3401', '34', 'Tam Kỳ', 'Tam Ky'),
            ('3402', '34', 'Hội An', 'Hoi An'),
            ('3403', '34', 'Điện Bàn', 'Dien Ban'),
            ('3404', '34', 'T Đại Lộc', 'Dai Loc'),
            ('3405', '34', 'Điện Bàn', 'Dien Ban'),
            ('3406', '34', 'Duy Xuyên', 'Duy Xuyen'),
            ('3407', '34', 'Quế Sơn', 'Que Son'),
            ('3408', '34', 'Nam Giang', 'Nam Giang'),
            ('3409', '34', 'Phước Sơn', 'Phuoc Son'),
            ('3410', '34', 'Hiệp Đức', 'Hiep Duc'),
            ('3411', '34', 'Thăng Bình', 'Thang Binh'),
            ('3412', '34', 'Tiên Phước', 'Tien Phuoc'),
            ('3413', '34', 'Bắc Trà My', 'Bac Tra My'),
            ('3414', '34', 'Nam Trà My', 'Nam Tra My'),
            ('3415', '34', 'Núi Thành', 'Nui Thanh'),
            ('3416', '34', 'Phú Ninh', 'Phu Ninh'),
            ('3417', '34', 'Nông Sơn', 'Nong Son')
          ) AS district_seed(district_code, province_code, area_name, area_name_en)
    LOOP
        district_seed_count := district_seed_count + 1;
        expected_id := (
            substr(md5('district:' || seed.district_code), 1, 8) || '-' ||
            substr(md5('district:' || seed.district_code), 9, 4) || '-' ||
            substr(md5('district:' || seed.district_code), 13, 4) || '-' ||
            substr(md5('district:' || seed.district_code), 17, 4) || '-' ||
            substr(md5('district:' || seed.district_code), 21, 12)
        )::uuid;

        SELECT area.administrative_area_id
          INTO parent_id
          FROM public.administrative_areas area
         WHERE area.code = 'PROVINCE:' || seed.province_code
           AND area.area_type = 'PROVINCE'
           AND area.legacy_code = seed.province_code;
        IF NOT FOUND THEN
            RAISE EXCEPTION
                'CANONICAL_ADMINISTRATIVE_AREA_WARDS: missing canonical province % for district %',
                seed.province_code, seed.district_code;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM public.administrative_areas area
             WHERE area.area_type = 'DISTRICT'
               AND area.legacy_code = seed.district_code
               AND area.code <> 'DISTRICT:' || seed.district_code
        ) THEN
            RAISE EXCEPTION
                'CANONICAL_ADMINISTRATIVE_AREA_WARDS: conflicting district legacy code %',
                seed.district_code;
        END IF;

        SELECT area.administrative_area_id, area.area_type, area.parent_area_id, area.legacy_code
          INTO existing_id, existing_type, existing_parent_id, existing_legacy_code
          FROM public.administrative_areas area
         WHERE area.code = 'DISTRICT:' || seed.district_code;
        IF FOUND AND (
            existing_id IS DISTINCT FROM expected_id
            OR existing_type IS DISTINCT FROM 'DISTRICT'
            OR existing_parent_id IS DISTINCT FROM parent_id
            OR existing_legacy_code IS DISTINCT FROM seed.district_code
        ) THEN
            RAISE EXCEPTION
                'CANONICAL_ADMINISTRATIVE_AREA_WARDS: district identity/parent mismatch for %',
                seed.district_code;
        END IF;

        INSERT INTO public.administrative_areas (
            administrative_area_id,
            parent_area_id,
            area_type,
            code,
            name,
            name_en,
            legacy_code,
            created_at
        )
        VALUES (
            expected_id,
            parent_id,
            'DISTRICT',
            'DISTRICT:' || seed.district_code,
            seed.area_name,
            seed.area_name_en,
            seed.district_code,
            now()
        )
        ON CONFLICT (code) DO UPDATE SET
            name = excluded.name,
            name_en = excluded.name_en,
            legacy_code = excluded.legacy_code;

        SELECT area.administrative_area_id,
               area.area_type,
               area.parent_area_id,
               area.legacy_code,
               area.name,
               area.name_en
          INTO reconciled_id,
               reconciled_type,
               reconciled_parent_id,
               reconciled_legacy_code,
               reconciled_name,
               reconciled_name_en
          FROM public.administrative_areas area
         WHERE area.code = 'DISTRICT:' || seed.district_code;
        IF reconciled_id IS DISTINCT FROM expected_id
           OR reconciled_type IS DISTINCT FROM 'DISTRICT'
           OR reconciled_parent_id IS DISTINCT FROM parent_id
           OR reconciled_legacy_code IS DISTINCT FROM seed.district_code
           OR reconciled_name IS DISTINCT FROM seed.area_name
           OR reconciled_name_en IS DISTINCT FROM seed.area_name_en THEN
            RAISE EXCEPTION
                'CANONICAL_ADMINISTRATIVE_AREA_WARDS: district reconciliation failed for %',
                seed.district_code;
        END IF;
    END LOOP;

    IF district_seed_count <> 399 THEN
        RAISE EXCEPTION
            'CANONICAL_ADMINISTRATIVE_AREA_WARDS: expected 399 district seeds, found %',
            district_seed_count;
    END IF;

    FOR seed IN
        SELECT *
          FROM (VALUES
            ('01001', '0101', '01', 'Phúc Xá', 'Phuc Xa'),
            ('01002', '0101', '01', 'Trúc Bạch', 'Truc Bach'),
            ('01003', '0101', '01', 'Vĩnh Phúc', 'Vinh Phuc'),
            ('01004', '0101', '01', 'Cống Vị', 'Cong Vi'),
            ('01005', '0101', '01', 'Liễu Giai', 'Lieu Giai'),
            ('01006', '0101', '01', 'Nguyễn Trung Trực', 'Nguyen Trung Truc'),
            ('01007', '0101', '01', 'Quán Thánh', 'Quan Thanh'),
            ('01008', '0101', '01', 'Ngọc Hà', 'Ngoc Ha'),
            ('01009', '0101', '01', 'Điện Biên', 'Dien Bien'),
            ('01010', '0101', '01', 'Đội Cấn', 'Doi Can'),
            ('01011', '0102', '01', 'Phúc Tân', 'Phuc Tan'),
            ('01012', '0102', '01', 'Đồng Xuân', 'Dong Xuan'),
            ('01013', '0102', '01', 'Hàng Mã', 'Hang Ma'),
            ('01014', '0102', '01', 'Hàng Bồ', 'Hang Bo'),
            ('01015', '0102', '01', 'Cửa Đông', 'Cua Dong'),
            ('01016', '0102', '01', 'Lý Thái Tổ', 'Ly Thai To'),
            ('01017', '0102', '01', 'Hàng Bạc', 'Hang Bac'),
            ('01018', '0102', '01', 'Hàng Gai', 'Hang Gai'),
            ('01019', '0102', '01', 'Tràng Tiền', 'Trang Tien'),
            ('01020', '0102', '01', 'Hoàn Kiếm', 'Hoan Kiem'),
            ('02001', '0201', '02', 'Bến Nghé', 'Ben Nghe'),
            ('02002', '0201', '02', 'Bến Thành', 'Ben Thanh'),
            ('02003', '0201', '02', 'Cầu Kho', 'Cau Kho'),
            ('02004', '0201', '02', 'Cầu Ông Lãnh', 'Cau Ong Lanh'),
            ('02005', '0201', '02', 'Đa Kao', 'Da Kao'),
            ('02006', '0201', '02', 'Nguyễn Thái Bình', 'Nguyen Thai Binh'),
            ('02007', '0201', '02', 'Nguyễn Cư Trinh', 'Nguyen Cu Trinh'),
            ('02008', '0201', '02', 'Phạm Ngự Lao', 'Pham Ngu Lao'),
            ('03001', '0301', '03', 'Hà Bàng', 'Ha Bang'),
            ('03002', '0301', '03', 'Phú Đô', 'Phu Do'),
            ('03003', '0301', '03', 'Minh Khai', 'Minh Khai'),
            ('04001', '0401', '04', 'Hải Châu 1', 'Hai Chau 1'),
            ('04002', '0401', '04', 'Hải Châu 2', 'Hai Chau 2'),
            ('05001', '0501', '05', 'An Khánh', 'An Khanh'),
            ('05002', '0501', '05', 'An Lạc', 'An Lac')
          ) AS ward_seed(ward_code, district_code, province_code, area_name, area_name_en)
    LOOP
        ward_seed_count := ward_seed_count + 1;
        expected_id := (
            substr(md5('ward:' || seed.ward_code), 1, 8) || '-' ||
            substr(md5('ward:' || seed.ward_code), 9, 4) || '-' ||
            substr(md5('ward:' || seed.ward_code), 13, 4) || '-' ||
            substr(md5('ward:' || seed.ward_code), 17, 4) || '-' ||
            substr(md5('ward:' || seed.ward_code), 21, 12)
        )::uuid;

        SELECT district.administrative_area_id
          INTO parent_id
          FROM public.administrative_areas district
          JOIN public.administrative_areas province
            ON province.administrative_area_id = district.parent_area_id
           AND province.area_type = 'PROVINCE'
           AND province.code = 'PROVINCE:' || seed.province_code
           AND province.legacy_code = seed.province_code
         WHERE district.code = 'DISTRICT:' || seed.district_code
           AND district.area_type = 'DISTRICT'
           AND district.legacy_code = seed.district_code;
        IF NOT FOUND THEN
            RAISE EXCEPTION
                'CANONICAL_ADMINISTRATIVE_AREA_WARDS: missing canonical district % / province % for ward %',
                seed.district_code, seed.province_code, seed.ward_code;
        END IF;

        IF EXISTS (
            SELECT 1
              FROM public.administrative_areas area
             WHERE area.area_type = 'WARD'
               AND area.legacy_code = seed.ward_code
               AND area.code <> 'WARD:' || seed.ward_code
        ) THEN
            RAISE EXCEPTION
                'CANONICAL_ADMINISTRATIVE_AREA_WARDS: conflicting ward legacy code %',
                seed.ward_code;
        END IF;

        SELECT area.administrative_area_id, area.area_type, area.parent_area_id, area.legacy_code
          INTO existing_id, existing_type, existing_parent_id, existing_legacy_code
          FROM public.administrative_areas area
         WHERE area.code = 'WARD:' || seed.ward_code;
        IF FOUND AND (
            existing_id IS DISTINCT FROM expected_id
            OR existing_type IS DISTINCT FROM 'WARD'
            OR existing_parent_id IS DISTINCT FROM parent_id
            OR existing_legacy_code IS DISTINCT FROM seed.ward_code
        ) THEN
            RAISE EXCEPTION
                'CANONICAL_ADMINISTRATIVE_AREA_WARDS: ward identity/parent mismatch for %',
                seed.ward_code;
        END IF;

        INSERT INTO public.administrative_areas (
            administrative_area_id,
            parent_area_id,
            area_type,
            code,
            name,
            name_en,
            legacy_code,
            created_at
        )
        VALUES (
            expected_id,
            parent_id,
            'WARD',
            'WARD:' || seed.ward_code,
            seed.area_name,
            seed.area_name_en,
            seed.ward_code,
            now()
        )
        ON CONFLICT (code) DO UPDATE SET
            name = excluded.name,
            name_en = excluded.name_en,
            legacy_code = excluded.legacy_code;

        SELECT area.administrative_area_id,
               area.area_type,
               area.parent_area_id,
               area.legacy_code,
               area.name,
               area.name_en
          INTO reconciled_id,
               reconciled_type,
               reconciled_parent_id,
               reconciled_legacy_code,
               reconciled_name,
               reconciled_name_en
          FROM public.administrative_areas area
         WHERE area.code = 'WARD:' || seed.ward_code;
        IF reconciled_id IS DISTINCT FROM expected_id
           OR reconciled_type IS DISTINCT FROM 'WARD'
           OR reconciled_parent_id IS DISTINCT FROM parent_id
           OR reconciled_legacy_code IS DISTINCT FROM seed.ward_code
           OR reconciled_name IS DISTINCT FROM seed.area_name
           OR reconciled_name_en IS DISTINCT FROM seed.area_name_en THEN
            RAISE EXCEPTION
                'CANONICAL_ADMINISTRATIVE_AREA_WARDS: ward reconciliation failed for %',
                seed.ward_code;
        END IF;
    END LOOP;

    IF ward_seed_count <> 35 THEN
        RAISE EXCEPTION
            'CANONICAL_ADMINISTRATIVE_AREA_WARDS: expected 35 ward seeds, found %',
            ward_seed_count;
    END IF;
END
$canonical_administrative_area_wards$;
