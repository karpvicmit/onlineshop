ALTER TABLE users
ADD COLUMN email_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN email_confirmation_token VARCHAR(255),
ADD COLUMN token_expiry_date TIMESTAMP;

CREATE INDEX idx_users_confirmation_token ON users(email_confirmation_token);