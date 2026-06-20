CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        total_amount DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'NEW',
                        delivery_address TEXT NOT NULL,
                        CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE order_items (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(10, 2) NOT NULL, -- WICHTIG: Preis zum Zeitpunkt der Bestellung
                             CONSTRAINT fk_orderitem_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_orderitem_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);