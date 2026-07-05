ALTER TABLE products ADD COLUMN sku VARCHAR(50);

UPDATE products SET sku = CONCAT('SKU-', LPAD(id, 4, '0')) WHERE sku IS NULL;

ALTER TABLE products MODIFY COLUMN sku VARCHAR(50) NOT NULL;

ALTER TABLE products ADD CONSTRAINT uc_products_sku UNIQUE (sku);

CREATE INDEX idx_products_sku ON products(sku);