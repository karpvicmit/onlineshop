ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'VORKASSE',
    ADD COLUMN shipping_method VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN shipping_cost DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

CREATE INDEX idx_orders_payment_method ON orders(payment_method);
CREATE INDEX idx_orders_shipping_method ON orders(shipping_method);