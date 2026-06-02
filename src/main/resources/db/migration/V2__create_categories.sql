-- Erstellung der Tabelle 'categories' für die Produktkategorisierung
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    slug VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index für schnellere Suche nach Slug (wichtig für URL-Routing)
CREATE INDEX idx_categories_slug ON categories(slug);