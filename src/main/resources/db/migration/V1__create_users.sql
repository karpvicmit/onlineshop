-- Erstellung der Tabelle 'users' für die Benutzerverwaltung
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index für schnellere Suche nach E-Mail (wird oft für Login verwendet)
CREATE INDEX idx_users_email ON users(email);