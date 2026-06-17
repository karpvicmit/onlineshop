-- Fügt initiale Testdaten für Kategorien und Produkte hinzu.

-- 1. Kategorien einfügen (Slugs sind bereits URL-freundlich generiert)
INSERT IGNORE INTO categories (name, description, slug, created_at) VALUES
                                                                 ('Elektronik', 'Smartphones, Laptops und hochwertiges Zubehör', 'elektronik', CURRENT_TIMESTAMP),
                                                                 ('Kleidung', 'Moderne Mode und Accessoires für Herren und Damen', 'kleidung', CURRENT_TIMESTAMP),
                                                                 ('Bücher', 'Fachliteratur, Romane und Hörbücher', 'buecher', CURRENT_TIMESTAMP),
                                                                 ('Lebensmittel', 'Frische Lebensmittel direkt zu Ihnen nach Hause', 'lebensmittel', CURRENT_TIMESTAMP),
                                                                 ('Sport & Freizeit', 'Outdoor-Ausrüstung und Fitnessgeräte', 'sport-freizeit', CURRENT_TIMESTAMP);

-- Produkte für Elektronik
INSERT IGNORE INTO products (name, description, price, stock, image_url, category_id, created_at, updated_at)
SELECT 'iPhone 15 Pro', 'Neuestes Smartphone mit Titan-Gehäuse und A17 Pro Chip', 1199.99, 15, '/uploads/iphone15pro.jpg', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories WHERE slug = 'elektronik';

INSERT IGNORE INTO products (name, description, price, stock, image_url, category_id, created_at, updated_at)
SELECT 'MacBook Air M2', 'Leichter und leistungsstarker Laptop für den Alltag und das Büro', 1299.00, 8, '/uploads/macbook_air_m2.jpg', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories WHERE slug = 'elektronik';

INSERT IGNORE INTO products (name, description, price, stock, image_url, category_id, created_at, updated_at)
SELECT 'Sony WH-1000XM5', 'Kabellose Noise-Cancelling-Kopfhörer mit erstklassigem Klang', 349.00, 25, '/uploads/sony_headphones.jpg', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories WHERE slug = 'elektronik';

-- Produkte für Kleidung
INSERT IGNORE INTO products (name, description, price, stock, image_url, category_id, created_at, updated_at)
SELECT 'Basic T-Shirt', '100% Bio-Baumwolle, schwarz, unisex, nachhaltig produziert', 19.99, 150, '/uploads/tshirt_black.jpg', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories WHERE slug = 'kleidung';

INSERT IGNORE INTO products (name, description, price, stock, image_url, category_id, created_at, updated_at)
SELECT 'Herren Jeans Slim Fit', 'Klassische blaue Jeans mit moderner Passform', 49.90, 60, '/uploads/jeans.jpg', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories WHERE slug = 'kleidung';

-- Produkte für Bücher
INSERT IGNORE INTO products (name, description, price, stock, image_url, category_id, created_at, updated_at)
SELECT 'Clean Code', 'Robert C. Martin - Ein Muss für jeden Softwareentwickler', 35.50, 40, '/uploads/cleancode.jpg', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories WHERE slug = 'buecher';

-- Produkte für Lebensmittel
INSERT IGNORE INTO products (name, description, price, stock, image_url, category_id, created_at, updated_at)
SELECT 'Landliebe Landkäse sanft-aromatisch Gouda', 'Erleben Sie den traditionellen und beliebtesten Käseklassiker von Landliebe! Der sanft-aromatische Gouda überzeugt durch seinen besonders cremigen und vollmundigen Geschmack.', 49.90, 60, '/uploads/kaese.png', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories WHERE slug = 'lebensmittel';

-- Produkte für Sport
INSERT IGNORE INTO products (name, description, price, stock, image_url, category_id, created_at, updated_at)
SELECT 'Yoga-Matte Premium', 'Rutschfeste Yogamatte aus umweltfreundlichem TPE-Material', 29.95, 30, '/uploads/yoga_mat.jpg', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories WHERE slug = 'sport-freizeit';