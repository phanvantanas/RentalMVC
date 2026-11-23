-- =====================================================
-- CSDL hệ thống cho thuê máy (MySQL 8+ / MariaDB)
-- =====================================================
CREATE DATABASE IF NOT EXISTS rental_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE rental_db;

-- 1. users: tài khoản
CREATE TABLE IF NOT EXISTS users (
  id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  email      VARCHAR(150) NOT NULL UNIQUE,
  phone      VARCHAR(20)  NULL,
  password   VARCHAR(255) NOT NULL,              -- lưu hash (bcrypt/argon2 hoặc text thuần)
  role       ENUM('admin','user') NOT NULL DEFAULT 'user',
  status     ENUM('active','locked') NOT NULL DEFAULT 'active',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. categories: danh mục (hãng, loại)
CREATE TABLE IF NOT EXISTS categories (
  id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- 3. products: mẫu máy
CREATE TABLE IF NOT EXISTS products (
  id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_category INT UNSIGNED NOT NULL,
  name        VARCHAR(150) NOT NULL,
  image       VARCHAR(255) NULL,
  price       DECIMAL(12,2) NOT NULL DEFAULT 0,  -- giá thuê / ngày
  deposit     DECIMAL(12,2) NOT NULL DEFAULT 0,  -- tiền đặt cọc
  description TEXT NULL,
  status      ENUM('active','inactive') NOT NULL DEFAULT 'active',
  CONSTRAINT fk_products_category
    FOREIGN KEY (id_category) REFERENCES categories(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_products_category (id_category)
) ENGINE=InnoDB;

-- 4. devices: từng chiếc máy
CREATE TABLE IF NOT EXISTS devices (
  id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  product_id INT UNSIGNED NOT NULL,
  serial     VARCHAR(100) NOT NULL UNIQUE,
  status     ENUM('available','maintenance') NOT NULL DEFAULT 'available',
  CONSTRAINT fk_devices_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_devices_product (product_id)
) ENGINE=InnoDB;

-- 5. rentals: đơn thuê
CREATE TABLE IF NOT EXISTS rentals (
  id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id    INT UNSIGNED NOT NULL,
  start_date DATE NOT NULL,
  end_date   DATE NOT NULL,
  status     ENUM('pending','confirmed','renting','returned','cancelled')
             NOT NULL DEFAULT 'pending',
  total      DECIMAL(12,2) NOT NULL DEFAULT 0,
  deposit    DECIMAL(12,2) NOT NULL DEFAULT 0,
  late_fee   DECIMAL(12,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rentals_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_rentals_dates CHECK (end_date >= start_date),
  INDEX idx_rentals_user (user_id),
  INDEX idx_rentals_status (status)
) ENGINE=InnoDB;

-- 6. rental_items: máy nào thuê trong đơn nào
CREATE TABLE IF NOT EXISTS rental_items (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  rental_id     INT UNSIGNED NOT NULL,
  device_id     INT UNSIGNED NOT NULL,
  price_per_day DECIMAL(12,2) NOT NULL,          -- chốt giá tại thời điểm thuê
  CONSTRAINT fk_items_rental
    FOREIGN KEY (rental_id) REFERENCES rentals(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_items_device
    FOREIGN KEY (device_id) REFERENCES devices(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  UNIQUE KEY uq_rental_device (rental_id, device_id),
  INDEX idx_items_device (device_id)
) ENGINE=InnoDB;

-- =====================================================
-- Dữ liệu mẫu (Sample Data)
-- =====================================================
INSERT INTO users (name, email, phone, password, role) VALUES
('Quản trị viên', 'admin@rental.com', '0901234567', 'admin123', 'admin'),
('Nguyễn Văn A', 'customer@gmail.com', '0987654321', '123456', 'user')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO categories (name) VALUES
('Sony'),
('Canon'),
('Fujifilm'),
('Nikon')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO products (id_category, name, image, price, deposit, description, status) VALUES
(1, 'Sony Alpha A7 Mark IV', 'sony_a7iv.jpg', 350000.00, 5000000.00, 'Máy ảnh Mirrorless full-frame 33MP, quay 4K 60p chuyên nghiệp', 'active'),
(1, 'Sony FX3 Cinema Line', 'sony_fx3.jpg', 700000.00, 10000000.00, 'Máy quay điện ảnh nhỏ gọn, tản nhiệt chủ động, cảm biến full-frame', 'active'),
(2, 'Canon EOS R6 Mark II', 'canon_r6ii.jpg', 400000.00, 6000000.00, 'Mirrorless full-frame đa dụng, chụp liên tiếp 40fps', 'active'),
(3, 'Fujifilm X-T5', 'fuji_xt5.jpg', 300000.00, 4500000.00, 'Máy ảnh cảm biến APS-C 40.2MP, màu ảnh phim đặc trưng', 'active');

INSERT INTO devices (product_id, serial, status) VALUES
(1, 'SN-SONY-A7IV-001', 'available'),
(1, 'SN-SONY-A7IV-002', 'available'),
(2, 'SN-SONY-FX3-001', 'available'),
(3, 'SN-CANON-R6II-001', 'available'),
(4, 'SN-FUJI-XT5-001', 'available');

-- Return audit and separate late-fee collection
CREATE TABLE IF NOT EXISTS rental_returns (
    rental_id INT UNSIGNED PRIMARY KEY,
    due_at DATETIME NOT NULL,
    actual_return_at DATETIME NOT NULL,
    late_seconds BIGINT UNSIGNED NOT NULL,
    daily_rate DECIMAL(12,2) NOT NULL,
    charged_percent INT UNSIGNED NOT NULL,
    calculated_fee DECIMAL(12,2) NOT NULL,
    final_fee DECIMAL(12,2) NOT NULL,
    adjustment_reason VARCHAR(500) NULL,
    received_by INT UNSIGNED NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fee_status ENUM('unpaid','paid','waived') NOT NULL,
    fee_paid_at DATETIME NULL,
    fee_received_by INT UNSIGNED NULL,
    policy_version VARCHAR(40) NOT NULL,
    CONSTRAINT fk_returns_rental FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE RESTRICT,
    CONSTRAINT fk_returns_receiver FOREIGN KEY (received_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_returns_fee_receiver FOREIGN KEY (fee_received_by) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
