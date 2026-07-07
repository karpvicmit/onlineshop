ALTER TABLE orders
    ADD COLUMN payment_provider VARCHAR(30) DEFAULT 'VORKASSE',
    ADD COLUMN payment_provider_id VARCHAR(255),
    ADD COLUMN payment_status VARCHAR(30) DEFAULT 'PENDING',
    ADD COLUMN paid_at TIMESTAMP NULL;

CREATE INDEX idx_orders_payment_status ON orders(payment_status);
CREATE INDEX idx_orders_payment_provider_id ON orders(payment_provider_id);