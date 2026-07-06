CREATE TABLE promo_codes (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             code VARCHAR(50) NOT NULL UNIQUE,
                             discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
                             discount_value DECIMAL(10, 2) NOT NULL,
                             min_order_amount DECIMAL(10, 2) DEFAULT NULL,
                             max_uses INT DEFAULT NULL,
                             used_count INT NOT NULL DEFAULT 0,
                             valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             valid_until TIMESTAMP NULL,
                             active BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             CONSTRAINT chk_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
                             CONSTRAINT chk_discount_value CHECK (discount_value >= 0),
                             CONSTRAINT chk_used_count CHECK (used_count >= 0)
);

CREATE INDEX idx_promo_codes_code ON promo_codes(code);
CREATE INDEX idx_promo_codes_active ON promo_codes(active);
CREATE INDEX idx_promo_codes_valid_until ON promo_codes(valid_until);

CREATE TABLE promo_code_usages (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   promo_code_id BIGINT NOT NULL,
                                   user_id BIGINT NOT NULL,
                                   order_id BIGINT NOT NULL,
                                   discount_amount DECIMAL(10, 2) NOT NULL,
                                   used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   CONSTRAINT fk_usage_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_usage_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_promo_usages_user ON promo_code_usages(user_id);
CREATE INDEX idx_promo_usages_promo_code ON promo_code_usages(promo_code_id);

ALTER TABLE orders
    ADD COLUMN promo_code_id BIGINT NULL,
ADD COLUMN discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
ADD CONSTRAINT fk_order_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id) ON DELETE SET NULL;

CREATE INDEX idx_orders_promo_code ON orders(promo_code_id);