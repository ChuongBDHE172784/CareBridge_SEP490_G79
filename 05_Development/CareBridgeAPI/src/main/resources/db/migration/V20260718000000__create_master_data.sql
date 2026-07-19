-- Master Data seed
CREATE TABLE IF NOT EXISTS provinces (
    province_id VARCHAR(2) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    region VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS districts (
    district_id VARCHAR(4) PRIMARY KEY,
    province_id VARCHAR(2) NOT NULL REFERENCES provinces(province_id),
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS specialties (
    specialty_id VARCHAR(5) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS hospitals (
    hospital_id VARCHAR(8) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    province_id VARCHAR(2) NOT NULL REFERENCES provinces(province_id),
    district_id VARCHAR(4) REFERENCES districts(district_id),
    address TEXT,
    level VARCHAR(20),
    type VARCHAR(30),
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Seed data (sample)
INSERT INTO provinces (province_id, name, name_en, region) VALUES
('01', 'Hà Nội', 'Hanoi', 'North'),
('79', 'TP. Hồ Chí Minh', 'Ho Chi Minh City', 'South'),
('48', 'Đà Nẵng', 'Da Nang', 'Central');

INSERT INTO districts (district_id, province_id, name, name_en) VALUES
('001', '01', 'Ba Đình', 'Ba Dinh'),
('002', '01', 'Hoàn Kiếm', 'Hoan Kiem'),
('701', '79', 'Quận 1', 'District 1'),
('702', '79', 'Quận 3', 'District 3');

INSERT INTO specialties (specialty_id, name, description, category) VALUES
('S01', 'Sản khoa', 'Chuyên khoa chăm sóc sức khỏe sinh sản', 'Clinical'),
('S02', 'Nhi khoa', 'Chuyên khoa chăm sóc trẻ em', 'Clinical'),
('S03', 'Hồi sức cấp cứu', 'Xử lý các tình huống khẩn cấp', 'Critical Care');

INSERT INTO hospitals (hospital_id, name, province_id, district_id, level, type) VALUES
('H001', 'Bệnh viện Bạch Mai', '01', '001', 'Hạng I', 'Công lập'),
('H002', 'Bệnh viện Chợ Rẫy', '79', '701', 'Hạng I', 'Công lập');
