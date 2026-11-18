CREATE TABLE IF NOT EXISTS rental_payments (
 rental_id INT UNSIGNED NOT NULL PRIMARY KEY,
 amount DECIMAL(12,2) NOT NULL,
 transfer_reference VARCHAR(40) NOT NULL UNIQUE,
 bank_code VARCHAR(20) NOT NULL,
 account_number VARCHAR(40) NOT NULL,
 account_holder VARCHAR(120) NOT NULL,
 pickup_time TIME NOT NULL,
 return_time TIME NOT NULL,
 status ENUM('pending','reported','paid') NOT NULL DEFAULT 'pending',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_payment_rental FOREIGN KEY (rental_id) REFERENCES rentals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
