CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone VARCHAR(20) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL DEFAULT '',
    avatar_url TEXT NOT NULL DEFAULT '',
    status_text VARCHAR(500) NOT NULL DEFAULT 'Hey there! I am using WhatsApp.',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users(phone);
